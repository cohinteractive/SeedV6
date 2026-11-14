# Phase 2 research state

Status: ACTIVE — C001 passed all predeclared gates; owner acceptance is not claimed.
R001 bounded clock/worker robustness is qualified and frozen for execution.
Observed start: 2026-11-14T10:26:04Z. Initial HEAD08877838, clean worktree,
build101; no exact-root CODEXLOG_CURRENT.md. Branch research/nnue-phase2.
No production change or acceptance is authorized.

## Verified starting point and bootstrap challenge

Read the Phase 1 REPORT and supporting recipes/manifests at
../conventional-nnue-cglhw/strength-20261114/. Its fixed512-game WDL25/CP64
confirmation is 211W/183D/118L, +63.82 Elo, descriptive paired-bootstrap95%
+41.59..+87.29 at W1/Hash32/200ms/CLV_C1 on Windows Java21. Preserve that result;
it does not establish a universal gain, live rating effect or multiworker transfer.

Phase 1 already includes a same-seed broad-data teacher-only control versus
WDL25, and a narrow/broad teacher-only control with matching early decay. These
are useful contrasts, but tiny32-game CP64 screens neither isolate causal Elo
contributions nor reproduce the recipe across seeds. CP64 differed in seed and
decay. CP64 means checkpoint64, not width64. The correct512 experiment's failure
on narrow data does not exclude adequate larger architectures.

Training uses pinned Bullet579b78b, AdamW, sparse king buckets, training-only768
factorizer, dual256 SCReLU and8 material heads. Runtime i16/int/long quantization
uses QA255/QB64/scale400. Research512 codec is rejected by production. A dedicated
research UCI launcher explicitly forces CLV_C1; use it for every primary match.
Do not rely on generic evaluator-aware routing for experimental weights.

The broad67,108,864-record mixture is from two extractions of Jan/Feb2024 BT4.
Provenance carries prepared-unit/ordinal, not proven source-game identity. The
6,753-position holdout filters exact/symmetric and one-piece-neighbor leakage;
game independence remains unproved. Source metadata/decoder audit is selected
before claims of clean validation. Publisher says these older labels used FEN
without real history; history-fixed later data is a different source, not silently
interchangeable. No download or external model reuse is currently selected.

Host inspection: Ryzen5500/RTX4060 8GiB, CUDA available, 253.7GB disk free;
visible Java processes are IDE/Gradle infrastructure, no active trainer/match.
Existing compute permission NORMAL with no active operation. This permits bounded
isolated Windows research, not Mac access. Recheck before expensive launches.

## Selected work programme

| Unit | Hypothesis/decision | Status and next condition |
|---|---|---|
| A001 | WDL25's useful recipe survives independent initialization; controls separate supervision, coverage and decay | Five64-game screens audited; broad25% reproduction is plausible, no isolated causal Elo or established superiority |
| D001 | Existing source metadata can support stronger independence claims | Complete: chunk separation established, game independence unproved; D002 supplements input-frequency accounting |
| B001 | Compact nonlinear downstream capacity can beat width expansion economically | Deep256+16 is correct and diagnostically better, but its first64-game screen is negative; B003 tests a cheaper formulation |
| B002 | Stable pawn relationships supply efficient capacity | Pawn256 endpoint is correct but its first64-game screen is negative; B003 tests lower-cost128 formulation |
| S001 | Technically qualified candidates compete with production WDL25 | All512 games audited without caps; only reproduction has positive point estimate, with interval crossing zero; no confirmation selected |
| D002 | Broader records may still repeat evaluator inputs | Complete:32.48M additional-only fingerprints;2.83% of added records match original; source-game separation remains unproved |
| B003 | Depth/relations can replace some accumulator width | All three64 endpoints qualified, zero allocation, matched initializers and full curves; warmed comparative cost complete |
| S002 | Compact128 cost/capacity trade-offs can compete in equal time | All192 games audited, no caps: direct+49, deep-22, pawn-11 Elo; none independently confirmed |
| B004 | A matched duration extension can resolve remaining training adequacy | All three128 endpoints qualified;988.99 additional native seconds; unchanged geometry/cost, improved diagnostics |
| D003 | Quantization may change predictive comparisons or calibration | Complete:6753 positions/model, Java aggregates match, predictive ranking unchanged; no demonstrated material quantization limitation |
| A002 | More training of standard256 may change the recommendation | Complete:64 exact optimizer transitions,320.77 native seconds, all qualification gates pass |
| S003 | Equally trained128/256 formulations can compete in equal time | All256 games audited without caps; standard+38,direct+22,pawn+11,deep-16; exploratory only |
| C001 | The selected compact direct model improves over production WDL25 | Complete: 179W/204D/129L, +34.04 Elo, paired95% +10.86..+56.82; all three gates pass |
| R001 | The confirmed candidate transfers to longer clocks and two workers | Qualified/frozen: two fixed 64-game conditions, 7200 total match seconds; final outcomes pending |

This is a dynamic decision table, not a mandatory implementation checklist.
Conditional subnetworks, other activations, widths, quantization and threat features
remain hypotheses; expansion needs a specific information-bearing reason.

