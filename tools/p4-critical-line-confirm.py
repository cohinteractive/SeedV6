"""One prospective CLV confirmation: frozen admission, owned drain and pair accounting.

Reuses the existing Windows Job, resource guards, game worker and independent
game/request audits. Never displays interim scores. First pair-start consumes the
sole attempt; a lost progress journal after launch is charged conservatively.
"""
import argparse
import datetime as dt
import importlib.util
import json
import math
from pathlib import Path
import random
import statistics
import subprocess
import sys
import time

sys.dont_write_bytecode=True
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'docs/research/search/p4-critical-line'
spec=importlib.util.spec_from_file_location('clv_confirmation_support',ROOT/'tools/p4-multiworker-match.py')
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
support=m.mw.support
save=m.mw.save
GATE=dict(minimumScore=.53,lowerBoundExclusive=.50,confidence=.95,method='paired-normal',z=1.96)


def validate(p,b,now):
    if (p.get('campaign')!='CLV-R001' or p['category']!='confirmation' or p['kind']!='timed'
            or p['workers']!=4 or p['baseline']!='EXACT96' or p['candidate']!='C1'
            or p['tc']!='10+0.1' or p['maxMoves']!=300 or p['firstColorPolicy']!='alternate'
            or p['games']!=640 or p['openingIndices']!=list(range(320))):
        raise ValueError('Frozen320-pair CLV-C1/exact96 W4 domain required')
    c=p['confirmation'];used=b['consumed'];limits=b['ceilings']
    if (c['gate']!=GATE or c['interimScoreAccess']!='prohibited'
            or c['failureRule']!='incomplete-attempt-no-pass-no-replay'
            or c['bootstrap']!=dict(samples=10000,seed=2026110617)):
        raise ValueError('Changed prospective confirmation conditions')
    if b['pending'] or b['ownedProcesses']:raise ValueError('Reconcile existing ownership/admission before execution')
    if used['confirmationAttempts']!=0 or used['confirmationGames']!=0:
        raise ValueError('Independent attempt or game reservation already consumed')
    if used['initiatedTimedGames']+640>limits['initiatedTimedGames'] or limits['confirmationGames']<640:
        raise ValueError('Timed game ceiling')
    if p['timeoutSeconds']!=28800 or used['chargedWallSeconds']+28830>limits['chargedWallSeconds']:
        raise ValueError('Confirmation wall ceiling')
    if p['window'] not in b['authorizedIdleWindows']:raise ValueError('No separately authorized confirmation idle window')
    w=json.loads((ROOT/p['window']).read_text(encoding='utf8'))
    start=dt.datetime.fromisoformat(w['receivedUtc'].replace('Z','+00:00'))
    end=dt.datetime.fromisoformat(w['hardDeadlineUtc'].replace('Z','+00:00'))
    if (w['status']!='authorized' or p['id'] not in w['allowedProtocols']
            or w['maximumTimedGames']<640 or not start<=now or now+dt.timedelta(seconds=28830)>end):
        raise ValueError('Confirmation does not fit the actual owner scope/window')
    verify_evidence(p)
    return end


