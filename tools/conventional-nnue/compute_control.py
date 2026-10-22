"""Persistent owner permission and bounded Windows compute supervision for this CGLHW.

No daemon. Commands are usable from another terminal while Codex or a workload is
busy. Operational state lives in this worktree's Git metadata, outside build cleanup.
"""
import argparse
from contextlib import contextmanager
from datetime import datetime, timezone
import hashlib
import json
import math
import msvcrt
import os
from pathlib import Path
import struct
import subprocess
import sys
import time
import uuid
import windows_job as windows

ROOT = Path(__file__).resolve().parents[2]
MODES = ('NORMAL', 'COMPUTE_HOLD', 'FULL_PAUSE')
FILES = ('quantised.bin', 'raw.bin', 'optimiser_state/weights.bin',
         'optimiser_state/momentum.bin', 'optimiser_state/velocity.bin')
PIN = '579b78b37a694cacd6e9ede4706386c15c34837c'


def utc(): return datetime.now(timezone.utc).isoformat(timespec='seconds')
def sha(path):
    with Path(path).open('rb') as stream: return hashlib.file_digest(stream, 'sha256').hexdigest()


def atomic(path, value):
    path = Path(path); path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_name(path.name + '.' + uuid.uuid4().hex + '.tmp')
    with temporary.open('x', encoding='utf-8', newline='\n') as stream:
        json.dump(value, stream, indent=2); stream.write('\n'); stream.flush(); os.fsync(stream.fileno())
    os.replace(temporary, path)


def default_directory():
    result = subprocess.run(['git', '-C', str(ROOT), 'rev-parse', '--path-format=absolute',
                             '--git-path', 'seed-conventional-nnue'], capture_output=True, text=True, check=True)
    return Path(result.stdout.strip())


class Control:
    def __init__(self, directory=None):
        self.directory = Path(directory).resolve() if directory else default_directory()
        self.path = self.directory / 'control.json'

    @contextmanager
    def lock(self):
        self.directory.mkdir(parents=True, exist_ok=True)
        with (self.directory / 'control.lock').open('a+b') as stream:
            if stream.tell() == 0: stream.write(b'0'); stream.flush()
            deadline = time.monotonic() + 5
            while True:
                stream.seek(0)
                try: msvcrt.locking(stream.fileno(), msvcrt.LK_NBLCK, 1); break
                except OSError:
                    if time.monotonic() >= deadline: raise RuntimeError('Compute control is busy; retry the command')
                    time.sleep(.05)
            try: yield
            finally:
                stream.seek(0); msvcrt.locking(stream.fileno(), msvcrt.LK_UNLCK, 1)

    def read(self):
        state = json.loads(self.path.read_text(encoding='utf-8'))
        if (state.get('schema') != 1 or state.get('repository') != str(ROOT)
            or state.get('mode') not in MODES or not isinstance(state.get('generation'), int)
            or 'active' not in state or 'expires_at' not in state):
            raise ValueError('Unknown/inconsistent compute state: expensive work is prohibited')
        if state['expires_at'] is not None and (not isinstance(state['expires_at'], (int, float))
            or not math.isfinite(state['expires_at'])):
            raise ValueError('Invalid compute window')
        if state['active'] is not None:
            a = state['active']
            if (not isinstance(a, dict) or a.get('job') != 'Local\\SeedNNUE-' + a.get('id', '')
                or not isinstance(a.get('controller'), dict) or 'stop_requested_at' not in a
                or not 1 <= a.get('grace_seconds', 0) <= 60):
                raise ValueError('Invalid operation ownership: inspect before recovery')
        return state

    def write(self, state): atomic(self.path, state)

    def reconcile(self, state):
        changed = False
        if state['mode'] == 'NORMAL' and state['expires_at'] is not None and time.time() >= state['expires_at']:
            state.update(mode='COMPUTE_HOLD', expires_at=None, reason='Authorized compute window expired', updated=utc())
            state['generation'] += 1; changed = True
        a = state['active']
        if a:
            if state['mode'] != 'NORMAL' and a['stop_requested_at'] is None:
                a['stop_requested_at'] = time.time(); changed = True
            owner = windows.identity(a['controller']['pid'])
            if owner != a['controller']:
                count = windows.inspect(a['job'])
                if count:
                    # Only this operation's named, non-breakaway Windows job is eligible.
                    windows.inspect(a['job'], stop=True)
                    raise RuntimeError('Orphaned owned job is stopping; repeat status to confirm release')
                a.update(result='interrupted-supervisor', ended=utc(), owned_processes=0,
                         recovery='Reverify latest checkpoint; preserve incomplete output; explicitly authorize normal')
                atomic(self.directory / 'operations' / a['id'] / 'result.json', a)
                state.update(active=None, last_operation=a, mode='COMPUTE_HOLD', expires_at=None,
                             reason='Supervisor disappeared; reconciliation requires explicit continuation', updated=utc())
                state['generation'] += 1; changed = True
        if changed: self.write(state)
        return state

    def status(self):
        with self.lock():
            if not self.path.exists():
                return dict(mode='UNKNOWN', expensive_allowed=False, lightweight_allowed=True,
                            control_file=str(self.path), reason='No permission; explicit normal/hold/pause is required')
            state = self.reconcile(self.read())
            a = state['active']; count = windows.inspect(a['job']) if a else 0
            return dict(state, control_file=str(self.path), owned_processes=count,
                        expensive_allowed=state['mode'] == 'NORMAL' and a is None,
                        lightweight_allowed=state['mode'] != 'FULL_PAUSE',
                        hold_complete=state['mode'] != 'NORMAL' and a is None and count == 0)

    def request(self, mode, minutes=None, now=False):
        with self.lock():
            if self.path.exists(): state = self.reconcile(self.read())
            else:
                state = dict(schema=1, repository=str(ROOT), mode=mode, generation=0, expires_at=None,
                             active=None, last_operation=None, latest_checkpoint=None)
            state.update(mode=mode, expires_at=time.time() + minutes * 60 if minutes else None,
                         reason='Explicit owner control', updated=utc())
            state['generation'] += 1
            if mode != 'NORMAL' and state['active']:
                a = state['active']
                if a['stop_requested_at'] is None: a['stop_requested_at'] = time.time()
                a['emergency'] = a['emergency'] or now
            self.write(state)  # persist request before attempting any stop
            if now and state['active']: windows.inspect(state['active']['job'], stop=True)
        return self.status()


