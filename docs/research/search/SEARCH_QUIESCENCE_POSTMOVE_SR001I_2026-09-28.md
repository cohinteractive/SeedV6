# SR-001I — post-move static-evaluation evidence

Evidence unit complete; no futility policy implemented or adopted. Repository
masters remain Search Contract **R015** / Research Frontier **F010**, unchanged.
The inherited H worktree matched its completed no-bump receipt (one modified
tracked file, 16 untracked, empty index). It was preserved as
**`79ac6d26e0f91823f1f1f91bcaa10d831166ea1b`** before I implementation.
Build remains **23**; I contains diagnostic/test/report work only.

## Finding and its semantic scope

For the stipulated **nonterminal, non-check child**, the qsearch abstraction
offers the opponent stand-pat. Consequently:

```text
Q(child) >= Eval(child)
V(parent move) = -Q(child) <= -Eval(child) = postMoveStatic
```

This is a bound within the existing stand-pat qsearch abstraction, not a claim
about legal-pass chess minimax or evaluator accuracy. Immediate terminals must
be excluded first; checked children do not have this stand-pat option.

For the actual child window `[-beta, -alpha]`, `postMoveStatic <= alpha` means
`Eval(child) >= childBeta`. The existing child stand-pat cutoff already resolves
that invocation, fail-soft, without searching descendants. **All 102,120
equality-inclusive zero-margin predictions were such one-node child cutoffs.**
Strict zero accounts for 101,671 of them. No observed eligible continuation
exceeded postMoveStatic, and complete enumeration independently supports this.
The evidence identifies an existing bound/cutoff, not a new measured saving.

A naive move skip is not established as equivalent: a fail-low move may improve
the node's local best and its reported score/PV while remaining below incoming
alpha. Return the evidence to GPT; no policy acceptance/rejection follows here.

## Recorder, eligibility and identity

I extends H's guarded source-copy launcher. Only a separately compiled
`build/sr001i-instrumented/.../ExactSearch.class` receives instrumentation.
Production sources/classes and all CONTROL/A–G selectors are untouched. H's
launcher was factored into reusable functions; its generated diagnostic Search
source was verified identical to the committed H generator's output.

The caller allocates fixed primitive recording arrays once. H supplies stable
node/move/window/board/result fields; I adds immediate child adjudication,
negated natural child stand-pat, child subtree-node count and stand-pat-cutoff
status. No additional evaluator invocation, board transition or Search decision
is introduced. CSV formatting occurs after Search. The optional test-oracle
observer similarly receives the naturally evaluated static value, separate from
the selected continuation's terminal cause. Its 200,000-node / ply-64 suitability
guard is unchanged.

Prospective observations are legal non-promotion captures, including EP, from
non-check qnodes, which do not give check. The **primary numeric dataset further
requires a nonterminal child** after Search's ordinary mate/stalemate and rule
adjudication. Promotions, checking captures and checked-parent evasions are not
analyzed for futility. No move is excluded from Search.

The corpus has all 61 H fixtures plus one legal immediate-stalemate fixture:
`k7/8/1p2Q3/4K3/8/8/8/8 w - - 0 1`, `Qe6xb6`. It removes the last black pawn
and leaves Ka8 without a legal move, without giving check. Each fixture runs at
full window and the two one-unit windows around its completed value. Together
with 14 benchmark runs, **200/200 runs completed**, recorder-on/off identical in
score, best move, PV, normal/q/total nodes, maximum qply and completion/depth.

All **197 retained H runs and 195,921 H observation rows** match their common
fields exactly. The two new stalemate observations bring the total to
**195,923**, of which **195,913** are primary. Independent production-class JVM
runs also reproduce all **28 CONTROL/QSEARCH_BASELINE** retained H/G depth-2/3
records. No diagnostic timing is used as a Search performance measurement.

## Immediate terminal children

| Adjudication | Visited observations | Exhaustive eligible-capture occurrences |
|---|---:|---:|
| Insufficient material | 8 | 28 |
| Stalemate | 2 | 13 |
| Mate | 0 | 0 |
| Repetition | 0 | 0 |
| Rule-50 | 0 | 0 |
| Other | 0 | 0 |

These are protected by adjudication; their postMoveStatic fields are absent,
not fabricated as zero. Mate children are outside the non-check-child class.
Captures reset the halfmove clock, and their material decrease prevents return
to a pre-capture position in legal history. Root/history/checked-terminal tests
still cover repetition, rule-50 and mate precedence without inventing capture
examples. Selected-line draws reached *later* remain in the numeric dataset.

## Distributions and offline screen

Percentiles use nearest rank. Repeated visits are observations, not independent
positions; mate scores and fail-soft bounds are explicitly identified.

