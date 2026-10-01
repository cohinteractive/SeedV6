# SR-004 unit two: bounded probes and directional bracketing

Research evidence only. No SR-004 disposition or production adoption is assigned.
Contract R018 and Frontier F013 remain unchanged. This continues the first-unit
report `SEARCH_MTD_SR004_2026-09-29.md` and its committed raw archive.

## Verified starting state and boundaries

Clean `main` at `3607f455a0c9c348459c7c1fb69cd7e485e80f10`, five commits ahead
of local `origin/main`; version 0.0.0 build 23. Comparison with `475fbde`
confirmed the first unit changed no production source, either master, or
version bytes. No push or remote query was performed. Local tracking state
and this conversation establish the unpushed handoff, not a fresh remote audit.

The maintained finalizer/recovery hashes and source commit matched the supplied
canonical binding; `begin` succeeded before mutation. This remains a test-only
unit with a `no_bump` decision. Root `CODEXLOG_CURRENT.md` is absent, so no
journal was created. Version coordination is independent of the research result.

All changes are in the existing test research driver, harness, tests, analyzer,
Gradle task description, and second-unit report/evidence archive. No recursive
Search, production selection/configuration, PVS, TT policy, SearchKey,
hash-move semantics, quiet-history lifecycle, ordering, static leaves,
mate normalization, cancellation/publication or time-management code changed.
No aspiration, score prediction, pass-cap sweep or score-width sweep was added.

## Phase 1: retained traces establish the bracketing premise

`python tools/analyze-search-mtd.py --diagnose SEARCH_MTD_SR004_2026-09-29_EVIDENCE.zip`
reads the original committed evidence, without rerunning Search.
`phase1.json` retains all per-target patterns and numerical results. Counts
below use 171 distinct positive-depth MTD targets (depth >=2) once; work/time
estimates use all three old timing repetitions.

Signed error is previous exact score minus target exact score, in Search units.
There are **86 positive, 70 negative, 15 zero** errors. The signed range is
[-31,251,+31,652], quartiles -51/+91, median +1, mean +32.29. Quartiles use
sorted indexes floor(n/4) and floor(3n/4). Mate discovery dominates the extreme
raw errors; these are not centipawns. Exact-score recurrence is **15/171 (8.77%)**.

| Absolute error diagnostic | Targets | Proportion |
| --- | ---: | ---: |
| 0 | 15 | 8.77% |
| exactly 1 | 1 | 0.58% |
| <=4, inclusive | 26 | 15.20% |
| <=16, inclusive | 44 | 25.73% |
| <=64, inclusive | 83 | 48.54% |
| >64 | 88 | 51.46% |

These overlapping diagnostic bins do not select thresholds or policies.

| Error sign | Targets | Mean passes | Median | Maximum |
| --- | ---: | ---: | ---: | ---: |
| Positive (guess too high) | 86 | 31.16 | 5 | 342 |
| Negative (guess too low) | 70 | 19.47 | 6 | 272 |
| Zero | 15 | 2 | 2 | 2 |

All **171/171** sequences have exactly one classification switch, at the final
pass: 86 are UPPER/UPPER/.../LOWER, 85 LOWER/LOWER/.../UPPER (including exact
guesses). No oscillating/alternating pathology was observed. Of 4,073 zero
passes, 2,679 fail low and 1,394 fail high. Of the 119 sequences longer than two,
58 start low and 61 start high; the longer low sequences dominate pass count.

Fail-soft distance beyond the cutoff boundary is `score-beta` for fail-high and
`alpha-score` for fail-low. It is exactly zero in 2,607/4,073 passes (64.01%),
<=1 in 3,044 (74.74%), <=4 in 3,491 (85.71%), <=16 in 3,847 (94.45%),
and <=64 in 3,997 (98.13%). Median is zero, upper quartile two. Large jumps
exist (maximum 31,576), but ordinary traces commonly move almost one score
unit per pass. Fail-soft evidence does not reliably skip the score-space walk.

**52/171 (30.41%)** converge in the first two passes, including all 15 exact
guesses and 37 non-exact guesses helped by fail-soft jumps. The remaining
119 targets had already spent **18,786,345 nodes / 12.0279 s** in their first
two probes: 12.64% of their eventual zero-pass nodes, but 43.31% of matched
CONTROL iteration nodes. This is retained-trace cost, not a prediction of
fallback cost after different prior iterative TT histories.

