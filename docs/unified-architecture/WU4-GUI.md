# WU4 desktop GUI ownership

16 October 2026 (Pacific/Auckland). Accepted prerequisite:
`ff7291aae877244843e99afa3c47120642f25bc5` (WU3), branch
`migration/unified-architecture`, authorized worktree
`C:/projects/seed/java/seedv6-unified`.

**Structural implementation and required non-interactive validation are complete;
GPT/user reconciliation is pending. Full native GUI runtime acceptance is not
claimed.** The owner explicitly authorized WU4 completion and commit without
desktop interaction on resuming this unit. Both Windows and macOS G2-G5 native
walkthroughs are deferred to later user-performed WU7 P1-P3 acceptance and remain
required before full GUI runtime acceptance or final production cutover.
[CONTRACT](CONTRACT.md) and [ACCEPTANCE](ACCEPTANCE.md) explicitly record this
bounded clarification. Other correctness and preservation gates are unchanged.

## Ownership and launch paths

All **75 GUI Java sources** move byte-for-byte from `app/src/main/java` to
`app-gui/src/main/java`, retaining `com.ohinteractive.seedv6.gui` packages.
All **35 runtime GUI resources** move unchanged with their classpath paths:
FlatDarkLaf properties, ten application icons, and both twelve-piece artwork sets.
[wu4-evidence.json](wu4-evidence.json) contains the complete relocation/hash map.
Existing native packaging artwork remains in build-tool inputs (`tools/icons` and
the root emblem); final packaging ownership is WU7, not a second runtime resource tree.
The two artwork generators and guide now target app-gui resources; Python syntax
and destination checks pass without regenerating artwork.

The extraction includes ChessFrame/SwingLauncher, BoardPanel/PieceRenderer,
GameController/GameSession/EngineSearchAdapter, per-side model selection,
TrainingPanel/TrainingController and data/corpus controllers, LearningArenaPanel/
LearningArenaController and views, TrainingSettings/TrainingFolders, native file
pickers, application menus/icons, themes, events and shutdown coordination.
Controllers consume existing core/training services; no algorithm is copied.

`ApplicationVersion` is shared runtime support used by GUI About and UCI
diagnostics. Its unchanged source moves to engine-core; the existing Gradle
version-resource generator is applied there once. Core gains no GUI or external
dependency. Numeric authority remains root VERSION_STATE.txt.

- `:app-gui:runGui` launches existing SwingLauncher with core/training and desktop
  libraries, independently of legacy app, retaining the optional vector JVM flag.
- `:app:run --args=gui`, unqualified `run --args=gui`, `Main gui`, and the existing
  distribution launcher retain Main's unchanged delegation to SwingLauncher.
- Ordinary Main/UCI startup and protocol implementation remain in legacy app.
- Windows/macOS package tasks retain names, distribution input, `app.jar` main JAR
  and SwingLauncher main class. Only embedded version read/stamp location changes
  to `engine-core.jar`. No new native package or production release was created.
- Legacy `:app:generateApplicationVersion` delegates to core. `runGui` deliberately
  avoids adding a second generic `run` task. Final unified packaging is deferred.

```text
engine-core     -> (none)
engine-training -> engine-core (api)
app-gui         -> engine-core, engine-training
app-uci         -> engine-core
research        -> engine-core, engine-training
app             -> engine-core, engine-training, app-gui (temporary composition)
```

Only the transitional app-to-GUI edge is newly allowed. Reverse, GUI-to-UCI/research
and training-to-GUI edges remain forbidden, including test/custom configurations.
FlatLaf 3.7 and JNA platform 5.17.0 move to GUI implementation dependencies; app's
mixed integration tests retain explicit test dependencies. No dependency version
changes. Existing Guava stays in app; Gson/SQLite/Zstandard stay in training.

Final production ownership: core **109 sources / 174 classes**, training
**134 / 317**, GUI **75 / 220**, legacy app **9 / 11**. App retains UCI/Main and
remaining research/verification composition. App-uci and research remain scaffolds.
No duplicate Java classes, shared source roots or second engine/training tree exists.

