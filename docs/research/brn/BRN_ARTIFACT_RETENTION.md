# BRN artifact retention and proposed cleanup

Inventory and recommendation: 2026-10-08 (Pacific/Auckland), checkout `791814d`.
**Review only: nothing was copied, moved, renamed or deleted.** This guide serves
the owner's knowledge-preservation objective, not preservation of every resumable
experiment. Read the [completed research summary](BRN_COMPLETED_RESEARCH.md) first.

## Decision and non-overlapping accounting

Retain the tracked knowledge and two final C02 models with their loading metadata.
A small optional archive supports selected continuation/calibration work. The rest
of the named BRN directories can subsequently be retired after review and verified
preservation of the exceptions. **Do not delete `app/build/`, `build/`, or
`app/build/research/` wholesale on the basis of this report.** Their status differs.

Sizes are current logical file bytes; GiB = bytes / 2^30, MiB = bytes / 2^20.
The inventory found 23,257 files across the two build roots, no reparse points and
no multiple-hardlink files. Filesystem allocation/compression and later changes
can alter actual reclaimed disk space. There is no parent/child double counting.

| Mutually exclusive class within the two build roots | Bytes | GiB | Recommendation |
| --- | ---: | ---: | --- |
| B: essential selected-function package | 888,860 | 0.000828 | Preserve the four exact files below |
| C: specifically enumerated optional archive | 143,937,971 | 0.134053 | Keep only if its stated capabilities are wanted |
| D: summary-backed deletion candidates, excluding B and C | 70,234,333,134 | 65.410820 | Eligible for later owner-reviewed cleanup |
| E: other/mixed artifacts not cleared by this BRN review | 1,833,527,745 | 1.707606 | No deletion recommendation |
| **Total `app/build/` plus `build/`** | **72,212,687,710** | **67.253306** | Snapshot, not total repository/storage usage |

`app/build/` contributes 68,816,884,744 bytes; `build/` contributes 3,395,802,966.
The named BRN scope is B+C+D = 70,379,159,965 bytes (65.545700 GiB).
Keeping B and all C permits **65.410820 GiB** recovery; declining C permits
**65.544873 GiB**. Keeping only B leaves 884,248 bytes of executable model payloads
plus 4,612 bytes of metadata (0.848 MiB total). Keeping B+C costs 144,826,831 bytes
(138.118 MiB). These totals exclude retained tracked documentation and external
live stores/corpora. No copies have yet secured B or C outside the deletion targets.

## A. Essential knowledge

Keep the tracked `docs/brn/`, `docs/research/brn/` and
`docs/research/brn-architecture-cglhw/` records, including their existing compact
JSON/TSV evidence and reproduction sources. They total about 31.58 MiB before
this consolidation; Markdown alone about 1.80 MiB. They are outside this cleanup
scope. The [summary](BRN_COMPLETED_RESEARCH.md) maps their coverage and adds the
previously build-only BRN-2 GUI/headless performance findings.

Keep application, verification and research-tool source in Git. Historical plans
contain original build-tree paths/hashes as provenance; those identifiers need not
resolve after retirement to remain useful records. A hash does not recreate its
file. Detailed raw reanalysis/reproduction will require the relevant optional or
separately archived inputs, beyond the minimal package.

## B. Essential binary retention candidates

These two final selected models retain both evidenced source/order outcomes for
less than 1 MiB. Neither run dominates sufficiently to discard the other merely
to save 442 KB. No other build-tree weights are required by the application's
current default or inspected saved model selections.

| Exact full path | Bytes | SHA-256 |
| --- | ---: | --- |
| `C:/projects/seed/java/seedv6/app/build/research/brn-architecture/z01-scaling-compiled-compute-600-211/selected.model` | 442,124 | `c86c4bc006e7503153a71df669e6c68c4c6de9e82f99db3e30ceccb8580588af` |
| `C:/projects/seed/java/seedv6/app/build/research/brn-architecture/z01-scaling-compiled-compute-600-211/result.json` | 2,306 | `3b2e5470a4d78816d75e49607eded1dd57393e6c9047cf94d770ab1f41916c79` |
| `C:/projects/seed/java/seedv6/app/build/research/brn-architecture/z01-scaling-compiled-compute-600-337/selected.model` | 442,124 | `f4baf07b4c678f0cd7c556790a9a7a200bd33bcd61a7ecc0f55748ace33005c0` |
| `C:/projects/seed/java/seedv6/app/build/research/brn-architecture/z01-scaling-compiled-compute-600-337/result.json` | 2,306 | `cd49415841ed12b9bd763a9b5f3ac8a9f874c316a120616638cf239e3121d9e7` |

