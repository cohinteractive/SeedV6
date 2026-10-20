# BRN successor workstream state

Updated21 October2026. **Human training gate G3 is verified complete. Trained Wave3 assessment is active.
No programme success is claimed.** This remains the original CGLHW.
Read [CONTRACT](CONTRACT.md), [BOOTSTRAP](BOOTSTRAP.md), [WAVE1](WAVE1.md),
[completed investigations](WAVE2.md), and [implementation/training history](WAVE3.md), and [trained assessment](WAVE3-ASSESSMENT.md), and [continuation preparation](WAVE4.md).

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

All managed roots are under E:/SeedV6-Networks. Exact checkpoint IDs, hashes and
sources: `wave2-trained-checkpoints.json`, `wave3-lineages.json`, `wave3-trained-checkpoints.json`. Frozen reference
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
   evidence of practical efficiency. G3 is now verified complete; trained costs and strength are being assessed.
7. **Current after G3.** Actual exposures/endpoints verified; trained parity passes.
   Half correction improves development prediction and equal-node strength on the
   first screen. GATE's conditional ablation supports useful signal; cubic-term
   value is unestablished. Equal-time gains remain unproved. Additive shrinkage also
   helps prediction, so fresh controls and a longer clock are next. Independent confirmation requires
   fresh opening clusters and predeclared error/multiplicity/stopping rules.
8. **Deferred.** Further calibration/normalization, selective feature/attack/blocker
   formulations, more efficient corrections, distillation or new architecture remain
   options if evidence warrants. Do not mechanically exhaust old architecture lists
   or prematurely reject W1; no credible direction has been declared exhausted.

## Current action and exact resume point

**Human actions required after this prompt: None currently.** G3 completion is
verified from all three histories/sessions/checkpoint chains, not inferred from
this invocation.32 generations each,262144 additional updates,matching requested
recipe. All99 initial/intermediate/latest/Best W3 payloads remain available.
RESCUBIC Best31 differs from latest32. Frozen identities and settings are in
`wave3-trained-checkpoints.json`; full audit in `raw/wave3/trained-audit-01`.

The four primary development plans are complete:320 games,41622 independently
replayed moves,no failed/capped games. Two first-iteration legal fallbacks (one each
engine) occurred in a single CUBIC-vs-control clock game and remain included.
See `wave3-primary-results.json` and WAVE3-ASSESSMENT for all results/uncertainty.
Against Gen32, GATE45.31% nodes/26.56% time, CUBIC46.88%/42.19%, continued BRE56.25%/48.44%.
Against continued BRE, CUBIC71.88% nodes/56.25% time; small reused samples do not
establish an architectural ranking or programme success.

The cache cost revision is verified and committed at0583989/build47, after the
research audit/screen commit5c06d70. Balanced timings found modest whole-search
savings (~4.7% GATE/~3.0% CUBIC), not elimination of the cost gap. Fourteen core tests
and actual trained/variant oracles pass; primary runtime remains frozen unchanged.

The calibration/ablation follow-up is complete:320 games,43240 independently
replayed moves,no failures/caps/fallbacks. `wave3-calibration-results.json` and
`wave3-variant-runtime.json` preserve it. Half-GATE71.875% nodes/35.9375% time,
half-CUBIC64.0625%/45.3125%,CUBICBest31 54.6875%/48.4375% vsGen32.
Unscaled optimized GATE51.5625%/CUBIC32.8125% at50ms. Short-clock rerun changes
cannot be assigned solely to the small cache speedup. Constant half-GATE37.5%
vs full half-GATE; quadratic-only half-CUBIC51.5625% vs full half-CUBIC.
All five variant cache oracles passed6333 positions and48 exact search comparisons.

The12-arm actual-GUI-loop order diagnostic also completed:192 updates/arm, no
saved models. Source-order sensitivity exists, particularly in continued BRE,
but no consistent residual-specific ordering defect is established. Production
ordering remains unchanged. The compiled half-change BRE control passed12360
actual-weight comparisons with zero integer differences and retains ordinary
single-table cost. Eight research variant tests pass. See WAVE3-ASSESSMENT.

Fresh-development status:
- `wave3-fair-control-node-plan.json` is complete192games/24063 independently
  replayed moves, no failures/caps/missing pairs. Half-GATE57.8125%, half-CUBIC56.25%,
  half-BRE58.59375% against continued full BRE; all descriptive intervals include50%.
  No expressive separation established. `wave3-fair-control-node-results.json`.
