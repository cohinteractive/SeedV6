"""Separate common calibration from sparse-pawn contributions in the bounded pilot.

No fitting. Masks are unadapted sensitivity diagnostics, not independent controls.
"""
import hashlib
import importlib.util
import json
import math
from pathlib import Path
import statistics as st
import sys
import chess

s=importlib.util.spec_from_file_location('pilot',Path(__file__).with_name('probe-brn-pawn-conditions.py'))
pilot=importlib.util.module_from_spec(s);s.loader.exec_module(pilot)
def read(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main(admission_path,result_path,output):
    admission_path,result_path,output=map(Path,[admission_path,result_path,output]);assert not output.exists()
    a,r=read(admission_path),read(result_path);assert r['admissionSha256']==sha(admission_path)
    findings=[]
    for panel in ['fresh5','old']:
        for source in ['bt4','cp']:
            p=Path(a['predictions'][panel])/(source+'-predictions.jsonl');assert sha(p)==r['inputs'][str(p)]
            accum={(arm['mode'],mask):[] for arm in r['arms'] for mask in ['all','affine-only','pawn-only']}
            pawn_counts=[];blocker_counts=[];multi_blockers=0;pawn_total=0
            for i,line in enumerate(p.read_text(encoding='utf-8').splitlines()):
                if i<1024:continue
                row=json.loads(line);b=chess.Board(row['fen'])
                if not b.is_valid():continue
                pieces=b.piece_map();material=sum((1 if v.color==b.turn else -1)*{1:1,2:3.2,3:3.3,4:5,5:9}.get(v.piece_type,0) for v in pieces.values())
                phase=sum({1:1,2:3,3:3,4:5,5:9}.get(v.piece_type,0) for v in pieces.values())
                v=row['predictions'];g=v['Gen32'];q=2*(v[a['predictor']+'-zero-heads']-g)
                baseline=material+.25*(v[a['predictor']]-material);base_loss=pilot.fitlib.objective(baseline,phase,row['target'])[0]
                affine=[1,material/8,(g-material)/4,q]
                for square,piece in pieces.items():
                    if piece.piece_type!=chess.PAWN:continue
                    blockers=sum(o.piece_type==chess.PAWN and o.color!=piece.color and
                        abs(chess.square_file(t)-chess.square_file(square))<=1 and
                        ((chess.square_rank(t)>chess.square_rank(square)) if piece.color else
                         (chess.square_rank(t)<chess.square_rank(square))) for t,o in pieces.items())
                    pawn_total+=1;multi_blockers+=blockers>=2
                for arm in r['arms']:
                    w=arm['weights'];f=pilot.features(b,arm['mode']=='conditional')
                    ca=.125*sum(x*y for x,y in zip(w[:4],affine));cp=.125*sum(x*y for x,y in zip(w[4:],f))
                    for mask,change in [('all',ca+cp),('affine-only',ca),('pawn-only',cp)]:
                        accum[arm['mode'],mask].append((pilot.fitlib.objective(baseline+change,phase,row['target'])[0]-base_loss,change))
            arms=[]
            for arm in r['arms']:
                metrics={}
                for mask in ['all','affine-only','pawn-only']:
                    values=accum[arm['mode'],mask]
                    metrics[mask]=dict(count=len(values),crossEntropyChange=st.mean(v[0] for v in values),
                        changeRmsPawns=math.sqrt(st.mean(v[1]*v[1] for v in values)))
                expected=next(m for m in arm['metrics'] if m['panel']==panel and m['source']==source)
                assert abs(metrics['all']['crossEntropyChange']-expected['meanCrossEntropyChange'])<1e-12
                metrics['pawnBeyondFittedAffine']=metrics['all']['crossEntropyChange']-metrics['affine-only']['crossEntropyChange']
                arms.append(dict(mode=arm['mode'],metrics=metrics))
            findings.append(dict(panel=panel,source=source,arms=arms,pawnOccurrences=pawn_total,multipleOpposingBlockers=multi_blockers))
    report=dict(id='wave9-pawn-condition-impact-v1',admission=str(admission_path),admissionSha256=sha(admission_path),
        pilot=str(result_path),pilotSha256=sha(result_path),findings=findings,
        limitations=['No refitting. Affine/pawn masks are unadapted; joint optimization couples coefficients.',
        'Consumed panels and model selection, not independent strength/generalization.',
        'Gen32 predictions use the preserved trainer representation; tiny compiled/trainer roundoff remains as in earlier diagnostics.'])
    with output.open('x',encoding='utf-8',newline='\n') as f:json.dump(report,f,indent=2);f.write('\n')
    print('Verified prior pilot metrics and separated calibration/pawn masks; no fitting')
if __name__=='__main__':main(*sys.argv[1:])
