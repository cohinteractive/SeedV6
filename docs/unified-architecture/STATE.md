# Unified architecture migration state

Updated 16 October 2026 (Pacific/Auckland). Governing [CONTRACT](CONTRACT.md),
[BASELINE / preservation](BASELINE.md), [ACCEPTANCE matrix](ACCEPTANCE.md),
[foundation evidence](foundation-evidence.json).

## Current boundary

**WU0-WU6 are accepted by GPT. WU7 packaging implementation and available Windows/
portable validation are complete, pending GPT/user reconciliation; full cross-platform
acceptance is not complete.** The owner WU7 launch
accepts `328531f672260cf4fd5e49b26724fee7329bb0d8` and authorizes native candidate
packaging with no version bump. [WU7 packaging](WU7-PACKAGING.md) records commands,
private model inputs, provenance and native/human gates. No desktop interaction
is authorized. Mac-native execution, human GUI/client acceptance and the 19
inherited failures remain open; WU8 does not advance automatically.

Architectural discovery on 16 October 2026 is complete. The owner accepted
restructuring existing SeedV6 into engine-core, engine-training, app-gui, app-uci
and research, with shared Gradle build logic and one production engine. No new
repository/history replacement or independently maintained engine copies.

Migration worktree: `C:/projects/seed/java/seedv6-unified`.
Branch: `migration/unified-architecture`.
Starting revision: `24720213e12d99a581a713c74f901f89899bf2b4`.
Accepted production search: `35294146bb7e44975b516005b2cc196439666e09`, with
SR-024 acceptance evidenced in BASELINE. Build remains `0.0.0`, build **35**.
The branch starts at later main to preserve other capabilities/evidence; SR-021
research seams are present but not accepted for production.

## Stage ledger

| Unit | Status | Evidence / next gate |
| --- | --- | --- |
| Discovery | COMPLETE; five-project decision accepted by owner | Owner's WU0 launch, independently verified repository/source/artifact identities in BASELINE. |
| WU0 Foundation | ACCEPTED by GPT | Owner's WU1 launch confirms acceptance of `81e868a72b942202e1ced89fe30a5a13e3af155f`; G0 evidence below is retained. |
| WU1 Gradle foundation | ACCEPTED by GPT | Owner WU2 launch accepts `bd864a9f1231e6c73997b9302a411449bc2a5a21`; all 19 inherited failures remain explicitly deferred. |
| WU2 Core | ACCEPTED by GPT | Owner WU3 launch accepts `09076c1d06ac8b532e065f4750bd0d8b206fe7db`; [WU2 record](WU2-CORE.md). |
| WU3 Training | ACCEPTED by GPT | Owner WU4 launch accepts `ff7291aae877244843e99afa3c47120642f25bc5`; 19 inherited failures remain open at dependent runtime gates. |
| WU4 GUI | ACCEPTED by GPT (structural scope) | 75 GUI sources / 35 resources in app-gui; scoped non-interactive checks complete; 19 inherited failures retained; Windows/macOS human G2-G5 deferred to WU7. |
| WU5 UCI | ACCEPTED by GPT | Standalone core-only Java UCI, explicit legacy profile, 159 standalone requests and shared production/model parity; native/client gates remain WU7. |
| WU6 Research | ACCEPTED by GPT | [WU6 record](WU6-RESEARCH.md), [evidence](wu6-evidence.json); research owns tools/references, production activation/classpaths isolated, compiled core seams disclosed. |
| WU7 Packaging | Implementation and available validation COMPLETE; GPT/user reconciliation pending | [Implementation](WU7-PACKAGING.md), [evidence](wu7-evidence.json); native Mac/human gates open; WU8 remains gated. |
| WU8 Cutover | NOT STARTED | Applicable matrix passed/scoped, rollback demonstrated and explicit owner decision. |

## WU0 evidence and validation

