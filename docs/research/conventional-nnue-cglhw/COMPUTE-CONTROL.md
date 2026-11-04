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

### Explicit completed-run finalization (4 November 2026)

The owner-authorized training-infrastructure maintenance adds a **prospective,
opt-in** lifecycle to new training outputs. It is separate from research acceptance,
Search, and application build-version management. It does not reopen the closed
[historical cleanup](STORAGE-CLEANUP-20261104.md), adopt existing directories, or
change frozen protocols. An existing requirement to retain a particular checkpoint
still binds: retain it explicitly, or resolve the actual conflict before finalizing.
In particular, do not apply this to historical R057's no-checkpoint-deletion
protocol, suspended successor work, R010 data, production assets or user stores.

For a separately authorized **new** run, include
`"training_finalization": "explicit-v1"` in its immutable configuration, using a
new output directory with an existing parent. Run and resume through the usual
`nnue-control.cmd train CONFIG`. The runner creates `training-run.json` (unique
run ID, exact root/config identity and lifecycle state), `training-config.json`
(original config bytes) and `training-run.lock`. Ordinary configs without this
field keep their existing behavior. No automatic cleanup occurs.

The per-run lease covers training and known managed `resume`/`initial_state`
sources. Interrupted work stays resumable with the same config and pinned trainer.
Completion requires every requested superbatch, valid receipts, weights/AdamW
geometry, immutable recipes and successful segment execution. Failed/partial
attempt directories remain evidence. Completion alone neither selects a model nor
authorizes deleting checkpoints. Rerunning a completed/finalizing/finalized output
gets a terminal-state diagnostic before the controller's checkpoint-resume scan.

Inspect `training-run.json` for the ID and state; inspect the existing receipts,
metrics and conclusions to settle selection. Use `evaluate_checkpoint.py` through
the existing compute supervisor to produce independent exports **before cleanup**.
Place/copy each required evaluation directory inside the run, for example
`RUN/exports/final/`, preserving `network.cnnue`, its `.cnnue.json` metadata,
`parity.json`, `validation.json`, manifest and associated evidence. Finalization
validates the envelope, dimensions, embedded SHA-256, source receipt/payload hash,
exact quantized payload correspondence, passing parity and payload-bound validation
metrics. It does not run new training or infer playing strength. Keep the final
endpoint even when a different earlier model was selected or the result was weak.

Create a compact selection JSON outside the run (or inside it before preview).
Paths below are exact run-relative files using `/`, not globs or directories:

```json
{
  "run_id": "ID_FROM_training-run.json",
  "dependencies_resolved": true,
  "restart_retired": true,
  "reason": "Selection and retention review, with the relevant conclusion/reference",
  "models": [{
    "path": "exports/final/network.cnnue",
    "source": "segments/000064-0123456789ab/run/seed-cnnue-v1-64/quantised.bin",
    "role": "final"
  }],
  "retain": [],
  "evidence": ["exports/final/parity.json", "exports/final/validation.json"]
}
```

The two `true` values are an explicit operator decision, **not defaults**: first
review frozen comparisons, downstream configs/continuations, active consumers and
applicable retention obligations. Copy/preserve relevant conclusions and provenance;
list additional required binary files in `retain`. Other selected/comparison models
use additional `models` entries with role `selected` or `comparison`. To preserve a
full NNUE restart point, list all five checkpoint binaries in `retain`; source
receipts/configurations are always kept. Do not retire a run if any dependency is
unresolved. Historical text mentioning a generation alone is not a live consumer.
There is no global dependency scanner: external/frozen programs must be quiescent,
and their required models must be included in this review. Old binaries cannot
participate in the new lease protocol. Do not bypass it with direct trainers.

```powershell
.\nnue-control.cmd preview-training RUN --selection SELECTION.json
.\nnue-control.cmd finalize-training RUN --selection SELECTION.json --plan-sha256 HASH_FROM_PREVIEW
```

The shared entrypoint, also used by BRN, is:

```powershell
python -B tools/training_lifecycle.py preview RUN --selection SELECTION.json
python -B tools/training_lifecycle.py finalize RUN --selection SELECTION.json --plan-sha256 HASH_FROM_PREVIEW
```

