# SR-001D: exact qsearch transposition reuse

Research complete for GPT reconciliation; no acceptance, rejection, production
adoption or canon maintenance is implied. Unlimited SR-001A remains the semantic
reference. Exact reuse preserves all tested values and enables Kiwipete depth 4
to finish under the existing budget, but does not improve aggregate wall time.

**Authority and inherited boundary.** Repository masters remain Search Contract
R015 and Research Frontier F010. Their SHA-256 values remain respectively
`52dd6d2d8a24881f1ac73f3639b051f35ad9ca379f1eeb74c6242a32b84e6882` and
`3fd49cd19c49cfd223ea7c56d4547f7b3003d5e5d5ab2babd967630e39f64187`.
No repository/ancestor AGENTS.md was found; supplied shared governance applies.
Before mutation, the six modified tracked and five untracked SR-001C files,
empty index, build 19, report and CSVs were inspected. All ten non-version files
matched the finalized C receipt byte-for-byte; version state matched its verified
bump. No unrelated work was present. SR-001C was committed as
`c2ad4ef7b528dafe84d58d89da08c6c4975d9d46`. A/B commits remain
`666b6812f238a1a0ae42f896c7a19cabcce70654` and
`1df0e773eec2318704bc24288de6b871732f360e`.

**Implementation.** `ExactSearch.quiescenceResearch(evaluator, QSEARCH_QTT)` uses
primitive selector 16. Normal Search remains TT-off CONTROL ordered alpha-beta;
the qsearch move set, SEE/material ordering, stand-pat, all-evasion checked nodes,
draw precedence, actual-ply mate scores and absolute-ply-256 incomplete safety are
unchanged. CONTROL, QSEARCH_BASELINE, all B selectors and all C selectors remain
directly selectable. No production caller opts into qsearch or qTT.

One private `TTable` and one reusable `TEntry` belong to the research worker. Its
backing allocation is reusable, but entries are cleared before any node is entered
in each fixed-depth invocation, including consecutive depths inside a driver request. There is no
cross-invocation, iteration, request, game, parallel or shared evidence reuse.
The accepted default requests **64 MiB**, giving **2,097,152 slots / 48 MiB of
three primitive long arrays**, plus headers and the existing 32 padded locks.
`TTable.clear()` writes its 16 MiB data array. Construction is outside harness
timing; each invocation's clearing is inside timing. No sizing sweep or changes
to TTable mechanics, replacement policy, synchronization or normal Search TT.

The unchanged `SearchKey` fingerprints ordered reversible repetition history,
resets after captures/pawn moves, and includes complete board status (including
rule clock/fullmove/raw-EP state). The fixed evaluator owns each invocation.
History fingerprints now also travel through normal TT-off ancestors and qmoves
when this separate table is selected. Depth **0**, generation **0** are caller
conventions solely in this isolated unlimited-qsearch table; neither represents
normal Search depth-zero equivalence.

Cancellation and mate/stalemate/rule-draw adjudication precede probing. EXACT
returns immediately; LOWER returns only at score >= beta; UPPER only at score <=
alpha. Other bounds do not tighten either window edge or affect ordering.
Completed nonterminal nodes save UPPER at score <= original alpha, LOWER at
score >= original beta, otherwise EXACT. Actual root/path ply normalizes mate
scores through `TranspositionScores`. Terminal nodes need no cache evidence and
are not stored. A completion checkpoint prevents incomplete nodes from saving;
previously completed descendants may retain their valid entries until the next
invocation clears them.

Natural best-move metadata may be written, but **no qTT hash move is consumed**.
An immediate cache return exposes an empty suffix at that node: ancestors keep
their searched legal prefix and the line ends at the reuse point. A cold root
still establishes its own best move/stand-pat. This follows the existing result
contract allowing TT-truncated PVs, without reconstructing an unsearched line.
There is no qTT ordering, SEE pruning, horizon, delta/futility, quiet checks,
PVS inside qsearch, quiet-history update or evaluator-specific policy.

**Correctness and corpus.** Eleven focused qTT tests cover original-window bound
classification/equality, fail-soft stand-pat/tactical cutoffs, exact/lower/upper
reuse, non-cutting poisoned bounds leaving the complete child visitation sequence
unchanged, mate normalization at actual plies 11 and 23 independently of qply,
terminal/draw-before-poisoned-cache precedence, cancellation by final evaluation,
node budget, thread interruption, checked/non-check absolute-ply-256 abort,
cold per-depth lifecycle, evaluator/history state, and natural transpositions.
Harness tests exercise explicit selection, counters, illegal combinations and
budget interruption. Existing A/B/C tests remain intact in meaning.