Despite the `compiled-compute-600` directory names, these are unchanged research
tuple-model bytes selected from the 300-second `endpoint-002` on each run.
`BrnArchitectureControls.loadView` reads `result.json`, checks complete/hash/parity
identity, reads `selected.model` and compiles the tables in memory. It does not
require the historical `sourceRun` directory just to load this function. Thus both
files per directory belong in B. Metadata SHA-256 agrees with the final match plan.

This package supports the retained research loader and future deliberate conversion
work; it is **not** a ready-made application lineage, optimizer checkpoint or
automatic promotion. Current `BrnPair2Codec` uses a different compiled format.
The explicit `pair2-parity` developer command in [BRN_PAIR2](../../brn/BRN_PAIR2.md)
also requires C1 datasets. Normal application training/tests do not require those
research datasets or either historical model. No preservation destination/import
has been created or selected by this task.

## C. Optional archival assets

The following is one deliberately bounded optional package, not a requirement to
keep all unique/expensive files. Paths in this table are exact repository-relative
identifiers under **`C:/projects/seed/java/seedv6/`**. Directory entries explicitly
mean their complete current contents; filename lists mean only those files.

| ID | Exact paths | Bytes | Continuing value and limits |
| --- | --- | ---: | --- |
| C1 | Complete `app/build/research/brn-architecture/z01-data-a/` and `z01-data-b/` | 37,502,022 | Six files total: each `positions.bin`, `manifest.json`, and `seek/d8e423b9c6ab7ca56b3383d52d09d0a3c36611c6e42f517316a2cbe448c58842.json`. Enables final-model parity and new analyses on these fixed populations. Tests are already opened. |
| C2 | `app/build/research/brn-architecture/z01-long-tuple2-211/endpoint-002/selected.state` and `result.json`; same two filenames under `z01-long-tuple2-337/endpoint-002/` | 5,469,552 | Selected unfused weights/moments plus exact shuffle cursor/step metadata. With C1, B and retained research code, supports selected-endpoint optimizer continuation. Does not preserve every previous/later trajectory or application-store lifecycle. |
| C3 | `app/build/research/brn-programme/r4-typed-eight-71/production-preview/network.brn3`; `app/build/research/brn-programme/r4-typed-replication-97/production-preview/network.brn3` | 18,948,704 | The two original V1 inference models. Optional native BRN comparisons; optimizer continuation is deliberately not retained here. |
| C4 | Complete `app/build/research/brn-learning/windows-lineage-v2/g9/` | 72,541,524 | Exactly `network.brn3` (9,474,352), `training.state` (63,066,732), `manifest.bin` (440). Mechanism/calibration reference and prospective optimizer continuation on separately supplied data. This three-file export is not a complete normal lineage or exact replay package. |
| C5 | `app/build/research/brn-architecture/e002-native-brn-old/selected.model` and `result.json` | 9,476,169 | Frozen native BRN practical control from the final comparison; its historical training exposure is unknown. Not a claim that it equals the user's current Best. |
| **C total** | **17 files, disjoint from B** | **143,937,971** | **137.270 MiB; none required for reading the findings** |

Verified SHA-256 anchors for the optional binaries:

| Asset | SHA-256 |
| --- | --- |
| C1 A / B `positions.bin` | `d6d6a11cbd22aa70c48a206bd8881d2d302acb68e53bda6554459d739406c51d` / `40f0bb8ba4548bd293a4a010807e6a3b9a259bc75c150d55f5c65d3c7c4a631f` |
| C2 seed211 / seed337 `selected.state` | `85d63c687bec44fc2274b4f116b5cfd6e0b06d291c6a861e7921c1d83fbddb16` / `80af2293a9223062cc066c926dc3260f53b229361c7e33a7b715891c5ef027ab` |
| C3 seed71 / seed97 `network.brn3` | `340696c96da0affd624addc236e536942d03dd53f2216884b4892d1cd2e058fd` / `9b9d56bfeef25c68090e0819e2abe384be3a6b3d5ef598bf279fcac6ffcc764d` |
| C4 Gen9 model / training state | `6751619a7043264048c536144afea2b8921ea6eeaf0dc643b7b2ce4c40e64c57` / `90ee0f24d12164dfcb46bb1d675137d26756b44403b3cacc9fa014c3605cb26f` |
| C5 native model | `8a5d6ce4648d711c7005cb534f320f4a5aca03e8f73458b077cf82a9e8672222` |

