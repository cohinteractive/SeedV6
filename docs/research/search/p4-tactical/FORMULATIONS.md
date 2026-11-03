# Tactical formulations and transfer evidence

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
W4 technical qualification also passed; W4 competitive risk remains unmeasured. The first fixed W1 screen was interrupted for external PC load:63 initiated games,
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
