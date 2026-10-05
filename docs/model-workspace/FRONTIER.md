# Model workspace frontier

Governing requirements: [CONTRACT](CONTRACT.md). Verified starting map and challenged
assumptions: [DISCOVERY](DISCOVERY.md). Workstream status: IMPLEMENTATION COMPLETE and locally verified; human acceptance not recorded.
Initial baseline: clean Git worktree, build 34, 2026-10-05.

## Execution queue

| ID | State | Coherent unit and acceptance evidence |
| --- | --- | --- |
| F01 | DONE | Bootstrap/dependency/compatibility map and contract challenge; source inspection recorded in DISCOVERY. |
| F02 | DONE | Shared ModelLibrary over existing lineage/adoption mechanisms; bounded advisory metadata catalog; explicit snapshot resolution including no-Best campaign stores. TrainingLineages discovery delegates to it. |
| F03 | DONE | Separate checksummed notes/tags, factual details and known/unknown cumulative sampled exposure. Unavailable payloads retain valid catalog metadata and editable notes but cannot load. |
| F04 | DONE | Common ModelSelectionPanel; Play opponent and independent sides; exact identity checks, persisted legacy/new selections, concrete names and Swap Sides. Focused loader and native window tests passed. |
| F05 | DONE | Explicit recipe rates/defaults, additive codec, exact optimizer/resume ownership, immutable initialization evidence, prior configuration archive and shared Training generation browser. Ownership presentation completed in F08. |
| F06 | DONE | Portable Max (persisted zero), hardware/support bounds across Play/Training/Arena; explicit termination policy over compatible fields; timer closes admission and finishes the current generation. Immediate resumable stop remains separate. |
| F07 | DONE | Reusable catalog of existing DataSource descriptors and preparation cache; common Training/Arena picker; additive saved-mix import; explicit readiness/BT4 compatibility; independent weights/cursors. |
| F08 | DONE | Single recipe editor, explicit lineage/provenance, Data & exposure and Validation & run ownership; shared Best-lineage browser for generators/teachers; native and scaled layout checks. Baseline dashboard graph overflow repaired. |
| F09 | DONE | Arena shared protocol/competitor recipe persistence; preserve old config binding, frozen tranches/targets, resumes/history; integrate lineage identities and optional exact existing-model starts. |
| F10 | DONE | Shared participant/match presentation semantics where justified; Arena live board using existing feed/BoardPanel, exact side identities. |
| F11 | DONE | Shared rich training progress presentation in Arena and Training; retain optimizer/loss/exposure metrics and EDT ownership. |
| F12 | DONE | Arena Setup/Live/History, concrete history identities, winners and W/D/L; symmetric stacked scores/50% chart with unscored cases. |
| F13 | DONE | Compatibility/fixture cleanup, picker shutdown and campaign publication ownership, guides, broad package regression plus resolved reruns, and native layout verification. |

## Current execution and resume

F01-F13 are complete. All contract gates have implementation and proportionate
local evidence. The 669-case broad run exposed stale fixtures; the subsequent
311-case and focused reruns resolved every identified failure. Final native
Training/presentation checks passed 13/13 after repairing the small-window
Diagnostics layout; Arena setup/live/history and exact-copy starts also passed.
No known in-scope defect or human gate remains. Do not restart this workstream
from an older in-progress evidence entry. Future work starts from a new request,
the contract/evidence map below, and current Git inspection. Human acceptance,
strength acceptance and deployment are not inferred from this completion.

All work is attributable to this workstream, from a clean baseline. The local
commit series starts at c1f0cf3; the final F13 commit contains this completion
record and follows 5435db5. No push or deployment occurred.

Version initialization: pinned helper hashes verified; local begin invoked before
mutation at 02:41 UTC. Capture was stopped after over seven minutes, before it
created an operation/token/reservation. Local status then reported idle, active
null, unfinished [], blockers []; all 97 historical operations completed. Under
the explicit SeedV6 non-critical invocation override this is a non-blocking version
warning, not a successful begin or verified bump. No version bytes changed, no
replacement bump attempted, no shared helper edited. Root journal absent.
Final local status was rechecked: idle, active null, unfinished [], blockers [],
verified_bump false. Build remains 34. Root journal remains absent.
Human actions required after this prompt: None.

