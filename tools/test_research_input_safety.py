"""Deleted research inputs must not turn into replacement evidence or output trees."""
import json
import importlib.util
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

TOOLS = Path(__file__).resolve().parent


class ResearchInputSafetyTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)

    def run_tool(self, tool, *args):
        return subprocess.run([sys.executable, '-B', str(TOOLS / tool), *map(str, args)],
                              cwd=self.root, capture_output=True, text=True, timeout=15)

    def test_missing_or_empty_source_never_replaces_evidence(self):
        output = self.root / 'evidence.json'
        output.write_bytes(b'important existing evidence\r\n')
        missing = self.root / 'missing'
        empty = self.root / 'empty'
        empty.mkdir()
        for source in (missing, empty):
            for options in ((), ('--allow-empty',)):
                result = self.run_tool('summarize-brn-architecture.py', source, output, *options)
                self.assertNotEqual(0, result.returncode)
                self.assertEqual(b'important existing evidence\r\n', output.read_bytes())

    def test_empty_requires_explicit_intent_and_existing_directory(self):
        output = self.root / 'new' / 'evidence.json'
        source = self.root / 'source'
        result = self.run_tool('summarize-brn-architecture.py', source, output, '--allow-empty')
        self.assertNotEqual(0, result.returncode)
        self.assertFalse(output.parent.exists())
        source.mkdir()
        result = self.run_tool('summarize-brn-architecture.py', source, output)
        self.assertIn('No execution receipts', result.stderr)
        self.assertFalse(output.parent.exists())
        result = self.run_tool('summarize-brn-architecture.py', source, output, '--allow-empty')
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual([], json.loads(output.read_text())['runs'])
        self.assertEqual([output], list(output.parent.iterdir()))

    def test_valid_receipt_and_malformed_input_are_transactional(self):
        source = self.root / 'source'
        source.mkdir()
        receipt = source / 'sample.execution.json'
        receipt.write_text(json.dumps({'status': 'failed', 'command': ['example'],
                                      'sourceSha256': {}, 'compiledSha256': {}}))
        output = self.root / 'evidence.json'
        result = self.run_tool('summarize-brn-architecture.py', source, output, '--compact')
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual('failed', json.loads(output.read_text())['runs'][0]['status'])
        before = output.read_bytes()
        receipt.write_text('{broken')
        result = self.run_tool('summarize-brn-architecture.py', source, output)
        self.assertNotEqual(0, result.returncode)
        self.assertEqual(before, output.read_bytes())
        fresh = self.root / 'new' / 'evidence.json'
        result = self.run_tool('summarize-brn-architecture.py', source, fresh)
        self.assertNotEqual(0, result.returncode)
        self.assertFalse(fresh.parent.exists())

    def test_prepare_requires_explicit_navigation_without_creating_output(self):
        source = self.root / 'source.jsonl'
        source.write_text('{}\n')
        output = self.root / 'new' / 'dataset'
        for options in ((), ('--raw-start', '-1'), ('--raw-start', '100'),
                        ('--raw-start', '100', '--navigation', str(self.root / 'missing'))):
            result = self.run_tool('brn-successor.py', 'prepare', source, output, *options)
            self.assertNotEqual(0, result.returncode)
            self.assertIn('error:', result.stderr)
            self.assertFalse(output.parent.exists())

    def test_concurrent_evidence_publication_cannot_be_replaced(self):
        source = self.root / 'source'
        source.mkdir()
        output = self.root / 'evidence.json'
        spec = importlib.util.spec_from_file_location('collector', TOOLS / 'summarize-brn-architecture.py')
        collector = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(collector)
        def race(temporary, destination):
            self.assertTrue(Path(temporary).read_text().endswith('\n'))
            Path(destination).write_bytes(b'concurrent evidence')
            raise FileExistsError(destination)
        with patch.object(sys, 'argv', ['collector', str(source), str(output), '--allow-empty']), \
                patch.object(collector.os, 'link', side_effect=race):
            with self.assertRaises(FileExistsError):
                collector.main()
        self.assertEqual(b'concurrent evidence', output.read_bytes())
        self.assertEqual({source, output}, set(self.root.iterdir()))

    def test_unrelated_mode_has_no_navigation_requirement(self):
        result = self.run_tool('brn-successor.py', 'runtime', 'data', self.root / 'output')
        self.assertIn('--model required', result.stderr)
        self.assertNotIn('navigation', result.stderr.split('error:')[-1])

    def test_learning_reports_fail_clearly_and_historical_export_is_retired(self):
        source = self.root / 'source'
        source.mkdir()
        result = self.run_tool('analyze-brn-learning.py', source)
        self.assertIn('No match.json inputs', result.stderr)
        output = self.root / 'export'
        result = self.run_tool('analyze-brn-learning.py', source, '--export', output)
        self.assertNotEqual(0, result.returncode)
        self.assertFalse(output.exists())


if __name__ == '__main__':
    unittest.main()
