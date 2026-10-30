# Foundational TT information architecture - qualification assessment

Objective [TTF-R001](CONTRACT.md), under Search Contract P4-R003. Assessment dated
30 October 2026. **Accepted limited foundational technical qualification.
TTF-R001 technical execution and owner-acceptance closeout are complete.**
The governing A-G standard has not changed. This is an experimental foundation,
not an accepted champion, production selection or claim of stronger play.

## Result and intended use

**TTF-F3-v1: packed coherent main-node information** makes raw static evaluation
available alongside searched scores and completed move guidance, with separate
meaning and eligibility. Existing improving, futility and verified-null admission
consumers use it through the main-node evaluation path. It avoids repeated NNUE
inference while preserving the tested W1 Search work and results. A retention-only
ablation demonstrates that availability alone does not deliver that consequence.

The intended limited research purpose is a tested starting point for selective
Search that needs truthful co-retained raw evaluation, heuristic searched scores
and move guidance. The connection to practical strength is computational: avoiding
redundant evaluation may leave capacity for more useful Search or dependent
mechanisms. Neither a reliable elapsed-time improvement nor stronger play has been
established. No hypothetical correction, extension or pruning consumer is qualified
by this work, and no successor objective is launched.

The accepted qualification applies to the explicit F3/H2-integrated profile and stated domains,
not every feature combination that the low-level configuration API can express.
H2 is a reproducible causal control and dependency here; this does not retroactively
accept the historical H2 campaign or establish H2 as the best possible foundation.

## Mechanism and boundaries

The initial limitation was concrete: `constructionEval` reused a value only within
one live node; subsequent iterations/windows recomputed it. The legacy `TYPE_EVAL`
table representation could not coexist safely with searched information. H1/H2
provided selective score provenance/retention and completed hints, but no such raw
evaluation service. Production's extension configuration still suppresses ordinary
TT score reuse; this change does not remove that safeguard.

The selected layout packs signed 16-bit ordinary raw evaluation, an 8-bit request
epoch and explicit validity into the unused high half of the existing move word.
All generated unsigned 32-bit move metadata is preserved. Full SearchKey identity
and the existing striped locks remain; no compressed identity or racy publication
was imported. One locked snapshot exposes distinct raw, ordinary-score,
heuristic-score and completed-hint fields. Existing H2 score age/depth and hint
freshness rules remain separate from current-request raw validity.

Evaluation-only admission cannot evict a score or hint, even an old one. Same-key
updates preserve raw information; collisions invalidate it. Raw writes do not
refresh score/hint lifetime. Zero is valid, mate-domain values are rejected, and
generation wrap clears the packed lane. The evaluator definition must be fixed
within a request; raw values are not reused across requests. Synthetic null
searches and legal null verifiers are excluded. Accumulator transition callbacks,
history, request accounting and cancellation remain active on cache hits.

The main kernel materializes a cached value only when a real static-evaluation
consumer asks for it. Availability at an earlier score cutoff is not counted as
consumption. A successful heuristic score write can also avoid a duplicate
completed-hint write because it already applies that qualified update; rejected
score/mate paths and legacy hint diagnostics keep their fallback. There is no
qsearch cache, new null-admission rule or changed production selector.

The opt-in table rejects unsupported high-bit moves and legacy `TYPE_EVAL` writes
before mutation. Legacy tables retain their full-long move API. These are explicit
representation boundaries, not a general-purpose information framework.

## Acceptance evidence

