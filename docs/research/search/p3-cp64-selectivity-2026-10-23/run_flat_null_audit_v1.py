"""Replay every prospectively selected integrated null cutoff, including adverse/capped results."""
import json
import os
import subprocess
import time
from run_observation_v1 import HERE, ROOT, digest, write_new

name='flat-null-cutoff-audit-v1';input=HERE/'raw/flat-null-observe-v1.jsonl'
assert json.loads((HERE/'flat-null-observe-v1-execution.json').read_text())['status']=='passed'
binding=json.loads((HERE/'flat-null-v1-binding.json').read_text())
for p,sha in binding['files'].items():assert digest(ROOT/p)==sha,p
write_new(HERE/(name+'-binding.json'),{'runtimeBindingSha256':digest(HERE/'flat-null-v1-binding.json'),
    'runnerSha256':digest(__file__),'inputSha256':digest(input),'protocolSha256':digest(HERE/'flat-null-v1.json'),
    'auditSourceSha256':digest(ROOT/'research/src/main/java/com/ohinteractive/seedv6/search/exact/Cp64FlatNullAudit.java')})
runtime=HERE/'raw/runtime-v4';output=HERE/'raw'/(name+'.jsonl');log=HERE/'raw'/(name+'.log')
command=['C:/java/jdk-21/bin/java.exe','--add-modules=jdk.incubator.vector','-Djava.awt.headless=true',
    '-Xms256m','-Xmx1024m','-XX:ActiveProcessorCount=4','-cp',str(runtime/'classes')+os.pathsep+str(runtime/'lib/*'),
    'com.ohinteractive.seedv6.search.exact.Cp64FlatNullAudit',str(ROOT/'build-assets/nnue-candidate/conventional-cp64.cnnue'),
    str(ROOT/'docs/research/search/p3-construction-2026-10-20/diagnostic-corpus.tsv'),str(input),str(output)]
receipt={'status':'running','command':command,'bindingSha256':digest(HERE/(name+'-binding.json'))};start=time.monotonic()
try:
    with log.open('xb') as stream:
        process=subprocess.Popen(command,cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT,
            creationflags=subprocess.CREATE_NO_WINDOW|subprocess.BELOW_NORMAL_PRIORITY_CLASS)
        try:receipt['exitCode']=process.wait(timeout=1800)
        except BaseException:process.kill();process.wait();raise
    assert receipt['exitCode']==0
    rows=[json.loads(x) for x in output.read_text().splitlines()];assert rows[-1]['status']=='passed'
    receipt['profiles']={}
    for r in rows:
        if r.get('type')!='cutoff-audit':continue
        p=receipt['profiles'].setdefault(r['profile'],{'samples':0,'completed':0,'nodes':0,'falseThresholds':0,'adverse':[],'capped':[]})
        p['samples']+=1;p['nodes']+=r['nominal']['result']['nodes']
        if r['nominal']['result']['completed']:
            p['completed']+=1
            if r['nominal']['parentRelativeScore']<r['sample']['beta']:p['falseThresholds']+=1;p['adverse'].append(r)
        else:p['capped'].append(r)
    assert sum(p['samples'] for p in receipt['profiles'].values())==rows[-1]['samples']
    assert all(digest(ROOT/p)==sha for p,sha in binding['files'].items());receipt['status']='passed'
except BaseException as e:receipt.update(status='failed',failure=repr(e));raise
finally:
    receipt['wallSeconds']=time.monotonic()-start
    receipt['evidence']={str(p.relative_to(HERE)):digest(p) for p in (output,log) if p.exists()}
    write_new(HERE/(name+'-summary.json'),receipt)
print(json.dumps({k:{a:b for a,b in p.items() if a not in ('adverse','capped')} for k,p in receipt['profiles'].items()},indent=2))