| Population / metric | Count | Min | Median | p90 | p95 | p99 | Max |
|---|---:|---:|---:|---:|---:|---:|---:|
| All primary staticGap | 195,913 | -1,424 | 18 | 671 | 839 | 1,111 | 32,477 |
| Alpha-raising staticGap | 50,115 | -1,424 | -329 | -78 | -39 | -7 | -1 |
| Alpha-raising **positive** staticGap | **0** | — | — | — | — | — | — |
| Final-best fail-low positive staticGap | 30,701 | 1 | 197 | 723 | 882 | 1,107 | 31,709 |
| All primary continuationGain | 195,913 | -33,692 | 0 | 0 | 0 | 0 | **0** |
| Alpha-raising continuationGain | 50,115 | -1,316 | -123 | 0 | 0 | 0 | **0** |
| Beta-cutoff continuationGain | 45,593 | -1,316 | -126 | 0 | 0 | 0 | **0** |
| Exact uniquely-best continuationGain | 305 | -762 | 0 | 0 | 0 | 0 | **0** |
| Exact equal-best continuationGain | 8 | -447 | -382 | -36 | -36 | — | -36 |

There are no positive continuationGain witnesses. EP (7 observations),
SEE-negative captures (49,354), same-target recaptures (35,675), clearance
fixtures (5), later mate lines (273) and later draw lines (11) all obey the bound.
These subclass populations overlap. The largest negative gain is a losing
mate-bound line, not a 33,692-unit ordinary static-evaluation error.

`locally_relevant` in the datasets retains H's definition: alpha raise **or**
final-best move. Its 30,701 positive gaps are all final-best fail-low bounds,
with continuationGain exactly zero. Their bound/PV relevance must not be
misreported as alpha improvements or ignored when considering an actual skip.

| Margin | Strict predicted / % | Inclusive predicted / % | Alpha raises / cutoffs lost | Final-best fail-low strict / inclusive | Exact-best visits strict / inclusive |
|---:|---:|---:|---:|---:|---:|
| 0 | 101,671 / 51.8960% | 102,120 / 52.1252% | 0 / 0 | 30,701 / 31,064 | 23 / 40 |
| 300 | 50,724 / 25.8911% | 50,838 / 25.9493% | 0 / 0 | 11,761 / 11,785 | 18 / 18 |
| 832 | 10,117 / 5.1640% | 10,179 / 5.1957% | 0 / 0 | 1,935 / 1,938 | 5 / 5 |
| 31,709 | 14 / 0.0071% | 15 / 0.0077% | 0 / 0 | 0 / 1 | 0 / 1 |
| 31,710 | 14 / 0.0071% | 14 / 0.0071% | 0 / 0 | 0 / 0 | 0 / 0 |

Strict means `post + margin < alpha`; inclusive means `<= alpha`. Margins 300
and 832 are the median/p90 of positive **non-improving** static gaps, not safety
calibration constants. Zero is the smallest nonnegative margin protecting all
observed alpha raises/cutoffs and outcome-sensitive selected lines. If literal
protection of every observed final-best fail-low return is required instead,
the sample minimum is 31,709 strict / 31,710 inclusive. That large number comes
from a mate-valued incoming window, not a useful conventional futility margin.

All screened mate-/draw-sensitive counts are zero for post-static evidence.
The exact-best counts are uniquely-best moves whose exact values are already
at/below the visited alpha, not lost exact improvements. Every prediction at
every shown nonnegative margin was already a child stand-pat cutoff. These are
visited-move predictions only: **no node/time savings are claimed**.

## Direct H comparison and tactical witnesses

On the common nonterminal class, strict margin-zero material delta predicts
113,773 moves (58.0732%), including **5,230 alpha raises / 4,198 beta cutoffs**.
Its positive protective-gap distribution is 1 / 67 / 225 / 268 / 354 / 520
(min/median/p90/p95/p99/max). Post-static predicts 101,671 moves (51.8960%),
including **zero** raises/cutoffs. At comparable zero observed raise losses,
H's strict margin 520 predicts 34,028 (17.3689%); post-static margin zero predicts
51.8960%, all already cut by child stand-pat. H's inclusive margin 520 still
predicts four raises/cutoffs. This is stronger separation with no demonstrated
additional subtree elimination.

| Retained H witness | H alphaGap | postMoveStatic | I staticGap | Searched / exact move value | I treatment |
|---|---:|---:|---:|---:|---|
| Kiwipete d2 `h3g2`, qply 17/path 19, passed-pawn change | 520 | -56 | -59 | -56 / -56 | Retained; continuationGain 0 |
| EP-winning `e5d6`, lower window | 304 | 434 | -1 | 434 / 434 | Retained; continuationGain 0 |
| SEE-negative clearance `Na4xc5`, lower window | 180 | -410 | -1 | -410 / -410 | Retained; continuationGain 0 |
| `draw-five`, `Kb5xa5`, full window | 43 | — | — | 0 / 0 | Immediate insufficient material |
| King captures last rook `Ke1xd2`, lower window | 280 | — | — | 0 / 0 | Immediate insufficient material |
| X-ray sequence `Ra1xa5`, lower window | 331 | 707 | -1 | 707 / 707 | Retained; continuationGain 0 |

