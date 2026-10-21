"""Bounded rank-one basis-adaptation diagnostic; no exported model or owner writes.

Four arms distinguish fixed versus trainable embeddings and degree2 versus3 on
the same continued BRE base. Existing rank-four component0 initializes every arm.
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

def polynomial(indices,embedding,degree):
    first=second=third=0.0
    for f in indices:
        v=embedding[f];third+=second*v;second+=first*v;first+=v
    norm=1/math.sqrt(496 if degree==2 else 4960)
    value=(second if degree==2 else third)*norm
    gradient=[((first-embedding[f]) if degree==2 else second-embedding[f]*(first-embedding[f]))*norm for f in indices]
    return value,gradient

def main(admission_path,output):
    admission_path,output=map(Path,[admission_path,output]);assert not output.exists()
    a=pilot.read(admission_path);assert a['id']=='wave9-adaptive-degree-feasibility-v1'
    assert pilot.sha(Path(__file__))==a['scriptSha256']
    model=a['anchor'];data=basis.codec.payload(model['root'],model['checkpoint'],'network.*')
    assert basis.codec.sha(data)==model['networkSha256']
    w=struct.unpack('>3849d',basis.codec.decode(data,False,11)[0]);initial=[w[769+4*f] for f in range(768)]
    rows=[];bindings={}
    for panel,source in itertools.product(['fresh5','old'],['bt4','cp']):
        p=Path(a['predictions'][panel])/(source+'-predictions.jsonl');bindings[str(p)]=pilot.sha(p)
        assert bindings[str(p)]==a['inputs'][str(p)]
        for i,line in enumerate(p.read_text(encoding='utf-8').splitlines()):
            r=json.loads(line);board=chess.Board(r['fen'])
            if not board.is_valid():continue
            oriented=board if board.turn else board.mirror();pieces=board.piece_map()
            indices=[(basis.types[v.piece_type]-1+(0 if v.color else 6))*64+sq for sq,v in sorted(oriented.piece_map().items())]
            m=sum((1 if v.color==board.turn else -1)*{1:1,2:3.2,3:3.3,4:5,5:9}.get(v.piece_type,0) for v in pieces.values())
            phase=sum({1:1,2:3,3:3,4:5,5:9}.get(v.piece_type,0) for v in pieces.values())
            base=r['predictions']['W9-BRE-CONTROL-LR001-g176']
            rows.append(dict(panel=panel,source=source,index=i,fit=panel=='fresh5' and i<1024,indices=indices,
                base=base,material=m,phase=phase,target=r['target'],affine=[1,m/8,(base-m)/4]))
    fitting=[r for r in rows if r['fit']];assert len(fitting)==2043
    means=[0]+[st.mean(r['affine'][k] for r in fitting) for k in range(1,3)]
    scales=[1]+[math.sqrt(st.mean((r['affine'][k]-means[k])**2 for r in fitting)) for k in range(1,3)]
    for r in rows:r['affine']=[(v-m)/s for v,m,s in zip(r['affine'],means,scales)]
    reports=[];budget=a['budget'];max_gradient_error=0
    for degree,adaptive in itertools.product([2,3],[False,True]):
        values=[polynomial(r['indices'],initial,degree)[0] for r in fitting]
        center=st.mean(values);scale=math.sqrt(st.mean((v-center)**2 for v in values));assert scale>1e-10
        # Check the composed objective gradient on actual positions, independently
        # perturbing a parameter that appears in the polynomial combination sum.
        for r in fitting[:3]:
            e=initial.copy();v,g=polynomial(r['indices'],e,degree);head=.03
            _,d=basis.fitlib.objective(r['base']+.5*head*(v-center)/scale,r['phase'],r['target'])
            for j in [0,len(r['indices'])-1]:
                f=r['indices'][j];old=e[f];h=1e-5;e[f]=old+h
                plus=basis.fitlib.objective(r['base']+.5*head*(polynomial(r['indices'],e,degree)[0]-center)/scale,r['phase'],r['target'])[0]
                e[f]=old-h
                minus=basis.fitlib.objective(r['base']+.5*head*(polynomial(r['indices'],e,degree)[0]-center)/scale,r['phase'],r['target'])[0]
                e[f]=old;max_gradient_error=max(max_gradient_error,abs((plus-minus)/(2*h)-.5*head*d*g[j]/scale))
        assert max_gradient_error<1e-7
        embedding=initial.copy();heads=[0.0]*4;first=[0.0]*772;second=[0.0]*772
        rng=random.Random(budget['seed']);step=0;curve=[]
        for epoch in range(8):
            order=list(range(len(fitting)));rng.shuffle(order);losses=[]
            for start in range(0,len(order),128):
                ids=order[start:start+128];gradient=[0.0]*772
                for ix in ids:
                    r=fitting[ix];v,pg=polynomial(r['indices'],embedding,degree);x=r['affine']+[(v-center)/scale]
                    loss,d=basis.fitlib.objective(r['base']+.5*sum(c*z for c,z in zip(heads,x)),r['phase'],r['target']);losses.append(loss)
                    for k in range(4):gradient[k]+=.5*d*x[k]/len(ids)
                    if adaptive:
                        for f,g in zip(r['indices'],pg):gradient[4+f]+=.5*d*heads[3]*g/scale/len(ids)
                step+=1
                for k in range(772 if adaptive else 4):
                    old=heads[k] if k<4 else embedding[k-4]
                    origin=0 if k<4 else initial[k-4]
                    g=gradient[k]+budget['l2']*(old-origin)
                    first[k]=.9*first[k]+.1*g;second[k]=.999*second[k]+.001*g*g
                    rate=budget['headRate'] if k<4 else budget['embeddingRate']
                    value=old-rate*(first[k]/(1-.9**step))/(math.sqrt(second[k]/(1-.999**step))+1e-8)
                    if k<4:heads[k]=value
                    else:embedding[k-4]=value
            curve.append(st.mean(losses))
        assert step==128;metrics=[]
        for panel,source,mapping in itertools.product(['fresh5','old'],['bt4','cp'],['raw','quarter-search']):
            selected=[r for r in rows if r['panel']==panel and r['source']==source and r['index']>=1024]
            losses=[];masked=[];changes=[]
            for r in selected:
                v=polynomial(r['indices'],embedding,degree)[0];gain=1 if mapping=='raw' else .25
                offset=r['material']+gain*(r['base']-r['material'])
                affine=.5*gain*sum(c*x for c,x in zip(heads[:3],r['affine']))
                delta=.5*gain*heads[3]*(v-center)/scale
                old=basis.fitlib.objective(offset,r['phase'],r['target'])[0]
                losses.append(basis.fitlib.objective(offset+affine+delta,r['phase'],r['target'])[0]-old)
                masked.append(basis.fitlib.objective(offset+affine,r['phase'],r['target'])[0]-old);changes.append(delta)
            metrics.append(dict(panel=panel,source=source,mapping=mapping,count=len(selected),crossEntropyChange=st.mean(losses),
                standardError=st.stdev(losses)/math.sqrt(len(losses)),degreeRmsPawns=math.sqrt(st.mean(v*v for v in changes)),
                unadaptedDegreeMaskDifference=st.mean(losses)-st.mean(masked)))
        reports.append(dict(degree=degree,adaptiveEmbedding=adaptive,trainableParameters=772 if adaptive else 4,
            steps=step,heads=heads,featureCenter=center,featureScale=scale,epochLoss=curve,metrics=metrics,
            embeddingChangeRms=math.sqrt(st.mean((x-y)**2 for x,y in zip(embedding,initial))),maximumEmbeddingChange=max(abs(x-y) for x,y in zip(embedding,initial))))
    report=dict(id=a['id']+'-results',admission=str(admission_path),admissionSha256=pilot.sha(admission_path),inputs=bindings,
        maximumFiniteDifferenceError=max_gradient_error,affineMeans=means,affineScales=scales,arms=reports,
        limitations=['Only128 updates per arm on consumed panels; no independent prediction/strength or convergence claim.',
        'Same rank-one component0 initialization, fixed fit-only normalization; adapted arms have768 extra parameters and explicit lower embedding rate.',
        'Degree2 remains additive pairwise; degree3 permits three-piece interactions. Tiny diagnostic does not establish the best rank or basis.',
        'Degree masks are unadapted. No checkpoint, competitive trained model, optimizer export or owner writes.'])
    with output.open('x',encoding='utf-8',newline='\n') as f:json.dump(report,f,indent=2);f.write('\n')
    print('Completed four bounded rank-one adaptation arms and objective finite differences')
if __name__=='__main__':main(*sys.argv[1:])
