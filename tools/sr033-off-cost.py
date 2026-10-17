"""Rotated warmed complete-request OFF timing against frozen accepted production.

One active single-worker request at a time, equal nodes and deterministic result
parity. Request timing includes abandoned iterations; excludes process startup,
model load and explicit warmup. Descriptive cost evidence, not strength inference.
"""
import argparse
import importlib.util
import json
from pathlib import Path
import re
import statistics
from types import SimpleNamespace

spec=importlib.util.spec_from_file_location('verify',Path(__file__).with_name('sr033-bridge.py'))
bridge=importlib.util.module_from_spec(spec);spec.loader.exec_module(bridge);b=bridge.b;m=bridge.m;v=bridge.v.v


def run(a):
    out=Path(a.out);out.mkdir(parents=True,exist_ok=False)
    frozen={str(Path(p).resolve()):b.digest(p) for p in (__file__,bridge.__file__,bridge.v.__file__,v.__file__,m.__file__,b.__file__,a.java,a.openings)}
    for install in (a.baseline,a.candidate):frozen.update(b.installation_hashes(install))
    model=Path(a.store)/'checkpoints'/b.CHECKPOINT
    frozen.update({str(p.resolve()):b.digest(p) for p in model.iterdir() if p.is_file()})
    report={'status':'running','arguments':vars(a),'frozen':frozen,'rows':[],'identities':[]};engines=[]
    try:
        for name,install in [('baseline',a.baseline),('candidate',a.candidate)]:
            args=SimpleNamespace(java=a.java,store=a.store,baseline=install,baseline_policy='off',baseline_workers=1)
            e=b.Uci(m.command(args,'baseline'),out/(name+'.txt'));engines.append(e)
            e.send('uci');e.until('uciok',90);e.send('setoption name OwnBook value false')
            e.send('isready');lines=e.until('readyok',90);e.send('uci');lines+=e.until('uciok',90)
            identity=[line for line in lines if line.startswith('info string evaluator=')][-1]
            fields=dict(x.split('=',1) for x in identity.split() if '=' in x)
            assert fields['workers']=='1' and fields['sha256']==b.NETWORK_SHA256 and fields['checkpoint']==b.CHECKPOINT
            assert fields['mode']=='production' and fields['see']=='qxray' and fields['lmr']=='quiet' and fields['lmp']=='window' and fields['reuse']=='effort'
            assert fields.get('feedback','off')=='off'
            report['identities'].append(identity)
            for _ in range(3):assert v.request(e,[],'go nodes 1000000')['nodes']==1000000
        count=json.loads(Path(a.openings).with_suffix('.selection.json').read_text())['count']
        prefixes=b.opening_prefixes(a.openings,count)[:8]
        for repeat in range(4):
            for index,prefix in enumerate(prefixes):
                results={}
                for side in ((0,1) if (index+repeat)%2==0 else (1,0)):
                    results[side]=v.request(engines[side],prefix,'go nodes 500000')
                def identity(r):
                    info=r['finalInfo'];return r['nodes'],r['bestmove'],re.search(r'score (?:cp|mate) -?\d+',info).group(0),info.split(' pv ')[-1]
                assert identity(results[0])==identity(results[1])
                report['rows'].append({'repeat':repeat,'prefix':prefix,'baseline':results[0],'candidate':results[1]})
        assert all(b.digest(p)==h for p,h in frozen.items())
        totals={side:sum(r[side]['elapsedNanos'] for r in report['rows']) for side in ('baseline','candidate')}
        report.update(status='passed',inputsStable=True,totalElapsedNanos=totals,
                      aggregateWallRatio=totals['candidate']/totals['baseline'],
                      medianPairedWallRatio=statistics.median(r['candidate']['elapsedNanos']/r['baseline']['elapsedNanos'] for r in report['rows']),
                      limits='Single-host descriptive warmed request timing, not whole-lifetime CPU, strength, equivalence or a cross-platform performance guarantee.')
        print(json.dumps({k:report[k] for k in ('status','aggregateWallRatio','medianPairedWallRatio','totalElapsedNanos')}))
    except BaseException as error:
        report.update(status='failed',failure=repr(error));raise
    finally:
        for e in engines:e.close()
        b.save(out/'result.json',report)


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    for k in ('baseline','candidate','store','openings','out'):p.add_argument('--'+k,required=True)
    p.add_argument('--java',default='C:/java/jdk-21/bin/java.exe');run(p.parse_args())
