# CH-R001 resume state

**ACTIVE: CH-C2 frozen W1 exploratory screen running.** Read [CONTRACT](CONTRACT.md)
first. The governing strength objective is not yet demonstrated. Normal production
HL-G1/build85 policy and the accepted research champion HL-G1/build84 are unchanged.

## Resume and ownership

CH-C1 is complete and drained. CH-C2 uses `c2-screen-v1-protocol.json`, frozen in
commit `eea24b5e`, and output `raw/c2-screen-v1-w1`. Its original execution session
is94506, observed Python launcher31132/worker10508 and owned Fastchess16848.
Revalidate `run.json`, output and exact processes before any action; identifiers
are observations, not perpetual liveness claims. Never relaunch into existing output.
At the latest inspection30 of64 games were complete and status was running. No
partial score has informed selection or stopping. The original qualified supervisor
owns timeout, warning handling and process draining. Await its final independent
audit and handle drain before reading results or running other engine computation.
A chat observer produced excessive repeated output; this did not restart or alter
the match. Authoritative evidence is the existing runner output, not chat polling.

Next: reconcile the completed C2 audit and actual games/wall time; assess its result
alongside mechanism/cost evidence. Select further bounded development only for a
specific credible question, or freeze a justified finalist for W4 risk and the single
fresh160-pair W1 confirmation. No owner decision currently blocks continuation.
Cross-request lifetime is a conditional investigation, not a selected implementation.
No independent confirmation or W4 competitive match has started.

## Resource ledger

Completed spend:32 W1 exploratory pairs/64 games;0 confirmation pairs/games/attempts;
0 W4 pairs/games. Charged match wall1951.7385866s, conservatively including setup/audit.
Completed-spend balance:96 W1 exploratory pairs/192 games;160 confirmation pairs/320
games;32 W4 pairs/64 games;576 total games;41,248.2614134s of43,200s match wall.
C2 currently reserves32 exploratory pairs/64 games and at most3600 runner seconds;
add actual execution, including partial/failed games and whole run wall, at drainage.
Ordinary technical work is separate; summaries below identify bounded tests/diagnostics.
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
No runtime edits after that freeze. Full GUI/training/platform tests were not run;
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
