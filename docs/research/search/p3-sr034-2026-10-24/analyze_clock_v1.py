"""Whole-game resource/clock audit after a completed frozen comparison only.

PGN clocks are rounded observations. The first searched move of each color has
no previous recorded clock, so its allocation is not inferred. Later inferred
allocations use the previous same-color time-left and the actual baseline formula.
"""
from collections import Counter, defaultdict
import json
from pathlib import Path
import re
import statistics as st
import sys
from run_trajectory_v1 import HERE, ROOT, digest, write_new, module

def distribution(values):
    if not values:return {'n':0}
    v=sorted(values)
    return {'n':len(v),'min':v[0],'p10':v[int(.1*(len(v)-1))],'median':st.median(v),
            'mean':st.mean(v),'p90':v[int(.9*(len(v)-1))],'p99':v[int(.99*(len(v)-1))],'max':v[-1]}

def main():
    stage=sys.argv[1];summary_path=HERE/(stage+'-summary.json')
    completion=json.loads(summary_path.read_text());assert completion['status'] in ('passed','resource-unqualified')
    path=HERE/'raw'/stage/'games.pgn';run=json.loads((path.parent/'run.json').read_text());assert run['status']=='passed'
    b=module('baseline',ROOT/'tools/search-baseline.py');rows=[];games=0;checks=Counter()
    with path.open(encoding='utf8') as stream:
        while (game:=b.chess.pgn.read_game(stream)) is not None:
            assert not game.errors;board=game.board();previous={};games+=1
            for ply,node in enumerate(game.mainline(),1):
                telemetry=re.search(r'info string request nodes=(\d+) elapsedNanos=(\d+) termination=(\w+)',node.comment)
                actor=game.headers['White' if board.turn else 'Black'];factual=board.legal_moves.count()==1
                fullmove=board.fullmove_number;move=node.move
                assert move in board.legal_moves;board.push(move);factual=factual or board.is_checkmate()
                if telemetry is None:
                    assert 'book' in node.comment and ply<=16;continue
                total=int(telemetry[1]);elapsed=int(telemetry[2])/1e6;termination=telemetry[3]
                left=re.search(r'\btl=(\d+(?:\.\d+)?)s',node.comment);assert left,node.comment
                remaining=float(left[1])*1000
                n=re.search(r'\bn=(\d+)',node.comment);completed=None if n is None else int(n[1])
                if completed is not None:
                    assert completed<=total,'Completed nodes exceed whole request'
                    checks['accountedRequests']+=1
                    if termination=='TIME_ALLOCATION':assert completed==total,'Soft stop abandoned or fabricated work'
                else:checks['noCompletedNodeAnnotation']+=1
                hard=None
                if actor in previous:
                    before=round(previous[actor]);hard=min(max(0,before-50),max(1,before//30+75-10))
                row={'actor':actor,'game':games,'moveNumber':fullmove,'remainingMillis':remaining,
                     'requestMillis':elapsed,'termination':termination,'factual':factual,'inferredHardMillis':hard,
                     'wholeNodes':total,'incompleteNodes':None if completed is None else total-completed}
                if hard is not None:
                    row['requestMinusInferredHardMillis']=elapsed-hard
                    row['savedMillisAtSoftStop']=max(0,hard-elapsed) if termination=='TIME_ALLOCATION' and not factual else 0
                rows.append(row);previous[actor]=remaining
    assert games==64
    actors={}
    for actor in ('baseline','candidate'):
        selected=[x for x in rows if x['actor']==actor]
        inferred=[x for x in selected if x['inferredHardMillis'] is not None]
        totals=run['results']['wholeRequestResources'][actor]
        assert sum(x['wholeNodes'] for x in selected)==totals['nodes']
        actors[actor]={'requests':len(selected),'terminations':dict(Counter(x['termination'] for x in selected)),
            'nonfactualSoftStops':sum(x['termination']=='TIME_ALLOCATION' and not x['factual'] for x in selected),
            'remainingMillis':distribution([x['remainingMillis'] for x in selected]),
            'requestMillis':distribution([x['requestMillis'] for x in selected]),
            'requestMinusInferredHardMillis':distribution([x['requestMinusInferredHardMillis'] for x in inferred]),
            'hardStopOverrunMillis':distribution([x['requestMinusInferredHardMillis'] for x in inferred if x['termination']=='TIME_LIMIT']),
            'inferredSoftSavedMillis':sum(x['savedMillisAtSoftStop'] for x in inferred),
            'incompleteNodes':sum(x['incompleteNodes'] or 0 for x in selected),
            'wholeNodes':totals['nodes'],'wholeRequestElapsedNanos':totals['elapsedNanos'],
            'lowClockResponses':sum(x['remainingMillis']<500 for x in selected),
            'byMoveNumber':[]}
        for low,high in ((1,20),(21,40),(41,60),(61,1000)):
            segment=[x for x in selected if low<=x['moveNumber']<=high]
            actors[actor]['byMoveNumber'].append({'range':[low,high],'requests':len(segment),
                'remainingMillis':distribution([x['remainingMillis'] for x in segment]),
                'requestMillis':distribution([x['requestMillis'] for x in segment]),
                'softStops':sum(x['termination']=='TIME_ALLOCATION' and not x['factual'] for x in segment)})
    cost=json.loads((path.parent/'process-cost.json').read_text());assert cost['status']=='passed'
    result={'status':'passed','games':games,'resourceQualification':completion['status'],
        'pgnSha256':digest(path),'completionSummarySha256':digest(summary_path),'analysisScriptSha256':digest(Path(__file__)),
        'strength':{k:run['results'][k] for k in ('wdl','statistics','terminations')},'actors':actors,'integrity':dict(checks),
        'actorNativeCpuSeconds':cost['cpuSeconds'],
        'peakWorkingSetBytes':{x['actor']:x.get('peakWorkingSetBytes') for x in cost['processes']},
        'limits':['Post-match descriptive audit; no new statistical look or selection rule.',
            'Inferred allocations use rounded PGN clocks, exclude first searched move per color, and omit protocol-transit timing.',
            'Reported savings are within-position allocation differences, not causal whole-game value.',
            'Whole-game incomplete nodes use final completed-iteration node annotation; absent annotations remain disclosed.',
            'Fixed32-pair development, W1/10+0.1 only; no independent confirmation or multiworker strength.']}
    write_new(HERE/(stage+'-clock-analysis.json'),result)
    print(json.dumps({'status':'passed','strength':result['strength'],'actors':{
        a:{k:v for k,v in x.items() if k not in ('byMoveNumber','requestMinusInferredHardMillis')} for a,x in actors.items()}},indent=2))

if __name__=='__main__':main()
