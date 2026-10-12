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

Current continuation: six formulations and 704 development games are complete.
FORCED's independent 2,048-game confirmation finished inconclusive at -5.09 Elo
and is closed. Fresh BOUNDARY development finished +1.36 point Elo with wide
uncertainty and passes every independent audit; it meets the predeclared
advancement condition. Freeze and execute its wholly separate independent
128/384/1024-pair confirmation using the reserved .025 positive-tail allocation.
Do not pool or extend FORCED. All 72 retained-history UCI comparisons passed.
No production extension is adopted. Reconcile the final independent result,
perform any justified integration, and close SR-030 in both masters without
selecting a successor.

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

During the frozen games, source review identified one telemetry refinement for
the next code boundary: maximumExtensionPlies currently records awarded credit
before the evaluator admission hook, so an aborted attempt can count an edge
whose child was never entered. Completed results and adapter actual node/main/q/
maximum-real-ply accounting are unaffected. Move this research statistic to
actual main-node entry and add an admission-cancellation witness before final
freeze; do not interrupt or reclassify the valid clock experiments.

## Evidence-led follow-up hypotheses (before v1 score comparison)

The measured broad giving/evasion D6 cost and sparse forced coverage motivate two
additional mechanisms, rather than changing another feature or sweeping numeric
thresholds. LEAF extends an evasion only at remaining main depth one, once/path:
it exposes a quiet reply after the evasion beyond the accepted qsearch boundary.
REPEAT consumes one eligibility credit at each checked positive-depth main node,
adds a ply only on every second such occurrence, and stops after two added plies.
The occurrences are path-local, not globally counted, not qnodes and not retries;
intervening non-check nodes preserve credit. This tests accumulated checking
pressure versus an isolated check without introducing fractional main depth.

These hypotheses follow cost/coverage evidence before comparative v1 game scores.
After all four frozen screens finish, implement and challenge them with the same
independent oracle, model/transition, accounting and cost gates, then complete a
fresh shared 16-pair development screen for both. Any further selection must use
all mechanism/cost/strength evidence and independent confirmation for adoption.

## Completed initial clock screens and v2 semantic refinement

All four declared 16-pair screens completed with stable inputs, 128 normal game
terminations, legal prefixes/PVs, complete color reversal and whole-request
accounting. Candidate W/D/L: boundary 13/9/10 (+32.67 point Elo), giving 14/4/14
(0.00), evasion 11/6/15 (-43.66), forced 15/6/11 (+43.66). Every uncertainty
interval is wide and includes zero; none establishes benefit or inferiority.
The shared-opening dependence and four-way exploratory selection are retained.
The cheap boundary/forced policies remain credible; broad cost lacks a compensating
screen signal. LEAF/REPEAT were motivated and recorded before viewing these scores.

The v2 code adds only those two explicit research controls. REPEAT stores integral
eligibility credits in the existing per-ply primitive array and converts pairs to
added plies; no per-node allocation or fractional depth/TT domain is introduced.
PVS retries reuse the same path slot; the final four-credit state exhausts its
two-ply allowance. LEAF has one credit. Production stays OFF. The maximum-added-
plies statistic now updates at admitted main-node entry, so cancellation before
the first extended child reports zero. Diagnostic awarded-credit counters retain
their distinct decision meaning. The correction does not change a search tree,
score, completed result or whole-request admission.

Twenty-one focused tests and all thirteen Python adapter tests pass. The first
repeat fixtures did not exercise four checked positions before mate/cutoffs;
the strengthened depth-eight forcing witness now observes the full cap. It also
independently checks the depth-three repeated horizon and cancellation/reuse.
All six policies run the existing oracle/bounds, TT, history, PVS and accounting
gates. A system-Python invocation lacked python-chess; rerunning in the established
measurement venv passed. An incorrect diagnostic input path produced only an
empty file, retained temporarily outside evidence; the corrected run completed.

The follow-up study completes 234 requests. At D6, admitted-node ratios are
LEAF 1.137 and REPEAT 1.021; changed moves are 4/26 and 1/26. At 100k nodes,
LEAF is shallower on 3/26 roots and REPEAT on 1/26; neither is deeper. The forcing
and opening witnesses exercise repeated credit exhaustion. These are diagnostic
cost/horizon observations, not strength or clean throughput conclusions.

The v2 real-model interaction audit passes 156 requests, 3,074,320 evaluations,
24,207 cold-score checks and 3,638,942 prepared transitions. It includes both
LMR/LMP-off and cumulative controls and completed unpruned TT differentials.
The two clean cost batches pass all 624 requests: LEAF D6/100k/1M wall ratios
1.081/.989/.980; REPEAT 1.002/1.005/.997. Equal-node throughput is close and
small differences remain JIT/desktop-sensitive; request allocation/GC/JIT facts
are preserved, without treating early compilation allocation as per-node cost.
Seven tests pass for the check-aware prospective confirmation assessor, adapted
without changing any previous feature's frozen assessor or confirmation.

