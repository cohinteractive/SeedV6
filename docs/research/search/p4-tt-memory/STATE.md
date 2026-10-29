# TT memory workstream ? resume here

Status: **ACTIVE**, 30 October 2026. Objective [TT-R001](CONTRACT.md) is subordinate
to Search P4-R001. Current programme master pair: **P4-R001 / P4-F005**.
H1's W1/W4 development screens are promising but inconclusive. Independent W1
confirmation is frozen and selected. No confirmed gain, research acceptance,
champion advancement or production promotion has occurred.

## Identities and completed foundations

| Role | Source/build and frozen executable manifest |
| --- | --- |
| Immutable baseline AND accepted champion | `753367614b8aa7e32dee337fd76b542737141ac1` / build79; [runtime79](runtime79-manifest.json). |
| Accounting-corrected technical control | `0bfb4ea8422ef78443e309930166ad46df5cc454` / build80. Artifact built at `9fec1d2af379827d105d523c32162fd771905cb9`, whose runtime source is identical; [runtime80](runtime80-manifest.json). Not a champion. |
| Experimental H1 | `a72cc496fd39aeddefea4382044bd2bf2e9f6cbb` / build81, flags7602175; [runtimeh1-v1](runtimeh1-v1-manifest.json). Explicit research selection only. |

CP64/checkpoint64/S6CNU002/eight heads/BT4_Q_LOGIT400_V1 and its full SHA are pinned
in TT-R001. Production policy stays flags3407871, margins100/200, extensionCap1,
ADAPTIVE16, OWNER W1 / ROOT_REUSE W2?16, existing BOUNDARY2. VERIFIED_NULL still
requires original18. Hash64 gives 2,097,152 direct-mapped slots / 50,331,648 bytes
of entry payload; overhead and process memory are separate. Both actors use the
same vector-256-long backend. All frozen runtime/model hashes were reverified
before freezing confirmation; the host snapshot is bound in its protocol.

Bootstrap verified clean main at 9fec1d2a and the supplied P4-R001/P4-F003 masters.
The accepted inventory, accounting correction and external research map remain
closed starting evidence. The framing survived challenge: global score suppression
alone was insufficient; hints and score retention are coupled; accounting80 cannot
be presumed competitively neutral; the old Phase3 research UCI launcher omitted
the current clock wrapper. Those comparison dependencies are now characterized
or corrected in the explicit research consumer. Do not repeat the inventory/map.

## Evidence and decisions

[H1 technical report](H1.md) and [comparison bridge](BRIDGE.md) hold details,
limitations, raw locations, artifact differences and consumer qualification.
All game rows below are exploratory, 32 opening pairs/color reversal at10+0.1,
Hash64, one active game, on the recorded Windows/Ryzen5500/Java21 host.

| Comparison | W/D/L for the first named side | Score | Paired normal95 |
| --- | ---: | ---: | ---: |
| build80 versus79, W1 |27/12/25|51.56%|41.99?61.14%|
| build80 versus79, W4 |15/24/25|42.19%|33.83?50.55%|
| H1 versus80, W1 |30/14/20|57.81%|48.63?67.00%|
| H1 versus80, W4 |25/23/16|57.03%|47.13?66.93%|

Each completed 64-game run passed strict PV/bestmove checks, independent PGN/
request accounting audit and immutable-input checks. Conservative Hoeffding
intervals are substantially wider; see per-run summaries. No competitive
79/80 equivalence is established. W4's adverse bridge makes transferring an H1
versus80 gain into a champion79 gain especially inappropriate.

Technical evidence: 297 core/UCI/selected research tests passed on the H1
implementation (prototype build80 metadata), followed by the additional reached
legal-null-verifier witness and build81 artifact preflight. Independent exact
controls were unchanged. W1?16 budget/refusal/cancel/drain/reuse and concurrent
collision safeguards passed. The 39-request launcher check matched all 11 W1
control depth/node outcomes across frozen80,81 ordinary UCI and81 research control;
W4 accounting/PVs passed without asserting scheduling equality. The initial
136-request 79/80 bridge found W4 counted-work differences: nodes are not equal-work
currency across that correction.

Nine-root instrumentation showed actual H1 score admissions/reuse and retained
legal guidance; it is not strength evidence. Bound-class occupancy was not
implemented in that first diagnostic, and this limitation remains recorded.
The clean 180-request cost diagnostic showed no material integrated regression
(geometric-mean H1/control wall ratios 0.987 W1,0.960 W4). Defer the proposed
combined-access optimization; test unchanged H1 instead of tuning prematurely.

Retain failures: `raw/bridge-diagnostics-v1` (W4 identity-announcement harness
failure after W1); `raw/bridge-w1-v1` (3+0.03 strict missing-score interruption
in game52 after 51 complete games; no complete strength score); `raw/h1-tests-v1`
(raw-eval compatibility regression, fixed without changing the oracle); the first
H1 observation console (PowerShell argument split before main). Successful retries
have distinct paths. V2 W1 tracing omitted engine=true and captured lifecycle only;
W4 and H1 traces include full UCI communications. Do not erase these limitations.
The zero-work fixture demonstrates truthful legal fallback, not the exact untraced
V1 failure cause. Frozen historical BOUNDARY2 studies remain unopened and unchanged.

