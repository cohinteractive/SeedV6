# Phase 3: TT Information Effectiveness and Reuse

Owner-authorized CGLHW, 23 October 2026 (Pacific/Auckland). **TECHNICALLY COMPLETE:
awaiting GPT/owner reconciliation and acceptance; production remains WIDEN16.** This is subordinate evidence and continuity
under [Contract P3-R018](../../../source/CHESS_SEARCH_CONTRACT.md) and
[Frontier P3-F018](../../../source/CHESS_SEARCH_RESEARCH_FRONTIER.md), not a new
contract or an acceptance/promotion authority.

## Final disposition and correct resume point

Completed: diagnostic-v1, followup-v1 (aspiration reverse ablation and cross-request
lifetime), deeper-v1 (different roots, W1/W4), full-window correctness and ordinary
UCI baseline parity. Their frozen protocols, bindings, receipts and summaries are
in the linked experiment directory. Pilot-v1 passed all four operational games.
The frozen 32-pair W1/W4 development completed: full-window scored 25/12/27 W/D/L
at W1 and 30/7/27 at W4, with broad intervals crossing zero. Neither meets its
frozen 0.55 confirmation-selection gate. Lifetime-full-v1 is complete (390 neutral
pairs), with maximum occupancy 37.56% and no justified retention-policy change.
Adaptive-v1 passed 11 test methods and all four diagnostics-OFF cost domains.
Both worker domains meet its frozen cost/completion gate. The separate
adaptive-clock-v1 protocol, 32 new paired openings per W1/W4 domain at 10+0.1,
passed clock-runtime-v2 identity/parity qualification and opening binding. Both
domains finished: W1 30/19/15 and W4 28/18/18 W/D/L. All 128 games pass legal,
terminal/history/request and lifecycle audits; neither is confirmation. Both
pass the score-only 0.55 gate, but later host reconciliation identifies substantial
non-engine load during W4. Its full resource gate is NOT passed. The original
summary's automatic score-only eligibility label is superseded by the resource
disposition below, without altering results or the closed protocol. W1 subsequently
completed fresh independent confirmation; W4 subsequently completed a separate
quiet-host development screen, without pooling or extending closed data.
Access-profile-v1 completed without an obvious TT access concentration. No TT
policy change is justified. The CP64 block adaptation passed26 continuation,
identity, process-fault and resource-gate tests. Four studies were frozen in
[tt-session-v1-binding.json](p3-tt-effectiveness-2026-10-23/tt-session-v1-binding.json)
under [the prospective protocol](p3-tt-effectiveness-2026-10-23/adaptive-confirmation-v1.md):
two-block operational pilot; W1 128-pair confirmation; W4 fresh32-pair resource
requalification; conditional W4 128-pair confirmation.
The owner answered: **wait until the workstation is quiet, then continue**.
The controller started at2026-10-23T05:49:12Z; its initial quiet-host check passed
(0.59 busy logical cores). Both pilot blocks completed, including pause/resume,
export, game and resource audits (other host load0.71/0.67 logical cores).
W1 confirmation completed all128 pairs by2026-10-23T08:11:19Z, with a primary
pair-normal97.5% interval supporting a gain (details below). All four blocks pass
resource/game audits; one failed pre-block admission caused a15min wait and no
games were consumed or excluded. The subsequent resource-qualified W4 screen
finished28/13/23 W/D/L (53.91%), below its frozen55% selection gate. Its result is
inconclusive, not a rejection or equivalence claim. W4 confirmation was therefore
never launched; its frozen conditional inputs remain preserved. The controller
exited successfully. There are no active research games or remaining automatic
work units, and no need to rerun the controller.

The next admissible action is **GPT/owner reconciliation of these findings**,
not another experiment or production change. ADAPTIVE16 is a validated W1 research
configuration under the primary test's assumptions; other worker/resource/clock/
platform domains remain unconfirmed. Full-window remains a conditional CPU-saving
configuration with strength unconfirmed. Further broad enumeration, width tuning,
TT redesign or proof relaxation is not justified by current evidence. No binding
TT owner decision is requested. Any production promotion or independent successor
requires separate owner authorization; this record does not grant acceptance.

All failed, negative, inconclusive and frozen evidence is preserved. Raw inputs,
plans, games, JFR and other bulk outputs are local-only under `raw/` and absent from
a fresh checkout. Check availability before reanalysis; use the tracked bindings,
summaries and exact source/configuration/model/tool identities. Do not replay,
overwrite or extend closed names/protocols. Runtime baseline, CP64 and TT policy
remain unchanged. Both Project Source mirrors need the final revisions below.

## Verified bootstrap