## Evidence log

- Bootstrap: inspected architecture/storage/GUI/training/validation/data/Arena and
  relevant tests; challenged and revised intended design around existing managed
  placement, missing Best in Arena, shared preparation versus separate cursors,
  and old config-hash compatibility. No runtime verification claimed.
- Foundation: `:app:test -Pheadless --tests '*ModelLibraryTest' --tests
  '*TrainingLineagesTest' --tests '*TrainingFoldersTest' --tests
  '*AvailableCheckpointsTest'` passed after correcting a test-only Java compound
  `var` declaration. Compiled main/verification/test and installed distribution.
  Synthetic-store tests cover read-only legacy browsing, duplicate names/distinct
  UUIDs, no-Best explicit load, metadata versus payload integrity, annotation round
  trip and immutable-file hashes, stale/corrupt edits and changed lineage identity.
  Full suite, real user stores, strength/training experiments and native render
  checks deliberately deferred to affected/integration units. `git diff --check`
  passed. No external user data or preferences used.
- F04: headless HumanEngineSelectionTest, PlayStoreWorkflowTest, PerSideNnuePlayTest,
  BestNnuePlayTest, ModelLibraryTest and ModelSelectionPanelTest passed. A caught
  evaluator-sharing regression was fixed: share checkpoint reads, keep independent
  evaluator definitions for each side. Publish-only Arena stores default to recorded
  Latest; missing Best with promotion evidence remains unavailable, never silently
  selects a different model. Swap freezes Best aliases to their displayed concrete
  checkpoints and transfers complete bindings/catalogs. Existing root preferences
  import without moving stores; changed lineage UUIDs and missing generations fail
  visibly. Generation notes use locked, stale-checked sidecars.
- F04 native: ModelSelectionPanelTest (5) and HumanEngineSelectionSmokeTest (2)
  passed together, including real Swap Sides button, independent exact bindings,
  next-game-only changes and 1440x950/1100x760 layouts. Updated render artifacts in
  ignored app/build/gui-smoke/workflow-refinement reviewed; clipped Register action
  fixed with wrapping grid. PerSideNnueSmokeTest is slow-tagged and was excluded by
  the ordinary task; no slow NNUE render claim. Tests use temporary stores/preferences.
- F08 regression evidence: PlayStoreWorkflowTest.oneNativeWindowVerifiesDashboardAndExplicitPerSideSetup
  fails before Play at Training graphs bottom=647 versus viewport=612. The same
  failure reproduced unchanged on detached c1f0cf3 in the temporary baseline
  worktree C:/projects/seed/java/seedv6-model-workspace-baseline. This is pre-F04;
  retain as an in-scope Training layout repair, not a successful native check.
  The source-clean baseline worktree was verified and removed after F08 repaired it.
- F06: SearchThreads keeps zero/Max semantic through preferences and lineage codecs,
  resolving to min(available logical processors, engine support limit). Existing
  positive stored requests remain readable and are bounded for this machine; low-level
  search APIs retain positive worker counts. Arena JSON fields/identity are unchanged.
  RunTermination interprets existing generation/time fields, including legacy combined
  bounds. Time expiry closes next-generation admission, never the active cancellation
  controls. Stop Now still saves exact partial work, and Resume receives a fresh budget.
- F06 validation: headless TrainingRunControlTest, ThreadAndTerminationTest,
  TrainingLineagesTest, TrainingDashboardTest and LearningArena*Test passed (55 tests,
  zero failures, one native-only skip). Covered expiry at all coherent phases, real
  timer before admission, immediate-stop override, finite bounds, optimizer and game
  resume, history failures, source/Arena receipts and material variants. Corrected two
  test-only compilation errors before the passing run. Native ThreadAndTerminationTest,
  TrainingSearchThreadsTest and HumanEngineSelectionSmokeTest then passed (7/7).
  Reviewed run-termination-max.png in ignored GUI artifacts: explicit policy, active
  bound and Max (12 on this machine) render correctly. `git diff --check` passed.
  Full/strength experiments remain deferred; no real user stores used.