class Supervisor:
    def __init__(self, control, name, resume_note, grace=30):
        self.control = control; self.id = uuid.uuid4().hex; self.job = None; self.finalized = False
        self.directory = control.directory / 'operations' / self.id
        self.name = name; self.resume_note = resume_note; self.grace = grace
        self.result = 'failed'; self.stopped = False

    def __enter__(self):
        with self.control.lock():
            state = self.control.reconcile(self.control.read())
            if state['mode'] != 'NORMAL' or state['active']:
                raise RuntimeError('Expensive launch denied: permission held or another operation is active')
            self.job = windows.Job('Local\\SeedNNUE-' + self.id)
            a = dict(id=self.id, job='Local\\SeedNNUE-' + self.id, name=self.name,
                     controller=windows.identity(os.getpid()), started=utc(), stop_requested_at=None,
                     emergency=False, grace_seconds=self.grace, resume_note=self.resume_note,
                     command=None, checkpoint=None, child_pid=None)
            state['active'] = a
            try:
                atomic(self.directory / 'operation.json', a); self.control.write(state)
            except BaseException:
                self.job.close(); raise
        return self

    def permission(self, state):
        a = state['active']
        if not a or a['id'] != self.id: raise RuntimeError('Operation lease changed unexpectedly')
        if state['mode'] != 'NORMAL' or a['stop_requested_at'] is not None:
            self.stopped = True
        return not self.stopped

    def allowed(self):
        with self.control.lock():
            return self.permission(self.control.reconcile(self.control.read()))

    def describe_training(self, config, output):
        with self.control.lock():
            state = self.control.reconcile(self.control.read()); self.permission(state)
            state['active']['training_plan'] = dict(config=str(config), output=str(output), config_sha256=sha(config))
            self.control.write(state)

    def run(self, command, log):
        with self.control.lock():
            state = self.control.reconcile(self.control.read())
            if not self.permission(state): return 75
            environment = dict(os.environ, SEED_NNUE_CONTROL=str(self.control.path), SEED_NNUE_OPERATION=self.id)
            pid = self.job.start(command, log, ROOT, environment)
            state['active'].update(command=command, log=str(Path(log).resolve()), child_pid=pid, child_started=utc())
            self.control.write(state)
        print(json.dumps(dict(operation=self.id, child_pid=pid, log=str(log))), flush=True)
        killed = False
        while True:
            code = self.job.poll()
            if code is not None:
                # A command can exit while leaving descendants. Never let those leak.
                if self.job.count(): self.job.stop()
                deadline = time.monotonic() + 10
                while self.job.count():
                    if time.monotonic() > deadline: raise RuntimeError('Owned processes have not terminated')
                    time.sleep(.05)
                with self.control.lock():
                    state = self.control.reconcile(self.control.read()); self.permission(state)
                    state['active'].update(child_pid=None, child_exit_code=code, owned_processes=0)
                    if killed: state['active']['forced_stop'] = True
                    self.control.write(state)
                return code
            with self.control.lock():
                state = self.control.reconcile(self.control.read()); self.permission(state)
                a = state['active']
                if self.stopped and (a['emergency'] or time.time() - a['stop_requested_at'] >= self.grace):
                    if not killed:
                        self.job.stop(); killed = True
                        a['forced_stop'] = True; a['forced_stop_at'] = utc(); self.control.write(state)
            time.sleep(.2)

    def checkpoint(self, receipt):
        with self.control.lock():
            state = self.control.reconcile(self.control.read()); self.permission(state)
            state['active']['checkpoint'] = receipt; state['latest_checkpoint'] = receipt
            self.control.write(state)

    def __exit__(self, kind, error, traceback):
        try:
            self.job.stop()
            deadline = time.monotonic() + 10
            while self.job.count():
                if time.monotonic() > deadline: raise RuntimeError('Cleanup pending: owned job remains active')
                time.sleep(.05)
            with self.control.lock():
                state = self.control.reconcile(self.control.read())
                a = state['active']
                if not a or a['id'] != self.id: raise RuntimeError('Operation ownership changed during cleanup')
                a.update(result='failed' if kind else 'held' if self.stopped else self.result,
                         ended=utc(), owned_processes=0, error=str(error) if error else None)
                atomic(self.directory / 'result.json', a)
                state.update(active=None, last_operation=a); self.control.write(state)
                self.finalized = True
        finally:
            self.job.close()


