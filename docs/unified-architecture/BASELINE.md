# Migration baseline and preservation inventory

Observed 16 October 2026 (Pacific/Auckland). Governing [contract](CONTRACT.md),
dynamic [state](STATE.md), and machine-readable [foundation evidence](foundation-evidence.json).
Paths below are observed locations, not portable build dependencies. No historical
artifact is copied, moved, rebuilt or modified by WU0.

## Repository identities and selected starting point

| Role | Verified identity | Interpretation |
| --- | --- | --- |
| Original SeedV6 | `C:/projects/seed/java/seedv6`; `main` at `24720213e12d99a581a713c74f901f89899bf2b4`; clean | Migration starts here to retain current GUI/training, research records and SR-021 closeout. This is not a new production acceptance. |
| Primary remote | `https://github.com/cohinteractive/SeedV6`; remote main `24720213e12d99a581a713c74f901f89899bf2b4` | Live `git ls-remote` agrees; original configured origin is the equivalent SSH URL. No fetch/push or remote ref modification. |
| Migration | `C:/projects/seed/java/seedv6-unified`; `migration/unified-architecture` | New sibling directory/branch, absent before creation; linked worktree of existing repository, not a new repository. |
| Historical UE local | `C:/projects/seed/java/seedv6-ue`; `main` at `c6bdd5249992d88cd35ed07b164cbb090afaa06e` | Pre-existing merge in progress; preserve files, index and metadata. |
| UE remote | `https://github.com/cohinteractive/seedv6-ue`; remote main `58511f45bf1bf0a45ca25f31da402a9e0ba56c50` | Live `git ls-remote` agrees; accepted integration `87b8b5b50acf3d2fe19793bfb0e8b24a45be114b` is in its locally verified ancestry. |

No additional parent/nested AGENTS.md was found for these edits. Root AGENTS.md,
the active Search Contract/Frontier, model-workspace contract/frontier and existing
architecture/build/storage documentation were inspected. Process inspection found
IDE Java services and Gradle daemons, no observed engine/training/research workload
that would be affected by sibling worktree creation; none were stopped. Only WU0's
own stalled optional version capture was stopped, as recorded in STATE.
Later preservation checks observed five ignored Gradle cache/report mtime changes
in the original checkout, with unchanged sizes; no task-authorship claim is made.
Tracked files, index/HEAD and protected artifact bytes remain the preservation
boundary verified in [STATE](STATE.md) and the evidence manifest. No Gradle build
was launched by WU0, and no source/research change is inferred from those caches.

## Accepted production and actual main-source differences

Accepted implementation: **`35294146bb7e44975b516005b2cc196439666e09`**.
Technical closeout: **`aff8d744f115a73978fe469d3d02de2d436513f6`**.
Both are ancestors of the migration start. Authoritative evidence:

- [Search Contract](../../source/CHESS_SEARCH_CONTRACT.md), currently P2-R037,
  retained production baseline and SR-024 section.
- [Search Frontier](../../source/CHESS_SEARCH_RESEARCH_FRONTIER.md), currently
  P2-F041: **SR-021 activation, 2026-10-15** explicitly confirms SR-024 owner
  acceptance, GPT reconciliation without remediation and both P2-R028/P2-F032
  mirror updates. Earlier pending statements in the unchanged SR-024 closeout
  are historical; current SR-021 reconciliation is a separate pending gate.
- [SR-024 report](../research/search/SR024_SEE_PRUNING_PHASE2_2026-10-14.md),
  [production manifest](../research/search/sr024-phase2-2026-10-14/production-v1-identities.json)
  and [integration observations](../research/search/sr024-phase2-2026-10-14/integration-uci-v1/integration.json).
- UE `RELEASES.md` at `87b8b5b50acf3d2fe19793bfb0e8b24a45be114b` records the
  separate owner's “accept and continue” for exact SR-024 UCI integration.

Selected Pair-2 behavior: QXRAY qcapture omission with promotion/EP/check/evasion
and geometric-clearance protections; quiet LMR and WINDOW; native EFFORT at one
worker, ROOT_REUSE at 2..16; private histories/evaluator scratch, honest proof,
mate and cancellation boundaries. Default worker count remains one. This does
not promote other research policies or replace other evaluator configurations.

The 377-entry production source/build/resource manifest matches 372 current
working files. The five changed existing paths are `app/build.gradle`,
`search/driver/ExactSearchAdapter.java`, `search/exact/ExactSearch.java`,
`search/exact/ParallelSearch.java`, and `uci/UciEvaluation.java` (Java paths under
`app/src/main/java/com/ohinteractive/seedv6/`). New `search/exact/FutilityPolicy.java`
is outside that earlier manifest. Inspection identifies explicit SR-021 research
factories, OFF-default experimental UCI configuration and associated task wiring;
`ProductionSearch` retains accepted QXRAY selection. No runtime equivalence is
newly claimed by this static inspection. WU2 must account for these seams rather
than treating main as a byte-identical accepted engine.

All 377 manifest entries were checked against the accepted Git revision: 376
match raw blob bytes; `gradlew.bat` uses working-tree CRLF versus Git LF and
matches after that identified checkout conversion. This is not a source drift.

