# SR-001 Phase 2 quiescence research

2026-10-09. **ACTIVE**, authorized feature-level CGLHW. This report records
research evidence and continuation, not a replacement Contract or Frontier.
Production remains static until a frozen candidate earns independent strength
confirmation and integration validation. No other SR feature is launched.

## Verified bootstrap

Started clean on `075d342ec44301bf10642eea39936f64fe34c8f1`, active canon
P2-R002/P2-F002. The launch explicitly confirms predecessor acceptance and both
Project Source refreshes; the older pending-handback prose is historical.
Frozen R045/F052 SHA-256 values match the active Contract. No nested/ancestor
AGENTS.md was found. Root journal is absent; do not create it.

The pinned shared version helper digests match, and the repository-local adapter
was invoked before mutation. Its whole-tree begin scan was stopped before any
reservation after prolonged capture, under the SeedV6 non-critical override.
Subsequent status: idle, active=null, unfinished=[], blockers=[]. Build remains
35; no token, finish, replacement attempt or verified bump is claimed. This is
a non-blocking bookkeeping warning, not pending version repair.

The principal evaluator remains generation 32, lineage
`235d0688-e060-44e3-8f1e-119c39bc8017`, checkpoint
`g000032-s001753088-5837c7f7192aa3cf9a265388f93649327455e804541398ae3ef6b69e09014304`,
network SHA-256 `5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7`.
Its external store, pinned Fastchess executable, Python chess environment and
full 8moves_v3 opening book are present at the baseline report's locations.
Mapping remains `BRN_PAIR2_MATERIAL_RESIDUAL_QUARTER_V1`; HCE pruning is off.

Rebuilt `:app:installDist :app:verificationClasses` offline with Java 21 and
froze the installation at `app/build/sr001-p2-baseline`. Hash-bound UCI preflight
passed, source label `075d342ec443`, with stable inputs and the required evaluator
identity. Three repeated depth-4 requests on each of start/Kiwipete/endgame,
20,000 nodes, movetime, clocks, explicit stop and mate/stalemate are exercised.
Raw receipts: [evidence](sr001-phase2-2026-10-09/).

Initial regression command:

```powershell
.\gradlew.bat :app:installDist :app:verificationClasses :app:test -Pheadless --offline --tests 'com.ohinteractive.seedv6.search.exact.Quiescence*Test' --tests 'com.ohinteractive.seedv6.search.exact.ExactSearchTest' --tests 'com.ohinteractive.seedv6.search.exact.PvsSearchTest' --tests 'com.ohinteractive.seedv6.search.driver.*'
```

144 discovered tests, 133 passed, 11 existing disabled, zero failures/errors,
16 suites. The 11 H/I tests require separately instrumented historical source;
their disabled executions are not runtime passes. Their
XML results are retained. Ordinary runtime reference tests did run.

## Historical evidence and bootstrap challenge

Read the retained SR-001A-I reports, relevant R045 technical/TT/PVS/leaf sections,
F052 row, existing factories, exhaustive oracles and tactical corpus. Historical
HCE evidence establishes mechanisms and counterexamples, not Phase 2 strength:

- A: unpruned captures/EP/all promotions, all checked evasions, no stand-pat in
  check; large tails and expansion. Preserve the independently invocable oracle.
- B: SEE pruning saves work but can omit checking sacrifices, promotion draws
  and nonchecking clearance. Such selective differences need analysis, not
  automatic classification as implementation defects or universal rejection.
- C: q4/q8/q12 truncate resources; checks continue to a non-check boundary.
- D: cold separate exact qTT reduced nodes but increased aggregate HCE time.
- E-G: ordinary checks recover resources, yet unlimited/forcing continuations
  can explode; initial-only checks offer a distinct bounded hypothesis.
- H: immediate material gain is not an evaluation upper bound. Do not import
  numerical HCE delta margins into Pair-2 without independent calibration.
