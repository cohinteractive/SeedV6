"""SeedV6's non-critical version boundary around the pinned shared implementation.

Shared parsing, attribution, atomic replacement, locking and success receipts are
unchanged. Only failed version attempts become terminal warnings. No app, index,
refs, cache, hook or journal writes are performed by this adapter.
"""
import argparse
import importlib
import json
import os
from pathlib import Path
import sys

SHARED = Path('C:/projects/codebase-entropy-control/scripts/codex-versioning')
PINS = {
    'finalizer.py': 'd09d1a0bed42cb12b00666dbc1b561c3042b602f8cb403afd6bacb5868bdc907',
    'recovery.py': '9cecc02bcef1c7b882bb367c6812cfe3155181994c7eff76ed5f55bb543bf9d4',
}
POLICY = 'seedv6-noncritical-version/1'


def shared():
    import hashlib
    for name, digest in PINS.items():
        if hashlib.sha256((SHARED / name).read_bytes()).hexdigest() != digest:
            raise RuntimeError('Shared version implementation binding changed: ' + name)
    sys.path.insert(0, str(SHARED))
    module = importlib.import_module('finalizer')
    if Path(module.__file__).resolve() != (SHARED / 'finalizer.py').resolve():
        raise RuntimeError('Unexpected finalizer module binding.')
    return module


f = shared()


class IntegrityFailure(f.Blocked):
    """Failures outside optional version bookkeeping remain blocking."""


def critical(condition, message):
    if not condition:
        raise IntegrityFailure(message)


def save(path, value):
    try:
        f.atomic_json(path, value)
    except Exception as error:
        raise IntegrityFailure('Cannot persist finalizer metadata: ' + str(error)) from error


def capture(root):
    try:
        return f.stable_snapshot(root)
    except f.Blocked as error:
        # Cache writers can make this optional version check unstable. All other
        # inspection failures (Git, unsupported trees, etc.) retain their severity.
        if str(error) == 'Repository changed during capture; attribution is unavailable.':
            raise
        raise IntegrityFailure(str(error)) from error
    except Exception as error:
        raise IntegrityFailure('Cannot inspect repository: ' + str(error)) from error


def require_environmental(root, paths):
    """Unexpected source/index effects are not optional version failures."""
    try:
        for name in paths:
            critical(isinstance(name, str) and not Path(name).is_absolute()
                     and '..' not in Path(name).parts, 'Invalid write-boundary evidence path.')
            critical(not f.git(root, 'ls-files', '--', name)
                     and bool(f.git(root, 'check-ignore', '--', name, allow_missing=True)),
                     'Non-version change is not ignored environmental content: ' + name)
    except IntegrityFailure:
        raise
    except Exception as error:
        raise IntegrityFailure('Cannot verify non-version changes: ' + str(error)) from error


def warning_path(location, token):
    f.operation(location, token)  # Validate the token before using it as a path.
    return location / 'warnings' / token


def warning_receipt(root, folder):
    receipt = f.read_json(folder / 'warning.json')
    result = receipt['result']
    critical(receipt['policy'] == POLICY and receipt['identity'] == f.repository(root)
             and receipt['token'] == folder.name
             and result['token'] == folder.name and result['status'] == 'warning'
             and result['verified_bump'] is False and result['after'] is None
             and not result['blockers'] and bool(result['warnings'])
             and f.sha(f.encoded(result)) == receipt['result_sha256'],
             'Invalid terminal version warning receipt.')
    for name in ('baseline.json', 'state.json'):
        critical(f.sha((folder / name).read_bytes()) == receipt['preserved_sha256'][name],
                 'Preserved version history integrity failure: ' + name)
    return result


def release(location, token):
    active = location / 'active.json'
    if active.exists() and f.read_json(active) == {'token': token}:
        active.unlink()  # Never release another operation, including on old retries.


