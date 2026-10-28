"""Reconcile the fixed R031 endpoints, controls, checkpoints and resource evidence."""
import json
import math
import struct
from pathlib import Path
from baseline import ROOT, admit, sha


def main():
    resource = admit(initial=True)
    folder = ROOT/'docs/research/brn-relational-cglhw'
    plan_path = folder/'r031-plan.json'
    plan = json.loads(plan_path.read_text())
    records = []
    for config_path in plan['configs']:
        path = ROOT/config_path
        config = json.loads(path.read_text())
        out = ROOT/config['output']
        binding = json.loads((out/'binding.json').read_text())
        assert sha(path) == binding['config_sha256']
        assert sha(ROOT/'tools/brn-relational/run_objective.py') == binding['runner_sha256']
        for file, expected in binding['runtime_sha256'].items():
            assert sha(ROOT/file) == expected
        for key,hash_key in (('parent_config','parent_config_sha256'),('initial_state','initial_state_sha256'),('parent_receipt','parent_receipt_sha256')):
            assert sha(ROOT/config[key]) == config[hash_key]
        result = json.loads((out/'result.json').read_text())
        parent_receipt = json.loads((ROOT/config['parent_receipt']).read_text())
        assert abs(result['metrics'][0]['validation_mse']-parent_receipt['validation_mse']) < 1e-13
        endpoint = json.loads((out/'epoch-2.json').read_text())
        assert endpoint == result['metrics'][-1] and endpoint['updates'] == 16384
        assert sha(out/'epoch-2.state') == endpoint['state_sha256']
        segments = []
        for n in range(1,9):
            receipt = json.loads((out/f'segment-{n}.json').read_text())
            assert receipt['completed_positions'] == n*131072 and receipt['updates'] == 8192+n*1024
            assert receipt['config_sha256'] == sha(path)
            assert sha(out/f'segment-{n}.state') == receipt['state_sha256']
            segments.append(dict(path=str((out/f'segment-{n}.state').relative_to(ROOT)), **receipt))
        assert segments[-1]['state_sha256'] == endpoint['state_sha256']
        state = (out/'epoch-2.state').read_bytes()
        magic,mode,count,rate,step = struct.unpack('>Qiidq',state[:32])
        assert magic == 0x42524e43504e3031 and count == 62401 and rate == config['rate'] and step == 16384
        assert mode == (config['mode']=='TWO') and len(state) == 40+24*count
        values = struct.unpack('>'+str(count)+'d',state[32:32+8*count])
        families = {}
        boundaries = [0,769,6913,28513,34657,56257,62401]
        for i,name in enumerate(('unary','source','first_relation','first_readout','second_relation','second_readout')):
            a = values[boundaries[i]:boundaries[i+1]]
            assert all(math.isfinite(x) and abs(x)<=.99 for x in a)
            families[name] = dict(rms=math.sqrt(sum(x*x for x in a)/len(a)),clamped=sum(abs(x)>=.99 for x in a)/len(a))
        executions = [json.loads(p.read_text()) for p in sorted(out.glob('execution-*.json'))]
        assert executions and all(e['process_absent'] for e in executions) and executions[-1]['status']=='COMPLETE'
        records.append(dict(config=config_path,config_sha256=sha(path),result_sha256=sha(out/'result.json'),
                            result=result,segments=segments,parameter_diagnostics=families,executions=executions))
    gates = []
    for seed in (71,97):
        control = next(r['result'] for r in records if r['result']['seed']==seed and r['result']['rate']==.01)
        low = next(r['result'] for r in records if r['result']['seed']==seed and r['result']['rate']==.001)
        parent_path = folder/f'raw/r028-one-{seed}/result.json'
        parent = json.loads(parent_path.read_text())
        parent_summary = json.loads((folder/'r028-results.json').read_text())
        assert parent == next(r['result'] for r in parent_summary['records'] if r['result']['seed']==seed and r['result']['mode']=='ONE')
        a,b,c = control['metrics'][-1],low['metrics'][-1],parent['metrics'][-1]
        gate = dict(seed=seed,teacher_gain_over_control=1-b['validation_mse']/a['validation_mse'],
                    teacher_gain_over_parent=1-b['validation_mse']/c['validation_mse'],
                    control_teacher_gain_over_parent=1-a['validation_mse']/c['validation_mse'],
                    train_gain_over_control=1-b['train_mse']/a['train_mse'],
                    train_gain_over_parent=1-b['train_mse']/c['train_mse'],
                    bt4_gain_over_control=1-low['original_bt4_validation_mse']/control['original_bt4_validation_mse'],
                    bt4_gain_over_parent=1-low['original_bt4_validation_mse']/parent['original_bt4_validation_mse'],
                    full_gain_over_unary=1-b['validation_mse']/b['unary_only_validation_mse'],
                    parent_path=str(parent_path.relative_to(ROOT)),parent_sha256=sha(parent_path))
        gate['passes'] = gate['teacher_gain_over_control']>=.05 and gate['teacher_gain_over_parent']>=.05 and gate['bt4_gain_over_control']>=-.01 and gate['bt4_gain_over_parent']>=-.01 and gate['full_gain_over_unary']>=.01
        gates.append(gate)
    samples = [s for r in records for e in r['executions'] for s in e['resources']]
    result = dict(plan_sha256=sha(plan_path),analysis_sha256=sha(Path(__file__)),analysis_resource=resource,records=records,gates=gates,
                  advance=all(g['passes'] for g in gates),elapsed_seconds=sum(e['elapsed_seconds'] for r in records for e in r['executions']),
                  maximum_cpu_percent=max(s['cpu_percent'] for s in samples),minimum_available_gib=min(s['available_gib'] for s in samples),
                  scope='Fixed warm-start endpoints;unchanged inference/moments and equal shrink;no rate/epoch selection beyond declared arms,no search;parents and all segment checkpoints preserved.')
    with (folder/'r031-results.json').open('x') as stream:
        json.dump(result,stream,indent=2);stream.write('\n')
    print(json.dumps({k:result[k] for k in ('gates','advance','elapsed_seconds','maximum_cpu_percent','minimum_available_gib')},indent=2))


if __name__ == '__main__':
    main()
