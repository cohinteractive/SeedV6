"""Explicit, prospective training-run lifecycle. No historical-run adoption.

The two training adapters own completion; an operator owns model selection and
dependency review. Only a preview-bound, receipt-backed allowlist can be removed.
"""
import argparse
from contextlib import contextmanager, ExitStack
import hashlib
import json
import math
import os
from pathlib import Path, PurePosixPath
import re
import stat
import struct
import sys
import threading
import uuid
import zlib
import zipfile

POLICY = 'explicit-v1'
OWNER = 'training-run.json'
CONFIG = 'training-config.json'
LOCK = 'training-run.lock'
PLAN = 'training-finalization-plan.json'
PROGRESS = 'training-finalization.json'
INTERNAL = {OWNER, LOCK, PLAN, PROGRESS}
EPOCH_WORKFLOWS = {
    'PairObjectiveDiagnostic': None,
    'SharedGeometryTrainingDiagnostic': (b'BRNSGP01', 14209, 1),
    'TiedGeometryTrainingDiagnostic': (b'BRNSGT01', 20353, 1),
    'MixedTiedTrainingDiagnostic': (b'BRNMTR01', 20417, 2),
    'ContextGatedTrainingDiagnostic': (b'BRNCGT01', 20481, 2),
}
SUPPORTED = {'nnue', 'OneTraining', *EPOCH_WORKFLOWS}
NNUE_FILES = ('quantised.bin', 'raw.bin', 'optimiser_state/weights.bin',
              'optimiser_state/momentum.bin', 'optimiser_state/velocity.bin')
PIN = '579b78b37a694cacd6e9ede4706386c15c34837c'
_LEASES = set()
_LEASES_LOCK = threading.Lock()


def require(condition, message):
    if not condition:
        raise ValueError(message)


def pairs(items):
    result = {}
    for key, value in items:
        require(key not in result, 'Duplicate JSON key: ' + key)
        result[key] = value
    return result


def read_json(path):
    checked(path)
    return json.loads(Path(path).read_text(encoding='utf-8'), object_pairs_hook=pairs,
                      parse_constant=lambda x: (_ for _ in ()).throw(ValueError('Nonfinite JSON')))


def encoded(value):
    return (json.dumps(value, sort_keys=True, separators=(',', ':'), allow_nan=False) + '\n').encode()


def digest(path):
    checked(path)
    with Path(path).open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def checked(path, directory=False):
    """Do not resolve away evidence of symlinks/junctions or accept hard links."""
    path = Path(os.path.abspath(path))
    for part in reversed((path, *path.parents)):
        info = part.lstat()
        require(not stat.S_ISLNK(info.st_mode) and not getattr(info, 'st_file_attributes', 0) & 0x400,
                'Link/reparse point refused: ' + str(part))
        if part != path or directory:
            require(stat.S_ISDIR(info.st_mode), 'Not a directory: ' + str(part))
        else:
            require(stat.S_ISREG(info.st_mode) and info.st_nlink == 1,
                    'Not a private regular file: ' + str(part))
    return path


def relative(root, name):
    require(isinstance(name, str) and '\\' not in name and ':' not in name,
            'Expected a portable run-relative path')
    p = PurePosixPath(name)
    require(name == p.as_posix() and not p.is_absolute() and p.parts and
            all(x not in ('.', '..') and not x.endswith((' ', '.')) for x in p.parts),
            'Unsafe run-relative path: ' + name)
    return root.joinpath(*p.parts)


def atomic(root, name, value):
    checked(root, directory=True)
    destination = relative(root, name)
    if destination.exists() or destination.is_symlink():
        checked(destination)
    temporary = root / ('.training-' + uuid.uuid4().hex + '.tmp')
    with temporary.open('xb') as stream:
        stream.write(encoded(value)); stream.flush(); os.fsync(stream.fileno())
    os.replace(temporary, destination)
    if os.name != 'nt':
        fd = os.open(root, os.O_RDONLY | os.O_DIRECTORY)
        try:
            os.fsync(fd)
        finally:
            os.close(fd)


