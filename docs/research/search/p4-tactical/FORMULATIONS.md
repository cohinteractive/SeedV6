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
checklist. This formulation passed the W1 technical qualifications linked from STATE. No
competitive result yet; W4 remains unqualified.
