"""Read-only status/outlier sensitivity of the closed fixed-basis diagnostic.

No refitting, checkpoint selection or owner writes. Both fitted head controls are
reported on every panel and status subset; the original all-row results remain.
"""
import json
import math
from pathlib import Path
import statistics
import struct
import sys
import importlib.util

spec=importlib.util.spec_from_file_location('heads',Path(__file__).with_name('probe-brn-fixed-basis-heads.py'))
h=importlib.util.module_from_spec(spec);spec.loader.exec_module(h)

def main(base,output):
    base=Path(base);admission=json.loads((base/'wave7-fixed-head-admission.json').read_text())
    results=json.loads((base/'wave7-fixed-head-results.json').read_text())
    anchor=admission['anchors'][0];assert anchor['name']=='Q4'
    raw=h.codec.payload(anchor['root'],anchor['checkpoint'],'network.*')
    assert h.codec.sha(raw)==anchor['networkSha256']
    weights=struct.unpack('>3845d',h.codec.decode(raw,False,8)[0])
    arms=[a for a in results['arms'] if a['anchor']=='Q4' and a['mode'] in ('quadratic','cubic')]
    metrics=[];fit=[]
    for panel in ('fresh5','old'):
        for source in ('bt4','cp'):
            path=Path(anchor['predictions'][panel])/(source+'-predictions.jsonl')
            assert h.codec.sha(path.read_bytes())==results['inputs'][str(path)]
            rows=[]
            for i,line in enumerate(path.read_text().splitlines()):
                row=json.loads(line);board=h.chess.Board(row['fen']);pieces=board.piece_map()
                phase=sum({1:1,2:3,3:3,4:5,5:9}.get(p.piece_type,0) for p in pieces.values())
                material=sum((1 if p.color==board.turn else -1)*{1:1,2:3.2,3:3.3,4:5,5:9}.get(p.piece_type,0) for p in pieces.values())
                _,q,c,_=h.features(board,weights,4)
                pred=row['predictions'];value=pred[anchor['predictor']];zero=pred[anchor['predictor']+'-correction0.0']
                loss,g=h.fitlib.objective(value,phase,row['target'])
                item=dict(valid=board.is_valid(),phase=phase,material=material,target=row['target'],base=value,zero=zero,q=q,c=c,loss=loss,gradient=[g*v for v in c])
                if panel=='fresh5' and i<1024:fit.append(item)
                else:rows.append(item)
            for arm in arms:
                for subset in ('all','valid','nonstandard'):
                    selected=[r for r in rows if subset=='all' or r['valid']==(subset=='valid')]
                    if not selected:continue
                    for mapping in ('raw','search-half'):
                        changes=[]
                        for r in selected:
                            change=sum(a*b for a,b in zip(arm['headDelta'],r['q'] if arm['mode']=='quadratic' else r['c']))
                            value=r['base'] if mapping=='raw' else r['material']+.25*(r['zero']+.5*(r['base']-r['zero'])-r['material'])
                            gain=1 if mapping=='raw' else .125
                            changes.append(h.fitlib.objective(value+gain*change,r['phase'],r['target'])[0]-h.fitlib.objective(value,r['phase'],r['target'])[0])
                        metrics.append(dict(panel=panel,source=source,mode=arm['mode'],subset=subset,mapping=mapping,count=len(selected),meanCrossEntropyChange=statistics.mean(changes)))
    initial=[]
    for subset in ('all','valid','nonstandard'):
        selected=[r for r in fit if subset=='all' or r['valid']==(subset=='valid')]
        if selected:initial.append(dict(subset=subset,count=len(selected),meanInitialLoss=statistics.mean(r['loss'] for r in selected),maximumInitialLoss=max(r['loss'] for r in selected),absoluteCubicGradientSum=sum(sum(abs(g) for g in r['gradient']) for r in selected)))
    out=dict(id='wave7-fixed-head-status-sensitivity-v1',inputs={str(base/n):h.codec.sha((base/n).read_bytes()) for n in ('wave7-fixed-head-admission.json','wave7-fixed-head-results.json')},metrics=metrics,initialFitSensitivity=initial,limitations=['Post-diagnostic sensitivity; no refit or independent data.','Nonstandard status is a sensitivity classification, not a new training filter.','Tiny prediction changes do not establish chess strength or optimized convergence.'])
    with Path(output).open('x',encoding='utf-8',newline='\n') as f:json.dump(out,f,indent=2);f.write('\n')
    print(json.dumps(initial))

if __name__=='__main__':main(*sys.argv[1:])
