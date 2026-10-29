"""Reconcile R048 fixed-node games, deterministic probes and owned-process release."""
import json
import re
import math
import random
from pathlib import Path
import chess
from baseline import ROOT, admit, sha, summarize, parse_receipt
from compare_neural import replay


def differences(left,right):
    roots=sorted(set(left)|set(right));values={root:(left.get(root)-right.get(root) if left.get(root) is not None and right.get(root) is not None else None) for root in roots};observed=[v for v in values.values() if v is not None];n=len(observed)
    result=dict(by_root=values,complete_pairs=n,missing_pairs=len(roots)-n,full_allocation_difference_bounds=[(sum(observed)-(len(roots)-n))/len(roots),(sum(observed)+(len(roots)-n))/len(roots)])
    if n:
        mean=sum(observed)/n;rng=random.Random(2026102948);boot=sorted(sum(rng.choice(observed) for _ in range(n))/n for _ in range(10000));width=math.sqrt(2*math.log(40)/n)
        result.update(mean=mean,paired_bootstrap_95=[boot[249],boot[9749]],hoeffding_95=[max(-1,mean-width),min(1,mean+width)])
    return result


def main():
    resource = admit(initial=True)
    folder = ROOT/'docs/research/brn-relational-cglhw'
    plan_path = folder/'r048-plan.json'; plan = json.loads(plan_path.read_text())
    out = ROOT/plan['output']; binding = json.loads((out/'binding.json').read_text())
    assert binding['plan_sha256'] == sha(plan_path)
    assert binding['runner_sha256'] == sha(ROOT/'tools/brn-relational/compare_neural.py')
    assert binding['engine_helper_sha256'] == sha(ROOT/'tools/brn-relational/baseline.py')
    for name,expected in binding['runtime'].items():
        assert sha(ROOT/plan['runtime']/name) == expected
    assert sha(ROOT/plan['openings']) == plan['openings_sha256']
    openings = json.loads((ROOT/plan['openings']).read_text())
    stored = json.loads((out/'results.json').read_text())
    comparisons = []
    for specification in plan['comparisons']:
        name = specification['id']; directory = out/name
        record = next(x for x in stored if x['id']==name)
        games = []; totals = {side:dict(nodes=0,elapsed_ns=0,requests=0) for side in ('neural','cp64')}
        hashes = {}
        for i,opening in enumerate(openings):
            board = chess.Board()
            for move in opening['moves'].split():
                board.push_uci(move)
            first_color = board.turn
            for color in (True,False):
                file = directory/f'game-{i:03d}-{int(color)}.json'
                assert sha(file) == record['games'][file.name]
                g = json.loads(file.read_text());assert g['opening']==i and g['left_color']==color
                replay(g,opening,plan);games.append(g);hashes[file.name]=sha(file)
                for ply,request in enumerate(g['receipts']):
                    turn = first_color if ply%2==0 else not first_color
                    t = totals['neural' if turn==color else 'cp64']
                    t['nodes']+=request['nodes'];t['elapsed_ns']+=request['elapsed_ns'];t['requests']+=1
        summary = summarize(games,2*len(openings))
        assert {k:v for k,v in summary.items() if k!='scope'} == {k:v for k,v in record['summary'].items() if k!='scope'}
        for t in totals.values():
            t['observed_nodes_per_second']=t['nodes']/(t['elapsed_ns']/1e9)
        probes = []
        for file in sorted(directory.glob('probes-*.json')):
            data = json.loads(file.read_text());assert len(data)==8
            for probe in data:
                assert probe['fen'] in plan['probe_fens'] and probe['budget']==plan['nodes']
                observed = probe['repeats'];assert len(observed)==2
                normalize = lambda r:(r['nodes'],r['termination'],r['move'],re.sub(r' time \d+| nps \d+','',r['info'] or ''))
                assert normalize(observed[0])==normalize(observed[1])
                for r in observed:
                    assert chess.Move.from_uci(r['move']) in chess.Board(probe['fen']).legal_moves
                    parse_receipt([f"info string request nodes={r['nodes']} elapsedNanos={r['elapsed_ns']} termination={r['termination']}"],plan['nodes'])
            probes.append(dict(path=str(file.relative_to(ROOT)),sha256=sha(file),requests=16))
        releases = []
        for file in sorted(directory.glob('release-*.json')):
            data = json.loads(file.read_text());assert data['remaining']==0 and not data['errors']
            releases.append(dict(path=str(file.relative_to(ROOT)),sha256=sha(file),**data))
        assert probes and releases
        comparisons.append(dict(id=name,summary=summary,elapsed_seconds=record.get('elapsed_seconds'),request_totals=totals,probes=probes,releases=releases,games=hashes))
    def frozen_root_scores(number,record):
        reference_plan=json.loads((folder/(number+'-plan.json')).read_text())
        assert reference_plan['openings_sha256']==plan['openings_sha256'] and reference_plan['nodes']==plan['nodes'] and reference_plan['max_plies']==plan['max_plies']
        scores={};directory=ROOT/reference_plan['output']/record['id']
        for i,opening in enumerate(openings):
            pair=[]
            for color in (True,False):
                file=directory/f'game-{i:03d}-{int(color)}.json';assert sha(file)==record['games'][file.name]
                game=json.loads(file.read_text());assert game['opening']==i and game['left_color']==color;replay(game,opening,reference_plan);pair.append(game['score'])
            scores[str(i)]=sum(pair)/2 if None not in pair else None
        return scores
    mixed_path=folder/'r047-results.json';parent_path=folder/'r043-results.json';mixed=json.loads(mixed_path.read_text());parent=json.loads(parent_path.read_text());attribution=[]
    for seed,control in zip((71,97),comparisons):
        assert control['id']==f'low{seed}-vs-cp64'
        m=next(x for x in mixed['comparisons'] if x['id']==f'mix{seed}-vs-cp64');p=next(x for x in parent['comparisons'] if x['id']==f'tied{seed}-vs-cp64')
        controls=frozen_root_scores('r048',control);mixes=frozen_root_scores('r047',m);parents=frozen_root_scores('r043',p)
        attribution.append(dict(seed=seed,mixed_score=m['summary']['score'],control_score=control['summary'].get('score'),parent_score=p['summary']['score'],mix_minus_matched_control=differences(mixes,controls),control_minus_parent=differences(controls,parents)))
    samples=[s for c in comparisons for r in c['releases'] for s in r['resources']]
    consistent=all(a['mix_minus_matched_control']['missing_pairs']==0 and a['mix_minus_matched_control'].get('mean',-1)>=.05 for a in attribution)
    result=dict(plan_sha256=sha(plan_path),validation_sha256=sha(folder/'r048-validation.json'),runtime_manifest_sha256=sha(folder/'r047-runtime-manifest.json'),binding=binding,analysis_sha256=sha(Path(__file__)),analysis_resource=resource,comparisons=comparisons,attribution=attribution,mixed_evidence_sha256=sha(mixed_path),parent_evidence_sha256=sha(parent_path),completed_games=sum(c['summary']['completed_games'] for c in comparisons),caps=sum(c['summary']['censored'] for c in comparisons),max_cpu_percent=max(s['cpu_percent'] for s in samples),minimum_available_gib=min(s['available_gib'] for s in samples),node_budget=plan['nodes'],source_revision=plan['source_revision'],build=plan['build'],directionally_consistent_mixing_effect=consistent,broader_confirmation_justified=False,limitations='Eight consumed opening clusters and selected candidates;bootstrap descriptive,conservative bounded-difference intervals reported. Architecture contrast holds training fixed;training contrast jointly changes exposure/rate/shrink. No Elo,noninferiority,promotion or automatic match expansion.')
    with (folder/'r048-results.json').open('x') as stream:json.dump(result,stream,indent=2);stream.write('\n')
    print(json.dumps(dict(comparisons=[{k:v for k,v in c.items() if k not in ('games','releases','probes')} for c in comparisons],attribution=attribution,directionally_consistent=consistent,max_cpu=result['max_cpu_percent'],minimum_free_gib=result['minimum_available_gib']),indent=2))


if __name__ == '__main__':
    main()