def archive_warning(root, location, token, result):
    """Prepare receipt, atomically archive owned history, then release reservation.

    Every interruption point is resumable. Original baseline/state bytes are kept;
    the shared scanner never sees a pending operation after the archive rename.
    Must be called with the shared OS command lock held.
    """
    source = f.operation(location, token)
    target = warning_path(location, token)
    critical(source.resolve().is_relative_to(location.resolve())
             and target.resolve().is_relative_to(location.resolve()),
             'Version history path escapes the resolved metadata directory.')
    if target.exists():
        critical(not source.exists(), 'Duplicate live/archived version operation.')
        stored = warning_receipt(root, target)
    else:
        f.load_operation(root, location, token)  # Ownership and immutable integrity.
        if not (source / 'warning.json').exists():
            save(source / 'warning.json', {
                'policy': POLICY, 'identity': f.repository(root), 'token': token,
                'result': result, 'result_sha256': f.sha(f.encoded(result)),
                'preserved_sha256': {name: f.sha((source / name).read_bytes())
                                     for name in ('baseline.json', 'state.json')},
            })
        stored = warning_receipt(root, source)
        target.parent.mkdir(parents=True, exist_ok=True)
        os.rename(source, target)
    release(location, token)
    return stored


def warning(token, baseline, decision, message, **extra):
    return f.result_fields(token, status='warning', decision=decision,
                           before=baseline['version'], warnings=[message], **extra)


def reconcile_pending(root, location):
    """Resume only this adapter's interrupted finish/cleanup, never an active root."""
    active = location / 'active.json'
    if active.exists():
        token = f.read_json(active).get('token')
        target = warning_path(location, token)
        if target.exists():
            archive_warning(root, location, token, None)
    for folder in sorted((location / 'operations').glob('*')):
        if (folder / 'warning.json').exists():
            archive_warning(root, location, folder.name, None)
            continue
        state, baseline = f.load_operation(root, location, folder.name)
        if state['phase'] == 'begin_pending':
            critical(state['mutation'] == 'not_attempted'
                     and baseline['finalizer_sha256'] == PINS['finalizer.py']
                     and active.exists() and f.read_json(active) == {'token': folder.name},
                     'Interrupted begin does not own a supported reservation.')
            archive_warning(root, location, folder.name, warning(
                folder.name, baseline, None, 'Interrupted version baseline capture; no update attempted.'))
            continue
        attempt = state.get('seedv6_attempt')
        if attempt is None or state['phase'] == 'completed':
            continue
        critical(attempt.get('policy') == POLICY and attempt.get('decision') in ('bump', 'no_bump'),
                 'Invalid SeedV6 version attempt marker.')
        critical(not state.get('integrity_failure'), state.get('integrity_failure', ''))
        critical(active.exists() and f.read_json(active) == {'token': folder.name},
                 'Interrupted version attempt does not own the reservation.')
        archive_warning(root, location, folder.name, warning(
            folder.name, baseline, attempt['decision'],
            'Interrupted version attempt; no verified update claimed and no retry bump attempted.'))


def begin(root):
    location = f.state_location(root)
    if not os.path.lexists(root / f.VERSION) and not location.exists():
        return f.begin(root)
    with f.command_lock(location):
        reconcile_pending(root, location)
        f.no_overlap(root, location)
        if os.path.lexists(root / f.VERSION):
            try:
                f.version_bytes(root)
            except Exception as error:
                return f.result_fields(status='warning', warnings=[type(error).__name__ + ': ' + str(error)])
    # The shared begin takes the same lock and rechecks overlaps itself.
    try:
        return f.begin(root)
    except f.Blocked as error:
        if str(error) not in ('Repository changed during capture; attribution is unavailable.',
                              'Repository changed during begin.', 'Version changed during begin.'):
            raise
        with f.command_lock(location):
            active = location / 'active.json'
            if not active.exists():
                return f.result_fields(status='warning', warnings=[str(error)])
            token = f.read_json(active)['token']
            state, baseline = f.load_operation(root, location, token)
            critical(state['phase'] == 'begin_pending' and state['mutation'] == 'not_attempted',
                     'Capture failure does not own an unfinished begin.')
            return archive_warning(root, location, token, warning(token, baseline, None, str(error)))


