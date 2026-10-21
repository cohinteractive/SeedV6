# Verified temporary engine baseline

22 October 2026 (Pacific/Auckland). Bounded post-suspension stabilization;
not evaluator research, NNUE implementation, Search development or strength acceptance.
Starting revision: `e23745d0f4b6df810a7ca2070ed6966d067d905c`, build 60.
Stabilized application revision: `b4367ea221f3502c92546581fb52d80a9e833261`,
**0.0.0 build 61**. The subsequent evidence-only documentation commit does not
change application code or increment the build.
Build increases once with the GUI application change. No finalizer is involved.

## Selected evaluator and actual entry points

**BRE-Pair 2 Gen32**, runtime family `BRN_PAIR2`, with existing recursive
production **Policy 4**, is the temporary standard. Actual loading, search and
desktop play passed; no fallback, new model or training was needed.

| Identity | Verified value |
| --- | --- |
| Tracked payload | `build-assets/production/bre-pair2-g32.brn-pair2` |
| SHA-256 | `5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7` |
| Bytes | 795,032 |
| Lineage | `235d0688-e060-44e3-8f1e-119c39bc8017` |
| Checkpoint | `g000032-s001753088-5837c7f7192aa3cf9a265388f93649327455e804541398ae3ef6b69e09014304` |
| Schema | `seedv6.brn.pair2`, version 1 |
| Mapping | `BRN_PAIR2_MATERIAL_RESIDUAL_QUARTER_V1`: STM material + 0.25 residual, times 100, half-away rounding, capped at +/-32511 |

The reference owner's Gen32 runtime and optimizer artifacts are locally available
and hash-verified against the suspension inventories. The original optimizer
payload is 2,731,424 bytes, SHA-256
`f8bdda5eda0ff7893ee36c2270baaedecaf50e14ff5202ab786996f72a2788d5`.
Other original BRE generations and the G9 continued-BRE lineage through Gen176
also exist locally. They are preserved research inputs, not runtime dependencies
or substitute baselines. No strength ranking or new comparison was performed.

| Route | Effective selection |
| --- | --- |
| Standalone `UciMain` / `seedv6-uci` | Existing SHA-pinned Gen32 loader; no evaluator UCI switch, no training/GUI/research classpath. `--model` supplies the development distribution's asset. Native UCI packages resolve `models/baseline.brn-pair2` relative to application home. |
| Native GUI / `SwingLauncher` / legacy launcher with `gui` | Fresh Human-vs-Engine now loads bundled Gen32 via `PlayEvaluator.pinned(POLICY4_PAIR2)`. Fresh White/Black Play selectors also default to Policy 4. Existing saved per-side choices remain respected; choose Policy 4 explicitly to replace a previously saved experimental/network selection. |
| Legacy `Main` / `seedv6` **without** `gui` | Deliberate compatibility HCE profile, unchanged. Use standalone UCI for this baseline. |

The bundled model is copied into `engine-core.jar` at
`/models/bre-pair2-g32.brn-pair2`. Core `ProductionModel` checks bounded length,
SHA-256 and codec integrity before creating inference tables. GUI Play needs no
checkpoint store, optimizer file, external training library or research lineage.
GUI includes training services for its separate workspaces; startup can inspect
saved library metadata but does not start training. Standalone UCI needs only core.

Only two production files changed: `ChessFrame` installs the existing pinned
profile at startup; `PlayEnginePanel` changes fresh White/Black defaults while
preserving preferences and Arena's network default. HCE, NNUE, BRN variants,
construction-v1, search policies and trained bytes remain unchanged.

## Build and launch

Use installed Java 21 and the Gradle wrapper. No model download is needed.
For Java distributions, retain the repository's isolated Gradle-home convention:

```powershell
$env:GRADLE_USER_HOME = "$PWD/build/migration/gradle-user-home"
.\gradlew.bat --no-daemon --max-workers=1 :app:installDist :app-uci:installDist
.\app-uci\build\install\seedv6-uci\bin\seedv6-uci.bat --model "$PWD/build-assets/production/bre-pair2-g32.brn-pair2"
# Separate desktop process:
.\app\build\install\seedv6\bin\seedv6.bat gui
```

For a self-contained Windows GUI:

```powershell
.\gradlew.bat --no-daemon --max-workers=1 :app-gui:packageWindows
```

The task prints its candidate directory containing `SeedV6-GUI/SeedV6-GUI.exe`.
Native packaging additionally needs Python, Git and a complete Java 21 JDK
21.0.6 or newer. This host compiled with `C:/java/jdk-21` and automatically selected
`C:/java/jdk-21-packaging` (Temurin 21.0.12.1) for native tools/runtime.
See [packaging instructions](unified-architecture/WU7-PACKAGING.md).
Do not launch loose IDE `bin/` classes as a distribution.

