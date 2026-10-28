"""Bounded existing Java inference microbenchmark, with explicit argv and provenance."""
import json
from pathlib import Path
import subprocess
import sys
from baseline import ROOT,admit,sha

out=Path(sys.argv[1]);resource=admit(initial=True);out.mkdir(parents=True,exist_ok=False)
runtime=ROOT/'docs/research/brn-relational-cglhw/raw/r002-runtime'
command=['C:/java/jdk-21/bin/java.exe','--add-modules=jdk.incubator.vector','-XX:ActiveProcessorCount=1',
    '-XX:+UseSerialGC','-Xmx384m','-cp',str(runtime/'*'),'com.ohinteractive.seedv6.tools.eval.ConventionalNnueBenchmark',
    str(ROOT/'build-assets/nnue-candidate/conventional-cp64.cnnue')]
result=subprocess.run(command,capture_output=True,text=True,timeout=60,
    creationflags=subprocess.CREATE_NO_WINDOW|subprocess.BELOW_NORMAL_PRIORITY_CLASS)
(out/'stdout.log').write_text(result.stdout);(out/'stderr.log').write_text(result.stderr)
result.check_returncode()
report=dict(result=json.loads(result.stdout),resources=resource,command=command,
    runtime_sha256={x.name:sha(x) for x in sorted(runtime.glob('*.jar'))})
(out/'result.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2))
