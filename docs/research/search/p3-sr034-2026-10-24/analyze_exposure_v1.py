"""Exploratory allocation exposure from already completed, censored trajectories.

This neither selects a whole-game policy nor treats a later move as better.
Repeated roots and normalized hypothetical clocks are dependent observations.
"""
from collections import defaultdict
import json
from pathlib import Path
from run_trajectory_v1 import HERE, digest, write_new


def main():
    sources = ('trajectory-openings-v1', 'trajectory-played-v1')
    inputs = {}; witnesses = []; groups = defaultdict(list)
    for name in sources:
        path = HERE / 'raw' / (name + '.jsonl')
        inputs[str(path.relative_to(HERE))] = digest(path)
        rows = [json.loads(line) for line in path.read_text().splitlines()]
        assert rows[-1]['status'] == 'passed'
        for row in rows:
            if row.get('type') != 'trajectory':
                continue
            seq = row['observed']['iterations']
            results = [s['iteration']['result'] for s in seq]
            for i in range(2, len(seq) - 1):
                r, following = results[i:i+2]
                if r['depth'] < 6 or any(abs(x['score']) >= 32512 for x in results[i-2:i+2]):
                    continue
                stable = len({x['bestMove'] for x in results[i-2:i+1]}) == 1
                if stable and r['bestMove'] != following['bestMove']:
                    current = seq[i]['observedNanos']; later = seq[i+1]['observedNanos']
                    witnesses.append({'source': name, 'root': row['id'], 'round': row['round'],
                        'depth': r['depth'], 'move': r['bestMove'], 'nextMove': following['bestMove'],
                        'score': r['score'], 'nextScore': following['score'],
                        'boundaryElapsedNanos': current, 'nextBoundaryElapsedNanos': later,
                        'hypotheticalHardBudgetExposureNanos': max(0, 2 * current - later),
                        'exposureMeaning': 'A half-budget stop here can miss the next changed completed move only when nextBoundary < hardBudget <= 2*boundary; earlier stops may preempt this boundary.'})
            for milliseconds in (50, 100, 200, 400):
                budget = milliseconds * 1_000_000
                if not seq or seq[-1]['observedNanos'] < 2 * budget:
                    continue  # Require observed coverage, never infer uncaptured work.
                base = [i for i, x in enumerate(seq) if x['observedNanos'] < budget]
                extra = [i for i, x in enumerate(seq) if x['observedNanos'] < 2 * budget]
                if not base or base[-1] < 2:
                    continue
                i = base[-1]; j = extra[-1]; r = results[i]
                if r['depth'] < 6 or any(abs(x['score']) >= 32512 for x in results[i-2:j+1]):
                    continue
                conditions = {'all': True,
                    'latestMoveChanged': r['bestMove'] != results[i-1]['bestMove'],
                    'stable3': len({x['bestMove'] for x in results[i-2:i+1]}) == 1,
                    'latestScoreFell': r['score'] < results[i-1]['score']}
                for condition, include in conditions.items():
                    if include:
                        groups[(milliseconds, condition)].append({
                            'depthGain': results[j]['depth'] - r['depth'],
                            'changed': results[j]['bestMove'] != r['bestMove']})
    result = {'status': 'passed', 'inputs': inputs, 'analysisScriptSha256': digest(Path(__file__)),
        'stableThenChangedWitnesses': witnesses,
        'doubleBudgetObservations': [dict(budgetMillis=key[0], condition=key[1], n=len(rows),
            changedMoves=sum(r['changed'] for r in rows),
            gainedCompletedIterations=sum(r['depthGain'] for r in rows)) for key, rows in sorted(groups.items())],
        'limitations': ['Exploratory post hoc mechanism description; no confirmation evidence.',
            'The played-root wall-time run failed the historical one-other-core screen; no qualified speed or calibrated threshold claim.',
            'Observation overhead, JIT and host effects remain; repeated rounds and clock budgets are dependent.',
            'A later selective move is not a demonstrated better move.',
            'Node-limited coverage excludes the expensive tail; this is not a policy simulator or a whole-game cost estimate.']}
    write_new(HERE / 'trajectory-exposure-v1-summary.json', result)
    print(json.dumps({'witnesses': len(witnesses),
        'positiveExposureWitnesses': sum(x['hypotheticalHardBudgetExposureNanos'] > 0 for x in witnesses),
        'doubleBudgetObservations': result['doubleBudgetObservations']}, indent=2))


if __name__ == '__main__':
    main()
