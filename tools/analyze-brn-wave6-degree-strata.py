"""Separate trained-degree and unadapted ablation evidence in fixed piece-count bins."""
import collections,hashlib,importlib.util,json,math,statistics
from pathlib import Path
import chess
spec=importlib.util.spec_from_file_location('metrics',Path(__file__).with_name('analyze-brn-residual-data.py'))
metric=importlib.util.module_from_spec(spec);spec.loader.exec_module(metric)
b=Path('docs/research/brn-successor-cglhw');out=[]
for panel in ['old','fresh5']:
    for source in ['bt4','cp']:
        p=b/f'raw/wave6/trained-predictions-{panel}-01/{source}-predictions.jsonl'
        groups=collections.defaultdict(list)
        for ix,line in enumerate(p.read_text().splitlines()):
            row=json.loads(line);board=chess.Board(row['fen']);pieces=board.piece_map()
            phase=sum({1:1,2:3,3:3,4:5,5:9}.get(x.piece_type,0) for x in pieces.values())
            white=sum((1 if x.color else -1)*{1:1,2:3.2,3:3.3,4:5,5:9}.get(x.piece_type,0) for x in pieces.values())
            material=white*(1 if board.turn else -1);v=row['predictions']
            bin_name='2-12' if len(pieces)<=12 else '13-24' if len(pieces)<=24 else '25+'
            for rank in [4,16]:
                name=f'W6-RESCUBIC-R{rank}-LR001-g128';base=v[name+'-correction0.0']
                unary=v[name+'-unary'];quad=v[name+'-quadratic'];full=v[name]
                values={'cubic':full,'unadaptedQuadratic':quad,'unary':unary,'cubicOnly':v[name+'-cubic-only']}
                if rank==16:values['trainedQuadratic']=v['W6-RESQUAD-R16-LR001-g128']
                for variant,raw in values.items():
                    mapped=material+.25*(base+.5*(raw-base)-material)
                    losses=(metric.cross_entropy(raw,phase,row['target']),metric.cross_entropy(mapped,phase,row['target']),quad-unary,full-quad)
                    for bucket in ['all',bin_name]:
                        groups[(rank,variant,'fit' if ix<1024 else 'evaluation',bucket)].append(losses)
        rows=[]
        for key,values in groups.items():
            rows.append(dict(rank=key[0],variant=key[1],partition=key[2],pieceBin=key[3],count=len(values),
                rawCE=statistics.mean(v[0] for v in values),halfMappedCE=statistics.mean(v[1] for v in values),
                sourceQuadraticRms=math.sqrt(statistics.mean(v[2]**2 for v in values)),
                sourceCubicRms=math.sqrt(statistics.mean(v[3]**2 for v in values))))
        out.append(dict(panel=panel,source=source,path=str(p),sha256=hashlib.sha256(p.read_bytes()).hexdigest(),metrics=rows))
with (b/'wave6-degree-strata.json').open('x',encoding='utf-8',newline='\n') as f:
    json.dump(dict(id='wave6-degree-strata-v1',sources=out,question='Does degree3 signal concentrate in the previously used fixed piece-count strata?',
        limitations=['Posthoc consumed-panel diagnostic, not a selected routing rule or strength claim.',
          'Bins reuse prior W5 definitions; fit/evaluation halves kept separate. No per-position selection or reweighting.',
          'Trained Q16 has adapted retained weights; unadapted quadratic removes only cubic heads from C16.',
          'RMS columns always refer to source cubic decomposition, including rows labelled trainedQuadratic.']),f,indent=2);f.write('\n')
for source in out:
    print(source['panel'],source['source'])
    for bin_name in ['all','2-12','13-24','25+']:
        values={r['variant']:r for r in source['metrics'] if r['rank']==16 and r['partition']=='evaluation' and r['pieceBin']==bin_name}
        print(bin_name,'C16-minus-trainedQ mapped',round(values['cubic']['halfMappedCE']-values['trainedQuadratic']['halfMappedCE'],9),
              'C16-minus-unadaptedQ mapped',round(values['cubic']['halfMappedCE']-values['unadaptedQuadratic']['halfMappedCE'],9))
