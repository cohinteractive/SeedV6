# CLV-R001 execution state

6 November 2026 (Pacific/Auckland). **ACTIVE; unchanged C1 selected for the sole
independent confirmation; BLOCKED on a separate owner idle window. Governing
strength success is unproven. No campaign closeout, acceptance or promotion.**

Read [CONTRACT](CONTRACT.md), [RESULTS](RESULTS.md), `budget.json`,
`candidate-build99-manifest.json`, `c1-timed-screen-w4-v2-analysis.json`,
`c1-confirm-w4-v2-protocol.json` and `confirmation-readiness-v1.json`.
Repository masters P4-R034/P4-F052 govern; this is the focused resume point.

## Actual outcome and frozen candidate

C1 uses current near-depth node-level TT bounds to reduce otherwise-positive quiet
LMR by one ply, after existing adjustments. It does not assume the retained move
produced the score; no TT redesign or singular extension was needed. Explicit
flags3329490943; normal production stays1182007295. Shared engine-core architecture,
CP64, PVS/ADAPTIVE16, Hash64, EAGER_ROOT atW4 and BOUNDARY2 remain.

Implementation commit **46d1543121f9bc13e7c2a4832efa78bd14b5363c**, build99;
`raw/runtime-c1-build99-v1/`. Core SHA256
`9d7cf507fdecd7b7aaad07ff35c1bb54ed7daecd5034cb3e31467eb4a5c85ea4`.
Exact control is original build96/source44fc2bf2 app/core JARs in
`raw/baseline-build96-v1/app/`; core SHA256
`3211f4f375d667f3f442a21d88d8b9755d178ea6a261f0fee74f8ffe84bf84ea`.
The command invokes original UciMain with historical MW_C4_W4, without `--nnue`
(which would select a different old route). CP64 SHA256
`8431626c2fca81e0a1e9ccd5e45ddd8829e6e3282c756c21fad75b06d075bb2b`.
Java21, Windows Ryzen5500, W4/Hash64/10+0.1 are pinned in manifests/protocol.

Completed exploration: **31W/15D/18L in64 games;60.15625%; paired-normal95%
50.107827-70.204673%; bounded95%36.148180-84.164320%**. All games normal;
9039 audited requests, no fallback warnings, input stability and complete drain.
White76.5625%/Black43.75% and first62.5%/second57.8125% remain descriptive risks.
The separate4-game operational pilot passed and was not pooled or used for strength
selection. The positive screen is not independent acceptance.

Prior fixed work:240 requests/80,904,550 nodes, repeated W1 determinism and real W4
consumption, with mixed decision proxies and a small adverse W4 depth cost.
Both low-budget formulations matched the deeper control on14/20 roots; no proxy
strength claim.36 actual-UCI checks added960901 nodes; legal PV, accounting, stop,
reset/reuse, worker reconstruction and drain passed. Java215 core tests passed;
broader research53/54 then8/8 rechecks after correcting one inherited obsolete
TC-P1 default expectation.4 development admission tests and9 confirmation-control
checks pass. See RESULTS and preserved logs/XML for all failures and limitations.
Native GUI/platform/install/deployment checks were not performed.

## Authority, processes and resource accounting

The owner authorized75 minutes for the concrete4-game pilot and64-game screen.
`owner-idle-window-v1.json`: first clock observation2026-11-05T21:42:37Z through
22:57:37Z (10:42:37-11:57:37am NZDT6 November). **That exact68-game scope is fulfilled.**
Unused time grants no confirmation or additional games. No owned experiment remains;
all8 supervised runs drained and budget.pending/ownedProcesses are empty.

Spent **2357.434475000133 charged seconds /86400**, **68/640 development games**,
**0/640 confirmation games**, **0/1 confirmation attempts**. Failed/interrupted games0.
Fixed-work nodes81,865,451. Remaining wall84042.565525s; development572 games;
confirmation640. No closed-campaign allowance transfers. Source/input preparation,
compilation and ordinary correctness tests are not chess experiments; all launched
experiments, operational pilots and in-worker audits are charged under CONTRACT.

Planning reserve28830s is within the original ceiling. Full640-game forecast is
20982s;25%+120s margin26348s. Protocol maximum28800s plus30s drain. Request a
**separate8h15m PC-idle and awake window**; no such authority exists yet.
Do not launch against the old window or manufacture dates/permission.

