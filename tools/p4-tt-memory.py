"""Phase 4 TT research: frozen runtimes, UCI bridge and owned paired matches.

Reuses the established independent PGN/history audit and Windows process ownership.
No production selection, historic-result parsing or promotion is performed.
"""
import argparse
import importlib.util
import json
import os
from pathlib import Path
import platform
import re
import shutil
import subprocess
import sys
import time

sys.dont_write_bytecode = True


def module(name, file):
    spec = importlib.util.spec_from_file_location(name, Path(__file__).with_name(file))
    result = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(result)
    return result


m = module('p4_match_support', 'p3-selectivity-null-match.py')
b = m.b
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'docs/research/search/p4-tt-memory'
JAVA = 'C:/java/jdk-21/bin/java.exe'
MODEL = ROOT / 'build-assets/nnue-candidate/conventional-cp64.cnnue'


def command(runtime, profile='control', workers=1, actor='diagnostic'):
    runtime = Path(runtime).resolve()
    result = [JAVA, '--add-modules=jdk.incubator.vector', '-Xms256m', '-Xmx512m',
              '-XX:ActiveProcessorCount=4', '-Djava.awt.headless=true',
              '-Dseedv6.syzygy.config=', '-Dseedv6.syzygy.path=',
              '-Dseedv6.clockPolicy=BOUNDARY2', '-Dseedv6.research.actor=' + actor,
              '-cp', str(runtime/'classes') + os.pathsep + str(runtime/'lib/*')]
    if profile == 'control':
        return result + ['com.ohinteractive.seedv6.uci.UciMain']
    return result + ['com.ohinteractive.seedv6.integration.P4TtUciMain',
                     '--network', str(MODEL), '--profile', profile]


def configure(engine, workers, hash_mib=64, profile='control'):
    engine.send('uci'); lines = engine.until('uciok', 90)
    for name, value in [('OwnBook', 'false'), ('Hash', hash_mib), ('Threads', workers)]:
        engine.send(f'setoption name {name} value {value}')
    engine.send('isready'); lines += engine.until('readyok', 90)
    # Production intentionally suppresses announcements on resource changes.
    # A fresh UCI identification reports the selected worker/hash configuration.
    engine.send('uci'); lines += engine.until('uciok', 90)
    identities = [s for s in lines if 'networkSha256=' + m.NETWORK_SHA256 in s]
    if not identities:
        raise ValueError('Missing frozen CP64 runtime identity')
    identity = identities[-1]
    for token in ('checkpoint=64', 'schema=S6CNU002', 'heads=8',
                  'scoreMapping=BT4_Q_LOGIT400_V1', 'aspiration=ADAPTIVE16',
                  f'workers={workers};hashMiB={hash_mib}', 'clockPolicy=boundary2-d6-v1'):
        if token not in identity:
            raise ValueError('Missing identity ' + token + ': ' + identity)
    if profile == 'control' and 'flags=3407871' not in identity:
        raise ValueError('Incorrect control flags')
    if profile == 'h1' and ('flags=7602175' not in identity or 'ttPolicy=H1-current-sufficient-nonpv-normal' not in identity):
        raise ValueError('Incorrect H1 flags/policy')
    return identity


def preflight(cmd, path, workers, profile, hash_mib=64):
    engine = b.Uci(cmd, path)
    try:
        identity = configure(engine, workers, hash_mib, profile)
        requests = []
        for _ in range(2):
            engine.send('ucinewgame'); engine.send('isready'); engine.until('readyok')
            engine.send('position startpos'); engine.send('go nodes 20000')
            r = b.inspect_result(b.chess.Board(), engine.until('bestmove ', 60))
            if r['nodes'] != 20000:
                raise ValueError('Whole-request node limit')
            requests.append(r)
        return {'identity': identity, 'requests': requests,
                'warmup': 'Preflight is a separate process; match processes start cold symmetrically.'}
    finally:
        engine.close()


def freeze(args):
    source = Path(args.install).resolve(); target = OUT/'raw'/args.name
    if target.exists():
        raise ValueError('Never overwrite a frozen runtime')
    shutil.copytree(source, target)
    report = {'sourceCommit': args.commit, 'runtimeSourceNote': args.note,
              'installation': str(target), 'files': b.installation_hashes(target),
              'java': JAVA, 'javaSha256': b.digest(JAVA),
              'javaVersion': subprocess.run([JAVA, '-version'], capture_output=True, text=True, check=True).stderr,
              'modelSha256': b.digest(MODEL), 'platform': platform.platform(),
              'hardware': 'Ryzen 5 5500; 6 physical / 12 logical; 32 GiB installed',
              'hashMiB': 64, 'ttEntries': 2097152, 'ttEntryPayloadBytes': 50331648,
              'layout': 'three long arrays, direct mapped, 32 synchronized stripes; overhead excluded'}
    b.save(OUT/(args.name+'-manifest.json'), report)


def stable(r):
    info = r['finalInfo'] or ''
    info = re.sub(r'\b(time|nps) \d+ ?', '', info)
    return r['bestmove'], r['nodes'], r['termination'], info


