# BOUNDARY2 confirmation and conditional production adoption

Status: **ACTIVE — timing diagnosis before prospective confirmation**, 24 October 2026.
Subordinate focused record under `source/CHESS_SEARCH_CONTRACT.md` and
`source/CHESS_SEARCH_RESEARCH_FRONTIER.md`; not another governing master.

## Authority and bounded objective

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

## Dependency queue and exact restart point

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

Current restart: execute `game-diagnostic-v1-a01 full` using
`run_game_diagnostic_v1.py`, then join its events with `audit_game_events_v1.py`.
The required `game-smoke-v1-a01` event audit passed. Do not repeat a started name.
Check its summary, worker log, owned-process state and raw availability before
resuming; a failed/incomplete capture remains evidence. Challenge the timing
findings before any fresh strength protocol; no production mutation is admitted.
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
