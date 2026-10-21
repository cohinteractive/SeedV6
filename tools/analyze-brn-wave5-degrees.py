"""Reproduce fixed posthoc W5 degree diagnostics; optional NEW_OUTPUT preserves prior results."""
import json,collections,statistics,math,sys,importlib.util,hashlib
from pathlib import Path
import chess
spec=importlib.util.spec_from_file_location('metrics',Path('tools/analyze-brn-residual-data.py'));metric=importlib.util.module_from_spec(spec);spec.loader.exec_module(metric)
d=Path('docs/research/brn-successor-cglhw');out=[]
for panel in ['trained-predictions-02','fresh-predictions-01']:
 for src in ['bt4','cp']:
  path=d/f'raw/wave5/{panel}/{src}-predictions.jsonl';rows=[json.loads(s) for s in path.read_text().splitlines()]
  groups=collections.defaultdict(list);statuses=collections.Counter();fens=set()
  for ix,row in enumerate(rows):
   board=chess.Board(row['fen']);statuses[int(board.status())]+=1;fens.add(board.fen().split(' ')[0])
   pieces=board.piece_map();phase=sum({1:1,2:3,3:3,4:5,5:9}.get(p.piece_type,0) for p in pieces.values())
   white=sum((1 if p.color else -1)*{1:1,2:3.2,3:3.3,4:5,5:9}.get(p.piece_type,0) for p in pieces.values());material=white*(1 if board.turn else -1)
   v=row['predictions'];family='fit' if ix<1024 else 'evaluation'
   for name in ['W5-RESCUBIC-R4-LR001-g96','W5-RESCUBIC-R16-LR001-g96']:
    base=v[name+'-correction0.0'];unary=v[name+'-unary'];q=v[name+'-quadratic']-unary;c=v[name]-v[name+'-quadratic']
    variants={'full':v[name],'unary':unary,'quadratic':unary+q,'cubic-only':unary+c}
    for label,raw in variants.items():
     mapped=material+.25*(base+.5*(raw-base)-material)
     ce=metric.cross_entropy(mapped,phase,row['target'])
     for bucket in ['all', 'pieces-'+('2-12' if len(pieces)<=12 else '13-24' if len(pieces)<=24 else '25+')]:
      groups[(family,name,bucket,label)].append((ce,q,c))
  results={':'.join(key):dict(n=len(x),ce=statistics.mean(i[0] for i in x),quadraticRms=math.sqrt(statistics.mean(i[1]**2 for i in x)),cubicRms=math.sqrt(statistics.mean(i[2]**2 for i in x))) for key,x in groups.items()}
  out.append(dict(panel=panel,source=src,sha256=hashlib.sha256(path.read_bytes()).hexdigest(),statusCounts=dict(statuses),uniquePlacements=len(fens),metrics=results))
target=Path(sys.argv[1]) if len(sys.argv)>1 else d/'wave5-degree-diagnostics.json'
with target.open('x',encoding='utf8',newline='\n') as f:json.dump(dict(id='wave5-posthoc-degree-diagnostics-v1',question='Do degree2 and degree3 contributions cancel, and is their value uniform across piece counts?',sources=out,limitations=['Posthoc exploratory algebra from already acquired predictions, including fresh panel now consumed.','No model training, no search measurement, no optimal scale/phase claim. Cubic-only means full minus quadratic plus unary; finite reassociation is diagnostic.','Piece-count bins fixed here for mechanism exploration; correlated rows cannot be treated as independent games.']),f,indent=2);f.write('\n')
for x in out:
 print(x['panel'],x['source'],x['statusCounts'])
 for name in ['W5-RESCUBIC-R4-LR001-g96','W5-RESCUBIC-R16-LR001-g96']:
  print(name,{label:round(x['metrics']['evaluation:'+name+':all:'+label]['ce'],9) for label in ['full','unary','quadratic','cubic-only']})