Initial economics: A001 at most5 matched recipes ×64 superbatches, <=1 native
GPU-hour expected ceiling /2 supervisor wall-hours; all actual costs retained.
Architecture feasibility pilots: <=1 GPU-hour/family before reassessment; no
strength test before parity and measured cost. Initial development allocation
<=4 match-hours, confirmation provisionally<=6 hours with sample/gates frozen
separately. These are revisable resource bounds, never completion criteria.
Training and match decisions must account for source uncertainty, seed effects,
selection bias and equal-time search-cost interactions.

## Restart

Read CONTRACT.md, this file and selected experiment protocol/results. Check Git
diff, protected hashes and nnue-control.cmd status. Preserve all completed and
failed raw directories; verify exact recipes and receipts before resumption.
No full engine suite, GUI/package build, deployment, push or promotion is selected.
Build101 remains unchanged for uncommitted/research-tooling work.

## First execution findings

A001 reproduction reached checkpoint64. The259 Python/Rust/Java probes pass exact
feature/integer/full-refresh agreement and GPU float tolerance. Trained legal-walk
incremental checks and real UCI lifecycle pass; measured incremental allocation0.
Common6753-position teacher/outcome MSE .017226444/.069416296, versus incumbent
.017243709/.069774644 in Phase1. This is diagnostic reproduction, not demonstrated
equal-time strength. Native endpoint and exact model identity are in
`a001-replica-assessment.json`; raw evidence remains under `raw/a001-replica-*`.
All five matched controls have now completed through `train_controls.py`.
`a001-diagnostic-summary.json` verifies exact same initial tensor-file hash across
the five recipes, so these conditional contrasts do not mix initialization seeds:

| Recipe | Teacher MSE | Outcome MSE |
|---|---:|---:|
| Broad/.25/early reproduction | .017226444 | .069416296 |
| Broad/teacher-only/early | .016906156 | .069371195 |
| Broad/.50/early | .018637482 | .070623216 |
| Narrow/.25/early | .019793377 | .073171972 |
| Broad/.25/late | .018182926 | .070808034 |

Coverage and earlier decay improve these diagnostics conditional on this seed and
WDL25 recipe. Teacher-only still beats outcome mixing on loss; neither loss ranking
nor the worse50% outcome model establishes the playing-strength ranking. No factorial
interaction or numerical decomposition of the historical63.8-Elo gain is established.

D001 (`d001-data-audit.json`) confirms2048 training units disjoint from16 original
diagnostic units and zero reuse between extractions. Three original validation
chunks neighbor training chunks within the same source shard. The packed decoder
has no global game identity; complete self-contained BINP chains do not prove that
the original game never reappears in another base/chunk. Both months are in training,
so this is not a source-month holdout. Source outcomes are traceable labels, not
an independent audit of the original game/adjudication. These distinctions improve
the independence assessment without inventing unavailable game separation.

Pinned loader inspection establishes global batch-offset continuation modulo data
length; broad64 sees four passes, narrow64 eight. Checkpoint segmentation does not
reset every process to the first minibatch. AdamW's pinned state is momentum/velocity
plus weights; its update does not maintain an omitted bias-correction step counter.

Initial launch failed before training because the new raw/ parent did not exist;
created the intended parent and the unchanged pilot completed. A first automation
attempt correctly refused to re-enter a lifecycle-completed pilot. The driver now
verifies and skips complete endpoints, then starts the new continuation directory.
Failed driver log `raw/a001-replica-driver.log` is retained; successful continuation
log is `raw/a001-replica-driver-02.log`. No partial checkpoint was accepted.

Architecture design/prospective gates are `B001-B002.md`. Isolated trainer built
successfully; initial Java validation passed22 targeted tests (13 conventional,
1 production baseline,4 prior research and4 Phase2). Exact-state/score/search
comparison to CP64 and hostile bounded SIMD arithmetic pass. Subsequently expanded
promotion/capture pawn-relation fixtures need their targeted rerun before games.
Three Python feature/quantization and two new warmup tests pass, along with five
existing match-budget/durable-publication tests. Full engine suite deliberately
not run. Java/native logs: `raw/java-validation-01.log`, `raw/native-build-01.log`.
Original trainer and both protected networks remain byte-identical.

B001 deep16 pilot completed4 superbatches in24.78 native-process seconds; all base
initial tensors exactly match A001 (`b-deep16-initializer.json`). Its first export
correctly failed independent validation: the new Python oracle incorrectly rounded
the product to binary32 before quantization. One merged weight0.5039215683937073
times255 is below128.5 in binary64; the extra binary32 rounding changed128 to129.
Pinned Bullet promotes to binary64 before multiplication. Inspection also established
that Bullet raw.bin saves untransformed named tensors, not merged/transposed float
weights; the oracle now constructs float inference from optimizer tensors and checks
raw.bin against its actual semantics. No trainer/network fix or retraining was needed.
Original failed assessment/log remains; corrected assessment uses the distinct
`-r2` output and driver log `raw/architecture-driver-02.log`. Further training is
gated on that corrected independent parity result, not on waiving the failure.

The bounded SIMD kernel is a separate implementation change. An exact-payload WDL25
base-envelope benchmark is selected to separate kernel cost from architecture cost;
architecture-vs-production strength would otherwise conflate both contributions.

