"""HL-R001 explicit profiles over the existing owned, fully audited match runner."""
import argparse
import importlib.util
import json
from pathlib import Path
import sys

sys.dont_write_bytecode = True
spec = importlib.util.spec_from_file_location('hl_match', Path(__file__).with_name('p4-tt-memory.py'))
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)
m.OUT = m.ROOT / 'docs/research/search/p4-learning'
old_command, old_configure = m.command, m.configure
PROFILES = {'learning-control': ('control', 'HL-C80', 3407871),
            'retain': ('retain', 'HL-R1', 36962303),
            'guided': ('guided', 'HL-G1', 104071167)}


def command(runtime, profile='control', workers=1, actor='diagnostic'):
    if profile not in PROFILES:
        return old_command(runtime, profile, workers, actor)
    return old_command(runtime, 'control', workers, actor)[:-1] + [
        'com.ohinteractive.seedv6.integration.P4LearningUciMain',
        '--network', str(m.MODEL), '--profile', PROFILES[profile][0]]


def configure(engine, workers, hash_mib=64, profile='control'):
    identity = old_configure(engine, workers, hash_mib, profile)
    if profile in PROFILES:
        p, formulation, flags = PROFILES[profile]
        for token in (f'profile={p}', f'formulation={formulation}', f'flags={flags}',
                      'ttPolicy=production-hints', 'slotBytes=24'):
            if token not in identity:
                raise ValueError('Missing learning identity ' + token)
    return identity


m.command, m.configure = command, configure


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--protocol', required=True)
    parser.add_argument('--workers', type=int, choices=(1, 4), required=True)
    args = parser.parse_args()
    path = m.OUT / args.protocol
    p = json.loads(path.read_text(encoding='utf-8'))
    if m.b.digest(__file__) != p['adapterSha256']:
        raise ValueError('Adapter differs from prospective protocol')
    if args.workers not in p['workers']:
        raise ValueError('Undeclared worker domain')
    if m.b.digest(p['openings']) != p['openingSha256']:
        raise ValueError('Opening input differs from protocol')
    for manifest_path in p['runtimeManifests']:
        manifest = json.loads(Path(manifest_path).read_text(encoding='utf-8'))
        if any(m.b.digest(f) != h for f, h in manifest['files'].items()):
            raise ValueError('Frozen runtime differs from manifest ' + manifest_path)
    match_args = argparse.Namespace(name=p['namePrefix']+'-w'+str(args.workers),
        baseline=p['baseline'], candidate=p['candidate'], baseline_policy=p['baselineProfile'],
        candidate_policy=p['candidateProfile'], openings=p['openings'], workers=args.workers,
        pairs=p['pairs'], tc=p['tc'], protocol=args.protocol, warning_policy=p['warningPolicy'],
        seed=p['seed'], concurrency=1, timeout=p['timeoutSeconds'])
    m.match(match_args)
    if m.b.digest(__file__) != p['adapterSha256']:
        raise ValueError('Routing adapter changed during match')


if __name__ == '__main__':
    main()
