# Conventional NNUE native packaging repair

The [24 October Build 74 identity repair](#build-74-production-identity-repair---24-october-2026)
below records the subsequent CP64 selectivity packaging regression. Earlier
platform results and version/acceptance statements retain their historical scope.

Current consolidation (23 October 2026): the normal competitive baseline is now
Construction V1 + Conventional NNUE CP64; see the
[construction qualification record](research/search/P3_CONSTRUCTION_2026-10-20.md#construction-v1--cp64-consolidation-23-october-2026).
The E008-specific configuration, launchers and historical results below are
preserved. Earlier ordinary-default/GUI-worker-limit statements describe the
pre-consolidation state. Native Mac evidence below does not qualify this new build.


23 October 2026, Pacific/Auckland. Native Apple Silicon packaging and automated
runtime acceptance passed on an M1 Mac with Temurin 21.0.12.1+1 and Python 3.9.6.
Starting branch was clean `main` at `64eff9a51006`; changes remain uncommitted.

## Root cause and scope

`gradle/native-packaging.gradle` delegates to `tools/native_packaging.py`. The
reported Gradle line is the subprocess invocation, not the module policy itself.
Mac UCI assembly ran `jdeps` over both complete runtime JARs and required exactly
`java.base`. It then linked only that module. Runtime verification also required
base alone; Windows UCI assembly likewise requested only base.

That restriction originally enforced a small headless Gen32 runtime. It is now
an outdated assumption about the shared engine-core JAR, which also contains the
conventional NNUE Vector API kernel. Actual `jdeps` output is
`java.base,jdk.incubator.vector`. This is not a separate legacy-distribution gate:
the retained `packageLegacyMac` GUI path already includes/resolves Vector.

The fix preserves the useful boundary by requiring exactly base and Vector in
both the static dependency check and the bundled UCI runtime. Unexpected modules,
GUI/training JARs, and forbidden owner classes remain errors. No dependency check
is suppressed and no unsupported module is silently discarded.

`SeedV6-UCI` remains the Gen32 default; its Mac launcher bytes are identical to
HEAD. The companion `SeedV6-NNUE-CP64` selects the existing dedicated
`NnueCandidateUciMain`, explicitly resolves Vector, and retains the source
candidate's heap/processor settings. This avoids routing the qualified candidate
through ordinary `UciMain --nnue`, which is the different research control.
Windows uses an additional console launcher with the same contract. Both
launchers share the same JARs/runtime. The native image carries the candidate
guide/attribution and frozen network manifest.

No evaluator, checkpoint, search algorithm, model bytes, Gradle task wiring,
portable launcher, or BotLi configuration changed. S6CNU002 checkpoint 64, eight
heads, E008, scalar fallback and Gen32 are preserved.

## Files changed

| File | Change |
| --- | --- |
| `tools/native_packaging.py` | Exact module dependency policy, jlink/jpackage runtime closure, companion launcher, candidate documentation, runtime verification integration. |
| `tools/native_package_checks.py` | Exact runtime-module check, Windows companion config/console validation, short NNUE identity/search/fallback/termination gate; only the exact incubator warning is allowed for NNUE. |
| `tools/test_native_packaging.py` | Mac/Windows assembly checks and negative module/owner-boundary regressions. |
| `docs/nnue-botli-candidate.md` | Native invocation, executable selection and validation scope. |
| `docs/unified-architecture/WU7-PACKAGING.md` | Current two-module UCI runtime and companion launcher. |
| `docs/nnue-native-packaging-fix.md` | This validation record. |

## Validation

All Gradle commands used `./gradlew --offline --no-daemon --max-workers=1`.

| Check | Result |
| --- | --- |
| Unmodified `:app-uci:packageMac` | Reproduced the exact `Standalone UCI must require java.base only: java.base,jdk.incubator.vector` failure. |
| Fixed `:app-uci:packageMac` | Passed in 24 seconds, including real image and independently extracted ZIP launches. |
| Final coordinated `packageMac` | Passed in 49 seconds; both native applications and ZIPs verified, identical engine-core/model/configuration; GUI resources/JNI verified headlessly. Includes the final guide/manifest copies. |
| Bundled runtime | Exactly `java.base@21.0.12.1` and `jdk.incubator.vector@21.0.12.1`; native arm64 Java/JVM and executable permissions checked. |
| Actual NNUE executable | Checkpoint 64, S6CNU002, eight heads, both pinned hashes, full E008 identity and one-worker limit verified. Normal backend on M1: `vector-128-long`. |
| NNUE search/fallback | Nine depth-3 searches per image/extracted ZIP: three roots in normal, `--scalar`, and module-absent modes. Scores, nodes, PVs and legal bestmoves match; startpos gives `bestmove d2d4`, pawn ending `e1d2`, promotion ending `a7a8q`. Quit/EOF exit 0 and protocol streams close. |
| Existing native Gen32 | 63 searches per image/extracted ZIP; workers 1/4/16, book behavior, time controls, stop, quit, EOF, signal termination and bad-model rejection pass. |
| Source distributions | `:app-uci:installNnueCandidateDist :app-uci:installDist` pass. Relocated dedicated NNUE launcher passes nine searches and all three execution modes; ordinary Gen32 passes 63 searches with its required model supplied. |
| Selected Java tests | `:app-uci:test --tests '*NnueCandidateTest' --tests '*StandaloneBoundaryTest' -PconventionalVectorTests`: 10 passed, zero failures/skips. Includes 259 independent reference positions across all eight heads, E008 settings, depth-5 rebuild parity and asset rejection. |
| Python packaging tests | `python3 -B -m unittest discover -s tools -p 'test_native_packaging.py' -v`: 17 passed. |
| JDK selection tests | Same command with `test_packaging_jdk.py`: 10 passed. |
| Preservation | Gen32 Mac launcher bytes unchanged; engine/app/Gradle/model/version inputs unchanged in Git; `git diff --check` passes. |

The initial ad hoc Gen32 source-distribution fixture omitted its documented model
file and failed startup. This was test setup, not a product regression. That
fixture/failure record is retained; a separate fixture with the required pinned
model passed. Dedicated NNUE needs no external model and passed immediately.

## Artifacts and local evidence

Final coordinated candidate: `candidate-20261022T200833-c2f000f0` (UTC identifier).

- NNUE executable: `releases/candidates/macos-arm64/candidate-20261022T200833-c2f000f0/SeedV6-UCI/SeedV6-NNUE-CP64`.
- Gen32 executable: same image's `SeedV6-UCI` launcher.
- UCI archive: same candidate directory's `SeedV6-UCI-macos-arm64.zip`.
- GUI image/archive: `dist/mac/candidates/candidate-20261022T200833-c2f000f0/`.
- Earlier successful UCI candidate: `releases/candidates/macos-arm64/candidate-20261022T200549-a5e0a650/`.

Each candidate retains `candidate.json`, `build-manifest.json`, file hashes,
source fingerprints, JDK identity and original/extracted runtime transcripts.
Both applications use engine-core SHA-256
`872607a5591d89d3101c893cee812c69984dddc4eb305b8449a9dc2ca7430185`.
The frozen NNUE file SHA-256 remains
`8431626c2fca81e0a1e9ccd5e45ddd8829e6e3282c756c21fad75b06d075bb2b`.

Local-only evidence is under `build/packaging/nnue-mac-repair/`: `before.log`,
`packageMac.log`, `coordinated-packageMac.log`, `source-tests.log`,
`packaging-tests.log`, `jdk-tests.log`, `summary.json`,
`source-distribution/nnue-result.json`,
`source-distribution/gen32-initial-failure.json` and
`source-gen32-with-model/result.json`. The original native failure remains at
`releases/candidates/macos-arm64/.staging-candidate-20261022T200225-b28baa80-858c6a31ffb24ea7a4a4ae43265b7c6c/attempt.json`.
These paths were present when analyzed and are Git-ignored; a fresh checkout
does not contain them. No raw output is staged or force-added. This report was
written after validation; completed candidate source manifests are unchanged.

## Git state and remaining actions

Five tracked files modified and this report newly untracked on `main`; no staged
changes, commits, push or deployment. There were no pre-existing local edits.
`VERSION_STATE.txt` remains build **65**, as required for uncommitted work. Any
later qualifying commit must increment build to **66** in that same commit.
No finalizer was invoked.

Native Mac automated packaging acceptance is complete. Windows command/config
coverage is portable unit coverage only; actual Windows native assembly/launch
still needs a Windows host. Interactive GUI acceptance and live BotLi activation
remain separate owner actions. No training, tournament, network activity,
full-suite execution, fresh strength claim, installation replacement or live
BotLi configuration change was performed. Review/commit is the remaining source
integration step; keep the complete image together when using either executable.

## Build 74 production identity repair - 24 October 2026

Bounded remediation of accepted `d9a91cbe8b52caf6f3031e17d2ea5561c1064fa0`,
starting from clean `main`, version 0.0.0 build 74. The owner reports both Project
Source mirrors synchronized to P3-R028/P3-F028. No new research is admitted.

### Diagnosis and correction

`gradle/native-packaging.gradle:24-34` invokes `tools/native_packaging.py package`;
line 34 propagates the Python exit, not the defect. In the starting revision,
`native_package_checks.py:225` still required `flags=262143`, and
`native_packaging.py:397` wrote the same obsolete `productionSearch.flags` into
both manifests. These were packaging-local literals, not values supplied by
Gradle, the production model manifest or the packaged engine.

The exact unmodified `gradlew.bat packageWindows --console=plain` reproduced
successful preflight, UCI failure `Wrong baseline search/evaluator identity`,
and GUI `VERIFIED_LOCAL_CANDIDATE`. The retained failing candidate is
`candidate-20261023T195311-29a5200e`; its failed UCI image is under
`releases/candidates/windows-x64/.staging-candidate-20261023T195311-29a5200e-a765f92889e242258875d99fde9d23a6/`.

Directly starting that image's `SeedV6-UCI.exe` with no arguments and empty PATH
reported the correct default: `baseline=construction-v1-cp64`,
`search=construction-v1`, **flags3407871**, flattened original18 plus
NOMINAL_FUTILITY/VERIFIED_NULL, ADAPTIVE16, workers1/Hash64 (Hash8 after setting it),
CONVENTIONAL_NNUE, checkpoint64, S6CNU002/eight heads, BT4_Q_LOGIT400_V1.
Network SHA-256 is `8431626c2fca81e0a1e9ccd5e45ddd8829e6e3282c756c21fad75b06d075bb2b`;
payload SHA-256 is `5da0253642ab33b8e446b5b1c5764750be008cd3dbacc7cd1992a95bf4a881f6`.

The actual config selects `com.ohinteractive.seedv6.uci.UciMain` with exactly
`app-uci.jar` and `engine-core.jar`. `UciMain:16` selects
`BaselineUciConfiguration.load`, which delegates to `BaselineEngine.load`;
`BaselineEngine:42-52` correctly binds adoption to frozen CP64. The companion
`SeedV6-NNUE-CP64` remains the separate recursive E008 comparison launcher.
`native_packaging.layouts` / `verify_runtime` select the ordinary launcher for
both platforms and call the same `verify_uci` function. Thus macOS has the same
stale assertion, although its reported failure was not executed on this Windows host.

The correction is limited to the two Python packaging modules and their tests:

- Pin flags3407871 and the complete accepted Search/evaluator identity independently
  of candidate output. Require exact unique fields from one baseline identity,
  including configuration, network/payload hashes and score mapping; diagnostics
  cannot supply missing fields or conceal a different identity.
- Check the actual UCI cold default before setting any options, then recheck
  configured workers/Hash. Keep protocol, legal-move, lifecycle, dependency,
  model rejection, archive and relocation gates intact.
- Independently verify the baseline probe for GUI and UCI before assembly, and
  write the same pinned production flags into both package manifests. The retained
  BREP2 asset manifest is still checked separately, not relabelled as CP64.
- Add five focused tests with independent accepted fixtures and negative changes
  to flags, configuration, evaluator, network, mapping, options, duplicate/missing
  fields, diagnostic contamination and cold defaults. Valid legacy model metadata
  cannot authorize an obsolete baseline in either application.

No Java source, mechanism, evaluator, TT policy, runtime closure or launcher
recipe changed. HCE/BREP2-32/unrelated conventional payloads still use flags262143;
explicit historical Construction and E008 identities remain separate.

### Executed validation and evidence

All new raw evidence is local under `build/packaging/identity-repair-20261024/`;
these files and candidate paths were present and verified ignored before staging.
They are not available from a fresh checkout. Failed evidence is retained.

| Check | Result / evidence |
| --- | --- |
| Before repair | Exact Windows task fails as reported; `before-packageWindows.log`, `before-runtime.json`. |
| Portable Python tests | 22 packaging + 10 JDK-selection tests pass on Python 3.13.4; `packaging-tests.log`, `jdk-tests.log`. New regression tests were also run before implementation (`regression-before.log`, failing). Windows and macOS assembly fixtures pass; Python 3.9 runtime was not available/executed. |
| After repair | Exact `gradlew.bat packageWindows --console=plain` passes, including native preflight, both application tasks and coordinated identity comparison; `after-packageWindows.log`. |
| Native UCI / extracted ZIP | Each passes 63 searches at workers1/4/16, cold Hash64, configured Hash8, depth/timed/clocks/stop/book/position/quit/EOF/termination and damaged-model rejection. Each companion E008 passes nine searches with normal/scalar/module-absent equivalence. |
| All native worker options | 32 completed depth3 searches at every worker1-16 with Hash1 and Hash64; identities, legal starting moves, Clear Hash, stop/quit, and invalid Threads0/17 / Hash0/1025 rejection with prior settings retained pass. `verify-native-workers.py`, `native-all-workers.json`. |
| Java/build | 183 engine-core + 20 UCI + four selected GUI + two selected Arena tests pass, no failures/skips. `assemble` and UCI `installDist` pass. `java-validation.log`, `java-test-summary.json`. |
| GUI packaging | Original/extracted headless resources, JNI, module/classpath closure and baseline identity pass; no interactive GUI walkthrough. |
| Preservation | Before/after core JAR entries differ only in generated `application-version.properties` (dirty-source provenance); Search classes and model bytes are identical. `candidate-summary.json`. |
| macOS | Shared logic and portable assembly fixtures pass on Windows. No native macOS task or executable was run; independent Apple Silicon validation remains required. |

Java command (worktree-local Gradle home, two workers, no downloads):

```text
gradlew.bat --gradle-user-home build/migration/gradle-user-home --offline --no-daemon --max-workers=2 --console=plain :engine-core:test :app-uci:test :app-gui:test --tests '*BaselineScoreTest' --tests '*ConstructionGameplayTest' :engine-training:test --tests '*BaselineArenaTest' --tests '*ConstructionArenaTest' assemble :app-uci:installDist -Pheadless -PconventionalVectorTests
```

The full root integration/research suite was not rerun for this packaging-only
change. The adoption record's three inherited supplemental failures in
EngineSearchAdapterTest/LearningArenaLifecycleTest are not remediated or claimed
passing here. The optional `verify_cp64_production.py` distribution harness was
not run (default Python lacks `chess`); native all-worker checks above and the
Java UCI suite supply the relevant configuration coverage without that dependency.

Successful coordinated candidate: **candidate-20261023T195552-c00e3315**.
UCI image/archive/receipts are under `releases/candidates/windows-x64/` plus that
ID; GUI equivalents are under `dist/windows/candidates/` plus that ID. Both
core JAR hashes are `facac0db65c7f97a120083174ea4de20ef210daf3252a79a3fd5eb2cac99e6f7`.
UCI archive SHA-256: `0e5ae5d3e66599aa172e1cbb503955904d6a860290985de1df422610ad95bf9d`.
GUI archive SHA-256: `9bb3f9fefbad035149106bbf1339f31cbd4de8124a59f8b6fca552f59d8b80f4`.
Manifests truthfully record d9a91cbe8b52 plus the three uncommitted tooling/test
changes at packaging time. They are not relabelled as clean final-commit artifacts;
this report was added afterward. Full receipts retain hashes and transcripts.

### Completion boundary

This is verification/metadata repair for the existing accepted application, with
no new application build warranted: `VERSION_STATE.txt` remains byte-identical
at **0.0.0 build74**. One attributable commit contains the tooling, tests and this
record. No finalizer is used; root CODEXLOG_CURRENT.md is absent and not created.
Neither Phase 3 master changes; **PROJECT_SOURCE_REFRESH_REQUIRED: NO**.
No push, deployment, external GUI/BotLi installation or successor CGLHW occurs.

Human actions required after this prompt:

- Run `./gradlew packageMac` from the repair commit on supported Apple Silicon
  macOS and retain its candidate receipts/transcripts. This blocks macOS runtime
  acceptance only; it does not block completion of this Windows-validated repair.
  Prior Mac packaging results above do not certify the repaired revision.
