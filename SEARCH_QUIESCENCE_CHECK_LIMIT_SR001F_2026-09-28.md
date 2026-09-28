# SR-001F: bounded ordinary quiet-check participation

Research complete for GPT reconciliation. Structural limits remove the observed
SR-001E ply-256 failures and recover several quiet-check resources cheaply in
compact positions. They do not provide interchangeable tactical coverage or a
uniform workload bound. INITIAL_ONLY misses a deeper mate and a queen fork;
MAX_ONE misses a two-check mate; MAX_TWO misses a three-check mate and frequently
exhausts the node budget. No candidate is accepted, rejected, combined, adopted
or canonized by this report. SR-001 remains pending.

**Authority and inherited boundary.** Search Contract R015 and Research Frontier
F010 remain unchanged, with SHA-256 respectively
`52dd6d2d8a24881f1ac73f3639b051f35ad9ca379f1eeb74c6242a32b84e6882` and
`3fd49cd19c49cfd223ea7c56d4547f7b3003d5e5d5ab2babd967630e39f64187`.
No repository/ancestor AGENTS.md was found; supplied shared governance applies.
Before mutation, all five modified tracked/eight untracked E paths matched its
finalized receipt, including build 21, report and five datasets. The index was
empty and there was no unrelated work. SR-001E was preserved in focused commit
`a8e789f3fa855bf2834c4372897562133b1f61f8`; A/B/C/D boundaries remain intact.

**Mechanics and exact candidate definitions.** Three primitive selectors, 33/34/35,
reuse ExactSearch's E quiet-generation/make/check phase after the unchanged
SEE-good/SEE-bad tactical phases. An ordinary quiet check is a legal noncapture,
nonpromotion move from a nonchecked qnode that leaves the opponent in check.
All E forms remain eligible, including discoveries, double checks, pawn/king
checks and checking castling. Generated order is retained within that phase.

* `QCHECK_INITIAL_ONLY`: the ordinary quiet-check phase is eligible iff qply is 0.
* `QCHECK_MAX_ONE`: eligible iff the path's ordinary-quiet-check count is 0.
* `QCHECK_MAX_TWO`: eligible iff that count is less than 2.

One integer recursion argument starts at zero on each normal-Search-to-qsearch
entry. Only a searched ordinary quiet check increments the child argument; it
counts both sides' moves together. Siblings receive independent values.
Captures, en passant, every promotion and every forced evasion leave it unchanged,
including quiet/counterchecking evasions. An already checked node always searches
all legal evasions, regardless of allowance. This is no qdepth or absolute tree
size cap: tactical and evasion chains remain unlimited subject to existing safety.

The existing suffix-validation entry point now accepts consumed quiet-check count
as well as consumed qplies. Harness/corpus PV re-search retains both, instead of
accidentally granting a fresh allowance. Root/path ply still controls mate distance.
Terminal/draw precedence, stand-pat, recursive fail-soft alpha-beta, evaluator
boundary, repetition/rule maintenance, qPV and incomplete handling are unchanged.
No normal quiet history is read/updated. No per-node objects, move wrappers,
collections or extra dynamic dispatch were added. Reused boards/move buffers
remain the hot-path mechanism; diagnostic-only primitive counters are optional.

CONTROL, QSEARCH_BASELINE, all B/C selectors, QSEARCH_QTT and unrestricted E remain
directly selectable. F uses no normal Search TT, qTT, SEE pruning, qdepth,
delta/futility, PVS, reductions, check extensions or new ordering heuristic.
Production/default behavior is unchanged. Board/Gen, SearchKey and TTable were
not modified.

**Correctness and reference preservation.** The final Search-scoped regression
passed **263 tests in 27 suites**, no failures/errors/skips, in 71 seconds. This
includes 14 F tests, 22 harness tests, existing exact/static/A/B/C/D/E/oracle,
driver and SEE tests. The earlier focused F/harness run also passed; final suite
includes the subsequent normal-entry and harness additions.