B001 reached64 without retraining/forking. First endpoint assessment passed feature,
float, integer and trained-delta checks, but correctly stopped at the allocation
gate:680 bytes/evaluation (40 per dot,17 dots), with update+head about1.44us. Its
diagnostic teacher/outcome MSE .016659301/.068576093 improves the common controls;
this remains no strength evidence. `raw/b-deep16-64-assessment-r2` is retained.

JFR (`kernel-correction.json` and referenced raw profile) identifies Int256 iota
shuffle allocations. Static species/combined reduction alone did not fix them.
Caching the high-half rotation and converting part0 removes measured allocations:
0 bytes/100000 update+head calls; trained parity still passes. Expanded Phase2
tests pass again (`raw/java-validation-03.log`), including related-pawn promotion,
non-pawn capture of a related pawn, mirroring, null/transfer and exact SIMD arithmetic.
One diagnostic JVM launch failed before startup due to PowerShell splitting an
unquoted -XX argument; the quoted retry and original failure log are both retained.

The first frozen runtime `frozen-runtime-v1.json` contains the allocating kernel
and is NOT qualified for games. `frozen-runtime-v2.json` is the corrected current
runtime; the original416-opening pool/32+256+128 partitions are unchanged. No games
were played with v1. `architecture-runtime-v1.json` separately preserves the exact
runtime of the first architecture assessments. `native-trainer.json` identifies
the frozen Phase2 executable; --trainer can select its exact bytes on later resume.
`raw/architecture-driver-03.log` owns current reassessment and pawn-pair execution.
Use new assessment directories for changed validation, never overwrite prior output.

## Completed architecture/cost milestone and selected next work

Both architecture64 endpoints pass259-position independent feature/GPU/quantized
parity, raw incremental/full recomputation, real UCI lifecycle and zero measured
hot allocation. Deep16 teacher/outcome MSE .016659301/.068576093; pawnpairs
.017086385/.070142244. Pawnpairs are not uniformly better diagnostically and cost
~1.05us/update+head versus production~0.74us. No strength ranking follows.

The first activation-cache implementation was slower (~2.10us) and is retained as
`raw/kernel-r4-*`. Explicit SIMD preparation and independent dense reduction chains
reduce deep16 to~1.26us, allocation0, with unchanged trained scores (`raw/kernel-r5-*`).
This adds2048 scratch bytes per deep accumulator, ~0.50MiB per257-ply state, not a
measured whole-worker RSS claim. Expanded four-test Phase2 suite passes again in
`raw/java-validation-04.log`. Current selected runtime is **frozen-runtime-v3.json**;
v2 remains the valid pre-cache control, v1 is unqualified for games. All opening
partitions are unchanged; no games occurred during kernel revision.

`comparative-cost.json` uses separate JVMs, a complete retained warmup pass, two
measured trials and reversed process order. Mean measured NPS: WDL25 production
652422; same-payload bounded kernel689226 (+5.64%); deep16 before activation cache
446557, after506193 (+13.35%); pawnpairs444057. WDL25 kernel controls and deep
before/after controls match nodes/scores/depths/completion/configuration on all48
requests each. Different learned networks change trees, so their NPS ratios are
not intrinsic kernel speedups or Elo. Cache/branch hardware counters were not collected.

The separate instrumented fixed-root runs in the endpoint assessments count
2,110,114 evaluations/2,352,072 nodes for deep256 and2,170,190/2,424,898 for pawn256
(about90% each). Feature additions per child are8.47 and14.47; perspective
refreshes per child .198 and .202. These are different learned search trees, not
an intrinsic paired-cost experiment. They show that downstream evaluation is
frequent and that stable pawn relationships still add meaningful update work.
The first cost-driver invocation failed at import before any trials; the corrected
`raw/comparative-cost-02.log` and original failure are both preserved.

`training-curves.json` retains all320 A001 and128 architecture superbatches and
common midpoint diagnostics. Deep32->64 teacher/outcome MSE .017062134/.069296538
to .016659301/.068576093; pawn32->64 .017391336/.070726159 to .017086385/.070142244.
Both still improve; neither is declared converged or an architectural ceiling.
Selected initial game endpoint remains64. If strength/cost evidence leaves a
credible direction, consider a separately frozen duration/compactness revision;
do not reject a whole family solely from these endpoints or extend until lucky.

`screen-plan.json` is frozen and the final UCI runtime passed all nine preflights
(incumbent plus eight candidates). The first match's actual protocol verifies
200ms/W1/Hash32/CLV_C1,600-ply censoring,2400-second ceiling, -Xbatch on both sides
and two fixed-node warmup passes. No interim results guide selection.

Preparation/source/evidence committed as `1341c622da309bfa8aa619a1a54eee453aeb9ef3`
on research/nnue-phase2; no push. Build101 and production asset bytes are unchanged:
this commit affects isolated research sources/tooling, not the shipped application.
Raw models, training output, profiles, runtime copies and games are verified ignored.
CRLF-aware staged whitespace/JSON/scope checks passed; no active Git hook/finalizer.

Completed operation: `run_screens.py --runtime-version 3`, driver log
`raw/screen-driver-01.log`, observed exit0. Do not rerun its completed games.
It executed and audited the eight initial
64-game paired screens (five training controls, deep16, pawnpairs, exact-weight
kernel control). All against protected WDL25, same32 roots,200ms/W1/Hash32/CLV_C1,
with fixed-node warmup and -Xbatch. Each match<=2400s; aggregate initial<=14400s.
This is a bounded selection stage, not confirmation or an equivalence proof.
Frozen order: deep16, replica, narrow, teacher, wdl50, pawnpairs, kernel, late.
Completed-match reports appear as screen-NAME-results.json, with independent legal
replay/artifact audits. Review whole endpoints; preserve interrupted/failed evidence.

