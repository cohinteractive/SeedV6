"""Freeze one selected Phase 3 comparison; never launch games or replace evidence.

Invoke from this repository with the validated research Python after finalist
selection. The comparison JSON must state selectionRationale and identify the
actual pre-experiment champion, selected candidate and existing opening receipt.
The copied script is provenance only; preparation uses this repository location.
"""
import argparse
from datetime import datetime,timezone
import hashlib
import importlib.util
import json
from pathlib import Path
import shutil
import subprocess


def digest(path):
    with Path(path).open('rb')as stream:return hashlib.file_digest(stream,'sha256').hexdigest()


def read(path):return json.loads(Path(path).read_text(encoding='utf-8-sig'))


def prepare(config_path,label,seed):
    root=Path(__file__).resolve().parent;repo=root.parents[3]
    config_path=Path(config_path).resolve();comparison=read(config_path);args=comparison['arguments']
    workers=1 if label=='w1'else 4;index=1 if label=='w1'else 2
    if args['baseline_workers']!=workers or args['candidate_workers']!=workers or args['baseline_policy']!='0':
        raise ValueError('Selected comparison must use the champion in the declared worker domain')
    if not comparison.get('selectionRationale'):raise ValueError('Record finalist selection before freezing')
    if not comparison.get('confirmationForecast'):raise ValueError('Record decision-value and resource forecast before freezing')
    if args['tc']!='10+0.1' or args['concurrency']!=1 or args.get('background')is not True:
        raise ValueError('Wrong clock/resource domain')
    runtime_trees={};known=read(root/'integrated-v1-manifest.json')['frozenRuntimeTrees']
    for side in ('baseline','candidate'):
        install=Path(args[side])
        if not (install/'lib').is_dir()or not (install/'research-launcher.properties').is_file():
            raise ValueError('Frozen research installation unavailable: '+side)
        runtime_trees[side]={str(p.relative_to(install)):digest(p)for p in sorted(install.rglob('*'))if p.is_file()}
        if runtime_trees[side]!=known[side]:raise ValueError('Unexplained runtime identity change: '+side)
    if not Path(args['java']).is_file()or not Path(args['runner']).is_file()or not Path(args['store']).is_dir():
        raise ValueError('Local JDK, game runner or model store unavailable')
    source_selection=Path(args['openings']).with_suffix('.selection.json');opening_record=read(source_selection)
    book=Path(opening_record['source'])
    if not book.is_file() or digest(book)!=opening_record['sourceSha256']:
        raise ValueError('Original opening source unavailable or changed')
    out=root/('confirmation-v1-'+label);bundle=root/'raw'/('confirmation-v1-'+label+'-tools')
    if out.exists()or bundle.exists():raise ValueError('Never replace an existing protocol or reuse its contrast')
    excluded=[]
    for path in sorted((repo/'docs/research/search').rglob('*.selection.json')):
        if 'raw'in path.parts:continue
        data=read(path)
        if 'positionKeys'in data:excluded.append(str(path.resolve()))
    names=('p3-information-confirmation.py','sr032-confirmation.py','sr024-confirmation.py',
           'tournament-session.py','p3-information-match.py','sr032-match.py','sr021-match.py',
           'search-baseline.py','sr024-process-cost.py','sr036-process-cost.py','tournament-process.py')
    if not all((repo/'tools'/name).is_file()for name in names):raise ValueError('Missing maintained dependency')
    authority={}
    for name,revision in [('CHESS_SEARCH_CONTRACT.md','P3-R003'),('CHESS_SEARCH_RESEARCH_FRONTIER.md','P3-F002')]:
        path=repo/'source'/name
        if '**'+revision+'**'not in path.read_text(encoding='utf8'):raise ValueError('Reconcile changed canon before freezing')
        authority[str(path)]={'revision':revision,'sha256':digest(path)}
    source_head=subprocess.check_output(['git','rev-parse','HEAD'],cwd=repo,text=True).strip()
    out.mkdir();bundle.mkdir()
    for name in names:shutil.copy2(repo/'tools'/name,bundle/name)
    shutil.copy2(__file__,bundle/'prepare_confirmation.py')
    spec=importlib.util.spec_from_file_location('frozen_p3_confirmation',bundle/'p3-information-confirmation.py')
    c=importlib.util.module_from_spec(spec);spec.loader.exec_module(c);t=c.t;b=c.m.b
    all_openings=out/'all-openings.pgn'
    b.select_openings(book,all_openings,512,seed,1,excluded)
    selection=read(all_openings.with_suffix('.selection.json'))
    with all_openings.open(encoding='utf8')as stream:games=[b.chess.pgn.read_game(stream)for _ in range(512)]
    protocol_path=out/'protocol.json';stages=[]
    for number,(first,last)in enumerate(((0,128),(128,512)),1):
        openings=out/f'stage{number}-openings.pgn'
        openings.write_bytes(('\n\n'.join(str(g)for g in games[first:last])+'\n').encode())
        receipt=openings.with_suffix('.selection.json')
        t.save(receipt,dict(selection,count=last-first,positionKeys=selection['positionKeys'][first:last],
                            outputSha256=digest(openings),slice=[first,last],parentSelection=str(all_openings.with_suffix('.selection.json'))))
        stage_args=dict(args,openings=str(openings),pairs=last-first,seed=seed)
        config=dict(comparison,purpose='confirmatory',assessmentProtocol=protocol_path.name,
                    assessmentPlan='Only prospective cumulative 128/512-pair looks. Existing SR-024 paired-normal rule with family alpha .05/2^contrastIndex, Bonferroni across looks, minimum 20 pairs on each side, 2.5-SE sensitivity, bounded-pair reporting and nonbinding computational futility. No block looks, post-close extension, retuning or selective replay. Explicit owner acceptance remains separate.',
                    blockPairs=8,blockLimitSeconds=1200,arguments=stage_args)
        config_file=out/f'stage{number}-config.json';t.save(config_file,config)
        study=root/'raw'/f'confirm-v1-{label}-stage{number}'
        stages.append({'pairs':last-first,'openings':str(openings),'selection':str(receipt),
                       'config':str(config_file),'plan':str(study/'plan.json'),'export':str(study)+'-export'})
    frozen=list(bundle.iterdir())+list(out.iterdir())+[config_path,book,source_selection]+[Path(p)for p in excluded]
    frozen+=list(root.glob('*summary.json'))
    frozen+=[root/'integrated-v1-manifest.json',root/'uci-preflight-v1.json',root/'uci-preflight-w4-v1.json']
    expected={k:args[k]for k in ('baseline_policy','candidate_policy','baseline_workers','candidate_workers','tc','concurrency','background')}
    expected.update(seed=seed,operational=True,timeout=960,
                    java=str(Path(args['java']).resolve()),store=str(Path(args['store']).resolve()))
    p={'design':'p3-information-paired-stages-v1','family':'P3-O1O2','contrastIndex':index,
       'createdUtc':datetime.now(timezone.utc).isoformat(),'sourceHead':source_head,'authorityAtFreeze':authority,
       'executorSha256':digest(c.__file__),'frozenFiles':{str(p.resolve()):digest(p)for p in frozen if p.is_file()},
       'looks':[128,512],'familyAlpha':c.m.rules.FAMILY_ALPHA,
       'minimumPairsEachSideForNormal':c.m.rules.MIN_EACH_SIDE,'requiredSensitivitySigma':c.m.rules.SENSITIVITY_SIGMA,
       'futilityPowerThreshold':c.m.rules.FUTILITY_POWER,'expectedAdapter':'p3-information-match.py',
       'expectedArguments':expected,'runtimeTrees':runtime_trees,
       'excludedSelections':excluded,'stages':stages,'execution':str(root/'raw'/('confirmation-v1-'+label+'-execution')),
       'selectionRationale':comparison['selectionRationale'],
       'forecast':comparison['confirmationForecast'],
       'resourcePlan':'One game at a time, BelowNormal, unchanged worker count and 10+0.1. Eight-pair restarts retain development domain. Twenty-minute block watchdog is an operational allowance, not a search clock change. Admit only complete blocks within explicit stop-before windows; observe shared-workstation headroom and preserve abnormal blocks. Assess the first scheduled look before choosing continuation windows. No automatic disruptive 12-hour session.',
       'limits':'Approximate paired inference on one host, pinned evaluator and clock; selection-biased development is excluded. Host samples cannot reconstruct unsampled load. Clock equality does not imply CPU equality. Worker domains are separate. No automatic champion adoption.',
       'versionFinalization':'Deferred: original integrity-failure reservation remains untouched. Frozen source/binary hashes identify research; embedded build 35 is not a finalized research version.'}
    t.save(protocol_path,p);c.verify(protocol_path)
    for stage in stages:t.prepare(stage['config'],Path(stage['plan']).parent)
    c.verify(protocol_path)
    return {'protocol':str(protocol_path),'sha256':digest(protocol_path),'executor':str(bundle/'p3-information-confirmation.py'),
            'looks':p['looks'],'contrastIndex':index,'gamesAtFirstLook':256,'gamesAtMaximum':1024}


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--comparison-config',required=True);parser.add_argument('--label',choices=('w1','w4'),required=True)
    parser.add_argument('--seed',type=int,required=True);a=parser.parse_args()
    print(json.dumps(prepare(a.comparison_config,a.label,a.seed),indent=2))
