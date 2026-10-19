# construction-v1 gameplay activation

Owner-authorized experimental gameplay, 20 October 2026. This does not promote
the candidate, accept Workstream 3, change Phase 3 canon or launch more research.
Production Policy 4 remains available and remains the ordinary UCI default.

The gameplay binding uses the existing flattened PVS composition: all 18 controls,
flags **262143**, futility **100 × depth**, reverse futility **200 × depth**, shared
extension cap **1**, aspiration **OFF**, requested Hash **64 MiB**, and **1–16**
workers. It loads only BRE-Pair 2 generation 32, checkpoint
`g000032-s001753088-5837c7f7192aa3cf9a265388f93649327455e804541398ae3ef6b69e09014304`,
lineage `235d0688-e060-44e3-8f1e-119c39bc8017`, mapping
`BRN_PAIR2_MATERIAL_RESIDUAL_QUARTER_V1`, SHA-256
`5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7`.
The tracked payload is embedded in `engine-core.jar` and verified before use.
No checkpoint library, research workspace, local experimental output or model
download is needed. Missing/corrupt/different model bytes fail explicitly.

The frozen [construction handback](research/search/P3_CONSTRUCTION_2026-10-20.md)
and protocol retain their historical WIDEN16, core commit `2224a8b` and build 39
identities. This gameplay activation deliberately defaults aspiration to OFF at
the owner's request; it does not rewrite those experiments.

## macOS / BotLi

From the retrieved repository, with an installed Java 21 JDK:

```sh
git pull --ff-only
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
./gradlew --gradle-user-home build/migration/gradle-user-home --no-daemon --max-workers=1 :app-uci:installConstructionDist :app-uci:constructionDistTar
./app-uci/build/install/seedv6-construction-v1/bin/seedv6-construction-v1
```

The final command is the executable for BotLi. Point its engine executable at
the **absolute path** to `app-uci/build/install/seedv6-construction-v1/bin/seedv6-construction-v1`.
If the existing configuration separates directory and executable name, set the
directory to that absolute `bin` directory and the name to `seedv6-construction-v1`.
Use the existing UCI protocol setting. No research options or command arguments
are necessary; ordinary `Threads` and `Hash` options remain available. Preserve
credentials, account settings and the rest of the existing workflow. Ensure the
BotLi process inherits Java 21 through `JAVA_HOME` (or has Java 21 on `PATH`).

Keep the complete installed directory, including `lib/`, together if relocating.
The distribution is Java-only, with generated POSIX and Windows launchers; it
does not bundle a JRE. The TAR in `app-uci/build/distributions/` preserves executable
permissions. `constructionDistZip` is also available. Standard `seedv6-uci` and
native production-UCI packaging still launch Policy 4.

For a local handshake, enter `uci`, `isready`, `position startpos`, `go depth 4`,
wait for `bestmove`, then `quit`. Expect:

```text
id name SeedV6 construction-v1 experimental ...
info string policy=construction-v1 experimental=true flags=262143 configuration=flat-pvs-v1[...]...;aspiration=OFF;...
uciok
readyok
info depth ...
bestmove ...
```

The full identity includes margins, extension cap, actual workers/Hash and model
SHA-256, and is re-emitted after resource changes. The standard `UseBook`/`OwnBook`
aliases default to false; when enabled, repertoire hits are explicitly labelled
and misses use the selected Search. Leave them false when checking Search activity.
`--aspiration WIDEN16` is an optional explicit launcher variant; OFF is the default
for both GUI paths. `--model <file>` accepts only byte-identical pinned input.
Research JVM properties are rejected by this dedicated launcher.

Optional bounded local process verification (Python 3, standard library only):

```sh
python3 -B tools/test_construction_uci.py --install app-uci/build/install/seedv6-construction-v1 --out build/construction-uci-check-1
```

Use a fresh output directory on retries. This runs UCI requests, not games or a
Lichess connection. Native macOS and the owner's BotLi process must be checked on
the Mac; Windows validation cannot establish them.

## Windows GUI and existing harness

Build the normal native GUI using its existing tooling (Java 21.0.6+ packaging
JDK and Python 3.9+, discovered as described in the [packaging guide](unified-architecture/WU7-PACKAGING.md)):

```powershell
.\gradlew.bat :app-gui:packageWindows
```

