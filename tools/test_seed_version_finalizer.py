"""Focused failure/re-entry tests; all mutation occurs in disposable Git repos."""
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

import seed_version_finalizer as s

f = s.f


class FinalizerTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix='seed-finalizer-test-')
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.git('init', '-b', 'main')
        self.git('config', 'core.hooksPath', str(self.root / 'no-hooks'))
        self.git('config', 'core.autocrlf', 'false')
        self.git('config', 'commit.gpgsign', 'false')
        self.raw = b'\xef\xbb\xbf{\r\n "major": 1, "minor": 2, "patch": 3, "build": 4\r\n}'
        (self.root / f.VERSION).write_bytes(self.raw)
        self.write('source.txt', 'initial\n')
        self.write('.gitignore', '.gradle/\napp/build/\n')
        self.write('.gradle/cache', 'before')
        self.git('add', '.')
        self.git('-c', 'user.name=Fixture', '-c', 'user.email=fixture@example.invalid', 'commit', '-m', 'fixture')
        self.location = f.state_location(self.root)

    def git(self, *args):
        return f.git(self.root, *args)

    def write(self, name, text):
        path = self.root / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding='utf-8')

    def start(self):
        token = s.begin(self.root)['token']
        self.write('source.txt', 'attributable work ' + token)
        return token

    def folder(self, token):
        return f.operation(self.location, token)

    def cli(self, *args):
        p = subprocess.run([sys.executable, '-B', str(Path(s.__file__)), '--repo', str(self.root), *args],
                           capture_output=True, text=True)
        return p.returncode, json.loads(p.stdout)

    def assert_released(self, token):
        self.assertFalse((self.location / 'active.json').exists())
        self.assertFalse(self.folder(token).exists())
        self.assertEqual(s.status(self.root)['unfinished'], [])
        self.assertEqual(s.status(self.root)['status'], 'idle')

    def assert_next(self):
        token = s.begin(self.root)['token']
        self.assertIsNotNone(token)
        self.assertEqual(s.finish(self.root, token, 'no_bump')['status'], 'no_bump')

    def test_success_preserves_format_and_retry_never_releases_newer_token(self):
        token = self.start()
        result = s.finish(self.root, token, 'bump')
        self.assertEqual(result['status'], 'bumped')
        self.assertTrue(result['verified_bump'])
        self.assertEqual((self.root / f.VERSION).read_bytes(), self.raw.replace(b'"build": 4', b'"build": 5'))
        new = s.begin(self.root)['token']
        self.assertTrue(s.finish(self.root, token, 'bump')['replayed'])
        self.assertEqual(f.read_json(self.location / 'active.json'), {'token': new})
        s.finish(self.root, new, 'no_bump')

    def test_write_exception_is_warning_cleanup_then_next_start(self):
        token = self.start()
        before = {n: (self.folder(token) / n).read_bytes() for n in ('baseline.json', 'state.json')}
        with patch.object(f, 'replace_version', side_effect=PermissionError('simulated read-only version')):
            result = s.finish(self.root, token, 'bump')
        self.assertEqual(result['status'], 'warning')
        self.assertIn('simulated read-only version', result['warnings'][0])
        self.assertEqual(result['blockers'], [])
        self.assertFalse(result['verified_bump'])
        self.assertIsNone(result['after'])
        self.assertEqual((self.root / f.VERSION).read_bytes(), self.raw)
        self.assertEqual((s.warning_path(self.location, token) / 'baseline.json').read_bytes(), before['baseline.json'])
        self.assert_released(token)
        self.assert_next()

    def test_partial_write_exception_does_not_double_increment(self):
        token = self.start()
        original = f.replace_version
        def partial(*args):
            original(*args)
            raise RuntimeError('after replacement')
        with patch.object(f, 'replace_version', side_effect=partial):
            result = s.finish(self.root, token, 'bump')
        self.assertEqual(result['status'], 'warning')
        self.assertFalse(result['verified_bump'])
        raw = (self.root / f.VERSION).read_bytes()
        new = s.begin(self.root)['token']
        self.assertTrue(s.finish(self.root, token, 'bump')['replayed'])
        self.assertEqual((self.root / f.VERSION).read_bytes(), raw)
        self.assertEqual(f.read_json(self.location / 'active.json'), {'token': new})
        s.finish(self.root, new, 'no_bump')

    def test_original_gradle_failure_before_and_after(self):
        token = self.start()
        original = f.replace_version
        def churn(*args):
            original(*args)
            self.write('.gradle/cache', 'concurrent build changed ignored cache')
        with patch.object(f, 'replace_version', side_effect=churn):
            with self.assertRaisesRegex(f.Blocked, 'outside VERSION_STATE'):
                f.finish(self.root, token, 'bump')
        with self.assertRaisesRegex(f.Blocked, 'Overlapping'):
            f.begin(self.root)
        preserved = {n: (self.folder(token) / n).read_bytes() for n in ('baseline.json', 'state.json')}
        raw = (self.root / f.VERSION).read_bytes()
        self.assertEqual(s.reconcile_legacy(self.root, token)['status'], 'warning')
        for name, content in preserved.items():
            self.assertEqual((s.warning_path(self.location, token) / name).read_bytes(), content)
        self.assertTrue(s.reconcile_legacy(self.root, token)['replayed'])
        self.assertEqual((self.root / f.VERSION).read_bytes(), raw)
        self.assert_released(token)
        token = self.start()
        self.write('.gradle/cache', 'before second failure')
        with patch.object(f, 'replace_version', side_effect=churn):
            result = s.finish(self.root, token, 'bump')
        self.assertEqual(result['status'], 'warning')
        self.assertIn('outside VERSION_STATE', result['warnings'][0])
        self.assert_released(token)
        self.assert_next()

    def test_unexpected_version_content_warning_exit_zero_and_no_overwrite(self):
        token = self.start()
        self.write(f.VERSION, 'unexpected version content')
        code, result = self.cli('finish', token, '--decision', 'bump')
        self.assertEqual(code, 0)
        self.assertEqual(result['status'], 'warning')
        self.assertFalse(result['verified_bump'])
        self.assert_released(token)
        code, result = self.cli('begin')
        self.assertEqual(code, 0)
        self.assertEqual(result['status'], 'warning')
        self.assertIsNone(result['token'])
        self.assertFalse((self.location / 'active.json').exists())
        self.assertEqual((self.root / f.VERSION).read_text(), 'unexpected version content')
        (self.root / f.VERSION).write_bytes(self.raw)
        self.assert_next()

    def test_interrupted_update_reentry_never_repeats_write(self):
        token = self.start()
        original = f.replace_version
        def interrupted(*args):
            original(*args)
            raise KeyboardInterrupt()
        with patch.object(f, 'replace_version', side_effect=interrupted):
            with self.assertRaises(KeyboardInterrupt):
                s.finish(self.root, token, 'bump')
        raw = (self.root / f.VERSION).read_bytes()
        self.assert_next()  # begin recovers the interrupted attempt, not only finish.
        result = s.finish(self.root, token, 'bump')
        self.assertEqual(result['status'], 'warning')
        self.assertTrue(result['replayed'])
        self.assertEqual((self.root / f.VERSION).read_bytes(), raw)
        self.assert_released(token)

    def test_cleanup_interruption_at_each_boundary_is_repeatable(self):
        for boundary in ('receipt', 'rename', 'release'):
            with self.subTest(boundary=boundary):
                token = self.start()
                before = (self.root / f.VERSION).read_bytes()
                if boundary == 'receipt':
                    original = s.save
                    def stop(path, value):
                        original(path, value)
                        if path.name == 'warning.json':
                            raise KeyboardInterrupt()
                    context = patch.object(s, 'save', side_effect=stop)
                elif boundary == 'rename':
                    original = os.rename
                    def stop(*args):
                        original(*args)
                        raise KeyboardInterrupt()
                    context = patch.object(os, 'rename', side_effect=stop)
                else:
                    context = patch.object(s, 'release', side_effect=PermissionError('cleanup unavailable'))
                with context, patch.object(f, 'replace_version', side_effect=OSError('version write failed')):
                    with self.assertRaises((KeyboardInterrupt, PermissionError)):
                        s.finish(self.root, token, 'bump')
                self.assert_next()
                self.assertTrue(s.finish(self.root, token, 'bump')['replayed'])
                self.assert_released(token)
                self.assertEqual((self.root / f.VERSION).read_bytes(), before)

    def test_capture_failure_during_begin_releases_pending_reservation(self):
        original = f.stable_snapshot
        calls = 0
        def unstable(root):
            nonlocal calls
            calls += 1
            if calls == 2:
                raise f.Blocked('Repository changed during capture; attribution is unavailable.')
            return original(root)
        with patch.object(f, 'stable_snapshot', side_effect=unstable):
            result = s.begin(self.root)
        self.assertEqual(result['status'], 'warning')
        self.assert_released(result['token'])
        self.assertEqual(s.finish(self.root, result['token'], 'bump')['status'], 'warning')
        self.assert_next()

    def test_interrupted_begin_is_reconciled_without_claiming_a_bump(self):
        original = f.stable_snapshot
        calls = 0
        def stop(root):
            nonlocal calls
            calls += 1
            if calls == 2:
                raise KeyboardInterrupt()
            return original(root)
        with patch.object(f, 'stable_snapshot', side_effect=stop):
            with self.assertRaises(KeyboardInterrupt):
                s.begin(self.root)
        old = f.read_json(self.location / 'active.json')['token']
        self.assert_next()
        self.assert_released(old)
        self.assertFalse(s.status(self.root, old)['verified_bump'])

    def test_absent_version_remains_stateless(self):
        (self.root / f.VERSION).unlink()
        result = s.begin(self.root)
        self.assertEqual(result['status'], 'not_applicable')
        self.assertFalse(self.location.exists())

    def test_corruption_stays_blocking(self):
        token = self.start()
        with (self.folder(token) / 'baseline.json').open('ab') as stream:
            stream.write(b'corruption')
        code, result = self.cli('finish', token, '--decision', 'bump')
        self.assertEqual(code, 2)
        self.assertEqual(result['status'], 'blocked')
        self.assertIn('integrity', result['blockers'][0])
        self.assertTrue((self.location / 'active.json').exists())
        self.assertFalse(s.warning_path(self.location, token).exists())

    def test_unrelated_metadata_change_not_swallowed_or_later_auto_retired(self):
        token = self.start()
        original = f.replace_version
        def unrelated(*args):
            original(*args)
            self.git('config', 'fixture.changed', 'true')
        with patch.object(f, 'replace_version', side_effect=unrelated):
            with self.assertRaisesRegex(s.IntegrityFailure, 'repository identity'):
                s.finish(self.root, token, 'bump')
        with self.assertRaisesRegex(s.IntegrityFailure, 'repository identity'):
            s.begin(self.root)
        self.assertFalse(s.warning_path(self.location, token).exists())

    def test_unexpected_source_write_is_not_a_noncritical_failure(self):
        token = self.start()
        original = f.replace_version
        def unrelated(*args):
            original(*args)
            self.write('source.txt', 'unexpected write outside version boundary')
        with patch.object(f, 'replace_version', side_effect=unrelated):
            with self.assertRaisesRegex(s.IntegrityFailure, 'not ignored'):
                s.finish(self.root, token, 'bump')
        with self.assertRaisesRegex(s.IntegrityFailure, 'not ignored'):
            s.begin(self.root)

    def test_active_work_and_conflicting_retry_stay_protected(self):
        token = self.start()
        with self.assertRaisesRegex(f.Blocked, 'Overlapping'):
            s.begin(self.root)
        with patch.object(f, 'replace_version', side_effect=OSError('version failure')):
            s.finish(self.root, token, 'bump')
        with self.assertRaisesRegex(s.IntegrityFailure, 'contradicts'):
            s.finish(self.root, token, 'no_bump')
        self.assert_next()

    def test_legacy_tracked_changes_are_not_excused(self):
        token = self.start()
        original = f.replace_version
        def change_source(*args):
            original(*args)
            self.write('source.txt', 'unrelated concurrent edit')
        with patch.object(f, 'replace_version', side_effect=change_source):
            with self.assertRaises(f.Blocked):
                f.finish(self.root, token, 'bump')
        with self.assertRaisesRegex(s.IntegrityFailure, 'not ignored'):
            s.reconcile_legacy(self.root, token)
        self.assertEqual((self.root / 'source.txt').read_text(), 'unrelated concurrent edit')


if __name__ == '__main__':
    unittest.main(verbosity=2)
