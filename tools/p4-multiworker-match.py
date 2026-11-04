"""MW-R001 paired competition with independent audits and owned deadline supervision.
One parent owns a kill-on-close Windows Job; the child runs sequential frozen pairs.
There is no scheduler, automatic retry, or future-window authority.
"""
import argparse
import ctypes as c
from ctypes import wintypes as w
import datetime as dt
import importlib.util
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time

sys.dont_write_bytecode = True
def module(name, file):
    spec = importlib.util.spec_from_file_location(name, Path(__file__).with_name(file))
    m = importlib.util.module_from_spec(spec); spec.loader.exec_module(m); return m
mw = module('mw_supervision', 'p4-multiworker.py')
b = module('mw_game_audit', 'search-baseline.py')
trace = module('mw_trace_audit', 'p4-tt-trace.py')
ROOT, OUT = mw.ROOT, mw.OUT
JAVA = 'C:/java/jdk-21/bin/java.exe'
MODEL = ROOT/'build-assets/nnue-candidate/conventional-cp64.cnnue'


def owned_members(job):
    """Read only the retained Job's process IDs; never attach/kill by image name."""
    class Members(c.Structure):
        _fields_=[('assigned',w.DWORD),('listed',w.DWORD),('ids',c.c_size_t*256)]
    value=Members()
    if not job.k.QueryInformationJobObject(job.handle,3,c.byref(value),c.sizeof(value),None):
        raise c.WinError(c.get_last_error())
    job.k.OpenProcess.argtypes=[w.DWORD,w.BOOL,w.DWORD];job.k.OpenProcess.restype=w.HANDLE
    job.k.QueryFullProcessImageNameW.argtypes=[w.HANDLE,w.DWORD,w.LPWSTR,c.POINTER(w.DWORD)]
    result=[]
    for pid in list(value.ids)[:value.listed]:
        handle=job.k.OpenProcess(0x1000,False,pid);name=None
        if handle:
            try:
                length=w.DWORD(32768);text=c.create_unicode_buffer(length.value)
                if job.k.QueryFullProcessImageNameW(handle,0,text,c.byref(length)):name=text.value
            finally:job.k.CloseHandle(handle)
        result.append(dict(pid=pid,image=name))
    return result


def command(runtime, architecture, actor):
    return [JAVA, '--add-modules=jdk.incubator.vector', '-Xms128m', '-Xmx512m',
            '-XX:ActiveProcessorCount=4', '-Djava.awt.headless=true',
            '-Dseedv6.syzygy.config=', '-Dseedv6.syzygy.path=',
            '-Dseedv6.clockPolicy=BOUNDARY2', '-Dseedv6.research.actor='+actor,
            '-cp', str(Path(runtime)/'classes')+os.pathsep+str(Path(runtime)/'lib/*'),
            'com.ohinteractive.seedv6.integration.P4MultiworkerUciMain',
            '--network', str(MODEL), '--architecture', architecture]


def configure(engine, workers, architecture):
    engine.send('uci'); lines = engine.until('uciok', 60)
    for name, value in [('Threads', workers), ('Hash', 64), ('OwnBook', 'false')]:
        engine.send(f'setoption name {name} value {value}')
    engine.send('isready'); lines += engine.until('readyok', 60)
    engine.send('uci'); lines += engine.until('uciok', 60)
    identities = [x for x in lines if 'networkSha256=' in x]
    if not identities: raise ValueError('Missing runtime identity')
    identity = identities[-1]
    for token in ['flags=1177812991', 'checkpoint=64', 'schema=S6CNU002', 'heads=8',
                  'networkSha256=8431626c2fca81e0a1e9ccd5e45ddd8829e6e3282c756c21fad75b06d075bb2b',
                  'scoreMapping=BT4_Q_LOGIT400_V1', 'clockPolicy=boundary2-d6-v1',
                  f'workers={workers};hashMiB=64;aspiration=ADAPTIVE16',
                  'formulation='+{'ROOT_REUSE':'TC-P1','WHOLE_ROOT_RACE':'MW-C1','WHOLE_ROOT_SHARING':'MW-C2','EAGER_ROOT':'MW-C3'}[architecture],
                  'learning='+('completed-winner-private-copies' if workers>1 and architecture=='WHOLE_ROOT_SHARING' else 'request-worker-private'),
                  'architecture='+('OWNER' if workers == 1 else architecture)]:
        if token not in identity: raise ValueError('Missing identity '+token+': '+identity)
    return identity


