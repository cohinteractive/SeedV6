"""CH-R001 explicit correction profiles over the qualified owned match runner."""
import argparse
import importlib.util
import json
from pathlib import Path
import sys

sys.dont_write_bytecode = True
spec = importlib.util.spec_from_file_location('ch_learning', Path(__file__).with_name('p4-learning.py'))
hl = importlib.util.module_from_spec(spec)
spec.loader.exec_module(hl)
m, b = hl.m, hl.m.b
m.OUT = m.ROOT / 'docs/research/search/p4-correction'


def command(runtime, profile='guided', workers=1, actor='diagnostic'):
    if profile == 'production85':
        return hl.old_command(runtime, 'control', workers, actor)
    if profile not in ('corrected', 'corrected-q'):
        return hl.command(runtime, profile, workers, actor)
    return hl.old_command(runtime, 'control', workers, actor)[:-1] + [
        'com.ohinteractive.seedv6.integration.P4CorrectionUciMain',
        '--network', str(m.MODEL), '--profile', profile]


def configure(engine, workers, hash_mib=64, profile='guided'):
    if profile not in ('production85', 'corrected', 'corrected-q'):
        return hl.configure(engine, workers, hash_mib, profile)
    identity = hl.old_configure(engine, workers, hash_mib, profile)
    expected = ('profile=corrected-q', 'formulation=CH-C2', 'flags=506724351',
                'correction=pawn-main-q-ema32-cap256-request', 'ttPolicy=production-hints', 'slotBytes=24') if profile == 'corrected-q' else (
        ('flags=104071167', 'searchProfile=HL_G1') if profile == 'production85' else (
        'profile=corrected', 'formulation=CH-C1', 'flags=238288895',
        'correction=pawn-main-ema32-cap256-request', 'ttPolicy=production-hints', 'slotBytes=24'))
    for token in expected:
        if token not in identity:
            raise ValueError('Missing correction identity ' + token)
    return identity


m.command, m.configure = command, configure


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--protocol', required=True)
    parser.add_argument('--workers', type=int, choices=(1, 4), required=True)
    args = parser.parse_args()
    path = m.OUT / args.protocol
    p = json.loads(path.read_text(encoding='utf-8'))
    if b.digest(__file__) != p['adapterSha256'] or args.workers not in p['workers']:
        raise ValueError('Adapter/domain differs from prospective protocol')
    if p['baselineProfile'] != 'guided' or p['baseline'] != str((m.ROOT / 'docs/research/search/p4-learning/raw/runtime-build84-v1').resolve()):
        raise ValueError('Require exact frozen84 guided champion')
    if b.digest(p['openings']) != p['openingSha256']:
        raise ValueError('Opening input differs from protocol')
    for manifest_path in p['runtimeManifests']:
        manifest = json.loads(Path(manifest_path).read_text(encoding='utf-8'))
        if any(b.digest(f) != h for f, h in manifest['files'].items()):
            raise ValueError('Frozen runtime differs from manifest ' + manifest_path)
    if p['tc'] != '10+0.1' or p['pairs']*2 > p['remainingCategoryGames'] or p['pairs']*2 > p['remainingTotalGames']:
        raise ValueError('Unapproved clock or resource admission')
    if p['timeoutSeconds'] > p['remainingMatchSeconds'] or p['estimatedMatchSeconds'] > p['timeoutSeconds']:
        raise ValueError('Runtime does not fit remaining allowance')
    match_args = argparse.Namespace(name=p['namePrefix']+'-w'+str(args.workers),
        baseline=p['baseline'], candidate=p['candidate'], baseline_policy=p['baselineProfile'],
        candidate_policy=p['candidateProfile'], openings=p['openings'], workers=args.workers,
        pairs=p['pairs'], tc=p['tc'], protocol=args.protocol, warning_policy=p['warningPolicy'],
        seed=p['seed'], concurrency=1, timeout=p['timeoutSeconds'])
    m.match(match_args)
    if b.digest(__file__) != p['adapterSha256']:
        raise ValueError('Routing adapter changed during match')


if __name__ == '__main__':
    main()
