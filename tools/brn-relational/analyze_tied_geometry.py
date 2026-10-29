"""R041 fixed neural mechanism/family gates; no epoch selection or iid strength inference."""
import json
import math
from pathlib import Path
import struct
from baseline import ROOT, admit, sha
from analyze_fields import sigmoid


def main():
    resource=admit(initial=True);folder=ROOT/'docs/research/brn-relational-cglhw'
    plan=json.loads((folder/'r041-plan.json').read_text());records=[];samples=[]
    source=json.loads((folder/'r035-results.json').read_text());spec=source['datasets']['validation']
    assert sha(ROOT/spec['labeled'])==spec['labeled_sha256'] and sha(ROOT/spec['positions'])==spec['positions_sha256']
    target=[float(line.split('\t')[1]) for line in (ROOT/spec['labeled']).read_text().splitlines()]
    positions=json.loads((ROOT/spec['positions']).read_text());groups={}
    for i,p in enumerate(positions):
        for root in {(s['seed'],s['opening']) for s in p['sources']}:groups.setdefault(str(root),[]).append(i)
    assert len(groups)==8 and len(positions)==len(target)==1398
    for config_name in plan['configs']:
        config_path=ROOT/config_name;config=json.loads(config_path.read_text());out=ROOT/config['output'];binding=json.loads((out/'binding.json').read_text())
        assert binding['config_sha256']==sha(config_path) and binding['runner_sha256']==sha(ROOT/'tools/brn-relational/run_objective.py')
        for name,digest in binding['runtime_sha256'].items():assert sha(ROOT/name)==digest
        result_path=out/'result.json';result=json.loads(result_path.read_text());epoch=json.loads((out/'epoch-1.json').read_text())
        assert result['metrics'][-1]==epoch and epoch['updates']==8192 and sha(out/'epoch-1.state')==epoch['state_sha256']
        assert result['mode']==config['mode'] and result['seed']==config['seed']
        segments=[]
        for n in range(1,9):
            r=json.loads((out/f'segment-{n}.json').read_text());assert r['completed_positions']==n*131072 and r['updates']==n*1024 and r['config_sha256']==sha(config_path) and sha(out/f'segment-{n}.state')==r['state_sha256'];segments.append(r)
        executions=[]
        for p in sorted(out.glob('execution-*.json')):
            r=json.loads(p.read_text());assert r['process_absent'];samples.extend(r['resources']);executions.append(dict(path=str(p.relative_to(ROOT)),sha256=sha(p),**r))
        assert executions and executions[-1]['status']=='COMPLETE'
        loss=[(sigmoid(x)-y)**2 for x,y in zip(result['played_validation_logits'],target)]
        assert len(loss)==1398 and abs(sum(loss)/len(loss)-result['played_validation_mse'])<1e-14
        by_root={root:dict(rows=len(indices),teacher_mse=sum(loss[i] for i in indices)/len(indices)) for root,indices in groups.items()}
        payload=(out/'epoch-1.state').read_bytes();magic,mode,count,rate,step=struct.unpack_from('>qiidq',payload)
        assert magic==0x42524e5347543031 and count==20353 and rate==.01 and step==8192 and mode==(config['mode']=='TIED') and len(payload)==40+24*count
        weights=struct.unpack_from('>'+str(count)+'d',payload,32)
        sets=dict(unary=range(769),source=range(769,6913),readout=range(6913,13057),static_transform=[p for p in range(13057,14209) if (p-13057)%12<10],context_transform=[p for p in range(13057,14209) if (p-13057)%12>=10],second_readout=range(14209,count))
        parameters={name:dict(rms=math.sqrt(sum(weights[p]**2 for p in indices)/len(indices)),clamped=sum(abs(weights[p])>=.99-1e-14 for p in indices)/len(indices)) for name,indices in sets.items()}
        records.append(dict(config=config_name,config_sha256=sha(config_path),result_path=str(result_path.relative_to(ROOT)),result_sha256=sha(result_path),result={k:v for k,v in result.items() if k!='played_validation_logits'},by_root=by_root,parameter_diagnostics=parameters,segments=segments,executions=executions))
    control_evidence=json.loads((folder/'r040-results.json').read_text());baseline=json.loads((folder/'r028-results.json').read_text());gates=[]
    for record in records:
        r=record['result'];seed=r['seed'];assert r['mode']=='TIED'
        saved=next(x for x in control_evidence['records'] if x['result']['mode']=='CONTEXT' and x['result']['seed']==seed)
        assert sha(ROOT/saved['result_path'])==saved['result_sha256']
        control=saved['result'];assert sha(folder/f'raw/r040-context-{seed}/epoch-1.state')==control['metrics'][-1]['state_sha256']
        old=next(x['result'] for x in baseline['records'] if x['result']['mode']=='ONE' and x['result']['seed']==seed)
        played=next(m for m in source['models'] if m['id']==f'r028-one-{seed}')
        assert sha(folder/f'raw/r028-one-{seed}/epoch-1.state')==played['state_sha256']
        endpoint=r['metrics'][-1];comparison=dict(seed=seed,old_teacher_gain_over_control=1-endpoint['validation_mse']/control['metrics'][-1]['validation_mse'],played_gain_over_control=1-r['played_validation_mse']/control['played_validation_mse'],bt4_gain_over_control=1-r['original_bt4_validation_mse']/control['original_bt4_validation_mse'],old_unary_gain=1-endpoint['validation_mse']/endpoint['unary_only_validation_mse'],second_used_old=r['second_pass_disabled_validation_mse']>endpoint['validation_mse'],second_used_played=r['second_pass_disabled_played_validation_mse']>r['played_validation_mse'])
        comparison['mechanism_pass']=comparison['old_teacher_gain_over_control']>=.02 and comparison['played_gain_over_control']>=.05 and comparison['bt4_gain_over_control']>=-.01 and comparison['old_unary_gain']>=.01 and comparison['second_used_old'] and comparison['second_used_played']
        comparison.update(old_gain_over_r028=1-endpoint['validation_mse']/old['metrics'][-1]['validation_mse'],played_gain_over_r028=1-r['played_validation_mse']/played['teacher_mse'],bt4_gain_over_r028=1-r['original_bt4_validation_mse']/old['original_bt4_validation_mse'],full_inference_ns=r['full_inference_ns'])
        comparison['old_family_route']=comparison['old_gain_over_r028']>=.02 and comparison['played_gain_over_r028']>=-.01
        comparison['played_family_route']=comparison['played_gain_over_r028']>=.1 and comparison['old_gain_over_r028']>=-.02
        comparison['common_guards']=comparison['bt4_gain_over_r028']>=-.01 and comparison['full_inference_ns']<=10000
        comparison['roots_improved_over_control']=sum(record['by_root'][root]['teacher_mse']<saved['by_root'][root]['teacher_mse'] for root in groups)
        gates.append(comparison)
    family_pass=(all(g['old_family_route'] for g in gates) or all(g['played_family_route'] for g in gates)) and all(g['common_guards'] for g in gates)
    result=dict(plan_sha256=sha(folder/'r041-plan.json'),validation_sha256=sha(folder/'r041-validation.json'),analysis_sha256=sha(Path(__file__)),analysis_resource=resource,records=records,gates=gates,family_pass=family_pass,
                transfer_followup_justified=family_pass and all(g['mechanism_pass'] for g in gates),
                elapsed_seconds=sum(e['elapsed_seconds'] for r in records for e in r['executions']),maximum_cpu_percent=max(s['cpu_percent'] for s in samples),minimum_available_gib=min(s['available_gib'] for s in samples),
                scope='Two fixed TIED endpoints versus frozen exact ONE controls;no new development fitting;two family screening routes are not confirmation;no strength claim.')
    with (folder/'r041-results.json').open('x') as stream:json.dump(result,stream,indent=2);stream.write('\n')
    print(json.dumps(dict(gates=gates,family_pass=family_pass,metrics=[dict(seed=r['result']['seed'],train=r['result']['metrics'][-1]['train_mse'],old_teacher=r['result']['metrics'][-1]['validation_mse'],played=r['result']['played_validation_mse'],bt4=r['result']['original_bt4_validation_mse'],cost_ns=r['result']['full_inference_ns']) for r in records],transfer_followup_justified=result['transfer_followup_justified'],elapsed_seconds=result['elapsed_seconds'],maximum_cpu_percent=result['maximum_cpu_percent'],minimum_available_gib=result['minimum_available_gib']),indent=2))


if __name__ == '__main__':
    main()