The final v2 broad run passes 726 tests with 11 existing skips (737 discovered).
The follow-up historical replay also completes every bounded root/reference call;
its full results remain separate from the v1 witnesses and from game evidence.

V2 is frozen from clean source `41587d2a80995cffe60acdf5859f5aa9b9846e54`,
with all 704 main classes byte-matched. app.jar SHA-256:
`1e39d6b8afc5e720b080561cebfacf6e0411a13f9921a354a50de374b717c76c`.
Five real-UCI bridges pass 320 deterministic comparisons: actual default versus
accepted v5, and each original extension policy versus v1. This validates tree/
score/PV/work equivalence across the telemetry correction. The v2 preservation
audit also passes all 62 deterministic HCE/research comparisons and both
multiworker preflights. No timing equivalence is claimed.

`screen-protocol-v2.json` prospectively freezes fresh shared 16-pair LEAF/REPEAT
screens at 10+0.1, concurrency four, one worker each. Inputs, source and adapter
are frozen before outcomes. The desktop CPU sample after verification shows
ordinary IDE/browser background work and no running build/test/benchmark;
exclusive CPU isolation is not claimed. Complete both before selection.

## Finalist development selection

Both v2 16-pair screens pass all audits with 64 normal games: LEAF 16/6/10
(+65.92 point Elo), REPEAT 9/10/13 (-43.66). Their wide bounded-pair intervals
include zero. Different initial screen batches cannot establish a direct ranking.
Across all six screens there are 192 development games, none adoption evidence.

Advance unchanged FORCED and LEAF to complete fresh shared 64-pair batches at
15+0.15 (seed 20261303, partition zero, concurrency four, one worker/engine).
Complete both before comparative selection; no early score-based stopping. FORCED
has low measured cost and a positive exploratory result, while LEAF exposes a
quiet reply at the useful main/q boundary with moderate cost and a positive
screen. This is joint mechanism/cost/strength selection, not a statistical claim.
BOUNDARY remains an overlap/order control with a modest positive screen but no
new unpruned leaf value; broad GIVING/EVASION impose substantial cost without a
compensating exploratory signal. REPEAT's low cost did not produce a positive
screen. Those observations do not prove the unselected mechanisms inferior or
exclude small benefits. All six remain reproducible research controls.

If a finalist stays credible after fresh development, freeze a prospective
partition-one independent confirmation; never pool these selected development
results into its inferential sample. Ordinary difficulties are not a handback.

Cross-binary actual-default cost/parity passes 312 requests and 156 deterministic
comparisons against accepted v5. D6/100k/1M whole-request elapsed ratios are 0.999/1.034/0.996;
three 1M warmups per JVM and rotated order are retained. This bounded desktop
sample finds no large retained-default overhead, not guaranteed timing equality.

### Independent confirmation design to freeze after finalist selection

