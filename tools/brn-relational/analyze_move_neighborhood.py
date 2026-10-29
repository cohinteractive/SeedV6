"""R049 paired local errors, frozen models, no fitting or strength inference."""
import json
import math
from pathlib import Path
import time
import chess
from baseline import ROOT, admit, sha
from analyze_fields import sigmoid

FOLDER = ROOT / 'docs/research/brn-relational-cglhw'


def mean(values):
    return sum(values) / len(values) if values else None


def paired_interval(values):
    n = len(values)
    if n < 2: return None
    center = mean(values)
    standard_error = math.sqrt(sum((v-center)**2 for v in values) / (n*(n-1)))
    return [center-1.96*standard_error, center+1.96*standard_error]


def metrics(records):
    if not records: return dict(pairs=0)
    parent = [r['parent_loss'] for r in records]
    child = [r['child_loss'] for r in records]
    substantial = [r for r in records if abs(r['teacher_delta_cp']) >= 100]
    return dict(pairs=len(records), parent_mse=mean(parent), child_mse=mean(child),
                child_parent_mse_ratio=mean(child)/mean(parent),
                paired_mse_increase=mean([c-p for p,c in zip(parent,child)]),
                paired_mse_increase_approximate_95=paired_interval([c-p for p,c in zip(parent,child)]),
                parent_cp_mae=mean([r['parent_cp_error'] for r in records]),
                child_cp_mae=mean([r['child_cp_error'] for r in records]),
                delta_cp_rmse=math.sqrt(mean([r['delta_cp_error']**2 for r in records])),
                delta_probability_rmse=math.sqrt(mean([r['delta_probability_error']**2 for r in records])),
                substantial_teacher_deltas=len(substantial),
                substantial_sign_disagreement=mean([float(r['teacher_delta_cp']*r['model_delta_cp']<=0) for r in substantial]))


