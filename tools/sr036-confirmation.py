"""Audit frozen SR-036 finalist confirmations; never control or extend games.

The pre-existing paired-normal/2.5-SE and bounded-pair sensitivity rule is retained.
Family alpha .05 is allocated as .05/2**contrastIndex, then across 128/384/1024 looks.
Independent partition-one pairs only; computational futility is not inferiority.
An independently declared fixed bounded-pair contrast uses its own allocated alpha.
"""
import argparse
import importlib.util
import json
import math
from pathlib import Path
import statistics


def module(name, file):
    spec = importlib.util.spec_from_file_location(name, Path(__file__).with_name(file))
    value = importlib.util.module_from_spec(spec); spec.loader.exec_module(value); return value


b = module('baseline', 'search-baseline.py')
stats = module('paired_rule', 'sr007-confirmation.py')


def fixed_bounded(pair_totals, contrast_index, planned_pairs):
    if type(contrast_index) is not int or contrast_index<1 or planned_pairs<1 or len(pair_totals)!=planned_pairs:
        raise ValueError('Requires the complete prospectively fixed contrast')
    if any(x not in (0,.5,1,1.5,2) for x in pair_totals):raise ValueError('Invalid pair total')
    alpha=stats.FAMILY_ALPHA/2**contrast_index
    mean=statistics.mean(x/2 for x in pair_totals)
    radius=math.sqrt(math.log(2/alpha)/(2*planned_pairs))
    bounds=[max(0.,mean-radius),min(1.,mean+radius)]
    return {'pairs':planned_pairs,'score':mean,'logisticElo':stats.elo(mean),
            'pentanomial':[pair_totals.count(x) for x in (0,.5,1,1.5,2)],
            'familyAlpha':stats.FAMILY_ALPHA,'contrastIndex':contrast_index,'confirmationAlpha':alpha,
            'adjustedHoeffdingScore':bounds,'adjustedHoeffdingElo':[stats.elo(x) for x in bounds],
            'decision':'positive' if bounds[0]>.5 else 'negative' if bounds[1]<.5 else 'inconclusive',
            'method':'One prospectively fixed paired sample; family alpha .05/2^contrastIndex; two-sided bounded-pair Hoeffding is primary. Assumes independent bounded opening-pair observations. No normal approximation, optional looks, exploratory pooling or post-close extension.'}


def assess(protocol_path, runs):
    protocol = json.loads(Path(protocol_path).read_text(encoding='utf-8'))
    staged=protocol['design']=='staged_normal'
    assert staged or protocol['design']=='fixed_bounded'
    planned=protocol['looks']
    if staged:
        assert planned == list(stats.LOOKS)
        assert protocol['futilityPowerThreshold'] == stats.FUTILITY_POWER
        assert protocol['minimumPairsEachSideForNormal'] == stats.MIN_EACH_SIDE
        assert protocol['requiredSensitivitySigma'] == stats.SENSITIVITY_SIGMA
    else:assert len(planned)==1 and planned[0]>0
    assert protocol['assessorSha256'] == b.digest(__file__)
    assert protocol['statisticalRuleSha256'] == b.digest(stats.__file__)
    assert protocol['familyAlpha'] == stats.FAMILY_ALPHA
    assert protocol['partition'] == 1
    assert len(protocol['stages']) == len(planned)
    assert 1<=len(runs)<=len(planned)
    assert [sum(s['pairs'] for s in protocol['stages'][:i+1]) for i in range(len(planned))] == planned
    assert all(b.digest(p) == value for p, value in protocol['frozenFiles'].items())
    excluded = set()
    for path in protocol['excludedSelections']:
        assert str(Path(path).resolve()) in protocol['frozenFiles']
        excluded.update(json.loads(Path(path).read_text())['positionKeys'])
    seen = set(); totals = []; records = []; looks = []
    for run_path, stage in zip(runs, protocol['stages']):
        if looks: assert looks[-1]['decision'] == 'continue', 'Prior stopping decision forbids extension'
        run_path = Path(run_path); run = json.loads((run_path/'run.json').read_text())
        assert run['status'] == 'passed' and run['inputsStable'] and run['runnerExitCode'] == 0
        assert Path(run['arguments']['out']).resolve() == run_path.resolve()
        for key, value in protocol['expectedArguments'].items(): assert run['arguments'][key] == value, key
        assert run['arguments']['pairs'] == stage['pairs']
        assert Path(run['arguments']['openings']).resolve() == Path(stage['openings']).resolve()
        assert all(run['filesBefore'].get(p) == value for p, value in protocol['runtimeFiles'].items())
        for side, expected in protocol['expectedPreflightFields'].items():
            fields = dict(x.split('=',1) for x in run[side+'Preflight']['identity'].split() if '=' in x)
            assert all(fields.get(k) == v for k,v in expected.items()), side
        selection = json.loads(Path(stage['selection']).read_text())
        assert selection['partition'] == 1 and selection['count'] == stage['pairs']
        keys = selection['positionKeys']; assert len(keys) == len(set(keys))
        assert not excluded.intersection(keys)
        assert not seen.intersection(keys); seen.update(keys)
        assert all(int(b.hashlib.sha256(key.encode()).hexdigest(), 16) % 2 == 1 for key in keys)
        actual_keys = []
        for prefix in b.opening_prefixes(stage['openings'], stage['pairs']):
            board = b.chess.Board()
            for move in prefix: board.push_uci(move)
            actual_keys.append(' '.join(board.fen().split()[:4]))
        assert actual_keys == keys
        assert b.digest(stage['openings']) == stage['sha256'] == run['filesBefore'][str(Path(stage['openings']).resolve())]
        audit = b.analyze(run_path/'games.pgn', stage['openings'], stage['pairs'], True,
                          run['arguments']['concurrency'] > 1)
        assert audit['terminations'] == {'normal': 2*stage['pairs']}
        for key in ('wdl', 'terminations', 'statistics', 'wholeRequestResources'): assert audit[key] == run['results'][key]
        values = [g['candidateScore'] for g in audit['games']]
        totals.extend(values[i]+values[i+1] for i in range(0,len(values),2))
        looks.append(stats.decision(totals, protocol['contrastIndex']) if staged
                     else fixed_bounded(totals,protocol['contrastIndex'],planned[0]))
        records.append({'run': str(run_path), 'runSha256': b.digest(run_path/'run.json'),
                        'pgnSha256': b.digest(run_path/'games.pgn'), 'wdl': audit['wdl']})
    assert all(b.digest(p) == value for p, value in protocol['frozenFiles'].items())
    return {'status':'passed', 'protocolSha256':b.digest(protocol_path), 'inputsStable':True, 'records':records,
            'looks':looks, 'pairTotals':totals, 'finalDecision':looks[-1]['decision']}


if __name__ == '__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--protocol',required=True);p.add_argument('--run',action='append',required=True);p.add_argument('--out',required=True)
    a=p.parse_args(); output=Path(a.out)
    if output.exists(): raise ValueError('Never overwrite a closed assessment')
    report={'status':'failed'}
    try:
        report=assess(a.protocol,a.run);print(json.dumps(report['looks'][-1],indent=2))
    except BaseException as error:
        report['failure']=repr(error);raise
    finally:
        output.write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8',newline='\n')
