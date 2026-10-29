"""Audit scoreless fallback requests from the pinned Fastchess full UCI trace.

This does not excuse arbitrary warnings or fabricate a score. Fastchess continues
to check every reported PV; the paired-game audit independently checks outcomes.
Only normal game-clock TIME_LIMIT/TIME_ALLOCATION fallbacks are admitted here.
"""
import re
from pathlib import Path

import chess

ENGINE = re.compile(r'^\[Engine\] .*?\s(baseline|candidate)\s+(<---|--->) (.*)$')
MISSING = re.compile(r'^Warning; No info line available to extract score from engine (baseline|candidate)$')
TELEMETRY = re.compile(r'^info string request nodes=(\d+) elapsedNanos=(\d+) termination=(\w+)$')


def missing_actor(line):
    match = MISSING.fullmatch(line.strip())
    return match[1] if match else None


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
    if words[0] != 'go' or len(words) % 2 != 1:
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


def audit(path):
    states = {actor:{'position':None, 'request':None, 'outputs':[], 'last':None}
              for actor in ('baseline', 'candidate')}
    warnings = []
    for number, line in enumerate(Path(path).open(encoding='utf-8'), 1):
        line = line.rstrip('\r\n')
        if '[WARN ' in line or '[ERROR' in line or '[FATAL' in line:
            text = line.split(' --- ', 1)[-1]
            actor = missing_actor(text)
            if actor is None:
                raise ValueError('Unapproved runner warning/error: '+text)
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
                state['request'] = None; state['outputs'] = []
    if any(s['request'] is not None for s in states.values()):
        raise ValueError('Undrained request at end of trace')
    return {'status':'passed', 'qualifiedMissingScoreWarnings':len(warnings), 'fallbacks':warnings,
            'limits':'Validates the narrowly admitted missing-score warnings; does not replace full PV checks, game audit or process/input qualification.'}
