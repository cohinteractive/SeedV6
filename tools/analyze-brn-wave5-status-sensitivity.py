"""Posthoc standard-board-status sensitivity; never filters source training data.

Uses the already consumed evaluation halves of saved CP predictions. The paired
all/valid results diagnose whether flagged rows explain the fitting conclusions.
"""
import collections
import hashlib
import importlib.util
import json
import statistics
import sys
from pathlib import Path
import chess

spec=importlib.util.spec_from_file_location('metrics',Path(__file__).with_name('analyze-brn-residual-data.py'))
metric=importlib.util.module_from_spec(spec);spec.loader.exec_module(metric)
directory=Path('docs/research/brn-successor-cglhw');results=[]
for panel in ['trained-predictions-02','fresh-predictions-01']:
    path=directory/f'raw/wave5/{panel}/cp-predictions.jsonl'
    groups=collections.defaultdict(list);statuses=collections.Counter()
    for index,line in enumerate(path.read_text(encoding='utf8').splitlines()):
        if index<1024:continue
        row=json.loads(line);board=chess.Board(row['fen']);status=board.status();statuses[int(status)]+=1
        phase=sum({1:1,2:3,3:3,4:5,5:9}.get(p.piece_type,0) for p in board.piece_map().values())
        material=sum((1 if p.color==board.turn else -1)*{1:1,2:3.2,3:3.3,4:5,5:9}.get(p.piece_type,0) for p in board.piece_map().values())
        values=row['predictions']
        for family in ['R4','R16']:
            for generation in [64,96]:
                name=f'W5-RESCUBIC-{family}-LR001-g{generation}'
                raw=values[name+'-correction0.5']
                loss=metric.cross_entropy(material+.25*(raw-material),phase,row['target'])
                groups[(family,generation,'all')].append(loss)
                if status==chess.STATUS_VALID:groups[(family,generation,'standard-valid')].append(loss)
    rows=[dict(family=f,generation=g,subset=s,count=len(v),crossEntropy=statistics.mean(v)) for (f,g,s),v in groups.items()]
    results.append(dict(panel=panel,input=str(path),sha256=hashlib.sha256(path.read_bytes()).hexdigest(),statusCounts=statuses,rows=rows))
report=dict(id='wave5-cp-status-sensitivity-v1',sources=results,
    statusFlags={str(int(s)):s.name for s in chess.Status if int(s) in [8,64,128,256]},
    mapping='material + .25 residual; half correction; saved evaluation half only',
    limitations=['Posthoc sensitivity, not a replacement protocol or independent validation.',
      'No filtering or modification of owner datasets, histories or previous metrics.',
      'Standard-board validity does not establish label correctness, uniqueness or game independence.'])
with Path(sys.argv[1]).open('x',encoding='utf8',newline='\n') as stream:
    json.dump(report,stream,indent=2);stream.write('\n')
for source in results:
    print(source['panel'],source['statusCounts'])
    for row in source['rows']:print(row)
