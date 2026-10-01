# SR-001C: Bounded qsearch horizon research - 2026-09-28

The nominal horizons reduce qsearch work, but can hide mates, draws and long
exchanges. This is research evidence for GPT reconciliation, not policy
acceptance/rejection, combination or production adoption. SR-001 remains PENDING.
Repository masters **R015/F010** were read before mutation and match the preceding
unit's SHA-256 hashes. Neither canon was edited. No additional repository-local
or ancestor development/governance instructions were found; supplied governance
applies. CODEXLOG_CURRENT.md is absent.

All ten inherited SR-001B files matched its completed finalizer receipt, including
verified build 18; no unrelated changes existed. That exact unit was committed as
**1df0e773eec2318704bc24288de6b871732f360e**, `Preserve SR-001B quiescence SEE pruning research`,
before SR-001C mutation. Git flagged the inherited corpus CSV's CRLF endings as
whitespace during the boundary check; its verified bytes were preserved. The
worktree was clean immediately after the commit. SR-001A remains at
`666b6812f238a1a0ae42f896c7a19cabcce70654`.

The task's reconciled SR-001B outcome is retained here: SEE_ALL and SEE_PROMO_SAFE
are not retained standalone policies under the researched conditions; the
check/promotion-safe variant is a useful research comparator, not accepted
production policy. All three selectors remain cheap historical comparators.
SEE is workload evidence, not standalone pruning authority. No SEE pruning is
combined with the new horizons.

**Implementation.** The existing primitive research factory accepts exactly
QDEPTH_4, QDEPTH_8 and QDEPTH_12 alongside its previous selectors. An owner-final
integer sets the nominal horizon; bounded modes force the SEE-pruning field to
QSEARCH_BASELINE. The existing qply argument already counts qsearch edges from
zero at the normal boundary, independently of normal remaining depth and actual
root ply. No per-node state object, runtime class, policy abstraction, allocation
or evaluator-specific branch was added.

At qply >= limit, cancellation and terminal/draw adjudication still run first.
A non-check nonterminal node returns its actual validated static/stand-pat value,
fail-soft, with empty qPV. Existing staged legal generation is used solely to
establish terminal/nonterminal status there; no tactical ordering, search-child
preparation or tactical recursion follows. This is an explicit approximation,
not unlimited minimax equivalence. Checked nodes cannot take that exit: every
legal evasion stays eligible, subject to ordinary alpha-beta cutoffs, and both
ply counts increment. Counterchecks continue until the first applicable
non-check node. The independent absolute root-ply-256 Aborted/incomplete guard
is preserved. Neither nominal horizon nor safety interruption fabricates an
evaluation for an unresolved checked node.

Below the horizon, SR-001A terminal/draw, stand-pat, move admission (captures, EP,
all promotions), SEE/material ordering, all-evasion, actual-ply mate scoring,
fail-soft negamax and PV semantics are unchanged. No qsearch TT/hash evidence,
PVS, quiet checks, quiet-history updates, SEE/delta/futility pruning, normal
selectivity, extensions or evaluator-specific policy was introduced. Production
constructors, SearchDriver and default static leaves do not opt in.

A small explicit `searchQuiescenceSuffix` validation entry preserves consumed
qplies when the harness re-searches a qPV suffix. A fresh qply zero would wrongly
grant it a new horizon. The suffix's mate score/statistics are relative to its
new root; the harness applies the existing root-distance adjustment. This is
validation support, not an arbitrary configurable qdepth policy. Tests explicitly
show that resetting the horizon gives a different answer on the mate fixture.

