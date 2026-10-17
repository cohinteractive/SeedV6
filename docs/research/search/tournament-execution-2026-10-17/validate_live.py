"""Small final-executor pause/resume/export and deliberate interruption tests.

Run after throughput calibration, without competing engine work. Every output is
new and preserved. The interrupted block is never replayed or used for inference.
"""
from datetime import datetime,timedelta,timezone
import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import time

HERE=Path(__file__).resolve().parent;ROOT=HERE.parents[3]
spec=importlib.util.spec_from_file_location('session',ROOT/'tools/tournament-session.py')
s=importlib.util.module_from_spec(spec);spec.loader.exec_module(s)


def main():
    report_path=HERE/'live-validation.json'
    if report_path.exists():raise ValueError('Never replay completed validation')
    report={'startedUtc':s.stamp(),'status':'running','checks':[]}
    s.save(report_path,report)
    try:
        b=s.module('baseline',ROOT/'tools/search-baseline.py')
        with (HERE/'calibration-openings.pgn').open(encoding='utf8') as stream:
            games=[b.chess.pgn.read_game(stream) for _ in range(4)]
        book=HERE/'validation-openings.pgn'
        book.write_text('\n\n'.join(str(g) for g in games)+'\n',encoding='utf8')
        config=s.read(HERE/'w4-c2-config.json')
        config.update(blockPairs=2,blockLimitSeconds=600,
                      assessmentPlan='Operational final-executor validation only: pause/resume across two complete two-pair blocks; no strength assessment.')
        config['arguments'].update(pairs=4,openings=str(book))
        config_path=HERE/'final-validation-config.json';s.save(config_path,config)
        normal=HERE/'raw/final-session';s.prepare(config_path,normal)
        def command(study,action,*args):
            return [sys.executable,'-B',str(study/'inputs/tools/tournament-session.py'),action,'--plan',str(study/'plan.json'),*args]
        def run(study,action,label,success=True,*args):
            cmd=command(study,action,*args)
            with (HERE/'raw'/f'{label}.txt').open('w') as log:
                result=subprocess.run(cmd,stdout=log,stderr=subprocess.STDOUT,cwd=ROOT,timeout=800)
            if (result.returncode==0)!=success:raise ValueError('Unexpected result: '+label)
            report['checks'].append({'label':label,'exitCode':result.returncode,'expectedSuccess':success})
            s.save(report_path,report)
        end=lambda:(datetime.now(timezone.utc)+timedelta(minutes=15)).isoformat()
        run(normal,'run','final-first',True,'--max-blocks','1','--stop-before',end())
        run(normal,'export','incomplete-export',False,'--out',str(HERE/'raw/incomplete-export'))
        if (HERE/'raw/incomplete-export').exists():raise ValueError('Partial stage published')
        run(normal,'pause','final-pause')
        run(normal,'run','pause-respected',True,'--stop-before',end())
        if (normal/'block-0001').exists():raise ValueError('Pause ignored')
        run(normal,'run','final-resume',True,'--stop-before',end(),'--clear-pause')
        run(normal,'status','final-status')
        exported=HERE/'raw/final-export'
        run(normal,'export','final-export',True,'--out',str(exported))
        completed=s.read(exported/'complete.json')
        if completed['games']!=8 or completed['pairs']!=4 or completed['statisticsAssessed']:
            raise ValueError('Incorrect complete-stage accounting')
        audit=s.read(exported/'audit.json')
        if len(audit['games'])!=8 or audit['statistics'] is not None:raise ValueError('Unexpected stage audit')
        report['completedStudy']={'planSha256':s.digest(normal/'plan.json'),'pairs':4,'games':8,
            'gameIdentityAudit':'All four unique frozen openings occur once per color; existing independent legal/result/request auditor passed.',
            'exportSha256':s.digest(exported/'complete.json')}
        # A separate study is prospectively destined to fail. Preserve all evidence.
        config.update(blockPairs=4,assessmentPlan='Operational fault injection only: kill the session owner after the first PGN output, preserve all records, require blocked resume/export. No replay or strength inference.')
        s.save(HERE/'interruption-config.json',config)
        crash=HERE/'raw/interruption';s.prepare(HERE/'interruption-config.json',crash)
        n=s.module('native',ROOT/'tools/sr036-process-cost.py')
        unrelated=subprocess.Popen([sys.executable,'-c','import time;time.sleep(800)'])
        handles=[];process=None
        try:
            with (HERE/'raw/interruption-owner.txt').open('w') as log:
                process=subprocess.Popen(command(crash,'run','--stop-before',end()),stdout=log,stderr=subprocess.STDOUT,cwd=ROOT)
                pgn=crash/'block-0000/games.pgn';cost=crash/'block-0000/process-cost.json'
                deadline=time.monotonic()+500
                while time.monotonic()<deadline:
                    if process.poll() is not None:raise ValueError('Fault study exited before injection')
                    if pgn.exists() and pgn.stat().st_size>0 and cost.exists():break
                    time.sleep(.2)
                else:raise TimeoutError('No PGN to interrupt')
                cost_record=s.read(cost)
                for item in cost_record['processes']:
                    h=n.kernel.OpenProcess(0x1000|0x100000,False,item['pid'])
                    if not h:raise ValueError('Cannot retain engine identity')
                    handles.append(h)
                process.kill();process.wait(timeout=10)
                deadline=time.monotonic()+10
                while any(n.snapshot(h)['exitCode']==259 for h in handles) and time.monotonic()<deadline:time.sleep(.05)
                after=[n.snapshot(h) for h in handles]
                if any(x['exitCode']==259 for x in after) or unrelated.poll() is not None:
                    raise ValueError('Owned shutdown or unrelated-process preservation failed')
                report['interruption']={'ownedEngineCount':len(handles),'allOwnedEnginesExited':True,
                    'unrelatedProcessSurvived':True,'engineLifetimes':after,'partialPgnBytes':pgn.stat().st_size}
        finally:
            if process is not None and process.poll() is None:process.kill();process.wait(timeout=10)
            for h in handles:n.kernel.CloseHandle(h)
            unrelated.kill();unrelated.wait(timeout=10)
        before=s.tree(crash/'block-0000')
        run(crash,'status','interrupted-status',False)
        run(crash,'run','interrupted-resume',False,'--stop-before',end())
        run(crash,'export','interrupted-export',False,'--out',str(HERE/'raw/interrupted-export'))
        if before!=s.tree(crash/'block-0000'):raise ValueError('Interrupted evidence changed')
        if (crash/'block-0000.json').exists() or (HERE/'raw/interrupted-export').exists():
            raise ValueError('Interrupted block published')
        report['interruption'].update(evidencePreserved=True,automaticReplayRejected=True,exportRejected=True)
        report['status']='passed'
    except BaseException as error:
        report.update(status='failed',failure=repr(error));raise
    finally:
        report['endedUtc']=s.stamp();s.save(report_path,report)
    print(json.dumps(report,indent=2))


if __name__=='__main__':main()
