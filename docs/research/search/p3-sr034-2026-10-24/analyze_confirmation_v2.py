"""Path compatibility adapter for the immutable v1 final analysis.

The SR-034 digest helper requires pathlib.Path, while inherited v1 passes string
keys for external files. Normalize that argument only. Frozen input checks,
statistical functions, sample, thresholds and output remain the original v1 code.
Validate this adapter before opening the single final result.
"""
from pathlib import Path
from run_trajectory_v1 import HERE, module


def analysis_module():
    analysis = module('immutable_confirmation_analysis_v1', HERE/'analyze_confirmation_v1.py')
    original_digest = analysis.digest
    analysis.digest = lambda path: original_digest(Path(path))
    return analysis


if __name__ == '__main__': analysis_module().main()
