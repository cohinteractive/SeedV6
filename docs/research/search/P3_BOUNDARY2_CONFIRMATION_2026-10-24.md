# BOUNDARY2 confirmation and conditional production adoption

Status: **PRODUCTION DEFAULT INTEGRATED; bounded technical qualification passed**, 24 October 2026.
Subordinate focused record under `source/CHESS_SEARCH_CONTRACT.md` and
`source/CHESS_SEARCH_RESEARCH_FRONTIER.md`; not another governing master.

## Production adoption - owner decision and implementation

The24October2026 owner decision supersedes the suspension and optional-only
recommendation below. Adopt exact BOUNDARY2 as the default for every normal
managed-clock evaluator/configuration and workers1-16 through shared production
ownership; retain factual fallback and explicit-limit semantics. The decision
expressly waives independent-strength and timing qualification as vetoes on this
bounded adoption, while preserving correctness/lifecycle invariants and all
failed-study identities/results. No further tournament, general timing diagnosis,
score opening, external installation or successor is authorized.

The owner accepts unconfirmed strength, the old baseline timing anomalies and
new candidate124.2323ms/runner125ms excess, unknown causes/frequencies/comparative
risk, and potentially adverse strength/clock consequences; actual-game adverse
behavior is grounds for prompt reconsideration. This is a production decision,
not retrospective research qualification or an Elo claim. Masters P3-R034/F034
record the authority. Prior mirror refresh is not presumed or needed as a veto
on this directly authorized repository work; revised mirrors still require owner
refresh before planning dependent on those external copies.

Start: clean HEAD a17136aa0da0e06beb741884a3bed68655e3bbff, build76, no root
CODEXLOG_CURRENT.md. `adoption-baseline-v1.json` records verified311 inputs,
exact runtime membership and unopened historical evidence hashes before edits.
Current source changes will be accounted against this baseline without rewriting
frozen manifests; original source remains in Git and frozen runtime files remain
unchanged. No finalizer is invoked.

### Implemented composition and supported scope

`ClockPolicy` in engine-core binds hard allocation and `Boundary2ClockPolicy`
completed-iteration stopping. The production implementation matches the unchanged
`Cp64SpendingPolicy(owner,false)` reference, including overflow-safe half ceilings,
depth6/stable3 guards, mate/no-move handling, clipped hard cap and resets. There is
no extra iteration, parameter tuning or change to SearchControl/SearchDriver stop
logic. The wrapper delegates traversal and ownership, and has private per-owner
state reset on request start/end, new game and close. Hard/node/external stops
continue to dominate soft completion.

| Normal composition | Production clock default and preserved configuration |
| --- | --- |
| Shared BaselineEngine CP64 | BOUNDARY2; flattened PVS, flags3407871, ADAPTIVE16, checkpoint64/eight heads, BT4_Q_LOGIT400_V1, requested Hash64 unchanged. |
| Shared HCE/BREP2-32 and unrelated conventional payload | BOUNDARY2; existing flags262143/ADAPTIVE16 and evaluator/score identities unchanged. The default is not restricted by CP64 payload identity. |
| GUI Play, independent white/black bindings | Shared normal competitive factory uses selected clocks; normal learned-model compatibility route also decorates its existing Search. Current depth/fixed-movetime UI remains non-clock-managed. Historical pinned controls stay explicit and unchanged. |
| Ordinary Arena/other BaselineEngine consumers | Receive the same selected owner. Existing depth/per-move contracts remain unchanged; no fabricated remaining clock or new game-clock UI. |
| Standard UCI/BotLi default | Shared CP64 composition; actual wtime/btime/increment/movestogo use BOUNDARY2 allocation. |
| Standard UCI --model Pair-2 / --nnue conventional | Same shared clock policy at their existing configuration boundary; retained recursive/selective or recursive-unpruned kernel and evaluator identity, rather than a silent Search migration. |
| Legacy UCI HCE/explicit checkpoint | BOUNDARY2 around the existing evaluator/ProductionSearch factory; retained model selection, score mapping, Hash64 and legacy quiet request reporting. Clock identity is announced by the handshake. |
| All normal worker settings | Explicit1-16 supported; existing Max remains min(available processors,16). No worker/cohort/TT algorithm change. |

Explicit `ConstructionUciMain`, pinned historical Search configurations,
`NnueCandidateUciMain`, independent references and research controls do not inherit
the production default. The four research spending/time launcher/economics call
sites now explicitly create their original unwrapped Construction composition,
preventing default leakage or double wrapping. Frozen reference policy, protocols,
311 bound files, both25-file runtime directories and historical/partial evidence
remain unchanged, verified by hashes and membership. No historical score is opened.

### Factual fallback and configuration behavior

- UCI/BotLi: `setoption name ClockPolicy value FACTUAL`; restore with
  `setoption name ClockPolicy value BOUNDARY2`. `uci` advertises the combo and
  reports the actual policy identity (`factual` or `boundary2-d6-v1`).
- Process startup, including GUI/Arena: JVM option `-Dseedv6.clockPolicy=FACTUAL`
  (for generated launchers it may be supplied through `JAVA_OPTS`). Absence selects
  BOUNDARY2. Invalid policy strings fail explicitly; no silent fallback.
- Core callers: `BaselineEngine.load(evaluator, ClockPolicy.FACTUAL)`, immutable
  `withClockPolicy(FACTUAL)`, or the explicit static create overload. Existing
  bindings snapshot the setting; switching one instance does not mutate another.

