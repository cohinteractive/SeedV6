# WU3 training ownership

16 October 2026 (Pacific/Auckland). The owner's WU3 launch accepts WU2 commit
`09076c1d06ac8b532e065f4750bd0d8b206fe7db` and admits this structural extraction.
The starting migration worktree was clean on `migration/unified-architecture`.
The [contract](CONTRACT.md), [acceptance matrix](ACCEPTANCE.md), model-workspace
contract and existing training/research contracts retain their boundaries.
This unit does not adopt research, alter the accepted engine, or create a release.

**Implementation and scoped structural validation are complete. GPT/user
reconciliation is pending; WU3 is not declared accepted and WU4 is not started.**

## Ownership and public interfaces

`engine-training/src/main/java/com/ohinteractive/seedv6/` now owns 134 existing
production Java sources, relocated without changing their bytes or packages:

| Existing package | Sources | Ownership / principal interfaces |
| --- | ---: | --- |
| `core.brn`, `core.brn1`, `core.brn2`, `core.brn3`, `core.brnpair2` | 16 | Existing trainers, Adam state/configuration, five training codecs, BRN-3 objective/vector kernels. Package spelling is retained for compatibility; these are training implementations, not engine-core sources. |
| `training.nnue` | 9 | `TrainableNnue`, `NnueTrainer`, Adam optimizer/parameters/gradients/scratch, targets and `TrainingStateCodec`. |
| `training.model` | 6 | `NetworkTrainingState`, `NetworkModel`, `TrainingArchitecture`, recipes, architecture capabilities and `ModelLibrary`. |
| `training.checkpoint` | 24 | `CheckpointStore`, manifests/payload verification, partial generations, restart/recovery, lineage/provenance, retention, validation/promotion receipts and frozen replay. |
| `training.data` | 12 | Source descriptors/library, readers, BINP preparation/decoding, labels and independent source ledgers/cursors. |
| `corpus`, `corpus.lichess` | 13 | SQLite catalog, shard reader/writer, preparation/view, Lichess decoder/importer and existing headless `CorpusMain`. |
| `training.service` | 21 | `TrainerService`, configuration/session/termination, sequential corpus training and reusable Learning Arena campaign services. |
| `training.selfplay` | 17 | Existing headless game/sampling/control and family-specific optimization services; corpus-only families retain that restriction. |
| `training.validation` | 9 | Validation arena/configuration, held-out loss, progress/control and existing promotion policy. |
| `training.history`, `training.telemetry` | 7 | Durable generation/history records and presentation-neutral progress/game/optimization snapshots. |

The binpack attribution resource moves to the same classpath path in the training
JAR. GUI icons/themes remain in app; opening book, Syzygy resources/JNI and all
108 accepted engine sources remain in core. No new native build is introduced.
The complete file/hash mapping is in [WU3 evidence](wu3-evidence.json).

Source inspection found no GUI, UCI or application dependency in the relocation
set. No new interface or adapter is necessary. Existing observers, controls,
snapshots and configuration records already separate service work from Swing.
All 134 sources have one production owner; no source root is shared or copied.

## Inference boundary and application composition

Core retains every runtime model, runtime codec, geometry, immutable Pair-2
compilation input, inference workspace, score mapping and search factory.
The WU2 package-local codec helpers remain accessible on the classpath; this is
not a JPMS migration. Training retains parameter folding/snapshots, compilation
orchestration and optimizer serialization. `NetworkModel` is the existing
workspace/family facade over core models, not another inference implementation.
There is no model-registry, schema, optimizer, calibration or promotion change.

Legacy app still owns `Main`, `ApplicationVersion`, all Swing presentation,
preferences, training/data controllers, model-selection UI, Arena UI and UCI
protocol/application construction. It consumes training through an explicit
Gradle dependency. `:app:corpus` and existing training/research JavaExec tasks keep
their names and main classes. Independent experiments/oracles and mixed integration
tests remain under app verification/test for WU6; no research implementation moves
into training or core. WU4 and WU5 remain unimplemented.

