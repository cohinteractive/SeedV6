"""Bounded BRE-Pair 2 preflight and Fastchess adapter (Python chess==1.11.2).

Fastchess owns games, clocks, pairing and failure detection. This adapter freezes
inputs, verifies the real UCI identity, and independently audits the resulting PGN.
No training, engine selection, promotion, or strength acceptance is performed.
"""
import argparse
import collections
import hashlib
import json
import math
import os
from pathlib import Path
import platform
import queue
import random
import re
import shutil
import signal
import statistics
import subprocess
import threading
import time

import chess
import chess.pgn

CHECKPOINT = 'g000032-s001753088-5837c7f7192aa3cf9a265388f93649327455e804541398ae3ef6b69e09014304'
NETWORK_SHA256 = '5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7'


def digest(path):
    with Path(path).open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def save(path, value):
    Path(path).write_text(json.dumps(value, indent=2, allow_nan=False) + '\n', encoding='utf-8')


def command(java, installation, store, qpolicy='static', lmr='off', aspiration='off', lmp='off', check='off', reuse='off', nmp='off', internal='off'):
    installation = Path(installation).resolve()
    research = (installation / 'research-launcher.properties').is_file()
    classpath = os.pathsep.join([str(installation / 'classes'), str(installation / 'lib' / '*')]) if research else str(installation / 'lib' / '*')
    main_class = 'com.ohinteractive.seedv6.integration.ResearchUciMain' if research else 'com.ohinteractive.seedv6.Main'
    return [str(Path(java).resolve()), '-Xms256m', '-Xmx512m', '-XX:ActiveProcessorCount=1',
            '-Djava.awt.headless=true', '-Dseedv6.syzygy.config=', '-Dseedv6.syzygy.path=',
            '-Dseedv6.uci.pair2Store=' + str(Path(store).resolve()),
            '-Dseedv6.uci.checkpoint=' + CHECKPOINT,
            '-Dseedv6.uci.networkSha256=' + NETWORK_SHA256, '-Dseedv6.uci.warmupDepth=4',
            '-Dseedv6.uci.qsearchResearch=' + qpolicy,
            '-Dseedv6.uci.lmrResearch=' + lmr,
            '-Dseedv6.uci.aspirationResearch=' + aspiration,
            *(['-Dseedv6.uci.lmpResearch=' + lmp] if lmp != 'default' else []),
            *(['-Dseedv6.uci.checkResearch=' + check] if check != 'off' else []),
            *(['-Dseedv6.uci.reuseResearch=' + reuse] if reuse != 'default' else []),
            *(['-Dseedv6.uci.nmpResearch=' + nmp] if nmp != 'off' else []),
            *(['-Dseedv6.uci.internalResearch=' + internal] if internal != 'off' else []),
            '-cp', classpath, main_class]


def clean_environment():
    env = os.environ.copy()
    for name in ('JAVA_TOOL_OPTIONS', 'JDK_JAVA_OPTIONS', '_JAVA_OPTIONS', 'CLASSPATH'):
        env.pop(name, None)
    return env


class Uci:
    """Only protocol preflight, never a game runner. All reads have a deadline."""
    def __init__(self, cmd, transcript):
        self.log = Path(transcript).open('w', encoding='utf-8')
        self.lines = queue.Queue()
        self.process = subprocess.Popen(cmd, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                                        stderr=subprocess.PIPE, text=True, encoding='utf-8', env=clean_environment())
        self.errors = []
        self.reader = threading.Thread(target=self._read, daemon=True)
        self.err_reader = threading.Thread(target=lambda: self.errors.extend(self.process.stderr), daemon=True)
        self.reader.start()
        self.err_reader.start()

    def _read(self):
        for line in self.process.stdout:
            self.lines.put(line.rstrip())
        self.lines.put(None)

    def send(self, text):
        self.log.write('> ' + text + '\n'); self.log.flush()
        self.process.stdin.write(text + '\n'); self.process.stdin.flush()

    def until(self, prefix, timeout=30):
        end = time.monotonic() + timeout
        lines = []
        while True:
            line = self.lines.get(timeout=max(.001, end - time.monotonic()))
            if line is None:
                raise RuntimeError('Engine exited: ' + ''.join(self.errors))
            self.log.write('< ' + line + '\n'); self.log.flush()
            lines.append(line)
            if line.startswith(prefix):
                return lines

    def close(self):
        try:
            if self.process.poll() is None:
                self.send('quit'); self.process.wait(timeout=5)
        finally:
            if self.process.poll() is None:
                self.process.kill(); self.process.wait()
            self.reader.join(2); self.err_reader.join(2)
            self.log.write('stderr: ' + ''.join(self.errors)); self.log.close()


