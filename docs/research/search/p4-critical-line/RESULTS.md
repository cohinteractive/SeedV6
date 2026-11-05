# CLV-R001 findings and decisions

6 November 2026. Initial investigation; no playing-strength evidence yet.

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
