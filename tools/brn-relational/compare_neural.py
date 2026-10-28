"""Fixed-node neural calibration with immutable per-game recovery; no move clocks."""
import argparse
import json
import os
import re
from pathlib import Path
import sys
import time
import subprocess
import chess
from baseline import ROOT,Engine,admit,sha,summarize,parse_receipt
from compute_control import atomic


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('plan',type=Path);a=p.parse_args()
    plan=json.loads(a.plan.read_text());runtime=ROOT/plan['runtime'];out=ROOT/plan['output'];out.mkdir(parents=True,exist_ok=True)
    binding=dict(plan_sha256=sha(a.plan),runner_sha256=sha(Path(__file__)),engine_helper_sha256=sha(ROOT/'tools/brn-relational/baseline.py'),
        runtime={str(f.relative_to(runtime)):sha(f) for f in runtime.rglob('*') if f.is_file()})
    if (out/'binding.json').exists():
        if json.loads((out/'binding.json').read_text())!=binding:raise ValueError('Frozen match binding changed')
    else:atomic(out/'binding.json',binding)
    if sha(ROOT/plan['openings'])!=plan['openings_sha256']:raise ValueError('Openings changed')
    if sha(ROOT/plan['validation'])!=plan['validation_sha256']:raise ValueError('Prediction panel changed')
    for comparison in plan['comparisons']:
        for side in ('left','right'):
            spec=comparison[side]
            if spec.get('evaluator','pair') not in ('pair','cp64','compose8'):raise ValueError('Unknown evaluator')
            if sha(ROOT/spec['path'])!=spec['sha256']:raise ValueError('Model changed')
    if sha(ROOT/plan['prediction_evidence'])!=plan['prediction_evidence_sha256']:raise ValueError('Prediction evidence changed')
    openings=json.loads((ROOT/plan['openings']).read_text());summaries=[]
    for comparison in plan['comparisons']:
        directory=out/comparison['id'];directory.mkdir(exist_ok=True);engines={};games=[];start=time.monotonic();samples=[];probes=[]
        if all((directory/f'game-{i:03d}-{color}.json').exists() for i in range(len(openings)) for color in (1,0)):
            for i,opening in enumerate(openings):
                for color in (1,0):
                    game=json.loads((directory/f'game-{i:03d}-{color}.json').read_text());replay(game,opening,plan);games.append(game)
            summaries.append(dict(id=comparison['id'],summary=summarize(games,len(openings)*2),verified_complete=True,games={f.name:sha(f) for f in directory.glob('game-*.json')}));continue
        resource=admit(initial=True);samples.append(resource)
        try:
            for side in ('left','right'):
                spec=comparison[side];model=ROOT/spec['path']
                if sha(model)!=spec['sha256']:raise ValueError('Model changed')
                command=['C:/java/jdk-21/bin/java.exe','--add-modules=jdk.incubator.vector','-XX:ActiveProcessorCount=1',
                    '-XX:+UseSerialGC','-Xmx384m','-cp',str(runtime/'classes')+os.pathsep+str(runtime/'lib/*'),
                    'com.ohinteractive.seedv6.integration.BrnRelationalUciMain',spec.get('evaluator','pair'),str(model)]
                engines[side]=Engine(command,directory/(side+'-'+str(time.time_ns())+'.log'))
                if not any(spec['sha256'] in line and 'profile=common' in line for line in engines[side].identity):raise ValueError('UCI model/profile identity')
                # Actual model startup + special-position legal search before its games.
                for fen in plan['probe_fens']:
                    observed=[]
                    for _ in range(2):
                        engines[side].reset();_,r=engines[side].search(chess.Board(fen),plan['nodes']);observed.append(r)
                    comparable=lambda r:(r['nodes'],r['termination'],r['move'],re.sub(r' time \d+| nps \d+','',r['info'] or ''))
                    if comparable(observed[0])!=comparable(observed[1]):raise ValueError('Nondeterministic fixed-node probe')
                    probes.append(dict(side=side,fen=fen,budget=plan['nodes'],repeats=observed))
            atomic(directory/('probes-'+str(time.time_ns())+'.json'),probes)
            for i,opening in enumerate(openings):
                for color in (chess.WHITE,chess.BLACK):
                    path=directory/f'game-{i:03d}-{int(color)}.json'
                    if path.exists():
                        game=json.loads(path.read_text());replay(game,opening,plan);games.append(game);continue
                    samples.append(admit())
                    for e in engines.values():e.reset()
                    board=chess.Board()
                    for text in opening['moves'].split():board.push_uci(text)
                    moves=[];receipts=[]
                    while not board.is_game_over(claim_draw=True) and len(moves)<plan['max_plies']:
                        if len(moves)%32==0:samples.append(admit())
                        side='left' if board.turn==color else 'right'
                        move,r=engines[side].search(board,plan['nodes']);moves.append(move.uci());receipts.append(r);board.push(move)
                    outcome=board.outcome(claim_draw=True);score=None if outcome is None else .5 if outcome.winner is None else int(outcome.winner==color)
                    game=dict(opening=i,left_color=color,score=score,moves=moves,receipts=receipts,result=board.result(claim_draw=True))
                    replay(game,opening,plan);atomic(path,game);games.append(game)
            summary=summarize(games,len(openings)*2);summary['scope']='Neural fixed-node calibration;left-arm score;reused openings, no promotion,final or equal-time Elo claim'
            summaries.append(dict(id=comparison['id'],summary=summary,elapsed_seconds=time.monotonic()-start,resources=samples,
                games={f.name:sha(f) for f in directory.glob('game-*.json')}))
            atomic(out/'results.json',summaries)
        finally:
            errors=[]
            for e in engines.values():
                try:e.close()
                except Exception as exc:errors.append(repr(exc))
            atomic(directory/('release-'+str(time.time_ns())+'.json'),dict(remaining=sum(e.p.poll() is None for e in engines.values()),errors=errors,resources=samples,interruption=repr(sys.exc_info()[1]) if sys.exc_info()[1] else None))
            if errors:raise RuntimeError('Engine release errors '+repr(errors))
    atomic(out/'results.json',summaries)
    print(json.dumps(summaries,indent=2))


def replay(game,opening,plan):
    board=chess.Board()
    for text in opening['moves'].split()+game['moves']:board.push_uci(text)
    if len(game['moves'])!=len(game['receipts']):raise ValueError('Move/receipt mismatch')
    for move,r in zip(game['moves'],game['receipts']):
        if move!=r['move']:raise ValueError('Receipt move mismatch')
        parse_receipt([f"info string request nodes={r['nodes']} elapsedNanos={r['elapsed_ns']} termination={r['termination']}"],plan['nodes'])
    outcome=board.outcome(claim_draw=True);score=None if outcome is None else .5 if outcome.winner is None else int(outcome.winner==game['left_color'])
    if score!=game['score'] or board.result(claim_draw=True)!=game['result']:raise ValueError('Game replay mismatch')
    if score is None and len(game['moves'])!=plan['max_plies']:raise ValueError('Incomplete game cannot be a cap')


if __name__=='__main__':main()
