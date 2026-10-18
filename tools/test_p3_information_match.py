import importlib.util
from pathlib import Path
from types import SimpleNamespace
import unittest

spec=importlib.util.spec_from_file_location('p3_match_test',Path(__file__).with_name('p3-information-match.py'))
p=importlib.util.module_from_spec(spec);spec.loader.exec_module(p)


class InformationMatchTests(unittest.TestCase):
    def args(self,policy,workers=1):
        return SimpleNamespace(java='C:/java/jdk-21/bin/java.exe',baseline='build/research/runtime',
                               candidate='build/research/runtime',store='unused',baseline_policy=policy,
                               candidate_policy=policy,baseline_workers=workers,candidate_workers=workers,operational=True)

    def test_explicit_candidate_identity_and_shared_runner_are_selected(self):
        for policy in map(str,range(8)):
            cmd=p.command(self.args(policy),'candidate')
            self.assertIn('-Dseedv6.uci.informationResearch='+policy,cmd)
            self.assertFalse(any(x.startswith('-Dseedv6.uci.tacticalExtension=')for x in cmd))
            self.assertEqual('com.ohinteractive.seedv6.integration.P3InformationUciMain',cmd[-1])
        self.assertIs(p.m.command,p.command)

    def test_unsupported_worker_and_policy_domains_fail_before_launch(self):
        for policy,workers in [('1',4),('7',2),('8',1),('off',1)]:
            with self.assertRaises(ValueError):p.command(self.args(policy,workers),'candidate')
        for policy in ['0','4']:self.assertTrue(p.command(self.args(policy,4),'candidate'))

    def test_background_is_required(self):
        with self.assertRaises(ValueError):p.run(SimpleNamespace(background=False))


if __name__=='__main__':unittest.main()
