"""Descriptive matched-opening reverse ablation after both development matches close."""
import json
import math
import statistics as st
from pathlib import Path
from run_trajectory_v1 import HERE, digest, write_new


def main():
    means = {}; evidence = {}
    for profile in ('unstable2','boundary2'):
        stage = 'spending-development-'+profile+'-v1'
        path = HERE/(stage+'-summary.json'); receipt = json.loads(path.read_text()); assert receipt['status'] == 'passed'
        for p, sha in receipt['evidence'].items(): assert digest(HERE/p) == sha, p
        run = json.loads((HERE/'raw'/stage/'run.json').read_text()); assert run['status'] == 'passed'
        scores = [x['candidateScore'] for x in run['results']['games']]; assert len(scores) == 64
        means[profile] = [(scores[i]+scores[i+1])/2 for i in range(0,64,2)]
        evidence[path.name] = digest(path)
    differences = [b-u for b,u in zip(means['boundary2'],means['unstable2'])]
    mean = st.fmean(differences); se = math.sqrt(st.variance(differences)/32)
    normal = [max(-1,mean-st.NormalDist().inv_cdf(.975)*se),min(1,mean+st.NormalDist().inv_cdf(.975)*se)]
    radius = 2*math.sqrt(math.log(40)/(2*32))
    write_new(HERE/'spending-reverse-ablation-v1.json', {'status':'passed','pairs':32,'evidence':evidence,
        'scriptSha256':digest(Path(__file__)),'pairMeans':means,'boundaryMinusUnstableScore':mean,
        'pairedNormal95ScoreDifference':normal,'boundedPair95ScoreDifference':[max(-1,mean-radius),min(1,mean+radius)],
        'limits':['Descriptive development comparison against separate baseline games on the same32 openings; not a direct head-to-head Elo estimate.',
            'Complete-policy effects and environmental/game-path differences remain; no independent component confirmation or causally valuable move labels.',
            'Both intervals assume representative independent opening pairs; no correction can establish absent systematic bias.']})
    print(json.dumps({'boundaryMinusUnstableScore':mean,'pairedNormal95ScoreDifference':normal}))


if __name__ == '__main__': main()
