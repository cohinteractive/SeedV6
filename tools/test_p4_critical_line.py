import copy
import importlib.util
import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('clv', ROOT / 'tools/p4-critical-line.py')
clv = importlib.util.module_from_spec(spec)
spec.loader.exec_module(clv)


class AdmissionTest(unittest.TestCase):
    def setUp(self):
        self.ledger = json.loads((clv.execution.OUT / 'budget.json').read_text())
        self.ledger['pending'] = []
        self.ledger['ownedProcesses'] = []
        self.ledger['authorizedIdleWindows'] = []
        self.ledger['consumed'] = {k: 0 for k in self.ledger['consumed']}
        self.protocol = dict(campaign='CLV-R001', kind='fixed-work', games=0, workers=4,
                             timeoutSeconds=120, maximumNodes=10000, requests=2)

    def test_fixed_work_requires_no_timed_window(self):
        clv.validate(self.protocol, self.ledger)

    def test_timed_without_fresh_owner_window_is_rejected(self):
        self.protocol.update(kind='timed', games=4, openingIndices=[0, 1], category='exploration', window='missing')
        with self.assertRaisesRegex(ValueError, 'idle-window'):
            clv.validate(self.protocol, self.ledger)

    def test_pending_and_exhausted_resources_are_not_recycled(self):
        self.ledger['pending'] = [{'id': 'unfinished'}]
        with self.assertRaisesRegex(ValueError, 'ownership'):
            clv.validate(self.protocol, self.ledger)
        self.ledger['pending'] = []
        self.ledger['consumed']['chargedWallSeconds'] = 86300
        with self.assertRaisesRegex(ValueError, 'allowance'):
            clv.validate(self.protocol, self.ledger)

    def test_closed_campaign_and_confirmation_reserve_cannot_be_used(self):
        self.protocol['campaign'] = 'IC-R001'
        with self.assertRaisesRegex(ValueError, 'campaign'):
            clv.validate(self.protocol, self.ledger)
        self.protocol.update(campaign='CLV-R001', kind='timed', games=4, openingIndices=[0, 1])
        self.ledger['consumed']['initiatedTimedGames'] = 638
        with self.assertRaisesRegex(ValueError, 'Development game'):
            clv.validate(self.protocol, self.ledger)
        self.ledger['consumed']['initiatedTimedGames'] = 0
        self.ledger['consumed']['confirmationAttempts'] = 1
        with self.assertRaisesRegex(ValueError, 'after independent'):
            clv.validate(self.protocol, self.ledger)


if __name__ == '__main__':
    unittest.main()
