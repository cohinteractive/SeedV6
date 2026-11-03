# Engine formulation identity and About — 4 November 2026

Bounded identification workstream; implementation/qualification evidence for separate
GPT/owner reconciliation. This record does not grant acceptance, deployment or release.

## Verified starting point

Clean `main`, `a8d4681f2ec3`, version 0.0.0/build90. No root
`CODEXLOG_CURRENT.md`. The current masters are **P4-R016/P4-F032**.
The [TC-P1 production record](research/search/p4-tactical/PRODUCTION.md) and canon
record owner adoption, completed Windows integration and qualification, with no
blocking integration action. Production commit `5f093f3a716c` is an ancestor of
HEAD; intervening changes are documentation/evidence only. Both retained build90
GUI/UCI ZIP hashes and version-file hash match that record.

Production remains **TC_P1, flags1177812991**, HL-G1 plus bounded capture ProbCut,
W1-W16. Rollbacks remain HL_G1/104071167 and LEGACY/3407871. Research champion
HL-G1/build84, immutable P4-BASELINE-001/build79, failed TC-R001 strength outcome,
and accepted W4 competitive uncertainty are unchanged. No research campaign ran.

## Implementation and sources of truth

- `engine-core/.../search/driver/EngineFormulation.java`: immutable presentation
  snapshot of the actual `ConstructionSearch`, resolved evaluator identity and
  clock policy. Named descriptions require equality of the actual flags **and
  margins** with the existing authoritative factories. Arbitrary/custom flags
  receive a custom label. Enabled mechanisms are enumerated from
  `FlatSearchConfig.Feature`, and principal descriptions are filtered by those
  actual bits, independently of a selected profile name. Enabled does not imply
  a mechanism fired on a particular position.
- `BaselineEngine.java`: exposes that snapshot from the same
  `selectedConfiguration` used to create kernels. Existing model identity text is
  reused; pinned loaders already verify model bytes. The existing TC_P1 default
  now has one named constant used by both selection and the default description.
  Existing diagnostic identity fields and all defaults/flags remain unchanged.
- `app-gui/.../gui/AboutEnginePanel.java`, `GameController.java`, `ChessFrame.java`:
  the existing About action reads controller-owned, resolved Play bindings and
  effective worker count. It does not load models, create Search workers, read
  training stores or discover configuration on the EDT. About retains the exact
  `ApplicationVersion.aboutText()` information: version, build, version-updated
  date, source revision, desktop build and platform.
- `app-uci/.../uci/UciConfiguration.java`, `BaselineUciConfiguration.java`,
  `UciEngine.java`: the production adapter exposes the same structured snapshot.
  The existing UCI name and options remain unchanged. One additional standard
  `info string formulation snapshot: ...` line appears before `uciok`. It describes
  the configuration **at that handshake**. Repeating `uci` after Threads, Hash or
  ClockPolicy changes reports the reconstructed configuration; `isready` stays
  quiet except for `readyok`. No new command/option or dynamic Search/evaluator
  selection is introduced. Existing explicit historical/research launchers retain
  their own detailed identities rather than inheriting the production label.
- Three new focused test classes cover core, GUI and UCI identities.
  `tools/gui-smoke/AboutFormulationProbe.java` renders actual packaged Swing
  components offscreen using the application theme and bundled runtime.

`cfg-` is the first 16 hexadecimal digits of SHA-256 over the existing complete
Search configuration identity, full evaluator identity and clock-policy identity,
separated by LF. It includes margins, worker/hash/aspiration/parallel settings,
model digest, schema/backend/mapping where available, and clocks. It excludes
display labels, consumer and build timestamps. It is a compact comparison aid,
not an artifact-integrity digest; source/build provenance is shown separately.
The full existing identity remains copyable on the Configuration tab and in UCI.

## GUI presentation and scope

The About tab adds:

```text
Engine formulation
Production default (CP64):
TC-P1 — HL-G1 + bounded capture ProbCut

Play — current game bindings (including paused games)
Black: TC-P1 — HL-G1 + bounded capture ProbCut
Enabled: PVS, QXRAY quiescence, late-move reductions, late-move pruning, ...
Evaluator: Conventional NNUE CP64 · S6CNU002 · 8 heads · SHA-256 8431626c2fca
flags=1177812991 · W4 · Hash 64 MiB · aspiration ADAPTIVE16 · clock BOUNDARY2 · cfg-5388b4d069c3a16a
```

The actual enabled line lists all selected principal mechanisms, including retained
learning, history-guided reductions and bounded capture ProbCut for TC-P1.
An HL_G1 binding displays **HL-G1 — retained Search learning and history-guided
reductions**, flags104071167 and no enabled ProbCut, while the separate production
default remains visible. HCE/BREP2-32 show Construction V1/flags262143 and their
own evaluator identities. Explicit historical Play bindings keep their existing
policy/checkpoint/network descriptions; they are not renamed TC-P1. Those older
recursive bindings do not acquire a new flat-configuration fingerprint.