The exhaustive oracle independently enumerates legal moves with each candidate's
eligibility expression and primitive consumed count. There are **65 matching
corpus oracle comparisons**, totaling 9,714 enumerated nodes, plus three matching
normal-depth-1 comparisons proving fresh allowance at each normal leaf. The
corpus includes unrestricted enumeration on practical bounded fixtures, and
candidate enumeration of deeper/sacrifice cases. No guard-truncated oracle is
treated as an exact value. Large unrestricted trees, including the three-check
mate fixture, were not exhaustively enumerated; the latter's legal mating line
and unique forced replies are independently checked.

Focused tests cover sibling independence, first/second/third eligibility,
captures/promotions/evasions not consuming allowance, counterchecks with closed
allowance, all initial quiet-check forms, rule-50/repetition/stalemate/mate
precedence, actual-ply mate scoring, fail-soft cutoffs, PV suffix state, cancellation,
thread interruption, absolute safety, diagnostic parity, determinism and absent
history/TT/selective state. All completed corpus PVs are legal/admitted and each
prefix realizes the same value when re-searched with the consumed allowance.

All **42 CONTROL/QSEARCH_BASELINE benchmark records** match retained D records
exactly in completion, score, move, PV, normal/q/total nodes and maximum qply.
All **106 old E corpus baseline/unrestricted records** match their recorded
values, PVs, nodes, completion and E diagnostics. All seven freshly paired
unrestricted benchmark trees match E. Sixty F diagnostic trees match their clean
counterparts; all paired warmed trees also match primary/retained trees.

**Corpus and preserved E resources.**
[CORPUS.csv](SEARCH_QUIESCENCE_CHECK_LIMIT_SR001F_2026-09-28_CORPUS.csv) contains
58 fixtures / 290 mode rows: all 53 E rows plus five new compact fixtures (one
pre-existing duplicate FEN remains intentional). INITIAL_ONLY and MAX_ONE each
complete all 58. MAX_TWO completes 56, with two budget stops. Unrestricted E
completes 31, with 21 budget and six ply-256 stops. Scores of incomplete searches
are left blank and never used as semantic comparisons.

| Existing completed E resource | INITIAL_ONLY | MAX_ONE | MAX_TWO | Unrestricted |
|---|---:|---:|---:|---:|
| Mate in one, `g6g7` | 32767 | 32767 | 32767 | 32767 |
| Knight fork winning queen, insufficient-material draw | 0 | 0 | 0 | 0 |
| Quiet queen sacrifice, `e6g8 f8g8 h6f7` | 152; mate lost | 152; mate lost | 32765 | 32765 |
| Quiet check reaching rule-50 draw | 0 | 0 | 0 | 0 |
| Pawn check forcing king displacement | 241 | 241 | 241 | 241 |

Thus INITIAL_ONLY/MAX_ONE preserve four of the five resources; MAX_TWO preserves
all five. All five start with a qply-0 check, but the queen sacrifice also needs
the second ordinary quiet check at qply 2. Starting at qply 0 does not imply that
one check suffices. Discovery/double-check/king-discovery/castling membership and
their unchanged stand-pat values also survive all candidates. All three now find
the fresh-clock mate in one that E fails to visit before exhausting its budget:
12/12/58 qnodes, versus E's one-million-node incomplete result.

**Deeper first checks and repeated checks.** Full FENs, scores, PVs and first-check
qply/ordinal are retained in CORPUS.csv. The five added fixtures are deterministic,
legal and mechanism-focused; they are not claims about real-game frequency.

| Fixture / mechanism | BASELINE | INITIAL_ONLY | MAX_ONE | MAX_TWO | Unrestricted |
|---|---:|---:|---:|---:|---:|
| first-after-capture-mate | 546 | 546 | -32766 | -32766 | -32766 |
| exchange-deeper-check | 381 | 381 | 377 | 374 | 374 |
| capture-allows-queen-fork | 944 | 944 | 587 | 449 | Incomplete |
| third-check-smother | 155 | 218 | 218 | 205 | 32763 |
| repeated-check-no-extra-value | 692 | 735 | 735 | 735 | 735 |

* **First at qply 1, mate:** from
  `5rRk/6pp/7N/8/8/8/8/4K3 b - - 0 1`, Black's only evasion is `f8g8`.
  White then has ordinary quiet `h6f7#`. INITIAL_ONLY cannot introduce that
  move at qply 1. All five modes match complete enumeration in four/five nodes.
  The negative root mate score reflects Black to move, not a regression claim.
