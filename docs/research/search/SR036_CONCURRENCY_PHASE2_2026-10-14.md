# SR-036 Concurrency Extension — Phase 2

Owner launch: 2026-10-14 local. **ACTIVE; research incomplete.** Only SR-036 is
authorized. Its inherited optional same-depth coordinator remains **BASELINE**.
This report and the active Contract/Frontier are the restart record; no successor
feature is selected. Technical completion will not establish owner acceptance.

Current checkpoint: ROOT_REUSE4 source `d652078` has passed targeted, broad,
stratified real-model, production-bridge, UCI lifecycle and cohort-oracle validation.
All development, serial ablation and contention diagnostics are closed/audited.
Two independent protocols at `sr036-phase2-2026-10-14/confirmation/` were frozen
and committed before launch; confirmation is now running under their fixed rules.
Inspect actual process/receipt state before any restart; never duplicate an active
or closed stage. Wait for stage completion and its independent assessment.
Production remains unchanged. Never tune the finalist or extend a closed result.
Sections below retain the sequence of prospective decisions and later outcomes;
earlier statements of pending work describe those historical stages.

## Authority and verified bootstrap

The owner accepted SR-007 as DEFERRED and explicitly confirmed replacement and
synchronization of BOTH P2-R022/P2-F026 Project Source mirrors. These predecessor
actions are complete. The launch permits autonomous SR-036 research, prototypes,
measurement, validation, integration if supported, and attributable commits.
Binding proof, rule, isolation, publication and lifecycle requirements remain.
Substantial TT storage/locking changes and binding-contract changes are owner-gated.

Starting HEAD is clean `986837d`; starting masters are P2-R022/P2-F026. There is
no root CODEXLOG_CURRENT.md, so none is created. VERSION_STATE.txt is build35.
Both pinned shared-finalizer digests match. The local adapter's optional begin
capture was stopped before any reservation/version mutation, after inspecting its
owned PID and absent active.json; no verified bump or replacement bump is claimed.
The SeedV6-only non-critical override permits research to continue. Final status
verification is pending. Existing editor/Gradle Java processes are unrelated and
are preserved; no prior research process was found.

Verified SHA-256 identities:

| Artifact | Identity |
| --- | --- |
| Accepted production source | `450988455af31383d17d126c6f51d277451f5a18` |
| `app/build/sr005-p2-production-v2/lib/app.jar` | `dfc9d9cb3e5d372eb40015fcc5b53093c5841d09ddad2960cc7c54f21bb4abf8` |
| Frozen Phase1 Contract R045 | `9f820c2e9efd366b179902d3fc14bdd8f88e6ae793356aded6b6119f73e8401b` |
| Frozen Phase1 Frontier F052 | `ead3743400b1dc513e02ad1c776b39db1486e8800c947cece9e20ba199e8fc1a` |
| generation32 network | `5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7` |
| checkpoint manifest | `a51513ddb6e23a8537cc38b8aeb7676af5723c3e3b6bb8151f265bb87ae83891` |
| training.state | `f8bdda5eda0ff7893ee36c2270baaedecaf50e14ff5202ab786996f72a2788d5` |
| training-metadata.bin | `135fb61ed736f313158e5514f2a16a81eda6526c4f034d2232ff26f8ee4176e7` |

Evaluator store is `E:/SeedV6-Networks/BRN-Pair2/235d0688-e060-44e3-8f1e-119c39bc8017`,
checkpoint `g000032-s001753088-5837c7f7192aa3cf9a265388f93649327455e804541398ae3ef6b69e09014304`.
The existing [measurement specification](BRE_PAIR2_MEASUREMENT_BASELINE_2026-10-09.md)
governs quarter-residual mapping and immutable model selection. No retraining,
recalibration or later Best selection is authorized. Runtime preflight remains
to be performed; byte/source verification is not represented as runtime QA.

## Actual composition and initial challenge

