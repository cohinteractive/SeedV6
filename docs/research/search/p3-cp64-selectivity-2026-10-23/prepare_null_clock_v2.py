"""Freeze a new resource-qualified protocol before any clock games exist."""
import json, shutil, subprocess
from pathlib import Path
from run_observation_v1 import HERE, ROOT, digest, write_new

old=json.loads((HERE/'null-clock-development-v1-binding.json').read_text())
for p,sha in old['files'].items():assert digest(ROOT/p)==sha,p
for p,sha in old['externalFiles'].items():assert digest(p)==sha,p
for stage in ['primary','increment']:
    assert not (HERE/'raw'/('null-clock-'+stage+'-v1')).exists(),'Old games must remain unplayed'
assert json.loads((HERE/'cpu-delivery-v1-summary.json').read_text())['criteriaPassed']
plan=json.loads((HERE/'null-clock-development-v1.json').read_text());plan['id']=plan['id'].replace('-v1','-v2')
plan['resourceAdmission']='New prospectively frozen qualification, supported by cpu-delivery-v1. No v1 result or limit is rewritten. Pre/post fixed72-request owner-thread delivery: each profile median>=.90 and nearest-rank fifth percentile>=.80; profile median spread<=.05. Preflight may reuse a passing identical calibration ending within15min. Idle30s and whole-owned-run global screens: <=2.0 other average logical CPUs,>=8GiB available,<=75% endpoint memory load. Native per-actor lifetimeCPU/whole-Search-request elapsed must each be .90..1.25 and absolute actor difference<=.10; acknowledge startup/JIT/GC contribution. All gates must pass. No computational overlap; only one W1 game. This practical qualification does not establish perfect isolation or rule out bursts.'
plan['resourcePremise']='Four old prelaunch admissions failed the investigator-selected1.0-core ceiling, including a fully idle check1.19269. cpu-delivery-v1 then passed frozen criteria: medians .99791/.99974/.99904, fifth percentiles>=.9890, median spread .00184, otherCPU .4184, memory35%. Reassessing a self-chosen gate before any game outcome is an authorized experimental decision; no Contract resource limit is changed. Preserve all old admissions, protocols and evidence. Failed new admission/postqualification is not a strength failure and cannot trigger outcome-dependent replacement.'
plan['openings']='Reuse the exact never-played64 openings selected and split by null-clock-development-v1; source, partition, seed, exclusions and split unchanged. No new outcome information exists. Independent confirmation remains fresh partition1 excluding every development selection.'
for stage in plan['stages']:stage['name']=stage['name'].replace('-v1','-v2')
write_new(HERE/'null-clock-development-v2.json',plan)
newfiles=[HERE/'null-clock-development-v2.json',Path(__file__),HERE/'run_null_clock_v2.py',HERE/'clock_resource_v2.py',
    HERE/'cpu-delivery-v1.json',HERE/'cpu-delivery-v1-binding.json',HERE/'cpu-delivery-v1-summary.json',HERE/'null-clock-primary-v1-admission-01.json']
for stage in ['primary','increment']:
    for suffix in ['-openings.pgn','-openings.selection.json']:
        source=HERE/('null-clock-'+stage+'-v1'+suffix);target=HERE/('null-clock-'+stage+'-v2'+suffix)
        with target.open('xb') as stream:stream.write(source.read_bytes())
        assert digest(source)==digest(target);newfiles.append(target)
files=dict(old['files']);files.update({str(p.relative_to(ROOT)):digest(p) for p in newfiles})
calibration=json.loads((HERE/'cpu-delivery-v1-binding.json').read_text())
for p,sha in calibration['files'].items():assert digest(p)==sha;files[str(Path(p).relative_to(ROOT))]=sha
write_new(HERE/'null-clock-development-v2-binding.json',{'head':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),
    'files':files,'externalFiles':old['externalFiles'],'supersedesUnplayedPlanSha256':digest(HERE/'null-clock-development-v1.json')})
print('Frozen resource-qualified v2, with identical never-played opening splits.')
