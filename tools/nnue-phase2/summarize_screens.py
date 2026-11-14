"""Lightweight synthesis of eight audited endpoints; exploratory shared-root contrasts."""
import json
import random
import statistics
from pathlib import Path
from bootstrap import BASE, write, sha
from run_screens import charged


def main():
    plan_path=BASE/'screen-plan.json';plan=json.loads(plan_path.read_text());endpoints={};pairs={}
    for candidate in plan['candidates']:
        name=candidate['id'];report=BASE/f'screen-{name}-results.json';out=BASE/f'raw/screen-{name}'
        result=json.loads(report.read_text());protocol=json.loads((out/'protocol.json').read_text())
        assert result['games']==result['completed']==64 and result['censored']==0
        assert result['legal_replay_and_outcome_audit']==result['frozen_artifact_recheck']=='PASS'
        assert result['protocol_sha256']==sha(out/'protocol.json') and result['games_sha256']==sha(out/'games.jsonl')
        assert protocol['network_sha256']==candidate['sha256'] and protocol['openings_sha256']==plan['openings_sha256']
        assert protocol['opponent_network_sha256']==plan['incumbent_sha256']
        games=[json.loads(line) for line in (out/'games.jsonl').read_text().splitlines()]
        assert {(g['opening_index'],g['candidate_white']) for g in games}=={(i,c) for i in range(32) for c in (True,False)}
        values=[sum(g['score'] for g in games if g['opening_index']==i)/2 for i in range(32)]
        assert statistics.mean(values)==result['score'];pairs[name]=values
        endpoints[name]={k:result[k] for k in ('wins','draws','losses','score','elo','paired_bootstrap_95_elo')}
        endpoints[name].update(report=str(report),report_sha256=sha(report),paired_scores=values,
                               observed_match_seconds=charged(out))
    contrasts={};rng=random.Random(2026111506)
    for comparator in ('teacher','wdl50','narrow','late'):
        values=[a-b for a,b in zip(pairs['replica'],pairs[comparator])]
        samples=sorted(statistics.mean(rng.choices(values,k=32)) for _ in range(10000))
        contrasts[comparator]=dict(reference='replica',comparator=comparator,
            score_difference=statistics.mean(values),paired_bootstrap_95_score_difference=[samples[249],samples[9749]],
            paired_differences=values)
    result=dict(schema='seed-phase2-s001-synthesis-v1',plan_sha256=sha(plan_path),endpoints=endpoints,
        a001_conditional_score_contrasts=contrasts,total_games=512,total_censored=0,
        observed_match_seconds=sum(e['observed_match_seconds'] for e in endpoints.values()),
        controls='Same training initialization within A001, same32 opening pairs and WDL25 opponent,200ms/W1/Hash32/CLV_C1. Distinct trained trees and sequential runtime periods.',
        contrast_limits='Post-hoc descriptive paired bootstrap, not multiplicity-adjusted acceptance or direct head-to-head Elo. Conditional on one initialization/data order; data coverage also changes frequencies and passes. Independent confirmation remains unused.',
        conclusion='No established improvement over WDL25. Reproduction is the only positive point estimate; both256 architecture additions and the50% outcome recipe are uncompetitive in these screens. Proceed with the separately selected compact128 efficiency hypothesis.')
    write(BASE/'s001-summary.json',result)
    print(json.dumps(dict(endpoints={k:{f:v for f,v in e.items() if f not in ('paired_scores','report_sha256','report')} for k,e in endpoints.items()},
                          contrasts=contrasts,observed_match_seconds=result['observed_match_seconds']),indent=2))


if __name__=='__main__':main()
