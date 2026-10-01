# SR-001H — qsearch delta evidence calibration

2026-09-28. Evidence only; no pruning policy accepted, rejected, or implemented.
SR-001 remains pending. CONTROL and every A–G selector remain independently
selectable. No production Java source, Search move membership, score mapping,
Search Contract, or Research Frontier changed.

## Boundary and implementation

Repository masters remain **R015/F010**. Their SHA-256 values are respectively
`52dd6d2d8a24881f1ac73f3639b051f35ad9ca379f1eeb74c6242a32b84e6882` and
`3fd49cd19c49cfd223ea7c56d4547f7b3003d5e5d5ab2babd967630e39f64187`.
G's five modified and nine untracked paths, empty index, and build 23 matched
its finalized receipt `da346d67aaf44071ae87d18bd9d14650`, including all inherited
non-version file hashes. G was preserved in focused commit
**`4c6f5e7258226548ae2619cb1dfb15208f6b6995`** before H implementation.

Following the existing SR-003/SR-017 diagnostic convention,
`archive/search-programme/tools/search-delta-diagnostics.py` applies nine uniquely checked substitutions
to a copy of ExactSearch under `build/sr001h-instrumented/`, compiles it, and
places it first on the diagnostic JVM classpath. Production source/classes stay
untouched. Integration-point drift fails closed. A package-private research
factory attaches the recorder only to unlimited, TT-off QSEARCH_BASELINE.
There is **no delta decision in either the production or diagnostic search**.

The test-only recorder preallocates a 300,000-row flat primitive buffer (about
48.1 MiB), plus per-ply primitive bookkeeping. Overflow fails the diagnostic
rather than sampling or returning a chess score. There is no recurring recorder
allocation in recursion. Extra child check detection, board snapshots and
recording are untimed diagnostic work. CSV formatting/compression happens after
Search. Stored SEE sign comes from the already computed SR-015 ordering key;
no second SEE implementation or numerical SEE policy is introduced.

Each record includes the run/node/observation identity, current FEN, actual ply,
qply, window at visitation, original stand-pat, move/victim/gain, SEE sign, EP,
previous qmove, move score and bound, alpha/cutoff/transient-best/final-best
flags, selected returned-line terminal cause, final node score, and separate
move/node/invocation completion flags. The recapture flag means a same-square
capture following another **qsearch capture**; it excludes quiet predecessors
and does not inspect the preceding normal-Search move. FEN alone does not encode
history: the run and observation identify the actual history-bearing execution.
Terminal-cause flags describe returned-line evidence, not unique necessity.

## Score and material domains

The [domain CSV](SEARCH_QUIESCENCE_DELTA_SR001H_2026-09-28_DOMAINS.csv) records the
inspection. HCE returns a side-to-move integer directly to Search, with its own
static range of ±30,000. ExactEvaluator admits non-mate scores through ±32,511;
the mate band starts at ±32,512, with base mate 32,768 and actual-path-ply
normalization. No centipawn conversion intervenes.

| Piece | Ordering/SEE exchange value | HCE phase-dependent material range |
|---|---:|---:|
| Pawn | 100 | 100–120 |
| Knight | 320 | 300–325 |
| Bishop | 330 | 330–350 |
| Rook | 500 | 550–600 |
| Queen | 975 | 900–975 |

Ordering uses captured exchange material plus promotion gain; EP's empty packed
victim is treated as a pawn. SEE uses the same exchange values, but represents
an optional-stop exchange calculation, not a general tactical/evaluation bound.
HCE adds phase, piece-square, mobility, passed-pawn, king-safety and other terms.
Its passed-pawn calculation alone includes rank, king-distance and promotion-race
terms. Capturing material can also change the phase/material values of survivors.

