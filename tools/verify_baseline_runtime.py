"""Compare the real GUI adapter and ordinary UCI at fixed depth using relocated runtime jars."""
import argparse
import json
import os
from pathlib import Path
import re
import subprocess
import sys
from native_package_checks import Engine, check, VECTOR_WARNING

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT/'tools/conventional-nnue'))
import compute_gate

FENS = [
    'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1',
    'r3k2r/pppq1ppp/2n1bn2/3pp3/3PP3/2N1BN2/PPPQ1PPP/R3K2R w KQkq - 0 1',
    '4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1',
    '7k/P7/8/8/8/8/8/7K w - - 0 1',
]

def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--java', type=Path, required=True)
    p.add_argument('--gui-java', type=Path)
    p.add_argument('--gui-lib', type=Path, required=True)
    p.add_argument('--uci-lib', type=Path, required=True)
    p.add_argument('--gui-classes', type=Path, default=ROOT/'app-gui/build/classes/java/test')
    p.add_argument('--output', type=Path, required=True)
    a = p.parse_args(); compute_gate.require(); a.output.mkdir(parents=True, exist_ok=False)
    env = {k:v for k,v in os.environ.items() if k not in ('JAVA_TOOL_OPTIONS','_JAVA_OPTIONS','JDK_JAVA_OPTIONS','CLASSPATH','JAVA_HOME')}
    gui_jars = [jar.resolve() for jar in a.gui_lib.glob('*.jar') if jar.name not in ('app.jar','app-uci.jar')]
    gui_cp = os.pathsep.join(map(str,[a.gui_classes.resolve(), *gui_jars]))
    report = dict(status='PASS', results=[], uci_sessions=[], gui_jars=[j.name for j in gui_jars])
    previous = None
    for mode, opts in [('vector',['--add-modules=jdk.incubator.vector']), ('scalar',[])]:
        signatures=[]
        engine = Engine([a.java.resolve(),*opts,'-Xmx768m','-cp',str(a.uci_lib.resolve()/'*'),
                         'com.ohinteractive.seedv6.uci.UciMain'],a.output.resolve(),env,(VECTOR_WARNING,))
        try:
            engine.send('uci'); identity='\n'.join(engine.until('uciok'))
            check('search=construction-v1' in identity and 'schema=S6CNU002 heads=8' in identity,identity)
            check(('head=vector-' if mode=='vector' else 'head=scalar-long') in identity,identity)
            engine.send('setoption name Threads value 1\nsetoption name Hash value 64\nsetoption name UseBook value false\nisready')
            engine.until('readyok')
            for fen in FENS:
                engine.send('ucinewgame\nposition fen '+fen+'\ngo depth 5')
                lines=engine.until('bestmove ',timeout=45)
                info=next(line for line in reversed(lines) if line.startswith('info depth 5 '))
                signature=dict(score=int(re.search(r'score cp (-?\d+)',info)[1]),
                    nodes=int(re.search(r' nodes (\d+)',info)[1]),move=lines[-1].split()[1],pv=info.split(' pv ')[1])
                gui=subprocess.run([str((a.gui_java or a.java).resolve()),*opts,'-Djava.awt.headless=true','-Xmx768m',
                    '-cp',gui_cp,'com.ohinteractive.seedv6.gui.ConventionalGuiProbe',fen],cwd=a.output,
                    env=env,text=True,capture_output=True,timeout=45,check=True)
                match=re.search(r'RESULT score=(-?\d+) nodes=(\d+) move=(\S+) pv=(.*)',gui.stdout)
                check(match is not None,gui.stdout+gui.stderr)
                expected=dict(score=int(match[1]),nodes=int(match[2]),move=match[3],pv=match[4])
                check(signature==expected,f'GUI/UCI disagreement {mode} {fen}: {signature} != {expected}')
                report['results'].append(dict(mode=mode,fen=fen,signature=signature,gui=gui.stdout))
                signatures.append(signature)
            # Search terminal values are independent of static NNUE scores.
            for fen, score in [('7k/6Q1/6K1/8/8/8/8/8 b - - 0 1','mate 0'),
                               ('7k/5Q2/6K1/8/8/8/8/8 b - - 0 1','cp 0'),
                               ('7k/5Q2/6K1/8/8/8/8/8 w - - 0 1','mate 1')]:
                engine.send('ucinewgame\nposition fen '+fen+'\ngo depth 3')
                check(any('score '+score in line for line in engine.until('bestmove ')), 'Terminal score mismatch')
            engine.close()
            report['uci_sessions'].append(dict(mode=mode,sent=engine.sent,stdout=engine.lines,stderr=engine.errors))
        finally: engine.kill()
        if previous is not None: check(previous==signatures,'Scalar/Vector runtime mismatch')
        previous=signatures
    (a.output/'result.json').write_text(json.dumps(report,indent=2)+'\n')
    print('PASS: eight exact GUI/UCI/scalar/Vector depth-5 comparisons; six terminal/mate UCI searches')

if __name__=='__main__':main()