- Clean starting main HEAD `43b690f` (full identity in bootstrap-validation.json).
  Runtime source matches `18f3c4f12a01417a48fb4c044ff3bde2c5b44b2d`: Git reports
  no changes since that anchor in engine-core, app-uci, app-gui or engine-training.
  Root VERSION_STATE.txt is 0.0.0 build 69; root CODEXLOG_CURRENT.md is absent.
  Local AGENTS.md expressly removes finalizer dependencies. No finalizer invoked;
  no journal created. Commit-based build increments apply only to shipped changes.
- Canon headers were P3-R016/P3-F014 as the owner stated. Launch supersedes earlier
  no-successor wording, preserving historical evidence. Baseline source inspection
  confirms BaselineEngine -> ConstructionSearch.v1, flags 262143/WIDEN16,
  configurable 1-16 workers and requested default Hash64. Runtime QA is recorded below.
- Actual frozen CP64 file is 3,940,952 bytes, SHA-256
  `8431626c2fca81e0a1e9ccd5e45ddd8829e6e3282c756c21fad75b06d075bb2b`.
  Loader checks length/hash, S6CNU002 and eight heads. Its manifest describes the
  historical E008 export; it is not authority to replace the selected composition.
- Unified architecture preserves one shared core; research can consume core,
  production cannot depend on research. Use worktree-local Gradle home
  `build/migration/gradle-user-home`, constrained parallelism, existing runners.
- Existing incomplete final Windows GUI interaction and native macOS acceptance
  are unchanged qualifications, not blockers to this headless objective.

## Lifecycle model and programme challenge

Static inspection establishes the following implementation facts, not runtime
verification: TTable has three long arrays, direct mapping and 32 coherent striped
locks; Hash64 requests 2,097,152 slots / 48 MiB primitive payload. Hash72 would
have identical capacity. SearchKey preserves full status and ordered relevant
real history. Each request advances generation, retaining legal older hints;
eight-bit generation wrap clears data. Ordinary proof needs same depth/current
generation, cutoff-only admission and selective eligibility. Full Construction
enables extension policies and excludes ordinary proof globally, even in subtrees
which use no extension. Quiescence and restricted root jobs are not ordinary proof
producers. Completed legal hints remain separate, with legal membership checks.
Hints cannot displace proof; deeper current-generation hints survive, equal-depth
completions refresh, and old-generation hints may be replaced. Current consumers
probe ordinary proof first and then hints, so zero proof is not zero TT traffic.

Prior Policy 4 and MTD records were consulted for mechanisms and controls. Their
Pair-2 measurements are historical, not CP64 measurements. Existing Construction
records explicitly document zero ordinary proof under extension policy. This
launch does not reopen those experiments or their outcomes.

Challenge decisions before substantial execution:

1. Do not infer a defect from sparse proof. Establish actual hint usefulness by
   counterfactual complete-request controls, rather than reward higher hit rates.
2. Do not redesign retention before separating absent producers, identity misses,
   pressure losses and consumers. Observe current storage without modifying it.
3. Do not relax proof, identity, layout, locks or replacement experimentally.
   Selectivity and extension suppression are legitimate limits. Any alternative
   proof/retention policy needs a bounded owner decision with evidence and costs.
4. Include current CP64, aspiration retries, aborted work and worker drainage.
   The old flat Pair-2 or recursive E008 runner is not the selected comparator.
5. Start with development fixtures and deterministic observation parity; use
   fresh frozen openings for any independent confirmation. Small diagnostic
   depth/node savings alone cannot establish strength or production adoption.
6. Avoid a large new tracing/orchestration framework. Reuse existing TTable
   subclassing, optional diagnostics, SearchDriver, full-history fixtures and
   tournament tools. Instrumented costs remain separate from clean timing.

## Dynamic hypothesis and decision queue

| ID | Hypothesis / discriminator | Current disposition and next gate |
| --- | --- | --- |
| T1 | Completed hints materially reduce current CP64 request work despite no ordinary proof. | SUPPORTED mechanism value in diagnostic-v1: hints-off increases D6 work/time at W1/W4, despite slightly cheaper fixed-node requests. Not Elo evidence. |
| T2 | Current capacity/replacement loses consequential guidance. | Full-trajectory check complete: maximum37.56% occupancy, no saturation demonstrated; retained work benefit concentrated early, later results nearly neutral/slightly adverse. No capacity/replacement redesign justified in tested domains; long clocks remain untested. |
| T3 | Aspiration/PVS/iteration or root guidance consumes hints ineffectively. | Full-window reduces request costs but misses its strength-selection gate. Existing ADAPTIVE16 passes diagnostics-OFF cost gates and W1 primary independent confirmation; W4 remains inconclusive. Useful consumer improvement without a TT redesign; no width sweep. |
| T4 | TT access/coordination costs consume the information benefit. | JFR observed 7/3764 W1 and 14/4552 W4 Java samples with TTable frames, no monitor-entry event above10ms. No obvious concentration; small/inlined costs and short waits remain unquantified. No locking/layout change justified. |
| T5 | Producer depth/selective restrictions leave valuable proof or guidance unavailable. | Zero ordinary proof confirmed; completed hints provide value. No evidence yet justifies weakening proof restrictions or binding-policy experimentation. Empty proof probes/repeated writes are traffic observations, not permission to alter TT access/replacement. |
| T6 | A promising integrated candidate improves practical strength. | Adaptive W1 fresh128-pair confirmation supports a gain under the primary pair-normal test; conservative bounded sensitivity crosses zero. W4 fresh resource-qualified screen is inconclusive and below55%, so no confirmation launched. No cross-domain extrapolation or promotion. |

