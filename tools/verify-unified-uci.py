"""Bounded WU2 process checks using the retained SR-024 independent UCI auditor."""
import argparse
import importlib.util
import json
import zipfile
import re
from pathlib import Path

spec = importlib.util.spec_from_file_location('sr024_integration', Path(__file__).with_name('verify-sr024-integration.py'))
v = importlib.util.module_from_spec(spec)
spec.loader.exec_module(v)


def production_command(a, install, workers):
    cmd = v.command(a, install, 'production', workers)
    with zipfile.ZipFile(Path(install)/'lib/app.jar') as jar:
        if 'com/ohinteractive/seedv6/uci/LegacyUciEvaluation.class' in jar.namelist():
            cmd = [x for x in cmd if not re.match(r'-Dseedv6\.uci\.[^=]*Research=', x)]
    return cmd


def main(a):
    root = Path(__file__).resolve().parents[1]
    out = Path(a.out).resolve()
    if not out.is_relative_to(root/'build/migration'):
        raise ValueError('Output must stay in the migration worktree')
    out.mkdir(parents=True, exist_ok=False)
    report = {'status': 'running', 'arguments': vars(a), 'parity': [], 'workers': [], 'transitions': []}
    assert v.b.digest(Path(a.accepted)/'lib/app.jar') == '2341955302900ae1a15879d0cff6b14a3744498db0dd862b8cd2aabe7482ed0f'
    assert v.b.digest(Path(a.store)/'checkpoints'/v.b.CHECKPOINT/'network.brn-pair2') == v.b.NETWORK_SHA256
    active = []
    try:
        openings = root/'docs/research/search/sr024-phase2-2026-10-14/aa-openings.pgn'
        count = json.loads(openings.with_suffix('.selection.json').read_text())['count']
        prefixes = [[]] + v.b.opening_prefixes(openings, count)[:8]
        for label, install in [('accepted', a.accepted), ('current', a.current)]:
            active.append(v.start(production_command(a, install, 1), out/(label+'.txt')))
            v.identify(active[-1], 1, 'production')
        for prefix in prefixes:
            for go in ('go depth 5', 'go nodes 100000'):
                results = [v.v.request(e, prefix, go) for e in active]
                assert v.result_key(results[0]) == v.result_key(results[1]), (prefix, go, results)
                assert results[0]['termination'] == results[1]['termination']
                report['parity'].append({'prefix': prefix, 'go': go, 'results': results})
        for e in active: e.close()
        active = []
        e = v.start(production_command(a, a.current, 1), out/'workers.txt')
        active.append(e)
        for workers in range(1, 17):
            identity = v.identify(e, workers, 'production')
            assert 'experimental=true' not in identity
            assert 'futility=' not in identity or 'futility=off' in identity
            results = [v.v.request(e, [], 'go nodes 100000') for _ in range(2)]
            assert all(r['nodes'] == 100000 and r['termination'] == 'NODE_LIMIT' for r in results)
            report['workers'].append({'workers': workers, 'identity': identity, 'results': results})
        for workers in (1, 4, 1, 6, 2, 16, 1):
            identity = v.identify(e, workers, 'production')
            timed = v.v.request(e, prefixes[1], 'go movetime 50')
            assert timed['termination'] == 'TIME_LIMIT', timed
            e.send('ucinewgame'); e.send('isready'); e.until('readyok')
            e.send('position startpos'); e.send('go infinite')
            lines = e.until('info depth 1 ', 90)
            e.send('stop'); lines += e.until('bestmove ', 90)
            stopped = v.b.inspect_result(v.b.chess.Board(), lines)
            assert stopped['termination'] == 'STOPPED', stopped
            report['transitions'].append({'workers': workers, 'identity': identity, 'timed': timed, 'stopped': stopped})
        report.update(status='passed', exactComparisons=len(report['parity']),
                      requests=2*len(report['parity'])+2*len(report['workers'])+2*len(report['transitions']))
    finally:
        for e in active: e.close()
        (out/'result.json').write_text(json.dumps(report, indent=2)+'\n', encoding='utf-8')
    print(json.dumps({k: report[k] for k in ('status', 'exactComparisons', 'requests')}))


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    for key in ('java', 'store', 'accepted', 'current', 'out'):
        p.add_argument('--'+key, required=True)
    main(p.parse_args())
