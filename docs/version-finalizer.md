# SeedV6 version finalizer

The owner authorized non-critical build bookkeeping on 2026-10-04. The local
entrypoint in `tools/seed_version_finalizer.py` is the repository-specific
exception to shared r3's persistent failed reservations. `AGENTS.md` routes future
Codex work through it. The shared source and governance registry are unchanged.
The adapter retains shared parsing, full-tree attribution, build-only atomic
replacement, OS locking and successful retry receipts. VERSION_STATE.txt remains
the sole JSON version authority; Gradle only reads it.

The original recurring failure was operation
`9e5cb01570284814a3a7f45a498ae785`: post-write verification observed changes to
ignored Gradle cache files and `app/build/resources/test`, in addition to the
version file. Shared finish recorded `blocked/uncertain` and retained `active.json`.
Both reservation and unfinished-operation scans then rejected every new begin.
Shared r3 recovery accepts only `not_attempted`, so it cannot resolve this case.
Build 33 was already committed when remediation began; it is not retrospectively
claimed as a verified bump by that failed operation.

That stale operation has been reconciled: its baseline and failed state were
archived byte-for-byte under `.git/codex-versioning/warnings/` with a terminal
warning receipt, and its active reservation was released. Version bytes remained
unchanged. The 97 earlier completed operations and unrelated Git metadata were
preserved. No root journal existed, so none was created.

The local finish isolates version inspection, replacement and verification.
Version failures produce `status: warning`, `verified_bump: false`, `after: null`,
diagnostics and exit 0. Its finally path writes a terminal warning receipt,
atomically moves the original operation into `codex-versioning/warnings/TOKEN`,
then releases only the matching reservation. Original baseline/state bytes remain
available for inspection; ordinary start/status no longer report the old failure.
A retry returns the historical warning without another version write. An
interrupted version attempt is likewise closed as unverified rather than bumped
again. No previous worktree bytes, index entries, refs or caches are reverted.

Receipt preparation, archive rename and reservation release are replayable at
each interruption boundary. The OS lock serializes cooperating commands. State
corruption, foreign reservations, helper drift, repository-metadata changes and
failure to store/release finalizer metadata are still errors (exit 2). Disk or
permission failures that prevent cleanup cannot honestly be reported as successful
cleanup; re-entry completes cleanup after storage becomes usable.

The explicit `reconcile-legacy TOKEN` command only accepts an intact owned shared
post-write boundary failure whose non-version paths are ignored and untracked.
It preserves the original evidence, releases the reservation, and never writes
VERSION_STATE.txt or infers mutation ownership from its current number. Other
legacy failure classes require inspection rather than automatic removal.

Historical research reports describe the blocker as it existed at their completion;
their version warnings do not impose a new pending action after this reconciliation.
No release, acceptance or provenance certification is implied by retiring optional
bookkeeping. The normal build number scheme and semantic bump decision are unchanged.

Validation: `python -B tools/test_seed_version_finalizer.py` passes 15 disposable-
repository tests, including the original Gradle-cache failure before/after, normal
bumps, malformed version content, partial writes, interrupted begin/update/cleanup,
repeat calls, foreign reservations, corruption and unexpected non-version effects.
The real repository's old reservation was reconciled and its preserved hashes
checked. A real full-tree begin was attempted but its capture stalled over the
20.6 GB working tree; only that validation process was stopped, before it created
an operation/reservation. This is not a successful full-tree capture claim. No
application tests were needed: application and Gradle behavior did not change.
