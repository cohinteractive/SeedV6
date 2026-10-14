# SR-024 SEE-Based Move Pruning — Phase 2

Status: **ACTIVE**, owner-authorized feature-level investigation. No production
adoption or strength conclusion yet. Only SR-024 is selected.

## Authority, bootstrap and reproducibility

The launch explicitly confirms SR-036 IMPLEMENTED acceptance, reconciliation
without remediation, and subsequent synchronization of BOTH P2-R026/P2-F030
Project Source mirrors. Those historical gates are satisfied. It also accepts
the separate all-worker feature-availability verification: cumulative Search
remains a requirement throughout supported worker counts 1..16. Future SR-024
acceptance and mirror synchronization remain external owner actions.

Starting clean HEAD: `d96476053f8f9f7bf57fbe3c58c60bac4f4a00b7`.
Starting canon: P2-R026/P2-F030, SR-024 PENDING, no ACTIVE feature.
Application sources have no diff from the frozen SR-036 source
`b73a646d4232058f1b927beac52418839cd64520`.
Frozen comparator: `app/build/sr036-production-v1/lib/app.jar`, SHA-256
`bec9636831288f346f2a3590dcf6c3fa558ed65fb967e83c85e9daed79fb51b0` (verified).
The frozen Phase1 Contract/Frontier hashes match
`9f820c2e9efd366b179902d3fc14bdd8f88e6ae793356aded6b6119f73e8401b` /
`ead3743400b1dc513e02ad1c776b39db1486e8800c947cece9e20ba199e8fc1a`.

Fixed evaluator: store
`E:/SeedV6-Networks/BRN-Pair2/235d0688-e060-44e3-8f1e-119c39bc8017`, checkpoint
`g000032-s001753088-5837c7f7192aa3cf9a265388f93649327455e804541398ae3ef6b69e09014304`.
All four checkpoint hashes independently match the
[measurement specification](BRE_PAIR2_MEASUREMENT_BASELINE_2026-10-09.md), including
network `5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7`.
Quarter-residual mapping remains unchanged. Host: Ryzen 5 5500, six physical /
twelve logical cores; `C:/java/jdk-21/bin/java.exe` reports Java 21+35-2513.
Runtime preflight and targeted validation are the next steps; static identity
checks do not establish runtime correctness.

No root CODEXLOG_CURRENT.md exists; none is created. Build remains 35.
Both pinned shared-finalizer source hashes match. The local optional begin
capture was stopped at its verified owned PID before reservation/version
mutation after prolonged full-tree capture; a concurrent initialization retry
was refused by the OS command lock and made no changes. Final status is idle,
with no active/unfinished operation or blocker. No finish/replacement bump or
version repair is required. This is a non-blocking initialization warning under
the SeedV6-specific owner override, not a verified bump.

## Actual baseline and bootstrap challenge

`ProductionSearch.create` selects native EFFORT for one worker and ROOT_REUSE
for 2..16 Pair-2 workers. Both use unpruned qsearch, protected quiet LMR and
WINDOW LMP. Shared workers use the same recursive cumulative composition,
with completed-root ordering and private seeded history. HCE and explicit
static controls retain their separate policies. No Pair-2 NMP or IID/IIR is
adopted. Default workers remain one.

The current SEE is exact for an **optional destination-square exchange game**:
it considers all legal recaptures and promotions, including changing pins and
king safety. It is not a position-score bound. The production sign test is a
nonallocating threshold recurrence, not an assumed greedy least-attacker swap.
SR-015 already computes that sign for tactical ordering, so some pruning can
reuse evidence without a second exchange calculation. Ordinary quiet moves are
outside its API; a quiet formulation requires separately validated semantics.

Historical evidence inspected: frozen SR-015/SR-024 canon, the complete local
`C:/Users/Central/Documents/SeedV6-SR024-2026-10-01/REPORT.txt`, and
[SR-001B](SEARCH_QUIESCENCE_SEE_SR001B_2026-09-28.md). Phase1 capture omissions
had false predictions; refined D2..4 quiet omission had concentrated isolated
savings but flat/slower complete iterative cost. That HCE/static-leaf evidence
is not current Pair-2 rejection. Qsearch SEE omission lost promotion/draw,
checking-mate and nonchecking-clearance resources. Protecting checks/promotions
retains the former resources but cannot protect all off-destination tactics.

