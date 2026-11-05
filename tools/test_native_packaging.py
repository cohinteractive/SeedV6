"""Portable safety/configuration tests; all fixtures live in ignored build/packaging."""
import argparse
import hashlib
import io
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch
import zipfile

import native_packaging as p
import native_package_checks as checks


# Independent accepted CP64 fixture: never assembled from verifier constants or
# from the runtime being checked. HCE/BREP2 and historical E008 are not this default.
def cp64_identity(workers=1, hash_mib=64):
    return ('baseline=construction-v1-cp64 search=construction-v1 searchProfile=MW_C4 '
            f'flags={1182007295} '
            'configuration=flat-pvs-v1[QXRAY,LMR,LMP,EFFORT,COMPLETED_HINTS,CAPTURE_HISTORY,'
            'CONTINUATION,KILLERS,COUNTERMOVES,NOMINAL_HISTORY,BAD_TACTICAL_LAST,IMPROVING,'
            'CAUTION,CHECK_EXTENSION,RECAPTURE_EXTENSION,FUTILITY,REVERSE_FUTILITY,'
            'HISTORY_PROTECTION,NOMINAL_FUTILITY,VERIFIED_NULL,'
            + 'HEURISTIC_TT_MEMORY,'
            + 'REQUEST_LEARNING,HISTORY_EFFORT,TACTICAL_VERIFICATION];futility=100d;reverse=200d;'
            f'extensionCap=1;workers={workers};hashMiB={hash_mib};aspiration=ADAPTIVE16;'
            f'parallel={"OWNER" if workers == 1 else "EAGER_ROOT"} '
            'evaluator=CONVENTIONAL_NNUE checkpoint=64 schema=S6CNU002 heads=8 '
            'networkSha256=8431626c2fca81e0a1e9ccd5e45ddd8829e6e3282c756c21fad75b06d075bb2b '
            'payload=5da0253642ab33b8e446b5b1c5764750be008cd3dbacc7cd1992a95bf4a881f6 '
            'head=scalar-long scoreMapping=BT4_Q_LOGIT400_V1 score=side-to-move-cp build=74 source=fixture')


def cp64_handshake(identity=None):
    return ['id name SeedV6-74',
            'option name Threads type spin default 1 min 1 max 16',
            'option name Hash type spin default 64 min 1 max 1024',
            'option name Clear Hash type button',
            'option name UseBook type check default false',
            'option name OwnBook type check default false',
            'info string ' + (cp64_identity() if identity is None else identity), 'uciok']


