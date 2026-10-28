"""R034 prespecified composition matching, without tuning bins or fitting models."""
import json
import math
from pathlib import Path
import chess
from baseline import ROOT, admit, sha
from analyze_fields import sigmoid


def cell(pieces, cp):
    assert 2 <= pieces <= 32
    phase = 0 if pieces <= 10 else 1 if pieces <= 20 else 2
    magnitude = abs(cp)
    confidence = 0 if magnitude < 200 else 1 if magnitude < 600 else 2 if magnitude < 1600 else 3
    return phase * 4 + confidence


def reconstructed_cp(probability):
    assert 0 <= probability <= 1
    if probability in (0, 1):
        return math.inf if probability else -math.inf
    cp = round(400 * (math.log(probability) - math.log1p(-probability)))
    assert abs(sigmoid(cp / 400) - probability) <= 1e-14
    return cp


def verified(folder, name):
    path = folder / ('raw/' + name + '.json')
    receipt = json.loads(path.with_suffix('.receipt.json').read_text())
    assert sha(path) == receipt['sha256']
    return json.loads(path.read_text()), dict(path=str(path.relative_to(ROOT)), sha256=sha(path), receipt=receipt)


def main():
    resource = admit(initial=True)
    folder = ROOT / 'docs/research/brn-relational-cglhw'
    assert [cell(p, cp) for p, cp in ((10,199),(10,-200),(11,599),(20,600),(21,1599),(32,1600))] == [0,1,5,6,10,11]
    old, old_ref = verified(folder, 'r029-prediction')
    played, played_ref = verified(folder, 'r034-prediction')
    original, original_ref = verified(folder, 'r026-prediction')
    for key in ('rows','teacher_targets','teacher_cp','pieces','in_check'):
        assert played[key] == original[key]
    assert old['rows'] == 8192 and played['rows'] == 991
    old_plan = json.loads((folder/'r029-plan.json').read_text())
    old_cases = ROOT/old_plan['cases']
    assert sha(old_cases) == old_plan['cases_sha256']
    boards = [chess.Board(line.split('\t')[0]) for line in old_cases.read_text().splitlines()]
    assert len(boards) == old['rows'] and not any(board.is_check() for board in boards)
    assert [len(board.piece_map()) for board in boards] == old['pieces']
    selected = [i for i, check in enumerate(played['in_check']) if not check]
    assert len(selected) == 876
    old_cp = [reconstructed_cp(p) for p in old['teacher_targets']]
    old_cells = [[i for i in range(old['rows']) if cell(old['pieces'][i],old_cp[i]) == c] for c in range(12)]
    played_cells = [[i for i in selected if cell(played['pieces'][i],played['teacher_cp'][i]) == c] for c in range(12)]
    retained = [c for c in range(12) if len(old_cells[c]) >= 20 and len(played_cells[c]) >= 10]
    old_n = sum(len(old_cells[c]) for c in retained)
    played_n = sum(len(played_cells[c]) for c in retained)
    assert old_n and played_n
    coverage = dict(old=old_n/old['rows'],played_noncheck=played_n/len(selected),old_retained=old_n,played_retained=played_n)
    labels = [f'{p}|{c}' for p in ('pieces2-10','pieces11-20','pieces21-32') for c in ('absCP0-199','absCP200-599','absCP600-1599','absCP1600+')]
    cells = [dict(id=c,label=labels[c],old_count=len(old_cells[c]),played_count=len(played_cells[c]),retained=c in retained) for c in range(12)]
    models = []
    for model in played['models']:
        previous = next(m for m in old['models'] if m['id'] == model['id'])
        assert model['kind'] == previous['kind'] == 'PIECE_ONE'
        assert model['state_sha256'] == previous['state_sha256']
        old_loss = [(sigmoid(x)-y)**2 for x,y in zip(previous['logits'],old['teacher_targets'])]
        played_loss = [(sigmoid(x)-y)**2 for x,y in zip(model['logits'],played['teacher_targets'])]
        assert len(old_loss) == old['rows'] and len(played_loss) == played['rows']
        assert abs(sum(old_loss)/old['rows']-previous['teacher_mse']) < 1e-14
        assert abs(sum(played_loss)/played['rows']-model['teacher_mse']) < 1e-14
        details = [dict(id=c,old_mse=sum(old_loss[i] for i in old_cells[c])/len(old_cells[c]) if old_cells[c] else None,
                        played_mse=sum(played_loss[i] for i in played_cells[c])/len(played_cells[c]) if played_cells[c] else None) for c in range(12)]
        old_overlap = sum(old_loss[i] for c in retained for i in old_cells[c])/old_n
        played_overlap = sum(played_loss[i] for c in retained for i in played_cells[c])/played_n
        matched_old = sum(details[c]['old_mse']*len(played_cells[c]) for c in retained)/played_n
        # Independently check weighted total and cell partition accounting.
        assert sum(len(indices) for indices in old_cells) == old['rows']
        assert sum(len(indices) for indices in played_cells) == len(selected)
        raw_old = sum(old_loss)/old['rows']
        raw_played = sum(played_loss[i] for i in selected)/len(selected)
        models.append(dict(id=model['id'],state_sha256=model['state_sha256'],cells=details,raw_old_mse=raw_old,raw_played_noncheck_mse=raw_played,
                           raw_ratio=raw_played/raw_old,old_overlap_mse=old_overlap,played_overlap_mse=played_overlap,
                           composition_matched_old_mse=matched_old,residual_ratio=played_overlap/matched_old))
    assert {m['id'] for m in models} == {'r028-one-71','r028-one-97'}
    passed = coverage['old'] >= .75 and coverage['played_noncheck'] >= .75 and all(m['residual_ratio'] >= 1.5 for m in models)
    result = dict(plan_sha256=sha(folder/'r034-plan.json'),analysis_sha256=sha(Path(__file__)),resources=resource,
                  old_evidence=old_ref,played_evidence=played_ref,original_played_evidence=original_ref,
                  target_and_metadata_parity=True,old_inverse_forward_parity=True,coverage=coverage,cells=cells,models=models,
                  search_distribution_training_experiment_justified=passed,
                  scope='Prespecified descriptive matching on consumed development;unknown game clustering;CP64 reference is not ground truth;no holdout reclassification,training,collection or strength claim.')
    with (folder/'r034-results.json').open('x') as stream:
        json.dump(result,stream,indent=2);stream.write('\n')
    print(json.dumps(dict(coverage=coverage,cells=cells,models=[{k:v for k,v in m.items() if k!='cells'} for m in models],search_distribution_training_experiment_justified=passed),indent=2))


if __name__ == '__main__':
    main()