Select the next question by expected decision value. Negative findings may close
formulations; incomplete/resource-limited evidence stays inconclusive or deferred.
No unrelated evaluator/GUI/training/hot-path programme, MTD redevelopment or
automatic successor. Closure requires a defensible lifecycle account and supported
dispositions of credible affordable opportunities, not an arbitrary positive Elo.

## Closed diagnostic-v1 findings

Source/experiment bootstrap committed as `09e126a`; model and runtime hashes are
in [diagnostic-v1-binding.json](p3-tt-effectiveness-2026-10-23/diagnostic-v1-binding.json).
All 24 plain/observed full-request pairs match, including aborted work. All 576
cost requests (144 warmups, 432 measured) pass accounting, PV and helper-drain
checks. No production class/source policy changed; build69 remains selected.
Ordinary proof probes/hits/stores in baseline observation: **765,161 / 0 / 0**.
The second probe supplies **245,411 hints (32.07%)**. Hash64 stores 508,294 hints;
1,845 collision writes versus Hash1's 81,216. Same-key refreshes overwhelmingly
repeat the retained move (221,800/224,582), but replacement is owner-reserved and
these counts alone cannot justify suppressing a write or changing retention.

| Control / baseline | W1 D6 elapsed totals | W4 D6 elapsed totals | W1 D6 node geometric ratio | W4 D6 node geometric ratio |
| --- | ---: | ---: | ---: | ---: |
| Hints off | 1.233 | 1.327 | 1.225 | 1.317 |
| Hash1 | 1.020 | 0.998 | 1.009 | 0.996 |

D6 requests all complete. At 250k admitted nodes, hints-off elapsed totals fall
to 0.957/0.949 of baseline (W1/W4), while completed depths shift down. This is a
cost-benefit tradeoff, not proof of stronger play or a reason to remove hints.
Hash1 reduces hint availability to 27.41%; bounded request depth changes are
small and selective, not a default-memory recommendation. Multiworker process
CPU ratios do not equal elapsed ratios; see the summary, including a 1.127 Hash1
D6 CPU ratio despite nearly equal elapsed time. Requested Hash64 is unchanged.

In the 12 baseline observation requests, 60/154 passes return completed failed
aspiration bounds, consuming 863,603/2,979,366 nodes (28.99%); 11 further passes
abort at the full-request cap. Failed passes produce reusable guidance, so the
next controlled question is their net value, not simply reducing their count.
The full five-run cost was 177.5 seconds wall / 293.2 process CPU seconds; peak
working set is in per-run receipts. Instrumented plain-then-observed timing lacks
warmup/order control and is not an overhead estimate. Existing Construction
counters reset on abort; they omit aborted invocation mechanism counts, while
table counters and whole-request nodes/time include that work. This limitation
is retained rather than silently treating missing counters as zero work.

Cost-mode uses the ordinary uninstrumented TTable, but enables the existing
worker diagnostics to reconcile all entered nodes. "Clean" in harness comments
therefore means no observing table, not all diagnostics disabled. Its CPU/time
ratios apply to that shared diagnostic setting, and diagnostic overhead was not
independently estimated. Do not quote those ratios as a production-speed claim.
Clock engines use default diagnostics OFF (`seedv6.searchDiagnostics` is absent,
and the existing runner removes ambient JVM injection variables); game/request
telemetry and native lifetime cost provide the independent practical domain.

No clock games, independent strength confirmation, new TT policy, candidate
acceptance or production promotion follows from this diagnostic.

## Closed followup-v1 and deeper-v1

[Followup protocol](p3-tt-effectiveness-2026-10-23/followup-v1.json),
[summary](p3-tt-effectiveness-2026-10-23/followup-v1-summary.json),
[deeper protocol](p3-tt-effectiveness-2026-10-23/deeper-v1.json) and
[summary](p3-tt-effectiveness-2026-10-23/deeper-v1-summary.json) retain all outcomes.
At W1 D6, full-window uses 0.805 of baseline total nodes and 0.775 elapsed time.
Hints-off costs more with both drivers: hints-off/full-window uses 3,381,363
nodes versus full-window's 2,625,123. Thus the opportunity concerns consumer
interaction, not simply removing information production. At a fixed 250k nodes,
full-window completes fewer D7/D8 requests than WIDEN16; do not hide that negative
depth result or generalize the shallow timing gain.

