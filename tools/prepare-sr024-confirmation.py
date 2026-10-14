"""Freeze a prospectively justified SR-024 protocol from a closed development run.

Samples one fresh partition-1 permutation, then slices it into predeclared stages.
No score filtering, repeated opening parsing for sampling, or outcome-dependent looks.
"""
import argparse
import datetime
import importlib.util
import json
from pathlib import Path


def prepare(a):
    development=Path(a.development).resolve();run=json.loads((development/'run.json').read_text())
    assert run['status']=='passed' and run['inputsStable']
    runner=Path(a.runner_module).resolve();assessor=runner.with_name('sr024-confirmation.py')
    spec=importlib.util.spec_from_file_location('baseline',runner.with_name('search-baseline.py'))
    b=importlib.util.module_from_spec(spec);spec.loader.exec_module(b)
    looks=[int(n) for n in a.looks.split(',')]
    assert looks==sorted(set(looks)) and looks[0]>=2 and a.contrast_index>=1
    out=Path(a.out).resolve();assert not out.exists()
    stem=out.with_suffix('');excluded=[]
    for p in Path('docs/research/search').rglob('*.selection.json'):
        data=json.loads(p.read_text(encoding='utf-8-sig'))
        if data.get('partition')==1:excluded.append(str(p.resolve()))
    excluded.append(str(Path(run['arguments']['openings']).with_suffix('.selection.json').resolve()))
    excluded=sorted(set(excluded))
    sample=stem.with_name(stem.name+'-all.pgn')
    b.select_openings(a.book,sample,looks[-1],a.seed,1,excluded)
    parent=sample.with_suffix('.selection.json');selection=json.loads(parent.read_text())
    games=[]
    with sample.open(encoding='utf8') as stream:
        while (game:=b.chess.pgn.read_game(stream)) is not None:games.append(str(game))
    assert len(games)==looks[-1]
    stages=[];offset=0
    for index,end in enumerate(looks,1):
        path=stem.with_name(stem.name+f'-stage{index}.pgn');assert not path.exists()
        path.write_text('\n\n'.join(games[offset:end])+'\n',encoding='utf8',newline='\n')
        receipt=path.with_suffix('.selection.json')
        b.save(receipt,dict(selection,count=end-offset,positionKeys=selection['positionKeys'][offset:end],
            outputSha256=b.digest(path),parentSelection=str(parent),parentSelectionSha256=b.digest(parent),
            slice=[offset,end],selection='Prospective contiguous slice of one frozen uniform permutation; no outcome filtering'))
        stages.append({'pairs':end-offset,'openings':str(path),'selection':str(receipt),'sha256':b.digest(path)})
        offset=end
    runtime=dict(run['filesBefore']);runtime.pop(str(Path(run['arguments']['openings']).resolve()))
    assert str(runner) in runtime and all(b.digest(p)==h for p,h in runtime.items())
    expected={k:v for k,v in run['arguments'].items() if k not in ('openings','out','pairs')}
    expected.update(tc=a.tc,seed=a.runner_seed,timeout=28800)
    frozen=dict(runtime)
    extra=excluded+[str(sample),str(parent),str(assessor),str(Path(__file__).resolve()),str(Path(a.executor).resolve()),
                    str(Path(a.source_manifest).resolve()),str(Path(a.book).resolve()),str(development/'run.json')]
    extra += [s[k] for s in stages for k in ('openings','selection')]
    frozen.update({p:b.digest(p) for p in extra})
    protocol={'design':'staged_normal','createdUtc':datetime.datetime.now(datetime.timezone.utc).strftime('%Y-%m-%dT%H:%M:%SZ'),
        'hypothesis':'Positive clock-strength gain against accepted production within the stated worker domain.',
        'contrastIndex':a.contrast_index,'familyAlpha':.05,'looks':looks,'stages':stages,'partition':1,
        'minimumPairsEachSideForNormal':20,'requiredSensitivitySigma':2.5,'futilityPowerThreshold':.10,
        'runnerModule':str(runner),'assessorModule':str(assessor),'assessorSha256':b.digest(assessor),
        'executorSha256':b.digest(a.executor),'expectedArguments':expected,
        'expectedPreflightFields':{side:dict(x.split('=',1) for x in run[side+'Preflight']['identity'].split() if '=' in x) for side in ('baseline','candidate')},
        'excludedSelections':excluded,'runtimeFiles':runtime,'frozenFiles':frozen,
        'sourceManifest':str(Path(a.source_manifest).resolve()),'rationale':a.rationale,
        'stopping':'Evaluate only completed cumulative looks. Positive/negative require adjusted paired-normal and 2.5-SE agreement when eligible; otherwise bounded inference. Plug-in conditional power below .10 stops for computational futility. Maximum without a boundary is inconclusive. Never extend after a stopping decision.',
        'limits':'Approximate paired-normal model; adjusted Hoeffding sensitivity also reported. Development is excluded. Futility reduces power and does not prove inferiority, equivalence or absence of small gains. No inference across worker counts, weights, hosts or clocks.'}
    b.save(out,protocol)
    print(json.dumps({'protocol':str(out),'sha256':b.digest(out),'looks':looks,'freshPositions':looks[-1],
                      'availableAfterExclusions':selection['availableDistinctPositions'],'excludedPositions':selection['excludedDistinctPositions']}))


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    for name in ('development','runner-module','source-manifest','executor','book','out','looks','rationale'):p.add_argument('--'+name,required=True)
    p.add_argument('--contrast-index',type=int,required=True);p.add_argument('--seed',type=int,required=True)
    p.add_argument('--runner-seed',type=int,default=20261024);p.add_argument('--tc',default='10+0.1')
    prepare(p.parse_args())