def audit_requests(path, p):
    states = {side:dict(position=None, request=None, outputs=[]) for side in ('baseline','candidate')}
    totals = {side:dict(nodes=0, requests=0) for side in states}
    with path.open(encoding='utf-8') as stream:
        for line in stream:
            match = trace.ENGINE.match(line.rstrip())
            if not match: continue
            side, direction, text = match.groups(); state = states[side]
            if direction == '<---':
                if text.startswith('position '): state['position'] = text
                elif text.startswith('go '):
                    if state['request'] is not None: raise ValueError('Overlapping Search')
                    if p['kind'] == 'fixed' and text.split() != ['go','nodes',str(p['nodesPerMove'])]:
                        raise ValueError('Fixed-work request also has a clock or wrong nodes: '+text)
                    state.update(request=text, outputs=[])
            elif state['request'] is not None:
                state['outputs'].append(text)
                if text.startswith('bestmove '):
                    result = b.inspect_result(trace.position(state['position']), state['outputs'])
                    if p['kind'] == 'fixed' and (result['nodes'] > p['nodesPerMove']
                            or result['termination'] not in ('NODE_LIMIT','COMPLETED')):
                        raise ValueError('Incorrect fixed-work termination/accounting')
                    totals[side]['nodes'] += result['nodes']; totals[side]['requests'] += 1
                    state.update(request=None, outputs=[])
    if any(s['request'] is not None for s in states.values()): raise ValueError('Unfinished Search')
    return totals


def audit_pair(directory, p, reverse=False):
    warnings = trace.audit(directory/'uci-trace.log')
    with (directory/'games.pgn').open(encoding='utf-8') as stream:
        headers=[b.chess.pgn.read_headers(stream),b.chess.pgn.read_headers(stream)]
    expected=['candidate','baseline'] if reverse else ['baseline','candidate']
    if [h.get('White') if h else None for h in headers]!=expected:
        raise ValueError('Unexpected frozen first-game color/order')
    # Bind both raw games by exact opening/color after independently checking order.
    result = b.analyze(directory/'games.pgn', directory/'opening.pgn', 1,
                       require_accounting=True, unordered=reverse)
    requests = audit_requests(directory/'uci-trace.log', p)
    for side in requests:
        for k in ('nodes','requests'):
            if requests[side][k] != result['wholeRequestResources'][side][k]:
                raise ValueError('Trace/PGN request accounting mismatch')
    for game in result['games']:
        termination = game['headers'].get('Termination','').lower()
        if termination == 'normal': continue  # Independent chess rules already checked by b.analyze.
        if not ('adjudication' in termination and game['headers']['Result']=='1/2-1/2'
                and game['plies'] >= 2*p['maxMoves']-1):
            raise ValueError('Unqualified game termination '+str(game))
    return dict(results=result, firstGameWhite=expected[0], traceAudit=warnings, requests=requests,
                nodes=sum(x['nodes'] for x in requests.values()))


def emit(stream, row):
    stream.write(json.dumps(row)+'\n'); stream.flush(); os.fsync(stream.fileno())


