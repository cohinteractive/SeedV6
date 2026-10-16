# Current SeedV6 baseline

## Application and build

The root Gradle project includes the five target owners `engine-core`,
`engine-training`, `app-gui`, `app-uci` and `research`, plus temporary `app`.
`engine-core` owns the runtime implementation and has no project dependency.
`engine-training` owns trainers, optimizer/checkpoint continuation, model workspace,
corpus, self-play and validation services, and exposes core through its API dependency.
The WU4 extraction places desktop presentation, controllers, preferences and
resources in `app-gui`, which consumes core/training. WU5 places the single UCI
protocol/session and standalone `UciMain` in `app-uci`, depending only on core.
Legacy `app` consumes all four production owners and retains accepted-only
checkpoint configuration and Main delegation. `research` owns independent
references, empirical runners, diagnostics and research configuration. Root-only
integration/research-launch source sets provide the owner-approved mixed
composition without a root runtime JAR or production dependency on research. See
[WU5](unified-architecture/WU5-UCI.md) for the two explicit application profiles.
WU4 structural validation is complete under the
owner-authorized non-interactive boundary; native user acceptance remains deferred
to WU7. See [the WU4 record](unified-architecture/WU4-GUI.md). Shared Java/JUnit conventions and enforced project dependency/source-root
boundaries live in `gradle/`. See [the WU1 transition](unified-architecture/WU1-GRADLE.md)
for the foundation and legacy removal gate, and [WU3](unified-architecture/WU3-TRAINING.md)
for current training ownership and dependency declarations. Java 21's application plugin launches
[`Main`](../app/src/main/java/com/ohinteractive/seedv6/Main.java), selecting desktop
`SwingLauncher` for `gui` and `UciEngine` otherwise. The application JAR contains legacy main output; legacy `installDist` and `packageLegacyWindows`/`packageLegacyMac`
include its runtime dependencies, including `app-gui.jar`, `app-uci.jar`,
`engine-core.jar` and `engine-training.jar`. The separate `:app-uci:installDist`
contains only UCI and core JARs, with a Java launcher and an explicitly supplied,
checksum-pinned production model. Unified native candidates are documented in WU7 below.
Training owns the unchanged Gson/SQLite/Zstandard dependencies, optional trainer-only
BRN-3 vector kernels and binpack notice. Core owns the opening
book and optional Syzygy native sources/resources/license. Shared headless ApplicationVersion and its generated identity resource now belong
to core; GUI About and legacy UCI consume that one owner. Desktop FlatLaf/JNA and
all runtime GUI artwork belong to app-gui. The legacy `:app:compileSyzygy` task delegates to core.

Production main/test classpaths have no research dependency. Research consumes
core/training; root integration tests may compose all owners. A root research UCI
adapter composes research/core/UCI only and is never packaged as production.
No source set includes archive/docs. See [WU6](unified-architecture/WU6-RESEARCH.md).

## Production search

[`SearchDriver`](../engine-core/src/main/java/com/ohinteractive/seedv6/search/driver/SearchDriver.java)
performs full-window completed iterations over
[`ExactSearchAdapter`](../engine-core/src/main/java/com/ohinteractive/seedv6/search/driver/ExactSearchAdapter.java)
and [`ExactSearch`](../engine-core/src/main/java/com/ohinteractive/seedv6/search/exact/ExactSearch.java).
This is the sole production algorithm lineage, confirmed from construction sites
and retained by the Phase 2 [contract](../source/CHESS_SEARCH_CONTRACT.md) and
[frontier](../source/CHESS_SEARCH_RESEARCH_FRONTIER.md).

Normal TT-enabled search uses recursive nominal-depth PVS, legal hash moves,
SEE/material/main quiet history, staged lazy generation and static leaves.
HCE production enables the accepted calibrated static-null/null-move composition;
explicit/neural evaluator facilities remain exact. Mate-domain, repetition and
score-provenance contracts remain in force. `ExactSearch` retains explicit research
factory seams for active tests; they are not application selection switches.
Its existing name identifies the independent exact baseline and was retained.

[`ParallelSearch`](../engine-core/src/main/java/com/ohinteractive/seedv6/search/exact/ParallelSearch.java)
is the current same-depth cooperative coordinator, using private worker state and
shared coherent table evidence. Managed Threads=1 uses `ExactSearchAdapter`;
Threads>1 uses this coordinator. It does not select the legacy root-split search.
The optional configured Syzygy root WIN outcome remains a separate guarded result
in managed search, with distinct provenance; ordinary search and training contracts
are preserved.

