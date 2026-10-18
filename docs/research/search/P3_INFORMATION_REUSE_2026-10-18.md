# Phase 3: Search Information Retention and Reuse — O1 + O2

Owner-selected first CGLHW, 18 October 2026 (Pacific/Auckland). **ACTIVE research;
no candidate accepted or promoted.** Governed solely by the Phase 3
[Contract](../../../source/CHESS_SEARCH_CONTRACT.md) and
[Frontier](../../../source/CHESS_SEARCH_RESEARCH_FRONTIER.md). This focused record
contains evidence and resume state, not additional policy authority.

## Resume and work selection

Current work: baseline, O2 and combined O1/O2 diagnostics, integrated regression,
worker lifecycle checks and hint-lifetime replay passed. Hints alone (policy 4)
completed its 32-pair one-worker clock screen with a promising development result,
not confirmation. The frozen hints + EFFORT history retention + nominal-only
maluses screen (policy 7) is running. Assess it only after completion; then select
informative reverse ablations, separate multiworker evidence and fresh independent
confirmation. Production defaults and the accepted champion remain unchanged.

Version-dependent operations are BLOCKED by the inherited reservation described
below. Independent O1/O2 source inspection, research, testing and separately
attributed evidence continue under the launch's safe-work authorization. Do not
attribute this work to the old operation, finish its token, edit its state, bypass
its integrity check, or claim authoritative finalized identity. No build bump.

## Bootstrap facts and provenance

Starting clean `main` HEAD: `d536f7ad8000172f00addd55d810f7d647196969`.
Root journal absent; VERSION_STATE.txt present, 0.0.0 build 35, SHA-256
`92a26d4e187edc6c47ed88100d355f77b4597d98524d21d005a3c164986b4bca`.
Remote `origin` exists; no push authorized. The historical architecture text's
no-remote statement describes its earlier checkpoint, not current state.
Predecessor masters P3-R002/P3-F001 match the launch. Owner synchronization
confirmation supersedes the Frontier's historical P3-R001 refresh wording.
Activation updates only current scope/status, not binding proof/production policy.

The pinned shared finalizer/recovery hashes and maintained-source commit
`53fac440d2da90642e926492f1278d3f98dc9939` match. Local `begin` reports
`IntegrityFailure: Nested repositories are unsupported.`, no new token.
Read-only status finds retained token `ebe0487f7c214b398fc2625a8ac053fa`,
phase active, mutation not_attempted, local no_bump attempt with integrity_failure.
Its baseline HEAD is `11060c8df2e5fc4dcc7356f52e9ea4a6e18439f2`;
baseline SHA-256 `d295906e131fa2c4c9b04532fa8f996dd70501c0b4f5d0db73ec71ed8a5ca4a5`,
state SHA-256 `f34cd8ba09a6abf0642fab88d1f5d78b337452a90a21b540eeef94f01c3b6d2c`.
The current working-content scan finds no nested .git. WU7's
[record](../../unified-architecture/STATE.md) explains validation clones were
preserved under `.git/portable-build-validation`; the adapter rejects the stored
integrity failure before recapture. This is a real retained finalization blocker,
not a current nested-clone finding or terminal non-blocking warning. Shared r3
requires blocked/not_attempted evidence; this local active/integrity_failure shape
is ineligible. A separately authorized supported local reconciliation extension
with immutable history, endpoint accounting and disposable-repository tests is
the proportionate repair path. No policy/tooling/metadata repair is made here.

Accepted champion: SR-001 + SR-029 + SR-022 + SR-005 + SR-036 + SR-024,
Search source anchor `35294146bb7e44975b516005b2cc196439666e09`. Current
`ProductionSearch.create` selects QXRAY + quiet LMR + WINDOW + EFFORT at one
Pair-2 worker and ROOT_REUSE + QXRAY at 2–16. Static/SEE-off/LMR-off/LMP-off,
independent exact/TT-off, HCE and other evaluator paths remain explicit controls.
The production generation-32 payload in `build-assets/production` was freshly
hashed: `5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7`.
Lineage `235d0688-e060-44e3-8f1e-119c39bc8017`, checkpoint
`g000032-s001753088-5837c7f7192aa3cf9a265388f93649327455e804541398ae3ef6b69e09014304`,
quarter-residual mapping unchanged. Source verification is not runtime parity.

