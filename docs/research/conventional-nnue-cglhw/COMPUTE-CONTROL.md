# Owner compute controls

From PowerShell in the repository (Python is already installed):

```powershell
.\nnue-control.cmd status
.\nnue-control.cmd normal
.\nnue-control.cmd hold
.\nnue-control.cmd pause
.\nnue-control.cmd normal --minutes 480
```

NORMAL authorizes compute; COMPUTE HOLD releases compute and allows lightweight
work; FULL PAUSE also tells Codex to finish safe bookkeeping and end execution.
The last command authorizes eight hours, then holds. Commands do not start Codex
or a campaign. Tell Codex to continue/resume when needed. Conversational equivalents:
“continue normally”, “compute hold; I am gaming”, “fully pause the NNUE CGLHW”, and
“resume NNUE overnight [for N hours]”. Codex must operate the same durable control.

Use a second terminal while Codex or training is busy. No service is installed.
Hold/pause requests persist immediately; an active operation remains pending.
Release is confirmed by `status` showing `active: null`, `owned_processes: 0` and
`hold_complete: true`. Other applications may still use the GPU and are untouched.
For immediate emergency cleanup, losing uncheckpointed work:

```powershell
.\nnue-control.cmd hold --now
.\nnue-control.cmd pause --now
```

Only the recorded Windows Job Object is terminated. The unfinished superbatch,
current game or partial dataset may be lost; completed checkpoints/games remain.
Partial outputs are preserved as evidence. Never kill by process name or edit
control.json manually.

## Persistence and supervision

`git rev-parse --path-format=absolute --git-path seed-conventional-nnue` locates
operational state for this worktree: atomic control.json, an OS file lock and
per-operation records. This local state survives sessions, reboots and build cleanup,
but is not Git content. Preserve it with local evidence backups. A fresh clone or
missing file starts UNKNOWN; explicit authorization is needed. Corrupt state fails
closed and needs inspection, preserving its bytes. STATE.md requires fresh agents
to consult it, so an ACTIVE workstream label never releases a hold.

Records include permission/window, pending stop, unique job, controller PID plus
creation time, child command/PID/log, checkpoint and resume note. Status checks job
process counts and reconciles dead controllers as interrupted plus COMPUTE HOLD.
Repeated holds do not reset the deadline. NORMAL cannot cancel a latched stop;
cleanup must finish before a later launch. An expired window holds on the active
supervisor's next poll, or the next status/launch when idle. It never renews itself.

All expensive CGLHW commands use these interfaces:

```powershell
.\nnue-control.cmd check
.\nnue-control.cmd check --lightweight
.\nnue-control.cmd train PATH_TO_IMMUTABLE_CONFIG.json
.\nnue-control.cmd run --name "NNUE validation" --resume-note "Rerun bounded validation if interrupted" --log NEW_LOG_PATH -- C:\java\jdk-21\bin\java.exe JAVA_ARGUMENTS
```

`run` takes an executable and arguments after `--`. Batch scripts need explicit
cmd.exe /d /c or PowerShell. Set local CUDA/PATH/Gradle environment as in RUNBOOK.md.
Logs need new paths. Only one expensive operation holds a lease. Native training,
conversion and Python dataset/match entry points also check the active lease.
Alternate state directories are test isolation, never a permission bypass.

Windows 10+ JOB_LIST assigns ownership atomically inside process creation, closing
the crash gap before assignment. The initially suspended child is immediately
resumed. All descendants inherit ownership;
no breakaway/WMI/service launch is allowed. Kill-on-job-close cleans up if the
supervisor dies. Explicit cleanup confirms zero processes before clearing activity.
This does not suspend GPU resources as a hold mechanism or traverse unrelated PIDs.

## Checkpoints and recovery

`train CONFIG` freezes the exact requested bytes and trainer hash in a new output
directory. It runs one superbatch per child, saving each complete boundary. All
segments preserve batch size, data/probe hashes, seed, constant LR/WDL and absolute
superbatch index. The direct loader seeks to
`batch_size * batches_per_superbatch * (superbatch - 1) mod dataset_records`.
Weights, AdamW momentum/velocity and the pinned clipping/decay/beta settings are
restored. This pinned AdamW has no additional bias-correction step counter.
GPU float reductions remain nondeterministic: frozen checkpoint/inference bytes
are reproducible; regenerated training bytes are not promised identical.