Initial priorities are hypotheses, not a compulsory matrix or adoption choice:

- Reuse the existing negative tactical class for protected qsearch omission;
  compare whether retaining the first qlayer better preserves tactical resources.
- Contrast omission with tactical postponement, retaining legal alternatives.
- Investigate protected near-horizon nominal capture omission only after actual
  opportunity/cost observations; preserve searched prefixes and conservative proof.
- Revisit quiet exchange information if current opportunity and measured cost
  justify it; do not transplant HCE numerical margins into Pair-2.

Every experiment compares within worker domains against current production,
especially EFFORT1 and ROOT_REUSE4. Independent ExactSearch/TT-off and unpruned
SR-001A references remain unchanged. Omission supplies no searched child,
history outcome, PV suffix or ordinary mathematical proof. No dependency
discharge or TT architecture redesign is proposed.

## Research continuation

Next: run existing independent SEE/oracle and relevant cumulative Search tests,
verify frozen-production UCI identities at one/four workers, then add explicit
OFF-default research controls and worker-private mechanism diagnostics.
Use historical tactical witnesses before clock screens. Select subsequent
formulations from observed economics and failures; no successor may be activated.

Reuse established four-game single-worker concurrency and one-game four-worker
configuration only while workload/host conditions remain comparable. Preserve
complete request cost, actual workers, clocks, CPU and game audit evidence.
Clock screens are exploratory; a surviving finalist needs fresh independent
confirmation with frozen identities, paired openings, uncertainty, stopping and
multiplicity rules specified before outcomes. No fixed game quota or Elo floor
is imposed. Durable evidence belongs beside this report in
`sr024-phase2-2026-10-14/`.

## First implementation and validation checkpoint

The frozen baseline preflight passed four real-model 100,000-node requests at
workers 1 and 4, with exact checkpoint/mapping and cumulative policy identities.
The pre-change targeted suite passed **85 tests**, no skips/failures/errors.
Evidence: `bootstrap/` and `bootstrap-xml/` beneath the report evidence directory.
System Python lacks python-chess; the established research virtual environment
`app/build/search-baseline/venv/Scripts/python.exe` is used without dependency changes.

Explicit `seedv6.uci.seeResearch=off|qsafe|qdeep|main|order` defaults OFF.
QSAFE omits negative nonchecking ordinary captures outside check; promotions and
EP remain protected. QDEEP additionally keeps the complete first qlayer. MAIN
omits later negative captures at interior narrow windows and nominal depths1..3,
protecting checks, promotions, EP, hash/completed guidance, first searched moves,
negative alpha and mate windows. Alpha>=0 protects a potential omitted rule draw;
it is not an imported evaluator surplus margin. ORDER retains all captures but
moves negative captures after quiets, requiring earlier quiet generation/history
sampling. EFFORT/hash guidance retains priority. These are distinct exploratory
mechanisms, not a jointly adopted policy. All retain cumulative LMR/WINDOW/EFFORT
and apply to every configured ROOT_REUSE worker, with private diagnostics.

New tests pass **17 tests**, including **272 independent full-legal-enumeration
qsearch comparisons**, original checking/promotion/clearance fixtures, actual
omission guards, selective/root-proof exclusion, OFF equivalence, cancellation/
reuse, all supported worker counts and incompatible UCI option rejection.
Three failed test attempts are retained: a test wrongly required a completed
iteration under a tiny budget; an invalid qpolicy spelling obscured the intended
option-conflict assertion; a short dynamic cohort did not schedule work on one
of sixteen workers. The tests now express valid lifecycle/configuration/scheduling
requirements. No production defect was hidden by these corrections. Deep actual
per-helper execution remains a separate measurement/possible-adoption requirement.

