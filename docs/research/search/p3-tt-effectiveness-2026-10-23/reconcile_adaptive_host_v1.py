"""Reconcile the closed development score gate with its independent host evidence."""
from datetime import datetime
import json
from run_diagnostic_v1 import digest, write_new, HERE

report={'supersedes':'Score-only eligibility labels in adaptive-clock-v1-summary.json; original outcomes remain unchanged.',
    'priorSummarySha256':digest(HERE/'adaptive-clock-v1-summary.json'),
    'protocolSha256':digest(HERE/'adaptive-clock-v1.json'),'domains':{}}
for workers in (1,4):
    start_path=HERE/f'raw/adaptive-clock-w{workers}-v1-host-start.json'
    end_path=HERE/f'raw/adaptive-clock-w{workers}-v1-host-end.json'
    start=json.loads(start_path.read_text());end=json.loads(end_path.read_text())
    seconds=(datetime.fromisoformat(end['host']['utc'])-datetime.fromisoformat(start['host']['utc'])).total_seconds()
    report['domains'][str(workers)]={'observedSeconds':seconds,
        'nonEngineAverageLogicalCores':end['otherBusyCpuSecondsInWindow']/seconds,
        'aggregateNonIdleFraction':end['aggregateNonIdleFraction'],
        'hostEvidence':{str(p.relative_to(HERE)):digest(p) for p in (start_path,end_path)},
        'disposition':('Eligible for prospective fresh confirmation, with observed desktop-load limits; not a confirmed gain.' if workers==1 else
                       'Score gate passes but resource qualification is incomplete. Not admitted to confirmation by the closed gate; fresh quiet-host development screen required.'),
        'limits':'Partial-window observation, not a complete process attribution or isolation guarantee. The new1-core gate is prospective and is not retroactively imposed on this closed screen.'}
write_new(HERE/'adaptive-clock-host-disposition-v1.json',report)
print(json.dumps(report,indent=2))
