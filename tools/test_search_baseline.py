import importlib.util
import math
import json
import random
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('baseline', Path(__file__).with_name('search-baseline.py'))
baseline = importlib.util.module_from_spec(spec); spec.loader.exec_module(baseline)


class PairedStatisticsTest(unittest.TestCase):
    def test_check_extension_requires_explicit_matching_research_identity(self):
        identity = 'lmp=window mode=production nominalLmpProof=off experimental=true check=forced'
        baseline.check_lmp_identity(identity, 'default', check='forced')
        for requested in ('off', 'giving'):
            with self.assertRaises(ValueError):
                baseline.check_lmp_identity(identity, 'default', check=requested)
        with self.assertRaises(ValueError):
            baseline.check_lmp_identity(identity.replace('experimental=true', ''), 'default', check='forced')
        command = baseline.command('java', 'install', 'store', 'production', 'production', 'off', 'default', 'evasion')
        self.assertIn('-Dseedv6.uci.checkResearch=evasion', command)
        self.assertFalse(any(x.startswith('-Dseedv6.uci.checkResearch=') for x in baseline.command('java', 'install', 'store')))

    def test_default_lmp_requires_real_production_and_explicit_off_keeps_the_old_control(self):
        baseline.check_lmp_identity('lmp=window mode=production nominalLmpProof=off', 'production')
        baseline.check_lmp_identity('lmp=off mode=production', 'default', workers=2)
        baseline.check_lmp_identity('lmp=off mode=control', 'production', lmr='off')
        baseline.check_lmr_identity('lmp=off lmr=quiet mode=control', 'production', 'production', 1, 'off')
        for identity in ('mode=production', 'lmp=off mode=production',
                         'lmp=window mode=control nominalLmpProof=off',
                         'lmp=window mode=production nominalLmpProof=off experimental=true',
                         'lmp=window mode=production nominalLmpProof=on'):
            with self.assertRaises(ValueError): baseline.check_lmp_identity(identity, 'production')
        cmd = baseline.command('java', 'install', 'store', 'production', 'production', 'off', 'default')
        self.assertFalse(any(x.startswith('-Dseedv6.uci.lmpResearch=') for x in cmd))


    def test_lmp_identity_cannot_claim_unreported_policy_or_nominal_proof(self):
        baseline.check_lmp_identity('mode=production', 'off')
        baseline.check_lmp_identity('lmp=guarded experimental=true nominalLmpProof=off', 'guarded')
        for identity in ('mode=production', 'lmp=shallow experimental=true nominalLmpProof=off',
                         'lmp=guarded', 'lmp=guarded experimental=true nominalLmpProof=on'):
            with self.assertRaises(ValueError): baseline.check_lmp_identity(identity, 'guarded')

    def test_fresh_selection_excludes_prior_positions_and_binds_receipts(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory); source = root/'source.pgn'
            games = []; rng = random.Random(6006)
            while len(games) < 32:
                game = baseline.chess.pgn.Game(); board = game.board(); node = game
                for _ in range(16):
                    if board.is_game_over(claim_draw=True): break
                    move = rng.choice(sorted(board.legal_moves, key=lambda m: m.uci()))
                    board.push(move); node = node.add_main_variation(move)
                if board.ply() == 16: games.append(str(game))
            source.write_text('\n\n'.join(games)+'\n', encoding='utf-8')
            first = root/'first.pgn'; second = root/'second.pgn'
            baseline.select_openings(source, first, 2, 77, 1)
            receipt = first.with_suffix('.selection.json')
            baseline.select_openings(source, second, 2, 77, 1, [receipt])
            a = json.loads(receipt.read_text())
            b = json.loads(second.with_suffix('.selection.json').read_text())
            self.assertTrue(set(a['positionKeys']).isdisjoint(b['positionKeys']))
            self.assertEqual({str(receipt.resolve()): baseline.digest(receipt)}, b['excludedSelections'])
            self.assertEqual(2, b['excludedDistinctPositions'])
            self.assertEqual(2, len(baseline.opening_prefixes(second, 2)))

    def test_lmr_identity_distinguishes_production_controls_and_frozen_legacy_binaries(self):
        for identity, policy, lmr, workers in (
                ('mode=production lmr=quiet', 'production', 'production', 1),
                ('mode=production lmr=off', 'production', 'production', 2),
                ('mode=control lmr=off', 'production', 'off', 1),
                ('mode=control lmr=off', 'unpruned', 'production', 1),
                ('mode=production', 'production', 'off', 1),
                ('mode=control', 'static', 'off', 1)):
            baseline.check_lmr_identity(identity, policy, lmr, workers)
        for identity, lmr, workers in (
                ('mode=production lmr=quiet', 'off', 1),
                ('mode=production lmr=off', 'production', 1),
                ('mode=control lmr=quiet', 'production', 1),
                ('mode=production lmr=quiet', 'production', 2),
                ('mode=production', 'quiet', 1)):
            with self.assertRaises(ValueError):
                baseline.check_lmr_identity(identity, 'production', lmr, workers)

    def test_concurrent_output_is_bound_to_prefix_and_color_without_dropping_duplicates(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory); openings = root/'openings.pgn'; games = root/'games.pgn'
            openings.write_text('[Result "*"]\n\n1. Nf3 Nf6 *\n')
            body = '\n\n1. Nf3 Nf6 2. Ng1 Ng8 3. Nf3 Nf6 4. Ng1 Ng8 1/2-1/2\n\n'
            records = ['[White "'+w+'"]\n[Black "'+b+'"]\n[Termination "normal"]\n[Result "1/2-1/2"]'+body
                       for w,b in [('candidate','baseline'),('baseline','candidate')]]
            games.write_text(''.join(records))
            with self.assertRaises(ValueError): baseline.analyze(games, openings, 1)
            self.assertEqual([0,2,0], baseline.analyze(games, openings, 1, unordered=True)['wdl'])
            games.write_text(records[0]*2)
            with self.assertRaisesRegex(ValueError, 'Duplicate concurrent'):
                baseline.analyze(games, openings, 1, unordered=True)
            games.write_text(''.join(records).replace('Nf3', 'Nc3').replace('Nf6', 'Nc6')
                             .replace('Ng1', 'Nb1').replace('Ng8', 'Nb8'))
            with self.assertRaises(ValueError): baseline.analyze(games, openings, 1, unordered=True)

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

    def test_reuse_identity_requires_explicit_matching_research_request(self):
        identity = 'lmp=window mode=production nominalLmpProof=off experimental=true reuse=pv'
        baseline.check_lmp_identity(identity,'default',reuse='pv')
        for requested in ('off','score'):
            with self.assertRaises(ValueError): baseline.check_lmp_identity(identity,'default',reuse=requested)
        with self.assertRaises(ValueError): baseline.check_lmp_identity(identity.replace('experimental=true',''),'default',reuse='pv')
        with self.assertRaises(ValueError): baseline.check_lmp_identity(identity.replace('reuse=pv','reuse=off'),'default',reuse='pv')

    def test_reuse_off_is_explicit_and_default_omits_only_its_property(self):
        off = baseline.command('java','install','store','production','production','off','default')
        self.assertIn('-Dseedv6.uci.reuseResearch=off',off)
        default = baseline.command('java','install','store','production','production','off','default','off','default')
        self.assertFalse(any('reuseResearch' in x for x in default))
        self.assertEqual(default,[x for x in off if 'reuseResearch' not in x])
        for policy in ('root','pv','score','effort','history','pvhistory'):
            selected = baseline.command('java','install','store','production','production','off','default','off',policy)
            self.assertIn('-Dseedv6.uci.reuseResearch='+policy,selected)
            self.assertEqual(default,[x for x in selected if 'reuseResearch' not in x])

    def test_default_reuse_requires_actual_effort_and_preserves_other_compositions(self):
        base='lmp=window mode=production nominalLmpProof=off reuse=effort'
        baseline.check_lmp_identity(base,'default',reuse='default')
        for invalid in (base.replace('reuse=effort','reuse=off'),base+' experimental=true',base.replace('reuse=effort','')):
            with self.assertRaises(ValueError):baseline.check_lmp_identity(invalid,'default',reuse='default')
        baseline.check_lmp_identity('lmp=off mode=production reuse=off','default',workers=2,reuse='default')
        baseline.check_lmp_identity('lmp=window mode=production nominalLmpProof=off reuse=off check=forced experimental=true',
                                    'default',check='forced',reuse='default')
        baseline.check_lmp_identity('lmp=window mode=production nominalLmpProof=off reuse=off experimental=true',
                                    'window',reuse='default')

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
