"""Destructive checks use TemporaryDirectory only; no retained research is read."""
import hashlib
import json
import os
from pathlib import Path
import struct
import subprocess
import sys
import tempfile
from types import SimpleNamespace
import unittest
from unittest.mock import patch
import zlib
import zipfile

import training_lifecycle as t


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(t.encoded(value))


def neural(workflow, step=1):
    if workflow == 'OneTraining':
        count = 19201
        head = b'BRNONE01' + struct.pack('>IIIdq', 8, 4, count, .01, step)
    else:
        magic, count, _ = t.EPOCH_WORKFLOWS[workflow]
        head = magic + struct.pack('>IIdq', 1, count, .01, step)
    body = head + b'\0'*(count*24)
    return body + struct.pack('>Q', zlib.crc32(body))


def pair_model():
    body = b'S6P2RT01' + struct.pack('>9I', 1, 1, 1, 327, 13, 2, 294, 113231, 64) + b'\0'*(795032-48)
    return body + struct.pack('>I', zlib.crc32(body))


class Fixtures:
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix='seed-training-test-')
        self.addCleanup(self.temp.cleanup)
        self.base = Path(self.temp.name)
        self.root = self.base / 'run'
        self.config = self.base / 'config.json'
        self.selection = self.base / 'selection.json'

    def setup_run(self, workflow='OneTraining', complete=True):
        config = dict(output=str(self.root), training_finalization=t.POLICY, samples=256, epochs=1, width=8, rank=4)
        if workflow in t.EPOCH_WORKFLOWS:
            config['epochs'] = t.EPOCH_WORKFLOWS[workflow][2] if t.EPOCH_WORKFLOWS[workflow] else 2
        write_json(self.config, config)
        with t.training(self.config, workflow) as session:
            if workflow == 'OneTraining':
                write_json(self.root / 'launch-1.binding.json', dict(config_sha256=t.digest(self.config),
                    plan_sha256='plan', runner_sha256='runner', sources={'source': 'hash'}, runtime_sha256={'runtime.jar': 'hash'}))
                write_json(self.root / 'launch-1.execution.json', dict(returncode=0, direct_child_absent=True))
                previous = None
                for i in (1, 2):
                    directory = self.root / 'segments' / f'{i*128:09d}-abcd1234-abc'
                    directory.mkdir(parents=True)
                    state = directory / 'model.state'; state.write_bytes(neural(workflow, i))
                    receipt = directory / 'receipt.json'
                    write_json(receipt, dict(config_sha256=t.digest(self.config), state_sha256=t.digest(state),
                        begin_exposures=(i-1)*128, exposures=i*128, optimizer_steps=i,
                        previous_receipt=str(previous) if previous else None,
                        previous_receipt_sha256=t.digest(previous) if previous else None))
                    previous = receipt
                write_json(self.root / 'initial.json', dict(config_sha256=t.digest(self.config), metrics={}))
                write_json(self.root / 'result.json', dict(config_sha256=t.digest(self.config), exposures=256, samples=256, epochs=1,
                    latest_receipt=str(previous), latest_receipt_sha256=t.digest(previous), metrics={'mse': .1}))
                model = state.relative_to(self.root).as_posix()
            else:
                write_json(self.root / 'binding.json', dict(config_sha256=t.digest(self.config), runtime_sha256={'runtime.jar': 'hash'}, runner_sha256='runner'))
                write_json(self.root / 'execution-1.json', dict(status='COMPLETE', process_absent=True))
                (self.root / 'config.sha256').write_text(t.digest(self.config))
                metrics = []
                for epoch in range(1, config['epochs']+1):
                    state = self.root / f'epoch-{epoch}.state'
                    state.write_bytes(b'disposable-pair-optimizer' if workflow == 'PairObjectiveDiagnostic' else neural(workflow, epoch*8192))
                    r = dict(epoch=epoch, state_sha256=t.digest(state), validation_mse=.1, updates=epoch*8192)
                    model = state.name
                    if workflow == 'PairObjectiveDiagnostic':
                        model = f'epoch-{epoch}.brn-pair2'; (self.root / model).write_bytes(pair_model())
                        r['model_sha256'] = t.digest(self.root / model)
                    write_json(self.root / f'epoch-{epoch}.json', r); metrics.append(r)
                if workflow != 'PairObjectiveDiagnostic':
                    for segment in range(1, config['epochs']*8+1):
                        state = self.root / f'segment-{segment}.state'; state.write_bytes(neural(workflow, segment*1024))
                        write_json(self.root / f'segment-{segment}.json', dict(completed_positions=segment*131072,
                            config_sha256=t.digest(self.config), state_sha256=t.digest(state)))
                write_json(self.root / 'result.json', dict(metrics=metrics))
            if complete:
                session.complete()
        write_json(self.selection, dict(run_id=t.read_json(self.root / t.OWNER)['run_id'],
            dependencies_resolved=True, restart_retired=True, reason='Synthetic completed test; no external consumers',
            retain=[], evidence=['result.json'], models=[dict(path=model, role='final')]))
        return model

    def preview(self):
        return t.operate(self.root, 'preview', self.selection)

    def finalize(self, preview=None):
        preview = preview or self.preview()
        return t.operate(self.root, 'finalize', self.selection, preview['plan_sha256'])

    def fingerprint(self):
        return {p.relative_to(self.root).as_posix(): (p.read_bytes(), p.stat().st_mtime_ns)
                for p in self.root.rglob('*') if p.is_file()}


