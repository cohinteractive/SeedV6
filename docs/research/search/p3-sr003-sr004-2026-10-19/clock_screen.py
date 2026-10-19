"""Workstream 2 development clock screen using the existing audited runner unchanged."""
import argparse,importlib.util,json,pathlib,types
ROOT=pathlib.Path(__file__).resolve().parents[4]
spec=importlib.util.spec_from_file_location('sr004_runner',ROOT/'tools/sr032-match.py')
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
b=m.b
def command(args,side):
    cmd=b.command(args.java,getattr(args,side),args.store,'production','production','off','default','off','default')
    cmd[3]='-XX:ActiveProcessorCount=2'
    cmd[4:4]=['-Dseedv6.uci.rootDriver='+getattr(args,side+'_policy'),
              '-Dseedv6.uci.warmupWorkers='+str(getattr(args,side+'_workers')),'-Dseedv6.research.actor='+side]
    cmd[-1]='com.ohinteractive.seedv6.integration.P3DriverUciMain'
    return cmd
def preflight(cmd,path,workers,policy):
    engine=b.Uci(cmd,path)
    try:
        engine.send('uci');lines=engine.until('uciok',120)
        engine.send('setoption name OwnBook value false');engine.send('setoption name Threads value '+str(workers))
        engine.send('isready');lines+=engine.until('readyok',120)
        identity=[s for s in lines if s.startswith('info string evaluator=')][-1]
        f=dict(s.split('=',1) for s in identity.split() if '=' in s)
        expected={'sha256':b.NETWORK_SHA256,'checkpoint':b.CHECKPOINT,'mapping':'BRN_PAIR2_MATERIAL_RESIDUAL_QUARTER_V1',
                  'kernel':'flat','rootDriver':policy,'hints':'completed_same_key','workers':str(workers),
                  'mode':'research','experimental':'true','leaves':'see_selective','lmr':'quiet','lmp':'window',
                  'see':'qxray','reuse':'effort','concurrency':'root_reuse' if workers>1 else 'off'}
        for key,value in expected.items():
            if f.get(key)!=value:raise ValueError((key,value,identity))
        requests=[]
        for _ in range(2):
            engine.send('ucinewgame');engine.send('isready');engine.until('readyok')
            engine.send('position startpos');engine.send('go nodes 100000')
            r=b.inspect_result(b.chess.Board(),engine.until('bestmove ',60))
            if r['nodes']!=100000:raise ValueError('Incomplete request accounting')
            requests.append(r)
        return {'identity':identity,'requests':requests}
    finally:engine.close()
m.command=command;m.preflight=preflight
p=argparse.ArgumentParser();p.add_argument('protocol');p.add_argument('--preflight-only',action='store_true');a=p.parse_args()
protocol=json.loads(pathlib.Path(a.protocol).read_text());args=types.SimpleNamespace(**protocol['arguments'])
for path,sha in protocol['frozenFiles'].items():
    if b.digest(path)!=sha:raise ValueError('Frozen input changed: '+path)
if a.preflight_only:
    out=pathlib.Path(args.out).parent/'uci-preflight-v1';out.mkdir(exist_ok=False)
    result={side:preflight(command(args,side),out/(side+'-uci.txt'),getattr(args,side+'_workers'),getattr(args,side+'_policy')) for side in ('baseline','candidate')}
    b.save(out/'result.json',result)
else:m.run(args)
for path,sha in protocol['frozenFiles'].items():
    if b.digest(path)!=sha:raise ValueError('Frozen input changed after run: '+path)
