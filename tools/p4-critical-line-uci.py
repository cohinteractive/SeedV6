"""CLV fixed-node UCI qualification and supervised development-match adapter.

The control executes original build96 app-uci.jar and engine-core.jar directly.
No newer or research executable silently replaces that competitive artifact.
"""
import argparse
import importlib.util
import json
import os
from pathlib import Path
import sys
import zipfile

sys.dont_write_bytecode = True
spec = importlib.util.spec_from_file_location('clv_match_support', Path(__file__).with_name('p4-multiworker-match.py'))
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)
OUT = m.ROOT / 'docs/research/search/p4-critical-line'
IDENTITY = None


def command(runtime, formulation, actor):
    global IDENTITY
    common = [m.JAVA, '--add-modules=jdk.incubator.vector', '-Xms128m', '-Xmx512m',
              '-XX:ActiveProcessorCount=4', '-Djava.awt.headless=true', '-Dseedv6.syzygy.config=',
              '-Dseedv6.syzygy.path=', '-Dseedv6.clockPolicy=BOUNDARY2', '-Dseedv6.research.actor=' + actor]
    if formulation == 'EXACT96':
        return common + ['-Dseedv6.searchProfile=MW_C4_W4', '-cp', str(OUT / 'raw/baseline-build96-v1/app/*'),
                         'com.ohinteractive.seedv6.uci.UciMain']
    if formulation not in ('C1', 'CONTROL'):
        raise ValueError('CLV formulation')
    with zipfile.ZipFile(Path(runtime) / 'lib/engine-core.jar') as jar:
        properties = dict(line.split('=',1) for line in jar.read('com/ohinteractive/seedv6/application-version.properties').decode().splitlines() if '=' in line)
    IDENTITY = dict(build=int(properties['build']),source=properties['sourceRevision'])
    return common + ['-cp', str(Path(runtime) / 'classes') + os.pathsep + str(Path(runtime) / 'lib/*'),
                     'com.ohinteractive.seedv6.integration.ClvUciMain', '--network', str(m.MODEL), '--formulation', formulation]


def configure(engine, workers, formulation):
    engine.send('uci'); lines = engine.until('uciok', 60)
    for name, value in [('Threads', workers), ('Hash', 64), ('OwnBook', 'false')]:
        engine.send(f'setoption name {name} value {value}')
    engine.send('isready'); lines += engine.until('readyok', 60)
    engine.send('uci'); lines += engine.until('uciok', 60)
    identities = [x for x in lines if 'networkSha256=' in x and x.startswith('info string ')]
    if not identities: raise ValueError('Missing runtime identity')
    identity = next(x for x in reversed(identities) if 'formulation snapshot:' not in x)
    baseline = formulation == 'EXACT96'
    flags = (1182007295 if workers == 4 else 1177812991) if baseline else (3329490943 if formulation == 'C1' else 1182007295)
    schedule = 'OWNER' if workers == 1 else 'ROOT_REUSE' if baseline and workers != 4 else 'EAGER_ROOT'
    tokens = ['flags=' + str(flags), 'checkpoint=64', 'schema=S6CNU002', 'heads=8',
              'networkSha256=8431626c2fca81e0a1e9ccd5e45ddd8829e6e3282c756c21fad75b06d075bb2b',
              'scoreMapping=BT4_Q_LOGIT400_V1', 'clockPolicy=boundary2-d6-v1',
              f'workers={workers};hashMiB=64;aspiration=ADAPTIVE16;parallel={schedule}']
    tokens += ['build=96', 'source=44fc2bf2fe54', 'searchProfile=MW_C4_W4'] if baseline else [
        'formulation=CLV-' + formulation, 'researchIdentity=CLV-R001-C1-v1',
        'coreBuild=' + str(IDENTITY['build']), 'coreSource=' + IDENTITY['source']]
    for token in tokens:
        if token not in identity: raise ValueError('Missing identity ' + token + ': ' + identity)
    return identity


def qualify(runtime, output):
    nodes = requests = 0
    with Path(output).open('x', encoding='utf8') as stream:
        for formulation in ('EXACT96', 'CONTROL', 'C1'):
            engine = m.b.Uci(command(runtime, formulation, 'qualification'), Path(output).parent / (formulation + '-uci.txt'))
            try:
                for i, (workers, cap, stop) in enumerate([(4, 20000, False), (4, 100000, False),
                        (4, 100000, True), (1, 10000, False), (2, 10000, False), (4, 20000, False)]):
                    if i in (0, 3, 4, 5):
                        identity = configure(engine, workers, formulation)
                        m.emit(stream, dict(type='identity', formulation=formulation, identity=identity))
                        engine.send('ucinewgame'); engine.send('isready'); engine.until('readyok')
                    engine.send('position startpos'); engine.send('go nodes ' + str(cap)); lines = []
                    if stop:
                        lines += engine.until('info depth 2 ', 60); engine.send('stop')
                    lines += engine.until('bestmove ', 60)
                    result = m.b.inspect_result(m.b.chess.Board(), lines)
                    if result['nodes'] > cap or (not stop and result['nodes'] != cap): raise ValueError('Node accounting')
                    if stop and result['termination'] not in ('STOPPED', 'NODE_LIMIT'): raise ValueError('Stop dominance')
                    nodes += result['nodes']; requests += 1
                    m.emit(stream, dict(type='request', formulation=formulation, workers=workers,
                            measurement=dict(outcome=dict(nodes=result['nodes'])), result=result))
            finally:
                engine.close()
            if engine.process.returncode != 0: raise ValueError('UCI exit')
        m.emit(stream, dict(type='summary', status='passed', requests=requests, nodes=nodes, activeHelpersAfterClose=0))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--runtime', type=Path); parser.add_argument('--output', type=Path)
    parser.add_argument('--worker', action='store_true'); parser.add_argument('--protocol', type=Path)
    parser.add_argument('--directory', type=Path); args = parser.parse_args()
    if args.worker:
        p = json.loads(args.protocol.read_text(encoding='utf8'))
        ledger = json.loads((OUT / 'budget.json').read_text(encoding='utf8'))
        if p['campaign'] != 'CLV-R001' or not any(x['id'] == p['id'] for x in ledger['pending']):
            raise ValueError('Only the admitted CLV supervisor may start this worker')
        if p['kind'] != 'timed' or p['window'] not in ledger['authorizedIdleWindows']:
            raise ValueError('No recorded timed-window authority')
        m.command = command; m.configure = configure
        m.worker(args.protocol, args.directory)
    else:
        qualify(args.runtime, args.output)
