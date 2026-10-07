import importlib.util
from pathlib import Path
import unittest

spec=importlib.util.spec_from_file_location('scaling',Path(__file__).with_name('analyze-brn-scaling.py'))
scaling=importlib.util.module_from_spec(spec);spec.loader.exec_module(scaling)


class PairedScalingTest(unittest.TestCase):
    def test_paired_cancellation_retains_shared_opening_variability(self):
        rows={'a':{1:0,2:1,3:.25},'b':{1:1,2:0,3:.75}}
        r=scaling.paired_difference(rows,rows,[1,2,3],1000)
        self.assertTrue(r['complete'])
        self.assertEqual([0,0],r['initializationAndOpeningBootstrap95'])
        self.assertEqual([0,0],r['allPlannedDifferenceBounds'])

    def test_uniform_maximum_improvement(self):
        a={'a':{1:0,2:0},'b':{1:0,2:0}}
        b={'a':{1:1,2:1},'b':{1:1,2:1}}
        r=scaling.paired_difference(a,b,[1,2],1000)
        self.assertEqual([1,1],r['initializationAndOpeningBootstrap95'])
        self.assertEqual(1,r['completeMatchedPointDifference'])

    def test_opposing_seed_effects_are_not_opening_replications(self):
        a={'a':{1:0,2:0},'b':{1:1,2:1}}
        b={'a':{1:1,2:1},'b':{1:0,2:0}}
        r=scaling.paired_difference(a,b,[1,2],1000)
        self.assertEqual([0,0],r['openingClusterBootstrap95'])
        self.assertEqual([-1,1],r['initializationAndOpeningBootstrap95'])

    def test_missing_outcomes_remain_adverse(self):
        a={'a':{1:1,2:None},'b':{1:0,2:None}}
        b={'a':{1:None,2:None},'b':{1:1,2:.25}}
        r=scaling.paired_difference(a,b,[1,2],1000)
        self.assertFalse(r['complete'])
        self.assertEqual(1,r['completeMatchedPairs'])
        self.assertEqual(0,r['completeOpeningClusters'])
        self.assertEqual([-1.75/4,2.25/4],r['allPlannedDifferenceBounds'])
        self.assertNotIn('initializationAndOpeningBootstrap95',r)

    def test_rejects_corrupt_domains_and_unpaired_samples(self):
        for a,b,indices in [({'a':{1:float('nan')}},{'a':{1:0}},[1]),
                             ({'a':{1:0}},{'b':{1:0}},[1]),
                             ({'a':{2:0}},{'a':{1:0}},[1]),
                             ({'a':{1:0}},{'a':{1:0}},[1,1])]:
            with self.assertRaises(ValueError):scaling.paired_difference(a,b,indices,100)


if __name__=='__main__':unittest.main()
