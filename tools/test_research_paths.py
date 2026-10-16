"""WU6 launch/path regression checks; no engine, campaign or training run."""
import importlib.util
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
import research_paths as paths


class ResearchPathsTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        spec = importlib.util.spec_from_file_location('wu6_baseline', Path(__file__).with_name('search-baseline.py'))
        cls.current = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(cls.current)
        source = subprocess.check_output(['git', 'show', '3301d457d6f6eb3dad9bc4c0f2c7659c1fa7fcaf:tools/search-baseline.py'], cwd=paths.ROOT)
        cls.historical = {'__name__': 'wu5_baseline', '__file__': str(Path(__file__).with_name('search-baseline.py'))}
        exec(compile(source, 'WU5-search-baseline.py', 'exec'), cls.historical)

    def test_frozen_installation_command_preserves_every_argument(self):
        arguments = ('java', 'frozen-installation', 'frozen-store', 'production', 'production',
                     'off', 'default', 'off', 'default', 'off', 'off')
        self.assertEqual(self.historical['command'](*arguments), self.current.command(*arguments))

    def test_explicit_workspace_changes_only_composition(self):
        folder = paths.ROOT / 'build/migration/research-path-tests'
        folder.mkdir(parents=True, exist_ok=True)
        with tempfile.TemporaryDirectory(dir=folder) as name:
            install = Path(name)
            old = self.current.command('java', install, 'store')
            (install / 'research-launcher.properties').write_text('mainClass=com.ohinteractive.seedv6.integration.ResearchUciMain\n')
            current = self.current.command('java', install, 'store')
            self.assertEqual(old[:-2], current[:-2])
            self.assertEqual('com.ohinteractive.seedv6.integration.ResearchUciMain', current[-1])
            self.assertEqual(os.pathsep.join([str(install/'classes'), str(install/'lib/*')]), current[-2])

    def test_runtime_and_receipt_owners_are_independent_of_app(self):
        self.assertEqual([str(paths.ROOT/'research/build/install/research/lib/*')], paths.runtime_paths())
        source = {p.relative_to(paths.ROOT).as_posix() for p in paths.source_files()}
        for name in ('engine-core/src/main/java/com/ohinteractive/seedv6/core/Board.java',
                     'engine-training/src/main/java/com/ohinteractive/seedv6/training/checkpoint/CheckpointStore.java',
                     'research/src/main/java/com/ohinteractive/seedv6/search/flat/FlatNegamax.java'):
            self.assertIn(name, source)
        self.assertTrue(all(p.split('/')[0] in paths.OWNERS for p in source))


if __name__ == '__main__':
    unittest.main(verbosity=2)
