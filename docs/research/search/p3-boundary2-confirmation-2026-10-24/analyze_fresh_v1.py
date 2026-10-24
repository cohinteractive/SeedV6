"""One prospectively declared final192-pair look, guarded by all six qualifications."""
import json
import math
import statistics
import sys
from verify_inputs import HERE, ROOT, digest, write_new
from run_diagnostic_v1 import module
from study_common_v1 import stable, qualified_block, reference


def intervals(pairs,alpha=.05):
    n=len(pairs); assert n>=2 and all(0<=p<=1 for p in pairs)
    mean=statistics.fmean(pairs); variance=statistics.variance(pairs); se=math.sqrt(variance/n)
    z=statistics.NormalDist().inv_cdf(1-alpha/2)
    normal=None if se==0 else [max(0,mean-z*se),min(1,mean+z*se)]
    radius=math.sqrt(math.log(2/alpha)/(2*n))
    return {'pairs':n,'score':mean,'pairVariance':variance,'standardError':se,'alpha':alpha,'z':z,
            'normal95Score':normal,'hoeffding95Score':[max(0,mean-radius),min(1,mean+radius)]}


def elo(score):
    return '-infinity' if score<=0 else 'infinity' if score>=1 else 400*math.log10(score/(1-score))


def self_test():
    value=intervals([0.]*96+[1.]*96)
    assert math.isclose(value['pairVariance'],48/191,rel_tol=1e-14)
    assert math.isclose(value['standardError'],1/(2*math.sqrt(191)),rel_tol=1e-14)
    assert math.isclose(value['z'],1.959963984540054,rel_tol=1e-12)
    assert value==intervals([0.,1.]*96)
    assert intervals([.5]*192)['normal95Score'] is None
    assert math.isclose(elo(.75),190.84850188786498) and elo(.5)==0
    assert value['hoeffding95Score'][0]<value['normal95Score'][0]<.5
    delta=1/(1+10**(-50/400))-.5
    raw=math.ceil((value['z']*.5/delta)**2); assert raw==189 and math.ceil(raw/32)*32==192
    return {'status':'passed','checks':'Closed-form variance/SE/z, permutation, zero-variance, Elo, conservative-bound ordering and prospective sample arithmetic','rawPrecisionPairs':raw,'roundedPairs':192,'analyzerSha256':digest(__file__)}


def main():
    binding=stable(); plan=json.loads((HERE/'confirmation-v1.json').read_text()); assert plan['domain']['pairs']==192
    blocks=[]; clocks=[]
    for name in binding['blocks']:
        block,clock=qualified_block(name); blocks.append(block); clocks.append(clock)
    assert len(blocks)==6
    folder=HERE/'raw/boundary2-fresh-v1-final'; folder.mkdir(exist_ok=False)
    combined=folder/'games.pgn'
    with combined.open('xb') as stream:
        for name in binding['blocks']: stream.write((HERE/'raw'/name/'games.pgn').read_bytes()+b'\n\n')
    b=module('baseline',ROOT/'tools/search-baseline.py')
    result=b.analyze(combined,HERE/'boundary2-fresh-v1-openings.pgn',192,require_accounting=True)
    assert result['terminations']=={'normal':384}
    for actor in ('baseline','candidate'):
        for key in ('requests','nodes','elapsedNanos'):
            assert result['wholeRequestResources'][actor][key]==sum(r['integrity']['wholeRequestResources'][actor][key] for r in blocks)
    scores=[g['candidateScore'] for g in result['games']]; assert len(scores)==384
    pairs=[(scores[i]+scores[i+1])/2 for i in range(0,384,2)]
    stats=intervals(pairs); normal=stats['normal95Score']; bounded=stats['hoeffding95Score']
    assert math.isclose(stats['score'],result['statistics']['score'],abs_tol=1e-15)
    assert [pairs.count(p) for p in (0,.25,.5,.75,1)]==result['statistics']['pentanomial']
    stats.update(elo=elo(stats['score']),normal95Elo=None if normal is None else [elo(x) for x in normal],
        hoeffding95Elo=[elo(x) for x in bounded],pentanomial=result['statistics']['pentanomial'])
    disposition='inconclusive' if normal is None else 'positive_primary' if normal[0]>.5 else 'negative_primary' if normal[1]<.5 else 'inconclusive'
    sensitivity='positive' if bounded[0]>.5 else 'negative' if bounded[1]<.5 else 'crosses_equality'
    costs=[json.loads((HERE/'raw'/name/'process-cost.json').read_text()) for name in binding['blocks']]
    record={'status':'passed','primaryDisposition':disposition,'boundedSensitivity':sensitivity,'statistics':stats,
        'wdl':result['wdl'],'terminations':result['terminations'],'pairScores':pairs,
        'descriptiveBlockPairMeans':[statistics.fmean(pairs[i:i+32]) for i in range(0,192,32)],
        'wholeRequestResources':result['wholeRequestResources'],
        'resources':{'matchWallSeconds':sum(r['matchWallSeconds'] for r in blocks),
            'allOwnedMatchCpuSeconds':sum(r['execution']['totalOwnedCpuSeconds'] for r in blocks),
            'actorNativeCpuSeconds':{a:sum(c['cpuSeconds'][a] for c in costs) for a in ('baseline','candidate')},
            'blockOtherAverageCores':[r['resourceQualification']['otherAverageLogicalCores'] for r in blocks]},
        'clock':{'allBlocksQualified':True,'maximumReportedOverHardMillis':max(c['maximumRequestOverHardMillis'] for c in clocks),
            'maximumRunnerChargeOverHardMillis':max(c['maximumRunnerChargeOverHardMillis'] for c in clocks),
            'minimumPreIncrementReserve':{a:min(c['actors'][a]['reserveBeforeIncrementMillis']['min'] for c in clocks) for a in ('baseline','candidate')}},
        'binding':reference(HERE/'confirmation-v1-binding.json'),'blocks':{name:reference(HERE/(name+'-summary.json')) for name in binding['blocks']},
        'combinedEvidence':reference(combined),'analyzerSha256':digest(__file__),
        'limits':['One selected complete policy, fresh192-pair W1/10+0.1 domain; no component or multiworker strength claim.',
            'Normal approximation and representative independent-pair assumptions; opening-family/persistent-host dependence remains possible.',
            'Bounded sensitivity is separate and may cross equality; a positive primary result is not automatic production acceptance.',
            'Historical unqualified scores remained unopened and supplied no selection, protocol or inference evidence.']}
    stable(); write_new(HERE/'confirmation-v1-result.json',record)
    print(json.dumps({k:v for k,v in record.items() if k not in ('pairScores','blocks','limits')},indent=2))


if __name__=='__main__':
    if len(sys.argv)>1 and sys.argv[1]=='--self-test': print(json.dumps(self_test()))
    else: main()
