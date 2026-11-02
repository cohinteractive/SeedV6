# HL-G1 experimental production integration - 2 November 2026

The owner separately authorizes HL-G1 across **W1-W16** and accepts unresolved W4
competitive risk. This bounded integration does not reopen accepted/closed HL-R001,
launch research, advance another champion, or authorize deployment. Governing
masters: **P4-R005/P4-F021**. Starting state was clean `main` at
`d4eb9d6f`, build84, P4-R004/P4-F020; root `CODEXLOG_CURRENT.md` was absent.

## Selection and preservation

Normal GUI Play, built-in Arena and standalone UCI select **HL-G1 - request-scoped
retained Search learning with bounded history-guided reductions**, flags**104071167**,
through shared `BaselineEngine`. The only Search-policy difference from prior
production flags3407871 is REQUEST_LEARNING + HISTORY_EFFORT. The qualified kernels,
gravity updates, reduction scale/cap, tactical exclusions, verification and
deeper-futility consistency are unchanged. No alternative Search implementation.

CP64/checkpoint64/S6CNU002/eight heads/BT4_Q_LOGIT400_V1, Hash64, ADAPTIVE16,
BOUNDARY2, worker default1, bounded Max, OWNER at W1 and ROOT_REUSE at W2-16 remain.
The CP64 file SHA-256 remains
`8431626c2fca81e0a1e9ccd5e45ddd8829e6e3282c756c21fad75b06d075bb2b`.
HCE, BREP2-32 and unrelated conventional payloads retain flags262143. Explicit
Policy4, E008, Construction and research selections bypass the production selector.
Research factories that previously borrowed the default now explicitly call
`legacyConfiguration`; their flags/aspiration and independent controls are preserved.

HL-G1 research identity remains implementation
`b8ea8b2856eed3f6c774860ca540375054e4de30`, **build84**, `guided`, flags104071167.
All33 files in `runtime-build84-v1-manifest.json` were present and hash-verified.
The new core JAR differs from that frozen core only in `BaselineEngine` classes
and generated version metadata. No historical manifest, result, protocol or frozen
BOUNDARY2 study was edited or reopened. P4-BASELINE-001 remains build79,
`753367614b8aa7e32dee337fd76b542737141ac1`, flags3407871. Corrected build80 remains
a separate technical control; rollback does not claim competitive equivalence to79.

## Rollback and precedence

Normal launches require no Search parameter. The shared JVM property
**`-Dseedv6.searchProfile=LEGACY`** selects previous flags3407871 with current
correctness/accounting fixes. `HL_G1` explicitly selects the default. Invalid
values fail. A binding snapshots its selection; worker/hash/clock reconstruction
does not silently switch policy. Explicit core API selection precedes the property;
explicit `ConstructionSearch` configurations are independent.

For a Java UCI distribution, set `SEEDV6_UCI_OPTS=-Dseedv6.searchProfile=LEGACY`
in the launch environment, then run `bin/seedv6-uci.bat` (or the Unix launcher).
For either native Windows application, a process-scoped `JAVA_TOOL_OPTIONS`
setting works with the unchanged launcher. For example, from a fresh PowerShell
terminal used only for this launch:

```powershell
$env:JAVA_TOOL_OPTIONS = '-Dseedv6.searchProfile=LEGACY'
& '<candidate>/SeedV6-UCI/SeedV6-UCI.exe'
# Or launch <candidate>/SeedV6-GUI/SeedV6-GUI.exe from this terminal.
Remove-Item Env:JAVA_TOOL_OPTIONS
```

Preserve any pre-existing JVM options when applying this in an operator environment.
UCI `uci` reports `searchProfile=LEGACY flags=3407871`; GUI binding identity reports
the same. Remove the override and restart to restore HL-G1. The explicit API is
`BaselineEngine.load(CONVENTIONAL_CP64, clockPolicy, SearchProfile.LEGACY)`;
`BaselineEngine.legacyConfiguration(...)` is the property-independent composition.
This restores the previous policy, not all bytes of historical build79.

## Qualification and artifact boundary

Local raw evidence is preserved under **`raw/production-build85-v1/`**, ignored by
Git. Fresh checkouts retain this report and reusable probes, not that raw output.
No competitive games, tournament, confirmation extension or external-engine study
was run. The short built-in Arena regression uses disposable test stores and
fixed shallow games solely to verify consumer routing and lifecycle.