```text
engine-core     -> (none)
engine-training -> engine-core (api)
app-gui         -> engine-core, engine-training
app-uci         -> engine-core
research        -> engine-core, engine-training
app             -> engine-core, engine-training
```

Training exposes core because its public APIs use core types. Gson 2.11.0,
zstd-jni 1.5.7-20 and sqlite-jdbc 3.53.4.0 are training implementation dependencies.
Their versions are unchanged. Direct Gson use by retained verification code and
SQLite/Zstandard use by integration tests have explicit nonproduction dependencies
in app. FlatLaf, JNA and existing Guava stay in app. Core gains no external library.
The optional BRN-3 vector compile flag moves to training; legacy launch/test flags
and the scalar fallback retain their behavior. Distribution assembly includes
each module JAR once and preserves the existing complete-runtime-classpath launchers.

Four independent existing tests move byte-for-byte to training: partial-generation
store persistence, Pair-2 loss/continuation, promotion policy and BRN-3 vector
parity. Mixed checkpoint/service/research tests retain their existing app tasks.
The production boundary test now inspects all three JARs and all production roots,
including duplicate classes and forbidden application dependencies in training.

## Validation and continuity

Execution results are recorded here and in `wu3-evidence.json`.
Raw logs, disposable stores and the retained WU2
distribution live under ignored `build/migration/wu3/`. The bounded
`tools/verify-unified-training.py` bridge requires explicit built distributions,
JDK and pinned checkpoint inputs; it refuses to reuse its output directory.
It runs identical probes against WU2 and training/core-only WU3 classpaths.

The independent disk-continuation probe compares uninterrupted and interrupted
work for all seven families: nonzero optimizer updates, persisted partial cursor,
close/reopen, exact weights/moments/step, immutable parent, generation identity,
lineage UUID, source reservation/cursor and Best/latest catalog separation.
Historical checkpoint loading uses a private clone of the original four artifacts
and lineage sidecar, so its payload-lock operations cannot affect the user store.
The existing pinned-model probe additionally verifies exact recompile, codec
round trips and one-batch private continuation of the real generation-32 state.

The expanded pre-extraction baseline exposed three corpus subprocess failures:
their manually assembled classpaths omitted core after WU2. The two test helpers
now include Board's actual code source. This is a test-harness repair, with no
expectation suppression or production change. The nine previously recorded
checkpoint restart failures and ten GUI failures remain separately reconciled;
neither their inherited status nor byte-preserved relocation proves continuation.
Positive continuity evidence is required in addition to that reconciliation.

Two additional timing-sensitive test fixtures were investigated without changing
production semantics. SequentialTrainingTest created byte-identical files whose
path-independent identities collide when timestamps fall in the same millisecond.
A deterministic same-time probe reproduces this with both WU2 and WU3; one fixture
now has a different record count. HceArenaGuiTest accepted intermediate
`RUNNING | ROUND_COMPLETE` as final completion and reopened before worker release.
An instrumented copy demonstrates the same race with both distributions. Its
wait now requires the exact `COMPLETE` status and enabled Start control. No
expectation is removed, no timeout increased, and no failing test is disabled.

The initial broad replay also exceeded the unchanged 90-second wait in the
16-generation session fixture while other validation was active. The unchanged
five-case session suite passed when replayed alone; the 16-cycle case took
76.730 seconds (baseline total 91.483 seconds). No production or timeout change
was made for this result. Earlier adverse logs and results are retained.

| Executed scope | Cases | Passed | Failed | Skipped |
| --- | ---: | ---: | ---: | ---: |
| Pre-WU3 broad training reference | 642 | 627 | 12 | 3 |
| Initial migrated broad training (11 cases now in training module) | 631 | 617 | 11 | 3 |
| Independent core module | 61 | 61 | 0 | 0 |
| Independent training module | 11 | 11 | 0 | 0 |
| Engine/search/UCI/version | 987 | 976 | 0 | 11 |
| Recorded inherited GUI selection | 38 | 21 | 10 | 7 |
| Initial additional application integration | 83 | 76 | 2 | 5 |
| Final application + sequential training retest | 92 | 86 | 0 | 6 |
| Isolated unchanged session retest | 5 | 5 | 0 | 0 |
| Reconciled distinct selected cases, using final focused outcomes | 1,643 | 1,598 | 19 | 26 |

