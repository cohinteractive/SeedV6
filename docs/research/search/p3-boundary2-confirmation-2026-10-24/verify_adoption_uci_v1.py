"""Small real-UCI adoption checks. No games, strength analysis or anomaly campaign."""
from pathlib import Path
import json,os,queue,re,subprocess,sys,threading,time
import chess
from verify_inputs import HERE,ROOT,digest,write_new
from run_diagnostic_v1 import background
from run_adoption_validation_v3 import run_owned

class Engine:
    def __init__(self,folder,name,entry,args=(),properties=()):
        self.path=folder/name;self.path.mkdir();self.lines=[];self.requests=[];self.queue=queue.Queue()
        command=['C:/java/jdk-21/bin/java.exe','--add-modules=jdk.incubator.vector','-Djava.awt.headless=true','-Xms128m','-Xmx512m','-XX:ActiveProcessorCount=4',*properties,'-cp',str(ROOT/'app/build/install/seedv6/lib/*'),entry,*map(str,args)]
        self.command=command
        self.out=(self.path/'stdout.txt').open('w',encoding='utf8');self.err=(self.path/'stderr.txt').open('w',encoding='utf8')
        self.process=subprocess.Popen(command,cwd=ROOT,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=self.err,text=True,encoding='utf8',creationflags=subprocess.CREATE_NO_WINDOW)
        def reader():
            for line in self.process.stdout:
                self.out.write(line);self.out.flush();self.queue.put(line.strip())
            self.queue.put(None)
        self.reader=threading.Thread(target=reader,daemon=True);self.reader.start()
    def send(self,line):self.process.stdin.write(line+'\n');self.process.stdin.flush()
    def until(self,prefix):
        lines=[];end=time.monotonic()+15
        while time.monotonic()<end:
            line=self.queue.get(timeout=max(.01,end-time.monotonic()));assert line is not None,'Early EOF'
            lines.append(line)
            if line.startswith(prefix):return lines
        raise TimeoutError(prefix)
    def handshake(self,policy):
        self.send('uci');lines=self.until('uciok')
        assert f'option name ClockPolicy type combo default {policy} var BOUNDARY2 var FACTUAL' in lines
        assert any('clockPolicy='+('factual' if policy=='FACTUAL' else 'boundary2-d6-v1') in x for x in lines)
        return [x for x in lines if x.startswith(('id name','info string'))]
    def search(self,command,policy,workers,zero_allowed=False):
        self.send('position startpos');self.send(command);lines=self.until('bestmove ')
        move=lines[-1].split()[1]
        if not (zero_allowed and move=='0000'):assert chess.Move.from_uci(move) in chess.Board().legal_moves
        request=next((x for x in lines if x.startswith('info string request ')),None)
        if command=='go nodes 100' and request:assert 'nodes=100 ' in request
        if command=='go depth 2':assert any(x.startswith('info depth 2 ') for x in lines)
        self.requests.append({'command':command,'policy':policy,'workers':workers,'bestmove':move,'request':request})
    def close(self):
        if self.process.poll() is None:
            try:self.send('quit');self.process.wait(timeout=15)
            except BaseException:self.process.kill();self.process.wait();raise
        self.reader.join(timeout=2);assert not self.reader.is_alive()
        self.out.close();self.err.close();self.process.stdin.close();self.process.stdout.close()
        assert self.process.returncode==0
        errors=(self.path/'stderr.txt').read_text(encoding='utf8').splitlines()
        assert all(x=='WARNING: Using incubator modules: jdk.incubator.vector' for x in errors),errors
        return {'name':self.path.name,'command':self.command,'requests':self.requests,'exitCode':self.process.returncode}

