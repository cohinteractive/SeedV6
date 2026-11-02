# CH-R001 resume state

**ACTIVE: CH-C3 technically qualified; committed artifact freeze next.** Read [CONTRACT](CONTRACT.md)
first. The governing strength objective is not yet demonstrated. Normal production
HL-G1/build85 policy and the accepted research champion HL-G1/build84 are unchanged.

## Resume and ownership

C1 and C2 are complete, fully audited and drained. No competitive or diagnostic process remains active. C3 sequential diagnostics
(session31779/Python34300) completed with six successful execution records and
clean process exits; its UCI checks also closed all engines. C2's original session94506 is no longer available; its
run record says passed/exit0, all engine handles drained and observed OS PIDs are
absent. Never restart either output. The independent arithmetic cross-check of C2
pair mean/standard error agrees with the qualified runner.

Next: implement and qualify CH-C3, bounded cross-request retention of completed
correction feedback over the main+qsearch integration. Snapshot only completed
invocations; abort restores that snapshot; each request decays it7/8; new-game or
runtime failure forgets it. Propagate private forgetting through the idle/drained
parallel new-game path. HL histories remain request-scoped. This is a specific
lifetime hypothesis grounded in inspected external lifetime and current timed-search
abort behavior, not a parameter sweep. No C3 game is admitted until meaningful
lifetime/rollback/worker/default-isolation and runtime/cost checks pass.

Remaining development can fund a32-pair C3 screen if qualified; further spend depends
on evidence. No finalist selected, no independent confirmation/W4 match started.
The confirmation analysis tool has been adapted to the exact84 comparator; seven
synthetic cases pass, including practical-minimum rejection and disagreement with
bounded sensitivity. Degenerate zero variance cannot replace the required normal
criterion with a different success rule. This preparation uses no confirmation data.

## Resource ledger

Completed spend:64 W1 exploratory pairs/128 games;0 confirmation pairs/games/attempts;
0 W4 pairs/games. Charged match wall3988.850172s, including setup/audit.
Remaining:64 W1 exploratory pairs/128 games;160 confirmation pairs/320 games;
32 W4 pairs/64 games;512 total games;39,211.149828s of43,200s match wall.
No active reservation. Ordinary technical work is separate and summarized below.
Budgets are ceilings, not consumption targets. No pooling exploration and confirmation.

## Authority and bootstrap challenge

Owner request is retained exactly at ignored `raw/owner-request.txt`; bootstrap
manifest pins it. Initial clean `main` HEAD was296c0fd1f2b04d06177b30a93535534cf8fa39ad,
version0.0.0/build85, mastersP4-R005/P4-F021. Existing detached baseline79 worktree
was untouched. Root CODEXLOG_CURRENT.md is absent; no journal created. VERSION_STATE
is strict UTF-8 JSON, regular/non-reparse/single-link. Direct qualifying commit-based
build management applies; no finalizer or replacement machinery is used.

The two Phase4 masters now record launch atP4-R006/P4-F022. Owner reports launch
mirrorsP4-R005/P4-F021 current; later mirror refresh is owner-maintained. Shared
behavioral governance and CEC were not changed, and no registry release was assumed.
No champion/default promotion, push, release, deployment or successor objective is
within this authorization. TT-R001 reconciliation remains separate; F3 is optional
and was not adopted. Neither frozen BOUNDARY2 study has been opened or replayed.

Bootstrap verified all33 exact frozen84 runtime files, CP64 and Java21+35 hashes.
Host: Windows11 10.0.26300, Ryzen5 5500,6C/12T, about32GiB installed. Exact comparator
is `../p4-learning/raw/runtime-build84-v1`, guided/flags104071167, source
b8ea8b2856eed3f6c774860ca540375054e4de30. Current build85 foundation is not substituted
for that artifact. Immutable79 and corrected80 remain distinct, with competitive
bridge unknown. HL-G1's accepted W1 result and adverse/inconclusive W4 qualification
remain as recorded in CONTRACT and prior campaign; no universal superiority claim.

Code inspection established raw evaluation, original windows, nominal depths,
completed quiet moves and selective provenance at the proposed hooks. Synthetic
null/verifier scratch needs explicit restoration. Request-private HL learning retains
across iterations but clears at request boundaries/abort; ROOT_REUSE avoids reseeding.
Production extension settings suppress ordinary TT proof use; F3 is unnecessary for
these candidates. Independent exact main/qsearch references remain unchanged.
Default factories do not select correction flags. Qualified owned-job runner performs
legal-history/PV/adjudication/accounting audits and narrowly audited scoreless fallback.
Prior runs estimated the640-game ceiling at about5.7h, below12h; actual run accounting
remains binding. Existing independent opening source/exclusion receipts support a
fresh160-pair confirmation; those inputs and its protocol still require freeze.

## Implemented formulations and technical qualification

[FORMULATIONS](FORMULATIONS.md) records verified pinned external implementations,
author-reported external tests, Seed-specific hypotheses and measured Seed evidence.
Stockfish19 and Bagatur5.5c both preserve raw evaluation and correct qsearch stand-pat;
contexts/formulas/lifetimes differ. Inspected Ethereal14 files supply related history/
static consumer context, without a correction implementation found in those files.
Pinned raw source hashes are retained; external success is not Seed strength evidence.

