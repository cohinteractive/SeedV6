"""Portable safety/configuration tests; all fixtures live in ignored build/packaging."""
import argparse
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import zipfile

import native_packaging as p


class PackagingSafety(unittest.TestCase):
    def setUp(self):
        root = p.ROOT / 'build/packaging/tests'; root.mkdir(parents=True, exist_ok=True)
        self.temp = tempfile.TemporaryDirectory(dir=root)
        self.work = Path(self.temp.name)

    def tearDown(self):
        self.temp.cleanup()

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

    def test_mac_uci_uses_only_base_runtime_and_direct_exec(self):
        stage = self.work / 'stage'; stage.mkdir()
        model = self.work / 'model'; model.write_bytes(b'fixture-only')
        jars = []
        for name in ('engine-core.jar', 'app-uci.jar'):
            jar = self.work / name; jar.write_bytes(b'fixture-only'); jars.append(jar)
        context = {'platform': 'macos', 'model': str(model), 'modelSha256': p.sha(model), 'jdk': str(self.work / 'jdk'),
                   'version': {'major': 0, 'minor': 0, 'patch': 0, 'build': 35}}
        identity = {'model.sha256': p.sha(model), 'version': '0.0.0', 'build': '35'}
        commands = []
        def native(command, **kwargs):
            commands.append(command)
            if str(command[0]).endswith('jdeps'):
                return 'java.base'
            runtime = Path(command[-1]); (runtime / 'legal/java.base').mkdir(parents=True)
            (runtime / 'legal/java.base/LICENSE').write_text('fixture-only')
            return ''
        with patch.object(p, 'model_probe', return_value=identity), patch.object(p, 'run', side_effect=native):
            image, _ = p.build_image(context, 'uci', stage, jars)
        self.assertEqual(p.launcher_text(), (image / 'SeedV6-UCI').read_text())
        self.assertEqual('java.base', commands[1][commands[1].index('--add-modules') + 1])
        self.assertEqual({'engine-core.jar', 'app-uci.jar'}, {f.name for f in (image / 'app').glob('*.jar')})
        self.assertEqual(p.sha(model), p.sha(image / 'models/baseline.brn-pair2'))


if __name__ == '__main__':
    unittest.main()
