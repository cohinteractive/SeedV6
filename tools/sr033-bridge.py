"""Actual standalone production versus frozen research OFF, plus deterministic tree parity."""
import argparse
import importlib.util
from pathlib import Path
from types import SimpleNamespace


def module(name, file):
    spec=importlib.util.spec_from_file_location(name,Path(__file__).with_name(file))
    m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m


m=module('feedback_match','sr033-match.py');b=m.b
v=module('bridge','verify-sr024-integration.py')


def run(a):
    out=Path(a.out);out.mkdir(parents=True,exist_ok=False)
    report=dict(status='running',arguments=vars(a),parity=[],production=[])
    frozen={**b.installation_hashes(a.baseline),**b.installation_hashes(a.candidate)}
    frozen.update({str(p.resolve()):b.digest(p) for p in Path(a.production).rglob('*') if p.is_file()})
    frozen[str(Path(a.model).resolve())]=b.digest(a.model)
    engines=[]
    try:
        for installation in [a.baseline,a.candidate]:
            args=SimpleNamespace(java=a.java,store=a.store,baseline=installation,baseline_policy='off',baseline_workers=1)
            engines.append(v.start(m.command(args,'baseline'),out/f'parity-{len(engines)}-uci.txt'))
        standalone=[a.java,'-Xms256m','-Xmx512m','-XX:ActiveProcessorCount=6','-Djava.awt.headless=true',
                    '-cp',str(Path(a.production).resolve()/'lib'/'*'),'com.ohinteractive.seedv6.uci.UciMain','--model',str(Path(a.model).resolve())]
        engines.append(v.start(standalone,out/'standalone-uci.txt'))
        prefixes=[[]]+b.opening_prefixes(a.openings,32)[:8]
        for prefix in prefixes:
            for go in ['go depth 5','go nodes 100000']:
                results=[v.v.request(e,prefix,go) for e in engines]
                assert v.result_key(results[0])==v.result_key(results[1])==v.result_key(results[2]),(prefix,go,results)
                report['parity'].append(dict(prefix=prefix,go=go,results=results))
        e=engines[-1]
        for workers in range(1,17):
            e.send(f'setoption name Threads value {workers}');e.send('isready');e.until('readyok',90)
            e.send('uci');lines=e.until('uciok',90)
            identity=[s for s in lines if s.startswith('info string evaluator=')][-1]
            fields=dict(s.split('=',1) for s in identity.split() if '=' in s)
            assert fields['workers']==str(workers) and fields['sha256']==b.NETWORK_SHA256
            assert fields['mode']=='production' and fields['see']=='qxray' and fields['lmr']=='quiet' and fields['lmp']=='window'
            assert fields['concurrency']==('off' if workers==1 else 'root_reuse')
            r=v.v.request(e,[],'go nodes 100000');assert r['nodes']==100000
            report['production'].append(dict(workers=workers,identity=identity,result=r))
        assert all(b.digest(p)==h for p,h in frozen.items())
        report.update(status='passed',inputsStable=True)
    except BaseException as error:
        report.update(status='failed',failure=repr(error));raise
    finally:
        for e in engines:e.close()
        report['files']=frozen;b.save(out/'bridge.json',report)


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    for key in ['baseline','candidate','production','model','store','openings','out']:p.add_argument('--'+key,required=True)
    p.add_argument('--java',default='C:/java/jdk-21/bin/java.exe')
    run(p.parse_args())
