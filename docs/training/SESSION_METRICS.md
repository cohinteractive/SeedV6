# Session dashboard measurements

Verified against the training and presentation paths on 2026-10-08. These are
measurement contracts, not changes to optimization, promotion, sampling or model
formats.

## Elapsed clocks

Before consolidation (`e793a8d^:TrainingDashboard.java`), **Campaign elapsed** and
**Generation elapsed** occupied the top header, beside the state badge, using
17-point values. They now occupy that position as **Session elapsed** and
**Generation elapsed**, with bold 17-point values and 12-point labels. The compact
status panel, conditional position progress and expanding graphs remain.

`TrainerService.elapsed()` measures monotonic time since this service's Start,
including preparation, acquisition, optimization and validation. Stop, completion
and failure freeze it. The next Start resets it. `TrainingSession` persists a
generation budget, **not elapsed time**: resuming an ACTIVE budget after a crash
does not reconstruct the earlier invocation's duration. A loaded saved session
therefore displays Unavailable until Start, rather than claiming a historical
zero duration.

`GenerationTiming` measures active time in the current generation. A safe stop
saves `PartialGeneration.activeNanos`; resume adds new active time and excludes
stopped time. History settlement freezes it at the recorded total. New generations
start their own clock. Legacy/crash candidate recovery with no original timing
keeps generation duration unavailable. No crash wall-clock subtraction is used.

Loading a partial generation previously threw away its saved duration, sample
count and loss sum, and could identify the previous model as its Candidate. The
read-only stopped view now restores the recorded generation clock, optimizer
step/count, sample visits and running mean. It does not fabricate final fit or
session time. Stale partial records for other generations are ignored. Polling
remains the existing 500 ms Swing timer; no clock thread, checkpoint reads on the
EDT, or new repaint loop was added.

## BRE-Pair 2 mathematics

Let `v` be the trainer's side-to-move output in pawns, `t` the supplied target in
[-1, 1], and `m = clamp(material, 17, 78) / 58`. Material counts both colours with
P/N/B/R/Q = 1/3/3/5/9. The pinned supervision link uses:

```text
a = ((-13.50030198*m + 40.92780883)*m - 36.82753545)*m + 386.83004070
b = (( 96.53354896*m - 165.79058388)*m + 90.89679019)*m + 49.29561889
w = sigmoid((a/b)*(v - 1))
l = sigmoid(-(a/b)*(v + 1))
p = (1 + w - l)/2
q = (1 + t)/2
cross_entropy = -q*ln(p) - (1-q)*ln(1-p)
```

`BrnPair2Trainer.trainBatch` returns this binary expected-score cross-entropy
in **nats per sample**, measured before each batch's update. `Brn3Objective`
computes it using stable log-probabilities, including extreme predictions.
This is not three-class W/D/L cross-entropy.

`BrnPair2CorpusOptimization` adds `batchMean * actualBatchCount`, then divides by
total sample visits. Short final batches therefore have the right weight. All
epochs contribute; the mean spans changing pre-update models, not only the
current weights or the last epoch. A saved training cursor retains both loss sum
and visit count and resumes at a complete optimizer boundary.

Initial and final fit instead evaluate the fixed sampled training set once:

```text
cp = round(100*v)
x = cp*a/100
W = floor(0.5 + 1000*sigmoid((x-a)/b))
L = floor(0.5 + 1000*sigmoid((-x-a)/b))
y = (W-L)/1000
final_fit = sum(0.5*(y-t)^2) / number_of_training_samples
```

This is **mean half-squared target error**, not MSE without the half factor, not
cross-entropy and not percentage accuracy. It uses the trained mutable model's
final weights and a CP/per-mille-rounded outcome link. Running CE uses a smooth
link. The sample population, target adapter and equal per-visit weighting agree
within a generation, but the objective, link rounding, weights at evaluation time
and repetition accounting differ. No scalar conversion can reconstruct one loss
from the other.

Corpus targets are fixed side-to-move outcomes: compatible CP labels use the
pinned Stockfish WDL adapter, and explicit BT4 sources supply BT4 Q. BRE-Pair 2
and BRN-3 skip CP mate examples. A mixed-source generation retains each source's
label profile. Source Q is not interpreted as CP. Held-out evaluation uses a
separately reserved set and immutable model snapshots (including exported weight
rounding); it is not the final training-fit measurement. Runtime search score
calibration is also separate from training fit.

## Generation 66 evidence

