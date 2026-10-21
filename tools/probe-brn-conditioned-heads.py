"""Bounded feasibility diagnostic for phase-conditioned quadratic heads.

No model export, owner writes, or substantive training. Six128-update arms on
2043 previously consumed status-valid rows. All results and negative cells retained.
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
basis=module('basis','probe-brn-fixed-basis-heads.py')

def main(admission_path,output):
    admission_path,output=Path(admission_path),Path(output);assert not output.exists()
    admission=json.loads(admission_path.read_text());assert admission['id'] in ('wave8-phase-head-feasibility-v1','wave8-affine-head-control-v1','wave8-calibrated-degree-control-v1')
    affine = admission['id']=='wave8-affine-head-control-v1'
    joint = admission['id']=='wave8-calibrated-degree-control-v1'
    anchor=admission['anchor'];raw=basis.codec.payload(anchor['root'],anchor['checkpoint'],'network.*')
    assert basis.codec.sha(raw)==anchor['networkSha256']
    data=basis.codec.decode(raw,False,8)[0];weights=struct.unpack('>3845d',data)
    rows=[];bindings={};forward_error=0
    for panel,source in itertools.product(('fresh5','old'),('bt4','cp')):
        p=Path(anchor['predictions'][panel])/(source+'-predictions.jsonl');bindings[str(p)]=basis.codec.sha(p.read_bytes())
        for i,line in enumerate(p.read_text().splitlines()):
            row=json.loads(line);board=chess.Board(row['fen'])
            if not board.is_valid():continue
            pieces=board.piece_map();phase=sum({1:1,2:3,3:3,4:5,5:9}.get(p.piece_type,0) for p in pieces.values())
            context=2*min(24,sum({2:1,3:1,4:2,5:4}.get(p.piece_type,0) for p in pieces.values()))/24-1
            material=sum((1 if p.color==board.turn else -1)*{1:1,2:3.2,3:3.3,4:5,5:9}.get(p.piece_type,0) for p in pieces.values())
            unary,q,c,_=basis.features(board,weights,4);v=row['predictions']
            base=v[anchor['predictor']];zero=v[anchor['predictor']+'-correction0.0']
            forward_error=max(forward_error,abs(base-(zero+unary+sum(weights[3841+r]*q[r] for r in range(4)))))
            rows.append(dict(panel=panel,source=source,fit=panel=='fresh5' and i<1024,
                phase=phase,material=material,target=row['target'],base=base,zero=zero,
                quadratic=q,cubic=c,phaseQuadratic=[x*context for x in q],
                affine=[1.0,material/8,(zero-material)/4,base-zero]))
    for row in rows:
        row['affineQuadratic']=row['affine']+row['quadratic']
        row['affineCubic']=row['affine']+row['cubic']
    assert forward_error<1e-9
    fitting=[r for r in rows if r['fit']];assert len(fitting)==2043
    reports=[]
    modes=('affine','affineQuadratic','affineCubic') if joint else ('affine',) if affine else ('quadratic','cubic','phaseQuadratic')
    objectives=('search-half',) if joint else ('raw','search-half')
    for mode,objective in itertools.product(modes,objectives):
        def mapped(row,mapping):
            if mapping=='raw':return row['base'],1
            return row['material']+.25*(row['zero']+.5*(row['base']-row['zero'])-row['material']),.125
        size=len(fitting[0][mode])
        delta=[0.0]*size;first=[0.0]*size;second=[0.0]*size;rng=random.Random(2026102208);step=0;curve=[]
        for epoch in range(8):
            order=list(range(len(fitting)));rng.shuffle(order);losses=[]
            for begin in range(0,len(order),128):
                ids=order[begin:begin+128];gradient=[0.0]*size
                for ix in ids:
                    row=fitting[ix];offset,gain=mapped(row,objective);v=row[mode]
                    loss,d=basis.fitlib.objective(offset+gain*sum(a*x for a,x in zip(delta,v)),row['phase'],row['target']);losses.append(loss)
                    for k in range(size):gradient[k]+=d*gain*v[k]/len(ids)
                step+=1;c1=1-.9**step;c2=1-.999**step
                for k,g in enumerate(gradient):
                    first[k]=.9*first[k]+.1*g;second[k]=.999*second[k]+.001*g*g
                    delta[k]-=.001*(first[k]/c1)/(math.sqrt(second[k]/c2)+1e-8)
            curve.append(statistics.mean(losses))
        assert step==128
        measurements=[]
        for panel,source,mapping in itertools.product(('fresh5','old'),('bt4','cp'),('raw','search-half')):
            selected=[r for r in rows if r['panel']==panel and r['source']==source and not r['fit']]
            changes=[];squares=[]
            for row in selected:
                offset,gain=mapped(row,mapping);change=gain*sum(a*x for a,x in zip(delta,row[mode]))
                changes.append(basis.fitlib.objective(offset+change,row['phase'],row['target'])[0]-basis.fitlib.objective(offset,row['phase'],row['target'])[0]);squares.append(change*change)
            measurements.append(dict(panel=panel,source=source,mapping=mapping,count=len(selected),
                meanCrossEntropyChange=statistics.mean(changes),standardError=statistics.stdev(changes)/math.sqrt(len(changes)),changeRmsPawns=math.sqrt(statistics.mean(squares))))
        reports.append(dict(mode=mode,objective=objective,parameters=size,fitRows=len(fitting),steps=step,headDelta=delta,epochLoss=curve,metrics=measurements))
    with output.open('x',encoding='utf-8',newline='\n') as f:
        json.dump(dict(id=admission['id']+'-results',admission=str(admission_path),admissionSha256=basis.codec.sha(admission_path.read_bytes()),inputs=bindings,
            maximumJavaForwardError=forward_error,arms=reports,limitations=[
            'Previously consumed status-valid panels; no independent generalization, optimized convergence or strength claim.',
            'Same four-parameter budget, but basis conditioning differs; nominal optimization is not equal effective optimization.',
            'Both objectives reported; raw trains the current function, search-half is a diagnostic only and changes the objective.',
            'No checkpoint or optimizer exported; no owner/source artifacts changed.']),f,indent=2);f.write('\n')
    print(len(reports),'bounded conditioned-head arms completed; maximum forward error',forward_error)

if __name__=='__main__':main(*sys.argv[1:])
