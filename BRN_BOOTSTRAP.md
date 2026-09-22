# BRN bootstrap with an NNUE generator

This work follows the human remediation gate on 2026-09-22. The user accepted
per-architecture folder persistence, Resume for NNUE/BRN-0/1/2, opening the
originally misidentified BRN-2 store, and materially faster incremental BRN-2
inference. The depth-4 / six-thread untrained BRN-2 run still stalled severely.
Those accepted remediation changes were inspected and isolated as commit
`fc21935` (24 files on `f1026a8`); 26 focused regressions passed before committing.
Inherited untracked `app/bin/` was excluded and preserved. Neither exact root
`CODEXLOG_CURRENT.md` nor `VERSION_STATE.txt` exists.

## Model responsibilities and targets

`TrainerConfig.architecture` and `NetworkTrainingState` identify the student.
`TrainingSource` separately selects ordinary self-play or NNUE bootstrap.
For bootstrap, `TrainerService` gives only the pinned `NetworkModel.Nnue` to
the unchanged `SelfPlayBatch` / `SelfPlayRunner` search path. The BRN student is
used for optimizer updates and direct held-out prediction, never for bootstrap
game search. NNUE Best is not copied into, or promoted as, the student's Best.
BRN models and exact Adam state retain their existing architecture-specific codecs.

`TrajectorySampler.Sample` already stores a board and terminal result, without
coupling the target to the generating evaluator. Its definition is unchanged:
`-1 / 0 / +1` from that sampled position's side to move. Incomplete/capped games
supply no training targets. No teacher-score distillation, new architecture,
search-policy change, quiescence change or NNUE-training change is introduced.

## Configuration and source selection

The existing architecture-specific initial learning-rate controls remain.
BRN configuration adds two source choices, Bootstrap with NNUE and Self-play
with BRN, plus an NNUE Generator Store field shown for bootstrap. The student
folder is labelled BRN checkpoint store (student). NNUE hides the entire BRN
source panel. Game-pair count is disabled for bootstrap.

New BRN GUI folders default to bootstrap. Existing stores load the durable
selection; legacy stores without a selection remain self-play. Unspecified source
in the service API restores the stored selection on Resume, so older callers
cannot silently switch a bootstrap lineage back to BRN search. A new fresh BRN
request with an unspecified source requires explicit NNUE selection; ordinary
fresh self-play callers specify `TrainingSource.SELF_PLAY`. Apply saves a source
draft bound to its architecture and exact student
folder, plus the generator selection; a queued save cannot reinterpret another
architecture's folder. Start resolves pending asynchronous folder inspection.
All source fields lock through startup, running, stopping and close.

Generator selection uses `CheckpointStore.readBestSnapshot`, including the
existing acceptance-chain, architecture, checksum and codec validation. No
Candidate/latest fallback exists. Missing, corrupt and non-NNUE sources fail
explicitly. Canonical existing paths/ancestors are checked to reject identical,
nested or aliased student/generator locations. The generator remains an external
NNUE store; no NNUE payload is written into BRN stores. Selecting a store does not
prove it is strong: the user must choose the intended trained NNUE Best.

## Partition and promotion

Existing defaults collect up to 32 evenly spaced positions per completed game,
so 64 games can supply up to 2,048 samples. `BootstrapPartition` shuffles the
indices of completed games that supplied samples using a new independent,
generation-derived HOLDOUT seed. It reserves `max(2, ceil(games / 5))` whole games.
All samples from a reserved game stay out of updates. The other games train the
Candidate in the existing one-pass online BRN optimizer.

At least four completed sampled games are required: two training and two held
out. A normal fully completed 64-game generation holds out 13 games, rather than
making a zero/one-example decision. Short smoke fixtures can use the minimum;
they establish mechanics, not statistical power or strength. Common positions
may occur in different games; the split prevents within-trajectory overlap, not
all repeated chess positions. Each generation has a fresh holdout; this is not a
permanent unseen benchmark for reporting generalization or comparing strength.

