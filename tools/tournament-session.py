"""Frozen pair-block sessions around sr021-match.py; no statistical decisions.

Prepare a NEW study only. Never pass a historical confirmation protocol. Every
block starts fresh JVMs, including within one session. Resume validates all prior
receipts and inputs; incomplete/abnormal blocks are preserved and block replay.
"""
import argparse
from contextlib import contextmanager
from datetime import datetime, timezone
import hashlib
import importlib.util
import json
import math
import os
from pathlib import Path
import platform
import shutil
import sys
import time
from types import SimpleNamespace

FILES = ('tournament-session.py', 'tournament-process.py', 'sr021-match.py',
         'search-baseline.py', 'sr024-process-cost.py', 'sr036-process-cost.py')


def adapter_name(record):
    name = record.get('adapter', 'sr021-match.py')
    require(name in ('sr021-match.py', 'sr033-match.py'), 'Unsupported match adapter')
    return name


def require(condition, message):
    if not condition:
        raise ValueError(message)


def digest(path):
    with Path(path).open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def read(path):
    return json.loads(Path(path).read_text(encoding='utf-8-sig'))


def save(path, value):
    path = Path(path)
    temp = path.with_suffix(path.suffix + '.tmp')
    with temp.open('w', encoding='utf8', newline='\n') as stream:
        json.dump(value, stream, indent=2, allow_nan=False)
        stream.write('\n'); stream.flush(); os.fsync(stream.fileno())
    os.replace(temp, path)


def module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    result = importlib.util.module_from_spec(spec); spec.loader.exec_module(result)
    return result


def tree(path):
    path = Path(path)
    require(path.is_dir(), 'Missing frozen directory: ' + str(path))
    return {str(p.relative_to(path)): digest(p) for p in sorted(path.rglob('*'))
            if p.is_file()}


def host():
    import chess
    return {'host': platform.node(), 'platform': platform.platform(),
            'logicalCpus': os.cpu_count(), 'python': sys.version,
            'pythonExecutable': str(Path(sys.executable).resolve()),
            'chessPackage': str(Path(chess.__file__).parent.resolve())}


def stamp():
    return datetime.now(timezone.utc).isoformat()


@contextmanager
def locked(path):
    import msvcrt
    # OS lock releases on process exit. The inert file is not a stale reservation.
    with Path(path).open('a+b') as stream:
        stream.seek(0)
        if not stream.read(1):
            stream.write(b'0'); stream.flush()
        stream.seek(0)
        msvcrt.locking(stream.fileno(), msvcrt.LK_NBLCK, 1)
        try:
            yield
        finally:
            stream.seek(0); msvcrt.locking(stream.fileno(), msvcrt.LK_UNLCK, 1)