- Primary and UE remote main IDs match live `git ls-remote` results. Local
  ancestry verifies accepted primary integration/closeout and UE SR-024 in remote
  main. Original primary was clean; UE's known staged/unmerged state was captured.
- Unoccupied sibling path and branch checked before `git worktree add`; Git common
  directory confirms one existing repository, with separate worktree/index/HEAD.
  No active research process was observed. Original branch was never switched.
- All 15 frozen SR-024 installation hashes match. All four selected checkpoint
  file hashes match; local UE runtime model matches. Phase 1 archives and UE
  immutable SR-024 fixtures checked. No model bytes copied or added to Git.
- Production manifest versus current source identifies five changed existing
  entries and the new SR-021 FutilityPolicy. Accepted-revision blob verification
  identifies only the known gradlew.bat CRLF conversion. Static factory inspection
  supports the production/research distinction; no new runtime parity claim.
- Existing Windows v6 ZIP inspected without extraction: source/release metadata
  identifies SR-036 and local c6bdd524; its JAR hash differs from the reported
  SR-024 Mac JAR. Missing unpacked Windows v6 image and uninspected Mac evidence
  remain explicit. Earlier existing package files are covered by preservation
  hashes, not rebuilt.
- Preservation comparison covers original primary/UE tracked-file bytes,
  working-file size/mtime inventories, HEAD/branch/status and protected release/
  frozen-image hashes. UE index/unmerged entries and merge metadata are checked.
  Compact results and scope are in foundation-evidence.json. This does not claim
  to have rehashed every large ignored research output or examined Mac local state.
- The first strict inventory comparison found five original-checkout ignored
  Gradle cache/report files with changed modification times and unchanged sizes:
  three cache lock files, `fileChanges/last-build.bin`, and
  `build/reports/problems/problems-report.html`. Exact before/after observations
  are retained in foundation-evidence.json. These are untracked/ignored generated
  state, outside frozen research/artifact roots; tracked bytes, index, HEAD and
  protected artifact hashes still match. IDE/Gradle processes were present, but
  authorship is not proven or assigned to WU0. No Gradle/build command was run by
  this task. The follow-up check permits only these inspected environmental
  observations and still rejects any source, protected artifact or UE change.
  Whole-original-working-tree immutability is therefore not claimed.
- Documentation validation checks local links, stage/matrix consistency and the
  exact changed-file scope; `git diff --check` is required before commit. Only
  README navigation and `docs/unified-architecture/` are attributable changes.
- Completed one-off Python identity/preservation/documentation validation:
  **PASS with the recorded original-checkout environmental limitation**.
  Checked 3,597 primary and 110 UE tracked files; 1,098 primary and 590 UE
  protected artifact files; inventories of 44,076 primary and 2,706 UE working
  files. UE inventory/index/merge metadata are unchanged. Validated 26 local
  documentation links, nine stage IDs, 28 matrix rows and all five project owners.
  Windows v6 ZIP CRC check passed; no artifact was extracted or executed.
  `git diff --check` passed. Exact new paths were inspected before staging;
  representative raw JAR/JSONL output remains ignored and the frozen SR-024
  opening PGN remains tracked. No ignore exception or raw output is added.
- Application/Gradle tests, native GUI/client/platform execution, strength runs,
  trainer recompilation and full external corpus hashing were not run: WU0 changes
  documentation only. Their future gates are explicitly pending in ACCEPTANCE.

### Version and journal bookkeeping

Both pinned shared helper hashes and maintained commit were verified. Original
checkout's optional local begin stalled during pre-reservation full-tree capture;
only this invocation's verified process was stopped. Subsequent local status was
idle, active null, unfinished empty, blockers empty. Under the SeedV6-specific
non-critical invocation override this is a **non-blocking version warning**,
not a completed begin, bump or repair requirement.