## Selected work: independent unchanged-H1 W1 confirmation

[h1-confirm-w1-v1-protocol.json](h1-confirm-w1-v1-protocol.json) is prospectively
frozen before any confirmation games. It binds 128 new partition1 opening pairs
(seed2026103002), with **zero** final-position overlap with the 32 development pairs,
exact runtimes, host, analysis implementation and input hashes. Both H1 screens
have drained and passed operational qualification. The selector output is
`h1-confirm-w1-v1-openings.pgn` and its `.selection.json` manifest.

1. Execute `tools/p4-tt-memory.py match` with name `h1-confirm-w1-v1-control80`,
   baseline `docs/research/search/p4-tt-memory/raw/runtime80`, candidate
   `docs/research/search/p4-tt-memory/raw/runtimeh1-v1`, candidate-policy h1,
   workers1, pairs128, tc10+0.1, timeout14400, the confirmation opening file and
   protocol above. Use `build/research/venv/Scripts/python.exe -B`. Check the raw
   run directory/status/process evidence first; never duplicate a live run.
2. After the complete audited run, execute `tools/p4-tt-confirm-analysis.py control80`.
   The adjusted primary lower bound must exceed 0.5 before the conditional direct79
   block executes. A failed criterion is not permission to extend or pool samples.
3. If the prospective gate passes, use the same 128 pairs and protocol for
   `h1-confirm-w1-v1-champion79`, baseline runtime79, all other settings unchanged;
   then run `tools/p4-tt-confirm-analysis.py champion79`. No claim against79 is
   inferred from the80 result alone. Both fixed comparisons must be qualified for
   the joint claim; owner/GPT acceptance stays separate.

This is confirmatory claim attempt 1: nominal alpha 0.025 is reserved, split across
controls to give 98.75% paired-normal primary intervals. Bounded sensitivity is
reported explicitly; approximate inference never becomes exact proof. Future
adaptive candidate/domain claims must retain this expenditure, using the protocol's
prospective allocation rather than resetting it. The 128-pair block targets about
six score points of precision at the pilot variance, not small-Elo resolution.
Expected cost is about 2h10m per executed block. No optional within-run stopping,
H1 changes, heavy overlapping work or mutation of frozen inputs is permitted.

W4 direct79/independent confirmation remains unresolved; no W1-to-W4 strength
transfer is assumed. Review it after the selected W1 unit. Conditional mechanism
follow-up, not selected: measure score availability across consecutive real game
requests before considering bounded age. Fresh-table diagnostics did not measure
that lifetime; new-generation hints convert old H1 scores to hint-only, so removing
cutoff generation checks alone would not establish useful retention. Preserve
history, hint legality, score-domain and coherence safeguards in any later revision.
Do not close this CGLHW merely because the current turn or one comparison ends.

## Rebuild, resume and human actions

Ignored evidence must exist before reuse; a fresh checkout lacks it. Reverify
manifests and hardware, and never overwrite frozen installations. Build79 at its
exact detached commit and source80 at 9fec1d2a use `gradlew.bat :app-uci:installDist
--console=plain`; outputs are `app-uci/build/install/app-uci`. Existing logs:
`build/p4-tt/control79/build79-build.log` and
`build/validation/p4-tt-control80-build.log`. H1 at a72cc496 uses
`gradlew.bat installResearchWorkspace --console=plain`, output `build/research/runtime`,
log `build/validation/p4-tt-build81.log`. Use Java21+35 and checkout-local
`GRADLE_USER_HOME=<checkout>/build/migration/gradle-user-home`. Freeze new copies
with the objective tool and qualify new identities; rebuilds do not inherit old hashes.
The retained diagnostic prototype `raw/runtimeh1-diagnostic-v1` has its own manifest
and must not be confused with the causal80 control or committed81 candidate.

A running JSON status alone is not proof of a live process: compare the run's
process-cost PID/creation evidence with actual OS processes and commands. Preserve
interrupted PGNs/logs/manifests, resolve the cause and record a separately named
prospective retry; never interpret partial scores as a complete comparison or
reset confirmation error expenditure after viewing game evidence.

Git: all changes belong to this authorized objective; raw outputs remain ignored.
Application version is 0.0.0/build81 from the H1 implementation commit. Repository
AGENTS.md uses commit-based build rules with **no finalizer dependency**. Root
CODEXLOG_CURRENT.md was absent; do not create it. Inspect current Git/file presence
again on resume and preserve unrelated changes. No push/deployment/release is authorized.

Human action: owner refresh of the separate Phase4 Project Frontier mirror to
**P4-F005**, confirming **P4-R001 / P4-F005**. This is **non-blocking** for repository
research and blocks only mirror-dependent Project planning. Prior mirror refreshes
are not inferred complete. No owner action currently blocks the selected research.
