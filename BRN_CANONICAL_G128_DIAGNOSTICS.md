# Canonical BRN-2: first 128-generation campaign diagnostics

Measured 2026-09-22 UTC (2026-09-23 New Zealand time), against authoritative
SeedV6 commit `2cd51640b5738c34b66787ea407792ce4ed13429`.

**Structural color symmetry is exact, but large color-independent evaluation
volatility and Kiwipete qsearch expansion remain.** The campaign's published Best
is generation **121**, not its generation-128 Candidate. At the established
million-node bound, this Best completes Kiwipete only through depth 3; the unchanged
trained NNUE reference completes depth 4 in 41,435 nodes. This is a diagnostic
baseline, not a playing-strength assessment, accepted architecture verdict, or
authorization to change training/search/calibration.

## Exact experiment and stopped state

Student store: `E:\SeedV6-Networks\BRN\BRN-2\training001`.
The saved architecture-specific preference, schema-2 manifests, 128 consecutive
history rows, accepted promotion chain, and generation-129 attempt all identify
this campaign. The sibling `BRN-2\training` contains historical schema-1 manifests
and was not loaded as a current evaluator. Repository smoke stores are short test
lineages, not this campaign. There is no unresolved store ambiguity.

The immutable diagnostic target is:

```text
E:\SeedV6-Networks\BRN\BRN-2\training001\checkpoints\g000121-s000196690-5cf3400fde8d0c61c024b0dac47a8db54e425f7899f62bd2f657f77cc5b6b315
model: network.brn2
model SHA-256: cc2072fe57fcfbc8ca8f83111586e3bfff4fe0abb554998e8ca8b859d1786c47
training SHA-256: 5b29899b380937769ebf2f1a90153916797311c3bc95dd706fdf3a3280e50d4c
architecture: seedv6.brn.2; feature schema: 2; width: 32
generation: 121; optimizer step: 196690; training search depth: 2
network bytes: 13165364; training.state bytes: 39496044
Adam: learning rate 0.001, beta1 0.9, beta2 0.999, epsilon 1e-8
```

Both `refs/best` and the resulting-Best field in history generation 128 name that
checkpoint. Its last promotion was at generation 121; generations 122–128 all
retained it. `refs/latest-training` instead names:

```text
g000128-s000208104-21f97ce7dab8d760f87ce57621abe3012aba26d03d5ea236ed1c32591bf8ed13
```

That settled but non-promoted training checkpoint is the parent of the stopped
generation 129, not the diagnostic baseline. `generation-attempt.bin` records
generation 129, parent g128, incumbent g121, `NNUE_BOOTSTRAP`, and current-format
settings. Its matching bootstrap `.plan` pins NNUE g74. There is **no corresponding
g129 `.data` batch, checkpoint, validation, promotion, or history row**, and staging
is empty. The durable boundary is an attempted generation before batch publication;
the number of in-memory games/plies reached before Stop is not persisted. Training
requires the durable batch before updates. No generation-129 training state was
used, no training was resumed, and neither store was opened through a training
writer or recovery service.

NNUE reference and generator throughout all 128 completed generations:

```text
E:\SeedV6-Networks\NNUE\training\checkpoints\g000074-s000008942-2e22ccbfdeb18f5f26f91f0df183411ac1d01844ccd43e56e9005ee3f5837bc6
model: network.nnue
model SHA-256: 3279ff72654f56c8f5ce3cc89cc55d1f253de6ae9df00bd61a41293f57dd16a9
training SHA-256: 453b7259d4e8041fa4abb8cc0887540434871d2b151c33676f36e749e0c6cb34
optimizer step: 8942
```

This is also the NNUE store's current published Best and the exact historical g315
diagnostic reference. Its own checkpoint training-depth metadata is 6; the BRN
campaign's pinned generation search depth is 2. These are different settings.
NNUE latest-training is g75 and was not substituted for Best.

`CheckpointInspection.accepted` verified 65 accepted identities including g0,
`validations` verified 128 decisions, and `lineage` verified 129 generations including
g0. Exact diagnostic loading used the existing manifest/payload/optimizer integrity
checks. The model and training files were compatible and intact.

## Training history and retained evidence

All 128 checksummed history rows and all 128 checksummed sample batches with their
matching plans survive. Batch hashes, plan hashes, sample counts and generation
times agree with history. All 129 manifests survive; **103 complete payload pairs**
remain: g0, g10, g20, and g29–g128. The other 26 have the established payload-pruned
records and metadata. A directory/manifest alone was not treated as a loadable model.
Retention was not changed and no checkpoint was recreated.

| Requested milestone | Best at that boundary | Payload available? | Static measurement used |
|---|---|---|---|
| g0 | g0 | Yes | g0 |
| g1 | g1 | No | g0 is the nearest surviving published model; no g1 result fabricated |
| g4 | g4 | No | g0; no g4 result fabricated |
| g16 | g15 | No | g10, nearest surviving published Best |
| g32 | g31 | Yes | g31 |
| g64 | g64 | Yes | g64 |
| End of g128 | g121 | Yes | g121 |

There were **64 Candidate promotions and 64 non-promotions**, no ties. Training
continued from latest-training, while promotion compared each Candidate with the
published Best on that generation's held-out games. A non-promotion does not mean
the optimizer reverted to Best. No Candidate-versus-Best game arena ran.

All generations used WDL, the same pinned NNUE model, depth 2, six threads, 64
requested games, opening length 0–8, up to 32 samples/game, and a 1,024-ply game
cap. BRN's effective training configuration was one shuffled online pass,
minibatch 1; the saved generic GUI minibatch value 32 does not override that BRN
contract. Optimizer step 208,104 at latest-training equals the total training
samples processed. Best stops at step 196,690.

Loss is mean `0.5 * (prediction - terminal STM WDL target)^2`, before score mapping.
The persisted `loss` is the Candidate's **post-training loss on its training
partition**, not the online running loss. `candidateLoss` and `bestLoss` compare
the two models on the **same held-out partition within that generation**. Held-out
games change between generations: these are not measurements against one fixed
validation set and the incumbent column is not a monotone global best-loss curve.

| Generations | Mean final training loss | Mean Candidate held-out | Mean incumbent held-out | Promotions |
|---|---|---|---|---|
| 1–8 | 0.0766929 | 0.242166 | 0.28063 | 6 |
| 9–16 | 0.0828074 | 0.21908 | 0.218203 | 4 |
| 17–32 | 0.0774058 | 0.201574 | 0.207688 | 10 |
| 33–64 | 0.0844821 | 0.191075 | 0.190623 | 14 |
| 65–96 | 0.0881957 | 0.176186 | 0.179608 | 18 |
| 97–128 | 0.0887957 | 0.176716 | 0.169031 | 12 |