def inspect_result(board, lines):
    best = lines[-1].split()[1]
    if best == '0000':
        if not (board.is_game_over(claim_draw=True)):
            raise ValueError('Missing legal bestmove')
    elif chess.Move.from_uci(best) not in board.legal_moves:
        raise ValueError('Illegal bestmove: ' + best)
    infos = [line for line in lines if line.startswith('info depth ')]
    for line in infos:
        if ' pv ' in line:
            pvboard = board.copy()
            for move in line.split(' pv ', 1)[1].split():
                parsed = chess.Move.from_uci(move)
                if parsed not in pvboard.legal_moves:
                    raise ValueError('Illegal PV prefix: ' + line)
                pvboard.push(parsed)
    requests = [line for line in lines if line.startswith('info string request ')]
    if len(requests) != 1 or any('search failed' in line for line in lines):
        raise ValueError('Missing request accounting or Search failure')
    values = dict(item.split('=', 1) for item in requests[0].split()[3:])
    values.update(bestmove=best, finalInfo=infos[-1] if infos else None)
    for key in ('nodes', 'elapsedNanos'):
        values[key] = int(values[key])
    return values


def preflight(cmd, output, workers=1):
    engine = Uci(cmd, output)
    report = {'commands': cmd, 'workers': workers, 'requests': []}
    start = time.monotonic()
    try:
        engine.send('uci'); hello = engine.until('uciok')
        if not any(line.startswith('option name Threads type spin ') for line in hello) or not any(
                line.startswith('option name OwnBook type check ') for line in hello):
            raise ValueError('Required UCI options unavailable')
        identity = [s for s in hello if s.startswith('info string evaluator=')]
        if len(identity) != 1 or ('checkpoint=' + CHECKPOINT) not in identity[0] or ('sha256=' + NETWORK_SHA256) not in identity[0]:
            raise ValueError('Incorrect runtime evaluator identity')
        engine.send('setoption name OwnBook value false')
        engine.send('setoption name Threads value ' + str(workers))
        engine.send('isready'); ready = engine.until('readyok')
        configured = [s for s in ready if s.startswith('info string evaluator=')]
        if configured: identity = [configured[-1]]
        report['identity'] = identity[0]
        policy_prefix = '-Dseedv6.uci.qsearchResearch='
        policy = next((s[len(policy_prefix):] for s in cmd if s.startswith(policy_prefix)), 'production')
        expected_leaves = ('unpruned' if workers == 1 else 'static') if policy == 'production' else policy
        if (' leaves=' + expected_leaves + ' ') not in identity[0]:
            raise ValueError('Incorrect runtime leaf policy identity')
        lmr_prefix = '-Dseedv6.uci.lmrResearch='
        lmr = next((s[len(lmr_prefix):] for s in cmd if s.startswith(lmr_prefix)), 'production')
        prefix = '-Dseedv6.uci.lmpResearch='
        lmp = next((s[len(prefix):] for s in cmd if s.startswith(prefix)), 'production')
        prefix = '-Dseedv6.uci.aspirationResearch='
        aspiration = next((s[len(prefix):] for s in cmd if s.startswith(prefix)), 'off')
        check_lmr_identity(identity[0], policy, lmr, workers, lmp)
        check_prefix = '-Dseedv6.uci.checkResearch='
        check = next((s[len(check_prefix):] for s in cmd if s.startswith(check_prefix)), 'off')
        reuse_prefix = '-Dseedv6.uci.reuseResearch='
        requested_reuse = next((s[len(reuse_prefix):] for s in cmd if s.startswith(reuse_prefix)), 'default')
        nmp_prefix = '-Dseedv6.uci.nmpResearch='
        nmp = next((s[len(nmp_prefix):] for s in cmd if s.startswith(nmp_prefix)), 'off')
        internal_prefix = '-Dseedv6.uci.internalResearch='
        internal = next((s[len(internal_prefix):] for s in cmd if s.startswith(internal_prefix)), 'off')
        check_lmp_identity(identity[0], lmp, policy, lmr, aspiration, workers, check, requested_reuse, nmp, internal)
        actual_aspiration = re.search(r'(?:^| )aspiration=(\S+)', identity[0])
        if (actual_aspiration.group(1) if actual_aspiration else 'off') != aspiration:
            raise ValueError('Incorrect runtime aspiration policy identity')
        prefix = '-Dseedv6.uci.checkResearch='
        extension = next((s[len(prefix):] for s in cmd if s.startswith(prefix)), 'off')
        actual_extension = re.search(r'(?:^| )check=(\S+)', identity[0])
        if (actual_extension.group(1) if actual_extension else 'off') != extension:
            raise ValueError('Incorrect runtime check extension identity')
        prefix = '-Dseedv6.uci.reuseResearch='
        reuse = next((s[len(prefix):] for s in cmd if s.startswith(prefix)), 'default')
        actual = dict(part.split('=', 1) for part in identity[0].split() if '=' in part).get('reuse', 'off')
        if actual != resolved_reuse(reuse,policy,lmr,aspiration,lmp,extension,workers): raise ValueError('Incorrect runtime iteration reuse identity')
        report['startupAndWarmupSeconds'] = time.monotonic() - start
        fens = [chess.STARTING_FEN,
                'r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1',
                '8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1']
        for fen in fens:
            observations = []
            for _ in range(3):
                engine.send('ucinewgame'); engine.send('position fen ' + fen)
                engine.send('go depth 4')
                result = inspect_result(chess.Board(fen), engine.until('bestmove '))
                observations.append(result); report['requests'].append({'fen': fen, 'budget': 'depth 4', **result})
            if workers == 1:
                # Only timing/NPS may differ; full PV, score, depth, nodes must repeat.
                def stable(r):
                    info = r['finalInfo'].split()
                    return r['bestmove'], r['nodes'], info[info.index('score')+1:info.index('time')], info[info.index('pv'):]
                if len({str(stable(r)) for r in observations}) != 1:
                    raise ValueError('Non-repeatable fixed-depth production Search')
        for budget in ('nodes 20000', 'movetime 100', 'wtime 1000 btime 1000 winc 50 binc 50', 'infinite'):
            engine.send('ucinewgame'); engine.send('position startpos'); engine.send('go ' + budget)
            if budget == 'infinite':
                engine.until('info depth 2 '); engine.send('stop')
            result = inspect_result(chess.Board(), engine.until('bestmove '))
            if budget == 'nodes 20000' and result['nodes'] != 20000:
                raise ValueError('Incorrect whole-request node budget')
            report['requests'].append({'fen': chess.STARTING_FEN, 'budget': budget, **result})
        for fen in ('7k/6Q1/5K2/8/8/8/8/8 b - - 0 1', '7k/5K2/6Q1/8/8/8/8/8 b - - 0 1'):
            engine.send('position fen ' + fen); engine.send('go depth 4')
            report['requests'].append({'fen': fen, 'budget': 'depth 4', **inspect_result(chess.Board(fen), engine.until('bestmove '))})
        report['status'] = 'passed'
        return report
    finally:
        engine.close()


