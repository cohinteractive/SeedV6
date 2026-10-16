# Unified SeedV6 migration contract

Revision 1, 16 October 2026 (Pacific/Auckland). This is the SeedV6-local
Contract-Governed Long-Horizon Workstream authorized by the owner's Work Unit 0
request. Architectural discovery is complete; the five-project decision is
accepted. This document governs migration, not new search research or shared
behavioral policy. [STATE](STATE.md) records progress and the next admission;
[BASELINE](BASELINE.md) records source, preservation and provenance;
[ACCEPTANCE](ACCEPTANCE.md) defines validation gates.

## Purpose and final ownership

Restructure the existing SeedV6 repository and retain its Git history. GUI and
UCI must consume one authoritative production engine. Preserve Play, Network
Training, Arena, standalone UCI, supported training and research capabilities.

| Final Gradle project | Ownership and allowed production dependencies |
| --- | --- |
| `engine-core` | Board, legal moves, rules, accepted search, evaluation/inference, TT and runtime engine services. No GUI, training or research dependency. |
| `engine-training` | Optimizers, checkpoint continuation, corpus access, self-play, validation and training services; depends on core. |
| `app-gui` | Desktop Play, Network Training, Arena, settings and presentation; consumes core and training. |
| `app-uci` | Lightweight standalone protocol application, options and process lifecycle; consumes core, without training or GUI runtime dependencies. |
| `research` | Experiments, independent references, verification tools, benchmarks and harnesses; may consume core/training. Production never depends on it. |

Shared compilation, version resources and packaging conventions belong in Gradle
build logic, not a sixth runtime project. Package boundaries may be refined by
dependency evidence. Temporary retention of the current `:app` during staged
migration is permitted; it is removed at cutover, not a sixth final owner.
Compile each production implementation once: no copied engine trees, duplicate
search/evaluator implementations, or independently maintained GUI/UCI forks.
Independent research oracles remain distinct and cannot be loaded as production.

## WU6 root verification composition amendment

Owner decision during WU6, 16 October 2026: "Approve root test/research composition only."
The root may own `integrationTest` and `researchLauncher` source sets and their
explicit project dependencies. This preserves mixed GUI/UCI comparisons against
independent research oracles and a small adapter for the historical research UCI
configuration. It is not another runtime project: no root main source set/runtime
JAR, no production distribution inclusion and no production-to-research edge.
Research remains limited to core/training. Only root integration configurations
may consume all existing owners; researchLauncher may consume core/research/UCI.
The boundary guard must reject arbitrary root configurations and production roots.

## Scope, authority and modification boundaries

Implementation belongs in `C:/projects/seed/java/seedv6-unified`, branch
`migration/unified-architecture`, starting at the verified revision in BASELINE.
The original SeedV6 checkout, its current branch and research are preserved.
The UCI Edition checkout is read-only, including its unresolved merge, index,
release artifacts and local model. Use immutable Git objects there for reference,
not its conflicted working files as an integration source.

Applicable `AGENTS.md`, the Search Contract and model-workspace compatibility
contract retain their scoped authority. Migration does not complete pending
SR-021 research reconciliation, change research conclusions, synchronize Project
Source mirrors, or modify the shared registry. This contract does not grant
perpetual implementation authorization: each admitted work unit is independently
reviewable. Work Unit 0 stops for GPT/user reconciliation before Work Unit 1.

Builds run from the migration worktree. Before any Gradle execution, use a
worktree-local Gradle user home, for example `build/migration/gradle-user-home`,
plus the worktree's normal project caches/output directories. Do not point build
outputs, cleanup, test stores, package destinations or temporary exports at either
historical checkout. Use small disposable stores for tests. Read frozen inputs
explicitly and check availability/hash; never mutate them or traverse/migrate
external training libraries as a setup ritual. Raw new research output follows
the existing ignored `raw/` convention. No large archives/corpora/build trees are
copied for this foundation.

## Production selection and invariants

