# CH-R001 resume state

**ACTIVE — CH-C1 technically qualified prototype, 3 November2026.**
Read [CONTRACT](CONTRACT.md) first. CH-C1 is experimental; no strength result yet.
CH-C1 screen is complete and drained; no match is running.
Normal production policy and research champion HL-G1/build84
are unchanged. This is progress from bootstrap, not research completion.

## Bootstrap evidence and challenge

Verified initial clean `main` HEAD `296c0fd1f2b04d06177b30a93535534cf8fa39ad`,
P4-R005/P4-F021, version0.0.0/build85. Existing baseline79 detached worktree remains
untouched. Root CODEXLOG_CURRENT.md absent: no journal capability or file creation.
Version is regular, non-reparse, single-link strict UTF-8 JSON; exact bytes/hash
in bootstrap manifest. Follow direct commit-based build handling; no finalizer.
No shared registry release assumed or shared policy changed. The new explicit
owner request supplies launch authority; historical no-successor statements remain
historical. Owner reports P4-R005/P4-F021 mirrors current at launch.

All33 exact frozen84 runtime manifest files hash-match, as do CP64 and Java21+35.
Host is Windows11 10.0.26300, Ryzen5 5500,6C/12T, about32GiB installed and16GiB free
at bootstrap. Existing owned-job Fastchess runner supports audited scoreless legal
fallbacks and independent history/PV/accounting audits; it will be qualified before
games. Prior HL complete match costs (~30–32s/game) suggest640 games ~5.7h, leaving
margin below12h; this is an estimate, not a resource waiver. Fresh160 roots remain
feasible using the retained independent source and exclusion receipts without reading
the forbidden clock studies. Inputs still need their own prospective selection/freeze.

Static findings rechecked against current source (not fresh runtime qualification):

- Shared default selects HL-G1 flags104071167. Raw CP64 is cached per node in
  FlatSearchState.rawEval, used by improving, reverse/shallow/deeper futility and
  verified-null entry. Optional F3 raw TT caching is separate and unnecessary initially.
- Completed-node hook has original window, nominal assigned depth, chosen move and
  searched score. SelectiveEpoch distinguishes exhaustive proof; qsearch/reduced
  fail-low/clipped pruning scores are not ground truth. Terminal/rule exits bypass
  completed(), but descendant outcomes still need conservative eligibility treatment.
- Request learning retains private quiet/capture/continuation/killer/counter tables
  across iterations/windows/root jobs, clears on abort and at request boundaries;
  ROOT_REUSE avoids reseeding retained quiet history. Correction can use this lifetime
  without shared mutable state. Cross-request lifetime requires a separate justified
  design, especially abort handling; not silently enabled.
- VERIFIED_NULL saves/restores borrowed per-ply raw scratch and excludes synthetic/
  verification learning. New working-eval scratch must receive equivalent restoration.
  Original18 dependency prevents invalid reverse ablations.
- Existing exact/qsearch references remain independent. Optional experimental flags
  must not alter their semantics or normal factories. Default isolation will be run.

External pinned search/history files from the HL raw evidence are locally present
and hash-matched. Stockfish19 uses multiple structural/continuation corrections,
clamps corrected scores, retains raw TT static values, directionally restricts quiet
feedback and couples correction to LMR. Bagatur5.5c uses pawn/material correction
and corrected stand-pat, with broader update eligibility. Ethereal14 search/history
has no correction-history match in inspected files; its static/improving/history
consumers are comparison context, not evidence that correction is ineffective.
Stockfish introduction commit b4d995d0 reports STC/LTC tests; reported external
success is not independently audited SeedV6 evidence. Source links and details will
be retained in FORMULATIONS alongside the first implemented hypothesis.

## Dynamic next work

1. Finish the diagnostic raw-value callback fix's focused validation.
2. Implement/qualify CH-C2: same guarded request-private learning plus corrected
   qsearch stand-pat. Freeze a new source/artifact/configuration before a second
   prospective32-pair W1 screen on the common development inputs against exact84.
3. Diagnose the integrated result with signal/cost evidence. Retention/context/effort
   changes remain contingent, not an automatic parameter sweep or F3 adoption.
4. Select a justified integrated finalist; W4 risk and one fresh W1 confirmation,
   with prospective protocols, remaining-budget checks and full audits.

Queue is revisable within CONTRACT, not a fixed exhaustive checklist. Current
hypothesis: bounded context learning can improve evaluation-informed selectivity;
not established. No bootstrap blocker identified; runtime qualification pending.

## Resource state and gates

Spent:32 W1 exploratory pairs/64 games;0 W1 confirmation pairs/games/attempts;
0 W4 pairs/games;1951.74s conservatively charged match wall (including setup/audit).
Remaining:96 W1 exploratory pairs/192 games;160 confirmation pairs/320 games;
32 W4 pairs/64 games;576 total games;41,248.26s match wall.
Technical evidence is detailed below; no competitive games have been run.
No promotion, default change, deployment, push or release authorized.
Next action: qualify the qsearch integration and diagnostic fix. No owner decision
currently blocks autonomous continuation; no independent confirmation has been spent.

