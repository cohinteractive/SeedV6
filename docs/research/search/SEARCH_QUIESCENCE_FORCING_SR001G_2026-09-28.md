# SR-001G: forcingness-based ordinary quiet-check participation

Research complete for GPT reconciliation. Legal-reply count distinguishes some
valuable forcing checks: all three candidates recover the two- and three-check
smothered mates. Broad initial participation also preserves the prior initial
fork/draw resources. This evidence does not establish a satisfactory workload or
tactical-error boundary. A four-reply deeper queen fork is still lost; repeated
qualifying checks still reach the million-node budget and absolute ply 256.
Allowing two replies substantially worsens completion. No candidate is accepted,
rejected, combined, adopted or canonized here. SR-001 remains pending.

**Authority and inherited boundary.** Repository masters remain Search Contract
R015 and Research Frontier F010, unchanged SHA-256:
`52dd6d2d8a24881f1ac73f3639b051f35ad9ca379f1eeb74c6242a32b84e6882` and
`3fd49cd19c49cfd223ea7c56d4547f7b3003d5e5d5ab2babd967630e39f64187`.
The masters, their authority/maintenance rules and applicable version governance
were read before mutation. No repository/ancestor AGENTS.md was found; supplied
shared governance applies. All five modified tracked/eight untracked F paths
matched its finalized receipt, build 22 and the reported datasets. The index was
empty; no unrelated work was present. F was preserved in focused commit
`1e051bb4b66eba414d7d6463a95181afb671c9f4`. A-E boundaries remain intact.

**Mechanics and exact semantics.** Three primitive selectors, 36/37/38, reuse the
E/F quiet phase, existing reusable child boards and accepted tactical ordering.
An ordinary quiet check is a legal noncapture/nonpromotion move from a nonchecked
qnode that leaves the opponent in check. Direct, discovered, double, pawn, king
and castling forms have no additional category filter.

* FORCED_ONE: admit iff the checked child has zero or one legal evasion, at any qply.
* INITIAL_PLUS_ONE: admit every ordinary quiet check at qply 0; deeper, require zero/one evasion.
* INITIAL_PLUS_TWO: admit every ordinary quiet check at qply 0; deeper, require zero/one/two evasions.

Qply starts at zero at each normal leaf and increments on every qmove. Actual
root/path ply remains separate and controls mate distance. The inherited ordinary
quiet-check count is available for diagnostics/PV validation but never limits G.
Repeated qualifying checks and siblings remain independently eligible.

The child checkers bitboard already computed for quiet-check recognition is
passed directly to existing `Gen.genEvasion(..., legal=true, ...)`. This generator
produces the complete legal list, including king moves, captures, blocks and
promotions. It has no threshold-count interface; full generation avoids a Gen
redesign. One reusable 512-long buffer holds this temporary list. Child qsearch
then generates its own evasions normally; duplicate work is intentionally visible.
Broad initial checks bypass counting in clean INITIAL_PLUS runs. Classification
never evaluates, pushes history, returns mate directly, changes ordering or
searches a response. A zero-response check enters ordinary child search for mate.

An additional six-long reusable board handles eligibility at the existing final
ply slot: if all remaining ordinary checks are excluded and there are no tacticals,
stand-pat resolves normally; any eligible unresolved move still causes incomplete
unwind. This is not a new horizon. The two G-only buffers total 4,144 payload bytes
per owner, allocated once; optional primitive counters are separate. No recurring
node/move allocation or new policy object/dispatch layer was introduced.

Parent terminal/draw precedence, stand-pat, fail-soft negamax, legal qPV prefixes,
actual-ply mate scores, history maintenance and cancellation/incomplete semantics
remain intact. Checked nodes search every legal evasion, without a forcing filter.
Captures, en passant and all promotions retain the A tactical phases and SEE/material
ordering; eligible quiet checks follow in generated order. No qsearch quiet-history
use or updates, count cap, qdepth, SEE pruning, qTT, delta/futility, PVS, reductions,
check extensions or evaluator-specific branch participates. Board/Gen, SearchKey,
TTable and production/default consumers were not changed.

**Validation and retained references.** The Search-scoped regression passed
**273 tests in 28 suites**, zero failures/errors/skips, in 68 seconds: nine G tests,
23 harness tests, existing exact/static/A-F/oracle, driver and SEE tests. Focused
G/harness tests passed first. After the final paired-harness selector was added,
all 23 harness tests passed again (six seconds); recursive code did not change.