A credible unchanged finalist will use the checked `tools/sr030-confirmation.py`
procedure, with cumulative looks at 128/384/1024 fresh partition-one pairs,
15+0.15, one worker and four concurrent games. Before any confirmation outcomes,
freeze exact candidate/model/Java/runner/adapter/assessor identities, all stage
openings and exclusions (including prior features' confirmation selections),
commands and runtime conditions. Positive and negative stopping requires both
the Bonferroni-adjusted paired-normal interval and required 2.5-SE interval to
exclude half score on the same side. Normal inference requires at least 20 pairs
above and below half; otherwise only the adjusted bounded-pair Hoeffding interval
can qualify. Report Hoeffding sensitivity in all cases. The procedure is the
established approximate paired design, not a distribution-free gain claim.

The three-look two-sided family alpha is .05, allocating at most .025 approximate
positive-tail error to this finalist. Reserve .025 of a .05 feature positive-tail
budget only for a separately justified fresh finalist/design; it is not an
instruction to keep trying candidates until one wins. Stop at a qualifying look,
otherwise finish inconclusive at 1024 pairs; no post-stop extension, development
pooling, candidate tuning or clock changes. No minimum Elo gain is imposed.
Prospective power/precision planning will distinguish a resolvable gain from a
small benefit this sample cannot settle. Feature disposition will reconcile all
mechanisms and evidence, not mechanically equate an inconclusive look with
universal rejection or use unfinished ordinary work as a reason for deferral.

## Frozen independent FORCED confirmation

Fresh 64-pair development passes all 256 normal games and audits. FORCED records
51/35/42, +24.47 point Elo, paired-normal 95% [-25.26,+75.23]. LEAF records
42/29/57, -40.90, [-96.61,+12.77]. Their same-opening exploratory score difference
(LEAF minus FORCED) is -.09375 with normal 95% [-.19525,+.00775]; this is not a
head-to-head match or proof of superiority. FORCED retains a positive signal
with low measured cost; LEAF's initially favorable screen does not replicate.
Select unchanged FORCED for independent confirmation, without treating either
development interval as an adoption or universal rejection result. Total
exploratory work is 448 games; all remain excluded from confirmation inference.

The final `confirmation-protocol.json` freezes 1024 fresh partition-one pairs,
three disjoint stages of 128/256/640 pairs, seed 20261304 and all identities before
any confirmation game. It excludes prior Phase 2 confirmation selections as well
as current/prior development roots. Every stage must pass independent legal,
pairing, normal-termination, input and resource re-audits. The assessor was
strengthened to reject any adverse termination itself, in addition to the frozen
runner's check; all seven statistical tests still pass. No match/test workers
remain before launch; normal IDE/Gradle background processes are retained.

Prospective planning uses observed forced-development pair variance .08505,
a rounded .10 scenario and the bounded maximum .25. At .10, 1024 pairs give
approximately 85% final-look power for the +24.47-Elo development-sized effect,
but only 3.8% for five Elo. Approximately 21,570 pairs would be needed for 80%
final-look power for five Elo under that scenario. These are planning estimates,
not guaranteed power or a minimum acceptable gain. The fixed experiment can
support a modest clear gain while leaving a genuinely small benefit unresolved;
it will not be extended after observing its finished result.

### Housekeeping and validation scope

Removed 21 verified task-owned temporary compile/test/cost console files and the
empty incorrect-input probe from `app/build`, after retaining the successful XML,
JSONL, summaries and freeze console in the evidence directory. No recursive delete,
unrelated cleanup, frozen installation removal or manual cleanup is required.
Both candidate installations and the original comparator remain intentionally
retained for reproduction. No push, release, deployment or version-system repair
has been performed.

GUI/browser suites, unrelated long NNUE training, other-platform/weight strength,
longer-clock strength and multiworker strength are outside this bounded evidence.
Ordinary HCE and other research controls, single-worker production equivalence and
supported multiworker UCI preflights have been checked. The 11 broad-suite skips
are preserved in XML; static inspection is not reported as runtime verification.
Any strength-supported production integration still requires its own concrete
factory/control/lifecycle validation and measured-binary equivalence before
technical closeout. No acceptance or Project Source synchronization is claimed.

### Prescribed confirmation look 1

The first 128 pairs (256 games) pass every frozen input/runtime/source, legal,
normal-termination, pairing and whole-request audit. Candidate W/D/L is [102, 56, 98].
Mean score .5078125 gives +5.43 point Elo, adjusted paired interval
[-42.03,+53.09], required 2.5-SE [-44.15,+55.24], and adjusted Hoeffding
[-91.67,+103.40]. Decision: **continue**. None establishes a gain or loss.
The unchanged, predeclared second stage adds 256 fresh pairs to reach 384;
no settings, candidate, stopping rule or sample limit have changed.

Feature-level continuation remains evidence-led after the completed FORCED
confirmation. A qualifying gain permits concrete production integration and
validation. If FORCED does not qualify, revisit whether BOUNDARY's modest positive
initial screen warrants fresh 15+0.15 development before closing the feature.
Its overlap with the unpruned qsearch value domain does not prove a lack of clock
benefit: main/q ordering, PVS and history effects can change selective work.
This is a possible further SR-030 investigation, not a change to the frozen
FORCED confirmation or authority to extend its sample. Any later independent
candidate needs its own fresh prospective design and the reserved positive-tail
allocation; no current or future exploratory result may be pooled into FORCED.

The 11 broad-run skips are specifically the opt-in QuiescenceDeltaEvidenceTest
and QuiescencePostMoveEvidenceTest suites, gated by `sr001h.instrumented` and
`sr001i.instrumented`. Those instrumentation flags were not enabled; ordinary
qsearch, oracle, rules and extension tests did run. A final real-UCI comparison
of all six retained SR-023 history controls is prepared in
`tools/verify-sr030-history-controls.py`; run it only after clock games finish,
against `app/build/sr023-p2-candidates-v2` and the final candidate installation.
The helper is not yet executed and must not be reported as a passed check.

Before the second FORCED look is available, make the conditional follow-up
concrete: if its completed confirmation does not qualify a gain, run unchanged
BOUNDARY on 128 fresh partition-zero pairs at 15+0.15, seed 20261305, with the
same frozen binaries/model and four concurrent single-worker games. Freeze the
book and exact command before launch; exclude all previous development roots.
Complete the fixed batch before inspecting its score. A positive paired point
estimate, together with the existing low-cost mechanism and passing audits,
permits a separately frozen independent confirmation using the reserved .025
positive-tail allocation; it is not itself adoption evidence. A nonpositive
result supplies no further positive development replication and does not justify
arbitrary retuning or another confirmation of this same formulation. This
conditional work neither changes nor extends the FORCED experiment.


### Prescribed confirmation look 2

The cumulative 384-pair / 768-game look passes every independent audit. Stage
2 adds 199/113/200 W/D/L; cumulative W/D/L is **301/169/298**. Mean score
.501953125 gives **+1.36 point Elo**, adjusted paired-normal interval
**[-25.18,+27.91]**, required 2.5-SE **[-26.36,+29.09]**, and adjusted
Hoeffding **[-53.93,+56.72]**. Pentanomial counts are [62,57,135,76,54].
Decision: **continue**. This is neither a qualifying gain nor a loss. The
unchanged final stage adds the predeclared 640 fresh pairs to reach 1024;
all candidate, input, clock, audit and stopping rules remain frozen.


### Finished independent FORCED confirmation (2026-10-13 local)

All three prescribed stages and independent re-audits pass. The final 640-pair
stage scores 486/275/519; cumulative **1,024 pairs / 2,048 games** score
**787 wins, 444 draws, 817 losses**, all normal terminations. Mean score
.49267578125 gives **-5.09 point Elo**. Adjusted paired-normal interval is
**[-21.56,+11.36]**, required 2.5-SE **[-22.29,+12.08]**, and adjusted
Hoeffding **[-38.85,+28.57]**. Pentanomial counts: [169,175,349,179,152].
The frozen decision is **inconclusive**. No gain or loss is established; the
favorable development-sized gain did not replicate in the independent sample.
A smaller benefit remains statistically possible. This confirmation is finished:
no additional pairs, tuning or exploratory pooling may extend it.

The predeclared conditional BOUNDARY development now applies. This is the
remaining positive initial-screen mechanism without a larger fresh replication;
its qsearch overlap is not a proof that its selective ordering/work effect cannot
help at fixed clock. Run the unchanged frozen policy on 128 fresh pairs under
the recorded rule before resolving the feature disposition.

Between clock experiments, the final retained-history UCI preservation audit
passes **72 deterministic comparisons across all six SR-023 controls**. Both
frozen JAR/source identities and the generation-32 model are checked; depth/node
score, PV, work and termination agree, timed/cancelled requests are audited,
and all inputs remain stable. This completes the previously pending check;
`history-controls-v2/controls.json` and all transcripts retain the evidence.
No engine source or frozen installation changed.

The conditional BOUNDARY batch is frozen in `boundary-development-protocol.json`: 128
fresh partition-zero pairs, seed 20261305, 15+0.15, unchanged v2 versus accepted
v5, one worker and four simultaneous games. Twenty-five valid prior selection
receipts exclude 5,028 distinct positions; selection draws from 16,709 remaining
partition-zero roots. Inputs and the exact launch command are hashed before play.
An initial overly broad receipt glob encountered two legacy metadata schemas and
created no book or games; restricting to complete position-key selection receipts
resolved that tooling-only issue. No experimental result was discarded.


### BOUNDARY development reconciliation (2026-10-13 local)

The predeclared 128-pair replication completes **101/55/100 W/D/L**, score
.501953125 and **+1.36 point Elo**. Paired-normal 95% interval is
**[-35.87,+38.61]**, bounded-pair 95% **[-83.63,+86.51]**; pentanomial
[17,25,46,20,20]. All 256 games terminate normally; independent input/runtime,
identity, legal-prefix, color-pairing and whole-request re-audits pass in
`boundary-development-assessment.json`. Total exploratory play is now **704
games**, entirely separate from confirmation inference.

This is a small positive point estimate, not an established benefit or a precise
replication of the original +32.67-Elo screen. Alongside the low-cost mechanism,
it meets the prospectively recorded advancement condition. Advance unchanged
BOUNDARY to a fresh independent confirmation using the reserved .025 positive-tail
allocation. The two finalists together allocate at most .05 approximate feature
positive-tail error; no allocation remains for automatic additional finalists.
This is the final evidence-supported remaining mechanism, not arbitrary tuning.

Use the same independently audited 128/384/1024-pair sequential procedure,
15+0.15, one worker, four simultaneous games and frozen v2. Select wholly fresh
partition-one openings with seed 20261306, excluding all previous selections,
including the finished FORCED confirmation and BOUNDARY development. Freeze the
protocol, input identities, exact commands, sample limits and power/precision
limitations before play. Do not pool either candidate's development or the
FORCED confirmation into BOUNDARY, and never extend either finished experiment.
A 1024-pair design can resolve a clearer gain still compatible with the very wide
development interval; it has poor power for a truly tiny gain such as the observed
point estimate. The prospective planning record must state that limitation; no
minimum worthwhile Elo or universal no-benefit claim follows from this sample.