def elo(score):
    if score <= 0: return '-infinity'
    if score >= 1: return '+infinity'
    return 400 * math.log10(score / (1 - score))


def resolved_reuse(reuse, policy, lmr, aspiration, lmp, check, workers):
    if reuse != 'default': return reuse
    return 'effort' if (policy == lmr == 'production' and aspiration == check == 'off'
                        and lmp in ('production','default') and workers == 1) else 'off'


def check_lmp_identity(identity, lmp, policy='production', lmr='production', aspiration='off', workers=1, check='off', reuse='off', nmp='off', internal='off'):
    fields = dict(part.split('=', 1) for part in identity.split() if '=' in part)
    production = lmp in ('production', 'default')
    expected = ('window' if policy == 'production' and lmr == 'production'
                and aspiration == 'off' and workers == 1 else 'off') if production else lmp
    actual = fields.get('lmp') if production else fields.get('lmp', 'off')
    if actual != expected:
        raise ValueError('LMP policy identity mismatch')
    if expected != 'off' and fields.get('nominalLmpProof') != 'off':
        raise ValueError('Missing LMP proof boundary')
    if not production and lmp != 'off' and fields.get('experimental') != 'true':
        raise ValueError('Missing experimental LMP identity')
    if production and policy == lmr == 'production' and aspiration == 'off':
        if fields.get('mode') != 'production' or (fields.get('experimental') == 'true' and check == 'off' and reuse in ('off','default') and nmp == 'off' and internal == 'off'):
            raise ValueError('Default LMP requires the canonical production composition')
    if check != 'off' and (fields.get('check') != check or fields.get('experimental') != 'true'):
        raise ValueError('Missing explicit check extension identity')
    if (fields.get('reuse', 'off') != resolved_reuse(reuse,policy,lmr,aspiration,lmp,check,workers)
            or (reuse not in ('off','default') and fields.get('experimental') != 'true')):
        raise ValueError('Missing explicit iteration reuse identity')
    if fields.get('internal', 'off') != internal or (internal != 'off' and fields.get('experimental') != 'true'):
        raise ValueError('Incorrect internal search policy identity')
    if fields.get('nmp', 'off') != nmp or (nmp != 'off' and fields.get('experimental') != 'true'):
        raise ValueError('Missing explicit null-move identity')