The early average held-out loss falls, improvement continues through the middle
blocks, and the last two Candidate blocks are approximately flat (0.17619 versus
0.17672). Individual Candidate held-out losses range 0.06659–0.32107; incumbent
losses range 0.06922–0.36333. The defensible characterization is **noisy improvement
followed by a late plateau in these changing-batch averages**, not proven convergence.
Training loss does not fall across those blocks, and the training/holdout gap
persists. The full per-generation losses and decisions are preserved below.

At g121 promotion: training loss 0.08540854, Candidate held-out 0.12419813 versus
incumbent 0.13513555. At g128: Candidate training 0.11496951, Candidate held-out
0.22388028 versus g121 Best 0.18883537, so Best was retained. The latter is Best's
loss on the g128 held-out batch, not an aggregate loss for the campaign.

| Population | Count | W | D | L |
|---|---|---|---|---|
| All generations training | 208,104 | 72,544 | 69,265 | 66,295 |
| All generations heldOut | 52,974 | 18,408 | 17,820 | 16,746 |
| Generation 128 training | 1,632 | 649 | 416 | 567 |
| Generation 128 heldOut | 416 | 125 | 160 | 131 |

W/D/L above describes sampled positions from the side-to-move perspective; samples
within each game share its terminal outcome and are correlated. Whole-game train /
holdout splitting is preserved. Across the completed self-play games, physical
White wins/draws/Black wins were 2,906 / 2,723 / 2,554. Generation 128 had 24 / 18 /
22. The zeros in history's game-pair validation W/D/L fields are inapplicable
bootstrap placeholders, not a draw-only dataset.

There were 8,192 requested games, **8,183 completed and nine capped/aborted** games,
one each in g7, g29, g39, g46, g50, g57, g83, g93 and g119. Completed generations
still had sufficient samples and durable decisions. Total played plies were
1,366,221. There are no missing generation numbers, failed validation decisions,
restart archives or staging remnants in this campaign. Durable history contains
no stall/error event log, so it cannot reconstruct every transient GUI event or
the precise Stop progress in generation 129.

Recorded start/end: `2026-09-22T22:59:25.573413900Z` to
`2026-09-22T23:31:00.993059Z` (31m35.420s elapsed).

| Recorded phase | Sum seconds | Median seconds/generation | Max seconds/generation |
|---|---|---|---|
| selfPlayNs | 1,113.68 | 8.53877 | 11.6866 |
| trainingNs | 41.9259 | 0.328179 | 0.437605 |
| validationNs | 0.998761 | 0.00742125 | 0.0144686 |
| totalNs | 1,894.55 | 14.2527 | 21.1814 |

Sum of generation totals is 1,894.550s; the small wall-time gap includes history
appends/between-generation work. Totals include loading, persistence/publication
and other lifecycle work beyond the three phase timers. Derived rates are 7.348
completed games/s during generation, 4.319 completed games/s across generation
totals, and 4,964 training samples/s during the recorded training phase (which
includes metric passes). These are campaign rates, not standalone inference rates.

## Fixed evaluator corpus and conventions

The unchanged `Brn2DiagnosticCorpus` reuses 15 established roots and every legal
one-ply child, sorted by coordinate move: **15 roots, 233 children, 233 edges**.
Corpus ID `seedv6-brn2-diagnostics-v1`; SHA-256:
`0503b850d5ed49686e72e601b1216270cfd40fc3bac18fac4dae697b323f3bd8`.

The roots are opening-start, middlegame-kiwipete, quiet-endgame, tactical-queen,
quiet-pawn, check-evasion, transposition-knights, qsearch-exchanges, promotion-race,
en-passant, checkmate-terminal, stalemate-terminal, opening-ruy-lopez,
quiet-fianchetto and queen-endgame. FENs remain in the corpus source and JSONL
records. This covers the requested existing opening/middlegame/endgame, quiet,
tactical and special-move fixtures; it is small, synthetic and not game-frequency
weighted. There is no separate asserted representative closed-position stratum.
Terminal fixtures receive static predictions here, while search adjudicates them.

Root and child populations are separated below; pooled results weight every
sample equally and therefore weight high-mobility root families more heavily.
SD is population SD; percentiles interpolate at `(n-1)*p`. Units are raw pre-tanh
head scalar `z`, normalized `v = tanh(z)`, and current SeedV6 integer search score.
“Raw” does not mean inverse-tanh reconstructed from a rounded bounded output.

| Population | Representation | count | mean | stddev | p50 | p5 | p25 | p75 | p95 | min | max |
|---|---|---|---|---|---|---|---|---|---|---|---|
| roots | rawPreTanh | 15 | 0.118967 | 0.37454 | 0.106194 | -0.40171 | -0.0198416 | 0.232382 | 0.687219 | -0.583865 | 1.17294 |
| roots | normalized | 15 | 0.0970235 | 0.302093 | 0.105797 | -0.376599 | -0.0198097 | 0.22809 | 0.559403 | -0.525469 | 0.825214 |
| roots | searchScore | 15 | 3,154.33 | 9,821.52 | 3,440 | -12,243.5 | -644.5 | 7,415.5 | 18,186.8 | -17084 | 26829 |
| children | rawPreTanh | 233 | -0.0545088 | 0.337346 | -0.0997018 | -0.68409 | -0.216096 | 0.206839 | 0.476025 | -1.17014 | 0.737315 |
| children | normalized | 233 | -0.0467531 | 0.30055 | -0.0993727 | -0.594162 | -0.212794 | 0.203939 | 0.443054 | -0.824317 | 0.62752 |
| children | searchScore | 233 | -1,520.01 | 9,771.19 | -3,231 | -19,316.8 | -6,918 | 6,630 | 14,403.8 | -26799 | 20401 |
| all | rawPreTanh | 248 | -0.0440163 | 0.342219 | -0.0949496 | -0.676968 | -0.213769 | 0.207423 | 0.478248 | -1.17014 | 1.17294 |
| all | normalized | 248 | -0.0380569 | 0.302591 | -0.0946652 | -0.589538 | -0.210572 | 0.204499 | 0.444839 | -0.824317 | 0.825214 |
| all | searchScore | 248 | -1,237.29 | 9,837.55 | -3,077.5 | -19,166.5 | -6,846 | 6,648.25 | 14,462 | -26799 | 26829 |

### Limits and symmetry

The implementation has unbounded finite raw head output, lower-zero ReLU boundaries
without upper clipping, and tanh output in [-1, 1]. Current score mapping is
`sign(v) * max(1, round(abs(v)*32511))` for nonzero `v`, zero otherwise. It does not
apply an additional BRN clamp; the mate band starts at 32512. These units are not
calibrated centipawns. Hidden ReLU zero-activity fractions were not instrumented;
they are not an upper saturation limit.

All **248/248 fixed-corpus outputs** have `abs(v) < .95`; counts at .95, .99, .999,
exactly 1 and absolute score 32511 are all zero. Maximum absolute value is
0.82521381. Sampled output saturation does not explain these fixed-corpus deltas.

Raw, normalized and search-score color-reversal residuals are **exactly zero** for
all 248 positions; all 233 score-edge decompositions also have zero residual.
The sampled color-dependent
edge component is zero, including captures, checks, promotions, castling and EP.
This validates structural color symmetry, not temporal smoothness: exchanging
colors and reflecting ranks is a different transformation from playing a move.