class PackagingSafety(unittest.TestCase):
    def setUp(self):
        root = p.ROOT / 'build/packaging/tests'; root.mkdir(parents=True, exist_ok=True)
        self.temp = tempfile.TemporaryDirectory(dir=root)
        self.work = Path(self.temp.name)

    def tearDown(self):
        self.temp.cleanup()

    def test_cp64_identity_accepts_supported_workers_hash_and_backends(self):
        for workers in range(1, 17):
            for hash_mib in (1, 8, 64, 1024):
                for backend in ('scalar-long', 'vector-128-long', 'vector-256-long'):
                    checks.verify_baseline_identity(cp64_identity(workers, hash_mib).replace(
                        'head=scalar-long', 'head=' + backend), workers, hash_mib)

    def test_cp64_identity_rejects_stale_or_wrong_search_and_evaluator(self):
        for old, new in (
                ('flags=1182007295', 'flags=3407871'), ('searchProfile=MW_C4', 'searchProfile=LEGACY'),
                ('flags=1182007295', 'flags=262143'), ('flags=1182007295', 'flags=11820072950'),
                ('baseline=construction-v1-cp64', 'baseline=construction-v1-BREP2_32'),
                ('search=construction-v1', 'search=E008_QXRAY_LMR_WINDOW_EFFORT_HINTS'),
                ('flat-pvs-v1[', 'recursive-pvs['), (',NOMINAL_FUTILITY', ''), (',VERIFIED_NULL', ''), (',REQUEST_LEARNING', ''), (',HISTORY_EFFORT', ''), (',TACTICAL_VERIFICATION', ''),
                ('flags=1182007295', 'flags=104071167'), ('searchProfile=MW_C4', 'searchProfile=HL_G1'),
                ('futility=100d', 'futility=101d'), ('aspiration=ADAPTIVE16', 'aspiration=WIDEN16'),
                ('parallel=OWNER', 'parallel=ROOT_REUSE'), ('workers=1;', 'workers=4;'),
                ('hashMiB=64;', 'hashMiB=8;'), ('evaluator=CONVENTIONAL_NNUE', 'evaluator=HCE'),
                ('evaluator=CONVENTIONAL_NNUE', 'evaluator=BREP2_32'), ('checkpoint=64', 'checkpoint=32'),
                ('schema=S6CNU002', 'schema=S6CNU001'), ('heads=8', 'heads=1'),
                ('networkSha256=843', 'networkSha256=943'), ('payload=5da', 'payload=6da'),
                ('scoreMapping=BT4_Q_LOGIT400_V1', 'scoreMapping=OTHER'),
                ('score=side-to-move-cp', 'score=white-cp')):
            with self.subTest(field=old), self.assertRaisesRegex(RuntimeError, 'baseline search/evaluator identity'):
                checks.verify_baseline_identity(cp64_identity().replace(old, new), 1, 64)
        for identity in (cp64_identity().replace('flags=1182007295 ', ''), cp64_identity() + ' flags=262143'):
            with self.assertRaisesRegex(RuntimeError, 'baseline search/evaluator identity'):
                checks.verify_baseline_identity(identity, 1, 64)

    def test_c4_identity_requires_guarded_memory_and_worker_appropriate_schedule(self):
        for workers in (1, 2, 3, 4, 5, 8, 16):
            schedule = 'OWNER' if workers == 1 else 'EAGER_ROOT'
            for old, new in [('flags=1182007295', 'flags=1177812991'),
                             ('HEURISTIC_TT_MEMORY,', ''),
                             ('parallel=' + schedule, 'parallel=ROOT_REUSE'),
                             ('parallel=' + schedule, 'parallel=INDEPENDENT'),
                             ('searchProfile=MW_C4', 'searchProfile=MW_C4_W4')]:
                with self.subTest(workers=workers, field=old), self.assertRaises(RuntimeError):
                    checks.verify_baseline_identity(cp64_identity(workers).replace(old, new), workers, 64)

    def test_uci_identity_cannot_borrow_fields_from_other_protocol_lines(self):
        checks.verify_uci_identity(cp64_handshake(), 1, 64)
        for names in ([], ['id name SeedV6-74', 'id name SeedV6-74'],
                      ['id name SeedV6 Construction V1 NNUE CP64 0.0.0 (build 74)'],
                      ['id name SeedV6-74 source=fixture'], ['id name SeedV6-unknown']):
            with self.assertRaisesRegex(RuntimeError, 'Wrong UCI identity'):
                checks.verify_uci_identity(names + cp64_handshake()[1:], 1, 64)
        bad = cp64_handshake(cp64_identity().replace('flags=1182007295', 'flags=262143'))
        bad.insert(-1, 'info string flags=1182007295')
        for lines in (bad, cp64_handshake() + ['info string ' + cp64_identity()],
                      [line for line in cp64_handshake() if not line.startswith('info string baseline=')]):
            with self.assertRaises(RuntimeError):
                checks.verify_uci_identity(lines, 1, 64)
        for option in ('Threads', 'Hash'):
            bad = [line.replace('max 16', 'max 1') if option == 'Threads' else line.replace(
                'default 64', 'default 8') for line in cp64_handshake()]
            with self.assertRaisesRegex(RuntimeError, 'option'):
                checks.verify_uci_identity(bad, 1, 64)

    def test_native_gate_checks_cold_default_before_setting_options(self):
        for identity in (cp64_identity().replace('flags=1182007295', 'flags=3407871'),
                         cp64_identity().replace('searchProfile=MW_C4', 'searchProfile=LEGACY'),
                         cp64_identity().replace('flags=1182007295', 'flags=262143'), cp64_identity(1, 8)):
            with patch.object(checks, 'Engine') as engine_class:
                engine = engine_class.return_value
                engine.until.return_value = cp64_handshake(identity)
                with self.assertRaisesRegex(RuntimeError, 'baseline search/evaluator identity'):
                    checks.verify_uci(self.work / 'launcher', self.work, {}, self.work / 'model')
                self.assertEqual([('uci',)], [call.args for call in engine.send.call_args_list])
                engine.kill.assert_called_once()

    def test_sha256_vectors_and_chunk_boundaries(self):
        payload = self.work / 'hash input with spaces'
        for data in (b'', b'abc', *(bytes(range(256)) * (size // 256) + bytes(range(size % 256))
                                   for size in (1024 * 1024 - 1, 1024 * 1024, 1024 * 1024 + 1,
                                                3 * 1024 * 1024 + 123))):
            with self.subTest(bytes=len(data)):
                payload.write_bytes(data)
                actual = p.sha(payload)
                self.assertEqual(hashlib.sha256(data).hexdigest(), actual)
                # Also compare the former implementation when running on 3.11+.
                if hasattr(hashlib, 'file_digest'):
                    with payload.open('rb') as stream:
                        self.assertEqual(hashlib.file_digest(stream, 'sha256').hexdigest(), actual)
        payload.write_bytes(b'abc')
        self.assertEqual('ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad', p.sha(payload))
        with self.assertRaises(FileNotFoundError):
            p.sha(self.work / 'missing')

    def test_sha256_bounded_reads_and_short_chunks(self):
        data = bytes(range(256)) * 8193
        test = self
        class ShortReads(io.BytesIO):
            def read(self, size=-1):
                test.assertGreater(size, 0, 'Hashing must not read the whole file at once')
                test.assertLessEqual(size, 1024 * 1024)
                return super().read(min(size, 8191))
        with patch.object(Path, 'open', return_value=ShortReads(data)):
            self.assertEqual(hashlib.sha256(data).hexdigest(), p.sha(self.work / 'short reads'))

    def fixture(self):
        image = self.work / 'image with spaces'; image.mkdir()
        (image / 'payload').write_bytes(b'compiled runtime payload')
        p.write_json(image / 'build-manifest.json', {'files': p.file_inventory(image)})
        archive = self.work / 'image.zip'; p.make_zip(image, archive)
        return image, archive

    def test_image_archive_and_extraction_must_match(self):
        image, archive = self.fixture()
        p.verify_integrity(image, archive)
        p.extract_verified(archive, self.work / 'extracted')
        p.verify_integrity(self.work / 'extracted' / image.name)
        (image / 'payload').write_bytes(b'altered')
        with self.assertRaisesRegex(RuntimeError, 'inventory'):
            p.verify_integrity(image, archive)

    def test_archive_corruption_cannot_be_certified(self):
        image, archive = self.fixture()
        with zipfile.ZipFile(archive, 'a') as zipped:
            zipped.writestr(image.name + '/extra', b'undeclared')
        with self.assertRaisesRegex(RuntimeError, 'Archive differs'):
            p.verify_integrity(image, archive)

    def test_zip_traversal_cannot_escape(self):
        archive = self.work / 'bad.zip'
        with zipfile.ZipFile(archive, 'w') as zipped:
            zipped.writestr('../outside', b'no')
        with self.assertRaisesRegex(RuntimeError, 'Unsafe'):
            p.extract_verified(archive, self.work / 'extract')
        self.assertFalse((self.work / 'outside').exists())

    def test_occupied_destination_is_preserved(self):
        source = self.work / 'staged'; source.mkdir(); (source / 'new').write_text('new')
        target = self.work / 'occupied'; target.mkdir(); (target / 'keep').write_text('keep')
        with self.assertRaises((OSError, RuntimeError)):
            p.rename_exclusive(source, target)
        self.assertEqual('keep', (target / 'keep').read_text())
        self.assertTrue((source / 'new').exists())

    def test_wrong_platform_fails_before_reading_jdk(self):
        other = 'macos' if os.name == 'nt' else 'windows'
        with self.assertRaisesRegex(RuntimeError, 'requires a .* host'):
            p.host_check(other, self.work / 'missing-jdk')

    def test_native_failure_has_no_final_candidate(self):
        context = self.work / 'context.json'
        p.write_json(context, {'platform': 'windows', 'jdk': str(self.work), 'source': {}, 'candidate': 'failure'})
        final = self.work / 'candidates/failure'
        args = argparse.Namespace(context=str(context), platform='windows', application='uci', jar=[])
        with patch.object(p, 'source_state', return_value={}), patch.object(p, 'host_check'), \
             patch.object(p, 'destination', return_value=final), \
             patch.object(p, 'build_image', side_effect=RuntimeError('jpackage exit 23')):
            with self.assertRaisesRegex(RuntimeError, 'jpackage exit 23'):
                p.package(args)
        self.assertFalse(final.exists())
        self.assertFalse(list(final.parent.glob('.staging-*')))
        attempts = list(final.parent.glob('.last-attempt-*.json'))
        self.assertEqual(1, len(attempts))
        self.assertEqual('FAILED', json.loads(attempts[0].read_text())['status'])

    def published_fixture(self, name='current'):
        output = self.work / name; output.mkdir()
        image = output / 'image'; image.mkdir()
        (image / 'payload').write_text(name)
        p.write_json(image / 'build-manifest.json', {'files': p.file_inventory(image)})
        archive = output / 'image.zip'; p.make_zip(image, archive)
        p.write_json(output / 'candidate.json', {'status': 'VERIFIED_LOCAL_CANDIDATE',
            'image': image.name, 'archive': archive.name, 'archiveSha256': p.sha(archive),
            'manifestSha256': p.sha(image / 'build-manifest.json')})
        return output

    def test_current_paths_are_stable_separate_and_outside_candidates(self):
        self.assertEqual(p.ROOT / 'dist/windows/current', p.destination('windows', 'gui', 'current'))
        self.assertEqual(p.ROOT / 'releases/current/windows-x64', p.destination('windows', 'uci', 'current'))
        self.assertEqual(p.ROOT / 'dist/windows/candidates/frozen-identity',
                         p.destination('windows', 'gui', 'frozen-identity'))

    def test_success_replaces_complete_distribution_and_removes_backup(self):
        old = self.published_fixture()
        new = self.published_fixture('new')
        p.publish(new, old, True)
        p.verify_published(old)
        self.assertEqual('new', (old / 'image/payload').read_text())
        self.assertFalse(new.exists())
        self.assertFalse((self.work / '.previous-current').exists())

    def test_finder_metadata_does_not_block_replacing_current_distribution(self):
        old = self.published_fixture(); new = self.published_fixture('new')
        metadata = old / '.DS_Store'; metadata.write_bytes(b'Finder metadata')
        p.verify_published(old)
        self.assertEqual(b'Finder metadata', metadata.read_bytes())
        p.publish(new, old, True)
        p.verify_published(old)
        self.assertEqual('new', (old / 'image/payload').read_text())
        self.assertFalse((self.work / '.previous-current').exists())

    def test_directory_named_like_finder_metadata_is_preserved(self):
        old = self.published_fixture(); new = self.published_fixture('new')
        directory = old / '.DS_Store'; directory.mkdir()
        note = directory / 'user-notes.txt'; note.write_text('keep this work')
        with self.assertRaisesRegex(RuntimeError, 'Unexpected files'):
            p.publish(new, old, True)
        self.assertEqual('keep this work', note.read_text())
        self.assertTrue(new.exists())

    @unittest.skipIf(os.name == 'nt', 'Symlink creation requires Windows privileges')
    def test_symlink_named_like_finder_metadata_is_preserved(self):
        old = self.published_fixture(); new = self.published_fixture('new')
        metadata = old / '.DS_Store'; metadata.symlink_to('image/payload')
        with self.assertRaisesRegex(RuntimeError, 'Unexpected files'):
            p.publish(new, old, True)
        self.assertTrue(metadata.is_symlink())
        self.assertEqual('current', metadata.read_text())
        self.assertTrue(new.exists())

    def test_publication_failure_restores_exact_previous_distribution(self):
        old = self.published_fixture(); before = p.file_inventory(old)
        new = self.published_fixture('new')
        rename = p.rename_exclusive
        def fail_new(source, target):
            if source == new:
                raise OSError('injected publication failure')
            rename(source, target)
        with patch.object(p, 'rename_exclusive', side_effect=fail_new):
            with self.assertRaisesRegex(OSError, 'injected'):
                p.publish(new, old, True)
        self.assertEqual(before, p.file_inventory(old))
        self.assertTrue(new.exists())

    def test_interruption_after_retirement_restores_previous_before_build(self):
        old = self.published_fixture(); before = p.file_inventory(old)
        p.rename_exclusive(old, self.work / '.previous-current')
        p.recover_previous(old)
        self.assertEqual(before, p.file_inventory(old))

    def test_failed_restore_names_preserved_backup_and_next_build_recovers(self):
        old = self.published_fixture(); before = p.file_inventory(old)
        new = self.published_fixture('new'); previous = self.work / '.previous-current'
        rename = p.rename_exclusive
        def fail_publication_and_restore(source, target):
            if source in (new, previous):
                raise OSError('injected unavailable destination')
            rename(source, target)
        with patch.object(p, 'rename_exclusive', side_effect=fail_publication_and_restore):
            with self.assertRaisesRegex(RuntimeError, 'previous complete distribution remains'):
                p.publish(new, old, True)
        self.assertFalse(old.exists())
        self.assertEqual(before, p.file_inventory(previous))
        p.recover_previous(old)
        self.assertEqual(before, p.file_inventory(old))

    def test_interruption_after_publication_cleans_verified_previous(self):
        old = self.published_fixture()
        p.rename_exclusive(old, self.work / '.previous-current')
        new = self.published_fixture('new'); p.rename_exclusive(new, old)
        p.recover_previous(old)
        p.verify_published(old)
        self.assertFalse((self.work / '.previous-current').exists())

    def test_modified_output_and_backup_are_preserved_for_inspection(self):
        old = self.published_fixture()
        new = self.published_fixture('new')
        (old / 'image/payload').write_text('user edit')
        with self.assertRaisesRegex(RuntimeError, 'inventory'):
            p.publish(new, old, True)
        self.assertEqual('user edit', (old / 'image/payload').read_text())
        self.assertTrue(new.exists())

    def test_unrelated_files_beside_current_image_are_not_discarded(self):
        old = self.published_fixture(); new = self.published_fixture('new')
        (old / '.DS_Store').write_bytes(b'Finder metadata')
        note = old / 'user-notes.txt'; note.write_text('keep this work')
        with self.assertRaisesRegex(RuntimeError, 'Unexpected files'):
            p.publish(new, old, True)
        self.assertEqual('keep this work', note.read_text())
        self.assertTrue(new.exists())

    def test_frozen_output_cannot_be_replaced(self):
        old = self.published_fixture('frozen'); before = p.file_inventory(old)
        new = self.published_fixture('new')
        with self.assertRaises((OSError, RuntimeError)):
            p.publish(new, old, False)
        self.assertEqual(before, p.file_inventory(old))

    @unittest.skipUnless(os.name == 'nt', 'Windows handle sharing check')
    def test_actual_locked_file_leaves_previous_distribution_unchanged(self):
        old = self.published_fixture(); before = p.file_inventory(old)
        new = self.published_fixture('new')
        with (old / 'image/payload').open('rb'):
            with self.assertRaisesRegex(RuntimeError, 'locked'):
                p.publish(new, old, True)
        self.assertEqual(before, p.file_inventory(old))
        self.assertTrue(new.exists())

    def test_output_lock_excludes_another_python_process_and_releases(self):
        output = self.work / 'current'
        command = [sys.executable, '-B', '-c',
                   'import native_packaging as p; from pathlib import Path; import sys\n'
                   'with p.output_lock(Path(sys.argv[1])): print("acquired")', str(output)]
        with p.output_lock(output):
            result = subprocess.run(command, cwd=Path(p.__file__).parent, capture_output=True, text=True)
            self.assertNotEqual(0, result.returncode)
            self.assertIn('Another build owns', result.stderr)
        result = subprocess.run(command, cwd=Path(p.__file__).parent, capture_output=True, text=True)
        self.assertEqual(0, result.returncode, result.stderr)

    def test_failed_packaging_preserves_current_and_cleans_staging(self):
        old = self.published_fixture(); before = p.file_inventory(old)
        args = argparse.Namespace(application='uci', platform='windows', keep_staging=False)
        with patch.object(p, 'build_image', side_effect=RuntimeError('compile/package failure')):
            with self.assertRaisesRegex(RuntimeError, 'compile/package failure'):
                p.package_locked(args, {'candidate': 'current'}, [], old)
        self.assertEqual(before, p.file_inventory(old))
        self.assertFalse((self.work / '.staging-current').exists())
        self.assertIn('compile/package failure', (self.work / '.last-attempt-current.json').read_text())

    def test_abandoned_owned_staging_is_removed_but_unowned_is_preserved(self):
        old = self.published_fixture(); stage = self.work / '.staging-current'; stage.mkdir()
        (stage / 'large-incomplete-output').write_bytes(b'partial')
        p.write_json(stage / 'attempt.json', {'owner': 'someone-else', 'output': str(old)})
        args = argparse.Namespace(application='uci', platform='windows', keep_staging=False)
        with self.assertRaisesRegex(RuntimeError, 'Unowned'):
            p.package_locked(args, {'candidate': 'current'}, [], old)
        self.assertTrue((stage / 'large-incomplete-output').exists())
        p.write_json(stage / 'attempt.json', {'owner': 'seedv6-native-packaging', 'output': str(old)})
        with patch.object(p, 'build_image', side_effect=RuntimeError('new build failure')):
            with self.assertRaisesRegex(RuntimeError, 'new build failure'):
                p.package_locked(args, {'candidate': 'current'}, [], old)
        self.assertFalse(stage.exists())

    def test_explicit_diagnostic_retention_blocks_automatic_discard(self):
        old = self.published_fixture()
        args = argparse.Namespace(application='uci', platform='windows', keep_staging=True)
        with patch.object(p, 'build_image', side_effect=RuntimeError('diagnose this')):
            with self.assertRaisesRegex(RuntimeError, 'diagnose this'):
                p.package_locked(args, {'candidate': 'current'}, [], old)
        self.assertTrue((self.work / '.staging-current/attempt.json').exists())
        with self.assertRaisesRegex(RuntimeError, 'retained by request'):
            p.package_locked(args, {'candidate': 'current'}, [], old)

    def test_generated_readonly_launcher_is_cleaned(self):
        stage = self.work / 'staging'; stage.mkdir()
        file = stage / 'launcher.exe'; file.write_bytes(b'fixture')
        file.chmod(0o444)
        p.remove_idle_tree(stage)
        self.assertFalse(stage.exists())

    def test_mac_launcher_and_native_command_configuration(self):
        text = p.launcher_text()
        self.assertIn('exec "$APP_HOME/runtime/bin/java"', text)
        self.assertIn('"$APP_HOME/app/*"', text)
        self.assertIn('"$@"', text)
        self.assertIn('/usr/bin/dirname', text)
        self.assertNotIn('open -a', text)
        self.assertNotIn('JAVA_HOME/bin', text)
        self.assertIn('--add-modules=jdk.incubator.vector', text)  # Normal CP64 resolves its optional fast backend.
        nnue = p.launcher_text(nnue=True)
        self.assertIn('--add-modules=jdk.incubator.vector', nnue)
        self.assertIn('-XX:ActiveProcessorCount=2', nnue)
        self.assertIn(checks.NNUE_MAIN + ' "$@"', nnue)
        self.assertEqual((Path('SeedV6-GUI.app/Contents/app'), Path('SeedV6-GUI.app/Contents/runtime/Contents/Home'),
                          Path('SeedV6-GUI.app/Contents/MacOS/SeedV6-GUI')),
                         p.layouts(Path('SeedV6-GUI.app'), 'gui', 'macos'))

    def test_mac_gui_template_preserves_display_version_and_all_icon_sizes(self):
        jdk = self.work / 'jdk'; (jdk / 'jmods').mkdir(parents=True)
        with zipfile.ZipFile(jdk / 'jmods/jdk.jpackage.jmod', 'w') as archive:
            archive.writestr('classes/jdk/jpackage/internal/resources/Info-lite.plist.template',
                             'display=DEPLOY_BUNDLE_SHORT_VERSION; build=DEPLOY_BUNDLE_CFBUNDLE_VERSION')
        stage = self.work / 'stage'; stage.mkdir()
        with patch.object(p, 'run', return_value='') as run:
            resources, icon = p.mac_resources({'jdk': str(jdk), 'version': {'major': 0, 'minor': 0, 'patch': 0, 'build': 35}}, stage)
        self.assertEqual('display=0.0.0; build=DEPLOY_BUNDLE_CFBUNDLE_VERSION', (resources / 'Info.plist').read_text())
        commands = [call.args[0] for call in run.call_args_list]
        self.assertEqual(10, sum(c[0] == '/usr/bin/sips' for c in commands))
        self.assertEqual('/usr/bin/iconutil', commands[-1][0])
        self.assertEqual(stage / 'seedv6.icns', icon)

    def build_uci(self, platform='macos', modules='java.base,jdk.incubator.vector'):
        stage = self.work / 'stage'; stage.mkdir()
        model = self.work / 'model'; model.write_bytes(b'fixture-only')
        jars = []
        for name in ('engine-core.jar', 'app-uci.jar'):
            jar = self.work / name; jar.write_bytes(b'fixture-only'); jars.append(jar)
        context = {'platform': platform, 'model': str(model), 'modelSha256': p.sha(model), 'jdk': str(self.work / 'jdk'),
                   'version': {'major': 0, 'minor': 0, 'patch': 0, 'build': 35}}
        identity = {'model.sha256': p.sha(model), 'version': '0.0.0', 'build': '35',
                    'baseline.identity': cp64_identity()}
        commands = []
        def native(command, **kwargs):
            commands.append(command)
            if 'jdeps' in Path(command[0]).name:
                return modules
            if 'jpackage' in Path(command[0]).name:
                image = stage / 'SeedV6-UCI'
                shutil.copytree(stage / 'input', image / 'app')
                (image / 'SeedV6-UCI.exe').touch()
                runtime = image / 'runtime'
            else:
                runtime = Path(command[-1])
            (runtime / 'legal/java.base').mkdir(parents=True)
            (runtime / 'legal/java.base/LICENSE').write_text('fixture-only')
            return ''
        with patch.object(p, 'model_probe', return_value=identity), patch.object(p, 'run', side_effect=native), \
             patch.object(p, 'production_manifest', return_value={'identity': {'model.sha256': p.sha(model)}}):
            image, _ = p.build_image(context, 'uci', stage, jars)
        return image, commands, model

    def test_mac_uci_links_exact_modules_and_keeps_baseline_and_e008_launchers(self):
        image, commands, model = self.build_uci()
        self.assertEqual(p.launcher_text(), (image / 'SeedV6-UCI').read_text())
        self.assertEqual(p.launcher_text().encode('utf-8'), (image / 'SeedV6-UCI').read_bytes())
        if os.name != 'nt':
            self.assertEqual(0o755, (image / 'SeedV6-UCI').stat().st_mode & 0o777)
        self.assertEqual(p.launcher_text(nnue=True).encode('utf-8'), (image / checks.NNUE_LAUNCHER).read_bytes())
        if os.name != 'nt':
            self.assertEqual(0o755, (image / checks.NNUE_LAUNCHER).stat().st_mode & 0o777)
        self.assertEqual('java.base,jdk.incubator.vector', commands[1][commands[1].index('--add-modules') + 1])
        self.assertEqual({'engine-core.jar', 'app-uci.jar'}, {f.name for f in (image / 'app').glob('*.jar')})
        self.assertEqual(p.sha(model), p.sha(image / 'models/baseline.brn-pair2'))
        self.assertEqual(p.sha(p.ROOT / 'docs/nnue-botli-candidate.md'), p.sha(image / 'NNUE-CANDIDATE.md'))
        self.assertEqual(p.sha(p.ROOT / 'build-assets/nnue-candidate/manifest.json'), p.sha(image / 'network-manifest.json'))

    def test_windows_uci_links_same_modules_and_adds_nnue_console_launcher(self):
        image, commands, _ = self.build_uci(platform='windows')
        command = commands[1]
        self.assertEqual('java.base,jdk.incubator.vector', command[command.index('--add-modules') + 1])
        self.assertIn('--win-console', command)
        self.assertEqual(p.APPS['uci'][1], command[command.index('--main-class') + 1])
        name, properties = command[command.index('--add-launcher') + 1].split('=', 1)
        self.assertEqual(checks.NNUE_LAUNCHER, name)
        config = Path(properties).read_text()
        self.assertIn('main-class=' + checks.NNUE_MAIN, config)
        self.assertIn('--add-modules=jdk.incubator.vector', config)
        self.assertIn('win-console=true', config)

    def test_uci_jdeps_rejects_unexpected_desktop_dependency(self):
        with self.assertRaisesRegex(RuntimeError, 'must require exactly'):
            self.build_uci(modules='java.base,java.desktop,jdk.incubator.vector')
        self.assertFalse((self.work / 'stage/SeedV6-UCI/runtime').exists())

    def test_uci_jdeps_rejects_missing_vector_dependency(self):
        with self.assertRaisesRegex(RuntimeError, 'must require exactly'):
            self.build_uci(platform='windows', modules='java.base')
        self.assertFalse((self.work / 'stage/SeedV6-UCI').exists())

    def test_uci_runtime_rejects_missing_extra_modules_and_owner_leakage(self):
        app = self.work / 'app'; app.mkdir()
        for name in ('app-uci.jar', 'engine-core.jar'):
            with zipfile.ZipFile(app / name, 'w') as jar:
                jar.writestr(name + '.class', b'fixture')
        modules = ['java.base@21', 'jdk.incubator.vector@21']
        checks.verify_dependencies('uci', app, modules, 'macos')
        for invalid in (modules[:1], modules + ['java.desktop@21'], modules + ['java.sql@21']):
            with self.subTest(modules=invalid), self.assertRaisesRegex(RuntimeError, 'runtime must contain exactly'):
                checks.verify_dependencies('uci', app, invalid, 'macos')
        with zipfile.ZipFile(app / 'engine-core.jar', 'a') as jar:
            jar.writestr('com/ohinteractive/seedv6/training/Leak.class', b'fixture')
        with self.assertRaisesRegex(RuntimeError, 'UCI owner leakage'):
            checks.verify_dependencies('uci', app, modules, 'macos')

    def test_tracked_model_and_negative_inputs(self):
        model = p.ROOT / 'build-assets/production' / p.production_manifest()['payload']
        self.assertEqual('5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7', p.verify_model(model))
        missing = self.work / 'missing.brn-pair2'
        with self.assertRaisesRegex(RuntimeError, 'Required production model is missing'):
            p.verify_model(missing)
        missing.write_bytes(b'truncated')
        with self.assertRaisesRegex(RuntimeError, 'Incompatible production model length'):
            p.verify_model(missing)
        data = bytearray(model.read_bytes()); data[120] ^= 1
        missing.write_bytes(data)
        with self.assertRaisesRegex(RuntimeError, 'expected SHA-256'):
            p.verify_model(missing)

    def test_manifest_cannot_change_engine_identity(self):
        model = p.ROOT / 'build-assets/production' / p.production_manifest()['payload']
        context = {'platform': 'windows', 'model': str(model), 'modelSha256': p.sha(model)}
        identity = dict(p.production_manifest()['identity'], **{'model.evaluator': 'OTHER'})
        with patch.object(p, 'model_probe', return_value=identity):
            with self.assertRaisesRegex(RuntimeError, 'authoritative engine-core model identity'):
                p.build_image(context, 'uci', self.work, [])

    def test_gui_and_uci_reject_wrong_baseline_even_with_valid_legacy_model(self):
        model = p.ROOT / 'build-assets/production' / p.production_manifest()['payload']
        context = {'platform': 'windows', 'model': str(model), 'modelSha256': p.sha(model)}
        for app in ('gui', 'uci'):
            stage = self.work / app; stage.mkdir()
            identity = dict(p.production_manifest()['identity'], **{
                'baseline.identity': cp64_identity().replace('flags=1182007295', 'flags=262143')})
            with patch.object(p, 'model_probe', return_value=identity), patch.object(p, 'run') as native:
                with self.assertRaisesRegex(RuntimeError, 'baseline search/evaluator identity'):
                    p.build_image(context, app, stage, [])
                native.assert_not_called()


if __name__ == '__main__':
    unittest.main()
