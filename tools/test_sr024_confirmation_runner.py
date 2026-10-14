"""No-engine checks of frozen-input, stopping and protocol-mutation gates."""
import hashlib
import importlib.util
import json
from pathlib import Path
import shutil
import tempfile
from types import SimpleNamespace
import unittest
from unittest.mock import Mock,patch

spec=importlib.util.spec_from_file_location('runner',Path(__file__).with_name('run-sr024-confirmation.py'))
r=importlib.util.module_from_spec(spec);spec.loader.exec_module(r)


class RunnerTest(unittest.TestCase):
    def setUp(self):
        self.build=Path('app/build').resolve();self.root=Path(tempfile.mkdtemp(prefix='sr024-confirmation-test-',dir=self.build)).resolve()
        self.addCleanup(self.cleanup)
        self.runner=self.root/'runner.py';self.assessor=self.root/'assessor.py'
        self.runner.write_text('frozen runner');self.assessor.write_text('frozen assessor')
        self.digest=lambda p:hashlib.sha256(Path(p).read_bytes()).hexdigest()
        self.save=lambda p,d:Path(p).write_text(json.dumps(d))
        self.protocol=self.root/'protocol.json';self.out=self.root/'out'
        self.save(self.protocol,{'executorSha256':self.digest(r.__file__),'assessorSha256':self.digest(self.assessor),
            'runnerModule':str(self.runner),'assessorModule':str(self.assessor),
            'frozenFiles':{str(p):self.digest(p) for p in (self.runner,self.assessor)},'expectedArguments':{},
            'stages':[{'openings':'unused','pairs':1} for _ in range(3)]})

    def cleanup(self):
        assert self.root.resolve().is_relative_to(self.build) and self.root.name.startswith('sr024-confirmation-test-')
        shutil.rmtree(self.root)

    def modules(self,decisions,action=None):
        self.calls=Mock(side_effect=action);self.audit=Mock(side_effect=[{'finalDecision':x,'looks':[{'decision':x}]} for x in decisions])
        engine=SimpleNamespace(b=SimpleNamespace(digest=self.digest,save=self.save),run=self.calls)
        return patch.object(r,'module',side_effect=[engine,SimpleNamespace(assess=self.audit)])

    def test_positive_stops_without_later_stage(self):
        with self.modules(['positive']):r.run(self.protocol,self.out)
        self.assertEqual(1,self.calls.call_count)
        self.assertEqual('closed',json.loads((self.out/'execution.json').read_text())['status'])

    def test_futility_after_continue_stops_without_extension(self):
        with self.modules(['continue','futility']):r.run(self.protocol,self.out)
        self.assertEqual(2,self.calls.call_count)

    def test_bad_digest_is_rejected_before_import_or_launch(self):
        self.runner.write_text('changed')
        with self.modules([]),self.assertRaises(AssertionError):r.run(self.protocol,self.out)
        self.assertEqual(0,self.calls.call_count);self.assertFalse(self.out.exists())

    def test_protocol_change_during_run_blocks_assessment(self):
        def change(_):self.protocol.write_text(self.protocol.read_text()+' ')
        with self.modules([],change),self.assertRaises(AssertionError):r.run(self.protocol,self.out)
        self.assertEqual(1,self.calls.call_count);self.assertEqual(0,self.audit.call_count)
        self.assertEqual('failed',json.loads((self.out/'execution.json').read_text())['status'])

    def test_failed_stage_never_advances(self):
        with self.modules([],RuntimeError('fixture failure')),self.assertRaises(RuntimeError):r.run(self.protocol,self.out)
        self.assertEqual(1,self.calls.call_count);self.assertEqual(0,self.audit.call_count)


if __name__=='__main__':unittest.main()
