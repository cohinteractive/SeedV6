# WU1 Gradle foundation

16 October 2026 (Pacific/Auckland). Authorized by the WU1 request following GPT
acceptance of WU0 commit `81e868a72b942202e1ced89fe30a5a13e3af155f`.
Implementation is confined to `C:/projects/seed/java/seedv6-unified`, branch
`migration/unified-architecture`. [CONTRACT](CONTRACT.md), [STATE](STATE.md) and
G1 in [ACCEPTANCE](ACCEPTANCE.md) govern this unit. WU1 does not begin extraction.

## Actual structure and transition

There are **six included projects during transition**, plus the root build:

| Project | Plugin / physical directory | Declared production project dependencies | WU1 source ownership |
| --- | --- | --- | --- |
| `engine-core` | `java-library` / `engine-core/` | None | Empty; WU2 extraction target. |
| `engine-training` | `java-library` / `engine-training/` | `engine-core` | Empty; WU3 extraction target. |
| `app-gui` | `java` / `app-gui/` | `engine-core`, `engine-training` | Empty; WU4 extraction target. No invented entry point. |
| `app-uci` | `java` / `app-uci/` | `engine-core` | Empty; WU5 implementation target. No invented entry point. |
| `research` | `java-library` / `research/` | `engine-core`, `engine-training` | Empty; WU6 separation target. |
| `app` (temporary) | Existing `application` / `app/` | None currently | All existing main, verification and test code/resources. |

The target dependency declarations use `implementation`, without broad classpath
inheritance, copied trees, fake APIs or production implementations. Empty modules
compiling is **not runtime separation**. Current GUI, UCI, training and research
remain together in the legacy project with the existing internal source sets.

Renaming `:app` to `:app-gui` now would alter a public developer-task route and
default JAR identity. Native packaging and retained research scripts refer to
`:app:installDist`, `app/build`, and `app.jar`. Examples include both native
packagers, `brn-architecture.py`, `brn-successor.py`, `brn-throughput.py` and SR
integration validators. Retaining `:app`, explicitly permitted by the contract,
preserves those paths without aliases or duplicating compilation. It is not a
sixth final owner.

WU2/WU3 may add legacy `app -> core/training` dependencies as implementation is
removed from legacy source ownership. No target may depend on legacy `app`.
WU4/WU5 establish real application entry points; WU6 remaps developer/test tasks.
WU7 adjusts native packaging with platform acceptance. WU8 removes `:app` from
settings and the boundary allowlist only after parity, task/path mappings,
packaging/rollback gates and explicit cutover acceptance. No such gate is claimed
here. Source extraction must transfer ownership rather than add overlapping roots.

## Build changes and enforcement

- `settings.gradle`: adds five target identities; keeps `:app`.
- Each target `build.gradle`: plugin, responsibility and explicit dependencies.
- `gradle/java-conventions.gradle`: shares the existing Maven Central repository,
  Java 21 toolchain and catalog JUnit Jupiter / launcher setup. The existing app
  applies it in place of equivalent local declarations. No dependency upgrades.
- Root `build.gradle`: base lifecycle aggregation plus the boundary script;
  native package task implementations and paths are unchanged.
- `gradle/project-boundaries.gradle`: exact project identity allowlist, allowed
  dependency directions, cycle traversal and overlapping Java source-root check.
  Every configuration is inspected, including tests and custom configurations.
  Dependency-addition callbacks reject forbidden late declarations. The explicit
  `verifyProjectBoundaries` task reports the graph and runs before Java compilation
  and every module's `check`; root `check` aggregates all module checks. Validation
  also occurs during configuration, so an invalid dependency fails even `help`.
  The task retains values rather than Gradle model objects for configuration-cache
  reuse. This checks declared Gradle project dependencies and source ownership;
  it does not certify future Java package/API isolation or arbitrary file-based
  classpath additions. Those remain review and extraction-validation obligations.
- `tools/test_gradle_architecture.py`: real Gradle fixtures test valid and invalid
  graphs, legacy direction, custom/late/test dependencies, cycles, source overlap,
  unknown identities and configuration-cache replay. Fixtures stay under ignored
  `build/migration/architecture-tests/`; no source implementations are copied.

