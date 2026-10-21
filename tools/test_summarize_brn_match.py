"""Fail-closed distinction between match and nearby diagnostic process receipts."""
import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location("summary", Path(__file__).with_name("summarize-brn-match.py"))
summary = importlib.util.module_from_spec(spec)
spec.loader.exec_module(summary)


class DiagnosticProcessTest(unittest.TestCase):
    def test_read_only_audit_receipt_requires_bounded_known_entrypoint(self):
        record = dict(args=["java.exe", "-Xmx768m", "-XX:ActiveProcessorCount=1",
                            "--add-modules=jdk.incubator.vector", "-cp", "frozen/*",
                            "com.ohinteractive.seedv6.tools.eval.BrnLineageAudit", "plan", "output"],
                      runtimeSha256="a"*64, priority="BelowNormal", heapMiB=768,
                      activeProcessors=1, exitCode=0, startedUtc="2026-10-21T07:00:00+00:00",
                      completedUtc="2026-10-21T07:00:01+00:00")
        self.assertEqual(record["args"][6], summary.diagnostic_entrypoint(record))
        for changed in ({"activeProcessors": 8}, {"args": record["args"][:6]+[
                "com.ohinteractive.seedv6.tools.eval.BrnSuccessorMatch", "plan", "output"]},
                {"args": record["args"] + ["unexpected"]}):
            with self.subTest(changed=changed), self.assertRaises(AssertionError):
                summary.diagnostic_entrypoint({**record, **changed})

    def test_bounded_gradle_qa_is_separate_and_unknown_work_fails_closed(self):
        record = dict(started="2026-10-21T01:00:00+00:00", ended="2026-10-21T01:01:00+00:00",
                      exitCode=0, args=["cmd.exe", "/d", "/c", "gradlew.bat",
                                        ":engine-core:test", "--offline", "--max-workers=1"])
        self.assertEqual("separate-bounded-gradle-qa", summary.diagnostic_entrypoint(record))
        for token in (":research:unknown", "com.ohinteractive.seedv6.tools.eval.BrnSuccessorMatch"):
            with self.subTest(token=token), self.assertRaises(AssertionError):
                summary.diagnostic_entrypoint({**record, "args": record["args"] + [token]})
        with self.assertRaises(AssertionError):
            summary.diagnostic_entrypoint({**record, "args": record["args"][:-1]})

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
