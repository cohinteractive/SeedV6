# macOS packaging compatibility recovery

19 October 2026 (Pacific/Auckland). Focused packaging repair on
`fix/macos-python39-packaging`, based directly on
`6b8d27566d80dd6c864cb531ea3530f70bf8a076` (Policy 4 production promotion).
The initial working tree was clean. Local `main`, `origin/main` and a live
`git ls-remote --heads origin` all identified that baseline; the remote is
`git@github.com:cohinteractive/SeedV6.git`. No remote write was performed.

## Recovery and scope

Both historical leads were available as ordinary Git objects; the HEAD reflog
also retains the local Python compatibility commit before the reset to the shared
baseline. No object salvage or destructive Git operation was needed.

- `18ddd6ece499ab4304a65aeed774992b60196bac` tracked the approved model/manifest,
  added installed-JDK discovery and optional overrides, disabled automatic JDK
  downloads, and added packaging safety/configuration tests and documentation.
  It is an ancestor of the current baseline. Its model assets, Gradle packaging
  convention, Python packaging/JDK scripts and tests are already present unchanged.
  No assets or JDK configuration needed restoration.
- `ab0a426efa9eb758a34d0dc2c62bca993d5f2442` added two Python compatibility fixes,
  regression tests and historical Mac validation documentation. It branched from
  `18ddd6e` and is not in the current main lineage. Only its exact changes to
  `tools/native_packaging.py` and `tools/test_native_packaging.py` were applied;
  the resulting two files match that commit byte-for-byte. Its old acceptance
  records were inspected, not substituted for current validation.

Changed files:

| File | Purpose |
| --- | --- |
| `tools/native_packaging.py` | Stream SHA-256 in bounded 1 MiB reads; write the Mac UCI launcher as exact UTF-8/LF bytes, retaining executable permissions. |
| `tools/test_native_packaging.py` | Restore empty/known/binary digest, boundary/multiple/short/bounded read, missing-file and launcher byte/mode regressions. |
| `README.md` | Correct the supported packaging Python minimum to 3.9. |
| `docs/unified-architecture/WU7-PACKAGING.md` | Document Python 3.9 support and link current native evidence. |
| This report | Preserve recovery, validation, limitations and integration instructions in Git. |

No production Java, Search algorithm/default, evaluator, model, research evidence,
Gradle convention or finalizer change is included. Contract P3-R007 and Frontier
P3-F005 are unchanged. Policy 4 remains the accepted production champion.

## Root cause and environment

Gradle invokes `python3 -B tools/native_packaging.py` through
`gradle/native-packaging.gradle`. The `--info` reproduction confirmed that command
without a Python override. `/usr/bin/python3` selects Xcode Python **3.9.6**, whose
`sys.executable` is `/Applications/Xcode.app/Contents/Developer/usr/bin/python3`.
`hashlib` is the expected Xcode standard-library module under
`Python3.framework/Versions/3.9/lib/python3.9/hashlib.py`; it has no `file_digest`.
This is the newer API's Python 3.11 requirement, not module shadowing or a JDK fault.
The second recovered fix avoids Python 3.10's `Path.write_text(newline=...)` API.

The existing `-PpackagingPython=<executable>` override can select a newer Python,
but is unnecessary with the portable source fix. No interpreter installation,
dependency or global environment change was made.

Host: macOS **26.4**, build **25E246**, arm64. Gradle, compiler and packaging used
Eclipse Temurin **21.0.12.1+1**, home
`/Users/charlesclark/Library/Java/JavaVirtualMachines/jdk-21.0.12.1+1/Contents/Home`.
Native discovery selected that JDK without overrides. The bundled runtimes report
21.0.12.1; GUI launcher, both Java executables and both JVM libraries are arm64.

## Validation and retained evidence

Local raw evidence is under
`build/packaging/macos-recovery-20261019-9blecf4c/` (ignored, unavailable in a fresh
clone). Baseline failures are retained alongside successful results.

| Check | Outcome |
| --- | --- |
| Unmodified `./gradlew packageMac` | Reproduced failure at `:prepareMacPackaging`: `module 'hashlib' has no attribute 'file_digest'`. Detailed invocation retained in `baseline-packageMac.log`. |
| Unmodified packaging/JDK tests | 21 tests, 5 errors caused by `file_digest`; `baseline-tests.log`. |
| `python3 -B -m unittest discover -s tools -p 'test_*packaging*.py' -v` | All 23 pass under actual Python 3.9.6; `python39-tests.log`. |
| Independent SHA-256 | Six cases (0, 3, 1 MiB minus/at/plus one, and 3 MiB + 123 bytes) match `/usr/bin/shasum -a 256`; pinned model also matches. `independent-sha256.json`. |
| Python compatibility | Six packaging/discovery/verification/test scripts parse under Python 3.9. A newer Python runtime was not tested in this recovery. |
| Corrected `./gradlew packageMac` | **BUILD SUCCESSFUL in 29s**; both images/ZIPs `VERIFIED_LOCAL_CANDIDATE`; `packageMac.log`. |
| Built-in native verification | GUI headless resources/JNI and UCI lifecycle checks pass for images and independently extracted ZIPs; identical GUI/UCI core/model identity. |
| Outside-checkout verification | `tools/verify-native-candidate.py` passes for copied images and extracted ZIPs of both applications, with paths containing spaces, unrelated cwd, empty PATH and no JAVA_HOME/JDK_HOME. |
| Explicit production smoke | Pinned Pair-2 hash, `hints=completed_same_key`, `history=invocation`, `learning=all_searched`, default worker 1 / TT 64 MiB, QXRAY/quiet LMR/WINDOW and futility off; `uciok`, `readyok`, depth-3 `bestmove c2c4`, exit 0, empty stderr. `explicit-policy4-smoke.json`. |

