# TC-P1 rolling experimental production integration - 4 November 2026

This bounded integration implements the owner's explicit adoption of exact TC-P1
across W1-W16, accepting the unresolved W4 competitive warning. It is separate
from TC-R001's completed, unsuccessful strength-development execution. Starting
state: clean main1f55124b,0.0.0/build89, mastersP4-R015/P4-F031; no root journal.
Current canon is P4-R016/P4-F032; focused tactical CONTRACT revision2. No shared
behavioral governance, frozen experiment, model, clock policy or search parameter
is changed. Production source is **5f093f3a716cc218c48a7ecc54bbfc1eca44f364**, **0.0.0/build90**.
The subsequent documentation/evidence commit records final artifacts without another
version change. [Compact qualification manifest](production-build90-qualification.json)
records hashes, commands/evidence locations and exact scope.

## Exact selection and reproducibility

Normal GUI Play, built-in Arena and standalone UCI use **TC_P1**, exact **HL-G1 +
bounded capture ProbCut**, **flags1177812991**. The one runtime edit is in shared
`BaselineEngine`: default selector plus TACTICAL_VERIFICATION over HL-G1. No new
Search implementation. The kernel/configuration/parameters from TC-P1 implementation
`a7c45dffeaffc362bc7c7026d03a2e6434472be2` are preserved: eligible noncheck interior
null-window depth4-8; raw+300 guard; at most3 SEE-nonnegative legal tacticals;
beta+150 qsearch then depth-3 child verification; selective original-beta return.
Probe isolation suppresses nested verification and learning/TT side effects while
real moves extend real history and all work consumes the request budget.

Unchanged: frozen CP64/checkpoint64/S6CNU002/eight heads/BT4_Q_LOGIT400_V1,
PVS/ADAPTIVE16, requested Hash64, BOUNDARY2 with explicit FACTUAL fallback, default
W1, supported W1-W16/bounded Max, OWNER at W1 and ROOT_REUSE at W2-W16. CP64 file
SHA2568431626c2fca81e0a1e9ccd5e45ddd8829e6e3282c756c21fad75b06d075bb2b.
HCE, BREP2-32 and unrelated payloads retain flags262143. Explicit historical
Construction/Policy4/E008/research profiles remain independent.

| Selection | Meaning | Flags |
| --- | --- | ---: |
| No property, or `-Dseedv6.searchProfile=TC_P1` | Current rolling production: HL-G1 + exact TC-P1 | 1177812991 |
| `-Dseedv6.searchProfile=HL_G1` | Previous HL-G1-only production rollback | 104071167 |
| `-Dseedv6.searchProfile=LEGACY` | Established pre-HL rollback with current correctness fixes | 3407871 |
| Explicit research `--profile probcut` | Frozen TC-P1 formulation | 1177812991 |
| Explicit research `--profile guided` | Frozen HL-G1 formulation | 104071167 |

Invalid production or research selectors fail. Explicit core API selection wins;
bindings snapshot selection and preserve it across clock/hash/worker reconstruction,
reset and reuse. Research profiles do not inherit the production property. Exact
historical binaries still require pinned source/artifacts, not merely matching flags.
The37-file frozen89 and33-file frozen84 manifests were present and hash-verified.

For a native GUI or UCI rollback, launch from a separate PowerShell with a
process-local JVM option (preserve existing options if present):

```powershell
$previousOptions = $env:JAVA_TOOL_OPTIONS
$env:JAVA_TOOL_OPTIONS = "$previousOptions -Dseedv6.searchProfile=HL_G1".Trim()
& '<candidate>/SeedV6-UCI/SeedV6-UCI.exe'
# GUI: & '<candidate>/SeedV6-GUI/SeedV6-GUI.exe'
$env:JAVA_TOOL_OPTIONS = $previousOptions
```

Restart without the override to restore TC_P1. Java-only UCI supports
`SEEDV6_UCI_OPTS=-Dseedv6.searchProfile=HL_G1` with its generated launcher.
`BaselineEngine.load(CONVENTIONAL_CP64, clocks, SearchProfile.HL_G1)` is the explicit
API. The historical `LEGACY` meaning is unchanged.

## Research outcome and boundaries

TC-R001 completed640 independent W1 games:235W/202D/203L,52.50%, paired-normal95%
49.37-55.63%. Both original gates failed (>=53% observed and lower strictly>50%).
The sole attempt remains spent; no campaign or allowance is renewed. W4 completed
96 games:26W/32D/38L,43.75%, normal95%34.94-52.56%, an unresolved adverse warning,
neither proven regression nor neutrality. The owner explicitly accepted this risk
for adoption. Integration value and potential future combination value justify the
owner decision; adoption is not new competitive evidence or confirmed superiority.

