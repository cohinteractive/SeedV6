"""Verify the integrated factory keeps the confirmed executable's search class bytes.

Runtime checks cover production, explicit research and preserved legacy composition.
Parallel output need not be deterministic; this is an applicability/integration
check, not a new strength experiment or a substitute for independent game evidence.
"""
import argparse
import importlib.util
from pathlib import Path
from types import SimpleNamespace
import zipfile

spec=importlib.util.spec_from_file_location('match',Path(__file__).with_name('sr036-match.py'))
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);b=m.b


def main(a):
    out=Path(a.out);out.mkdir(parents=True,exist_ok=False)
    report={'status':'running','arguments':vars(a),'preflights':[]}
    frozen={str(Path(p).resolve()):b.digest(p) for p in (__file__,m.__file__,b.__file__,a.java)}
    for install in (a.confirmed,a.integrated):frozen.update(b.installation_hashes(install))
    model=Path(a.store)/'checkpoints'/b.CHECKPOINT
    frozen.update({str(p.resolve()):b.digest(p) for p in model.iterdir() if p.is_file()})
    report['filesBefore']=frozen
    try:
        assert b.digest(model/'network.brn-pair2')==b.NETWORK_SHA256
        before=Path(a.confirmed)/'lib'/'app.jar';after=Path(a.integrated)/'lib'/'app.jar'
        with zipfile.ZipFile(before) as x,zipfile.ZipFile(after) as y:
            names={n for n in x.namelist() if n.endswith('.class')}
            assert names=={n for n in y.namelist() if n.endswith('.class')}
            changed=sorted(n for n in names if x.read(n)!=y.read(n))
            expected=['com/ohinteractive/seedv6/search/driver/ProductionSearch.class',
                      'com/ohinteractive/seedv6/uci/UciEvaluation.class']
            assert changed==expected,changed
            report['classComparison']={'classes':len(names),'unchanged':len(names)-len(changed),
                                       'changed':changed,'allOtherClassBytesIdentical':True}
        old={p.name:b.digest(p) for p in (Path(a.confirmed)/'lib').glob('*.jar') if p.name!='app.jar'}
        new={p.name:b.digest(p) for p in (Path(a.integrated)/'lib').glob('*.jar') if p.name!='app.jar'}
        assert old==new,'Runtime dependencies changed'
        report['runtimeDependenciesIdentical']=True
        for policy in ('production','legacy','root_reuse'):
            for workers in (1,2,4,6,16):
                args=SimpleNamespace(java=a.java,baseline=a.integrated,store=a.store,
                                     baseline_policy=policy,baseline_workers=workers)
                observed=m.preflight(m.command(args,'baseline'),out/f'{policy}-{workers}.txt',workers,policy)
                report['preflights'].append({'policy':policy,'workers':workers,**observed})
        assert all(b.digest(p)==h for p,h in frozen.items()),'Input mutation during integration validation'
        report.update(status='passed',inputsStable=True)
    except BaseException as error:
        report.update(status='failed',failure=repr(error));raise
    finally:b.save(out/'integration.json',report)


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    for key in ('confirmed','integrated','store','out'):p.add_argument('--'+key,required=True)
    p.add_argument('--java',default='C:/java/jdk-21/bin/java.exe')
    main(p.parse_args())
