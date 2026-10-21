"""Small fixed king-contact interaction diagnostic, not a trainable evaluator.

Geometric contact counts are additive king/piece relations. Joint attacker counts
add higher-order information. Blockers are deliberately excluded in both arms.
"""
import importlib.util
import itertools
import json
import math
from pathlib import Path
import random
import statistics as st
import sys
import chess

s=importlib.util.spec_from_file_location('pilot',Path(__file__).with_name('probe-brn-continued-base-heads.py'))
pilot=importlib.util.module_from_spec(s);s.loader.exec_module(pilot)
basis=pilot.basis

def contact(piece,square,king):
    ring=chess.BB_KING_ATTACKS[king]|chess.BB_SQUARES[king]
    if piece.piece_type==chess.PAWN:return bool(chess.BB_PAWN_ATTACKS[piece.color][square]&ring)
    if piece.piece_type==chess.KNIGHT:return bool(chess.BB_KNIGHT_ATTACKS[square]&ring)
    # Empty-board rays: each count depends only on this piece and the enemy king.
    f,r=chess.square_file(square),chess.square_rank(square)
    for target in chess.scan_forward(ring):
        df=abs(chess.square_file(target)-f);dr=abs(chess.square_rank(target)-r)
        if df+dr and ((piece.piece_type in (chess.BISHOP,chess.QUEEN) and df==dr)
                     or (piece.piece_type in (chess.ROOK,chess.QUEEN) and (df==0 or dr==0))):return True
    return False

def features(board):
    counts=[]
    for color in [board.turn,not board.turn]:
        king=board.king(not color);assert king is not None
        counts.extend(sum(contact(chess.Piece(t,color),sq,king) for sq in board.pieces(t,color)) for t in range(1,6))
    interactions=[]
    for offset in [0,5]:
        pawn,knight,bishop,rook,queen=counts[offset:offset+5]
        minor=knight+bishop;heavy=rook+queen;total=pawn+minor+heavy
        interactions.extend([total*(total-1)/2,minor*heavy,pawn*(minor+heavy)])
    return counts,interactions

def witness():
    # Mixed difference of king location and two optional knights, with other
    # features fixed. A sum of pairwise relations has zero third difference.
    values=[]
    for moved,n1,n2 in itertools.product([0,1],repeat=3):
        b=chess.Board(None);b.turn=chess.WHITE
        b.set_piece_at(chess.A1 if not moved else chess.H1,chess.Piece(chess.KING,chess.WHITE))
        b.set_piece_at(chess.H8,chess.Piece(chess.KING,chess.BLACK))
        if n1:b.set_piece_at(chess.C2,chess.Piece(chess.KNIGHT,chess.BLACK))
        if n2:b.set_piece_at(chess.C3,chess.Piece(chess.KNIGHT,chess.BLACK))
        linear,joint=features(b);values.append(((-1)**(3-moved-n1-n2),sum(linear),joint[3]))
    linear=sum(s*x for s,x,y in values);nonlinear=sum(s*y for s,x,y in values)
    assert linear==0 and nonlinear!=0
    return dict(additiveThirdDifference=linear,jointThirdDifference=nonlinear)