Four independent tests move unchanged into app-gui: GameSessionTest,
SquareMapperTest, PieceRendererTest and ApplicationMenuTest (21 cases).
DesktopOwnershipTest adds independent classpath/service/version/artwork checks,
including absence of Main/UciEngine. Mixed GUI/integration tests remain in app.
ProductionSearchBoundaryTest now checks all four production owners and GUI imports.
Eleven disposable Gradle fixtures exercise the allowed transition and forbidden edges.

## Automated evidence

Commands, inputs, exact results and raw evidence hashes are in wu4-evidence.json.
Gradle uses a worktree-local user home, Java 21, `--no-daemon --max-workers=2`.
Tests are forced headless, with isolated temporary/home/LOCALAPPDATA directories
and a file-backed PreferencesFactory. No resumed test opened/focused a window,
used desktop automation or accessed actual user preferences/stores. Native-only
tests remain skipped by their existing headless assumptions. Existing slow-NNUE
tag selection is unchanged; no inherited test was disabled or suppressed.

| Selection | Cases | Passed | Failed | Skipped |
| --- | ---: | ---: | ---: | ---: |
| Fresh engine-core | 61 | 61 | 0 | 0 |
| Fresh engine-training | 11 | 11 | 0 | 0 |
| Fresh app-gui | 22 | 22 | 0 | 0 |
| Retained-app GUI replay | 322 | 284 | 10 | 28 |
| Engine/search/UCI/version (reconciled) | 987 | 976 | 0 | 11 |
| Checkpoint / representative training services | 138 | 129 | 9 | 0 |
| Expanded Gradle boundary fixtures | 11 | 11 | 0 | 0 |

The complete unfiltered suite is **not passing/claimed**. Selected coverage and
reconciled identities are recorded separately from repeated diagnostic runs.
The nine GenerationRestartTest failures and ten GUI failures remain open and
match WU3 names/messages. Eight GUI failures concern obsolete architecture-selector
assumptions; TablebasePresentationTest and WorkflowRefinementGuiTest retain their
presentation assertions. Separate remediation remains required at their dependent
runtime gates; normal continuation does not certify crash-recovery failures.

Positive headless evidence covers game/session rules and navigation, engine/model
bindings, configuration/settings round trips, training controls/status, and Arena
configuration/controller/lifecycle/history integration. It does not certify native
mouse move entry, visual appearance/scaling, actual picker interaction or native
window shutdown. Those scenarios remain deferred even where component tests pass.

### Arena timeout investigation

The initial migrated broad run had an additional failure in
`LearningArenaGuiTest.controllerRunsOffEdtAndReopensDurableResults`: 32.767 seconds,
exceeding the fixture's unchanged 30-second condition wait. Its WU3 broad baseline
passed in 8.877 seconds. This adverse run is preserved.

The unchanged WU3 compiled test was replayed serially against retained WU3 and
WU4 distribution JARs under identical headless isolation. WU3 passes took 9.320
and 9.264 seconds; WU4 passes took 9.381 and 9.342 seconds. The full migrated GUI
replay then passed this test in 9.509 seconds, leaving only the ten exact inherited
GUI failures. All 722 production class payloads match WU3. No timeout, test
assertion or production lifecycle code was relaxed. Together these establish
sufficient evidence against a WU4 regression. The original transient timing cause
was not captured; it is not relabeled as one of the 19 inherited defects or claimed
to have been diagnosed exactly. No remaining WU4 correctness blocker was found.

An initial paired-test harness used the wrong installation directory and failed
current-side JUnit discovery before case execution; its logs/XML remain separate.
The earlier pre-extraction isolation-harness failure is also preserved. These are
validation harness errors, not application regressions.

The expanded archive boundary test initially rejected five empty legacy `gui/`
directory entries. Inspection proved both normal and fresh JARs contained no GUI
payload there. The assertion now filters directory entries and still rejects any
GUI class/resource file. Its focused two-case retest passes; the initial failed
engine-suite result remains recorded. No production change was needed.

### Build, packaging and preservation

- Fresh independent core/training/GUI compilation and tests pass without app;
  fresh legacy installDist/assemble/test/verification compilation passes. Registered
  UCI, research and packaging task routes remain available.
