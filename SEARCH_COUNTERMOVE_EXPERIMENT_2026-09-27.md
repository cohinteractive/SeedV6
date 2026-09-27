# SR-016 compact countermove experiment — 2026-09-27

Recommendation: **COUNTERMOVE NOT JUSTIFIED**. Main quiet history alone remains
the provisional SR-016 baseline. The separately selectable candidate is retained
as reproducible research evidence. This completes a bounded experiment, not
Search acceptance or production adoption.

The evidence now appears sufficient for GPT/user to close SR-016 with plain main
quiet history. This recommendation does not change programme status: Search
Contract **R009** and Research Frontier **F004** were verified unchanged; SR-015
remains ACCEPTED, SR-016 and SR-017 remain PENDING. No canon/frontier maintenance
was performed.

Starting worktree: clean at `772fb95c8bbfff561bb90e0ff2c0aca05fb61f94`.
Main quiet history from `79670d4` remains unchanged. Prior continuation and killer
modes remain independently selectable, with their meanings preserved.

## Implementation and files

Relative to the existing main/test Java package `com/ohinteractive/seedv6`:

- `search/exact/ExactSearch.java`: candidate-only countermove storage/reset,
  immediate previous-move context, cutoff storage and quiet promotion.
- New `search/exact/Countermoves.java`: compact context and newest-reply primitive.
- `tools/search/ExactSearchHarness.java`: standalone/paired option and help.
- New `search/exact/CountermovesTest.java`: 11 focused context, learning, ordering,
  lifecycle and semantic/oracle tests.
- `search/exact/ExactSearchOrderingTest.java`: new mode added to the existing
  ordering/oracle matrices, preserving historical visitation assertions.
- `CaptureHistoryTest.java`, `QuietHistoryTest.java`, `ContinuationHistoryTest.java`,
  `KillerMovesTest.java`: reflection calls supply the additional primitive context
  argument; historical assertions are unchanged.
- `tools/search/ExactSearchHarnessTest.java`: standalone/pair and repeated TT-off/on
  coverage.
- This report and root `VERSION_STATE.txt` complete the work unit.

No production driver, evaluator, SEE, TT implementation, generation, move
application, main-history primitive, old experiment primitive or old report changed.

A: `see-material-quiet-history`.
B: `see-material-quiet-history-countermove`
(`SEE_MATERIAL_QUIET_HISTORY_COUNTERMOVE`, mode 9).

All established constructors still select **CONTROL**. Every previous mode and
alias remains intact. Candidate B has main history and countermoves only; its
continuation-history, killer and capture-history tables are null.

## Context, representation and lifecycle

The existing recursive `long previousMove` argument is reused without changing
recursion: root passes zero; a child receives the exact full move just played.
No path object, new per-ply stack or second previous-move representation is added.

For nonzero previousMove:

```text
piece = promoted piece including side, if present; otherwise moving piece including side
compact(piece) = (piece & 7) - 1 + (piece >>> 3) * 6
context = compact(piece) * 64 + previous destination square
```

Verified white piece codes 1..6 map to 0..5, black 9..14 to 6..11. Countermoves
reuse only `ContinuationHistory.compactPiece`, the existing pure arithmetic
mapping; no continuation table, scoring or update is enabled. The mapping needs
bit operations, subtraction and multiplication, with no lookup table. Contexts
are 0..767. Previous captures, EP, quiets and castling all qualify; promotions use
the promoted piece actually occupying the destination. Previous source square,
victim, score, TT state, ply and earlier ancestors are not part of the key.
Root context is -1 even when supplied real-game history contains previous moves.

Storage is one owner-local **long[768]**, **6,144 payload bytes (6 KiB)**, excluding
array header/alignment. Each value is one entire generated move, with no identity
normalization or truncation; zero is empty. Legal generated moves encode a
nonzero moving piece, so that sentinel is unambiguous. This is 192 times smaller
in payload than the prior 1,179,648-byte immediate-continuation table. Main
history remains its existing 262,144-byte int payload.

The candidate allocates its array once per ExactSearch owner. All nodes in one
fixed-depth invocation share it; independent owners do not. Every top-level
fixed-depth call clears both main history and countermoves before the first
cancellation checkpoint, including calls inside one request. Cancellation or
thread interruption clears both again before publishing an incomplete result.
There is no cross-invocation or iterative-deepening reuse.

