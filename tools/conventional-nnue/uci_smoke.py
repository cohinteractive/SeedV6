"""Bounded real UCI lifecycle check with independent python-chess move validation."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import queue
import subprocess
import threading
import time
import chess


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--network',type=Path)
    parser.add_argument('--workers',type=int,choices=(1,2),default=1)
    parser.add_argument('--output',type=Path,required=True)
    parser.add_argument('--selective-research-classes',type=Path)
    parser.add_argument('--runtime-lib',type=Path,help='Frozen UCI jar directory; default is the installed application')
    parser.add_argument('--vector',action='store_true',help='Explicitly enable the optional Java Vector API module')
    args=parser.parse_args();args.output.mkdir(parents=True,exist_ok=False)
    root=Path(__file__).resolve().parents[2]
    libs=(args.runtime_lib or root/'app-uci/build/install/seedv6-uci/lib').resolve()
    jars=sorted(libs.glob('*.jar'))
    if not jars:raise ValueError('UCI runtime directory contains no jars')
    jar_hashes={jar.name:hashlib.sha256(jar.read_bytes()).hexdigest() for jar in jars}
    command=['C:/java/jdk-21/bin/java.exe','-XX:ActiveProcessorCount=2','-Xmx768m','-Djava.awt.headless=true',
        '-cp',str(libs/'*'),'com.ohinteractive.seedv6.uci.UciMain']
    command+=['--nnue',str(args.network.resolve())] if args.network else ['--model',str(root/'build-assets/production/bre-pair2-g32.brn-pair2')]
    if args.selective_research_classes:
        if not args.network or args.workers!=1:raise ValueError('Selective research needs network and one worker')
        command=command[:5]+[str(libs/'*')+os.pathsep+str(args.selective_research_classes.resolve()),
            'SeedNnueSelectiveResearch',str(args.network.resolve())]
    if args.vector:command.insert(1,'--add-modules=jdk.incubator.vector')
    flags=(subprocess.CREATE_NO_WINDOW|subprocess.BELOW_NORMAL_PRIORITY_CLASS) if hasattr(subprocess,'CREATE_NO_WINDOW') else 0
    p=subprocess.Popen(command,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=True,creationflags=flags)
    q=queue.Queue();transcript=[];results=[]
    def collect(stream,kind):
        for line in stream:
            value=line.rstrip();transcript.append([kind,value]);q.put((kind,value))
    for stream,kind in ((p.stdout,'out'),(p.stderr,'err')):threading.Thread(target=collect,args=(stream,kind),daemon=True).start()
    def send(line):
        transcript.append(['in',line]);p.stdin.write(line+'\n');p.stdin.flush()
    def until(prefix,timeout=15):
        deadline=time.monotonic()+timeout;seen=[]
        while time.monotonic()<deadline:
            try:kind,line=q.get(timeout=.5)
            except queue.Empty:continue
            seen.append(line)
            if kind=='out' and line.startswith(prefix):return line,seen
        raise RuntimeError('UCI timeout: '+prefix)
    try:
        send('uci');_,intro=until('uciok')
        identity='evaluator=CONVENTIONAL_NNUE' if args.network else 'evaluator=BRN_PAIR2'
        assert any(identity in line for line in intro),intro
        for line in (f'setoption name Threads value {args.workers}','setoption name Hash value 32',
            'setoption name Ponder value false','setoption name UseBook value false','isready'):send(line)
        until('readyok')
        positions=[chess.STARTING_FEN,
            'r3k2r/pppq1ppp/2n1bn2/3pp3/3PP3/2N1BN2/PPPQ1PPP/R3K2R w KQkq - 0 1',
            '4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1',
            '4k3/P7/8/8/8/8/7p/4K3 b - - 0 1']
        for fen in positions:
            send('ucinewgame');send('position fen '+fen);send('go movetime 300')
            best,lines=until('bestmove ');move=best.split()[1]
            assert chess.Move.from_uci(move) in chess.Board(fen).legal_moves,(fen,move)
            results.append(dict(fen=fen,bestmove=move,last_info=next((x for x in reversed(lines) if x.startswith('info depth')),None)))
        send('position startpos');send('go infinite');time.sleep(.15);send('stop');until('bestmove ')
        send('isready');until('readyok');send('ucinewgame');send('position startpos');send('go depth 2');until('bestmove ')
        send('quit');assert p.wait(timeout=10)==0
        assert not any('exception' in line.lower() or 'error' in line.lower() for kind,line in transcript if kind=='err')
        summary=dict(status='PASS',workers=args.workers,network=str(args.network),identity=intro,
            searches=results,lifecycle='stop/readiness/newgame/repeated search/quit passed',command=command,
            runtime_lib=str(libs),jar_sha256=jar_hashes)
        (args.output/'summary.json').write_text(json.dumps(summary,indent=2)+'\n');print(json.dumps(summary,indent=2))
    finally:
        if p.poll() is None:p.terminate();p.wait(timeout=10)
        (args.output/'transcript.json').write_text(json.dumps(transcript,indent=2)+'\n')


if __name__=='__main__':main()
