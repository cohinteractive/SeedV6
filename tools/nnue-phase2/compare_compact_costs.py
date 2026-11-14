"""B003 cost comparison after qualified endpoints; no strength inference or selection."""
import argparse
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
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--runtime-version',type=int,required=True);a=p.parse_args()
    compute_gate.require();out=BASE/'raw/compact128-cost';out.mkdir(exist_ok=False);start=time.monotonic()
    manifest=BASE/f'frozen-runtime-v{a.runtime_version}.json';runtime=json.loads(manifest.read_text());workspace=Path(runtime['path'])
    for name,digest in runtime['files'].items():assert sha(workspace/name)==digest
    assert sha(runtime['incumbent'])==runtime['incumbent_sha256']
    classes=out/'classes';helper=ROOT/'tools/nnue-phase2/WarmedCost.java'
    subprocess.run(['C:/java/jdk-21/bin/javac.exe','-d',str(classes),str(helper)],check=True,timeout=60)
    prefix='com.ohinteractive.seedv6.tools.eval.';kernel=json.loads((BASE/'baseline-kernel-r5.json').read_text())['metadata']
    assert sha(kernel['network'])==kernel['network_sha256']
    models=[dict(id='wdl25-production',probe=prefix+'StrengthSearchProbe',network=runtime['incumbent']),
            dict(id='wdl25-kernel',probe=prefix+'Phase2SearchProbe',network=kernel['network'])]
    footprints={}
    for name in ('base','deep16','pawnpairs'):
        evidence=BASE/f'compact128-{name}-64-assessment.json';report=json.loads(evidence.read_text());meta=report['metadata']
        assert report['parity']['status']=='PASS' and report['uci']['status']=='PASS' and report['benchmark']['allocated_bytes_100000']==0
        assert meta['hidden']==128 and sha(meta['network'])==meta['network_sha256']
        models.append(dict(id='compact128-'+name,probe=prefix+'CompactSearchProbe',network=meta['network'],evidence_sha256=sha(evidence)))
        # Primitive-array payload only. Object/array headers and JVM alignment are excluded.
        model=meta['features']*128*2+128*2+256*8*4+8*2
        if name=='deep16':model+=256*16*4+16*2+16*8*4+8*2
        state=2*128*4+(272 if name=='deep16' else 16)*4+4*8
        footprints[name]=dict(file_bytes=meta['bytes'],model_array_payload_bytes=model,
                              accumulator_array_payload_bytes=state,stack257_array_payload_bytes=257*state,
                              limitation='Calculated from fixed geometry, excludes Java headers/alignment; not process RSS.')
    java=['C:/java/jdk-21/bin/java.exe','--add-modules=jdk.incubator.vector','-XX:ActiveProcessorCount=1','-Xmx512m','-Xbatch',
          '-cp',str(classes)+os.pathsep+str(workspace/'lib/*'),'WarmedCost']
    roots=PRIOR/'cost-roots.fens';reports=[];commands=[]
    for repeat,order in enumerate([models,list(reversed(models))]):
        for model in order:
            compute_gate.require()
            if time.monotonic()-start>900:raise RuntimeError('B003 comparative cost budget reached')
            command=java+[model['probe'],str(roots),model['network']];log=out/f'{repeat}-{model["id"]}.jsonl';err=log.with_suffix('.stderr')
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
        left=next(r for r in reports if r['repeat']==repeat and r['id']=='wdl25-production')['rows']
        right=next(r for r in reports if r['repeat']==repeat and r['id']=='wdl25-kernel')['rows']
        fields=['round','root','nodes','score','depth','completed','configuration']
        assert [[r[f] for f in fields] for r in left]==[[r[f] for f in fields] for r in right],'Equal-payload search divergence'
        equivalent.append(dict(repeat=repeat,requests=len(left),node_score_depth_completion_configuration='EXACT MATCH'))
    summary={model['id']:dict(nps_trials=[r['nps'] for r in reports if r['id']==model['id']],
              mean_nps=statistics.mean(r['nps'] for r in reports if r['id']==model['id'])) for model in models}
    result=dict(summary=summary,footprints=footprints,models=models,reports=reports,commands=commands,
        equal_payload_search_equivalence=equivalent,runtime_manifest_sha256=sha(manifest),roots_sha256=sha(roots),
        helper_source_sha256=sha(helper),helper_class_sha256=sha(classes/'WarmedCost.class'),elapsed_seconds=time.monotonic()-start,
        policy='Separate JVM per model/repeat; full warmup pass retained, second pass measured; reverse trial order on repeat2.',
        limitation='Learned evaluations change search trees. NPS is neither intrinsic kernel speedup nor Elo. Hardware cache/branch counters unavailable in this experiment.')
    write(BASE/'compact128-cost.json',result);write(out/'summary.json',result);print(json.dumps(summary,indent=2))


if __name__=='__main__':main()
