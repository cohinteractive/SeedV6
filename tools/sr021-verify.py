"""Frozen accepted-production bridge and explicit futility UCI research preflight."""
import argparse, importlib.util, json
from pathlib import Path
from types import SimpleNamespace

def module(name, filename):
    spec=importlib.util.spec_from_file_location(name,Path(__file__).with_name(filename))
    value=importlib.util.module_from_spec(spec);spec.loader.exec_module(value);return value
m=module('futility_match','sr021-match.py')
v=module('integration','verify-sr024-integration.py')
b=m.b

def run(a):
    out=Path(a.out);out.mkdir(parents=True,exist_ok=False)
    report={'status':'running','arguments':vars(a),'parity':[],'preflights':[]}
    frozen={**b.installation_hashes(a.baseline),**b.installation_hashes(a.candidate)}
    engines=[]
    try:
        for side in ('baseline','candidate'):
            args=SimpleNamespace(java=a.java,store=a.store,baseline=getattr(a,side),baseline_policy='off',baseline_workers=1)
            e=v.start(m.command(args,'baseline'),out/(side+'-uci.txt'));engines.append(e)
            v.identify(e,1,'production')
        prefixes=[[]]+b.opening_prefixes(a.openings,32)[:8]
        for prefix in prefixes:
            for go in ('go depth 5','go nodes 100000'):
                results=[v.v.request(e,prefix,go) for e in engines]
                assert v.result_key(results[0])==v.result_key(results[1]),(prefix,go,results)
                report['parity'].append(dict(prefix=prefix,go=go,results=results))
        for e in engines:e.close()
        engines=[]
        for policy in ('off','leaf','shallow','prefix','deep'):
            for workers in (1,4):
                args=SimpleNamespace(java=a.java,store=a.store,baseline=a.candidate,baseline_policy=policy,baseline_workers=workers)
                report['preflights'].append(dict(policy=policy,workers=workers,**m.preflight(m.command(args,'baseline'),out/f'{policy}-{workers}-uci.txt',workers,policy)))
        assert all(b.digest(p)==h for p,h in frozen.items())
        report.update(status='passed',inputsStable=True)
    except BaseException as error:
        report.update(status='failed',failure=repr(error));raise
    finally:
        for e in engines:e.close()
        report['files']=frozen;b.save(out/'verification.json',report)

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    for name in ('baseline','candidate','store','openings','out'):p.add_argument('--'+name,required=True)
    p.add_argument('--java',default='C:/java/jdk-21/bin/java.exe');run(p.parse_args())
