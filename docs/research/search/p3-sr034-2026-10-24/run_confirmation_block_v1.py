"""One frozen SR-034 confirmation block; expose integrity/resources, not scores."""
import ctypes
import json
from datetime import datetime, timezone
from pathlib import Path
import sys
import time
from types import SimpleNamespace
from run_trajectory_v1 import HERE, ROOT, digest, write_new, module
from run_spending_economics_v1 import execute, host_interval


def main():
    assert sys.argv[1] == 'finalist'
    block = int(sys.argv[2]); assert 1 <= block <= 4
    binding = json.loads((HERE/'confirmation-v1-binding.json').read_text())
    stage = binding['stages']['finalist']; name = stage['blocks'][block-1]
    def stable():
        for p, sha in binding['files'].items(): assert digest(ROOT/p) == sha, p
        for p, sha in binding['externalFiles'].items(): assert digest(Path(p)) == sha, p
    stable()
    if len(sys.argv) > 3 and sys.argv[3] == 'worker':
        assert sys.stdin.readline().strip() == 'GO'
        m = module('match', HERE/'spending_match_v1.py')
        m.run(SimpleNamespace(baseline=str(HERE/'raw/runtime-production-v2'), candidate=str(HERE/'raw/runtime-candidate-v2'),
            network=str(ROOT/'build-assets/nnue-candidate/conventional-cp64.cnnue'), runner=str(ROOT/'build/research/tools/fastchess.exe'),
            openings=str(HERE/(name+'-openings.pgn')), out=str(HERE/'raw'/name), java='C:/java/jdk-21/bin/java.exe',
            baseline_policy='baseline', candidate_policy=stage['candidate'], baseline_workers=1, candidate_workers=1,
            hash_mib=64, background=True, operational=True, concurrency=1, pairs=32, tc='10+0.1',
            seed=stage['seed']+block, timeout=5400))
        return
    attempt = int(sys.argv[3]) if len(sys.argv) > 3 else 1; assert attempt >= 1
    k = ctypes.WinDLL('kernel32', use_last_error=True); k.GetCurrentProcess.restype = ctypes.c_void_p
    k.SetPriorityClass.argtypes = [ctypes.c_void_p, ctypes.c_ulong]
    assert k.SetPriorityClass(k.GetCurrentProcess(), 0x4000)
    assert not (HERE/(name+'-launch.json')).exists() and not (HERE/'raw'/name).exists(), 'Never replay a started block'
    previous = []
    for other in stage['blocks']:
        assert not (HERE/(other+'-launch.json')).exists() or (HERE/(other+'-summary.json')).exists(), 'Unclosed block: '+other
    for earlier in stage['blocks'][:block-1]:
        receipt = json.loads((HERE/(earlier+'-summary.json')).read_text()); assert receipt['status'] == 'passed', earlier
        assert receipt['bindingSha256'] == digest(HERE/'confirmation-v1-binding.json')
        for p, sha in receipt['evidence'].items(): assert digest(HERE/p) == sha, p
        for field in ('preDelivery','postDelivery','clockAnalysis'):
            ref = receipt[field]; assert digest(HERE/ref['path']) == ref['sha256'], field
        assert receipt['clockReliability']['clockReliabilityPassed']
        previous.append(receipt)
    if attempt > 1:
        prior = json.loads((HERE/f'{name}-admission-{attempt-1:02}.json').read_text())
        assert not prior['passed']
        assert (datetime.now(timezone.utc)-datetime.fromisoformat(prior['end']['utc'])).total_seconds() >= 900
    pre = previous[-1]['postDelivery'] if previous else stage['firstPreDelivery']
    path = HERE/pre['path']; assert digest(path) == pre['sha256']
    value = json.loads(path.read_text()); assert value['status'] == 'passed' and value['criteriaPassed']
    if (datetime.now(timezone.utc)-datetime.fromisoformat(value['hostInterval']['end']['utc'])).total_seconds() > 900:
        pre_name = f'{name}-a{attempt:02}-pre-delivery'; value = execute(pre_name, 'delivery')
        pre = {'path':pre_name+'-summary.json', 'sha256':digest(HERE/(pre_name+'-summary.json'))}
    admission_path = HERE/f'{name}-admission-{attempt:02}.json'
    if not value['criteriaPassed']:
        write_new(admission_path, {'passed':False, 'end':value['hostInterval']['end'], 'reason':'Pre-delivery resource gate', 'preDelivery':pre})
        raise SystemExit('Prelaunch delivery failed; block unplayed')
    host = module('host', ROOT/'tools/p3-tt-session.py'); owned = module('owned', ROOT/'tools/tournament-process.py')
    b = module('baseline', ROOT/'tools/search-baseline.py')
    start = host.host_sample(); time.sleep(30); admission = host_interval(start, host.host_sample())
    write_new(admission_path, admission)
    if not admission['passed']: raise SystemExit('Prelaunch host gate failed; block unplayed')
    stable(); start = host.host_sample()
    receipt = {'status':'running', 'hypothesis':'finalist', 'block':block, 'preDelivery':pre,
        'bindingSha256':digest(HERE/'confirmation-v1-binding.json'), 'admissionSha256':digest(admission_path)}
    write_new(HERE/(name+'-launch.json'), receipt | {'hostStart':start, 'admissionAttempt':attempt})
    try:
        with (HERE/'raw'/(name+'-worker.log')).open('xb') as log:
            receipt['execution'] = owned.run_owned([sys.executable, '-B', str(Path(__file__).resolve()), 'finalist', str(block), 'worker'],
                log, 5500, ROOT, b.clean_environment())
        receipt['resourceQualification'] = host_interval(start, host.host_sample(), receipt['execution']['totalOwnedCpuSeconds'])
        assert receipt['execution']['exitCode'] == 0 and receipt['execution']['ownedProcessesRemaining'] == 0
        post = execute(name+'-post-delivery', 'delivery')
        receipt['postDelivery'] = {'path':name+'-post-delivery-summary.json', 'sha256':digest(HERE/(name+'-post-delivery-summary.json'))}
        result = json.loads((HERE/'raw'/name/'run.json').read_text()); assert result['status'] == 'passed'
        assert result['results']['statistics'] is None and result['results']['terminations'] == {'normal':64}
        cost = json.loads((HERE/'raw'/name/'process-cost.json').read_text()); assert cost['status'] == 'passed'
        ratios = {a:cost['cpuSeconds'][a]/(result['results']['wholeRequestResources'][a]['elapsedNanos']/1e9) for a in ('baseline','candidate')}
        receipt['actorDelivery'] = {'nativeCpuToSearchElapsed':ratios, 'absoluteDifference':abs(ratios['baseline']-ratios['candidate']),
            'passed':all(.90 <= x <= 1.25 for x in ratios.values()) and abs(ratios['baseline']-ratios['candidate']) <= .10}
        clock = module('clock_audit', HERE/'audit_confirmation_clock_v1.py').audit(name)
        receipt['clockReliability'] = {key:clock[key] for key in ('clockReliabilityPassed','maximumRequestOverHardMillis')}
        receipt['clockAnalysis'] = {'path':name+'-clock-analysis-v1.json', 'sha256':digest(HERE/(name+'-clock-analysis-v1.json'))}
        receipt['matchWallSeconds'] = result['matchWallSeconds']; stable()
        receipt['integrity'] = {'terminations':result['results']['terminations'], 'wholeRequestResources':result['results']['wholeRequestResources'],
            'inputsStable':result['inputsStable'], 'scoreInspection':'Deferred until all four blocks complete'}
        receipt['status'] = 'passed' if receipt['resourceQualification']['passed'] and post['criteriaPassed'] and receipt['actorDelivery']['passed'] else 'resource-unqualified'
        if not clock['clockReliabilityPassed']: receipt['status'] = 'clock-unqualified'
    except BaseException as error:
        receipt.update(status='failed', failure=repr(error)); raise
    finally:
        receipt['evidence'] = {str(p.relative_to(HERE)):digest(p) for p in sorted((HERE/'raw'/name).glob('*')) if p.is_file()}
        worker_log = HERE/'raw'/(name+'-worker.log')
        if worker_log.exists(): receipt['evidence'][str(worker_log.relative_to(HERE))] = digest(worker_log)
        write_new(HERE/(name+'-summary.json'), receipt)
    print(json.dumps({'status':receipt['status'], 'block':block, 'clockReliability':receipt['clockReliability'],
        'hostOtherAverageLogicalCores':receipt['resourceQualification']['otherAverageLogicalCores'],
        'actorDelivery':receipt['actorDelivery'], 'postDeliveryPassed':post['criteriaPassed'], 'scores':'Unopened'}, indent=2))


if __name__ == '__main__': main()
