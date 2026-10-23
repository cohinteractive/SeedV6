"""SR-034 launcher/identity adapter over the existing owned paired-game infrastructure.

The underlying runner, accounting and legal/history audit are reused unchanged.
Protocols must bind this adapter AND p3-selectivity-match.py and its dependencies.
"""
import importlib.util
from pathlib import Path
import os

def module(name, path):
    spec=importlib.util.spec_from_file_location(name,path)
    result=importlib.util.module_from_spec(spec);spec.loader.exec_module(result);return result

m=module('time_match_base',Path(__file__).with_name('p3-selectivity-match.py'))
b=m.b
PROFILES=('baseline','half','stable3-half')

def command(args, side):
    profile=getattr(args,side+'_policy')
    if profile not in PROFILES:raise ValueError('Unknown SR-034 profile')
    runtime=Path(getattr(args,side)).resolve()
    return [str(Path(args.java).resolve()),'--add-modules=jdk.incubator.vector',
            '-Xms256m','-Xmx512m','-XX:ActiveProcessorCount=4','-Djava.awt.headless=true',
            '-Dseedv6.research.actor='+side,
            '-Dseedv6.time.warmupWorkers='+str(getattr(args,side+'_workers')),
            '-cp',str(runtime/'classes')+os.pathsep+str(runtime/'lib/*'),
            'com.ohinteractive.seedv6.integration.Cp64TimeUciMain',
            '--network',str(Path(args.network).resolve()),'--profile',profile]

def verify_identity(lines, workers, profile, hash_mib):
    identities=[x.removeprefix('info string ') for x in lines if x.startswith('info string mode=research ')]
    if not identities:raise ValueError('Missing research identity')
    identity=identities[-1]
    fields=[x.partition('=') for x in identity.split()]
    expected={'mode':'research','experimental':'true','profile':profile,'evaluator':'CONVENTIONAL_NNUE',
        'checkpoint':'64','schema':'S6CNU002','heads':'8','flags':'3407871',
        'networkSha256':m.NETWORK_SHA256,'payload':'5da0253642ab33b8e446b5b1c5764750be008cd3dbacc7cd1992a95bf4a881f6',
        'scoreMapping':'BT4_Q_LOGIT400_V1','warmupNodes':'200000','warmupWorkers':str(workers),
        'clockPolicy':'factual' if profile=='baseline' else profile+'-d6-v1',
        'hardBudget':'baseline-unchanged','book':'OFF','tablebase':'OFF'}
    for key,value in expected.items():
        if [v for k,sep,v in fields if k==key and sep]!=[value]:raise ValueError('Wrong identity '+key)
    configurations=[v for k,sep,v in fields if k=='configuration' and sep]
    expected_config=('flat-pvs-v1[QXRAY,LMR,LMP,EFFORT,COMPLETED_HINTS,CAPTURE_HISTORY,CONTINUATION,'
        'KILLERS,COUNTERMOVES,NOMINAL_HISTORY,BAD_TACTICAL_LAST,IMPROVING,CAUTION,CHECK_EXTENSION,'
        'RECAPTURE_EXTENSION,FUTILITY,REVERSE_FUTILITY,HISTORY_PROTECTION,NOMINAL_FUTILITY,VERIFIED_NULL];'
        f'futility=100d;reverse=200d;extensionCap=1;workers={workers};hashMiB={hash_mib};aspiration=ADAPTIVE16;'
        f'parallel={"OWNER" if workers==1 else "ROOT_REUSE"}')
    if configurations!=[expected_config]:raise ValueError('Wrong Search composition')
    return identity

def preflight(cmd,path,workers,profile,hash_mib=64):
    engine=b.Uci(cmd,path)
    try:
        engine.send('uci');engine.until('uciok',90)
        engine.send('setoption name OwnBook value false')
        engine.send('setoption name Hash value '+str(hash_mib))
        engine.send('setoption name Threads value '+str(workers))
        engine.send('isready');engine.until('readyok',90)
        engine.send('uci');identity=verify_identity(engine.until('uciok',90),workers,profile,hash_mib)
        requests=[]
        for _ in range(2):
            engine.send('ucinewgame');engine.send('isready');engine.until('readyok')
            engine.send('position startpos');engine.send('go nodes 100000')
            r=b.inspect_result(b.chess.Board(),engine.until('bestmove ',60))
            if r['nodes']!=100000 or r['termination']!='NODE_LIMIT':raise ValueError('Node mode changed')
            requests.append(r)
        return {'identity':identity,'requests':requests}
    finally:engine.close()

def run(args):
    m.command=command;m.preflight=preflight
    return m.run(args)
