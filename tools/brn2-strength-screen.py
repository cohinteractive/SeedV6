"""Pinned BRN supervision strength screen; smoke gate, read-only stores, hard watchdog.

Build and targeted-test the Java tool first. Run smoke with a NEW app/build output,
then screen with the same output. No checkpoint is written or promoted.
"""
from pathlib import Path
import argparse
import collections
import hashlib
import json
import math
import os
import statistics
import struct
import subprocess
import time

ROOT = Path(__file__).resolve().parents[1]
PIN = 'g000074-s000008942-2e22ccbfdeb18f5f26f91f0df183411ac1d01844ccd43e56e9005ee3f5837bc6'
PINS = {
    'brn50': ('experimental-supervision-ablation-20260923/blended', 125,
              '570e78b8e64369ca2fa9ec102c8f5fbbb7a0f39ef205062a4cd44cc12e726935',
              '36b77a6aaefcf84616ef7aa0a0f3f040ffeb46955d6790634e75057d319136c9'),
    'brn75': ('experimental-supervision-ablation-weight-sweep-20260923/weight0p75', 125,
              '6124771b5f83e86964aad8fa6f441dde378aca58b0aabe8541bbd9602b456e37',
              '557863160233cfbeec56123dc8a5b14382b7830965039667f6c8055122a1fb74'),
    'brn100': ('experimental-supervision-ablation-20260923/teacher', 127,
               '286dd8151abe7e935c1fd8b26834171a7c08565357a4416e42582ceffb2ed1d8',
               '7d92e70fdb343b9e4d78df1cf8fb89acf818431a353cf04469edfdbee8363872'),
    'nnue74': (PIN, 74, '3279ff72654f56c8f5ce3cc89cc55d1f253de6ae9df00bd61a41293f57dd16a9',
               '453b7259d4e8041fa4abb8cc0887540434871d2b151c33676f36e749e0c6cb34'),
}


def check(condition, message):
    if not condition:
        raise ValueError(message)