- F05 recipe unit: TrainingRecipe defines architecture defaults; explicit optional
  LR overrides are distinct from legacy inheritance. New managed lineages persist
  explicit defaults; lineage configuration v4 reads v1-v3 unchanged. Preferences and
  all settings-copy paths retain overrides. Generation fingerprints include explicit
  rates; old absent-rate fingerprints remain byte-identical. Rate changes happen only
  before bootstrap/publication or at generation admission, retaining all moments,
  step, beta and epsilon. Search/evaluator math is untouched. Actual optimizer rate
  remains in existing checkpoint codecs/sidecars; no historical backfill.
- F05 checks: TrainingRecipeTest (9 cases including all six architecture codecs),
  RecipeSettingsTest, TrainingLineagesTest and BRN/BRN1/BRN2/BRN3 GUI tests ran headless:
  39 tests, 5 native skips, only two stale architecture-list assertions failed. Both
  asserted the pre-material-parity five-architecture list already inconsistent with
  HEAD; updated them to the existing six identities. A focused native rerun of both
  cases, RecipeSettingsTest and Brn3GuiTest passed 7/7; BRN-3 component render reviewed.
  Tests prove rate changes leave every other optimizer byte unchanged, codecs round
  trip, different rates affect weights, identical overrides resume partial work,
  changed rates restart it and parent payload rate remains unchanged. Legacy custom
  stored rates stay inherited. Larger live/presentation cleanup is still F08; old BRN
  initialization-only controls must be removed when its unified recipe view lands.

## Next-unit compatibility decisions

Initialization facts and configuration revisions are additive sidecars. A fresh-model
factory records the initializer/seed; arbitrary caller-supplied trainers and historical stores cannot.
The existing master seed also drives run/shuffle streams, so separate its valid run
role from immutable initialization provenance rather than silently freezing valid run
choices. BRN-2 already has an immutable persisted run-seed contract; retain that rule.
Training model selection should reuse ModelLibrary and factual details while retaining
the service's authoritative Latest continuation (never train from an arbitrary picker
snapshot without an explicit branch operation). F08 finishes ownership presentation.

- F05 metadata/selection checks: headless revision/provenance/lineage/library/picker
  and production BRN controller checks ran (24 cases); two new provenance assertions
  initially stopped before bootstrap. Corrected the fixture boundary; provenance and
  native Training browser rerun passed 4/4. Reviewed training-generation-browser.png:
  locked lineage identity, generation/details controls, explicit Latest continuation.
  Catalog now retains valid manifests when payloads are missing, with diagnostics.
  Native picker/smoke plus metadata tests ran 17 cases: 16 passed, one old catalog
  expectation needed updating for this intended behavior. Updated it and reran all
  ModelLibraryTest cases headless, 5/5 passed. Missing/corrupt payloads remain unloadable.
  Initialization records are immutable; changed default configurations archive prior
  bytes; unknown historical initialization and exposure are not reconstructed.
- F05 provenance correction: initializer source cross-check found BRN-1/BRN-2 use
  fixed architecture seeds (only BRN-0 starts from zero weights). Corrected the new
  metadata, without changing initialization. Expanded provenance native test run
  passed 6/6, covering both NNUE identities, BRN-0/1/2 and unknown supplied trainers.
  Only temporary fixtures had received the incorrect metadata; no user store changed.
- F07: TrainingDataLibrary stores existing versioned descriptors under the model
  library, normalizes catalog weights only, and leaves original sources, frozen
  campaign descriptors, per-lineage mixes and ledgers untouched. Saved Training mixes
  and opened Arena descriptors are imported additively. The common background picker
  registers files/folders, confirms BT4 labels, prepares/retries the existing cache,
  reports known counts, readiness and explicit incompatibility, and relocates only
  the same fingerprint. Training removes selections from its mix, not the library.
  Arena starts from a selected ready descriptor without another file browse.
- F07 validation: headless TrainingDataLibraryTest, TrainingDataSelectorTest,
  Bt4TrainingDataPanelTest and LearningArena*Test passed. Native LearningArenaGuiTest,
  TrainingDataSelectorTest and expanded LineageProvenanceTest passed 11/11. Added a
  stronger native picker-visibility check after visual review caught the form
  shrinking it to one row at 1100px; fixed minimum height and reran the native layout
  case successfully at 1100x760 and 1440x950. Reviewed data-library-1100.png with all
  registration/preparation actions visible. No real data corpus or user store used.
  F13 still needs to update the old PlayStoreWorkflowTest assertion that pruned
  generations are omitted: they are now browsable but unloadable by design.
