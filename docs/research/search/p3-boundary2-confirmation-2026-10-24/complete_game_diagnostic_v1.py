"""Separate validation endpoint after the preserved post-capture import failure.

No game replay, original receipt rewrite, threshold change or score analysis.
"""
import json
import sys
from pathlib import Path
from verify_inputs import HERE, OLD, audit, digest, write_new
from run_diagnostic_v1 import module


def main(name):
    original=HERE/(name+'-summary.json'); record=json.loads(original.read_text())
    assert record['status']=='failed' and record['failure']=="ModuleNotFoundError(\"No module named 'run_trajectory_v1'\")"
    assert record['execution']['exitCode']==0 and record['execution']['ownedProcessesRemaining']==0
    assert record['integrity']['terminations']=={'normal':64}
    for path,sha in record['evidence'].items(): assert digest(HERE/path)==sha,path
    binding_path=HERE/(name+'-binding.json'); assert digest(binding_path)==record['bindingSha256']
    binding=json.loads(binding_path.read_text())
    for path,sha in binding['files'].items(): assert digest(path)==sha,path
    for runtime,paths in binding['runtimeInventory'].items():
        assert sorted(str(p.resolve()) for p in (OLD/'raw'/runtime).rglob('*') if p.is_file())==paths
    audit()
    sys.path.insert(0,str(OLD))
    clock_module=module('clock',OLD/'audit_confirmation_clock_v1.py'); captured=[]
    clock_module.write_new=lambda path,value:captured.append(value)
    clock=clock_module.audit(str((HERE/'raw'/name/'match').resolve())); assert len(captured)==1
    clock['context']='New outcome-blind development-opening diagnostic; historical helper reused solely for arithmetic/accounting. No strength inference.'
    clock_path=HERE/(name+'-recovered-clock-audit.json'); write_new(clock_path,clock)
    record['originalWrapperFailure']={'path':str(original.relative_to(HERE)),'sha256':digest(original),'failure':record.pop('failure')}
    record['clockReliability']={k:clock[k] for k in ('clockReliabilityPassed','maximumRequestOverHardMillis')}
    record['clockAuditSha256']=digest(clock_path)
    record['completionAdapterSha256']=digest(__file__)
    record['status']='passed' if record['resources']['passed'] else 'resource-unqualified'
    if not clock['clockReliabilityPassed']: record['status']='clock-unqualified'
    record['scope']='Separate post-capture validation of the unchanged complete experiment; original failed wrapper remains failed and preserved.'
    write_new(HERE/(name+'-completion.json'),record)
    print(json.dumps({k:record[k] for k in ('status','execution','clockReliability','originalWrapperFailure')},indent=2))


if __name__=='__main__': main(sys.argv[1])
