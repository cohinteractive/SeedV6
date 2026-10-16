# WU5 standalone UCI application

16 October 2026 (Pacific/Auckland). Accepted prerequisite:
`b412e34f35d5c1baccfe218f17d4c5a80a37a25c` (WU4), confirmed by the owner's WU5
launch. Implementation and validation belong only to `seedv6-unified`, branch
`migration/unified-architecture`. No desktop interaction, Computer Use, native
release, model publication or search research is part of this unit.

## Ownership and composition

`app-uci` owns `com.ohinteractive.seedv6.uci.UciMain`, `UciEngine`, the existing
command/move parsers, session/history boundary, output and score/info formatting,
`UciConfiguration`, `ProductionUciConfiguration` and `ProductionModel`.
Seven protocol sources relocate from app; no second protocol implementation remains.
Two independent parser test sources relocate unchanged. Mixed tests using legacy
Main, checkpoint stores or independent research oracles remain in app until WU6.

Core retains every engine implementation: board/rules, move generation, evaluation,
inference/codec, TT, clocks, search driver/lifecycle and accepted search policies.
The only core edits expose TT injection through the canonical production factory,
the existing exact adapter, and the existing parallel constructor. Hash is bounded
to 1..1024 MiB before allocation. Default construction still requests 64 MiB.
No search loop, evaluator arithmetic, model format, tuning or research policy changes.
GUI/training source and resource ownership is unchanged.

```text
engine-core     -> (none)
engine-training -> engine-core
app-gui         -> engine-core, engine-training
app-uci         -> engine-core
research        -> engine-core, engine-training
app             -> engine-core, engine-training, app-gui, app-uci (temporary)
```

The temporary app-to-UCI composition is covered by the same dependency guard as
all other edges, including test/custom configurations. Targets cannot depend on
legacy app. Standalone distribution `lib/` contains only `app-uci.jar` and
`engine-core.jar`; both applications use the same built core JAR.

## Configuration profiles and compatibility

| Boundary | Standalone `UciMain` | Legacy `Main` |
| --- | --- | --- |
| Default evaluation | Verified pinned generation-32 BRE-Pair 2 | Existing HCE; explicit checkpoint options retained |
| Search construction | `ProductionSearch.create(workers, evaluation, hashMiB)` | Existing `UciEvaluation` service selection |
| Options | Threads 1..16 (default 1), Hash 1..1024 (default 64), Clear Hash | Existing Threads and OwnBook (default true) |
| Book / tablebase | Neither selected by this product profile | Existing core book/optional tablebase behavior |
| Early-stop / drawn root | Legal fallback when no completed move exists; no invented score/PV | Existing sentinel behavior retained |
| Completed infinite request | Holds bestmove until stop, including terminal roots | Existing immediate terminal completion retained |
| Invalid supported input | Diagnostic on stderr, atomic position rejection | Existing quiet rejection retained |
| Model/research switches | Only `--model` / `seedv6.uci.model`; legacy/research properties rejected | Existing explicit checkpoint/warmup/control properties retained |

These are explicit boundary policies in one protocol implementation, not two engines
or two parsers. Existing legacy handshake and tests remain unchanged. `Main gui`,
app-gui direct launch, and desktop packaging entry points retain their WU4 behavior.
`runUci` is uniquely named so unqualified `run --args=gui` still selects legacy app.

Legacy `UciEvaluation` stays in app, implementing the small core-only configuration
interface. It retains checkpoint-store selection and disabled research seams; moving
that code into app-uci would incorrectly introduce a training dependency and promote
experimental composition into the standalone product. WU6 must separate these
remaining research/task responsibilities while retaining frozen controls.

## Immutable UE adaptations

Reference implementation is UE SR-024 commit
`87b8b5b50acf3d2fe19793bfb0e8b24a45be114b`; subsequent release record
`58511f45bf1bf0a45ca25f31da402a9e0ba56c50` changes only RELEASES.md. References are
read from Git objects, never the conflicted checkout. Retained adaptations:

- `UciSession` validates king counts, back-rank pawns, previous-mover check,
  castling pieces and en-passant consistency before optimized move generation.
- Model loading verifies length and pinned SHA-256, then decodes the same bounded
  bytes, eliminating a check/reopen race. It never resolves Best/latest or a store.
- Legal fallback uses core move generation; completed search results retain priority.
  Terminal roots still return `0000`; fallback fabricates no depth, score or PV.
- Threads/Hash reconfiguration allocates the replacement first; an allocation
  failure retains previous settings. Core closes/drains the old lifecycle before reuse.
