"""Read-only fixed-head score impact and fixed-window smoothing diagnostics."""
import hashlib
import importlib.util
import json
import math
from pathlib import Path
import statistics
import sys
import chess

spec = importlib.util.spec_from_file_location('metric', Path(__file__).with_name('analyze-brn-residual-data.py'))
metric = importlib.util.module_from_spec(spec); spec.loader.exec_module(metric)

def load(p): return json.loads(p.read_text(encoding='utf-8-sig'))
def score(v): return math.copysign(min(32511, math.floor(abs(v)*100+.5)), v)

def main(base, output):
    base, output = Path(base), Path(output)
    assert not output.exists()
    reports = []; errors = []; bindings = {}; bre_representation_errors = []
    names = ['W8-FIXED-CUBIC-R4-LR001', 'W8-FIXED-QUAD-R4-LR001', 'W8-BRE-CONTROL-LR001']
    for panel in ['old', 'fresh5']:
        average = base/f'raw/wave8/average-{panel}-01'
        avg_summary = load(average/'summary.json')
        for source, java in zip(['bt4', 'cp'], avg_summary['sources']):
            path = base/f'raw/wave8/trained-predictions-{panel}-01/{source}-predictions.jsonl'
            avg_path = average/f'{source}-predictions.jsonl'
            for p in [path, avg_path]: bindings[str(p)] = hashlib.sha256(p.read_bytes()).hexdigest()
            original = [json.loads(s) for s in path.read_text().splitlines()]
            averaged = [json.loads(s) for s in avg_path.read_text().splitlines()]
            assert len(original) == len(averaged) == 2048
            groups = {}
            for index, (row, avg) in enumerate(zip(original, averaged)):
                assert (row['fen'], row['target']) == (avg['fen'], avg['target'])
                board = chess.Board(row['fen']); pieces = board.piece_map()
                phase = sum({1:1,2:3,3:3,4:5,5:9}.get(p.piece_type,0) for p in pieces.values())
                material = sum((1 if p.color == board.turn else -1)*{1:1,2:3.2,3:3.3,4:5,5:9}.get(p.piece_type,0) for p in pieces.values())
                values = row['predictions']; reference = values['Gen32']
                partition = 'fit' if index < 1024 else 'evaluation'
                for name in names:
                    baseline = values[name+'-g160']
                    for variant, raw in [('initial', baseline), ('latest',values[name+'-g168']),
                                         ('best',values[name+'-best']), ('average8',avg['predictions'][name+'-average8'])]:
                        for mapping in ['raw','half-quarter']:
                            mapped = raw if mapping == 'raw' else material+.25*(reference+.5*(raw-reference)-material)
                            anchor = baseline if mapping == 'raw' else material+.25*(reference+.5*(baseline-reference)-material)
                            loss = metric.cross_entropy(mapped,phase,row['target'])
                            for validity in ['all'] + (['valid'] if board.is_valid() else []):
                                key = (name,variant,mapping,partition,validity)
                                groups.setdefault(key,[]).append((loss,mapped-anchor,score(mapped)-score(anchor)))
            cells = []
            for (name,variant,mapping,partition,validity), rows in groups.items():
                mean = statistics.mean(x[0] for x in rows)
                if variant in ('latest','average8') and mapping=='raw' and validity=='all':
                    error = abs(mean-java['metrics'][name+'-'+variant+':'+partition]['crossEntropy'])
                    # The maturity probe uses BRE trainer binary64 predictions;
                    # the averaging probe uses its compiled runtime representation.
                    (bre_representation_errors if variant=='latest' and 'BRE-CONTROL' in name else errors).append(error)
                cells.append(dict(name=name,variant=variant,mapping=mapping,partition=partition,validity=validity,
                    count=len(rows),crossEntropy=mean,changeRmsPawns=math.sqrt(statistics.mean(x[1]**2 for x in rows)),
                    changedIntegerScores=sum(x[2]!=0 for x in rows),meanAbsoluteIntegerChange=statistics.mean(abs(x[2]) for x in rows),
                    maximumAbsoluteIntegerChange=max(abs(x[2]) for x in rows)))
            reports.append(dict(panel=panel,source=source,cells=cells,
                parameterVsFunctionMeanError=java['maximumParameterVsFunctionMeanError']))
    assert max(errors)<1e-12
    with output.open('x',encoding='utf-8',newline='\n') as f:
        json.dump(dict(id='wave8-fixed-head-impact-v1',inputs=bindings,maximumJavaMetricError=max(errors),
            maximumBreTrainerVsRuntimeMetricDifference=max(bre_representation_errors),sources=reports,
            limitations=['Consumed data, no independent strength or generalization claim.',
             'Fixed eight-checkpoint window; no optimizer average, owner write or selected window.',
             'Integer impact uses independently reproduced research half-change/quarter mapping; raw units separate.',
             'Source/panel and valid/all strata all retained; no favorable-cell selection.']),f,indent=2);f.write('\n')
    print('Head impact and average diagnostics passed; maximum metric error',max(errors))

if __name__=='__main__': main(*sys.argv[1:])