## Learning, ordering and mechanics

After the existing post-child completion/cancellation check, a quiet beta cutoff
performs the unchanged main-history reward/malus update. With a valid immediate
context, the winning full move overwrites that context's countermove. Writing the
same move is idempotent. There is no secondary slot, score, gravity, aging or
depth restriction for countermoves.

Tactical cutoff moves, including EP and every promotion, never enter the table.
Failed quiets and generated-but-unsearched moves are not stored. Root cutoffs
still learn main history but cannot store a countermove. A searched quiet hash
cutoff is eligible, and a prior failed quiet hash receives the normal main-history
malus. Resolved TT evidence is not fabricated into searched-move learning.

Main history is unchanged: signed gravity bounded +/-16384, magnitude
`min(depth,64)^2`, positive reward for the winner and negative updates to earlier
searched quiets. No parameters were tuned.

Order remains legal hash first, descending immediate material within SEE >= 0
tacticals, descending material within SEE < 0 tacticals, then quiets. Full
`Gen.genAll` generation and the existing stable primitive history insertion pass
remain. Candidate B then stably promotes an exact matching countermove within the
generated quiet range using the existing direct primitive arraycopy helper.
It is a distinct quiet class, never a numerical history bonus.

Hash is already excluded from the quiet range and keeps precedence; a hash/reply
overlap occurs once. An absent, stale, wrong-full-identity or tactical stored value
cannot promote a quiet. No move is inserted into the move set. Every other quiet
keeps its main-history order and deterministic equal-history ties. At root, with
no context, ranking is exactly the main-history baseline.

Source and compiled `javap -p -c` inspection found no allocation, boxing,
collections, streams, comparator or strategy dispatch introduced on the node
path. Context calculation, store, ranking and promotion have no allocation
bytecodes. Negamax retains its pre-existing invalid-evaluator-score exception/
string path. Existing owner/top-level setup/result allocations remain; no
allocation-profiler claim is made. No SR-017 sorting/generation architecture or
prediction about a future handcrafted Sort cost is implied.

## Validation

```powershell
.\gradlew.bat :app:test --tests 'com.ohinteractive.seedv6.search.exact.*' --tests 'com.ohinteractive.seedv6.search.tt.*' --tests 'com.ohinteractive.seedv6.search.order.*' --tests 'com.ohinteractive.seedv6.tools.search.ExactSearchHarnessTest' -Pheadless=true --console=plain
```

**163 tests across 17 suites passed on the first run; zero failures, errors or
skips.** Includes 11 new countermove tests, main/continuation/killer/capture history
tests, ordering, ExactSearch, TT and harness tests.

The expanded ordering matrix makes **840 exhaustive score/optimal-move/PV
comparisons** (14 positions x depths 0..2 x ten modes x TT off/on). Counterspecific
depth-3 tests add ten candidate oracle comparisons across ordinary pawn play,
castling, evasions, and white/black promotion paths, also checking main-history
values. Historical main-history six-position depth-5 nodes/scores/best moves and
CONTROL/SEE deterministic visitation assertions pass unchanged.

Focused coverage includes all 768 context keys and side/destination distinctions;
source/victim exclusion; actual post-move piece checks including 16 promotions;
root exclusion despite real-game history; replacement/idempotence and context
isolation; quiet and tactical previous moves; actual nonroot quiet/hash cutoff
learning with unchanged main reward/malus; generated-but-unsearched exclusion;
actual EP/promotion cutoff exclusion and castling cutoff inclusion; owner/reset
isolation within one request; no candidate continuation/killer allocation; seeded
ranking with every legal hash, stale/absent/tactical countermoves, exact identity,
stable remaining ties and complete unique visitation; cancellation checks before
updates, later abort after valid learning, interruption and both-table cleanup;
terminal, draw, static and TT-resolved nodes. The existing semantic suite also
covers mate/stalemate/repetition, exact-depth/current-generation TT applicability
and incomplete results with no valid score/best/PV/root TT publication.

