"""Exercise the real Gradle boundary rules in small, disposable build fixtures.

Run from the migration worktree with Python. Everything, including Gradle's user
home, stays under build/migration; no application source or distributions copied.
"""
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest
from unittest.mock import patch


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / 'build/migration/architecture-tests'
MODULES = ('engine-core', 'engine-training', 'app-gui', 'app-uci', 'research')


class GradleArchitectureTest(unittest.TestCase):
    def setUp(self):
        OUTPUT.mkdir(parents=True, exist_ok=True)
        self.fixture = Path(tempfile.mkdtemp(prefix=self._testMethodName + '-', dir=OUTPUT))
        for name in ('settings.gradle', 'build.gradle', 'gradle.properties',
                     'gradle/java-conventions.gradle', 'gradle/project-boundaries.gradle',
                     'gradle/application-version.gradle', 'VERSION_STATE.txt',
                     'gradle/libs.versions.toml', 'gradle/integration.gradle', 'gradle/research-launcher.properties',
                     'gradle/package-mac.gradle', 'gradle/native-packaging.gradle', 'gradle/syzygy.gradle',
                     *(module + '/build.gradle' for module in MODULES)):
            target = self.fixture / name
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(ROOT / name, target)
        (self.fixture / 'app').mkdir()
        (self.fixture / 'app/build.gradle').write_text(
            "plugins { id 'java' }\napply from: rootProject.file('gradle/java-conventions.gradle')\n")
        self.invocations = 0

    def append(self, path, text):
        with (self.fixture / path).open('a', encoding='utf-8') as stream:
            stream.write('\n' + text + '\n')

    def gradle(self, *arguments, failure=None):
        wrapper = ROOT / ('gradlew.bat' if os.name == 'nt' else 'gradlew')
        env = os.environ.copy()
        env['GRADLE_USER_HOME'] = str(ROOT / 'build/migration/gradle-user-home')
        env['GIT_CEILING_DIRECTORIES'] = str(ROOT)
        env['TEMP'] = env['TMP'] = str(self.fixture)
        result = subprocess.run([str(wrapper), '-p', str(self.fixture), '--no-daemon',
                                 '--max-workers=2', '--console=plain', *arguments],
                                cwd=ROOT, env=env, stdout=subprocess.PIPE,
                                stderr=subprocess.STDOUT, text=True, timeout=120)
        self.invocations += 1
        (self.fixture / f'invocation-{self.invocations}.txt').write_text(result.stdout, encoding='utf-8')
        if failure:
            self.assertNotEqual(0, result.returncode, result.stdout)
            self.assertIn(failure, result.stdout)
        else:
            self.assertEqual(0, result.returncode, result.stdout)
        return result.stdout

    def test_target_graph_and_configuration_cache(self):
        tasks = ('verifyProjectBoundaries', *(f':{m}:classes' for m in MODULES))
        self.gradle(*tasks)
        cached = self.gradle(*tasks)
        self.assertIn('Reusing configuration cache.', cached)
        self.assertIn(':app-uci -> [:engine-core]', cached)
        self.assertIn(':app -> (none)', cached)

    def test_core_cannot_depend_on_training_even_for_tests(self):
        self.append('engine-core/build.gradle', "dependencies { testImplementation project(':engine-training') }")
        self.gradle(':engine-core:compileJava', failure='Forbidden project dependency: :engine-core -> :engine-training')

    def test_packaging_selection_is_portable_and_cache_tracked(self):
        self.append('build.gradle', """
tasks.register('packagingSelection') {
    inputs.property('windows', tasks.named('prepareWindowsPackaging').get().jdkPath)
    inputs.property('mac', tasks.named('prepareMacPackaging').get().jdkPath)
    inputs.property('model', tasks.named('prepareWindowsPackaging').get().modelPath)
    inputs.property('daemon', System.getProperty('java.home'))
    doLast {
        ['windows', 'mac', 'model', 'daemon'].each { key -> println key + '=' + inputs.properties[key] }
    }
}
""")

        def selection(*arguments):
            output = self.gradle('packagingSelection', *arguments)
            values = dict(line.split('=', 1) for line in output.splitlines()
                          if line.startswith(('windows=', 'mac=', 'model=', 'daemon=')))
            return values, output

        with patch.dict(os.environ, {'SEEDV6_PRODUCTION_MODEL': ''}):
            # Gradle environment providers preserve an explicitly empty value;
            # ordinary default validation below therefore uses an absent variable.
            os.environ.pop('SEEDV6_PRODUCTION_MODEL')
            baseline, _ = selection()
        self.assertEqual('', baseline['windows'])
        self.assertEqual('', baseline['mac'])
        self.assertEqual(self.fixture / 'build-assets/production/bre-pair2-g32.brn-pair2', Path(baseline['model']))
        local = self.fixture / 'build-assets/packaging.properties'
        local.parent.mkdir()
        first = self.fixture / 'packaging JDK one'
        local.write_text('windowsPackagingJdk=' + first.as_posix() + '\nmacPackagingJdk=' + first.as_posix() + '\n')
        values, _ = selection()
        self.assertEqual(first, Path(values['windows']))
        self.assertEqual(first, Path(values['mac']))
        self.assertEqual(baseline['daemon'], values['daemon'])
        _, cached = selection()
        self.assertIn('Reusing configuration cache.', cached)
        second = self.fixture / 'packaging JDK two'
        local.write_text('windowsPackagingJdk=' + second.as_posix() + '\n')
        values, _ = selection()
        self.assertEqual(second, Path(values['windows']))
        explicit = self.fixture / 'explicit JDK'
        values, _ = selection('-PwindowsPackagingJdk=' + str(explicit))
        self.assertEqual(explicit, Path(values['windows']))
        values, _ = selection('-PpackagingJdk=' + str(explicit))
        self.assertEqual(explicit, Path(values['windows']))
        self.assertEqual(explicit, Path(values['mac']))
        values, _ = selection('-PpackagingJdk=' + str(explicit), '-PmacPackagingJdk=' + str(first))
        self.assertEqual(explicit, Path(values['windows']))
        self.assertEqual(first, Path(values['mac']))
        with patch.dict(os.environ, {'SEEDV6_PRODUCTION_MODEL': 'environment-model'}):
            values, _ = selection()
            self.assertEqual('environment-model', values['model'])
            values, _ = selection('-PproductionModel=explicit-model')
            self.assertEqual('explicit-model', values['model'])
        local.unlink()
        values, _ = selection()
        self.assertEqual(baseline, values)

    def test_uci_cannot_depend_on_training_or_gui(self):
        path = self.fixture / 'app-uci/build.gradle'
        original = path.read_text()
        for target in ('engine-training', 'app-gui', 'research', 'app'):
            with self.subTest(target=target):
                path.write_text(original)
                self.append('app-uci/build.gradle', f"dependencies {{ runtimeOnly project(':{target}') }}")
                self.gradle('help', failure=f'Forbidden project dependency: :app-uci -> :{target}')

    def test_custom_and_late_dependencies_cannot_bypass_rules(self):
        self.append('engine-training/build.gradle', """
configurations { auditOnly }
gradle.projectsEvaluated {
    dependencies { auditOnly project(':research') }
}
""")
        self.gradle('help', failure='Forbidden project dependency: :engine-training -> :research')

    def test_target_cannot_consume_legacy_application(self):
        self.append('app-gui/build.gradle', "dependencies { implementation project(':app') }")
        self.gradle('check', failure='Forbidden project dependency: :app-gui -> :app')

    def test_legacy_can_consume_extracted_core_and_training(self):
        self.append('app/build.gradle', """
dependencies {
    implementation project(':engine-core')
    implementation project(':engine-training')
}
""")
        output = self.gradle('verifyProjectBoundaries', ':app:classes')
        self.assertIn(':app -> [:engine-core, :engine-training]', output)

    def test_legacy_can_delegate_to_gui_but_training_cannot(self):
        self.append('app/build.gradle', "dependencies { implementation project(':app-gui') }")
        self.gradle('verifyProjectBoundaries', ':app:classes')
        self.append('engine-training/build.gradle', "dependencies { implementation project(':app-gui') }")
        self.gradle('help', failure='Forbidden project dependency: :engine-training -> :app-gui')

    def test_legacy_can_delegate_to_uci(self):
        self.append('app/build.gradle', "dependencies { implementation project(':app-uci') }")
        output = self.gradle('verifyProjectBoundaries', ':app:classes')
        self.assertIn(':app -> [:app-uci]', output)
        self.assertIn(':app-uci -> [:engine-core]', output)

    def test_gui_cannot_consume_uci_or_research(self):
        path = self.fixture / 'app-gui/build.gradle'
        original = path.read_text()
        for target in ('app-uci', 'research'):
            with self.subTest(target=target):
                path.write_text(original)
                self.append('app-gui/build.gradle', f"dependencies {{ implementation project(':{target}') }}")
                self.gradle('help', failure=f'Forbidden project dependency: :app-gui -> :{target}')

    def test_research_cannot_depend_on_applications(self):
        path = self.fixture / 'research/build.gradle'
        original = path.read_text()
        for target in ('app', 'app-gui', 'app-uci'):
            with self.subTest(target=target):
                path.write_text(original)
                self.append('research/build.gradle', f"dependencies {{ testImplementation project(':{target}') }}")
                self.gradle('help', failure=f'Forbidden project dependency: :research -> :{target}')

    def test_root_composition_cannot_become_production(self):
        self.append('build.gradle', "sourceSets { main }")
        self.gradle('help', failure='Root source sets are verification/research composition only')

    def test_root_custom_configuration_cannot_bypass_scope(self):
        self.append('build.gradle', "configurations { shipped }; dependencies { shipped project(':research') }")
        self.gradle('help', failure='Forbidden root composition dependency: shipped')

    def test_cycles_are_rejected(self):
        # training -> core already exists; adding the reverse creates a cycle.
        self.append('engine-core/build.gradle', "dependencies { implementation project(':engine-training') }")
        self.gradle('verifyProjectBoundaries', failure='Forbidden project dependency: :engine-core -> :engine-training')

    def test_overlapping_source_roots_are_rejected(self):
        self.append('engine-core/build.gradle', "sourceSets.main.java.srcDir rootProject.file('app/src/main/java')")
        self.gradle(':app:classes', failure='Overlapping Java source ownership:')

    def test_unreviewed_project_identity_is_rejected(self):
        self.append('settings.gradle', "include('extra-runtime')")
        (self.fixture / 'extra-runtime').mkdir()
        self.gradle('help', failure='Unexpected project identities:')


if __name__ == '__main__':
    unittest.main(verbosity=2)