The passed-pawn witness's exact value exceeds H's raw material expression by
579, but equals postMoveStatic. The draw-five child would statically score
-230 in parent perspective; adjudication, not a numeric margin, supplies zero.
These stronger exact witness values are retained H oracle evidence, aligned to
unchanged I observations; one of H's other 12 witnesses remains guard-rejected
and is not relabelled exact. The witness CSV includes FENs, IDs, all 12 H cases,
terminal exclusions, the largest negative gain and the largest fail-low gap.

The latter is `Qe7xf7` at qply/path 2 in
`7k/4Qrbp/4NN2/8/8/8/8/4K3 w - - 0 2`: alpha 32,765, postStatic/returned/exact
value 1,056, gap 31,709. It is uniquely best at that node but cannot meet the
incoming mate-valued window. This explains why preserving a return/PV differs
from merely preserving its upper-bound validity.

## Exhaustive evidence and benchmark coverage

**56/62 fixtures** completed exhaustive enumeration, **1,495 oracle nodes**;
six inherited large-tail fixtures hit the unchanged suitability guard. Their
alpha-beta observations remain labelled as bounds. The oracle produced 613
eligible-capture occurrences: **572 nonterminal**, including 305 uniquely best,
8 equal-best and 259 below-best; 41 immediate terminals were protected separately.
Seven primary uniquely-best moves lead to a later draw, and 15 immediate-terminal
moves are uniquely drawing. No primary unique mating move was observed.
All 572 exact continuation gains are nonpositive. There are 280 visited/exact
links (270 nonterminal); their exact/upper/lower relationships all validate.

All seven HCE/TT-off positions ran at depths 2 and 3 with the unchanged one-million
total-node budget. Requested depth equalled completed depth in all 14 runs.

| Position | d2 qnodes / max qply / observations | d3 qnodes / max qply / observations |
|---|---:|---:|
| start | 98 / 2 / 5 | 978 / 6 / 56 |
| kiwipete | 18,871 / 23 / 11,940 | 108,106 / 26 / 68,481 |
| endgame | 100 / 4 / 26 | 352 / 4 / 53 |
| tactical | 87 / 3 / 31 | 444 / 5 / 30 |
| evasion | 18,326 / 28 / 12,530 | 30,656 / 27 / 20,356 |
| middlegame | 13,957 / 19 / 8,413 | 61,721 / 24 / 42,398 |
| transposition-pawns | 25 / 0 / 0 | 111 / 0 / 0 |
| Total | **51,464 / max 28 / 32,945** | **202,368 / max 27 / 131,374** |

Normal/total nodes: **174 / 51,638** at d2; **849 / 203,217** at d3.
The benchmark supplies 164,319 primary observations; the corpus/windows supply
31,594. Maximum qply across all runs is **31**. No executed diagnostic invocation
hit the budget or ply-256 limit; dedicated tests exercise incomplete safety.
Full scores, moves, PVs, depths, node counts and diagnostic elapsed/NPS are in
RUNS. Instrumented/export and cold validation timings are **not performance
results**. G's retained clean position-median sums are 27.351 ms (d2) and
106.933 ms (d3), with 12 warmups / 7 repetitions; no I performance gain is claimed.

## Evaluator domain versus calibration

`ExactEvaluator` supplies side-to-move integer scores, with ordinary values
restricted to +/-32,511; mate scores use the separate 32,512..32,768 band and
actual root/path ply. HCE clamps within +/-30,000. Post-static, alpha and searched
values use the same active invocation's score domain. No material-unit addition
or conversion is needed. This expression and the stand-pat proof are evaluator
generic under the existing interface and qsearch semantics.

Positive fixed margins still select different quantities of work across evaluator
scales/volatility and encode calibration assumptions if interpreted as comparable
numeric tolerance. In this exact eligibility class, however, nonnegative margins
need **no empirical safety allowance**: they merely select subsets of an existing
zero-margin bound. Margin zero compares order in one score domain, has no imported
unit scale, and identifies the existing cutoff. It is not a confidence estimate.

`SearchEvaluation.State` / `ExactEvaluator` expose evaluation and state transitions,
not a generic confidence/uncertainty scale. Current NNUE V1 and BRN mappings are
explicitly uncalibrated, sign-preserving bounded mappings to +/-32,511, not shared
centipawn values. No better generic confidence signal is exposed at this boundary.
The fixed-depth harness selects HCE directly, so no trained neural sample was
added. Cross-evaluator empirical margin-frequency validation remains deferred;
it is unnecessary for the algebraic stand-pat bound, and no mappings were changed.

