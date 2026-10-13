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
