"""SR-032 confirmation over frozen, audited pair-block sessions.

Statistical rules are the existing SR-024 paired assessor, bound by hash. Only
whole predeclared stages are assessed. Operational pauses never authorize looks.
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


t = module('sessions', Path(__file__).with_name('tournament-session.py'))
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
        t.require(plan_data['configurationSha256'] == t.digest(stage['config']), 'Wrong stage configuration')
        t.require(t.digest(plan.parent/'inputs/assessment-protocol.txt') == t.digest(protocol), 'Wrong frozen protocol')
        t.require(receipt['planSha256'] == t.digest(plan) and receipt['pairs'] == stage['pairs'], 'Export identity')
        t.require(receipt['gamesSha256'] == t.digest(export/'games.pgn') and
                  receipt['auditSha256'] == t.digest(export/'audit.json'), 'Export changed')
        t.require(t.digest(plan.parent/'inputs/schedule.pgn') == t.digest(stage['openings']), 'Wrong schedule')
        a = plan_data['arguments']
        for key, value in p['expectedArguments'].items():
            t.require(a[key] == value, 'Wrong domain: ' + key)
        for side in ('baseline', 'candidate'):
            t.require(t.tree(a[side]) == p['runtimeTree'], 'Wrong runtime: ' + side)
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


def prepare(a):
    root = Path(a.evidence).resolve(); out = root/'confirmation-v1'
    t.require(not out.exists(), 'Never replace a frozen confirmation')
    out.mkdir()
    excluded = []; unavailable = []
    for path in sorted(Path('docs/research/search').rglob('*.selection.json')):
        if 'raw' in path.parts: continue
        data = t.read(path)
        if 'positionKeys' in data: excluded.append(str(path.resolve()))
        else: unavailable.append(str(path.resolve()))
    book = root/'development-openings.selection.json'
    source = t.read(book)['source']
    all_openings = out/'all-openings.pgn'
    b.select_openings(source, all_openings, 512, 320118, 1, excluded)
    selection = t.read(all_openings.with_suffix('.selection.json'))
    with all_openings.open(encoding='utf8') as stream:
        games = [b.chess.pgn.read_game(stream) for _ in range(512)]
    slices = []
    for index, (start, end) in enumerate(((0,128),(128,512)), 1):
        path = out/f'stage{index}-openings.pgn'
        path.write_text('\n\n'.join(str(g) for g in games[start:end])+'\n', encoding='utf8', newline='\n')
        receipt = path.with_suffix('.selection.json')
        t.save(receipt, dict(selection, count=end-start, positionKeys=selection['positionKeys'][start:end],
                            outputSha256=t.digest(path), slice=[start,end], parentSelection=str(all_openings.with_suffix('.selection.json'))))
        slices.append((path, receipt, end-start))
    for contrast, name in enumerate(('recapture-w1','promotion-w4'), 1):
        development = t.read(root/f'screen-{name}-config.json')
        protocol_path = out/f'{name}-protocol.json'
        stages = []
        for index, (openings, receipt, pairs) in enumerate(slices, 1):
            config = dict(development)
            config.update(purpose='confirmatory', assessmentProtocol=protocol_path.name,
                          assessmentPlan='Only cumulative 128/512 paired looks, existing adjusted paired-normal and 2.5-SE rule, prospectively allocated family alpha and conditional-power futility; no operational block looks.',
                          arguments=dict(development['arguments'], openings=str(openings), pairs=pairs, seed=320118))
            config_path = out/f'{name}-stage{index}-config.json'; t.save(config_path, config)
            study = root/'raw'/f'confirm-{name}-v1-stage{index}'
            stages.append({'pairs':pairs, 'openings':str(openings), 'selection':str(receipt), 'config':str(config_path),
                           'plan':str(study/'plan.json'), 'export':str(study)+'-export'})
        runtime = Path(development['arguments']['candidate'])
        dependencies = [Path(__file__).resolve(), Path(rules.__file__), Path(t.__file__), Path(b.__file__),
                        root/'candidate-v3-identities.json', root/'development-results-v1.json', Path(source),
                        all_openings, all_openings.with_suffix('.selection.json')]
        dependencies += [Path(x) for x in excluded]
        dependencies += [Path(s[k]) for s in stages for k in ('config','openings','selection')]
        dependencies += [f for f in runtime.rglob('*') if f.is_file()]
        p = {'design':'sr032-paired-stages-v1', 'createdUtc':datetime.now(timezone.utc).isoformat(),
             'hypothesis':'Positive clock-strength gain in this worker domain; development excluded.',
             'contrastIndex':contrast, 'looks':[128,512], 'familyAlpha':.05, 'partition':1,
             'executorSha256':t.digest(__file__), 'stages':stages,
             'execution':str(root/'raw'/f'confirm-{name}-v1-assessments'),
             'excludedSelections':excluded, 'legacySelectionsWithoutPositionKeys':unavailable,
             'expectedArguments':{k:development['arguments'][k] for k in ('baseline_policy','candidate_policy','baseline_workers','candidate_workers','concurrency','tc')},
             'runtimeTree':t.tree(runtime), 'frozenFiles':{str(path):t.digest(path) for path in dependencies},
             'rules':'Existing sr024-confirmation.py decision(): alpha .05/2^contrastIndex divided by two looks; normal only with >=20 pair scores each side of half; also require 2.5-SE agreement; adjusted Hoeffding sensitivity. Conditional power <.10 stops for futility, never inferiority. Maximum unresolved is inconclusive. No extension after stopping.',
             'rationale':'Development selected RECAPTURE1 (+21.7 Elo, 17% fixed-depth elapsed cost) and PROMOTION4 (+38.2 Elo, near-neutral diagnostic elapsed cost), with wide intervals crossing zero. Fresh 128-pair looks can close reversed/weak signals. At 512 pairs, development plug-in Elo SE is about 10.6 native and 8.9 four-worker, with approximate adjusted half widths 26.6/24.3 Elo; estimates are selection-biased and variance forecasts uncertain. This gives useful decision value for the four-worker signal and a bounded chance to resolve native benefit; no small-gain power claim. Same fresh openings across domains, never pooled. Fixed 16-pair JVM restarts preserve the measured domain.',
             'forecast':'At observed 236/125 pairs per match hour, both first looks cost 1.57h match time plus about 5-10 minutes overhead; 25% slower about 1.96h plus overhead. Both maxima cost 6.27h match time, about 7.84h at 25% slowdown, plus session overhead. They do not fit the remaining daytime window if both continue. Run the first 128-pair native and four-worker stages, then prioritize the four-worker continuation if required. Immediate stop-before 2026-10-18T17:30:00+13:00; later execution requires an authorized sufficiently defined window. Pause only after complete blocks; operational boundaries never become statistical looks. No automatic overnight launch and no reduction of statistical maxima to fit the window.',
             'limits':'Approximate pair-normal inference, winner-selection uncertainty, one host/model/clock, no small-gain power claim; normal and conservative intervals both reported.'}
        t.save(protocol_path, p)
        for stage in stages:
            t.prepare(stage['config'], Path(stage['plan']).parent)
        print(json.dumps({'protocol':str(protocol_path), 'sha256':t.digest(protocol_path), 'looks':p['looks']}), flush=True)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest='action', required=True)
    prep = sub.add_parser('prepare'); prep.add_argument('--evidence', required=True)
    execute = sub.add_parser('run'); execute.add_argument('--protocol', required=True); execute.add_argument('--stop-before', required=True); execute.add_argument('--max-stages', type=int, choices=(1,2), default=2)
    audit = sub.add_parser('assess'); audit.add_argument('--protocol', required=True); audit.add_argument('--stages', type=int, required=True)
    args = parser.parse_args()
    if args.action == 'prepare': prepare(args)
    elif args.action == 'run': print(json.dumps(run(args.protocol, args.stop_before, args.max_stages)))
    else: print(json.dumps(assess(args.protocol, args.stages), indent=2))