Start d6/d7/d8 need 203/272/334 passes, middlegame 129/203/234, and endgame
d6/d7/d8 101/40/342. Their errors alternate with depth, but each individual
sequence is one-sided until its last pass. Example start d8: guess 204,
exact -134, 333 successive fail-lows followed by the final fail-high.

**The requested directional premise is supported**, so both variants were
implemented. No Search or TT change was needed to perform this diagnosis.

## Two research mechanisms

`MTD_TWO_PASS` uses the previous completed exact score, with the same ordinary
MTD beta rule and external bound interval as PREV. It allows at most two
completed zero-window calls. Proven equality triggers the existing
`[V-1,V+1]` exact-result materialization. Otherwise a single full-window call
uses the same TT/generation at that depth, and supplies the authoritative
result directly, with no extra materialization. Two is justified by the
exact-guess experiment, not fitted by a sweep. This bounds probe-call count;
it does not promise a constant-factor bound on Search-tree or wall-time cost.

`MTD_BRACKETED` first makes the ordinary previous-score probe. If it proves a
LOWER, move upward; if it proves an UPPER, move downward. Distance starts at
one and doubles after each further same-direction result. The distance is
measured from the **latest Search-proven bound**, including any fail-soft leap:

- upward: `beta = min(U, L + distance)`;
- downward: `beta = max(L + 1, U - distance + 1)`.

The negative-direction `+1` makes distance one probe `[U-1,U]`, immediately
adjacent to the proved upper value. Thresholds themselves never become bounds.
The first opposite Search result establishes the local bracket. Subsequent
calls use `beta = L + (U-L+1)/2` (integer division), splitting the remaining
inclusive integer interval. Each completed pass strictly shrinks `[L,U]`.
Domain extrema alone do not activate local bisection. Equality may of course
finish earlier when Search evidence meets a domain extremum.

All windows are `[beta-1,beta]`; final exact-result materialization is unchanged.
Score guards precede arithmetic; outward sums use `long`, midpoint differences
fit within the verified 65,537-score domain, and distance saturates at its full
65,536 span. Ordinary and mate bands retain their numeric ordering and existing
root/path-ply representation. Infinities and the invalid sentinel are not
scores. At most one initial, 17 outward and 16 bisection probes suffice even
with minimally informative truthful bounds; this is a domain-derived ceiling,
not an experimental pass cap.

Both use full-window depth 1, fresh matched default TTs per request, one
generation through all iterations/passes and the existing invocation-local
quiet-history reset. Cancellation in a probe, outward pass, bisection,
fallback or materialization makes the target incomplete. Stops between calls
do likewise; only the prior exact iteration remains authoritative.

## Validation and measurement

**70 tests passed**, zero failures: MtdResearchTest (12), AspirationResearchTest
(9), PvsSearchTest (12), ExactSearchTTableTest (18), SearchDriverTest (10),
SearchDriverTTableTest (4), TranspositionScoresTest (5). The initial focused
run found a test-fixture assumption: start d2 converges during bracketing and
does not bisect. The cancellation test now selects a depth proven to bisect,
then interrupts every actual phase and exhausts budgets exactly between calls.
No implementation semantic defect was found.

Coverage extends the existing 33-position shallow TT-off score and exact-PV
checks to both policies; tests cover all adjacent integer scores, mate-band
edges, safe outward/midpoint arithmetic, doubling from fail-soft bounds,
both search directions, actual bisection, mate distance, real repetition
history, halfmove-99 context, same-generation reuse, full fallback versus
materialization, cancellation and previous-completion retention. Full-depth
correctness gates precede timing; every returned PV prefix is independently
verified with the existing TT-off shallow / fresh-TT deeper suffix infrastructure.

Same 33-position manifest, requested depth 6 with the same eight extended to
8, deterministic single-thread HCE, Windows 11 / Ryzen 5 5500 / OpenJDK 21,
default 64 MiB-requested Search TT. JVM flags remain `-Xms512m -Xmx2g -Xbatch
-Djava.awt.headless=true`. Two depth-5 warmup rounds on the eight deeper
positions now include all five strategies. Three timing repetitions rotate
all five policies by position and repetition. Terminal roots stop at depth 1.

