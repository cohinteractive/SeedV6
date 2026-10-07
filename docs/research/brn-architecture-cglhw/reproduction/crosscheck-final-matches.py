"""Supplemental fixed-sample final identity, accounting and bounded-pair checks."""
import hashlib
import json
import math
from pathlib import Path
import sys

root=Path('docs/research/brn-architecture-cglhw')
def read(p):return json.loads(Path(p).read_text(encoding='utf-8'))
def sha(p):
    with Path(p).open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
assert len(sys.argv)==2,'EXPECTED_FINAL_MATCH_PLAN_SHA256'
expected=sys.argv[1];path=root/'z01-final-match-plan.json';assert sha(path)==expected;plan=read(path)
scores_path=root/'z01-final-match-results.json';resources_path=root/'z01-final-match-resources.json'
scores=read(scores_path);resources=read(resources_path)
assert scores['planSha256']==resources['planSha256']==expected
assert scores['analysisSourceSha256']==sha('tools/analyze-brn-matches.py')
assert resources['analysisSourceSha256']==sha('tools/audit-brn-match-resources.py')
assert resources['allBindingChecksPassed'] and resources['allPairFilesPresent'] and resources['recordedGames']==3072
assert plan['plannedGames']==3072 and len(plan['runs'])==768 and plan['requireMetadataIdentity']
assert set(scores['comparisons'])==set(plan['comparisons']) and len(scores['comparisons'])==6
for files in [plan['frozenFiles'],*plan['frozenCode'].values()]:
    for p,h in files.items():assert sha(p)==h
tested=read('app/build/research/brn-architecture/c02-auditor-tests-01/result.json')
assert tested['allChecksPassed']
for p,h in tested['sourceSha256'].items():assert sha(p)==h
reference=read('app/build/research/brn-architecture/z01-engine-scaling-compiled-600-vs-brn-211-1200.execution.json')
total=0;metadata=[]
for spec in plan['runs']:
    run=Path(spec['run']);execution=read(str(run)+'.execution.json');value=read(run/'result.json');config=read(run/'config.json')
    assert execution['status']=='completed' and execution['exitCode']==0
    for field,files in plan['frozenCode'].items():assert execution[field]==files==reference[field]
    for field in ['java','platform']:assert execution[field]==reference[field]
    assert execution['command'][:8]==reference['command'][:8]
    assert config['arguments']==['match']+spec['arguments']
    assert execution['command'][-len(config['arguments']):]==config['arguments']
    assert execution['timeoutSeconds']==900 and value['modelIdentitiesStable']
    assert value['recordedPairs']==value['plannedPairs']==2
    for field in ['candidateModelSha256','opponentModelSha256','candidateMetadataIdentity','opponentMetadataIdentity','candidateGain','opponentGain']:
        assert value[field]==config[field]
    total+=execution['elapsedSeconds']
    metadata.append({'run':run.as_posix(),'receiptSha256':sha(str(run)+'.execution.json'),'resultSha256':sha(run/'result.json')})
assert abs(total-sum(r['processSeconds'] for r in resources['provenance']))<1e-6
rows={}
for name,value in scores['comparisons'].items():
    assert value['plannedPairs']==value['recordedPairs']==256 and value['initializations']==['211','337']
    assert value['gainsByInitialization']==plan['gains'][name]
    assert value['metadataIdentitiesByInitialization']==plan['modelMetadataIdentities'][name]
    row={k:value[k] for k in ['complete','winsDrawsLossesCompletePairs','missingOrIncompletePairs','allPlannedPairsScoreBounds','maximumObservedPlies']}
    adverse_mean=value['allPlannedPairsScoreBounds'][0]
    row['conditionalBoundedPairOneSided95LowerWithMissingAdverse']=max(0,adverse_mean-math.sqrt(math.log(20)/(2*128)))
    row['conditionalBoundedPairSimultaneousSix95LowerWithMissingAdverse']=max(0,adverse_mean-math.sqrt(math.log(120)/(2*128)))
    row['bootstrapStrengthCriterionMet']=False
    row['bootstrapNoninferiorityNumericCriterionMet']=False
    if value['complete']:
        row.update(score=value['completedPairPointScore'],twoWay95=value['initializationAndOpeningBootstrap95'])
        row['bootstrapStrengthCriterionMet']=row['twoWay95'][0]>.5
        row['bootstrapNoninferiorityNumericCriterionMet']=row['twoWay95'][0]>=.45
        assert abs(row['conditionalBoundedPairOneSided95LowerWithMissingAdverse']-value['hoeffdingOneSided95LowerConditionalOnModels'])<1e-12
    rows[name]=row
report={'schema':'brn-architecture-final-match-crosscheck-v1','allChecksPassed':True,'planSha256':expected,
    'scoresSha256':sha(scores_path),'resourcesSha256':sha(resources_path),'auditorSha256':sha(__file__),
    'recordedGames':3072,'processSeconds':total,'comparisons':rows,
    'allSixBootstrapStrengthCriteriaMet':all(r['bootstrapStrengthCriterionMet'] for r in rows.values()),
    'allSixSimultaneousBoundedStrengthCriteriaMet':all(r['conditionalBoundedPairSimultaneousSix95LowerWithMissingAdverse']>.5 for r in rows.values()),
    'provenance':metadata,'scope':'Complete fixed allocation, with any unscored games adverse. Approximate unadjusted two-way bootstrap; only two training runs. Bounded-pair checks conditional on fixed models and independent generated opening clusters, not corpus-source uncertainty. Numeric noninferiority alone is not a recommendation: measured practical benefit is additionally required. No production adoption.'}
out=root/'z01-final-match-crosscheck.json'
with out.open('x',encoding='utf-8') as f:json.dump(report,f,indent=2);f.write('\n')
print(json.dumps({k:v for k,v in report.items() if k!='provenance'},indent=2))