## Frozen image and evaluator

Original image: `C:/projects/seed/java/seedv6/app/build/sr024-production-v1`.
All **15 installation files** match the production manifest. `lib/app.jar`:
2,252,659 bytes, SHA-256
`2341955302900ae1a15879d0cff6b14a3744498db0dd862b8cd2aabe7482ed0f`.
Embedded build is 35, source label `35294146bb7e (uncommitted changes)`; the
manifest distinguishes build-log bookkeeping from clean runtime sources. Preserve
both that label and the actual hashes; do not relabel or rebuild the reference.

The [measurement baseline](../research/search/BRE_PAIR2_MEASUREMENT_BASELINE_2026-10-09.md)
fixes evaluator identity; its older search-policy description is historical and
is superseded by cumulative SR-024 acceptance above. Store:
`E:/SeedV6-Networks/BRN-Pair2/235d0688-e060-44e3-8f1e-119c39bc8017`.
Checkpoint under `checkpoints/`:
`g000032-s001753088-5837c7f7192aa3cf9a265388f93649327455e804541398ae3ef6b69e09014304`.
All four files exist and their bytes were independently hashed in WU0:

| File | Bytes | SHA-256 |
| --- | ---: | --- |
| `network.brn-pair2` | 795,032 | `5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7` |
| `training.state` | 2,731,424 | `f8bdda5eda0ff7893ee36c2270baaedecaf50e14ff5202ab786996f72a2788d5` |
| `manifest.bin` | 449 | `a51513ddb6e23a8537cc38b8aeb7676af5723c3e3b6bb8151f265bb87ae83891` |
| `training-metadata.bin` | 191 | `135fb61ed736f313158e5514f2a16a81eda6526c4f034d2232ff26f8ee4176e7` |

Runtime schema `seedv6.brn.pair2` v1, family `BRN_PAIR2`; mapping
`BRN_PAIR2_MATERIAL_RESIDUAL_QUARTER_V1`, material + one quarter of learned
residual, symmetric rounding to 100 units/pawn. Preserve compiler/folding and mate
score domains. Latest training and saved Play selections are not this pinned
Best generation. Hash checks here do not rerun managed snapshot loading,
promotion-chain validation or trainer recompilation.

UE `models/baseline.brn-pair2` is also present, 795,032 bytes, with the same hash.
It is ignored by UE Git. Neither model copy is added to the migration worktree
or committed. Public model authorization remains deferred.

## UCI source and the historical v6 collision

Use immutable UE integration `87b8b5b50acf3d2fe19793bfb0e8b24a45be114b` and
remote tip `58511f45bf1bf0a45ca25f31da402a9e0ba56c50` for adaptation provenance.
Their README, RELEASES, source, `tools/test_uci.py`, source probes, frozen TSVs,
`tools/package-windows.ps1` and `tools/package_macos.py` remain accessible in Git.
WU0 does not port them.
The initial `src/main/resources/baseline.properties` still names source
`f22c76922d0ad22cb60f2403111db5fd0df0c38f`; it describes original model/engine
extraction, not the later cumulative release's sole search provenance.

Relevant UE adaptations include standalone immutable model loading, Hash/Threads
and Clear Hash, strict FEN validation, legal fallback for interrupted/drawn roots,
infinite analysis waiting for stop, EOF shutdown and lightweight native packaging.
Completed search semantics are pinned separately. Retain 366 prior frozen source
observations and SR-024's 203 native/serial-root observations. At remote tip:

| Fixture | SHA-256 |
| --- | --- |
| `src/test/resources/sr024-vectors.tsv` | `0a10bc7de11506fca7945e236caa8badffab4abbcee975fd1b72583e47dfa3e7` |
| `src/test/resources/sr024-tactical.tsv` | `bb839d60e62dc2f72d69ad45a421905fdb59f61676e70c937c8861a70320524e` |

| Historical identity | Evidence and availability |
| --- | --- |
| Windows `SeedV6-UE v6`, SR-001/029/022/005/036 | Local commit `c6bdd5249992d88cd35ed07b164cbb090afaa06e`; accepted upstream SR-036 `b73a646d4232058f1b927beac52418839cd64520`. Existing `releases/SeedV6-UE-v6-windows-x64.zip`, 15,830,176 bytes, SHA-256 `dd4dfd81cdbec3e87590f53bb97005f7970a182899b2b61673db238d3b9aa3b8`. Embedded manifest and release properties inspected directly inside ZIP, without extraction; clean source identity matches. |
| Windows v6 application JAR inside ZIP | `SeedV6-UE-v6/app/seedv6-ue-1.0.0.jar`, 202,534 bytes, SHA-256 `f073f8fd85dd6b9628f981ea6525b865696a12394c97b1ee53def3bde464a212`; embedded properties retain SR-036, no SR-024. |
| Windows unpacked v6 image | Historical record names `releases/v6/SeedV6-UE-v6.exe`, but `releases/v6` is absent at WU0 inspection. Do not claim it preserved or infer why it is absent. Existing ZIP preserves the package identity; no recreation or cleanup is needed for WU1. |
| macOS arm64 `SeedV6-UE v6`, SR-024 | Remote RELEASES records integration `87b8b5b50acf3d2fe19793bfb0e8b24a45be114b`, upstream `35294146bb7e44975b516005b2cc196439666e09`; package `/Users/charlesclark/projects/seed/java/seedv6-ue/releases/macos-arm64/v6/SeedV6-UE-v6`, ZIP `releases/SeedV6-UE-v6-macos-arm64.zip`. Reported JAR SHA-256 `4379603882b01eeae601fd6d5726f2abfb90ba5f2298647e17a42c603bdac83f`; reported ZIP 17,909,742 bytes, SHA-256 `72e4f297a6ccff6e99cd19323076c24155e8c8f336f75afa36cc9b08a4956625`. |

