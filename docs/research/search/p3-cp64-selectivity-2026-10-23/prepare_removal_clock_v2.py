"""Freeze H1 resource revision, compile only its measurement shim, then qualify."""
import json,subprocess,time
from pathlib import Path
from run_observation_v1 import HERE,ROOT,digest,write_new
import removal_resource_v2 as resource

for name in ['null-clock-primary-v2','null-clock-increment-v2']:
    assert (HERE/(name+'-summary.json')).exists(),'Finish active null work and post-delivery first'
assert not (HERE/'raw/clock-development-v1').exists(),'Original H1 must remain unplayed'
old=json.loads((HERE/'clock-development-v1-binding.json').read_text())
for p,sha in old['files'].items():assert digest(ROOT/p)==sha,p
for p,sha in old['externalFiles'].items():assert digest(p)==sha,p
plan=json.loads((HERE/'clock-development-v1.json').read_text());plan['id']=plan['id'].replace('-v1','-v2')
plan['resourceAdmission']='Same prospective gates as null-clock-development-v2, with actual baseline/no-improving profiles in the original runtime-v2. Pre/post owner-thread delivery per profile median>=.90, nearest-rank fifth percentile>=.80, median spread<=.05; use a passing identical calibration within15min or repeat before launch. Idle30s and whole-owned-run otherCPU<=2.0 logical cores, availableRAM>=8GiB/load<=75%. Each actor nativeCPU/Search-elapsed .90..1.25 and actor difference<=.10. One serial W1 game, BelowNormal; no computational overlap. Not perfect isolation.'
plan['openings']='Identical never-played clock-development-v1-openings.pgn,32 pairs, same seed2026102401/partition0/exclusions; no reselection or evaluator filtering. Prior failed prelaunch admissions and old1.0-core gate remain unchanged. Separate fresh partition1 confirmation if selected.'
plan['resourceRationale']='cpu-delivery-v1 and null primary pre/post demonstrate stable practical owner-thread delivery despite historical idle global-gate failures. Reassess the self-chosen resource premise before any H1 game; separately qualify actual H1 profiles using removal-cpu-v1. All original strength-selection/analysis rules and frozen build71 runtime remain unchanged.'
write_new(HERE/'removal-clock-v2.json',plan)
runtime=HERE/'raw/runtime-v2';classes=HERE/'raw/removal-cpu-v1-classes';classes.mkdir(exist_ok=False)
source=ROOT/'research/src/main/java/com/ohinteractive/seedv6/search/exact/Cp64RemovalCpuDelivery.java'
command=['C:/java/jdk-21/bin/javac.exe','--add-modules','jdk.incubator.vector','-cp',str(runtime/'lib/*'),'-d',str(classes),str(source)]
start=time.monotonic();log=HERE/'raw/removal-cpu-v1-compile.log'
with log.open('xb') as stream:
    result=subprocess.run(command,cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT,timeout=120,
        creationflags=subprocess.BELOW_NORMAL_PRIORITY_CLASS|subprocess.CREATE_NO_WINDOW)
write_new(HERE/'removal-cpu-v1-compile.json',{'command':command,'exitCode':result.returncode,'wallSeconds':time.monotonic()-start,
    'log':str(log.relative_to(HERE)),'logSha256':digest(log),'testsRun':0})
assert result.returncode==0
model=ROOT/'build-assets/nnue-candidate/conventional-cp64.cnnue';corpus=ROOT/'docs/research/search/sr006-phase2-2026-10-10/played-corpus.tsv'
files=[HERE/'removal-cpu-v1.json',source,Path(__file__),HERE/'removal_resource_v2.py',HERE/'clock_resource_v2.py',model,corpus]
files+=sorted(p for p in classes.rglob('*') if p.is_file())+sorted(p for p in runtime.rglob('*') if p.is_file())
base=['C:/java/jdk-21/bin/java.exe','--add-modules=jdk.incubator.vector','-Djava.awt.headless=true','-Xms256m','-Xmx1024m',
    '-XX:ActiveProcessorCount=4','-cp',str(classes)+';'+str(runtime/'classes')+';'+str(runtime/'lib/*'),
    'com.ohinteractive.seedv6.search.exact.Cp64RemovalCpuDelivery',str(model),str(corpus)]
write_new(HERE/'removal-cpu-v1-binding.json',{'files':{str(p):digest(p) for p in files},'commandBase':base})
calibration=resource.delivery('removal-cpu-v1');assert calibration['criteriaPassed'],'H1 delivery qualification failed'
allfiles=dict(old['files']);allfiles.update({str(p.relative_to(ROOT)):digest(p) for p in files})
for p in [HERE/'removal-clock-v2.json',HERE/'run_removal_clock_v2.py',HERE/'removal-cpu-v1-binding.json',HERE/'removal-cpu-v1-summary.json',HERE/'removal-cpu-v1-compile.json']:
    allfiles[str(p.relative_to(ROOT))]=digest(p)
write_new(HERE/'removal-clock-v2-binding.json',{'head':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),
    'files':allfiles,'externalFiles':old['externalFiles'],'sourcePlanSha256':digest(HERE/'clock-development-v1.json')})
print(json.dumps({'status':'prepared','calibrationPassed':True,'profiles':calibration['profiles']},indent=2))
