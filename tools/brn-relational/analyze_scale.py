"""Evaluate training-fitted neural scales on frozen development and played panels."""
import json
from pathlib import Path
from baseline import ROOT, admit, sha
from analyze_fields import sigmoid


def mse(logits, targets, scale, indices):
    return sum((sigmoid(scale*logits[i])-targets[i])**2 for i in indices)/len(indices)


def main():
    resource = admit(initial=True)
    folder = ROOT/'docs/research/brn-relational-cglhw'
    path = folder/'raw/r027-scale-fit.json'; receipt = json.loads(path.with_suffix('.receipt.json').read_text())
    assert sha(path) == receipt['sha256']
    fit = json.loads(path.read_text())
    panels = {}
    for number in (24,26):
        result = json.loads((folder/f'r{number:03}-results.json').read_text())
        raw = ROOT/result['raw_path']; assert sha(raw) == result['raw_sha256']
        panels[number] = json.loads(raw.read_text())
    models = []; gates = []
    for model in fit['models']:
        scale = model['scale']; assert .5 <= scale <= 2
        measured = {}
        for number,data in panels.items():
            source = next(m for m in data['models'] if m['id']==model['id'])
            assert source['state_sha256'] == model['state_sha256']
            indices = list(range(data['rows'])) if number==24 else [i for i in range(data['rows']) if not data['in_check'][i]]
            targets = ('teacher','bt4') if number==24 else ('teacher',)
            for target in targets:
                base = mse(source['logits'],data[target+'_targets'],1,indices)
                calibrated = mse(source['logits'],data[target+'_targets'],scale,indices)
                measured[f'r{number:03}_{target}'] = dict(baseline=base,calibrated=calibrated,gain=1-calibrated/base,rows=len(indices))
        gate = dict(id=model['id'],old_teacher_gain=1-model['calibrated_validation_mse']/model['baseline_validation_mse'],
                    old_bt4_gain=1-model['calibrated_bt4_mse']/model['baseline_bt4_mse'],
                    larger_teacher_gain=measured['r024_teacher']['gain'],larger_bt4_gain=measured['r024_bt4']['gain'],
                    played_noncheck_teacher_gain=measured['r026_teacher']['gain'])
        gate['passes'] = gate['old_teacher_gain']>=.02 and gate['larger_teacher_gain']>=.02 and gate['old_bt4_gain']>=-.01 and gate['larger_bt4_gain']>=-.01 and gate['played_noncheck_teacher_gain']>=0
        gates.append(gate);models.append(dict(fit=model,panels=measured))
    result = dict(raw_path=str(path.relative_to(ROOT)),raw_sha256=sha(path),plan_sha256=sha(folder/'r027-plan.json'),
                  analysis_sha256=sha(Path(__file__)),resources=resource,receipt=receipt,models=models,gates=gates,
                  advance=all(g['passes'] for g in gates),scope='Scalar fitted only on training;diagnostic panels consumed;no weights changed,no automatic games or promotion;positive scaling cannot fix sign errors.')
    with (folder/'r027-results.json').open('x') as stream:
        json.dump(result,stream,indent=2);stream.write('\n')
    print(json.dumps(dict(models=models,gates=gates,advance=result['advance']),indent=2))


if __name__ == '__main__':
    main()
