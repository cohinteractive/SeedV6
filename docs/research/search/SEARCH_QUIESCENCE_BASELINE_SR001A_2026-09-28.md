# SR-001A: Quiescence semantic/reference baseline — 2026-09-28

This unit establishes an explicitly invoked research baseline, not a production
policy or an accepted SR-001 outcome. Repository masters were verified at
**Search Contract R015 / Research Frontier F010**, starting at clean commit
`9cdf0d8`. Both masters remain byte-for-byte unchanged. No repository-local
AGENTS.md or additional development/governance instruction file was found.
The supplied shared governance applies. Root CODEXLOG_CURRENT.md is absent.

The existing static-leaf TT-off reference remains available through
`new ExactSearch()`, its existing evaluator constructors, and the unchanged
default `exactSearch --tt=off` harness route. Existing TT-on production PVS,
SearchDriver, GUI/UCI and training integration do not opt into qsearch.

The smallest fit is a final boolean on the existing owner, a direct private
recursive routine, and the explicit factory
`ExactSearch.quiescenceResearch(ExactEvaluator)`. The factory fixes normal Search
to its existing TT-off CONTROL ordered-alpha-beta path. It cannot accept a TT,
PVS, capture-history or alternate ordering policy. No new runtime class,
per-node wrapper, policy object, dispatch layer, or evaluator-specific branch
was introduced. The qsearch routine reuses the owner's board, move, PV and
generator buffers; its primitive lazy keys are allocated once per owner.
SearchLineHistory reserves the existing 256-ply capacity at invocation entry.

The implementation is deliberately retained as the no-selectivity qsearch
reference for comparison with separately authorized future experiments.

Implemented semantics:

- Cancellation/interruption is checked on entry and during traversal. The boundary
  position is handed to qsearch at remaining normal depth <= 0 using the same
  board, window, actual root ply, history and evaluator state.
- Mate/stalemate precede rule draws, which precede evaluation. The existing
  DrawAdjudicator still implements rule 50, formal threefold and insufficient
  material. No twofold heuristic or GHI redesign is introduced.
- Non-check nodes generate legal tacticals first. If that list is empty, the
  existing quiet generator establishes legal existence using reused scratch;
  it does not search quiet children. Thus stand-pat cannot hide stalemate.
- At a nonterminal non-check node, static evaluation initializes best and is the
  explicit option to stop tactical extension. It is not a pass move. Stand-pat
  >= beta returns the actual value; otherwise it may raise alpha.
- The move set is exactly legal captures, en passant and every promotion,
  including quiet/capture underpromotions. All remain eligible, including SEE-negative
  moves; only ordinary alpha-beta bounds can terminate visitation.
- Non-check tactical selection reuses snapshotOrdering/Sort.next: SEE >= 0 before
  SEE < 0, descending immediate captured material plus promotion gain, generated
  ordinal for ties. No new tactical score or capture history is added.
- Checked nodes forbid stand-pat and search the complete generated legal-evasion
  list in deterministic generated order. Quiet king moves and blocks participate;
  an evasion giving check naturally enters another checked qnode. No quiet-history
  rewards or maluses are published.
- Every searched edge calls the existing evaluator child boundary and pushes a
  real position to SearchLineHistory, with a finally-protected pop. Mate scores
  remain -32768 + actual root ply, not qply. Static scores retain their existing
  reserved-mate-band validation.
- Recursive negamax uses [-beta, -alpha] and fail-soft scores throughout. A strictly
  better searched continuation supplies its move plus child qPV; stand-pat ties
  preserve the empty qPV. A qsearch beta cutoff exposes only the legal cutoff-move
  prefix. Narrow-window results retain the existing bound/prefix interpretation.
- There is no semantic qdepth cutoff. The existing absolute storage and mate-domain
  limit is root ply 256. If a node there still needs a child, it throws the existing
  Aborted signal. The invocation returns completed=false, completedDepth=-1,
  Value.INVALID, and no best move/PV. Terminal or stand-pat proofs that need no
  child can still resolve. An unresolved checked node never returns static eval.

Search TT probes/stores/hash moves, PVS/scouts in qsearch, ordinary quiet checks,
SEE/delta/futility pruning, razoring, recapture-only policy, reductions, check
extensions, aspiration and evaluator-specific policies are all excluded.

Changed files:

- `app/src/main/java/com/ohinteractive/seedv6/search/exact/ExactSearch.java`
- `app/src/main/java/com/ohinteractive/seedv6/search/exact/ExactSearchResult.java`
- `app/src/verification/java/com/ohinteractive/seedv6/tools/search/ExactSearchHarness.java`
- `app/src/test/java/com/ohinteractive/seedv6/search/exact/QuiescenceOracle.java`
- `app/src/test/java/com/ohinteractive/seedv6/search/exact/QuiescenceResearchTest.java`
- `app/src/test/java/com/ohinteractive/seedv6/tools/search/ExactSearchHarnessTest.java`
- This report; root VERSION_STATE.txt is handled separately by the required
  maintained finalizer because invocable application code changed.

