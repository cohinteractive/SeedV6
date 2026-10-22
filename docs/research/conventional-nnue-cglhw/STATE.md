# Conventional NNUE state

23 October2026 Auckland. **A/B/C TECHNICAL QUALIFICATION COMPLETE in the tested
scope. The frozen candidate is ready for separately governed Search Phase3
evaluation. No production promotion or owner acceptance is inferred. NORMAL has
no expiry; completion does not change compute permission. No required technical
work remains for this scoped handoff.**
Read CONTRACT.md, COMPUTE-CONTROL.md, CANDIDATE-HANDOFF.md and E009-ROBUSTNESS.md. This
programme does not resume the historical `nnue-cglhw` or suspended BRN work.

**First action in every fresh conversation:** run `.\nnue-control.cmd status`.
Permission is operational state in `.git/seed-conventional-nnue/control.json`
(resolve with Git for linked worktrees), not this document's historical status.
Read COMPUTE-CONTROL.md. Unknown/inconsistent permission blocks expensive work.
Initial control integration retained NORMAL; never reset a later hold from this
historical initialization statement.
Control implementation and targeted checks are complete: see CONTROL-VALIDATION.md
and compact control-*.json evidence. Twelve Windows control tests plus real GPU,
dataset and UCI match hold/resume passed. Graceful GPU hold measured 1.627 seconds;
emergency 0.166 seconds, no remaining owned process. The later actual owner hold
also stopped safely at E003 checkpoint31. The owner subsequently released the hold
with NORMAL; this historical observation does not override future permission state.

## Dependency milestones

1. Verify baseline/environment; challenge the initial design; pin Bullet and data.
2. Establish bounded local GPU training/export/resume, independently checked against
   Java full-rebuild integer inference. No production-default change.
3. Verify incremental evaluator/search/UCI lifecycle and measured cost.
4. Train a useful checkpoint, validate provenance and compare fixed-time paired games.
5. Freeze a stable opt-in Search checkpoint; seek competitive confirmation only when
   screening supports it. Preserve the fallback if improvement is not established.

Milestones 1..5 and technical gates A/B/C are verified within the stated scope. Current
candidate is E007 checkpoint64, S6CNU002/eight material heads, optional vector
inference on frozen build64. Its unpruned independent confirmation was inconclusive
(42W/42D/44L). E008 tested the identical network with an explicit research-only
selective composition; its fixed128-game confirmation finished84W/26D/18L,no caps,
paired95% Elo roughly+142..+266. E009's independent1000ms confirmation completed
86W/33D/9L,no caps,paired95% Elo roughly+187..+307. Both final game/artifact audits
and the frozen-copy UCI check passed. Do not substitute historical E002/E003
candidates or repeat finished training/confirmations. Current handoff/re-entry
instructions are at the end.
No owner acceptance or production promotion is inferred.

## Preserved development history

Initial evidence: cross-language export parity, checkpoint continuation and opt-in UCI.
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

E003 is complete after explicit NORMAL release and verified resume32 from31.
All64 new segments17..80 completed (67,108,864 exposures); no completed work repeated.
Final checkpoint: `raw/e003-training/segments/000080-d19a96b4e37a/run/seed-cnnue-v1-80`.
Read E003.md and e003-*.json. Export/parity and actual one-/two-worker UCI pass.
Expanded-cohort MSE.007528 versus E002.013029, but common E002 cohort regresses
.006115 ->.006539. Fixed screen1W/4D/11L, no caps; no competitive gain/promotion.
Frozen E003 file is `raw/e003-evaluation/network.cnnue`; E002 remains the existing
stable opt-in handoff with negative strength limits. All historical evidence retained.

M002 rejected an overflow-proven int head:24 focused tests passed but no measured
speedup. Original long kernel restored/retested; no application change or build bump.
A bounded same-root probe completed48 depth4 searches. Gen32's existing Policy4
used3.46x less total time/5.26x fewer nodes than its unpruned control; NNUE's unpruned
searches were slower again. Different evaluator trees prevent pure arithmetic claims.
See M002.md and m002-*.json. No data-growth or architecture sweep is selected.

E004 completed: research-only E003/selective composition, five independent depth3
search oracle checks and one-worker UCI passed. Development screen1W/1D/14L, no
caps; no confirmation/adoption justified. See E004.md and e004-*.json, including
cross-screen jar identity limits and complete-game resume verification. Production
runtime sources/defaults remain unchanged; all evidence and rejected work preserved.

