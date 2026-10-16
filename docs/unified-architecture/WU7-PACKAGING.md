# WU7 unified native application packaging

16 October 2026 (Pacific/Auckland). The owner accepts WU0-WU6, including WU6
`328531f672260cf4fd5e49b26724fee7329bb0d8`. WU7 belongs only to the migration
worktree/branch. The owner additionally permits `C:/Temp/SeedV6 WU7 validation`
for disposable outside-repository tests. No graphical launcher is executed by
Codex. Native Mac execution, interactive GUI and external-client acceptance
remain open; implementation/commit is not full cross-platform acceptance.

## Build commands and private model input

Use Gradle 8.11.1, Python 3.11+ and a native Java 21 JDK containing jpackage,
jlink, javac and jdeps. Windows requires **21.0.6 or later 21.x** because Java
21 GA's native launcher fails forced subprocess termination. Initial WU7 validation
used the already-installed Oracle **21.0.7+8-LTS-245**, without changing system Java.
This is a packaging correctness requirement, not a dependency/library upgrade.
The observed failure matches the older launcher implementation; OpenJDK's
[21.0.6 launcher](https://github.com/openjdk/jdk21u/blob/jdk-21.0.6%2B7/src/jdk.jpackage/windows/native/applauncher/WinLauncher.cpp)
uses a Windows Job Object to terminate its child when the parent exits.

### Windows: one-time private setup, ordinary wrapper commands

17 October 2026 local-build follow-up: create ignored
`build-assets/packaging.properties` with an installed native x64 JDK path, using
forward slashes (or escaped backslashes) in Java properties syntax:

```properties
windowsPackagingJdk=C:/java/jdk-21-packaging
```

Windows native tools select `-PwindowsPackagingJdk` first, then this local
property, then the Gradle daemon's Java home when neither is configured. An
explicit invalid selection fails; it never falls back to another JDK. The
existing Java 21 / minimum 21.0.6 / host / architecture / tool checks remain.
The local file is a tracked configuration-cache input, including its creation,
change and deletion. It is ignored by Git and is not a shipped application input.

This selects jpackage, jlink, jdeps and the packaging probes independently of
Gradle's daemon and normal Java compiler toolchain. It does not override
`org.gradle.java.home`, change JAVA_HOME/PATH, or edit global Gradle properties.
A project `gradle.properties` entry would not override the user's global Java
home. The local follow-up was validated with the global daemon JDK still at
`C:/java/jdk-21` (Java 21 GA), and native tools/runtime at
`C:/java/jdk-21-packaging` (Temurin 21.0.12.1).

Restore the approved model once to ignored
`build-assets/production/network.brn-pair2`, checking the SHA-256 below both at
the local source and after copying. Keep the source unchanged. No remote fetch
or alternate evaluator is available. This checkout's private JDK setting and
approved local model are already configured. Then run either shell command:

```powershell
.\gradlew.bat packageWindows
```

```sh
./gradlew packageWindows
```

Both commands build GUI and UCI candidates. Module tasks
`:app-gui:packageWindows` and `:app-uci:packageWindows` remain available.
The owner's local-build amendment permits these ordinary Windows invocations
to use the unchanged default Gradle user home. Other migration builds retain
their isolated-user-home convention in [CONTRACT](CONTRACT.md).

### Mac and shared input conventions

Mac packaging is unchanged. A Mac checkout can keep its session setup in ignored
`build-assets/macos-env.sh`, then source it before using the existing tasks:

```sh
# Contents of build-assets/macos-env.sh (local paths only):
export GRADLE_USER_HOME="$PWD/build/migration/gradle-user-home"
export JAVA_HOME='/path/to/self-contained-arm64-jdk-21/Contents/Home'
# Restore the approved model to build-assets/production/network.brn-pair2 once.
```

```sh
. ./build-assets/macos-env.sh
./gradlew packageMac
./gradlew :app-gui:packageMac
./gradlew :app-uci:packageMac
```

These exports affect only that shell session; no global configuration is edited.
On either platform model selection remains `-PproductionModel` first, then
`SEEDV6_PRODUCTION_MODEL`, then `build-assets/production/network.brn-pair2`.
Explicit missing, corrupt or incompatible models fail even when the default
local file is valid. The canonical runtime loader still checks the approved
checksum, schema/layout, CRC and finite values; no validation is bypassed.
`-PpackagingPython=/path/to/python3` overrides the interpreter. Inputs must be
explicitly restored by the owner; the build never downloads or substitutes models.
Do not build or package concurrently with another build/test in the same checkout.
Packaging freezes source identities and refuses source drift. Separate platform
builds must use the same coordinated source revision, version, model and core inputs.
The default candidate identifier is unique on each invocation, including cached
Gradle configurations. `-PcandidateId=local-validation-01` chooses a reproducible
destination name; occupied destinations are refused, never overwritten. The same
ID may be used by independently invoked GUI and UCI tasks when each respective
destination is empty. No command increments VERSION_STATE or declares a release.

The canonical identity/validator is `engine-core`'s `core.brnpair2.ProductionModel`.
The existing UCI class delegates to it and retains JAR-relative path resolution.
The approved model is 795,032 bytes, SHA-256
`5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7`, generation 32,
lineage `235d0688-e060-44e3-8f1e-119c39bc8017`. The exact bounded bytes undergo
SHA-256 and existing BrnPair2Codec schema/layout/CRC/finite-value validation. No
codec, calibration, search, training algorithm or model format changes.

The source repository is public. Model bytes are **not committed or published**.
The historical UE path is never hard-coded as a build default; its verified bytes
may be copied to the ignored local input by an authorized operator. Fresh
checkouts lack the model and private JDK configuration. An authorized, validated asset-restoration/publication
mechanism remains a release gate; clean-clone reproducibility is not claimed.

## Candidate layout and production composition

| Application | Windows candidate parent | macOS candidate parent |
| --- | --- | --- |
| GUI | `dist/windows/candidates/ID/` | `dist/mac/candidates/ID/` |
| UCI | `releases/candidates/windows-x64/ID/` | `releases/candidates/macos-arm64/ID/` |

Each final directory contains its complete app image, independently portable ZIP,
and `candidate.json` containing archive and manifest SHA-256 values. Windows
launchers are `SeedV6-GUI/SeedV6-GUI.exe` (windowed) and
`SeedV6-UCI/SeedV6-UCI.exe` (console). Mac GUI is `SeedV6-GUI.app`; Mac UCI is
`SeedV6-UCI/SeedV6-UCI`, an executable shell launcher using its bundled runtime
with `exec`, suitable for a subprocess without Finder. Keep the entire app image.
Mac ZIP extraction must preserve executable permissions (e.g. `ditto -x -k`).

GUI includes app-gui, engine-core, engine-training, FlatLaf 3.7, JNA/platform
5.17.0, Gson 2.11.0 and its existing annotations dependency, Zstandard JNI
1.5.7-20 and SQLite JDBC 3.53.4.0. It uses the original SwingLauncher, artwork,
preferences and Play/Network Training/Arena services. Its runtime retains the
existing default jpackage module set plus jdk.incubator.vector, including
java.desktop, java.sql and jdk.unsupported. Only a separate desktop-package.jar
contains build timestamp/platform resources; engine-core.jar is never stamped.

UCI contains **only app-uci.jar and engine-core.jar**, the pinned model, launcher,
java.base-only runtime, licenses and candidate metadata. It has no GUI, training,
research or legacy implementation. Its default model is relative to the installed
JAR, never the invoking cwd; `--model` retains explicit validated override behavior.
GUI carries the same pinned payload for identity/inference verification but keeps
the accepted user-selected evaluator/checkpoint workflow and preferences. A runtime
payload is not a training checkpoint and cannot reconstruct optimizer state.

JNI libraries stay inside their original dependency JARs and are loaded by the
existing libraries. Headless packaging verification performs real Zstandard and
SQLite operations and loads JNA from the bundled runtime. Java runtime legal files
and intact dependency license resources are retained. Existing optional Syzygy
support remains available with `-PwithSyzygy` and the host C++ compiler. It retains
the existing native library embedding in engine-core.jar; such an opt-in core is
platform-specific and cannot be declared byte-identical to another platform.
Default core JARs have stable ordering/timestamps and platform-neutral metadata.

Temporary legacy responsibilities remain in `:app`: Main dispatch, accepted legacy
HCE/checkpoint profile, compatibility Java distribution and research task aliases.
Root `packageLegacyWindows` and `packageLegacyMac` retain the original GUI snapshot
workflows and directories; direct `tools/package-windows.ps1` remains available.
They are not unified candidates and retain their historical validation limitations.
The unqualified root `packageWindows`/`packageMac` commands now build both new apps.
No legacy owner or historical release is retired in WU7.

## Integrity, failure handling and provenance

`gradle/native-packaging.gradle` is the common Gradle convention; Python standard
library tooling consolidates the existing jpackage/jlink mechanisms. It rejects
wrong host/architecture/JDK before compilation. Mac requires arm64 and a JDK
without external package-manager dylib dependencies. The GUI reuses the existing
iconset and Info.plist technique, with displayed 0.0.0 and CFBundleVersion 35.
No new packaging framework or dependency upgrade is introduced.

Every build works in a unique ignored `.staging-*` directory. JARs and model are
copied, validated, packaged, inspected, exercised, archived, independently extracted
and checked before the entire deliverable directory is promoted by an exclusive
native rename. Failures retain `FAILED_UNVERIFIED` diagnostics and no final
candidate. Existing destinations, including races at promotion, are protected.
Earlier candidates/releases are never cleaned by these tasks or by Gradle clean.

`build-manifest.json` records full Git revision/status and working source hashes,
canonical version, shared core JAR hash, accepted cumulative SR-024 configuration,
model/schema/calibration identities, compiler JDK, packaging JDK, platform/arch,
file bytes/hashes/modes and actual headless checks. The shared engine identity is
separate from native packaging identity. Experimental core binary seams remain
compiled as accepted in WU6; production activation still excludes SR-021.

Root packaging compares actual GUI/UCI core/model/configuration, not displayed
version labels. Across platforms, use the verifier's `--peer` option. A core hash
mismatch fails and records both identities for investigation; matching versions
alone never establish parity. Toolchain changes, dirty-versus-clean revision
resources, optional native core entries and resource newline differences are
possible causes to inspect, not automatic waivers. Public release naming, model
distribution, signing/notarization and release acceptance require later decisions.

```text
python -B tools/test_native_packaging.py -v
python -B tools/verify-native-candidate.py --candidate <candidate-directory> \
  --work <new-disposable-directory-with-spaces> --jdk <native-jdk-21> \
  --peer <other-candidate/build-manifest.json>
```

The verifier checks receipts, image/archive hashes, native architecture, runtime
modules/dependency closure and both copied-image and extracted-ZIP execution with
empty PATH and no JAVA_HOME. GUI checks are headless resources/JNI only. UCI checks
exercise protocol flushing, position/depth/time controls, Threads/Hash/Clear Hash,
workers 1/4/16, stop/quit/EOF/process termination and model failure diagnostics.
Build-time probe classes are never shipped. Evidence remains local at the chosen
work directory; no upload is performed.

## Windows and preservation evidence

Final default candidates are `dist/windows/candidates/wu7-windows-final/` (GUI)
and `releases/candidates/windows-x64/wu7-windows-final/` (UCI), each containing the
image, ZIP and receipt described above. Their common core SHA-256 is
`adaf356401bc31b61b8b3a28e77a65aed6fcd17b85488afa40a8bf4e3696b886`;
a forced fresh core build reproduces it. Both include the approved model.
The manifests truthfully record WU6 HEAD plus dirty working-source hashes;
these are local WU7 validation candidates, not a claimed clean release build.

Both candidates pass copied-image and extracted-ZIP verification outside the
repository under the authorized `C:/Temp/SeedV6 WU7 validation` directory, with
spaces, unrelated cwd, empty PATH and no JAVA_HOME. The native UCI lifecycle and
model-negative suite runs on every image/extraction. The GUI probe loads JNA,
Zstandard and SQLite from its own runtime and checks presentation/native resources,
without starting Swing. Independent optional-Syzygy GUI/UCI candidates also pass;
their core differs only by the native DLL and Fathom license, with identical
portable entries. No optional-native/default or Windows/Mac byte parity is inferred.

Nine portable packaging/safety tests, fifteen Gradle architecture fixtures and
fifteen disposable finalizer tests pass. Mac task graph/configuration, launcher,
icon/plist/jlink construction, wrong-host rejection and four real arm64 JNI payload
headers are checked. Native Mac execution remains outstanding. Missing/corrupt
model and occupied-output checks fail as intended without a verified final output;
configuration-cache replay retains collision protection. Historical source,
artifacts, indexes/unfinished UE merge and size/mtime inventories remain unchanged.

The available headless regression selection executes 1,829 JUnit cases: **1,771
passed, 19 inherited failures, 39 existing skips**. This includes all ordinary
core/training/GUI/UCI/legacy/research module tests and selected root GUI, UCI,
production search, checkpoint and TrainingSession integration tests. The 19 names
and messages exactly match WU6; no test is suppressed. Expensive unrelated root
training suites and existing slow-NNUE opt-in tests are not rerun. The WU6 transient
TrainingSession timeout does not recur in this serialized run; its cause remains
unknown. This is not a claim that the entire unfiltered suite passes.

All 203 frozen SR-024 vectors match all 17 fields. The cloned generation-32 model
recompiles exactly and all seven persisted optimizer families continue exactly.
Legacy UCI passes 66 requests (ten exact comparisons). The historical protocol
suite executes 128 searches through the actual relocated Windows native UCI
executable. The separate standalone suite covers 159 searches, all workers 1..16,
Hash bounds/allocation recovery, strict positions and accepted production routing.

The production class comparison preserves all 317 training and 220 GUI classes,
173 pre-existing core classes and 13 UCI classes unchanged. Only ApplicationVersion,
the UCI model compatibility facade and the new canonical core model owner differ.
Search/evaluator/trainer algorithms and GUI resources remain unchanged.

Initial adverse attempts are retained: Java 21 GA leaves a launcher child after
parent termination; the GUI allowlist initially omitted Gson's existing annotation
dependency; Python ZIP extraction initially failed to restore a read-only launcher
attribute. All were corrected and retested. An apparent installed Temurin directory
contained no JDK; validation instead uses the available Oracle 21.0.7. No failing
attempt was promoted to a final candidate. An initial historical native test harness
omitted its frozen TSV input; the corrected harness reads that immutable input
without changing assertions. Exact artifacts, later safety/configuration checks and
finalization are recorded in the evidence JSON. Same-token `no_bump` finalization
completed without warnings or blockers; VERSION_STATE bytes remain unchanged at
0.0.0 build 35. No root journal exists and none was created.

## Local Windows configuration follow-up: 17 October 2026

The owner identifies `592c6c93e4be355f02f09bd77149647463deb709` as the last accepted
packaging commit and authorizes this bounded local-build improvement, without a
version bump or further migration admission. Only Windows JDK selection, its
Gradle regression fixture and these migration records change. The Python
packager, model validator, wrappers, compiler convention, macOS packaging and
application sources are unchanged. Existing `/build-assets/` ignore coverage
protects both local inputs; no model bytes or private configuration are tracked.

Executed from the migration worktree with inherited JAVA_HOME still pointing at
Java 21 GA, no GRADLE_USER_HOME or model override, and the unchanged global
`org.gradle.java.home=C:\\java\\jdk-21`:

| Invocation | Result | Fresh candidate ID (both GUI and UCI) |
| --- | --- | --- |
| Git Bash `./gradlew packageWindows` | PASS, 34.58 seconds | `candidate-20261016T215015-c6b16d41` |
| PowerShell `.\gradlew.bat packageWindows` | PASS, 29.47 seconds; configuration cache reused | `candidate-20261016T215044-a55cee2d` |

Each ID exists under both Windows candidate parents documented above, with image,
ZIP and receipt. Both invocations use packaging/runtime Java **21.0.12.1** and
compiler Java **21**. All four images contain engine-core SHA-256
`f156d3d1633b1fbf2829b03e9fb250107b2ead9db7c76e33c40b171261084651`
and the approved model SHA-256 above. Manifests record the accepted WU7 revision
plus exact dirty source hashes; these are local validation candidates, not clean
release identities. Subsequent tracked edits only complete documentation.

Every image and independently extracted ZIP passed runtime/module/dependency,
native architecture, integrity and relocatability checks with empty PATH and no
JAVA_HOME. GUI checks execute headless JNA/Zstandard/SQLite probes without Swing.
Native UCI checks pass workers 1/4/16, protocol/depth/timing, stop/quit/EOF, forced
subprocess termination, and missing/corrupt/incompatible model diagnostics.

Additional executed validation:

- `python -B tools/test_native_packaging.py -v`: all nine tests pass.
- `python -B tools/test_gradle_architecture.py GradleArchitectureTest.test_windows_packaging_jdk_is_local_and_cache_tracked`:
  passes six real Gradle fixture invocations covering absent/created/changed/
  deleted local configuration, cache reuse, explicit JDK precedence, paths with
  spaces, unchanged daemon selection and unchanged Mac JDK selection.
- `:app-uci:packageWindows -PproductionModel=<verified-local-source>` passes even
  with an invalid environment model; `:app-uci:packageWindows` with a valid
  SEEDV6_PRODUCTION_MODEL also passes. Both create independent verified candidates.
- `prepareWindowsPackaging -PwindowsPackagingJdk=C:/java/jdk-21` rejects Java 21
  GA. `packageWindows` with either explicit missing-model selector rejects it
  despite the valid default. UCI packaging rejects incompatible length and a
  full-sized checksum-corrupt copy before promotion. Reusing the Git Bash
  candidate via `packageWindows -PcandidateId=...` rejects the occupied output.
- Hash/size/mtime verification preserves all **6,117** pre-existing output files
  (**3,808,765,997 bytes**) under this worktree's dist/releases, and independently
  rechecks the new ordinary candidates after the collision attempt. Global
  Gradle property bytes, user/system/process Java/PATH settings, both JDK release
  files, the historical model and VERSION_STATE bytes are unchanged.

Raw local evidence, exact commands, receipts and manifests are retained under
`build/migration/wu7-local-windows/` (`ordinary-commands.json`,
`selection-and-safety.json`, `before.json`, `preservation.json` and command logs),
the existing Gradle fixture directory and the candidate directories. Failed
staging remains unverified and preserved. This evidence is ignored local output;
a fresh checkout does not contain it.

**Non-blocking version warning:** the pinned local finalizer's `begin` full-tree
capture was stopped after approximately five minutes before it created an
operation/token/reservation. This uses SeedV6's non-critical bookkeeping boundary;
it is not a successful finalization or a helper-issued terminal warning receipt.
Subsequent `status` reports idle, no active/unfinished operation and no blockers.
No finish or replacement bump was attempted. The direct before/after SHA-256 of
VERSION_STATE remains
`92a26d4e187edc6c47ed88100d355f77b4597d98524d21d005a3c164986b4bca`
(0.0.0 build 35). No root journal exists or was created.

No full application regression suite, Mac-native execution, graphical walkthrough,
external-client acceptance, remote model restoration or research campaign was
performed in this follow-up. Existing acceptance gates below remain unchanged.
Human actions required for the configured local Windows commands: **None**.
Fresh checkouts still need an installed suitable JDK, Python and a one-time
verified local model/configuration setup as described above.

## Acceptance boundary and continuation

See [wu7-evidence.json](wu7-evidence.json) for exact executed commands, artifacts,
test counts, adverse attempts, preservation checks and no-bump finalization.
Only native Windows/headless evidence is available in this invocation. Portable
tool tests and Windows wrong-platform rejection do not certify Mac execution.

Native Mac continuation must build both arm64 candidates at a coordinated clean
source identity; rebuild Windows at that same identity for comparison. Verify
runtime/native architecture, standalone dylib dependencies, archive executable
permissions, copied/extracted relocation, UCI model/protocol/stop/EOF/signals and
actual core/model agreement. Use the exact artifacts for deferred user GUI tests.
Mac signing/notarization remains undecided for public distribution.

Human Windows and Mac walkthroughs must cover Play move entry/response/reset/side
bindings, Training selection/start/stop/resume/progress, Arena setup/live/history,
preferences, native pickers, layout/scaling and shutdown during active work, using
disposable state. Real Arena/BotLi/client integration remains deferred; no client or
Lichess game is launched here. The 19 inherited GUI/checkpoint failures retain
their existing acceptance gates. WU6's transient concurrent timeout remains an
unresolved diagnostic observation; a passing repeat does not prove its cause fixed.

Recommended next unit: GPT/user reconciliation of WU7 implementation/evidence,
then native Mac packaging validation and owner GUI/client acceptance, with separate
inherited-failure remediation. **Do not advance WU8 cutover across these gates.**
