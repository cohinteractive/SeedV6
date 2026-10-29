"""Qualification of the narrow fallback exception and live warning supervision."""
import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

spec=importlib.util.spec_from_file_location('trace_audit',Path(__file__).with_name('p4-tt-trace.py'))
t=importlib.util.module_from_spec(spec);spec.loader.exec_module(t)
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'docs/research/search/p4-tt-memory'
WITNESS=json.loads((OUT/'h1-confirm-w1-v1-failure-summary.json').read_text())['traceFallbackAudit']['fallbacks'][0]


def fixture(outputs=None, request=None, position=None, warning=None):
    outputs=outputs if outputs is not None else [
        'info string request nodes=14 elapsedNanos=539316400 termination=TIME_LIMIT', 'bestmove g1h1']
    request=request or WITNESS['request'];position=position or WITNESS['positionCommand']
    lines=[f'[Engine] [12:00:00] <  2>  baseline <--- {position}',
           f'[Engine] [12:00:00] <  2>  baseline <--- {request}']
    lines.extend(f'[Engine] [12:00:00] <  2>  baseline ---> {x}' for x in outputs)
    lines.append(warning if warning is not None else
        '[WARN  ] [12:00:00] <   > fastchess --- Warning; No info line available to extract score from engine baseline')
    return '\n'.join(lines)+'\n'


class AuditTest(unittest.TestCase):
    def audit(self,text):
        with tempfile.TemporaryDirectory() as directory:
            path=Path(directory)/'trace.log';path.write_text(text,encoding='utf-8',newline='\n')
            return t.audit(path)

    def test_actual_request_witness(self):
        result=self.audit(fixture())
        self.assertEqual(result['qualifiedMissingScoreWarnings'],1)
        self.assertEqual(result['fallbacks'][0]['bestmove'],'g1h1')

    def test_zero_work_truthful_fallback(self):
        result=self.audit(fixture(outputs=[
            'info string request nodes=0 elapsedNanos=1000000 termination=TIME_LIMIT','bestmove g1h1']))
        self.assertEqual(result['fallbacks'][0]['nodes'],0)

    def test_reject_invalid_request_evidence(self):
        valid=['info string request nodes=14 elapsedNanos=539316400 termination=TIME_LIMIT','bestmove g1h1']
        cases={
            'illegal move':fixture(outputs=[valid[0],'bestmove a1a8']),
            'missing telemetry':fixture(outputs=[valid[1]]),
            'duplicate telemetry':fixture(outputs=[valid[0],valid[0],valid[1]]),
            'negative nodes':fixture(outputs=[valid[0].replace('nodes=14','nodes=-1'),valid[1]]),
            'claims completion':fixture(outputs=[valid[0].replace('TIME_LIMIT','COMPLETED'),valid[1]]),
            'reports score':fixture(outputs=['info depth 1 score cp 0 nodes 14 time 1 pv g1h1']+valid),
            'reports failure':fixture(outputs=['info string search failed']+valid),
            'not game clock':fixture(request='go movetime 100'),
            'negative clock':fixture(request='go wtime -1 btime 4655 winc 100 binc 100'),
            'duplicate clock field':fixture(request='go wtime 5967 btime 4655 winc 100 binc 100 wtime 5967'),
            'terminal':fixture(position='position startpos moves f2f3 e7e5 g2g4 d8h4',outputs=[valid[0],'bestmove 0000']),
            'unknown warning':fixture(warning='[WARN  ] [12] < 2> fastchess --- Warning; Illegal PV move'),
            'err severity':fixture(warning='[ERR   ] [12] < 2> fastchess --- failed'),
            'fatal severity':fixture(warning='[FATAL ] [12] < 2> fastchess --- failed'),
            'unmatched scoreless response':fixture(warning=''),
            'duplicate warning':fixture()+fixture().splitlines()[-1]+'\n',
            'empty trace':'',
            'undrained request':'\n'.join(fixture().splitlines()[:2])+'\n',
        }
        for name,text in cases.items():
            with self.subTest(name=name),self.assertRaises((ValueError,KeyError)):
                self.audit(text)

    def test_scored_request_has_no_exception(self):
        result=self.audit(fixture(outputs=['info depth 1 score cp 0 nodes 14 time 1 pv g1h1',
            'info string request nodes=14 elapsedNanos=1000000 termination=TIME_LIMIT','bestmove g1h1'],warning=''))
        self.assertEqual(result['qualifiedMissingScoreWarnings'],0)

    def test_live_supervisor_defers_only_known_warning(self):
        for allowed in (True,False):
            with self.subTest(allowed=allowed),tempfile.TemporaryDirectory() as directory:
                path=Path(directory)/'trace.log'
                warning=fixture().splitlines()[-1] if allowed else '[ERR ] [12] < 2> fastchess --- forced error'
                path.write_text(warning+'\n',encoding='utf-8',newline='\n')
                process=subprocess.Popen([sys.executable,'-B','-c','import time;time.sleep(0.4)'])
                try:
                    observed=[]
                    if allowed:
                        self.assertEqual(t.supervise(process,path,3,observed),0)
                        self.assertEqual(observed,['baseline'])
                    else:
                        with self.assertRaises(ValueError):t.supervise(process,path,3,observed)
                finally:
                    if process.poll() is None:process.terminate()
                    process.wait(timeout=5)

    def test_live_supervisor_timeout_and_missing_trace(self):
        for missing in (True,False):
            with self.subTest(missing=missing),tempfile.TemporaryDirectory() as directory:
                path=Path(directory)/'trace.log'
                if not missing:path.write_text('',encoding='utf-8')
                process=subprocess.Popen([sys.executable,'-B','-c',
                    'import time;time.sleep('+('0.01' if missing else '1')+')'])
                try:
                    with self.assertRaises(ValueError if missing else TimeoutError):
                        t.supervise(process,path,2 if missing else .05,[])
                finally:
                    if process.poll() is None:process.terminate()
                    process.wait(timeout=5)


if __name__=='__main__':unittest.main(verbosity=2)
