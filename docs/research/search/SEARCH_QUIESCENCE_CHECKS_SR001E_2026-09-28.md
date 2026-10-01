# SR-001E: ordinary quiet-check participation

Research complete for GPT reconciliation. Quiet checks expose compact mate,
material-saving and draw resources absent from SR-001A, but unrestricted inclusion
causes frequent incomplete searches and much larger trees. No policy is accepted,
rejected, restricted, combined, adopted or canonized here. Unlimited SR-001A remains
the independently selectable semantic reference.

**Authority and inherited boundary.** Repository masters remain Search Contract
R015 and Research Frontier F010, unchanged at SHA-256
`52dd6d2d8a24881f1ac73f3639b051f35ad9ca379f1eeb74c6242a32b84e6882` and
`3fd49cd19c49cfd223ea7c56d4547f7b3003d5e5d5ab2babd967630e39f64187`.
No repository/ancestor AGENTS.md was found; supplied shared governance applies.
The four modified tracked and seven untracked inherited D files, empty index,
build 20, report and data were inspected. All ten non-version files matched D's
finalized receipt byte-for-byte; VERSION_STATE matched its verified build 20.
No unexplained work was present. D was preserved before E mutation in commit
`c6787221b358ba3f9e0251caeea332ddf28d4336`. Earlier A/B/C boundaries are retained.

**Implementation and exact move definition.** Primitive selector
`ExactSearch.QSEARCH_QUIET_CHECKS = 32` uses the existing TT-off ordered-alpha-beta
research factory. The harness exposes `--leaves=quiet-checks` and the three-mode
`--leaves=sr001e` comparison. CONTROL and all A/B/C/D selectors remain independent;
no production/default caller opts into E.

An added quiet check is a legal move from `Gen.genQuiet` that is neither a capture
nor a promotion and leaves the opponent in check after `Board.makeMoveInto`.
The existing reusable child board supplies the check test. Nonchecking quiets
never reach evaluator-child preparation, history push or recursive search.
No Board/Gen redesign, recurring allocation or new ordering score is introduced.

Existing tacticals retain SR-015 lazy SEE-good/SEE-bad and material ordering.
Quiet moves are generated/appended after those phases and their checking subset
is searched in generated order. The legal-existence scratch is reused when no
tacticals exist; after child recursion it is regenerated through the existing
append mechanism. Promotions, en passant and capture checks stay in their
original tactical phase. Checked nodes still search every legal evasion in
generated order without stand-pat. No main quiet-history updates occur.

Cancellation and terminal/draw adjudication precede stand-pat. Fail-soft recursive
negamax, actual-root/path-ply mate scoring, history/rule-state maintenance and
legal searched qPV prefixes are unchanged. A best stand-pat has empty qPV.
There is no SEE pruning, qdepth, qTT, quiet-check cap, PVS or other selectivity in
E. Node-budget stops and unresolved absolute-ply-256 nodes remain incomplete.
At the last storage slot, a nonchecked node with no tactical or checking quiet
can still resolve through stand-pat; an unresolved quiet check cannot.

**Tests and independent oracle.** The test-only exhaustive oracle now optionally
admits quiet checks after independently generating all legal moves and making
each child. It has no alpha-beta cutoffs or tactical ordering dependence. Its
existing fixture-suitability guard fails rather than fabricating a value.
Seven practical quiet-inclusive full-window comparisons matched (67 exhaustive
nodes in total): direct mate, double discovery, blocked pawn check, king
discovery, castling check, rule-50 draw and rook checks with stand-pat best.
Six use a near-rule-50 state to make complete enumeration practical; the pawn
fixture is naturally bounded. These comparisons do not prove unrestricted large
trees exhaustively.

Focused tests verify direct/discovered/double checks, pawn pushes, a king move
opening a rook line, legal castling check, multiple checks, quiet sacrifice,
mate, material gain, and unchanged capture/EP/all-promotion membership. They also
check tactical-before-quiet order, generated quiet order, nonchecking-quiet
exclusion, checked-node all-evasion behavior, stand-pat, narrow fail-soft cutoff,
actual-ply mate distance, terminal precedence, no history updates, null excluded
policy state, cancellation/budget/thread interruption, absolute-ply safety,
determinism, clean/diagnostic parity and baseline retention.

A real-history repetition fixture starts from
`7k/8/8/8/8/8/8/rK6 w - - 2 2`, then makes seven moves of the repeated
`b1b2 a1a2 b2b1 a2a1` cycle. The root is not drawn. Its first added quiet check
`a2a1` reaches threefold; the candidate cuts at draw score 0 in window [-37,0],
while stand-pat is -37. The drawn child independently matches the oracle. This
demonstrates a valid draw bound, not a claim of the root's globally exact value.

