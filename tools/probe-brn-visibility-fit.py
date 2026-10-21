"""Bounded feasibility fit on consumed panels, never an owner model or strength test.

Two sources x first1024 rows fit; second1024 diagnose. Equal-size visibility and
geometry bases, fixed eight epochs,128-example batches, Adam. Four fixed arms
cross feature basis with raw/search-mapped objective. No selected hyperparameters.
"""
import collections
import hashlib
import importlib.util
import json
import math
from pathlib import Path
import random
import statistics
import sys
import chess

spec = importlib.util.spec_from_file_location('visibility', Path(__file__).with_name('verify-brn-visibility-features.py'))
feature = importlib.util.module_from_spec(spec); spec.loader.exec_module(feature)
spec = importlib.util.spec_from_file_location('metric', Path(__file__).with_name('analyze-brn-residual-data.py'))
metric = importlib.util.module_from_spec(spec); spec.loader.exec_module(metric)

def objective(pawns, phase, target):
    m = max(17, min(78, phase))/58
    a = ((-13.50030198*m+40.92780883)*m-36.82753545)*m+386.83004070
    b = ((96.53354896*m-165.79058388)*m+90.89679019)*m+49.29561889
    k = a/b
    def sigmoid(z):
        e = math.exp(-abs(z)); return 1/(1+e) if z >= 0 else e/(1+e)
    w, s, wc, sc = [sigmoid(z) for z in (k*(pawns-1), k*(pawns+1), -k*(pawns-1), -k*(pawns+1))]
    denominator = (w+s)*(wc+sc)
    ratio = k/2 if denominator < 1e-200 else k*(w*wc+s*sc)/denominator
    prediction = 1-wc-sc if pawns >= 0 else w+s-1
    return metric.cross_entropy(pawns, phase, target), (prediction-target)*ratio

def main(directory, output):
    directory, output = Path(directory), Path(output)
    assert not output.exists()
    fit, evaluation, bindings = [], [], {}
    for source in ('bt4','cp'):
        p = directory/(source+'-predictions.jsonl'); bindings[str(p)] = hashlib.sha256(p.read_bytes()).hexdigest()
        rows = [json.loads(line) for line in p.read_text().splitlines()]; assert len(rows) == 2048
        for i, row in enumerate(rows):
            board = chess.Board(row['fen']); pieces = board.piece_map()
            phase = sum({1:1,2:3,3:3,4:5,5:9}.get(p.piece_type,0) for p in pieces.values())
            material = sum((1 if p.color == board.turn else -1)*{1:1,2:3.2,3:3.3,4:5,5:9}.get(p.piece_type,0) for p in pieces.values())
            base = row['predictions']['Gen32']
            data = dict(source=source,phase=phase,material=material,base=base,target=row['target'],valid=board.is_valid(),
                visible=collections.Counter(feature.features(board,True)),geometry=collections.Counter(feature.features(board,False)))
            (fit if i<1024 else evaluation).append(data)
    max_gradient_error = 0
    for pawns in (-8,-2,-.3,0,.2,2,8):
        for phase in (17,48,78):
            for target in (-1,0,1):
                h=1e-5; expected=(objective(pawns+h,phase,target)[0]-objective(pawns-h,phase,target)[0])/(2*h)
                max_gradient_error=max(max_gradient_error,abs(expected-objective(pawns,phase,target)[1]))
    assert max_gradient_error < 1e-8, max_gradient_error
    reports=[]
    for basis in ('visible','geometry'):
        for gain in (1.0,.25):
            weights=[0.0]*2688; first=[0.0]*2688; second=[0.0]*2688; step=0; curves=[]
            rng=random.Random(2026102107)
            for epoch in range(8):
                order=list(range(len(fit)));rng.shuffle(order); losses=[]
                for start in range(0,len(order),128):
                    gradient=[0.0]*2688
                    for index in order[start:start+128]:
                        row=fit[index]; correction=sum(weights[k]*n for k,n in row[basis].items())
                        mapped=row['material']+gain*(row['base']+correction-row['material'])
                        loss,d=objective(mapped,row['phase'],row['target']);losses.append(loss)
                        for k,n in row[basis].items():gradient[k]+=gain*d*n/128
                    step+=1; c1=1-.9**step;c2=1-.999**step
                    for k,g in enumerate(gradient):
                        g+=.001*weights[k]
                        first[k]=.9*first[k]+.1*g;second[k]=.999*second[k]+.001*g*g
                        weights[k]-=.01*(first[k]/c1)/(math.sqrt(second[k]/c2)+1e-8)
                curves.append(statistics.mean(losses))
            assert step==128 and all(math.isfinite(w) for w in weights)
            metrics=[]
            for split, rows in [('fit',fit),('evaluation',evaluation)]:
                for source in ('bt4','cp'):
                    selected=[r for r in rows if r['source']==source]
                    for mapping in (1.0,.25):
                        deltas=[];squared=[]
                        for row in selected:
                            correction=sum(weights[k]*n for k,n in row[basis].items())
                            base=row['material']+mapping*(row['base']-row['material'])
                            deltas.append(objective(base+mapping*correction,row['phase'],row['target'])[0]-objective(base,row['phase'],row['target'])[0])
                            squared.append(correction*correction)
                        metrics.append(dict(split=split,source=source,mapping=mapping,count=len(selected),
                            meanCrossEntropyChange=statistics.mean(deltas),standardError=statistics.stdev(deltas)/math.sqrt(len(deltas)),
                            correctionRmsPawns=math.sqrt(statistics.mean(squared))))
            reports.append(dict(basis=basis,objectiveGain=gain,steps=step,epochLoss=curves,metrics=metrics,weights=weights))
    with output.open('x',encoding='utf-8',newline='\n') as f:
        json.dump(dict(id='brn-visibility-bounded-fit-v1',inputs=bindings,fitRows=len(fit),evaluationRows=len(evaluation),
            hyperparameters=dict(epochs=8,batch=128,rate=.01,l2=.001,seed=2026102107,parameters=2688),
            gradientMaximumError=max_gradient_error,arms=reports,
            limitations=['Consumed panels; row uncertainty ignores game/source correlation and prior research selection.',
                'Feasibility only:128 updates per arm, no checkpoint, owner write or substantive training.',
                'Equal parameter count does not imply identical optimization; fixed rate/regularization not optimized.',
                'Frozen Gen32 trainer binary64 values; match runtime folding differs slightly.',
                'Search-mapped and raw objectives tested separately; no inference/search/strength conclusion.']),f,indent=2);f.write('\n')
    print('Completed four bounded128-update feasibility arms; gradient error',max_gradient_error)

if __name__=='__main__': main(*sys.argv[1:])