## First formulation technical evidence

[FORMULATIONS](FORMULATIONS.md) defines CH-C1/`corrected`/flags238288895. Prototype
source and runtime manifests pin the uncommitted implementation atop296c0fd1; the
prototype's build85 label is inherited, not a claimed new application build.

`c1-tests-v1-summary.json`:215 selected JUnit tests passed, zero errors/failures/skips
after a focused test-harness correction. A new null-isolation test initially used
cumulative probe counters as per-invocation counts; the delta fix passed all7 new
tests. Failed logs/XML are retained. The stronger null-observation test confirms
discarded probes/verifiers preserve correction labels, score, PV and non-probe work.
Coverage includes core Search/rules, UCI, Construction, independent flattened main/
qsearch references, request learning, TT, verified-null/accounting, drivers and parallel
lifecycle. Project dependency boundaries passed. Full GUI/training/platform testing
was not run; no GUI/native integration or deployment claim.

`c1-diagnostic-v1-summary.json`:9 real-history roots/domain,18 requests/domain plus
18 clean W1 observation-neutrality repeats. W1 produced1727 eligible updates
(379upper/147interior/1201lower),54307 nonzero working changes/244659 reads and1772
changed improving decisions; max164 units, no near-saturation. W4 produced1924
updates,45936 changed reads and1045 improving changes. These are mechanism facts,
not independent labels or strength evidence. W1 observed/unobserved outcomes match
exactly; W4 scheduling equality is not claimed. Boards/PVs/accounting/drain checks pass.

`c1-isolation-v1-summary.json`:52 UCI requests and4 stop/reset checks passed. Exact W1
frozen84 guided, new normal production and explicit guided controls match; candidate
W1/W4 legality, budgets and reuse pass. Prototype runtimes remain unchanged.
`c1-cost-v1-summary.json`:144 clean requests including36 warmups,37.44s wall total;
geometric mean candidate/control wall ratios1.0041 W1/1.0313 W4. Small short-request
measurements indicate no prohibitive observed overhead, not speed/strength gains.
Runner narrow-fallback/warning-supervision6-test suite passed in0.90s; no games.

Bootstrap challenge has found no blocking contradiction. Exact84, evaluator/JVM,
host, runner and independent controls are available. The correction asymmetry
(main consumers corrected, qsearch raw), request-lifetime and broad pawn context
remain explicit scientific uncertainties to diagnose from exploratory evidence.

Build handling: after the technical qualification above, direct build85?86 changed
only the integer token; exact baseline bytes and resulting tuple/bytes verified.
Major/minor/patch remain0.0.0. This identifies attributable opt-in engine code in the
same implementation commit, not a production-policy promotion. Version SHA256:
98fdc7d39a424ea86124f1e99930b8f7c926429328cea79e7996bd8b650d28d1. Committed runtime rebuild/freeze and
artifact equivalence checks are the next required step before games.
`c1-runtime-v1-summary.json` adds14 W1/W4 managed-clock/movetime/history/terminal/
post-stop-reset requests plus2 stop checks, passed in1.74s with closed processes.

## Committed artifact and first match admission

CH-C1 source **d51d147920bfe1e0f30caee47c90d7191cfbf52a/build86** is frozen in
`raw/runtime-build86-c1-v1`. `build86-c1-qualification.json` compares2114 JAR
entries against the prototype: only generated version metadata differs; all core
implementation bytes match. Four artifact preflight requests passed. The rebuilt
launcher then passed14 managed/history/terminal/reuse requests plus2 stops.
Normal production policy remains HL-G1; this build identifies opt-in experimental code.

`development-v1-openings.pgn` prospectively selects32 distinct legal16 roots from
20,127 unique hash-bucket0 positions, seed2026110301, no evaluator/outcome filtering.
Its tracked receipt pins input identity/history. Existing narrow input exception
applies; generated games remain in ignored raw/. Parsing consumed42.93s technical time.
No forbidden BOUNDARY2 study was opened.

**Admitted next:** `c1-screen-v1-protocol.json`,32 W1 pairs/64 games, exact84 guided
against86 corrected,10+0.1, one active game. Estimate2300s, hard runner cap3600s,
inside remaining43,200s. No score-based decision until completed audit. At this
recording the match has not started; before any restart inspect the exact runner
process and `raw/c1-screen-v1-w1/run.json`/execution log. Never relaunch into an
existing output path or infer completion from silence. Resource counters above
remain0 until actual execution is reconciled; this admission reserves64 exploratory games.

The initial admission-state append encountered a Windows default text-decoding
error after protocol creation; no game had started. It was corrected to explicit
UTF-8 and the attributable state text repaired. Frozen protocol/artifact/input
bytes remain unchanged. No runtime code changed or additional build increment.

