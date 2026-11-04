"""Admission/continuity safeguards; no engine, match, or load-producing subprocess."""
import importlib.util
import json
from pathlib import Path
import tempfile
import sys
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('multiworker_tested', Path(__file__).with_name('p4-multiworker.py'))
mw = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mw)


class AdmissionTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.out = self.root / 'work'
        self.out.mkdir()
        self.ledger = {'pending': [], 'ownedProcesses': [], 'receipts': [],
                       'consumed': {'fixedNodes': 0, 'fixedWallSeconds': 0},
                       'ceilings': {'fixedNodes': 1000}}
        self.protocol = {'id': 'fixture', 'mode': 'A', 'kind': 'diagnostic',
                         'competitiveGames': 0, 'workers': 4, 'timeoutSeconds': 30,
                         'inputs': {}, 'maximumNodes': 100, 'requests': 1,
                         'resources': {'estimatedPeakGiB': 1.25, 'personalReserveGiB': 2},
                         'command': ['MUST-NOT-START']}
        self.path = self.root / 'protocol.json'
        self.path.write_text(json.dumps(self.protocol))
        mw.save(self.out / 'budget.json', self.ledger)
        for name, value in [('ROOT', self.root), ('OUT', self.out)]:
            p = patch.object(mw, name, value); p.start(); self.addCleanup(p.stop)

    def run_refusal(self, sample):
        with patch.object(mw.PcUse, 'sample', side_effect=sample), \
             patch.object(mw.subprocess, 'Popen') as popen:
            with self.assertRaises(SystemExit):
                mw.run(self.path)
            popen.assert_not_called()
        return json.loads((self.out / 'budget.json').read_text())

    def test_busy_pc_refusal_never_launches_or_charges_work(self):
        after = self.run_refusal(lambda: {'freeMemoryGiB': 4, 'overwatch': True})
        self.assertEqual(after['consumed'], self.ledger['consumed'])
        self.assertEqual(after['pending'], [])
        receipt = json.loads((self.out / 'fixture-execution.json').read_text())
        self.assertEqual(receipt['status'], 'refused-before-launch')
        self.assertEqual(receipt['drain']['totalOwnedProcesses'], 0)
        self.assertEqual(receipt['preflightEnvironment']['freeMemoryGiB'], 4)
        # Reusing the ID cannot overwrite refusal evidence or reserve more work.
        before = (self.out / 'budget.json').read_bytes()
        with self.assertRaises(FileExistsError): mw.run(self.path)
        self.assertEqual(before, (self.out / 'budget.json').read_bytes())

    def test_pending_ownership_and_node_ceiling_block_admission(self):
        for key, value in [('pending', [{'id': 'unresolved'}]), ('ownedProcesses', [1])]:
            b = dict(self.ledger); b[key] = value; mw.save(self.out / 'budget.json', b)
            with self.assertRaises(ValueError): mw.run(self.path)
        b = dict(self.ledger); b['consumed'] = {'fixedNodes': 950, 'fixedWallSeconds': 0}
        mw.save(self.out / 'budget.json', b)
        with self.assertRaises(ValueError): mw.run(self.path)
        self.assertFalse((self.out / 'raw').exists())

    def test_no_timed_or_competitive_admission(self):
        for key, value in [('mode', 'B'), ('competitiveGames', 2)]:
            p = dict(self.protocol); p[key] = value; self.path.write_text(json.dumps(p))
            with self.assertRaises(ValueError): mw.run(self.path)
        self.assertFalse((self.out / 'raw').exists())

    def test_unexpected_budget_edit_is_not_overwritten(self):
        def edit_and_refuse():
            self.edited = (self.out / 'budget.json').read_bytes() + b'\n'
            (self.out / 'budget.json').write_bytes(self.edited)
            return {'freeMemoryGiB': 4, 'overwatch': True}
        with patch.object(mw.PcUse, 'sample', side_effect=edit_and_refuse), \
             patch.object(mw.subprocess, 'Popen') as popen:
            with self.assertRaisesRegex(RuntimeError, 'Unexpected budget edit'): mw.run(self.path)
            popen.assert_not_called()
        self.assertEqual(self.edited, (self.out / 'budget.json').read_bytes())
        self.assertTrue(json.loads(self.edited)['pending'])

    @staticmethod
    def adequate(**changes):
        return dict(freeMemoryGiB=4.3, overwatch=True, foregroundApplication='Code.exe',
                    demandingForeground=False, logicalProcessors=12, totalCpuPercent=8, **changes)

    def test_budgeted_memory_and_background_app_can_be_adequate(self):
        d = mw.resource_decision(self.adequate(), 4, self.protocol['resources'])
        self.assertTrue(d['admissible'])
        self.assertEqual(d['requiredAvailableGiB'], 3.25)
        self.assertEqual(mw.continuation_action(d), 'proceed')
        for changes in [{'freeMemoryGiB': 3}, {'demandingForeground': True}, {'totalCpuPercent': 50}]:
            sample = self.adequate(); sample.update(changes)
            self.assertFalse(mw.resource_decision(sample, 4, self.protocol['resources'])['admissible'])

    def test_pressure_routes_to_useful_work_then_user_condition(self):
        sample = self.adequate(); sample['freeMemoryGiB'] = 2
        with patch.object(mw.PcUse, 'sample', return_value=sample), patch.object(mw.subprocess, 'Popen') as popen:
            with self.assertRaises(SystemExit) as error: mw.run(self.path, ['inspect root result selection'])
            self.assertEqual(error.exception.code, 75); popen.assert_not_called()
        state = json.loads((self.out / 'resource-state.json').read_text())
        self.assertEqual(state['status'], 'continue_low_impact_work')
        self.assertIsNone(state['pendingHumanAction'])
        self.assertEqual(mw.continuation_action(state['blocker'], []), 'await_user_input')

    def harmless_command(self, *, sleep=False):
        if sleep:
            return [sys.executable, '-B', '-c', 'import time; time.sleep(15)']
        script = ('import pathlib,json,sys; rows=[dict(type="request",measurement=dict(outcome=dict(nodes=7))),'
                  'dict(type="summary",status="passed",activeHelpersAfterClose=0,nodes=7,requests=1)];'
                  'pathlib.Path(sys.argv[1]).write_text("\\n".join(json.dumps(x) for x in rows)+"\\n")')
        return [sys.executable, '-B', '-c', script, '{output}']

    def test_recovery_rechecks_and_accounts_native_child_once(self):
        self.run_refusal(lambda: {'freeMemoryGiB': 2, 'overwatch': False})
        before = json.loads((self.out / 'resource-state.json').read_text())
        self.assertEqual(before['status'], 'await_user_input')
        self.assertTrue(before['pendingHumanAction'])
        self.protocol.update(id='recovered', command=self.harmless_command())
        self.path = self.root / 'recovered.json'; self.path.write_text(json.dumps(self.protocol))
        with patch.object(mw.PcUse, 'sample', return_value=self.adequate()): mw.run(self.path)
        receipt = json.loads((self.out / 'recovered-execution.json').read_text())
        ledger = json.loads((self.out / 'budget.json').read_text())
        self.assertEqual(receipt['status'], 'passed')
        self.assertEqual(receipt['drain']['ownedProcessesRemaining'], 0)
        self.assertGreater(receipt['drain']['totalOwnedProcesses'], 0)
        self.assertEqual(ledger['consumed']['fixedNodes'], 7)
        self.assertEqual(ledger['pending'], [])
        self.assertEqual(json.loads((self.out / 'resource-state.json').read_text())['status'], 'continue_research')

    def test_running_pressure_drains_and_conservatively_charges(self):
        self.protocol['command'] = self.harmless_command(sleep=True)
        self.path.write_text(json.dumps(self.protocol))
        bad = self.adequate(); bad['freeMemoryGiB'] = 1.5
        with patch.object(mw.PcUse, 'sample', side_effect=[self.adequate(), self.adequate(), bad]):
            with self.assertRaises(SystemExit) as error: mw.run(self.path)
            self.assertEqual(error.exception.code, 75)
        receipt = json.loads((self.out / 'fixture-execution.json').read_text())
        self.assertEqual(receipt['drain']['ownedProcessesRemaining'], 0)
        self.assertEqual(receipt['chargedNodes'], 100)
        self.assertEqual(json.loads((self.out / 'resource-state.json').read_text())['status'], 'await_user_input')
        self.assertEqual(json.loads((self.out / 'budget.json').read_text())['pending'], [])

    def test_owner_stop_is_checked_before_launch(self):
        (self.out / 'STOP').touch()
        self.run_refusal(lambda: self.adequate())


if __name__ == '__main__':
    unittest.main()
