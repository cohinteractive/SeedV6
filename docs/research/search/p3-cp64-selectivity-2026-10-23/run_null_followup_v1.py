"""Execute the prospectively selected late-position and unknown-reference challenge."""
import json
import os
import shutil
import subprocess
import time
from run_observation_v1 import HERE, ROOT, digest, write_new

assert json.loads((HERE/'null-followup-build-v1-summary.json').read_text())['exitCode']==0
runtime=HERE/'raw/null-followup-runtime-v1';shutil.copytree(ROOT/'build/research/runtime',runtime)
name='null-followup-v1'
network=ROOT/'build-assets/nnue-candidate/conventional-cp64.cnnue'
played=ROOT/'docs/research/search/sr006-phase2-2026-10-10/played-corpus.tsv'
original=ROOT/'docs/research/search/p3-construction-2026-10-20/diagnostic-corpus.tsv'
prior=HERE/'raw/complement-probes-v1.jsonl'
files=[HERE/(name+'.json'),network,played,original,prior,
    ROOT/'research/src/main/java/com/ohinteractive/seedv6/search/exact/Cp64NullFollowup.java',
    ROOT/'research/src/main/java/com/ohinteractive/seedv6/search/exact/Cp64SelectivityProbe.java',
    ROOT/'research/src/main/java/com/ohinteractive/seedv6/search/exact/Cp64SelectivityAudit.java']
files+=sorted(p for p in runtime.rglob('*') if p.is_file())
binding={'head':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),'runnerSha256':digest(__file__),
    'files':{str(p.relative_to(ROOT)):digest(p) for p in files}}
write_new(HERE/(name+'-binding.json'),binding)
output=HERE/'raw'/(name+'.jsonl');log=HERE/'raw'/(name+'.log')
command=['C:/java/jdk-21/bin/java.exe','--add-modules=jdk.incubator.vector','-Djava.awt.headless=true',
    '-Xms256m','-Xmx1024m','-XX:ActiveProcessorCount=4','-cp',str(runtime/'classes')+os.pathsep+str(runtime/'lib/*'),
    'com.ohinteractive.seedv6.search.exact.Cp64NullFollowup',str(network),str(played),str(original),str(prior),str(output)]
receipt={'status':'running','command':command,'bindingSha256':digest(HERE/(name+'-binding.json'))};start=time.monotonic()
try:
    with log.open('xb') as stream:
        process=subprocess.Popen(command,cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT,
            creationflags=subprocess.CREATE_NO_WINDOW|subprocess.BELOW_NORMAL_PRIORITY_CLASS)
        try:receipt['exitCode']=process.wait(timeout=1800)
        except BaseException:process.kill();process.wait();raise
    assert receipt['exitCode']==0
    rows=[json.loads(x) for x in output.read_text().splitlines()];assert rows[-1]['status']=='passed'
    observed=[r for r in rows if r.get('type')=='observation'];assert len(observed)==12 and all(r['neutral'] for r in observed)
    probes=[r for r in rows if r.get('type')=='late-null-probe'];assert len(probes)==rows[-1]['lateSamples']
    receipt.update(roots=12,samples=len(probes),captured=sum(r['null']['captured'] for r in probes),
        nominalCompleted=sum(r['nominal']['result']['completed'] for r in probes),
        incumbentCompleted=sum(r['target']['completed'] for r in probes),allProbeNodes=sum(r['null']['nodes'] for r in probes),
        neutral=True,byVerifier={},priorUnknownFollowup=[r for r in rows if r.get('type')=='prior-unknown-followup'])
    for kind in ('unprunedVerification','selectiveVerification'):
        nullHigh=[r for r in probes if r['null']['captured'] and r['null']['prediction']>=r['target']['sample']['beta']]
        accepted=[r for r in nullHigh if r[kind]['result']['completed'] and r[kind]['parentRelativeScore']>=r['target']['sample']['beta']]
        adverse=[r for r in accepted if r['nominal']['result']['completed'] and r['nominal']['parentRelativeScore']<r['target']['sample']['beta']]
        receipt['byVerifier'][kind]={'completed':sum(r[kind]['result']['completed'] for r in probes),
            'nullFailHigh':len(nullHigh),'accepted':len(accepted),'chargedNodes':sum(r[kind]['result']['nodes'] for r in nullHigh),
            'knownSavedIncumbentNodes':sum(r['target']['nodes'] for r in accepted if r['target']['completed']),
            'unknownAcceptedIncumbents':sum(not r['target']['completed'] for r in accepted),
            'unknownAcceptedNominal':sum(not r['nominal']['result']['completed'] for r in accepted),
            'falseThresholds':len(adverse),'adverse':adverse}
    assert all(digest(ROOT/p)==sha for p,sha in binding['files'].items());receipt['status']='passed'
except BaseException as e:receipt.update(status='failed',failure=repr(e));raise
finally:
    receipt['wallSeconds']=time.monotonic()-start
    receipt['evidence']={str(p.relative_to(HERE)):digest(p) for p in (output,log) if p.exists()}
    write_new(HERE/(name+'-summary.json'),receipt)
print(json.dumps({k:v for k,v in receipt.items() if k not in ('command','priorUnknownFollowup','byVerifier')},indent=2))
print(json.dumps({k:{a:b for a,b in v.items() if a!='adverse'} for k,v in receipt['byVerifier'].items()},indent=2))