Frozen exploratory installation: `app/build/sr024-p2-v1`, JAR SHA-256
`8dd1b404b528cc823050c23967e26c445fa5e701049a08487e56a764bf382dc6`.
`v1-identities.json` binds its full main-source manifest; test probe classes are
frozen separately under `app/build/sr024-probe-v1`. The source metadata records
an uncommitted snapshot; this is not misrepresented as a prior accepted binary.
Production defaults and frozen references remain unchanged.

Current work: rotated complete-request cost comparisons at 1 and 4 workers,
using the 26-position full-history SR036 diagnostic corpus, depth6 / 100,000
nodes / 100ms budgets, two rounds, and three 500,000-node warmups per mode.
The first diagnostic launch lacked JUnit on its test-harness classpath and failed
before a measured row; its empty output and failure remain excluded. The corrected
`cost-v1b.jsonl` run supplies the required classpath, recorded separately.
No clock-strength evidence exists yet. Upcoming initial exploratory screens use
32 newly selected paired/color-balanced partition-zero openings (seed2026101424),
5+0.05, frozen accepted comparator within the same worker count, four simultaneous
games for one worker or one game for four workers. Candidate investment remains
evidence-driven; these are development batches, never confirmation samples.

### Completed diagnostics and development launch

Source/evidence checkpoint: `e67b1a05a8554df2a7b94c9ccec57000515c000b`.
`cost-v1b.jsonl` closes **1,560 requests** successfully. Depth6 aggregate wall/node
ratios against OFF within one worker are QSAFE .731/.683, QDEEP .812/.768,
MAIN .909/.898 and ORDER .964/.942. Within four workers: .720/.668, .811/.764,
.991/.939 and .972/.936 respectively. Fixed-node wall ratios exceed one for
all candidates; reduced tree work is not improved per-node throughput. Terminal/
ceiling fixtures inflate aggregate completed-depth averages. CPU/JIT/GC/allocation
and full requests are retained in `analysis-cost-v1.json` and the raw evidence;
the single-process CPU samples are descriptive, not isolated engine lifetime cost.

The separate 260-request `mechanism-v1.jsonl` passes. Every studied mechanism
executes in the owner and all three ROOT_REUSE helpers. One-worker QSAFE/QDEEP
omit 317,155/193,917 qcaptures; MAIN omits 22,291 nominal captures. Four-worker
QSAFE omits 214,744 in the owner and 35,339/32,828/32,993 in helpers. Diagnostics
are private and excluded from clean cost timing; these counts do not prove strength.

`uci-v1c/verification.json` passes **18 deterministic OFF-versus-frozen comparisons**
(36 requests), **20 policy preflight requests** and **20 worker-transition requests**.
The first two bridge attempts failed in the new Python harness before comparisons:
unchanged Threads emits no redundant identity, and opening_prefixes requires the
full declared file count. Those failures/transcripts are retained; the corrected
harness repeats UCI explicitly and takes eight of the declared 32 openings.
There was no engine failure or identity substitution.

`aa-v1` closes eight normal paired games, **3/2/3**, at 5+0.05/concurrency4/one worker.
Inputs, legal PGN histories, pairing, termination and request accounting pass.
Native retained Windows handles measure full engine lifetime CPU: baseline
71.21875s, current OFF71.6875s (1.0066x), including startup/warmup/JIT/GC.
This validates infrastructure, not equivalence or immunity to clock contention.
Accounting binds every Java child to its actor, parent runner and creation time.

All four hypotheses have measured opportunity or cost headroom. Proceed with
four separate fixed 32-pair development batches, QSAFE/QDEEP/MAIN/ORDER1 against
accepted production1, 5+0.05/concurrency4 and the same 32 paired openings.
These exploratory comparisons can select candidates but cannot establish adoption.
No favorable optional stopping, within-batch source retuning or pooling into
confirmation is permitted. Evaluate completed results and adverse mechanisms before
choosing four-worker screens or further formulations. Candidate remains frozen v1;
runner dependencies are frozen in `app/build/sr024-runner-v1` before launch.
