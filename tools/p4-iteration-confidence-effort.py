"""Descriptive replay of final-pass versus all-pass root effort; no Search run.

Ranks are hypothetical and cannot predict a changed concurrent Search trajectory.
The resolved favorite stays first; ties preserve the final pass's input order.
"""
import argparse
from collections import Counter
import hashlib
import itertools
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'docs/research/search/p4-iteration-confidence'


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def replay(name):
    path = OUT / 'raw' / name / 'observations.jsonl'
    receipt = json.loads((OUT / (name + '-execution.json')).read_text())
    assert receipt['status'] == 'passed' and receipt['inputsStable']
    assert receipt['files'][str(path.relative_to(ROOT))] == digest(path)
    counts = Counter()
    for row in map(json.loads, path.read_text().splitlines()):
        if row['type'] != 'request':
            continue
        prior = None
        for depth, group in itertools.groupby(row['attempts'], key=lambda a: a['depth']):
            attempts = list(group)
            final = attempts[-1]
            if final['classification'] != 'resolved-selective' or not final['rootEffort']['split']:
                prior = None
                continue
            assert all(a['rootEffort']['split'] and a['result']['completed'] for a in attempts)
            assert all(a['classification'] in ('upper', 'lower') for a in attempts[:-1])
            favorite = final['result']['bestMove']
            last = {r['move']: r['nodes'] for r in final['rootEffort']['currentPass']}
            total = Counter()
            for attempt in attempts:
                effort = {r['move']: r['nodes'] for r in attempt['rootEffort']['currentPass']}
                assert set(effort) == set(last) and sum(effort.values()) == attempt['result']['nodes']
                total.update(effort)
            alternatives = [move for move in last if move != favorite]
            final_order = sorted(alternatives, key=lambda move: -last[move])
            all_order = sorted(alternatives, key=lambda move: -total[move])
            counts['resolvedSplitIterations'] += 1
            if len(attempts) > 1:
                counts['withEarlierBoundPasses'] += 1
                counts['earlierBoundAdmissions'] += sum(total.values()) - sum(last.values())
                counts['changedTopAlternative'] += final_order[:1] != all_order[:1]
                counts['changedTopThreeSet'] += set(final_order[:3]) != set(all_order[:3])
                counts['changedAlternativeOrder'] += final_order != all_order
            else:
                assert final_order == all_order
            if prior and prior[0] == depth - 1 and favorite != prior[1]:
                counts['nextCompletedFavoriteChanges'] += 1
                old_rank, new_rank = prior[2].index(favorite), prior[3].index(favorite)
                counts['nextFavoriteRankEarlier' if new_rank < old_rank else
                       'nextFavoriteRankLater' if new_rank > old_rank else 'nextFavoriteRankSame'] += 1
            prior = depth, favorite, final_order, all_order
    return dict(run=name, sourceSha256=digest(path), counts=dict(counts))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', required=True)
    args = parser.parse_args()
    result = dict(status='descriptive replay of existing fixed-work evidence; no experiment',
                  analyzerSha256=digest(Path(__file__)),
                  runs=[replay(n) for n in ('baseline-mixed-w4-v1', 'c1-mixed-w4-v1',
                                           'c1-mixed-w4-v2', 'baseline-mixed-w4-v2')],
                  semantics=__doc__.strip(),
                  limitations='Next-completed favorite is a retrospective selective observation, not truth. '
                              'Incomplete iterations are excluded; shared TT, order and worker correlation remain. '
                              'A changed ranking neither proves inadequate challenge nor predicts strength.')
    with Path(args.output).open('x', encoding='utf-8', newline='\n') as stream:
        json.dump(result, stream, indent=2)
        stream.write('\n')
    print(json.dumps(result))
