"""Fixed-node baseline qualification and resumable development games. No chess clocks."""
import argparse
import ctypes
from ctypes import wintypes
import hashlib
import json
import math
import os
from pathlib import Path
import queue
import random
import re
import subprocess
import sys
import threading
import time

import chess

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools/conventional-nnue'))
import compute_gate
from compute_control import atomic


def sha(path):
    with Path(path).open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def resources():
    class Memory(ctypes.Structure):
        _fields_ = [('length', wintypes.DWORD), ('load', wintypes.DWORD)] + [
            (name, ctypes.c_ulonglong) for name in
            ('total', 'available', 'page_total', 'page_available', 'virtual_total', 'virtual_available', 'extended')]
    def cpu():
        values = [wintypes.FILETIME() for _ in range(3)]
        if not ctypes.windll.kernel32.GetSystemTimes(*(ctypes.byref(x) for x in values)):
            raise OSError('GetSystemTimes failed')
        return [(x.dwHighDateTime << 32) | x.dwLowDateTime for x in values]
    before = cpu()
    time.sleep(.25)  # One bounded measurement, never an availability-retry loop.
    after = cpu()
    delta = [b-a for a,b in zip(before, after)]
    mem = Memory(); mem.length = ctypes.sizeof(mem)
    if not ctypes.windll.kernel32.GlobalMemoryStatusEx(ctypes.byref(mem)):
        raise OSError('GlobalMemoryStatusEx failed')
    return dict(cpu_percent=100*(1-delta[0]/max(1,delta[1]+delta[2])),
                available_gib=mem.available/2**30, utc=time.strftime('%Y-%m-%dT%H:%M:%SZ',time.gmtime()))


def admit(initial=False):
    compute_gate.require()
    r = resources()
    if r['cpu_percent'] >= (60 if initial else 80) or r['available_gib'] < (8 if initial else 6):
        raise RuntimeError('RESOURCE_GATE: '+json.dumps(r))
    return r


def parse_receipt(lines, budget):
    receipts = [re.fullmatch(r'info string request nodes=(\d+) elapsedNanos=(\d+) termination=(\w+)', s)
                for s in lines if s.startswith('info string request nodes=')]
    if len(receipts) != 1 or receipts[0] is None:
        raise ValueError('Missing/ambiguous whole-request receipt')
    nodes, elapsed, termination = receipts[0].groups()
    nodes = int(nodes)
    if nodes > budget or termination not in ('NODE_LIMIT', 'COMPLETED'):
        raise ValueError('Invalid fixed-work termination: '+str((nodes, termination)))
    if termination == 'NODE_LIMIT' and nodes != budget:
        raise ValueError('Inexact node-limit receipt')
    return dict(nodes=nodes, elapsed_ns=int(elapsed), termination=termination)


class Engine:
    def __init__(self, command, log):
        self.log = log.open('x', encoding='utf-8'); self.lock = threading.Lock()
        self.p = subprocess.Popen(command, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
            stderr=subprocess.PIPE, text=True, creationflags=subprocess.CREATE_NO_WINDOW | subprocess.BELOW_NORMAL_PRIORITY_CLASS)
        self.q = queue.Queue(); self.errors = []
        def read(stream, kind):
            for line in stream:
                s = line.strip()
                with self.lock: self.log.write(kind+' '+s+'\n'); self.log.flush()
                if kind == 'out': self.q.put(s)
                else: self.errors.append(s)
        self.threads = [threading.Thread(target=read,args=(stream,kind),daemon=True)
                        for stream,kind in ((self.p.stdout,'out'),(self.p.stderr,'err'))]
        for t in self.threads: t.start()
        self.send('uci'); self.identity = self.until('uciok')
        self.send('setoption name Threads value 1'); self.send('setoption name Hash value 16')
        self.send('setoption name Ponder value false'); self.send('setoption name UseBook value false')
        self.send('isready'); self.until('readyok')
    def send(self, line):
        with self.lock: self.log.write('in '+line+'\n'); self.log.flush()
        self.p.stdin.write(line+'\n'); self.p.stdin.flush()
    def until(self, prefix):
        deadline = time.monotonic()+30; lines=[]
        while time.monotonic() < deadline:
            try: line=self.q.get(timeout=.2)
            except queue.Empty:
                compute_gate.require()
                if self.p.poll() is not None: raise RuntimeError('Engine exited: '+repr(self.errors))
                continue
            lines.append(line)
            if line.startswith(prefix): return lines
        raise RuntimeError('Administrative watchdog; unscored request: '+prefix)
    def reset(self):
        self.send('ucinewgame'); self.send('isready'); self.until('readyok')
    def search(self, board, budget):
        compute_gate.require()
        # Preserve complete move history for formal and claimable draw semantics.
        root=board.root(); moves=' '.join(m.uci() for m in board.move_stack)
        self.send('position fen '+root.fen()+(' moves '+moves if moves else ''))
        self.send('go nodes '+str(budget)); lines=self.until('bestmove ')
        receipt=parse_receipt(lines,budget); move=chess.Move.from_uci(lines[-1].split()[1])
        if move not in board.legal_moves: raise ValueError('Illegal best move')
        for line in lines:
            if line.startswith('info depth ') and ' pv ' in line:
                replay=board.copy()
                for text in line.split(' pv ',1)[1].split(): replay.push_uci(text)
        receipt.update(move=move.uci(), info=next((s for s in reversed(lines) if s.startswith('info depth ')),None))
        return move,receipt
    def close(self):
        if self.p.poll() is None:
            try: self.send('quit'); self.p.wait(timeout=5)
            except (OSError,subprocess.TimeoutExpired): self.p.kill(); self.p.wait(timeout=5)
        for t in self.threads: t.join(timeout=2)
        self.log.close()
        unexpected=[s for s in self.errors if s!='WARNING: Using incubator modules: jdk.incubator.vector']
        if unexpected: raise RuntimeError('Engine stderr: '+repr(unexpected))