UCI changes allocate a replacement before retiring the old service, suppress old
publication, close/drain the old owner and preserve Threads/Hash/book selection.
The allocator and completed-iteration wrapper change together. Existing explicit
movetime overrides valid clock fields; pure depth/nodes/infinite do not acquire
clock semantics. Combined real clocks plus depth/node ceilings retain both limits.
No evaluator, TT layout/locking/replacement/generation/proof, history/repetition,
mate/PV or selective-verification algorithm was changed.

### Bounded qualification and retained failures

`adoption-integration-v1.json` reconciles latest suite receipts: **287 JUnit test
executions,0 failures/errors/skips**. This counts parameterized invocations, not
unique display names, and does not sum repeated runs as independent evidence.
Core/reference checks cover10000 selected-policy decision comparisons plus288
allocation edge combinations, explicit factual equivalence, reserve/increment/
overflow cases, incomplete versus completed publication, hard/node/external-stop
precedence, all3 competitive evaluators x workers1-16 under both policies,
representative1/4/16 real managed clocks, repeated requests/new games, cancellation,
active replacement/shutdown and private-worker/TT/history regressions. Existing
Max-bound coverage remains passing. GUI tests exercise real factories under both
policies for all3 competitive evaluators; headless Arena validates identities,
color ownership, bounded depth games and resume, without strength inference.

| Receipt | Actual result |
| --- | --- |
| adoption-focused-v1-a01 | New equivalence test constructed an invalid no-move result with nonzero move; failed fixture retained. Collector also rejected stale XML from an unrun project. No engine failure identified. |
| adoption-focused-v2-a01 | Corrected legal fixture and truthful stale/unrun accounting:45 tests pass;25 owned processes drain. |
| adoption-regression-v3-a01 | 280 executions;2 legacy handshake assertions expected the old option list. Other stages pass;71 owned processes drain. Original failed XML/receipt retained. |
| adoption-consumers-v4-a01 | Exact new option/identity assertions, GUI fallback and legacy profile coverage:78 tests pass;70 owned processes drain. No protocol assertion weakened. |
| adoption-uci-v1-a01 | 230 completed actual UCI requests in8.858s;5 profiles,12 owned processes drained. All explicit1-16 counts and representative1/4/16 clock/node/movetime checks; live policy replacement, stop and active quit; startup property fallback and subsequent explicit override. |

The first native checks use the locally built production classpath, not test
stubs or frozen research actors. The final build77 artifact verification follows
the qualifying application commit and will be appended separately. Unit/integration
logic was tested at build76 before the metadata increment. Source hashes in the
receipts bind the tested Java; no runtime code changed after the passing checks.
There is no new tournament, strength analysis, general timing campaign or claim
of a25ms timing bound. The only observed failures in this integration were test/
receipt expectations, transparently corrected; no new engine correctness defect
was found or accepted.

### Closeout limits and required follow-up

Local source default adoption is technically qualified under the owner's explicit
risk acceptance. Independent BOUNDARY2 strength remains unconfirmed; both studies
remain clock-unqualified and unopened. Historical baseline and candidate tails
still have unknown cause, relative frequency and comparative risk. More spending
can hurt clock use or strength; the owner accepts this and will reconsider adverse
actual-game behavior. Finite tests do not bound rare host/JVM stalls or establish
multiworker Elo. The50ms reserve is not a hard real-time scheduling guarantee.

Interactive GUI appearance, external BotLi/GUI installations, macOS, live games,
release/deployment and unrestricted full-system QA are not verified or performed.
No external files or installations are changed, and nothing is pushed. Production
build increments76 to77 once in the qualifying code commit; documentation/evidence
closeout does not bump again. No finalizer or absent CODEXLOG file is created.

Owner default-selection approval is already explicit; no second approval is
required. GPT/owner retains reconciliation/closeout authority. Both masters now
have headers `Internal revision: **P3-R034**` and
`Internal frontier revision: **P3-F034**`.
**PROJECT_SOURCE_REFRESH_REQUIRED: YES.** Mirror refresh/confirmation is a human
action blocking only planning dependent on those external copies, not this local
integration. External installation/release would be separately authorized work.
No successor CGLHW is started. The suspension below remains historical evidence.

## Owner-directed suspension and findings handoff — 24 October 2026

**Historical suspension authority (superseded for adoption above): STOP.** The owner suspended confirmation and conditional
production adoption for review of existing evidence and cost. No replacement
experiment, later block, lengthy diagnostic, production integration or activation
is authorized. The historical launch/queue below is retained as history and does
not authorize resumption. Phase 3 Contract P3-R033 and Frontier P3-F033 remain the
masters. SR-034 remains closed and unchanged; its results stay unopened.

### Safe stop, counts and evidence status

At **2026-10-24T06:21:19.0402278Z**, the verified sequence owner PID6924 was
terminated to prevent launch of later blocks. Its kill-on-close Windows Job
terminated worker PIDs28820/20188, Fastchess20108 and engine JVMs10996/20004.
Launcher27724 also exited; session92707 returned exit1. Independent executable-
scoped observation at06:21:42.2367971Z found zero experiment processes. No unrelated
process was terminated. The first broad observer predicate matched its own
PowerShell command; that observation and its explicit correction are preserved.

The design was **192 pairs /384 games**, not192 games. Preserved status:

| Portion | Completed games | Complete pairs | Disposition |
| --- | ---: | ---: | --- |
| Block1 | 64 | 32 | Fully qualified; scores unopened |
| Block2 | 45 | 22 | Interrupted and clock-unqualified; scores unopened |
| Total | 109 | 54 | One additional completed game has no finished reverse-color mate |

Block2 game46 (overall game110, second game of pair23) had started but did not
finish. It has a runner start record and no flushed PGN game. Its in-memory
observation is unavailable; it is not reconstructed, replayed or counted complete.
Blocks3-6 never started. All45 preserved block2 games have normal terminal records,
legal moves, exact frozen opening prefixes, and correct color pairing where both
games finished. Counts were reconciled against the runner without aggregating
results. No W/D/L, pair scores, Elo, confidence interval or interim strength look
was computed. The prescribed final analyzer was not invoked.