Candidate and incumbent BRN Best each predict the same ordered held-out list
using their existing normalized BRN inference and mean `0.5 * (prediction-target)^2`.
Strictly lower Candidate loss promotes. Higher loss or exact equality retains
Best. Latest Training remains the Candidate in either case, preserving the
existing continuous learning semantics. There is no loss threshold that switches
the source automatically and no significance/game-strength claim.

`ValidationRecord` has an explicit `brn-terminal-wdl-heldout-v1` alternative to
the unchanged legacy game-pair encoding. Evidence includes both checkpoint IDs,
generator store/checkpoint/model hash, partition-data hash, split seed, game and
sample counts, both losses and the deterministically derived decision. Acceptance
and recovery use the same existing durable promotion chain. Game-pair fields are
absent for loss evidence. Schema-2 history rows identify bootstrap WDL loss;
schema-1 history remains readable and byte-compatible when written. Dashboard,
diagnostics and history show loss without fabricating wins or game scores.

## Durability, Stop and transition

`training-source.bin` stores the mode and external generator path under student
store ownership. Before search, `bootstrap/<parent>.plan` pins the NNUE checkpoint
ID/model hash, incumbent, next generation, generation settings and split seed.
The immutable NNUE actor stays pinned even if the external Best changes. The next
generation selects the then-current Best.

After generation, a checksummed `bootstrap/<parent>.data` contains the exact
training/holdout partition, original generation time and game accounting. It is
published before updates. A stop before Candidate publication discards partial
optimizer work and restarts from the exact durable parent. Completed generation
data is reused, preserving exact training replay even with parallel search.
If generation itself was interrupted, the same checkpoint and indexed seeds are
used again; parallel search retains its existing scheduling nondeterminism.

After Candidate publication, held-out prediction and the decision boundary drain
even on Stop. An interrupted publication/decision is resolved from durable plan,
data and/or validation evidence before more learning. Recovery does not require
NNUE search when the data already exists. Missing/corrupt required evidence fails
closed. If the external pinned checkpoint is removed before data publication,
restore that exact checkpoint to resume; there is no fallback or payload copy.

Unchanged settings preserve the unfinished plan and exact continuation. The
stopped-reconfiguration correction below allows deliberate setting/source changes
to abandon only unfinished work and restart the same generation from its settled
parent. Selecting Self-play with BRN restores BRN game generation and the existing
game-pair validation. Earlier loss history is never reinterpreted.
Recovered decisions retain their durable validation records; as in the existing
lifecycle, analytics do not invent unavailable historical run timings.

If completed search produces fewer than four sampled games, the attempt fails
before training. Its pin is atomically preserved as an `.insufficient-plan` audit
artifact, freeing the configuration boundary. Increase game counts or maximum
plies and Resume to start a new attempt, which explicitly selects current NNUE
Best again. No Candidate, optimizer update or fake validation is published.

Sample records are bounded to 64 MiB each and retained for audit/replay. Normal
64-game/32-sample batches are roughly 112 KiB of sample payload. This first unit
does not add sample-retention maintenance. A fixed NNUE source/checkpoint and
equivalent seed/settings allow independent BRN-family comparisons without a
shared mutable RNG. An externally changing NNUE Best can change later generation
data; the recorded generator identity makes that visible.

## Stopped reconfiguration

Human testing accepted the NNUE-bootstrap implementation apart from the old pin
guard, which required finishing an interrupted generation before changing its
settings. That guard is now preceded by explicit generation reconciliation for
all four architectures and both BRN source modes.

Before search, `generation-attempt.bin` records the settled training parent,
incumbent Best, generation number, effective settings and source. Identical
settings preserve existing exact Resume: bootstrap reuses its pin and durable
partition; ordinary self-play retains its existing regeneration/replay behavior.
The existing parallel-search scheduling limits on reproducibility are unchanged.

Changed effective settings restart only an unfinished attempt. The replacement
starts from the settled parent's exact model, optimizer moments, step and stored
learning rate, using the same next-generation number. It generates new samples;
bootstrap pins the selected NNUE store's current Best anew. Changes include:

- Self-play depth, threads, games, opening bounds, sample limit, game-ply limit
  and score mapping; master seed and starting position.
- Training epochs, minibatch size and shuffle settings where applicable.
- BRN source mode and generator path.
- Ordinary self-play validation pairs, opening bounds, depth, threads, ply bound,
  score mapping and promotion-policy settings. Bootstrap has no game-pair gate,
  so those inapplicable validation settings do not restart it.

Run generation limits are safe to change without restart. The GUI's architecture
learning-rate fields are explicitly **Initial learning rate**: they apply only
to fresh lineages. Changing them while resuming does not change the effective
optimizer rate and does not restart work. This fix adds no optimizer retuning API.
Network architecture continues to select its own store and is not a restart input.

Before superseding work, a newly selected bootstrap generator is validated.
Under the existing exclusive store lock, a checksummed `generation-restart.bin`
intent records the replacement and hashes of exclusively owned old artifacts.
Existing forced atomic publication/move helpers archive the old attempt, pin,
sample data and any undecided Candidate beneath
`restarted-generations/<unique-attempt>/`. Candidate payload ownership also excludes
concurrent readers. Latest-training is restored to the settled parent, source and
replacement attempt are published, then the intent becomes the archive receipt.
Best, earlier validations, promotion evidence and history are untouched. Archived
payloads are outside checkpoint discovery, generation counting and retention.
Unrelated files and failed staging evidence are preserved; unexpected files inside
a Candidate prevent archival rather than being moved or deleted.

Opening the store completes an interrupted restart idempotently before normal
reference recovery. Tests inject failures before and after eight atomic boundaries,
including pin/data/Candidate archival, reference replacement and the final receipt.
Hashes reject changed evidence. Archives are retained for audit; repeated abandoned
Candidates can consume disk space. This unit adds no archive-retention policy.

Before Candidate publication, partial updates are discarded just as in existing
Stop handling. After publication but before a durable validation decision, a
changed configuration archives that Candidate too. Once validation has committed
a decision, recovery completes that decision under its recorded settings and
preserves the settled generation; new settings apply to the following generation.
Normal Stop during validation already drains to this decision boundary. No accepted
generation is rolled back, and no existing history row is rewritten or duplicated.

The status line and Diagnostics explicitly report a restarted unfinished generation
and its settled parent. Unchanged Resume keeps its normal wording. Existing depth
confirmation remains; no new modal or configuration control is introduced.

Compatibility: previous-release bootstrap pins already contain the settings needed
for unchanged Resume or restart, even without an attempt record. Older ordinary
self-play stores did not persist full generation settings: an already-published
legacy undecided Candidate exposes only depth and stored source for comparison;
unknown historical settings are not guessed. Unpublished ordinary work had no
durable samples/updates and already regenerates from its settled parent. New
attempts persist all effective settings, including validation, for future resumes.
Public checkpoint writers can publish a later Candidate without an attempt record.
Recovery supersedes a stale attempt only after proving its generation was settled
in that Candidate's lineage, then uses the known legacy Candidate metadata.

The correction changes 14 files: `TrainerConfig`, `TrainerService`,
`CheckpointStore`, `BootstrapPlan`, `TrainingController`, `TrainingPanel`, the new
`GenerationAttempt` and `GenerationRestart` classes; `BrnBootstrapTest`,
`TrainingControllerTest`, the new `StoppedReconfigurationTest` and
`GenerationRestartTest`; and this report plus `README.md`. No network, search,
target, score-mapping or promotion-policy implementation changed.

The final focused lifecycle/checkpoint/controller/BRN-GUI selection passed **65
tests** in **3m 4s**. There are **37 added test cases**: 27 stopped-reconfiguration
cases, nine restart-transaction cases (including 16 injected crash scenarios), and
one controller notice case. Existing bootstrap assertions now expect restart when
an unfinished generation's source changes; exact-resume coverage remains passing.
Coverage includes all three BRNs, ordinary NNUE, generation/training/decision
interruption, both mode transitions, generator changes, preserved promoted and
retained history, restored optimizer bytes, same generation numbering, legacy
pins, invalid replacement sources and exclusion of fresh-only learning rates.

