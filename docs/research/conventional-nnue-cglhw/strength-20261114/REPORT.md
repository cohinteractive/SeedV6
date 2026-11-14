# NNUE strength campaign — 14 November 2026

New owner-authorized evaluator research; historical E001–E009 protocols and
results remain frozen. Start observed 2026-11-13T23:48:22Z. Initial Git HEAD
`dc58c03e1abe45a422631d3f4331374413f0a578`, clean; build 100. No root
CODEXLOG_CURRENT.md. Production CP64 is protected, with no automatic promotion.

## Completed campaign result

**WDL25 checkpoint 64 is the selected research incumbent.** Independent fixed
confirmation against the protected CP64 finished 211W/183D/118L in 512 games
(256 paired roots), score 59.082%, estimated **+63.82 Elo**. The ordinary paired
bootstrap 95% interval is +41.59..+87.29. The predeclared conservative 97.5%
decision intervals are +37.79..+90.57 (paired normal) and +38.15..+90.91
(paired bootstrap); both pass. All games and frozen artifacts pass independent
audit; no game was capped. This establishes a meaningful improvement at the
tested Windows/Java21 W1, Hash32, 200ms-per-move conditions. It does not establish
a universal +50 Elo gain across clocks, platforms or worker counts.

The gain comes from a retrained **256-unit conventional architecture**, with
broader existing data, earlier learning-rate decay and 25% real-outcome targets.
The 512-unit architectural experiment was correct but slower and rejected in
development play. Production CP64, production configuration, search, board rules,
feature inventory and build 100 remain unchanged. No production promotion is made.

Selected local model: `raw/wdl25-64/exports/final/network.cnnue`, SHA256
`a25e76fcdfe1b854452685bec87bc759bed2ed166312579e024410f0903517ab`.
The full resumable optimizer endpoint is
`raw/wdl25-64/segments/000064-950382f144b5/run/seed-cnnue-v1-64`.
These are ignored local artifacts; the tracked recipes, manifests, measurements
and this report identify them. A fresh Git checkout does not contain the weights
or datasets. `confirmation-wdl25-results.json` contains the audited result and
per-game evidence hashes; the immutable selection policy is `confirmation-plan.json`.

## Protected baseline and initial decisions

CP64 means E007 checkpoint 64, **not a 64-unit network**. Source and bundled
manifest specify 7,680 features (10 mirrored king buckets × 12 piece/color
channels × 64 squares), dual 256-unit accumulators, SCReLU and eight material
heads, selected by `(pieceCount-2)/4`. Both kings are features. Black orientation
flips ranks; the perspective king selects horizontal mirroring. No further hidden
layer or explicit relational features. i16 weights, int accumulators, long head
arithmetic; QA=255, QB=64, output scale=400; clamp ±32511. Optional Java 21 Vector
API preserves exact integer arithmetic. Incremental state copies parent sums,
applies changed-square deltas, and refreshes a perspective on king-context change.

Protected file `build-assets/nnue-candidate/conventional-cp64.cnnue`: 3,940,952
bytes, SHA256 `8431626c2fca81e0a1e9ccd5e45ddd8829e6e3282c756c21fad75b06d075bb2b`;
payload `5da0253642ab33b8e446b5b1c5764750be008cd3dbacc7cd1992a95bf4a881f6`.
Actual production source selects CLV_C1, not README's older MW_C4 description.
All comparisons must explicitly bind identical CLV_C1 search, clock, worker/hash
settings; the generic evaluator-aware factory otherwise routes unknown weights
to older search. No search implementation or default is to be edited.

Bullet pin `579b78b37a694cacd6e9ede4706386c15c34837c`, MIT; Rust wrapper adds a
training-only 768-feature factorizer, merged before quantization. AdamW, sigmoid
MSE, teacher-only BT4_Q_LOGIT400_V1, batch 4096 × 1024, seed 20261022, initial LR
.001, halved every 128 superbatches. CP64 saw 268,435,456 exposures. Historical
checkpoints 128/256 lowered training loss without useful held-out improvement;
their tiny same-search screen did not establish a strength difference. They are
evidence against blindly extending that recipe, not evidence of a capacity limit.

