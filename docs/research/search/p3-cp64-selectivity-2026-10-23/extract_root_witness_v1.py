"""Preserve the development move difference without equating unequal horizons."""
import json
from run_observation_v1 import HERE,digest,write_new

path=HERE/'raw/flat-null-n250k-v1.jsonl';rows=[json.loads(x) for x in path.read_text().splitlines()]
rows=[r for r in rows if r.get('type')=='request' and r['round']>=0]
baseline={(r['id'],r['round']):r for r in rows if r['profile']=='baseline'}
changed={r['id'] for r in rows if r['profile']=='nominal-verified-null' and r['outcome']['lastCompletedResult']['bestMove']!=baseline[r['id'],r['round']]['outcome']['lastCompletedResult']['bestMove']}
records=[]
for row in rows:
    if row['id'] not in changed:continue
    outcome=row['outcome'];result=outcome['lastCompletedResult']
    records.append({k:row[k] for k in ('id','round','profile','termination')}|{'requestNodes':outcome['nodes'],
        'attemptedDepth':outcome['attemptedDepth'],'lastCompleted':{k:result[k] for k in ('bestMove','score','depth','nodes','principalVariation','selective')}})
write_new(HERE/'fixed-node-move-witness-v1.json',{'source':str(path.relative_to(HERE)),'sourceSha256':digest(path),
    'selection':'Every measured root where the selected composition changes best move versus full atN250k; both repeated rounds and every compared profile retained.',
    'distinctRoots':sorted(changed),'records':records,
    'finding':'The two recorded composition move changes are repeated observations of played-01, with full completingD7 and compositionD9. This is an unequal-horizon selective comparison, not a demonstrated missed resource or correctness failure. Existing PV legality checks pass; no mathematical proof is claimed.',
    'nextQuestion':'Before independent composition confirmation, compare full/nominal/null/composition at matchedD7 andD9 on this exact full-history root with a bounded budget. Retain caps and same-depth differences; do not tune the candidate from this witness.'})
print('Preserved unequal-horizon move witness:',sorted(changed))
