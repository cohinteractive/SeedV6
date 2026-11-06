# CLV-R001 findings and decisions

7 November 2026. **Technical execution complete; independent acceptance failed;
governing strength objective UNMET.** The initial investigation and successive
decisions below retain their original time scope. Final evidence and disposition
follow at the end; GPT reconciliation and owner disposition remain pending.

## Bootstrap challenge

- Verified current P4-R033/P4-F051 masters and accepted IC closure `2e45f703`.
  This explicit new owner launch supersedes the prior no-successor status only for
  CLV; closed research and frozen independent controls remain closed.
- Starting HEAD `b8ac447e` is clean/build98. `git diff 44fc2bf2 HEAD -- engine-core`
  is inspected by path; Search differences are BaselineEngine/EngineFormulation
  selection/identity, with ExactSearch, ParallelSearch and TTable unchanged.
  Exact build96 core/native/package/config manifests, Java21 and CP64 hashes match
  the historical pins. A newer executable will not substitute for exact96.
- MW-C4 production already stores selective estimates and completed hints. Ordinary
  proof reuse remains distinct; extension-enabled production suppresses ordinary
  proofs. A claim that production is hints-only would be false.
- `TTable.saveHeuristic` can retain a deeper completed move while replacing its
  score, so score depth/bound and move identity are not necessarily one observation.
  That is valid existing hint semantics, not automatic move-specific verification
  authority. Node-level evidence and move-specific evidence need different guards.
- `ExactSearch.flatNegamax` immediately consumes eligible current-generation
  ordinary-domain heuristic scores only at interior null windows with sufficient
  depth and a legal move. Shallower/noncutting information is currently ordering
  guidance; existing score fields may be sufficient for bounded node-level relief.
- LMR reduces late quiet nonchecking moves, then verifies reduced fail-highs at
  nominal depth. Reduced fail-lows are threshold predictions. History/improving/
  within-node refutation relief already interact; do not describe a duplicate as new.
- Singular verification would require explicit excluded-move semantics, preserved
  scratch/history/learning/TT and terminal handling; the current null/tactical
  isolated probes do not automatically provide that contract. Do not start with
  a large TT or singular-extension implementation before an economical dependency
  investigation.
- Fixed-node diagnostics can establish consumption and decisions, never Elo.
  Confirmation needs fresh held-out pairs and a frozen one-attempt protocol.
  No idle window exists; preparation/fixed work is authorized, timed execution is not.

Initial challenge finds no blocking identity/authority contradiction. Keep one
focused STATE queue, one budget and compact experiment records; no extra FRONTIER.

## Pinned implementation and development evidence

Fresh upstream downloads and twelve path-history entries per engine are hashed in
`external-manifest.json`, under `raw/external/`. Source facts below are directly
inspected; development test numbers are author reports, not independently reproduced
or audited test data. No external numerical constants were transplanted.

