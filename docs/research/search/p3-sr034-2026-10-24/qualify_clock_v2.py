"""Actual UCI clock/mode/fallback/worker qualification. No strength inference."""
import ctypes
import json
import re
import shutil
import subprocess
from types import SimpleNamespace
from run_trajectory_v1 import HERE, ROOT, digest, write_new, module

def signature(r):
    return {k:r[k] for k in ('bestmove','nodes','termination')} | {
        'result':None if r['finalInfo'] is None else
        [re.search(pattern,r['finalInfo']).groups() for pattern in (r'depth (\d+)',r'score (cp|mate) (-?\d+)',r'pv (.*)$')]}

def main():
    k=ctypes.WinDLL('kernel32',use_last_error=True);k.GetCurrentProcess.restype=ctypes.c_void_p
    k.SetPriorityClass.argtypes=[ctypes.c_void_p,ctypes.c_ulong];assert k.SetPriorityClass(k.GetCurrentProcess(),0x4000)
    assert json.loads((HERE/'clock-policy-validation-v1-summary.json').read_text())['exitCode']==0
    runtime=HERE/'raw/runtime-clock-v1';binding_path=HERE/'clock-runtime-v1-binding.json'
    if not binding_path.exists():
        shutil.copytree(ROOT/'build/research/runtime',runtime)
        files=sorted(p for p in runtime.rglob('*') if p.is_file())
        files += [ROOT/'tools/p3-time-match.py',ROOT/'tools/p3-selectivity-match.py',ROOT/'tools/search-baseline.py',
                  ROOT/'build-assets/nnue-candidate/conventional-cp64.cnnue',Path(__file__)]
        write_new(binding_path,{'head':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),
            'sourceState':subprocess.check_output(['git','status','--short'],cwd=ROOT,text=True),
            'files':{str(p.relative_to(ROOT)):digest(p) for p in files}})
    binding=json.loads(binding_path.read_text())
    def stable():
        for path,sha in binding['files'].items():assert digest(ROOT/path)==sha,path
    stable();m=module('time_match',ROOT/'tools/p3-time-match.py');b=m.b
    args=SimpleNamespace(baseline=str(runtime),candidate=str(runtime),baseline_policy='baseline',candidate_policy='baseline',
        baseline_workers=1,candidate_workers=1,java='C:/java/jdk-21/bin/java.exe',network=str(ROOT/'build-assets/nnue-candidate/conventional-cp64.cnnue'))
    report={'status':'running','bindingSha256':digest(binding_path),'checks':[], 'fixedControls':{}, 'qualifierSha256':digest(Path(__file__))}
    def request(engine,position,board,go):
        engine.send('ucinewgame');engine.send('isready');engine.until('readyok')
        engine.send('position '+position);engine.send('go '+go)
        lines=engine.until('bestmove ',60);r=b.inspect_result(board,lines)
        depths=[int(x.split()[2]) for x in lines if x.startswith('info depth ')]
        assert depths==list(range(1,len(depths)+1)),'False/gapped completed depth'
        engine.send('stop');engine.send('isready');assert engine.until('readyok')==['readyok'],'Stale publication'
        return r
    try:
        for profile in m.PROFILES:
            args.candidate_policy=profile
            engine=b.Uci(m.command(args,'candidate'),HERE/'raw'/f'clock-{profile}-v2-uci.txt')
            try:
                engine.send('uci');m.verify_identity(engine.until('uciok',90),1,profile,64)
                for workers in range(1,17):
                    engine.send(f'setoption name Threads value {workers}');engine.send('isready');engine.until('readyok')
                    r=request(engine,'startpos',b.chess.Board(),'depth 3')
                    assert r['termination']=='COMPLETED'
                    report['checks'].append({'profile':profile,'workers':workers,'mode':'depth3','result':r})
                engine.send('setoption name Threads value 1');engine.send('isready');engine.until('readyok')
                fixed=[]
                for go in ('depth 7','nodes 100000','movetime 50'):
                    r=request(engine,'startpos',b.chess.Board(),go)
                    assert r['termination']!='TIME_ALLOCATION'
                    if go!='movetime 50':fixed.append(signature(r))
                    report['checks'].append({'profile':profile,'workers':1,'mode':go,'result':r})
                report['fixedControls'][profile]=fixed
                for workers in (1,4,16):
                    engine.send(f'setoption name Threads value {workers}');engine.send('isready');engine.until('readyok')
                    for remaining,inc,mtg in ((0,0,30),(50,100,30),(51,0,30),(300,0,30),(1000,0,30),
                                              (1000,100,30),(10000,100,30),(1000,0,1)):
                        go=f'wtime {remaining} btime {remaining} winc {inc} binc {inc} movestogo {mtg}'
                        r=request(engine,'startpos',b.chess.Board(),go)
                        hard=min(max(0,remaining-50),max(1,remaining//mtg+inc-inc//4-10))
                        if remaining<=50:assert r['nodes']==0 and r['finalInfo'] is None
                        report['checks'].append({'profile':profile,'workers':workers,'mode':'clock','remaining':remaining,
                            'increment':inc,'movesToGo':mtg,'hardMillis':hard,'overrunMillis':r['elapsedNanos']/1e6-hard,'result':r})
                    # External stop and immediate position replacement must not publish old work.
                    engine.send('position startpos');engine.send('go infinite');engine.until('info depth 2 ',60)
                    engine.send('stop');r=b.inspect_result(b.chess.Board(),engine.until('bestmove ',60))
                    assert r['termination']=='STOPPED';report['checks'].append({'profile':profile,'workers':workers,'mode':'infinite-stop','result':r})
                    engine.send('position startpos');engine.send('go infinite');engine.until('info depth 2 ',60)
                    board=b.chess.Board();board.push_uci('e2e4')
                    engine.send('position startpos moves e2e4');engine.send('isready')
                    fence=engine.until('readyok');assert not any(x.startswith('bestmove ') for x in fence)
                    engine.send('go depth 3')
                    r=b.inspect_result(board,engine.until('bestmove ',60));assert r['termination']=='COMPLETED'
                    engine.send('isready');assert engine.until('readyok')==['readyok']
                    report['checks'].append({'profile':profile,'workers':workers,'mode':'replace','result':r})
            finally:engine.close()
        assert all(v==report['fixedControls']['baseline'] for v in report['fixedControls'].values()),'Nonclock changes'
        # Compare current ordinary production with the experimental baseline at the same deterministic node budget.
        cmd=m.command(args,'baseline');entry=cmd.index('com.ohinteractive.seedv6.integration.Cp64TimeUciMain')
        engine=b.Uci(cmd[:entry]+['com.ohinteractive.seedv6.uci.UciMain'],HERE/'raw/clock-ordinary-v2-uci.txt')
        try:
            engine.send('uci');hello=engine.until('uciok',90)
            assert any('flags=3407871' in x and 'aspiration=ADAPTIVE16' in x for x in hello)
            actual=[signature(request(engine,'startpos',b.chess.Board(),go)) for go in ('depth 7','nodes 100000')]
            assert actual==report['fixedControls']['baseline'];report['ordinaryProductionParity']=True
        finally:engine.close()
        stable();report['status']='passed'
    except BaseException as error:report.update(status='failed',failure=repr(error));raise
    finally:
        write_new(HERE/'clock-qualification-v2.json',report)
    print(json.dumps({'status':report['status'],'requests':len(report['checks']),
        'ordinaryProductionParity':report['ordinaryProductionParity'],
        'maxClockOverrunMillis':max(x['overrunMillis'] for x in report['checks'] if 'overrunMillis' in x)}))

if __name__=='__main__':
    from pathlib import Path
    main()