- F08: removed duplicate initial-only BRN rate editors and separate NNUE/BRN-3
  batch/epoch editors. One capability-aware recipe editor owns actual rate/batch/epochs;
  legacy initializer-rate fields remain readable in persisted settings. Data/exposure
  owns source mix, positions, generated games, sampled positions and run/shuffle seed.
  Validation/run owns validation method, pairs, explicitly named shared search/opening
  protocol, ply cap and termination. Existing shared search semantics are unchanged.
  BestLineageField uses the common model browser restricted to compatible legacy NNUE
  Best for generation/teacher roles; the backend still pins Best per generation.
- F08 checks: headless BRN GUI families, recipe, selector, exposure layout and threads
  ran 49 cases (8 native skips). Three old standalone-panel assertions bypassed the
  controller's existing source resolver or omitted Training Data from the mode count;
  corrected their setup/expectations after inspecting HEAD. Native role-browser,
  BRN-3 and exposure-layout checks passed. Native BRN-0/1/2 render checks were updated
  to select the recipe tab. Their rerun plus the three corrected cases and real-window
  Play/dashboard workflow passed 7/7. A later 18-case layout/run-control check exposed
  a header symmetry regression; restored that symmetry and kept compact 120px overview
  charts. Final affected native/scaled/exposure checks passed 5/5. Graph bottom is now
  607 within viewport 612 (baseline 647); reviewed native dashboard and BRN recipe PNGs.
  Unchanged file bytes retained; `git diff --check` passed. Full integration still F13.

- F09: nullable competitor rate and exact initial-model binding preserve legacy JSON
  and the pre-change golden campaign hash. Existing checkpoints copy full verified
  model/optimizer state into independent campaign stores; source stores and Best stay
  untouched. Campaign lineages receive stable independent UUIDs, known initialization
  provenance, names and registration in the common browser. Shared exposure/tranches,
  recipes and resume receipts retain their existing ownership.
- F09 checks: ArenaRecipeConfigurationTest plus all LearningArena tests passed
  headless, including exact legacy binding, source preservation, inherited/overridden
  optimizer rates, divergent trained weights, durable resume and publication recovery.
  Native form renders passed at 1100x760/1440x950. The new native browser/campaign
  scenario exceeded the fixture five-second wait; a bounded 60-second rerun passed
  in 15 seconds, verifying exact selection, seed disabling, actual inherited rate
  and discoverable independent campaign lineages. No real user stores touched.

- F10: Arena publishes exact immutable ModelLibrary bindings through the existing
  ActiveGameFeed/ValidationControl. Reversed games swap both complete participants;
  pause/end/failure clear live state. The shared MatchPresentation and BoardPanel
  render Training and Arena with the searching model architecture's units. GUI
  polling reads the latest move directly; it never evaluates a board or changes
  search/promotion policy. Setup/Live/History shell added.
- F10 checks: five focused feed, Training board, mixed-architecture presentation
  and real campaign tests passed. Native image live-match.png reviewed. Initial
  label wrapping collapsed the new board; constrained two-line presentation fixed
  it. A mate-in-one fixture had no observable intermediate publication; changed
  it to a real four-ply game and verified both concrete colour assignments.

- F11: shared TrainingProgressView in Training diagnostics and Arena Live exposes
  actual optimizer LR, batch/epochs, sample visits/target, updates, step, loss and
  active duration. Arena reports campaign exposure and restart-segment throughput;
  it does not conflate visits with unique positions. Existing snapshots lacking
  recipe/exposure evidence remain unknown. Worker publications are constant-size.
- F11 checks: 18 focused progress, optimizer continuation, stored-rate, controller,
  dashboard and run-presentation cases ran; one stale legacy corpus label failed.
  Baseline source confirmed the Training Data naming predates this work. Updated
  three obsolete labels and reran the affected case successfully. Native progress
  rendering and Arena setup layouts passed; expanded explanatory text height after
  reviewing the native image. Actual partial resume counts and inherited LR passed.
  Full-window Live/diagnostics integration follows in F12/F13.

