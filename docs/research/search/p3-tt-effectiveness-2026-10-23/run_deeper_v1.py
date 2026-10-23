"""Frozen deeper-v1 development run using the existing owned-process executor."""
import ctypes
import importlib.util
import json
import os
from pathlib import Path
import sys
from run_diagnostic_v1 import digest, write_new, ROOT, HERE

spec=importlib.util.spec_from_file_location('owned',ROOT/'tools/tournament-process.py')
owned=importlib.util.module_from_spec(spec);spec.loader.exec_module(owned)
kernel=ctypes.WinDLL('kernel32',use_last_error=True)
kernel.GetCurrentProcess.restype=ctypes.c_void_p
kernel.SetPriorityClass.argtypes=[ctypes.c_void_p,ctypes.c_ulong]
if not kernel.SetPriorityClass(kernel.GetCurrentProcess(),0x4000):raise ctypes.WinError(ctypes.get_last_error())
protocol=json.loads((HERE/'deeper-v1.json').read_text())
prior=json.loads((HERE/protocol['runtimeBinding']).read_text())
runtime=HERE/protocol['runtime']
files={str(HERE/'deeper-v1.json'):digest(HERE/'deeper-v1.json'),str(Path(__file__)):digest(__file__),
       str(ROOT/protocol['corpus']):digest(ROOT/protocol['corpus']),str(ROOT/protocol['model']):digest(ROOT/protocol['model'])}
for name,sha in prior['files'].items():
    if '/raw/runtime-v1/' in name.replace('\\','/'):
        if digest(ROOT/name)!=sha:raise ValueError('Frozen runtime changed: '+name)
        files[str(ROOT/name)]=sha
write_new(HERE/'deeper-v1-binding.json',{'files':files,'executorSha256':digest(ROOT/'tools/tournament-process.py')})
for experiment in protocol['experiments']:
    name=experiment['name'];output=HERE/'raw'/f'{name}.jsonl';log=HERE/'raw'/f'{name}.log'
    if output.exists() or log.exists():raise ValueError('No replay or overwrite: '+name)
    args=[protocol['model'],protocol['corpus'],str(output),str(experiment['workers'])]
    args += [str(protocol[k]) for k in ('warmups','rounds','depth','nodeBudget','roots')]
    args += ['cost',protocol['profiles']]
    command=['C:/java/jdk-21/bin/java.exe',*protocol['jvm'],'-cp',str(runtime/'classes')+';'+str(runtime/'lib/*'),
             'com.ohinteractive.seedv6.search.exact.ConstructionTtStudy',*args]
    receipt={'command':command,'priority':'BelowNormal inherited from executor','status':'running'}
    try:
        with log.open('x',encoding='utf8') as stream:
            receipt.update(owned.run_owned(command,stream,1200,ROOT,os.environ.copy()))
        if receipt['exitCode']!=0 or json.loads(output.read_text().splitlines()[-1]).get('status')!='passed':
            raise RuntimeError('Incomplete deeper experiment: '+name)
        receipt['status']='passed'
    except BaseException as error:
        receipt.update(status='failed',failure=repr(error));raise
    finally:
        receipt['evidence']={str(p.relative_to(HERE)):digest(p) for p in (output,log) if p.exists()}
        write_new(HERE/f'{name}-execution.json',receipt)
    print(name,receipt['status'],receipt['wallSeconds'],flush=True)
