"""Correct v1's comparator activation: ordinary default UCI is BaselineEngine.

The explicit historical --nnue path is recursive and is preserved, not modified.
Frozen v1 runtime, transcripts and failed qualification remain intact.
"""
import importlib.util
import json
import re
from types import SimpleNamespace
from run_diagnostic_v1 import digest, write_new, ROOT, HERE

spec=importlib.util.spec_from_file_location('match',ROOT/'tools/p3-tt-match.py')
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
binding=json.loads((HERE/'clock-runtime-v1-binding.json').read_text())
assert all(digest(p)==sha for p,sha in binding['files'].items())
assert digest(ROOT/'tools/p3-tt-match.py')==binding['adapterSha256']
args=SimpleNamespace(baseline=str(HERE/'raw/clock-runtime-v1'),baseline_policy='baseline',baseline_workers=1,
                     java='C:/java/jdk-21/bin/java.exe',network=str(ROOT/'build-assets/nnue-candidate/conventional-cp64.cnnue'))
research=m.command(args,'baseline');entry=research.index('com.ohinteractive.seedv6.integration.P3TtUciMain')
standard=research[:entry]+['com.ohinteractive.seedv6.uci.UciMain']
roots=[[]]+[line.split('\t')[2].split() for line in (ROOT/'docs/research/search/p3-construction-2026-10-20/diagnostic-corpus.tsv').read_text().splitlines()[:2]]
report={'status':'running','correctedPremise':'No-argument standard UciMain is selected Construction V1. Explicit --nnue is preserved recursive historical opt-in.',
        'priorFailedQualificationSha256':digest(HERE/'uci-qualification-v1.json'),'qualificationSha256':digest(__file__),
        'checks':{}}
try:
    for side,command in (('research',research),('ordinary',standard)):
        engine=m.b.Uci(command,HERE/'raw'/f'qualification-v2-{side}-uci.txt')
        try:
            engine.send('uci');identity=engine.until('uciok',90)
            assert any('flags=262143' in x and 'aspiration=WIDEN16' in x and 'networkSha256='+m.NETWORK_SHA256 in x for x in identity)
            engine.send('setoption name OwnBook value false');engine.send('setoption name Threads value 1')
            engine.send('setoption name Hash value 64');engine.send('isready');engine.until('readyok',90)
            results=[]
            for prefix in roots:
                board=m.b.chess.Board()
                for move in prefix:board.push_uci(move)
                engine.send('ucinewgame');engine.send('isready');engine.until('readyok')
                engine.send('position startpos'+(' moves '+' '.join(prefix) if prefix else ''))
                engine.send('go nodes 100000')
                results.append(m.b.inspect_result(board,engine.until('bestmove ',60)))
            report['checks'][side]={'identity':identity,'requests':results}
        finally:engine.close()
    for a,b in zip(report['checks']['ordinary']['requests'],report['checks']['research']['requests']):
        for key in ('bestmove','nodes','termination'):assert a[key]==b[key],(key,a,b)
        for pattern in (r'depth (\d+)',r'score (cp|mate) (-?\d+)',r'pv (.*)$'):
            assert re.search(pattern,a['finalInfo']).groups()==re.search(pattern,b['finalInfo']).groups(),(a,b)
    report.update(status='passed',baselineParity=True,requestsPerActivation=3)
except BaseException as error:
    report.update(status='failed',failure=repr(error));raise
finally:write_new(HERE/'uci-qualification-v2.json',report)
print(report['status'],flush=True)