- F12: Arena now has Setup/Live/History, named model rows with an always-visible
  generation column, both W/D/L/score perspectives, round winner and exposure.
  Exact checkpoint/tranche/location evidence is in selected-row details. The native
  100% stacked chart uses complementary shares, a 50% line, named colour legends
  and explicit pending/unscored slots. It reads existing ValidationResult evidence;
  incomplete pairs never become draws. The main label is Arena; learning campaign
  remains the current mode.
- F12 checks: history scoring/colour pixels, 100/75/50/25/0 scores, real draws, capped
  pairs and pending rounds passed. Native saved-campaign History and integrated
  Live layouts at 1100x760/1440x950 and main setup passed. Reviewed history/live PNGs;
  added a separate generation column after long NNUE labels clipped that information,
  moved full-bar labels away from parity, and reran both history tests successfully.

- F13 in progress: broad headless GUI/service/checkpoint/model/data/validation/
  telemetry regression is running. No full-suite/strength claim. Updated operating
  guides and architecture ownership map; removed remaining misleading Training
  depth wording. Added cancellation/drain ownership to the common data picker after
  review found preparation outliving its dialog. New shutdown and full-window
  Diagnostics checks await the broad run's completion before compilation/reruns.
  Known broad failures under investigation/remediation: a concurrency fixture uses
  the pre-existing opening book and never starts its expected sustained search;
  old picker counts assume missing payload metadata disappears; architecture-list
  assertion used the wrong pre-existing order. Fixes are in the worktree, not yet
  validated. Final compatibility cleanup, targeted native checks and acceptance
  reconciliation remain required.

- F13 review follow-up: campaign switching clears old publications before exposing
  a new root; callbacks bind root before publishing new endpoints. Added a delayed
  draft regression. Retained the existing scoring/promotion explanation behind an explicit
  Validation rules action and updated the native Training smoke fixture for all six architecture choices and
  the six-tab ownership layout. These changes still await compilation/reruns.
- Broad service failures inspected against baseline 8699a1b: old bootstrap labels
  predate the current Held-out WDL label; source changes already preserve the
  independent validator; missing optional source/teacher metadata already uses
  legacy fallback. A blended-resume fixture incorrectly rejected source/objective
  edits that already invoke the supported restart workflow. Corrected expectations,
  preserving immutable-seed, invalid-teacher, corruption and exact-resume checks.
  No production compatibility or optimization behavior changed to satisfy them.

## Acceptance evidence map

The implementation and final integration evidence below reconcile every contract gate.

| Contract gate | Implementation and focused evidence |
| --- | --- |
| Library / identity / annotations | ModelLibrary, TrainingLineages, ModelSelectionPanel; ModelLibraryTest, TrainingFoldersTest, ModelSelectionPanelTest, LineageProvenanceTest. Metadata discovery is advisory; exact loading verifies payloads. |
| Play | Independent shared browsers and complete Swap bindings; HumanEngineSelectionTest/SmokeTest, PerSideNnuePlayTest, PlayStoreWorkflowTest. |
| Recipe / provenance | TrainingRecipe, LineageConfiguration, LineageRevision, LineageProvenance; TrainingRecipeTest, RecipeSettingsTest, LineageProvenanceTest and architecture codec checks. |
| Threads / termination | SearchThreads and RunTermination; ThreadAndTerminationTest, TrainingRunControlTest and TrainingSearchThreadsTest. |
| Reusable data | TrainingDataLibrary/Selector over existing DataSource and cache; TrainingDataLibraryTest, TrainingDataSelectorTest, Bt4TrainingDataPanelTest and Arena source tests. |
| Training ownership | One recipe editor, separate data/exposure and validation/run tabs, shared role browser; architecture GUI, exposure/scaled-layout and native Play/Training window checks. |
| Arena setup / compatibility | Nullable recipe/exact-start extensions, independent stores, unchanged old binding; ArenaRecipeConfigurationTest, LearningArenaTest and LearningArenaMaterialTest. |
| Arena live | ActiveGameFeed, MatchPresentation and TrainingProgressView; ArenaMatchPresentationTest, ArenaOptimizationProgressTest, TrainingProgressViewTest, real paired campaign feed and native render checks. |
| Arena history / chart | ArenaRoundSummary, ArenaHistoryView and ArenaScoreChart; ArenaHistoryViewTest covers named generations, draws/caps, complementary scores, parity and native layouts. |
| Integration | Broad 669-case package run; all failures resolved through 311-case and 20-case reruns; final 13/13 native Training/presentation checks plus Arena setup/live/history/exact-start checks. See recorded skips and boundaries below. |

