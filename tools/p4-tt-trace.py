"""Audit scoreless fallback requests from the pinned Fastchess full UCI trace.

This does not excuse arbitrary warnings or fabricate a score. Fastchess continues
to check every reported PV; the paired-game audit independently checks outcomes.
Only normal game-clock TIME_LIMIT/TIME_ALLOCATION fallbacks are admitted here.
"""
import re
import subprocess
import time
from pathlib import Path

import chess

ENGINE = re.compile(r'^\[Engine\] .*?\s(baseline|candidate)\s+(<---|--->) (.*)$')
MISSING = re.compile(r'^Warning; No info line available to extract score from engine (baseline|candidate)$')
TELEMETRY = re.compile(r'^info string request nodes=(\d+) elapsedNanos=(\d+) termination=(\w+)$')
SEVERITY = re.compile(r'^\[(WARN|ERR|ERROR|FATAL)\s*\]')


def missing_actor(line):
    match = MISSING.fullmatch(line.strip())
    return match[1] if match else None


def warning(line):
    if not SEVERITY.match(line):
        return None
    text = line.split(' --- ', 1)[-1].strip()
    actor = missing_actor(text)
    if actor is None or not line.startswith('[WARN'):
        raise ValueError('Unapproved runner warning/error: '+text)
    return actor


def supervise(process, trace, timeout, observed):
    """Retain the runner's checks; only defer the known warning for full audit.

    Poll the append-only trace, without parsing Search or changing either engine.
    All other warning/error severities stop the owned match. A deferred warning
    is not accepted evidence until audit() validates its exact request.
    """
    deadline = time.monotonic()+timeout
    stream = None; pending = ''
    def scan(final=False):
        nonlocal stream, pending
        if stream is None and Path(trace).exists():
            stream = Path(trace).open(encoding='utf-8')
        if stream is None:
            if final: raise ValueError('Missing full UCI trace')
            return
        while chunk := stream.read(1024*1024):
            lines = (pending+chunk).split('\n'); pending = lines.pop()
            for line in lines:
                actor = warning(line.rstrip('\r'))
                if actor is not None: observed.append(actor)
        if final and pending:
            actor = warning(pending.rstrip('\r'))
            if actor is not None: observed.append(actor)
            pending = ''
    try:
        while True:
            scan()
            remaining = deadline-time.monotonic()
            if remaining <= 0: raise TimeoutError('Owned match timeout')
            try:
                code = process.wait(timeout=min(.25, remaining))
            except subprocess.TimeoutExpired:
                continue
            scan(final=True)
            return code
    finally:
        if stream is not None: stream.close()


def position(text):
    words = text.split()
    if words[:2] != ['position', 'startpos']:
        raise ValueError('Expected full real startpos history')
    board = chess.Board()
    if len(words) > 2:
        if words[2] != 'moves':
            raise ValueError('Malformed position')
        for move in words[3:]:
            board.push_uci(move)
    return board


def fallback(board, request, outputs):
    if board.is_game_over(claim_draw=True):
        raise ValueError('Fallback at terminal position')
    words = request.split()
    if len(words) != 9 or words[0] != 'go':
        raise ValueError('Expected game-clock request')
    options = dict(zip(words[1::2], words[2::2]))
    if set(options) != {'wtime', 'btime', 'winc', 'binc'} or any(int(v) < 0 for v in options.values()):
        raise ValueError('Unexpected game-clock options')
    if any(x.startswith('info ') and not x.startswith('info string ') for x in outputs):
        raise ValueError('Missing-score warning despite Search info')
    if any('fail' in x.lower() or 'error' in x.lower() for x in outputs):
        raise ValueError('Engine reported failure')
    metrics = [TELEMETRY.fullmatch(x) for x in outputs if x.startswith('info string request ')]
    if len(metrics) != 1 or metrics[0] is None:
        raise ValueError('Missing/malformed whole-request accounting')
    metric = metrics[0]
    if metric[3] not in ('TIME_LIMIT', 'TIME_ALLOCATION'):
        raise ValueError('Scoreless fallback without clock termination')
    best = [x for x in outputs if x.startswith('bestmove ')]
    if len(best) != 1 or len(best[0].split()) != 2 or best[0] != outputs[-1]:
        raise ValueError('Missing/ambiguous bestmove')
    move = best[0].split()[1]
    if chess.Move.from_uci(move) not in board.legal_moves:
        raise ValueError('Illegal fallback')
    return {'position':board.fen(), 'request':request, 'nodes':int(metric[1]),
            'elapsedNanos':int(metric[2]), 'termination':metric[3], 'bestmove':move,
            'completedDepthOrScoreReported':False}


def audit_lines(lines):
    states = {actor:{'position':None, 'request':None, 'outputs':[], 'last':None}
              for actor in ('baseline', 'candidate')}
    warnings = []; scoreless = []; completed = 0
    for number, line in enumerate(lines, 1):
        line = line.rstrip('\r\n')
        actor = warning(line)
        if actor is not None:
            item = states[actor]['last']
            if item is None or item.get('warningMatched'):
                raise ValueError('Warning without unique completed request')
            result = fallback(position(item['position']), item['request'], item['outputs'])
            item['warningMatched'] = True
            warnings.append(dict(actor=actor, traceLine=number, positionCommand=item['position'], **result))
            continue
        match = ENGINE.match(line)
        if not match:
            continue
        actor, direction, text = match.groups(); state = states[actor]
        if direction == '<---':
            if text.startswith('position '):
                state['position'] = text
            elif text.startswith('go '):
                if state['request'] is not None or state['position'] is None:
                    raise ValueError('Overlapping request or missing real history')
                state['request'] = text; state['outputs'] = []
        elif state['request'] is not None:
            state['outputs'].append(text)
            if text.startswith('bestmove '):
                state['last'] = {key:state[key] for key in ('position', 'request', 'outputs')}
                completed += 1
                if not any(x.startswith('info ') and not x.startswith('info string ') for x in state['outputs']):
                    scoreless.append(state['last'])
                state['request'] = None; state['outputs'] = []
    if any(s['request'] is not None for s in states.values()):
        raise ValueError('Undrained request at end of trace')
    if completed == 0 or any(not x.get('warningMatched') for x in scoreless):
        raise ValueError('Missing trace requests or unaccounted scoreless response')
    return {'status':'passed', 'completedRequests':completed,
            'qualifiedMissingScoreWarnings':len(warnings), 'fallbacks':warnings,
            'limits':'Validates the narrowly admitted missing-score warnings; does not replace full PV checks, game audit or process/input qualification.'}


def audit(path):
    with Path(path).open(encoding="utf-8") as stream:
        return audit_lines(stream)
