# BRN successor workstream state

Updated21 October2026. **G4 is complete and verified. All six arms reached64,
with exact expected continuation and preserved starting payloads. Wave4 trained
assessment is closed; G5 is complete and verified. Wave5 trained assessment is active.
No programme success is claimed.** This remains the original
CGLHW. Current evidence/protocol: [WAVE5-ASSESSMENT](WAVE5-ASSESSMENT.md).
Read [CONTRACT](CONTRACT.md), [BOOTSTRAP](BOOTSTRAP.md), [WAVE1](WAVE1.md),
[completed investigations](WAVE2.md), and [implementation/training history](WAVE3.md), and [trained assessment](WAVE3-ASSESSMENT.md), and [completed G4 training gate](WAVE4.md).

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
| W5-RESCUBIC-R4-LR001 / R16-LR001 | Completed96; Best93/95 respectively; exact compact/full bootstrap continuation verified |
| W5-BRE-CONTROL-LR001 / LR003 | Completed96; Best95/94 respectively; expected optimizer/data continuation verified |

All managed roots are under E:/SeedV6-Networks. Exact checkpoint IDs, hashes and
sources: `wave2-trained-checkpoints.json`, `wave3-lineages.json`, `wave3-trained-checkpoints.json`, `wave4-owner-readiness.json`, `wave4-trained-checkpoints.json`, `wave5-owner-readiness.json`. Frozen reference
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

**Human actions required after this prompt: None.** G5 is complete, verified from
all133 retained payload/optimizer pairs and128 new history rows. No repeated training.
Current unit: execute the prospectively frozen256-game primary W5 assessment in
[WAVE5-ASSESSMENT](WAVE5-ASSESSMENT.md) and `wave5-primary-admission.json`.
Latest/Best for all four lineages face frozenGen32 at32768 nodes and200ms/move;
eight fresh development opening pairs, common search and fixed half-change calibration.
Four actual trained cubic cache probes pass6333 positions and48 search root passes
each, with zero integer/search differences. Use ONLY preserved `wave5-runtime.json`.
The root started clean b43b761/build55. Read-only audit/freeze preserved owner models.
G5 pause is preserved at raw/wave5/g5-completed-pause.txt. Primary nodes128 are
complete; clock42 games are preserved. A temporary cubic-only research-build pause
is active. Finish its tests/oracles, preserve the pause and resume primary clock
to128 using original wave5-runtime.json. Then execute frozen direct160 and contingent
cubic-only64 protocols in their admissions. No new human action. See the assessment.
Next: finish fixed primary counts, independently replay receipts, compare R4/R16,
both continued BRE rates, endpoint selection and cost. Admit focused direct/maturity/
mechanism controls based on evidence, without a predetermined new training gate.
No independent strength confirmation or useful cubic chess advantage is established.
All admitted W4 games are closed. The research frontier is evidence-driven:

1. **Closed:** all64 BRE full-versus-half calibration controls,8970 legal moves.
   Both node scores are50%; full clock scores37.5% atLR001 and34.375% atLR003
   against their half variants. Small selected screens, not proof of optimal scaling.
   Use `wave4-bre-scale-admission.json` and frozen `wave4-average-runtime.json`.
2. **Closed:** the32-game depth control,4555 legal moves. GATE00143.75%,CUBIC001
   53.125% versus BRE001 half at131072 nodes, below their shallow point scores.
   Pair uncertainty includes no change; throughput alone is not an adequate explanation.
   See `wave4-depth-node-results.json` and the activation record.
3. **Closed:** the prospectively fixed96 hybrid games in `wave4-hybrid-direct-admission.json`:
   full GATE001, full CUBIC001 and packed rank4 CUBIC001 on the same BRE001 half
   base, each16 games at131072 nodes and16 at100ms. Use ONLY
   `wave4-packed-core-runtime.json`. Node-v2 closes48 games/7366 legal moves; clock48 closes6326 legal moves.
   Clock scores GATE34.375%,CUBIC56.25%,packed rank4 CUBIC43.75%, all uncertain.
   The original node plan was rejected before model/game execution for two positive
   budgets; preserve it and use `wave4-hybrid-direct-node-v2-plan.json` under
   `wave4-hybrid-direct-budget-amendment.json`. Same intended131072-node limit and
   internal2-second safety; zero games were discarded or replayed.
   Three selected nonlinear ablations pass actual cache/search oracles; see
   `wave4-selected-hybrid-ablation-readiness.json`. Their admitted controls follow below.
