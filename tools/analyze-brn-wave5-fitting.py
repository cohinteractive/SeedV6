"""Fixed W5 maturity/calibration/interaction diagnostics on consumed prediction panels."""
import importlib.util,json,hashlib,math,statistics,sys,collections
from pathlib import Path
import chess
spec=importlib.util.spec_from_file_location('metrics',Path(__file__).with_name('analyze-brn-residual-data.py'))
metric=importlib.util.module_from_spec(spec);spec.loader.exec_module(metric)
def main(directory,output,panel_status='consumed'):
    assert panel_status in ('consumed','fresh-ordinal-reserved')
    directory,output=Path(directory),Path(output)
    assert not output.exists()
    java=json.loads((directory/'summary.json').read_text(encoding='utf-8'))
    results=[];maximum=0
    for source,reference in zip(('bt4','cp'),java['sources']):
        path=directory/(source+'-predictions.jsonl')
        rows=[json.loads(s) for s in path.read_text(encoding='utf-8').splitlines()]
        grouped=collections.defaultdict(list);base_difference=0
        for index,row in enumerate(rows):
            board=chess.Board(row['fen']);pieces=board.piece_map()
            phase=sum({1:1,2:3,3:3,4:5,5:9}.get(p.piece_type,0) for p in pieces.values())
            white=sum((1 if p.color else -1)*{1:1,2:3.2,3:3.3,4:5,5:9}.get(p.piece_type,0) for p in pieces.values())
            material=white*(1 if board.turn else -1);values=row['predictions']
            # Pair2 trainer sums binary64 absolute/shared parameters; its runtime
            # snapshot folds to float before compiling tables. Frozen residuals
            # use that compiled Gen32, exposed independently by zero correction.
            zeros=[v for n,v in values.items() if n.endswith('-correction0.0')]
            assert zeros and all(v==zeros[0] for v in zeros)
            base=zeros[0];base_difference=max(base_difference,abs(base-values['Gen32']))
            variants={'Gen32':values['Gen32']}
            for name,raw in values.items():
                if not name.startswith('W5-') or '-correction' in name:continue
                for alpha in (.5,1):
                    variants[name+':'+str(alpha)]=base+alpha*(raw-base)
            partition='fit' if index<1024 else 'evaluation'
            for name,raw in variants.items():
                mapped=material+.25*(raw-material)
                ce=metric.cross_entropy(mapped,phase,row['target'])
                grouped[(name,partition)].append((ce,(raw-base)**2,mapped))
        measured={}
        for (name,partition),values in grouped.items():
            measured[name+':'+partition]=dict(count=len(values),crossEntropy=statistics.mean(v[0] for v in values),
                changeRmsPawns=math.sqrt(statistics.mean(v[1] for v in values)),
                mappedMin=min(v[2] for v in values),mappedMax=max(v[2] for v in values))
            if name=='Gen32':key='Gen32@0.25:'+partition
            elif name.endswith(':1'):key=name[:-2]+'@0.25:'+partition
            elif ':0.5' in name and 'RESCUBIC' in name and '-unary' not in name and '-quadratic' not in name:key=name[:-4]+'-correction0.5@0.25:'+partition
            else:continue
            difference=abs(measured[name+':'+partition]['crossEntropy']-reference['metrics'][key]['crossEntropy'])
            if difference>maximum:
                maximum=difference
                worst=(source,key,measured[name+':'+partition]['crossEntropy'],reference['metrics'][key]['crossEntropy'])
        results.append(dict(source=source,sha256=hashlib.sha256(path.read_bytes()).hexdigest(),
            maximumGen32TrainerVersusFrozenRuntimeRawDifference=base_difference,metrics=measured))
    assert maximum<1e-12,(maximum,worst)
    with output.open('x',encoding='utf-8') as f:json.dump(dict(id='wave5-fixed-panel-maturity-calibration-v1',panelStatus=panel_status,maximumJavaReproductionError=maximum,sources=results,limitations=[('Already consumed panels; no independent strength or generalization claim.' if panel_status=='consumed' else 'New source ordinals beyond G5 cursors, reserved geometry partition; game/FEN overlap and historical base exposure not excluded. This probe consumes the panel for development, not final confirmation.'),'Fixed .5/1 change and quarter material residual; no per-position selection or filtering.','Unary/quadratic ablations are post-training interventions, not separately optimized architectures.','Cubic predictors use compiled frozen base. Gen32/BRE predictors from the inherited assessment tool use binary64 trainer functions before float folding; match runtimes use verified compiled snapshots. Recorded tiny baseline raw differences must not be called exact function equality.']),f,indent=2);f.write('\n')
if __name__=='__main__':main(*sys.argv[1:])