def check_lmr_identity(identity, policy, lmr, workers, lmp=None):
    fields = dict(part.split('=', 1) for part in identity.split() if '=' in part)
    expected = ('quiet' if workers == 1 and policy == 'production' else 'off') if lmr == 'production' else lmr
    # Frozen pre-LMR binaries report leaf mode but have no LMR field.
    legacy_off = lmr == 'off' and 'lmr' not in fields and fields.get('mode') in ('production', 'control')
    if fields.get('lmr') != expected and not legacy_off:
        raise ValueError('Incorrect runtime LMR policy identity')
    explicit_lmp_control = lmp == 'off' and fields.get('lmp') == 'off' and fields.get('mode') == 'control'
    if policy == 'production' and lmr == 'production' and fields.get('mode') != 'production' and not explicit_lmp_control:
        raise ValueError('Production comparison requires the canonical production composition')


def paired_statistics(pairs):
    if not pairs or any(x not in (0, .5, 1, 1.5, 2) for x in pairs):
        raise ValueError('Need complete valid pair totals')
    n = len(pairs); mean = sum(pairs) / (2 * n)
    # Distribution-free two-sided 95% bound on independent bounded pair scores.
    # Conservative, nondegenerate even for all draws; no independent-game fiction.
    radius = math.sqrt(math.log(40) / (2 * n))
    interval = [max(0, mean-radius), min(1, mean+radius)]
    variance = sum((x/2-mean)**2 for x in pairs)/(n-1) if n > 1 else 0
    # Pair-level CLT, never a binomial/game-level error bar. Suppress degenerate
    # or tiny-sample intervals; the conservative bound remains available.
    standard_error = math.sqrt(variance/n)
    normal = None
    if n >= 30 and variance > 0:
        width = statistics.NormalDist().inv_cdf(.975)*standard_error
        normal = [max(0, mean-width), min(1, mean+width)]
    return {'pairs': n, 'pentanomial': [pairs.count(x) for x in (0, .5, 1, 1.5, 2)],
            'score': mean, 'logisticElo': elo(mean), 'pairedHoeffding95Score': interval,
            'pairedHoeffding95Elo': [elo(x) for x in interval],
            'pairScoreStandardError': standard_error,
            'pairedNormal95Score': normal, 'pairedNormal95Elo': [elo(x) for x in normal] if normal else None,
            'method': 'Fixed sample opening-pair mean; pair-variance normal approximation (only n>=30, nonzero variance), plus conservative bounded-pair Hoeffding alpha=.05. No acceptance inferred.'}