First completed screen: deep16 is8W/33D/23L, no caps, exploratory-82.97 Elo,
paired-bootstrap95% -149.78..-27.20 (`screen-deep16-results.json`). Independent
legal replay and all frozen artifacts pass. Disposition: reject this64 endpoint
at200ms against WDL25; do not infer a family-wide capacity limit. Remaining S001
comparisons continue unchanged. B003 is now prospectively selected: matched128
direct/deep/pawn models to test replacement of accumulator width by representation,
with independent correctness, measured cost and fixed game endpoints. See B003.md.

D002 is also selected (D002.md): a bounded probabilistic input-overlap audit of
the frozen broad mixture, not a claim of game independence or an instruction to
change datasets. Its initial process-level queue failed at the handoff described
below; the audit subsequently ran after all eight screens released the controller.
Logs/output are raw/d002-input-overlap*. It did not compete with timed games.

Second completed screen: independent-seed replica22W/26D/16L, no caps,
exploratory+32.67 Elo, paired95% -32.67..+100.42; legal/artifact audit passes
(`screen-replica-results.json`). It is a plausible candidate, not an established
improvement or equivalence proof. Source-level and sampling independence are not
implied by the new initialization seed.

Third completed screen: narrow-data14W/23D/27L, no caps, exploratory-71.57 Elo,
paired95% -136.97..-10.86; legal/artifact audit passes
(`screen-narrow-results.json`). This endpoint is uncompetitive against WDL25.
Together with the matched broad replica, it supports retaining the broad mixture
for the next architectures, without isolating diversity from sample weighting,
passes per record, or fixed-order effects. Same-root screening contrasts are
selection evidence, not independent confirmation or a multi-seed causal estimate.

Fourth completed screen: teacher-only16W/25D/23L, no caps, exploratory-38.15 Elo,
paired95% -106.35..+27.20; legal/artifact audit passes
(`screen-teacher-results.json`). Better diagnostic loss did not translate into a
better screen score than the WDL25 reproduction. Neither this small result nor
the cross-screen point difference establishes an optimal outcome ratio.

Fifth completed screen: WDL50 is14W/21D/29L, no caps, exploratory-82.97 Elo,
paired95% -143.33..-21.74; legal/artifact audit passes
(`screen-wdl50-results.json`). Reject this50% recipe endpoint against WDL25.
Its weaker common diagnostics and strength screen provide no reason to transfer
50% supervision to the compact experiments. Retain their prospective25% recipe;
no general optimum across architectures, seeds or data has been established.

Sixth completed screen: pawnpairs256 is12W/28D/24L, no caps, exploratory-65.92 Elo,
paired95% -130.69..-5.43; legal/artifact audit passes
(`screen-pawnpairs-results.json`). Reject this64 endpoint at200ms against WDL25.
Both initial256 architecture additions are uncompetitive here. Their improving
training curves and cost penalties leave the already selected compact128
replacement hypothesis unresolved; do not call either family inherently harmful.

Seventh completed screen: unchanged-weight kernel control19W/20D/25L, no caps,
exploratory-32.67 Elo, paired95% -77.25..+10.86; legal/artifact audit passes
(`screen-kernel-results.json`). Exact fixed-node score/tree equivalence and the
measured throughput gain do not establish equal-time strength. This screen does
not support a strength-improvement claim or a production change; a small true
gain remains unresolved. Preserve this negative endpoint without re-running it.

Eighth completed screen: late-decay16W/29D/19L, no caps, exploratory-16.30 Elo,
paired95% -77.25..+43.66; legal/artifact audit passes
(`screen-late-results.json`). Early decay has better diagnostics and a higher
screen point estimate in this matched seed; the small strength samples do not
establish the isolated effect. S001 finished successfully (driver exit0) at15:08Z.

`s001-summary.json` aggregates the512 audited games and10,887.57 observed match
seconds. Post-hoc shared-opening score contrasts (replica minus each control,
percentage points, descriptive paired95%) are teacher+10.16[-3.91,+24.22],
WDL50+16.41[+2.34,+29.69], narrow+14.84[+3.91,+26.56], late+7.03[-5.47,+18.75].
These are scores against WDL25, not direct head-to-head Elo; they are not
multiplicity-adjusted acceptance, independent confirmation, or multi-seed effects.

The PowerShell follow-up queue incorrectly treated Get-Process.ExitCode after
WaitForExit as failure despite the unified exec observing the screen driver's
actual exit0. Both dependent queues stopped before D002/build/training. This is a
supervision handoff failure, not a failed match or a reason to repeat games.
Recovered from the verified eight audited reports and controller status using
directly observed child exits. Neither stopped queue remains active.