**Evaluator assessment:** (1) These quantities are approximately commensurate
for the tested HCE expression, so the comparison is meaningful; they are not an
upper-bound contract. (2) General evaluator compatibility is **unproven**.
ExactEvaluator specifies perspective/range, not calibration. NNUE V1 explicitly
uses uncalibrated bounded tanh output scaled by 32,511; BRN-0/1/2 similarly map
bounded values to the full non-mate range. Both mappings explicitly disavow
centipawn calibration. (3) A fixed numeric delta margin would encode evaluator
calibration assumptions today. (4) No established evaluator-independent numeric
upper bound expresses this delta evidence. Binary SEE supplies evaluator-independent
exchange classification, but is not such a bound and B already exposed its limits.
No conversion, remapping or evaluator-specific Search branch was added.

## Method and coverage

Eligibility is exactly: a non-check qnode, a legal capture (including EP), no
promotion, and no check in the made child. Checked evasions, promotions and
checking captures are outside the prospective class. They are still searched
normally and may occur later in a recorded continuation. Ordinary quiet checks
are absent. No SEE pruning, qdepth, qTT, delta/futility or other selectivity
participates.

The established 60 A–G fixtures were retained. One compact legal fixture was
added: `4k3/8/8/8/8/8/3r4/4K3 w - - 0 1`, where non-checking `Kxd2`
changes a losing HCE position into insufficient-material draw. Each fixture ran
full-window plus windows `[V-1,V)` and `[V,V+1)` around its completed baseline
value. The seven existing HCE/TT-off headless positions ran at normal depths 2/3,
under the unchanged one-million-total-node budget. Every diagnostic invocation
also ran recorder-off and required identical completion, score, best move, PV,
normal/q/total nodes and maximum qply. Independently, production-class harness
CONTROL/baseline runs were compared with G's retained records.

**197 completed diagnostic invocations; 195,921 eligible observations:** 164,319
from 14 benchmark runs, 26,109 from full-window corpus runs, and 5,493 from narrow
corpus runs. Revisited/transposed positions and overlapping fixtures remain
observations, not independent statistical samples. The tables split benchmark,
full-corpus and narrow-corpus populations. All 60 retained corpus baselines and
all 28 production CONTROL/baseline benchmark rows match G exactly. No benchmark
or corpus invocation hit the node budget or ply-256 safety; maximum observed
qply was 31 across the corpus and 28 across these benchmarks.

## Gap distribution and counterfactual screen

`rawDeltaBound = standPat + gain`; `alphaGap = alphaBeforeMove - rawDeltaBound`.
The task's explicit gap interpretation uses **gap > margin**. Its conceptual
`raw + margin <= alpha` instead includes equality. Both are retained separately;
no equality policy is silently chosen. Under the strict screen, a positive gap
G requires margin at least G to retain that observation; the inclusive integer
screen requires G+1. Percentiles below use nearest rank.

| Population | Count | Min | Median | p90 | p95 | p99 | Max |
|---|---:|---:|---:|---:|---:|---:|---:|
| All alpha-raising moves: signed gap | 50,121 | -975 | -279 | 5 | 71 | 229 | 520 |
| Alpha-raising moves with positive gap | 5,233 | 1 | 67 | 225 | 269 | 354 | 520 |
| Benchmark-only positive-gap alpha raises | 4,308 | 1 | 66 | 219 | 267 | 361 | 520 |

Positive-gap alpha raises include 972 same-target q-recaptures, 89 SEE-negative
moves and one EP. Victims: 3,758 pawns, 530 rooks, 412 bishops, 357 knights and
176 queens. Gaps exceed 100/200/300/400/500 in 1,913/754/121/11/4 observations.
The larger margins below are the positive-gap median, p95, p99 and maximum,
derived from this sample rather than conventional engine constants.

| Strict margin | Predicted pruned | Eligible % | Alpha raises lost | Beta cutoffs lost | Final-best fail-low bounds affected | Draw-line observations exposed |
|---:|---:|---:|---:|---:|---:|---:|
| 0 | 113,778 | 58.0734 | 5,233 | 4,200 | 32,010 | 7 |
| 67 | 98,693 | 50.3739 | 2,598 | 2,093 | 27,301 | 6 |
| 269 | 59,900 | 30.5735 | 253 | 157 | 15,081 | 4 |
| 354 | 50,407 | 25.7282 | 52 | 31 | 12,472 | 0 |
| 520 | 34,028 | 17.3682 | 0 | 0 | 7,949 | 0 |

