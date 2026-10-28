"""Restartable nested million-position sample; SQLite commits are selection checkpoints."""
import argparse
import json
import math
from pathlib import Path
import shutil
import sqlite3
import time

import chess
from baseline import ROOT, admit, sha
from compute_control import atomic
from prepare_objective import alias, unpack

TARGET = 1048576
CHUNK = 4096
SEED = 2026102903
MULTIPLIER = 7919


def database(path, binding):
    db = sqlite3.connect(path)
    db.execute('PRAGMA journal_mode=WAL')
    db.execute('PRAGMA synchronous=FULL')
    db.execute('PRAGMA cache_size=-8192')
    db.execute('CREATE TABLE IF NOT EXISTS meta (key TEXT PRIMARY KEY, value TEXT NOT NULL)')
    db.execute('CREATE TABLE IF NOT EXISTS rows (alias TEXT PRIMARY KEY, ordinal INTEGER UNIQUE, source_id INTEGER UNIQUE, line TEXT)')
    old = db.execute("SELECT value FROM meta WHERE key='binding'").fetchone()
    encoded = json.dumps(binding, sort_keys=True)
    if old is not None and old[0] != encoded:
        db.close()
        raise ValueError('Selection binding changed')
    if old is None:
        db.execute('INSERT INTO meta VALUES (?,?)', ('binding', encoded))
        for key in ('prefix', 'cursor', 'count'):
            db.execute('INSERT INTO meta VALUES (?,?)', (key, '0'))
        db.commit()
    return db


def number(db, key):
    return int(db.execute('SELECT value FROM meta WHERE key=?', (key,)).fetchone()[0])


def set_number(db, key, value):
    db.execute('UPDATE meta SET value=? WHERE key=?', (str(value), key))


def commit_candidates(db, records, cursor, limit):
    """Commit accepted rows and source cursor together, including rejected duplicates."""
    with db:
        count = number(db, 'count')
        for source_id, key, line in records:
            if count >= limit:
                break
            accepted = db.execute('INSERT OR IGNORE INTO rows VALUES (?,?,?,?)',
                                  (key, count, source_id, line)).rowcount
            count += accepted
        set_number(db, 'count', count)
        set_number(db, 'cursor', cursor)


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('output', type=Path)
    p.add_argument('--seconds', type=int, default=240)
    args = p.parse_args()
    start = time.monotonic()
    samples = [admit(initial=True)]
    args.output.mkdir(parents=True, exist_ok=True)
    prior = ROOT / 'docs/research/brn-relational-cglhw/raw/r002-coverage-data'
    manifest = json.loads((prior / 'manifest.json').read_text())
    for name in ('train.tsv', 'validation.tsv', 'selected-indices.json'):
        if sha(prior / name) != manifest['files'][name]:
            raise ValueError('Prior subset identity: ' + name)
    binary = Path(manifest['source'])
    if sha(binary) != manifest['source_sha256']:
        raise ValueError('Binary source identity')
    size = binary.stat().st_size // 32
    if binary.stat().st_size % 32 or math.gcd(MULTIPLIER, size) != 1:
        raise ValueError('Permutation requires integral records and coprime step')
    binding = dict(source_sha256=manifest['source_sha256'], prior_manifest_sha256=sha(prior / 'manifest.json'),
                   script_sha256=sha(Path(__file__)), target=TARGET, chunk=CHUNK, seed=SEED, multiplier=MULTIPLIER)
    done = args.output / 'manifest.json'
    if done.exists():
        result = json.loads(done.read_text())
        if result['binding'] != binding or any(sha(args.output / n) != h for n, h in result['files'].items()):
            raise ValueError('Completed selection identity')
        print(json.dumps(dict(status='verified-complete', manifest_sha256=sha(done))))
        return
    db = database(args.output / 'selection.sqlite', binding)
    status = 'CHECKPOINTED'
    error = None
    try:
        # Validation reservations are idempotent and cannot become training rows.
        with db:
            for line in (prior / 'validation.tsv').read_text().splitlines():
                db.execute('INSERT OR IGNORE INTO rows VALUES (?,NULL,NULL,NULL)',
                           (alias(chess.Board(line.split('\t')[0])),))
        ids = json.loads((prior / 'selected-indices.json').read_text())
        prefix = (prior / 'train.tsv').read_text().splitlines()
        while number(db, 'prefix') < len(prefix) and time.monotonic() - start < args.seconds:
            samples.append(admit())
            first = number(db, 'prefix')
            last = min(first + CHUNK, len(prefix))
            with db:
                for index in range(first, last):
                    key = alias(chess.Board(prefix[index].split('\t')[0]))
                    db.execute('INSERT INTO rows VALUES (?,?,?,?)', (key, index, ids[index], prefix[index]))
                set_number(db, 'prefix', last)
                set_number(db, 'count', last)
        del ids, prefix
        with binary.open('rb') as stream:
            while number(db, 'prefix') == manifest['train'] and number(db, 'count') < TARGET and time.monotonic() - start < args.seconds:
                samples.append(admit())
                cursor = number(db, 'cursor')
                last = min(cursor + CHUNK, size)
                if last == cursor:
                    raise ValueError('Insufficient unique positions')
                records = []
                for index in range(cursor, last):
                    source_id = (MULTIPLIER * index + SEED) % size
                    stream.seek(source_id * 32)
                    board, target = unpack(stream.read(32))
                    records.append((source_id, alias(board), f'{board.fen()}\t{target:.17g}'))
                commit_candidates(db, records, last, TARGET)
        if number(db, 'count') == TARGET:
            samples.append(admit())
            # Interrupted publication repeats serialization, never selection/training.
            temporary = args.output / 'train.tsv.part'
            with temporary.open('w', newline='') as output:
                for row in db.execute('SELECT line FROM rows WHERE ordinal IS NOT NULL ORDER BY ordinal'):
                    output.write(row[0] + '\n')
            temporary.replace(args.output / 'train.tsv')
            temporary = args.output / 'selected-indices.json.part'
            with temporary.open('w', newline='') as output:
                output.write('[')
                for index, row in enumerate(db.execute('SELECT source_id FROM rows WHERE ordinal IS NOT NULL ORDER BY ordinal')):
                    output.write((',' if index else '') + str(row[0]))
                output.write(']\n')
            temporary.replace(args.output / 'selected-indices.json')
            shutil.copyfile(prior / 'validation.tsv', args.output / 'validation.tsv')
            result = dict(schema='brn-relational-scale-sample-v1', binding=binding, train=TARGET,
                          validation=4096, original_train_prefix=manifest['train'], source=str(binary),
                          selection_cursor=number(db, 'cursor'),
                          files={n: sha(args.output / n) for n in ('train.tsv', 'validation.tsv', 'selected-indices.json')},
                          limitations='Consumed development panel; unknown game identity; placement/color/rank/file aliases purged; affine permutation is deterministic, not iid random sampling.')
            atomic(done, result)
            status = 'COMPLETE'
    except Exception as exc:
        error = repr(exc)
        raise
    finally:
        receipt = dict(status=status if error is None else 'STOPPED', error=error,
                       prefix=number(db, 'prefix'), train=number(db, 'count'), cursor=number(db, 'cursor'),
                       seconds=time.monotonic()-start, resources=samples)
        db.close()
        atomic(args.output / ('execution-' + str(time.time_ns()) + '.json'), receipt)
        print(json.dumps({k: v for k, v in receipt.items() if k != 'resources'}))


if __name__ == '__main__':
    main()