def prepare(config_path, output):
    require(os.name == 'nt', 'Only Windows execution has been validated')
    require(sys.flags.optimize == 0, 'Existing auditors require Python assertions enabled')
    config_path = Path(config_path).resolve(); config = read(config_path)
    args = dict(config['arguments'])
    for key in ('java', 'runner', 'baseline', 'candidate', 'store', 'openings'):
        p = Path(args[key])
        args[key] = str((config_path.parent / p).resolve()) if not p.is_absolute() else str(p.resolve())
    require(args['baseline_workers'] == args['candidate_workers'] and
            args['baseline_workers'] in range(1, 17), 'Worker domains must match')
    require(args['concurrency'] in (1, 2, 3, 4), 'Concurrency must be 1..4')
    block_pairs = config['blockPairs']; limit = config['blockLimitSeconds']
    require(type(block_pairs) is int and block_pairs >= args['concurrency'],
            'Block needs at least concurrency opening pairs')
    require(type(limit) is int and limit > 240, 'Allow preflight plus a bounded match')
    require(config.get('assessmentPlan'), 'Prospective assessment plan required')
    require(config.get('purpose') in ('operational', 'exploratory', 'confirmatory'), 'Explicit evidence purpose required')
    protocol = None
    if config['purpose'] == 'confirmatory':
        require(config.get('assessmentProtocol'), 'Confirmatory stages require a separate prospective protocol file')
        protocol = (config_path.parent / config['assessmentProtocol']).resolve()
        require(protocol.is_file(), 'Missing prospective statistical protocol')
    out = Path(output).resolve()
    require(not out.exists(), 'Never replace an existing study')
    adapter = adapter_name(config)
    runner = module('match', Path(__file__).with_name(adapter))
    # Validate the entire schedule before creating the study.
    pairs = args.pop('pairs')
    prefixes = runner.b.opening_prefixes(args['openings'], pairs)
    known = {tuple(p) for p in prefixes}; lengths = {len(p) for p in prefixes}
    require(0 not in lengths and all(not any(tuple(p[:n]) in known for n in lengths if n < len(p))
                                    for p in prefixes), 'Ambiguous or empty opening prefixes')
    require(pairs >= block_pairs, 'Study smaller than a block')
    with Path(args['openings']).open(encoding='utf8') as stream:
        openings = [runner.b.chess.pgn.read_game(stream) for _ in range(pairs)]
    require(all(not game.end().board().is_game_over(claim_draw=True) for game in openings),
            'Terminal openings are not valid tournament calibration inputs')
    require(pairs >= block_pairs and (pairs % block_pairs == 0 or pairs % block_pairs >= args['concurrency']),
            'Final block smaller than concurrency; choose another block size')
    out.mkdir(parents=True)
    payload = out / 'inputs'; payload.mkdir()
    if protocol is not None:
        shutil.copy2(protocol, payload/'assessment-protocol.txt')
    tool_dir = payload / 'tools'; tool_dir.mkdir()
    for name in dict.fromkeys((*FILES, adapter)):
        shutil.copy2(Path(__file__).with_name(name), tool_dir / name)
    for side in ('baseline', 'candidate'):
        source = Path(args[side]); require((source/'lib').is_dir(), 'Missing installation')
        destination = payload / side
        require(not destination.is_relative_to(source), 'Study must be outside input installation')
        shutil.copytree(source, destination)
        args[side] = str(destination)
    shutil.copy2(args['runner'], payload/'fastchess.exe'); args['runner'] = str(payload/'fastchess.exe')
    shutil.copy2(args['openings'], payload/'schedule.pgn')
    args.pop('openings')
    args.update(operational=True, timeout=limit-240)
    blocks = []
    for index, first in enumerate(range(0, pairs, block_pairs)):
        count = min(block_pairs, pairs-first)
        require(count >= args['concurrency'], 'Final block smaller than concurrency; choose another block size')
        path = payload / f'block-{index:04d}-openings.pgn'
        path.write_text('\n\n'.join(str(g) for g in openings[first:first+count])+'\n', encoding='utf8')
        blocks.append({'index': index, 'firstPair': first, 'pairs': count, 'openings': str(path),
                       'pairIds': [hashlib.sha256(' '.join(p).encode()).hexdigest() for p in prefixes[first:first+count]]})
    model = Path(args['store'])/'checkpoints'/runner.b.CHECKPOINT
    require(digest(model/'network.brn-pair2') == runner.b.NETWORK_SHA256, 'Wrong evaluator')
    java_home = Path(args['java']).parent.parent
    chess_package = Path(runner.b.chess.__file__).parent
    require(runner.b.chess.__version__ == '1.11.2', 'Validated chess auditor version required')
    plan = {'schema': 1, 'createdUtc': stamp(), 'purpose': config['purpose'], 'adapter': adapter,
            'assessmentPlan': config['assessmentPlan'], 'host': host(), 'pairs': pairs,
            'arguments': args, 'blocks': blocks, 'blockLimitSeconds': limit,
            'failurePolicy': 'Preserve and block any partial/abnormal block. Never automatic replay or replacement.',
            'restartPolicy': 'Fresh JVMs at every frozen block; operational sessions do not change block boundaries.',
            'frozenTrees': {str(p): tree(p) for p in (payload, model, java_home, chess_package)},
            'pythonExecutable': {str(Path(sys.executable).resolve()): digest(sys.executable)},
            'configurationSha256': digest(config_path)}
    save(out/'plan.json', plan)
    save(out/'identity.json', {'planSha256': digest(out/'plan.json')})
    return {'plan': str(out/'plan.json'), 'pairs': pairs, 'games': 2*pairs, 'blocks': len(blocks)}


