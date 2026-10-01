# Current SeedV6 baseline

## Application and build

The root Gradle project includes `app`. Java 21's application plugin launches
[`Main`](../app/src/main/java/com/ohinteractive/seedv6/Main.java), selecting desktop
`SwingLauncher` for `gui` and `UciEngine` otherwise. Application JARs, `installDist`,
and native Windows/macOS packaging contain only `sourceSets.main.output` and its
runtime dependencies. Generated version and native Syzygy resources remain main
resources. The native Fathom license/provenance stays beside the vendored sources.

`main` has no dependency on `verification` or `test`. The custom `verification`
source set depends on main; test depends on both. JavaExec developer tasks use
verification's runtime classpath. No source set includes `archive/` or `docs/`.
No reflection, ServiceLoader, resource class-name registry or packaging include
was found that selects an additional production search.

## Production search

[`SearchDriver`](../app/src/main/java/com/ohinteractive/seedv6/search/driver/SearchDriver.java)
performs full-window completed iterations over
[`ExactSearchAdapter`](../app/src/main/java/com/ohinteractive/seedv6/search/driver/ExactSearchAdapter.java)
and [`ExactSearch`](../app/src/main/java/com/ohinteractive/seedv6/search/exact/ExactSearch.java).
This is the sole production algorithm lineage, confirmed from construction sites
and reconciled with the programme's [contract](search/CHESS_SEARCH_CONTRACT.md) and
[frontier](search/CHESS_SEARCH_RESEARCH_FRONTIER.md).

Normal TT-enabled search uses recursive nominal-depth PVS, legal hash moves,
SEE/material/main quiet history, staged lazy generation and static leaves.
HCE production enables the accepted calibrated static-null/null-move composition;
explicit/neural evaluator facilities remain exact. Mate-domain, repetition and
score-provenance contracts remain in force. `ExactSearch` retains explicit research
factory seams for active tests; they are not application selection switches.
Its existing name identifies the independent exact baseline and was retained.

[`ParallelSearch`](../app/src/main/java/com/ohinteractive/seedv6/search/exact/ParallelSearch.java)
is the current same-depth cooperative coordinator, using private worker state and
shared coherent table evidence. Managed Threads=1 uses `ExactSearchAdapter`;
Threads>1 uses this coordinator. It does not select the legacy root-split search.
The optional configured Syzygy root WIN outcome remains a separate guarded result
in managed search, with distinct provenance; ordinary search and training contracts
are preserved.

| Consumer | Confirmed construction path |
| --- | --- |
| Play | `SwingLauncher -> ChessFrame -> GameController -> EngineSearchAdapter -> SearchLifecycleService -> SearchDriver -> ExactSearchAdapter / ParallelSearch -> ExactSearch` |
| UCI | `Main -> UciEngine -> SearchLifecycleService -> SearchDriver -> ExactSearchAdapter / ParallelSearch -> ExactSearch` |
| Network Training | `TrainingController -> TrainerService -> Operations.generate -> SelfPlayBatch -> SelfPlayRunner -> SearchDriver -> ExactSearchAdapter -> ExactSearch` |
| Candidate validation | `TrainerService -> Operations.validate -> ValidationArena -> SearchDriver -> ExactSearchAdapter -> ExactSearch` |
| Developer strength arena | `ResearchTool -> StrengthArena -> SearchDriver -> ExactSearchAdapter -> ExactSearch` |
| Score-mapping queries | `ScoreMappingStudy -> SearchDriver -> ExactSearchAdapter -> ExactSearch` |
| Production training diagnostics | `ToolMain / brnDiagnostic -> BrnDiagnosticTraining -> TrainerService`, observing the same production drivers |

Self-play's thread setting schedules independent games, each with its own driver;
it does not activate root workers. Validation uses private synchronous players.
Play/UCI retain lifecycle ownership, cancellation and completed-iteration publication.
The existing UCI `go depth` cap remains 64; training/manifests use `ExactSearch.MAX_DEPTH`.

## Transposition table

[`TTable`](../app/src/main/java/com/ohinteractive/seedv6/search/tt/TTable.java) is the
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

These live in `app/src/verification/java` and `resources`. Existing tests and
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
`ExactSearch` moved to [the archive](../archive/search-programme/README.md).
No historical Java was classified as archive: retained alternatives have active
oracle/test/comparison consumers and therefore belong in verification.

## Documentation and developer commands

The [guide](guide.md) retains operation, training and packaging details.
`docs/search/` holds both programme masters; `docs/research/search/` holds search
reports and their CSV/GZIP/ZIP evidence; `docs/research/brn/` holds BRN reports.
`docs/history/` contains transplant discovery and explicitly historical WS architecture.
`docs/images/` contains Play/Training references. Build-required source artwork
remains at its established root paths. Root README is the repository entry point.

Use `:app:verificationClasses` before direct Java developer commands. Their
classpath needs main and verification classes/resources plus application dependencies.
Existing JavaExec task names retain their arguments. The former shipped diagnostic
commands now run through `:app:developerTools -PtoolArgs="brn-diagnostic ..."` or
`-PtoolArgs="frozen-wdl ..."`; normal GUI/UCI packaging remains independent.

The search contract/frontier identify manually refreshed ChatGPT Project Source
mirrors. Their repository master locations are now `docs/search/`; no external
mirror was changed by the cleanup.
