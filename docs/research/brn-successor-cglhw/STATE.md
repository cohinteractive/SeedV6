# BRN successor workstream state

Updated22 October2026. **G4 is complete and verified. All six arms reached64,
with exact expected continuation and preserved starting payloads. Wave4 trained
assessment is closed; G5 is complete and verified. Wave5 trained assessment is closed; G6 is complete and verified through128; Wave6 trained assessment is closed; G7 is complete and verified through160; Wave7 assessment is closed; G8 is verified complete through Gen168; Wave8 assessment is closed; G9 is verified complete through Gen176; its incomplete trained assessment is SUSPENDED BY OWNER DECISION.
No programme success is claimed.** This remains the original
CGLHW. Current assessment: [WAVE9-ASSESSMENT](WAVE9-ASSESSMENT.md); completed gate: [WAVE9](WAVE9.md).
Read [CONTRACT](CONTRACT.md), [BOOTSTRAP](BOOTSTRAP.md), [WAVE1](WAVE1.md),
[completed investigations](WAVE2.md), and [implementation/training history](WAVE3.md), and [trained assessment](WAVE3-ASSESSMENT.md), and [completed G4 training gate](WAVE4.md).

Objective: statistically credible practical chess improvement over frozen BRE-Pair2
Gen32 through useful additional expressiveness, with measured costs and compatible
search. Architectural complexity or lower validation loss alone is insufficient.

## Current status: SUSPENDED BY OWNER DECISION

Owner-directed suspension,22 October2026. **Not completed, not abandoned and not
successfully concluded.** Conventional NNUE development and Search Phase3 now
have priority because the engine needs a strong, stable evaluator and BRN/BRE
experimentation has not demonstrated sufficient practical strength improvement
to justify continuing as the current development priority. The long-term objective,
contract, trained artifacts, accepted findings and unresolved hypotheses remain.
No new research, tournament, training gate or wave is authorized without a
deliberate owner reactivation. No NNUE/stabilization work is part of this suspension.

Execution is quiescent after the active bounded launch exited normally at
2026-10-21T22:44:15.0763277Z. The waiting dispatcher was stopped separately to
prevent its queued reference comparison. The existing sentinel
build/research/brn-successor-batches.pause remains active; no new hook/automation
was installed. See WAVE9-ASSESSMENT.md's suspension section and
wave9-suspension-preservation.json for verified receipts and artifact hashes.
Final preservation verifies6,858 unique owner files unchanged and the complete
G9/live-reference file sets unchanged. Frozen runtime hashes pass. The batch
lease is released; no associated research process, scheduled task or host goal
remains. Exact execution evidence: wave9-suspension-execution.json.

G9's four owner lineages remain verified Latest176, head Best175 and BRE Best176.
There are229 preserved development games:208 in closed protocols,21 in the
incomplete scaled-BRE-clock32 protocol. Primary reference-clock32 is unstarted.
The next missing scale receipt is p005-m1-c1.json;43 total scheduled games remain
unplayed. This owner stop is independent of outcomes, not favorable optional
stopping or a closed fixed-budget result. No independent strength gain is established.

Implementation57dde44/build60 is committed and native packaging is verified;
core cache reuse passed60 targeted tests and nine actual-model probes. New clipped
pooling/frozen-calibration feasibility scripts are complete but only bounded
prediction diagnostics. No new production architecture, lineage or G10 gate exists.
Frozen tournament jars remain build59. Full hashes and limitations are in the
existing manifests; source datasets and all owner checkpoints remain preserved.

**Reactivation point:** only after a deliberate owner decision, verify inactivity,
local evidence, checkpoint/runtime hashes and intervening repository changes.
Then explicitly archive the pause and resume the unchanged scaled-BRE-clock plan
for its11 missing games, followed by the untouched32-game primary reference plan,
or record an authorized amendment. Preserve all completed games. Interpret the
remaining calibration/expressiveness comparisons before selecting further work.
The clipped threshold, data-domain and search-calibration hypotheses remain open;
they have not been accepted as a new training programme.

No action is required to maintain suspension. Separate authorization is required
for engine stabilization; deliberate owner reactivation is required for CGLHW work.
Unattributed bin/,engine-core/bin/,engine-training/bin/ outputs remain preserved
and excluded from research commits. Existing VSCode/Gradle processes are unrelated
and must not be stopped as part of research suspension.