Run `SeedV6-GUI.exe` inside the newly printed candidate directory under
`dist/windows/candidates/`. It includes its Java runtime. For an immediate source
build/run using the installed Java 21 toolchain:

```powershell
.\gradlew.bat --gradle-user-home build/migration/gradle-user-home :app-gui:runGui
```

In **Play → Engine vs Engine**, select
**construction-v1 / Pair-2 g32 (experimental)** in either side's existing
**Engine / evaluator** selector. Select **Policy 4 / Pair-2 g32** on the other
side for the fixed-model production comparison, or retain any supported HCE/network
choice. Choose the existing limits/Threads and click **Start Game**. Swap Sides
moves the complete binding. In **Human vs Engine**, select construction-v1 in the
existing evaluator selector and click **New Game**. No model-library setup is needed.

In **Arena → Match only → Setup**, use the same selections for participants A/B.
The existing **Create and run**, limits, worker settings, pause/resume and
colour reversal remain in use. Pinned profiles are fixed gameplay participants;
they cannot be used as trainable Learning campaign participants. The saved
campaign/endpoint retains the profile and generation-32 identity. Each participant
owns separate Search, TT and mutable worker state, even with identical model bytes.

Play's player labels and Engine footer identify the active policy; hover the player
identity/footer for the configuration and model checksum. Arena participant labels,
Live and History identify construction-v1 versus Policy 4; History includes the
model hash and resources. Check the **active** player labels after Start Game,
because changing a setup selector only changes the next game's draft. Failed
selection reports an error and pauses the game; it does not run Policy 4 under a
construction-v1 label. Worker changes preserve the current profile. GUI requested
Hash remains its existing 64 MiB; UCI Hash is adjustable from 1–1024 MiB.

## Implementation and validation boundary

`ConstructionSearch` moved unchanged in behavior from research ownership to
`engine-core` as the shared immutable composition seam. `PinnedSearch` binds the
verified payload and either that composition or `ProductionSearch`. The new UCI
entry point and GUI Play/Arena adapters use it; the historical research launcher
still supports its controls through the same composition. No GUI/UCI dependency
on research, Search algorithm change, production promotion or canon edit is involved.

Automated coverage checks actual instantiated Search objects (including all 18
flags and flattened frames), every supported worker count, canonical factory
equivalence, pinned bytes, unsupported combinations, budgets/reset/reuse/stop,
independent participants, Arena persistence/resume and native Windows Swing
selection/rendering. The packaged-process test uses an unrelated client working
directory and a relocated installation with spaces, checks UCI handshakes,
clocks, Hash/Threads, node limits, book options, stop/new-game/reuse, quit/EOF,
optional WIDEN16 and startup rejection. Only tiny bounded fixture games are used
for harness plumbing; no strength result or tournament is claimed.

Native Mac runtime, BotLi integration and live Lichess gameplay are owner checks.
Workstream 3 acceptance and the previously requested master-mirror reconciliation
remain separate gates for future research; they do not block this authorized
experimental gameplay activation.

Validated on Windows, 20 October 2026, for build 40: **214 Java tests passed**
(67 core, 15 UCI, 25 GUI, 1 pinned Arena, 25 construction/Policy 4 research
regressions and 81 distinct focused integration tests across the two selections).
Four older native-display integration checks were skipped by the headless
integration task; the new native `ConstructionDesktopTest` passed without a skip.
The 13 native-packaging and 10 JDK-selection Python checks passed. Native Windows
GUI packaging verified the image and extracted ZIP with the bundled runtime.
Experimental UCI process checks passed from the relocated distribution, including
book options, 1/4/16 workers and OFF/WIDEN16. ZIP/TAR closure and TAR executable
permissions passed; the POSIX launcher also completed handshake/search/quit under
Windows Git Bash. None of these results is a native Mac or BotLi launch claim.

Local ignored evidence is under `build/activation/` (commands/logs, suite snapshots,
UCI transcripts and archive checks) and `app-gui/build/activation-gui/` (native
Swing captures). Early validation setup failures are retained: the isolation init
script initially replaced Gradle's test classpath; a later guard test briefly ran
against the earlier GUI compilation; the first Git Bash probe supplied a Windows
backslash script path. Corrected initialization/recompilation/POSIX invocation
passed; no engine defect remained from these attempts. A fresh checkout does not
include these local outputs; the committed tests and commands reproduce the checks.
