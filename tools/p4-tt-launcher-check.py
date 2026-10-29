"""Bounded Phase 4 launcher/control isolation check; run between strength matches."""
import importlib.util
import json
from pathlib import Path
import sys

sys.dont_write_bytecode = True
spec = importlib.util.spec_from_file_location('p4_memory', Path(__file__).with_name('p4-tt-memory.py'))
p4 = importlib.util.module_from_spec(spec)
spec.loader.exec_module(p4)
b = p4.b


def main():
    out = p4.OUT/'raw/launcher-control-v1'
    out.mkdir(exist_ok=False)
    commands = {
        'control80': p4.command(p4.OUT/'raw/runtime80'),
        'production81': p4.command(p4.OUT/'raw/runtimeh1-v1'),
        'researchControl81': p4.command(p4.OUT/'raw/runtimeh1-v1', 'h1')[:-1] + ['control']}
    frozen = {str(Path(p).resolve()): b.digest(p) for p in
              [__file__, p4.__file__, b.__file__, p4.JAVA, p4.MODEL,
               p4.OUT/'bridge-v1-openings.pgn', p4.OUT/'launcher-control-v1-protocol.json']}
    for name in ('runtime80', 'runtimeh1-v1'):
        manifest = json.loads((p4.OUT/(name+'-manifest.json')).read_text())
        if any(b.digest(p) != h for p, h in manifest['files'].items()):
            raise ValueError('Frozen runtime changed')
        frozen.update(manifest['files'])
    positions = ['startpos'] + ['startpos moves '+' '.join(moves) for moves in
                              b.opening_prefixes(p4.OUT/'bridge-v1-openings.pgn', 32)[:8]]
    report = {'status': 'running', 'commands': commands, 'filesBefore': frozen,
              'protocol': 'launcher-control-v1-protocol.json', 'rows': [], 'identities': {}}
    try:
        for name, command in commands.items():
            engine = b.Uci(command, out/(name+'.txt'))
            try:
                for workers in (1, 4):
                    report['identities'][name+'-w'+str(workers)] = p4.configure(engine, workers)
                    requests = [(pos, 'depth 5') for pos in positions] if workers == 1 else []
                    requests += [('startpos', 'nodes 20000'), ('startpos', 'nodes 100000')]
                    for position, request in requests:
                        board = b.chess.Board()
                        if ' moves ' in position:
                            for move in position.split(' moves ')[1].split(): board.push_uci(move)
                        engine.send('ucinewgame'); engine.send('isready'); engine.until('readyok')
                        engine.send('position '+position); engine.send('go '+request)
                        result = b.inspect_result(board, engine.until('bestmove ', 60))
                        if request.startswith('nodes ') and result['nodes'] != int(request.split()[1]):
                            raise ValueError('Whole-request accounting mismatch')
                        report['rows'].append(dict(profile=name, workers=workers, position=position,
                                                   request=request, result=result))
            finally:
                engine.close()
        for position, request in [(pos, 'depth 5') for pos in positions] + [('startpos', 'nodes 20000'), ('startpos', 'nodes 100000')]:
            results = [p4.stable(r['result']) for r in report['rows'] if r['workers'] == 1
                       and r['position'] == position and r['request'] == request]
            if len(results) != 3 or any(r != results[0] for r in results):
                raise ValueError('W1 control/launcher behavior mismatch: '+position+' / '+request)
        if any(b.digest(p) != h for p, h in frozen.items()): raise ValueError('Inputs changed')
        report.update(status='passed', inputsStable=True)
    except BaseException as error:
        report.update(status='failed', failure=repr(error)); raise
    finally:
        b.save(out/'observations.json', report)
    b.save(p4.OUT/'launcher-control-v1-summary.json', {
        'status':'passed', 'requests':len(report['rows']), 'identities':report['identities'],
        'raw':str(out), 'protocol':report['protocol'], 'scriptSha256':b.digest(__file__),
        'interpretation':'All11 W1 depth/node outcomes match across frozen80 ordinary UCI,81 ordinary UCI and81 research control; W4 accounting/PV qualification only, no scheduling equality or strength claim.'})


if __name__ == '__main__':
    main()