**Semantic corpus.** [CORPUS.csv](SEARCH_QUIESCENCE_CHECKS_SR001E_2026-09-28_CORPUS.csv)
contains the established 42 fixtures plus 11 targeted rows: 53 rows, 52 unique
FENs (the fresh-clock quiet-mate fixture intentionally repeats an existing case).
The candidate completed 27, hit one million nodes on 20, and reached absolute
ply 256 on six. Of the old 42, all 17 completed values were unchanged; the other
25 yielded no completed value. Ten of the 11 added rows completed.
All completed PV moves were legal; each completed PV prefix was re-searched for
value consistency. Incomplete results are not reported as semantic differences.

The five completed score differences all begin with an added quiet check at
qply 0; baseline qPV is empty in each. Full FENs and counts are in the CSV.

| Fixture | Baseline -> quiet checks | Candidate qPV | Tactical consequence | Qnodes / max qply |
|---|---:|---|---|---:|
| quiet-direct-mate | 1146 -> 32767 | g6g7 | Mate in one | 12 / 2 |
| quiet-knight-fork | -752 -> 0 | f5d6 e8d7 d6f7 | Wins queen; remaining material is a draw | 12 / 3 |
| quiet-smother-sacrifice | 82 -> 32765 | e6g8 f8g8 h6f7 | Quiet queen sacrifice, forced mate | 571 / 20 |
| quiet-check-rule50-draw | -454 -> 0 | a1a8 e8d7 | Reaches rule-50 draw | 6 / 2 |
| endgame-pawn-displacement | 146 -> 241 | g2g4 h5h4 | Pawn check forces king displacement; material unchanged | 272 / 24 |

The bounded discovery/double-check/king/castling fixtures validate inclusion but
retain stand-pat as best. They establish neither frequency nor playing value.
No new completed promotion-access divergence was observed; promotion membership
is retained. Twenty-five old fixtures remain semantically unclassified for E
because they did not complete.

The fresh-clock direct-mate position
`7k/8/5KQ1/8/8/8/8/8 w - - 0 1` reaches the million-node budget at max qply 100.
A focused test records root moves and proves `g6g7#` was not visited before the
stop. Earlier generated checking branches consume the budget. The same board
with halfmove clock 98 completes and finds mate. This is a practical failure to
deliver an available resource, not a wrong completed score or an ordering change
proposed by this unit.

**Benchmark method.** Java 21, HCE, normal Search TT off, unchanged 1,000,000-node
budget, existing seven-position FEN suite and harness. JVM flags remain
`-Xms256m -Xmx256m -Xbatch`. Primary runs use 12 warmups and seven repetitions,
rotating mode order, upper-median timing; semantic/PV verification is outside
timing. Depths 2 and 3 include all seven positions. Only endgame and
transposition-pawns completed candidate depth 3, so only those were attempted at
depth 4. No budget was increased.

[BENCHMARK.csv](SEARCH_QUIESCENCE_CHECKS_SR001E_2026-09-28_BENCHMARK.csv) retains
all 48 primary rows plus 21 additional matched-position timing rows. Every row
has FEN, mode, requested/completed depth, best move, score, PV, normal/q/total
nodes, max qply, elapsed median, NPS, budget and completion reason. Incomplete
row score/PV fields are the existing harness's incomplete-result fields, not
completed search evidence; use `completed`/`completion_reason`.

The initial matched depth-2 timing misleadingly suggested a speedup: the tiny
start baseline alone measured 1.161 ms. A separate matched-only run with 128
warmups and 15 repetitions removed that sign reversal. All 21 additional trees
match the primary trees. Both datasets are retained as `primary` and
`matched_warmed`; no favorable timing subset is substituted silently. The
summary below uses the stronger-warmup dataset. JVM startup/JIT sensitivity is
consistent with the observation, but no profiler established the precise cause.

| Normal depth / mutually completed set | Qnodes baseline -> E (change) | Total nodes baseline -> E (change) | Summed median ms baseline -> E | Wall change | Max qply baseline -> E | Score / best-move differences |
|---|---:|---:|---:|---:|---:|---:|
| 2 / start, endgame, pawns (3/7) | 223 -> 1,157 (+418.8%) | 269 -> 1,203 (+347.2%) | 0.099 -> 0.420 | about +324% | 4 -> 26 | 1 / 1 |
| 3 / endgame, pawns (2/7) | 463 -> 1,813 (+291.6%) | 542 -> 1,902 (+250.9%) | 0.135 -> 0.587 | about +335% | 4 -> 25 | 1 / 1 |
| 4 / endgame, pawns (2/2 eligible) | 2,387 -> 58,653 (+2357.2%) | 2,901 -> 59,183 (+1940.1%) | 0.838 -> 19.616 | about +2241% | 5 -> 133 | 1 / 1 |