The independent Bagatur report resolves from the canon link to
`C:/projects/seed/java/BAGATUR_SEARCH_FRONTIER_ARCHITECTURE_ANALYSIS.md`.
Its O1/O2 and cross-feature findings support hypotheses below. It is external
comparative evidence, not a SeedV6 implementation prescription. Relevant accepted
Phase 2 reports SR-005/024/029/022/036, SR-023 label findings, measurement baseline
and tournament execution procedure were inspected in their historical context.

## Verified implementation and bootstrap challenge

- TTable is direct mapped, three long arrays, 32 coherent striped locks. Requested
  64 MiB rounds to 2,097,152 entries / 48 MiB primitive payload; requested 72 MiB
  would retain that capacity in the existing representation. Actual object/lock
  overhead and traffic still need measurement. Current replacement may reject a
  same-key shallower/equal-depth same/weaker-type update even across generations;
  retained moves can nevertheless guide while their score is inapplicable.
- ExactSearch ordinary proof requires equal depth/current generation, original
  window classification, cutoff-only bounds and selective-epoch eligibility.
  Matching older/deeper moves undergo legal membership checks. SearchKey mixes
  board key, complete status and ordered relevant real-history fingerprint. No
  position-only fallback. Qsearch is not a nominal TT producer; conservative
  ancestor suppression also removes associated hash-move production.
- Main quiet history is 65,536 ints, side/piece/from/to, signed gravity bounded
  at 16,384, bonus min(depth,64)^2. Quiet winners reward and prior searched quiets
  receive maluses. Omitted moves are removed from the searched prefix. A reduced
  unverified fail-low may currently receive a malus after a later quiet cutoff;
  that is heuristic selective evidence, not an independently verified nominal
  failure. Richer capture/continuation/killer/countermove controls are unadopted.
- Ordinary history clears each invocation; SR-005 HISTORY/PVHISTORY retains only
  completed adjacent full-window iterations with matching root/history/request.
  Abort clears reuse/history. ROOT_REUSE seeds private workers after the eldest
  root branch, drains before copying and publication, and excludes root proof.
- EFFORT preserves completed PV-prefix and root inclusive effort. TT precedence
  remains first. Ordering changes affect actual quiet rank, PVS retries, WINDOW
  and LMR. A hint experiment must account for those effects, not call it purely
  mechanical or expect selective equality to a finite-horizon oracle.

Challenge outcomes: do not reuse historical HISTORY directly as a cumulative
candidate, because it replaces EFFORT and predates QXRAY. Retain champion guidance
when testing lifetime. Missing hints do not establish replacement pressure or
need for a new TT: first separate producer suppression from collision/retention.
Do not manufacture labels for omitted/censored outcomes. Nominal label filtering
and lifetime may interact; independent component Elo is not required for a joint
candidate. Exact oracle disagreement is a diagnosis gate. Proof and hint streams
must remain separate even when hint-only information pays off.

## Prospective research sequence and evidence rules

1. Observation-neutral current-champion probe: TT probes/hits/proof stores and
   completed useful-move opportunities; history update eligibility and retained
   coverage. Verify score, PV, node and completion parity with instrumentation off.
2. Controlled EFFORT-preserving request-local history retention and nominal-label
   filtering. Compare baseline, retention, label filtering and their combination
   if observed label density warrants it. Explicit fresh-request/abort/new-game
   and private-worker checks. No cross-request persistence initially.
3. Completed same-SearchKey legal hints using existing proof-safe construction
   seams. Measure current-storage alternatives before redesign, no score admission
   or proof replacement changes. Include hint priority versus EFFORT and overhead.
4. Integrated candidate and reverse ablation if mechanism/cost evidence warrants;
   progressive paired/color-reversed clock screens, then frozen fresh independent
   confirmation at the inherited 10+0.1 standard, with prospective looks and
   multiplicity/selection accounting. No development data becomes confirmation.

Initial diagnostics use small outcome-blind full-history roots, depth/node caps,
one process/worker, constrained heap and BelowNormal priority. Exact and TT-off
references, unpruned qsearch, terminal/draw/mate/PV/cancellation checks precede
strength claims. Worker counts and concurrent games are distinct; one-worker
results do not certify ROOT_REUSE. No unrelated heavy work overlaps clocks.
The existing `tools/tournament-session.py` and `tools/search-baseline.py` provide
frozen inputs, pair blocks, Windows accounting and failure preservation; reuse
them instead of inventing orchestration. No disruptive 12-hour run authorized.

