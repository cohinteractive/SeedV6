"""Fail-closed distinction between match and nearby diagnostic process receipts."""
import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location("summary", Path(__file__).with_name("summarize-brn-match.py"))
summary = importlib.util.module_from_spec(spec)
spec.loader.exec_module(summary)


class DiagnosticProcessTest(unittest.TestCase):
    def test_known_diagnostic_is_classified_without_claiming_it_is_a_match(self):
        entry = "com.ohinteractive.seedv6.tools.eval.BrnHybridProbe"
        self.assertEqual(entry, summary.diagnostic_entrypoint(dict(arguments=["-Xmx768m", entry, "plan.json"], exitCode=0)))

    def test_match_unknown_and_malformed_receipts_cannot_hide_as_diagnostics(self):
        for record in ({}, dict(arguments=[], exitCode=0),
                       dict(arguments=["com.ohinteractive.seedv6.tools.eval.BrnSuccessorMatch"], exitCode=0),
                       dict(arguments=["com.ohinteractive.seedv6.tools.eval.Unknown"], exitCode=0),
                       dict(arguments=["com.ohinteractive.seedv6.tools.eval.BrnHybridProbe"])):
            with self.subTest(record=record), self.assertRaises(AssertionError):
                summary.diagnostic_entrypoint(record)


if __name__ == "__main__":
    unittest.main()