Validation evidence:

```text
.\gradlew.bat :app:test -Pheadless --tests '*QuiescenceResearchTest' --tests '*ExactSearchTest' --tests '*ExactSearchHarnessTest'
.\gradlew.bat :app:test -Pheadless --tests 'com.ohinteractive.seedv6.search.exact.*' --tests 'com.ohinteractive.seedv6.tools.search.ExactSearchHarnessTest' --tests 'com.ohinteractive.seedv6.search.driver.*'
.\gradlew.bat :app:test -Pheadless --tests '*QuiescenceResearchTest' --tests 'com.ohinteractive.seedv6.tools.search.ExactSearchHarnessTest'
```

The initial focused set passed **71 tests**. The wider, still Search-scoped
regression run passed **191 tests in 37 seconds**, including static ExactSearch,
TT, ordering/staging, PVS, flat-reference equivalence, driver and production
boundary/integration tests. After two additional qsearch tests and bounded PV
verification were added, the final affected run passed **33 tests**
(**16 qsearch + 17 harness**, 4 seconds). Existing static-reference test meanings
were unchanged. `git diff --check` passed.

The independent test-only oracle generates the complete legal list, applies the
same terminal/draw precedence and stand-pat/evasion/admission rules, and enumerates
every admitted child without windows, alpha-beta cutoffs, SEE or ordering. It
makes **33 full-window root comparisons**, including an 18-case HCE matrix
(six fixtures at normal depths 0/1/2), plus oracle checks of every returned PV
prefix. Its fixture-only node/ply guards throw a test failure rather than a score;
none fired. Repeatability checks compare score, move, PV, normal/q/total nodes and
maximum qply. Narrow-window checks use the correct lower/upper bound inequalities,
not globally exact cutoff-score equality.

Focused coverage includes quiet nonterminal leaves; a profitable three-capture
exchange; visited losing captures with empty stand-pat PV; EP; every quiet and
capture promotion; a rook underpromotion avoiding queen stalemate; quiet-only
king evasions; a quiet bishop block giving check; checked mate; stalemate; a
legally replayed repetition cycle completed by a qline evasion; rule-50 reached
by evasion and reset by capture; actual-ply mate scoring; fail-soft stand-pat and
tactical cutoffs; legal extended/empty PVs; cancellation, interruption and node
budget propagation; exact tactical visitation order; reuse and determinism.

The absolute-ply guard was exercised by placing checked and non-check tactical
fixtures directly in slot 256 via test reflection. It emits the same Aborted
signal used by the tested incomplete-result path; mate at slot 256 returns
-32512 even when qply is 3. No natural benchmark line reached that boundary.

Two initial promotion assertions incorrectly assumed HCE must prefer immediate
promotion. Oracle agreement showed that HCE's advanced-pawn evaluation could
leave stand-pat best. Those move-choice fixtures now use an evaluator-independent
material evaluator, while HCE remains in the full-window matrix and all measured
runs. The rook-underpromotion fixture is
`8/k1P5/2K5/8/8/8/8/8 w - - 0 1`: queen promotion stalemates, rook promotion
continues with a positive material value. This observation does not change HCE
or the specified stand-pat semantics.

Headless HCE comparison:

```text
.\gradlew.bat :app:exactSearch "-PsearchArgs=--leaves=both --tt=off --position=start,kiwipete,endgame,tactical,evasion,middlegame,transposition-pawns --depth=2 --warmups=12 --repetitions=7 --node-limit=1000000"
.\gradlew.bat :app:exactSearch "-PsearchArgs=--leaves=both --tt=off --position=start,kiwipete,endgame,tactical,evasion,middlegame,transposition-pawns --depth=3 --warmups=12 --repetitions=7 --node-limit=1000000"
```

Environment: AMD Ryzen 5 5500, Windows 11 amd64, OpenJDK 21, one search thread,
`-Xms256m -Xmx256m -Xbatch`. The two modes rotate order each round. Timings are
median search wall time, including invocation setup but excluding worker
construction, FEN parsing and semantic verification. All repeated results were
deterministic; every completed PV prefix was verified outside timing by a
corresponding TT-off full-window search, with the same external node budget.
An initial depth-2 screen with 3 warmups/5 samples exposed substantial JIT timing
noise; the retained measurements use 12/7. Tiny timings remain descriptive,
not a controlled throughput optimization or strength experiment.

Each search has an external one-million-total-node cancellation budget.
Reaching it, including at a final checkpoint, reports incomplete rather than
a score. `--leaves=static|qsearch|both` selects the baseline(s); TT-on and other
research policy overrides are rejected in this mode. Existing non-SR-001 harness
modes retain their behavior.

