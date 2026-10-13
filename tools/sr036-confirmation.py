"""Audit a frozen SR-036 staged finalist confirmation; never control or extend games.

The pre-existing paired-normal/2.5-SE and bounded-pair sensitivity rule is retained.
Family alpha .05 is allocated as .05/2**contrastIndex, then across 128/384/1024 looks.
Independent partition-one pairs only; computational futility is not inferiority.
"""
import argparse
import importlib.util
import json
from pathlib import Path


def module(name, file):
    spec = importlib.util.spec_from_file_location(name, Path(__file__).with_name(file))
    value = importlib.util.module_from_spec(spec); spec.loader.exec_module(value); return value


b = module('baseline', 'search-baseline.py')
stats = module('paired_rule', 'sr007-confirmation.py')


def assess(protocol_path, runs):
    protocol = json.loads(Path(protocol_path).read_text(encoding='utf-8'))
    assert protocol['looks'] == list(stats.LOOKS)
    assert protocol['assessorSha256'] == b.digest(__file__)
    assert protocol['statisticalRuleSha256'] == b.digest(stats.__file__)
    assert protocol['familyAlpha'] == stats.FAMILY_ALPHA
    assert protocol['futilityPowerThreshold'] == stats.FUTILITY_POWER
    assert protocol['minimumPairsEachSideForNormal'] == stats.MIN_EACH_SIDE
    assert protocol['requiredSensitivitySigma'] == stats.SENSITIVITY_SIGMA
    assert protocol['partition'] == 1
    assert len(runs) in (1, 2, 3)
    assert [sum(s['pairs'] for s in protocol['stages'][:i+1]) for i in range(3)] == list(stats.LOOKS)
    assert all(b.digest(p) == value for p, value in protocol['frozenFiles'].items())
    seen = set(); totals = []; records = []; looks = []
    for run_path, stage in zip(runs, protocol['stages']):
        if looks: assert looks[-1]['decision'] == 'continue', 'Prior stopping decision forbids extension'
        run_path = Path(run_path); run = json.loads((run_path/'run.json').read_text())
        assert run['status'] == 'passed' and run['inputsStable'] and run['runnerExitCode'] == 0
        for key, value in protocol['expectedArguments'].items(): assert run['arguments'][key] == value, key
        assert run['arguments']['pairs'] == stage['pairs']
        assert Path(run['arguments']['openings']).resolve() == Path(stage['openings']).resolve()
        assert all(run['filesBefore'].get(p) == value for p, value in protocol['runtimeFiles'].items())
        selection = json.loads(Path(stage['selection']).read_text())
        assert selection['partition'] == 1 and selection['count'] == stage['pairs']
        keys = selection['positionKeys']; assert len(keys) == len(set(keys))
        assert not seen.intersection(keys); seen.update(keys)
        assert all(int(b.hashlib.sha256(key.encode()).hexdigest(), 16) % 2 == 1 for key in keys)
        actual_keys = []
        for prefix in b.opening_prefixes(stage['openings'], stage['pairs']):
            board = b.chess.Board()
            for move in prefix: board.push_uci(move)
            actual_keys.append(' '.join(board.fen().split()[:4]))
        assert actual_keys == keys
        audit = b.analyze(run_path/'games.pgn', stage['openings'], stage['pairs'], True,
                          run['arguments']['concurrency'] > 1)
        assert audit['terminations'] == {'normal': 2*stage['pairs']}
        for key in ('wdl', 'terminations', 'statistics', 'wholeRequestResources'): assert audit[key] == run['results'][key]
        values = [g['candidateScore'] for g in audit['games']]
        totals.extend(values[i]+values[i+1] for i in range(0,len(values),2))
        looks.append(stats.decision(totals, protocol['contrastIndex']))
        records.append({'run': str(run_path), 'runSha256': b.digest(run_path/'run.json'),
                        'pgnSha256': b.digest(run_path/'games.pgn'), 'wdl': audit['wdl']})
    return {'status':'passed', 'protocolSha256':b.digest(protocol_path), 'records':records,
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
