"""Compare degree interventions under training versus search score mappings; saved predictions only."""
import json,math,statistics,sys,importlib.util,hashlib,collections
from pathlib import Path
import chess
spec=importlib.util.spec_from_file_location('metrics',Path(__file__).with_name('analyze-brn-residual-data.py'));metric=importlib.util.module_from_spec(spec);spec.loader.exec_module(metric)
d=Path('docs/research/brn-successor-cglhw');results=[];maximum=0;comparisons=0
for panel in ['trained-predictions-02','fresh-predictions-01']:
 for source in ['bt4','cp']:
  path=d/f'raw/wave5/{panel}/{source}-predictions.jsonl';groups=collections.defaultdict(list)
  for ix,line in enumerate(path.read_text().splitlines()):
   if ix<1024:continue
   row=json.loads(line);board=chess.Board(row['fen']);pieces=board.piece_map()
   phase=sum({1:1,2:3,3:3,4:5,5:9}.get(p.piece_type,0) for p in pieces.values())
   material=sum((1 if p.color==board.turn else -1)*{1:1,2:3.2,3:3.3,4:5,5:9}.get(p.piece_type,0) for p in pieces.values())
   v=row['predictions']
   for name in ['W5-RESCUBIC-R4-LR001-g96','W5-RESCUBIC-R16-LR001-g96']:
    base=v[name+'-correction0.0'];unary=v[name+'-unary'];q=v[name+'-quadratic']-unary;c=v[name]-v[name+'-quadratic']
    for label,raw in {'full':v[name],'unary':unary,'quadratic':unary+q,'cubic-only':unary+c}.items():
     for correction in [.5,1]:
      for mapping in [.25,1]:
       value=material+mapping*(base+correction*(raw-base)-material)
       groups[(name,label,correction,mapping)].append(metric.cross_entropy(value,phase,row['target']))
  rows=[dict(model=n,variant=v,correctionScale=c,materialResidualGain=m,count=len(x),crossEntropy=statistics.mean(x)) for (n,v,c,m),x in groups.items()]
  reference=json.loads((d/f'raw/wave5/{panel}/summary.json').read_text())['sources'][0 if source=='bt4' else 1]['metrics']
  for row in rows:
   if row['variant']=='cubic-only':continue
   if row['correctionScale']==.5:
    if row['variant']!='full':continue
    name=row['model']+'-correction0.5'
   else:name=row['model']+('' if row['variant']=='full' else '-'+row['variant'])
   key=name+'@'+str(float(row['materialResidualGain']))+':evaluation'
   maximum=max(maximum,abs(row['crossEntropy']-reference[key]['crossEntropy']));comparisons+=1
  results.append(dict(panel=panel,source=source,input=str(path),sha256=hashlib.sha256(path.read_bytes()).hexdigest(),rows=rows))
target=Path(sys.argv[1])
assert comparisons==64 and maximum<1e-12,(comparisons,maximum)
print('Independent Java metric comparisons',comparisons,'maximum error',maximum)
with target.open('x',encoding='utf8',newline='\n') as f:json.dump(dict(id='wave5-degree-calibration-diagnostic-v1',sources=results,limitations=['Posthoc reused prediction rows; no chess-strength conclusion or calibrated model selection.','Raw gain1/full correction is the trained objective function; gain.25/half correction is the fixed strength-screen mapping.','Source partition/base overlap and correlated observations remain; no significance inference.']),f,indent=2);f.write('\n')
for src in results:
 print(src['panel'],src['source'])
 for family in ['R4','R16']:
  rows=[r for r in src['rows'] if '-'+family+'-' in r['model'] and r['materialResidualGain']==1 and r['correctionScale']==1]
  print(family,'raw objective:',{r['variant']:round(r['crossEntropy'],7) for r in rows})