def worker(protocol, directory):
    p = json.loads(protocol.read_text()); games=[]
    with (ROOT/p['openings']).open(encoding='utf-8') as stream:
        while (game := b.chess.pgn.read_game(stream)) is not None:
            if game.errors: raise ValueError('Invalid opening')
            games.append(game)
    commands = {side:command(ROOT/p['runtime'], p[side], side) for side in ('baseline','candidate')}
    with (directory/'progress.jsonl').open('x', encoding='utf-8') as progress:
        identities = {}
        for side, cmd in commands.items():
            engine = b.Uci(cmd, directory/(side+'-preflight.txt'))
            try: identities[side] = configure(engine, p['workers'], p[side])
            finally: engine.close()
        emit(progress, dict(type='identity', identities=identities))
        for pair, opening in enumerate(p['openingIndices']):
            reverse=p.get('firstColorPolicy','baseline-first')=='alternate' and pair%2==1
            sub = directory/f'pair-{pair:03d}'; sub.mkdir()
            (sub/'opening.pgn').write_text(str(games[opening])+'\n', encoding='utf-8')
            cmd = [str(ROOT/'build/research/tools/fastchess.exe')]
            for side, engine in commands.items():
                cmd += ['-engine','name='+side,'cmd='+engine[0], 'args='+subprocess.list2cmdline(engine[1:])]
            cmd += ['-each','proto=uci','restart=off','option.OwnBook=false','option.Hash=64',
                    'option.Threads='+str(p['workers']), 'timemargin=0']
            cmd += ['nodes='+str(p['nodesPerMove'])] if p['kind']=='fixed' else ['tc='+p['tc']]
            cmd += ['-rounds','1','-repeat','-concurrency','1','-srand',str(p['seed']+pair),
                    '-openings','file='+str(sub/'opening.pgn'),'format=pgn','order=sequential',
                    '-maxmoves',str(p['maxMoves']),
                    '-pgnout','file='+str(sub/'games.pgn'),'nodes=true','nps=true','pv=true',
                    'match_line=^info string request ', '-ratinginterval','2','-autosaveinterval','0',
                    '-log','file='+str(sub/'uci-trace.log'),'level=trace','append=false',
                    'compress=false','realtime=false','engine=true']
            if p['warningPolicy']=='strict': cmd.append('-strict')
            if reverse: cmd.append('-reverse')
            # Conservatively initiate both paired games BEFORE runner creation. A failed
            # pair keeps both charges; no hidden/replayed partial game or duplicate score.
            emit(progress, dict(type='pair-start', pair=pair, openingIndex=opening,
                                initiatedGames=2, utc=mw.support.utc().isoformat(), command=cmd))
            with (sub/'runner.txt').open('x', encoding='utf-8') as log:
                process = subprocess.Popen(cmd, cwd=sub, env=b.clean_environment(), stdout=log,
                    stderr=subprocess.STDOUT, creationflags=0x4000 | subprocess.CREATE_NO_WINDOW)
                code = process.wait()  # Parent Job enforces independent deadline/resource stop.
            if code != 0: raise RuntimeError('Fastchess exit '+str(code))
            result = audit_pair(sub, p, reverse)
            mw.save(sub/'verified.json', result)
            emit(progress, dict(type='pair-complete', pair=pair, **result))
        emit(progress, dict(type='summary', status='passed', pairs=len(p['openingIndices'])))


def admission(p, ledger, now):
    if p['kind'] not in ('fixed','timed') or p['workers'] not in (1,2,4): raise ValueError('Match domain')
    if p.get('firstColorPolicy','baseline-first') not in ('baseline-first','alternate'):
        raise ValueError('First-game color policy')
    if p['kind']=='fixed' and (type(p['nodesPerMove']) is not int or not 1 <= p['nodesPerMove'] <= 1_000_000):
        raise ValueError('Fixed-work admission limit')
    if p['category'] != 'exploration': raise ValueError('Independent confirmation needs its own prospective admission')
    pairs = len(p['openingIndices'])
    if not 1 <= pairs <= 64 or len(set(p['openingIndices'])) != pairs: raise ValueError('Opening count/duplicates')
    if not 1 <= p['timeoutSeconds'] <= 28800 or not 1 <= p['maxMoves'] <= 400: raise ValueError('Operating bounds')
    if ledger['pending'] or ledger['ownedProcesses']: raise ValueError('Unresolved prior ownership/admission')
    timed = p['kind']=='timed'; group = 'timed' if timed else 'fixed'
    reserve = ledger['confirmation']['protectedTimedGames'] if timed else 0
    if ledger['consumed'][group+'Initiated']+2*pairs+reserve > ledger['ceilings'][group+'Games']:
        raise ValueError('Game allowance/reserve')
    maximum = 0 if timed else 2*pairs*2*p['maxMoves']*p['nodesPerMove']
    if maximum+ledger['consumed']['fixedNodes'] > ledger['ceilings']['fixedNodes']: raise ValueError('Node allowance')
    if timed and ledger['consumed']['timedWallSeconds']+p['timeoutSeconds']+60+ledger['confirmation']['protectedTimedWallSeconds'] > ledger['ceilings']['timedWallSeconds']:
        raise ValueError('Timed wall/reserve')
    end = now+dt.timedelta(seconds=p['timeoutSeconds'])
    if p['mode']=='B':
        window = json.loads((ROOT/p['window']).read_text())
        hard = dt.datetime.fromisoformat(window['hardDeadlineUtc'].replace('Z','+00:00'))
        start = dt.datetime.fromisoformat(window['receivedUtc'].replace('Z','+00:00'))
        if window['status']!='authorized' or not start <= now < hard or end > hard-dt.timedelta(seconds=60):
            raise ValueError('Batch does not fit the fixed owner window with drain headroom')
    elif p['mode']!='A' or timed or p['timeoutSeconds']>1800:
        raise ValueError('No timed/high-load authority')
    return maximum, end


