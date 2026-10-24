"""Matched, uninstrumented frozen-actor native owner-CPU availability calibration."""
import json
import math
from pathlib import Path
import statistics
import sys
import time
from types import SimpleNamespace
from verify_inputs import HERE, ROOT, OLD, audit, digest, write_new
from run_diagnostic_v1 import module, roots, background
from run_persistent_v2 import OwnerCpu
from analyze_diagnostic_v1 import signature


def host_interval(start,end,cpu=0):
    host=module('host',ROOT/'tools/p3-tt-session.py')
    value=host.host_interval(start,end,cpu)
    value['passed']=value['otherAverageLogicalCores']<=2 and value['memoryEndpointsPass']
    return value


def worker(name):
    assert sys.stdin.readline().strip()=='GO'
    background(); m=module('spending',OLD/'spending_match_v1.py')
    native=module('native',ROOT/'tools/sr036-process-cost.py')
    out=HERE/'raw'/name; engines={}; owners={}; cases=roots(m.b.chess)
    args=SimpleNamespace(baseline=str(OLD/'raw/runtime-production-v2'),candidate=str(OLD/'raw/runtime-candidate-v2'),
        baseline_policy='baseline',candidate_policy='boundary2',baseline_workers=1,candidate_workers=1,
        java='C:/java/jdk-21/bin/java.exe',network=str(ROOT/'build-assets/nnue-candidate/conventional-cp64.cnnue'))
    try:
        for actor,profile in [('baseline','baseline'),('candidate','boundary2')]:
            engine=m.b.Uci(m.command(args,actor),out/(actor+'-uci.txt')); engines[actor]=engine
            engine.send('uci'); m.verify_identity(engine.until('uciok',90),1,profile,64)
            for _ in range(4):
                engine.send('ucinewgame'); engine.send('isready'); engine.until('readyok')
                engine.send('position startpos'); engine.send('go nodes 100000')
                result=m.b.inspect_result(m.b.chess.Board(),engine.until('bestmove ',30))
                assert result['nodes']==100000 and result['termination']=='NODE_LIMIT'
            owners[actor]=OwnerCpu(engine.process.pid,out/(actor+'-threads.txt'))
        signatures={}
        with (out/'requests.jsonl').open('x',encoding='utf8',newline='\n') as stream:
            for round_index in range(2):
                for index,(rid,position,board) in enumerate(cases):
                    for actor in (('baseline','candidate') if (round_index+index)%2==0 else ('candidate','baseline')):
                        engine=engines[actor]; owner=owners[actor]
                        engine.send('ucinewgame'); engine.send('position '+position)
                        engine.send('isready'); engine.until('readyok')
                        cpu0=owner.sample(); native0=native.snapshot(int(engine.process._handle))['totalCpuSeconds']
                        start=time.perf_counter_ns(); engine.send('go nodes 400000')
                        lines=engine.until('bestmove ',60); elapsed=time.perf_counter_ns()-start
                        cpu1=owner.sample(); native1=native.snapshot(int(engine.process._handle))['totalCpuSeconds']
                        result=m.b.inspect_result(board,lines)
                        assert result['nodes']==400000 and result['termination']=='NODE_LIMIT'
                        assert cpu0['creationTicks']==cpu1['creationTicks']
                        engine.send('isready'); assert engine.until('readyok')==['readyok']
                        current_signature=signature({'result':result})
                        if rid in signatures: assert current_signature==signatures[rid],('Non-clock divergence',actor,rid)
                        else: signatures[rid]=current_signature
                        owned=cpu1['cpuSeconds']-cpu0['cpuSeconds']
                        stream.write(json.dumps({'actor':actor,'round':round_index,'root':rid,
                            'ownerCpuSeconds':owned,'externalElapsedNanos':elapsed,
                            'nativeCpuSeconds':native1-native0,'cpuDelivery':owned/(result['elapsedNanos']/1e9),
                            'result':result})+'\n'); stream.flush()
    finally:
        for owner in owners.values(): owner.close()
        for engine in engines.values(): engine.close()


def execute(name):
    assert all(c.isalnum() or c=='-' for c in name)
    background(); audit(); out=HERE/'raw'/name; out.mkdir(exist_ok=False)
    owned=module('owned',ROOT/'tools/tournament-process.py'); host=module('host',ROOT/'tools/p3-tt-session.py')
    b=module('baseline',ROOT/'tools/search-baseline.py')
    record={'status':'running','scriptSha256':digest(__file__),
        'method':'28 requests per exact actor:14 existing diagnostic roots x2 balanced rounds;400k nodes, native owner CPU, no JFR or wrapper;4x100k warmups plus unchanged launcher warmup.'}
    start=host.host_sample()
    try:
        with (out/'worker.log').open('xb') as stream:
            record['execution']=owned.run_owned([sys.executable,'-B',str(Path(__file__).resolve()),name,'worker'],stream,300,ROOT,b.clean_environment())
        assert record['execution']['exitCode']==0
        record['resources']=host_interval(start,host.host_sample(),record['execution']['totalOwnedCpuSeconds'])
        rows=[json.loads(line) for line in (out/'requests.jsonl').read_text().splitlines()]; assert len(rows)==56
        record['actors']={}
        for actor in ('baseline','candidate'):
            values=sorted(r['cpuDelivery'] for r in rows if r['actor']==actor); assert len(values)==28
            record['actors'][actor]={'requests':len(values),'medianCpuDelivery':statistics.median(values),
                'fifthPercentileCpuDelivery':values[math.ceil(.05*len(values))-1]}
        record['medianSpread']=abs(record['actors']['baseline']['medianCpuDelivery']-record['actors']['candidate']['medianCpuDelivery'])
        record['criteriaPassed']=record['resources']['passed'] and record['medianSpread']<=.05 and all(
            r['medianCpuDelivery']>=.90 and r['fifthPercentileCpuDelivery']>=.80 for r in record['actors'].values())
        audit(); record['status']='passed'
    except BaseException as error:
        record.update(status='failed',failure=repr(error)); raise
    finally:
        record['evidence']={str(p.relative_to(HERE)):digest(p) for p in out.rglob('*') if p.is_file()}
        write_new(HERE/(name+'-summary.json'),record)
    print(json.dumps({k:v for k,v in record.items() if k not in ('evidence','resources')},indent=2),flush=True)
    return record


if __name__=='__main__':
    if len(sys.argv)==3 and sys.argv[2]=='worker': worker(sys.argv[1])
    else: execute(sys.argv[1])
