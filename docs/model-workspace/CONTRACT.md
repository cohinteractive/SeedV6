# Model workspace CGLHW contract

Revision 1, 2026-10-05 (Pacific/Auckland). SeedV6-local product workstream,
authorized by the owner's attached request to establish and autonomously execute
the model/training/play/arena architecture and UX reorganization. This is not
shared behavioral governance. [FRONTIER](FRONTIER.md) is the dynamic execution
record; [DISCOVERY](DISCOVERY.md) records repository evidence and design challenges.

## Objective and ownership

Users work with Architecture -> named Model Lineage -> immutable Generation,
reusable prepared data, training recipes, match/validation protocols and campaigns.
Paths remain operational details. Build on the existing production implementation,
not an invented replacement. No giant mutable engine object owns all concerns.

| Owner | Required meaning |
| --- | --- |
| Architecture | Existing evaluator/trainer/schema capabilities and sensible defaults; preserve NNUE material-parity versus legacy distinction and all BRN versions. |
| Lineage | Stable identity separate from name/path, architecture, provenance/initialization seed, defaults and useful configuration history. Managed new placement, safe external adoption. |
| Generation | Immutable exact checkpoint, parent, actual recipe/exposure where recorded, Best/Latest status and mutable user notes/tags stored separately. Unknown old metadata stays unknown. |
| Recipe | Explicit learning rate, minibatch, epochs and legitimate architecture parameters, with lineage defaults and actual generation provenance. |
| Training run | Source selection, exposure, permitted overrides and explicit termination policy. |
| Match/validation protocol | Search budget, bounded portable threads, openings/seeds, pairs/colour reversal, ply cap, validation and promotion rules. Search depth is not training depth. |
| Campaign | Shared experimental conditions, same actual frozen tranche/targets/exposure by default, architecture-specific competitor recipes, durable orchestration and history. |

## Product acceptance

All rows need implementation and proportionate evidence, not merely a renamed UI.

| Gate | Required outcome |
| --- | --- |
| Library | User-configurable root; automatic architecture/UUID placement for new named lineages; existing external stores safely usable without relocation. |
| Identity/browser | Common domain and selection semantics across Play, Training, validation and Arena; architecture/name/generation visible; Best, Latest, counts and useful evidence; generation annotations; no fabricated historical provenance. |
| Play | Library-based Human vs Engine and independent White/Black Engine vs Engine selections across architectures; Swap Sides preserves whole bindings and exact chosen snapshots. |
| Training | Identity/provenance, recipe, source/exposure, validation/match and termination have distinct domain ownership and clear presentation. Configurable LR with current defaults; seeds that initialize a lineage are not misleading mutable run knobs. |
| Threads | Auto/Max is persisted semantically and resolved per machine; normal explicit choices bounded by available/supported CPUs. Existing low-level API behavior is preserved unless change is necessary and justified. |
| Termination | Explicit Unlimited, N generations or time budget. Time budget finishes the admitted coherent generation; immediate pause/cancellation remains separate and resumable. Preserve legacy combined bounds explicitly where needed. |
| Data | Reuse DataSource/DataSources/readers/cache; add/register/prepare once, select in compatible consumers. Preserve each lineage's cursor/mix and each campaign's independent traversal. Lichess/BT4 capability errors explicit; no repeat path browsing. |
| Arena setup | Shared campaign protocol versus competitor recipes; same frozen records and targets, exposure, epochs, match/opening conditions by default. Support existing learning campaign; shared model/match concepts can accommodate fixed comparisons without forcing inappropriate unification. |
| Arena live | Setup/Live/History or equivalent; established board with exact White/Black identities and match state; reusable rich training progress with optimizer/exposure/loss information. |
| Arena results | Actual competitor/lineage/generation identities, per-round W/D/L, score and winner. Symmetric 100% stacked vertical score bars, rounds horizontally, visible 50% parity line; unscored rounds stay unscored. |
| Compatibility | Existing checkpoints, optimizer bytes, lineages, pointers, receipts, source registrations/cursors, preferences, selections and campaign history/resume remain supported; additive, idempotent adapters with unknown states for missing evidence. |
| Integration | Focused and final integration checks pass; appropriate native Swing runtime/layout evidence. Superseded duplicate/path-first flows removed only after replacements proven. No known in-scope defect silently abandoned. |

## Invariants and decisions

- Reuse TrainingLineage UUIDs, TrainingFolders root/adoption preferences and
  CheckpointStore codecs/ownership. No second independent model registry.
- Arena uses publish-only stores, intentionally without Best. Browsing/loading
  explicit snapshots must not require Best or create promotion evidence.
- Do not mutate, relocate, rewrite, prune or enumerate full external payloads as a
  migration ritual. Registration and optional sidecar metadata are additive;
  actual training retains existing publication/retention semantics. Browsing is
  advisory; exact loading still verifies payload identity and handles pruning.
- Keep search/evaluator/training mathematics, calibration, promotion logic, legal
  moves and node-critical paths unchanged except an explicitly justified necessary
  requirement. Configurable LR changes a requested parameter, not optimizer math.
- Existing campaign identity binds serialized configuration and recipe strings.
  Additive fields must not invalidate historical binding or optimizer continuation.
- Store initialization seeds separately from run/shuffle/opening semantics. Do not
  reconstruct a historical seed or timestamp from path, generation or filesystem time.
- Treat run time-budget boundary change as an explicit requested behavior change.
  Preserve Stop Now and exact resumable cancellation separately.
- No pushes/deployments or destructive Git operations. Preserve inherited work;
  local coherent commits permitted when attribution and validation are clear.

## Autonomous execution and stop conditions

Inspect -> choose highest-value admissible frontier item -> implement a coherent
unit -> focused validation -> remediate -> update evidence/frontier/resume point ->
continue without routine approval. Revise this stable contract deliberately;
ordinary discoveries belong in the frontier. Naming, reversible layout, schema,
compatibility adapters, test design and sequencing are autonomous.

Stop affected work only for material destructive/irreversible external-data changes,
unavoidable loss/compatibility tradeoffs, conflict with a necessary product invariant,
unresolvable authority/product decision, unavailable required resources/permissions,
required validation that cannot be performed, or material unauthorized engine/math
changes. Record evidence and the blocking action before escalation. Do not turn
routine uncertainty or non-critical version warnings into blockers.

## Validation and completion

Test affected persistence round trips, legacy loading, exact identities, independent
participants, source compatibility, resume/cancellation and EDT ownership. Use tiny
synthetic stores/campaigns; no expensive long training, strength experiment or whole
suite after every unit. Broaden near integration based on change surface. Native
Swing component/window harnesses are appropriate here; this is not a browser app.
If browser work becomes required, only the mandated Playwright MCP is permitted.

Record tests actually run, results and omitted checks. Static inspection is not
runtime evidence. Fresh sessions must read these records, inspect Git and resume
the next admissible item. A completed turn/commit is not whole-workstream acceptance
or deployment. Completion requires all gates satisfied or a legitimate documented
out-of-scope/deferred decision or genuine human gate, and honest final Git/version,
risks and human-action reporting. No acceptance is inferred from touching frontier rows.