All measured attempts completed normally, at their requested normal depth.
CONTROL's normal count includes its static leaves. In QSEARCH_BASELINE, the
boundary leaf is counted once as a qnode, not also as a normal node; total =
normal + qnodes. Maximum qply counts qsearch edges below that boundary, which
starts at zero. These classification differences matter when comparing normal
counts; total nodes is the comparable total-work count.

Exact position definitions:

| Name | FEN |
| --- | --- |
| start | `rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1` |
| kiwipete | `r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1` |
| endgame | `8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1` |
| tactical | `4k3/8/2p1p3/3q4/2P1P3/3Q4/8/4K3 w - - 0 1` |
| evasion | `r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1` |
| middlegame | `r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10` |
| transposition-pawns | `4k3/pp6/8/8/8/8/PP6/4K3 w - - 0 1` |

In the tables, C = CONTROL and Q = QSEARCH_BASELINE.

**Normal depth 2: requested = completed for every row; all completed normally.**

| Position | Mode | Best | Score | Normal nodes | Qnodes | Total | Max qply | Median ms | NPS |
| --- | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| start | C | e2e4 | 0 | 128 | 0 | 128 | 0 | 0.166 | 770156 |
| start | Q | g1f3 | 0 | 21 | 98 | 119 | 2 | 0.192 | 621085 |
| kiwipete | C | e2a6 | -12 | 214 | 0 | 214 | 0 | 0.130 | 1643625 |
| kiwipete | Q | e2a6 | 61 | 49 | 18871 | 18920 | 23 | 9.064 | 2087309 |
| endgame | C | b4f4 | 165 | 30 | 0 | 30 | 0 | 0.012 | 2542372 |
| endgame | Q | b4f4 | 165 | 15 | 100 | 115 | 4 | 0.053 | 2165725 |
| tactical | C | c4d5 | 690 | 63 | 0 | 63 | 0 | 0.021 | 2943925 |
| tactical | Q | c4d5 | 1072 | 25 | 87 | 112 | 3 | 0.038 | 2970822 |
| evasion | C | d2d4 | -1011 | 138 | 0 | 138 | 0 | 0.124 | 1113801 |
| evasion | Q | g1h1 | -410 | 7 | 18326 | 18333 | 28 | 9.220 | 1988502 |
| middlegame | C | g5f6 | -6 | 185 | 0 | 185 | 0 | 0.119 | 1554621 |
| middlegame | Q | c3d5 | 111 | 47 | 13957 | 14004 | 19 | 7.528 | 1860304 |
| transposition-pawns | C | e1d2 | 0 | 35 | 0 | 35 | 0 | 0.008 | 4166666 |
| transposition-pawns | Q | e1d2 | 0 | 10 | 25 | 35 | 0 | 0.010 | 3535353 |

| Position | Mode | PV |
| --- | --- | --- |
| start | C | `e2e4 e7e5` |
| start | Q | `g1f3 b8c6` |
| kiwipete | C | `e2a6 h3g2` |
| kiwipete | Q | `e2a6 h3g2 f3g2 b4c3 d2c3 e6d5 e5g6 f7g6 g2g6 e7f7` |
| endgame | C | `b4f4 h4g3` |
| endgame | Q | `b4f4 h4g3` |
| tactical | C | `c4d5 e6d5` |
| tactical | Q | `c4d5 c6d5 e4d5` |
| evasion | C | `d2d4 b2a1q` |
| evasion | Q | `g1h1 b2a1q d1a1 a3b4` |
| middlegame | C | `g5f6 e7f6` |
| middlegame | Q | `c3d5 e7d8 g5f6 g7f6` |
| transposition-pawns | C | `e1d2 e8d7` |
| transposition-pawns | Q | `e1d2 e8d7` |

**Normal depth 3: requested = completed for every row; all completed normally.**

