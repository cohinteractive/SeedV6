# BRN successor workstream state

Updated20 October2026. **Human training gate G3 is verified complete. Trained Wave3 assessment is active.
No programme success is claimed.** This remains the original CGLHW.
Read [CONTRACT](CONTRACT.md), [BOOTSTRAP](BOOTSTRAP.md), [WAVE1](WAVE1.md),
[completed investigations](WAVE2.md), and [implementation/training history](WAVE3.md), and [trained assessment](WAVE3-ASSESSMENT.md).

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
   Half correction improves development prediction; now prospective strength screen against Gen32 and continued BRE under common
   Policy4. For useful expressiveness, ablate conditional GATE to constant/additive
   and CUBIC to quadratic/unary, with real costs. Independent confirmation requires
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

Next: profile-guided residual cache cost reduction, preserving score/search semantics;
then freeze a new half-correction/Best31/ablation development study. Original primary
runtime/receipts remain immutable. Research correction controls pass7 tests;
18 tiny optimizer arms completed without saving models. LR.001 is a credible
continuation hypothesis, not an established optimum. No next human training gate
or independent confirmation has yet been admitted. No tournament is currently queued.

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
