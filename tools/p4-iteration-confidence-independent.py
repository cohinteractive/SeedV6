"""Summarize drained owner/worker observations without selecting helper results."""
import argparse
from collections import Counter
import hashlib
import json
from pathlib import Path
import statistics

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'docs/research/search/p4-iteration-confidence'


def analyze(name):
    path=OUT/'raw'/name/'observations.jsonl';digest=hashlib.sha256(path.read_bytes()).hexdigest()
    receipt=json.loads((OUT/(name+'-execution.json')).read_text())
    assert receipt['status']=='passed' and receipt['inputsStable'] and receipt['drain']['ownedProcessesRemaining']==0
    assert receipt['files'][str(path.relative_to(ROOT))]==digest
    rows=[json.loads(line) for line in path.read_text().splitlines()]
    data=[r for r in rows if r['type']=='request'];depths=[];helpers=Counter();moves=Counter();shares=[]
    for row in data:
        outcome=row['measurement']['outcome'];depths.append(outcome['lastCompletedResult']['depth'])
        if 'workerProgress' not in row:continue
        workers=row['workerProgress'];owner=workers[0];assert sum(w['nodes'] for w in workers)==outcome['nodes']
        assert owner['completedDepth']==outcome['lastCompletedResult']['depth']
        shares.append(owner['nodes']/outcome['nodes'])
        helpers['requestsWithDeeperHelper']+=any(w['completedDepth']>owner['completedDepth'] for w in workers[1:])
        for worker in workers[1:]:
            helpers['deeper' if worker['completedDepth']>owner['completedDepth'] else
                    'sameDepth' if worker['completedDepth']==owner['completedDepth'] else 'shallower']+=1
            if worker.get('completedResult') and owner.get('completedResult'):
                moves['sameMove' if worker['completedResult']['bestMove']==owner['completedResult']['bestMove'] else 'differentMove']+=1
    nodes=sum(row['measurement']['outcome']['nodes'] for row in data)
    assert nodes==receipt['observedNodes']==rows[-1]['nodes']
    return dict(run=name,inputSha256=digest,requests=len(data),nodes=nodes,meanOwnerDepth=statistics.mean(depths),
                depthCounts=dict(Counter(depths)),requestElapsedSeconds=sum(r['measurement']['elapsedNanos'] for r in data)/1e9,
                processCpuSeconds=sum(r['measurement']['processCpuNanos'] for r in data)/1e9,
                helperDepths=dict(helpers),helperMoveAgreement=dict(moves),meanOwnerNodeShare=statistics.mean(shares) if shares else None)


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--runs',nargs='+',required=True);parser.add_argument('--output',required=True)
    args=parser.parse_args()
    result=dict(status='validated fixed-work observation; not strength or clean-speed qualification',
                analyzerSha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),runs=[analyze(n) for n in args.runs],
                limitations='Fresh request/TT, selected repeated roots, JIT/measurement overhead and SMP variance. '
                            'Helper depths and moves are actual completed observations at differing depths; '
                            'common TT/order can correlate them. No voting, truth or causal-speed inference.')
    with Path(args.output).open('x',encoding='utf-8',newline='\n') as stream:json.dump(result,stream,indent=2);stream.write('\n')
    print(json.dumps(result))
