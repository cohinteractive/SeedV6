"""Independently audit frozen SR-024 paired confirmations, including worker domains.

The protocol chooses looks from measured information value before outcomes.
Family alpha .05 is spent as .05/2**contrastIndex and then across planned looks.
Approximate paired-normal inference requires 20 pairs on either side of half;
otherwise only bounded-pair inference applies. Nonbinding futility is not inferiority.
"""
import argparse
import importlib.util
import json
import math
from pathlib import Path
import statistics

FAMILY_ALPHA=.05
SENSITIVITY_SIGMA=2.5
MIN_EACH_SIDE=20
FUTILITY_POWER=.10
spec=importlib.util.spec_from_file_location('baseline',Path(__file__).with_name('search-baseline.py'))
b=importlib.util.module_from_spec(spec);spec.loader.exec_module(b)

def elo(score):
    if score <= 0: return '-infinity'
    if score >= 1: return '+infinity'
    return 400 * math.log10(score / (1 - score))


def interval(mean, radius):
    return [max(0., mean-radius), min(1., mean+radius)]


def decision(pair_totals, candidate_index, looks):
    if not looks or list(looks) != sorted(set(looks)) or any(type(n) is not int or n < 2 for n in looks):
        raise ValueError("Invalid prospective looks")
    if type(candidate_index) is not int or candidate_index < 1:
        raise ValueError("Confirmation index must be a prespecified positive integer")
    confirmation_alpha = FAMILY_ALPHA / (2 ** candidate_index)
    n = len(pair_totals)
    if n not in looks or any(x not in (0, .5, 1, 1.5, 2) for x in pair_totals):
        raise ValueError('Only complete predeclared paired looks are admissible')
    values = [x/2 for x in pair_totals]
    mean = statistics.mean(values)
    variance = statistics.variance(values)
    se = math.sqrt(variance/n) if variance else None
    below = sum(x < .5 for x in values); above = sum(x > .5 for x in values)
    normal_eligible = se is not None and min(below,above) >= MIN_EACH_SIDE
    per_look_alpha = confirmation_alpha/len(looks)
    if per_look_alpha <= 0 or 1-per_look_alpha/2 == 1:
        raise ValueError("Allocated alpha is below supported floating-point precision")
    z = statistics.NormalDist().inv_cdf(1-per_look_alpha/2)
    primary = interval(mean,z*se) if normal_eligible else None
    sensitivity = interval(mean,SENSITIVITY_SIGMA*se) if normal_eligible else None
    bounded = interval(mean,math.sqrt(math.log(2/per_look_alpha)/(2*n)))
    if primary is None:
        positive, negative = bounded[0] > .5, bounded[1] < .5
    else:
        positive = primary[0] > .5 and sensitivity[0] > .5
        negative = primary[1] < .5 and sensitivity[1] < .5
    # Nonbinding computational futility: stops can only prevent adoption. Treat
    # the observed mean/variance as plug-in forecasts, not evidence of no effect.
    final_n = looks[-1]
    power = None
    if normal_eligible and n < final_n:
        final_radius = max(z, SENSITIVITY_SIGMA) * math.sqrt(variance/final_n)
        remaining_se = math.sqrt(variance*(final_n-n))/final_n
        power = 1-statistics.NormalDist().cdf((.5+final_radius-mean)/remaining_se)
    state = ('positive' if positive else 'negative' if negative else
             'inconclusive' if n == final_n else
             'futility' if power is not None and power < FUTILITY_POWER else 'continue')
    return {'pairs': n, 'score': mean, 'logisticElo': elo(mean),
            'pentanomial': [pair_totals.count(x) for x in (0,.5,1,1.5,2)],
            'pairSampleVariance': variance, 'pairScoreStandardError': se,
            'normalEligible': normal_eligible, 'pairsBelowHalf': below, 'pairsAboveHalf': above,
            'minimumPairsEachSideForNormal': MIN_EACH_SIDE,
            'familyAlpha': FAMILY_ALPHA, 'candidateIndex': candidate_index, 'confirmationAlpha': confirmation_alpha, 'plannedLooks': list(looks),
            'perLookAlpha': per_look_alpha, 'normalCriticalValue': z,
            'primaryAdjustedScore': primary,
            'primaryAdjustedElo': [elo(x) for x in primary] if primary else None,
            'required2p5SeScore': sensitivity,
            'required2p5SeElo': [elo(x) for x in sensitivity] if sensitivity else None,
            'adjustedHoeffdingScore': bounded, 'adjustedHoeffdingElo': [elo(x) for x in bounded],
            'decision': state, 'plugInConditionalPowerAtMaximum': power, 'futilityPowerThreshold': FUTILITY_POWER,
            'method': 'Prospectively specified cumulative looks per fresh confirmation; family alpha .05 spent as .05/2^candidateIndex, then Bonferroni across looks; paired-normal intervals and required 2.5-SE sensitivity. Approximate inference; adjusted bounded-pair Hoeffding sensitivity. Degenerate or sparse two-sided outcomes use only the bounded interval. No exploratory pooling. Before the maximum look, normal-eligible plug-in conditional power below .10 stops for computational futility, not inferiority or a minimum Elo threshold.'}


def assess(protocol_path, runs):
    protocol = json.loads(Path(protocol_path).read_text(encoding='utf-8'))
    assert protocol['design']=='staged_normal'
    planned=protocol['looks']
    assert protocol['futilityPowerThreshold']==FUTILITY_POWER
    assert protocol['minimumPairsEachSideForNormal']==MIN_EACH_SIDE
    assert protocol['requiredSensitivitySigma']==SENSITIVITY_SIGMA
    assert protocol['assessorSha256'] == b.digest(__file__)
    assert protocol['familyAlpha'] == FAMILY_ALPHA
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
        assert run['arguments']['baseline_workers']==run['arguments']['candidate_workers']
        cpu=json.loads((run_path/'process-cost.json').read_text(encoding='utf-8'))
        assert cpu['status']=='passed'
        assert len(cpu['processes'])==2*run['arguments']['concurrency']
        assert all(p['lifetime']['exitCode']==0 for p in cpu['processes'])
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
        looks.append(decision(totals, protocol['contrastIndex'], planned))
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