G tests independently enumerate full legal response sets to check 0/1/2/3+
thresholds at both qply 0 and deeper suffixes, all initial check forms, independent
siblings, repeated third checks, unfiltered checked/counterchecking evasions,
tacticals/promotions/EP, terminal/rule-50/repetition precedence, actual ply versus
qply, stand-pat/mate fail-soft cutoffs, fresh normal-leaf entry, cancellation,
thread interruption, ply-256 safety, diagnostic repeatability and absent quiet
history/TT/selective state. Nonchecking quiets are absent from observed children.

The independent full-enumeration oracle matches **40 G fixture/candidate values**:
15 FORCED_ONE (141 enumerated nodes), 15 INITIAL_PLUS_ONE (1,345), ten
INITIAL_PLUS_TWO (112), plus three normal-depth-1 entry comparisons. Eight other
G oracle attempts hit the existing suitability guard (200,000 nodes or actual
ply >64); those are explicitly `guard-rejected-not-exact`, never approximate
oracle values or accepted mismatches. Guard cases include long promotion/check
branches; complete alpha-beta can terminate through valid cutoffs where exhaustive
enumeration is impractical. The three-check mate is exhaustively verified for
FORCED_ONE/INITIAL_PLUS_ONE; its unique legal mating replies are independently
checked for every G candidate.

All **290 retained F corpus rows** exactly match, including A/E/F scores, PVs,
nodes, completion, oracle counts and diagnostics. All **63 fresh CONTROL/BASELINE/
INITIAL_ONLY primary benchmark rows** match F's deterministic records. All 93
additional paired trees match primary/retained records, including seven fresh E
trees. All 60 G diagnostic benchmark trees match clean execution. A-D tests also
pass unchanged. Every earlier selector remains independently usable.

**Corpus coverage and limits.** [CORPUS.csv](SEARCH_QUIESCENCE_FORCING_SR001G_2026-09-28_CORPUS.csv)
contains 60 fixtures / 360 rows: all 58 F fixtures plus two compact legal fork
variants. Each runs BASELINE, G's three candidates, INITIAL_ONLY and unrestricted E.
One inherited duplicate FEN remains. FORCED_ONE/INITIAL_PLUS_ONE each complete
59/60; INITIAL_PLUS_TWO completes 54/60. E completes 31/60. Incomplete scores are
blank and excluded from semantic comparisons.

Completed G score differences versus BASELINE are 9/18/17 (59/59/54 matched);
versus INITIAL_ONLY, 14/5/7; versus E, 5/1/1 (31 matched each). These reflect move-set
semantics, not automatic tactical improvements. All reported PV moves are legal
and eligible. Full-window re-search of every PV suffix agrees wherever it completes.
Two INITIAL_PLUS_TWO suffix checks do not complete: successive-xrays at suffix ply 2
hits the budget; tail-middlegame-d3d4 at suffix ply 1 reaches absolute ply 256.
Those retain legal searched prefixes, explicitly marked in `pv_validation`; no
independent full-suffix-value proof is claimed for those two rows.

[WITNESSES.csv](SEARCH_QUIESCENCE_FORCING_SR001G_2026-09-28_WITNESSES.csv) records
80 completed differences, including G comparisons against BASELINE/I/E and TWO
against ONE. In 77, following the higher-valued continuation locates the first
excluded check, its path/qply/ordinal, exact evasion count and each side's
admission. Three comparisons of promotion-positional-resource stop at a suffix
budget after `b7b8q b3c2 b8d8` (qply 3); their causal check is unresolved rather
than inferred. [RESOURCES.csv](SEARCH_QUIESCENCE_FORCING_SR001G_2026-09-28_RESOURCES.csv)
adds 25 explicit legal mechanism witnesses with complete response lists, including
off-root-PV lines. These limitations do not replace required bounded oracle tests.

**Resource-by-resource evidence.** Scores are side-to-move values; a higher number
is not automatically better chess. F = FORCED_ONE; I+1/I+2 = INITIAL_PLUS variants.

