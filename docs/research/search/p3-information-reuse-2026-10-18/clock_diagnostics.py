"""Summarize completed clock evidence only; no strength assessment or replay."""
from collections import Counter
import hashlib
import json
from pathlib import Path
import re
import statistics
import sys
import chess.pgn


def read(path):return json.loads(Path(path).read_text(encoding='utf-8-sig'))
def digest(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def summarize(summary_path):
    summary_path=Path(summary_path).resolve();s=read(summary_path)
    study=Path(s['plan']).parent;export=Path(s['export']['export'])
    receipt=read(export/'complete.json');games=export/'games.pgn'
    if digest(export/'complete.json')!=s['exportReceiptSha256'] or digest(games)!=receipt['gamesSha256']:
        raise ValueError('Completed evidence changed')
    depth={side:Counter()for side in ('baseline','candidate')};missing=Counter();count=0
    with games.open(encoding='utf8')as stream:
        while (game:=chess.pgn.read_game(stream))is not None:
            count+=1
            if game.errors:raise ValueError('PGN parse error')
            for node in game.mainline():
                if 'request nodes='not in node.comment:continue
                actor=game.headers['White'if node.ply()%2 else 'Black']
                match=re.match(r'^[^/\s]+/(\d+)\s',node.comment)
                if match:depth[actor][int(match[1])]+=1
                else:missing[actor]+=1
    if count!=sum(s['wdl']):raise ValueError('Game count mismatch')
    actors={};files={str(games):digest(games)}
    for actor,counts in depth.items():
        requests=sum(counts.values())+missing[actor]
        if requests!=s['wholeRequestResources'][actor]['requests']:raise ValueError('Request coverage mismatch')
        values=sorted(counts.elements());pauses=[];logs=[]
        for block in s['blocks']:
            directory=study/f"block-{block['index']:04d}"
            cost=read(directory/'process-cost.json')
            if digest(directory/'process-cost.json')!=block['processCostSha256']:raise ValueError('CPU evidence changed')
            for process in cost['processes']:
                if process['actor']!=actor:continue
                path=directory/f"gc-{process['pid']}.log";text=path.read_text(encoding='utf8')
                times=[float(x)for x in re.findall(r'GC\(\d+\) Pause [^\r\n]*? (\d+(?:\.\d+)?)ms$',text,re.M)]
                pauses+=times;logs.append(str(path));files[str(path)]=digest(path)
        actors[actor]={'requests':requests,'reportedDepthHistogram':dict(sorted(counts.items())),
                       'missingReportedDepth':missing[actor],'meanReportedDepth':statistics.mean(values)if values else None,
                       'medianReportedDepth':statistics.median(values)if values else None,
                       'reportedDepthAtMostFive':sum(v for k,v in counts.items()if k<=5),
                       'gcLogCount':len(logs),'recordedPauseCount':len(pauses),'recordedPauseMilliseconds':sum(pauses),
                       'maximumRecordedPauseMilliseconds':max(pauses,default=None)}
    return {'completedSummary':str(summary_path),'completedSummarySha256':digest(summary_path),
            'games':count,'actors':actors,'files':files,
            'limits':'Post-hoc descriptive diagnostics on completed development games. Different trajectories prevent matched-position depth/NPS comparisons. Reported depth is a completed publication, not all attempted depth. GC summary pauses include match startup/warmup and exclude separate preflight; they are not allocation totals, all JVM costs or peak memory. No strength inference, tuning or stopping rule.'}


if __name__=='__main__':
    if len(sys.argv)<3:raise ValueError('NEW-output.json completed-summary.json [completed-summary.json ...]')
    out=Path(sys.argv[1])
    if out.exists():raise ValueError('Never overwrite an analysis')
    result={'sourceSha256':digest(__file__),'studies':[summarize(p)for p in sys.argv[2:]]}
    out.write_bytes((json.dumps(result,indent=2)+'\n').encode())
    print(json.dumps([{k:v for k,v in s.items()if k not in ('files','limits')}for s in result['studies']],indent=2))
