"""Bounded real-UCI preservation audit against frozen SR-005 research v1.

Run outside clock experiments. Single-worker deterministic controls must agree;
multiworker execution is checked for legality, accounting and actual identity,
not falsely required to have identical scheduling-dependent node streams.
"""
import argparse
import importlib.util
from pathlib import Path
import re

spec = importlib.util.spec_from_file_location('baseline', Path(__file__).with_name('search-baseline.py'))
b = importlib.util.module_from_spec(spec)
spec.loader.exec_module(b)


def stable(result):
    info = result['finalInfo']
    if info:
        info = re.sub(r'\b(?:time|nps) \d+\s*', '', info)
    return {key: result.get(key) for key in ('nodes', 'bestmove', 'termination')} | {'info': info}


def hce(cmd, transcript):
    engine = b.Uci(cmd, transcript)
    results = []
    try:
        engine.send('uci'); hello = engine.until('uciok')
        assert not any('evaluator=BRN_PAIR2' in line for line in hello)
        engine.send('setoption name OwnBook value false')
        engine.send('setoption name Threads value 1')
        engine.send('isready'); engine.until('readyok')
        for fen in (b.chess.STARTING_FEN, '8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1'):
            engine.send('ucinewgame'); engine.send('position fen ' + fen)
            engine.send('go depth 4')
            lines = engine.until('bestmove ')
            board = b.chess.Board(fen); move = lines[-1].split()[1]
            assert b.chess.Move.from_uci(move) in board.legal_moves
            assert not any('search failed' in line for line in lines)
            infos = [line for line in lines if line.startswith('info depth ')]
            assert infos and infos[-1].startswith('info depth 4 ')
            for line in infos:
                pv = board.copy()
                for uci in line.split(' pv ', 1)[1].split():
                    parsed = b.chess.Move.from_uci(uci)
                    assert parsed in pv.legal_moves
                    pv.push(parsed)
            # Ordinary HCE has no explicit Pair-2 identity/request telemetry.
            # Compare the reported completed iteration, not invented request work.
            results.append({'bestmove': move, 'finalInfo': infos[-1],
                            'completionEvidence': 'depth 4 info followed by legal bestmove'})
        return results
    finally:
        engine.close()


def main(args):
    out = Path(args.out).resolve(); out.mkdir(parents=True, exist_ok=False)
    frozen = {str(Path(p).resolve()): b.digest(p) for p in (__file__, b.__file__, args.java)}
    for install in (args.baseline, args.candidate):
        frozen.update(b.installation_hashes(install))
    checkpoint = Path(args.store) / 'checkpoints' / b.CHECKPOINT
    assert b.digest(checkpoint / 'network.brn-pair2') == b.NETWORK_SHA256
    frozen.update({str(p.resolve()): b.digest(p) for p in checkpoint.iterdir() if p.is_file()})
    report = {'status': 'running', 'arguments': vars(args), 'filesBefore': frozen, 'controls': {}}
    controls = (
        ('lmp-off', 'production', 'production', 'off', 'off', 1),
        ('lmr-off', 'production', 'off', 'off', 'default', 1),
        ('static', 'static', 'off', 'off', 'default', 1),
        ('qsearch', 'unpruned', 'off', 'off', 'default', 1),
        ('aspiration', 'production', 'production', 'one16', 'default', 1),
        ('multiworker', 'production', 'production', 'off', 'default', 2),
    )
    controls = [(*row[:5], 'off', None, row[5]) for row in controls]
    controls += [('check-'+p,'production','production','off','default',p,None,1)
                 for p in ('boundary','giving','evasion','forced','leaf','repeat')]
    controls += [(p,'production','production','off',p,'off',None,1)
                 for p in ('history-protect','history-extra','history-replace','history-deep','history-gate','history-leaf')]
    controls += [('reuse-'+p,'production','production','off','default','off',p,1)
                 for p in ('observe','root','pv','score','effort','history','pvhistory')]
    try:
        comparisons = 0
        for name, leaves, lmr, aspiration, lmp, check, reuse, workers in controls:
            pair = {}
            for side in ('baseline', 'candidate'):
                cmd = b.command(args.java, getattr(args, side), args.store, leaves, lmr, aspiration, lmp, check,
                                reuse if reuse is not None else ('off' if side == 'baseline' else 'default'))
                pair[side] = b.preflight(cmd, out / (name + '-' + side + '.txt'), workers)
            if workers == 1:
                for old, new in zip(pair['baseline']['requests'], pair['candidate']['requests'], strict=True):
                    assert (old['fen'], old['budget']) == (new['fen'], new['budget'])
                    if old['budget'] in ('depth 4', 'nodes 20000'):
                        assert stable(old) == stable(new), (name, old, new)
                        comparisons += 1
            report['controls'][name] = pair
        pair = {}
        for side in ('baseline', 'candidate'):
            cmd = [str(Path(args.java).resolve()), '-Xms256m', '-Xmx512m', '-XX:ActiveProcessorCount=1',
                   '-Djava.awt.headless=true', '-Dseedv6.syzygy.config=', '-Dseedv6.syzygy.path=',
                   '-cp', str(Path(getattr(args, side)).resolve() / 'lib' / '*'), 'com.ohinteractive.seedv6.Main']
            pair[side] = hce(cmd, out / ('hce-' + side + '.txt'))
        assert [stable(r) for r in pair['baseline']] == [stable(r) for r in pair['candidate']]
        comparisons += len(pair['baseline']); report['controls']['hce'] = pair
        cmd = b.command(args.java,args.candidate,args.store,'production','production','off','default','off','default')
        engine = b.Uci(cmd,out/'worker-transitions.txt')
        transitions = []
        try:
            engine.send('uci'); hello = engine.until('uciok')
            assert any('reuse=effort ' in line and 'workers=1 ' in line for line in hello)
            engine.send('setoption name OwnBook value false')
            engine.send('isready'); engine.until('readyok')
            for workers in (1,2,1):
                if transitions:
                    engine.send('setoption name Threads value '+str(workers))
                    engine.send('isready'); ready=engine.until('readyok')
                    identities=[line for line in ready if line.startswith('info string evaluator=')]
                    assert len(identities)==1
                    identity=identities[0]
                    assert ('reuse='+('effort' if workers==1 else 'off')+' ') in identity
                    assert ('leaves='+('unpruned' if workers==1 else 'static')+' ') in identity
                    assert 'experimental=true' not in identity
                else: identity=next(line for line in hello if line.startswith('info string evaluator='))
                engine.send('ucinewgame');engine.send('position startpos');engine.send('go depth 4')
                result=b.inspect_result(b.chess.Board(),engine.until('bestmove '))
                transitions.append({'workers':workers,'identity':identity,'result':result})
            assert stable(transitions[0]['result'])==stable(transitions[2]['result'])
            comparisons+=1
            report['workerTransitions']=transitions
        finally: engine.close()
        report['invalidConfigurations']=b.failure_probes(cmd)
        assert all(b.digest(path) == value for path, value in frozen.items())
        report.update(status='passed', inputsStable=True, deterministicComparisons=comparisons)
    except BaseException as error:
        report.update(status='failed', failure=repr(error))
        raise
    finally:
        b.save(out / 'controls.json', report)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('baseline', 'candidate', 'store', 'java', 'out'):
        parser.add_argument('--' + name, required=True)
    main(parser.parse_args())
