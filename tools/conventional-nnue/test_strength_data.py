"""Small binary mixture fixtures; run under the ordinary compute supervisor."""
import contextlib
import io
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import mix_strength_data as mix


class Mixture(unittest.TestCase):
    def source(self, root, unit, values):
        root.mkdir()
        for name,size in [('train.data',32),('train-provenance.bin',12)]:
            (root/name).write_bytes(b''.join(bytes([v])*size*4096 for v in values))
        (root/'validation.txt').write_text('unchanged validation\n')
        d=dict(prepared_identity='fixture',train=len(values)*4096,validation=1,units=[dict(index=unit)],
               hashes={n:mix.sha(root/n) for n in ('train.data','train-provenance.bin','validation.txt')})
        (root/'manifest.json').write_text(json.dumps(d))

    def test_batches_and_provenance_are_aligned_and_sources_untouched(self):
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder);left=root/'left';right=root/'right';out=root/'out'
            self.source(left,0,[11,12]);self.source(right,1,[21,22])
            before={str(p):mix.sha(p) for s in (left,right) for p in s.iterdir()}
            with patch('sys.argv',['mix',str(left),str(right),str(out)]),contextlib.redirect_stdout(io.StringIO()):mix.main()
            for name,size in [('train.data',32),('train-provenance.bin',12)]:
                self.assertEqual((out/name).read_bytes(),b''.join(bytes([v])*size*4096 for v in (11,21,12,22)))
            self.assertEqual(before,{str(p):mix.sha(p) for s in (left,right) for p in s.iterdir()})
            self.assertEqual(json.loads((out/'manifest.json').read_text())['train'],16384)

    def test_chunk_overlap_refuses_before_output(self):
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder);left=root/'left';right=root/'right';out=root/'out'
            self.source(left,0,[11]);self.source(right,0,[21])
            with patch('sys.argv',['mix',str(left),str(right),str(out)]):
                with self.assertRaisesRegex(ValueError,'chunk overlap'):mix.main()
            self.assertFalse(out.exists())


if __name__=='__main__':unittest.main()