`ProductionSearch.create` selects qsearch + quiet LMR + WINDOW LMP + EFFORT for
one-worker Pair-2. At 2..16 workers it selects inherited ParallelSearch with
static leaves and none of those four policies. HCE alone enables its calibrated
NMP/static-null composition. Ordinary startup remains HCE. Current HEAD adds
explicit, default-off SR-018/SR-007 controls relative to the accepted source;
the accepted factory and ParallelSearch are unchanged. Baseline runtime parity
will be verified separately.

The coordinator races identical nominal-depth full-root searches, owner plus
N-1 helpers, at depth >=3 and with multiple legal root moves. It drains helpers
before result/counters/reset/close. Each worker owns evaluator, board, path,
history and PV scratch. Shared TTable retains coherent 32-stripe locking, 64 MiB
requested/48 MiB logical entries, one generation per request, equal-depth/current
generation proof, conservative selective suppression. Separate 512 KiB move
reservations defer busy non-eldest moves without supplying score/proof.

Material research challenges, not assumed conclusions:

- Existing worker-count comparisons are composition-confounded. Compare modern
  candidates with both inherited same-worker Search and accepted one-worker EFFORT.
- Modern qsearch/selective dependencies suppress most ordinary score stores;
  historical static-search cooperation gains may not transfer. Do not weaken proof
  or import foreign racy TT/cross-depth cutoff rules to improve measured scaling.
- A cancelled worker clears its EFFORT state. Naive factory transfer therefore
  does not preserve completed-iteration guidance for every worker. Compare private
  retention with copied, genuinely completed winner observations after draining.
- Reservations alter move order and thus LMR/LMP opportunity. Useful diversity,
  redundant work and tactical damage must be measured; identical policies alone
  do not establish the best cooperative design.
- Additional nominal depth/NPS is diagnostic, not strength. Aggregate admissions
  must include losing/aborted workers and qsearch. CPU/request cost and drain
  latency matter alongside equal-clock play.
- Current SearchControl has an awaitTermination facility. The historical SR-035
  ceiling lead is not an established current defect; no SR-035 work is activated.

Historical evidence was read at
`C:/Users/Central/Documents/SeedV6-SR036-2026-10-01/REPORT.txt` and frozen R045/F052.
It covers root splitting/scouting, Jamboree, reservation racing, independent-depth
Lazy SMP, diversification and voting on static HCE/initialized NNUE, with no Elo
claim. It also preserves a corrected original-scout-window retry bug, lifecycle
repairs, and blocking-JIT deadline outliers. It is useful evidence, not a Phase2
architecture ban or current Pair-2 strength measurement.

