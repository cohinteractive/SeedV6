"""Match budget and final-only bootstrap contracts, using no real engines."""
import contextlib
import io
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch,MagicMock
import paired_match as match


class StrengthMatch(unittest.TestCase):
    def test_interim_summary_never_runs_bootstrap_but_final_preserves_pairs(self):
        games=[dict(score=s,opening_index=i//2) for i,s in enumerate([1,0,.5,.5,1,0,0,1])]
        interim=match.summarize(games,bootstrap=False);final=match.summarize(games)
        self.assertNotIn('paired_bootstrap_95_elo',interim)
        for key in ('wins','draws','losses','completed','censored'):self.assertEqual(interim[key],final[key])
        self.assertEqual(final['pairs'],4);self.assertEqual(final['score'],.5)

    def test_budget_stops_before_play_and_exhausted_resume_launches_nothing(self):
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder);openings=root/'openings.json';network=root/'network.cnnue';out=root/'match'
            openings.write_text(json.dumps([dict(name='fixture',moves='')]))
            network.write_bytes(b'fixture; not loaded by mocked engines')
            args=['match','--network',str(network),'--opponent-network',str(network),'--openings',str(openings),
                  '--output',str(out),'--max-wall-seconds','60']
            engine=MagicMock()
            with patch('sys.argv',args),patch.object(match.compute_gate,'require'),patch.object(match.compute_gate,'permitted',return_value=True),\
                 patch.object(match.chess.engine.SimpleEngine,'popen_uci',return_value=engine),\
                 patch.object(match.time,'monotonic',side_effect=[0,0,61,61]),contextlib.redirect_stdout(io.StringIO()):
                with self.assertRaises(SystemExit) as stopped:match.main()
                self.assertEqual(stopped.exception.code,76)
            engine.play.assert_not_called();self.assertFalse((out/'games.jsonl').exists())
            interruptions=list(out.glob('interrupted-*.json'));self.assertEqual(len(interruptions),1)
            record=json.loads(interruptions[0].read_text());self.assertEqual(record['reason'],'wall-budget')
            self.assertEqual(record['elapsed_seconds'],61)
            with patch('sys.argv',args+['--resume']),patch.object(match.compute_gate,'require'),\
                 patch.object(match.chess.engine.SimpleEngine,'popen_uci') as launch,contextlib.redirect_stdout(io.StringIO()):
                with self.assertRaises(SystemExit) as stopped:match.main()
                self.assertEqual(stopped.exception.code,76);launch.assert_not_called()
            self.assertEqual(len(list(out.glob('interrupted-*.json'))),1)


if __name__=='__main__':unittest.main()
