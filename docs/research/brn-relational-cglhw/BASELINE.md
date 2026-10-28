# Baseline characterization

Source inspection at bootstrap HEAD78b372c/build77; runtime requalification is
recorded separately. Historical measurements below are not new verification.

## Strong NNUE

`build-assets/nnue-candidate/conventional-cp64.cnnue`, S6CNU002, 3,940,952 bytes,
SHA256 `8431626c2fca81e0a1e9ccd5e45ddd8829e6e3282c756c21fad75b06d075bb2b`.
Selected E007 checkpoint64; latest256 is different and was not selected.
Implementation: engine-core `core/nnue/conventional/{ConventionalFeatures,
ConventionalNetwork,ConventionalAccumulator,ConventionalVectorHead}.java`.

7680 inputs: ten mirrored king buckets x twelve color/type channels x64 squares,
including kings. Shared256-wide feature transform, separately accumulated from
both perspectives. Concatenate clipped squared activations and select one of eight
material-count heads `(pieces-2)/4`. Trainer adds a768-feature factorizer, folded
into bucket weights for inference. No explicit material prior or tanh. i16 weights,
i32 accumulators, i64 head arithmetic, QA255/QB64/scale400, truncate integer division
and static-score clamp32511. Vector head is optional and exact to scalar semantics.
Changed squares update both perspectives; king context changes refresh that side.
Null moves change head order; per-ply parents survive siblings/undo. Codec validates
schema, length and payload SHA. SearchEvaluation owns worker-local state.

Training: pinned Bullet579b78b37a694cacd6e9ede4706386c15c34837c in
`tools/conventional-nnue/trainer`, Rust/CUDA implementation. Dual perspectives,
AdamW (pinned defaults plus FT/factor clipping +/-0.99), LR.001, batch4096,
1024 batches/superbatch, seed20261022, selected64=268,435,456 exposures.
Squared error after sigmoid; WDL mixture0 (teacher score only), scale400;
BT4_Q_V1 becomes `400*logit(clamp((1+Q)/2,.0001,.9999))` with verified STM/white
conversion. E006 data:33,554,432 train and524,288 consumed validation positions,
deterministic sampled prepared chunks, quiet/noncheck/ply>=16 filtering, exact
placement/STM dedup; no game identity. Corpus/provenance/config records are in
conventional-nnue-cglhw/e006-* and e007-training-64-config.json. Preserve original
stores and records. Segmented training saves optimizer/cursor; GPU is not bitwise
reproducible. Frozen payload identity is the reproducibility anchor.

The local upstream checkout was reverified at that exact pin. Its AdamW defaults
are decay.01,beta1.9,beta2.999,default clipping+/-1.98 (FT/factor override+/-.99);
the update has no Adam bias-correction counter. SparseInputType supplies fixed
active-index lists and the current ValueTrainer frontend supplies sparse
STM/non-STM inputs plus output bucket selection. A custom relational lookup encoder
could use that interface, but dynamic per-piece message passing is not supplied
by this existing trainer. Custom tensor/input/kernel support and export/parity
would need separate implementation and measurement. No arbitrary-graph GPU
compatibility is claimed. The new CPU graph prototype uses none of Bullet's GPU.

Historical E00764 unpruned confirmation42W/42D/44L was inconclusive. Same weights
with E008 selective search scored84W/26D/18L at200ms and86W/33D/9L at1000ms in
separate128-game confirmations. Read CANDIDATE-HANDOFF/E008/E009 for assumptions
and intervals. This supports a complete-engine result, not architecture-only Elo.
Current production is Construction V1/flags3407871/ADAPTIVE16, with BOUNDARY2 only
for managed clocks; this research will not revisit unopened failed clock studies.
Source reinspection names the two extra CP64-specific flags: NOMINAL_FUTILITY
and VERIFIED_NULL. BaselineEngine selects them only for the frozen CP64 payload;
its BREP2/HCE path uses original262143. R001/R001B deliberately hold262143 fixed.
Their results cannot establish strength against the deployed CP64 composition.
Any promising candidate needs a separately frozen comparison using the deployed
CP64 mechanism set,with evaluator/search factors stated explicitly; no Search
policy or production default has been changed by this investigation.

## Relational baseline and surviving evidence

`build-assets/production/bre-pair2-g32.brn-pair2`,795032 bytes,SHA256
`5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7`.
Gen32 lineage235d0688-e060-44e3-8f1e-119c39bc8017. core/brnpair2 owns inference;
training/core/brnpair2 owns trainer/optimizer.327 anchored pair templates,13 square
categories,two canonical roles,shared shape terms,normalization1/sqrt654;
113231 double training parameters fold to110527 floats, then294 compiled physical
pair tables with double STM values. Sparse changed-square updates maintain exact
table sums. Fixed material+quarter residual,100cp/pawn. Original masked Adam.01,
batch128,eight epochs,expected-score CE via Stockfish WDL. Training.state is needed
for continuation; runtime tables cannot reconstruct optimizer state.

Prior architecture programme tested energies,pair/triple tables,cubic products,
edge functions,context/self pooling and bitboard circuits. Pair compilation was
the practical winner at tested budgets; lower loss did not predict game strength.
Successor W1..W9 added gated/cubic/pooled heads and longer user training; calibrated
heads improved fitting inconsistently, no independent practical successor proven.
Preserved reports are evidence; raw availability must be checked individually.

## Infrastructure and initial attribution limits

Existing research probes cover full/incremental NNUE parity, legal transitions,
special moves, overflow and warmed latency. Root researchLauncher composes explicit
Construction settings. Existing paired_match.py is clock-based and is not a valid
default for this programme; use a dedicated fixed-node protocol with request-node
receipts and legal replay. Existing control supervisor provides process ownership,
hold and checkpoint records. Reuse it; no background scheduler.

The installed research runtime can be older than HEAD. Rebuild isolated local
outputs before new source-bound comparisons and freeze hashes. A matched-target
relational trainer can reuse data/targets but cannot reuse Bullet's fixed NNUE
frontend unchanged. Sparse pair/graph operations need explicit mapping/custom
operators or a separate CPU/autodiff trainer and independent Java parity. No claim
of GPU efficiency or arbitrary relational support is established by NNUE success.
