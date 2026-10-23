"""Pair accounting, immutable continuation and real Windows process fault tests."""
import importlib.util
import json
from pathlib import Path
from datetime import datetime, timedelta, timezone
import subprocess
import sys
import tempfile
import time
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('session', Path(__file__).with_name('p3-tt-session.py'))
s = importlib.util.module_from_spec(spec); spec.loader.exec_module(s)
b = s.module('baseline', Path(__file__).with_name('search-baseline.py'))
p = s.module('processes', Path(__file__).with_name('tournament-process.py'))


class SessionTests(unittest.TestCase):
    def test_adapter_is_explicit_bounded_cp64_only(self):
        self.assertEqual('p3-tt-match.py', s.adapter_name({}))
        self.assertEqual('p3-tt-match.py', s.adapter_name({'adapter':'p3-tt-match.py'}))
        with self.assertRaisesRegex(ValueError, 'Unsupported'):
            s.adapter_name({'adapter':'../sr033-match.py'})

    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory(prefix='seed-session-')
        self.addCleanup(self.tmp.cleanup); self.root = Path(self.tmp.name).resolve()
        self.plan_path = self.root/'plan.json'
        self.plan = {'arguments': {'runner': str(Path(__file__).resolve().parent.parent/'fastchess.exe'),
                                 'concurrency': 1}, 'blocks': []}
        sequences = [('f3 e5 g4 Qh4#', 2), ('e4 e5 Bc4 Nc6 Qh5 Nf6 Qxf7#', 2)]
        self.games = []
        for index, (moves, prefix_count) in enumerate(sequences):
            game = b.chess.pgn.Game(); node = game
            for san in moves.split():
                node = node.add_main_variation(node.board().parse_san(san))
                node.comment = 'info string request nodes=1 elapsedNanos=1 termination=COMPLETED'
            opening = b.chess.pgn.Game(); prefix = opening
            for move in list(game.mainline_moves())[:prefix_count]: prefix = prefix.add_main_variation(move)
            book = self.root/f'opening-{index}.pgn'; book.write_text(str(opening)+'\n')
            self.plan['blocks'].append({'index': index, 'pairs': 1, 'openings': str(book), 'pairIds': [f'pair{index}']})
            game.headers.update(Result=game.end().board().result(), Termination='normal')
            self.games.append(game)
        s.save(self.plan_path, self.plan)

    def block(self, index):
        block = self.plan['blocks'][index]; folder = self.root/f'block-{index:04d}'; folder.mkdir()
        games = []
        for white, black in [('baseline','candidate'), ('candidate','baseline')]:
            self.games[index].headers.update(White=white, Black=black)
            games.append(str(self.games[index]))
        (folder/'games.pgn').write_text('\n\n'.join(games)+'\n')
        audit = b.analyze(folder/'games.pgn', block['openings'], 1, True, True, statistics_enabled=False)
        s.save(folder/'run.json', {'status':'passed', 'arguments':dict(self.plan['arguments'], pairs=1,
               openings=block['openings'], out=str(folder)), 'runnerExitCode':0, 'inputsStable':True,
               'results':audit, 'matchWallSeconds':20, 'wallSeconds':23})
        s.save(folder/'process-cost.json', {'status':'passed', 'processes':[
            {'lifetime':{'exitCode':0}}, {'lifetime':{'exitCode':0}}], 'cpuSeconds':{'baseline':10,'candidate':10}})
        s.save(self.root/f'block-{index:04d}.started.json', {'planSha256':s.digest(self.plan_path),'block':index})
        return folder

    def test_complete_block_recovered_once_then_resume_two(self):
        self.block(0)
        with patch.object(b, 'paired_statistics', side_effect=AssertionError('unplanned look')):
            first=s.reconcile(self.plan_path,self.plan)
            self.assertEqual(1,len(first))
            receipt=(self.root/'block-0000.json').read_bytes()
            self.assertEqual(first,s.reconcile(self.plan_path,self.plan))
            self.assertEqual(receipt,(self.root/'block-0000.json').read_bytes())
            self.block(1); result=s.reconcile(self.plan_path,self.plan)
        self.assertEqual(['pair0','pair1'],[x['pairIds'][0] for x in result])
        self.assertEqual(4,sum(x['games'] for x in result))
        self.assertTrue(all(x['statisticsAssessed'] is False for x in result))

    def test_partial_game_block_never_replayed(self):
        folder=self.block(0)
        with (folder/'games.pgn').open() as stream: game=b.chess.pgn.read_game(stream)
        (folder/'games.pgn').write_text(str(game)+'\n')
        with self.assertRaisesRegex(ValueError,'Missing or excess'):s.reconcile(self.plan_path,self.plan)
        self.assertFalse((self.root/'block-0000.json').exists())

    def test_duplicate_color_rejected(self):
        folder=self.block(0); text=(folder/'games.pgn').read_text()
        (folder/'games.pgn').write_text(text.replace('[White "candidate"]','[White "baseline"]'))
        with self.assertRaisesRegex(ValueError,'Duplicate'):s.reconcile(self.plan_path,self.plan)

    def test_changed_completed_evidence_rejected(self):
        folder=self.block(0);s.reconcile(self.plan_path,self.plan)
        (folder/'late-output.txt').write_text('new')
        with self.assertRaisesRegex(ValueError,'evidence changed'):s.reconcile(self.plan_path,self.plan)

    def test_noncontiguous_output_rejected(self):
        self.block(1)
        with self.assertRaisesRegex(ValueError,'Noncontiguous'):s.reconcile(self.plan_path,self.plan)

    def test_failed_and_ambiguous_launch_block(self):
        s.save(self.root/'block-0000.started.json', {'planSha256':s.digest(self.plan_path),'block':0})
        with self.assertRaisesRegex(ValueError,'incomplete launch'):s.reconcile(self.plan_path,self.plan)
        folder=self.block(0);record=s.read(folder/'run.json');record['status']='failed';s.save(folder/'run.json',record)
        with self.assertRaisesRegex(ValueError,'did not complete'):s.reconcile(self.plan_path,self.plan)

    def test_abnormal_exit_not_published(self):
        folder=self.block(0);record=s.read(folder/'process-cost.json');record['processes'][0]['lifetime']['exitCode']=1
        s.save(folder/'process-cost.json',record)
        with self.assertRaisesRegex(ValueError,'lifecycle'):s.reconcile(self.plan_path,self.plan)

    def test_unknown_opening_rejected(self):
        folder=self.block(0)
        (folder/'games.pgn').write_text(str(self.games[1])+'\n\n'+str(self.games[1]))
        with self.assertRaisesRegex(ValueError,'Unknown'):s.reconcile(self.plan_path,self.plan)

    def test_lock_rejects_competing_owner_and_releases(self):
        with s.locked(self.root/'lock'):
            with self.assertRaises(OSError):
                with s.locked(self.root/'lock'):pass
        with s.locked(self.root/'lock'):pass

    def test_forecast_3072_games_and_uncertainty(self):
        x=s.forecast(1536,262,6)
        self.assertAlmostEqual(11.72519,x['expectedPlayHours'],places=4)
        self.assertEqual(3,x['planningSessions'])
        self.assertGreater(x['planningPlayHours'],x['expectedPlayHours'])
        with self.assertRaises(ValueError):s.forecast(10,0,6)
        with self.assertRaises(ValueError):s.utc('2026-10-17T12:00:00')

    def test_identity_rejects_added_jar_and_changed_config(self):
        payload=self.root/'inputs';(payload/'tools').mkdir(parents=True)
        import shutil
        shutil.copy2(s.__file__,payload/'tools/tournament-session.py')
        plan={'host':s.host(),'frozenTrees':{str(payload):s.tree(payload)},'pythonExecutable':{}}
        s.save(self.plan_path,plan);s.save(self.root/'identity.json',{'planSha256':s.digest(self.plan_path)})
        s.verify(self.plan_path)
        (payload/'extra.jar').write_bytes(b'extra')
        with self.assertRaisesRegex(ValueError,'tree changed'):s.verify(self.plan_path)
        plan['host']['logicalCpus']=0;s.save(self.plan_path,plan)
        with self.assertRaisesRegex(ValueError,'Plan identity'):s.verify(self.plan_path)

    def test_changed_binary_model_or_adapter_is_rejected(self):
        import shutil
        payload=self.root/'inputs';(payload/'tools').mkdir(parents=True)
        shutil.copy2(s.__file__,payload/'tools/tournament-session.py')
        for name in ('candidate.jar','baseline.jar','conventional-cp64.cnnue','adapter.class'):
            (payload/name).write_bytes(b'original')
        plan={'host':s.host(),'frozenTrees':{str(payload):s.tree(payload)},'pythonExecutable':{}}
        s.save(self.plan_path,plan);s.save(self.root/'identity.json',{'planSha256':s.digest(self.plan_path)})
        for name in ('candidate.jar','baseline.jar','conventional-cp64.cnnue','adapter.class'):
            with self.subTest(name=name):
                (payload/name).write_bytes(b'changed')
                with self.assertRaisesRegex(ValueError,'tree changed'):s.verify(self.plan_path)
                (payload/name).write_bytes(b'original')

    def test_research_installation_identity_includes_adapter_and_marker(self):
        (self.root/'lib').mkdir();(self.root/'classes').mkdir()
        (self.root/'lib/core.jar').write_bytes(b'jar')
        marker=self.root/'research-launcher.properties';marker.write_text('research')
        adapter=self.root/'classes/Adapter.class';adapter.write_bytes(b'class')
        hashes=b.installation_hashes(self.root)
        self.assertIn(str(marker),hashes);self.assertIn(str(adapter),hashes)

    def test_window_and_pause_do_not_launch_and_export_requires_whole_stage(self):
        self.plan.update(blockLimitSeconds=900,pairs=2)
        end=(datetime.now(timezone.utc)+timedelta(seconds=20)).isoformat()
        with patch.object(s,'verify',return_value=self.plan),patch.object(s,'module'):
            result=s.execute(self.plan_path,end)
        self.assertEqual('paused',result['status']);self.assertEqual(0,result['cumulativePairs'])
        self.assertFalse((self.root/'block-0000.started.json').exists())
        with patch.object(s,'verify',return_value=self.plan):
            with self.assertRaisesRegex(ValueError,'Incomplete scheduled stage'):
                s.export_stage(self.plan_path,self.root/'export')
        self.assertFalse((self.root/'export').exists())
        s.save(self.root/'PAUSE',{'requested':True})
        end=(datetime.now(timezone.utc)+timedelta(hours=1)).isoformat()
        with patch.object(s,'verify',return_value=self.plan),patch.object(s,'module'):
            result=s.execute(self.plan_path,end)
        self.assertEqual('paused',result['status']);self.assertFalse((self.root/'block-0000.started.json').exists())

    def test_no_statistics_in_operational_audit(self):
        folder=self.block(0)
        with patch.object(b,'paired_statistics',side_effect=AssertionError('look')):
            result=b.analyze(folder/'games.pgn',self.plan['blocks'][0]['openings'],1,True,True,statistics_enabled=False)
        self.assertIsNone(result['statistics'])

    def test_slow_preflight_or_pause_during_it_prevents_launch(self):
        self.plan.update(blockLimitSeconds=900,pairs=2)
        initial=datetime.now(timezone.utc)
        for reason in ('window','pause'):
            with self.subTest(reason=reason):
                current=[initial];calls=[]
                class Clock(datetime):
                    @classmethod
                    def now(cls,tz=None):return current[0]
                def verify(path):
                    calls.append(path)
                    if len(calls)==2:
                        if reason=='window':current[0]+=timedelta(seconds=80)
                        else:s.save(self.root/'PAUSE',{'requested':True})
                    return self.plan
                end=(initial+timedelta(seconds=1000)).isoformat()
                with patch.object(s,'datetime',Clock),patch.object(s,'verify',side_effect=verify),patch.object(s,'module') as modules:
                    result=s.execute(self.plan_path,end)
                    modules.return_value.run_owned.assert_not_called()
                self.assertEqual(2,len(calls))
                self.assertEqual('paused',result['status'])
                self.assertFalse((self.root/'block-0000.started.json').exists())

    def test_complete_stage_export_reaudits_all_pairs_without_a_look(self):
        self.block(0);self.block(1)
        inputs=self.root/'inputs';inputs.mkdir()
        (inputs/'schedule.pgn').write_text('\n\n'.join(Path(x['openings']).read_text() for x in self.plan['blocks']))
        self.plan.update(pairs=2,assessmentPlan='Fixed synthetic stage')
        original=s.module
        def modules(name,path):return b if name=='export_audit' else original(name,path)
        with patch.object(s,'verify',return_value=self.plan),patch.object(s,'module',side_effect=modules):
            result=s.export_stage(self.plan_path,self.root/'export')
            self.assertEqual(2,result['pairs']);self.assertFalse(result['statisticsAssessed'])
            self.assertEqual(4,s.read(self.root/'export/complete.json')['games'])
            with self.assertRaisesRegex(ValueError,'Never replace'):s.export_stage(self.plan_path,self.root/'export')

    def test_resource_or_configuration_mismatch_rejected(self):
        folder=self.block(0);record=s.read(folder/'run.json')
        record['arguments']['concurrency']=2;s.save(folder/'run.json',record)
        with self.assertRaisesRegex(ValueError,'did not complete'):s.reconcile(self.plan_path,self.plan)

    def test_confirmatory_purpose_requires_durable_prospective_protocol(self):
        config={'purpose':'confirmatory','assessmentPlan':'Fixed prospective look','blockPairs':1,
                'blockLimitSeconds':300,'arguments':{key:'missing' for key in
                ('java','runner','baseline','candidate','network','openings')}}
        config['arguments'].update(baseline_workers=4,candidate_workers=4,concurrency=1,
            baseline_policy='baseline',candidate_policy='adaptive',hash_mib=64,tc='10+0.1',background=True)
        path=self.root/'config.json';s.save(path,config)
        with patch.object(s,'__file__',str(Path(s.__file__).with_name('tournament-session.py'))), self.assertRaisesRegex(ValueError,'separate prospective protocol'):
            s.prepare(path,self.root/'new-study')
        self.assertFalse((self.root/'new-study').exists())

    def test_host_resource_qualification_is_independent_of_results(self):
        start={'monotonic':10,'ticks':[0,0,0],'availableBytes':12*1024**3,'memoryLoadPercent':50}
        end=dict(start,monotonic=40,ticks=[0,300_000_000,600_000_000])
        self.assertFalse(s.host_interval(start,end)['passed'])
        self.assertTrue(s.host_interval(start,end,owned_cpu=60)['passed'])
        self.assertFalse(s.host_interval(start,dict(end,availableBytes=7*1024**3),60)['passed'])
        with self.assertRaises(ValueError):s.host_interval(start,start)

    def test_resource_failure_blocks_a_complete_block_without_deleting_games(self):
        self.plan['resourceGate']='enabled';s.save(self.plan_path,self.plan)
        folder=self.block(0);raw=(folder/'games.pgn').read_bytes()
        sample={'monotonic':10,'ticks':[0,0,0],'availableBytes':12*1024**3,'memoryLoadPercent':50}
        end=dict(sample,monotonic=40,ticks=[0,300_000_000,600_000_000])
        resource=s.host_interval(sample,end)
        s.save(self.root/'block-0000.host.json',dict(resource,planSha256=s.digest(self.plan_path)))
        with self.assertRaisesRegex(ValueError,'resource qualification'):s.reconcile(self.plan_path,self.plan)
        self.assertEqual(raw,(folder/'games.pgn').read_bytes())
        self.assertFalse((self.root/'block-0000.json').exists())

    def test_busy_host_pauses_before_launch_without_consuming_a_block(self):
        self.plan.update(resourceGate='enabled',blockLimitSeconds=900,pairs=2)
        start={'monotonic':10,'ticks':[0,0,0],'availableBytes':12*1024**3,'memoryLoadPercent':50}
        end=dict(start,monotonic=40,ticks=[0,300_000_000,600_000_000])
        deadline=(datetime.now(timezone.utc)+timedelta(hours=1)).isoformat()
        with patch.object(s,'verify',return_value=self.plan), patch.object(s,'host_sample',side_effect=[start,end]), patch.object(s.time,'sleep'), patch.object(s,'module') as modules:
            result=s.execute(self.plan_path,deadline)
            modules.return_value.run_owned.assert_not_called()
        self.assertEqual('paused',result['status'])
        self.assertFalse((self.root/'block-0000.started.json').exists())


