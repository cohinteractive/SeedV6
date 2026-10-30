# TT memory workstream ? resume here

Status: **ACTIVE**, 30 October 2026. Objective [TT-R001](CONTRACT.md) is subordinate
to Search P4-R001. Current programme master pair: **P4-R001 / P4-F008**.
H1's W1/W4 development screens are promising but inconclusive. The first W1
confirmation failed operationally after 112 completed games; it is not a completed
strength comparison. No confirmed gain, acceptance, champion advancement or
production promotion has occurred.

**Selected work:** clean H1/H2 successive-request cost diagnostic, prospectively
frozen as [TT-H2-COST-V1](h2-cost-v1-protocol.json), now executing W1 then W4.
H2 is committed at b0be4fe1/build82 and its frozen runtime passed preflight; see
[H2](H2.md). No strength match is active. The prototype passed27 focused tests,
310 broader tests and78 launcher/isolation requests. The committed rebuild has
identical class bytes and four successful artifact requests; this is not a full
test rerun. Cost results will inform selection of a new paired strength screen.
Do not resume the failed H1 confirmation or execute its conditional champion block.
H1 remains frozen at a72cc496/build81; build79 remains the accepted champion.

## Identities and completed foundations

| Role | Source/build and frozen executable manifest |
| --- | --- |
| Immutable baseline AND accepted champion | `753367614b8aa7e32dee337fd76b542737141ac1` / build79; [runtime79](runtime79-manifest.json). |
| Accounting-corrected technical control | `0bfb4ea8422ef78443e309930166ad46df5cc454` / build80. Artifact built at `9fec1d2af379827d105d523c32162fd771905cb9`, whose runtime source is identical; [runtime80](runtime80-manifest.json). Not a champion. |
| Experimental H1 | `a72cc496fd39aeddefea4382044bd2bf2e9f6cbb` / build81, flags7602175; [runtimeh1-v1](runtimeh1-v1-manifest.json). Explicit research selection only. |
| Experimental H2 | `b0be4fe1ce5bfa8ac714108a3122b7615f25967d` / build82, flags15990783; [runtimeh2-v1](runtimeh2-v1-manifest.json). Explicit research selection only. |

CP64/checkpoint64/S6CNU002/eight heads/BT4_Q_LOGIT400_V1 and its full SHA are pinned
in TT-R001. Production policy stays flags3407871, margins100/200, extensionCap1,
ADAPTIVE16, OWNER W1 / ROOT_REUSE W2?16, existing BOUNDARY2. VERIFIED_NULL still
requires original18. Hash64 gives 2,097,152 direct-mapped slots / 50,331,648 bytes
of entry payload; overhead and process memory are separate. Both actors use the
same vector-256-long backend. All frozen runtime/model hashes were reverified
before freezing confirmation; the host snapshot is bound in its protocol.

Bootstrap verified clean main at 9fec1d2a and the supplied P4-R001/P4-F003 masters.
The accepted inventory, accounting correction and external research map remain
closed starting evidence. The framing survived challenge: global score suppression
alone was insufficient; hints and score retention are coupled; accounting80 cannot
be presumed competitively neutral; the old Phase3 research UCI launcher omitted
the current clock wrapper. Those comparison dependencies are now characterized
or corrected in the explicit research consumer. Do not repeat the inventory/map.

## Evidence and decisions

[H1 technical report](H1.md) and [comparison bridge](BRIDGE.md) hold details,
limitations, raw locations, artifact differences and consumer qualification.
All game rows below are exploratory, 32 opening pairs/color reversal at10+0.1,
Hash64, one active game, on the recorded Windows/Ryzen5500/Java21 host.

| Comparison | W/D/L for the first named side | Score | Paired normal95 |
| --- | ---: | ---: | ---: |
| build80 versus79, W1 |27/12/25|51.56%|41.99?61.14%|
| build80 versus79, W4 |15/24/25|42.19%|33.83?50.55%|
| H1 versus80, W1 |30/14/20|57.81%|48.63?67.00%|
| H1 versus80, W4 |25/23/16|57.03%|47.13?66.93%|

