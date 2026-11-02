# Independent confirmation preparation

This records preparation for `g1-confirm-v1-protocol.json`; its frozen rules govern
the actual games and analysis. Preparation preceded all confirmation games; the
completed outcome is now recorded in [RESULTS](RESULTS.md). R1's adverse/inconclusive development
ablation supports selecting unchanged G1. The objective's initial allowance
remains one W1 attempt, up to 160 fresh opening pairs, one-sided nominal alpha .025.

## Inputs and exposure

The pinned Stockfish books revision is
`a804a97e709f40aa75a00f9d19e84915b7a3a45c`; its 8moves_v3 PGN has SHA256
`5835239f88cc2c7511b177c32392a69f3ede21819cf0616f80a7f907cd21d17e`.
[Upstream](https://github.com/official-stockfish/books/tree/a804a97e709f40aa75a00f9d19e84915b7a3a45c)
provides 16-ply start-position histories under CC0. Downloaded source, license,
metadata and archive are local under `raw/confirmation-book-v1`, identified by
`confirmation-book-v1-manifest.json`. A newly downloaded book is not automatically
unexposed: earlier SeedV6 campaigns used this book too.

`confirmation-input-v1-protocol.json` binds the selection implementation before
execution: exclude the entire pinned legal16 source and all tracked non-BOUNDARY2
receipts containing positionKeys, then sample 160 unique legal, nonterminal roots
from held-out hash partition 1 with seed 2026110204. Preserve all 16 real plies.
There is no candidate evaluation or outcome filter. Full PGN parsing runs only
between game batches. If the eligible pool is insufficient, preserve that failure;
do not search seeds or change exclusions after seeing game outcomes.

Selection completed after R1 drained in51.96s. It checked34,700 source games and
40,462 legal16 histories, excluded56,553 distinct historical/source keys, and found
1,215 eligible distinct partition-1 roots. The fixed seed selected160 with zero
overlap. `confirmation-v1-openings.pgn` SHA256 is
`389d31e283a7b85ad9073d0a3766527ea431874322105bbbd9f17d9f2629b695`.
The sampling fraction and exclusion-driven curated subset limit generalization;
no finite-population correction is used to narrow the reported uncertainty.

Read-only metadata inventory found 128 usable receipts with 28,636 distinct keys.
Six other selection records have no key array. Their reviewed purposes are:

- Root-witness and played-corpus records select post-opening diagnostic trajectories.
- Information-reuse and TT-effectiveness lifetime records replay games from already
  recorded development/baseline openings; they are not separate confirmation roots.
- SR029 A/A uses the first four quiet development openings, already covered by its
  full receipt. Tournament calibration uses the first twelve SR024 development
  openings, likewise covered by its full receipt.

The selector lists these non-key records explicitly. The two historical BOUNDARY2
studies have not been opened or hashed: its path guard precedes all file access.
Excluding the whole legal16 source is deliberately broader than inspecting their
individual selections. Distinct roots do not remove all opening-family correlation
or establish representativeness outside the declared curated-book distribution.

## Inference and resource planning

Frozen primary criterion: a fresh direct comparison against champion79, 160 fixed
color-reversed pairs, paired-normal one-sided 97.5% lower score bound above .5 and
observed score at least .53. The practical target is a positive three-point score
gain, not merely a nonzero implementation effect. Plan power around a six-point gain;
the pilot estimate is not a promised or unbiased effect size. Use actual confirmation
pair variance, not pilot variance, in the final interval. Degenerate sample variance
uses the nondegenerate bounded Hoeffding lower bound instead. Report bounded
sensitivity at the same alpha even when it disagrees with normal inference.

The first G1 pilot pair SD is approximately .2535. At that SD, 160 pairs have about
85% normal-approximation power for a six-point gain, but substantially less for a
three-point gain. At SD .31, the six-point power falls to about69%; roughly839 pairs
would be needed for80% power at three points. Actual opponent and book variance may
differ. Such a larger campaign remains outside authority. At the measured60.4s/pair,
160 pairs cost about2h41m; reserve a hard timeout and retain all failed work.

Mean/standard-error inference follows the usual independent-sample mean framework
([NIST](https://itl.nist.gov/div898/handbook/eda/section3/eda352.htm)); the pair-score
distribution is discrete, so normal inference is explicitly approximate. Individual
games are never treated as independent replicates within a color-reversed pair.
Historical TT attempts retain their own spent allowance and conclusions; this
separately authorized HL family does not restore or pool their claims.

`tools/p4-learning-confirm-analysis.py` checks exact settings, artifact/protocol/input
identities, complete audited games and drained handles, then independently recomputes
pair scores before inference. Six synthetic checks passed in
`confirmation-analysis-v1-qualification.json`; these are not match results. The
frozen protocol binds the script and final source/artifact/input hashes. Operational
launch gates and W4 investigation were completed before confirmation. The actual
result and its limitations are reported separately; this design is not acceptance.
