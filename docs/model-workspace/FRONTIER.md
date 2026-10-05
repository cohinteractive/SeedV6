# Model workspace frontier

Governing requirements: [CONTRACT](CONTRACT.md). Verified starting map and challenged
assumptions: [DISCOVERY](DISCOVERY.md). Workstream status: ACTIVE, not accepted.
Initial baseline: clean Git worktree, build 34, 2026-10-05.

## Execution queue

| ID | State | Coherent unit and acceptance evidence |
| --- | --- | --- |
| F01 | DONE | Bootstrap/dependency/compatibility map and contract challenge; source inspection recorded in DISCOVERY. |
| F02 | DONE | Shared ModelLibrary over existing lineage/adoption mechanisms; bounded advisory metadata catalog; explicit snapshot resolution including no-Best campaign stores. TrainingLineages discovery delegates to it. |
| F03 | PARTIAL | Separate checksummed generation notes/tags and reusable factual details/editor implemented/tested. Cumulative known/unknown lineage exposure still pending. |
| F04 | DONE | Common ModelSelectionPanel; Play opponent and independent sides; exact identity checks, persisted legacy/new selections, concrete names and Swap Sides. Focused loader and native window tests passed. |
| F05 | NEXT | Training selection integration; provenance/default recipe/config history ownership, explicit configurable rates for supported architectures and actual checkpoint provenance; resume compatibility. |
| F06 | DONE | Portable Max (persisted zero), hardware/support bounds across Play/Training/Arena; explicit termination policy over compatible fields; timer closes admission and finishes the current generation. Immediate resumable stop remains separate. |
| F07 | READY | Reusable Training Data library over existing descriptors/cache, independent mixes/cursors; select in Training/Arena; explicit Lichess/BT4 capability/readiness. |
| F08 | READY after F05-F07 | Network Training UX organized around lineage, recipe, exposure, validation/match and termination; native layout verification. Fix baseline dashboard graphs extending below native viewport. |
| F09 | READY after F02,F05,F07 | Arena shared protocol/competitor recipe persistence; preserve old config binding, frozen tranches/targets, resumes/history; integrate lineage identities. |
| F10 | READY after F04 | Shared participant/match presentation semantics where justified; Arena live board using existing feed/BoardPanel, exact side identities. |
| F11 | READY after F05 | Shared rich training progress presentation in Arena and Training; retain optimizer/loss/exposure metrics and EDT ownership. |
| F12 | READY | Arena Setup/Live/History, concrete history identities, winners and W/D/L; symmetric stacked scores/50% chart with unscored cases. |
| F13 | READY after integration | Compatibility cleanup, remove superseded path-first/duplicate flows, guides; broader risk-based regression and native acceptance evidence. |

## Current execution and resume

Foundation, F04 and F06 implemented and focused checks passed. Next: F05 recipe and
lineage provenance, then F07 and their Training/Arena consumers. Shared ModelSelectionPanel
is ready for reuse; it performs catalog I/O on workers and protects exact selections.
Native Play selection/Swap Sides and layouts verified; broad integration pending.
Do not treat the contract creation as completion. Continue through admissible units.

Version initialization: pinned helper hashes verified; local begin invoked before
mutation at 02:41 UTC. Capture was stopped after over seven minutes, before it
created an operation/token/reservation. Local status then reported idle, active
null, unfinished [], blockers []; all 97 historical operations completed. Under
the explicit SeedV6 non-critical invocation override this is a non-blocking version
warning, not a successful begin or verified bump. No version bytes changed, no
replacement bump attempted, no shared helper edited. Root journal absent.
No blockers or required human actions identified. No push/deployment authorized.

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
  Baseline worktree is source-clean and can be removed after its comparison purpose.
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

## Next-unit compatibility decisions

F05 inspection found LR values currently initialize BRN optimizers only; resume restores
the payload, and NNUE/BRN-3 GUI rates are fixed. Generation fingerprints currently omit
LR. Add an explicit optional recipe override with a stable absence for old settings:
old unchanged resumes must keep their stored optimizer, while deliberate overrides
join the attempt fingerprint and existing safe restart path. Apply a rate change only
at a generation boundary, retaining moments/step and beta/epsilon. Checkpoint optimizer
sidecars already provide actual historical LR; never backfill inferred recipes. New
lineages should record explicit defaults; preserve old configuration bytes/readers and
configuration history additively. UI provenance seeds must be locked for initialized
lineages, independently of run/shuffle semantics. F08 will finish presentation cleanup.
