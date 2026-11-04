# IC-R001 resume state

5 November 2026 (Pacific/Auckland). **ACTIVE; IC-C1 rejected; IC-C2 qualified for exploration,
BLOCKED on a new owner idle window. Governing strength objective UNRESOLVED.** No owner
acceptance, closure, champion advance, production promotion or deployment.

Start here, then [CONTRACT](CONTRACT.md), [RESULTS](RESULTS.md), `budget.json` and
named protocols. Current Phase4 masters **P4-R030/P4-F048**. Owner-confirmed launch
mirrors were P4-R029/P4-F047; mirrors are owner-maintained.

## Authority and identities

- Initial clean HEAD4af0e298, build97, Search identical to accepted build96 source
  `44fc2bf2fe54e87dee7721bdaa451f3b00f3b1d0`. This execution began clean at06ca08f9,
  which contains the bootstrap, candidate, fixed-work evidence and frozen protocols.
- Exact control is MW-C4/EAGER_ROOT/1182007295 atW4, CP64/Hash64/PVS/ADAPTIVE16/
  BOUNDARY2,10+0.1, Windows Ryzen5 5500/Java21. TC-P1 routing elsewhere is preserved.
  `baseline-manifest.json` and `runtime-c1-uci-v1-manifest.json` pin original96 core
  and UCI JARs, shared evaluator/runtime and the isolated research binding.
- **IC-C1 Volatility carryover:** ADAPTIVE16 adds half the previous resolved initial
  width as a floor, capped512. Center, recovery, mate verification, publication,
  hard stops and BOUNDARY2 remain unchanged. No production-default change.
- VERSION_STATE.txt is strict regular/single-link JSON0.0.0/97; research-only work
  is **no_bump** and all bytes remain unchanged. Root CODEXLOG_CURRENT.md absent.
  No shared behavioral-governance or CEC maintenance is authorized or performed.

## Completed evidence and decisions

Bootstrap challenged root-score comparability, completion provenance, correlated
workers and effort semantics. Pinned external source/history/reversions were studied.
Four diagnostics passed160 requests/32M admissions. C1 repeatedly reduced failed-pass
work; depth/effort changes did not establish decision quality or strength. Prior
41 Java/6 supervisor tests and12-request UCI qualification passed.

Owner authorized75 minutes starting at first observed21:56:47Z; hard end23:11:47Z
(12:11:47pm NZDT). `owner-idle-window-v1.json` preserves exact wording and scope:
prepared4-game operational pilot then unchanged64-game screen only. **That scope
is now fully spent; remaining clock time does not authorize additional timed games.**

Pilot passed4 normal games/535 audited requests, stable inputs and native drain.
Its score was not used for selection and is permanently excluded from statistics.
Screen passed64 normal games/8,548 audited requests, no fallback warnings, stable
inputs and all132 owned processes drained. Mean total CPU35.60%, maximum78.26%,
minimum free memory14.76GiB, peak owned commitment0.817GiB. The analysis implementation
was pinned and three analysis tests passed before viewing any screen scores.

IC-C1: **14W/25D/25L,41.40625%**,32 opening pairs. Paired-normal95%34.9507-47.8618%;
Hoeffding95%17.3982-65.4143%. This is adverse selected exploratory evidence, not
independent confirmation or universal inferiority. **Do not advance C1, replay this
screen, enlarge its sample, or spend the sole confirmation attempt on it.**

Read-only replays found weak/mixed support for cumulative failed-pass effort and
asymmetric directional carryover; both are deferred. Raw evidence, failed/negative
conclusions, manifests and frozen source/runtime identities remain preserved.

## Current candidate, resources and exact next action

**IC-C2 Upper-window volatility carryover** is the next isolated formulation.
Root-job diagnosis found that C1's lower failed-pass work coincided with more
within-pass full retries and incomplete work. C2 preserves baseline initial alpha
and recovery width while carrying volatility only into initial beta. This tests
asymmetric cost, not score-direction confidence. C1 remains frozen and rejected.

