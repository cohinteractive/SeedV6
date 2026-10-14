# SR-024 SEE-Based Move Pruning — Phase 2

Status: **ACTIVE; owner-confirmed idle host, fresh replacement confirmation being
prepared.** No production adoption or technical feature closeout yet. Only
SR-024 is selected. The chronological entries below record what was known at each
checkpoint; the resource interruption and resumption conditions at the end supersede
earlier running/next-step statements.

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

`aa-v1` closes eight normal paired games, **3/2/3**, at 5+0.05/concurrency 4/one worker.
Inputs, legal PGN histories, pairing, termination and request accounting pass.
Native retained Windows handles measure full engine lifetime CPU: baseline
71.21875s, current OFF71.6875s (1.0066x), including startup/warmup/JIT/GC.
This validates infrastructure, not equivalence or immunity to clock contention.
Accounting binds every Java child to its actor, parent runner and creation time.

All four hypotheses have measured opportunity or cost headroom. Proceed with
four separate fixed 32-pair development batches, QSAFE/QDEEP/MAIN/ORDER1 against
accepted production1, 5+0.05/concurrency 4 and the same 32 paired openings.
These exploratory comparisons can select candidates but cannot establish adoption.
No favorable optional stopping, within-batch source retuning or pooling into
confirmation is permitted. Evaluate completed results and adverse mechanisms before
choosing four-worker screens or further formulations. Candidate remains frozen v1;
runner dependencies are frozen in `app/build/sr024-runner-v1` before launch.

### Additional premise checks prepared during the frozen screens