Each completed 64-game run passed strict PV/bestmove checks, independent PGN/
request accounting audit and immutable-input checks. Conservative Hoeffding
intervals are substantially wider; see per-run summaries. No competitive
79/80 equivalence is established. W4's adverse bridge makes transferring an H1
versus80 gain into a champion79 gain especially inappropriate.

Technical evidence: 297 core/UCI/selected research tests passed on the H1
implementation (prototype build80 metadata), followed by the additional reached
legal-null-verifier witness and build81 artifact preflight. Independent exact
controls were unchanged. W1?16 budget/refusal/cancel/drain/reuse and concurrent
collision safeguards passed. The 39-request launcher check matched all 11 W1
control depth/node outcomes across frozen80,81 ordinary UCI and81 research control;
W4 accounting/PVs passed without asserting scheduling equality. The initial
136-request 79/80 bridge found W4 counted-work differences: nodes are not equal-work
currency across that correction.

Nine-root instrumentation showed actual H1 score admissions/reuse and retained
legal guidance; it is not strength evidence. Bound-class occupancy was not
implemented in that first diagnostic, and this limitation remains recorded.
The clean 180-request cost diagnostic showed no material integrated regression
(geometric-mean H1/control wall ratios 0.987 W1,0.960 W4). Defer the proposed
combined-access optimization; test unchanged H1 instead of tuning prematurely.

Retain failures: `raw/bridge-diagnostics-v1` (W4 identity-announcement harness
failure after W1); `raw/bridge-w1-v1` (3+0.03 strict missing-score interruption
in game52 after 51 complete games; no complete strength score); `raw/h1-tests-v1`
(raw-eval compatibility regression, fixed without changing the oracle); the first
H1 observation console (PowerShell argument split before main). Successful retries
have distinct paths. V2 W1 tracing omitted engine=true and captured lifecycle only;
W4 and H1 traces include full UCI communications. Do not erase these limitations.
The zero-work fixture demonstrates truthful legal fallback, not the exact untraced
V1 failure cause. Frozen historical BOUNDARY2 studies remain unopened and unchanged.

## Failed independent confirmation and selected qualification

The prospectively frozen [attempt1 protocol](h1-confirm-w1-v1-protocol.json)
bound 128 partition1 opening pairs, exact runtimes and an adjusted analysis gate.
It stopped during game113 after 112 completed normal games. The full trace proves
that baseline80 returned a legal `g1h1` fallback with14 nodes,539316400ns and
TIME_LIMIT, with no completed depth/score. Fastchess's blanket strict missing-score
warning stopped the match. The cause of the low work during that elapsed time is
unestablished; no engine defect or clock-policy correction is inferred.

[Failure summary](h1-confirm-w1-v1-failure-summary.json) binds all raw files and
verifies every frozen input unchanged after failure. Evidence remains at
`raw/h1-confirm-w1-v1-control80`. The cleanup observer separately failed converting
an exit timestamp; its call order permits an exit-time race. All owned match PIDs
were absent in the post-failure OS inventory. Preserve the missing candidate CPU
lifetime observation rather than reconstructing it.

Attempt1's nominal alpha0.025 remains spent. Partial outcomes are not confirmation,
are not pooled into later samples, and do not open the conditional champion79 gate.
Do not modify its protocol, inputs, analysis or run. A later confirmation needs a
new prospective claim allocation and independent inputs; it is not selected yet.

[Runner qualification](runner-qualification-v1-summary.json) passed six tests
(including 18 invalid-evidence subcases), a replay audit of 13,724 retained trace
requests, and eight normal qualification games across W1/W4. Each live domain
exercised two real scoreless fallbacks, passed all game/accounting/input checks,
and explicitly drained both engine handles before successful lifetime snapshots.
The first test run caught duplicate-clock-field acceptance and an unclosed file;
failed logs/source copies remain in `raw/runner-qualification-v1`.

Default strict mode is unchanged. The explicitly selected audited-fallback mode
requires protocol declaration, monitors warning severities, stops/rejects other
warnings/errors, and validates every missing-score warning against its unique
real-history request: legal fallback, exactly one accounting line, clock termination,
no completed Search info and no engine failure. No score is fabricated. Full PV,
game/result, accounting and immutable-input gates remain. Live polling observes
logged warnings; it is not a claim of zero detection latency. A completed old
protocol is never retrospectively passed under these rules.