On twelve different historical full-history development roots, target D8/2m cap,
two measured rounds after one warmup, full-window completes all 24 requests at
each of W1/W4; WIDEN16 completes 20/24 at each. Full-window/baseline elapsed totals
are 0.602 (W1) and 0.719 (W4); process CPU ratios 0.592/0.583, nodes
15.49m/25.09m and 14.98m/24.01m. All selected moves match on these roots. Capped
requests remain included, so these are not equal-completed-depth speedups. No
Elo inference follows. The two JVMs took 167.2 seconds wall in total.

Lifetime-v1 replays four actual CP64 game trajectories from the prior baseline
comparison, selected before measurement, 32 requests each at plies 16-47. All
128 retained plain/observed requests match. Of 2,685,618 hint hits, 108,178 are
older-generation (4.03%); maximum observed occupancy is 16.58%. Ordinary probes
6,451,856, hits/stores zero. Retaining TT across requests completes D7 in 105/128
requests versus 99/128 with per-request clearing; total nodes are 33.58m versus
35.25m, including 23/29 caps. The 98 jointly completed cases use a 0.961 node
ratio, a conditionally selected subset, not a universal speedup. Twenty selected
moves differ; no playing-strength claim. These are four trajectories, not 128
independent games. Lifetime timing lacks warmup, so node/depth/age comparisons
are primary. Its peak working set, 876 MB, belongs to the six-owner diagnostic
JVM and must not be represented as one production engine's memory. The three
followup runs took 302.4 seconds wall, including 181.2 for lifetime-v1.

Generation age is last-write age, not causal information age: an old hint may
guide a completed search which refreshes it into the current generation. The 4%
older-hit share therefore does not bound the benefit of cross-request information.
The retained/cleared control is the relevant counterfactual. Source inspection of
`ExactSearch.beginRequest/endRequest/newGame` shows that feedback and iteration
guidance are already cleared per request; in this W1 composition `newGame` adds
TT clearing. Rich Construction state is invocation-local. This supports the
retention interpretation without asserting that every table hit was decisive.

## Candidate and clock qualification

The candidate is existing `ConstructionSearch.v1(workers,64,null)`, full-window
PVS with all 18 controls and frozen CP64. It changes no TT identity, layout,
locking, replacement or proof rule. The research launcher lives in the existing
root research seam and the adapter reuses established Fastchess, legal-history
auditing and Windows owned-process/lifetime accounting. No production dependency
on research or second Search implementation is introduced.

`ConstructionTtFullWindowTest` passed independent CP64 accumulator rebuild/PV
comparisons on six representative boards (including EP, promotion, mate and draw)
and budget, cancellation, new-game, expiry, drainage and reuse checks for every
worker count 1-16. These are two test methods with multiple cases, not two cases.
`ConstructionTtStudyTest` passed three methods including legal replay of all four
lifetime fixtures. Existing exact/TT-off/mate/history suites passed in bootstrap;
no proof eligibility changed. Boundaries and research workspace build pass.

UCI qualification v1 passed both profiles at W1/W4/W16 but its attempted parity
check failed: `UciMain --nnue` activates the deliberately preserved historical
recursive opt-in, not ordinary build69 Search. This was an experimental comparator
error, not evidence of a baseline defect. Its script, runtime, transcripts and
failed receipt are immutable evidence. [Qualification v2](p3-tt-effectiveness-2026-10-23/uci-qualification-v2.json)
uses no-argument ordinary UCI; identity and node/score/depth/move/PV parity pass
on startpos and two complete-history roots at 100k nodes. The frozen research
launcher warms up its actual profile for 200k nodes before UCI handshake; clocks
begin afterward and process lifetime CPU still includes startup/JIT/GC/drain.

The [operational pilot](p3-tt-effectiveness-2026-10-23/pilot-v1-summary.json)
passed all four games with independent legal/history/result and telemetry audit,
stable inputs and clean exits. It is deliberately excluded from strength inference.
It took 129.6 seconds match wall time, 76.3/76.7 lifetime CPU seconds and
361.8/315.5 MB peak working set for baseline/candidate. The two engines do not
search simultaneously in a one-game cohort. Pilot-scaled estimates are 34.6
minutes per 32-pair development domain and 2.3 hours per 128-pair confirmation;
game length/desktop availability can change these. The development protocol
freezes both worker domains separately at 10+0.1, selects no intermediate result,
and prospectively specifies the conditional confirmation gate. The new PGNs are
frozen opening inputs covered by the existing narrow `*-openings.pgn` exception;
generated tournament PGNs and runtime copies stay ignored under `raw/`.

