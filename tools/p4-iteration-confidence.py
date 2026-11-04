"""IC-R001 bounded fixed-work execution. Reuses existing native Job supervision.

Timed exploration requires a recorded owner idle window. Fresh ledger; no old campaign allowance is consumed.
Every launched run charges admission-through-drain wall, even on interruption.
"""
import argparse
import datetime as dt
import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import time

sys.dont_write_bytecode = True
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'docs/research/search/p4-iteration-confidence'
spec = importlib.util.spec_from_file_location('ic_supervision', ROOT / 'tools/p4-multiworker.py')
supervision = importlib.util.module_from_spec(spec)
spec.loader.exec_module(supervision)
support = supervision.support
save = supervision.save


def validate(p, ledger):
    timed=p['kind']=='timed'
    if p['kind'] not in ('fixed-work','timed') or p['workers']!=4:
        raise ValueError('IC diagnostic/timed W4 domain only')
    if not timed and p['games']!=0: raise ValueError('Diagnostic cannot play games')
    maximum=7200 if timed else 300
    if type(p['timeoutSeconds']) is not int or not 0<p['timeoutSeconds']<=maximum:
        raise ValueError('Operating deadline')
    if not timed and (type(p['maximumNodes']) is not int or p['maximumNodes']<=0 or p['requests']<=0):
        raise ValueError('Diagnostic work bounds')
    if ledger['pending'] or ledger['ownedProcesses']:
        raise ValueError('Reconcile prior ownership and charge before restart')
    reserve=ledger.get('planningReserve',{}).get('confirmationWallSeconds',21600) if timed else 0
    if ledger['consumed']['chargedWallSeconds']+p['timeoutSeconds']+30+reserve>ledger['ceilings']['chargedWallSeconds']:
        raise ValueError('Insufficient experimental wall allowance/reserve')
    if timed:
        if p.get('category')!='exploration':raise ValueError('Independent confirmation requires its own prospective admission')
        if type(p['games']) is not int or p['games']!=2*len(p['openingIndices']) or not 2<=p['games']<=128:
            raise ValueError('Bounded paired game count')
        if len(set(p['openingIndices']))!=len(p['openingIndices']):raise ValueError('Duplicate openings')
        reserve_games=ledger.get('planningReserve',{}).get('confirmationGames',480)
        if ledger['consumed']['initiatedTimedGames']+p['games']+reserve_games>ledger['ceilings']['initiatedTimedGames']:
            raise ValueError('Timed game allowance/reserve')
        if p['window'] not in ledger['authorizedIdleWindows']:raise ValueError('No recorded owner idle-window authority')
        window=json.loads((ROOT/p['window']).read_text())
        start=dt.datetime.fromisoformat(window['receivedUtc'].replace('Z','+00:00'))
        end=dt.datetime.fromisoformat(window['hardDeadlineUtc'].replace('Z','+00:00'))
        now=support.utc()
        if window['status']!='authorized' or not start<=now or now+dt.timedelta(seconds=p['timeoutSeconds']+30)>end:
            raise ValueError('Batch does not fit owner idle window with drain margin')


