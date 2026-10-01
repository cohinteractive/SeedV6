# Historical search instrumentation

The scripts in `tools/` instrumented earlier revisions of `ExactSearch` for
SR-003, SR-017 and SR-001H/I. Their exact source substitutions no longer match
the current engine (for example, staged generation call counts changed). They
are retained for inspecting or replaying the original research revision, not
as supported commands against the current baseline.

Use the original repository revision and the scripts' original root `tools/`
placement when reconstructing that research. Reports and raw committed evidence
are under [docs/research/search](../../docs/research/search/).

This directory is outside every Gradle Java/resource source set and is not
compiled or packaged. Active reference implementations and diagnostics instead
live in `app/src/verification`; JUnit oracles remain in `app/src/test`.
