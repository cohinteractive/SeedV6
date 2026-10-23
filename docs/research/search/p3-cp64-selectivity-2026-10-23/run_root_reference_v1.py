"""Compile only an independent-reference shim; preserve bounded witness evidence."""
import json,subprocess,time
from pathlib import Path
from run_observation_v1 import HERE,ROOT,digest,write_new
from run_clock_v1 import module

assert json.loads((HERE/'root-witness-v1-summary.json').read_text())['status']=='passed'
prior=json.loads((HERE/'flat-null-v1-binding.json').read_text())
for p,sha in prior['files'].items():assert digest(ROOT/p)==sha,p
runtime=HERE/'raw/runtime-v4';classes=HERE/'raw/root-reference-v1-classes';classes.mkdir(exist_ok=False)
source=ROOT/'research/src/main/java/com/ohinteractive/seedv6/search/exact/Cp64RootMoveReference.java'
command=['C:/java/jdk-21/bin/javac.exe','--add-modules','jdk.incubator.vector','-cp',str(runtime/'lib/*'),'-d',str(classes),str(source)]
compilelog=HERE/'raw/root-reference-v1-compile.log';start=time.monotonic()
with compilelog.open('xb') as stream:
    result=subprocess.run(command,cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT,timeout=120,
        creationflags=subprocess.BELOW_NORMAL_PRIORITY_CLASS|subprocess.CREATE_NO_WINDOW)
write_new(HERE/'root-reference-v1-compile.json',{'command':command,'exitCode':result.returncode,'wallSeconds':time.monotonic()-start,
    'log':str(compilelog.relative_to(HERE)),'logSha256':digest(compilelog),'testsRun':0})
assert result.returncode==0
files=[source,Path(__file__),HERE/'root-reference-v1.json',HERE/'root-witness-v1-summary.json',HERE/'root-witness-v1-corpus.tsv',HERE/'flat-null-v1-binding.json']
files+=sorted(p for p in classes.rglob('*') if p.is_file())
write_new(HERE/'root-reference-v1-binding.json',{'files':{str(p.relative_to(ROOT)):digest(p) for p in files},'runtimeBindingSha256':digest(HERE/'flat-null-v1-binding.json')})
output=HERE/'raw/root-reference-v1.jsonl';log=HERE/'raw/root-reference-v1.log'
command=['C:/java/jdk-21/bin/java.exe','--add-modules=jdk.incubator.vector','-Djava.awt.headless=true','-Xms256m','-Xmx1024m',
    '-XX:ActiveProcessorCount=4','-cp',str(classes)+';'+str(runtime/'classes')+';'+str(runtime/'lib/*'),
    'com.ohinteractive.seedv6.search.exact.Cp64RootMoveReference',str(ROOT/'build-assets/nnue-candidate/conventional-cp64.cnnue'),
    str(HERE/'root-witness-v1-corpus.tsv'),str(output)]
native=module('native',ROOT/'tools/sr036-process-cost.py');record={'status':'running','command':command};start=time.monotonic()
try:
    with log.open('xb') as stream:
        process=subprocess.Popen(command,cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT,
            creationflags=subprocess.BELOW_NORMAL_PRIORITY_CLASS|subprocess.CREATE_NO_WINDOW)
        try:record['exitCode']=process.wait(timeout=600)
        except BaseException:process.kill();process.wait();raise
        finally:record['nativeLifetime']=native.snapshot(int(process._handle))
    assert record['exitCode']==0
    rows=[json.loads(x) for x in output.read_text().splitlines()];assert rows[-1]['status']=='passed'
    record['references']=[r for r in rows if r.get('type')=='reference'];assert len(record['references'])==4
    for p,sha in prior['files'].items():assert digest(ROOT/p)==sha,p
    record['status']='passed'
except BaseException as error:record.update(status='failed',failure=repr(error));raise
finally:
    record['wallSeconds']=time.monotonic()-start;record['evidence']={str(p.relative_to(HERE)):digest(p) for p in (output,log) if p.exists()}
    write_new(HERE/'root-reference-v1-summary.json',record)
print(json.dumps(record,indent=2))
