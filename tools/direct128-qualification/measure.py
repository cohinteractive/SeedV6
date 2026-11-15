"""Bounded production integration measurements; no games, training or selection."""
import json
import os
from pathlib import Path
import statistics
import subprocess

ROOT = Path(__file__).resolve().parents[2]
RAW = ROOT / 'docs/research/nnue-direct128-production/raw'
SOURCE = ROOT / 'docs/research/nnue-phase2-cglhw/raw/compact128-base-64-assessment-r1/network.p2nnue'
PROMOTED = ROOT / 'build-assets/production/conventional-direct128.cnnue'
ROLLBACK = ROOT / 'build-assets/production/conventional-wdl25.cnnue'
ROOTS = ROOT / 'docs/research/conventional-nnue-cglhw/strength-20261114/cost-roots.fens'


def main():
    out = RAW / 'measurements-r1'
    out.mkdir(exist_ok=False)
    cp = str(RAW / 'classes') + os.pathsep + str(ROOT / 'research/build/install/research/lib/*')
    commands = []

    def run(name, java, args):
        cmd = [str(java), '--add-modules=jdk.incubator.vector', '-XX:ActiveProcessorCount=1',
               '-Xmx512m', '-Xbatch', '-cp', cp, *map(str,args)]
        commands.append(cmd)
        (out / 'commands.json').write_text(json.dumps(commands, indent=2)+'\n')
        with (out/(name+'.jsonl')).open('x') as stdout, (out/(name+'.stderr')).open('x') as stderr:
            subprocess.run(cmd, cwd=ROOT, stdout=stdout, stderr=stderr, check=True, timeout=180)
        return [json.loads(line) for line in (out/(name+'.jsonl')).read_text().splitlines()]

    result = {'kernels': {}, 'search': {}, 'parallel': {}}
    for runtime in ['jdk-21','jdk-21-packaging']:
        java = Path('C:/java') / runtime / 'bin/java.exe'
        for name, probe, net in [('research','com.ohinteractive.seedv6.tools.eval.CompactNnueBenchmark',SOURCE),
                                  ('production','ProductionNnueBenchmark',PROMOTED),
                                  ('rollback','ProductionNnueBenchmark',ROLLBACK)]:
            data = run(runtime+'-'+name,java,[probe,net])[0]
            result['kernels'][runtime+'-'+name] = data
            if name != 'rollback':
                assert data['allocated_bytes_100000'] == 0
            print(runtime,name,'update+head ns',statistics.median(data['update_plus_head_ns']),
                  'allocated',data['allocated_bytes_100000'],flush=True)
    java = Path('C:/java/jdk-21-packaging/bin/java.exe')
    for repeat, names in enumerate([['research','production','rollback'],['rollback','production','research']]):
        for name in names:
            probe,net = {'research':('com.ohinteractive.seedv6.tools.eval.CompactSearchProbe',SOURCE),
                         'production':('ProductionSearchProbe',PROMOTED),
                         'rollback':('ProductionSearchProbe',ROLLBACK)}[name]
            data = run(f'search-{repeat}-{name}',java,['WarmedCost',probe,ROOTS,net])
            measured = data[data.index({'stage':'measured'})+1:]
            assert len(measured) == 24
            result['search'][f'{repeat}-{name}'] = dict(nps=sum(r['nodes'] for r in measured)*1e9/sum(r['elapsed_ns'] for r in measured),rows=measured)
            print('search',repeat,name,result['search'][f'{repeat}-{name}']['nps'],flush=True)
        fields=['round','root','nodes','score','depth','completed','configuration']
        signatures=lambda name:[[r[f] for f in fields] for r in result['search'][f'{repeat}-{name}']['rows']]
        assert signatures('production') == signatures('research'), 'Same-payload search changed'
    for workers in [2,4,16]:
        data=run(f'workers-{workers}',java,['ParallelProductionCheck',PROMOTED,ROOTS,workers])[0]
        assert data['status']=='PASS'
        result['parallel'][str(workers)]=data
        print('workers',workers,data['evaluationsByThread'],flush=True)
    (out/'summary.json').write_text(json.dumps(result,indent=2)+'\n')


if __name__ == '__main__':
    main()
