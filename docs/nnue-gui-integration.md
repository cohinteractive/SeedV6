# Checkpoint-64 E008 in Play

Current consolidation (23 October 2026): the normal competitive baseline is now
Construction V1 + Conventional NNUE CP64; see the
[construction qualification record](research/search/P3_CONSTRUCTION_2026-10-20.md#construction-v1--cp64-consolidation-23-october-2026).
The E008-specific configuration, launchers and historical results below are
preserved. Earlier ordinary-default/GUI-worker-limit statements describe the
pre-consolidation state. Native Mac evidence below does not qualify this new build.


23 October 2026 (Auckland). The Play workspace exposes
**E008 + Conventional NNUE (Checkpoint 64)** in Human vs Engine and independently
for White and Black in Engine vs Engine. Existing choices and the default
Policy 4 / Pair-2 generation 32 remain unchanged. The selection is persisted
through the existing Play preferences. Training and persisted learning arenas
are outside this integration; their evaluator schema is unchanged.

## Shared implementation and resources

`engine-core` owns `search.driver.ConventionalNnueEngine`. Both the dedicated
`NnueCandidateUciMain` and GUI `PlayEvaluator` load this immutable binding. It
constructs the existing `SearchEvaluation.conventional` and the exact recursive
E008 adapter: QXRAY, QUIET LMR, WINDOW LMP, EFFORT and completed TT hints. No
Construction V1 mechanisms, aspiration, futility, null move or extensions are
added. The inference implementation is unchanged. Each lifecycle service owns
fresh search, TT and accumulator state; only immutable evaluator definitions can
be shared. No GUI production dependency on `app-uci` was introduced.

The tracked input is `build-assets/nnue-candidate/conventional-cp64.cnnue`.
`engine-core:processResources` now embeds the sole runtime copy at
`models/conventional-cp64.cnnue` in `engine-core.jar`, so direct GUI source runs,
the ordinary `:app:installDist` and `:app-gui:packageWindows` / `packageMac`
all receive it transitively. No working-directory lookup, model library,
training store, NAS or manual network transfer is involved. Existing GUI
launchers resolve `jdk.incubator.vector`; native GUI images bundle the module.
Unsupported vector geometry or an unresolved vector module uses the existing
exact scalar inference. Standard source builds use Java 21.

The loader requires exactly 3,940,952 bytes and full-file SHA-256
`8431626c2fca81e0a1e9ccd5e45ddd8829e6e3282c756c21fad75b06d075bb2b`, then verifies
S6CNU002 and eight material heads. Network provenance and source attribution
remain in [the candidate guide](nnue-botli-candidate.md#network-provenance-and-notices)
and the tracked manifest. Invalid resources fail the selected engine load;
the GUI reports the error, retains the previous binding, and leaves the game
paused. It never starts another evaluator under the NNUE label.

Play uses 64 MiB requested hash and **one search worker** for this choice,
regardless of the global Threads setting. Other engines retain that setting.
Selector/thread tooltips and live status explain the effective limit. Depth,
time limits and the GUI's existing optional opening repertoire remain in use;
book decisions are marked BOOK. Equivalence checks disable books and compare
actual searched results. NNUE uses the same no-tablebase service as the dedicated
candidate. Search cancellation, new-game invalidation, side ownership, engine
replacement and window shutdown use the existing lifecycle service.

## Targeted validation

Windows 11 AMD64 / Java 21, sequentially through the owner compute controller:

- Four existing `NnueCandidateTest` tests passed after extraction: 259 independent
  reference positions across all eight heads, actual E008 settings, five depth-5
  incremental/full-rebuild comparisons, strict network rejection.
- Seven GUI tests passed: `ConventionalGameplayTest` (2),
  `ConventionalDesktopTest` (1), `ConstructionGameplayTest` (2), and
  `ProductionBaselineDesktopTest` (2). No skips. They cover existing defaults,
  saved and independent choices, actual search fields, one-worker enforcement,
  board preservation, legal moves, four depth-5 full-rebuild comparisons,
  timed search, stop, reset, replacement and terminated search owners.
- The real Swing frame selected NNUE, played an engine move and a human reply,
  started another game, switched to Construction V1, used NNUE independently
  for Black, and closed during a fresh long search. Screenshots were inspected.
- `:app:installDist` and `:app-uci:installNnueCandidateDist` passed. A relocated
  GUI classpath deliberately excluded `app.jar` and `app-uci.jar`; the tracked
  `ConventionalGuiProbe` ran from an unrelated directory with no training store.
  Four roots (start, castling, en-passant, promotion), depth 5, Hash64, books off,
  produced identical score, move, node count and PV with vector-256-long and
  module-absent scalar-long. Those results also exactly matched both current UCI
  and the preserved qualified build-65 UCI distribution. This is runtime parity,
  not a comparison of identity strings or a new strength qualification.
- Isolated GUI distribution copies with missing, truncated, corrupt and
  incompatible resources reported understandable failures, preserved the prior
  binding, and successfully switched to and searched with Policy 4.
- Normal `:app-gui:packageWindows` built and verified the native GUI image and
  independently extracted ZIP. Its bundled Java and application jars loaded the
  network and matched the portable start-position depth-5 result from an
  unrelated working directory. No system Java or UCI jar was needed at runtime.
- The updated offline UCI verifier passed after the resource move, including
  its scalar/vector checks, lifecycle, resource rejection and offline client API.

Local evidence is under `build/nnue-gui/`: `compile-02.log`, `test-04.log`,
`distribution-01/result.json`, `package-01.log`, `native-runtime-03.log`,
`desktop-05.log`, and `uci-regression-01/result.json`. The distribution verifier script is retained
there as local reproducibility evidence. Swing PNGs are under
`app-gui/build/nnue-gui/`. Failed initial harness/build attempts are preserved:
a removed Gradle block initially left a closing brace; a probe used a nonexistent
FEN method; the oracle initially received a BOOK result; and native probe command
quoting / an incorrect expected literal were corrected before passing reruns.
These were not accepted as validation. Bulk outputs remain ignored.

Useful targeted rerun (set the installed Java 21 `JAVA_HOME`; on Windows use
`gradlew.bat`, and the local compute controller where applicable):

```text
./gradlew --no-daemon --max-workers=1 :app-gui:test --tests '*ConventionalGameplayTest' --tests '*ConventionalDesktopTest' --tests '*ConstructionGameplayTest' --tests '*ProductionBaselineDesktopTest' :app-uci:test --tests '*NnueCandidateTest' -PconventionalVectorTests
./gradlew :app:installDist
./gradlew :app-gui:packageWindows
```

On macOS use the existing `:app-gui:packageMac` route. Native macOS execution and
Apple Silicon vector geometry/performance were not tested. No full test suite,
training, tournament, live bot activation, push or deployment was performed.
Timed searches can legitimately complete different depths across hosts; the
parity assertions here use fixed depth, one worker and identical settings.

## Change inventory

- `engine-core/build.gradle`, new `ConventionalNnueEngine.java`: shared asset,
  strict loader, qualified factory and identity.
- `app-uci/build.gradle`, `NnueCandidateUciMain.java`: delegate to shared core.
- GUI `PlayEvaluator.java`, `PlayParticipants.java`, `PlayEnginePanel.java`,
  `EngineSearchAdapter.java`, `EngineCard.java`, `ChessFrame.java`: choice,
  bindings, one-worker lifecycle, display and explanatory text.
- `app-gui/build.gradle`, new `ConventionalGameplayTest.java`,
  `ConventionalDesktopTest.java`, `ConventionalGuiProbe.java`: targeted checks
  and optional vector test flag.
- `tools/verify_nnue_candidate.py`, `tools/package_nnue_candidate.py`: follow
  the shared resource owner; the offline verifier still accepts historical
  UCI-owned assets and rejects duplicate embedded networks.
- This document, the candidate guide and `VERSION_STATE.txt`: integration
  evidence, current resource location and the application build increment
  included with the implementation commit (65 to 66).

Human actions required after this prompt: None. Native Mac verification remains
an unperformed platform check; this task does not deploy or activate anything.
