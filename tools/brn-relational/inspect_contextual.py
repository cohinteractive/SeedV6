"""Independent bounded R014 context ablation; never changes trained weights."""
import argparse
import json
import math
from pathlib import Path
import struct
import time
import zlib
import chess
from baseline import ROOT, admit, sha
from compute_control import atomic

E,L,R,O,G,N=769,6913,28513,32113,38257,38449


def weights(path):
    data=path.read_bytes()
    if len(data)!=40+24*N:raise ValueError('State length')
    magic,mode,n,rate,step=struct.unpack_from('>qiidq',data)
    if magic!=0x42524e4341543031 or mode not in (0,1) or n!=N:raise ValueError('Geometry')
    if zlib.crc32(data[:-8])!=struct.unpack_from('>q',data,len(data)-8)[0]:raise ValueError('CRC')
    result=struct.unpack_from('>'+str(N)+'d',data,32)
    if not all(math.isfinite(v) for v in result):raise ValueError('Weights')
    return result,mode==1


def evaluate(w,fen,global_context=True):
    b=chess.Board(fen);pieces=b.piece_map();squares=sorted(pieces);flip=0 if b.turn else 56
    nodes={s:(6-p.piece_type+6*(p.color!=b.turn))*64+(s^flip) for s,p in pieces.items()}
    h=[[0.]*8 for _ in range(2)];norm=1/math.sqrt(len(squares)-1)
    for k in range(2):
        king=b.king(b.turn if k==0 else not b.turn);tq=king^flip
        for s in squares:
            if s==king:continue
            sq=s^flip;rel=k*225+(tq//8-sq//8+7)*15+tq%8-sq%8+7
            for d in range(8):h[k][d]+=norm*w[E+nodes[s]*8+d]*w[R+rel*8+d]
        h[k]=[v/(1+abs(v)) for v in h[k]]
    values={chess.KING:0,chess.QUEEN:9,chess.ROOK:5,chess.BISHOP:3.3,chess.KNIGHT:3.2,chess.PAWN:1}
    material=sum(values[pieces[s].piece_type]*(1 if pieces[s].color==b.turn else -1) for s in squares)/4
    base=material+w[768]+sum(w[nodes[s]] for s in squares);full=base;off=base
    incoming={s:[] for s in squares}
    for s in squares:
        for t in b.attacks(s):
            if t in pieces:incoming[t].append(s)
    for t in squares:
        sources=incoming[t]
        if not sources:continue
        a=[0.]*8;tq=t^flip
        for s in sources:
            sq=s^flip;rel=nodes[t]//64*225+(tq//8-sq//8+7)*15+tq%8-sq%8+7
            for d in range(8):a[d]+=w[E+nodes[s]*8+d]*w[L+rel*8+d]/math.sqrt(len(sources))
        for d in range(8):
            gain=1+sum(w[G+(nodes[t]//64*2+k)*8+d]*h[k][d] for k in range(2)) if global_context else 1;z=a[d]*gain
            full+=w[O+nodes[t]*8+d]*z/(1+abs(z))/8;off+=w[O+nodes[t]*8+d]*a[d]/(1+abs(a[d]))/8
    return full,off


def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('plan',type=Path);args=parser.parse_args();plan=json.loads(args.plan.read_text());out=ROOT/plan['output']
    resources=[admit(initial=True)];start=time.monotonic();results=[]
    binding=dict(plan_sha256=sha(args.plan),runner_sha256=sha(Path(__file__)))
    for arm in plan['arms']:
        path=ROOT/arm['state']
        if sha(path)!=arm['state_sha256']:raise ValueError('Model identity')
        w,global_context=weights(path);metrics={}
        for panel in plan['panels']:
            p=ROOT/panel['path']
            if sha(p)!=panel['sha256']:raise ValueError('Data identity')
            losses=[0.,0.];delta=0.;count=0
            with p.open() as f:
                for line in f:
                    if count==panel['rows']:break
                    if count%512==0:
                        resources.append(admit())
                        if time.monotonic()-start>plan['max_seconds']:raise RuntimeError('Bounded ablation allocation exhausted')
                    fen,y=line.rstrip().split('\t');target=float(y);a,b=evaluate(w,fen,global_context)
                    for i,v in enumerate((a,b)):
                        prediction=1/(1+math.exp(-v)) if v>=0 else math.exp(v)/(1+math.exp(v));losses[i]+=(prediction-target)**2
                    delta+=(400*(a-b))**2;count+=1
            if count!=panel['rows']:raise ValueError('Panel length')
            metrics[panel['name']]=dict(rows=count,full_mse=losses[0]/count,context_off_mse=losses[1]/count,context_rms_cp=math.sqrt(delta/count))
        if abs(metrics['validation']['full_mse']-arm['java_validation_mse'])>1e-12:raise ValueError('Independent evaluator mismatch')
        results.append(dict(seed=arm['seed'],global_context=global_context,state=arm['state'],state_sha256=arm['state_sha256'],panels=metrics))
    if out.exists():raise ValueError('Immutable output already exists')
    atomic(out,dict(binding=binding,results=results,resources=resources,seconds=time.monotonic()-start,scope='Independent Python equations match complete Java development validation MSE within1e-12; panels are consumed development or explicitly bounded training prefixes,not final quality/strength evidence.'))
    print(json.dumps(results,indent=2))


if __name__=='__main__':main()
