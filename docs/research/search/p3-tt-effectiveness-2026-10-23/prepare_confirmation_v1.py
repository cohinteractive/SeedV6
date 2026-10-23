"""Freeze new CP64 block studies; creates no games and never replaces inputs."""
import importlib.util
import json
from pathlib import Path
import shutil
import sys
from run_diagnostic_v1 import digest, write_new, ROOT, HERE

spec=importlib.util.spec_from_file_location('baseline',ROOT/'tools/search-baseline.py')
b=importlib.util.module_from_spec(spec);spec.loader.exec_module(b)
validation=json.loads((HERE/'tt-session-validation-v1.json').read_text())
assert validation['status']=='passed'
assert all(digest(ROOT/p)==sha for p,sha in validation['files'].items())
binding=json.loads((HERE/'clock-runtime-v2-binding.json').read_text())
# This prior binding uses absolute paths; retain its original source metadata.
assert all(digest(p)==sha for p,sha in binding['files'].items())
exclusions=[HERE/name for name in ('clock-development-exclusions.json',
    'pilot-v1-openings.selection.json','development-clock-v1-openings.selection.json',
    'adaptive-clock-v1-openings.selection.json')]
exclusions.append(ROOT/'docs/research/search/p3-construction-2026-10-20/baseline-cp64/openings.selection.json')
book=Path('C:/projects/seed/java/seedv6/app/build/search-baseline/tools/openings/sr021-bjbraams/legal16-v1.pgn')
assert digest(book)=='eaf3c706dc6038d18a05c8608417a5ee2e3e43438487d73bbf2c7191183db42f'
for name,count,seed,partition in [('adaptive-confirmation-v1',128,2026102304,1),
                                   ('adaptive-requalification-v1',32,2026102305,0)]:
    b.select_openings(book,HERE/f'{name}-openings.pgn',count,seed,partition,exclusions)
bundle=HERE/'raw/tt-session-v1-tools';bundle.mkdir()
for name in ('tournament-process.py','p3-tt-match.py','search-baseline.py',
             'sr024-process-cost.py','sr036-process-cost.py'):
    shutil.copy2(ROOT/'tools'/name,bundle/name)
shutil.copy2(ROOT/'tools/p3-tt-session.py',bundle/'tournament-session.py')
spec=importlib.util.spec_from_file_location('session',bundle/'tournament-session.py')
s=importlib.util.module_from_spec(spec);spec.loader.exec_module(s)
studies={}
for label,workers,pairs,block,limit,opening,purpose,seed in [
    ('pilot',1,2,1,600,'pilot-v1','operational',2026102304),
    ('w1',1,128,32,3600,'adaptive-confirmation-v1','confirmatory',2026102304),
    ('requal-w4',4,32,32,3600,'adaptive-requalification-v1','exploratory',2026102305),
    ('w4',4,128,32,3600,'adaptive-confirmation-v1','confirmatory',2026102304)]:
    config={'purpose':purpose,'adapter':'p3-tt-match.py','blockPairs':block,
        'blockLimitSeconds':limit,'assessmentProtocol':'adaptive-confirmation-v1.md',
        'assessmentPlan':'adaptive-confirmation-v1.md; no interim strength looks. '+
             ('Operational pilot only.' if label=='pilot' else 'W4 confirmation requires quiet-host requalification.'),
        'arguments':{'java':'C:/java/jdk-21/bin/java.exe',
             'runner':str(ROOT/'build/research/tools/fastchess.exe'),
             'baseline':str(HERE/'raw/clock-runtime-v2'),'candidate':str(HERE/'raw/clock-runtime-v2'),
             'network':str(ROOT/'build-assets/nnue-candidate/conventional-cp64.cnnue'),
             'openings':str(HERE/f'{opening}-openings.pgn'),'baseline_policy':'baseline',
             'candidate_policy':'adaptive','baseline_workers':workers,'candidate_workers':workers,
             'concurrency':1,'hash_mib':64,'background':True,'pairs':pairs,'tc':'10+0.1','seed':seed}}
    config_path=HERE/f'tt-session-{label}-v1-config.json';write_new(config_path,config)
    output=HERE/f'raw/tt-session-{label}-v1'
    result=s.prepare(config_path,output)
    studies[label]={'plan':str((output/'plan.json').relative_to(HERE)),
        'planSha256':digest(output/'plan.json'),'identitySha256':digest(output/'identity.json'),
        'configSha256':digest(config_path),'pairs':pairs,'blocks':result['blocks'],
        'frozenPayloadFiles':len(s.tree(output/'inputs')),'status':'prepared; no games started'}
write_new(HERE/'tt-session-v1-binding.json',{'status':'prepared','studies':studies,
    'protocol':'adaptive-confirmation-v1.md','protocolSha256':digest(HERE/'adaptive-confirmation-v1.md'),
    'packagingSourceSha256':digest(ROOT/'tools/p3-tt-session.py'),
    'preparerSha256':digest(__file__),
    'runtimeBindingSha256':digest(HERE/'clock-runtime-v2-binding.json'),
    'selectionReceipts':{name:digest(HERE/name) for name in (
        'adaptive-confirmation-v1-openings.selection.json','adaptive-requalification-v1-openings.selection.json')},
    'prerequisites':'Operational two-block pilot with pause/resume/export before research games. W4 confirmation also requires its fresh32-pair resource-qualified development score>=0.55. All studies enforce quiet-host admission; preparation does not authorize bypassing prerequisites.'})
print(json.dumps(studies,indent=2))
