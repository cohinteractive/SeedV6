"""R057 bounded foreground stage, using the existing Job Object supervisor."""
import argparse
import json
import os
from pathlib import Path
import subprocess
import time
from baseline import ROOT, admit, sha
from compute_control import atomic
from training_lifecycle import training, require_java_guard, read_json
from contextlib import nullcontext


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('plan', type=Path)
    args = parser.parse_args()
    plan = json.loads(args.plan.read_text())
    limit = plan['maximum_seconds']
    if not 0 < limit <= 300:
        raise ValueError('Non-checkpointed stage must be bounded to <=300 seconds')
    directory = ROOT / 'docs/research/brn-relational-cglhw/raw/r057'
    binding = dict(plan_sha256=sha(args.plan), runner_sha256=sha(Path(__file__)),
                   sources={p: sha(ROOT / p) for p in plan['sources']})
    command = plan['command']
    main = 'com.ohinteractive.seedv6.core.brnpair2.OneTraining'
    lifecycle_context = (training(ROOT / command[command.index(main)+1], 'OneTraining')
                         if main in command else nullcontext(None))
    if main in command and json.loads((ROOT / command[command.index(main)+1]).read_text()).get('training_finalization'):
        binding['runtime_sha256'] = require_java_guard(command, 'OneTraining')
    with lifecycle_context as lifecycle:
        if lifecycle:
            for prior in lifecycle.root.glob('launch-*.binding.json'):
                if read_json(prior)['runtime_sha256'] != binding['runtime_sha256']:
                    raise ValueError('ONE runtime changed; preserve the run and its pinned restart implementation')
        if lifecycle and (lifecycle.root / 'result.json').exists():
            lifecycle.complete()  # Reconcile a crash after result publication.
            print('Verified terminal ONE result; no training repeated', flush=True)
            return
        if lifecycle:
            directory = lifecycle.root
            binding['config_sha256'] = lifecycle.owner['config_sha256']
        directory.mkdir(parents=True, exist_ok=True)
        attempt = directory / (('launch' if lifecycle else plan['stage']) + '-' + str(time.time_ns()))
        atomic(attempt.with_suffix('.binding.json'), binding)
        started = time.monotonic()
        process = None
        samples = []
        error = None
        code = None
        env = os.environ.copy()
        env.update(plan.get('environment', {}))
        if plan.get('path_prepend'):
            env['PATH'] = os.pathsep.join(str(ROOT / p) for p in plan['path_prepend']) + os.pathsep + env.get('PATH', '')
        try:
            samples.append(admit(initial=True))
            with attempt.with_suffix('.stdout.log').open('x') as out, attempt.with_suffix('.stderr.log').open('x') as err:
                process = subprocess.Popen(plan['command'], cwd=ROOT, env=env, stdout=out, stderr=err,
                    creationflags=subprocess.CREATE_NO_WINDOW | subprocess.BELOW_NORMAL_PRIORITY_CLASS)
                while True:
                    try:
                        code = process.wait(timeout=5)
                        if code:
                            raise RuntimeError('Stage exit ' + str(code))
                        break
                    except subprocess.TimeoutExpired:
                        samples.append(admit())
                        if time.monotonic() - started > limit:
                            raise RuntimeError('Bounded stage deadline reached; preserve incomplete evidence')
        except Exception as exc:
            error = repr(exc)
            raise
        finally:
            if process is not None and process.poll() is None:
                process.terminate()
                try:
                    process.wait(timeout=5)
                except subprocess.TimeoutExpired:
                    process.kill()
                    process.wait(timeout=5)
            atomic(attempt.with_suffix('.execution.json'), dict(stage=plan['stage'], command=plan['command'],
                seconds=time.monotonic()-started, returncode=code, error=error, resources=samples,
                direct_child_absent=process is None or process.poll() is not None,
                descendant_release='Verify Job Object supervisor status after this runner exits'))
            print(attempt, flush=True)
        if lifecycle and (lifecycle.root / 'result.json').exists():
            lifecycle.complete()
    print(attempt.with_suffix('.stdout.log').read_text(), flush=True)


if __name__ == '__main__':
    main()
