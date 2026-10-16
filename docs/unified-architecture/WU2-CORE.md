# WU2 shared production engine core

16 October 2026 (Pacific/Auckland). The owner accepted WU1 commit
`bd864a9f1231e6c73997b9302a411449bc2a5a21` and admitted this unit. Implementation
is confined to the clean `migration/unified-architecture` worktree. The
[contract](CONTRACT.md), [acceptance matrix](ACCEPTANCE.md), Search Contract and
model-workspace compatibility contract govern the extraction. This is a structural
migration, not a release or new search adoption. GPT/user reconciliation precedes WU3.

## Ownership and interfaces

`engine-core` owns 108 main Java sources under
`engine-core/src/main/java/com/ohinteractive/seedv6/`:

| Package | Responsibility / principal existing interfaces |
| --- | --- |
| `core`, `core.move`, `core.util` | Packed `Board`, `Gen`, moves, legality, SEE, HCE, bitboards, FEN and Zobrist identity. |
| `rules` | `GameHistory`, `SearchLineHistory`, position identity, formal repetition and draw rules. |
| `search.common`, `driver`, `manage` | Requests/results, controls, clocks, observers, completed iterations, `ProductionSearch`, `SearchDriver`, lifecycle and cancellation. |
| `search.exact`, `search.tt`, `search.diagnostics` | Existing `ExactSearch`, `ParallelSearch`, worker policies, coherent `TTable`, mate normalization and production diagnostics. |
| `search.evaluation`, `core.nnue`, `core.brn*` | Existing `SearchEvaluation`, immutable model definitions, runtime codecs, score mappings, worker-local inference and BRN-3 inference. |
| `book`, `search.tablebase` | Opening-book policy/resource and optional guarded Syzygy runtime adapter. |

The opening book and Syzygy integrity manifest move to core resources. Unchanged JNI/Fathom sources, license
and provenance move to `engine-core/src/main/native/syzygy`. The existing optional
`gradle/syzygy.gradle` now applies to core, so native resources enter the core JAR
when explicitly built. The legacy `:app:compileSyzygy` entry point delegates to it.

Java packages and existing search APIs are retained. All board, move generation,
rules, search, TT, time/lifecycle and factory sources relocate byte-for-byte.
No source-directory sharing, alternate production engine or copied algorithm tree
is introduced. Nine existing pure runtime test sources move into core, alongside
three new runtime boundary test cases. Mixed integration tests and independent
research references remain under app test/verification until their later owners
are ready; they consume the same core dependency.

## Inference versus training

`BrnPair2Geometry` holds the existing relation/category definitions, with private
arrays and package-local access. Both trainer and runtime compilation consume it.
`BrnPair2Weights` replaces trainer-nested `Model` as the immutable, validated,
defensively copied float32 compilation input. `BrnPair2Trainer.snapshot()` performs
the same folding and returns that input. Compiled tables, residual gain `.25`,
rounding, category layout and arithmetic order are unchanged.

`BrnCodec`, `Brn1Codec`, `Brn2Codec`, `Brn3Codec` and `BrnPair2Codec` keep runtime
model encode/decode and their existing schema identities in core. Exact optimizer
read/write methods move into corresponding `*TrainingCodec` classes in app;
consumers explicitly call those owners. Shared binary header/checksum helpers are
package-local in the runtime codecs. Schema constants that describe both artifact
types remain shared metadata, without an optimizer implementation or store dependency.
A final call-site audit establishes that every `Brn3VectorKernels` method is
trainer-only, including its hidden-layer calculation. The original helper remains
byte-identical in app, without an unnecessary split or core vector dependency.

All trainers, Adam state/configuration, objectives, `NetworkTrainingState`,
`NetworkModel`, `TrainingArchitecture`, checkpoint stores and model-library
selection remain in app for WU3. `UciEvaluation` remains an application boundary:
it selects/verifies an explicit checkpoint, then supplies immutable evaluation to
core. No second registry, fallback model, UUID inference or workspace migration.
Package-local cooperation across Gradle modules is intentional; there is no JPMS
module boundary or core-to-app compilation dependency.

## Application and Gradle transition

The project graph is now:

```text
engine-core     -> (none)
engine-training -> engine-core
app-gui         -> engine-core, engine-training
app-uci         -> engine-core
research        -> engine-core, engine-training
app             -> engine-core
```

The four remaining target owners are scaffolds. Legacy application entry points,
training/GUI/UCI implementations and external dependencies remain in app. Its
verification and test classpaths inherit the explicit core dependency. `app.jar`
is a thin application JAR; the distribution includes `engine-core.jar` once, just
as it includes other runtime dependency JARs. Existing launcher and native
packaging tasks consume the complete distribution libraries. Direct developer
Java commands must include core, or use the existing Gradle JavaExec tasks.

The existing boundary rules still reject forbidden projects, cycles and overlapping
source roots. The production-boundary test now checks both JARs, rejects duplicate
classes and verification leakage, and asserts that core contains no GUI, UCI or
training classes. `jdeps` resolves core only to `java.base`, with
no optional vector module; no external library or application dependency.

## Production and persistence evidence

The frozen SR-024 application JAR and all four pinned generation-32 checkpoint
files were rehashed before use against [BASELINE](BASELINE.md). No model bytes
are copied into Git. The accepted source is `35294146bb7e44975b516005b2cc196439666e09`,
with closeout `aff8d744f115a73978fe469d3d02de2d436513f6` and later recorded owner acceptance.

