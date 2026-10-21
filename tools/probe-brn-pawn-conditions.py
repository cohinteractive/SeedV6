"""Bounded additive-blocker versus passed-pawn information diagnostic.

No checkpoint export, owner writes or search. Each arm gets exactly128 updates
on the same2043 previously consumed, status-valid fit rows. Calibration and pawn
rank counts are shared; only additive blocker counts versus the no-blocker
condition differ. A positive diagnostic is not architecture/strength acceptance.
"""
import hashlib
import importlib.util
import itertools
import json
import math
from pathlib import Path
import random
import statistics as st
import sys
import chess

spec=importlib.util.spec_from_file_location('fitlib',Path(__file__).with_name('probe-brn-visibility-fit.py'))
fitlib=importlib.util.module_from_spec(spec);spec.loader.exec_module(fitlib)

def features(board, conditional):
    """Side-to-move canonical own/opponent rank counts; no attack legality claims."""
    b=board if board.turn else board.mirror()
    out=[0.0]*24
    for color in [chess.WHITE,chess.BLACK]:
        enemy=b.pawns & b.occupied_co[not color]
        for square in b.pieces(chess.PAWN,color):
            file,rank=chess.square_file(square),chess.square_rank(square)
            progress=rank if color else 7-rank
            assert 1<=progress<=6
            index=(0 if color else 6)+progress-1
            mask=0
            for f in range(max(0,file-1),min(7,file+1)+1):
                for r in (range(rank+1,8) if color else range(0,rank)):
                    mask |= chess.BB_SQUARES[chess.square(f,r)]
            blockers=chess.popcount(enemy & mask)
            out[index]+=1/math.sqrt(8)
            out[12+index]+=(int(blockers==0) if conditional else blockers/4)/math.sqrt(8)
    return out

def naive(board,conditional):
    b=board if board.turn else board.mirror();out=[0.0]*24
    for s,p in b.piece_map().items():
        if p.piece_type!=chess.PAWN:continue
        rank=chess.square_rank(s);index=(0 if p.color else 6)+(rank if p.color else 7-rank)-1
        blockers=sum(q.piece_type==chess.PAWN and q.color!=p.color and
            abs(chess.square_file(t)-chess.square_file(s))<=1 and
            ((chess.square_rank(t)>rank) if p.color else (chess.square_rank(t)<rank))
            for t,q in b.piece_map().items())
        out[index]+=1/math.sqrt(8);out[12+index]+=(int(blockers==0) if conditional else blockers/4)/math.sqrt(8)
    return out

