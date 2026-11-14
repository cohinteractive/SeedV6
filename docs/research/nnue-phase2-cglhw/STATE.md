# Phase 2 research state

Status: ACTIVE — all five training controls and both architecture endpoints assessed;
final runtime/cost checks pass. Preparing fixed equal-time screens; no scored games yet.
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
| A001 | WDL25's useful recipe survives independent initialization; controls separate supervision, coverage and decay | Freeze matched-seed reproduction and ablations; run first4-superbatch parity pilot, then fixed64 only after it passes |
| D001 | Existing source metadata can support stronger independence claims | Audit prepared chunk/chain semantics and existing train/validation identities; produce a bounded disjoint diagnostic if justified |
| B001 | Compact nonlinear downstream capacity can beat width expansion economically | Inspect pinned trainer operations; choose a small residual/downstream pilot, measure actual invocation cost before long play |
| B002 | Stable pawn relationships supply efficient capacity | Design a bounded symmetric pawn-pair vocabulary with independently testable delta maintenance; first measure refresh/update economics |
| S001 | Technically qualified candidates compete with production WDL25 | Fresh fixed paired screen; independent confirmation only after candidate selection and prospective family decision |

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
The first cost-driver invocation failed at import before any trials; the corrected
`raw/comparative-cost-02.log` and original failure are both preserved.

`training-curves.json` retains all320 A001 and128 architecture superbatches and
common midpoint diagnostics. Deep32->64 teacher/outcome MSE .017062134/.069296538
to .016659301/.068576093; pawn32->64 .017391336/.070726159 to .017086385/.070142244.
Both still improve; neither is declared converged or an architectural ceiling.
Selected initial game endpoint remains64. If strength/cost evidence leaves a
credible direction, consider a separately frozen duration/compactness revision;
do not reject a whole family solely from these endpoints or extend until lucky.

Next: freeze `screen-plan.json` through `run_screens.py --runtime-version 3
--prepare-only`, recheck final UCI runtime, then execute/audit the eight initial
64-game paired screens (five training controls, deep16, pawnpairs, exact-weight
kernel control). All against protected WDL25, same32 roots,200ms/W1/Hash32/CLV_C1,
with fixed-node warmup and -Xbatch. Each match<=2400s; aggregate initial<=14400s.
This is a bounded selection stage, not confirmation or an equivalence proof.

Threadline CREATE `17030feb-f225-4713-9aa4-c3065adcc796` published atomically to
`C:/dev/tl-local/seed-phase2-488fb569-3e18-4aa7-9a56-225b48001b3d.json`.
Schema/semantic/size checks and exact final bytes pass (`threadline-bootstrap-publication.json`).
Dedicated Playwright lists only about:blank; Threadline staging is not observed.
No Apply performed. Owner review/manual Apply is non-blocking for this research.

Completion criteria: NOT SATISFIED. No new strength winner or promotion recommendation.
