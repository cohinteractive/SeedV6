"""Phase 3 O1/O2 configurations on the existing frozen pair-block/PGN runner.

The SR-032 engine/clock/owned-process/auditing implementation is reused unchanged
except its optional background launch flag. This module owns only configuration
and preflight. Explicit policy 0 is the accepted cumulative production comparator.
"""
import importlib.util
from pathlib import Path

spec=importlib.util.spec_from_file_location('p3_existing_runner',Path(__file__).with_name('sr032-match.py'))
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
b=m.b
original_command=m.command


def command(args,side):
    policy=str(getattr(args,side+'_policy'));workers=getattr(args,side+'_workers')
    if policy not in tuple(map(str,range(8))) or (workers!=1 and policy not in ('0','4')):
        raise ValueError('Unsupported Phase 3 policy/worker domain')
    cmd=original_command(args,side)
    cmd=[('-Dseedv6.uci.informationResearch='+policy if s.startswith('-Dseedv6.uci.tacticalExtension=') else s) for s in cmd]
    if cmd[-1]!='com.ohinteractive.seedv6.integration.ResearchUciMain':
        raise ValueError('Phase 3 requires explicit unified research workspace')
    cmd[-1]='com.ohinteractive.seedv6.integration.P3InformationUciMain'
    return cmd


def preflight(cmd,path,workers,policy):
    policy=int(policy);engine=b.Uci(cmd,path)
    try:
        engine.send('uci');lines=engine.until('uciok',90)
        engine.send('setoption name OwnBook value false');engine.send('setoption name Threads value '+str(workers))
        engine.send('isready');lines+=engine.until('readyok',90)
        identity=[s for s in lines if s.startswith('info string evaluator=')][-1]
        f=dict(s.split('=',1)for s in identity.split()if '='in s)
        expected={'sha256':b.NETWORK_SHA256,'checkpoint':b.CHECKPOINT,'mapping':'BRN_PAIR2_MATERIAL_RESIDUAL_QUARTER_V1',
                  'workers':str(workers),'information':str(policy),'leaves':'see_selective','see':'qxray','lmr':'quiet','lmp':'window',
                  'reuse':'effort_history'if policy&1 else'effort','mode':'research'if policy else'production',
                  'concurrency':'root_reuse'if workers>1 else'off','nmp':'off','internal':'off','aspiration':'off',
                  'hints':'completed_same_key'if policy&4 else'off','learning':'nominal_only'if policy&2 else'all_searched',
                  'history':'completed_iterations'if policy&1 else'root_seeded_private'if workers>1 else'invocation'}
        for key,value in expected.items():
            if f.get(key)!=value:raise ValueError((key,value,identity))
        if policy and f.get('experimental')!='true':raise ValueError('Missing experimental identity')
        requests=[]
        for _ in range(2):
            engine.send('ucinewgame');engine.send('isready');engine.until('readyok')
            engine.send('position startpos');engine.send('go nodes 100000')
            r=b.inspect_result(b.chess.Board(),engine.until('bestmove ',60))
            if r['nodes']!=100000:raise ValueError('Node admission mismatch')
            requests.append(r)
        return {'identity':identity,'requests':requests}
    finally:engine.close()


m.command=command
m.preflight=preflight


def run(args):
    if not getattr(args,'background',False):raise ValueError('Phase 3 default requires background launch')
    return m.run(args)