The reconciled 19 failure names/messages exactly match WU2. These counts are a
union of selected runs, not a claim that a single full suite passed. Baseline's
three extra corpus failures, the two timing-sensitive fixture defects and the
session timeout are accounted for above and in the evidence manifest.

The following independent evidence establishes the structural continuity boundary:

- All seven families pass disk save/close/reopen/continue with exact encoded
  optimizer states and checkpoint identities against the retained WU2 distribution.
  The original generation-32 Pair-2 checkpoint loads from a clone with its original
  lineage UUID and optimizer step 1,753,088. Exact recompilation and private
  one-batch continuation match the frozen accepted model probe. No evaluator is
  substituted and no historical checkpoint is updated.
- All 203 SR-024 vectors match across 17 fields. The real-model process bridge
  passes 66 requests and ten exact comparisons across workers 1..16, resets,
  evaluator changes and stop/quit transitions. SR-021 remains deferred/disabled.
- Independent module tests pass: core 61/61; training 11/11. Engine/search/UCI/version
  selection passes 976 cases with 11 existing skips. Corpus tests exercise SQLite,
  Zstandard, BINP and subprocess resource/classpath resolution; service tests cover
  supported self-play, held-out validation, selection/promotion and persisted cursors.
- All 722 production class byte payloads match WU2 exactly: 173 core, 317 training,
  232 app. All 12 external dependency JARs and all 39 resource paths are unchanged.
  Only generated version-resource sourceRevision reflects the dirty migration tree;
  numeric version/build is unchanged. No duplicate production class exists.
- Nine dependency-boundary fixtures pass. All target projects compile, configuration
  cache replays, installDist assembles, and Windows/Mac packaging dry-runs pass with
  warning-mode fail. Core still resolves only java.base. Training-only probes run
  without app, GUI, UCI, verification or research classes on their classpath.

The nine inherited restart failures concern interrupted atomic archive/replacement
recovery. Positive partial save/resume, model reconstruction, seven-family optimizer
continuation, source cursors and identity tests establish safe byte-preserved
extraction; they do not certify those failing crash-recovery paths. Those cases
remain open for separate remediation before the dependent runtime acceptance gate.
The ten inherited GUI failures likewise remain open. Native Swing, macOS, real
UCI clients, the full unfiltered/slow suites, large training campaigns and final
native release packaging were not run or certified by WU3.

## Preservation, version and handback

Both historical checkouts retain their original tracked/protected bytes, status,
HEAD/index/merge metadata and full size/mtime inventories: primary 3,597 tracked
files, 1,098 protected artifacts and 44,076 inventory entries; UE 110, 590 and
2,706 respectively. The four pinned checkpoint payloads and lineage sidecar are
unchanged. Raw generated evidence stays ignored in the migration worktree;
no model bytes are committed or published and no user-store migration occurs.

Version authority remains `0.0.0`, build **35**. Local finalizer token
`1d00be72962646729db46bb2039c166e` completed `no_bump`, with identical version
bytes and no warnings/blockers; subsequent status is idle with no unfinished
operation. No release or native package is created. Finalizer evidence is recorded
in `wu3-evidence.json`; no root journal
exists or is created. The attributable WU3 commit is the commit introducing this
record. No push, deployment or next-unit work is authorized by this handback.

Human actions required after this prompt:

- **Blocking for WU4:** GPT/user reconciliation and acceptance of WU3.
- **Non-blocking for WU3; blocking dependent runtime acceptance:** authorize and
  complete separate remediation of the 19 inherited GUI/checkpoint failures.
- **Non-blocking now:** deferred model-publication/platform/release decisions
  remain due at their existing downstream gates.
