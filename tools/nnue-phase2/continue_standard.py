"""A002 exact256 continuation and qualification against the frozen v4 runtime."""
import argparse
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import time
import uuid
from bootstrap import ROOT, BASE, PRIOR, write, sha
sys.path.insert(0,str(ROOT/'tools/conventional-nnue'))
from compute_control import verify_checkpoint


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--runtime-version',type=int,required=True)
    p.add_argument('--assessment-revision',type=int,default=1);a=p.parse_args();os.chdir(ROOT);started=time.monotonic()
    runtime_path=BASE/f'frozen-runtime-v{a.runtime_version}.json';runtime=json.loads(runtime_path.read_text())
    for name,digest in runtime['files'].items():
        assert sha(Path(runtime['path'])/name)==digest
        assert sha(ROOT/'build/research/runtime'/name)==digest,'Installed UCI assessment runtime changed'
    jars={Path(n).name:d for n,d in runtime['files'].items() if n.startswith('lib/') and n.endswith('.jar')}
    installed=list((ROOT/'research/build/install/research/lib').glob('*.jar'));assert installed
    for path in installed:assert jars.get(path.name)==sha(path),'Installed probe runtime changed'
    original=ROOT/'build/conventional-nnue/cargo-target/release/seed-conventional-nnue.exe'
    expected=json.loads((BASE/'baseline.json').read_text())['trainer_sha256'];assert sha(original)==expected
    binary=BASE/f'raw/trainer-conventional-{expected}.exe'
    if not binary.exists():
        with original.open('rb') as src,binary.open('xb') as dst:shutil.copyfileobj(src,dst)
    assert sha(binary)==expected
    parent=Path(json.loads((BASE/'a001-replica-assessment-spec.json').read_text())['models'][0]['checkpoint'])
    receipt,parent_config=verify_checkpoint(parent);assert receipt['completed_superbatch']==64
    assert parent_config['hidden']==256 and parent_config['output_buckets']==8
    config=json.loads((BASE/'a001-replica-64.json').read_text())
    config.update(output=str(BASE/'raw/a002-replica256-128'),start_superbatch=65,end_superbatch=128,resume=str(parent))
    recipe=BASE/'a002-replica256-128.json'
    if recipe.exists():assert json.loads(recipe.read_text())==config
    else:write(recipe,config)
    plan=dict(protocol_sha256=sha(BASE/'A002.md'),parent_receipt_sha256=sha(parent/'seed-manifest.json'),recipe_sha256=sha(recipe),
        trainer=str(binary),trainer_sha256=expected,runtime_manifest_sha256=sha(runtime_path),supervisor_budget_seconds=1800,training_budget_seconds=900,
        driver_sha256=sha(Path(__file__)),assessment_tool_sha256=sha(ROOT/'tools/conventional-nnue/assess_strength.py'),
        cost_tool_sha256=sha(ROOT/'tools/nnue-phase2/compare_compact_costs.py'))
    frozen=BASE/'a002-plan.json'
    if frozen.exists():assert json.loads(frozen.read_text())==plan
    else:write(frozen,plan)
    python=[sys.executable,'-B'];control=python+[str(ROOT/'tools/conventional-nnue/compute_control.py')]
    def execute(command,label,limit):
        remaining=1800-(time.monotonic()-started)
        if remaining<=0:raise RuntimeError('A002 supervisor budget exhausted')
        nonce=uuid.uuid4().hex[:12];command=command.copy()
        if '--log' in command:
            at=command.index('--log')+1;old=Path(command[at]);command[at]=str(old.with_name(old.stem+'-'+nonce+old.suffix))
        with (BASE/f'raw/{label}-driver-{nonce}.log').open('xb') as log:
            subprocess.run(command,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=min(limit,remaining))
    out=Path(config['output']);endpoints=list(out.glob('segments/*/run/seed-cnnue-v1-128/seed-manifest.json'))
    if not endpoints:
        execute(control+['train','--name','phase2-a002-replica256','--trainer',str(binary),str(recipe)],'a002-training',900)
        endpoints=list(out.glob('segments/*/run/seed-cnnue-v1-128/seed-manifest.json'))
    assert len(endpoints)==1;checkpoint=endpoints[0].parent;verify_checkpoint(checkpoint)
    previous=parent;seconds=0.;steps=[]
    for verified in sorted(out.glob('segments/*/verified.json')):
        record=json.loads(verified.read_text());current=Path(record['path']);verified_receipt,segment=verify_checkpoint(current)
        assert segment['resume']==str(previous)
        for field in ('weights','momentum','velocity'):
            assert sha(previous/f'optimiser_state/{field}.bin')==sha(current.parent/f'initial/{field}.bin')
        previous=current;steps.append(verified_receipt['completed_superbatch']);seconds+=record['wall_seconds']
    assert steps==list(range(65,129)) and previous==checkpoint
    evaluation=BASE/f'raw/a002-replica256-eval-r{a.assessment_revision}'
    spec=dict(models=[dict(id='replica256',checkpoint=str(checkpoint),evaluation=str(evaluation))],
              probes=config['probes'],validation=str(PRIOR/'raw/mixed-holdout/validation.txt'),roots=str(PRIOR/'cost-roots.fens'))
    spec_path=BASE/f'a002-assessment-spec-r{a.assessment_revision}.json'
    if spec_path.exists():assert json.loads(spec_path.read_text())==spec
    else:write(spec_path,spec)
    assessment=BASE/f'raw/a002-assessment-r{a.assessment_revision}'
    if not (assessment/'summary.json').exists():
        execute(control+['run','--name','phase2-a002-assessment','--resume-note','Qualify128-superbatch conventional256 control; no games or promotion',
            '--log',str(assessment)+'.log','--']+python+['tools/conventional-nnue/assess_strength.py',str(spec_path),str(assessment)],'a002-assessment',300)
    result=json.loads((assessment/'summary.json').read_text());model=result['results']['replica256']
    assert model['parity']['status']==model['uci']['status']=='PASS' and model['benchmark']['incremental_allocated_bytes_20000']==0
    report=BASE/'a002-replica256-assessment.json'
    if report.exists():assert json.loads(report.read_text())==result
    else:write(report,result)
    summary=dict(additional_native_seconds=seconds,verified_optimizer_continuations=len(steps),parent=str(parent),checkpoint=str(checkpoint),
                 receipt_sha256=sha(checkpoint/'seed-manifest.json'),assessment_sha256=sha(report),training_plan_sha256=sha(frozen))
    summary_path=BASE/'a002-summary.json'
    if summary_path.exists():assert json.loads(summary_path.read_text())==summary
    else:write(summary_path,summary)
    print('A002 conventional256 endpoint qualified:',model['network_sha256'],flush=True)
    cost=BASE/'compact128-128-controlled-cost.json'
    if not cost.exists():
        execute(control+['run','--name','phase2-duration-controlled-cost','--resume-note','Compare equally trained128/256 controls in the same frozen runtime',
            '--log',str(BASE/'raw/duration-controlled-cost.log'),'--']+python+['tools/nnue-phase2/compare_compact_costs.py',
            '--runtime-version',str(a.runtime_version),'--checkpoint','128','--standard-assessment',str(report)],'duration-controlled-cost',900)
    print('A002 and controlled cost complete; no games automatically launched.',flush=True)


if __name__=='__main__':main()
