"""Small fixed-basis optimizer diagnostic. Frozen quadratic anchors, no owner writes.

Reproduce Java quadratic predictions independently, expose degree2/3 features of
their fixed embeddings, and fit only4..32 new head deltas on2048 consumed rows.
All arms and budgets are fixed by the admission; no architecture is implemented.
"""
import hashlib
import importlib.util
import itertools
import json
import math
from pathlib import Path
import random
import statistics
import struct
import sys
import chess

def module(name, filename):
    spec=importlib.util.spec_from_file_location(name,Path(__file__).with_name(filename))
    result=importlib.util.module_from_spec(spec);spec.loader.exec_module(result);return result

codec=module('degree','audit-brn-degree-projection.py')
fitlib=module('fit','probe-brn-visibility-fit.py')
types=fitlib.feature.TYPES

def features(board,weights,width):
    board=board if board.turn else board.mirror()
    indices=[(types[p.piece_type]-1+(0 if p.color else 6))*64+s for s,p in sorted(board.piece_map().items())]
    first=[0.0]*width;second=[0.0]*width;third=[0.0]*width
    power2=[0.0]*width;power3=[0.0]*width
    unary=weights[0]+sum(weights[1+f]/math.sqrt(32) for f in indices)
    for f in indices:
        for r in range(width):
            v=weights[769+f*width+r]
            third[r]+=second[r]*v;second[r]+=first[r]*v;first[r]+=v
            power2[r]+=v*v;power3[r]+=v*v*v
    error=max(abs(third[r]-(first[r]**3-3*first[r]*power2[r]+2*power3[r])/6) for r in range(width))
    return unary,[v/math.sqrt(496) for v in second],[v/math.sqrt(4960) for v in third],error

def main(admission_path,output):
    admission_path,output=Path(admission_path),Path(output);assert not output.exists()
    admission=json.loads(admission_path.read_text());assert admission['id']=='wave7-fixed-basis-head-diagnostic-v1'
    sources={};bindings={};reports=[];forward_error=0;basis_error=0
    for anchor in admission['anchors']:
        raw=codec.payload(anchor['root'],anchor['checkpoint'],'network.*');assert codec.sha(raw)==anchor['networkSha256']
        array=codec.decode(raw,False,anchor['wire'])[0];weights=struct.unpack('>'+str(len(array)//8)+'d',array)
        width=anchor['width'];head=769+768*width;rows=[]
        for panel in ('fresh5','old'):
            for source in ('bt4','cp'):
                path=Path(anchor['predictions'][panel])/(source+'-predictions.jsonl')
                if str(path) not in sources:
                    sources[str(path)]=[json.loads(s) for s in path.read_text().splitlines()]
                    bindings[str(path)]=hashlib.sha256(path.read_bytes()).hexdigest()
                for i,row in enumerate(sources[str(path)]):
                    board=chess.Board(row['fen']);pieces=board.piece_map()
                    material=sum((1 if p.color==board.turn else -1)*{1:1,2:3.2,3:3.3,4:5,5:9}.get(p.piece_type,0) for p in pieces.values())
                    phase=sum({1:1,2:3,3:3,4:5,5:9}.get(p.piece_type,0) for p in pieces.values())
                    unary,q,c,e=features(board,weights,width);basis_error=max(basis_error,e)
                    values=row['predictions'];zero=values[anchor['predictor']+'-correction0.0'];base=values[anchor['predictor']]
                    forward_error=max(forward_error,abs(base-(zero+unary+sum(weights[head+r]*q[r] for r in range(width)))))
                    rows.append(dict(source=source,panel=panel,fit=panel=='fresh5' and i<1024,phase=phase,material=material,
                        target=row['target'],base=base,zero=zero,q=q,c=c))
        assert forward_error<1e-9 and basis_error<1e-8,(forward_error,basis_error)
        fitting=[row for row in rows if row['fit']];assert len(fitting)==2048
        for mode in ('quadratic','cubic','both'):
            def vector(row):return row['q'] if mode=='quadratic' else row['c'] if mode=='cubic' else row['q']+row['c']
            size=width*(2 if mode=='both' else 1);delta=[0.0]*size;first=[0.0]*size;second=[0.0]*size
            step=0;rng=random.Random(2026102107);curve=[]
            for epoch in range(8):
                order=list(range(len(fitting)));rng.shuffle(order);losses=[]
                for begin in range(0,len(order),128):
                    gradient=[0.0]*size
                    for index in order[begin:begin+128]:
                        row=fitting[index];v=vector(row);raw=row['base']+sum(d*x for d,x in zip(delta,v))
                        loss,g=fitlib.objective(raw,row['phase'],row['target']);losses.append(loss)
                        for k in range(size):gradient[k]+=g*v[k]/128
                    step+=1;c1=1-.9**step;c2=1-.999**step
                    for k,g in enumerate(gradient):
                        first[k]=.9*first[k]+.1*g;second[k]=.999*second[k]+.001*g*g
                        delta[k]-=.001*(first[k]/c1)/(math.sqrt(second[k]/c2)+1e-8)
                curve.append(statistics.mean(losses))
            assert step==128
            measurements=[]
            for panel,source in itertools.product(('fresh5','old'),('bt4','cp')):
                selected=[row for row in rows if not row['fit'] and row['panel']==panel and row['source']==source]
                for mapping in ('raw','search-half'):
                    losses=[];changes=[]
                    for row in selected:
                        change=sum(d*x for d,x in zip(delta,vector(row)))
                        if mapping=='raw':base=row['base'];gain=1
                        else:base=row['material']+.25*(row['zero']+.5*(row['base']-row['zero'])-row['material']);gain=.125
                        losses.append(fitlib.objective(base+gain*change,row['phase'],row['target'])[0]-fitlib.objective(base,row['phase'],row['target'])[0]);changes.append(change*change)
                    measurements.append(dict(panel=panel,source=source,mapping=mapping,count=len(selected),meanCrossEntropyChange=statistics.mean(losses),standardError=statistics.stdev(losses)/math.sqrt(len(losses)),changeRmsPawns=math.sqrt(statistics.mean(changes))))
            reports.append(dict(anchor=anchor['name'],mode=mode,parameters=size,steps=step,headDelta=delta,epochLoss=curve,metrics=measurements))
    with output.open('x',encoding='utf-8',newline='\n') as f:
        json.dump(dict(id=admission['id']+'-results',admission=str(admission_path),admissionSha256=hashlib.sha256(admission_path.read_bytes()).hexdigest(),inputs=bindings,maximumJavaForwardError=forward_error,maximumThirdMomentIdentityError=basis_error,arms=reports,
            limitations=['Consumed data; fixed bounded diagnostic, not independent generalization or chess strength.','Only new head deltas optimized; anchors, embeddings, original optimizer states and owner artifacts unchanged.','Ranks have different ancestry and maturity; causal comparisons are within each anchor.','New zero moments/step for diagnostic head deltas, not continuation of inherited Adam.','Only128 updates per arm; no conclusion about optimized convergence.']),f,indent=2);f.write('\n')
    print('Six fixed-basis arms completed; forward/basis errors',forward_error,basis_error)

if __name__=='__main__':main(*sys.argv[1:])
