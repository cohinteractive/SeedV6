# SR-006 Phase 2: aspiration windows

2026-10-10 to 2026-10-11 local. **REJECTED** technical disposition for the
investigated single-worker generation-32 Pair-2 conditions. The six-formulation
investigation found no credible strength-supported aspiration adoption; the
independent finalist result is statistically inconclusive, not proof of no gain.
Production retains full-window cumulative SR-001+SR-029. Owner/GPT acceptance
and Project Source synchronization remain external. No successor was selected.

## Bootstrap and historical challenge

Clean starting HEAD `a65a6ed42fe4a8cb7e08b12c0e714e0476eb78e6`, Contract
P2-R004 / Frontier P2-F006. The launch confirms SR-029 owner/GPT acceptance and
both mirror replacements/synchronization. Earlier pending language is historical.
Actual code selects unpruned qsearch plus protected quiet LMR for single-worker
Pair-2. Mandatory frozen cumulative control is `app/build/sr029-p2-production-final`,
source `8940a5f9bf35e3dbb4012b88fdcce4f7294f108e`, app.jar SHA-256
`50976cb719da6e75ef2d63ad836f88ba26514f35e6ead1594801523663181dd2`.
[Preflight](sr006-phase2-2026-10-10/baseline-preflight/run.json) passes actual
UCI identity, deterministic depth, node/time/stop/terminal and stable-input checks.
An initial CLI spelling error exited before any run; corrected to `--baseline-qpolicy`.

All four generation-32 checkpoint hashes match the measurement baseline at
`E:/SeedV6-Networks/BRN-Pair2/235d0688-e060-44e3-8f1e-119c39bc8017`.
The exact checkpoint, mapping and network hash remain unchanged; no latest/Play
substitution, training or store mutation. Both frozen R045/F052 archive hashes
match the active Contract and their bytes remain untouched. Root journal absent.
Pinned version helper hashes verified; local begin was stopped during prolonged
capture before reservation. Subsequent status idle, no token/unfinished/blocker.
Non-blocking version warning; build 35 retained, no finish or replacement bump due.

Read active canon, historical SR-006 R045/F052 entries, retained
`AspirationResearch{,Search,Test}`, and local raw `build/sr006b-final` / `build/sr006c`.
Their 33-position first-repeat node totals reproduce historical conclusions:
CONTROL 263,152,114; ASP-512 264,838,090; ASP-628 262,493,617;
ADAPT-172/628 264,636,284. Fixed immediate-full retry and stable/volatile widths
preserved bounded exact semantics but offered no established wall-time benefit.
The old HCE widths, score parity and cheap TT-assisted retry findings do not
transfer numerically to Pair-2 qsearch/LMR. No Phase 1 strength evidence exists
for these policies. Retained tests cover equality boundaries, mate discovery,
original-window TT types, same-generation retries and cancellation between attempts.

Read SR-001 and SR-029 reports, actual adapter/driver/PVS/TT/lifecycle/UCI seams.
Current qsearch and reduced dependencies suppress ordinary nominal TT stores;
the root often has no reusable proof or hash move. Quiet history resets on every
invocation. Aspiration can therefore duplicate expensive work without the cheap
fallback of the historical experiment. It can also change selective decisions,
so full-window equality is mandatory for exact controls, not assumed for LMR.
Changing TT/history policy to make aspiration win would confound this feature.

