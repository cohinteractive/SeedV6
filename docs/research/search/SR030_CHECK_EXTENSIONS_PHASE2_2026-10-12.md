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

## Initial implementation and semantic gates

Four explicit research policies (all production defaults stay OFF):

| Name | Exact rule | Per-path credit |
| --- | --- | --- |
| boundary | Giving-check edge at parent depth 1, child depth 1 instead of 0 | one +1 |
| giving | Giving-check edge at parent depth >1, avoiding last-ply qsearch overlap | two +1 |
| evasion | Every evasion edge from a checked positive-depth node, including a checked root | two +1 |
| forced | As evasion, but only when that checked node has exactly one legal evasion | two +1 |

The small credit bounds provide an explicit termination/work envelope, not tuned
constants or a claim that longer forcing lines are unimportant. Every qsearch
still follows its unchanged unpruned policy; unused credit cannot restart there.
PVS scouts and full retries use the same frozen child depth and used-credit slot.
Checking/evasion eligibility inherits LMR/LMP protection; no omission/reduction
policy is silently changed. An extended edge increments conservative selective
provenance. Ordinary TT cutoffs are suppressed while credit remains, and stores
from affected ancestors remain suppressed; consumed-credit unaffected actual-
depth proof remains ordinary. No policy TT or qTT is added.

Added main plies are a separate result statistic; requested nominal depth and
maximum qply are unchanged in meaning. The supported final real-ply slot resolves
terminal/draw positions but aborts an unresolved positive-depth continuation.
Prepared transitions, including extensions and aborted work, retain actual main/q
classification and whole-request node/evaluator accounting; retries do not admit
the same child twice.

The first focused 20 tests passed after correcting one test accessor and strengthening
fixtures that initially did not reach a second forced evasion. The repeated-check
fixture now exercises exhausted two-credit state. No engine correction was needed
for those test construction failures. Independent full legal minimax/qsearch,
original-window bounds, ordinary-TT incompatibility, legal PV, real-ply mate/clock,
EP, twofold/threefold, cancellation/reuse, PVS retries, actual accounting and UCI
isolation are covered. Boundary-only values equal unpruned qsearch on the compact
witness set, supporting the overlap explanation without assuming equal tree costs.

The broad Search/UCI/rules run passed **725 tests, 11 existing skips, zero failures**
(736 discovered). Thirteen Python adapter tests pass. XML and summaries are retained.
The first 26-root/390-request fixed-depth and fixed-node study passed accounting
and legal-PV checks. D6 admitted-node ratios to cumulative baseline are boundary
0.975, giving 1.557, evasion 1.457, forced 1.004. These are diagnostic trees,
not clean timings or strength. Distinct extension placement has material cost;
forced checks concentrate added effort. All four remain in the declared screen.

Frozen v1 source is `d9578bbe63c4` (full identity in candidate-v1.json), app.jar
`03087d3539a456853f3dbecb2feb8b10a7480a38241aa8928a44cbb76d8463f7`.
All 704 compiled main classes match the installation. Its embedded dirty suffix
reflects the newly created build-console evidence file, not an uncommitted runtime
change; source/installation hashes preserve that distinction. Actual-default
bridge passed 64 deterministic score/PV/node/termination comparisons against
accepted v5. No strength result is inferred from equivalence.

The local remaining-depth argument includes prior granted extension credit;
root requested depth remains nominal, and real ply indexes history/evaluator/mate
state. Existing LMR/LMP eligibility sees this actual remaining main depth with
unchanged formulas and protections. This can change downstream selective trees,
so extended selective scores are not exact ordinary nominal values.

Historical witness replay completed all 74 bounded observations. With Pair-2 and
qsearch, the old adverse root changes direction: at D4 giving/evasion chooses
Kd7 over baseline Kf7; independent unpruned D5/D6 child-root checks favor Kd7,
while Kf7 is a forced loss by D6. At D6 changed choices are equally lost by the
checked deeper horizons. The promotion/draw root already keeps Kc6 in the accepted
qsearch baseline; giving/evasion discovers its mate at nominal D6, earlier than
the baseline. These are local horizon benefits, not general strength proof or
inheritance of Phase 1's static-leaf adverse disposition.

Clean same-JVM cost experiments completed 1,248 requests (26 roots, two rotated
rounds, D6/100k/1M nodes, both sides, four policies). D6 wall ratios were boundary
1.045, giving 1.521, evasion 1.409, forced 0.945; respective 1M-node ratios were
1.021, 1.034, 0.992, 0.996. Nominal depth was lower on 20/24/6 of 52 1M-node
requests for giving/evasion/forced, with 2/2/0 deeper. Boundary split 6 deeper/
6 shallower. All node/accounting/legality and declared limits passed. Allocations,
JIT time and GC counters are retained; small timing/throughput differences are
not stable speed claims, and owner creation/new-game preparation is outside the
measured request. No additional per-node objects were introduced. Clock games
against the independent original binary include any executable overhead.

Real-model integrity: 312 interaction requests, 6,571,068 evaluations,
51,740 cold-workspace score comparisons and 7,794,733 prepared transitions all
pass; unpruned TT-on/off values agree wherever the bounded request completes.
Current-model LMR-off, LMP-off and cumulative combinations are recorded separately.
Frozen actual-UCI preservation audit passes 62 deterministic comparisons plus
both two-worker preflights, covering HCE, static, unpruned, LMR/LMP-off and
aspiration controls. No timing equivalence is inferred from scheduling-dependent
multiworker runs. The shared desktop uses Balanced power, without exclusive CPU
isolation; background variation remains a measurement limitation.