These are counterfactual **visited-move** counts, not measured savings or counts
of changed root decisions. Pruned subtrees overlap; changed bounds/windows would
change later visitation. No node-saving estimate is asserted. Alpha raises and
cutoffs are direct local counterexamples. A changed fail-low upper-bound value
is separately reported and does **not** by itself prove a wrong root value.
The broader 89,616 alpha-raising/final-best observations have signed-gap
min/median/p90/p95/p99/max -975/-68/484/695/1022/31817; their large tail includes
fail-low visits under mate-valued windows, not evidence for a 31,817-unit margin.

Equality matters: the inclusive margin-0 screen predicts 114,009 prunes, 5,292
lost alpha raises and 4,247 lost cutoffs. Inclusive margin 520 still loses four
alpha raises/cutoffs. Complete strict/inclusive group tables are in
[MARGINS](SEARCH_QUIESCENCE_DELTA_SR001H_2026-09-28_MARGINS.csv), with distributions
in [DISTRIBUTIONS](SEARCH_QUIESCENCE_DELTA_SR001H_2026-09-28_DISTRIBUTIONS.csv).
Even strict 520 is only the largest visited alpha gap in this sample, **not a
general safe margin**.

## Interpretable witnesses and oracle evidence

The compact [witness values](SEARCH_QUIESCENCE_DELTA_SR001H_2026-09-28_WITNESS_VALUES.csv)
include FENs, source observation IDs, full-window PVs, exact values where bounded,
and HCE component changes. The [witness index](SEARCH_QUIESCENCE_DELTA_SR001H_2026-09-28_WITNESSES.csv)
contains 3,832 distinct board/window/move/outcome records with occurrence counts.

| Witness | qply / move | Stand + gain | Alpha | Gap | Searched score | Interpretation |
|---|---|---:|---:|---:|---:|---|
| Kiwipete d2 | 17 / h3g2 | -735 + 100 | -115 | 520 | -56 | Non-checking pawn advance/capture; immediate HCE gain 679 includes passed-pawn change 540. Exact move and node value -56. |
| Kiwipete d3 | 12 / e7e6 | 48 + 100 | 590 | 442 | 607 | Removes an advanced pawn; immediate gain 559, passed-pawn change 350. Exact move value 607, but another move gives true node best 672: locally useful is not necessarily globally best. |
| EP winning | 0 / e5d6 | 29 + 100 | 433 | 304 | 434 | EP produces a passed pawn; immediate gain 405, passed-pawn component 288; uniquely best. |
| EP defended suffix | 1 / d4d6 | 285 + 100 | 681 | 296 | 734 | Recapture removes that advanced pawn; gain 449 includes passed-pawn change 315; uniquely best. |
| Three-capture x-ray suffix | 2 / a1a5 | 275 + 100 | 706 | 331 | 707 | Final recapture in an exchange; passed-pawn removal contributes 300 beyond its material component. |
| Non-checking clearance | 0 / a4c5 | -691 + 100 | -411 | 180 | -410 | SEE-negative move opens the rook's a-file attack on the bishop; immediate HCE gain 281 includes passed-pawn removal 150. Retains B's meaningful clearance resource. |
| Promotion/stalemate fixture suffix | 2 / b8d8 | -673 + 500 | 0 | 173 | 20 | This is an ordinary rook capture after an earlier promotion; protecting the promotion move does not protect its subsequent continuation. |
| New dead-draw fixture | 0 / e1d2 | -781 + 500 | -1 | 280 | 0 | Unique insufficient-material draw; static child would be 7, but terminal adjudication returns 0. |
| Draw-five suffix | 4 / b5a5 | -605 + 330 | -232 | 43 | 0 | Unique draw by insufficient material; static child would be -230. Draw semantics exceed an evaluation-only estimate. |

