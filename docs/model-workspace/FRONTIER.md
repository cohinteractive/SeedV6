# Model workspace frontier

Governing requirements: [CONTRACT](CONTRACT.md). Verified starting map and challenged
assumptions: [DISCOVERY](DISCOVERY.md). Workstream status: ACTIVE, not accepted.
Initial baseline: clean Git worktree, build 34, 2026-10-05.

## Execution queue

| ID | State | Coherent unit and acceptance evidence |
| --- | --- | --- |
| F01 | DONE | Bootstrap/dependency/compatibility map and contract challenge; source inspection recorded in DISCOVERY. |
| F02 | DONE | Shared ModelLibrary over existing lineage/adoption mechanisms; bounded advisory metadata catalog; explicit snapshot resolution including no-Best campaign stores. TrainingLineages discovery delegates to it. |
| F03 | PARTIAL | Separate checksummed generation notes/tags, stale-edit protection, optional history/optimizer metadata projection implemented/tested; UI editing and cumulative known/unknown exposure still pending. |
| F04 | NEXT | Common model selector; Play opponent and independent sides, concrete identities and Swap Sides; preserve legacy preferences and pinning. |
| F05 | READY after F02 | Training selection integration; provenance/default recipe/config history ownership, explicit configurable rates for supported architectures and actual checkpoint provenance; resume compatibility. |
| F06 | READY | Portable semantic threads and explicit termination domain/UI; soft generation-boundary time budget, immediate stop separate; focused run/resume tests. |
| F07 | READY | Reusable Training Data library over existing descriptors/cache, independent mixes/cursors; select in Training/Arena; explicit Lichess/BT4 capability/readiness. |
| F08 | READY after F05-F07 | Network Training UX organized around lineage, recipe, exposure, validation/match and termination; native layout verification. |
| F09 | READY after F02,F05,F07 | Arena shared protocol/competitor recipe persistence; preserve old config binding, frozen tranches/targets, resumes/history; integrate lineage identities. |
| F10 | READY after F04 | Shared participant/match presentation semantics where justified; Arena live board using existing feed/BoardPanel, exact side identities. |
| F11 | READY after F05 | Shared rich training progress presentation in Arena and Training; retain optimizer/loss/exposure metrics and EDT ownership. |
| F12 | READY | Arena Setup/Live/History, concrete history identities, winners and W/D/L; symmetric stacked scores/50% chart with unscored cases. |
| F13 | READY after integration | Compatibility cleanup, remove superseded path-first/duplicate flows, guides; broader risk-based regression and native acceptance evidence. |

## Current execution and resume

Bootstrap and F02/F03 foundation implemented and focused checks passed. Next:
F04, shared selection UI and concrete Play identities/Swap Sides. Reuse ModelLibrary;
preserve actual loading verification, GUI worker lifecycle and missing-selection errors.
No broad acceptance or native GUI checks have run yet.
Do not treat the contract creation as completion. Continue through admissible units.

Version initialization: pinned helper hashes verified; local begin invoked before
mutation at 02:41 UTC. Capture was stopped after over seven minutes, before it
created an operation/token/reservation. Local status then reported idle, active
null, unfinished [], blockers []; all 97 historical operations completed. Under
the explicit SeedV6 non-critical invocation override this is a non-blocking version
warning, not a successful begin or verified bump. No version bytes changed, no
replacement bump attempted, no shared helper edited. Root journal absent.
No blockers or required human actions identified. No push/deployment authorized.

## Evidence log

- Bootstrap: inspected architecture/storage/GUI/training/validation/data/Arena and
  relevant tests; challenged and revised intended design around existing managed
  placement, missing Best in Arena, shared preparation versus separate cursors,
  and old config-hash compatibility. No runtime verification claimed.
- Foundation: `:app:test -Pheadless --tests '*ModelLibraryTest' --tests
  '*TrainingLineagesTest' --tests '*TrainingFoldersTest' --tests
  '*AvailableCheckpointsTest'` passed after correcting a test-only Java compound
  `var` declaration. Compiled main/verification/test and installed distribution.
  Synthetic-store tests cover read-only legacy browsing, duplicate names/distinct
  UUIDs, no-Best explicit load, metadata versus payload integrity, annotation round
  trip and immutable-file hashes, stale/corrupt edits and changed lineage identity.
  Full suite, real user stores, strength/training experiments and native render
  checks deliberately deferred to affected/integration units. `git diff --check`
  passed. No external user data or preferences used.