## Verification and limits

Local evidence is retained under `build/baseline-stabilization/20261022/` and
`dist/windows/candidates/`. These are ignored outputs, not fresh-clone contents.
The reusable regression tests are committed; no bulk output is force-added.

- Java distribution builds and project-boundary verification passed. GUI, core,
  training, UCI, legacy composition and selected integration sources compiled.
- 17 selected module tests passed: Pair-2 runtime boundary (3), actual Gen32
  cache/search mapping (1), UCI parser/boundary cases (11), GUI baseline (2).
  Gen32 checks cover 32 changing/backtracking placement/perspective combinations,
  fresh versus cached scores, qsearch state transfer and evaluator identity rejection.
  Wrong-size, corrupt/different and missing payloads are rejected without fallback.
- 43 selected integration cases passed across `EngineSearchAdapterTest`,
  `HumanEngineSelectionTest`, `PlayPresentationTest`, `PlayStoreWorkflowTest`,
  `UciSessionTest` and `UciReportingTest`. One existing native store-window case
  was skipped by the root task's headless setting; native baseline Play was
  separately exercised. A store selector test now explicitly selects Network
  instead of depending on the superseded default.
- Fresh actual standalone launcher: UCI identity/handshake, Threads=1, Hash=32,
  Ponder=false, UseBook=false, `isready`, `ucinewgame`, starting position,
  opening moves, castling-rich position, en-passant and promotion positions,
  five 400 ms searches, legal returned moves accepted by the UCI move parser,
  `go infinite` followed by bounded `stop`, readiness and graceful `quit` passed.
  An independent fresh JVM using the installed jars repeated the checks.
- Native GUI packaging verified dependency/JNI resources, model loading,
  checksums and ZIP extraction/relocation. The candidate
  `baseline-20261022-precommit` supplied interactive verification of the changed
  application at build 60. The final `baseline-20261022-build61` native GUI
  candidate and both Java distributions rebuilt successfully from `b4367ea`.
  Fresh final UCI repeated all five searches/stop/quit; fresh native GUI selected
  Gen32 and About displayed build 61 / `b4367ea221f3`. Both GUI and UCI contain
  identical core jar SHA-256
  `90fd5a076c85fd6094629d24111dc05ea72d728d061ee07b080d753db9739caa`.
  Final evidence is retained in `final-*` files. The embedded revision includes
  `(uncommitted changes)` because the version generator counts inherited untracked
  `bin/` output; tracked application sources were clean at final packaging.
- The actual packaged GUI was operated through native Windows controls using the
  existing isolated-preferences fixture and an empty model-library path. Fresh
  startup selected Policy 4 / Pair-2 g32. `e2e4` received book reply `d7d6`.
  Loading `4k3/8/8/8/4P3/8/8/4K3 b - - 0 1` caused a real depth-4 search and
  legal `e8f7`, 602 nodes, approximately 23 ms; the UI showed Gen32 and COMPLETED.
  Normal window close drained both native launcher/JVM processes. No training
  or Arena run was started. User preferences and owner stores were not used.
- Initial negative validation outputs remain retained: an overly broad receipt
  glob also counted summary files; the first GUI search assertion encountered
  the normal opening book; one store test assumed Network was still the default.
  Corrected selection/fixtures and retests passed; these were not model failures.

Smoke resource limits: one search worker, no pondering, 32 MiB UCI TT,
64 MiB GUI TT, 768 MiB heap ceiling. Final completed iterations in the initial
UCI runs ranged from depth 7 to 12 and roughly 0.36 to 2.55 million NPS;
opening/middlegame observations were about 0.36 to 1.44 million NPS. Direct UCI
JVM peak resident memory was 193 MiB (575 MiB private committed); native GUI JVM
peaked at 345 MiB resident (656 MiB private committed). These short cold/warming
smokes show no obvious pathological slowdown, not a controlled benchmark,
strength measurement or long-session memory test.

Not run: full suite, expensive NNUE/training suites, tournaments, sustained
benchmarks, Mac/Linux GUI, arbitrary trained-model compatibility, user's saved
GUI configuration, and external chess-client acceptance. Native UCI packaging's
fixed 1/4/16-worker validation matrix was not run under this operation's
single-worker smoke constraint; the actual Java UCI distribution was verified.

## Preservation

The [successor state](research/brn-successor-cglhw/STATE.md) and governing contract
still say SUSPENDED BY OWNER DECISION. No successor document or raw experiment
was edited. Checks before/after verified all **6,858** owner files recorded by
the suspension inventories, all **15,306** local files in the successor evidence
tree, frozen runtime jars, Gen32 payload/manifest and all **229** game receipts.
The scaled-BRE comparison remains at **21 receipts**, with **43 games unplayed**
across that comparison and the unstarted reference comparison. Pause sentinel
and resumption instructions remain intact. Source datasets/W1 owner libraries
outside those inventories were not exhaustively rehashed and were not touched.

