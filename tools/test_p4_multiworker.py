"""Admission/continuity safeguards; no engine, match, or load-producing subprocess."""
import importlib.util
import json
from pathlib import Path
import tempfile
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
                         'command': ['MUST-NOT-START']}
        self.path = self.root / 'protocol.json'
        self.path.write_text(json.dumps(self.protocol))
        mw.save(self.out / 'budget.json', self.ledger)
        for name, value in [('ROOT', self.root), ('OUT', self.out)]:
            p = patch.object(mw, name, value); p.start(); self.addCleanup(p.stop)

    def run_refusal(self, sample):
        with patch.object(mw.support.PcUse, 'sample', side_effect=sample), \
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
        with patch.object(mw.support.PcUse, 'sample', side_effect=edit_and_refuse), \
             patch.object(mw.subprocess, 'Popen') as popen:
            with self.assertRaisesRegex(RuntimeError, 'Unexpected budget edit'): mw.run(self.path)
            popen.assert_not_called()
        self.assertEqual(self.edited, (self.out / 'budget.json').read_bytes())
        self.assertTrue(json.loads(self.edited)['pending'])


if __name__ == '__main__':
    unittest.main()