def attempt_version(root, folder, state, baseline, decision, evidence):
    """Only this boundary is fail-open; storage/ownership/Git integrity are not."""
    observed = capture(root)
    try:
        changes = f.attributable(baseline['snapshot'], observed)
    except Exception as error:
        raise IntegrityFailure('Invalid attribution evidence: ' + str(error)) from error
    critical('recovery_endpoint' not in baseline, 'Linked recovery requires the shared recovery protocol.')
    critical(decision != 'bump' or bool(changes),
             'Bump requires at least one attributable substantive repository change.')
    evidence.update(changes=changes, changed_paths=[c['path'] for c in changes])
    raw = f.version_bytes(root)
    version = f.validate_version(f.parse_json(raw.decode('utf-8-sig')))
    f.require(version == baseline['version'] and f.sha(raw) == baseline['version_sha256']
              and raw.hex() == baseline['version_hex']
              and observed['files'][f.VERSION] == baseline['snapshot']['files'][f.VERSION],
              'Expected-before tuple or protected authority bytes/mode mismatch.')
    f.require(capture(root) == observed, 'Repository changed before finalization.')
    if decision == 'no_bump':
        evidence['status'] = 'no_bump'
        return
    updated = f.increment_bytes(raw, version)
    state.update(phase='mutation_pending', mutation='uncertain', evidence=evidence)
    save(folder / 'state.json', state)
    f.replace_version(root, folder, raw, updated, observed['files'][f.VERSION]['mode'])
    post = capture(root)
    critical(all(post[k] == observed[k] for k in observed if k != 'files'),
             'Mutation changed repository identity, HEAD, index, refs or configuration.')
    touched = {p for p in set(observed['files']) | set(post['files'])
               if observed['files'].get(p) != post['files'].get(p)}
    require_environmental(root, touched - {f.VERSION})
    f.require(touched == {f.VERSION},
              'Mutation changed files outside VERSION_STATE.txt: ' + repr(sorted(touched)))
    actual_raw = f.version_bytes(root)
    after = f.validate_version(f.parse_json(actual_raw.decode('utf-8-sig')))
    f.require(actual_raw == updated and after == {**version, 'build': version['build'] + 1}
              and post['files'][f.VERSION]['sha256'] == f.sha(actual_raw)
              and post['files'][f.VERSION]['mode'] == observed['files'][f.VERSION]['mode'],
              'Independent version bytes/tuple/mode verification failed.')
    evidence.update(status='bumped', after=after, verified_bump=True)


