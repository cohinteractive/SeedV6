"""Sequential correctness, diagnostic loss and cost assessment of explicit checkpoints.

Run under nnue-control, after training, with the local CUDA DLL path. Does not
select a winner or play games. The immutable specification names every input
and a new export directory for each model; commands and identities are retained.
"""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import time
import compute_gate


def sha(path):
    with Path(path).open('rb') as stream:return hashlib.file_digest(stream,'sha256').hexdigest()


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('spec',type=Path);parser.add_argument('output',type=Path)
    a=parser.parse_args();compute_gate.require();spec=json.loads(a.spec.read_text())
    a.output.mkdir(parents=True,exist_ok=False);commands=[];results={};start=time.monotonic()
    tools=Path(__file__).parent;python=[sys.executable,'-B']
    java=['C:/java/jdk-21/bin/java.exe','--add-modules=jdk.incubator.vector','-XX:ActiveProcessorCount=1','-Xmx512m','-Xbatch',
          '-cp','research/build/install/research/lib/*']
    def run(command,name):
        compute_gate.require();log=a.output/(name+'.log');err=a.output/(name+'.stderr')
        commands.append(dict(command=list(map(str,command)),stdout=str(log),stderr=str(err)))
        (a.output/'commands.json').write_text(json.dumps(commands,indent=2)+'\n')
        with log.open('xb') as out,err.open('xb') as errors:
            subprocess.run(command,stdout=out,stderr=errors,check=True,timeout=300)
        return log
    for model in spec['models']:
        name=model['id'];evaluation=Path(model['evaluation']);network=evaluation/'network.cnnue'
        run(python+[str(tools/'evaluate_checkpoint.py'),model['checkpoint'],spec['probes'],spec['validation'],str(evaluation),
                    '--vector','--strength-research'],name+'-export')
        if model.get('expected_sha256') and sha(network)!=model['expected_sha256']:raise ValueError('Protected incumbent changed')
        run(python+[str(tools/'strength_float_check.py'),model['checkpoint'],str(network),spec['validation'],
                    str(evaluation/'float-validation.json')],name+'-float')
        metadata=json.loads((evaluation/'network.cnnue.json').read_text());hidden=metadata['hidden']
        # Benchmark actual match kernels; separately exercise trained research delta parity.
        benchmark=run(java+['com.ohinteractive.seedv6.tools.eval.'+('ConventionalNnueBenchmark' if hidden==256 else 'StrengthNnueBenchmark'),
                           str(network)],name+'-benchmark')
        if hidden==256:
            run(java+['com.ohinteractive.seedv6.tools.eval.StrengthNnueBenchmark',str(network)],name+'-research-parity-benchmark')
        run(python+[str(tools/'uci_smoke.py'),'--network',str(network),'--strength-research','--vector','--output',str(evaluation/'uci')],name+'-uci')
        results[name]=dict(network=str(network),network_sha256=sha(network),metadata=metadata,
                           parity=json.loads((evaluation/'parity.json').read_text()),
                           validation=json.loads((evaluation/'validation.json').read_text()),
                           float_validation=json.loads((evaluation/'float-validation.json').read_text()),
                           benchmark=json.loads(benchmark.read_text()),uci=json.loads((evaluation/'uci/summary.json').read_text()))
        print(json.dumps(dict(model=name,validation=results[name]['validation'],benchmark=results[name]['benchmark'])),flush=True)
    search=run(java+['com.ohinteractive.seedv6.tools.eval.StrengthSearchProbe',spec['roots']]+
               [x['network'] for x in results.values()],'search-cost')
    rows=[json.loads(line) for line in search.read_text().splitlines()]
    for model in results.values():
        matches=[r for r in rows if r['payload']==model['metadata']['payload_sha256']]
        nodes=sum(r['nodes'] for r in matches);nanos=sum(r['elapsed_ns'] for r in matches)
        model['search_cost']=dict(roots=len(matches),nodes=nodes,elapsed_ns=nanos,nps=nodes*1e9/nanos,
                                 limitation='Rotated fixed roots/depths with bounded requests. Different evaluations change node counts; NPS is not a speedup or Elo claim.')
    manifest=dict(spec=str(a.spec),spec_sha256=sha(a.spec),results=results,commands=commands,elapsed_seconds=time.monotonic()-start,
                  validation_sha256=sha(spec['validation']),roots_sha256=sha(spec['roots']),
                  raw_search=str(search),search_rows=rows)
    (a.output/'summary.json').write_text(json.dumps(manifest,indent=2)+'\n')
    print('Assessment completed:',a.output/'summary.json',flush=True)


if __name__=='__main__':main()
