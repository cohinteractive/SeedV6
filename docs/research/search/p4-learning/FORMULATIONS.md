# Formulations and decisions

## HL-R1 - request-scoped worker-private learning

Explicit `retain` profile, flags36962303, built on technical control80's production
flags3407871. Retain main/capture/immediate-continuation history and killer/countermove
guidance through iterations, aspiration retries and W4 root jobs within one request.
Always reset path/static-evaluation/extension scratch. Clear learned tables on
request entry/end, new-game reset and abortion; no cross-request retention. W4 root
ordering no longer clears owner learning; helpers keep their private learning
instead of having quiet tables overwritten by repeated owner seeding. No shared
mutable history, new TT policy, evaluator or clock policy.

Purpose: stop discarding useful prior decisions within one search and establish a
causal lifetime control for downstream effort allocation. Existing gravity updates,
nominal filtering, selective protections and LMR remain. This is an experimental
formulation, not accepted production or a proven strength improvement.

Frozen first prototype: `retain-v1-runtime-manifest.json`, `raw/retain-v1-source/`.
16 focused tests passed;18 instrumented requests/domain plus18 clean W1 neutrality
requests passed. In nine W1 roots, zeros among eligible combined-history decisions
fell from36,306/64,964 to13,515/68,045. The negative band(-64..-1) grew from26,343
to49,882. Retention increases exercised learning but does not automatically save
work: starting-position depth8 rose85,443 to127,464 nodes; some capped requests
completed one less depth. W4 varied differently. No strength/speed inference.

The32-pair W1 reverse ablation against actual frozen80 passed all operational
audits but scored19W/20D/25L,45.31%, paired normal95 interval35.61-55.01%.
This adverse/inconclusive development result does not establish a regression,
but provides no support for selecting retention alone. It cost1933.56s wall.

## HL-G1 - retained learning with bounded history-guided effort

Explicit `guided` profile, flags104071167. Adds HISTORY_EFFORT to HL-R1. On already
eligible late quiet reductions, combine main history with half immediate continuation
(the existing quiet-ordering weighting), round signed evidence in64-unit bands
(half-band32), and subtract at most two plies from the existing reduction or add
at most two for negative history. Clamp to0..depth-2; never extend. Existing
improving/caution relief occurs first. Preserve root/hash/check/tactical/check-giving/
mate/near-draw exclusions and full verification of reduced fail-highs. Legal null
verification retains its previous reduction policy. The deeper-futility eligibility
calculation uses the same prospective reduction; its nominal margin and safeguards
remain. Thus effort and existing omission eligibility remain internally consistent.

This scale is a SeedV6 hypothesis: baseline eligible values are predominantly within
64 units, and one depth6 cutoff contributes36 before gravity. An external divisor
of several thousand would almost never act. The bounded2 cap limits a first
experiment; it is not externally validated calibration or a blind parameter grid.
Retention-only is the reverse ablation. Verify actual up/down changes, legal PV,
accounting, reset/cancellation, clean cost and controlled strength before selection.

Risk: small histories may be noisy; negative threshold predictions can reinforce
under-search. Nominal filtering excludes unverified reduced failures from maluses,
but does not prove learned values predict good allocation. Stronger protection can
spend saved effort and deeper futility eligibility can change. Diagnose negative
results using those dependencies before buying additional games or tuning constants.

Prototype G1 qualification:330 selected tests passed,65 UCI requests and4 stop/reset
checks passed, plus54 instrumented W1/W4 requests and27 independent clean W1
neutrality requests. Frozen `guided-v1-runtime-manifest.json` and source snapshot
retain the exact prototype. Clean costs (`cost-v1-summary.json`) measured54 requests
per domain after27 warmups/domain. G1/control geometric-mean wall ratios1.0153 W1
and0.9921 W4; R1/control0.9948/0.9918. Short windows and per-root variability preclude
a speed claim, but expose no prohibitive cost for an exploratory strength screen.
First development screen(`g1-screen-v1-w1-summary.json`):32pairs/64 normal games
against actual frozen80,30W/18D/16L,60.94% score, paired normal95 interval52.15-69.72%.
Bounded Hoeffding95 interval36.93-84.95% illustrates remaining uncertainty. Runtime
audit passed with no missing-score warnings or lifecycle failures.1931.67s total
wall. Positive exploratory signal; no independent or champion-relative inference.
The completed retention-only reverse ablation scored45.31% on the same development
roots,15.625 points below G1. This common-control exploratory comparison supports
prioritizing the integrated G1 formulation; it is not an independent isolated-component
gain or champion comparison. Keep G1 unchanged for W4 risk and fresh W1 confirmation.

The prospective reference audit completed43 first-event stratified samples, all
with complete independent reduced/nominal TT-off qtrees and legal real-history replay.
One negative-band depth3 sample crossed its threshold only at nominal depth;42 did
not cross at either depth. No population rate, flat-kernel error count or safety
defect follows. See `reference-audit-v1-summary.json` for the witness and limits.
