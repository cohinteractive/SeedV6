# BRN successor workstream state

Updated21 October2026. **G4 is complete and verified. All six arms reached64,
with exact expected continuation and preserved starting payloads. Wave4 trained
assessment is active; no programme success is claimed.** This remains the original
CGLHW. Current evidence/protocol: [WAVE4-ASSESSMENT](WAVE4-ASSESSMENT.md).
Read [CONTRACT](CONTRACT.md), [BOOTSTRAP](BOOTSTRAP.md), [WAVE1](WAVE1.md),
[completed investigations](WAVE2.md), and [implementation/training history](WAVE3.md), and [trained assessment](WAVE3-ASSESSMENT.md), and [current G4 training gate](WAVE4.md).

Objective: statistically credible practical chess improvement over frozen BRE-Pair2
Gen32 through useful additional expressiveness, with measured costs and compatible
search. Architectural complexity or lower validation loss alone is insufficient.

## Candidate registry and preserved reference

| Candidate | Identity / state |
| --- | --- |
| Frozen Gen32 | BRN-Pair2/235d0688-e060-44e3-8f1e-119c39bc8017, Gen32; never replace with live Best/latest |
| W1-GATE | 699672ff-5dcc-326a-afef-924996c76f79, Gen200 frozen; historical Best198 |
| W1-CUBIC | e04052b9-6784-3c12-abb5-abc75cd21f59, Gen200 frozen |
| W1-POOL | 97b2fdda-f812-3455-9f62-43e5ee322651, Gen200 frozen; lower research priority, not discarded |
| W3-RESGATE | BRN-GEN32-GATED/e6b6775d-39cd-392b-bd18-5e964a445f62, latest/Best32, frozen Gen32 plus trained gated correction |
| W3-RESCUBIC | BRN-GEN32-CUBIC/22b42be6-15c4-3f36-a376-3776aa1931f2, latest32/Best31, frozen Gen32 plus trained cubic correction |
| W3-BRE-CONTROL | BRN-Pair2/ed0750a3-3163-3979-9f26-2e466c49c771, latest/Best32,262144 added updates from exact Gen32 clone |
| W4-RESGATE-LR001 / LR003 | Completed64; LR001 Best63, LR003 Best64; exact W3 latest32 ancestry |
| W4-RESCUBIC-LR001 / LR003 | Completed64; both Best63; exact W3 latest32 ancestry; W3 Best31 preserved |
| W4-BRE-CONTROL-LR001 / LR003 | Both completed64 and Best64; original failed LR001 session successfully resumed |

All managed roots are under E:/SeedV6-Networks. Exact checkpoint IDs, hashes and
sources: `wave2-trained-checkpoints.json`, `wave3-lineages.json`, `wave3-trained-checkpoints.json`, `wave4-owner-readiness.json`. Frozen reference
network SHA256 is `5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7`.
Original lineages/campaigns/checkpoints remain preserved. W1 human training is verified
complete for investigation; it does not establish competitive maturity. Never resume
the user's paused Learning Arena campaign as an inferred authorization to train.

## Established findings and hypothesis frontier

1. **Verified provenance/confounds.** Gen32 used28,049,408 training positions,
   224,395,264 visits,1,753,088 updates, LR.01, half BT4/half CP; W1 each used26,214,400
   positions,209,715,200 visits,1,638,400 updates,LR.003,CP-only. Nominal generation
   counts misled. Historical executable identity and game overlap remain incomplete.
2. **Verified search confound.** Existing W1 production dispatch differs from Pair2;
   old frozen Arena bindings also used ConstructionV1. Common-policy Gen0 results
   match. Matched .01/.01 BT4 round8 rematches score GATE45.31% at equal nodes and
   48.44% at equal time, inconclusive; historical one-sided aggregate is not reproduced.
   Causal shares of policy/rate/data/architecture are not isolated.
3. **Correctness supported, not universally proved.** Finite differences, independent
   forward/runtime, persistence/continuation, color/special moves and service tests
   pass. No demonstrated W1 gradient/checkpoint bug explains the gap. Rates have very
   different functional effects; increasing W1 rate is not a general fix.
4. **BRE advantages supported.** Compiled additive tables are much faster; W1 shared
   compact features are not a strict superset of BRE's absolute-location pair detail.
   Lower development prediction error and useful cubic/conditional prediction signal
   do not establish chess value. Calibration/search interaction is material.
5. **Existing-model screens complete.** Original clock screen GATE35.16%,CUBIC44.53%
   (64games each); cached/calibration controls remain inconclusive. BT4-reference:
   BRE48.44%,GATE37.5% (32each). Final half mixtures: GATE43.75%,CUBIC47.66% (64each).
   Reused development openings, small samples and selection prevent confirmation.
   Do not extend unchanged mixtures to seek significance. Preserve all raw evidence.
