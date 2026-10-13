"""One-time prospective SR-036 v4 confirmation freeze. Refuses existing outputs."""
from datetime import datetime, timezone
import importlib.util
import json
from pathlib import Path
import sys

spec=importlib.util.spec_from_file_location('baseline',Path(__file__).with_name('search-baseline.py'))
b=importlib.util.module_from_spec(spec);spec.loader.exec_module(b)


def absolute(p):return str(Path(p).resolve())


def main():
    evidence=Path('docs/research/search/sr036-phase2-2026-10-14')
    prior=[];unkeyed=[]
    for p in sorted(Path('docs/research/search').rglob('*.selection.json')):
        r=json.loads(p.read_text(encoding='utf-8-sig'))
        (prior if 'positionKeys' in r and 'count' in r else unkeyed).append(p)
    out=evidence/'confirmation';out.mkdir(exist_ok=False)
    corpus=[evidence/'diagnostic-corpus.tsv',Path('docs/research/search/sr007-phase2-2026-10-13/integrity-corpus.tsv')]
    keys=set()
    for p in corpus:
        for line in p.read_text().splitlines():
            if not line.strip() or line.startswith('#'):continue
            f=line.split('\t');board=b.chess.Board(f[1])
            if len(f)>2 and f[2].strip():
                for move in f[2].split():board.push_uci(move)
            keys.add(' '.join(board.fen().split()[:4]))
    diagnostic=out/'diagnostic-exclusions.selection.json'
    b.save(diagnostic,{'count':len(keys),'positionKeys':sorted(keys),
                       'sourceInputs':{absolute(p):b.digest(p) for p in corpus}})
    excluded=prior+[diagnostic]
    book=Path('app/build/search-baseline/tools/openings/8moves_v3.pgn')
    master=out/'reserved-openings.pgn'
    b.select_openings(book,master,1088,2026101403,1,excluded)
    selection=json.loads(master.with_suffix('.selection.json').read_text())
    games=[]
    with master.open() as stream:
        while (g:=b.chess.pgn.read_game(stream)) is not None:games.append(str(g))
    assert len(games)==1088
    groups={};offset=0
    for name,counts in (('primary',(128,256,640)),('inherited',(64,))):
        groups[name]=[]
        for index,count in enumerate(counts,1):
            p=out/(name+'-openings-'+str(index)+'.pgn')
            p.write_text('\n\n'.join(games[offset:offset+count])+'\n',encoding='utf-8',newline='\n')
            receipt=p.with_suffix('.selection.json')
            b.save(receipt,{'count':count,'partition':1,'seed':2026101403,
                           'positionKeys':selection['positionKeys'][offset:offset+count],
                           'outputSha256':b.digest(p),'sourceSelection':absolute(master.with_suffix('.selection.json')),
                           'sourceSelectionSha256':b.digest(master.with_suffix('.selection.json')),
                           'selection':'Fixed contiguous slice of the prospectively reserved random sample','offset':offset})
            groups[name].append({'pairs':count,'openings':absolute(p),'selection':absolute(receipt),
                                 'sha256':b.digest(p),'out':absolute(out/(name+'-stage'+str(index)))})
            offset+=count
    java=absolute('C:/java/jdk-21/bin/java.exe')
    fastchess=absolute('app/build/search-baseline/tools/fastchess/fastchess-windows-x86-64/fastchess.exe')
    runner=Path('app/build/sr036-runner-v5/sr036-match.py').resolve()
    assessor=Path('app/build/sr036-confirmation-tools-v1/sr036-confirmation.py').resolve()
    rule=assessor.with_name('sr007-confirmation.py')
    store=absolute('E:/SeedV6-Networks/BRN-Pair2/235d0688-e060-44e3-8f1e-119c39bc8017')
    candidate=absolute('app/build/sr036-p2-v4')
    model=Path(store)/'checkpoints'/b.CHECKPOINT
    assert b.digest(model/'network.brn-pair2')==b.NETWORK_SHA256
    assert b.digest(Path(candidate)/'lib/app.jar')=='e600a749db510db01a329b05b9ebd0201a464584b99a0a626b8dcbde221f3e35'
    accepted=absolute('app/build/sr005-p2-production-v2')
    assert b.digest(Path(accepted)/'lib/app.jar')=='dfc9d9cb3e5d372eb40015fcc5b53093c5841d09ddad2960cc7c54f21bb4abf8'
    common={absolute(p):b.digest(p) for p in [Path(__file__),Path(b.__file__),book,*excluded,*corpus,
                  master,master.with_suffix('.selection.json'),assessor,rule,assessor.with_name('search-baseline.py')]}
    for stages in groups.values():
        for stage in stages:
            for key in ('openings','selection'):common[stage[key]]=b.digest(stage[key])
    runtime={absolute(p):b.digest(p) for p in [Path(java),Path(fastchess),runner,runner.with_name('search-baseline.py'),
                                              *[p for p in model.iterdir() if p.is_file()]]}
    runtime.update(b.installation_hashes(candidate))
    for name in ('primary','inherited'):
        baseline=accepted if name=='primary' else absolute('app/build/sr036-inherited-control-v4')
        workers=1 if name=='primary' else 4
        arguments={'baseline':baseline,'candidate':candidate,'baseline_policy':'off','baseline_workers':workers,
                   'candidate_policy':'root_reuse','candidate_workers':4,'store':store,'runner':fastchess,
                   'java':java,'tc':'10+0.1','concurrency':1,'seed':20261014,'timeout':86400}
        actual_runtime=runtime|b.installation_hashes(baseline)
        fields={'checkpoint':b.CHECKPOINT,'sha256':b.NETWORK_SHA256,'mapping':'BRN_PAIR2_MATERIAL_RESIDUAL_QUARTER_V1',
                'ttMiB':'64','nmp':'off','check':'off','aspiration':'off','build':'35'}
        preflight={'baseline':fields|{'source':'450988455af3' if name=='primary' else 'd65207820788',
                      'workers':str(workers),'leaves':'unpruned' if workers==1 else 'static',
                      'reuse':'effort' if workers==1 else 'off','lmr':'quiet' if workers==1 else 'off',
                      'lmp':'window' if workers==1 else 'off','mode':'production'},
                   'candidate':fields|{'source':'d65207820788','workers':'4','leaves':'unpruned','reuse':'effort',
                      'lmr':'quiet','lmp':'window','internal':'off','mode':'research','concurrency':'root_reuse',
                      'rootHistory':'seeded_private','experimental':'true'}}
        stages=groups[name]
        for stage in stages:
            cmd=[absolute(sys.executable),'-B',str(runner)]
            for k,v in (arguments|{k:stage[k] for k in ('pairs','openings','out')}).items():
                cmd+=['--'+k.replace('_','-'),str(v)]
            stage['command']=cmd
        protocol={'feature':'SR-036 Phase2 concurrency extension','family':'SR036-root-reuse-v4',
                  'contrastIndex':1 if name=='primary' else 2,'familyAlpha':.05,
                  'design':'staged_normal' if name=='primary' else 'fixed_bounded',
                  'looks':[128,384,1024] if name=='primary' else [64],
                  'futilityPowerThreshold':.10,'minimumPairsEachSideForNormal':20,'requiredSensitivitySigma':2.5,
                  'partition':1,'frozenAtUtc':datetime.now(timezone.utc).isoformat(timespec='seconds'),
                  'assessorPath':str(assessor),'assessorSha256':b.digest(assessor),'statisticalRuleSha256':b.digest(rule),
                  'expectedArguments':arguments,'expectedPreflightFields':preflight,'stages':stages,
                  'runtimeFiles':actual_runtime,'frozenFiles':common|actual_runtime,
                  'excludedSelections':[absolute(p) for p in excluded],
                  'unkeyedHistoricalSelectionFiles':[absolute(p) for p in unkeyed],
                  'resources':'Ryzen5500 six physical/twelve logical cores, Windows11, Java21+35-2513; one game, candidate4 vs baseline1/4 as specified, 256-512MiB heap per JVM, ActiveProcessorCount6 (not affinity), 64MiB requested shared TT per engine, no book/tablebase/ponder, three depth4 warmup requests, persistent engines with per-game newGame; no overlapping CPU-heavy research.',
                  'interpretation':'No development pooling or finalist tuning. Contrasts answer different questions and use disjoint pairs. Independent opening-pair sampling is an approximation to the stated population, not training-source independence or universal platform/time-control strength. Primary staged normal inference requires bounded sensitivity disclosure; inherited contrast uses fixed bounded-pair inference. Success requires its applicable interval wholly above .5, negative wholly below, otherwise apply only the frozen futility/final-inconclusive rule. No extension after a closed decision.',
                  'planning':'Report records effect/variance/throughput forecasts and resource rationale. Primary first128 pairs roughly2h; maximum1024 roughly16h if warranted by the frozen rule. Fixed inherited64 roughly1h; large development effect supports bounded confirmation. Neither sample size is a universal quota or Elo gain floor.'}
        b.save(out/(name+'-protocol.json'),protocol)
    print(json.dumps({'status':'frozen_before_games','excludedKeyedSelections':len(excluded),
                      'unkeyedHistoricalSelections':len(unkeyed),'availablePositions':selection['availableDistinctPositions'],
                      'reservedPairs':1088,'protocols':[absolute(out/(x+'-protocol.json')) for x in groups]},indent=2))


if __name__=='__main__':main()
