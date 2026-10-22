# Bootstrap observations and programme challenge

22 October 2026. Starting clean HEAD `499685a` / application build 61. Exact root
CODEXLOG_CURRENT.md absent; do not create it. VERSION_STATE.txt present; repository
owner's no-finalizer, commit-based policy applies. First successful UTC clock
observation was 2026-10-22T05:28:43Z after an unsupported PowerShell -AsUTC attempt;
there is no repository journal capability to initialize.

## Baseline and protected ownership

Read root AGENTS.md, README, architecture, `docs/engine-baseline-2026-10-22.md`,
Search P3-R014/P3-F012, historical NNUE and BRN states, and current inference/search
sources. Standalone UCI and fresh desktop Play select Pair-2 Gen32/Policy 4; legacy
CLI remains HCE. Current production payload SHA256 independently matches
`5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7`.
Historical baseline smoke outputs exist under
`build/baseline-stabilization/20261022/`; current runtime retest is pending.
HEAD includes a later GUI Play edit; the older baseline report is not proof of all
current code. No research process was observed among current Java processes (IDE
language/build servers and Gradle daemon only). No concurrent tracked edits observed.

Preserve `docs/research/nnue-cglhw`, `brn-architecture-cglhw`,
`brn-successor-cglhw`, owner data/model stores and pause sentinel. BRN successor
explicitly suspended; older NNUE STATE's active wording does not activate it here.
Search construction is technically complete pending owner acceptance. Recursive
Policy 4 remains production; do not promote construction or inherit Pair-2-specific
search calibration for NNUE. SearchEvaluation's worker-local State and main/qsearch
transfer are useful seams; no board/search algorithm rewrite is needed.

Existing NNUE is a distinct float HalfKP-like 49,152 -> 64x2 -> 32 -> 1 clip/tanh
network with legacy score mapping and material variants. Reuse placement-delta
logic, lifecycle seams and test patterns; preserve network format/trainer semantics.
Do not retrofit SCReLU integers into an existing identity. Full-refresh inference
must remain useful independently of incremental optimization.

## Environment and upstream

Observed Windows 11 Pro 10.0.26300, Ryzen 5 5500 (6/12), ~31.8 GiB RAM with
~16 GiB free. RTX 4060 8188 MiB, driver 621.01, 6825 MiB free/12% GPU at first
check; CPU load 51%, so keep workers conservative. C: ~284 GiB free, E: ~1.2 TiB.
Java21, Python3.13, Rust/Cargo1.89, MSVC14.44 and Windows SDK exist. CUDA_PATH/nvcc
absent. WSL2 has docker-desktop; do not repurpose it or install a distribution.

Current [Bullet upstream](https://github.com/jw1912/bullet), pinned revision
`579b78b37a694cacd6e9ede4706386c15c34837c`, declares Rust>=1.87/MIT. Its actual
GPU build.rs has Windows CUDA library paths and links cuda/cudart/nvrtc/cublas;
it uses runtime compilation, not a compulsory nvcc build. [Getting started](https://github.com/jw1912/bullet/blob/579b78b37a694cacd6e9ede4706386c15c34837c/docs/2-getting-started.md)
and [saved format](https://github.com/jw1912/bullet/blob/579b78b37a694cacd6e9ede4706386c15c34837c/docs/4-saved-networks.md)
were checked against source, not historical API recollection. No GitHub release
is advertised on the current root page; the immutable source pin is the build identity.

## Initial challenge and resulting choices

- Existing old NNUE workstream conflicts with a new scope if reused: create this
  separate directory, preserve old research, and avoid bootstrap-material fairness
  restrictions that belonged to a different objective.
- A GPU driver alone is not a CUDA development/runtime installation. Verify a real
  training step before claiming environment success. Prefer official component
  archives in ignored local build storage; no driver/admin/global PATH mutation.
- Full HalfKA and large multi-layer nets add data and JVM cost before a useful
  checkpoint exists. Start from Bullet's demonstrated mirrored king-bucket inputs,
  256 accumulator units, SCReLU and one scalar head. Use its documented 10-bucket
  layout and factorizer merging; 1,966,849 deployed parameters (~3.75 MiB i16).
  This is a reversible initial choice, not an established best architecture.
- Integer shorts may overflow on hostile weights even if examples assume training
  clipping. Use int accumulator sums and long output arithmetic with explicit
  clipping/scaling, then measure Java cost; optimize only with equivalence evidence.
- Export is column-major, little-endian; padding and factorizer merge are material.
  Raw float export does not necessarily apply transforms. Independent upstream
  feature mapping and GPU-vs-export checks are prerequisites to acceptance.
- Existing BT4 sources use inverse-Q labels, not ordinary CP. Do not silently pass
  their integer score field into Bullet's CP link. Lichess eval-only data has no
  game outcome; any such start must use score supervision without invented outcomes.
  Data discovery/licensing and an explicit target profile precede substantial training.
- Frozen Gen32's Policy4 has evaluator-specific controls. Compare both practical
  complete configurations and matched search controls if attributing evaluator
  benefit; never call fixed-node speed/strength evidence equivalent to fixed time.
- A long unattended process would break mandatory user-facing reporting. Use bounded
  asynchronous jobs, inspect logs while active, and report at least every15 minutes.

These challenges narrow implementation without reopening the conventional direction.
See STATE.md for subsequent actual verification. Additional source-verified findings:
Bullet's `LocalSettings.test_set` is currently unimplemented (prints a warning), so
held-out validation must be independent. Quantized alignment padding is repeated
ASCII `bullet`, not zero; first export rejected the wrong assumption before producing
an artifact. Export now independently reproduces every merged/rounded parameter from
optimizer weights. Exact-format mismatch is a failure, never silently tolerated.