def verify_checkpoint(path):
    """Reject incomplete receipt, changed config, bad hashes, or unreadable AdamW state."""
    path = Path(path).resolve()
    receipt = json.loads((path / 'seed-manifest.json').read_text())
    step = int(path.name.removeprefix('seed-cnnue-v1-'))
    config_path = path.parent / 'config.json'
    config = json.loads(config_path.read_text())
    if (receipt['schema'] != 'seed-bullet-checkpoint-v1' or receipt['bullet'] != PIN
        or receipt['completed_superbatch'] != step or receipt['config_sha256'] != sha(config_path)
        or set(receipt['files']) != set(FILES)):
        raise ValueError('Checkpoint receipt/configuration mismatch')
    for name in FILES:
        if not 0 < (path / name).stat().st_size <= 16*1024*1024:
            raise ValueError('Checkpoint payload outside bounded architecture size')
        if sha(path / name) != receipt['files'][name]: raise ValueError('Checkpoint checksum mismatch: ' + name)
    shapes = {'l0w': 7680*256, 'l0f': 768*256, 'l0b': 256, 'l1w': 512, 'l1b': 1}
    for name in FILES[2:]:
        data = (path / name).read_bytes(); offset = 0; records = {}
        while offset < len(data):
            end = data.index(b'\n', offset); key = data[offset:end].decode('ascii')
            count, = struct.unpack_from('<Q', data, end + 1); offset = end + 9 + count * 4
            if key in records or offset > len(data): raise ValueError('Invalid optimizer record')
            records[key] = count
        if records != shapes: raise ValueError('Incomplete optimizer tensors')
    quant = (path / 'quantised.bin').read_bytes(); payload = 1966849 * 2
    if (len(quant) != (payload+63)//64*64 or quant[payload:] != (b'bullet'*11)[:len(quant)-payload]
        or (path / 'raw.bin').stat().st_size != payload * 2):
        raise ValueError('Checkpoint network geometry/padding mismatch')
    return dict(path=str(path), completed_superbatch=step, next_superbatch=step+1,
                receipt_sha256=sha(path / 'seed-manifest.json'), config_sha256=sha(config_path), verified_at=utc()), config


