# TT memory workstream — resume here

Status: **ACTIVE**, 30 October 2026. Governing objective [TT-R001](CONTRACT.md),
under Search P4-R001. Bootstrap and [comparison bridge](BRIDGE.md) complete;
V1 match incomplete, V2 W1/W4 passed with wide uncertainty. H1 has passed
technical/information-use and launcher qualification. W1 H1 screen is promising
but inconclusive; W4 screen active. No confirmed TT strength gain. Champion remains P4-BASELINE-001/build79.

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

1. **Completed: bridge/qualification.** Separate frozen build79/build80
   runtimes; exercise W1 deterministic depth/nodes and W4 behavior/admission,
   request limits, legal PVs, cancellation/reuse. Use repeated W4 observations as
   scheduling samples, not deterministic equality claims. Both32-pair clock
   bridges completed. W4 build80 score42.19% (normal95 33.83–50.55%) is adverse
   and uncertain; no equivalence assumed. Promising candidates need direct79.
2. **Implemented: bounded integrated heuristic memory H1/build81.** Explicit research selection,
   separate selective provenance, ordinary-score-only heuristic reuse initially,
   conservative non-PV eligibility, useful retention without losing completed legal
   hints; exact controls and production defaults retained. Technical and
   information-use qualification passed; strength remains unmeasured.
3. **Selected:** H1 strength screen versus80; direct versus79 for any promising result;
   reverse ablations only for interpretation, independent confirmation only after
   credible screening and feasible precision/cost assessment.

Conditional mechanism follow-up, not a selected revision: H1's initial diagnostic
starts each root with a fresh table and does not characterize information lifetime
across real consecutive game requests. A new-generation completed hint converts
an old H1 score record to hint-only; merely removing cutoff generation checks
would therefore not establish useful cross-request retention. If strength results
justify another integrated candidate, first measure this availability on real
history sequences, then consider bounded age with independent score/hint retention.
Do not assume that longer-lived scores improve strength or relax history/coherence.

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
- Bridge V1 `raw/bridge-w1-v1` stopped in game52 (51 complete) on strict missing-score
  warning; W4 did not start. Partial scores are not a complete strength result.
  [V2](bridge-v2-protocol.json) selects 10+0.1 and full UCI trace with strict gates
  retained. Source/zero-allocation qualification is in [H1](H1.md); the exact
  untraced event cause remains unknown. Check raw/V2 run.json before resuming.
- H1 implementation: opt-in flag HEURISTIC_TT_MEMORY (flags7602175); separate
  heuristic marker; ordinary-score-only current-generation sufficient-depth
  interior null-window reuse, excluded from synthetic null and legal verifier.
  Same 24-byte entry payload/locks/keys. Score and completed-move depths coexist;
  deeper hints survive shallow score writes, completed hints can refresh without
  erasing a useful score, and newer hints cannot renew an old score's eligibility.
  Same-key score refresh permits two plies of depth loss; this is an exploratory
  setting, not an externally validated Seed tuning constant. Production remains
  flags3407871. Mechanism diagnostics passed; strength screening is next.
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
`raw/`; summaries/manifests stay outside it. H1 is P4-TT-H1-v1, committed as experimental build81 with the implementation;
its frozen runtime manifest will bind the exact commit and executable hashes.
No default, champion or release changed. Bootstrap commit: `9d8389e8`.

[H1 technical report](H1.md) and [diagnostic summary](h1-diagnostics-v1-summary.json)
record 18,364 W1 /19,601 W4 heuristic cutoffs across nine100k-node requests,
unchanged capacity and clean/instrumented W1 equality. These are diagnostics only.
Selected now: prospective H1 strength screening versus80
and direct79 comparisons if promising. Keep cheap mechanism reverse ablations
conditional on what the integrated strength evidence needs to explain.

### Current execution detail

H1 source commit is `a72cc496` / build81; `runtimeh1-v1-manifest.json` binds its
frozen executable. W1/W4 artifact identity/node-limit preflight passed in
`h1-build81-preflight-summary.json`. The diagnostic prototype is separately retained
at `raw/runtimeh1-diagnostic-v1`; its manifest preserves the JARs identified by the
older diagnostic manifests even after the ordinary build directory was refreshed.

`raw/bridge-w1-v2/run.json` and its tracked summary describe the completed,
audited 32-pair10+0.1 W1 bridge: build80 scored27W/12D/25L,51.56%; paired normal95
42.0–61.1%, bounded Hoeffding95 27.6–75.6%. All64 normal games and frozen inputs
passed independent audit. This establishes neither difference nor equivalence.
W4 V2 passed all64 normal games at `raw/bridge-w4-v2/run.json`; build80 scored
15W/24D/25L,42.19%, with wide uncertainty. [BRIDGE](BRIDGE.md) records the
comparison-design consequence. A protocol deviation was found during W1 execution: `level=trace`
captures runner lifecycle, but this pinned Fastchess also requires `engine=true`
for UCI communications. That switch was omitted. Preserve the frozen run; its
strict warning gate and independent PGN/request audit still qualify a complete
run, but it cannot supply the promised exact failing-request trace if interrupted.
Do not infer that full communications were captured. The adapter now adds
`engine=true`; `bridge-w4-v2-execution.json` prospectively records the symmetric
logging correction and binds the unchanged V2 protocol. Completed W1 inputs
remain evidenced by their frozen hashes, not the later adapter revision.

