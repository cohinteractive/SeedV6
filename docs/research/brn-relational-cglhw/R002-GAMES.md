# R002 playing diagnostic

Prospective,29 October2026. Hypothesis: the improved fitting from training through
the actual search score contributes to playing strength,not merely validation loss.
The131072-position16-epoch endpoints are fixed prospectively; no best-epoch swap.
Two independent order seeds71/97. Four comparisons: aligned versus matched legacy
for each seed,then aligned versus frozen Gen32 for each seed. Eight previously
consumed development openings,both colors,16 games/comparison,64 total fixed.

Use10,000 exact whole-request nodes/move,Construction V1 flags262143/ADAPTIVE16,
one game/active search,one worker/engine,16MiB TT,384MiB heap,BelowNormal,book/TB/
ponder off. Default node accounting was qualified by R001; new model selection
adds SHA-identified `pair` codec loading only in the research launcher. No normal
application behavior changes. Before games,actual special-position search/PV
legality and compiled-full/cache prediction checks must pass on these payloads.
The same source/runtime/settings are used for all four comparisons.

No chess clocks.30-second administrative watchdog censors failures;640-played-ply
cap is unscored. Preserve atomic game receipts,legal replay,completed-game hashes
and exact frozen binding. Resume only unplayed games; no favorable stopping or
extension. Report all comparisons with opening-pair intervals and adverse missing
bounds; two-seed results are correlated through shared data/openings. No fresh
confirmation or equal-time Elo claim. A positive screen may justify a new
independent fixed-work comparison; losses must inform the next research decision.

Expected <=5 minutes total based on R001. Recheck resources per comparison/game
and32 plies; stop/release on actual contention or owner hold. CPU only,one search.
Completed R002 training is not repeated. The graph CONTEXT pilot is separately
negative and is not a playing candidate.

## Result and admission

All64 games completed,zero caps/failures. Left-arm W/D/L and score:

| Comparison | W/D/L | Score | Paired bootstrap95% |
|---|---|---|---|
| aligned71 vs legacy71 |7/6/3|62.5%|50..71.875%|
| aligned97 vs legacy97 |9/6/1|75%|65.625..84.375%|
| aligned71 vs Gen32 |3/8/5|43.75%|34.375..56.25%|
| aligned97 vs Gen32 |5/7/4|53.125%|31.25..71.875%|

Conservative bounded-pair intervals are much wider and all include equality.
Same-data/order-seed and opening dependencies prevent treating these as independent
confirmations. No stronger-than-Gen32 or CP64 claim. The training-method hypothesis
has promising development evidence; lower loss alone remains insufficient.

Actual frozen Gen32 MSE.033831 on the same consumed panel versus aligned.025378/
.025356,despite no clear playing lead. This directly reinforces that loss rankings
are not strength rankings. Five payloads pass full/cache checks to~8e-15 pawns.
Exact receipts,hashes,uncertainty and zero-process release are in r002-games-results.json.

One larger aligned-only coverage stage is admitted: nested1,048,576 training
positions with the same4096 diagnostic panel,explicit data fork from each saved
epoch16 optimizer,then eight large-data epochs (global17..24). No repeated
completed epochs. Legacy's adverse fitting and weaker matched screens do not
justify repeating its unchanged recipe at this allocation. Expected~100..150s/arm,
hard300s,one CPU/512MiB; epoch checkpoints. Extraction must checkpoint chunks so
interruption does not repeat completed source selection. Admission after this
stage requires >=2% validation improvement over131k in both orders and a plausible
playing/cost opportunity; otherwise stop unchanged data/recipe scaling and revise
the hypothesis. No final test or production selection is consumed.
