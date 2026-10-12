# SR-005 Phase 2: iterative-deepening information reuse

Authorized 2026-10-13 local. **ACTIVE**; only SR-005 is authorized. This record
contains evidence and continuation under the active Contract/Frontier.

## Bootstrap and challenge

Clean HEAD `13004b1`, Contract P2-R011 / Frontier P2-F015, PENDING SR-005 and
no ACTIVE feature match the launch. The owner explicitly accepts SR-030 DEFERRED
and confirms BOTH predecessor mirrors replaced and synchronized. Historical
pending statements impose no outstanding predecessor action.

Mandatory accepted comparator: `app/build/sr022-p2-production-v5`, source
`2f08516d0f24c9a08946bff122dcf5f3a0c8667f`, app.jar
`0ca1af7b730040aa26828133539f5c2977ae69fb8cfbcdf233c16db969e6d2c8`.
Full-window PVS + SR-001 unpruned qsearch + SR-029 quiet LMR + SR-022 WINDOW.
ProductionSearch matches that accepted source; current research code includes
SR-023/030 controls, all off. Retained runtime source `41587d2` is not substituted
for the accepted comparator. [Bootstrap](sr005-phase2-2026-10-13/bootstrap.json)
verifies all 19 installation/checkpoint/archive hashes.

Freeze generation 32, lineage `235d0688-e060-44e3-8f1e-119c39bc8017`, checkpoint
`g000032-s001753088-5837c7f7192aa3cf9a265388f93649327455e804541398ae3ef6b69e09014304`,
network `5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7`.
Store `E:/SeedV6-Networks/BRN-Pair2/235d0688-e060-44e3-8f1e-119c39bc8017`;
quarter-residual score mapping remains fixed. Read the baseline and Phase 2
SR-001/029/006/022/023/030 reports and relevant frozen R045/F052 sections.

Phase 1 SR-005 (historical commit `18f5e00`) observed 90 PV candidates through
D6 and 60 more through D10: all matched legal TT moves. It used HCE/static leaves,
PVS, one generation and 64 MiB requested TT. No strength games were reported.
No separate raw SR-005 report/probe was found among retained repository research
or root build directories; the preserved canon and historical commit remain
the available detailed evidence. This limitation does not turn old redundancy
into a current result. Frozen archives remain byte-identical.

Current code materially changes that information relationship: every qsearch
entry increments conservative selective provenance, so affected ancestors and
roots do not save ordinary TT proof or its associated hash move. LMR/LMP add
suppression. The driver keeps only the completed result; it passes neither PV
nor root alternatives back to the next iteration. Main quiet history resets at
each invocation. Existing TT entries still supply legal hints and exact eligible
proof. Prior PV/root/history reuse may therefore add information without changing
TT architecture. Measure coverage and actual ordering, not merely storage.

