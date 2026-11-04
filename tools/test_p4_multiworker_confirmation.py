"""Prospective claim safeguards with synthetic/local development fixtures only."""
import copy
import hashlib
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('mw_confirmation_tested',
    Path(__file__).with_name('p4-multiworker-confirmation.py'))
c = importlib.util.module_from_spec(spec); spec.loader.exec_module(c)


class ConfirmationTest(unittest.TestCase):
    def setUp(self):
        import chess.pgn
        self.temp = tempfile.TemporaryDirectory(); self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        # These are development fixtures, never an actual confirmation attempt.
        source = Path(__file__).resolve().parents[1]/'docs/research/search/p4-multiworker/development-v1-openings.pgn'
        opening = self.root/'reserve-fixture.pgn'; opening.write_bytes(source.read_bytes())
        with opening.open(encoding='utf-8') as stream:
            keys = []
            while (game := chess.pgn.read_game(stream)) is not None:
                keys.append(' '.join(game.end().board().fen().split()[:4]))
        self.p = dict(kind='timed', mode='B', workers=4, baseline='ROOT_REUSE',
            candidate='EAGER_MEMORY', runtime='fixture-runtime', firstColorPolicy='alternate',
            openingIndices=list(range(32)), timeoutSeconds=600, tc='10+0.1', maxMoves=300,
            openings=opening.name, inputs={opening.name:hashlib.sha256(opening.read_bytes()).hexdigest()},
            confirmation=dict(status='prospectively-frozen', gate=copy.deepcopy(c.GATE),
                failureRule=c.FAILURE_RULE, interimScoreAccess='prohibited', resume='none-after-first-game',
                pairs=32, bootstrap=dict(samples=10000,seed=2026110417),
                reserveSelection='selection.json', freshnessAudit='freshness.json',
                developmentSelection='development.json', timingVarianceBasis='basis.json',
                qualificationEvidence=['technical.json'], selectionRationale='Fixture only',
                precisionAssessment='Fixture only'))
        self.write('selection.json',dict(count=len(keys),positionKeys=keys,
            outputSha256=self.p['inputs'][opening.name]))
        self.write('freshness.json',dict(status='passed',overlaps=0))
        self.write('development.json',dict(positionKeys=['fixture-disjoint']))
        self.write('basis-protocol.json',dict(tc='10+0.1',maxMoves=300))
        self.write('basis.json',dict(status='complete exploratory analysis',kind='timed',workers=4,
            candidate=self.p['candidate'],baseline=self.p['baseline'],runtime=self.p['runtime'],
            statistics=dict(pairs=32,pairedNormal95Score=[.4,.7],pairScoreStandardError=.05),
            wallSeconds=320,protocol='basis-protocol.json'))
        self.write('technical.json',dict(status='fixture'))
        self.ledger = dict(confirmation=dict(attemptsStarted=0,attemptsMaximum=1,pairsMaximum=480,
            protectedTimedGames=960,protectedTimedWallSeconds=86400,protocol=None))

    def write(self,name,value):
        path=self.root/name;path.write_text(json.dumps(value),encoding='utf-8')
        self.p['inputs'][name]=hashlib.sha256(path.read_bytes()).hexdigest()

    def test_matching_frozen_sample_and_measured_margin(self):
        result=c.validate(self.root,self.p,self.ledger)
        self.assertEqual(result['pairs'],32);self.assertEqual(result['minimumTimeoutSeconds'],520)
        self.p['timeoutSeconds']=519
        with self.assertRaisesRegex(ValueError,'throughput'):c.validate(self.root,self.p,self.ledger)

    def test_primary_gate_and_failure_boundary_cannot_drift(self):
        for key,value in [('gate',dict(c.GATE,minimumScore=.52)),('failureRule','drop-bad-games'),
                          ('resume','replay'),('interimScoreAccess','allowed'),('pairs',31)]:
            p=copy.deepcopy(self.p);p['confirmation'][key]=value
            with self.assertRaises(ValueError):c.validate(self.root,p,self.ledger)
        for key,value in [('kind','fixed'),('workers',2),('firstColorPolicy','baseline-first')]:
            p=copy.deepcopy(self.p);p[key]=value
            with self.assertRaises(ValueError):c.validate(self.root,p,self.ledger)

    def test_unspent_attempt_and_fixed_reserve_order(self):
        p=copy.deepcopy(self.p);p['openingIndices'].reverse()
        with self.assertRaises(ValueError):c.validate(self.root,p,self.ledger)
        self.ledger['confirmation']['attemptsStarted']=1
        with self.assertRaises(ValueError):c.validate(self.root,self.p,self.ledger)

    def test_freshness_and_hashed_evidence(self):
        original=(self.root/'freshness.json').read_bytes()
        (self.root/'freshness.json').write_text('{"status":"passed","overlaps":1}')
        with self.assertRaisesRegex(ValueError,'changed'):c.validate(self.root,self.p,self.ledger)
        (self.root/'freshness.json').write_bytes(original)
        selection=json.loads((self.root/'selection.json').read_text())
        self.write('development.json',dict(positionKeys=selection['positionKeys'][:1]))
        with self.assertRaisesRegex(ValueError,'overlap'):c.validate(self.root,self.p,self.ledger)

    def test_clock_and_candidate_must_match_throughput_basis(self):
        for key,value in [('tc','30+0.3'),('candidate','ROOT_MEMORY'),('runtime','other')]:
            p=copy.deepcopy(self.p);p[key]=value
            with self.assertRaises(ValueError):c.validate(self.root,p,self.ledger)

    def test_first_game_consumption_and_idempotent_reconciliation(self):
        before=copy.deepcopy(self.ledger)
        self.assertFalse(c.consume_attempt(self.ledger,'claim','abc',0,'now'))
        self.assertEqual(before,self.ledger)
        self.assertTrue(c.consume_attempt(self.ledger,'claim','abc',2,'now'))
        self.assertEqual(self.ledger['confirmation']['attemptsStarted'],1)
        self.assertFalse(c.consume_attempt(self.ledger,'claim','abc',2,'later'))
        self.assertEqual(self.ledger['confirmation']['firstInitiatedObservedUtc'],'now')
        with self.assertRaises(ValueError):c.consume_attempt(self.ledger,'replacement','def',2,'later')

    def test_gate_requires_both_prospective_conditions(self):
        # Explicit boundary inputs exercise the decision, not an invented sample.
        s=dict(pairs=32,pentanomial=[0,0,0,32,0],score=.53,pairedNormal95Score=[.500001,.56])
        self.assertEqual(c.assess(s)['primaryStatisticalGate'],'passed')
        for replacement in [dict(score=.52999),dict(pairedNormal95Score=[.5,.56]),
                            dict(pairedNormal95Score=None)]:
            self.assertEqual(c.assess(dict(s,**replacement))['primaryStatisticalGate'],'not-passed')


if __name__=='__main__':unittest.main()