def finish(root, token, decision):
    critical(decision in ('bump', 'no_bump'), 'Decision must be bump or no_bump.')
    location = f.state_location(root)
    with f.command_lock(location):
        reconcile_pending(root, location)
        archived = warning_path(location, token)
        if archived.exists():
            result = warning_receipt(root, archived)
            critical(result['decision'] is None or result['decision'] == decision,
                     'Retry decision contradicts terminal warning.')
            return {**result, 'replayed': True}
        state, baseline = f.load_operation(root, location, token)
        if state['phase'] == 'completed':
            critical(state['result']['decision'] == decision, 'Retry decision contradicts completed decision.')
            release(location, token)
            return {**state['result'], 'replayed': True}
        critical(state['phase'] == 'active' and state['mutation'] == 'not_attempted',
                 'Prior operation is incomplete; inspect ownership before reconciliation.')
        critical(f.read_json(location / 'active.json') == {'token': token}, 'Active reservation mismatch.')
        f.no_overlap(root, location, allowed=token)
        critical(baseline['finalizer_sha256'] == PINS['finalizer.py']
                 and baseline.get('implementation') == {
                     'finalizer_sha256': PINS['finalizer.py'], 'recovery_sha256': PINS['recovery.py']},
                 'Version implementation changed since begin.')
        folder = f.operation(location, token)
        evidence = f.result_fields(token, decision=decision, before=baseline['version'])
        state['seedv6_attempt'] = {'policy': POLICY, 'decision': decision}
        save(folder / 'state.json', state)
        failed = False
        try:
            attempt_version(root, folder, state, baseline, decision, evidence)
        except IntegrityFailure as error:
            state['integrity_failure'] = str(error)
            save(folder / 'state.json', state)
            raise
        except Exception as error:
            failed = True
            evidence.update(status='warning', after=None, verified_bump=False,
                            warnings=[type(error).__name__ + ': ' + str(error)], blockers=[])
        finally:
            if failed:
                # Cleanup is independent of the version exception and preserves
                # even uncertain/partially successful attempts without compensation.
                archive_warning(root, location, token, evidence)
        if failed:
            return evidence
        state.update(phase='completed', mutation='verified' if decision == 'bump' else 'not_attempted',
                     result=evidence, result_sha256=f.sha(f.encoded(evidence)))
        # Completion persistence is outside the optional build-update catch.
        save(folder / 'state.json', state)
        release(location, token)
        return evidence


def reconcile_legacy(root, token):
    """Explicitly retire only a verified legacy post-write boundary failure."""
    import ast
    location = f.state_location(root)
    with f.command_lock(location):
        reconcile_pending(root, location)
        if warning_path(location, token).exists():
            return {**warning_receipt(root, warning_path(location, token)), 'replayed': True}
        state, baseline = f.load_operation(root, location, token)
        critical(f.read_json(location / 'active.json') == {'token': token}, 'Legacy reservation mismatch.')
        f.no_overlap(root, location, allowed=token)
        prefix = 'Mutation changed files outside VERSION_STATE.txt: '
        critical(state['phase'] == 'blocked' and state['mutation'] == 'uncertain'
                 and state.get('error', '').startswith(prefix)
                 and state.get('evidence', {}).get('decision') == 'bump'
                 and baseline['finalizer_sha256'] == PINS['finalizer.py'],
                 'Legacy failure is not a recognized version verification failure.')
        touched = ast.literal_eval(state['error'][len(prefix):])
        critical(isinstance(touched, list) and f.VERSION in touched,
                 'Invalid legacy write-boundary evidence.')
        require_environmental(root, [name for name in touched if name != f.VERSION])
        return archive_warning(root, location, token, warning(
            token, baseline, 'bump', state['error'] +
            ' Historical failure released under SeedV6 non-critical version policy; no verified bump claimed.'))


def status(root, token=None):
    if token and warning_path(f.state_location(root), token).exists():
        return {**warning_receipt(root, warning_path(f.state_location(root), token)), 'phase': 'completed_warning'}
    return f.status(root, token)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--repo', default='.')
    commands = parser.add_subparsers(dest='command', required=True)
    commands.add_parser('begin')
    end = commands.add_parser('finish')
    end.add_argument('token')
    end.add_argument('--decision', choices=('bump', 'no_bump'), required=True)
    commands.add_parser('status').add_argument('token', nargs='?')
    commands.add_parser('reconcile-legacy').add_argument('token')
    args = parser.parse_args()
    try:
        root = Path(f.repository(Path(args.repo).resolve())['root'])
        result = (begin(root) if args.command == 'begin' else
                  finish(root, args.token, args.decision) if args.command == 'finish' else
                  reconcile_legacy(root, args.token) if args.command == 'reconcile-legacy' else
                  status(root, args.token))
    except Exception as error:
        result = f.result_fields(getattr(args, 'token', None), blockers=[type(error).__name__ + ': ' + str(error)])
    print(json.dumps(result, sort_keys=True))
    return 2 if result['status'] == 'blocked' else 0


if __name__ == '__main__':
    raise SystemExit(main())