Preview is read-only: it reports exact run identity, target paths/hashes/sizes,
total logical recoverable bytes, retained files and selected outputs. Finalize
recomputes the preview and refuses changed inputs. It removes only the recorded
allowlist: successful-segment initializer tensor copies and unretained checkpoint
`raw.bin`, `quantised.bin`, and optimizer weights/momentum/velocity. It never
recursively removes directories. All configs, receipts, logs, metrics, datasets,
unknown files and failed-attempt evidence remain. Selected neural BRN `.state`
files retain the optimizer bytes required by their existing inference readers.

**Consequences:** deleted intermediate weights/optimizer states are not restorable
by this tool. Exact checkpoint-level replay, hashes and resume from those points
are lost. Preserved runtime exports retain their model identities; GPU retraining
was never promised bitwise identical. Start future training in a new run from a
deliberately retained compatible state. The global compute-control
`latest_checkpoint` is historical progress, not a promise that retired restart
bytes remain; the finalization report identifies the preserved models.

### Refusal, interruption and filesystem limits

Missing/corrupt/ambiguous required outputs, incomplete intervals, suspended or
interrupted state, changed config/receipts, unresolved retention review, duplicate
progress, links/junctions/reparse points, hard links, special files, unsafe relative
paths, or an occupied run lease refuse cleanup. Malformed metadata is not fresh
permission. Preserve it for diagnosis. Historical pruned outputs have no enrollment
record and cannot be finalized/adopted by editing their old configs.

`training-finalization-plan.json` pins the selection and exact before-state;
`training-finalization.json` records confirmed deletions, logical byte counts,
the single in-flight target and any error. These files are compact run evidence,
not replacements for training receipts. Keep them. If a file is locked, release
the actual consumer, then repeat preview and finalize with the **same** hash.
After interruption, `preview RUN` reads the saved plan without requiring selection
again. Repeat `finalize RUN --plan-sha256 HASH` to finish remaining work. A missing
in-flight file is reported separately as `reconciled_absent`, never as a newly
verified deletion; already acknowledged targets must stay absent. Changed retained
outputs, new files, reappearing targets or damaged metadata stop recovery for
inspection. Do not delete manifests, fabricate receipts or recreate discarded state.
An interrupted `.training-*.tmp` metadata publication needs manual provenance
inspection; the tool will not silently discard it. Repeated successful finalization
checks the retained evidence and performs no additional deletion.

The implementation checks ancestors without resolving away links, uses a per-run
OS lease, revalidates the inventory before cleanup and after it, rechecks file
identities between deletions, and hashes the actual opened target. Windows pins
ancestors against renaming and uses a handle opened without write/delete sharing
for deletion. POSIX uses directory-relative `O_NOFOLLOW` opens/unlink and advisory
locking. Use a private local filesystem and cooperative training tools; this is
not an adversarial filesystem sandbox or protection against privileged out-of-band
rewrites, power-loss/storage failure, or network filesystems with broken locking.
Reported bytes are logical file lengths, not a claim about volume free-space delta.

Validation on Windows: temporary-fixture tests exercise NNUE, Pair-2, ONE and four
neural epoch layouts, actual locked files, concurrent processes, hard links and
junctions, changed inputs, partial cleanup/reconciliation, idempotence and exact
byte counts. Java tests check the live cross-language lease and native serialized
models against the independent Python readers; existing optimizer/segment-resume
and owner-control tests also run. The 4 November maintenance validation passed
23 lifecycle tests, 19 focused Java tests and 14 existing compute-control tests.
The final Java run used the worktree-local Gradle home, offline dependencies and
one worker/active processor. No historical data is used destructively and no
GPU training/tournament is needed. Native POSIX/macOS execution is not validated
on this Windows host; run the fixture suite there before operational use. The
existing training supervisors remain Windows-specific.

```powershell
python -B -m unittest discover -s tools -p test_training_lifecycle.py -v
python -B -m unittest discover -s tools/conventional-nnue -p test_compute_control.py -v
# Through the existing supervisor, with worktree-local Gradle home/resource limits:
# :research:test --tests '*TrainingFinalizationTest' and the relevant resume tests
```

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
restarts only the unfinished game from its opening. Subsequent harness invocations
publish flushed PGN/details first and atomically replace the complete game ledger
last, protecting earlier games from an interrupted append. Failed `.pending-*`
writes remain local evidence and are not completion records. Three local fault
tests passed (0.079s): replacement/newlines, failed flush, failed publication/retry.
This change does not retrofit the already-running E008 confirmation process.
Malformed legacy records or storage corruption still require reconciliation;
never infer completion from a partial file. Generic build/validation resume notes
explain safe rerunning.

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