### Parent–child volatility

All deltas compare one perspective: `abs(-f(child) - f(parent))`. Child STM values
are negated before comparison. Quiet means no capture, promotion or delivered
check; tactical is the union of those three exclusions. Castling can be quiet,
and check evasions can be quiet. Special categories overlap; they are not disjoint
subtotals. In particular, the five checking edges are a small sample.

| Category | Representation | count | p50 | p90 | p95 | p99 | max |
|---|---|---|---|---|---|---|---|
| quiet | rawPreTanh | 210 | 0.307833 | 0.700739 | 0.827784 | 1.02721 | 1.07345 |
| quiet | normalized | 210 | 0.299235 | 0.602396 | 0.68216 | 0.784664 | 0.963215 |
| quiet | searchScore | 210 | 9,728.5 | 19,584.7 | 22,177.5 | 25,510 | 31315 |
| tactical | rawPreTanh | 23 | 0.16387 | 0.570551 | 0.600706 | 0.662449 | 0.679068 |
| tactical | normalized | 23 | 0.155616 | 0.526794 | 0.546137 | 0.564675 | 0.569339 |
| tactical | searchScore | 23 | 5,059 | 17,126.6 | 17,756 | 18,357.6 | 18509 |
| capture | rawPreTanh | 16 | 0.0840527 | 0.432791 | 0.507724 | 0.524871 | 0.529158 |
| capture | normalized | 16 | 0.0805101 | 0.389013 | 0.450245 | 0.463656 | 0.467009 |
| capture | searchScore | 16 | 2,617.5 | 12,647 | 14,637.8 | 15,073.9 | 15183 |
| gives-check | rawPreTanh | 5 | 0.55145 | 0.592247 | 0.597886 | 0.602398 | 0.603526 |
| gives-check | normalized | 5 | 0.521361 | 0.540143 | 0.544139 | 0.547336 | 0.548136 |
| gives-check | searchScore | 5 | 16,949 | 17,561 | 17,691 | 17,795 | 17821 |
| promotion | rawPreTanh | 4 | 0.559423 | 0.656406 | 0.667737 | 0.676802 | 0.679068 |
| promotion | normalized | 4 | 0.490577 | 0.554946 | 0.562143 | 0.5679 | 0.569339 |
| promotion | searchScore | 4 | 15,948.5 | 18,041 | 18,275 | 18,462.2 | 18509 |
| castle | rawPreTanh | 2 | 0.122781 | 0.208392 | 0.219094 | 0.227655 | 0.229795 |
| castle | normalized | 2 | 0.116829 | 0.197842 | 0.207969 | 0.21607 | 0.218096 |
| castle | searchScore | 2 | 3,798 | 6,431.6 | 6,760.8 | 7,024.16 | 7090 |
| en-passant | rawPreTanh | 1 | 0.0770828 | 0.0770828 | 0.0770828 | 0.0770828 | 0.0770828 |
| en-passant | normalized | 1 | 0.076428 | 0.076428 | 0.076428 | 0.076428 | 0.076428 |
| en-passant | searchScore | 1 | 2,485 | 2,485 | 2,485 | 2,485 | 2485 |

NNUE on the identical edges, in current search-score units:

| Category | count | p50 | p90 | p95 | p99 | max |
|---|---|---|---|---|---|---|
| quiet | 210 | 3,351.5 | 13,812.2 | 17,991.1 | 34,091.6 | 45167 |
| tactical | 23 | 4,194 | 35,360 | 38,595.8 | 39,191.2 | 39330 |
| capture | 16 | 3,044 | 38,183 | 38,856.8 | 39,235.3 | 39330 |
| gives-check | 5 | 4,306 | 20,456.4 | 21,517.2 | 22,365.8 | 22578 |
| promotion | 4 | 18,567 | 21,762.6 | 22,170.3 | 22,496.5 | 22578 |
| castle | 2 | 28,076.5 | 41,748.9 | 43,458 | 44,825.2 | 45167 |
| en-passant | 1 | 9,901 | 9,901 | 9,901 | 9,901 | 9901 |

Large static changes are not automatically chess errors, especially for promotions
and exchanges. Nevertheless, quiet BRN median 9,728.5 versus NNUE 3,351.5, and the
five checking-edge median 16,949 versus 4,306, identify remaining consistency
concerns. The 23 static tactical edges have median 5,059 versus 4,194; this small
population must not be equated with the historical qsearch-tree tactical population.

To distinguish numeric scale from relative variation, the following divides the
median absolute edge delta by the IQR of that evaluator's pooled 248 outputs in
the same representation. This diagnostic is invariant to multiplication by a
positive constant. It is not a score calibration, centipawn mapping or accuracy
metric, and it is sensitive to this corpus and its mobility weighting.

| Category | Model | Representation | IQR | Median delta / IQR |
|---|---|---|---|---|
| quiet | brn2 | rawPreTanh | 0.421193 | 0.730861 |
| quiet | brn2 | normalized | 0.41507 | 0.720925 |
| quiet | brn2 | searchScore | 13,494.2 | 0.720937 |
| quiet | nnue | rawPreTanh | 0.689753 | 0.227515 |
| quiet | nnue | normalized | 0.662356 | 0.155645 |
| quiet | nnue | searchScore | 21,534 | 0.155638 |
| tactical | brn2 | rawPreTanh | 0.421193 | 0.389062 |
| tactical | brn2 | normalized | 0.41507 | 0.374914 |
| tactical | brn2 | searchScore | 13,494.2 | 0.3749 |
| tactical | nnue | rawPreTanh | 0.689753 | 0.261048 |
| tactical | nnue | normalized | 0.662356 | 0.194758 |
| tactical | nnue | searchScore | 21,534 | 0.194762 |

Quiet normalized median delta/IQR is 0.721 for BRN versus 0.156 for NNUE;
tactical is 0.375 versus 0.195. Thus simple score-unit amplification is not the
entire observed difference: variation is already visible before mapping and after
this scale normalization. Tanh is nonlinear, so raw and normalized ratios need
not coincide.

### NNUE descriptive reference

Both evaluators use the actual side-to-move convention. Spearman uses average ranks
for ties; sign agreement includes zero as a distinct sign. NNUE is a trained
reference, not ground truth or a supervision target.

| Population | Representation | Pearson | Spearman | Sign agreement |
|---|---|---|---|---|
| roots | rawPreTanh | 0.574753 | 0.392857 | 0.6 |
| roots | normalized | 0.512794 | 0.392857 | 0.6 |
| roots | searchScore | 0.512803 | 0.392857 | 0.6 |
| children | rawPreTanh | -0.225934 | -0.0364736 | 0.55794 |
| children | normalized | -0.130508 | -0.0364736 | 0.55794 |
| children | searchScore | -0.130508 | -0.0365106 | 0.55794 |
| all | rawPreTanh | -0.182917 | -0.0116057 | 0.560484 |
| all | normalized | -0.0981314 | -0.0116057 | 0.560484 |
| all | searchScore | -0.0981305 | -0.0116379 | 0.560484 |

