"""CH-R001 frozen84/default/explicit guided and correction UCI qualification."""
import argparse
import importlib.util
import json
from pathlib import Path
import sys
import time

sys.dont_write_bytecode = True
spec = importlib.util.spec_from_file_location('hl', Path(__file__).with_name('p4-correction.py'))
hl = importlib.util.module_from_spec(spec)
spec.loader.exec_module(hl)
m, b = hl.m, hl.m.b


def main(args):
    started = time.monotonic()
    out = m.OUT / 'raw' / args.name
    out.mkdir(exist_ok=False)
    old = m.ROOT / 'docs/research/search/p4-tt-memory/raw'
    for manifest_path in (m.ROOT/'docs/research/search/p4-learning/runtime-build84-v1-manifest.json', Path(args.manifest)):
        manifest = json.loads(manifest_path.read_text())
        if any(b.digest(p) != h for p, h in manifest['files'].items()):
            raise ValueError('Runtime differs from manifest ' + str(manifest_path))
    candidate = Path(args.runtime).resolve()
    profiles = {'frozen84': (m.ROOT/'docs/research/search/p4-learning/raw/runtime-build84-v1', 'guided'),
                'production': (candidate, 'production85'), 'researchControl': (candidate, 'guided'),
                'corrected': (candidate, 'corrected')}
    openings = old.parent/'bridge-v1-openings.pgn'
    positions = ['startpos'] + ['startpos moves '+' '.join(x) for x in b.opening_prefixes(openings, 32)[:8]]
    frozen = {str(p.resolve()): b.digest(p) for p in [Path(__file__), Path(hl.__file__), Path(m.__file__), openings]}
    for runtime, _ in profiles.values():
        frozen.update(b.installation_hashes(runtime))
    report = {'status': 'running', 'rows': [], 'identities': {}, 'filesBefore': frozen, 'commands': {}}
    try:
        for name, (runtime, profile) in profiles.items():
            cmd = m.command(runtime, profile)
            report['commands'][name] = cmd
            engine = b.Uci(cmd, out/(name+'.txt'))
            try:
                for workers in (1, 4):
                    report['identities'][name+'-w'+str(workers)] = m.configure(engine, workers, profile=profile)
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
                    if name in ('production', 'corrected'):
                        engine.send('position startpos'); engine.send('go infinite'); time.sleep(.1); engine.send('stop')
                        result = b.inspect_result(b.chess.Board(), engine.until('bestmove ', 30))
                        report.setdefault('stopRequests', []).append(dict(profile=name, workers=workers, result=result))
                        engine.send('ucinewgame'); engine.send('isready'); engine.until('readyok')
            finally:
                engine.close()
        for position, request in [(p,'depth 5') for p in positions]+[('startpos','nodes 20000'),('startpos','nodes 100000')]:
            values = [m.stable(r['result']) for r in report['rows'] if r['profile'] in ('frozen84','production','researchControl')
                      and r['workers']==1 and r['position']==position and r['request']==request]
            if len(values)!=3 or any(v!=values[0] for v in values):
                raise ValueError('W1 control mismatch '+position+' '+request)
        if any(b.digest(p)!=h for p,h in frozen.items()): raise ValueError('Input changed')
        report.update(status='passed',inputsStable=True)
    except BaseException as error:
        report.update(status='failed',failure=repr(error)); raise
    finally:
        report['wallSeconds'] = time.monotonic()-started
        b.save(out/'observations.json',report)
    summary = {k:v for k,v in report.items() if k not in ('rows','filesBefore')}
    summary.update(requests=len(report['rows']),raw=str(out),
        interpretation='W1 frozen84 guided, current production and research control match. Candidate/W4 checks establish bounded accounting/PV/lifecycle qualification, not strength or scheduling equality.')
    b.save(m.OUT/(args.name+'-summary.json'),summary)
    print(json.dumps({'status':report['status'],'requests':len(report['rows']),'wallSeconds':report['wallSeconds']}))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for key in ('runtime','manifest','name'): parser.add_argument('--'+key,required=True)
    main(parser.parse_args())
