# IC-R001 resume state

5 November 2026 (Pacific/Auckland). **ACTIVE; awaiting owner idle-window authority
for timed exploration. Governing strength objective UNRESOLVED.** Bootstrap and the
first implementation/diagnostic unit are complete. No acceptance, formal closure,
champion advance, production promotion or deployment is claimed.

Start here, then [CONTRACT](CONTRACT.md), [RESULTS](RESULTS.md), `budget.json` and
named protocols. Current Phase4 masters **P4-R030/P4-F048**. Owner-confirmed launch
mirrors were P4-R029/P4-F047; mirrors are owner-maintained.

## Authority and identities

- Initial clean main HEAD `4af0e298b759e222fe53de4fe6ff14bc0b46f612`, build97.
  Search/core matched accepted96 `44fc2bf2fe54e87dee7721bdaa451f3b00f3b1d0`.
  Later packaging/UCI naming changes were reconciled without replacing the control.
- `baseline-manifest.json` verifies original96 artifacts/CP64/Java21; local exact
  snapshot `raw/baseline-build96-v1/app/`. Production is MW-C4/EAGER_ROOT/1182007295
  atW4, TC-P1/1177812991 elsewhere. Closed campaigns/champion/baseline79 preserved.
- Candidate **IC-C1 Volatility carryover** retains half the previous resolved
  initial aspiration half-width as an ADAPTIVE16 floor, capped512. Same MW-C4,
  PVS, CP64/Hash64/W4/BOUNDARY2; research-only, no production change. RESULTS
  describes the mechanism, dependencies and actual evidence.
- `runtime-c1-uci-v1-manifest.json` pins source/class/JAR hashes. Snapshot
  `raw/runtime-c1-uci-v1/{classes,lib}` loads original96 core/UCI for both CONTROL
  and VOLATILITY_CARRYOVER with identical research binding/runtime. OutsideW4 both
  delegate exact production routing. Candidate is an overlay, not a new app build.
- VERSION_STATE.txt strictly validated regular/single-link UTF-8 JSON0.0.0/97.
  Research-only tools/tests/docs: **no_bump**, all version bytes preserved.
  CODEXLOG_CURRENT.md absent; no journal created. Shared governance/CEC unchanged.

## Evidence and limitations

Source challenge found no comparable challenger score vector; completion already
protected; shared TT correlates workers; effort excludes earlier failed passes and
is not useful-work measurement. Stable move is not confidence. Pinned external
source/history/reversions studied; no external code/constants or frozen clock-study
scores imported/opened.

Four diagnostics passed160 requests/32M admissions: baseline bound-pass fractions
15.86%/21.13%, C1 9.24%/10.09%; mean depths7.825/7.725 versus7.875/7.900. SMP
variability is material; no speed/decision-quality/strength claim. Latest41 Java
and6 supervisor tests passed. UCI qualification passed12 requests/320,289 admissions,
node/stop/reset/reuse/PV, W1/W2 fallback identities and native drain. All source,
protocols, summaries, receipts and raw evidence retained. No managed-clock game,
full GUI/platform/deployment qualification or independent confirmation yet.

Setup failures/corrections are recorded in RESULTS. No failed experimental launch,
lost evidence or unaccounted experiment remains.

## Resource/process state

Fresh `budget.json`:37.5157479000045/86,400 charged seconds;86,362.48425209999s
remaining.0 initiated/completed/failed/interrupted timed games of800.0/1 independent
attempt spent; max240 pairs/480 games. Planning reserve480 games/21,600s is inside
that total. No pending reservation, active experiment or owned process. No idle
window exists. Compilation/docs/correctness tests excluded; all five launched
experiments include admission-through-drain wall.

## Next action and exact resume

The useful next evidence is timed competition, not additional node-only tuning.
Unchanged candidate has two prepared fixed protocols:

1. `c1-timed-pilot-w4-v1-protocol.json`:2 pairs/4 operational games, max300s.
2. After score-blind pilot technical/operational audit passes,
   `c1-timed-screen-w4-v1-protocol.json`:32 pairs/64 exploratory games, max3600s.

Both10+0.1/W4/CP64/Hash64/BOUNDARY2/exact-C4, alternating first colors, cold pair
JVMs, fixed samples; no live score stopping, selective deletion or automatic replay.
Pilot never enters selection statistics.34 development pairs are frozen in
`development-v1-openings.pgn` and its selection manifest; not confirmation inputs.

**Blocking human action:** owner authorizes a specific75-minute idle window, or
another concrete window that fits a responsibly adapted prospective batch. This
is the execution request's timed-experiment gate. Unused budget, old MW permission,
a later invocation or silence do not grant authority.

After explicit authority, record actual received time, hard UTC end, status and
owner wording in new `owner-idle-window-v1.json`; register its exact relative path
in `budget.json.authorizedIdleWindows`. Do not create a fictitious permission file.
Recheck frozen inputs/raw availability/resources/pending state. Execute sequentially:

```powershell
$env:PYTHONPATH = (Resolve-Path build/research/ic-python).Path
python -B tools/p4-iteration-confidence.py --protocol docs/research/search/p4-iteration-confidence/c1-timed-pilot-w4-v1-protocol.json
# Review operational audits without pilot WDL as a selection gate.
python -B tools/p4-iteration-confidence.py --protocol docs/research/search/p4-iteration-confidence/c1-timed-screen-w4-v1-protocol.json
```

Local Python dependency is chess1.11.2 in ignored build/research/ic-python. Supervisor
enforces wall/games/reserve/window, CPU/memory/STOP, frozen inputs and owned Job drain.
It deliberately rejects independent confirmation: freeze its own candidate/control,
fresh held-out audit, sample/method/stopping/failure rules and power assessment first.
Do not overwrite admitted protocols/receipts; use new IDs for justified revisions.
Select further integrated work from actual exploratory evidence.

Raw/runtime files are ignored and local, not Git deliverables. A fresh checkout
must check availability and restore exact hashed artifacts or explicitly reconstruct
and qualify a new identified runtime; hashes alone do not imply availability.
Current source hashes and normal Git history identify this research implementation.
No external installation/push is required.

## Other human actions and disposition

Owner refresh of Project Source mirrors toP4-R030/P4-F048 is non-blocking for
repository work, required before mirror-dependent GPT coordination. Final acceptance,
champion/promotion and deployment remain distinct. Do not close IC-R001 or launch
a successor. Changes are attributable from the initially clean tree; final commit
and Git inspection are reported in handback and available in normal Git history.
