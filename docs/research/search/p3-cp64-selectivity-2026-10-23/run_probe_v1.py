"""Run the complete frozen prediction replay in a new immutable runtime snapshot."""
import json
import os
import shutil
import subprocess
import time
from run_observation_v1 import HERE, ROOT, digest, write_new

assert json.loads((HERE/'probe-build-v1-summary.json').read_text())['exitCode']==0
protocol=json.loads((HERE/'probe-v1.json').read_text())
runtime=HERE/'raw/probe-runtime-v1';shutil.copytree(ROOT/'build/research/runtime',runtime)
output=HERE/'raw/complement-probes-v1.jsonl';log=HERE/'raw/complement-probes-v1.log'
files=[HERE/'probe-v1.json',HERE/protocol['input'],ROOT/protocol['corpus'],
       ROOT/'build-assets/nnue-candidate/conventional-cp64.cnnue',
       ROOT/'research/src/main/java/com/ohinteractive/seedv6/search/exact/Cp64SelectivityProbe.java']
files+=sorted(p for p in runtime.rglob('*') if p.is_file())
binding={'head':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),
         'files':{str(p.relative_to(ROOT)):digest(p) for p in files},'runnerSha256':digest(__file__)}
write_new(HERE/'probe-v1-binding.json',binding)
command=['C:/java/jdk-21/bin/java.exe','--add-modules=jdk.incubator.vector','-Djava.awt.headless=true',
         '-Xms256m','-Xmx768m','-XX:ActiveProcessorCount=4','-cp',str(runtime/'classes')+os.pathsep+str(runtime/'lib/*'),
         'com.ohinteractive.seedv6.search.exact.Cp64SelectivityProbe',
         str(ROOT/'build-assets/nnue-candidate/conventional-cp64.cnnue'),str(ROOT/protocol['corpus']),
         str(HERE/protocol['input']),str(output),str(protocol['nominalBudget']),str(protocol['probeBudget'])]
receipt={'status':'running','command':command,'bindingSha256':digest(HERE/'probe-v1-binding.json')};started=time.monotonic()
try:
    with log.open('xb') as stream:
        process=subprocess.Popen(command,cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT,
            creationflags=subprocess.CREATE_NO_WINDOW|subprocess.BELOW_NORMAL_PRIORITY_CLASS)
        try:receipt['exitCode']=process.wait(timeout=1800)
        except BaseException:process.kill();process.wait();raise
    assert receipt['exitCode']==0
    rows=[json.loads(line) for line in output.read_text().splitlines()]
    assert rows[-1]['status']=='passed' and rows[-1]['samples']==83
    assert all(digest(ROOT/p)==sha for p,sha in binding['files'].items())
    audits=[r for r in rows if r.get('type')=='probe']
    receipt['byMechanism']={}
    null=[r for r in audits if r['sample']['kind']=='null-opportunity']
    fut=[r for r in audits if r['sample']['kind']=='reduction-futility-opportunity']
    assert len(null)==48 and len(fut)==35
    def complete(r,k):return r[k]['result']['completed']
    def high(r,k):return r[k]['parentRelativeScore']>=r['sample']['beta']
    captured=[r for r in null if r['null']['captured']]
    accepted=[r for r in captured if r['null']['prediction']>=r['sample']['beta']]
    verified=[r for r in accepted if complete(r,'verification') and high(r,'verification')]
    receipt['byMechanism']['null']={'samples':len(null),'captured':len(captured),
        'nullFailHigh':len(accepted),'verificationCompleted':sum(complete(r,'verification') for r in null),
        'nominalCompleted':sum(complete(r,'nominal') for r in null),'verifiedFailHigh':len(verified),
        'nullFalseThresholds':sum(complete(r,'nominal') and not high(r,'nominal') for r in accepted),
        'verifiedFalseThresholds':sum(complete(r,'nominal') and not high(r,'nominal') for r in verified),
        'probeNodes':sum(r['null']['nodes'] for r in null),
        'verificationNodesOnNullFailHigh':sum(r['verification']['result']['nodes'] for r in accepted),
        'nominalNodes':sum(r['nominal']['result']['nodes'] for r in null)}
    quadrants={};adverse=[]
    for r in fut:
        if complete(r,'reduced') and complete(r,'nominal'):
            rh=r['reduced']['parentRelativeScore']>r['sample']['alpha'];nh=r['nominal']['parentRelativeScore']>r['sample']['alpha']
            key=f'reducedRaises={rh};nominalRaises={nh}';quadrants[key]=quadrants.get(key,0)+1
            if nh:adverse.append({'id':r['id'],'sampleIndex':r['sampleIndex'],'sample':r['sample'],
                'reducedScore':r['reduced']['parentRelativeScore'],'nominalScore':r['nominal']['parentRelativeScore']})
    receipt['byMechanism']['reductionFutility']={'samples':len(fut),'reducedCompleted':sum(complete(r,'reduced') for r in fut),
        'nominalCompleted':sum(complete(r,'nominal') for r in fut),'quadrants':quadrants,'adverse':adverse,
        'alsoPassNominalChildMargin':sum(r['sample']['raw']+100*(r['sample']['depth']-1)<=r['sample']['alpha'] for r in fut),
        'reducedNodes':sum(r['reduced']['result']['nodes'] for r in fut),'nominalNodes':sum(r['nominal']['result']['nodes'] for r in fut)}
    receipt['status']='passed'
except BaseException as error:receipt.update(status='failed',failure=repr(error));raise
finally:
    receipt['wallSeconds']=time.monotonic()-started
    receipt['evidence']={str(p.relative_to(HERE)):digest(p) for p in (output,log) if p.exists()}
    write_new(HERE/'probe-v1-summary.json',receipt)
print(json.dumps(receipt.get('byMechanism',{}),indent=2))
