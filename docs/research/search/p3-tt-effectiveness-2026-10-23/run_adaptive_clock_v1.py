"""Run the two frozen domains serially; block on owned process completion."""
import ctypes
import importlib.util
import json
import os
import sys
from datetime import datetime, timezone
from run_diagnostic_v1 import digest, write_new, ROOT, HERE

spec=importlib.util.spec_from_file_location('owned',ROOT/'tools/tournament-process.py')
owned=importlib.util.module_from_spec(spec);spec.loader.exec_module(owned)
kernel=ctypes.WinDLL('kernel32',use_last_error=True)
kernel.GetCurrentProcess.restype=ctypes.c_void_p
kernel.SetPriorityClass.argtypes=[ctypes.c_void_p,ctypes.c_ulong]
if not kernel.SetPriorityClass(kernel.GetCurrentProcess(),0x4000):raise ctypes.WinError(ctypes.get_last_error())
binding=json.loads((HERE/'adaptive-clock-v1-binding.json').read_text())
protocol=json.loads((HERE/'adaptive-clock-v1.json').read_text())
assert all(digest(p)==sha for p,sha in binding['files'].items())
runtime=HERE/protocol['runtime']
for experiment in protocol['experiments']:
    name=experiment['name'];workers=experiment['workers'];log=HERE/'raw'/f'{name}-adapter.log'
    output=HERE/'raw'/name;receiptPath=HERE/f'{name}-execution.json'
    if log.exists() or output.exists() or receiptPath.exists():raise ValueError('No replay or overwrite: '+name)
    command=[sys.executable,'-B',str(ROOT/'tools/p3-tt-match.py'),
             '--baseline',str(runtime),'--candidate',str(runtime),
             '--network',str(ROOT/'build-assets/nnue-candidate/conventional-cp64.cnnue'),
             '--runner',str(ROOT/'build/research/tools/fastchess.exe'),
             '--openings',str(HERE/protocol['openings']),'--out',str(output),
             '--baseline-workers',str(workers),'--candidate-workers',str(workers),
             '--baseline-policy','baseline','--candidate-policy','adaptive',
             '--pairs',str(protocol['pairsPerDomain']),'--tc',protocol['clock'],'--hash-mib','64',
             '--seed',str(protocol['seed']),'--timeout','5400','--concurrency','1','--background']
    receipt={'status':'running','command':command,'startedUtc':datetime.now(timezone.utc).isoformat(),
             'protocolSha256':digest(HERE/'adaptive-clock-v1.json'),'executorSha256':digest(__file__),
             'bindingSha256':digest(HERE/'adaptive-clock-v1-binding.json')}
    print(name,'started',receipt['startedUtc'],flush=True)
    try:
        with log.open('x',encoding='utf8') as stream:receipt.update(owned.run_owned(command,stream,5700,ROOT,os.environ.copy()))
        assert receipt['exitCode']==0
        assert json.loads((output/'run.json').read_text())['status']=='passed'
        assert all(digest(p)==sha for p,sha in binding['files'].items())
        receipt['status']='passed'
    except BaseException as error:
        receipt.update(status='failed',failure=repr(error));raise
    finally:
        receipt['finishedUtc']=datetime.now(timezone.utc).isoformat()
        receipt['evidence']={str(p.relative_to(HERE)):digest(p) for p in [log,output/'run.json',output/'process-cost.json',output/'games.pgn'] if p.exists()}
        write_new(receiptPath,receipt)
    print(name,'passed',receipt['wallSeconds'],flush=True)
