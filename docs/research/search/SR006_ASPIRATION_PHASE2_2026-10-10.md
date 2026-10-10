# SR-006 Phase 2: aspiration windows

2026-10-10 local. **ACTIVE**, authorized feature CGLHW. Only SR-006 is selected.
This report and the existing Frontier provide continuity; the Search Contract
remains governing authority. No aspiration adoption or strength claim yet.

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

## Current continuation

Frontier P2-F007 remains ACTIVE for SR-006 only. Diagnostics, broad regressions
and real-model oracle checks passed. Frozen v1, off-policy bridge and A/A passed.
The four predeclared exploratory matches and clean-cost study completed;
WIDEN16's 128-game 30+0.3 development comparison is running. Before resumption,
inspect owned processes and receipts rather than starting duplicates.
After it, declare an adequately sized independent confirmation design before
any held-out games. No confirmation protocol or adoption decision exists yet.
Production defaults,
exact/reference, HCE and multiworker paths are preserved.
No external action blocks this internal work. At feature handback, owner/GPT
reconciliation and replacement/confirmation of both current Project Source mirrors
will be required before a successor can be planned or launched. No deployment.
