"""R042 frozen larger-panel and earlier-played transfer; no refit or strength claim."""
import json
from pathlib import Path
from baseline import ROOT, admit, sha
from analyze_fields import sigmoid


def main():
    resource=admit(initial=True);folder=ROOT/'docs/research/brn-relational-cglhw'
    def read(name):
        path=folder/('raw/'+name+'.json');receipt=json.loads(path.with_suffix('.receipt.json').read_text());assert sha(path)==receipt['sha256']
        return json.loads(path.read_text()),dict(path=str(path.relative_to(ROOT)),sha256=sha(path),receipt=receipt)
    old,old_ref=read('r042-old-prediction');played,played_ref=read('r042-played-prediction')
    old_base,old_base_ref=read('r029-prediction');played_base,played_base_ref=read('r034-prediction');original,original_ref=read('r026-prediction');old_pairs,old_pairs_ref=read('r024-prediction')
    for other in (old_base,old_pairs):
        for key in ('rows','teacher_targets','bt4_targets','pieces'):assert old[key]==other[key]
    for other in (played_base,original):
        for key in ('rows','teacher_targets','teacher_cp','pieces','in_check'):assert played[key]==other[key]
    assert old['rows']==8192 and played['rows']==991
    noncheck=[i for i,c in enumerate(played['in_check']) if not c];assert len(noncheck)==876
    cases_path=folder/'raw/r026-data/cases.json';assert sha(cases_path)=='35a6382b538132367d59378038286c721c1d3945a466b084a01e8b2775f760f4'
    cases=json.loads(cases_path.read_text())['positions'];root_groups={}
    for i in noncheck:
        for source in cases[i]['sources']:
            opening=int(Path(source['game']).stem.split('-')[1]);root_groups.setdefault(str(opening),set()).add(i)
    assert len(root_groups)==8
    panels={};losses={}
    for name,data,baseline,pairs in (('old',old,old_base,old_pairs),('played',played,played_base,original)):
        models=[];losses[name]={};ids=set()
        for model in data['models']+baseline['models']+pairs['models']:
            if model['id'] in ids:continue
            ids.add(model['id']);labels=('teacher','bt4') if name=='old' else ('teacher',)
            loss={label:[(sigmoid(x)-y)**2 for x,y in zip(model['logits'],data[label+'_targets'])] for label in labels}
            assert len(model['logits'])==data['rows']
            for label in labels:assert abs(sum(loss[label])/data['rows']-model[label+'_mse'])<1e-13
            losses[name][model['id']]=loss
            subsets={}
            for lo,hi in ((2,10),(11,20),(21,32)):
                indices=[i for i,p in enumerate(data['pieces']) if lo<=p<=hi and (name=='old' or not data['in_check'][i])]
                subsets[f'pieces{lo}-{hi}']=dict(rows=len(indices),teacher_mse=sum(loss['teacher'][i] for i in indices)/len(indices) if indices else None)
            row={k:v for k,v in model.items() if k!='logits'}|dict(phases=subsets)
            if name=='played':
                row['noncheck_teacher_mse']=sum(loss['teacher'][i] for i in noncheck)/len(noncheck)
                row['by_root']={root:dict(rows=len(indices),teacher_mse=sum(loss['teacher'][i] for i in indices)/len(indices)) for root,indices in root_groups.items()}
            models.append(row)
        panels[name]=models
    def mse(panel,model,label='teacher'):
        loss=losses[panel][model][label];indices=range(len(loss)) if panel=='old' else noncheck
        return sum(loss[i] for i in indices)/len(indices)
    gates=[]
    for seed in (71,97):
        tied=f'r041-tied-{seed}';masked=f'r041-mask-{seed}';control=f'r040-context-{seed}';base=f'r028-one-{seed}'
        for panel in (old,played):
            t=next(m for m in panel['models'] if m['id']==tied);m=next(m for m in panel['models'] if m['id']==masked)
            assert t['state_sha256']==m['state_sha256']==sha(folder/f'raw/r041-tied-{seed}/epoch-1.state')
        g=dict(seed=seed,old_teacher_gain_over_control=1-mse('old',tied)/mse('old',control),old_bt4_gain_over_control=1-mse('old',tied,'bt4')/mse('old',control,'bt4'),played_gain_over_control=1-mse('played',tied)/mse('played',control),second_used_old=mse('old',masked)>mse('old',tied),second_used_played=mse('played',masked)>mse('played',tied),old_teacher_gain_over_r028=1-mse('old',tied)/mse('old',base),old_bt4_gain_over_r028=1-mse('old',tied,'bt4')/mse('old',base,'bt4'),played_gain_over_r028=1-mse('played',tied)/mse('played',base))
        g['passes']=g['old_teacher_gain_over_control']>=.02 and g['old_bt4_gain_over_control']>=-.01 and g['played_gain_over_control']>=.05 and g['second_used_old'] and g['second_used_played'] and g['old_teacher_gain_over_r028']>=.02 and g['old_bt4_gain_over_r028']>=-.01 and g['played_gain_over_r028']>=.05
        t=next(m for m in panels['played'] if m['id']==tied);c=next(m for m in panels['played'] if m['id']==control)
        g['roots_improved_over_control']=sum(t['by_root'][root]['teacher_mse']<c['by_root'][root]['teacher_mse'] for root in root_groups);gates.append(g)
    result=dict(old_evidence=old_ref,played_evidence=played_ref,old_r028_reference=old_base_ref,played_r028_reference=played_base_ref,original_played_reference=original_ref,old_pair_reference=old_pairs_ref,
                validation_sha256=sha(folder/'r042-validation.json'),analysis_sha256=sha(Path(__file__)),resources=resource,target_and_metadata_parity=True,panels=panels,gates=gates,
                playing_calibration_justified=all(g['passes'] for g in gates),scope='Frozen consumed development;unknown old game correlation,eight earlier-played opening clusters;no refit,final confirmation or strength claim.')
    with (folder/'r042-results.json').open('x') as stream:json.dump(result,stream,indent=2);stream.write('\n')
    print(json.dumps(dict(gates=gates,playing_calibration_justified=result['playing_calibration_justified'],models={panel:[{k:v for k,v in m.items() if k not in ('phases','by_root')} for m in models if m['id'].startswith(('r041','r040','pair24','gen32'))] for panel,models in panels.items()}),indent=2))


if __name__ == '__main__':
    main()
