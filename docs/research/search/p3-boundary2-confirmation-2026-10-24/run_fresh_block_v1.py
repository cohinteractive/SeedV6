"""One frozen fresh confirmation block; operational qualification only, no scores."""
from datetime import datetime,timezone
import json
from pathlib import Path
import sys
import time
from types import SimpleNamespace
from verify_inputs import HERE, ROOT, OLD, digest, write_new
from run_diagnostic_v1 import module, background
from run_delivery_v1 import execute, host_interval
from study_common_v1 import stable, qualified_block, reference, read_reference, evidence
from audit_fresh_clock_v1 import audit as clock_audit


def main(block,attempt,worker):
    background(); binding=stable(); assert 1<=block<=6
    name=binding['blocks'][block-1]; plan=json.loads((HERE/'confirmation-v1.json').read_text())
    if worker:
        assert sys.stdin.readline().strip()=='GO'
        m=module('spending',OLD/'spending_match_v1.py')
        m.run(SimpleNamespace(baseline=str(OLD/'raw/runtime-production-v2'),candidate=str(OLD/'raw/runtime-candidate-v2'),
            baseline_policy='baseline',candidate_policy='boundary2',baseline_workers=1,candidate_workers=1,
            java='C:/java/jdk-21/bin/java.exe',network=str(ROOT/'build-assets/nnue-candidate/conventional-cp64.cnnue'),
            runner=str(ROOT/'build/research/tools/fastchess.exe'),openings=str(HERE/(name+'-openings.pgn')),
            out=str(HERE/'raw'/name),hash_mib=64,background=True,operational=True,concurrency=1,pairs=32,
            tc='10+0.1',seed=plan['domain']['seed']+block,timeout=5400))
        return
    assert attempt>=1 and not (HERE/'raw'/name).exists() and not (HERE/(name+'-launch.json')).exists()
    for other in binding['blocks']:
        assert not (HERE/(other+'-launch.json')).exists() or (HERE/(other+'-summary.json')).exists(),('Unclosed block',other)
    previous=[]
    for earlier in binding['blocks'][:block-1]: previous.append(qualified_block(earlier)[0])
    admission_path=HERE/f'{name}-admission-{attempt:02}.json'; assert not admission_path.exists()
    if attempt>1:
        prior=json.loads((HERE/f'{name}-admission-{attempt-1:02}.json').read_text()); assert not prior['passed']
        assert (datetime.now(timezone.utc)-datetime.fromisoformat(prior['end']['utc'])).total_seconds()>=900
    pre=previous[-1]['postDelivery'] if previous else binding['firstDelivery']
    delivery=read_reference(pre); evidence(delivery)
    assert delivery['status']=='passed' and delivery['criteriaPassed']
    if (datetime.now(timezone.utc)-datetime.fromisoformat(delivery['resources']['end']['utc'])).total_seconds()>900:
        pre_name=f'{name}-a{attempt:02}-pre-delivery'; delivery=execute(pre_name)
        pre=reference(HERE/(pre_name+'-summary.json'))
    if not delivery['criteriaPassed']:
        write_new(admission_path,{'passed':False,'end':delivery['resources']['end'],'reason':'Pre-delivery failed','preDelivery':pre})
        print('Prelaunch delivery failed; block unstarted'); return
    host=module('host',ROOT/'tools/p3-tt-session.py'); owned=module('owned',ROOT/'tools/tournament-process.py')
    b=module('baseline',ROOT/'tools/search-baseline.py')
    start=host.host_sample(); time.sleep(30); admission=host_interval(start,host.host_sample())
    write_new(admission_path,admission)
    if not admission['passed']: print('Host admission failed; block unstarted'); return
    stable(); start=host.host_sample()
    record={'status':'running','block':block,'preDelivery':pre,'bindingSha256':digest(HERE/'confirmation-v1-binding.json'),
        'admission':reference(admission_path),'scores':'Unopened until all six blocks qualify'}
    write_new(HERE/(name+'-launch.json'),record|{'hostStart':start,'attempt':attempt})
    try:
        with (HERE/'raw'/(name+'-worker.log')).open('xb') as stream:
            record['execution']=owned.run_owned([sys.executable,'-B',str(Path(__file__).resolve()),str(block),str(attempt),'worker'],
                stream,5500,ROOT,b.clean_environment())
        record['resourceQualification']=host_interval(start,host.host_sample(),record['execution']['totalOwnedCpuSeconds'])
        assert record['execution']['exitCode']==0 and record['execution']['ownedProcessesRemaining']==0
        post=execute(name+'-post-delivery'); record['postDelivery']=reference(HERE/(name+'-post-delivery-summary.json'))
        result=json.loads((HERE/'raw'/name/'run.json').read_text())
        assert result['status']=='passed' and result['results']['statistics'] is None
        assert result['results']['terminations']=={'normal':64}
        record['integrity']={k:result['results'][k] for k in ('terminations','wholeRequestResources')}
        cost=json.loads((HERE/'raw'/name/'process-cost.json').read_text()); assert cost['status']=='passed'
        ratios={a:cost['cpuSeconds'][a]/(result['results']['wholeRequestResources'][a]['elapsedNanos']/1e9) for a in ('baseline','candidate')}
        spread=abs(ratios['baseline']-ratios['candidate'])
        record['actorDelivery']={'nativeCpuToReportedElapsed':ratios,'absoluteDifference':spread,
            'passed':all(.90<=r<=1.25 for r in ratios.values()) and spread<=.10}
        clock=clock_audit(name); record['clockAudit']=reference(HERE/(name+'-clock-audit.json'))
        record['clockReliability']={k:clock[k] for k in ('clockReliabilityPassed','completeClockPassed','maximumRequestOverHardMillis','maximumRunnerChargeOverHardMillis')}
        record['matchWallSeconds']=result['matchWallSeconds']; stable()
        record['status']='passed' if record['resourceQualification']['passed'] and post['criteriaPassed'] and record['actorDelivery']['passed'] else 'resource-unqualified'
        if not clock['clockReliabilityPassed'] or not clock['completeClockPassed']: record['status']='clock-unqualified'
    except BaseException as error:
        record.update(status='failed',failure=repr(error)); raise
    finally:
        record['evidence']={str(p.relative_to(HERE)):digest(p) for p in (HERE/'raw'/name).rglob('*') if p.is_file()}
        log=HERE/'raw'/(name+'-worker.log')
        if log.exists(): record['evidence'][str(log.relative_to(HERE))]=digest(log)
        write_new(HERE/(name+'-summary.json'),record)
    print(json.dumps({k:record[k] for k in ('status','block','clockReliability','actorDelivery','scores')},indent=2))


if __name__=='__main__': main(int(sys.argv[1]),int(sys.argv[2]) if len(sys.argv)>2 else 1,len(sys.argv)>3 and sys.argv[3]=='worker')
