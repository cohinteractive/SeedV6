# Unified architecture migration state

Updated 16 October 2026 (Pacific/Auckland). Governing [CONTRACT](CONTRACT.md),
[BASELINE / preservation](BASELINE.md), [ACCEPTANCE matrix](ACCEPTANCE.md),
[foundation evidence](foundation-evidence.json).

## Current boundary

**WU0 and WU1 are accepted by GPT, as confirmed in the owner's WU2 launch.
WU2 implementation and scoped validation are complete; GPT/user reconciliation
is pending. No WU3 work is admitted.**
The WU2 starting point is the clean accepted WU1 commit
`bd864a9f1231e6c73997b9302a411449bc2a5a21`. See
[WU2 core extraction](WU2-CORE.md) and [WU2 evidence](wu2-evidence.json).
The earlier [WU1 record](WU1-GRADLE.md) and [evidence](wu1-evidence.json) remain
historical accounts of that unit's handback.

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
| WU2 Core | COMPLETE; GPT/USER ACCEPTANCE PENDING | One runtime owner, inference/training split and accepted-source bridge; [WU2 record](WU2-CORE.md). |
| WU3 Training | NOT STARTED | Shared core interfaces; exact checkpoint/optimizer/workspace compatibility, T1-T4. |
| WU4 GUI | NOT STARTED | Core/training validated; G2-G5 automated/native evidence. |
| WU5 UCI | NOT STARTED | Core validated; adapt immutable UE boundary behavior, U1-U4/E5/E6. |
| WU6 Research | NOT STARTED | Ownership settled with references protected throughout; R1-R3 and task/path mapping. |
| WU7 Packaging | NOT STARTED | Core/apps/training/research gates; P1-P5 plus native/client acceptance. |
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

## Outstanding decisions and limitations

| Item | Effect on next work |
| --- | --- |
| WU2 GPT/user acceptance | Blocking WU3 admission after WU2 implementation/validation handback. WU0 and WU1 are accepted. |
| Inherited GUI/checkpoint failures | All 19 remain owner-deferred for WU2 only while demonstrably inherited and unchanged. Separate remediation remains required before their dependent GUI/training runtime acceptance gates. |
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

WU2 resolves the runtime inference/training dependency split within the approved
five-owner architecture. Full research-seam isolation remains a WU6 responsibility;
the extraction preserves disabled behavior without claiming that later gate.
The WU0 ignored Gradle mtime observations qualify that earlier preservation claim;
WU1's before/after inventories match. Neither authorizes interference with
unrelated IDE/Gradle processes.

## Next independently reviewable unit

After GPT/user reconciliation of WU2, admit **WU3 - Training ownership**.
Move trainers, optimizer codecs, checkpoint/corpus/self-play/validation services
into engine-training while preserving exact continuation, identities and workspace
contracts. Retain the 19 inherited GUI/checkpoint failures as explicit separate
remediation obligations; their deferral does not satisfy the later runtime gates.
WU3 is not started by this turn. WU6 owns the remaining disabled research seams;
model publication and final packaging remain separately gated.

Human actions required after this prompt:

- **Blocking for WU3:** GPT/user must reconcile and accept WU2 before the next unit.
- **Non-blocking for WU2:** authorize separate remediation of the 19 inherited
  failures before their dependent GUI/training runtime acceptance gates.
- **Non-blocking now:** deferred public-model/platform/release decisions remain
  due only at their stated downstream boundaries.