## Closed full-window clock development

[Frozen protocol](p3-tt-effectiveness-2026-10-23/development-clock-v1.json) and
[summary](p3-tt-effectiveness-2026-10-23/development-clock-v1-summary.json) cover
32 opening pairs per worker domain at10+0.1, same frozen opening set with colors
reversed, one game at a time. All128 games finish normally, independently replay
with correct legal/terminal/history results and complete request telemetry, and
all engine processes drain/exit. There is no interim stopping, selective replay,
sample extension or adjudication. Opening pairs across worker domains are
correlated and are not pooled.

| Full-window minus WIDEN16 | W1 | W4 |
| --- | ---: | ---: |
| W / D / L | 25 / 12 / 27 | 30 / 7 / 27 |
| Score | 48.44% | 52.34% |
| Descriptive logistic Elo | -10.9 | +16.3 |
| Approximate paired-normal95 Elo | [-99.9,+76.8] | [-67.4,+102.0] |
| Conservative bounded-pair95 Elo | [-196.2,+167.9] | [-161.2,+203.6] |
| Candidate/baseline process CPU | 0.999 | 0.789 |
| Candidate/baseline request nodes | 568.23m /560.28m | 761.17m /881.30m |
| Candidate/baseline peak working set | 361.7/380.2 MB | 388.1/390.4 MB |

Both strength results are statistically inconclusive. Neither reaches the frozen
0.55 resource-selection gate for substantial confirmation; that gate is not an
inferiority or equivalence test. No independent confirmation was run and no gain,
noninferiority or broad failure of full-window is claimed. W1 is unpromising for
further expenditure under this screen; W4 remains a conditional CPU-saving
configuration whose strength is uncertain. CPU falls about21% in W4 while equal
clocks are held; equal-clock observations do not establish equal-compute strength.
Different played positions/trees prevent treating the game node ratio as a
fixed-position performance ratio. The diagnostic advantage did not become a
demonstrated playing-strength advantage.

Matches took1786.0/1688.1 seconds (57.9min combined). Full engine-lifetime CPU is
897.7/896.9 seconds baseline/candidate at W1 and1945.3/1534.6 at W4, including
startup/JIT/GC/aborted work/drain. Post-start read-only host samples observed
15.4% aggregate non-idle logical CPU during W1 and25.5% during late W4. This does
not establish isolation, affinity or freedom from localized contention; desktop
activity remains a limitation. See local `raw/host-cpu-development-*.json` and
`raw/host-cpu-w4-*.json`; initial minutes are outside those windows.

## Closed full-trajectory retention

[Lifetime-full protocol](p3-tt-effectiveness-2026-10-23/lifetime-full-v1.json)
and [summary](p3-tt-effectiveness-2026-10-23/lifetime-full-v1-summary.json) cover
all390 supplied game prefixes from ply16 through each actual game end. Every
retained plain/observed pair matches exactly. The first128 requests reproduce the
previous early/middle results; they are not additional independent samples.
Maximum occupancy reaches37.56%, final per-owner occupancy14.06%-37.56%.
13.33m ordinary probes still produce zero ordinary proof hits/stores; 5.80m
table hints are available. Of9.99m writes,0.623m replace colliding keys and5.35m
repeat the prior same-key move. Record-age1/2 hits total217,836 (3.75%). These
are traffic/provenance diagnostics, not counts of decisive move improvements.

| Request window | Retained / cleared total nodes | Target-D7 completions retained / cleared | Node ratio among jointly completed requests |
| --- | ---: | ---: | ---: |
| Plies16-47 (replayed) | 33.58m /35.25m | 105/99 of128 | 0.961 |
| Plies48-79 | 17.96m /17.98m | 121/121 of122 | 0.999 |
| Plies80+ | 12.80m /12.71m | 136/136 of140 | 1.015 |

Across all prefixes, retention uses64.34m versus65.94m nodes with28 versus34
caps, but measured request CPU is essentially equal (111.156/111.125 seconds).
The retained benefit is concentrated early; later retention is nearly neutral
or slightly adverse in this small selective-search sample. It is not uniformly
beneficial and the59 differing selected moves have no quality adjudication.
Four trajectories cannot establish a universal policy. Neither occupancy nor
collision counts demonstrate a capacity bottleneck, and this evidence does not
justify a binding retention change, table expansion, or clearing in production.
Long-clock/higher-pressure domains remain untested. The complete six-owner
diagnostic took321.9 wall/333.0 CPU seconds, peak working set865.3MB.

## Remaining bounded adaptive formulation

