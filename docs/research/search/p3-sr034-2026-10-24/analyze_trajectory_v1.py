"""Describe frozen repeated trajectories; deeper choices are not quality ground truth."""
from collections import defaultdict
import json
import statistics as st
from run_trajectory_v1 import HERE, ROOT, digest, write_new

def distribution(values):
    if not values:return {'n':0}
    values=sorted(values)
    def q(p):return values[min(len(values)-1,int(p*(len(values)-1)))]
    return {'n':len(values),'min':values[0],'p10':q(.1),'median':st.median(values),'p90':q(.9),'p95':q(.95),'max':values[-1]}

def main():
    plan=json.loads((HERE/'trajectory-v1.json').read_text())
    binding=json.loads((HERE/'trajectory-v1-binding.json').read_text())
    for path,sha in binding['files'].items():assert digest(ROOT/path)==sha,path
    groups=defaultdict(list); costs=[]; inputs={}; executions=[]
    for experiment in plan['experiments']:
        name=experiment['name'];path=HERE/'raw'/f'{name}.jsonl'
        receipt=json.loads((HERE/f'{name}-execution.json').read_text());assert receipt['status']=='passed'
        inputs[str(path.relative_to(HERE))]=digest(path);executions.append(receipt)
        rows=[json.loads(s) for s in path.read_text().splitlines()];assert rows[-1]['status']=='passed'
        for row in rows:
            if row.get('type')!='trajectory':continue
            groups[(name,row['id'])].append(row)
            costs.append({'ratio':row['observed']['elapsedNanos']/row['plain']['elapsedNanos'],
                          'cleanCpu':row['plain']['processCpuNanos'],'cleanElapsed':row['plain']['elapsedNanos'],
                          'observedElapsed':row['observed']['elapsedNanos']})
    data=[]; repeats=0; unused=[]; all_depths=[]; tails=[]; comparisons=[]
    counterfactual=defaultdict(list)
    for key,rows in groups.items():
        assert len(rows)==plan['rounds']
        seqs=[row['observed']['iterations'] for row in rows]
        signatures=[[s['iteration']['result'] for s in seq] for seq in seqs]
        assert all(x==signatures[0] for x in signatures),('Repeated deterministic work changed',key)
        repeats+=1
        s=signatures[0];all_depths.append(s[-1]['depth'] if s else 0)
        for row in rows:
            outcome=row['observed']['outcome'];last=outcome.get('lastCompletedResult')
            unused.append({'nodes':outcome['nodes']-(last['nodes'] if last else 0),
                           'fraction':1-(last['nodes'] if last else 0)/max(1,outcome['nodes'])})
        # Use repeat medians for time and deterministic nodes, retaining individual cost ratios separately.
        node_cost=[r['nodes']-(s[i-1]['nodes'] if i else 0) for i,r in enumerate(s)]
        elapsed=[[p['observedNanos']-(seq[i-1]['observedNanos'] if i else 0) for i,p in enumerate(seq)] for seq in seqs]
        time_cost=[st.median(e[i] for e in elapsed) for i in range(len(s))]
        for i,r in enumerate(s):
            if i<2 or i+1>=len(s) or not r['hasMove']:continue
            # TranspositionScores.MATE_THRESHOLD = 32768 - 256; never transfer HCE thresholds.
            if any(abs(s[j]['score'])>=32512 for j in range(i-2,i+2)):continue
            stable=len({s[j]['bestMove'] for j in range(i-2,i+1)})==1
            node_ratio=node_cost[i+1]/max(1,node_cost[i]);time_ratio=time_cost[i+1]/max(1,time_cost[i])
            previous=s[i-1]['diagnostics']['iteration'];current=r['diagnostics']['iteration']
            retries=(current['failLowResearches']+current['failHighResearches']
                     -previous['failLowResearches']-previous['failHighResearches'])
            parity_nodes=node_cost[i-1]*node_cost[i]/max(1,node_cost[i-2])
            parity_time=time_cost[i-1]*time_cost[i]/max(1,time_cost[i-2])
            x={'root':list(key),'depth':r['depth'],'stable3':stable,'selective':r['selective'],
               'changedNow':r['bestMove']!=s[i-1]['bestMove'],'scoreDelta':r['score']-s[i-1]['score'],
               'scoreRange3':max(s[j]['score'] for j in range(i-2,i+1))-min(s[j]['score'] for j in range(i-2,i+1)),
               'retries':retries,'nextChanged':r['bestMove']!=s[i+1]['bestMove'],
               'twoLaterChanged':None if i+2>=len(s) else r['bestMove']!=s[i+2]['bestMove'],
               'nextNodeRatio':node_ratio,'nextTimeRatio':time_ratio,
               'parityNodePredictedActual':parity_nodes/max(1,node_cost[i+1]),
               'parityTimePredictedActual':parity_time/max(1,time_cost[i+1]),
               'lastCostOverpredictsNext':time_ratio<1,
               'halfLastCostOverpredictsNext':time_ratio<.5}
            data.append(x)
            if node_ratio<.5 or node_ratio>10:tails.append(x)
        for row in rows:
            seq=row['observed']['iterations']
            for budget_ms in (50,100,200,400):
                budget_ns=budget_ms*1000000
                # Need later observed work covering the hypothetical deadline; otherwise censored.
                if not seq or seq[-1]['observedNanos']<budget_ns:continue
                available=[(j,p) for j,p in enumerate(seq) if p['observedNanos']<budget_ns]
                if not available:continue
                baseline=available[-1][1]['iteration']['result']
                for policy in ('half','stable3-half','half-last-forecast'):
                    stop=None
                    for j,p in available:
                        r=p['iteration']['result']
                        if r['depth']<6 or j<2 or not r['hasMove']:continue
                        if any(abs(seq[k]['iteration']['result']['score'])>=32512 for k in range(j-2,j+1)):continue
                        stable=len({seq[k]['iteration']['result']['bestMove'] for k in range(j-2,j+1)})==1
                        eligible=(p['observedNanos']>=budget_ns/2 and (policy=='half' or (policy=='stable3-half' and stable)))
                        if policy=='half-last-forecast':
                            eligible=(p['observedNanos']-seq[j-1]['observedNanos'])/2>budget_ns-p['observedNanos']
                        if eligible:stop=p;break
                    counterfactual[(budget_ms,policy)].append({'stopped':stop is not None,
                        'savedFraction':0 if stop is None else 1-stop['observedNanos']/budget_ns,
                        'differentMove':False if stop is None else stop['iteration']['result']['bestMove']!=baseline['bestMove'],
                        'lostCompletedDepth':0 if stop is None else baseline['depth']-stop['iteration']['result']['depth']})
            for i in range(2,len(seq)-1):
                if node_cost[i]==0 or node_cost[i+1]==0:continue
                comparisons.append((seq[i+1]['observedNanos']-seq[i]['observedNanos'])/
                                   max(1,seq[i]['observedNanos']-seq[i-1]['observedNanos']))
    tables=[]
    conditions={'all':lambda x:True,'stable3':lambda x:x['stable3'],'changedNow':lambda x:x['changedNow'],
                'scoreFell':lambda x:x['scoreDelta']<0,'noFallStable3':lambda x:x['stable3'] and x['scoreDelta']>=0,
                'retries':lambda x:x['retries']>0,'noRetriesStable3':lambda x:x['stable3'] and x['retries']==0}
    for low,high in ((3,5),(6,8),(9,12)):
        for name,predicate in conditions.items():
            rows=[x for x in data if low<=x['depth']<=high and predicate(x)]
            later=[x for x in rows if x['twoLaterChanged'] is not None]
            tables.append({'depthRange':[low,high],'condition':name,'n':len(rows),
                'nextMoveChanges':sum(x['nextChanged'] for x in rows),
                'twoLaterN':len(later),'twoLaterChanges':sum(x['twoLaterChanged'] for x in later),
                'scoreRange3':distribution([x['scoreRange3'] for x in rows]),
                'nextNodeRatio':distribution([x['nextNodeRatio'] for x in rows]),
                'nextTimeRatio':distribution([x['nextTimeRatio'] for x in rows])})
    qualification=[]
    for e in executions:
        life=e['nativeLifetime']
        qualification.append({'nativeLifetime':life,'hostStart':e['hostStart'],'hostEnd':e['hostEnd'],
                              'peakWorkingSetBytes':e['peakWorkingSetBytes'],'wallSeconds':e['wallSeconds']})
    result={'status':'passed','inputs':inputs,'uniqueRoots':len(groups),'repeatStableRoots':repeats,
        'pairedRequests':len(costs),'completedDepth':distribution(all_depths),'conditions':tables,
        'selectiveSamples':sum(x['selective'] for x in data),'ordinarySamples':len(data),
        'abandonedNodes':distribution([x['nodes'] for x in unused]),
        'abandonedFraction':distribution([x['fraction'] for x in unused]),
        'rawNextTimeRatio':distribution(comparisons),
        'parityNodePredictedActual':distribution([x['parityNodePredictedActual'] for x in data]),
        'parityTimePredictedActual':distribution([x['parityTimePredictedActual'] for x in data]),
        'parityFormula':'cost[d-1] * cost[d] / cost[d-2], predicts cost[d+1]; descriptive, not a lower bound',
        'lastCostOverpredictsNext':sum(x['lastCostOverpredictsNext'] for x in data),
        'halfLastCostOverpredictsNext':sum(x['halfLastCostOverpredictsNext'] for x in data),
        'observedCleanElapsedRatio':distribution([x['ratio'] for x in costs]),
        'cleanTotalElapsedNanos':sum(x['cleanElapsed'] for x in costs),
        'observedTotalElapsedNanos':sum(x['observedElapsed'] for x in costs),
        'cleanTotalProcessCpuNanos':sum(x['cleanCpu'] for x in costs),'resourceReceipts':qualification,
        'nodeGrowthTailWitnesses':tails,
        'counterfactualStops':[{'budgetMillis':key[0],'policy':key[1],'eligibleTrajectories':len(rows),
            'stops':sum(x['stopped'] for x in rows),'differentMoves':sum(x['differentMove'] for x in rows),
            'savedFraction':distribution([x['savedFraction'] for x in rows]),
            'lostCompletedDepth':distribution([x['lostCompletedDepth'] for x in rows])}
            for key,rows in sorted(counterfactual.items())],
        'limitations':['32 historical-development roots are not representative independent games.',
          'Adjacent depths and repeated roots are dependent; no inferential p-values.',
          'Resolved selective scores are heuristic evidence, not nominal proof or truth.',
          'Deeper move changes are not automatically improvements; quality requires separate references/games.',
          'One-million-node ceiling censors larger next iterations; forecast tails underrepresent extreme growth.',
          'Wall times include observational overhead and host/JIT/GC variation; node evidence is separate.',
          'No strength, clock reliability or multiworker claim.']}
    write_new(HERE/'trajectory-v1-summary.json',result)
    print(json.dumps({k:result[k] for k in ('status','uniqueRoots','pairedRequests','completedDepth','abandonedFraction',
                     'lastCostOverpredictsNext','halfLastCostOverpredictsNext','observedCleanElapsedRatio')}))
    for row in tables:print(json.dumps(row))

if __name__=='__main__':main()