| Resource | Decisive ordinary checks (qply: replies) | F | I+1 | I+2 |
|---|---|---|---|---|
| Quiet mate in one | `g6g7`, 0:0 | 32767, retained | 32767 | 32767 |
| Knight fork wins queen, insufficient-material draw | `f5d6`, 0:4 | -752, lost | 0, retained | 0 |
| Queen sacrifice forcing mate | `e6g8`, 0:1; `h6f7`, 2:0 | 32765, retained | 32765 | 32765 |
| Rule-50 draw | `a1a8`, 0:4 | -454, lost | 0, retained | 0 |
| Pawn displacement | `g2g4`, 0:4 | 146, lost | 241, retained | 241 |
| First deeper check mates after forced capture | `f8g8 h6f7#`, 1:0 | -32766, retained | -32766 | -32766 |
| Three-check smothered mate | `c2a3`, 0:1; `d3b1`, 2:1; `a3c2`, 4:0 | 32763, retained | 32763 | 32763 |
| Original deeper queen-fork danger after `a2b3` | `f4d3`, 1:4 | lost; chooses `a2b3`, 944 | lost, 944 | lost, 944 |
| Exchange then deeper rook check | `g8g6`, 3:2 | 381; check excluded | 381 | 377; check retained |
| Further exchange-line rook check | `g6h6`, 5:3 | excluded | excluded | excluded; E returns 374 |
| First check after quiet promotion | `a8g2`, 2:6 | excluded, 1063 | excluded, 1063 | excluded, 1063 |
| Repeated checks with no additional E value | `a1a8`, 0:3; `a8a7`, 2:6 | 692 | 735; second excluded | 735; second excluded |

The direct/double-discovery and king-discovery membership witnesses have zero or
one reply and are admitted by every G candidate. The pawn-check membership
fixture has six replies; checking castling has four: FORCED_ONE excludes them,
while both INITIAL_PLUS forms admit them initially. Their original stand-pat
values remain unchanged. Capture/promotion checks bypass G eligibility entirely.
Thus FORCED_ONE retains two of the five original useful E resources; both broad
initial variants retain all five. Unlike F's count limits, G retains the third mate.
All G candidates also find the fresh-clock mate in one that E misses before its
budget stop: 738/738/6,291 qnodes and maximum qply 18/18/26, versus INITIAL_ONLY's
12 qnodes and E's one-million-node incomplete run. Generated ordering remains
unchanged; G does not prioritize the mating move.

The new fork variants provide a meaningful one-versus-two response distinction.
Both begin with legal `a2b3`, then Black's first ordinary check `f4d3+` at qply 1.
Adding legal black pawns on c3/g2 restricts White to `Kd1` or `Ke2`; adding f3
leaves only `Kd1`. Full FENs and exact replies are in the datasets/tests.

* **Two replies:** the qply-1 suffix returns -41 under F/I+1, versus **1246** under
  I+2, whose PV is `f4d3 e1e2 d3f2`, winning the queen. At the original root,
  I+1/I+2 both choose `f2e3+` and return 57; that alternative masks the threshold
  difference in the root score. The branch distinction is independently asserted.
* **One reply:** all G suffixes return **1763**, beginning
  `f4d3 e1d1 d3f2 d1c2`, versus captures-only 436. I+1/I+2 change the root choice
  from INITIAL_ONLY's `a2b3` (-436) to `f2e3` (-459). F's restricted initial set
  returns stand-pat -1138. The signs reflect perspective and different available
  continuations, not an evaluator or correctness mismatch.

The original four-reply fork remains a concrete important exclusion even though
unrestricted E cannot finish that entire fixture. Its legal queen-winning branch
and F's MAX_ONE/MAX_TWO recovery were already verified. A compact deeper-first
quiet-check draw fixture remains unfilled; it was not a prerequisite for G and
no large fixture-generation project was undertaken.

**Headless method.** Same seven FENs, HCE, normal TT off, one-million-node budget,
Java 21, `-Xms256m -Xmx256m -Xbatch`. Primary measurements use 12 warmups/seven
repetitions and upper medians, rotating multi-mode order. Depths 2/3 run all five
primary modes on all seven. At depth 4, G's three candidates run start/endgame/
tactical/pawns; FORCED_ONE/INITIAL_PLUS_ONE additionally run Kiwipete/evasion/
middlegame after completing their depth-3 runs. INITIAL_PLUS_TWO skips those three
following depth-3 failure. CONTROL/BASELINE run all seven at depth 4, including
one comparable Kiwipete baseline failure. The budget never increases.

