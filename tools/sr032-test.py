"""Run selected repository JUnit tests against a frozen SR-032 runtime, in isolation."""
from pathlib import Path
import subprocess, sys, json, hashlib
root=Path(__file__).resolve().parents[1]
tag=sys.argv[1];names=sys.argv[2:]
out=root/'build/sr032'/tag
out.mkdir(parents=True,exist_ok=False)
runtime=root/'build/sr032/candidate-v3'
cache=Path('C:/Users/Central/.gradle/caches/modules-2/files-2.1')
jars=[]
for group,artifact,version in [('org.junit.jupiter','junit-jupiter-api','5.10.3'),('org.junit.jupiter','junit-jupiter-engine','5.10.3'),('org.junit.jupiter','junit-jupiter-params','5.10.3'),('org.junit.platform','junit-platform-engine','1.10.3'),('org.junit.platform','junit-platform-launcher','1.10.3'),('org.junit.platform','junit-platform-commons','1.10.3'),('org.opentest4j','opentest4j','1.3.0'),('org.apiguardian','apiguardian-api','1.1.2')]:
    found=list((cache/group/artifact/version).glob('*/'+artifact+'-'+version+'.jar'))
    if len(found)!=1:raise ValueError((artifact,found))
    jars+=found
sources=[]
for name in names:
    found=list((root/'research/src/test/java').rglob(name+'.java'))
    if len(found)!=1:raise ValueError(name)
    sources+=found
runner=out/'RunTests.java'
runner.write_text('''import org.junit.platform.launcher.core.*;
import org.junit.platform.launcher.listeners.*;
import org.junit.platform.engine.discovery.DiscoverySelectors;
public class RunTests { public static void main(String[] args) {
 var request=LauncherDiscoveryRequestBuilder.request();
 for(String name:args) request.selectors(DiscoverySelectors.selectClass(name));
 var listener=new SummaryGeneratingListener(); var launcher=LauncherFactory.create();
 launcher.registerTestExecutionListeners(listener);launcher.execute(request.build());
 var result=listener.getSummary();result.printTo(new java.io.PrintWriter(System.out));
 result.printFailuresTo(new java.io.PrintWriter(System.out));
 if(result.getTestsFailedCount()!=0||result.getTestsFoundCount()==0)System.exit(1);
}}''')
classpath=';'.join(map(str,[out,runtime/'classes',runtime/'lib/*',root/'research/build/classes/java/test']+jars))
cmd=['C:/java/jdk-21/bin/javac.exe','--release','21','-cp',classpath,'-d',str(out),str(runner)]+list(map(str,sources))
log=root/'docs/research/search/sr032-phase2-2026-10-18/raw'/f'{tag}.log'
with log.open('w') as f:
    result=subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT)
    if result.returncode==0:
        classes=[str(p.relative_to(root/'research/src/test/java')).replace('\\','.').replace('/','.')[:-5] for p in sources]
        result=subprocess.run(['C:/java/jdk-21/bin/java.exe','-Xmx2g','-XX:ActiveProcessorCount=6','--add-modules=jdk.incubator.vector','-cp',classpath,'RunTests']+classes,stdout=f,stderr=subprocess.STDOUT)
print(log.read_text())
h=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
(log.parent.parent/f'{tag}-summary.json').write_text(json.dumps(dict(status='passed' if result.returncode==0 else 'failed',tests=names,log=str(log),source={str(p):h(p) for p in sources},runtime=str(runtime)),indent=2)+'\n')
raise SystemExit(result.returncode)