Read-only inspection of the selected BRE-Pair 2 lineage
`E:/SeedV6-Networks/BRN-Pair2/235d0688-e060-44e3-8f1e-119c39bc8017`
found no history-decoding/checksum warnings. Checkpoint manifests confirmed the
architecture, generation and full Candidate IDs for both records:

| Record | Stored final fit | Candidate identity |
| --- | --- | --- |
| Generation 66 | 0.1205895841399367 | `g000066-s019578880-f34512ae48293354741130564d3361476dc3659698d7becf8c4b2a6e9056bff6` |
| Generation 32, still Best when inspected | 0.10781458681613643 | `g000032-s001753088-5837c7f7192aa3cf9a265388f93649327455e804541398ae3ef6b69e09014304` |

The six-decimal dashboard values are correct roundings. Generation 66's corpus
receipt records 8,388,608 usable samples, no held-out samples, the
`BRN3_SOURCE_OUTCOME_V1` adapter and both `BT4_Q_V1` and
`STOCKFISH_CP_MATE_V1` source profiles. Its history records eight epochs, batches
of 128 and learning rate 0.01: 67,108,864 visits and 524,288 updates for a complete
generation. Generations 32 and 66 have matching recorded recipes/selection
identity, but their training hashes differ: each fit describes its own samples.
Game-pair validation retained Generation 32; neither displayed fit is a loss
comparison on Generation 66's held-out set.

The remembered ~0.62 was not an exact recorded observation. Completed-generation
history stores **finalLoss only**. `SelfPlayTraining.Statistics` retains running
CE in memory; checkpoint manifests and model/optimizer codecs do not store the
generation's running mean. Safe-stop partials store a sum/count, but the available
partials belong to generations 2, 12, 31 and 62, not 66. Thus Generation 66's
running CE is unavailable. The objective change explains why those scales differ;
there is no evidence of misrecording or wrong model association in its completed
value. Exact replay of all 8,388,608 samples/eight epochs was not performed.

## Architecture contracts and presentation

| Architecture / persisted recipe | Reported running mean | Final training fit |
| --- | --- | --- |
| BRE-Pair 2 | Smooth expected-score cross-entropy | Half-squared error, rounded outcome link |
| BRN-3 | Half-squared error, rounded outcome link | Same measurement on final weights |
| Legacy NNUE | Half-squared error on bounded output | Same objective on final weights |
| Legacy material NNUE | Half-squared error on clipped material + bounded residual | Same objective on final weights |
| Calibrated material NNUE | Smooth expected-score cross-entropy | Same objective on final weights |
| BRN-0 / BRN-1 / BRN-2 | Half-squared target error, online sample updates | Same base measurement on final weights |

BRN-3 optimizes CE while deliberately reporting half-squared error from its batch
API. BRN-2 capture-consistency training reports base error without its auxiliary
Huber penalty. Blended WDL/teacher and CP-scaled targets retain their own meanings.
These differences must not be renamed based only on the optimizer objective.
All current `HeldOutLoss` paths report half-squared target error, including
calibrated material NNUE whose training fit is CE.

The dashboard separates **Live training** from **Completed-model fit** with named
groups and a divider. Bold objective labels precede the values, with explicit
running-mean/final scopes. A completed current Candidate is labeled **Current
model fit**. Identical model roles merge only for identical model IDs and the
same final measurement. Snapshot final fit must belong to the snapshot's current
generation. Best fit is retrieved by the exact Best ID, not by latest row or
numeric generation proximity. No held-out value replaces missing training fit;
Unavailable remains visible, and paired held-out evidence stays in validation.

The service now exposes its already-known final-objective description in the
immutable run snapshot; no persistence schema changed. Historical objective
markers are used where present. Architectures with invariant reported objectives
can be labeled from the lineage architecture. Missing material-NNUE recipe
evidence displays **Objective unavailable**, since that architecture has two
persisted objective variants. Current settings never relabel historical models.

Final fits with matching objectives, targets and recipes can be read together as
descriptive fits on each generation's samples. They are not controlled evaluation
on a shared dataset. A running average is a different measurement from final fit
even when its objective matches. Held-out Candidate/incumbent pairs on the same
recorded set are the supported direct loss comparison. Playing-strength evidence
remains the separate game-pair result; lower fit alone does not establish a
stronger evaluator or comparability across architectures.

## Correction validation and worktree outcome