## Disposition and future continuation

A is technically verified: local Bullet/GPU/optimizer continuation, controlled
hold/resume, export, independent Java inference and CPU search integration.
B is technically available: frozen E002 and E003 artifacts, actual lifecycle/cost
and weak strength evidence. E002 remains the documented stable Search opt-in;
neither is accepted as competitive or as a replacement for Gen32. Raw checkpoints
are local-only; use frozen hashes for exact inference, not GPU regeneration promises.
C is **not achieved**. Three bounded coverage stages and the research search trial
remain clearly weak in these small screens. The arithmetic optimization had no
measured benefit and was removed. No architecture/parameter/data-size sweep follows.

The owner superseded the previous initial-batch stopping interpretation. CONTRACT
revision3 distinguishes experiment rejection from programme stopping. All E001..E004
negative evidence remains unchanged. Substantial overnight local computation and
reasonable conventional architecture refinements are authorized, without an8-hour
expiry or routine approval gate. Honour later COMPUTE HOLD/FULL PAUSE exactly.

Overnight development sequence: E005 data, E006 coverage, E007 material heads,
M003 vector inference, then E008 controlled search composition.

E005 used correctly transformed BT4 targets and improved the same-search E003
screen to 11W/5D/0L. Its Gen32 screen remained weak, and more fixed-sample training
worsened held-out MSE. Saved checkpoint16 (5W/2D/9L against Gen32) was selected
for E006's fourfold coverage expansion, with optimizer preserved. Read E005.md
and e005-stage-results.json; all old checkpoints remain intact.

E006 completed through256, with a predeclared LR reduction at129. Broader data
improved held-out MSE from .021569 to .016864 at64, but128/256 stayed near .01699/
.01691. Final Gen32 screen1W/6D/9L. Fixed one-head training stops at this plateau;
the programme continues. Read E006.md and stage64/128/256 evidence. Preserve the
33,554,432-position dataset and identical524,288-position holdout in raw/e006-data.
Exact prior runtime jars are recorded in e006-frozen-runtime.json.

E007 adds eight material-count output heads under S6CNU002, retaining256 hidden
units and exact S6CNU001 compatibility. Source implementation passed27 targeted
Java tests, immutable-geometry recipe testing, actual GPU hold/resume (2.61s,
owned CUDA PID gone),259-position parity across all buckets, actual1/2-worker UCI,
and measured inference cost. Re-exported legacy bytes and scores are unchanged.
Read E007.md, e007-implementation-evidence.json and e007-probes.json. No strength
acceptance follows these correctness checks. Production Gen32 is unchanged.

Implementation committed as e54ec3b, build63; installed UCI identity/lifecycle
verified. E00764 completed with exact259-position parity, MSE.016816835, Gen32
screen5W/4D/7L (development only). Material diagnostics show modest endgame gains
and slightly weaker18..25-piece fit; teacher probabilities agree closely with real
outcomes. Read e007-stage64-results.json and E007.md. Same-search E00664 screen
completed7W/6D/3L, no caps; tiny reused development sample, not confirmation.
E007256 completed (1,073,741,824 total exposures), exact parity passed on frozen
build63 runtime, flat MSE.016866643. No further identical-recipe training selected.
Read e007-stage256-results.json. M003 optional SIMD passed scalar/vector tests,
legacy/eight-head parity, actual UCI and identical-node search checks; integrated
cost~.72us versus~1.71us and measured fixed-root throughput1.40x. Preserve all raw
failures/results and m003-integrated-evidence.json. Implementation committed6213630/build64; actual scalar/vector UCI identity and
lifecycle passed (m003-build64-uci.json).256-versus64 was near-tied4W/9D/3L;
retain64 as the development candidate. Vector-enabled64 versusGen32 completed
8W/5D/3L on reused development openings, which justifies prospective confirmation
but is not C. E007-CONFIRMATION.md freezes128 games/64 fresh pairs at200ms,800-ply
censoring,one worker,32MiB,no book/ponder,unchanged build64 runtime and both artifacts.
Confirmation completed42W/42D/44L,no caps,in2151.94s. Paired95% Elo about-60..+49
around-5.43; improvement/equivalence is not established and C remains unmet.
Full128-game legal replay,outcome/history/color audit and frozen artifact recheck
passed. Read e007-confirmation-results.json,E007-CONFIRMATION.md and exact frozen
runtime manifest e007-confirmation-runtime.json. Preserve the unused first and
canonical-en-passant-normalized v2 opening selections; never reuse them as fresh
independent confirmation. No opportunistic extension of the finished protocol.