The final unfiltered `.\gradlew.bat :app:fullCheck --console=plain` passed on
2026-09-22 in **25m 36s** (exit 0):

| Suite | Test cases | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| Routine (149 suites) | 860 | 0 | 0 | 0 |
| Slow NNUE / persistence / lifecycle / GUI (14 suites) | 82 | 0 | 0 | 0 |
| **Full gate** | **942** | **0** | **0** | **0** |

Counts were read from the final JUnit XML. The local build logs are
`app/build/stopped-reconfiguration-compatibility-final.log` and
`app/build/stopped-reconfiguration-full-gate-final.log`. Whitespace validation
passed, including the new files. Controller/diagnostic behavior and native GUI
regressions passed automatically; no separate manual visual check or long training
campaign was performed for this correction. The bounded original bootstrap timing
measurements below are historical, not measurements of this lifecycle correction.

## Original bootstrap validation and bounded measurements

The initial focused selection passed 53 tests, zero failures/errors/skips. It
included real two-generation BRN-0/1/2 bootstrap lifecycles, pinned NNUE-only search,
no game-arena bootstrap validation, train/holdout exclusion, loss promotion and
retention/ties, exact optimizer replay, stops at every lifecycle phase, interrupted
decision recovery followed by deliberate self-play, source/path rejection,
partition/target semantics, legacy GUI self-play, and per-architecture folders.

The final unfiltered `:app:fullCheck` passed on 2026-09-22 in **27m 42s**:

| Suite | Test cases | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| Routine (147 suites) | 823 | 0 | 0 | 0 |
| Slow NNUE / persistence / lifecycle / GUI (14 suites) | 82 | 0 | 0 | 0 |
| **Full gate** | **905** | **0** | **0** | **0** |

This includes all 29 cases in the four new bootstrap test classes. The focused
history regression selection also passed 8/8 before the final full gate. Native
Swing configuration and completed-training screenshots were generated by the
real GUI lifecycle test and visually inspected: separate source fields, loss
decision, no fabricated game W/D/L, and no clipping. `git diff --check` and an
explicit whitespace/final-newline check of all 13 new task files passed.

The bounded smoke then passed in **27 seconds** of Gradle wall time, using one
deterministic NNUE fixture (initialization seed 17), depth 4, six search threads,
eight games per generation, training seed 71 and learning rate 0.001 for all
students. Each family completed two generations. Each generation completed all
eight games and produced 24 terminal samples: **18 training / 6 held out**, from
six training / two held-out games. All six Candidates were published and promoted
by lower held-out loss. BRN-2 stopped after generation 1, closed its service, then
resumed with a new service and an unspecified source; the stored bootstrap mode,
generator and exact optimizer were restored for generation 2.

Machine: Windows, AMD Ryzen 5 5500 (6 cores / 12 logical processors), OpenJDK
21+35-2513, smoke JVM `-Xms512m -Xmx1024m`. One common generator checkpoint was
`g000000-s000000000-b2c939a8d30037e8eb049a6815eef706f63136e75001dd47051c4b4178a5988c`.

All timings below are **milliseconds**; losses are rounded for display. Full
precision losses and generator identity are stored in each student's history.

| Student | Gen | Generation | Training | Validation | Total | Candidate loss | Best loss | Decision |
|---|---:|---:|---:|---:|---:|---:|---:|---|
| BRN-0 | 1 | 225.022 | 5.732 | 2.693 | 983.845 | 0.477457495 | 0.500000000 | Promote |
| BRN-0 | 2 | 75.900 | 0.686 | 0.067 | 738.697 | 0.451152582 | 0.477457495 | Promote |
| BRN-1 | 1 | 68.082 | 4.991 | 0.931 | 2854.019 | 0.418402551 | 0.505739244 | Promote |
| BRN-1 | 2 | 67.430 | 1.673 | 0.119 | 2921.055 | 0.336019430 | 0.418402551 | Promote |
| BRN-2 | 1 | 65.425 | 4.548 | 0.853 | 5067.852 | 0.322387699 | 0.496709068 | Promote |
| BRN-2 | 2 | 65.584 | 2.543 | 0.194 | 4929.937 | 0.143028495 | 0.322387699 | Promote |