All six benchmark batches completed successfully. Every mode repeated its
score/best/nodes/PV deterministically across warmups and measurements. A/B exact
scores, best moves and PVs were identical in all measured cases, across both TT
settings and depth-6 repeats; equal-valued alternatives were permitted but not
needed. Existing harness TT-off CONTROL best-child/PV-endpoint re-search and legal
PV checks ran outside timing.

## Benchmark method

AMD Ryzen 5 5500, Windows 11 amd64, OpenJDK **21+35-2513**, HCE, one Search thread.
The unchanged Gradle JavaExec task supplies **-Xms256m -Xmx256m -Xbatch**.
Five warmups and seven measured repetitions per position/mode; alternating paired
execution order each round; fourth elapsed sample of seven is the median.
Every benchmark batch launches a fresh JVM. One fresh-JVM repeat of both depth-6
batches tests the modest initial effects.

Search wall time includes per-invocation main-history/countermove clearing and
Search setup; owner construction/FEN parsing and semantic verification are outside
timing. TT-on uses separate cold 4 MiB tables, cleared outside timed Search before
each invocation. TT-off has no table. Every measured candidate starts empty.

```powershell
.\gradlew.bat :app:exactSearch '-PsearchArgs=--position=ordering --depth=5 --warmups=5 --repetitions=7 --ordering=see-material-quiet-history,see-material-quiet-history-countermove --tt=off' --console=plain
.\gradlew.bat :app:exactSearch '-PsearchArgs=--position=ordering --depth=5 --warmups=5 --repetitions=7 --ordering=see-material-quiet-history,see-material-quiet-history-countermove --tt=on' --console=plain
```

For confirmation, replace `--position=ordering --depth=5` with
`--position=kiwipete,middlegame,evasion --depth=6`, run both TT settings and repeat
both commands once. No tuning or broader corpus was added.

A = main history, B = main history + countermove. Aggregate throughput is total
nodes divided by summed position medians, not averaged per-position NPS.
Summed milliseconds and approximate aggregate NPS below use printed three-decimal
medians; percentage changes come directly from the harness's unrounded
nanoseconds. Small last-digit rounding differences are not extra precision.

### Depth 5, TT off

| Position | Score | Best | A nodes | B nodes | A median ms | B median ms | A NPS | B NPS |
| --- | ---: | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| start | 197 | e2e3 | 31,418 | 29,487 | 17.084 | 16.145 | 1,839,041 | 1,826,431 |
| kiwipete | 403 | d5e6 | 83,934 | 83,934 | 57.845 | 57.703 | 1,451,010 | 1,454,586 |
| tactical | 1177 | e4d5 | 4,861 | 4,861 | 2.024 | 1.924 | 2,401,917 | 2,527,164 |
| evasion | -81 | c4c5 | 20,741 | 20,741 | 18.299 | 19.315 | 1,133,468 | 1,073,806 |
| middlegame | 250 | c3d5 | 141,150 | 139,853 | 96.794 | 96.050 | 1,458,245 | 1,456,046 |
| transposition-pawns | 6 | e1d2 | 1,563 | 1,563 | 0.371 | 0.399 | 4,216,347 | 3,916,311 |

Totals: nodes 283,667 -> 280,439 (**-1.138%**); summed medians 192.417 -> 191.536 ms (**-0.458%**); aggregate throughput approximately 1,474,230 -> 1,464,158 NPS (**-0.683%**).

### Depth 5, cold 4 MiB TT on

| Position | Score | Best | A nodes | B nodes | A median ms | B median ms | A NPS | B NPS |
| --- | ---: | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| start | 197 | e2e3 | 27,911 | 26,819 | 17.091 | 16.973 | 1,633,129 | 1,580,088 |
| kiwipete | 403 | d5e6 | 65,515 | 65,515 | 46.308 | 46.395 | 1,414,778 | 1,412,125 |
| tactical | 1177 | e4d5 | 4,280 | 4,280 | 1.275 | 1.333 | 3,357,652 | 3,210,080 |
| evasion | -81 | c4c5 | 19,834 | 19,834 | 15.709 | 15.729 | 1,262,572 | 1,260,990 |
| middlegame | 250 | c3d5 | 120,175 | 119,481 | 87.205 | 87.126 | 1,378,069 | 1,371,357 |
| transposition-pawns | 6 | e1d2 | 1,532 | 1,532 | 0.374 | 0.397 | 4,101,740 | 3,856,998 |

