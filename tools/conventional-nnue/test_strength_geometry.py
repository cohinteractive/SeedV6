"""Geometry/export/retention contract tests; synthetic weights, no GPU needed."""
import array
import hashlib
import json
from pathlib import Path
import struct
import sys
import tempfile
import unittest
import compute_control as cc
import export_network as export
import training_lifecycle as lifecycle


class StrengthGeometry(unittest.TestCase):
    def fixture(self, root, hidden):
        config = dict(hidden=hidden, output_buckets=8)
        (root/'config.json').write_text(json.dumps(config))
        checkpoint=root/'seed-cnnue-v1-1';(checkpoint/'optimiser_state').mkdir(parents=True)
        tensors={'l0w':7680*hidden,'l0f':768*hidden,'l0b':hidden,'l1w':2*hidden*8,'l1b':8}
        data=b''.join(name.encode()+b'\n'+struct.pack('<Q',count)+bytes(count*4) for name,count in tensors.items())
        for name in ('weights','momentum','velocity'):(checkpoint/f'optimiser_state/{name}.bin').write_bytes(data)
        payload=bytes(((7680+1)*hidden+(2*hidden+1)*8)*2)
        (checkpoint/'raw.bin').write_bytes(bytes(len(payload)*2))
        padding=(b'bullet'*11)[:(-len(payload))%64]
        (checkpoint/'quantised.bin').write_bytes(payload+padding)
        receipt=dict(schema='seed-bullet-checkpoint-v1',bullet=cc.PIN,completed_superbatch=1,
                     config_sha256=cc.sha(root/'config.json'),files={n:cc.sha(checkpoint/n) for n in cc.FILES})
        (checkpoint/'seed-manifest.json').write_text(json.dumps(receipt))
        version=3 if hidden==512 else 2
        network=root/'network.cnnue'
        network.write_bytes(f'S6CNU00{version}'.encode()+struct.pack('<8I',version,7680,hidden,10,255,64,400,len(payload))+
                            hashlib.sha256(payload).digest()+payload)
        return checkpoint,network,payload

    def test_both_geometries_have_matching_export_and_resume_contracts(self):
        for hidden in (256,512):
            with self.subTest(hidden=hidden),tempfile.TemporaryDirectory() as folder:
                checkpoint,network,payload=self.fixture(Path(folder),hidden)
                cc.verify_checkpoint(checkpoint);lifecycle.verify_nnue_checkpoint(checkpoint)
                lifecycle.verify_model(network,'nnue')
                floats=export.verify_quantization(payload,checkpoint/'optimiser_state/weights.bin')
                self.assertEqual(len(floats)*2,len(payload))
                self.assertEqual(export.evaluate(array.array('h',payload),export.chess.Board(),hidden)['score'],0)
                # New metadata must not let a different geometry reinterpret the same bytes.
                config=checkpoint.parent/'config.json';config.write_text(json.dumps(dict(hidden=512 if hidden==256 else 256,output_buckets=8)))
                with self.assertRaises(ValueError):cc.verify_checkpoint(checkpoint)

    def test_old_recipes_default_to_256_but_width_cannot_change_on_data_fork(self):
        old=dict(data_sha256='a',probes_sha256='b',seed=1,batch_size=4096,batches_per_superbatch=1024,learning_rate=.001,wdl=0,output_buckets=8)
        self.assertEqual(cc.recipe(old),cc.recipe(dict(old,hidden=256)))
        self.assertFalse(cc.initial_resume_compatible(dict(old,hidden=512,data_fork_reason='New data cannot change width'),old))


if __name__=='__main__':unittest.main()
