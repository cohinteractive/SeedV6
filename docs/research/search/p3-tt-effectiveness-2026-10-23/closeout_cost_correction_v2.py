"""Add the six directly owned Java jobs omitted by v1's nativeLifetime-only sum."""
import json
from pathlib import Path
from run_diagnostic_v1 import digest, write_new, HERE

report=json.loads((HERE/'closeout-v1.json').read_text())
report['corrects']={'source':'closeout-v1.json','sha256':digest(HERE/'closeout-v1.json'),
    'scope':'Diagnostic resource totals only. v1 verified these receipts/evidence but omitted their Windows Job CPU/wall fields. Clock counts/costs and all experimental outcomes are unchanged.'}
added=[]
for name,sha in report['executionReceipts'].items():
    path=HERE/name;assert digest(path)==sha
    receipt=json.loads(path.read_text())
    if 'nativeLifetime' not in receipt and Path(receipt['command'][0]).name.lower()=='java.exe':
        assert receipt['status']=='passed' and receipt['exitCode']==0
        assert receipt['totalOwnedProcesses']==1 and receipt['ownedProcessesRemaining']==0
        report['diagnosticJvmWallSeconds']+=receipt['wallSeconds']
        report['diagnosticJvmCpuSeconds']+=receipt['totalOwnedCpuSeconds']
        added.append(name)
assert len(added)==6
report['corrects']['addedReceipts']=added
report['correctionScriptSha256']=digest(__file__)
write_new(HERE/'closeout-v2.json',report)
print(json.dumps({k:report[k] for k in ('status','clockGamesAudited','clockMatchWallSeconds',
    'clockEngineCpuSeconds','diagnosticJvmWallSeconds','diagnosticJvmCpuSeconds','corrects')},indent=2))