BRN minus NNUE search-score disagreement:

| Population | count | mean | stddev | p50 | p5 | p25 | p75 | p95 | min | max |
|---|---|---|---|---|---|---|---|---|---|---|
| roots | 15 | 4,894.2 | 13,115 | 2,170 | -9,811.5 | -5,576.5 | 10,526 | 27,285.6 | -13245 | 31690 |
| children | 233 | -3,540.41 | 19,339.2 | -386 | -47,258.8 | -9,455 | 11,303 | 19,859.2 | -55649 | 29833 |
| all | 248 | -3,030.25 | 19,126.7 | -354.5 | -46,382.9 | -9,188 | 11,201 | 20,186.8 | -55649 | 31690 |

Pooled median absolute disagreement is 10,115.5; p95 is 46,382.9 and maximum 55,649.
Root correlation is positive, but child and pooled correlations are weak/negative.
The difference matters: these 233 sibling positions are correlated observations,
not 233 independent tests of general playing quality.

Surviving published models on the same fixed corpus:

| Generation | Raw SD | Value SD | Quiet score delta median | Tactical score delta median | NNUE Pearson |
|---|---|---|---|---|---|
| 0 | 0.14943 | 0.144224 | 3,456.5 | 1,358 | -0.363805 |
| 10 | 0.263309 | 0.242608 | 9,629 | 5,980 | 0.654879 |
| 31 | 0.426017 | 0.370955 | 14,535.5 | 11,077 | -0.276584 |
| 64 | 0.314671 | 0.275143 | 12,811.5 | 9,158 | 0.481377 |
| 121 | 0.342219 | 0.302591 | 9,728.5 | 5,059 | -0.0981305 |

Neither volatility nor NNUE agreement improves monotonically with generation.
This limits a claim that “more of the same training” is already demonstrably
converging to the desired search behavior. g0 is the actual surviving campaign
checkpoint, not a newly manufactured replacement.

## Bounded search

Unchanged existing conditions: depth 4 requested, one search thread, private cold
262,144-entry TT per invocation, mate-distance-only selectivity, full windows,
singleton root history, diagnostics enabled, qshadow disabled. Each position/backend
had one warmup and two measured repetitions, in its own JVM. BRN and NNUE used
identical limits and fixtures. All non-timing fields checked (nodes, q/main counts,
depth, status, score, move, PV and evaluation calls) matched between repetitions.
Main + q nodes equals the control's total on every invocation, including capped ones.

**Safety bounds:** each search had an exact 1,000,000-node engine bound and a
cooperative 10,000-ms engine bound. Each position/backend process (one warmup plus
two measured searches) additionally had a **40-second hard subprocess watchdog**.
Python `subprocess.run(timeout=40)` kills the directly launched Java process on
expiry. No watchdog fired; no wall-clock engine timeout or exception occurred.

Times below are median search-only milliseconds over the two measured repetitions;
model loading, JVM startup, TT/worker construction and JSON formatting are excluded.
NPS is median per-run NPS. Small sub-millisecond timings are especially sensitive
to JIT/OS scheduling. There is no claim of a controlled architecture speed ranking.

| Position | Model | mainNodes | qNodes | nodes | qRatio | qPerMain | medianMs | nps | completedDepth | status |
|---|---|---|---|---|---|---|---|---|---|---|
| check-evasion | BRN2 | 28 | 2 | 30 | 0.0666667 | 0.0714286 | 2.31135 | 13,263 | 4 | COMPLETED |
| checkmate-terminal | BRN2 | 0 | 0 | 0 | ? | ? | 0.2912 | ? | 1 | TERMINAL |
| en-passant | BRN2 | 625 | 22 | 647 | 0.0340031 | 0.0352 | 6.52275 | 108,597 | 4 | COMPLETED |
| middlegame-kiwipete | BRN2 | 7595 | 992405 | 1000000 | 0.992405 | 130.666 | 6,859.76 | 145,805 | 3 | NODE_LIMIT |
| opening-ruy-lopez | BRN2 | 9465 | 5510 | 14975 | 0.367947 | 0.582145 | 144.859 | 104,703 | 4 | COMPLETED |
| opening-start | BRN2 | 5780 | 333 | 6113 | 0.0544741 | 0.0576125 | 59.3089 | 103,341 | 4 | COMPLETED |
| promotion-race | BRN2 | 464 | 373 | 837 | 0.445639 | 0.803879 | 8.18465 | 112,255 | 4 | COMPLETED |
| qsearch-exchanges | BRN2 | 1488 | 882 | 2370 | 0.372152 | 0.592742 | 12.8935 | 190,748 | 4 | COMPLETED |
| queen-endgame | BRN2 | 1887 | 170 | 2057 | 0.0826446 | 0.0900901 | 7.7086 | 274,223 | 4 | COMPLETED |
| quiet-endgame | BRN2 | 413 | 45 | 458 | 0.0982533 | 0.108959 | 7.7913 | 60,494.2 | 4 | COMPLETED |
| quiet-fianchetto | BRN2 | 11565 | 37459 | 49024 | 0.764095 | 3.239 | 410.279 | 119,629 | 4 | COMPLETED |
| quiet-pawn | BRN2 | 410 | 0 | 410 | 0 | 0 | 7.059 | 65,488.9 | 4 | COMPLETED |
| stalemate-terminal | BRN2 | 0 | 0 | 0 | ? | ? | 0.3363 | ? | 1 | TERMINAL |
| tactical-queen | BRN2 | 217 | 8 | 225 | 0.0355556 | 0.0368664 | 6.4925 | 34,753.2 | 4 | COMPLETED |
| transposition-knights | BRN2 | 1348 | 30 | 1378 | 0.0217707 | 0.0222552 | 7.91035 | 202,609 | 4 | COMPLETED |
| check-evasion | NNUE | 28 | 2 | 30 | 0.0666667 | 0.0714286 | 2.4251 | 12,445.1 | 4 | COMPLETED |
| checkmate-terminal | NNUE | 0 | 0 | 0 | ? | ? | 0.23245 | ? | 1 | TERMINAL |
| en-passant | NNUE | 598 | 40 | 638 | 0.0626959 | 0.0668896 | 13.013 | 51,613.1 | 4 | COMPLETED |
| middlegame-kiwipete | NNUE | 9135 | 32300 | 41435 | 0.779534 | 3.53585 | 82.5055 | 502,817 | 4 | COMPLETED |
| opening-ruy-lopez | NNUE | 11517 | 10223 | 21740 | 0.470239 | 0.887644 | 56.1925 | 404,464 | 4 | COMPLETED |
| opening-start | NNUE | 2217 | 126 | 2343 | 0.0537772 | 0.0568336 | 8.36655 | 282,172 | 4 | COMPLETED |
| promotion-race | NNUE | 467 | 474 | 941 | 0.503719 | 1.01499 | 10.9465 | 99,147.6 | 4 | COMPLETED |
| qsearch-exchanges | NNUE | 1778 | 1419 | 3197 | 0.443854 | 0.798088 | 9.31615 | 350,240 | 4 | COMPLETED |
| queen-endgame | NNUE | 1888 | 171 | 2059 | 0.08305 | 0.090572 | 6.27585 | 330,381 | 4 | COMPLETED |
| quiet-endgame | NNUE | 300 | 16 | 316 | 0.0506329 | 0.0533333 | 8.64205 | 37,813.8 | 4 | COMPLETED |
| quiet-fianchetto | NNUE | 4865 | 3928 | 8793 | 0.446719 | 0.8074 | 24.1588 | 364,719 | 4 | COMPLETED |
| quiet-pawn | NNUE | 576 | 0 | 576 | 0 | 0 | 8.21915 | 98,517.5 | 4 | COMPLETED |
| stalemate-terminal | NNUE | 0 | 0 | 0 | ? | ? | 0.3862 | ? | 1 | TERMINAL |
| tactical-queen | NNUE | 219 | 10 | 229 | 0.0436681 | 0.0456621 | 15.3952 | 14,916.5 | 4 | COMPLETED |
| transposition-knights | NNUE | 1306 | 14 | 1320 | 0.0106061 | 0.0107198 | 7.7132 | 182,046 | 4 | COMPLETED |

