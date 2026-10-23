"""Validate the new analysis and input schema before independent play."""
import ast,json,subprocess,sys
from run_observation_v1 import HERE,ROOT,digest,write_new

names=['run_root_witness_v1.py','prepare_confirmation_v1.py','run_confirmation_block_v1.py','analyze_confirmation_v1.py']
for name in names:ast.parse((HERE/name).read_text())
plan=json.loads((HERE/'confirmation-v1.json').read_text());d=plan['domain'];a=plan['analysis']
assert d['pairsPerHypothesis']==d['blocks']*d['pairsPerBlock']==128
assert d['blocks']==4 and d['pairsPerBlock']==32 and d['workers']==d['concurrentGames']==1 and d['clock']=='10+0.1'
assert a['alphaPerHypothesis']==.025 and a['familywiseAlpha']==2*a['alphaPerHypothesis']==.05
log=HERE/'raw/confirmation-statistics-validation-v1.log'
command=[sys.executable,'-B',str(HERE/'analyze_confirmation_v1.py'),'--self-test']
with log.open('xb') as stream:result=subprocess.run(command,cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT,timeout=30)
record={'status':'passed' if result.returncode==0 else 'failed','command':command,'exitCode':result.returncode,
    'checks':['Four script syntax checks','Protocol block/sample/domain/alpha consistency','Closed-form pair variance/standard error',
              '97.5% normal quantile','Permutation invariance','Zero-variance refusal','Elo transform'],
    'files':{name:digest(HERE/name) for name in names+['confirmation-v1.json']},'log':str(log.relative_to(HERE)),'logSha256':digest(log)}
write_new(HERE/'confirmation-statistics-validation-v1.json',record)
print(json.dumps(record,indent=2));raise SystemExit(result.returncode)
