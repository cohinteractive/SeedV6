"""Boundary tests for confirmation continuation; no engine experiments."""
import importlib.util
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('confirmation', Path(__file__).with_name('sr032-confirmation.py'))
c = importlib.util.module_from_spec(spec); spec.loader.exec_module(c)


class ContinuationTest(unittest.TestCase):
    def test_pause_does_not_assess_or_export(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp); protocol = root/'protocol.json'; protocol.write_text('{}')
            p = {'execution':str(root/'execution'), 'stages':[{'plan':'unused', 'export':'unused'}]}
            with patch.object(c, 'verify', return_value=p), patch.object(c, 'command', return_value={'status':'paused'}) as command, patch.object(c, 'assess') as assess:
                self.assertEqual('paused', c.run(protocol, 'future')['status'])
                assess.assert_not_called(); self.assertEqual(1, command.call_count)

    def test_stopping_decision_prevents_next_stage(self):
        for decision in ('positive', 'negative', 'futility', 'inconclusive'):
            with self.subTest(decision=decision), tempfile.TemporaryDirectory() as temp:
                root = Path(temp); protocol = root/'protocol.json'; protocol.write_text('{}')
                export = root/'export'; export.mkdir()
                p = {'execution':str(root/'execution'), 'stages':[{'plan':'first', 'export':str(export)}, {'plan':'forbidden'}]}
                result = {'finalDecision':decision, 'looks':[{'decision':decision}]}
                with patch.object(c, 'verify', return_value=p), patch.object(c, 'command', return_value={'status':'complete'}) as command, patch.object(c, 'assess', return_value=result):
                    self.assertEqual('closed', c.run(protocol, 'future')['status'])
                    self.assertEqual(1, command.call_count)
                    command.reset_mock()
                    self.assertEqual('closed', c.run(protocol, 'future')['status'])
                    command.assert_not_called()

    def test_changed_stored_assessment_blocks(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp); protocol = root/'protocol.json'; protocol.write_text('{}')
            out = root/'execution'; out.mkdir(); c.t.save(out/'assessment-1.json', {'different':True})
            p = {'execution':str(out), 'stages':[{}]}
            with patch.object(c, 'verify', return_value=p), patch.object(c, 'command') as command, patch.object(c, 'assess', return_value={'finalDecision':'futility'}):
                with self.assertRaisesRegex(ValueError, 'Stored assessment changed'): c.run(protocol, 'future')
                command.assert_not_called()
                self.assertEqual('blocked', c.t.read(out/'execution.json')['status'])

    def test_scheduled_first_look_does_not_launch_next_stage(self):
        with tempfile.TemporaryDirectory() as temp:
            root=Path(temp);protocol=root/'protocol.json';protocol.write_text('{}')
            export=root/'export';export.mkdir()
            p={'execution':str(root/'execution'),'stages':[{'plan':'first','export':str(export)},{'plan':'forbidden'}]}
            result={'finalDecision':'continue','looks':[{'decision':'continue'}]}
            with patch.object(c,'verify',return_value=p),patch.object(c,'command',return_value={'status':'complete'}) as command,patch.object(c,'assess',return_value=result):
                self.assertEqual('paused_after_scheduled_look',c.run(protocol,'future',1)['status'])
                self.assertEqual(1,command.call_count)
        with self.assertRaisesRegex(ValueError,'Invalid scheduled-stage'):
            c.run('unused','unused',0)

    def test_prospective_rule_has_no_small_gain_claim(self):
        for index in (1, 2):
            result = c.rules.decision([0]*32+[2]*32+[1]*64, index, [128,512])
            self.assertEqual('futility', result['decision'])
            self.assertLess(result['primaryAdjustedScore'][0], .5)
            self.assertGreater(result['primaryAdjustedScore'][1], .5)


if __name__ == '__main__': unittest.main()