**Corpus.** [The complete CSV](SEARCH_QUIESCENCE_DEPTH_SR001C_2026-09-28_CORPUS.csv)
contains **168 rows: 42 fixtures x unlimited/4/8/12**. It records FEN, oracle
method, limit, both scores, best moves/PVs, baseline max qply, candidate nodes/max
qply, and each divergence's first causally relevant truncation qply, path, FEN,
omitted continuation and categories. A witness follows the higher-valued side's
optimal continuation until the non-check horizon explains the disagreement;
this also handles optimistic values caused by hiding an opponent response.
Category tags cover the path plus omitted move; `sacrifice` denotes existing
SEE-negative evidence, not a separate tactical heuristic.

The 34 SR-001B fixtures plus two compact resources are exhaustively enumerated:
**144 root comparisons**, no alpha-beta cutoffs or ordering dependency. The
candidate oracle tracks qply independently and changes only the explicit horizon.
All scores agree with the corresponding complete restricted tree, and the largest
root enumeration is 735 nodes. Nine additional normal-depth-1 comparisons pass.
The six longer fixtures are legal derivatives of existing Kiwipete, evasion and
middlegame positions; these use the preserved full-window unlimited alpha-beta
reference, not claimed exhaustive enumeration. Each reference/witness/PV search
has a one-million-node interruption budget; all complete. Every returned PV prefix
is legal, admissible and realizes its mode's value with consumed qplies preserved.

| Fixture | Unlimited | Q4 | Q8 | Q12 | First relevant truncated qply |
| --- | ---: | ---: | ---: | ---: | --- |
| successive-xrays | -306 | -12 | -306 | -306 | Q4: 5, after mandatory check resolution |
| tactical | 1072 | 690 | 1072 | 1072 | Q4: 4 |
| mate-five | 32763 | 2659 | 32763 | 32763 | Q4: 4 |
| draw-five | 0 | -232 | 0 | 0 | Q4: 4 |
| tail-kiwipete-e1d1 | 300 | 177 | 213 | 262 | Q4: 4; Q8: 8; Q12: 12 |
| tail-kiwipete-f3h3 | 590 | 258 | 463 | 590 | Q4: 4; Q8: 8 |
| tail-evasion- | -391 | -470 | -391 | -391 | Q4: 4 |
| tail-evasion-f1f2 | 696 | 368 | 624 | 696 | Q4: 4; Q8: 8 |
| tail-evasion-f3d4 | 671 | 404 | 571 | 671 | Q4: 4; Q8: 8 |
| tail-middlegame-d3d4 | 133 | -16 | 133 | 133 | Q4: 4 |

Q4 differs on **10/42**, Q8 on **4/42**, Q12 on **1/42**. All three match unlimited
on the other **32 fixtures**. Of the original 34, only successive-xrays and tactical
differ (Q4 only). All earlier SEE-pruning counterexamples survive these horizons:
the short checking mate, non-checking clearance and promotion/draw resources.
That does not establish general sacrifice preservation.

Distinct losses and qualifications:

- `mate-five`, FEN `7k/5prp/4QB2/8/8/7R/8/4K2R w - - 0 1`: unlimited PV
  `h3h7 h8g8 h7g7 g8f8 e6f7` mates on qply 5. Q4 terminates at non-check qply 4
  before `e6f7`, returning 2659 rather than 32763. Q8/Q12 retain the mate.
- `draw-five`, FEN `7k/2b4n/1p6/qK6/8/Q7/8/R7 w - - 0 1`: unlimited PV
  `a3a5 b6a5 a1a5 c7a5 b5a5` reaches king versus king+knight, adjudicated drawn.
  Q4 stops before the last king capture at qply 4 and scores -232; Q8/Q12 retain
  the draw. Current-node draw precedence is intact; the future draw is hidden.