def main():
    started = time.monotonic(); resources = [admit(initial=True)]; sampled = started

    def check():
        nonlocal sampled
        if time.monotonic()-sampled >= 5:
            resources.append(admit()); sampled = time.monotonic()
        if time.monotonic()-started > 60: raise TimeoutError('Bounded analysis allocation exhausted')

    selection_path = FOLDER / 'r049-selection.json'
    selection = json.loads(selection_path.read_text())
    for name, digest in selection['files'].items():
        assert sha(ROOT/name) == digest, name
    pairs = json.loads((FOLDER/'raw/r049-data/selection.json').read_text())['pairs']
    assert len(pairs) == 4096 and len({p['parent_alias'] for p in pairs}) == len(pairs)
    assert len({p['child_alias'] for p in pairs}) == len(pairs)
    models = {}; evidence = []
    for shard, name in enumerate(selection['plans']):
        planpath = ROOT/name; plan = json.loads(planpath.read_text())
        out = ROOT/plan['output']; receipt_path = out.with_suffix('.receipt.json')
        bound = json.loads(out.with_suffix('.binding.json').read_text())
        assert bound['plan_sha256'] == sha(planpath)
        assert bound['runner_sha256'] == sha(ROOT/'tools/brn-relational/run_message_audit.py')
        for jar, digest in bound['runtime'].items(): assert sha(ROOT/plan['runtime']/jar) == digest
        receipt = json.loads(receipt_path.read_text()); assert sha(out) == receipt['sha256']
        data = json.loads(out.read_text()); assert data['config_sha256'] == sha(planpath)
        assert data['rows'] == 1024 and not any(data['in_check'])
        cases = json.loads((ROOT/plan['cases']).read_text())['positions']
        assert sha(ROOT/plan['cases']) == plan['cases_sha256']
        for spec in plan['models']: assert sha(ROOT/spec['state']) == spec['state_sha256']
        evidence.append(dict(plan=name, plan_sha256=sha(planpath), output=plan['output'],
                             output_sha256=sha(out), receipt_sha256=sha(receipt_path), receipt=receipt))
        for model in data['models']:
            spec = next(s for s in plan['models'] if s['id'] == model['id'])
            assert model['state_sha256'] == spec['state_sha256'] and model['kind'] == spec['kind']
            records = models.setdefault(model['id'], [])
            loss = [(sigmoid(x)-t)**2 for x,t in zip(model['logits'], data['teacher_targets'])]
            assert abs(mean(loss)-model['teacher_mse']) < 1e-13
            for offset in range(512):
                i = offset*2; pair = pairs[shard*512+offset]
                parent = chess.Board(pair['fen']); move = chess.Move.from_uci(pair['move'])
                assert parent.is_legal(move)
                parent.push(move); assert parent.fen() == pair['child_fen']
                assert cases[i]['fen'] == pair['fen'] and cases[i+1]['fen'] == pair['child_fen']
                assert abs(data['teacher_targets'][i] - pair['teacher_target']) < 1e-14
                tp,tc = data['teacher_cp'][i:i+2]; mp,mc = model['logits'][i:i+2]
                assert all(math.isfinite(v) for v in (tp,tc,mp,mc))
                td = -tc-tp; md = (-mc-mp)*400
                probability_error = (1-sigmoid(mc)-sigmoid(mp)) - (1-sigmoid(tc/400)-sigmoid(tp/400))
                records.append(dict(parent_loss=loss[i], child_loss=loss[i+1],
                                    parent_cp_error=abs(mp*400-tp), child_cp_error=abs(mc*400-tc),
                                    teacher_delta_cp=td, model_delta_cp=md, delta_cp_error=md-td,
                                    delta_probability_error=probability_error,
                                    restricted=max(abs(tp),abs(tc))<=800, pieces=pair['pieces'], capture=pair['capture']))
        check()
    summaries = {}
    for name, records in models.items():
        summaries[name] = dict(all=metrics(records),
                              restricted=metrics([r for r in records if r['restricted']]),
                              quiet=metrics([r for r in records if not r['capture']]),
                              captures=metrics([r for r in records if r['capture']]),
                              phases={f'{lo}-{hi}':metrics([r for r in records if lo<=r['pieces']<=hi])
                                      for lo,hi in ((2,10),(11,20),(21,32))})
    gates = []; comparisons = []
    for seed in (71,97):
        mixed = models[f'r045-mix-{seed}']; control = models[f'r044-low-{seed}']
        restricted = summaries[f'r045-mix-{seed}']['restricted']
        gate = dict(seed=seed, restricted_pairs=restricted['pairs'],
                    child_parent_mse_ratio=restricted['child_parent_mse_ratio'],
                    paired_increase_lower95=restricted['paired_mse_increase_approximate_95'][0])
        gate['passes'] = (selection['pairs']==4096 and selection['captures']>=128 and
                          gate['restricted_pairs']>=1024 and gate['child_parent_mse_ratio']>=1.5 and
                          gate['paired_increase_lower95']>0)
        gates.append(gate)
        differences = [m['delta_cp_error']**2-c['delta_cp_error']**2 for m,c in zip(mixed,control)]
        comparisons.append(dict(seed=seed, mix_minus_control_delta_cp_mse=mean(differences),
                                approximate_95=paired_interval(differences)))
    result = dict(selection_sha256=sha(selection_path), analysis_sha256=sha(Path(__file__)),
                  protocol_sha256=sha(FOLDER/'R049.md'), evidence=evidence, models=summaries,
                  matched_comparisons=comparisons, gates=gates,
                  local_data_pilot_justified=all(g['passes'] for g in gates),
                  parent_target_parity=True, legal_move_replay=True,
                  seconds=time.monotonic()-started, resources=resources,
                  limitations='Known fitted parents and random noncheck legal children,not iid games. Approximate parent-level intervals ignore unknown source-game correlations. Teacher is not ground truth;no strength or final-panel claim.')
    with (FOLDER/'r049-results.json').open('x') as stream:
        json.dump(result,stream,indent=2);stream.write('\n')
    print(json.dumps(dict(gates=gates, matched_comparisons=comparisons,
                         local_data_pilot_justified=result['local_data_pilot_justified'],
                         models={name:row['all'] for name,row in summaries.items()}),indent=2))


if __name__ == '__main__':
    main()
