# SR-030 Phase 2: check extensions

Authorized 2026-10-12 local. **ACTIVE**, SR-030 only. This is research evidence
under the existing Contract/Frontier, not a separate governance structure.

## Baseline and challenge

Clean HEAD `f4927c05d2d5eb55490a18276fdaf20804fa7800`, P2-R008/P2-F012.
The owner explicitly confirms SR-023 technical reconciliation, DEFERRED
acceptance and replacement/synchronization of BOTH predecessor mirrors.
No predecessor human action remains. Its completed confirmation stays closed.

Mandatory comparator: `app/build/sr022-p2-production-v5`, source
`2f08516d0f24c9a08946bff122dcf5f3a0c8667f`, app.jar
`0ca1af7b730040aa26828133539f5c2977ae69fb8cfbcdf233c16db969e6d2c8`.
All 19 frozen installation/checkpoint/archive hashes match, as recorded in
[bootstrap.json](sr030-phase2-2026-10-12/bootstrap.json). ProductionSearch is
unchanged from that source. Single-worker full-window PVS uses SR-001 unpruned
qsearch, SR-029 protected quiet LMR and SR-022 WINDOW; aspiration/history pruning
remain off. Actual UCI preflight and default equivalence precede comparisons.

Freeze generation 32, lineage `235d0688-e060-44e3-8f1e-119c39bc8017`, checkpoint
`g000032-s001753088-5837c7f7192aa3cf9a265388f93649327455e804541398ae3ef6b69e09014304`,
network `5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7`,
quarter-residual mapping. Store: `E:/SeedV6-Networks/BRN-Pair2/235d0688-e060-44e3-8f1e-119c39bc8017`.

Checked nominal parents and checking moves are already protected from LMR/LMP.
Qsearch searches all evasions, captures, EP and promotions, but no quiet checks.
Consequently the Phase 1 last-ply giving-check +1 does not necessarily add a
new move domain: its checked child already receives all evasions in qsearch.
It can still change PVS/order/history and main/q accounting. Extending an
evasion at positive depth can expose a full quiet reply otherwise absent at
the leaf. Test that distinction rather than importing static-leaf conclusions.

Read frozen R045/F052 SR-030, Phase 2 SR-001/029/006/022/023 and the historical
`C:/Users/Central/Documents/SeedV6-SR030-2026-10-01/REPORT.txt`. Retain the
promotion/draw witness and adverse faster-mate witness; no old Elo experiment
exists to repeat. Credit changes value applicability: a consumed-credit ordinary
proof cannot answer an unused-credit invocation. No new TT namespace is assumed.

Primary implementation research consulted on 2026-10-12:
[Ethereal search](https://github.com/AndyGrant/Ethereal/blob/master/src/search.c)
extends checked positions in its own composition;
[Stockfish 15](https://github.com/official-stockfish/Stockfish/blob/sf_15/src/search.cpp)
used deep checking-edge eligibility, and the later
[Stockfish removal](https://github.com/official-stockfish/Stockfish/commit/96837bc4396d205536cdaabfc17e4885a48b0588)
passed its simplification tests. These motivate competing hypotheses, not
transferable strength, score thresholds, TT semantics or copied source.

## Prospective investigation

Start with semantic overlap/oracle checks and distinct bounded mechanisms:
last-ply checking-edge overlap control; general checking-edge extension;
checked-node/evasion extension; and checked positions with exactly one legal
evasion. The last stays within check extensions, not general forced replies.
Choose minimal integer credit accounting, with identical edge depth/credit on
scout and full retries. Nominal requested depth stays unchanged; actual added
plies and qplies stay separate. Resolve supported real-ply exhaustion honestly.
Preserve independent static/qsearch and LMR/LMP-off controls, and conservatively
exclude incompatible ordinary TT reads and extension-dependent stores.

Before candidate outcomes, development screens are fixed at 16 fresh partition-0
pairs per materially distinct candidate, 10+0.1, four simultaneous independent
games, one worker/engine, Java 21 with ActiveProcessorCount=1 and 256/512 MiB
heap, three depth-four warmups, 64 MiB requested TT, no book/tablebase/ponder.
Complete each declared screen except integrity failure. No build, benchmark or
training overlaps clock games. Small screens explain mechanisms, not adoption.
Follow-up uses mechanism, tactical and cost evidence jointly; no arbitrary sweep.

Promising finalists require a frozen prospective independent partition-1 design
before any confirmation outcomes: executable/model/options/openings identities,
sample size or error-controlled looks, paired uncertainty, wider-SE and bounded
Hoeffding sensitivity, legal/pairing/failure/accounting audits and explicit
positive versus inconclusive rules. Never pool development or extend a finished
confirmation post hoc. No arbitrary minimum Elo; no proxy-based adoption.

## Continuation and bookkeeping

Next: actual comparator preflight; focused prototypes and independent semantic
checks; fixed-depth/node/cost diagnostics; development; evidence-led follow-up
and independent confirmation if justified. Remain inside SR-030 until a
defensible feature disposition or genuine external blocker.

Root journal absent; none created. Pinned local version entrypoint invoked
before mutation. Its prolonged initial capture was stopped before reservation;
subsequent status is idle, no token/unfinished/blockers. Non-blocking SeedV6
version warning, build 35 unchanged, no verified bump/replacement or repair.

Final owner/GPT reconciliation and BOTH final master mirror replacements with
explicit synchronization will gate dependent GPT planning. Activation itself
does not claim external acceptance, synchronization of new revisions or deployment.