Totals: nodes 239,247 -> 237,461 (**-0.747%**); summed medians 167.962 -> 167.953 ms (**-0.005%**); aggregate throughput approximately 1,424,411 -> 1,413,854 NPS (**-0.742%**).

### Depth 6 confirmation, TT off

| Position | Score | Best | A nodes | B nodes | A median ms | B median ms | A NPS | B NPS |
| --- | ---: | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| kiwipete | -220 | e2a6 | 219,294 | 219,294 | 170.353 | 169.764 | 1,287,293 | 1,291,755 |
| middlegame | -126 | e2e1 | 564,514 | 571,605 | 406.874 | 411.834 | 1,387,440 | 1,387,950 |
| evasion | -787 | d2d4 | 157,868 | 157,845 | 110.141 | 109.827 | 1,433,330 | 1,437,220 |

Totals: nodes 941,676 -> 948,744 (**0.751%**); summed medians 687.368 -> 691.425 ms (**0.590%**); aggregate throughput approximately 1,369,974 -> 1,372,158 NPS (**0.159%**).

### Depth 6 confirmation, cold 4 MiB TT on

| Position | Score | Best | A nodes | B nodes | A median ms | B median ms | A NPS | B NPS |
| --- | ---: | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| kiwipete | -220 | e2a6 | 167,760 | 167,760 | 133.830 | 132.040 | 1,253,529 | 1,270,526 |
| middlegame | -126 | e2e1 | 482,640 | 490,098 | 356.106 | 366.546 | 1,355,328 | 1,337,070 |
| evasion | -787 | d2d4 | 99,849 | 99,830 | 71.817 | 72.347 | 1,390,319 | 1,379,873 |

Totals: nodes 750,249 -> 757,688 (**0.992%**); summed medians 561.753 -> 570.933 ms (**1.634%**); aggregate throughput approximately 1,335,550 -> 1,327,105 NPS (**-0.632%**).

### Depth 6 fresh-JVM repeat, TT off

| Position | Score | Best | A nodes | B nodes | A median ms | B median ms | A NPS | B NPS |
| --- | ---: | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| kiwipete | -220 | e2a6 | 219,294 | 219,294 | 169.370 | 171.862 | 1,294,759 | 1,275,991 |
| middlegame | -126 | e2e1 | 564,514 | 571,605 | 402.740 | 412.640 | 1,401,683 | 1,385,240 |
| evasion | -787 | d2d4 | 157,868 | 157,845 | 106.177 | 106.036 | 1,486,835 | 1,488,601 |

Totals: nodes 941,676 -> 948,744 (**0.751%**); summed medians 678.287 -> 690.538 ms (**1.806%**); aggregate throughput approximately 1,388,315 -> 1,373,920 NPS (**-1.037%**).

### Depth 6 fresh-JVM repeat, cold 4 MiB TT on

| Position | Score | Best | A nodes | B nodes | A median ms | B median ms | A NPS | B NPS |
| --- | ---: | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| kiwipete | -220 | e2a6 | 167,760 | 167,760 | 136.069 | 137.057 | 1,232,908 | 1,224,013 |
| middlegame | -126 | e2e1 | 482,640 | 490,098 | 355.636 | 365.705 | 1,357,116 | 1,340,146 |
| evasion | -787 | d2d4 | 99,849 | 99,830 | 71.218 | 72.086 | 1,402,027 | 1,384,873 |

Totals: nodes 750,249 -> 757,688 (**0.992%**); summed medians 562.923 -> 574.848 ms (**2.119%**); aggregate throughput approximately 1,332,774 -> 1,318,067 NPS (**-1.104%**).

### Verified common principal variations

These PVs match A/B, TT off/on and depth-6 repeat:

| Depth | Position | Common PV |
| ---: | --- | --- |
| 5 | start | e2e3 e7e5 d1f3 g8e7 f3f7 |
| 5 | kiwipete | d5e6 e7e6 e2a6 h3g2 f3f6 |
| 5 | tactical | e4d5 e6d5 d3g6 e8d8 g6c6 |
| 5 | evasion | c4c5 a3b4 c5b6 b2a1q d1a1 |
| 5 | middlegame | c3d5 e7d7 g5f6 g7f6 f3e5 |
| 5 | transposition-pawns | e1d2 e8d7 d2c3 d7d6 c3d4 |
| 6 | kiwipete | e2a6 b4c3 d2c3 e6d5 e5f7 h3g2 |
| 6 | middlegame | e2e1 h7h6 c3d5 c5f2 e1f2 f6d5 |
| 6 | evasion | d2d4 a3b4 a1b1 b6a7 h6f7 e8f7 |