No active research writer, supervisor or matching scheduled task was observed.
Existing Codex app servers, VSCode language server and idle Gradle services were
not treated as active research or terminated. No conflicting source/configuration
writer was observed. Repository journal `CODEXLOG_CURRENT.md` is absent; none was
created. Root version follows the repository owner's commit-based rule.

Inherited `bin/`, `engine-core/bin/`, `engine-training/bin/` remain untracked and
retained. During source editing, automatic IDE compilation changed
`bin/integrationTest/.../PlayStoreWorkflowTest.class` and added
`engine-core/bin/test/.../ProductionBaselineTest.class`; these correspond to this
turn's tests and are excluded from the commit. The former disassembles as an IDE
unresolved-classpath error stub, reinforcing that only Gradle distributions were
validated. No direct writer trace was collected; timing and active VSCode compiler
support this attribution. All other protected local files were unchanged.

## NNUE reuse inventory and Search Phase 3 boundary

| Existing component | Reusable boundary / limitation |
| --- | --- |
| `engine-core/core/nnue` | `NnueFeatureSchema` v1 (`seedv6.nnue.halfkp.v1`), 49,152 king/piece/square features; immutable float network, 64 units per perspective, concatenated 128 -> 32 -> 1; clip/tanh inference. Existing format, not a newly selected NNUE architecture. |
| `NnueAccumulator`, `NnueEvaluator`, `SearchEvaluation.State` | Worker-confined incremental state, child transitions, king refresh and independent scalar/full-recompute oracles. `initializeFrom` is the main/qsearch boundary; definitions must match. Pair-2 uses a separate rolling placement cache and immutable shared pair tables. |
| `NnueScoreMapping`, material variants | Legacy V1 is full-range sign-preserving mapping of bounded output, not empirically calibrated centipawns. `NNUE_MATERIAL` adds fixed material and has a separately persisted calibrated objective. Preserve those distinctions; see historical [E013](research/nnue-cglhw/E013.md). |
| `engine-training/training/nnue` | `TrainableNnue`, `NnueTrainer`, Adam state/gradients, `NnueCorpusTargets`, exact `TrainingStateCodec`. CP/mate targets and explicit perspective conversion/WDL adapters exist; target-family support must be checked rather than assumed. |
| Shared training/model services | `NetworkModel`, `NetworkTrainingState`, `TrainingArchitecture`, `CheckpointStore`/manifest/lineage, immutable payload hashes, optimizer continuation, Best/Latest, model library, prepared corpus/readers, data-source reservations, validation and campaign services. Reusable infrastructure; no training invoked here. |
| Model loading | `NnueNetworkCodec` and `NnueBinaryFormat` enforce version/dimensions/CRC; training checkpoints additionally bind runtime/optimizer identity. Production UCI intentionally accepts only the pinned Pair-2 payload today. NNUE production selection will require a separately authorized integration change. |
| Tests / diagnostics | Existing `NnueFeatureSchemaTest`, `NnueEvaluatorTest`, `NnueAccumulatorTest`, `NnueEvaluationStateTest`, codec/gradient/learning/calibration tests, corpus/checkpoint tests and `:research:nnueResearch` / search benchmarks are available. Inventory only; NNUE correctness/strength was not recertified. |
| Search construction | `SearchLifecycleService -> SearchDriver -> ExactSearchAdapter -> ExactSearch`, with private evaluator/cache state and `TTable`; managed multiworker path is `ParallelSearch`. Pair-2 production enables QXRAY/quiet LMR/WINDOW/completed hints and EFFORT at one worker (ROOT_REUSE at 2..16). A different evaluator does not automatically inherit these calibrated policies. |

Runtime inference requires no suspended research infrastructure. Historical NNUE
research records remain intact, including an older STATE labelled active; that
text does not authorize continuation under this stabilization request. No new
NNUE CGLHW, design, training or implementation was initiated.

**Evaluator readiness:** the frozen Gen32 baseline is operational for future
authorized NNUE and Search work on this Windows host. **Search-work readiness is
separate:** current repository masters P3-R014/P3-F012 say Workstream 3 construction
is technically complete but pending GPT/owner reconciliation and acceptance;
recursive Policy 4 remains production, construction-v1 remains explicit and
experimental. This operation does not accept it, promote it or launch a successor.
No evaluator blocker was found; next work still requires its own authorization.

Human actions required after this prompt: none to use/maintain this baseline.
Blocking before subsequent development: GPT/owner reconciliation and separate
authorization of the next unit; resolve the existing Phase 3 acceptance boundary
before any progression that depends on it. No push or deployment is part of this work.
