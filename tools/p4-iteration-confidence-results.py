"""Analyze only a complete, drained IC exploratory sample; pilot is operational only.
Uses the existing frozen independent game audits and paired-statistics definition.
"""
import argparse
import importlib.util
import json
from pathlib import Path
import statistics
import sys

sys.dont_write_bytecode=True
spec=importlib.util.spec_from_file_location('ic_result_support',Path(__file__).with_name('p4-multiworker-match.py'))
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
OUT=m.ROOT/'docs/research/search/p4-iteration-confidence'


def analyze(receipt_path,operational=False):
    receipt_path=Path(receipt_path).resolve();r=json.loads(receipt_path.read_text())
    protocol=m.ROOT/r['protocol'];p=json.loads(protocol.read_text())
    if (r['status']!='passed' or r['error'] or not r['inputsStable']
            or r['drain']['ownedProcessesRemaining']!=0 or r['initiatedGames']!=p['games']
            or r['completedGames']!=p['games'] or m.b.digest(protocol)!=r['protocolSha256']
            or p['id']!=r['id'] or p['category']!='exploration'):
        raise ValueError('Only complete, input-stable, audited and drained exploratory samples')
    operational=operational or 'pilot' in p['id']
    for name,digest in r['files'].items():
        if m.b.digest(m.ROOT/name)!=digest:raise ValueError('Changed raw evidence '+name)
    directory=OUT/'raw'/p['id'];resources={s:dict(requests=0,nodes=0,elapsedNanos=0) for s in ('baseline','candidate')}
    pairs=[];wdl=[0,0,0];warnings=0;terminations={};first=[];second=[];colors={'White':[],'Black':[]}
    for i in range(len(p['openingIndices'])):
        v=json.loads((directory/f'pair-{i:03d}'/'verified.json').read_text());result=v['results']
        if len(result['games'])!=2:raise ValueError('Pair count')
        if v['firstGameWhite']!=('candidate' if i%2 else 'baseline'):raise ValueError('First-color alternation')
        warnings+=v['traceAudit']['qualifiedMissingScoreWarnings']
        for side in resources:
            for key in resources[side]:resources[side][key]+=result['wholeRequestResources'][side][key]
        for game in result['games']:
            t=game['headers']['Termination'];terminations[t]=terminations.get(t,0)+1
        if not operational:
            pairs.append(sum(g['candidateScore'] for g in result['games']))
            for j in range(3):wdl[j]+=result['wdl'][j]
            for game in result['games']:
                score=game['candidateScore'];color='White' if game['headers']['White']=='candidate' else 'Black'
                colors[color].append(score)
                (first if game['headers']['White']==v['firstGameWhite'] else second).append(score)
    if sum(v['nodes'] for v in resources.values())!=r['observedNodes']:raise ValueError('Node total')
    samples=[json.loads(x) for x in (directory/'environment.jsonl').read_text().splitlines()]
    summary=dict(status='passed complete operational review' if operational else 'complete exploratory analysis',
                 id=p['id'],games=r['completedGames'],wholeRequestResources=resources,terminations=terminations,
                 qualifiedFallbackWarnings=warnings,chargedWallSeconds=r['chargedWallSeconds'],drain=r['drain'],
                 environment=dict(samples=len(samples),meanTotalCpuPercent=statistics.mean(s['totalCpuPercent'] for s in samples),
                                  maximumTotalCpuPercent=max(s['totalCpuPercent'] for s in samples),
                                  minimumFreeMemoryGiB=min(s['freeMemoryGiB'] for s in samples),peakOwnedCommittedGiB=r['peakOwnedCommittedGiB']),
                 execution=str(receipt_path.relative_to(m.ROOT)),executionSha256=m.b.digest(receipt_path),
                 protocolSha256=m.b.digest(protocol),analyzerSha256=m.b.digest(Path(__file__)),
                 strengthInterpretation='Pilot never informs selection or pools with screen.' if operational else
                     'Exploratory paired interval only; selected candidate/openings and approximate dependence. No independent strength, equivalence or promotion claim.')
    if not operational:
        summary.update(wdl=wdl,statistics=m.b.paired_statistics(pairs),
                       descriptiveGameOrderScores=dict(first=statistics.mean(first),second=statistics.mean(second)),
                       descriptiveCandidateColorScores={c:statistics.mean(x) for c,x in colors.items()})
    return summary


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--receipt',required=True)
    parser.add_argument('--output',required=True);parser.add_argument('--operational-only',action='store_true')
    args=parser.parse_args();result=analyze(args.receipt,args.operational_only)
    with Path(args.output).open('x',encoding='utf-8',newline='\n') as f:json.dump(result,f,indent=2);f.write('\n')
    print(json.dumps(result))