The established **42 fixtures** are compared in four windows each: full,
exact value +/- 1, fail-high and fail-low. On the 36 compact fixtures the existing
independent exhaustive oracle supplies the value; six longer tails use preserved
unlimited alpha-beta. All **168 comparisons** satisfy the exact/bound relationship,
with zero returned-score differences in this sample (cutoff scores are not
required to be globally exact). All 42 full-window best moves match. Every
reported full-window qPV prefix is checked for legal admitted moves and value
consistency by independent unlimited re-search, including actual-ply mate offset.
Narrow-window prefixes are checked for legality; their bounds are checked against
the full value. No unresolved divergence exists.

Full-window corpus qnodes fall **43,217 -> 20,658 (52.2%)**; this corpus reduction
is dominated by the intentionally difficult `tail-kiwipete-e1d1` (35,898 -> 15,174),
and should not be substituted for the broader benchmark result. Two full-window
PVs shorten at cache reuse: `tail-kiwipete-e1d1` (11 -> 6 moves) and
`tail-evasion-f3d4` (9 -> 5). Their legal retained prefixes realize the reference
scores. Mate, draw, promotion, underpromotion and the earlier SEE-clearance and
depth-truncation counterexamples retain the unlimited values.

Controlled transposition: `4k3/8/2p1p3/3q4/2P1P3/3Q4/8/4K3 w - - 0 1`.
Both admitted qlines
`d3d5 c6d5 c4d5 e6d5 e4d5` and
`d3d5 c6d5 e4d5 e6d5 c4d5`
reach `4k3/8/8/3P4/8/8/8/4K3 b - - 0 3` with identical conservative keys.
The complete root search keeps score 1072 and reduces **31 -> 28 qnodes**, with
seven hits, three EXACT returns and two LOWER cutoffs. The primitive table does
not need relaxed history identity for this reuse.

History counterexample: from `r6k/8/8/8/8/8/8/1K6 b - - 1 1`, follow seven moves
of `a8b8 b1a1 b8a8 a1b1`. With a constant static score 37, fresh history yields
-37 while the actual path reaches repetition during a checked qline and yields 0.
An existing proof for the same board with fresh history is not reused. Similarly,
a checked position with halfmove clock 0 versus 99 yields -37 versus 0, with no
invalid hit. Commuting quiet knight sequences from the initial position produce
identical board/status but distinct history-sensitive keys, as required.

**Data and benchmark method.** Detailed durable rows:

- [63 benchmark rows](SEARCH_QUIESCENCE_QTT_SR001D_2026-09-28_BENCHMARK.csv): FEN,
  requested/completed depth, status, best, score, PV, normal/q/total nodes, max
  qply, median time, NPS and run conditions for all three modes at depths 2/3/4.
- [21 qTT diagnostic rows](SEARCH_QUIESCENCE_QTT_SR001D_2026-09-28_DIAGNOSTICS.csv).
- [168 corpus/window rows](SEARCH_QUIESCENCE_QTT_SR001D_2026-09-28_CORPUS.csv).
- [Eight identity audit rows](SEARCH_QUIESCENCE_QTT_SR001D_2026-09-28_IDENTITY.csv).

Same seven positions and HCE/TT-off conditions as A/B/C; one-million total entered
nodes per invocation, no raised budget. Clean timing uses 12 warmups, seven
measured repetitions, rotating mode order, upper median per position, JVM
`-Xms256m -Xmx256m -Xbatch`, Java 21 on Windows/amd64. Semantic/PV checks occur
outside timing. Counters are disabled in all timing rows. Separate diagnostic
runs enable them with zero warmups/one repetition; all 21 diagnostic trees match
their clean rows for completion, score, best/PV and every node/ply count.
An initial depth-4 run overlapped compilation/corpus work and was discarded for
timing; the retained depth-4 run was repeated without that concurrent workload.

All 42 CONTROL and unlimited-baseline tree records, including incomplete Kiwipete,
match C exactly for completion, score, best/PV, normal/q/total nodes and max qply.
Of 20 mutually completed baseline/qTT pairs, all scores and best moves match.
The sole unmatched pair is Kiwipete depth 4 (baseline incomplete, qTT complete).

| Normal depth / matched positions | Baseline qnodes -> qTT | Qnode reduction | Total-node reduction | Sum median ms: baseline -> qTT | Wall change | Aggregate NPS change | Max qply |
|---|---:|---:|---:|---:|---:|---:|---:|
| 2 / 7 | 51,464 -> 40,985 | 20.36% | 20.29% | 27.535 -> 40.176 | +45.91% | -45.37% | 28 -> 28 |
| 3 / 7 | 202,368 -> 162,122 | 19.89% | 19.80% | 107.790 -> 131.776 | +22.25% | -34.40% | 27 -> 27 |
| 4 / 6, excluding Kiwipete | 264,341 -> 199,830 | 24.40% | 23.90% | 148.456 -> 161.844 | +9.02% | -30.20% | 26 -> 26 |

