# Conventional NNUE state

22 October 2026. **COMPUTE HOLD requested by owner and completed at 07:00:54 UTC.
A operational; B technically available as E002 opt-in with negative strength limits;
competitive C open. No production promotion.**
Read CONTRACT.md, COMPUTE-CONTROL.md, E002.md and the current E003.md. This
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
emergency 0.166 seconds, no remaining owned process. The later actual owner hold
also stopped safely at E003 checkpoint31. Explicit owner release is required before
any further expensive operation; no action is needed to remain on hold.

## Dependency milestones

1. Verify baseline/environment; challenge the initial design; pin Bullet and data.
2. Establish bounded local GPU training/export/resume, independently checked against
   Java full-rebuild integer inference. No production-default change.
3. Verify incremental evaluator/search/UCI lifecycle and measured cost.
4. Train a useful checkpoint, validate provenance and compare fixed-time paired games.
5. Freeze a stable opt-in Search checkpoint; seek competitive confirmation only when
   screening supports it. Preserve the fallback if improvement is not established.

Milestones 1..3 and stable opt-in availability B are technically verified. E002 is
the current frozen opt-in checkpoint with negative strength limits; competitive C
remains open. No owner acceptance or production promotion is inferred.

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
No training process was running at the safe control-integration boundary. Owner
controls changed save4 to every superbatch, preserving optimizer, data cursor and
constant LR/WDL. Completed E002 evidence is below; do not rerun it unnecessarily.

E002 has now completed:16 verified supervised segments,16,777,216 exposures.
Expanded-cohort MSE.006115 versus pilot.014133 on the same104719 positions;
old-prefix MSE.005685 versus pilot.005442. Material queen-removal response improves
to about834cp. Exact128-position cross-language parity and actual one-/two-worker
build62 UCI checks pass. Development screen2W/1D/13L, no caps; no promotion.
Read E002.md and e002-*.json for frozen identity, costs, limitations and local paths.
E003 extraction is complete:4,194,304 accepted from7,046,013 source lines in668.44s;
3,773,996 train /420,308 validation. All frozen extraction hashes and every binary
target conversion pass. Reuse raw/e003-data; do not repeat completed extraction.
The narrowly declared data-fork guard is built/tested. Segment17 initializer bytes
exactly match E002 checkpoint16 weights, momentum and velocity. Recipe stays fixed.

E003 completed segments17..31 (15,728,640 additional exposures) before the actual
owner COMPUTE HOLD. Checkpoint31 verifies; its optimizer, immutable plan, trainer
hash and progress are preserved. Latest path:
`raw/e003-training/segments/000031-33d794da5eeb/run/seed-cnnue-v1-31`.
Owned job/process count is zero; trainer/controller PIDs and trainer CUDA PID were
verified absent. See e003-hold-20261022.json and E003.md. No emergency stop was needed.
Current permission persists in Git metadata; this historical record cannot release it.

After explicit NORMAL authorization, inspect status and rerun the exact
`nnue-control.cmd train docs/research/conventional-nnue-cglhw/e003-training-config.json`
with the local CUDA environment from E003.md. It verifies/skips17..31 and resumes32;
fixed endpoint80 remains unchanged. Do not rebuild/change the pinned trainer or
immutable plan casually. After80: parity, common-cohort validation, UCI and the
predeclared development screen. These checks have not yet run for E003. No useful
independent lightweight work remains in this unit; end execution while held.

Raw tools/output: `build/conventional-nnue/`; upstream source clone:
`build/conventional-nnue/upstream/bullet` at
`579b78b37a694cacd6e9ede4706386c15c34837c`. These ignored paths are local evidence,
not fresh-clone dependencies. Recreate using pinned manifests/scripts once recorded.