Compiler/vector flags, JavaExec arguments, application main class/name, dependency
versions, source/resource directories, test filters, verification/research tasks,
optional Syzygy/native license handling and version resources remain equivalent.
`Main` still selects `SwingLauncher` for `gui` and UCI otherwise. Development
outputs remain `app/build/classes`, `app/build/libs/app.jar`, and
`app/build/install/seedv6`. Target outputs use their own `<module>/build/`.
No production runtime dependency on `research` or `verification` is introduced.

## Validation method and evidence

Local transcripts, snapshots and test reports are under ignored
`build/migration/wu1/`; the compact committed result is
[wu1-evidence.json](wu1-evidence.json). These local outputs are unavailable in a
fresh checkout. The reproducible boundary fixture suite is tracked in `tools/`.

All Gradle invocations use the unchanged 8.11.1 wrapper, JDK 21 and
`GRADLE_USER_HOME=<migration worktree>/build/migration/gradle-user-home`,
`--no-daemon --max-workers=2`. Disposable temporary files are inside the migration
worktree. The WU1 inspection init script sets test `java.io.tmpdir` there;
`TEMP`/`TMP` do likewise for child processes. `GIT_CEILING_DIRECTORIES` is the
migration root so a source-archive fixture cannot accidentally discover the parent
repository. Normal root Git inspection still works.

Commands (run from the migration root, with the environment above):

```powershell
.\gradlew.bat --no-daemon --max-workers=2 projects :app:tasks --all
# Baseline: captured before build edits, with a local source/task snapshot init script.
.\gradlew.bat --no-daemon --max-workers=2 --no-configuration-cache `
  -I build/migration/wu1/inspect.gradle `
  '-Pwu1Snapshot=build/migration/wu1/baseline-model.json' `
  :app:classes :app:verificationClasses :app:test -Pheadless `
  --tests '*Version*Test' --tests '*Uci*Test' --tests '*ProductionSearch*Test'
.\gradlew.bat --no-daemon --max-workers=2 projects verifyProjectBoundaries
.\gradlew.bat --no-daemon --max-workers=2 `
  -I build/migration/wu1/inspect.gradle check :app:verificationClasses -Pheadless
# Final bounded regression; independent of the failed/incomplete expanded run.
.\gradlew.bat --no-daemon --max-workers=2 `
  -I build/migration/wu1/inspect.gradle :app:test -Pheadless `
  --tests 'com.ohinteractive.seedv6.core.*' `
  --tests 'com.ohinteractive.seedv6.search.*' `
  --tests 'com.ohinteractive.seedv6.uci.*' --tests '*Version*Test'