Across measured runs (30 per backend, including four terminal invocations each):

| Model | mainNodes | qNodes | totalNodes | qPerTotal | qPerMain | totalMs | aggregateNps | worstMs |
|---|---|---|---|---|---|---|---|---|
| BRN2 | 82,570 | 2,074,478 | 2,157,048 | 0.961721 | 25.1239 | 15,083.4 | 143,008 | 6,953.79 |
| NNUE | 69,788 | 97,446 | 167,234 | 0.582693 | 1.39631 | 507.576 | 329,476 | 85.3744 |

BRN completed 24 nonterminal invocations and had **two Kiwipete node-limit
incompletions**; NNUE completed all 26 nonterminal invocations. The Kiwipete warmup
also hit the node limit. All other nonterminal positions finished depth 4. Terminal
fixtures were adjudicated in the first iteration (reported depth 1), with no nodes.
There were **zero stalls, hard watchdog
terminations, wall-clock timeouts or search failures**. “Node-limit incomplete” is
not a completed depth-4 result.

### Kiwipete sentinel and historical comparison

| Metric | Historical schema-1 BRN g315 | Current schema-2 BRN Best g121 | Same trained NNUE g74 |
|---|---:|---:|---:|
| Requested/completed depth | 4 / 4 | 4 / 3 | 4 / 4 |
| Main nodes | 13,348 | 7,595 | 9,135 |
| Qnodes | 658,493 | 992,405 | 32,300 |
| Total nodes | 671,841 | 1,000,000 (capped prefix) | 41,435 |
| Q / total | 98.0132% | 99.2405% | 77.9534% |
| Q / main | 49.3327 | 130.6656 | 3.53585 |
| Current measured latency, ms | Not remeasured | 6,953.787 / 6,765.734 | 85.374 / 79.637 |

Current BRN has already consumed 24.13 times NNUE's completed node count and 1.49
times the historical BRN completed count without finishing depth 4. Its final
depth-4 total is unknown. Median current BRN latency is about 83.1 times NNUE's.
The identical NNUE node count corroborates comparability of this sentinel setup.
The BRN runs differ in schema, learned weights, training duration and visited tree;
this is not an isolated causal experiment proving that the symmetry change made
search worse. It does establish that eliminating color asymmetry did not remove
this pathology in the measured campaign.

### Bounded tree volatility and color reversal

To compare with the historical **qsearch-tree** delta figures, one additional
BRN-driven Kiwipete pass enabled the existing qshadow and symmetry probes. Same
depth/node/TT/search policy; **30,000-ms cooperative limit and 45-second hard process
watchdog**, one repetition, no warmup. This pass reached the identical million-node
prefix, counts, completed score/move/PV and evaluation-call count as the driver-only
runs. Search time was 10,537.494ms; process time 13.349s. No time/watchdog bound fired.
Instrumentation timing is not mixed into the main throughput comparison.

The exact adjacent-static histogram covers 659,077 edges with actual driver and
shadow evaluations at both endpoints. NNUE is evaluated on the **same BRN-visited
positions** without influencing any search decision:

| Absolute parent–child score delta | n | Mean | Median | p95 | Maximum |
|---|---:|---:|---:|---:|---:|
| BRN | 659,077 | 13,264.013 | 11,516 | 31,584.2 | 58,541 |
| NNUE shadow | 659,077 | 6,768.641 | 3,869 | 22,942 | 56,495 |

Historical full-tree medians were about 11,854.5 (BRN) and 4,290 (NNUE). Current BRN's
median is only 2.9% lower numerically and remains 2.98 times the same-edge NNUE
median. The old completed tree and current capped prefix contain different edges;
the 2.9% is not an estimated causal improvement or an apples-to-apples error rate.

Symmetry sampling used entry stride 101 and limit 8,192, across iterations with
duplicates retained. Once the limit was reached later entries were not sampled;
this is a deterministic prefix sample, not a random tree survey. **All 8,192 raw,
normalized and score symmetry residuals were zero.** Rebuilt scores matched every
available actual driver score in the sample. All 5,434 eligible sampled edges had
zero color-dependent component. Their original/color-independent median was
11,888.5; same-edge NNUE median was 4,303.5. Historical sampled color-dependent
component median was about 6,545.5; the historical static color-symmetry residual
median/p95/max of about 9,999 / 30,802 / 40,595 has become 0 / 0 / 0 on the fixed
corpus. These components can reinforce or cancel and cannot be subtracted from
population medians to assign a percentage of search cost to color asymmetry.

In the 8,192-position qsearch sample, 11 values (0.1343%) had `abs(v) >= .95`;
none reached .99, .999, exactly 1 or the score endpoint. Largest absolute normalized
value was 0.98521032 and raw magnitude 2.44977512. This supports predominantly
unsaturated sampled outputs, not an exhaustive assertion about every searched node.
Seven qtrace aborted-node returns are the expected unwind of the node cap, with
zero exception returns; they are not seven independent stalls.

## Inference throughput and memory

The existing full benchmark could not pair an explicitly trained NNUE checkpoint
with the BRN checkpoint and separate the requested cache operations. A narrowly
scoped `Brn2InferenceBenchmark` was added under the existing tools package. It uses
the same 233 prebuilt corpus children for every operation, keeps parents/caches
outside timing, and never calls training or search. It does not alter any existing
production class. Update measurements visit one-ply siblings from prepared roots;
they exclude move generation, long-path periodic rebuilds and main-to-qsearch copy
frequency. Full BRN uses the optimized full workspace; dual rebuild prepares both
production perspectives; cached forward selects one perspective and executes once.

