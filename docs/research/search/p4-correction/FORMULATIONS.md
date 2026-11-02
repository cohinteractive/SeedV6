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

**Technically qualified; first32-pair W1 screen adverse/inconclusive.**
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

CH-C1 source d51d1479/build86 completed the first W1 screen against exact84:
22W/16D/26L,46.875%, paired95 interval35.08-58.67%. No gain established.
Costs were1.0041x W1/1.0313x W4 in short clean diagnostics; not speed claims.
The optional deeper-omission trace mislabeled working as raw, discovered after
freeze. No game observer was enabled and the diagnostic counters ignored that
argument; the source fix and independent constant-evaluator regression passed for future artifacts.

## CH-C2 - pawn correction shared by main decisions and qsearch stand-pat

**Technically qualified; build87 screen complete, adverse/inconclusive.**
Profile `corrected-q`,flags506724351. Adds CORRECTED_QSEARCH to CH-C1 while keeping
its pawn key, update eligibility/EMA/cap, request lifetime and HL-G1 effort policy.
At non-check, nonterminal ordinary qsearch nodes, raw evaluator output is validated,
then a bounded corrected working stand-pat is used. Real rule/terminal resolution
still precedes evaluation; synthetic null and legal null verification remain raw
and do not train. A changed stand-pat explicitly taints selective provenance.
Raw main evaluation and raw diagnostic callback meanings remain intact.

Hypothesis: using correction only for selective decisions creates an inconsistent
estimate relative to raw leaf scores. Sharing it with the leaf consumer may make
learned structural bias useful to returned evaluations and pruning together.
Risks: self-referential feedback, broad pawn-context generalization, increased
qnode hash work and tactical distortion. No claim that external integration transfers
its strength or constants. The same32 development pairs supplied a descriptive
common-input screen; selection remained exploratory and confirmation was separate.

## Lifetime investigation that justified C3

Pinned Stockfish19 engine.cpp/thread.cpp/uci.cpp (local text hashes in
`external-lifetime-v1-manifest.json`) connect UCI ucinewgame/Clear Hash to
Engine.search_clear, ThreadPool.clear and Worker.clear. Normal Engine.go starts
thinking without those clears; the inspected search.cpp clears correction tables
in Worker.clear, not on each ordinary request. This supported a concrete transfer
question after C2. Seed C1/C2 cleared correction at request boundaries and abort.

Seed lifecycle inspection established two supporting dependencies. ExactSearch
aborts clear current feedback, while managed requests commonly finish on an aborted
final iteration; retaining only at request end would save no useful state. C3
therefore added completed-invocation snapshots and rollback. ParallelSearch.newGame
previously cleared only shared TT, sufficient for request-only histories; C3 adds
idle/drained per-worker correction reset. These were dependencies of longer-lived
state, not defects in C1/C2 or grounds for broad parallel redesign. C3 implemented
and qualified these dependencies before competitive selection.

C2 completed22W/13D/29L (44.53125%), paired95 interval33.37-55.69%; all audits
passed. No gain established. Common-input difference from C1 is descriptive only.

## CH-C3 - decayed completed correction across Search requests

**Source d530b28a/build88 frozen; selected finalist failed independent confirmation.**
Profile `corrected-retained`, flags1043595263. Extends C2 main+qsearch correction
with a private64KiB completed-invocation snapshot.
Successful managed invocations replace the snapshot; an aborted invocation restores
it and discards unfinished updates. Each new request decays saved values7/8 toward
zero before use (about5 requests half-life). New-game and runtime errors clear all
correction state; W4 new-game explicitly forgets private snapshots after draining.
Other HL histories retain their existing request-only lifetime. Unmanaged standalone
invocations do not participate in retained learning. No shared mutable table or
parallel cooperation redesign. This addresses a verified lifetime difference from
the pinned Stockfish source, not an inferred causal explanation of C2 losses.
Additional copying/aging and stale broad pawn aliases are measured risks.

C3 qualification passed62 selected tests plus12 focused repeats, W1/W4 observations,
sequence retention/rollback/reset and clean costs,78 UCI isolation requests+8 stops,
and20 runtime requests+2 stops. All18 later W1 requests read retained feedback;12
first evaluations and14 final request outcomes change. No independent label truth or
strength follows. Clean wall ratios1.00527 W1/1.00582 W4 versus guided, short-variable
measurements. Default/exact84 controls remain equal and frozen C1/C2 artifacts intact.

C3 screen:27W/19D/18L,57.03125%, paired95 interval47.90-66.16%, conservative
bounded95 interval33.02-81.04%;64 normal games and9013 request audits passed.
Selected as the integrated finalist after development, with mixed-phase technical
qualification and W4 risk completed before the sole fresh160-pair confirmation.
These selected32-pair results established neither a gain nor an isolated retention
effect. W4 risk scored49.21875% (normal95%39.02-59.42%), inconclusive.

Independent W1 confirmation:100W/87D/133L,44.84375%, normal95%40.18-49.50%,
conservative bounded95%34.11-55.58%. Both frozen primary conditions failed.
All320 games/44,291 request audits passed; that technical validity does not imply
strength. The selected exploratory result did not generalize; causation is not
diagnosed. See [RESULTS](RESULTS.md) for qualifications, costs, resource accounting
and the terminal authority boundary. No retuning, repeat attempt or promotion.
