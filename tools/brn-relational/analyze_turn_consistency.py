"""R050 frozen odd/even perspective decomposition; no score fitting."""
import json
import math
from pathlib import Path
import time
import chess
from baseline import ROOT, admit, sha
from analyze_fields import sigmoid
from analyze_move_neighborhood import mean, paired_interval


def summary(rows):
    result=dict(pairs=len(rows))
    for endpoint in ('parent','child','delta_cp'):
        original=[r[endpoint+'_original'] for r in rows]
        projected=[r[endpoint+'_projected'] for r in rows]
        difference=[o-p for o,p in zip(original,projected)]
        result[endpoint]=dict(original_mse=mean(original),projected_mse=mean(projected),
                              relative_gain=1-mean(projected)/mean(original),
                              mean_improvement=mean(difference),approximate_95=paired_interval(difference))
        if endpoint=='delta_cp':
            result[endpoint]['original_rmse']=math.sqrt(mean(original))
            result[endpoint]['projected_rmse']=math.sqrt(mean(projected))
    substantial=[r for r in rows if abs(r['teacher_delta'])>=100]
    result['substantial_teacher_deltas']=len(substantial)
    result['original_sign_disagreement']=mean([float(r['original_delta']*r['teacher_delta']<=0) for r in substantial])
    result['projected_sign_disagreement']=mean([float(r['projected_delta']*r['teacher_delta']<=0) for r in substantial])
    return result