## Validation, artifacts and reproduction

**273 existing Search-scoped tests passed**, zero failures/errors. The ordinary
run separately skips the 11 shadow-only tests. **All 11 shadow tests passed**
(six I plus five retained H), zero skips/failures/errors. Coverage includes exact
corpus identity/repeatability, no added evaluation/transition calls (count and
ordered hash), child-static bound and one-node cutoff identity, narrow-window
fail-soft returns, EP/negative SEE, immediate insufficient/stalemate exclusion,
mate/rule-50/repetition precedence, actual-ply mate scoring, ply-256 abort,
budgets, interruption, evaluator-triggered cancellation and oracle observer
score/node/evaluation identity. Final analysis checks all H alignments, arithmetic,
completed records, exact bound relationships and independent production records.

Commands, repository root (`$env:DEBUG=''` for Gradle):

```text
.\gradlew.bat :app:compileTestJava -Pheadless
python tools/search-postmove-diagnostics.py --test
python tools/search-postmove-diagnostics.py .
.\gradlew.bat :app:exactSearch -Pheadless '-PsearchArgs=--leaves=both --tt=off --position=start,kiwipete,endgame,tactical,evasion,middlegame,transposition-pawns --depth=2 --warmups=0 --repetitions=1 --node-limit=1000000' > app/build/sr001i-clean-depth2.txt
.\gradlew.bat :app:exactSearch -Pheadless '-PsearchArgs=--leaves=both --tt=off --position=start,kiwipete,endgame,tactical,evasion,middlegame,transposition-pawns --depth=3 --warmups=0 --repetitions=1 --node-limit=1000000' > app/build/sr001i-clean-depth3.txt
python tools/analyze-search-postmove.py
.\gradlew.bat :app:test -Pheadless --tests 'com.ohinteractive.seedv6.search.exact.*' --tests 'com.ohinteractive.seedv6.tools.search.ExactSearchHarnessTest' --tests 'com.ohinteractive.seedv6.search.driver.*' --tests 'com.ohinteractive.seedv6.core.SeeThresholdTest' --tests 'com.ohinteractive.seedv6.core.SeeGeneratedLegalTest'
```

Java 21, one thread, `-Xms256m -Xmx256m -Xbatch` for diagnostics; no GUI.
The analysis expects the shown Windows PowerShell UTF-16 production-harness logs.
An initial Java local-name compile error was fixed before validation; a PowerShell
stderr-redirection wrapper reported failure despite a completed diagnostic run,
so the final diagnostic run used Python `subprocess` with merged output and
explicitly verified exit 0. No Search correctness divergence occurred.

Changed files: test-only `QuiescenceOracle.java`; H's launcher
`archive/search-programme/tools/search-delta-diagnostics.py`; new test-only `QuiescencePostMoveEvidence.java`,
`QuiescencePostMoveEvidenceTest.java`, `QuiescencePostMoveCorpus.java`; new
`archive/search-programme/tools/search-postmove-diagnostics.py`, `tools/analyze-search-postmove.py`; this
report and ten prefix-matched CSV/CSV.gz datasets:
OBSERVATIONS (losslessly compressed, about 6.44 MB), RUNS, IDENTITY,
PRODUCTION_IDENTITY, ORACLE, ORACLE_LINKS, DISTRIBUTIONS, MARGINS,
TERMINAL_CHILDREN and WITNESSES. H datasets are unchanged.

Deliberately skipped: optional depth 4 / repeated known large tails; slow NNUE
and training; neural empirical calibration requiring a different setup; unrelated
GUI/browser/full-suite tests; strength/self-play; guard-bypassing enumeration;
JVM/cache profiling; actual pruning and performance trials. None is needed to
validate this unchanged-tree evidence unit.

Remaining unknowns: real-game occurrence frequencies, other evaluator distributions,
and any benefit or risk from a separately designed implementation that changes
where this already-existing proof is resolved. No evidence here establishes a
new source of descendant-tree savings or authorizes changing fail-soft/PV returns.
Checked-child, terminal-child and other eligibility changes remain outside scope.

I remains uncommitted pending GPT reconciliation: **two modified tracked files,
16 untracked files, nothing staged**. No production source or canon changed; no
push/history rewrite. Finalization is **no_bump** (build 23); verified helper finish
and final Git audit are reported in the completion. CODEXLOG_CURRENT.md is absent
and was not created.

Human actions required after this prompt: None for completing this evidence unit.
Any later pruning implementation, policy acceptance or canon maintenance requires
a separate GPT reconciliation/authorization; no such work is begun here.
