"""Portable discovery/compatibility fixtures; no Java processes or downloads."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch

import packaging_jdk as j


class PackagingJdkTest(unittest.TestCase):
    def setUp(self):
        root = Path(__file__).resolve().parents[1] / 'build/packaging/tests'
        root.mkdir(parents=True, exist_ok=True)
        self.temp = tempfile.TemporaryDirectory(dir=root)
        self.work = Path(self.temp.name)

    def tearDown(self):
        self.temp.cleanup()

    def jdk(self, name, version='21.0.6', target='windows', arch=None):
        folder = self.work / name
        (folder / 'bin').mkdir(parents=True)
        (folder / 'jmods').mkdir()
        os_name = 'Windows' if target == 'windows' else 'Darwin'
        arch = arch or ('amd64' if target == 'windows' else 'aarch64')
        (folder / 'release').write_text(f'JAVA_VERSION="{version}"\nOS_NAME="{os_name}"\nOS_ARCH="{arch}"\n')
        for tool in ('java', 'javac', 'jpackage', 'jlink', 'jdeps'):
            (folder / 'bin' / (tool + ('.exe' if target == 'windows' else ''))).touch()
        for module in ('java.base', 'jdk.jpackage'):
            (folder / 'jmods' / (module + '.jmod')).touch()
        return folder

    def test_highest_compatible_patch_selected_independent_of_daemon(self):
        ga = self.jdk('daemon GA', '21')
        old = self.jdk('minimum', '21.0.6+7-LTS')
        newer = self.jdk('latest', '21.0.12.1')
        other = self.jdk('other major', '25')
        with patch.object(j, 'candidates', return_value=[newer, ga, other, old]):
            selected, _ = j.select('windows', gradle_jdk=str(ga))
        self.assertEqual(newer, selected)

    def test_bad_explicit_jdk_never_falls_back(self):
        ga = self.jdk('GA', '21')
        with patch.object(j, 'candidates') as discover:
            with self.assertRaisesRegex(RuntimeError, 'Explicit packaging JDK.*21.0.6'):
                j.select('windows', explicit=str(ga))
        discover.assert_not_called()

    def test_valid_explicit_jdk_skips_discovery(self):
        valid = self.jdk('valid with spaces')
        with patch.object(j, 'candidates') as discover:
            self.assertEqual(valid, j.select('windows', explicit=str(valid))[0])
        discover.assert_not_called()

    def test_no_compatible_installation_has_actionable_diagnostic(self):
        for paths in ([], [self.jdk('old', '21.0.5')]):
            with patch.object(j, 'candidates', return_value=paths):
                with self.assertRaisesRegex(RuntimeError, 'No compatible installed.*21.0.6.*windowsPackagingJdk'):
                    j.select('windows')

    def test_rejects_other_major_ga_old_patch_early_access_on_both_platforms(self):
        for target in ('windows', 'macos'):
            for version in ('17.0.12', '21', '21.0.5', '22.0.2', '25', '21.0.7-ea', 'invalid'):
                with self.subTest(target=target, version=version):
                    with self.assertRaisesRegex(RuntimeError, '21.0.6'):
                        j.validate(self.jdk(target + version, version, target), target)

    def test_wrong_os_architecture_and_incomplete_jdk_fail(self):
        with self.assertRaisesRegex(RuntimeError, 'architecture'):
            j.validate(self.jdk('arm windows', arch='aarch64'), 'windows')
        with self.assertRaisesRegex(RuntimeError, 'OS'):
            j.validate(self.jdk('wrong os', target='macos', arch='amd64'), 'windows')
        for absent in ('bin/jpackage.exe', 'bin/javac.exe', 'jmods/java.base.jmod', 'jmods/jdk.jpackage.jmod'):
            folder = self.jdk(absent.replace('/', '-'))
            (folder / absent).unlink()
            with self.assertRaisesRegex(RuntimeError, 'Missing JDK tool|Incomplete JDK'):
                j.validate(folder, 'windows')

    def test_mac_bundle_and_home_supported(self):
        folder = self.jdk('Temurin.jdk/Contents/Home', target='macos')
        self.assertEqual(folder, j.home(folder.parent.parent))
        self.assertEqual(folder, j.select('macos', explicit=str(folder.parent.parent))[0])

    def test_mac_external_dylib_dependency_rejected(self):
        folder = self.jdk('mac', target='macos')
        (folder / 'lib').mkdir(); (folder / 'lib/libfont.dylib').touch()
        result = subprocess.CompletedProcess([], 0, 'self\ninstall-id\n /opt/homebrew/lib/external.dylib (compatibility version 1)\n', '')
        with patch.object(j.subprocess, 'run', return_value=result):
            with self.assertRaisesRegex(RuntimeError, 'external library'):
                j.validate(folder, 'macos')

    def test_discovery_includes_conventional_windows_root_and_gradle_paths(self):
        folder = self.jdk('java/installed')
        extra = self.jdk('unusual custom')
        with patch.dict(os.environ, {'SystemDrive': self.work.as_posix(), 'JAVA_HOME': str(extra)}), \
             patch.object(j, 'registry_homes', return_value=[]):
            found = j.candidates('windows', installation_paths=str(extra))
        self.assertIn(folder, found)
        self.assertIn(extra, found)

    def test_mac_java_home_discovery_with_spaces(self):
        folder = '/Library/Java/JavaVirtualMachines/Mac Vendor.jdk/Contents/Home'
        result = subprocess.CompletedProcess([], 0, '', '21.0.6 (arm64) "Vendor" ' + folder + '\n')
        # Real java_home emits POSIX absolute paths. This test also runs on Windows.
        with patch.object(j.subprocess, 'run', return_value=result):
            found = j.candidates('macos')
        self.assertIn(j.home(folder), found)


if __name__ == '__main__':
    unittest.main()