- I: eligible post-move static pruning duplicates the one-node child stand-pat
  cutoff; skipping the child can also change fail-soft returns/PVs.

Source inspection identifies a crucial integration boundary: current positive
depth-one mate-domain lower bound assumes static leaves. Qsearch needs a valid
real-ply bound. Historical qsearch factories are TT-off ordered alpha-beta;
comparing that complete path against production PVS would confound leaf policy
with traversal/ordering. New candidates must use the production driver, ordering,
clock/lifecycle and PVS, and explicitly suppress ordinary nominal proof affected
by qsearch. No TTable storage/locking or proof-applicability redesign is authorized.

For external design comparison, inspected the official
[Stockfish qsearch implementation](https://github.com/official-stockfish/Stockfish/blob/master/src/search.cpp)
on 2026-10-09. Its stand-pat, exchange/futility selectivity and recapture exemptions
suggest families worth testing, but its score thresholds, TT proof, draw and
maximum-ply policies do not transfer to SeedV6. No source code is copied.

The first hypotheses are conventional unpruned qsearch; promotion/check-safe
SEE; bounded tactical horizon; first-layer tacticals followed by recaptures;
limited quiet checks; and check-only leaf verification. They differ materially.
Prototype only enough combinations to explain evidence. Numerical delta tuning
and qTT are later possibilities if a specific mechanism/cost justifies them.

All initial policies have no qTT and no HCE numeric calibration. A qsearch
dependency is selective relative to nominal static Search even if its own
restricted tree is exactly enumerated. Preserve independent static and SR-001A
paths, terminal/history/evasion/mate semantics, and incomplete-result honesty.
Do not enable experimental policies by default.

## Prospective measurement and selection

Separate correctness, fixed-depth/node diagnostics, total request economics and
strength. Use the existing Fastchess adapter, one worker, requested 64 MiB TT,
book/tablebases off, frozen model/mapping, Java 21 flags and three depth-4 warmups.
Every run binds commands, binaries, model, openings and runner by hash. No build,
test or training workload alongside timed matches. Normal desktop load remains
a limitation and must be recorded for confirmation.

Initial exploratory screen: 16 distinct opening pairs per surviving family,
partition 0, seed 20261011, 10+0.1, complete fixed sample, no significance-based
early stopping. These short matches select mechanisms and estimate variance;
they cannot independently support adoption. Candidate order is predeclared below;
no concurrent engine matches. Preserve every adverse result.
Correctness failures stop the affected run and invalidate it until diagnosed.

Advance credible distinct candidates to 60+0.6. Prospectively freeze each next
sample size from exploratory effect/paired variance and available resources;
record it before seeing outcomes. Small possible gains receive correspondingly
more evidence, not a minimum-Elo rule. Avoid arbitrary parameter sweeps.

One frozen finalist gets untouched partition-1 openings and a separately declared
fixed sample. Primary confirmation: paired-score normal 95% interval, justified
only with adequate cluster count/outcome distribution; also require examination
of the conservative Hoeffding interval and report both. Adoption requires a
positive primary lower bound, no unresolved integrity failures, and meaningful
independent confirmation. If multiple confirmatory hypotheses become necessary,
predeclare family error control before those games. Do not extend a nearly
significant sample after inspecting it. Longer-control diagnostic/confirmation
work is selected according to the observed effect and cost.

Continue while a credible distinct investigation can change the decision.
Inconclusive strength is neither adoption nor proof of inferiority. A final
DEFERRED outcome requires precise evidence deficits/resumption conditions;
REJECTED requires substantial investigation, not budget exhaustion.

## First implementation and correctness gate

Explicit `quiescencePvsResearch` composes the existing staged ordering, PVS,
history, real-ply mate bounds and production driver. A monotone qsearch dependency
epoch suppresses ordinary nominal ancestor/root stores; no qTT is added.
Independent static constructors and all historical SR-001A-G factories retain
their semantics. No production default changes. UCI opts in only with
`seedv6.uci.qsearchResearch`, requires an explicit verified Pair-2 store and
supports one worker. It reports the selected policy, experimental status and
proof exclusions. Invalid names fail; defaults remain unchanged.

Researched selectors: unpruned; SEE with promotions retained; SEE with promotions
and checking captures retained; q4/q8/q12; initial-only quiet checks; unrestricted
first tacticals then destination recaptures/promotions; SEE/promotion-safe plus
initial quiet checks; check-only continuation; q2. Every mode keeps all checked
evasions and only ends a nominal qhorizon out of check. Absolute real ply 256
still aborts unresolved work, without a fabricated draw/static result.

First focused tests passed, then the broad exact/driver/manage/UCI/rules run
passed **421 tests**, 11 historical instrumentation tests disabled, zero
failures/errors, 53 suites, 109 seconds. A subsequent focused run passed the
new concrete negative-mate depth-one witness, node accounting/aborted iteration
and UCI-selection checks. The explicit `sr001Pair2Probe` passed **250** comparisons
against independent full legal enumeration using fresh model recomputation,
including promotions, EP, checked evasions, terminal/draw and limited-check trees.
This supplements, rather than modifies, the preserved HCE SR-001A reference.

The negative-mate witness follows `e7f8` from
`5r1k/4Qpbp/4NN1N/8/8/8/8/4K3 w - - 0 1`; at the resulting black node,
nominal depth one plus qsearch returns -32766, including a mate-band scout.
The static-only -32511 lower bound would be invalid there.
Tests also verify no q-dependent root TT proof, independent static reuse,
board/history restoration, all-policy cancellation, legal PVs and owner reuse.
Raw XML/logs and the complete real-model oracle rows are retained.

One initial Gradle command attached `--tests` to `verificationClasses` and failed
before execution. Corrected task order ran successfully; both logs are preserved.
Six Python adapter/statistics tests passed. No benchmark uses that failed command.

## First whole-request diagnostics

The real Pair-2 oracle rows also preserve adverse selective differences. On the
34 compact fixtures, SEE/promotions differs from unpruned on two, SEE/checks on
one, q4 on two, recapture on three, check-only on nineteen and q2 on six.
For example, checking sacrifice is +32765 unpruned, +898 SEE/promotions, +1061
recapture and +820 q2; SEE/checks preserves the mate. Nonchecking clearance is
-336 unpruned versus -408 for all three selective SEE/recapture forms. Recapture
also changes the promotion positional resource from -108 to -485. q4 changes
successive x-rays from -88 to -11 and the exchange fixture from +717 to +646.
These are correctly enumerated restricted-tree differences, with score units
uncalibrated as before. They are tactical costs to weigh in playing evidence,
not hidden implementation failures or proof that a policy cannot improve Elo.

`Sr001Diagnostics` ran seven positions, nine policies, depths 3/4 and a 20,000
admitted-child budget, five repetitions with rotating policy order: **945 rows**
in `diagnostics-01.csv`. Three depth-4 start-position warmups per owner precede
measurement. A shared JVM uses Java 21, one active processor, 256 MiB initial /
1024 MiB maximum heap to hold nine independent 64 MiB-request TTs. This diagnostic
heap/owner arrangement differs from the separate 512 MiB match JVMs. No concurrent
test/build was running. No allocation/JIT steady-state claim follows.

| Policy | d4 whole nodes (sum) | d4 request ms (sum of position medians) |
| --- | ---: | ---: |
| Static | 17,521 | 8.65 |
| Unpruned | 65,927 | 32.40 |
| SEE promotions | 41,363 | 28.16 |
| SEE checks/promotions | 49,061 | 27.77 |
| q4 | 46,573 | 25.35 |
| Recapture | 40,386 | 25.21 |
| SEE + initial checks | 47,504 | 29.93 |
| Check-only | 21,141 | 11.19 |
| q2 | 41,652 | 20.72 |

Every fixed-depth request completed. The 20,000-node requests include the final
aborted iteration; the static underpromotion fixture instead reaches its depth-64
ceiling after 712 nodes. All other fixed-node requests consume exactly 20,000.
Candidate qsearch typically loses one nominal completed ply at this budget.
That is a cost measurement, not evidence of lost strength. Main/q node counts
are admitted transitions (q-boundary children belong to q); recursive entry
counts in ExactSearch/reference outputs have a different convention. Evaluations,
maximum real/q ply, root move, score and legal PV prefixes remain in raw rows.
TT/SEE/generation-time breakdowns and allocations were not separately profiled.

Static depth-4 start/Kiwipete/endgame reproduce baseline nodes 3912/7761/738,
scores 7/12/6 and moves b1c3/e2a6/b4f4. Compared with Phase 1, Pair-2 trees show
smaller expansion on these bounded fixtures; this warrants strength testing of
all eight distinct prototypes rather than rejecting them on runtime cost.

## Current continuation

**Host-load discovery during screen 02:** actual hardware is Ryzen 5 5500,
6 cores/12 logical processors, Windows 11 Pro 10.0.26300, approximately 31.8 GiB
RAM, Balanced power plan, match-process affinity mask 4095 (all logical CPUs).
`host-screen.json` and a five-second process CPU sample show a separate
Weasel/SeedV6-UE chess workload and EndlessSpace active alongside this run.
These are not Codex-owned research processes and were not stopped or modified.
The preceding screens are exploratory under uncontrolled external desktop load;
they must not be represented as quiet-host confirmation. No Codex test/build or
training workload runs concurrently with the matches. The owner subsequently
confirmed the other applications were shut down and authorized continuation.
At 03:47:48 UTC, their process names were absent and a fresh five-second CPU
sample showed the two match engines dominating activity, with no comparable
external workload (`host-after-owner-close.json`). The requested quiet-window
action is satisfied. Screen 01 remains externally loaded; screen 02 spans the
transition. Later screens and confirmation require continued quiet conditions.
Do not retrospectively relabel the earlier evidence or request this action again
without a new observed need.

Completed screen 01 (recapture): 32/32 normal games, all inputs stable and
independent PGN/accounting audit passed, 904.72 seconds. Candidate W/D/L 20/7/5,
pair bins [1,0,5,3,7], score .734375, score-equivalent Elo +176.66. Pair-score SE
.07384; normal interval deliberately unavailable at n=16. Conservative 95%
Hoeffding score interval [.39485,1] (Elo [-74.17,+infinity]) is inconclusive.
This is selection evidence, not an established gain. Whole-request totals:
baseline 941,372,688 nodes / 448.134 s; candidate 838,505,543 / 449.225 s.
Neither those work totals nor the favorable point estimate establish adoption.

Completed screen 02 (SEE/promotions): 32/32 normal games, stable inputs and
independent PGN/accounting audit passed, 803.01 seconds. Candidate W/D/L 27/2/3,
pair bins [0,0,3,2,11], score .875, score-equivalent Elo +338.04. Pair-score SE
.05103; normal interval unavailable at n=16. Conservative 95% Hoeffding score
interval [.53547,1] (Elo [+24.69,+infinity]) is positive for this exploratory
sample, but is not independent confirmation after candidate selection and does
not remove the mixed external-load limitation. Whole-request totals: baseline
855,129,872 nodes / 396.292 s; candidate 759,192,504 nodes / 400.518 s.
The eight policies share these 16 opening clusters; do not pool them as 128
independent pairs or infer a precise ranking from separate point estimates.

Completed screen 03 (q4), after the quiet-host transition: 32/32 normal games,
stable inputs and all independent audits passed, 801.99 seconds. Candidate W/D/L
27/1/4, pair bins [0,0,4,1,11], score .859375, score-equivalent Elo +314.45,
pair-score SE .05576. The exploratory Hoeffding 95% score interval is
[.51985,1] (Elo [+13.80,+infinity]); n=16 normal interval remains suppressed.
Whole-request totals: baseline 960,011,193 nodes / 394.883 s; candidate
878,231,518 nodes / 401.372 s. This supports longer-clock investigation of
bounded as well as SEE-restricted trees, not a precise superiority ranking.

Completed screen 04 (unpruned): 32/32 normal games, stable inputs and all audits
passed, 834.00 seconds. Candidate W/D/L 25/6/1, pair bins [0,0,1,6,9], score .875,
score-equivalent Elo +338.04, pair-score SE .03953; Hoeffding 95% score interval
[.53547,1] (Elo [+24.69,+infinity]), normal interval unavailable. Whole-request
totals: baseline 1,002,476,010 nodes / 411.104 s; candidate 920,467,575 nodes /
416.043 s. Unpruned ties SEE/promotions' score on this small common sample and
avoids its documented selective tactical omissions. Despite its larger fixed-depth
tree, it remains a serious finalist; cost alone does not reject it. No formal
equivalence or relative-superiority claim follows from matching point estimates.

Completed screen 05 (SEE/promotions plus initial quiet checks): 32/32 normal
games, stable inputs and all audits passed, 738.99 seconds. Candidate W/D/L
24/7/1, pair bins [0,0,2,5,9], score .859375, score-equivalent Elo +314.45,
pair-score SE .04546; Hoeffding 95% score interval [.51985,1]
(Elo [+13.80,+infinity]), normal interval unavailable. Whole-request totals:
baseline 868,121,102 nodes / 363.239 s; candidate 654,651,651 nodes / 369.642 s.
Initial checks recover distinct resources in the oracle fixtures, but this
screen does not show a clear score advantage over simpler unpruned qsearch.

Completed screen 06 (check-only): 32/32 normal games, stable inputs and all
audits passed, 885.97 seconds. Candidate W/D/L 5/4/23, pair bins [8,4,3,0,1],
score .21875, score-equivalent Elo -221.14, pair-score SE .07172; Hoeffding 95%
score interval [0,.55828] (Elo [-infinity,+40.68]), normal interval unavailable.
Whole-request totals: baseline 1,072,007,104 nodes / 439.139 s; candidate
1,005,239,158 nodes / 440.102 s. The point estimate is strongly adverse, though
this small-sample conservative interval alone does not establish inferiority.
Do not advance this policy ahead of the strong capture-search families. Its
modest fixed-depth cost did not buy a favorable playing result. This compares
complete configurations: checked-leaf extension and conservative ordinary-TT
suppression are both present; it does not isolate one causal mechanism.

Completed screen 07 (q2): 32/32 normal games, stable inputs and all audits passed,
843.97 seconds. Candidate W/D/L 18/8/6, pair bins [0,0,6,8,2], score .6875,
score-equivalent Elo +136.97, pair-score SE .04270; Hoeffding 95% score interval
[.34797,1] (Elo [-109.09,+infinity]), normal interval unavailable. Whole-request
totals: baseline 996,935,784 nodes / 417.090 s; candidate 934,895,366 nodes /
420.143 s. This is favorable but inconclusive exploration and trails the deeper
capture-search point estimates; no paired opening was a net loss, but many were
split. Preserve it as a useful depth/cost alternative rather than an established
gain or an equally strong cheaper substitute.

Completed screen 08 (SEE with promotions and checking captures retained): 32/32
normal games, stable inputs and all audits passed, 824.87 seconds. Candidate
W/D/L 24/5/3, pair bins [0,0,4,3,9], score .828125, score-equivalent Elo +273.15,
pair-score SE .05458; Hoeffding 95% score interval [.48860,1]
(Elo [-7.92,+infinity]), normal interval unavailable. Whole-request totals:
baseline 1,021,591,554 nodes / 406.946 s; candidate 923,826,840 nodes / 411.884 s.
It preserves the checking-sacrifice mate witness, but still omits nonchecking
clearances and has no established playing advantage over unpruned here.

All eight fixed screens are complete: 256 normal games, no forfeits, timeouts,
incomplete games or input-integrity failures. Each uses the same 16 opening
clusters; the eight runs cannot be pooled as 128 independent openings.

| Policy | W/D/L | Candidate score | Selection implication |
| --- | ---: | ---: | --- |
| Recapture | 20/7/5 | .734375 | Favorable, lower-ranked; external-load limitation |
| SEE/promotions | 27/2/3 | .875 | Cost-saving contender; mixed-load limitation |
| q4 | 27/1/4 | .859375 | Credible bounded alternative, no demonstrated ranking edge |
| Unpruned | 25/6/1 | .875 | Leading simple formulation; preserves tactical resources |
| SEE + initial checks | 24/7/1 | .859375 | Added checking work has no demonstrated ranking edge |
| Check-only | 5/4/23 | .218750 | Adverse complete-policy screen; do not advance |
| q2 | 18/8/6 | .687500 | Cheaper but lower-ranked, inconclusive gain |
| SEE/checks/promotions | 24/5/3 | .828125 | Credible selective alternative, no demonstrated ranking edge |

**Next prospective stage, declared before any new games:** compare unpruned and
SEE/promotions separately against the frozen static baseline at **60+0.6**, eight
new partition-0 opening pairs each, selection/runner seed **20261012**, complete
fixed samples, in that order. Both use the same eight new positions, explicitly
excluding the initial 16. Continue one runner and one worker per engine, the
same model/heap/TT/book/TB controls, quiet host and whole-request auditing.
These 32 games are exploratory time-control/selection evidence, not independent
confirmation or eight-pair statistical acceptance. There is no early score stop.

Rationale: unpruned matches the best short-screen score, has only one loss,
retains the known tactical resources and is the simplest move domain. SEE/
promotions ties its score with materially lower fixed-depth work; repeating it
under the quiet host addresses the earlier load limitation. q4/SEE-check/initial-
check point estimates do not establish a ranking edge sufficient to justify an
arbitrary parameter sweep. Retain their raw evidence and precise omissions.
Use the longer-clock results to select one defensible finalist and predeclare
its independent partition-1 confirmation size, clock and uncertainty test.

Bootstrap, first implementation and correctness/diagnostic gates are complete.
Candidate source `8b7b34b` is frozen at `app/build/sr001-p2-candidates-v1`;
`candidate-v1.json` binds binaries and validation. The declared 16-pair 10+0.1
screen completed in the fixed order above. Keep the frozen baseline installation
unchanged. Validate the prepared mechanics optimization, freeze the next candidate
installation with its source/hash receipt, then execute the declared longer-clock
stage. Preserve all prior evidence; production remains static pending confirmation.

While the frozen v1 screen runs, source review identified two explicit-suffix
API issues outside the UCI/driver path. The existing suffix signature lacks the
previous destination required by recapture policy, and direct candidate suffix
entry must retain selective provenance. Source now rejects recapture suffixes
with consumed qplies and initializes the candidate suffix dependency correctly.
Added regression passed in the gap after screen 01; no compiled match
binary or frozen adapter is changed. The static-control test now uses a fresh
table as its constructor ownership precondition requires. These edits must be
included in the final adoption build; they do not change the v1 ordinary-driver
experiment. The same run verifies independent best-move realization and absence
of all q-dependent nominal stores in the start-position witness.
That suffix/reference/driver run comprises 23 tests, all passed with no skips.

A source-level economy opportunity is retained for post-screen investigation:
non-check qnodes without tacticals currently generate the entire quiet list for
legal existence even when their policy cannot search quiet checks. Production
already has an independently tested exact legal-existence helper. Reusing it
only in the new PVS composition could save generation without changing tree
values, ordering or the preserved historical oracle. This is a hypothesis until
differential correctness and whole-request timing are measured; frozen v1 is
unchanged. No qTT/proof-boundary relaxation or numerical delta threshold is
needed to pursue it.
During screen 08, prepared this small `ExactSearch` source edit: compute quiet-
check eligibility before terminal generation and use `hasLegalMoveNotInCheck`
only for nominal PVS qnodes with no tacticals and no eligible quiet checks.
After the timed batch ended, the legal-existence/reference/Phase-2/driver run
passed **91 tests**, with 11 historical instrumentation tests disabled, no
failures/errors, 12 suites. All **250** real-Pair-2 oracle rows are identical to
v1. Two quiet-host old/new diagnostic comparisons, with order reversed on the
repeat, produced **3,780 rows**; every non-timing field agrees across versions,
including full PV, scores, node/evaluation counts, depths and termination.

The fixed-depth timing effect is mixed and not a demonstrated general speedup.
For example, unpruned d4 sums of position medians were 29.79/31.62 ms before
and 30.35/28.86 ms after; SEE/promotions were 19.09/19.16 before and 19.11/19.46
after. At the complete 20,000-node request budget, unpruned was 54.06/57.63 ms
before versus 48.96/50.24 after; SEE/promotions 55.54/56.19 versus 48.79/50.32.
Unchanged static control was 43.17/46.71 versus 42.91/44.79 ms. This repeatable
bounded-request saving and exact semantic comparison justify retaining the small
mechanical change for the next candidate. No strength or universal speed claim
comes from these short JVM measurements. Raw CSVs use `diagnostics-v1/v2-quiet*`;
test XMLs and logs are in `optimization-tests/` and `optimization-tests.txt`.

The next candidate also includes the already-tested explicit-suffix provenance
fixes. Neither change mutates the frozen v1 installation. The eight longer-clock
openings selected with seed 20261012 have zero position-key overlap with the
initial 16; their selection receipt retains the source/output hashes and keys.

Candidate v2 is frozen from `3b74f06ca6d4` at
`app/build/sr001-p2-candidates-v2`; app.jar SHA-256
`f34bb1f4fa34852d559d61fabd6adea4f6e7f346dd933b0f832561d17ad621a4`.
`candidate-v2.json` binds the source, clean embedded revision, dependencies,
validation and prospective longer stage. Build remains 35 under the already
recorded non-blocking version warning. The baseline/v1 binaries are unchanged.

Pre-longer host samples at 05:17:23 and 05:18:12 UTC still show none of the
previous chess/game workloads. They do show an Edge process at approximately
.90 and .43 core equivalents respectively. Ordinary desktop activity therefore
remains present; this is not a dedicated or strictly idle host. Preserve these
samples and qualify the exploratory clock measurements accordingly. Proceed
with the declared exploratory stage, keeping all Codex builds/tests/training
stopped during matches; recheck competing load before independent confirmation.

**Longer stage 01 complete (unpruned v2, 60+0.6):** 16/16 normal games,
stable inputs and independent audits passed, 2,396.46 seconds. Candidate W/D/L
**15/1/0**, pair bins [0,0,0,1,7], score .96875, score-equivalent Elo +596.54.
Pair-score SE .03125; the normal interval is unavailable at n=8. Conservative
95% Hoeffding score interval [.48859,1] (Elo [-7.93,+infinity]) is still
inconclusive despite the high point estimate. Do not substitute this selection
sample for independent confirmation. Whole-request totals: baseline
2,832,152,465 nodes / 1,182.460 s; candidate 2,827,079,596 nodes / 1,208.915 s.
Game-dependent work/NPS is not an isolated implementation speed comparison.

The 05:55:57 UTC host sample still finds no other chess/game workload. The match
engines together consumed approximately .99 core equivalents; two Vivaldi
processes together consumed .50, with smaller desktop activity. This supports
continued serialized exploration on the six-core host, while preserving the
ordinary-background-load limitation and avoiding a claim of complete isolation.
Raw receipt: `host-longer-midpoint.json`. Longer stage 02 (SEE/promotions v2)
subsequently completed its full declared eight-pair sample.

**Longer stage 02 complete (SEE/promotions v2, 60+0.6):** 16/16 normal games,
stable inputs and all independent audits passed, 2,462.35 seconds. Candidate
W/D/L **11/3/2**, pair bins [0,1,1,2,4], score .78125, score-equivalent Elo +221.14,
pair-score SE .09952. Hoeffding 95% score interval [.30109,1]
(Elo [-146.29,+infinity]); normal interval unavailable at n=8. Whole-request
totals: baseline 3,095,182,591 nodes / 1,221.253 s; candidate 3,010,113,465 nodes /
1,234.087 s. This is another favorable but inconclusive exploratory result.

### Finalist selection and prospective independent confirmation

Selected **unpruned v2**: conventional stand-pat out of check, all legal captures,
EP and promotions, all checked evasions, no added quiet checks, no nominal qcap,
no qTT and no q-dependent ordinary nominal proof. It ties the leading short-
screen score, leads the longer screen, preserves the known selective tactical
resources, and has the simplest move domain. Neither the short nor longer sample
establishes a precise superiority margin over the other qsearch policies.

Additional small against-static screens have diminishing ability to rank near-
ceiling contenders. A qdepth/SEE/check-parameter sweep would add selection without
resolving that ranking. This selection seeks a defensible gain over the accepted
baseline, not a claim of globally optimal qsearch. q4 and limited checks remain
credible alternatives; recapture/q2 remain lower-ranked possibilities. Numerical
delta/futility would need independent calibration, and qTT would need legitimate
policy/history/horizon proof and a measured benefit. None is universally rejected
or silently authorized as a neighboring feature.

**Declared before selecting or playing confirmation openings:** frozen candidate
`3b74f06ca6d4`, v2 JAR hash above, `--candidate-qpolicy unpruned`, versus the
unchanged `075d342` static baseline. **40 new opening pairs (80 games), 30+0.3,
partition 1, selection/runner seed 20261013, fixed complete sample.** No result-
based stopping, extension, replacement, discarded failures or parameter changes.
Thirty seconds triples the initial screen's clock and increment and lies between
the two explored controls; the larger independent opening sample is more useful
than another eight-pair high-variance run. This establishes only the stated fast-
clock domain; 60+0.6 remains separately reported exploratory evidence.

Because exploration produced highly skewed/near-boundary pair outcomes, revise
the earlier *prospective* normal-primary plan now, before any held-out games:
**primary acceptance uses the two-sided 95% bounded-pair Hoeffding interval**.
Its lower score bound must exceed .5 (equivalently its Elo lower bound exceeds 0).
Report the paired normal interval secondarily where the adapter permits it;
do not select between intervals after seeing outcomes. At n=40 the Hoeffding
radius is approximately .2147. The large exploratory effects justify this
distribution-free design and sample; its finite resolution is not a minimum-Elo
adoption rule. A smaller observed gain would require further valid evidence.

Only this finalist confirmation is currently planned. Positive-lower-bound
acceptance spends one-sided alpha .025. If a materially justified additional
confirmation becomes necessary, predeclare a fresh frozen candidate, fixed
sample and untouched openings with one-sided alpha .0125, then .00625, etc.;
the geometric sum is at most .05. Never reuse an unadjusted .025 test or extend
this sample after inspecting it. Preserve every attempted confirmation.

Keep one worker, 64 MiB requested TT, frozen model/mapping, 256/512 MiB heap,
one active JVM processor, warmup 3x depth 4, book/TB off, serialized Fastchess,
strict normal endings and full PGN/accounting/input audits. Do not run Codex
builds/tests/training alongside it. Recheck the six-core host before launch and
during the run: no other chess/training/game load is allowed; ordinary desktop
activity is recorded, not represented as complete machine isolation. The prior
owner action remains satisfied. Integrity failures invalidate acceptance rather
than being silently excluded. No production default is enabled before confirmation.
At external handback reconcile both active masters and require owner replacement
and confirmation of both Project Source mirrors. No such handback is claimed yet.