def verify(plan_path):
    require(sys.flags.optimize == 0, 'Existing auditors require Python assertions enabled')
    plan_path = Path(plan_path).resolve(); plan = read(plan_path)
    require(digest(plan_path) == read(plan_path.parent/'identity.json')['planSha256'], 'Plan identity changed')
    require(plan['host'] == host(), 'Host/Python/resource domain changed')
    require(digest(__file__) == plan['frozenTrees'][str(plan_path.parent/'inputs')]['tools\\tournament-session.py'],
            'Invoke the study frozen executor; executor identity changed')
    for path, expected in plan['frozenTrees'].items():
        require(tree(path) == expected, 'Frozen input tree changed: ' + path)
    for path, expected in plan['pythonExecutable'].items():
        require(digest(path) == expected, 'Python executable changed')
    return plan


def audit_block(plan, block, folder):
    runner = module('audit_match', Path(plan['arguments']['runner']).parent/'tools'/adapter_name(plan))
    record = read(folder/'run.json')
    expected = dict(plan['arguments'], pairs=block['pairs'], openings=block['openings'], out=str(folder))
    require(record['status'] == 'passed' and record['arguments'] == expected and
            record['runnerExitCode'] == 0 and record['inputsStable'], 'Block did not complete validly')
    cost = read(folder/'process-cost.json')
    require(cost['status'] == 'passed' and len(cost['processes']) == 2*expected['concurrency'] and
            all(p['lifetime']['exitCode'] == 0 for p in cost['processes']), 'Engine lifecycle audit failed')
    audit = runner.b.analyze(folder/'games.pgn', block['openings'], block['pairs'],
                             require_accounting=True, unordered=True, statistics_enabled=False)
    require(audit['terminations'] == {'normal': 2*block['pairs']}, 'Abnormal games block continuation')
    require(record['results'] == audit, 'Stored audit differs from fresh PGN audit')
    return {'pairs': block['pairs'], 'games': 2*block['pairs'], 'pairIds': block['pairIds'],
            'matchWallSeconds': record['matchWallSeconds'], 'runnerWallSeconds': record['wallSeconds'],
            'cpuSeconds': cost['cpuSeconds'], 'statisticsAssessed': False}


def reconcile(plan_path, plan):
    root = Path(plan_path).parent; results = []
    for block in plan['blocks']:
        folder = root/f"block-{block['index']:04d}"
        receipt = root/f"block-{block['index']:04d}.json"
        if not folder.exists():
            require(not receipt.exists(), 'Receipt without evidence')
            require(not (root/f"block-{block['index']:04d}.started.json").exists(),
                    'Prior incomplete launch; preserve and investigate')
            break
        start = read(root/f"block-{block['index']:04d}.started.json")
        require(start['planSha256'] == digest(plan_path) and start['block'] == block['index'],
                'Block ownership/identity changed')
        summary = audit_block(plan, block, folder)  # Never run an incomplete block again.
        evidence = tree(folder)
        if receipt.exists():
            saved = read(receipt)
            require(saved['planSha256'] == digest(plan_path) and saved['evidence'] == evidence and
                    saved['summary'] == summary, 'Completed block evidence changed')
        else:
            # Completion-before-receipt crash: adopt only fully audited existing output.
            save(receipt, {'planSha256': digest(plan_path), 'summary': summary,
                           'evidence': evidence, 'verifiedUtc': stamp()})
        results.append(summary)
    for block in plan['blocks'][len(results)+1:]:
        require(not (root/f"block-{block['index']:04d}").exists() and
                not (root/f"block-{block['index']:04d}.json").exists(), 'Noncontiguous output')
    return results


def utc(value):
    result = datetime.fromisoformat(value.replace('Z', '+00:00'))
    require(result.tzinfo is not None, 'Execution window must include a UTC offset')
    return result.astimezone(timezone.utc)


