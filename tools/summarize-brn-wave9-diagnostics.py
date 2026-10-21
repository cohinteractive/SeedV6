"""Summarize trained G9 coefficients, consumed-panel predictions and cache parity."""
import importlib.util
import json
import math
from pathlib import Path
import statistics as st
import struct
import sys
import chess

BASE=Path('docs/research/brn-successor-cglhw')
def module(name,file):
    spec=importlib.util.spec_from_file_location(name,Path(__file__).with_name(file))
    m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m
basis=module('basis','probe-brn-fixed-basis-heads.py')
def read(p): return json.loads(p.read_text(encoding='utf-8-sig'))
def sha(p): return basis.codec.sha(p.read_bytes())
def main(output):
    output=Path(output);assert not output.exists()
    audit=read(BASE/'raw/wave9/trained-audit-01/stores.json')
    bindings={};coefficients=[]
    for store,wire in zip(audit[1:4],[11,12,13]):
        trajectory=[]
        for cp in store['chain']:
            raw=basis.codec.payload(store['root'],cp['id'],'network.*')
            data=basis.codec.decode(raw,False,wire)[0];weights=struct.unpack('>'+str(len(data)//8)+'d',data)
            h=weights[3845:]
            trajectory.append(dict(generation=cp['generation'],heads=h,
                searchConstant=h[0]/8,searchMaterialCoefficient=1+h[1]/64,
                searchBaseResidualCoefficient=.25+h[2]/32,searchQAnchorCoefficient=(1+h[3])/8))
        coefficients.append(dict(name=store['name'],trajectory=trajectory))
    panels=[]
    for panel in ['fresh5','old']:
        for source in ['bt4','cp']:
            p=BASE/f'raw/wave9/trained-predictions-{panel}-01/{source}-predictions.jsonl';bindings[str(p)]=sha(p)
            rows=[]
            for i,line in enumerate(p.read_text(encoding='utf-8').splitlines()):
                if i<1024:continue
                row=json.loads(line);board=chess.Board(row['fen'])
                if not board.is_valid():continue
                pieces=board.piece_map()
                material=sum((1 if p.color==board.turn else -1)*{1:1,2:3.2,3:3.3,4:5,5:9}.get(p.piece_type,0) for p in pieces.values())
                phase=sum({1:1,2:3,3:3,4:5,5:9}.get(p.piece_type,0) for p in pieces.values())
                rows.append((row,material,phase))
            metrics={};values={}
            for name in rows[0][0]['predictions']:
                losses=[];scores=[];residuals=[]
                for row,m,phase in rows:
                    v=m+.25*(row['predictions'][name]-m)
                    losses.append(basis.fitlib.objective(v,phase,row['target'])[0]);scores.append(v);residuals.append(v-m)
                values[name]=scores
                metrics[name]=dict(crossEntropy=st.mean(losses),scoreMeanPawns=st.mean(scores),
                    scoreRmsPawns=math.sqrt(st.mean(v*v for v in scores)),residualRmsPawns=math.sqrt(st.mean(v*v for v in residuals)),
                    minPawns=min(scores),maxPawns=max(scores),beyondSearchClamp=sum(abs(v)>=325.11 for v in scores))
            names=[s['name']+'-g176' for s in audit[1:4]]
            contrasts=[]
            pairs=[(names[1],names[0]),(names[2],names[0]),(names[2],names[1])]
            pairs += [(n,n+'-affine-only') for n in names[1:]]
            pairs += [(n,n.replace('-g176','-g168')) for n in names]
            for left,right in pairs:
                ds=[a-b for a,b in zip(values[left],values[right])]
                contrasts.append(dict(left=left,right=right,
                    crossEntropyDifference=metrics[left]['crossEntropy']-metrics[right]['crossEntropy'],
                    meanScoreChangePawns=st.mean(ds),rmsScoreChangePawns=math.sqrt(st.mean(d*d for d in ds)),
                    maxAbsoluteChangePawns=max(map(abs,ds))))
            panels.append(dict(panel=panel,source=source,statusValidEvaluationRows=len(rows),metrics=metrics,contrasts=contrasts))
    caches=[]
    for label in ['affine','quad','cubic']:
        p=BASE/f'raw/wave9/trained-cache-{label}-01.json';d=read(p);bindings[str(p)]=sha(p)
        assert d['completed'] and d['integerDifferences']==d['freshIntegerDifferences']==0
        assert d['maximumRawError']<1e-9
        searches=d['searches'];groups={}
        for s in searches:groups.setdefault((s['root'],s['pass']),[]).append(s)
        assert len(groups)==48
        for group in groups.values():
            assert len(group)==2
            assert group[0]['result']==group[1]['result']
        caches.append({k:d[k] for k in ['candidateName','positions','maximumRawError','maximumFreshRawError',
            'integerDifferences','freshIntegerDifferences','candidateWeightBytes','combinedPayloadBytes',
            'combinedImmutablePrimitiveBytes','accumulatorPrimitiveBytes','cacheDetails','transitionLatency']}
            |dict(fullCacheSearchPairs=len(groups)))
    result=dict(id='wave9-trained-diagnostics-v1',inputs=bindings,coefficients=coefficients,panels=panels,cache=caches,
        limitations=['Consumed evaluation halves, status-valid rows only; source/game/training overlap unknown. No independent generalization or strength claim.',
        'New heads optimize search-pawn loss, BRE optimizes raw-pawn loss; these panel comparisons apply common search mapping.',
        'Best175 is separately reported; Latest176 remains prospectively selected. Masks lack separate adaptation.',
        'Changing generation validation panels do not prove convergence. Affine explains most fitting improvement; small degree differences vary by panel.',
        'Microbenchmarks are warmed isolated measurements, not practical strength or proportional NPS guarantees.',
        'Diagnostic preparation initially named unavailable historical input filenames; it stopped before that probe, then used the preserved actual files. No owner or completed output changed.'])
    with output.open('x',encoding='utf-8',newline='\n') as f:json.dump(result,f,indent=2);f.write('\n')
    print('Verified three trained cache oracles; four consumed panels; coefficient trajectories169..176 plus bootstrap')
if __name__=='__main__':main(*sys.argv[1:])
