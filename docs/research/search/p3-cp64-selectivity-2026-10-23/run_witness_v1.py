"""Run the complete frozen prediction replay in a new immutable runtime snapshot."""
import json
import os
import shutil
import subprocess
import time
from run_observation_v1 import HERE, ROOT, digest, write_new

assert json.loads((HERE/'witness-build-v1-summary.json').read_text())['exitCode']==0
protocol=json.loads((HERE/'witness-v1.json').read_text())
runtime=HERE/'raw/witness-runtime-v1';shutil.copytree(ROOT/'build/research/runtime',runtime)
output=HERE/'raw/prediction-replay-v1.jsonl';log=HERE/'raw/prediction-replay-v1.log'
files=[HERE/'witness-v1.json',HERE/protocol['observations'],ROOT/protocol['corpus'],
       ROOT/'build-assets/nnue-candidate/conventional-cp64.cnnue',
       ROOT/'research/src/main/java/com/ohinteractive/seedv6/search/exact/Cp64SelectivityWitness.java']
files+=sorted(p for p in runtime.rglob('*') if p.is_file())
binding={'head':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),
         'files':{str(p.relative_to(ROOT)):digest(p) for p in files},'runnerSha256':digest(__file__)}
write_new(HERE/'witness-v1-binding.json',binding)
command=['C:/java/jdk-21/bin/java.exe','--add-modules=jdk.incubator.vector','-Djava.awt.headless=true',
         '-Xms256m','-Xmx768m','-XX:ActiveProcessorCount=4','-cp',str(runtime/'classes')+os.pathsep+str(runtime/'lib/*'),
         'com.ohinteractive.seedv6.search.exact.Cp64SelectivityWitness',
         str(ROOT/'build-assets/nnue-candidate/conventional-cp64.cnnue'),str(ROOT/protocol['corpus']),
         str(HERE/protocol['observations']),str(output),str(protocol['budgetPerSample'])]
receipt={'status':'running','command':command,'bindingSha256':digest(HERE/'witness-v1-binding.json')};started=time.monotonic()
try:
    with log.open('xb') as stream:
        process=subprocess.Popen(command,cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT,
            creationflags=subprocess.CREATE_NO_WINDOW|subprocess.BELOW_NORMAL_PRIORITY_CLASS)
        try:receipt['exitCode']=process.wait(timeout=1200)
        except BaseException:process.kill();process.wait();raise
    assert receipt['exitCode']==0
    rows=[json.loads(line) for line in output.read_text().splitlines()]
    assert rows[-1]['status']=='passed' and rows[-1]['samples']==protocol['samples']
    assert all(digest(ROOT/p)==sha for p,sha in binding['files'].items())
    audits=[r for r in rows if r.get('type')=='predictionAudit']
    receipt['byMechanism']={}
    for reverse in (False,True):
        selected=[r for r in audits if r['sample']['reverse']==reverse]
        receipt['byMechanism']['reverse' if reverse else 'futility']={
            'samples':len(selected),'completed':sum(r['reference']['completed'] for r in selected),
            'falseThresholds':sum(r['falseThreshold'] for r in selected),'nodes':sum(r['reference']['nodes'] for r in selected),
            'adverse':[{'id':r['id'],'sampleIndex':r['sampleIndex'],'depth':r['sample']['depth'],
                'raw':r['sample']['raw'],'alpha':r['sample']['alpha'],'beta':r['sample']['beta'],
                'score':r['parentRelativeScore']} for r in selected if r['falseThreshold']]}
    receipt['status']='passed'
except BaseException as error:receipt.update(status='failed',failure=repr(error));raise
finally:
    receipt['wallSeconds']=time.monotonic()-started
    receipt['evidence']={str(p.relative_to(HERE)):digest(p) for p in (output,log) if p.exists()}
    write_new(HERE/'witness-v1-summary.json',receipt)
print(json.dumps(receipt.get('byMechanism',{}),indent=2))
