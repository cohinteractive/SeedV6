"""Short real dataset/match boundary checks, inside the owner's outer supervisor."""
import argparse
import json
from pathlib import Path
import subprocess
import sys
import time
import compute_control as cc
import compute_gate


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('output',type=Path);p.add_argument('source',type=Path);p.add_argument('network',type=Path)
    a=p.parse_args();compute_gate.require();a.output.mkdir(parents=True,exist_ok=False)
    control=cc.Control(a.output/'test-control');control.request('NORMAL')
    prefix=[sys.executable,'-B',str(Path(cc.__file__)),'--state-dir',str(control.directory)]
    def run(label,command):
        return subprocess.Popen(prefix+['run','--name',label,'--resume-note','Control validation; inspect outputs',
            '--log',str(a.output/(label+'.log')),'--',*command],stdout=subprocess.DEVNULL)
    process=None
    try:
        dataset=a.output/'dataset'
        process=run('dataset',[sys.executable,'-B','tools/conventional-nnue/prepare_lichess.py',
            str(a.source),str(dataset),'--positions','131072'])
        deadline=time.monotonic()+20
        while not dataset.exists():
            if process.poll() is not None or time.monotonic()>deadline:raise RuntimeError('Dataset did not start')
            time.sleep(.05)
        time.sleep(.5);start=time.monotonic();control.request('COMPUTE_HOLD')
        process.wait(timeout=35);dataset_latency=time.monotonic()-start
        manifest=json.loads((dataset/'manifest.json').read_text())
        if not manifest['stopped_for_compute_hold'] or manifest['completed_requested_sample']:
            raise RuntimeError('Dataset hold did not preserve marked partial evidence')
        if not control.status()['hold_complete']:raise RuntimeError('Dataset cleanup incomplete')
        compute_gate.require();control.request('NORMAL')
        openings=json.loads((cc.ROOT/'docs/research/conventional-nnue-cglhw/openings-screen.json').read_text())[:2]
        cc.atomic(a.output/'openings.json',openings)
        match=a.output/'match'
        command=[sys.executable,'-B','tools/conventional-nnue/paired_match.py','--network',str(a.network),
            '--openings',str(a.output/'openings.json'),'--output',str(match),'--movetime-ms','10','--max-plies','100']
        process=run('match',command);deadline=time.monotonic()+30
        while not (match/'games.jsonl').exists():
            if process.poll() is not None or time.monotonic()>deadline:raise RuntimeError('First game did not finish')
            time.sleep(.02)
        start=time.monotonic();control.request('COMPUTE_HOLD');process.wait(timeout=35)
        match_latency=time.monotonic()-start
        records=[json.loads(line) for line in (match/'games.jsonl').read_text().splitlines()]
        if not 0<len(records)<4:raise RuntimeError('No incomplete match witnessed')
        hashes={path.name:cc.sha(path) for path in match.glob('0*.pgn')}
        protocol_hash=cc.sha(match/'protocol.json')
        if not control.status()['hold_complete']:raise RuntimeError('Engine job did not clean up')
        compute_gate.require();control.request('NORMAL')
        process=run('match-resume',command+['--resume']);code=process.wait(timeout=30)
        if code!=0:raise RuntimeError('Match resume failed')
        final=[json.loads(line) for line in (match/'games.jsonl').read_text().splitlines()]
        if len(final)!=4 or len({(g['opening_index'],g['candidate_white']) for g in final})!=4:
            raise RuntimeError('Completed games repeated or omitted')
        if any(cc.sha(match/name)!=digest for name,digest in hashes.items()) or cc.sha(match/'protocol.json')!=protocol_hash:
            raise RuntimeError('Resume changed completed game/protocol evidence')
        cc.atomic(a.output/'summary.json',dict(dataset_hold_seconds=dataset_latency,
            dataset_accepted_before_hold=manifest['counts']['accepted'],dataset_partial_marked=True,
            match_hold_seconds=match_latency,completed_games_before_resume=len(records),
            completed_games_after_resume=len(final),completed_game_bytes_preserved=True,owned_processes_final=control.status()['owned_processes']))
        print((a.output/'summary.json').read_text())
    finally:
        control.request('COMPUTE_HOLD',now=True)
        if process and process.poll() is None:process.wait(timeout=15)


if __name__=='__main__':main()