Migration-worktree begin succeeded with token
`5ba25c5420b34a449d542858003329c3`. WU0 selects `no_bump`; same-token finish is the
final substantive bookkeeping boundary after validation. Its receipt is retained
under the migration worktree's resolved Git metadata and reported at handback.
No manual version edits or replacement bump. Root `CODEXLOG_CURRENT.md` is absent
in both SeedV6 worktrees and UE; none is created. UE has no VERSION_STATE.txt,
and no finalizer is run there. No shared registry/helper was modified.

## WU1 evidence and validation

- Six included Gradle projects: the five final owners and explicitly temporary
  `:app`. All Java implementation remains in `app/src`; target projects are empty.
  Core has no project dependency; training consumes core; GUI and research consume
  core/training; UCI consumes core. Legacy app currently has no project dependencies.
  Its allowed future dependencies are only core/training. No target consumes app.
- Shared Maven Central / Java 21 / JUnit conventions preserve the existing
  versions. Wrapper remains 8.11.1. The legacy configured task/dependency/source-set/
  toolchain/compiler/main-class/output snapshot is identical before and after.
  Existing packaging implementations and paths are untouched.
- `verifyProjectBoundaries` enforces all dependency configurations, project
  identities, cycle rejection and non-overlapping Java source roots. Configuration
  and compile/check hooks enforce it; configuration-cache replay is supported.
- Baseline compilation of main, verification and test source sets passed, and
  59 focused version/UCI/production-search tests passed. The nine disposable Gradle
  boundary tests passed, including invalid graph and configuration-cache cases.
- Final bounded core/search/UCI/version regression passed 1,006 of 1,017 cases,
  with 11 existing opt-in skips and zero failures/errors (3m48s). All 97 legacy
  task names and the measured build model are preserved. Actual target compilation,
  cache replay and both packaging dry-runs pass with `--warning-mode fail`.
- The expanded headless suite observed ten GUI failures and nine checkpoint-restart
  failures, all reproduced with WU0 build scripts and separate outputs. It was
  stopped after 12m39s; no complete suite count/pass is claimed. No source, test
  expectation, global filter or ignored-failure setting was changed.
- The owner's final gate decision is: "Yes—commit WU1 and document all 19 inherited
  failures for separate remediation." This scopes the WU1 commit gate only;
  the full suite remains failed/incomplete and later runtime acceptance is open.
  No source remediation or WU2 implementation is authorized by that deferral.
- All 716 main, 311 verification and 922 test class files, and all 756 application
  JAR entries, are byte-identical to the WU0 build replay. Both native packaging
  task graphs pass dry-run validation. No native package is executed or certified.
- Original SeedV6: 3,597 tracked files, 1,098 protected artifacts, 44,076 size/mtime
  inventory entries and recorded Git metadata are unchanged. UE: 110 tracked files,
  590 protected artifacts, 2,706 inventory entries and all recorded merge/index
  metadata are unchanged. All 15 SR-024 frozen installation hashes still match WU0.
- Version is still `0.0.0`, build 35. Local finalizer token
  `43045c25824846d28384c4b412f1bdac` finished `no_bump`, with no warnings/blockers
  and identical version bytes. Subsequent status is idle with no active/unfinished
  operation. No journal exists, no production package is built, no
  public model bytes are copied and no historical checkout is written.

## WU2 evidence and validation

- Core is the sole runtime owner: 108 Java sources, two runtime resources and nine
  optional native/provider/provenance files. App has an explicit core dependency;
  all remaining target projects stay scaffolds. All 50 search source files are
  byte-identical to WU1. Inference geometry/weights and five runtime codecs are
  independent of trainers/stores; persisted formats and training semantics stay intact.
- Frozen accepted-primary JAR and immutable UE probe both match the relocated
  core on all 203 source vectors/17 fields. Real-model UCI passes 10 exact
  comparisons and 66 requests across 1..16 workers and lifecycle transitions.
  Seven model/optimizer families match frozen bytes; real generation-32 model
  recompilation and a private one-batch optimizer continuation match exactly.
