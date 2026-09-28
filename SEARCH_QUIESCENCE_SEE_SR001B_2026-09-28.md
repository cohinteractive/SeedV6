# SR-001B: SEE-negative qsearch pruning research - 2026-09-28

The three explicit candidates materially reduce this shallow HCE qtree, but
they lose different tactical resources. This is evidence for GPT reconciliation,
not acceptance/rejection or production adoption. SR-001 remains PENDING.
Search Contract **R015** and Research Frontier **F010** were read and verified
before mutation; both repository masters remain unchanged. No additional local
development/governance instructions were found. Root CODEXLOG_CURRENT.md is absent.

The inherited eight-file SR-001A worktree matched its completed version-finalizer
receipt byte-for-byte (all seven reported source/test/report hashes and verified
VERSION_STATE build 17). There was no unrelated work. Before experimental edits,
that exact baseline was committed as
**666b6812f238a1a0ae42f896c7a19cabcce70654**,
`Preserve SR-001A unpruned quiescence research baseline`. The worktree was then clean.
The [SR-001A report](SEARCH_QUIESCENCE_BASELINE_SR001A_2026-09-28.md) is preserved.

Implementation is one final primitive selector on ExactSearch, an overload of
`quiescenceResearch(evaluator, pruning)`, and a direct eligibility test in its
existing qsearch loop. The one-argument factory and QSEARCH_BASELINE (0) still
invoke unpruned SR-001A. Existing static-leaf constructors and the default
TT-off harness remain independently usable. All four qsearch selectors fix normal
Search to TT-off CONTROL ordered alpha-beta; no production caller opts in.

| Selector | Non-check qnode SEE-negative treatment |
| --- | --- |
| QSEARCH_BASELINE (0) | Search every admitted tactical, subject only to alpha-beta cutoffs |
| SEE_ALL (1) | Prune every SEE-negative admitted tactical |
| SEE_PROMO_SAFE (2) | Prune only SEE-negative non-promotions |
| SEE_CHECK_PROMO_SAFE (3) | Prune only SEE-negative non-promotions that do not give check |

The implementation consumes the existing SR-015 lazy ordering key's binary class;
it neither recalculates SEE nor uses a magnitude threshold. Existing SEE threshold
and generated-legal validation tests pass. Captures, EP and all promotion types
remain the admitted set; SEE-nonnegative moves are eligible in every candidate.
Checked qnodes bypass pruning and retain every legal evasion, including negative
captures and quiet blocks/king moves, subject to ordinary alpha-beta cutoffs.
Legal existence is established before pruning, so removing every tactical leaves
stand-pat rather than a false stalemate. Terminal/draw precedence, repetition and
rule state, fail-soft windows, actual-root-ply mate distances, PV prefixes,
cancellation and the existing absolute-ply incomplete-result guard are unchanged.

Check detection has no already-saved move-check bit in this path. Only a negative
non-promotion in SEE_CHECK_PROMO_SAFE is made into the existing reusable child
board and tested by the existing `checkers` helper. If retained, that same board
is searched; if pruned, evaluator child-state/history are untouched. No unmake,
temporary object, new runtime class, policy dispatch or per-move allocation is
added. This make/check work is inside measured time, even for rejected moves;
qnodes count entered recursive nodes, not these rejected child-board preparations.
Timing does not isolate the check test from the extra branches it preserves.

No normal-Search SEE pruning/SR-024, qsearch TT/hash evidence, qsearch PVS,
ordinary quiet checks, delta/futility pruning, reductions, qdepth cutoff,
evaluator-specific rule, or qsearch quiet-history update was introduced.

**Differential method and results.** The durable
[corpus CSV](SEARCH_QUIESCENCE_SEE_SR001B_2026-09-28_CORPUS.csv) contains all
**136 rows: 34 fixtures x baseline/three candidates**, including FEN, score,
best qmove/PV, qnodes, maximum qply, exhaustive nodes and causal pruning witness.
Fixtures reuse Search/SEE examples and add compact promotion, sacrifice and
clearance resources. Coverage includes winning/losing/defended captures, pins,
x-rays, EP for both colors, quiet/capture promotions and underpromotions,
checking/mating captures, stand-pat, quiet/negative-capture evasions, successive
exchanges, mate/stalemate and rule-50/insufficient-material adjudication.