CH-C1, `corrected`/flags238288895, adds worker-private bounded pawn correction to
HL-G1 main selective evaluation/improving consumers. The2x16384 short table uses
four fractional bits, cap256 native units and depth-weighted EMA. Eligible completed
quiet ordinary outcomes train relative to raw evaluation, with directional bounds,
null/verifier, repetition/rule50 and subtree rule-result safeguards. Selective labels
remain heuristic. Request-end/abort clears. Raw TT/diagnostic meanings are preserved.

C1 technical evidence: `c1-tests-v1-summary.json` records215 selected JUnit tests,
zero failures/errors/skips after correcting a diagnostic test's cumulative-counter
assumption. Failed logs/XML remain in raw/. Coverage includes Search/rules/UCI,
Construction/exact references, learning/TT/null/accounting and parallel lifecycle;
dependency boundaries passed. C1 observation:18 requests/domain plus18 clean W1
repeats; W1 updates1727, changed reads54307, changed improving1772, max164, no near
saturation. W4 updates1924, all4 private workers used. W1 observation neutral; W4
scheduling equality not claimed. Clean144 requests including36 warmups took37.44s;
wall ratio1.0041 W1/1.0313 W4 versus guided, not a speed/strength claim. UCI isolation
52 requests+4 stops and runtime14 requests+2 stops passed; postcommit runtime checks
also passed. Qualified runner's6 supervision/audit tests passed in0.90s.

C1 source d51d147920bfe1e0f30caee47c90d7191cfbf52a/build86; frozen
`raw/runtime-build86-c1-v1`. Manifest and `build86-c1-qualification.json` compare2114
JAR entries with prototype: only generated version metadata differs;4 preflights pass.
Direct build85->86 changed only the build token, preserving user fields and all
other bytes. Version hash98fdc7d39a424ea86124f1e99930b8f7c926429328cea79e7996bd8b650d28d1.

CH-C2, `corrected-q`/flags506724351, adds corrected ordinary qsearch stand-pat to C1;
no qsearch training. Terminal/rule adjudication precedes evaluation, null/verifier
uses raw, changed stand-pat taints selective provenance. Same contexts/updates/lifetime.
C2 also repairs an optional deeper-futility trace argument that C1 mislabeled working
as raw. This observer was disabled in games and the old diagnostic counter ignored
that argument; stored raw/search policy were correct. Independent constant-evaluator
regression passed. All9 current C1 W1 root outcomes reproduce the frozen prototype.

C2 `c2-tests-v1-summary.json`:59 selected JUnit tests pass, including9 correction
cases and learning/null/accounting/exact-reference/boundary checks,51s build/test.
Observation27 requests/domain plus27 clean W1 repeats:1732 W1 updates,81238 of849931
qreads changed,1893 improving changes,max243,no near saturation. W4 also exercises
qcorrection. Clean216 requests including54 warmups took57.55s; wall ratios0.9681 W1/
1.0332 W4, short variable diagnostics, not speed claims. Isolation65 requests+6 stops,
runtime14 requests+2 stops passed; all engines closed before competitive admission.

C2 source84c951287ea7c9ef0a280d32baacf976715c90c5/build87; frozen
`raw/runtime-build87-c2-v1`. `build87-c2-qualification.json`:2114 JAR entries equal
the prototype except generated version metadata, all27 non-JAR files identical,
4 preflights passed. Direct build86->87 changed only build; current version hash
 a32ab6aaab3b2923b3a3def16f7c4e7457540b83115e33dbd7ed2e36d0f1479d.
C3 changes will follow this freeze; the frozen C2 artifact is preserved. Full GUI/training/platform tests were not run;
no GUI/native integration or deployment conclusion. Technical qualifications do not
establish playing strength.

## Competitive evidence and decisions

`development-v1-openings.pgn` prospectively selects32 distinct legal16 roots from
20127 unique bucket0 positions, seed2026110301, no outcome/evaluator filtering.
Tracked selection receipt pins legal histories/source/hash. Selection took42.93s
technical time. Only frozen input has the narrow ignore exception; raw games stay ignored.

CH-C1-SCREEN-V1, frozen81cb86a6:32 W1 pairs64 games,10+0.1/Hash64/BOUNDARY2,
exact84 guided versus86 corrected. **22W/16D/26L,46.875%**, paired-normal95 interval
35.0781-58.6719%; Hoeffding95 interval22.8669-70.8831%. All64 normal,8780 request
audits passed, no warnings, input hashes stable, handles drained; OS process closure
verified. Whole wall1951.7385866s, runner1948.4820658s. Compact summary
`c1-screen-v1-w1-summary.json`, raw `raw/c1-screen-v1-w1`. Old session56125 and PIDs
are historical; never restart this completed experiment.

