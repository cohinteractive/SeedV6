"""Known arithmetic at128/256 and byte-identical re-export of frozen256 controls."""
import argparse
import array
import json
from pathlib import Path
import chess
from bootstrap import BASE, write, sha
import codec
import compute_gate


def main():
    p=argparse.ArgumentParser();p.add_argument('output',type=Path);a=p.parse_args();compute_gate.require();a.output.mkdir(exist_ok=False)
    checked=[]
    for width in (128,256):
        for kind in (0,1,2):
            count=7680+(4560 if kind==2 else 0)
            w=array.array('h',[0])*(count*width);bias=array.array('h',[255])*width
            head=array.array('h',[0])*(2*width*8)
            for bucket in range(8):head[bucket*2*width]=64
            parts=[w,bias,head,array.array('h',[0])*8]
            if kind==1:
                downstream=array.array('h',[0])*(2*width*16);downstream[0]=64
                residual=array.array('h',[0])*128
                for bucket in range(8):residual[bucket*16]=64
                parts.extend([downstream,array.array('h',[0])*16,residual,array.array('h',[0])*8])
            expected=800 if kind==1 else 400
            for fen in (chess.STARTING_FEN,'4k3/8/8/3pP3/8/8/8/4K3 b - - 0 1'):
                assert codec.evaluate(parts,chess.Board(fen),kind,True)['score']==expected
            checked.append(dict(hidden=width,kind=kind,known_link_score=expected))
    regressions=[]
    for name in ('deep16','pawnpairs'):
        prior=json.loads((BASE/f'b-{name}-64-assessment.json').read_text());metadata=prior['metadata']
        output=a.output/(name+'.p2nnue');new=codec.export(Path(metadata['checkpoint']),output)
        assert new['network_sha256']==metadata['network_sha256'],'Existing256 wire bytes changed'
        quant=array.array('h');quant.frombytes(output.read_bytes()[76:]);parts=codec.split_flat(quant,codec.KINDS[name],256)
        rows=Path(metadata['network']).parent/'java-vector.jsonl'
        for line in rows.read_text().splitlines():
            row=json.loads(line);actual=codec.evaluate(parts,chess.Board(row['fen']),codec.KINDS[name],True)
            assert actual['score']==row['score'] and actual['accumulators']==row['accumulators']
        regressions.append(dict(architecture=name,sha256=new['network_sha256'],positions=259,integer_and_accumulators='EXACT MATCH'))
    result=dict(status='PASS',known_arithmetic=checked,frozen256_regressions=regressions)
    write(a.output/'summary.json',result);print(json.dumps(result,indent=2))


if __name__=='__main__':main()