def recipe(config):
    return tuple(config[key] for key in ('data_sha256', 'probes_sha256', 'seed', 'batch_size',
        'batches_per_superbatch', 'learning_rate', 'wdl'))


def initial_resume_compatible(request, previous):
    """Only a prospectively labelled data fork may change dataset/probe identity.

    Optimizer/schedule and model remain fixed. Subsequent segments still require
    full recipe equality, so a declared initial fork cannot mask later mutation.
    """
    current, prior = recipe(request), recipe(previous)
    reason = request.get('data_fork_reason')
    return current == prior or (current[2:] == prior[2:] and isinstance(reason, str) and len(reason.strip()) >= 12)


def train(control, args):
    source = Path(args.config).resolve(); requested_bytes = source.read_bytes()
    request = json.loads(requested_bytes); output = Path(request['output']).resolve()
    if request['batches_per_superbatch'] > 1024:
        raise ValueError('Unmeasured interruption boundary: at most 1024 batches per supervised superbatch')
    if request['start_superbatch'] < 1 or request['end_superbatch'] < request['start_superbatch']:
        raise ValueError('Invalid training interval')
    # Acquire permission before creating any run artifacts or hashing a large dataset.
    with Supervisor(control, args.name or 'Bullet training', 'Rerun the same train CONFIG; completed segments are verified and skipped', args.grace_seconds) as supervisor:
        supervisor.describe_training(source, output)
        plan = output / 'requested-config.json'
        if output.exists():
            if not plan.exists() or plan.read_bytes() != requested_bytes:
                raise ValueError('Output already exists without this exact immutable training plan')
        else:
            output.mkdir(parents=True)
            with plan.open('xb') as stream:
                stream.write(requested_bytes); stream.flush(); os.fsync(stream.fileno())
            atomic(output / 'segmentation.json', dict(schema=1, created=utc(), bullet=PIN,
                boundary='one superbatch per process; constant LR/WDL; complete optimizer continuation',
                requested_save_rate=request.get('save_rate', 1), effective_save_rate=1,
                trainer_sha256=sha(args.trainer), config_sha256=sha(plan)))
        provenance = json.loads((output / 'segmentation.json').read_text())
        if provenance['trainer_sha256'] != sha(args.trainer):
            raise ValueError('Trainer changed since this plan; reconcile explicitly before continuation')
        latest = None
        if request.get('resume'):
            latest, previous = verify_checkpoint(request['resume'])
            if not initial_resume_compatible(request, previous) or latest['next_superbatch'] != request['start_superbatch']:
                raise ValueError('Original resume recipe mismatch')
        verified = {}
        for candidate in output.glob('segments/*/run/seed-cnnue-v1-*/seed-manifest.json'):
            if not supervisor.allowed(): return 75
            receipt, previous = verify_checkpoint(candidate.parent)
            if recipe(previous) != recipe(request): raise ValueError('Segment recipe mismatch')
            step = receipt['completed_superbatch']
            if step in verified: raise ValueError('Ambiguous duplicate completed superbatch')
            if not request['start_superbatch'] <= step <= request['end_superbatch']: raise ValueError('Unexpected progress')
            verified[step] = receipt
        next_step = request['start_superbatch']
        for step in sorted(verified):
            if step != next_step: raise ValueError('Noncontiguous completed training segments')
            latest = verified[step]; next_step += 1
        if latest: supervisor.checkpoint(latest)
        for step in range(next_step, request['end_superbatch'] + 1):
            if not supervisor.allowed(): return 75
            attempt = output / 'segments' / f'{step:06d}-{uuid.uuid4().hex[:12]}'
            attempt.mkdir(parents=True)
            config = dict(request, output=str(attempt / 'run'), start_superbatch=step,
                          end_superbatch=step, save_rate=1, resume=latest['path'] if latest else None)
            atomic(attempt / 'config.json', config)
            started = time.monotonic()
            code = supervisor.run([str(Path(args.trainer).resolve()), 'train', str(attempt / 'config.json')], attempt / 'console.log')
            # A complete receipt may survive a stop after saving but before process exit.
            checkpoint = attempt / 'run' / f'seed-cnnue-v1-{step}'
            if (checkpoint / 'seed-manifest.json').exists():
                latest, _ = verify_checkpoint(checkpoint)
                supervisor.checkpoint(latest)
                atomic(attempt / 'verified.json', dict(latest, process_exit_code=code,
                       wall_seconds=time.monotonic() - started))
                print(json.dumps(dict(checkpoint=latest, segment_seconds=time.monotonic()-started)), flush=True)
            if supervisor.stopped: return 75
            if code != 0 or not (checkpoint / 'seed-manifest.json').exists():
                raise RuntimeError(f'Training segment failed ({code}); preserve {attempt}')
        supervisor.result = 'completed'
        return 0


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--state-dir', type=Path, help='Isolated control state for validation only; never bypass owner state')
    sub = parser.add_subparsers(dest='action', required=True)
    normal = sub.add_parser('normal', help='Explicitly authorize compute; does not launch work itself')
    normal.add_argument('--minutes', type=float, help='Permission window, then COMPUTE HOLD; no automatic renewal')
    for name in ('hold', 'pause'):
        command = sub.add_parser(name); command.add_argument('--now', action='store_true', help='Emergency stop owned job; lose uncheckpointed work')
    sub.add_parser('status')
    check = sub.add_parser('check'); check.add_argument('--lightweight', action='store_true')
    verify = sub.add_parser('verify'); verify.add_argument('checkpoint')
    for name in ('run', 'train'):
        command = sub.add_parser(name); command.add_argument('--name'); command.add_argument('--grace-seconds', type=int, default=30)
        if name == 'run':
            command.add_argument('--resume-note', required=True); command.add_argument('--log', type=Path, required=True)
            command.add_argument('command', nargs=argparse.REMAINDER)
        else:
            command.add_argument('--trainer', default=str(ROOT / 'build/conventional-nnue/cargo-target/release/seed-conventional-nnue.exe'))
            command.add_argument('config')
    args = parser.parse_args(); control = Control(args.state_dir)
    if hasattr(args, 'grace_seconds') and not 1 <= args.grace_seconds <= 60:
        parser.error('grace-seconds must be 1..60; default 30')
    if args.action == 'normal' and args.minutes is not None and not 0 < args.minutes <= 24*60:
        parser.error('minutes must be >0 and <=1440')
    if args.action in ('normal', 'hold', 'pause'):
        result = control.request(dict(normal='NORMAL', hold='COMPUTE_HOLD', pause='FULL_PAUSE')[args.action],
                                 getattr(args, 'minutes', None), getattr(args, 'now', False))
    elif args.action in ('status', 'check'):
        result = control.status()
        if args.action == 'check':
            allowed = result['lightweight_allowed'] if args.lightweight else result['expensive_allowed']
            print(json.dumps(result, indent=2)); return 0 if allowed else 75
    elif args.action == 'verify': result, _ = verify_checkpoint(args.checkpoint)
    elif args.action == 'train': return train(control, args)
    else:
        command = args.command[1:] if args.command[:1] == ['--'] else args.command
        if not command: parser.error('run needs -- executable arguments')
        with Supervisor(control, args.name or Path(command[0]).name, args.resume_note, args.grace_seconds) as supervisor:
            code = supervisor.run(command, args.log)
            supervisor.result = 'completed' if code == 0 else 'failed'
            return 75 if supervisor.stopped else code
    print(json.dumps(result, indent=2)); return 0


if __name__ == '__main__':
    try: sys.exit(main())
    except (OSError, ValueError, KeyError, RuntimeError) as error:
        print(f'NNUE control: {error}', file=sys.stderr); sys.exit(2)
