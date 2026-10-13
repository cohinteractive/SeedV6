# SR-036 Concurrency Extension — Phase 2

Owner launch: 2026-10-14 local. **ACTIVE; research incomplete.** Only SR-036 is
authorized. Its inherited optional same-depth coordinator remains **BASELINE**.
This report and the active Contract/Frontier are the restart record; no successor
feature is selected. Technical completion will not establish owner acceptance.

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