**New material timing failure found during this owner-requested operational
handoff:** block2 game39, ply28, BOUNDARY2, remaining7959ms/increment100ms,
base330ms/hard660ms, reported784.2323ms and runner charge785ms. Overruns are
**124.2323ms reported /125ms charged**, both above the frozen25ms ceiling.
Termination is TIME_LIMIT; pre-increment reserve7174ms. This completed-game
failure precedes the owner kill and is independent grounds for disqualification.
The full root/history/telemetry witness is preserved. It is not a flag loss or
proof of an allocation arithmetic defect; causal attribution remains unknown.

Block2's6096 retained requests pass the checked clock conservation, legal history
and completed/whole-node accounting identities. Baseline maxima are4.5824ms
reported/8ms charged; candidate maxima include the adverse event above. Minimum
reserves are1017/50ms. Exactly one retained request breaches the clock gate.
Runner failure-marker screening found no error/crash/illegal/timeout line, but
this is not complete correctness certification. Admission passed (other host
load.5880 average cores; memory endpoints passed), and block1's passing post-
delivery calibration was the fresh block2 prerequisite. Block2 has no final
native CPU/resource qualification or post-calibration; termination prevented
those receipts. No resource-pass inference or unobserved-game timing claim.

Block1's8821 requests passed both ceilings (max1.7845ms reported/7ms charged),
legal/accounting/resource/identity checks and owned drain. Native CPU/reported
elapsed ratios1.01171/1.01539 and post-calibration medians.99980/1.00132 passed.
Only its64 games/32 pairs qualify operationally; they do not independently qualify
the study or permit a partial strength conclusion.

`owner-suspension-v1.json` binds completed counts, original launch/admission,
process snapshots and partial raw evidence. `owner-pause-clock-tail-v1.json`
binds the adverse witness. The original running/attached launch, run and CPU
receipts are preserved verbatim; the separate suspension record supplies their
terminal operational disposition. All311 frozen inputs, exact runtime membership,
block1 qualification references and historical SR-034 evidence hashes were
reverified. Raw evidence remains local under `raw/`, excluded from Git. A fresh
checkout alone cannot reproduce this audit without the recorded local evidence.

The frozen stopping rule closes a started interrupted/failed attempt. **Do not
resume this study, finish block2, replace its game, run blocks3-6 or open partial
scores.** Reproducible frozen inputs are preserved, but this is not a restartable
qualified192-pair result. Any future experiment would require separate explicit
owner authorization and prospective rules; none is proposed as an automatic next
step. Historical SR-034 and this fresh confirmation's scores remain unopened and
statistically inadmissible under their frozen protocols. Operational timing and
integrity findings are legitimate; strength superiority or inferiority is not.

### Timing investigation: findings, hypotheses and cost

The completed diagnosis covered the two historical adverse histories,12 existing
diagnostic roots, repeated persistent JVM requests at9799/2933/500/150ms clocks,
and actual changing full-game histories. Instrumentation combined UCI reported
wall time, independent send-to-bestmove timing, native owner-thread CPU/cycles,
process CPU, JFR request/GC events, safepoint logs, monotonic/UTC comparison,
Fastchess integer clock conservation, legal/node accounting and owned-process
fences. Balanced instrumentation on/off runs and fixed-node signatures screened
measurement perturbation. The game bridge's baseline median overhead was4.19%;
it was therefore excluded from strength games. Detailed receipts below retain
all unsuccessful setup and validation attempts.

The17,227 measured diagnostic requests (17,003 clock-managed) did not reproduce
a25ms reported overrun; their maximum was4.4495ms. **That statement is limited to
the diagnostic captures. The later uninstrumented block2 now does reproduce a
large deadline-accounting violation.** Failure to reproduce during diagnosis was
never proof of impossibility; neither this event nor old baseline events establish
that BOUNDARY2 caused the stall or share one historical mechanism.

- Gross allocation/rounding errors and clock-conservation errors are unsupported
  by the inspected formulas and measured identities. Hard/node/external limits
  dominate cooperative soft completion in source and targeted tests. This weakens
  a systematic arithmetic or stop-precedence defect; it does not prove every path.
- Submission/owner scheduling, cooperative checkpoint spacing, unwind/drain,
  publication and runner transport have distinct timing boundaries. The pilot
  startup request's21ms actual over-hard versus3.645ms reported over-hard supported
  checking both boundaries. The new event appears in both, so runner transport
  alone cannot explain its engine-reported excess; no precise stall location is
  established.
- OS descheduling, GC/safepoints, JIT/persistent JVM state and lifecycle delay remain
  plausible. Measured diagnostic pauses were small, and UTC/monotonic differences
  were at most.0256ms in the persistent capture. This weakens clock-source drift
  and large recorded pauses as explanations of those captures only. Neither old
  adverse requests nor the uninstrumented new event has per-request scheduler/GC
  evidence sufficient for causal identification. Aggregate host CPU cannot exclude
  a brief stall; owner CPU deficit alone does not identify preemption.
- Exact runtime/payload/classpath membership and matching fixed-node signatures
  support actor identity and deterministic controls. Instrumentation perturbs
  execution and the finite host-specific dependent sample supplies no rare-tail
  probability bound or all-platform guarantee.