def main(admission_path,output):
    admission_path,output=map(Path,[admission_path,output]);assert not output.exists()
    a=pilot.read(admission_path);assert a['id']=='wave9-king-contact-feasibility-v1'
    assert pilot.sha(Path(__file__))==a['scriptSha256'];proof=witness();rows=[];bindings={}
    for panel,source in itertools.product(['fresh5','old'],['bt4','cp']):
        p=Path(a['predictions'][panel])/(source+'-predictions.jsonl');bindings[str(p)]=pilot.sha(p)
        assert bindings[str(p)]==a['inputs'][str(p)]
        for i,line in enumerate(p.read_text(encoding='utf-8').splitlines()):
            r=json.loads(line);b=chess.Board(r['fen'])
            if not b.is_valid():continue
            f,h=features(b);assert (f,h)==features(b.mirror())
            pieces=b.piece_map();m=sum((1 if v.color==b.turn else -1)*{1:1,2:3.2,3:3.3,4:5,5:9}.get(v.piece_type,0) for v in pieces.values())
            phase=sum({1:1,2:3,3:3,4:5,5:9}.get(v.piece_type,0) for v in pieces.values())
            base=r['predictions']['W9-BRE-CONTROL-LR001-g176'];affine=[1,m/8,(base-m)/4]
            rows.append(dict(panel=panel,source=source,index=i,fit=panel=='fresh5' and i<1024,
                base=base,material=m,phase=phase,target=r['target'],affine=affine,linear=affine+f,joint=affine+f+h))
    fitting=[r for r in rows if r['fit']];assert len(fitting)==2043
    reports=[];budget=a['budget']
    for mode in ['affine','linear','joint']:
        size=len(fitting[0][mode]);means=[0]+[st.mean(r[mode][k] for r in fitting) for k in range(1,size)]
        scales=[1]+[math.sqrt(st.mean((r[mode][k]-means[k])**2 for r in fitting)) for k in range(1,size)];assert min(scales)>1e-12
        for r in rows:r['x']=[(v-m)/s for v,m,s in zip(r[mode],means,scales)]
        w=[0.0]*size;first=[0.0]*size;second=[0.0]*size;rng=random.Random(budget['seed']);step=0;curve=[]
        for epoch in range(8):
            order=list(range(len(fitting)));rng.shuffle(order);losses=[]
            for start in range(0,len(order),128):
                ids=order[start:start+128];gradient=[0.0]*size
                for ix in ids:
                    r=fitting[ix];raw=r['base']+.5*sum(c*x for c,x in zip(w,r['x']))
                    loss,d=basis.fitlib.objective(raw,r['phase'],r['target']);losses.append(loss)
                    for k in range(size):gradient[k]+=.5*d*r['x'][k]/len(ids)
                step+=1
                for k,g in enumerate(gradient):
                    g+=budget['l2']*w[k];first[k]=.9*first[k]+.1*g;second[k]=.999*second[k]+.001*g*g
                    w[k]-=budget['rate']*(first[k]/(1-.9**step))/(math.sqrt(second[k]/(1-.999**step))+1e-8)
            curve.append(st.mean(losses))
        assert step==128;metrics=[]
        for panel,source,mapping in itertools.product(['fresh5','old'],['bt4','cp'],['raw','quarter-search']):
            selected=[r for r in rows if r['panel']==panel and r['source']==source and r['index']>=1024]
            losses=[];masks=[];changes=[]
            for r in selected:
                gain=1 if mapping=='raw' else .25;offset=r['material']+gain*(r['base']-r['material'])
                change=.5*gain*sum(c*x for c,x in zip(w,r['x']));mask=.5*gain*sum(c*x for c,x in zip(w[:13],r['x'][:13]))
                old=basis.fitlib.objective(offset,r['phase'],r['target'])[0]
                losses.append(basis.fitlib.objective(offset+change,r['phase'],r['target'])[0]-old)
                masks.append(basis.fitlib.objective(offset+mask,r['phase'],r['target'])[0]-old);changes.append(change)
            metrics.append(dict(panel=panel,source=source,mapping=mapping,count=len(selected),crossEntropyChange=st.mean(losses),
                standardError=st.stdev(losses)/math.sqrt(len(losses)),changeRmsPawns=math.sqrt(st.mean(x*x for x in changes)),
                unadaptedJointMaskDifference=st.mean(losses)-st.mean(masks)))
        reports.append(dict(mode=mode,parameters=size,steps=step,coefficients=w,means=means,scales=scales,epochLoss=curve,metrics=metrics))
    report=dict(id=a['id']+'-results',admission=str(admission_path),admissionSha256=pilot.sha(admission_path),inputs=bindings,witness=proof,arms=reports,
        limitations=['Consumed panels and selected hypothesis; not independent prediction or chess evidence.',
        'Geometric contacts ignore blockers and tactical feasibility in both controls. Joint terms represent multiple attackers, not verified king danger.',
        'Joint has six additional coefficients; adapted linear control and unadapted joint mask are distinct comparisons.',
        'Small128-update fits, no convergence claim, model export, owner mutation or automatic training-wave admission.'])
    with output.open('x',encoding='utf-8',newline='\n') as f:json.dump(report,f,indent=2);f.write('\n')
    print('Completed three king-contact feasibility arms, mirror and third-difference checks')
if __name__=='__main__':main(*sys.argv[1:])