* **First at qply 3 after an exchange:** from
  `5rrk/8/6KN/8/8/1Q6/8/8 w - - 0 1`, witness
  `h6g8 f8g8 g6h6` reaches Black's `g8g6+`. After `h6h5`, another check
  `g6h6+` at qply 5 distinguishes MAX_ONE from MAX_TWO. The checks reposition
  the king; no material changes on these checking edges. This is a small
  positional difference, not evidence of tactical superiority from score alone.
* **First at qply 1, material danger:** from
  `4k3/8/8/8/5n2/1r6/P4Q2/4K3 w - - 0 1`, BASELINE/INITIAL_ONLY choose
  `a2b3`, missing `f4d3+` followed by `d3f2`, a knight fork winning the queen.
  MAX_ONE/MAX_TWO instead choose `f2f4`. They differ on later rook checks in
  that alternative. Unrestricted E does not complete; no exact E-value claim
  is made for this fixture. All bounded modes match their exhaustive oracles.
* **First at qply 2 following promotion:** the existing quiet-promotion fixture
  has no ordinary qply-0 check. After `a7a8q` and the checked king's evasion,
  a checking queen move is available. INITIAL_ONLY stays at 1063; MAX_ONE and
  MAX_TWO return 1081/1083. Candidate oracles agree. This is positional
  information following promotion, not a newly proven material win.
* **A third check is necessary:** from
  `4k3/8/8/8/8/3q4/PPn5/1KR5 b - - 96 1`, unrestricted E completes in
  89 qnodes with `c2a3 b1a1 d3b1 c1b1 a3c2#`. The ordinary checks are at
  qplies 0, 2 and 4. Both White replies are uniquely forced. The rook capture
  resets rule 50 before the final mate. MAX_TWO excludes the third check and
  returns 205; its complete oracle also returns 205. This establishes a real
  mate lost solely to the count boundary under these semantics.
* **Repeated checks without extra value:** the rook fixture at halfmove 96 has
  a second quiet check available, but MAX_ONE/MAX_TWO/E all return 735.
  MAX_TWO/E visit 13 qnodes versus MAX_ONE's five. Extra participation can add
  work without changing the completed value.

A separate compact **deeper-first quiet-check draw resource** was not established
reliably within this fixture effort. Qply-0 rule-50/repetition resources and the
queen-fork draw are covered, but do not fill that gap. No claim that deeper draw
checks are unnecessary follows. No substantial fixture-generation project or
invalid/artificial history was introduced to force coverage.

[WITNESSES.csv](SEARCH_QUIESCENCE_CHECK_LIMIT_SR001F_2026-09-28_WITNESSES.csv)
records all **72 completed score differences** against BASELINE and/or E, each
with the first causally excluded quiet check, path, qply and ordinal. These
follow the higher-valued continuation recursively with retained path state;
none exhausted its witness budget. A witness can be off the final root PV.
INITIAL_ONLY/MAX_ONE/MAX_TWO differ from BASELINE on 15/25/24 completed corpus
scores (58/58/56 matched); they differ from completed E on 4/3/1 scores (31
matched each). Equal scores do not prove globally equivalent move sets.

**Headless method and durable rows.** Same seven named FENs, HCE, normal TT off,
1,000,000-node limit, Java 21 and `-Xms256m -Xmx256m -Xbatch`. Primary timing uses
12 warmups/seven repetitions, upper median; multi-mode runs rotate mode order.
Depths 2/3 run all bounded candidates on all seven. At depth 4, all three run
start/endgame/tactical/pawns; INITIAL_ONLY/MAX_ONE also run Kiwipete/evasion/
middlegame because they completed depth 3. MAX_TWO skips those three after its
depth-3 budget failures. CONTROL/BASELINE run all seven at depth 4, including
one comparable baseline Kiwipete rerun (again 999,200 qnodes, max qply 32, incomplete).

[BENCHMARK.csv](SEARCH_QUIESCENCE_CHECK_LIMIT_SR001F_2026-09-28_BENCHMARK.csv)
contains **160 rows**: 102 primary F-unit rows, 42 fresh paired timing rows and
16 explicitly marked retained E primary rows. Each has full FEN, requested/
completed depth, score, move, PV, normal/q/total nodes, max qply, median ms, NPS,
completion reason, budget, warmups/repetitions and source/run-set identity.
Retained E failures provide historical completion context, not fresh timing.