Existing E006 dataset: 33,554,432 shuffled training records, 524,288 validation
records, sampled from local January/February 2024 BT4 source shards. Chunk-disjoint
validation and exact placement/STM dedup are documented, but game/symmetry and
near-neighbor separation are unproved. Scores are probability-link units, not
original centipawns. Original source/checkpoints/datasets remain untouched.

Host: Ryzen 5 5500 (6 cores/12 threads), approximately 32 GiB RAM, RTX 4060 8 GiB,
Java 21; existing local CUDA libraries and native trainer are present. Compute
control was NORMAL, no active operation. Use its existing single-workload
supervisor. Upper budgets: 12 GPU-training hours and six match-testing hours;
these are ceilings, not spending targets.

## Prospective sequence

1. Audit and rehash existing data; form a deterministic diagnostic holdout that
   excludes exact and single-piece placement neighbors under perspective/mirror
   symmetries. Do not assume source-game independence where IDs are unavailable.
2. Run focused existing correctness tests and warmed CP64 inference/search costs.
3. If data supports meaningful experiments, test fresh matched-width controls and
   a 512-unit network with unchanged features/activation/heads. Consider a modest
   WDL mixture as a separate same-width training experiment, preserving link scale.
   Start with short runs, then fixed pilot exposure endpoints. No loss-only winner.
4. Independently verify Bullet float, merged/quantized export, Python integer and
   Java fresh/incremental inference before play. Keep experimental implementation
   in research source sets wherever practical.
5. Freeze paired opening screens and equal wall-clock settings before matches.
   Only promising candidates earn fresh independent confirmation; report paired
   uncertainty and censored games. Production acceptance remains separate.

## Reference inspection