The versioned [Stockfish 17.1 source](https://github.com/official-stockfish/Stockfish/blob/sf_17.1/src/search.cpp)
motivates gradual directional widening and volatility-dependent windows.
Its numerical widths, score averaging, root sorting, depth reductions and
selective semantics are not imported. Every candidate here searches the same
nominal depth on retries and preserves original invocation windows and proof.

Retained SR-029 depth-six diagnostic data provide only an initial hypothesis:
96 adjacent ordinary-score changes per policy, quiet LMR median 3, 90th-percentile
14, 92/96 at most 16, maximum 157 internal units. Unpruned qsearch median 4,
90th-percentile 11.9, maximum 79. These small tactical-corpus samples neither
calibrate centipawns nor prove future-window coverage. Fresh opening samples,
resource comparisons and actual games must challenge them.

## Prospective investigation

Compare fixed narrow one-shot/full fallback, wider one-shot, directional geometric
widening and recent-volatility-adaptive widening. Initial exploratory widths
16 and 64 derive from the observed Pair-2 movements, not HCE/Stockfish margins.
No arbitrary grid sweep. Start only after semantic tests; adapt materially distinct
hypotheses to measured failure/retry/root-decision costs.

Depth one, missing prior adjacent completed score, and mate-domain history use
full windows. Narrow endpoints exclude mate scores. Only a strictly inside-window
completed return resolves aspiration; equality fails. A mate return or bounded
retry exhaustion triggers a full window. Cancellation or exhausted budget leaves
the prior completed iteration, with every attempt and aborted child accounted.
All retries share request generation, immutable root/history, depth and evaluator;
no failed bound is relabeled exact or published as a completed iteration.

Use the frozen model, Java 21 baseline flags, three untimed depth-four warmups,
one worker, requested 64 MiB TT, book/tablebases/ponder off. Freeze installations,
commands and hashes. Timed work excludes builds/tests/training. Up to four
independent games may run concurrently, as validated by SR-029; this does not
enable multiworker Search. Record host conditions and preserve failures.

Initial diagnostics use preserved tactical/history roots and fresh partition-0
openings. Measure score changes, retries/direction, failed/aborted costs, legal PV,
root decisions, depth, actual main/q work and whole-request time independently.
Initial exploratory games: 32 pairs per credible family, 10+0.1, partition 0,
seed 20261060, fixed complete samples with no significance-based stopping.
These guide candidate selection, not acceptance. Larger/longer comparisons may
follow evidence. Before independent confirmation, freeze one finalist and declare
fresh partition-1 seed, fixed sample, clock, primary interval/acceptance criterion
and statistical sensitivity. No exploratory pooling or post-hoc sample extension.
Playing strength, with adequate positive evidence, is required for adoption;
efficiency and deeper nominal results alone cannot establish a gain.

## First prototype and completed opening diagnostics

Explicit `AspirationSearch` surrounds the existing adapter's window entry, with
unchanged recursion/TT storage. Policies are ONE16, WIDE64 (one narrow attempt,
then full fallback), WIDEN16 (double width and expand only the failed side from
the fail-soft return), and ADAPTIVE16 (initial width max(16, twice the last score
movement), capped at 512, then the same widening). Four unsuccessful narrow
attempts cap retries before full fallback. All attempts keep nominal depth.
No production default selects these policies. UCI rejects invalid compositions
and non-single-worker research; measured identity explicitly reports aspiration.

The initial 13 focused tests passed after correcting a test's use of a
package-private key helper (compile-only failure; no engine defect concealed).
They cover exact TT-off score agreement, equality failures and original TT types,
single request generation, cancellation/last-completed retention, exhausted
retry budgets, mates, oscillating failures and actual-domain accounting.
Additional deadline/interrupt, exact-final-node and independent qsearch tests
are being validated before clock games. A review hardening prevents unbracketed
standalone calls from consuming prior scores; the driver already brackets requests.
Avoided an unnecessary immutable request copy when its observer is already NONE.

The first diagnostic run completed **1,035 requests**: 69 roots, five policies,
depth 6, depth 8 with a 2M-node ceiling, and 100k nodes. The 64 fresh eight-move
partition-0 openings use seed 20261059; five retained adverse/history roots
complete the corpus. Every final legal PV and main+q/request-node equality check
passed. [Raw traces and summary](sr006-phase2-2026-10-10/diagnostics-v1-summary.json)
preserve incomplete attempts, invocation windows and retry costs. Source hashes
were captured during execution before subsequent adapter hardening.

All policies completed all 69 depth-six roots. Relative whole-request nodes:
ONE16 .79685, WIDE64 .90590, WIDEN16 .79265, ADAPTIVE16 .81233. They changed
13/12/13/14 root moves respectively versus full-window selective control.
These are selective research differences, not exact-reference failures or
strength gains. Depth-eight completion under the ceiling was 22 roots for
control versus 28/28/28/29; incomplete roots remain in the raw record and are
not mixed into fixed-depth node ratios. Most opening score changes are small,
but ordinary changes up to 157 occur. WIDEN16 saved retry work versus immediate
full fallback; adaptive widths reduced failures further at some initial cost.
Instrumented timing is diagnostic, not a clean throughput claim.

Opening-only stability is insufficient. Next repeat on 64 outcome-blind sampled
positions from SR-029's exploratory `screen-02-quiet/games.pgn`, preserving
complete move prefixes. The reproducible `sr006-midgame-corpus.py` alternates
early/late halves of available game trajectories using seed 20261061; it never
selects by result or evaluation and does not use held-out confirmation openings.

### Played positions, correctness gate and prospective games

The 64 played-position run completed **960 requests** with all accounting and
final-PV checks passing. All depth-six requests finished. Node ratios to control:
ONE16 .84988, WIDE64 .84956, WIDEN16 .84072, ADAPTIVE16 .83493. At depth eight,
control completed 49 roots under 2M nodes; candidates completed 53/54/53/55.
Total retry nodes there were 8.21M/.52M/6.49M/2.58M respectively. At 100k nodes,
mean completed depth was 5.72 control versus 5.95/6.03/6.00/6.02. No terminal
root reached the ceiling in this sample. Root changes and all unsuccessful/aborted
attempts remain in [raw/summary evidence](sr006-phase2-2026-10-10/played-v1-summary.json).
These samples are diagnostic trajectories, not independent strength observations.

Played-position score movement has heavier tails than openings: median 4–8 by
depth, with maxima 535 at depth two, 621 at three and 349 at six. This supports
testing both wide protection and recent-volatility adaptation, rather than
assuming one fixed narrow margin. Directional widening reduces retry costs
versus immediate full fallback but can oscillate. No universal volatility bound
or evaluator calibration is inferred.

Broad validation passed **667 Java tests**, 11 pre-existing historical disabled,
zero failures/errors; all nine Python runner tests pass. Independent unpruned
qsearch checks cover all 33 historical roots and four policies with HCE.
The explicit frozen-model oracle passed **828** separate depth-1/2/3 comparisons
over 69 roots, four policies, full prefixes, independent TT-off ordered qsearch
and legal PV replay. Production LMR is intentionally excluded from oracle equality;
its selective interactions are measured in the production diagnostics/games.
Source, XML, transcripts and results are retained in the evidence directory.

All four mechanisms survive the diagnostic gate. Run the predeclared 32-pair
10+0.1 exploratory samples in this order: ONE16, WIDE64, WIDEN16, ADAPTIVE16,
same seed-20261060 partition-0 opening sample, concurrency four, one worker per
engine, fixed control `sr029-p2-production-final`. Freeze candidate source/JAR
before play; validate off-policy equivalence and A/A first. All samples run to
their fixed size barring integrity failure. No acceptance or ranking significance
is inferred from these screens; confirmation will use fresh held-out clusters.

### Frozen v1 and runtime bridge

Candidate source `d3a1186c7c2a4f82d4fd4177f8736aba89830083`, frozen installation
`app/build/sr006-p2-candidates-v1`, app.jar SHA-256
`863c333cce2230cd6faf7234a53e2b613f9575aaa549bac290ed0f92d337ade4`.
All 22 recursive Search/TT classes are byte-identical to accepted SR-029.
The off-policy bridge passes all **36** deterministic production/unpruned/static
depth/node comparisons, default and drained worker transitions, whole-request
budgets/stop/terminal and invalid-model checks, with stable frozen inputs.
The four-game A/A runtime check passed normal game/pair/legality/accounting and
stable-input audits; it is infrastructure evidence only, not a strength sample.
Receipts are `candidate-v1.json`, `off-policy-bridge/bridge.json` and `aa-01/run.json`.

Screens use the archived `search-baseline-v1.py`, `--baseline-qpolicy production
--candidate-qpolicy production --baseline-lmr production --candidate-lmr production`,
explicit `--candidate-aspiration POLICY`, 32 pairs, 10+0.1, seed 20261060,
concurrency four and a 3600-second failure timeout. Each directory's run receipt
records the full command and hashes; host/power/memory are in `screen-host.json`.
No build, tests, diagnostic benchmark or training accompanies the matches.

### Completed four-policy exploratory screen

All four fixed 32-pair/64-game 10+0.1 screens completed and passed full
game/pair/prefix/legality/accounting, normal-ending and stable-input audits.
The runner/JVM and all four model hashes also match SR-029's accepted confirmation.
The [summary](sr006-phase2-2026-10-10/screen-01-summary.json) links raw receipts:

| Policy | Candidate W/D/L | Elo | Exploratory paired-normal 95% Elo |
| --- | --- | --- | --- |
| ONE16 | 30/8/26 | +21.74 | [-59.39,+105.36] |
| WIDE64 | 23/9/32 | -49.18 | [-135.06,+31.01] |
| WIDEN16 | 34/13/17 | +94.55 | [+7.19,+195.77] |
| ADAPTIVE16 | 22/14/28 | -32.67 | [-88.34,+21.37] |

WIDEN16 is the promising selection, not an accepted improvement. Four screened
policies, a small sample, wide uncertainty and selection bias prevent treating
its positive approximate lower bound as confirmation. No exploratory games will
be pooled into the independent confirmation. Wider/adaptive windows are not
universally inferior; their node/nominal-depth advantages did not establish
strength here. A simpler immediate-full fallback is still inconclusive.

Next prospectively run **64 fresh partition-0 pairs / 128 games at 30+0.3**,
selection seed **20261062**, WIDEN16 against the frozen cumulative control,
same v1 binary/model/JVM/worker/TT settings and concurrency four. Fixed complete
sample, no significance-based stopping, 10,800-second failure timeout. This is
a longer-control development comparison for generalization and confirmation
sizing, not independent acceptance. Do not extend this sample after results.
Before it, run the prepared clean rotated whole-request timing/allocation and
100k/1M-node study on the 64 played roots. No simultaneous compute campaign.

### Clean request economics and longer-control start

The clean cost study completed **768 requests**, two rotated rounds over the
64 played roots, at depth six and 100k/1M admitted nodes. No attempt trace
callbacks run in timed searches. Driver instances persist and reset between
roots; construction is outside timing, while request/result work and all failed
or aborted attempts are inside. All accounting, PV and limit checks passed.
Compiled driver/recursive/TT bytes match the frozen candidate JAR.

At depth six, WIDEN16/control node ratio is .84072 and whole-request time ratio
**.84202** (7.993 versus 9.492 seconds over 128 requests per side). At 100k and
1M nodes, time ratios are .99898 and 1.00117; mean completed depths are
6.000 versus 5.719 and 8.297 versus 7.922. Every fixed-node request exhausted
its budget, so these means have no terminal-ceiling outliers. This supports
lower tree cost rather than faster node processing.

Measured request-thread allocation increases 24.7% at depth six (about 14.7 kB
extra per request), 28.2% at 100k and 12.4% at 1M. No measured GC collection
occurred. Compilation remained active; these are warmed bounded measurements,
not a universal steady-state JVM/cache claim. Raw per-request JIT/allocation,
time and outcome records and [summary](sr006-phase2-2026-10-10/clean-cost-01-summary.json)
retain those limits. Economics do not substitute for strength confirmation.

The declared 128-game 30+0.3 development comparison is now running in
`longer-01-widen16`. Its 64 seed-20261062 partition-0 openings have **zero overlap**
with the 32 initial screen openings. All source/JAR/model options remain frozen;
there is no concurrent build/test/benchmark/training workload. Inspect its
actual receipt and owned processes on resumption; do not launch a duplicate.
`longer-runtime-host-sample.json` records a two-second live sample: the eight
game-engine JVMs account for about 8.08 CPU-seconds; the three other observed
Java processes use zero in that interval. This is consistent with four active
single-worker games, not proof of an otherwise exclusive desktop host.

### Review follow-up pending after the frozen longer match

Review of the retained adverse depth-six traces explains concrete regressions.
At `played-47`, WIDEN16 uses 160,466 nodes versus 93,587 control (1.7146x),
finishing with the same score 1111 and move. Three fail-high attempts precede
resolution at depths four and six; the depth-six failed attempts each repeat
21,595 nodes before a 55,206-node resolved attempt. This is expensive duplicated
work as the score rises from 909 to 1111, not an uncounted or accepted bound.
At `played-54`, alternating score changes exceed the initial width at every
depth two through six: 138,674 versus 87,998 nodes (1.5759x), final scores 6
versus 4 and different root moves under selective LMR. Raw windows show one
retry at each depth. Adaptive widths are a credible response to this witness,
but their complete exploratory match did not establish better strength.
These cases limit the aggregate efficiency claim; they do not justify tuning
the frozen finalist after seeing future confirmation outcomes.

V1's `failLowResearches` / `failHighResearches` counters included a completed
failed attempt when node exhaustion prevented any retry. Whole-request nodes,
returned depth/PV and actual searches are correct; raw traces separately retain
every failed attempt. The established diagnostic meaning counts failures that
cause another invocation. A source-only correction moves the increment after
retry admission and adds assertions to the exhausted-budget test. The running
v1 installation is unchanged. After the match, run the affected tests and freeze
and deterministically bridge the correction before independent confirmation.
Do not close this validation item merely because the strength run completes.
The prepared `tools/verify-sr006-bridge.py` compares explicit WIDEN16 v1/v2
preflight requests and depth-six/100k-node requests over both full-prefix corpora;
it is not yet run while the timed match is active.
Pending tests also cover direct observer publication of only the aggregate and
the opening selector's new `--exclude-selection` option. For independent
confirmation, exclude both predecessor `confirmation-openings.selection.json`
position sets before the fixed-seed draw, retaining their receipt hashes.
This strengthens freshness across successive feature decisions without selecting
by outcome or trying alternative seeds. The live match uses the archived v1
runner, so these working-tree tooling edits cannot alter its configuration.

### Longer result and next hypothesis

The complete fixed 64-pair/128-game 30+0.3 development comparison passed all
normal-ending, legality, pairing, accounting and frozen-input audits in 2,891
seconds. Candidate W/D/L is **51/27/50**, score .50390625, **+2.71 Elo**;
paired-normal 95% **[-47.00,+52.54]**, with the 2-SE and Hoeffding sensitivities
preserved in `longer-01-summary.json`. The short-screen improvement did not
replicate at this sample/clock. The intervals overlap, so this does not prove a
time-control interaction or zero effect. It supplies no adequate positive
effect estimate for selecting a decisive confirmation size. No games are
pooled, no sample is extended, and no partition-1 game has been played.

The counter correction and direct-observer test now pass all **10 focused Java
tests**; all **10 Python tests** pass, including excluded-position selection.
Next freeze and bridge this non-algorithmic correction, then examine an
asymmetric root window with a finite lower bound and infinite upper bound.
The concrete hypothesis is that previous-score alpha can save initial work
without repeatedly failing high on upward score jumps (`played-47`). Fail-low
still requires a resolved retry; it is not waived. This remains SR-006, with
unchanged recursive PVS, LMR, history and TT proof. Compare its costs, root
decisions and games before selecting a new finalist; efficiency cannot qualify it.

The corrected v2 is frozen at source `5bf13b5edca320e2f305cc96c85e6407dca938f5`,
installation `app/build/sr006-p2-candidates-v2`, app.jar SHA-256
`92dd370ed39f1926b926ff50d3186e11acc8b7ee09f6a468bc54692cd999f419`.
The first copy used a nonexistent Gradle distribution directory and failed
before engine launch; the empty failed manifest and preflight failure are retained.
The corrected copy from `app/build/install/seedv6` passes all **278 deterministic
v1/v2 comparisons** (12 preflight and 266 full-prefix corpus depth/node requests),
legal PV, budget/time/stop/terminal and frozen-input checks. See
`counter-bridge-v2-retry-01/bridge.json`; the diagnostic counter repair changes no
observed score, move, PV, depth, total nodes or termination in those comparisons.

Compare two distinct new hypotheses next: **ALPHA16**, previous completed score
minus 16 as alpha with beta at infinity, widening only failed lows; and
**PARITY16**, a symmetric width-16 window centered on the completed score two
depths earlier where available, otherwise the adjacent-depth score. The latter
targets the observed odd/even oscillation (`played-54`) rather than expanding
every volatile window. Both retain same-depth geometric retries, the four-failure
cap, full fallback and request isolation. Explicitly force full verification of
mate-domain returns even when inside ALPHA16's unbounded side. No production
default changes. Run semantic/oracle checks and rotated diagnostics on existing
opening and played corpora before selecting clock comparisons.

V3 implements those two explicit research policies. All **18 focused Java
tests** pass, including both new window constructions, upward ordinary scores,
failed lows, inside-window mate fallback, independent exact agreement and UCI
configuration checks. The first run's two synthetic fixtures returned disabled
diagnostics to enabled requests; fixing the fixtures resolves both failures
without relaxing production diagnostics. The failed XML/log is retained.
The frozen-model oracle then passes **1,242 independent comparisons** (69 roots,
six policies, depths one through three), including legal PV replay.
`new-families-source-v3.json` binds this working source and tests. The rotated
opening diagnostics are running as `diagnostics-v3.jsonl`, comparing off,
WIDEN16, ALPHA16 and PARITY16; repeat on played roots next. No clock games run
concurrently with those studies.

Both v3 studies completed: **828 opening/adverse requests** and **768 played
requests**, with all accounting and legal-PV checks passing. Source hashes
remain stable. All **798** matched off/WIDEN16 requests reproduce v1's scores,
moves, PVs, depths, total nodes and completion under all three limits; only
elapsed time and the corrected retry counters are excluded from that comparison.

| V3 policy | Opening depth-6 node ratio | Played depth-6 node ratio | Played depth-8 completed / 64 | Played 100k mean depth |
| --- | --- | --- | --- | --- |
| Full window | 1 | 1 | 49 | 5.71875 |
| WIDEN16 | .79265 | .84072 | 53 | 6.00000 |
| ALPHA16 | .93554 | .89520 | 52 | 5.93750 |
| PARITY16 | .77875 | .82000 | 53 | 6.015625 |

All depth-six requests finish. ALPHA16/PARITY16 change 9/12 played root moves;
largest depth-six score differences from control are 20 internal units in this
sample, not calibrated error or strength estimates. Played depth-eight retry
nodes are 3.31M/4.16M versus WIDEN16's 6.49M. ALPHA16 has no fail-high retries;
its three inside-window mate returns still receive full verification. Full raw
traces and summaries retain all failures, aborts and individual regressions.

At `played-47`, ALPHA16 uses 87,278 nodes, PARITY16 189,860, control 93,587 and
WIDEN16 160,466, all score 1111. The asymmetric hypothesis addresses this
upward-jump witness; the parity predictor worsens it. At `played-54`, respective
nodes are 78,492 / 97,769 / 87,998 / 138,674, scores 1 / 5 / 4 / 6. Neither
new approach dominates all roots. These results justify games, not adoption.

**Prospective second development comparison:** freeze v3, then run ALPHA16 and
PARITY16 in that order, each against the accepted cumulative control for **64
pairs / 128 games at 30+0.3**, concurrency four, one Search worker per engine,
same checkpoint/JVM/TT/warm-up/no-book/no-tablebase settings. Use one fixed fresh
partition-0 selection, **seed 20261063**, excluding this feature's diagnostic,
initial-screen and longer-comparison opening position sets before drawing.
Both complete their fixed samples irrespective of the other's score, barring
integrity failure; 10,800-second failure timeout each. No significance stopping,
sample extension, exploratory pooling into confirmation or acceptance decision.
This directly examines the longer resource regime where WIDEN16 was inconclusive.
Choose any later independent confirmation only after reviewing these complete
results and freezing its design before any held-out games.

V3 is frozen from clean source `14283788ba46552288b3a144d93b2514564c0d32`
at `app/build/sr006-p2-candidates-v3`, app.jar SHA-256
`5f68df477505d9d91c7892245e30cfb9a0878686fb0747fbe204a14ca678af1a`.
All 22 recursive/TT classes remain byte-identical to accepted SR-029. The
64 selected openings are verified disjoint from all 159 unique positions in
the three excluded SR-006 sets. Ten current Python tests pass. Use the archived
`search-baseline-v3.py`; `candidate-v3.json`, `new-family-openings.selection.json`
and `new-family-host.json` bind source, binaries, fresh inputs and host conditions.
Start `screen-02-alpha16`, audit it, then run `screen-02-parity16` unchanged.
No build, test, benchmark or training may accompany the clock games.

ALPHA16's complete 128-game sample passes all game/pair/legality/accounting,
normal-ending and stable-input audits: **56/25/47 W/D/L**, score .53515625,
**+24.47 Elo**, paired-normal 95% **[-32.34,+82.63]**. The full 2-SE/Hoeffding
sensitivities and resources are in `screen-02-alpha16-summary.json`. This is a
promising point estimate with substantial uncertainty, not confirmation or
established gain. The predeclared PARITY16 match follows unchanged; no decision
is made from an interim score and no ALPHA16 sample is extended.

PARITY16's complete sample also passes every audit: **51/22/55 W/D/L**,
**-10.86 Elo**, paired-normal 95% **[-65.72,+43.46]**; retained sensitivities and
resources are in `screen-02-parity16-summary.json`. Its tree savings do not
establish playing strength. Across six formulations and **640 development
games**, ALPHA16 is the selected finalist: the best positive estimate among
the three longer comparisons, a simpler retry mechanism, and concrete relief
of fail-high duplication. This is a research selection, not a statistically
established ranking of policies or an adoption decision.

The clean ALPHA16 study passes **768 requests**. Depth-six node/time ratios are
.89520 / **.88992** (8.699 versus 9.775 seconds), with 14.0% more request-thread
allocation (about 8.3 kB/request). At 100k/1M nodes, time ratios are 1.01057 /
.99964; completed-depth means are 5.9375 / 8.171875 versus 5.71875 / 7.921875.
All fixed-node requests exhaust their budgets. Compilation remains active and
one process GC collection occurs during a candidate depth-six request; these
are bounded warmed measurements, not isolated steady-JIT/cache or GC-causality
claims. All 31 driver/recursive/TT compiled classes match frozen v3 byte for byte.
See `clean-cost-02-summary.json` and the raw per-request evidence.

### Prospective independent ALPHA16 confirmation

This design is frozen **before any partition-1 game**. It prospectively replaces
the initially anticipated single fixed sample with **three predetermined
cumulative looks**, not an extension chosen after seeing confirmation results.
Finalist: unchanged v3 ALPHA16 against the frozen accepted SR-001+SR-029 control,
generation 32, the same mapping, Java/heap/warm-up/TT, 30+0.3, four independent
games, one Search worker per engine, book/tablebases/ponder off. No tuning,
policy switching or development-game pooling is allowed during confirmation.

Select **1,024 unique partition-1 opening pairs**, seed **20261106**, excluding
the SR-001 and SR-029 `confirmation-openings.selection.json` position sets
before the draw. Freeze the entire ordered sample and its consecutive stage
files before stage one. Stages add **256, 256 and 512 pairs** respectively:
cumulative looks after **256, 512 and 1,024 pairs** (512, 1,024 and at most
2,048 games). Each stage runs completely; no within-stage significance stop.
Allow 43,200 seconds per stage as an integrity timeout, not an acceptance rule.

At the observed ALPHA16 development pair SD of about .333, the maximum sample
has SE about .0104. The required 2.5-SE half-width is about .026 score units
(roughly 18 Elo near equality), with about 81% normal-approximation power at the
observed +24.47 Elo effect if that effect/variance generalize. This is planning,
not a promise that the development estimate is true. There is no minimum Elo
gain: qualification requires a credible positive lower bound relative to .5.

Primary method: paired-mean normal intervals with family alpha **.05** divided
equally among the **three** prespecified looks. Each interval uses
`z = Phi^-1(1 - .05/(2*3))` (about 2.394) times the estimated pair SE.
The [NIST Bonferroni inequality](https://www.itl.nist.gov/div898/handbook/prc/section4/prc473.htm)
supports combining a finite set of interval-coverage events without requiring
the looks to be independent. Applying it to these nested looks is the design
inference here; each underlying paired-normal interval remains an approximation,
not an exact finite-sample guarantee. The
[NIST mean-interval method](https://www.itl.nist.gov/div898/handbook/eda/section3/eda352.htm)
also motivates retaining an explicit wider finite-sample sensitivity.

For feature-level false-positive gain control, this two-sided .05 family spends
at most **.025 on positive-tail claims** under that approximate model. Reserve
the other .025 of a feature-level one-sided .05 budget for at most one separately
predeclared, fresh candidate confirmation if subsequent evidence requires it.
No second confirmatory candidate is selected or authorized by this reserve;
ALPHA16 remains fixed throughout this experiment. Report this accounting before
any further confirmatory hypothesis rather than silently resetting alpha.

Qualification at a complete audited look requires **both** its adjusted primary
lower score bound and its **2.5-SE sensitivity lower bound to exceed .5**.
Stop positively then and preserve the unused frozen stages. Stop negatively
only if both upper bounds are below .5. Otherwise continue to the next already
declared stage; at the final look, a nonqualifying result is inconclusive and
cannot justify adoption. Normal eligibility additionally requires nonzero
observed variance and at least **20 pairs below .5 and 20 pairs above .5**.
This conservative dispersion guard does not prove normality; it prevents sparse
one-sided outcomes from qualifying through a spuriously narrow approximation.
Degenerate or insufficiently dispersed samples suppress the normal interval;
only the adjusted bounded-pair Hoeffding interval can then qualify
positive/negative. At every look report that Hoeffding sensitivity with radius
`sqrt(log(2*3/.05)/(2*n))`, including any disagreement. Unadjusted 95% intervals
from the runner are descriptive only and cannot qualify this confirmation.

All raw games, paired clusters, original-window/legality/resource integrity and
input hashes must pass. A failure invalidates qualification and is preserved
for investigation. The assessment script independently re-audits each stage,
rejects overlap/configuration drift and refuses stages after an earlier stopping
decision. Reaching the maximum sample does not automatically close SR-006; review
the substantive evidence and remaining viable hypotheses under the Contract.

The **seven statistical tests** pass, including unadjusted-only significance,
the required wider sensitivity, sparse/degenerate outcomes, invalid look sizes
and pair-cluster variance. Protocol preflight verifies all 1,024 distinct legal
eight-move prefixes, partition membership, exclusion of 293 predecessor positions,
stage boundaries and every frozen input. A real development receipt is rejected
by the confirmation assessor, as required. The committed
`confirmation-protocol.json` and `confirmation-protocol-preflight.json` bind
the design and audit tooling; protocol SHA-256 is
`26400e5b3f629fa55937e47b69e9ac1ad9afe4ec2854b60f12c5d79f14ff78a0`.
Stage directories will be `confirmation-01-alpha16-stage-01`, `-02`, `-03`.
After each complete stage, run `tools/sr006-confirmation.py --protocol` with the
frozen protocol, one `--run` for each completed stage in order, and a new
`--out` receipt. Launch another stage only for the returned `continue` decision.
Never assess or act on an incomplete stage. Keep all timed stages isolated from
builds, tests, diagnostic benchmarks and training.

### Independent confirmation look one

The complete first stage passes the runner audit and independent frozen assessor:
**512 games / 256 fresh pairs**, **194 wins, 128 draws, 190 losses**. All 512
terminations are normal; legal games/full opening histories, balanced pairing,
whole-request resource records, actual runtime configuration and frozen hashes
pass. Wall time is 11,033 seconds. The stage-two host snapshot finds an additional
VS Code JDT process that started during stage one; all three current Java
background processes are idle across a five-second endpoint sample. This remains
an ordinary desktop experiment without exclusive CPU isolation. Endpoint samples
do not establish continuous absence of background contention; retain that
limitation when interpreting small effects. No build, test, benchmark or training
was launched by this workstream during the timed games.
Candidate/control request totals are 34,239 / 34,243, with 38,653,161,079 /
38,641,677,193 nodes. These are match totals, not fixed-work speed comparisons.

The candidate score is .50390625, estimated **+2.71 Elo**. The prospectively
adjusted paired-normal interval is **[-28.49, +33.96]**, required 2.5-SE sensitivity
**[-29.88, +35.35]**, and adjusted Hoeffding sensitivity **[-65.23, +70.87]**.
The normal dispersion guard passes (85 pairs below .5, 86 above .5). All methods
remain inconclusive. This neither establishes benefit nor proves equivalence.
`confirmation-look-01.json` records the audited **continue** decision. No
exploratory games are pooled. The next authorized action is the already declared
stage two: the next 256 frozen pairs, unchanged candidate/control/settings, then
the cumulative 512-pair look. This continuation follows the predeclared rule;
no policy or sample-size change has been made after seeing this result.

### Independent confirmation look two

The second complete stage passes the runner audit and independent frozen
assessor: another **512 games / 256 disjoint fresh pairs**, **200 wins, 106 draws,
206 losses**, all normal terminations, stable inputs and valid full-history,
legality, pairing and resource evidence. Wall time is 11,187 seconds. This owned
match continued while the conversation was interrupted; resumption inspected
its existing receipt and continued monitoring without restarting or changing the
experiment. Endpoint host sampling finds the remaining two Java background
processes idle over five seconds. Continuous host isolation remains unclaimed.

The cumulative **1,024 games / 512 pairs** score **394 wins, 234 draws, 396
losses**, mean .4990234375 and estimated **-0.68 Elo**. The adjusted paired-normal
interval is **[-23.01, +21.65]**, required 2.5-SE sensitivity **[-24.00, +22.64]**,
and adjusted Hoeffding sensitivity **[-48.50, +47.12]**. The dispersion guard
passes (168 pairs below .5, 161 above .5); all methods remain inconclusive.
`confirmation-look-02.json` preserves the independently audited **continue**
decision and both cumulative looks. These results have not replicated the
exploratory ALPHA16 gain, but neither establish inferiority nor equivalence.
The predeclared rule requires the final 512 fresh pairs. No tuning, development
pooling, changed stopping rule or extra sample beyond the frozen design is used.

### Final independent confirmation and feature review - 2026-10-11 local

The last **1,024-game / 512-pair** stage passes all runner and independent
assessor audits, scoring **415/219/390 W/D/L**, with all endings normal and
all frozen inputs stable. Wall time is 22,057 seconds. The three cumulative
looks are preserved in `confirmation-look-03.json`; no additional look or
sample extension is authorized by this design.

The complete independent **2,048 games / 1,024 unique fresh pairs** score
**809 wins, 453 draws, 786 losses**, mean **.505615234375**, estimated **+3.90
Elo**. The adjusted paired-normal interval is **[-12.30, +20.12]**, required
2.5-SE sensitivity **[-13.02, +20.84]**, and adjusted Hoeffding sensitivity
**[-29.77, +37.64]**. The dispersion guard passes (327 pairs below .5, 329 above
.5); every method remains inconclusive. The frozen assessor returns
**inconclusive**, so ALPHA16 does not qualify for adoption. No development games
are pooled. All 2,048 normal endings, legal full-prefix games, reversed-color
pairs, actual runtime identities, whole-request resources, disjoint opening
sets and stable file hashes pass independently reproduced audits.

**Feature-level technical disposition: REJECTED under the investigated
single-worker generation-32 Pair-2 conditions.** This follows the complete
investigation, not merely the maximum sample or an unsuccessful experiment. The candidate's statistical
result remains inconclusive; the feature decision is non-adoption after
proportionate, substantial research found no credible beneficial formulation.
Small positive or negative effects remain compatible with these intervals.
There is no equivalence, universal-inferiority or minimum-Elo claim.

The investigated mechanisms cover immediate full fallback at two empirically
motivated widths, failed-side geometric widening, recent-volatility adaptation,
a one-sided lower window and parity-based centering. They address different
observed failure mechanisms, not an arbitrary parameter sweep. WIDEN16's
exploratory +94.55 estimate did not replicate in its longer 128-game comparison
(+2.71, inconclusive). ALPHA16 addressed concrete upward-jump retry duplication
and gave a promising longer-development estimate, but the independent sample
returned +3.90 with every required lower bound below zero. PARITY16 improved
aggregate tree savings while worsening specific adverse witnesses and
showing no strength support in its complete longer comparison. Wide/adaptive
and immediate-fallback alternatives supplied no credible positive case beyond
these selected mechanisms. No statistically established ranking is inferred
from their small development samples.

The retained adverse traces and actual costs explain why node savings alone
cannot decide adoption: retries repeat expensive qsearch under suppressed
ordinary TT reuse and reset invocation history; selective LMR can change root
scores and moves; request allocation increases; fixed-node throughput is nearly
unchanged. These are measured observations and mechanisms, not a demonstrated
causal explanation of the final near-zero strength estimate. Early/depth/stability
gates or more width constants remain conceivable, but the evidence identifies
no distinct, supported mechanism among them requiring another experiment here.
A further tuning sweep or a fresh sample solely to seek significance would not
supply that missing hypothesis. The reserved second positive-tail allocation is
unused. No affirmative absence of every possible aspiration benefit is claimed.

Changed evaluator/score behavior, a newly evidenced retry/quality failure mode,
or separately authorized ordering/history/TT changes could motivate later
reconsideration through the Frontier. They are neither prerequisites imposed
on Phase 2 nor work launched here. No TT/history redesign, nominal-depth change,
LMR weakening or other Search feature is used to rescue this candidate.

The production factory remains full-window cumulative SR-001+SR-029. Explicit
research policies, their tests/instrumentation and frozen evidence remain
available; `aspirationResearch=off` is still the UCI default and cumulative
control. Exact/reference, unpruned-only/static controls, HCE startup and
multiworker paths are preserved. The conditional integration plan below was
not activated because the adoption gate did not pass.

### Conditional integration checks

Only a qualifying independent result permits adoption. Integrate through the
shared `ProductionSearch` factory for single-worker Pair-2; retain an explicit
full-window qsearch+quiet-LMR control alongside unpruned-only and static controls.
Preserve managed book/root-tablebase composition in normal and control lifecycle
services. Verify Play, self-play, validation, convenience-driver and UCI consumers,
HCE/other evaluators and drained multiworker transitions. Broader quiet-history,
TT policy and recursive Search remain outside this change.

The existing SR-029 UCI bridge removes only the qsearch/LMR process properties
when testing defaults. Its command helper now also supplies aspiration=off, so
reusing that check unchanged would fail to exercise a future aspiration default.
The SR-006 production bridge must omit **all three policy properties** for the
real default, and compare explicit ALPHA16 to the frozen confirmed installation
at deterministic depth/node limits with full histories. Keep explicit off-policy
comparisons to the accepted cumulative baseline. Recheck actual identities and
optional root outcomes, then run the appropriate integration regressions before
claiming IMPLEMENTED. No production default has been changed during research.

## Final validation and handback

The final retained-code regression command
`:app:test -Pheadless --offline --tests com.ohinteractive.seedv6.search.* --tests com.ohinteractive.seedv6.uci.* --tests com.ohinteractive.seedv6.rules.*`
passes **670 tests**, **11 pre-existing disabled**, **zero failures/errors**
(681 discovered, 94 suites), exit zero, in 2m58s. The log's PowerShell
`NativeCommandError` wraps the JVM incubator warning; the actual Gradle result
and XML establish success. `final-regressions.json`, `.txt` and the copied XML
preserve the evidence. This broad run covers the final v3 code after the earlier
broad v1 suite; no builds/tests overlapped confirmation games.

Earlier validation remains applicable: 18 focused v3 tests, 1,242 real-model
independent exact-qsearch comparisons, both full-prefix diagnostic corpora,
798 deterministic old-policy reproductions, 278 v1/v2 counter-bridge comparisons,
actual UCI node/time/stop/terminal/invalid-configuration checks, ten runner tests
and seven confirmation-statistics tests. Failed fixture/CLI/freeze attempts and
corrected reruns remain preserved. No unresolved semantic defect is known.

`final-static-integrity.json` verifies all **702 rebuilt main class entries**
byte-identical to frozen v3, both Phase 1 archives unchanged, root journal absent
and version build 35 unchanged. The production factory remains cumulative
full-window qsearch+quiet-LMR; explicit aspiration is research-only. Existing
factory, lifecycle, consumer and UCI regressions pass. The conditional production
adoption bridge was not activated because no aspiration production integration
was made; deterministic retained-policy bridges and the full class comparison
cover the unchanged code. No visual GUI/browser QA or unrelated full application
suite was run for this engine/research-only change. Long clocks, other weights,
evaluators, platforms and multiworker aspiration strength remain unmeasured.

The feature is technically resolved as **REJECTED** within these conditions.
All 640 development games and 2,048 independent confirmation games remain
separate. The ordinary desktop host was not exclusively isolated; JVM/JIT and
allocation limits, approximate paired inference and wider bounded sensitivity
are explicit. No universal inferiority, equivalence or zero-effect claim is made.
The optional second confirmation allocation remains unused. No future feature
is activated, and no dependency is silently changed to rescue aspiration.

Attributable implementation/research/evidence commits retain the frozen sources,
important negative results, raw games and failures. The final closeout commits
record the remaining evidence and canon. No inherited changes were present at
launch, no unrelated work was absorbed, and no push/deployment/history rewrite
was performed. Frozen ignored installations are intentionally retained as
reproducibility evidence; no manual cleanup is required. Root journal remains
absent. Build 35 remains under the documented non-blocking version warning;
there is no verified bump, replacement finalization or manual compensation.
`final-version-status.json` confirms idle state with no active operation,
unfinished work or blocker. The final canon check verifies all 38 dispositions;
only SR-006 changes, and both new revision-history entries are unique.

**CANON_UPDATED:** P2-R004 -> **P2-R005**.
**FRONTIER_UPDATED:** P2-F006 -> P2-F007 activation -> **P2-F008** closeout.
**PROJECT_SOURCE_REFRESH_REQUIRED: YES.** Repository masters are authoritative;
Codex has not synchronized Project Source mirrors or claimed owner acceptance.

**Human actions required after this prompt:**

- **Blocking dependent programme planning and successor launch:** owner replaces
  BOTH Project Source mirrors from `source/CHESS_SEARCH_CONTRACT.md` (P2-R005)
  and `source/CHESS_SEARCH_RESEARCH_FRONTIER.md` (P2-F008), then explicitly confirms
  synchronization. A requested refresh is not evidence of completion.
- **Blocking acceptance and successor selection:** GPT/owner reconciles and
  accepts or redirects this technical closeout. The repository disposition and
  commits do not establish human acceptance.

No internal validation, cleanup or version-repair action remains for the owner.
Deployment remains separately authorized work and was not performed.
