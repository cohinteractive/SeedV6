import importlib.util
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('resource_probe', Path(__file__).with_name('brn_process_resource.py'))
probe = importlib.util.module_from_spec(spec)
spec.loader.exec_module(probe)


class OwnedProcessTests(unittest.TestCase):
    def test_complete_records_a_real_allocation(self):
        with tempfile.TemporaryFile() as output:
            result = probe.execute([sys.executable, '-B', '-c',
                                    'import time; allocation=bytearray(32*1024*1024); time.sleep(1.2)'], output, 10)
        self.assertEqual('completed', result['status'])
        self.assertEqual(0, result['exitCode'])
        if os.name == 'nt':
            self.assertGreaterEqual(result['processMemory']['peakWorkingSetBytes'], 32*1024*1024)
            self.assertGreaterEqual(result['processMemory']['peakPrivateCommitBytes'], 32*1024*1024)

    def test_timeout_terminates_only_its_child(self):
        with subprocess.Popen([sys.executable, '-B', '-c', 'import time; time.sleep(10)']) as separate:
            try:
                with tempfile.TemporaryFile() as output:
                    result = probe.execute([sys.executable, '-B', '-c', 'import time; time.sleep(10)'], output, .15)
                self.assertEqual('timeout', result['status'])
                self.assertIsNotNone(result['exitCode'])
                self.assertIsNone(separate.poll())
            finally:
                separate.terminate()
                separate.wait()


if __name__ == '__main__':
    unittest.main()