Environment: Windows 11 amd64, AMD Ryzen 5 5500 (6 cores / 12 logical processors),
Java `21+35-2513`, `-Xms256m -Xmx1024m`. Two warmup rounds and three measured rounds
of 60,000 evaluations per operation, rotating operation order, one thread, no
concurrent task-owned search/test run. The micro process had a 90-second watchdog
and finished normally. It checks full/cached/update value equivalence before timing:
maximum BRN normalized difference 6.66e-16; NNUE 3.84e-7 (float reassociation).

| Operation | Median ns/eval | Median eval/s | Measured eval/s range |
|---|---|---|---|
| brn-full | 12,058.5 | 82,929.2 | 82,464–84,645 |
| brn-cached-forward | 867.975 | 1.15211e+06 | 1,132,781–1,156,716 |
| brn-update-forward | 4,920.15 | 203,246 | 202,244–221,246 |
| brn-dual-rebuild-forward | 25,633.6 | 39,011.3 | 38,562–39,690 |
| nnue-full | 2,202.35 | 454,060 | 449,494–488,855 |
| nnue-cached-forward | 1,041.31 | 960,329 | 954,112–1,002,307 |
| nnue-update-forward | 1,215.92 | 822,420 | 794,334–826,147 |

All measured operation loops allocated **0 bytes/evaluation** according to the JVM's
current-thread allocation counter. These are warmed fixture throughputs, not end-to-end
search or training throughput. BRN update-plus-forward was 2.45 times as fast as
full recomputation here, but about 4.05 times slower than NNUE update-plus-forward.
BRN cached forward alone was faster than NNUE cached forward. Dual rebuild plus
forward took 2.13 times the BRN single-perspective full path on this workload;
this is a practical observation of preparing both caches, not an isolated benchmark
of an unimplemented single-cache incremental evaluator.

Current `Brn2Accumulator` primitive array payload:

| Storage | Bytes |
|---|---:|
| Two `64 * 32` double relation arrays | 32,768 |
| Two 64-int code arrays | 512 |
| Five-long remembered position | 40 |
| 32-double context and 32-double pooled arrays | 512 |
| **Per accumulator, primitive array payload** | **33,832** |
| **Second perspective's incremental payload** | **16,640** |

Main and qsearch each instantiate 257 slots, **514 accumulators per worker**.
Primitive array payload is 17,389,648 bytes = **16.584061 MiB/worker**, confirming
the earlier estimate. The second perspective accounts for **8.156738 MiB/worker**.
This estimate excludes scalar fields, objects, array headers, references and padding.

Runtime construction allocation measured after warming each constructor was
**34,048 bytes/accumulator**, **8,751,408 bytes/main stack** and the same for the
qsearch stack: **16.691986 MiB/worker** for the two evaluation states on this JVM.
This is a construction-allocation measurement, not process RSS. Six such worker
states would allocate approximately **100.152 MiB**, versus 99.504 MiB of primitive
arrays alone. Shared model weights (1,645,665 doubles, about 12.555 MiB), TT, move
ordering, boards, histories and other engine objects are additional. Neither cache
layout nor inference was optimized by this unit.

## Interpretation and limits

1. **The old color pathology is removed in the measured representations.** Fixed
   and sampled-tree reversal residuals and the color-dependent edge component are
   exactly zero. This result is independent of whether the learned values are good.
2. **Color-independent inconsistency remains substantial.** Quiet fixed-corpus
   deltas and normalized/IQR ratios exceed NNUE, while the large tree's tactical
   median remains close to the historical BRN magnitude. Kiwipete still expands
   almost entirely in qsearch and now cannot finish within the established node cap.
3. **Score scaling amplifies visible numbers but is not the whole explanation.**
   Raw/value deltas and robust ratios remain material without saturation. This run
   did not alter scale, and cannot establish the search effect of any alternative
   calibration. Scale-invariant measures are descriptive, not a proposed mapping.
4. **Training/generalization or WDL-target limitations are plausible, not separated.**
   Changing-batch loss averages improve then flatten; training/holdout gaps, noisy
   promotions, uneven milestone behavior and poor pooled reference agreement remain.
   Shallow NNUE-generated terminal WDL trajectories provide no direct constraint
   that adjacent static values agree. No target ablation, fixed validation-loss
   campaign, longer controlled training run or playing-strength experiment was done.
   The evidence does not isolate insufficient training from noisy/limited targets,
   optimization behavior, sample distribution or representational limitations.
5. **Fundamental architectural failure is not demonstrated.** Schema-2 correctness
   and incremental numerical agreement hold, but evaluator quality remains limited
   under this campaign. This does not justify BRN-3, an inference rewrite, blended
   supervision, teacher scores, calibration, promotion changes or NNUE changes.

Small static categories (promotion n=4, checks n=5, castling n=2, EP n=1), correlated
siblings, a single seed/campaign, pruned early payloads, and a capped tactical tree
limit generalization. Earlier-generation held-out metrics remain available, but no
single fixed held-out-loss curve was retrospectively invented. Exact generation-129
in-memory progress and a complete historical GUI error log are unavailable.
Timing is host/JVM sensitive. Reproduction needs the pinned checkpoint payloads to
remain available under existing retention; this task did not change retention.

## Validation, reproduction and work-unit boundary

Applicable user governance and the repository's active
`source/CHESS_SEARCH_CONTRACT.md` were inspected. Its distinction between mechanical
speed, tree shape and playing strength is preserved; its canon was not modified.
Root `CODEXLOG_CURRENT.md` and `VERSION_STATE.txt` were independently absent, so no
journal or version-finalization capability was activated or created.

Changed tracked scope is this report, its link from `BRN_DIAGNOSTICS.md`,
`app/src/main/java/com/ohinteractive/seedv6/tools/search/Brn2InferenceBenchmark.java`,
and its targeted test. Existing evaluator, training, search, promotion, BRN-0/1 and
NNUE implementations were unchanged. Starting Git HEAD was the supplied canonical
commit; the sole inherited untracked item, `app/bin/`, was preserved and excluded
from the diagnostic commit. The focused commit hash and final Git state are reported
in the completion message. No push, deployment or history rewrite was performed.

Targeted validation: **16 tests, zero failures/errors/skips** (15 existing
`Brn2DiagnosticsTest` cases and the new full/cached/update all-child equivalence
test). The latter visits all special-move fixtures and repeats sibling updates.
The final targeted command rebuilt the new tool and passed. Runtime checks include
the full static/symmetry corpus, four retained earlier model loads/evaluations,
90 bounded base-search invocations (30 warmups + 60 measured), one bounded qshadow
pass, and the trained inference workload with its pre-timing numerical comparison.
Every measured main/q/total accounting check passed; repeated search fields matched.