The accepted search source is SeedV6
`35294146bb7e44975b516005b2cc196439666e09`, closed by
`aff8d744f115a73978fe469d3d02de2d436513f6` and subsequently accepted as recorded in
the SR-021 activation entry. Preserve cumulative Pair-2 SR-001/SR-029/SR-022/
SR-005/SR-036/SR-024: QXRAY, quiet LMR, WINDOW, EFFORT at one worker and
ROOT_REUSE at 2..16. Preserve all other supported evaluator/control behavior.
Neither latest HEAD nor an enabled flag is production acceptance authority.

Migration starts from newer main to retain GUI/training and research evidence.
Its SR-021 factories, `FutilityPolicy`, UCI experimental switch and research tasks
are not accepted production. SR-021 is deferred without adoption. Later extraction
must explicitly reconcile those additions against the accepted revision, keep
experimental capability under research ownership, and prove accepted behavior.
Do not roll back the whole application to the search revision or promote an
experiment by moving its package. No algorithm, calibration or strength tuning is
authorized by structural migration.

Preserve board/rule semantics, history and real-ply mate accounting; nominal TT
proof versus selective predictions; legal PV and completed-iteration publication;
whole-request budgets, worker-private mutable state, coherent shared TT, and
draining before reset/reuse/shutdown. Preserve one-worker and supported multiworker
operation. Require exact deterministic reference comparisons where their protocol
defines them; scheduling-dependent node counts, tied moves and PVs are not global
parallel equality requirements.

UCI Edition is an adaptation reference, not a second production engine to import.
Port justified boundary behavior (strict FEN, legal early-stop/draw fallback,
infinite/stop/EOF semantics, hash/worker options and native launcher packaging)
onto the shared engine with regression evidence. Retain the source provenance of
each adaptation and its distinction from a search algorithm change.

## Models and training compatibility

Pin generation 32 of lineage `235d0688-e060-44e3-8f1e-119c39bc8017`, payload
`network.brn-pair2`, SHA-256
`5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7`,
795,032 bytes. Preserve schema/compiler and
`BRN_PAIR2_MATERIAL_RESIDUAL_QUARTER_V1` calibration. BASELINE binds the full
checkpoint and optimizer state. Fail explicit model selection on missing,
incompatible or corrupt payloads; never silently substitute Best/latest/HCE.
GUI evaluator selection remains supported; a pinned UCI model must not override
unrelated GUI preferences. Common engine provenance does not mean every GUI
session is forced to use this model.

Ordinary Git is the preferred eventual storage for this small production payload,
but both repositories are public. Do not newly commit, publish or upload model
bytes without explicit owner authorization. Until then, use an explicit external
local input with checksum validation; packaging tests may use local inputs without
publishing them. Model publication is not a WU0/WU1 prerequisite.

Preserve model-workspace architecture/lineage/generation identity, checkpoint
codecs, Adam moments/step, partial-generation continuation, recipes, cursors,
promotion receipts, Best/latest separation, references, preferences and campaign
history. Runtime tables alone cannot reconstruct training state. No data migration,
optimizer reset or inferred historical identity is authorized. Preserve supported
NNUE/BRN families and their workflows; do not invent Pair-2 self-play support where
the existing contract requires corpus data. Compatibility changes must be additive,
idempotent, evidenced and reversible, or escalated before affected work proceeds.

## Packaging, version and evidence

Provide native Windows GUI/UCI and macOS GUI/UCI distributions, bundled matching
runtimes/licenses, relocatable launchers and explicit architecture/platform
identity. Validate complete app images and extracted archives outside the build
tree with paths containing spaces and without external Java. Native execution is
required on each supported platform; static inspection cannot certify it.

Keep `VERSION_STATE.txt` as SeedV6's existing version authority and use the local
finalizer under AGENTS.md. Structural/application changes are assessed per unit;
WU0 is documentation-only and must not bump. Builds/retries do not invent release
identity. Every future package records source commit/dirty state, version/build,
engine baseline and changes, model SHA-256, JAR/archive hashes, runtime and host
architecture. A displayed version alone is never an artifact identity. Preserve
both historical UCI v6 identities; do not overwrite, rename or delete either.
The first unified release name and historical repository retirement/publication
require later owner decisions. No push, release publication or history rewrite
is authorized here.

