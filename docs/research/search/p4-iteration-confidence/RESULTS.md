# IC-R001 evidence and work selection

5 November 2026. [STATE](STATE.md) is the resume point; [CONTRACT](CONTRACT.md)
defines authority and success. The first timed exploratory result is adverse;
IC-C1 is not selected for confirmation. Governing strength success remains unresolved.

## Bootstrap challenge: current implementation facts

Source inspected at `4af0e298` (Search identical to accepted `44fc2bf2`).

| Question | Observed fact and implication |
| --- | --- |
| Does challenger information exist? | ParallelSearch retains preferred move and per-move effort after a resolved pass, not a complete score/bound/depth/worker vector. Root ordering receives placeholder score zero. A confidence policy cannot treat these as alternative evaluations. |
| Are root scores comparable? | Root jobs use changing alpha/scout windows. Every original-window scout fail-high below beta receives full retry even if another worker raises alpha. Failed bounds and selective scores cannot be ranked as equal-depth exact values. |
| Is completion missing? | AspirationSearch already withholds failed/incomplete iterations. SearchDriver publishes only resolved completion. ParallelSearch joins workers before returning. Preserve this adequate boundary; do not invent a completion defect. |
| Is agreement independent? | MW-C4 shares current-generation heuristic TT scores/hints under coherent locks, plus common ordering. Worker agreement is correlated; no independent votes or statistical confidence follow. |
| Does eager scheduling already help? | Finite windows at depth>=3 overlap the preferred full search and alternative scouts. Full/one-sided/shallow paths retain owner-first behavior. Changing aspiration changes scheduling exposure as well as retry cost. |
| What does effort mean? | splitEffort includes restricted-job retries within the final pass; previousEffort is updated only on resolution. Failed aspiration passes are absent from the next ranking. Admissions, not useful work, are counted. Root and worker totals must not be conflated. |
| Hidden resets? | Request start clears preferred/effort/root ordering and worker request learning. Abort clears additional worker learning. A per-window whole-root race has different lifetime from drained eager root splitting. MW-C1/C2 negatives do not settle independent iteration. |
| Is stable favorite confidence? | No evidence of adequate challenger coverage is retained. Narrowing solely because a move repeats could reinforce an early error. Observe stability jointly with score evolution/failure, without calling it calibrated confidence. |
| Is aspiration the bottleneck? | Unknown on accepted MW-C4. Prior MW diagnostics measured different architectures/configurations. Re-measure exact C4 before selecting a recovery/feedback mechanism. |

Core references: `search/driver/{BaselineEngine,AspirationSearch,SearchDriver}.java`,
`search/exact/{ParallelSearch,IterationReuse,ExactSearch,ConstructionSearch}.java`,
`search/common/SearchControl.java`, `search/manage/ClockPolicy.java` under engine-core.
Existing research AttemptObserver and drained worker accounting permit a first study
without production instrumentation. Research-only reflection may read existing root
effort at a quiescent boundary with explicit field/shape checks, never mutate it.

MW-R001's ARCHITECTURE/RESULTS/STATE/PRODUCTION were consulted. Its eager-memory
strength result is integrated, not an independent scheduling or TT effect. Prior
whole-root race/sharing failures and adverse fixed-work results remain unchanged;
MW-C6 is tested but runtime/strength-unqualified. No frozen clock study was opened.

## Dynamic selection

1. **Selected now:** root-job provenance observation to explain the mismatch between
   C1's reduced failed-pass work and adverse timed result. Separate within-pass
   scout/full-retry work, honest bounds and completion rather than assume a cause.
2. **Completed:** exact-C4 diagnostics, IC-C1 implementation/qualification and the
   fixed64-game exploratory screen. C1 is rejected for confirmation; do not repeat
   or enlarge that completed sample.
3. **Deferred after weak/mixed replay evidence:** cumulative failed-pass effort
   ordering and directional aspiration carryover. No parameter tuning is selected.
4. **Conditional:** challenger-aware ordering/verification after provenance-bearing
   measurement. Independent worker iterations remain a larger accounting/lifetime
   alternative, not a mechanism already rejected on strength.

Do not implement a fixed feature queue. Reorder from results; do not select a
candidate by depth/NPS alone. Timed exploratory evidence is the next strength
dependency after a technically qualified integrated formulation.

## External investigation and transfer limits

Pinned raw files were available and hash-verified (`external-sources.json`). Fresh
history/reversion retrievals are indexed in `external-history.json` and
`external-reversions.json`; raw responses stay in `raw/external/`. Web API history
retrieval failed; ordinary HTTP obtained the exact pinned history. No independent
replication of external tests is claimed.