C1 is adverse/inconclusive, not a gain or universal mechanism rejection. The concrete
main/qsearch asymmetry justified C2: selective decisions used corrected estimates
while leaf scores stayed raw, unlike both inspected correction implementations.
That is a hypothesis, not an explanation proved by C1 score. C2 uses the same32
opening pairs, exact84 comparator, seed2026110303, fixed64 games, estimated2300s,
runner cap3600s. Common inputs help descriptive comparison but do not establish an
independent component effect; all exploration remains selection-biased. No score
will be used until the prospective complete audit or operational termination.

## Conditional questions and provenance limitations

Pinned Stockfish19 engine/thread/UCI source connects ucinewgame/Clear Hash to worker
correction clearing; ordinary go does not clear. `external-lifetime-v1-manifest.json`
pins this source. Cross-request retention is a credible transfer question, conditional
on C2 and remaining development value. It needs explicit completed-label retention,
new-game/reset/abort ownership, aging and worker-private tests, not skipping a reset.
Other context/effort adaptations likewise need evidence; no parameter grid is planned.

Early state writes encountered Windows default encoding errors; attributable text was
repaired using explicit UTF-8. No frozen protocol/input/artifact was changed. Some task
JSON uses CRLF; CRLF-aware `git diff --check` passes without persistent Git config or
rewriting frozen bytes. Raw evidence is local-only and must be checked for availability
on a fresh checkout. No unrelated inherited work was absorbed or discarded. Latest
known HEAD eea24b5e; current uncommitted continuity/lifetime records belong to CH-R001.

## C2 completed result and C3 decision

`c2-screen-v1-w1-summary.json`:64 normal games/32 pairs,22W/13D/29L,44.53125%.
Paired-normal95 interval33.3702-55.6923%; Hoeffding95 interval20.5232-68.5393%.
All9407 request audits passed, no warnings, inputs stable, handles drained; observed
OS processes absent. Whole wall2037.1115854s, runner2034.7643092s. Raw evidence stays
at `raw/c2-screen-v1-w1`. No result was used before complete audit/drain.
C2 did not establish a gain. Its2.34 percentage-point difference below C1 on common
inputs is descriptive and too uncertain/selected to isolate a qsearch effect.

The next question is whether repeated rebuilding/discarding feedback each move
limits useful structural learning. C3 retains a completed-invocation snapshot and
ages it, while preserving raw/working distinctions and all C2 eligibility/consumers.
The7/8 per-request factor gives roughly5-request half-life without tuning to game
scores; it limits stale structural/tactical aliases, but cannot eliminate them.
Additional payload is64KiB/worker; copies occur only at completed invocation and
request boundaries. Risks remain context collisions, nonpawn changes and feedback
self-reinforcement. W1/W4 tests and representative sequential-history diagnostics
must establish actual retention/abort rollback and new-game/error clearing. This is
not a strength promise or automatic promotion of C2 despite negative evidence.


C3 initial qualification passed62 selected tests in53s. The new sequence diagnostic
initially failed compilation for a missing Move import (raw log retained); after
repair,12 focused correction tests passed and research workspace installed in6s.
Prototype `runtime-c3-prototype-v1-manifest.json` and `c3-source-v1-manifest.json`
pin uncommitted C3 atopf9af98d4 with inherited build87 metadata. This is not a new
committed application build. Main observation and three-request sequence observation/
clean cost protocols were frozen before execution. Session31779 owns their sequential
Java execution; no competitive games. Await all execution records before summarizing.


C3 completed qualification: `c3-diagnostic-v1-summary.json` records54 main observed
requests plus108 sequence requests and81 clean W1 repeats. All invariants and W1
observation neutrality pass. Current C2 reproduces all9 prior frozen W1 root outcomes;
C3's9 first sequence requests match C2, while14/18 later W1 outcomes differ. All18
later W1 requests consume carried feedback,12 change their first evaluation; W4 has
69 retained-worker requests and23 changed first reads. W1 sequences save249 completed
invocations and restore25 aborts; W4 saves5200/restores79. Max correction253 W1/227 W4,
no near-saturation counts. These are mechanism facts, not independently true labels.

Clean sequence cost:486 requests including162 warmups,67.20s execution total. C3/
guided geometric-mean wall1.00527 W1/1.00582 W4; simultaneous C2 ratios0.99662/0.99728.
These short variable measurements include changed work, not isolated speed effects.
C3 UCI isolation passed78 requests+8 stop/reset checks; exact84/default/guided W1
controls match. Runtime checks passed20 requests+2 stops, including W1 fresh versus
Clear Hash reset equality and repeated-request reuse; processes closed. See
`c3-cost-v1-summary.json`, `c3-isolation-v1-summary.json`, `c3-runtime-v1-summary.json`.
C3 is ready for direct qualifying build handling, committed rebuild/artifact bridge,
and prospective32-pair screen. No C3 match has started or been admitted yet.

Direct qualifying finalization verified build87->88 with unchanged user fields and
all other version bytes; hashc5716852b62716a63cf540cbc6582f5d3d978ad24431da48e672789e9f5557a8.
This identifies the attributable opt-in retained-feedback implementation in its own
commit. No finalizer, production/default promotion or extra documentation bump.