The candidate-aware exhaustive oracle is unchanged in value/admission behavior;
an optional test-only completion observer records fully enumerated child values.
**55/61 fixtures matched**, with 1,493 exhaustive nodes and 612 eligible move
occurrences: 320 uniquely best, 14 equal-best and 278 below the true node best.
Twenty-two occurrences were uniquely draw-preserving; none was uniquely required
for mate. Of the 334 best/equal-best occurrences, 205 uniquely best captures had
value above raw material evidence. Clamped nonnegative excess over raw evidence
for best/equal-best moves had median/p90/p95/p99/max **90/288/328/531/533**.

The six retained large-tail fixtures hit the existing 200,000-node exhaustive
guard: tail-kiwipete-e1d1, tail-kiwipete-f3h3, tail-evasion-, tail-evasion-f1f2,
tail-evasion-f3d4 and tail-middlegame-d3d4. Their partial oracle records were
discarded; completed alpha-beta baseline results remain independently available.
No guard was raised or converted into a chess score.

[ORACLE_LINKS](SEARCH_QUIESCENCE_DELTA_SR001H_2026-09-28_ORACLE_LINKS.csv) joins 278
visited corpus observations to exact enumerated continuations; all satisfy the
appropriate fail-soft bound relationship. No ambiguous-value matches were used.
Forty-two positive-gap observations truly exceed visited alpha (41 uniquely best,
one equal-best). Strict margins 67/269/354/520 expose 40/10/0/0 such joined
counterexamples, respectively. Separately, 11/12 selected diagnostic states were
fully enumerated; one Kiwipete c3b2 state hit the guard. The maximum-gap h3g2
witness has exact excess **579** over raw evidence, larger than the observed
520 alpha gap. This alone illustrates why a zero-error visited-margin screen is
not a bound across other windows.

Seven positive-gap returned draw-line observations were seen, four raising alpha;
all involved insufficient material. Seventy-six positive-gap returned mate-line
observations were losing fail-lows, improving neither alpha nor local best:
they are not evidence of a lost mating resource. No required mating resource,
stalemate, formal repetition or rule-50 false-prune witness was established in
this restricted class. Their absence in this bounded sample is a coverage limit,
not permission to disregard those outcomes. Protected promotions/checking
captures and complete evasion semantics remain essential context.

## Headless diagnostic coverage

Full scores, best moves, PVs, requested/completed depth, normal/q/total nodes,
qply and explicitly diagnostic timing/NPS are in
[RUNS](SEARCH_QUIESCENCE_DELTA_SR001H_2026-09-28_RUNS.csv).
[IDENTITY](SEARCH_QUIESCENCE_DELTA_SR001H_2026-09-28_IDENTITY.csv) retains all 28
production-class comparisons and G's clean timing context (12 warmups/7 repeats).

| Position | d2 qnodes / qply / observations | d3 qnodes / qply / observations |
|---|---:|---:|
| start | 98 / 2 / 5 | 978 / 6 / 56 |
| kiwipete | 18,871 / 23 / 11,940 | 108,106 / 26 / 68,481 |
| endgame | 100 / 4 / 26 | 352 / 4 / 53 |
| tactical | 87 / 3 / 31 | 444 / 5 / 30 |
| evasion | 18,326 / 28 / 12,530 | 30,656 / 27 / 20,356 |
| middlegame | 13,957 / 19 / 8,413 | 61,721 / 24 / 42,398 |
| transposition-pawns | 25 / 0 / 0 | 111 / 0 / 0 |
| Total | **51,464 / max 28 / 32,945** | **202,368 / max 27 / 131,374** |

Normal/total nodes: **174/51,638** at d2; **849/203,217** at d3. All completed.
G's retained sums of clean position medians are 27.351 ms and 106.933 ms; H makes
no performance comparison from instrumented timings. Raw evidence is losslessly
compressed CSV, about 4.96 MB, in
[OBSERVATIONS](SEARCH_QUIESCENCE_DELTA_SR001H_2026-09-28_OBSERVATIONS.csv.gz).

## Validation, reproduction and disposition

**273 existing Search-scoped tests passed**, zero failures/errors. The ordinary
run discovers five shadow-only H tests and skips them there; all **five H tests
passed separately with instrumentation enabled**, zero skips/failures/errors.
They cover complete-corpus identity and repeatability, exact eligibility/material/
SEE recording, stand-pat and narrow windows, final-node evidence, EP, negative
SEE, draw/repetition/rule-50 precedence, actual-path mate distance, ply-256
incomplete safety, node budgets, interruption, evaluator-triggered cancellation,
and exhaustive-oracle observer identity. Analysis asserts arithmetic, completed
records, retained results, and exact/upper/lower oracle relationships.