Store evidence checks verified checksums and cross-links, the accepted publication
chain and the actual pinned models. A store file inventory with sizes/mtimes and
hashes for all files below 1 MB was unchanged between the audit snapshot and final
verification. Selected model and optimizer SHA-256 values were independently
rechecked. The first fingerprint was taken after the initial static/base-search
reads; it is not claimed as a snapshot before those reads. Loaded-model integrity
records also report unchanged model hashes. References and generation-129 evidence
remained the same throughout observed checks.

Generated local evidence follows the existing `app/build/` convention in
`app/build/brn2-canonical-g128/`: `static-final.jsonl`, `static-g0.jsonl`,
`static-g10.jsonl`, `static-g31.jsonl`, `static-g64.jsonl`, 30 `search-*.jsonl` files,
`kiwipete-qshadow.jsonl`, `inference.jsonl`, `store-audit.json`, fingerprints,
`analysis.json`, process-watchdog receipts, targeted-test logs and the one-off
read/analysis/runner scripts. These generated artifacts are ignored rather than
added to Git; this committed report preserves the key results and complete loss
history. `artifact-sha256.json` records the measured output-file hashes.

Build and focused tests:

```powershell
.\gradlew.bat :app:classes --console=plain
.\gradlew.bat :app:test --tests '*Brn2InferenceBenchmarkTest' --tests '*Brn2DiagnosticsTest' --console=plain
```

The existing `:app:brn2Diagnostics` task accepts exact checkpoint paths via
`-Pbrn2Checkpoint=...`, `-PnnueCheckpoint=...`, a new output path through
`-PdiagnosticOutput=...`, and `-PdiagnosticArgs=...`. Evaluator-only settings used
`--depth=0 --symmetry=true`. Search settings were
`--positions=<one corpus name> --drivers=<brn2 or nnue> --depth=4 --nodes=1000000 --time-ms=10000 --warmup=1 --repetitions=2 --qshadow=false`.
The recorded runner launches Java directly with each process wrapped in the
40-second Python watchdog; the Gradle task alone supplies only cooperative bounds.
The qshadow runner uses the separate bounds and sampling settings described above.

After `:app:classes`, the new standalone probe is:

```powershell
java -Xms256m -Xmx1024m -cp 'app/build/classes/java/main;app/build/resources/main;app/build/install/seedv6/lib/*' com.ohinteractive.seedv6.tools.search.Brn2InferenceBenchmark '<exact BRN checkpoint above>' '<exact NNUE checkpoint above>' 60000 3
```

The measured invocation was enclosed in `subprocess.run(..., timeout=90)` and its
JSONL stdout retained. Diagnostic outputs were always new files. No external
browser verification was needed for these CLI/runtime measurements.

Deliberately skipped: `fullCheck`, slow/GUI suites, broad benchmarks, long self-play,
training Resume, strength matches, policy/target/calibration changes, schema-1 model
loading, inference optimization and architecture experiments. They are outside this
diagnostic unit and no changed shared production boundary justifies them. The
incomplete depth-4 Kiwipete result is reported as a bounded result, not hidden by a
larger/unrestricted search.

Human actions required after this prompt: **None**. This diagnostic work unit does
not assert user acceptance or operational/deployment completion.

## Complete persisted loss/decision history

`Train` is final Candidate training-partition loss; `Candidate` and `Incumbent` are
held-out losses on that row's partition. P = promoted; R = retained. Values are
rounded here to six decimals; full-precision checksummed values remain in the source
history and `store-audit.json`.

