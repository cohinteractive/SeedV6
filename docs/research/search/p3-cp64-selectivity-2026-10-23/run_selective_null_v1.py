"""Freeze and execute the all-target selective legal verifier sensitivity."""
import json
import os
import shutil
import subprocess
import time
from run_observation_v1 import HERE, ROOT, digest, write_new

assert json.loads((HERE/'selective-null-build-v1-summary.json').read_text())['exitCode']==0
name='selective-null-verification-v1';runtime=HERE/'raw/selective-null-runtime-v1'
shutil.copytree(ROOT/'build/research/runtime',runtime)
input=HERE/'raw/complement-probes-v1.jsonl';cost=HERE/'deeper-null-cost-v1-summary.json'
corpus=ROOT/'docs/research/search/p3-construction-2026-10-20/diagnostic-corpus.tsv'
files=[HERE/(name+'.json'),input,cost,corpus,ROOT/'research/src/main/java/com/ohinteractive/seedv6/search/exact/Cp64SelectiveNullVerification.java',
    ROOT/'build-assets/nnue-candidate/conventional-cp64.cnnue']+sorted(p for p in runtime.rglob('*') if p.is_file())
binding={'head':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),'runnerSha256':digest(__file__),
    'files':{str(p.relative_to(ROOT)):digest(p) for p in files}}
write_new(HERE/(name+'-binding.json'),binding)
output=HERE/'raw'/(name+'.jsonl');log=HERE/'raw'/(name+'.log')
command=['C:/java/jdk-21/bin/java.exe','--add-modules=jdk.incubator.vector','-Djava.awt.headless=true',
    '-Xms256m','-Xmx768m','-XX:ActiveProcessorCount=4','-cp',str(runtime/'classes')+os.pathsep+str(runtime/'lib/*'),
    'com.ohinteractive.seedv6.search.exact.Cp64SelectiveNullVerification',str(files[5]),str(corpus),str(input),str(output)]
receipt={'status':'running','command':command,'bindingSha256':digest(HERE/(name+'-binding.json'))};started=time.monotonic()
try:
    with log.open('xb') as stream:
        process=subprocess.Popen(command,cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT,
            creationflags=subprocess.CREATE_NO_WINDOW|subprocess.BELOW_NORMAL_PRIORITY_CLASS)
        try:receipt['exitCode']=process.wait(timeout=1800)
        except BaseException:process.kill();process.wait();raise
    assert receipt['exitCode']==0
    rows=[json.loads(x) for x in output.read_text().splitlines()];assert rows[-1]['status']=='passed'
    original={(r['id'],r['sampleIndex']):r for r in (json.loads(x) for x in input.read_text().splitlines()) if r.get('type')=='probe'}
    incumbent={(r['id'],r['sampleIndex']):r for r in json.loads(cost.read_text())['comparisons']}
    comparisons=[]
    for r in rows:
        if r.get('type')!='selective-verification':continue
        key=(r['id'],r['sampleIndex']);old=original[key];beta=r['sample']['beta'];n=old['null']
        nullHigh=n['captured'] and n['prediction']>=beta;high=r['result']['completed'] and r['parentRelativeScore']>=beta
        comparisons.append({'id':r['id'],'sampleIndex':r['sampleIndex'],'depth':r['sample']['depth'],
            'nullFailHigh':nullHigh,'selectiveCompleted':r['result']['completed'],'selectiveFailHigh':high,
            'selectiveNodes':r['result']['nodes'],'unprunedFailHigh':old['verification']['parentRelativeScore']>=beta,
            'nominalCompleted':old['nominal']['result']['completed'],'nominalFailHigh':old['nominal']['parentRelativeScore']>=beta,
            'incumbentNodes':incumbent[key]['incumbent']['nodes'],'extensionPlies':r['result']['maximumExtensionPlies']})
    assert len(comparisons)==48
    accepted=[r for r in comparisons if r['nullFailHigh'] and r['selectiveFailHigh']]
    receipt.update(samples=48,completed=sum(r['selectiveCompleted'] for r in comparisons),comparisons=comparisons,
        changedVerificationThresholds=sum(r['selectiveCompleted'] and r['selectiveFailHigh']!=r['unprunedFailHigh'] for r in comparisons),
        accepted=len(accepted),nominalUnknown=sum(not r['nominalCompleted'] for r in accepted),
        nominalFalseThresholds=sum(r['nominalCompleted'] and not r['nominalFailHigh'] for r in accepted),
        allProbeNodes=4314,verificationNodesCharged=sum(r['selectiveNodes'] for r in comparisons if r['nullFailHigh']),
        savedIncumbentNodes=sum(r['incumbentNodes'] for r in accepted))
    assert all(digest(ROOT/p)==sha for p,sha in binding['files'].items());receipt['status']='passed'
except BaseException as e:receipt.update(status='failed',failure=repr(e));raise
finally:
    receipt['wallSeconds']=time.monotonic()-started
    receipt['evidence']={str(p.relative_to(HERE)):digest(p) for p in (output,log) if p.exists()}
    write_new(HERE/(name+'-summary.json'),receipt)
print(json.dumps({k:v for k,v in receipt.items() if k not in ('command','comparisons')},indent=2))