def worker(name):
    assert sys.stdin.readline().strip()=='GO';background();folder=HERE/'raw'/name
    rows=[]
    profiles=[('cp64','com.ohinteractive.seedv6.uci.UciMain',()),('pair2','com.ohinteractive.seedv6.uci.UciMain',('--model',ROOT/'build-assets/production/bre-pair2-g32.brn-pair2')),('conventional','com.ohinteractive.seedv6.uci.UciMain',('--nnue',ROOT/'build-assets/nnue-candidate/conventional-cp64.cnnue')),('legacy-hce','com.ohinteractive.seedv6.Main',())]
    for name,entry,args in profiles:
        e=Engine(folder,name,entry,args)
        try:
            default=e.handshake('BOUNDARY2');e.send('setoption name Hash value 1');e.send('setoption name OwnBook value false')
            for policy in ('BOUNDARY2','FACTUAL'):
                e.send('setoption name ClockPolicy value '+policy);e.handshake(policy)
                for workers in range(1,17):
                    e.send('setoption name Threads value '+str(workers));e.send('ucinewgame');e.search('go depth 2',policy,workers)
                    if workers in (1,4,16):
                        for command in ('go nodes 100','go movetime 40','go wtime 1500 btime 1500 winc 100 binc 100','go wtime 50 btime 50 winc 100 binc 100'):
                            e.search(command,policy,workers,zero_allowed=(name=='legacy-hce' and 'wtime 50 ' in command))
            # Change policy while infinite Search is active, then verify new publication and option identity.
            e.send('go infinite');e.until('info depth ');e.send('setoption name ClockPolicy value BOUNDARY2');e.handshake('BOUNDARY2')
            e.search('go depth 2','BOUNDARY2',16)
            e.send('go infinite');e.until('info depth ');e.send('stop');e.until('bestmove ')
            e.send('go infinite');e.until('info depth ')
        finally:row=e.close();row['defaultIdentity']=default;rows.append(row)
    e=Engine(folder,'property-fallback','com.ohinteractive.seedv6.uci.UciMain',properties=('-Dseedv6.clockPolicy=FACTUAL',))
    try:
        default=e.handshake('FACTUAL');e.search('go depth 2','FACTUAL',1)
        e.send('setoption name ClockPolicy value BOUNDARY2');e.handshake('BOUNDARY2');e.search('go depth 2','BOUNDARY2',1)
    finally:rows.append(e.close())
    write_new(folder/'checks.json',{'status':'passed','profiles':rows,'requestCount':sum(len(x['requests']) for x in rows),'scope':'Functional actual UCI/default/fallback/all16workers and representative clock/node/movetime/stop/reconfiguration/quit. No timing-tail bound or strength claim.'})

def main(name):
    background();folder=HERE/'raw'/name;folder.mkdir(exist_ok=False)
    inputs={str(p.relative_to(ROOT)):digest(p) for p in (ROOT/'app/build/install/seedv6/lib').glob('*.jar')}
    before=digest(ROOT/'VERSION_STATE.txt');record={'scriptSha256':digest(__file__),'runtimeInputs':inputs,'buildStateSha256':before}
    try:
        env=os.environ.copy()
        for key in ('JAVA_TOOL_OPTIONS','JDK_JAVA_OPTIONS','_JAVA_OPTIONS'):env.pop(key,None)
        with (folder/'worker.txt').open('xb') as log:
            record['execution']=run_owned([sys.executable,'-B',str(Path(__file__).resolve()),name,'worker'],log,600,ROOT,env)
        assert record['execution']['exitCode']==0
        checks=json.loads((folder/'checks.json').read_text());record.update(status=checks['status'],requestCount=checks['requestCount'],profiles=[x['name'] for x in checks['profiles']])
        for p,sha in inputs.items():assert digest(ROOT/p)==sha
        assert digest(ROOT/'VERSION_STATE.txt')==before
    except BaseException as error:record.update(status='failed',failure=repr(error));raise
    finally:
        record['evidence']={str(p.relative_to(HERE)):digest(p) for p in folder.rglob('*') if p.is_file()}
        write_new(HERE/(name+'-summary.json'),record)
    print(json.dumps({k:v for k,v in record.items() if k not in ('runtimeInputs','evidence')},indent=2))

if __name__=='__main__':
    if len(sys.argv)==3 and sys.argv[2]=='worker':worker(sys.argv[1])
    else:main(sys.argv[1])