Completed between matches: `h1-cost-v1-summary.json` records180 total clean100k-node
requests including warmup,144 measured. H1/control geometric-mean per-root wall
ratios were0.987 W1 and0.960 W4; process CPU ratios0.967/0.926. No material
integrated cost regression appeared in this cheap sample. Different trees and
short noisy timings preclude attributing this to isolated transaction overhead.
Decision: defer combined-access H2 optimization; proceed with the technically
qualified H1 instead of changing it on an unmeasured performance premise.
Speed never answers strength. The additional explicit H1 legal-null-
verifier witness passed (`h1-null-witness-v1-summary.json`).
`h1-screen-v1-protocol.json` prospectively defines coarse W1/W4 strength screening
against80, with independent confirmation/direct79 follow-up required for a
promising result. W1 completed all64 normal games and stable-input audit:
30W/14D/20L,57.81%; paired normal95 48.63–67.00%, bounded Hoeffding95
33.80–81.82%. This is promising but inconclusive exploratory evidence, not a
confirmed gain. See `h1-screen-w1-v1-summary.json` and its raw run.
W4 is now running at `raw/h1-screen-w4-v1/run.json`; inspect its status before
resuming and never duplicate the active match. Do not overlap heavy work or edit
frozen adapter/protocol/runtime inputs. Next: review both completed screens and
freeze proportionate independent H1 confirmation/direct79 comparisons if still
justified. H1 remains fixed; no H2 revision is selected. The positive W1 point
estimate makes testing unchanged H1 more informative than premature tuning.
Prepared prospective design: `h1-confirm-w1-v1-protocol.json`, selected from the
completed W1 screen while W4 remains active. Bind128 independent partition1
opening pairs after W4 drains, then confirm unchanged H1 versus80; direct79 is
conditional on the prospective adjusted-interval gate. Expected cost about2h10m
for the first block, another2h10m only if justified. Claim attempt1 reserves
nominal alpha0.025, split between controls; future adaptive claims must not reset
this expenditure. The protocol is not yet executable/frozen: opening selection,
disjointness verification and hashes remain pending. No candidate tuning is selected.
`launcher-control-v1-summary.json` passed39 requests: all11 W1 depth/node outcomes
match across frozen80,81 ordinary UCI and81 research control. W4 accounting/PV
checks passed. Source/artifact/configuration isolation is qualified in these
bounded domains; strength and timing equivalence are not inferred.

## Rebuild and evidence availability

All frozen runtime manifests identify actual files; verify their existence and
hashes before reuse. A fresh checkout lacks ignored evidence. Do not overwrite a
frozen installation or assume a rebuild inherits its executable identity.

- Build79: detached `753367614b8aa7e32dee337fd76b542737141ac1`,
  `gradlew.bat :app-uci:installDist --console=plain`; original build log remains
  `build/p4-tt/control79/build79-build.log`.
- Build80 control: source `9fec1d2af379827d105d523c32162fd771905cb9`, same Gradle
  task; original log `build/validation/p4-tt-control80-build.log`. Its Search
  source is identical to correction commit0bfb4ea8; source metadata is distinct.
- H1/build81: detached `a72cc496fd39aeddefea4382044bd2bf2e9f6cbb`,
  `gradlew.bat installResearchWorkspace --console=plain`; original log
  `build/validation/p4-tt-build81.log`. Output is `build/research/runtime`, with
  separate launcher classes and runtime JARs; never a production distribution.

Use Java21+35 identified in manifests and a checkout-local
`GRADLE_USER_HOME=<checkout>/build/migration/gradle-user-home`. Freeze copies under
new ignored paths with `tools/p4-tt-memory.py freeze`; generate new manifests and
qualify rebuilt identities before comparisons. Ordinary UCI distributions come
from `app-uci/build/install/app-uci`. Tests and diagnostic commands/evidence are
linked in their compact summaries/manifests; interrupted outputs stay preserved.

## Resume and human actions

Read this file, TT-R001 and current Phase 4 masters; inspect Git and available local
evidence before continuing the selected item. Do not restart closed preparatory
work or duplicate runs. Preserve exact runtime/protocol identities and failures.
A `running` JSON status alone does not prove a process is still live: compare the
run's process-cost PID/creation evidence with actual OS processes and commands.
If interrupted, preserve the partial PGN, logs and manifests, record the outcome,
and resolve the operational cause before a separately named prospective retry.
Never overwrite a frozen run or interpret partial scores as completed evidence.
Owner mirror refresh is non-blocking for repository research, blocking only
planning that relies on the Phase 4 Project mirror. No production/champion action
is authorized. No other owner action currently blocks execution.
