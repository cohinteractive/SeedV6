# M001: first operational Bullet / Java path

22 October 2026. **Technically operational; no competitive or owner-acceptance
claim.** Starting clean499685a/build61. All changes in this unit are attributable
to this new programme. No suspended files, owner libraries, Search canon, old NNUE
formats or default evaluator selection changed. No finalizer was used.

## Implementation and evidence

Pinned Bullet579b78b, Rust1.89, CUDA13.0.2 extracted local libraries and existing
driver621.01 successfully trained on RTX4060. Official component archives total
724,622,431 bytes; hashes and local provisioning receipt are retained. No nvcc,
driver installation, administrator action, WSL change or global environment change.
Python dependencies are isolated in an ignored venv. Cargo.lock fixes dependencies.

Architecture: Bullet's10 mirrored king buckets,7680 inputs,256 units/perspective,
SCReLU, one512-to-1 head, training-only768-input factorizer merged on export.
Deployed1,966,849 i16 parameters; immutable payload3,933,698 bytes plus72-byte
S6CNU001 header. Header binds version, dimensions, buckets, QA255/QB64, scale400,
length and SHA256. Java uses int accumulators and long head arithmetic, explicit
two-stage truncating scale, normal-score clamp32511, no handcrafted material term.

Java full rebuild is unit-major and independent of the feature-delta kernel.
Worker/ply-local sums update changed board squares; bucket OR horizontal-mirror
changes refresh the affected perspective. Existing SearchEvaluation lifecycle is
reused; no board/move representation rewrite. Explicit `--nnue` UCI selection
loads this format and fails on invalid payloads. Ordinary Gen32/HCE and saved GUI
choices remain unchanged. The new conventional profile uses existing unpruned
quiescence/PVS at1..16 workers; no Pair-2/HCE numerical pruning, LMR, completed
hints or construction-v1 adoption. A narrow factory/table seam and conventional-only
worker routing provide this integration; no Search algorithm change.

Actual validation:

- Seven new core tests plus frozen production baseline pass:953 legal/sibling
  transitions,30 special transitions, all promotions, both EP/colors/castles,
  bucket/mirror crossing, same-context king moves, null/undo/reset, every raw unit,
  corrupt/truncated/oversized/schema payloads, extreme weights, worker isolation,
  main/qsearch transfer and full-rebuild versus complete qsearch at1/2 workers.
- 19 existing focused legacy NNUE/production-boundary cases pass;15 UCI module
  tests pass. **42 cases, no skips/failures in the final selected sets.** Builds
  enforce project boundaries. Full suites, GUI/native packaging and strength
  acceptance were not substituted or run as a completion ritual.
- Actual baseline JVM: five400ms searches, legal-move reapplication, stop/readiness/
  quit; peak resident193MiB. New opt-in UCI: four300ms searches independently
  validated by python-chess, stop/newgame/repeated search/quit at1 and2 workers,
  before and after unpruned-qsearch routing. Two workers were a brief isolation
  check, not a tournament concurrency setting. All passed.
- Smoke and pilot export independently regenerate **every** quantized weight from
  optimizer floats/factorizer and round-half-away.128 held-out positions agree on
  Python/Java/Bullet features,512 integer accumulator values and final integer
  scores including independent Java full rebuild. Pilot GPU float max absolute
  raw difference2.1333e-7 versus independent double reference (limit2e-5).
  Pilot quantization RMS4.01cp, maximum15.08cp on those probes; quantization is
  measured separately from floating-point inference agreement.

## Dataset, pilot and continuation

Existing CC0 Lichess eval archive read only:22,086,532,809 bytes. No dataset
download. Bounded prefix224579 lines /20,840,925 compressed bytes produced131072
accepted positions:117882 training,13190 diagnostic validation. Source prefix,
outputs, filters and limitations are in dataset-manifest.json. Use wdl=0: labels
are white-relative CP, not outcomes; result0.5 is an unused binary-format placeholder.
Game IDs are absent, so related-game independence is NOT established. Mixed
Stockfish versions/scales, source-prefix sampling and discarded tactical positions
limit the first checkpoint. BT4 inverse-Q and new bulk data remain deferred.

Pilot16: seed20261022, batch1024,128 batches/superbatch,16 superbatches,
2,097,152 exposures, AdamW lr.001, factorizer/transformer clipping.99, save every4.
One loader worker, bounded small batches, below-normal process priority. GPU training
reported about2.7s plus compilation/export/startup overhead; this tiny run is not a
sustained throughput benchmark. Validation sigmoid MSE.00544199 versus constant
zero.01753995, CP MAE83.856. Training final loss~.00046 indicates a substantial
generalization gap. This is a useful pipeline checkpoint, not a strength result.

Pilot payload SHA256 `f9320a0f63ed8d6846e90065bc98f5227a92aa32abc21319c2ba578520c54b30`;
local file `build/conventional-nnue/runs/pilot-16/pilot-16.cnnue`, complete-file SHA256
`f4bfb96a94372f109d239d2ef16898253a43a6762100dcd54b9c145a82f7c54c`.
Optimizer and runtime outputs are preserved, not added to Git.

Continuation binds input hashes, seed/recipe, step, complete optimizer weights/
momentum/velocity and checkpoint receipt. Altered inputs/recipe are rejected, not
silently treated as resume. The small4096-position continuation had identical
quantized bytes; float differences were comparable to repeated continuation
(max weight2.20e-7 versus2.09e-7). Longer pilot12->16 continuation differs by
max2.023e-6 in float weights and21 quantized values by1. **GPU training is not
bitwise deterministic.** Upstream atomic reductions support this explanation;
do not claim exact optimizer replay or arbitrary-future byte identity. Preserve
the actual frozen checkpoint/hash for exact inference reproduction. Resume receipts
are emitted after successful bounded training, so interrupted jobs without completed
receipts require inspection before continuation, not a bypass.

## Cost and limitations

Single-worker warmed255-transition walk, three repetitions of20000 evaluations:
incremental+head1411..1605ns, full refresh+head4784..4839ns, frozen Pair-2 rolling
evaluation88..92ns. Measured incremental allocation is0 bytes across20000 calls;
network load76ms in that JVM. This is not an optimized JVM limit, sustained NPS
comparison, or Elo claim. The substantial cost difference makes complete-engine
testing essential. No competitive promotion or default switch is justified yet.

Retained negative evidence: unsupported initial PowerShell timestamp flag; wrong
assumption about Bullet padding rejected before export; one test accessor typo
failed compilation, corrected and retested. Completed output is preserved. Source
line-ending additions were corrected to existing LF; diff checks pass.

Primary raw paths: `build/conventional-nnue/` logs, all runs, data and UCI transcripts;
`engine-core/build/test-results/test`, `app/build/test-results/test`, and
`app-uci/build/test-results/test` hold the last selected JUnit outputs. Compact
receipts in this directory preserve observed evidence; a fresh clone must recreate
ignored inputs/artifacts using RUNBOOK.md. Upcoming experiments use local `raw/`.

The subsequent E001 development screen is recorded separately; its negative result
does not invalidate operational correctness or establish competitive readiness.
Human actions required: None for independent bounded NNUE continuation. Search
Phase3 construction acceptance remains outside this programme.