- The evasion derivatives lose promotion continuations at horizons 4 and 8.
  For example, f3d4's unlimited PV is `b6d4 g1h1 b2a1q d1a1 d4a1 b4a3 a8a7
  f1a1 g7h6`, score 671; Q8 scores 571. Its causal alternate branch stops at
  qply 8 before `a1f6`. Promotions themselves are never filtered out.
- Kiwipete after `e1d1` needs evidence beyond all three horizons. Q12 stops after
  `h3g2 f3g2 b4c3 d2c3 a6e2 d1e2 e6d5 e5g6 f7g6 g2g6 e8d8 g6f6`, omitting
  `g7f6` at qply 13. Its unlimited best PV is only 11 plies long: a short final
  PV does not mean all relevant alternatives fit that horizon. Baseline observed
  max qply is 31. Q8's earlier witness omits `f7g6` on qply 9.
- Successive-xrays becomes overoptimistic: Q4 resolves a checked node at the
  horizon, then stops at non-check qply 5 before `d7d4`, changing -306 to -12.
  The horizon error can have either sign and can include a missed mate.
- No stalemate-specific loss, EP-specific loss, or mate/draw requiring more than
  eight plies was established in this bounded corpus. Existing short stalemate,
  EP and underpromotion fixtures pass; HCE still prefers stand-pat in the inherited
  rook-underpromotion fixture. These are coverage limits, not preservation proofs.

**Headless benchmark.** [All 105 rows](SEARCH_QUIESCENCE_DEPTH_SR001C_2026-09-28_BENCHMARK.csv)
include exact FEN, requested/completed depth, completion flag, best move, score,
PV, normal/q/total nodes, maximum observed qply, median ms, NPS and run conditions.
Same seven positions, HCE, TT off, one thread, one-million-total-node budget,
12 warmups/seven samples with rotating mode order; Windows 11 amd64, Ryzen 5 5500,
OpenJDK 21, `-Xms256m -Xmx256m -Xbatch`. Timings include search invocation setup,
exclude owner construction/FEN parsing and semantic verification. Preliminary
unwarmed screens are excluded. Warmed runs were sequential. Repeated results
were deterministic; completed PV prefixes were verified outside timing.

All bounded attempts completed at requested normal depth. Unlimited Kiwipete
depth 4 consistently hit the unchanged budget: **800 normal + 999200 qnodes**,
observed max qply **32**, median **463.991 ms**, completedDepth=-1, INVALID score,
no move/PV. No budget was raised. Its work is not a completed baseline value or
a valid denominator for completed-value comparisons. The harness explicitly
flags incomplete aggregate comparisons; below, depth-4 relative comparisons
use the six mutually completed positions only.

The table sums per-position medians for completed positions. Nodes are entered
nodes; qsearch boundaries count only as qnodes, while CONTROL static leaves count
as normal nodes. Max qply counts qsearch edges, not root ply or nominal depth.

| Depth | Mode | Completed | Normal | Qnodes | Total | Sum ms | Max qply |
| ---: | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 2 | CONTROL | 7/7 | 793 | 0 | 793 | 0.486 | 0 |
| 2 | Unlimited | 7/7 | 174 | 51464 | 51638 | 26.031 | 28 |
| 2 | Q4 | 7/7 | 174 | 7257 | 7431 | 4.228 | 5 |
| 2 | Q8 | 7/7 | 174 | 22594 | 22768 | 12.548 | 11 |
| 2 | Q12 | 7/7 | 174 | 37685 | 37859 | 20.075 | 15 |
| 3 | CONTROL | 7/7 | 7938 | 0 | 7938 | 4.600 | 0 |
| 3 | Unlimited | 7/7 | 849 | 202368 | 203217 | 104.872 | 27 |
| 3 | Q4 | 7/7 | 907 | 37245 | 38152 | 21.382 | 7 |
| 3 | Q8 | 7/7 | 849 | 94265 | 95114 | 52.529 | 11 |
| 3 | Q12 | 7/7 | 849 | 166500 | 167349 | 87.812 | 14 |
| 4 | CONTROL | 7/7 | 59785 | 0 | 59785 | 35.238 | 0 |
| 4 | Unlimited (completed subset) | 6/7 | 5576 | 264341 | 269917 | 145.824 | 26 |
| 4 | Q4 | 7/7 | 8708 | 182885 | 191593 | 107.349 | 6 |
| 4 | Q8 | 7/7 | 8104 | 461906 | 470010 | 258.321 | 11 |
| 4 | Q12 | 7/7 | 8071 | 1020266 | 1028337 | 534.797 | 15 |

| Depth / matched positions | Candidate | Qnode reduction | Total reduction | Wall reduction | Matched max qply | Score / best differences |
| --- | --- | ---: | ---: | ---: | --- | --- |
| 2 / 7 | Q4 | 85.899% | 85.609% | 83.758% | 28 -> 5 | 2 / 0 |
| 2 / 7 | Q8 | 56.097% | 55.908% | 51.796% | 28 -> 11 | 0 / 1 |
| 2 / 7 | Q12 | 26.774% | 26.684% | 22.880% | 28 -> 15 | 0 / 0 |
| 3 / 7 | Q4 | 81.595% | 81.226% | 79.611% | 27 -> 7 | 2 / 0 |
| 3 / 7 | Q8 | 53.419% | 53.196% | 49.911% | 27 -> 11 | 2 / 0 |
| 3 / 7 | Q12 | 17.724% | 17.650% | 16.267% | 27 -> 14 | 0 / 0 |
| 4 / 6 | Q4 | 58.105% | 56.667% | 55.571% | 26 -> 5 | 0 / 0 |
| 4 / 6 | Q8 | 35.379% | 34.635% | 32.803% | 26 -> 10 | 0 / 0 |
| 4 / 6 | Q12 | 10.034% | 9.827% | 9.422% | 26 -> 15 | 0 / 0 |

Percentages above are reproducible from the CSV's rounded median ms; raw
nanosecond harness percentages differ only in the final displayed decimals.
CONTROL differs from unlimited by score/best on 4/3 positions at depth 2,
6/3 at depth 3, and 5/2 among the six completed depth-4 pairs.

At depth 2, Q4 changes Kiwipete 61 -> 32 and middlegame 111 -> 84. Q8 changes
Kiwipete's best move e2a6 -> d5e6 at score 61. At depth 3, Q4 changes Kiwipete
32 -> 150 and evasion -503 -> -153; Q8 changes them to 44 and -411. Q12 matches
every completed benchmark root's score/move, but still differs on the longer
corpus counterexample. No depth-4 unlimited Kiwipete value is claimed.

For Kiwipete depth 4, Q4 completes at 74630 total / 72139 qnodes, qply 6,
42.561 ms, score 7; Q8 at 293580 / 291085, qply 11, 160.331 ms, score 32;
Q12 at 784945 / 782450, qply 15, 402.713 ms, score 32. All choose e2a6.
Q12 still consumes nearly four-fifths of the per-attempt budget on this position.
Across all seven positions, total-node growth from depth 2 -> 3 -> 4 is
5.13x/5.02x for Q4, 4.18x/4.94x for Q8 and 4.42x/6.14x for Q12; wall growth
is 5.06x/5.02x, 4.19x/4.92x and 4.37x/6.09x respectively. Horizons enable all
these completed depth-4 attempts, but substantial normal-depth scaling remains.

Maximum measured check overrun is three qplies (overall maxima 7/11/15 for
limits 4/8/12). A focused counterchecking-block test reaches exactly limit+3
and asserts that every node expanded beyond the horizon is checked. No runaway
bounded run or natural ply-256 interruption occurred. Mandatory check resolution
does not provide an absolute recursion bound; existing draw/cancellation/storage
safety remains necessary.

Net node reductions are not an exact histogram of the original tree beyond each
horizon: changed scores/windows alter alpha-beta visitation. A limit can even
increase work locally (tail-kiwipete-f3h3: unlimited 743 qnodes, Q12 936, same score).
Errors have a clearly located approximation boundary, but no general score-error
bound; the missed mate is more consequential than a bounded centipawn deviation.
Q8/Q12 retain more sampled resources than Q4, with smaller workload savings.
These data permit comparison with SR-001B, not a universal superiority claim.
Whether to combine a horizon with a separate mechanism or move to another control
family requires GPT reconciliation and a new authorized unit; no combination was
implemented or evaluated. No playing-strength improvement is inferred.

**Validation and commands.** DEBUG was cleared for Gradle runs.

```powershell
.\gradlew.bat :app:test -Pheadless --tests '*Quiescence*ResearchTest' --tests '*ExactSearchHarnessTest' --tests '*SeeThresholdTest' --tests '*SeeGeneratedLegalTest'
.\gradlew.bat :app:test -Pheadless --tests 'com.ohinteractive.seedv6.search.exact.*' --tests 'com.ohinteractive.seedv6.tools.search.ExactSearchHarnessTest' --tests 'com.ohinteractive.seedv6.search.driver.*' --tests 'com.ohinteractive.seedv6.core.SeeThresholdTest' --tests 'com.ohinteractive.seedv6.core.SeeGeneratedLegalTest'
.\gradlew.bat :app:exactSearch '-PsearchArgs=--leaves=sr001c --tt=off --position=start,kiwipete,endgame,tactical,evasion,middlegame,transposition-pawns --depth=2 --warmups=12 --repetitions=7 --node-limit=1000000'
# Same command with --depth=3, then --depth=4 (both run).
java -Xmx256m -cp 'app/build/classes/java/main;app/build/classes/java/test' com.ohinteractive.seedv6.search.exact.QuiescenceDepthCorpus
git diff --check
```

Search/SEE regressions passed **222 tests / 24 suites in 40 seconds**. After the
incomplete-aggregate label and compact draw evidence were added, the final affected
run passed **63 tests / six suites in six seconds**, including ten new qdepth tests.
Tests cover exact limit behavior for all three constants, qply/root-ply independence,
quiet-only nonterminal existence, mate/stalemate/rule draws before the limit,
rule-50 and real-history repetition through checked overrun, fail-soft/empty PV,
absolute-ply Aborted behavior, cancellation/interruption/reuse, horizon-aware suffix
verification, and positive normal-depth transitions. Fixed boundary tests use
the existing reflection convention rather than adding a score-producing test policy.

All **28 CONTROL/unlimited depth-2/3 benchmark tree records** match SR-001B exactly
for score, best/PV, normal/q/total nodes and maximum qply. The existing 14-tree
SR-001A freeze test and all static-reference/SEE tests still pass. The only change
to an SR-001B assertion moves its invalid-selector example from 4 to 5 because 4
is now explicitly authorized. The B reports/CSVs and A implementation tests remain
unchanged. No required differential validation is unavailable.

Deliberately skipped: unrelated full-suite validation, slow NNUE/training suites,
GUI/browser tests (no integration dependency), long strength/self-play matches,
depths beyond 4, a global tactical-corpus programme, and allocation/JIT profiling.
No forced unlimited Kiwipete completion or larger node budget was attempted.
Allocation claims are source inspection; this evidence is specific to shallow HCE
TT-off searches, not neural evaluation, production TT/PVS or time management.

Changed files: ExactSearch.java, ExactSearchHarness.java, QuiescenceOracle.java,
QuiescenceSeeResearchTest.java and ExactSearchHarnessTest.java; new
QuiescenceDepthCorpus.java, QuiescenceDepthResearchTest.java, this report and its
two CSVs. VERSION_STATE.txt is finalized separately by the verified maintained
helper: begin succeeded at build 18; runtime-impact decision is bump, with its
verified result reported in the completion. SR-001C remains uncommitted; no push
or unrelated changes. Root journal remains absent.

Human actions required after this prompt: **None to complete SR-001C research.**
GPT programme reconciliation is required before accepting/rejecting a qdepth
policy, combining mechanisms, production adoption or canon updates (blocking
for those later actions). Neither Search canon was changed.
