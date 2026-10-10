import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location('confirmation',Path(__file__).with_name('sr022-confirmation.py'))
c = importlib.util.module_from_spec(spec); spec.loader.exec_module(c)


class ConfirmationTest(unittest.TestCase):
    def test_unadjusted_significance_does_not_qualify(self):
        r = c.decision([2]*146+[0]*110)
        self.assertGreater(r['score']-1.96*r['pairScoreStandardError'],.5)
        self.assertLess(r['primaryAdjustedScore'][0],.5)
        self.assertEqual('continue',r['decision'])
        self.assertTrue(2.39 < r['normalCriticalValue'] < 2.40)

    def test_positive_negative_and_null_decisions(self):
        self.assertEqual('positive',c.decision([2]*160+[0]*96)['decision'])
        self.assertEqual('negative',c.decision([2]*96+[0]*160)['decision'])
        self.assertEqual('continue',c.decision([0,2]*128)['decision'])
        self.assertEqual('inconclusive',c.decision([0,2]*512)['decision'])

    def test_wider_sensitivity_is_a_required_gate(self):
        r = c.decision([2]*147+[0]*109)
        self.assertGreater(r['primaryAdjustedScore'][0],.5)
        self.assertLess(r['required2p5SeScore'][0],.5)
        self.assertEqual('continue',r['decision'])

    def test_degenerate_samples_require_nonzero_bounded_uncertainty(self):
        r = c.decision([1]*256)
        self.assertIsNone(r['primaryAdjustedScore'])
        self.assertLess(r['adjustedHoeffdingScore'][0],.5)
        self.assertGreater(r['adjustedHoeffdingScore'][1],.5)
        self.assertEqual('continue',r['decision'])
        self.assertEqual('positive',c.decision([1.5]*256)['decision'])

    def test_sparse_one_sided_outcomes_cannot_use_a_narrow_normal_interval(self):
        r = c.decision([1]*248+[1.5]*8)
        self.assertGreater(r['score']-2.5*r['pairScoreStandardError'],.5)
        self.assertFalse(r['normalEligible'])
        self.assertIsNone(r['primaryAdjustedScore'])
        self.assertEqual('continue',r['decision'])

    def test_only_prespecified_complete_looks_are_valid(self):
        for pairs in ([1]*255,[1]*257,[1]*768,[1]*1025,[.25]*256):
            with self.assertRaises(ValueError): c.decision(pairs)

    def test_cumulative_pair_statistics_preserve_cluster_variance(self):
        r = c.decision([0,2]*256)
        self.assertEqual(.5,r['score'])
        self.assertAlmostEqual(128/511,r['pairSampleVariance'])
        self.assertEqual([256,0,0,0,256],r['pentanomial'])


if __name__ == '__main__': unittest.main()