def select_openings(book, output, count, seed, partition, exclude=()):
    """Deduplicate final placement/status; disjoint exploration/confirmation buckets.

    No evaluator filters. Partition and sample are determined before matches.
    PGN prefixes retain their real history, castling, EP and clock state.
    """
    if partition not in (0, 1) or count < 1: raise ValueError('Invalid opening selection')
    admitted = {}; rng = random.Random(seed)
    excluded = set(); exclusions = {}
    for receipt in exclude:
        receipt = Path(receipt)
        previous = json.loads(receipt.read_text(encoding='utf-8-sig'))
        keys = previous['positionKeys']
        if len(keys) != previous['count'] or len(set(keys)) != len(keys):
            raise ValueError('Invalid excluded opening selection')
        excluded.update(keys)
        exclusions[str(receipt.resolve())] = digest(receipt)
    with Path(book).open(encoding='utf-8') as stream:
        while (game := chess.pgn.read_game(stream)) is not None:
            if game.errors: raise ValueError('Invalid source opening')
            moves = list(game.mainline_moves())
            if len(moves) != 16 or game.board().fen() != chess.STARTING_FEN:
                raise ValueError('Expected startpos and eight full opening moves')
            board = game.end().board()
            key = ' '.join(board.fen().split()[:4])
            if board.is_game_over(claim_draw=True): continue
            if int(hashlib.sha256(key.encode()).hexdigest(), 16) % 2 != partition: continue
            if key in excluded: continue
            if key not in admitted:
                fresh = chess.pgn.Game(); fresh.headers['Event'] = 'SeedV6 frozen opening'
                fresh.headers['Result'] = '*'; node = fresh
                for move in moves: node = node.add_main_variation(move)
                admitted[key] = str(fresh)
    keys = rng.sample(sorted(admitted), count)
    output = Path(output)
    if output.exists(): raise ValueError('Opening output already exists')
    output.write_text('\n\n'.join(admitted[key] for key in keys) + '\n', encoding='utf-8')
    save(output.with_suffix('.selection.json'), {'source': str(Path(book).resolve()), 'sourceSha256': digest(book),
         'seed': seed, 'partition': partition, 'availableDistinctPositions': len(admitted),
         'count': count, 'outputSha256': digest(output), 'positionKeys': keys,
         'excludedSelections': exclusions, 'excludedDistinctPositions': len(excluded),
         'selection': 'sha256(final FEN first four fields) mod 2; sorted unique keys; Python random.sample'})


def opening_prefixes(openings, pairs):
    with Path(openings).open(encoding='utf-8') as stream:
        prefixes = []; positions = set()
        while (game := chess.pgn.read_game(stream)) is not None:
            if game.errors: raise ValueError('Invalid opening PGN')
            prefixes.append([m.uci() for m in game.mainline_moves()])
            positions.add(' '.join(game.end().board().fen().split()[:4]))
            if game.board().fen() != chess.STARTING_FEN: raise ValueError('Opening must retain startpos history')
    if len(prefixes) != pairs or len(positions) != pairs:
        raise ValueError('Opening count/duplicate mismatch')
    return prefixes