- Final engine/search/UCI/version run: 989 cases, 978 passed, 11 existing skips;
  core 61/61 and focused retests 24/24 pass. Reconciled selected coverage is 1,278
  distinct cases: 1,241 passed, 19 inherited failures, 18 existing skips. The 19
  failure names/messages exactly match WU1. All new layout/classpath test failures
  are resolved; no suppression or unrelated remediation. See WU2 evidence for
  initial failed runs and actual commands. Full unfiltered/slow suites were not run.
- Nine dependency-boundary fixtures, actual target compilation, configuration-cache
  reuse, installDist and packaging dry-runs pass. Core resolves only java.base.
  The optional Windows JNI provider compiles and loads from the core JAR alone.
  Native GUI/macOS/client and final distribution acceptance remain later gates.
- Distribution class ownership is unique. All 220 GUI classes and CheckpointStore
  remain byte-identical to WU1. Both historical repositories' tracked/protected
  bytes, recorded Git metadata and full size/mtime inventories match before/after.
  Frozen inputs remain unchanged; no public model bytes, push or release.
- SR-021 remains deferred and production-disabled. Embedded research seams and
  remaining verification ownership are explicitly deferred to WU6; complete
  experimental isolation is not claimed. [WU2](WU2-CORE.md) records the split,
  [evidence](wu2-evidence.json) records reproducible inputs, checks and limitations.
- Version remains 0.0.0 build 35. Local finalizer token
  `0374b98ef93d43d8a5d92003a4b09292` completed `no_bump`, without warnings/blockers;
  version bytes are identical and subsequent status is idle. No journal exists or
  was created. The attributable WU2 commit is the commit introducing this record;
  no push, deployment or WU3 implementation follows.

## WU3 evidence and validation

- Training is the sole owner of 134 unchanged Java sources and one binpack notice:
  trainers/Adam/codecs, model workspace/checkpoints/lineage, corpus/data,
  self-play, validation, telemetry/history and reusable campaign services.
  Four independent tests move unchanged. Core's 108 sources remain untouched;
  no new interface, format, numerical or persisted-identity change is needed.
- Training exposes core through `api`; legacy app explicitly depends on both.
  Gson/SQLite/Zstandard move to training with unchanged versions. GUI/UCI/main
  entry points and independent verification/research consumers remain in app.
  All 722 production class byte payloads are unchanged and uniquely owned.
- Seven families pass exact before/after disk optimizer continuation, checkpoint
  reconstruction, cursor/reservation and lineage/catalog checks. A private clone
  of the real generation-32 Pair-2 checkpoint retains its UUID and step; exact
  recompilation/private continuation match the pinned model. All 203 SR-024 vectors
  and 66 real-model UCI requests pass. SR-021 remains deferred/disabled.
- Core 61/61 and training 11/11 pass independently. Engine/search/UCI/version
  coverage passes 976 with 11 skips. Final application/sequential replay passes
  86 with six skips; isolated session replay passes 5/5. Reconciled selected
  coverage is 1,643 distinct cases: 1,598 passed, 19 inherited failures, 26 skips.
  All 19 names/messages match WU2. Corpus classpaths and two timing-sensitive
  fixtures were repaired without changing production behavior or suppressing tests.
  Initial adverse runs remain recorded; full unfiltered/slow suites were not run.
- The nine interrupted-archive restart failures are not certified by normal
  continuation tests. Independent positive evidence supports this structural
  extraction; separate crash-recovery and GUI remediation remains required at
  dependent runtime gates. Native Swing/macOS/client acceptance remains later work.
- Nine Gradle boundary fixtures, target compilation, configuration-cache reuse,
  installDist and Windows/Mac packaging dry-runs pass. Core resolves only java.base;
  training-only probes run without legacy app. Historical repository inventories,
  Git metadata and protected artifacts, plus the pinned checkpoint/lineage, match
  before/after. No production release, model publication or historical cleanup.
