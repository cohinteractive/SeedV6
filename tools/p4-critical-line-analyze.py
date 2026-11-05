"""Hash-verified descriptive CLV fixed-work summary. No Elo/ground-truth inference."""
import argparse
import collections
import hashlib
import json
from pathlib import Path
import statistics

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'docs/research/search/p4-critical-line'


def digest(p):
    return hashlib.sha256(Path(p).read_bytes()).hexdigest()


def analyze(name):
    receipt = OUT / (name + '-execution.json')
    r = json.loads(receipt.read_text(encoding='utf8'))
    if r['status'] != 'passed' or not r['inputsStable'] or r['drain']['ownedProcessesRemaining']:
        raise ValueError('Require completed, stable and drained run')
    protocol = ROOT / r['protocol']
    if digest(protocol) != r['protocolSha256']: raise ValueError('Changed protocol')
    for p, sha in r['files'].items():
        if digest(ROOT / p) != sha: raise ValueError('Changed evidence ' + p)
    rows = [x for x in map(json.loads, (OUT / 'raw' / name / 'observations.jsonl').read_text().splitlines()) if x['type'] == 'request']
    profiles = {}; indexed = {}
    for profile in sorted({x['profile'] for x in rows}):
        group = [x for x in rows if x['profile'] == profile]
        results = [x['measurement']['outcome']['lastCompletedResult'] for x in group]
        indexed[profile] = {(x['root'], x['round']): x['measurement']['outcome']['lastCompletedResult'] for x in group}
        profiles[profile] = dict(requests=len(group), nodes=sum(x['measurement']['outcome']['nodes'] for x in group),
            eligibleNodes=sum(x['criticalNodes'] for x in group), reliefApplications=sum(x['criticalReliefs'] for x in group),
            meanCompletedDepth=statistics.mean(x['depth'] for x in results),
            observedRequestWallSeconds=sum(x['measurement']['elapsedNanos'] for x in group)/1e9,
            decisions=[dict(root=x['root'], round=x['round'], **{k:x['measurement']['outcome']['lastCompletedResult'][k]
                        for k in ('bestMove','score','depth')}) for x in group])
    a, b = indexed['control'], indexed['c1']
    if a.keys() != b.keys(): raise ValueError('Unpaired diagnostic requests')
    return dict(id=name, executionSha256=digest(receipt), protocolSha256=r['protocolSha256'], profiles=profiles,
        changedMoveComparisons=sum(a[k]['bestMove']!=b[k]['bestMove'] for k in a),
        higherDepth=sum(b[k]['depth']>a[k]['depth'] for k in a), lowerDepth=sum(b[k]['depth']<a[k]['depth'] for k in a),
        chargedWallSeconds=r['chargedWallSeconds'], limitations='Selected repeated historical roots; selective references are not ground truth. SMP changes are scheduling-dependent. Wall includes diagnostic/JIT effects, not clean speed. No playing-strength evidence.')


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__); p.add_argument('--runs', nargs='+', required=True); p.add_argument('--output', required=True)
    args=p.parse_args(); result=dict(runs=[analyze(n) for n in args.runs], analyzerSha256=digest(__file__))
    with Path(args.output).open('x', encoding='utf8', newline='\n') as f: json.dump(result,f,indent=2);f.write('\n')
    print(json.dumps([{k:v for k,v in r.items() if k!='profiles'} for r in result['runs']]))
