"""Descriptive teacher-score calibration, never an applied fit or strength estimate."""
import json
import math
from pathlib import Path
import chess
from baseline import ROOT, admit, sha
from analyze_fields import sigmoid


def describe(logits, teacher_cp, targets, indices):
    stable = [i for i in indices if teacher_cp[i] is not None and abs(teacher_cp[i]) <= 1600]
    strong = [i for i in stable if abs(teacher_cp[i]) >= 300]
    x = [400*logits[i] for i in stable]; y = [teacher_cp[i] for i in stable]
    def sd(values):
        mean = sum(values)/len(values)
        return math.sqrt(sum((v-mean)**2 for v in values)/len(values))
    denominator = sum(v*v for v in x)
    return dict(rows=len(indices), teacher_mse=sum((sigmoid(logits[i])-targets[i])**2 for i in indices)/len(indices) if indices else None,
                stable_rows=len(stable), bias_student_minus_teacher_cp=sum(a-b for a,b in zip(x,y))/len(x) if x else None,
                student_sd_cp=sd(x) if x else None, teacher_sd_cp=sd(y) if y else None,
                through_origin_slope=sum(a*b for a,b in zip(x,y))/denominator if denominator else None,
                strong_rows=len(strong), opposite_sign_vs_teacher=sum(logits[i]*teacher_cp[i]<0 for i in strong)/len(strong) if strong else None)


def main():
    resource = admit(initial=True)
    folder = ROOT/'docs/research/brn-relational-cglhw'
    source = folder/'raw/r026-prediction.json'
    receipt = json.loads(source.with_suffix('.receipt.json').read_text())
    assert sha(source) == receipt['sha256']
    played = json.loads(source.read_text())
    selection = json.loads((folder/'r026-selection.json').read_text())
    assert sha(ROOT/selection['cases']) == selection['cases_sha256']
    cases = json.loads((ROOT/selection['cases']).read_text())['positions']
    assert played['in_check'] == [c['in_check'] for c in cases]
    assert played['pieces'] == [c['pieces'] for c in cases]
    old_path = folder/'raw/r024-prediction.json'
    assert sha(old_path) == json.loads((folder/'r024-results.json').read_text())['raw_sha256']
    old = json.loads(old_path.read_text()); old_plan = json.loads((folder/'r024-plan.json').read_text())
    assert sha(ROOT/old_plan['cases']) == old_plan['cases_sha256']
    old['in_check'] = [chess.Board(line.split('\t')[0]).is_check() for line in (ROOT/old_plan['cases']).read_text().splitlines()]
    old['teacher_cp'] = [round(400*math.log(p/(1-p))) if 0 < p < 1 else None for p in old['teacher_targets']]
    assert len(old['in_check']) == old['rows'] == 8192
    results = {}; selected = []
    for name, data in [('development8192', old), ('played_roots', played)]:
        assert all(abs(sigmoid(cp/400)-target) < 1e-12 for cp,target in zip(data['teacher_cp'],data['teacher_targets']) if cp is not None and abs(cp)<=1600)
        rows = []
        for model in data['models']:
            assert len(model['logits']) == data['rows']
            all_rows = list(range(data['rows'])); noncheck = [i for i in all_rows if not data['in_check'][i]]
            groups = {'all': all_rows, 'noncheck': noncheck}
            for lo,hi in ((2,10),(11,20),(21,32)):
                groups[f'pieces{lo}-{hi}'] = [i for i in all_rows if lo <= data['pieces'][i] <= hi]
            summary = {group: describe(model['logits'],data['teacher_cp'],data['teacher_targets'],indices) for group,indices in groups.items()}
            assert abs(summary['all']['teacher_mse']-model['teacher_mse']) < 1e-14
            rows.append(dict(id=model['id'], state_sha256=model['state_sha256'], groups=summary))
            if model['id'].startswith('r022-compose-'):
                selected.append(dict(panel=name,id=model['id'],**summary['noncheck']))
        results[name] = dict(rows=data['rows'], noncheck_rows=sum(not x for x in data['in_check']),models=rows)
    assert len(selected) == 4
    gate = all(r['stable_rows']>=100 and r['through_origin_slope'] is not None and r['through_origin_slope']>1.25 for r in selected)
    result = dict(raw_path=str(source.relative_to(ROOT)),raw_sha256=sha(source),analysis_sha256=sha(Path(__file__)),resources=resource,
                  selection_sha256=sha(folder/'r026-selection.json'),receipt=receipt,panels=results,primary_calibration=selected,
                  training_only_calibration_experiment_justified=gate,
                  scope='Descriptive projection against CP64;no score transform fitted/applied,no truth/strength claim;played roots are not search leaves.')
    with (folder/'r026-results.json').open('x') as stream:
        json.dump(result,stream,indent=2);stream.write('\n')
    print(json.dumps(dict(primary=selected,calibration_gate=gate),indent=2))


if __name__ == '__main__':
    main()