The full-window result does not close every aspiration formulation. The existing
ADAPTIVE16 policy derives its initial width from the last two completed scores,
retains bounded search, and preserves existing widening/mate verification. Current
CP64 retry pressure provides a specific Phase3 reason to test this single control;
historical Pair-2 costs had a different retry regime. No new TT policy or parameter
grid is introduced. [adaptive-v1](p3-tt-effectiveness-2026-10-23/adaptive-v1.json)
freezes W1/W4 target-depth/fixed-node controls with diagnostics OFF and a modest
cost/completion gate before any additional clock expenditure. Failed gates close
or defer that formulation; they do not trigger an automatic width sweep.

All four adaptive cost runs passed. In target-D8/2m-capped requests, adaptive
finishes 22/24 versus baseline 20/24 at both W1/W4. Elapsed ratios are 0.764/0.754,
CPU ratios 0.749/0.780 and nodes 19.56m/25.09m (W1), 18.73m/24.16m (W4). Selected
moves agree on those roots; capped requests remain included. At fixed 250k nodes,
W1 has two higher and two lower completed depths, with twenty equal; W4 has seven
higher, seventeen equal, zero lower. Fixed-node elapsed ratios 0.973/1.034 and CPU
0.951/0.971 show that cheaper target-depth work is not a uniform NPS gain. These
are diagnostics-OFF complete-request costs, not playing-strength evidence.
All 288 requests (96 warmups, 192 measured) satisfy accounting/legal-PV/drain
checks; four JVMs total 230.7 wall /384.7 native CPU seconds. See
[adaptive summary](p3-tt-effectiveness-2026-10-23/adaptive-v1-summary.json) and
[validation](p3-tt-effectiveness-2026-10-23/adaptive-validation-v1.json).

Both domains pass the prospectively frozen cost/completion gate, admitting the
separate [adaptive clock screen](p3-tt-effectiveness-2026-10-23/adaptive-clock-v1.json)
on 32 fresh partition0 openings. It retains the same 10+0.1 clock, one-game
concurrency, CP64/Hash64, background priority, warmup and complete game audit.
This is the only additional aspiration formulation; prior full-window selection
and two worker domains remain exploratory multiple comparisons. New independent
confirmation requires its frozen strength-selection gate; it is not automatic.
The research launcher/adapter add only the existing adaptive activation. Exact
closed v1 source bytes are preserved under `raw/clock-v1-source`, in
[its source manifest](p3-tt-effectiveness-2026-10-23/clock-v1-source-preservation.json)
and Git commit8e933ea; v1 runtime, protocols and results remain untouched.

## Validation and local evidence

### W1 independent confirmation endpoint

The [frozen W1 endpoint](p3-tt-effectiveness-2026-10-23/tt-session-w1-v1-summary.json)
contains128 new partition1 opening pairs/256 games at10+0.1, W1/Hash64/CP64,
one-game concurrency, diagnostics OFF and unchanged ADAPTIVE16 versus WIDEN16.
All four32-pair blocks passed complete schedule/color, legal/terminal/history,
request telemetry, source/runtime identity, native lifecycle and host audits.
One failed resource admission paused before block1; it launched no games and
did not remove or replace an outcome. No interim strength looks occurred.

ADAPTIVE16 scored **120/58/78 W/D/L**,58.2031%, descriptive Elo **+57.52**.
The prospectively primary paired-normal97.5% score interval is[0.520468,0.643595],
Elo[+14.23,+102.67], supporting a gain under its normal/exchangeable-pair
assumptions. Alpha0.025 is allocated to each worker domain, retaining the frozen
two-domain familywise allocation even if W4 is not admitted. The conservative
bounded-pair97.5% sensitivity interval is[0.451198,0.712864], Elo[-34.02,+157.97],
and crosses zero. Report that sensitivity; do not present this as assumption-free
proof or universal strength. No pooling with development, historical or W4 games.

Native game-engine CPU is3666.66s candidate /3663.86s baseline (ratio1.00076),
peak working sets392.22/392.59MB, complete request elapsed3621.80/3617.95s, and
nodes2.29614b/2.27264b. Thus the clock gain is not a lower-CPU result; the cheaper
target-depth diagnostic work does not translate to less CPU at fixed clocks.
Whole owned cohort CPU including preflight/coordinator is7384.92s, match wall
7270.50s (~121.2min). Non-owned mean host load per block is0.939/0.771/0.795/0.881
logical cores, within the frozen gate; no isolation or burst-bound claim follows.
This is a validated research configuration in the tested Windows/JDK21/CP64/W1/
Hash64/10+0.1 domain. Owner acceptance and any production promotion are separate.

### W4 resource-qualified screen and final selection

