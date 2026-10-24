"""Frozen new-study identity and evidence checks; historical outcomes stay sealed."""
import json
from pathlib import Path
from verify_inputs import HERE, OLD, audit, digest


def stable():
    path=HERE/'confirmation-v1-binding.json'; binding=json.loads(path.read_text())
    for name,sha in binding['files'].items(): assert digest(name)==sha,name
    for runtime,paths in binding['runtimeInventory'].items():
        assert sorted(str(p.resolve()) for p in (OLD/'raw'/runtime).rglob('*') if p.is_file())==paths
    audit()
    return binding


def reference(path):
    path=Path(path)
    return {'path':str(path.relative_to(HERE)),'sha256':digest(path)}


def read_reference(ref):
    path=HERE/ref['path']; assert digest(path)==ref['sha256'],path
    return json.loads(path.read_text())


def evidence(record):
    for path,sha in record['evidence'].items(): assert digest(HERE/path)==sha,path


def qualified_block(name):
    assert name.startswith('boundary2-fresh-v1-b')
    path=HERE/(name+'-summary.json'); value=json.loads(path.read_text())
    assert value['status']=='passed' and value['bindingSha256']==digest(HERE/'confirmation-v1-binding.json')
    assert value['execution']['exitCode']==0 and value['execution']['ownedProcessesRemaining']==0
    evidence(value)
    for key in ('preDelivery','postDelivery'):
        delivery=read_reference(value[key]); assert delivery['status']=='passed' and delivery['criteriaPassed']
        evidence(delivery)
    clock=read_reference(value['clockAudit']); assert clock['clockReliabilityPassed'] and clock['completeClockPassed']
    assert value['resourceQualification']['passed'] and value['actorDelivery']['passed']
    return value,clock