- [Stockfish19](https://github.com/official-stockfish/Stockfish/blob/edb0d9db6731067ec50ce619ff372b463bc4dd5d/src/search.cpp)
  uses per-root score averages/second-moment-like state, asymmetric recovery and
  effective-depth adjustment; root effort/PV variability feed continuation. Seed
  lacks those information structures and has different selective/lifetime semantics.
  [Issue2717](https://github.com/official-stockfish/Stockfish/issues/2717) documents
  nominal-depth/worker-selection anomalies and unsuccessful proposed fixes. Preserve
  honest effective depth; do not transfer the parameters or publication semantics.
- [Ethereal14](https://github.com/AndyGrant/Ethereal/blob/96cde8d2f82a3ab0536bf33572405fda96f6e236/src/search.c)
  retains/reverts best lines across failure, adjusts recovery depth and uses richer
  TT information. [Reversion93a3bb15](https://github.com/AndyGrant/Ethereal/commit/93a3bb15155a34f3334cd200e23de52e03a98e09)
  removed aspiration-failure-dependent root LMR after reported adverse tests; the
  actual diff confirms that coupling. Failure direction is not automatic justification
  for selectivity changes. Author-reported tests are not independent Seed evidence.
- [Bagatur5.5c mediator](https://github.com/bagaturchess/Bagatur/blob/56d1b3651f62bc770c7cde5f4c1b0360890b9bd5/Search/src/bagaturchess/search/impl/rootsearch/sequential/mtd/Mediator_AlphaAndBestMoveWindow.java)
  varies trust windows from move repetition and score range. Its tuning comments
  lack a complete protocol there. [SearchersInfo](https://github.com/bagaturchess/Bagatur/blob/56d1b3651f62bc770c7cde5f4c1b0360890b9bd5/Search/src/bagaturchess/search/impl/rootsearch/parallel/SearchersInfo.java)
  filters upper-bound/no-PV observations and coordinates depth reports. These assume
  its MTD/coordinator semantics, unlike Seed's root-job cohort.

No external code/constants were imported. IC-C1 adapts Seed's aspiration driver
under research ownership. Current ClockPolicy composition was read only to preserve
the accepted interface; no frozen independent clock study was opened.

## Executed baseline and selected IC-C1

`baseline-mixed-w4-v1`:40 fresh-TT requests on20 mixed-phase real-history roots,
forward/reverse rounds,200k admissions/request, depth ceiling14. Exact build96 core,
CP64/Hash64/W4/EAGER_ROOT/ADAPTIVE16/BOUNDARY2 composition. All8M nodes accounted;
legal PV/restoration/drain passed. Completed failed passes used15.86445% of admissions;
incomplete passes26.280425%. After two completed iterations with the same favorite,
the next completion changed move35/142 times. This is descriptive selected-root
evidence, not an error rate or proof that the original favorite was wrong.

Read-only width replay found83/313 finite initial windows would widen under the
proposed carryover floor;38 of those83 failed initially. This does not predict the
changed Search trajectory or saved work. It justifies a small experiment instead
of confidence-based early termination.

**IC-C1 - Volatility carryover aspiration over MW-C4:** preserve ADAPTIVE16's center,
current score-change width, mate boundaries, recovery, four-failure fallback,
requested depth and resolved-only publication. Add a floor equal to half the
previous resolved iteration's *initial* half-width, capped512. A recent volatile
step is forgotten gradually after a quiet step. Failed/incomplete iterations do not
update carryover; request start/end/newGame reset it. No stable-move confidence,
worker vote, provisional publication, independent clock policy or new TT eligibility.

Changed windows also change selective/order/TT trajectories; existing eager scheduling
remains the finite-window path. The candidate is integrated with C4; fewer failures
alone are not better decisions. Explicit research UCI selection activates C1 only
atW4; all other workers delegate exact production routing.

## Reciprocal fixed-work evidence

Prospective order:baseline-v1, C1-v1, C1-v2, baseline-v2. Each40 requests/8M nodes;
all160 requests passed accounting, legal PV, restoration and native Job drain.

| Run | Mean completed depth | Completed bound-pass admissions | Incomplete-pass admissions |
| --- | ---: | ---: | ---: |
| Exact C4 baseline v1 | 7.825 | 15.86445% | 26.280425% |
| IC-C1 v1 | 7.875 | 9.2414125% | 28.3012125% |
| IC-C1 v2 | 7.900 | 10.090625% | 25.90225% |
| Exact C4 baseline v2 | 7.725 | 21.1349375% | 28.4958% |

Lower failed-pass work repeats; baseline variability is material. Small depth and
incomplete-work differences do not establish strength or causal speed. Instrumentation
can perturb scheduling; effort is admissions, not useful work. No exact reference
changed and no selective result was labelled exhaustive truth.

**Decision:** retain unchanged IC-C1 for a small timed exploratory screen. More
node-only samples or width tuning cannot answer the current strength question.
Challenger-aware instrumentation and independent iterations remain conditional
alternatives. No second candidate or independent confirmation is selected.

### Supporting allocation analysis (before screen results)

`c1-allocation-support-v1.json` summarizes the existing four runs, without new
Search execution. Mean initial finite widths were102.10/99.22 for the baseline
and115.70/114.67 for C1. The final winner's share of resolved split-pass admissions
was27.01%/26.23% versus26.22%/25.77%. Alternative admissions increased, but this
excludes earlier failed passes and does not measure useful work or adequate challenge.

`root-effort-replay-v1.json`, reproduced by
`tools/p4-iteration-confidence-effort.py`, compares ranking alternatives by the final
resolved pass with ranking them by all completed passes of that same iteration.
The favorite remains first; tied effort preserves the final pass's input order.
Incomplete iterations are excluded. All four input hashes and admission sums passed.

In the two baseline runs,135 resolved split iterations had earlier failed passes;
including their work changes the top alternative46 times and the top-three set87
times. Against the *next completed selective favorite*, the hypothetical cumulative
ranking moves that favorite earlier10 times and later8 times (116 unchanged).
C1 gives16 earlier/8 later/118 unchanged. This is retrospective, correlated evidence;
the next favorite is not ground truth, and a changed search order changes the trajectory.
**Decision:** simple accumulation of failed-pass effort is not presently supported
strongly enough to select another implementation or spend timed games. Preserve this
weak/mixed finding; provenance-bearing challenger observations remain conditional.

`window-direction-replay-v1.json` separately replays C1's width recurrence on both
baseline trajectories and retains the sign of the score innovation behind the
carried width. In the83/90 windows that would widen,38/49 initial passes failed.
The retained direction matches50/87 failures; the latest nonzero movement matches
27/64, with23 zero-movement cases separate. These correlated observations provide
weak/mixed directional evidence. **Decision:** defer asymmetric carryover rather
than turn this into directional parameter tuning. No new candidate or experiment
was created by either replay, and no screen score was accessed for these decisions.

## Technical and operational qualification

`technical-validation-v2.json`:41 Java tests passed (31 research,10 core), zero
failures/errors/skips. Includes carryover/reset, stopped bound, incomplete retry,
reversed failure, mate full-window verification, depth/accounting/reentry, eager-root,
memory/exact-TT and production-profile checks. Six supervisor tests passed, including
refusal, failed/timeout charging and native drain, resource reserve and absent/expired
idle windows. Compilation/project dependency boundary verification passed.

`c1-uci-qualification-v1`:12 requests/320,289 admissions passed. Both formulations
load **pinned build96 engine-core and app-uci jars**, identical binding/JVM/evaluator.
W4 caps, stop, legal PV, reset/reuse/resource reconstruction, W1/W2 fallback identity
and normal JVM exits passed; parent Job drained all descendants. No managed-clock
competitive game ran. Build97 core differs only in generated metadata; actual
comparison loads96 rather than relying on that equivalence.

Full identities are in `baseline-manifest.json` and `runtime-c1-uci-v1-manifest.json`:
core SHA256 `3211f4f375d667f3f442a21d88d8b9755d178ea6a261f0fee74f8ffe84bf84ea`,
research JAR `7834ce39967dc920e72ab075dd8ff66d138a432c98a6131f5f6d6f45e9a7b8f1`,
CP64 file `8431626c2fca81e0a1e9ccd5e45ddd8829e6e3282c756c21fad75b06d075bb2b`.
Source/class hashes distinguish the research overlay from application build96.
Exact earlier observation source was recovered and verified against frozen hashes;
raw snapshots preserve that source, final source and all binaries.

Setup failures resolved before experiments: PowerShell split an initial Gradle `-D`
argument (corrected using exact Java argv); default Python lacked chess (installed
1.11.2 only in ignored `build/research/ic-python`). A documentation protocol-preparation
pipeline mistakenly entered Python's console; Ctrl-C stopped it, and process inspection
found no surviving Python. No experiment/protocol write occurred in that failed command.
Frozen raw experimental evidence was not overwritten. Full application/platform,
browser/UI, deployed-client and timed-strength validation were not run or claimed.
No production source/default changed.

## Bootstrap feasibility and earlier authorization boundary

`feasibility-v1.json` models maximum240-pair confirmation. Prior MW pair SD is an
illustrative assumption only: roughly40% gate power at true53% score,81% at55%;
SD0.35 gives26%/60%. Actual C1-vs-C4 dependence/variance is unknown. The envelope
can investigate a moderate gain; it cannot guarantee detection of small gains.
Primary acceptance and one-attempt allowance are unchanged.

Prepared protocols:4-game operational pilot, then64-game/32-pair exploration,
10+0.1/W4/exact-C4, alternating first colors, cold pair JVMs, fixed300-move cap,
no score-driven stopping. Pilot is permanently excluded from selection; its
score-blind technical/operational audit must pass first. Failed/interrupted games
and partial pairs stay charged/preserved. Confirmation is unselected/unspent;
future held-out roots must exclude all used inputs.

Prior throughput suggests35-40 minutes, not a measurement of this candidate.
Pilot/screen hard limits5/60 minutes plus drain fit a requested **75-minute owner
idle window**. After68 development games,252 further development games remain while
protecting480 confirmation games. Both actual protocol admissions correctly rejected
absent owner authority.

Expenditure: **37.5157479000045s /86,400s**,32,320,289 admissions, **0/800 timed games**,
**0/1 independent attempt**. All five experimental runs passed; no pending admission
or owned process. Strength is **UNRESOLVED**. This is an authorization handback,
not success, programme closure, acceptance or production adoption.

## Completed owner-authorized timed exploration

The owner authorized75 minutes starting at first observed2026-11-04T21:56:47Z;
end23:11:47Z. The exact4+64-game scope is recorded in `owner-idle-window-v1.json`.
Pilot passed4 normal games/535 requests and native drain,129.7151052 charged seconds.
Its technical/operational review preceded screen admission without using pilot WDL.
The pilot is permanently excluded from selection and strength statistics.

The unchanged screen completed32 pairs/64 games in2014.4675876 charged seconds.
All8,548 requests passed independent legality/PV/history/terminal/accounting audits;
inputs remained stable and all132 owned processes drained. No fallback warnings,
failed/interrupted games or operational exclusions. Mean total CPU35.6008%, peak
78.2552%; minimum free memory14.7568GiB, peak owned commitment0.8173GiB. The pinned
analysis tool and three tests preceded score access; operational review passed first.

**IC-C1:14 wins,25 draws,25 losses;41.40625%.** Pair pentanomial[2,10,17,3,0];
paired-normal95%[34.9507%,47.8618%], Hoeffding95%[17.3982%,65.4143%]. Both intervals
assume independent opening pairs; selected/repeated opening families and hardware
correlation can limit that assumption. Normal approximation uses only32 pairs and
is not exact. This is an adverse exploratory result, not independent confirmation,
universal inferiority, equivalence or a newly qualified baseline. No live score
stopping, tuning or sample expansion occurred.

Candidate first/second-game scores43.75%/39.0625%; White/Black45.3125%/37.5%.
These are descriptive, not extra hypothesis tests or reasons to exclude games.
Total candidate/control admissions1,773,090,158/1,791,926,653 and measured request
elapsed954.4306/961.8775s do not identify a causal speed or clock-policy effect:
requests and positions diverged. The common BOUNDARY2 policy was unchanged.

**Decision:** reject IC-C1 for independent confirmation. Reduced aspiration retry
work did not translate into favorable timed play. Do not rescue it by more games
or blind width tuning. Investigate the credible structural interaction: wider
windows can change concurrent scout/full retries, root ordering and shared-TT
trajectories *within* successful passes, which current effort data cannot separate.
A research-only root-job observation is selected before another candidate.

Evidence: `c1-timed-{pilot,screen}-w4-v1-execution.json`, corresponding operational
reviews, `c1-timed-screen-w4-v1-analysis.json`, `screen-analysis-definition-v1.json`;
raw local traces/PGNs at `raw/c1-timed-{pilot,screen}-w4-v1/`, all hashed in receipts.
Total ledger2181.6984407000127s (36m21.70s),68/800 games,0/1 confirmation attempt.
84,218.30155929999s and732 games remain, including480 reserved confirmation games.
No pending reservation or owned process remains. The75-minute permission's4+64
scope is fulfilled and grants no additional timed games. The governing objective
remains unresolved; this negative formulation does not close the workstream.

## Root-job diagnosis and selected IC-C2 hypothesis

`P4ObservedRootSearch` is a mechanical, research-owned copy of exact96
`ParallelSearch`, reproduced and checked by `p4-iteration-confidence-observer-source.py`.
Only class identity and bounded per-job primitive observations differ. Each unique
job's worker owns its two initial/retry slots; records are read after joins and
never feed Search policy. Original production source remains unchanged. The public
`researchWindows` seam supplies the same observation wrapper to both formulations.
The first compile used an incompatible concrete-owner constructor; it was corrected
before any experiment. Ten targeted Java tests then passed, including exact-score,
eager concurrency, draw history, stop/accounting, immutable snapshots and reset.

Four reciprocal observations (C4,C1,C1,C4), each40 requests/8M admissions, passed
all accounting/PV/restoration/drain checks. Runtime/source identities are frozen in
`runtime-root-observation-v1-manifest.json`; C1's class bytes match its earlier runtime.
`root-job-diagnosis-v1.json` preserves whole-root/scout/initial-full/full-retry counts,
original bound windows, worker and completion semantics. The independent accounting
analysis validated every retry's antecedent and every admission partition.

| Run | Bound-pass work | Full root-retry work | Incomplete-pass work | Mean depth |
| --- | ---: | ---: | ---: | ---: |
| C4 v1 |18.2930%|19.1615%|23.6453%|7.850|
| C1 v1 |9.2442%|20.3795%|30.9687%|7.850|
| C1 v2 |9.7713%|23.0646%|28.8778%|7.825|
| C4 v2 |17.6873%|19.4516%|26.9483%|7.750|

Categories overlap: aspiration outcome and root-job type classify the same work.
C1 has552/544 full retries despite fewer scouts7731/7723, versus C4's507/508
retries from8717/8713 scouts. This supports a within-pass cost interaction; it does
not prove the timed-loss cause, independent useful work, or clean speed. Instrumentation
and shared-TT/order/SMP trajectories remain important limitations.

**Selected IC-C2 - Upper-window volatility carryover:** preserve baseline ADAPTIVE16
initial alpha and recovery doubling width. Retain C1's half-previous-resolved-initial
upper width only for initial beta, still capped512. Thus the initial scout threshold
is not lowered by remembered volatility. Unfailed endpoints, mate verification,
resolved-only publication, request resets, depth, shared TT and BOUNDARY2 remain.
This is an asymmetric *cost* hypothesis, not a prediction of score direction (the
prior directional replay was weak). No constants are tuned and no production default
changes. It is a new isolated formulation, not a redefinition or rescue of frozen C1.

Before any timed proposal: qualify C2 and compare it with C4 in a second fixed
reciprocal root-job batch. Seek whether it avoids C1's extra root-retry work while
retaining some failed-pass reduction. Diagnostics cannot establish strength. Preserve
negative findings; do not select a timed run solely for deeper Search or fewer retries.

## IC-C2 qualification and next timed boundary

Seventeen targeted Java tests passed (C1:6, observer:4, C2:7), including C2's
unchanged lower endpoint and baseline-sized fail-low recovery after retained upper
volatility. Compilation and project dependency boundaries passed. Four C2 reciprocal
fixed runs passed160 requests/32M admissions, legal PV/restoration/accounting and
native drain; `c2-fixed-analysis-v1.json` contains the validated partitions.

| Run | Bound-pass work | Full root-retry work | Incomplete-pass work | Mean depth |
| --- | ---: | ---: | ---: | ---: |
| C4 control v1 |15.6399%|20.1795%|29.0940%|7.775|
| C2 v1 |17.2791%|18.7587%|26.2616%|7.800|
| C2 v2 |16.7844%|17.7113%|24.7132%|7.900|
| C4 control v2 |17.3180%|18.3740%|26.9035%|7.800|

C2 does not retain a clear reduction in failed-pass work. Its full-retry and
incomplete-work fractions do not repeat C1's observed increase; control variability
and overlapping values limit the inference. These are hypothesis-directed diagnostics,
not evidence of strength. **Decision:** retain the unchanged C2 formulation for one
bounded exploratory timed screen to test whether preserving the lower-bound cost
structure matters in practical play. No width constants were fitted, no C1 games
are replayed as C1, and no negative evidence is discarded.

`runtime-c2-v1-manifest.json` pins exact96 core/UCI, CP64, source/classes and research
JAR SHA256 `52443c7a56957d19613b6ea716a5f2c3b7a25cd0001500640a254032f97a094b`.
UCI uses original ParallelSearch, not the observation copy. The C2 qualification
passed12 requests/320,269 admissions, caps/stop/PV/reset/reconstruction, W1/W2
production fallback and normal JVM/native drain. No timed C2 game has run.

Prepared `c2-timed-{pilot,screen}-w4-v1-protocol.json`:4 operational games followed
only after score-blind qualification by64 fixed exploratory games,10+0.1/W4/exact-C4.
Same frozen DEVELOPMENT openings are reused deliberately; they are not fresh
held-out confirmation. No C1/C2 score pooling and no pilot pooling. The paired-normal
and Hoeffding method, exact analyzer, inputs, stopping/failure rules and sample are
pinned prospectively. This adaptive second formulation can introduce selection bias.
The sole independent attempt remains entirely unspent and unselected.

Observed68-game C1 throughput was35m44s. C2 hard limits5/60 minutes plus drain fit a
new75-minute window. After that proposed68 games,184 development games remain while
protecting480 confirmation games. Both new admissions were checked and correctly
refused before process creation because no new owner authority exists.

Current total:2247.7328160000616 charged seconds (37m27.73s),96,640,558 fixed admissions,
68 completed/initiated timed games, zero failed/interrupted games,0/1 confirmation
attempt.84,152.26718399994s and732 games remain. All16 experiments completed and drained;
no pending reservation or owned process. Full application/UI/platform/deployment and
independent strength validation were not run. No production defaults or version bytes
changed. New diagnostics and C2 preparation are research-only, no_bump.

**Blocking next human action:** authorize a new concrete75-minute idle window for
prepared C2 pilot4 plus screen64. Prior75-minute scope covered C1 only and is spent;
remaining clock time is not additional authorization. Mirrors P4-R030/P4-F048 still
need owner refresh before dependent GPT coordination. Governing strength objective
is unresolved; no acceptance, closure, champion/promotion or deployment is claimed.

## Challenger provenance replay before C2 screen results

`challenger-replay-v1.json`, reproduced by
`tools/p4-iteration-confidence-challengers.py`, analyzes only the four existing C4
root-job runs. It requires final resolved split passes at or below the request's
last published depth; every observed root job is complete and the move set has
exactly the reported legal-root count. All input hashes passed. No new Search run
or timed-score access occurred.

A marker records that an original-window scout fail-high received a full retry.
It is a provenance event, not an exact comparable score or confidence. Across927
published split iterations there were1265 marked alternative entries;1110 (87.75%)
were already in the three highest final-pass-effort alternatives. In280 next-depth
favorite changes,130 had the marker and190 were already among effort's top three.
A hypothetical marker-first ordering moves the next favorite earlier7 times,
later24, and leaves its rank unchanged249 times. Observed job order breaks ties;
actual next-iteration history-order ties and changed Search trajectories may differ.
The four runs reuse selected roots and correlated SMP; none of these counts are
independent trials, proof of correct moves or calibrated confidence.

**Decision:** defer marker-first root ordering. The event contains descriptive
information, but current effort ordering already captures much of it, and this
replay does not support overriding that ordering. Preserve this negative/weak
candidate-selection evidence. No additional implementation or competitive sample
is selected by it; the unchanged C2 timed screen remains the active experiment.

The conditional independent-iteration alternative was rechecked against current
source without running it: `engine-core/.../search/exact/IndependentSearch.java`
is unchanged from exact96 and already exposes an opt-in iterative owner seam through
SearchLifecycleService. It has private adjacent aspiration loops, a shared generation
and admission budget, OWNER-only completed result selection/BOUNDARY2 decisions,
then local helper stop and join. Its progress snapshots expose actual completed helper
depths, not independent votes. Earlier MW-C6 tests do not constitute an IC runtime or
strength qualification. If C2 does not warrant continuation, a separately identified
W4 diagnostic of this existing architecture is a possible next structural unit;
no new coordinator, arbitration rule, candidate or timed authorization is selected now.

## Completed IC-C2 timed exploration

Owner window v2 authorized prepared4+64 games from first observed2026-11-04T23:38:44Z
until2026-11-05T00:53:44Z. Pilot passed4 normal games/763 audited requests, no fallback
warnings, stable inputs and native drain;157.1903228 charged seconds. Its score-blind
operational review preceded admission of the unchanged screen. Pilot WDL is excluded.

Screen passed64 normal games/9,852 audited requests, stable inputs and all132 owned
processes drained;2183.2793712 charged seconds. Mean total CPU38.0712%, maximum88.1510%,
minimum free memory15.0523GiB and peak owned commitment0.7793GiB. No failed/interrupted
games, exclusions, live-score stopping, tuning, or sample changes. Complete operational
review passed before the fixed paired analysis was opened.

**IC-C2:21 wins,25 draws,18 losses;52.34375%.** Pair pentanomial[2,7,10,12,1];
paired-normal95%[43.7212%,60.9663%], bounded Hoeffding95%[28.3357%,76.3518%]. The
normal interval is approximate with32 opening pairs; both methods assume independent
pairs and do not remove adaptive selection, repeated-opening-family or hardware
limitations. This reused DEVELOPMENT sample is not independent confirmation.
C1/C2 results and pilots are not pooled.

Descriptive first/second-game scores43.75%/60.9375%, White/Black62.5%/42.1875%.
These small subgroups do not justify exclusions or extra hypotheses. Candidate/control
request elapsed1036.4555/1039.7575s and admissions1,523,930,897/1,552,571,556 are from
divergent games. Their differences, and lower throughput for both formulations
compared with the C1 batch, do not establish a causal speed/clock effect or a
between-batch strength comparison. Protocol resource guards passed; no thermal or
clean-speed equivalence is claimed.

**Disposition:** C2 is inconclusive, retained as a weak exploratory reference but
not selected for the sole confirmation attempt. No claim of improvement, equivalence
or failure of every aspiration mechanism. Do not respond by blind width tuning or
automatically enlarging the completed sample. The fresh next unit is bounded
qualification/observation of the existing IndependentSearch architecture with C4's
shared-memory flags, owner-only publication and unchanged BOUNDARY2. This investigates
cooperation/state lifetime rather than treating the older synchronized-race negatives
as decisive. No timed independent-worker variant is selected before diagnostics.

Evidence is `c2-timed-{pilot,screen}-w4-v1-execution.json`, corresponding operational
reviews and `c2-timed-screen-w4-v1-analysis.json`; all raw traces/PGNs remain ignored
at their hashed `raw/c2-timed-{pilot,screen}-w4-v1/` paths. Current total4588.202510000032s
(1h16m28.20s),136/800 initiated/completed games,0 failed/interrupted,0/1 confirmation
attempt.81,811.79748999997s and664 games remain, including480 reserved confirmation
games (184 development games left). All18 experiments completed and drained. Window
v2's4+64 scope is fully used; it grants no further timed games. Strength remains
unresolved and the workstream remains active, without promotion or owner acceptance.


## Independent-owner and complementary-depth diagnostics

Following the inconclusive C2 result, existing exact96 `IndependentSearch` was
qualified under a new IC identity with C4 memory flags1182007295.29 tests passed:
6 existing independent-search tests and23 lifecycle-service tests. The new
`P4IndependentIterationStudy` checks owner-only publication, completed helper PVs,
board restoration, exact global/private accounting and drain. Shared TT correlates
workers; agreement is not confidence or evidence of correct moves.

Each reciprocal batch uses the first20 mixed-phase fixture roots, forward then
reverse,40 requests/run,200,000 global nodes/request, depth cap14. Fresh request/TT,
selected repeated roots, JIT/observation overhead and SMP variability limit inference.
All eight launched runs passed, each with8M admissions and native drain.

| Batch/run | Mean owner depth | Summed request seconds | Process CPU seconds |
| --- | ---: | ---: | ---: |
| Independent control v1 |7.825|6.5155|24.3125|
| Independent owner v1 |7.200|5.6414|29.1250|
| Independent owner v2 |7.175|5.0722|25.5156|
| Independent control v2 |7.925|6.4664|24.4844|
| Complementary control v1b |7.750|6.6895|25.7969|
| C3 v1 |7.000|5.9482|29.7656|
| C3 v2 |7.000|5.6312|28.1250|
| Complementary control v2 |7.825|7.2619|27.6094|

Independent helpers matched owner completed depth224/240 times, were deeper6 and
shallower10; completed move agreement228/240. Owner used approximately25% of nodes.
**Decision:** defer timed independent-owner testing. Lower owner depth and extensive
same-depth repetition provide no compelling allocation improvement. Faster measured
elapsed is not clean-speed qualification or strength evidence; nodes traverse different
selective trajectories and the application still lacks a timed qualification here.

**IC-C3 Complementary-depth helpers** tests a structural response to that duplication.
`tools/p4-iteration-confidence-complementary-source.py --check` verifies the isolated
research copy against exact96: only class identity/documentation and helper depth-loop
start/stride differ. Owner retains every adjacent depth; helpers1/3 visit even depths,
helper2 odd depths atW4. Depth1 clamps to1. Owner-only selection/BOUNDARY2, hard stops,
private mutable worker state, common guarded TT/generation/global budget and joins
are preserved. Skipped depths are never represented as completed.

Source inspection before experimentation identified important coupling: aspiration's
adjacency guard gives helpers full windows across gaps, and IterationReuse invalidates
adjacent-only completed PV/effort reuse. Request learning remains. This is an integrated
schedule/window/reuse change, not isolated diversity. Seven tests passed, including
actual parity/completion provenance, exact rules/PV/history, accounting/caps, helper
failure and stop dominance, managed replacement/reuse, and tail-drain witnesses.
Small other-worker correctness cases grant no competitive worker-count authority.

C3 helpers matched depth129/240, were shallower105 and deeper6; move agreement189/240.
Owner depth fell further to7.0 in both runs. **Decision:** defer C3 timed testing.
Reduced same-depth repetition mostly leaves helpers shallower; no evidence here
justifies consuming games on C3 or implementing helper-result voting. This does not
prove universal inferiority, but preserves a negative structural investigation.

`complementary-control-v1` was refused before launch at37.03% existing CPU load
(projected70.36%, above daytime planning envelope). No owned process/node/charge.
Unrelated pre-existing Java processes were not touched. Two later read-only resource
samples at16.13%/11.525% were admissible; `complementary-resource-recheck-v1.json`
records them. New frozen v1b explicitly names the refused predecessor. The original
protocol/refusal and all subsequent results remain preserved, with no score exclusion.

`runtime-independent-v1-manifest.json` and `runtime-complementary-v1-manifest.json`
pin source/core96/CP64/runtime/test identities. Research JAR hashes respectively
`e31c799fd03f1fa36e55fc2531c80b1f86fec78a93cc56e15a16889f1a5ebcaa` and
`a8daca1ddad7ef1a33be602b023b60b849096f3c4c0b41c53a1d1b2de99f7bfa`.
`independent-analysis-v1.json`/`complementary-analysis-v1.json`, produced by
`tools/p4-iteration-confidence-independent.py`, rehash complete observations against
their successful drained receipts. Raw evidence remains at `raw/independent-*/` and
`raw/complementary-*/`; frozen JAR/source copies stay under their named runtime paths.
Eight new experiments charged61.00638979999349s and64M admissions; the refusal charged0.

## Separate prospective C2 developmental replication

The unchanged qualified C2 formulation remains the only current candidate with a
non-adverse, though inconclusive, timed screen. Root-challenger replay and independent
owner/C3 observations do not justify another timed formulation. Select **one fixed64
replication on32 new IC-development positions** to test consistency across input sets
before considering the sole independent attempt. This is an evidence-directed new
unit, not a larger replacement of the completed screen, a rescue of C1, width tuning,
or automatic continuation after each result. No further extension is preauthorized.

`development-v2-openings.pgn` uses unchanged `search-baseline.py::select_openings`,
seed2026110506, hash partition0, sorted unique final-FEN keys and Python random.sample.
Source hash verified against development v1; no source results/evaluation filtering.
It selects32 legal startpos16-ply histories from20,110 eligible distinct positions,
excluding all34 previous IC pilot/screen keys and20 mixed-phase diagnostic final keys.
The selection manifest pins source/selector/output/exclusion hashes and environment;
`diagnostic-root-exclusions-v1.json` makes the latter exclusion reproducible. Existing
narrow `*-openings.pgn` input exception covers this intentional fixture; no tournament
output exception or force-add is introduced. Freshness is **only relative to these IC
inputs**, not every historical campaign; future confirmation must exclude these too.

`c2-timed-replication-w4-v1-protocol.json` freezes unchanged C2 runtime and exact96
control,64 games/32 pairs,W4/10+0.1, alternating first color, all full-history audits,
fixed analyzer/method, stopping/failure rules and3600s hard limit. The unchanged runtime
already passed pilot4 and screen64, so another pilot is unnecessary. Complete operational
review must pass before any score analysis. Standalone replication results are primary;
any combined C2 development summary is secondary descriptive only, never confirmation.
No C1/pilot pooling or score-prefix/subgroup selection.

Observed C2 pair SD approximately0.24886 yields estimated SE0.04399 and95% half-width
0.08623 for32 new pairs; normal-approximation80% detectable gain is about12.33 percentage
points. These uncertain planning estimates ignore selection/dependence. This small
screen cannot reliably qualify a3-point gain; a later confirmation decision still
needs defensible power and fresh inputs, with the original53%/lower>50% gate unchanged.
Prior screen used2183s; a requested65-minute idle window accommodates3600s plus30s drain.
The previous v2 permission covers completed4+64 only and cannot authorize these games.

Current total4649.208899800025 charged seconds (1h17m29.21s),160,640,558 fixed admissions,
136 initiated/completed games,0 failed/interrupted,0/1 confirmation attempt.26 launches
passed/drained plus one zero-charge refusal; no pending/owned experiment. Remaining
81,750.79110019998s and664 games:184 development plus480 confirmation reserve. Proposed64
would leave120 development games and preserve480 confirmation games/21,600s reserve.
All46 protocol inputs verified locally before the new authorization request. No new
replication game has run. Full application/UI/platform/deployment and independent
strength validation were not run. Research-only work leaves build97 bytes unchanged.

**Human gate:** new65-minute idle authority for this exact replication. Mirror refresh
toP4-R030/P4-F048 remains an owner dependency before mirror-based GPT coordination only.
The governing objective remains unresolved; no acceptance or closure is claimed.