Generation measures actual NNUE self-play/search and sampling. Training includes
the existing BRN training metrics and updates. Validation measures the direct
Candidate/Best prediction pass. Per-generation total also includes model/state
loading, sample/checkpoint persistence and decision publication, so it
is larger than the three measured phases; it excludes service startup/recovery
and the subsequent history-row append.
The larger BRN payloads retain substantial persistence cost. The first BRN-0
generation includes cold JVM/search effects. These are single bounded observations,
not architecture speed rankings or timing assertions. The warm generation range
of 65-76 ms and the NNUE-only actor assertions demonstrate that BRN inference cost
does not control this fixture's search.

The legal queen-endgame fixture is `7k/8/5K2/8/8/8/3Q4/8 w - - 0 1`. It exercises
real search, terminal targets, updates and durable decisions, but its short,
repeated trajectories do not establish normal-position throughput, generalization
or playing strength. The user-authorized fresh-store GUI pilot remains necessary.
No long campaign or longer architecture comparison was run.

Local run artifacts (under ignored `app/build/`) are
`brn-bootstrap-full-gate.log`, `brn-bootstrap-smoke.log`,
`brn-bootstrap-smoke-final/`, and
`gui-smoke/brn-bootstrap-{configuration,dashboard}.png`. All required automated
checks completed; the next human gate below remains outstanding.

Coverage maps to the requested contracts as follows:

| Contract | Automated evidence |
|---|---|
| Configuration, separate paths, restart persistence, fresh/legacy defaults, active controls | `BrnBootstrapGuiTest`, `BrnBootstrapTest`, existing `TrainingFoldersTest` |
| NNUE-only source, rejected folders left untouched, immutable per-generation Best pin | `BrnBootstrapTest` invalid-source and changing-generator cases |
| BRN absent from bootstrap search and game-arena validation | Real two-generation lifecycle case for each family asserts the actor entering actual self-play is `NetworkModel.Nnue`; a BRN game-validation call fails the test |
| Terminal side-to-move WDL, deterministic whole-game split, no held-out updates | `BootstrapPartitionTest`, optimizer-step/sample-exclusion assertions in all three real lifecycle cases |
| Same held-out examples, existing half-squared error, lower/worse/tie decisions | `HeldOutLossTest`, durable promotion/replay cases in `BrnBootstrapTest` |
| Explicit history mode and retained legacy/future-schema behavior | Lifecycle history assertions and `HistoryRepositoryTest` |
| Stop/restart/Resume, exact optimizer, interrupted decision, deliberate source transition | `BrnBootstrapTest` phase-stop, exact-replay and recovery cases |
| Ordinary BRN training and game-pair validation | Existing `BrnTrainerServiceTest`, `Brn1TrainerServiceTest`, `Brn2TrainerServiceTest` and their GUI suites |
| NNUE, checkpoint/recovery, controller and evaluator-selection regressions | Unfiltered repository `fullCheck`, including its separately tagged slow NNUE suite |
| Original store recognition and per-architecture folder remediation | Existing `NetworkArchitectureTest`, `TrainingFoldersTest` and BRN GUI/store suites |

Development validation caught and corrected a temporary-fixture initialization
issue, Apply-before-asynchronous-source-load behavior, and the legacy history
test's now-supported schema-2 sentinel. Unsupported future history still blocks
append; damaged known schema-1/2 rows remain preserved and diagnosable while later
valid rows can be recorded. An earlier full-gate attempt was intentionally
interrupted to include the insufficient-data retry safeguard; it is not counted
as a passed gate.

Reproduce the repository gate:

