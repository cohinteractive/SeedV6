"""Synthetic prospective-rule tests, with no game outcomes or engine selection."""
import importlib.util
from pathlib import Path
import unittest

spec=importlib.util.spec_from_file_location('confirmation',Path(__file__).with_name('sr024-confirmation.py'))
c=importlib.util.module_from_spec(spec);spec.loader.exec_module(c)


class RuleTest(unittest.TestCase):
    def test_positive_negative_and_nonbinding_futility(self):
        looks=[128,320,768]
        positive=c.decision([2]*50+[1]*58+[0]*20,1,looks)
        self.assertEqual('positive',positive['decision']);self.assertTrue(positive['normalEligible'])
        self.assertEqual('negative',c.decision([0]*50+[1]*58+[2]*20,1,looks)['decision'])
        self.assertEqual('futility',c.decision([0]*32+[2]*32+[1]*64,1,looks)['decision'])

    def test_sparse_normal_is_not_used(self):
        a=c.decision([2]*60+[0]*4,1,[64,192,512])
        self.assertFalse(a['normalEligible']);self.assertIsNone(a['primaryAdjustedScore'])
        self.assertEqual('positive',a['decision'])
        self.assertEqual('inconclusive',c.decision([1]*512,1,[64,192,512])['decision'])

    def test_extra_looks_and_later_contrasts_spend_more_error(self):
        sample=[2]*50+[1]*58+[0]*20
        one=c.decision(sample,1,[128]);three=c.decision(sample,1,[128,320,768]);later=c.decision(sample,2,[128,320,768])
        self.assertGreater(one['primaryAdjustedScore'][0],three['primaryAdjustedScore'][0])
        self.assertGreater(three['primaryAdjustedScore'][0],later['primaryAdjustedScore'][0])
        self.assertEqual(.025/3,three['perLookAlpha'])

    def test_only_complete_valid_prospective_looks(self):
        for values,index,looks in [([1]*127,1,[128,320]),([.3]*128,1,[128]),([1]*128,0,[128]),
                                  ([1]*128,1,[320,128]),([1]*128,1,[128,128])]:
            with self.assertRaises(ValueError):c.decision(values,index,looks)


if __name__=='__main__':unittest.main()
