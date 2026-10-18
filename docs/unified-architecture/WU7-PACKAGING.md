# WU7 unified native application packaging

16 October 2026 (Pacific/Auckland). The owner accepts WU0-WU6, including WU6
`328531f672260cf4fd5e49b26724fee7329bb0d8`. Initial WU7 work belonged to the migration
worktree/branch; the portable follow-up below uses the independent unified `main`.
The owner additionally permitted `C:/Temp/SeedV6 WU7 validation`
for disposable outside-repository tests. No graphical launcher is executed by
Codex. The [19 October recovery validation](MACOS-PACKAGING-RECOVERY-2026-10-19.md)
adds native Mac automated evidence. Interactive GUI and external-client acceptance
remain open; implementation/commit is not full cross-platform acceptance.

## Current portable build commands and tracked production input

17 October 2026: these instructions supersede the private setup used during WU7.
A fresh ordinary Git clone contains the wrapper, project configuration, approved
model and identity manifest. No Git LFS, historical checkout, manual model copy,
environment setup or ignored configuration file is required. Standard development
tools are Git, Python 3.9+, a shell and a complete installed native **Java 21 JDK,
21.0.6 or newer**. Gradle 8.11.1 is supplied by the wrapper; normal Gradle/dependency
downloads may require network access. JDKs and models are never downloaded by
the build. Java 21 GA, older patches, other Java majors, early-access JDKs, missing
native tools/JMODs and incompatible OS/architectures are rejected for packaging.
Windows supports x64; macOS supports arm64 with self-contained dylib dependencies.

### Build both applications

```powershell
.\gradlew.bat packageWindows
```

```sh
./gradlew packageWindows
```

On native macOS arm64:

```sh
./gradlew packageMac
```

For one application, use `:app-gui:packageWindows`, `:app-uci:packageWindows`,
`:app-gui:packageMac` or `:app-uci:packageMac`. Root tasks build both independently
and compare their complete engine/model identity. These remain local candidates.

### Installed JDK selection

Selection order is the platform Gradle property (`-PwindowsPackagingJdk` or
`-PmacPackagingJdk`), then generic `-PpackagingJdk`, then the corresponding platform
setting in optional `build-assets/packaging.properties`, then its generic
`packagingJdk` setting. Without a selection, discovery chooses the newest compatible
installed Java 21 patch (deterministic path ordering breaks ties). Explicit invalid
selections fail rather than falling back. Paths can contain spaces; quote the
entire `-Pname=value` argument. Mac overrides accept a `.jdk` bundle or its
`Contents/Home` directory.

Discovery examines the Gradle daemon home, JAVA_HOME/JDK_HOME, PATH Java,
`org.gradle.java.installations.paths`, user `.jdks` and existing Gradle `jdks`
caches. Windows additionally searches the system drive's `java` directory,
standard Program Files vendor directories (Java, Eclipse Adoptium, Microsoft,
Amazon Corretto, BellSoft, Zulu, Semeru), and JavaSoft JDK registry entries.
Mac uses `/usr/libexec/java_home -V`, system/user `Library/Java/JavaVirtualMachines`,
Homebrew's `openjdk@21` locations, SDKMAN's installed Java candidates and `/opt/java`.
Discovery runs on every preflight, even on configuration-cache replay; installing
a suitable JDK does not require deleting Gradle caches.