Commands (PowerShell, repository root; set `$env:DEBUG=''` for Gradle):

```text
.\gradlew.bat :app:compileTestJava -Pheadless
python tools/search-delta-diagnostics.py --test
python tools/search-delta-diagnostics.py .
.\gradlew.bat :app:exactSearch -Pheadless '-PsearchArgs=--leaves=both --tt=off --position=start,kiwipete,endgame,tactical,evasion,middlegame,transposition-pawns --depth=2 --warmups=0 --repetitions=1 --node-limit=1000000' > app/build/sr001h-clean-depth2.txt
.\gradlew.bat :app:exactSearch -Pheadless '-PsearchArgs=--leaves=both --tt=off --position=start,kiwipete,endgame,tactical,evasion,middlegame,transposition-pawns --depth=3 --warmups=0 --repetitions=1 --node-limit=1000000' > app/build/sr001h-clean-depth3.txt
python tools/analyze-search-delta.py
java -Xmx256m -cp 'app/build/classes/java/main;app/build/classes/java/test;app/build/resources/main' com.ohinteractive.seedv6.search.exact.QuiescenceDeltaCorpus --witnesses app/build/sr001h-selected.tsv
.\gradlew.bat :app:test -Pheadless --tests 'com.ohinteractive.seedv6.search.exact.*' --tests 'com.ohinteractive.seedv6.tools.search.ExactSearchHarnessTest' --tests 'com.ohinteractive.seedv6.search.driver.*' --tests 'com.ohinteractive.seedv6.core.SeeThresholdTest' --tests 'com.ohinteractive.seedv6.core.SeeGeneratedLegalTest'
```

The diagnostic launcher uses Java 21, `-Xms256m -Xmx256m -Xbatch`; no GUI. The
analysis script expects the shown Windows PowerShell UTF-16 clean-harness logs.
It reproduces distributions, margin screens, witness selection, oracle joins,
domain inspection and retained identity checks. Witness evaluation is a separate
production-class invocation and labels guard-rejected results explicitly.

Deliberately skipped: depth 4 and known expensive tails solely for more samples;
slow NNUE/training; neural empirical calibration (the current fixed-depth surface
is HCE, and inspection already establishes the mapping gap); unrelated GUI/browser/
full-suite tests; strength/self-play; guard-bypassing exhaustive enumeration;
JVM/GC profiling; and actual pruning/performance trials. None is required to
validate unchanged baseline execution in this evidence unit.

Remaining unknowns: generalization beyond these fixtures/windows, unbiased
real-game frequency, evaluator-wide score calibration, relevant mate/stalemate/
repetition/rule-50 examples in this eligibility class, and actual savings/root
errors after a pruning policy changes visitation. The evidence supports a useful
HCE workload estimate and interpretable failure classes, but establishes neither
a universal material bound nor an evaluator-independent safe numeric margin.
No safeguard or margin is adopted. GPT must decide whether to authorize a
separately measured candidate family or redirect research.

Files: one modified test file (`QuiescenceOracle.java`); new test-only
`QuiescenceDeltaEvidence.java`, `QuiescenceDeltaEvidenceTest.java`, and
`QuiescenceDeltaCorpus.java`; the two diagnostic/analysis tools; this report and
ten CSV/CSV.gz datasets. H remains uncommitted, with an empty index. No production
source changed, no push/history rewrite occurred, and both Search canons remain
untouched. Version impact is **no_bump**: diagnostic/test/report work only; the
inherited application identity is build 23. The maintained finalizer's verified
finish and final Git audit are reported in the completion. CODEXLOG_CURRENT.md
is absent and was not created.

Human actions required after this prompt: None for completing this evidence unit.
GPT reconciliation is required before implementing a delta candidate, adopting
any qsearch policy or performing canon maintenance.
