# Reusable Windows GUI/UCI outputs and bounded candidate cleanup

4 November 2026. Ordinary build infrastructure maintenance, authorized by the
owner. Starting tree: clean `main`, `76d5015d348880cdfc2fdb222811239fecc83be0`,
version 0.0.0/build91. Repository masters remain P4-R016/P4-F032. No Search,
research protocol, production configuration, external installation or release
change is part of this work.

## Original behavior and dependency inspection

`gradle/native-packaging.gradle` defaulted `candidateId` to `auto`.
`tools/native_packaging.py` consequently generated a timestamp/UUID on each
normal build, putting GUI images under `dist/windows/candidates/ID` and UCI
images under `releases/candidates/windows-x64/ID`. Each contained one runnable
image, its portable ZIP and identity receipt. These were separate GUI/UCI
distributions, not two copies of the same application. Both shared core/model
identity. Successful staging retained another extracted image, input JARs/model,
jpackage temporary files and probes. There was no success/failure cleanup.

The Java-only `:app-uci:installDist` already uses a stable Gradle install directory.
`tools/package_nnue_candidate.py` is a deliberately invoked, clean-source freeze
of the dedicated E008 distribution into `releases/candidates/portable-java21`;
it is not an automatic side effect of native or ordinary Java builds and is
unchanged. Explicit legacy GUI snapshot entrypoints remain available; they are
not the normal unified tasks. No unrelated cache or historical checkout was cleaned.

Inspection covered the masters, research STATE/PRODUCTION/protocol/manifests,
tracked and ignored local launcher/verification scripts, packaging tasks, current
repository configuration, process command lines and local desktop/start-menu
SeedV6 shortcuts. No scoped historical distribution was running. BotLi's recorded
external consumer is on macOS and cannot be inspected from this Windows host;
its configuration/installation was not changed or claimed unused. Build77 and
latest build91 outputs are protected against unresolved owner shortcut/external use.

Build labels alone were not used as identities. In particular, the native
preversion build84 production package differs from the actual frozen HL-G1
research champion. Frozen79/80, H1/81, H2/82, TTF/83, champion84, correction86-88,
tactical89 and prototype runtimes live at their recorded research `raw/` paths,
outside cleanup scope. Their original locations and manifests remain intact.

## Cleanup record

The [per-directory inventory](windows-build-cleanup-2026-11-04.json) is the
authoritative record of exact names, approximate byte sizes, A/B/C/D classifications,
decisions, research/local references, artifact identities and deletion outcomes.
The initial three trees contained 179 directories, 31,039,258,995 bytes
(28.908 GiB). The cleanup deleted all 149 dry-run selections totaling
24,856,644,843 bytes (23.150 GiB): 62 obsolete ordinary distributions and 87
successful staging trees, with no deletion errors. Thirty directories remain.
Sizes are sums of file lengths, not an exact filesystem allocation/free-space
measurement. The inventory records each deletion and retained identity check.

| Scope | Deleted directories | File bytes removed | Retained directories |
| --- | ---: | ---: | ---: |
| `dist/windows/candidates` | 86 | 20,716,715,418 | 18 |
| `releases/candidates/windows-x64` | 63 | 4,139,929,425 | 11 |
| `releases/candidates/portable-java21` | 0 | 0 | 1 |

Required retained packages include exact build90 GUI/UCI production artifacts,
build91 current identity artifacts, HL-G1 build85 production and its recorded
preversion comparison, SR034's build74 native baseline, build68/69 historical
comparison inputs, and BRN waves5-9 native research dependencies. Full images,
bundled runtimes/models, ZIPs and identity evidence were preserved together.
The inventory names the particular scripts/records and precise reasons for each.

Eleven ambiguous directories are deliberately preserved: GUI/UCI adaptive16 build70,
GUI BRN wave4 and wave9-build60 artifacts, GUI/UCI build77, the portable E008 build65
delivery, and four failed/interrupted staging trees (GUI formulation-precommit-v1,
GUI tc-p1-build90-v1, and UCI candidate attempts 20261023T194312/20261023T195311).
Their deletion is not established safe by this maintenance task. They are not
automatically swept by future builds. Retaining these is not a blocker to normal
builds or to the independently safe cleanup.

