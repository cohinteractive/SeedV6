# Compute-control validation, 22 October 2026

The owner steering is integrated into the existing CGLHW (contract revision 2).
Initial permission remains NORMAL, with no expiry; this is historical initialization,
not permission to reset later controls. The completed E002 data extraction was
preserved. No training was active when integration began. No production runtime,
default evaluator, frozen experiment, suspended BRN work or application version was
changed by this tooling unit. Build remains 62. No finalizer is involved.

## Implemented boundary

`nnue-control.cmd` is a thin Python entry point. The standard-library controller
stores locked/atomic local state in worktree Git metadata, serializes expensive
operations, and supervises an owned Windows job only while work runs. Windows 10+
JOB_LIST assigns the child atomically inside CreateProcessW. Kill-on-job-close,
restricted inherited handles and no breakaway cover owned descendants without
process-name killing. Dead-controller reconciliation holds; absent/corrupt state
cannot authorize launches. Mode commands work in an independent terminal.

Single-superbatch Bullet processes supply frequent boundaries. Original plan,
trainer hash, exact per-segment configs, checkpoint receipts, optimizer payloads,
absolute progress and logs are retained. Native training/conversion and Python
dataset/match tools also check leases. Constant LR/WDL and absolute loader cursor
preserve the established training recipe. `save4` becomes `save1` for safe stopping.
No Bullet dependency fork, scheduler service, administrator change or OS driver
change was needed. GUI/live browser checks do not apply to this CLI capability.

## Actual checks

Twelve short Windows integration tests passed, zero failures/errors in 7.86 seconds:
NORMAL launch, held launch denial, lightweight permission under HOLD, FULL PAUSE,
fresh-process persistence, explicit resume, repeated/latched holds, competing
launch rejection, failed-operation cleanup, cooperative stop, forced owned-tree
cleanup, unrelated-process survival, supervisor crash, crash inside process creation,
unknown/corrupt/nonfinite permission rejection and window expiry. Some individual
tests cover multiple assertions. Evidence:
`build/conventional-nnue/control-tests-c7aadebb1577/summary.json` and
`build/conventional-nnue/control-tests-3.log`. Earlier passing iterations remain.

On this Windows 11 / RTX4060, short **real GPU** tests used the existing pilot data,
batch1024, 128 batches/superbatch, four superbatches, constant LR/WDL:

| Check | Observed result |
| --- | --- |
| Graceful hold during segment 2 | 1.627 seconds; completed checkpoint 2; zero owned processes; CUDA PID absent |
| Explicit resume after HOLD/FULL PAUSE | Started at segment 3; ended at 4; segments 1/2 not repeated |
| Emergency hold during segment 2 | 0.166 seconds; checkpoint 1 preserved; incomplete segment 2 retained |
| Resume after emergency | Started from verified checkpoint 1; finished through 4; no completed segment repeated |
| Held fresh-process training launch | Rejected before starting another trainer |
| Optimizer integrity | All five file hashes and tensor geometry readable; tampered momentum copy rejected |

Raw evidence: `build/conventional-nnue/control-gpu-1/`,
`build/conventional-nnue/control-gpu-emergency-1/`, and
`build/conventional-nnue/control-checkpoint-corruption-1/`. Actual CUDA ownership was
observed with nvidia-smi before the request and its PID checked absent afterward;
Windows process/job state independently confirmed termination. Other applications
and overall GPU usage were not used as ownership criteria. The emergency run used
the final atomic-creation job implementation, including nested jobs in the test.

The graceful resumed endpoint was compared with a nonsegmented uninterrupted
reference of the same recipe/data/seed: maximum weight difference 4.8541e-5, RMS
3.2377e-7; momentum maximum 2.8813e-9; velocity maximum 1.7053e-13. Quantized bytes
were not identical. These are observed segmented/GPU continuation differences,
not a claim of exact replay or proof that every longer run will have this bound.
Pinned source inspection confirms the same optimizer parameters and data offset;
GPU reductions are nondeterministic. Frozen file hashes remain authoritative.

Real CPU/data/UCI checks also passed: a dataset hold completed in 0.174 seconds,
preserving a marked 3433-position partial extraction; a match hold completed in
0.171 seconds after one completed game. Resume completed all four test games
without changing the first game's bytes or frozen protocol. All owned engine
processes exited. These 10ms/100-ply test games are control tests, not strength
evidence. Raw: `build/conventional-nnue/control-workloads-2/`. The first attempt
used an incorrect network path and failed before engine launch; its data and
failure evidence remain under `control-workloads-1/`.

The permission-aware native trainer compiled successfully with locked dependencies
and one Cargo worker. Native guards are exercised by the real runs. Existing
runtime tests are not rerun for this non-runtime tooling-only change; the prior
42 selected Java tests remain the application boundary evidence. Final Python
syntax inspection and `git diff --check` are recorded with the commit check.

## Limits and disposition

The measurements above are short local observations, not guaranteed interruption
times. Normal polling is 0.2 seconds with a default 30-second grace policy before
owned-job termination. A simulated uncooperative tree stopped in 1.358 seconds
using a one-second test grace; a supervisor-crash cleanup was observed in 0.011
seconds. Driver/kernel stalls or machine failure can exceed a timing policy.
Actual Windows reboot, power loss and storage failure were not induced. Atomic
publication, kill-on-close and checksum-based recovery were tested through process
failure/corruption; hardware crash durability remains bounded by Windows/storage.

No multi-hour run, long timeout or overnight user-reporting campaign was used to
validate controls. Window expiry was tested over 1.2 seconds; it uses UTC wall time,
so manual system-clock changes affect the window. No service wakes Codex or renews
authorization. The original 15-minute user-facing reporting requirement remains;
current asynchronous execution supports reporting and has not required an alternative.

Raw state/checkpoints are local, absent from fresh clones. Code/contract/compact
summaries are tracked; raw evidence is ignored and preserved. A new clone is UNKNOWN.
The control capability is technically verified; this is not owner acceptance or
completion/promotion of the NNUE evaluator. Continue E002 under the **observed**
current mode. Human actions required after this work unit: None.
