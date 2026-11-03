# Tactical formulations and transfer evidence

## Separate owner adoption - 4 November 2026

The owner reviewed the unsuccessful TC-R001 outcome and explicitly authorized exact
TC-P1 for normal experimental production across W1-W16, accepting the unresolved
W4 competitive warning. This bounded integration is separate from research execution.
The rolling baseline is **HL-G1 + ProbCut**, **TC_P1/flags1177812991**, **build90**;
[PRODUCTION](PRODUCTION.md) records selection, source/build identity, qualification
and practical HL_G1 rollback. `LEGACY` and frozen `probcut`/`guided` remain available.
Future authorized research normally starts from the latest owner-adopted production
formulation unless a specific objective justifies another base; features are reversible.

The original640-game W1 criterion failed (52.50%, normal95%49.37-55.63% against
required>=53% and lower>50%); the sole attempt remains spent. Historical results,
protocols, resources and conclusions remain valid and unchanged. No confirmation
allowance is renewed. W4 remains43.75%, normal95%34.94-52.56%, an adverse warning,
neither proven regression nor neutrality. Adoption does not advance the HL-G1/build84
research champion or change immutable P4-BASELINE-001/build79. No successor campaign,
push, deployment, active-bot change or release follows. Masters: **P4-R016/P4-F032**.


## Inspected external behavior and development evidence

Source revisions and locally rehashed files are pinned in
`../p4-learning/external-manifest.json` and the bootstrap manifest. These are
implementation observations, not independent strength tests:

