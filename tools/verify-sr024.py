"""Frozen UCI baseline parity, policy identity, legal PV and worker-transition checks."""
import argparse
import importlib.util
import json
from pathlib import Path
import re
from types import SimpleNamespace

spec=importlib.util.spec_from_file_location('match',Path(__file__).with_name('sr024-match.py'))
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);b=m.b


def identity(engine,workers,policy):
    engine.send('setoption name Threads value '+str(workers));engine.send('isready')
    lines=engine.until('readyok',90)
    engine.send('uci');lines+=engine.until('uciok',90)
    line=[x for x in lines if x.startswith('info string evaluator=')][-1]
    fields=dict(x.split('=',1) for x in line.split() if '=' in x)
    assert fields['workers']==str(workers) and fields['sha256']==b.NETWORK_SHA256
    assert fields['checkpoint']==b.CHECKPOINT and fields['mapping']=='BRN_PAIR2_MATERIAL_RESIDUAL_QUARTER_V1'
    assert fields['lmr']=='quiet' and fields['lmp']=='window' and fields['reuse']=='effort'
    assert fields.get('concurrency','off')==('root_reuse' if workers>1 else 'off')
    assert fields.get('see','off')==policy and fields['internal']=='off' and fields['nmp']=='off'
    return line


def request(e,prefix,go):
    board=b.chess.Board()
    for move in prefix:board.push_uci(move)
    e.send('ucinewgame');e.send('isready');e.until('readyok')
    e.send('position startpos'+(' moves '+' '.join(prefix) if prefix else ''));e.send(go)
    result=b.inspect_result(board,e.until('bestmove ',90))
    return result


def main(a):
    out=Path(a.out);out.mkdir(parents=True,exist_ok=False)
    report={'status':'running','arguments':vars(a),'parity':[],'transitions':[],'preflights':[]}
    frozen={str(Path(p).resolve()):b.digest(p) for p in (__file__,m.__file__,b.__file__,a.java,a.openings)}
    for install in (a.baseline,a.candidate):frozen.update(b.installation_hashes(install))
    model=Path(a.store)/'checkpoints'/b.CHECKPOINT
    frozen.update({str(p.resolve()):b.digest(p) for p in model.iterdir() if p.is_file()})
    report['filesBefore']=frozen
    args=SimpleNamespace(java=a.java,store=a.store,baseline_workers=1,baseline_policy='off')
    engines=[]
    try:
        for name,install in (('baseline',a.baseline),('candidate',a.candidate)):
            args.baseline=install;e=b.Uci(m.command(args,'baseline'),out/(name+'-bridge.txt'));engines.append(e)
            e.send('uci');e.until('uciok',90);e.send('setoption name OwnBook value false');identity(e,1,'off')
        count=json.loads(Path(a.openings).with_suffix('.selection.json').read_text())['count']
        prefixes=[[]]+b.opening_prefixes(a.openings,count)[:8]
        for prefix in prefixes:
            for go in ('go depth 5','go nodes 100000'):
                results=[request(e,prefix,go) for e in engines]
                def key(r):
                    info=r['finalInfo'] or ''
                    return [r['bestmove'],r['nodes'],re.search(r'score (?:cp|mate) -?\d+',info).group(0),info.split(' pv ')[-1]]
                assert key(results[0])==key(results[1]),(prefix,go,results)
                report['parity'].append({'prefix':prefix,'go':go,'results':results})
        for e in engines:e.close()
        engines=[]
        args.baseline=a.candidate
        for policy in ('off','qsafe','qdeep','qscout','qpawn','qxray','main','quiet','order'):
            args.baseline_policy=policy
            for workers in (1,4):
                args.baseline_workers=workers
                report['preflights'].append({'policy':policy,'workers':workers,
                    **m.preflight(m.command(args,'baseline'),out/f'{policy}-{workers}.txt',workers,policy)})
            args.baseline_workers=1;e=b.Uci(m.command(args,'baseline'),out/f'{policy}-transitions.txt');engines.append(e)
            e.send('uci');e.until('uciok',90);e.send('setoption name OwnBook value false')
            for workers in (1,4,16,1):
                observed=identity(e,workers,policy)
                result=request(e,prefixes[1],'go nodes 100000');assert result['nodes']==100000
                report['transitions'].append({'policy':policy,'workers':workers,'identity':observed,'result':result})
            e.close();engines=[]
        assert all(b.digest(p)==h for p,h in frozen.items())
        report.update(status='passed',inputsStable=True,parityComparisons=len(report['parity']))
    except BaseException as error:
        report.update(status='failed',failure=repr(error));raise
    finally:
        for e in engines:e.close()
        b.save(out/'verification.json',report)


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    for k in ('baseline','candidate','store','openings','out'):p.add_argument('--'+k,required=True)
    p.add_argument('--java',default='C:/java/jdk-21/bin/java.exe');main(p.parse_args())