No engine allocation, evaluator, TT, cancellation or publication implementation
bug is established. Actual **harness/test defects** were found and transparently
repaired: decimal-versus-hex native thread-ID parsing; a postcapture helper import;
JAVA_TOOL_OPTIONS contaminating expected UCI stderr; missing explicit child stdout
capture; an immediate descendant-drain assertion needing bounded observation;
and a GUI replacement-factory fixture that failed to disable book. They are not
proven explanations of historical or current game timing tails. Failed originals
and separate repairs remain preserved. No runtime remediation was made.

Further broad replay or another tournament has poor expected value for the
present owner decision: substantial exposure did not localize the cause, and the
new adverse event lacks causal instrumentation. A cause-finding project would
need a separately justified targeted plan, not more of the same sample. Stop now;
no speculative JVM/priority/engine change and no additional research requirement
is imposed merely because the original CGLHW contemplated it.

### Exact study and source identity

Frozen protocol `P3-BOUNDARY2-FRESH-CONFIRMATION-v1`, source preparation HEAD
`27fc0533dd3f546b5628db7697f4a09c112a7327`, freeze commit `6eaf435`; binding SHA256
`06aa683d38cb40fb22c2d674bfb45d1fd6cf833f388eb067ad9cdb0d9efb13d2`.
Six32-pair blocks; seed2026102447/partition1;192 unique held-out roots from8646
eligible after30043 exclusions; eight-full-move histories and reversed colors.
Block2 Fastchess seed2026102449. The study's311-file binding is the complete
executable/source/input identity, not the current documentation commit.

| Frozen actor artifact | SHA256 |
| --- | --- |
| Native accepted build74 core | `facac0db65c7f97a120083174ea4de20ef210daf3252a79a3fd5eb2cac99e6f7` |
| Native accepted build74 UCI | `1ba9d9bcf44fcf385c6495e909fc3c24bb2dea4d1a10ed4f0d48b75319f67a19` |
| Candidate build76 core (source614511ce9a75) | `54cb27641a4c4a7c0fcc61ec17da772011bee3a09dec0e691e0b038c5b357e9b` |
| Candidate build76 UCI | `ce5a89a03724022c1acd106e9229eeb7bcef31e1d1dea92f4bccbd58278708d9` |
| Matched research JAR | `3f769be1b3f0b41696c3f70b68cbf8b3021b77610c38ad16056c855f619bb6c3` |

Both25-file runtime directories remain under SR-034's `raw/`. CP64 network SHA256
`8431626c2fca81e0a1e9ccd5e45ddd8829e6e3282c756c21fad75b06d075bb2b`, payload
`5da0253642ab33b8e446b5b1c5764750be008cd3dbacc7cd1992a95bf4a881f6`;
S6CNU002/eight heads, BT4_Q_LOGIT400_V1, flags3407871, ADAPTIVE16, Hash64.
Java21+35-2513 at `C:/java/jdk-21`, bound VM/modules/tools; one worker, one game,
BelowNormal, Xms256m/Xmx512m, ActiveProcessorCount4;200k-node actor startup warmup.
10+0.1, timemargin0, book/tablebaseOFF, no score adjudication, no JFR bridge.
The unchanged BOUNDARY2 algorithm is described below; no alternate finalist,
parameter retuning or clock-tolerance change occurred.

### Adoption readiness and recommendation

**Recommend available but non-default production configuration, only if the owner
authorizes that separate implementation after this review.** This is an engineering
availability recommendation, not a positive strength-adoption recommendation or a
claim that the current candidate meets the frozen timing qualification. The new
125ms tail materially strengthens the case against default activation on current
evidence. No implementation or activation is performed in this suspension.

1. **Available, default unchanged:** technically feasible and reversible through
   shared BaselineEngine. Existing core clock-completion and UCI allocation seams
   exist, but the policy class and launcher currently reside in research. A
   production implementation must have no research dependency and must pair the
   hard allocator with its completed-iteration policy under one explicit setting.
   Offering an opt-in requires no claim of confirmed Elo and no further tournament
   solely to establish that availability. Enabling it would carry explicitly
   acknowledged unresolved timing/strength uncertainty.
2. **Normal production default:** not authorized or supported by a qualified
   independent BOUNDARY2 result. Choosing default adoption without one requires an
   explicit informed owner revision/exception to the existing independent-strength
   requirement, acknowledgment of both the new timing failure and unknown cause,
   and post-evidence acceptance of bounded integration validation. This record
   does not waive any requirement or deem the implementation accepted.
3. **Independently confirmed improvement:** not established. Selected development
   evidence is not independent confirmation. Both frozen confirmations are
   unqualified and unopened. Neither optional availability nor an informed default
   decision would establish an independently confirmed strength gain.

Minimum work for the recommended optional path, under a later authorization:

- Put the exact extension-off policy in the shared production composition, retain
  the frozen research reference, add explicit factual/BOUNDARY2 configuration and
  truthful identity reporting, and keep factual behavior the default. Match the
  exact CP64 payload; preserve HCE/BREP2/unrelated payload behavior and all existing
  selectivity, aspiration, evaluator and TT policies. Reset mutable policy history
  per request/new game/owner replacement; never share it across workers/engines.
- Verify policy equivalence and allocator rounding/saturation/reserve boundaries,
  including0/50/51ms, increments/moves-to-go, ordinary/mate/no-move completions,
  interrupted iterations and hard/node/external-stop precedence. Couple allocator
  and soft policy so one cannot be enabled without the other.
- Standard UCI/BotLi needs the shared setting wired through both service creation
  and clock allocation, with option/identity and replacement/shutdown checks.
  GUI Play and ordinary Arena currently submit depth/per-move limits, not remaining
  game clocks. Verify shared routing and unchanged non-clock behavior; BOUNDARY2
  cannot alter their current clock behavior merely by wrapping the engine.
  Adding actual game-clock GUI/Arena controls would be a separate product decision.
