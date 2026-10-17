"""Summarize completed operational pilots; never calculate strength statistics."""
import hashlib
import json
from pathlib import Path
import re

HERE=Path(__file__).resolve().parent


def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def read(path):return json.loads(path.read_text(encoding='utf-8-sig'))


def main():
    execution=read(HERE/'calibration-execution.json')
    if execution['status']!='passed':raise ValueError('Campaign incomplete')
    extra=read(HERE/'extra-calibration.json')
    if extra['status']!='passed':raise ValueError('Additional calibration incomplete')
    for row in extra['sessions']:
        execution['sessions'].append({**row,'index':f"c4-{row['index']}"})
        execution['order'].append(row['study'])
    rows=[]
    for name in dict.fromkeys(execution['order']):
        study=HERE/'raw'/name;plan=read(study/'plan.json');blocks=[]
        for block in plan['blocks']:
            receipt=read(study/f"block-{block['index']:04d}.json")
            folder=study/f"block-{block['index']:04d}"
            if any(sha(folder/file)!=digest for file,digest in receipt['evidence'].items()):
                raise ValueError('Completed evidence changed')
            run=read(folder/'run.json');summary=receipt['summary']
            pauses=[]
            for log in folder.glob('gc-*.log'):
                pauses.extend(float(x) for x in re.findall(r'Pause[^\n]*? ([\d.]+)ms',log.read_text()))
            cost=read(folder/'process-cost.json')
            blocks.append({**summary,'gamesPerMatchHour':summary['games']*3600/summary['matchWallSeconds'],
                'pairIds':summary['pairIds'],'gcPauseCount':len(pauses),'gcPauseMilliseconds':sum(pauses),
                'maxGcPauseMilliseconds':max(pauses,default=None),
                'preflightAndAuditSeconds':summary['runnerWallSeconds']-summary['matchWallSeconds'],
                'wholeRequestResources':run['results']['wholeRequestResources'],
                'processCpuSeconds':[{'actor':p['actor'],'cpuSeconds':p['lifetime']['totalCpuSeconds']} for p in cost['processes']],
                'receiptSha256':sha(study/f"block-{block['index']:04d}.json")})
        match=sum(x['matchWallSeconds'] for x in blocks);runner=sum(x['runnerWallSeconds'] for x in blocks)
        sessions=[x for x in execution['sessions'] if x['study']==name]
        session=sum(x['sessionWallSeconds'] for x in sessions);games=sum(x['games'] for x in blocks)
        cpu={actor:sum(x['cpuSeconds'][actor] for x in blocks) for actor in ('baseline','candidate')}
        samples=[read(HERE/'raw'/f"host-{x['index']}.json") for x in sessions]
        rows.append({'study':name,'workers':plan['arguments']['baseline_workers'],
            'concurrency':plan['arguments']['concurrency'],'timeControl':plan['arguments']['tc'],
            'games':games,'pairs':games//2,'matchSeconds':match,'runnerSeconds':runner,'sessionSeconds':session,
            'gamesPerMatchHour':games*3600/match,'pairsPerMatchHour':games*1800/match,
            'gamesPerSessionHour':games*3600/session,'pairsPerSessionHour':games*1800/session,
            'blockGamesPerHourRange':[min(x['gamesPerMatchHour'] for x in blocks),max(x['gamesPerMatchHour'] for x in blocks)],
            'cpuSeconds':cpu,'averageEngineLogicalCpus':sum(cpu.values())/match,
            'candidateToBaselineCpu':cpu['candidate']/cpu['baseline'],
            'minimumSampledFreeMemoryGiB':min(x['systemMemoryKiB']['FreePhysicalMemory'] for x in samples)/1024**2,
            'sampledSystemCpuPercent':[x['systemCpuInstant']['PercentProcessorTime'] for x in samples],
            'pagingSamples':[x['pagingInstant'] for x in samples],
            'planSha256':sha(study/'plan.json'),'blocks':blocks})
    output={'classification':'Operational/exploratory A/A; no independent strength evidence',
        'sameFrozenSchedule':len({tuple(p for b in x['blocks'] for p in b['pairIds']) for x in rows})==1,
        'studies':rows,'limits':'Two six-pair strata per setting, same 12 development openings, fixed order, not random population confidence bounds. Startup and drain effects matter. Host sampling is sparse; native CPU is full engine lifetime. No inference of candidate/comparator immunity to contention.'}
    (HERE/'calibration-summary.json').write_text(json.dumps(output,indent=2)+'\n')
    print(json.dumps([{k:x[k] for k in ('study','games','gamesPerMatchHour','pairsPerMatchHour','gamesPerSessionHour','averageEngineLogicalCpus')} for x in rows],indent=2))


if __name__=='__main__':main()
