import hashlib
from pathlib import Path
import tempfile
import unittest
from prepare_scale import database,commit_candidates,number
from prepare_coverage_extension import clone_parent


class CoverageExtensionTest(unittest.TestCase):
    def test_clone_preserves_parent_prefix_cursor_and_resume_binding(self):
        with tempfile.TemporaryDirectory() as directory:
            parent=Path(directory)/'parent.sqlite';child=Path(directory)/'child.sqlite'
            db=database(parent,{'parent':'identity'});commit_candidates(db,[(1,'a','first'),(2,'b','second')],4096,2);db.close()
            original=hashlib.sha256(parent.read_bytes()).digest();binding={'child':'identity'}
            db=clone_parent(parent,child,binding,2)
            self.assertEqual(4096,number(db,'cursor'));self.assertEqual([(0,1,'first'),(1,2,'second')],db.execute('SELECT ordinal,source_id,line FROM rows ORDER BY ordinal').fetchall())
            commit_candidates(db,[(1,'a','ignored'),(3,'c','third')],8192,3);db.close()
            db=clone_parent(parent,child,binding,2);self.assertEqual(3,number(db,'count'));self.assertEqual(8192,number(db,'cursor'));db.close()
            self.assertEqual(original,hashlib.sha256(parent.read_bytes()).digest())
            with self.assertRaises(ValueError):clone_parent(parent,child,{'child':'changed'},2)
            with self.assertRaises(ValueError):clone_parent(parent,Path(directory)/'bad.sqlite',binding,99)


if __name__=='__main__':unittest.main()