- Actual dependency/source-root verification and all eleven expanded fixtures pass.
  Configuration cache is stored and reused. Windows/macOS packaging dry-runs pass
  with `--warning-mode fail`; they are configuration evidence, not native builds.
- JDK 21's actual jpackage LauncherData resolver includes app-gui/core/training and
  loads the configured desktop/shared/legacy classes without initialization/windows.
  Direct GUI and Main-gui CLI probes reach the expected headless guard; Main UCI
  answers `uci`/`isready` and exits. These prove dispatch/classpath, not GUI behavior.
- All 374 original production source/resource/native inputs and all 722 production
  class payloads match WU3. Resource paths and third-party JAR bytes match; only
  generated sourceRevision metadata differs. Core `jdeps` reports only java.base.
- Seven-family persisted optimizer continuation and the cloned historical Pair-2
  checkpoint reproduce exactly. UUID, generation/step, runtime reconstruction,
  cursor/reservation/catalog checks and original input hashes are preserved.
- All 203 accepted SR-024 vectors match on 17 required fields; 66 real-model UCI
  requests pass, including exact deterministic comparisons and workers 1..16.
  SR-021 remains deferred/disabled; no search, evaluation or training change.
- Both historical repositories' tracked/protected bytes, Git metadata/status and
  size/mtime inventories match before/after. Existing research and frozen artifacts
  remain untouched. No model publication, historical merge resolution, push or deploy.

## Native evidence and remaining gates

The **earlier interrupted Windows session is historical evidence only**: direct
SwingLauncher (no app.jar) opened Play and displayed board, pieces, icons and theme.
A pawn-selection click preceded the user's physical Escape stop. No completed move,
engine response, training run, Arena run, preferences reopening or shutdown was
verified there. Its isolated fixture/logs remain under `build/migration/wu4/native*`.
No additional desktop interaction occurred on resume and no visual correctness
claim is inferred from compilation, process startup or headless tests.

At WU7, the user must validate Windows and macOS Play move entry/engine response,
new game/reset/navigation/side bindings, Training model/checkpoint selection and
start/stop/resume/progress, Arena learning and match-only setup/live/history/reopen,
preferences persistence, native pickers, layout/scaling and clean close during
active work, against identified package candidates using disposable copied state.
Inherited failures remain separate remediation dependencies. Native client/package
and production cutover acceptance are not completed by WU4.

## Version, Git and next boundary

Version is unchanged: **0.0.0, build 35**. This is structural migration, not a
release. The earlier interrupted root completed `no_bump`; resumed local finalizer
token `01331a8acf8d4d629cf4d91552982566` also completed `no_bump`, with unchanged
version bytes and idle final status. No root journal existed or was created.
No finalization warning or blocker was reported.

The WU4 commit is the commit introducing this record, including prior attributable
uncommitted extraction and resumed validation/remediation. The working tree is
clean after commit; no push or deployment. Raw evidence remains ignored under
`build/migration/wu4/`, including the retained pre-extraction distribution/classes
and the separate `resume/` evidence. Fresh checkouts do not contain those raw files.

Recommended next unit: WU5 standalone UCI, **only after GPT/user reconciliation**
of this structural WU4 handback. WU6 retains research separation, WU7 platform
packaging/GUI/client acceptance, and WU8 legacy-app removal and explicit cutover.
No next-unit implementation has begun. An additional source-path audit found
inherited research-tool assumptions in `tools/analyze-search-delta.py` (old core
source path) and `tools/brn-architecture.py` (receipt scans omit extracted owners).
They are unchanged in WU4 and require WU6 path/provenance review; the immutable
Git source path in `brn-throughput.py` remains historical by design.

Human actions required after this prompt:

- **Immediate blocker to WU4 structural completion: None.** Live GUI validation is
  not required before this owner-scoped structural gate.
- **Blocking WU5 admission:** GPT/user reconciliation of WU4 evidence and commit.
- **Deferred; blocking full GUI runtime acceptance/WU7 platform acceptance/cutover:**
  user-performed Windows/macOS G2-G5 walkthroughs and resolution/reconciliation of
  the inherited GUI/checkpoint failures at their dependent runtime boundaries.
