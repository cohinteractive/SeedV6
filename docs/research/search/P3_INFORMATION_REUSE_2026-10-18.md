# Phase 3: Search Information Retention and Reuse — O1 + O2

Owner-selected first CGLHW, 18 October 2026 (Pacific/Auckland). **ACTIVE research;
no candidate accepted or promoted.** Governed solely by the Phase 3
[Contract](../../../source/CHESS_SEARCH_CONTRACT.md) and
[Frontier](../../../source/CHESS_SEARCH_RESEARCH_FRONTIER.md). This focused record
contains evidence and resume state, not additional policy authority.

## Resume and work selection

Current work: baseline observation complete; next implement explicit, default-off
EFFORT-preserving history retention and nominal-label controls, then completed
same-SearchKey hints in the existing table where proof is absent. Keep defaults
unchanged. Compare coherent combinations only after individual semantic controls
pass. No strength conclusion yet.

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

## Human dependencies and boundaries

- Blocking finalized-version operations only: supported reconciliation authority
  for the inherited local integrity-failure reservation. Independent research may
  continue; no old-token attribution, metadata bypass or manual version edit.
- Blocking dependent GPT planning/subsequent launch: owner refreshes both Project
  Source mirrors to P3-R003/P3-F002 and confirms synchronization. Current Codex
  repository-based research is already authorized.
- Blocking any future production adoption: Contract confirmation and explicit
  GPT/owner acceptance. No candidate or strength claim exists at bootstrap.