4. **Closed protocol:** `wave4-hybrid-followup-admission.json`,128 fixed games/17037 legal moves.
   GATE versus fast constant gates at nodes/time; CUBIC versus quadratic at nodes
   and compiled unary at time; CUBIC versus Gen32 and BRE003 at time on16 pairs.
   Use ONLY `wave4-constant-compiled-runtime.json`. The32 node controls are closed:
   GATE versus constant gates50%,CUBIC versus quadratic34.375%, both uncertain.
   The32 internal clock controls are also closed: GATE versus fast constant gates
   31.25%,CUBIC versus compiled unary50%. External clocks close32 games each:
   CUBIC versus Gen32 54.6875% and versus BRE003 half71.875%. No failures/caps/fallbacks.
   Development selection,16 reused opening pairs and the unresolved exposure/ablation
   controls prevent a confirmation or architectural-success claim.
   Do not optimize the quadratic-clock result by assumption: no such match is admitted.
5. Any promising hybrid must face both BRE rates, frozen Gen32 and an expressive
   ablation. It combines two post-reference training streams; matched additional
   BRE training/data diversity may remain a human gate before causal conclusions.
   W5 preparation is admitted to test whether retraining can recover compact cubic
   quality. Distinct rank4 projection, full rank16 continuation and both BRE-rate
   controls start from actual Latest64, retain optimizer state and add32 generations
   at the now-completed G5 gate. See the active W5 assessment above.
   Fast additive ablations are implemented and verified: unary terms fit the
   existing BRE table; constant GATE retains four additional physical pair relations
   in an explicitly bounded research layout. Actual parity/cost and the first
   strength controls are recorded in WAVE4-ASSESSMENT; neither is a trained successor.

Source-exposure bounds now quantify the previously known cursor/split confound in
`wave4-source-exposure-bounds.json`. All BRE64 continuation ordinals lie inside
Gen32's scanned source intervals;908694..983040 residual training ordinals lie beyond
them. This is not exact selected-record/FEN overlap. Matching aggregate hybrid steps
alone cannot isolate expressiveness from source coverage or optimization behaviour.

Before launch, inspect active Java research processes and preserved receipts. Never
start a competing runner, change frozen plans/runtimes, enlarge closed protocols or
stop on a favourable partial score. The batch wrapper holds a worktree lease and
rejects active matches/diagnostics; each process binds its launcher hash. The separate
read-only probe wrapper is validated for active-lease/incompatible-runtime rejection
and three normal actual probes. It enforces the same lease, jar hashes, capabilities
and bounded resources after the preserved runtime mislaunch below.

Resources: one concurrent game, one worker/engine, no pondering,64MiB TT,768MiB
JVM,ActiveProcessorCount1,BelowNormal,12games/240seconds per batch (finish current
game). No concurrent heavy build/probe/training. The G4 pause sentinel was preserved
at raw/wave4/g4-completed-pause.txt and removed after audit/admission. G5 pause is preserved at raw/wave5/g5-completed-pause.txt; the temporary unary-build pause is preserved
at raw/wave4/unary-build-pause-01.txt. No game was interrupted. Substantive training remains exclusively the owner's GUI action.

## Completed W4 evidence and interpretation

Exact training: all199 initial/new/reference checkpoint+optimizer pairs pass codec,
checksums and24-board runtime/state integer parity. All192 new histories have expected
parentage,8192 updates/generation, no warnings or unsettled reservation/partial state.
Both rate arms within each family have identical data ranges/hashes; GATE/CUBIC also
match. BRE's inherited cursor/split differs, so architectures did not see identical
data. The failed BRE001 Gen33 attempt persisted no provisional updates and its
original session ultimately completed. Best63 differs fromLatest64 for GATE001 and
both CUBIC arms. See `wave4-trained-checkpoints.json`, training audit/validation and
`WAVE4-ASSESSMENT.md`. Lower-rate validation is better in32/32 residual and31/32 BRE
matched generations; this does not establish better chess or an optimal rate.