As before, TT/owner construction, output, CONTROL-oracle acquisition and PV
validation are outside timed requests. All passes, root-entry/reset work,
fallback/materialization, result/observer handling are included. Primary nodes
are production admitted children; raw recursive visited nodes are retained too.
Separate observation-only TT runs precede clean timing. Mechanical TT hits are
not labeled as depth-applicable cutoffs. No candidate was stopped or suite reduced.

```powershell
$env:DEBUG=''
.\gradlew.bat :app:test -Pheadless --tests 'com.ohinteractive.seedv6.search.exact.MtdResearchTest' --tests 'com.ohinteractive.seedv6.search.exact.AspirationResearchTest' --tests 'com.ohinteractive.seedv6.search.exact.PvsSearchTest' --tests 'com.ohinteractive.seedv6.search.exact.ExactSearchTTableTest' --tests 'com.ohinteractive.seedv6.search.driver.SearchDriverTest' --tests 'com.ohinteractive.seedv6.search.driver.SearchDriverTTableTest' --tests 'com.ohinteractive.seedv6.search.tt.TranspositionScoresTest' --console=plain
.\gradlew.bat :app:mtdResearch --console=plain '-PresearchArgs=--output=build/sr004b --depth=6 --deep-depth=8 --repetitions=3 --warmups=2'
python tools/analyze-search-mtd.py --diagnose SEARCH_MTD_SR004_2026-09-29_EVIDENCE.zip
python tools/analyze-search-mtd.py build/sr004b
```

Full/slow suites, GUI/browser work, neural-evaluator benchmarks, self-play/Elo,
deeper exhaustive TT-off coverage, parameter sweeps and changes to TT/history
policy were deliberately omitted. They are outside this bounded driver unit.

## Completed aggregate measurement

All **1,020 full-depth exact-result/PV checks** passed before timing (204 per
strategy). Every timing repetition reproduced the diagnostic run's scores,
moves, PVs, nodes, guesses, thresholds, bounds and pass sequence. TWO-PASS
has one different exact PV from CONTROL, BRACKETED four; neither changes any
best move. There were no mate, path-history, PV or cancellation issues.

Independent comparison with the first-unit ZIP confirmed **all 15,081 old-policy
pass rows and 1,836 iteration rows** match in every deterministic field,
excluding fresh timing/NPS fields. Thus CONTROL/PREV/ORACLE retained their
original trees despite the expanded harness. Request/iteration/pass node sums
agree; the final completion marker and archive integrity were verified.

The table sums **all three repetitions**, 99 requests and 612 completed
iterations per strategy, including every iterative depth and all completion work.

| Strategy | Nodes | Seconds | Node change vs CONTROL | Time change | NPS |
| --- | ---: | ---: | ---: | ---: | ---: |
| CONTROL | 43,851,261 | 28.6369 | baseline | baseline | 1,531,283 |
| MTD-PREV | 150,478,128 | 84.1179 | +243.156% | +193.739% | 1,788,896 |
| MTD-ORACLE | 37,609,530 | 23.9983 | -14.234% | -16.198% | 1,567,174 |
| TWO-PASS | 55,023,777 | 35.6889 | +25.478% | +24.625% | 1,541,761 |
| BRACKETED | 57,095,379 | 35.6728 | +30.202% | +24.569% | 1,600,528 |

Per-repetition seconds: CONTROL 9.4186 / 9.7267 / 9.4917; PREV 27.3964 /
28.7304 / 27.9911; ORACLE 7.9400 / 8.0970 / 7.9613; TWO-PASS 11.7572 /
11.7351 / 12.1967; BRACKETED 11.8238 / 11.9263 / 11.9227. TWO-PASS regressed
20.65-28.50% elapsed in each repetition; BRACKETED 22.61-25.61%. ORACLE
improved 15.70-16.75% each time. These effects exceed the observed sample
variation. Absolute timings changed from unit one, so all comparisons use
this run's CONTROL, not historical seconds.

## TWO-PASS: bounded call count, costly failed-probe stage

**52/171 targets (30.41%)** prove exactness in two passes; **119/171 (69.59%)**
fall back. The target membership matches Phase 1: all 15 exact guesses are
in the successful group; 37 wrong guesses also converge in two. Every target
uses exactly two probes plus exactly one exact completion call.