The research-only observation copy is mechanically checked against exact96 source,
with job-private records read after drain. It is absent from timed UCI composition.
Two reciprocal diagnostic batches passed320 requests/64M admissions. C2 does not
clearly reduce failed-pass work versus C4; its root-retry/incomplete costs avoid the
C1 pattern. This supports a bounded timed test, not a strength or causal-speed claim.
RESULTS preserves all counts, variability and weak/negative alternatives.

Latest17 Java tests passed; C2 UCI qualification passed12 requests/320,269 nodes,
node/stop/PV/reset/accounting, W1/W2 fallback and native drain. Exact source/class/
runtime/evaluator hashes are in `runtime-c2-v1-manifest.json`; local frozen runtime
is `raw/runtime-c2-v1/`. Core96 and UCI96 remain the same pinned control. C2 has no
timed result and is not selected for independent confirmation.

`budget.json`:2247.7328160000616/86,400 charged seconds (37m27.73s),68/800 initiated
and completed timed games,0 failed/interrupted.96,640,558 fixed admissions.
Remaining84,152.26718399994s and732 games, including480-game/21,600s confirmation
planning reserve;252 development games currently available.0/1 independent attempt.
All16 experiments completed; no pending reservation, active experiment or owned
process. Compilation/correctness tests/read-only analysis are not charged experiments.

**Blocking next action:** new75-minute owner idle window for prepared C2 pilot4
then screen64. Protocols `c2-timed-pilot-w4-v1-protocol.json` and
`c2-timed-screen-w4-v1-protocol.json` pin exact inputs/methods and hard5/60-minute
limits. The same frozen DEVELOPMENT openings are reused; no independent/fresh claim,
no C1/C2 pooling, no pilot pooling, no score-based stopping or automatic replay.
After68 proposed games184 development games remain with480 confirmation games reserved.

After explicit authority, record actual received UTC, hard end and exact owner
wording in NEW `owner-idle-window-v2.json`; register that path in the ledger. Do not
rewrite v1 or fabricate permission. Recheck frozen hashes/local availability and
pending/ownership/resources. Execute sequentially:

```powershell
$env:PYTHONPATH = (Resolve-Path build/research/ic-python).Path
python -B tools/p4-iteration-confidence.py --protocol docs/research/search/p4-iteration-confidence/c2-timed-pilot-w4-v1-protocol.json
# Review the completed pilot operationally without selection WDL.
python -B tools/p4-iteration-confidence-results.py --receipt docs/research/search/p4-iteration-confidence/c2-timed-pilot-w4-v1-execution.json --output docs/research/search/p4-iteration-confidence/c2-timed-pilot-w4-v1-operational.json --operational-only
# Only after that review passes:
python -B tools/p4-iteration-confidence.py --protocol docs/research/search/p4-iteration-confidence/c2-timed-screen-w4-v1-protocol.json
```

Review full screen operational validity before analysis. The results tool rejects
live/incomplete samples; failed or interrupted games remain charged and preserved.
No further candidate, retuning or independent attempt is selected. Confirmation
still requires a separate freeze, fresh held-out inputs, sample/method/failure/
stopping rules and meaningful power. Production remains unchanged.

Raw/runtime paths under `raw/` are ignored local evidence, absent from a fresh checkout.
Check availability/hashes before analysis or reconstruct and qualify a new identity;
never silently replace old snapshots. Python chess1.11.2 is in ignored
`build/research/ic-python`; set PYTHONPATH there for existing supervisor/UCI tools.
Do not edit an admitted protocol, receipt or its inputs. No auto replay/prefix scoring.

## Human dependencies

A new specific idle window blocks C2 timed execution. The previous window's4+64
scope is fulfilled; silence, a later invocation and unused budget do not authorize
more timed games. Owner refresh of Project Source mirrors toP4-R030/P4-F048 is
non-blocking for repository work, required before mirror-dependent GPT coordination.
Acceptance, champion/promotion, installation/deployment/push/release remain separate.