- Version remains 0.0.0 build 35; finalizer token
  `1d00be72962646729db46bb2039c166e` completed `no_bump` without warnings/blockers,
  version bytes remain identical and subsequent status is idle. Receipt is in WU3 evidence. The WU3
  commit is the commit introducing that record. No journal exists or was created.

## Outstanding decisions and limitations

| Item | Effect on next work |
| --- | --- |
| WU4 validation/reconciliation | Structural checks complete under the explicit owner decision; GPT/user reconciliation before WU5. Native Windows/macOS acceptance remains a later WU7/full-runtime/cutover gate. |
| Inherited GUI/checkpoint failures | All 19 remain unchanged. The WU3 owner permits structural acceptance with sufficient independent positive continuity evidence; this does not waive the failing crash-recovery or GUI paths. Separate remediation remains required before their dependent runtime acceptance gates. |
| Public model bytes | Deferred, non-blocking for WU1. Explicit owner authorization required before newly committing/publishing payload; use verified external local input in the meantime. |
| Mac signing/notarization | Deferred to public distribution policy; no blocker to foundation/scaffold or local unsigned QA. |
| Intel Mac support | Deferred target decision; do not claim support from arm64 evidence. |
| Historical Mac binary and uncommitted Mac-only work | Uninspected; no fresh provenance verification or destructive cleanup authorized. Does not block scaffold; required to make claims about those actual artifacts. |
| Missing unpacked Windows v6 directory | ZIP exists and is hashed/inspected. Preserve discrepancy; not a foundation blocker and not permission to recreate it. |
| First unified release naming | Deferred until packaging/release preparation; no release identity invented in WU0. |
| Historical repository publication/retirement | Separate owner decision; retain access/history/evidence now. No push authorized. |
| UE merge | Pre-existing, unchanged; never a source for new commits or packaging. Use immutable references. No need to resolve it for WU1. |
| Missing historical BRN research payloads | Prior owner deletion documented; no reconstruction or fresh verification claimed. Check inputs before any dependent research. |
| SR-021 closeout/mirrors | Separate research handback pending; no adoption/resumption. Accepted SR-024 baseline remains established. |

WU2 resolves runtime inference ownership; WU3 establishes training ownership
within the approved five-owner architecture. Full research-seam isolation remains a WU6 responsibility;
the extraction preserves disabled behavior without claiming that later gate.
The WU0 ignored Gradle mtime observations qualify that earlier preservation claim;
WU1's before/after inventories match. Neither authorizes interference with
unrelated IDE/Gradle processes.

## WU4 evidence and validation

- App-gui solely owns all 75 GUI Java sources, 35 runtime resources and desktop
  launcher. Package names/bytes are retained. ApplicationVersion and its generated
  resource have one shared owner in core. Final source counts: core 109, training
  134, GUI 75, legacy app 9. Core and training implementations remain single-owner.
- App temporarily consumes app-gui in addition to core/training; targets never
  depend on app. Main GUI delegation and UCI remain unchanged. FlatLaf/JNA runtime
  ownership moves to GUI without upgrades; existing package tasks retain launch
  configuration and read/stamp the core identity resource.
- Fresh core/training/GUI tests pass 61/11/22. Final retained-app GUI replay is
  322 cases: 284 pass, ten inherited failures, 28 skipped. Reconciled engine/search/
  UCI/version is 987: 976 pass, 11 skips. Checkpoint/service selection is 138:
  129 pass and nine inherited failures. Reconciled selected JUnit coverage is
  1,541 cases: 1,483 pass, 19 inherited failures, 39 skips. Full suite pass is not claimed.
- The additional Arena timeout does not recur: unchanged WU3/WU4 paired replays
  pass twice each in 9.26-9.38 seconds; full GUI replay passes it in 9.509 seconds.
  Original timeout evidence remains; exact transient cause is unknown. No timeout
  relaxation or production fix was needed. A new archive-boundary assertion was
  corrected to distinguish empty directories from payload; its two cases pass.
