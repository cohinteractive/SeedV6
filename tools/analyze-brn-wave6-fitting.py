"""Independent fixed-panel G6 maturity/degree/calibration diagnostics, no fitting."""
import collections,hashlib,importlib.util,json,math,statistics,sys
from pathlib import Path
import chess
spec=importlib.util.spec_from_file_location('metrics',Path(__file__).with_name('analyze-brn-residual-data.py'))
metric=importlib.util.module_from_spec(spec);spec.loader.exec_module(metric)

def main(directory,output):
    directory,output=Path(directory),Path(output);assert not output.exists()
    java=json.loads((directory/'summary.json').read_text());results=[];max_error=0
    for source,j in zip(['bt4','cp'],java['sources']):
        path=directory/(source+'-predictions.jsonl')
        rows=[json.loads(s) for s in path.read_text().splitlines()]
        grouped=collections.defaultdict(list);status=collections.Counter();base_error=0
        for ix,row in enumerate(rows):
            board=chess.Board(row['fen']);valid=int(board.status());status[valid]+=1
            pieces=board.piece_map();phase=sum({1:1,2:3,3:3,4:5,5:9}.get(p.piece_type,0) for p in pieces.values())
            white=sum((1 if p.color else -1)*{1:1,2:3.2,3:3.3,4:5,5:9}.get(p.piece_type,0) for p in pieces.values())
            material=white*(1 if board.turn else -1);v=row['predictions']
            zeros=[p for n,p in v.items() if n.endswith('-correction0.0')]
            assert zeros and all(p==zeros[0] for p in zeros)
            base=zeros[0];base_error=max(base_error,abs(base-v['Gen32']))
            split='fit' if ix<1024 else 'evaluation'
            for name,raw in v.items():
                for gain in [.25,1.0]:
                    mapped=material+gain*(raw-material)
                    ce=metric.cross_entropy(mapped,phase,row['target'])
                    grouped[(name,gain,split,'all')].append((ce,mapped,raw-base))
                    if valid==0:grouped[(name,gain,split,'valid')].append((ce,mapped,raw-base))
                if name.startswith(('W6-', 'W7-')) and '-correction' not in name:
                    half=base+.5*(raw-base);mapped=material+.25*(half-material)
                    ce=metric.cross_entropy(mapped,phase,row['target'])
                    grouped[(name+'-half',.25,split,'all')].append((ce,mapped,half-base))
                    if valid==0:grouped[(name+'-half',.25,split,'valid')].append((ce,mapped,half-base))
        measured=[]
        for (name,gain,split,validity),values in grouped.items():
            ce=statistics.mean(x[0] for x in values)
            if validity=='all' and not name.endswith('-half'):
                expected=j['metrics'][name+'@'+str(gain)+':'+split]['crossEntropy']
                max_error=max(max_error,abs(ce-expected))
            measured.append(dict(name=name,gain=gain,partition=split,validity=validity,count=len(values),
                crossEntropy=ce,changeRmsPawns=math.sqrt(statistics.mean(x[2]**2 for x in values)),
                mappedMin=min(x[1] for x in values),mappedMax=max(x[1] for x in values)))
        results.append(dict(source=source,predictionsSha256=hashlib.sha256(path.read_bytes()).hexdigest(),
            statuses=dict(status),maximumBaseTrainerVsCompiledError=base_error,metrics=measured))
    assert max_error<1e-12,max_error
    with output.open('x',encoding='utf-8',newline='\n') as f:
        json.dump(dict(id=java['plan']+'-diagnostics-v1',directory=str(directory),maximumJavaMeanError=max_error,
            sources=results,limitations=['Both panels were consumed previously; no independent generalization/strength claim.',
             'Second half is evaluation partition of these diagnostic panels; status-valid sensitivity does not prove reachability.',
             'Half-change quarter mapping fixed from earlier waves; full/raw metrics separate objective effects.',
             'Quadratic/unary/cubic-only inference ablations have no adaptation; separately trained quadratic lineages are the adapted degree controls.',
             'BRE raw trainer values differ slightly from float-folded runtime; actual match hashes/caches separately verified.']),f,indent=2);f.write('\n')
    print('Independent Java metric reproduction maximum error',max_error)

if __name__=='__main__':main(*sys.argv[1:])