The original default oracle remains full enumeration without SEE. Its optional
candidate selector independently applies the three move-eligibility predicates
using validated SEE >= 0, without production ordering keys, sorting, windows or
alpha-beta cutoffs. Every full-window result equals its corresponding complete
tree value; every PV prefix is legal, eligible and realizes that value. There are
also **12 normal-depth-1 candidate/oracle comparisons**. All pass. Root move-visit
tests independently verify eligibility, including all negative promotion types.
The fixed corpus reaches max qply 6; the largest exhaustive root has 735 nodes
(largest alpha-beta root 45 qnodes). Its test-only 200,000-node/64-ply guards never
fired. These guards throw test failure; they never return a truncated score.

| Fixture | Baseline score / PV | Divergent candidate score / PV | First relevant omission |
| --- | --- | --- | --- |
| promotion-stalemate-resource | 0 / `b7b8q d8b8` | SEE_ALL -904 / empty | `b7b8q`, promotion, gives check |
| promotion-positional-resource | -389 / `b7b8q b3a3 b8d8` | SEE_ALL -1563 / empty | `b7b8q`, promotion, gives check |
| checking-sacrifice | 32765 / `e7f8 g7f8 h6f7` | SEE_ALL and SEE_PROMO_SAFE 1076 / `e6f8 g7h6 e7f7` | `e7f8`, non-promotion, gives check |
| nonchecking-clearance | -410 / `a4c5` | All three -691 / empty | `a4c5`, non-promotion, no check |

All seven divergent rows have a SEE-negative witness at qply 0. SEE_ALL differs
on 4/34 fixtures, SEE_PROMO_SAFE on 2/34, SEE_CHECK_PROMO_SAFE on 1/34. Promotion
pruning loses a forced stalemate resource: accepting the sacrificed promoted
piece leaves White with no legal move. In the second promotion fixture Black
instead chooses a king evasion, after which White captures the rook. Both safe
variants preserve these resources. Checking-sacrifice pruning loses a mate in
three plies; only the check-safe variant preserves it. The clearance capture
opens the rook's a-file attack on the bishop, an off-destination consequence
outside the SEE exchange. Even the strongest safeguard loses that resource.
These are understandable counterexamples, not a global bound on tactical error.

Underpromotion eligibility is exercised, but HCE prefers stand-pat (1110) in the
rook-underpromotion fixture. The existing SR-001A material-evaluator test still
proves that rook promotion avoids queen-promotion stalemate. No HCE change follows.
One inherited SR-001A counterchecking-block fixture had both kings initially
checked (`4r3/8/7k/8/8/8/8/2B1K3 w - - 0 1`). It is preserved with that unit;
SR-001B uses the legal replacement `4r3/8/8/8/8/8/8/2B1K1k1 w - - 0 1`, where
Bc1e3 blocks and gives check. The new corpus asserts that the non-moving king
is initially safe. This qualifies the earlier fixture's evidence, not the
baseline implementation or its accepted programme status.

**Headless measurements.** The
[benchmark CSV](SEARCH_QUIESCENCE_SEE_SR001B_2026-09-28_BENCHMARK.csv) contains
all **70 rows**, exact FENs, requested/completed normal depth, completion flag,
best move, score, PV, normal/q/total nodes, max qply, median elapsed ms and NPS.
All five modes use the same seven SR-001A positions at normal depths 2 and 3,
HCE, TT off, one thread, a one-million-total-node cancellation budget, 12 warmups
and seven samples. Modes rotate execution order. Every repeated result is
deterministic; completed PV prefixes are revalidated outside timing under the
same candidate. All attempts and verifications completed; no budget increase
or incomplete natural search occurred.

Environment: AMD Ryzen 5 5500, Windows 11 amd64, OpenJDK 21,
`-Xms256m -Xmx256m -Xbatch`, as in SR-001A. Median search wall times include
invocation setup and check classification; exclude FEN parsing, owner construction
and semantic verification. Times below sum seven position medians, not a median
of aggregate runs. Percentages use harness nanoseconds; displayed ms are rounded.
Tiny times and raw NPS are descriptive, not strength or isolated throughput proof.
Boundary nodes count only as qnodes in qsearch modes; CONTROL counts static leaves
as normal nodes. Total = normal + qnodes; max qply starts at zero at the boundary.