Mac binary, permissions, local checkout and uncommitted Mac-only work were **not
inspected**. The Mac hashes/runtime claims above are recorded evidence, not fresh
verification. Equal displayed v6 does not imply equal search, JAR or platform.
Keep both records and existing artifacts unchanged; do not resolve the collision
by overwriting/renaming either release.

UE's unresolved merge has HEAD `c6bdd524...`, MERGE_HEAD `58511f45...`, three
unmerged files (`README.md`, `RELEASES.md`, `release.properties`), and 13 staged
non-conflict paths. Index stages and merge metadata are captured in the evidence
record. Do not resolve/abort/continue it, reset, stash, commit in UE, or use its
working release properties as authority.

## Preservation and storage boundaries

| Evidence/capability | Preserve and use |
| --- | --- |
| Historical repositories | Both local repositories and both verified remotes; SeedV6 history is the migration history. Keep UE source/acceptance/release lineage accessible after eventual unified cutover; retirement is a separate decision. |
| Search acceptance | `source/CHESS_SEARCH_CONTRACT.md`, `source/CHESS_SEARCH_RESEARCH_FRONTIER.md`, linked cumulative reports and manifests under `docs/research/search/`; SR-024 confirmations, adverse witnesses and interrupted attempts; SR-021 deferred evidence unchanged. |
| Frozen Phase 1 | `docs/search-phase1/CHESS_SEARCH_CONTRACT.md` SHA-256 `9f820c2e9efd366b179902d3fc14bdd8f88e6ae793356aded6b6119f73e8401b`; Frontier SHA-256 `ead3743400b1dc513e02ad1c776b39db1486e8800c947cece9e20ba199e8fc1a`; both rehashed and matched. |
| Independent references | `app/src/test/.../search/exact/ExhaustiveOracle.java`, `QuiescenceOracle.java`, `Sr006ModelOracle.java`, `core/SeeLegalOracle.java`, flat/TT-off controls, frozen source vectors and perft cases; verification `FlatExactSearch`, `FlatNegamax`, `QuiescenceSearch`, `ExactSearchHarness`, BRN native reference. Keep algorithms independent and test/research-only. |
| Frozen fixtures | Existing tracked `app/src/test/resources`, frozen opening PGNs and selection/protocol manifests in search evidence directories, including SR-024 and SR-021. Do not conflate opening inputs with ignored tournament outputs. UE TSV fixtures remain referenced by immutable revision until a later attributable port. |
| Production models | Full pinned checkpoint above and existing external model-library lineages. Ordinary Git eventual preference applies only after owner authorization; references/hashes now. |
| Continuation contracts | [Model workspace contract](../model-workspace/CONTRACT.md), [frontier](../model-workspace/FRONTIER.md), [Pair-2 guide](../brn/BRN_PAIR2.md), [training data](../corpus-training.md), [Arena guide](../learning-arena.md); preserve all supported family codecs, optimizer bytes, UUIDs, partial generations, recipes, source cursors, receipts and campaign resumes. Model workspace implementation completion is recorded; human acceptance is not inferred. |
| Earlier packages | Original SeedV6 `dist/windows`, `dist/mac` where present; UE `releases/` and `dist/` where present, including older v1-v5 and v6 ZIP. WU0 compares all existing files in these directories by hash; it does not infer absent platform packages exist. |
| External data/raw research | `E:/SeedV6-Corpus` registrations and source descriptors, external model library, ignored `app/build` and `docs/research` output at original recorded paths. Check availability before dependent analysis; no full corpus scan, move, upload or force-add. Fresh worktree lacks ignored evidence. |
| Known prior evidence loss | [BRN architecture STATE](../research/brn-architecture-cglhw/STATE.md) records owner deletion of historical `app/build/research/` content on 2026-10-13. Retained reports/manifests remain; unavailable weights, continuation states, datasets and raw results are not invented or recertified. This does not negate the separately present production checkpoint. |
| Unresolved/local state | UE merge/index and conflicted files, original SeedV6 research/output and current branch; Mac local state remains unknown. Worktree metadata/new branch are the only intended changes to the original repository administration. |

Existing accepted research conclusions and limitations remain authoritative within
their original experiment. WU0 performs identity/preservation checks, not a new
strength experiment or broad certification of old measurements. New source layout
must retain reproducible commands or explicit path/task mappings and must record
which external evidence is absent before declaring a future gate passed.