D002 completed in9.90s, all three synthetic fixtures pass, exact67,108,864-record
input hash verified (`d002-input-overlap.json`). It found65,642,886 fingerprints
overall,33,164,045 original and33,164,910 additional, with686,069 shared and
32,478,841 additional-only. Of the additional33,554,432 records,951,243 (2.83%)
match original fingerprints. Total duplicate records are1,465,978 (2.18%). The
broader mixture substantially expands input coverage; this does not isolate the
Elo contribution of diversity from training frequency or prove game independence.
The63-bit canonical hash is probabilistic; the stated random-collision upper
approximation is .000488 expected pairs, not a collision-free guarantee.

B003 implementation and initial endpoints are **qualified for screens**: CompactNetwork/
Accumulator/VectorHead and probes/tests, explicit S6P2N002/hidden128, separate
phase2_compact.rs executable, width-aware Python oracle and run_compact.py.
Java compile and all eight selected tests pass (four CompactNetworkTest and four
existing Phase2NetworkTest, zero failures/errors/skips). Research workspace installed;
full engine suite deliberately not run. `raw/compact-java-validation-01.log`.
Native compact build passed in15.80s (`raw/compact-native-build-01.log`). The width
oracle's six known-arithmetic cases pass; re-exported256 deep/pawn files are
byte-identical and all259 accumulators/scores match each prior Java fixture
(`raw/compact-codec-validation-01/summary.json`). Original trainer binaries,
protected WDL25 and VERSION101 hashes remain unchanged. Runtime v4 is frozen
(`frozen-runtime-v4.json`). `run_compact.py --runtime-version 4` finished with
observed exit0. All4-SB pilots and64 endpoints passed their gates; do not retrain
completed recipes. Native wall totals986.64s for the trio.
The direct128 control reached64 and passed259 exact parity probes, GPU tolerance,
trained incremental/full checks and UCI; zero hot allocation. Teacher/outcome MSE
.017893933/.070308064, quantization RMS5.96 link units, update+head about386ns
on the fixed walk (`compact128-base-64-assessment.json`). Prediction loss is
slightly worse than the256 reproduction; the later common cost comparison and
fixed strength endpoints are recorded below.

Deep128+16 also reached64 and qualified: all259 parity probes, GPU tolerance,
trained incremental/full and UCI pass; zero allocation, update+head about668ns.
Teacher/outcome MSE .016535340/.069377701, quantization RMS8.48 link units,
mean absolute link381.88 (`compact128-deep16-64-assessment.json`). Teacher loss is
slightly better than deep256 while this fixed-walk cost is much lower; this is a
credible efficiency hypothesis, not evidence of equal-time strength.

Pawn128 also qualified: teacher/outcome MSE .017446663/.070600296, quantization
RMS5.88, about636ns/update+head, zero allocation and all259 parity/UCI gates pass.
`compact128-cost.json` completes the149.23s warmed two-repeat/reversed-order study:
mean NPS WDL25 production645837, exact-weight kernel695930, direct128923086,
deep128714819, pawn128624967. All48 unchanged-weight requests match exactly.
Different learned trees mean these are not intrinsic kernel speedups or Elo.
All base initializer tensors match across the three compact recipes. Full loss
curves are retained; final-quarter means continue improving modestly.
S002.md selects all three for one fixed screen each with explicit economic
justification. Confirmation/robustness partitions remain unused.

Completed S001/D002, compact implementation/qualification and frozen S002 plan
committed as1507592eb4aa556555503de69d8c6ad7609dbe6e on research/nnue-phase2.
No push; build101 retained because these are isolated research changes. All58
staged files were inspected, JSON/Python syntax and CRLF-aware whitespace checks
passed; ten representative bulk evidence paths were verified ignored. No raw
models, checkpoints, games or runtime copies were staged. Four final-runtime UCI
preflights pass; actual S002 protocol verifies200ms/W1/Hash32/CLV_C1/600-ply cap.

First S002 endpoint: deep128+16 is17W/26D/21L, no caps, exploratory-21.74 Elo,
paired95% -82.97..+38.15; independent legal/artifact audit passes
(`screen-compact128-deep16-results.json`). Its point result is closer to WDL25
than deep256's, but there is no established advantage or cross-width causal Elo
estimate. Direct128 and pawn128 continue unchanged in the frozen order.

Second S002 endpoint: direct128 is25W/23D/16L, no caps, exploratory+49.18 Elo,
paired95% -10.86..+112.33; independent legal/artifact audit passes
(`screen-compact128-base-results.json`). It is the best screen point estimate so
far and a credible confirmation candidate, not an established improvement or
ranking. Its weaker prediction loss and higher measured throughput reinforce
the distinction between diagnostic accuracy and practical equal-time strength.
B004 was predeclared/committed before this result and continues unchanged.

Third S002 endpoint: pawn128 is14W/34D/16L, no caps, exploratory-10.86 Elo,
paired95% -77.25..+60.31; independent legal/artifact audit passes
(`screen-compact128-pawnpairs-results.json`). Direct128 leads the exploratory
point estimates; neither added representation has established an advantage.
S002 finished successfully; controller NORMAL/idle/zero owned processes verified
before B004. Available disk219,805,708,288 bytes at that check.

