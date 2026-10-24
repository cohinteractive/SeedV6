"""Cross-check custom JFR request events against independent UCI client observations."""
import json
from pathlib import Path
import subprocess
import sys
from verify_inputs import HERE, digest, write_new
from analyze_diagnostic_v1 import analyze


def main(name):
    analyze(name)
    summary=json.loads((HERE/(name+'-analysis.json')).read_text())
    capture=HERE/'raw'/name
    derived=HERE/'raw'/(name+'-events-v1'); derived.mkdir(exist_ok=False)
    rows=[json.loads(line) for line in (capture/'requests.jsonl').read_text().splitlines()]
    record={'status':'running','requestEvents':0,'runs':{},'failures':[],'analyzerSha256':digest(__file__)}
    try:
        for label in sorted({r['run'] for r in rows if r['instrumented']}):
            output=derived/(label+'.json')
            with output.open('xb') as stream:
                completed=subprocess.run(['C:/java/jdk-21/bin/jfr.exe','print','--json','--events',
                    'seedv6.ClockRequest,jdk.GCPhasePause,jdk.SafepointBegin,jdk.ExecuteVMOperation,jdk.JavaMonitorEnter,jdk.JavaMonitorWait,jdk.ThreadPark',
                    str(capture/(label+'.jfr'))],stdout=stream,stderr=subprocess.PIPE,timeout=60,
                    creationflags=subprocess.CREATE_NO_WINDOW|subprocess.BELOW_NORMAL_PRIORITY_CLASS)
            assert completed.returncode==0,completed.stderr.decode(errors='replace')
            events=json.loads(output.read_text())['recording']['events']
            actual=[e['values'] for e in events if e['type']=='seedv6.ClockRequest']
            expected=[r for r in rows if r['run']==label]
            assert len(actual)==len(expected)==98,(label,len(actual))
            assert [e['ordinal'] for e in actual]==list(range(1,99)),label
            extra=[]
            for e,r in zip(actual,expected):
                assert e['telemetryValid'] and e['completedDelivery']
                assert e['profile']==r['profile'] and e['command']=='go '+r['go']
                assert e['accountedNodes']==r['result']['nodes']
                assert e['reportedElapsedNanos']==r['result']['elapsedNanos']
                assert e['termination']==r['result']['termination'] and e['bestmove']==r['result']['bestmove']
                assert e['ownerCpuNanos']>=0 and e['processCpuNanos']>=0
                duration=e['deliveredAtNanos']-e['receivedNanos']
                assert duration>=e['reportedElapsedNanos'] and e['reportedAtNanos']<=e['deliveredAtNanos']
                assert duration<=r['externalElapsedNanos']+2_000_000
                extra.append((duration-e['reportedElapsedNanos'])/1e6)
            record['requestEvents']+=len(actual)
            record['runs'][label]={'events':len(actual),'allClientFieldsMatch':True,
                                  'maximumBoundaryMinusReportedMillis':max(extra),'eventJsonSha256':digest(output)}
        for value in summary['instrumentationCost']:
            if not value['descriptiveFivePercentScreenPassed']:
                record['failures'].append('Instrumentation elapsed screen: '+value['profile'])
        for value in summary['timingGroups']:
            if value['maximumRequestOverrunMillis']>25:
                record['failures'].append('Clock screen: '+value['profile'])
        if summary['captureStatus']!='passed': record['failures'].append('Resource qualification')
        record['status']='passed' if not record['failures'] else 'unqualified'
        record['diagnosticAnalysisSha256']=digest(HERE/(name+'-analysis.json'))
        record['derivedEvidence']={str(p.relative_to(HERE)):digest(p) for p in derived.iterdir()}
    except BaseException as error:
        record.update(status='failed',failure=repr(error)); raise
    finally:
        write_new(HERE/(name+'-bridge-assessment.json'),record)
    print(json.dumps(record,indent=2))


if __name__=='__main__': main(sys.argv[1])
