# Correction formulations and external evidence

## Verified external implementation and reported development

Pinned Stockfish19 source [search.cpp](https://github.com/official-stockfish/Stockfish/blob/edb0d9db6731067ec50ce619ff372b463bc4dd5d/src/search.cpp)
lines85–130,812–860,1324,1629–1638,1719–1742 and
[history.h](https://github.com/official-stockfish/Stockfish/blob/edb0d9db6731067ec50ce619ff372b463bc4dd5d/src/history.h)
define pawn/minor/nonpawn/continuation corrections, gravity bounded entries and
weighted working evaluation with ordinary-domain clipping. Raw evaluation remains
distinct in TT storage. Quiet, direction-compatible completed feedback updates
correction; correction also conditions reductions and qsearch stand-pat. Shared
NUMA contexts and tuned coefficients are not transplanted into SeedV6.

The [original introduction](https://github.com/official-stockfish/Stockfish/commit/b4d995d0d910044cf4ea2ad3ee30fd1d21070cd8)
reports passing STC128,672/LTC97,422-game SPRTs. Later quadratic application
[6f9071c6](https://github.com/official-stockfish/Stockfish/commit/6f9071c64354a34970e7b5669701d0ad15b7a694)
and null-feedback [0c797367](https://github.com/official-stockfish/Stockfish/commit/0c797367a3a9783ff87422d543eb2106fea3e948)
report further passed tests. These are author-reported external outcomes, not
independent game audits or evidence that their effects transfer to SeedV6. The
current pinned source differs materially from the original pawn-only formulation.

Pinned Bagatur5.5c [Search_PVS_NWS.java](https://github.com/bagaturchess/Bagatur/blob/56d1b3651f62bc770c7cde5f4c1b0360890b9bd5/Search/src/bagaturchess/search/impl/alg/impl1/Search_PVS_NWS.java)
preserves rawStaticEval, sums pawn/material corrections into static consumers and
qsearch, and uses correction magnitude in effort adjustment. Completed non-check,
ordinary depth>=1 outcomes update without Stockfish's quiet/direction restriction.
[CorrectionHistory](https://github.com/bagaturchess/Bagatur/blob/56d1b3651f62bc770c7cde5f4c1b0360890b9bd5/Search/src/bagaturchess/search/impl/history/CorrectionHistory.java)
clips depth*(searched-raw)/8 at32; [StatHistory](https://github.com/bagaturchess/Bagatur/blob/56d1b3651f62bc770c7cde5f4c1b0360890b9bd5/Search/src/bagaturchess/search/impl/history/StatHistory.java)
uses2x16384 running-statistic contexts with count normalization at16. Source says
getEntropy supplies the mean; call cadence/lifetime is not inferred from this class.
Its [release notes](https://github.com/bagaturchess/Bagatur/releases/tag/v5.5a)
describe correction/volatility as experimental, without isolated measured gain.

Pinned Ethereal14 [search.c](https://github.com/AndyGrant/Ethereal/blob/96cde8d2f82a3ab0536bf33572405fda96f6e236/src/search.c)
and history.c/thread.c supply static/improving/history/LMR comparison context; no
correction-history implementation was found in those inspected files. Absence in
these files is not a universal engine-history claim or mechanism rejection.
Existing local external files are hash-verified against `../p4-learning/external-manifest.json`.

## CH-C1 — request-private pawn correction for main selective decisions

**Experimental, technical qualification underway; no competitive evidence.**
Profile `corrected`, flags238288895 = HL-G1/flags104071167 + EVAL_CORRECTION.
Comparator is frozen84 guided; normal production remains HL-G1. No TT foundation,
clock, evaluator, parallel cooperation or exact-reference changes.

Aim: learn recurring structural evaluation bias across iterations/re-searches and
improve futility/reverse-futility, improving/LMR and verified-null admission together.
Worker-private2x16384 signed shorts (64KiB payload) indexed by hashed white/black
pawn occupancy and side to move. Four fractional bits; correction limited to256
native CP64 score units. A depth-weighted EMA (min(depth,8)/32) approaches clipped
searched-minus-RAW residual. This is a starting SeedV6 hypothesis, not external tuning.
Working evaluation snapshots raw+correction once per main node and stays inside
the ordinary score domain; raw TT/static diagnostics keep their original values.

Learning needs completed interior depth>=2, available non-check raw/working values,
quiet nonzero chosen move, ordinary score/window, and no null probe/legal verifier.
Upper outcomes can lower an overoptimistic working estimate; lower outcomes can
raise an underestimate. Window-interior scores are still selective, not exhaustive
truth. Reject nodes with terminal/rule outcomes anywhere in their searched subtree,
current repetition, or nominal horizon reaching rule50. Conservative subtree epoch
exclusion may discard useful signals and is measured. Aborted invocations and request
end/new-game clear learned tables just as HL-G1 does. No shared or cross-request state.

Retain real chess/rule/PV/accounting semantics. Verified-null borrows/restores working
evaluation and eligibility scratch alongside raw; synthetic/verification outcomes
do not train. Qsearch stand-pat initially stays raw. That asymmetry and request-local
lifetime are testable potential limitations, not immutable architectural constraints.
Optional diagnostics count learning by bound, exclusions, correction magnitude and
changed improving decisions without changing traversal. They are disabled in games.
