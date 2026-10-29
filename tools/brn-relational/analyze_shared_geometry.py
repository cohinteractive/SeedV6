"""R040 fixed neural mechanism/family gates; no epoch selection or iid strength inference."""
import json
import math
from pathlib import Path
import struct
from baseline import ROOT, admit, sha
from analyze_fields import sigmoid


def main():
    resource=admit(initial=True);folder=ROOT/'docs/research/brn-relational-cglhw'
    plan=json.loads((folder/'r040-plan.json').read_text());records=[];samples=[]
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
        assert magic==0x42524e5347503031 and count==14209 and rate==.01 and step==8192 and mode==(config['mode']=='CONTEXT') and len(payload)==40+24*count
        weights=struct.unpack_from('>'+str(count)+'d',payload,32)
        sets=dict(unary=range(769),source=range(769,6913),readout=range(6913,13057),static_transform=[p for p in range(13057,count) if (p-13057)%12<10],context_transform=[p for p in range(13057,count) if (p-13057)%12>=10])
        parameters={name:dict(rms=math.sqrt(sum(weights[p]**2 for p in indices)/len(indices)),clamped=sum(abs(weights[p])>=.99-1e-14 for p in indices)/len(indices)) for name,indices in sets.items()}
        records.append(dict(config=config_name,config_sha256=sha(config_path),result_path=str(result_path.relative_to(ROOT)),result_sha256=sha(result_path),result={k:v for k,v in result.items() if k!='played_validation_logits'},by_root=by_root,parameter_diagnostics=parameters,segments=segments,executions=executions))
    def arm(mode,seed):return next(r['result'] for r in records if r['result']['mode']==mode and r['result']['seed']==seed)
    mechanism=[]
    for seed in (71,97):
        c=arm('CONTEXT',seed);g=arm('GEOMETRY',seed);ce=c['metrics'][-1];ge=g['metrics'][-1]
        r=dict(seed=seed,old_teacher_gain=1-ce['validation_mse']/ge['validation_mse'],played_gain=1-c['played_validation_mse']/g['played_validation_mse'],bt4_gain=1-c['original_bt4_validation_mse']/g['original_bt4_validation_mse'],old_unary_gain=1-ce['validation_mse']/ce['unary_only_validation_mse'],context_used_old=c['context_disabled_validation_mse']>ce['validation_mse'],context_used_played=c['context_disabled_played_validation_mse']>c['played_validation_mse'])
        r['passes']=r['old_teacher_gain']>=.02 and r['played_gain']>=.05 and r['bt4_gain']>=-.01 and r['old_unary_gain']>=.01 and r['context_used_old'] and r['context_used_played']
        mechanism.append(r)
    baseline=json.loads((folder/'r028-results.json').read_text());families=[]
    for mode in ('GEOMETRY','CONTEXT'):
        seeds=[]
        for seed in (71,97):
            r=arm(mode,seed);old=next(x['result'] for x in baseline['records'] if x['result']['mode']=='ONE' and x['result']['seed']==seed)
            played=next(m for m in source['models'] if m['id']==f'r028-one-{seed}')
            assert sha(folder/f'raw/r028-one-{seed}/epoch-1.state')==played['state_sha256']
            row=dict(seed=seed,old_teacher_gain=1-r['metrics'][-1]['validation_mse']/old['metrics'][-1]['validation_mse'],played_gain=1-r['played_validation_mse']/played['teacher_mse'],bt4_gain=1-r['original_bt4_validation_mse']/old['original_bt4_validation_mse'],full_inference_ns=r['full_inference_ns'])
            row['old_route']=row['old_teacher_gain']>=.02 and row['played_gain']>=-.01
            row['played_route']=row['played_gain']>=.1 and row['old_teacher_gain']>=-.02
            row['common_guards']=row['bt4_gain']>=-.01 and row['full_inference_ns']<=10000;seeds.append(row)
        families.append(dict(mode=mode,seeds=seeds,passes=(all(s['old_route'] for s in seeds) or all(s['played_route'] for s in seeds)) and all(s['common_guards'] for s in seeds)))
    result=dict(plan_sha256=sha(folder/'r040-plan.json'),validation_sha256=sha(folder/'r040-validation.json'),analysis_sha256=sha(Path(__file__)),analysis_resource=resource,records=records,mechanism=mechanism,families=families,
                transfer_followup_justified=(all(m['passes'] for m in mechanism) and all(arm('CONTEXT',seed)['full_inference_ns']<=10000 for seed in (71,97))) or any(f['passes'] for f in families),
                elapsed_seconds=sum(e['elapsed_seconds'] for r in records for e in r['executions']),maximum_cpu_percent=max(s['cpu_percent'] for s in samples),minimum_available_gib=min(s['available_gib'] for s in samples),
                scope='Four fixed one-epoch neural endpoints;old and eight-root played consumed development;two prespecified family routes are screening,not independent confirmation;no strength claim.')
    with (folder/'r040-results.json').open('x') as stream:json.dump(result,stream,indent=2);stream.write('\n')
    print(json.dumps(dict(mechanism=mechanism,families=families,metrics=[dict(mode=r['result']['mode'],seed=r['result']['seed'],train=r['result']['metrics'][-1]['train_mse'],old_teacher=r['result']['metrics'][-1]['validation_mse'],played=r['result']['played_validation_mse'],bt4=r['result']['original_bt4_validation_mse'],cost_ns=r['result']['full_inference_ns']) for r in records],transfer_followup_justified=result['transfer_followup_justified'],elapsed_seconds=result['elapsed_seconds'],maximum_cpu_percent=result['maximum_cpu_percent'],minimum_available_gib=result['minimum_available_gib']),indent=2))


if __name__ == '__main__':
    main()