| Depth | Mode | Normal | Qnodes | Total | Median-sum ms | Max qply | Score / best differences vs baseline |
| ---: | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 2 | CONTROL | 793 | 0 | 793 | 0.484 | 0 | 4 / 3 |
| 2 | QSEARCH_BASELINE | 174 | 51464 | 51638 | 26.505 | 28 | 0 / 0 |
| 2 | SEE_ALL | 174 | 8246 | 8420 | 4.699 | 19 | 1 / 1 |
| 2 | SEE_PROMO_SAFE | 174 | 10516 | 10690 | 5.796 | 20 | 1 / 1 |
| 2 | SEE_CHECK_PROMO_SAFE | 174 | 23968 | 24142 | 12.484 | 23 | 1 / 1 |
| 3 | CONTROL | 7938 | 0 | 7938 | 4.631 | 0 | 6 / 3 |
| 3 | QSEARCH_BASELINE | 849 | 202368 | 203217 | 103.824 | 27 | 0 / 0 |
| 3 | SEE_ALL | 828 | 40828 | 41656 | 22.570 | 23 | 0 / 0 |
| 3 | SEE_PROMO_SAFE | 828 | 44712 | 45540 | 24.478 | 23 | 0 / 0 |
| 3 | SEE_CHECK_PROMO_SAFE | 828 | 83250 | 84078 | 43.760 | 25 | 0 / 0 |

| Depth | Candidate | Qnode reduction | Total reduction | Wall-time change | Max-qply change |
| ---: | --- | ---: | ---: | ---: | ---: |
| 2 | SEE_ALL | 83.977% | 83.694% | -82.274% | 28 -> 19 |
| 2 | SEE_PROMO_SAFE | 79.566% | 79.298% | -78.134% | 28 -> 20 |
| 2 | SEE_CHECK_PROMO_SAFE | 53.428% | 53.248% | -52.899% | 28 -> 23 |
| 3 | SEE_ALL | 79.825% | 79.502% | -78.260% | 27 -> 23 |
| 3 | SEE_PROMO_SAFE | 77.906% | 77.590% | -76.423% | 27 -> 23 |
| 3 | SEE_CHECK_PROMO_SAFE | 58.862% | 58.626% | -57.851% | 27 -> 25 |

Relative to SEE_PROMO_SAFE, SEE_ALL saves another 21.59% / 8.69% of qnodes
at depths 2 / 3 (total 21.23% / 8.53%; wall 18.93% / 7.79%). Relative to
SEE_CHECK_PROMO_SAFE it saves another 65.60% / 50.96% of qnodes (total
65.12% / 50.46%; wall 62.36% / 48.42%). The stronger safeguard retains
materially more tree as well as the mating resource; acceptability of that cost
is a programme decision. No fixed-tree microbenchmark isolates check detection.

Only middlegame at depth 2 changes benchmark score: 111 -> 84 for all candidates,
with best `c3d5` unchanged. After normal moves `c3d5 e7d8`, the boundary FEN is
`r2q1rk1/1pp2ppp/p1np1n2/2bNp1B1/2B1P1b1/P2P1N2/1PP1QPPP/R4RK1 w - - 2 11`.
The first omitted move is `g5f6`, SEE-negative, non-promotion, no check, qply 0
(root ply 2). Baseline qPV `g5f6 g7f6` scores 111; candidates choose
`d5f6 g7f6` for 84. Forcing the omitted move gives 111 under every mode, isolating
the loss to eligibility. A focused test records this. At normal depth 3 the
bishop exchange is a normal move and all modes recover 111, confirming the
pruning boundary. An optional complete-enumeration probe of this larger boundary
hit the test oracle's 200,000-node guard; no exhaustive result is claimed there.
Full-window unpruned/candidate alpha-beta reference checks completed (848/8/8/341
qnodes respectively). The fixed bounded corpus was unaffected.

Only kiwipete at depth 2 changes benchmark best move: `e2a6` -> `d5e6` for all
candidates, with reported score 61 unchanged. Equal selective scores do not prove
that both moves have equal unpruned values. Every candidate has zero score/best
differences on the seven depth-3 roots; the compact tactical counterexamples
demonstrate why that small-sample result cannot establish tactical equivalence.

