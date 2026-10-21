"""Bind read-only G9 frozen-checkpoint diagnostics before strength selection."""
import copy
import hashlib
import json
from pathlib import Path

BASE = Path('docs/research/brn-successor-cglhw')
def read(p): return json.loads(p.read_text(encoding='utf-8-sig'))
def save(p, value):
    if p.exists():
        assert read(p) == value, ('Preserve different prior evidence', str(p))
        return
    with p.open('x', encoding='utf-8', newline='\n') as f:
        json.dump(value, f, indent=2); f.write('\n')

def main():
    report = read(BASE/'wave9-trained-checkpoints.json')
    latest = read(BASE/'wave9-trained-model-plan.json')
    runtime = read(BASE/'wave9-runtime.json')
    for name, digest in runtime['jars'].items():
        assert hashlib.sha256((Path(runtime['runtime'])/name).read_bytes()).hexdigest() == digest
    models = [latest['models'][0]]
    for row, spec in zip(report['lineages'], latest['models'][1:]):
        for key, suffix in [('initial','g168'),('latest','g176'),('best','best')]:
            m = copy.deepcopy(spec); cp = row[key]
            m.update(name=spec['name']+'-'+suffix, checkpoint=cp['id'], networkSha256=cp['networkSha256'])
            models.append(m)
    plan = BASE/'wave9-maturity-model-plan.json'
    save(plan, dict(id='wave9-fixed-panel-maturity-v1', models=models))
    prefix_args = [runtime['java'], '-Xmx768m','-XX:ActiveProcessorCount=1',
                   '--add-modules=jdk.incubator.vector','-cp',runtime['runtime']+'/*']
    def request(label, main, args):
        prefix = (BASE/'raw/wave9'/label).resolve()
        save(Path(str(prefix)+'-request.json'),dict(args=prefix_args+[main]+[str(a) for a in args],
             prefix=str(prefix), cwd=str(Path.cwd()), timeoutSeconds=180))
    for label, source in [('fresh5','raw/wave5/fresh-data-01'),('old','raw/wave2/data-01')]:
        # Preserve consumed panels, and fail closed if unavailable.
        names = ['bt4','cp'] if label == 'fresh5' else ['bt4-fresh','lichess-sealed']
        paths = [BASE/source/(s+'-positions.jsonl') for s in names]
        for p in paths: assert p.is_file(), p
        output = BASE/'raw/wave9'/('trained-predictions-'+label+'-01')
        request(output.name, 'com.ohinteractive.seedv6.tools.eval.BrnResidualAssessment',
                [plan.resolve(),*[p.resolve() for p in paths],output.resolve()])
    for label, m in zip(['affine','quad','cubic'],latest['models'][1:4]):
        output=BASE/'raw/wave9'/('trained-cache-'+label+'-01.json')
        request(output.stem,'com.ohinteractive.seedv6.tools.eval.BrnGatedCacheProbe',
                [(BASE/'wave9-trained-model-plan.json').resolve(),
                 (BASE/'raw/wave8/degree-node').resolve(),
                 (BASE/'raw/wave5/fresh-data-01/bt4-positions.jsonl').resolve(),
                 output.resolve(),m['name']])
    print('Prepared maturity and three actual trained cache probes; no training or owner writes')

if __name__=='__main__': main()