- `tools/verify-unified-core.py` exports the immutable UE `Sr024SourceProbe` and
  fixture inputs from `58511f45bf1bf0a45ca25f31da402a9e0ba56c50`. It verifies the
  recorded SR-024 TSV hashes and runs the same probe against the frozen primary
  JAR and core alone. **All 203 vectors match all 17 fields exactly**, including
  score, nodes/qnodes, maximum qply, depths, budget/interruption, selectivity and PV.
  This covers native EFFORT, serial ROOT_REUSE, window and tactical qsearch cases.
  The UE probe is a verification fixture, not an imported engine implementation.
- `tools/UnifiedModelCompatibilityProbe.java` runs unchanged against both frozen
  primary and migrated distribution classpaths. The pinned optimizer state
  round-trips exactly at step **1,753,088**, and recompiles to the exact unchanged
  `5ab54abe...84eb7` runtime model. All seven supported family initializers produce
  identical model and optimizer SHA-256 values across the two applications, and
  decode/reencode/recompile exactly. Existing regression tests cover nonzero
  optimizer continuation and independent inference references.
- `tools/verify-unified-uci.py` reuses the retained SR-024 independent legal-PV
  auditor: **10 exact one-worker comparisons and 66 process requests passed**.
  Every worker setting 1..16 completed two 100,000-node requests. Worker changes
  1/4/1/6/2/16/1 passed timed and explicit-stop requests. Production identities
  retain QXRAY, quiet LMR, WINDOW, EFFORT1/ROOT_REUSE2..16, calibration and model
  checksum, without SR-021 activation. Concurrent node/PV equality is not required.

The complete selected expanded run had 1,211 cases: 27 failures (nine inherited
checkpoint failures and 18 test-layout failures), 11 existing skips. Two ownership
assertions and sixteen UCI harness filesystem/classpath assumptions were remediated;
the initial wildcard-path correction also needed Windows-safe string construction.
Final engine/search/UCI/version regression: **989 cases, 978 passed, 11 existing
skips, zero failures**, 3m50s. Core: **61 passed**. Final packaging/process/inference/
vector retest: **24 passed**. The GUI replay reproduces all ten inherited failures.
Across these selected runs, the latest result for each distinct case is **1,278
cases: 1,241 passed, 19 inherited failures, 18 existing skips**. All 19 names and
failure messages exactly match WU1. This is not a full-suite green claim; no tests
were suppressed or disabled. All 220 GUI class files and CheckpointStore.class
remain byte-identical to the retained WU1 baseline build.

Nine Gradle boundary fixture tests pass. Actual target compilation, configuration-
cache replay and both native packaging dry-runs pass with `--warning-mode fail`.
The Windows JNI library compiles through the legacy alias, and loads from the
core JAR alone with its integrity manifest available; temporary extraction stays
inside this worktree. The final distribution contains 722 classes (173 core,
549 app) with no duplicate ownership; 692 existing class files are byte-identical
to WU1. The removed class is only trainer-nested Pair-2 Model, replaced by the
runtime-owned immutable input. No search class bytes or parameters change.

A private copy of the real optimizer state also continues one three-position
batch to step 1,753,089; its optimizer and compiled-model hashes match the frozen
application exactly. The original state/model files remain untouched. Original
SeedV6 and UE tracked bytes, protected artifacts, Git/index/merge metadata and
all recorded size/mtime inventories match the pre-work snapshots.

Detailed commands, test counts, failures, hashes and local evidence paths are in
[wu2-evidence.json](wu2-evidence.json). Local raw outputs stay under ignored
`build/migration/wu2/`; a fresh checkout must supply the frozen external inputs.
All Gradle validation uses the worktree-local `build/migration/gradle-user-home`,
JDK 21, wrapper 8.11.1, `--no-daemon --max-workers=2`, local test temporary files
and `GIT_CEILING_DIRECTORIES` set to the migration root.

No training campaign, strength tournament, Elo claim or performance improvement
is part of WU2. Native GUI/client/platform and final packaging acceptance remain
their later gates. Static packaging checks do not certify a native application image.

## Deferred isolation and next boundary

The disabled experimental factory seams/policies inside `ExactSearch`,
`ExactSearchAdapter` and `ParallelSearch`, including SR-021 `FutilityPolicy`, remain
unchanged. Explicit UCI research properties still belong to the legacy application.
Removing them now would broaden the extraction into research architecture work;
the owner explicitly permits retaining disabled seams with this accounting.
No independent oracle or historical experiment is deleted or rewritten. WU2
establishes accepted production routing and no dependency on the research project;
it does **not** claim complete experimental-code isolation. WU6 must remove/remap
these seams while retaining frozen controls and independent verification.

The 19 owner-deferred GUI/checkpoint failures remain open for their later runtime
gates. WU3 may start only after GPT/user reconciliation of this core boundary;
it owns the remaining trainer/checkpoint/corpus/self-play/validation move. Public
model inclusion, new standalone UCI, final packaging and release naming remain
outside this unit. Version stays `0.0.0`, build **35**, using local finalizer
`no_bump`; no release, push or historical artifact replacement is authorized.

Finalization completed `no_bump` with token
`0374b98ef93d43d8a5d92003a4b09292`, no warnings/blockers and identical version
bytes. Subsequent status is idle with no active or unfinished operation. Root
CODEXLOG_CURRENT.md is absent and was not created. The commit introducing this
record is the independently reviewable WU2 boundary; it does not establish
GPT/user acceptance, a release or authorization for WU3.
