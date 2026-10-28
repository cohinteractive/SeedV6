import tempfile
from pathlib import Path
import unittest

from prepare_scale import database, number, commit_candidates, MULTIPLIER, SEED


class SelectionResumeTest(unittest.TestCase):
    def test_checkpoint_reopen_duplicate_and_rollback(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / 'selection.sqlite'
            db = database(path, {'test': 1})
            commit_candidates(db, [(7, 'a', 'FENa'), (9, 'a', 'duplicate'), (10, 'b', 'FENb')], 3, 5)
            self.assertEqual((number(db, 'count'), number(db, 'cursor')), (2, 3))
            # Interrupted uncommitted next chunk is rolled back on close.
            db.execute('INSERT INTO rows VALUES (?,?,?,?)', ('lost', 2, 11, 'lost'))
            db.close()
            db = database(path, {'test': 1})
            self.assertEqual(db.execute('SELECT count(*) FROM rows').fetchone()[0], 2)
            commit_candidates(db, [(11, 'c', 'FENc'), (12, 'd', 'FENd')], 5, 3)
            self.assertEqual((number(db, 'count'), number(db, 'cursor')), (3, 5))
            self.assertEqual(db.execute('SELECT source_id FROM rows ORDER BY ordinal').fetchall(), [(7,), (10,), (11,)])
            db.close()
            with self.assertRaisesRegex(ValueError, 'binding changed'):
                database(path, {'test': 2})

    def test_permutation_has_no_replacement(self):
        for size in (32, 1024):
            self.assertEqual(len({(MULTIPLIER * i + SEED) % size for i in range(size)}), size)


if __name__ == '__main__':
    unittest.main()