## Pre-suspension working notes (historical; no continuation authority)

The owner reports all four G9 GUI runs complete; actual persisted evidence verifies
Gen169..176, correct settings and ancestry, 65,536 new updates per arm, and no
partial/staging or unsettled-session anomaly. Latest176 for all; Best175 for the
three calibrated heads, Best176 for BRE. All37 payload/state pairs pass24-board
runtime checks. All frozen3,845 values and excluded Adam moments remain exact.
Head data reservations match through176; BRE retains its documented separate
history/objective. No repeated training is required.

Full checkpoint/state hashes, source chains, recipes and trajectories:
wave9-trained-checkpoints.json. Read-only admission and frozen stores:
wave9-trained-admission.json and wave9-trained-model-plan.json. All1,960 owner
files were unchanged by auditing and freezing. Actual trained cache probes pass
4,340 played positions and48 full/cache search pairs per head, with no integer
or search discrepancy; see wave9-trained-diagnostics.json.

Calibration explains most consumed-panel fitting improvement. Learned Gen32
non-material coefficients are approximately.77-.78 versus the initial.25;
material coefficients are approximately.81. Cubic beats quad on five changing
held-out generations, but its mean loss over all eight is higher; the later four
favor cubic. These are fitting observations, not strength evidence or convergence.
All heads' Best175 differs from Latest176. Preserve the prospective Latest selection.

The fixed192-game development programme is in wave9-assessment-protocol.json.
Run only the frozen build59 wave9-runtime.json, one game/worker, no ponder,
64MiB TT,768MiB heap,BelowNormal, bounded12-game/240-second launches. Compare
calibrated affine/quad/cubic at time and nodes, all three with continued BRE,
and cubic/BRE with frozen Gen32. New32 development roots exclude prior exact
roots; partition1 remains untouched. No independent confirmation candidate.
Common Policy4/QXRAY; native learned calibrated mapping once, no legacy half
scaling. BRE uses its native quarter mapping in this protocol; older half-change
results are distinct evidence, not silently carried forward.

G9 pause is archived at raw/wave9/g9-completed-pause.txt following verification.
Next: complete the fixed batches and independent legal replay, interpret paired
uncertainty, calibration and costs; continue the highest-value justified research.
The affine-node32 games are closed (Q31.25%,C21.875%, both uncertain);
BRE-clock48 closes A43.75%,Q18.75%,C21.875%; degree-node16 closes C62.5%
against Q. Affine-clock32 closes Q46.875%,C28.125% versus affine;
degree-clock32 closes C40.625% versus Q. Primary reference-clock32 remains.
The fixed scale-node and scale-clock follow-ups are closed at16 games each,
scoring56.25% and59.375% for quarter-scaled cubic against its native function,
with wide uncertainty. Scaled-degree-clock is active before the final controls;
all original192 and separate80 budgets remain mandatory and unchanged.
Separately admitted80 scale-sensitivity games are fixed in
wave9-scale-followup-admission.json; use ONLY wave9-calibrated-projection-runtime.json
for those projected variants. Its24 targeted tests and five actual4,340-position/
48-search-pair probes pass; seven production/dependency jars remain build59-exact.
Primary192 games keep the original runtime and native coefficients. No app bump.
A two-arm128-update pawn-condition pilot is complete, with tiny mixed differences
against additive geometry. No-fit attribution finds pawn terms hurt both BT4
panels while helping CP; shared calibration explains the apparent broad gains.
No model export or architecture/training gate is admitted. Independent Java/Python
feature/cache checks pass4,340 positions; correction-only cached cost is about15ns,
but played sequences do not establish recursive-search cost. See the pawn-condition
impact and pawn-cost-verification records. The intentional between-batch pause
is archived and BRE-clock completed with all34 earlier receipts preserved.
The separate six-arm128-update continued-base pilot is mixed: cubic beats its
adapted affine control on only one of four consumed panels with BRE176 as base.
No newer-base architecture or training gate is justified by that result.
Its between-batch affine-clock pause preserves12 receipts and is archived in
wave9-continued-base-pause.json; the original32-game plan resumed. A label-free
cubic-to-additive projection leaves most learned cubic signal unexplained; this
does not establish its usefulness or optimal training. See basis-projection records.
King-contact conjunction and rank-one embedding-adaptation pilots also fail to
show consistent gains; all arms/panels remain in their respective records. No
new architecture is admitted. A bounded read-only new-ordinal prediction check
is admitted in wave9-fresh-data-plan.json, beyond current candidate source cursors;
no fitting is permitted on that window in this check. Execute only between games.
That read-only check is complete:2,048 valid BT4 and2,029 valid CP rows,19 invalid
CP excluded. Cubic beats adapted affine/quad on both new windows (small BT4 margin),
but fixed uniform head averages173..176 help BT4 and hurt CP. No consistent
averaging remedy or independent strength evidence. See wave9-new-window-results.json.
Neither this pilot nor scale sensitivity constitutes success.
Never extend these matches merely for significance. If calibrated cubic adds no
useful value, investigate a different information basis or selective mechanism
rather than another unchanged head extension. No substantive autonomous training.

