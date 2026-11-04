"""Analysis rejects live/incomplete samples and preserves pair-level uncertainty."""
import importlib.util
import json
import math
from pathlib import Path
import tempfile
import unittest

spec=importlib.util.spec_from_file_location('ic_analysis',Path(__file__).with_name('p4-iteration-confidence-results.py'))
a=importlib.util.module_from_spec(spec);spec.loader.exec_module(a)


class ResultsTest(unittest.TestCase):
    def test_live_receipt_cannot_be_scored(self):
        with tempfile.TemporaryDirectory(prefix='ic-analysis-') as directory:
            root=Path(directory);protocol=root/'protocol.json';protocol.write_text('{}')
            receipt=root/'receipt.json';receipt.write_text(json.dumps({'status':'running','protocol':str(protocol)}))
            with self.assertRaises(ValueError):a.analyze(receipt)
    def test_declared_statistics_use_opening_pairs_not_independent_games(self):
        result=a.m.b.paired_statistics([.5,1.5]*16)
        self.assertEqual(32,result['pairs']);self.assertEqual(.5,result['score'])
        self.assertAlmostEqual(math.sqrt(.0625/31),result['pairScoreStandardError'])
        lo,hi=result['pairedNormal95Score'];self.assertAlmostEqual(.5,(lo+hi)/2)
        self.assertGreater(hi-lo,.17)
    def test_small_or_degenerate_samples_do_not_manufacture_normal_confidence(self):
        self.assertIsNone(a.m.b.paired_statistics([1]*32)['pairedNormal95Score'])
        self.assertIsNone(a.m.b.paired_statistics([0,2])['pairedNormal95Score'])


if __name__=='__main__':unittest.main()
