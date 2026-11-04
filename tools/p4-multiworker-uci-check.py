"""Bounded MW UCI identity, whole-request accounting, stop and reuse qualification."""
import importlib.util
import json
from pathlib import Path
import sys

sys.dont_write_bytecode=True
spec=importlib.util.spec_from_file_location('mw_uci_match',Path(__file__).with_name('p4-multiworker-match.py'))
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)


def main(runtime, output, architectures=('ROOT_REUSE','WHOLE_ROOT_RACE')):
    if not architectures or len(set(architectures))!=len(architectures) or any(
            a not in ('ROOT_REUSE','WHOLE_ROOT_RACE','WHOLE_ROOT_SHARING') for a in architectures):
        raise ValueError('Unknown/duplicate qualification architecture')
    nodes=0; requests=0
    with Path(output).open('x',encoding='utf-8') as stream:
        for architecture in architectures:
            engine=m.b.Uci(m.command(runtime,architecture,'qualification'),Path(output).parent/(architecture+'-uci.txt'))
            try:
                identity=m.configure(engine,4,architecture)
                m.emit(stream,dict(type='identity',architecture=architecture,identity=identity))
                for index,(cap,stop) in enumerate([(20000,False),(100000,False),(100000,True),(20000,False)]):
                    if index in (0,3):
                        engine.send('ucinewgame');engine.send('isready');engine.until('readyok')
                    engine.send('position startpos');engine.send('go nodes '+str(cap));lines=[]
                    if stop:
                        lines+=engine.until('info depth 2 ',60);engine.send('stop')
                    lines+=engine.until('bestmove ',60)
                    result=m.b.inspect_result(m.b.chess.Board(),lines)
                    if result['nodes']>cap or (not stop and result['nodes']!=cap):raise ValueError('Incorrect node accounting')
                    if stop and result['termination'] not in ('STOPPED','NODE_LIMIT'):raise ValueError('Incorrect stop')
                    nodes+=result['nodes'];requests+=1
                    m.emit(stream,dict(type='request',architecture=architecture,index=index,
                        measurement=dict(outcome=dict(nodes=result['nodes'])),result=result))
            finally:engine.close()
            if engine.process.returncode!=0:raise ValueError('Nonzero UCI exit')
        m.emit(stream,dict(type='summary',status='passed',nodes=nodes,requests=requests,activeHelpersAfterClose=0,
            scope='All selected JVMs exited normally after UCI quit; no surviving in-process helper is possible. Parent Job verifies descendants.'))


if __name__=='__main__':main(sys.argv[1],sys.argv[2],tuple(sys.argv[3].split(',')) if len(sys.argv)>3 else ('ROOT_REUSE','WHOLE_ROOT_RACE'))