| Requirement | Evidence and scope |
| --- | --- |
| A. Real limitation | Source inspection and baseline diagnostics establish repeated raw inference and the incompatible legacy evaluation-only representation. [External investigation](EXTERNAL.md) examines pinned Stockfish 19, Ethereal 14 and Bagatur 5.5c information meanings and actual consumers. No independent external mechanism Elo claim was verified or transferred. |
| B. Useful truthful information | Packed storage tests cover zero, ordinary/mate domains, coherent misses, same-key preservation, collisions, hint/score/raw lifetime separation, generation wrap, low32 move integrity and concurrent collisions. The real CP64 diagnostics exercise production and exposure under the selected selective configuration. |
| C. Consequential consumer | Existing main-node improving/futility/verified-null static consumers materialize the retained raw value. The reverse ablation keeps the new layout, snapshot and production but hides raw consumption; it retains availability and avoids no evaluator calls. |
| D. Search consequence | [54 diagnostic requests](diagnostic-v3-summary.json): W1 avoided 14,301 of 337,396 evaluator calls (4.24%), including 5,546 calls across 27 repeated-window retries. Nodes, scores, PVs and aspiration windows matched H2. [624 persistent-history requests](lifetime-v1-summary.json): W1 avoided 490,175 of 9,304,481 calls (5.27%) across 104 roots, with identical nodes/depth/score/PV. Retention-only had 637,921 available values and zero avoidance. This measures actual inference removed, not merely hit counters; it does not demonstrate faster Search. |
| E. Costs and trade-offs | [216 clean-cost requests](cost-v3-summary.json), including warmups: geometric paired wall ratios 0.9859 W1 / 0.9914 W4; CPU ratios 1.0071 / 0.9759 versus H2. These short single-host results show no material observed overhead in this domain, not a proven speed gain. [Allocation/reset](allocation-final-v1-summary.json): identical table capacity, payload and constructor bytes; median clear cost rises from 0.668 to 2.331 ms. Both game domains passed their operational audits; process costs and adverse competitive evidence are characterized below. |
| F. Correctness and operation | [Final 323 tests](final-tests-v1-summary.json): 206 engine-core, 26 UCI and 91 selected research tests; zero failures/errors/skips. Focused additions cover independent full-rebuild evaluator agreement, real history/repetition/terminals, synthetic-null/verifier exclusion, score validation, and W1-16 budget/deadline/drain/reset/restart. [104 launcher/control requests plus two stop requests](isolation-v1-retry1-summary.json) preserve frozen build80/H1/H2 W1 routes and H2/foundation W1 results; W4 accounting/PV/lifecycle passes. All 32 games and 4,723 request traces passed the game/trace/accounting/input/drain gates, with zero missing-score warnings. |
| G. Reproducibility | Implementation commit `32feb9078d0aa53a26e6c23bfb772f815e501e7f`, build83, explicit foundation launcher and flags32767999. [Runtime manifest](runtime-f3-build83-v1-manifest.json), prospective protocols, source/runtime/evaluator hashes and retained local raw evidence identify each formulation. [Prototype-to-final](prototype-final-equivalence.json) and [build83 artifact comparison](build83-artifact-equivalence.json) connect the measurements and final tested artifact. |

W4 consumed 500,026 raw values in the persistent-history study. Its scheduling
changes the searched trees, so cross-profile total differences are not treated as
a deterministic causal equality or a clean speed measurement. Instrumented
diagnostic timings are never used as clean cost evidence.

The avoided CP64 calls execute `ConventionalAccumulator.evaluate` and
`ConventionalNetwork.score` (observed backend `vector-256-long`). Thus the measured
consequence removes output-head evaluation and score mapping, while accumulator
preparation/transition work remains. A 5.27% reduction in these calls is not a
5.27% reduction in total Search work or CPU time. The already-efficient head and
additional cache traffic explain why call avoidance requires separate cost evidence.

Causal and clean-cost studies compare the H2, foundation and retention-only modes
inside the same frozen prototype runtime. They isolate the selected formulation;
they are not a clean benchmark of every shared-core change versus historical build82
or of production latency versus build80. Launcher isolation compares historical
runtimes directly for semantics, and the game screen uses the actual frozen H2
artifact. Across the 27 measured root/round pairs per domain, individual foundation
wall ratios range from 0.811 to 1.120 in W1 and 0.868 to 1.072 in W4, illustrating
why the near-neutral geometric means are not a speedup claim.

The final suite repeats the earlier 323-test qualification after the defensive
legacy-write check; these are 323 distinct tests, not 646 distinct tests. Existing
independent recursive/exhaustive/TT-off research references were preserved. The
full-rebuild evaluator comparison also verifies unchanged transition callbacks.
No complete unrelated GUI/training/native-platform repository test matrix ran.

## Resource and game evidence

