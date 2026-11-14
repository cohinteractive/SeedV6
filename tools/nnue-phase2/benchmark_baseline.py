"""Exact WDL25 payload control for the new experimental downstream kernel."""
import json
import argparse
from pathlib import Path
import subprocess
import time
from bootstrap import ROOT, BASE, PRIOR, write, sha
from codec import wrap_conventional
import compute_gate


def main():
    p=argparse.ArgumentParser();p.add_argument('--revision',type=int,default=1);a=p.parse_args()
    if a.revision<1:raise ValueError('Invalid revision')
    suffix='' if a.revision==1 else f'-r{a.revision}'
    compute_gate.require();out=BASE/f'raw/baseline-kernel{suffix}';out.mkdir(exist_ok=False)
    original=ROOT/'build-assets/production/conventional-wdl25.cnnue';network=out/'wdl25.p2nnue'
    metadata=wrap_conventional(original,network);commands=[]
    java=['C:/java/jdk-21/bin/java.exe','--add-modules=jdk.incubator.vector','-XX:ActiveProcessorCount=1','-Xmx512m','-Xbatch','-cp',str(ROOT/'research/build/install/research/lib/*')]
    def run(command,name):
        compute_gate.require();path=out/(name+'.jsonl');err=out/(name+'.stderr')
        commands.append(dict(command=command,stdout=str(path),stderr=str(err)))
        with path.open('xb') as stream,err.open('xb') as errors:subprocess.run(command,stdout=stream,stderr=errors,check=True,timeout=300)
        return path
    probes=str(ROOT/'docs/research/conventional-nnue-cglhw/raw/e007-probes/probes.fens')
    reference=run(java+['com.ohinteractive.seedv6.tools.eval.ConventionalNnueProbe',str(original),probes],'production-parity')
    candidate=run(java+['com.ohinteractive.seedv6.tools.eval.Phase2NnueProbe',str(network),probes],'experimental-parity')
    if reference.read_bytes()!=candidate.read_bytes():raise ValueError('WDL25 payload changed behavior')
    costs=json.loads(run(java+['com.ohinteractive.seedv6.tools.eval.Phase2NnueBenchmark',str(network)],'phase2-cost').read_text())
    production=json.loads(run(java+['com.ohinteractive.seedv6.tools.eval.ProductionNnueCostBenchmark',str(original)],'production-cost').read_text())
    result=dict(metadata=metadata,parity='Exact259-row feature/accumulator/score/full-refresh match',
        experimental_cost=costs,production_cost=production,commands=commands,
        limitation='Identical deterministic255-transition walk, operation loops, warmup/repeats and JVM options; separate monomorphic production/research kernels. Microbenchmark is not equal-time strength. Base payload control separates arithmetic-kernel cost from architecture.')
    write(BASE/f'baseline-kernel{suffix}.json',result);write(out/'summary.json',result)
    print(json.dumps(result,indent=2))


if __name__=='__main__':main()
