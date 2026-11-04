"""Small admission/deadline tests; no competitive engine or campaign resources."""
import datetime as dt
import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import time
import unittest
from unittest.mock import patch

spec=importlib.util.spec_from_file_location('mw_match_tested',Path(__file__).with_name('p4-multiworker-match.py'))
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)

class MatchTest(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup)
        self.root=Path(self.temp.name);self.out=self.root/'study';self.out.mkdir();(self.out/'raw').mkdir()
        for name,value in [('ROOT',self.root),('OUT',self.out)]:
            p=patch.object(m,name,value);p.start();self.addCleanup(p.stop)
        self.now=dt.datetime.now(dt.timezone.utc)
        self.window=dict(status='authorized',receivedUtc=(self.now-dt.timedelta(seconds=5)).isoformat(),
                         hardDeadlineUtc=(self.now+dt.timedelta(seconds=3600)).isoformat())
        (self.root/'window.json').write_text(json.dumps(self.window))
        self.p=dict(id='fixture',kind='fixed',mode='A',category='exploration',workers=4,
            openingIndices=[0],timeoutSeconds=1,maxMoves=200,nodesPerMove=100,window='window.json',
            resources=dict(estimatedPeakGiB=2,personalReserveGiB=2),inputs={})
        self.ledger=dict(pending=[],ownedProcesses=[],receipts=[],confirmation=dict(protectedTimedGames=960,protectedTimedWallSeconds=86400),
            ceilings=dict(timedGames=3000,fixedGames=6000,fixedNodes=100000000000,timedWallSeconds=259200),
            consumed=dict(fixedInitiated=0,fixedCompleted=0,fixedFailed=0,fixedInterrupted=0,fixedWallSeconds=0,fixedNodes=0,
                          timedInitiated=0,timedCompleted=0,timedFailed=0,timedInterrupted=0,timedWallSeconds=0))

    def test_deadline_authority_does_not_restart_and_protects_confirmation(self):
        p=dict(self.p,mode='B',kind='timed',timeoutSeconds=300)
        self.assertEqual(m.admission(p,self.ledger,self.now)[0],0)
        with self.assertRaises(ValueError):m.admission(p,self.ledger,self.now+dt.timedelta(seconds=3400))
        self.ledger['consumed']['timedInitiated']=2040
        with self.assertRaises(ValueError):m.admission(p,self.ledger,self.now)
        with self.assertRaises(ValueError):m.admission(dict(p,mode='A'),self.ledger,self.now)

    def test_fixed_work_node_reservation_and_pending_ownership(self):
        self.assertEqual(m.admission(self.p,self.ledger,self.now)[0],80000)
        self.ledger['pending']=[dict(id='old')]
        with self.assertRaises(ValueError):m.admission(self.p,self.ledger,self.now)

    def test_idle_resource_envelope_is_explicit_without_weakening_daytime(self):
        sample=dict(totalCpuPercent=45,freeMemoryGiB=10,logicalProcessors=12,overwatch=False,demandingForeground=False)
        self.assertFalse(m.mw.resource_decision(sample,4,self.p['resources'])['admissible'])
        self.assertTrue(m.mw.resource_decision(sample,4,self.p['resources'],idle=True)['admissible'])
        sample['demandingForeground']=True
        self.assertFalse(m.mw.resource_decision(sample,4,self.p['resources'],idle=True)['admissible'])

    def test_reversed_pair_preserves_real_history_legality_and_exact_color_binding(self):
        directory=self.out/'fixture';directory.mkdir()
        (directory/'opening.pgn').write_text(str(m.b.chess.pgn.Game())+'\n')
        games=[];trace=[]
        moves=['g1f3','g8f6','f3g1','f6g8']*2
        for white in ('candidate','baseline'):
            game=m.b.chess.pgn.Game();game.headers.update(White=white,
                Black='baseline' if white=='candidate' else 'candidate',Result='1/2-1/2',Termination='normal')
            node=game;played=[]
            for ply,move in enumerate(moves):
                actor=game.headers['White' if ply%2==0 else 'Black']
                prefix=f'[Engine] fixture {actor} '
                trace.extend([prefix+'<--- position startpos'+(' moves '+' '.join(played) if played else ''),
                              prefix+'<--- go nodes 100',
                              prefix+'---> info depth 1 score cp 0 time 0 nodes 1 pv '+move,
                              prefix+'---> info string request nodes=1 elapsedNanos=1 termination=COMPLETED',
                              prefix+'---> bestmove '+move])
                node=node.add_main_variation(m.b.chess.Move.from_uci(move))
                node.comment='info string request nodes=1 elapsedNanos=1 termination=COMPLETED'
                played.append(move)
            games.append(str(game))
        (directory/'games.pgn').write_text('\n\n'.join(games)+'\n')
        (directory/'uci-trace.log').write_text('\n'.join(trace)+'\n')
        result=m.audit_pair(directory,self.p,reverse=True)
        self.assertEqual(result['nodes'],16);self.assertEqual(result['results']['wdl'],[0,2,0])
        self.assertEqual(result['firstGameWhite'],'candidate')
        with self.assertRaises(ValueError):m.audit_pair(directory,self.p,reverse=False)
        (directory/'games.pgn').write_text(games[0]+'\n\n'+games[0]+'\n')
        with self.assertRaises(ValueError):m.audit_pair(directory,self.p,reverse=True)

    def test_reversed_decisive_games_score_the_named_candidate_in_both_colors(self):
        directory=self.out/'mates';directory.mkdir()
        (directory/'opening.pgn').write_text(str(m.b.chess.pgn.Game())+'\n')
        games=[];trace=[]
        # Candidate wins as White first, then Black. A color/index inversion cannot
        # hide behind a paired1-1 or two draws in this fixture.
        for white,moves,result in [
            ('candidate',['e2e4','e7e5','f1c4','b8c6','d1h5','g8f6','h5f7'],'1-0'),
            ('baseline',['f2f3','e7e5','g2g4','d8h4'],'0-1')]:
            game=m.b.chess.pgn.Game();game.headers.update(White=white,
                Black='baseline' if white=='candidate' else 'candidate',Result=result,Termination='normal')
            node=game;played=[]
            for ply,move in enumerate(moves):
                actor=game.headers['White' if ply%2==0 else 'Black'];prefix=f'[Engine] fixture {actor} '
                trace.extend([prefix+'<--- position startpos'+(' moves '+' '.join(played) if played else ''),
                              prefix+'<--- go nodes 100',prefix+'---> info depth 1 score cp 0 time 0 nodes 1 pv '+move,
                              prefix+'---> info string request nodes=1 elapsedNanos=1 termination=COMPLETED',
                              prefix+'---> bestmove '+move])
                node=node.add_main_variation(m.b.chess.Move.from_uci(move))
                node.comment='info string request nodes=1 elapsedNanos=1 termination=COMPLETED';played.append(move)
            self.assertTrue(game.end().board().is_checkmate());games.append(str(game))
        (directory/'games.pgn').write_text('\n\n'.join(games)+'\n')
        (directory/'uci-trace.log').write_text('\n'.join(trace)+'\n')
        result=m.audit_pair(directory,self.p,reverse=True)
        self.assertEqual(result['results']['wdl'],[2,0,0]);self.assertEqual(result['results']['statistics']['score'],1)
        self.assertEqual(result['nodes'],11)

    def test_native_deadline_drains_and_conservatively_accounts_partial_pair(self):
        path=self.root/'protocol.json';path.write_text(json.dumps(self.p));m.mw.save(self.out/'budget.json',self.ledger)
        original=subprocess.Popen
        def child(command,**kwargs):
            directory=command[command.index('--directory')+1]
            script='import pathlib,json,sys,time; p=pathlib.Path(sys.argv[1])/"progress.jsonl"; p.write_text(json.dumps(dict(type="pair-start",pair=0,initiatedGames=2))+"\\n"); time.sleep(30)'
            return original([sys.executable,'-B','-c',script,directory],**kwargs)
        sample=dict(totalCpuPercent=5,freeMemoryGiB=16,logicalProcessors=12,overwatch=False,demandingForeground=False)
        before=time.monotonic()
        with patch.object(m.subprocess,'Popen',side_effect=child),patch.object(m.mw.PcUse,'sample',return_value=sample):
            with self.assertRaises(SystemExit):m.supervise(path)
        self.assertLess(time.monotonic()-before,8)
        receipt=json.loads((self.out/'fixture-execution.json').read_text())
        self.assertEqual(receipt['status'],'interrupted');self.assertEqual(receipt['initiatedGames'],2)
        self.assertEqual(receipt['chargedNodes'],80000);self.assertEqual(receipt['drain']['ownedProcessesRemaining'],0)
        ledger=json.loads((self.out/'budget.json').read_text());self.assertEqual(ledger['pending'],[])
        self.assertEqual(ledger['consumed']['fixedInterrupted'],2)
        with self.assertRaises(FileExistsError):m.supervise(path)

    def test_confirmation_native_first_game_consumes_claim_and_drains_partial_attempt(self):
        p=dict(self.p,category='confirmation',kind='timed',mode='B',openingIndices=list(range(32)))
        self.ledger['confirmation'].update(attemptsStarted=0,attemptsMaximum=1,pairsMaximum=480,protocol=None)
        path=self.root/'claim.json';path.write_text(json.dumps(p));m.mw.save(self.out/'budget.json',self.ledger)
        original=subprocess.Popen
        def child(command,**kwargs):
            directory=command[command.index('--directory')+1]
            script='import pathlib,json,sys,time; p=pathlib.Path(sys.argv[1])/"progress.jsonl"; p.write_text(json.dumps(dict(type="pair-start",pair=0,initiatedGames=2))+"\\n"); time.sleep(30)'
            return original([sys.executable,'-B','-c',script,directory],**kwargs)
        sample=dict(totalCpuPercent=5,freeMemoryGiB=16,logicalProcessors=12,overwatch=False,demandingForeground=False)
        # Separate validator tests exercise actual frozen metadata. This fixture
        # exercises native ownership/accounting without playing reserve positions.
        with patch.object(m.confirmation,'validate'),patch.object(m.subprocess,'Popen',side_effect=child), \
                patch.object(m.mw.PcUse,'sample',return_value=sample):
            with self.assertRaises(SystemExit):m.supervise(path)
        receipt=json.loads((self.out/'fixture-execution.json').read_text())
        ledger=json.loads((self.out/'budget.json').read_text())
        self.assertEqual(receipt['status'],'interrupted');self.assertEqual(receipt['initiatedGames'],2)
        self.assertEqual(receipt['drain']['ownedProcessesRemaining'],0);self.assertEqual(ledger['pending'],[])
        self.assertEqual(ledger['confirmation']['attemptsStarted'],1)
        self.assertEqual(ledger['confirmation']['protocolSha256'],m.b.digest(path))
        self.assertEqual(ledger['consumed']['timedInterrupted'],2);self.assertGreater(ledger['consumed']['timedWallSeconds'],0)

    def test_confirmation_preflight_block_does_not_spend_uninitiated_attempt(self):
        p=dict(self.p,category='confirmation',kind='timed',mode='B',openingIndices=list(range(32)))
        self.ledger['confirmation'].update(attemptsStarted=0,attemptsMaximum=1,pairsMaximum=480,protocol=None)
        path=self.root/'claim.json';path.write_text(json.dumps(p));m.mw.save(self.out/'budget.json',self.ledger)
        sample=dict(totalCpuPercent=95,freeMemoryGiB=1,logicalProcessors=12,overwatch=False,demandingForeground=False)
        with patch.object(m.confirmation,'validate'),patch.object(m.mw.PcUse,'sample',return_value=sample):
            with self.assertRaises(SystemExit):m.supervise(path)
        receipt=json.loads((self.out/'fixture-execution.json').read_text())
        ledger=json.loads((self.out/'budget.json').read_text())
        self.assertEqual(receipt['initiatedGames'],0);self.assertEqual(receipt['drain']['ownedProcessesRemaining'],0)
        self.assertEqual(ledger['confirmation']['attemptsStarted'],0);self.assertEqual(ledger['consumed']['timedWallSeconds'],0)
        self.assertEqual(ledger['pending'],[])


if __name__=='__main__':unittest.main()
