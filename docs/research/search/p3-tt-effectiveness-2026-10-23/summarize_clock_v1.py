"""Fixed endpoint development reconciliation, with no extra statistical looks."""
import json
from run_diagnostic_v1 import digest, write_new, HERE

report={'status':'passed','protocolSha256':digest(HERE/'development-clock-v1.json'),'domains':{},
        'interpretation':'Development only, correlated openings across W1/W4; no pooled estimate, confirmation or production acceptance. Point estimates and intervals crossing zero do not establish gain.'}
for workers in (1,4):
    name=f'development-clock-w{workers}-v1';path=HERE/'raw'/name
    receipt=json.loads((HERE/f'{name}-execution.json').read_text())
    run=json.loads((path/'run.json').read_text());cost=json.loads((path/'process-cost.json').read_text())
    assert receipt['status']==run['status']==cost['status']=='passed'
    result=run['results'];assert result['terminations']=={'normal':64}
    score=result['statistics']['score']
    decision='eligible for prospective fresh confirmation' if score>=.55 else ('unpromising in this development domain' if score<.5 else 'inconclusive/deferred under frozen resource selection rule')
    report['domains'][str(workers)]={'rawPath':str(path.relative_to(HERE)),
        'rawManifestSha256':digest(path/'run.json'),'costSha256':digest(path/'process-cost.json'),
        'wdl':result['wdl'],'statistics':result['statistics'],'terminations':result['terminations'],
        'resources':result['wholeRequestResources'],'wallSeconds':run['wallSeconds'],
        'matchWallSeconds':run['matchWallSeconds'],'cpuSeconds':cost['cpuSeconds'],
        'candidateToBaselineCpu':cost['candidateToBaselineCpu'],
        'peakWorkingSetBytes':{p['actor']:p.get('peakWorkingSetBytes') for p in cost['processes']},
        'ownedProcessesRemaining':receipt['ownedProcessesRemaining'],'decision':decision}
write_new(HERE/'development-clock-v1-summary.json',report)
print(json.dumps(report,indent=2))
