import unittest
from analyze_confirmation import confirmation_family_bounds


class ConfirmationStatistics(unittest.TestCase):
    def test_two_attempt_allowance_widens_both_intervals(self):
        pairs=[0,.25,.5,.5,.5,.75,1,.75]*4
        single=confirmation_family_bounds(pairs,1);family=confirmation_family_bounds(pairs,2)
        self.assertEqual(family['per_attempt_confidence'],.975)
        for key in ('paired_normal_score','paired_bootstrap_score'):
            self.assertLessEqual(family[key][0],single[key][0])
            self.assertGreaterEqual(family[key][1],single[key][1])

    def test_identical_pair_scores_cannot_assert_a_gain(self):
        result=confirmation_family_bounds([.5]*64,2)
        self.assertEqual(result['paired_normal_score'],[.5,.5])
        self.assertEqual(result['paired_bootstrap_score'],[.5,.5])
        self.assertEqual(result['paired_normal_elo'],[0.,0.])

    def test_invalid_or_inadequate_pairs_are_rejected(self):
        for values,family in [([.5],2),([float('nan'),.5],2),([-.1,.5],1),([.5,.5],3)]:
            with self.assertRaises(ValueError):confirmation_family_bounds(values,family)


if __name__=='__main__':unittest.main()
