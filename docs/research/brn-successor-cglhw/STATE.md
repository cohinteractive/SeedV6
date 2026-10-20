# BRN successor workstream state

Updated21 October2026. **G3 and its admitted Wave3 assessment are complete.
G4 remains active. The first Gen33 attempt failed because continuation preparation
omitted inherited seek indexes. All six lineages have been repaired and verified;
human Resume/completion is blocking the next research decision.
No programme success is claimed.** This remains the original CGLHW.
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
| W4-RESGATE-LR001 / LR003 | Exact W3 latest32 continuations; omitted seek indexes repaired; no training sessions |
| W4-RESCUBIC-LR001 / LR003 | Exact W3 latest32 continuations; seek repaired; no sessions; W3 Best31 preserved |
| W4-BRE-CONTROL-LR001 / LR003 | Seek repaired; LR001 has resumable Gen33 attempt/ACTIVE budget through64, no durable updates; LR003 not started |

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
   they do not establish an optimal rate or guarantee improvement. Training is
   the next missing discriminating evidence. See WAVE4 for exact settings/gate.
9. **Deferred.** Further calibration/normalization, selective feature/attack/blocker
   formulations, more efficient corrections, distillation or new architecture remain
   options if evidence warrants. Do not mechanically exhaust old architecture lists
   or prematurely reject W1; no credible direction has been declared exhausted.

## Current action and exact resume point

**Human actions required after this prompt: Blocking: perform G4 GUI training in
all six prepared W4 lineages and report completion or failure.** This blocks
trained-W4 comparison and the next architecture/strength decision. No other human
action is required. Do not infer completion from a later invocation.

Read [WAVE4](WAVE4.md), `wave4-seek-remediation.json` and the historical
`wave4-owner-readiness.json`. **First resume W4-BRE-CONTROL-LR001 using
Resume Generation33, unchanged settings and existing session.** Its32-additional
budget still ends at64. Do not restart/import/reset the lineage. Actual Gen33
completion remains the blocking operational check; no substantive owner training
was run by Codex. All six actual lineages
were created and verified, including native GUI controls and screenshots. They
start at accepted generation32 with exact W3 latest32 weights/Adam state and copied
source progress. New bootstrap IDs differ from original W3 IDs; do not infer them
from generation labels. Latest and Best both currently point to their bootstrap.
The GUI correctly reports0 newly completed generations and next33. LR001 BRE's
failed worker left an INCREMENTAL attempt and ACTIVE session, but no partial
optimizer, Gen33 checkpoint, reservation or history. Its reported508 updates were
provisional; ordinary Resume reacquires Gen33 from the unchanged Gen32 Adam state.
The other five roots still have no session/attempt. All817 pre-repair files in
the six W4 roots, three W3 roots and frozen Gen32 checkpoint remain byte-identical;
the repair added only six source-bound CP seek indexes (986602 bytes).

Each requires32 additional generations through64, one lineage at a time: LR001
uses.001, LR003 uses.003; batch128,8epochs,seed1,one worker,131072 training positions,
held-out WDL, existing half BT4/half CP selection. Leave inherited-rate checkbox
unchecked. Full prerequisites, resume arithmetic, preservation and evidence
requirements are in WAVE4; do not manually recreate/import these lineages.

After the owner reports readiness:
1. Inspect current Git/governance and local evidence; verify actual histories,
   sessions, latest/Best IDs, immutable origins, steps, settings and cursor ranges.
   Expected new generations33..64,262144 added updates/arm. Expected final steps
   524288 for residuals and2277376 for BRE. Early stops remain explicit.
2. Freeze actual selected endpoints separately from live lineages. Check parity,
   calibration and learning trajectories; compare .001 versus .003 within family.
   Do not silently substitute Best, reset Adam or ignore missing controls.
3. Admit the next bounded development protocols only after those checks. Compare
   against frozen Gen32 and appropriately continued BRE, including calibration
   controls, fixed nodes/time, cost and nonlinear ablations where useful. Research
   delta caches remain explicit paths; GUI Arena timing is not a decisive gate.
4. If a positive practical case emerges, predeclare genuinely independent
   confirmation with opening dependence, candidate selection, multiplicity and
   stopping handled. Reserved confirmation partition1 remains unused. Otherwise
   adapt the same frontier using evidence; no fixed wave limit or automatic rejection.

All admitted W3 protocols are closed:1152 strength-screen games and153189
independently replayed moves. Primary320, calibration320, fresh-node192 and fresh
clock/delta320. No failures/caps/missing pairs; the primary short-clock study's two
legal fallbacks remain included and documented. Never pool reused roots or
chronologically separate runtimes as independent confirmation. The32 fresh roots
share some early prefixes; pair intervals are descriptive, not verified independent
opening-family intervals. Full results: `wave3-primary-results.json`,
`wave3-calibration-results.json`, `wave3-fair-control-node-results.json`,
`wave3-clock-results.json`.

No tournament, probe or autonomous training is running or queued. The owned
`build/research/brn-successor-batches.pause` sentinel prevents accidental batch
admission during G4. Remove it only when new evidence/plans justify resumed games;
never resume or enlarge a closed W3 protocol. Future tournaments remain one game,
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
CODEXLOG_CURRENT.md was absent; VERSION_STATE remains build50.

The focused G4 seek remediation began clean at d7d7599/build50. It corrects
continuation navigation inheritance without changing evaluator, optimizer, data
selection or search policy. The earlier readiness tests used offsets below4096
and failed to detect the omission; their readiness conclusion was insufficient.
The new tests cover large inherited offsets, missing/corrupt/uncovered indexes,
locked repair, exact failed/resumed Adam equality, real source boundary decoding
and native GUI Resume. Detailed evidence and final build: WAVE4 and the remediation
manifest. G4 remains active; the frontier and research objective are unchanged.

Raw evidence is local-only under raw/wave1..wave4 and module build paths; a fresh
checkout lacks it. Verify availability before analysis. Compact tracked manifests
bind local reports, exact configurations, hashes and limitations. Preserve all
negative/failed/frozen evidence. A completed turn or implementation is not programme
success, adoption or user acceptance. The next admissible dependent action is
verification of evidenced G4 training completion, not another unchanged W3 match.