Primary external references inspected 2026-10-13:
[Stockfish 17 search](https://github.com/official-stockfish/Stockfish/blob/sf_17/src/search.cpp)
retains root scores/PVs and stable root ordering;
[Ethereal search](https://github.com/AndyGrant/Ethereal/blob/master/src/search.c)
offers a separate iterative search composition. These motivate hypotheses, not
copied implementation, transferable constants, proof rules or strength evidence.

## Prospective internal work

1. Quantify completed-PV coverage versus existing TT hints. Compare root-only
   retention and legal exact-prefix PV hints; preserve legal TT precedence.
2. Compare retaining previously searched root alternatives, using bounded
   primitive state and explicitly heuristic scores/effort. PVS fail-low values
   are bounds, not exact rankings. Do not add extra full searches to rank them.
3. Test request-local quiet-history retention from completed iterations as a
   distinct reuse hypothesis; unchanged history indexing/update rules, no
   cross-request persistence or wholesale history architecture change.
4. Study interactions with actual first/global/quiet ordinals, LMR/LMP and PVS,
   finite-resource root choices, complete-request cost and independent references.

Only completed full-window iterations of the same root/history, immutable
evaluator/policy and request may seed reuse. Cancellation, end/new request and
owner changes invalidate it. A hint never supplies a score, PV publication,
deeper depth or TT bound. Exact proof rules and selective suppression stay intact.
No aspiration, IID/IIR, clock policy, TT storage or neighboring feature is added.

Reuse prior four-game concurrency calibration: observed Ryzen 5 5500, six physical
cores/twelve logical, same Java 21/Pair-2 workload and 256/512 MiB heaps with
ActiveProcessorCount=1. SR-029 calibration and completed SR-022/023/030 campaigns
support four concurrent games, one worker per engine. Verify idle resource state,
actual UCI/model/binary identities and A/A before comparisons. Do not overlap
clock games with builds/tests/benchmarks/training. Desktop contention remains a
limitation; reassess material candidate-dependent workload effects.

Initial development uses inexpensive mechanism diagnostics and 16 paired
openings per promoted formulation at 10+0.1, four games, three depth-four warmups,
64 MiB requested TT, no book/tablebase/ponder. Freeze exact samples/configurations
before results. Progressive investment follows joint cost/mechanism/strength
evidence. Screens are exploratory; finalists require fresh prospective independent
confirmation, explicit stopping/error control and positive lower-bound evidence.
No arbitrary minimum gain, automatic game quota or proxy-based adoption.

## Continuation and bookkeeping

Current continuation: semantic gates, real-model diagnostic study and all six
clean cost runs pass. Next freeze the tested runtime, verify default equivalence,
and declare fresh development screens before starting games. No clock sample
has started.
No feature disposition or production adoption yet. Current activation masters
are P2-R012/P2-F016. External handback requires both final mirrors and owner/GPT
reconciliation; internal work continues from repository masters.

Root journal absent, not created. Local pinned finalizer begin attempted before
mutation; prolonged initial capture stopped before reservation. Subsequent status
idle, active null, unfinished/blockers empty. **Non-blocking version warning**;
build 35 unchanged, no bump/finish/replacement/repair claimed. No push/deployment.

## Initial mechanisms and semantic evidence

All controls are explicit `seedv6.uci.reuseResearch`, production OFF:

| Control | Information consumed at the next completed depth |
| --- | --- |
| observe | Previous PV coverage only; ordering and history remain unchanged. |
| root | Previous root best move, after any legal TT move. |
| pv | Previous PV moves only on the identical real move prefix; after legal TT. |
| score | PV hints plus stable root-alternative ordering by previous fail-soft returns. |
| effort | PV hints plus stable root-alternative ordering by prior inclusive node effort. |
| history | Existing quiet-history table retained across completed adjacent iterations. |
| pvhistory | PV hints plus the same completed-iteration history retention. |

Root alternative returns can be fail-low bounds; they are explicitly heuristic
ranks, not exact scores. No extra searches produce them. Root best remains ahead
of alternative ranks. TT proof processing is unchanged and separate from hint
priority. Generated legal membership validates every consumed hint; following
the exact move prefix from an identical root/full history validates PV context.
The stored immutable history keys and all board/status words are checked at
each iteration boundary. Evaluator/policy are fixed by the private owner factory.
Narrow windows, skipped depths, changed roots/history and incomplete iterations
invalidate retention. End/new request and new game clear eligibility. Qsearch
does not consume hints, and nominal/real/q depths are unchanged.

Only the existing ordering keys change; actual searched indices and quiet counts
still drive LMR/LMP, and prior scores never supply alpha, beta or proof. A quiet
hint may force early quiet generation and therefore earlier history sampling;
this is part of the measured mechanism/cost, not an assumed free lookup. No
per-node heap allocation is introduced. Root/PV/rank scratch is bounded primitive
owner state. History retains the existing table without adding another table.

Accepted frozen comparator actual-default UCI preflight passes budgets, stop,
terminal, deterministic depth and identity checks. Initial eight focused tests
pass; broad Search/UCI/rules finds 746 tests, **735 passed, 11 historical disabled,
zero failures/errors**. Thirteen Python adapter tests pass. Focused checks cover
observation neutrality, unpruned value agreement, legal PV, request isolation,
aborts, skipped depths/windows/history, terminal/mate/clock boundaries, TT proof
suppression and legal-TT precedence. Additional independent enumeration and UCI
policy gates are being validated before the source freeze.

The real-model diagnostic study completes 624 observations across 26 full-history
roots, eight controls and D4/D6/100k-node resources. Observation-only results
match OFF in score, best move, PV, total nodes, completed depth and selectivity.
Across completed observed iterations there are 1,441 candidate-bearing PV nodes:
144 same TT move, 1,292 absent TT moves, five different TT moves. This directly
refutes transferring Phase 1 redundancy to this cumulative selective baseline.
Counts include repeated requests/modes and are not independent population rates.

| Control | D6 nodes / OFF | Changed D6 best moves (26) | Mean completed depth at 100k |
| --- | ---: | ---: | ---: |
| OFF / observe | 1.0000 | 0 | 7.115 |
| root | .9013 | 8 | 7.731 |
| pv | .7828 | 9 | 8.962 |
| score | .7461 | 9 | 9.000 |
| effort | .7652 | 4 | 8.923 |
| history | .9801 | 6 | 7.462 |
| pvhistory | .7891 | 7 | 8.962 |

These are mechanism/resource observations, not strength. Selective ordering
changes root decisions; greater nominal depth is not presumed more accurate.
The diagnostic run overlapped the broad regression worker, so its instrumented
wall times are excluded from clean cost conclusions. An early results copy
occurred before broad completion; `broad-v1-summary.json` and the copied XML were
subsequently replaced with the complete, verified final 746-test results.
Clean rotated no-callback cost/allocation runs follow after regressions finish.
Raw study, summary, XML and commands are in the linked evidence directory.

### Clean costs and progression

All six separate rotated comparisons complete 312 requests each: 26 roots,
two rounds, two policies, D6/100k/1M resources. Clean D6 elapsed ratios for
root/pv/score/effort/history/pvhistory are **.912/.794/.770/.773/.945/.804**.
At one million nodes they are **.980/.900/.900/.899/.977/.914**. Q/main mix,
completed depth and early terminal exhaustion affect these aggregate ratios;
they are not universal NPS or exact speedup estimates. No callback runs inside
these timed searches. Allocation, compilation and GC totals are retained in
`cost-v1-summary.json`; initial/JIT effects remain observable. Higher completed
depth can allocate more per-request result snapshots despite bounded reuse state.

All six materially distinct controls merit the inexpensive initial clock screen:
root is a causal scope ablation, PV directly restores missing information,
score/effort test alternative evidence, history tests discarded internal learning,
and pvhistory tests the interaction. No superiority ranking or adoption follows
from the cost ordering. The additional 17 focused tests (including independent
enumeration and UCI gates) and all 14 adapter tests pass. An initial opening
selection attempt included a noncanonical selection receipt lacking positionKeys;
it failed before creating a sample or running a game. The corrected selector
uses only canonical full-position selection receipts and retains exclusions.
Automatic approval review rejected removing generated binary test logs with
only "blocked by policy" supplied as the reason. These harmless task artifacts
are retained with the XML; they do not obstruct research or require cleanup.