| Phase (all repetitions) | Calls | Nodes | Seconds |
| --- | ---: | ---: | ---: |
| Depth-1 full | 99 | 1,668 | 0.0034 |
| Zero-window probes | 1,026 | 19,262,898 | 12.8666 |
| Full-window fallback | 357 | 35,753,649 | 22.7993 |
| Successful-convergence materialization | 156 | 5,562 | 0.0029 |

The residual 0.0167 s versus summed pass times includes driver, observer,
record handling and timing noise. Normal root-entry/reset work is inside
each timed pass; it is not excluded by this residual calculation.

| Target group | TWO-PASS nodes / seconds | Matched CONTROL nodes / seconds | Node / time change |
| --- | ---: | ---: | ---: |
| Exact in two (52) | 482,265 / 0.2083 | 475,401 / 0.2011 | +1.44% / +3.59% |
| Fallback (119) | 54,539,844 / 35.4655 | 43,374,192 / 28.4280 | +25.74% / +24.76% |

The successful group is only 1.08% of CONTROL's aggregate node work, and its
small elapsed difference is not strong timing evidence. Exact previous-score
recurrence is too sparsely placed in this workload to reproduce ORACLE's
aggregate savings. This group includes 37 non-exact first guesses and is not
equivalent to the all-depth ORACLE experiment.

For fallback targets, the probes alone cost **18,786,195 nodes / 12.6648 s**.
Fallback itself is **17.57% fewer nodes and 19.80% less time** than matched
CONTROL iterations; 83/119 fallback calls individually use fewer nodes.
Yet the probes spend more than the fallback saves. Their TT information has
demonstrable reuse, without a net aggregate benefit. This comparison includes
each strategy's own preceding iteration history; it is not a causal ablation
holding the entry TT identical or clearing it between probes and fallback.

Separate diagnostics show 598,705 mechanical hits in 2,199,396 fallback probes,
and 1,440/1,440 hits during successful materialization. All applicable score
reuse still obeys equal-depth/current-generation/cutoff-only rules; no changed
TT interpretation was used to make fallback or PV recovery cheaper.

## BRACKETED: the long progression is removed, total work remains higher

Across distinct targets, zero-window passes fall from PREV's **4,073 to 1,098**
(-73.04%): **849 bracketing** (including the initial probes), **249 bisection**.
Mean is **6.42**, median **5**, maximum **18**, versus PREV 23.82 / 6 / 342.
There are **584 fail-lows / 514 fail-highs**. Unlike the old one-sided traces,
opposite results within bisection are intentional proven-interval refinement.

Positive guess-error targets average 6.60 passes (median 5, max 18), negative
7.14 (median 6, max 18), zero 2 (all exactly two). Directional asymmetry in
the former hundreds-of-pass counts is largely removed, but pass cost is not
uniform. Start d8 falls 334->18, middlegame d8 234->17, endgame d8 342->18.

| Phase (all repetitions) | Calls | Nodes | Seconds |
| --- | ---: | ---: | ---: |
| Depth-1 full | 99 | 1,668 | 0.0034 |
| Bracketing, including initial | 2,547 | 39,238,881 | 24.7847 |
| Bisection | 747 | 16,601,499 | 10.2296 |
| Materialization | 513 | 1,253,331 | 0.6465 |

Materialization is 2.20% of nodes / 1.81% of elapsed time. Removing that cost
would still leave a clear regression. BRACKETED reduces PREV's nodes **62.06%**
and time **57.59%**, but uses **51.81% more nodes / 48.65% more time** than
ORACLE, and the aggregate CONTROL regressions shown above remain.

The full pass CSV preserves work by threshold in order. Representative start
d8: the first two passes cost 438,332 and 1,137 nodes. Outward thresholds then
reach beta -307, producing LOWER -307 against UPPER -52. Bisection uses beta
-179, -115, -147, -131, -139, -135, -133, -134 before equality at -134.
The beta -115 pass alone costs 701,234 nodes and beta -147 costs 460,129;
materialization costs 129,333. Fewer thresholds do not mean that each threshold
is cheap. Middlegame d8 has a 2,537,408-node first probe and a later
1,040,267-node bisection pass; endgame d8 completes the same 18-pass pattern
with much smaller per-pass work. No hundreds-of-pass sequence remains.