B004 is prospectively selected from the near-competitive deep128 result and
still-improving compact loss curves: continue all three matched models once to128,
with exact optimizer/data/schedule continuation and unchanged inference. This is
a test of training duration, not an assertion that undertraining caused the result.
Budget1800 native seconds for the trio; no compute starts until S002 releases the
controller. New model-dependent gates and a separate strength decision follow.
Prospective protocol and initial driver preparation committed as
2b05699dcdc3101b2eda7fd247a73d59278719eb before either remaining S002 result.
`continue_compact.py` completed the three exact-state continuations after verifying
all S002 audits, with observed exit0. Driver log
raw/duration128-driver-01.log. Runtime v4 and the frozen compact trainer are
reused; duration128-plan.json pins protocol, parent receipts and exact Python
tool bytes. Its guard treats an audited cap as censored rather than a missing
game; actual S002 had zero caps. No checkpoint128 strength test has started.
Do not retrain completed endpoints; inspect controller ownership before new work.

Direct128 reached checkpoint128 and passed all259 parity/GPU/incremental/UCI
gates, with zero hot allocation and about383ns/update+head. Teacher/outcome MSE
improves from .017893933/.070308064 to .017420831/.069720512; mean absolute link
468.38 and quantization RMS7.79 (`compact128-base-128-assessment.json`). This
narrows the diagnostic gap to the256 reproduction at unchanged inference
geometry, without establishing a strength gain. The remaining endpoints and
common warmed comparison completed next, as recorded below.

All B004 endpoints and the common cost comparison subsequently completed.
Additional native seconds: base282.89, deep314.06, pawn392.04 (988.99 total).
All128 receipts use global LR .000125 and exact optimizer continuation. Last16
mean training losses are .015818625/.01450825/.014761125; improvements from the
preceding16 means have diminished to roughly0.3–0.4%, without proving convergence.
Pawn128 at checkpoint128 qualifies with teacher/outcome MSE
.017149732/.070462918, RMS quantization error7.45, zero allocation and about642ns
update+head. Warmed mean NPS in compact128-128-cost.json: production630067,
fast-kernel687663, direct953499, deep735745, pawn624167. All48 equal-weight control
requests match exactly. These learned-tree NPS differences are not Elo evidence.

Deep128 at checkpoint128 has also qualified: teacher/outcome MSE
.016184664/.068778838, zero allocation, about674ns/update+head and all259
parity/incremental/UCI gates pass. Sampled quantization RMS10.13 and maximum52.73
link units (`compact128-deep16-128-assessment.json`). D003 is selected before
any128 games: quantify float-versus-quantized teacher/outcome loss, signed/RMS
error and material-head breakdown on the same6753 diagnostics for all three
final128 models. Use existing independent Python oracle, cross-check quantized
aggregates against Java, retain per-position data under raw/quantization-128.
Budget600s/one CPU, only after B004 releases the controller. No new labels,
model changes, strength inference or claim of source-game independence. This
tests whether quantization is an observed limitation rather than assuming it.

D003 completed in30.64s (`quantization-128.json`), all6753-position quantized
aggregates matching independent Java assessments. Float-to-quantized teacher-MSE
deltas: base+0.00001239, deep+0.00003673, pawn-0.00001634; outcome deltas
-0.00001030/+0.00006625/-0.00004708. Ranking is unchanged. Deep's teacher delta is
about0.23%; its low absolute-score calibration persists in floating inference
(391.85 vs391.76 quantized), rather than being an export-induced collapse.
RMS score errors are7.40/9.92/7.85, with full per-position evidence under
raw/quantization-128. This does not prove quantization irrelevant to games, but
does not identify a major predictive limitation warranting QAT in this programme.

A002.md closes a remaining control gap before any128 games: standard256 receives
the same eight-pass training endpoint as the compact models. Its independent-seed
64 parent and original trainer hash57a9ee3c... are verified; it uses the existing
conventional kernel. Thus no width-only causal Elo claim is intended. Budget900s
supervised training/1800s overall, plus at most2400 game seconds if qualified.
`continue_standard.py --runtime-version 4` completed with observed exit0, log raw/a002-driver-01.log;
a002-plan.json pins the protocol, parent, trainer, tool and runtime identities.
Its64 optimizer initial states match their immediate parents byte for byte.
A002 teacher/outcome MSE .016887551/.069223508,259 parity/GPU checks and UCI pass,
zero allocation; standard network SHA25679de7381cf98c24a3e04304c50f92392e3792464a1810c0d136fd0e874cafeaa.
The new182.37s controlled cost study compares all four endpoints together:
WDL25 production653885 NPS, fast-kernel684325, direct128955203, deep128734965,
pawn128620493, standard256655453; all48 equal-weight requests match exactly.
These different learned-tree rates are not intrinsic speedups or Elo. S003.md
selects one fixed screen per128 endpoint with the predeclared9600-second combined
allowance. Independent confirmation/robustness partitions remain unused.

Completed operation: `run_duration_screens.py --runtime-version 4 --candidates base
deep16 pawnpairs replica256 --decision docs/research/nnue-phase2-cglhw/S003.md`.
Driver log raw/duration-screen-driver-01.log; plan duration128-screen-plan.json.
Frozen order: duration128-pawnpairs, duration128-deep16, duration128-replica256,
duration128-base; driver observed exit0. Do not repeat its completed games.
No further training/model variants are selected.

Duration/quantization controls, S002 outcomes and frozen S003 preparation committed
asdbbdfc0c6b116695af4759bffcb3e4a4f245e336. All28 staged files were inspected;
JSON/Python syntax and CRLF-aware whitespace checks pass,13 bulk evidence paths
are verified ignored. Full training/game/per-position outputs remain local-only.
Build101, WDL25 and CP64 bytes remain unchanged; no push or promotion.

