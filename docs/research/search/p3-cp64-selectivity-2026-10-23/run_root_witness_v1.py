"""Diagnose one retained unequal-horizon move witness without tuning."""
import json,subprocess,time
from pathlib import Path
from run_observation_v1 import HERE,ROOT,digest,write_new
from run_clock_v1 import module

def main():
    assert (HERE/'removal-clock-v2-summary.json').exists(),'Finish H1 and post-delivery first'
    original=json.loads((HERE/'flat-null-v1-binding.json').read_text())
    def stable():
        for p,sha in original['files'].items():assert digest(ROOT/p)==sha,p
    stable();runtime=HERE/'raw/runtime-v4';source=ROOT/'docs/research/search/sr006-phase2-2026-10-10/played-corpus.tsv'
    selected=[(i+1,line) for i,line in enumerate(source.read_bytes().splitlines(keepends=True)) if line.startswith(b'played-01\t')]
    assert len(selected)==1
    corpus=HERE/'root-witness-v1-corpus.tsv'
    with corpus.open('xb') as stream:stream.write(selected[0][1])
    selection=HERE/'root-witness-v1-selection.json'
    write_new(selection,{'source':str(source.relative_to(ROOT)),'sourceSha256':digest(source),'id':'played-01',
        'sourceLine':selected[0][0],'output':str(corpus.relative_to(HERE)),'outputSha256':digest(corpus),'method':'Exact source line selected by id; bytes/history unchanged'})
    files=[HERE/'root-witness-v1.json',Path(__file__),HERE/'flat-null-v1-binding.json',HERE/'fixed-node-move-witness-v1.json',source,corpus,selection]
    write_new(HERE/'root-witness-v1-binding.json',{'files':{str(p.relative_to(ROOT)):digest(p) for p in files},'runtimeBindingSha256':digest(HERE/'flat-null-v1-binding.json')})
    native=module('native',ROOT/'tools/sr036-process-cost.py');summary={'status':'running','depths':{}}
    try:
        for depth in (7,9):
            name=f'root-witness-d{depth}-v1';output=HERE/'raw'/(name+'.jsonl');log=HERE/'raw'/(name+'.log')
            command=['C:/java/jdk-21/bin/java.exe','--add-modules=jdk.incubator.vector','-Djava.awt.headless=true','-Xms256m','-Xmx1024m',
                '-XX:ActiveProcessorCount=4','-cp',str(runtime/'classes')+';'+str(runtime/'lib/*'),'com.ohinteractive.seedv6.search.exact.Cp64SelectivityLab',
                str(ROOT/'build-assets/nnue-candidate/conventional-cp64.cnnue'),str(corpus),str(output),'1','0','1',str(depth),'5000000','1','cost',
                'baseline,nominal-futility,verified-null,nominal-verified-null']
            record={'command':command,'status':'running'};start=time.monotonic()
            try:
                with log.open('xb') as stream:
                    process=subprocess.Popen(command,cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT,
                        creationflags=subprocess.BELOW_NORMAL_PRIORITY_CLASS|subprocess.CREATE_NO_WINDOW)
                    try:record['exitCode']=process.wait(timeout=600)
                    except BaseException:process.kill();process.wait();raise
                    finally:record['nativeLifetime']=native.snapshot(int(process._handle))
                assert record['exitCode']==0
                rows=[json.loads(x) for x in output.read_text().splitlines()];assert rows[-1]['status']=='passed'
                selected=[x for x in rows if x.get('type')=='request'];assert len(selected)==4 and all(x['id']=='played-01' and x['round']==0 for x in selected)
                outcomes={}
                for row in selected:
                    outcome=row['outcome'];last=outcome['lastCompletedResult']
                    outcomes[row['profile']]={'targetDepthCompleted':outcome['targetDepthCompleted'],'attemptedDepth':outcome['attemptedDepth'],
                        'requestNodes':outcome['nodes'],'termination':row['termination'],
                        'lastCompleted':None if last is None else {k:last[k] for k in ('bestMove','score','depth','nodes','principalVariation','selective')}}
                record['outcomes']=outcomes;stable();record['status']='passed'
                summary['depths'][str(depth)]=outcomes
            except BaseException as error:record.update(status='failed',failure=repr(error));raise
            finally:
                record['wallSeconds']=time.monotonic()-start
                record['evidence']={str(p.relative_to(HERE)):digest(p) for p in (output,log) if p.exists()}
                write_new(HERE/(name+'-execution.json'),record)
        summary['status']='passed'
    except BaseException as error:summary.update(status='failed',failure=repr(error));raise
    finally:write_new(HERE/'root-witness-v1-summary.json',summary)
    print(json.dumps(summary,indent=2))

if __name__=='__main__':main()
