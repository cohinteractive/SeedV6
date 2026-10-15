"""Verify adopted SEE routing against both frozen candidates and preserved controls.

No strength inference: deterministic native-lane parity, actual UCI identities,
legal PV/budgets/stops and all worker settings, plus frozen hot-class equality.
Existing frozen tournament tools are imported without changing their semantics.
"""
import argparse
import importlib.util
import json
from pathlib import Path
import re
from types import SimpleNamespace
import zipfile


def module(name, filename):
    spec = importlib.util.spec_from_file_location(name, Path(__file__).with_name(filename))
    value = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(value)
    return value


v = module('verify', 'verify-sr024.py')
m, b = v.m, v.b
c = module('concurrency', 'sr036-match.py')


def command(a, install, policy, workers):
    args = SimpleNamespace(java=a.java, store=a.store, baseline=install,
                           baseline_policy='qxray' if policy == 'production' else policy,
                           baseline_workers=workers)
    cmd = m.command(args, 'baseline')
    if policy == 'production':
        cmd = [x for x in cmd if not x.startswith('-Dseedv6.uci.seeResearch=')]
    return cmd


def identify(e, workers, policy):
    actual = 'qxray' if policy == 'production' else policy
    line = v.identity(e, workers, actual)
    fields = dict(x.split('=', 1) for x in line.split() if '=' in x)
    expected_mode = 'production' if policy == 'production' else 'control' if policy == 'off' else 'research'
    assert fields['mode'] == expected_mode, line
    assert fields['leaves'] == ('see_selective' if actual in ('qxray','qsafe','qdeep','qscout','qpawn') else 'unpruned'), line
    assert ('experimental' in fields) == (expected_mode == 'research'), line
    return line


def start(cmd, path):
    e = b.Uci(cmd, path)
    e.send('uci'); e.until('uciok', 90)
    e.send('setoption name OwnBook value false')
    return e


def result_key(r):
    info = r['finalInfo'] or ''
    return [r['bestmove'], r['nodes'], re.search(r'score (?:cp|mate) -?\d+', info).group(0), info.split(' pv ')[-1]]


