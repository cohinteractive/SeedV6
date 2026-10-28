# R007 original-target continuation playing screen

Prospective after both prediction audits, before any games with epoch40 models.
Pure distillation failed its gate and receives no games. Longer BT4 training
improves original-label MSE by3.21%/3.67%; check whether that translates to play
before using either endpoint as a benchmark or spending more training resources.

Two seed-matched comparisons, epoch40 versus its own epoch24, on the first16
frozen R001B development roots. Both colors,32 games per comparison,64 total,
20,000 exact whole-request nodes, common flags262143,TT16MiB,one worker,one game,
no clocks/book/TB/ponder. Openings are reused diagnostics,not independent final
evidence. Same frozen runtime and verified protocol as R001B. Cap640 plies remains
unscored;30s watchdog censors failure,never a chess clock or time-forfeit award.

Fixed-N with no significance-driven extension. Report each seed and opening-pair
bootstrap intervals; missing/capped outcomes also get all-game bounds. An epoch40
candidate becomes the preferred development control only if both seeds score
at least50% with no caps/protocol failures. Seed71 remains canonical,never select
whichever seed wins more. Otherwise retain the played epoch24 control and record
the longer-training result as inconclusive/negative as warranted. This small
screen cannot establish superiority or CP64 competitiveness, even if passed.

Expected3–8 minutes serial,under1GiB total resident memory,one active search,
BelowNormal. Each game is atomic and replay-verified; resume skips completed
games and repeats only an incomplete game from its frozen opening. Existing
resource admission/monitor/Job Object release applies. Exact invocation:
`compare_pairs.py docs/research/brn-relational-cglhw/r007-bt4-games-plan.json`
through nnue-control with a fresh launch log. No background queue.

## Result

All64 games completed and replayed;zero caps/failures;both engine groups released.
Seed71:13W11D8L,57.8125%,paired bootstrap95%42.1875?73.4375%.
Seed97:12W7D13L,48.4375%,paired bootstrap95%32.8125?64.0625%.
Neither interval establishes a strength change;the second seed fails the50%
admission threshold. **Retain canonical epoch24; no further identical exposure
or epoch40 promotion.** Lower prediction loss did not give a reproducible playing
benefit in this bounded screen. Preserve both outcomes and all checkpoints.
Serial stages112.39/109.99s,one active search,no clocks/GPU. See
r007-bt4-games-results.json and its raw result/binding/release hashes.