def main():
    started=time.monotonic();samples=[admit(initial=True)]
    folder=ROOT/'docs/research/brn-relational-cglhw'
    selection=json.loads((folder/'r050-selection.json').read_text())
    for name,digest in (selection['inputs']|selection['files']).items():assert sha(ROOT/name)==digest
    old_selection=json.loads((folder/'r049-selection.json').read_text())
    assert sha(folder/'r049-selection.json')==selection['source_sha256']
    evidence=[];rows={};decomposition={};teacher_even=[];sampled=started
    for shard,(old_name,new_name) in enumerate(zip(old_selection['plans'],selection['plans'])):
        data=[];cases=[]
        for name in (old_name,new_name):
            planpath=ROOT/name;plan=json.loads(planpath.read_text());path=ROOT/plan['output']
            receipt=json.loads(path.with_suffix('.receipt.json').read_text())
            assert sha(path)==receipt['sha256']
            binding=json.loads(path.with_suffix('.binding.json').read_text())
            assert binding['plan_sha256']==sha(planpath)
            assert binding['runner_sha256']==sha(ROOT/'tools/brn-relational/run_message_audit.py')
            for name,digest in binding['runtime'].items():assert sha(ROOT/plan['runtime']/name)==digest
            for model in plan['models']:assert sha(ROOT/model['state'])==model['state_sha256']
            output=json.loads(path.read_text());assert output['config_sha256']==sha(planpath)
            assert output['rows']==1024 and not any(output['in_check'])
            data.append(output);cases.append(json.loads((ROOT/plan['cases']).read_text())['positions'])
            evidence.append(dict(plan=str(planpath.relative_to(ROOT)),plan_sha256=sha(planpath),
                                 output=plan['output'],output_sha256=sha(path),receipt=receipt))
        original,opposite=data
        for i,(a,b) in enumerate(zip(*cases)):
            first=chess.Board(a['fen']);second=chess.Board(b['fen'])
            assert first.board_fen()==second.board_fen() and first.turn!=second.turn
            assert first.is_valid() and second.is_valid() and not first.is_check() and not second.is_check()
            teacher_even.append((original['teacher_cp'][i]+opposite['teacher_cp'][i])/2)
        for model in original['models']:
            other=next(m for m in opposite['models'] if m['id']==model['id'])
            assert model['kind']==other['kind'] and model['state_sha256']==other['state_sha256']
            group=rows.setdefault(model['id'],[])
            parts=decomposition.setdefault(model['id'],dict(odd_error2=0.,even_error2=0.,total_error2=0.,rows=0,even_sum=0.))
            predictions=[]
            for i,(a,b) in enumerate(zip(model['logits'],other['logits'])):
                a*=400;b*=400;ta=original['teacher_cp'][i];tb=opposite['teacher_cp'][i]
                odd=(a-b)/2;even=(a+b)/2;teacher_odd=(ta-tb)/2;teacher_e=(ta+tb)/2
                odd2=(odd-teacher_odd)**2;even2=(even-teacher_e)**2;total=((a-ta)**2+(b-tb)**2)/2
                assert abs(odd2+even2-total)<=1e-8*max(1,total)
                parts['odd_error2']+=odd2;parts['even_error2']+=even2;parts['total_error2']+=total
                parts['rows']+=1;parts['even_sum']+=even
                predictions.append(odd)
            for i in range(0,1024,2):
                tp,tc=original['teacher_cp'][i:i+2];mp,mc=model['logits'][i:i+2];ap,ac=predictions[i:i+2]
                td=-tc-tp;md=(-mc-mp)*400;ad=-ac-ap
                group.append(dict(parent_original=(sigmoid(mp)-sigmoid(tp/400))**2,
                                  child_original=(sigmoid(mc)-sigmoid(tc/400))**2,
                                  parent_projected=(sigmoid(ap/400)-sigmoid(tp/400))**2,
                                  child_projected=(sigmoid(ac/400)-sigmoid(tc/400))**2,
                                  delta_cp_original=(md-td)**2,delta_cp_projected=(ad-td)**2,
                                  teacher_delta=td,original_delta=md,projected_delta=ad,
                                  restricted=max(abs(tp),abs(tc))<=800))
        if time.monotonic()-sampled>=5:samples.append(admit());sampled=time.monotonic()
        if time.monotonic()-started>60:raise TimeoutError('Bounded audit analysis exhausted')
    models={name:dict(all=summary(group),restricted=summary([r for r in group if r['restricted']])) for name,group in rows.items()}
    for name,parts in decomposition.items():
        models[name]['perspective_error_decomposition']=dict(
            odd_cp_mse=parts['odd_error2']/parts['rows'],even_cp_mse=parts['even_error2']/parts['rows'],
            both_perspectives_cp_mse=parts['total_error2']/parts['rows'],
            even_error_fraction=parts['even_error2']/parts['total_error2'],mean_even_cp=parts['even_sum']/parts['rows'])
    ordered=sorted(teacher_even);center=mean(teacher_even)
    teacher=dict(mean_even_cp=center,rms_even_cp=math.sqrt(mean([v*v for v in teacher_even])),
                 standard_deviation=math.sqrt(mean([(v-center)**2 for v in teacher_even])),
                 percentile5=ordered[int(.05*len(ordered))],percentile95=ordered[int(.95*len(ordered))])
    gates=[]
    for seed in (71,97):
        row=models[f'r045-mix-{seed}']['all']
        gate=dict(seed=seed,child_mse_gain=row['child']['relative_gain'],delta_cp_mse_gain=row['delta_cp']['relative_gain'],
                  child_improvement_lower95=row['child']['approximate_95'][0])
        gate['passes']=gate['child_mse_gain']>=.05 and gate['delta_cp_mse_gain']>=.05 and gate['child_improvement_lower95']>0
        gates.append(gate)
    result=dict(selection_sha256=sha(folder/'r050-selection.json'),analysis_sha256=sha(Path(__file__)),
                interval_helper_sha256=sha(Path(__file__).with_name('analyze_move_neighborhood.py')),
                protocol_sha256=sha(folder/'R050.md'),teacher=teacher,models=models,gates=gates,
                perspective_prototype_justified=all(g['passes'] for g in gates),evidence=evidence,
                seconds=time.monotonic()-started,resources=samples,
                limitations='Frozen development mechanism diagnostic;random legal children,known fitted parents,unknown source-game correlations. CP64 tempo need not be zero;projection is not an engine promotion or strength result.')
    with (folder/'r050-results.json').open('x') as stream:json.dump(result,stream,indent=2);stream.write('\n')
    print(json.dumps(dict(teacher=teacher,gates=gates,perspective_prototype_justified=result['perspective_prototype_justified'],models=models),indent=2))


if __name__=='__main__':main()