def main(admission_path,output):
    admission_path,output=Path(admission_path),Path(output);assert not output.exists()
    a=json.loads(admission_path.read_text(encoding='utf-8-sig'))
    assert a['id']=='wave9-pawn-condition-feasibility-v1'
    assert hashlib.sha256(Path(__file__).read_bytes()).hexdigest()==a['scriptSha256']
    rows=[];bindings={};oracle=0;mirror=0
    for panel,source in itertools.product(['fresh5','old'],['bt4','cp']):
        path=Path(a['predictions'][panel])/(source+'-predictions.jsonl')
        bindings[str(path)]=hashlib.sha256(path.read_bytes()).hexdigest()
        for i,line in enumerate(path.read_text(encoding='utf-8').splitlines()):
            row=json.loads(line);b=chess.Board(row['fen'])
            if not b.is_valid():continue
            pieces=b.piece_map();m=sum((1 if p.color==b.turn else -1)*{1:1,2:3.2,3:3.3,4:5,5:9}.get(p.piece_type,0) for p in pieces.values())
            phase=sum({1:1,2:3,3:3,4:5,5:9}.get(p.piece_type,0) for p in pieces.values())
            v=row['predictions'];g=v['Gen32'];q=2*(v[a['predictor']+'-zero-heads']-g)
            base=m+.25*(v[a['predictor']]-m)
            f=[1,m/8,(g-m)/4,q]
            controls={}
            for label,conditional in [('additive',False),('conditional',True)]:
                x=features(b,conditional);y=naive(b,conditional)
                oracle=max(oracle,max(abs(l-r) for l,r in zip(x,y)))
                mirror=max(mirror,max(abs(l-r) for l,r in zip(x,features(b.mirror(),conditional))))
                controls[label]=f+x
            rows.append(dict(panel=panel,source=source,index=i,fit=panel=='fresh5' and i<1024,
                phase=phase,target=row['target'],base=base,**controls))
    assert oracle<1e-12 and mirror==0
    witness={}
    for label,conditional in [('additive',False),('conditional',True)]:
        third=0
        for bits in itertools.product([0,1],repeat=3):
            board=chess.Board('7k/8/8/8/8/8/8/7K w - - 0 1')
            for present,square,color in zip(bits,[chess.A2,chess.A4,chess.B4],[True,False,False]):
                if present:board.set_piece_at(square,chess.Piece(chess.PAWN,color))
            third+=(-1)**(3-sum(bits))*features(board,conditional)[12]
        witness[label]=third
    assert abs(witness['additive'])<1e-12 and abs(witness['conditional']-1/math.sqrt(8))<1e-12
    fit=[r for r in rows if r['fit']];assert len(fit)==2043
    reports=[]
    for mode in ['additive','conditional']:
        weights=[0.0]*28;first=[0.0]*28;second=[0.0]*28;rng=random.Random(a['budget']['seed']);step=0;curve=[]
        for epoch in range(8):
            order=list(range(len(fit)));rng.shuffle(order);losses=[]
            for start in range(0,len(order),128):
                ids=order[start:start+128];gradient=[0.0]*28
                for i in ids:
                    r=fit[i];x=r[mode];score=r['base']+.125*sum(w*v for w,v in zip(weights,x))
                    loss,d=fitlib.objective(score,r['phase'],r['target']);losses.append(loss)
                    for k,v in enumerate(x):gradient[k]+=.125*d*v/len(ids)
                step+=1;c1=1-.9**step;c2=1-.999**step
                for k,g in enumerate(gradient):
                    g+=a['budget']['l2']*weights[k]
                    first[k]=.9*first[k]+.1*g;second[k]=.999*second[k]+.001*g*g
                    weights[k]-=a['budget']['rate']*(first[k]/c1)/(math.sqrt(second[k]/c2)+1e-8)
            curve.append(st.mean(losses))
        assert step==128
        metrics=[]
        for panel,source in itertools.product(['fresh5','old'],['bt4','cp']):
            selected=[r for r in rows if r['panel']==panel and r['source']==source and r['index']>=1024]
            ds=[];scores=[]
            for r in selected:
                change=.125*sum(w*v for w,v in zip(weights,r[mode]));scores.append(change)
                ds.append(fitlib.objective(r['base']+change,r['phase'],r['target'])[0]-fitlib.objective(r['base'],r['phase'],r['target'])[0])
            metrics.append(dict(panel=panel,source=source,count=len(selected),meanCrossEntropyChange=st.mean(ds),
                standardError=st.stdev(ds)/math.sqrt(len(ds)),changeRmsPawns=math.sqrt(st.mean(v*v for v in scores))))
        reports.append(dict(mode=mode,steps=step,weights=weights,epochLoss=curve,metrics=metrics))
    result=dict(id=a['id']+'-results',admission=str(admission_path),admissionSha256=hashlib.sha256(admission_path.read_bytes()).hexdigest(),
        inputs=bindings,independentFeatureMaximumError=oracle,colorMirrorMaximumError=mirror,
        thirdMixedDifference=witness,arms=reports,
        limitations=['Consumed status-valid panels, no independent generalization or playing-strength claim.',
            'Fixed128 updates/arm, not optimized convergence or evidence that either basis is unsuitable.',
            'Same28 parameter budget and nominal Adam, but different feature conditioning/scale.',
            'No checkpoint/optimizer export or owner writes; no architecture implementation admitted by this pilot alone.',
            'Passed-pawn condition ignores piece blockers, king safety and tactical feasibility; it describes pawn geometry only.'])
    with output.open('x',encoding='utf-8',newline='\n') as f:json.dump(result,f,indent=2);f.write('\n')
    print('Completed two128-update pawn-condition diagnostic arms; feature/mirror/degree witnesses pass')
if __name__=='__main__':main(*sys.argv[1:])