- Check explicit workers1-16 functionally and representative1/4/16 timing,
  cancellation, repeated requests, reconfiguration and complete drain. Preserve
  Max<=min(available processors,16), private mutable worker state and TT
  identity/locking/generation/proof/history invariants. Existing CP64 worker tests
  are useful prerequisites, not BOUNDARY2 multiworker qualification or Elo.

The genuine risks are mispaired allocator/policy configuration, policy-state
leakage between requests, evaluator scope mistakes, altered explicit-limit
semantics, multiworker cancellation/drain latency and the **observed unexplained
125ms deadline excess**. The algorithm makes no direct evaluator/TT change, but
integration still must preserve their identities and ownership contracts. The50ms
reserve and cooperative deadlines are not a guarantee against host/JVM pauses.
A small bounded functional/integration verification is necessary before shipping
an optional feature; another lengthy tournament is not. No claim of satisfying
the25ms research gate or any strict latency guarantee follows from those tests.

### Validation, repository state and owner actions

Earlier work passed265 targeted/core/research/UCI/headless integration tests with
zero failures/errors/skips after the test-only book fixture correction. No runtime
source changed. Suspension validation consists of process exit/drain checks,
outcome-blind completed-record accounting and clocks, frozen/historical hash and
membership checks, and document/Git consistency checks. No new engines, games or
lengthy tests were launched for the suspension. Full-suite, interactive GUI,
external BotLi installation, BOUNDARY2 multiworker timing/strength and release QA
are not claimed. Application build remains76; finalizer use remains prohibited.

The suspension commit changes only this record, the two masters, compact pause/
adverse/evidence records and the existing block2 admission/launch records.
Earlier commits in this work unit contain diagnostic tools/evidence and the
single integration-test fixture repair; they do not activate production BOUNDARY2.
There is no root CODEXLOG_CURRENT.md, so no journal file is created. No push,
installation, deployment or release is performed.

**PROJECT_SOURCE_REFRESH_REQUIRED: YES.** Exact master headers are
`Internal revision: **P3-R033**` and `Internal frontier revision: **P3-F033**`.
Owner/GPT review and a new explicit scope decision block any resumed experiment
or production integration/default change. Owner refresh/confirmation of both
Project Source mirrors blocks planning dependent on those external copies.
Neither action blocks completion of this safe suspension itself. Return control
to the owner; no autonomous successor.

## Historical launch authority and bounded objective

The owner accepts SR-034's inconclusive/deferred technical closeout and confirms
P3-R030/P3-F030 mirror synchronization. This separate launch supersedes historical
no-successor wording only for diagnosis, fresh BOUNDARY2 confirmation and conditional
local adoption. Closed SR-034 protocols, results and evidence remain unchanged.
Its unqualified confirmation scores must remain unopened and unused, including
for diagnosis, protocol design and selection. No alternate finalist or retuning.

Retain production CP64/eight heads, BT4_Q_LOGIT400_V1, flattened flags3407871,
ADAPTIVE16, requested Hash64 and factual clock allocation. Preserve all binding
TT, history, proof, completion, cancellation and worker-ownership guarantees.
The Contract's explicit post-evidence GPT/owner acceptance is still required
before production promotion. Prepare/validate promotion-ready integration if
evidence qualifies; conditional launch is not acceptance of unseen results.
No external installation, live bot change, push, deployment or release.

## Bootstrap and challenge

Observed starting HEAD `64b0a80`, clean worktree, application build76 and masters
P3-R030/P3-F030. Root AGENTS.md is the only discovered instruction file.
CODEXLOG_CURRENT.md is absent; none is created. Repository owner instructions
prohibit finalizer use and increment build only with qualifying application commits.

BOUNDARY2's exact implementation is `Cp64SpendingPolicy(owner, false)`:
hard=min(2*base, remaining-50ms), nominal=ceil(hard/2), completed ordinary depth>=6,
stable3 may stop at ceil(nominal/2), otherwise first completed boundary>=nominal.
The nominal target is half the **clipped hard cap**, not always the original base.
No change-trigger extra iteration. The accepted native baseline must retain both
its exact build74 core and UCI JARs; source labels/build numbers alone are insufficient.

Static inspection finds monotonic cooperative checks at traversal, iteration and
allocation boundaries; hard limits dominate soft completion. UCI Publication starts
before lifecycle submission; its reported elapsed includes owner scheduling,
execution/unwind/drain and callback/report preparation. Fastchess starts its timer
after sending `go` and setting up its read, ending on `bestmove` receipt; response
transport is included but the preceding command send/setup is not. The diagnostic
send-to-bestmove interval starts earlier and is not an exact Fastchess charge.
The reported UCI elapsed is not solely traversal time.
TimeManager's 50ms reserve, 10ms overhead, default30 moves and increment rounding
remain unchanged. These are source findings, not a diagnosis of historical stalls.

Challenges to the first approach: isolated adverse-root replay cannot reconstruct
historical JVM/TT/heap/scheduler state; aggregate process CPU can hide a short stall;
CPU deficit alone cannot distinguish preemption from GC/waits; GC association is
not necessarily causation; logging can perturb timing; passing a small sample does
not certify all future requests. Neither actor's favorable timing excuses the other.
No threshold relaxation or speculative engine patch is justified at bootstrap.

## Historical dependency queue — suspended, not an execution instruction

1. Verify frozen executable/payload/tool bindings and local evidence availability
   without reading historical game outcomes. Preserve a compact verification receipt.
2. Establish bounded outcome-free UCI diagnostics on unchanged actors: complete
   request wall/native CPU, JVM GC/safepoint events and lifecycle fences. Use known
   adverse histories plus existing development inputs and low remaining clocks.
   Compare instrumentation on/off with balanced order and deterministic node controls.
   Add discriminating instrumentation only if these measurements cannot resolve
   the relevant mechanism. Keep serial W1/BelowNormal and owned process cleanup.
