"""Audit and summarize the separately planned deeper lifetime replay."""
from collections import Counter
import hashlib
import json
from pathlib import Path


def sha(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()
def read(path):return json.loads(Path(path).read_text(encoding='utf-8-sig'))


if __name__=='__main__':
    root=Path(__file__).resolve().parent;out=root/'lifetime-v2-summary.json'
    if out.exists():raise ValueError('Never overwrite completed analysis')
    manifest=root/'lifetime-v2-manifest.json';m=read(manifest);plan=root/'lifetime-v2.json';p=read(plan)
    execution=root/'lifetime-v2-execution.json';run=read(execution)
    assert run['status']=='passed' and run['exitCode']==0 and sha(plan)==m['planSha256']
    assert sha(m['inputSelection'])==m['inputSelectionSha256'] and sha(m['runner'])==m['runnerSha256']
    assert all(sha(path)==value for path,value in m['frozenLibraries'].items())
    assert all(sha(root/'raw/lifetime-v2-classes'/path)==value for path,value in m['frozenClasses'].items())
    selection=read(m['inputSelection']);assert sha(selection['output'])==selection['outputSha256']
    raw=root/'raw/lifetime-v2.jsonl';rows=[json.loads(line)for line in raw.read_text().splitlines()]
    footer=rows.pop();assert footer=={'status':'passed','observations':len(rows)}
    requested=max(p['configs']['depths']);end=p['configs']['endPlyExclusive']
    expected={(f"game-{g['scheduledGameIndex']}",ply)for g in selection['games']for ply in range(16,min(end,g['fullPlies']))}
    configs=[]
    names=('probes','hits','currentHits','olderHits','proofBlocked','deeperBlocked','emptyWrites','sameKeyWrites','collisionWrites','proofSaveAttempts')
    for policy in p['configs']['policies']:
        for size in p['configs']['requestedTTMiB']:
            group=[r for r in rows if r['policy']==policy and r['requestedMiB']==size]
            assert len(group)==len(expected) and {(r['game'],r['ply'])for r in group}==expected
            assert all(r['requestedDepth']==requested and r['recursiveEntryCapPerInvocation']==p['configs']['recursiveEntryCapPerInvocation']for r in group)
            counters={key:sum(r['tt'][key]for r in group)for key in names}
            assert counters['hits']==counters['currentHits']+counters['olderHits']
            ages={i:sum(r['tt']['hitAge'][i]for r in group)for i in range(256)}
            configs.append({'policy':policy,'requestedMiB':size,'entries':group[0]['tt']['entries'],
                            'requests':len(group),'completedDepthHistogram':dict(sorted(Counter(r['completedDepth']for r in group).items())),
                            'incompleteRequestedDepth':sum(r['completedDepth']<requested for r in group),
                            'recursiveNodes':sum(r['recursiveNodes']for r in group),'counters':counters,
                            'hitAges':{k:v for k,v in ages.items()if v},
                            'olderHitFraction':counters['olderHits']/counters['hits']if counters['hits']else None,
                            'maximumHintOccupancyFraction':max(r['tt']['occupiedHints']/r['tt']['entries']for r in group),
                            'maximumProofOccupancyFraction':max(r['tt']['occupiedProof']/r['tt']['entries']for r in group)})
    assert len(rows)==len(expected)*len(configs)
    result={'id':p['id'],'sourceSha256':sha(__file__),'planSha256':sha(plan),'manifestSha256':sha(manifest),
            'executionSha256':sha(execution),'raw':str(raw),'rawSha256':sha(raw),
            'status':'passed; every plain/observed invocation matched, including capped invocations',
            'observations':len(rows),'configs':configs,'limits':p['limitations']}
    out.write_bytes((json.dumps(result,indent=2)+'\n').encode());print(json.dumps(configs,indent=2))
