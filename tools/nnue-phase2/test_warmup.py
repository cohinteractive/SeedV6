"""Warmup is unscored fixed work with legal moves, permission boundaries and distinct games."""
import sys
from pathlib import Path
import unittest
from unittest.mock import patch
from types import SimpleNamespace
import chess
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'conventional-nnue'))
import paired_match


class Engine:
    def __init__(self):self.calls=[]
    def play(self,board,limit,game,info):
        self.calls.append((board.fen(),limit,game));return SimpleNamespace(move=next(iter(board.legal_moves)),info={'nodes':123})


class WarmupTest(unittest.TestCase):
    def test_fixed_equal_work_without_scored_game_records(self):
        engines=[Engine(),Engine()];rows=[]
        with patch.object(paired_match.compute_gate,'require') as permission:
            paired_match.warm_engines(engines,[chess.STARTING_FEN],20000,rows)
        self.assertEqual(4,permission.call_count);self.assertEqual(4,len(rows))
        self.assertEqual([0,1,0,1],[r['engine'] for r in rows])
        for engine in engines:
            self.assertEqual(2,len(engine.calls));self.assertTrue(all(c[1].nodes==20000 and c[1].time is None for c in engine.calls))
            self.assertIsNot(engine.calls[0][2],engine.calls[1][2])
        self.assertTrue(all('score' not in r for r in rows))

    def test_permission_or_illegal_move_stops_before_play(self):
        engines=[Engine(),Engine()];rows=[]
        with patch.object(paired_match.compute_gate,'require',side_effect=RuntimeError('held')):
            with self.assertRaises(RuntimeError):paired_match.warm_engines(engines,[chess.STARTING_FEN],20000,rows)
        self.assertFalse(rows);self.assertFalse(engines[0].calls)
        engines[0].play=lambda *a,**k:SimpleNamespace(move=chess.Move.null(),info={})
        with patch.object(paired_match.compute_gate,'require'):
            with self.assertRaises(RuntimeError):paired_match.warm_engines(engines,[chess.STARTING_FEN],20000,rows)


if __name__=='__main__':unittest.main()