class Lifecycle(Fixtures, unittest.TestCase):
    def test_guarded_runtime_required_without_changing_old_runtime(self):
        jar = self.base / 'runtime.jar'
        name = 'com/ohinteractive/seedv6/core/brnpair2/OneTraining.class'
        guard = 'com/ohinteractive/seedv6/core/brnpair2/TrainingRunGuard.class'
        command = ['java', '-cp', str(jar), 'com.ohinteractive.seedv6.core.brnpair2.OneTraining']
        with zipfile.ZipFile(jar, 'w') as archive:
            archive.writestr(name, b'legacy class')
        original = jar.read_bytes()
        with self.assertRaisesRegex(ValueError, 'new runtime'): t.require_java_guard(command, 'OneTraining')
        self.assertEqual(jar.read_bytes(), original)
        with zipfile.ZipFile(jar, 'w') as archive:
            archive.writestr(name, guard.removesuffix('.class').encode()); archive.writestr(guard, b'guard')
        t.require_java_guard(command, 'OneTraining')

    def test_preview_finalization_retention_counts_idempotence(self):
        model = self.setup_run()
        before = self.fingerprint(); preview = self.preview()
        self.assertEqual(before, self.fingerprint())
        self.assertEqual(preview['target_files'], 1)
        result = self.finalize(preview)
        self.assertEqual(result['deleted_files'], 1)
        self.assertEqual(result['deleted_bytes'], len(neural('OneTraining')))
        self.assertEqual(result['deleted_bytes'], preview['recoverable_bytes'])
        self.assertEqual(result['recoverable_bytes'], 0)
        self.assertEqual(result['remaining_files'], 0)
        for name, record in preview['retained'].items():
            self.assertEqual(t.digest(self.root / name), record['sha256'])
        self.assertTrue((self.root / model).is_file())
        self.assertEqual(result, self.finalize(preview))
        self.assertEqual(result, t.operate(self.root, 'preview'))
        with self.assertRaisesRegex(ValueError, 'finalized'):
            with t.training(self.config, 'OneTraining'):
                self.fail('Cannot resume finalized output')

    def test_all_epoch_adapters(self):
        for workflow in t.EPOCH_WORKFLOWS:
            with self.subTest(workflow=workflow), tempfile.TemporaryDirectory(dir=self.base) as d:
                self.root = Path(d) / 'run'; self.config = Path(d) / 'config.json'; self.selection = Path(d) / 'select.json'
                model = self.setup_run(workflow)
                expected = 3 if workflow == 'PairObjectiveDiagnostic' else t.EPOCH_WORKFLOWS[workflow][2]*9-1
                result = self.finalize()
                self.assertEqual(result['deleted_files'], expected)
                t.verify_model(self.root / model, workflow)
                self.assertTrue((self.root / 'result.json').is_file())

    def test_incomplete_suspended_running_refused(self):
        self.setup_run(complete=False)
        for status in ('interrupted', 'suspended', 'running', 'failed'):
            owner = t.read_json(self.root / t.OWNER); owner['status'] = status; write_json(self.root / t.OWNER, owner)
            with self.assertRaisesRegex(ValueError, 'not completed'):
                self.preview()

    def test_resume_keeps_checkpoint_and_requires_full_target(self):
        self.setup_run(complete=False)
        result = self.root / 'result.json'; result.unlink()
        before = {p: p.read_bytes() for p in self.root.rglob('model.state')}
        with t.training(self.config, 'OneTraining') as run:
            with self.assertRaises(FileNotFoundError):
                run.complete()
        self.assertEqual({p: p.read_bytes() for p in before}, before)
        self.assertEqual(t.read_json(self.root / t.OWNER)['status'], 'interrupted')

    def test_failed_attempt_bytes_are_retained_after_successful_resume(self):
        self.setup_run(complete=False)
        orphan = self.root / 'segments/000000128-failed000000'
        orphan.mkdir(); partial = orphan / 'model.state'; partial.write_bytes(b'failed write evidence')
        with t.training(self.config, 'OneTraining') as run:
            run.complete()
        self.finalize()
        self.assertEqual(partial.read_bytes(), b'failed write evidence')

    def test_unexpected_binary_is_retained_not_glob_deleted(self):
        self.setup_run()
        extra = self.root / 'comparison.bin'; extra.write_bytes(b'independent consumer')
        self.finalize()
        self.assertEqual(extra.read_bytes(), b'independent consumer')

    def test_known_restart_consumer_holds_source_run_lease(self):
        model = self.setup_run()
        child = self.base / 'child.json'
        write_json(child, dict(output=str(self.base / 'child'), training_finalization=t.POLICY,
                               initial_state=str(self.root / model)))
        with t.training(child, 'OneTraining'):
            result = subprocess.run([sys.executable, '-B', str(Path(t.__file__)), 'preview', str(self.root),
                                     '--selection', str(self.selection)], capture_output=True, text=True)
            self.assertIn('busy', result.stderr)

    def test_no_historical_adoption(self):
        write_json(self.config, dict(output=str(self.root), training_finalization=t.POLICY))
        self.root.mkdir(); (self.root / 'old.state').write_bytes(b'preserve')
        with self.assertRaises(FileNotFoundError):
            with t.training(self.config, 'OneTraining'):
                pass
        self.assertEqual(list(self.root.iterdir()), [self.root / 'old.state'])

    def test_missing_corrupt_ambiguous_exports(self):
        model = self.setup_run(); path = self.root / model; original = path.read_bytes()
        path.unlink()
        with self.assertRaises(ValueError): self.preview()
        path.write_bytes(original[:-1])
        with self.assertRaises(ValueError): self.preview()
        path.write_bytes(original)
        selection = t.read_json(self.selection); selection['models'] = []; write_json(self.selection, selection)
        with self.assertRaisesRegex(ValueError, 'No selected'): self.preview()

    def test_invalid_model_even_if_receipt_hash_matches(self):
        model = self.setup_run(); path = self.root / model; path.write_bytes(b'corrupt')
        receipt = path.with_name('receipt.json'); r = t.read_json(receipt); r['state_sha256'] = t.digest(path); write_json(receipt, r)
        result = t.read_json(self.root / 'result.json'); result['latest_receipt_sha256'] = t.digest(receipt); write_json(self.root / 'result.json', result)
        with self.assertRaisesRegex(ValueError, 'Truncated'): self.preview()

    def test_path_traversal_and_external_receipt_refused(self):
        self.setup_run()
        for name in ('../victim', '/victim', 'C:/victim', 'segments/../victim', 'a\\b', 'foo:stream'):
            selection = t.read_json(self.selection); selection['retain'] = [name]; write_json(self.selection, selection)
            with self.assertRaises(ValueError): self.preview()
        r = t.read_json(self.root / 'result.json'); r['latest_receipt'] = str(self.base / 'victim.json')
        write_json(self.root / 'result.json', r)
        with self.assertRaises(ValueError): t.completion(self.root, 'OneTraining')

    def test_dependency_retention_and_unresolved_review(self):
        self.setup_run(); preview = self.preview(); target = next(iter(preview['targets']))
        selection = t.read_json(self.selection); selection['retain'] = [target]; write_json(self.selection, selection)
        self.assertEqual(self.finalize()['deleted_files'], 0)
        self.assertTrue((self.root / target).exists())

    def test_review_flags_are_required(self):
        self.setup_run()
        for flag in ('dependencies_resolved', 'restart_retired'):
            selection = t.read_json(self.selection); selection[flag] = False; write_json(self.selection, selection)
            with self.assertRaisesRegex(ValueError, 'retention review'): self.preview()
            selection[flag] = True; write_json(self.selection, selection)

    def test_changes_after_preview_are_refused(self):
        self.setup_run(); preview = self.preview()
        path = self.root / 'result.json'; path.write_bytes(path.read_bytes()+b' ')
        with self.assertRaisesRegex(ValueError, 'Preview first'): self.finalize(preview)
        self.assertFalse((self.root / t.PLAN).exists())

    def test_link_and_hardlink_refused(self):
        self.setup_run(); outside = self.base / 'outside'; outside.write_bytes(b'keep')
        link = self.root / 'linked'; os.link(outside, link)
        with self.assertRaisesRegex(ValueError, 'private regular'): self.preview()
        link.unlink()
        try:
            link.symlink_to(outside)
        except OSError:
            if os.name != 'nt': raise
            directory = self.root / 'junction'
            command = ['cmd', '/d', '/c', 'mklink', '/J', str(directory), str(self.base)]
            subprocess.run(command, check=True, capture_output=True)
            try:
                with self.assertRaisesRegex(ValueError, 'Link/reparse'): self.preview()
            finally:
                os.rmdir(directory)
        else:
            with self.assertRaisesRegex(ValueError, 'Link/reparse'): self.preview()
            link.unlink()
        self.assertEqual(outside.read_bytes(), b'keep')

    def test_concurrent_operation_refused_in_fresh_process(self):
        self.setup_run()
        with t.run_lock(self.root):
            result = subprocess.run([sys.executable, '-B', str(Path(t.__file__)), 'preview', str(self.root),
                                     '--selection', str(self.selection)], capture_output=True, text=True)
        self.assertEqual(result.returncode, 2)
        self.assertIn('busy', result.stderr)

    def test_interruption_after_unlink_reconciles_without_fabricated_count(self):
        self.setup_run(); preview = self.preview(); original = t.remove_target
        def crash(*args):
            original(*args)
            raise KeyboardInterrupt('Simulated crash after deletion before acknowledgment')
        with patch.object(t, 'remove_target', crash), self.assertRaises(KeyboardInterrupt):
            self.finalize(preview)
        result = self.finalize(preview)
        self.assertEqual(result['deleted_files'], 0)
        self.assertEqual(result['reconciled_absent_files'], 1)
        self.assertEqual(result['reconciled_absent_bytes'], preview['recoverable_bytes'])

    def test_locked_file_failure_and_retry(self):
        self.setup_run(); preview = self.preview(); target = self.root / next(iter(preview['targets']))
        if os.name == 'nt':
            with target.open('rb'):
                with self.assertRaisesRegex(ValueError, 'Cleanup incomplete'):
                    self.finalize(preview)
        else:
            with patch.object(t, 'remove_target', side_effect=PermissionError('file locked')):
                with self.assertRaisesRegex(ValueError, 'Cleanup incomplete'):
                    self.finalize(preview)
        self.assertTrue(target.exists())
        self.assertEqual(self.finalize(preview)['deleted_files'], 1)

    def test_changed_retained_file_during_partial_cleanup_stops_retry(self):
        self.setup_run('ContextGatedTrainingDiagnostic'); preview = self.preview(); original = t.remove_target
        calls = []
        def fail(*args):
            if calls: raise PermissionError('locked')
            original(*args); calls.append(1)
        with patch.object(t, 'remove_target', fail), self.assertRaises(ValueError): self.finalize(preview)
        progress = t.read_json(self.root / t.PROGRESS)
        self.assertEqual(len(progress['deleted']), 1)
        (self.root / 'result.json').write_bytes(b'{}')
        with self.assertRaisesRegex(ValueError, 'changed/missing'): self.finalize(preview)

    def test_unexpected_new_file_during_cleanup(self):
        self.setup_run(); preview = self.preview()
        with patch.object(t, 'remove_target', side_effect=PermissionError('locked')), self.assertRaises(ValueError):
            self.finalize(preview)
        (self.root / 'unresolved.part').write_bytes(b'partial')
        with self.assertRaisesRegex(ValueError, 'New files'): self.finalize(preview)

    def test_target_replaced_after_stat_refused_by_open_identity(self):
        self.setup_run(); preview = self.preview(); name, record = next(iter(preview['targets'].items()))
        path = self.root / name; data = path.read_bytes(); path.unlink(); path.write_bytes(data)
        with self.assertRaisesRegex(ValueError, 'changed'):
            t.remove_target(self.root, name, record)
        self.assertTrue(path.exists())