The accepted SEE uses P100/N320/B330/R500/**Q975**; the fixed Pair-2 innate
material term uses Q900 (with unchanged quarter-residual learned information).
Thus SEE sign is reused as an existing heuristic, not asserted to equal neural
material or full evaluation. A test-only independent legal-exchange/cost probe
will measure sign sensitivity to Q900 and allocation/runtime cost after the clock
batches, without changing their binary or competing for their CPU. Even a matching
material term would not make exchange information a position-score proof.

Primary reference inspection: pinned Stockfish17.1
[search](https://raw.githubusercontent.com/official-stockfish/Stockfish/sf_17.1/src/search.cpp)
uses separate depth/history-dependent capture and quiet SEE thresholds, and SEE
within qsearch. Its
[exchange implementation](https://raw.githubusercontent.com/official-stockfish/Stockfish/sf_17.1/src/position.cpp)
uses a least-attacker recurrence with pin filtering and special-move handling.
This differs from SeedV6's exhaustive legal optional-exchange contract. These
references motivate distinct evidence/cost questions; their constants, selective
proof assumptions and tactical exceptions are not transplanted or authoritative.

### First completed exploratory result

QSAFE1 closes 32 pairs/64 games: **22 wins, 20 draws, 22 losses**, 0.00 Elo,
unadjusted paired-normal95% interval **[-51.07,+51.07]**; pair bins1/7/16/7/1.
All games terminate normally, input identities remain stable, and the independent
legal-history/pair/request audit passes. Match wall 248.72s; lifetime baseline/
candidate CPU496.3125/497.328125s (1.00205x). Baseline/candidate admissions are
869,208,489/823,418,997 over 4,664 requests each. Equal clocks do not imply equal
nodes or general equal-compute strength. `analysis-qsafe1.json` preserves exact
statistics/resources and limits. The outcome is inconclusive, not equivalence,
inferiority or a feature-level disposition. Remaining frozen development batches
continue; no confirmation selection or rule is made from this single screen.

Using this exploratory variance and observed throughput, illustrative80%-power
normal planning estimates with a conservative family/look critical value are
about 650 pairs/1.40h for a20-Elo effect and 2,595 pairs/5.60h for 10 Elo at this
short-clock/concurrency configuration. These are planning estimates, not gain
floors, compulsory quotas or the cost of the longer-clock/four-worker domains.

## Completed first development contrasts and next hypotheses

All four predeclared 32-pair screens closed with 64 normal games, stable inputs,
complete legal/pair/request audits and successful lifetime process accounting.
They share development openings and are exploratory, with no acceptance claim.

| One-worker policy | W/D/L | Elo | Unadjusted paired-normal 95% | CPU ratio |
|---|---:|---:|---:|---:|
| QSAFE | 22/20/22 | 0.00 | -51.07 to +51.07 | 1.0020 |
| QDEEP | 23/11/30 | -38.15 | -103.50 to +24.58 | 0.9971 |
| MAIN | 20/15/29 | -49.18 | -126.22 to +23.27 | 1.0023 |
| ORDER | 26/11/27 | -5.43 | -85.81 to +74.36 | 1.0088 |

None demonstrates gain, equivalence or inferiority. MAIN saved little complete
four-worker depth cost, while ORDER saved little in either domain. Their negative
or near-zero clock estimates do not motivate expensive confirmation now. QDEEP's
first-layer safeguard neither removes deeper off-square tactical risk nor improves
the observed strength/resource trade-off. QSAFE retains substantial node savings
but has no observed one-worker gain; its four-worker interaction remains open.

Two new, separately named exploratory policies test different adverse mechanisms:
**QSCOUT** preserves original full-window qsearch calls and restricts omissions to
original null windows, where a coarse prediction might be more useful. A targeted
clearance witness explicitly checks that its narrow prediction can still be wrong;
it never becomes ordinary proof. **QPAWN** preserves exchange losses up to one
material pawn, including B-for-N differences. This is an exchange-material
allowance, not an HCE static-evaluation surplus transplanted to Pair2. It requires
an extra threshold recurrence only for an already negative, nonchecking capture.
Its usefulness must survive measured overhead and clock play, not just node counts.
Both continue to protect checked nodes, checks, promotions and EP. Production
remains OFF. The original frozen v1 binaries/runners are unchanged.

An isolated test-only SEE probe will cross-check random legal tactical exchanges
against an independent oracle, compare sign/numeric runtime, and inspect queen900
versus existing queen975 exchange signs. Those diagnostics cannot establish
position-score bounds or strength and do not change the evaluator.

Current resume point: first development batch complete; validate/freeze the new
policies and measure their request economics before selecting additional games.

V2 targeted tests passed 18 tests (340 independent q-oracle comparisons), and its
UCI bridge passed 18 deterministic OFF comparisons,28 policy preflight requests
and 28 worker transitions. Frozen v2 JAR SHA-256:
`9e8b202f3001ceb3e49029927d4dfa58841065084b0660c4baa306d47edc60ff`.
The first cost launch failed before Java main because PowerShell split an unquoted
`-Djava.awt.headless=true`; `cost-v2-launch.txt` is retained, with no observations
pooled. The corrected v2b launch quotes the property.

The isolated probe passed 10,227 tactical moves from 2,521 positions against the
independent legal exchange oracle. Sign counts:5,541 positive,1,319 zero,3,367
negative. Of negatives,289 lose at most a pawn, including206 B-for-N losses.
Queen900 versus queen975 produced no sign differences in this corpus; it does
not currently motivate an exchange-value change. Median measured threshold cost
33.17ns/call versus numeric69.19ns/call supports reusing the existing sign. Small
measurement-loop allocation (~.003 bytes/call) includes iteration machinery;
this is not a measured allocation inside SEE. Random legal games are neither
representative search frequencies nor evidence that quiet/capture pruning wins.

A materially distinct quiet hypothesis remains worth a bounded current-composition
probe: exchange-negative non-pawn/non-king quiet moves can survive WINDOW, especially
at depths3..4. Unlike tactical policies, quiet SEE is extra work, so it must earn
its cost. The prospective QUIET experiment will protect the first 3 searched quiets,
root/checked/full-window nodes, checks, hash/completed guidance, pawns/kings and
negative-alpha/mate windows. It will examine depths2..4 after existing WINDOW
omissions, using exact optional destination exchange negativity without an HCE
static margin. This tests earlier material risk among moves WINDOW retains; it
does not replace WINDOW or weaken the independent reference. A separately named,
independently oracle-checked quiet exchange entry will preserve the tactical API.

## V2/V3 economics and prospective second development batch

V2 clean diagnostics closed 936 complete requests and 156 separate instrumented
requests. Depth6 wall/node ratios: QSCOUT1 .734/.695, QPAWN1 .793/.744,
QSCOUT4 .757/.713, QPAWN4 .809/.739. Fixed-node wall ratios were1.046/1.059
for QSCOUT/QPAWN at one worker and 1.083/1.072 at four: tree reduction is still
the opportunity, not per-node speed. Owner and every one of three helpers recorded
actual omissions. Terminal fixtures can finish early or hit the supported depth
ceiling, so aggregate completed depth is not interpreted as useful playing depth.

QUIET preserves existing WINDOW first, then applies the documented scout/prefix
protections to remaining moves. Its separate SEE entry accepts an already prepared
legal non-king quiet child and evaluates only optional destination recaptures.
The independent allocating oracle matched189,830 thresholds over 18,983 quiet moves,
including4,737 negative exchanges. All19 v3 targeted tests passed, including the
340 qsearch oracle cases, omission guards, no-proof assertions and lifecycle tests.
V3 frozen JAR: `9e30e512ba9bba2dd538622b0a9baab683eba2236db82b9921280a457c9580e6`.
Its624 clean requests gave QUIET1 wall/node ratios .957/.964 at depth6 and
QUIET4 1.019/1.022. Separate104 instrumented requests confirm actual use. The small
one-worker savings and absent four-worker savings limit its appeal; modest timing
differences and noisy JVM CPU values do not establish an optimization.

The next exploratory batch is fixed before outcomes: QSCOUT1 and QPAWN1 use frozen
v2, QUIET1 frozen v3, each against accepted production for 32 pairs/64 games on the
same development openings,5+0.05, four simultaneous games and existing resource
settings. About12 further wall minutes at measured throughput can distinguish a
strong adverse trade-off or identify a policy worth a more informative four-worker
screen. These samples cannot resolve small gains; their purpose is internal
selection. No independent confirmation rule or outcome is changed. Broader
Search/rules/UCI/SEE regression and v3 OFF/option/lifecycle bridge precede games.

That regression completed:806 tests total,795 passed,11 existing disabled
QuiescenceDeltaEvidence/PostMoveEvidence diagnostics skipped, zero failures/errors.
The v3 UCI bridge passed 100 real-model requests:36 requests in18 deterministic
OFF comparisons,32 option/policy preflight requests and 32 transitions across
1/4/16/1 workers. The earlier v2 bridge totals92 requests. All eight v3 policy
identities retain the cumulative recursive mechanisms; these checks do not claim
strength or deep per-helper activation at every worker count.

An extra warmed OFF cost bridge made32 paired500,000-node requests after three
1,000,000-node warmups per engine, rotating request order across eight openings.
Every pair matched nodes/score/bestmove/PV. Baseline total request time8.607s,
v3 OFF8.509s, aggregate ratio .9887 and paired median .9858. No slowdown was
observed here; this descriptive one-host probe is not equivalence, strength or
cross-platform performance evidence. Model load/startup/warmup are excluded;
aborted iterative work inside each request is included. Evidence: `off-cost-v3/`.

Current resume point: QSCOUT1/QPAWN1/QUIET1 fixed exploratory games are running
sequentially, with no concurrent Java tests/builds/benchmarks. No outcomes have
been used to change their policies or fixed sample sizes. Select the four-worker
follow-up and any independent finalist only from completed, audited evidence.

QSCOUT1 has now closed:23/15/26 W/D/L, -16.30 Elo, unadjusted paired-normal95%
[-92.29,+58.14], process CPU ratio .9928,231.74s match wall. Preserving original
full windows has not produced an observed gain despite most QSAFE node savings.
The other two fixed screens continue unchanged. Note that fixed-node runtime
differences include changed node composition: pruning away cheap nodes can lower
NPS without equivalent added per-call overhead. Only the isolated SEE probe
measures its own call cost; none of these timings alone establishes strength.

The second batch is closed. QPAWN1 scored19/18/27, -43.66 Elo, unadjusted95%
[-115.92,+24.98], CPU ratio .9983,248.04s wall. QUIET1 scored21/16/27,
-32.67 Elo, [-111.86,+43.23], CPU ratio .9951,229.74s wall. Each has64 normal
audited games; neither establishes inferiority. The quiet policy's small savings
and weak clock estimate do not justify large additional precision now.

All eight v3 policies passed a real-model integrity probe:1,650 bounded native
EFFORT fixed-depth invocations / ROOT_REUSE4 driver requests, including incomplete
iterations and reuse. Every one of 12,087,665 actual rolling evaluator calls matched
fresh recomputation. Independent nominal/static search verified722 sampled ordinary
TT proofs with full legal history and mate normalization. Root boards, legal PVs,
request work accounting and helper drain passed. This is instrumented correctness,
not timing or strength evidence. The native one-worker part uses the same recursive
EFFORT owner with explicit request boundaries; its actual UCI/driver integration
is separately covered by the frozen bridges. See `integrity-v3.jsonl` and source.

QSAFE remains the leading current formulation by work reduction, non-adverse
one-worker point estimate and lower observed pair variance. The next fixed worker
contrast is QSAFE4 versus accepted ROOT_REUSE4:32 pairs,5+0.05, one simultaneous
game, frozen v1, same development openings and model. Expected wall cost is roughly
four times the one-worker screen; it tests transfer to the actual parallel tree,
not isolated worker scaling or equal CPU. No one/four-worker scores will be pooled.

The known nonchecking clearance failure also motivates one structural safeguard,
**QXRAY**, rather than further exchange-margin fitting. Before omitting a negative
ordinary capture, preserve it when vacating its source uncovers another friendly
slider's geometric attack on an enemy non-pawn/non-king piece. Checks/promotions/EP
remain protected as before. This can retain the witnessed off-square resource
without a new search, but is still heuristic: pins, reply choices, moved-piece
threats and other sacrifices are not resolved. Its ray mask is checked against an
independent square-scan oracle and the original clearance witness plus colour
mirror. Only a useful measured trade-off warrants a further clock screen. This
does not change any frozen v1/v2/v3 candidate, game or concluded statistical result.

QXRAY targeted validation passed 21 tests:3,424 independent clearance-mask
comparisons (103 protected captures), both-colour witness retention,408 independent
qsearch oracle comparisons, unchanged quiet oracle and policy/UCI guards. V4 source
snapshot is `28207f5942dd4f5aedb7ab0adbe7eb09ca594afb`, JAR
`86df1169ee1e0680f9f4bafa78f577839996d373cba1072edf12a348eb629152`.
Its UCI bridge passed 108 requests, including18 OFF deterministic pairs and every
policy's 1/4 preflight and 1/4/16/1 transitions. The first v4 cost run overlapped
source-history extraction, so its timing is not used as clean evidence; raw output
is retained and a clean v4b repeat will determine the cost conclusion. The separate
mechanism and runtime integrity evidence is unaffected by that timing limitation.

Source continuity: v1 runtime source is retained at `e67b1a0`; v3 at `5407759`;
v4 at `28207f5`. V2 was built between commits. `v2-sources.zip` now preserves329
source files, every byte checked against its original pre-experiment manifest.
The few Java differences were recovered from the v3 commit by removing only the
later QUIET addition; frozen runner originals were copied directly. A first
attempt to derive a runner from the later Git snapshot failed its hash check and
produced no archive. No unverified reconstruction is retained. The preservation
receipt records the archive hash and limitation; it does not invent a historical
v2 source commit or alter original game identities.

Clean v4b economics closed 624 requests: QXRAY1 depth6 wall/node .7665/.7003,
QXRAY4 .7147/.6918; fixed-node wall 1.0724/1.0547. Instrumented104-request
coverage observed omissions in the owner and each of three helpers. The expanded
v4 integrity probe passed 1,857 observations,13,426,998 rolling/fresh evaluator
comparisons and 823 independently replayed ordinary proofs. V4 OFF's separate 32
paired request probe preserved all deterministic results but measured wall ratio
1.0200 (median1.0121). This small timing difference is descriptive and unresolved;
the earlier v3 OFF probe was .9887. Do not claim zero runtime overhead or infer
strength from either short timing comparison.

Prospectively selected next fixed clock batch: QXRAY1, frozen v4,32 pairs at
5+0.05/concurrency 4 on the established development set; then the already specified
QSAFE4, frozen v1,32 pairs at 5+0.05/concurrency 1. QXRAY's narrow structural
safeguard fixes an actual adverse witness while retaining most savings, justifying
roughly four more wall minutes of screening. QSAFE4 addresses the remaining major
worker-domain uncertainty. No builds or CPU-heavy diagnostics overlap these games.

QXRAY1 closed successfully: **31/15/18 W/D/L, +71.57 Elo**, with unadjusted
paired-normal 95% interval **[-8.66,+160.29]**, pentanomial [4,2,12,5,9] and
pair-score variance .108304. All 64 games ended normally, full audits passed,
match wall was 237.64s and lifetime CPU ratio 1.00094. This is the first positive
development lead, selected among eight formulations; neither the point estimate
nor its exploratory interval establishes adoption or the guard's isolated Elo.
QSAFE4 continues under its unchanged fixed design. A subsequent QXRAY4 screen
with the same 32 development pairs/5+0.05/concurrency 1 is now justified to inspect
the promising policy's actual worker-domain behavior before independent confirmation.

Prospective confirmation planning (no confirmation games yet): QXRAY1's observed
variance suggests about 1,587 pairs for illustrative 80% paired-normal power at
20 Elo with family/look adjustment, ignoring early futility. At twice measured
short-clock throughput this projects about 6.55h at 10+0.1; 40 Elo needs about
400 pairs/1.65h and 10 Elo about 6,337 pairs/26.1h. These are uncertain plug-in
information/cost forecasts, not gain floors or guarantees. A first look around
160 pairs expects 30 below-half opening pairs from the development distribution,
supporting the 20-each-side normal-eligibility rule while testing the large observed
signal cheaply. Geometrically spaced subsequent looks near 512/1,600 provide
useful precision increases without repeatedly peeking. Final protocol identities,
openings, looks and stopping rules must be frozen before confirmation outcomes.

QSAFE4 closed: 26/11/27 W/D/L, -5.43 Elo, exploratory paired-normal 95%
[-72.18,+60.92], pentanomial [4,4,16,5,3], all 64 normal games. Match wall
867.26s; lifetime CPU baseline 781.797s, candidate 790.500s, ratio 1.01113.
Neither native EFFORT1 nor ROOT_REUSE4 provides a positive QSAFE development
signal. This does not establish equivalence or inferiority.

The independent **QXRAY1** protocol is now frozen before any confirmation game:
`confirmation-qxray1-protocol.json`, SHA-256
`6fe224a7b903c4203c53a0760cc7cca1466c2b0bfe675996d2583124e2d51b04`.
It compares frozen v4 QXRAY1 against accepted production1 at **10+0.1**, four
simultaneous games, with cumulative looks **160 / 512 / 1,600 pairs** (stage
increments 160/352/1,088). One uniform fresh partition-1 sample is sliced before
outcomes; all 1,600 positions are distinct and excluded from 9,605 prior/used
positions. There were 7,778 available eligible positions after exclusions.
Independent prefix/key/hash checks passed for all stages and 66 frozen files.

Family alpha .05 is allocated geometrically across confirmation contrasts;
this first contrast receives .025, split over its three looks. The primary
paired-normal adjusted interval must exclude zero Elo and the 2.5-SE sensitivity
must agree; normal inference requires at least 20 below-half and 20 above-half
pairs. Adjusted bounded Hoeffding sensitivity is always reported and is primary
when the normal eligibility condition is absent. Plug-in conditional power below
.10 stops for computational futility; a final undecided maximum is inconclusive.
No post-stop extension, retuning, exploratory pooling or live score looks are
allowed. Early futility reduces power relative to the planning estimates.

Four statistical-rule tests and five no-engine executor gate tests pass, including
frozen-input rejection before module import, stopping after positive/futility,
failure without stage advancement, and protocol mutation blocking assessment.
These synthetic outcomes are not chess evidence. The executor only launches
stages authorized by the preceding completed assessment. Current resume point:
QXRAY4's fixed development screen is running; the frozen QXRAY1 confirmation has
not started. Its results and final worker-domain design must remain separate.

QXRAY4 also closed positive but inconclusive: **29/15/20 W/D/L, +49.18 Elo**,
unadjusted paired-normal 95% **[-28.00,+131.61]**, pentanomial [4,3,12,6,7],
pair-score variance .101752. All 64 games/audits passed. Wall 882.78s; lifetime
CPU baseline 801.547s, candidate 820.078s, ratio 1.02312. Eight one-worker and
two four-worker contrasts now total 640 development games, each on the same
32 paired openings; they remain excluded from independent confirmation.

Resource reassessment is justified by the two completed four-worker matches:
their total lifetime CPU/wall averages are about 1.81/1.84 cores on the six-core
host. This leaves a credible opportunity for concurrency2; the old one-game
setting is not presumed universally optimal. A small prospective calibration
will use the first four existing development openings: A/A at concurrency2,
then QXRAY-versus-production at concurrency1/2/2/1, four pairs per block,
5+0.05 and four workers per engine. This is resource/infrastructure evidence,
not new independent strength evidence or a policy retune. Two games will be
selected only with complete normal/legal/option/process audits, materially
better throughput (at least1.3x across equal-game counterbalanced blocks), no
gross asymmetric resource shift or memory pressure, and no substantial reduction
in each actor's observed request throughput. Small samples cannot establish
concurrency equivalence; the eventual measured conditions and limitations will
be explicit. A few minutes of calibration may avoid hours of unnecessary serial
testing. The already frozen one-worker confirmation remains unchanged.

Calibration closed and independently audited all 40 games: A/A4 at concurrency 2
scored 3/2/3; the four QXRAY resource blocks scored 5/2/1, 4/1/3, 5/0/3 and
3/3/2. These reused four development openings and are **not** independent strength
evidence. The ABBA blocks took 105.19/63.88/54.86/116.22 seconds, giving 1.86456x
aggregate game throughput at concurrency 2. Lifetime candidate/baseline CPU ratios
were 1.02708 in the two serial blocks and .96694 in the two concurrent blocks;
game lengths and trees differ, so these do not establish CPU equivalence.

A separate controlled test used two warmed four-worker engines, eight development
roots, four rotated repetitions and 256 measured 300,000-node requests. All exact
budgets, identities, legal PVs and frozen hashes passed. Concurrent per-request
latency rose 8.21% for accepted production and 10.11% for QXRAY; two-engine group
speedups were 1.7505x and 1.7236x. This modest contention is real, with about 1.8%
relative latency disadvantage for QXRAY, and is not erased by better throughput.
It does not constitute the gross actor slowdown or asymmetric resource shift that
would defeat this calibration. About 16.2 GiB memory remained free after the clock
blocks. Choose **two simultaneous games** for four-worker confirmation, recording
these measured limits rather than claiming no contention or optimal scaling.
See `concurrency-analysis.json` and `concurrency-fixed-node/result.json`.

Prospective independent QXRAY4 design: 10+0.1, two simultaneous games, four workers
per side, frozen v4 versus accepted ROOT_REUSE4; cumulative looks **160/352/768
pairs**, contrast index 2 (alpha .0125 over three looks; z=2.86526). The same
normal-eligibility, 2.5-SE, bounded sensitivity and futility rules apply as QXRAY1.
The 32-pair QXRAY4 development variance .101752 implies illustrative 80% normal
power, ignoring futility, at 30 Elo around 754 pairs/6.2h; 20 Elo requires about
1,692/14h and 10 Elo 6,755/56h at the measured doubled-clock calibration forecast
of 29.69 seconds per pair. The positive 49-Elo lead and mechanism savings justify
768 pairs of maximum information; much larger small-effect precision is materially
more expensive. This is a planning sensitivity, not a minimum accepted gain.
The first 160 pairs expect roughly 35 below-half pairs; the geometric middle is
352. Follow the frozen boundary, without post-stop extension. Both worker domains
must support responsible adoption; no favorable result is assumed transferable.
The closed concurrency-b run supplies audited runtime options to protocol creation,
not a strength estimate. Fresh openings exclude QXRAY1's complete reserved sample.

The QXRAY4 protocol is frozen as `confirmation-qxray4-protocol.json`, SHA-256
`5b60641ffada0cd667d26dd03f70fd9d86999e93e9ef040106a8a2161498e237`.
Its 768 distinct partition-1 openings were sampled from 6,178 eligible positions
after 11,177 distinct exclusions, and are disjoint from all QXRAY1 stage keys.
All bound file hashes and stage cardinalities were checked before execution.
Current resume point: both protocols are frozen; execute QXRAY1 and then QXRAY4
without overlapping matches, builds or CPU-heavy diagnostics. No confirmation
outcome has yet been observed. Only completed prescribed looks permit decisions.

QXRAY1 confirmation look 1 is closed and independently audited: **160 pairs /
320 normal games, 130/69/121 W/D/L**, +9.774 Elo. Pentanomial [20,24,65,29,22];
pair variance .0866721, with 44 below-half and 51 above-half pairs. The adjusted
primary interval is **[-32.995,+52.843]**, required 2.5-SE [-30.740,+50.557],
and adjusted bounded sensitivity [-82.693,+103.681]. The prescribed decision is
**continue**: neither positive nor negative, and plug-in conditional power .22155
exceeds the .10 futility threshold. The weaker confirmation estimate does not
validate the development gain; it also cannot justify overriding the frozen rule.
Stage 2 is running toward the predeclared cumulative 512-pair look.

Look-1 match wall was 2,260.723s (37.68 minutes), about 14.13 seconds per pair
at four simultaneous games. Lifetime CPU was baseline 4,454.875s / candidate
4,451.953s, ratio .999344. Complete-request totals were 21,133/21,139 requests,
6,585,756,735/6,215,170,108 nodes and 4,465.198/4,466.637 seconds for baseline/
candidate. All normal endings, full real histories, legal PVs, colors/pairs,
actual options, process exits, accounting and frozen input hashes passed.
No partial stage-2 scores are inspected. QXRAY4 remains independently frozen and
will run after QXRAY1 closes, without overlapping clock matches or heavy QA.

### Resource interruption and explicit owner wait — 2026-10-14 UTC

A read-only host sample at 04:59:03Z found only 4.79 GiB physical memory available
(versus about 16.2 GiB after calibration). The eight owned one-worker engines
used about 1.68 GiB working set in total. A follow-up sample at 04:59:31Z found
Overwatch using about 1.99 logical CPU equivalents and 9.61 GiB private working
set, total host CPU 65%, and 4,883 MiB available memory. Page input was 11/s and
page output zero in that instantaneous sample; this is not evidence of sustained
paging or a complete resource trace. The heavy external workload materially
changes the calibrated clock conditions. No user application was modified or
closed. Raw samples are `confirmation-qxray1-host-memory.json` and
`confirmation-qxray1-host-resource.json` (CIM working-set bytes versus peak-working-
set KiB retain their native units).

The observed game-process start was **04:34:50Z**. Look-1 `run.json` closed at
**04:35:40Z**, with assessment at 04:35:42Z; stage 2 began at 04:35:44Z. Thus even
the completed first look has a late overlap of about 50 seconds. Its original
320-game audit and `continue` assessment remain immutable, but its strength
interpretation is **provisional with uncalibrated resource overlap**, not clean
confirmation of the originally intended host conditions. No score-selected games
are removed or reclassified. The extent of actual contention during those last
50 seconds is unknown; process existence alone does not reconstruct CPU use.

The second-stage owned Fastchess PID, creation/command identity, parent and all
eight actor-marked Java children were verified and recorded before cancelling
only that tree. Its wrapper then failed closed and did not launch QXRAY4. Both
runner and engine descendants are gone; stage-2 process/runner/execution records
truthfully retain failure from forced resource cancellation, alongside the partial
raw output. **Interrupted stage 2 is excluded from confirmatory inference.** No
partial stage-2 score was inspected or used to decide interruption. This is a
resource interruption, neither statistical futility nor evidence of inferiority.
See `confirmation-resource-interruption.json` and unchanged execution records.

The owner explicitly answered **"Wait until I confirm I'm finished"** to the
host-availability question. This is now a blocking human action for further clock
testing. Do not infer availability from elapsed time or silently restart. Functional
validation may finish independently; no further strength or timing benchmark runs
are authorized to resume before that confirmation.

Resume only after explicit owner confirmation and a lightweight check of host
conditions. Preserve the interrupted QXRAY1 protocol, all attempted stages and its
original provisional assessment. Do not overwrite its output directory, splice
selected games, pool interrupted data, or silently resume under changed rules.
A replacement one-worker confirmation, if still justified by a fresh decision-value
assessment, needs fresh independent openings, frozen prospective rules and the next
unused family contrast index (**3 or later**; index 1 remains spent and index 2 is
reserved for the untouched QXRAY4 protocol). Its scope must disclose this resource
interruption rather than describing it as a favorable post-stop extension. The
existing QXRAY4 protocol has **not started** and remains frozen and unaltered.
The original source and model remain unchanged. Production stays SEE OFF.

This is an external host-availability pause within ACTIVE SR-024, not a DEFERRED,
REJECTED, ACCEPTED or IMPLEMENTED technical closeout. Contract/Frontier remain
P2-R027/P2-F031; no successor is selected. Owner acceptance and the final both-mirror
handback are not claimed complete and remain due at actual feature closeout.

Interruption accounting: stage 2 consumed 1,484.109s whole-attempt wall; retained
native handles record baseline/candidate lifetime CPU 2,930.734/2,928.672s across
eight engines, all forced exit code 1. These are resource costs of an excluded
attempt, not strength data (`confirmation-qxray1-interrupted-resources.json`).
All 66 QXRAY1 and 70 QXRAY4 bound file hashes remain unchanged after cancellation.
Both Phase-1 archive hashes still match. The local version helper reports idle,
with no active/unfinished operation or blocker and `verified_bump=false`; build
35 and the initial non-blocking initialization warning remain unchanged. The root
journal is still absent and none has been created.

The final v4 functional pass for this pause completed successfully: **808 tests,
797 passed, 11 existing disabled, zero failures/errors**, across 108 suites.
Command: `gradlew.bat :app:test -Pheadless --tests 'com.ohinteractive.seedv6.search.*'
--tests 'com.ohinteractive.seedv6.rules.*' --tests 'com.ohinteractive.seedv6.uci.*'
--tests 'com.ohinteractive.seedv6.core.See*' --console=plain`. The disabled tests
are QuiescenceDeltaEvidenceTest (5) and QuiescencePostMoveEvidenceTest (6).
Raw output, summary and XML are `broad-v4.txt`, `broad-v4-summary.json` and
`broad-v4-xml/`. These functional checks ran after tournament cancellation;
their 2m59s elapsed time is not clean benchmark evidence. No source changes were
needed. GUI, training and unrelated expensive suites were deliberately omitted.

**Human action blocking clock continuation:** explicitly confirm that the heavy
workload is finished and the host is available. This pause does not require a
production deployment, version repair, user cleanup, or acceptance of an unfinished
feature. All frozen and interrupted research artifacts are deliberately retained.

### Owner-confirmed resumption — 2026-10-14 local

The owner confirmed that the heavy workload had finished, then explicitly
confirmed that the separately observed SeedV6-NNUE desktop computation was idle.
The first resumption sample (`resume-host-1.json`, 08:57:59–08:58:14 UTC) had
measured that application at 2.4186 logical CPU equivalents. No clock test ran
under that condition and no user application was controlled. The second sample
(`resume-host-2.json`, 09:01:41–09:01:56 UTC) found no such computation among
accessible processes above .005 logical CPUs; its largest observed process used
.0997 logical CPUs. Free physical memory was 18.11 GiB and instantaneous paging
input/output were zero. This establishes a suitable sampled starting condition,
not a complete resource history. The read-only observer records its hash, PID
creation checks, inaccessible-process counts and sampling limits. The previous
blocking host-availability action is now satisfied.

This root began from clean `aa830804b3ec539331a39a3b00a1d9bed6c50baa`; first UTC
observation was 08:49:54. Root CODEXLOG_CURRENT.md remains absent. The pinned
version binding was reverified. Local version initialization made no reservation
or version write and was interrupted after its full-tree capture stalled; only
the verified owned Python invocation was stopped. Subsequent local helper status
was idle, active null, no unfinished operation or blocker, token null and
verified_bump false. This is a disclosed **non-blocking interrupted initialization
warning**, not a successful capture or terminal helper warning receipt. There is
no finish token or replacement bump. Build 35 is unchanged; no version repair is
part of this research.

The unchanged QXRAY mechanism still merits clean independent investigation: it
repairs the measured clearance witness, reduces fixed-depth work in both worker
domains, and had the leading exploratory strength signals. Conversely, the
provisional old first look was only +9.774 Elo, not confirmation of the exploratory
+71.568. The old protocol had prescribed continue before an objective external
resource interruption; it was not closed for statistical futility. A fresh
replacement therefore tests the unresolved question with new data and new error
spending, without pooling, selectively excluding games, or extending a stopped
statistical test. All original reserved openings remain excluded.

Prospective replacement design: unchanged frozen v4 QXRAY versus accepted
EFFORT1, Java 21 AP1, 10+0.1 and four simultaneous games; cumulative looks
**160/576/2048 pairs**, family contrast index **3**, alpha .00625 over three looks,
z=3.078088. The same normal-eligibility, 2.5-SE agreement, bounded sensitivity and
conditional-power futility rules apply. The development pair variance .1083039
and observed 14.1295 wall seconds/pair imply illustrative 80% normal power,
ignoring futility, at 30 Elo around 898 pairs/3.53h, 20 Elo 2,014/7.90h and 10 Elo
8,039/31.55h. Using provisional old-look variance .0866721 only as a sensitivity
gives 718/2.82h, 1,611/6.32h and 6,434/25.25h respectively. These are model-based
planning examples, not accepted gain floors or promised power. Futility reduces
power. The 2,048-pair maximum forecasts 8.04h; 576 approximates the geometric
middle. This investment can change the adoption decision at materially finer
precision than development while avoiding an unjustified immediate tiny-effect
campaign. Follow every frozen stop without outcome-driven extension.

Run this replacement first, then the existing untouched index-2 QXRAY4 protocol,
with no overlapping match or heavy diagnostic work. QXRAY4 remains independently
necessary; one-worker evidence cannot establish its benefit. Resource checks
remain read-only and low frequency, separate from statistical outcome inspection.
All original interrupted artifacts and assessments remain immutable.

Replacement protocol frozen as `confirmation-qxray1-replacement-protocol.json`,
SHA-256 `754b2ecda7b5f08fc1a99a3510870cbc486524326616be7583db55baaf5fa9a8`.
Its 2,048 fresh partition-1 positions were sampled from 5,410 eligible positions
after 11,973 distinct exclusions, using selection seed 202610142413. All bound
hashes, stage slices and pairwise disjointness of the three complete reserved
samples were independently rechecked before launch.
