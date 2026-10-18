"""Regression checks for native process lifetime binding and retained handles."""
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch

spec=importlib.util.spec_from_file_location('cost',Path(__file__).with_name('sr024-process-cost.py'))
c=importlib.util.module_from_spec(spec);spec.loader.exec_module(c)


class CohortIdentityTest(unittest.TestCase):
    def exercise(self,extra=None,changed_birth=False):
        with tempfile.TemporaryDirectory() as temp:
            rows=[{'ProcessId':2,'Name':'java.exe','CommandLine':'-Dseedv6.research.actor=baseline','CreationTicks':200},
                  {'ProcessId':3,'Name':'java.exe','CommandLine':'-Dseedv6.research.actor=candidate','CreationTicks':210}]
            if extra:rows.insert(0,extra)
            births={1:100,2:220 if changed_birth else 200,3:210,4:extra['CreationTicks'] if extra else 50}
            active=True
            def snapshot(handle):return {'creationTicks':births[handle],'exitCode':259 if active else 0,'totalCpuSeconds':2}
            with patch.object(c.n.kernel,'OpenProcess',side_effect=lambda access,inherit,pid:pid), \
                 patch.object(c.n.kernel,'CloseHandle') as close,patch.object(c.n,'snapshot',side_effect=snapshot), \
                 patch.object(c.subprocess,'run',return_value=type('Result',(),{'stdout':json.dumps(rows)})()):
                cohort=c.Cohort(1,1,Path(temp)/'cost.json')
                error=None
                try:cohort.attach()
                except AssertionError as value:error=str(value)
                active=False
                if error is None:cohort.close()
                else:
                    with self.assertRaises(AssertionError):cohort.close()
                return error,cohort.report,[call.args[0] for call in close.call_args_list]

    def test_old_unrelated_parent_pid_lifetime_is_excluded(self):
        error,report,closed=self.exercise({'ProcessId':4,'Name':'unrelated.exe','CommandLine':'unrelated','CreationTicks':50})
        self.assertIsNone(error);self.assertEqual('passed',report['status'])
        self.assertEqual([4],[r['pid'] for r in report['excludedPriorParentPidLifetimes']])
        self.assertCountEqual([1,2,3,4],closed)
        self.assertEqual([2,3],[r['pid'] for r in report['processes']])

    def test_new_unexpected_child_still_fails_closed(self):
        error,report,closed=self.exercise({'ProcessId':4,'Name':'unrelated.exe','CommandLine':'unrelated','CreationTicks':150})
        self.assertIn('unrelated.exe',error);self.assertEqual('failed',report['status'])
        self.assertCountEqual([1,4],closed)

    def test_changed_pid_birth_is_not_attached(self):
        error,report,closed=self.exercise(changed_birth=True)
        self.assertIn('PID creation identity changed',error)
        self.assertEqual([],report['processes']);self.assertCountEqual([1,2],closed)

    def test_untagged_current_java_child_still_fails(self):
        error,report,closed=self.exercise({'ProcessId':4,'Name':'java.exe','CommandLine':'untagged','CreationTicks':150})
        self.assertIn('untagged',error);self.assertCountEqual([1,4],closed)

    def test_native_cim_birth_precision(self):
        handle=c.n.kernel.OpenProcess(0x1000|0x100000,False,os.getpid())
        self.assertTrue(handle)
        try:
            observed=c.n.snapshot(handle)
            command=f"(Get-CimInstance Win32_Process -Filter 'ProcessId={os.getpid()}').CreationDate.ToUniversalTime().ToFileTimeUtc()"
            value=subprocess.check_output(['powershell','-NoProfile','-NonInteractive','-Command',command],text=True)
            self.assertLess(abs(observed['creationTicks']-int(value.strip())),10)
        finally:c.n.kernel.CloseHandle(handle)


if __name__=='__main__':unittest.main()
