"""Bounded production-integration observation, separate from closed MW/IC campaigns.

Reuses the existing paired Fastchess runner, independent chess/trace audits and
Windows owned-process boundary. No campaign ledger, selection or tuning writes.
Run once per prospectively recorded protocol; existing output is never reused.
"""
import argparse
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import sys
import time

sys.dont_write_bytecode = True
spec = importlib.util.spec_from_file_location('production_match', Path(__file__).with_name('p4-multiworker-match.py'))
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)


def save(path, value):
    Path(path).write_bytes((json.dumps(value, indent=2) + '\n').encode())


def commands(runtime, profile, actor):
    # Both sides use the same production executable, CP64 and clock binding.
    # Only the explicit exact historical TC_P1 control overrides the default.
    return [m.JAVA, '--add-modules=jdk.incubator.vector', '-Xms128m', '-Xmx512m',
            '-XX:ActiveProcessorCount=4', '-Djava.awt.headless=true',
            '-Dseedv6.syzygy.config=', '-Dseedv6.syzygy.path=',
            '-Dseedv6.clockPolicy=BOUNDARY2',
            *(['-Dseedv6.searchProfile=TC_P1'] if profile == 'TC_P1' else []),
            '-cp', str(Path(runtime) / 'lib/*'), 'com.ohinteractive.seedv6.uci.UciMain']


def configure(engine, workers, profile):
    engine.send('uci'); engine.until('uciok', 60)
    engine.send(f'setoption name Threads value {workers}\nsetoption name Hash value 64\nsetoption name OwnBook value false')
    engine.send('setoption name Show Search Configuration\nisready')
    lines = engine.until('readyok', 60)
    identity = next(line for line in lines if line.startswith('info string baseline='))
    schedule = 'OWNER' if workers == 1 else 'EAGER_ROOT' if profile == 'MW_C4' else 'ROOT_REUSE'
    for token in [f'searchProfile={profile} ', 'flags=' + str(1182007295 if profile == 'MW_C4' else 1177812991),
                  f'workers={workers};hashMiB=64;aspiration=ADAPTIVE16;parallel={schedule}',
                  'checkpoint=64 schema=S6CNU002 heads=8', 'clockPolicy=boundary2-d6-v1',
                  'networkSha256=8431626c2fca81e0a1e9ccd5e45ddd8829e6e3282c756c21fad75b06d075bb2b']:
        if token not in identity:
            raise ValueError('Missing production identity: ' + token)
    return identity


def run(protocol, directory):
    p = json.loads(protocol.read_text())
    assert p['workers'] == [1, 4, 8] and p['openingIndices'] == [0, 1, 2, 3]
    assert p['maximumGames'] == 24 and p['maximumWallSeconds'] <= 3600
    assert p['candidate'] == 'MW_C4' and p['baseline'] == 'TC_P1'
    assert p['kind'] == 'timed' and p['tc'] == '10+0.1'
    for name, digest in p['inputs'].items():
        assert m.b.digest(m.ROOT / name) == digest, name
    directory.mkdir(parents=True, exist_ok=False)
    report = dict(status='running', protocol=str(protocol), protocolSha256=m.b.digest(protocol),
                  initiatedGames=0, completedGames=0, chargedWallSeconds=0, domains=[])
    save(directory / 'result.json', report)
    try:
        for workers in p['workers']:
            pc = m.mw.PcUse(); pc.sample(); time.sleep(.25); sample = pc.sample()
            decision = m.mw.resource_decision(sample, workers, p['resources'], idle=True)
            if not decision['admissible']:
                raise RuntimeError('Resource admission: ' + str(decision))
            domain = directory / f'w{workers}'; domain.mkdir()
            child_protocol = dict(p, workers=workers)
            save(domain / 'protocol.json', child_protocol)
            receipt = dict(workers=workers, resourceSample=sample, output=str(domain))
            report['domains'].append(receipt)
            job = m.mw.support.owned.OwnedJob(); process = None; started = time.monotonic()
            try:
                remaining = p['maximumWallSeconds'] - report['chargedWallSeconds']
                if remaining < 60: raise TimeoutError('No bounded match time remains')
                with (domain / 'console.log').open('x') as log, (domain / 'environment.jsonl').open('x') as environment:
                    process = subprocess.Popen([sys.executable, '-B', str(Path(__file__).resolve()), '--worker',
                        '--protocol', str(domain / 'protocol.json'), '--output', str(domain)], cwd=m.ROOT,
                        stdout=log, stderr=subprocess.STDOUT,
                        creationflags=0x4 | 0x4000 | subprocess.CREATE_NO_WINDOW)
                    job.assign(process); job.resume_initial_thread(process)
                    deadline = started + remaining
                    while process.poll() is None:
                        if time.monotonic() >= deadline: raise TimeoutError('Match wall ceiling')
                        try: process.wait(timeout=min(2, max(.01, deadline - time.monotonic())))
                        except subprocess.TimeoutExpired:
                            sample = pc.sample(); environment.write(json.dumps(sample) + '\n'); environment.flush()
                            decision = m.mw.resource_decision(sample, workers, p['resources'], running=True, idle=True)
                            if not decision['admissible']: raise RuntimeError('Resource pressure: ' + str(decision))
                    if process.returncode != 0: raise RuntimeError('Match/audit process failed: ' + str(process.returncode))
                # Job accounting can lag process exit briefly; require a natural drain.
                settle = time.monotonic() + 5
                while job.accounting().activeProcesses and time.monotonic() < settle: time.sleep(.05)
                receipt['naturalDrain'] = job.accounting().activeProcesses == 0
                if not receipt['naturalDrain']: raise RuntimeError('Owned descendants survived')
            finally:
                try: receipt['drain'] = m.mw.support.drain(job, process)
                finally:
                    job.close()
                    if process is not None and process.poll() is None: process.kill(); process.wait(timeout=10)
                receipt['chargedWallSeconds'] = time.monotonic() - started
                report['chargedWallSeconds'] += receipt['chargedWallSeconds']
                progress = domain / 'progress.jsonl'
                rows = [json.loads(line) for line in progress.read_text().splitlines()] if progress.exists() else []
                report['initiatedGames'] += 2 * sum(row['type'] == 'pair-start' for row in rows)
                completed = [row for row in rows if row['type'] == 'pair-complete']
                report['completedGames'] += 2 * len(completed)
                receipt['pairs'] = completed
                receipt['identities'] = next((row['identities'] for row in rows if row['type'] == 'identity'), {})
                save(directory / 'result.json', report)
        assert report['initiatedGames'] == report['completedGames'] == 24
        report['status'] = 'passed'
    except BaseException as error:
        report.update(status='incomplete', error=repr(error))
        raise
    finally:
        report['inputsStable'] = all(m.b.digest(m.ROOT / name) == digest for name, digest in p['inputs'].items())
        if not report['inputsStable']: report['status'] = 'failed-input-stability'
        save(directory / 'result.json', report)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--protocol', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--worker', action='store_true')
    args = parser.parse_args()
    if args.worker:
        m.command, m.configure = commands, configure
        m.worker(args.protocol.resolve(), args.output.resolve())
    else:
        run(args.protocol.resolve(), args.output.resolve())