For direct E timing, six-mode `sr001f-all` reruns only its completed sets:
start/endgame/pawns at depth 2, endgame/pawns at depths 3/4. These use 128 warmups
and 15 repetitions, with counters disabled. This avoids repeated known-expensive
E failures and provides contemporaneous comparisons of the changed shared method.
No current F wall-time claim relies on comparing against old E timings.
Additional depth-4 heavy cases used separate selector runs; this limits timing
precision versus one fully paired run. The larger effects are supported by
deterministic tree counts. Tiny timings and rounded milliseconds are not precise
microbenchmark estimates; no profiler/strength conclusion is inferred.

**Aggregate versus captures-only BASELINE, mutually completed positions.**
Percentages are changes, not reductions; differing subsets must not be compared
as though all positions completed. Full node/time sums and paired identities are
in [AGGREGATE.csv](SEARCH_QUIESCENCE_CHECK_LIMIT_SR001F_2026-09-28_AGGREGATE.csv).

| Depth | Candidate | Completed / attempted | Matched | Qnodes change | Total-node change | Wall change | Max qply B -> F | Score / move differences |
|---|---|---:|---:|---:|---:|---:|---:|---:|
| 2 | INITIAL_ONLY | 7/7 | 7 | +0.84% | +0.84% | +11.1% | 28 -> 28 | 0 / 0 |
| 2 | MAX_ONE | 7/7 | 7 | +662.7% | +660.5% | +661.4% | 28 -> 34 | 2 / 1 |
| 2 | MAX_TWO | 5/7 | 5 | +2193.3% | +2175.3% | +2056.9% | 19 -> 36 | 1 / 1 |
| 3 | INITIAL_ONLY | 7/7 | 7 | +2.30% | +2.29% | +1.91% | 27 -> 27 | 1 / 0 |
| 3 | MAX_ONE | 7/7 | 7 | +537.3% | +535.1% | +508.7% | 27 -> 34 | 2 / 1 |
| 3 | MAX_TWO | 4/7 | 4 | +155.8% | +138.8% | +123.8% | 6 -> 13 | 1 / 1 |
| 4 | INITIAL_ONLY | 6/7 | 6 | +27.5% | +26.9% | +33.4% | 26 -> 28 | 2 / 1 |
| 4 | MAX_ONE | 5/7 | 5 | +310.7% | +300.0% | +299.2% | 21 -> 29 | 2 / 1 |
| 4 | MAX_TWO | 4/4 eligible | 4 | +151.7% | +127.1% | +145.1% | 11 -> 20 | 2 / 1 |

BASELINE itself completed 7/7, 7/7 and 6/7 at depths 2/3/4; CONTROL completed
all 21. Counting incomplete work too, F qnodes at depths 2/3/4 were respectively:
INITIAL_ONLY 51,896 / 207,018 / 1,336,152; MAX_ONE 392,528 / 1,289,782 / 2,540,738;
MAX_TWO 2,327,138 / 3,004,444 / 27,577 (only four eligible depth-4 positions).
These spent-work totals are not completed-value comparisons.

**Fresh paired aggregate versus unrestricted E.** All rows below are mutually
completed: three positions at depth 2 and two at depths 3/4.

| Depth | Candidate | Qnode change | Total-node change | Wall change | Max qply E -> F | Score / move differences |
|---|---|---:|---:|---:|---:|---:|
| 2 | INITIAL_ONLY | -77.3% | -74.3% | -73.0% | 26 -> 4 | 1 / 1 |
| 2 | MAX_ONE | -56.2% | -54.0% | -54.5% | 26 -> 6 | 0 / 0 |
| 2 | MAX_TWO | -39.9% | -38.4% | -41.1% | 26 -> 8 | 0 / 0 |
| 3 | INITIAL_ONLY | -72.9% | -70.0% | -71.6% | 25 -> 4 | 1 / 1 |
| 3 | MAX_ONE | -54.3% | -51.8% | -56.5% | 25 -> 5 | 0 / 0 |
| 3 | MAX_TWO | -43.4% | -41.4% | -37.5% | 25 -> 7 | 0 / 0 |
| 4 | INITIAL_ONLY | -93.7% | -92.8% | -92.9% | 133 -> 6 | 0 / 0 |
| 4 | MAX_ONE | -92.3% | -91.4% | -91.6% | 133 -> 7 | 0 / 0 |
| 4 | MAX_TWO | -89.6% | -88.8% | -88.4% | 133 -> 9 | 0 / 0 |