@contextmanager
def run_lock(root):
    # POSIX record locks are process-scoped; also exclude concurrent callers in
    # this interpreter. OS locks below exclude other Python/Java processes.
    key = os.path.normcase(str(root))
    with _LEASES_LOCK:
        require(key not in _LEASES, 'Run is busy in this process')
        _LEASES.add(key)
    try:
        with _file_lock(root):
            yield
    finally:
        with _LEASES_LOCK:
            _LEASES.remove(key)


@contextmanager
def _file_lock(root):
    checked(root, directory=True)
    path = checked(root / LOCK)
    with path.open('r+b') as stream:
        try:
            if os.name == 'nt':
                import msvcrt
                msvcrt.locking(stream.fileno(), msvcrt.LK_NBLCK, 1)
            else:
                import fcntl
                fcntl.lockf(stream, fcntl.LOCK_EX | fcntl.LOCK_NB, 1)
        except OSError as exc:
            raise ValueError('Run is busy (training/recovery/finalization); retry after its owner exits') from exc
        try:
            yield
        finally:
            stream.seek(0)
            if os.name == 'nt':
                msvcrt.locking(stream.fileno(), msvcrt.LK_UNLCK, 1)
            else:
                fcntl.lockf(stream, fcntl.LOCK_UN, 1)


def inventory(root, hashes=True):
    result = {}
    def walk(directory):
        checked(directory, directory=True)
        for p in sorted(directory.iterdir()):
            info = p.lstat()
            if stat.S_ISDIR(info.st_mode):
                walk(p)
            else:
                checked(p)
                name = p.relative_to(root).as_posix()
                if name not in INTERNAL:
                    require(not name.startswith('.training-'), 'Interrupted metadata write; preserve and reconcile: ' + name)
                    result[name] = dict(bytes=info.st_size, identity=[info.st_dev, info.st_ino, info.st_mtime_ns])
                    if hashes:
                        result[name]['sha256'] = digest(p)
    walk(root)
    return result


def run_identity(root):
    owner = read_json(root / OWNER)
    require(owner['schema'] == POLICY and owner['workflow'] in SUPPORTED and
            owner['root'] == str(root) and owner['config_sha256'] == digest(root / CONFIG),
            'Run identity/configuration changed; no adoption or relocation')
    require(read_json(root / CONFIG).get('training_finalization') == POLICY, 'Run did not opt in')
    return owner


def require_java_guard(command, workflow):
    """Reject old frozen Java runtimes for opt-in runs, without modifying them."""
    flags = [i for i, word in enumerate(command) if word in ('-cp', '-classpath', '--class-path')]
    require(len(flags) == 1, 'Managed Java training requires an explicit classpath')
    entries = command[flags[0]+1].split(os.pathsep)
    main_name = 'com/ohinteractive/seedv6/core/brnpair2/' + workflow + '.class'
    guard_name = 'com/ohinteractive/seedv6/core/brnpair2/TrainingRunGuard.class'
    owners = 0
    artifacts = {}
    for entry in entries:
        path = Path(entry)
        paths = sorted(path.parent.glob('*.jar')) if path.name == '*' else [path]
        for candidate in paths:
            if candidate.is_dir():
                for file in sorted(candidate.rglob('*.class')):
                    artifacts[str(file.absolute())] = digest(file)
                if not (candidate / main_name).exists():
                    continue
                code = checked(candidate / main_name).read_bytes()
                checked(candidate / guard_name)
            else:
                checked(candidate)
                artifacts[str(candidate.absolute())] = digest(candidate)
                with zipfile.ZipFile(candidate) as archive:
                    if main_name not in archive.namelist():
                        continue
                    require(guard_name in archive.namelist(), 'Rebuild a new runtime with TrainingRunGuard; preserve frozen runtimes')
                    code = archive.read(main_name)
            require(b'com/ohinteractive/seedv6/core/brnpair2/TrainingRunGuard' in code,
                    'Training entrypoint lacks the lifecycle guard; use a new runtime')
            owners += 1
    require(owners == 1, 'Missing/ambiguous guarded training class on classpath')
    return artifacts


