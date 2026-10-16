# Unified architecture migration state

Updated 16 October 2026 (Pacific/Auckland). Governing [CONTRACT](CONTRACT.md),
[BASELINE / preservation](BASELINE.md), [ACCEPTANCE matrix](ACCEPTANCE.md),
[foundation evidence](foundation-evidence.json).

## Current boundary

**WU0 foundation implemented; validation/completion evidence is recorded below.
GPT/user reconciliation is pending. WU1 is not started or admitted by this turn.**
The user's steering to continue during WU0 continues this same bounded unit;
it is not a second journal unit or authority to start code migration.

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
| WU0 Foundation | IMPLEMENTED; awaiting GPT/user reconciliation | Isolated branch/worktree and five workstream records plus README navigation; G0 validation below. No engine/application/Gradle restructuring. |
| WU1 Gradle foundation | NOT STARTED | After WU0 reconciliation: project/dependency scaffold and shared conventions, retain current working app route; G1. |
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

## Outstanding decisions and limitations

| Item | Effect on next work |
| --- | --- |
| WU0 GPT/user reconciliation | Blocking admission gate for WU1; stop this invocation here. Completion/commit is not acceptance. |
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
The isolated ignored Gradle mtime observations do not affect source selection,
historical artifacts or this documentation-only work; they qualify the preservation
claim rather than require interference with unrelated IDE/Gradle processes.

## Next independently reviewable unit

After WU0 reconciliation, admit **WU1 — Gradle multi-project foundation**. Re-read
AGENTS.md and these records, inspect all worktrees and any intervening changes,
verify the starting branch/commit, then initialize version handling for that new
unit. Set a worktree-local Gradle user home under ignored `build/migration/`.

Inspect actual source/build dependencies before editing. Establish five target
project descriptors and shared build conventions with an acyclic dependency map;
keep the current app route temporarily buildable without duplicating source
compilation. Preserve Java 21, wrapper/toolchain, source-set test/verification
boundaries, version resources, native resources/licenses and current task coverage.
Run G1 configuration/build checks in the isolated tree. Record any task relocation
or transitional dependency and its removal gate. Do not silently turn scaffold
work into engine extraction, model publication or source-algorithm changes.

Evidence needed to advance from WU1: attributable diff/commit; configured project
and dependency graph; working existing application build/test entry points;
isolated outputs; no duplicate engine ownership; unchanged production identities
or explicitly justified resource/provenance changes; updated STATE with actual
commands/results/limits and WU2 scope. Public model, signing, Intel, historical Mac
artifact and release-name decisions need not be settled to do that work.

Human actions required after this prompt:

- **Blocking for WU1:** GPT/user reconciliation of WU0 and admission of the next
  bounded implementation unit. No code migration begins in this turn.
- **Non-blocking now:** resolve the deferred owner decisions at the specific
  publication/platform/release dependency above; no immediate action is needed.
