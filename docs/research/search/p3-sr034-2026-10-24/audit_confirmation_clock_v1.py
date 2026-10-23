"""Whole-game resource/clock audit, with runner-source clock conservation after a completed frozen confirmation block only. No strength output.

Runner-source-verified initial and subsequent clocks are checked against charged
time and increment. Both experimental candidates use the same reserve-capped
double hard allocation; baseline uses its original formula.
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

def audit(stage):
    clock_source=json.loads((HERE/'fastchess-clock-source-v1.json').read_text())
    for source in clock_source['files'].values():assert digest(HERE/source['local'])==source['sha256']
    path=HERE/'raw'/stage/'games.pgn';run=json.loads((path.parent/'run.json').read_text());assert run['status']=='passed'
    assert run['arguments']['candidate_policy'] in ('unstable2','boundary2')
    assert run['arguments']['baseline_policy']=='baseline' and run['arguments']['tc']=='10+0.1'
    assert run['results']['statistics'] is None, 'Confirmation blocks must defer score analysis'
    b=module('baseline',ROOT/'tools/search-baseline.py');rows=[];games=0;checks=Counter()
    with path.open(encoding='utf8') as stream:
        while (game:=b.chess.pgn.read_game(stream)) is not None:
            assert not game.errors;board=game.board();previous={'baseline':10100,'candidate':10100};games+=1
            assert game.headers['TimeControl']=='10+0.1'
            for ply,node in enumerate(game.mainline(),1):
                telemetry=re.search(r'info string request nodes=(\d+) elapsedNanos=(\d+) termination=(\w+)',node.comment)
                actor=game.headers['White' if board.turn else 'Black'];factual=board.legal_moves.count()==1
                fullmove=board.fullmove_number;move=node.move
                assert move in board.legal_moves;board.push(move);factual=factual or board.is_checkmate()
                if telemetry is None:
                    assert 'book' in node.comment and ply<=16;continue
                total=int(telemetry[1]);elapsed=int(telemetry[2])/1e6;termination=telemetry[3]
                left=re.search(r'\btl=(\d+(?:\.\d+)?)s',node.comment);assert left,node.comment
                remaining=round(float(left[1])*1000)
                charged=re.search(r'^\S+/\d+ (\d+(?:\.\d+)?)s',node.comment);assert charged,node.comment
                charged=round(float(charged[1])*1000)
                n=re.search(r'\bn=(\d+)',node.comment);completed=None if n is None else int(n[1])
                if completed is not None:
                    assert completed<=total,'Completed nodes exceed whole request'
                    checks['accountedRequests']+=1
                    if termination=='TIME_ALLOCATION':assert completed==total,'Soft stop abandoned or fabricated work'
                else:checks['noCompletedNodeAnnotation']+=1
                hard=None
                if actor in previous:
                    before=round(previous[actor]);assert remaining==before+100-charged,'Runner clock conservation'
                    checks['clockConservation']+=1
                    base=min(max(0,before-50),max(1,before//30+75-10));hard=base
                    if actor=='candidate':hard=min(max(0,before-50),2*hard)
                row={'actor':actor,'game':games,'moveNumber':fullmove,'remainingMillis':remaining,
                     'requestMillis':elapsed,'runnerChargedMillis':charged,'reserveBeforeIncrementMillis':remaining-100,'termination':termination,'factual':factual,'inferredHardMillis':hard,
                     'baseAllocationMillis':base,'requestMinusBaseMillis':elapsed-base,
                     'wholeNodes':total,'incompleteNodes':None if completed is None else total-completed}
                if hard is not None:
                    row['requestMinusInferredHardMillis']=elapsed-hard
                    row['savedMillisAtSoftStop']=max(0,base-elapsed) if termination=='TIME_ALLOCATION' and not factual else 0
                    row['unusedHardCapMillisAtSoftStop']=max(0,hard-elapsed) if termination=='TIME_ALLOCATION' and not factual else 0
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
            'reserveBeforeIncrementMillis':distribution([x['reserveBeforeIncrementMillis'] for x in selected]),
            'runnerChargedMillis':sum(x['runnerChargedMillis'] for x in selected),
            'runnerChargeMinusRequestMillis':distribution([x['runnerChargedMillis']-x['requestMillis'] for x in selected]),
            'requestMillis':distribution([x['requestMillis'] for x in selected]),
            'requestMinusInferredHardMillis':distribution([x['requestMinusInferredHardMillis'] for x in inferred]),
            'hardStopOverrunMillis':distribution([x['requestMinusInferredHardMillis'] for x in inferred if x['termination']=='TIME_LIMIT']),
            'inferredSoftSavedMillis':sum(x['savedMillisAtSoftStop'] for x in inferred),
            'unusedHardCapMillisAtSoftStop':sum(x['unusedHardCapMillisAtSoftStop'] for x in inferred),
            'requestMinusBaseMillis':distribution([x['requestMinusBaseMillis'] for x in selected]),
            'aboveBaseMillis':sum(max(0,x['requestMinusBaseMillis']) for x in selected),
            'belowBaseMillis':sum(max(0,-x['requestMinusBaseMillis']) for x in selected),
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
    result={'status':'passed','games':games,'clockReliabilityPassed':all(x['requestMinusInferredHardMillis']<=25 for x in rows),
        'maximumRequestOverHardMillis':max(x['requestMinusInferredHardMillis'] for x in rows),
        'resourceQualification':'See final wrapper receipt',
        'clockSourceManifestSha256':digest(HERE/'fastchess-clock-source-v1.json'),'pgnSha256':digest(path),'analysisScriptSha256':digest(Path(__file__)),
        'actors':actors,'integrity':dict(checks),
        'actorNativeCpuSeconds':cost['cpuSeconds'],
        'peakWorkingSetBytes':{x['actor']:x.get('peakWorkingSetBytes') for x in cost['processes']},
        'limits':['Prospective clock-reliability gate and descriptive cost audit; no additional statistical look.',
            'Runner f618e34 stores integer millisecond clocks after adding the increment; these are the next go clocks. Initial clock10100ms and each following clock are independently checked by conservation; candidate hard cap is twice base subject to reserve. Engine request elapsed excludes some protocol/runner cost.',
            'Savings/expenditure are relative to that actor clock\'s original base allocation; unused doubled hard cap is reported separately. These are not causal whole-game value.',
            'PGN retains final iteration data, not every policy trigger. Extension-specific aggregate value requires the matched extension-off control; no per-move trigger/quality labels are fabricated.',
            'Whole-game incomplete nodes use final completed-iteration node annotation; absent annotations remain disclosed.',
            'One fixed32-pair confirmation block; no interim strength output or multiworker inference.']}
    write_new(HERE/(stage+'-clock-analysis-v1.json'),result)
    return result


def main():
    result=audit(sys.argv[1])
    print(json.dumps({k:result[k] for k in ('status','games','clockReliabilityPassed','maximumRequestOverHardMillis')},indent=2))

if __name__=='__main__':main()