def analyze(pgn, openings, pairs, require_accounting=False, unordered=False):
    prefixes = opening_prefixes(openings, pairs)
    games = []
    with Path(pgn).open(encoding='utf-8') as stream:
        while (game := chess.pgn.read_game(stream)) is not None:
            if game.errors: raise ValueError('Invalid match PGN')
            games.append(game)
    if len(games) != 2 * pairs: raise ValueError('Missing or excess game records')
    if unordered:
        # Concurrent games can finish out of order. Bind every raw record to its
        # unique frozen legal prefix and color; never infer missing/duplicate games.
        slots = [None] * len(games)
        for game in games:
            moves = [m.uci() for m in game.mainline_moves()]
            matches = [i for i, prefix in enumerate(prefixes) if moves[:len(prefix)] == prefix]
            if len(matches) != 1: raise ValueError('Unknown or ambiguous concurrent opening')
            white = game.headers.get('White')
            if white not in ('baseline', 'candidate'): raise ValueError('Unknown concurrent engine')
            slot = 2 * matches[0] + (white == 'candidate')
            if slots[slot] is not None: raise ValueError('Duplicate concurrent opening/color')
            slots[slot] = game
        if any(game is None for game in slots): raise ValueError('Missing concurrent opening/color')
        games = slots
    scores = []; terminations = collections.Counter(); records = []
    resources = {name: {'requests': 0, 'nodes': 0, 'elapsedNanos': 0} for name in ('baseline', 'candidate')}
    # Default output order is checked strictly; concurrent output was bound above.
    for index, game in enumerate(games):
        moves = [m.uci() for m in game.mainline_moves()]
        if moves[:len(prefixes[index//2])] != prefixes[index//2]: raise ValueError('Opening mismatch')
        if game.headers['White'] != ('baseline' if index % 2 == 0 else 'candidate') or game.headers['Black'] != ('candidate' if index % 2 == 0 else 'baseline'):
            raise ValueError('Color/pair mismatch')
        result = game.headers['Result']
        if result not in ('1-0', '0-1', '1/2-1/2'): raise ValueError('Incomplete game result')
        white = {'1-0': 1, '0-1': 0, '1/2-1/2': .5}[result]
        scores.append(white if game.headers['White'] == 'candidate' else 1-white)
        termination = game.headers.get('Termination', '')
        if termination.lower() == 'normal':
            board = game.end().board()
            if board.is_checkmate():
                if result != ('0-1' if board.turn else '1-0'): raise ValueError('Incorrect mate result')
            elif result != '1/2-1/2' or not (board.is_stalemate() or board.is_insufficient_material()
                    or board.is_repetition(3) or board.halfmove_clock >= 100):
                raise ValueError('Unexplained normal result; no score adjudication permitted')
        terminations[termination] += 1
        for ply, node in enumerate(game.mainline()):
            if ply < len(prefixes[index//2]): continue
            telemetry = re.search(r'info string request nodes=(\d+) elapsedNanos=(\d+) termination=(\w+)', node.comment)
            if not telemetry:
                if require_accounting: raise ValueError('Missing whole-request telemetry')
                continue
            actor = game.headers['White' if ply % 2 == 0 else 'Black']
            metric = resources[actor]; metric['requests'] += 1
            metric['nodes'] += int(telemetry[1]); metric['elapsedNanos'] += int(telemetry[2])
            if telemetry[3] not in ('COMPLETED', 'TIME_LIMIT', 'TIME_ALLOCATION', 'NODE_LIMIT', 'STOPPED'):
                raise ValueError('Unexpected Search termination: ' + telemetry[3])
        records.append({'headers': dict(game.headers), 'plies': len(moves), 'candidateScore': scores[-1]})
    return {'wdl': [scores.count(x) for x in (1, .5, 0)], 'terminations': dict(terminations),
            'statistics': paired_statistics([scores[i]+scores[i+1] for i in range(0, len(scores), 2)]),
            'wholeRequestResources': resources, 'games': records}


def failure_probes(cmd):
    results = []
    for key, value in [('networkSha256', '0'*64), ('checkpoint', 'missing')]:
        prefix = '-Dseedv6.uci.' + key + '='
        invalid = [prefix+value if item.startswith(prefix) else item for item in cmd]
        result = subprocess.run(invalid, input='uci\nisready\nquit\n', capture_output=True,
                                text=True, env=clean_environment(), timeout=30)
        if result.returncode == 0 or 'uciok' in result.stdout or 'readyok' in result.stdout:
            raise ValueError('Invalid evaluator produced a usable UCI engine')
        results.append({'invalidOption': key, 'exit': result.returncode, 'stdout': result.stdout, 'stderr': result.stderr})
    return results


def installation_hashes(path):
    files = sorted((Path(path).resolve() / 'lib').glob('*.jar'))
    if not files: raise ValueError('No installed JARs: ' + str(path))
    return {str(file): digest(file) for file in files}


def run(args):
    if args.pairs < 1 or args.timeout < 1: raise ValueError('Positive pair count and timeout required')
    concurrency = getattr(args, 'concurrency', 1)
    if concurrency not in (1, 2, 3, 4): raise ValueError('Match concurrency must be 1..4')
    if args.workers != 1 and any(getattr(args, name + '_lmp', 'off') not in ('off', 'production', 'default') for name in ('baseline', 'candidate')):
        raise ValueError('LMP research requires one worker')
    if args.workers != 1 and any(getattr(args, name + '_lmr', 'off') not in ('off', 'production') for name in ('baseline', 'candidate')):
        raise ValueError('LMR research requires one worker')
    if args.workers != 1 and any(getattr(args, name + '_qpolicy', 'static') not in ('static', 'production')
                                for name in ('baseline', 'candidate')):
        raise ValueError('Experimental qsearch comparisons require one worker')
    if args.workers != 1 and any(getattr(args, side + '_check', 'off') != 'off' for side in ('baseline', 'candidate')):
        raise ValueError('Check research requires one worker')
    if args.workers != 1 and any(getattr(args, side + '_reuse', 'off') not in ('off','default') for side in ('baseline', 'candidate')):
        raise ValueError('Reuse research requires one worker')
    out = Path(args.out).resolve(); out.mkdir(parents=True, exist_ok=False)
    report = {'status': 'running', 'python': platform.python_version(), 'chess': chess.__version__,
              'os': platform.platform(), 'machine': platform.machine(), 'logicalCpus': os.cpu_count(),
              'checkpoint': CHECKPOINT, 'networkSha256': NETWORK_SHA256, 'arguments': vars(args)}
    started = time.monotonic()
    try:
        network = Path(args.store) / 'checkpoints' / CHECKPOINT / 'network.brn-pair2'
        if digest(network) != NETWORK_SHA256: raise ValueError('Selected network hash mismatch')
        java = shutil.which(args.java)
        if not java: raise ValueError('Java unavailable')
        report['javaVersion'] = subprocess.run([java, '-version'], capture_output=True, text=True, check=True).stderr
        frozen = {str(network.resolve()): digest(network), str(Path(java).resolve()): digest(java),
                  str(Path(__file__).resolve()): digest(__file__)}
        frozen.update({str(p.resolve()): digest(p) for p in network.parent.iterdir() if p.is_file()})
        for name in ('baseline', 'candidate'):
            install = getattr(args, name)
            if install: frozen.update(installation_hashes(install))
        report['filesBefore'] = frozen
        commands = {name: command(java, getattr(args, name), args.store, getattr(args, name + '_qpolicy', 'static'),
                                 getattr(args, name + '_lmr', 'off'), getattr(args, name + '_aspiration', 'off'),
                                 getattr(args, name + '_lmp', 'off'), getattr(args, name + '_check', 'off'), getattr(args, name + '_reuse', 'off'), getattr(args, name + '_nmp', 'off'), getattr(args, name + '_internal', 'off'))
                    for name in ('baseline', 'candidate') if getattr(args, name)}
        save(out / 'run.json', report)
        for name, cmd in commands.items():
            report[name + 'Preflight'] = preflight(cmd, out / (name + '-uci.txt'), args.workers)
        report['invalidConfigurationChecks'] = failure_probes(commands['baseline'])
        if args.runner:
            if not args.candidate or not args.openings: raise ValueError('Match requires candidate and openings')
            # Selection is completed before the runner starts; input has exactly one record per pair.
            analyze_openings = Path(args.openings).resolve()
            opening_prefixes(analyze_openings, args.pairs)
            frozen[str(analyze_openings)] = digest(analyze_openings)
            frozen[str(Path(args.runner).resolve())] = digest(args.runner)
            report['runnerVersion'] = subprocess.run([args.runner, '--version'], capture_output=True, text=True, check=True).stdout
            cmd = [str(Path(args.runner).resolve())]
            for name, engine in commands.items():
                import shlex
                arguments = subprocess.list2cmdline(engine[1:]) if os.name == 'nt' else shlex.join(engine[1:])
                cmd += ['-engine', 'name=' + name, 'cmd=' + engine[0], 'args=' + arguments]
            cmd += ['-each', 'proto=uci', 'tc=' + args.tc, 'timemargin=0', 'restart=off',
                    'option.Threads=' + str(args.workers), 'option.OwnBook=false',
                    '-rounds', str(args.pairs), '-repeat', '-concurrency', str(concurrency), '-srand', str(args.seed),
                    '-openings', 'file=' + str(analyze_openings), 'format=pgn', 'order=sequential',
                    '-pgnout', 'file=' + str(out / 'games.pgn'), 'nodes=true', 'nps=true', 'pv=true', 'timeleft=true', 'latency=true',
                    'match_line=^info string request ',
                    '-report', 'penta=true', '-ratinginterval', '2', '-autosaveinterval', '0', '-strict']
            report['runnerCommand'] = cmd; save(out / 'run.json', report)
            with (out / 'runner.txt').open('w', encoding='utf-8') as log:
                process = subprocess.Popen(cmd, cwd=out, env=clean_environment(), stdout=log, stderr=subprocess.STDOUT,
                                           start_new_session=os.name != 'nt')
                try:
                    returncode = process.wait(timeout=args.timeout)
                except BaseException:
                    # Only the process tree this invocation owns; leave other engines alone.
                    if process.poll() is None:
                        if os.name == 'nt':
                            subprocess.run(['taskkill', '/PID', str(process.pid), '/T', '/F'], capture_output=True, check=True)
                        else: os.killpg(process.pid, signal.SIGKILL)
                        process.wait(timeout=10)
                    raise
            report['runnerExit'] = returncode
            # Preserve adverse scores even when the runner failed. Never silently drop them.
            report['results'] = analyze(out / 'games.pgn', analyze_openings, args.pairs,
                                        require_accounting=True, unordered=concurrency > 1)
            if returncode: raise ValueError('Fastchess failed; see runner.txt and preserved results')
            failures = [t for t in report['results']['terminations'] if t.lower() != 'normal']
            if failures: raise ValueError('Adverse game termination: ' + repr(failures))
        if any(digest(path) != value for path, value in frozen.items()): raise ValueError('Frozen input changed during run')
        report['inputsStable'] = True; report['status'] = 'passed'
    except BaseException as error:
        report['status'] = 'failed'; report['failure'] = repr(error)
        raise
    finally:
        report['wallSeconds'] = time.monotonic() - started
        save(out / 'run.json', report)


if __name__ == '__main__':
    import sys
    if len(sys.argv) > 1 and sys.argv[1] == 'openings':
        parser = argparse.ArgumentParser(description=select_openings.__doc__)
        parser.add_argument('book'); parser.add_argument('output')
        parser.add_argument('--count', type=int, required=True); parser.add_argument('--seed', type=int, required=True)
        parser.add_argument('--partition', type=int, choices=(0, 1), required=True)
        parser.add_argument('--exclude-selection', action='append', default=[],
                            help='Prior selection receipt whose final positions must be excluded')
        a = parser.parse_args(sys.argv[2:])
        select_openings(a.book, a.output, a.count, a.seed, a.partition, a.exclude_selection)
        sys.exit(0)
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--baseline', required=True, help='Frozen installDist directory')
    parser.add_argument('--candidate', help='Separate candidate installDist directory; same path for A/A')
    policies = ('static', 'production', 'unpruned', 'see-promo', 'see-check', 'q4', 'q8', 'q12',
                'initial-checks', 'recapture', 'see-initial-checks', 'check-only', 'q2')
    parser.add_argument('--baseline-qpolicy', choices=policies, default='static')
    parser.add_argument('--candidate-qpolicy', choices=policies, default='static')
    for side in ('baseline', 'candidate'):
        parser.add_argument('--' + side + '-internal', choices=('off','observe','iid','iid_wide','iir_scout','iir_all','hybrid','iid_shallow','iid_near'), default='off')
        parser.add_argument('--' + side + '-nmp', choices=('off','observe','fixed','shallow','adaptive','verified','guarded'), default='off')
        parser.add_argument('--' + side + '-reuse', choices=('default','off','observe','root','pv','score','effort','history','pvhistory'), default='off')
        parser.add_argument('--' + side + '-check', choices=('off', 'boundary', 'giving', 'evasion', 'forced', 'leaf', 'repeat'), default='off')
        parser.add_argument('--' + side + '-lmp', choices=('off', 'production', 'default', 'shallow', 'lmr', 'combined', 'guarded', 'd2', 'window',
                            'history-protect', 'history-extra', 'history-replace', 'history-deep', 'history-gate', 'history-leaf'), default='off')
        parser.add_argument('--' + side + '-aspiration', choices=('off', 'one16', 'wide64', 'widen16', 'adaptive16', 'alpha16', 'parity16'), default='off')
    parser.add_argument('--baseline-lmr', choices=('production', 'off', 'fixed', 'scaled', 'scout', 'quiet'), default='off')
    parser.add_argument('--candidate-lmr', choices=('production', 'off', 'fixed', 'scaled', 'scout', 'quiet'), default='off')
    parser.add_argument('--store', required=True)
    parser.add_argument('--out', required=True, help='New output directory')
    parser.add_argument('--java', default='java')
    parser.add_argument('--workers', type=int, choices=range(1, 17), default=1)
    parser.add_argument('--concurrency', type=int, choices=range(1, 5), default=1,
                        help='Simultaneous games, distinct from Search workers; freeze as a resource condition')
    parser.add_argument('--runner'); parser.add_argument('--openings')
    parser.add_argument('--pairs', type=int, default=2)
    parser.add_argument('--tc', default='5+0.05')
    parser.add_argument('--seed', type=int, default=20261009)
    parser.add_argument('--timeout', type=int, default=600)
    run(parser.parse_args())