def verify_evidence(p):
    """Read-only preparation check; grants no window or execution authority."""
    c=p['confirmation']
    for name,digest in p['inputs'].items():
        if support.digest(ROOT/name)!=digest:raise ValueError('Changed input '+name)
    def evidence(name):
        if name not in p['inputs']:raise ValueError('Unpinned evidence '+name)
        return json.loads((ROOT/name).read_text(encoding='utf8'))
    selected=evidence(c['selection']);fresh=evidence(c['freshnessAudit']);basis=evidence(c['exploratoryEvidence'])
    basis_protocol=evidence(c['exploratoryProtocol'])
    if (selected['count']!=320 or len(set(selected['positionKeys']))!=320
            or selected['outputSha256']!=p['inputs'][p['openings']]
            or fresh['status']!='passed' or fresh['overlaps']!=0
            or fresh['selectionSha256']!=p['inputs'][c['selection']]
            or basis['status']!='complete exploratory analysis' or basis['games']!=64):
        raise ValueError('Unqualified selection, freshness or exploratory basis')
    if basis['protocolSha256']!=p['inputs'][c['exploratoryProtocol']] or any(
            basis_protocol[k]!=p[k] for k in ('runtime','baseline','candidate','workers','tc','maxMoves')):
        raise ValueError('Exploratory domain/identity mismatch')
    for name in c['qualificationEvidence']:
        if evidence(name)['status']!='passed':raise ValueError('Incomplete technical qualification')
    minimum=math.ceil(1.25*basis['chargedWallSeconds']*10+120)
    if p['timeoutSeconds']<minimum:raise ValueError('Insufficient prospective throughput margin')
    keys=[]
    with (ROOT/p['openings']).open(encoding='utf8') as f:
        while (game:=m.b.chess.pgn.read_game(f)) is not None:
            if game.errors or game.headers.get('Result')!='*' or game.end().board().is_game_over(claim_draw=True):
                raise ValueError('Illegal/non-input confirmation opening')
            keys.append(' '.join(game.end().board().fen().split()[:4]))
    if keys!=selected['positionKeys']:raise ValueError('Opening order/identity mismatch')
    return dict(pairs=320,minimumTimeoutSeconds=minimum,openingOrderVerified=True)


def progress(path):
    try:return [json.loads(x) for x in path.read_text(encoding='utf8').splitlines()],True
    except (OSError,ValueError):return [],False


def consume_attempt(b,p,receipt,initiated):
    if initiated<=0:return False
    used=b['consumed']
    if used['confirmationAttempts']:
        if b.get('confirmationAttempt',{}).get('protocolSha256')!=receipt['protocolSha256']:
            raise ValueError('Attempt already attributed to another protocol')
        return False
    used['confirmationAttempts']=1
    b['confirmationAttempt']=dict(id=p['id'],protocol=receipt['protocol'],protocolSha256=receipt['protocolSha256'],
        firstInitiatedObservedUtc=support.utc().isoformat(),resume='none-after-first-pair-start')
    return True