Detailed experiment configurations/manifests and compact results belong in
`p3-information-reuse-2026-10-18/`; bulk logs/observations in its ignored `raw/`.
Completed protocols stay immutable, including negative/incomplete evidence.
Next decisions follow information value, not an arbitrary experiment quota.

## Baseline observation — completed P3-O1O2-BASELINE-v1

[Frozen configuration](p3-information-reuse-2026-10-18/baseline-v1.json),
[source/host manifest](p3-information-reuse-2026-10-18/baseline-v1-manifest.json),
[compact results](p3-information-reuse-2026-10-18/baseline-v1-summary.json),
[bootstrap tests](p3-information-reuse-2026-10-18/bootstrap-validation.json).
Bulk output is local at `p3-information-reuse-2026-10-18/raw/baseline-v1.jsonl`.
The tool uses existing internal-search observation hooks with policy OFF, a
counting TTable subclass and completed-history reads outside recursion; no core
runtime source changed in this unit.

26 historical development roots, depths 1–6, 250,000 recursive-entry cap per
invocation: all 156 observations completed. Plain/observed score, best move, PV,
node/qnode counts, completion, selectivity and ply metadata matched. Cumulative
composition matches actual ProductionSearch at the tested opening through D4.
These are bounded diagnostic checks, not engine-strength evidence.

Across these observations: **300,044 TT probes, 225 hits, 552 save attempts**.
At D6: 208,407 probes / 196 hits; among nodes reaching the existing observation
point, 167 legal TT hints and 126 legal completed-PV hints. Stores are extremely
sparse in this sample; capacity/associativity redesign is not justified by it.
Selective producer suppression is the better first intervention hypothesis.
An existing-table hint-only experimental payload should first preserve every
ordinary proof entry and its replacement policy; no extra allocation is presumed.

Completed D6 history has 3,191 nonzero cells summed over 26 private owners, no
saturated cell; 1,592 cells overlap D5, 1,403 with the same sign. This supports
investigating lifetime but does not establish predictive validity: these are
separately relearned, censored tables on historical development fixtures.

Baseline regression: engine-core 61/61 passed; selected research Search/driver
405 cases, 394 passed and 11 existing skips, zero failures/errors. New probe
tests pass 2/2 after correcting a harness assumption. The first failed bridge
expected `recursive entries minus roots == production nodes`; actual adapter
counts prepared child transitions and explicit reentries, while recursive
entries include additional PVS retries. Preserve both quantities. The corrected
test counts the production admissions directly and matches its 2,172 nodes;
the earlier expectation was 2,239. Failed log/XML remain in raw evidence.
No engine change or oracle relaxation was used to fix the harness.

Instrumented elapsed values are not clean cost estimates. No clock games,
candidate, confidence interval, promotion, or multiworker strength claim yet.
Read-only baseline results do not resolve retained version-finalizer state.

## O2 retention and label quality — completed development diagnostics

[Prospective plan](p3-information-reuse-2026-10-18/history-v1.json),
[source manifest](p3-information-reuse-2026-10-18/history-v1-manifest.json),
[mechanism summary](p3-information-reuse-2026-10-18/history-observe-v1-summary.json),
[cost summary](p3-information-reuse-2026-10-18/history-cost-v1-summary.json).
The raw `history-observe-v1.jsonl` and `history-cost-v1.jsonl` files remain local
under this experiment's `raw/`, with logs and retained test XML.

`EFFORT_HISTORY` adds history retention to EFFORT rather than replacing its PV
and effort ordering. `HistoryLearning` tracks nominal child-depth observations in
16,448 bytes of worker-private primitive bit storage; an experimental filter
omits maluses for reduced unverified failures. All searched outcomes remain
heuristic under selective descendants. Production factories do not select either
alternative. No cross-request persistence or broader retry lifetime was added.

