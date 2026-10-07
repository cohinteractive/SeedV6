import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('match_analysis', Path(__file__).with_name('analyze-brn-matches.py'))
analysis = importlib.util.module_from_spec(spec)
spec.loader.exec_module(analysis)


def pair(index, first, second):
    def game(value):
        return {'score': value, 'termination': 'PLY_CAP' if value is None else 'fixture-terminal',
                'plies': 5, 'candidateNodes': 10, 'opponentNodes': 20, 'candidateSeconds': .1, 'opponentSeconds': .2}
    return {'index': index, 'identity': 'opening-'+str(index), 'white': game(first), 'black': game(second)}


class MatchAnalysisTests(unittest.TestCase):
    def test_opening_audit_hash_and_actual_fen_are_enforced(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            run = root/'run'
            run.mkdir()
            (root/'run.execution.json').write_text(json.dumps({'status': 'completed'}))
            (run/'config.json').write_text(json.dumps({'arguments': ['match', '', '', '', '1', '0', '25', '0', '42'],
                'candidateModelSha256': 'a', 'opponentModelSha256': 'b', 'maximumPlies': 1024,
                'protocolVersion': 'architecture-match-v2-explicit-cap'}))
            row = pair(0, 1, 0)
            row['fen'] = 'frozen-fen'
            (run/'pair-00000.json').write_text(json.dumps(row))
            audit = root/'audit.json'
            audit.write_text(json.dumps({'fresh': True, 'seed': 42, 'firstIndex': 0, 'count': 1,
                                         'openings': {'0': {'identity': 'opening-0', 'fen': 'frozen-fen'}}}))
            plan = {'firstOpeningIndex': 0, 'pairsPerInitialization': 1, 'depth': 0, 'millis': 25,
                'openingSeed': 42, 'maximumPlies': 1024, 'bootstrapReplicates': 100,
                'openingAudit': {'result': str(audit), 'sha256': analysis.digest(audit)},
                'comparisons': {'fixture': {'71': [{'run': str(run)}]}}}
            self.assertTrue(analysis.analyze(plan)['comparisons']['fixture']['complete'])
            row['fen'] = 'changed-fen'
            (run/'pair-00000.json').write_text(json.dumps(row))
            with self.assertRaisesRegex(ValueError, 'Opening differs from frozen geometry audit'):
                analysis.analyze(plan)
            audit.write_text('{}')
            with self.assertRaisesRegex(ValueError, 'Frozen opening audit changed'):
                analysis.analyze(plan)

    def test_shared_openings_are_clusters_not_independent_seed_pairs(self):
        rows = {s: {0: pair(0, 1, 1), 1: pair(1, 0, 0), 2: pair(2, .5, .5)} for s in ['71', '97']}
        result = analysis.summarize(rows, [0, 1, 2], 500)
        self.assertTrue(result['complete'])
        self.assertEqual(6, result['completePairs'])
        self.assertEqual(3, result['completeOpeningClusters'])
        self.assertEqual(.5, result['completedPairPointScore'])
        self.assertEqual([.5, .5], result['allPlannedPairsScoreBounds'])
        self.assertEqual([4, 4, 4], result['winsDrawsLossesCompletePairs'])

    def test_missing_and_capped_games_are_adverse_not_draws(self):
        rows = {'71': {0: pair(0, 1, 1), 1: pair(1, 1, None)}, '97': {0: pair(0, 0, 0)}}
        result = analysis.summarize(rows, [0, 1], 500)
        self.assertFalse(result['complete'])
        self.assertEqual(2, result['missingOrIncompletePairs'])
        self.assertEqual([.25, .75], result['allPlannedPairsScoreBounds'])
        self.assertEqual([2, 0, 2], result['winsDrawsLossesCompletePairs'])
        self.assertEqual(1, result['completeOpeningClusters'])
        with self.assertRaises(ValueError):
            analysis.score(pair(1, 2, None))

    def test_continuation_cannot_duplicate_an_opening_or_change_a_model(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            for name in ['part-a', 'part-b']:
                run = root/name
                run.mkdir()
                run.with_name(name+'.execution.json').write_text(json.dumps({'status': 'completed'}))
                (run/'config.json').write_text(json.dumps({'arguments': ['match', '', '', '', '1', '0', '25', '0', '42'],
                     'candidateModelSha256': 'a', 'opponentModelSha256': 'b', 'maximumPlies': 1024,
                     'protocolVersion': 'architecture-match-v2-explicit-cap'}))
                (run/'pair-00000.json').write_text(json.dumps(pair(0, 1, 0)))
            plan = {'firstOpeningIndex': 0, 'pairsPerInitialization': 1, 'depth': 0, 'millis': 25,
                    'openingSeed': 42, 'maximumPlies': 1024,
                    'comparisons': {'fixture': {'71': [{'run': str(root/'part-a')}, {'run': str(root/'part-b')}]}}}
            with self.assertRaisesRegex(ValueError, 'Duplicate continuation'):
                analysis.analyze(plan)
            config = json.loads((root/'part-b/config.json').read_text())
            config['candidateModelSha256'] = 'changed'
            (root/'part-b/config.json').write_text(json.dumps(config))
            with self.assertRaisesRegex(ValueError, 'Model changed'):
                analysis.analyze(plan)
            plan['comparisons']['fixture']['71'] = [{'run': str(root/'part-a')}]
            plan['modelHashes'] = {'fixture': {'71': ['a', 'b']}}
            self.assertTrue(analysis.analyze(plan)['comparisons']['fixture']['complete'])
            calibrated = json.loads((root/'part-a/config.json').read_text())
            calibrated['arguments'] += ['.25', '.5']
            calibrated['candidateGain'], calibrated['opponentGain'] = .25, .5
            (root/'part-a/config.json').write_text(json.dumps(calibrated))
            with self.assertRaisesRegex(ValueError, 'Gain differs from frozen plan'):
                analysis.analyze(plan)
            plan['gains'] = {'fixture': {'71': [.25, .5]}}
            self.assertTrue(analysis.analyze(plan)['comparisons']['fixture']['complete'])
            calibrated['candidateGain'] = .125
            (root/'part-a/config.json').write_text(json.dumps(calibrated))
            with self.assertRaisesRegex(ValueError, 'Gain arguments/config mismatch'):
                analysis.analyze(plan)
            calibrated['candidateGain'] = .25
            (root/'part-a/config.json').write_text(json.dumps(calibrated))
            plan['modelHashes']['fixture']['71'][0] = 'different-before-first-game'
            with self.assertRaisesRegex(ValueError, 'Model differs from frozen plan'):
                analysis.analyze(plan)


if __name__ == '__main__':
    unittest.main()
