"""Bounded, non-exported head-fit diagnostic on frozen Gen32 and continued BRE.

Six prespecified 128-update fits on consumed data distinguish anchor quality from
the fixed feature basis. No owner writes or competitive neural training.
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

s=importlib.util.spec_from_file_location('basis',Path(__file__).with_name('probe-brn-fixed-basis-heads.py'))
basis=importlib.util.module_from_spec(s);s.loader.exec_module(basis)
def read(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def sha(p):return basis.codec.sha(p.read_bytes())

def main(admission_path,output):
    admission_path,output=map(Path,[admission_path,output]);assert not output.exists()
    a=read(admission_path);assert a['id']=='wave9-continued-base-head-feasibility-v1'
    assert sha(Path(__file__))==a['scriptSha256']
    model=a['anchor'];data=basis.codec.payload(model['root'],model['checkpoint'],'network.*')
    assert basis.codec.sha(data)==model['networkSha256']
    w=struct.unpack('>3849d',basis.codec.decode(data,False,11)[0]);rows=[];bindings={};error=0
    for panel,source in itertools.product(['fresh5','old'],['bt4','cp']):
        p=Path(a['predictions'][panel])/(source+'-predictions.jsonl');bindings[str(p)]=sha(p)
        assert bindings[str(p)]==a['inputs'][str(p)]
        for i,line in enumerate(p.read_text(encoding='utf-8').splitlines()):
            r=json.loads(line);board=chess.Board(r['fen'])
            if not board.is_valid():continue
            pieces=board.piece_map()
            m=sum((1 if v.color==board.turn else -1)*{1:1,2:3.2,3:3.3,4:5,5:9}.get(v.piece_type,0) for v in pieces.values())
            phase=sum({1:1,2:3,3:3,4:5,5:9}.get(v.piece_type,0) for v in pieces.values())
            unary,q,c,e=basis.features(board,w,4);anchor=unary+sum(w[3841+j]*q[j] for j in range(4))
            v=r['predictions'];g=v[a['predictor']+'-zero-heads']-.5*anchor
            error=max(error,abs(g-v['Gen32']),e)
            rows.append(dict(panel=panel,source=source,index=i,fit=panel=='fresh5' and i<1024,
                material=m,phase=phase,target=r['target'],q=q,c=c,anchor=anchor,
                Gen32=g,continued=v['W9-BRE-CONTROL-LR001-g176']))
    assert error<1e-6
    fitting=[r for r in rows if r['fit']];assert len(fitting)==2043
    reports=[];b=a['budget'];assert b['epochs']==8 and b['batch']==128
    for base,mode in itertools.product(['Gen32','continued'],['affine','quadratic','cubic']):
        def vector(r):
            common=[1,r['material']/8,(r[base]-r['material'])/4,r['anchor']]
            return common+([] if mode=='affine' else r['q'] if mode=='quadratic' else r['c'])
        size=len(vector(fitting[0]));fv=[vector(r) for r in fitting]
        means=[0]+[st.mean(v[k] for v in fv) for k in range(1,size)]
        scales=[1]+[math.sqrt(st.mean((v[k]-means[k])**2 for v in fv)) for k in range(1,size)]
        assert min(scales)>1e-12
        standardized=lambda r:[(v-m)/s for v,m,s in zip(vector(r),means,scales)]
        for r in rows:r['x']=standardized(r)
        coefficients=[0.0]*size;first=[0.0]*size;second=[0.0]*size
        rng=random.Random(b['seed']);step=0;curve=[]
        for epoch in range(b['epochs']):
            order=list(range(len(fitting)));rng.shuffle(order);losses=[]
            for start in range(0,len(order),b['batch']):
                ids=order[start:start+b['batch']];gradient=[0.0]*size
                for ix in ids:
                    r=fitting[ix];raw=r[base]+.5*sum(c*x for c,x in zip(coefficients,r['x']))
                    loss,d=basis.fitlib.objective(raw,r['phase'],r['target']);losses.append(loss)
                    for k in range(size):gradient[k]+=.5*d*r['x'][k]/len(ids)
                step+=1;c1=1-.9**step;c2=1-.999**step
                for k,g in enumerate(gradient):
                    g+=b['l2']*coefficients[k]
                    first[k]=.9*first[k]+.1*g;second[k]=.999*second[k]+.001*g*g
                    coefficients[k]-=b['rate']*(first[k]/c1)/(math.sqrt(second[k]/c2)+1e-8)
            curve.append(st.mean(losses))
        assert step==128
        metrics=[]
        for panel,source in itertools.product(['fresh5','old'],['bt4','cp']):
            selected=[r for r in rows if r['panel']==panel and r['source']==source and r['index']>=1024]
            for mapping in ['raw','quarter-search']:
                losses=[];changes=[];masked=[]
                for r in selected:
                    gain=1 if mapping=='raw' else .25
                    offset=r['material']+gain*(r[base]-r['material'])
                    correction=.5*gain*sum(c*x for c,x in zip(coefficients,r['x']))
                    affine=.5*gain*sum(c*x for c,x in zip(coefficients[:4],r['x'][:4]))
                    original=basis.fitlib.objective(offset,r['phase'],r['target'])[0]
                    losses.append(basis.fitlib.objective(offset+correction,r['phase'],r['target'])[0]-original)
                    masked.append(basis.fitlib.objective(offset+affine,r['phase'],r['target'])[0]-original)
                    changes.append(correction)
                metrics.append(dict(panel=panel,source=source,mapping=mapping,count=len(selected),
                    crossEntropyChange=st.mean(losses),standardError=st.stdev(losses)/math.sqrt(len(losses)),
                    changeRmsPawns=math.sqrt(st.mean(x*x for x in changes)),
                    unadaptedDegreeMaskDifference=st.mean(losses)-st.mean(masked)))
        reports.append(dict(base=base,mode=mode,parameters=size,steps=step,epochLoss=curve,
            featureMeans=means,featureScales=scales,coefficients=coefficients,metrics=metrics))
    result=dict(id=a['id']+'-results',admission=str(admission_path),admissionSha256=sha(admission_path),
        inputs=bindings,maximumIndependentBasisError=error,arms=reports,
        limitations=['Consumed small panels, source/game/training overlap unknown; no independent prediction or strength claim.',
        'Only128 updates per arm with fit-only normalization; not converged optimal configurations or production optimizer continuation.',
        'Raw objective matches BRE convention; quarter-search metrics diagnose the distinct deployment function.',
        'Bases differ in exposure/history by design; within-base calibrated affine and quadratic are adapted controls.',
        'Degree masks are unadapted attribution checks; no model/optimizer export or owner artifact mutation.'])
    with output.open('x',encoding='utf-8',newline='\n') as f:json.dump(result,f,indent=2);f.write('\n')
    print('Completed six bounded continued-base/Gen32 head diagnostics; no model export')
if __name__=='__main__':main(*sys.argv[1:])