```powershell
.\gradlew.bat :app:fullCheck --console=plain
```

Reproduce only the bounded real smoke into a new output directory:

```powershell
.\gradlew.bat :app:brnBootstrapSmoke -PbootstrapSmokeRoot=app/build/brn-bootstrap-smoke-new --console=plain
```

An optional `-PbootstrapGenerator=<existing NNUE store>` uses its Best. Without
that option the driver creates a deterministic NNUE fixture. It runs depth 4,
six threads, eight games per generation, two generations per family, one common
generator and a bounded legal queen endgame. BRN-2 uses separate service instances
for Stop/restart/Resume. It prints generation, training, direct validation and
total times separately; no wall-clock threshold is an automated assertion.

## Files in the original bootstrap work unit

All paths below are relative to `app/src/` unless otherwise specified. The BRN
architecture/optimizer implementations and search algorithms are unchanged by
this unit; the earlier remediation is in its own commit.

| Area | Files |
|---|---|
| GUI | `main/java/com/ohinteractive/seedv6/gui/BrnTrainingSourcePanel.java` (new); `TrainingController.java`, `TrainingDashboard.java`, `TrainingDashboardModel.java`, `TrainingHistory.java`, `TrainingPanel.java`, `TrainingProgress.java`, `TrainingSettings.java` in the same package |
| Service | `main/java/com/ohinteractive/seedv6/training/service/TrainingSource.java` (new); `TrainerConfig.java`, `TrainerService.java`, `TrainerSnapshot.java` |
| Samples and loss | `main/java/com/ohinteractive/seedv6/training/selfplay/BootstrapPartition.java` (new); `main/java/com/ohinteractive/seedv6/training/validation/HeldOutLoss.java` (new) |
| Checkpoints | `main/java/com/ohinteractive/seedv6/training/checkpoint/BootstrapPlan.java`, `BootstrapData.java`, `BootstrapEvidence.java` (new); `CandidateLifecycle.java`, `CheckpointInspection.java`, `CheckpointPruner.java`, `CheckpointStore.java`, `SmallRecord.java`, `ValidationRecord.java` |
| History | `main/java/com/ohinteractive/seedv6/training/history/GenerationRecord.java`, `HistoryCodec.java`, `HistoryRepository.java` |
| Bounded smoke | `main/java/com/ohinteractive/seedv6/tools/search/BrnBootstrapSmoke.java` (new); repository `app/build.gradle` |
| GUI tests | `test/java/com/ohinteractive/seedv6/gui/BrnBootstrapGuiTest.java` (new); `BrnTrainingGuiTest.java`, `Brn1TrainingGuiTest.java`, `Brn2TrainingGuiTest.java` |
| Lifecycle tests | `test/java/com/ohinteractive/seedv6/training/service/BrnBootstrapTest.java` (new); `BrnTrainerServiceTest.java`, `Brn1TrainerServiceTest.java`, `Brn2TrainerServiceTest.java` |
| Sample/loss/history tests | `test/java/com/ohinteractive/seedv6/training/selfplay/BootstrapPartitionTest.java`, `test/java/com/ohinteractive/seedv6/training/validation/HeldOutLossTest.java` (new); `test/java/com/ohinteractive/seedv6/training/history/HistoryRepositoryTest.java` |
| Documentation | Repository `README.md`, `BRN_BOOTSTRAP.md` (new) |

## Human validation status

The user subsequently accepted the NNUE-bootstrap work apart from the stopped
reconfiguration defect described above. The original fresh-store GUI pilot is
therefore historical context, not a request to repeat it for this code correction.

Human actions required after this prompt: None for this bounded implementation.
An optional GUI spot-check can repeat Stop at an unfinished generation, change
depth/source, Resume, and inspect the restart notice; unchanged Resume should keep
its exact-continuation behavior. Automated results do not grant user acceptance
of the correction or authorize a longer experiment. Any longer controlled BRN
comparison remains a separate user/GPT planning decision; no such campaign,
strength tuning, architecture change or automatic transition is part of this fix.
