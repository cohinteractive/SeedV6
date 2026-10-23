"""Frozen two-profile diagnostics-OFF study; reuses existing process ownership."""
import ctypes
import importlib.util
import json
import os
import shutil
import subprocess
from run_diagnostic_v1 import digest, write_new, ROOT, HERE

assert json.loads((HERE/'lifetime-full-v1-execution.json').read_text())['status']=='passed'
assert json.loads((HERE/'adaptive-validation-v1.json').read_text())['status']=='passed'
spec=importlib.util.spec_from_file_location('owned',ROOT/'tools/tournament-process.py')
owned=importlib.util.module_from_spec(spec);spec.loader.exec_module(owned)
kernel=ctypes.WinDLL('kernel32',use_last_error=True);kernel.GetCurrentProcess.restype=ctypes.c_void_p
kernel.SetPriorityClass.argtypes=[ctypes.c_void_p,ctypes.c_ulong]
if not kernel.SetPriorityClass(kernel.GetCurrentProcess(),0x4000):raise ctypes.WinError(ctypes.get_last_error())
protocol=json.loads((HERE/'adaptive-v1.json').read_text());runtime=HERE/'raw/adaptive-v1-runtime'
shutil.copytree(ROOT/'build/research/runtime',runtime)
files=[HERE/'adaptive-v1.json',HERE/'adaptive-validation-v1.json',ROOT/protocol['model'],
       ROOT/'research/src/main/java/com/ohinteractive/seedv6/search/exact/ConstructionTtAdaptiveStudy.java',
       ROOT/'research/src/test/java/com/ohinteractive/seedv6/search/exact/ConstructionTtAdaptiveTest.java']
files+=sorted({ROOT/e['corpus'] for e in protocol['experiments']})
files+=sorted(p for p in runtime.rglob('*') if p.is_file())
binding={'sourceHead':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),
         'worktree':subprocess.check_output(['git','status','--short'],cwd=ROOT,text=True),
         'files':{str(p.relative_to(ROOT)):digest(p) for p in files},'runnerSha256':digest(__file__),
         'executorSha256':digest(owned.__file__),'memory':'Per-request JVM heap snapshots and fixed heap bounds; no native peak-working-set capture in this executor. Clock/lifetime native peaks are separate domains.'}
write_new(HERE/'adaptive-v1-binding.json',binding)
for experiment in protocol['experiments']:
    name=experiment['name'];output=HERE/'raw'/f'{name}.jsonl';log=HERE/'raw'/f'{name}.log'
    assert not output.exists() and not log.exists()
    command=['C:/java/jdk-21/bin/java.exe','--add-modules=jdk.incubator.vector','-Djava.awt.headless=true',
        '-Xms256m','-Xmx768m','-XX:ActiveProcessorCount=4','-cp',str(runtime/'classes')+';'+str(runtime/'lib/*'),
        'com.ohinteractive.seedv6.search.exact.ConstructionTtAdaptiveStudy',protocol['model'],experiment['corpus'],str(output),
        str(experiment['workers']),str(protocol['warmups']),str(protocol['rounds']),str(experiment['depth']),
        str(experiment['nodes']),str(protocol['roots'])]
    receipt={'status':'running','command':command,'priority':'BelowNormal inherited','bindingSha256':digest(HERE/'adaptive-v1-binding.json')}
    try:
        with log.open('x',encoding='utf8') as stream:receipt.update(owned.run_owned(command,stream,1200,ROOT,os.environ.copy()))
        assert receipt['exitCode']==0 and json.loads(output.read_text().splitlines()[-1])['status']=='passed'
        assert all(digest(ROOT/p)==sha for p,sha in binding['files'].items())
        receipt['status']='passed'
    except BaseException as error:
        receipt.update(status='failed',failure=repr(error));raise
    finally:
        receipt['evidence']={str(p.relative_to(HERE)):digest(p) for p in (output,log) if p.exists()}
        write_new(HERE/f'{name}-execution.json',receipt)
    print(name,'passed',receipt['wallSeconds'],flush=True)