The largest baseline run is kiwipete depth 3: 108106 qnodes / max qply 26.
SEE_ALL reduces it to 25933 / 23; SEE_PROMO_SAFE to 28839 / 23; the check-safe
candidate still uses 48538 / 25 (48713 total versus CONTROL's 2707). The original
deepest evasion depth-2 qline falls from 28 to 14/16/23; its qnodes fall from
18326 to 3030/3624/6861. No natural runaway was observed, but substantial remaining
tree and qply 25 leave deeper scaling and later control research unresolved.

**Validation and reproduction.** With DEBUG unset/empty:

```powershell
.\gradlew.bat :app:test -Pheadless --tests '*Quiescence*ResearchTest' --tests 'com.ohinteractive.seedv6.tools.search.ExactSearchHarnessTest' --tests 'com.ohinteractive.seedv6.core.SeeThresholdTest' --tests 'com.ohinteractive.seedv6.core.SeeGeneratedLegalTest'
.\gradlew.bat :app:test -Pheadless --tests 'com.ohinteractive.seedv6.search.exact.*' --tests 'com.ohinteractive.seedv6.tools.search.ExactSearchHarnessTest' --tests 'com.ohinteractive.seedv6.search.driver.*' --tests 'com.ohinteractive.seedv6.core.SeeThresholdTest' --tests 'com.ohinteractive.seedv6.core.SeeGeneratedLegalTest'
.\gradlew.bat :app:exactSearch '-PsearchArgs=--leaves=sr001b --tt=off --position=start,kiwipete,endgame,tactical,evasion,middlegame,transposition-pawns --depth=2 --warmups=12 --repetitions=7 --node-limit=1000000'
.\gradlew.bat :app:exactSearch '-PsearchArgs=--leaves=sr001b --tt=off --position=start,kiwipete,endgame,tactical,evasion,middlegame,transposition-pawns --depth=3 --warmups=12 --repetitions=7 --node-limit=1000000'
java -cp 'app/build/classes/java/main;app/build/classes/java/test' com.ohinteractive.seedv6.search.exact.QuiescenceSeeCorpus
git diff --check
```

The Search/SEE regression run passed **211 tests / 23 suites, 39 seconds**:
static reference, ordering/staging, TT/PVS, flat comparisons, driver boundaries,
qsearch and SEE. After adding the benchmark diagnostic test, the final focused
run passed **52 tests / five suites, five seconds** (16 SR-001A, nine SR-001B,
18 harness, nine SEE); corpus export was rerun successfully. All 14 recorded
SR-001A benchmark trees match exactly: score, move/PV, total/qnodes and max qply,
both through implicit and explicit baseline selection. Tests retain terminal/draw,
real-history repetition, rule 50, actual-ply mate, fail-soft bound, interrupted/
cancelled/incomplete, reuse and checked-all-evasion behavior. No required validation
is unavailable. No new default selection or positive-depth normal pruning exists.

Individual candidates are selectable by `--leaves=see-all|see-promo-safe|see-check-promo-safe`;
`--leaves=static|qsearch|both` retains SR-001A selection. TT-on and other normal
policy overrides are rejected for these leaf experiments. No unrelated NNUE,
training, full slow suite, GUI/browser QA, strength match or deeper stress campaign
was run. No allocation/JIT profiler was run; allocation statements are source
inspection, not profiler evidence. Shallow HCE timings establish neither playing
strength nor behavior with neural evaluators, production time controls or TT/PVS.

Changed files in SR-001B: ExactSearch.java, ExactSearchHarness.java,
QuiescenceOracle.java, ExactSearchHarnessTest.java; new QuiescenceSeeCorpus.java,
QuiescenceSeeResearchTest.java, this report and its two CSVs. VERSION_STATE.txt is
handled by the verified maintained finalizer: begin succeeded at build 17 before
mutation; runtime-impact decision is bump, with verified outcome in the completion
message. SR-001B remains uncommitted for reconciliation; no push. No unrelated
work was absorbed. Neither Search canon nor the SR-001A report was edited.

The evidence supports a material tree reduction and distinct promotion/checking
sacrifice safeguards, while disproving blanket tactical preservation for any
candidate. It does not choose an SR-001 policy, establish a global error bound,
or settle whether another control is required before adoption. Baseline and all
three candidates remain selectable for that next authorized decision.

Human actions required after this prompt: **GPT programme reconciliation** before
any candidate acceptance/rejection, production adoption or canon/frontier update
(blocking for those later actions; no blocking action for this completed research
unit). No such policy decision or canon update is performed here.