Closed strength screens (all development, not independent confirmation):

- Primary288 node/192 clock games: CUBIC001 Best63 and CUBIC003 Latest64 node68.75%
  against Gen32, but BRE003 also68.75%. Best-half clock scores GATE00143.75%,
  GATE00348.4375%,CUBIC00154.6875%,CUBIC00356.25%,BRE00146.875%,BRE00359.375%.
  All clock pair intervals include50%; no residual-minus-BRE003 separation.
- Original direct448 games,61709 legal moves, against both actual BRE64 controls,
  using fixed last-eight means. Low-rate node advantages do not consistently survive
  equal time. LR001 clock: GATE00121.875%,CUBIC00146.875%; LR003 clock:
  GATE001/00331.25%,CUBIC00153.125%,CUBIC00359.375%. Selected full-clock32 adds
 4528 legal moves; both high-rate full corrections43.75% against BRE003 half.
- Pruned64 node games,8250 legal moves: cubic rank4 versus rank16 full39.0625%,
  half51.5625%, uncertain. Better prediction does not establish harmless pruning.
- Four orientation replays reproduce595 moves/scores/depth/nodes/results exactly;
  excluded from strength. They support no loading/orientation defect explaining the
  large opening-set reversal. Opening dependence and variance remain material.

Primary uses `wave4-trained-runtime.json`; original direct/remaining scale/depth use
`wave4-average-runtime.json`; pruning uses `wave4-mask-runtime.json`. Preserve all.
All32 development roots are consumed; confirmation partition1 remains untouched.
Do not pool these selected/reused screens as independent games. All W3 protocols
are also closed (1152 games/153189 legal moves); their two historical legal fallbacks
remain included. See WAVE3-ASSESSMENT and its frozen manifests, not a reopened test.

## Implemented research refinements and their limits

Fixed Gen57..64 parameter averages improve residual development prediction at
unchanged model size. They are explicitly bound to eight inputs; no averaged Adam
state or fake checkpoint exists. Parameter averaging differs from function averaging
for nonlinear models. Exact loaders, cache/search oracles and smoke pass. The first
average diagnostic's unretained executable identity remains unknown; the separately
frozen33535b4 replay exactly reproduces its numerical result. CUBIC maturity is mixed:
full CP worsens fromW3 toW4, while half calibration improves. No generic maturity cure.
Fixed phase strata locate most full-CUBIC CP maturity degradation in17-24-piece
positions. A fit-only phase scale grid fails to improve both evaluation sources;
no phase-routing implementation is justified. See wave4-phase-grid-diagnostic.json.

Explicit continued-base hybrid APIs are implemented in583310e/build52. Formula:
Gen32 + .5*(BRE64-Gen32) + .5*(residual mean-Gen32). One compiled additive base,
not two evaluator calls. Eight actual compositions and two zero controls pass6333
positions/48 search-oracle root passes each,4096-row algebra/CE reproduction and128
legal smoke plies. The failed1024-node smoke remains excluded; its separate32768-node
replacement passed intentional caps. Full/constant-gate/quadratic/unary prediction
ablations show useful conditional/cubic signal on consumed panels, not chess value.
See `wave4-hybrid-readiness.json`, feasibility and ablation records. No combined
trainable checkpoint/optimizer or GUI/default adoption is implied.

Packed cubic cache is implemented in c396945/build53, explicitly opted in. Same
source payload, periodic rebuild and conservative rounding guard; only zero-head
components are packed.21 targeted tests pass. Four actual rank4/rank16/zero and
original/continued-base oracles pass4x6333 positions and48 root passes each with
zero integer/search differences.64 smoke plies pass;8 intentional caps excluded.
Rank4 search costs about11% less than same-base dense, with28409 primitive workspace
bytes versus4329; full rank16 gives no measured search saving. Source weights and
headers are separate memory costs. This optimizes the pruned function and does not
prove pruning retains chess quality. `wave4-packed-core-readiness.json` binds evidence.
The original isolated prototype retains `wave4-packed-runtime.json`; do NOT confuse
it with the integrated `wave4-packed-core-runtime.json`.

