# Migration acceptance matrix

Revision 3 (WU5 admission and application boundary), 16 October 2026. Apply with [CONTRACT](CONTRACT.md), immutable
[BASELINE](BASELINE.md) and dynamic [STATE](STATE.md). WU0 defines these gates;
it does not claim to have run application, client or platform acceptance.

Automated evidence must identify the command, source revision, configuration,
fixture/model hashes, result and limitations. Existing test names below are
starting points verified in the current tree, not a claim that their coverage
alone is sufficient. Preserve or deliberately remap their Gradle task/source-set
ownership. Record skips and absent payloads; a skipped required check is pending.
Native/human evidence identifies OS, architecture, runtime, package and scenario.

WU1 execution evidence is in [WU1-GRADLE](WU1-GRADLE.md) and
[wu1-evidence.json](wu1-evidence.json). Project/build-boundary and selected
regression checks pass. The owner explicitly authorized the WU1 commit with all
19 inherited GUI/checkpoint failures, reproduced with WU0, deferred for separate
remediation. The expanded full suite remains failed/incomplete. WU1 was subsequently accepted by GPT, as confirmed in the owner WU2 launch.
WU2 execution is recorded in [WU2-CORE](WU2-CORE.md) and
[wu2-evidence.json](wu2-evidence.json). The owner preserves the deferral of the 19
failures only while demonstrably inherited and unchanged; later GUI/training
acceptance requirements remain open. The owner's WU3 launch confirms GPT
acceptance of WU2 commit `09076c1d06ac8b532e065f4750bd0d8b206fe7db`.
R3 production routing/dependency checks do not claim WU6 completion: disabled
research seams remain explicitly accounted for in WU2.

WU3 execution is recorded in [WU3-TRAINING](WU3-TRAINING.md) and
[wu3-evidence.json](wu3-evidence.json). T1-T4 structural evidence includes cloned
historical checkpoint reconstruction, exact seven-family persisted optimizer
continuation, cursor/lineage/catalog identities, supported service/corpus tests and
the unchanged engine/model bridges. The nine inherited interrupted-archive restart
failures and ten GUI failures remain open; positive normal continuation does not
certify their failing paths. The owner's WU3 launch allows this bounded structural
gate when independent evidence is sufficient, with separate remediation before
dependent runtime acceptance. The owner subsequently confirmed WU3 acceptance in the WU4 launch; native
Swing/platform/client gates are not certified by those WU3 results.

WU4 owner clarification, 16 October 2026: WU3 is accepted. On resuming WU4,
the owner explicitly authorized structural completion and commit with sufficient
automated, non-interactive validation. Both Windows and macOS native G2-G5
walkthroughs are deferred to later user-performed acceptance under WU7 P1-P3.
They remain blocking for full GUI runtime acceptance and final production cutover,
not WU4 structural acceptance. This supersedes the earlier Mac-only deferral and
Windows walkthrough prerequisite. No other gate is waived. [WU4-GUI](WU4-GUI.md)
and [wu4-evidence.json](wu4-evidence.json) distinguish automated results, inherited
failures, the earlier limited Windows startup observation and unperformed scenarios.
No desktop interaction was performed during this resume. Scoped structural
validation is complete: 1,541 selected JUnit cases reconcile to 1,483 passed,
19 inherited failures and 39 skips; eleven boundary fixtures pass. Full GUI
runtime acceptance remains open and GPT/user reconciliation is pending.

The owner's WU5 launch confirms GPT acceptance of WU4 commit
`b412e34f35d5c1baccfe218f17d4c5a80a37a25c`. Earlier WU4 pending statements are
historical. WU5 establishes standalone Java UCI with core-only runtime dependencies
and explicit legacy compatibility composition; see [WU5-UCI](WU5-UCI.md).
Its required evidence is headless: no Computer Use or Swing windows. Native
Windows/macOS executables/runtime bundles, real Arena/BotLi clients, live games
and native signal behavior remain WU7 acceptance dependencies. WU5 headless evidence is complete:
1,472 selected JUnit cases (1,414 pass, 19 exact inherited failures, 39 skips),
159 standalone UCI searches, 66 legacy production requests, 203 exact SR-024 vectors,
seven-family persisted continuation and twelve Gradle boundary fixtures. See
[wu5-evidence.json](wu5-evidence.json). No new UCI/core regression remains; full-suite
or native/client acceptance is not inferred. WU4's native G2-G5
walkthrough deferral and the 19 inherited failures retain their existing gates.

