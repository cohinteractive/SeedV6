"""Narrow continuation of the four frozen studies; no new candidate or protocol.

Waits15min after failed resource admission, resumes only complete audited blocks,
and never reads interim strength outcomes. A partial/resource-invalid block raises
and ends the controller. Run again only after inspecting durable failure evidence.
"""
import ctypes
from datetime import datetime,timedelta,timezone
import importlib.util
import json
import time
from run_diagnostic_v1 import digest, write_new, ROOT, HERE
from assess_tt_stage_v1 import assess

spec=importlib.util.spec_from_file_location('lock_session',HERE/'raw/tt-session-v1-tools/tournament-session.py')
lock_session=importlib.util.module_from_spec(spec);spec.loader.exec_module(lock_session)
# OS releases this exclusive controller lock even on process death. Preserve the inert file.
controller_lock=lock_session.locked(HERE/'raw/confirmation-controller-v1.lock')
controller_lock.__enter__()
binding_path=HERE/'confirmation-controller-v1-binding.json'
files={str(p.relative_to(ROOT)):digest(p) for p in (
    HERE/'continue_confirmation_v1.py',HERE/'assess_tt_stage_v1.py',
    HERE/'adaptive-confirmation-v1.md',HERE/'tt-session-v1-binding.json')}
if binding_path.exists():assert json.loads(binding_path.read_text())['files']==files
else:write_new(binding_path,{'files':files,'createdUtc':datetime.now(timezone.utc).isoformat()})
binding=json.loads((HERE/'tt-session-v1-binding.json').read_text())
assert binding['protocolSha256']==digest(HERE/'adaptive-confirmation-v1.md')
kernel=ctypes.WinDLL('kernel32',use_last_error=True)
kernel.GetCurrentProcess.restype=ctypes.c_void_p
kernel.SetPriorityClass.argtypes=[ctypes.c_void_p,ctypes.c_ulong]
if not kernel.SetPriorityClass(kernel.GetCurrentProcess(),0x4000):raise ctypes.WinError(ctypes.get_last_error())
active_seconds=0
for label in ('pilot','w1','requal-w4','w4'):
    if label=='w4':
        screen=json.loads((HERE/'tt-session-requal-w4-v1-summary.json').read_text())
        if not screen['eligibleForConfirmation']:
            print('W4 confirmation not selected by its frozen new resource-qualified screen.',flush=True)
            break
    if label!='pilot':
        assert json.loads((HERE/'tt-session-pilot-v1-summary.json').read_text())['status']=='passed'
    entry=binding['studies'][label];plan_path=HERE/entry['plan']
    assert digest(plan_path)==entry['planSha256']
    spec=importlib.util.spec_from_file_location('frozen_session',plan_path.parent/'inputs/tools/tournament-session.py')
    s=importlib.util.module_from_spec(spec);spec.loader.exec_module(s)
    while True:
        assert all(digest(ROOT/p)==sha for p,sha in files.items())
        with s.locked(plan_path.parent/'execution.lock'):
            plan=s.verify(plan_path);done=s.reconcile(plan_path,plan)
        if len(done)==len(plan['blocks']):break
        if (plan_path.parent/'PAUSE').exists():
            print(label,'explicit PAUSE present; controller exits without clearing it',flush=True)
            raise SystemExit(0)
        if active_seconds+plan['blockLimitSeconds']>6*3600:
            print('Six-hour active-work window reserved capacity exhausted; audited block boundary.',flush=True)
            raise SystemExit(0)
        print(label,'resource admission for block',len(done),'at',datetime.now(timezone.utc).isoformat(),flush=True)
        started=time.monotonic()
        result=s.execute(plan_path,(datetime.now(timezone.utc)+timedelta(seconds=plan['blockLimitSeconds']+600)).isoformat(),max_blocks=1)
        if result['cumulativePairs']>result['initialPairs']:
            active_seconds+=time.monotonic()-started
            print(label,'completed operational pairs',result['cumulativePairs'],'of',plan['pairs'],flush=True)
        else:
            if (plan_path.parent/'PAUSE').exists():
                print(label,'explicit PAUSE received; exiting',flush=True)
                raise SystemExit(0)
            print(label,'host unavailable; waiting15min, no block launched',flush=True)
            time.sleep(900)
    report=assess(label,s,plan_path)
    print(label,'endpoint:',report['decision'],flush=True)
print('All admitted frozen stages complete; owner reconciliation remains separate.',flush=True)