- [Stockfish19 search.cpp](https://github.com/official-stockfish/Stockfish/blob/edb0d9db6731067ec50ce619ff372b463bc4dd5d/src/search.cpp),
  ProbCut step12: tactical move picker with a SEE threshold relative to static
  evaluation, raised beta, preliminary qsearch and conditional reduced Search.
  Existing TT values can veto attempts; successful results populate a reduced-depth
  lower bound. Improving changes margin/depth. Singular extensions instead need
  eligible TT move/value/depth and an excluded-move search. These dependencies and
  internal evaluation scale are not supplied automatically by SeedV6.
- [Bagatur5.5c Search_PVS_NWS.java](https://github.com/bagaturchess/Bagatur/blob/56d1b3651f62bc770c7cde5f4c1b0360890b9bd5/Search/src/bagaturchess/search/impl/alg/impl1/Search_PVS_NWS.java),
  lines838-980: separate TT ProbCut and capture/qsearch/reduced-Search path; the
  latter admits slightly negative SEE and uses static/improving context. Singular
  extensions require lower/exact TT support and sufficient depth, with double
  extension/multicut handling. One-reply extensions also exist. No isolated empirical
  gain was verified for these pinned Bagatur mechanisms.
- [Ethereal14 search.c](https://github.com/AndyGrant/Ethereal/blob/96cde8d2f82a3ab0536bf33572405fda96f6e236/src/search.c),
  lines496-532 and617-643/973-1007: non-PV, non-check ProbCut with a TT veto,
  noisy picker and reduced Search; qsearch precheck only at higher depth. Singular
  verification temporarily excludes the TT move and restores picker state; bounded
  double extensions and multicut interact. Its approximate Elo comments are author
  annotations without independently audited controls, not transferable measurements.

Stockfish's [mate/TB correction](https://github.com/official-stockfish/Stockfish/commit/3f5adc037e14e40d1e0e1380e3e7c5884ca528ed)
documents erroneous decisive-score handling after ProbCut changes and reports
non-regression testing. Its [later refinement](https://github.com/official-stockfish/Stockfish/commit/f00d91f8ac72de8d201f8b50968bb66b1235dc9a)
admits depth3 and avoids redundant reduced searches when qsearch would be repeated,
reporting passed STC/LTC tests. These are external authors' development claims;
raw tests were not independently audited. They motivate score-domain protection
and measuring useful work, not copying margins or presuming Seed gains.

## Verified Seed differences and first hypothesis

At baseline8e95d23d/build88, `ExactSearch.flatNegamax` uses broad extension-based
ordinary TT score suppression, legal completed hints, verified synthetic null with
a legal reduced verifier, shallow one-reply-check/recapture extensions sharing a
single credit, quiet LMR with nominal retry, NOMINAL_FUTILITY and HL-G1 effort.
`flatQuiescence` resolves terminals/draws before stand-pat, uses legal tacticals,
all check evasions and QXRAY protections. Capture/continuation/quiet histories are
private and retained within requests; ROOT_REUSE has private kernels/shared coherent
TT. Raw CP64 evaluation and mate bands differ from external engines' score scales.

**TC-P1 — bounded capture verification over HL-G1** is the first implementation
hypothesis: after existing verified-null opportunity, at interior non-check ordinary
null-window nodes of depth4-8, inspect at most three non-losing legal tacticals in
existing tactical order. Require raw evaluation+300>=beta and no imminent rule50.
Search each candidate at beta+150 with qsearch then depth-3 nominal child Search
(two plies below the ordinary child depth). Return only original beta as a selective threshold after
both ordinary-domain verifications fail high. Never manufacture a mate value.

The first version intentionally keeps full original Construction guards and uses
the existing legal-verification isolation mode: no nested null/ProbCut, learning,
heuristic TT/hints or iteration-feedback publication during probes. Real moves
extend real history; no synthetic pass. Main/q work consumes full request accounting.
No new TT foundation or correction history is selected. Useful cutoff frequency,
probe cost, search effort and tactical tradeoffs must be measured; missing TT vetoes
and conservative isolation may make this formulation too costly or ineffective.

The150/300 margins are untuned Seed CP64 hypotheses on the existing100/200-unit
futility scale, not copied cross-engine parameters. At most three candidates and
depth4-8 bound early cost. If evidence shows wasted qsearch duplication or dominant
null coverage, revise the integration instead of blindly sweeping constants.
Forced-line extension and qsearch substitutions remain alternatives, not a queued
checklist. At initial qualification, this formulation passed the W1 checks linked from STATE.
W4 technical qualification also passed; W4 competitive risk was unmeasured at that checkpoint. The first fixed W1 screen was interrupted for external PC load:63 initiated games,
62 completed. Its completed-game operational audit passes, but the batch is clock-
unqualified and incomplete; no outcomes were used to select or tune a candidate.
The unchanged formulation then proceeded to a different-input controlled V2 screen.


## Complete V2 evidence and next decision

Controlled W1 V2 versus exact84:25W/18D/21L,53.125%, paired-normal95%43.86-62.39%.
All64 games/8810 requests and input/drain audits pass. Brief PC-load spikes remain
recorded; the earlier interrupted V1 is not pooled. This is exploratory, not
independent strength evidence.

Existing observe-only predictions were calibrated using a research-only helper
against resumed nominal HL-G1, with unchanged frozen engine classes.243 completed
invocation pairs preserve score/PV, nominal nodes and retained learning;15 aborted
pairs remain separate.9467 dependent predictions span all eligible node depths4-8,
with12 null-window disagreements at depths4/5. These are conditional horizon labels,
not general error rates, error magnitudes or legality defects. No clear structural
failure or parameter adjustment is established. The current parameters are retained.

Following V2, the next selected unit was a fixed64-pair wider development sample, in two frozen
32-pair segments with no interim outcome-driven choice. It preserves129 further
development games after completion and all W4/confirmation reservations. This will
inform whether the unchanged integrated candidate merits the sole independent claim
or a justified alternative is preferable; neither confirmation nor success is automatic.

## Complete support decision, 4 November2026

The fixed64-pair support study passed operational audit and scored57.03125%
(53W/40D/35L), normal95%50.6242-63.4383%, bounded95%40.0550-74.0075%. No segment
outcomes were inspected before both passed; brief A CPU excursions were retained
as limitations before score inspection. This remains exploratory evidence.

Retain unchanged TC-P1/build89 as finalist for remaining qualification. No new
mechanism or parameter is selected. Next is a fixed32-pair W4 risk comparison
against exact84 guided, keeping prior HL-vs79 uncertainty explicit, followed by
full prospective independent-W1 freezing only if qualification remains adequate.
All640 independent games and the sole claim attempt remain unspent. No strength
acceptance, champion change or production adoption follows from development results.

## W4 investigation and unchanged W1 confirmation, 4 November2026

Two W4 risk samples scored43.75% each; adaptive48-pair pool normal95%34.94-52.56%.
The adverse warning persists, with no demonstrated neutrality or established regression.
Bounded clean cost wall1.0183/CPU0.9989 and all technical/operational checks found no
new correctness/lifecycle failure and do not explain or dismiss the strength risk.
All96 risk games are spent. Escalate before any multiworker adoption; no such adoption
is authorized. Under the original W1-only objective, proceed with unchanged TC-P1's
full fresh320-pair independent protocol, preserving W4 limitations prominently.
No additional mechanism, parameter change, criterion change or claim attempt is selected.

## Terminal disposition, 4 November2026

The sole fresh320-pair W1 confirmation completed640 games versus exact84:
235W/202D/203L,52.50%, paired-normal95%49.3690-55.6310%, bounded44.9080-60.0920%.
Technical/operational qualification passed, but both prospective strength gates failed.
TC-P1 is retained experimental evidence; the strength objective remains unmet.
Owner acceptance/reconciliation is pending. No parameter change, replacement,
renewed claim, further experiment or successor is selected or authorized.

Only the bounded capture-verification formulation was implemented. External source
inspection considered TT-supported singular/forced extensions and qsearch/capture
verification; absent ordinary TT bound/depth support and the optional F3's adverse
W4 history informed the initial choice. This does not establish that alternatives
would be better. Missing TT vetoes, conservative probe isolation/learning suppression,
overlap with verified null, selective-horizon prediction errors and parallel work/
scheduling interactions remain plausible explanations to distinguish in future
authorized work. Calibration labels do not establish their practical contribution.
The persistent W4 adverse estimate is unresolved; clean technical/cost checks cannot
prove neutrality. No specific causal defect or justified parameter change was found
before the finalist freeze, and confirmation results cannot now be used to tune it.

The sole attempt is spent. Remaining129 development games and9h21m49.47s wall do
not renew it. A different integrated hypothesis, supporting TT changes or another
independent claim would require renewed authority and prospective resources/inputs,
with the current failed outcome preserved. See RESULTS and terminal STATE.
