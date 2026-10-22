# Conventional NNUE state

22 October 2026. **ACTIVE: A technically operational; B/C open. Pilot16 is a
verified pipeline checkpoint, NOT a strength-accepted model.** Read CONTRACT.md,
M001-OPERATIONAL.md and E001.md. This new
programme does not resume the historical `nnue-cglhw` or suspended BRN work.

**First action in every fresh conversation:** run `.\nnue-control.cmd status`.
Permission is operational state in `.git/seed-conventional-nnue/control.json`
(resolve with Git for linked worktrees), not this document's ACTIVE label.
Read COMPUTE-CONTROL.md. Unknown/inconsistent permission blocks expensive work.
Initial control integration retained NORMAL; never reset a later hold from this
historical initialization statement.
Control implementation and targeted checks are complete: see CONTROL-VALIDATION.md
and compact control-*.json evidence. Twelve Windows control tests plus real GPU,
dataset and UCI match hold/resume passed. Graceful GPU hold measured 1.627 seconds;
emergency 0.166 seconds, no remaining owned process. No user action is required.

## Dependency milestones

1. Verify baseline/environment; challenge the initial design; pin Bullet and data.
2. Establish bounded local GPU training/export/resume, independently checked against
   Java full-rebuild integer inference. No production-default change.
3. Verify incremental evaluator/search/UCI lifecycle and measured cost.
4. Train a useful checkpoint, validate provenance and compare fixed-time paired games.
5. Freeze a stable opt-in Search checkpoint; seek competitive confirmation only when
   screening supports it. Preserve the fallback if improvement is not established.

Milestones 1..3 are technically verified; next is bounded data improvement and
complete-engine screening. No owner acceptance is inferred from these results.

Current evidence: cross-language export parity, checkpoint continuation and opt-in UCI.
Baseline targeted tests/build and fresh UCI searches/stop/quit passed. Local CUDA
13.0.2 libraries extracted/hash-verified without driver/admin/global changes. Pinned
Bullet compiled with Rust1.89 and completed a real RTX4060 training smoke (8192
sample exposures, two checkpoints). No useful strength claim from this smoke.

Java schema S6CNU001 implements int accumulators/long SCReLU head, independent
full rebuild, strict bounded SHA256 codec and worker-confined SearchEvaluation.
Seven new tests pass, including 953 legal/sibling transitions, 30 special transitions,
null/undo/qsearch/reset/network mismatch and hostile-weight overflow. Existing
production test passes. Raw evidence: `build/conventional-nnue/runtime-tests-1.log`.

Read-only Lichess pilot:131072 accepted from224579 input lines,117882 train/13190
diagnostic validation, depth>=12, quiet PV, noncheck, |CP|<=3000, exact placement/STM
dedup. Source is existing22GB archive, not a new download; consumed20.8MB compressed
prefix fingerprint in `build/conventional-nnue/data/pilot-131k/manifest.json`.
CC0 source, score-only target (`wdl=0`); outcomes unavailable. Unknown game relations
and prefix sampling limit representativeness. BT4 inverse-Q interpretation deferred.

Pilot16 trained 2,097,152 exposures; held-out sigmoid MSE .005442 vs zero .017540.
Exact Python/Java/Bullet features, Java/Python integer inference and GPU float
reference pass. Longer GPU continuation is not byte-identical (21 quantized
values differ by 1); frozen payload hashes are authoritative for reproduction.
Measured update+head ~1.4..1.6us, no hot allocation; Pair-2 ~90ns on that walk.
New UCI selects existing unpruned quiescence without inherited numerical pruning;
one-/two-worker actual lifecycle and full-rebuild search comparisons pass.

E001 fixed-time screen: **0W/1D/15L**, no caps. Preserve fallback. All 117882 binary
labels independently match white->STM conversion; material probes have correct
sign but weak amplitude (starting-position queen removal ~261cp). No export or
perspective failure found. Initial data coverage/generalization is inadequate.

E002 extraction completed before control integration: 1,048,576 accepted from
1,766,343 lines, 943,857 train / 104,719 validation, 164.06 seconds. Evidence:
`raw/e002-data/manifest.json`. Consumed compressed prefix 147,459,375 bytes, SHA256
`1184f0044504ffacf0e01630e5724b16030d828c5406bfe15f03487af673b8b0`.
No training process was running at the safe integration boundary. Preserve
hash partition/dedup/filters; document overlap with E001 and diagnostic validation.
Train this same architecture/seed, batch1024, 1024 batches/superbatch, 16 superbatches,
lr.001, wdl0 (16,777,216 exposures). Owner compute controls change save4 to every
superbatch with supervised segmented continuation, preserving data order and the
constant LR/WDL recipe. Controls are verified; training is the next admissible
operation when observed permission permits. Expansion is justified by
E001's large training/validation gap and losses; no architecture sweep. Verify export,
held-out loss/material behavior and the same development openings before deciding
whether more data, a target/source change or another intervention is warranted.
No human action currently blocks this. No unmonitored training/tournament.

Raw tools/output: `build/conventional-nnue/`; upstream source clone:
`build/conventional-nnue/upstream/bullet` at
`579b78b37a694cacd6e9ede4706386c15c34837c`. These ignored paths are local evidence,
not fresh-clone dependencies. Recreate using pinned manifests/scripts once recorded.