- **184 JUnit tests passed**, zero failures/errors/skips: 30 core book/rules,
  58 core Search, 26 UCI, 3 GUI, 1 Arena, 66 selected research/control regressions.
  Compilation includes research launchers; project dependency boundaries passed.
- Production factories and actual kernels select HL-G1 at every W1-W16. Tests
  cover private worker state, all-worker execution/budget accounting, independent
  verified-null admissions, cancellation/interruption, truthful completion,
  legal PVs, board restoration, real-history/repetition, mate/ordinary scores,
  new-game/reset/reuse and drainage. W1 rollback traversal matches the explicit
  preserved nominal-verified-null control on real-history roots at Hash1/64.
- **22 Python packaging tests passed.** A test-fixture edit initially inserted
  tuples where strings were required; it was corrected and the suite rerun.
  The first UCI checker invocation used Python without `python-chess`; the existing
  `build/research/venv/Scripts/python.exe` supplied it. Both failed logs are retained.
- Before version finalization, plain **`.\gradlew.bat packageWindows`** passed in
  1m54s, producing candidate `candidate-20261102T090707-79b214e1`. Its GUI and UCI
  images/ZIPs and independent extracted copies passed existing packaging checks,
  with identical core SHA-256
  `e53ff82adb653dffaca382b8e0e4ff24a405b7a355c4eaeac8ccd2147d2c1338`.
  This is a provisional build84-labelled integration artifact, not the frozen champion.
- Its bundled GUI runtime executed the actual EDT/Play adapter for **44 requests
  per profile** (HL-G1 and LEGACY), W1/W4/W8/W16: depth5, movetime150ms, stop,
  new-game/reuse, ordinary/mate/stalemate scores, twofold/threefold, legal moves/PVs,
  restoration and shutdown. It did not open a graphical window.
- Its native UCI launcher passed `verify_cp64_production.py` for both profiles:
  W1-W16, Hash reconstruction, node budgets, legal PVs, terminals/history,
  movetime, BOUNDARY2/FACTUAL managed clocks, stop, stale-result suppression,
  new-game, Clear Hash, quit and EOF. Clock rebind preserves the Search profile.

Commands and reusable validation entry points:

```powershell
$env:GRADLE_USER_HOME = "$PWD/build/migration/gradle-user-home"
.\gradlew.bat :engine-core:test --tests 'com.ohinteractive.seedv6.search.*' -Pheadless -PconventionalVectorTests
# The retained gradle-tests-v1.txt records the additional module/test filters.
python -B -m unittest discover -s tools -p test_native_packaging.py
build/research/venv/Scripts/python.exe -B tools/verify_cp64_production.py --launcher '<native UCI executable>' --output '<new evidence directory>'
# Repeat the preceding check with --profile LEGACY and another new output directory.
```

`tools/gui-smoke/HlG1ProductionProbe.java` and the existing
`ConventionalGuiProbe.java` compile against the candidate GUI `app/*` JARs and run
on its bundled runtime; no research/test library is loaded into the application.
Normal native commands remain `.\gradlew.bat packageWindows` (both applications)
and `./gradlew packageMac` on native macOS. No packaging workflow was replaced.

Build **84→85** was verified by replacing only the build integer after integration
validation; major/minor/patch and all other version bytes were preserved. Build85
is attributable to this shipped default/rollback integration, not research acceptance.
Clean-source build85 artifact construction and final runtime identity verification
follow the implementation commit and are recorded in the closeout below.

## Limits and human dependencies

Accepted W1 confirmation remains **56.40625% /160 fresh pairs**; direct W4 versus79
remains **46.09% /32 pairs**, adverse/inconclusive. W4 superiority and neutrality
are unestablished; all-worker technical integration does not change those conclusions.
No new strength or clock-policy claim. Native macOS, interactive visual GUI and
live external-client acceptance were not performed. These limit those claims,
not the authorized Windows/headless experimental integration.

No Lichess/BotLi configuration, installed executable or active bot was changed.
No push, release or deployment. Refresh both Phase4 Project Source mirrors to
**P4-R005/P4-F021** before mirror-dependent planning (non-blocking for this repository
integration; blocking for that later dependent planning). Phase3 mirrors stay frozen.
Deployment or another research objective requires separate owner authority.