Original assessment runtimes remain build59/e804482; research provenance6d2ab08
adds only projection/diagnostics. Implementation57dde44/build60 removes a duplicate
private BRE cache evaluation in calibrated inference. Sixty targeted tests and
nine actual-model probes pass; four paired median search-time changes are small
(.979,.974,.998,.983 new/old). Native build60 packaging is pending. The temporary
wave9-build60-pause.json preserves completed games for serial packaging and a
bounded four-arm clipped-pool feasibility diagnostic; finish the current batch,
run those jobs, archive the pause and resume the unchanged fixed programme.
Quiet-stratum/warm-start diagnostics are mixed or negative and do not admit a new
training gate. No new human gate yet. Original contract and all earlier
findings remain. Human actions required after this prompt: None at this active
assessment stage; report any later genuine gate explicitly.
Untracked bin/,engine-core/bin/,engine-training/bin/ appeared during this root;
their compiled classes/resources are unattributed environment output, preserved
and excluded from commits. Frozen experiment runtimes do not read these paths.

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
| W6-RESQUAD-R16-LR001 | Latest/Best128; exact degree2 projection from C16 Latest96, then32 verified generations; additive pairwise control |
| W6-RESCUBIC-R16-LR001 / R4-LR001 | Latest/Best128 in both; exact W5 Latest96 payload/Adam continuation; matched residual data |
| W6-BRE-CONTROL-LR001 | Latest/Best128; exact W5 BRE001 Latest96 continuation; distinct historical data/split remains a confound |
| W7 Q4 / C4 / BRE001 | Verified Latest160; Best160/158/158; all176 development games closed |
| W8 fixed C3 heads / fixed Q heads / BRE001 | Verified Latest168; Best167/167/168; all176 development games closed |
| W9 affine / affine+Q / affine+C / BRE001 | Verified Latest176; Best175/175/175/176; matched head data/frozen anchor, G9 assessment active |

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
9. **Suspended G9 frontier.** Training is verified through176. Matched calibration
   explains most fitting improvement; degree/control strength comparisons are
   preserved incomplete under the original fixed192-game protocol and80-game follow-up. Distinguish learned scale from
   new information and practical cost. No useful cubic chess gain or independent
   success is established. Do not repeat the completed owner action.
10. **Deferred.** Selective feature/attack/blocker
   formulations, more efficient corrections, distillation or new architecture remain
   options if evidence warrants. Do not mechanically exhaust old architecture lists
   or prematurely reject W1; no credible direction has been declared exhausted.

## Historical G6 preparation boundary (fulfilled)

The former blocking G6 action below is completed and verified; use the current restart point above.
G5 is complete and verified. All544 fixed W5 development games are closed, with
71731 independent legal move replays and no game failure/cap/fallback. No independent
strength confirmation or useful cubic chess advantage is established. See
WAVE5-ASSESSMENT and wave5-assessment-results.json. Do not restart those games.