def bridge(args):
    out = OUT/'raw'/args.name; out.mkdir(parents=True, exist_ok=False)
    openings = OUT/'bridge-v1-openings.pgn'
    prefixes = b.opening_prefixes(openings, 32)
    positions = [('startpos', b.chess.Board())]
    for moves in prefixes[:8]:
        board = b.chess.Board()
        for move in moves: board.push_uci(move)
        positions.append(('startpos moves ' + ' '.join(moves), board))
    for fen in ('7k/6Q1/5K2/8/8/8/8/8 b - - 0 1',
                '7k/5K2/6Q1/8/8/8/8/8 b - - 0 1'):
        positions.append(('fen ' + fen, b.chess.Board(fen)))
    report = {'status': 'running', 'rows': [], 'identities': {},
              'protocol': 'bridge-v1-protocol.json', 'arguments': vars(args)}
    try:
        for workers in (1, 4):
            for side in ('runtime79', 'runtime80'):
                runtime = OUT/'raw'/side
                manifest = json.loads((OUT/(side+'-manifest.json')).read_text())
                if any(b.digest(p) != h for p, h in manifest['files'].items()):
                    raise ValueError('Frozen runtime changed')
                engine = b.Uci(command(runtime), out/f'{side}-w{workers}.txt')
                try:
                    report['identities'][f'{side}-w{workers}'] = configure(engine, workers)
                    for index, (position, board) in enumerate(positions):
                        for repeat in range(3 if index < 9 else 1):
                            engine.send('ucinewgame'); engine.send('isready'); engine.until('readyok')
                            engine.send('position ' + position); engine.send('go depth 5')
                            r = b.inspect_result(board, engine.until('bestmove ', 120))
                            report['rows'].append(dict(side=side, workers=workers, index=index,
                                repeat=repeat, request='depth 5', result=r))
                    for request in ('nodes 20000', 'nodes 100000', 'movetime 100',
                                    'wtime 1000 btime 1000 winc 50 binc 50', 'infinite'):
                        engine.send('ucinewgame'); engine.send('isready'); engine.until('readyok')
                        engine.send('position startpos'); engine.send('go ' + request)
                        if request == 'infinite':
                            engine.until('info depth 2 '); engine.send('stop')
                        r = b.inspect_result(b.chess.Board(), engine.until('bestmove ', 60))
                        if request.startswith('nodes ') and r['nodes'] != int(request.split()[1]):
                            raise ValueError('Node limit not honored')
                        report['rows'].append(dict(side=side, workers=workers, request=request, result=r))
                finally:
                    engine.close()
                b.save(out/'observations.json', report)
        comparisons = []
        for w in (1, 4):
            for i in range(9):
                rows = {s: [r['result'] for r in report['rows'] if r['side']==s and
                        r['workers']==w and r.get('index')==i] for s in ('runtime79','runtime80')}
                comparisons.append({'workers': w, 'position': i,
                    'within79Stable': len({str(stable(r)) for r in rows['runtime79']})==1,
                    'within80Stable': len({str(stable(r)) for r in rows['runtime80']})==1,
                    'firstCompleteIdentityEqual': stable(rows['runtime79'][0])==stable(rows['runtime80'][0]),
                    'nodes79': [r['nodes'] for r in rows['runtime79']],
                    'nodes80': [r['nodes'] for r in rows['runtime80']]})
        report['status'] = 'passed'
        b.save(OUT/(args.name+'-summary.json'), {'status':'passed', 'requests':len(report['rows']),
              'comparisons':comparisons, 'identities':report['identities'],
              'raw':str(out/'observations.json'), 'interpretation':'Behavioral diagnostics, not strength/equivalence proof.'})
    except BaseException as error:
        report.update(status='failed', failure=repr(error)); raise
    finally:
        b.save(out/'observations.json', report)