17 focused tests pass, including expanded retention invalidation/rule cases,
independent unpruned enumeration, owner isolation, aborted-learning clearing and
observation parity. Across 780 diagnostic observations, the 156 champion results
match the pre-change frozen baseline, and 156 observation-only results match the
champion. Baseline observed 19,606 nominal and 1,941 reduced-unverified maluses;
the latter are about 9% of maluses in this sample, not independent error labels.
The per-invocation 250k cap interrupted 1 retention, 2 filter-only and 1 combined
D6 observations. They remain in evidence; completed-comparison summaries exclude
them explicitly and do not interpret interruption as inferiority or futility.

The separate 416-request cost run completed every D6 request with its larger
prospective safety budget. It uses two rotated rounds, four warmed owners, no
learning observer and production admission accounting:

| Configuration | D6 nodes / champion | D6 elapsed / champion | 100k-admission mean completed depth |
| --- | ---: | ---: | ---: |
| Champion | 1.000 | 1.000 | 9.038 |
| EFFORT_HISTORY | .962 | .962 | 9.154 |
| Nominal-only maluses | 1.059 | 1.020 | 9.077 |
| Both | 1.043 | .990 | 9.154 |

Retention elapsed ratios vary .912–1.013 across rounds; this is noisy local cost
evidence, not a reliable speedup claim. Fixed-node elapsed ratios are 1.020,
1.042 and 1.004 respectively. Thread allocation, GC/compilation indicators and
new-game reset costs are retained. Thread CPU does not include background JIT/GC
CPU, and engine construction is excluded. No strength games have run.

Disposition: retain all three as controlled research alternatives; no standalone
adoption or rejection. Better label semantics need not reduce nodes, and retaining
biased history may help or hurt. O1 combinations now have greater decision value
than a threshold sweep of these unchanged formulations. Broad final regression
and any candidate's clock/worker confirmation remain required.

## O1 and combinations — completed development diagnostics

[Prospective plan](p3-information-reuse-2026-10-18/hints-v1.json),
[source manifest](p3-information-reuse-2026-10-18/hints-v1-manifest.json),
[mechanism summary](p3-information-reuse-2026-10-18/hints-observe-v1-summary.json),
[cost summary](p3-information-reuse-2026-10-18/hints-cost-v1-summary.json),
[integrated validation](p3-information-reuse-2026-10-18/integrated-validation-v1.json).
Raw observations/costs and logs remain in this experiment's `raw/` directory.

Explicit `withCompletedHints` uses a previously unused payload bit within the
existing three TT arrays. Ordinary probe excludes hint-only entries completely;
ordinary proof saves may displace hints, and hints never displace any proof.
Current-generation deeper hints survive shallower completions; equal-depth latest
completions and old generations can refresh. Complete positive-depth real nodes
with a searched move may publish a hint, including selective nodes. Incomplete
nodes, qnodes, synthetic/verification and restricted root jobs cannot do so.
Completed descendants remain eligible when a later ancestor is cancelled.
Probes retain the exact SearchKey and validate current legal membership; hints
affect existing ordering/protection, never scores, bounds or PV certification.
All production factories leave the new behavior off. This is an experimental
representation/replacement alternative, not adoption of a new production policy.

936 diagnostic invocations: champion and observation-only each match all 156
pre-change baseline results. Hints alone produced 258,915 eligible completions,
256,042 accepted hint writes and 80,696 legal hits from 261,727 probes, zero illegal
hits. This addresses the observed scarcity of 552 ordinary stores without relaxing
proof admission. One retention+hints and two all-feature D6 invocations hit the
250k cap; their incomplete evidence is preserved, not labelled inferior.

The uninstrumented 520-request cost run completed all D6 requests:

| Policy | Components beyond champion | D6 nodes / champion | D6 elapsed / champion | Mean depth at 100k admissions |
| --- | --- | ---: | ---: | ---: |
| 0 | none | 1.000 | 1.000 | 9.038 |
| 4 | hints | .863 | .894 | 9.231 |
| 5 | hints + retention | .846 | .934 | 9.346 |
| 6 | hints + nominal-only maluses | .848 | .914 | 9.269 |
| 7 | all three | .840 | .908 | 9.346 |

Round-to-round timing remains variable; all costs are diagnostics on historical
development roots, not strength. Complete request admissions, allocation, reset,
GC and JIT indicators are retained; thread CPU excludes background JVM CPU.
Direct constructor allocation measurement on this JDK gives 50,335,976 bytes for
both requested 64 and 72 MiB, including arrays/locks (2,097,152 entries and
50,331,648 array payload bytes). A request increase to 72 adds no capacity in this
representation. No default memory change or redesign is justified here.

