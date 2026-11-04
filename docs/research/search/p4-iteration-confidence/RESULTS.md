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
