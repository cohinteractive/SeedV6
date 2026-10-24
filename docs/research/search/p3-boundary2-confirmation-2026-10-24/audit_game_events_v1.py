"""Join new game request telemetry/JFR/clock conservation; never expose game scores."""
from collections import Counter
from datetime import datetime
from decimal import Decimal
import json
from pathlib import Path
import re
import subprocess
import sys
from verify_inputs import HERE, ROOT, digest, write_new
from run_diagnostic_v1 import module


def epoch_ns(value):
    dt=datetime.fromisoformat(value)
    return int(dt.timestamp())*1_000_000_000+dt.microsecond*1000


def duration_ns(value):
    match=re.fullmatch(r'PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+(?:\.\d+)?)S)?',value)
    assert match,value
    h,m,s=match.groups()
    return int((Decimal(h or 0)*3600+Decimal(m or 0)*60+Decimal(s or 0))*1_000_000_000)


def main(name):
    receipt_path=HERE/(name+'-summary.json'); receipt=json.loads(receipt_path.read_text())
    assert receipt['status'] in ('passed','clock-unqualified','resource-unqualified')
    for path,sha in receipt['evidence'].items(): assert digest(HERE/path)==sha,path
    raw=HERE/'raw'/name; derived=HERE/'raw'/(name+'-event-audit-v1'); derived.mkdir(exist_ok=False)
    cost=json.loads((raw/'match/process-cost.json').read_text()); assert cost['status']=='passed'
    events={}; pauses={}; waits={}; paths={}
    for actor in ('baseline','candidate'):
        process=next(p for p in cost['processes'] if p['actor']==actor)
        matches=list((raw/'events').glob(actor+'-'+str(process['pid'])+'*.jfr')); assert len(matches)==1
        path=matches[0]; paths[actor]=str(path.relative_to(HERE)); target=derived/(actor+'.json')
        with target.open('xb') as stream:
            result=subprocess.run(['C:/java/jdk-21/bin/jfr.exe','print','--json','--events',
                'seedv6.ClockRequest,jdk.GCPhasePause,jdk.SafepointBegin,jdk.ExecuteVMOperation,jdk.JavaMonitorEnter,jdk.JavaMonitorWait,jdk.ThreadPark,jdk.FileRead,jdk.FileWrite',str(path)],
                stdout=stream,stderr=subprocess.PIPE,timeout=90,
                creationflags=subprocess.CREATE_NO_WINDOW|subprocess.BELOW_NORMAL_PRIORITY_CLASS)
        assert result.returncode==0,result.stderr.decode(errors='replace')
        values=json.loads(target.read_text())['recording']['events']
        events[actor]=sorted([e['values'] for e in values if e['type']=='seedv6.ClockRequest'],key=lambda x:x['ordinal'])
        assert [e['ordinal'] for e in events[actor]]==list(range(1,len(events[actor])+1))
        pauses[actor]=[]; waits[actor]=[]
        for event in values:
            if event['type']=='seedv6.ClockRequest': continue
            v=event['values']; start=epoch_ns(v['startTime']); duration=duration_ns(v['duration'])
            item={'type':event['type'],'startEpochNanos':start,'endEpochNanos':start+duration,'durationMillis':duration/1e6,
                  'operation':v.get('name',v.get('operation')),'thread':(v.get('eventThread') or {}).get('javaName')}
            if event['type'] in ('jdk.GCPhasePause','jdk.SafepointBegin','jdk.ExecuteVMOperation'): pauses[actor].append(item)
            else: waits[actor].append(item)
    b=module('baseline',ROOT/'tools/search-baseline.py')
    cursors=Counter(); rows=[]; games=0
    with (raw/'match/games.pgn').open(encoding='utf8') as stream:
        while (game:=b.chess.pgn.read_game(stream)) is not None:
            assert not game.errors; games+=1; board=game.board(); previous={'baseline':10100,'candidate':10100}
            for ply,node in enumerate(game.mainline(),1):
                match=re.search(r'info string request nodes=(\d+) elapsedNanos=(\d+) termination=(\w+)',node.comment)
                actor=game.headers['White' if board.turn else 'Black']; white=board.turn
                move=node.move; assert move in board.legal_moves; board.push(move)
                if match is None:
                    assert ply<=16 and 'book' in node.comment; continue
                event=events[actor][cursors[actor]]; cursors[actor]+=1
                assert event['telemetryValid'] and event['completedDelivery']
                assert event['accountedNodes']==int(match[1]) and event['reportedElapsedNanos']==int(match[2])
                assert event['termination']==match[3] and event['bestmove']==move.uci()
                assert event['profile']==('baseline' if actor=='baseline' else 'boundary2')
                tokens=event['command'].split(); assert tokens[0]=='go'
                go=dict(zip(tokens[1::2],tokens[2::2])); before=int(go['wtime' if white else 'btime'])
                assert before==previous[actor] and int(go['winc' if white else 'binc'])==100
                left=round(float(re.search(r'\btl=(\d+(?:\.\d+)?)s',node.comment)[1])*1000)
                charged=round(float(re.search(r'^\S+/\d+ (\d+(?:\.\d+)?)s',node.comment)[1])*1000)
                assert left==before+100-charged
                previous[actor]=left
                base=min(max(0,before-50),max(1,before//30+65))
                hard=base if actor=='baseline' else min(max(0,before-50),2*base)
                start=epoch_ns(event['startTime'])
                early=start+hard*1_000_000
                late=start+(event['reportedAtNanos']-event['receivedNanos'])-event['reportedElapsedNanos']+hard*1_000_000
                end=start+(event['deliveredAtNanos']-event['receivedNanos'])
                row={'actor':actor,'gameOrdinal':games,'ply':ply,'eventOrdinal':event['ordinal'],
                     'beforeMillis':before,'hardMillis':hard,'reportedMillis':event['reportedElapsedNanos']/1e6,
                     'overrunMillis':event['reportedElapsedNanos']/1e6-hard,'runnerChargeMillis':charged,
                     'reserveBeforeIncrementMillis':left-100,'ownerCpuMillis':event['ownerCpuNanos']/1e6,
                     'processCpuMillis':event['processCpuNanos']/1e6,'termination':event['termination'],
                     'boundaryMillis':(event['deliveredAtNanos']-event['receivedNanos'])/1e6,
                     'ownerCpuMinusReportedMillis':(event['ownerCpuNanos']-event['reportedElapsedNanos'])/1e6}
                if row['overrunMillis']>5 or charged-hard>10:
                    row['position']=event['position']; row['command']=event['command']
                    row['deadlineOverlappingPausesApprox']=[p for p in pauses[actor] if p['startEpochNanos']<=late+2e6 and p['endEpochNanos']>=early-2e6]
                    row['requestPausesApprox']=[p for p in pauses[actor] if p['startEpochNanos']<=end+2e6 and p['endEpochNanos']>=start-2e6]
                    row['requestOwnerWaitsApprox']=[p for p in waits[actor] if p['thread']=='seedv6-search-worker' and p['startEpochNanos']<=end and p['endEpochNanos']>=start]
                rows.append(row)
    assert games==sum(receipt['integrity']['terminations'].values())
    assert all(cursors[a]==len(events[a])==receipt['integrity']['wholeRequestResources'][a]['requests'] for a in events)
    totals={}
    for actor in events:
        selected=[r for r in rows if r['actor']==actor]
        totals[actor]={'requests':len(selected),'maximumOverrunMillis':max(r['overrunMillis'] for r in selected),
            'over25Millis':sum(r['overrunMillis']>25 for r in selected),'minimumReserveBeforeIncrementMillis':min(r['reserveBeforeIncrementMillis'] for r in selected),
            'reportedSeconds':sum(r['reportedMillis'] for r in selected)/1000,'ownerCpuSeconds':sum(r['ownerCpuMillis'] for r in selected)/1000,
            'processCpuSeconds':sum(r['processCpuMillis'] for r in selected)/1000,
            'maximumJvmPauseMillis':max((p['durationMillis'] for p in pauses[actor]),default=0)}
    clock_pass=all(r['overrunMillis']<=25 for r in rows)
    record={'status':'passed' if clock_pass and receipt['status']=='passed' else 'clock-unqualified' if not clock_pass else receipt['status'],
            'telemetryPassed':True,'games':games,'clockConservationChecks':len(rows),'actors':totals,
            'adverseRequests':[r for r in rows if 'position' in r], 'jfrInputs':paths,'scores':'Unopened; diagnostic only',
            'receiptSha256':digest(receipt_path),'analyzerSha256':digest(__file__),
            'derivedEvidence':{str(p.relative_to(HERE)):digest(p) for p in derived.iterdir()},
            'limits':['Approximate Publication/deadline bracketing; no direct first-checkpoint timestamp.',
                      'JFR UTC parsing is microsecond resolution and pause matching allows2ms tolerance.',
                      'Per-owner CPU has Windows granularity; a deficit alone is not an OS preemption diagnosis.',
                      'Diagnostic overhead/domain differs from uninstrumented production; no strength or historical-cause inference.']}
    write_new(HERE/(name+'-event-audit.json'),record)
    print(json.dumps({k:v for k,v in record.items() if k not in ('adverseRequests','derivedEvidence','limits')},indent=2))


if __name__=='__main__': main(sys.argv[1])