def sha(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def save(path, data):
    path.write_text(json.dumps(data, indent=2, allow_nan=False) + '\n', encoding='utf-8')


def records(path):
    with path.open(encoding='utf-8') as stream:
        for line in stream:
            yield json.loads(line)


def fingerprint(root):
    result = {}
    for path in sorted(root.rglob('*')):
        if path.is_file():
            stat = path.stat()
            result[path.relative_to(root).as_posix()] = dict(bytes=stat.st_size, mtimeNs=stat.st_mtime_ns, sha256=sha(path))
    check(result, f'Empty/missing protected store: {root}')
    return result


def inputs(networks):
    brn = networks / 'BRN/BRN-2'
    protected = dict(canonical=brn / 'training001', nnue=networks / 'NNUE/training',
                     ablation=brn / 'experimental-supervision-ablation-20260923',
                     sweep=brn / 'experimental-supervision-ablation-weight-sweep-20260923')
    identities = {}
    for actor, (relative, generation, model_hash, training_hash) in PINS.items():
        folder = protected['nnue'] / 'checkpoints' / relative if actor == 'nnue74' else brn / relative / 'boundary-128'
        model = folder / ('network.nnue' if actor == 'nnue74' else 'network.brn2')
        check(sha(model) == model_hash, f'Model mismatch: {actor}')
        check(sha(folder / 'training.state') == training_hash, f'Training state mismatch: {actor}')
        metadata = folder / ('manifest.bin' if actor == 'nnue74' else 'experiment.json')
        if actor != 'nnue74':
            experiment = json.loads(metadata.read_text())
            check(experiment['boundary'] == 128 and experiment['bestGeneration'] == generation
                  and experiment['modelSha256'] == model_hash and experiment['trainingSha256'] == training_hash,
                  f'Export metadata mismatch: {actor}')
            check((folder.parent / 'best.txt').read_text().splitlines() ==
                  ['boundary-128/network.brn2', f'Best generation {generation}'], f'Best reference mismatch: {actor}')
            replay = folder.parent.parent / 'replay.jsonl'
            check(any(r.get('type') == 'milestone' and r.get('snapshot') == experiment for r in records(replay)),
                  f'Export is not a recorded accepted milestone: {actor}')
        identities[actor] = dict(path=str(folder if actor == 'nnue74' else model), generation=generation,
                                 modelSha256=model_hash, trainingSha256=training_hash,
                                 metadataPath=str(metadata), metadataSha256=sha(metadata))
    # Existing managed readers coordinate through this lock; prohibit creating it in this experiment.
    check((protected['nnue'] / 'payload.lock').is_file(), 'Missing NNUE read-coordination lock; cannot mutate store')
    return protected, identities


def run(stage, output, timeout):
    target = output / f'{stage}.jsonl'
    check(not target.exists(), f'Output already exists: {target}')
    cp = os.pathsep.join(str(ROOT / p) for p in ('app/build/classes/java/main', 'app/build/resources/main', 'app/build/install/seedv6/lib/*'))
    command = ['java', '-Xms256m', '-Xmx1536m', '-cp', cp,
               'com.ohinteractive.seedv6.tools.search.Brn2StrengthScreen', stage, str(output / 'config.properties'), str(target)]
    receipt = dict(command=command, watchdogSeconds=timeout, stalledOutputSeconds=120, startedUtc=time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime()))
    start = time.monotonic()
    try:
        with (output / f'{stage}.log').open('x', encoding='utf-8') as log:
            process = subprocess.Popen(command, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT)
            while process.poll() is None:
                time.sleep(1)
                stale = time.time() - target.stat().st_mtime if target.exists() else time.monotonic() - start
                if time.monotonic() - start > timeout or stale > 120:
                    process.kill()
                    process.wait()
                    raise TimeoutError(f'{stage}: hard process/stall watchdog fired')
            receipt['exit'] = process.returncode
            check(process.returncode == 0, f'{stage} failed; preserved {stage}.log and JSONL')
        rows = list(records(target)) if stage != 'screen' else None
        if rows is not None:
            check(rows[-1]['type'] == 'end' and rows[-1]['completed'], 'Incomplete output')
            ids = {r['identity']['actor']: r['identity'] for r in rows if r['type'] == 'identity'}
            check(set(ids) == set(PINS), 'Not all actors loaded')
            for actor, pin in PINS.items():
                check(ids[actor]['modelSha256'] == pin[2], 'Loaded identity mismatch')
            check(ids['nnue74']['generation'] == 74 and ids['nnue74']['optimizerStep'] == 8942
                  and ids['nnue74']['trainingSha256'] == PINS['nnue74'][3]
                  and ids['nnue74']['checkpointId'] == PIN, 'NNUE manifest identity mismatch')
        receipt['completed'] = True
    except Exception as error:
        receipt.update(completed=False, error=str(error))
        raise
    finally:
        receipt['elapsedSeconds'] = time.monotonic() - start
        save(output / f'{stage}-process.json', receipt)
        print(stage, {k: v for k, v in receipt.items() if k != 'command'}, flush=True)