- Infinite-result publication stays owned by the UCI session until stop. Replacement,
  Clear Hash, new game, position changes and shutdown discard stale publication.
  No UCI publication lock is held when entering core's callback generation gate.
- Output lines flush promptly; diagnostics use stderr. Quit/EOF close core workers;
  the standalone JVM shutdown hook requests cooperative drain.

Excluded: UE's independent Board/search/evaluation/TT/lifecycle implementations,
release counter/properties, native launchers/distributions and experimental engines.
Historical Windows/macOS v6 identities and the unfinished UE merge remain preserved.

## Model and launch

The model is lineage `235d0688-e060-44e3-8f1e-119c39bc8017`, generation 32,
checkpoint `g000032-s001753088-5837c7f7192aa3cf9a265388f93649327455e804541398ae3ef6b69e09014304`.
Runtime payload is 795,032 bytes, SHA-256
`5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7`.
Core retains `BRN_PAIR2_MATERIAL_RESIDUAL_QUARTER_V1`. Production remains SR-024:
QXRAY, protected quiet LMR, WINDOW, completed-iteration EFFORT, ROOT_REUSE at 2..16.
SR-021 is deferred without adoption. Startup errors exit nonzero before protocol stdout.

From the migration worktree, use the local Gradle home:

```powershell
$env:GRADLE_USER_HOME = "$PWD/build/migration/gradle-user-home"
.\gradlew.bat --no-daemon --max-workers=2 :app-uci:installDist
.\app-uci\build\install\seedv6-uci\bin\seedv6-uci.bat --model "C:\absolute\path\baseline.brn-pair2"
```

The Unix Java launcher is `bin/seedv6-uci`. Direct Java entry is
`java -cp "<installation>/lib/*" com.ohinteractive.seedv6.uci.UciMain --model <file>`.
Gradle developers may use `:app-uci:runUci --args="--model <file>"`.
Protocol clients should use the installed launcher/direct Java, without Gradle output.
Java 21 is required; this intermediate Java distribution does not bundle a runtime.

Without an override, the model resolves to `<installation>/models/baseline.brn-pair2`.
Relative overrides are also installation-relative, never client-cwd-relative.
Compiled-class launches resolve relative to their code-source directory and should
normally use an explicit absolute model path. No developer-machine/store path is
embedded. Model bytes are not in Git, JARs or distribution archives. A verified
local copy is used only in ignored validation output for relocation tests.

## Validation and remaining gates

Implementation and required headless validation are complete. GPT/user reconciliation
is pending; this is not native/runtime cutover acceptance. Exact commands, inputs,
results and hashes are in [wu5-evidence.json](wu5-evidence.json). Raw evidence stays
under ignored `build/migration/wu5/`; fresh checkouts require the stated local inputs.

| Selected JUnit scope | Cases | Passed | Failed | Skipped |
| --- | ---: | ---: | ---: | ---: |
| Core | 61 | 61 | 0 | 0 |
| Training | 11 | 11 | 0 | 0 |
| GUI owner | 22 | 22 | 0 | 0 |
| UCI owner | 13 | 13 | 0 | 0 |
| Legacy engine/search/UCI/version | 979 | 968 | 0 | 11 |
| Checkpoint | 64 | 55 | 9 | 0 |
| Legacy GUI integration | 322 | 284 | 10 | 28 |
| Reconciled distinct selected cases | 1,472 | 1,414 | 19 | 39 |

All 19 failure names/messages exactly match WU4. No test is disabled, weakened or
suppressed; no WU5-introduced correctness failure remains. This is selected coverage,
not a full-suite pass. The nine checkpoint crash-restart failures are not certified
by the positive normal-continuation evidence.

- Fresh independent app-uci/core compilation, 13 standalone unit cases and Java
  installation pass with `--warning-mode fail`. Effective runtime classpath is
  core alone; `jdeps` reports only `java.base`. All 159 standalone process searches
  pass with `--limit-modules java.base`, using immutable UE SR-024 protocol tests
  plus independent python-chess legality checks and added failure/relocation cases.
  The separate relocated generated Java launcher passes from a directory with spaces,
  unrelated cwd and `DEBUG=1`. No model is packaged into the produced archives.
- `verify-unified-core.py` matches all 203 accepted SR-024 vectors on all 17 fields
  against both the frozen primary JAR and current core. Fixture SHA-256 remains
  `0a10bc7de11506fca7945e236caa8badffab4abbcee975fd1b72583e47dfa3e7`.
  `verify-unified-uci.py` passes 66 real-model legacy requests and ten exact
  comparisons, including workers 1..16 and transitions 1/4/1/6/2/16/1.