- `wave3-longer-clock-plan.json` remains active/incomplete, original192-game target,
  reference Gen32,200ms/move, same32 fresh development roots. Use unchanged
  `wave3-fair-control-runtime.json` (15e2ba4/build47) and `raw/wave3/longer-clock`.
- `wave3-gate-delta-clock-plan.json` is admitted64games on the same roots/200ms,
  faster half-GATE vsGen32. Use separate `wave3-gate-delta-runtime.json`
  (b765a7f/build48), output `raw/wave3/gate-delta-clock`. Two-game legal smoke
  independently replayed16moves; intentional eight-ply caps excluded from strength.
Never mix runtimes/receipts or substitute code in an existing plan. No optional
extension or independent-confirmation claim. One game/worker,768MiB/64MiB TT,
BelowNormal,12games/240seconds per invocation, completing the current game.

GATE guarded delta cache is verified:6333 actual positions,48 full/cache search
pairs per balanced run, all integer/search results equal; warmed search time
falls26.8%. See `wave3-gate-delta-guarded-results.json`. Explicit research factory
only; ordinary model loading unchanged. A CUBIC delta prototype then passed25143
comparisons and approximately halved transition cost. Its guarded integration passes25 selected tests and eight balanced runs of6333
positions/48 full-cache search pairs, all old/new integer/search results equal.
Warmed search time falls17.5%; worker primitive storage10162->4329bytes. See
`wave3-cubic-delta-guarded-results.json`. Admit the separate64-game
`wave3-cubic-delta-clock-plan.json` after clean runtime freeze and legal smoke.

Exact current action: CUBIC delta committed atcfae34d/build49, separately frozen in
`wave3-cubic-delta-runtime.json`; two-game/16-move legal smoke passed independent
replay (intentional eight-ply caps excluded). Tournament admission is resumed,
serially finishing longer-clock192, GATE delta64, CUBIC delta64. Resume04 log
prefixes may already exist: inspect active processes/receipts and choose unique
prefixes. No simultaneous heavy work; use the owned pause sentinel at a batch
boundary before tests/builds. Investigate any `.failed` before continuation.
Independently replay completed protocols. Exact-state GUI continuation preparation is verified on isolated fixtures
(13 passing tests, including six native GUI configurations). See WAVE4 and
`wave4-preparation-results.json`. No owner-library fork or human training gate
until clock evidence reconciliation. Resume05 prefixes are the next planned set.

Source progress started at zero and overlaps Gen32 history. Hybrid stable-layout
splits differ from BRE sequential holdout; residual tranches match each other,
not the control. Same counts are not identical data/effective optimization.
Continued BRE inherited1753088 Adam updates; corrections started without moments.

## Restart and provenance

All admitted Wave2 games and Wave3 correctness/initial-cost/smoke jobs are complete;
the primary trained-W3 screen and bounded optimizer study are complete. No autonomous training is queued. Keep one game,one worker/engine,
no pondering,64MiB TT,768MiB JVM,ActiveProcessorCount1,BelowNormal for future screens;
no simultaneous heavy jobs or substantial training by Codex.

Raw evidence is local-only under `raw/wave1`, `raw/wave2`, `raw/wave3` and module
build directories; a fresh checkout does not contain it. Check availability before
analysis. Frozen runtimes/receipts/plans retain original outcomes including failures.
The first Wave3 cache probe failed a raw-bitwise history assertion; the recorded
retry found only inherited BRE rounding (~8e-15), zero integer differences and exact
full/cache/reference search equality. Do not silently reinterpret the failed attempt.

This continuation started clean at2334760/build42. Subsequent coherent commits added
research controls/caches through build45. Wave3's qualifying implementation includes
build46 in the same closing commit; inspect Git to establish that boundary. Earlier
Wave1 chronology remains in WAVE1 and Git. No finalizer, root journal creation,
production-default replacement, push or deployment. Repository-root
CODEXLOG_CURRENT.md was absent. Finished turn, implementation readiness and accepted
workstream outcome remain distinct; owner acceptance/success are not established.



Continuation fixture verification is complete; source-state/cursor preservation,
nonzero bootstrap authentication and the actual GUI session path pass. No real W4
lineages exist yet. The owned admission sentinel was used to stop after59/192
longer-clock games (no game interrupted); remove only that sentinel after this
coherent preparation commit, then resume original longer-clock and both separately
frozen delta-clock protocols with unique resume05 log prefixes. No other heavy jobs.