Each of the four standard UCI verification executions (image, extracted ZIP,
outside copied image, outside extracted ZIP) completed 63 short searches at
workers 1/4/16, plus stop/quit/EOF/process-termination and invalid-model checks.
These are bounded packaging checks, not strength/tournament research.

Outside verification evidence is retained at
`/private/tmp/SeedV6 Mac recovery syhv76o6/{gui,uci}/verification.json`.
`relocation-gui.log`, `relocation-uci.log`, `outside-path.txt` and
`candidate-summary.json` in the local evidence directory identify those results.

Candidate ID: `candidate-20261018T214223-b09d178d` (UTC timestamp).
GUI: `dist/mac/candidates/ID/SeedV6-GUI.app`; UCI:
`releases/candidates/macos-arm64/ID/SeedV6-UCI`. Each parent contains its ZIP,
`candidate.json` and an image `build-manifest.json` with full inventories.

| Artifact | SHA-256 |
| --- | --- |
| Shared core JAR | `1818c820b62bb67441c99d9d8f05426187d70c7b43d0c1ee3228d7a8b354e385` |
| Pinned generation-32 Pair-2 model | `5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7` |
| GUI ZIP | `f125058d7b75c20b4965d87f97d37381560dffb2b99b61b5345c3d1b10e2948f` |
| UCI ZIP | `ae65a1a15675d96a951770696e9a41c1574ec486927f40674a1db1df026e3446` |

Candidates truthfully record baseline `6b8d2756` plus the uncommitted packaging
repair and exact source hashes at build time. They are validation candidates,
not clean-commit release builds. The inherited manifest's `productionSearch`
annotation lists cumulative Phase 2 components; it is not an exhaustive Policy 4
description. Current Policy 4 is evidenced by unchanged promoted source and the
explicit live UCI identity above, not inferred from that legacy annotation.

## Version and acceptance boundaries

VERSION_STATE.txt remains byte-identical: **0.0.0 build 35**. The initial required
finalizer begin failed with `FileNotFoundError` for
`C:/projects/codebase-entropy-control/scripts/codex-versioning/finalizer.py`, before
returning a token. The owner then explicitly instructed: "Ignore the finalizer
entirely, do not use it. Continue". It was not invoked again, investigated,
modified or repaired. No successful finalization or version bump is claimed.

Local packaging repair and native automated validation are complete. Full native
GUI Play/Training/Arena walkthroughs, real BotLi/Arena client integration, current
Windows-native execution and same-revision Windows/Mac comparison remain pending.
No GUI windows, tournament research, signing/notarization, external release or
deployment was performed. The compiler's incubating-vector warning is non-blocking.
Existing migration acceptance gaps are not closed by this repair.

## Local commit and safe integration

The focused commit containing this report is directly atop `6b8d2756`; identify it
with `git log -1 fix/macos-python39-packaging`. Local `main` remains at the shared
baseline. The feature branch has no remote upstream. Remote publication and
Windows synchronization are pending, independently of local commit completion.

After explicit owner authorization, with clean working trees and if shared main
still equals the verified baseline, ordinary fast-forward integration on the Mac is:

```sh
git fetch origin
git merge-base --is-ancestor origin/main fix/macos-python39-packaging
git switch main
git merge --ff-only origin/main
git merge --ff-only fix/macos-python39-packaging
git push origin main
```

Stop if the ancestry check fails. If main has advanced and diverged, merge the
updated `origin/main` into the feature branch in a new commit, review any conflicts
and rerun relevant packaging validation, then fast-forward local main and use a
normal push. Preserve both histories; no rebase, reset, amendment or force-push is
needed. If the remote advances again, a normal push safely rejects the race.

After the authorized remote integration, on Windows with clean local main:

```sh
git fetch origin
git switch main
git merge --ff-only origin/main
```

If Windows has additional unpublished commits or unrelated modifications, preserve
them and review the divergence before integration; do not reset or overwrite them.
The Mac fix must land in shared main, rather than remain a machine-local patch.

Blocking human actions for local repair: **None**. Owner authorization is required
before remote publication; Windows synchronization follows that integration.
Deferred human acceptance: native GUI and real client checks against the exact
candidate artifacts. None of these is claimed completed by a local commit.