| Consumer | Confirmed construction path |
| --- | --- |
| Play | `SwingLauncher -> ChessFrame -> GameController -> EngineSearchAdapter -> SearchLifecycleService -> SearchDriver -> ExactSearchAdapter / ParallelSearch -> ExactSearch` |
| Standalone / legacy UCI | `UciMain / Main -> UciEngine -> SearchLifecycleService -> SearchDriver -> ExactSearchAdapter / ParallelSearch -> ExactSearch` |
| Network Training | `TrainingController -> TrainerService -> Operations.generate -> SelfPlayBatch -> SelfPlayRunner -> SearchDriver -> ExactSearchAdapter / ParallelSearch -> ExactSearch` |
| Candidate validation | `TrainerService -> Operations.validate -> ValidationArena -> SearchDriver -> ExactSearchAdapter / ParallelSearch -> ExactSearch` |
| Developer strength arena | `ResearchTool -> StrengthArena -> SearchDriver -> ExactSearchAdapter -> ExactSearch` |
| Score-mapping queries | `ScoreMappingStudy -> SearchDriver -> ExactSearchAdapter -> ExactSearch` |
| Production training diagnostics | `ToolMain / brnDiagnostic -> BrnDiagnosticTraining -> TrainerService`, observing the same production drivers |

Play/UCI, self-play and candidate validation share `ProductionSearch.create`:
one worker uses `ExactSearchAdapter`; N workers use `ParallelSearch(N, ...)`,
including the calling main worker and at most N-1 helpers. Training's Search threads
setting applies to each individual search. Self-play games, validation games and
validation pairs remain sequential; each colour owns private search/evaluator state.
Parallel scheduling may change tied moves or selective visitation despite unchanged seeds.
Play/UCI retain lifecycle ownership, cancellation and completed-iteration publication.
The existing UCI `go depth` cap remains 64; training/manifests use `ExactSearch.MAX_DEPTH`.

## Model workspace ownership

`ModelLibrary` is the shared domain catalog over existing `TrainingLineage` UUIDs,
`TrainingFolders` placement/adoption and `CheckpointStore` manifests. A concrete
binding contains root, optional recorded UUID, name, architecture, checkpoint and
generation. Discovery is advisory and metadata-only; resolving a model verifies
payloads through existing reader/pruner coordination. No user stores are moved.
Lineage revisions, initializer evidence and generation annotations are additive
checksummed sidecars; immutable model/optimizer bytes and promotion receipts retain
their original authority. Historical missing evidence remains unknown.

`TrainingRecipe` owns rate/batch/epochs; `TrainerConfig` retains source, validation
and run ownership. Nullable rate overrides preserve legacy fingerprint and codec
compatibility; applying a rate never resets optimizer moments or step. Generation
admission is the update boundary. Soft time budgets stop the next admission;
immediate cancellation still uses exact partial checkpoint/cursor ownership.

`TrainingDataLibrary` catalogs the existing versioned `DataSource` descriptors,
reusing `PreparedBinpack` and readers. The catalog is shared; lineage mixes, ledgers,
campaign source descriptors and frozen tranches remain independent. Preparation and
inspection run outside the EDT and the common picker's owned workers drain at close.

Learning campaigns compose independent named stores, optional exact optimizer/model
copies and shared exposure/match protocol. Optional new config fields are omitted
when null, preserving old campaign identity hashes. `ValidationArena` already owns
both candidate validation and campaign games. `ActiveGameFeed`, `BoardPanel` and
`MatchPresentation` now share exact participant/move semantics; `OptimizationSnapshot`
and `TrainingProgressView` share bounded optimizer metrics. Play keeps its separate
interactive lifecycle. No metadata work enters node-critical search paths, and no
promotion, score calibration, evaluator or training mathematics is changed.

## Transposition table

[`TTable`](../engine-core/src/main/java/com/ohinteractive/seedv6/search/tt/TTable.java) is the
accepted and used production table. Adapters own it, requests share one generation
across iterations, and `ParallelSearch` shares it within its drained worker cohort.
`TranspositionScores` remains production score-domain support. The older
`TranspositionTable` belongs exclusively to verification and cannot enter production.
`Magic` remains production: `Move` still uses its sliding attacks. It was not
removed merely because the legal generator primarily uses PEXT.

## Source classification

Production contains the board/generator/evaluator core, rules, canonical search,
managed lifecycle/tablebases, GUI/UCI, training/checkpoint/history/validation,
and application version handling. The caller/import audit found no other orphaned
production class after the moves; package documentation remains alongside its code.

The following former main-source facilities are active verification:

- Legacy `search.alphabeta` (`AlphaBetaPvsSearch`, `RootParallelSearch`, `SelectiveSearchPolicy`),
  `search.flat.FlatNegamax`, `search.quiescence.QuiescenceSearch`, `search.iterative`,
  and legacy `search.order` move ordering/picking.
