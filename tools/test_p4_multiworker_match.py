"""Small admission/deadline tests; no competitive engine or campaign resources."""
import datetime as dt
import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import time
import unittest
from unittest.mock import patch

spec=importlib.util.spec_from_file_location('mw_match_tested',Path(__file__).with_name('p4-multiworker-match.py'))
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)

class MatchTest(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup)
        self.root=Path(self.temp.name);self.out=self.root/'study';self.out.mkdir();(self.out/'raw').mkdir()
        for name,value in [('ROOT',self.root),('OUT',self.out)]:
            p=patch.object(m,name,value);p.start();self.addCleanup(p.stop)
        self.now=dt.datetime.now(dt.timezone.utc)
        self.window=dict(status='authorized',receivedUtc=(self.now-dt.timedelta(seconds=5)).isoformat(),
                         hardDeadlineUtc=(self.now+dt.timedelta(seconds=3600)).isoformat())
        (self.root/'window.json').write_text(json.dumps(self.window))
        self.p=dict(id='fixture',kind='fixed',mode='A',category='exploration',workers=4,
            openingIndices=[0],timeoutSeconds=1,maxMoves=200,nodesPerMove=100,window='window.json',
            resources=dict(estimatedPeakGiB=2,personalReserveGiB=2),inputs={})
        self.ledger=dict(pending=[],ownedProcesses=[],receipts=[],confirmation=dict(protectedTimedGames=960,protectedTimedWallSeconds=86400),
            ceilings=dict(timedGames=3000,fixedGames=6000,fixedNodes=100000000000,timedWallSeconds=259200),
            consumed=dict(fixedInitiated=0,fixedCompleted=0,fixedFailed=0,fixedInterrupted=0,fixedWallSeconds=0,fixedNodes=0,
                          timedInitiated=0,timedCompleted=0,timedFailed=0,timedInterrupted=0,timedWallSeconds=0))

    def test_deadline_authority_does_not_restart_and_protects_confirmation(self):
        p=dict(self.p,mode='B',kind='timed',timeoutSeconds=300)
        self.assertEqual(m.admission(p,self.ledger,self.now)[0],0)
        with self.assertRaises(ValueError):m.admission(p,self.ledger,self.now+dt.timedelta(seconds=3400))
        self.ledger['consumed']['timedInitiated']=2040
        with self.assertRaises(ValueError):m.admission(p,self.ledger,self.now)
        with self.assertRaises(ValueError):m.admission(dict(p,mode='A'),self.ledger,self.now)

    def test_fixed_work_node_reservation_and_pending_ownership(self):
        self.assertEqual(m.admission(self.p,self.ledger,self.now)[0],80000)
        self.ledger['pending']=[dict(id='old')]
        with self.assertRaises(ValueError):m.admission(self.p,self.ledger,self.now)

    def test_idle_resource_envelope_is_explicit_without_weakening_daytime(self):
        sample=dict(totalCpuPercent=45,freeMemoryGiB=10,logicalProcessors=12,overwatch=False,demandingForeground=False)
        self.assertFalse(m.mw.resource_decision(sample,4,self.p['resources'])['admissible'])
        self.assertTrue(m.mw.resource_decision(sample,4,self.p['resources'],idle=True)['admissible'])
        sample['demandingForeground']=True
        self.assertFalse(m.mw.resource_decision(sample,4,self.p['resources'],idle=True)['admissible'])

    def test_native_deadline_drains_and_conservatively_accounts_partial_pair(self):
        path=self.root/'protocol.json';path.write_text(json.dumps(self.p));m.mw.save(self.out/'budget.json',self.ledger)
        original=subprocess.Popen
        def child(command,**kwargs):
            directory=command[command.index('--directory')+1]
            script='import pathlib,json,sys,time; p=pathlib.Path(sys.argv[1])/"progress.jsonl"; p.write_text(json.dumps(dict(type="pair-start",pair=0,initiatedGames=2))+"\\n"); time.sleep(30)'
            return original([sys.executable,'-B','-c',script,directory],**kwargs)
        sample=dict(totalCpuPercent=5,freeMemoryGiB=16,logicalProcessors=12,overwatch=False,demandingForeground=False)
        before=time.monotonic()
        with patch.object(m.subprocess,'Popen',side_effect=child),patch.object(m.mw.PcUse,'sample',return_value=sample):
            with self.assertRaises(SystemExit):m.supervise(path)
        self.assertLess(time.monotonic()-before,8)
        receipt=json.loads((self.out/'fixture-execution.json').read_text())
        self.assertEqual(receipt['status'],'interrupted');self.assertEqual(receipt['initiatedGames'],2)
        self.assertEqual(receipt['chargedNodes'],80000);self.assertEqual(receipt['drain']['ownedProcessesRemaining'],0)
        ledger=json.loads((self.out/'budget.json').read_text());self.assertEqual(ledger['pending'],[])
        self.assertEqual(ledger['consumed']['fixedInterrupted'],2)
        with self.assertRaises(FileExistsError):m.supervise(path)


if __name__=='__main__':unittest.main()