The pinned [Bullet input-bucket example](https://github.com/jw1912/bullet/blob/579b78b37a694cacd6e9ede4706386c15c34837c/examples/progression/3_input_buckets.rs)
supports the current feature/factorizer/SCReLU/material-head formulation with a
wider transformer, WDL blending and cosine learning rate. These are hypotheses
for SeedV6, not claimed strength gains.

Current [Stockfish architecture source](https://github.com/official-stockfish/Stockfish/blob/master/src/nnue/nnue_architecture.h)
uses K32H3, FullThreats and PP_3Wide features, 1024 transformed dimensions, 32/32
nonlinear stages and 192 stacks. Its [architecture history](https://github.com/official-stockfish/nnue-pytorch/blob/master/docs/nnue_architecture_history.md)
identifies SFNNv20 at commit `26647279081cdc39d72ef9f10c33289a7da4a5e9`.
This motivates capacity and relational hypotheses but also a much larger Java
incremental-cost/validation burden. No Stockfish code or model is imported.

The [BT4 publisher's current dataset card](https://huggingface.co/datasets/xushawn/test80-bt4-relabel)
identifies ODbL licensing and says January/February labels used FEN alone; real
history reconstruction is associated with later `-histfix` files. This limits
the current supervision's treatment of repetition/history. No replacement data
or third-party weights were downloaded.

Execution results and any material stopping condition follow below.

## Data audit and first fixed recipes

All three E006 data/provenance hashes matched. Of 8,192 evenly spaced validation
positions, 411 overlap training under placement symmetry/STM-insensitive matching;
1,289 have a one-piece placement neighbor (including those exact overlaps). The
label-blind filtered diagnostic holdout retains 6,903 positions, hash
`52368d3a54827fb19870cb8924877bb45d64ab1c1e24289ac201a8017bc038af`.
It is more conservative than exact evaluator-input dedup; it still cannot certify
source-game independence. It is used for bounded diagnostics, never independent
strength confirmation. Audit took 36.4 seconds; source data is unchanged.

Fresh 256/512 controls use seed 20261114, identical E006 data, features, heads,
teacher-only objective and quantization. LR .001 halves every 32 superbatches,
an earlier decay than CP64. Stage 1 is four superbatches (16,777,216 exposures);
a parity/convergence pass permits continuation to a fixed 64-superbatch endpoint
(268,435,456 total exposures). This separates width effects from the new common
training schedule. Neither model is a continuation of CP64. A same-width WDL
mixture remains a possible later experiment, not an already completed result.

The initial existing core tests passed (13 ConventionalNetwork + one production
baseline); four new research tests pass across both widths, special moves, sibling
updates/undo, null/qsearch transfer, independent workers, corrupt codecs and exact
SIMD raw arithmetic. Frozen CP64 research-clone inference and CLV_C1 search scores/
node counts match its production implementation on the selected test roots.

## Fixed comparison policy (before playing candidates)

Primary screens: sixteen fresh paired opening roots, 32 games per candidate,
100 ms per move, one worker, 32 MiB hash, persistent warmed-over-play Java 21
processes, Vector API, no book/tablebase, unchanged CLV_C1 and BOUNDARY2 on both
sides. Formal python-chess endings/claimable draws; 600-ply cap is censored,
never a draw. No score adjudication or search tuning. Record all artifact hashes
and actual commands in each protocol. The 256-unit training candidates use the
original production inference kernel; only 512 uses the isolated research kernel.

The small screens are rejection/selection evidence. A positive result may justify
a separately frozen confirmation, within the six-hour aggregate match ceiling.
Freeze its sample and candidate before play; do not extend it based on interim
scores. Exact confirmation size will be selected from observed match costs,
before any confirmation game. No +20–30 or +50 Elo claim is warranted unless
independent paired results and their uncertainty support it. If screens reject
the first candidates, assess an outcome-mixed 256-unit training control before
concluding that evaluator research has reached diminishing returns.

Current tooling fixes are research-only: 512x8 is an explicit S6CNU003 identity;
S6CNU001/002 retain their bytes/geometry. Production rejects S6CNU003. Trainer,
exporter, receipt/resume verification and existing opt-in training retention now
understand this bounded geometry. The native config also accepts the existing
`explicit-v1` retention marker, which the supervisor already passes through;
without that compatibility fix a managed run would fail deserialization.
Synthetic geometry tests (2), existing retention tests (23) and owner-control
tests (14) pass. Original trainer binary is retained under its SHA256 path.

Initial command failures are preserved: one unquoted PowerShell JVM option
failed before the audit started; one export's Bullet feature subprocess lacked
the process-local CUDA DLL path. Corrected invocations passed using new output
locations. No partial result is treated as validated.

## First endpoint measurements

Both models reached checkpoint 64 with exact optimizer continuation and passing
259-position parity, real UCI searches/stop/reset/quit, and trained legal-walk
incremental/fresh agreement. Total native training-process wall time (including
startup/data verification/checkpoint I/O) is 730.91 seconds = 0.203 hours.
This is a conservative GPU-active-time ceiling, not a profiler utilization integral.

| Model | Held-out teacher MSE (6,903) | Outcome MSE | Median update+head ns | Fixed-root NPS |
|---|---:|---:|---:|---:|
| cp64 | 0.019006811 | 0.072541280 | 730.4 | 388906 |
| width256 | 0.018470366 | 0.071897774 | 738.3 | 486366 |
| width512 | 0.019191618 | 0.072407305 | 1354.8 | 195828 |

All measured hot incremental allocations were zero. The microbenchmark uses one
255-transition legal walk; first timing samples still show warm-up effects, so
all repeats are retained. Search costs use 12 fixed roots, two rotated measured
rounds after warm-up, depths 1 to 7 and bounded requests. Evaluations change node
counts/types: these NPS figures are not intrinsic kernel speedups or Elo evidence.

On the fixed 1,024-position float comparison, quantized minus float teacher MSE
is only about 0.000008 to 0.000015. Quantization RMS error is 7.38/8.75/8.38 link units
for CP64/256/512. Capacity is therefore not established as the main bottleneck;
the wider net fits training better but generalizes less well at this endpoint.
Earlier decay plus fresh initialization is a provisional improvement in the
256 control, not an isolated causal proof about learning rate or a strength result.
Mean absolute link scores changed (438/476/500); all retain the exact logit400
semantics and none saturated the static score clamp. No search compensation is made.

## Coverage hypothesis (selected before the wider screen completes)

The first 256 screen is 11W/8D/13L, no caps, 46.875%, exploratory Elo -21.7,
paired bootstrap 95% -112.3..+65.9. Lower loss has not demonstrated stronger play.
The wider network's lower training loss and worse held-out fit make additional
capacity without better generalization a weak next investment.

Next controlled test: sample another 33,554,432 records from the **existing local**
BT4 prepared source, explicitly excluding all 1,040 E006 training/validation
chunks. No external download or replacement of established data. Preserve source,
label conversion, filtering and per-source dedup. Mix old/new data 50:50 by
alternating complete 4,096-record minibatches, including aligned provenance.
This produces 67,108,864 records; do not claim global placement uniqueness or
game independence. Reaudit the original diagnostic sample against the entire
mixture and compare all relevant controls on the same resulting holdout.

Keep width 256, eight heads, teacher-only target, seed 20261114, LR .001 halved
every 32 superbatches and 268,435,456 exposures. Fresh four-superbatch smoke,
parity/convergence gate, then fixed 64 endpoint. Thus coverage changes while
architecture, target and total exposure remain fixed. This is a hypothesis about
sample diversity, not a claim that source-history limitations are repaired.
Extraction uses the previously established 10 GiB heap/two active processors;
17 GiB free RAM was observed before preparation. All expensive work remains
sequential under owner control; do not process data concurrently with matches.

## Screen audits, coverage inputs and retention

Both 32-game screens pass independent legal PGN replay, outcome, opening/color,
move-record and frozen-artifact checks. The 512 result is 7W/9D/16L, no caps,
35.9375%, Elo -100.4 with exploratory paired bootstrap 95% -221.1..0.0. It is
rejected for further training on this dataset/recipe; this is screening evidence,
not a universal claim about wider NNUEs. No new research incumbent is established.

New extraction: 33,554,432 train / 524,288 reserved diagnostic positions, source
chunks disjoint from E006. All training targets/results/provenance and all file
hashes pass verification; 66,560 distributed packed boards match Bullet. The
50:50 mixture has 67,108,864 records, SHA256
`3a4d8d009e730e2700cf5552b48a165c7cc044d84b3bdb6518a3108d440a5729`.
Its global audit retains 6,753 of the same 8,192 diagnostic samples; retained rows
are a verified subset of the earlier holdout. New holdout SHA256
`2742eb3acdfcc0d6c45e040fd1183e48f9cd430174b2542078570fa7e3ea8271`.
Prior losses are not compared directly across those different holdouts.

The existing opt-in training retention workflow removed 1,004 disposable files
(12,028,777,008 logical bytes) from **only this campaign**'s four completed runs.
Full checkpoint 4 and 64 optimizer endpoints, all exported networks, receipts,
logs, partial failures, parity, validation and match evidence remain. All four
cleanup operations finished and reverified with zero residual targets. Completed
run directories are terminal; use a new run and a retained endpoint for further
training. No inherited dataset, checkpoint, research or production file was removed.
Selection and cleanup manifests are retained alongside this report.

## Coverage endpoint and outcome-target hypothesis

On the common 6,753-position holdout: CP64 teacher/outcome MSE is
0.019167440 / 0.072436851; same-width 33M retrain 0.018689798 / 0.071888393;
67M coverage model 0.016829663 / 0.069677913. It passes all parity, float/quantized,
trained incremental, resource identity and UCI checks with unchanged inference cost.
The coverage screen nevertheless finishes 10W/12D/10L, no caps, Elo 0 with paired
bootstrap 95% -88.7..+88.7. Independent replay/artifact audit passes.
Loss improvement is not playing-strength acceptance.

Next and separate training-objective control: width256, same 67M mixture, same
seed/batches/LR schedule and total 268,435,456 exposures, but wdl=0.25. The pinned
Bullet `crates/bullet_lib/src/value.rs` explicitly forms
`target = blend * result + (1 - blend) * sigmoid(score / 400)`. Genuine STM game
results were already verified for every source training record. Thus this is a
25% outcome/75% teacher mixture, retaining logit400 inference semantics. Fresh
initialization, four-superbatch smoke/parity gate, then a fixed 64 endpoint.
The hypothesis is improved playing-oriented supervision, not lower teacher loss.
No learning-rate, architecture, data or search adjustment accompanies it.

The exact 47-file research runtime is preserved in `raw/frozen-runtime` with its
tracked hash manifest. Coverage and subsequent play use that same Java snapshot.
New match diagnostics record full client-side per-move elapsed time; historical
UCI nodes/time fields can omit unfinished iteration work and are labeled as
reported metrics. Mocked wall-budget/terminal-resume tests and three existing
atomic match-publication tests pass. No statistical decision uses interim
bootstrap results; final paired analysis remains unchanged.

## Direct incumbent continuation control (prospective)

Fresh 256-unit controls share the same new initializer, but original CP64 used
seed 20261022. A direct continuation tests a different practical route: preserve
the incumbent's learned weights and full AdamW state while broadening its data.
Source is the verified retained E007 checkpoint 64, read-only; original artifacts
are never updated. Use the existing explicit data-fork mechanism and a new output.

Keep the original seed 20261022, LR .001, half-rate every 128 global superbatches,
batch4096 x1024, teacher-only objective, geometry and quantization. The declared
67M data mixture is the sole recipe fork. Initial stage65 to 68 verifies optimizer
continuity and export correctness; then continue69 to 128 if coherent. This adds
268,435,456 exposures to the historical 268,435,456; it does not claim exact
continuation of the old sampling stream. No search retuning accompanies it.

Historical E007/128 (`raw/e007-evaluation128/network.cnnue`, SHA256
`69bc000a7f265cf50537c815a056911ab3e046315420aea89e4ad2f1d159b147`)
is a preserved old-data reference at the same global exposure count and schedule.
Its earlier search experiments do not establish strength under current CLV_C1.
A new, explicitly labeled current-search comparison may use these unchanged
weights; no historical protocol or result is reopened or rewritten. Its discarded
optimizer means new float re-verification of that old endpoint is unavailable;
its retained export/parity evidence and strict loader remain the identity basis.

## Independent confirmation selection - WDL25

The fixed outcome-mixture screen finished **12W/15D/5L**, no caps, score60.9375%,
Elo+77.25, exploratory paired-bootstrap95% +21.74..+149.78. Its independent
PGN/outcome/artifact audit passes. Four development comparisons used the same
16 roots, so this result is selection evidence, not acceptance or a final gain.

This new observation changes priority: freeze WDL25 checkpoint64 for independent
confirmation. **The prepared incumbent continuation has not run** and is deferred
while confirmation executes. No candidate or training change occurs during it.

Predeclared endpoint:512 games/256 fresh paired roots,200ms/move,600-ply cap,
W1/32MiB, CLV_C1 flags 3329490943, BOUNDARY2, Vector API, no book/tablebases,
exact frozen runtime/model hashes. The new roots are verified disjoint from all
16 development roots and the generator's recorded historical exclusions.
Prior screens consumed about1263.5 match seconds. A19,800-second confirmation
wall budget leaves aggregate testing below the approximately six-hour ceiling.
The bound counts observed prior work on resume; process-kill/startup overhead
is not an exact resource ledger. Incomplete or capped testing cannot establish
the planned positive decision. No interim efficacy stopping or extension.

Ordinary95% paired intervals are descriptive. The positive decision uses both
paired-normal and percentile-bootstrap97.5% lower bounds above50%, a conservative
Bonferroni allowance for at most two independent confirmation attempts in this
campaign. A second attempt is only conditional on a separately supported candidate,
fresh roots and sufficient remaining budget; it is not automatic. The detailed
immutable plan is `confirmation-plan.json`.

## Implementation and reproduction

- Research Java implementation: `research/src/main/java/com/ohinteractive/seedv6/core/nnue/conventional/Strength{Network,Accumulator,VectorHead}.java`.
  Production `Conventional*` implementations remain unchanged. The research loader
  reads original formats and explicit 512-unit S6CNU003; production rejects S6CNU003.
- `NnueStrengthUciMain` in the existing research-launcher source set binds the
  same explicit CLV_C1 configuration to either evaluator. All 256-unit matches
  use the original production kernel. No search, move generation, board, clock or
  TT-policy implementation is edited.
- Training/export changes are under `tools/conventional-nnue`; geometry-aware
  verification also extends the existing `tools/training_lifecycle.py`.
  `ValidationSeparationAudit.java`, `mix_strength_data.py`, `assess_strength.py`
  and `strength_float_check.py` retain deterministic selection and verification.
- Actual commands, file identities and measurements are in the assessment JSONs,
  each raw assessment's `commands.json`, and each match's `protocol.json`.
  Python/chess versions and confirmation tool hashes are in `confirmation-tools.json`.
  Rust is 1.89.0; Bullet and Cargo.lock remain pinned. The current trainer is
  preserved as `build/conventional-nnue/frozen-trainers/57a9ee3cd70334a5c6a6bbe3ea42b58985179da4fa81f166f4e14b5540b2c8e3/seed-conventional-nnue.exe`.
- Use the existing RUNBOOK's process-local CUDA DLL path and compute supervisor.
  Reproduction needs the local ignored datasets/checkpoints; Git alone does not
  contain them. Verify their manifests before use. GPU reductions are not promised
  bitwise reproducible; frozen exported bytes define the exact tested models.
- Completed/retired run and export directories must not be reused. New experiments
  require copied recipes with new output paths and a deliberately retained compatible
  optimizer endpoint. The existing controller rejects recipe or geometry drift.
- The 512 network is 7,881,816 bytes versus CP64's 3,940,952 bytes. Numeric accumulator
  storage per 257-ply state is approximately 1.00 versus 0.50 MiB, excluding object
  headers, placements, other search state and TT. These are layout calculations,
  not measured whole-worker RSS. Hardware cache counters were not collected.

No full engine test-suite ritual, GUI/native-package acceptance, installation,
release or production promotion is part of this evaluator research. Qualification
so far is Windows/Java21, one search worker and the explicitly recorded clocks;
other worker counts, platforms and long controls require separate evidence.

## Final measurements and interpretation

All development screens used the same 16 paired roots and 100ms per move; their
intervals are exploratory selection evidence. Confirmation used 256 newly frozen,
disjoint paired roots and 200ms per move. Both sides always used the identical
CLV_C1 search implementation, flags 3329490943, BOUNDARY2, W1, Hash32 and hardware.

| Candidate / endpoint | W / D / L | Estimated Elo | Paired bootstrap interval | Status |
|---|---:|---:|---:|---|
| Fresh256, 33M teacher, screen | 11 / 8 / 13 | -21.74 | 95% -112.33..+65.92 | No demonstrated gain |
| Width512, 33M teacher, screen | 7 / 9 / 16 | -100.42 | 95% -221.14..0.00 | Reject this recipe |
| Coverage256, 67M teacher, screen | 10 / 12 / 10 | 0.00 | 95% -88.74..+88.74 | Loss-only improvement |
| WDL25, 67M, screen | 12 / 15 / 5 | +77.25 | 95% +21.74..+149.78 | Selected for confirmation |
| WDL25, independent confirmation | 211 / 183 / 118 | **+63.82** | **97.5% +38.15..+90.91** | **Research incumbent** |

No caps occurred in any screen or confirmation. The final audit replays all 512
games, checks both colors/opening histories, 1,024 PGN/detail files, legal moves,
formal outcomes and frozen model/runtime hashes. Terminations: 329 checkmates,
174 threefold repetitions, 3 fifty-move draws, 6 insufficient-material draws.
The screen and confirmation estimates are not pooled. Only one independent
confirmation attempt was used; the more conservative two-attempt allowance
remains in the reported decision interval.

The completed training-chain audit (`training-final-results.json`) verifies all
256 superbatches and 252 exact optimizer-state continuations across eight staged
runs. Four fresh candidates each received 268,435,456 exposures: 1,073,741,824 total.
Native training-process wall was 1,364.405 seconds, **0.379 GPU-hours upper bound**,
including CPU verification, startup and checkpoint I/O. This is not a utilization
integral. Terminal training losses were .011422 (fresh256), .009544 (width512),
.012576 (coverage) and .015184 (WDL25); data/objective differences prevent treating
these as a common ranking. All three fresh256 experiments shared exact initial
weights and optimizer state. Original CP64 had a different seed and schedule.

Common 6,753-position diagnostic losses:

| Model | Teacher sigmoid MSE | Actual-outcome MSE |
|---|---:|---:|
| Protected CP64 | .019167440 | .072436851 |
| Fresh256, 33M | .018689798 | .071888393 |
| Coverage256, 67M | .016829663 | .069677913 |
| WDL25, 67M | .017243709 | .069774644 |

The coverage-only model has the lowest diagnostic losses but did not establish
stronger play. WDL25's confirmed strength therefore cannot be selected from loss
alone. Its mean absolute output is 468.8 link units versus CP64's 437.4; neither
reaches the static clamp on this set. Quantization/score scaling was unchanged.
On the separate fixed 1,024-position float diagnostic, WDL25's float/quantized
teacher MSE is .017735595/.017656413, RMS difference 7.665 link units, maximum 34.048.
On the 259 cross-language probes, its GPU float error is 1.22e-6 or less (limit 2e-5)
and quantization RMS/max error 7.094/23.993. Integer features, accumulators and
scores agree exactly across independent Python, Java and Bullet checks.

In the same final assessment, median incremental-update-plus-head cost was
746.3ns for CP64 and 776.7ns for WDL25 (roughly 1.34M/1.29M evaluations per second).
The first timing repeat remains visibly cold; all repeats are retained, and the
small difference is not a robust speed regression claim. Both use the exact same
production 256 inference kernel and measured zero hot-path allocation. Width512
cost about 1.85 times CP64 in its matched assessment and doubled model/accumulator
storage. During confirmation, last-reported UCI throughput was 0.715M nodes/s for
WDL25 versus 0.723M for CP64; mean reported depths were 15.961/15.998. Different
trees and incomplete-iteration reporting prevent interpreting these as pure
kernel speed ratios. Equal wall-clock play, rather than throughput, selected the
research incumbent.

Focused software checks passed: 14 existing baseline Java tests, 4 new research
tests and 49 distinct Python workflow/statistics tests. Real smoke and endpoint
exports, all-parameter independent quantization, 259-position feature/float/integer
parity, 255-transition trained incremental/fresh walks, UCI lifecycle and explicit
resolved search identities also passed. Full engine suites, GUI/native packaging,
multiworker play, cache counters and whole-worker RSS were deliberately outside
this targeted research validation. The final training-summary helper initially
failed because an absolute receipt path was compared with a relative campaign
root; resolving the root fixed the helper, and its complete retry passed. Both
logs remain; no training or tournament evidence was changed.

The initial campaign ends at its successful independent confirmation, rather
than spending the remaining ceilings on new selection. The prepared incumbent
data-fork recipe remains explicitly **unexecuted**; no second confirmation or
production replacement was performed. CP64's capacity is not established as the
main limitation. Current evidence favors training/data/target improvements before
another wider model: a larger network fit training better but generalized and
played worse in this controlled pilot. This does not rule out wider networks with
better data, longer training or other architectures.

Next research priorities are an independently seeded WDL25 replication and
longer-clock/multiworker confirmation before any deployment decision, then more
reliable game-grouped/history-correct supervision. A controlled width comparison
with the improved supervision can follow if those checks hold. No new downloads
or data-generation expense was incurred. Chunk/near-neighbor separation cannot
prove source-game independence; multi-move relatives, opening-family dependence,
single-seed variability and clock/platform transfer remain limitations. The
confirmed point estimate exceeds the ambitious initial +50 target, but its lower
bound does not prove a gain of at least 50 Elo.

Required human actions for this completed research campaign: **None**.
Production adoption remains blocked on the user's separate explicit acceptance;
this research selection and any local commit do not supply that acceptance.

## Final retention, resource and provenance checks

`final-validation.json` records passing scalar-without-Vector-module versus frozen
Vector output equality on all 259 probes for CP64, WDL25 and width512. All 47 frozen
runtime files match their manifest. Protected CP64 weights, production inference
sources, search canons, feature inventory, Cargo.lock and VERSION_STATE.txt match
their initial bytes. The version is a regular, single-link, strict JSON file and
remains 0.0.0 build 100. Research tooling/source-set work does not warrant a new
normal application build; no version finalizer was invoked or introduced.

Confirmation client-side time per move averaged 186.652ms for WDL25 and 186.667ms
for CP64, including protocol overhead. Both commands are identical except for
the network path. Observed match wall totals: 1,263.462 seconds for four screens
and 10,264.065 seconds for confirmation, **11,527.527 seconds / 3.202 hours** overall,
excluding small supervisor startup and audit overhead. Training and play remained
well below their 12-hour and 6-hour ceilings; there is no active experiment left.

All eight campaign training runs are now retired through the existing opt-in
training retention workflow, retaining full checkpoint 4 and 64 optimizer endpoints,
all tested/final model exports and all compact verification/provenance evidence.
Only campaign-created disposable initializers and intermediate weights were
removed: 2,008 files, 20,047,988,832 logical bytes. Every retirement reports finalized,
zero residual deletion targets and no error. The two sets of retention selection,
input and result JSONs record the exact decisions. Inherited data, checkpoints,
experiments and all failed/negative results remain untouched. This training-artifact
retention workflow is separate from the prohibited application-version finalizer.

Raw data, weights, checkpoint state, detailed search rows and tournament PGNs
remain under ignored `raw/` paths. Exact representative bulk paths were checked
against the existing ignore rule before staging. Tracked assessment files contain
compact analytical/reproducibility summaries and hashes of their preserved full
raw summaries; no raw output was force-added. Git deliverables consist of the
research implementation/launcher/tests, bounded trainer/export/control extensions,
data and match utilities, recipes, manifests, concise measurements, this report
and its README link. Initial Git state was clean; these changes are attributable
to this campaign. No inherited changes were absorbed, no history was rewritten,
and no push, deployment, release or production promotion was performed.

The staged whitespace check passes with `cr-at-eol` enabled for the frozen
Windows JSON files. The default check labels their CRLF carriage returns as
trailing whitespace; their hash-bearing bytes were deliberately preserved rather
than normalized after the experiments. No repository Git configuration changed.
