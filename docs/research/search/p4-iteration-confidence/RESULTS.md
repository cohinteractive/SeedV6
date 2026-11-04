# IC-R001 evidence and work selection

5 November 2026. [STATE](STATE.md) is the resume point; [CONTRACT](CONTRACT.md)
defines authority and success. No playing-strength evidence exists for IC-R001 yet.

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

1. **Selected now:** exact-C4 fixed-node observation of completed/provisional
   iteration trajectories and aspiration recovery. This resolves the missing
   mechanism evidence cheaply and uses existing seams.
2. **Conditional:** bounded history of score innovations / failure direction for
   aspiration, if observations show volatile recovery or forgotten instability.
   Keep completion/mate/cancellation honest and BOUNDARY2 unchanged.
3. **Alternative:** challenger-aware root ordering/verification, only after a
   provenance-bearing root-job observation is justified; current effort alone is
   insufficient evidence of inadequate challenge.
4. **Deferred:** independent worker iterations/arbitration; larger accounting and
   lifetime dependency already identified in MW-R001. Not rejected on strength.

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

## Feasibility and next boundary

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
