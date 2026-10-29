"""R036 fixed-endpoint data-transfer gates with opening-grouped descriptive losses."""
import json
from pathlib import Path
from baseline import ROOT, admit, sha
from analyze_fields import sigmoid


def main():
    resource=admit(initial=True);folder=ROOT/'docs/research/brn-relational-cglhw'
    path=folder/'raw/r036-prediction.json';receipt=json.loads(path.with_suffix('.receipt.json').read_text())
    assert sha(path)==receipt['sha256'];prediction=json.loads(path.read_text())
    parent_path=folder/'raw/r035-prediction.json';parent=json.loads(parent_path.read_text())
    assert sha(parent_path)==json.loads(parent_path.with_suffix('.receipt.json').read_text())['sha256']
    for key in ('rows','teacher_targets','bt4_targets','pieces'):assert prediction[key]==parent[key]
    evidence=json.loads((folder/'r035-results.json').read_text());spec=evidence['datasets']['validation']
    assert sha(ROOT/spec['positions'])==spec['positions_sha256']
    positions=json.loads((ROOT/spec['positions']).read_text());groups={}
    for i,position in enumerate(positions):
        for root in {(s['seed'],s['opening']) for s in position['sources']}:groups.setdefault(str(root),[]).append(i)
    assert len(groups)==8 and len(positions)==prediction['rows']==1398
    losses={};models=[]
    for model in parent['models']+prediction['models']:
        loss=[(sigmoid(x)-y)**2 for x,y in zip(model['logits'],prediction['teacher_targets'])]
        assert len(loss)==1398 and abs(sum(loss)/len(loss)-model['teacher_mse'])<1e-14
        losses[model['id']]=loss
        by_root={root:dict(rows=len(indices),teacher_mse=sum(loss[i] for i in indices)/len(indices)) for root,indices in groups.items()}
        models.append({k:v for k,v in model.items() if k not in ('logits','bt4_mse')}|dict(by_root=by_root))
    records=[];gates=[];samples=[]
    controls=json.loads((folder/'r031-results.json').read_text())
    for seed in (71,97):
        config_path=folder/f'r036-mix-{seed}.json';config=json.loads(config_path.read_text());out=ROOT/config['output']
        assert sha(ROOT/config['initial_state'])==config['initial_state_sha256']
        binding=json.loads((out/'binding.json').read_text());assert binding['config_sha256']==sha(config_path)
        assert binding['runner_sha256']==sha(ROOT/'tools/brn-relational/run_objective.py')
        for name,digest in binding['runtime_sha256'].items():assert sha(ROOT/name)==digest
        result=json.loads((out/'result.json').read_text());endpoint=json.loads((out/'epoch-2.json').read_text())
        assert result['metrics'][-1]==endpoint and endpoint['updates']==16384
        assert sha(out/'epoch-2.state')==endpoint['state_sha256']
        for segment in range(1,9):
            s=json.loads((out/f'segment-{segment}.json').read_text());assert s['completed_positions']==segment*131072 and s['updates']==8192+segment*1024
            assert s['state_sha256']==sha(out/f'segment-{segment}.state') and s['config_sha256']==sha(config_path)
        executions=[]
        for p in sorted(out.glob('execution-*.json')):
            e=json.loads(p.read_text());assert e['process_absent'];executions.append(dict(path=str(p.relative_to(ROOT)),sha256=sha(p),**e));samples.extend(e['resources'])
        assert executions and executions[-1]['status']=='COMPLETE'
        prior=folder/f'raw/r031-control-{seed}';control=json.loads((prior/'result.json').read_text());control_checkpoint=json.loads((prior/'epoch-2.json').read_text())
        frozen=next(r for r in controls['records'] if Path(r['config']).name==f'r031-control-{seed}.json')
        assert sha(prior/'result.json')==frozen['result_sha256'] and control==frozen['result']
        assert control['metrics'][-1]==control_checkpoint and sha(prior/'epoch-2.state')==control_checkpoint['state_sha256']
        mix=next(m for m in models if m['id']==f'r036-mix-{seed}');c=next(m for m in models if m['id']==f'r031-control-{seed}');p=next(m for m in models if m['id']==f'r028-one-{seed}')
        assert mix['state_sha256']==endpoint['state_sha256'] and c['state_sha256']==control_checkpoint['state_sha256']
        assert abs(mix['teacher_mse']-result['new_validation_mse'])<1e-13
        gate=dict(seed=seed,new_dev_gain_over_parent=1-mix['teacher_mse']/p['teacher_mse'],new_dev_gain_over_control=1-mix['teacher_mse']/c['teacher_mse'],
                  old_teacher_gain_over_control=1-endpoint['validation_mse']/control_checkpoint['validation_mse'],
                  old_bt4_gain_over_control=1-result['original_bt4_validation_mse']/control['original_bt4_validation_mse'])
        gate['passes']=gate['new_dev_gain_over_parent']>=.1 and gate['new_dev_gain_over_control']>=.1 and gate['old_teacher_gain_over_control']>=-.05 and gate['old_bt4_gain_over_control']>=-.03
        gate['roots_improved_over_control']=sum(mix['by_root'][root]['teacher_mse']<c['by_root'][root]['teacher_mse'] for root in groups)
        gates.append(gate);records.append(dict(config_sha256=sha(config_path),binding=binding,result=result,result_sha256=sha(out/'result.json'),executions=executions,
                                             reused_control_result_sha256=sha(prior/'result.json'),reused_control_checkpoint=control_checkpoint))
    result=dict(plan_sha256=sha(folder/'r036-plan.json'),analysis_sha256=sha(Path(__file__)),resources=resource,prediction_path=str(path.relative_to(ROOT)),prediction_sha256=sha(path),
                validation_manifest_sha256=sha(folder/'r036-validation.json'),mixture_manifest_sha256=sha(folder/'r036-mixture.json'),records=records,models=models,gates=gates,
                transfer_followup_justified=all(g['passes'] for g in gates),max_cpu_percent=max(s['cpu_percent'] for s in samples),minimum_available_gib=min(s['available_gib'] for s in samples),
                scope='Fixed final checkpoints;one mixture,no tuning. Eight development opening clusters;per-root descriptive losses,not1398 independent trials. No strength claim.')
    with (folder/'r036-results.json').open('x') as stream:json.dump(result,stream,indent=2);stream.write('\n')
    print(json.dumps(dict(gates=gates,models=[{k:v for k,v in m.items() if k!='by_root'} for m in models],training=[dict(seed=r['result']['seed'],new_train_mse=r['result']['new_train_mse'],new_validation_mse=r['result']['new_validation_mse'],old_teacher_mse=r['result']['metrics'][-1]['validation_mse'],old_bt4_mse=r['result']['original_bt4_validation_mse']) for r in records],transfer_followup_justified=result['transfer_followup_justified'],max_cpu=result['max_cpu_percent'],minimum_free_gib=result['minimum_available_gib']),indent=2))


if __name__ == '__main__':
    main()
