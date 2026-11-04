"""Admission, failed-run charging and native drain safeguards, without engine games."""
import copy
import datetime as dt
import importlib.util
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

spec=importlib.util.spec_from_file_location('ic',Path(__file__).with_name('p4-iteration-confidence.py'))
ic=importlib.util.module_from_spec(spec);spec.loader.exec_module(ic)


class SupervisionTest(unittest.TestCase):
    def setUp(self):
        self.ledger={'ceilings':{'chargedWallSeconds':86400},'consumed':{'chargedWallSeconds':0,'fixedWallSeconds':0,'fixedNodes':0},
                     'pending':[],'ownedProcesses':[],'receipts':[]}
        self.p={'id':'fixture','kind':'fixed-work','games':0,'workers':4,'timeoutSeconds':5,'maximumNodes':10,'requests':1,
                'resources':{'estimatedPeakGiB':1.25,'personalReserveGiB':2},'inputs':{},'command':[sys.executable,'-c','raise SystemExit(3)']}
    def test_rejects_timed_games_and_unbounded_execution(self):
        for key,value in [('kind','timed'),('games',2),('workers',16),('timeoutSeconds',301)]:
            p=dict(self.p);p[key]=value
            with self.assertRaises(ValueError):ic.validate(p,self.ledger)
    def test_pending_ownership_and_drain_margin_protect_allowance(self):
        for key in ('pending','ownedProcesses'):
            b=copy.deepcopy(self.ledger);b[key]=['unreconciled']
            with self.assertRaises(ValueError):ic.validate(self.p,b)
        b=copy.deepcopy(self.ledger);b['consumed']['chargedWallSeconds']=86370
        with self.assertRaises(ValueError):ic.validate(self.p,b)
    def execute(self,busy):
        with tempfile.TemporaryDirectory(prefix='seed-ic-test-') as directory:
            root=Path(directory);out=root/'work';out.mkdir()
            (out/'budget.json').write_text(json.dumps(self.ledger));p=root/'protocol.json';p.write_text(json.dumps(self.p))
            sample={'freeMemoryGiB':16,'totalCpuPercent':95 if busy else 1,'logicalProcessors':12,
                    'overwatch':False,'demandingForeground':False,'foregroundApplication':'test'}
            with patch.object(ic,'ROOT',root),patch.object(ic,'OUT',out),patch.object(ic.supervision.PcUse,'sample',return_value=sample):
                with self.assertRaises(SystemExit):ic.run(p)
                receipt=json.loads((out/'fixture-execution.json').read_text());ledger=json.loads((out/'budget.json').read_text())
                before=(out/'budget.json').read_bytes()
                with self.assertRaises(FileExistsError):ic.run(p)
                self.assertEqual(before,(out/'budget.json').read_bytes())
            return receipt,ledger
    def test_busy_refusal_spends_no_allowance_and_cannot_overwrite_evidence(self):
        r,b=self.execute(True)
        self.assertEqual('refused-before-launch',r['status']);self.assertEqual(0,r['chargedWallSeconds'])
        self.assertEqual(self.ledger['consumed'],b['consumed']);self.assertEqual([],b['pending'])
    def test_failed_launched_process_is_charged_and_drained(self):
        r,b=self.execute(False)
        self.assertEqual('failed',r['status']);self.assertGreater(r['chargedWallSeconds'],0)
        self.assertEqual(0,r['drain']['ownedProcessesRemaining']);self.assertEqual([],b['pending'])
        self.assertEqual(r['chargedWallSeconds'],b['consumed']['chargedWallSeconds'])
        self.assertEqual(10,b['consumed']['fixedNodes'])
    def test_timed_requires_explicit_window_and_preserves_confirmation_allowance(self):
        p=dict(self.p,kind='timed',category='exploration',games=4,openingIndices=[0,1],window='window.json')
        b=copy.deepcopy(self.ledger);b['ceilings']['initiatedTimedGames']=800
        b['consumed']['initiatedTimedGames']=0;b['authorizedIdleWindows']=[]
        with self.assertRaises(ValueError):ic.validate(p,b)
        with tempfile.TemporaryDirectory(prefix='seed-ic-window-test-') as directory:
            root=Path(directory);now=ic.support.utc()
            window={'status':'authorized','receivedUtc':(now-dt.timedelta(seconds=1)).isoformat(),
                    'hardDeadlineUtc':(now+dt.timedelta(minutes=10)).isoformat()}
            (root/'window.json').write_text(json.dumps(window));b['authorizedIdleWindows']=['window.json']
            with patch.object(ic,'ROOT',root):
                ic.validate(p,b)
                b['consumed']['initiatedTimedGames']=318
                with self.assertRaises(ValueError):ic.validate(p,b)
                b['consumed']['initiatedTimedGames']=0
                window['hardDeadlineUtc']=(now+dt.timedelta(seconds=10)).isoformat()
                (root/'window.json').write_text(json.dumps(window))
                with self.assertRaises(ValueError):ic.validate(p,b)
    def test_timeout_charges_interrupted_process_and_drains_it(self):
        self.p.update(timeoutSeconds=1,command=[sys.executable,'-c','import time; time.sleep(10)'])
        r,b=self.execute(False)
        self.assertIn('TimeoutError',r['error']);self.assertGreaterEqual(r['chargedWallSeconds'],1)
        self.assertEqual(0,r['drain']['ownedProcessesRemaining']);self.assertEqual([],b['pending'])


if __name__=='__main__':unittest.main()
