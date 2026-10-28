"""Read-only loss cross-check of frozen neural endpoints; no strength inference."""
import json
import math
from pathlib import Path
from baseline import ROOT, admit, sha


def sigmoid(x):
    return 1 / (1 + math.exp(-x)) if x >= 0 else math.exp(x) / (1 + math.exp(x))


def contrast(candidate, control):
    delta = [a-b for a, b in zip(candidate, control)]
    n = len(delta)
    mean = sum(delta)/n
    se = math.sqrt(sum((x-mean)**2 for x in delta)/(n-1)/n)
    return dict(mean_loss_difference=mean, paired_position_95=[mean-1.96*se, mean+1.96*se],
                relative_gain=1-sum(candidate)/sum(control))


def main():
    resource = admit(initial=True)
    folder = ROOT/'docs/research/brn-relational-cglhw'
    raw = folder/'raw/r024-prediction.json'
    receipt = json.loads(raw.with_suffix('.receipt.json').read_text())
    assert sha(raw) == receipt['sha256']
    data = json.loads(raw.read_text())
    old = folder/'raw/r016-prediction.json'
    assert sha(old) == json.loads((folder/'r016-results.json').read_text())['raw_sha256']
    previous = json.loads(old.read_text())
    assert data['teacher_targets'] == previous['teacher_targets']
    assert data['bt4_targets'] == previous['bt4_targets']
    assert data['rows'] == 8192 and len(data['pieces']) == 8192
    losses = {}; models = []
    for model in data['models']:
        assert len(model['logits']) == data['rows']
        scores = [sigmoid(x) for x in model['logits']]
        loss = {label: [(p-y)**2 for p, y in zip(scores, data[label+'_targets'])]
                for label in ('teacher', 'bt4')}
        for label in loss:
            assert abs(sum(loss[label])/data['rows']-model[label+'_mse']) < 1e-14
        losses[model['id']] = loss
        phases = {}
        for lo, hi in ((2, 10), (11, 20), (21, 32)):
            indices = [i for i, pieces in enumerate(data['pieces']) if lo <= pieces <= hi]
            phases[f'{lo}-{hi}'] = dict(rows=len(indices), **{
                label+'_mse': sum(loss[label][i] for i in indices)/len(indices)
                for label in loss})
        models.append({k: v for k, v in model.items() if k != 'logits'} | dict(phases=phases))
    comparisons = []
    for seed in (71, 97):
        for candidate, control in ((f'r022-compose-{seed}', f'r017-all-{seed}'),
                                   (f'r023-wide-{seed}', f'r022-compose-{seed}')):
            comparisons.append(dict(candidate=candidate, control=control, **{
                label: contrast(losses[candidate][label], losses[control][label])
                for label in ('teacher', 'bt4')}))
    for model in data['models']:
        if model['kind'] == 'PAIR':
            continue
        for control in ('pair24-71', 'gen32'):
            comparisons.append(dict(candidate=model['id'], control=control, **{
                label: contrast(losses[model['id']][label], losses[control][label])
                for label in ('teacher', 'bt4')}))
    local = json.loads((folder/'r022-results.json').read_text())
    support = all(g['teacher_gain'] > 0 and g['original_change'] <= .01 for g in local['gates'])
    support &= all(c['teacher']['relative_gain'] > 0 and c['bt4']['relative_gain'] >= -.01
                   for c in comparisons[:4] if c['candidate'].startswith('r022'))
    result = dict(raw_path=str(raw.relative_to(ROOT)), raw_sha256=sha(raw),
                  plan_sha256=sha(folder/'r024-plan.json'), analysis_sha256=sha(Path(__file__)),
                  resources=resource, receipt=receipt, teacher_and_bt4_row_parity=True,
                  models=models, comparisons=comparisons,
                  separate_playing_calibration_protocol_justified=bool(support),
                  scope='Consumed BRN development; paired-position intervals ignore unknown game correlation. No failed gate reversed, strength claim or automatic promotion.')
    with (folder/'r024-results.json').open('x') as stream:
        json.dump(result, stream, indent=2); stream.write('\n')
    print(json.dumps(dict(models=[{k:v for k,v in m.items() if k!='phases'} for m in models],
                          primary_comparisons=comparisons[:4], playing_protocol_justified=bool(support)), indent=2))


if __name__ == '__main__':
    main()
