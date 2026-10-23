"""Capture the CP64 session adaptation's meaningful continuation/fault tests."""
import json
import subprocess
import sys
from run_diagnostic_v1 import digest, write_new, ROOT, HERE

path=HERE/'raw/tt-session-tests-v2.log'
assert not path.exists()
result=subprocess.run([sys.executable,'-B',str(ROOT/'tools/test_p3_tt_session.py'),'-v'],
                      cwd=ROOT,capture_output=True,text=True)
path.write_text(result.stdout+result.stderr,encoding='utf8')
write_new(HERE/'tt-session-validation-v1.json',{
    'status':'passed' if result.returncode==0 else 'failed','exitCode':result.returncode,
    'log':str(path.relative_to(HERE)),'logSha256':digest(path),
    'tests':26,'files':{str(p.relative_to(ROOT)):digest(p) for p in
         (ROOT/'tools/p3-tt-session.py',ROOT/'tools/test_p3_tt_session.py')},
    'limits':'Adapted existing continuation and Windows process-fault tests, plus resource admission/qualification. Earlier25-test output is preserved; PowerShell returned1 because unittest wrote to stderr, despite its OK result. No chess pilot or confirmation claimed here.'})
print(result.stdout+result.stderr)
sys.exit(result.returncode)
