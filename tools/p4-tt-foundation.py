"""TTF-R001 explicit launcher adapter over the qualified owned match/audit runner.

Historical protocols/results are not changed. All new evidence lives in the new
objective. Existing profile validation remains; the foundation has extra identity
checks. Never interpret this exploratory adapter as promotion authority.
"""
import argparse
import importlib.util
import json
from pathlib import Path
import sys

sys.dont_write_bytecode = True
spec = importlib.util.spec_from_file_location('ttf_match', Path(__file__).with_name('p4-tt-memory.py'))
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)
m.OUT = m.ROOT / 'docs/research/search/p4-tt-foundation'
old_command, old_configure = m.command, m.configure


def command(runtime, profile='control', workers=1, actor='diagnostic'):
    if profile != 'foundation':
        return old_command(runtime, profile, workers, actor)
    base = old_command(runtime, 'control', workers, actor)
    return base[:-1] + ['com.ohinteractive.seedv6.integration.P4TtFoundationUciMain',
                       '--network', str(m.MODEL)]


def configure(engine, workers, hash_mib=64, profile='control'):
    identity = old_configure(engine, workers, hash_mib, profile)
    if profile == 'foundation':
        for token in ('formulation=TTF-F3-v1', 'flags=32767999', 'slotBytes=24',
                      'ttPolicy=F3-packed-coherent-main-raw-request-H2-score-age1'):
            if token not in identity:
                raise ValueError('Missing foundation identity ' + token)
    return identity


m.command, m.configure = command, configure

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--protocol', required=True)
    parser.add_argument('--workers', type=int, choices=(1, 4), required=True)
    args = parser.parse_args()
    protocol_path = m.OUT / args.protocol
    protocol = json.loads(protocol_path.read_text(encoding='utf-8'))
    if m.b.digest(__file__) != protocol['adapterSha256']:
        raise ValueError('Adapter differs from prospective protocol')
    if args.workers not in protocol['workers']:
        raise ValueError('Undeclared worker domain')
    for manifest_path in protocol['runtimeManifests']:
        manifest=json.loads(Path(manifest_path).read_text(encoding='utf-8'))
        if any(m.b.digest(p)!=h for p,h in manifest['files'].items()):
            raise ValueError('Frozen runtime differs from manifest '+manifest_path)
    match_args = argparse.Namespace(
        name=protocol['namePrefix'] + '-w' + str(args.workers),
        baseline=protocol['baseline'], candidate=protocol['candidate'],
        baseline_policy=protocol['baselineProfile'], candidate_policy='foundation',
        openings=protocol['openings'], workers=args.workers, pairs=protocol['pairs'],
        tc=protocol['tc'], protocol=args.protocol, warning_policy=protocol['warningPolicy'],
        seed=protocol['seed'], concurrency=1, timeout=protocol['timeoutSeconds'])
    m.match(match_args)
    # The reused runner freezes all its own supports. This new routing wrapper is
    # an additional input, and its prospective hash must still hold at completion.
    stable=m.b.digest(__file__)==protocol['adapterSha256']
    m.b.save(m.OUT/(match_args.name+'-adapter-validation.json'),
             {'status':'passed' if stable else 'failed','adapterSha256':m.b.digest(__file__),
              'protocol':args.protocol,'runnerSummary':match_args.name+'-summary.json'})
    if not stable:
        raise ValueError('Routing adapter changed during match')
