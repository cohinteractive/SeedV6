"""Bounded warm-start/quiet-domain diagnostic with adapted affine controls.

All four arms start at the verified trained G9 affine function. New delta heads
have fresh moments. Equal per-source visits; quiet filtering changes unique data.
No checkpoint or optimizer is exported.
"""
import importlib.util
import itertools
import json
import math
from pathlib import Path
import random
import statistics as st
import struct
import sys
import chess

s=importlib.util.spec_from_file_location('pilot',Path(__file__).with_name('probe-brn-continued-base-heads.py'))
pilot=importlib.util.module_from_spec(s);s.loader.exec_module(pilot);basis=pilot.basis

def main(admission_path,output):
    admission_path,output=map(Path,[admission_path,output]);assert not output.exists()
    a=pilot.read(admission_path);assert a['id']=='wave9-warm-quiet-head-feasibility-v1'
    assert pilot.sha(Path(__file__))==a['scriptSha256']
    model=a['anchor'];data=basis.codec.payload(model['root'],model['checkpoint'],'network.*');assert basis.codec.sha(data)==model['networkSha256']
    w=struct.unpack('>3849d',basis.codec.decode(data,False,11)[0]);rows=[];bindings={};error=0
    for panel,folder in a['predictions'].items():
        for source in ['bt4','cp']:
            p=Path(folder)/(source+'-predictions.jsonl');bindings[str(p)]=pilot.sha(p);assert bindings[str(p)]==a['inputs'][str(p)]
            for i,line in enumerate(p.read_text(encoding='utf-8').splitlines()):
                r=json.loads(line);board=chess.Board(r['fen'])
                if not board.is_valid():continue
                legal=list(board.legal_moves);quiet=bool(legal) and not board.is_check() and not any(board.is_capture(m) or m.promotion for m in legal)
                pieces=board.piece_map();material=sum((1 if v.color==board.turn else -1)*{1:1,2:3.2,3:3.3,4:5,5:9}.get(v.piece_type,0) for v in pieces.values())
                phase=sum({1:1,2:3,3:3,4:5,5:9}.get(v.piece_type,0) for v in pieces.values())
                unary,q,c,e=basis.features(board,w,4);anchor=unary+sum(w[3841+k]*q[k] for k in range(4))
                v=r['predictions'];g=v[a['predictor']+'-zero-heads']-.5*anchor
                affine=[1,material/8,(g-material)/4,anchor];raw=g+.5*(anchor+sum(x*y for x,y in zip(w[3845:],affine)))
                error=max(error,abs(raw-v[a['predictor']]),e)
                rows.append(dict(panel=panel,source=source,index=i,fit=panel=='fresh5' and i<1024,quiet=quiet,
                    base=material+.25*(raw-material),phase=phase,target=r['target'],features=affine+c))
    assert error<1e-8
    fit=[r for r in rows if r['fit']];assert len(fit)==2043
    means=[0]+[st.mean(r['features'][k] for r in fit) for k in range(1,8)]
    scales=[1]+[math.sqrt(st.mean((r['features'][k]-means[k])**2 for r in fit)) for k in range(1,8)];assert min(scales)>1e-12
    for r in rows:r['x']=[(v-m)/s for v,m,s in zip(r['features'],means,scales)]
    reports=[];budget=a['budget']
    for mode,quiet_only in itertools.product(['affine','cubic'],[False,True]):
        pools={s:[r for r in fit if r['source']==s and (r['quiet'] or not quiet_only)] for s in ['bt4','cp']}
        assert all(len(p)>64 for p in pools.values())
        rng=random.Random(budget['seed']);orders={s:[] for s in pools};size=4 if mode=='affine' else 8
        heads=[0.0]*size;first=[0.0]*size;second=[0.0]*size;curve=[]
        for step in range(1,129):
            batch=[]
            for source in ['bt4','cp']:
                for _ in range(64):
                    if not orders[source]:orders[source]=list(range(len(pools[source])));rng.shuffle(orders[source])
                    batch.append(pools[source][orders[source].pop()])
            gradient=[0.0]*size;losses=[]
            for r in batch:
                score=r['base']+.125*sum(c*x for c,x in zip(heads,r['x']))
                loss,d=basis.fitlib.objective(score,r['phase'],r['target']);losses.append(loss)
                for k in range(size):gradient[k]+=.125*d*r['x'][k]/128
            for k,g in enumerate(gradient):
                g+=budget['l2']*heads[k];first[k]=.9*first[k]+.1*g;second[k]=.999*second[k]+.001*g*g
                heads[k]-=budget['rate']*(first[k]/(1-.9**step))/(math.sqrt(second[k]/(1-.999**step))+1e-8)
            if step%16==0:curve.append(dict(step=step,batchCrossEntropy=st.mean(losses)))
        metrics=[]
        for panel,source,subset in itertools.product(a['predictions'],['bt4','cp'],['all','strict-quiet']):
            selected=[r for r in rows if r['panel']==panel and r['source']==source and (panel=='new' or r['index']>=1024) and (subset=='all' or r['quiet'])]
            losses=[];masked=[];changes=[]
            for r in selected:
                delta=.125*sum(c*x for c,x in zip(heads,r['x']));affine=.125*sum(c*x for c,x in zip(heads[:4],r['x'][:4]))
                old=basis.fitlib.objective(r['base'],r['phase'],r['target'])[0]
                losses.append(basis.fitlib.objective(r['base']+delta,r['phase'],r['target'])[0]-old)
                masked.append(basis.fitlib.objective(r['base']+affine,r['phase'],r['target'])[0]-old);changes.append(delta)
            metrics.append(dict(panel=panel,source=source,subset=subset,count=len(selected),crossEntropyChange=st.mean(losses),
                descriptiveRowSE=st.stdev(losses)/math.sqrt(len(losses)),changeRmsPawns=math.sqrt(st.mean(x*x for x in changes)),
                unadaptedDegreeMaskDifference=st.mean(losses)-st.mean(masked)))
        reports.append(dict(mode=mode,quietOnly=quiet_only,steps=128,visitsPerSource=8192,uniqueFitRows={s:len(p) for s,p in pools.items()},
            heads=heads,curve=curve,metrics=metrics))
    result=dict(id=a['id']+'-results',admission=str(admission_path),admissionSha256=pilot.sha(admission_path),inputs=bindings,
        maximumIndependentForwardError=error,featureMeans=means,featureScales=scales,arms=reports,
        limitations=['Post-hoc hypothesis selected after aggregate and quiet-stratum observations; all evaluation windows now consumed development evidence.',
        'Only128 updates with fit-only normalization and new moments; not production optimizer continuation, convergence or strength.',
        'Equal source visits but different unique pools and reuse rates. Quiet is a board property, not actual recursive-search leaf sampling.',
        'Warm calibrated affine start differs from original G9 zero-head initialization. Degree masks are unadapted.',
        'No model/optimizer export, owner writes, source replacement or automatic training-gate admission.'])
    with output.open('x',encoding='utf-8',newline='\n') as f:json.dump(result,f,indent=2);f.write('\n')
    print('Completed four bounded warm-affine/quiet-domain head diagnostics')
if __name__=='__main__':main(*sys.argv[1:])