def match(args):
    # Same suspended-before-assignment Windows Job ownership and independent audit
    # as the accepted adapter. Trace logging retains the exact failing request if
    # strict Fastchess rejects missing information or another protocol warning.
    out = OUT/'raw'/args.name; out.mkdir(parents=True, exist_ok=False)
    runner = ROOT/'build/research/tools/fastchess.exe'
    report = {'status':'running', 'arguments':vars(args), 'platform':platform.platform(),
              'logicalCpus':os.cpu_count(), 'model':b.digest(MODEL),
              'hardware':'Ryzen 5 5500, 6C/12T, 32 GiB; one active game unless specified',
              'javaVersion':subprocess.run([JAVA,'-version'],capture_output=True,text=True,check=True).stderr}
    frozen = {str(Path(p).resolve()):b.digest(p) for p in
              [__file__, m.__file__, b.__file__, m.c.__file__, m.c.n.__file__, m.owned.__file__,
               runner, JAVA, MODEL, args.openings, OUT/args.protocol]}
    for side in ('baseline','candidate'): frozen.update(b.installation_hashes(getattr(args,side)))
    report['filesBefore']=frozen
    commands={side:command(getattr(args,side),getattr(args,side+'_policy'),args.workers,side)
              for side in ('baseline','candidate')}
    report['commands']=commands; started=time.monotonic()
    try:
        if b.digest(MODEL)!=m.NETWORK_SHA256: raise ValueError('Frozen CP64 mismatch')
        for side,cmd in commands.items():
            report[side+'Preflight']=preflight(cmd,out/(side+'-uci.txt'),args.workers,getattr(args,side+'_policy'))
        cmd=[str(runner)]
        for side,engine in commands.items():
            cmd+=['-engine','name='+side,'cmd='+engine[0],'args='+subprocess.list2cmdline(engine[1:]),
                  'option.Threads='+str(args.workers)]
        cmd+=['-each','proto=uci','tc='+args.tc,'timemargin=0','restart=off','option.OwnBook=false','option.Hash=64',
              '-rounds',str(args.pairs),'-repeat','-concurrency',str(args.concurrency),'-srand',str(args.seed),
              '-openings','file='+str(Path(args.openings).resolve()),'format=pgn','order=sequential',
              '-pgnout','file='+str(out/'games.pgn'),'nodes=true','nps=true','pv=true','timeleft=true','latency=true',
              'match_line=^info string request ','-report','penta=true','-ratinginterval','2','-autosaveinterval','0',
              '-strict','-log','file='+str(out/'uci-trace.log'),'level=trace','append=false','compress=false','realtime=false','engine=true']
        report['runnerCommand']=cmd;b.save(out/'run.json',report);match_started=time.monotonic()
        with (out/'runner.txt').open('w',encoding='utf-8') as log:
            job=m.owned.OwnedJob();process=None;cohort=None
            try:
                process=subprocess.Popen(cmd,cwd=out,env=b.clean_environment(),stdout=log,
                    stderr=subprocess.STDOUT,creationflags=0x00000004|0x00004000)
                job.assign(process);job.resume_initial_thread(process)
                cohort=m.MemoryCohort(process.pid,args.concurrency,out/'process-cost.json');cohort.attach()
                code=process.wait(timeout=args.timeout)
            except BaseException:
                job.close()
                if process is not None and process.poll() is None: process.kill();process.wait(timeout=10)
                if cohort is not None:cohort.wait_for_exit()
                raise
            finally:
                try:
                    if cohort is not None:cohort.close()
                finally:job.close()
        report['matchWallSeconds']=time.monotonic()-match_started;report['runnerExitCode']=code
        report['results']=b.analyze(out/'games.pgn',args.openings,args.pairs,require_accounting=True,
                                    unordered=args.concurrency>1)
        if code!=0 or report['results']['terminations']!={'normal':args.pairs*2}:
            raise ValueError('Incomplete or operationally failed strict match')
        if any(b.digest(p)!=h for p,h in frozen.items()):raise ValueError('Frozen inputs changed')
        report.update(status='passed',inputsStable=True)
    except BaseException as error:
        report.update(status='failed',failure=repr(error));raise
    finally:
        report['wallSeconds']=time.monotonic()-started;b.save(out/'run.json',report)
    summary = {k:v for k,v in report.items() if k not in ('filesBefore','results')}
    summary['results'] = {k:v for k,v in report['results'].items() if k != 'games'}
    summary['raw'] = str(out)
    summary['adapterSha256'] = frozen[str(Path(__file__).resolve())]
    summary['interpretation'] = 'Exploratory fixed-pair screen unless separately prospectively designated; no acceptance.'
    b.save(OUT/(args.name+'-summary.json'), summary)


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__); sub = p.add_subparsers(dest='mode', required=True)
    f = sub.add_parser('freeze')
    for x in ('install','name','commit','note'): f.add_argument('--'+x, required=True)
    f.set_defaults(func=freeze)
    d = sub.add_parser('bridge'); d.add_argument('--name', default='bridge-diagnostics-v1'); d.set_defaults(func=bridge)
    s = sub.add_parser('select'); s.add_argument('--book',required=True); s.add_argument('--name',required=True)
    s.add_argument('--pairs',type=int,required=True); s.add_argument('--seed',type=int,required=True)
    s.add_argument('--partition',type=int,default=0)
    s.set_defaults(func=lambda a: b.select_openings(a.book, OUT/(a.name+'-openings.pgn'),a.pairs,a.seed,a.partition))
    r = sub.add_parser('match')
    for x in ('name','baseline','candidate','openings'): r.add_argument('--'+x,required=True)
    for side in ('baseline','candidate'): r.add_argument('--'+side+'-policy',default='control')
    r.add_argument('--workers',type=int,choices=(1,4),required=True)
    r.add_argument('--pairs',type=int,required=True); r.add_argument('--tc',default='3+0.03')
    r.add_argument('--protocol',default='bridge-v1-protocol.json')
    r.add_argument('--seed',type=int,default=20261030); r.add_argument('--concurrency',type=int,default=1)
    r.add_argument('--timeout',type=int,default=7200); r.set_defaults(func=match)
    args = p.parse_args()
    function = args.func; del args.func; function(args)
