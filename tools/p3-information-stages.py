"""Phase 3 packaging version of the existing SR-032 staged executor.

Statistical rules are the existing SR-024 paired assessor, bound by hash. Only
whole predeclared stages are assessed. Operational pauses never authorize looks.
The historical SR-032 source path is frozen by closed protocols. This separate
version retains the per-side binary binding needed by Phase 3; the freezer
packages it as sr032-confirmation.py inside new immutable tool bundles.
"""
import argparse
from datetime import datetime, timezone
import importlib.util
import json
from pathlib import Path
import subprocess
import sys


def module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    value = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(value)
    return value


session_source=Path(__file__).with_name('p3-information-session.py')
if not session_source.exists():session_source=Path(__file__).with_name('tournament-session.py')
t = module('sessions', session_source)
b = module('baseline', Path(__file__).with_name('search-baseline.py'))
rules = module('rules', Path(__file__).with_name('sr024-confirmation.py'))


def verify(path):
    p = t.read(path)
    t.require(p['executorSha256'] == t.digest(__file__), 'Confirmation executor changed')
    t.require(p['design'] == 'sr032-paired-stages-v1', 'Unknown protocol')
    t.require(all(t.digest(k) == v for k, v in p['frozenFiles'].items()), 'Frozen evidence changed')
    t.require(p['looks'] == [128, 512] and p['contrastIndex'] in (1, 2), 'Unexpected design')
    return p


def command(plan, action, *args):
    plan = Path(plan)
    result = subprocess.run([sys.executable, '-B', str(plan.parent/'inputs/tools/tournament-session.py'),
                             action, '--plan', str(plan), *args], check=True, capture_output=True, text=True)
    return json.loads(result.stdout)


def verify_runtime(p, arguments):
    """Permit explicitly frozen per-side binaries; legacy protocols share one tree."""
    for side in ('baseline', 'candidate'):
        expected = p['runtimeTrees'][side] if 'runtimeTrees' in p else p['runtimeTree']
        t.require(t.tree(arguments[side]) == expected, 'Wrong runtime: ' + side)


def assess(protocol, count):
    p = verify(protocol)
    t.require(1 <= count <= len(p['stages']), 'Invalid stage count')
    seen = set(); excluded = set(); totals = []; looks = []; records = []
    for path in p['excludedSelections']:
        excluded.update(t.read(path)['positionKeys'])
    for stage in p['stages'][:count]:
        t.require(not looks or looks[-1]['decision'] == 'continue', 'Closed confirmation cannot extend')
        plan = Path(stage['plan']); export = Path(stage['export'])
        t.require(command(plan, 'status')['status'] == 'complete', 'Incomplete stage')
        plan_data = t.read(plan); receipt = t.read(export/'complete.json')
        if 'expectedAdapter' in p:
            t.require(plan_data['adapter'] == p['expectedAdapter'], 'Wrong match adapter')
        t.require(plan_data['configurationSha256'] == t.digest(stage['config']), 'Wrong stage configuration')
        t.require(t.digest(plan.parent/'inputs/assessment-protocol.txt') == t.digest(protocol), 'Wrong frozen protocol')
        t.require(receipt['planSha256'] == t.digest(plan) and receipt['pairs'] == stage['pairs'], 'Export identity')
        t.require(receipt['gamesSha256'] == t.digest(export/'games.pgn') and
                  receipt['auditSha256'] == t.digest(export/'audit.json'), 'Export changed')
        t.require(t.digest(plan.parent/'inputs/schedule.pgn') == t.digest(stage['openings']), 'Wrong schedule')
        a = plan_data['arguments']
        for key, value in p['expectedArguments'].items():
            t.require(a[key] == value, 'Wrong domain: ' + key)
        verify_runtime(p, a)
        selection = t.read(stage['selection']); keys = selection['positionKeys']
        t.require(selection['partition'] == 1 and len(keys) == stage['pairs'] == len(set(keys)), 'Opening selection')
        t.require(not seen.intersection(keys) and not excluded.intersection(keys), 'Opening reuse')
        seen.update(keys)
        actual = []
        for prefix in b.opening_prefixes(stage['openings'], stage['pairs']):
            board = b.chess.Board()
            for move in prefix: board.push_uci(move)
            actual.append(' '.join(board.fen().split()[:4]))
        t.require(keys == actual and all(int(b.hashlib.sha256(k.encode()).hexdigest(), 16) % 2 == 1 for k in keys),
                  'Opening identity or partition')
        audit = b.analyze(export/'games.pgn', stage['openings'], stage['pairs'], True, True)
        t.require(audit['terminations'] == {'normal': 2*stage['pairs']}, 'Abnormal games')
        scores = [g['candidateScore'] for g in audit['games']]
        totals.extend(scores[i]+scores[i+1] for i in range(0, len(scores), 2))
        looks.append(rules.decision(totals, p['contrastIndex'], p['looks']))
        records.append({'export':str(export), 'completeSha256':t.digest(export/'complete.json'),
                        'wdl':audit['wdl'], 'wholeRequestResources':audit['wholeRequestResources']})
    verify(protocol)
    return {'status':'passed', 'protocolSha256':t.digest(protocol), 'records':records,
            'looks':looks, 'pairTotals':totals, 'finalDecision':looks[-1]['decision']}


def run(protocol, stop_before, max_stages=2):
    t.require(type(max_stages) is int and max_stages in (1,2), "Invalid scheduled-stage limit")
    protocol = Path(protocol).resolve(); p = verify(protocol)
    out = Path(p['execution']); out.mkdir(exist_ok=True)
    record = {'protocolSha256':t.digest(protocol), 'status':'running', 'assessmentFiles':[]}
    try:
        for index, stage in enumerate(p['stages'][:max_stages], 1):
            verify(protocol)
            assessment_path = out/f'assessment-{index}.json'
            if assessment_path.exists():
                result = assess(protocol, index)
                t.require(result == t.read(assessment_path), 'Stored assessment changed')
            else:
                status = command(stage['plan'], 'run', '--stop-before', stop_before)
                if status['status'] != 'complete':
                    record['status'] = status['status']; return record
                if not Path(stage['export']).exists():
                    command(stage['plan'], 'export', '--out', stage['export'])
                result = assess(protocol, index)
                t.save(assessment_path, result)
            record['assessmentFiles'].append(str(assessment_path))
            record['decision'] = result['finalDecision']
            t.save(out/'execution.json', record)
            print(json.dumps({'completedLook':index, 'assessment':result['looks'][-1]}), flush=True)
            if result['finalDecision'] != 'continue':
                record['status'] = 'closed'; break
        if record['status'] == 'running': record['status'] = 'paused_after_scheduled_look'
        return record
    except BaseException as error:
        record.update(status='blocked', failure=repr(error)); raise
    finally:
        t.save(out/'execution.json', record)



if __name__ == "__main__":
    raise SystemExit("Use the Phase 3 confirmation binding in a frozen tool bundle")