3. Diagnose observed tails and qualify a fair prospective method. Freeze no new
   strength sample until this gate is supported. Historical 25ms rule stays frozen;
   10+0.1 confirmation standard remains binding. Preserve all failed attempts.
4. Freeze fresh independent openings, exact actors/JVM/resources, paired inference,
   sample rationale, looks/stopping and qualification gates before new score evidence.
5. Execute only admitted confirmation, opening analysis only after its prerequisites.
6. If supported, qualify consumer paths and workers1-16 proportionately; prepare
   integration and hand back for required acceptance. Otherwise retain production
   factual clocks and close/defer with evidence and precise limits.

Pre-suspension restart instruction (superseded above): the new192-pair study is frozen; execute the six
`run_fresh_block_v1.py` blocks in order, only while prior blocks
qualify. The actual-game diagnostic, current265 tests, new56-request native
calibration, mathematical self-tests and new complete-clock audit validation all
pass. Block1 qualifies; continue blocks2-6 only in order with the same gates. Check binding/launch/summary state before any
resume; never replay a started name. Preserve every failed endpoint. Production
remains unchanged; the final analyzer is gated behind all six qualified blocks.

Freeze: `confirmation-v1-binding.json` binds311 inputs at source HEAD27fc053,
including exact25-file runtime inventories, JDK VM/modules/tools, source book,
protocol/analysis/runner/calibration bytes and validation receipts. Fresh selection
uses seed2026102447/partition1:192 unique positions sampled from8,646 eligible,
after30,043 distinct prior/diagnostic positions are excluded. The six consecutive
32-pair input partitions are immutable. Historical selection receipts are exclusion
inputs; historical unqualified game scores remain unopened. Masters P3-R032/F032
record progression; their Project Source mirrors have not been refreshed by Codex.

First fresh block:64 games complete normally with both clock gates passing
(maximum reported overrun1.7845ms; maximum runner-charge overrun7ms). Native
CPU/reported elapsed ratios1.01171/1.01539 and post-delivery owner medians
.99980/1.00132 pass their fixed criteria; values slightly over1 reflect native
accounting granularity and boundary differences. Integrity, resource and owned-
drain checks pass. Scores remain unopened; this is qualification, not a strength
look. Continue the remaining fixed blocks without reselection or interim inference.
Evidence/scripts/compact manifests: `p3-boundary2-confirmation-2026-10-24/`.
New bulk evidence: its ignored `raw/` directory. Check availability on every resume.
The actual game-path diagnostic follows the completed persistent capture below.

## Validation and limitations

Historical validation is reference evidence, not validation of new work. Historical
clock-tail cause remains unknown. No fresh strength inference, multiworker strength,
application adoption, owner acceptance of a new result or deployment is claimed.

## First timing capture

`bootstrap-input-audit-v1.json` verifies265 frozen inputs and34 historical evidence
files by hash, including all four blocks' raw availability. No historical outcome
was parsed. Exact native build74 core SHA256 is
`facac0db65c7f97a120083174ea4de20ef210daf3252a79a3fd5eb2cac99e6f7`;
candidate build76 core is
`54cb27641a4c4a7c0fcc61ec17da772011bee3a09dec0e691e0b038c5b357e9b`.
Their UCI, research, payload and launcher bytes remain bound in the receipt chain.

`diagnostic-v1-a01-summary.json` and `-analysis.json`:784 actual UCI requests pass,
112 fixed100k-node signatures agree exactly across actors/instrumentation,672
clock requests pass legal/PV/completed-depth/accounting/fence checks. Every owned
descendant drains. Runtime114.716s, owned CPU166.828s, host/memory screen passes.
Maximum request-over-hard1.712ms; maximum external send-to-bestmove over-hard7.161ms.
No25ms event is reproduced. Paired median instrument-on/off elapsed ratios are
.97419 baseline/.98311 BOUNDARY2; both pass the frozen descriptive5% screen.
Root-level ratios vary, so this is not a statistical equivalence guarantee.
Maximum recorded safepoints are4.384-5.222ms across instrumented JVMs, including
startup/shutdown. Short-process evidence cannot diagnose a rare persistent tail.

Selected follow-up: `persistent-diagnostic-v1.json`,64 rounds x14 roots x4 clocks
per actor (3,584 each), alternating only one active request across two persistent
JVMs. Matched JFR/logging and native named-worker CPU/cycle counters distinguish
request cost from JVM aggregate cost; retain all events and failures. No engine,
clock constants, accepted comparator or statistical criterion changes.

