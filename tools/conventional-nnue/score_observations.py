"""Describe searched-score rises in already completed/audited CP64 comparison games.

Post-hoc description only: one first qualifying observation per game, no strength
selection, probability fit or model changes. Keep all games/outcomes unchanged.
"""
import argparse
import hashlib
import json
from pathlib import Path
import chess
import compute_gate

VALUES = {chess.PAWN:100, chess.KNIGHT:320, chess.BISHOP:330, chess.ROOK:500, chess.QUEEN:900}

def material(board, side):
    return sum(value * (len(board.pieces(piece, side))-len(board.pieces(piece, not side)))
               for piece, value in VALUES.items())

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--match', type=Path, action='append', required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--raw', type=Path, required=True)
    args = parser.parse_args()
    compute_gate.require()
    if args.output.exists() or args.raw.exists():
        raise ValueError('Preserve completed observations; choose fresh outputs')
    rows=[]; groups=[]
    for folder in args.match:
        protocol=json.loads((folder/'protocol.json').read_text())
        frozen='8431626c2fca81e0a1e9ccd5e45ddd8829e6e3282c756c21fad75b06d075bb2b'
        assert protocol['network_sha256']==protocol['opponent_network_sha256']==frozen
        records=[json.loads(line) for line in (folder/'games.jsonl').read_text().splitlines()]
        assert len(records)==2*len(protocol['openings'])
        selected=[]
        for record in records:
            compute_gate.require()
            stem=f"{record['opening_index']:03d}-"+('white' if record['candidate_white'] else 'black')
            details=json.loads((folder/(stem+'.json')).read_text())
            board=chess.Board()
            for move in protocol['openings'][record['opening_index']]['moves'].split():
                board.push_uci(move)
            previous={}; event=None
            for ply, info in enumerate(details['moves']):
                side=board.turn; cp=info.get('score_cp'); balance=material(board,side)
                move=chess.Move.from_uci(info['move']); assert move in board.legal_moves
                child=board.copy(); child.push(move)
                prior=previous.get(side)
                if event is None and cp is not None and cp>=500 and prior is not None:
                    old_cp, old_balance=prior
                    if cp-old_cp>=200 and balance==old_balance and material(child,side)==balance:
                        candidate=side==record['candidate_white']
                        score=record['score'] if candidate else (None if record['score'] is None else 1-record['score'])
                        event=dict(budget_ms=protocol['movetime_ms'],game=stem,ply_after_opening=ply,
                            engine=protocol['candidate_name'] if candidate else protocol['opponent_name'],
                            side='White' if side else 'Black',previous_cp=old_cp,searched_cp=cp,
                            material_balance_cp=balance,move=info['move'],fen=board.fen(),
                            formal_score_for_observed_side=score,termination=record['termination'],
                            local_game=str(folder/(stem+'.pgn')))
                previous[side]=(cp,balance) if cp is not None else None
                board=child
            outcome=board.outcome(claim_draw=True)
            expected=None if outcome is None else .5 if outcome.winner is None else float(outcome.winner==record['candidate_white'])
            assert expected==record['score']
            if event is not None: selected.append(event); rows.append(event)
        groups.append(dict(budget_ms=protocol['movetime_ms'],games=len(records),observations=len(selected),
            approximately_balanced_material=sum(abs(r['material_balance_cp'])<=100 for r in selected),
            wins=sum(r['formal_score_for_observed_side']==1 for r in selected),
            draws=sum(r['formal_score_for_observed_side']==.5 for r in selected),
            losses=sum(r['formal_score_for_observed_side']==0 for r in selected),
            censored=sum(r['formal_score_for_observed_side'] is None for r in selected),
            protocol_sha256=sha(folder/'protocol.json'),games_sha256=sha(folder/'games.jsonl')))
    args.raw.write_text(json.dumps(rows,indent=2)+'\n',encoding='utf-8',newline='\n')
    result=dict(selection='Post-hoc: first event per game where the same side has searched cp >=500 and a rise >=200 from its previous turn, with unchanged material balance across those positions and its selected move.',
        material='P=100,N=320,B=330,R=500,Q=900; diagnostic material balance only, not an evaluator',
        groups=groups,examples=rows[:4],local_evidence=str(args.raw),evidence_sha256=sha(args.raw),
        score_mapping='BT4_Q_LOGIT400_V1; cp fields are CP64 teacher-link units in UCI convention, not material centipawns',
        limits='Conditional, post-hoc, small and opening-pair-correlated observations. Search scores, not static inference. Formal outcomes do not prove probability calibration, tactical correctness or cross-engine centipawn equivalence.')
    args.output.write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8',newline='\n')
    print(json.dumps(result,indent=2))

if __name__=='__main__': main()
