# Repository discovery and challenged assumptions

Observed 2026-10-05 before implementation, starting from a clean worktree.
Root AGENTS.md applies; no nested AGENTS.md found. Java 21 / Gradle / Swing;
main ships, verification/test do not. No CODEXLOG_CURRENT.md exists; do not create
one. VERSION_STATE.txt exists (initial build 34); use the local SeedV6 finalizer.

## Verified dependency map

| Area | Production evidence and implications |
| --- | --- |
| Architectures | training/model/TrainingArchitecture owns schema/file identities; gui/NetworkArchitecture mirrors six types: NNUE legacy, NNUE_MATERIAL, BRN/BRN1/BRN2/BRN3. Historical enum strings must stay stable. |
| Managed placement | gui/TrainingFolders already owns baseTrainingRoot and per-base architecture adoption preferences. TrainingLineages.create already places UUID directories under architecture folders. Reuse; do not create a rival catalog. |
| Lineage metadata | checkpoint/TrainingLineage is a checksummed UUID/name/architecture/created/configuration/origin sidecar. LineageConfiguration v1..3 holds complete GUI settings. Legacy read proposes defaults with an explicit unknown-configuration notice; adoption writes only the sidecar. |
| Checkpoints | CheckpointStore publishes immutable manifest/network/optimizer with content identity; refs/best and refs/latest-training have different meanings. PayloadAccess coordinates readers with pruning. AvailableCheckpoints currently fully decodes every payload; common library needs bounded metadata browsing and final verification on selected load. Pruned metadata survives. |
| Generation evidence | HistoryRepository/GenerationRecord retain candidate/incumbent/resultingBest, validations, samples/loss/timing/effective settings with nullable unknowns. GenerationAttempt/PartialGeneration retain exact resume identity. New annotations must not rewrite these or model hashes. |
| Play | ChessFrame -> PlayEnginePanel has per-side paths and generation pickers; PlayParticipants loads atomically before GameController installs bindings. Explicit loads currently read Best first. Human network opponent also uses independent-store selection. Preserve lifecycle/search pinning, fix shared selection and add whole-binding swap. |
| Training UI | TrainingPanel already selects named lineages, locks seeds after initialization and groups tabs; TrainingSettings/LineageConfiguration still mix recipes, sources, match protocol and limits. TrainingController owns I/O off EDT; TrainerService owns run/state. |
| Rates/resume | NNUE has Adam .001 and BRN3 masked Adam .003; GUI BRN3 config passes hard-coded .003, NNUE initializer uses defaults. Optimizer codecs already persist actual hyperparameters. Existing resume restores optimizer state, so a new GUI LR alone is insufficient; boundary application and attempt provenance must be tested. Older BRN online-pass/minibatch constraints remain. |
| Termination | TrainerService's timer calls stop(), cancelling active self-play/validation/optimization; stopAfterGeneration/admitGeneration already supply a safe boundary mechanism. Explicit requested time-budget semantics should use that boundary, not pretend current behavior already matches. |
| Threads | Play, Training and Arena spinners allow ParallelSearch.MAX_WORKERS rather than available processors; per-search workers, sequential games. Add semantic configuration outside numeric engine APIs. |
| Data | DataSource path-independent fingerprint/label/shards; DataSources weighted selection under lineage/training-data; SourceLedger cursor/reservation; SourceReaders + global PreparedBinpack cache. Library discovery must reuse descriptors and keep allocation ownership separate. No globally shared cursor. |
| Source compatibility | CorpusTraining.targetPolicy already supports direct BT4 Q for NNUE (both variants) and BRN3. BRN2 CP policy rejects it. Arena config validates both policies, but GUI Draft registers a raw path without explicit BT4 profile. docs/learning-arena.md has stale BT4 exclusion; source implementation is authoritative. |
| Arena | LearningArenaConfig/State/Service/Tranche/Training provide fresh competitors, identical frozen records/targets, exact exposure, atomic receipts, optimizer snapshots and per-game resume. CheckpointStores A/B use publish only: no Best or promotion. Identity hashes config JSON + recipe/target IDs, so defaulted new fields cannot casually change old identities. |
| Presentation | BoardPanel shared between Play and TrainingBoard; ActiveGameFeed immutable move-boundary snapshots; ValidationControl supports optional feed, currently omitted by Arena. TrainingDashboard rich phase/optimizer/loss data, SelfPlayTraining.Progress supplies common optimizer metrics. Arena Update currently exposes detail/search only. Reuse without injecting Swing into services or metadata into search nodes. |
| Results | Arena table uses raw A/B IDs and A score, no chart. ValidationResult counts only fully scored pairs; capped/cancelled games are not draws. Chart must show complementary scores only where scored results exist. |

## Bootstrap challenge and resulting decisions

1. The request could be misread as requiring an entirely new library. Existing
   TrainingFolders/TrainingLineages already implement much of it; extract/reuse
   shared domain browsing and retain their preference/migration keys.
2. Requiring Best for any model would exclude valid Arena endpoints and rejected
   candidates. Explicit generation selection must work without Best. Never invent
   a Best pointer for campaign competitors.
3. Full payload inspection per catalog refresh scales with all model sizes.
   Introduce a clearly advisory metadata catalog, validate exact payload on load,
   and retain visible corrupt/missing/pruned diagnostics without silently choosing
   another model. Never claim a metadata listing established payload integrity.
4. A globally reused source must not reuse a lineage cursor. Reuse registration
   descriptors/preparation, retain per-lineage weighted mixes and per-campaign
   independent frozen-tranche traversal. Historical receipt hashes remain intact.
5. New campaign config fields can change old JSON identity hashes even with a
   sensible default. Add explicit compatibility tests before changing persistence.
6. Rich Arena presentation is possible through existing telemetry seams; no new
   game runner or heavyweight chart dependency is justified.
7. Existing training settings already lock initialization seeds and persist
   configuration. Refactor ownership incrementally; do not lose obscure BRN2
   supervision, frozen-replay, teacher, corpus or legacy-resume controls.

## Validation inventory

Relevant focused tests: TrainingLineagesTest, TrainingFoldersTest,
TrainingStoreRecognitionTest, AvailableCheckpointsTest, CheckpointRetentionTest,
GenerationRestartTest, TrainingRunControlTest, TrainingSettingsValidationTest,
TrainingSearchThreadsTest, PlayStoreWorkflowTest, PerSideNnuePlayTest,
BestNnuePlayTest, TrainingDataGuiTest/Bt4TrainingDataPanelTest, SequentialTrainingTest,
NnueCorpusTrainingTest, Brn3CorpusTrainingTest, LearningArenaTest,
LearningArenaMaterialTest, LearningArenaGuiTest and board/dashboard/layout tests.
Default Gradle test excludes slow-nnue; -Pheadless skips native-window checks.
Run native focused render harnesses separately and inspect produced images.

No real external stores, preferences or corpus data were mutated for this discovery.
No engine performance/strength claim follows from it.
