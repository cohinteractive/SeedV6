"""Execute each prospective deeper-v1 audit once against immutable runtime-v3."""
import json
import os
import subprocess
import sys
import time
from run_observation_v1 import HERE, ROOT, digest, write_new

mode=sys.argv[1];assert mode in ('null-cost','omissions')
name='deeper-'+mode+'-v1'
input=HERE/('raw/complement-probes-v1.jsonl' if mode=='null-cost' else 'raw/deeper-observe-v1.jsonl')
binding=json.loads((HERE/'deeper-v1-binding.json').read_text())
for p,sha in binding['files'].items():assert digest(ROOT/p)==sha,p
if mode=='omissions':assert json.loads((HERE/'deeper-observe-v1-execution.json').read_text())['status']=='passed'
files=[HERE/'deeper-v1.json',input,ROOT/'research/src/main/java/com/ohinteractive/seedv6/search/exact/Cp64SelectivityAudit.java',
       ROOT/'research/src/main/java/com/ohinteractive/seedv6/search/exact/Cp64SelectivityProbe.java']
write_new(HERE/(name+'-binding.json'),{'runtimeBindingSha256':digest(HERE/'deeper-v1-binding.json'),
    'runnerSha256':digest(__file__),'files':{str(p.relative_to(ROOT)):digest(p) for p in files}})
runtime=HERE/'raw/runtime-v3';output=HERE/'raw'/(name+'.jsonl');log=HERE/'raw'/(name+'.log')
command=['C:/java/jdk-21/bin/java.exe','--add-modules=jdk.incubator.vector','-Djava.awt.headless=true',
    '-Xms256m','-Xmx1024m','-XX:ActiveProcessorCount=4','-cp',str(runtime/'classes')+os.pathsep+str(runtime/'lib/*'),
    'com.ohinteractive.seedv6.search.exact.Cp64SelectivityAudit',mode,
    str(ROOT/'build-assets/nnue-candidate/conventional-cp64.cnnue'),
    str(ROOT/'docs/research/search/p3-construction-2026-10-20/diagnostic-corpus.tsv'),str(input),str(output)]
receipt={'status':'running','command':command,'bindingSha256':digest(HERE/(name+'-binding.json'))};started=time.monotonic()
try:
    with log.open('xb') as stream:
        process=subprocess.Popen(command,cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT,
            creationflags=subprocess.CREATE_NO_WINDOW|subprocess.BELOW_NORMAL_PRIORITY_CLASS)
        try:receipt['exitCode']=process.wait(timeout=1800)
        except BaseException:process.kill();process.wait();raise
    assert receipt['exitCode']==0
    rows=[json.loads(x) for x in output.read_text().splitlines()];assert rows[-1]['status']=='passed'
    for p,sha in binding['files'].items():assert digest(ROOT/p)==sha,p
    if mode=='null-cost':
        old=[json.loads(x) for x in (HERE/'raw/opportunity-d8-w1-v1.jsonl').read_text().splitlines()]
        expected={r['id']:r['plain']['outcome'] for r in old if r.get('type')=='observation' and r['profile']=='baseline'}
        probes=[r for r in (json.loads(x) for x in input.read_text().splitlines()) if r.get('type')=='probe' and r['sample']['kind']=='null-opportunity']
        actual={};requests=0
        for r in rows:
            if r.get('type')!='null-cost':continue
            assert r['request']['outcome']==expected[r['id']],r['id'];requests+=1
            for t in r['targets']:actual[(r['id'],t['index'])]=t
        assert requests==12 and len(actual)==48 and all(t['entered'] for t in actual.values())
        comparisons=[]
        for p in probes:
            t=actual[(p['id'],p['sampleIndex'])];s=p['sample'];v=p['verification'];n=p['null']
            accepted=n['captured'] and n['prediction']>=s['beta']
            verified=accepted and v['result']['completed'] and v['parentRelativeScore']>=s['beta']
            comparisons.append({'id':p['id'],'sampleIndex':p['sampleIndex'],'depth':s['depth'],'rawSurplus':s['raw']-s['beta'],
                'nullFailHigh':accepted,'verifiedFailHigh':verified,'probeNodes':n['nodes'],
                'verificationNodes':v['result']['nodes'] if accepted else 0,'incumbent':{k:t[k] for k in ('completed','nodes','score')},
                'incumbentFailHigh':t['completed'] and t['score']>=s['beta']})
        accepted=[c for c in comparisons if c['verifiedFailHigh']]
        receipt.update(defaultOutcomeParity=True,requests=requests,targets=48,completed=sum(t['completed'] for t in actual.values()),
            comparisons=comparisons,allProbeNodes=sum(c['probeNodes'] for c in comparisons),
            verificationNodes=sum(c['verificationNodes'] for c in comparisons),
            knownSavedIncumbentNodes=sum(c['incumbent']['nodes'] for c in accepted if c['incumbent']['completed']),
            unknownAccepted=sum(not c['incumbent']['completed'] for c in accepted))
        receipt['economicLimit']='Conditional sample only. Probe includes its restricted legal parent overhead; verification is retained unpruned legalD-2. Incumbent costs are completed subtrees in original complete requests, not independent population or whole-tree counterfactual savings.'
    else:
        receipt['profiles']={}
        for r in rows:
            if r.get('type')!='omission-audit':continue
            p=receipt['profiles'].setdefault(r['profile'],{'samples':0,'reducedCompleted':0,'nominalCompleted':0,
                'reducedNodes':0,'nominalNodes':0,'quadrants':{},'adverse':[],'historySigns':{}})
            p['samples']+=1;s=r['omission']['sample'];sign='negative' if s['history']<0 else 'zero'
            p['historySigns'][sign]=p['historySigns'].get(sign,0)+1
            for kind in ('reduced','nominal'):
                p[kind+'Completed']+=int(r[kind]['result']['completed']);p[kind+'Nodes']+=r[kind]['result']['nodes']
            if r['reduced']['result']['completed'] and r['nominal']['result']['completed']:
                reduced=r['reduced']['parentRelativeScore']>s['alpha'];nominal=r['nominal']['parentRelativeScore']>s['alpha']
                key=f'reducedRaises={reduced};nominalRaises={nominal}';p['quadrants'][key]=p['quadrants'].get(key,0)+1
                if reduced or nominal:p['adverse'].append(r)
    receipt['status']='passed'
except BaseException as e:receipt.update(status='failed',failure=repr(e));raise
finally:
    receipt['wallSeconds']=time.monotonic()-started
    receipt['evidence']={str(p.relative_to(HERE)):digest(p) for p in (output,log) if p.exists()}
    write_new(HERE/(name+'-summary.json'),receipt)
print(json.dumps({k:v for k,v in receipt.items() if k not in ('command','comparisons','profiles')},indent=2))
