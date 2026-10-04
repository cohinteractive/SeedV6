# Learning Arena V1

Learning Arena compares two learning trajectories at equal information and exposure.
It uses the production evaluators, trainers, search and paired-game runner. It never
initializes Best, makes promotion decisions or prunes campaign checkpoints. Ordinary
Play and Network Training retain their existing workflows and score behavior.

## Operation

Open the **Learning Arena** main tab. Configure A and B independently as NNUE or
BRN-3, with a name, fresh model seed and minibatch size. V1 starts fresh; importing
an existing checkpoint is not offered. NNUE uses its existing initializer and Adam
defaults (learning rate .001); BRN-3 uses its existing material/residual initializer
and masked Adam defaults (.003). No evaluator mathematics or score calibration is
changed. NNUE versus NNUE and BRN-3 versus BRN-3 use the same campaign controller.

Choose one shared Lichess JSONL/PZstandard source or existing Seed corpus directory.
Source acquisition is sequential, through `SourceReaders`, starting at raw ordinal
zero. It never generates training games or wraps at EOF. V1's GUI does not register
BT4 sources: ordinary NNUE does not support those labels. The campaign source is
independent of ordinary training lineages and their cursors.

Set positions per round and the shared epoch count. Epochs are repeated exposure,
so both competitors use the same count; minibatches and optimizers remain independent.
The opening/shuffle seed determines reproducible openings and training shuffles,
not corpus selection. Set a finite number of training rounds, or zero to run until
paused. Round 0 is additional to this limit.

**Start new** creates a uniquely named campaign under
`<Base Training Root>/learning-arena/<timestamp-id>/`. The base is the existing
**File > Training storage settings** selection. The status area shows the exact
campaign path. **Open campaign...** loads saved configuration and history without
starting work; **Resume** uses that saved configuration. Edits are for a new campaign,
not a way to change an existing experiment. **Pause safely** and application shutdown
request cancellation, save optimizer progress at a safe boundary and join the worker.
Pause can take time while the current minibatch, checkpoint write or source read finishes.

## Round and persistence protocol

The campaign has its own OS writer lock and atomic `campaign.json` state. Each
competitor has an isolated ordinary `CheckpointStore` (`A/`, `B/`) using the existing
model, optimizer, manifest and payload codecs. `publish` is used, never `initialize`,
promotion or Best recovery. Native checkpoint generation is recorded separately from
campaign round, even though fresh-only V1 normally makes their numbers coincide.

The state machine derives the next action from durable receipts:

1. Round 0: initialize/publish A, initialize/publish B, run the initial paired arena,
   commit its results. Exposure is zero for both.
2. Round N: select and publish the shared tranche; train/publish A from its previous
   checkpoint; train/publish B from its previous checkpoint; run the paired arena;
   mark the round complete. Only then create round N+1 or mark the campaign complete.

Statuses are READY, RUNNING, PAUSED, FAILED and COMPLETE. A saved RUNNING state after
a process interruption is resumable. COMPLETE Resume is a no-op. Failures keep prior
receipts and show a diagnostic; Resume retries only incomplete work after its cause
has been addressed. Disk/identity/persistence failures never authorize advancing a stage.

For each tranche, eligibility is the intersection of the two existing source-specific
target policies. In the intended NNUE/BRN-3 comparison this excludes mate labels and
out-of-range CP labels. Both policies must produce the exact same binary64 target for
every retained position. A disagreement stops acquisition. No target normalization is
introduced. The tranche stores absolute chess position/rule fields, raw labels,
resolved targets and source ordinals, plus a SHA-256 manifest binding the campaign,
source fingerprint, raw range and record count. Its complete directory is atomically
published **before either trainer runs**. Both use this same immutable artifact, with
their existing feature encoders. A frozen tranche can finish even if its original
source is temporarily unavailable. The source must be restored to its registered
location and fingerprint to select the next tranche. There is no implicit relocation
or substitution. Duplicate source positions are not deduplicated; counts mean records.

Optimizer state and the exact shuffle/minibatch cursor are saved together through a
checksummed atomic reference, approximately every 30 seconds at optimizer boundaries,
and unconditionally at safe pause and training completion. Each new save supersedes
only the previous partial optimizer payload. A crash can recompute the uncommitted
interval from the last saved state; it does not apply those updates twice. Resume of a
fully trained partial publishes the checkpoint without repeating optimizer updates.
If checkpoint publication succeeded before the campaign receipt was written, its
deterministic checkpoint identity makes publication idempotent. Completed A training
is skipped when B resumes. Final checkpoints and frozen tranches remain retained.

Each completed game is durably recorded before its reversed-colour partner or next
pair starts. A crash can replay an in-flight, uncommitted game, but never a game with a
completed receipt. Opening identities are verified on continuation. A final saved
arena is marked complete before round advancement, so restart cannot advance twice.
Snapshot writes use forced files and atomic renames, without non-atomic fallback.
As with existing checkpoint storage, Windows may not support directory forcing;
this is process-crash consistency and corruption detection, not a universal guarantee
against power loss. Preserve the entire campaign directory when moving or backing up.

## Matches and results

Every arena uses the same starting FEN and seeded legal opening sequence across rounds.
Each opening is played twice with A's colour reversed, through `ValidationArena` and
`HeadlessGame`. Each colour has private evaluator/search state and the production
`SearchDriver`/`ProductionSearch` implementation. Search threads apply per search;
games run sequentially. One thread gives the strongest deterministic control.

Depth mode completes the configured depth. Time mode uses a per-move deadline and
the last fully completed iteration, up to the existing maximum depth. A budget too
small for even one completed iteration is an explicit failure, not a substituted move
or draw. Fixed depth is one controlled comparison, not perfect isolation of evaluator
quality; fixed time includes evaluation cost and scheduling effects.

History records per-round checkpoint identities, selected-record counts and
total consumed records including epochs, plus game results and A's paired W/D/L and
score percentage. Scores include only pairs with two actual chess results, following
the existing runner's accounting. Ply caps remain unscored, not adjudicated draws;
unscored pair counts are shown. The table provides longitudinal access without a new
chart subsystem. Live telemetry shows available depth, nodes and elapsed search time.

## Validation and boundaries

`LearningArenaTest` exercises shared target/record equality, source-independent frozen
resume, corrupt-tranche rejection, exclusive ownership, byte-exact optimizer resume,
checkpoint-publication interruption, per-game interruption, successive tranches,
fixed-time arenas and a tiny fresh NNUE/BRN-3 Round 0 → training → Round 1 smoke run.
`LearningArenaGuiTest` covers independent controls, EDT/worker ownership, saved-result
loading and native Swing rendering at two window sizes (desktop test skips headless).
Existing validation, promotion, Play-controller and corpus-training tests provide
regression coverage. These short checks establish execution, not comparative strength.

Run the bounded campaign/controller checks with:

```powershell
.\gradlew.bat :app:test -Pheadless --tests '*LearningArena*Test'
# Native tab/render check, on a desktop:
.\gradlew.bat :app:test --tests '*LearningArenaGuiTest.mainTabShowsAndRendersAtSupportedDesktopSizes'
```

There is no external Mac experiment, score calibration, reference opponent, statistical
acceptance, automatic seed replication, distributed training or checkpoint pruning.
Long campaigns require storage for every completed checkpoint and tranche. No long
strength run or broad benchmark is part of this feature's validation.