The initial integrated freeze-name collision safely refused to overwrite the old
prototype, but the shell incorrectly continued to four probes using that old runtime.
Their unsupported fields were ignored. All four are excluded in
`wave4-packed-probe-runtime-incident.json`; replacement probes verify capabilities
and all jars first. No source model/runtime was altered. Preserve the failed/negative
evidence; do not infer correctness from the excluded outputs.

## Preservation, historical provenance and acceptance

All1405 audited W4 live files and710 prior W3/frozen files were unchanged by the
read-only G4 audit/freeze and final stopping-boundary recheck; no W1 artifact was touched.
Four distinct W5 owner roots were prepared without changing any existing lineage.
All72 W5 files remain unchanged after native previews/frozen-copy probes.
See wave5-preservation-check.json for exact inventories and scope.
Source cursor/split, sparse BRE versus dense
residual Adam, sequential promotion validation and aggregate hybrid exposure remain
comparison limitations. A displayed generation or Best pointer is not an identity.

Historical G3 implementation:87008af/build46 start, material reuse0583989/build47,
GATE delta b765a7f/build48,CUBIC delta cfae34d/build49,continuation31b6d49/build50.
Seek repair fc69aeb/build51 and final evidence d7c53d8 preserve exact failed/resumed
Adam equality, real large-offset decoding and six-lineage native GUI Resume checks.
The owner completed G4 using build51; no repeat repair is indicated. Current root
began clean at d7c53d8/build51; audit/admission8acdc4e, hybrid583310e/build52,
packed c396945/build53. Later research tooling/reports do not bump the application.
No finalizer, push, deployment, default replacement or journal creation. Root
CODEXLOG_CURRENT.md is absent. Inspect Git for later coherent evidence commits.

Current compact implementation82331a5/build55 and packaging-sourcebd65091 are verified;
88 distinct JUnit tests, actual5-payload audit,3 cache/search probes and64 smoke plies pass.
Build55 native candidate: dist/windows/candidates/candidate-20261021T013302-0f255e57.
Precise lineage IDs/checkpoint hashes/settings/resume actions are in WAVE5/readiness.

Raw evidence is local-only under raw/wave1..wave5 and build paths; a fresh checkout
lacks it. Verify availability before analysis. Tracked manifests/reports bind paths,
identities/configurations/results and limitations. All historical detail and negative
findings remain in WAVE1..WAVE4-ASSESSMENT, manifests and Git history; this state
consolidation removes superseded current-action prose, not accepted evidence.
Turn completion, implementation completion and research acceptance are distinct.
No statistically credible positive strength improvement or useful-expression
causation has yet been established. Continue at the active frontier above.

Compiled unary controls are implemented in2d04ae8/build53 (research-only, no bump) and pass18 targeted tests plus actual2x6333-position/48-root-pass score/search oracles,4096-row prediction reproduction and32 legal smoke plies. See wave4-unary-compiled-readiness.json. Their payload/cache size is ordinary BRE, and they save35%/26% search time against evaluating the same unary ablation with the residual caches. The admitted control strength games are now closed above.

Hybrid node-v2 scores against the identical BRE001 base: GATE71.875% (8/7/1),CUBIC59.375% (8/3/5),packed rank4 CUBIC50% (5/6/5). Small selected development evidence, not a confirmation. The fixed clock48 is now complete; its follow-up selection is frozen separately.

The complete constant-gate additive control is implemented in a6c4a9c/build54 and passes17 targeted tests,6333 actual positions/48 triple search root passes,4096 prediction rows and16 legal smoke plies. It retains all210 GATE relations in298 total compiled pairs, adds10880 immutable primitive bytes to BRE, and saves37% paired search cost versus the slow identical ablation. Ordinary Pair2 checkpoint export rejects this research layout. See wave4-constant-compiled-readiness.json. The historical bounded build pause is preserved in raw/wave4/constant-build-pause-01.txt. That G5 preparation gate is historical and fulfilled; follow the current action above.
