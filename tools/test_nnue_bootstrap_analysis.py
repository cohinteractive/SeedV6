"""Scientific accounting checks; no engine or external dependencies."""
import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location("analysis", Path(__file__).with_name("analyze-nnue-bootstrap.py"))
analysis = importlib.util.module_from_spec(spec)
spec.loader.exec_module(analysis)


class ConfirmationTest(unittest.TestCase):
    def design(self, score):
        variants = [f"v{i}" for i in range(8)]
        seeds = list(range(32))
        meta = {"args": ["out", "confirm", ",".join(variants), "seeds", "2", "3", "2048"], "seeds": seeds}
        records = [dict(variant=v, seed=s, index=2*s+i, openingHash=f"opening-{2*s+i}",
                        first={"nnueScore": score}, second={"nnueScore": score})
                   for v in variants for s in seeds for i in range(2)]
        return records, meta

    def test_complete_negative_design_has_conservative_simultaneous_bound(self):
        records, meta = self.design(0)
        result = analysis.confirmation(records, meta)
        self.assertTrue(result["completeIndependentDesign"])
        self.assertTrue(result["allTestedArmsBelow045"])
        self.assertGreater(result["hoeffdingRadius"], .281)
        self.assertLess(result["hoeffdingRadius"], .282)

    def test_administrative_terminations_cannot_support_negative_result(self):
        records, meta = self.design(None)
        result = analysis.confirmation(records, meta)
        self.assertFalse(result["allTestedArmsBelow045"])
        self.assertEqual(1, result["variants"]["v0"]["simultaneous95Upper"])

    def test_incomplete_duplicate_or_shared_openings_do_not_create_confidence(self):
        records, meta = self.design(0)
        self.assertFalse(analysis.confirmation(records[:-1], meta)["completeIndependentDesign"])
        self.assertFalse(analysis.confirmation(records[:-1]+[records[0]], meta)["completeIndependentDesign"])
        for r in records:
            r["index"] %= 2
            r["openingHash"] = f"shared-{r['index']}"
        self.assertFalse(analysis.confirmation(records, meta)["completeIndependentDesign"])

    def test_mismatched_openings_between_architectures_are_rejected(self):
        records, meta = self.design(0)
        records[-1]["openingHash"] = "foreign-opening"
        with self.assertRaises(ValueError):
            analysis.confirmation(records, meta)

    def test_prior_ablation_requires_matched_games_and_conservatively_counts_caps(self):
        def row(seed, a, b):
            return dict(seed=seed,index=seed,openingHash=str(seed),
                        first=dict(nnueColor=0,nnueScore=a),second=dict(nnueColor=1,nnueScore=b))
        old=[row(i,0,0) for i in range(32)]
        new=[row(i,1,None) for i in range(32)]
        result=analysis.parity_comparison(old,new,list(range(32)))
        self.assertEqual([.5,1],result["scoreImprovementBounds"])
        self.assertGreater(result["conservativeOneSided95ImprovementLower"],0)
        self.assertFalse(analysis.parity_comparison(old,new[:-1],list(range(32)))["completePairedDesign"])
        new[-1]["openingHash"]="wrong"
        with self.assertRaises(ValueError): analysis.parity_comparison(old,new,list(range(32)))


if __name__ == "__main__":
    unittest.main()
