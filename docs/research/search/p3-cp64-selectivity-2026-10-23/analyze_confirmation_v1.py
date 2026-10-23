"""Single final128-pair look with the prospectively allocated alpha."""
import json,math,statistics,sys
from run_observation_v1 import HERE,ROOT,digest,write_new
from run_clock_v1 import module

def intervals(pair_means,alpha=.025):
    n=len(pair_means);assert n>=2 and all(0<=x<=1 for x in pair_means)
    mean=statistics.fmean(pair_means);variance=statistics.variance(pair_means);se=math.sqrt(variance/n)
    z=statistics.NormalDist().inv_cdf(1-alpha/2)
    normal=None if se==0 else [max(0,mean-z*se),min(1,mean+z*se)]
    radius=math.sqrt(math.log(2/alpha)/(2*n));bounded=[max(0,mean-radius),min(1,mean+radius)]
    return {'pairs':n,'score':mean,'pairVariance':variance,'pairScoreStandardError':se,'alpha':alpha,'z':z,
        'pairedNormal97_5Score':normal,'pairedHoeffding97_5Score':bounded}

def elo(score):
    if score<=0:return '-infinity'
    if score>=1:return 'infinity'
    return 400*math.log10(score/(1-score))

def self_test():
    # Independent closed-form variance for64 zero/64 one pair means is32/127.
    result=intervals([0.0]*64+[1.0]*64)
    assert math.isclose(result['pairVariance'],32/127,rel_tol=1e-14)
    assert math.isclose(result['pairScoreStandardError'],1/(2*math.sqrt(127)),rel_tol=1e-14)
    assert math.isclose(result['z'],2.241402727604947,rel_tol=1e-12)
    assert result['pairedNormal97_5Score'][0]<.5<result['pairedNormal97_5Score'][1]
    assert result['pairedHoeffding97_5Score'][0]<result['pairedNormal97_5Score'][0]
    assert intervals([.5]*128)['pairedNormal97_5Score'] is None
    assert intervals([0.0,1.0]*64)==result
    assert elo(.5)==0 and math.isclose(elo(.75),190.84850188786498)
    print('Statistical closed-form, permutation, zero-variance and Elo checks passed')

def main():
    if sys.argv[1]=='--self-test':self_test();return
    hypothesis=sys.argv[1];binding=json.loads((HERE/'confirmation-v1-binding.json').read_text());assert hypothesis in binding['admitted']
    stage=binding['stages'][hypothesis];prefix=stage['prefix'];blocks=[]
    for p,sha in binding['files'].items():assert digest(ROOT/p)==sha,p
    for p,sha in binding['externalFiles'].items():assert digest(p)==sha,p
    for name in stage['blocks']:
        receipt=json.loads((HERE/(name+'-summary.json')).read_text());assert receipt['status']=='passed',name
        assert receipt['bindingSha256']==digest(HERE/'confirmation-v1-binding.json')
        for p,sha in receipt['evidence'].items():assert digest(HERE/p)==sha,p
        for field in ['preDelivery','postDelivery']:
            ref=receipt[field];assert digest(HERE/ref['path'])==ref['sha256']
            delivery=json.loads((HERE/ref['path']).read_text());assert delivery['status']=='passed' and delivery['criteriaPassed']
        blocks.append(receipt)
    folder=HERE/'raw'/prefix;folder.mkdir(exist_ok=False);combined=folder/'combined-games.pgn'
    with combined.open('xb') as stream:
        for name in stage['blocks']:stream.write((HERE/'raw'/name/'games.pgn').read_bytes()+b'\n\n')
    b=module('baseline',ROOT/'tools/search-baseline.py')
    result=b.analyze(combined,HERE/(prefix+'-openings.pgn'),128,require_accounting=True)
    assert result['terminations']=={'normal':256}
    for actor in ['baseline','candidate']:
        for metric in ['requests','nodes','elapsedNanos']:
            assert result['wholeRequestResources'][actor][metric]==sum(x['integrity']['wholeRequestResources'][actor][metric] for x in blocks)
    scores=[x['candidateScore'] for x in result['games']];pairs=[(scores[i]+scores[i+1])/2 for i in range(0,256,2)]
    stats=intervals(pairs);normal=stats['pairedNormal97_5Score'];bounded=stats['pairedHoeffding97_5Score']
    assert math.isclose(stats['score'],result['statistics']['score'],abs_tol=1e-15)
    assert [pairs.count(value) for value in [0,.25,.5,.75,1]]==result['statistics']['pentanomial']
    disposition='inconclusive' if normal is None else 'positive_primary' if normal[0]>.5 else 'negative_primary' if normal[1]<.5 else 'inconclusive'
    sensitivity='positive' if bounded[0]>.5 else 'negative' if bounded[1]<.5 else 'crosses_equality'
    stats.update(logisticElo=elo(stats['score']),pairedNormal97_5Elo=None if normal is None else [elo(x) for x in normal],
        pairedHoeffding97_5Elo=[elo(x) for x in bounded],pentanomial=result['statistics']['pentanomial'])
    costs=[json.loads((HERE/'raw'/name/'process-cost.json').read_text()) for name in stage['blocks']]
    resource={'ownedCpuSeconds':sum(x['execution']['totalOwnedCpuSeconds'] for x in blocks),'matchWallSeconds':sum(x['matchWallSeconds'] for x in blocks),
        'engineCpuSeconds':{actor:sum(x['cpuSeconds'][actor] for x in costs) for actor in ['baseline','candidate']},
        'maximumEnginePeakWorkingSetBytes':{actor:max((p['peakWorkingSetBytes'] for x in costs for p in x['processes'] if p['actor']==actor and 'peakWorkingSetBytes' in p),default=None) for actor in ['baseline','candidate']},
        'memoryReadFailures':[{'actor':p['actor'],'windowsError':p.get('memoryUnavailableWindowsError')} for x in costs for p in x['processes'] if 'peakWorkingSetBytes' not in p],
        'blockOtherAverageLogicalCores':[x['resourceQualification']['otherAverageLogicalCores'] for x in blocks],
        'blockActorDelivery':[x['actorDelivery'] for x in blocks]}
    summary={'status':'passed','hypothesis':hypothesis,'candidate':stage['candidate'],'flags':stage['flags'],'runtime':stage['runtime'],
        'bindingSha256':digest(HERE/'confirmation-v1-binding.json'),'wdl':result['wdl'],'terminations':result['terminations'],
        'statistics':stats,'primaryDisposition':disposition,'boundedSensitivity':sensitivity,'pairScores':pairs,
        'wholeRequestResources':result['wholeRequestResources'],'resources':resource,
        'blockReceipts':{name+'-summary.json':digest(HERE/(name+'-summary.json')) for name in stage['blocks']},
        'combinedEvidence':{'path':str(combined.relative_to(HERE)),'sha256':digest(combined)},
        'limits':['Fresh fixed128-pair W1 confirmation only; normal approximation and representative independent-pair assumptions apply.',
            'Both uncertainty methods depend on the sampling/domain assumptions; neither rules out systematic opening-family or clock effects.',
            'Conservative bounded sensitivity is reported separately, including any interval crossing equality.',
            'No multiworker strength inference, automatic production promotion, external installation or successor objective.']}
    write_new(HERE/(prefix+'-summary.json'),summary)
    print(json.dumps({k:v for k,v in summary.items() if k not in ['pairScores','blockReceipts']},indent=2))

if __name__=='__main__':main()
