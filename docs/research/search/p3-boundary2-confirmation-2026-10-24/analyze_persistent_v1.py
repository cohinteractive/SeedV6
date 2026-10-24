"""Complete persistent timing analysis, without any game result or strength data."""
from collections import Counter
import json
import statistics
import sys
from verify_inputs import HERE, digest, write_new
from analyze_diagnostic_v1 import safepoints


def analyze(name):
    receipt_path=HERE/(name+'-summary.json')
    receipt=json.loads(receipt_path.read_text())
    assert receipt['status'] in ('passed','resource-unqualified')
    for path,sha in receipt['evidence'].items(): assert digest(HERE/path)==sha,path
    raw=HERE/'raw'/name
    rows=[json.loads(line) for line in (raw/'requests.jsonl').read_text().splitlines()]
    assert len(rows)==7168
    events={p:safepoints(raw/(p+'-gc.log')) for p in ('baseline','boundary2')}
    wall_disagreement=max(abs((r['endEpochNanos']-r['startEpochNanos'])-r['externalElapsedNanos'])/1e6 for r in rows)
    assert wall_disagreement<=2, 'UTC/monotonic disagreement invalidates pause correlation'
    summaries={}; adverse=[]
    for profile in ('baseline','boundary2'):
        selected=[r for r in rows if r['profile']==profile]; assert len(selected)==3584
        summaries[profile]={'requests':len(selected), 'maximumOverrunMillis':max(r['overrunMillis'] for r in selected),
            'over25Millis':sum(r['overrunMillis']>25 for r in selected),
            'maximumTransportResidualMillis':max((r['externalElapsedNanos']-r['result']['elapsedNanos'])/1e6 for r in selected),
            'ownerCpuSeconds':sum(r['ownerCpuSeconds'] for r in selected),
            'nativeCpuSeconds':sum(r['nativeCpuSeconds'] for r in selected),
            'requestSeconds':sum(r['result']['elapsedNanos'] for r in selected)/1e9,
            'terminations':dict(Counter(r['result']['termination'] for r in selected)),
            'totalSafepoints':len(events[profile]),
            'maximumSafepointMillis':max(e['durationMillis'] for e in events[profile]),
            'maximumReachingSafepointMillis':max(e['reachingMillis'] for e in events[profile])}
    for row in rows:
        if row['overrunMillis']<=5: continue
        # Publication starts after command receipt and before elapsed annotation is emitted.
        # Transport is excluded from UCI elapsed; these external bounds bracket its start.
        early_deadline=row['startEpochNanos']+row['hardMillis']*1e6
        late_deadline=row['endEpochNanos']-row['result']['elapsedNanos']+row['hardMillis']*1e6
        es=[e for e in events[row['profile']] if e['startEpochNanosApprox']<=row['endEpochNanos']+2e6
            and e['endEpochNanosApprox']>=row['startEpochNanos']-2e6]
        overlaps=[e for e in es if e['startEpochNanosApprox']<=late_deadline+2e6
                  and e['endEpochNanosApprox']>=early_deadline-2e6]
        adverse.append({k:v for k,v in row.items() if k!='result'}|{
            'elapsedNanos':row['result']['elapsedNanos'],'termination':row['result']['termination'],
            'earliestDeadlineEpochNanosApprox':early_deadline,'latestDeadlineEpochNanosApprox':late_deadline,
            'overlappingSafepoints':es,'deadlineOverlappingSafepointsApprox':overlaps,
            'requestMinusOwnerCpuMillis':row['result']['elapsedNanos']/1e6-row['ownerCpuSeconds']*1000,
            'interpretation':'JVM pause near deadline; inspect precise JFR and transport uncertainty before attributing'
                if overlaps else 'No recorded JVM safepoint overlaps approximate deadline; scheduling/checkpoint/reporting remains unresolved'})
    result={'status':'analyzed','captureStatus':receipt['status'],'requests':len(rows),'actors':summaries,
            'maximumUtcMonotonicIntervalDisagreementMillis':wall_disagreement,
            'adverseOver5ms':adverse,'receiptSha256':digest(receipt_path),'analyzerSha256':digest(__file__),
            'limits':['Outcome-free timing diagnostic, not strength evidence or reconstruction of historical failures.',
                      'OS native CPU granularity and process background work preclude exact per-request scheduling attribution.',
                      'UTC log time is rounded to milliseconds; deadline bracketing omits small lifecycle setup differences.',
                      'Safepoint association can identify a plausible measured mechanism; absence is not absence of OS stalls.']}
    write_new(HERE/(name+'-analysis.json'),result)
    print(json.dumps({k:v for k,v in result.items() if k not in ('adverseOver5ms','limits')},indent=2))


if __name__=='__main__': analyze(sys.argv[1])
