"""Reconcile complete R057 fixed-node games, paired uncertainty, throughput and PGNs."""
import argparse
import datetime
import json
import math
from pathlib import Path
import random
import chess
import chess.pgn
from baseline import ROOT,sha,parse_receipt
import compute_gate


def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('plan',type=Path);parser.add_argument('output',type=Path);args=parser.parse_args()
    compute_gate.require();plan=json.loads(args.plan.read_text());out=ROOT/plan['output'];binding=json.loads((out/'binding.json').read_text())
    assert binding['plan_sha256']==sha(args.plan) and binding['runner_sha256']==sha(ROOT/'tools/brn-relational/compare_neural.py')
    assert binding['engine_helper_sha256']==sha(ROOT/'tools/brn-relational/baseline.py')
    for name,digest in binding['runtime'].items():assert sha(ROOT/plan['runtime']/name)==digest
    assert sha(ROOT/plan['openings'])==plan['openings_sha256'];openings=json.loads((ROOT/plan['openings']).read_text())
    assert len(plan['comparisons'])==1;comparison=plan['comparisons'][0];directory=out/comparison['id'];games=[];hashes={};pairs=[]
    totals={side:dict(nodes=0,elapsed_ns=0,requests=0) for side in ['one','cp64']};pgn_dir=directory/'pgn';pgn_dir.mkdir(exist_ok=True)
    for i,opening in enumerate(openings):
        compute_gate.require();pair=[]
        for color in [True,False]:
            path=directory/f'game-{i:03d}-{int(color)}.json';data=json.loads(path.read_text());hashes[path.name]=sha(path)
            assert data['opening']==i and data['left_color']==color and len(data['moves'])==len(data['receipts'])
            board=chess.Board();pgn=chess.pgn.Game();node=pgn
            for move in opening['moves'].split():board.push_uci(move);node=node.add_variation(chess.Move.from_uci(move))
            for text,receipt in zip(data['moves'],data['receipts'],strict=True):
                move=chess.Move.from_uci(text);assert move in board.legal_moves and text==receipt['move']
                parse_receipt([f"info string request nodes={receipt['nodes']} elapsedNanos={receipt['elapsed_ns']} termination={receipt['termination']}"],plan['nodes'])
                total=totals['one' if board.turn==color else 'cp64'];total['nodes']+=receipt['nodes'];total['elapsed_ns']+=receipt['elapsed_ns'];total['requests']+=1
                board.push(move);node=node.add_variation(move)
            outcome=board.outcome(claim_draw=True);score=None if outcome is None else .5 if outcome.winner is None else int(outcome.winner==color)
            assert score==data['score'] and data['result']==board.result(claim_draw=True)
            if score is None:assert len(data['moves'])==plan['max_plies']
            pgn.headers.update(Event='R057 fixed-node ONE vs CP64',Site='Local research',Date=datetime.datetime.fromtimestamp(path.stat().st_mtime,datetime.timezone.utc).strftime('%Y.%m.%d'),Round=f'{i+1}.{int(color)}',White='ONE' if color else 'CP64',Black='CP64' if color else 'ONE',Result=data['result'])
            pgn.headers['NodeBudget']=str(plan['nodes']);pgn.headers['OpeningSource']=opening['name'];text=str(pgn)+'\n\n';target=pgn_dir/(path.stem+'.pgn')
            if target.exists():assert target.read_text()==text
            else:
                with target.open('x') as f:f.write(text)
            # Separate PGN parse/replay must reproduce all moves and the final board.
            with target.open() as stream:parsed=chess.pgn.read_game(stream)
            replay=parsed.end().board();assert not parsed.errors and replay.fen()==board.fen();hashes[str(target.relative_to(directory))]=sha(target)
            games.append(data);pair.append(score)
        if None not in pair:pairs.append(sum(pair)/2)
    scores=[g['score'] for g in games if g['score'] is not None];expected=len(openings)*2;uncertainty={}
    if pairs:
        rng=random.Random(20261029061);n=len(pairs);mean=sum(pairs)/n;boot=sorted(sum(rng.choice(pairs) for _ in range(n))/n for _ in range(20000));delta=math.sqrt(math.log(40)/(2*n))
        uncertainty=dict(complete_pairs=n,score=mean,paired_bootstrap_95=[boot[499],boot[19499]],hoeffding_95=[max(0,mean-delta),min(1,mean+delta)],bootstrap_seed=20261029061,bootstrap_samples=20000)
    for total in totals.values():total['nodes_per_second']=total['nodes']*1e9/total['elapsed_ns']
    releases=[]
    for p in directory.glob('release-*.json'):
        r=json.loads(p.read_text());assert r['remaining']==0 and not r['errors'];releases.append(dict(path=str(p.relative_to(ROOT)),sha256=sha(p)))
    assert releases
    result=dict(status='COMPLETE',plan_sha256=sha(args.plan),games=len(games),wins=scores.count(1),draws=scores.count(.5),losses=scores.count(0),censored=expected-len(scores),
                all_game_score_bounds=[sum(scores)/expected,(sum(scores)+expected-len(scores))/expected],uncertainty=uncertainty,request_totals=totals,files=hashes,releases=releases,
                confirmation_screen_pass=len(openings)==32 and len(scores)==expected and sum(scores)/expected>=.4,
                limitations='Fixed-node equal-work evidence only; common research search differs from production CP64. Related opening-family dependence can limit pairwise intervals. Development is not independent confirmation. No equal-time Elo claim.')
    with args.output.open('x') as f:f.write(json.dumps(result,indent=2)+'\n')
    print(json.dumps({k:v for k,v in result.items() if k not in ('files','releases')},indent=2))


if __name__=='__main__':main()