The wrapper still needs an installed Java that can launch Gradle. Gradle's Java
21 compiler toolchain remains independently selected and may use Java 21 GA.
All jpackage/jlink/jdeps, package probes and bundled runtimes use the validated
packaging JDK. The diagnostic prints both homes. A newer packaging JDK does not
change `org.gradle.java.home`; user-level Gradle properties take precedence over
project `gradle.properties` for that setting. No JAVA_HOME, PATH or global Gradle
settings are changed. See Gradle's [property precedence](https://docs.gradle.org/current/userguide/build_environment.html#sec:gradle_configuration_properties)
and [installed toolchains](https://docs.gradle.org/current/userguide/toolchains.html).
Automatic toolchain download is disabled and no resolver
is registered; unusual compiler installations can use Gradle's explicit
`-Porg.gradle.java.installations.paths` setting.

The local properties file is **optional** and ignored. Its only supported settings
are `packagingJdk`, `windowsPackagingJdk` and `macPackagingJdk`. Set the relevant
key to your installed JDK home using forward slashes or escaped backslashes in
Java properties syntax. Never commit machine-specific paths. Missing files mean
automatic discovery; creation, edits and deletion invalidate configuration-cache
inputs. All explicit command-line JDK properties precede local settings.
`-PpackagingPython=<python-executable>` selects a nonstandard Python installation.
macOS defaults to `python3`; Xcode's Python 3.9.6 is supported without an upgrade.
File integrity uses SHA-256 with bounded 1 MiB reads, and the UCI shell launcher
is written as exact UTF-8/LF bytes. Packaging and candidate verification use only
the Python standard library. See the [19 October recovery validation](MACOS-PACKAGING-RECOVERY-2026-10-19.md)
for current Mac evidence and acceptance limits.

### Production model identity and staging

The only tracked payload is `build-assets/production/bre-pair2-g32.brn-pair2`
(795,032 bytes). Adjacent `manifest.json` describes generation 32, lineage,
checkpoint, schema version 1, BRN_PAIR2 evaluator and unchanged quarter-residual
calibration. SHA-256:

```text
5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7
```

The manifest documents the existing `engine-core` ProductionModel authority;
it is not a second model registry. Preflight checks existence, exact size and
checksum. The Java build probe independently uses that authoritative loader to
verify its pinned checksum, format/schema/layout, CRC and finite values, and
compares all manifest identity fields against the compiled core. A manifest
change cannot silently authorize a different evaluator or payload.

Model selection on both platforms is `-PproductionModel`, then
`SEEDV6_PRODUCTION_MODEL`, then the tracked canonical file. An explicit missing,
corrupt or incompatible selection fails even if the tracked default is valid.
The old ignored `build-assets/production/network.brn-pair2` works only when
explicitly selected. Both applications receive the exact verified bytes at their
existing `models/baseline.brn-pair2` runtime location; image, ZIP and independently
extracted ZIP checks verify the model and same shared engine-core JAR. GUI model
preferences are preserved; bundling this asset does not force every GUI session
to use it. Search, model serialization and evaluator mathematics are unchanged.

Only the payload and manifest are unignored under `build-assets/production`.
Local settings, other build assets, generated packages, caches and raw research
outputs remain excluded. No training checkpoints, optimizer state, corpora or
datasets are added. Existing research retention rules remain unchanged.

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

The owner authorizes ordinary Git tracking of this approved payload in the unified
repository. Future clones obtain the exact bytes through Git; no historical UE
path is a build input. Pushing to a public remote would expose those bytes, but
this work does not push or publish. The earlier private-input restrictions below
describe historical WU7 runs and are superseded by the current portable setup.

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

## Historical local Windows configuration follow-up: 17 October 2026

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
At that point fresh checkouts needed an installed suitable JDK, Python and a
one-time verified local model/configuration setup. The portable follow-up removes
the model/configuration setup requirement; it preserves these run records.

## Portable build and tracked-asset validation: 17 October 2026

This work starts from clean independent unified `main` at
`11060c8df2e5fc4dcc7356f52e9ea4a6e18439f2`; no remotes are configured. The owner's
current request authorizes the model commit, portable configuration and headless
validation, with no version increment or push. The historical source payload was
verified before and after copying; ordinary Git stores exactly one production
model, with binary attributes preventing text conversion. No application source,
test, search/evaluator/training algorithm, serialized format or opening-book
behavior is changed. All 400 application source/test files retain their original
hashes. Historical repositories are read-only throughout.

Windows native tools automatically select installed Temurin **21.0.12.1** while
the daemon and compiler remain on **Java 21 GA**. No ignored local configuration
or legacy model file exists. Model inputs, manifests and generated ZIP/image
inventories pass the independent decoder/checksum/evaluator checks above.

| Executed validation | Exact result |
| --- | --- |
| Git Bash `./gradlew packageWindows` | PASS, 53 seconds; GUI/UCI `candidate-20261016T231443-0adaa26c` |
| PowerShell `.\gradlew.bat packageWindows` | PASS, 35 seconds; cache reused; GUI/UCI `candidate-20261016T231550-d382f23c` |
| `python -B tools/test_packaging_jdk.py -v` | 10 tests PASS; both platform version/architecture/tool checks, automatic newest-patch discovery, explicit valid/invalid selection, no-compatible-JDK diagnostic, Mac discovery and external-dylib rejection |
| `python -B tools/test_native_packaging.py -v` | 11 tests PASS; integrity, archive/extraction, model/manifest failures, occupied output, failed native assembly and Mac construction |
| `python -B tools/test_gradle_architecture.py -v` | 15 unchanged fixtures PASS; new selection fixture initially failed and was corrected, then passed separately as described below |
| `python -B tools/test_gradle_architecture.py GradleArchitectureTest.test_packaging_selection_is_portable_and_cache_tracked -v` | PASS, 10 actual Gradle invocations: default, local file creation/change/deletion, cache reuse, generic/platform explicit precedence, both platforms, environment/property model precedence and unchanged daemon |
| Explicit valid packaging JDK; explicit model with invalid environment model | PASS; independent UCI candidate `portable-explicit-uci` |
| Environment-selected approved model | PASS; independent GUI candidate `portable-env-gui` |
| Explicit Java 21 GA; missing explicit/environment model; corrupt and truncated model | Expected exit 1, clear diagnostics, no verified final candidate |
| `packageMac --dry-run`; `packageMac` on Windows | Dry-run PASS; native invocation expected exit 1 before compilation, requiring macOS |
| Reuse first candidate ID, then repeat with cached configuration | Expected exit 1 twice; all 503 files in the original candidate pair unchanged |

The initial selection-fixture failure exposed a provider closure that captured a
Gradle script object. Resolving the optional path during configuration removes
that configuration-cache incompatibility without caching JDK discovery. The
corrected selection fixture passes, as do subsequent native explicit builds and
both clean-checkout commands. No fixture assertion was weakened.

The disposable ordinary Git clone at `build/clone qa` contains only committed
source/assets from validation snapshot `2ecabe850b8cf15292b1a21452707ad387e7edb0`,
tree `5f72d2e0473c8900b31431598c7d904d74b2973d`. This unreferenced snapshot of the
reviewed index enables validation before the final attributable commit. All
implementation/assets/tests match the final change; subsequent edits only complete
documentation. Both its index object and checked-out model hash match the required
SHA-256. No model transfer, local configuration file, custom environment variables
or historical repository is used by either packaging command.

| Clean-clone command | Result and candidate ID |
| --- | --- |
| Git Bash `./gradlew packageWindows` | PASS, exit 0, 65.875 seconds; `candidate-20261016T232149-ebbd83f9` |
| PowerShell `.\gradlew.bat packageWindows` | PASS, exit 0, 32.007 seconds; `candidate-20261016T232252-4f5f0ae5` |

Both builds produce GUI and UCI images/ZIPs with core SHA-256
`a71d549d7fbad0c9430648142f04c040bc5a2773e52b9e196952db4e16d522f9`
and the approved model. Their manifests record a clean snapshot revision;
Git status is empty before and after. The working checkout's core hash is
`d5ca36f380744ecac09f05774b5d9aae460b401c459d7ae6f9d8ca64df9fb4df`;
comparison of every JAR entry shows only the embedded sourceRevision metadata
differs. Model identity, accepted cumulative SR-024 composition and all core inputs
match; application version stays **0.0.0 build 35**.

Each UCI image and independently extracted ZIP passes **63 searches**, workers
1/4/16, UseBook/OwnBook aliases and fallbacks, protocol/depth/clocks/stop/quit/EOF,
forced termination and missing/corrupt/incompatible model checks. Bundled runtime
verification uses empty PATH, no JAVA_HOME/JDK_HOME, unrelated working directories
and paths with spaces. GUI verification loads resources, JNA, Zstandard and SQLite
headlessly; no graphical launcher or real external client is opened.

The first disposable clone was nested too deeply under the evidence directory:
Windows Java rejected the extracted runtime path with `PATH_MAX`/JVM-load errors.
Both attempts remain `FAILED_UNVERIFIED` with no final candidates. Repeating from
the shorter `build/clone qa` directory passes. This is an existing native Windows
path-length constraint; use a reasonably short checkout path. No completed output
was overwritten or deleted to obtain a passing result.

The affected headless regression selection ran with the repository-local Gradle
user home and the command below (PowerShell continuation shown):

```powershell
.\gradlew.bat --no-daemon --max-workers=2 --console=plain -Pheadless --continue `
  :engine-core:test :app-gui:test :app-uci:test `
  :app:test --tests '*BrnPair2*' --tests '*CheckpointStoreTest' `
  --tests '*GenerationRestartTest' --tests '*ApplicationVersionTest' `
  integrationTest --tests 'com.ohinteractive.seedv6.gui.*' `
  --tests 'com.ohinteractive.seedv6.uci.*' --tests '*ProductionSearchTest'
```

Result: **exit 1, 7m28s; 434 cases: 388 passed, 18 inherited failures, 28 skips**.
Core (61), GUI module (22) and UCI module (13) tests all pass. The selected legacy
tests have 9 passes and the 9 known GenerationRestart failures. Root integration
has 283 passes, the 9 known GUI failures and 28 existing headless skips. Every
failure's class, test name and message exactly matches `wu7-evidence.json`; no
test was suppressed or assertion changed. The previously recorded
`TablebasePresentationTest.engineCardLabelsOutcomeWithoutEvaluationUnitsOrASearchedPv`
failure was outside this selection and remains open. All 19 inherited migration
failures therefore retain their acceptance gates. This is not a passing full-suite
claim; broad unrelated training/research campaigns, optional Syzygy builds,
native Mac and interactive/client acceptance were not rerun.

Full command logs, selection failures, candidate receipts, clean-clone identities,
preservation hashes and regression results remain local under
`build/migration/portable-build/`; final candidates remain in their documented
output directories and the disposable clone. The earlier deep-clone failure is
retained in `clean-checkout.json` and `clean-git-bash.log`; passing clean evidence
is in `clean-short-checkout.json` and `clean-short-*.log`. These raw outputs stay
ignored and are not available in a fresh clone. No generated artifacts, private
settings, JDK installation, experimental checkpoint or training data is committed.
Before/after checks preserve global Gradle property bytes, process/user/system
Java/PATH settings, both JDK release files and the historical model. VERSION_STATE
remains byte-identical (SHA-256
`92a26d4e187edc6c47ed88100d355f77b4597d98524d21d005a3c164986b4bca`).
No root journal existed and none was created. The task's attributable Git commit
contains these records, the single approved payload/manifest and portable build
tooling/tests; it is not final owner acceptance or publication.
After validation, the two disposable clones' Git metadata is preserved under
`.git/portable-build-validation/`; their source/output directories remain intact.
This removes the unsupported nested layout without discarding validation evidence
or changing its active operation. Clean status above describes the verified clones
before this reversible metadata relocation.

**Version-finalization blocker:** local `begin` created token
`ebe0487f7c214b398fc2625a8ac053fa`. The requested same-token `finish --decision
no_bump` rejected nested validation repositories before any version write. Even
after their metadata relocation and a successful read-only full-tree scan,
`reconcile_pending` rejects the retained `integrity_failure` before retrying capture.
Status reports phase `active`, mutation `not_attempted`; no successful finish or
terminal warning is claimed. The documented `reconcile-legacy` route accepts a
different historical post-write failure, so it was not misapplied. Operation,
reservation, baseline and failure evidence remain intact. This is a blocking
limitation for later version-finalizer operations and work requiring finalized
version provenance; it does not change the independently verified version bytes
or invalidate the completed packaging tests. The build/asset commit proceeds
without a version increment, release or finalized-version claim. A separately
authorized supported reconciliation is required before another finalizer begin;
no helper, shared registry or reservation was edited to bypass the failure.

Native macOS discovery/packaging is prepared and portable fixtures pass, but no
native Mac execution is claimed. Mac arm64 builds, GUI walkthroughs and real-client
acceptance remain separate gates, as do the inherited migration failures below.
No WU8 cutover, official release, model publication/push or version bump occurs.

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
