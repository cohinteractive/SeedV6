"""Execute/audit the already frozen sealed allocation after final match audits."""
import hashlib
import json
import math
from pathlib import Path
import subprocess
import sys

root=Path('docs/research/brn-architecture-cglhw')
def read(p):return json.loads(Path(p).read_text(encoding='utf-8'))
def sha(p):
    with Path(p).open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
def write(p,v):
    with p.open('x',encoding='utf-8') as f:json.dump(v,f,indent=2);f.write('\n')
path=root/'z01-final-quality-plan.json'
expected='296e6b6ffbc5ed3a022952e854493cb0bf278dac8d4e018635673bf5429182cd'
assert sha(path)==expected;plan=read(path)
assert plan['finalDecisionsFrozen'] and plan['protocol']=='sealed-final-v1'
assert read(root/'z01-final-match-crosscheck.json')['allChecksPassed']
matches=read(root/'z01-final-match-results.json')
assert len(matches['comparisons'])==6 and all(v['recordedPairs']==v['plannedPairs']==256 for v in matches['comparisons'].values())
output=root/'z01-final-quality-results.json';assert not output.exists()
execution_plan={'schema':'brn-architecture-quality-execution-plan-v1','qualityPlan':path.as_posix(),
                'qualityPlanSha256':expected,'controllerSha256':sha(__file__),'runs':[]}
for key,entry in plan['entries'].items():
    args=[path.as_posix(),expected,key,entry['output'],'sealed-final-v1']
    execution_plan['runs'].append({'profile':key,'mode':'quality','run':entry['output'],'arguments':args})
execution_path=root/'z01-final-quality-execution-plan.json'
if execution_path.exists():assert read(execution_path)==execution_plan
else:write(execution_path,execution_plan)
def frozen():
    assert sha(path)==expected and sha(__file__)==execution_plan['controllerSha256']
    for files in [plan['frozenFiles'],*plan['frozenCode'].values()]:
        for p,h in files.items():assert sha(p)==h,'Frozen file changed: '+p
def finite(value):
    if isinstance(value,dict):
        for v in value.values():finite(v)
    elif isinstance(value,list):
        for v in value:finite(v)
    elif isinstance(value,(float,int)) and not isinstance(value,bool):assert math.isfinite(value)
def numeric_difference(a,b):
    if isinstance(a,dict):
        assert isinstance(b,dict) and a.keys()==b.keys()
        return max((numeric_difference(a[k],b[k]) for k in a),default=0)
    if isinstance(a,list):
        assert isinstance(b,list) and len(a)==len(b)
        return max((numeric_difference(x,y) for x,y in zip(a,b)),default=0)
    if isinstance(a,(float,int)) and not isinstance(a,bool):return abs(a-b)
    assert a==b
    return 0
report={'schema':'brn-architecture-sealed-quality-results-v1','allChecksPassed':True,
        'planSha256':expected,'executionPlanSha256':sha(execution_path),
        'profiles':{},'processSeconds':0,'scope':'Full sealed populations; descriptive quality only. All model/gain/family decisions fixed before unsealing. Unknown native historical exposure remains unknown.'}
reference=read('app/build/research/brn-architecture/z01-qsearch-brn-211.execution.json')
for i,spec in enumerate(execution_plan['runs'],1):
    frozen();run=Path(spec['run']);receipt=Path(str(run)+'.execution.json');key=spec['profile'];entry=plan['entries'][key]
    if not receipt.exists():
        assert not run.exists()
        print(f'START {i}/22 {key}',flush=True)
        subprocess.run([sys.executable,'-B','tools/brn-architecture.py','--timeout','300','--receipt',str(receipt),'quality',*spec['arguments']],check=True)
    execution=read(receipt);value=read(run/'result.json');frozen()
    assert execution['status']=='completed' and execution['exitCode']==0 and execution['timeoutSeconds']==300
    assert execution['command'][-5:]==spec['arguments']
    assert execution['command'][:7]==reference['command'][:7]
    assert execution['command'][7]=='com.ohinteractive.seedv6.training.service.BrnArchitectureQuality'
    for field,files in plan['frozenCode'].items():assert execution[field]==files
    for field in ['java','platform']:assert execution[field]==reference[field]
    assert execution['processMemory']['readFailures']==0 and execution['processMemory']['postExitSampleSucceeded']
    assert value['schema']=='brn-architecture-quality-v1' and value['planSha256']==expected
    assert value['entryKey']==key and value['entry']==entry and value['protocol']=='sealed-final-v1'
    assert value['modelIdentityStable'] and value['modelSha256']==entry['modelSha256']
    assert value['modelMetadataIdentity']==entry['modelMetadataIdentity']
    metrics=value['metrics'];reliability=value['reliability'];finite(metrics);finite(reliability)
    assert metrics['positions']==entry['count']==32768
    for strata in ['phaseStrata','materialStrata']:
        assert sum(r['positions'] for r in metrics[strata].values())==32768
        weighted=sum(r['positions']*r['outcomeHalfMse'] for r in metrics[strata].values())/32768
        assert abs(weighted-metrics['outcomeHalfMse'])<1e-12
    for mapping in ['raw','calibratedSearch']:
        rel=reliability[mapping]
        assert rel['positions']==32768 and sum(r['positions'] for r in rel['bins'])==32768
        gap=sum(r['positions']*abs(r.get('signedGap',0)) for r in rel['bins'])/32768
        assert abs(gap-rel['weightedAbsoluteBinGap'])<1e-12
    report['profiles'][key]={'entry':entry,'metrics':metrics,'reliability':reliability,
        'processSeconds':execution['elapsedSeconds'],'processMemory':execution['processMemory'],
        'provenance':{'run':run.as_posix(),'resultSha256':sha(run/'result.json'),'receiptSha256':sha(receipt)}}
    report['processSeconds']+=execution['elapsedSeconds']
    print(f'COMPLETE {i}/22 {key}',flush=True)
report['pairRepresentationMaximumMetricDifferences']={}
for seed in [211,337]:
    a,b=[report['profiles'][f'{family}-{seed}'] for family in ['tuple2','compiled-pair']]
    difference=max(numeric_difference(a[k],b[k]) for k in ['metrics','reliability'])
    assert difference<=1e-12
    report['pairRepresentationMaximumMetricDifferences'][str(seed)]=difference
report['totalPositionVisits']=22*32768
report['distinctSealedPositions']=2*32768
write(output,report)
print(json.dumps({'allChecksPassed':True,'profiles':22,'positionVisits':22*32768,'processSeconds':report['processSeconds'],'sha256':sha(output)}))
