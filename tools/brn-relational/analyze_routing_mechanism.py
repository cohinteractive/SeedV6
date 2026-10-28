"""R033 reconcile frozen routing decomposition without granting further attention tuning."""
import json
from pathlib import Path
from baseline import ROOT, admit, sha


def main():
    resource = admit(initial=True)
    folder = ROOT/'docs/research/brn-relational-cglhw'
    path = folder/'raw/r033-mechanism.json'
    receipt = json.loads(path.with_suffix('.receipt.json').read_text())
    assert sha(path) == receipt['sha256']
    data = json.loads(path.read_text());assert data['teacher_row_parity']
    previous = json.loads((folder/'r032-results.json').read_text())
    models = []
    for model in data['models']:
        seed = int(model['id'].split('-')[-1])
        parent = next(r['result'] for r in previous['records'] if r['result']['mode']=='DYNAMIC' and r['result']['seed']==seed)
        assert parent['metrics'][-1]['state_sha256'] == model['state_sha256']
        old = model['old_development'];played=model['played_noncheck']
        assert abs(old['full_teacher_mse']-parent['metrics'][-1]['validation_mse']) < 1e-13
        assert abs(old['geometry_only_teacher_mse']-parent['metrics'][-1]['content_masked_validation_mse']) < 1e-13
        panels = {}
        for name,rows in (('old_development',4096),('played_all',991),('played_noncheck',876)):
            panel=model[name];assert panel['rows']==rows
            assert panel['maximum_full_reconstruction_error']<=1e-12 and panel['maximum_geometry_reconstruction_error']<=1e-12
            metrics=panel['metrics']
            route_keys=('geometry_centered_rms','static_content_centered_rms','context_centered_rms','normalized_entropy','remove_all_content_attention_l1','remove_neighborhood_attention_l1')
            means={key:metrics[key]['mean'] for key in route_keys}
            means['context_to_geometry_centered_rms_ratio']=means['context_centered_rms']/means['geometry_centered_rms']
            activations={key:{field:metrics[key][field] for field in ('mean','rms','minimum','maximum')} for key in ('first_preactivation','first_activation','first_softsign_derivative','second_preactivation','second_activation','second_softsign_derivative','unary_logit','first_pass_logit_contribution','second_pass_logit_contribution')}
            panels[name]={k:v for k,v in panel.items() if k!='metrics'} | dict(routing_means=means,activations=activations,
                full_gain_over_geometry_mask=1-panel['full_teacher_mse']/panel['geometry_only_teacher_mse'],
                full_gain_over_neighborhood_mask=1-panel['full_teacher_mse']/panel['static_content_teacher_mse'],
                weak_all_content_routing=means['remove_all_content_attention_l1']<.02,
                weak_neighborhood_routing=means['remove_neighborhood_attention_l1']<.02,
                weak_context_logit_relative_to_geometry=means['context_to_geometry_centered_rms_ratio']<.1)
        models.append(dict(id=model['id'],state_sha256=model['state_sha256'],panels=panels,played_noncheck_to_development_loss=played['full_teacher_mse']/old['full_teacher_mse']))
    result=dict(raw_path=str(path.relative_to(ROOT)),raw_sha256=sha(path),plan_sha256=sha(folder/'r033-plan.json'),analysis_sha256=sha(Path(__file__)),analysis_resource=resource,receipt=receipt,models=models,
                search_distribution_experiment_justified=all(m['played_noncheck_to_development_loss']>=2 for m in models),
                attention_tuning_authorized=False,
                scope='Frozen consumed panels;per-position loss,per-node/channel activation and per-receiver routing statistics;no fitted ablation,causal strength claim or attention-family rejection.')
    with (folder/'r033-results.json').open('x') as stream:
        json.dump(result,stream,indent=2);stream.write('\n')
    print(json.dumps(dict(models=[dict(id=m['id'],played_to_development_loss=m['played_noncheck_to_development_loss'],panels={k:{key:v[key] for key in ('full_teacher_mse','full_gain_over_geometry_mask','full_gain_over_neighborhood_mask','routing_means')} for k,v in m['panels'].items() if k!='played_all'}) for m in models],search_distribution_experiment_justified=result['search_distribution_experiment_justified']),indent=2))


if __name__ == '__main__':
    main()
