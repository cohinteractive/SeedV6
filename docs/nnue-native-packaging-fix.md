# Conventional NNUE native packaging repair

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