Times are sums of rounded position medians, not a median of aggregate runs.
Aggregate NPS is summed nodes divided by summed time, not the mean of row NPS.
At every matched depth: **zero score differences, zero best-move differences**.

All-position work totals below include the incomplete baseline attempt at depth 4;
that row is workload evidence, not a matched completed-value comparison.

| Depth | Mode | Completed / 7 | Normal nodes | Qnodes | Total nodes | Sum median ms | Max qply |
|---|---|---:|---:|---:|---:|---:|---:|
| 2 | CONTROL | 7 | 793 | 0 | 793 | 0.627 | 0 |
| 2 | BASELINE | 7 | 174 | 51,464 | 51,638 | 27.535 | 28 |
| 2 | QTT | 7 | 174 | 40,985 | 41,159 | 40.176 | 28 |
| 3 | CONTROL | 7 | 7,938 | 0 | 7,938 | 5.084 | 0 |
| 3 | BASELINE | 7 | 849 | 202,368 | 203,217 | 107.790 | 27 |
| 3 | QTT | 7 | 849 | 162,122 | 162,971 | 131.776 | 27 |
| 4 | CONTROL | 7 | 59,785 | 0 | 59,785 | 35.789 | 0 |
| 4 | BASELINE, budget-limited Kiwipete | 6 | 6,376 | 1,263,541 | 1,269,917 | 622.961 | 32 |
| 4 | QTT | 7 | 8,070 | 1,129,348 | 1,137,418 | 829.279 | 32 |

**Kiwipete depth 4:** unlimited baseline interrupts at exactly **1,000,000 nodes**
(800 normal / 999,200 qnodes), observed qply 32, score INVALID, no best/PV,
474.505 ms. qTT **completes** at **932,012 nodes** (2,494 normal / 929,518 qnodes),
qply 32, score **32**, best **e2a6**, legal verified prefix
`e2a6 e6d5 c3d5 e7e5`, 667.435 ms, 1,396,407 NPS. This establishes a completion
change, not a measured speedup or root-score comparison against an unavailable
completed baseline. No force-completion run or larger budget was attempted.

**Reuse diagnostics.** Probes follow adjudication; hits require the full SearchKey.
The EXACT column counts applicable EXACT resolutions; LOWER/UPPER count actual
cutoffs. Non-cutting hits do not count as resolutions. Stores count save attempts:
accepted/rejected writes and replacements are not cheaply exposed by the accepted
void TTable API, so they were not measured and no mechanics were changed.

| Matched set | Probes | Hits | Hit rate | EXACT | LOWER | UPPER | Save attempts |
|---|---:|---:|---:|---:|---:|---:|---:|
| Depth 2, seven | 40,936 | 1,864 | 4.55% | 94 | 1,122 | 585 | 39,135 |
| Depth 3, seven | 161,913 | 7,709 | 4.76% | 330 | 4,888 | 2,058 | 154,637 |
| Depth 4, six | 199,643 | 15,459 | 7.74% | 1,048 | 8,939 | 4,373 | 185,283 |
| Kiwipete depth 4, completed qTT only | 928,194 | 65,417 | 7.05% | 3,577 | 37,930 | 17,246 | 869,441 |

Resolved-node counts are 1,801 / 7,276 / 14,360 for the matched sets and 58,753
for Kiwipete depth 4. A hit itself remains an entered qnode. Net avoided visits
versus the baseline are 10,479 / 40,246 / 64,511 qnodes on matched sets; these
are whole-run differences, not a sum of imagined subtree sizes per hit.

The separate allocation-heavy **test-only identity audit** traces every entered
baseline qposition at depth 2, including terminals: 51,464 visits, 39,153 distinct
board keys, 39,671 distinct board-plus-rule states, and 39,768 distinct SearchKeys
(summed per invocation). Apparent duplicates fall from 12,311 to 11,793 with rule
state, then to 11,696 with conservative ordered history. Thus the history component
removes **97 / 11,793 = 0.82%** of same-board/rule duplicate opportunities in this
sample; rule state plus history remove 615 / 12,311 = 5.00% of board-only ones.
This is evidence that conservative identity is not the principal measured limiter,
not authority to relax it. Counts include terminal/no-children repetitions and do
not predict cache hit rate or prunable subtree size; applicability, windows,
replacement and earlier cache cutoffs also affect actual reuse.

**Interpretation and limits.** Duplicated work is measurable, but most visited work
remains: qTT retains roughly 76-80% of baseline qnodes on matched benchmarks and
does not reduce their maximum qply. It is not a tail-depth control. Kiwipete still
uses 929,518 qnodes at qply 32; no natural absolute-ply-256 failure occurred.
Evasion depth 4 alone improves wall time (74.076 -> 71.754 ms, about 3.1%); this
does not offset the other cases. Small trees pay substantial fixed overhead:
transposition-pawns depth 2 has 25 qnodes and zero hits but 0.010 -> 0.884 ms.

