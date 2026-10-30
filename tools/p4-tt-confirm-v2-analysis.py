"""Apply the prospective second H1 W1 confirmation rule to a complete audited match."""
import argparse
import hashlib
import json
import math
from pathlib import Path
import statistics

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT/'docs/research/search/p4-tt-memory'
PROTOCOL = OUT/'h1-confirm-w1-v2-protocol.json'


def digest(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def analyze(side):
    run_dir = OUT/'raw'/('h1-confirm-w1-v2-'+side)
    target = OUT/('h1-confirm-w1-v2-'+side+'-analysis.json')
    if target.exists():
        raise ValueError('Never overwrite a completed confirmation analysis')
    protocol = json.loads(PROTOCOL.read_text())
    if protocol['status'] != 'prospectively frozen before confirmation games':
        raise ValueError('Protocol is not frozen')
    if protocol['analysisToolSha256'] != digest(__file__):
        raise ValueError('Prospectively bound analysis implementation changed')
    run_path = run_dir/'run.json'
    run = json.loads(run_path.read_text())
    args = run['arguments']
    stats = run['results']['statistics']
    expected_control = 'runtime80' if side == 'control80' else 'runtime79'
    if (run['status'] != 'passed' or not run['inputsStable'] or run['runnerExitCode'] != 0
            or args['workers'] != 1 or args['pairs'] != 160 or args['tc'] != '10+0.1'
            or args['concurrency'] != 1 or args['protocol'] != PROTOCOL.name
            or args['warning_policy'] != 'audited-fallback'
            or not run.get('engineHandlesDrained')
            or run.get('traceAudit', {}).get('status') != 'passed'
            or args['candidate_policy'] != 'h1' or args['baseline_policy'] != 'control'
            or Path(args['baseline']).name != expected_control
            or Path(args['candidate']).name != 'runtimeh1-v1'
            or stats['pairs'] != 160 or run['results']['terminations'] != {'normal': 320}):
        raise ValueError('Not the completed, qualified, prospectively defined comparison')
    if run['filesBefore'][str(PROTOCOL.resolve())] != digest(PROTOCOL):
        raise ValueError('Protocol changed after freezing')
    if run['filesBefore'][str((ROOT/args['openings']).resolve())] != protocol['openingSha256']:
        raise ValueError('Confirmation opening identity mismatch')
    for name in (expected_control, 'runtimeh1-v1'):
        path = OUT/(name+'-manifest.json')
        if digest(path) != protocol['runtimeManifestSha256'][path.name]:
            raise ValueError('Bound runtime manifest changed')
        manifest = json.loads(path.read_text())
        if any(run['filesBefore'].get(p) != h for p, h in manifest['files'].items()):
            raise ValueError('Actual match artifact differs from the frozen source/executable identity')
    alpha = 0.00625
    if protocol['claimAttempt'] != 2 or protocol['alphaPerComparison'] != alpha or protocol['pairs'] != 160:
        raise ValueError('Prospective error allocation or sample differs')
    mean, se = stats['score'], stats['pairScoreStandardError']
    if not math.isfinite(mean) or not math.isfinite(se) or not 0 <= mean <= 1 or se < 0:
        raise ValueError('Invalid paired statistics')
    z = statistics.NormalDist().inv_cdf(1-alpha/2)
    normal = [max(0, mean-z*se), min(1, mean+z*se)] if se > 0 else None
    radius = math.sqrt(math.log(2/alpha)/(2*stats['pairs']))
    bounded = [max(0, mean-radius), min(1, mean+radius)]
    primary = normal if normal is not None else bounded
    result = {
        'status':'completed adjusted analysis', 'claimAttempt':2, 'domain':'W1 OWNER10+0.1',
        'opponent':side, 'candidate':'P4-TT-H1-v1/a72cc496/build81',
        'protocol':PROTOCOL.name, 'protocolSha256':digest(PROTOCOL),
        'raw':str(run_dir), 'runSha256':digest(run_path), 'pgnSha256':digest(run_dir/'games.pgn'),
        'analysisToolSha256':digest(__file__), 'pairs':stats['pairs'],
        'wdl':run['results']['wdl'], 'pentanomial':stats['pentanomial'],
        'score':mean, 'pairScoreStandardError':se, 'alphaThisComparison':alpha,
        'normalCriticalValue':z, 'pairedNormal99_375Score':normal,
        'boundedHoeffding99_375Score':bounded,
        'primaryIntervalUsed':'paired normal' if normal is not None else 'bounded fallback for degenerate sample variance',
        'primaryCriterionPassed':primary[0] > .5,
        'conservativeSensitivityPositive':bounded[0] > .5,
        'method':'Prospective nominal alpha allocation; paired-normal primary inference is approximate. The bounded interval is reported as conservative sensitivity, not silently discarded.',
        'acceptance':'Research inference only; no champion advancement or production promotion.'}
    if side == 'control80':
        result['next'] = ('Execute the frozen160-pair direct79 comparison.' if result['primaryCriterionPassed']
                          else 'Do not execute the conditional direct79 block under this protocol; H1 is not independently confirmed. Reassess affordable connected work without extending or pooling this sample.')
    else:
        first_path = OUT/'h1-confirm-w1-v2-control80-analysis.json'
        first = json.loads(first_path.read_text())
        if not first['primaryCriterionPassed'] or first['protocolSha256'] != digest(PROTOCOL):
            raise ValueError('Conditional direct79 gate was not met')
        result['firstAnalysisSha256'] = digest(first_path)
        result['jointPrimaryCriterionPassed'] = result['primaryCriterionPassed']
        result['next'] = 'Record the joint result, conservative sensitivity, supported resource domain and remaining qualifications; owner/GPT acceptance remains separate.'
    target.write_text(json.dumps(result, indent=2)+'\n', encoding='utf-8')
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('side', choices=('control80', 'champion79'))
    analyze(parser.parse_args().side)
