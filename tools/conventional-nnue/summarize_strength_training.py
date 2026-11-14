"""Audit this campaign's completed optimizer chains, including retired file hashes.

Retirement plans preserve hashes verified before deletion; they do not recreate
deleted weights or imply that old terminal run directories can be resumed.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re
import compute_gate


def sha(path):
    with path.open('rb') as stream:return hashlib.file_digest(stream,'sha256').hexdigest()


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('campaign',type=Path);p.add_argument('output',type=Path)
    a=p.parse_args();a.campaign=a.campaign.resolve();compute_gate.require();runs=[];chains=0
    for run in sorted((a.campaign/'raw').iterdir()):
        if not (run/'training-run.json').is_file():continue
        compute_gate.require();owner=json.loads((run/'training-run.json').read_text())
        if owner['status'] not in ('completed','finalized'):raise ValueError('Incomplete run: '+str(run))
        request=json.loads((run/'requested-config.json').read_text())
        plan_path=run/'training-finalization-plan.json';plan=json.loads(plan_path.read_text()) if plan_path.exists() else None
        def recorded_hash(path):
            if path.is_file():return sha(path),'current bytes'
            key=path.relative_to(run).as_posix()
            if not plan or key not in plan['targets']:raise ValueError('Missing unaccounted initializer: '+str(path))
            return plan['targets'][key]['sha256'],'verified before retirement'
        segments=[]
        for verification in sorted((run/'segments').glob('*/verified.json')):
            v=json.loads(verification.read_text());checkpoint=Path(v['path']);receipt_path=checkpoint/'seed-manifest.json'
            receipt=json.loads(receipt_path.read_text());config_path=checkpoint.parent/'config.json';config=json.loads(config_path.read_text())
            if sha(receipt_path)!=v['receipt_sha256'] or sha(config_path)!=receipt['config_sha256'] or v['process_exit_code']!=0:
                raise ValueError('Receipt/config/process mismatch')
            initial={name:recorded_hash(checkpoint.parent/'initial'/f'{name}.bin') for name in ('weights','momentum','velocity')}
            if config.get('resume'):
                prior=json.loads((Path(config['resume'])/'seed-manifest.json').read_text())
                if any(value[0]!=prior['files'][f'optimiser_state/{name}.bin'] for name,value in initial.items()):
                    raise ValueError('Optimizer continuation mismatch: '+str(checkpoint))
                if prior['completed_superbatch']+1!=receipt['completed_superbatch']:raise ValueError('Nonconsecutive cursor')
                chains+=1
            text=re.sub(r'\x1b\[[0-9;]*[A-Za-z]','',(verification.parent/'console.log').read_text())
            losses=re.findall(r'running loss ([0-9.]+)',text)
            if not losses:raise ValueError('Missing training loss')
            segments.append(dict(superbatch=receipt['completed_superbatch'],process_seconds=v['wall_seconds'],
                                 training_loss=float(losses[-1]),learning_rate=receipt['effective_learning_rate'],
                                 initializer=initial,receipt_sha256=v['receipt_sha256']))
        expected=list(range(request['start_superbatch'],request['end_superbatch']+1))
        if [s['superbatch'] for s in segments]!=expected:raise ValueError('Incomplete/duplicate segment sequence')
        runs.append(dict(run=str(run),status=owner['status'],hidden=request.get('hidden',256),wdl=request['wdl'],
                         data_sha256=request['data_sha256'],seed=request['seed'],segments=segments,
                         new_exposures=len(segments)*request['batch_size']*request['batches_per_superbatch']))
    seconds=sum(s['process_seconds'] for r in runs for s in r['segments'])
    result=dict(status='PASS',runs=runs,verified_optimizer_continuations=chains,
                campaign_new_exposures=sum(r['new_exposures'] for r in runs),training_process_wall_seconds=seconds,
                gpu_active_hours_upper_bound=seconds/3600,
                limits='Native-process wall includes CUDA initialization, data verification and checkpoint I/O; not a GPU utilization integral. Hash-identical resume initializers do not imply bitwise deterministic subsequent GPU reductions. Training losses across different WDL objectives are not directly comparable.')
    with a.output.open('x',encoding='utf-8') as stream:json.dump(result,stream,indent=2);stream.write('\n')
    print(json.dumps({k:v for k,v in result.items() if k!='runs'},indent=2))


if __name__=='__main__':main()
