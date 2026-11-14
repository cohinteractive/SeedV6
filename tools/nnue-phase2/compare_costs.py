"""Matched pure-JVM cost trials, exact WDL25 kernel control and reversed trial order."""
import json
import os
from pathlib import Path
import statistics
import subprocess
import sys
import time
from bootstrap import ROOT, BASE, PRIOR, write, sha
sys.path.insert(0,str(ROOT/'tools/conventional-nnue'))
import compute_gate


def main():
    compute_gate.require();out=BASE/'raw/comparative-cost';out.mkdir(exist_ok=False);start=time.monotonic()
    runtime=json.loads((BASE/'frozen-runtime-v3.json').read_text());workspace=Path(runtime['path'])
    for name,digest in runtime['files'].items():assert sha(workspace/name)==digest
    classes=out/'classes';helper=ROOT/'tools/nnue-phase2/WarmedCost.java'
    subprocess.run(['C:/java/jdk-21/bin/javac.exe','-d',str(classes),str(helper)],check=True,timeout=60)
    conventional='com.ohinteractive.seedv6.tools.eval.StrengthSearchProbe';phase2='com.ohinteractive.seedv6.tools.eval.Phase2SearchProbe'
    baseline=json.loads((BASE/'baseline-kernel-r5.json').read_text())
    models=[dict(id='wdl25-production',probe=conventional,network=runtime['incumbent'],workspace=str(workspace)),
            dict(id='wdl25-kernel',probe=phase2,network=baseline['metadata']['network'],workspace=str(workspace))]
    for name in ['deep16','pawnpairs']:
        report=json.loads((BASE/f'b-{name}-64-assessment.json').read_text())
        assert report['parity']['status']=='PASS' and report['benchmark']['allocated_bytes_100000']==0
        models.append(dict(id=name,probe=phase2,network=report['metadata']['network'],workspace=str(workspace)))
    prior=json.loads((BASE/'frozen-runtime-v2.json').read_text())
    for name,digest in prior['files'].items():assert sha(Path(prior['path'])/name)==digest
    deep=next(m for m in models if m['id']=='deep16')
    models.append(dict(deep,id='deep16-before-cache',workspace=prior['path']))
    reports=[];commands=[]
    java=['C:/java/jdk-21/bin/java.exe','--add-modules=jdk.incubator.vector','-XX:ActiveProcessorCount=1','-Xmx512m','-Xbatch',
          '-cp',str(classes)+os.pathsep+str(workspace/'lib/*'),'WarmedCost']
    roots=PRIOR/'cost-roots.fens'
    for repeat,order in enumerate([models,list(reversed(models))]):
        for model in order:
            compute_gate.require()
            if time.monotonic()-start>900:raise RuntimeError('Comparative-cost wall budget reached')
            command=java.copy();command[command.index('-cp')+1]=str(classes)+os.pathsep+str(Path(model['workspace'])/'lib/*')
            command += [model['probe'],str(roots),model['network']];log=out/f'{repeat}-{model["id"]}.jsonl';err=log.with_suffix('.stderr')
            commands.append(dict(command=command,stdout=str(log),stderr=str(err)))
            (out/'commands.json').write_text(json.dumps(commands,indent=2)+'\n')
            with log.open('xb') as stream,err.open('xb') as error:subprocess.run(command,stdout=stream,stderr=error,check=True,timeout=180)
            rows=[];stage=None
            for line in log.read_text().splitlines():
                value=json.loads(line)
                if 'stage' in value:stage=value['stage']
                elif stage=='measured':rows.append(value)
            assert len(rows)==24
            nodes=sum(r['nodes'] for r in rows);elapsed=sum(r['elapsed_ns'] for r in rows)
            reports.append(dict(id=model['id'],repeat=repeat,network_sha256=sha(model['network']),nps=nodes*1e9/elapsed,
                                nodes=nodes,elapsed_ns=elapsed,rows=rows,log=str(log),log_sha256=sha(log)))
    equivalent=[]
    for repeat in range(2):
        for original,candidate in [('wdl25-production','wdl25-kernel'),('deep16-before-cache','deep16')]:
            left=next(r for r in reports if r['repeat']==repeat and r['id']==original)['rows']
            right=next(r for r in reports if r['repeat']==repeat and r['id']==candidate)['rows']
            fields=['round','root','nodes','score','depth','completed','configuration']
            assert [[r[f] for f in fields] for r in left]==[[r[f] for f in fields] for r in right],'Equal-payload search divergence'
            equivalent.append(dict(original=original,candidate=candidate,repeat=repeat,requests=len(left),node_score_depth_completion_configuration='EXACT MATCH'))
    summary={model['id']:dict(nps_trials=[r['nps'] for r in reports if r['id']==model['id']],
              mean_nps=statistics.mean(r['nps'] for r in reports if r['id']==model['id'])) for model in models}
    result=dict(summary=summary,equal_payload_search_equivalence=equivalent,reports=reports,commands=commands,
        runtime_manifest_sha256=sha(BASE/'frozen-runtime-v3.json'),prior_runtime_manifest_sha256=sha(BASE/'frozen-runtime-v2.json'),helper_source_sha256=sha(helper),helper_class_sha256=sha(classes/'WarmedCost.class'),
        elapsed_seconds=time.monotonic()-start,policy='Separate JVM per model/repeat, complete first pass warmup retained, second pass timing, reverse process order on repeat2.',
        limitation='Different learned evaluations change trees. NPS is not an intrinsic evaluator speedup or an Elo result. No hardware cache/branch counters collected.')
    write(BASE/'comparative-cost.json',result);write(out/'summary.json',result);print(json.dumps(summary,indent=2))


if __name__=='__main__':main()
