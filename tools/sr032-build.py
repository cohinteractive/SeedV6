"""Freeze an SR-032 overlay against the verified SR-033 research dependencies.

This isolates unrelated inherited training edits. Never overwrites an experiment.
The ordinary Gradle build remains authoritative for eventual integration.
"""
from pathlib import Path
import hashlib
import json
import shutil
import subprocess
import sys

root=Path(__file__).resolve().parents[1]
tag=sys.argv[1]
if not tag.replace('-','').isalnum(): raise ValueError('Invalid tag')
target=root/'build/sr032'/tag
baseline=root/'build/sr033/candidate-v1'
shutil.copytree(baseline,target)
sources=sorted((root/'engine-core/src/main/java').rglob('*.java'))
sources+=sorted((root/'research/src/main/java/com/ohinteractive/seedv6/search/exact').glob('Sr032*.java'))
sources+=[root/'research/src/main/java/com/ohinteractive/seedv6/uci/UciEvaluation.java']
command=['C:/java/jdk-21/bin/javac.exe','--release','21','--add-modules','jdk.incubator.vector',
         '-cp',str(target/'classes')+';'+str(target/'lib/*'),'-d',str(target/'classes')]+list(map(str,sources))
evidence=root/'docs/research/search/sr032-phase2-2026-10-18'
with (evidence/'raw'/f'{tag}-build.log').open('w') as log:
    result=subprocess.run(command,stdout=log,stderr=subprocess.STDOUT)
digest=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
manifest=dict(status='passed' if result.returncode==0 else 'failed',command=command,
              source={str(p.relative_to(root)):digest(p) for p in sources},
              files={str(p.relative_to(root)):digest(p) for p in target.rglob('*') if p.is_file()},
              dependencyOrigin=str(baseline.relative_to(root)),
              inheritedTrainingChanges='excluded through frozen predecessor libraries',
              versionFinalization='DEFERRED_FINALIZATION; build35; no verified bump')
(evidence/f'{tag}-identities.json').write_text(json.dumps(manifest,indent=2)+'\n')
print(manifest['status'],target)
if result.returncode:print((evidence/'raw'/f'{tag}-build.log').read_text())
raise SystemExit(result.returncode)