Integrated regression: engine-core 64/64 passed; selected research Search/driver
and UCI 430 cases, 419 passed and 11 existing skips, no failures/errors. Tests
include proof/hint segregation and dominance, generation/replacement, concurrent
collisions, legality/identity, independent exact enumeration, aborted publication,
combined-history clearing and four-worker ROOT_REUSE drain/reuse. The new launcher
and research workspace build passed. Match/session Python tests passed 3 + 23.
Four-worker hints are implemented with existing private ROOT_REUSE histories;
history retention/filter combinations are explicitly unsupported above one worker.
Functional four-worker checks do not establish multiworker strength.

## Clock development v1 — frozen, execution underway

Policies 4 and 7 are chosen for complementary decision value: isolate information
availability, then test an integrated candidate without demanding independent O2
Elo gains. [Policy 4 plan](p3-information-reuse-2026-10-18/clock-development-v1-p4.json)
and [policy 7 plan](p3-information-reuse-2026-10-18/clock-development-v1-p7.json)
freeze 32 color-reversed pairs each at 10+0.1, one worker, concurrency one,
BelowNormal process priority, four eight-pair blocks and no outcome-based stopping.
Shared new partition-0 openings exclude 92 prior selection receipts. They remain
development data; correlated comparisons and tuning selection preclude a
confirmation claim. No post-hoc extension or selective failure exclusion.

The comparator uses the pre-experiment core JAR preserved before O2 edits, SHA-256
`9b41d6b174846cfaf3af9f3089d8c3d13b0614fbf0a4717a6fe46aec346c61d9`;
new research launcher/classes are shared. The frozen installations are local at
`raw/clock-v1-baseline` and `raw/clock-v1-candidate`. Each prepared study freezes
its own copies, runner/tools, JDK, model and opening schedule. Source/hash manifests
are the provenance; embedded build 35 is not a finalized research version.
[UCI preflight](p3-information-reuse-2026-10-18/uci-preflight-v1.json) confirms pinned
Pair-2 identity, cumulative policies, warmup and two legal exactly-100k-admission
requests for each binary/configuration. Complete process costs and all requests
will be audited. No equal-compute inference from equal clocks.

Resume from `raw/clock-development-v1-p4/plan.json` and the equivalent `p7` path;
both are already prepared. Invoke each study's `inputs/tools/tournament-session.py`
with the validated `build/research/venv/Scripts/python.exe`, `-B`, and `status`
before resuming `run --plan ... --stop-before <explicit UTC time>`. An existing
controller owns an OS lock; do not start a competing controller. Never prepare
over these directories, replay partial blocks or alter the frozen inputs. Use
`summarize_clocks.py` only on a completed full screen, with new local export and
summary destinations. Initial source/results are committed at `dc8ad41`.

The follow-up diagnostic was prospectively specified in
[lifetime-v1.json](p3-information-reuse-2026-10-18/lifetime-v1.json): first four
completed p4 games, separate color ownership, prior-request hint ages and storage
pressure at 64/1 MiB. It ran between the screens, with no overlapping clock games.
This is observer work, not a new candidate mechanism. Preserve all raw trajectories
and instrumented results.

The separate [four-worker hint screen](p3-information-reuse-2026-10-18/clock-development-v1-w4-p4.json)
is also prepared at `raw/clock-development-v1-w4-p4/plan.json`, with the same
development openings, 32 pairs, concurrency one and BelowNormal priority. Run
after p7, verify its own frozen-binary preflight and observe host headroom; never
pool worker domains. Current user GUI work must remain untouched.

`tools/p3-information-confirmation.py` supplies the Phase 3 identity checks around
the existing SR-032 staged executor and unchanged SR-024 decision rule. A small
back-compatible runtime-tree check now allows the actual pre-experiment champion
binary on one side and the experimental binary on the other. Three new binding
tests and six shared continuation tests pass. No confirmation protocol or finalist
has yet been frozen. Initial family contrast indices 1/2 are reserved for one/four
workers, with prospective 128/512-pair looks and inherited alpha spending,
two-sided eligibility, sensitivity and stopping rules. Freeze an immutable copy
of the assessor/dependency bundle before any confirmation; closed protocols and
their tools must remain unchanged. Later retuned confirmations require unused
family alpha, not reuse of a closed contrast.