class Nnue(Fixtures, unittest.TestCase):
    # A complete zero-valued tensor fixture needs no GPU.
    def test_nnue(self):
        config = dict(output=str(self.root), training_finalization=t.POLICY, start_superbatch=1,
                      end_superbatch=1, data_sha256='data', probes_sha256='probes', seed=1,
                      batch_size=1, batches_per_superbatch=1, learning_rate=.001, wdl=0)
        write_json(self.config, config)
        with t.training(self.config, 'nnue') as run:
            (self.root / 'requested-config.json').write_bytes(self.config.read_bytes())
            write_json(self.root / 'segmentation.json', dict(config_sha256=t.digest(self.config), trainer_sha256='synthetic trainer hash'))
            attempt = self.root / 'segments/000001-0123456789ab'
            checkpoint = attempt / 'run/seed-cnnue-v1-1'
            optimizer = checkpoint / 'optimiser_state'; optimizer.mkdir(parents=True)
            initial = attempt / 'run/initial'; initial.mkdir()
            tensors = b''.join(key.encode()+b'\n'+struct.pack('<Q', count)+b'\0'*(count*4)
                               for key, count in dict(l0w=7680*256, l0f=768*256, l0b=256, l1w=512, l1b=1).items())
            for name in ('weights.bin', 'momentum.bin', 'velocity.bin'):
                (optimizer / name).write_bytes(tensors); (initial / name).write_bytes(tensors)
            size = (7680*256+256+513)*2; payload = b'\0'*size
            (checkpoint / 'quantised.bin').write_bytes(payload+(b'bullet'*11)[:(-size)%64])
            (checkpoint / 'raw.bin').write_bytes(b'\0'*(size*2))
            write_json(attempt / 'run/config.json', config)
            write_json(checkpoint / 'seed-manifest.json', dict(schema='seed-bullet-checkpoint-v1', bullet=t.PIN,
                completed_superbatch=1, config_sha256=t.digest(attempt / 'run/config.json'),
                files={name: t.digest(checkpoint / name) for name in t.NNUE_FILES}))
            write_json(attempt / 'verified.json', dict(process_exit_code=0, receipt_sha256=t.digest(checkpoint / 'seed-manifest.json')))
            run.complete()
        # Exercise the real controller's restart scan with genuine receipt/tensor
        # files. The supervisor is a no-work seam: no GPU child may be launched.
        if os.name == 'nt':
            sys.path.insert(0, str(Path(t.__file__).parent / 'conventional-nnue'))
            import compute_control as cc
            trainer = self.base / 'trainer'; trainer.write_bytes(b'synthetic identity; never executed')
            write_json(self.root / 'segmentation.json', dict(config_sha256=t.digest(self.config), trainer_sha256=t.digest(trainer)))
            owner = t.read_json(self.root / t.OWNER); owner['status'] = 'interrupted'; write_json(self.root / t.OWNER, owner)
            class NoWorkSupervisor:
                def __init__(self, *args): pass
                def __enter__(self): return self
                def __exit__(self, *args): pass
                def allowed(self): return True
                def describe_training(self, *args): pass
                def checkpoint(self, receipt): self.receipt = receipt
                def run(self, *args): raise AssertionError('Completed checkpoint must be skipped')
            with patch.object(cc, 'Supervisor', NoWorkSupervisor):
                code = cc.train(SimpleNamespace(status=lambda: dict(expensive_allowed=True)),
                    SimpleNamespace(config=self.config, trainer=trainer, name=None, grace_seconds=30))
            self.assertEqual(code, 0)
            self.assertEqual(t.read_json(self.root / t.OWNER)['status'], 'completed')
        export = self.root / 'exports/final/network.cnnue'; export.parent.mkdir(parents=True)
        export.write_bytes(b'S6CNU001'+struct.pack('<8I', 1, 7680, 256, 10, 255, 64, 400, size)+hashlib.sha256(payload).digest()+payload)
        write_json(export.with_suffix('.cnnue.json'), dict(file_sha256=t.digest(export), bullet_quantised_sha256=t.digest(checkpoint / 'quantised.bin')))
        write_json(export.parent / 'parity.json', dict(status='PASS', positions=1))
        write_json(export.parent / 'validation.json', dict(positions=1, payload_sha256=hashlib.sha256(payload).hexdigest()))
        write_json(self.selection, dict(run_id=t.read_json(self.root / t.OWNER)['run_id'], dependencies_resolved=True,
            restart_retired=True, reason='Independent synthetic export', retain=[], evidence=['exports/final/parity.json'],
            models=[dict(path='exports/final/network.cnnue', source='segments/000001-0123456789ab/run/seed-cnnue-v1-1/quantised.bin', role='final')]))
        preview = self.preview(); self.assertEqual(preview['target_files'], 8)
        result = self.finalize(preview); self.assertEqual(result['deleted_files'], 8)
        t.verify_model(export, 'nnue')
        self.assertEqual(result, self.finalize(preview))


if __name__ == '__main__':
    unittest.main()