INITIAL_ONLY loses E's endgame pawn-check resource at normal depths 2/3, where
it lies at qply 2/1. It recovers it at normal depth 4, where it is at qply 0.
MAX_ONE/MAX_TWO retain that benchmark resource at all three depths.

Directly versus MAX_ONE on their mutually completed sets, MAX_TWO adds
294.7% / 75.6% / 73.4% qnodes and 278.4% / 48.9% / 51.8% wall time at depths
2/3/4, with 0/0/1 score differences and no best-move differences. Heavy failed
MAX_TWO positions are absent from those percentages. On the common four
depth-4-eligible positions, qnodes scale from depth 2 -> 3 -> 4 as follows:
INITIAL_ONLY 417 -> 1,914 -> 13,570; MAX_ONE 689 -> 2,745 -> 15,906;
MAX_TWO 1,342 -> 4,821 -> 27,577. This is improved completion relative to E,
but not evidence of cheap general deeper Search.

**Separate diagnostics and pathology.**
[DIAGNOSTICS.csv](SEARCH_QUIESCENCE_CHECK_LIMIT_SR001F_2026-09-28_DIAGNOSTICS.csv)
retains 60 candidate rows. Diagnostic-only generation scans all legal quiets
at eligible nonterminal nonchecked nodes, including nodes subsequently cut by
stand-pat/tacticals. `generated_checks` counts the legal checking subset in that
scan; `searched` counts actual recursive quiet-check children. These diagnostic
scans are absent from clean timing.

`prevented_nodes` counts nonterminal nonchecked nodes whose allowance is closed,
before stand-pat. It proves phase ineligibility, not that each such node had a
useful quiet check or would otherwise have searched one. Legal-existence quiet
generation needed for stalemate detection still occurs. The existing chain
counter counts added checks connected through quiet evasions, resetting on a
tactical edge; it is not the length of every possible checking sequence.

| Depth | Candidate | Generated checks | First searched | Second searched | Quiet-check cutoffs | Allowance-closed nodes |
|---|---|---:|---:|---:|---:|---:|
| 2 | INITIAL_ONLY | 90 | 39 | 0 | 0 | 45,703 |
| 2 | MAX_ONE | 89,163 | 17,691 | 0 | 986 | 271,589 |
| 2 | MAX_TWO | 759,019 | 14,328 | 161,860 | 11,413 | 1,599,076 |
| 3 | INITIAL_ONLY | 1,803 | 42 | 0 | 1 | 180,198 |
| 3 | MAX_ONE | 304,881 | 56,059 | 0 | 2,804 | 881,816 |
| 3 | MAX_TWO | 1,073,198 | 27,297 | 226,511 | 16,708 | 1,866,478 |
| 4 | INITIAL_ONLY | 5,638 | 1,053 | 0 | 28 | 1,168,840 |
| 4 | MAX_ONE | 529,717 | 106,337 | 0 | 7,173 | 1,728,535 |
| 4 | MAX_TWO, four eligible | 15,073 | 1,258 | 3,516 | 62 | 7,756 |

First plus second equals all searched quiet checks. These sums include partial
work from budget stops. Observed maximum ordinary checks per path and maximum
chain are 1/1/2 for INITIAL_ONLY/MAX_ONE/MAX_TWO, as required.

No F benchmark or corpus run reached ply 256. Budget stops remain:

* MAX_TWO depth 2: Kiwipete (999,965 qnodes, max qply 42), evasion (999,994, 38).
* MAX_TWO depth 3: Kiwipete (999,968, 38), evasion (999,934, 37), middlegame
  (999,721, 38). These three were not attempted at depth 4.
