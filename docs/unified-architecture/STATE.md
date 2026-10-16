# Unified architecture migration state

Updated 16 October 2026 (Pacific/Auckland). Governing [CONTRACT](CONTRACT.md),
[BASELINE / preservation](BASELINE.md), [ACCEPTANCE matrix](ACCEPTANCE.md),
[foundation evidence](foundation-evidence.json).

## Current boundary

**WU0 is accepted by GPT, as confirmed in the owner's WU1 launch. WU1 implementation
and scoped validation are complete, with all 19 inherited failures explicitly
deferred by the owner for the WU1 commit. WU1 awaits GPT/user acceptance;
no WU2 implementation is admitted.**
The WU1 baseline is the clean accepted commit
`81e868a72b942202e1ced89fe30a5a13e3af155f`. See the
[WU1 build transition](WU1-GRADLE.md) and [WU1 evidence](wu1-evidence.json).

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
| WU1 Gradle foundation | COMPLETE; GPT/USER ACCEPTANCE PENDING | Five target descriptors plus authorized temporary `:app`, shared conventions and enforced graph/source ownership. Build-boundary and selected regression checks pass; all 19 WU0 failures are owner-deferred for this commit. [WU1 record](WU1-GRADLE.md); no runtime migration or WU2 admission. |
| WU2 Core | NOT STARTED | WU1 gate; reconcile accepted source versus main research seams, E1-E6/R1/R3. |
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

## Outstanding decisions and limitations

| Item | Effect on next work |
| --- | --- |
| WU1 GPT/user acceptance | Blocking WU2 admission. Implementation and scoped validation are complete; the owner authorized the WU1 commit with all 19 inherited failures deferred. WU0 remains accepted. |
| Inherited GUI/checkpoint failures | Non-blocking for WU1 under the explicit owner decision; separate remediation remains outstanding. Reconcile them before claiming the dependent GUI/training runtime acceptance gates. |
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

No unresolved architectural choice currently prevents the Gradle scaffold.
The exact training/inference package split and research seam mechanics are future
evidence-based implementation tasks, not a request to change the five owners.
The WU0 ignored Gradle mtime observations qualify that earlier preservation claim;
WU1's before/after inventories match. Neither authorizes interference with
unrelated IDE/Gradle processes.

## Next independently reviewable unit

After explicit GPT/user acceptance of WU1, admit
**WU2 - Shared production core**. Re-read governance and migration evidence,
inspect intervening changes and version state, and retain isolated build outputs.
Account explicitly for accepted SR-024 versus main's SR-021 research seams before
moving any implementation. Transfer board/rules/search/evaluation/inference/runtime
ownership once, preserving independent references and test/verification coverage.
Validate E1-E6/R1-R3; do not treat empty target compilation as runtime separation.
The local-only evidence and external inputs must be checked before dependent QA.
GUI/training source and packaging task migrations remain their later units.

Human actions required after this prompt:

- **Blocking for WU2:** GPT/user must accept WU1 before admitting source extraction.
  The WU1 commit is already authorized with the 19 inherited failures deferred.
  No WU2 work begins in this turn.
- **Non-blocking for WU1:** authorize separate remediation of the documented
  inherited GUI/checkpoint failures before their dependent runtime acceptance.
- **Non-blocking now:** resolve the deferred owner decisions at the specific
  publication/platform/release dependency above; no immediate action is needed.
