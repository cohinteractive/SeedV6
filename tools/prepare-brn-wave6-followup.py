"""Admit fixed G6 cost and compact-degree controls after completed primary screens.

No games, model writes, training, or modification of completed protocols.
"""
import copy
import hashlib
import json
from pathlib import Path

BASE = Path('docs/research/brn-successor-cglhw')


def load(path):
    return json.loads(path.read_text(encoding='utf-8-sig'))


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def save(path, value):
    with path.open('x', encoding='utf-8', newline='\n') as stream:
        json.dump(value, stream, indent=2)
        stream.write('\n')


def main():
    evidence = {}
    for label in ['degree-node', 'degree-clock', 'compact-node', 'compact-clock', 'bre-clock']:
        path = BASE / f'wave6-{label}-results.json'
        result = load(path)
        assert result['status'] == 'Closed fixed-budget development experiment'
        evidence[str(path)] = digest(path)
    source = BASE / 'wave6-bre-clock-plan.json'
    bre = load(source)
    assert len(bre['models']) == 4 and bre['pairs'] == 8 and bre['moveMillis'] == 200
    node = copy.deepcopy(bre)
    node.update(id='wave6-bre-node-v1', nodes=262144, moveMillis=0)
    c4 = copy.deepcopy(bre['models'][3])
    assert c4['name'] == 'W6-RESCUBIC-R4-LR001-g128-half'
    quad = copy.deepcopy(c4)
    quad['name'] += '-unadapted-quadratic'
    quad['correctionAblation'] = 'quadratic'
    protocols = []
    plans = [('bre-node', node)]
    for mode, nodes, millis in [('node', 262144, 0), ('clock', 0, 200)]:
        plan = copy.deepcopy(bre)
        plan.update(id=f'wave6-compact-degree-{mode}-v1', models=[quad, c4], nodes=nodes, moveMillis=millis)
        plans.append((f'compact-degree-{mode}', plan))
    for label, plan in plans:
        path = BASE / f'wave6-{label}-plan.json'
        save(path, plan)
        protocols.append(dict(label=label, plan=str(path), sha256=digest(path),
                              games=2 * plan['pairs'] * (len(plan['models']) - 1)))
    probe = dict(id='wave6-compact-degree-probe-v1', models=[
        load(BASE / 'wave6-trained-model-plan.json')['models'][0], quad])
    probe_path = BASE / 'wave6-compact-degree-model-plan.json'
    save(probe_path, probe)
    save(BASE / 'wave6-followup-admission.json', dict(
        id='wave6-cost-and-compact-degree-followup-v1', evidenceSha256=evidence,
        sourcePlan=str(source), sourcePlanSha256=digest(source),
        protocols=protocols, totalGames=80, runtime='wave6-runtime.json',
        probePlan=str(probe_path), probePlanSha256=digest(probe_path),
        prerequisites='Finish primary reference-clock64; pass actual masked-C4 cache/fresh-score/search oracle before ablation games. No competing workloads.',
        questions=[
            'At262144 equal nodes on the same roots, does residual quality versus continued BRE differ from the completed200ms screen? All three arms retained, no point-score winner selected.',
            'Does current C4 performance depend on its third-degree heads when retained weights and cubic-cache implementation stay fixed? C16 already has a trained degree control; compact capacity may change the role of degree3.'],
        selection='Latest128 and half learned change remain fixed. C4 selected for the compact-degree diagnostic from its measured cost advantage and completed compact tradeoff, not frozen-reference outcomes.',
        interpretation='Unadapted C4 degree ablation tests current functional contribution, not optimal quadratic architecture or equal adaptation. Any training gate requires the combined evidence and explicit expected value.',
        stopping='Complete fixed48+16+16 games unless correctness/resource failure; no significance-dependent extension. Original208-game admission remains separately closed and immutable.',
        statistics='Development only, eight reused reversed-colour pairs each; descriptive and bounded pair intervals. No independent confirmation, causal throughput decomposition, or pooled score.',
        resources='One game, one search worker, no pondering,64MiB TT,768MiB heap,ActiveProcessorCount1,BelowNormal;12games/240sec batches.'))
    print('Admitted80 follow-up games, gated on primary completion and actual ablation parity')


if __name__ == '__main__':
    main()