Bullet pin `579b78b37a694cacd6e9ede4706386c15c34837c` has no external graceful-stop
API in ValueTrainer.run; callbacks cannot break its loop. Segments avoid an upstream
fork. Our wrapper flushes five checkpoint payloads, then atomically publishes a
receipt. The controller verifies config/receipt hashes, every payload hash,
optimizer tensor geometry and network length/padding before recording progress.
Full export parity remains a separate acceptance check. Partial receipts are invalid.

After NORMAL, reissue the same `train CONFIG`. It verifies the immutable plan,
trainer, recipes, receipts and contiguous progress, skips completed superbatches,
and makes a fresh directory for the next attempt. Interrupted files are retained.
Trainer changes or ambiguous/corrupt evidence require reconciliation. E003 adds a
narrow `data_fork_reason` declaration for a prospectively recorded data/probe change
at an initial checkpoint import. It preserves weights/optimizer, seed, batch shape,
LR/WDL and next-superbatch index; it does not claim the old data stream continues.
Unlabelled data changes and optimizer/schedule changes are rejected. Subsequent
segments require full equality with the new immutable recipe. This is separate
from holding/resuming an unchanged experiment, which never needs a data fork.

E005 adds optional `lr_drop_every`/`lr_gamma` to that immutable recipe. Omitted
fields preserve the old constant learning rate. Decay uses the global superbatch
index and does not restart at each process; checkpoint receipts record the effective
rate. Tested restart across four decay steps preserved weights/momentum/velocity.
Original trainer binaries are retained by SHA256 under the ignored
`build/conventional-nnue/frozen-trainers`; use `train --trainer PATH CONFIG` when
resuming a plan whose recorded binary differs from the current build. Do not replace
the plan's binary identity silently. See E005.md for the measured larger-batch hold.

Superbatches are capped at 1024 batches. Holds are polled every 0.2 seconds and
default to a 30-second grace interval. If the current process has not finished,
the supervisor terminates its job and retains the last verified checkpoint. This
is a timeout policy, not a hard real-time Windows/driver guarantee. `--now` bypasses
the interval. See CONTROL-VALIDATION.md for measured latency and conditions.

Dataset extraction checks every 1000 source lines and writes a marked partial
manifest on hold. It cannot resume the compressed stream: reuse completed datasets,
or preserve a partial extraction and restart to a new path when necessary.
Conversion checks every 4096 records; partial binaries are regenerated to new paths.
BT4 prepared-chunk extraction checks each chunk/every4096 records and every65536
shuffle swaps. A hold leaves partial outputs without a completed manifest; they
are evidence, not a usable dataset. Reuse verified finished datasets rather than
extracting them again. The supervisor deadline still bounds uncooperative cleanup.
Matches check between moves, preserve interrupted-game evidence and quit engines.
`paired_match.py --resume` verifies its frozen protocol, skips completed games and
restarts only the unfinished game from its opening. Torn game records require
reconciliation. Generic build/validation resume notes explain safe rerunning.

If Codex ends while the supervisor remains alive, local commands still work. If
the supervisor crashes, Windows kills its job; the next status records interruption
and holds. If Windows terminates, memory-only work is lost. Resume rechecks files
and uses the last complete receipt. Atomic writes/flushes reduce torn records but
cannot guarantee storage survival. No malformed state/checkpoint is accepted.

## Reporting

Codex still reports to the user at least every 15 minutes during active long work;
logs supplement reports. No wake-up or campaign scheduler is installed. Compute
FULL PAUSE works independently; Codex observes it before its next work unit, does
necessary safe bookkeeping and ends. Under COMPUTE HOLD it does useful inexpensive
work or ends without repeated polling. If the execution environment cannot support
reports, affected unattended work requires an agreed alternative before launch.

Windows references: [Job Objects](https://learn.microsoft.com/en-us/windows/win32/procthread/job-objects),
[CreateProcessW](https://learn.microsoft.com/en-us/windows/win32/api/processthreadsapi/nf-processthreadsapi-createprocessw).
Pinned Bullet sources inspected: `crates/bullet_lib/src/value.rs`,
`crates/trainer/src/run.rs`, `reader.rs`, `reader/fixed_size.rs`, `optimiser.rs`,
`optimiser/adam.rs`. Runtime Java inference and the production baseline are unchanged.
