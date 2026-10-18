"""Deliberate native failure after engine attachment; no strength inference."""
import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import traceback
from types import SimpleNamespace
from unittest.mock import patch

root=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('match',Path(__file__).with_name('sr032-match.py'))
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
e=root/'docs/research/search/sr032-phase2-2026-10-18'
config=json.loads((e/'screen-promotion-w4-config.json').read_text())
tag=sys.argv[1] if len(sys.argv)>1 else 'v2'
assert tag.startswith('v') and tag[1:].isdigit()
out=e/'raw'/('process-cleanup-fault-'+tag)
assert not out.exists(),'Never replace fault evidence'
args=SimpleNamespace(**dict(config['arguments'],pairs=2,out=str(out),timeout=120,operational=True))
unrelated=subprocess.Popen([sys.executable,'-B','-c','import time; time.sleep(60)'])
attach=m.c.Cohort.attach
def fail_after_attachment(cohort):
    attach(cohort)
    raise RuntimeError('Intentional ownership regression fault after complete engine attachment')
try:
    try:
        with patch.object(m.c.Cohort,'attach',fail_after_attachment):m.run(args)
        raise AssertionError('Fault was not raised')
    except Exception:
        out.mkdir(exist_ok=True)
        failure=traceback.format_exc()
        (out/'fault.txt').write_text(failure)
        assert 'Intentional ownership regression fault' in failure
    report=json.loads((out/'process-cost.json').read_text())
    assert json.loads((out/'run.json').read_text())['status']=='failed'
    assert len(report['processes'])==4
    assert all(x['lifetime']['exitCode']!=259 for x in report['processes'])
    assert unrelated.poll() is None,'Unrelated owned fixture was affected'
    receipt={'status':'passed','purpose':'Intentional operational failure, not strength data',
             'allFourBoundEnginesExited':True,'unrelatedFixtureUnaffected':True,
             'evidence':str(out),'sourceSha256':m.b.digest(__file__),
             'adapterSha256':m.b.digest(m.__file__),'observerSha256':m.b.digest(m.c.__file__),
             'nativeOwnerSha256':m.b.digest(m.owned.__file__)}
    (e/('process-ownership-fault-'+tag+'-summary.json')).write_text(json.dumps(receipt,indent=2)+'\n')
    print(json.dumps(receipt))
finally:
    if unrelated.poll() is None:unrelated.kill()
    unrelated.wait(timeout=10)