Only engine-controlled sides are shown, with White/Black labels for independent
bindings. Before Start Game the dialog explicitly says the production default is
not an active runtime; loading and human-only games have separate truthful states.
Scope is explicit: **Play bindings only; Arena selects its engines independently
and its current configuration is not observed here.** Arena continues to use the
same core factories and its existing participant identity telemetry. No campaign
state is read or altered by About.

The panel is 660x490 logical pixels, inside the existing icon/OK JOptionPane;
the offscreen render is 764x565 pixels at the tested scale. Both tabs contain
read-only, wrapping, scrollable, selectable text. Long/two-engine descriptions
remain scrollable. Default and HL_G1 renders were visually inspected.

## Automated qualification

Local logs, XML, renders and negative evidence are retained in ignored
`build/formulation-identity/`; they are not available from a fresh checkout.

- **167 selected JUnit tests passed, no failures/errors/skips:** 93 core Search,
  rules and book; 27 UCI; 6 GUI; 7 version/resource; 28 existing Search/control;
  5 GUI adapter lifecycle; 1 production Arena. This includes nine new identity
  tests, existing W1-W16 kernel selection, snapshot/property isolation,
  Hash/worker/clock reconstruction, legal PV/history/budgets/drain/reset, and the
  actual built-in Arena with three evaluators at W1/W4/W16. Arena tests are shallow
  routing/lifecycle checks, not playing-strength evidence.
- **32 Python packaging/JDK tests passed.** All 16 architecture fixture tests also
  passed. The first combined Python command used an incorrect import path for two
  packaging modules (two loader errors); the corrected invocation from `tools/`
  passed all 32. Logs retain both attempts.
- Project boundaries and research-workspace assembly passed. Existing version
  tests cover missing optional desktop metadata and unavailable Git metadata,
  embedded-artifact independence and invalid version-resource inputs.
- `packageWindows :app-uci:installDist -PcandidateId=formulation-precommit-v2`
  passed, including both native images, ZIPs, independently extracted copies,
  bundled-runtime probes and common core/model identity. These provisional
  artifacts are build90 **with uncommitted source**, not the historical build90.
- Core JAR comparison against preserved production build90: only
  `BaselineEngine.class`, `BaselineEngine$SearchProfile.class` and version
  properties changed; only `EngineFormulation` and its switch helper were added.
  All existing Search kernels/configuration definitions/evaluator classes and
  model entries are byte-identical. No entries were removed.

The broader, additional `MatchOnlyArenaTest` run had **21 failures** (20 network
variants plus fixed-model resume) accessing nonexistent `refs/best` in disposable
legacy match fixtures; three other tests in that class passed. The fixed-model
failure was reproduced using the retained, hash-verified **build90 runtime JARs**
and unchanged test bytecode. This is an inherited issue, outside the modified
production path; it was not repaired or counted as a pass. The first reproduction
attempt had a test-discovery classpath error; the corrected restricted replay
reproduced the actual `NoSuchFileException`. Both results are preserved.

The first package attempt correctly rejected a source-inventory change caused by
Python test-generated untracked cache files. Only those attributable cache files
were removed; a fresh candidate succeeded. The render probe initially lacked a
FlatLaf import; compilation and packaged rendering passed after adding it.

Reproduction commands are recorded verbatim in the logs. Tests use the existing
Gradle wrapper, a worktree-local Gradle home for non-packaging execution, selected
JUnit filters, `-Pheadless -PconventionalVectorTests`, and existing packaging
tasks. No finalizer/coordinator or governance change is involved.

## Source commit boundary

The attributable shipped GUI/core/UCI change warrants one build increment. Only
the build integer changed **90 → 91**; major/minor/patch and all other version
bytes were preserved. Strict tuple/regular-file and before/after byte checks passed;
resulting version SHA-256 is
`4e87e34cc8db4604f3b6e7d70127d4ce52a1f9144645cbe2cbf68b8b79136fa0`.
Final artifacts built from the committed revision and their executed consumer
checks were pending at the source commit boundary; the subsequent evidence below
records their completion without retroactively attributing it to that commit.
No push, installed-app replacement, bot change, deployment or release is authorized.

## Committed build91 artifacts and consumer verification

Implementation commit: **`4bc550b376295903f9c7d6428a9a3c303c6caa51`**.
The following command passed from that clean source in 1m30s:

```powershell
.\gradlew.bat --no-daemon --max-workers=1 packageWindows :app-uci:installDist :installResearchWorkspace -PcandidateId=formulation-build91-v1
```

Both native images, ZIPs and independent extracted copies passed the established
package gates. Bundled runtime: Temurin Java21.0.12.1+1, Windows x64. GUI, native
UCI, Java-only UCI and research workspace contain the identical core JAR,
SHA-256 `324947d9e08e7077f124e2307c1489d0b8504e8fba8683865bb93113712f54fb`.
Embedded About and protocol provenance is **0.0.0/build91/source4bc550b37629**,
with no uncommitted-source marker. The subsequent report-only commit does not
change these artifact identities or require another build increment.