class ProcessTests(unittest.TestCase):
    def test_no_launcher_or_child_code_runs_before_job_assignment(self):
        with tempfile.TemporaryDirectory(prefix='seed-suspended-') as directory:
            root=Path(directory);marker=root/'started'
            original=p.OwnedJob.assign
            def delayed_assign(job,process):
                time.sleep(.2)
                self.assertFalse(marker.exists(), 'Worker ran before containment')
                original(job,process)
            code=f'import pathlib,sys;pathlib.Path({str(marker)!r}).write_text("started");sys.stdin.readline()'
            with patch.object(p.OwnedJob,'assign',delayed_assign),(root/'log').open('wb') as log:
                result=p.run_owned([sys.executable,'-B','-c',code],log,5,root,None)
            self.assertEqual(0,result['exitCode']);self.assertTrue(marker.exists())

    def test_owned_normal_exit_and_watchdog_leave_unrelated_alive(self):
        unrelated=subprocess.Popen([sys.executable,'-c','import time;time.sleep(60)'])
        self.addCleanup(lambda: (unrelated.kill(),unrelated.wait()) if unrelated.poll() is None else None)
        with tempfile.TemporaryDirectory(prefix='seed-job-') as directory:
            root=Path(directory)
            with (root/'log').open('wb') as log:
                result=p.run_owned([sys.executable,'-c','import sys;assert sys.stdin.readline()=="GO\\n"'],log,5,root,None)
                self.assertEqual(0,result['exitCode'])
                with self.assertRaises(subprocess.TimeoutExpired):
                    p.run_owned([sys.executable,'-c','import sys,time;sys.stdin.readline();time.sleep(60)'],log,.3,root,None)
            self.assertIsNone(unrelated.poll())

    def test_parent_death_kills_owned_descendants(self):
        n=s.module('native',Path(__file__).with_name('sr036-process-cost.py'))
        with tempfile.TemporaryDirectory(prefix='seed-parent-') as directory:
            root=Path(directory);pidfile=root/'child.pid'
            child='import time;time.sleep(60)'
            worker=f'import sys,subprocess,time,pathlib;sys.stdin.readline();p=subprocess.Popen([sys.executable,"-c",{child!r}]);pathlib.Path({str(pidfile)!r}).write_text(str(p.pid));time.sleep(60)'
            supervisor=f'import importlib.util,sys;spec=importlib.util.spec_from_file_location("p",{p.__file__!r});m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);m.run_owned([sys.executable,"-c",{worker!r}],sys.stdout.buffer,60,{str(root)!r},None)'
            process=subprocess.Popen([sys.executable,'-B','-c',supervisor],stdout=subprocess.DEVNULL)
            handle=None
            try:
                end=time.monotonic()+10
                while not pidfile.exists() and time.monotonic()<end:time.sleep(.05)
                self.assertTrue(pidfile.exists())
                handle=n.kernel.OpenProcess(0x1000|0x100000,False,int(pidfile.read_text()))
                self.assertTrue(handle)
                process.kill();process.wait(timeout=5)
                end=time.monotonic()+5
                while n.snapshot(handle)['exitCode']==259 and time.monotonic()<end:time.sleep(.05)
                self.assertNotEqual(259,n.snapshot(handle)['exitCode'])
            finally:
                if process.poll() is None:process.kill();process.wait()
                if handle:n.kernel.CloseHandle(handle)


if __name__=='__main__':unittest.main()
