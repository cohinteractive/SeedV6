"""B004 exact-state64-to128 continuation; qualification/cost only, never games."""
import argparse
import json
from pathlib import Path
import re
import statistics
import subprocess
import sys
import time
import uuid
from bootstrap import ROOT, BASE, write, sha
from codec import verify_checkpoint


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--runtime-version',type=int,required=True)
    p.add_argument('--assessment-revision',type=int,default=1);a=p.parse_args();started=time.monotonic()
    # No expensive work or recipe creation until all frozen S002 endpoints exist.
    for name in ('base','deep16','pawnpairs'):
        result=json.loads((BASE/f'screen-compact128-{name}-results.json').read_text())
        assert result['games']==result['completed']==64
        assert result['legal_replay_and_outcome_audit']==result['frozen_artifact_recheck']=='PASS'
    manifest=BASE/f'frozen-runtime-v{a.runtime_version}.json';runtime=json.loads(manifest.read_text())
    for name,digest in runtime['files'].items():assert sha(Path(runtime['path'])/name)==digest
    trainer=json.loads((BASE/'compact-trainer.json').read_text());binary=Path(trainer['path']);source=Path(trainer['source'])
    assert sha(binary)==trainer['sha256'] and sha(source)==trainer['source_sha256']
    plan=dict(protocol_sha256=sha(BASE/'B004.md'),runtime_manifest_sha256=sha(manifest),trainer_manifest_sha256=sha(BASE/'compact-trainer.json'),
              start_superbatch=65,end_superbatch=128,native_wall_budget_seconds=1800,supervisor_wall_budget_seconds=3600,parents={})
    for name in ('base','deep16','pawnpairs'):
        report=json.loads((BASE/f'compact128-{name}-64-assessment.json').read_text());parent=Path(report['metadata']['checkpoint'])
        config,receipt=verify_checkpoint(parent);assert config['hidden']==128 and receipt['completed_superbatch']==64
        plan['parents'][name]=dict(checkpoint=str(parent),receipt_sha256=sha(parent/'receipt.json'),assessment_sha256=sha(BASE/f'compact128-{name}-64-assessment.json'))
    frozen=BASE/'duration128-plan.json'
    if frozen.exists():assert json.loads(frozen.read_text())==plan
    else:write(frozen,plan)
    python=[sys.executable,'-B'];control=python+[str(ROOT/'tools/conventional-nnue/compute_control.py')]
    def execute(command,label,budget=None):
        remaining=3600-(time.monotonic()-started) if budget is None else budget
        if remaining<=0:raise RuntimeError('B004 supervisor budget reached; preserve evidence and reassess')
        nonce=uuid.uuid4().hex[:12];command=command.copy()
        if '--log' in command:
            at=command.index('--log')+1;old=Path(command[at]);command[at]=str(old.with_name(old.stem+'-'+nonce+old.suffix))
        with (BASE/f'raw/{label}-driver-{nonce}.log').open('xb') as log:
            subprocess.run(command,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=remaining)
    spent=0.;summaries={};initial=json.loads((BASE/'compact128-curves.json').read_text())
    for name in ('base','deep16','pawnpairs'):
        config=json.loads((BASE/f'compact128-{name}-64.json').read_text())
        config.update(output=str(BASE/f'raw/compact128-{name}-128'),start_superbatch=65,end_superbatch=128,resume=plan['parents'][name]['checkpoint'])
        recipe=BASE/f'compact128-{name}-128.json'
        if recipe.exists():assert json.loads(recipe.read_text())==config
        else:write(recipe,config)
        out=Path(config['output'])
        if not (out/'completed.json').exists():
            if spent>=1800:raise RuntimeError('B004 native budget exhausted')
            execute(control+['run','--name','phase2-duration128-'+name,'--resume-note','Resume exact64-to128 optimizer/data/schedule chain; never replace old endpoints',
                '--log',str(BASE/f'raw/duration128-{name}.log'),'--']+python+['tools/nnue-phase2/train_architecture.py',str(recipe),
                '--trainer',str(binary),'--trainer-source',str(source),'--max-native-seconds',str(1800-spent)],'duration128-'+name)
        completed=json.loads((out/'completed.json').read_text());spent+=completed['native_wall_seconds']
        if spent>1800:raise RuntimeError('B004 native budget exceeded at segment boundary; reassess before further work')
        checkpoint=Path(completed['checkpoint']);config,receipt=verify_checkpoint(checkpoint);assert receipt['completed_superbatch']==128
        assessment=BASE/f'raw/compact128-{name}-128-assessment-r{a.assessment_revision}'
        if not (assessment/'summary.json').exists():
            execute(control+['run','--name','phase2-duration128-assess-'+name,'--resume-note','Qualify new weights under unchanged runtime; no acceptance from loss',
                '--log',str(assessment)+'.log','--']+python+['tools/nnue-phase2/assess_architecture.py',str(checkpoint),str(assessment),
                '--runtime',runtime['path'],'--trainer',str(binary)],'duration128-assess-'+name)
        result=json.loads((assessment/'summary.json').read_text())
        assert result['parity']['status']=='PASS' and result['parity']['quantization_rms_link_error']<=30
        assert result['uci']['status']=='PASS' and result['benchmark']['allocated_bytes_100000']==0
        report=BASE/f'compact128-{name}-128-assessment.json'
        if report.exists():assert json.loads(report.read_text())==result
        else:write(report,result)
        values=list(initial[name]['superbatches'])
        for verified in sorted(out.glob('segments/*/verified.json')):
            segment=json.loads((verified.parent/'config.json').read_text())
            transcript=re.sub(r'\x1b\[[0-9;]*[A-Za-z]','',(verified.parent/'console.log').read_text(encoding='utf-8'))
            losses=re.findall(r'running loss\s+([0-9.]+)',transcript)
            if not losses:raise ValueError('Missing training loss')
            values.append(dict(superbatch=segment['end_superbatch'],loss=float(losses[-1]),config_sha256=sha(verified.parent/'config.json')))
        values.sort(key=lambda row:row['superbatch']);assert [v['superbatch'] for v in values]==list(range(1,129))
        curve=out/'full-loss-curve.json'
        if curve.exists():assert json.loads(curve.read_text())==values
        else:write(curve,values)
        parent=json.loads((BASE/f'compact128-{name}-64-assessment.json').read_text())
        summaries[name]=dict(additional_native_seconds=completed['native_wall_seconds'],validation64=parent['validation'],validation128=result['validation'],
            reported_16_superbatch_means=[statistics.mean(v['loss'] for v in values[i:i+16]) for i in range(0,128,16)],
            full_curve=str(curve),curve_sha256=sha(curve),assessment_sha256=sha(report),effective_lr_at128=receipt['effective_learning_rate'])
        print('Duration128 endpoint qualified:',name,flush=True)
    summary=BASE/'duration128-summary.json'
    if summary.exists():assert json.loads(summary.read_text())==summaries
    else:write(summary,summaries)
    if not (BASE/'compact128-128-cost.json').exists():
        execute(control+['run','--name','phase2-duration128-cost','--resume-note','Controlled warmed cost; strength selection remains separate',
            '--log',str(BASE/'raw/duration128-cost.log'),'--']+python+['tools/nnue-phase2/compare_compact_costs.py','--runtime-version',str(a.runtime_version),'--checkpoint','128'],
            'duration128-cost',900)
    print('Duration128 qualification/cost complete; no games automatically launched.',flush=True)


if __name__=='__main__':main()
