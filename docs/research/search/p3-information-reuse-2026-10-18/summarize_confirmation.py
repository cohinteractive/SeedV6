"""Preserve a verified scheduled look with complete match-resource accounting.

protocol.json completed-stage-count NEW-summary.json; never assesses partial
stages, replaces a closed result, or changes the frozen statistical rule.
"""
import importlib.util
import json
from pathlib import Path
import sys


def summarize(protocol,count,output):
    protocol=Path(protocol).resolve();output=Path(output)
    if output.exists():raise ValueError('Never overwrite a scheduled-look summary')
    root=protocol.parent.parent;label=protocol.parent.name.removeprefix('confirmation-v1-')
    executor=root/'raw'/('confirmation-v1-'+label+'-tools')/'p3-information-confirmation.py'
    spec=importlib.util.spec_from_file_location('frozen_confirmation',executor)
    c=importlib.util.module_from_spec(spec);spec.loader.exec_module(c);t=c.t
    p=c.verify(protocol);assessment=c.assess(protocol,count)
    stored=Path(p['execution'])/f'assessment-{count}.json'
    t.require(assessment==t.read(stored),'Scheduled assessment is missing or changed')
    cpu={'baseline':0.,'candidate':0.};wall=0.;blocks=[];samples=[]
    for number,stage in enumerate(p['stages'][:count],1):
        planpath=Path(stage['plan']);plan=t.read(planpath)
        for block in plan['blocks']:
            folder=planpath.parent/f"block-{block['index']:04d}"
            receipt=t.read(folder.with_suffix('.json'));run=t.read(folder/'run.json');cost=t.read(folder/'process-cost.json')
            for name in ('run.json','process-cost.json'):
                t.require(t.digest(folder/name)==receipt['evidence'][name],'Completed resource evidence changed')
            t.require(run['status']==cost['status']=='passed','Unsuccessful resource audit')
            for actor in cpu:cpu[actor]+=cost['cpuSeconds'][actor]
            wall+=run['wallSeconds']
            blocks.append({'stage':number,'index':block['index'],'pairs':block['pairs'],
                           'wallSeconds':run['wallSeconds'],'cpuSeconds':cost['cpuSeconds'],
                           'receipt':str(folder.with_suffix('.json')),'receiptSha256':t.digest(folder.with_suffix('.json'))})
        for path in sorted(planpath.parent.parent.glob(planpath.parent.name+'-host*.json')):
            sample=t.read(path)
            samples.append({'path':str(path),'sha256':t.digest(path),
                            **{k:sample[k]for k in ('startedUtc','elapsedSeconds','systemMemoryKiB','systemCpuInstant','pagingInstant','limits')},
                            'busyProcesses':[{k:r[k]for k in ('name','averageLogicalCpus','workingSetBytes')}for r in sample['processes']]})
    games=sum(sum(record['wdl'])for record in assessment['records'])
    result={'protocol':str(protocol),'protocolSha256':t.digest(protocol),'sourceSha256':t.digest(__file__),
            'frozenAssessor':str(executor),'frozenAssessorSha256':t.digest(executor),
            'storedAssessment':str(stored),'storedAssessmentSha256':t.digest(stored),'assessment':assessment,
            'games':games,'fullMatchEngineCpuSeconds':cpu,'candidateToBaselineCpu':cpu['candidate']/cpu['baseline'],
            'blockWallSeconds':wall,'gamesPerHour':games*3600/wall,'blocks':blocks,'hostSamples':samples,
            'limits':'The frozen assessor owns inference and the prospective stopping decision. Worker domains are separate. Full match JVM CPU includes startup, warmup, JIT, GC and aborted requests; separate preflight JVMs are excluded. Equal clocks do not imply equal compute. Host point samples cannot reconstruct unsampled contention. A positive statistical decision is not owner acceptance or production adoption.'}
    c.verify(protocol);t.save(output,result)
    print(json.dumps({k:result[k]for k in ('games','candidateToBaselineCpu','gamesPerHour')},indent=2))
    print(json.dumps(assessment['looks'][-1],indent=2))


if __name__=='__main__':
    if len(sys.argv)!=4:raise ValueError(__doc__)
    summarize(sys.argv[1],int(sys.argv[2]),sys.argv[3])
