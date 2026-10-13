"""Actual UCI worker transitions, stop/reuse and supported-control lifecycle checks."""
import argparse
import importlib.util
import json
from pathlib import Path
import time

spec=importlib.util.spec_from_file_location('match',Path(__file__).with_name('sr036-match.py'))
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);b=m.b


def main(a):
    out=Path(a.out);out.mkdir(parents=True,exist_ok=False)
    report={'status':'running','arguments':vars(a),'observations':[]}
    frozen={str(Path(p).resolve()):b.digest(p) for p in (__file__,m.__file__,b.__file__,a.java)}
    frozen.update(b.installation_hashes(a.install))
    model=Path(a.store)/'checkpoints'/b.CHECKPOINT
    frozen.update({str(p.resolve()):b.digest(p) for p in model.iterdir() if p.is_file()})
    assert b.digest(model/'network.brn-pair2')==b.NETWORK_SHA256
    report['filesBefore']=frozen
    try:
        for policy in ('off','private','cooperative','plain','owner','offset','mixed','root_split','root_full','root_reuse'):
            cmd=b.command(a.java,a.install,a.store,'production','production','off','default','off','default')
            cmd[3]='-XX:ActiveProcessorCount=6';cmd[4:4]=['-Dseedv6.uci.concurrencyResearch='+policy]
            engine=b.Uci(cmd,out/(policy+'.txt'))
            try:
                engine.send('uci');engine.until('uciok',60);engine.send('setoption name OwnBook value false')
                for workers in (1,4,1,6,2,16,1):
                    engine.send('setoption name Threads value '+str(workers));engine.send('isready');ready=engine.until('readyok',60)
                    identity=[x for x in ready if x.startswith('info string evaluator=')][-1]
                    assert 'workers='+str(workers) in identity and 'sha256='+b.NETWORK_SHA256 in identity
                    for position in ('position startpos','position startpos moves g1f3 g8f6 f3g1 f6g8',
                                     'position fen 7k/8/8/3pP3/8/8/8/K7 w - d6 0 1'):
                        board=b.chess.Board(position.split('fen ',1)[1]) if ' fen ' in position else b.chess.Board()
                        if ' moves ' in position:
                            for move in position.split(' moves ',1)[1].split():board.push_uci(move)
                        engine.send('ucinewgame');engine.send('isready');engine.until('readyok')
                        engine.send(position);engine.send('go nodes 5000')
                        result=b.inspect_result(board,engine.until('bestmove ',60));assert result['nodes']<=5000
                        report['observations'].append({'policy':policy,'workers':workers,'kind':'nodes',**result})
                    engine.send('position startpos');engine.send('go infinite')
                    time.sleep(.03);started=time.monotonic();engine.send('stop')
                    lines=engine.until('bestmove ',60)
                    if lines[-1]=='bestmove 0000':
                        # A stop before any completed iteration may honestly have no move.
                        assert not any(x.startswith('info depth ') for x in lines)
                        requests=[x for x in lines if x.startswith('info string request ')]
                        assert len(requests)==1
                        result=dict(x.split('=',1) for x in requests[0].split()[3:])
                        assert result['termination']=='STOPPED'
                        result.update(bestmove='0000',finalInfo=None)
                    else: result=b.inspect_result(b.chess.Board(),lines)
                    report['observations'].append({'policy':policy,'workers':workers,'kind':'stop','stopRoundTripSeconds':time.monotonic()-started,**result})
                # Close with live work; UCI must own cancellation/draining.
                engine.send('position startpos');engine.send('go infinite');time.sleep(.02)
            finally:engine.close()
            assert engine.process.returncode==0 and not engine.errors
        assert all(b.digest(p)==h for p,h in frozen.items())
        report.update(status='passed',inputsStable=True,requests=len(report['observations']))
    except BaseException as error:
        report.update(status='failed',failure=repr(error));raise
    finally:b.save(out/'lifecycle.json',report)


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    for key in ('install','store','out'):p.add_argument('--'+key,required=True)
    p.add_argument('--java',default='C:/java/jdk-21/bin/java.exe')
    main(p.parse_args())