- Seven-family persisted continuation, pinned checkpoint identity/reconstruction,
  203 exact SR-024 vectors and 66 UCI requests pass. All 374 original production
  inputs and 722 class payloads match WU3, with no duplicates. SR-021 remains deferred.
- Eleven expanded boundary fixtures, actual dependency verification, fresh assembly,
  configuration-cache reuse, packaging dry-runs and headless entry-point/classpath
  probes pass. No resumed desktop interaction or native production package was made.
- Both historical repositories' tracked/protected bytes, Git state/metadata and
  full size/mtime inventories match before/after, including across the resume
  boundary. Version remains 0.0.0 build 35; local no-bump finalization and exact
  receipts are in WU4 evidence. No journal existed or was created; no push/deploy.
- Native Windows startup from the interrupted earlier root remains historical
  evidence only. Move input/engine response, Training/Arena native workflows,
  preferences/pickers, scaling and cooperative window shutdown require later user
  validation against identified WU7 candidates. The 19 failing paths remain open
  at their own dependent runtime gates and were not suppressed.
- WU6 must review inherited research-tool source/provenance paths in
  `tools/analyze-search-delta.py` and `tools/brn-architecture.py`; no research
  execution, ownership migration or historical-result rewrite occurs in WU4.

## WU5 evidence and validation

- app-uci owns the single UCI protocol stack and headless UciMain; only engine-core
  is on its runtime classpath. Legacy Main injects its existing UciEvaluation profile;
  HCE/book defaults, checkpoint/research properties and GUI dispatch remain intact.
- Standalone selects only the verified pinned generation-32 Pair-2 runtime payload,
  canonical SR-024 construction, Threads/Hash/Clear Hash, legal fallback and infinite
  publication ownership. No second engine, evaluator registry or model is packaged.
- Selected JUnit: 1,472 cases, 1,414 pass, 19 exact inherited failures, 39 skips.
  Thirteen standalone tests pass in a fresh build. Twelve boundary fixtures pass.
  No WU5-introduced correctness regression remains; the full suite is not green.
- Headless process checks: 159 standalone searches, 66 legacy production requests,
  ten exact legacy comparisons, all worker counts 1..16, relocated Java launcher,
  bounded quit/EOF, OOM retention and explicit invalid-model failures. Direct Java
  tests restrict available modules to java.base. No actual Arena/BotLi session occurs.
- All 203 SR-024 vectors match 17 fields; seven-family disk continuation and historical
  checkpoint reconstruction match WU4. GUI/training class payloads are byte-identical.
- Independent compilation, effective runtime classpath, distribution assembly,
  configuration-cache reuse and native packaging dry-runs pass. Historical tracked/
  protected bytes, Git/merge metadata and full size/mtime inventories are unchanged.
  Version remains 0.0.0 build 35; no-bump finalization completed without warnings.
  Model bytes and native releases are not published.
- WU6 must separate remaining legacy UciEvaluation research configuration, core
  disabled research seams, verification/oracle/test ownership and inherited tool paths.
  app retains Main compatibility and checkpoint/research composition until WU8.

## WU6 evidence and validation

- Research owns 212 main Java sources, 145 test sources and the frozen OPT5 corpus.
  Root owns 131 mixed integration sources and three unchanged binpack fixtures;
  a small root research-UCI adapter is the approved composition exception.
- Research depends only on core/training. The other module dependency directions
  are unchanged; root has no production source set or runtime JAR. Fifty-five
  research tasks retain their arguments, with app task aliases and documented paths.
- Production legacy UCI uses accepted-only configuration and rejects ambient research
  selectors. GUI and standalone UCI remain independent of research. SR-021 remains
  deferred; core experimental classes/factories persist unchanged, with no claim of
  complete binary exclusion. Research can deliberately reproduce supported controls.