Clean timings have both optional counter sets disabled. Separate diagnostics use
zero warmups/one repetition. Fresh INITIAL_ONLY primary runs provide all-depth
reference trees/times. Tiny standalone INITIAL_ONLY timings showed warm-up noise
(e.g. start depth 2); their historical-looking speedups are not performance
conclusions. Additional paired runs use 128 warmups/15 repetitions: seven modes
including E on E's completed sets (three positions at depth 2, two at 3/4), and
four modes I/G on four common positions at depths 2/3 and three at depth 4.
These give direct warmed comparisons without repeatedly exhausting E's budget.
No current wall-time conclusion depends on old E timing.

[BENCHMARK.csv](SEARCH_QUIESCENCE_FORCING_SR001G_2026-09-28_BENCHMARK.csv) retains
**232 rows**: 102 primary CONTROL/BASELINE/G, 21 separate INITIAL_ONLY, 49 paired
E/I/G/controls, 44 paired I/G and 16 clearly marked retained E rows for completion
context. Full FEN, requested/completed depth, move/score/PV, normal/q/total nodes,
max qply, median time, NPS, completion reason, warmups/repetitions/budget and source
identity are present. [AGGREGATE.csv](SEARCH_QUIESCENCE_FORCING_SR001G_2026-09-28_AGGREGATE.csv)
contains 48 matched comparisons with absolute sums and matched position names.

**Primary aggregate versus unlimited BASELINE.** Percentages are changes, not
reductions. Max qply below is for mutually completed positions only; failures are
reported separately. Different matched subsets cannot be compared as all-position
performance. CONTROL completes all 21; BASELINE completes 7/7, 7/7, 6/7.

| Depth | Candidate | Completed/attempted | Matched | Qnodes | Total nodes | Wall | Max qply B -> G | Score/move differences |
|---|---|---|---|---|---|---|---|---|
| 2 | FORCED_ONE | 7/7 | 7 | +43.3% | +43.2% | +101.4% | 28 -> 60 | 0/0 |
| 2 | INITIAL_PLUS_ONE | 7/7 | 7 | +44.3% | +44.2% | +101.7% | 28 -> 60 | 0/0 |
| 2 | INITIAL_PLUS_TWO | 4/7 | 4 | +66.5% | +54.1% | +60.5% | 4 -> 16 | 0/0 |
| 3 | FORCED_ONE | 7/7 | 7 | +20.9% | +20.8% | +70.9% | 27 -> 59 | 0/0 |
| 3 | INITIAL_PLUS_ONE | 7/7 | 7 | +21.7% | +21.6% | +73.2% | 27 -> 59 | 1/0 |
| 3 | INITIAL_PLUS_TWO | 4/7 | 4 | +6.8% | +6.0% | +19.0% | 6 -> 15 | 0/0 |
| 4 | FORCED_ONE | 5/7 | 5 | +56.6% | +55.4% | +105.0% | 26 -> 58 | 0/0 |
| 4 | INITIAL_PLUS_ONE | 4/7 | 4 | +24.9% | +20.9% | +53.5% | 11 -> 11 | 2/1 |
| 4 | INITIAL_PLUS_TWO | 3/4 | 3 | +18.8% | +16.2% | +47.3% | 11 -> 11 | 1/1 |

At matched completed positions, I+2 versus I+1 adds 18.6%/4.2%/0.6% qnodes and
9.4%/2.6%/0.3% wall time at depths 2/3/4, with no score/move differences. Those
small matched percentages omit the additional I+2 failures. Across all attempts,
I+2 completes 4/7, 4/7 and 3/4 versus I+1's 7/7, 7/7 and 4/7.

**Comparison with INITIAL_ONLY.** On primary mutually completed sets, FORCED_ONE
adds 42.1%/18.2%/7.9% qnodes and 91.3%/68.1%/51.9% wall time at depths 2/3/4;
I+1 adds 43.1%/19.0%/0.8% qnodes and 91.6%/70.4%/18.7% wall time. The depth-4
survivor subsets exclude G safety failures. Warmed paired I/G comparisons on the
small common sets avoid the standalone tiny-timing artifact:

| Depth | Candidate | Matched | Qnodes | Total nodes | Wall | Score/move differences |
|---|---|---|---|---|---|---|
| 2 | FORCED_ONE | 4 | -24.7% | -21.1% | -4.0% | 0/0 |
| 2 | INITIAL_PLUS_ONE | 4 | +4.3% | +3.7% | +21.1% | 0/0 |
| 2 | INITIAL_PLUS_TWO | 4 | +23.7% | +20.3% | +36.6% | 0/0 |
| 3 | FORCED_ONE | 4 | -0.6% | -0.5% | +6.2% | 0/0 |
| 3 | INITIAL_PLUS_ONE | 4 | +0.9% | +0.8% | +7.0% | 0/0 |
| 3 | INITIAL_PLUS_TWO | 4 | +5.2% | +4.6% | +10.3% | 0/0 |
| 4 | FORCED_ONE | 3 | -15.2% | -13.4% | +1.0% | 1/1 |
| 4 | INITIAL_PLUS_ONE | 3 | -0.1% | -0.1% | +15.7% | 0/0 |
| 4 | INITIAL_PLUS_TWO | 3 | +0.4% | +0.4% | +12.6% | 0/0 |

**Fresh paired comparison with unrestricted E.** Only E's completed sets are
included. All G forms lose the endgame's deeper four-reply pawn check at depths
2/3. At depth 4 it is initial: I+1/I+2 recover it, FORCED_ONE still excludes it.

| Depth | Candidate | Matched | Qnodes | Total nodes | Wall | Max qply E -> G | Score/move differences |
|---|---|---|---|---|---|---|---|
| 2 | FORCED_ONE | 3 | -80.7% | -77.6% | -69.5% | 26 -> 4 | 1/1 |
| 2 | INITIAL_PLUS_ONE | 3 | -77.3% | -74.3% | -66.4% | 26 -> 4 | 1/1 |
| 2 | INITIAL_PLUS_TWO | 3 | -77.3% | -74.3% | -66.4% | 26 -> 4 | 1/1 |
| 3 | FORCED_ONE | 2 | -74.5% | -71.5% | -71.9% | 25 -> 4 | 1/1 |
| 3 | INITIAL_PLUS_ONE | 2 | -72.9% | -70.0% | -71.3% | 25 -> 4 | 1/1 |
| 3 | INITIAL_PLUS_TWO | 2 | -72.9% | -70.0% | -71.8% | 25 -> 4 | 1/1 |
| 4 | FORCED_ONE | 2 | -95.9% | -95.1% | -93.9% | 133 -> 5 | 1/1 |
| 4 | INITIAL_PLUS_ONE | 2 | -93.7% | -92.8% | -91.2% | 133 -> 6 | 0/0 |
| 4 | INITIAL_PLUS_TWO | 2 | -93.6% | -92.7% | -91.2% | 133 -> 6 | 0/0 |

Tiny sub-millisecond measurements and rounded CSV times have limited precision.
The large tree/completion effects are deterministic; these timings are not a
JVM/cache profile or playing-strength experiment. NPS is recorded, not used alone
as evidence of improvement. No recurring allocation was added by source
inspection; runtime allocation/GC/cache behavior was not profiled.

**Classification cost and diagnostics.**
[DIAGNOSTICS.csv](SEARCH_QUIESCENCE_FORCING_SR001G_2026-09-28_DIAGNOSTICS.csv)
contains 60 G benchmark rows. Bins 0/1/2/3+ count actual quiet-check candidates
reached in the quiet phase (plus final-slot eligibility checks), not every
counterfactual move behind an alpha-beta cutoff. Required calls/moves are full
legal-evasion generation for threshold decisions. Initial diagnostic-only calls
count broad initial checks and are absent from clean timing. The inherited
`nodes_with_checks` scan also runs only with diagnostics and counts any legal
quiet check, including excluded checks. No diagnostic scan mutates evaluator,
history, eligibility or ordering.