6. **Wave3 admitted and implemented.** Freeze entire Gen32 function, train only a new
   correction; compare with continued BRE. Verified zero-head score/search parity,
   gradients, codecs/base binding, GUI, corpus continuation, actual-owner smoke.
   Initial cached search costs2.11x(GATE)/1.68x(CUBIC) Gen32: substantial risk, not
   evidence of practical efficiency. G3 and all admitted assessment protocols are now complete.
7. **Trained Wave3 findings.** Exact exposures/endpoints and trained parity verified.
   Half correction improves development prediction; additive shrinkage also helps.
   Fresh equal-node scores against continued full BRE: half-GATE57.8125%,
   half-CUBIC56.25%, half-BRE58.59375%; no expressive separation.
   At200ms versus frozen Gen32, faster half-GATE44.53125%, faster half-CUBIC
   46.09375%, half-BRE53.125%. All descriptive intervals include50%.
   GATE conditional ablation supports useful signal; cubic-term chess value remains
   unestablished. Guarded caches reduce controlled search cost26.8%/17.5%, but
   same-root node ratios still trail Gen32 (.631/.711). No positive confirmation
   candidate is ready. Details/negative findings: WAVE3-ASSESSMENT and manifests.
8. **G4 admitted.** Exact-state .001/.003 continuation in six new GUI lineages
   tests rate versus additional exposure, retaining both expressive families and
   additive controls. Bounded optimizer diagnostics support this intervention;
   they do not establish an optimal rate or guarantee improvement. G4 is verified complete. Lower rate improves same-data held-out loss in32/32
   residual generations and31/32 BRE generations, with lower training fit at.001.
   This is an optimization result, not strength. See WAVE4-ASSESSMENT.
9. **Deferred.** Further calibration/normalization, selective feature/attack/blocker
   formulations, more efficient corrections, distillation or new architecture remain
   options if evidence warrants. Do not mechanically exhaust old architecture lists
   or prematurely reject W1; no credible direction has been declared exhausted.

## Current action and exact resume point

**Human actions required after this prompt: None.** G4 was satisfied by the owner
report and persisted verification, not merely a later invocation. Do not repeat G4
or follow historical Gen33 Resume instructions. Preserve all six live roots.

Current unit: execute the prospective Wave4 development assessment in
WAVE4-ASSESSMENT. Audit/freeze/parity/calibration are complete. The model plan binds
nine distinct Latest/Best endpoints; Best63 differs from Latest64 in three residual
arms. Both continued BRE rates are mandatory controls. Fixed-node primary288 games
are CLOSED,35656 moves independently verified, no failures/caps/missing pairs.
CUBIC001 Best63 and CUBIC003 Latest64 score68.75% against Gen32, but BRE003 also
scores68.75%; all residual-minus-BRE003 intervals include zero. BRE001 scores40.625%
despite better validation. These are small selected development comparisons.
The preselected-Best100ms192-game protocol is active on the same sixteen roots.
Exact plans: `wave4-primary-node-plan.json`, `wave4-primary-clock-plan.json`;
runtime: `wave4-trained-runtime.json`; evidence under `raw/wave4`.
Resume only the active immutable plans/receipts, inspecting errors and batch limits;
never enlarge closed W3 protocols. Then analyze rate, Best/latest, inference cost and
common-opponent differences; admit focused direct comparisons against both trained
BRE controls and mechanism/calibration diagnostics as warranted. A fixed last-eight
checkpoint average improves residual development prediction at unchanged inference
size/cost; explicit hash/ancestry-bound research loading and12 tests plus96 legal
smoke plies pass. No optimizer/checkpoint is changed. See the average plans/results;
the frozen33535b4 runtime reproduced all diagnostic results exactly, and all eight
full/half averaged residuals pass actual cache/search oracles. The first diagnostic's
unretained jar identity remains explicitly unknown, not retrospectively inferred.
Fit-only rank removal on averaged cubic models gives plausible quality/cost tradeoffs;
full16 reproduction and independent prediction checks pass. The isolated sparse-rank
prototype passes actual-model parity but only rank4/8 reduces transition cost;
rank12/16 offers no gain. Defer integration pending pruned chess quality evidence.
Packed-layout oracles also pass, but variable timing baselines leave its incremental
benefit uncertain; no core/application path or pruning strength protocol has changed.
Fixed-window maturity comparison verifies GATE/BRE prediction improvement but
CUBIC full CP degradation; half calibration partly offsets it. Extra generations
are not automatically an improvement. See wave4-maturity-average-results.json.
The prospective direct admission fixes448 follow-up games against both actual BRE
Gen64 endpoints, all four residual means, additive means and full/half calibration
on eight unused development roots. See wave4-direct-admission.json; begin only after
primary-clock completion/validation. No favorable partial-score stopping.
Primary protocols continue using frozen original jars. Fresh confirmation
partition1 remains unused. No architecture decision or further training is inferred
from loss or an initial equal-time defeat.