At requested Hash64 both H2 and the foundation have 2,097,152 entries and
50,331,648 bytes (48 MiB) of entry payload. Measured table constructor allocation is
50,335,976 bytes in both modes; approximate retained heap delta is 53,481,656 bytes
in both after the first sample. These are different measurements: JVM heap-region
rounding is not entry payload. The foundation additionally has one 257-element
integer scratch array per worker (about 1 KiB plus array overhead).

The higher clear cost is caused by clearing the move/raw array as well as the
score array. Clear runs only with drained workers, at new-game reset or generation
wrap. The 32 measured empty-table clear samples per layout are a characterization,
not a universal latency bound. Actual process peaks include evaluator, Search,
JVM, workers and allocation/GC behavior and must not be inferred from Hash64.

**Both domains completed.** The fixed [protocol](screen-v1-protocol.json)
uses eight opening pairs per domain, W1 OWNER then W4 ROOT_REUSE, 10+0.1,
one active game, frozen H2/build82 versus foundation/build83, no book/tablebase,
and the unchanged BOUNDARY2 clock. The first eight existing exploratory bridge
opening inputs are reused in original order, without outcome filtering. No old
game results are pooled and no frozen BOUNDARY2 study is accessed.

W1 completed all 16 games normally in 489.76 seconds. The candidate scored
5 wins, 5 draws and 6 losses (46.875%); the paired bounded95 score interval is
0-94.891%. All 2,199 request traces passed, with zero missing-score warnings,
legal game/PV and full accounting checks, stable inputs and drained process handles.
This slightly adverse point estimate is not hidden or reinterpreted as equivalence.
Fixed-work W1 comparisons remain identical, clean-cost ratios near neutral, and
this tiny timed sample cannot distinguish a systematic effect from variation.
No extra games are purchased to improve its result or precision.

W1 engine lifetime CPU was 261.422 seconds H2 versus 261.938 foundation (ratio1.0020).
Peak working sets were 354,320,384 versus 352,100,352 bytes; Windows
PeakPagefileUsage counters were 430,620,672 versus 426,082,304 bytes. These single-process
lifetime peaks are descriptive and include different timed game trajectories,
JIT/GC and evaluator state; they do not establish a memory optimization or speed gain.

W4 completed all 16 games normally in 528.56 seconds. The candidate scored
3 wins, 5 draws and 8 losses (34.375%); its paired bounded95 score interval is
0-82.391%. All 2,524 request traces passed, with zero missing-score warnings,
stable inputs and drained handles. The adverse point estimate is a material
competitive warning; this is not a neutral or successful strength screen.

W4 engine lifetime CPU was 602.875 seconds H2 versus 586.719 foundation
(ratio0.9732). Peak working sets were 389,910,528 versus 393,244,672 bytes;
Windows PeakPagefileUsage counters were 427,433,984 versus 430,985,216 bytes.
Whole-request work was 309,650,165 versus 301,280,922 nodes, so lifetime CPU per
searched node was nearly identical. Different trajectories prevent causal speed
or quality inference from these aggregates.

The negative signal triggered a [post-screen trace investigation](screen-v1-trace-analysis.json),
not additional games or outcome-dependent tuning. All request totals match the
independent PGN accounting. Neither actor had a request exceeding its available
clock or a zero-depth completed response. W4 median completed depth was9 for both;
both had137 TIME_LIMIT terminations. Completed-iteration nodes were89.45% versus
89.39% of whole-request nodes. Candidate minimum available clock was255ms versus
150ms for H2. Thus the inspected evidence exposes no clock/accounting/fallback
failure or broad effort/depth collapse that supplies a concrete repair. It does
not explain away the losses: a competitive penalty, including a parallel Search
interaction, remains possible. No game-level quality cause is established.

The eight paired units per domain support only a coarse exploratory screen. The
bounded Hoeffding95 score half-width is about 48 percentage points before clipping;
the analyzer omits a normal interval for n<30. There is no independent confirmation,
equivalence, adjusted discovery or champion79-relative claim. Sample size is fixed
and cannot expand because a result is attractive or inconclusive.

## Alternatives and negative evidence

