"""Reconcile R043 fixed-node games, deterministic probes and owned-process release."""
import json
import re
from pathlib import Path
import chess
from baseline import ROOT, admit, sha, summarize, parse_receipt
from compare_neural import replay


def main():
    resource = admit(initial=True)
    folder = ROOT/'docs/research/brn-relational-cglhw'
    plan_path = folder/'r043-plan.json'; plan = json.loads(plan_path.read_text())
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
    prior = json.loads((folder/'r030-results.json').read_text())
    assert json.loads((folder/'r030-plan.json').read_text())['openings_sha256']==plan['openings_sha256']
    points = [dict(seed=seed,tied_score=now['summary']['score'],one_score=old['summary']['score'],difference=now['summary']['score']-old['summary']['score']) for seed,now,old in zip((71,97),comparisons,prior['comparisons'])]
    samples = [s for c in comparisons for r in c['releases'] for s in r['resources']]
    result = dict(plan_sha256=sha(plan_path),runtime_manifest_sha256=sha(folder/'r043-runtime-manifest.json'),binding=binding,analysis_sha256=sha(Path(__file__)),analysis_resource=resource,comparisons=comparisons,descriptive_r030_point_comparison=points,
                  completed_games=sum(c['summary']['completed_games'] for c in comparisons),caps=sum(c['summary']['censored'] for c in comparisons),
                  max_cpu_percent=max(s['cpu_percent'] for s in samples),minimum_available_gib=min(s['available_gib'] for s in samples),
                  node_budget=plan['nodes'],source_revision=plan['source_revision'],build=plan['build'],
                  broader_confirmation_justified=all(c['summary']['score']>=.45 and c['summary']['censored']==0 for c in comparisons),
                  limitations='Eight consumed opening clusters;small diagnostic,unknown training-seed distribution;point-score differences do not establish improvement,noninferiority or equal-time Elo. No production promotion.')
    with (folder/'r043-results.json').open('x') as stream:
        json.dump(result,stream,indent=2);stream.write('\n')
    print(json.dumps(dict(comparisons=[{k:v for k,v in c.items() if k not in ('games','releases','probes')} for c in comparisons],points=points,broader_confirmation=result['broader_confirmation_justified'],max_cpu=result['max_cpu_percent'],minimum_free_gib=result['minimum_available_gib']),indent=2))


if __name__ == '__main__':
    main()
