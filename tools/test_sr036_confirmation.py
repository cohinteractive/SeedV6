"""Exercise inference guards reused by the SR-036 frozen confirmation assessor."""
import importlib.util
from pathlib import Path
import unittest

spec=importlib.util.spec_from_file_location('sr036',Path(__file__).with_name('sr036-confirmation.py'))
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)


class ConfirmationTests(unittest.TestCase):
    def test_no_unplanned_look(self):
        with self.assertRaises(ValueError):m.stats.decision([1]*127)

    def test_favorable_and_unfavorable_have_symmetric_intervals(self):
        a=[2]*60+[1]*48+[0]*20
        positive=m.stats.decision(a)
        negative=m.stats.decision([2-x for x in a])
        self.assertEqual(positive['decision'],'positive')
        self.assertEqual(negative['decision'],'negative')
        self.assertAlmostEqual(positive['primaryAdjustedScore'][0],1-negative['primaryAdjustedScore'][1])

    def test_draws_cannot_manufacture_zero_uncertainty(self):
        r=m.stats.decision([1]*128)
        self.assertFalse(r['normalEligible']);self.assertIsNone(r['primaryAdjustedScore'])
        self.assertLess(r['adjustedHoeffdingScore'][0],.5)
        self.assertGreater(r['adjustedHoeffdingScore'][1],.5)
        self.assertEqual(r['decision'],'continue')

    def test_later_contrast_spends_less_alpha(self):
        values=[2]*45+[1]*40+[0]*43
        a=m.stats.decision(values,1);b=m.stats.decision(values,2)
        self.assertLess(b['confirmationAlpha'],a['confirmationAlpha'])
        self.assertLess(b['primaryAdjustedScore'][0],a['primaryAdjustedScore'][0])
        self.assertGreater(b['primaryAdjustedScore'][1],a['primaryAdjustedScore'][1])
        self.assertEqual(a['decision'],'futility')


if __name__=='__main__':unittest.main()