[FORMULATIONS](FORMULATIONS.md) preserves the engineering progression and exact
frozen artifacts. TTF-F1-v1 added a separate raw lane with main/qsearch traffic and
an optional searched-bound veto of speculative null admission. It avoided 20.72%
of W1 evaluator calls but clean wall costs rose about 10.55% W1 and 7.86% W4.
The null guard vetoed 23 attempts but increased total nodes by 170 (0.047%). Neither
was justified as the final foundation; both implementations and outputs remain.

TTF-F2-v1 used one coherent main-node snapshot and removed qsearch/guard traffic.
It avoided the same 14,301 diagnostic calls as F3, with near-neutral noisy timing,
but increased table constructor allocation by 8,388,624 bytes. F3 preserves that
useful information flow without the extra table array. F2 is an intermediate
diagnostic formulation, not independently accepted or declared inferior in Elo.

Retained setup failures include the first prototype harness compile (its aspiration
constructors needed concrete owner types), the initial V2 wrapper's Python
manifest-preparation syntax error/missing runtime, and the initial isolation
invocation's system Python without `chess`. Their logs remain; corrected builds
and separately named retries completed before evidence was accepted. No engine
change was needed for the constructor-typing correction. The V2/isolation retries
used their frozen protocols and existing research environment.
An artifact-equivalence assertion also initially named the wrong
version-resource path; the actual resource was inspected and the unchanged
content-equivalence requirement passed. No failed run was relabelled as successful.

## Reproduction and continuation

Start at [STATE](STATE.md). Raw output is intentionally ignored and local-only;
a fresh checkout contains protocols/manifests/summaries, not the runtime jars,
source snapshots, XML, JSONL, traces or games. Check availability and hashes before
reuse. Preserve completed evidence; use new output names for new authorized runs.

The runtime is frozen under `raw/runtime-f3-build83-v1`. The explicit research
entry point is `com.ohinteractive.seedv6.integration.P4TtFoundationUciMain --network
<CP64 path>`, with `classes` and `lib/*` on the classpath. Use the exact JVM, clock,
Hash and worker settings in the protocol/manifest. The default UCI entry point
still selects flags3407871; H1 is7602175 and H2 is15990783. The candidate adds
TT_INFORMATION to H2, flags32767999, with normal margins100/200 and extensionCap1.
CP64 is S6CNU002/eight heads/BT4_Q_LOGIT400_V1, 3,940,952 bytes, SHA256
`8431626c2fca81e0a1e9ccd5e45ddd8829e6e3282c756c21fad75b06d075bb2b`.

`tools/p4-tt-foundation.py --protocol <new-protocol.json> --workers 1|4` uses the
existing qualified owned-process runner and full trace audit. The research Python
environment is `build/research/venv/Scripts/python.exe` (Python3.13.4, chess1.11.2).
Do not rerun the frozen screen under its completed names. Diagnostic and lifetime
main classes, arguments, corpora, limits and raw paths are pinned in their protocols;
`P4TtFoundationStudy` supports the explicit `h2`, `foundation` and `retain` profiles.
The selected Gradle test command is recorded in `raw/final-tests-v1/gradle.log`:

```powershell
$env:GRADLE_USER_HOME = Join-Path (Get-Location) 'build/migration/gradle-user-home'
.\gradlew.bat :engine-core:test :app-uci:test :research:test `
  --tests '*ConstructionSearchTest' --tests '*Cp64FlatNullTest' `
  --tests '*ExactSearchTTableTest' --tests '*FlatExactSearchTest' `
  --tests '*MateDistancePruningTest' --tests '*P4TtAgedMemoryTest' `
  --tests '*P4TtMemoryTest' --tests '*P4TtInformationTest' `
  --tests '*VerifiedNullAccountingTest' --tests '*TranspositionTableConcurrencyTest' `
  --tests '*TranspositionTableTest' --tests '*TtExactSearchTest' `
  installResearchWorkspace --console=plain
