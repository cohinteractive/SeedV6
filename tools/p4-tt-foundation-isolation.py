"""Frozen control, prototype and production routing qualification for TTF-R001."""
import argparse
import importlib.util
import json
from pathlib import Path
import sys
import time

sys.dont_write_bytecode = True
spec = importlib.util.spec_from_file_location('ttf', Path(__file__).with_name('p4-tt-foundation.py'))
ttf = importlib.util.module_from_spec(spec)
spec.loader.exec_module(ttf)
m, b = ttf.m, ttf.m.b


def main(args):
    out = m.OUT / 'raw' / args.name
    out.mkdir(exist_ok=False)
    old = m.ROOT / 'docs/research/search/p4-tt-memory/raw'
    for name in ('runtime80','runtimeh1-v1','runtimeh2-v1'):
        manifest=json.loads((old.parent/(name+'-manifest.json')).read_text())
        if any(b.digest(p)!=h for p,h in manifest['files'].items()):
            raise ValueError('Historical runtime differs from its frozen manifest: '+name)
    candidate = Path(args.runtime).resolve()
    profiles = {
        'frozen80': (old/'runtime80', 'control'),
        'production': (candidate, 'control'),
        'researchControl': (candidate, 'research-control'),
        'frozenH1': (old/'runtimeh1-v1', 'h1'),
        'h1': (candidate, 'h1'),
        'frozenH2': (old/'runtimeh2-v1', 'h2'),
        'h2': (candidate, 'h2'),
        'foundation': (candidate, 'foundation')}
    openings = old.parent/'bridge-v1-openings.pgn'
    positions = ['startpos'] + ['startpos moves '+' '.join(x) for x in b.opening_prefixes(openings, 32)[:8]]
    frozen = {str(p.resolve()): b.digest(p) for p in [Path(__file__), Path(ttf.__file__), Path(m.__file__), openings]}
    for runtime, _ in profiles.values():
        frozen.update(b.installation_hashes(runtime))
    report = {'status': 'running', 'rows': [], 'identities': {}, 'filesBefore': frozen, 'commands': {}}
    try:
        for name, (runtime, profile) in profiles.items():
            cmd = m.command(runtime, 'h2')[:-1]+['control'] if profile == 'research-control' else m.command(runtime, profile)
            report['commands'][name] = cmd
            engine = b.Uci(cmd, out/(name+'.txt'))
            try:
                for workers in (1, 4):
                    report['identities'][name+'-w'+str(workers)] = m.configure(engine, workers, profile='control' if profile=='research-control' else profile)
                    requests = [(p, 'depth 5') for p in positions] if workers == 1 else []
                    requests += [('startpos', 'nodes 20000'), ('startpos', 'nodes 100000')]
                    for position, request in requests:
                        board = b.chess.Board()
                        if ' moves ' in position:
                            for move in position.split(' moves ')[1].split(): board.push_uci(move)
                        engine.send('ucinewgame'); engine.send('isready'); engine.until('readyok')
                        engine.send('position '+position); engine.send('go '+request)
                        result = b.inspect_result(board, engine.until('bestmove ', 60))
                        if request.startswith('nodes ') and result['nodes'] != int(request.split()[1]):
                            raise ValueError('Request accounting')
                        report['rows'].append(dict(profile=name, workers=workers, position=position, request=request, result=result))
                    if name == 'foundation':
                        engine.send('position startpos'); engine.send('go infinite'); time.sleep(.1); engine.send('stop')
                        result = b.inspect_result(b.chess.Board(), engine.until('bestmove ', 30))
                        report.setdefault('stopRequests', []).append(dict(workers=workers, result=result))
                        engine.send('ucinewgame'); engine.send('isready'); engine.until('readyok')
            finally:
                engine.close()
        for group in [('frozen80','production','researchControl'), ('frozenH1','h1'), ('frozenH2','h2','foundation')]:
            for position, request in [(p,'depth 5') for p in positions]+[('startpos','nodes 20000'),('startpos','nodes 100000')]:
                values = [m.stable(r['result']) for r in report['rows'] if r['profile'] in group and r['workers']==1
                          and r['position']==position and r['request']==request]
                if len(values)!=len(group) or any(v!=values[0] for v in values):
                    raise ValueError('W1 policy/control mismatch '+str(group)+' '+position+' '+request)
        if any(b.digest(p)!=h for p,h in frozen.items()): raise ValueError('Input changed')
        report.update(status='passed',inputsStable=True)
    except BaseException as error:
        report.update(status='failed',failure=repr(error)); raise
    finally:
        b.save(out/'observations.json',report)
    summary={k:v for k,v in report.items() if k not in ['rows','filesBefore']}
    summary.update(requests=len(report['rows']),stopRequests=report.get('stopRequests',[]),raw=str(out),
                   interpretation='W1 controls match, including foundation versus frozen H2; W4 verifies accounting/PV/lifecycle, not scheduling equality. No strength claim.')
    b.save(m.OUT/(args.name+'-summary.json'),summary)


if __name__ == '__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--runtime',required=True);parser.add_argument('--name',required=True)
    main(parser.parse_args())
