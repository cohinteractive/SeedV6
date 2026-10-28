"""R029 frozen all-piece neural endpoints against already frozen development references."""
import json
from pathlib import Path
from baseline import ROOT, admit, sha
from analyze_fields import sigmoid, contrast


def main():
    resource = admit(initial=True)
    folder = ROOT/'docs/research/brn-relational-cglhw'
    path = folder/'raw/r029-prediction.json'
    receipt = json.loads(path.with_suffix('.receipt.json').read_text())
    assert sha(path) == receipt['sha256']
    data = json.loads(path.read_text())
    reference = json.loads((folder/'r024-results.json').read_text())
    previous_path = ROOT/reference['raw_path']
    assert sha(previous_path) == reference['raw_sha256']
    previous = json.loads(previous_path.read_text())
    for key in ('rows','teacher_targets','bt4_targets','pieces'):
        assert data[key] == previous[key]
    losses = {}; models = []
    for m in previous['models'] + data['models']:
        loss = {label:[(sigmoid(x)-y)**2 for x,y in zip(m['logits'],data[label+'_targets'])] for label in ('teacher','bt4')}
        assert len(m['logits']) == data['rows'] == 8192
        for label in loss:
            assert abs(sum(loss[label])/data['rows']-m[label+'_mse']) < 1e-14
        losses[m['id']] = loss
        if not m['id'].startswith('r028'):
            continue
        phases = {}
        for low,high in ((2,10),(11,20),(21,32)):
            indices = [i for i,n in enumerate(data['pieces']) if low<=n<=high]
            phases[f'{low}-{high}'] = dict(rows=len(indices),**{label+'_mse':sum(loss[label][i] for i in indices)/len(indices) for label in loss})
        models.append({k:v for k,v in m.items() if k!='logits'} | dict(phases=phases))
    comparisons = []
    for seed in (71,97):
        for mode in ('one','two'):
            name = f'r028-{mode}-{seed}'
            for control in (f'r022-compose-{seed}','pair24-71','gen32'):
                comparisons.append(dict(candidate=name,control=control,**{label:contrast(losses[name][label],losses[control][label]) for label in ('teacher','bt4')}))
        comparisons.append(dict(candidate=f'r028-two-{seed}',control=f'r028-one-{seed}',**{label:contrast(losses[f'r028-two-{seed}'][label],losses[f'r028-one-{seed}'][label]) for label in ('teacher','bt4')}))
    gates = []
    training = json.loads((folder/'r028-results.json').read_text())
    old_reference = json.loads((folder/'r022-results.json').read_text())
    for seed in (71,97):
        new = next(r['result'] for r in training['records'] if r['result']['mode']=='ONE' and r['result']['seed']==seed)
        old = next(r['result'] for r in old_reference['arms'] if r['result']['seed']==seed)
        comparison = next(c for c in comparisons if c['candidate']==f'r028-one-{seed}' and c['control']==f'r022-compose-{seed}')
        gate = dict(seed=seed,old_teacher_gain=1-new['metrics'][-1]['validation_mse']/old['metrics'][-1]['validation_mse'],old_bt4_gain=1-new['original_bt4_validation_mse']/old['original_bt4_validation_mse'],larger_teacher_gain=comparison['teacher']['relative_gain'],larger_bt4_gain=comparison['bt4']['relative_gain'])
        gate['passes'] = gate['old_teacher_gain']>=.02 and gate['larger_teacher_gain']>=.02 and gate['old_bt4_gain']>=-.01 and gate['larger_bt4_gain']>=-.01
        gates.append(gate)
    result = dict(raw_path=str(path.relative_to(ROOT)),raw_sha256=sha(path),plan_sha256=sha(folder/'r029-plan.json'),analysis_sha256=sha(Path(__file__)),resources=resource,receipt=receipt,reference_raw_sha256=sha(previous_path),target_and_piece_parity=True,models=models,comparisons=comparisons,gates=gates,separate_one_pass_followup_justified=all(g['passes'] for g in gates),scope='Consumed development;unknown game clustering;no reversal of R028 second-pass failure,no strength or promotion claim.')
    with (folder/'r029-results.json').open('x') as stream:
        json.dump(result,stream,indent=2);stream.write('\n')
    print(json.dumps(dict(gates=gates,models=[{k:v for k,v in m.items() if k!='phases'} for m in models],followup=result['separate_one_pass_followup_justified']),indent=2))


if __name__ == '__main__':
    main()
