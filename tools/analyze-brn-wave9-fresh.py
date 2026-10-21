"""Prespecified read-only post176 prediction and head-stability check.

All new ordinal-window rows are evaluation, not fit data. Fixed four-generation
averages diagnose stability and are never written as checkpoints/Adam states.
"""
import importlib.util
import itertools
import json
import math
from pathlib import Path
import statistics as st
import struct
import sys
import chess

s=importlib.util.spec_from_file_location('pilot',Path(__file__).with_name('probe-brn-continued-base-heads.py'))
pilot=importlib.util.module_from_spec(s);s.loader.exec_module(pilot);basis=pilot.basis

def main(admission_path,output):
    admission_path,output=map(Path,[admission_path,output]);assert not output.exists()
    a=pilot.read(admission_path);assert a['id']=='wave9-new-window-stability-check-v1'
    assert pilot.sha(Path(__file__))==a['scriptSha256']
    trained=pilot.read(Path(a['trainedEvidence']));audit=pilot.read(Path(a['audit']))
    weights={};average={};source_hashes={};prefix=None
    for lineage,store,wire in zip(trained['lineages'][:3],audit[1:4],[11,12,13]):
        name=lineage['name'];root=Path(a['frozen'])/name;heads=[]
        for cp in store['chain']:
            if cp['generation'] not in [173,174,175,176]:continue
            raw=basis.codec.payload(str(root),cp['id'],'network.*');assert basis.codec.sha(raw)==cp['networkSha256']
            encoded=basis.codec.decode(raw,False,wire)[0]
            if prefix is None:prefix=encoded[:3845*8]
            assert prefix==encoded[:3845*8]
            w=struct.unpack('>'+str(len(encoded)//8)+'d',encoded)
            heads.append(w[3845:]);source_hashes[cp['id']]=cp['networkSha256']
            if cp['generation']==176:weights[name]=w
        assert len(heads)==4
        average[name]=[st.mean(v[k] for v in heads) for k in range(len(heads[0]))]
    reports=[];bindings={};maximum_oracle_error=0
    for source in ['bt4','cp']:
        p=Path(a['predictions'])/(source+'-predictions.jsonl');bindings[str(p)]=pilot.sha(p)
        values={};scores={};metadata=[];invalid=0
        for line in p.read_text(encoding='utf-8').splitlines():
            r=json.loads(line);board=chess.Board(r['fen'])
            if not board.is_valid():invalid+=1;continue
            pieces=board.piece_map();m=sum((1 if v.color==board.turn else -1)*{1:1,2:3.2,3:3.3,4:5,5:9}.get(v.piece_type,0) for v in pieces.values())
            phase=sum({1:1,2:3,3:3,4:5,5:9}.get(v.piece_type,0) for v in pieces.values())
            v=r['predictions'];name=trained['lineages'][0]['name'];unary,q,c,_=basis.features(board,weights[name],4)
            anchor=unary+sum(weights[name][3841+k]*q[k] for k in range(4))
            g=v[name+'-g176-zero-heads']-.5*anchor;affine=[1,m/8,(g-m)/4,anchor]
            raw_models={'Gen32':g,'BRE176':v['W9-BRE-CONTROL-LR001-g176'],'initial168':g+.5*anchor}
            for name,w in weights.items():
                degree=[] if 'AFFINE' in name else q if '-QUAD-' in name else c
                def predict(h):return g+.5*(anchor+sum(x*y for x,y in zip(h,affine+degree)))
                raw_models[name+'-latest']=predict(w[3845:])
                maximum_oracle_error=max(maximum_oracle_error,abs(raw_models[name+'-latest']-v[name+'-g176']))
                raw_models[name+'-best']=v[name+'-best']
                raw_models[name+'-mean173to176']=predict(average[name])
                if degree:raw_models[name+'-degreeMeanOnly']=predict(list(w[3845:3849])+average[name][4:])
            for name,raw in raw_models.items():
                for scale in [1,.25]:
                    score=m+.25*scale*(raw-m);key=name+('@native' if scale==1 else '@search025')
                    values.setdefault(key,[]).append(basis.fitlib.objective(score,phase,r['target'])[0])
                    scores.setdefault(key,[]).append(score)
            metadata.append((len(pieces),phase,r['target'],m))
        assert len(metadata)+invalid==2048
        metrics={k:dict(crossEntropy=st.mean(v),scoreRmsPawns=math.sqrt(st.mean(x*x for x in scores[k])),
            minPawns=min(scores[k]),maxPawns=max(scores[k])) for k,v in values.items()}
        contrasts=[]
        names=[l['name'] for l in trained['lineages'][:3]]
        pairs=[(names[1]+'-latest',names[0]+'-latest'),(names[2]+'-latest',names[0]+'-latest'),(names[2]+'-latest',names[1]+'-latest')]
        for name in names:
            pairs.extend([(name+'-latest',name+'-best'),(name+'-mean173to176',name+'-latest')])
            if 'AFFINE' not in name:pairs.append((name+'-degreeMeanOnly',name+'-latest'))
        for left,right in pairs:
            for scale in ['@native','@search025']:
                ds=[x-y for x,y in zip(values[left+scale],values[right+scale])]
                contrasts.append(dict(left=left+scale,right=right+scale,crossEntropyDifference=st.mean(ds),descriptiveRowSE=st.stdev(ds)/math.sqrt(len(ds))))
        reports.append(dict(source=source,validRows=len(metadata),invalidRowsExcluded=invalid,meanPieces=st.mean(v[0] for v in metadata),
            meanMaterialPhase=st.mean(v[1] for v in metadata),nearDecisiveTargetFraction=st.mean(abs(v[2])>.9 for v in metadata),metrics=metrics,contrasts=contrasts))
    assert maximum_oracle_error<1e-9
    report=dict(id=a['id']+'-results',admission=str(admission_path),admissionSha256=pilot.sha(admission_path),inputs=bindings,
        averageSourceHashes=source_hashes,averageHeads=average,maximumIndependentForwardError=maximum_oracle_error,panels=reports,
        limitations=['All rows are new-window evaluation for these fixed candidates; ordinal exclusion is not game/FEN independence or chess strength.',
        'Row standard errors are descriptive and ignore source/game clustering and multiple comparisons.',
        'Uniform173..176 and degree-only averages were fixed before these labels; no checkpoint/optimizer export or runtime/search-parity claim for averages.',
        'Native and fixed.25 residual scale are separate search functions. Loss is evaluated on that function, not directly comparable GUI objectives across families.',
        'No training, owner cursor mutation, source replacement or independent strength confirmation.'])
    with output.open('x',encoding='utf-8',newline='\n') as f:json.dump(report,f,indent=2);f.write('\n')
    print('Verified new-window predictions and fixed head-stability controls, no fitting')
if __name__=='__main__':main(*sys.argv[1:])