Diagnostic bracketing hits/probes: 1,031,413/4,565,579; bisection:
877,538/1,736,628; materialization: 206,727/241,328. These show retained
memory is being consulted, not that every mechanical hit is a cutoff.

## Concentration, exceptions and throughput

| Position (depth) | CONTROL nodes/run | TWO-PASS nodes/run | BRACKETED nodes/run | TWO-PASS time change | BRACKETED time change |
| --- | ---: | ---: | ---: | ---: | ---: |
| start (8) | 2,379,876 | 3,192,276 | 4,237,151 | +32.40% | +69.95% |
| middlegame (8) | 6,037,108 | 8,986,733 | 8,087,323 | +49.38% | +30.43% |
| Kiwipete (8) | 4,344,917 | 4,226,024 | 4,589,139 | -6.12% | -1.55% |
| endgame (8) | 82,548 | 104,090 | 201,198 | +28.18% | +125.85% |
| evasion (8) | 800,372 | 832,936 | 855,484 | +3.78% | +5.83% |
| tactical (8) | 159,034 | 125,556 | 148,696 | -26.61% | -15.36% |

Times combine three samples. Kiwipete BRACKETED increases nodes 5.62% yet
shows slightly lower wall time; that small time difference is not convincing
in isolation. TWO-PASS improves nodes on 2/33 positions (Kiwipete and tactical),
BRACKETED on 3/33. Tiny positions are retained but do not carry strong timing
claims. Quiet-endgame also regresses nodes 42.12% / 165.95% respectively.

Start plus middlegame explain **101.02%** of TWO-PASS's excess nodes (other
positions net a small saving), and **88.51%** of BRACKETED's. Excluding start
only, TWO-PASS remains +23.79% nodes / +23.17% time and BRACKETED +20.90% /
+16.09%. Excluding both, TWO-PASS is **-0.61% nodes / -3.61% time** and
BRACKETED **+8.18% / +1.44%**. The full-suite loss is real but concentrated;
there is no claim that every position or arbitrary sub-suite loses equally.

TWO-PASS throughput is 0.68% higher than CONTROL; BRACKETED 4.52% higher.
BRACKETED's modestly higher node count than TWO-PASS produces essentially
the same measured elapsed time. Request-minus-pass residuals are only about
0.017 / 0.009 s. These data point to Search work, rather than obvious external
driver overhead, as the main economic cost. Entry/reset work inside passes
and TT-hit/tree-shape differences remain included and are not causally isolated
as separate per-entry or per-node costs.

## Evidence boundary and repository outcome

The current API safely supports both mechanisms; no new semantic prerequisite
or required architectural repair was discovered. Economics remain conditional
on the existing history-sensitive key, cutoff-only single-entry evidence,
replacement and per-invocation quiet-history reset. In particular, same-key,
same-depth, same-type stronger bounds can be rejected by the unchanged
`TTable.save` rule. Neither this unit nor its hit counters isolate that rule
or history reset as the cause of the remaining regression. These observations
do not authorize reopening or changing either policy.

The perfect-guess comparison still has headroom in the matched run. The two
practical policies substantially reduce unrestricted PREV's work, but both
lose aggregate wall time to CONTROL in every repetition. The bounded probe
stage fails economically because useful cheaper fallback does not repay the
probe cost, while logarithmically bounded score discovery still asks expensive
Search questions. Exact-result/PV recovery remains a small component.

`SEARCH_MTD_SR004B_2026-09-29_EVIDENCE.zip` preserves phase-1 diagnosis, original
baseline identity check, configuration/FEN manifest, all correctness and TT
diagnostic rows, every measured pass/iteration/request, completion marker and
JSON/CSV summaries. Uncompressed outputs remain under ignored `build/sr004b`.
The first-unit report/archive are preserved byte-for-byte. This unit is
research/test-only with no application build impact; the completion reports
the actual finalizer result and focused commit. No push is authorized or made.

The evidence above separates the promised two-pass opportunity, fallback
economics, directional convergence improvement, remaining workload cost and
concentration. It supplies GPT/user with the basis to decide whether a
specific further SR-004 unit is justified or the workstream has enough
evidence for closure. It makes no programme disposition or production-adoption
decision and requires no Project Source refresh.

Human actions required after this prompt: None.
