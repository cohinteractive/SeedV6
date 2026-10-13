"""Frozen-engine fixed-work contention diagnostic, never a playing-strength test.

Compare isolated, homogeneous and mixed pairs of active engines. Each request has
the same aggregate node cap and real opening history. No timed tournament overlaps.
"""
import argparse
from concurrent.futures import ThreadPoolExecutor
import importlib.util
import json
import os
from pathlib import Path
import platform
import subprocess
import threading
import time

spec=importlib.util.spec_from_file_location('match',Path(__file__).with_name('sr036-match.py'))
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);b=m.b


def main(a):
    out=Path(a.out);out.mkdir(parents=True,exist_ok=False)
    report={'status':'running','arguments':vars(a),'observations':[],'identities':{},
            'os':platform.platform(),'logicalCpus':os.cpu_count(),
            'javaVersion':subprocess.run([a.java,'-version'],capture_output=True,text=True,check=True).stderr}
    frozen={str(Path(p).resolve()):b.digest(p) for p in (__file__,m.__file__,b.__file__,a.java,a.openings)}
    selection=Path(a.openings).with_suffix('.selection.json')
    frozen[str(selection.resolve())]=b.digest(selection)
    frozen.update(b.installation_hashes(a.install))
    model=Path(a.store)/'checkpoints'/b.CHECKPOINT
    frozen.update({str(p.resolve()):b.digest(p) for p in model.iterdir() if p.is_file()})
    assert b.digest(model/'network.brn-pair2')==b.NETWORK_SHA256
    report['filesBefore']=frozen;engines={}
    try:
        for name in ('b1','b2','c1','c2'):
            workers=1 if name.startswith('b') else 4
            policy='off' if workers==1 else 'root_reuse'
            cmd=b.command(a.java,a.install,a.store,'production','production','off','default','off','default')
            cmd[3]='-XX:ActiveProcessorCount=6'
            cmd[4:4]=['-Dseedv6.uci.concurrencyResearch='+policy,'-Dseedv6.uci.warmupWorkers='+str(workers)]
            engine=b.Uci(cmd,out/(name+'.txt'));engines[name]=engine
            engine.send('uci');lines=engine.until('uciok',90)
            engine.send('setoption name OwnBook value false');engine.send('setoption name Threads value '+str(workers))
            engine.send('isready');lines+=engine.until('readyok',60)
            identity=[x for x in lines if x.startswith('info string evaluator=')][-1]
            fields=dict(x.split('=',1) for x in identity.split() if '=' in x)
            assert fields['workers']==str(workers) and fields['sha256']==b.NETWORK_SHA256
            assert fields.get('concurrency','off')==policy and fields['reuse']=='effort'
            report['identities'][name]=identity
            for _ in range(3):
                engine.send('ucinewgame');engine.send('position startpos');engine.send('go nodes 500000')
                assert b.inspect_result(b.chess.Board(),engine.until('bestmove ',90))['nodes']==500000
        prefixes=b.opening_prefixes(a.openings,json.loads(selection.read_text())['count'])[:6]
        assert len(prefixes)==6
        groups=(('b1',),('c1',),('b1','b2'),('c1','c2'),('b1','c1'))
        with ThreadPoolExecutor(max_workers=2) as pool:
            for repeat in range(2):
                for root,prefix in enumerate(prefixes):
                    board=b.chess.Board()
                    for move in prefix:board.push_uci(move)
                    for step in range(len(groups)):
                        group=groups[(step+root+repeat)%len(groups)]
                        for name in group:
                            e=engines[name];e.send('ucinewgame');e.send('isready');e.until('readyok')
                            e.send('position startpos moves '+' '.join(prefix))
                        barrier=threading.Barrier(len(group))
                        def search(name):
                            barrier.wait(timeout=10);started=time.monotonic()
                            e=engines[name];e.send('go nodes 500000')
                            r=b.inspect_result(board,e.until('bestmove ',90))
                            assert r['nodes']==500000 and r['termination']=='NODE_LIMIT'
                            return {'engine':name,'wallSeconds':time.monotonic()-started,**r}
                        futures=[pool.submit(search,name) for name in group]
                        observations=[f.result() for f in futures]
                        report['observations'].append({'repeat':repeat,'root':root,'group':list(group),'requests':observations})
        assert all(b.digest(p)==h for p,h in frozen.items())
        report.update(status='passed',inputsStable=True,measuredRequests=sum(len(x['requests']) for x in report['observations']),
                      limits='Fixed aggregate work on six development openings; scheduling/tree variation and residual JIT remain. Does not establish absence of clock-game contention or playing strength. JVM ergonomic CPU count is six, not affinity.')
    except BaseException as error:
        report.update(status='failed',failure=repr(error));raise
    finally:
        cleanup=[]
        for name,e in engines.items():
            try:e.close()
            except BaseException as error:cleanup.append([name,repr(error)])
            if e.process.returncode!=0 or e.errors:cleanup.append([name,e.process.returncode,e.errors])
        if cleanup:report.update(status='failed',cleanupFailures=cleanup)
        b.save(out/'contention.json',report)
    assert report['status']=='passed'


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    for key in ('install','store','openings','out'):p.add_argument('--'+key,required=True)
    p.add_argument('--java',default='C:/java/jdk-21/bin/java.exe')
    main(p.parse_args())