No destructive store migration, external-data relocation, search/evaluator math
change, strength experiment, push or deployment is part of this workstream. A full
ordinary fixed-model Arena mode is not required by this contract; reusable bindings,
match presentation and result summaries provide its domain foundation while the
existing learning campaign remains the shipped mode.

- F13 broad regression completed: `:app:test -Pheadless` filtered to `gui.*`,
  `training.service.*`, `training.checkpoint.*`, `training.model.*`, `training.data.*`,
  `training.validation.*` and `training.telemetry.*`: 669 tests, 18 failures,
  29 skips, 37m11s. XML inspection accounted for every failure: nine GUI fixture
  cases; four bootstrap label/validator cases; three handcrafted metadata/restart
  cases; two stopped-reconfiguration validator cases. All have worktree fixes
  grounded in current/baseline behavior. The unchanged passing cases are retained
  as regression evidence; corrected and newly changed areas now require reruns.

- First targeted rerun compiled main but exposed the retained presentation test's
  dependency on the old validation explanation helper. Preserved that useful help
  and made it reachable from Validation & run instead of leaving it unused. Main,
  verification and test compilation then passed; the targeted runtime run is active.

- Targeted GUI plus changed service rerun: 311 cases, 2 failures, 26 skips,
  7m35s. Original bootstrap/handcrafted/GUI failures passed. Follow-on
  TO_SELF_PLAY assertion still assumed no held-out data plan after switching
  generator; corrected to use the independent validator. A previously passing
  Play fixture raced fast book moves against a queued stop and assumed an
  unrequested seven-ply cap; retained the real delivery/history assertions and
  made cleanup unconditional. These two cases now receive a final focused rerun.

- Final focused rerun passed in 32s: {'tests': 20, 'failures': 0, 'errors': 0, 'skipped': 2}. It covered all nine changed-setting parameters, real Play lifecycle, and validation/diagnostic presentation. All identified broad/rerun failures are resolved; final native integration follows.

- F13 native integration: initial six-case window run passed five cases and caught
  collapsed validation diagnostics at 1100x760. Splitter adjustment alone could
  not create enough space under wrapped tabs. The shared progress view now has
  a compact Training presentation with every metric retained; both diagnostic
  panes remain visible and scrollable, and usable divider positions are preserved.
  Restored the established inline scoring/promotion explanation, with a shortcut
  that scrolls to it instead of opening a modal. Final PlayStoreWorkflowTest native
  window, TrainingWorkspaceSmokeTest, ValidationPresentationTest and
  TrainingProgressViewTest passed 13/13, zero skips, in 18s. Reviewed diagnostics
  at 1100x760 and 1440x950 and Arena history/live at 1100x760. Earlier native Arena
  exact-model selection, setup and history checks passed in the six-case run.
  Test layout fixtures are not claims about real campaign results; real campaign
  execution, move-side reversal and optimizer continuation have separate tests.
- Final integration outcome: shared library/selection, recipes/provenance, source
  reuse, run limits, Arena identities/live/history/chart and compatibility are
  implemented and locally verified. Compilation, installed distribution generation
  and `git diff --check` passed. No existing user model/data stores were relocated
  or rewritten. Version warning is non-blocking under the repository override;
  local status is idle and no bump is claimed.

## Validation boundaries and remaining risks

The full unfiltered suite, slow-tagged NNUE suite, long training/strength/benchmark
experiments, and validation against the user's real large stores/downloaded corpus
were deliberately not run. Focused checks use synthetic temporary stores/data and
native Windows Swing windows. Consequently no empirical strength, real-corpus
acceptance, other-platform rendering, or deployment claim is made. These are
outside this workstream's required validation, not concealed unfinished gates.
Historical metadata remains unknown when evidence is absent; registered/pruned
snapshots can become unavailable and exact loads fail visibly. Campaign stores
retain their publish-only/no-Best lifecycle. No required human action remains.
