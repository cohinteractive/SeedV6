# External mechanisms and transfer decisions

Pinned source bytes/URLs/SHA256 are in `external-manifest.json`; local copies live
in ignored `raw/external`. These are inspected implementation facts, not independent
verification of external strength claims. No SeedV6 gain follows from engine strength.

| Implementation | Inspected relationship | SeedV6 transfer question |
| --- | --- | --- |
| [Stockfish19 search.cpp](https://github.com/official-stockfish/Stockfish/blob/edb0d9db6731067ec50ce619ff372b463bc4dd5d/src/search.cpp), lines326-330,690-715,1313-1400 | Main history decays at Search start; initialization is separate. Graded reductions consume main and multiple continuation histories alongside PV/cut-node/TT/correction context. Reduced fail-highs cause further search and continuation feedback. Some continuation storage is NUMA-shared. | Retention and the consuming reduction policy form a package. SeedV6 has different update scales, only immediate continuation, different worker jobs and no correction/TT-PV context. Start worker-private, request-scoped, bounded and with existing verification; do not copy shared access, coefficients or extensions. |
| [Ethereal14 history.c](https://github.com/AndyGrant/Ethereal/blob/96cde8d2f82a3ab0536bf33572405fda96f6e236/src/history.c), [thread.c](https://github.com/AndyGrant/Ethereal/blob/96cde8d2f82a3ab0536bf33572405fda96f6e236/src/thread.c), [search.c](https://github.com/AndyGrant/Ethereal/blob/96cde8d2f82a3ab0536bf33572405fda96f6e236/src/search.c) | Thread reset clears learned tables at new-game reset; new Search initializes transient state separately. History uses bounded gravity, threat-conditioned main/capture tables and counter/follow-up contexts. Easy shallow first-move quiet cutoffs skip learning. Quiet LMR adjusts a logarithmic base with bounded history, node and ordering context; fail-highs get further search. | SeedV6's depth-squared bonuses are far smaller and its nominal-only malus filtering changes the distribution. Measure scale and sign bias before adapting a reduction divisor. Request retention is a conservative first lifetime. Easy-cutoff suppression and broader outcomes are conditional follow-ons, not assumed necessities. The source's approximate Elo comments are not isolated audited evidence. |
| [Bagatur5.5c Search_PVS_NWS.java](https://github.com/bagaturchess/Bagatur/blob/56d1b3651f62bc770c7cde5f4c1b0360890b9bd5/Search/src/bagaturchess/search/impl/alg/impl1/Search_PVS_NWS.java), lines1300-1515 | Reductions combine main/immediate/follow-up history relative to an explicit neutral scale with improving, volatility, searched/static divergence and correction information. Tried moves register all/bad/good outcomes. Futility also consumes the resulting reduced depth; reduced fail-highs are re-searched. | SeedV6 signed gravity has a different neutral/scale and lacks several contexts. Transfer bounded conditioning, not normalized probability assumptions or evaluation correction. Keep nominal futility and existing tactical safeguards initially; inspect outcome filtering and reduced-depth consistency if evidence warrants. This inspection does not establish Bagatur table lifetime or individual-mechanism Elo. |

Initial hypothesis: retaining completed heuristic labels across a request avoids
discarding useful iteration/re-search information, while a bounded graded consumer
redirects effort. Counter-hypotheses: small/easy cutoff noise accumulates, nominal
filtering biases positive history, and sign-based pruning protection can consume
the saved effort. W4 repeated owner seeding can erase helper diversity. Measure
these before assuming retention itself improves play.

No external development-test result has been independently audited for this unit;
implementation evidence supports experiments, not a quantitative expected Elo gain.
No external source code is incorporated into SeedV6 by these inspections.

## Development evidence and deeper representation check

[Ethereal's shallow first-quiet update series](https://github.com/AndyGrant/Ethereal/commit/aef4e6c92aaa15ad77b4789cdde8027edb36056f)
reports a cumulative5.15±2.23 Elo LTC result over23,563 games, while the final
depth3 increment did not pass its own SPRT and depth4 regressed. Thus neither the
isolated threshold nor monotonic benefit is established by that report. Its
[later update-formula change](https://github.com/AndyGrant/Ethereal/commit/3997875c92271220a267155bc114a540eeec0ab1)
reports a much smaller1.41±1.13 Elo LTC result over50,480 games. These author-reported
results motivate outcome-quality investigation, not copied thresholds or a SeedV6
effect estimate. Underlying complete game sets were not independently audited.

[Stockfish CMHC development](https://github.com/official-stockfish/Stockfish/pull/6653)
reports passing STC/LTC SPRTs after103,264/47,406 games for context-dependent
continuation update weights. The direct Fishtest result endpoint was inaccessible
through the research tool; the claim is attributed to the primary PR, not independently
confirmed. SeedV6's single continuation context lacks that supporting structure.
This does not justify adding a large context table without measured transfer value.

Additional pinned Bagatur history files are hashed in
`external-followup-manifest.json`. HistoryTable maintains signed depth-squared
good-minus-bad totals and all-attempt totals per moving piece/destination, returning
an offset normalized ratio. Its clear operation halves both totals with a positive
denominator floor. ContinuationHistory wraps those tables by prior piece/destination.
The call cadence of clear is not established by these classes alone. This explains
why its neutral-centered LMR term cannot be directly applied to SeedV6's gravity
values: magnitude there is normalized by exposure, whereas SeedV6 magnitude also
reflects frequency, depth and retained lifetime. An exposure-normalized or differently
weighted SeedV6 signal is a contingent hypothesis, not selected code.
