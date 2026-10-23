"""Qualify the frozen explicit profiles and ordinary baseline against the same source/model."""
import ctypes
import importlib.util
import json
import re
from types import SimpleNamespace
from run_observation_v1 import HERE, ROOT, digest, write_new

k=ctypes.WinDLL('kernel32',use_last_error=True);k.GetCurrentProcess.restype=ctypes.c_void_p
k.SetPriorityClass.argtypes=[ctypes.c_void_p,ctypes.c_ulong];assert k.SetPriorityClass(k.GetCurrentProcess(),0x4000)
assert json.loads((HERE/'separation-v1-summary.json').read_text())['status']=='passed'
spec=importlib.util.spec_from_file_location('match',ROOT/'tools/p3-selectivity-match.py')
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
binding=json.loads((HERE/'separation-v1-binding.json').read_text());assert all(digest(ROOT/p)==sha for p,sha in binding['files'].items())
runtime=HERE/'raw/runtime-v2'
args=SimpleNamespace(baseline=str(runtime),candidate=str(runtime),baseline_policy='baseline',candidate_policy='no-improving',
    baseline_workers=1,candidate_workers=1,java='C:/java/jdk-21/bin/java.exe',network=str(ROOT/'build-assets/nnue-candidate/conventional-cp64.cnnue'))
report={'status':'running','bindingSha256':digest(HERE/'separation-v1-binding.json'),
        'adapterSha256':digest(ROOT/'tools/p3-selectivity-match.py'),'checks':{}}
try:
    for workers in (1,4,16):
        args.candidate_workers=workers
        for p in ('baseline','no-improving','lazy-improving'):
            args.candidate_policy=p;key=f'{p}-w{workers}'
            report['checks'][key]=m.preflight(m.command(args,'candidate'),HERE/'raw'/f'{key}-uci.txt',workers,p)
    cmd=m.command(args,'baseline');entry=cmd.index('com.ohinteractive.seedv6.integration.Cp64SelectivityUciMain')
    cmd=cmd[:entry]+['com.ohinteractive.seedv6.uci.UciMain'];engine=m.b.Uci(cmd,HERE/'raw/ordinary-baseline-uci.txt')
    try:
        engine.send('uci');identity=engine.until('uciok',90)
        assert any('flags=262143' in s and 'aspiration=ADAPTIVE16' in s and 'build=71' in s for s in identity)
        engine.send('setoption name OwnBook value false');engine.send('setoption name Threads value 1')
        engine.send('setoption name Hash value 64');engine.send('isready');engine.until('readyok')
        results=[]
        for _ in range(2):
            engine.send('ucinewgame');engine.send('isready');engine.until('readyok')
            engine.send('position startpos');engine.send('go nodes 100000')
            results.append(m.b.inspect_result(m.b.chess.Board(),engine.until('bestmove ',60)))
        for a,b in zip(results,report['checks']['baseline-w1']['requests']):
            for key in ('bestmove','nodes','termination'):assert a[key]==b[key],key
            for pattern in (r'depth (\d+)',r'score (cp|mate) (-?\d+)',r'pv (.*)$'):
                assert re.search(pattern,a['finalInfo']).groups()==re.search(pattern,b['finalInfo']).groups()
        report['ordinaryBaseline']={'identity':identity,'results':results,'explicitProfileParity':True}
    finally:engine.close()
    assert all(digest(ROOT/p)==sha for p,sha in binding['files'].items())
    report['status']='passed'
except BaseException as error:report.update(status='failed',failure=repr(error));raise
finally:write_new(HERE/'uci-qualification-v1.json',report)
print(report['status'])
