# CH-R001 resume state

**ACTIVE â€” CH-C1 technically qualified prototype, 3 November2026.**
Read [CONTRACT](CONTRACT.md) first. CH-C1 is experimental; no strength result yet.
No match running. Normal production policy and research champion HL-G1/build84
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
games. Prior HL complete match costs (~30â€“32s/game) suggest640 games ~5.7h, leaving
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

1. Complete final bounded managed-clock/history smoke checks, coherent source commit
   and committed runtime freeze; verify identical tested classes except version metadata.
2. Freeze32-pair first W1 exploratory comparison against exact84, after new development
   input selection and prospective resource/audit checks. CH-C1 corrects main selective
   decisions using request/worker-private pawn feedback; raw stand-pat remains unchanged.
3. Follow evidence into qsearch/effort coupling or lifetime/context adjustment where
   useful. No blind sweep or automatic F3 adoption. Freeze qualified artifact before
   exploratory paired games against exact84.
4. Select a justified integrated finalist; W4 risk and one fresh W1 confirmation,
   with prospective protocols, remaining-budget checks and full audits.

Queue is revisable within CONTRACT, not a fixed exhaustive checklist. Current
hypothesis: bounded context learning can improve evaluation-informed selectivity;
not established. No bootstrap blocker identified; runtime qualification pending.

## Resource state and gates

Spent:0 W1 exploratory pairs/games;0 W1 confirmation pairs/games/attempts;
0 W4 pairs/games;0 match seconds. Remaining:128/160/32 pairs,640 games,43,200s.
Technical evidence is detailed below; no competitive games have been run.
No promotion, default change, deployment, push or release authorized.
Next action: artifact finalization and the first frozen W1 development experiment.

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
