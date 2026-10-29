Completed continuation: [R057-ASSESSMENT.md](R057-ASSESSMENT.md) records the
final trained-model and playing results. The measurements/planning boundary below
is preserved as originally recorded.

# R057 interim efficiency finding - small ONE, untrained

The width8/rank4 ONE implementation has a credible practical efficiency path.
In the primary phase-balanced comparison it meets the prospective cost criteria
against frozen CP64 with its normal vector head. This establishes computational
feasibility for this small configuration, **not learned quality or playing strength**.
The selected full-scale trained configuration still requires these comparisons.

## Function and implementation

R056's actual source computes, for each orientation and occupied recipient i,

`P[r,d] = sum_j K[j,r]*E[j,d]`

`a[i,d] = (sum_r T[i,r]*P[r,d] - (sum_r T[i,r]*K[i,r])*E[i,d]) / sqrt(R*(n-1))`

`score_cp = 400*(material/4 + bias + sum_i unary[i] + sum_i,d Q[i,d]*softsign(a[i,d])/D)`.

The new immutable snapshot precomputes `K*E` and self-content products. Per-ply
state maintains both orientation pools, unary sums, exact material tenths and
piece count. Changed-square deltas handle promotions, captures, castling and en
passant without move-type guesses; null moves select the other orientation.
Children copy the compact parent; undo retains the parent. Qsearch ownership is
explicit through the research-only lifecycle adapter. Production selection and
search settings are unchanged; research implementations never become core imports.

Recipient softsign and Q readout remain dependent on all other pieces. These cannot
be replaced by a single cached scalar: a changed pool generally changes every
recipient. Update is O(changed_squares*R*D); readout remains O(n*R*D).
No second pass, dense MIX, blocker approximation, quantization or CP64 backbone
was introduced. R056 source and its ordered-pair quadratic oracle are preserved.

The 25,409-field R056 checkpoint includes inactive TWO weights; ONE has19,201
active parameters. CP64 has1,970,440. The immutable optimized double payload is
325,904 bytes and each accumulator's primitive payload is569 bytes, excluding JVM
headers/alignment. There are no training moments in the inference snapshot.
The recorded allocation/init measurements include actual JVM objects and cold
class loading; they are not a steady-state model-load latency estimate.

## Correctness and numerical limits

Focused tests cover independent quadratic/full equivalence, colour/rank symmetry,
quiet/capture/promotion/castling/en-passant/null moves, parent/sibling preservation,
10,000 make/undo transitions, legal transposition paths, reset, ownership rejection,
main/qsearch copies and real ConstructionSearch repeatability. Original gradient,
checkpoint continuation, full-recompute research mapping and CP64 regression tests
also pass. See r057-phase-validation.json and retained XMLs in raw/r057.

Tolerance is `1e-8 cp + 1e-12*abs(reference cp)`. Maximum observed random-history
error was4.092726157978177e-12 cp; special/null maximum4.547473508864641e-13 cp.
There were zero rounded-score differences in that test. These are numerical
reassociation bounds observed on the tested states, not universal bitwise equality.
Search additionally detects scores within1e-6 cp of a half-centipawn rounding
boundary and rebuilds in R056's exact operation order. Targeted boundary tests
verify this fallback, including256-ply null chains. Supported snapshots require
finite R056-clipped weights (absolute value<=.99). Arbitrary out-of-range weights
are rejected. This is research validation, not production qualification.

An initial whole-search test failed because its new queen fixture illegally left
the nonmoving king in check. The failure and executed source binding are retained;
the corrected FEN was independently checked with python-chess, then all focused
checks passed. No engine correctness repair was inferred from that invalid input.

## Primary measured costs

Primary evidence: [r057-efficiency-results.json](r057-efficiency-results.json),
raw/r057/lifecycle-v3-vector/result.json, frozen runtime-v3 and
r057-vector-lifecycle-plan.json. Seed71 initialization with deterministic seed97
bounded random weights; **no candidate training**. Thirty roots: six diagnostic
positions plus three sampled original-validation positions in each of eight
material buckets. Fixed source indices/identities are in r057-cost-roots.json.
These are consumed cost/development positions, not a fresh strength panel.

Nine rotating-order rounds in one JVM, one logical CPU/SerialGC/512MiB heap,
BelowNormal supervisor ownership. CP64 uses `vector-256-long`, matching its
launcher capability. Common search flags262143/ADAPTIVE16, one worker, TT16MiB,
no clocks/ponder/book/tablebases. CP64 production flags3407871 remain a separate
composition, not silently treated as this common-search control.

| Cost (median microseconds) | R056 full ONE | Incremental ONE | CP64 |
|---|---:|---:|---:|
| Full evaluation/rebuild | .956 | 1.024 | 3.435 |
| Incremental update | n/a | .327 | 1.362 |
| Readout on prepared state | n/a | .576 | .622 |
| Update plus readout | n/a | .689 | 1.552 |
| Full trace per evaluated position, including all state work | .747 | .510 | 1.066 |
| Whole-search aggregate nodes/second | 630,497 | 748,498 | 591,260 |

Component timings are measured separately; their medians need not add. The full
trace contains84,939 events:206 initializations,45,000 children and39,733 readouts.
This common flat search uses shared main/qsearch storage, so it made zero explicit
cross-state copies; copy correctness is independently tested, not falsely claimed
as timed in the trace. Parents/children for component timings are prebuilt; the
complete trace includes actual lifecycle work. All measured hot-loop allocation
deltas were zero. Transitions are sampled across the entire frozen trace.

Incremental ONE's lifecycle/CP64 ratio is.4782 and NPS ratio1.2659, passing the
prospective <=1.25 and >=.8 criteria. Across90 requests per evaluator, ONE consumed
879,729 nodes and CP64 879,759; early completions account for less than the900,000
maximum. Full and incremental ONE agreed on each root's repeated best move, score,
completed depth and node count. All reported PVs were legally replayed.

## Interpretation and unfinished work

The identical transition replay is the strongest evidence about inference cost.
Whole-search CP64/ONE timings follow different evaluator-dependent trees, so their
NPS difference is not a pure kernel effect and says nothing about playing strength.
Nine timing rounds are correlated; saved min/max values are descriptive, not
independent confidence intervals. The trace was generated with untrained ONE;
a CP64-generated trace and trained-model repeats would broaden applicability.
No claim that every depth, material distribution or consumer machine has this ratio.

Earlier v1/v2 observations are preserved. They used CP64's scalar head, and their
CP64 full-evaluation component used the slow independent unit-major oracle. Those
full costs must not be presented as normal CP64 refresh performance. The primary
v3 corrects both comparison details; no old results or failures were overwritten.

Precomputation makes the small model viable computationally. The remaining hot
work is recipient readout, and wider/richer ONE increases it. Adequate capacity,
generalization, original-target learning and fixed-node competitiveness remain
unanswered pending the authorized full-scale training opportunity. This finding
does not lift the broader programme suspension or authorize production promotion.