Before deletion, candidate image/archive/receipt identities and staging extracted
images/input JARs/model copies were checked. Tracked files, links/junctions,
unexpected contents, changed inventories and active/locked paths are rejected.
Deletion is limited to each explicitly inventoried, resolved direct child of the
three authorized roots. Raw research evidence, datasets, training checkpoints,
external JDKs/models, frozen manifests and Git history were excluded.

## Normal and frozen builds

```powershell
.\gradlew.bat :app-gui:packageWindows
.\gradlew.bat :app-uci:packageWindows
.\gradlew.bat packageWindows
# Deliberate independent freeze; refuses an occupied identity:
.\gradlew.bat packageWindows -PcandidateId=my-comparison-v1
```

Canonical Windows executables:

- `dist/windows/current/SeedV6-GUI/SeedV6-GUI.exe`
- `releases/current/windows-x64/SeedV6-UCI/SeedV6-UCI.exe`

Each parent contains its one complete image, portable ZIP and existing
`candidate.json` receipt. Normal builds create no historical release copy.
Explicit candidates retain the old `candidates/ID` paths and never overwrite them.
The shared convention also uses stable current paths on macOS; native macOS
execution was unavailable here and is not claimed verified.

A Gradle build-service OS lock protects shared preflight context and compilation
through build completion. Separate per-output OS locks protect Python staging and
publication; GUI/UCI outputs are independent. Verified trees are renamed as units.
The previous successful tree is retired to a fixed backup, checked for racing
consumers, and restored if publication fails. Current receipts and image/archive
contents must verify before replacement. Running/locked Windows outputs fail with
an actionable close-and-retry message. No consumer sees a partly assembled image;
Windows directory replacement has a short absent-path interval.

Successful publication cleans its backup and staging, including jpackage's
read-only generated files. Failure retains a small fixed diagnostic JSON and
removes idle staging. A subsequent build recovers an interrupted backup and only
reclaims staging with its matching ownership marker after OS-lock, process and
open-file checks. In-use or unowned trees are preserved. Explicit
`-PkeepPackagingStaging=true` retains diagnostic staging and requires inspection
and moving/removing it before reuse. Incremental compilation caches are untouched.

The generated production-profile label now comes from the already verified runtime
identity instead of the stale hard-coded `HL_G1` label. This corrects new packaging
metadata to `TC_P1`; historical manifests and Search selection remain unchanged.

## Executed validation

Local logs are under ignored `build/packaging/cleanup-20261104/`; no research
campaigns, tournaments, training or expensive Search benchmarks ran.

- 37 Python packaging tests passed, including real Windows locked-file behavior,
  a competing Python process, replacement rollback, interrupted-publication
  recovery, modified-output refusal, diagnostic retention, owned/unowned staging,
  read-only cleanup, frozen-output refusal, archive/integrity checks and existing
  UCI/model/module boundary checks.
- 10 JDK-selection tests passed. Two relevant existing Gradle architecture fixtures
  passed: graph/configuration-cache and portable/cache-tracked packaging selection.
  Native builds also passed the repository project-boundary guard.
- Independent normal GUI and UCI builds passed on Windows x64 using the discovered
  Temurin 21.0.12.1 packaging JDK and established Java21 compiler. Required images,
  JARs, runtime, legal files, models and both UCI launchers were verified by existing
  checks, including independently extracted ZIPs with empty PATH/unrelated cwd.
- Actual canonical GUI opened to the prepared Play screen, remained responsive
  during a rejected rebuild, and closed normally. No game or training was started.
  Canonical native UCI returned `uciok`/`readyok` and quit with exit0 on empty PATH.
  Existing packaging exercises bounded UCI protocol/search/lifecycle and NNUE checks.
- A real rebuild with the GUI running failed with the expected close-processes
  message. GUI file hashes/modes remained unchanged across that rejection and the
  independent UCI build. A missing-model preflight failed without replacing UCI.
  An explicit build against existing `formulation-build91-v1` rejected the occupied
  frozen path. A second Gradle process using a different project cache was rejected
  by the native build lock before packaging/compilation.
