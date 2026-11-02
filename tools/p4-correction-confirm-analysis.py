"""Apply the prospectively frozen CH-R001 W1 champion-relative confirmation criterion."""
import argparse
import hashlib
import json
import math
from pathlib import Path
import statistics

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'docs/research/search/p4-correction'


def digest(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def paired_inference(values, alpha, practical_minimum):
    if len(values) != 160 or any(x not in (0, .25, .5, .75, 1) for x in values):
        raise ValueError('Require 160 complete opening-pair scores')
    mean = statistics.mean(values)
    se = statistics.stdev(values) / math.sqrt(len(values))
    z = statistics.NormalDist().inv_cdf(1 - alpha)
    normal_lower = max(0, mean - z * se) if se > 0 else None
    bounded_lower = max(0, mean - math.sqrt(math.log(1 / alpha) / (2 * len(values))))
    primary_lower = normal_lower
    return {'score': mean, 'pairScoreStandardError': se, 'oneSidedAlpha': alpha,
            'normalCriticalValue': z, 'pairedNormalLower': normal_lower,
            'boundedHoeffdingLower': bounded_lower, 'primaryLower': primary_lower,
            'primaryMethod': 'paired normal approximation' if normal_lower is not None else 'normal unavailable for zero sample variance; no success claim',
            'practicalMinimumScore': practical_minimum,
            'primaryCriterionPassed': primary_lower is not None and primary_lower > .5 and mean >= practical_minimum,
            'conservativeSensitivityPositive': bounded_lower > .5}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--protocol', required=True)
    args = parser.parse_args()
    path = OUT / args.protocol
    p = json.loads(path.read_text(encoding='utf-8'))
    if (p['status'] != 'prospectively frozen before confirmation games'
            or p['analysisToolSha256'] != digest(__file__)
            or p['claimAttempt'] != 1 or p['oneSidedAlpha'] != .025
            or p['pairs'] != 160 or p['practicalMinimumScore'] != .53):
        raise ValueError('Unexpected prospective claim, criterion or analysis identity')
    run_path = OUT / 'raw' / (p['namePrefix'] + '-w1') / 'run.json'
    run = json.loads(run_path.read_text(encoding='utf-8'))
    a = run['arguments']
    expected = {'baseline': p['baseline'], 'candidate': p['candidate'],
                'baseline_policy': 'guided', 'candidate_policy': p['candidateProfile'],
                'openings': p['openings'], 'workers': 1, 'pairs': 160, 'tc': '10+0.1',
                'protocol': path.name, 'warning_policy': 'audited-fallback',
                'seed': p['seed'], 'concurrency': 1, 'timeout': p['timeoutSeconds']}
    if any(a.get(k) != v for k, v in expected.items()):
        raise ValueError('Actual comparison differs from frozen settings')
    if (run['status'] != 'passed' or not run['inputsStable'] or run['runnerExitCode'] != 0
            or not run['engineHandlesDrained'] or run['traceAudit']['status'] != 'passed'
            or run['results']['terminations'] != {'normal': 320}
            or Path(p['baseline']).resolve() != (ROOT / 'docs/research/search/p4-learning/raw/runtime-build84-v1').resolve()):
        raise ValueError('Not a complete audited champion-relative W1 comparison')
    if run['filesBefore'][str(path.resolve())] != digest(path):
        raise ValueError('Protocol changed during or after games')
    if digest(p['openings']) != p['openingSha256'] or digest(p['openingSelection']) != p['openingSelectionSha256']:
        raise ValueError('Held-out input identity changed')
    if run['filesBefore'][str(Path(p['openings']).resolve())] != p['openingSha256']:
        raise ValueError('Match did not use the frozen confirmation input')
    for manifest_path in p['runtimeManifests']:
        if digest(manifest_path) != p['runtimeManifestSha256'][manifest_path]:
            raise ValueError('Frozen runtime identity manifest changed')
        files = json.loads(Path(manifest_path).read_text(encoding='utf-8'))['files']
        if any(run['filesBefore'].get(f) != h or digest(f) != h for f, h in files.items()):
            raise ValueError('Actual artifact differs from frozen identity')
    scores = [g['candidateScore'] for g in run['results']['games']]
    if len(scores) != 320 or any(x not in (0, .5, 1) for x in scores):
        raise ValueError('Invalid complete game scores')
    values = [(scores[i] + scores[i + 1]) / 2 for i in range(0, len(scores), 2)]
    result = paired_inference(values, p['oneSidedAlpha'], p['practicalMinimumScore'])
    pentanomial = [values.count(x) for x in (0, .25, .5, .75, 1)]
    original = run['results']['statistics']
    if (pentanomial != original['pentanomial'] or original['pairs'] != 160
            or abs(original['score'] - result['score']) > 1e-12
            or abs(original['pairScoreStandardError'] - result['pairScoreStandardError']) > 1e-12):
        raise ValueError('Independent recomputation disagrees with audited summary')
    result.update(status='completed prospective analysis', claimAttempt=1,
                  domain='W1 OWNER, CP64 Hash64, 10+0.1, frozen fresh opening distribution',
                  candidate=p['candidateIdentity'], opponent=p['controlIdentity'],
                  wdl=run['results']['wdl'], pentanomial=pentanomial, pairs=160,
                  protocol=str(path), protocolSha256=digest(path), raw=str(run_path.parent),
                  runSha256=digest(run_path), pgnSha256=digest(run_path.parent / 'games.pgn'),
                  analysisToolSha256=digest(__file__),
                  interpretation='One frozen finalist, fresh direct champion comparison, one nominal one-sided alpha .025 attempt. '
                  'Pair-level normal inference is approximate; conservative bounded sensitivity is retained, not silently discarded. '
                  'No pooling with exploration or historical claims. No arbitrary-position, worker, clock or platform generalization.',
                  acceptance='Research criterion only. Mandatory W4 characterization and technical qualification remain separately applicable; GPT/owner reconciliation, acceptance and any promotion are distinct.')
    target = OUT / (p['namePrefix'] + '-w1-analysis.json')
    with target.open('x', newline='\n', encoding='utf-8') as stream:
        json.dump(result, stream, indent=2)
        stream.write('\n')
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    main()