## Interpretation and SR-016 recommendation

**Tree effect:** Depth-5 nodes decrease 1.138% TT-off / 0.747% TT-on. Start saves
6.146% / 3.912%, Middlegame 0.919% / 0.577%; the other four positions are unchanged.
At depth 6 the aggregate reverses to **+0.751% / +0.992%**, reproducibly. Middlegame
adds 1.256% / 1.545% nodes, Kiwipete is unchanged and Evasion saves only 23 / 19
nodes (0.015% / 0.019%). Thus context promotion does not consistently improve the tree.

**Mechanical effect:** The compact discrete signal is much cheaper in payload than
continuation history. Aggregate throughput changes are -0.683% / -0.742% at depth 5,
+0.159% / -0.632% at initial depth 6, and -1.037% / -1.104% on the repeat. These are
end-to-end measurements including lookup/store/promotion/reset and changed node
mixes; they do not isolate a precise per-operation cost. There is no large
continuation-table reset/cache footprint, but compactness alone does not earn a
place in the preferred policy.

**Wall-time effect:** Depth 5 improves only 0.458% TT-off and 0.005% TT-on (effectively
unchanged). Initial depth-6 time regresses **0.590% / 1.634%**, and the fresh-JVM
repeat regresses **1.806% / 2.119%**. Middlegame is slower in both repeats/settings;
Kiwipete has no node benefit and its small timing effect changes sign. Evasion's
tiny savings yield no material benefit and TT-on wall time regresses in both
depth-6 batches. The discrete countermove fails to provide useful marginal Search
improvement beyond main history under this protocol.

Recommendation: **COUNTERMOVE NOT JUSTIFIED**. Main quiet history alone remains the
provisional SR-016 baseline. No extra countermove tuning experiment is materially
required; retain the candidate mode for evidence and reproducibility.

### Is SR-016 ready to close?

**Yes, the current evidence appears sufficient for GPT/user reconciliation and
closure with plain main quiet history.** Main history has the retained favourable
evidence; immediate continuation, two killers and now compact countermoves have
each failed to justify a stronger preferred baseline under the bounded protocol.
The compact-context test also separates countermove's weak policy signal from the
large continuation table's footprint: simply making context storage cheap did not
produce a sustained gain.

Relative history could in principle normalize differing move opportunities, but
these experiments identify no concrete exposure-bias failure that gives it enough
distinct expected value to require another SR-016 unit now. Richer contextual
histories similarly remain unproven, not exhaustively disproved. No further
mechanism is implemented or automatically queued. Future specific evidence after
changes to Search depth, frontier policy or lifecycle could justify reopening a
bounded question; that possibility does not prevent closing the current research
decision. This assessment itself does not mark SR-016 ACCEPTED/IMPLEMENTED or edit
the authoritative frontier.

## Limits, deliberately skipped checks and completion boundaries

No playing-strength inference is made from fixed-depth timing. The corpus, depths,
HCE evaluator and one machine/JVM limit generalization. Main-history interactions
under persistent iterative-deepening state, other evaluators and later sorting/
generation mechanics remain unknown. Timing variability is especially visible in
short searches; repeated node counts are exact while wall-time magnitudes vary.

Deliberately not run: full/long-running suite, standalone SEE/perft suites (SEE,
generation and move application unchanged), GUI/UCI/production-driver integration,
NNUE/BRN, self-play/Elo matches, allocation/hardware-counter profiling, depth >6,
further repeats or parameter/replacement/persistence tuning.

No new continuation/killer combination, relative history, richer context, pruning,
reductions, staged generation, PVS, SR-017 framework, canon/frontier maintenance or
production adoption was performed. Normal validated-unit commit and version
finalization follow repository policy; nothing is pushed. No root CODEXLOG file
exists, and none is created.

Human actions required after this prompt: None.