The focused Gradle selections covered dashboard/model rendering, live/final/Best
lookup, missing metrics, objective variants, lineage loading, controller
transitions, phase failures, safe stop/resume, crash-budget recovery, generation
transitions, BRE-Pair 2 arithmetic and corpus persistence, BRN-3 corpus training,
calibrated NNUE and held-out loss. The new arithmetic fixture independently checks
the CE formula, unequal minibatch weighting, two epochs, final half-squared fit
and exact cursor continuation on three positions.

- First combined selection: 110 cases, 107 passed, two native-only cases skipped,
  one existing 16-generation session-budget test exceeded its 90-second wait.
- Final headless selection: 41 cases, 40 passed, one native-only case skipped.
  This included the previously timed-out test, which passed in isolation within
  its unchanged limit, and the final presentation/recovery changes.
- Native Swing selection: three passed, including both headless skips and the
  workspace resize/navigation/stop/failure smoke test. Added assertions require
  both timers to be fully visible without scrolling.
- Native workspace repeated at 150% FlatLaf scaling: one passed. Dashboard
  fixtures also rendered at logical widths 700/980/1500 and scales 100/150/200%.
  Normal, narrow, high-DPI and native workspace images were visually inspected.
- Across these selections, 111 distinct test invocations ultimately passed.
  `git diff --check` passed. The initial timeout remains evidence of the existing
  session test's tight time margin, not a claim of benchmark stability.

JUnit XML evidence is retained under ignored `app/build/timers-loss-validation/`.
Rendered fixtures are under `app/build/gui-smoke/training-timers-loss/`; native
workspace captures are under `app/build/gui-smoke/100%/` and `150%/`. They are
synthetic QA fixtures, not observations of a newly launched production campaign.
The full suite, long training, full Generation 66 replay, performance benchmarks,
and expensive validation campaigns were intentionally not run. Actual lineage
records were inspected read-only; no production training session was started.

Files changed (paths relative to `app/src`, except this document):

| File | Change |
| --- | --- |
| `main/java/com/ohinteractive/seedv6/gui/TrainingDashboard.java` | Header timers, objective-first grouped metrics, distinct validation labels |
| `main/java/com/ohinteractive/seedv6/gui/TrainingLoss.java` | Architecture/recorded-objective labels and generation association guard |
| `main/java/com/ohinteractive/seedv6/gui/TrainingController.java` | Restore saved partial telemetry without inventing measurements |
| `main/java/com/ohinteractive/seedv6/training/service/TrainerSnapshot.java` | Optional final-objective description in the immutable run publication |
| `main/java/com/ohinteractive/seedv6/training/service/TrainerService.java` | Publish the already-known final objective |
| `test/java/com/ohinteractive/seedv6/gui/TrainingDashboardRedesignTest.java` | Updated explicit-scope/no-held-out-substitution assertions |
| `test/java/com/ohinteractive/seedv6/gui/TrainingLossPresentationTest.java` | Objective, identity, transition, scaling and rendering regressions |
| `test/java/com/ohinteractive/seedv6/gui/TrainingLineagesTest.java` | Real read-only partial reload with preceding Candidate and frozen clock |
| `test/java/com/ohinteractive/seedv6/gui/TrainingWorkspaceSmokeTest.java` | Native timer visibility assertions |
| `test/java/com/ohinteractive/seedv6/training/selfplay/BrnPair2LossTest.java` | Weighted CE, final fit and exact continuation fixtures |
| `test/java/com/ohinteractive/seedv6/training/service/BrnPair2CorpusTrainingTest.java` | Service-to-history final-loss and objective association |
| `test/java/com/ohinteractive/seedv6/training/service/TrainingRunControlTest.java` | Clock failure freezing and generation transition assertions |
| `docs/training/SESSION_METRICS.md` | This verified contract, investigation and validation record |

The starting repository was clean on `main` at `e793a8d`, already at 0.0.0 build
35. Earlier reported unstaged redesign/staged research work was no longer pending.
This correction leaves only its own files unstaged/untracked; no commit, push or
deployment was performed. No root journal existed and none was created.

Non-blocking version warning: the pinned local `begin` was invoked before code
changes, but its full-tree baseline scan was stopped after more than eight minutes
without producing a token or reservation, under the repository's non-critical
version override. The local finalizer subsequently reported `idle`, no active or
unfinished operations and no blockers. Version bytes remain 0.0.0 build 35; no
verified bump is claimed and no replacement bump was attempted.

Human actions required after this prompt: None.
