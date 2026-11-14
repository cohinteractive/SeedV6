"""Export, independent feature/float/integer parity, JVM delta/cost and UCI assessment.

Run under existing owner compute supervision. Completed and failed outputs are
immutable. No model selection or games. Uses separately built research runtime.
"""
import argparse
import array
import json
import math
from pathlib import Path
import subprocess
import sys
import time
import chess
from bootstrap import ROOT, BASE, PRIOR, write, sha
import codec
import compute_gate


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('checkpoint',type=Path);p.add_argument('output',type=Path)
    p.add_argument('--pilot',action='store_true');a=p.parse_args();compute_gate.require()
    a.output.mkdir(parents=True,exist_ok=False);commands=[];start=time.monotonic()
    network=a.output/'network.p2nnue';meta=codec.export(a.checkpoint,network);kind=codec.KINDS[meta['architecture']]
    probes=ROOT/'docs/research/conventional-nnue-cglhw/raw/e007-probes/probes.fens'
    validation=PRIOR/'raw/mixed-holdout/validation.txt'
    java=['C:/java/jdk-21/bin/java.exe','--add-modules=jdk.incubator.vector','-XX:ActiveProcessorCount=1','-Xmx512m','-Xbatch','-cp',str(ROOT/'research/build/install/research/lib/*')]
    def run(command,name):
        compute_gate.require();stdout=a.output/(name+'.jsonl');stderr=a.output/(name+'.stderr')
        commands.append(dict(command=list(map(str,command)),stdout=str(stdout),stderr=str(stderr)))
        (a.output/'commands.json').write_text(json.dumps(commands,indent=2)+'\n')
        with stdout.open('xb') as out,stderr.open('xb') as err:subprocess.run(command,stdout=out,stderr=err,check=True,timeout=300)
        return stdout
    trainer=ROOT/'build/conventional-nnue/cargo-target/release/seed-phase2-nnue.exe'
    bf=run([str(trainer),'features',str(probes),meta['architecture']],'bullet-features')
    jf=run(java+['com.ohinteractive.seedv6.tools.eval.Phase2NnueProbe',str(network),str(probes)],'java-vector')
    sf=run(java[:1]+['-Dseed.nnue.scalar=true']+java[1:]+['com.ohinteractive.seedv6.tools.eval.Phase2NnueProbe',str(network),str(probes)],'java-scalar')
    if jf.read_bytes()!=sf.read_bytes():raise ValueError('Scalar/vector outputs differ')
    def rows(path):return [json.loads(line) for line in Path(path).read_text().splitlines()]
    bullets=rows(bf);javas=rows(jf);gpu=rows(a.checkpoint/'gpu-probes.jsonl');fens=probes.read_text().splitlines()
    if len(fens)!=259 or not len(bullets)==len(javas)==len(gpu)==len(fens):raise ValueError('Missing probes')
    quant=array.array('h');quant.frombytes(network.read_bytes()[76:])
    if sys.byteorder!='little':quant.byteswap()
    fp=[values for values,scale in codec.merged(codec.tensors(a.checkpoint/'optimiser_state/weights.bin',kind),kind)]
    qp=codec.split_flat(quant,kind)
    max_gpu=0.;qerrors=[];python_rows=[];activations=[];downstream=[];residuals=[]
    for fen,b,j,g in zip(fens,bullets,javas,gpu):
        board=chess.Board(fen);q=codec.evaluate(qp,board,kind,True);f=codec.evaluate(fp,board,kind,False)
        if not b['fen']==j['fen']==g['fen']==fen:raise ValueError('Probe order mismatch')
        if q['features']!=[sorted(b['stm']),sorted(b['ntm'])] or q['features']!=j['features']:raise ValueError('Feature parity mismatch')
        if q['score']!=j['score'] or q['score']!=j['rebuilt'] or q['accumulators']!=j['accumulators']:raise ValueError('Integer parity mismatch')
        max_gpu=max(max_gpu,abs(f['raw']-g['raw']));qerrors.append(q['score']-f['score']);python_rows.append(dict(fen=fen,integer=q['score'],float_link=f['score']))
        activations.extend(x for side in q['accumulators'] for x in side)
        downstream.extend(q['downstream_activations']);residuals.append(q['residual_link'])
    if max_gpu>2e-5:raise ValueError('GPU float mismatch')
    with (a.output/'python-probes.jsonl').open('x') as stream:
        for row in python_rows:stream.write(json.dumps(row)+'\n')
    parity=dict(status='PASS',positions=len(fens),exact='Independent Python, Rust features, Java scalar/vector/full-refresh/raw sums',
                gpu_max_abs_raw_error=max_gpu,quantization_rms_link_error=math.sqrt(sum(x*x for x in qerrors)/len(qerrors)),
                quantization_max_abs_link_error=max(map(abs,qerrors)))
    activation_diagnostic=dict(probe_accumulator_count=len(activations),zero_fraction=sum(x<=0 for x in activations)/len(activations),
        clipped_high_fraction=sum(x>=255 for x in activations)/len(activations),minimum=min(activations),maximum=max(activations),
        mean_abs_residual_link=sum(map(abs,residuals))/len(residuals),limitation='Probe activations only, not the distribution of searched nodes.')
    if downstream:activation_diagnostic.update(downstream_zero_fraction=sum(x==0 for x in downstream)/len(downstream),downstream_clipped_high_fraction=sum(x==255**2 for x in downstream)/len(downstream))
    val=json.loads(run(java+['com.ohinteractive.seedv6.tools.eval.Phase2NnueProbe',str(network),'--validation',str(validation)],'validation').read_text())
    result=dict(metadata=meta,parity=parity,validation=val,activation_diagnostic=activation_diagnostic)
    if not a.pilot:
        bench=json.loads(run(java+['com.ohinteractive.seedv6.tools.eval.Phase2NnueBenchmark',str(network)],'benchmark').read_text())
        if bench['incremental_parity']!='PASS' or bench.get('allocated_bytes_100000',0)!=0:raise ValueError('Incremental correctness/allocation gate failed')
        result['benchmark']=bench
        uci=a.output/'uci';run([sys.executable,'-B',str(ROOT/'tools/conventional-nnue/uci_smoke.py'),'--network',str(network),'--strength-research','--vector','--output',str(uci)],'uci')
        result['uci']=json.loads((uci/'summary.json').read_text())
        search=run(java+['com.ohinteractive.seedv6.tools.eval.Phase2SearchProbe',str(PRIOR/'cost-roots.fens'),str(network)],'search-cost')
        observations=rows(search);nodes=sum(x['nodes'] for x in observations);nanos=sum(x['elapsed_ns'] for x in observations)
        result['search_cost']=dict(nps=nodes*1e9/nanos,nodes=nodes,elapsed_ns=nanos,rows=observations)
        counts=run(java[:1]+['-Dseed.phase2.count=true']+java[1:]+['com.ohinteractive.seedv6.tools.eval.Phase2SearchProbe',str(PRIOR/'cost-roots.fens'),str(network)],'search-invocations')
        result['search_invocations']=[dict(root=x['root'],round=x['round'],nodes=x['nodes'],counts=x['diagnostic_counters']) for x in rows(counts)]
    result.update(commands=commands,validation_sha256=sha(validation),elapsed_seconds=time.monotonic()-start,
                  limitation='Diagnostic holdout is not source-game independent; benchmark and NPS are not strength acceptance. Instrumented invocation run is separate from timing.')
    write(a.output/'summary.json',result);print(json.dumps(dict(architecture=meta['architecture'],parity=parity,validation=val)),flush=True)


if __name__=='__main__':main()
