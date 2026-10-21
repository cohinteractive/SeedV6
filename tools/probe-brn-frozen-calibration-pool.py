"""Four bounded head-only fits isolate clipping from affine recalibration.

Fixed Q4 piece-sum basis and fit-only normalization. Gen32/BRE176 bases remain
exact; linear and clipped arms have four fresh output parameters each.
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

s=importlib.util.spec_from_file_location('pool',Path(__file__).with_name('probe-brn-clipped-pool.py'))
pool=importlib.util.module_from_spec(s);s.loader.exec_module(pool);pilot=pool.pilot;basis=pool.basis

def main(admission_path,output):
    admission_path,output=map(Path,[admission_path,output]);assert not output.exists()
    a=pilot.read(admission_path);assert a['id']=='wave9-frozen-calibration-pool-v1'
    assert pilot.sha(Path(__file__))==a['scriptSha256']
    model=a['anchor'];data=basis.codec.payload(model['root'],model['checkpoint'],'network.*');assert basis.codec.sha(data)==model['networkSha256']
    w=struct.unpack('>3849d',basis.codec.decode(data,False,11)[0]);embedding=w[769:3841];rows=[];bindings={}
    prior=pilot.read(Path(a['priorResult']));assert pilot.sha(Path(a['priorResult']))==a['priorResultSha256']
    centers=prior['featureCenters'];widths=prior['featureWidths']
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
                x=[.5+(sum(embedding[4*f+k] for f in indices)-centers[k])/widths[k] for k in range(4)]
                rows.append(dict(panel=panel,source=source,index=i,fit=panel=='fresh5' and i<1024,x=x,material=m,phase=phase,target=r['target'],
                    Gen32=r['predictions']['Gen32'],continued=r['predictions']['W9-BRE-CONTROL-LR001-g176']))
    fitting=[r for r in rows if r['fit']];assert len(fitting)==2043;reports=[];b=a['budget'];max_error=0
    for base,clipped in itertools.product(['Gen32','continued'],[False,True]):
        for r in rows:r['features']=[pool.activation(x,clipped)[0] for x in r['x']]
        heads=[0.0]*4;first=[0.0]*4;second=[0.0]*4;rng=random.Random(b['seed']);step=0;curve=[]
        for r in fitting[:3]:
            _,g=basis.fitlib.objective(r[base]+.5*.03*sum(r['features']),r['phase'],r['target']);h=1e-5
            for k in range(4):
                scores=[r[base]+.5*(.03*sum(r['features'])+sign*h*r['features'][k]) for sign in [1,-1]]
                losses=[basis.fitlib.objective(score,r['phase'],r['target'])[0] for score in scores]
                max_error=max(max_error,abs((losses[0]-losses[1])/(2*h)-.5*g*r['features'][k]))
        assert max_error<1e-7
        for epoch in range(8):
            order=list(range(len(fitting)));rng.shuffle(order);losses=[]
            for start in range(0,len(order),128):
                ids=order[start:start+128];gradient=[0.0]*4
                for ix in ids:
                    r=fitting[ix];loss,g=basis.fitlib.objective(r[base]+.5*sum(c*x for c,x in zip(heads,r['features'])),r['phase'],r['target']);losses.append(loss)
                    for k in range(4):gradient[k]+=.5*g*r['features'][k]/len(ids)
                step+=1
                for k,g in enumerate(gradient):
                    g+=b['l2']*heads[k];first[k]=.9*first[k]+.1*g;second[k]=.999*second[k]+.001*g*g
                    heads[k]-=b['rate']*(first[k]/(1-.9**step))/(math.sqrt(second[k]/(1-.999**step))+1e-8)
            curve.append(st.mean(losses))
        assert step==128;metrics=[]
        for panel,source,mapping in itertools.product(a['predictions'],['bt4','cp'],['raw','quarter-search']):
            selected=[r for r in rows if r['panel']==panel and r['source']==source and (panel=='new' or r['index']>=1024)];losses=[];changes=[]
            for r in selected:
                gain=1 if mapping=='raw' else .25;offset=r['material']+gain*(r[base]-r['material']);delta=.5*gain*sum(c*x for c,x in zip(heads,r['features']))
                losses.append(basis.fitlib.objective(offset+delta,r['phase'],r['target'])[0]-basis.fitlib.objective(offset,r['phase'],r['target'])[0]);changes.append(delta)
            metrics.append(dict(panel=panel,source=source,mapping=mapping,count=len(selected),crossEntropyChange=st.mean(losses),
                descriptiveRowSE=st.stdev(losses)/math.sqrt(len(losses)),correctionRmsPawns=math.sqrt(st.mean(x*x for x in changes))))
        reports.append(dict(base=base,clipped=clipped,steps=step,heads=heads,epochLoss=curve,metrics=metrics))
    result=dict(id=a['id']+'-results',admission=str(admission_path),admissionSha256=pilot.sha(admission_path),inputs=bindings,
        maximumFiniteDifferenceError=max_error,arms=reports,limitations=['Post-hoc frozen-calibration mechanism diagnostic; all windows are consumed development evidence.',
        'Only128 updates and four trainable heads; no convergence, architecture-wide or strength claim.',
        'Fixed basis and fit-only normalization inherited from the prior pilot. Raw objective and quarter-search function differ.',
        'No model/optimizer export, source replacement, owner writes or automatic training-wave admission.'])
    with output.open('x',encoding='utf-8',newline='\n') as f:json.dump(result,f,indent=2);f.write('\n')
    print('Completed four bounded frozen-calibration pool diagnostics')
if __name__=='__main__':main(*sys.argv[1:])
