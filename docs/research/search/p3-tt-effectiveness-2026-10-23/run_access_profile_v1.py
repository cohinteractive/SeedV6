"""Bounded read-only JFR observation of the unchanged ordinary UCI baseline."""
import ctypes
import importlib.util
import json
import os
import shutil
import subprocess
import time
import xml.etree.ElementTree as ET
from run_diagnostic_v1 import digest, write_new, ROOT, HERE

for workers in (1,4):
    assert json.loads((HERE/f'adaptive-clock-w{workers}-v1-execution.json').read_text())['status']=='passed'
spec=importlib.util.spec_from_file_location('baseline',ROOT/'tools/search-baseline.py')
b=importlib.util.module_from_spec(spec);spec.loader.exec_module(b)
spec=importlib.util.spec_from_file_location('native',ROOT/'tools/sr036-process-cost.py')
n=importlib.util.module_from_spec(spec);spec.loader.exec_module(n)
k=n.kernel;k.GetCurrentProcess.restype=ctypes.c_void_p
k.SetPriorityClass.argtypes=[ctypes.c_void_p,ctypes.c_ulong]
if not k.SetPriorityClass(k.GetCurrentProcess(),0x4000):raise ctypes.WinError(ctypes.get_last_error())
protocol=json.loads((HERE/'access-profile-v1.json').read_text())
runtime=HERE/protocol['runtime'];prior=json.loads((HERE/protocol['runtimeBinding']).read_text())
assert all(digest(p)==sha for p,sha in prior['files'].items())
settings=HERE/'raw/access-profile-v1.jfc';shutil.copyfile('C:/java/jdk-21/lib/jfr/profile.jfc',settings)
events=['jdk.ExecutionSample','jdk.NativeMethodSample','jdk.JavaMonitorEnter','jdk.ThreadPark']
settingsValues={e.attrib['name']:{s.attrib['name']:s.text for s in e} for e in ET.parse(settings).getroot().findall('event') if e.attrib['name'] in events}
jfr='C:/java/jdk-21/bin/jfr.exe';java='C:/java/jdk-21/bin/java.exe'
helpResult=subprocess.run([jfr,'help','print'],capture_output=True,text=True,check=True,timeout=60)
assert '--json' in helpResult.stdout and '--stack-depth' in helpResult.stdout
write_new(HERE/'access-profile-v1-binding.json',{'protocolSha256':digest(HERE/'access-profile-v1.json'),
    'runnerSha256':digest(__file__),'runtimeBindingSha256':digest(HERE/protocol['runtimeBinding']),
    'files':{str(p):digest(p) for p in (settings,ROOT/protocol['corpus'],ROOT/b.__file__,ROOT/n.__file__,ROOT/jfr,ROOT/java)},
    'runtimeFiles':prior['files'],'settings':settingsValues,'jfrHelp':helpResult.stdout})
roots=[]
for line in (ROOT/protocol['corpus']).read_text().splitlines()[:protocol['roots']]:
    identity,fen,moves=line.split('\t');board=b.chess.Board(fen)
    for move in moves.split():board.push_uci(move)
    roots.append((identity,fen,moves,board))
for workers in protocol['workers']:
    name=f'access-profile-w{workers}-v1';recording=HERE/'raw'/f'{name}.jfr';export=HERE/'raw'/f'{name}-events.json'
    transcript=HERE/'raw'/f'{name}-uci.txt';requestsPath=HERE/'raw'/f'{name}-requests.json'
    assert not any(p.exists() for p in (recording,export,transcript,requestsPath))
    command=[java,'--add-modules=jdk.incubator.vector','-Xms256m','-Xmx512m','-XX:ActiveProcessorCount=4',
        '-Djava.awt.headless=true',f'-XX:StartFlightRecording=filename={recording},settings={settings},disk=true,dumponexit=true',
        '-cp',str(runtime/'classes')+os.pathsep+str(runtime/'lib/*'),'com.ohinteractive.seedv6.uci.UciMain']
    receipt={'status':'running','command':command,'bindingSha256':digest(HERE/'access-profile-v1-binding.json'),
             'priority':'BelowNormal inherited','profileTimingNotPerformanceEvidence':True};requests=[];engine=None;started=time.monotonic()
    try:
        engine=b.Uci(command,transcript)
        engine.send('uci');identity=engine.until('uciok',90)
        assert any('baseline=construction-v1-cp64' in x and 'flags=262143' in x and 'aspiration=WIDEN16' in x for x in identity)
        receipt['identity']=identity
        engine.send('setoption name OwnBook value false');engine.send('setoption name Threads value '+str(workers))
        engine.send('setoption name Hash value 64');engine.send('isready');engine.until('readyok',60)
        for _ in range(2):
            engine.send('ucinewgame');engine.send('isready');engine.until('readyok')
            engine.send('position startpos');engine.send('go nodes 200000')
            assert b.inspect_result(b.chess.Board(),engine.until('bestmove ',60))['nodes']==200000
        for roundNumber in range(protocol['rounds']):
            for identity,fen,moves,board in roots:
                engine.send('ucinewgame');engine.send('isready');engine.until('readyok')
                engine.send('position fen '+fen+' moves '+moves)
                engine.send('go nodes '+str(protocol['nodesPerRequest']))
                result=b.inspect_result(board,engine.until('bestmove ',60));assert result['nodes']==protocol['nodesPerRequest']
                requests.append({'id':identity,'round':roundNumber,**result})
        engine.close();receipt['nativeLifetime']=n.snapshot(int(engine.process._handle));engine=None
        assert receipt['nativeLifetime']['exitCode']==0
        write_new(requestsPath,requests)
        with export.open('x',encoding='utf8') as stream:
            subprocess.run([jfr,'print','--json','--stack-depth','64','--events',','.join(events),str(recording)],
                           stdout=stream,stderr=subprocess.PIPE,text=True,check=True,timeout=120)
        assert all(digest(p)==sha for p,sha in prior['files'].items())
        receipt.update(status='passed',requests=len(requests),requestNodes=sum(r['nodes'] for r in requests),
                       requestElapsedNanos=sum(r['elapsedNanos'] for r in requests))
    except BaseException as error:
        receipt.update(status='failed',failure=repr(error));raise
    finally:
        if engine is not None:
            engine.close();receipt['nativeLifetime']=n.snapshot(int(engine.process._handle))
            if not requestsPath.exists():write_new(requestsPath,requests)
        receipt['wallSeconds']=time.monotonic()-started
        receipt['evidence']={str(p.relative_to(HERE)):digest(p) for p in (recording,export,transcript,requestsPath) if p.exists()}
        write_new(HERE/f'{name}-execution.json',receipt)
    print(name,'passed',receipt['wallSeconds'],flush=True)
