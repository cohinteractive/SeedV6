"""Summarize observed JFR/JIT/NMT evidence without treating samples as causal cost.
Usage: summarize_profile.py PROFILE-STEM NEW-SUMMARY.json
"""
import collections
import hashlib
import json
import pathlib
import re
import subprocess
import sys

stem=pathlib.Path(sys.argv[1]);target=pathlib.Path(sys.argv[2])
if target.exists(): raise SystemExit("Refusing to replace summary")
events=stem.with_suffix(".events-depth256.json")
if events.exists(): raise SystemExit("Refusing to replace raw events")
with events.open("xb") as out:
    subprocess.run(["C:/java/jdk-21/bin/jfr.exe","print","--json","--stack-depth","256","--events","jdk.ExecutionSample",str(stem.with_suffix('.jfr'))],stdout=out,check=True)
data=json.loads(events.read_text())['recording']['events']
tops=collections.Counter();inclusive=collections.Counter();samples=0;truncated=0;recursiveFrames=[]
for event in data:
    value=event['values'];trace=value.get('stackTrace')
    if not trace: continue
    frames=trace['frames']
    methods=[f['method']['type']['name']+'.'+f['method']['name'] for f in frames]
    if not any('com/ohinteractive/seedv6/search/exact/ExactSearch.' in m for m in methods): continue
    samples+=1;truncated+=bool(trace['truncated']);tops[methods[0]]+=1
    inclusive.update(set(methods))
    recursiveFrames.append(sum(m.endswith('.negamax') or m.endswith('.quiescence') for m in methods))
compiled={};failures=collections.Counter()
for line in stem.with_suffix('.jit.xml').open():
    if '<inline_fail ' in line:
        m=re.search("reason='([^']+)'",line)
        if m: failures[m.group(1)]+=1
    if '<nmethod ' not in line or "compiler='c2'" not in line: continue
    match=re.search("method='com.ohinteractive.seedv6.search.exact.ExactSearch (quiescence|flatQuiescence|negamax|flatNegamax) ([^']+)'",line)
    if match:
        name=match.group(1)+' '+match.group(2)
        current={k:re.search(k+"='([^']+)'",line).group(1) for k in ['bytes','size','stamp']}
        previous=compiled.get(name,{})
        current['firstC2Stamp']=previous.get('firstC2Stamp',current['stamp'])
        current['c2Versions']=previous.get('c2Versions',0)+1
        compiled[name]=current
log=stem.with_suffix('.log').read_text()
nmt={}
for name in ['Java Heap','Thread','Code','GC','Compiler']:
    m=re.search(re.escape(name)+r' \(reserved=(\d+), committed=(\d+)\)',log)
    if m:nmt[name]={'reserved':int(m.group(1)),'committed':int(m.group(2))}
m=re.search(r'stack: reserved=(\d+), committed=(\d+)',log)
if m:nmt['threadStacks']={'reserved':int(m.group(1)),'committed':int(m.group(2))}
result={'profile':str(stem),'analysisRevision':2,'jfrPrintStackDepth':256,
        'supersedes':'v1 summaries used jfr print default display depth 5; those sampled-stack maxima and search sample counts were display-limited. Original derived output remains preserved.',
        'jfrSha256':hashlib.sha256(stem.with_suffix('.jfr').read_bytes()).hexdigest(),
        'searchStackSamples':samples,'truncatedSearchStackSamples':truncated,
        'maxSampledRecursiveFrames':max(recursiveFrames,default=0),
        'topMethods':tops.most_common(16),'inclusiveMethods':inclusive.most_common(20),
        'lastObservedC2Compilation':compiled,'wholeJvmInlineFailureReasons':failures.most_common(12),
        'nativeMemoryAtExitBytes':nmt,
        'limits':'Sampling is approximate; inlined scopes may appear in traces. Top/inclusive samples cannot isolate call overhead or hardware branches/cache misses. NMT is whole-process exit state including JVM/JIT/JFR/GC, not per-search peak. Thread stack reservations remain even with flat Search. Clean cost data exclude these profiled runs.'}
target.write_text(json.dumps(result,indent=2)+'\n')
print(json.dumps({'profile':str(stem),'samples':samples,'compiledMethods':list(compiled),'nmt':nmt}))