- Reconciled JUnit: 2,146 cases: 2,085 pass, 19 exact inherited failures and 42 existing skips; remaining failures/messages and failing source
  bytes match WU5. The initial broad run had one additional 90-second TrainingSession
  timeout; two unchanged serialized WU5/WU6 replay pairs pass. Original failure and
  paired-contention failures remain retained; the exact transient cause is unknown. Fifteen architecture fixtures and three path/launch tests pass.
- 203 exact SR-024 vectors, 66 legacy and 159 standalone UCI requests, seven-family
  disk continuation and pinned model reconstruction pass. Four exact historical
  research comparisons, ten research requests, 22 selector-rejection subprocesses,
  frozen corpus execution and compatibility task parity pass.
- All production class payloads in core/training/GUI/UCI match WU5. Classpaths and
  ZIP/TAR contents exclude research. Assembly, cache reuse and packaging dry-runs
  pass. Historical repositories, model bytes and research canon/evidence are unchanged.
- Version remains 0.0.0 build 35 with owner-authorized no-bump finalization; no journal,
  native release or push. WU6 completes the research responsibilities retained at WU5;
  app still owns legacy launch/checkpoint composition and compatibility/build tasks.

## WU7 implementation and available validation

The owner's WU7 launch accepts WU6 and admits unified native packaging with no
version increment. [WU7 packaging](WU7-PACKAGING.md) describes root/module native
tasks, legacy aliases, private model inputs, candidate conventions and integrity
checks. Windows GUI/UCI candidates package successfully; actual native UCI and
headless GUI/JNI relocation checks pass, including external disposable paths with
spaces and empty PATH. [Exact evidence](wu7-evidence.json) records the final
`wu7-windows-final` GUI/UCI candidates, source inputs, artifact hashes and commands.
Both default packages contain core SHA-256
`adaf356401bc31b61b8b3a28e77a65aed6fcd17b85488afa40a8bf4e3696b886`
and the approved generation-32 model. A fresh core rebuild reproduces the hash.

Selected regression results: 1,829 JUnit cases, 1,771 pass, 19 exact inherited
failures and 39 existing skips; 203 frozen vectors match all 17 fields; seven
optimizer families continue exactly; 66 legacy requests, 159 current standalone
searches and 128 historical-protocol searches through the relocated native UCI
executable pass. Nine packaging safety/configuration tests, fifteen architecture
fixtures and fifteen disposable finalizer tests pass. Independent optional-Syzygy
GUI/UCI packaging passes; its core differs only by the native library and license.
All historical tracked/protected hashes, Git/merge metadata and working-file
size/mtime inventories remain unchanged. Failed attempts stay unverified; occupied
destinations are protected. No new packaging regression remains.

Native macOS arm64 tasks are implemented; Windows cannot certify their execution.
Both platforms' interactive GUI and external-client gates remain unperformed.
The 19 inherited failures remain open and unsuppressed; the earlier transient
concurrent timeout is not claimed fixed. No model bytes or official release are
published. Version remains 0.0.0 build 35; same-token no-bump finalization
completed without warnings or blockers and version bytes were independently
verified unchanged. The root journal is absent and was not created. No WU8 work
is admitted.

## Next admission and human actions

Recommend GPT/user reconciliation of WU7 implementation and evidence, then Mac-native
packaging validation and user GUI/client acceptance against exact candidates.
Separate inherited-failure remediation remains necessary at its existing gates.

Human actions required after this prompt:

- **Immediate implementation blocker: None.** Available WU7 checks are complete.
- **Blocking dependent migration:** GPT/user reconciliation before further work;
  WU8 cutover requires native Mac, GUI/client and inherited-failure gates passed or
  explicitly reconciled by the authorized owner.
- **Deferred; blocking public release:** authorize and validate model restoration/
  publication, select the first unified release identity, and decide Mac signing/
  notarization policy. No current local candidate implies those decisions.
