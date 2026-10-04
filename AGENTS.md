# SeedV6 version finalization

Owner instruction, 2026-10-04: "For now, version/build bookkeeping is
NON-CRITICAL" and a failed update must clean up and cease obstructing subsequent
work. This is an explicit SeedV6-only override of the fail-closed version failure
and invocation portions of CODEX-VERSION-FINALIZATION-SEMANTICS r3. It does not
change shared policy for other repositories, Git policy, task coordination,
application version format, or unrelated integrity/ownership checks.

For this repository use the local entrypoint below instead of invoking the shared
finalizer directly. It reuses the pinned shared implementation; do not edit that
implementation or its global binding for a SeedV6-only change.

```text
python -B tools/seed_version_finalizer.py --repo . begin
python -B tools/seed_version_finalizer.py --repo . finish TOKEN --decision bump
python -B tools/seed_version_finalizer.py --repo . finish TOKEN --decision no_bump
python -B tools/seed_version_finalizer.py --repo . status [TOKEN]
```

Only an `active` begin requires finish; retain its token and select bump/no_bump
from the work's actual application impact as before. A `warning` begin is terminal
even when it includes a token; do not attempt a replacement bump. A `warning`
result is terminal, exits zero, and must be reported
as a non-blocking version warning. Never claim a bump unless `verified_bump` is
true. Never manually compensate for a partial write. Corrupt state, changed tool
binding, conflicting ownership and metadata failures remain blocking; report the
actual problem rather than treating them as optional version failures.

Failed attempt history is retained under Git metadata `codex-versioning/warnings/`
with a terminal receipt; it is not pending work. The local entrypoint completes
interrupted warning cleanup before starting a new operation and never releases a
different token. The shared OS command lock is released on process exit; its
inactive `command.lock` file must not be deleted as a substitute for unlocking.
Root journal presence handling remains unchanged; do not create a journal.

Targeted validation: `python -B tools/test_seed_version_finalizer.py` (disposable
Git repositories only). See [finalizer details](docs/version-finalizer.md).
