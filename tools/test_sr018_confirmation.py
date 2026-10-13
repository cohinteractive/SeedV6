import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location('confirmation',Path(__file__).with_name('sr018-confirmation.py'))
c = importlib.util.module_from_spec(spec); spec.loader.exec_module(c)


class ConfirmationTest(unittest.TestCase):
    def test_unadjusted_significance_does_not_qualify(self):
        r = c.decision([2]*77+[0]*51)
        self.assertGreater(r['score']-1.96*r['pairScoreStandardError'],.5)
        self.assertLess(r['primaryAdjustedScore'][0],.5)
        self.assertEqual('continue',r['decision'])
        self.assertTrue(2.63 < r['normalCriticalValue'] < 2.64)

    def test_positive_negative_and_null_decisions(self):
        self.assertEqual('positive',c.decision([2]*85+[0]*43)['decision'])
        self.assertEqual('negative',c.decision([2]*43+[0]*85)['decision'])
        self.assertEqual('futility',c.decision([0,2]*64)['decision'])
        self.assertEqual('inconclusive',c.decision([0,2]*512)['decision'])

    def test_family_spending_penalizes_additional_confirmations(self):
        pairs=[2]*79+[0]*49
        first=c.decision(pairs,1);second=c.decision(pairs,2)
        self.assertEqual(.025,first['confirmationAlpha'])
        self.assertEqual(.0125,second['confirmationAlpha'])
        self.assertEqual('positive',first['decision'])
        self.assertEqual('continue',second['decision'])
        self.assertAlmostEqual(.05,sum(c.decision([1]*128,i)['confirmationAlpha'] for i in range(1,21)),places=6)
        for i in (0,-1,1.5,True):
            with self.assertRaises(ValueError):c.decision(pairs,i)

    def test_degenerate_samples_require_nonzero_bounded_uncertainty(self):
        r = c.decision([1]*128)
        self.assertIsNone(r['primaryAdjustedScore'])
        self.assertLess(r['adjustedHoeffdingScore'][0],.5)
        self.assertGreater(r['adjustedHoeffdingScore'][1],.5)
        self.assertEqual('continue',r['decision'])
        self.assertEqual('positive',c.decision([1.5]*128)['decision'])

    def test_sparse_one_sided_outcomes_cannot_use_a_narrow_normal_interval(self):
        r = c.decision([1]*120+[1.5]*8)
        self.assertGreater(r['score']-2.5*r['pairScoreStandardError'],.5)
        self.assertFalse(r['normalEligible'])
        self.assertIsNone(r['primaryAdjustedScore'])
        self.assertEqual('continue',r['decision'])

    def test_only_prespecified_complete_looks_are_valid(self):
        for pairs in ([1]*127,[1]*129,[1]*256,[1]*512,[1]*768,[1]*1025,[.25]*128):
            with self.assertRaises(ValueError): c.decision(pairs)

    def test_futility_is_a_forecast_and_cannot_override_a_positive_result(self):
        r=c.decision([0,2]*64)
        self.assertLess(r['plugInConditionalPowerAtMaximum'],.10)
        self.assertEqual('futility',r['decision'])
        self.assertEqual('positive',c.decision([2]*85+[0]*43)['decision'])
        self.assertEqual('inconclusive',c.decision([0,2]*512)['decision'])

    def test_cumulative_pair_statistics_preserve_cluster_variance(self):
        r = c.decision([0,2]*192)
        self.assertEqual(.5,r['score'])
        self.assertAlmostEqual(96/383,r['pairSampleVariance'])
        self.assertEqual([192,0,0,0,192],r['pentanomial'])


if __name__ == '__main__': unittest.main()