These are sums of per-position medians, not one aggregate stopwatch sample.
Milliseconds in CSV are rounded to three decimals; percentage differences from
the harness's unrounded aggregate are noticeable on the tiny shallow sets.
Raw NPS is recorded, not interpreted as strength or improved Search.

| Depth | E completed | Node-budget stops | Absolute-ply stops | All attempted E qnodes | Maximum qply |
|---|---:|---|---|---:|---:|
| 2 | 3/7 | kiwipete, tactical, evasion | middlegame | 3,016,849 | 254 |
| 3 | 2/7 | start, kiwipete, tactical, evasion | middlegame | 4,017,513 | 253 |
| 4, eligible only | 2/2 | None | None | 58,653 | 133 |

CONTROL and QSEARCH_BASELINE completed all attempted primary rows. Their 32
records exactly match D's completed flag, score, best move, PV, normal/q/total
nodes and max qply. E's endgame best move changes from `b4f4` to `b4c4` at all
three depths; scores change 165/167/155 to 173/173/173. Pawns remain unchanged;
start depth 2 also remains unchanged. For endgame+pawns, E qnodes scale
1,059 -> 1,813 -> 58,653 across depths 2/3/4; the depth-4 endgame is the main
growth source (58,127 qnodes, max qply 133).

[WITNESSES.csv](SEARCH_QUIESCENCE_CHECKS_SR001E_2026-09-28_WITNESSES.csv) traces
the endgame root change through alternative continuation
`b4c4 h5c5 c4f4 h4h5` to the same position used by the pawn-displacement fixture.
The relevant new `g2g4+` occurs at qply 2/1/0 for normal depth 2/3/4, respectively.
It is a resource in an alternative line, so it need not appear in the final root
PV. Independent continuation searches yield 146 versus 241. No material change
occurs in `g2g4 h5h4`; numerical improvement alone is not a playing-strength claim.

**Separate diagnostics.** Optional primitive counters and a reusable ply array
exist only for research diagnostic instances. Clean timing disables them. A
diagnostic-only legal-quiet scan counts nonterminal nonchecked qnodes with at
least one quiet check, even if stand-pat/tacticals later cut. That extra make/check
work is excluded from clean timing. All 16 diagnostic trees matched clean rows.
[DIAGNOSTICS.csv](SEARCH_QUIESCENCE_CHECKS_SR001E_2026-09-28_DIAGNOSTICS.csv)
retains per-position counters, including partial work on incomplete searches.

| Depth | Nodes with checks, all attempts | Quiet checks searched | Quiet-check beta cutoffs | Longest chain | Matched-complete searched / cutoffs |
|---|---:|---:|---:|---:|---:|
| 2 | 798,464 | 1,578,103 | 18,112 | 50 | 324 / 17 |
| 3 | 1,299,676 | 1,855,868 | 22,263 | 50 | 432 / 27 |
| 4, eligible only | 17,462 | 21,244 | 2,001 | 36 | 21,244 / 2,001 |

Here a chain counts added quiet checks connected only through quiet evasions:
quiet evasions preserve the count, tactical edges reset it. A counterchecking
quiet evasion does not itself increment it; a quiet pawn check does. This
explicit observed statistic is not an execution cap or all-check run length.
Cutoffs prove useful alpha-beta bounds; their count does not establish strength.

**Pathological paths and safety.**
[SAFETY.csv](SEARCH_QUIESCENCE_CHECKS_SR001E_2026-09-28_SAFETY.csv) preserves two
complete 256-ply witnesses with their halfmove-clock reset plies. Tests replay
every edge for legality and candidate admission and verify no expanded parent
or final node already had an authoritative terminal/draw result.

* Middlegame depth 2 stops after 15,701 total nodes, qply 254. At actual ply 256
  the halfmove clock is 16 and the position has occurred twice, not three times.
  Captures/clock resets at actual plies 119, 219 and 240 explain why the long
  checking path has not reached rule 50.
* The nonchecking-clearance corpus fixture stops after 1,927 nodes at qply 256.
  Its final checked position has halfmove clock 70 and one occurrence; reset
  plies include 1, 101 and 186. Legal evasions remain, so no static score is
  fabricated.