- `verify-unified-training.py` matches all seven persisted optimizer continuations,
  original checkpoint reconstruction and private one-batch continuation against WU4.
  It clones the historical store before any store-lock operations; original payloads
  and lineage sidecar are unchanged. Training/GUI class payloads remain byte-identical.
- Twelve disposable Gradle boundary fixtures pass, including legacy-to-UCI admission
  and forbidden UCI dependencies. Actual assembly, configuration-cache replay,
  Windows/macOS packaging dry-runs and launch-task dispatch checks pass. Actual
  `runUci` answers uci/isready and exits successfully with configuration cache stored.
- Final production counts are core 109 sources / 174 classes, training 134 / 317,
  GUI 75 / 220, UCI 11 / 14, legacy app 2 / 2. No duplicate source/class exists.
  Both applications contain the identical core JAR. Seven sources move to UCI;
  five pure protocol sources and both parser test sources preserve exact bytes.
  All unchanged GUI/training inputs are preserved; core changes are limited to the
  three constructor/factory sources identified above.
- Both historical repositories match before/after tracked hashes, protected artifact
  hashes, branch/HEAD/status/index/merge metadata and full size/mtime inventories.
  Primary: 3,597 tracked files, 1,098 protected artifacts, 44,076 inventory entries;
  UE: 110, 590 and 2,706. Existing releases and frozen inputs are untouched.

Two validation harness issues were corrected without application changes: system
Python lacked python-chess, so the established migration-local virtualenv was used;
an initial classpath-inspection script caused a Gradle deprecation by resolving a
foreign project configuration, then passed as an owner task in a fresh build.
Adverse logs remain local. No warning suppression or acceptance relaxation occurred.

Native Windows executable/runtime bundling, macOS launcher/runtime packaging,
Arena client sessions, BotLi configuration/live games, platform signal behavior,
model publication and release naming remain WU7 or separate owner decisions.
WU4's user-performed Windows/macOS GUI walkthroughs remain deferred. The 19 inherited
GUI/checkpoint failures are not suppressed and remain open at their dependent gates.
No native execution, live game, strength gain or full unfiltered suite pass is claimed.

Version remains 0.0.0 build 35 under the owner's structural no-bump instruction.
GPT/user reconciliation of this unit is required before WU6; no WU6 work is begun.


## Reproduction and handback

All Gradle commands use a migration-local `GRADLE_USER_HOME`, Java 21,
`--no-daemon --max-workers=2` and headless test isolation. Principal commands:

```text
gradlew.bat :app-uci:test :engine-core:test :engine-training:test :app-gui:test -Pheadless
gradlew.bat :app:test -Pheadless --tests com.ohinteractive.seedv6.core.* --tests com.ohinteractive.seedv6.search.* --tests com.ohinteractive.seedv6.uci.* --tests *Version*Test
python -B tools/test_gradle_architecture.py -v
build/migration/wu2/python/Scripts/python.exe -B tools/verify-standalone-uci.py --java C:/java/jdk-21/bin/java.exe --install app-uci/build/install/seedv6-uci --model C:/projects/seed/java/seedv6-ue/models/baseline.brn-pair2 --ue-repo C:/projects/seed/java/seedv6-ue --out build/migration/wu5/standalone-process-02
```

The evidence manifest records the full isolation arguments, all other commands and
input identities. Output directories are immutable evidence: use a new output path
when reproducing. The Python UCI verifier needs python-chess; its recorded environment
is local validation tooling, not an application dependency. Test preference/home/temp
and LOCALAPPDATA overrides remain inside the migration worktree.

Local finalizer token `9eff0f2e573d472ba2539057875dbdd6` completed `no_bump`,
with unchanged version bytes, no warnings/blockers and idle final status.
No root CODEXLOG_CURRENT.md exists or was created. The WU5 commit is the commit
introducing this record, without push/deployment or model publication.

Human actions required after this prompt:

- Immediate blocker to WU5 implementation/commit: **None**.
- **Blocking WU6 admission:** GPT/user reconciliation of this WU5 commit and evidence.
- **Deferred; blocking dependent runtime/WU7/cutover acceptance:** separate remediation
  of the 19 inherited failures; user-performed Windows/macOS GUI and real-client
  checks against identified candidates; model-publication and platform/release
  decisions at their actual packaging dependencies.
