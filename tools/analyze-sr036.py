"""Summarize closed SR-036 diagnostic and development evidence with explicit limits."""
import argparse
import importlib.util
import json
import math
from pathlib import Path
import statistics

spec=importlib.util.spec_from_file_location('baseline',Path(__file__).with_name('search-baseline.py'))
b=importlib.util.module_from_spec(spec);spec.loader.exec_module(b)


def costs(path):
    rows=[json.loads(line) for line in Path(path).read_text().splitlines()]
    assert rows[-1]['status']=='passed' and rows[-1]['observations']==len(rows)-1
    rows=rows[:-1];result={}
    for subset in ('all','opening'):
        result[subset]={}
        for budget in sorted({r['budget'] for r in rows}):
            groups={}
            for config in sorted({r['config'] for r in rows}):
                data=[r for r in rows if r['budget']==budget and r['config']==config
                      and (subset=='all' or r['id'].startswith('opening-'))]
                r={key:sum(x[key] for x in data) for key in
                   ('elapsedNanos','cpuNanos','allocatedBytes','gcCollections','compilationMillis')}
                r.update(requests=len(data),nodes=sum(x['outcome']['nodes'] for x in data),
                         qnodes=sum(sum(x['workerQnodes']) for x in data),
                         incompleteWorkerAdmissions=sum(sum(x['abortedWorkerNodes']) for x in data),
                         meanCompletedDepth=statistics.mean(x['outcome']['lastCompletedResult']['depth'] for x in data),
                         maximumElapsedMillis=max(x['elapsedNanos']/1e6 for x in data))
                groups[config]=r
            baseline=groups['production:1']
            for r in groups.values():
                r.update({key+'Ratio':r[key]/baseline[key] for key in ('elapsedNanos','cpuNanos','nodes')})
                r['incompleteAdmissionFraction']=r['incompleteWorkerAdmissions']/max(1,r['nodes'])
            result[subset][budget]=groups
    return {'source':str(path),'sha256':b.digest(path),'costs':result,
            'limits':'Fixed-depth/node/timed diagnostics are not strength. Incomplete admissions are not a unique-position duplication rate; CPU/JIT/GC are process-wide. Terminal/ceiling roots distort aggregate nominal depth.'}


def match(path):
    path=Path(path);run=json.loads((path/'run.json').read_text())
    assert run['status']=='passed' and run['inputsStable'], 'Only valid closed runs'
    a=run['arguments'];audit=b.analyze(path/'games.pgn',a['openings'],a['pairs'],True,a['concurrency']>1)
    for key in ('wdl','statistics','terminations','wholeRequestResources'):assert audit[key]==run['results'][key]
    scores=[g['candidateScore'] for g in audit['games']]
    pairs=[(scores[i]+scores[i+1])/2 for i in range(0,len(scores),2)]
    variance=statistics.variance(pairs) if len(pairs)>1 else None
    forecasts={}
    if variance:
        # Planning only: 80% normal power, conservative staged critical value.
        z=statistics.NormalDist().inv_cdf(1-(.025/3)/2)+statistics.NormalDist().inv_cdf(.8)
        for elo in (10,20,30,50):
            gain=1/(1+10**(-elo/400))-.5
            n=math.ceil(variance*z*z/(gain*gain))
            forecasts[str(elo)]={'pairs':n,'hoursAtObservedThroughput':n*run['matchWallSeconds']/a['pairs']/3600}
    return {'run':str(path),'runSha256':b.digest(path/'run.json'),'status':'audited',
            'wdl':audit['wdl'],'statistics':audit['statistics'],'pairVariance':variance,
            'resourceArguments':{k:a[k] for k in ('baseline_policy','candidate_policy','baseline_workers','candidate_workers','concurrency','tc')},
            'matchWallSeconds':run['matchWallSeconds'],'wholeRequestResources':audit['wholeRequestResources'],
            'illustrative80PercentPower':forecasts,
            'limits':'Exploratory only unless independently bound to a prospective confirmation. Plug-in variance/normal-power forecasts assume this population and throughput; they are not required game quotas, acceptance thresholds or predictions of the true gain.'}


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--cost',action='append',default=[])
    p.add_argument('--run',action='append',default=[]);p.add_argument('--out',required=True);a=p.parse_args()
    out=Path(a.out)
    if out.exists():raise ValueError('Use a new output for each closed analysis')
    value={'costs':[costs(x) for x in a.cost],'matches':[match(x) for x in a.run]}
    out.write_text(json.dumps(value,indent=2)+'\n',encoding='utf-8',newline='\n')