| ID / stage | Preserved capability and acceptance condition | Automated validation / existing reference | Later native, client or human acceptance |
| --- | --- | --- | --- |
| G0 / WU0 | Independent worktree; unchanged originals and existing releases; correct production/research distinction; complete contract/state/gates; no runtime/build/model publication change | Git/worktree/ancestry checks; tracked-file and protected-artifact hashes; metadata inventory; frozen installation/model/fixture verification; documentation links and scope diff | GPT/user reconciliation of the foundation before WU1. No runtime acceptance implied. |
| G1 / WU1 | Five target projects, acyclic dependencies and shared build logic; transitional app remains usable; isolated outputs | Gradle projects/tasks/configuration and existing build/test entry points in isolated Gradle home; source ownership/classpath checks; no duplicate compilation or accidental packaging of research | Review dependency map and remaining extraction boundaries. No native package certification required for scaffold alone. |
| E1 / WU2 | Board/move-generation correctness: legal moves, make/unmake, castling, EP, promotion, check, repetition and draw precedence | Existing core/rules/perft tests, frozen perft cases, `SeeLegalOracle`, `SeeGeneratedLegalTest`, full legal enumerations; compare independent controls | None beyond required automated evidence; unexpected semantic differences block extraction. |
| E2 / WU2 | Accepted cumulative SR-024 search at each supported worker count; no SR-021 adoption | `ProductionSeeTest`, production driver tests, accepted-source probes and UE SR-024 vectors/tactical TSVs; static audit of selection and research seams; frozen source/JAR bridge with identical settings | Review any observed difference against accepted source; new search behavior requires separate authority. Historical strength results are not rerun merely for relocation. |
| E3 / WU2 | Evaluation/inference equivalence across retained HCE, NNUE and BRN families; pinned Pair-2 identity/calibration | Codec/inference and score-mapping tests, fresh-versus-incremental evaluation, private caches, invalid/truncated/wrong-hash failures; real pinned checkpoint probes when applicable | Select exact GUI models later under G5; no silent fallback or forced change of default evaluator. |
| E4 / WU2 | TT depth/generation/provenance, selective dependencies, mate score normalization and legal PV | `TTableTest`, `TranspositionScoresTest`, `TranspositionTableConcurrencyTest`, TT-off/exhaustive oracles, full/narrow-window sequences, terminal/draw/mate witnesses | None beyond automated evidence; selective predictions must not become ordinary nominal proof. |
| E5 / WU2, WU5 | Time management, complete-request node limits, cancellation and lifecycle ownership | `TimeManagerTest`, `SearchLifecycleServiceTest`, fake clocks and process tests: depth/movetime/clocks/increments/nodes, interrupted first iteration, reset/reuse, stop/quit/EOF, helper drain and failure recovery | Client interruption/responsiveness under U4; native timing sanity on each release platform. |
| E6 / WU2, WU5 | One-worker EFFORT and 2..16 ROOT_REUSE, worker-private state and coherent TT | Factory/policy checks across 1..16; actual work in helpers at representative 4/16 counts; legal bestmove/PV, budget accounting, transitions 1/4/1/6/2/16/1, cancellation/failure/reuse | No exact arbitrary parallel node/PV requirement. Portable thread setting behavior checked in GUI/native scenarios. |
| G2 / WU4 structural, WU7 native | Play: Human vs Engine, independently selected Engine vs Engine, legal moves, restart, side swap, live board and shutdown | `ChessFrameSmokeTest`, model-selection and controller/lifecycle tests, tiny model fixtures and EDT checks | Native Swing walkthrough on Windows/macOS, including both side bindings, exact snapshots, resize/scaling and close during search. |
| G3 / WU4 structural, WU7 native | Network Training: lineage/recipe/source/exposure/run controls and resumable stop | Training GUI and service tests, start/pause/resume/stop-after-generation, data picker worker cleanup and progress; no user-store writes in tests | Native start/stop/resume with disposable store and supported prepared data; readable progress/layout, storage chooser and errors. |
| G4 / WU4 structural, WU7 native | Arena: learning campaigns and match-only; Setup/Live/History, identities, W/D/L and unscored states | `ArenaSemanticsTest`, `ArenaHistoryViewTest`, `LearningArenaTest`, `MatchOnlyArenaTest`, recipe/configuration compatibility, same frozen tranches and independent participants | Native campaign and match-only walkthrough, exact generation choices, live board/progress, results/history and reopening a saved campaign. |
| G5 / WU4 structural, WU7 native | Evaluator selection, existing settings and persistence; common model library | `ModelLibraryTest`, checkpoint/selection tests, legacy/new preference round trips, Max thread setting, Best/latest/explicit generation separation, side swap preserves whole binding | Existing settings import/reopen using safe copied fixtures; Windows/macOS native file/folder pickers and no unexpected user-store relocation. |
| T1 / WU3 | Serialization/schema compatibility and exact model identities for supported families | `NnueCodecTest`, BRN codec/checkpoint suites, malformed/CRC/hash/family failures, Pair-2 saved-trainer recompilation and exact runtime tables | Real historical checkpoint load is required before declaring historical format compatibility; pin inputs and keep originals read-only. |
| T2 / WU3 | Optimizer state and exact continuation, including partial generations | Adam tests, `PartialGenerationStoreTest`, `GenerationRestartTest`, `CheckpointStoreTest`; uninterrupted versus resumed weights/moments/step, shuffle position, reserved ranges and cancellation boundary | Bounded disposable-store restart workflow; runtime-only model import must never imply resumable optimizer state. |
| T3 / WU3 | Lineage UUID, architecture/generation, Best/latest, promotions and history | Model library/retention/available-checkpoint tests; receipts, pointers, source ledger/cursors, annotations, legacy campaign fingerprints; missing provenance remains unknown | Review any storage-format change before using real stores. Additive migration only unless separately authorized. |
| T4 / WU3 | Reproducible supported training, corpus access, self-play and validation workflows | Fixed seeds/data slices/recipes and exposure manifests; supported self-play/validation services; Lichess/Seed/BINP adapters and independent cursors; data exhaustion explicit; NNUE expensive tests only when affected | Small representative supported workflows on target platforms. Do not demand exact scheduling-dependent games or invent Pair-2 self-play capability. Large training/corpus reruns need justified scope. |
| U1 / WU5 | Protocol and options: uci/isready/ucinewgame/position/go, Hash, Threads, Clear Hash, clean stdout and identity | Existing main UCI tests plus UE `tools/test_uci.py` and executable harness; malformed FEN/moves/options, readiness during search, safe allocation failure; preserve supported options or record explicit compatibility mapping | Real-client option discovery under U4. Lightweight UCI runtime must not pull GUI or training artifacts. |
| U2 / WU5 | Legal bestmove and truthful results at normal, terminal, draw and early-interruption roots | Full-history frozen vectors, legal PV, mate/stalemate/rule-draw and zero-budget cases; fallback does not fabricate depth/score/PV; terminal sentinel handled correctly | Client can complete legal games using the new launcher; no strength claim from compatibility testing. |
| U3 / WU5 | Clock controls, depth/nodes/movetime/infinite, stop/quit/EOF and supported signal shutdown | Process-level timeout/drain checks; clock/increment precedence and budget semantics; repeated searches, worker changes, interruption before depth one, EOF and exit during work | Native launcher tests under P1/P2; cancellation remains responsive without leaked helper processes. |
| U4 / WU5, WU7 | BotLi and Arena compatibility | Local configuration/launch validation and scripted UCI handshake/game harness; preserve reference Threads=1/Hash=64 and unsupported ponder distinction | Actual Arena client session on Windows and BotLi subprocess/configuration on supported host. No account messages, live online games or credential changes without explicit authority. Record unavailable clients as pending, not passed. |
| P1 / WU7 | Windows GUI and UCI distributions with bundled matching runtime | Native build, file/manifests/licenses, app-image and separately extracted ZIP; launch from paths with spaces without system Java; model/hash failures, occupied output refusal, prior release hashes unchanged | Native Windows Play/Training/Arena and UCI client smoke against those exact package hashes. |
| P2 / WU7 | macOS GUI and UCI distributions with bundled matching runtime | Run on native Mac: app/launcher permissions, architecture, runtime, manifest, image/extracted ZIP relocation, empty PATH, stop/EOF/signal behavior and preservation checks | Native macOS GUI/UCI acceptance. Apple Silicon is the evidenced historical target; Intel support is undecided. Signing/notarization policy decided before a public release making those claims. |
| P3 / WU7 | Relocatable packages and no developer-machine dependencies | Relocated image/archive launch with no system Java, no checkout/model-store path dependency; resources/native libraries/licenses included; no training/GUI dependency in UCI | Native filesystem and chooser behavior on both platforms; distinguish unsigned local validation from public distribution acceptance. |
| P4 / WU7 | Version identity and common engine/model provenance | Compare source/version/build/engine baseline, engine artifact, model hash, runtime/host and package manifest; GUI and UCI use same core source/artifact within a build; model identity reported when selected | Owner selects unified release name; no v6 collision or accidental overwrite. Model inclusion in public Git/release requires explicit owner authorization. |
| P5 / WU7, WU8 | Earlier releases/history preserved and rollback works | Pre/post hash inventory, output collision refusal; load/launch preserved release or clean accepted-revision worktree without destructive Git; persistence backward-compatibility tests | Owner cutover decision after exact candidate review; rollback must not overwrite user stores. Historical Mac package verification remains unavailable until actually inspected. |
| R1 / WU2, WU6 | Independent research references and retained verification capability | Run independent exact/TT-off, exhaustive qsearch/SEE/perft and BRN references after task remapping; source/path ownership audit; protect fixtures from regeneration by implementation under test | No new validation claim where local evidence is absent; preserve original references rather than replacing expected data with candidate output. |
| R2 / WU6 | Accepted research provenance, frozen/negative/interrupted evidence and reproducibility | Manifest/hash checks, tracked frozen opening inputs, immutable source and protocol references; documented old/new task/path mapping; ignored raw evidence remains at original locations | Known BRN historical evidence loss and unknown Mac-only work remain disclosed; renewed research requires its own contract/admission. |
| R3 / WU2, WU6 | Experimental isolation without losing research tools | Dependency/JAR-content checks: production has no research/verification dependency, no alternate engine selector; SR-021 stays non-adopted; benchmark/probe tooling still usable with explicit inputs | Review seam changes if extraction cannot preserve both accepted behavior and independent research capability. |

For deterministic single-worker or explicitly serial reference protocols, compare
the fields required by that protocol (scores, nodes/qnodes, depths, termination,
selectivity and PV). For arbitrary concurrent searches, validate legal outcomes,
resource accounting, policy availability and lifecycle invariants; do not demand
identical tied moves, node counts or PVs. Any numerical tolerance must come from
the existing evaluator contract or be justified before accepting a difference.

Historical reports of 800 broad tests, 188 real-model UCI requests, UE vector
counts and platform package tests are provenance, not WU0 execution results.
The preserved capabilities are accepted only when the relevant new-stage checks
run against the migrated source/artifacts. Automated checks support the owner-scoped WU4 structural gate; they do not
establish full native Swing, Mac or real-client acceptance. Deferred G2-G5 evidence
must identify the exact WU7 candidate/package hashes before those gates close. No browser verification is
needed for WU0; if later required, use only the repository-mandated Playwright MCP.