def supervise(protocol):
    p = json.loads(protocol.read_text()); ledger_path=OUT/'budget.json'; ledger=json.loads(ledger_path.read_text())
    maximum, deadline = admission(p, ledger, mw.support.utc())
    for name, digest in p['inputs'].items():
        if b.digest(ROOT/name)!=digest: raise ValueError('Changed input '+name)
    directory=OUT/'raw'/p['id']; directory.mkdir(exist_ok=False)
    receipt_path=OUT/(p['id']+'-execution.json')
    if receipt_path.exists(): raise FileExistsError(receipt_path)
    receipt=dict(id=p['id'], status='admitted', protocol=str(protocol), protocolSha256=b.digest(protocol),
                 maximumNodes=maximum, maximumGames=2*len(p['openingIndices']), deadlineUtc=deadline.isoformat(),
                 mode=p['mode'], kind=p['kind'], startedUtc=mw.support.utc().isoformat())
    mw.save(receipt_path,receipt)
    ledger['pending'].append(dict(id=p['id'], maximumNodes=maximum, maximumGames=receipt['maximumGames'], receipt=str(receipt_path)))
    mw.save(ledger_path,ledger); admitted=ledger_path.read_bytes()
    job=mw.support.owned.OwnedJob(); process=None; error=None; pressure=False; peak=0; started=time.monotonic()
    # Wall clock and monotonic stop both apply; backward clock changes cannot extend a batch.
    stop_at=started+max(0,(deadline-mw.support.utc()).total_seconds()); pc=mw.PcUse()
    try:
        pc.sample();time.sleep(.5);sample=pc.sample();receipt['preflightEnvironment']=sample
        decision=mw.resource_decision(sample,p['workers'],p['resources'],idle=p['mode']=='B')
        receipt['resourceDecision']=decision;mw.save(receipt_path,receipt)
        if not decision['admissible'] or (OUT/'STOP').exists():
            pressure=True;raise RuntimeError('Temporary resource condition or owner STOP: '+str(decision))
        with (directory/'console.log').open('x',encoding='utf-8') as log, (directory/'environment.jsonl').open('x') as env:
            process=subprocess.Popen([sys.executable,'-B',str(Path(__file__).resolve()),'--worker',
                    '--protocol',str(protocol.resolve()),'--directory',str(directory)],cwd=ROOT,stdout=log,stderr=subprocess.STDOUT,
                    creationflags=0x4|0x4000|subprocess.CREATE_NO_WINDOW)
            job.assign(process);receipt.update(status='running',processId=process.pid);mw.save(receipt_path,receipt)
            mw.save(OUT/'resource-state.json',dict(status='running',receipt=str(receipt_path),processId=process.pid,deadlineUtc=deadline.isoformat()))
            job.resume_initial_thread(process);hot=0
            while True:
                remaining=min(stop_at-time.monotonic(),(deadline-mw.support.utc()).total_seconds())
                if remaining<=0: raise TimeoutError('Independent batch/window deadline')
                if (OUT/'STOP').exists():pressure=True;raise RuntimeError('Owner STOP')
                try: code=process.wait(timeout=min(2,remaining));break
                except subprocess.TimeoutExpired:
                    sample=pc.sample();peak=max(peak,mw.peak_memory(job));sample['peakOwnedCommittedGiB']=peak
                    env.write(json.dumps(sample)+'\n');env.flush()
                    decision=mw.resource_decision(sample,p['workers'],p['resources'],running=True,idle=p['mode']=='B')
                    hot=hot+1 if (sample['totalCpuPercent'] or 0)>=(95 if p['mode']=='B' else 80) else 0
                    if not decision['admissible'] or hot>=3 or peak>p['resources']['estimatedPeakGiB']:
                        pressure=True;raise RuntimeError('Resource pressure: '+str(decision)+f'; peak={peak}; hot={hot}')
            if code!=0:raise RuntimeError('Worker exit '+str(code))
    except BaseException as problem:error=repr(problem)
    finally:
        try:
            peak=max(peak,mw.peak_memory(job));receipt['activeOwnedBeforeDrain']=job.accounting().activeProcesses
            receipt['membersBeforeDrain']=owned_members(job)
            # Process exit can precede Job accounting's final removal. Give owned
            # descendants a bounded natural exit interval, inside the60s headroom.
            settle=time.monotonic()
            while error is None and job.accounting().activeProcesses and time.monotonic()-settle<5:
                time.sleep(.05)
            receipt['settleOwnedSeconds']=time.monotonic()-settle
            receipt['membersAfterSettle']=owned_members(job)
            if error is None and receipt['membersAfterSettle']:
                error='Owned processes survived normal worker exit: '+str(receipt['membersAfterSettle'])
            receipt['drain']=mw.support.drain(job,process)
        finally:
            job.close()
            if process is not None and process.poll() is None:process.kill();process.wait(timeout=10)
    receipt.update(error=error,wallSeconds=time.monotonic()-started,endedUtc=mw.support.utc().isoformat(),peakOwnedCommittedGiB=peak)
    rows=[];read_error=None
    try:
        if (directory/'progress.jsonl').exists():rows=[json.loads(l) for l in (directory/'progress.jsonl').read_text().splitlines()]
    except (ValueError,OSError) as e:read_error=repr(e)
    begins=[r for r in rows if r['type']=='pair-start'];done=[r for r in rows if r['type']=='pair-complete']
    intact_journal=read_error is None and (directory/'progress.jsonl').exists()
    initiated=2*len(begins) if intact_journal else (receipt['maximumGames'] if process is not None else 0)
    passed=not error and not read_error and rows and rows[-1].get('status')=='passed' and len(done)==len(p['openingIndices'])
    receipt.update(status='passed' if passed else 'interrupted' if pressure or (error and 'TimeoutError' in error) else 'failed',
                   initiatedGames=initiated,completedGames=2*len(done),
                   gameCounting='Both paired games conservatively initiated before each Fastchess creation; unfinished/unverified pairs retained, not rescored',
                   observedNodes=sum(r['nodes'] for r in done),chargedNodes=(sum(r['nodes'] for r in done) if passed else maximum if initiated else 0) if p['kind']=='fixed' else 0,
                   pairs=[dict(pair=r['pair'],wdl=r['results']['wdl'],score=r['results']['statistics']['score']) for r in done],
                   files={str(f):b.digest(f) for f in directory.rglob('*') if f.is_file()},
                   inputsStable=all(b.digest(ROOT/n)==h for n,h in p['inputs'].items()))
    if not receipt['inputsStable']:receipt['status']='failed'
    mw.save(receipt_path,receipt)
    if ledger_path.read_bytes()!=admitted:raise RuntimeError('Unexpected ledger edit; preserve pending admission')
    group='fixed' if p['kind']=='fixed' else 'timed';c=ledger['consumed']
    c[group+'Initiated']+=initiated;c[group+'Completed']+=2*len(done)
    c[group+('Interrupted' if receipt['status']=='interrupted' else 'Failed')]+=initiated-2*len(done)
    c['fixedNodes']+=receipt['chargedNodes'];c[group+'WallSeconds']+=receipt['wallSeconds'] if process is not None else 0
    ledger['pending']=[];ledger['receipts'].append(str(receipt_path.relative_to(ROOT)));mw.save(ledger_path,ledger)
    disposition=mw.continuation_action({'admissible':False},p.get('alternativeWork',[])) if pressure else ('continue_research' if receipt['status']=='passed' else 'investigate_failure')
    mw.save(OUT/'resource-state.json',dict(status=disposition,
            receipt=str(receipt_path.relative_to(ROOT)),ownedProcessesRemaining=0,temporaryResourceBlocker=pressure,
            pendingHumanAction=error if disposition=='await_user_input' else None,
            alternativeWork=p.get('alternativeWork',[]),clientBoundary='Agent handles input; repository state does not preserve client liveness.',
            resumeState='docs/research/search/p4-multiworker/STATE.md',resumeRequiresNewRunId=True))
    print(json.dumps({k:receipt[k] for k in ('id','status','initiatedGames','completedGames','chargedNodes','wallSeconds','error','drain')}))
    if receipt['status']!='passed':raise SystemExit(75 if pressure else 1)


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--protocol',type=Path,required=True)
    parser.add_argument('--worker',action='store_true');parser.add_argument('--directory',type=Path)
    args=parser.parse_args()
    if args.worker:worker(args.protocol,args.directory)
    else:supervise(args.protocol)
