# CLV-R001 execution state

6 November 2026 (Pacific/Auckland). **ACTIVE; timed development BLOCKED pending an
explicit owner idle window. Strength objective remains unmet; campaign not closed.**
Read [CONTRACT](CONTRACT.md), [RESULTS](RESULTS.md), `baseline-manifest.json` and
`budget.json`. No timed idle window exists. No strength success, confirmation
selection, production change, champion advancement or owner closeout is claimed.

Starting clean main HEAD `b8ac447e`, build98. Exact build96 control and CP64/Java21
hashes verified locally; Search kernel is unchanged through build98. Root journal
absent; strict single-link regular VERSION_STATE bytes recorded before mutation.
No inherited changes. No global governance/CEC maintenance.

## Findings and current work selection

Bootstrap challenge and fresh Stockfish/Ethereal/Bagatur source/history inspection
are in RESULTS. C1 uses current near-depth node-level TT bounds to reduce quiet LMR
by one ply, after existing adjustments; flags3329490943, explicit/non-default.
Production stays1182007295. Score/move association is deliberately not inferred.
No TT redesign or singular implementation is needed for this initial consumer.

Four fixed-work studies completed240 requests/80,904,550 nodes; mixed proxies show
consumption and deterministic W1 effects, not better chess.18 actual-UCI requests
also passed,480454 nodes. Java:215 core checks,53/54 broader research checks then
8/8 focused rechecks after correcting an inherited stale default-identity test;
4 initial CLV tests and4 Python admission tests pass. Failed evidence is preserved.
Read `technical-validation-v1.json`, `c1-fixed-analysis-v1.json` and named receipts.

1. Obtain an explicit75-minute PC-idle window for the frozen4-game operational
   pilot and separate64-game exploratory W4 screen. No timed process is authorized
   now. Preserve precise owner terms and UTC start/end in `owner-idle-window-v1.json`
   (`status=authorized`, `receivedUtc`, `hardDeadlineUtc`, source/terms of actual
   authorization), then add that path to budget.authorizedIdleWindows. Do not
   manufacture dates, infer old permission or create authority from the template.
2. Recheck pending ownership/budget and frozen input hashes; run the exact commands
   below. The screen is admitted only after the pilot's complete operational review.
   If the screen cannot fit its3600s ceiling plus30s drain before the owner deadline,
   do not start it. Preserve interruptions/spent pairs; no automatic replacement.
3. After the full independent game/request audit, interpret the exploratory screen
   and choose the next justified hypothesis. Do not mechanically extend an
   inconclusive sample or pool the pilot into selection.
4. Confirmation is unselected/unspent. It needs a full fresh input-only exclusion
   audit, fixed prospective protocol, operational qualification and an idle window.

All admitted experiments drained; ledger pending/ownedProcesses empty. Closed
campaign allowances remain untouched. Raw evidence stays local/ignored. Exact
budget is authoritative in `budget.json`: **132.96221470012097 charged seconds of
86400;81,865,451 admitted fixed-work nodes;0/640 development timed games;
0/640 confirmation games;0/1 independent attempts.** Remaining wall86267.0377853s.
The committed-runtime UCI recheck adds18 passed requests/480447 nodes, for276
total fixed-work requests across6 supervised runs. All failed setup/frozen inputs
are preserved. Tests/compilation are uncharged under CONTRACT; no experiment is hidden.

## Frozen executable and exact next commands

Implementation commit **46d1543121f9bc13e7c2a4832efa78bd14b5363c**, build99.
`candidate-build99-manifest.json` pins `raw/runtime-c1-build99-v1/`; core SHA256
`9d7cf507fdecd7b7aaad07ff35c1bb54ed7daecd5034cb3e31467eb4a5c85ea4`.
The kernel/config class bytes match the qualified prototype; committed metadata
was then exercised in `c1-uci-qualification-build99-v1`. Exact comparator remains
original build96 core SHA256
`3211f4f375d667f3f442a21d88d8b9755d178ea6a261f0fee74f8ffe84bf84ea`,
with original app-uci.jar in `raw/baseline-build96-v1/app/`.
The control command invokes its original `UciMain` without an opt-in evaluator
argument; `--nnue` would select a different historical route and is not used.

Use **v2** timed protocols. Unlaunched v1 protocols are retained: preflight found
duplicate slash/backslash supervisor paths with one stale hash after the pilot
gate addition. v2 canonicalizes paths; candidate/sample/criteria are unchanged.
`ready-for-idle-window-v1.json` verifies every input and deliberate refusal with
no recorded window. No production selector, inventory, installed client, global
governance/CEC or historical research evidence changed. Build98->99 was the sole
verified build-only increment with the runtime implementation commit; final
protocol/report/tooling changes have no further build impact.

Run from repository root **only after the explicit window is recorded**:

```powershell
$env:PYTHONPATH = (Resolve-Path -LiteralPath build/research/ic-python).Path
python -B tools/p4-critical-line.py --protocol docs/research/search/p4-critical-line/c1-timed-pilot-w4-v2-protocol.json
python -B tools/p4-critical-line-results.py --receipt docs/research/search/p4-critical-line/c1-timed-pilot-w4-v2-execution.json --operational-only --output docs/research/search/p4-critical-line/c1-timed-pilot-w4-v2-operational.json
python -B tools/p4-critical-line.py --protocol docs/research/search/p4-critical-line/c1-timed-screen-w4-v2-protocol.json
python -B tools/p4-critical-line-results.py --receipt docs/research/search/p4-critical-line/c1-timed-screen-w4-v2-execution.json --output docs/research/search/p4-critical-line/c1-timed-screen-w4-v2-analysis.json
```

Execute sequentially with failure checks; do not blindly run later commands after
a failed predecessor. The supervisor's native Job owns all descendants, samples
existing CPU/foreground/memory guards and observes the CLV `STOP` file. Never
delete an unexplained STOP. Use process waiting, not repeated model polling.
75 minutes covers4020s maximum planned reservations plus review slack; historical
64-game throughput was about35 minutes, an estimate not a promised duration.

Additional fixed-work sweeps now cannot resolve the governing strength question.
C1 has mixed proxies and is selected only for economical exploration. Singular
verification, move-associated TT information and complementary pruning changes
remain evidence-dependent options after that screen, not pending prescribed tasks.
No independent confirmation, acceptance, champion, promotion or deployment claim.
Full native/GUI/platform qualification and timed strength were not performed.

Human actions: **blocking for timed research** - authorize the specific idle window.
**Non-blocking for repository work** - refresh both Phase4 Project Source mirrors
to P4-R034/P4-F052 before mirror-dependent GPT coordination. No owner acceptance or
formal closeout is requested at this intermediate execution boundary.