| Depth | Candidate | Classified | Required generations | Legal replies generated | Checks searched | Check cutoffs | Max used/chain |
|---|---|---|---|---|---|---|---|
| 2 | FORCED_ONE | 24068 | 24068 | 96830 | 2529 | 824 | 18/8 |
| 2 | INITIAL_PLUS_ONE | 24283 | 24244 | 97603 | 2578 | 825 | 18/8 |
| 2 | INITIAL_PLUS_TWO | 2053235 | 2053198 | 6567894 | 810706 | 125108 | 115/54 |
| 3 | FORCED_ONE | 62070 | 62070 | 262133 | 4532 | 1187 | 18/8 |
| 3 | INITIAL_PLUS_ONE | 61284 | 61242 | 260488 | 4345 | 1100 | 18/8 |
| 3 | INITIAL_PLUS_TWO | 2315005 | 2314991 | 6870714 | 1101176 | 106651 | 103/48 |
| 4 | FORCED_ONE | 265209 | 265209 | 1172743 | 14087 | 3788 | 113/50 |
| 4 | INITIAL_PLUS_ONE | 219328 | 218479 | 990394 | 10937 | 1646 | 114/42 |
| 4 | INITIAL_PLUS_TWO | 706520 | 706030 | 1625899 | 530576 | 32 | 35/25 |

Sums include incomplete work. At depth 4 I+2 has four attempts; the other modes
have seven. Chain counts ordinary checks connected through quiet evasions,
resetting on a tactical edge; a forced countercheck does not itself consume an
ordinary-check ordinal. Pawn evasions can reset rule 50 without resetting this
chain statistic. Maximum used counts both sides' ordinary checks on one path.
Full response bins and diagnostic-only generation counts are in the CSV. Even
when trees match, legal quiet generation/make/check and evasion classification
add work outside the node count; at depths 2/3 the one-reply modes increase wall
time by roughly 70-102% versus baseline for only 21-44% more qnodes.

**Pathology and incomplete results.** The following primary failures are retained;
no score or policy truncation was substituted. Qply plus requested normal depth
is 256 for every listed absolute-safety stop.

| Depth | Position | Mode | Reason | Qnodes | Max qply |
|---|---|---|---|---|---|
| 2 | kiwipete | INITIAL_PLUS_TWO | node-budget | 999994 | 201 |
| 2 | evasion | INITIAL_PLUS_TWO | node-budget | 999998 | 228 |
| 2 | middlegame | INITIAL_PLUS_TWO | absolute-ply-256 | 71167 | 254 |
| 3 | kiwipete | INITIAL_PLUS_TWO | node-budget | 999994 | 94 |
| 3 | evasion | INITIAL_PLUS_TWO | node-budget | 999996 | 227 |
| 3 | middlegame | INITIAL_PLUS_TWO | node-budget | 999965 | 200 |
| 4 | tactical | INITIAL_PLUS_TWO | node-budget | 999959 | 74 |
| 4 | kiwipete | FORCED_ONE | absolute-ply-256 | 646082 | 252 |
| 4 | middlegame | FORCED_ONE | absolute-ply-256 | 42353 | 252 |
| 4 | kiwipete | INITIAL_PLUS_ONE | absolute-ply-256 | 685731 | 252 |
| 4 | evasion | INITIAL_PLUS_ONE | absolute-ply-256 | 7504 | 252 |
| 4 | middlegame | INITIAL_PLUS_ONE | absolute-ply-256 | 43328 | 252 |
| 4 | kiwipete | QSEARCH_BASELINE | node-budget | 999200 | 32 |

Corpus FORCED_ONE/I+1 each hit the budget on mate-five (max qply 116). I+2 hits
it on bishop-xray (112), three-capture-xray (218), mate-five (98),
tail-kiwipete-e1d1 (147), tail-evasion- (122), tail-evasion-f1f2 (148).
No primary G corpus root reaches ply 256, but the separately recorded PV suffix
validation does. Corpus maximum ordinary checks used is 55/55/107; longest
chain is 50 for all three. Benchmark maxima are 113/114/115 checks used and
50/42/54 chain length. Thus a one-reply rule limits response branching at each
admitted check, not the number of distinct checks, other tactical choices,
forced evasions or total path length. It does not guarantee shallow qsearch.

**Commands and reproducibility.**

