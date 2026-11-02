"""Summarize frozen HL clean-cost requests; no timing-to-strength inference."""
import argparse
import hashlib
import json
import math
from pathlib import Path
import statistics


def analyze(path):
    rows = [json.loads(s) for s in path.read_text().splitlines()]
    if rows[-1].get('status') != 'passed':
        raise ValueError('Incomplete diagnostic ' + str(path))
    requests = [r for r in rows if r.get('type') == 'request' and r['round'] >= 0]
    profiles = sorted(set(r['profile'] for r in requests))
    roots = sorted(set(r['root'] for r in requests))
    report = {'input':str(path), 'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),
              'measuredRequests':len(requests), 'warmupRequests':sum(r.get('round') == -1 for r in rows),
              'profiles':{}}
    for profile in profiles:
        values = {}
        for metric in ('elapsedNanos', 'processCpuNanos'):
            ratios = []
            for root in roots:
                current = [r['measurement'][metric] for r in requests if r['root']==root and r['profile']==profile]
                control = [r['measurement'][metric] for r in requests if r['root']==root and r['profile']=='control']
                if not current or len(current)!=len(control): raise ValueError('Unbalanced cost cells')
                ratios.append(statistics.mean(current)/statistics.mean(control))
            values[metric] = {'geometricMeanRatio':math.exp(statistics.mean(map(math.log,ratios))),
                              'minRootRatio':min(ratios),'maxRootRatio':max(ratios),'rootRatios':dict(zip(roots,ratios))}
        selected = [r for r in requests if r['profile']==profile]
        values['nodes'] = sum(r['measurement']['outcome']['nodes'] for r in selected)
        values['meanCompletedDepth'] = statistics.mean(r['measurement']['outcome']['lastCompletedResult']['depth'] for r in selected)
        report['profiles'][profile] = values
    return report


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--input',type=Path,nargs='+',required=True)
    parser.add_argument('--output',type=Path,required=True)
    args=parser.parse_args()
    result={'status':'passed','inputs':[analyze(p) for p in args.input],
            'limits':'Short fixed-node requests, rotated order, one warmup/two measured rounds. Descriptive wall/CPU ratios, no confidence/speed/strength claim; differing completed depths are selective outcomes, not correctness proofs.'}
    with args.output.open('x',encoding='utf-8') as f:json.dump(result,f,indent=2);f.write('\n')
    for item in result['inputs']:
        print(item['input'],{p:round(v['elapsedNanos']['geometricMeanRatio'],4) for p,v in item['profiles'].items()})
