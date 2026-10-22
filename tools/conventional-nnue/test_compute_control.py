"""Real Windows process/control integration checks; short sleeping workloads only.

Every test uses isolated control state. Never changes the owner's permission.
Evidence is retained under build/conventional-nnue/control-tests-*.
"""
import json
import os
from pathlib import Path
import subprocess
import sys
import time
import unittest
import uuid
import compute_control as cc
import windows_job as windows

EVIDENCE = cc.ROOT / 'build/conventional-nnue' / ('control-tests-' + uuid.uuid4().hex[:12])
EVIDENCE.mkdir(parents=True)
MEASUREMENTS = []


class Controls(unittest.TestCase):
    def setUp(self):
        self.folder = EVIDENCE / self._testMethodName
        self.control = cc.Control(self.folder / 'state')
        self.procs = []

    def tearDown(self):
        if self.control.path.exists():
            try: self.control.request('COMPUTE_HOLD', now=True)
            except (ValueError, RuntimeError): pass
        for process in self.procs:
            if process.poll() is None: process.kill()
            process.wait(timeout=10)

    def command(self, *arguments, check=False):
        return subprocess.run([sys.executable, '-B', str(Path(cc.__file__)), '--state-dir',
            str(self.control.directory), *arguments], capture_output=True, text=True, check=check)

    def launch(self, cooperative=True, grace=2, children=False):
        self.folder.mkdir(parents=True, exist_ok=True)
        worker = self.folder / ('worker-' + uuid.uuid4().hex + '.py')
        worker.write_text('import sys,os,time,json,subprocess\n'
            f'sys.path.insert(0,{str(Path(cc.__file__).parent)!r})\nimport compute_gate\n'
            + ('child=subprocess.Popen([sys.executable,"-c","import time;time.sleep(60)"])\n' if children else '')
            + f'open({str(self.folder / "pids.json")!r},"w").write(json.dumps([os.getpid()'
            + (',child.pid' if children else '') + ']))\n'
            + ('while compute_gate.permitted():time.sleep(.02)\n' if cooperative else 'time.sleep(60)\n')
            + f'open({str(self.folder / "boundary.txt")!r},"w").write("safe boundary")\n')
        process = subprocess.Popen([sys.executable, '-B', str(Path(cc.__file__)), '--state-dir',
            str(self.control.directory), 'run', '--name', 'integration simulation', '--grace-seconds', str(grace),
            '--resume-note', 'Simulation; no training state', '--log', str(self.folder / 'worker.log'),
            '--', sys.executable, '-B', str(worker)], stdout=subprocess.DEVNULL, stderr=subprocess.PIPE, text=True)
        self.procs.append(process)
        deadline=time.monotonic()+10
        while not (self.folder / 'pids.json').exists():
            if process.poll() is not None: self.fail(process.stderr.read())
            if time.monotonic()>deadline:self.fail('Worker did not start')
            time.sleep(.02)
        return process, json.loads((self.folder / 'pids.json').read_text())

    def assert_stopped(self, process, pids, start):
        process.wait(timeout=10)
        state = self.control.status()
        self.assertTrue(state['hold_complete']); self.assertEqual(state['owned_processes'], 0)
        for pid in pids:self.assertIsNone(windows.identity(pid))
        MEASUREMENTS.append(dict(test=self._testMethodName, stop_seconds=time.monotonic()-start,
            owned_processes=state['owned_processes'], result=state['last_operation']['result']))

    def test_unknown_and_corrupt_fail_closed(self):
        self.assertFalse(self.control.status()['expensive_allowed'])
        self.assertNotEqual(self.command('check').returncode, 0)
        self.control.request('NORMAL'); self.control.path.write_text('{partial')
        self.assertNotEqual(self.command('normal').returncode, 0)
        self.assertEqual(self.control.path.read_text(), '{partial')

    def test_permission_persists_in_fresh_process_and_lightwork(self):
        self.control.request('COMPUTE_HOLD')
        before=self.control.path.read_bytes()
        self.assertNotEqual(self.command('check').returncode, 0)
        self.assertEqual(self.command('check', '--lightweight').returncode, 0)
        self.assertEqual(before,self.control.path.read_bytes())
        self.control.request('FULL_PAUSE')
        self.assertNotEqual(self.command('check', '--lightweight').returncode, 0)
        self.command('normal', check=True)
        self.assertTrue(self.control.status()['expensive_allowed'])

    def test_hold_denies_new_launch(self):
        self.control.request('COMPUTE_HOLD')
        result=self.command('run','--resume-note','test','--log',str(self.folder/'denied.log'),
                            '--',sys.executable,'-c','print(1)')
        self.assertNotEqual(result.returncode,0);self.assertFalse((self.folder/'denied.log').exists())

    def test_nonfinite_window_is_invalid_permission(self):
        self.control.request('NORMAL')
        state=self.control.read();state['expires_at']=float('nan');self.control.write(state)
        self.assertNotEqual(self.command('check').returncode,0)
        self.assertNotEqual(self.command('normal').returncode,0)

    def test_data_fork_requires_label_and_preserves_optimizer_recipe(self):
        original=dict(data_sha256='old',probes_sha256='probes',seed=1,batch_size=1024,
                      batches_per_superbatch=1024,learning_rate=.001,wdl=0.)
        self.assertTrue(cc.initial_resume_compatible(original,original))
        changed=dict(original,data_sha256='new')
        self.assertFalse(cc.initial_resume_compatible(changed,original))
        changed['data_fork_reason']='E003 prospectively declared coverage expansion'
        self.assertTrue(cc.initial_resume_compatible(changed,original))
        for key,value in [('seed',2),('batch_size',512),('batches_per_superbatch',512),
                          ('learning_rate',.002),('wdl',.5)]:
            self.assertFalse(cc.initial_resume_compatible(dict(changed,**{key:value}),original),key)
        self.assertFalse(cc.initial_resume_compatible(dict(changed,data_fork_reason=''),original))

    def test_cooperative_hold_and_repeat(self):
        self.control.request('NORMAL');process,pids=self.launch()
        started=time.monotonic();a=self.control.request('COMPUTE_HOLD')
        stamp=a['active']['stop_requested_at']
        b=self.control.request('COMPUTE_HOLD')
        if b['active']:self.assertEqual(stamp,b['active']['stop_requested_at'])
        self.assert_stopped(process,pids,started)
        self.assertTrue((self.folder/'boundary.txt').exists())

    def test_pause_cleans_children_without_stopping_unrelated(self):
        unrelated=subprocess.Popen([sys.executable,'-c','import time;time.sleep(60)'])
        self.procs.append(unrelated)
        self.control.request('NORMAL');process,pids=self.launch(cooperative=False,children=True,grace=1)
        started=time.monotonic();self.control.request('FULL_PAUSE')
        self.assert_stopped(process,pids,started)
        self.assertIsNone(unrelated.poll())
        self.assertFalse(self.control.status()['lightweight_allowed'])

    def test_emergency_stop(self):
        self.control.request('NORMAL');process,pids=self.launch(cooperative=False,children=True)
        started=time.monotonic();self.control.request('COMPUTE_HOLD',now=True)
        self.assert_stopped(process,pids,started)
        self.assertFalse((self.folder/'boundary.txt').exists())

    def test_supervisor_crash_kills_owned_tree_and_reconciles(self):
        self.control.request('NORMAL');process,pids=self.launch(cooperative=False,children=True)
        started=time.monotonic();process.kill();process.wait(timeout=5)
        deadline=time.monotonic()+5
        while any(windows.identity(pid) for pid in pids):
            if time.monotonic()>deadline:self.fail('Job leaked after supervisor crash')
            time.sleep(.02)
        self.assert_stopped(process,pids,started)
        self.assertEqual(self.control.status()['last_operation']['result'],'interrupted-supervisor')
        self.assertFalse(self.control.status()['expensive_allowed'])

    def test_crash_inside_createprocess_does_not_orphan_suspended_child(self):
        self.folder.mkdir(parents=True)
        witness=self.folder/'created.json';job_name='Local\\SeedNNUE-'+uuid.uuid4().hex
        script=self.folder/'crash.py'
        script.write_text('import sys,os,json,ctypes\n'
            f'sys.path.insert(0,{str(Path(cc.__file__).parent)!r})\nimport windows_job as w\n'
            'original=w.create_process\n'
            'def crash(*args):\n'
            '    result=original(*args)\n'
            '    pi=ctypes.cast(args[-1],ctypes.POINTER(w.ProcessInfo)).contents\n'
            f'    open({str(witness)!r},"w").write(json.dumps(dict(pid=pi.pid,result=result)))\n'
            '    os._exit(99)\n'
            'w.create_process=crash\n'
            f'job=w.Job({job_name!r})\n'
            f'job.start([sys.executable,"-c","import time;time.sleep(60)"],{str(self.folder/"crash.log")!r},{str(cc.ROOT)!r},dict(os.environ))\n')
        process=subprocess.Popen([sys.executable,'-B',str(script)]);self.procs.append(process)
        self.assertEqual(process.wait(timeout=10),99)
        result=json.loads(witness.read_text());self.assertTrue(result['result'])
        deadline=time.monotonic()+5
        while windows.identity(result['pid']):
            if time.monotonic()>deadline:self.fail('Suspended child escaped atomic job assignment')
            time.sleep(.02)
        self.assertEqual(windows.inspect(job_name),0)

    def test_window_expiry_enforced_while_busy(self):
        self.control.request('NORMAL',minutes=.02)
        process,pids=self.launch()
        started=time.monotonic()
        self.assert_stopped(process,pids,started)
        self.assertEqual(self.control.status()['mode'],'COMPUTE_HOLD')

    def test_normal_does_not_cancel_latched_stop(self):
        self.control.request('NORMAL');process,pids=self.launch(cooperative=False,grace=1)
        self.control.request('COMPUTE_HOLD');self.control.request('NORMAL')
        process.wait(timeout=10)
        self.assertEqual(self.control.status()['last_operation']['result'],'held')
        self.assertTrue(self.control.status()['expensive_allowed'])
        for pid in pids:self.assertIsNone(windows.identity(pid))

    def test_competing_operation_denied_and_failure_cleared(self):
        self.control.request('NORMAL');process,pids=self.launch()
        result=self.command('run','--resume-note','test','--log',str(self.folder/'competing.log'),
                            '--',sys.executable,'-c','print(1)')
        self.assertNotEqual(result.returncode,0)
        self.control.request('COMPUTE_HOLD');process.wait(timeout=5)
        self.control.request('NORMAL')
        result=self.command('run','--resume-note','test','--log',str(self.folder/'failed.log'),
                            '--',sys.executable,'-c','raise SystemExit(7)')
        self.assertEqual(result.returncode,7);self.assertIsNone(self.control.status()['active'])
        self.assertEqual(self.control.status()['last_operation']['result'],'failed')


if __name__=='__main__':
    result=unittest.TextTestRunner(verbosity=2).run(unittest.defaultTestLoader.loadTestsFromTestCase(Controls))
    cc.atomic(EVIDENCE/'summary.json',dict(tests=result.testsRun,failures=len(result.failures),
        errors=len(result.errors),measurements=MEASUREMENTS))
    print('Evidence:',EVIDENCE)
    sys.exit(not result.wasSuccessful())