First S003 endpoint: pawn128 at checkpoint128 is19W/28D/17L, no caps,
exploratory+10.86 Elo, paired95% -60.31..+77.25; legal/artifact audit passes
(`screen-duration128-pawnpairs-results.json`). It is inconclusive and supplies
no established gain despite improved diagnostics. The other three fixed
comparisons continue unchanged; no independent confirmation is yet selected.

Second S003 endpoint: deep128+16 at checkpoint128 is16W/29D/19L, no caps,
exploratory-16.30 Elo, paired95% -77.25..+43.66; legal/artifact audit passes
(`screen-duration128-deep16-results.json`). Improved diagnostics from duration
do not establish an equal-time benefit. Across the tested widths/durations,
neither implemented nonlinear/relational addition has a demonstrated gain.
Further variants are not selected without a concrete new information-bearing
limitation; these are conditional results, not global architectural capacity limits.
The standard256 and direct128 duration screens continue unchanged.

Third S003 endpoint: standard256 at checkpoint128 is24W/23D/17L, no caps,
exploratory+38.15 Elo, paired95% -21.74..+106.35; legal/artifact audit passes
(`screen-duration128-replica256-results.json`). This is promising but unconfirmed;
the point estimate is close to its64 reproduction's+32.67, so duration has no
established causal strength gain here. Only the direct128 final screen remains.
Wait for that full audit before selecting or freezing independent confirmation.

Final S003 endpoint: direct128 at checkpoint128 is21W/26D/17L, no caps,
exploratory+21.74 Elo, paired95% -43.66..+94.55; legal/artifact audit passes
(`screen-duration128-base-results.json`). All15 development endpoints are complete:
960 audited games, no caps,20,371.17 observed match seconds (5.659h), with no pooled
strength estimate. `screening-summary.json` records every endpoint/hash and the
post-hoc duration contrasts. All four duration score-difference intervals include
zero; improved diagnostic loss did not establish a duration strength gain.

C001.md selects **direct128 checkpoint64**, SHA256
defaf21da465d6f63fc13a2299299e927029d82558d663c7bbc801e7c35e9d1c,
for exactly one independent512-game confirmation. Highest exploratory point+49.18,
strong measured cost profile, full correctness qualification; not a statistically
established ranking over the close standard256 controls. Select this existing
endpoint rather than the lowest-loss/latest model, and retain all alternatives
as unconfirmed. No alternate primary confirmation or unlucky-result retest is selected.

The reserved256 confirmation pairs match the original pool and were verified legal,
unique, previously unused in Phase2, and disjoint from development/cost/parity roots. Related
opening-family dependence can remain. Runtime v4,200ms/W1/Hash32/CLV_C1,-Xbatch,
two20k-node warmup passes,800-ply censor ceiling and14400s total match budget.
Gates:512 complete/uncensored and audited; both paired normal95/bootstrap95 lower
score bounds>0.5; point Elo>=20. Passing advances to separately qualified robustness,
not production promotion. The128 robustness pairs remain unused.
The entire256-pair confirmation partition is now consumed/reserved by C001's
single fixed attempt; do not reuse it to select or test an alternate candidate.

An unexecuted preparation-only driver assumed Elo fields exist for censored
summaries. Corrected before any preflight/game: absent statistics now leave the
practical gate false and report null, without imputation. Initial c001-plan.json
and source snapshot raw/c001-preparation-v1 are preserved. **Active plan is
c001-plan-v2.json**; all scientific parameters compare exactly equal to the first
plan, only reporting safety/driver identity changed. No completed protocol was rewritten.

C001 completed with driver exit0 and independent audit exit0 at
2026-11-14T22:14:33Z; controller idle, zero owned processes. Log
raw/c001-driver-01.log, native output raw/c001-direct128-64. All 512 games are
uncensored: 179W/204D/129L, 54.8828%, +34.04 Elo. Paired bootstrap95%
+10.86..+56.82 and normal95% +11.15..+57.22. Legal replay and frozen artifacts
pass; technical, statistical and practical gates all pass (c001-results.json,
c001-decision.json). No development pooling or acceptance claim. Do not repeat
these games or modify frozen runtime/models/openings/protocols. R001.md selects
bounded robustness; only subsequent research source/tool revisions may change.
Both C001 preflights pass; the actual native protocol matches the selected model
and incumbent hashes,200ms/W1/Hash32/CLV_C1,800-ply censor ceiling,14400s and
family size1. REPORT.md now consolidates the completed evidence and explicitly
marks C001/results/research completion as pending. A read-only comparison of
existing cost observations matches144 WDL25 v3/v4 bounded requests exactly
(runtime-v3-v4-equivalence.json); this is not a new/exhaustive runtime test.