Live admission observed: W1 screen launched through the qualified runner, Python
parent10424/worker5308 and owned Fastchess30676, execution tool session56125.
`raw/c1-screen-v1-w1/run.json` says running; first completed game exists. These are
observed handles, not guarantees of future liveness. Recheck processes/run status
before any action; wait for final independent audit and process drain. No partial
result was used for selection or a stopping decision. In-flight resources are
charged from the final run record (or actual partial output if interrupted).

Additional diagnostic reconciliation: each W4 private worker consumed and updated
correction; update counts941/400/307/276 and changed reads26206/6640/6544/6546.
The compact diagnostic summary now records these per-worker totals from the same
unchanged raw output. This does not establish parallel competitive strength.

## Pending diagnostic-only correction (screen remains frozen)

During read-only follow-up, found that deeper-futility SelectivityTrace.omission's
`raw` argument received constructionEval's now-corrected return. Stored raw values,
search decisions, completed scores and game telemetry remain correct. This optional
trace is disabled in games; the CH-C1 diagnostic implementation counts omissions
without consuming that argument, so recorded diagnostic counts/costs are unaffected.
The working source now passes the preserved rawEval value and names the local
pruning value working. A constant-evaluator/nonzero-correction trace regression is
added but **not yet executed**; defer compilation/testing until the match drains.
This source edit does not mutate the frozen runtime, protocol or match adapter.
Do not claim the pending fix tested or build a replacement for the in-flight match.

## CH-C1 first strength screen ? complete, adverse/inconclusive

`c1-screen-v1-w1-summary.json`:64 normal games/32 pairs,22W/16D/26L,46.875%.
Paired-normal95 interval35.08?58.67%; bounded95 interval22.87?70.88%.
All8780 request audits passed; no missing-score warnings; inputs stable; both engine
handles drained; OS inspection finds no Fastchess process. Total wall1951.74s,
runner wall1948.48s. Raw output remains `raw/c1-screen-v1-w1`. No partial result
informed a decision. The old live PIDs/session above are historical, not a restart target.

This does not establish a CH-C1 gain or a universal rejection of correction history.
The technically working main-only signal did not earn immediate confirmation.
Decision: investigate the concrete main/qsearch mismatch. Both inspected external
engines also correct stand-pat; SeedV6 C1 leaves the returned leaf evaluation raw
while shifting only selective decisions. CH-C2 will add that consumer with the same
bounded contexts, learning eligibility and lifetime. This is a hypothesis, not an
explanation proved by the score. Common-opening screens will be descriptive and
selection-biased; no isolated component or independent-strength inference follows.

Pending trace fix validated after screen drainage: all8 focused correction tests
passed (`raw/raw-trace-fix-tests-v1.txt`,16s build/test wall), including a constant
raw evaluator with deliberately nonzero working correction and reached omissions.
No historical runtime/evidence was replaced. CH-C2 now adds corrected ordinary
qsearch stand-pat; its prototype and expanded qualification are in progress.

CH-C2 selected qualification passed59 JUnit tests with zero failures/errors/skips,
including the expanded correction/null/isolation cases, in51s build/test wall.
`runtime-c2-prototype-v1-manifest.json` and `c2-source-v1-manifest.json` pin the
prototype. Sequential W1/W4 observation and clean-cost jobs are running via execution
session88656; no competitive match is active. Inspect the execution JSON/logs before
rerunning; each output path is CREATE_NEW. C2 remains uncommitted experimental source
at build86 pending final technical qualification and direct qualifying build handling.

C2 observation diagnostics passed:27 requests/domain plus27 clean W1 repeats.
W1 C2 made1732 eligible main updates, changed81,238 of849,931 qsearch reads,
and1893 improving decisions; max correction243, no near-saturation events. W4
also exercised corrected stand-pat. W1 observation remains neutral. The current
C1 profile reproduces all9 frozen-prototype root outcomes (nodes, depth/completion,
score and PV), validating that the raw trace repair leaves C1 policy unchanged.
See `c2-diagnostic-v1-summary.json`; these are mechanism facts, not strength.

C2 clean cost passed216 requests including54 warmups in57.55s total. Candidate/
guided geometric-mean wall ratio0.9681 W1/1.0332 W4; short-variable measurements,
not a speed claim. C2 UCI isolation passed65 requests plus6 stop/reset checks;
frozen84/default/explicit guided W1 controls match. Candidate managed-clock/history/
terminal/reset checks passed14 requests plus2 stops. All owned engines closed.
`c2-cost-v1-summary.json`, `c2-isolation-v1-summary.json`, `c2-runtime-v1-summary.json`
preserve evidence. No match or diagnostic process remains active.

C2 is ready for committed artifact qualification and a separate32-pair exploratory
W1 screen versus frozen84 on the common development inputs. No strength is assumed.
Direct version finalization verified only build86->87, with unchanged user fields
and all other bytes; no finalizer. The second implementation commit includes the
qsearch consumer and the diagnostic raw-value repair. Prior frozen C1 remains intact.
