"""Sequential bounded execution of a content-bound research plan; never overwrite runs."""
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import time

assert len(sys.argv)==3,'PLAN EXPECTED_SHA256'
path=Path(sys.argv[1]);expected=sys.argv[2]
def read(p):return json.loads(Path(p).read_text(encoding='utf-8'))
def sha(p):
    with Path(p).open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
assert sha(path)==expected
plan=read(path)
for i,spec in enumerate(plan['runs'],1):
    assert sha(path)==expected
    for p,h in plan['frozenFiles'].items():assert sha(p)==h,'Frozen input changed: '+p
    for kind,files in plan['frozenCode'].items():
        for p,h in files.items():assert sha(p)==h,'Frozen code changed: '+p
    run=Path(spec['run']);receipt=Path(str(run)+'.execution.json')
    if not receipt.exists():
        assert not run.exists(),'Unaccounted output without receipt'
        command=[sys.executable,'-B','tools/brn-architecture.py','--timeout',str(plan['timeoutSeconds']),
                 '--receipt',str(receipt),spec['mode'],*spec['arguments']]
        print(f'START {i}/{len(plan["runs"])} {run.name}',flush=True)
        child=subprocess.Popen(command);start=time.monotonic()
        while True:
            try:code=child.wait(timeout=30);break
            except subprocess.TimeoutExpired:print(f'ACTIVE {i}/{len(plan["runs"])} elapsed={time.monotonic()-start:.0f}s',flush=True)
        assert code==0,'Execution failed; preserve evidence and investigate'
    execution=read(receipt);result=read(run/'result.json')
    assert execution['status']=='completed' and execution['exitCode']==0
    for kind,files in plan['frozenCode'].items():assert execution[kind]==files,'Code inventory or identity changed'
    args=([spec['mode']] if spec['mode'] in ['train','match','measure'] else [])+spec['arguments']
    assert result['arguments']==args and execution['command'][-len(args):]==args
    if spec['mode']=='tuple-compile':assert result['complete'] and result['parityPassed']
    print(f'COMPLETE {i}/{len(plan["runs"])} {run.name}',flush=True)
print('ALL PLANNED EXECUTIONS COMPLETE; RUN SEMANTIC AUDIT BEFORE INTERPRETATION',flush=True)
