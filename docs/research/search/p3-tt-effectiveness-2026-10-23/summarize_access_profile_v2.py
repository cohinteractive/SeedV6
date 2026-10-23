"""Inclusive sampled-stack attribution, never an exact CPU/savings estimate."""
from collections import Counter
import json
import re
from run_diagnostic_v1 import digest, write_new, HERE

def seconds(value):
    match=re.fullmatch(r'PT(?:(\d+(?:\.\d+)?)H)?(?:(\d+(?:\.\d+)?)M)?(?:(\d+(?:\.\d+)?)S)?',value)
    assert match,value
    return sum(float(x or 0)*factor for x,factor in zip(match.groups(),(3600,60,1)))
report={'status':'passed','protocolSha256':digest(HERE/'access-profile-v1.json'),'domains':{},'classificationCorrection':'V1 grouped TT-package classes but SearchKey resides in search.exact; V2 explicitly includes that class. TTable counts and raw evidence are unchanged.', 'priorSummarySha256':digest(HERE/'access-profile-v1-summary.json'),
    'limits':['Sampling includes startup, warmup and all requests; finite/inlined stack attribution is not an exact CPU or causal-savings estimate.',
              'ExecutionSample and NativeMethodSample have different periods and are never pooled. Native samples can represent blocking native reads/parks.',
              'Monitor/park threshold10ms misses shorter waits. Summed thread durations overlap and are not lost request/CPU time.',
              'Profiled time includes unestimated JFR overhead and fixed-node-budget coordination; it is not production clock performance.']}
for workers in (1,4):
    name=f'access-profile-w{workers}-v1';path=HERE/'raw'/f'{name}-events.json'
    receipt=json.loads((HERE/f'{name}-execution.json').read_text());assert receipt['status']=='passed'
    events=json.loads(path.read_text())['recording']['events'];counts=Counter(e['type'] for e in events)
    value={'eventsSha256':digest(path),'eventCounts':dict(counts),'requestNodes':receipt['requestNodes'],
           'requests':receipt['requests'],'wallSecondsIncludingExport':receipt['wallSeconds'],
           'nativeProcessCpuSeconds':receipt['nativeLifetime']['totalCpuSeconds'],'samples':{}}
    for kind in ('jdk.ExecutionSample','jdk.NativeMethodSample'):
        selected=[e['values'] for e in events if e['type']==kind];categories=Counter();tt=Counter();leaf=Counter();threads=Counter();truncated=missing=0
        for v in selected:
            stack=v.get('stackTrace') or {};frames=stack.get('frames',[])
            if not frames:missing+=1
            truncated+=bool(stack.get('truncated',False))
            names=[f['method']['type']['name'].replace('/','.')+'.'+f['method']['name'] for f in frames]
            threads[(v.get('sampledThread') or {}).get('javaName','unknown')]+=1
            if names:leaf[names[0]]+=1
            categories['TTandSearchKeyInclusive']+=any('.search.tt.' in n or '.search.exact.SearchKey.' in n for n in names)
            categories['TTable']+=any('.search.tt.TTable.' in n for n in names)
            categories['SearchInclusive']+=any('.search.' in n for n in names)
            categories['EvaluationInclusive']+=any('.core.nnue.' in n or '.search.evaluation.' in n for n in names)
            categories['ParallelSearchInclusive']+=any('.search.exact.ParallelSearch.' in n for n in names)
            for method in set(n for n in names if '.search.tt.' in n or '.search.exact.SearchKey.' in n):tt[method]+=1
        value['samples'][kind]={'count':len(selected),'missingStack':missing,'truncatedStacks':truncated,
            'inclusiveStackCounts':dict(categories),'ttMethodsInclusive':dict(tt.most_common()),
            'topLeafMethods':dict(leaf.most_common(15)),'sampledJavaThreads':dict(threads),
            'TTablePresenceFraction':categories['TTable']/len(selected) if selected else None}
    for kind,classField in (('jdk.JavaMonitorEnter','monitorClass'),('jdk.ThreadPark','parkedClass')):
        selected=[e['values'] for e in events if e['type']==kind];classes=Counter();durations=Counter();tt=[]
        for v in selected:
            className=(v.get(classField) or {}).get('name','unknown').replace('/','.')
            classes[className]+=1;durations[className]+=seconds(v['duration'])
            if 'TTable' in className:tt.append(v)
        value[kind]={'count':len(selected),'classCounts':dict(classes),'overlappingRecordedDurationSeconds':dict(durations),
            'TTableMonitorEvents':len(tt)}
    report['domains'][str(workers)]=value
write_new(HERE/'access-profile-v2-summary.json',report)
for workers,value in report['domains'].items():
    print(workers,value['eventCounts'],value['samples']['jdk.ExecutionSample']['inclusiveStackCounts'],
          value['samples']['jdk.ExecutionSample']['ttMethodsInclusive'])