- [Stockfish19 search](https://github.com/official-stockfish/Stockfish/blob/edb0d9db6731067ec50ce619ff372b463bc4dd5d/src/search.cpp):
  singular eligibility requires a legal candidate context, adequate TT depth,
  non-decisive lower evidence and no nested excluded search. An excluded-move
  search drives extension/multicut/negative-extension choices. LMR also uses TT-PV,
  score/depth, histories and node/window context; these are interacting predictions.
  [5f7348f03f82](https://github.com/official-stockfish/Stockfish/commit/5f7348f03f820038f5d246b82de544cbc1d8ffd2)
  reports passing STC90784/LTC209640-game tests for less aggressive LMR in loose
  alpha windows. This supports investigating effort context, not CLV's exact rule.
- [Ethereal14 search](https://github.com/AndyGrant/Ethereal/blob/96cde8d2f82a3ab0536bf33572405fda96f6e236/src/search.c):
  depth>=8, a sufficiently deep lower-bound TT move and non-root context admit
  singularity. Its wrapper excludes a move while suppressing TT interaction at
  that node. Quiet LMR combines node/improving, ordering stage and history signals;
  reduced fail-highs receive deeper search. [799e3509657a](https://github.com/AndyGrant/Ethereal/commit/799e3509657af1a97f79d0a15315ec3cebd726cb)
  reports a singular-wrapper change with depth/margin changes, initially buggy
  max-ply tests, then corrected SMP tests (5161/21206 games). It does not isolate
  singularity's value or establish Seed transfer. [363f1008dbc7](https://github.com/AndyGrant/Ethereal/commit/363f1008dbc7b933152f4fcf2ac0c8def7460610)
  reports double-extension tests; unbounded extension growth is not justified.
- [Bagatur5.5c Search](https://github.com/bagaturchess/Bagatur/blob/56d1b3651f62bc770c7cde5f4c1b0360890b9bd5/Search/src/bagaturchess/search/impl/alg/impl1/Search_PVS_NWS.java):
  singularity has depth, check, ply-growth, TT bound/depth and move-possibility
  gates, plus a guarded double extension. Active LMR uses histories, volatility and
  repeated non-improving state. **Eval-search divergence and within-node score
  feedback blocks are commented out at this pin.** Older Seed descriptive material
  must not be read as evidence they are active. History includes explicit experiment
  commenting and [5b0c30803fdf](https://github.com/bagaturchess/Bagatur/commit/5b0c30803fdf380b97fbd621aafadb2c675f88fb)
  guarding double extension, plus PV/null scratch corrections. No independently
  audited empirical gain for those mechanisms was available in inspected records.

## CLV-C1 - near-depth TT-backed quiet verification effort

Explicit flags **3329490943 = MW-C4 + TT_CRITICAL_RELIEF**. Default production
stays1182007295. At interior ordinary-score null windows of depth>=4, a legal
completed hint and current-generation LOWER/EXACT selective estimate>=beta at
depth[d-2,d-1] mark node-level expected competitiveness. After existing relief and
history adjustment, reduce an otherwise-positive quiet LMR by one ply. Existing
pruning, fail-high nominal verification, extension cap, scores and publications
are unchanged. The marker is private per-node frame state, reset on every entry;
synthetic and verification probes do not consume it. Cumulative private counters
include incomplete jobs and are read only after drain. No new TT storage/read or
evaluation is needed; score and retained move are not asserted to correspond.

Hypothesis: after a formerly competitive continuation disappoints, some remaining
quiet alternatives deserve more verification than baseline LMR assigns. This is
deliberate selectivity, not exhaustive proof or an adopted strength improvement.

Initial fixed W4 run `c1-fixed-w4-v2`:40 requests/6,000,000 admitted nodes on20
historical mixed-phase roots,150k/request, fresh owners, alternating formulation
order. Candidate observed5788 eligible nodes/1618 relief applications. Eight final
moves differ across candidate/control; W4 scheduling means these differences are
not individually attributable causal improvements. All request/private accounting,
board/PV checks, input stability and process drain passed. Charged6.4504178s; no games.
Raw evidence and hashes are in its execution receipt. These roots are development
inputs and must be excluded from any confirmation. No clean-speed claim.

Setup observations: default Python lacked python-chess; v1 never reached admission
and launched no experiment. Reused existing isolated `build/research/ic-python`
1.11.2 with pinned files; no installation or old-state mutation. Frozen v1 remains.
Initial four new Java tests and four admission tests pass. Expanded checks passed
215 core tests and53/54 research tests; the one failure was an inherited TC-P1
default expectation predating build98, confirmed unchanged at starting HEAD.
Corrected that relevant identity test to require explicit historical TC-P1 and
the adopted MW-C4 default, preserving its other checks. All8 focused rechecks
passed. `technical-validation-v1.json` pins the retained XML, including failure.

## Replication, deeper diagnostic and selection boundary

`c1-fixed-analysis-v1.json` is produced by the hash-checking analyzer and retains
all request decisions. Later W1/W4 protocols use the same C1 kernel and a W1-capable
study. Frozen binaries and source identities distinguish these dirty build98
prototypes from accepted build98; they do not overwrite that production artifact.

| Fixed-work run | Requests / actual nodes | Candidate effect | Limitation |
| --- | --- | --- | --- |
| W1,150k,2 reversed rounds |80 /12,000,000|11194 signals,2720 applications; each root repeats exactly; one of20 distinct final moves changes; mean depth7.75 vs7.85|Deterministic effect, no quality proof|
| W1,up to1M |40 /38,904,550|26114 signals,5550 applications; five final moves change; mean depth9.9 vs9.8|Deeper selective reference, not truth|
| W4,300k,2 reversed rounds |80 /24,000,000|21922 signals,6383 applications; six of40 paired final moves change; mean depth8.175 vs8.325|Parallel trees vary; adverse depth cost retained|

At150k, both formulations agree with the1M **control** choice on14/20 roots.
Candidate agreement with its own1M choice is15/20 versus control14/20. This selected
small sample provides no demonstrated decision-quality advantage. Request wall is
reported descriptively; JIT, GC and observation/ordering effects preclude a clean
speed claim. No parameter was selected by maximizing these proxy results.

`c1-uci-qualification-v1` runs the **original build96 app-uci.jar/core.jar**, not a
new research control executable, plus current CONTROL/C1. All18 fixed-node UCI
requests pass identities, W4/W1/W2 reconstruction, stop dominance, reset/reuse,
legality/PV/accounting and natural process drain. Actual nodes480454. Exact96 W4
reports1182007295/EAGER_ROOT; other exact96 workers correctly retain historical
TC-P1 behavior. Current research CONTROL/C1 use OWNER atW1 and EAGER_ROOT otherwise.
The separate new Java lifecycle test coversW1/W2/W4/W8/W16. This is not native GUI,
platform or playing-strength qualification.

**Decision:** retain C1 unchanged for a bounded exploratory W4 screen against
exact96. Existing TT information supports this consumer without a redesign.
Consumption, deterministic integration and absence of correctness failures justify
an economical timed test; mixed proxies do not justify confirmation or additional
feature accumulation. If timed results are adverse, investigate node/decision
witnesses and targeted combinations or reject C1; do not blindly extend the sample.
Singular verification/move-associated information remains a possible later
dependency, not a preselected implementation programme.

Timed preparation:2 pilot pairs strictly operational, then32 different development
pairs/64 games, both prospectively frozen. Pilot scores never enter selection.
Development selection uses seed2026110601, no scores, excluding prior recorded
non-BOUNDARY2 selections and current diagnostic roots. It is not independent
confirmation freshness. The first selector wrote the v1 PGN then failed on a
Path/string digest call; preserve that input and failure record. Corrected v2
uses the same seed and a complete manifest. No experiment/game authority was spent.

No timed idle window exists. The screen is exploratory and imprecise; it cannot
replace the fixed independent score>=53% and paired95% lower>50% gate. All640
independent games and the sole attempt remain reserved. Full320-pair confirmation
is preferred if a later frozen candidate merits it and fresh inputs/feasibility
support it; neither candidate nor independent protocol is selected now.

## Committed candidate and exact handback

Commit46d15431 contains the attributable runtime work and the sole build98->99
increment, preserving exact version representation/user fields. The new flag is
non-default; production inventory needs no change. Committed runtime99 is frozen
in `candidate-build99-manifest.json`; kernel/config class bytes match the tested
prototype. Its18-request actual-UCI recheck passed with480447 nodes, original96
control identities, legal PV/accounting and natural drain. This is evidence-bearing
technical progress, not confirmation of stronger chess.

Six experiments total:276 requests,81,865,451 nodes,132.96221470012097 charged
seconds, zero timed games, no active owned processes or unresolved reservations.
All640 independent games and the sole attempt remain available only under their
original conditions. Tests/compilation are excluded from experimental charging as
prospectively specified. `budget.json` and execution receipts reconcile exactly.

Timed pilot/screen v2 are frozen but unlaunched. v1 preflight found one stale
supervisor hash under a duplicate Windows path spelling; the corrected prospective
v2 uses canonical paths and leaves v1 intact. Both v2 input checks pass and admission
correctly refuses absent owner-window authority. No refusal spent games or time.
Pilot operational review gates the screen; complete paired audits gate analysis.
The required next decision is the75-minute idle window, not confirmation selection
or production adoption. STATE contains exact commands and restart requirements.

The original default whitespace checker interprets CR in newly authored frozen
JSON as trailing whitespace. Frozen bytes/protocol hashes were preserved; a
per-command CR-aware check and explicit space/tab scan pass. No global Git setting
or repository attribute was changed. Raw binaries/logs/observations were verified
ignored and excluded from staging. No push, installation or deployment occurred.

## Owner-authorized timed development - 6 November 2026

The owner replied "Authorize75 minutes now" to the concrete pilot4/screen64
request. `owner-idle-window-v1.json` records first clock observation21:42:37Z and
hard deadline22:57:37Z on5 November (10:42:37-11:57:37am NZDT6 November), with the
explicit scope. Its timestamp is an observation, not exact native message timing.
That scope is now fulfilled; unused clock time grants no further games.

Pilot v2 passed4 normal games,515 independently audited requests, no qualified
fallback warnings and native drain; charged126.2590071s. Its scores were not
inspected for candidate selection or pooled. `c1-timed-pilot-w4-v2-operational.json`
is the evidence-bearing operational review that admitted the separate screen.

Screen v2 completed64 normal games: **31W/15D/18L,60.15625%**, paired-normal95%
**50.107827-70.204673%**, bounded95%36.148180-84.164320%, descriptive logistic
Elo+71.57 (paired-normal interval+0.75 to+148.89). Pentanomial[1,7,9,8,7];32 pairs.
All9039 requests passed independent game/history/PV/terminal/accounting audits,
input hashes were stable, no fallback warning occurred and all132 owned processes
drained. Charged2098.2132532s. Mean CPU35.84%, maximum66.67%; minimum free memory
16.14GiB, peak owned commitment0.79046GiB. No score-based stopping, exclusion,
replacement or tuning occurred. See the immutable protocol, execution and analysis.

This is **promising exploration, not independent strength success**. Candidate
score was76.5625% as White and43.75% as Black; first/second games62.5%/57.8125%.
These descriptive strata are not separate tests or causal explanations and are
not excluded/reweighted. The small sample, opening dependence, normal approximation
and mixed fixed-work proxies remain uncertainty. Neither the point estimate nor
the barely-positive exploratory normal lower bound satisfies independent acceptance.

Decision: select **unchanged C1/build99/source46d15431** for the sole independent
confirmation. This was the only implemented integrated candidate, not the winner
of a parameter sweep. The basis combines externally motivated TT information use,
demonstrated consumption, deterministic integration, worker/lifecycle qualification,
economical fixed-work costs and the completed competitive observation. The adverse
depth observations and colour split are preserved. No further development or
candidate substitution is selected before this attempt.

## Frozen independent preparation and remaining authority

The comprehensive input-only freshness audit includes360 recorded input files,
all tracked Search PGN fixtures/root and history TSVs, selection keys, hash-verified
named replay inputs, the entire legal16 development pool and CLV inputs. Frozen
clock **opening/selection inputs only** were included; no clock result, execution,
analysis, transcript or tournament-output files were opened or reinterpreted.
Some historical input manifests retain source game result headers; the selector
ignores them. This work does not revise any closed study's conclusions.

The old16-ply pool could not supply320 fresh pairs after these exclusions. To retain
full real history and the established broad source family, use exact14-ply records
from [pinned Stockfish-books bjbraams source](https://github.com/official-stockfish/books/tree/a804a97e709f40aa75a00f9d19e84915b7a3a45c).
Freshly retrieved archive/unpacked bytes match the historical local source; manifest
pins them. Of198350 records,44033 have14 plies,4264 of these overlap exclusions,
and19928 distinct legal positions remain in the fixed odd-hash partition. The fixed
seed2026110605 selects320, with zero overlap. No evaluator/result filtering or
confirmation Search was performed. `confirmation-v1-openings.selection.json` and
`confirmation-freshness-v1.json` preserve the exact distribution and all exclusions.
This seven-move distribution differs from the eight-move development sample; that
transfer uncertainty is explicit, not retrospective acceptance redefinition.

`c1-confirm-w4-v2-protocol.json` freezes640 games/320 opposite-colour pairs, original
exact96 control, unchanged C1, source/runtime/evaluator/configuration, input order,
runner, all447 input pins, no interim score access and the original primary gates.
Primary normal interval uses the CONTRACT's1.96 multiplier and sample pair variance;
zero sample variance is explicitly reported as a degenerate descriptive interval.
Bounded95%, fixed-seed10000-resample pair bootstrap, colour/order and operational
sensitivity remain supplementary, without changing the gates or excluding games.

The sole attempt is consumed at first durable pair-start; both games are then
charged. Interrupted/disqualified attempts cannot pass or be replayed/replaced.
Pending ownership and the durable progress journal prevent silent restart. Nine
focused tests cover actual supervisor control flow with isolated synthetic workers,
including successful640-game accounting, failed-pair spending and refusal before
process creation. They launch no real engines/games. Full evidence/order/identity
preflight passes. Existing Windows Job, PC guards and independent game/request
audits are reused. `confirmation-readiness-v1.json` records checks and limitations.

Preparation failures are preserved: selectionv1 rejected inherited source outcome
headers unnecessarily; v2 exposed a manifest-relative path; v3 established the old
pool's inadequate cardinality; v4 successfully selected the new14-ply population.
No partial confirmation input was used. A synthetic-test mock PID needed an integer;
the corrected9-test run passed. Confirmation protocolv1 never launched: its full
evidence dry-run caught a missing local binding after helper extraction; corrected
v2 keeps the identical candidate/sample/gates. No attempt or confirmation game was
spent by these preparation steps. Logs remain under the named ignored raw paths.

Current expenditure: **2357.434475000133s /86400s**,68 development timed games,
zero failures/interruption,0 confirmation games/attempts,81,865,451 fixed-work nodes.
All experiments drained; no active pending reservation. Planning reserve now28830s
for the same authorized640-game confirmation, within the existing24-hour ceiling.
The observed throughput projects20982s (5h49m42s); its25%+120s allowance requires
26348s. Freeze an8-hour maximum plus30s drain and request an**8h15m PC-idle/awake
window**. The changed opening distribution makes throughput/variance forecasts
uncertain. No confirmation is authorized by the already-completed75-minute scope.

Master pair remains P4-R034/P4-F052; focused research advances without another
programme-canon change. Owner Project Source refresh remains necessary before
mirror-dependent coordination. No acceptance, champion, production promotion,
installation, push or deployment follows. Strength objective remains unproven.

## Sole independent confirmation - completed 7 November 2026

The owner explicitly authorized the separately requested8h15m idle/awake window.
`owner-idle-window-v2.json` records first clock observation2026-11-06T11:55:45Z
through20:10:45Z (12:55:45am-9:10:45am NZDT7 November), for the sole frozen
`c1-confirm-w4-v2` attempt. Actual admission11:57:07.989560Z; completion
17:45:45.009638Z, inside both the8-hour run ceiling and owner window. First durable
pair-start consumed the attempt, recorded at11:57:10.723820Z. No intermediate
scores were inspected by Codex; deterministic process waits supervised the unchanged
run. No additional game, replacement, tuning, candidate substitution or stopping
decision was made from scores.

**640 normal games /320 fresh opposite-colour pairs:242W/191D/207L.** Candidate
points337.5/640, score**52.734375%**; pair sample variance0.0782076557601881,
SE0.015633263390942655. Frozen primary paired-normal95% interval:
**49.6702553754-55.7984946246%**. Gate A(score>=53%) **FAILED**;
gate B(lower strictly>50%) **FAILED**. This is an unsuccessful acceptance outcome
with inconclusive statistical strength evidence, not demonstrated improvement or
equivalence. The positive point estimate does not establish the governing objective.

Pentanomial[23,73,112,70,42]. Descriptive logistic effect+19.0194 Elo, approximate
normal interval about-2.3 to+40.5 Elo. Supplementary pair bootstrap95%
49.6875-55.859375%; bounded Hoeffding95%45.142357-60.326393%. These methods have
different assumptions; none changes either primary gate. Pairing does not eliminate
opening-family, common-opponent, platform or selective-distribution dependencies.
The held-out seven-move distribution differs from the eight-move exploration and
qualifies only its declared domain. No historical clock result was opened or revised.

The frozen result is `c1-confirm-w4-v2-analysis.json`; the raw replay and exact
arithmetic verification are `c1-confirm-w4-v2-verification.json`. Every original
pair total, WDL, primary mean/variance/interval/gate and all frozen input/raw hashes
match independently. Candidate White score62.03125%, Black43.4375%; actual first-game
score52.03125%, second53.4375%, with160 openings beginning each colour order.
These strata are descriptive only; no exclusion, weighting or extra test follows.

**Supplementary reporting correction:** the frozen analyzer's
`descriptiveGameOrderScores` enumerated the verifier's colour-canonicalized records,
so its43.4375%/62.03125% fields were colour slots rather than actual execution order.
`tools/p4-critical-line-final-audit.py` derives the corrected52.03125%/53.4375%
directly from raw sequential PGNs and checks all640 legal histories/terminals and
all320 pair totals using exact rational arithmetic. The original analyzer and its
output bytes remain unchanged. Use the verification supplement for game-order
sensitivity. This implementation correction affects no primary statistic, game
validity, resource total, frozen condition or acceptance conclusion; it is not a
retrospective protocol relaxation.

All**90187** engine requests passed independent UCI/PV/history/legal terminal and
whole-request resource audits (baseline45085; candidate45102). Total admitted
nodes29,017,307,055; baseline14,465,601,877 and candidate14,551,705,178. No qualified
fallback warnings occurred. Mean total CPU32.06%, maximum65.84%; minimum free
memory17.9071GiB, peak owned commitment0.818516GiB. All1284 owned processes drained;
no process/reservation remains. Actual game lengths49-426 plies, mean154.9172.
Execution charged20916.9871262s (5h48m36.99s), including startup, in-worker audits,
supervision and drain under the frozen charging convention. The native execution
receipt records all evidence hashes and exact endpoints.

## Final interpretation, resources and handback

**Codex technical/research execution is complete; the governing practical W4
strength-development objective is UNMET.** The independent attempt was valid and
both acceptance conditions failed. No further autonomous research unit is selected:
the sole independent allowance is spent, and another confirmation or changed
acceptance condition requires new explicit owner authority. This is not a claim
that every selectivity idea is ineffective or that the full research space/time
budget is exhausted. No extra resources are requested to rescue inconclusive data.

C1 was genuinely enabled/exercised, its TT input was available and its state,
accounting, concurrency and lifecycle behavior qualified. A small fixed-work depth
cost and unchanged deeper-control agreement were retained before exploration.
The completed confirmation provides no material technical failure explaining away
the strength result. Small true benefit, sampling variation and opening-population
effects remain compatible with the evidence; their separate contributions are not
identified. Do not pool the positive64-game screen or operational pilot with the
independent sample, reinterpret proxy depth/NPS as strength, or declare an enabling
foundation successful in place of the failed governing objective.

The investigation establishes a limited information-flow fact: existing coherent
selective TT fields can drive node-level reduction relief without a table redesign.
It does not establish that they support move-specific singular verification;
score/hint association remains a distinct dependency. Potential future questions
include reliability of a near-depth bound when the leading continuation disappoints,
interactions with quiet pruning/history relief, and adequately isolated excluded-move
verification. They are conditional opportunities with uncertain value, not an active
queue, selected successor or authority to repeat this claim.

Final ledger: **23274.421601200127/86400 charged seconds (6h27m54.42s)**;
**708/1280 initiated/completed timed games**, comprising68 development and640
confirmation; **1/1 independent attempts spent**. Failed/interrupted games0.
Fixed-work admissions81,865,451 across276 requests; timed request audits99741.
All9 supervised experiments passed/drained; pending and ownedProcesses empty.
Unused63125.5783988s and572 development games are not renewed claim, continuation
or successor authority. Both explicit idle-window scopes are fulfilled.

Current engine source/configuration remains C1/build99/source46d15431 against
original build96/source44fc2bf2, as pinned. Only records, input-neutral verification
tooling and minimal technical-disposition canon updates are made after confirmation:
**no_bump**, exact build99 bytes retained. Production flags1182007295, champion,
baseline79, closed campaigns and installed/external clients remain unchanged.
No native GUI/platform/deployment acceptance was performed or inferred; there is no
push, installation, production promotion, champion advance or formal closure.

Masters advance to**P4-R035/P4-F053** for this technical disposition. The owner must
refresh both Project Source mirrors before dependent GPT coordination. GPT
reconciliation and owner disposition/possible formal closeout are outstanding;
neither is claimed by this handback. STATE is the exact next-action boundary.
