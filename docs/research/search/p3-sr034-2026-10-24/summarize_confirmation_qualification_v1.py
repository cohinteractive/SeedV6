"""Close the clock-unqualified study without a strength look or gate bypass."""
import json
from pathlib import Path
from run_trajectory_v1 import HERE, ROOT, digest, write_new


def main():
    binding = json.loads((HERE/'confirmation-v1-binding.json').read_text())
    for p, sha in binding['files'].items(): assert digest(ROOT/p) == sha, p
    for p, sha in binding['externalFiles'].items(): assert digest(Path(p)) == sha, p
    receipts = []; clocks = []; evidence = {}
    for block in range(1,5):
        name = f'sr034-confirmation-v1-b{block:02}'; path = HERE/(name+'-summary.json')
        r = json.loads(path.read_text()); assert r['status'] == ('passed' if block < 4 else 'clock-unqualified')
        assert r['bindingSha256'] == digest(HERE/'confirmation-v1-binding.json')
        for p, sha in r['evidence'].items(): assert digest(HERE/p) == sha, p
        for field in ('preDelivery','postDelivery','clockAnalysis'):
            ref = r[field]; assert digest(HERE/ref['path']) == ref['sha256'], field
        assert r['integrity']['terminations'] == {'normal':64} and r['execution']['ownedProcessesRemaining'] == 0
        assert r['resourceQualification']['passed'] and r['actorDelivery']['passed']
        assert json.loads((HERE/r['postDelivery']['path']).read_text())['criteriaPassed']
        c = json.loads((HERE/r['clockAnalysis']['path']).read_text()); assert 'strength' not in c
        receipts.append(r); clocks.append(c); evidence[path.name] = digest(path)
    tail = json.loads((HERE/'confirmation-clock-tail-v1.json').read_text())
    assert tail['requestsChecked'] == sum(c['integrity']['clockConservation'] for c in clocks) == 34032
    actors = {}
    for actor in ('baseline','candidate'):
        rows = [c['actors'][actor] for c in clocks]
        request = {key:sum(r['integrity']['wholeRequestResources'][actor][key] for r in receipts) for key in ('requests','nodes','elapsedNanos')}
        assert request['requests'] == tail['actors'][actor]['requests']
        assert request['nodes'] == sum(x['wholeNodes'] for x in rows)
        assert request['elapsedNanos'] == sum(x['wholeRequestElapsedNanos'] for x in rows)
        actors[actor] = request | tail['actors'][actor] | {
            'nativeCpuSeconds':sum(c['actorNativeCpuSeconds'][actor] for c in clocks),
            'maximumEnginePeakWorkingSetBytes':max(c['peakWorkingSetBytes'][actor] for c in clocks),
            'incompleteNodes':sum(x['incompleteNodes'] for x in rows),
            'nonfactualSoftStops':sum(x['nonfactualSoftStops'] for x in rows),'byMoveNumber':[]}
        for i in range(4):
            segments = [x['byMoveNumber'][i] for x in rows]; n = sum(x['requests'] for x in segments)
            actors[actor]['byMoveNumber'].append({'range':segments[0]['range'],'requests':n,
                'meanRequestMillis':sum(x['requestMillis'].get('mean',0)*x['requests'] for x in segments)/n,
                'meanRemainingMillis':sum(x['remainingMillis'].get('mean',0)*x['requests'] for x in segments)/n})
    evidence['confirmation-clock-tail-v1.json'] = digest(HERE/'confirmation-clock-tail-v1.json')
    record = {'status':'clock-unqualified','disposition':'inconclusive_no_qualified_strength_result','candidate':'boundary2',
        'gamesCompleted':256,'pairsCompleted':128,'qualifiedBlocks':[1,2,3],'clockUnqualifiedBlocks':[4],
        'scores':'Unopened; the prescribed final strength analysis requires all four blocks to qualify and was not invoked.',
        'clockGateMillis':25,'adverseRequestCount':len(tail['adverseRequests']),'adverseActors':sorted({x['actor'] for x in tail['adverseRequests']}),
        'allResourceGatesPassed':True,'allNormalEndings':True,'allOwnedDescendantsDrained':True,
        'integrity':{k:sum(c['integrity'][k] for c in clocks) for k in ('accountedRequests','clockConservation')},
        'actors':actors,'matchWallSeconds':sum(r['matchWallSeconds'] for r in receipts),
        'ownedCpuSeconds':sum(r['execution']['totalOwnedCpuSeconds'] for r in receipts),
        'blockOtherAverageLogicalCores':[r['resourceQualification']['otherAverageLogicalCores'] for r in receipts],
        'bindingSha256':digest(HERE/'confirmation-v1-binding.json'),'evidence':evidence,'scriptSha256':digest(Path(__file__)),
        'limits':['Operational resource observations across the entire clock-unqualified study; no strength estimate, positive claim or component inference.',
            'Two actual baseline timing tails are corroborated by runner charges. Request-level CPU/GC/scheduler traces were unavailable; exact cause is unresolved.',
            'Candidate maximum overrun2.326ms does not waive the comparison gate. Candidate minimum reserve50ms and765 responses below500ms remain limitations.',
            'No replay, exclusion, substitution, relaxed gate, retuning or fallback finalist. The frozen v1 study is closed; any later confirmation needs a separate prospective plan and resolved timing-qualification approach.',
            'No production adoption, multiworker strength claim, deployment or successor launch.']}
    write_new(HERE/'confirmation-v1-disposition.json', record)
    print(json.dumps({k:v for k,v in record.items() if k not in ('evidence','scriptSha256')},indent=2))


if __name__ == '__main__': main()
