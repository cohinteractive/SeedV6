"""Aggregate a complete frozen MW-R001 match; never score a live or failed prefix."""
import argparse
import importlib.util
import json
from pathlib import Path
import statistics

spec=importlib.util.spec_from_file_location('mw_result_support',Path(__file__).with_name('p4-multiworker-match.py'))
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)


def analyze(receipt_path):
    r=json.loads(receipt_path.read_text())
    p_path=m.ROOT/r['protocol']; p=json.loads(p_path.read_text())
    if (r['status']!='passed' or not r['inputsStable'] or r['error']
            or r['drain']['ownedProcessesRemaining']!=0
            or r['completedGames']!=r['maximumGames'] or r['initiatedGames']!=r['completedGames']
            or len(r['pairs'])!=len(p['openingIndices'])):
        raise ValueError('Requires the complete audited sample and clean drain, not a live/failed prefix')
    if m.b.digest(p_path)!=r['protocolSha256'] or p['id']!=r['id'] or p['category'] not in ('exploration','confirmation'):
        raise ValueError('Protocol identity/domain changed')
    claim=p['category']=='confirmation'
    if claim:
        for tool in ('tools/p4-multiworker-results.py','tools/p4-multiworker-confirmation.py'):
            if m.b.digest(m.ROOT/tool)!=p['inputs'].get(tool): raise ValueError('Changed frozen confirmation analysis')
        if p['confirmation']['gate']!=m.confirmation.GATE: raise ValueError('Changed primary gate')
    directory=m.OUT/'raw'/p['id']
    def verified(path):
        if m.b.digest(path)!=r['files'][str(path)]: raise ValueError('Changed/missing evidence '+str(path))
    resources={side:dict(requests=0,nodes=0,elapsedNanos=0) for side in ('baseline','candidate')}
    pairs=[]; wdl=[0,0,0]; first=[]; second=[]; by_color={'White':[],'Black':[]}; warnings=0
    terminations={}; plies=[]
    for index in range(len(p['openingIndices'])):
        sub=directory/f'pair-{index:03d}'; verified(sub/'verified.json'); verified(sub/'games.pgn')
        v=json.loads((sub/'verified.json').read_text()); result=v['results']
        total=sum(g['candidateScore'] for g in result['games'])
        if r['pairs'][index]!=dict(pair=index,wdl=result['wdl'],score=total/2):
            raise ValueError('Pair identity/accounting mismatch')
        pairs.append(total)
        for i in range(3):wdl[i]+=result['wdl'][i]
        for side in resources:
            for k in resources[side]:resources[side][k]+=result['wholeRequestResources'][side][k]
        warnings+=v['traceAudit']['qualifiedMissingScoreWarnings']
        with (sub/'games.pgn').open(encoding='utf8') as stream:
            headers=[m.b.chess.pgn.read_headers(stream),m.b.chess.pgn.read_headers(stream)]
            if m.b.chess.pgn.read_headers(stream) is not None: raise ValueError('Excess game')
        if {h['White'] for h in headers}!={'baseline','candidate'}:raise ValueError('Colors')
        for game in result['games']:
            score=game['candidateScore']; color='White' if game['headers']['White']=='candidate' else 'Black'
            by_color[color].append(score); plies.append(game['plies'])
            (first if game['headers']['White']==headers[0]['White'] else second).append(score)
            t=game['headers']['Termination'];terminations[t]=terminations.get(t,0)+1
    if sum(v['nodes'] for v in resources.values())!=r['observedNodes']:raise ValueError('Node total')
    environment=directory/'environment.jsonl';verified(environment)
    samples=[json.loads(line) for line in environment.read_text().splitlines()]
    summary=dict(status='complete independent confirmation analysis' if claim else 'complete exploratory analysis',id=p['id'],kind=p['kind'],workers=p['workers'],
        candidate=p['candidate'],baseline=p['baseline'],runtime=p['runtime'],games=r['completedGames'],wdl=wdl,
        statistics=m.b.paired_statistics(pairs),wholeRequestResources=resources,
        descriptiveGameOrderScores=dict(first=statistics.mean(first),second=statistics.mean(second)),
        descriptiveCandidateColorScores={color:statistics.mean(values) for color,values in by_color.items()},
        orderInterpretation='Post-hoc descriptive strata; game order/color/opening dependence is not a causal warmup test. No exclusions or score-driven extension.',
        terminations=terminations,meanGamePlies=statistics.mean(plies),qualifiedFallbackWarnings=warnings,
        chargedNodes=r['chargedNodes'],wallSeconds=r['wallSeconds'],peakOwnedCommittedGiB=r['peakOwnedCommittedGiB'],
        environment=dict(samples=len(samples),meanTotalCpuPercent=statistics.mean(s['totalCpuPercent'] for s in samples),
                         maximumTotalCpuPercent=max(s['totalCpuPercent'] for s in samples),
                         minimumFreeMemoryGiB=min(s['freeMemoryGiB'] for s in samples)),
        drain=r['drain'],protocol=str(p_path.relative_to(m.ROOT)),protocolSha256=m.b.digest(p_path),
        execution=str(receipt_path.relative_to(m.ROOT)),executionSha256=m.b.digest(receipt_path),
        analyzerSha256=m.b.digest(Path(__file__)),rawDirectory=str(directory.relative_to(m.ROOT)),
        interpretation='Frozen development sample only; paired intervals do not correct candidate selection/multiple comparisons. No independent confirmation, neutrality, champion or production claim.')
    if claim:
        summary['confirmation']=m.confirmation.assess(summary['statistics'])
        summary['interpretation']='One complete independent fixed sample, separate from candidate-selection exploration. The statistical gate is reported without inferring technical acceptance, champion advancement, production adoption or transfer beyond the frozen W4 domain.'
    return summary


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--receipt',type=Path,required=True);parser.add_argument('--output',type=Path,required=True)
    args=parser.parse_args()
    if args.output.exists():raise FileExistsError('Preserve previous analysis')
    result=analyze(args.receipt.resolve())
    with args.output.open('x',encoding='utf8') as out:json.dump(result,out,indent=2);out.write('\n')
    print(json.dumps({k:result[k] for k in ('id','games','wdl','statistics','descriptiveGameOrderScores','environment')}))
