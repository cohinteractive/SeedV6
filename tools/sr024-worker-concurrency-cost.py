"""Controlled warmed request cost at serial/concurrent four-worker engines.

Equal node budgets and roots isolate gross contention from changing game lengths.
This diagnostic does not establish strength or concurrency equivalence.
"""
import argparse
from concurrent.futures import ThreadPoolExecutor
import importlib.util
import json
from pathlib import Path
from types import SimpleNamespace
import time


def run(a):
    spec=importlib.util.spec_from_file_location('verify',Path(a.runner_directory)/'verify-sr024.py')
    v=importlib.util.module_from_spec(spec);spec.loader.exec_module(v);b=v.b
    out=Path(a.out);out.mkdir(parents=True,exist_ok=False)
    frozen={str(Path(p).resolve()):b.digest(p) for p in (__file__,v.__file__,v.m.__file__,b.__file__,a.java,a.openings)}
    for install in (a.baseline,a.candidate):frozen.update(b.installation_hashes(install))
    model=Path(a.store)/'checkpoints'/b.CHECKPOINT
    frozen.update({str(p.resolve()):b.digest(p) for p in model.iterdir() if p.is_file()})
    count=json.loads(Path(a.openings).with_suffix('.selection.json').read_text())['count']
    prefixes=b.opening_prefixes(a.openings,count)[:8]
    report={'status':'running','arguments':vars(a),'frozen':frozen,'rows':[],'identities':[]};engines=[]
    try:
        for policy,install in [('off',a.baseline),('qxray',a.candidate)]:
            for index in range(a.concurrency):
                args=SimpleNamespace(java=a.java,store=a.store,baseline=install,baseline_policy=policy,baseline_workers=4)
                e=b.Uci(v.m.command(args,'baseline'),out/f'{policy}-{index}.txt');engines.append(e)
                e.send('uci');e.until('uciok',90);e.send('setoption name OwnBook value false')
                report['identities'].append(v.identity(e,4,policy))
                for _ in range(3):assert v.request(e,[],'go nodes 1000000')['nodes']==1000000
            with ThreadPoolExecutor(max_workers=a.concurrency) as pool:
                for repeat in range(4):
                    for index,prefix in enumerate(prefixes):
                        for concurrency in ((1,a.concurrency) if (repeat+index)%2==0 else (a.concurrency,1)):
                            started=time.perf_counter_ns()
                            def request(e):return v.request(e,prefix,'go nodes 300000')
                            results=list(pool.map(request,engines)) if concurrency>1 else [request(e) for e in engines]
                            assert all(x['nodes']==300000 for x in results)
                            report['rows'].append({'policy':policy,'repeat':repeat,'root':index,'concurrency':concurrency,
                                'results':results,'groupWallNanos':time.perf_counter_ns()-started})
            for e in engines:e.close()
            engines=[]
        assert all(b.digest(p)==h for p,h in frozen.items())
        summary={}
        for policy in ('off','qxray'):
            totals={c:sum(sum(x['elapsedNanos'] for x in row['results']) for row in report['rows'] if row['policy']==policy and row['concurrency']==c) for c in (1,a.concurrency)}
            wall={c:sum(row['groupWallNanos'] for row in report['rows'] if row['policy']==policy and row['concurrency']==c) for c in (1,a.concurrency)}
            summary[policy]={'concurrentToSerialRequestLatency':totals[a.concurrency]/totals[1],
                'groupSpeedup':wall[1]/wall[a.concurrency],'requestElapsedNanos':totals,'groupWallNanos':wall}
            if a.concurrency==2:
                summary[policy].update(twoToOneRequestLatency=totals[2]/totals[1],
                    twoToOneNodeThroughput=totals[1]/totals[2],twoEngineGroupSpeedup=wall[1]/wall[2])
        report.update(status='passed',inputsStable=True,summary=summary,
            limits='Eight development roots, four rotated repetitions, fixed node budgets, warmed private engines. Actual search scheduling can change trees. No whole-lifetime CPU, statistical strength or universal no-contention claim.')
        print(json.dumps(summary))
    except BaseException as error:
        report.update(status='failed',failure=repr(error));raise
    finally:
        for e in engines:e.close()
        b.save(out/'result.json',report)


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    for name in ('baseline','candidate','store','openings','out','runner-directory'):p.add_argument('--'+name,required=True)
    p.add_argument('--java',default='C:/java/jdk-21/bin/java.exe')
    p.add_argument('--concurrency',type=int,choices=(2,3,4),default=2)
    run(p.parse_args())