C2 metadata binds selected exposure/step: 26,161,792 / 204,389 for seed211 and
24,971,392 / 195,089 for seed337. Its `selected.model` files are byte-identical
to B; retaining additional copies is unnecessary. If historical tooling expects
the original endpoint paths, those exact model bytes would have to be restored
there deliberately. No such restoration is performed or implied here.

The entire old `brn-learning/windows-lineage/` has 27 files / 356,508,302 bytes
that SHA-256-match the corresponding `windows-lineage-v2/` files. V2 adds
`provenance.json`; keep its already tracked evidence copy. This is a verified
duplicate subset, not an assumption that all checkpoints in the research tree
are duplicates. Other intermediate models may be unique and still nonessential.

## D. Directory-level candidates and preservation exceptions

These are proposed **future targets**, not deletion instructions for this turn.
“Whole” means no B/C exception was selected in that directory. “Mixed” means the
listed B/C files must first be preserved or, for C only, explicitly declined.
All paths below are full paths. Rows within each table are disjoint.

| Research target | Current bytes (GiB) | Disposition / retained knowledge |
| --- | ---: | --- |
| `C:/projects/seed/java/seedv6/app/build/research/brn-architecture/` | 60,631,355,599 (56.467350) | **Mixed: B, C1, C2, C5.** All other families, intermediate/last/gen0/continuation-test checkpoints, optimizer histories, datasets and raw receipts/traces are D. Final methods/results/dispositions remain in the architecture canon. |
| `C:/projects/seed/java/seedv6/app/build/research/brn-programme/` | 2,372,874,252 (2.209911) | **Mixed: C3.** R0–R4 variants, superseded states, old datasets and service-smoke stores are D; V1/ledger/compact evidence preserve their findings. |
| `C:/projects/seed/java/seedv6/app/build/research/brn-successor/` | 1,571,231,023 (1.463323) | **Whole.** No promoted new architecture/model; retain findings and R01 source. `.successor` training snapshots are optional historical resumption, not application runtime dependencies. |
| `C:/projects/seed/java/seedv6/app/build/research/brn-learning/` | 939,586,201 (0.875058) | **Mixed: C4.** Duplicate copied lineages, other generations/states, continuation endpoints, prospective slices and per-ply traces are D. Learning result/evidence preserve both successful and failed-cap results. |
| `C:/projects/seed/java/seedv6/app/build/research/brn-throughput/` | 358,595,451 (0.333968) | **Whole.** Profiles, rejected prototypes, scratch runtime/JAR/native bundles and loading outputs; retained source/benchmarks preserve implementation and exact-state findings. |
| `C:/projects/seed/java/seedv6/app/build/research/brn-learning-evidence-preview/` | 849,389 (0.000791) | **Whole.** Intermediate evidence-export staging, superseded by tracked final evidence. |
| `C:/projects/seed/java/seedv6/app/build/research/brn-learning-evidence-preview-v2/` | 881,825 (0.000821) | **Whole.** Same role. |
| **Research subtree total** | **65,875,373,740 (61.351223)** | Includes B+C; do not add this total to its rows. |

Within `brn-architecture`, `.state` files alone occupy 47,797,291,382 bytes (1,136
files), `.model` files 12,295,557,088 (1,601), and JSON 465,504,109 (9,181).
The programme has 2,090,767,623 bytes of `.state`; successor has 1,404,995,496 bytes
of `.successor`; learning has 630,667,320 bytes of `.state`. These are explanatory
subtotals already included above, not extra recoverable space. Markdown preserves
their scientific findings, not their complete numerical states.

Additional BRN targets outside `research/` are all **whole-directory candidates**,
except the explicitly named `.tar` file. They contain historical scratch stores,
models/optimizer copies, benchmark/profile data, rendered/test evidence or source
snapshots used by historical tooling. None is an inspected active user store.

