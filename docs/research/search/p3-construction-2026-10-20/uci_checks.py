"""Event-driven real-process checks of the explicit research launcher; no games."""
import hashlib,json,pathlib,queue,re,subprocess,threading,time

base=pathlib.Path(__file__).resolve().parent
raw=base/'raw'; logpath=raw/'uci-v1.log'; summary=base/'uci-v1-summary.json'
if logpath.exists() or summary.exists():raise SystemExit('Refusing to overwrite evidence')
java=['C:/java/jdk-21/bin/java.exe','--add-modules=jdk.incubator.vector',
      '-Djava.awt.headless=true','-Xms128m','-Xmx768m','-XX:ActiveProcessorCount=2']
tail=['-cp','build/research/runtime/classes;build/research/runtime/lib/*',
      'com.ohinteractive.seedv6.integration.P3ConstructionUciMain',
      '--model','build-assets/production/bre-pair2-g32.brn-pair2']
flags=subprocess.BELOW_NORMAL_PRIORITY_CLASS|subprocess.CREATE_NO_WINDOW
legal={f'{f}2{f}{rank}' for f in 'abcdefgh' for rank in '34'}|{'b1a3','b1c3','g1f3','g1h3'}
records=[]; started=time.monotonic()
with logpath.open('x',encoding='utf-8') as log:
    def record(line):log.write(line+'\n');log.flush()
    class Session:
        def __init__(self,props):
            self.command=java+['-D'+x for x in props]+tail
            record('COMMAND '+json.dumps(self.command));self.q=queue.Queue()
            self.p=subprocess.Popen(self.command,stdin=subprocess.PIPE,stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,text=True,bufsize=1,creationflags=flags)
            def read():
                for line in self.p.stdout:self.q.put(line.rstrip())
                self.q.put(None)
            self.reader=threading.Thread(target=read,daemon=True);self.reader.start()
        def send(self,line):record('> '+line);self.p.stdin.write(line+'\n');self.p.stdin.flush()
        def until(self,predicate):
            lines=[];end=time.monotonic()+30
            while True:
                line=self.q.get(timeout=max(.001,end-time.monotonic()))
                if line is None:raise AssertionError('Unexpected process exit')
                record('< '+line);lines.append(line)
                assert 'search failed:' not in line and 'command failed:' not in line,line
                if predicate(line):return lines
        def ready(self):self.send('isready');return self.until(lambda x:x=='readyok')
        def search(self,command,budget):
            self.send(command);lines=self.until(lambda x:x.startswith('bestmove '))
            assert lines[-1].split()[1] in legal,lines[-1]
            counts=[int(re.search(r'request nodes=(\d+)',x)[1]) for x in lines if 'request nodes=' in x]
            assert len(counts)==1 and 0<=counts[0]<=budget,counts
            return counts[0]
        def close(self,eof=False):
            if eof:self.p.stdin.close()
            else:self.send('quit')
            assert self.p.wait(timeout=30)==0
            self.reader.join(timeout=5)
            while not self.q.empty():
                line=self.q.get()
                if line is not None:record('< '+line)
    for profile,aspiration in [('rich','WIDEN16'),('rich','OFF'),('policy4','OFF'),('exact','OFF')]:
        s=Session(['seedv6.construction.profile='+profile,'seedv6.construction.aspiration='+aspiration])
        try:
            s.send('uci');identity=s.until(lambda x:x=='uciok')
            assert any('experimental=true' in x and 'aspiration='+aspiration in x for x in identity)
            assert any('model.sha256=5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7' in x for x in identity)
            s.ready();sessions=[]
            for workers in [1,4,16]:
                s.send('setoption name Threads value '+str(workers));s.send('setoption name Hash value 1')
                s.ready();s.send('uci');lines=s.until(lambda x:x=='uciok')
                assert any('workers='+str(workers) in x and 'ttMiB=1' in x for x in lines)
                s.send('ucinewgame');s.send('position startpos')
                depth=s.search('go depth 3 nodes 20000',20000)
                nodes=s.search('go nodes 1234',1234)
                assert nodes==1234
                s.send('setoption name Clear Hash');s.send('setoption name Hash value 64');s.ready()
                sessions.append({'workers':workers,'depth3Nodes':depth,'boundedNodes':nodes})
            if profile=='rich' and aspiration=='WIDEN16':
                s.send('go infinite');s.until(lambda x:x.startswith('info depth 1 '));s.send('stop')
                stopped=s.until(lambda x:x.startswith('bestmove '))
                assert stopped[-1].split()[1] in legal
                s.send('go infinite');s.until(lambda x:x.startswith('info depth 1 '))
                s.send('ucinewgame');lines=s.ready()
                assert not any(x.startswith('bestmove ') for x in lines)
                s.send('position startpos');s.search('go nodes 2000',2000)
                s.send('setoption name Threads value 0');lines=s.ready()
                assert any('rejected command' in x for x in lines)
            s.close(eof=profile=='exact')
            records.append({'profile':profile,'aspiration':aspiration,'sessions':sessions,'exitCode':0})
        finally:
            if s.p.poll() is None:s.p.kill();s.p.wait()
    invalid=[['seedv6.construction.disable=LMR'],['seedv6.construction.profile=unknown'],
             ['seedv6.construction.futilityMargin=garbage'],['seedv6.construction.reverseMargin=0'],
             ['seedv6.construction.disable=NOT_A_FEATURE'],['seedv6.construction.aspiration=unknown']]
    for props in invalid:
        command=java+['-D'+x for x in props]+tail
        p=subprocess.run(command,capture_output=True,text=True,timeout=30,creationflags=flags)
        record('REJECTION '+json.dumps(command)+'\n'+p.stdout+p.stderr)
        assert p.returncode!=0 and 'Exception' in p.stderr
        records.append({'rejected':props,'exitCode':p.returncode})
    command=java+tail[:-1]+['build-assets/production/nonexistent-p3-model']
    p=subprocess.run(command,capture_output=True,text=True,timeout=30,creationflags=flags)
    record('MODEL REJECTION '+p.stdout+p.stderr);assert p.returncode!=0
    records.append({'rejected':'missing pinned model','exitCode':p.returncode})
result={'status':'passed','elapsedSeconds':time.monotonic()-started,'records':records,
        'logSha256':hashlib.sha256(logpath.read_bytes()).hexdigest(),
        'runtime':{str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in pathlib.Path('build/research/runtime').rglob('*') if p.is_file()}}
summary.write_bytes((json.dumps(result,indent=2)+'\n').encode());print(json.dumps({k:v for k,v in result.items() if k not in ['runtime','records']}))