```

The final build83 artifact differs from the tested final prototype only in generated
version metadata across 2,128 compared members/files. Between the measured F3
prototype and final source, the only core change rejects unsupported legacy eval
writes; measured producer/consumer paths never invoke that API. Final focused and
broader tests, launcher isolation and committed-artifact game preflights cover it.

## Residual limits and human gates

No standalone speed gain, Elo improvement, H2 equivalence, tactical superiority,
cross-request raw-cache benefit or general parallel cooperation gain is claimed.
Hash sizes/platforms/evaluators/time controls beyond the stated domains, sustained
production reliability and future information-dependent mechanisms need their own
evidence. Cross-worker scheduling and single-host short-window noise limit cost
inference. The strict request lifetime and history-rich key are conservative and
may forgo useful reuse; changing them needs a separately supported formulation.

Build79/commit753367614b8aa7e32dee337fd76b542737141ac1 remains the accepted champion.
Build80/commit0bfb4ea8422ef78443e309930166ad46df5cc454 is the accounting-corrected
technical control, not assumed competitively neutral. H1/build81 and H2/build82
retain their historical experimental status and separate pending reconciliation.
No prior protocol, result, claim allowance or conclusion changes here.

## Disposition and authority

**TTF-R001 is accepted and closed as a successfully completed, limited foundational
engineering objective.** In the 30 October 2026 closeout authorization, the owner
confirmed GPT's technical reconciliation and acceptance of the completed evidence
against the original A-G requirements, explicitly accepted the limited qualification,
and authorized this documentation closeout. The accepted formulation is
**TTF-F3-v1 - Packed coherent main-node information**, implementation
`32feb9078d0aa53a26e6c23bfb772f815e501e7f`, 0.0.0/build83, flags32767999.
The original evidence, costs, adverse findings and A-G requirements are unchanged.

The specific enabling benefit is actual repeated raw-evaluation work
removed by existing selective consumers, with preserved deterministic W1 results,
truthful independent information lifetimes, unchanged table capacity/payload,
near-neutral observed request costs and passed operational checks. The measured
reset cost and small worker scratch addition are proportionate to this limited
research use. This is a judgment about the agreed foundational purpose, not a
statistical performance finding or a relaxation of the original acceptance test.

The adverse W4 game signal narrows the practical recommendation: this artifact is
not endorsed as a competitively stronger or neutral replacement, especially in W4.
Its actual information capability and causal W1 work avoidance support a limited
foundation qualification despite unresolved strength. The trace investigation found no
supported cheap repair; buying enough additional strength precision is not required
to establish A-D and is not automatically launched. Competitive use needs a new
explicitly scoped decision with this negative evidence visible. Future options
include targeted parallel-interaction diagnosis or a newly authorized integrated
consumer/strength objective, not blind tuning or replay of historical samples.

GPT technical reconciliation and explicit owner acceptance are complete. No successor
objective is active or authorized; future selection requires separate GPT/owner
coordination and authorization, preserving the exact configuration, relevant H2
dependency/control, operational qualifications and unresolved competitive risks.
This acceptance is neither champion advancement nor production promotion.

The owner confirms the **P4-R003/P4-F015** Project mirrors were refreshed before
closeout. The repository pair is now **P4-R003/P4-F016**. Refresh the separate
Frontier Project Source to P4-F016 before mirror-dependent planning; this is
non-blocking for completed repository closeout and was not done automatically.
This documentation-only acceptance record ran no new research or technical
qualification and leaves build83 unchanged. No default change, push, release or
deployment occurred.

The implementation commit is `32feb9078d0aa53a26e6c23bfb772f815e501e7f` (build82 to83,
only the build integer changed). Bootstrap is `7754ebd59e38fd50e93dbf7e021999d78bbead89`;
prospective screen freeze is `e2132531` and the final report commit is discoverable
from Git history. Final documentation/analysis does not warrant another bump.
All changes are attributable to this objective; the starting tree was clean.
Raw evidence remains ignored at its recorded paths; no push or destructive Git
operation occurred. Root CODEXLOG_CURRENT.md was absent and was not created.
[Evidence integrity](evidence-integrity.json) records the relevant availability and
hash checks. [STATE](STATE.md) remains the unique terminal resume point: TTF-R001 is
accepted and closed; do not restart completed work or begin an unauthorized successor.