Four distinct W6 owner lineages are ready: W6-RESQUAD-R16-LR001,
W6-RESCUBIC-R16-LR001, W6-RESCUBIC-R4-LR001 and W6-BRE-CONTROL-LR001.
All begin at explicitly bound W5 Latest96, rate.001,32 additional generations97..128.
Q16/C16 share exact source state/data except the explicit removal of degree3 heads
and moments in Q16. Q16 is an additive pairwise control, not an eligible higher-order
success claim. Exact roots, IDs/hashes and recipes: wave6-owner-readiness.json.

Implementation7e28740/build56 passes141 targeted tests including the actual native
owner GUI preview. Independent actual projection verifies39219 retained values and
source/cursor/seek inheritance; five actual payload/state pairs pass 24-board scores.
Four cache probes pass6333 positions and48 search root passes each; projected Q16
matches its masked C16 source on all96 search results.64 legal smoke plies and
packaged/relocated native GUI checks pass. No full-suite or substantive training claim.
Build56 executable: dist/windows/candidates/candidate-20261021T051743-83f14ec3/
SeedV6-GUI/SeedV6-GUI.exe (packaging source4faba8d5a79d).

Final preservation rehash passes710 historical W3/frozen,1405 W4,925 trained W5
and72 new W6 files; W1 untouched, not freshly rehashed. All research engines/GUI
preview processes have exited. At that historical boundary the runner pause was active and owner training had not begun.
The pause is now archived and all four arms are verified at128.

Historical gate resume instructions (now executed): after the owner reports G6, verify actual histories, recipes, parent
chains, optimizer steps and Latest/Best; expected final128 with1048576 residual or
2801664 BRE steps. Resolve material anomalies before games. Archive the pause as
raw/wave6/g6-completed-pause.txt only when completion is evidenced. Freeze actual
endpoints, then admit bounded trained Q16/C16 and continued-BRE comparisons at
practical depth and equal time. Human gate completion cannot be inferred from a
later prompt. No G5 retraining, manual Arena or other human action is required.

Historical G6 frontier (superseded by the current frontier above): (1) trained Q16/C16 degree contribution, (2) R4 maturity versus
capacity and practical cost, (3) training/search mapping alignment, (4) selective or
new corrections if these mechanisms lack value. R16 vector integration is deferred:
isolated11.8-13.3% savings do not establish search gains; R4 vector was slower.
Do not mechanically extend unchanged matches or training to obtain a favourable score.
The reserved confirmation opening partition has not been consumed.

Historical closed W4 controls (retained for continuity):

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
The owner completed G4 using build51; no repeat repair is indicated. The historical G4 assessment root
began clean at d7c53d8/build51; audit/admission8acdc4e, hybrid583310e/build52,
packed c396945/build53. Later research tooling/reports do not bump the application.
No finalizer, push, deployment, default replacement or journal creation. Root
CODEXLOG_CURRENT.md is absent. Inspect Git for later coherent evidence commits.

Historical compact implementation82331a5/build55 and packaging-sourcebd65091 are verified;
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
causation has yet been established. The frontier above is suspended; owner reactivation is required.

Compiled unary controls are implemented in2d04ae8/build53 (research-only, no bump) and pass18 targeted tests plus actual2x6333-position/48-root-pass score/search oracles,4096-row prediction reproduction and32 legal smoke plies. See wave4-unary-compiled-readiness.json. Their payload/cache size is ordinary BRE, and they save35%/26% search time against evaluating the same unary ablation with the residual caches. The admitted control strength games are now closed above.

Hybrid node-v2 scores against the identical BRE001 base: GATE71.875% (8/7/1),CUBIC59.375% (8/3/5),packed rank4 CUBIC50% (5/6/5). Small selected development evidence, not a confirmation. The fixed clock48 is now complete; its follow-up selection is frozen separately.

The complete constant-gate additive control is implemented in a6c4a9c/build54 and passes17 targeted tests,6333 actual positions/48 triple search root passes,4096 prediction rows and16 legal smoke plies. It retains all210 GATE relations in298 total compiled pairs, adds10880 immutable primitive bytes to BRE, and saves37% paired search cost versus the slow identical ablation. Ordinary Pair2 checkpoint export rejects this research layout. See wave4-constant-compiled-readiness.json. The historical bounded build pause is preserved in raw/wave4/constant-build-pause-01.txt. That G5 preparation gate is historical and fulfilled; follow the current action above.