The [fresh W4 endpoint](p3-tt-effectiveness-2026-10-23/tt-session-requal-w4-v1-summary.json)
passes all64 game, identity, lifecycle and resource audits on32 new partition0
opening pairs. ADAPTIVE16 scores28/13/23 W/D/L,53.90625%, descriptive Elo+27.20;
paired-normal95% Elo[-51.34,+108.67], bounded95%[-148.03,+219.00]. It misses the
frozen55% confirmation-selection gate. This supports an inconclusive/deferred
W4 disposition, not inferiority, equivalence or a pooled result with the earlier
resource-unqualified W4 screen. No W4 confirmation games were launched, and its
unused alpha allocation is not transferred to W1. There is no further screen.

Native game-engine CPU is1882.48s candidate /1870.27s baseline (ratio1.00653),
peak working sets390.06/389.66MB, request elapsed825.37/824.24s and nodes872.40m/
854.67m. Match wall is1656.38s (~27.6min), full owned-cohort CPU3766.27s. Other
host load averages0.974 logical cores, passing the prospective gate. The earlier
diagnostic savings again do not establish a clock CPU reduction. Confirmation
would require a separately justified prospective decision; no retrospective
extension, retuning or replacement is authorized by this result.

### Closure rationale, limits and human actions

The lifecycle account, independent controls, retention and access observations,
two economical consumer formulations and fresh W1 confirmation answer the selected
credible questions. The evidence supports useful existing legal hints and a
domain-limited consumer improvement; it does not justify a TT-policy redesign.
Longer clocks, higher retention pressure, other workers/platforms, small/inlined
access costs and alternative binding proof policies remain explicit uncertainties.
No current discriminator offers enough expected decision value to justify another
width sweep or an automatic expensive study. These deferrals do not assert that
no further improvement exists. Keep the selected build69/WIDEN16 baseline pending
the owner's separate acceptance/promotion decision.

No shipped application source or CP64 bytes changed. Research instrumentation,
launchers, tooling, targeted tests, protocols, fixtures and reports are committed;
build69 remains unchanged because these are research/test/documentation changes.
Root CODEXLOG_CURRENT.md is absent and was not created. No finalizer was invoked.
The initial worktree was clean; all work here is attributable to this CGLHW.
No push, deployment, external publication or successor occurred.

[Final reconciliation v2](p3-tt-effectiveness-2026-10-23/closeout-v2.json) verifies
54 receipt-bound raw files, frozen block inputs/completed outputs and584 legal/
terminal/history/request-audited games, including8 operational pilot games and
256 independent W1 confirmation games. The earlier resource-unqualified W4 screen
is included in cost/accounting totals, not promoted to qualified strength evidence.
All recorded owned game processes drained; the conditional W4 confirmation has
zero launched blocks. Clock matches total16095.71s wall (~4.47h) and22208.55
game-engine CPU seconds (~6.17 CPU-hours). Diagnostic/profile JVMs add1269.67s
wall and1715.84 CPU seconds. These are measured research costs, excluding Gradle/
tests, selection/analysis overhead, uncounted preflights and idle availability waits;
they are not total workstation time or a pooled performance/strength estimate.
The preserved closeout-v1 cost sum omitted six Java Job-accounted receipts;
v2 adds those verified receipts without changing experimental results or audits.

Validation was scoped to affected Search semantics and research execution: the
bootstrap suites and corrected stale HCE assertion; exact/TT-off references;
CP64 incremental/rebuild and legal-PV cases; full-window/adaptive budgets,
cancellation/reset/helper draining at supported1-16 workers; ordinary UCI baseline
parity and1/4/16 profile identity; dependency boundaries; and26 block-continuation,
immutable-input, resource-admission and native process-fault tests. Frozen game
stages pass the audits specified in their receipts. This is not a claim to rerun
every repository test or to complete packaged Windows GUI/native macOS acceptance;
those inherited qualifications remain unchanged. Diagnostics-ON timing, JFR
sampling, desktop load and statistical sensitivity limits remain recorded above.

Human actions required after this prompt:

- **Blocking acceptance only:** GPT/owner reconciles and accepts or dispositions
  the completed CGLHW. No production promotion is implied; any such change needs
  separate explicit authorization and its applicable validation.
- **Blocking future mirror-dependent planning/next launch, non-blocking this
  completed repository work:** replace both external Project Source mirrors with
  Contract P3-R018 and Frontier P3-F018 and confirm synchronization. Codex has not
  synchronized them. `PROJECT_SOURCE_REFRESH_REQUIRED: YES`.

### Adaptive development endpoint and host qualification

The closed [adaptive clock summary](p3-tt-effectiveness-2026-10-23/adaptive-clock-v1-summary.json)
records W1 30/19/15 (61.72%, descriptive Elo+83, pair-normal95% +17 to+156)
and W4 28/18/18 (57.81%, +55, -17 to+131). Conservative bounded-pair intervals
are much wider and include zero: W1 -87 to+311, W4 -117 to+261. These are selected
development comparisons after full-window exploration, not independent gain
confirmation. Two domains share openings; never pool their estimates. Candidate/
baseline native CPU ratios are1.003 W1 and0.985 W4, with complete request elapsed
876.0/873.7s and856.0/852.8s respectively. Candidate peak working sets353.1MB
and390.3MB are in the receipts. Both complete128 games together took about58min.