**HL-G1/build84**, sourceb8ea8b2856eed3f6c774860ca540375054e4de30, remains research
champion. **P4-BASELINE-001/build79**, source753367614b8aa7e32dee337fd76b542737141ac1,
remains immutable. Future authorized research normally starts from the latest
owner-adopted rolling production baseline, while older controls/reverse ablations/
substitutions remain available. No About enhancement, concurrency programme,
active-bot change, push, release, installation replacement or deployment.

## Qualification and artifact record

Local raw output is retained under `raw/production-adoption-v1/`, ignored by Git;
fresh checkouts must check availability. Reused accepted TC-P1 evidence includes
W1/W4 probes, legal history/score/PV, isolated learning/TT effects, full-request
accounting and lifecycle checks linked from STATE and build89 qualification.
No competitive strength games or independent confirmation were run for adoption.
The shallow built-in Arena regression exercises routing/lifecycle only.

The initial integration regression had147 passing tests and one new overstrict
coverage assertion: a100000-node W2 request need not complete an eligible iteration
before aborted counters reset. This was a test-observation error, not evidence of a
kernel defect. The failed output/XML is preserved. The correction restricts the
deterministic completed-counter witness to W1; all16 actual production routes,
exact worker configs, private state, budgets and lifecycle remain checked. Frozen
W4 per-worker mechanism observations retain their original scope.

Qualification below distinguishes source tests, packaged runtime and native UI execution.

- **148 JUnit tests pass**,0 failures/errors/skips:88 core Search/rules/book,
  26 UCI,5 GUI,1 built-in Arena,28 selected production/research/control regressions.
  Project boundaries and research-workspace build pass.22 Python packaging tests pass.
- Actual W1-W16 kernels match exact probcut flags with private board/PV/history/
  evaluator state. W1 production and HL_G1 rollback match their explicit research
  traversals on four real-history roots, Hash1/64, repeats/reset and100000-node caps.
  LEGACY also retains its separate frozen-control parity. Tests exercise terminal,
  repetition/mate, legal PV, board restoration, verified-null/full-request accounting,
  hard limits, cancellation/failure, drain/reset/reuse and shutdown.
- `gradlew.bat --no-daemon --max-workers=1 packageWindows
  -PcandidateId=tc-p1-adoption-precommit-v1` passed in2m17s. Both native images/ZIPs,
  extracted copies and shared core/model identity passed established packaging gates.
  These provisional artifacts are labelled build89/uncommitted source, **not** the
  historical research build89. Final committed build90 artifacts supersede them for use.
- At the source commit boundary, final packaged checks were pending. They are now
  complete as recorded below; the source commit is not retroactively claimed to
  contain later evidence.


## Committed build90 artifacts and executed consumer checks

`gradlew.bat --no-daemon --max-workers=1 packageWindows :app-uci:installDist
:installResearchWorkspace -PcandidateId=tc-p1-build90-v2` passed in1m34s from clean
source **5f093f3a716c**. Both consumers' images, ZIPs and independently extracted
copies passed native packaging checks. Bundled runtime is Temurin Java21.0.12.1+1,
Windows x64; compilation uses the established Java21 toolchain. No global JDK,
model selection, launcher policy or installed application was replaced.

| Artifact | Repository-relative path |
| --- | --- |
| GUI executable | `dist/windows/candidates/tc-p1-build90-v2/SeedV6-GUI/SeedV6-GUI.exe` |
| GUI ZIP | `dist/windows/candidates/tc-p1-build90-v2/SeedV6-GUI-windows-x64.zip` |
| UCI executable | `releases/candidates/windows-x64/tc-p1-build90-v2/SeedV6-UCI/SeedV6-UCI.exe` |
| UCI ZIP | `releases/candidates/windows-x64/tc-p1-build90-v2/SeedV6-UCI-windows-x64.zip` |
| Java-only UCI launcher | `app-uci/build/install/seedv6-uci/bin/seedv6-uci.bat` |

Launch either native executable directly from its complete image directory, or
extract its complete ZIP and launch there. Normal launches require no profile
property. The generated Java-only launcher requires installed Java21. GUI About
and UCI runtime identity both report **0.0.0/build90/source5f093f3a716c**. The owner's
older build77/source78b372c3424b screenshot identifies another artifact; it was not
updated or overwritten by this work.

Shared engine-core SHA256 (GUI, UCI, Java distribution and research workspace):
`3e047b7ec4f5d261e90d8a875addb0074b11a154cbffccc549002b433b11dfd9`.
GUI ZIP SHA256: `a7091426debfe3eab769d57f203cce637d560dcd69ec5e113a36507c7e5f65cf`.
UCI ZIP SHA256: `dc0d95a60600fb3fd6f6a29a48d3c5b57cbeffac2b7a1a66b1496cb221a17735`.
Both candidate receipts/manifests retain source/build provenance and inventories.