class Training:
    def __init__(self, root, owner):
        self.root, self.owner, self.finished = root, owner, False

    def complete(self):
        # This is an execution boundary, never scientific acceptance/selection.
        completion(self.root, self.owner['workflow'])
        self.owner['status'] = 'completed'
        atomic(self.root, OWNER, self.owner)
        self.finished = True


@contextmanager
def training(config_path, workflow):
    config_path = checked(config_path)
    config = read_json(config_path)
    root = Path(os.path.abspath(config['output']))
    if config.get('training_finalization') is None:
        require(not (root / OWNER).exists(), 'Managed run requires its original opt-in config')
        yield None
        return
    require(config['training_finalization'] == POLICY and workflow in SUPPORTED, 'Unsupported finalizable workflow')
    # Enrollment is prospective: never attach lifecycle metadata to an old run.
    if not root.exists():
        checked(root.parent, directory=True)
        root.mkdir()
        (root / LOCK).write_bytes(b'0')
        (root / CONFIG).write_bytes(config_path.read_bytes())
        atomic(root, OWNER, dict(schema=POLICY, root=str(root), run_id=uuid.uuid4().hex,
                                workflow=workflow, working_directory=str(Path.cwd()),
                                config_sha256=digest(config_path), status='interrupted'))
    checked(root, directory=True)
    with run_lock(root), ExitStack() as dependencies:
        owner = run_identity(root)
        require(owner['workflow'] == workflow and owner['config_sha256'] == digest(config_path), 'Immutable run binding changed')
        require(owner['status'] in ('running', 'interrupted'),
                'Run is ' + owner['status'] + '; inspect/finalize it, or use a new output for new training')
        # Hold known cross-run restart sources against concurrent finalization.
        # Other comparison/frozen consumers remain the explicit retention review's
        # responsibility: arbitrary historical text is not a dependency registry.
        sources = set()
        for key in ('resume', 'initial_state'):
            if config.get(key):
                source = Path(os.path.abspath(config[key]))
                for ancestor in (source, *source.parents):
                    if (ancestor / OWNER).is_file():
                        require(ancestor != root, 'Self-referential initial checkpoint')
                        sources.add(ancestor)
                        break
        for source in sorted(sources):
            dependencies.enter_context(run_lock(source))
        owner.update(status='running', token=uuid.uuid4().hex)
        atomic(root, OWNER, owner)
        old = os.environ.get('SEED_TRAINING_TOKEN')
        os.environ['SEED_TRAINING_TOKEN'] = owner['token']
        session = Training(root, owner)
        try:
            yield session
        finally:
            if old is None:
                os.environ.pop('SEED_TRAINING_TOKEN', None)
            else:
                os.environ['SEED_TRAINING_TOKEN'] = old
            if not session.finished:
                owner['status'] = 'interrupted'
                atomic(root, OWNER, owner)


def verify_nnue_checkpoint(path):
    path = checked(path, directory=True)
    receipt = read_json(path / 'seed-manifest.json')
    config_path = path.parent / 'config.json'
    config = read_json(config_path)
    step = int(path.name.removeprefix('seed-cnnue-v1-'))
    require(receipt['schema'] == 'seed-bullet-checkpoint-v1' and receipt['bullet'] == PIN and
            receipt['completed_superbatch'] == step and receipt['config_sha256'] == digest(config_path) and
            set(receipt['files']) == set(NNUE_FILES), 'Checkpoint receipt/configuration mismatch')
    for name in NNUE_FILES:
        p = checked(path / name)
        require(0 < p.stat().st_size <= 16*1024*1024 and digest(p) == receipt['files'][name], 'Checkpoint hash/size: ' + name)
    heads = config.get('output_buckets', 1)
    require(type(heads) is int and heads in (1, 8), 'Unsupported NNUE geometry')
    shapes = {'l0w': 7680*256, 'l0f': 768*256, 'l0b': 256, 'l1w': 512*heads, 'l1b': heads}
    for name in NNUE_FILES[2:]:
        data = (path / name).read_bytes(); offset = 0; records = {}
        while offset < len(data):
            end = data.index(b'\n', offset); key = data[offset:end].decode('ascii')
            count, = struct.unpack_from('<Q', data, end+1); offset = end+9+count*4
            require(key not in records and offset <= len(data), 'Invalid optimizer record')
            records[key] = count
        require(records == shapes, 'Incomplete optimizer tensors')
    quant = (path / 'quantised.bin').read_bytes(); payload = (7680*256+256+513*heads)*2
    require(len(quant) == (payload+63)//64*64 and quant[payload:] == (b'bullet'*11)[:len(quant)-payload]
            and (path / 'raw.bin').stat().st_size == payload*2, 'NNUE network geometry/padding mismatch')
    return receipt, config