def run(path):
    path=Path(path);p=json.loads(path.read_text(encoding='utf8'));lp=OUT/'budget.json';b=json.loads(lp.read_text())
    hard=validate(p,b,support.utc());directory=OUT/'raw'/p['id'];directory.mkdir(exist_ok=False)
    rp=OUT/(p['id']+'-execution.json')
    if rp.exists():raise FileExistsError(rp)
    command=[str(ROOT/x[5:]) if x.startswith('ROOT:') else x for x in p['command']]
    command=[str(directory) if x=='{directory}' else x for x in command]
    receipt=dict(id=p['id'],status='admitted',protocol=str(path),protocolSha256=support.digest(path),
        command=command,startedUtc=support.utc().isoformat(),games=640,priority='below-normal')
    save(rp,receipt);b['pending']=[dict(id=p['id'],reservedWallSeconds=28830,reservedGames=640,receipt=str(rp))]
    save(lp,b);admitted=lp.read_bytes();start=time.monotonic();job=process=None;error=None;peak=0;pc=m.mw.PcUse()
    try:
        job=support.owned.OwnedJob();pc.sample();time.sleep(.5);sample=pc.sample()
        decision=m.mw.resource_decision(sample,4,p['resources'],idle=True)
        if (OUT/'STOP').exists():decision['reasons'].append('Owner STOP file')
        receipt.update(preflight=sample,resourceDecision=decision);save(rp,receipt)
        if decision['reasons']:raise m.mw.ResourcePressure('; '.join(decision['reasons']))
        with (directory/'console.log').open('xb') as log,(directory/'environment.jsonl').open('x') as environment:
            environment.write(json.dumps(sample)+'\n');environment.flush()
            process=subprocess.Popen(command,cwd=ROOT,stdout=log,stderr=subprocess.STDOUT,
                creationflags=0x4|0x4000|subprocess.CREATE_NO_WINDOW)
            job.assign(process);receipt.update(status='running',processId=process.pid);save(rp,receipt)
            job.resume_initial_thread(process);hot=0
            while True:
                if (OUT/'STOP').exists():raise m.mw.ResourcePressure('Owner STOP file')
                remaining=min(28800-(time.monotonic()-start),(hard-support.utc()).total_seconds()-30)
                if remaining<=0:raise TimeoutError('Confirmation deadline')
                # Input-only first-start observation; do not parse results during play.
                first=m.first_pair_start(directory/'progress.jsonl')
                if first and consume_attempt(b,p,receipt,2):
                    if lp.read_bytes()!=admitted:raise RuntimeError('Unexpected ledger edit')
                    save(lp,b);admitted=lp.read_bytes()
                try:code=process.wait(timeout=min(2,remaining));break
                except subprocess.TimeoutExpired:
                    sample=pc.sample();peak=max(peak,m.mw.peak_memory(job));sample['peakOwnedCommittedGiB']=peak
                    environment.write(json.dumps(sample)+'\n');environment.flush()
                    decision=m.mw.resource_decision(sample,4,p['resources'],running=True,idle=True)
                    hot=hot+1 if (sample['totalCpuPercent'] or 0)>=95 else 0
                    if hot>=3:decision['reasons'].append('CPU contention for three observations')
                    if peak>p['resources']['estimatedPeakGiB']:decision['reasons'].append('Owned memory envelope')
                    if decision['reasons']:raise m.mw.ResourcePressure('; '.join(decision['reasons']))
            if code:raise RuntimeError('Confirmation worker exit '+str(code))
    except BaseException as e:error=repr(e)
    finally:
        if job is not None:
            try:peak=max(peak,m.mw.peak_memory(job));receipt['drain']=support.drain(job,process)
            finally:
                job.close()
                if process is not None and process.poll() is None:process.kill();process.wait(timeout=10)
        else:receipt['drain']=dict(ownedProcessesRemaining=0,totalOwnedProcesses=0)
    wall=time.monotonic()-start;rows,intact=progress(directory/'progress.jsonl')
    begun=[x for x in rows if x.get('type')=='pair-start'];done=[x for x in rows if x.get('type')=='pair-complete']
    initiated=2*len(begun) if intact else 640 if process is not None else 0
    passed=not error and intact and rows and rows[-1].get('status')=='passed' and len(done)==320 and initiated==640
    receipt.update(status='passed' if passed else 'refused-before-launch' if process is None else 'failed',
        error=error,endedUtc=support.utc().isoformat(),wallSeconds=wall,chargedWallSeconds=wall if process is not None else 0,
        initiatedGames=initiated,completedGames=2*len(done),observedNodes=sum(x['nodes'] for x in done),peakOwnedCommittedGiB=peak,
        inputsStable=all(support.digest(ROOT/n)==d for n,d in p['inputs'].items()))
    if not receipt['inputsStable']:receipt['status']='failed'
    receipt['files']={str(f.relative_to(ROOT)):support.digest(f) for f in directory.rglob('*') if f.is_file()}
    save(rp,receipt)
    if lp.read_bytes()!=admitted:raise RuntimeError('Unexpected ledger edit; preserve pending ownership')
    consume_attempt(b,p,receipt,initiated);used=b['consumed'];used['chargedWallSeconds']+=receipt['chargedWallSeconds']
    used['timedWallSeconds']+=receipt['chargedWallSeconds'];used['initiatedTimedGames']+=initiated
    used['confirmationGames']+=initiated;used['completedTimedGames']+=receipt['completedGames']
    interrupted=error and any(x in error for x in ('TimeoutError','ResourcePressure','KeyboardInterrupt'))
    used['interruptedTimedGames' if interrupted else 'failedTimedGames']+=initiated-receipt['completedGames']
    b['pending']=[];b['receipts'].append(str(rp.relative_to(ROOT)));save(lp,b)
    print(json.dumps({k:receipt[k] for k in ('id','status','chargedWallSeconds','initiatedGames','completedGames','error','drain')}))
    if receipt['status']!='passed':raise SystemExit(1)