All admitted W3 protocols are closed:1152 strength-screen games and153189
independently replayed moves. Primary320, calibration320, fresh-node192 and fresh
clock/delta320. No failures/caps/missing pairs; the primary short-clock study's two
legal fallbacks remain included and documented. Never pool reused roots or
chronologically separate runtimes as independent confirmation. The32 fresh roots
share some early prefixes; pair intervals are descriptive, not verified independent
opening-family intervals. Full results: `wave3-primary-results.json`,
`wave3-calibration-results.json`, `wave3-fair-control-node-results.json`,
`wave3-clock-results.json`.

The G4 pause sentinel may be removed after the new immutable plans are recorded.
Wave4 assessment is active; no autonomous training is admitted. Tournaments remain one game,
one worker/engine,no pondering,64MiB TT,768MiB JVM,ActiveProcessorCount1,
BelowNormal,12games/240seconds per batch, finishing the current game. No competing
heavy jobs or substantive Codex training.

## Evidence, limitations and provenance

All99 W3 initial/intermediate endpoints remain preserved; CUBIC Best31 differs from
latest32. G3 used32 generations/262144 added updates per family. Original source
progress started at zero and overlaps Gen32 history. Hybrid stable geometry splits
and raw cursors differ from BRE sequential holdout; equal counts are not identical
data or effective optimization. W4 copies each family's existing state, matching
its two rate arms while retaining those cross-family confounds.

Actual owner readiness verifies691 original checkpoint/history/settings/reference/
session/lock files unchanged, plus source cursor/selection/configuration hashes
separately bound during preparation. See the scope correction in WAVE4/readiness:
the inventory excluded the acquisition directory, not scratch. Frozen Gen32's
bundled SHA remains exact. No W1/W3/frozen model was overwritten or trained.

Correctness evidence includes gradients, codecs, exact continuation and actual-model
cache/search parity. GATE/CUBIC guarded delta caches each passed eight balanced
6333-position/48-search runs with zero integer/search differences. These empirical
and conservative numerical checks are not a universal floating-point proof.
Thirteen continuation-preparation tests passed earlier; the final owner-native test
passes all six actual configurations with twelve preserved screenshots. Seven
independent match-analysis tests pass. Failed fixture/audit assumptions are retained
and explained; no production training defect was inferred from them.

This G3 assessment root began clean at87008af/build46. Runtime changes are committed
through31b6d49/build50: material reuse0583989/build47, GATE delta b765a7f/build48,
CUBIC delta cfae34d/build49, exact-state continuation31b6d49/build50. Frozen clock
runtimes separately bind15e2ba4/build47, b765a7f/build48 and cfae34d/build49.
Later analysis and readiness changes are tooling/tests/reports and do not bump the
application. Inspect Git for the closing handoff commit. No push, deployment,
production-default replacement, finalizer or root journal creation. Root
CODEXLOG_CURRENT.md was absent; that historical handoff retained build50.

The focused G4 seek remediation began clean at d7d7599/build50 and is implemented
in fc69aeb/build51. It corrects
continuation navigation inheritance without changing evaluator, optimizer, data
selection or search policy. The earlier readiness tests used offsets below4096
and failed to detect the omission; their readiness conclusion was insufficient.
The new tests cover large inherited offsets, missing/corrupt/uncovered indexes,
locked repair, exact failed/resumed Adam equality, real source boundary decoding
and native GUI Resume. Build51's Windows image/archive, extracted-image checks and
six-lineage Swing verification using its packaged jars/bundled runtime pass.
The owner successfully completed training with the repair. Detailed historical
evidence: WAVE4 and the remediation manifest. G4 is complete; the research
objective is unchanged. That remediation ended with no running training/tournament.
Current assessment began clean at d7c53d8/build51; G4 audit/admission is committed
at8acdc4e. Research-only ablation instrumentation does not change application build51.
No finalizer, journal creation, push or production replacement.

Raw evidence is local-only under raw/wave1..wave4 and module build paths; a fresh
checkout lacks it. Verify availability before analysis. Compact tracked manifests
bind local reports, exact configurations, hashes and limitations. Preserve all
negative/failed/frozen evidence. A completed turn or implementation is not programme
success, adoption or user acceptance. The next admissible action is the active trained-W4 assessment above.
