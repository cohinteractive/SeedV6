"""Audit the prospectively frozen, three-look SR-030 finalist confirmation.

No runner control or engine selection. Every supplied stage must be complete,
audited and disjoint; a qualifying earlier decision forbids adding later stages.
"""
import argparse
import hashlib
import importlib.util
import json
import math
from pathlib import Path
import statistics

LOOKS = (128, 384, 1024)
FAMILY_ALPHA = .05
SENSITIVITY_SIGMA = 2.5
MIN_EACH_SIDE = 20


def digest(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def elo(score):
    if score <= 0: return '-infinity'
    if score >= 1: return '+infinity'
    return 400 * math.log10(score / (1 - score))


def interval(mean, radius):
    return [max(0., mean-radius), min(1., mean+radius)]


def decision(pair_totals):
    n = len(pair_totals)
    if n not in LOOKS or any(x not in (0, .5, 1, 1.5, 2) for x in pair_totals):
        raise ValueError('Only complete predeclared paired looks are admissible')
    values = [x/2 for x in pair_totals]
    mean = statistics.mean(values)
    variance = statistics.variance(values)
    se = math.sqrt(variance/n) if variance else None
    below = sum(x < .5 for x in values); above = sum(x > .5 for x in values)
    normal_eligible = se is not None and min(below,above) >= MIN_EACH_SIDE
    per_look_alpha = FAMILY_ALPHA/len(LOOKS)
    z = statistics.NormalDist().inv_cdf(1-per_look_alpha/2)
    primary = interval(mean,z*se) if normal_eligible else None
    sensitivity = interval(mean,SENSITIVITY_SIGMA*se) if normal_eligible else None
    bounded = interval(mean,math.sqrt(math.log(2/per_look_alpha)/(2*n)))
    if primary is None:
        positive, negative = bounded[0] > .5, bounded[1] < .5
    else:
        positive = primary[0] > .5 and sensitivity[0] > .5
        negative = primary[1] < .5 and sensitivity[1] < .5
    state = 'positive' if positive else 'negative' if negative else 'inconclusive' if n == LOOKS[-1] else 'continue'
    return {'pairs': n, 'score': mean, 'logisticElo': elo(mean),
            'pentanomial': [pair_totals.count(x) for x in (0,.5,1,1.5,2)],
            'pairSampleVariance': variance, 'pairScoreStandardError': se,
            'normalEligible': normal_eligible, 'pairsBelowHalf': below, 'pairsAboveHalf': above,
            'minimumPairsEachSideForNormal': MIN_EACH_SIDE,
            'familyAlpha': FAMILY_ALPHA, 'plannedLooks': list(LOOKS),
            'perLookAlpha': per_look_alpha, 'normalCriticalValue': z,
            'primaryAdjustedScore': primary,
            'primaryAdjustedElo': [elo(x) for x in primary] if primary else None,
            'required2p5SeScore': sensitivity,
            'required2p5SeElo': [elo(x) for x in sensitivity] if sensitivity else None,
            'adjustedHoeffdingScore': bounded, 'adjustedHoeffdingElo': [elo(x) for x in bounded],
            'decision': state,
            'method': 'Three prespecified cumulative looks; Bonferroni-adjusted paired-normal intervals, required 2.5-SE sensitivity. Approximate inference; adjusted bounded-pair Hoeffding sensitivity. Degenerate or sparse two-sided outcomes use only the bounded interval. No exploratory pooling.'}


def assess(protocol_path, runs):
    protocol = json.loads(Path(protocol_path).read_text(encoding='utf-8'))
    assert protocol['looks'] == list(LOOKS)
    assert protocol['familyAlpha'] == FAMILY_ALPHA
    assert protocol['requiredSensitivitySigma'] == SENSITIVITY_SIGMA
    assert protocol['minimumPairsEachSideForNormal'] == MIN_EACH_SIDE
    assert protocol['assessorSha256'] == digest(__file__)
    assert protocol['partition'] == 1
    assert protocol['expectedArguments']['baseline_check'] == 'off'
    assert protocol['expectedArguments']['candidate_check'] in ('boundary','giving','evasion','forced','leaf','repeat')
    assert all(protocol['expectedArguments'][side+'_lmp'] == 'default' for side in ('baseline','candidate'))
    assert len(protocol['stages']) == len(LOOKS)
    assert [sum(s['pairs'] for s in protocol['stages'][:i+1]) for i in range(3)] == list(LOOKS)
    assert 1 <= len(runs) <= len(LOOKS)
    frozen = protocol['frozenFiles']
    all_frozen = frozen | protocol['additionalFrozenFiles']
    assert all(digest(path) == value for path,value in all_frozen.items())
    excluded = set()
    for receipt in protocol['excludedSelections']:
        assert str(Path(receipt).resolve()) in all_frozen
        excluded.update(json.loads(Path(receipt).read_text())['positionKeys'])
    adapter = Path(protocol['adapterPath']).resolve()
    assert frozen[str(adapter)] == digest(adapter)
    spec = importlib.util.spec_from_file_location('sr030_frozen_adapter', adapter)
    b = importlib.util.module_from_spec(spec); spec.loader.exec_module(b)
    seen = set(); totals = []; records = []; looks = []
    for index, run_dir in enumerate(runs):
        if looks and looks[-1]['decision'] != 'continue':
            raise ValueError('A preceding decision already stopped this confirmation')
        stage = protocol['stages'][index]; run_dir = Path(run_dir).resolve()
        receipt = run_dir/'run.json'; run = json.loads(receipt.read_text())
        assert run['status'] == 'passed' and run['inputsStable'] is True
        for key,value in protocol['expectedRuntime'].items():
            assert run[key] == value, (key,run[key],value)
        args = run['arguments']
        for key,value in protocol['expectedArguments'].items():
            assert args[key] == value, (key,args[key],value)
        assert Path(args['out']).resolve() == run_dir
        assert args['pairs'] == stage['pairs']
        opening = Path(stage['openings']).resolve()
        assert Path(args['openings']).resolve() == opening
        assert digest(opening) == stage['sha256'] == run['filesBefore'][str(opening)]
        assert all(run['filesBefore'][path] == value for path,value in frozen.items())
        for side, source in protocol['expectedSources'].items():
            identity = run[side+'Preflight']['identity']
            for fact in ('source='+source,'lmr=quiet','workers=1','leaves=unpruned',
                         'checkpoint='+b.CHECKPOINT,'sha256='+b.NETWORK_SHA256):
                assert fact in identity, (side,fact)
            assert 'aspiration=off' in identity
            b.check_lmp_identity(identity, protocol['expectedArguments'][side+'_lmp'],
                                 check=protocol['expectedArguments'][side+'_check'])
        keys = []
        for prefix in b.opening_prefixes(opening,stage['pairs']):
            board = b.chess.Board()
            for move in prefix: board.push_uci(move)
            keys.append(' '.join(board.fen().split()[:4]))
        assert keys == stage['positionKeys']
        assert all(int(hashlib.sha256(key.encode()).hexdigest(),16)%2 == 1 for key in keys)
        assert not excluded.intersection(keys)
        assert not seen.intersection(keys); seen.update(keys)
        audit = b.analyze(run_dir/'games.pgn',opening,stage['pairs'],True,True)
        assert audit['terminations'] == {'normal': stage['pairs']*2}, 'Adverse confirmation termination'
        for key in ('wdl','terminations','statistics','wholeRequestResources'):
            assert audit[key] == run['results'][key], key
        scores = [g['candidateScore'] for g in audit['games']]
        totals.extend(scores[i]+scores[i+1] for i in range(0,len(scores),2))
        look = decision(totals); looks.append(look)
        records.append({'run':str(run_dir),'receiptSha256':digest(receipt),
                        'pgnSha256':digest(run_dir/'games.pgn'),'wdl':audit['wdl'],
                        'wholeRequestResources':audit['wholeRequestResources']})
    assert all(digest(path) == value for path,value in all_frozen.items())
    return {'status':'passed','protocol':str(Path(protocol_path).resolve()),
            'protocolSha256':digest(protocol_path),'assessorSha256':digest(__file__),
            'inputsStable':True,'stages':records,'looks':looks,'pairTotals':totals,
            'finalDecision':looks[-1]['decision']}


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--protocol',required=True)
    parser.add_argument('--run',action='append',required=True)
    parser.add_argument('--out',required=True)
    args = parser.parse_args()
    output = Path(args.out)
    if output.exists(): raise ValueError('Assessment output already exists')
    result = {'status':'failed','arguments':vars(args)}
    try:
        result = assess(args.protocol,args.run)
        print(json.dumps(result['looks'][-1],indent=2))
    except BaseException as error:
        result['failure'] = repr(error)
        raise
    finally:
        output.write_text(json.dumps(result,indent=2,allow_nan=False)+'\n',encoding='utf-8')
