"""Summarize complete root-job observations, preserving bound/completion semantics."""
import argparse
from collections import Counter
import hashlib
import json
from pathlib import Path
import statistics

OUT=Path(__file__).resolve().parents[1]/'docs/research/search/p4-iteration-confidence'
ROOT=OUT.parents[3]


def analyze(name):
    path=OUT/'raw'/name/'observations.jsonl'
    receipt=json.loads((OUT/(name+'-execution.json')).read_text())
    digest=hashlib.sha256(path.read_bytes()).hexdigest()
    assert receipt['status']=='passed' and receipt['inputsStable'] and receipt['drain']['ownedProcessesRemaining']==0
    assert receipt['files'][str(path.relative_to(ROOT))]==digest
    rows=[json.loads(x) for x in path.read_text().splitlines()]
    counts=Counter();nodes=Counter();depth=[]
    for row in rows:
        if row['type']!='request':continue
        depth.append(row['measurement']['outcome']['lastCompletedResult']['depth'])
        for attempt in row['attempts']:
            entered=attempt['result']['nodes'];nodes['all']+=entered;nodes[attempt['classification']]+=entered
            jobs=attempt['rootJobs'];seen={}
            if jobs:assert sum(job['nodes'] for job in jobs)==entered
            else:nodes['wholeRoot']+=entered
            for job in jobs:
                assert job['alpha']<job['beta'] and job['nodes']>=0 and 0<=job['worker']<4
                assert job['depth']==attempt['depth'] and job['invocation'] in (0,1)
                key=job['job']
                if job['invocation']==1:
                    first=seen[key]
                    assert first['scout'] and first['completed'] and not job['scout']
                    assert first['score']>first['alpha'] and first['score']<attempt['beta']
                    assert first['move']==job['move'] and first['worker']==job['worker']
                else:assert key not in seen
                seen[key]=job
                label='scout' if job['scout'] else 'fullRetry' if job['invocation'] else 'initialFull'
                counts[label]+=1;nodes[label]+=job['nodes']
                if not job['completed']:nodes['aborted'+label]+=job['nodes']
    assert nodes['all']==receipt['observedNodes']==rows[-1]['nodes']
    assert nodes['all']==sum(nodes[k] for k in ('wholeRoot','scout','fullRetry','initialFull'))
    return dict(run=name,input=str(path.relative_to(ROOT)),sha256=digest,requests=len(depth),
                meanCompletedDepth=statistics.mean(depth),jobs=dict(counts),nodes=dict(nodes),
                fullRetryFraction=nodes['fullRetry']/nodes['all'],
                boundPassFraction=(nodes['upper']+nodes['lower'])/nodes['all'],
                incompletePassFraction=nodes['incomplete']/nodes['all'])


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--runs',nargs='+',required=True);parser.add_argument('--output',required=True)
    args=parser.parse_args()
    result=dict(status='validated observational accounting; no strength or causal speed claim',
                analyzerSha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
                runs=[analyze(name) for name in args.runs],
                limitations='Instrumentation may perturb concurrent scheduling. Admissions, not useful work. '
                            'Selective root bounds retain original windows and worker provenance; no independent confidence. '
                            'Root-job categories partition all split-pass admissions; wholeRoot covers shallow/non-split passes. '
                            'Aborted categories are subsets, and aspiration classifications overlap the job categories.')
    with Path(args.output).open('x',encoding='utf-8',newline='\n') as stream:json.dump(result,stream,indent=2);stream.write('\n')
    print(json.dumps(result))
