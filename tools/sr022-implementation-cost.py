"""Rotated cross-binary SR-022 cost/parity experiment; never playing-strength evidence.

Both persistent JVMs run the same policy and full supplied histories. Three 1M-node
warmups per JVM precede two rotated rounds. Engine whole-request time and UCI round
trip are distinct; new-game preparation is outside both. No diagnostic callback.
"""
import argparse
import importlib.util
import json
from pathlib import Path
import time

spec = importlib.util.spec_from_file_location('sr022_bridge', Path(__file__).with_name('verify-sr022-bridge.py'))
bridge = importlib.util.module_from_spec(spec); spec.loader.exec_module(bridge)
b = bridge.b


def positions(path):
    result = []
    for line in Path(path).read_text(encoding='utf-8').splitlines():
        if not line or line.startswith('#'): continue
        label, fen, *suffix = line.split('\t')
        moves = suffix[0].split() if suffix else []
        board = b.chess.Board(fen)
        for move in moves: board.push_uci(move)
        result.append((label, board, 'position fen '+fen+(' moves '+' '.join(moves) if moves else '')))
    return result


def request(engine, board, position, budget):
    engine.send('ucinewgame'); engine.send('isready'); engine.until('readyok')
    engine.send(position)
    started = time.perf_counter_ns()
    engine.send('go '+budget)
    lines = engine.until('bestmove ', timeout=60)
    roundtrip = time.perf_counter_ns()-started
    return b.inspect_result(board, lines) | {'uciRoundTripNanos': roundtrip}


def main(args):
    out = Path(args.out).resolve(); out.mkdir(parents=True, exist_ok=False)
    rows = positions(args.corpus)
    report = {'status':'running', 'arguments':vars(args), 'identities':{}, 'commands':{}}
    frozen = {str(Path(p).resolve()):b.digest(p) for p in
              (__file__, bridge.__file__, b.__file__, args.java, args.corpus)}
    for install in (args.control, args.candidate): frozen.update(b.installation_hashes(install))
    network = Path(args.store)/'checkpoints'/b.CHECKPOINT/'network.brn-pair2'
    assert b.digest(network) == b.NETWORK_SHA256
    frozen.update({str(p.resolve()):b.digest(p) for p in network.parent.iterdir() if p.is_file()})
    report['filesBefore'] = frozen
    engines = {}
    observations = []
    try:
        for name, install in (('control',args.control),('candidate',args.candidate)):
            cmd = b.command(args.java,install,args.store,'production','production','off',args.lmp)
            report['commands'][name] = cmd
            engine = engines[name] = b.Uci(cmd,out/(name+'-uci.txt'))
            engine.send('uci'); identity_lines = engine.until('uciok')
            engine.send('setoption name OwnBook value false')
            engine.send('setoption name Threads value 1')
            engine.send('isready'); identity_lines += engine.until('readyok')
            identity = [x for x in identity_lines if x.startswith('info string evaluator=')][-1]
            b.check_lmp_identity(identity,args.lmp)
            for fact in ('checkpoint='+b.CHECKPOINT,'sha256='+b.NETWORK_SHA256,
                         'workers=1','leaves=unpruned','lmr=quiet','aspiration=off'):
                assert fact in identity, (name,fact,identity)
            report['identities'][name] = identity
            for _ in range(3):
                assert request(engine,b.chess.Board(),'position startpos','nodes 1000000')['nodes'] == 1_000_000
        b.save(out/'cost.json',report)
        with (out/'observations.jsonl').open('x',encoding='utf-8',newline='\n') as stream:
            comparisons = 0
            for round_index in range(2):
                for index,(label,board,position) in enumerate(rows):
                    names = ('control','candidate') if (round_index+index)%2 == 0 else ('candidate','control')
                    for budget in ('depth 6','nodes 100000','nodes 1000000'):
                        pair = {}
                        for name in names:
                            value = request(engines[name],board,position,budget)
                            pair[name] = value
                            row = {'round':round_index,'id':label,'position':position,'budget':budget,'engine':name,**value}
                            observations.append(row); stream.write(json.dumps(row)+'\n'); stream.flush()
                        assert bridge.stable(pair['control']) == bridge.stable(pair['candidate']), (label,budget,pair)
                        comparisons += 1
            stream.write(json.dumps({'observationsComplete':True,'observations':len(observations),'deterministicComparisons':comparisons})+'\n')
        summary = {}
        for budget in ('depth 6','nodes 100000','nodes 1000000'):
            summary[budget] = {}
            for name in engines:
                selected = [x for x in observations if x['budget']==budget and x['engine']==name]
                summary[budget][name] = {key:sum(x[key] for x in selected)
                                        for key in ('nodes','elapsedNanos','uciRoundTripNanos')}
            summary[budget]['elapsedRatio'] = summary[budget]['candidate']['elapsedNanos']/summary[budget]['control']['elapsedNanos']
        assert all(b.digest(path)==value for path,value in frozen.items())
        report.update(status='passed',inputsStable=True,deterministicComparisons=comparisons,summary=summary)
    except BaseException as error:
        report.update(status='failed',failure=repr(error)); raise
    finally:
        cleanup_errors = []
        for engine in engines.values():
            try: engine.close()
            except BaseException as error: cleanup_errors.append(repr(error))
        if cleanup_errors: report.update(status='failed',cleanupErrors=cleanup_errors)
        b.save(out/'cost.json',report)
        if cleanup_errors: raise RuntimeError('Owned engine cleanup failed: '+repr(cleanup_errors))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('control','candidate','store','java','corpus','out'): parser.add_argument('--'+name,required=True)
    parser.add_argument('--lmp',default='window',choices=('off','shallow','lmr','combined','guarded','d2','window'))
    main(parser.parse_args())