- Repeated root `packageWindows` passed: both stable paths, common core/model/source
  identity, no additional historical candidate directory, and no current staging
  or backup left behind. Both old-success replacement paths were exercised.
- 22 original frozen runtime/prototype manifests covering 652 file entries were
  checked against their recorded hashes before and after cleanup. All 30 retained
  scoped directories have identical before/after file hashes, sizes and modes.
  Build90 ZIP/core identities
  and all historical evidence retain their original meanings.
- Comparing the newly packaged engine-core with frozen committed build91 found
  no added/removed ZIP entries and no changed classes/models/Search resources.
  Only generated `application-version.properties` differs for the working source
  identity. Version 0.0.0/build91 itself is unchanged.

During implementation, the first replacement tests exposed Windows parent-directory
rename restrictions while child handles were open; the second check after retiring
the directory addresses that race. The first actual native GUI attempt published a
verified image but failed staging cleanup on jpackage's read-only launcher. Cleanup
was corrected and the next successful build recovered/removed that staging. A
missing-model test initially had an incorrectly quoted PowerShell property and
failed task selection; the correctly quoted rerun reached the intended preflight
failure. These attempts remain in the local logs, not counted as passes.

Limits: native macOS, external BotLi, broad interactive gameplay/training and
power-loss durability were not tested. Interruption recovery was exercised with
controlled filesystem states, not by killing the host during directory renames.
The full Java/JUnit suite was not rerun for these packaging-only changes; no Java
source changed, and native runtime checks plus the class/model byte comparison
provide the proportionate application regression evidence.
File/process checks and OS locks are best-effort against noncooperating external
actors; unknown modifications fail closed. No acceptance, deployment or release
claim follows from these maintenance checks.

## Provenance and excluded follow-up

Changes are confined to the existing packaging implementation/convention, packaging
tests, generated-output ignore rule, current build instructions and this cleanup
record. The starting tracked tree was clean. At the initial maintenance handback,
no commit, push, release, deployment, BotLi installation replacement or production
configuration change had been performed.
No build increment is warranted for this non-runtime tooling/documentation work;
all original version-file bytes are preserved. Root `CODEXLOG_CURRENT.md` was
absent and was not created. No finalizer or behavioral-governance maintenance ran.

Modified tracked files: `.gitignore`, `gradle/native-packaging.gradle`,
`tools/native_packaging.py`, `tools/test_native_packaging.py`, `README.md`,
`docs/construction-v1-gameplay.md`, `docs/nnue-botli-candidate.md`, and
`docs/unified-architecture/WU7-PACKAGING.md`. The two new reviewable records are
this report and `windows-build-cleanup-2026-11-04.json`. All ten files were left
uncommitted at the initial handback; no unrelated tracked changes were inherited
or absorbed.

The 11 ambiguous directories, protected complete distributions, legacy snapshot
entrypoints, macOS candidates, general build caches and all research/model evidence
are intentionally outside further deletion. Future disposal of ambiguous artifacts
requires dependency/evidence disposition, not an automatic cleanup rule. No such
action is required to use the completed ordinary build workflow.

## Owner acceptance and Git closeout - 4 November 2026

The owner explicitly accepted the maintenance outcome and authorized one focused
Git closeout commit. Closeout found exactly the ten reported files, with all ten
matching the source hashes recorded by both final native builds. The saved logs
confirm 37 packaging tests, 10 JDK tests, two relevant Gradle architecture tests
and the final combined Windows build passed. No implementation or test changes
were needed. Closeout added this acceptance note and normalized the cleanup
inventory's CRLF line endings to LF after staged whitespace validation flagged
them; its JSON values and all recorded hashes are unchanged. The focused commit
contains the accepted implementation, tests, build instructions and maintenance
records.

Build91 and all version-file bytes remain unchanged. Builds and runtime tests
were not repeated for this Git-only closeout. No additional cleanup, research
storage/checkpoint work, changes under `docs/research`, Contract/Frontier edits,
push, release, deployment or external executable replacement were performed.

Human actions required after this prompt: None.