* INITIAL_ONLY depth 4: Kiwipete (999,213, 32).
* MAX_ONE depth 4: Kiwipete (999,827, 34), evasion (999,135, 34).
* MAX_TWO corpus: tail-kiwipete-e1d1 and tail-evasion-f1f2, each one million
  qnodes, maximum qply 40/35. Corpus maxima overall are 31/40/40 for I/ONE/TWO.

The count boundary therefore suppresses repeated ordinary checking tails but
does not bound the number of tactical branches or forced-evasion depth. No
additional truncation or fabricated score was introduced to complete these runs.

**Commands and validation scope.**

```powershell
$env:DEBUG=''
.\gradlew.bat :app:test -Pheadless --tests '*QuiescenceCheckLimitResearchTest' --tests '*ExactSearchHarnessTest'
.\gradlew.bat :app:test -Pheadless --tests 'com.ohinteractive.seedv6.search.exact.*' --tests 'com.ohinteractive.seedv6.tools.search.ExactSearchHarnessTest' --tests 'com.ohinteractive.seedv6.search.driver.*' --tests 'com.ohinteractive.seedv6.core.SeeThresholdTest' --tests 'com.ohinteractive.seedv6.core.SeeGeneratedLegalTest'
.\gradlew.bat :app:exactSearch '-PsearchArgs=--leaves=sr001f --tt=off --position=start,kiwipete,endgame,tactical,evasion,middlegame,transposition-pawns --depth=2 --warmups=12 --repetitions=7 --node-limit=1000000'
java -Xmx256m -cp 'app/build/classes/java/main;app/build/classes/java/test' com.ohinteractive.seedv6.search.exact.QuiescenceCheckLimitCorpus
java -Xmx256m -cp 'app/build/classes/java/main;app/build/classes/java/test' com.ohinteractive.seedv6.search.exact.QuiescenceCheckLimitCorpus --witnesses
```

Repeat the primary harness command at depth 3. At depth 4 use sr001f on
start/endgame/tactical/transposition-pawns, then individual qcheck-initial,
qcheck-one and both on kiwipete/evasion/middlegame. Diagnostic runs use the same
eligible candidate positions with `--quiet-check-diagnostics --warmups=0
--repetitions=1`. Fresh paired runs use `--leaves=sr001f-all --warmups=128
--repetitions=15`, with start/endgame/pawns at depth 2 and endgame/pawns at 3/4.
Corpus CLI emits CSV; `--witnesses` emits causal differences. All benchmark
conditions and source runs are also recorded in the durable dataset.

Skipped deliberately: slow NNUE/training, GUI/browser, unrelated expensive full
suites, strength/self-play matches, profiling, depths above 4, MAX_TWO depth 4
after failed depth 3, repeated known E failures, large unrestricted exhaustive
enumerations and all excluded policy combinations. These do not establish the
isolated participation semantics, or would exceed the bounded research scope.
No budget increase, evaluator-margin pruning or production adoption occurred.

**Questions returned to GPT.** INITIAL_ONLY is the lowest-cost tested option and
retains four E resources, but its assumption fails on independently verified
deeper checks. MAX_ONE recovers those resources and completes depths 2/3 here,
yet costs roughly six to eight times baseline qnodes and loses a second-check
mate. MAX_TWO has broader tactical coverage, with much worse completion and
cost; three checks can still be necessary. There is no demonstrated error bound
or single candidate simultaneously preserving all tested resources and solving
workload growth. Count boundaries are understandable approximation rules; their
promise for a later baseline/selectivity combination remains a programme decision.
Real-game prevalence, deeper-first draw resources and playing strength remain
unknown. No higher numeric score or raw NPS is treated as improvement.

Changed files: ExactSearch, ExactSearchHarness, QuiescenceOracle,
ExactSearchHarnessTest, new QuiescenceCheckLimitCorpus and
QuiescenceCheckLimitResearchTest, this report and five CSVs, and helper-owned
VERSION_STATE finalization. F remains uncommitted with an empty index; no reset,
stash, amend, rebase or push occurred. The runtime research change qualifies for
a bump from build 21; the finalizer's verified result is recorded in the final
completion/VERSION_STATE rather than presumed here. CODEXLOG_CURRENT.md is absent
and was not created.

Human actions required after this prompt: None for completing this research unit.
GPT reconciliation is required before policy acceptance/rejection, combination,
production adoption or canon maintenance.