def summarize(games, expected):
    scores=[g['score'] for g in games if g['score'] is not None]
    result=dict(completed_games=len(games),expected_games=expected,scored=len(scores),
        wins=scores.count(1),draws=scores.count(.5),losses=scores.count(0),
        censored=sum(g['score'] is None for g in games),
        full_allocation_score_bounds=[sum(scores)/expected,(sum(scores)+expected-len(scores))/expected])
    pairs={}
    for g in games: pairs.setdefault(g['opening'],[]).append(g['score'])
    pair_scores=[sum(v)/2 for v in pairs.values() if len(v)==2 and None not in v]
    if pair_scores:
        rng=random.Random(20261029); n=len(pair_scores); mean=sum(pair_scores)/n
        boot=sorted(sum(rng.choice(pair_scores) for _ in range(n))/n for _ in range(10000))
        result.update(complete_pairs=n,score=mean,paired_bootstrap_95=[boot[249],boot[9749]],
            hoeffding_95=[max(0,mean-math.sqrt(math.log(40)/(2*n))),min(1,mean+math.sqrt(math.log(40)/(2*n)))])
    result['scope']='reused development openings; tiny sample, no confirmation or equal-time Elo claim'
    return result


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('mode', choices=['preflight','match']); p.add_argument('plan', type=Path)
    p.add_argument('--resume',action='store_true'); a=p.parse_args()
    plan=json.loads(a.plan.read_text()); output=ROOT/plan['output']/a.mode
    resource=admit(initial=True)
    output.mkdir(parents=True,exist_ok=a.resume)
    runtime=ROOT/plan['runtime']; files={str(x.relative_to(ROOT)):sha(x) for x in sorted(runtime.rglob('*')) if x.is_file()}
    models={k:str(ROOT/v['path']) for k,v in plan['models'].items()}
    for k,v in plan['models'].items():
        if sha(models[k])!=v['sha256']: raise ValueError('Model identity: '+k)
    binding=dict(plan_sha256=sha(a.plan),runtime=files,runner_sha256=sha(Path(__file__)),python=sys.version)
    frozen=output/'binding.json'
    if frozen.exists():
        if json.loads(frozen.read_text())!=binding: raise ValueError('Frozen binding differs')
    else: atomic(frozen,binding)
    atomic(output/('resources-'+str(time.time_ns())+'.json'),resource)
    engines={}; results=[]; games=[]; status='FAILED'; error=None; started=time.monotonic()
    try:
        for key in models:
            command=['C:/java/jdk-21/bin/java.exe','--add-modules=jdk.incubator.vector',
                '-XX:ActiveProcessorCount=1','-XX:+UseSerialGC','-Xmx384m','-Djava.awt.headless=true',
                '-cp',str(runtime/'classes')+os.pathsep+str(runtime/'lib/*'),
                'com.ohinteractive.seedv6.integration.BrnRelationalUciMain',key,models[key]]
            engines[key]=Engine(command,output/(key+'-'+str(time.time_ns())+'.log'))
        atomic(output/'identity.json',{k:e.identity for k,e in engines.items()})
        if a.mode=='preflight':
            for key,e in engines.items():
                for fen in plan['probe_fens']:
                    for budget in plan['probe_nodes']:
                        observed=[]
                        for _ in range(2):
                            e.reset(); _,r=e.search(chess.Board(fen),budget); observed.append(r)
                        comparable=lambda r:(r['nodes'],r['termination'],r['move'],
                            re.sub(r' time \d+| nps \d+','',r['info'] or ''))
                        if comparable(observed[0])!=comparable(observed[1]): raise ValueError('Nondeterministic fixed-node probe')
                        results.append(dict(model=key,fen=fen,budget=budget,repeats=observed))
                admit()
            status='PASS'
        else:
            preflight=json.loads((ROOT/plan['output']/'preflight/result.json').read_text())
            if preflight['status']!='PASS': raise ValueError('Preflight not passed')
            if json.loads((ROOT/plan['output']/'preflight/binding.json').read_text())!=binding:
                raise ValueError('Preflight binding differs')
            openings=json.loads((ROOT/plan['openings']).read_text())
            if sha(ROOT/plan['openings'])!=plan['openings_sha256']: raise ValueError('Opening identity')
            for i,opening in enumerate(openings):
                for color in (chess.WHITE,chess.BLACK):
                    name=f'game-{i:03d}-{int(color)}.json'; path=output/name
                    if path.exists():
                        game=json.loads(path.read_text()); replay_game(game,opening,plan); games.append(game); continue
                    admit()
                    for e in engines.values(): e.reset()
                    board=chess.Board()
                    for move in opening['moves'].split(): board.push_uci(move)
                    moves=[]; receipts=[]
                    while not board.is_game_over(claim_draw=True) and len(moves)<plan['max_plies']:
                        if len(moves)%32==0: admit()
                        key='cp64' if board.turn==color else 'brep2'
                        move,r=engines[key].search(board,plan['nodes']); moves.append(move.uci()); receipts.append(r); board.push(move)
                    outcome=board.outcome(claim_draw=True)
                    score=None if outcome is None else .5 if outcome.winner is None else int(outcome.winner==color)
                    game=dict(opening=i,cp64_color=color,score=score,moves=moves,receipts=receipts,
                              result=board.result(claim_draw=True),reason=str(outcome.termination) if outcome else 'CAP')
                    replay_game(game,opening,plan); atomic(path,game); games.append(game)
            status='COMPLETE'
    except Exception as exc:
        error=repr(exc)
        if 'RESOURCE_GATE' in str(exc) or 'permission' in str(exc).lower(): status='RESOURCE_BLOCKED'
        raise
    finally:
        close_errors=[]
        for e in engines.values():
            try:e.close()
            except Exception as exc:close_errors.append(repr(exc))
        if close_errors: status='FAILED';error=str(close_errors)
        result=dict(status=status,error=error,elapsed_seconds=time.monotonic()-started,probes=results,
                    summary=summarize(games,plan['games']) if a.mode=='match' else None,
                    game_sha256={x.name:sha(x) for x in sorted(output.glob('game-*.json'))},
                    owned_engine_processes_remaining=sum(e.p.poll() is None for e in engines.values()))
        atomic(output/'result.json',result)
        print(json.dumps({k:v for k,v in result.items() if k not in ('probes','game_sha256')},indent=2))


def replay_game(game,opening,plan):
    board=chess.Board()
    for move in opening['moves'].split()+game['moves']: board.push_uci(move)
    if len(game['moves'])!=len(game['receipts']): raise ValueError('Move/receipt mismatch')
    for move,r in zip(game['moves'],game['receipts']):
        if move!=r['move'] or r['nodes']>plan['nodes']: raise ValueError('Game receipt mismatch')
    outcome=board.outcome(claim_draw=True)
    expected=None if outcome is None else .5 if outcome.winner is None else int(outcome.winner==game['cp64_color'])
    if expected!=game['score'] or board.result(claim_draw=True)!=game['result']: raise ValueError('Replay score mismatch')
    if outcome is None and len(game['moves'])!=plan['max_plies']: raise ValueError('Incomplete game recorded as complete')


if __name__=='__main__': main()
