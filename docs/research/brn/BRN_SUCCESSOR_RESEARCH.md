# BRN successor research record

[Contract](../../brn/BRN_SUCCESSOR_CONTRACT.md) and
[operational state](../../brn/BRN_SUCCESSOR_STATE.md). Started 2026-10-04.

## B00: starting implementation and bootstrap challenge

Inspected clean HEAD `12e1398` (latest: Optimize BRN-3 training kernels and corpus
preparation), root/ancestor instructions, README, build/source-set boundaries,
three prior BRN contracts/frontiers, production brn3 classes, research candidates,
data reader, diagnostics/match/runtime harnesses and parity tests. No repository
AGENTS.md was present. Root journal absent; observed UTC clock first succeeded at
2026-10-04T01:01:33Z after the initial PowerShell clock syntax failed. No journal
capability is activated. Maintained helper commit and both executable hashes match
the approved binding. Its begin is blocked by the historical reservation described
in state; status records an uncertain prior mutation following unexpected Gradle
changes during verification. Preserve that fact and build33, never infer success.
This programme is separately attributable later work under DEFERRED_FINALIZATION.

### Architecture reconstructed from executable source

`core/brn3/Brn3Layout`, `Brn3Features`, `Brn3Trainer`, `Brn3Workspace`, `Brn3Codec`
define 768 entities (12 owner/type channels x64 squares), 294528 unordered absolute
rows x8, 768 unary rows x8, two 96-value typed pools, 192x32 dense ReLU, linear
scalar residual and fixed material P1/N3.2/B3.3/R5/Q9/K0. Black perspectives exchange
colors and reflect ranks. For each piece: ReLU(unary + incident edge sum/sqrt(n-1));
typed sum/sqrt(n) pooling precedes mover/opponent ordered readout. Material is added
outside the residual. Search uses gain .25; raw training uses gain1. There is no
iterative exchange of updated states. Runtime sees placement and STM only.

Training uses an additional 259200 shared typed-displacement parameters, folded
into the absolute rows at snapshot. Thus absence of *all* statistical sharing is
falsified. Inference has2368577 parameters, training2627777; binary64 masked Adam
.003/.9/.999/1e-8, batch128/eight-epoch ordinary defaults, pure expected-score CE.
Absent coordinates/moments freeze; touched zeros evolve. Model float32, cached edge
sums double. Existing float/cache tolerance is1e-4 pawn, not bitwise trainer parity.
Training state independently preserves weights/moments/step/config; fixed layout,
magic and CRC prohibit incompatible reuse. New learned shapes need new identities.

All eight seed observations still hold structurally. Whether they hurt strength
is unproved. Symmetry, width8 and pooling may regularize usefully. Prior experiments
already rejected wider *untyped* and other relation families under different
conditions; they do not settle their interaction with today's typed architecture.
Absolute pairs with two occupants on one square are impossible; legal king/pawn
restrictions remove further possibilities. Quantitative coverage remains to measure.

Runtime keeps square-addressed raw sums for both perspectives. It rebuilds whenever
piece count differs, >8 squares change, or removed+added>8; otherwise it removes and
adds incident vectors and repools. Because sums are unnormalized, changing n need
not invalidate them: new normalization can be applied while repooling. This is a
testable implementation opportunity before any learned topology change. Search
workspaces are worker-local, but BRN's reuse follows the last evaluated placement,
not ancestry; evaluate actual leaf/sibling behavior before adding a stack.

### Infrastructure and challenge conclusions

Normal training is sequential source-backed `TrainerService`/`SequentialTraining`
with per-lineage cursors, independent optimizer and Best state, persisted architecture
enums/configuration and attempt identity. Do not change NNUE or user stores.
Verification has `BrnResearchData`, configurable `AbsoluteRelationCandidate`,
independent production parity/gradient/resume tests, `BrnResearchMain`, production
model bridge, diagnostics, frozen comparisons, paired matches and runtime probes.
These are useful foundations, but arbitrary new candidates need a common isolated
adapter, sufficient gradient checks and new formats before ordinary integration.

Source exists locally: E:/SeedV6-Corpus/incoming/lichess/lichess_db_eval.jsonl.zst,
22086532809 bytes. Local prior research datasets/models exist. Existing prospective
dataset: raw1578888..1953686,262144train/16384validation/16384test; source identity
d8e423b9c6ab7ca56b3383d52d09d0a3c36611c6e42f517316a2cbe448c58842.
Use validation for exploratory work and verify payload digests when reading. Do not
reopen old test data for selection; prepare a new frozen final range if warranted.
Source has no game identity, so geometry grouping does not prove game independence.

Earlier learning research identified material cancellation on search-selected
positions and established quarter-residual calibration. This is an additional
architectural interaction; lower raw corpus loss could select a weaker engine.
Production search disables qsearch; its existing research seam must remain separate.
Parameter count alone misses memory locality, sparse exposure, training cost and
engine branching. Use quality/cost Pareto evidence, then equal-time games.

