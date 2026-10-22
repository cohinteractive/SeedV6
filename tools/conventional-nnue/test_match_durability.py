"""Inexpensive fault checks for match evidence publication, with no engines/GPU."""
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

from paired_match import durable_text


class MatchDurabilityTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.ledger = self.root / 'games.jsonl'
        self.old = '{"game": 0}\n'
        durable_text(self.ledger, self.old)

    def test_complete_replacement_keeps_prior_games_and_exact_newlines(self):
        for count in range(2, 5):
            text = ''.join(json.dumps({'game': i})+'\n' for i in range(count))
            durable_text(self.ledger, text)
            self.assertEqual(self.ledger.read_bytes(), text.encode())
            self.assertEqual([json.loads(s)['game'] for s in self.ledger.read_text().splitlines()], list(range(count)))
        self.assertEqual(list(self.root.glob('*.pending-*')), [])

    def test_failed_flush_preserves_previous_committed_ledger(self):
        with patch('paired_match.os.fsync', side_effect=OSError('injected flush interruption')):
            with self.assertRaises(OSError):
                durable_text(self.ledger, self.old+'{"game": 1}\n')
        self.assertEqual(self.ledger.read_bytes(), self.old.encode())
        self.assertEqual(len(list(self.root.glob('games.jsonl.pending-*'))), 1)

    def test_failed_publish_preserves_old_ledger_and_pending_evidence(self):
        new = self.old+'{"game": 1}\n'
        with patch('paired_match.os.replace', side_effect=PermissionError('injected replace interruption')):
            with self.assertRaises(PermissionError):
                durable_text(self.ledger, new)
        self.assertEqual(self.ledger.read_bytes(), self.old.encode())
        pending = list(self.root.glob('games.jsonl.pending-*'))
        self.assertEqual(len(pending), 1)
        self.assertEqual(pending[0].read_bytes(), new.encode())
        durable_text(self.ledger, new)
        self.assertEqual(self.ledger.read_bytes(), new.encode())
        self.assertTrue(pending[0].exists())


if __name__ == '__main__':
    unittest.main()
