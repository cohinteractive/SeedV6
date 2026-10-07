"""Independent color accounting and corruption checks for the match resource audit."""
import copy
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest


SPEC = importlib.util.spec_from_file_location('match_resources',
    Path(__file__).with_name('audit-brn-match-resources.py'))
AUDIT = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(AUDIT)


class MatchResourceAuditTest(unittest.TestCase):
    def test_odd_game_counts_respect_starting_side_and_candidate_color(self):
        # Five plies from a black-to-move FEN are B,W,B,W,B.
        self.assertEqual({'candidate': 2, 'opponent': 3}, AUDIT.actor_moves(5, 'b', 'w'))
        self.assertEqual({'candidate': 3, 'opponent': 2}, AUDIT.actor_moves(5, 'b', 'b'))
        self.assertEqual({'candidate': 3, 'opponent': 2}, AUDIT.actor_moves(5, 'w', 'w'))
        self.assertEqual({'candidate': 0, 'opponent': 0}, AUDIT.actor_moves(0, 'b', 'w'))

    @staticmethod
    def write(path, value):
        path.write_text(json.dumps(value), encoding='utf-8')

    def fixture(self, root):
        run = root / 'match'
        run.mkdir()
        args = ['candidate', 'opponent', str(run), '1', '0', '50', '900', '840181', '.25', '.25']
        plan = {'schema': 'brn-architecture-match-plan-v1', 'timeoutSeconds': 900,
            'depth': 0, 'millis': 50, 'plannedGames': 2,
            'comparisons': {'a-vs-b': {'211': [{'run': str(run)}]}},
            'runs': [{'run': str(run), 'comparison': 'a-vs-b', 'seed': 211,
                      'mode': 'match', 'arguments': args}]}
        execution = {'status': 'completed', 'exitCode': 0, 'command': ['java', 'match'] + args,
            'timeoutSeconds': 900, 'sourceSha256': {'source': 'a'},
            'compiledSha256': {'class': 'b'}, 'runnerSha256': {'runner': 'c'},
            'java': '21', 'platform': 'test', 'elapsedSeconds': 1,
            'processMemory': {'peakWorkingSetBytes': 1234}}
        self.write(Path(str(run) + '.execution.json'), execution)
        self.write(run / 'config.json', {'arguments': ['match'] + args})
        game = {'plies': 5, 'moves': ['1', '2', '3', '4', '5'],
            'candidateNodes': 20, 'opponentNodes': 30,
            'candidateSeconds': .1, 'opponentSeconds': .15, 'termination': 'REPETITION'}
        black = copy.deepcopy(game)
        black.update(candidateNodes=30, opponentNodes=20, candidateSeconds=.15, opponentSeconds=.1)
        pair = {'index': 900, 'fen': '8/8/8/8/8/8/8/8 b - - 0 1',
                'white': game, 'black': black}
        self.write(run / 'pair-00900.json', pair)
        return plan, run, execution, pair

    def test_actual_timing_and_failure_denominator_are_separate(self):
        with tempfile.TemporaryDirectory() as directory:
            plan, run, execution, pair = self.fixture(Path(directory))
            result = AUDIT.analyze(plan)
            group = result['groups']['a-vs-b/211']
            for actor in group['actors'].values():
                self.assertEqual(5, actor['movesInGamesWithUnambiguousMoveAccounting'])
                self.assertAlmostEqual(50, actor['millisecondsPerPlayedMove'])
            self.assertAlmostEqual(.5, result['provenance'][0]['otherProcessSeconds'])
            pair['black']['failure'] = 'Unplayed failed search attempt'
            pair['black']['candidateSeconds'] += .04
            self.write(run / 'pair-00900.json', pair)
            group = AUDIT.analyze(plan)['groups']['a-vs-b/211']
            self.assertEqual(1, len(group['ambiguousGames']))
            self.assertAlmostEqual(.29, group['actors']['candidate']['secondsAllRecordedGames'])
            self.assertEqual(2, group['actors']['candidate']['movesInGamesWithUnambiguousMoveAccounting'])
            self.assertAlmostEqual(50, group['actors']['candidate']['millisecondsPerPlayedMove'])

    def test_changed_command_or_impossible_total_time_is_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            plan, run, execution, pair = self.fixture(Path(directory))
            execution['command'][-1] = '.5'
            receipt = Path(str(run) + '.execution.json')
            self.write(receipt, execution)
            with self.assertRaisesRegex(ValueError, 'Executed arguments'):
                AUDIT.analyze(plan)
            execution['command'][-1] = '.25'
            execution['elapsedSeconds'] = .1
            self.write(receipt, execution)
            with self.assertRaisesRegex(ValueError, 'exceeds whole process'):
                AUDIT.analyze(plan)

    def test_missing_pair_is_preserved_and_running_receipt_is_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            plan, run, execution, pair = self.fixture(Path(directory))
            # Create no replacement outcome; the planned second index must remain missing.
            plan['plannedGames'] = 4
            plan['runs'][0]['arguments'][3] = '2'
            execution['command'][-7] = '2'
            self.write(Path(str(run) + '.execution.json'), execution)
            self.write(run / 'config.json', {'arguments': ['match'] + plan['runs'][0]['arguments']})
            result = AUDIT.analyze(plan)
            self.assertFalse(result['allPairFilesPresent'])
            self.assertEqual([901], result['groups']['a-vs-b/211']['missingPairIndices'])
            execution['status'] = 'running'
            self.write(Path(str(run) + '.execution.json'), execution)
            with self.assertRaisesRegex(ValueError, 'still active'):
                AUDIT.analyze(plan)


if __name__ == '__main__':
    unittest.main()