Primary technical references consulted24October2026:
[JDK21 JFR configurations](https://docs.oracle.com/en/java/javase/21/jfapi/flight-recorder-configurations.html),
[JDK21 unified logging](https://docs.oracle.com/en/java/javase/21/docs/specs/man/java.html),
[Windows GetProcessTimes](https://learn.microsoft.com/en-us/windows/win32/api/processthreadsapi/nf-processthreadsapi-getprocesstimes).
JFR's documented typical low overhead is not substituted for the measured screen.
Process CPU sums JVM threads; neither a CPU deficit nor generic literature proves
historical preemption. Local source verifies Fastchess elapsed-millisecond charging
before increment, including the initial10100ms clock. No source or rounding defect
has been established. Python syntax and whitespace checks pass; no new application
code, build increment or application integration test has yet been needed.

Persistent attempt `persistent-v1-a01` failed before measured requests: JDK21
prints decimal native thread IDs while the v1 parser expected hexadecimal.
Ten baseline100k-node warmups completed; the setup failure drained all descendants
(3.853s,8.5 owned CPU-s). Preserve its binding, thread dump, JFR/logs and receipt.
`run_persistent_v2.py` accepts decimal or hexadecimal IDs and binds v1 as well;
only parsing/provenance changes, not the frozen sample, clocks or runtime.

The repaired `persistent-v1-a02` completed all7,168 measured requests after both
actual identities,20 deterministic warmups and native worker handles were verified.
All legality/PV/accounting/fence, resource and process-drain checks pass. Wall
1,337.244s, owned CPU1,464.234s. Maximum overruns: baseline4.4495ms,
BOUNDARY2 3.2466ms. Baseline owner CPU625.672s versus reported631.388s;
candidate679.563s versus685.362s. Maximum recorded safepoints5.602/4.868ms;
no request exceeds5ms. UTC/monotonic interval disagreement is at most.0256ms.
This negative reproduction does not establish the historical cause or cover
changing full-game histories and every persistent JVM condition.

## Actual game-path timing diagnosis

`Boundary2TimingMain.java` is an external diagnostic launcher delegating to the
unchanged frozen UCI entry. It records JFR request events with original reported
elapsed/nodes/termination, input position/clocks, delivered move, complete boundary
time and owner/process CPU. It forwards original output before committing events.
It is not application code or a production dependency. No allocation constant,
reserve, algorithm or baseline identity is changed.

`bridge-validation-v1-a01`:784 requests pass,112 deterministic signatures match,
672 clock requests pass, and392 custom events match independent client records
exactly. Maximum overrun1.942ms. Median instrument-on/off ratios1.04187 baseline
and.99831 candidate pass the prospective descriptive5% screen. This asymmetric
cost and the screen's limited precision preclude using the pilot for strength.
All process/resource checks pass. JDK21 absolute Windows log quoting and `%p`
filename expansion are separately verified in `jvm-path-check-v1.json`.

`game-diagnostic-v1.json` freezes64 actual serial Fastchess games on the existing
32 development openings, matched JFR/GC capture, W1/Hash64/10+0.1/BelowNormal,
exact actor/runtime membership and strengthened JVM byte binding. No scores are
opened or used. The first-opening two-game smoke is separate from the full sample.
`game-smoke-v1-a01` passes240 request/event/clock-conservation joins, with maximum
overrun2.238ms baseline/.474ms candidate and minimum pre-increment reserves
2,030/892ms. All two games terminate normally and all descendants drain. Full
diagnosis remains necessary: passing a smoke is not rare-tail qualification.

Every25ms exceedance remains an adverse witness. JFR pause/deadline overlap is
approximate (2ms matching tolerance), with no direct hard-checkpoint timestamp;
native CPU granularity and incomplete OS scheduling observability remain limits.
The old unqualified scores remain unopened; none of these diagnostics supplies
independent promotion-strength evidence.

Primary platform constraints checked during the game capture:
[Windows scheduling priorities](https://learn.microsoft.com/en-us/windows/win32/procthread/scheduling-priorities)
and [CPU analysis](https://learn.microsoft.com/en-us/windows-hardware/test/wpt/cpu-analysis)
describe ready/waiting/running states, priority-based preemption and interrupt/DPC
costs. [JDK21 G1](https://docs.oracle.com/en/java/javase/21/gctuning/garbage-first-g1-garbage-collector1.html)
explicitly treats pause targets probabilistically rather than as real-time bounds.
These establish plausible mechanisms, not their occurrence in either old adverse
request. No JVM, process-priority, system setting or Search remediation follows
from documentation alone. Broad host utilization cannot exclude short stalls;
owner CPU deficit cannot, without scheduling events, identify a ready-thread delay.

After the game capture and its event audit finish, `run_validation_v1.py` runs
fresh selected core/research/UCI tests and headless integrated consumer tests,
serially with four-processor JVM ergonomics and owned process cleanup. It includes
SpendingPolicyTest, independent Search/TT/history checks, all-worker existing CP64
functional coverage, clock/lifecycle tests and real UCI subprocess tests. It records
fresh JUnit XML, exact command, native CPU and drain; it does not claim GUI visual
QA, BOUNDARY2 multiworker timing qualification, or strength at workers2-16.

### Completed game capture and challenged readiness decision

All64 `game-diagnostic-v1-a01` games end normally;8,251 complete requests join
exactly to JFR and runner clock conservation. The wrapper failed afterward on a
missing historical helper import (`run_trajectory_v1`), before its clock audit.
Original failed receipt/raw bytes are preserved. `complete_game_diagnostic_v1.py`
separately revalidates bindings, directory membership and unchanged raw evidence,
then uses the original arithmetic on this new capture. Its completion receipt
passes; `audit_game_events_v2.py` changes only the receipt/derived-output paths
from v1 and also passes. No games are replayed and no old score is opened.

Baseline/BOUNDARY2 request counts4,125/4,126; maximum overruns3.9706/.6875ms;
minimum pre-increment reserves1,008/50ms. Owner CPU875.922/922.063s versus
reported884.230/929.948s; maximum JVM pauses4.467/3.219ms. Owned wall1,828.993s,
CPU1,997.922s; all descendants drain. Other host load averages.662 logical cores;
memory endpoints pass. One startup baseline request is charged422ms against
hard401ms, versus reported404.645ms; no overlapping recorded pause establishes
its cause. This illustrates the different accounting boundaries, not a defect.

Combined short, persistent, bridge and game diagnostics contain17,227 measured
requests (17,003 clock-managed), without reproducing a25ms reported overrun.
This dependent, host-specific exposure supplies no universal rare-event bound.
No calculation, hard-stop, lifecycle or publication correctness defect is established.
Historical cause remains unknown: there is insufficient old per-request CPU/GC/
scheduling evidence to identify it retrospectively. No speculative remediation.

Decision: a new independently qualified attempt is scientifically admissible after
current validation/calibration, while historical timing uncertainty remains explicit.
It must retain the25ms reported-over-hard ceiling for both actors and add the same
25ms gate on actual runner charge-over-hard, with nonnegative pre-increment reserve.
Matched exact-actor native CPU calibration and per-block resource gates accompany
uninstrumented strength games. Any started-block qualification failure closes the
attempt with scores unopened; no replay, relaxation or causal forgiveness. This
is a falsifiable prospective gate, not a claim that the old problem is fixed.

The planned192 pairs arise from a worst bounded-pair variance.25 normal95% design
target of approximately+/-50Elo precision near equality:189 pairs rounded to six
complete32-pair blocks. Approximate80% power is around71Elo; neither is a minimum
required gain or guaranteed power under correlated openings. One final look and
Hoeffding sensitivity; no interim extension or transfer of old outcomes. Estimated
serial execution is about3h15 with qualification, at low workstation occupancy.

### Validation failures and repair

`validation-v1-a01` retained265 fresh JUnit cases,17 failures, before strict owned
drain bookkeeping failed. Sixteen UCI cases rejected stderr introduced by this
harness's `JAVA_TOOL_OPTIONS`; it is removed rather than weakening protocol checks.
The GUI replacement test had a separate stale fixture: only its initial factory
disabled book. The evaluator-aware replacement factory legitimately returned BOOK
at workers2, leaving no completed Search depth. An explicit completion assertion
reproduced this in `validation-gui-diagnosis-v2-a01` (one failed test), whose XML,
log and finding are preserved. The test now delegates to the real evaluator-aware
factory, disables book on that path too, and requires COMPLETED before checking
depth. No runtime behavior, oracle, Search configuration or default is changed.

v1 also lacked explicit child stdout redirection. v2 captures output but the
generic match owner's immediate post-exit accounting check still rejected a
transient descendant count. v3 uses the same suspended-before-assignment owned
Job and permits at most10s for the exact owned tree to reach zero after wrapper
exit, recording every drain count. It does not change the frozen tournament tool.
The full current rerun must pass before claiming integrated validation. All
validation attempts remain separate; application build stays76 (test-only repair).

`validation-v3-a01` passes265 tests with0 failures/errors/skips in157.751s,
263.813 owned CPU-s. Its61 owned processes drain; the final descendant takes about
4.2s after wrapper exit, within the explicit10s grace. The separate provenance
receipt binds the tested fixture bytes observed after this rerun. Interactive GUI,
external BotLi installation, release and BOUNDARY2 multiworker strength remain unrun.

`delivery-qualification-v1-a01`:56 uninstrumented400k-node requests pass exact
actor/root/repeat signatures, legality/PV/accounting and drainage. Median owner
CPU delivery is.99278 baseline/.99858 candidate, fifth percentiles.95784/.91147,
median spread.00579; resource/availability gates pass. Wall36.136s, owned CPU48.391s.
`confirmation-v1-math-validation.json` checks the new192-pair arithmetic independently
on synthetic inputs. Applying the new clock audit to the completed diagnostic
reproduces8,251 checks and maximum actual runner-charge over hard21ms; both new
25ms ceilings pass. These are admission evidence, not strength observations.

## Consumer scope discovered during diagnosis

Static consumer review: GUI `GameController.SearchSettings` exposes DEPTH and
MOVETIME, constructing non-clock-managed limits. Ordinary Arena's
`ValidationArena` uses depth or per-move `ValidationControl.beginSearch(millis)`.
Both route competitive evaluation through `BaselineEngine`; neither currently
supplies remaining game clocks/increments. Standard UCI does supply those via
`GoCommandParser`, with the existing configuration allocation seam.

Any later shared integration must preserve explicit depth/node/movetime/infinite
semantics. Making BOUNDARY2 available through the shared owner is distinct from
introducing new whole-game-clock UI/Arena controls, which this investigation has
not implemented or presumed necessary. Report the actual activation domain;
do not claim changed GUI/Arena clock behavior from an inert non-clock wrapper.

Prospective application qualification, only if strength/timing gates justify it:

| Domain | Required evidence before a production-scope claim |
| --- | --- |
| Exact configuration | Frozen policy equivalence; CP64 identity/flags/aspiration/Hash; explicit factual control; HCE/BREP2/unrelated payload isolation. |
| UCI clocks | Remaining/increment/moves-to-go, clipped reserve, 0/50/51ms and late clocks, truthful completed iterations, hard/node/external-stop precedence. |
| Shared consumers | Real factory routing for ordinary UCI, GUI Play and Arena; preserve non-clock semantics; no production-to-research dependency. |
| Workers1-16 | Every explicit count functional, representative timing/cancellation/repeated-request/shutdown/drain; Max processor/16 bound. Functional checks are not multiworker Elo. |
| Correctness | Independent references, legal/PV/history/repetition/mates, nominal/selective proof safeguards, TT identity/locking/generation and private mutable state. |
| Acceptance | Evidence reconciliation and explicit GPT/owner acceptance before default activation; local integration distinct from external installation/release. |

Additional runtime-directory inventory during persistent capture:25 files in each
frozen production/candidate runtime, exactly matching the original binding's file
set, with no missing or unbound files. Existing hash verification alone would not
detect added classpath files; future freeze/revalidation must check both contents
and membership. The present captures use the verified exact sets. JDK21 reports
21+35-2513; strengthen the new confirmation's JVM identity with actual VM/modules/
configuration hashes, not solely the historical java.exe digest.
