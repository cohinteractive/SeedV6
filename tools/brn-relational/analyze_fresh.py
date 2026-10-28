"""Fixed-endpoint fresh-panel gate analysis and independent sampled inference parity."""
import argparse
import json
import math
from pathlib import Path
import time
from baseline import ROOT,admit,sha
from inspect_contextual import weights,evaluate


def sigmoid(x):return 1/(1+math.exp(-x)) if x>=0 else math.exp(x)/(1+math.exp(x))


def ratio_gain(a,b):
    n=len(a);mean=sum(a)/n;r=sum(b)/sum(a);z=[y-r*x for x,y in zip(a,b)];center=sum(z)/n
    se=math.sqrt(sum((v-center)**2 for v in z)/(n-1)/n)/mean;gain=1-r
    return dict(gain=gain,descriptive_95_interval=[gain-1.96*se,gain+1.96*se])


def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('plan',type=Path);parser.add_argument('output',type=Path);args=parser.parse_args()
    start=time.monotonic();resource=admit(initial=True);plan=json.loads(args.plan.read_text());raw=ROOT/plan['output'];audit=json.loads(raw.read_text());rows=(ROOT/plan['cases']).read_text().splitlines();models={x['id']:x for x in audit['models']};maximum=0
    if sha(ROOT/plan['cases'])!=plan['cases_sha256'] or sha(args.plan)!=audit['config_sha256']:raise ValueError('Audit input identity')
    if len(rows)!=audit['rows'] or any(float(row.split('\t')[1])!=y for row,y in zip(rows,audit['bt4_targets'])):raise ValueError('Original target parity')
    for spec in plan['models']:
        path=ROOT/spec['state']
        if sha(path)!=spec['state_sha256']:raise ValueError('Model identity')
        w,global_context=weights(path)
        for i,row in enumerate(rows[:64]):
            value,_=evaluate(w,row.split('\t')[0],global_context);error=abs(value-models[spec['id']]['logits'][i])*400;maximum=max(maximum,error)
            if error>1e-9:raise ValueError('Independent neural score mismatch')
    losses={name:{target:[(sigmoid(x)-y)**2 for x,y in zip(model['logits'],audit[target+'_targets'])] for target in ['bt4','teacher']} for name,model in models.items()};gates=[]
    for mode in ['local','global']:
        for seed in [71,97]:
            a=losses[f'r014-{mode}-{seed}'];b=losses[f'r015-{mode}-{seed}'];teacher=ratio_gain(a['teacher'],b['teacher']);bt4=ratio_gain(a['bt4'],b['bt4']);gates.append(dict(mode=mode,seed=seed,teacher=teacher,bt4=bt4,passes=teacher['gain']>=.05 and bt4['gain']>=-.05))
    composition={str(seed):ratio_gain(losses[f'r015-local-{seed}']['teacher'],losses[f'r015-global-{seed}']['teacher']) for seed in [71,97]}
    result=dict(raw_path=str(raw.relative_to(ROOT)),raw_sha256=sha(raw),plan_sha256=sha(args.plan),runner_sha256=sha(Path(__file__)),inference_helper_sha256=sha(Path(__file__).with_name('inspect_contextual.py')),
        models=[{k:v for k,v in m.items() if k!='logits'} for m in models.values()],target_gates=gates,composition=composition,composition_passes=all(v['gain']>=.02 for v in composition.values()),
        independent_checks=dict(models=8,positions_each=64,max_abs_cp_error=maximum,all_original_targets_exact=True),resources=resource,seconds=time.monotonic()-start,
        uncertainty='Paired-position delta-method ratio intervals are descriptive only;unknown source-game correlations can make these intervals too narrow. Fresh BRN development,not independent CP64 validation,final evaluation or playing evidence.')
    with args.output.open('x',encoding='utf8',newline='\n') as f:json.dump(result,f,indent=2);f.write('\n')
    print(json.dumps(dict(gates=gates,composition=composition,independent=result['independent_checks']),indent=2))


if __name__=='__main__':main()