| Generation | Train | Candidate | Incumbent | Decision | Resulting Best gen |
|---|---|---|---|---|---|
| 1 | 0.062800 | 0.246837 | 0.360188 | P | 1 |
| 2 | 0.052277 | 0.240973 | 0.363328 | P | 2 |
| 3 | 0.074572 | 0.272749 | 0.273491 | P | 3 |
| 4 | 0.067495 | 0.239693 | 0.291719 | P | 4 |
| 5 | 0.099587 | 0.237448 | 0.214795 | R | 4 |
| 6 | 0.102638 | 0.300975 | 0.266859 | R | 4 |
| 7 | 0.076013 | 0.269338 | 0.331245 | P | 7 |
| 8 | 0.078161 | 0.129318 | 0.143414 | P | 8 |
| 9 | 0.075607 | 0.129254 | 0.122872 | R | 8 |
| 10 | 0.100420 | 0.321071 | 0.325951 | P | 10 |
| 11 | 0.060927 | 0.263278 | 0.254449 | R | 10 |
| 12 | 0.091965 | 0.131135 | 0.148406 | P | 12 |
| 13 | 0.086744 | 0.157712 | 0.190401 | P | 13 |
| 14 | 0.082411 | 0.252818 | 0.237421 | R | 13 |
| 15 | 0.082398 | 0.232534 | 0.257997 | P | 15 |
| 16 | 0.081989 | 0.264838 | 0.208127 | R | 15 |
| 17 | 0.080075 | 0.188571 | 0.197663 | P | 17 |
| 18 | 0.085251 | 0.192456 | 0.234423 | P | 18 |
| 19 | 0.063147 | 0.187307 | 0.241442 | P | 19 |
| 20 | 0.091514 | 0.180352 | 0.162141 | R | 19 |
| 21 | 0.067611 | 0.096757 | 0.097422 | P | 21 |
| 22 | 0.090130 | 0.255156 | 0.288580 | P | 22 |
| 23 | 0.087192 | 0.179373 | 0.172238 | R | 22 |
| 24 | 0.078232 | 0.220200 | 0.178936 | R | 22 |
| 25 | 0.061023 | 0.269937 | 0.272496 | P | 25 |
| 26 | 0.080822 | 0.143444 | 0.143836 | P | 26 |
| 27 | 0.064500 | 0.268260 | 0.297764 | P | 27 |
| 28 | 0.073866 | 0.160899 | 0.208292 | P | 28 |
| 29 | 0.088901 | 0.221484 | 0.192770 | R | 28 |
| 30 | 0.085329 | 0.252142 | 0.231576 | R | 28 |
| 31 | 0.065278 | 0.191449 | 0.196325 | P | 31 |
| 32 | 0.075621 | 0.217391 | 0.207108 | R | 31 |
| 33 | 0.080683 | 0.273802 | 0.249916 | R | 31 |
| 34 | 0.084169 | 0.176070 | 0.132419 | R | 31 |
| 35 | 0.064173 | 0.171531 | 0.196247 | P | 35 |
| 36 | 0.066413 | 0.128627 | 0.129457 | P | 36 |
| 37 | 0.073408 | 0.203357 | 0.174927 | R | 36 |
| 38 | 0.112177 | 0.197416 | 0.238750 | P | 38 |
| 39 | 0.095561 | 0.315059 | 0.295944 | R | 38 |
| 40 | 0.080203 | 0.133236 | 0.162794 | P | 40 |
| 41 | 0.102451 | 0.285012 | 0.248032 | R | 40 |
| 42 | 0.087158 | 0.182471 | 0.167539 | R | 40 |
| 43 | 0.091974 | 0.280194 | 0.285087 | P | 43 |
| 44 | 0.076405 | 0.195016 | 0.171165 | R | 43 |
| 45 | 0.088043 | 0.135464 | 0.154379 | P | 45 |
| 46 | 0.087300 | 0.144102 | 0.175589 | P | 46 |
| 47 | 0.092113 | 0.189982 | 0.179514 | R | 46 |
| 48 | 0.076190 | 0.084827 | 0.095390 | P | 48 |
| 49 | 0.082454 | 0.273861 | 0.263276 | R | 48 |
| 50 | 0.100545 | 0.161578 | 0.141862 | R | 48 |
| 51 | 0.073371 | 0.239142 | 0.236497 | R | 48 |
| 52 | 0.076275 | 0.174269 | 0.153984 | R | 48 |
| 53 | 0.087710 | 0.225282 | 0.223643 | R | 48 |
| 54 | 0.057760 | 0.230126 | 0.286859 | P | 54 |
| 55 | 0.101431 | 0.136585 | 0.132632 | R | 54 |
| 56 | 0.093403 | 0.153142 | 0.143543 | R | 54 |
| 57 | 0.102491 | 0.171964 | 0.199634 | P | 57 |
| 58 | 0.089499 | 0.259321 | 0.233381 | R | 57 |
| 59 | 0.069543 | 0.169971 | 0.132165 | R | 57 |
| 60 | 0.096509 | 0.200331 | 0.192827 | R | 57 |
| 61 | 0.084795 | 0.095880 | 0.137070 | P | 61 |
| 62 | 0.081259 | 0.199571 | 0.213957 | P | 62 |
| 63 | 0.083055 | 0.151564 | 0.162197 | P | 63 |
| 64 | 0.064906 | 0.175654 | 0.189274 | P | 64 |
| 65 | 0.107238 | 0.297617 | 0.286413 | R | 64 |
| 66 | 0.085271 | 0.147370 | 0.149265 | P | 66 |
| 67 | 0.090103 | 0.124619 | 0.126435 | P | 67 |
| 68 | 0.097099 | 0.159083 | 0.139875 | R | 67 |
| 69 | 0.118073 | 0.179606 | 0.167805 | R | 67 |
| 70 | 0.076936 | 0.232499 | 0.240405 | P | 70 |
| 71 | 0.103721 | 0.109537 | 0.118137 | P | 71 |
| 72 | 0.094523 | 0.110115 | 0.120060 | P | 72 |
| 73 | 0.073575 | 0.066595 | 0.069222 | P | 73 |
| 74 | 0.094503 | 0.181273 | 0.155067 | R | 73 |
| 75 | 0.076617 | 0.214510 | 0.231806 | P | 75 |
| 76 | 0.076420 | 0.151650 | 0.160319 | P | 76 |
| 77 | 0.099855 | 0.224460 | 0.212647 | R | 76 |
| 78 | 0.081506 | 0.182388 | 0.154012 | R | 76 |
| 79 | 0.073454 | 0.167644 | 0.228851 | P | 79 |
| 80 | 0.098372 | 0.122014 | 0.118010 | R | 79 |
| 81 | 0.078887 | 0.208438 | 0.217768 | P | 81 |
| 82 | 0.070918 | 0.158819 | 0.171667 | P | 82 |
| 83 | 0.104991 | 0.210605 | 0.199163 | R | 82 |
| 84 | 0.084493 | 0.216917 | 0.217889 | P | 84 |
| 85 | 0.099918 | 0.136007 | 0.129060 | R | 84 |
| 86 | 0.088313 | 0.238816 | 0.245775 | P | 86 |
| 87 | 0.079005 | 0.169725 | 0.174684 | P | 87 |
| 88 | 0.081638 | 0.182867 | 0.177268 | R | 87 |
| 89 | 0.082018 | 0.187852 | 0.181486 | R | 87 |
| 90 | 0.070710 | 0.172508 | 0.156852 | R | 87 |
| 91 | 0.074141 | 0.237198 | 0.274960 | P | 91 |
| 92 | 0.082910 | 0.185977 | 0.199662 | P | 92 |
| 93 | 0.084493 | 0.149424 | 0.184679 | P | 93 |
| 94 | 0.080946 | 0.127042 | 0.157568 | P | 94 |
| 95 | 0.101901 | 0.224832 | 0.224770 | R | 94 |
| 96 | 0.109712 | 0.159940 | 0.155879 | R | 94 |
| 97 | 0.085801 | 0.183230 | 0.183282 | P | 97 |
| 98 | 0.081551 | 0.233538 | 0.260545 | P | 98 |
| 99 | 0.083528 | 0.189650 | 0.186777 | R | 98 |
| 100 | 0.096824 | 0.127288 | 0.152239 | P | 100 |
| 101 | 0.080122 | 0.144217 | 0.140311 | R | 100 |
| 102 | 0.077931 | 0.197398 | 0.190296 | R | 100 |
| 103 | 0.084787 | 0.279917 | 0.281475 | P | 103 |
| 104 | 0.097148 | 0.170250 | 0.174897 | P | 104 |
| 105 | 0.079656 | 0.099760 | 0.085266 | R | 104 |
| 106 | 0.082543 | 0.138869 | 0.139803 | P | 106 |
| 107 | 0.072665 | 0.129834 | 0.128755 | R | 106 |
| 108 | 0.082826 | 0.168293 | 0.171874 | P | 108 |
| 109 | 0.088883 | 0.261690 | 0.251780 | R | 108 |
| 110 | 0.077378 | 0.183171 | 0.150152 | R | 108 |
| 111 | 0.090224 | 0.084662 | 0.085704 | P | 111 |
| 112 | 0.130667 | 0.248047 | 0.220474 | R | 111 |
| 113 | 0.076643 | 0.242961 | 0.252236 | P | 113 |
| 114 | 0.104128 | 0.175912 | 0.146606 | R | 113 |
| 115 | 0.098539 | 0.265641 | 0.196292 | R | 113 |
| 116 | 0.099885 | 0.184089 | 0.180373 | R | 113 |
| 117 | 0.077603 | 0.141315 | 0.103763 | R | 113 |
| 118 | 0.076975 | 0.187538 | 0.208528 | P | 118 |
| 119 | 0.107796 | 0.152735 | 0.146647 | R | 118 |
| 120 | 0.087542 | 0.177695 | 0.195083 | P | 120 |
| 121 | 0.085409 | 0.124198 | 0.135136 | P | 121 |
| 122 | 0.075085 | 0.197125 | 0.183102 | R | 121 |
| 123 | 0.108054 | 0.213560 | 0.173454 | R | 121 |
| 124 | 0.082917 | 0.140559 | 0.140009 | R | 121 |
| 125 | 0.092214 | 0.170845 | 0.144619 | R | 121 |
| 126 | 0.079993 | 0.115755 | 0.111645 | R | 121 |
| 127 | 0.081174 | 0.101304 | 0.099038 | R | 121 |
| 128 | 0.114970 | 0.223880 | 0.188835 | R | 121 |