- `search.tt.TranspositionTable`, `search.exact.FlatExactSearch`, `search.common.WindowedSearch`,
  `search.diagnostics.SearchDiagnostics` and `QsearchDecisionTrace`.
- Experimental `core.BoardMoveType` / `GenMoveType`, and `core.util.MagicGenerator` tooling.
- All Java `tools` facilities: perft baselines/libraries, evaluation corpus/benchmark,
  exact/flat/legacy search harnesses, neural research, BRN diagnostics and comparisons.
- `training.service.BrnDiagnosticTraining` and `FrozenWdlReplay`, plus the diagnostic
  evaluation corpus resource. Their package placement preserves access to training
  test seams; their source set establishes the non-production boundary.

These live in `research/src/main/java` and `resources`. Existing tests and
explicit Gradle tools still use them. Legacy policy/aspiration hints moved out of
`SearchEvaluation` and `SingleDepthSearch`; only verification consumes that policy.
Capture training's unchanged tactical classification now lives in `MoveTactics`,
so it does not depend on legacy ordering state.

The SR-002 flat benchmark uses its explicit original non-MDP recursive control,
matching the dedicated flat oracle tests. Comparing it with the evolved default
constructor no longer established identical historical trees; its output now
states the historical comparison boundary.

The empty `GenRewrite` stub and one-off `codex-process-test.txt` were deleted.
Historical instrumentation scripts whose source anchors no longer match live
`ExactSearch` remain in the original checkout's ignored archive. The migration
worktree does not contain that local directory; see [historical reproduction
availability](unified-architecture/WU6-RESEARCH.md).
No historical Java was classified as archive: retained alternatives have active
oracle/test/comparison consumers and therefore belong in verification.

## Documentation and developer commands

The [guide](guide.md) retains operation, training and packaging details.
`source/` holds both active Phase 2 Search masters; `docs/search-phase1/` holds
the frozen, byte-preserved R045/F052 records. `docs/research/search/` holds search
reports and their CSV/GZIP/ZIP evidence; `docs/research/brn/` holds BRN reports.
`docs/history/` contains transplant discovery and explicitly historical WS architecture.
`docs/images/` contains Play/Training references. Build-required source artwork
remains at its established root paths. Root README is the repository entry point.

Use `:research:installResearch` for direct Java tools; its lib directory includes
research, core, training and required libraries. Existing `:app:` research task
names delegate to `:research:` tasks with their existing arguments. The old
`:app:verificationClasses` name delegates to research classes, but the old output
paths are replaced explicitly. `installResearchWorkspace` additionally prepares
the separate research UCI adapter. Production Main rejects all `seedv6.uci.*Research`
selectors, including explicit historical control defaults. Historical frozen
installations retain their original command conventions; they are not rewritten.

The search contract/frontier identify manually refreshed ChatGPT Project Source
mirrors. Their active repository master locations are `source/`; the owner must
refresh both mirrors and confirm synchronization before the next GPT-led Phase 2
research planning session. Initialization does not modify the external mirrors.

## Inference/training boundary

Core owns the immutable Pair-2 geometry and `BrnPair2Weights` compilation input,
all runtime model codecs and inference kernels. Optimizers, `*TrainingCodec`,
the unchanged trainer-only BRN-3 vector helper and checkpoint-store resolution
belong to engine-training after WU3.
Shared binary header helpers stay package-local in the runtime codecs; Java package
names are preserved across modules. No model/optimizer schema changes or model
registry are introduced. See [WU2](unified-architecture/WU2-CORE.md) and
[WU3](unified-architecture/WU3-TRAINING.md).

Experimental policies/factories remain compiled inside ExactSearch/ParallelSearch.
Production configurations never select SR-021; research properties are rejected by
production UCI and are not read by core/GUI. Research activation is confined to the
explicit research launcher. This is activation/classpath isolation, not complete
experimental-class exclusion from core. SR-021 remains deferred without adoption.

## Unified native packaging (WU7)

Root `packageWindows` / `packageMac` coordinate app-gui and app-uci directly;
module tasks package either independently. Shared Gradle conventions and native
jpackage/jlink tooling live under gradle/tools, with no extra runtime project.
Both candidates consume the same engine-core artifact and canonical core model
validator. Neither candidate contains legacy app or research; UCI also excludes
training and desktop libraries. Platform stamps stay outside the deterministic
core JAR. [WU7 packaging](unified-architecture/WU7-PACKAGING.md) documents private
asset restoration, provenance, legacy task aliases and unresolved acceptance gates.