def analyze(output):
    games, moves, pairs, summaries, openings = {}, collections.defaultdict(list), collections.defaultdict(list), {}, {}
    last = None
    for r in records(output / 'screen.jsonl'):
        last = r
        if r['type'] == 'game':
            check(r['game'] not in games, 'Duplicate game')
            games[r['game']] = r
        elif r['type'] == 'move':
            check(r['ply'] == len(moves[r['game']]), 'Move sequence attribution mismatch')
            check(r['nodes'] == r['mainNodes'] + r['qNodes'], 'Node accounting mismatch')
            moves[r['game']].append(r)
        elif r['type'] == 'pair':
            pairs[r['match']].append(r)
        elif r['type'] == 'summary':
            summaries[r['match']] = r
        elif r['type'] == 'opening':
            h = hashlib.sha256()
            for number in r['board'] + [len(r['history'])] + r['history']:
                h.update(struct.pack('>q', number))
            check(h.hexdigest() == r['hash'], 'Opening state hash mismatch')
            openings[r['index']] = r
    check(last == dict(type='end', completed=True, finishedUtc=last.get('finishedUtc')), 'Screen did not finish')
    check(len(games) == 768 and len(openings) == 64 and len(summaries) == 6, 'Incomplete four-actor screen')
    check(len({o['hash'] for o in openings.values()}) == 64, 'Duplicate opening identities')
    report = dict(games=len(games), openings=len(openings), matches={}, actors={})

    def search_stats(population):
        return dict(searches=len(population), nodes=sum(m['nodes'] for m in population),
                    mainNodes=sum(m['mainNodes'] for m in population), qNodes=sum(m['qNodes'] for m in population),
                    elapsedSeconds=sum(m['elapsedNs'] for m in population) / 1e9,
                    medianMillis=statistics.median(m['elapsedNs'] for m in population) / 1e6,
                    maxMillis=max(m['elapsedNs'] for m in population) / 1e6,
                    medianNodes=statistics.median(m['nodes'] for m in population),
                    medianDepth=statistics.median(m['completedDepth'] for m in population),
                    depths=dict(sorted(collections.Counter(m['completedDepth'] for m in population).items())),
                    timeLimit=sum(m['termination'] == 'TIME_LIMIT' for m in population),
                    failures=sum(m['failure'] is not None or m['termination'] not in ('TIME_LIMIT', 'COMPLETED') for m in population),
                    fallback=sum(m['fallback'] for m in population),
                    fallbackGames=sorted({m['game'] for m in population if m['fallback']}),
                    fallbackMoves=[m for m in population if m['fallback']],
                    over100ms=sum(m['elapsedNs'] > 100_000_000 for m in population),
                    worst=sorted(population, key=lambda m: m['elapsedNs'], reverse=True)[:5])

    for name, summary in summaries.items():
        a, b = name.split('-')
        subset = [g for g in games.values() if g['match'] == name]
        check(len(subset) == 128 and len(pairs[name]) == 64, 'Incomplete pairing')
        wdl = [0, 0, 0]
        colours = dict(white=[0, 0, 0], black=[0, 0, 0])
        for game in subset:
            check(game['openingHash'] == openings[game['opening']]['hash'], 'Opening pairing mismatch')
            check(game['plies'] == len(moves[game['game']]) and game['error'] is None, 'Game accounting/failure')
            check({game['white'], game['black']} == {a, b}, 'Actor mismatch')
            for i, move in enumerate(moves[game['game']]):
                check(move['actor'] == game['white' if i % 2 == 0 else 'black'], 'Colour attribution mismatch')
            term = game['termination']
            check(term not in ('PLY_CAP', 'SEARCH_FAILURE', 'CANCELLED', 'INFRASTRUCTURE_FAILURE'), 'Invalid game')
            white_score = 1 if term == 'WHITE_CHECKMATES_BLACK' else 0 if term == 'BLACK_CHECKMATES_WHITE' else .5
            score = white_score if game['white'] == a else 1 - white_score
            column = 0 if score == 1 else 1 if score == .5 else 2
            wdl[column] += 1
            colours['white' if game['white'] == a else 'black'][column] += 1
        check(wdl == [summary[k] for k in ('wins', 'draws', 'losses')], 'Independent WDL mismatch')
        for colour in colours:
            check(colours[colour] == [summary[colour][k] for k in ('wins', 'draws', 'losses')], 'Colour split mismatch')
        check(sorted(p['opening'] for p in pairs[name]) == list(range(64)), 'Pair indexing mismatch')
        for pair in pairs[name]:
            white = games[f'{name}/{pair["opening"]}/A-white']
            black = games[f'{name}/{pair["opening"]}/A-black']
            check(white['white'] == black['black'] == a and white['black'] == black['white'] == b,
                  'Pair colours were not swapped')
            check(white['openingHash'] == black['openingHash'] == pair['openingHash'], 'Pair states differ')
            check(pair['valid'] and pair['aWhite'] == white['termination'] and pair['aBlack'] == black['termination'],
                  'Pair outcome attribution mismatch')
            def white_score(game):
                return 1 if game['termination'] == 'WHITE_CHECKMATES_BLACK' else 0 if game['termination'] == 'BLACK_CHECKMATES_WHITE' else .5
            check(pair['score'] == (white_score(white) + 1 - white_score(black)) / 2, 'Pair score mismatch')
        mean = (wdl[0] + .5 * wdl[1]) / 128
        check(mean == summary['score'] == sum(p['score'] for p in pairs[name]) / 64, 'Score mismatch')
        check(abs(summary['confidence']['radius'] - math.sqrt(math.log(40) / 128)) < 1e-14, 'CI formula mismatch')
        lengths = [g['plies'] for g in subset]
        summary.update(meanPlies=statistics.mean(lengths), medianPlies=statistics.median(lengths), maxPlies=max(lengths),
                       pairScoreHistogram=dict(sorted(collections.Counter(str(p['score']) for p in pairs[name]).items())),
                       search=search_stats([m for g in subset for m in moves[g['game']]]))
        report['matches'][name] = summary
    for actor in PINS:
        report['actors'][actor] = search_stats([m for population in moves.values() for m in population if m['actor'] == actor])
    save(output / 'analysis.json', report)
    save(output / 'artifact-sha256.json', {p.name: sha(p) for p in sorted(output.iterdir()) if p.is_file() and p.name != 'artifact-sha256.json'})
    print(json.dumps({k: {key: v[key] for key in ('wins', 'draws', 'losses', 'score', 'confidence')} for k, v in report['matches'].items()}, indent=2))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('stage', choices=('smoke', 'screen', 'analyze'))
    parser.add_argument('output', type=Path)
    parser.add_argument('--networks', type=Path, default=Path('E:/SeedV6-Networks'))
    args = parser.parse_args()
    output = args.output.resolve()
    check(output.is_relative_to(ROOT / 'app/build'), 'Evidence output must be under app/build')
    if args.stage == 'analyze':
        analyze(output)
        return
    protected, identities = inputs(args.networks.resolve())
    for path in protected.values():
        check(not output.is_relative_to(path) and not path.is_relative_to(output), 'Output overlaps protected store')
    if args.stage == 'smoke':
        output.mkdir(parents=True, exist_ok=False)
        save(output / 'identities.json', identities)
        save(output / 'source.json', dict(head=subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip(),
                                         status=subprocess.check_output(['git', 'status', '--short'], cwd=ROOT, text=True)))
        lines = ['pairs=64', 'seed=20260923', 'openingPlies=8', 'moveMillis=50', 'maximumPlies=1024']
        for actor, identity in identities.items():
            lines += [f'{actor}.path={Path(identity["path"]).as_posix()}', f'{actor}.sha256={identity["modelSha256"]}']
        (output / 'config.properties').write_text('\n'.join(lines) + '\n', encoding='utf-8')
        save(output / 'config-sha256.json', dict(sha256=sha(output / 'config.properties')))
        before = {name: fingerprint(path) for name, path in protected.items()}
        save(output / 'inputs-before.json', before)
    else:
        check(json.loads((output / 'smoke-process.json').read_text())['completed'], 'Smoke did not pass')
        check(json.loads((output / 'identities.json').read_text()) == identities, 'Identities changed after smoke')
        before = json.loads((output / 'inputs-before.json').read_text())
        check(before == {name: fingerprint(path) for name, path in protected.items()}, 'Protected stores changed after smoke')
    check(sha(output / 'config.properties') == json.loads((output / 'config-sha256.json').read_text())['sha256'], 'Configuration changed')
    try:
        if args.stage == 'smoke':
            run('verify', output, 120)
            run('smoke', output, 240)
        else:
            run('screen', output, 45000)
            analyze(output)
    finally:
        after = {name: fingerprint(path) for name, path in protected.items()}
        save(output / f'inputs-after-{args.stage}.json', after)
        check(before == after, 'Protected source inventory/content/mtime changed; STOP')
        save(output / f'integrity-{args.stage}.json', dict(unchanged=True, fileCounts={k: len(v) for k, v in after.items()}))


if __name__ == '__main__':
    main()
