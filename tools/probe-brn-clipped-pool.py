"""Bounded clipped-pool feasibility diagnostic, with matched additive controls.

Four arms, 128 updates each; no checkpoint or optimizer export. This tests a
piece-sum threshold mechanism distinct from the polynomial information basis.
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

def activation(x,clipped):
    return (min(1,max(0,x)),float(0<x<1)) if clipped else (x,1.0)

def main(admission_path,output):
    admission_path,output=map(Path,[admission_path,output]);assert not output.exists()
    a=pilot.read(admission_path);assert a['id']=='wave9-clipped-pool-feasibility-v1'
    assert pilot.sha(Path(__file__))==a['scriptSha256']
    model=a['anchor'];data=basis.codec.payload(model['root'],model['checkpoint'],'network.*')
    assert basis.codec.sha(data)==model['networkSha256']
    w=struct.unpack('>3849d',basis.codec.decode(data,False,11)[0]);initial=list(w[769:3841]);rows=[];bindings={}
    for panel,folder in a['predictions'].items():
        for source in ['bt4','cp']:
            p=Path(folder)/(source+'-predictions.jsonl');bindings[str(p)]=pilot.sha(p);assert bindings[str(p)]==a['inputs'][str(p)]
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
    sums=[[sum(initial[4*f+k] for f in r['indices']) for k in range(4)] for r in fitting]
    centers=[st.mean(v[k] for v in sums) for k in range(4)]
    widths=[2*math.sqrt(st.mean((v[k]-centers[k])**2 for v in sums)) for k in range(4)];assert min(widths)>1e-8
    def features(r,embedding,clipped):
        values=[];derivatives=[]
        for k in range(4):
            x=.5+(sum(embedding[4*f+k] for f in r['indices'])-centers[k])/widths[k]
            v,d=activation(x,clipped);values.append(v);derivatives.append(d/widths[k])
        return r['affine']+values,derivatives
    max_error=0;reports=[];budget=a['budget']
    # An explicit third mixed difference distinguishes clipping from additive sums.
    witness={}
    for clipped in [False,True]:
        witness[str(clipped)]=sum((-1)**(3-sum(bits))*activation(-.1+.4*sum(bits),clipped)[0] for bits in itertools.product([0,1],repeat=3))
    assert abs(witness['False'])<1e-12 and abs(witness['True'])>.01
    for clipped,adaptive in itertools.product([False,True],[False,True]):
        for r in fitting[:3]:
            embedding=initial.copy();heads=[.03]*7;x,d=features(r,embedding,clipped)
            _,loss_d=basis.fitlib.objective(r['base']+.5*sum(c*v for c,v in zip(heads,x)),r['phase'],r['target'])
            for k in range(4):
                f=r['indices'][0];j=4*f+k;old=embedding[j];h=1e-6;embedding[j]=old+h
                plus=basis.fitlib.objective(r['base']+.5*sum(c*v for c,v in zip(heads,features(r,embedding,clipped)[0])),r['phase'],r['target'])[0]
                embedding[j]=old-h
                minus=basis.fitlib.objective(r['base']+.5*sum(c*v for c,v in zip(heads,features(r,embedding,clipped)[0])),r['phase'],r['target'])[0]
                embedding[j]=old;max_error=max(max_error,abs((plus-minus)/(2*h)-.5*loss_d*heads[3+k]*d[k]))
        assert max_error<1e-7
        embedding=initial.copy();heads=[0.0]*7;first=[0.0]*3079;second=[0.0]*3079;rng=random.Random(budget['seed']);step=0;curve=[]
        for epoch in range(8):
            order=list(range(len(fitting)));rng.shuffle(order);losses=[]
            for start in range(0,len(order),128):
                ids=order[start:start+128];gradient=[0.0]*3079
                for ix in ids:
                    r=fitting[ix];x,d=features(r,embedding,clipped)
                    loss,g=basis.fitlib.objective(r['base']+.5*sum(c*v for c,v in zip(heads,x)),r['phase'],r['target']);losses.append(loss)
                    for k in range(7):gradient[k]+=.5*g*x[k]/len(ids)
                    if adaptive:
                        for k in range(4):
                            value=.5*g*heads[3+k]*d[k]/len(ids)
                            for f in r['indices']:gradient[7+4*f+k]+=value
                step+=1
                for k in range(3079 if adaptive else 7):
                    old=heads[k] if k<7 else embedding[k-7];origin=0 if k<7 else initial[k-7]
                    g=gradient[k]+budget['l2']*(old-origin);first[k]=.9*first[k]+.1*g;second[k]=.999*second[k]+.001*g*g
                    rate=budget['headRate'] if k<7 else budget['embeddingRate']
                    value=old-rate*(first[k]/(1-.9**step))/(math.sqrt(second[k]/(1-.999**step))+1e-8)
                    if k<7:heads[k]=value
                    else:embedding[k-7]=value
            curve.append(st.mean(losses))
        assert step==128;metrics=[]
        for panel,source,mapping in itertools.product(a['predictions'],['bt4','cp'],['raw','quarter-search']):
            selected=[r for r in rows if r['panel']==panel and r['source']==source and (panel=='new' or r['index']>=1024)];losses=[];masked=[];active=[]
            for r in selected:
                x,d=features(r,embedding,clipped);gain=1 if mapping=='raw' else .25;offset=r['material']+gain*(r['base']-r['material'])
                correction=.5*gain*sum(c*v for c,v in zip(heads,x));affine=.5*gain*sum(c*v for c,v in zip(heads[:3],x[:3]))
                old=basis.fitlib.objective(offset,r['phase'],r['target'])[0]
                losses.append(basis.fitlib.objective(offset+correction,r['phase'],r['target'])[0]-old)
                masked.append(basis.fitlib.objective(offset+affine,r['phase'],r['target'])[0]-old);active.extend(float(v!=0) for v in d)
            metrics.append(dict(panel=panel,source=source,mapping=mapping,count=len(selected),crossEntropyChange=st.mean(losses),
                descriptiveRowSE=st.stdev(losses)/math.sqrt(len(losses)),unadaptedPoolMaskDifference=st.mean(losses)-st.mean(masked),activeDerivativeFraction=st.mean(active)))
        reports.append(dict(clipped=clipped,adaptive=adaptive,trainableParameters=3079 if adaptive else 7,steps=step,heads=heads,epochLoss=curve,
            embeddingChangeRms=math.sqrt(st.mean((x-y)**2 for x,y in zip(embedding,initial))),metrics=metrics))
    result=dict(id=a['id']+'-results',admission=str(admission_path),admissionSha256=pilot.sha(admission_path),inputs=bindings,
        maximumFiniteDifferenceError=max_error,thirdMixedDifference=witness,featureCenters=centers,featureWidths=widths,arms=reports,
        limitations=['Post-hoc mechanism selection; all six evaluation panels are consumed development data, not independent strength.',
        '128 updates per arm, fit-only normalization and fresh moments; not convergence or production optimizer continuation.',
        'Same four retained Q4 embedding directions in every arm; adaptive arms add3072 parameters. Linear pooled correction remains additive.',
        'Raw objective on frozen BRE176; quarter-search metrics report different deployment mapping. Masks are unadapted.',
        'No model/optimizer export, source replacement or owner writes. No automatic architecture or training-gate admission.'])
    with output.open('x',encoding='utf-8',newline='\n') as f:json.dump(result,f,indent=2);f.write('\n')
    print('Completed four bounded clipped-pool arms and gradient/expressiveness checks')
if __name__=='__main__':main(*sys.argv[1:])
