import copy
import datetime as dt
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
from unittest.mock import MagicMock
import contextlib
import io

ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('clv_confirm',ROOT/'tools/p4-critical-line-confirm.py')
c=importlib.util.module_from_spec(spec);spec.loader.exec_module(c)


class ConfirmationBoundaryTest(unittest.TestCase):
    def setUp(self):
        self.p=dict(id='confirmation',campaign='CLV-R001',category='confirmation',kind='timed',workers=4,
            baseline='EXACT96',candidate='C1',tc='10+0.1',maxMoves=300,firstColorPolicy='alternate',games=640,
            openingIndices=list(range(320)),timeoutSeconds=28800,window='window.json',
            confirmation=dict(gate=c.GATE.copy(),interimScoreAccess='prohibited',
                failureRule='incomplete-attempt-no-pass-no-replay',bootstrap=dict(samples=10000,seed=2026110617)))
        self.b=dict(pending=[],ownedProcesses=[],authorizedIdleWindows=[],
            consumed=dict(confirmationAttempts=0,confirmationGames=0,initiatedTimedGames=68,chargedWallSeconds=2400),
            ceilings=dict(initiatedTimedGames=1280,confirmationGames=640,chargedWallSeconds=86400))
        self.now=dt.datetime(2026,11,6,tzinfo=dt.timezone.utc)

    def test_no_old_window_or_spent_attempt_can_admit_confirmation(self):
        with self.assertRaisesRegex(ValueError,'separately authorized'):c.validate(self.p,self.b,self.now)
        self.b['consumed']['confirmationAttempts']=1
        with self.assertRaisesRegex(ValueError,'already consumed'):c.validate(self.p,self.b,self.now)

    def test_sample_and_gate_cannot_be_changed(self):
        p=copy.deepcopy(self.p);p['games']=638
        with self.assertRaisesRegex(ValueError,'320-pair'):c.validate(p,self.b,self.now)
        p=copy.deepcopy(self.p);p['confirmation']['gate']['minimumScore']=.52
        with self.assertRaisesRegex(ValueError,'prospective'):c.validate(p,self.b,self.now)

    def test_window_scope_and_drain_margin_are_mandatory(self):
        with tempfile.TemporaryDirectory() as directory,patch.object(c,'ROOT',Path(directory)):
            self.b['authorizedIdleWindows']=['window.json']
            w=dict(status='authorized',receivedUtc=self.now.isoformat(),hardDeadlineUtc=(self.now+dt.timedelta(hours=9)).isoformat(),
                maximumTimedGames=640,allowedProtocols=['old-pilot'])
            path=Path(directory)/'window.json';path.write_text(json.dumps(w))
            with self.assertRaisesRegex(ValueError,'actual owner scope'):c.validate(self.p,self.b,self.now)
            w['allowedProtocols']=['confirmation'];w['hardDeadlineUtc']=(self.now+dt.timedelta(hours=8)).isoformat();path.write_text(json.dumps(w))
            with self.assertRaisesRegex(ValueError,'actual owner scope'):c.validate(self.p,self.b,self.now)

    def test_first_pair_attempt_accounting_is_sticky_and_attributed(self):
        r=dict(protocol='p.json',protocolSha256='hash')
        self.assertFalse(c.consume_attempt(self.b,self.p,r,0))
        self.assertEqual(0,self.b['consumed']['confirmationAttempts'])
        self.assertTrue(c.consume_attempt(self.b,self.p,r,2))
        self.assertFalse(c.consume_attempt(self.b,self.p,r,4))
        self.assertEqual(1,self.b['consumed']['confirmationAttempts'])
        with self.assertRaisesRegex(ValueError,'another protocol'):
            c.consume_attempt(self.b,self.p,dict(protocol='q.json',protocolSha256='other'),2)

    def test_partial_progress_is_not_a_complete_sample(self):
        with tempfile.TemporaryDirectory() as directory:
            p=Path(directory)/'progress.jsonl';p.write_text('{"type":"pair-start"}\n{')
            self.assertEqual(([],False),c.progress(p))
            self.assertEqual('pair-start',c.m.first_pair_start(p)['type'])
        with self.assertRaisesRegex(ValueError,'Complete320'):c.assess([1]*319)

    def test_primary_strict_lower_boundary_and_degenerate_variance_are_explicit(self):
        draws=c.assess([1]*320)
        self.assertEqual([.5,.5],draws['primaryPairedNormal95']);self.assertEqual('not-passed',draws['primaryStatisticalGate'])
        constant=c.assess([1.5]*320)
        self.assertEqual([.75,.75],constant['primaryPairedNormal95']);self.assertEqual('passed',constant['primaryStatisticalGate'])
        self.assertEqual([.75,.75],constant['pairedBootstrapPercentile95'])

    def simulated_run(self,pairs,completed,exit_code,refuse=False):
        # An isolated synthetic process/ledger test: no engines, timed games or
        # real campaign state. Native ownership primitives are independently reused.
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory);out=root/'campaign';(out/'raw').mkdir(parents=True)
            p=copy.deepcopy(self.p);p.update(inputs={},command=['synthetic-worker'],resources=dict(estimatedPeakGiB=2,personalReserveGiB=2))
            b=copy.deepcopy(self.b);b['receipts']=[];b['consumed'].update(timedWallSeconds=0,completedTimedGames=0,failedTimedGames=0,interruptedTimedGames=0)
            (out/'budget.json').write_text(json.dumps(b));protocol=root/'protocol.json';protocol.write_text(json.dumps(p))
            process=MagicMock();process.pid=123456;process.wait.return_value=exit_code;process.poll.return_value=exit_code
            def launch(*args,**kwargs):
                rows=[]
                for i in range(pairs):
                    rows.append(dict(type='pair-start',pair=i))
                    if i<completed:rows.append(dict(type='pair-complete',pair=i,nodes=2))
                if completed==320:rows.append(dict(type='summary',status='passed'))
                (out/'raw'/p['id']/'progress.jsonl').write_text(''.join(json.dumps(x)+'\n' for x in rows))
                return process
            with contextlib.ExitStack() as stack:
                for target,value in [('ROOT',root),('OUT',out)]:stack.enter_context(patch.object(c,target,value))
                stack.enter_context(patch.object(c,'validate',return_value=c.support.utc()+dt.timedelta(hours=9)))
                stack.enter_context(patch.object(c.time,'sleep'))
                stack.enter_context(patch.object(c.support.owned,'OwnedJob',return_value=MagicMock()))
                stack.enter_context(patch.object(c.support,'drain',return_value=dict(ownedProcessesRemaining=0,totalOwnedProcesses=1)))
                stack.enter_context(patch.object(c.m.mw,'peak_memory',return_value=.1))
                pc=MagicMock();pc.sample.return_value=dict(totalCpuPercent=10)
                stack.enter_context(patch.object(c.m.mw,'PcUse',return_value=pc))
                stack.enter_context(patch.object(c.m.mw,'resource_decision',return_value=dict(reasons=['busy'] if refuse else [])))
                popen=stack.enter_context(patch.object(c.subprocess,'Popen',side_effect=launch))
                stack.enter_context(contextlib.redirect_stdout(io.StringIO()))
                if refuse or exit_code:
                    with self.assertRaises(SystemExit):c.run(protocol)
                else:c.run(protocol)
                if refuse:popen.assert_not_called()
            return json.loads((out/'budget.json').read_text())

    def test_complete_supervision_charges_exactly_one_attempt_and640_games(self):
        b=self.simulated_run(320,320,0)
        self.assertEqual(1,b['consumed']['confirmationAttempts']);self.assertEqual(640,b['consumed']['confirmationGames'])
        self.assertEqual(708,b['consumed']['initiatedTimedGames']);self.assertEqual([],b['pending'])

    def test_failed_pair_remains_spent_without_replacement(self):
        b=self.simulated_run(1,0,1)
        self.assertEqual(1,b['consumed']['confirmationAttempts']);self.assertEqual(2,b['consumed']['confirmationGames'])
        self.assertEqual(2,b['consumed']['failedTimedGames']);self.assertEqual(0,b['consumed']['completedTimedGames'])

    def test_refusal_before_process_creation_does_not_consume_attempt(self):
        b=self.simulated_run(0,0,0,True)
        self.assertEqual(0,b['consumed']['confirmationAttempts']);self.assertEqual(0,b['consumed']['confirmationGames'])
        self.assertEqual(2400,b['consumed']['chargedWallSeconds']);self.assertEqual([],b['pending'])


if __name__=='__main__':unittest.main()