## Independent sample and exact restart

**Use confirmation protocolv2**. It freezes unchanged C1 and exact96,320 fresh
opposite-colour pairs/640 games, no interim score access, score>=53% and paired-normal
95% lower strictly>50%, plus mandatory technical/operational qualification.
No tuning, substitution, optional stopping, pooling, selective exclusion, replacement
pairs or another attempt. First durable pair-start spends the attempt and both games.
Incomplete/disqualified attempts cannot pass; no replay/resume after first pair-start.
A crash requires factual pending-ledger/journal/process reconciliation before any action.

`confirmation-v1-openings.pgn` holds320 unique legal14-ply startpos histories from
pinned bjbraams source, fixed odd-hash partition and seed2026110605, selected from
19928 eligible positions. Comprehensive360-input audit reports zero overlap.
The earlier16-ply pool could not provide320 fresh pairs; this same-source-family
seven-move distribution is declared before confirmation. It adds transfer/throughput
uncertainty; no candidate or acceptance criterion changes. Frozen clock input
fixtures were checked without reopening their result files. Some input metadata
contains source headers, ignored by selection. No confirmation engine requests or
games have run.

All447 protocol input pins, opening order and qualification evidence pass read-only
preflight. Confirmation admission correctly refuses absent separate owner authority.
The new confirmation supervisor reuses existing native Job/workload/game audits;
9 tests include isolated synthetic full-sample, failed-pair and prelaunch-refusal
paths and launch no engines. Preparation failures and unlaunched protocolv1 are
preserved; v2 is the executable prospective specification.

Next actions:

1. Obtain explicit8h15m confirmation window. Record actual owner scope and observed
   UTC start/deadline in `owner-idle-window-v2.json`, with `status=authorized`,
   `receivedUtc`, `hardDeadlineUtc`, `allowedProtocols=["c1-confirm-w4-v2"]`,
   `maximumTimedGames=640`, and the authorization source/terms. Add its path to
   budget.authorizedIdleWindows. Do not infer authority from this template.
2. Recheck frozen pins, unchanged candidate, resources, pending state and all ceilings.
   Execute the one supervisor. Wait through existing process supervision; inspect
   no interim scores. Preserve the CLV STOP file if present; never remove it blindly.
3. Only after640 audited games and complete drain, run the frozen analysis. Report
   primary paired-normal, variance, effect size, bounded/bootstrapped sensitivity,
   all paired outcomes, colour/order/operational limitations and actual resource use.
4. If the sample is interrupted/disqualified or either primary condition fails,
   strength remains unmet. Do not spend another attempt or relabel technical value
   as strength success. GPT reconciliation/owner disposition remains separate.

```powershell
$env:PYTHONPATH = (Resolve-Path -LiteralPath build/research/ic-python).Path
python -B tools/p4-critical-line-confirm.py --protocol docs/research/search/p4-critical-line/c1-confirm-w4-v2-protocol.json
python -B tools/p4-critical-line-confirm.py --receipt docs/research/search/p4-critical-line/c1-confirm-w4-v2-execution.json --output docs/research/search/p4-critical-line/c1-confirm-w4-v2-analysis.json
```

Run sequentially with failure checks; never run analysis after a failed/incomplete
sample. No new feature development is selected while this unchanged candidate is
frozen. The remaining decisive evidence requires the independently held-out match.

Starting tree was clean b8ac447e/build98. Runtime work committed46d15431 with one
verified build-only increment98->99; earlier preparation handback35a5ff5e. Subsequent
reports/protocols/research tooling have no built/shipped impact: no further bump.
Raw evidence is local/ignored and preserved; compact records/fixtures are tracked.
Root journal absent; no finalizer, global behavioral registry or CEC maintenance.
No push, production-default change, champion advance, installation or deployment.

Human actions: **blocking for confirmation** - authorize the separate8h15m idle/awake
window. **Non-blocking for repository work** - refresh both Phase4 Project Source
mirrors toP4-R034/P4-F052 before mirror-dependent GPT coordination. No owner
acceptance or formal closeout is requested at this intermediate research boundary.
