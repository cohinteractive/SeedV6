"""Explicit IC-R001 UCI binding, bounded qualification and existing paired-game worker.
The worker is called only by the IC native Job supervisor; it grants no idle window.
"""
import argparse
import importlib.util
import json
import os
from pathlib import Path
import sys

sys.dont_write_bytecode=True
spec=importlib.util.spec_from_file_location('ic_games',Path(__file__).with_name('p4-multiworker-match.py'))
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)


def command(runtime,formulation,actor):
    if formulation not in ('CONTROL','VOLATILITY_CARRYOVER'):raise ValueError('IC formulation')
    return [m.JAVA,'--add-modules=jdk.incubator.vector','-Xms128m','-Xmx512m','-XX:ActiveProcessorCount=4',
            '-Djava.awt.headless=true','-Dseedv6.syzygy.config=','-Dseedv6.syzygy.path=',
            '-Dseedv6.clockPolicy=BOUNDARY2','-Dseedv6.research.actor='+actor,'-cp',
            str(Path(runtime)/'classes')+os.pathsep+str(Path(runtime)/'lib/*'),
            'com.ohinteractive.seedv6.integration.P4IterationConfidenceUciMain',
            '--network',str(m.MODEL),'--formulation',formulation]


def configure(engine,workers,formulation):
    engine.send('uci');lines=engine.until('uciok',60)
    for name,value in [('Threads',workers),('Hash',64),('OwnBook','false')]:engine.send(f'setoption name {name} value {value}')
    engine.send('isready');lines+=engine.until('readyok',60);engine.send('uci');lines+=engine.until('uciok',60)
    identities=[x for x in lines if 'networkSha256=' in x]
    if not identities:raise ValueError('No identity')
    identity=identities[-1];active=workers==4 and formulation=='VOLATILITY_CARRYOVER'
    for token in ['flags='+str(1182007295 if workers==4 else 1177812991),'checkpoint=64','schema=S6CNU002','heads=8',
                  'networkSha256=8431626c2fca81e0a1e9ccd5e45ddd8829e6e3282c756c21fad75b06d075bb2b',
                  'scoreMapping=BT4_Q_LOGIT400_V1','clockPolicy=boundary2-d6-v1',
                  f'workers={workers};hashMiB=64;aspiration='+('VOLATILITY_CARRYOVER' if active else 'ADAPTIVE16'),
                  'formulation='+('IC-C1_VOLATILITY_CARRYOVER' if active else 'IC_CONTROL'),
                  'parallel='+('OWNER' if workers==1 else 'EAGER_ROOT' if workers==4 else 'ROOT_REUSE'),
                  'coreBuild=96','coreSource=44fc2bf2fe54','researchIdentity=IC-R001-v1']:
        if token not in identity:raise ValueError('Missing identity '+token+': '+identity)
    return identity


def qualify(runtime,output):
    nodes=requests=0
    with Path(output).open('x',encoding='utf-8') as stream:
        for formulation in ('CONTROL','VOLATILITY_CARRYOVER'):
            engine=m.b.Uci(command(runtime,formulation,'qualification'),Path(output).parent/(formulation+'-uci.txt'))
            try:
                m.emit(stream,dict(type='identity',formulation=formulation,identity=configure(engine,4,formulation)))
                for i,(workers,cap,stop) in enumerate([(4,20000,False),(4,100000,False),(4,100000,True),(1,10000,False),(2,10000,False),(4,20000,False)]):
                    if i in (0,3,4,5):
                        configure(engine,workers,formulation);engine.send('ucinewgame');engine.send('isready');engine.until('readyok')
                    engine.send('position startpos');engine.send('go nodes '+str(cap));lines=[]
                    if stop:lines+=engine.until('info depth 2 ',60);engine.send('stop')
                    lines+=engine.until('bestmove ',60);result=m.b.inspect_result(m.b.chess.Board(),lines)
                    if result['nodes']>cap or (not stop and result['nodes']!=cap):raise ValueError('Node cap/accounting')
                    if stop and result['termination'] not in ('STOPPED','NODE_LIMIT'):raise ValueError('Stop dominance')
                    nodes+=result['nodes'];requests+=1
                    m.emit(stream,dict(type='request',formulation=formulation,workers=workers,
                                      measurement=dict(outcome=dict(nodes=result['nodes'])),result=result))
            finally:engine.close()
            if engine.process.returncode!=0:raise ValueError('UCI exit')
        m.emit(stream,dict(type='summary',status='passed',requests=requests,nodes=nodes,activeHelpersAfterClose=0))


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--runtime');parser.add_argument('--output');parser.add_argument('--worker',action='store_true')
    parser.add_argument('--protocol',type=Path);parser.add_argument('--directory',type=Path)
    args=parser.parse_args()
    if args.worker:
        m.command=command;m.configure=configure
        m.worker(args.protocol,args.directory)
    else:qualify(args.runtime,args.output)