The compatibility protocol's initial-zero-work prediction was false: actual
Fastchess commands included the increment, starting at140ms rather than40ms.
The real fallbacks did nonzero work; the requested compatibility path was still
exercised. The frozen protocol and this deviation are retained. Qualification
scores carry no playing-strength inference. No engine or clock policy changed.

[Lifetime diagnostic](h1-lifetime-v1-summary.json), harness b483d06c, completed
104 instrumented H1 requests in each W1/W4 domain plus 104 clean persistent W1
checks. All used100k nodes and passed legal-PV/accounting/drain checks; all W1
results/nodes/PVs/diagnostics matched. Frozen engine/runtime stayed a72cc496/build81.
Raw: `raw/h1-lifetime-v1`, including the initial compile failure and corrected run.

Stale score hits were11.96% W1/11.10% W4; about98.9% were one request old. Per-request
unique stale nonroot counts summed63,295/59,620, including5,739/4,989 at depth>=3.
Stale same-key writes replaced deeper prior scores with shallower fresh scores
15,874/14,865 times. No hint-only demotions appeared. Most stale scores were depth1-2.
These are availability/replacement findings, not cutoff eligibility or strength;
the observer lacks requested depth/window/extension-credit context. The eight
actor streams came from the first four development games without result filtering.

Selected [H2 hypothesis/design](H2.md): allow at most one prior request of heuristic
score age, protect useful prior depth while refreshing hints independently, retain
all H1 eligibility/provenance/coherence safeguards and physical capacity/payload.
The explicit implementation has passed prototype qualification (27 focused/310
broader tests and78 launcher checks). Committed build82 and its distinct frozen
runtime passed preflight; clean cost evidence is selected before games. No strength
gain is claimed. Do not tune a grid or add a major subsystem.
W4 direct79/independent confirmation remains unresolved. Any later confirmation is
a new claim attempt; H1 attempt1 alpha0.025 stays spent and its partial observations
are excluded from the new independent sample. No champion advancement or promotion.

## Rebuild, resume and human actions

Ignored evidence must exist before reuse; a fresh checkout lacks it. Reverify
manifests and hardware, and never overwrite frozen installations. Build79 at its
exact detached commit and source80 at 9fec1d2a use `gradlew.bat :app-uci:installDist
--console=plain`; outputs are `app-uci/build/install/app-uci`. Existing logs:
`build/p4-tt/control79/build79-build.log` and
`build/validation/p4-tt-control80-build.log`. H1 at a72cc496 uses
`gradlew.bat installResearchWorkspace --console=plain`, output `build/research/runtime`,
log `build/validation/p4-tt-build81.log`. Use Java21+35 and checkout-local
`GRADLE_USER_HOME=<checkout>/build/migration/gradle-user-home`. Freeze new copies
with the objective tool and qualify new identities; rebuilds do not inherit old hashes.
The retained diagnostic prototype `raw/runtimeh1-diagnostic-v1` has its own manifest
and must not be confused with the causal80 control or committed81 candidate.

A running JSON status alone is not proof of a live process: compare the run's
process-cost PID/creation evidence with actual OS processes and commands. Preserve
interrupted PGNs/logs/manifests, resolve the cause and record a separately named
prospective retry; never interpret partial scores as a complete comparison or
reset confirmation error expenditure after viewing game evidence.

Git: all changes belong to this authorized objective; raw outputs remain ignored.
Application version is0.0.0/build82, verified and included in the H2 implementation
commit b0be4fe1. Research-only artifacts do not warrant another build increment;
verify the exact current root tuple and frozen manifests. Repository
AGENTS.md uses commit-based build rules with **no finalizer dependency**. Root
CODEXLOG_CURRENT.md was absent; do not create it. Inspect current Git/file presence
again on resume and preserve unrelated changes. No push/deployment/release is authorized.

Human action: owner refresh of the separate Phase4 Project Frontier mirror to
**P4-F008**, confirming **P4-R001 / P4-F008**. This is **non-blocking** for repository
research and blocks only mirror-dependent Project planning. Prior mirror refreshes
are not inferred complete. No owner action currently blocks the selected qualification.