python -B tools/test_gradle_architecture.py
.\gradlew.bat --no-daemon --max-workers=2 packageWindows packageMac --dry-run
```

The inspection script is local measurement infrastructure, not a required build
adapter. Ordinary `:app:test -Pheadless` and `check -Pheadless` remain supported.
The pre/post configured `:app` snapshot compares all task names, all declared
dependencies, Java source/resource roots, toolchain, main class, compiler args and
output directory. No differences were observed. Configuration-cache checks use
the default enabled cache; inspection-only snapshots explicitly disable it to
force a fresh measurement. Development `installDist` is an existing dependency
of the launcher regression tests; it does not create a native production package.

Initial local measurement failures were corrected without application changes:
PowerShell BOM and argument quoting, an init-script task/property reference, and
Git discovery of the enclosing repository by the disposable version fixture.
All failed logs are retained. Boundary implementation review also replaced the
deprecated `ProjectDependency.dependencyProject` access with Gradle 8.11.1's
`path` API. No wrapper upgrade was needed.

Numerical results, preservation comparisons and final status are recorded
in the evidence manifest and STATE. No full `nnueSlowTest`,
strength tournament, training campaign, external corpus traversal, optional
native Syzygy compilation, native package build, GUI walkthrough, Mac execution
or real-client acceptance is part of G1. Their later-stage gates remain pending.

### Inherited failures and scoped commit decision

The expanded `check -Pheadless` run observed 19 failures: ten GUI cases and
nine `GenerationRestartTest` crash parameters. All 19 also fail using the WU0
build scripts exported from accepted commit `81e868a72b942202e1ced89fe30a5a13e3af155f`,
with a separate Gradle project cache and separate application outputs. The replay
changes only settings/build-file locations, catalog location and output isolation;
application sources and original build declarations are unchanged. Its deprecated
alternate-settings CLI is local comparison instrumentation, not part of the new
build. Baseline replay logs are retained, including initial path/catalog setup
errors before the successful reproduction of the test failures.

Eight GUI failures dereference an absent `networkArchitecture` selector; the
remaining two concern tablebase presentation and graph layout. The nine restart
parameters encounter a null `expectedArchitecture` (six directly and three while
expecting an injected IOException). These observations establish baseline failures,
not an authorized diagnosis/fix of GUI or checkpoint behavior.

The expanded run was stopped after 12m39s by terminating only its verified test
worker, after the failures were observed. It is **failed/incomplete**, with no
aggregate test-count claim. Its XML snapshot contains the previous 59-case run
and is explicitly not evidence for the interrupted run; use its console and the
complete focused baseline replay XML. No exclusion, disabled test, changed test
expectation or ignored failure was added to the build. The final selected
engine/rules/UCI/version regression is reported independently.

Source preservation is stronger than a source diff alone: the WU0 replay and WU1
build have identical hashes for all 716 main, 311 verification and 922 test class
files, and all 756 non-directory application-JAR entries. The task/source/dependency
model also matches. This supports attribution of the observed failures to the
inherited baseline, while leaving the full-suite outcome and later runtime
acceptance explicitly open.

The initial acceptance/commit hold under the owner's stop condition was reconciled
by the owner's explicit decision: **"Yes—commit WU1 and document all 19 inherited
failures for separate remediation."** This permits the WU1 commit with the recorded
baseline limitations. It does not change tests, waive later GUI/training runtime
acceptance, or authorize source remediation. WU1 implementation and scoped
validation are complete; GPT/user acceptance remains pending and WU2 is unadmitted.

Final selected regression: **1,017 cases, 1,006 passed, 11 existing opt-in skips,
zero failures/errors**, in 3m48s. All 97 legacy task names and their configured
dependency/source/toolchain/compiler/entry-point/output model are unchanged.
Nine boundary fixture tests passed in 102.355s. Actual target compilation and
configuration-cache replay pass with `--warning-mode fail`. Packaging dry-runs
pass with the same warning policy. `git diff --check` passes.

Final artifact review checks the exact 17-file change scope and 52 available local
documentation links. The existing `docs/architecture.md` link to ignored
`archive/search-programme/README.md` is unavailable in this worktree and unchanged
from WU0; all newly introduced links resolve. No historical archive is recreated.

The final owner decision covers both the ten inherited GUI failures and the nine
inherited checkpoint-restart failures. No unresolved validation decision blocks
this WU1 commit; separate remediation and dependent runtime acceptance remain open.

## Version and preservation boundary

WU1 is non-runtime build scaffolding under the owner's explicit no-release-change
instruction. `VERSION_STATE.txt` remains `0.0.0`, build **35**. The local finalizer
began with token `43045c25824846d28384c4b412f1bdac` and finished `no_bump`, with
no warnings/blockers. Subsequent status is idle, with no active/unfinished
operation. The version file's SHA-256 is unchanged; terminal receipt digests are
in the evidence manifest. No build increment is claimed or performed.
The older operating-guide reference to the shared finalizer is superseded here
by root AGENTS.md's explicit local-entrypoint instruction; no shared policy or
finalizer is edited. Gradle's existing version task only reads the authority and creates development
resources. No finalizer, release increment or native package task is triggered
by compilation or tests. `CODEXLOG_CURRENT.md` is absent; none is created.

No Java/native source, production resource, model, checkpoint, frozen protocol,
research result, packaging implementation, wrapper, catalog version, shared
finalizer or governance registry is changed. The five WU0 accepted-source versus
main manifest differences (including `app/build.gradle`) and the SR-021 research
seam remain accounted for by BASELINE; WU1 does not adopt SR-021 or certify a new
search implementation. Original SeedV6 and UE tracked bytes, protected packages,
frozen SR-024 files, Git identity/index and UE merge metadata are compared before
and after. Other historical ignored data is inventoried by size/mtime, not fully
rehashed. No external process is stopped or reconfigured.

The commit introducing this record is the attributable WU1 boundary; its identifier
and final Git state are reported at handback. No push is authorized or performed.
WU2 requires explicit WU1
acceptance and renewed accepted-source/SR-021 seam accounting before extraction
begins. Deferred public-model/platform/release decisions still do not block the
foundation itself.
