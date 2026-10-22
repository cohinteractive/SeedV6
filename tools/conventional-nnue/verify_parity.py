"""Verify three independent feature implementations, integer kernels and GPU float output."""
import argparse
import json
from pathlib import Path
import chess
from export_network import checkpoint_weights, merged_weights, features, FEATURES, HIDDEN


def rows(path):
    raw=path.read_bytes()
    text=raw.decode('utf-16' if raw.startswith((b'\xff\xfe',b'\xfe\xff')) else 'utf-8-sig')
    return [json.loads(line) for line in text.splitlines() if line.strip()]


def main():
    p=argparse.ArgumentParser(description=__doc__)
    for name in ('python','java','bullet','gpu','checkpoint','output'):p.add_argument('--'+name,type=Path,required=True)
    a=p.parse_args();py,jv,bu,gpu=map(rows,(a.python,a.java,a.bullet,a.gpu))
    if not len(py)==len(jv)==len(bu)==len(gpu):raise AssertionError('Different probe counts')
    weights=merged_weights(checkpoint_weights(a.checkpoint/'optimiser_state/weights.bin'))
    bias=weights[FEATURES*HIDDEN:(FEATURES+1)*HIDDEN];head=weights[(FEATURES+1)*HIDDEN:]
    max_float=0.;max_quant=0.;squared_quant=0.
    for x,y,z,g in zip(py,jv,bu,gpu):
        assert x['fen']==y['fen']==z['fen']==g['fen']
        assert x['score']==y['score']==y['rebuilt'],x['fen']
        assert x['accumulators']==y['accumulators'],x['fen']
        assert x['features']==y['features']==[sorted(z['stm']),sorted(z['ntm'])],x['fen']
        board=chess.Board(x['fen']);value=head[-1]
        for side,color in enumerate((board.turn,not board.turn)):
            indices=features(board,color)
            for i in range(HIDDEN):
                raw=bias[i]+sum(weights[f*HIDDEN+i] for f in indices)
                value+=max(0.,min(1.,raw))**2*head[side*HIDDEN+i]
        # Independent double-precision arithmetic vs GPU float: permit accumulated
        # float/FMA error, not feature, scaling or head-order disagreement.
        err=abs(value-g['raw']);max_float=max(max_float,err)
        assert err<2e-5,(x['fen'],value,g['raw'],err)
        error=abs(x['score']-value*400);max_quant=max(max_quant,error);squared_quant+=error*error
    result=dict(status='PASS',positions=len(py),exact_features='Python / Java / pinned Bullet',
        exact_integer='Python arbitrary precision / Java incremental / Java unit-major full rebuild',
        gpu_float_max_abs_raw_error=max_float,gpu_float_tolerance=2e-5,
        quantization_max_abs_cp_error=max_quant,quantization_rms_cp_error=(squared_quant/len(py))**.5,
        limitation='Quantization error is measured separately; no strength or calibration claim')
    a.output.write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result,indent=2))


if __name__=='__main__':main()
