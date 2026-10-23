"""Assess only complete, resource-qualified frozen stages at their fixed endpoint."""
import json
import math
import statistics
from run_diagnostic_v1 import digest, write_new, HERE

def assess(label,s,plan_path):
    report_path=HERE/f'tt-session-{label}-v1-summary.json'
    if report_path.exists():
        report=json.loads(report_path.read_text())
        assert report['planSha256']==digest(plan_path)
        return report
    plan=s.verify(plan_path)
    with s.locked(plan_path.parent/'execution.lock'):
        blocks=s.reconcile(plan_path,plan)
    assert len(blocks)==len(plan['blocks'])
    output=plan_path.parent/'export'
    if not output.exists():s.export_stage(plan_path,output)
    complete=s.read(output/'complete.json')
    assert complete['planSha256']==digest(plan_path)
    assert digest(output/'games.pgn')==complete['gamesSha256']
    assert digest(output/'audit.json')==complete['auditSha256']
    b=s.module('final_audit',plan_path.parent/'inputs/tools/search-baseline.py')
    audit=b.analyze(output/'games.pgn',plan_path.parent/'inputs/schedule.pgn',plan['pairs'],
                    require_accounting=True,unordered=True,statistics_enabled=False)
    assert audit==s.read(output/'audit.json')
    assert audit['terminations']=={'normal':2*plan['pairs']}
    report={'status':'passed','planSha256':digest(plan_path),'rawPath':str(plan_path.parent.relative_to(HERE)),
        'stage':label,'pairs':plan['pairs'],'exportSha256':digest(output/'complete.json'),
        'wdl':audit['wdl'],'resources':audit['wholeRequestResources'],
        'assessorSha256':digest(__file__),'cpuSeconds':{'baseline':0,'candidate':0},
        'peakWorkingSetBytes':{'baseline':0,'candidate':0},'matchWallSeconds':0,
        'ownedCpuSeconds':0,'hostOtherAverageLogicalCores':[]}
    for block in plan['blocks']:
        folder=plan_path.parent/f"block-{block['index']:04d}"
        cost=s.read(folder/'process-cost.json');run=s.read(folder/'run.json')
        for side in ('baseline','candidate'):
            report['cpuSeconds'][side]+=cost['cpuSeconds'][side]
        for process in cost['processes']:
            side=process['actor']
            report['peakWorkingSetBytes'][side]=max(report['peakWorkingSetBytes'][side],process['peakWorkingSetBytes'])
        report['matchWallSeconds']+=run['matchWallSeconds']
        host=s.read(plan_path.parent/f"block-{block['index']:04d}.host.json")
        report['ownedCpuSeconds']+=host['ownedCpuSeconds']
        report['hostOtherAverageLogicalCores'].append(host['otherAverageLogicalCores'])
    report['candidateToBaselineCpu']=report['cpuSeconds']['candidate']/report['cpuSeconds']['baseline']
    if label=='pilot':
        sessions=list((plan_path.parent/'sessions').glob('*.json'))
        nonempty=[s.read(p) for p in sessions if s.read(p)['completedBlocks']]
        assert len(nonempty)==2 and all(len(x['completedBlocks'])==1 for x in nonempty)
        report['decision']='operational pause/resume/export qualified; no strength interpretation'
        report['statisticsAssessed']=False
    else:
        scores=[x['candidateScore'] for x in audit['games']]
        pairs=[(scores[i]+scores[i+1])/2 for i in range(0,len(scores),2)]
        mean=statistics.mean(pairs);se=math.sqrt(statistics.variance(pairs)/len(pairs))
        alpha=.025 if label in ('w1','w4') else .05
        z=statistics.NormalDist().inv_cdf(1-alpha/2)
        normal=[max(0,mean-z*se),min(1,mean+z*se)] if se>0 else None
        radius=math.sqrt(math.log(2/alpha)/(2*len(pairs)))
        bounded=[max(0,mean-radius),min(1,mean+radius)]
        def elo(x):
            return '-Infinity' if x==0 else ('Infinity' if x==1 else 400*math.log10(x/(1-x)))
        report['statistics']={'score':mean,'pentanomial':[pairs.count(x) for x in (0,.25,.5,.75,1)],
            'pairStandardError':se,'confidence':1-alpha,'normalScore':normal,
            'boundedScore':bounded,'logisticElo':elo(mean),
            'normalElo':[elo(x) for x in normal] if normal else None,
            'boundedElo':[elo(x) for x in bounded]}
        if label=='requal-w4':
            report['eligibleForConfirmation']=mean>=.55
            report['decision']='eligible' if mean>=.55 else 'not selected; unpromising/inconclusive development, no further screen'
        else:
            report['decision']=('gain supported under primary pair-normal assumptions' if normal and normal[0]>.5 else
                'inferiority supported under primary pair-normal assumptions' if normal and normal[1]<.5 else 'inconclusive; not equivalence')
        report['limits']='Domain-specific, selected candidate; normal approximation/exchangeable opening-pair assumptions. Conservative bounded interval is sensitivity. No pooling, extension, owner acceptance or production promotion.'
    write_new(report_path,report)
    return report
