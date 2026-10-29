"""R035 reconcile data-generation games, exclusions, labels and frozen predictions."""
import json
import re
from pathlib import Path
import chess
from baseline import ROOT, admit, sha, summarize, parse_receipt
from compare_neural import replay
from prepare_objective import alias


def main():
    resource=admit(initial=True);folder=ROOT/'docs/research/brn-relational-cglhw'
    selection=json.loads((folder/'r035-corpus-selection.json').read_text())
    games=[];samples=[];comparisons=[]
    for seed in (71,97):
        plan_path=folder/f'r035-{seed}-plan.json';plan=json.loads(plan_path.read_text())
        out=ROOT/plan['output'];binding=json.loads((out/'binding.json').read_text())
        assert binding['plan_sha256']==sha(plan_path) and binding['runner_sha256']==sha(ROOT/'tools/brn-relational/compare_neural.py')
        for name,digest in binding['runtime'].items():assert sha(ROOT/plan['runtime']/name)==digest
        assert sha(ROOT/plan['openings'])==plan['openings_sha256']
        openings=json.loads((ROOT/plan['openings']).read_text());record=json.loads((out/'results.json').read_text())[0]
        directory=out/record['id'];current=[]
        for i,opening in enumerate(openings):
            for color in (True,False):
                path=directory/f'game-{i:03d}-{int(color)}.json';assert sha(path)==record['games'][path.name]
                game=json.loads(path.read_text());assert game['opening']==i and game['left_color']==color
                replay(game,opening,plan);current.append(game)
        probes=[]
        for path in directory.glob('probes-*.json'):
            data=json.loads(path.read_text());assert len(data)==8
            for probe in data:
                assert probe['budget']==10000 and probe['fen'] in plan['probe_fens']
                r=probe['repeats'];assert len(r)==2
                norm=lambda x:(x['nodes'],x['termination'],x['move'],re.sub(r' time \d+| nps \d+','',x['info'] or ''))
                assert norm(r[0])==norm(r[1])
                for request in r:
                    assert chess.Move.from_uci(request['move']) in chess.Board(probe['fen']).legal_moves
                    parse_receipt([f"info string request nodes={request['nodes']} elapsedNanos={request['elapsed_ns']} termination={request['termination']}"],10000)
            probes.append(dict(path=str(path.relative_to(ROOT)),sha256=sha(path),requests=16))
        releases=[]
        for path in directory.glob('release-*.json'):
            r=json.loads(path.read_text());assert r['remaining']==0 and not r['errors']
            samples.extend(r['resources']);releases.append(dict(path=str(path.relative_to(ROOT)),sha256=sha(path)))
        assert probes and releases
        summary=summarize(current,24)
        assert {k:v for k,v in summary.items() if k!='scope'}=={k:v for k,v in record['summary'].items() if k!='scope'}
        comparisons.append(dict(seed=seed,plan_sha256=sha(plan_path),binding=binding,summary=summary,probes=probes,releases=releases,elapsed_seconds=record['elapsed_seconds']))
        games.extend(current)
    teacher_path=folder/'raw/r035-teacher-data/result.json';teacher=json.loads(teacher_path.read_text())
    datasets={};keys={}
    for name,spec in selection['datasets'].items():
        path=ROOT/spec['positions'];assert sha(path)==spec['positions_sha256']
        positions=json.loads(path.read_text());assert len(positions)==spec['rows']
        labeled=ROOT/teacher['datasets'][name]['data'];assert sha(labeled)==teacher['datasets'][name]['sha256']
        lines=labeled.read_text().splitlines();assert len(lines)==len(positions)
        keys[name]=set()
        for position,line in zip(positions,lines):
            fen,target=line.split('\t');assert fen==position['fen'] and 0<=float(target)<=1
            board=chess.Board(fen);assert board.is_valid() and not board.is_check()
            key=alias(board);assert key==position['alias'];keys[name].add(key)
            expected='train' if position['sources'][0]['opening']<8 else 'validation';assert expected==name
            assert all(('train' if source['opening']<8 else 'validation')==name for source in position['sources'])
        assert len(keys[name])==len(positions)
        datasets[name]=spec|dict(labeled=str(labeled.relative_to(ROOT)),labeled_sha256=sha(labeled))
    assert not(keys['train']&keys['validation'])
    prediction_path=folder/'raw/r035-prediction.json';prediction=json.loads(prediction_path.read_text())
    assert sha(prediction_path)==json.loads(prediction_path.with_suffix('.receipt.json').read_text())['sha256']
    assert prediction['teacher_targets']==prediction['bt4_targets']
    assert prediction['rows']==datasets['validation']['rows']
    models=[{k:v for k,v in model.items() if k not in ('logits','bt4_mse')} for model in prediction['models']]
    result=dict(selection_sha256=sha(folder/'r035-corpus-selection.json'),analysis_sha256=sha(Path(__file__)),analysis_resource=resource,
                teacher_result_sha256=sha(teacher_path),prediction_path=str(prediction_path.relative_to(ROOT)),prediction_sha256=sha(prediction_path),
                comparisons=comparisons,datasets=datasets,models=models,completed_games=len(games),caps=sum(g['score'] is None for g in games),
                exact_teacher_relabel_parity=True,unique_disjoint_partition_parity=True,training_allocation_justified=selection['training_allocation_justified'],
                max_cpu_percent=max(s['cpu_percent'] for s in samples),minimum_available_gib=min(s['available_gib'] for s in samples),
                scope='Data generation on24 prospectively grouped opening clusters;not final strength confirmation;CP64 is a reference;played roots do not cover search leaves;no training performed.')
    with (folder/'r035-results.json').open('x') as stream:json.dump(result,stream,indent=2);stream.write('\n')
    print(json.dumps(dict(datasets=datasets,models=models,completed_games=len(games),caps=result['caps'],training_allocation_justified=result['training_allocation_justified'],scores=[c['summary'] for c in comparisons],max_cpu=result['max_cpu_percent'],minimum_free_gib=result['minimum_available_gib']),indent=2))


if __name__ == '__main__':
    main()
