# R002 million-position coverage fork

Prospective admission after R002-GAMES, source milestone da8e4f3. Hypothesis:
broader coverage can improve the aligned objective's remaining generalization
gap without changing Pair-2 inference. The 131k stage improved both seeds and
its aligned models beat their matched legacy controls in development games;
Gen32 comparisons remain inconclusive. No architecture or Elo conclusion.

Nested 1,048,576 unique training placements retain the original 131,072 prefix.
Keep the same consumed 4096-position development validation panel; this is not
final evaluation. Original E006 binary SHA and prior selection hashes are checked.
New records follow the coprime affine permutation `(7919*i+2026102903) mod 2^25`.
This is reproducible sampling without replacement, not an iid random sample;
unknown game/source correlations remain. Exact placement/STM/color/rank/file
aliases are purged within training and against the reserved development panel.

Extraction uses `tools/brn-relational/prepare_scale.py` and
`raw/r002-scale-data/selection.sqlite`: transactional row/cursor commits every
4096 candidates, checkpointed prefix import, immutable source/script binding.
An interrupted chunk repeats at most4096 records; final serialization may repeat,
selection never starts over. Each invocation has a240-second allocation, and a
completed tranche can immediately continue after normal admission; no availability
waiting. Expected total2–6minutes, one CPU,8MiB SQLite cache, under512MiB process
memory and roughly400MiB disk. Raw DB/TSV/IDs stay ignored. Resource failures exit
the programme after checkpoint/release; a tranche boundary is not research failure.

Train only aligned seeds71/97. Continue each coverage epoch16 optimizer state
for eight new million-position epochs17–24, batch128, Adam.01 and identical
seed+epoch shuffling. This is an explicit data fork, not a replay of an old run
or a matched-exposure claim. Freeze configs/parent identities before training;
use the already verified depth runtime. One serial JVM,512MiB heap,one logical
processor,BelowNormal,300-second bound per arm and per-epoch checkpoints.
Expected100–150seconds/arm; partial in-memory epoch repeats after interruption.
No GPU,clock tournament,parallel arm or production change.

The next data scale is admitted only if both seeds reduce endpoint validation
MSE by at least2% relative to their131k endpoints AND a plausible playing/cost
opportunity remains. Otherwise stop scaling this unchanged recipe and diagnose
the remaining limitation. Even passing this gate needs fixed-work playing evidence;
loss alone cannot promote a model. Preserve all endpoints rather than selecting
an opportunistic validation minimum. R003 CONTEXT remains rejected at this stage.

Two extraction tests passed before admission: transactional reopen/rollback,
duplicate filtering and binding rejection; permutation uniqueness. The existing
five chess-protocol tests also passed. No final data consumed.

## Observed result

Selection completed in two transactional tranches:241.22s and158.25s. The second
resumed669405 committed rows without repeating selection. SQLite quick_check
passed;1,048,576 training rows plus4096 reserved validation keys; the131k prefix
matched exactly by normalized text lines. Peak sampled system CPU64.10%,minimum
free RAM16.48GiB; extraction process working set observed about40MiB.

Both training arms completed,82.41s/83.50s including startup,peak sampled system
CPU34.74%/38.54%,all owned child processes absent. Epoch24 validation MSE:
seed71 .0236713394 (6.73% gain),seed97 .0237056570 (6.51% gain). Seed71 training
MSE.0239872; CP64 development MSE remains.0163693. The predictive gate passes,
but no playing or architectural advantage follows. All epochs17–24,models and
optimizer states are preserved under raw/r002-scale-aligned-{71,97}. Compact
identities/results are r002-scale-data-manifest.json and r002-scale-results.json.
Admit a broader fresh development opening screen before another data escalation;
keep seed71 as the canonical first-seed candidate rather than selecting a
validation minimum. Seed97 remains replication evidence and a surviving model.
