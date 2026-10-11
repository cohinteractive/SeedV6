"""Reproduce final SR-022 real-model default worker-switching validation from repo root."""
import importlib.util
from pathlib import Path

spec = importlib.util.spec_from_file_location('baseline', 'tools/search-baseline.py')
b = importlib.util.module_from_spec(spec)
spec.loader.exec_module(b)
out = Path(__file__).parent
installation = 'app/build/sr022-p2-production-v5'
store = 'E:/SeedV6-Networks/BRN-Pair2/235d0688-e060-44e3-8f1e-119c39bc8017'
java = 'C:/java/jdk-21/bin/java.exe'
cmd = b.command(java, installation, store, 'production', 'production', 'off', 'default')
# Also omit the other policy properties, exercising all actual startup defaults.
cmd = [s for s in cmd if not any(s.startswith('-Dseedv6.uci.' + key + '=')
       for key in ('qsearchResearch', 'lmrResearch', 'aspirationResearch'))]
frozen = b.installation_hashes(installation)
for p in (Path(store) / 'checkpoints' / b.CHECKPOINT).iterdir():
    if p.is_file(): frozen[str(p)] = b.digest(p)
for p in (__file__, b.__file__, java): frozen[str(p)] = b.digest(p)
report = {'status': 'running', 'filesBefore': frozen, 'commands': cmd, 'switches': []}
try:
    report['workers2'] = b.preflight(cmd, out / 'production-workers2-preflight.txt', 2)
    engine = b.Uci(cmd, out / 'production-worker-switches.txt')
    try:
        engine.send('uci'); hello = engine.until('uciok')
        identity = next(s for s in hello if s.startswith('info string evaluator='))
        engine.send('setoption name OwnBook value false')
        for workers in (1, 2, 1):
            engine.send('setoption name Threads value ' + str(workers))
            engine.send('isready'); ready = engine.until('readyok')
            identities = [s for s in ready if s.startswith('info string evaluator=')]
            if identities: identity = identities[-1]
            b.check_lmr_identity(identity, 'production', 'production', workers, 'production')
            b.check_lmp_identity(identity, 'production', workers=workers)
            assert (' workers=' + str(workers) + ' ') in identity
            engine.send('ucinewgame'); engine.send('position startpos'); engine.send('go depth 4')
            result = b.inspect_result(b.chess.Board(), engine.until('bestmove '))
            assert result['termination'] == 'COMPLETED'
            report['switches'].append({'workers': workers, 'identity': identity, **result})
        first, _, last = report['switches']
        for key in ('nodes', 'bestmove', 'termination'): assert first[key] == last[key]
        import re
        stable = lambda s: re.sub(r'\b(?:time|nps) \d+\s*', '', s)
        assert stable(first['finalInfo']) == stable(last['finalInfo'])
        import json
        bridge = json.loads((out / 'bridge-production-window/bridge.json').read_text())
        expected = bridge['preflights']['candidate']['requests'][0]
        assert stable(first['finalInfo']) == stable(expected['finalInfo'])
        for key in ('nodes', 'bestmove', 'termination'): assert first[key] == expected[key]
    finally:
        engine.close()
    assert all(b.digest(path) == value for path, value in frozen.items())
    report.update(status='passed', inputsStable=True, singleWorkerRestoredExactly=True)
except BaseException as error:
    report.update(status='failed', failure=repr(error))
    raise
finally:
    b.save(out / 'production-runtime.json', report)