Challenge outcome: preserve recent optimizations/calibration in original baseline;
measure already-shared absolute-table utilization; prioritize a mathematically
bounded runtime change; retain interacting hypotheses rather than freezing a
sequence. The existing contract/state/research convention is sufficient. Current
request opens non-placement-state research beyond older completed geometry-only
contracts, while original BRN-3's boundary stays fixed. No predetermined BRN-4.
One agent slot is available, so independent sub-agent execution is unavailable.
No browser/GUI changes or browser validation are needed for initial headless work.

Software validation and empirical measurements: pending R01/B01, not inferred from
historical reports. No successor candidate or strength claim has yet been accepted.

## A01 design: sharing and capacity screening (frozen before training)

Coverage probe on the prospective dataset finds 76362/294528 rows untouched,
80548 touched1..9 times and median exposure8. But validation unseen-pair occurrence
fraction is only0.00015619. Counts include both perspectives. Sufficient disjoint
impossibility conditions account for31968 rows (4224 same square,23712 additional
pawn-on-last-rank,4032 additional same-color king pairs); this is not a complete
legality enumeration. The hypothesis is statistical efficiency/unequal coverage,
not a proven widespread unseen-edge failure. Removing impossible storage alone
cannot improve representation and must justify its mapping overhead.

Primary sources consulted 2026-10-04:
- [Deep Sets](https://arxiv.org/abs/1703.06114): invariant aggregation is a valid
  family, not evidence that finite width8 sums preserve every relevant statistic.
- [Relation Networks](https://arxiv.org/abs/1706.01427): learned entity interactions
  motivate comparing pair parameterizations, not a chess performance guarantee.
- [Message Passing Neural Networks](https://arxiv.org/abs/1704.01212): contextual
  passes are a coherent alternative; their usefulness/cost here requires testing.
- [Principal Neighbourhood Aggregation](https://arxiv.org/abs/2004.05718): multiple
  aggregators motivate max/moment challengers; graph benchmarks are not chess proof.

First isolated alternatives preserve typed local ReLU, two perspectives, fixed
material, CE/masked Adam and head32: original absolute+relative control; relative-only
messages; hashed absolute residuals plus relative table; factorized entity-product
messages plus relative table. Factorization retains per-piece nonlinearity (unlike
old global R1), and can be evaluated via entity sums. Hash sharing is a capacity/cost
control, not an assumption that unrelated collisions are helpful. Width16 is an
interaction challenger only after narrow family screening. Pool summaries, receiver
interpretation and context depth will use survivor controls, not change everything
at once. New experimental state format never masquerades as BRN-3 or BRN-4.

Screen: first16384 existing training examples, first4096 validation examples,
eight epochs/batch128, seed71, rate.003, same deterministic epoch shuffle. Select by
validation outcome half-MSE; report raw and quarter-residual quality, training
seconds, parameter count and warmed inference. Advance according to contract
admission rule, then replicate before any champion promotion. Baseline adapter must
match the independent original double implementation and gradients/roundtrip/resume
must pass before trusting candidates. Individual run ceiling300s; full-sized runs
or games are not admitted yet. No sealed test labels enter screening.

## R01 and A01 early results

R01 removes the piece-count rebuild gate; changed endpoints still rebuild their
own sums, unchanged endpoints subtract/add incident float vectors into double raw
sums, and all nodes use the new relation/pool normalization. Tests pass: special
move/sibling/reverse-capture traversal,5000 random placements plus independent
workspaces, original training/codec/parity and calibrated search (six tests total,
zero skips/failures). Explicit FENs exercise castling, EP and promotion/capture.
Original implementation is compiled from `git show 12e1398:.../Brn3Workspace.java`
into an isolated overlay; no source checkout/store modification.

Three process repetitions, seven warmed timing rounds each, give median-of-process
latencies in ns (original -> count-changing): unrelated7074->7163, quiet3977->4401,
capture7751->5649, siblings4919->5290. All forced-rebuild comparisons have exactly
zero observed error and no integer changes. Sequence sizes:256 unrelated,
12732 quiet,756 capture,6744 sibling evaluations per
parity pass. All32 depth4 root score/move/node/completion identities match in all
three repetitions. Search totals original[1.3462,1.0817,1.0798]s versus
changed[1.0844,1.1353,.9941]s: no established overall search gain/regression.
Disposition: provisional runtime improvement for count-changing workloads, with
quiet/sibling micro-regression explicitly unresolved by shallow search. R02's
deeper warmed controls will decide retention; do not claim universal acceleration.

A01 harness passed three tests: exact all-parameter/all-moment control parity with
the independent original, finite differences for every relation/pooling family,
CRC roundtrip/corruption/exact continuation, tiny-set learning and input/color
boundaries. Twelve configurations are exercised within the parameterized test.
No main architecture/trainer/codec/NNUE changes were made for these prototypes.

| seed71,16k x8 | Parameters | Selected raw loss | Quarter loss | Training seconds |
|---|---:|---:|---:|---:|
| Absolute+relative width8 sum |2627777|.17762744|.20266733|13.145|
| Relative-only width8 sum |271553|.17452652|.20072772|6.086|
| Hash32768+relative width8 sum |533697|.17929006|.20250411|14.406|
| Factorized+relative width8 sum |277697|.18213721|.20298580|7.472|

No family has a strength conclusion. Relative-only advances for Pareto efficiency,
not because its1.75% loss difference clears the2% quality admission threshold.
Hash is rejected at this screen (more costly than relative, worse loss); factorized
is deferred (extra computation/parameters and worse loss than relative). Reopen
only if another configuration alters the interaction, not by discarding this result.
Generic double timings cannot be compared directly to optimized folded BRN-3.
All selected states remain ignored scratch artifacts; compact complete reports
and dataset/model hashes are in `evidence/successor-2026-10-04/`.

Next admitted A02/A03: width16 absolute and relative controls; relative sum+max and
sum+square alternatives; seed97 replication of absolute8 and relative8. Same data,
count/epochs/objective/shuffle recipe, new outputs,300s per process. Compare richer
pooling with width controls; do not credit additional capacity as a free gain.
These remain screening, not promotion. Original BRN-3 remains the champion.

R02 compares rolling, per-ply rolling and four nearest-placement caches via the
existing explicit evaluator seam, production SearchDriver,64 roots/depth5, three
rotated rounds and warmup. No main search or NNUE edits. Reject added memory/selection
complexity absent a repeatable material search benefit; every root must retain
score/move/node identity. Full eager ancestry copies also incur work at internal
nodes where BRN is not evaluated; counters will expose that cost before considering
such a prototype. Existing evaluation states do not maintain BRN ancestry.

## A04/A05/A06 admission (before their training)

Use relative-only width8 as the inexpensive survivor for three single-variable
probes. Directed typed displacement uses separate ordered receiver/sender messages
instead of adding the same vector to both endpoints; the existing allocated table
has space for ordered types, so parameter count stays271553 though more rows become
active. A cheap second contextual pass broadcasts mean other-piece updated states
through a shared learned width8 matrix, plus a self transform and residual, before
ReLU and typed pooling. It adds136 parameters and O(n*width^2) work, not a second
absolute edge table. This tests context depth; failure would not disprove all GNNs.

For non-placement inputs, test only four canonical relative castling rights and
eight EP-file one-hots per perspective at the readout. These control available legal
continuations in Board, can differ with identical placement and are absent from
BRN-3. No tactical labels, legal-move-count features, clocks/history/hash are added.
Rule adjudication already handles terminal history/clocks; no demonstrated need
to spend learned capacity on them. Dense-state features cost768 weights, no pair
table growth, and remain an incompatible experimental identity. Geometry-group
splits are retained (stricter than state-specific keys); placement-only dedup in
historical data may suppress paired state observations and must be disclosed.
Rights/EP gain, if any, requires fresh state-aware data confirmation before promotion.

All three use the same16k/eight-epoch seed71 screen and require independent gradient,
serialization and relevant input tests before training. Extend tests to all25
relation/pool combinations as a software check, not25 training experiments.

## R01/R02 decision and larger-data admission

All64 depth5 roots complete with identical score/move/node identity across original
and count-changing implementations and all three cache topologies. Three rotated
round medians in seconds: original rolling7.101/per-ply6.902/nearest4 7.107;
count-changing rolling6.725/per-ply7.036/nearest4 7.053. On the changed rolling path,
1516568 calls produce32892 rebuilds,1481594 updates and2082 repeats. Nearest4 reduces
rebuilds to25857 but is slower; per-ply rebuilds33045 and is also slower. Children
prepared1871903 exceed evaluated leaves, so eager copying would add internal-node
work too. Reject per-ply and nearest4 for this champion; defer eager ancestry/delta
stack unless a changed architecture/search profile supplies a plausible advantage.
R01 is retained: same learned function/models, fewer capture rebuilds,27% capture
micro benefit and about5% depth5 benefit in this bounded test, no search regression.
Quiet/sibling micro regressions remain a limitation; do not generalize the speedup.
Champion identity remains BRN-3 with exact-semantic runtime optimization R01.

Seed97 confirms the relative-only cost opportunity, not a quality superiority:
absolute raw loss.17432672 versus relative.17423949; training12.697 versus6.182s.
Width16 absolute.18576038/24.326s and relative.17684946/11.372s fail to improve the
width8 survivor. Relative max.18042414/8.396s and square.17964078/8.084s likewise
fail. Reject these configurations at the micro screen, preserving reopening for
larger-data/representation interactions. No blanket assertion that width or pooling
cannot help. Aggregate data and metrics are in architecture-screens.json.

Ten targeted tests now pass with zero failures/errors/skips, including all25
experimental gradient/resume combinations and explicit rights/EP boundaries.
Admit A04/A05/A06 micro runs and A01 scale-up: absolute8 versus relative8,
131072 examples x8 epochs, seeds71 and97 on the same prospective dataset;
same held4096 validation selection, <=300s/process, one process at a time,2GiB heap.
Reason: 16k can favor shared models simply through underexposure; two inexpensive
seed screens justify checking this before any promotion/integration or games.
This costs roughly eight times measured early training, still bounded local CPU.
No source test split, normal store or independent user campaign is consumed.

## A04/A05/A06 early interpretation and context ablation admission

Directed relative messages raw loss.18207075 and state-input probe.18105564 are
worse than relative symmetric .17452652 at the same16k/eight-epoch seed71 screen.
Reject these configurations at this screen, without claiming symmetry or omitted
state is universally optimal. Directionality activates more independent relation
rows (potential variance cost); rights/EP may need a better state representation or
paired-state data. Clocks/history stay with rules/search absent contrary evidence.

The cheap second context pass reaches .15437190, about11.5% below relative control,
and advances. This is not yet evidence that *third-party context* caused the gain:
it also adds a self transform and second local nonlinearity. Admit SUM_SELF ablation
with identical first-layer/readout/self-transform/bias initialization but no other-
piece message. It adds72 effective parameters versus136 for context. Compare both
on seed71 and replicate context seed97; preserve the same data/recipe and300s bounds.
Extend finite-difference/resume tests before running this ablation. If self alone
matches context, prefer the simpler structure and do not claim message-passing gain.
Then compare survivors at131k alongside the already-admitted absolute/relative
controls. Subsequent width/direction/pooling interactions are conditional on these
results. No architecture name or production default changes follow from this screen.

## A01 scale result: no unconditional sharing winner

At131072 x8, absolute+relative raw loss is.12516408/.12976969 (seeds71/97),
relative-only.13312484/.13022161. Relative has lower balanced-stratum loss
(.07429/.07304 versus.08005/.08319) but worse aggregate and quarter-calibrated loss
(.19465/.19422 versus.19100/.19132). More training exposes useful absolute capacity;
the micro near-parity cannot justify replacing the incumbent. Retain relative as a
training-cost/regularization branch for the depth interaction, not as champion.
No claim of architectural superiority from parameter savings. Width/pooling
rejections will be revisited only if the depth interaction changes this balance.

Admitted folded runtime prototype reuses BRN-3's tested square-local raw-sum
algorithm in verification, adding the optional self/context pass before typed
pooling. It expands shared rows to the same absolute float lookup for a comparable
cache cost. This deliberately does NOT convert trainable parameter savings into
an unsupported runtime-size claim: its folded payload remains about9.47MB.
Independent double versus folded float, transitions and immutable snapshot tests
are required before model diagnostics. Direct compact shared-row inference is a
separate later optimization if a family survives; no production identity yet.

Eleven targeted tests pass after adding self ablation and folded inference; zero
failures/errors/skips. The single parameterized gradient/resume test now exercises
30 configurations. Snapshot tests cover symmetric absolute/relative x sum/self/context
and300 transitions each, plus immutability. Admit micro self71, context97 and self97;
131k context71/97; and three micro interaction screens (absolute+context,
relative width16+context, directed+context). Their rationale is to test whether
earlier rejections change under the now-promising second pass, at bounded cheap
cost before an integrated family is selected. Each retains original controls and
300s/2GiB/single-process ceiling. No further combinatorial sweep is implied.

## A05 ablation/scale finding and bounded playing-screen admission

Micro context replicates: seed71/97 raw loss.15437/.14851 versus self-only
.16490/.15465 and plain relative.17453/.17424. The other-piece message supplies
additional micro benefit beyond a local extra layer, conditional on these seeds
and representation. At131k context.13273/.13040 does not beat absolute control
.12516/.12977 overall. However balanced-stratum losses.06693/.06243 are materially
better than absolute.08005/.08319; quarter aggregate loss is slightly worse.
Therefore neither raw aggregate loss nor training speed settles engine usefulness.
No promotion; retain this explicit trade-off for the smallest discriminating play
screen. Width/direction/absolute context micro interactions are still pending.

Admit frozen131k context versus matched131k absolute for both seeds to folded
validation,64 depth4 roots and16 separately bounded depth3 qsearch roots. Each
root has explicit node/time guards; incomplete searches remain failures, not
favorable censored measurements. For seed71, admit8 paired openings at depth4 and
16 paired openings at10ms/move (in bounded batches if necessary), exploratory
opening seed714091. Both use gain.25, production exact driver, one worker, identical
4MiB TT, fresh per-game state, equal depth1 warmup and2048-ply cap. A240s batch
budget records cancellation explicitly; wrapper300s is a final process bound.
No administrative cap becomes a draw. Complete-pair score/uncertainty and all
incomplete outcomes must be reported. A weak/inconclusive pilot cannot establish
inferiority; a promising result admits a fresh confirmatory plan, not promotion.
This modest campaign is justified by a replicated balanced-loss signal despite
aggregate loss, directly testing the known objective-to-strength mismatch.

## Folded diagnostics and final bootstrap checkpoint

All four scaled absolute/context checkpoints (two seeds) pass16384 validation
predictions: maximum float/cache error<3.8e-7 pawn, zero integer-score differences
versus their double reference. Each completes64 depth4 production-driver roots
and16 separate depth3 qsearch roots under the declared bounds. Context reduces
entered nodes modestly (324261/322834 versus333692/336879), but its extra pass
costs time: verification folded unrelated latency medians13.14/12.82us versus
absolute8.01/8.67us. These use the verification cache, not the original production
class; games explicitly route compatible sum controls through the production
Brn3Model/Workspace. An added transition test proves those sum snapshots compute
exactly the same floats/scores. Do not give the opponent a generic slow research
forward pass. Full validation still favors absolute aggregate and context balanced
loss; diagnostics do not settle playing strength.

Micro interactions do not beat relative8+context.15437: absolute+context.15848,
relative16+context.16001, directed+context.15690. They remain rejected at the micro
interaction gate. Absolute+context still improves its own micro baseline.17763,
so a larger-data absolute-context interaction remains admissible if needed after
the playing screen; prior absolute/relative ranking reversed with scale. No whole
design family is declared impossible from these experiments.

Current software evidence:13 distinct targeted tests, zero failures/errors/skips;
includes numeric gradients/optimizer/CRC, sparse transition/forbidden-input tests,
folded snapshots and match cancellation/terminal-rule/complete-pair accounting.
No full/slow suite, GUI/browser QA, long training, user-store writes or deployment.
Only main code change is R01 in Brn3Workspace; NNUE remains untouched.

## Reproduction and continuation

Read contract/state first; check Git and pending local processes before starting
work. Run one empirical process at a time. Java21/Gradle and Python are already
available. All output paths below must be new; change their final component for
a replay. The runner records actual compiled-byte hashes, Git HEAD, command,
timeout and completion/failure in adjacent execution receipts. Source changes
were never compiled concurrently with measurements. Early screens precede these
receipts; their compiled hashes and complete reports are retained separately.

```powershell
$env:DEBUG=''
.\gradlew.bat :app:compileVerificationJava :app:installDist --no-configuration-cache
$data='app/build/research/brn-learning/data-prospective-v2'
$root='app/build/research/brn-successor'
$model='app/build/research/brn-programme/r4-typed-eight-71/production-preview/network.brn3'
python tools/brn-successor.py baseline 12e1398 "$root/replay-original"
python tools/brn-successor.py runtime $data "$root/replay-original.json" --model $model --overlay "$root/replay-original/classes"
python tools/brn-successor.py train $data "$root/replay-control" --relations ABSOLUTE --positions 131072
python tools/brn-successor.py train $data "$root/replay-context" --relations RELATIVE --pool SUM_CONTEXT --positions 131072
python tools/brn-successor.py evaluate $data "$root/replay-context-eval.json" --model "$root/replay-context/selected.successor"
```

Use `--seed 97` for the second initialization, `--pool SUM_SELF` for context ablation,
and `--positions 16384` for screening. Modes coverage/cache run the recorded structural
and search-topology probes. `match CANDIDATE NEW_OUT --opponent STATE --pairs 8
--depth 4` is the admitted matched-depth pilot; `--depth 0 --millis 10 --pairs 16`
is equal time. Persist actual nextOpeningIndex before splitting/restarting a batch;
never overwrite or erase incomplete results. No final confirmation is authorized
by these examples alone: follow the contract's gates and recorded admissions.

If old ignored inputs are absent, reproduce their manifests through BrnResearchMain
prepare-uniform using the recorded source/range/navigation convention in BRN_V1.md;
verify hashes rather than silently substitute another dataset or model. Compact
tracked JSON evidence is authoritative for what was actually measured; ignored
checkpoints are reproducible local artifacts, not the only continuity mechanism.

## Bootstrap/R01 provenance boundary and remaining interaction admission

Checkpoint commit `709ef61` contains scoped governance, baseline evidence, R01 and
the verified experimental harnesses. It does not close the programme, promote an
architecture, finalize build33, or authorize release. Work started clean at
12e13981b3e3b6b478186c39f8ab6b55f3a6b16a on main. No push or user-store writes.

Before closing architecture questions, admit two remaining scale controls after the
current playing pilot: absolute+context width8,131k x8, seeds71/97; and original
absolute width16,131k x8, seed71. Micro rankings already reversed with data scale,
so a micro rejection alone is insufficient for these central capacity/depth
interactions. Each has the existing300s/2GiB bound; expected cost from measured
width8 is about2-3minutes for context and under5minutes for width16. Record width16
epoch4 as an approximate equal-training-compute comparison and epoch8 as equal
exposure; neither grants free extra capacity. Further width32 or combinations need
new justification from these results. This is empirical research, not routine QA.

## Playing pilot and recorder failure; context runtime experiment

Seed71 depth4 completes8 pairs:5W/6D/5L,score.5,paired bootstrap95%[.3125,.65625].
The initial10ms batch records11 complete pairs (5W/2D/15L,score.272727) before an
AccessDeniedException during atomic replacement of match.json. No model/search
failure was reported; the report publication failed. A concurrent read/file-sharing
conflict is plausible but exact lock ownership is not established. The wrapper
records the failed process. The next computed pair was not durably retained and
is unknown, not a win/loss/draw. Preserve initial11 and stdout/receipt. Resume in
a new output from nextOpeningIndex14 for5 pairs; no overwritten or cherry-picked
outcomes. The reporting-only fix writes immutable per-pair files and aggregate once
at the end. Inspect stdout/immutable pairs during runs; never live aggregate files.
Shared DataFiles and all production persistence remain untouched. Two match tests
pass after this bounded research-only correction.

Depth4 candidate/control total thinking33.39/17.93s highlights context runtime cost.
The11 recorded time pairs used11.221/11.216s, so the observed loss is not explained
by unequal total allotted compute. This partial pilot is not final acceptance.

Before rejecting contextual representation for prototype cost, admit a cheap
algebraic implementation experiment: Wself*h_i + Wother*(sum(h)-h_i)/(n-1)
equals (Wself-Wother/(n-1))*h_i + Wother*sum(h)/(n-1). Cache count-dependent
matrices, compute the broadcast once per perspective, then apply one local matrix.
This removes repeated per-piece division and duplicate transforms. No retraining,
parameters, calibration or learned function change. Double reassociation must meet
existing1e-4 pawn bound and retain all observed integer/root decisions. Preserve
original prototype via `baseline-context 709ef61 NEW_OVERLAY`; compare timings
and root identities before retaining. Only then may a separately labeled same-
opening exploratory time replay test whether the cost change matters in play.

## Context runtime result and production-cost correction

The five-pair continuation completes1W/3D/6L. Combined with the retained11 pairs,
the original time pilot completes16 pairs,6W/5D/21L,score.265625. The failed
publication and unknown unrecorded pair remain disclosed. After algebraic
factoring, three fresh-process original/factored latency medians are
13.919/10.053,11.967/8.739,11.755/8.687 microseconds. All64 depth4 root
scores/moves/nodes and16 qsearch completion/node identities match in all repeats;
maximum error3.23e-7 pawn and zero integer differences. Median depth4 aggregate
time2.424->1.579s. Retain factoring in the isolated context runtime. This does not
change production BRN-3. Same-opening exploratory replay completes16 pairs,
12W/3D/17L,score.421875,paired bootstrap95%[.25,.59375]. This is inconclusive
and supplies no playing-strength promotion. Timing-dependent move choices mean
the two pilots are not an exact causal strength estimate.

Audit correction: all earlier candidate training times compare the generic
research trainers. Current production BRN-3 already has optimized SIMD kernels.
The relative model's generic training advantage therefore is NOT an established
advantage over production BRN-3. Admit matched production controls using identical
seed+epoch order,128 batches,eight epochs,first4096 validation selection and
unchanged trainers/objectives. First reproduce16k seed71 and require exact selected
checkpoint bytes against generic ABSOLUTE. Then measure131k seeds71/97 and fresh
NNUE controls with the same exposure/selection. NNUE keeps its own established
Adam/objective/initialization/score mapping; equal exposure is not equal compute
or equal parameter count. Explicit vector module enabled for BRN3; capture trainer
class hashes. No optimizer or NNUE production changes are authorized by this fix.

The diagnostic harness now explicitly labels its partition and routes compatible
SUM controls through the production BRN3 workspace, matching the game harness.
Prior generic-cache timings remain labeled historical evidence; do not splice
them into a claimed production latency comparison. NNUE diagnostics use its
production float evaluator and existing V1 mapping; no artificial material term.

## Absolute-context interaction survives the scale gate

At131072 examples x8, absolute+context selects epochs5/3 for seeds71/97,
loss.12187215/.12188658 versus matched plain absolute.12516408/.12976969
(2.6%/6.1% lower). Balanced loss.06358/.05886 also improves, but quarter loss
.18960/.19225 is mixed versus.19100/.19132. Training129.34s for seed71
remains a generic-prototype cost, not an optimized production comparison.
This reverses the micro preference for relative+context and establishes that
parameterization/depth interact with data exposure. Retain absolute+context as
the admitted challenger, not the champion.

Admit full validation/numerical/search diagnostics for both seeds, followed by
16 equal-time10ms opening pairs each and8 depth4 pairs for seed71, using the
same exploratory opening seed714091. Fixed gain.25 initially; no silent
calibration advantage. Also admit absolute+SELF at131k x8 for both seeds to test
whether the extra local layer explains this larger-scale gain without other-piece
context. These are required integrated ablations, under the same300s/2GiB bounds.
Do not infer redundancy or message-passing benefit from only the relative micro
ablation. Width16 scale and these results determine the family frozen for fresh
range confirmation; no exhaustive grid, width32 or long campaign is admitted.

Width16 absolute at131k selects epoch8,raw loss.13148073,balanced.08554,
quarter.19075,training193.31s,5255489 parameters. At epoch4 (~97.48s,
approximately the generic eight-wide control's101.95..113.11s),loss.13881.
Neither the approximate equal-training-compute nor equal-exposure comparison
beats eight-wide raw loss; doubling state/table size supplies no demonstrated
Pareto advantage. Do not advance width16 or infer that all wider networks are
impossible. The independent original-research oracle test now covers width8/16
forward values and every optimizer weight/moment exactly; all7 newly run successor
candidate/match tests pass. Existing6 BRN3 tests remain earlier passing evidence.

The optimized production16k control reproduces the complete selected experimental
checkpoint byte-for-byte (SHA2566d687437e07388e3f3bde1c1eab4dc67edb67ddd703f6a106f49ad1c213d2fa3).
It takes6.13s training versus generic13.15s, comparable to relative6.09s. Thus
the generic ABSOLUTE quality control is valid despite misleading training-cost
comparisons. The production control's actual measured time is recorded separately.

Production131k controls also reproduce both selected checkpoints byte-for-byte.
Training takes49.84/51.58s for seeds71/97, versus generic113.11/101.95s and
relative45.63/50.92s. The relative branch retains a smaller training state but no
established large production-training speed advantage. Its folded deployment
still uses an absolute float table. Direct compact-relative deployment remains
deferred: no memory constraint has been identified for the current9.47MB shared
model, and the more predictive absolute-context branch is the admitted challenger.
No claim is made that folding is intrinsic to the relative architecture.

Absolute-context full16384 validation losses.11608006/.11820302 improve over
absolute.12143764/.12473957; maximum float error<1.7e-7 pawn,zero integer
differences. Both complete64 depth4 and16 qsearch roots. Equal-time exploratory
16-pair results are10W/3D/19L(score.359375,bootstrap95%[.1875,.546875])
and16W/5D/11L(score.578125,[.40625,.75]). The pooled point score is.46875;
do not select only seed97 or call this a strength improvement.

For subsequent game batches, strengthen symmetric JIT warmup before starting
the measured240s batch budget: eight deterministic depth4 roots per evaluator,
seed190413,alternating candidate/opponent order,fresh TT per root,1M-node/2s
guard per search; retain depth1 per-game warmup. Every warm root must complete.
The earlier pilots used only depth1 per-game warmup and remain exploratory.
This removes an avoidable startup concern, not a claimed explanation of the
observed losses. Freeze the same warmed protocol for all final references and
record it in each report. No extra model training or altered score mapping.

Seed71 absolute-context depth4 completes8 pairs,score.625,bootstrap95%
[.4375,.8125]. Together with mixed equal-time results, this justifies one more
bounded context-kernel experiment before freezing the runtime: interleave four
independent output dot products, retaining the ascending multiply/add order of
each output. The production head already exploits the same independence. Freeze
the tested factored source and its SHA256 in ignored `context-factored/src` before
editing; compile that overlay only after current empirical jobs finish. Require
targeted tests, three warmed repeat comparisons and unchanged root identities.
Before measurement, require at least3% lower median unrelated latency across the
three process medians and no more than3% median aggregate depth4 time regression.
Otherwise restore the frozen tested factored implementation. No retraining or
new topology is involved.

Absolute SELF ablation selects epoch8 for both seeds,raw losses.12478598/.12425120,
balanced.07099/.07536,quarter.18930/.18791,training121.34/120.01s. Context's
other-piece exchange improves raw and balanced losses on both seeds, so its effect
is not wholly explained by a local extra ReLU. However SELF quarter loss is lower
on both seeds. The known objective-to-play mismatch prevents rejecting this
ablation solely from raw loss. Admit full diagnostic checks and16 paired10ms
games per seed for SELF; rerun CONTEXT with the same new warmed runtime/protocol
on the exploratory openings. These are candidate-selection pilots, not independent
confirmation or fourfold independent evidence from reused openings. Keep both
seeds; compare pooled point scores and uncertainty without treating a noisy
selection as promotion. The resulting family/recipe will be frozen before any
new-range test or fresh confirmatory games are opened.

Four-output interleaving meets its predeclared bounded runtime gate: median of
three process latency medians9.242->8.816us(4.6% lower),aggregate depth4 time
1.781->1.791s(0.5% higher,within3% bound). Per-process timing variation is retained
in evidence; no search-speed improvement is claimed. Every raw validation metric,
64 root decision/node identity and16 qsearch node identity matches. Retain the
unroll in the experimental runtime only. Targeted7 tests pass after this change
and the warmed match protocol; SELF diagnostics also pass all roots and integer
parity. No production NNUE or BRN3 code changed in this stage.

## F01: frozen fresh-range confirmation protocol (before new data/results)

Warmed10ms pilots complete every pair: CONTEXT seed71/97 scores.421875/.4375;
SELF.328125/.5. Pooled points.4296875/.4140625 do not establish either a gain
against BRN3 or an ordering between the added layers. Freeze absolute width8
CONTEXT as the single confirmatory challenger because its raw/balanced predictive
gain replicates, the other-piece ablation has a measurable effect, and SELF has
shown no distinct strength or runtime advantage. SELF does not advance; this is
bounded selection under uncertainty, not proof of universal inferiority.

The unresolved question is whether replicated prediction/fixed-depth gains pay
for the extra inference at a broader search budget and on a fresh source range.
Use25ms/move, also used by the repository's prior BRN/NNUE reference comparison.
The unfavorable10ms evidence remains part of the final trade-off; a25ms win
would not erase it or establish dominance at every budget. No further architecture,
optimizer, calibration, checkpoint-selection or runtime tuning is permitted from
the confirmatory results. Alternative calibration/objectives remain deferred; A07's
observed objective-to-play mismatch is addressed by requiring actual games.

- New data starts at raw ordinal1953686, immediately after the exploratory range.
  Prepare131072 training,16384 validation and16384 sealed-test examples through the
  existing uniform-held-out/grouped-split reader and navigation index. Freeze its
  manifest before training. No game-disjoint or new-domain claim is possible.
- Fresh seeds71/97 for each unchanged production BRN3 trainer, CONTEXT challenger
  and unchanged production NNUE trainer. Eight epochs,batch128,identical seed+epoch
  shuffles,first4096 validation raw outcome-half-MSE checkpoint selection. Preserve
  each architecture's existing optimizer/objective/initialization. BRN gain.25;
  NNUE's existing V1 mapping. Equal exposure is not equal compute/parameter budget.
- Freeze selected state hashes before opening the test partition. Report all
 16384 test examples, material control, numerical parity,parameter/payload size,
  actual training cost,warmed inference,64 depth4 roots and16 separate qsearch
  roots. Original BRN3 and R01 use the same learned checkpoint; original workspace
  is loaded from12e1398 overlay. Require original/R01 root identities and three
  alternating repeat measurements; do not conflate implementation and learning.
- Challenger versus R01 BRN3:64 fresh opening pairs per seed at25ms,opening
  seed725031,start0,balanced legal-uniform6..10-ply sampler,colors reversed.
  Run in32-pair batches,continue exact nextOpeningIndex,check shared identities
  across seeds. No incomplete pair is a draw or a favorable exclusion.
- Primary strength evidence clusters the two seed results by opening identity:
 64 independent sampled opening clusters,256 games. Bootstrap10000 cluster
  resamples using fixed Python Random seed735611; report percentile95% interval,
  one-sided95% lower bound and conservative Hoeffding lower bound,plus each seed.
  Promotion requires pooled score>=.55,one-sided bootstrap lower>.5,both seeds
  >=.5,all planned games complete,nontrivial test learning versus material,and
  no>5% relative aggregate test-loss regression versus matched BRN3. These are
  evidence gates,not a claim of owner acceptance. If unmet,keep BRN3+R01 and
  report the challenger as unpromoted with the observed uncertainty.
- Three-reference comparison: same fresh BRN3 weights under original/R01 runtimes
  versus matching fresh NNUE,32 fresh pairs per seed at25ms on the same sampler.
  If CONTEXT passes promotion gates,also compare it with NNUE before integration.
  Never infer superiority to unseen mature/user NNUE checkpoints or other budgets.

Resource admission: measured cheap/scale gates support this bounded final stage.
One empirical process,2GiB Java heap,no user-store writes,no cloud spend. Individual
training/data commands retain300s timeout; match batches240s after bounded warmup,
wrapper300s. Prior same-exposure NNUE runs took224..242s,BRN3 now50..52s,and context
124..129s. Expected total sequential compute is under35minutes including games;
reassess any bound failure explicitly,without converting it to success. A longer
production campaign,hyperparameter sweep or extra candidate is not admitted.
Required architecture integration follows only a passed promotion gate. Failure
to promote is a valid outcome; no BRN4 identity will be manufactured.
