"""Deterministic A001 pilot/parity/endpoint execution through existing owner control.

No polling, games, selection or cleanup. Re-entry checks completed artifacts;
partial assessments require inspection rather than overwriting. Source recipes
are immutable. Run with the established process-local CUDA DLL path.
"""
import argparse
import json
import os
from pathlib import Path
import subprocess
import sys
import time

from bootstrap import ROOT, BASE, PRIOR, sha, write
sys.path.insert(0, str(ROOT / 'tools/conventional-nnue'))
from compute_control import verify_checkpoint


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('names', nargs='+', choices=['replica', 'teacher', 'wdl50', 'narrow', 'late'])
    a = p.parse_args()
    os.chdir(ROOT)
    start = time.monotonic()
    python = [sys.executable, '-B']
    control = python + ['tools/conventional-nnue/compute_control.py']
    trainer = ROOT / 'build/conventional-nnue/cargo-target/release/seed-conventional-nnue.exe'
    assert sha(trainer) == json.loads((BASE / 'baseline.json').read_text())['trainer_sha256']
    probes = 'docs/research/conventional-nnue-cglhw/raw/e007-probes/probes.fens'
    validation = str(PRIOR / 'raw/mixed-holdout/validation.txt')
    def execute(command):
        if time.monotonic() - start >= 7200:
            raise RuntimeError('A001 execution wall budget reached; reassess economics')
        subprocess.run(command, check=True, timeout=7200 - (time.monotonic() - start))
    def checkpoint(output, step):
        paths = list(Path(output).glob(f'segments/*/run/seed-cnnue-v1-{step}/seed-manifest.json'))
        if len(paths) != 1:
            raise RuntimeError(f'Expected exactly one completed endpoint {output}/{step}')
        verify_checkpoint(paths[0].parent)
        return paths[0].parent
    def train_if_needed(config_path, config, label):
        output = Path(config['output'])
        plan = output / 'requested-config.json'
        if plan.exists() and plan.read_bytes() != config_path.read_bytes():
            raise RuntimeError('Training plan changed')
        paths = list(output.glob(f"segments/*/run/seed-cnnue-v1-{config['end_superbatch']}/seed-manifest.json"))
        if paths:
            checkpoint(output, config['end_superbatch'])
        else:
            execute(control + ['train', '--name', label, str(config_path)])
    for name in a.names:
        pilot_path = BASE / f'a001-{name}-pilot.json'
        pilot = json.loads(pilot_path.read_text())
        train_if_needed(pilot_path, pilot, f'phase2-a001-{name}-pilot')
        cp4 = checkpoint(pilot['output'], 4)
        evaluation = BASE / f'raw/a001-{name}-pilot-eval'
        if not (evaluation / 'manifest.json').exists():
            execute(control + ['run', '--name', f'phase2-a001-{name}-pilot-parity',
                    '--resume-note', 'Inspect preserved pilot; parity gates endpoint continuation',
                    '--log', str(BASE / f'raw/a001-{name}-pilot-eval.log'), '--'] + python +
                    ['tools/conventional-nnue/evaluate_checkpoint.py', str(cp4), probes, validation,
                     str(evaluation), '--vector', '--strength-research'])
        parity = json.loads((evaluation / 'parity.json').read_text())
        if parity['status'] != 'PASS' or parity['positions'] != 259:
            raise RuntimeError('Pilot did not pass full parity')
        write_once = BASE / f'a001-{name}-pilot-results.json'
        if not write_once.exists():
            write(write_once, dict(parity=parity,
                  validation=json.loads((evaluation / 'validation.json').read_text()),
                  manifest_sha256=sha(evaluation / 'manifest.json'), evidence=str(evaluation)))
        final = dict(pilot, output=str(BASE / f'raw/a001-{name}-64'),
                     start_superbatch=5, end_superbatch=64, resume=str(cp4))
        final_path = BASE / f'a001-{name}-64.json'
        if final_path.exists():
            assert json.loads(final_path.read_text()) == final
        else:
            write(final_path, final)
        train_if_needed(final_path, final, f'phase2-a001-{name}-64')
        cp64 = checkpoint(final['output'], 64)
        spec = dict(models=[dict(id=name, checkpoint=str(cp64),
                    evaluation=str(BASE / f'raw/a001-{name}-final-eval'))], probes=probes,
                    validation=validation, roots=str(PRIOR / 'cost-roots.fens'))
        spec_path = BASE / f'a001-{name}-assessment-spec.json'
        if spec_path.exists():
            assert json.loads(spec_path.read_text()) == spec
        else:
            write(spec_path, spec)
        assessment = BASE / f'raw/a001-{name}-assessment'
        if not (assessment / 'summary.json').exists():
            execute(control + ['run', '--name', f'phase2-a001-{name}-assessment',
                    '--resume-note', 'Preserve raw assessment; inspect failure before retrying',
                    '--log', str(BASE / f'raw/a001-{name}-assessment.log'), '--'] + python +
                    ['tools/conventional-nnue/assess_strength.py', str(spec_path), str(assessment)])
        report = BASE / f'a001-{name}-assessment.json'
        if not report.exists():
            write(report, json.loads((assessment / 'summary.json').read_text()))
        print(f'A001 {name}: endpoint and assessment complete', flush=True)


if __name__ == '__main__':
    main()