| Position | Mode | Best | Score | Normal nodes | Qnodes | Total | Max qply | Median ms | NPS |
| --- | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| start | C | d2d4 | 42 | 992 | 0 | 992 | 0 | 0.481 | 2061512 |
| start | Q | b1c3 | 38 | 96 | 978 | 1074 | 6 | 0.609 | 1763836 |
| kiwipete | C | e2a6 | 393 | 2707 | 0 | 2707 | 0 | 1.724 | 1570367 |
| kiwipete | Q | e2a6 | 32 | 175 | 108106 | 108281 | 26 | 52.337 | 2068902 |
| endgame | C | b4c4 | 477 | 317 | 0 | 317 | 0 | 0.099 | 3218274 |
| endgame | Q | b4f4 | 167 | 44 | 352 | 396 | 4 | 0.116 | 3410852 |
| tactical | C | c4d5 | 1111 | 453 | 0 | 453 | 0 | 0.125 | 3635634 |
| tactical | Q | c4d5 | 1072 | 63 | 444 | 507 | 5 | 0.121 | 4176276 |
| evasion | C | d2d4 | -28 | 1027 | 0 | 1027 | 0 | 0.645 | 1592494 |
| evasion | Q | c4c5 | -503 | 112 | 30656 | 30768 | 27 | 16.202 | 1899036 |
| middlegame | C | c3d5 | 252 | 2296 | 0 | 2296 | 0 | 1.417 | 1620095 |
| middlegame | Q | c3d5 | 111 | 324 | 61721 | 62045 | 24 | 31.957 | 1941527 |
| transposition-pawns | C | e1d2 | 3 | 146 | 0 | 146 | 0 | 0.034 | 4319526 |
| transposition-pawns | Q | e1d2 | 3 | 35 | 111 | 146 | 0 | 0.037 | 3903743 |

| Position | Mode | PV |
| --- | --- | --- |
| start | C | `d2d4 b8c6 e2e4` |
| start | Q | `b1c3 d7d5 g1f3` |
| kiwipete | C | `e2a6 h3g2 f3f6` |
| kiwipete | Q | `e2a6 e6d5 c3d5 e7e5` |
| endgame | C | `b4c4 h5b5 a5b5` |
| endgame | Q | `b4f4 h4g3 f4f7` |
| tactical | C | `c4d5 e6d5 d3d5` |
| tactical | Q | `c4d5 c6d5 e4d5` |
| evasion | C | `d2d4 a3b4 h6f7` |
| evasion | Q | `c4c5 b6c5 d2d4 b2a1q d1a1 a3b4` |
| middlegame | C | `c3d5 c5f2 e2f2` |
| middlegame | Q | `c3d5 e7d8 g5f6 g7f6` |
| transposition-pawns | C | `e1d2 e8d7 d2d3` |
| transposition-pawns | Q | `e1d2 e8d7 d2d3` |

At depth 2, aggregate total nodes rise from **793 to 51,638** (65.12x):
174 normal + 51,464 qnodes. At depth 3 they rise from **7,938 to 203,217**
(25.60x): 849 normal + 202,368 qnodes. Sums of displayed per-position median
times are about 0.580 -> 26.105 ms and 4.525 -> 101.379 ms respectively.
These are different semantic trees; neither node count nor NPS is a strength
measurement.

The tactical depth-2 CONTROL PV stops after the recapture at score 690. Qsearch
continues through `c4d5 c6d5 e4d5`, scoring 1072. This is the intended horizon
change. Endgame, opening and evasion best moves also change in some comparisons;
these are not static-reference equivalence failures.

No runaway or incomplete natural run occurred. There is nevertheless substantial
expansion: at depth 2 the evasion position grows from 138 to 18,333 total nodes
(132.85x), with **18,326 qnodes / max qply 28**. Kiwipete depth 3 has the largest
measured qtree: **108,106 qnodes / max qply 26**, 108,281 total nodes versus
2,707 CONTROL nodes. Middlegame depth 3 has 61,721 qnodes / max qply 24. Across
this suite the deepest qline is 28, well below the absolute-ply safety boundary;
this bounded sample does not establish global explosion safety.

Remaining SR-001 questions exposed, without choosing a policy:

- The large evasion/tactical expansion and qply near 30 motivate a separately
  authorized control experiment against this preserved unpruned baseline.
  These data do not identify a safe pruning rule, margin or depth cap.
- Stand-pat may retain a high static advanced-pawn value over an immediate
  promotion. That follows the specified abstraction and evaluator; whether to
  revisit the model is a programme question, not grounds to alter this baseline.
- TT participation, positive-depth TT/PVS integration, quiet checks and later
  selectivity remain untested/open and outside this unit.
- Shallow HCE tree measurements do not establish playing strength, deeper-search
  scaling, behavior with neural evaluators or production time management.

No long NNUE/training suites, full repository/full slow suite, perft campaign,
GUI/browser QA, strength matches, deeper stress programme or JIT/allocation
profiler was run: none is needed to validate this explicit headless HCE semantic
unit. Allocation claims above describe inspected code and reused storage, not
a profiler measurement. No required correctness validation remains unavailable.

Changes are left uncommitted for review; no push or production adoption was
performed. The starting worktree was clean, so the listed changes are attributable
to this unit. VERSION_STATE.txt started at build 16; maintained-source executable
digests and binding were verified and begin succeeded before mutation. The
semantic finish decision is bump for the new invocable Search code; the final
verified helper outcome is reported in the completion message.

Human actions required after this prompt: **programme reconciliation in GPT**
before any SR-001 acceptance/rejection, production adoption or canon/frontier
update (blocking for those later actions, not for completion of this research
unit). Neither canon is edited by this unit.