| Full target | Bytes | Knowledge retained in |
| --- | ---: | --- |
| `C:/projects/seed/java/seedv6/app/build/brn-bootstrap-smoke-final/` | 291,505,586 | Bootstrap/remediation records |
| `C:/projects/seed/java/seedv6/app/build/brn2-canonical-bootstrap-20260922/` | 291,507,972 | Bootstrap/canonical diagnostics |
| `C:/projects/seed/java/seedv6/app/build/brn-remediation/` | 109,798,701 | Remediation record, including incomplete performance outcome |
| `C:/projects/seed/java/seedv6/app/build/brn2-benchmark/` | 9,133 | Canonical/diagnostic records |
| `C:/projects/seed/java/seedv6/app/build/brn2-blended-integration/` | 835,702 | Supervision-training record |
| `C:/projects/seed/java/seedv6/app/build/brn2-canonical-evidence/` | 273,531 | Canonical diagnostics |
| `C:/projects/seed/java/seedv6/app/build/brn2-canonical-g128/` | 18,050,366 | Canonical g128 tables |
| `C:/projects/seed/java/seedv6/app/build/brn2-fresh-75-diagnostics/` | 5,298,443 | Fresh-75 diagnostics |
| `C:/projects/seed/java/seedv6/app/build/brn2-frozen-wdl-analysis/` | 20,332,209 | Handcrafted-generation research |
| `C:/projects/seed/java/seedv6/app/build/brn2-frozen-wdl-preflight/` | 147,249,172 | Handcrafted-generation research |
| `C:/projects/seed/java/seedv6/app/build/brn2-handcrafted/` | 2,231,225 | Handcrafted-generation research |
| `C:/projects/seed/java/seedv6/app/build/brn2-handcrafted-75-preflight/` | 106,697,098 | Handcrafted-generation research |
| `C:/projects/seed/java/seedv6/app/build/brn2-handcrafted-postcampaign/` | 32,739,645 | Handcrafted-generation research |
| `C:/projects/seed/java/seedv6/app/build/brn2-handcrafted-wdl-preflight/` | 54,807,990 | Handcrafted-generation research |
| `C:/projects/seed/java/seedv6/app/build/brn2-independent-75-preflight/` | 2,134,046 | Independent-75 preflight |
| `C:/projects/seed/java/seedv6/app/build/brn2-strength-screen-20260923/` | 27,944,359 | Supervision strength screen |
| `C:/projects/seed/java/seedv6/app/build/brn2-supervision-ablation-20260923/` | 13,258,285 | Supervision ablation |
| `C:/projects/seed/java/seedv6/app/build/brn2-supervision-weight-sweep-20260923/` | 23,470,921 | Supervision weight sweep |
| `C:/projects/seed/java/seedv6/app/build/brn2-training-analysis-20260922/` | 4,647,591 | Canonical/diagnostic records |
| `C:/projects/seed/java/seedv6/app/build/pair2-integration/` | 73,582 | Pair-2 operator guide integration evidence |
| `C:/projects/seed/java/seedv6/build/brn-diagnostics/` | 3,306,227,762 | Cross-host method plus new summary's GUI/headless measurements and unresolved pause attribution |
| `C:/projects/seed/java/seedv6/build/brn-diagnostic-baseline/` | 30,547,145 | Historical source/test baseline, not a live dependency; methods and conclusions retained |
| `C:/projects/seed/java/seedv6/build/brn-diagnostic-baseline.tar` | 11,673,600 | Historical baseline archive; no exact-byte duplication with the expanded directory is asserted |
| `C:/projects/seed/java/seedv6/build/brn-full-suite-report/` | 1,207,993 | Historical validation reporting |
| `C:/projects/seed/java/seedv6/build/brn-full-suite-results/` | 1,023,321 | Historical validation reporting |
| `C:/projects/seed/java/seedv6/build/brn-tests-initial-results/` | 240,847 | Historical failures/results remain described in research reports |

`build/brn-diagnostics/` consists of GUI-bootstrap 1,817,753,654 bytes,
cross-host PC 1,264,592,762, training smoke 197,516,361, replay smoke 13,192,050
and legacy smoke 13,172,935. Full model/store copies, optimizer states, JSONL and
JFR permit reanalysis but are not needed to preserve the measured conclusions.
The older blocked GUI attempt is retained in history; it was not a successful run.

Declining historical raw archives loses independent re-auditing of every old root,
move, allocation/profile and training example. There is no claim that all raw
information was copied to Markdown. The rationale is that the relevant research
knowledge is adequately represented, not that every original byte is redundant.

## E. Dependencies, holds and precautions

Read-only consumer checks found no hard-coded input requirement for these research
trees in `app/src/main` or the build configuration. Current application codecs,
training and model selection load explicit user stores; ordinary tests use scratch
fixtures. Some GUI tests **write** images into build research directories; those
output destinations are not retained input dependencies.

