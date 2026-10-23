"""Final evidence/provenance reconciliation; no new games or statistical looks."""
from datetime import datetime,timezone
import importlib.util
import json
from pathlib import Path
import subprocess
from run_diagnostic_v1 import digest, write_new, ROOT, HERE

def read(path):return json.loads(Path(path).read_text(encoding='utf-8-sig'))
def git(*args):return subprocess.check_output(['git',*args],cwd=ROOT,text=True).strip()
report={'status':'passed','verifiedUtc':datetime.now(timezone.utc).isoformat(),
    'headBeforeCloseoutCommit':git('rev-parse','HEAD'),'branch':git('branch','--show-current'),
    'runtimeSourceAnchor':'18f3c4f12a01417a48fb4c044ff3bde2c5b44b2d',
    'version':read(ROOT/'VERSION_STATE.txt'),'versionSha256':digest(ROOT/'VERSION_STATE.txt'),
    'networkSha256':digest(ROOT/'build-assets/nnue-candidate/conventional-cp64.cnnue'),
    'journalPresent':(ROOT/'CODEXLOG_CURRENT.md').exists(),
    'executionReceipts':{},'boundRawFilesVerified':0,'studies':{},
    'clockGamesAudited':0,'clockMatchWallSeconds':0,'clockEngineCpuSeconds':0,
    'diagnosticJvmWallSeconds':0,'diagnosticJvmCpuSeconds':0}
assert report['version']=={'major':0,'minor':0,'patch':0,'build':69}
assert report['versionSha256']=='a1dbe35307fd6501bd66eade261a78a821cc3904e844433fe3fa62d17ebd2d03'
assert report['networkSha256']=='8431626c2fca81e0a1e9ccd5e45ddd8829e6e3282c756c21fad75b06d075bb2b'
assert not git('diff',report['runtimeSourceAnchor'],'--','engine-core','app-uci','app-gui','engine-training')
assert not report['journalPresent']
for path in sorted(HERE.glob('*-execution.json')):
    receipt=read(path);assert receipt['status']=='passed'
    for key,sha in receipt.get('evidence',{}).items():
        target=ROOT/key if (ROOT/key).exists() else HERE/key
        assert target.is_file() and digest(target)==sha,(path,key)
        report['boundRawFilesVerified']+=1
    if 'nativeLifetime' in receipt:
        assert receipt['nativeLifetime']['exitCode']==0
        report['diagnosticJvmCpuSeconds']+=receipt['nativeLifetime']['totalCpuSeconds']
        report['diagnosticJvmWallSeconds']+=receipt.get('elapsedSeconds',receipt.get('wallSeconds',0))
    if 'ownedProcessesRemaining' in receipt:assert receipt['ownedProcessesRemaining']==0
    report['executionReceipts'][path.name]=digest(path)

spec=importlib.util.spec_from_file_location('audit_baseline',ROOT/'tools/search-baseline.py')
b=importlib.util.module_from_spec(spec);spec.loader.exec_module(b)
for name in ('pilot-v1','development-clock-w1-v1','development-clock-w4-v1',
             'adaptive-clock-w1-v1','adaptive-clock-w4-v1'):
    folder=HERE/'raw'/name;run=read(folder/'run.json');cost=read(folder/'process-cost.json')
    assert run['status']==cost['status']=='passed'
    args=run['arguments']
    audit=b.analyze(folder/'games.pgn',args['openings'],args['pairs'],True,True,statistics_enabled=False)
    for key in ('wdl','terminations','wholeRequestResources','games'):assert audit[key]==run['results'][key]
    assert audit['terminations']=={'normal':2*args['pairs']}
    assert all(p['lifetime']['exitCode']==0 for p in cost['processes'])
    report['clockGamesAudited']+=2*args['pairs']
    report['clockMatchWallSeconds']+=run['matchWallSeconds']
    report['clockEngineCpuSeconds']+=sum(cost['cpuSeconds'].values())

binding=read(HERE/'tt-session-v1-binding.json')
for label,entry in binding['studies'].items():
    plan_path=HERE/entry['plan'];assert digest(plan_path)==entry['planSha256']
    spec=importlib.util.spec_from_file_location('frozen_session',plan_path.parent/'inputs/tools/tournament-session.py')
    s=importlib.util.module_from_spec(spec);spec.loader.exec_module(s)
    with s.locked(plan_path.parent/'execution.lock'):
        plan=s.verify(plan_path);blocks=s.reconcile(plan_path,plan)
    if label=='w4':
        assert not blocks and not list(plan_path.parent.glob('block-*.started.json'))
        assert not read(HERE/'tt-session-requal-w4-v1-summary.json')['eligibleForConfirmation']
        report['studies'][label]={'status':'not selected; never launched','pairs':0}
        continue
    summary=read(HERE/f'tt-session-{label}-v1-summary.json')
    assert len(blocks)==len(plan['blocks']) and summary['status']=='passed'
    assert digest(plan_path.parent/'export/complete.json')==summary['exportSha256']
    for path in (plan_path.parent/'sessions').glob('*.json'):
        session=read(path);assert session['status'] in ('paused','complete')
        assert all(x['exitCode']==0 and x['ownedProcessesRemaining']==0 for x in session['completedBlocks'])
    report['clockGamesAudited']+=2*plan['pairs']
    report['clockMatchWallSeconds']+=summary['matchWallSeconds']
    report['clockEngineCpuSeconds']+=sum(summary['cpuSeconds'].values())
    report['studies'][label]={'status':'complete and reaudited','pairs':plan['pairs'],
                            'summarySha256':digest(HERE/f'tt-session-{label}-v1-summary.json')}
assert report['clockGamesAudited']==584
report['limits']='584 legal/terminal/history/request-audited games include8 operational pilot games; the earlier adaptive W4 screen has a separately recorded resource concern. Totals measure research cost, not pooled strength.256 W1 games are independent confirmation. Diagnostic JVM totals exclude Gradle/tests, Python analysis, selection, preflight outside those JVMs and idle availability waits. No new statistical looks, functional tests, GUI/Mac verification, deployment or promotion in closeout.'
report['canon']={name:{'revision':revision,'sha256':digest(ROOT/'source'/name)} for name,revision in (
    ('CHESS_SEARCH_CONTRACT.md','P3-R018'),('CHESS_SEARCH_RESEARCH_FRONTIER.md','P3-F018'))}
report['closeoutScriptSha256']=digest(__file__)
write_new(HERE/'closeout-v1.json',report)
print(json.dumps({k:v for k,v in report.items() if k not in ('executionReceipts','canon')},indent=2))
