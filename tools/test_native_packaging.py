"""Portable safety/configuration tests; all fixtures live in ignored build/packaging."""
import argparse
import hashlib
import io
import json
import os
from pathlib import Path
import shutil
import tempfile
import unittest
from unittest.mock import patch
import zipfile

import native_packaging as p
import native_package_checks as checks


class PackagingSafety(unittest.TestCase):
    def setUp(self):
        root = p.ROOT / 'build/packaging/tests'; root.mkdir(parents=True, exist_ok=True)
        self.temp = tempfile.TemporaryDirectory(dir=root)
        self.work = Path(self.temp.name)

    def tearDown(self):
        self.temp.cleanup()

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
        attempts = list(final.parent.glob('.staging-*/attempt.json'))
        self.assertEqual(1, len(attempts))
        self.assertEqual('FAILED_UNVERIFIED', json.loads(attempts[0].read_text())['status'])

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
        identity = {'model.sha256': p.sha(model), 'version': '0.0.0', 'build': '35'}
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


if __name__ == '__main__':
    unittest.main()