def main(a):
    out = Path(a.out); out.mkdir(parents=True, exist_ok=False)
    report = {'status':'running', 'arguments':vars(a), 'parity':[], 'preflights':[], 'transitions':[], 'historical':[]}
    frozen = {str(Path(p).resolve()):b.digest(p) for p in
              (__file__, v.__file__, m.__file__, b.__file__, c.__file__, m.c.__file__, m.c.n.__file__, a.java, a.openings)}
    for install in (a.confirmed, a.accepted, a.integrated): frozen.update(b.installation_hashes(install))
    model = Path(a.store)/'checkpoints'/b.CHECKPOINT
    frozen.update({str(p.resolve()):b.digest(p) for p in model.iterdir() if p.is_file()})
    report['filesBefore'] = frozen
    engines = []
    try:
        assert b.digest(model/'network.brn-pair2') == b.NETWORK_SHA256
        with zipfile.ZipFile(Path(a.confirmed)/'lib/app.jar') as x, zipfile.ZipFile(Path(a.integrated)/'lib/app.jar') as y:
            names = {n for n in x.namelist() if n.endswith('.class')}
            assert names == {n for n in y.namelist() if n.endswith('.class')}
            changed = sorted(n for n in names if x.read(n) != y.read(n))
            expected = ['com/ohinteractive/seedv6/search/driver/ProductionSearch.class',
                        'com/ohinteractive/seedv6/search/manage/SearchLifecycleService$1.class',
                        'com/ohinteractive/seedv6/search/manage/SearchLifecycleService$SearchJob.class',
                        'com/ohinteractive/seedv6/search/manage/SearchLifecycleService.class',
                        'com/ohinteractive/seedv6/uci/UciEvaluation.class']
            assert changed == expected, changed
            report['classComparison'] = {'classes':len(names), 'changed':changed,
                                        'allOtherClassBytesIdentical':True, 'unchanged':len(names)-len(changed)}
        jars = lambda install: {p.name:b.digest(p) for p in (Path(install)/'lib').glob('*.jar') if p.name != 'app.jar'}
        assert jars(a.confirmed) == jars(a.integrated), 'Runtime dependencies changed'
        report['runtimeDependenciesIdentical'] = True
        count = json.loads(Path(a.openings).with_suffix('.selection.json').read_text())['count']
        prefixes = [[]] + b.opening_prefixes(a.openings, count)[:8]
        for name, frozen_install, frozen_policy, integrated_policy in (
                ('adopted', a.confirmed, 'qxray', 'production'), ('preserved', a.accepted, 'off', 'off')):
            engines = [start(command(a, frozen_install, frozen_policy, 1), out/(name+'-frozen.txt')),
                       start(command(a, a.integrated, integrated_policy, 1), out/(name+'-integrated.txt'))]
            v.identity(engines[0], 1, frozen_policy)
            identify(engines[1], 1, integrated_policy)
            for prefix in prefixes:
                for go in ('go depth 5', 'go nodes 100000'):
                    results = [v.request(e, prefix, go) for e in engines]
                    assert result_key(results[0]) == result_key(results[1]), (name, prefix, go, results)
                    report['parity'].append({'comparison':name,'prefix':prefix,'go':go,'results':results})
            for e in engines: e.close()
            engines = []
        for policy in ('production','off','qxray','qsafe','qdeep','qscout','qpawn','main','quiet','order'):
            for workers in (range(1,17) if policy == 'production' else (1,4)):
                e = start(command(a,a.integrated,policy,workers), out/f'{policy}-{workers}.txt'); engines = [e]
                identity = identify(e, workers, policy)
                requests = [v.request(e, [], 'go nodes 100000') for _ in range(2)]
                assert all(r['nodes'] == 100000 for r in requests)
                report['preflights'].append({'policy':policy,'workers':workers,'identity':identity,'requests':requests})
                e.close(); engines = []
        for policy in ('production','off','qxray'):
            e = start(command(a,a.integrated,policy,1), out/(policy+'-transitions.txt')); engines = [e]
            for workers in (1,4,16,1):
                identity = identify(e,workers,policy)
                requests = [v.request(e,prefixes[1],'go nodes 100000'), v.request(e,prefixes[1],'go movetime 50')]
                assert requests[0]['nodes'] == 100000
                assert requests[1]['termination'] == 'TIME_LIMIT', requests[1]
                e.send('ucinewgame'); e.send('isready'); e.until('readyok')
                e.send('position startpos'); e.send('go infinite')
                lines = e.until('info depth 1 ',90)
                e.send('stop'); lines += e.until('bestmove ',90)
                stopped = b.inspect_result(b.chess.Board(),lines)
                assert stopped['termination'] == 'STOPPED', stopped
                requests.append(stopped)
                report['transitions'].append({'policy':policy,'workers':workers,'identity':identity,'requests':requests})
            e.close(); engines = []
        for policy in ('legacy','root_reuse'):
            for workers in (1,4,16):
                args = SimpleNamespace(java=a.java,store=a.store,baseline=a.integrated,
                                       baseline_policy=policy,baseline_workers=workers)
                observed = c.preflight(c.command(args,'baseline'),out/f'historical-{policy}-{workers}.txt',workers,policy)
                report['historical'].append({'policy':policy,'workers':workers,**observed})
        assert all(b.digest(p) == h for p,h in frozen.items()), 'Input mutation during validation'
        report.update(status='passed',inputsStable=True,parityComparisons=len(report['parity']),
                      requests=2*len(report['parity'])+2*len(report['preflights'])+3*len(report['transitions'])+2*len(report['historical']))
    except BaseException as error:
        report.update(status='failed',failure=repr(error)); raise
    finally:
        for e in engines: e.close()
        b.save(out/'integration.json',report)


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    for key in ('confirmed','accepted','integrated','store','openings','out'): p.add_argument('--'+key,required=True)
    p.add_argument('--java',default='C:/java/jdk-21/bin/java.exe')
    main(p.parse_args())