Historical consumers remain: `tools/brn-successor.py` defaults its navigation path
to `brn-learning/data-prospective-v2/seek`; analyzers/exporters and frozen plans
read old results, datasets and checkpoints; the explicit Pair-2 parity command
reads B+C1. Retiring their inputs prevents those exact historical invocations.
It does not remove normal inference/training capability. Do not silently change
old plan paths/hashes or describe newly generated data as the old experiment.

The logged-in user's read-only Java Preferences inspection points the model library
and Arena outside the build trees, under `E:/SeedV6-Networks/`. Current BRN-3,
Pair-2 and NNUE-material `training-data/sources.json` point to
`E:/SeedV6-Corpus/incoming/lichess/lichess_db_eval.jsonl.zst` and/or
`E:/SeedV6-Corpus/incoming/bt4-t80`. The selected Arena campaign had no build-tree
reference. These **live user stores, registered sources, references, manifests,
history and optimizer states are outside all recommendations and size totals**.
They must remain intact. Other users' preferences and future/manual CLI paths
were not exhaustively audited; recheck actual consumers before eventual cleanup.

The E hold is the exact complement of the named targets, not an estimate that all
1.707606 GiB is essential. Principal contents are:

| Excluded/mixed area | Bytes | Reason for hold |
| --- | ---: | --- |
| `C:/projects/seed/java/seedv6/app/build/workstream-j/` | 807,016,223 | Earlier NNUE campaign/endurance checkpoints, distinct from BRN consolidation |
| `C:/projects/seed/java/seedv6/app/build/nnue-e013-*` (11 directories) | 475,252,875 | NNUE experiments/data/production scratch lineages; separate research has not been closed by this BRN result |
| `C:/projects/seed/java/seedv6/app/build/sr019/` | 250,263,051 | Search-research snapshots/probes including BRN measurements; mixed scope |
| `C:/projects/seed/java/seedv6/app/build/workstream-k/` | 151,512,318 | NNUE mapping/campaign evidence |
| `C:/projects/seed/java/seedv6/build/workstream-i/` | 33,323,373 | NNUE performance source/model/profile evidence |
| All other files/directories outside the exact D target tables | 116,159,905 | Mixed search/UI/native/Arena evidence and loose logs; no blanket deletion classification |
| **E total** | **1,833,527,745** | **Excluded from recoverable estimates** |

This includes remaining `phase*`, `sr*`, `codex-research`, `gui-smoke`,
`learning-arena`, `native`, preview/play fixtures and loose files (including BRN
logs). Their presence does not block the named BRN recommendations, but it blocks
a defensible recommendation to remove either parent build directory wholesale.
No active application dependency on a proposed D target was discovered; that is
a static/configuration finding, not a runtime test after removal.

## Verification and required review

Performed read-only enumeration/size accounting, alias checks, tracked-document
and source/consumer inspection; SHA-256 checked B and all C files; verified the
27-file duplicate lineage and two selected pair duplicates. Recomputed all six
final W/D/L totals/scores from 1,536 original pair files (3,072 games), checking
their hashes and all 768 config/execution-receipt bindings against retained
results. All 22 sealed-quality metric records matched the original reports; the
seven learning confirmations' hashes and W/D/L scores reconciled (832 games).
Cross-checked important calibration, throughput and historical GUI numbers against
their available JSON/Markdown sources. Both build-tree path/size/mtime inventories
were unchanged after documentation edits. This is not a full before/after hash
of every research byte. No games, training,
Gradle tasks, packaging, browser/GUI execution or exact reproduction were run.

Before any later cleanup, owner review/authorization is **blocking for deletion**.
Verified preservation of B is **blocking for deleting its parent**. Choosing to
keep or decline C is **blocking for deleting those mixed parents**; if kept, verify
copies first. Accepting loss of full historical replay/reanalysis is part of that
decision. Preservation, application import, and cleanup remain unperformed.
No human action blocks completion of this documentation/recommendation unit.

Version note: the pinned local finalizer `begin` was invoked before document edits,
but its lengthy full-tree capture was stopped before any token/reservation existed.
Subsequent local `status` returned `idle`, `active: null`, `unfinished: []`.
This is a non-blocking version-bookkeeping limitation under the SeedV6 override,
not a completed finalizer/no-bump receipt. Documentation-only impact warrants no
bump; build 34 is unchanged. Root `CODEXLOG_CURRENT.md` was absent and not created.
