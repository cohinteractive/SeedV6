"""Reconcile immutable ONE checkpoint receipts into a lightweight resumable progress record."""
import json
from pathlib import Path
import struct
import time
from baseline import ROOT,sha
from compute_control import atomic


def main():
    base=ROOT/'docs/research/brn-relational-cglhw';config_path=base/'r057-main-training.json'
    config=json.loads(config_path.read_text());config_hash=sha(config_path);out=ROOT/config['output']
    receipts=[]
    for path in (out/'segments').glob('*/receipt.json'):
        row=json.loads(path.read_text());assert row['config_sha256']==config_hash
        receipts.append((row['exposures'],path,row))
    receipts.sort();cursor=0;previous=None;seconds=0.;epochs=[]
    for end,path,row in receipts:
        assert row['begin_exposures']==cursor and end>cursor and row['optimizer_steps']==end//128
        assert row['next_epoch_zero_based']==end//config['samples'] and row['next_row_in_epoch']==end%config['samples']
        if previous:
            assert Path(row['previous_receipt']).resolve()==previous.resolve() and sha(previous)==row['previous_receipt_sha256']
        else:assert not row.get('previous_receipt')
        cursor=end;previous=path;seconds+=row['seconds']
        if 'metrics' in row:epochs.append(dict(epoch=end//config['samples'],exposures=end,metrics=row['metrics'],receipt=str(path.relative_to(ROOT)),receipt_sha256=sha(path)))
    latest=None
    if receipts:
        _,path,row=receipts[-1];state=path.with_name('model.state');assert sha(state)==row['state_sha256']
        with state.open('rb') as stream:header=stream.read(36)
        magic,width,rank,parameters,rate,step=struct.unpack('>Qiiidq',header)
        assert magic==0x42524e4f4e453031 and width==config['width'] and rank==config['rank'] and step==cursor//128
        latest=dict(receipt=str(path.relative_to(ROOT)),receipt_sha256=sha(path),state=str(state.relative_to(ROOT)),state_sha256=row['state_sha256'],optimizer_steps=step,rate=rate)
    target=config['samples']*config['epochs'];assert cursor<=target
    final_verified=False
    if (out/'result.json').exists():
        final=json.loads((out/'result.json').read_text())
        assert final['config_sha256']==config_hash and final['exposures']==cursor==target
        assert final['samples']==config['samples'] and final['epochs']==config['epochs']
        assert Path(final['latest_receipt']).resolve()==previous.resolve() and final['latest_receipt_sha256']==sha(previous)
        final_verified=True
    result=dict(status='COMPLETE' if final_verified else 'IN_PROGRESS',observed_utc=time.strftime('%Y-%m-%dT%H:%M:%SZ',time.gmtime()),
                config_sha256=config_hash,source_revision='19159d93',samples=config['samples'],target_exposures=target,completed_exposures=cursor,
                completed_equivalent_passes=cursor/config['samples'],next_epoch_zero_based=cursor//config['samples'],next_row_in_epoch=cursor%config['samples'],
                distinct_sample_rows_visited=min(cursor,config['samples']),canonical_unique_visited=33180466 if cursor>=config['samples'] else None,
                completed_checkpoints=len(receipts),training_segment_seconds=seconds,latest=latest,epoch_metrics=epochs,
                preliminary_exposures_excluded=8388608,scope='Verified committed checkpoint chain only; work after the last receipt is not counted. Segment seconds include state write/verification, exclude startup and epoch metrics. No playing result.')
    atomic(base/'r057-main-progress.json',result)
    print(json.dumps({k:result[k] for k in ['status','completed_exposures','target_exposures','completed_equivalent_passes','completed_checkpoints','training_segment_seconds']},indent=2))


if __name__=='__main__':main()
