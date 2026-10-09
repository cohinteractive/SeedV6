import importlib.util
import math
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('baseline', Path(__file__).with_name('search-baseline.py'))
baseline = importlib.util.module_from_spec(spec); spec.loader.exec_module(baseline)


class PairedStatisticsTest(unittest.TestCase):
    def test_static_control_is_explicit_after_production_default_changes(self):
        for policy in ('static', 'production', 'unpruned'):
            command = baseline.command('java', 'installation', 'store', policy)
            self.assertIn('-Dseedv6.uci.qsearchResearch=' + policy, command)

    def test_pentanomial_draws_and_color_reversal(self):
        result = baseline.paired_statistics([0, .5, 1, 1.5, 2])
        self.assertEqual([1]*5, result['pentanomial'])
        self.assertEqual(.5, result['score'])
        self.assertEqual(0, result['logisticElo'])
        self.assertEqual(baseline.paired_statistics([2, 1.5, 1, .5, 0]), result)

    def test_all_draws_does_not_claim_zero_uncertainty(self):
        result = baseline.paired_statistics([1]*100)
        self.assertLess(result['pairedHoeffding95Score'][0], .5)
        self.assertGreater(result['pairedHoeffding95Score'][1], .5)
        self.assertIsNone(result['pairedNormal95Score'])
        self.assertEqual('-infinity', baseline.paired_statistics([0])['logisticElo'])
        self.assertEqual('+infinity', baseline.paired_statistics([2])['logisticElo'])

    def test_known_elo_and_pair_sample_size(self):
        self.assertAlmostEqual(400, baseline.elo(10/11))
        result = baseline.paired_statistics([1.5]*1000)
        self.assertAlmostEqual(.75-math.sqrt(math.log(40)/2000), result['pairedHoeffding95Score'][0])
        with self.assertRaises(ValueError): baseline.paired_statistics([])
        with self.assertRaises(ValueError): baseline.paired_statistics([None])

    def test_pair_variance_keeps_within_opening_correlation(self):
        # Same aggregate W/L, radically different pairing: WL pairs all score .5;
        # WW/LL pairs have uncertainty. A game-binomial formula misses this.
        clustered = baseline.paired_statistics([0, 2]*50)
        self.assertAlmostEqual(math.sqrt((25/99)/100), clustered['pairScoreStandardError'])
        self.assertIsNotNone(clustered['pairedNormal95Score'])
        self.assertIsNone(baseline.paired_statistics([1]*100)['pairedNormal95Score'])

    def test_incomplete_and_color_mismatched_games_cannot_pass(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory); openings = root/'openings.pgn'; games = root/'games.pgn'
            openings.write_text('[Result "*"]\n\n1. e4 e5 *\n')
            games.write_text('[White "baseline"]\n[Black "candidate"]\n[Result "1-0"]\n\n1. e4 e5 1-0\n')
            with self.assertRaises(ValueError): baseline.analyze(games, openings, 1)
            games.write_text(games.read_text()+'\n'+games.read_text())
            with self.assertRaises(ValueError): baseline.analyze(games, openings, 1)

    def test_complete_color_reversed_draw_pair_is_half_score(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory); openings = root/'openings.pgn'; games = root/'games.pgn'
            openings.write_text('[Result "*"]\n\n1. Nf3 Nf6 *\n')
            body = '\n\n1. Nf3 Nf6 2. Ng1 Ng8 3. Nf3 Nf6 4. Ng1 Ng8 1/2-1/2\n\n'
            games.write_text(''.join('[White "'+white+'"]\n[Black "'+black+'"]\n[Termination "normal"]\n[Result "1/2-1/2"]'+body
                                    for white, black in [('baseline', 'candidate'), ('candidate', 'baseline')]))
            result = baseline.analyze(games, openings, 1)
            self.assertEqual([0, 2, 0], result['wdl'])
            self.assertEqual([0, 0, 1, 0, 0], result['statistics']['pentanomial'])
            tagged = games.read_text().replace('Ng1', 'Ng1 {info string request nodes=10 elapsedNanos=1000 termination=TIME_ALLOCATION}')
            games.write_text(tagged)
            result = baseline.analyze(games, openings, 1)
            self.assertEqual(4, result['wholeRequestResources']['baseline']['requests'] + result['wholeRequestResources']['candidate']['requests'])
            games.write_text(games.read_text()+'\n'+games.read_text())
            with self.assertRaises(ValueError): baseline.analyze(games, openings, 1)


if __name__ == '__main__': unittest.main()
