# TT memory workstream — resume here

Status: **ACTIVE**, 30 October 2026. Governing objective [TT-R001](CONTRACT.md),
under Search P4-R001. Bootstrap established; bridge diagnostics complete, bridge
matches running. H1 is an uncommitted experimental implementation under technical
qualification; no strength conclusion. Champion remains P4-BASELINE-001/build79.

## Bootstrap and challenge

Verified clean `main` at `9fec1d2af379827d105d523c32162fd771905cb9`;
live masters P4-R001/P4-F003 exactly match supplied identities. Both baseline79
and correction80 are ancestors; post80 changes are Frontier documentation only.
Correction changes ParallelSearch reentry by seven lines plus tests/version.
CP64 file SHA-256 matches TT-R001. VERSION_STATE is build80. Root journal absent.
Repository AGENTS.md explicitly supersedes obsolete shared finalizer requirements.
Architecture ownership and isolated local Gradle-home convention remain applicable.
Accepted inventory/map/correction are starting evidence, not repeated projects.

The framing survives source review, with four important cautions:

1. Removing global extension suppression alone leaves selective-subtree suppression;
   a useful candidate must address production, retention and consumption together.
2. Existing ordinary saves can displace hints, while hint saves cannot replace an
   ordinary record. Score admission alone can therefore damage move guidance.
3. Corrected admission can affect W4 stop boundaries, root work/order and strength.
   No behavioral or competitive equivalence is assumed; nodes are not a fair bridge
   resource in affected multiworker runs.
4. The existing Phase 3 selectivity UCI research adapter constructs Search directly
   without applying BaselineEngine's current clock wrapper. Reusing its identity
   would not certify the requested current managed-clock control. New experiments
   must bind and report the actual clock path.

Host observed: Windows 11 Pro 10.0.26300, Ryzen 5 5500, 6 physical/12 logical cores,
about 31.8 GiB visible RAM, Java at C:/java/jdk-21/bin/java.exe, Python 3.13.
Runtime versions and executable hashes belong in per-experiment manifests.

## Selected work and decision queue

1. **Selected: bridge/qualification.** Separate frozen build79/build80
   runtimes; exercise W1 deterministic depth/nodes and W4 behavior/admission,
   request limits, legal PVs, cancellation/reuse. Use repeated W4 observations as
   scheduling samples, not deterministic equality claims. Run bounded paired-clock
   bridge screening after protocol/consumer qualification. This is exploratory,
   not proof of equivalence or correction strength.
2. **Next: bounded integrated heuristic memory.** Explicit research selection,
   separate selective provenance, ordinary-score-only heuristic reuse initially,
   conservative non-PV eligibility, useful retention without losing completed legal
   hints; retain exact controls and production defaults. Final design follows
   relevant kernel inspection and initial diagnostics.
3. **Conditional:** strength screen versus80, direct versus79 as bridge warrants;
   reverse ablations only for interpretation, independent confirmation only after
   credible screening and feasible precision/cost assessment.

## Evidence and implementation in progress

- Exact detached source79 is available at `build/p4-tt/control79`; its local Gradle
  home and build outputs are isolated. Frozen ordinary UCI runtimes are at
  `raw/runtime79` and `raw/runtime80`, with complete JAR/Java/model hashes in the
  corresponding tracked manifests. Version resources report 79 / 80 respectively.
- [Bridge protocol](bridge-v1-protocol.json) froze diagnostics and 32 paired games
  per worker domain at 3+0.03 before observations. Fresh legal opening histories
  and selection provenance are in `bridge-v1-openings.pgn` / `.selection.json`.
- `raw/bridge-diagnostics-v1` preserves the first attempt: W1 observations completed,
  then W4 preflight failed because ordinary production UCI suppresses identity
  announcements after resource changes. The harness now requests a fresh UCI
  identity. This was a harness failure before W4 search, not an engine defect.
- [Successful retry](bridge-diagnostics-v1-retry1-summary.json): 136 requests,
  legality/PV/accounting checks passed. All nine W1 depth5 positions repeated
  identically and matched across79/80. W4 is scheduling-dependent; one stable
  position charged 14,580 nodes on79 vs14,637 on80. This rules out equating reported
  budgets; it does not establish the correction's causal effect on every W4 move.
  Request-limit, terminal and stop/reuse checks passed. Diagnostic elapsed times
  are not clean performance measurements (some development builds overlapped).
- Bridge matches `raw/bridge-w1-v1`, then `raw/bridge-w4-v1` run sequentially with
  no overlapping CPU-heavy research/build. Do not duplicate while active. See
  each `run.json`; process ownership is the established Windows Job mechanism.
- H1 implementation: opt-in flag HEURISTIC_TT_MEMORY (flags7602175); separate
  heuristic marker; ordinary-score-only current-generation sufficient-depth
  interior null-window reuse, excluded from synthetic null and legal verifier.
  Same 24-byte entry payload/locks/keys. Score and completed-move depths coexist;
  deeper hints survive shallow score writes, completed hints can refresh without
  erasing a useful score, and newer hints cannot renew an old score's eligibility.
  Same-key score refresh permits two plies of depth loss; this is an exploratory
  setting, not an externally validated Seed tuning constant. Production remains
  flags3407871. Detailed mechanism diagnostics and strength screening are next.
- H1 test v1 caught an attempted raw-eval probe change that confused signed raw
  values with marker bits. Original raw-eval behavior was restored; separate raw
  evaluation tables retain caller-managed validity. Failed output is preserved in
  `raw/h1-tests-v1`. [Rerun](h1-tests-v2-summary.json): 297 tests passed, zero
  failures/errors/skips (191 core,26 UCI,80 selected research), including table
  collision coherence, independent exact references, corrected verified-null,
  rebuilt/incremental evaluation, legal PVs and W1-16 bounded admission/draining.
  Native GUI/macOS and broad strength qualification have not run.
- Initial shell Python lacked python-chess; no experiment started in those failed
  setup attempts. Use existing `build/research/venv/Scripts/python.exe` (chess
  1.11.2), preferably with `-B`. No global package installation was performed.

No historical BOUNDARY2 scores were opened. Raw evidence is local-only under
`raw/`; summaries/manifests stay outside it. Build remains80 until an attributable
qualifying implementation commit; no default, champion or release changed.

## Resume and human actions

Read this file, TT-R001 and current Phase 4 masters; inspect Git and available local
evidence before continuing the selected item. Do not restart closed preparatory
work or duplicate runs. Preserve exact runtime/protocol identities and failures.
Owner mirror refresh is non-blocking for repository research, blocking only
planning that relies on the Phase 4 Project mirror. No production/champion action
is authorized. No other owner action currently blocks execution.