### Completed policy-4 screen

[Complete assessment](p3-information-reuse-2026-10-18/clock-development-v1-p4-summary.json):
**30 wins / 19 draws / 15 losses**, 61.71875%, descriptive logistic estimate
**+83 Elo**. Paired normal 95% interval +15 to +158; conservative bounded-pair
interval -87 to +311. All 64 games have normal, independently audited endings;
all blocks, complete requests, process lifetimes and frozen inputs passed audit.
The full screen took 1,865 block-wall seconds (about 31 minutes, 124 games/hour).
Candidate/champion full match process CPU: 946.78/945.53 seconds, ratio 1.0013.
Totals include JVM startup/warmup/JIT/GC and aborted requests, but not separate
preflight JVM CPU. Candidate completed 4,433 requests / 1.364 billion admissions;
champion 4,424 / 1.442 billion. Different game trajectories mean these are resource
observations, not an equal-position throughput comparison.

This is a small, selected development comparison. The normal approximation has
few pairs on the losing side and cannot satisfy the inherited confirmation rule.
The conservative interval includes no gain. No acceptance or promotion follows.
Disposition: promising enough for the frozen combined screen, interaction work
and fresh independent confirmation if subsequent validation remains sound.

A 15-second tail host sample saw 21% system CPU, about 16.7 GiB free, zero paging,
and an active user SeedV6 GUI consuming about one logical CPU alongside the match.
This is useful headroom evidence, not a reconstruction of unsampled desktop load.
Do not stop or reconfigure the user's GUI. Keep shared-workstation and approximate
pair-independence limitations explicit in later strength assessments.

### Hint lifetime and pressure

[Selection](p3-information-reuse-2026-10-18/lifetime-v1-selection.json),
[source/binary/test manifest](p3-information-reuse-2026-10-18/lifetime-v1-manifest.json),
[execution](p3-information-reuse-2026-10-18/lifetime-v1-execution.json),
[results](p3-information-reuse-2026-10-18/lifetime-v1-summary.json).
The first four scheduled games represent two color-reversed openings, not four
independent samples. Four configurations replayed 176 actual pre-move roots each,
with separate private color owners and D1–5 searches. All 704 requests completed
D5; every plain/observed score, legal PV, node and provenance tuple matched.

| Policy / requested MiB | TT hint probe hits | Older-request hit fraction | Collision replacements | Maximum hint occupancy |
| --- | ---: | ---: | ---: | ---: |
| 4 / 64 | 197,593 | 5.18% | 6,060 | 3.21% |
| 4 / 1 | 190,398 | 5.08% | 237,482 | 87.45% |
| 7 / 64 | 197,863 | 5.31% | 5,788 | 3.14% |
| 7 / 1 | 191,405 | 5.25% | 232,346 | 87.03% |

Most reuse is within the current request; prior-request guidance is present and
remains protected by unchanged identity/legality. Storage pressure creates large
replacement traffic at 1 MiB, while 64 MiB is lightly occupied in this shallow
sample. Node differences are small and selective; they do not establish strength
or justify a smaller/larger production default. Full-depth game retention remains
less directly measured. A redesign or identity relaxation has no demonstrated need.

Seven focused tests passed: three observer/real-history identity checks and four
research configuration/lifecycle cases, including actual 2-, 4- and 16-worker
completion, exact shared node-cap accounting, cancellation and helper draining.
These augment, and do not replace, the earlier broad regression. No production
runtime source changed in this diagnostic unit.

## Human dependencies and boundaries

- Blocking finalized-version operations only: supported reconciliation authority
  for the inherited local integrity-failure reservation. Independent research may
  continue; no old-token attribution, metadata bypass or manual version edit.
- Blocking dependent GPT planning/subsequent launch: owner refreshes both Project
  Source mirrors to P3-R003/P3-F002 and confirms synchronization. Current Codex
  repository-based research is already authorized.
- Blocking any future production adoption: Contract confirmation and explicit
  GPT/owner acceptance. Experimental candidates are not accepted champions.
