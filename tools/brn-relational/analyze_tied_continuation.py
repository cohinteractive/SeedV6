"""R044 fixed proportional-decay continuation gates; no epoch selection or iid strength inference."""
import json
import math
from pathlib import Path
import struct
from baseline import ROOT, admit, sha
from analyze_fields import sigmoid


def main():
    resource=admit(initial=True);folder=ROOT/'docs/research/brn-relational-cglhw'
    plan=json.loads((folder/'r044-plan.json').read_text());records=[];samples=[]
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
        result_path=out/'result.json';result=json.loads(result_path.read_text());epoch=json.loads((out/'epoch-2.json').read_text())
        assert result['metrics'][-1]==epoch and epoch['updates']==16384 and sha(out/'epoch-2.state')==epoch['state_sha256']
        assert result['mode']==config['mode'] and result['seed']==config['seed']
        segments=[]
        for n in range(1,9):
            r=json.loads((out/f'segment-{n}.json').read_text());assert r['completed_positions']==n*131072 and r['updates']==8192+n*1024 and r['config_sha256']==sha(config_path) and sha(out/f'segment-{n}.state')==r['state_sha256'];segments.append(r)
        executions=[]
        for p in sorted(out.glob('execution-*.json')):
            r=json.loads(p.read_text());assert r['process_absent'];samples.extend(r['resources']);executions.append(dict(path=str(p.relative_to(ROOT)),sha256=sha(p),**r))
        assert executions and executions[-1]['status']=='COMPLETE'
        loss=[(sigmoid(x)-y)**2 for x,y in zip(result['played_validation_logits'],target)]
        assert len(loss)==1398 and abs(sum(loss)/len(loss)-result['played_validation_mse'])<1e-14
        by_root={root:dict(rows=len(indices),teacher_mse=sum(loss[i] for i in indices)/len(indices)) for root,indices in groups.items()}
        payload=(out/'epoch-2.state').read_bytes();magic,mode,count,rate,step=struct.unpack_from('>qiidq',payload)
        assert magic==0x42524e5347543031 and count==20353 and rate==config['rate'] and step==16384 and mode==(config['mode']=='TIED') and len(payload)==40+24*count
        weights=struct.unpack_from('>'+str(count)+'d',payload,32)
        sets=dict(unary=range(769),source=range(769,6913),readout=range(6913,13057),static_transform=[p for p in range(13057,14209) if (p-13057)%12<10],context_transform=[p for p in range(13057,14209) if (p-13057)%12>=10],second_readout=range(14209,count))
        parameters={name:dict(rms=math.sqrt(sum(weights[p]**2 for p in indices)/len(indices)),clamped=sum(abs(weights[p])>=.99-1e-14 for p in indices)/len(indices)) for name,indices in sets.items()}
        records.append(dict(config=config_name,config_sha256=sha(config_path),result_path=str(result_path.relative_to(ROOT)),result_sha256=sha(result_path),result={k:v for k,v in result.items() if k!='played_validation_logits'},by_root=by_root,parameter_diagnostics=parameters,segments=segments,executions=executions))
    parents=json.loads((folder/'r041-results.json').read_text());gates=[]
    for seed in (71,97):
        low=next(r for r in records if r['result']['seed']==seed and r['result']['rate']==.001)
        control=next(r for r in records if r['result']['seed']==seed and r['result']['rate']==.01)
        parent=next(r for r in parents['records'] if r['result']['seed']==seed)
        assert sha(ROOT/parent['result_path'])==parent['result_sha256']
        assert sha(folder/f'raw/r041-tied-{seed}/epoch-1.state')==parent['result']['metrics'][-1]['state_sha256']
        r=low['result'];endpoint=r['metrics'][-1];comparisons={}
        for label,reference in [('parent',parent),('control',control)]:
            c=reference['result'];comparisons[label]=dict(old_teacher_gain=1-endpoint['validation_mse']/c['metrics'][-1]['validation_mse'],train_gain=1-endpoint['train_mse']/c['metrics'][-1]['train_mse'],played_gain=1-r['played_validation_mse']/c['played_validation_mse'],bt4_gain=1-r['original_bt4_validation_mse']/c['original_bt4_validation_mse'],roots_improved=sum(low['by_root'][root]['teacher_mse']<reference['by_root'][root]['teacher_mse'] for root in groups))
        gate=dict(seed=seed,comparisons=comparisons,old_unary_gain=1-endpoint['validation_mse']/endpoint['unary_only_validation_mse'],second_used_old=r['second_pass_disabled_validation_mse']>endpoint['validation_mse'],second_used_played=r['second_pass_disabled_played_validation_mse']>r['played_validation_mse'],full_inference_ns=r['full_inference_ns'],control_old_teacher_gain_over_parent=1-control['result']['metrics'][-1]['validation_mse']/parent['result']['metrics'][-1]['validation_mse'])
        gate['passes']=all(c['old_teacher_gain']>=.05 and c['played_gain']>=.02 and c['bt4_gain']>=-.01 for c in comparisons.values()) and gate['old_unary_gain']>=.01 and gate['second_used_old'] and gate['second_used_played'] and gate['full_inference_ns']<=10000
        gates.append(gate)
    result=dict(plan_sha256=sha(folder/'r044-plan.json'),validation_sha256=sha(folder/'r044-validation.json'),parent_evidence_sha256=sha(folder/'r041-results.json'),analysis_sha256=sha(Path(__file__)),analysis_resource=resource,records=records,gates=gates,transfer_followup_justified=all(g['passes'] for g in gates),elapsed_seconds=sum(e['elapsed_seconds'] for r in records for e in r['executions']),maximum_cpu_percent=max(s['cpu_percent'] for s in samples),minimum_available_gib=min(s['available_gib'] for s in samples),scope='Four fixed epoch2 endpoints;coupled rate/shrink recipe;consumed development,not final confirmation or playing strength.')
    with (folder/'r044-results.json').open('x') as stream:json.dump(result,stream,indent=2);stream.write('\n')
    print(json.dumps(dict(gates=gates,metrics=[dict(seed=r['result']['seed'],rate=r['result']['rate'],train=r['result']['metrics'][-1]['train_mse'],old_teacher=r['result']['metrics'][-1]['validation_mse'],played=r['result']['played_validation_mse'],bt4=r['result']['original_bt4_validation_mse']) for r in records],transfer=result['transfer_followup_justified'],elapsed_seconds=result['elapsed_seconds'],maximum_cpu_percent=result['maximum_cpu_percent'],minimum_available_gib=result['minimum_available_gib']),indent=2))


if __name__ == '__main__':
    main()