```powershell
$env:DEBUG=''
.\gradlew.bat :app:test -Pheadless --tests '*QuiescenceForcingResearchTest' --tests '*ExactSearchHarnessTest'
.\gradlew.bat :app:test -Pheadless --tests 'com.ohinteractive.seedv6.search.exact.*' --tests 'com.ohinteractive.seedv6.tools.search.ExactSearchHarnessTest' --tests 'com.ohinteractive.seedv6.search.driver.*' --tests 'com.ohinteractive.seedv6.core.SeeThresholdTest' --tests 'com.ohinteractive.seedv6.core.SeeGeneratedLegalTest'
.\gradlew.bat :app:exactSearch '-PsearchArgs=--leaves=sr001g --tt=off --position=start,kiwipete,endgame,tactical,evasion,middlegame,transposition-pawns --depth=2 --warmups=12 --repetitions=7 --node-limit=1000000'
java -Xmx256m -cp 'app/build/classes/java/main;app/build/classes/java/test' com.ohinteractive.seedv6.search.exact.QuiescenceForcingCorpus
java -Xmx256m -cp 'app/build/classes/java/main;app/build/classes/java/test' com.ohinteractive.seedv6.search.exact.QuiescenceForcingCorpus --witnesses
java -Xmx256m -cp 'app/build/classes/java/main;app/build/classes/java/test' com.ohinteractive.seedv6.search.exact.QuiescenceForcingCorpus --resources
```

Repeat the primary harness at depth 3. At depth 4 use sr001g on start/endgame/
tactical/transposition-pawns and individual qcheck-forced-one,
qcheck-initial-plus-one, and both on kiwipete/evasion/middlegame. Run qcheck-initial
on all seven at depths 2/3/4 for the separate I reference. Diagnostics use identical
G candidate sets with `--quiet-check-diagnostics --warmups=0 --repetitions=1`.
Paired `sr001g-paired` uses start/endgame/pawns at depth 2, endgame/pawns at 3/4;
`sr001g-initial` uses start/endgame/tactical/pawns at 2/3 and start/endgame/pawns
at 4. Both paired groups use 128 warmups/15 repetitions. Run retained
QuiescenceCheckLimitCorpus and compare its 290 rows against F's CSV for the exact
freeze check. Every source run and its configuration is identified in BENCHMARK.csv.
Aggregates sum matched completed rows and compute 100*(candidate/reference-1);
max qply is the maximum on that same matched set.

Skipped deliberately: slow NNUE/training, unrelated GUI/browser/full suites,
strength/self-play, JVM/GC/cache profiling, higher depths, I+2 depth 4 on the three
failed depth-3 positions, repeated expensive unrestricted failures in headless
benchmarks, guard-rejected exhaustive trees, and all excluded mechanism combinations.
They would broaden the unit or exceed its bounded evidence needs. No budget increase,
Board/Gen tuning, qTT tuning or evaluation-margin policy was introduced.

**Questions returned to GPT.** Reply count supplies useful direct evidence for
mate continuations: G recovers the demonstrated second/third-check mates lost by
F count limits, and the first deeper mate lost by INITIAL_ONLY. Broad qply-0 checks
recover three useful initial resources that FORCED_ONE excludes, with a small
shallow incremental tree cost. I+1 nevertheless misses the demonstrated deeper
four-reply fork; I+2 recovers the new two-reply fork and a small exchange evaluation
change, but loses three-plus-reply continuations and produces far more incomplete
runs. Classification has a visible mechanical cost, and repeated one-reply checks
still generate pathological depth-4 lines. These are discriminating tactical and
workload observations, not a general error bound or evidence of playing strength.
The balance of value, mechanical cost and failures is returned for programme
reconciliation before any baseline/selectivity decision. Real-game frequency,
remaining witness suffixes, deeper-first draw coverage and eventual interaction
with separately authorized pruning remain unknown.

Changed files: ExactSearch, ExactSearchHarness, QuiescenceOracle,
ExactSearchHarnessTest; new QuiescenceForcingCorpus and
QuiescenceForcingResearchTest; this report and six CSVs; helper-owned VERSION_STATE
finalization. G remains uncommitted pending reconciliation with an empty index.
No reset, discard, stash, amend, rebase or push occurred. The runtime research
change qualifies for build finalization from 22; the verified final identity is
recorded in VERSION_STATE and the final completion rather than presumed here.
CODEXLOG_CURRENT.md is absent and was not created. Neither Search canon changed.

Human actions required after this prompt: None for completing this research unit.
GPT reconciliation is required before any policy acceptance/rejection, combination,
production adoption or canon maintenance.