Source inspection shows no added per-node objects: reusable primitive history,
entry scratch and existing table arrays/locks. A six-long diagnostic array exists
only when enabled; snapshot cloning occurs outside recursion. The extra 48 MiB
allocation per research worker is excluded from timing, while clearing, hashing,
probing and storing are included. The higher wall times/lower throughput are
consistent with added work and memory traffic, especially cold-table clearing,
but this experiment does **not** isolate GC, allocation, lock, JIT or hardware-cache
costs. There is no allocation/cache profiler evidence and no causal percentage
attributed to any one cost. Requested capacity follows the user's fixed experiment;
collision/replacement losses are unquantified, not grounds for a sizing sweep.

No general fraction of all qsearch explosion can be assigned to transpositions
from these samples. Exact reuse removes some duplication without tactical loss
here, but leaves large distinct trees and does not demonstrate practical aggregate
speed improvement under this lifecycle/substrate. Whether later integration,
different authorized lifecycle/mechanics work or separate selectivity is warranted
remains for GPT; no candidate is accepted/rejected and no combination is tested.
There is no playing-strength inference.

**Validation and reproduction.** DEBUG was cleared for Gradle runs.

```powershell
$env:DEBUG=''
.\gradlew.bat :app:test -Pheadless --tests '*QuiescenceTtResearchTest' --tests '*ExactSearchHarnessTest'
.\gradlew.bat :app:test -Pheadless --tests 'com.ohinteractive.seedv6.search.exact.*' --tests 'com.ohinteractive.seedv6.tools.search.ExactSearchHarnessTest' --tests 'com.ohinteractive.seedv6.search.driver.*' --tests 'com.ohinteractive.seedv6.core.SeeThresholdTest' --tests 'com.ohinteractive.seedv6.core.SeeGeneratedLegalTest' --tests 'com.ohinteractive.seedv6.search.tt.TTableTest' --tests 'com.ohinteractive.seedv6.search.tt.TranspositionScoresTest'
.\gradlew.bat :app:exactSearch '-PsearchArgs=--leaves=sr001d --tt=off --position=start,kiwipete,endgame,tactical,evasion,middlegame,transposition-pawns --depth=2 --warmups=12 --repetitions=7 --node-limit=1000000'
# Repeat the same benchmark at --depth=3 and --depth=4; both were run.
.\gradlew.bat :app:exactSearch '-PsearchArgs=--leaves=qtt --qtt-diagnostics --tt=off --position=start,kiwipete,endgame,tactical,evasion,middlegame,transposition-pawns --depth=2 --warmups=0 --repetitions=1 --node-limit=1000000'
# Repeat diagnostics at --depth=3 and --depth=4; both were run.
java -Xmx256m -cp 'app/build/classes/java/main;app/build/classes/java/test' com.ohinteractive.seedv6.search.exact.QuiescenceTtCorpus
java -Xmx256m -cp 'app/build/classes/java/main;app/build/classes/java/test' com.ohinteractive.seedv6.search.exact.QuiescenceTtCorpus --identity
git diff --check
```

Focused initial run: **31 tests / two suites passed**. Search-scoped regressions:
**247 tests / 27 suites, zero failures/errors/skips, 44 seconds**. After adding
explicit assertions for the two convergent legal paths and checked ply-256
interruption, the affected **31 tests / two suites passed again** in five seconds.
All corpus, diagnostic parity, baseline-record parity and legal/value PV checks
passed. Both canons' hashes and unchanged TTable/SearchKey sources were checked.
No required validation was unavailable.

Deliberately skipped: unrelated full-suite validation, slow NNUE/training suites,
GUI/browser tests (no dependency), strength/self-play matches, depth beyond 4,
larger budgets, table sizing/replacement experiments, and JVM/GC/cache profiling.
Results are shallow HCE/TT-off research, not neural-evaluator or production
TT/PVS/time-management evidence.

Changed files: ExactSearch.java, ExactSearchHarness.java, ExactSearchHarnessTest.java;
new QuiescenceTtResearchTest.java, QuiescenceTtCorpus.java, this report and four CSVs.
VERSION_STATE.txt is finalized separately through the verified maintained helper:
begin at build 19, runtime-impact decision **bump**, verified outcome in the root
version state and completion. SR-001D remains uncommitted pending reconciliation;
nothing staged or pushed, no unrelated work absorbed. Root journal is absent.

Human actions required after this prompt: **None to complete SR-001D research.**
GPT reconciliation remains blocking for later policy acceptance/rejection,
production integration or canon updates. Neither Search canon was modified.