def completion(root, workflow):
    """Return only receipt-owned disposable files and all endpoint model identities."""
    config = read_json(root / CONFIG)
    files = inventory(root)
    candidates, endpoints = set(), {}
    def bind(name, expected):
        require(name in files and files[name]['sha256'] == expected, 'Missing/corrupt checkpoint: ' + name)
        candidates.add(name)
    if workflow == 'nnue':
        require((root / 'requested-config.json').read_bytes() == (root / CONFIG).read_bytes(), 'Requested config changed')
        provenance = read_json(root / 'segmentation.json')
        require(provenance['config_sha256'] == digest(root / CONFIG) and provenance['trainer_sha256'], 'Segmentation provenance changed/missing')
        found = {}
        for attempt in sorted((root / 'segments').iterdir()):
            checked(attempt, directory=True)
            if not (attempt / 'verified.json').exists():
                continue  # Failed/partial attempt: retain every byte, never a target.
            verified = read_json(attempt / 'verified.json')
            if verified['process_exit_code'] != 0:
                continue
            checkpoints = list((attempt / 'run').glob('seed-cnnue-v1-*'))
            require(len(checkpoints) == 1, 'Ambiguous/partial NNUE segment')
            checkpoint = checkpoints[0]
            receipt, segment_config = verify_nnue_checkpoint(checkpoint)
            step = receipt['completed_superbatch']
            require(step not in found and segment_config['start_superbatch'] == step and
                    segment_config['end_superbatch'] == step and
                    verified['receipt_sha256'] == digest(checkpoint / 'seed-manifest.json'), 'Segment progress mismatch')
            for key in ('data_sha256', 'probes_sha256', 'seed', 'batch_size', 'batches_per_superbatch', 'learning_rate', 'wdl'):
                require(segment_config[key] == config[key], 'Segment recipe mismatch: ' + key)
            for key, default in (('output_buckets', 1), ('lr_drop_every', 0), ('lr_gamma', 1.0)):
                require(segment_config.get(key, default) == config.get(key, default), 'Segment recipe mismatch: ' + key)
            prefix = checkpoint.relative_to(root).as_posix()
            for name, value in receipt['files'].items():
                bind(prefix + '/' + name, value)
            initial = attempt / 'run/initial'
            require(set(p.name for p in initial.iterdir()) == {'weights.bin', 'momentum.bin', 'velocity.bin'}, 'Unexpected initial state')
            candidates.update(p.relative_to(root).as_posix() for p in initial.iterdir())
            found[step] = prefix + '/quantised.bin'
        require(set(found) == set(range(config['start_superbatch'], config['end_superbatch']+1)), 'Incomplete NNUE interval')
        endpoints = {v: k == config['end_superbatch'] for k, v in found.items()}
    elif workflow == 'OneTraining':
        bindings = list(root.glob('launch-*.binding.json'))
        executions = list(root.glob('launch-*.execution.json'))
        require(bindings and executions, 'Missing ONE launcher provenance/execution evidence')
        for binding in bindings:
            b = read_json(binding)
            require(b['config_sha256'] == digest(root / CONFIG) and b['plan_sha256'] and b['runner_sha256'] and b['sources'] and b['runtime_sha256'],
                    'Missing/mismatched ONE launcher provenance')
        execution_records = [read_json(p) for p in executions]
        require(any(r.get('returncode') == 0 and r.get('error') is None and r.get('direct_child_absent') is True
                    for r in execution_records), 'No successful drained ONE execution receipt')
        result = read_json(root / 'result.json')
        working_directory = Path(read_json(root / OWNER)['working_directory'])
        def receipt_path(name):
            return Path(os.path.abspath(working_directory / name))
        target = config['samples'] * config['epochs']
        require(result['exposures'] == target and result['samples'] == config['samples'] and
                result['epochs'] == config['epochs'] and result['metrics'] and
                result['config_sha256'] == digest(root / CONFIG), 'Incomplete ONE exposure/metrics')
        path = receipt_path(result['latest_receipt'])
        expected_hash, exposure, seen = result['latest_receipt_sha256'], target, set()
        while True:
            require(path.is_relative_to(root) and path.parent.parent == root / 'segments' and path.name == 'receipt.json', 'Receipt chain outside selected run')
            require(path not in seen and digest(path) == expected_hash, 'Receipt chain identity/cycle')
            seen.add(path)
            r = read_json(path)
            require(r['exposures'] == exposure and 0 <= r['begin_exposures'] < exposure and
                    r['config_sha256'] == digest(root / CONFIG) and r['optimizer_steps'] == exposure//128, 'ONE cursor/config mismatch')
            name = path.with_name('model.state').relative_to(root).as_posix()
            bind(name, r['state_sha256']); endpoints[name] = exposure == target
            exposure = r['begin_exposures']
            if r['previous_receipt'] is None:
                require(exposure == 0, 'Missing ONE baseline')
                break
            path = receipt_path(r['previous_receipt']); expected_hash = r['previous_receipt_sha256']
        require({p / 'receipt.json' for p in (root / 'segments').iterdir() if (p / 'receipt.json').exists()} == seen,
                'Ambiguous ONE receipt branches')
        require(read_json(root / 'initial.json')['config_sha256'] == digest(root / CONFIG), 'ONE initial config identity')
    else:
        binding = read_json(root / 'binding.json')
        require(binding['config_sha256'] == digest(root / CONFIG) and binding['runtime_sha256'] and binding['runner_sha256'],
                'Missing/mismatched BRN runtime provenance')
        execution_records = [read_json(p) for p in root.glob('execution-*.json')]
        require(any(r.get('status') == 'COMPLETE' and r.get('process_absent') is True for r in execution_records),
                'Missing successful BRN execution evidence')
        result = read_json(root / 'result.json')
        require((root / 'config.sha256').read_text() == digest(root / CONFIG), 'BRN config identity')
        start, end = config.get('initial_epoch', 0)+1, config['epochs']
        require(end >= start, 'Empty epoch interval')
        metrics = {r['epoch']: r for r in result['metrics']}
        require(len(metrics) == len(result['metrics']), 'Ambiguous epoch metrics')
        for epoch in range(start, end+1):
            r = read_json(root / f'epoch-{epoch}.json')
            require(r['epoch'] == epoch and metrics.get(epoch) == r, 'Missing/mismatched terminal epoch metrics')
            bind(f'epoch-{epoch}.state', r['state_sha256'])
            if workflow == 'PairObjectiveDiagnostic':
                name = f'epoch-{epoch}.brn-pair2'; bind(name, r['model_sha256'])
            else:
                name = f'epoch-{epoch}.state'
            endpoints[name] = epoch == end
        if workflow != 'PairObjectiveDiagnostic':
            require(start == 1 and end == EPOCH_WORKFLOWS[workflow][2], 'Unsupported fixed neural interval')
            for segment in range(1, end*8+1):
                r = read_json(root / f'segment-{segment}.json')
                require(r['completed_positions'] == segment*131072 and r['config_sha256'] == digest(root / CONFIG), 'Neural segment cursor/config')
                bind(f'segment-{segment}.state', r['state_sha256'])
        expected = {name for name in files if name.endswith(('.state', '.brn-pair2')) and '/' not in name}
        require(expected == candidates, 'Unexpected/partial BRN checkpoint')
    return files, candidates, endpoints


def verify_model(path, workflow):
    data = checked(path).read_bytes()
    if workflow == 'nnue':
        require(len(data) >= 72, 'Truncated NNUE export')
        version, features, hidden, buckets, qa, qb, scale, size = struct.unpack_from('<8I', data, 8)
        heads = {1: 1, 2: 8}.get(version, 0)
        require(heads and data[:8] == f'S6CNU00{version}'.encode() and
                (features, hidden, buckets, qa, qb, scale) == (7680, 256, 10, 255, 64, 400) and
                size == (7680*256+256+513*heads)*2 and len(data) == size+72 and
                hashlib.sha256(data[72:]).digest() == data[40:72], 'Invalid NNUE export envelope/payload')
    elif workflow == 'PairObjectiveDiagnostic':
        require(len(data) == 795032 and data[:8] == b'S6P2RT01' and
                struct.unpack_from('>9I', data, 8) == (1, 1, 1, 327, 13, 2, 294, 113231, 64) and
                zlib.crc32(data[:-4]) == struct.unpack('>I', data[-4:])[0], 'Invalid Pair-2 export')
        require(all(math.isfinite(x[0]) for x in struct.iter_unpack('>d', data[44:-4])), 'Nonfinite Pair-2 model')
    else:
        require(len(data) >= 44, 'Truncated neural state')
        if workflow == 'OneTraining':
            width, rank, parameters, rate, step = struct.unpack_from('>IIIdq', data, 8)
            require(data[:8] == b'BRNONE01' and (width, rank) in ((8, 4), (16, 4), (32, 8)) and
                    parameters == 769+1536*(width+rank) and 0 < rate <= .1, 'Invalid ONE schema')
            offset = 36
        else:
            magic, count, _ = EPOCH_WORKFLOWS[workflow]
            mode, parameters, rate, step = struct.unpack_from('>IIdq', data, 8)
            require(data[:8] == magic and mode in (0, 1) and parameters == count, 'Invalid neural schema')
            offset = 32
        require(len(data) == offset+24*parameters+8 and math.isfinite(rate) and rate > 0 and step >= 0 and
                zlib.crc32(data[:-8]) == struct.unpack('>Q', data[-8:])[0], 'Invalid neural geometry/CRC/optimizer')
        for i, (value,) in enumerate(struct.iter_unpack('>d', data[offset:-8])):
            require(math.isfinite(value) and (i < 2*parameters or value >= 0) and
                    (workflow != 'OneTraining' or i >= parameters or abs(value) <= .99), 'Invalid neural parameter/moment')
        return dict(step=step, width=width, rank=rank) if workflow == 'OneTraining' else dict(step=step)


def make_plan(root, selection_path):
    owner = run_identity(root)
    require(owner['status'] == 'completed', 'Run is not completed: ' + owner['status'])
    selection = read_json(selection_path)
    require(selection['run_id'] == owner['run_id'] and selection['dependencies_resolved'] is True and
            selection['restart_retired'] is True and bool(selection['reason'].strip()),
            'Explicit selection, retention review and retirement decision required')
    files, candidates, endpoints = completion(root, owner['workflow'])
    keep = set(selection['retain'])
    require(selection['evidence'], 'List the retained metrics/conclusions/selection evidence')
    keep.update(selection['evidence'])
    for name in keep:
        relative(root, name)
        require(name in files, 'Missing retention dependency: ' + name)
    require(selection['models'], 'No selected/final models')
    terminal = False
    for model in selection['models']:
        name = model['path']; path = relative(root, name)
        require(name in files and model['role'] in ('selected', 'final', 'comparison'), 'Invalid model selection')
        model_info = verify_model(path, owner['workflow'])
        if owner['workflow'] == 'OneTraining':
            config = read_json(root / CONFIG)
            receipt = read_json(path.with_name('receipt.json'))
            require(model_info == dict(step=receipt['optimizer_steps'], width=config['width'], rank=config['rank']),
                    'Selected ONE model geometry/cursor mismatch')
        elif owner['workflow'] in EPOCH_WORKFLOWS and owner['workflow'] != 'PairObjectiveDiagnostic':
            require(model_info['step'] == read_json(path.with_suffix('.json'))['updates'], 'Selected neural model cursor mismatch')
        source = name
        if owner['workflow'] == 'nnue':
            source = model['source']; source_path = relative(root, source)
            require(source in endpoints, 'Export source is not a completed checkpoint')
            metadata = read_json(path.with_suffix(path.suffix + '.json'))
            require(metadata['file_sha256'] == digest(path) and metadata['bullet_quantised_sha256'] == digest(source_path)
                    and path.read_bytes()[72:] == source_path.read_bytes()[:path.stat().st_size-72], 'Export/source mismatch')
            validation = read_json(path.parent / 'validation.json')
            parity = read_json(path.parent / 'parity.json')
            require(parity['status'] == 'PASS' and parity['positions'] > 0 and validation['positions'] > 0 and
                    validation['payload_sha256'] == hashlib.sha256(path.read_bytes()[72:]).hexdigest(),
                    'Retain successful export parity and payload-bound validation metrics')
        require(source in endpoints, 'Selected model is not a verified endpoint')
        terminal |= endpoints[source]
        keep.add(name)
    require(terminal, 'Preserve the final endpoint, including negative/inconclusive results')
    targets = {p: files[p] for p in sorted(candidates-keep)}
    require(all(target_allowed(p, owner['workflow']) for p in targets), 'Target outside workflow allowlist')
    retained = {p: files[p] for p in sorted(files.keys()-targets.keys())}
    return dict(schema=POLICY, run_id=owner['run_id'], root=str(root), workflow=owner['workflow'],
                selection=selection, targets=targets, retained=retained)


def plan_hash(plan):
    return hashlib.sha256(encoded(plan)).hexdigest()


def target_allowed(name, workflow):
    if workflow == 'nnue':
        return re.fullmatch(r'segments/[0-9]{6}-[0-9a-f]{12}/run/(initial/(weights|momentum|velocity)\.bin|seed-cnnue-v1-[0-9]+/(raw\.bin|quantised\.bin|optimiser_state/(weights|momentum|velocity)\.bin))', name)
    if workflow == 'OneTraining':
        return re.fullmatch(r'segments/[0-9]{9,}-[0-9a-f-]{12}/model\.state', name)
    return re.fullmatch(r'(epoch-[0-9]+\.(state|brn-pair2)|segment-[0-9]+\.state)', name)


def validate_plan_files(root, plan, progress, hashes=True):
    require(plan['root'] == str(root) and plan['run_id'] == run_identity(root)['run_id'], 'Finalization belongs to another run')
    require(all(target_allowed(p, plan['workflow']) for p in plan['targets']), 'Target outside workflow allowlist')
    require(not plan['targets'].keys() & plan['retained'].keys(), 'Retention/deletion overlap')
    actual = inventory(root, hashes=hashes)
    expected = plan['targets'] | plan['retained']
    require(not actual.keys()-expected.keys(), 'New files appeared since preview; inspect/re-preview')
    removed = set(progress['deleted']) | set(progress['reconciled_absent'])
    pending = progress.get('in_flight')
    for name, record in expected.items():
        relative(root, name)
        if name in removed:
            require(name not in actual, 'Removed target reappeared; refuse to delete it again: ' + name)
        elif name == pending and name not in actual:
            continue
        else:
            expected_record = record if hashes else {k: v for k, v in record.items() if k != 'sha256'}
            require(actual.get(name) == expected_record, 'File changed/missing since preview: ' + name)


def remove_target(root, name, record):
    # Platform-specific deletion keeps ancestor directories pinned and compares
    # the opened file, not merely a pathname observed earlier.
    from training_unlink import unlink_verified
    unlink_verified(root, name, record)


def report(plan, progress=None):
    progress = progress or dict(deleted=[], reconciled_absent=[], in_flight=None, status='preview')
    deleted = progress['deleted']; reconciled = progress['reconciled_absent']
    remaining = {p: r for p, r in plan['targets'].items() if p not in deleted and p not in reconciled
                 and relative(Path(plan['root']), p).exists()}
    categories = {}
    for name, record in plan['targets'].items():
        category = ('initializer copies' if '/initial/' in name else 'optimizer tensors' if '/optimiser_state/' in name
                    else 'restart states' if name.endswith('.state') else 'intermediate model weights/exports')
        group = categories.setdefault(category, dict(files=0, bytes=0))
        group['files'] += 1; group['bytes'] += record['bytes']
    return dict(run_id=plan['run_id'], run=plan['root'], workflow=plan['workflow'], status=progress['status'],
                plan_sha256=plan_hash(plan), targets=plan['targets'], retained=plan['retained'],
                models=plan['selection']['models'], target_files=len(plan['targets']),
                categories=categories, planned_bytes=sum(v['bytes'] for v in plan['targets'].values()),
                remaining_files=len(remaining), recoverable_bytes=sum(v['bytes'] for v in remaining.values()),
                deleted_files=len(deleted), deleted_bytes=sum(plan['targets'][x]['bytes'] for x in deleted),
                reconciled_absent_files=len(reconciled),
                reconciled_absent_bytes=sum(plan['targets'][x]['bytes'] for x in reconciled),
                in_flight=progress.get('in_flight'), error=progress.get('error'))


def operate(root, action, selection_path=None, expected_hash=None):
    root = checked(root, directory=True)
    with run_lock(root):
        owner = run_identity(root)
        if (root / PLAN).exists():
            plan = read_json(root / PLAN)
            require(owner.get('finalization_sha256', plan_hash(plan)) == plan_hash(plan), 'Stored finalization plan changed')
            require(expected_hash is None or expected_hash == plan_hash(plan), 'Wrong preview hash')
            require(selection_path is None or read_json(selection_path) == plan['selection'], 'Selection changed after cleanup began')
            progress = read_json(root / PROGRESS) if (root / PROGRESS).exists() else dict(
                status='incomplete', deleted=[], reconciled_absent=[], in_flight=None)
            require(owner['status'] in ('completed', 'finalizing', 'finalized'), 'Run lifecycle changed')
            validate_plan_files(root, plan, progress)
        else:
            require(selection_path is not None, '--selection is required before first finalization')
            plan = make_plan(root, selection_path)
            progress = dict(status='preview', deleted=[], reconciled_absent=[], in_flight=None)
        if action == 'preview':
            return report(plan, progress)
        require(expected_hash == plan_hash(plan), 'Preview first; pass its exact --plan-sha256')
        if progress['status'] == 'finalized':
            return report(plan, progress)
        # Immutable deletion intent precedes all destructive work. A process crash
        # after unlink but before acknowledgment is reconciled separately, never
        # reported as a newly measured successful deletion.
        if not (root / PLAN).exists():
            atomic(root, PLAN, plan)
        owner.update(status='finalizing', finalization_sha256=plan_hash(plan)); atomic(root, OWNER, owner)
        progress.update(status='incomplete', error=None)
        atomic(root, PROGRESS, progress)
        try:
            validate_plan_files(root, plan, progress)
            pending = progress['in_flight']
            if pending and not relative(root, pending).exists():
                progress['reconciled_absent'].append(pending); progress['in_flight'] = None
                atomic(root, PROGRESS, progress)
            for name, record in plan['targets'].items():
                if name in progress['deleted'] or name in progress['reconciled_absent']:
                    continue
                # Recheck identities before each removal; full content hashes
                # bracket the operation and the opened target is always hashed.
                validate_plan_files(root, plan, progress, hashes=False)
                progress['in_flight'] = name; atomic(root, PROGRESS, progress)
                remove_target(root, name, record)
                progress['deleted'].append(name); progress['in_flight'] = None
                atomic(root, PROGRESS, progress)
            validate_plan_files(root, plan, progress)
            owner['status'] = 'finalized'; atomic(root, OWNER, owner)
            progress['status'] = 'finalized'; atomic(root, PROGRESS, progress)
        except (OSError, ValueError) as exc:
            progress['error'] = str(exc); atomic(root, PROGRESS, progress)
            raise ValueError('Cleanup incomplete; preserve records and repeat the same preview/finalize: ' + str(exc)) from exc
        return report(plan, progress)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=('preview', 'finalize'))
    parser.add_argument('run', type=Path)
    parser.add_argument('--selection', type=Path)
    parser.add_argument('--plan-sha256')
    args = parser.parse_args()
    try:
        print(json.dumps(operate(args.run, args.action, args.selection, args.plan_sha256), indent=2))
    except (OSError, ValueError, KeyError, TypeError, struct.error) as exc:
        print('Training finalization refused: ' + str(exc), file=sys.stderr)
        return 2
    return 0


if __name__ == '__main__':
    sys.exit(main())