def execute(plan_path, stop_before, max_blocks=None, start_after=None, clear_pause=False):
    plan_path = Path(plan_path).resolve(); root = plan_path.parent
    end = utc(stop_before)
    require(end > datetime.now(timezone.utc), 'Execution window already ended')
    if start_after:
        require(utc(start_after) < end, 'Empty execution window')
    require(max_blocks is None or max_blocks > 0, 'Positive block limit required')
    with locked(root/'execution.lock'):
        plan = verify(plan_path)
        results = reconcile(plan_path, plan)
        if clear_pause:
            (root/'PAUSE').unlink(missing_ok=True)
        sessions = root/'sessions'; sessions.mkdir(exist_ok=True)
        record_path = sessions/(datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%f')+'.json')
        session = {'startedUtc': stamp(), 'stopBefore': end.isoformat(),
                   'startAfter': start_after, 'initialPairs': sum(x['pairs'] for x in results),
                   'completedBlocks': [], 'statisticsAssessed': False, 'status': 'running'}
        save(record_path, session)
        process_tools = module('owned_process', root/'inputs/tools/tournament-process.py')
        baseline = module('baseline', root/'inputs/tools/search-baseline.py')
        try:
            while start_after and datetime.now(timezone.utc) < utc(start_after):
                if (root/'PAUSE').exists():
                    break
                time.sleep(min(10, max(0, (utc(start_after)-datetime.now(timezone.utc)).total_seconds())))
            for block in plan['blocks'][len(results):]:
                if ((root/'PAUSE').exists() or
                    (max_blocks is not None and len(session['completedBlocks']) >= max_blocks) or
                    (end-datetime.now(timezone.utc)).total_seconds() < plan['blockLimitSeconds']+30):
                    break
                verify(plan_path)
                # Full-tree hashing can consume material time, or a pause can
                # arrive during it. Admit only a full block after that preflight.
                if ((root/'PAUSE').exists() or
                    (end-datetime.now(timezone.utc)).total_seconds() < plan['blockLimitSeconds']+30):
                    break
                folder = root/f"block-{block['index']:04d}"
                # A prior launch without output is ambiguous, not authorization to replay.
                start_path = root/f"block-{block['index']:04d}.started.json"
                require(not start_path.exists(), 'Prior incomplete launch; manual investigation required')
                save(start_path, {'planSha256': digest(plan_path), 'block': block['index'], 'startedUtc': stamp()})
                with (root/f"block-{block['index']:04d}.worker.txt").open('wb') as log:
                    execution = process_tools.run_owned(
                        [sys.executable, '-B', str(root/'inputs/tools/tournament-session.py'),
                         'worker', '--plan', str(plan_path), '--block', str(block['index'])],
                        log, min(plan['blockLimitSeconds'], (end-datetime.now(timezone.utc)).total_seconds()),
                        root, baseline.clean_environment())
                require(execution['exitCode'] == 0, 'Block worker failed; preserve all output and investigate')
                verify(plan_path)
                results = reconcile(plan_path, plan)
                session['completedBlocks'].append({'index': block['index'], **execution})
                save(record_path, session)
            session['status'] = 'complete' if len(results) == len(plan['blocks']) else 'paused'
        except BaseException as error:
            session.update(status='blocked', failure=repr(error))
            raise
        finally:
            session.update(endedUtc=stamp(), cumulativePairs=sum(x['pairs'] for x in results))
            save(record_path, session)
        return {k: session[k] for k in ('status', 'initialPairs', 'cumulativePairs', 'statisticsAssessed')}


def worker(plan_path, index):
    require(sys.stdin.buffer.readline() == b'GO\n', 'Missing owned-process gate')
    plan_path = Path(plan_path).resolve(); plan = verify(plan_path)
    block = plan['blocks'][index]
    runner = module('match', plan_path.parent/'inputs/tools'/adapter_name(plan))
    runner.run(SimpleNamespace(**dict(plan['arguments'], pairs=block['pairs'],
               openings=block['openings'], out=str(plan_path.parent/f'block-{index:04d}'))))


def forecast(pairs, games_per_hour, session_hours, slowdown=1.25, reserve_hours=.25):
    require(pairs > 0 and games_per_hour > 0 and session_hours > reserve_hours >= 0 and slowdown >= 1,
            'Invalid forecast inputs')
    hours = 2*pairs/games_per_hour
    return {'pairs': pairs, 'games': 2*pairs, 'gamesPerHour': games_per_hour,
            'pairsPerHour': games_per_hour/2, 'expectedPlayHours': hours,
            'planningPlayHours': hours*slowdown, 'slowdownAllowance': slowdown,
            'sessionHours': session_hours, 'reserveHoursEachSession': reserve_hours,
            'expectedSessions': math.ceil(hours/(session_hours-reserve_hours)),
            'planningSessions': math.ceil(hours*slowdown/(session_hours-reserve_hours)),
            'limits': 'Scenario allowance, not a statistical confidence interval. Include block startup/audit cost in measured rate; window reserves must cover the frozen block watchdog. No sample-size or stopping decision.'}


def export_stage(plan_path, output):
    """Publish only a complete frozen stage, without an inferential assessment."""
    plan_path = Path(plan_path).resolve(); root = plan_path.parent; output = Path(output).resolve()
    with locked(root/'execution.lock'):
        plan = verify(plan_path); results = reconcile(plan_path, plan)
        require(len(results) == len(plan['blocks']), 'Incomplete scheduled stage cannot be exported')
        require(not output.exists(), 'Never replace a stage export')
        require(not output.is_relative_to(root/'inputs'), 'Export cannot modify frozen inputs')
        output.mkdir(parents=True)
        # An interrupted export has no completion manifest and cannot be published.
        with (output/'games.pgn').open('wb') as stream:
            for block in plan['blocks']:
                stream.write((root/f"block-{block['index']:04d}"/'games.pgn').read_bytes())
                stream.write(b'\n\n')
        b = module('export_audit', root/'inputs/tools/search-baseline.py')
        audit = b.analyze(output/'games.pgn', root/'inputs/schedule.pgn', plan['pairs'],
                          require_accounting=True, unordered=True, statistics_enabled=False)
        require(audit['terminations'] == {'normal': plan['pairs']*2}, 'Stage audit failed')
        verify(plan_path)
        save(output/'audit.json', audit)
        save(output/'complete.json', {'planSha256': digest(plan_path), 'pairs': plan['pairs'],
             'games': plan['pairs']*2, 'gamesSha256': digest(output/'games.pgn'),
             'auditSha256': digest(output/'audit.json'), 'blocks': results,
             'statisticsAssessed': False, 'assessmentPlan': plan['assessmentPlan']})
        return {'status':'complete', 'pairs':plan['pairs'], 'export':str(output), 'statisticsAssessed':False}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest='action', required=True)
    p = sub.add_parser('prepare'); p.add_argument('--config', required=True); p.add_argument('--out', required=True)
    p = sub.add_parser('run'); p.add_argument('--plan', required=True)
    p.add_argument('--stop-before', required=True); p.add_argument('--start-after')
    p.add_argument('--max-blocks', type=int)
    p.add_argument('--clear-pause', action='store_true')
    p = sub.add_parser('worker'); p.add_argument('--plan', required=True); p.add_argument('--block', type=int, required=True)
    p = sub.add_parser('status'); p.add_argument('--plan', required=True)
    p = sub.add_parser('export'); p.add_argument('--plan', required=True); p.add_argument('--out', required=True)
    p = sub.add_parser('pause'); p.add_argument('--plan', required=True)
    p = sub.add_parser('forecast'); p.add_argument('--pairs', type=int, required=True)
    p.add_argument('--games-per-hour', type=float, required=True); p.add_argument('--session-hours', type=float, required=True)
    p.add_argument('--slowdown', type=float, default=1.25); p.add_argument('--reserve-hours', type=float, default=.25)
    a = parser.parse_args()
    if a.action == 'prepare': result = prepare(a.config, a.out)
    elif a.action == 'run': result = execute(a.plan, a.stop_before, a.max_blocks, a.start_after, a.clear_pause)
    elif a.action == 'worker': worker(a.plan, a.block); return
    elif a.action == 'export': result = export_stage(a.plan, a.out)
    elif a.action == 'pause':
        path = Path(a.plan).resolve()
        require(digest(path) == read(path.parent/'identity.json')['planSha256'], 'Plan identity changed')
        save(path.parent/'PAUSE', {'requestedUtc':stamp(), 'planSha256':digest(path)})
        result = {'pause':'requested after the current complete block; does not kill games'}
    elif a.action == 'status':
        with locked(Path(a.plan).resolve().parent/'execution.lock'):
            p = verify(a.plan); done = reconcile(Path(a.plan).resolve(), p)
            result = {'completePairs': sum(x['pairs'] for x in done), 'totalPairs': p['pairs'],
                      'status': 'complete' if len(done) == len(p['blocks']) else 'paused', 'statisticsAssessed': False}
    else: result = forecast(a.pairs, a.games_per_hour, a.session_hours, a.slowdown, a.reserve_hours)
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    main()