def assess(pair_totals):
    if len(pair_totals)!=320 or any(x not in (0,.5,1,1.5,2) for x in pair_totals):raise ValueError('Complete320 legal pair totals required')
    values=[x/2 for x in pair_totals];mean=statistics.mean(values);variance=statistics.variance(values);se=math.sqrt(variance/320)
    normal=[max(0,mean-1.96*se),min(1,mean+1.96*se)];rng=random.Random(2026110617)
    bootstrap=sorted(statistics.mean(rng.choices(values,k=320)) for _ in range(10000))
    return dict(score=mean,pairVariance=variance,pairStandardError=se,primaryPairedNormal95=normal,
        gate=GATE,primaryStatisticalGate='passed' if mean>=.53 and normal[0]>.50 else 'not-passed',
        pairedBootstrapPercentile95=[bootstrap[249],bootstrap[9749]],
        limitations='Normal iid-pair approximation; zero sample variance produces a degenerate descriptive interval. Bounded and bootstrap sensitivity do not remove opening/opponent/dependence limitations. Technical qualification and owner acceptance remain separate.')


def analyze(path,output):
    rp=Path(path);r=json.loads(rp.read_text());p=json.loads((ROOT/r['protocol']).read_text());b=json.loads((OUT/'budget.json').read_text())
    if (r['status']!='passed' or r['error'] or not r['inputsStable'] or r['initiatedGames']!=640 or r['completedGames']!=640
            or r['drain']['ownedProcessesRemaining'] or support.digest(ROOT/r['protocol'])!=r['protocolSha256']
            or b['consumed']['confirmationAttempts']!=1 or b['confirmationAttempt']['protocolSha256']!=r['protocolSha256']):
        raise ValueError('Only complete, stable, drained, uniquely attributed confirmation may be assessed')
    for name,digest in r['files'].items():
        if support.digest(ROOT/name)!=digest:raise ValueError('Changed evidence '+name)
    pairs=[];wdl=[0,0,0];resources={s:dict(nodes=0,requests=0,elapsedNanos=0) for s in ('baseline','candidate')}
    colors={'White':[],'Black':[]};orders=[[],[]];warnings=0;terminations={}
    for i in range(320):
        v=json.loads((OUT/'raw'/p['id']/f'pair-{i:03d}'/'verified.json').read_text());a=v['results']
        if len(a['games'])!=2 or v['firstGameWhite']!=('candidate' if i%2 else 'baseline'):raise ValueError('Pair/order audit')
        pairs.append(sum(g['candidateScore'] for g in a['games']));warnings+=v['traceAudit']['qualifiedMissingScoreWarnings']
        for j in range(3):wdl[j]+=a['wdl'][j]
        for side in resources:
            for k in resources[side]:resources[side][k]+=a['wholeRequestResources'][side][k]
        for j,g in enumerate(a['games']):
            colors['White' if g['headers']['White']=='candidate' else 'Black'].append(g['candidateScore']);orders[j].append(g['candidateScore'])
            t=g['headers']['Termination'];terminations[t]=terminations.get(t,0)+1
    if sum(x['nodes'] for x in resources.values())!=r['observedNodes']:raise ValueError('Resource total mismatch')
    result=dict(status='complete independently held-out confirmation analysis',id=p['id'],pairs=pairs,wdl=wdl,
        primary=assess(pairs),sensitivity=m.b.paired_statistics(pairs),wholeRequestResources=resources,
        descriptiveColorScores={k:statistics.mean(v) for k,v in colors.items()},descriptiveGameOrderScores=[statistics.mean(v) for v in orders],
        terminations=terminations,qualifiedFallbackWarnings=warnings,executionSha256=support.digest(rp),
        protocolSha256=r['protocolSha256'],analyzerSha256=support.digest(Path(__file__)),chargedWallSeconds=r['chargedWallSeconds'],drain=r['drain'])
    with Path(output).open('x',encoding='utf8',newline='\n') as f:json.dump(result,f,indent=2);f.write('\n')
    print(json.dumps({k:v for k,v in result.items() if k!='pairs'}))


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--protocol');parser.add_argument('--receipt');parser.add_argument('--output')
    args=parser.parse_args()
    if args.protocol:run(args.protocol)
    elif args.receipt and args.output:analyze(args.receipt,args.output)
    else:parser.error('Require --protocol or --receipt with --output')