Thus these witnessed long tails are compatible with current draw semantics;
they did not expose a repetition/terminal correctness blocker. The existing
absolute safety interruption remains necessary. No inferred new cycle rule was
introduced.

**Validation actually run and reproduction.** Search-scoped regression passed
in 45 seconds, covering existing static/exact, A/B/C/D/oracle, harness, driver
and SEE tests. After the final fixture/test additions, the focused run passed
33 tests in two suites (12 quiet-check tests and 21 harness tests), zero failures
or errors, in 11 seconds. Corpus, diagnostic parity, benchmark determinism,
baseline freeze, seven oracle comparisons and both safety witnesses passed.
`git diff --check` is part of final validation.

```powershell
$env:DEBUG=''
.\gradlew.bat :app:test -Pheadless --tests 'com.ohinteractive.seedv6.search.exact.*' --tests 'com.ohinteractive.seedv6.tools.search.ExactSearchHarnessTest' --tests 'com.ohinteractive.seedv6.search.driver.*' --tests 'com.ohinteractive.seedv6.core.SeeThresholdTest' --tests 'com.ohinteractive.seedv6.core.SeeGeneratedLegalTest'
.\gradlew.bat :app:test -Pheadless --tests '*QuiescenceQuietCheckResearchTest' --tests '*ExactSearchHarnessTest'
.\gradlew.bat :app:exactSearch '-PsearchArgs=--leaves=sr001e --tt=off --position=start,kiwipete,endgame,tactical,evasion,middlegame,transposition-pawns --depth=2 --warmups=12 --repetitions=7 --node-limit=1000000'
java -Xmx256m -cp 'app/build/classes/java/main;app/build/classes/java/test' com.ohinteractive.seedv6.search.exact.QuiescenceQuietCheckCorpus
java -Xmx256m -cp 'app/build/classes/java/main;app/build/classes/java/test' com.ohinteractive.seedv6.search.exact.QuiescenceQuietCheckCorpus --safety
java -Xmx256m -cp 'app/build/classes/java/main;app/build/classes/java/test' com.ohinteractive.seedv6.search.exact.QuiescenceQuietCheckCorpus --witnesses
```

Repeat the primary harness command at depth 3; depth 4 uses only
`--position=endgame,transposition-pawns`. Matched warmed commands use
`--warmups=128 --repetitions=15`, with start/endgame/pawns at depth 2 and
endgame/pawns at depths 3/4. Diagnostic commands use
`--quiet-check-diagnostics --warmups=0 --repetitions=1`: the three-mode comparison
at depths 2/3 and individual `--leaves=quiet-checks` at depth 4. CSV rows retain
the exact FENs and timing conditions.

**Scope, remaining unknowns and final boundary.** Slow NNUE/training, GUI/browser,
unrelated full-suite tests and long strength/self-play matches were deliberately
not run: they do not validate this isolated headless move-set experiment. Depth 4
was skipped for the five candidate depth-3 failures; known baseline Kiwipete
depth-4 budget failure was not rerun. No higher depths, budget increases, large
tree exhaustive enumeration, JVM/GC/allocation/cache profiling, policy
combinations or ordering/selectivity experiments were performed. Allocation
observations are source inspection, not profiling evidence.

Important quiet mates, sacrifices, material-saving and draw resources exist in
the targeted evidence. Their prevalence in realistic play is unknown. The tested
unrestricted execution also prevents many bounded requests from returning any
answer. This evidence supports posing a later selective quiet-check question,
but does not decide it or infer that all quiet checks should be included/excluded.
Future move-set/selectivity comparisons must state whether these resources are
representable and retain clock-reset/checking-tail fixtures; capture-only
selectivity cannot recover moves absent from its move set. No conclusion about
playing strength follows from these headless measurements.

Changed paths are ExactSearch, ExactSearchHarness, QuiescenceOracle,
ExactSearchHarnessTest, new QuiescenceQuietCheckResearchTest and
QuiescenceQuietCheckCorpus, this report and its five CSVs, plus helper-owned
VERSION_STATE finalization. Both canons, Board/Gen, SearchKey and TTable mechanics
remain untouched. E is left uncommitted with nothing staged; D's focused commit
is the retained boundary. No reset, stash, rewrite or push occurred.
The runtime research change qualifies for a build bump from inherited build 20;
the maintained finalizer's verified outcome is reported in the completion and
VERSION_STATE, rather than presumed here. Root CODEXLOG_CURRENT.md is absent and
was not created.

Human actions required after this prompt: None for completing this research unit.
GPT reconciliation remains the authority gate before any later acceptance,
rejection, restriction, combination, production adoption or canon maintenance.