Robustness preparation finding (static inspection): NnueStrengthUciMain explicitly
accepts only one worker, and uci_smoke.py rejects strength-research workers2 even
though generic UCI advertises a wider Threads range. Existing worker-transfer unit
checks are not an observed two-worker research search. If a candidate approaches
promotion suitability, actual multiworker testing needs isolated research-launcher/
harness enablement, a new frozen runtime and targeted parallel evaluation-state/
UCI checks. Do not infer worker2 coverage from the advertised option. This does not
block the current fixed W1 tests; no such enablement or robustness game has occurred.
Completed S001 used immutable v3; these source edits did not change its games.
Always inspect controller/process ownership before resuming. Logs use
raw/compact-*-01.log and raw/compact-driver-01.log.
The compact training driver retained full loss curves and completed its prospective
reversed-order cost comparison. `run_compact_screens.py --runtime-version 4
--candidates base deep16 pawnpairs --decision docs/research/nnue-phase2-cglhw/S002.md`
finished with observed exit0; driver log raw/compact-screen-driver-01.log. Its
frozen order was deep128, direct128, pawn128. All192 games passed independent
endpoint audits. Do not repeat completed games or modify the frozen artifacts.
Python syntax/CLI checks are preparation checks; completed runtime/model gates
are separately evidenced above. S003 subsequently froze its qualified models
and prospective decision. All five final-runtime UCI preflights pass. The actual
first-game protocol verifies200ms/W1/Hash32/CLV_C1,600-ply censoring and2400s limit.

Threadline CREATE `17030feb-f225-4713-9aa4-c3065adcc796` published atomically to
`C:/dev/tl-local/seed-phase2-488fb569-3e18-4aa7-9a56-225b48001b3d.json`.
Schema/semantic/size checks and exact final bytes pass (`threadline-bootstrap-publication.json`).
Dedicated Playwright lists only about:blank; Threadline staging is not observed.
No Apply performed. Owner review/manual Apply is non-blocking for this research.

Material S001/D002 UPDATE candidate `4ba7d558-313d-4735-b82e-a9f818e8cc84`
published atomically to
`C:/dev/tl-local/seed-phase2-checkpoint-bcfb0919-9d3b-4d5f-a995-6adb3c0eb529.json`.
2430 bytes; SHA25636d7d62d4a41c94da9b66859054a86d66ad958fe27f955e9bc8bece0261c96bf.
Current V1 schema/semantic/size checks and exact final bytes pass. Unknown current
unresolved items and grouping are preserved by omission. Dedicated Playwright
again shows only about:blank: staging is NOT OBSERVED; Apply NOT PERFORMED.
Manual target assignment, sequence/current-state review, V1 legacy/freshness
approval and Apply remain with the owner, non-blocking for repository research.
See `threadline-s001-checkpoint.json` and `threadline-s001-publication.json`.

C001 pre-result UPDATE candidate `bc66e42c-e5b1-440b-9b24-545be867f5ec`
published atomically to
`C:/dev/tl-local/seed-phase2-confirmation-bfb51800-2aa9-43a5-b820-03d105dc6f1b.json`.
2770 bytes; SHA256cb0db99e2e69f7b27d6d77316570cee90630b664c8de7ed3946eccc5be0e8365.
Current V1 schema/semantic/size checks and exact final bytes pass. Dedicated
Playwright still exposes only about:blank: staging NOT OBSERVED, Apply NOT
PERFORMED. Manual target/sequence/current-state review, V1 legacy/freshness
approval and Apply remain with the owner and do not block research. Unknown
unresolved items/grouping remain omitted. Publication manifests retain all details.

Completion criteria: NOT SATISFIED. Direct128 checkpoint64 is independently
stronger under the primary tested conditions; R001 qualification/robustness and
final synthesis remain. No promotion or deployment is authorized.

## R001 execution boundary

Qualification passes in r001-qualification.json (raw/r001-qualification-r4).
The actual trained candidate's 82,474 evaluations match independent full rebuild,
including 29,784 helper-thread evaluations across four 20k-node searches. Every
root includes helper and qsearch work, legal completed PVs, node-budget unwind,
drained workers and depth3 new-game restart. Eight targeted Java tests and five
Python match/warmup/durability tests pass. Both models pass real UCI lifecycle
and resolved CLV_C1 configuration at W1/W2. Sixteen bounded W1 requests match
exact moves, nodes, scores, depths and PVs across frozen v4/v5.

Runtime v5 changes only the research launcher's accepted worker counts and the
engine-core jar's sourceRevision metadata. All other jar entries are identical;
major/minor/patch/build remain unchanged. This is bounded qualification, not
cross-platform proof. The active plan is r001-plan.json; preserve all pinned
tools/runtime/models/openings until both matches and audits finish.

Retained preparation failures: missing compute_gate import before work; overly
strict whole-jar equality before recognizing sourceRevision-only metadata;
a test assumption that qsearch calls initializeFrom (ExactEvaluator actually
uses one absolute-ply stack); and an attempt to configure Python chess's managed
Ponder option. All were harness assumptions, with no observed evaluator mismatch
or scored games. Logs r001-qualification-driver-01..05.log and attempts r1..r4
remain local. The failed parallel helper source/classes are preserved in r2.

Next/current driver: run_robustness.py, log raw/r001-driver-01.log. Run/check its
process and nnue-control status before resuming; never launch duplicates. First
64 games at400ms/W1, then64 at200ms/W2, each on32 distinct unused robustness pairs.
Raw outputs raw/r001-long and raw/r001-workers. No interim efficacy decisions,
sample extension, retest or pooled score. Driver produces independently audited
r001-long-results.json, r001-workers-results.json and r001-summary.json. Remaining
64 robustness pairs are unused and are not an automatic extension allowance.