def run(path):
    path=Path(path); p=json.loads(path.read_text(encoding='utf-8'))
    ledger_path=OUT/'budget.json'; ledger=json.loads(ledger_path.read_text())
    validate(p,ledger)
    for name,digest in p['inputs'].items():
        if support.digest(ROOT/name)!=digest: raise ValueError('Changed input '+name)
    run_dir=OUT/'raw'/p['id']; run_dir.mkdir(parents=True,exist_ok=False)
    receipt_path=OUT/(p['id']+'-execution.json')
    if receipt_path.exists(): raise FileExistsError(receipt_path)
    output=run_dir/'observations.jsonl'
    command=[str(ROOT/x[5:]) if x.startswith('ROOT:') else x for x in p['command']]
    command=[str(output) if x=='{output}' else str(run_dir) if x=='{directory}' else x for x in command]
    timed=p['kind']=='timed'
    receipt={'id':p['id'],'status':'admitted','protocol':str(path),'protocolSha256':support.digest(path),
             'command':command,'startedUtc':support.utc().isoformat(),'games':p['games'],'priority':'below-normal'}
    save(receipt_path,receipt)
    ledger['pending']=[{'id':p['id'],'reservedWallSeconds':p['timeoutSeconds']+30,'reservedGames':p['games'],'receipt':str(receipt_path)}]
    save(ledger_path,ledger); admitted=ledger_path.read_bytes()
    start=time.monotonic(); process=None; job=None; error=None; peak=0
    pc=supervision.PcUse()
    try:
        job=support.owned.OwnedJob()
        pc.sample();time.sleep(.5);initial=pc.sample()
        receipt['preflight']=initial
        decision=supervision.resource_decision(initial,4,p['resources'],idle=timed)
        if (OUT/'STOP').exists(): decision['reasons'].append('Owner STOP file')
        receipt['resourceDecision']=decision;save(receipt_path,receipt)
        if decision['reasons']: raise supervision.ResourcePressure('; '.join(decision['reasons']))
        with (run_dir/'console.log').open('xb') as log,(run_dir/'environment.jsonl').open('x') as environment:
            environment.write(json.dumps(initial)+'\n');environment.flush()
            process=subprocess.Popen(command,cwd=ROOT,stdout=log,stderr=subprocess.STDOUT,
                                     creationflags=0x4|0x4000|subprocess.CREATE_NO_WINDOW)
            job.assign(process)
            receipt.update(processId=process.pid,status='running');save(receipt_path,receipt)
            job.resume_initial_thread(process);hot=0
            while True:
                if (OUT/'STOP').exists(): raise supervision.ResourcePressure('Owner STOP file')
                remaining=p['timeoutSeconds']-(time.monotonic()-start)
                if timed:
                    window=json.loads((ROOT/p['window']).read_text())
                    hard=dt.datetime.fromisoformat(window['hardDeadlineUtc'].replace('Z','+00:00'))
                    remaining=min(remaining,(hard-support.utc()).total_seconds()-30)
                if remaining<=0: raise TimeoutError('Diagnostic hard deadline')
                try:
                    code=process.wait(timeout=min(2,remaining));break
                except subprocess.TimeoutExpired:
                    sample=pc.sample();peak=max(peak,supervision.peak_memory(job))
                    sample['peakOwnedCommittedGiB']=peak
                    environment.write(json.dumps(sample)+'\n');environment.flush()
                    decision=supervision.resource_decision(sample,4,p['resources'],running=True,idle=timed)
                    hot=hot+1 if (sample['totalCpuPercent'] or 0)>=(95 if timed else 80) else 0
                    if hot>=3: decision['reasons'].append('CPU contention for three observations')
                    if peak>p['resources']['estimatedPeakGiB']: decision['reasons'].append('Owned memory envelope')
                    if decision['reasons']: raise supervision.ResourcePressure('; '.join(decision['reasons']))
            if code: raise RuntimeError('Diagnostic exit '+str(code))
    except BaseException as problem:
        error=repr(problem)
    finally:
        if job is not None:
            try:
                peak=max(peak,supervision.peak_memory(job));receipt['drain']=support.drain(job,process)
            finally:
                job.close()
                if process is not None and process.poll() is None: process.kill();process.wait(timeout=10)
        else: receipt['drain']={'totalOwnedProcesses':0,'note':'Job creation failed before process launch'}
    receipt.update(wallSeconds=time.monotonic()-start,endedUtc=support.utc().isoformat(),error=error,
                   peakOwnedCommittedGiB=peak,chargedWallSeconds=0)
    if process is not None: receipt['chargedWallSeconds']=receipt['wallSeconds']
    rows=[]
    if timed:
        progress=run_dir/'progress.jsonl';intact=False
        try:
            rows=[json.loads(x) for x in progress.read_text().splitlines()];intact=True
        except (OSError,ValueError):pass
        begun=[x for x in rows if x.get('type')=='pair-start']
        done=[x for x in rows if x.get('type')=='pair-complete']
        initiated=2*len(begun) if intact else p['games'] if process is not None else 0
        passed=not error and intact and rows and rows[-1].get('status')=='passed' and len(done)==len(p['openingIndices'])
        receipt.update(status='passed' if passed else 'refused-before-launch' if process is None else 'failed',
                       initiatedGames=initiated,completedGames=2*len(done),observedNodes=sum(x['nodes'] for x in done),
                       gameCounting='Conservatively charge both games before pair creation; failed/partial pairs preserved')
    else:
        try:
            rows=[json.loads(x) for x in output.read_text().splitlines()]
            summary=rows[-1];nodes=sum(x['measurement']['outcome']['nodes'] for x in rows if x['type']=='request')
            assert not error and summary['type']=='summary' and summary['status']=='passed'
            assert summary['activeHelpersAfterClose']==0 and summary['requests']==p['requests']
            assert summary['nodes']==nodes and 0<=nodes<=p['maximumNodes']
            receipt.update(status='passed',observedNodes=nodes,summary=summary)
        except (OSError,ValueError,KeyError,IndexError,AssertionError):
            receipt.update(status='refused-before-launch' if process is None else 'failed',
                           observedNodes=None,maximumPossibleNodes=0 if process is None else p['maximumNodes'])
    receipt['inputsStable']=all(support.digest(ROOT/n)==d for n,d in p['inputs'].items())
    if not receipt['inputsStable']: receipt['status']='failed'
    receipt['files']={str(f.relative_to(ROOT)):support.digest(f) for f in run_dir.rglob('*') if f.is_file()}
    save(receipt_path,receipt)
    if ledger_path.read_bytes()!=admitted: raise RuntimeError('Unexpected ledger edit; retain pending reservation')
    ledger['consumed']['chargedWallSeconds']+=receipt['chargedWallSeconds']
    if timed:
        c=ledger['consumed'];c['timedWallSeconds']+=receipt['chargedWallSeconds']
        c['initiatedTimedGames']+=receipt['initiatedGames'];c['completedTimedGames']+=receipt['completedGames']
        interrupted=error and any(x in error for x in ('TimeoutError','ResourcePressure','KeyboardInterrupt'))
        c['interruptedTimedGames' if interrupted else 'failedTimedGames']+=receipt['initiatedGames']-receipt['completedGames']
    else:
        ledger['consumed']['fixedWallSeconds']+=receipt['chargedWallSeconds']
        ledger['consumed']['fixedNodes']+=receipt.get('observedNodes') or receipt.get('maximumPossibleNodes',0)
    ledger['pending']=[];ledger['receipts'].append(str(receipt_path.relative_to(ROOT)));save(ledger_path,ledger)
    print(json.dumps({k:receipt[k] for k in ('id','status','chargedWallSeconds','error','drain')}))
    if receipt['status']!='passed': raise SystemExit(75 if isinstance(error,str) and 'ResourcePressure' in error else 1)


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--protocol',required=True)
    run(parser.parse_args().protocol)
