"""Bounded common-holdout float/quantized diagnostic; never an Elo acceptance test."""
import argparse
import array
import json
import math
from pathlib import Path
import sys
import time
import chess
from bootstrap import BASE, PRIOR, write, sha
import codec
import compute_gate


def sigmoid(score):return 1/(1+math.exp(-score/400))


def stats():
    return dict(positions=0,quantized_teacher=0.,float_teacher=0.,quantized_outcome=0.,float_outcome=0.,
                quantized_abs_link=0.,float_abs_link=0.,signed_link_error=0.,squared_link_error=0.,float_clamped=0)


def add(s,q,f,teacher,outcome,clamped):
    pq,pf=sigmoid(q),sigmoid(f);s['positions']+=1
    s['quantized_teacher']+=(pq-teacher)**2;s['float_teacher']+=(pf-teacher)**2
    s['quantized_outcome']+=(pq-outcome)**2;s['float_outcome']+=(pf-outcome)**2
    s['quantized_abs_link']+=abs(q);s['float_abs_link']+=abs(f)
    s['signed_link_error']+=q-f;s['squared_link_error']+=(q-f)**2;s['float_clamped']+=clamped


def finish(s):
    n=s['positions'];assert n>0
    result={k:(v if k in ('positions','float_clamped') else v/n) for k,v in s.items()}
    result['rms_link_error']=math.sqrt(result.pop('squared_link_error'))
    result['teacher_mse_quantization_delta']=result['quantized_teacher']-result['float_teacher']
    result['outcome_mse_quantization_delta']=result['quantized_outcome']-result['float_outcome']
    return result


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--output',type=Path,default=BASE/'raw/quantization-128');a=p.parse_args()
    compute_gate.require();started=time.monotonic();out=a.output
    if (BASE/'quantization-128.json').exists():raise ValueError('Preserve completed quantization diagnostic')
    out.mkdir(exist_ok=False)
    for source in (Path(__file__),Path(codec.__file__)):(out/source.name).write_bytes(source.read_bytes())
    validation=PRIOR/'raw/mixed-holdout/validation.txt';validation_hash=sha(validation)
    assert validation_hash=='2742eb3acdfcc0d6c45e040fd1183e48f9cd430174b2542078570fa7e3ea8271'
    positions=[]
    for line in validation.read_text().splitlines():
        fen,cp,wdl=line.split('|');board=chess.Board(fen.strip())
        teacher=sigmoid(float(cp)*(1 if board.turn else -1));outcome=float(wdl)
        if not board.turn:outcome=1-outcome
        assert outcome in (0,.5,1)
        positions.append((board,teacher,outcome,(len(board.piece_map())-2)//4))
    assert len(positions)==6753;results={}
    for name in ('base','deep16','pawnpairs'):
        evidence=BASE/f'compact128-{name}-128-assessment.json';assessment=json.loads(evidence.read_text());meta=assessment['metadata']
        assert assessment['parity']['status']=='PASS' and meta['hidden']==128 and assessment['validation_sha256']==validation_hash
        checkpoint=Path(meta['checkpoint']);config,_=codec.verify_checkpoint(checkpoint);assert config['end_superbatch']==128
        kind=codec.KINDS[name];fp=[values for values,scale in codec.merged(codec.tensors(checkpoint/'optimiser_state/weights.bin',kind,128),kind)]
        network=Path(meta['network']);assert sha(network)==meta['network_sha256']
        quant=array.array('h');quant.frombytes(network.read_bytes()[76:])
        if sys.byteorder!='little':quant.byteswap()
        qp=codec.split_flat(quant,kind,128);total=stats();groups={};errors=[];rows=out/(name+'.jsonl')
        with rows.open('x',encoding='utf-8',newline='\n') as stream:
            for i,(board,teacher,outcome,bucket) in enumerate(positions):
                if i%64==0:
                    compute_gate.require()
                    if time.monotonic()-started>600:raise RuntimeError('Quantization diagnostic budget reached')
                q=codec.evaluate(qp,board,kind,True)['score'];raw=codec.evaluate(fp,board,kind,False)['score']
                f=max(-32511.,min(32511.,raw));clamped=int(f!=raw)
                add(total,q,f,teacher,outcome,clamped);add(groups.setdefault(bucket,stats()),q,f,teacher,outcome,clamped)
                errors.append(abs(q-f));stream.write(json.dumps(dict(index=i,bucket=bucket,quantized=q,floating=f,teacher_probability=teacher,outcome=outcome))+'\n')
        aggregate=finish(total);expected=assessment['validation']
        assert abs(aggregate['quantized_teacher']-expected['sigmoid_mse'])<1e-12
        assert abs(aggregate['quantized_outcome']-expected['wdl_mse'])<1e-12
        assert abs(aggregate['quantized_abs_link']-expected['mean_abs_link'])<1e-10
        errors.sort();aggregate.update(max_abs_link_error=errors[-1],p95_abs_link_error=errors[math.ceil(.95*len(errors))-1])
        results[name]=dict(aggregate=aggregate,material_heads={str(k):finish(v) for k,v in sorted(groups.items())},
            assessment_sha256=sha(evidence),network_sha256=sha(network),checkpoint_receipt_sha256=sha(checkpoint/'receipt.json'),
            raw_positions=str(rows),raw_positions_sha256=sha(rows),java_quantized_aggregate_check='PASS')
        print(json.dumps(dict(candidate=name,aggregate=aggregate)),flush=True)
    report=dict(models=results,positions=len(positions),validation_sha256=validation_hash,source_snapshot=str(out/Path(__file__).name),source_sha256=sha(out/Path(__file__).name),
        codec_sha256=sha(out/Path(codec.__file__).name),elapsed_seconds=time.monotonic()-started,
        floating='Independent float64 arithmetic from merged native float32 tensors; prior259-probe GPU checks bound sampled implementation differences. Both score variants retain the same32511 clamp and400 link scale.',
        limits='Common diagnostic holdout, not source-game independent. Material-head breakdown is descriptive. Float/quantized predictive loss differences are not strength differences or a QAT acceptance gate.')
    write(BASE/'quantization-128.json',report);write(out/'summary.json',report)


if __name__=='__main__':main()
