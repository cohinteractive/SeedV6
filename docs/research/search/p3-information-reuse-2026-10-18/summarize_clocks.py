"""One completed frozen exploratory screen; no partial-sample decision or replay.

Run with the validated research Python: plan.json NEW-local-export NEW-summary.json.
Existing frozen session and PGN auditors own completion/input/lifecycle checks.
"""
import importlib.util
import json
from pathlib import Path
import sys


def main(plan_path, export_path, summary_path):
    plan_path=Path(plan_path).resolve();summary_path=Path(summary_path)
    if summary_path.exists():raise ValueError('Never overwrite an assessment')
    tools=plan_path.parent/'inputs/tools'
    spec=importlib.util.spec_from_file_location('frozen_session',tools/'tournament-session.py')
    s=importlib.util.module_from_spec(spec);spec.loader.exec_module(s)
    plan=s.verify(plan_path)
    if plan['purpose']!='exploratory':raise ValueError('Development summaries cannot assess confirmation')
    result=s.export_stage(plan_path,export_path)
    if result['status']!='complete':raise ValueError('Whole prospective sample required')
    b=s.module('frozen_baseline',tools/'search-baseline.py')
    audit=b.analyze(Path(export_path)/'games.pgn',plan_path.parent/'inputs/schedule.pgn',plan['pairs'],
                    require_accounting=True,unordered=True,statistics_enabled=True)
    cpu={'baseline':0.,'candidate':0.};wall=0.;blocks=[]
    for block in plan['blocks']:
        root=plan_path.parent/f"block-{block['index']:04d}"
        run=s.read(root/'run.json');cost=s.read(root/'process-cost.json')
        wall+=run['wallSeconds']
        for actor in cpu:cpu[actor]+=cost['cpuSeconds'][actor]
        blocks.append({'index':block['index'],'pairs':block['pairs'],'wallSeconds':run['wallSeconds'],
                       'runSha256':s.digest(root/'run.json'),'processCostSha256':s.digest(root/'process-cost.json')})
    host_samples=[]
    for p in sorted(plan_path.parent.parent.glob(plan_path.parent.name+'-host*.json')):
        sample=s.read(p)
        host_samples.append({'path':str(p),'sha256':s.digest(p),
                             **{k:sample[k]for k in ('startedUtc','elapsedSeconds','systemMemoryKiB','systemCpuInstant','pagingInstant','limits')},
                             'busyProcesses':[{k:row[k]for k in ('name','averageLogicalCpus','workingSetBytes')}for row in sample['processes']]})
    summary={'purpose':'exploratory','plan':str(plan_path),'planSha256':s.digest(plan_path),
             'sourceSha256':s.digest(__file__),'export':result,'exportReceiptSha256':s.digest(Path(export_path)/'complete.json'),
             'configuration':plan['arguments'],'wdl':audit['wdl'],'terminations':audit['terminations'],
             'statistics':audit['statistics'],'wholeRequestResources':audit['wholeRequestResources'],
             'fullMatchEngineCpuSeconds':cpu,'candidateToBaselineCpu':cpu['candidate']/cpu['baseline'],
             'blockWallSeconds':wall,'gamesPerHour':plan['pairs']*2*3600/wall,'blocks':blocks,'hostSamples':host_samples,
             'limitations':'Development selection, shared openings and multiple correlated comparisons; intervals are descriptive, no promotion or confirmation claim. Pair model assumes independent opening observations. Process CPU includes match JVM startup/warmup/JIT/GC/aborted requests, but excludes separate preflight JVMs. Equal clocks do not establish equal compute. Host point samples cannot reconstruct unsampled activity on this shared workstation. One-worker evidence cannot establish multiworker strength.'}
    s.save(summary_path,summary)
    print(json.dumps({k:summary[k]for k in ('wdl','statistics','candidateToBaselineCpu','gamesPerHour')},indent=2))


if __name__=='__main__':
    if len(sys.argv)!=4:raise ValueError('plan.json NEW-export NEW-summary.json')
    main(*sys.argv[1:])