The passive host windows identify about0.96 other busy logical cores during W1
and2.99 during W4. A subsequent read-only process sample finds the owner's game
using2.33 cores. See [availability evidence](p3-tt-effectiveness-2026-10-23/post-development-host-check.json).
The host sample is partial-window evidence, not a complete process attribution or
proof of isolation. W4's substantial unplanned load makes its clock strength
screen resource-unqualified. The score-only selection in the generated summary
did not enforce the protocol's resource condition; this explicit disposition
corrects that omission. All original data, protocol and summary remain intact.
The [host disposition receipt](p3-tt-effectiveness-2026-10-23/adaptive-clock-host-disposition-v1.json)
binds both raw host windows and supersedes only the score-only eligibility labels.
W1's background workload also limits generalization, though no comparable active
gaming interval was observed. Fresh studies require a prospective resource gate.

The closed adaptive-clock-v1 JSON has duplicate `selection` keys: the latter
strength gate wins in ordinary JSON parsing. The former opening description is
still visible in its immutable source, and actual openings/exclusions are bound
by the separate selection receipt and binding. New protocols use distinct keys;
no historical protocol or outcome is rewritten to conceal this metadata flaw.

### Access profile and resulting disposition

[Access profile v2 summary](p3-tt-effectiveness-2026-10-23/access-profile-v2-summary.json)
is a corrected categorization of the same immutable JFR recordings. W1 and W4
each use24 complete1m-node requests on12 full-history roots, plus400k startup
warmup, ordinary default UCI/BaselineEngine, diagnostics OFF and frozen runtimev2.
Java execution samples containing TTable frames number7/3764 and14/4552;
including SearchKey changes W4 to17. No monitor-entry event exceeds the configured
10ms threshold. ThreadPark has1/534 events; overlapping park durations are not
lost CPU. JFR/default10ms Java and20ms native sampling can miss inlined/small costs;
these counts are not CPU percentages, upper bounds, or proof of zero contention.
Profile timing includes JFR and export overhead, not an uninstrumented speed test.

The first summary/script incorrectly treated SearchKey as residing in `.tt`;
v2 corrects the category to `.exact.SearchKey`, retaining v1 and its raw data.
TTable counts are unchanged. The profiles took46.2s W1 and23.7s W4 wall including
export. Repeated same-move writes and empty ordinary probes remain possible
micro-cost hypotheses, but no evidence now justifies owner-reserved access,
replacement, layout or locking changes. No separate optimization programme is
opened. NNUE frames being common is not authority to optimize or change CP64.

[Bootstrap validation](p3-tt-effectiveness-2026-10-23/bootstrap-validation.json)
binds the actual tests. The first run executed 84 checks: 83 passed, one stale
research assertion failed because it expected HCE rejection after build69 added
HCE support. Its original XML/log is preserved. Only that test expectation was
corrected; the second run passed 12 checks including the affected suite, new
CP64 observer neutrality/abort/history/reset and table-counter checks, and UCI
composition. Project dependency boundaries passed. Tests overlap across runs;
96 executions are not 96 distinct tests. No production source changed.

The first diagnostic protocol uses 12 historical development roots, existing
hints-off/Hash1 controls, one-/four-worker complete requests and separately
instrumented one-worker table observations. `run_diagnostic_v1.py` freezes the
runtime and hashes before execution, runs one BelowNormal JVM at a time with
blocking waits, and preserves native lifetime CPU/peak memory and failed output.
The host is a Ryzen 5 5500, 6 cores/12 threads, approximately 32 GiB RAM. Existing
editor/Gradle processes and a SeedV3 GUI process were observed and left untouched;
development timing is not an isolated-machine or clock-strength claim.

Use `p3-tt-effectiveness-2026-10-23/` for compact protocols, summaries, identities
and manifests, with bulk output in its ignored `raw/`. Check availability before
analysis; a fresh checkout lacks local raw evidence. Preserve failures and partial
runs. Keep source/configuration, model, workers/Hash, roots/history, JVM/host,
warm-up, allocation, request CPU/time, errors and cancellation auditable. Use
BelowNormal priority and modest process concurrency. Freeze costly protocols
before results, never retune closed finalists, and account for selection effects.
Existing tournament machinery provides pair blocks and process accounting.

W1 fresh confirmation supports a gain under the primary test, with conservative
sensitivity and domain limits above. W4 is inconclusive/deferred; no candidate is
promoted. Both canon mirrors
must be refreshed to P3-R018/P3-F018 before dependent external GPT planning;
Codex has not synchronized them. Repository research is already authorized.
