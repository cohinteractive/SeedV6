"""Apply the prospectively recorded fixed-window diagnostic rule; no match selection by wins."""
import collections,hashlib,importlib.util,json,statistics
from pathlib import Path
import chess
spec=importlib.util.spec_from_file_location('metrics',Path(__file__).with_name('analyze-brn-residual-data.py'))
metric=importlib.util.module_from_spec(spec);spec.loader.exec_module(metric)
b=Path('docs/research/brn-successor-cglhw');out=[];errors=[];eligibility=collections.defaultdict(list)
for panel in ['old','fresh5']:
    directory=b/'raw/wave6/average-diagnostics-01'/panel
    java=json.loads((directory/'summary.json').read_text())
    for source,reference in zip(['bt4','cp'],java['sources']):
        path=directory/(source+'-predictions.jsonl');groups=collections.defaultdict(list)
        for ix,line in enumerate(path.read_text().splitlines()):
            row=json.loads(line);board=chess.Board(row['fen']);pieces=board.piece_map();is_valid=board.status()==chess.STATUS_VALID
            phase=sum({1:1,2:3,3:3,4:5,5:9}.get(p.piece_type,0) for p in pieces.values())
            white=sum((1 if p.color else -1)*{1:1,2:3.2,3:3.3,4:5,5:9}.get(p.piece_type,0) for p in pieces.values())
            material=white*(1 if board.turn else -1);part='fit' if ix<1024 else 'evaluation'
            for name,raw in row['predictions'].items():
                for gain in [.25,1.0]:
                    ce=metric.cross_entropy(material+gain*(raw-material),phase,row['target'])
                    groups[(name,part,gain,'all')].append(ce)
                    if is_valid:groups[(name,part,gain,'valid')].append(ce)
        means={key:statistics.mean(v) for key,v in groups.items()}
        for (name,part,gain,validity),value in means.items():
            if gain==1.0 and validity=='all':errors.append(abs(value-reference['metrics'][name+':'+part]['crossEntropy']))
        comparisons=[]
        for binding in java['bindings']:
            name=binding['name']
            for part in ['fit','evaluation']:
                for validity in ['all','valid']:
                    values={variant:means[(name+'-'+variant+'-half',part,.25,validity)] for variant in ['latest','average8','ensemble8']}
                    eligibility[name].append(values['average8']<values['latest'])
                    comparisons.append(dict(name=name,partition=part,validity=validity,**values,averageMinusLatest=values['average8']-values['latest']))
        out.append(dict(panel=panel,source=source,predictionsSha256=hashlib.sha256(path.read_bytes()).hexdigest(),
                        parameterVsFunctionMeanRawMaximum=reference['maximumParameterVsFunctionMeanError'],comparisons=comparisons))
maximum=max(errors);assert maximum<1e-12,maximum
report=dict(id='wave6-last-eight-diagnostic-result-v1',maximumIndependentJavaError=maximum,sources=out,
            eligibility={k:dict(passedCells=sum(v),requiredCells=len(v),eligible=all(v)) for k,v in eligibility.items()},
            criterion='Lower fixed half-change/quarter-mapped CE than Latest128 in all four fit and all four evaluation source/panel cells, also in standard-valid sensitivity; fixed121..128 window only.',
            limitations=['Consumed panels, selection diagnostic only; not independent validation or chess strength.',
                         'Parameter mean differs from function ensemble in nonlinear models; ensemble is not a search candidate.',
                         'No optimizer average, trainable checkpoint export or owner-store write.'])
with (b/'wave6-average-diagnostic-results.json').open('x',encoding='utf-8',newline='\n') as f:json.dump(report,f,indent=2);f.write('\n')
print(json.dumps(dict(maximumIndependentJavaError=maximum,eligibility=report['eligibility']),indent=2))
