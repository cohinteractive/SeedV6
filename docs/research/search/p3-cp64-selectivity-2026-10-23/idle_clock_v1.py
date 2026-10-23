"""New resource admission for the untouched, never-launched H1 clock protocol.

No failed or partial game is replayed. This wrapper preserves every admission,
uses the frozen original worker, and never relaxes the prospective host gate.
"""
import ctypes
import json
import sys
import time
from run_observation_v1 import HERE, ROOT, digest, write_new
from run_clock_v1 import module

attempt=4
name='clock-development-v1-idle4'
assert not (HERE/'raw/clock-development-v1').exists(), 'Never replay a started match'
previous=HERE/'clock-development-v1-resume3-admission.json'
assert not json.loads(previous.read_text())['passed']
k=ctypes.WinDLL('kernel32',use_last_error=True);k.GetCurrentProcess.restype=ctypes.c_void_p
k.SetPriorityClass.argtypes=[ctypes.c_void_p,ctypes.c_ulong];assert k.SetPriorityClass(k.GetCurrentProcess(),0x4000)
plan=json.loads((HERE/'clock-development-v1-binding.json').read_text())
def stable():
    for p,sha in plan['files'].items():assert digest(ROOT/p)==sha,p
    for p,sha in plan['externalFiles'].items():assert digest(p)==sha,p
stable()
host=module('host',ROOT/'tools/p3-tt-session.py');owned=module('owned',ROOT/'tools/tournament-process.py')
baseline=module('baseline',ROOT/'tools/search-baseline.py')
write_new(HERE/(name+'-binding.json'),{'originalBindingSha256':digest(HERE/'clock-development-v1-binding.json'),
    'priorFailedAdmissionSha256':digest(previous),'wrapperSha256':digest(__file__),
    'reason':'No prior games launched. Fully idle admission with no concurrent agent tool/edit activity during its30-second interval. Same source/configuration/openings/sample/analysis and1.0-core gate; this tests an activity-context hypothesis without asserting causation or relaxing any limit.'})
start=host.host_sample();time.sleep(30);end=host.host_sample()
admission=host.host_interval(start,end);write_new(HERE/(name+'-admission.json'),admission)
if not admission['passed']:raise SystemExit('Resource admission failed; no games launched')
start=host.host_sample();receipt={'status':'running','bindingSha256':digest(HERE/(name+'-binding.json'))}
write_new(HERE/(name+'-launch.json'),receipt|{'hostStart':start})
try:
    with (HERE/'raw'/(name+'-worker.log')).open('xb') as log:
        receipt['execution']=owned.run_owned([sys.executable,'-B',str(HERE/'run_clock_v1.py'),'worker'],log,5500,ROOT,baseline.clean_environment())
    end=host.host_sample();receipt['resourceQualification']=host.host_interval(start,end,receipt['execution']['totalOwnedCpuSeconds'])
    assert receipt['execution']['exitCode']==0
    result=json.loads((HERE/'raw/clock-development-v1/run.json').read_text());assert result['status']=='passed';stable()
    receipt['results']=result['results'];receipt['matchWallSeconds']=result['matchWallSeconds']
    receipt['status']='passed' if receipt['resourceQualification']['passed'] else 'resource-unqualified'
    receipt['evidence']={str(p.relative_to(HERE)):digest(p) for p in sorted((HERE/'raw/clock-development-v1').iterdir()) if p.is_file()}
except BaseException as e:receipt.update(status='failed',failure=repr(e));raise
finally:write_new(HERE/(name+'-summary.json'),receipt)
print(json.dumps({'status':receipt['status'],'results':{k:v for k,v in receipt['results'].items() if k!='games'}},indent=2))
