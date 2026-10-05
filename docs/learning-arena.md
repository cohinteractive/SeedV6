# Arena: learning campaigns

Learning Arena compares two learning trajectories at equal information and exposure.
It uses the production evaluators, trainers, search and paired-game runner. It never
initializes Best, makes promotion decisions or prunes campaign checkpoints. Ordinary
Play and Network Training retain their existing workflows and score behavior.

## Operation

Open **Arena > Setup**. The current mode is a learning campaign: compare initial
snapshots, train both on the same tranche, compare again, and repeat. Select each
competitor's architecture, name and recipe independently. Supported trainers are
**NNUE (material parity)**, **NNUE (legacy, no material)** and **BRN-3**.

Choose **Fresh** with an initialization seed, or **Select model...** to use the
common Architecture / Lineage / Generation browser. Best is frozen to its concrete
checkpoint. An existing snapshot's complete model and optimizer are copied into
an independent campaign lineage; the original store is unchanged. The initialization
seed is disabled for copies. An explicit learning rate overrides only that rate;
**Use checkpoint / architecture rate** inherits a copied rate or a fresh default.
NNUE defaults to .001 and BRN-3 to .003. Minibatches remain competitor-specific.

New campaign lineages have independent stable identities and appear in the model
library once initialized. Generations start at campaign Gen 0, with the original
source generation and binding preserved in campaign configuration. Campaign stores
have Latest but no Best: they can be selected explicitly in Play and for new Arena
experiments. Ordinary Training still requires its accepted bootstrap/Best lifecycle;
registration never invents promotion evidence for a campaign store.

Material-parity NNUE adds the same fixed STM material as BRN-3: P=1,N=3.2,B=3.3,
R=5,Q=9,K=0, at 100 engine units per pawn. The incremental fixed term remains
present during training and search. The learned NNUE contribution keeps its
original V1 scale. New lineages train the combined score in pawn units through
the existing WDL link and cross-entropy loss (E013). This prevents outcome-sized
residuals from overwhelming the useful material prior. Gen-0 search scores stay
identical. Historical material v1 lineages and exact copies retain their original
linear-outcome objective; updating the app does not change their weights or recipe.
Start a fresh material-parity model with the same seed to test corrected training.
See [NNUE training semantics](training/NNUE_CORPUS.md).

Historical `NNUE` campaigns/checkpoints remain **legacy, no material**; Resume does
not convert them. Material parity persists as `NNUE_MATERIAL` with distinct model
and optimizer formats. A legacy campaign is not evidence for material-parity
strength. Start a fresh campaign to use the corrected practical baseline. The
configuration and status display identify both variants explicitly.

Select the shared source from the reusable **Training Data** library. Register a
Lichess JSONL/PZstandard file, existing Seed data directory, or BT4 BINP/Zstd source
once. BINP requires explicit confirmation of `BT4_Q_V1` and completed preparation;
both NNUE variants and BRN-3 support that profile. The picker displays readiness,
known counts and incompatibilities. Saved campaign descriptors stay frozen even
if a library entry is later relocated. Acquisition starts at raw ordinal zero,
never wraps at EOF, and has its own cursor independent of ordinary lineages.

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
campaign round, even though this mode normally makes their numbers coincide.

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

**Live** shows the existing read-only chess board with exact White/Black architecture,
lineage and generation bindings. Reversed games swap both bindings. Move scores
retain the searching model's units and are not fresh evaluations or win probabilities.
Training uses the shared optimizer-progress view: actual rate, batch/epochs,
sample visits/target, updates, step, loss, elapsed segment and campaign exposure.
After Resume, throughput covers only that resumed segment. Unknown historical
metrics remain unknown; opening a saved campaign does not fabricate live telemetry.

**History** shows both named models and generations, W/D/L, score, round winner,
campaign exposure and unscored-pair counts. Selected snapshot details expose exact
checkpoint, store and tranche identities. The 100% stacked columns give the first
model the lower share and the second the complementary upper share, with a visible
50% parity line. Draws split points. Only completed rounds with valid pairs produce
bars; pending and wholly unscored rounds remain empty. Capped pairs never become
draws. If some pairs are capped, scores use valid pairs only, with the omitted
pair count visible. These are experimental match results, not promotion decisions.

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