- **Native UCI:** `verify_cp64_production.py` passes for no-property TC_P1, explicit
  HL_G1 and LEGACY, each all W1-W16/two sessions/46 completed search responses.
  It covers Hash/worker reconstruction, node caps, movetime, controlled
  BOUNDARY2/FACTUAL managed-clock requests and profile retention, legal PVs,
  terminals/mates/repetition, stop, reset/stale-result suppression, Clear Hash,
  quit and active-search EOF. These are operational clock checks, not competitive
  timing equivalence. Java-only launchers separately pass all three profiles at
  W1/W16 depth/stop/quit, using SEEDV6_UCI_OPTS for explicit rollback.
- **Packaged GUI Play service:** the retained `HlG1ProductionProbe` (now supporting
  all three profiles without renaming its historical entry point) runs against
  the actual GUI app JARs on its bundled runtime. **44 requests per profile**,
  W1/W4/W8/W16, pass depth/movetime/stop, reset/reuse, real twofold/threefold,
  mate/stalemate/ordinary-score, legal moves/PVs, restoration and shutdown checks.
  This part is headless EDT/service evidence, not interactive acceptance.
- **Native Windows GUI:** the new executable was separately launched through
  Computer Use without a Search override. About displayed version0.0.0/build90,
  source5f093f3a716c, Windows/x64. Existing user settings (human Black, W4,5000ms)
  were unchanged. A non-book legal FEN was loaded; after human `e5d4`, the CP64
  engine replied `f3d4` (Nxd4), displayed completed depth11/PV and TIME_LIMIT,
  then0/4 active threads. The11-move displayed PV was independently replayed
  legally. Stop Game and normal window close completed; all candidate processes
  drained. The packaged Arena view was inspected; its existing paused read-only
  campaign was not resumed or changed. Actual built-in Arena execution is the
  separate shallow source regression described above, not an interactive campaign.
- **Frozen behavior bridge:**48 bounded W1 requests on four positions/real histories
  at Hash1/64 show exact published depth/score/node/PV, full-request nodes and
  bestmove parity: packaged default = frozen89 probcut = current explicit probcut;
  packaged HL_G1 = frozen84 guided = current explicit guided. Current research
  profiles ignore conflicting production overrides. Invalid production selector
  exits nonzero rather than falling back. All37 frozen89/33 frozen84 runtime files
  still match their manifests. Core ZIP entry sets match frozen89; only
  BaselineEngine.class, BaselineEngine$SearchProfile.class and application-version
  properties differ. ExactSearch/FlatSearchConfig and all other core entries are
  byte-identical. No ProbCut parameter or algorithm changed.

The build90-v1 packaging attempt was stopped after commit whitespace validation
rejected a newly authored report's line endings and the shell had continued. Its
log is retained. The report was normalized, the source commit completed, and fresh
v2 artifacts were built from the clean revision. There was one verified89->90 bump;
no failed attempt is treated as a successful artifact or another build increment.

Reproduction entry points (use fresh evidence output directories):

```powershell
build/research/venv/Scripts/python.exe -B tools/verify_cp64_production.py --launcher '<UCI executable>' --output '<new directory>'
# Repeat with --profile HL_G1 and --profile LEGACY into distinct new directories.
# Compile ConventionalGuiProbe.java and tools/gui-smoke/HlG1ProductionProbe.java
# against <GUI image>/app/*, then run on <GUI image>/runtime/bin/java.exe.
```

The retained raw logs include exact Gradle invocations, JUnit XML/summary, GUI
transcripts, UCI commands/responses and `frozen_bridge.py`; the compact manifest
hashes the evidence used here. Raw evidence is local-only and not force-added.

## Completion limits and human actions

This bounded Windows production integration and qualification is complete. The
rolling baseline is **TC_P1/HL-G1 + ProbCut, flags1177812991, build90, source5f093f3a716c**.
No newly discovered correctness, concurrency or lifecycle defect remains. W4's
competitive warning, failed TC-R001 strength criterion and all historical clock/
champion qualifications remain explicit. No claim of broad interactive acceptance,
native macOS/mobile qualification, training-workflow validation or live external
client acceptance is made. The automatic GUI package receipt remains headless-only;
the later bounded native GUI check is separately recorded, not silently substituted.

No installed GUI replacement, bot configuration, active Lichess change, push,
release or deployment occurred. About formulation visibility and concurrency
research remain separate owner-directed workstreams. Version bytes after the sole
bump have SHA256 `24178904b9ae3360bd48abea0f1acc9b909b02cc1fbc319df0cf56dae113df6e`.
Root CODEXLOG_CURRENT.md is absent and was not created; no finalizer/coordinator ran.

Human actions required after this prompt:

- **Non-blocking for completed integration; blocking before mirror-dependent
  planning:** owner refresh of `CHESS_SEARCH_CONTRACT.md` to **P4-R016** and
  `CHESS_SEARCH_RESEARCH_FRONTIER.md` to **P4-F032** in the Phase4 ChatGPT Project
  Sources. No automatic refresh is claimed; Phase3 mirrors remain unchanged.
- **Blocking for this completed Windows integration:** None.
- Native macOS or deployment acceptance, if later requested, requires its own
  qualification/authority; it is outside this unit and not an integration blocker.