Primary references inspected: [Joerg/Kuszmaul, Massively Parallel Chess](https://people.csail.mit.edu/bradley/papers/JoergKu94.pdf)
and pinned Stockfish sf_17.1 [worker search](https://raw.githubusercontent.com/official-stockfish/Stockfish/sf_17.1/src/search.cpp)
and [thread coordination/voting](https://raw.githubusercontent.com/official-stockfish/Stockfish/sf_17.1/src/thread.cpp).
Jamboree separates an eldest full search from parallel scouts and required full
retries. Stockfish uses worker iterative deepening and a completed-depth/score
voting policy. These motivate alternatives, not authority to copy incompatible
proof semantics or assume their policy choices transfer to SeedV6.

## Initial experimental scope and restart action

Hardware verified by CIM: Ryzen 5 5500, 6 physical/12 logical cores; Java
21+35-2513. Four independent single-worker games are not automatically a valid
multiworker tournament configuration. Start with sequential complete-request
cost/validity diagnostics and bounded concurrency calibration. Retain background
JIT, warm all relevant worker paths, and report actual engine workers separately
from tournament concurrency. Existing Fastchess and independent python-chess PGN
audits are available. Independent recursive/TT-off/qsearch oracles remain intact.

Next admissible action: implement explicit default-off SR-036 experimental
controls for cumulative same-depth cooperation, reservation ablation and
completed-observation transfer, then correctness/cost diagnostics. Evidence will
choose further asymmetric/depth/scheduling alternatives and clock comparisons;
this is not a mandatory matrix or preselected adoption. Keep clean single-worker,
inherited multiworker and other evaluator controls. Before any finalist outcomes,
freeze an independent protocol with binary/model/openings/resource identities,
selection/multiplicity handling, inference, stopping rules and limitations.

No clock-strength result, improvement, final disposition, implementation acceptance,
release or Project Source refresh is claimed at activation.

## First mechanism stage (development, not strength evidence)

Default-off controls are PRIVATE (worker-private completed reuse), COOPERATIVE
(same-depth reservation race with drained winner ordering copied privately), PLAIN
(reservation ablation), OWNER (only owner completion publishes), OFFSET (helpers
search one nominal depth ahead, never publish the owner's target), and MIXED
(unpruned qsearch helpers with selective owner). No static-horizon worker shares
proof with a qsearch worker. Copying includes only completed PV/root ranking and
immutable history identity; pending effort, mutable history and scores are not
copied. Production factory is unchanged. The UCI option is a process research
property; incompatible configurations fail rather than silently changing controls.

Initial targeted validation passed, including all-policy exact node budgets,
qsearch/aggregate accounting, blocked-helper stop/drain, injected helper failure
and reuse, independent qsearch control, transferred-state isolation and one-worker
cumulative result/node/PV parity. Broad Search/rules/UCI validation is still running
at this update. The completed version status check is idle with no active or
unfinished operation; build35 remains unchanged and the stopped initialization
capture is a disclosed non-blocking warning, not a verified finalization.

`sr036-phase2-2026-10-14/cost-v1.jsonl` records 1,560 requests over 26 full-history
roots, ten configurations, two rotated rounds and depth6/node100000/time100ms
conditions. `Sr036Cost` warms each configuration with three 500,000-node requests;
construction and newGame are outside timing. Process CPU, allocation, GC/JIT,
actual worker admissions, qsearch and incomplete-worker admissions are retained.
Use the raw root-level data: simple mean depth includes shallow terminal roots and
easy positions reaching the ceiling and is not useful-depth/strength evidence.

Depth6 COOPERATIVE 2/4/6 wall ratios to accepted one-worker composition are
0.957/0.865/0.867, process CPU ratios 1.787/3.193/4.740, node ratios
1.840/3.253/4.502. Four-worker incomplete-worker admissions are 74.8% of all
admissions. This is work abandoned at iteration boundaries, **not a measured
duplicate-position rate**; it may contribute information before cancellation.
At four workers, PRIVATE/PLAIN/OWNER/OFFSET/MIXED wall ratios are
1.010/1.035/1.079/1.112/1.227. Inherited static four-worker wall .415 is
composition-confounded and cannot establish strength superiority. Timed100ms
maximum observed request duration is 100.21ms across these bounded samples;
this is not a deadline guarantee. Process-wide compilation/GC and diagnostic
overheads remain explicit limitations. No separate throughput/clock claims yet.

Decision: transfer is credible enough for exploratory clock play, while limited
speedup despite high aggregate throughput motivates a materially distinct root
work-distribution experiment. Baseline proof suppression is retained. Root
distribution must preserve full original history/root ply, first-child full
windows, original-scout-window retry, terminal checks and restricted-root proof
exclusion. Independent-depth Lazy SMP/voting/shared history/DTS remain hypotheses,
not empirically rejected architectures. Next action remains within SR-036.

## Root work-distribution stage and validation

ROOT_SPLIT searches the eldest branch fully, then assigns remaining root scouts
through a shared atomic job index. Every original-window scout improvement gets
a full retry even if another worker raised alpha meanwhile. All jobs must complete
and all workers drain before publication. ROOT_FULL instead searches each branch
with a full window. Root jobs retain original board/history and real ply, exclude
unrestricted-root TT reads/writes, and preserve applicable interior proof. Terminal
and rule-draw roots use the ordinary whole-root path. Ordering is previous best,
completed inclusive root effort and existing material order; internal completed-PV
reuse is absent in these root-job formulations, a disclosed composition difference.

`cost-root-v2.jsonl` has 936 complete requests with the same diagnostic design.
For the 16 opening positions alone, ROOT_SPLIT 2/4/6 depth6 wall ratios are
1.039/.831/.814, CPU ratios 1.640/1.789/1.851. COOPERATIVE4 is .861/3.281.
ROOT_FULL4 is 3.176 wall/10.640 CPU and is not a plausible finalist in this form.
Timed100ms opening-only average depths are one-worker 5.69, COOPERATIVE4 5.72,
ROOT_SPLIT4 5.69 and ROOT_SPLIT6 5.75: these are not clock-game strength evidence.
Root scouting's incomplete-job admissions are far lower than racing; neither
measurement quantifies unique useful positions. Eldest-branch serialization and
lost internal PV guidance remain credible limitations.

The accepted-source runtime bridge passes 64 deterministic comparisons. The
properly scoped broad Search/rules/UCI run passes 761 tests with 11 existing skips,
zero failures/errors (`broad-v2-xml`). An earlier broad wildcard also selected
unnecessary diagnostic-training fixture tests and completed successfully; future
runs use the fully qualified package filters. It did not train or mutate the
frozen external evaluator. `targeted-v1-xml` is the earlier 37-test targeted run,
not the broad result. Root job/window/proof/mate tests and all-policy budgets,
cancellation/failure recovery then passed on the root prototype.

`Sr036Integrity` on the actual frozen network checks 612 two-/four-worker requests
across all nine policies and 17 history/special roots: 14,744,955 evaluator calls
match private fresh recomputation, 308,906 store attempts were observed and 2,000
sampled full-path ordinary proofs passed independent nominal/static TT-off replay.
Sampling was capped globally, so this first proof sample is not balanced across
policies; it is not a universal proof or calibrated error rate. Aggregate budgets,
legal PV, input preservation and zero remaining active workers pass.

Frozen v1 JAR: `a15eb06f70bd8abc538ae3ccd7754207d43fac2f663b84fc8980cfcdd6ca634c`;
v2 with root controls: `9acff0848da0612e3591227fc934d7837ef40521661e83cc03df41d562700ec1`.
v1 source/evidence checkpoint is commit `1a3e809`; manifests and frozen installation
directories remain available. Rebuilt metadata changes hashes and must be recorded.

The first eight calibration games finished normally (4/2/2, 123.67s) but the
runner's allowed-argument list was extended while the match was active. Its strict
frozen-input audit correctly marked the run FAILED. The entire original receipt,
PGN and failure remain at `calibration-c1`; this is not accepted strength evidence.
Calibration is being repeated with an immutable copied runner and fixed v2 JAR,
at one and two simultaneous games. No test/benchmark workload overlaps it.
All these games are exploratory, never independent confirmation.

Both frozen-runner repetitions passed all input/game audits: one-game concurrency
8 games in 119.38s (3/3/2), two-game concurrency 67.20s (1/3/4). These reuse four
calibration openings and are not independent strength estimates. Aggregate request
rates change from baseline/candidate 2.09/5.64 million admissions/s to 1.65/5.45;
different resulting trees confound causal interpretation. The small A/B pilot
does not establish absence of candidate-dependent contention. Select **one game
at a time** for four-/six-worker primary comparisons on this six-core host.
The throughput sacrifice buys clearer resource attribution; no universal worker
or tournament-concurrency rule is inferred.

Prospective next development: separate frozen COOPERATIVE4 and ROOT_SPLIT4 versus
accepted cumulative one-worker composition, 32 paired openings each, 5+0.05,
one game at a time, fixed partition-zero development selection. Analyze only
complete batches; no strength acceptance from these screens. At pilot throughput,
each 64-game batch is roughly 16 minutes, with large paired uncertainty expected.
These compare materially different coordination/cost mechanisms. If useful signals
survive, freeze the finalist and a fresh partition-one staged confirmation before
observing any confirmation outcomes. Inherited same-worker static comparison is
also required before any production adoption. Eight calibration games from the
failed mutable-runner attempt remain excluded. No active research may start a
successor feature.

Current frozen development source is commit `3924a29`, installation
`app/build/sr036-p2-v3`, JAR SHA-256
`9c5cea7514a36e067918347d9282e04bc04b9736bdb5c13a537a0d5a1c9c2e55`.
Its accepted-production bridge also passed all 64 deterministic comparisons.
Immutable runner files are under `app/build/sr036-runner-v2`; live evidence is
`development-cooperative4/run.json`, followed by `development-root4/run.json`.
Both fixed batches are authorized and must finish/audit before analysis. Do not
launch overlapping CPU-heavy work or restart a batch with an existing output path.

While these frozen batches run, source-only follow-up adds simultaneous-owner/
generation-wrap and actual UCI worker-transition validation, and balances ordinary
proof samples by policy. These checks are pending execution, not passed results.
A UCI telemetry correction distinguishes root-only effort from full EFFORT as
`reuse=root_effort`; frozen v3 reports the older generic `reuse=effort` label for
root policies, whose exact effective composition is defined above and by
`concurrency=root_split/root_full`. The correction changes no search tree and is
not retroactively applied to frozen evidence. Also reject explicit warmupWorkers
without a Pair-2 store. These metadata/validation changes will require a new frozen
installation and class-identity bridge before independent confirmation/adoption.

The closed COOPERATIVE4 development batch passed all audits: 64 games, 23 wins,
14 draws, 27 losses; paired point estimate -21.74 Elo, unadjusted paired-normal
95% interval [-95.34,+49.92], conservative Hoeffding interval [-211.21,+154.56].
It used 902.39s wall time, 2.563 billion candidate admissions versus 0.951 billion
baseline admissions, with approximately 447.8s reported thinking time each.
This establishes neither benefit nor inferiority. ROOT_SPLIT4 is still running.

Source-only next hypothesis, ROOT_REUSE, remedies a specific root-job integration
loss: each worker privately reads the previous genuinely completed full-root PV,
and copies the eldest completed branch's quiet history before helpers start,
retaining that private history across this iteration's assigned root jobs. No
history is shared during execution. Restricted jobs cannot publish replacement
completed ordering observations, scores or root proof. The coordinator records a
new full-root observation only after every job completed and workers drained.
This refinement is pending validation and cost measurement; it has no strength
result and does not alter frozen v3 or the active experiment.

ROOT_SPLIT4 subsequently closed and passed all audits: 24/14/26 over 32 pairs,
-10.86 Elo, exploratory paired-normal95 [-98.44,+75.34], bounded95
[-196.18,+167.93]; 846.10s match wall, 1.309 billion versus 0.874 billion
admissions. Neither initial architecture establishes benefit or inferiority.
`analysis-development-v3.json` independently reaudits both batches. With their
observed paired variance and throughput, illustrative fresh 80%-power designs
under the staged critical value cost about 40-54h for +10 Elo, 10-13h for +20,
4.5-6h for +30 or 1.6-2.2h for +50. These are conditional planning scenarios,
not an adoption gain floor, required quota or true-effect prediction.

ROOT_REUSE targeted tests pass, including read-only completed-PV consumption,
restricted-job non-publication, request clearing, private owner concurrency and
270-request generation wrap per owner. The stratified real-model audit passes
680 requests, 15,466,624 fresh-evaluation comparisons and 1,319 ordinary TT proof
replays across every policy. Samples per policy range 64-250; sparse selective
stores limit coverage. The first v4 invocation used a nonexistent corpus path
and failed before observations; its empty output and failed log are retained.
The corrected `integrity-v4b.jsonl` uses the established SR-007 special/history
corpus, not reconstructed or invented prior history.

`cost-root-reuse-v4.jsonl` passes 780 complete requests. Opening depth6 wall
ratios for ROOT_REUSE 2/4/6 are .704/.566/.545, CPU ratios 1.048/1.120/1.232,
admission ratios 1.032/1.132/1.215. ROOT_SPLIT4 remains .823 wall/1.628 CPU.
Timed100ms completed depth averages 5.69 one-worker, 5.91/6.06/6.06 ROOT_REUSE
2/4/6; timed CPU ratios 1.33/1.72/1.94. This credible integration improvement
warrants new clock development. Four workers capture most observed benefit;
six add little measured depth with more CPU. This is not universal optimality.
Root jobs allocate about 37MB versus 3MB across these 32 fixed-depth requests;
GC/JIT counters are retained and no unique-position/cache-coherence rate is
claimed. Cost-task JVMs enable the incubator module; actual UCI tournament JVMs
use ordinary clean-stderr startup, so absolute rates must not be interchanged.

Prospective next development: freeze validated ROOT_REUSE4 and run 32 paired
5+0.05 openings against accepted one-worker, then the inherited OFF four-worker
path, one game at a time. Reuse the development partition-zero set for mechanism
comparison; no selection-adjusted acceptance is claimed. These ~15-minute screens
test whether the improved economics survive clock play and whether composition
improvement beats the inherited path. Independent partition-one confirmation is
required for any favorable adoption conclusion; source tuning stops before it.

The refreshed fully qualified Search/rules/UCI suite passes 765 tests with 11
existing skips (776 total), zero failures/errors, in `broad-v4-xml`. Four inference
guard tests also pass. Actual UCI lifecycle and frozen-production bridge checks
remain pending on the next immutable installation. No production adoption yet.

Frozen v4 is source `d652078`, JAR
`e600a749db510db01a329b05b9ebd0201a464584b99a0a626b8dcbde221f3e35`,
installation `app/build/sr036-p2-v4`; immutable runner is `app/build/sr036-runner-v4`.
Its accepted-production bridge passes all 64 comparisons. Actual UCI lifecycle
passes 280 requests over all ten modes and 1->4->1->6->2->16->1 worker transitions,
including history/EP node budgets, stop/reuse and quit with live work. The original
lifecycle script incorrectly required repeated identity output when Threads stayed
at one; the failed receipt is retained at `uci-lifecycle-v4`. The corrected checker
retains the last protocol identity and passes at `uci-lifecycle-v4b`; no engine or
frozen executable changed. The two ROOT_REUSE4 development batches may now run.

Validation limits: the 70 observed UCI stop round trips average 0.879ms and peak
at 1.781ms; these samples are not a scheduling/deadline guarantee. The frozen
production bridge's 52 corpus requests have identical 5,679,028 admissions on
each side and 3.586s/3.342s observed control/candidate time. Its sequential small
sample verifies behavior and shows no obvious slowdown, not statistical speed
equivalence. All clocks compare accepted policy composition in the same frozen
installation, bridged to the accepted historical executable.

Accounting scope: `abortedWorkerNodes` counts an incomplete full-root worker
invocation for racing and an interrupted individual job for splitting. Finished
jobs in an incomplete root cohort remain included in total admissions but not
that interrupted-job bucket. These categories cannot establish a comparable
unique-position duplication or wholly wasted-work percentage. CPU/allocation/GC
are measured by the cost harness, not process-tree CPU totals for tournaments.
The all-root timed aggregate also includes terminal/easy roots finishing before
the deadline; opening-only numbers avoid that particular distortion. The v4
timed sample's maximum elapsed request is 100.3221ms, not a universal bound.

Current running receipts are `development-reuse4/run.json` (one-worker comparator)
and then `development-reuse4-legacy4/run.json` (inherited four-worker comparator),
both under this report's evidence directory. Finish and independently audit each
fixed batch before interpreting it; do not restart existing output directories or
overlap CPU-heavy work. If interrupted, inspect actual process/receipt state first.
The eleven broad-suite skips are the existing `sr001h.instrumented` and
`sr001i.instrumented` opt-in historical observer tests; their production hooks are
not activated. No GUI/mobile, training campaign or unrelated full suite is claimed.

Before interpreting the refinement's speedup as purely parallel, an additional
post-match diagnostic will run the same ROOT_REUSE coordinator with one worker
directly in the cost harness. UCI one-worker behavior deliberately remains the
accepted production factory. This diagnostic-only serial-scheduler ablation can
separate scheduling/order changes from additional workers, without activating an
independent root-driver programme. ROOT_REUSE combines prior-PV consumption and
private seeded history; the current evidence does not isolate those two effects.

A source-only follow-up also checks complete root cohorts against independent
unpruned qsearch in the protected shallow/sparse domain (depth3, at most six
pieces), with 1/2/4/6 workers and repeated requests. It is pending execution until
matches end; the earlier branch-oracle checks do not themselves establish this
whole-coordinator comparison. No frozen engine or runner bytes change.

The first ROOT_REUSE4 batch closed with stable inputs/normal outcomes: 31/15/18,
score .6015625, +71.568 Elo, paired-normal95 [-0.905,+150.869], bounded95
[-98.835,+290.197]. Match wall 889.723s; candidate/baseline admissions
1,347,002,854 / 938,612,531 with 442.37s / 441.38s reported thinking time.
This is a promising development signal, not established gain: its uncertainty
includes zero and the formulation was selected after development evidence.
The independently planned inherited four-worker screen remains in progress.
Subject to its audit and the remaining diagnostics, freeze this unchanged
candidate for fresh partition-one confirmation; do not tune from confirmation.

Confirmation preparation will also test fixed-work contention for ROOT_REUSE's
lower measured CPU occupancy: isolated baseline/candidate, two baselines, two
candidates and a mixed pair, rotated across six development roots and two rounds,
500,000 aggregate admissions each. This 96-request calibration is pending until
the game batch ends. It may justify higher tournament throughput; absent convincing
comparability retain one game. No A/A absence-of-contention claim is inferred.
The existing short screens share one immutable installation with explicit separate
policies; confirmation will use separate frozen installations as the measurement
specification prescribes. Prefer the actual historical accepted binary for the
one-worker comparator, with matching preflight and source/hash identity. Its absent
pre-SR007 `internal` telemetry means OFF, now explicitly recognized by the adapter;
the still-running development uses the untouched frozen v4 runner.

All development is now closed/audited. Against inherited OFF4, ROOT_REUSE4 scores
54/4/6, +338.04 Elo, exploratory paired-normal95 [+221.55,+594.23] and bounded95
[+96.13,+infinity]. Wall757.843s; admissions candidate1.154 billion versus
baseline2.675 billion, reported thinking378.53s/373.92s. This large composition-
and-coordination improvement is distinct from the +71.57 single-worker screen;
neither development comparison is independent finalist confirmation.

The complete-cohort oracle test passes all 108 protected-domain requests over
three root policies and 1/2/4/6 workers (nine targeted tests pass overall).
The serial-scheduler ablation passes 780 requests. Opening depth6 ROOT_REUSE1
wall is 1.055x production, CPU .977x and admissions1.035x; 2/4/6 workers give
wall .717/.589/.537 and admissions1.053/1.134/1.176. Timed100ms mean completed
depth is 5.69 production, 5.63 serial-root, 5.84/6.03/6.06 parallel-root.
Thus the earlier wall improvement is not explained by a faster serial scheduler
in this sample. Absolute CPU/GC/JIT counters retain sampling noise and limits.

Contention calibration's first invocation failed before measured observations
because the opening reader requires the whole file's count, not a prefix count.
The failed `contention-v4` receipt is retained; v4b reads the frozen selection
count and then takes six roots. No engine changes were needed. The corrected
calibration is running; prospective confirmation clock/concurrency is not yet set.

The corrected contention audit passes all 96 fixed-work requests and clean
process shutdown. Mean elapsed ratios to the matched solo case are 1.367 for
two baseline engines, 1.312 for two candidates, and 1.161/1.180 for baseline/
candidate in a mixed pair. Dispersion is large; the second homogeneous instance
has no separate solo measurement and receives fewer measured requests, so JIT/
instance effects remain confounded. This does not establish negligible distortion.
Keep **one simultaneous game**. A larger calibration could clarify throughput,
but its absence does not block the conservative resource configuration.

Select unchanged ROOT_REUSE4 for two independent confirmation contrasts, both
10+0.1, one game, 64MiB requested TT and the frozen generation32 mapping. The main
contrast against the actual accepted historical one-worker executable uses
128/384/1024 cumulative fresh pairs, family index1 (.025 total alpha), existing
paired-normal/2.5-SE and bounded sensitivity, and prespecified computational
futility. At the development variance, an illustrative 80%-power target for the
observed +71.57 effect is104 pairs (~1.6h at the doubled clock); +50 is210 (~3.2h),
+30 is576 (~8.9h), +20 is1292 (~20h). These uncertain planning scenarios justify
an informative first look (~2h), with larger looks only under the frozen rule;
the 1024 endpoint targets useful precision rather than imposing a universal quota.

The second contrast asks whether the existing four-worker option improves. Use
64 independent fresh pairs against a separate frozen v4 OFF4 installation,
family index2 (.0125 alpha), one fixed look and bounded-pair Hoeffding as primary.
Its expected ~one-hour cost is proportionate to the very large development effect;
64 pairs give bounded score radius~.199 and good planning power even for a markedly
attenuated +200 Elo scenario. This is not a gain floor. The staged primary contrast
answers scalability above accepted one-worker; the fixed contrast answers upgrading
the inherited option. Neither dataset is pooled with development or the other
contrast. All openings, identities and rules must be frozen and committed before
any confirmation games. No confirmation outcome has yet been observed.

The protocols were frozen at 2026-10-13T12:52:34Z and are committed before launch.
Primary SHA-256 `1011330ff687b04c5c670c2fd2b84ff67c81a4f5eec120b86cfedd996182db45`;
inherited SHA-256 `9339beaeae6bb8e3ff9086ab07f4576f6c53c67646af5181fc801c3f25e1be2b`.
They reserve 1,088 distinct partition-one pairs from 8,866 remaining eligible
positions, excluding 39 keyed selections/current diagnostic endpoints. Two older
unkeyed selection formats were separately checked: the four A/A roots are covered
by an existing exclusion and all 64 played-corpus endpoints have zero overlap with
the reserved sample (`additional-exclusion-audit.json`). This is opening separation,
not proof of training-source independence. Frozen runner v5 recognizes the accepted
binary's older metadata; frozen assessor/statistical code is under
`app/build/sr036-confirmation-tools-v1`. Source/evidence checkpoint is `93e2d22`;
actual engine remains frozen v4/d652078. Current canon advances P2-R023->P2-R024
and P2-F027->P2-F028, preserving ACTIVE extension and inherited BASELINE.

Launch checkpoint is `a1ecd83`. Execution proceeds through primary stages only
while their frozen assessor returns `continue`, then runs the single inherited
stage. Every stage's exact command is in its protocol. Receipts are
`confirmation/primary-stageN/run.json` and `inherited-stage1/run.json`; assessments
are `primary-assessment-N.json` / `inherited-assessment-1.json`. A runner/audit
failure stops the sequence for investigation. No within-stage strength polling,
retuning or CPU-heavy overlapping work is authorized by this execution plan.

At 2026-10-13T14:11:57Z, a passive Windows process observer attached to the
already-running first primary stage's two game JVMs and Fastchess. Its binding
records command lines, parent PIDs and creation timestamps, with creation identity
verified against the opened read/synchronize handles. It waits for process exit;
it does not signal, suspend or change the processes, inspect game outcomes, or
alter any frozen input. `primary-stage1/process-cost.json` is initially `attached`
and becomes a terminal receipt after exit. Do not duplicate an attached observer.
[GetProcessTimes](https://learn.microsoft.com/en-us/windows/win32/api/processthreadsapi/nf-processthreadsapi-getprocesstimes)
provides cumulative lifetime user/kernel CPU across process threads, so attachment
during the match does not discard earlier CPU. Totals include startup, warm-up,
JIT and GC, and exclude adapter/preflight/audit processes; they are not isolated
game-clock CPU. This augments the separate request-cost harness without changing
the confirmation design or claiming equal CPU resources from equal wall clocks.