Preserve accepted, adverse, interrupted, negative and inconclusive research,
frozen inputs and original protocols. Existing ignored evidence stays local at
recorded paths; summaries/manifests remain in Git. Missing historical evidence
stays explicitly missing. No retroactive recreation or claim of verified Mac-only
work. Source attribution and artifact checksums do not certify runtime behavior.

## Stages and admission gates

| Unit | Dependency | Bounded deliverable | Gate before downstream work |
| --- | --- | --- | --- |
| WU0 Foundation | Completed discovery and this owner request | Isolated worktree; baseline/preservation record; contract, state and matrix | G0 evidence complete; GPT/user reconciliation before WU1. No source/build restructuring. |
| WU1 Gradle foundation | WU0 reconciled | Five target projects and shared build conventions; explicit acyclic dependency map and transitional `:app` ownership | G1: project/task discovery, wrapper/toolchain and existing build/test route work in isolation; no duplicated source ownership; no source algorithm migration. |
| WU2 Shared production core | G1 | Evidence-based engine/inference/runtime extraction, research seam accounting | E1-E6, R1-R3: deterministic accepted-source bridge, retained independent controls, worker/lifecycle checks; no GUI/training dependency in core. |
| WU3 Training ownership | WU2 core interfaces validated | Training/corpus/checkpoint/self-play/validation extraction | T1-T4: round trips, exact continuation and identities, compatible workflow/campaign tests; no user-store migration. |
| WU4 GUI ownership | WU2 and WU3 | Play, Training and Arena consume shared projects | G2-G5: automated regression and settings/persistence retained; owner-authorized WU4 structural boundary defers native Swing acceptance to WU7 before full runtime acceptance/cutover (see ACCEPTANCE). |
| WU5 Standalone UCI | WU2; reconcile any later shared-core changes | Adapt UE protocol/lifecycle onto the same core, without copied engine | U1-U4 and E5/E6: source-vector/process suites, option/clock/shutdown tests; later real-client acceptance recorded separately. |
| WU6 Research separation | WU2-WU5 ownership settled; preserve references throughout | Move remaining experimental/verification/test tooling into appropriate research/test ownership | R1-R3: independent references still runnable, frozen inputs preserved, no experimental runtime leakage; document task/path mappings. |
| WU7 Native packaging/integration | WU2-WU6 gates | Windows and macOS GUI/UCI package candidates | P1-P5 plus native GUI/client checks; model/signing/platform decisions only at their actual distribution dependency. |
| WU8 Cutover | All applicable gates and owner acceptance | Final five-project build, operator/reproduction docs, rollback evidence | Remove transitional app/duplicate wiring only after parity; explicit owner cutover decision. No automatic push/deploy/retirement. |

WU5 need not wait for GUI presentation work once its core dependency is stable;
the table is the default review order, not authorization for concurrent research
or automatic progression. Split a unit further if necessary for independent review.
Every accepted unit records actual tests, limits, files/commit, Git/version state,
remaining decisions and next gate in STATE. Contract amendments are explicit;
ordinary findings and measurements go in STATE/BASELINE or linked evidence.

## Human decisions, stopping and rollback

Stop affected work for contradictory identities/acceptance authority, unexplained
sensitive changes, overwrite risk, interference with active research, unavailable
required validation, or a choice changing the approved ownership/capabilities.
Preserve evidence and report the precise dependency. Do not resolve another
checkout's merge or destructively manipulate Git to proceed. Routine package
boundaries and reversible implementation details within these invariants may be
resolved from evidence.

Deferred decisions: public model bytes; Mac signing/notarization for public
distribution; Intel Mac support; direct historical Mac binary/local-work evidence;
first unified release naming; publication/retirement of historical repositories.
They do not block WU1. Model authorization gates public model inclusion;
native platform acceptance gates claims for that platform; signing/support policy
gates only releases making those claims.

Final cutover requires all matrix rows passed or explicitly scoped by the owner,
no unexplained capability loss, frozen engine/model/package provenance and a
tested rollback route. Keep historical source and artifacts accessible. Rollback
uses preserved artifacts or a separate worktree at a recorded accepted commit,
without resetting the original checkout, deleting evidence or downgrading user
stores. Any new persistence format needs backward compatibility or an authorized
backup/restore plan before use. A commit or completed Codex turn is not final
acceptance, deployment or permission to publish.
