"""Read-only challenger-marker replay on published, resolved C4 split passes.
An original-window scout fail-high requiring full retry is an event, not confidence.
"""
import argparse
from collections import Counter
import hashlib
import itertools
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'docs/research/search/p4-iteration-confidence'


def analyze(name):
    path=OUT/'raw'/name/'observations.jsonl'
    digest=hashlib.sha256(path.read_bytes()).hexdigest()
    receipt=json.loads((OUT/(name+'-execution.json')).read_text())
    assert receipt['status']=='passed' and receipt['inputsStable']
    assert receipt['files'][str(path.relative_to(ROOT))]==digest
    count=Counter();markers=alternatives=0
    for row in map(json.loads,path.read_text().splitlines()):
        if row['type']!='request':continue
        last_depth=row['measurement']['outcome']['lastCompletedResult']['depth'];prior=None
        for depth,group in itertools.groupby(row['attempts'],key=lambda a:a['depth']):
            final=list(group)[-1]
            if final['classification']!='resolved-selective':continue
            if depth>last_depth:
                count['resolvedInvocationNotPublished']+=1
                continue
            jobs=final['rootJobs']
            if not jobs:prior=None;continue
            effort=Counter();retried=set()
            for job in jobs:
                assert job['completed']
                effort[job['move']]+=job['nodes']
                if job['invocation']==1:retried.add(job['move'])
            assert len(effort)==final['result']['legalRootMoves']
            favorite=final['result']['bestMove'];assert favorite in effort
            others=[move for move in effort if move!=favorite]
            marks=retried-{favorite};markers+=len(marks);alternatives+=len(others)
            order=sorted(others,key=lambda move:-effort[move])
            marked_order=sorted(others,key=lambda move:(move not in marks,-effort[move]))
            count['publishedSplitIterations']+=1
            count['withRetriedAlternative']+=bool(marks)
            count['markedAlternativesAlreadyTopThree']+=len(marks & set(order[:3]))
            count['changesTopAlternative']+=order[:1]!=marked_order[:1]
            count['changesTopThreeSet']+=set(order[:3])!=set(marked_order[:3])
            if prior and depth==prior[0]+1 and favorite!=prior[1]:
                count['nextPublishedFavoriteChanges']+=1
                count['nextFavoriteWasRetried']+=favorite in prior[4]
                count['nextFavoriteAlreadyTopThree']+=favorite in prior[2][:3]
                a,b=prior[2].index(favorite),prior[3].index(favorite)
                count['nextFavoriteRankEarlier' if b<a else 'nextFavoriteRankLater' if b>a else 'nextFavoriteRankSame']+=1
            prior=depth,favorite,order,marked_order,marks
    return dict(run=name,sha256=digest,counts=dict(count),markedAlternativeEntries=markers,
                allAlternativeEntries=alternatives)


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--output',required=True)
    args=parser.parse_args()
    result=dict(status='descriptive baseline-only replay; no new experiment or candidate',
                analyzerSha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
                runs=[analyze(n) for n in ('root-baseline-v1','root-baseline-v2','root-c2-control-v1','root-c2-control-v2')],
                semantics='Only final resolved split passes at or below the request last-published depth. '
                          'Marker means an original-window scout fail-high received a full retry. '
                          'Favorite stays first; hypothetical alternative order is marker-first then final-pass admissions. '
                          'Ties retain observed job order, which need not equal future history-order ties.',
                limitations='Repeated selected roots and correlated SMP; no independent trials. '
                            'Next-published favorite is selective, not ground truth. No cross-window score comparison, '
                            'independent worker vote, confidence calibration or changed-trajectory prediction.')
    with Path(args.output).open('x',encoding='utf-8',newline='\n') as stream:json.dump(result,stream,indent=2);stream.write('\n')
    print(json.dumps(result))