| Artifact | Repository-relative path |
| --- | --- |
| GUI executable | `dist/windows/candidates/formulation-build91-v1/SeedV6-GUI/SeedV6-GUI.exe` |
| GUI ZIP | `dist/windows/candidates/formulation-build91-v1/SeedV6-GUI-windows-x64.zip` |
| UCI executable | `releases/candidates/windows-x64/formulation-build91-v1/SeedV6-UCI/SeedV6-UCI.exe` |
| UCI ZIP | `releases/candidates/windows-x64/formulation-build91-v1/SeedV6-UCI-windows-x64.zip` |
| Java UCI launcher | `app-uci/build/install/seedv6-uci/bin/seedv6-uci.bat` |

GUI ZIP SHA-256:
`ce7c7511e6e96c4764a38aff61e8b51e332cfe633bbe02ff1ef9229773b19971`.
UCI ZIP SHA-256:
`e4afeaa9a13bb6ed1427ee574e73cad84dfff313af8b6f38758737c0f71aa361`.
The adjacent `candidate.json` files retain full source/runtime/inventory receipts;
their SHA-256 values are respectively
`11c8e999813b24da88daeefaf605a6e22b401a149a892533a4fa6247d8dec0f1` and
`f5cd07c2d92f01997ecfd3a7b283fbdffd96a8a5fb5b9d856f7b6032da50297b`.

- **Packaged GUI Play:** `HlG1ProductionProbe`, compiled against final GUI JARs
  and run on the bundled runtime, passed **44 requests per profile**, TC_P1,
  HL_G1 and LEGACY, W1/W4/W8/W16. Checks cover depth, movetime, stop, real-history
  repetition/terminals/mates, legal moves/PVs, restoration, reuse and drain.
- **Native UCI:** existing `verify_cp64_production.py` passed each of the same
  three profiles, **two sessions/46 search responses per profile, all W1-W16**,
  including reconstruction, node/time/clock requests, legal PVs, terminals,
  repetition, stop/reset/Clear Hash/quit/active EOF. These are operational checks,
  not competitive clock or playing-strength evidence.
- **Cross-consumer identity:** eight additional native/Java-only handshake sessions
  (no override, explicit TC_P1, HL_G1 and LEGACY on each launcher) passed. Each
  recorded W1/Hash64/BOUNDARY2, W4/Hash64/BOUNDARY2, then W16/Hash1/FACTUAL.
  Native and Java outputs matched; explicit TC_P1 matched no override; IDs changed
  with runtime settings. W4 IDs matched the packaged GUI binding exactly:

  | Effective profile | Flags | W4 / Hash64 / BOUNDARY2 ID |
  | --- | ---: | --- |
  | TC_P1 | 1177812991 | `cfg-5388b4d069c3a16a` |
  | HL_G1 | 104071167 | `cfg-f941509e3c9ea0f4` |
  | LEGACY | 3407871 | `cfg-d55ff96f603078d1` |

  All handshake terminators/readiness responses were preserved. A native invalid
  selector exited nonzero with the existing selector error. Full output and the
  reproducible check script are retained at `final-identities/` and
  `verify-identities.py` under the local evidence directory.
- **Packaged About:** final idle/current-game panels rendered successfully for
  all three profiles; default and rollback images were inspected. Final build and
  source metadata are visible; rollback's enabled list excludes ProbCut. The
  source-version comparison against build90 was repeated with the final core and
  confirmed the same limited class changes described above.

Evidence locations under `build/formulation-identity/`: `package-build91-v1.log`,
`gui-build91-{TC_P1,HL_G1,LEGACY}/`, `gui-service-build91-*.log`,
`uci-build91-*/result.json`, and `final-identities/summary.json`. Failures and
provisional runs remain at their earlier paths. Final version bytes still match
the verified build91 hash. No root journal was created; no unrelated source
changes were present or absorbed. The final report commit contains only this
qualification update; the resulting working tree is checked separately at handback.

## Remaining qualification boundary

Native interactive Swing presentation has **not** been verified. The required
`mcp__playwright__browser_tabs(action="list")` initialized, but the mandated browser
tools cannot control the native Swing window. Offscreen rendering and automated
EDT/service tests are explicitly not interactive acceptance. Native macOS,
external third-party GUI/client acceptance, installation replacement, deployment
and release were not performed.

Human actions required after this prompt:

- **Blocking for interactive presentation acceptance; non-blocking for the
  implementation and automated Windows qualification:** owner opens the final
  candidate, inspects Help/About before and after Start Game, checks both tabs,
  then repeats with `-Dseedv6.searchProfile=HL_G1` to confirm readable override
  presentation and absence of enabled ProbCut. Check the owner's normal display
  scaling; an Engine vs Engine selection should show independent White/Black
  bindings. Close the candidate normally. No saved training campaign needs running.
- **Blocking for workstream acceptance:** GPT/owner reconciliation of this evidence
  and its explicit limits. No acceptance is claimed by a commit or this report.
- **Non-blocking:** track the inherited legacy `MatchOnlyArenaTest` fixture issue
  separately if that historical checkpoint workflow is to be qualified.
