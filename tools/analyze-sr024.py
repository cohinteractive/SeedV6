"""Closed SR-024 cost and paired-game audits; exploratory unless protocol-bound."""
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
    rows=rows[:-1];groups={}
    for workers in sorted({int(r['config'].split(':')[1]) for r in rows}):
        domain={}
        for budget in sorted({r['budget'] for r in rows}):
            modes={}
            for config in sorted({r['config'] for r in rows if r['config'].endswith(':'+str(workers))}):
                data=[r for r in rows if r['config']==config and r['budget']==budget]
                result={k:sum(x[k] for x in data) for k in ('elapsedNanos','cpuNanos','allocatedBytes','gcCollections','compilationMillis')}
                result.update(requests=len(data),nodes=sum(x['outcome']['nodes'] for x in data),
                              qnodes=sum(sum(x['workerQnodes']) for x in data),
                              meanCompletedDepth=statistics.mean(x['outcome']['lastCompletedResult']['depth'] for x in data),
                              workerSeeCounters=[[sum(x['seeCounters'][w][c] for x in data) for c in range(6)] for w in range(workers)])
                modes[config]=result
            ref=modes.get('off:'+str(workers))
            if ref:
                for result in modes.values():
                    result.update({k+'Ratio':result[k]/ref[k] for k in ('elapsedNanos','cpuNanos','nodes','qnodes')})
            domain[budget]=modes
        groups[workers]=domain
    return {'path':str(path),'sha256':b.digest(path),'domains':groups,
            'limitations':'Fixed-depth/node/time diagnostics are not strength. Rotated repeated requests use one process; CPU/JIT/GC include its background activity. Compare only within worker domains. Diagnostic-sink runs are not clean cost claims.'}


def match(path):
    path=Path(path);run=json.loads((path/'run.json').read_text());a=run['arguments']
    assert run['status']=='passed' and run['inputsStable']
    assert a['baseline_workers']==a['candidate_workers']
    audit=b.analyze(path/'games.pgn',a['openings'],a['pairs'],True,a['concurrency']>1)
    for k in ('wdl','statistics','terminations','wholeRequestResources'):assert audit[k]==run['results'][k]
    scores=[g['candidateScore'] for g in audit['games']]
    pairs=[(scores[i]+scores[i+1])/2 for i in range(0,len(scores),2)]
    mean=statistics.mean(pairs);variance=statistics.variance(pairs)
    half=1.96*math.sqrt(variance/len(pairs))
    def elo(p):return None if p<=0 or p>=1 else 400*math.log10(p/(1-p))
    cpu=json.loads((path/'process-cost.json').read_text());assert cpu['status']=='passed'
    z=statistics.NormalDist().inv_cdf(1-.025/6)+statistics.NormalDist().inv_cdf(.8)
    forecasts={}
    for target in (10,20,40):
        gain=1/(1+10**(-target/400))-.5;n=math.ceil(variance*z*z/(gain*gain))
        forecasts[target]={'pairs':n,'hours':n*run['matchWallSeconds']/a['pairs']/3600}
    return {'path':str(path),'sha256':b.digest(path/'run.json'),'arguments':a,'wdl':audit['wdl'],
            'statistics':audit['statistics'],'pairedNormal95Elo':[elo(max(0,mean-half)),elo(min(1,mean+half))],
            'pairVariance':variance,'pairs':len(pairs),'wallSeconds':run['matchWallSeconds'],
            'wholeRequestResources':audit['wholeRequestResources'],'cpuSeconds':cpu['cpuSeconds'],
            'candidateToBaselineCpu':cpu['candidateToBaselineCpu'],'illustrative80PercentPower':forecasts,
            'limitations':'Exploratory unless independently bound to a prospective confirmation. Normal intervals are approximate and unadjusted here. Power forecasts are plug-in planning estimates, not game quotas or predicted true effects.'}


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--cost',action='append',default=[])
    p.add_argument('--match',action='append',default=[]);p.add_argument('--out',required=True);a=p.parse_args()
    b.save(a.out,{'costs':[costs(x) for x in a.cost],'matches':[match(x) for x in a.match]})