E008 controlled same-network policy screen completed9W/5D/2L; subsequent Gen32
screen12W/1D/3L,all uncensored. Six independent selective search roots,actual UCI,
and provenance guards passed. Read E008.md,e008-verification.json and both results.
This is promising development evidence,not C. No training/network bytes changed.

E008 confirmation completed84W/26D/18L,no caps,in2280.46s; statistical gate passed
at200ms,paired95% Elo roughly+142..+266. All128 game replays and frozen artifacts
passed. All owned match processes exited. Preserve e008-confirmation-results.json
and E008-CONFIRMATION.md. A separate14-root depth5 independent full-rebuild oracle
matched score/move/PV/nodes/qnodes/provenance; completed-match resume launched no
engines. Read e008-deep-verification.json. This is a complete-engine research gain;
it does not establish pure evaluator causation,default promotion or Phase3 acceptance.

## Current qualification and re-entry

E009 completed86W/33D/9L,no caps,in11522.84s at1000ms/move,one worker,32MiB,
no book/ponder. Paired score.80078125,descriptive+241.67 Elo; bootstrap95%
[+187.25,+303.47],normal95% [+188.19,+307.46]. Its frozen statistical gate and
all128 legal game/history/color/outcome/detail and frozen-artifact checks passed.
Read E009-ROBUSTNESS.md and e009-confirmation-results.json. Preserve the independent
E007/E008 results; do not pool clocks or extend completed protocols post hoc.

The final frozen-copy UCI smoke passed identity,legal searches,stop/readiness/
newgame/repeated search/quit,and exact jar hashes. Read candidate-frozen-uci.json.
The --runtime-lib option is now runtime-verified. No pending ledger writes remained;
all owned match/smoke processes exited. No full-suite,GUI,other-platform,selective
multiworker or external-engine/tournament-clock strength validation is claimed.

CANDIDATE-HANDOFF.md and candidate-handoff.json identify the exact3,940,952-byte
E00764 export,selected64 and latest256 optimizer checkpoints,frozen trainer/runtime,
launcher source/classes,baseline,comparison commands and complete evidence links.
Both checkpoint receipts/configurations/tensor geometries and all payload hashes
were reverified.65..256 already completed; no training restart or repetition is
needed. Keep build64 runtime copies and all raw evidence,including failed,
negative and inconclusive experiments. Production remains Gen32/Policy4.

The programme progressed through failed approaches to the contract's genuine
A/B/C technical criteria; this is a scoped qualification handoff,not an experiment-
failure/diminishing-returns stop or a claim that further improvements are impossible.
Owner acceptance,production adoption and separately governed Search Phase3 work
remain distinct. No pending required technical work or human action blocks this
handoff. Do not launch an arbitrary new training queue merely to consume time.

On fresh entry,inspect compute control first,then the handoff and actual local
artifacts. E009 and its final smoke are complete; do not resume them as unfinished
work. Use the frozen candidate for separately authorized downstream work within
its authority. Any further NNUE experiment needs a reasoned new question and
prospective comparison; retain all completed evidence and the fallback. The
owner's experiment/programme stopping distinction in CONTRACT revision3 remains
binding. A later hold always overrides the historical NORMAL observation below.

Persisted owner permission remains NORMAL generation3 with no expiry at this
update. Always reconcile nnue-control.cmd status on fresh entry; this prose never
releases a later hold. Frozen trainers are retained by SHA256 for existing immutable
plans; do not substitute a new binary into an old plan. Original1-head optimizer
geometry cannot be silently resumed as8 heads. Preserve all evidence, fallback and
15-minute user-facing reporting (active host updates are more frequent).

Raw tools/output: `build/conventional-nnue/`; upstream source clone:
`build/conventional-nnue/upstream/bullet` at
`579b78b37a694cacd6e9ede4706386c15c34837c`. These ignored paths are local evidence,
not fresh-clone dependencies. Recreate using pinned manifests/scripts once recorded.
