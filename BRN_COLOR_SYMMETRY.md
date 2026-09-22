# BRN-2 color/perspective symmetry diagnostic

Measured 2026-09-22 UTC in the authoritative `C:\projects\seed\java\seedv6`
repository, starting at `c7a7c6228ff9057e35398b6027266a4ee51ae6e3`.

**BRN-2 g315 has material color/perspective asymmetry.** On 248 fixed corpus
positions, its median absolute symmetry residual is 9,999 search units; NNUE's
is zero, with maximum one. On 6,620 sampled Kiwipete qsearch positions, the BRN
median is 9,914. The symmetry-dependent component of sampled tactical edge deltas
has median magnitude 6,545.5, versus 11,787 for the original deltas. This is large
enough to contribute materially to evaluation instability and local stand-pat
decisions. It does not establish what fraction of total qsearch expansion it causes.

The evidence fits outcome **B**, with qualified side/ply observations rather than
a clean **D** sign-alternation mechanism. Substantial color-invariant volatility
remains. No remedy, architecture, training, mapping, evaluator behavior, search
semantics, pruning, move ordering, TT behavior, or GUI was changed. BRN-2 has not
been fixed, and this diagnostic does not establish strength or acceptance.

## Structural contract and implementation evidence

BRN-2 must learn this invariance; it is not guaranteed by construction.

- `core/brn/BrnFeatures.extract` enumerates absolute ascending squares and absolute
  piece codes. A node feature is `(code - 1) * 64 + square`. White and Black
  have different codes, with Black's color bit at bit 3. There is no us/them or
  side-to-move board canonicalization.
- `core/brn/BrnFeatureSchema.relationIndex` orders endpoints by absolute square
  and indexes both absolute codes and geometric displacement. Rank reversal can
  reverse endpoint order and displacement. Canonical endpoint order is not
  color canonicalization.
- `core/brn2/Brn2Model` has independent node rows, independent `RELATION_A_ROW`
  and `RELATION_B_ROW` tables, and 64 independent raw status rows. These are
  ordinary independently initialized/trainable parameters, without color ties.
  Shared channel biases, pooling and head weights do not impose symmetry on
  the independent inputs or relation endpoint parameters.
- `Brn2Workspace` composes each endpoint with status context and local ReLU,
  pools into global ReLU and applies a linear head followed by tanh. Status
  context includes STM, castling, en-passant, halfmove and fullmove bits. There
  is no post-head color sign flip. `Brn2Model.evaluate` and `SearchEvaluation.brn2`
  implement the established side-to-move contract.

NNUE is structurally symmetric in exact arithmetic for this transformation.
`core/nnue/NnueFeatureSchema.relativePieceChannel` uses friendly/enemy channels;
`orientSquare` uses `square ^ 56` for Black, for both king and piece squares.
`NnueAccumulator` builds both perspectives using the same network transformer
weights and biases. Color reversal exchanges the two perspective feature
multisets. `writeInput` puts the current side first, so swapping STM restores
the same concatenated input to the shared head. Castling, EP and counters are
not NNUE features; STM only selects concatenation order. Symmetry is not a
learned approximation here, although prediction quality is learned.

Bit-exact numerical invariance is not promised: accumulators visit absolute
squares in ascending order, which changes the addition order after reflection.
Float rounding and incremental updates can therefore differ. The measured
NNUE normalized discrepancies are below 5.3e-7; occasional score rounding differs
by one. `NnueEvaluator.raw()` and `boundedValue()` already expose its actual
pre-tanh and normalized values, so these are measured directly, not inferred.

## Exact transformation and rule-state scope

No complete existing board color-reversal utility was found. The new tool-only
`ColorReversal.transform` operates on a detached six-long board:

1. Reverse bytes of P0/P1/P2: square `s` maps to `s ^ 56`, preserving files.
2. XOR P3 with occupied squares, then reverse bytes: every piece swaps color;
   empty squares remain empty. Thus e2 maps to e7, including kings and pawns.
3. Toggle the STM bit. Exchange K with k and Q with q, retaining castle wing.
4. Map a present EP target with `^56`; retain canonical zero for absent EP.
   Reject noncanonical EP encodings rather than silently dropping state.
5. Preserve the halfmove clock, numeric fullmove label and remaining status bits.
6. Recompute the Zobrist key from transformed pieces, STM, rights and EP.

The six-long representation has P0-P2 type bits, P3 Black occupancy, STATUS at
index 4 and KEY at index 5. STATUS contains STM at bit 0, four castling bits at
1-4, six EP bits at 5-10, seven halfmove bits at 11-17 and ten fullmove bits at
18-27. Absent EP decodes as `Value.INVALID` (`Integer.MIN_VALUE`), not -1.
The transform preserves all bits outside the explicitly transformed fields.

**Fullmove numbering caveat:** Black's move increments the FEN fullmove number.
Preserving the numeric label per snapshot is an involution and preserves chess
legality and draw state. Playing the mapped move on T(P) therefore produces the
same placement, key, rights, EP and halfmove state as T(C), but its fullmove label
is +1 for an original White move and -1 for an original Black move. Tests check
this explicitly. Edge diagnostics evaluate T(P) and T(C) directly with each
snapshot's original numeric label; they do not pretend fullmove bookkeeping
commutes. This isolates color reversal at equal counter values. BRN's sensitivity
to advancing that label in actual opposite-color games is outside this test.

There is no supplied preceding repetition history to transform: corpus searches
use singleton root history. Sampled model evaluation is independent of repetition
adjudication. Check/terminal sample evaluations are marked diagnostic only and
are never treated as actual stand-pat values. No tempo-only toggle test is used.

Tests verify every mapped legal move on all 15 roots and their reversed roots,
not merely equal move counts, plus every root/child involution and square mapping.
Special move categories, EP in both directions, individual castling bits, clocks,
kings, pawn directions and board preservation are covered.

## Checkpoints and score convention

BRN-2 generation 315, optimizer step 484,034:

```text
E:\SeedV6-Networks\BRN\BRN-2\training\checkpoints\g000315-s000484034-700d920ce6a34a8584539ebd12276972d54e135c17e3384d7f865168be370e36
model SHA-256: ef2760727a1485da30a451c32eeec8e5d541e0c83de97f1d87d8cb57524665fe
training SHA-256: 7dff164f7708077131ef72e5dbc14313a38b3e606c000f090cbb36ed2be9d450
```

Trained NNUE Generator, generation 74, optimizer step 8,942:

```text
E:\SeedV6-Networks\NNUE\training\checkpoints\g000074-s000008942-2e22ccbfdeb18f5f26f91f0df183411ac1d01844ccd43e56e9005ee3f5837bc6
model SHA-256: 3279ff72654f56c8f5ce3cc89cc55d1f253de6ae9df00bd61a41293f57dd16a9
training SHA-256: 453b7259d4e8041fa4abb8cc0887540434871d2b151c33676f36e749e0c6cb34
```

Both are exact checkpoint inputs, never mutable Best/latest selection. Eight
checkpoint files and both serialized loaded models were unchanged after the runs.

The existing 15-root corpus SHA-256 is
`0503b850d5ed49686e72e601b1216270cfd40fc3bac18fac4dae697b323f3bd8`.
It supplies 15 roots, 233 legal children and 233 edges. Both evaluators use their
production root rebuild and parent-child accumulator paths, in independent
scratch for each orientation. Q snapshots use fresh production root rebuilds.

Principal residual is `r(P) = score(P) - score(T(P))`, **without negation**.
The corresponding normalized and raw residuals use the same subtraction.
The BRN mapping remains `sign(v) * max(1, round(abs(v) * 32511))`, with zero
mapped to zero. NNUE uses V1 scale 32511. These units are not calibrated centipawns.
Threshold counts are strictly greater than 100, 1,000, 5,000, 10,000 and 20,000
search units (normalized thresholds divide each by 32511). They span rounding-
scale versus plainly material differences; they are descriptive, not risk gates.
Quantiles interpolate at `(n-1)*p`; correlation is Pearson on the paired values.

## Static results

All absolute statistics below concern the residual, not the score magnitude.

| Population / evaluator | n | Signed mean | Absolute mean | Median abs | p95 abs | p99 abs | Max abs | Correlation |
|---|---|---|---|---|---|---|---|---|
| all / BRN2 | 248 | -4,009.31 | 12,121.86 | 9,999 | 30,801.6 | 34,874.43 | 40,595 | -0.12903359 |
| all / NNUE | 248 | 0.00403226 | 0.00403226 | 0 | 0 | 0 | 1 | 1.00000000 |
| roots / BRN2 | 15 | -6,077.2 | 9,920.67 | 7,900 | 23,292.8 | 23,609.76 | 23,689 | -0.23279522 |
| roots / NNUE | 15 | 0 | 0 | 0 | 0 | 0 | 0 | 1 |
| children / BRN2 | 233 | -3,876.18 | 12,263.57 | 10,238 | 30,922.4 | 34,879.08 | 40,595 | -0.12580571 |
| children / NNUE | 233 | 0.00429185 | 0.00429185 | 0 | 0 | 0 | 1 | 1.00000000 |

Original-side breakdown (all roots and children; uneven sample sizes):

| Population / evaluator | n | Signed mean | Absolute mean | Median abs | p95 abs | p99 abs | Max abs | Correlation |
|---|---|---|---|---|---|---|---|---|
| white / BRN2 | 42 | -7,824.19 | 9,628.1 | 7,270 | 23,660.7 | 30,753.06 | 34,129 | -0.09455569 |
| white / NNUE | 42 | 0.02380952 | 0.02380952 | 0 | 0 | 0.59000000 | 1 | 1.00000000 |
| black / BRN2 | 206 | -3,231.51 | 12,630.3 | 10,658.5 | 30,947.25 | 34,887.45 | 40,595 | -0.15051762 |
| black / NNUE | 206 | 0 | 0 | 0 | 0 | 0 | 0 | 1 |

Counts strictly above each absolute search-score threshold:

| Population / evaluator | >100 | >1,000 | >5,000 | >10,000 | >20,000 |
|---|---|---|---|---|---|
| all / BRN2 | 225 | 212 | 167 | 124 | 60 |
| all / NNUE | 0 | 0 | 0 | 0 | 0 |
| white / BRN2 | 41 | 40 | 30 | 16 | 6 |
| white / NNUE | 0 | 0 | 0 | 0 | 0 |
| black / BRN2 | 184 | 172 | 137 | 108 | 54 |
| black / NNUE | 0 | 0 | 0 | 0 | 0 |

Normalized and pre-tanh residuals over all 248 positions:

Near-one NNUE correlations in the tables are rounded for display. The combined
static search-score correlation is 0.9999999999916128; the qsample search-score
correlation is 0.9999999999974174. Neither is asserted to be bit-exact invariance.

| Population / evaluator | n | Signed mean | Absolute mean | Median abs | p95 abs | p99 abs | Max abs | Correlation |
|---|---|---|---|---|---|---|---|---|
| BRN2 / normalized | 248 | -0.12332318 | 0.37285512 | 0.30754173 | 0.94741934 | 1.07 | 1.25 | -0.12903453 |
| BRN2 / rawPreTanh | 248 | -0.14739703 | 0.42462760 | 0.33622572 | 1.11 | 1.4 | 1.47 | -0.10600257 |
| NNUE / normalized | 248 | -3.05461e-08 | 5.65406e-08 | 2.55605e-08 | 2.29545e-07 | 3.47874e-07 | 4.292e-07 | 1.00000000 |
| NNUE / rawPreTanh | 248 | -3.84058e-08 | 6.87038e-08 | 2.98023e-08 | 2.68221e-07 | 4.13731e-07 | 4.76837e-07 | 1.00000000 |

Kiwipete's root itself has a moderate residual: BRN 6,869 versus 1,166, residual
5,703; NNUE is 16,685 in both orientations. Its root-plus-48-child family has
median absolute residual **26,939**, the largest of the 15 families. Next is
queen exchanges at 14,914, followed by quiet endgame at 14,386.5. Its maximum
40,595 is also the corpus maximum: `d2g5` yields BRN -19,060 versus +21,535.
NNUE gives -18,746 in both orientations on that child. Thus the pathology is
associated with unusually large nearby static errors, not an unusually large
root error alone. Family mobility/population weights differ.

## Edge symmetry and tactical magnitude

For each edge, `d = -score(C) - score(P)` and
`dT = -score(T(C)) - score(T(P))`. The signed edge error is
`d - dT = -(r(P) + r(C))`. This is distinct from simply subtracting opposite-STM
scores. Raw rows record both deltas, their signed/absolute difference and the
following exact decomposition in search units:

```text
symmetric component S = (d + dT) / 2
antisymmetric component A = (d - dT) / 2
d = S + A
```

This decomposition is an analysis of the existing predictions, not an alternative
evaluator supplied to search. S and A can reinforce or cancel; their absolute
magnitudes are not additive shares of a cause, explained variance or node savings.

| Overlapping category | n | BRN median abs error | BRN p95 | BRN max | NNUE median | NNUE p95 | NNUE max |
|---|---|---|---|---|---|---|---|
| all | 233 | 9,869 | 28,551 | 45,612 | 0 | 0 | 1 |
| quiet | 210 | 9,664 | 28,561.5 | 45,612 | 0 | 0 | 1 |
| capture | 16 | 20,444 | 28,023.75 | 32,937 | 0 | 0 | 0 |
| gives-check | 5 | 4,897 | 12,595.2 | 14,364 | 0 | 0 | 0 |
| evasion | 3 | 3,598 | 6,335.8 | 6,640 | 0 | 0 | 0 |
| promotion | 4 | 5,987 | 13,177.5 | 14,364 | 0 | 0 | 0 |
| castle | 2 | 11,610.5 | 13,991.45 | 14,256 | 0 | 0 | 0 |
| en-passant | 1 | 8,906 | 8,906 | 8,906 | 0 | 0 | 0 |
| non-capture-non-promotion | 213 | 9,359 | 28,551 | 45,612 | 0 | 0 | 1 |

On the static corpus, BRN's original absolute delta median is 4,992, while its
absolute A median is 4,934.5 and absolute S median is 6,062.5. Captures have original
median 4,578 versus A median 10,222, illustrating cancellation rather than a
universal reduction in volatility when removing the color-dependent component.
NNUE's static edge error has mean 0.004292 and maximum one (one quiet edge);
all other listed special categories are exactly zero in integer search units.

The static corpus is not the earlier 452,292-edge qsearch population. For a
matched comparison in the actual Kiwipete tree, 4,467 sampled edges have actual
driver statics at both endpoints. Both BRN and NNUE are evaluated on these same
original and transformed snapshots:

| Metric (absolute) | n | Mean | Median | p95 | Max |
|---|---|---|---|---|---|
| BRN2 edge error | 4467 | 16,118.78 | 13,091 | 41,534 | 84,527 |
| BRN2 original delta | 4467 | 14,334.41 | 11,787 | 36,784.4 | 59,395 |
| BRN2 transformed delta | 4467 | 11,847.12 | 9,010 | 32,058 | 57,891 |
| BRN2 S component | 4467 | 10,127.17 | 7,625.5 | 27,842.8 | 54,378 |
| BRN2 A component | 4467 | 8,059.39 | 6,545.5 | 20,767 | 42,263.5 |
| NNUE edge error | 4467 | 0.00201478 | 0 | 0 | 1 |
| NNUE original delta | 4467 | 6,928.36 | 4,448 | 22,310.3 | 45,463 |
| NNUE transformed delta | 4467 | 6,928.36 | 4,448 | 22,310.3 | 45,463 |
| NNUE S component | 4467 | 6,928.36 | 4,448 | 22,310.3 | 45,463 |
| NNUE A component | 4467 | 0.00100739 | 0 | 0 | 0.50000000 |

The sampled BRN original delta median 11,787 closely tracks the previously
measured full-tree median 11,854.5. Edge-symmetry error median 13,091 is 110.4%
of that historical median; half-error/A median 6,545.5 is 55.2%. On the current
matched sample it is 55.5% of the original median. These are ratios of population
medians, not a per-edge attributable percentage. The S median of 7,625.5 still
exceeds NNUE's 4,448, so color-invariant tactical variation also remains material.

## Bounded Kiwipete qsearch sample

BRN-2 drives depth 4 with a 1,000,000-node bound, no wall-clock cutoff, one thread,
cold 262,144-entry TT, mate-distance-only selectivity and full windows. Two runs
both complete with score 3,665, move `d5d6`, PV `d5d6 a6e2 d6e7 e2f3`,
671,841 total nodes, 13,348 main nodes and 658,493 qnodes (98.0132%).
A fresh run with tracing/symmetry disabled matches every non-timing search field.

Sampling selects `(entryOrdinal-1) % 101 == 0` while
`(entryOrdinal-1) / 101 < 8192`, across all iterative attempts. Of 668,618 observed
q positions, 6,620 are selected. The cap never truncates this run; the stride
covers its complete observed sequence. Repeated positions/visits are retained.
Selection is independent of scores, STM, check state and cutoff class. It is a
deterministic systematic sample, not a random independent sample.

The existing qshadow still evaluates NNUE at every actual driver static (stride 1).
During search, the added hook only copies selected detached boards and metadata.
Both original/transformed model evaluations and aggregation run after search.
For all 5,842 sampled positions with actual BRN statics, rebuilding reproduces
the driver's integer score exactly. The other 778 are checked diagnostic statics.

| Population / evaluator | n | Signed mean | Absolute mean | Median abs | p95 abs | p99 abs | Max abs | Correlation |
|---|---|---|---|---|---|---|---|---|
| BRN2 / score | 6,620 | -3,369.72 | 12,133.04 | 9,914 | 31,316.15 | 41,494.22 | 58,207 | 0.59787970 |
| BRN2 / normalized | 6,620 | -0.10364882 | 0.37319796 | 0.30494752 | 0.96325128 | 1.28 | 1.79 | 0.59787969 |
| BRN2 / rawPreTanh | 6,620 | -0.15160523 | 0.52429971 | 0.43400761 | 1.33 | 1.83 | 3.19 | 0.62500783 |
| NNUE / score | 6,620 | -0.000302115 | 0.00211480 | 0 | 0 | 0 | 1 | 1.00000000 |
| NNUE / normalized | 6,620 | 1.91493e-10 | 5.36885e-08 | 3.77494e-08 | 1.7342e-07 | 2.66073e-07 | 5.27749e-07 | 1.00000000 |
| NNUE / rawPreTanh | 6,620 | -2.78553e-11 | 7.81627e-08 | 5.96046e-08 | 2.38419e-07 | 3.57628e-07 | 5.96046e-07 | 1.00000000 |

| Evaluator | >100 | >1,000 | >5,000 | >10,000 | >20,000 |
|---|---|---|---|---|---|
| BRN2 | 6571 | 6176 | 4738 | 3282 | 1293 |
| NNUE | 0 | 0 | 0 | 0 | 0 |

NNUE is measured on exactly the same 6,620 sampled boards, not a separately
selected NNUE tree. Fourteen NNUE residuals are one in absolute integer units;
the remaining 6,606 are zero. No NNUE residual exceeds any configured threshold.

The largest BRN outlier is ordinal 202,203, qply 4, Black to move, DRIVER_ONLY:
original +28,371 versus reversed -29,836, residual +58,207. Its z values are
1.344078 and -1.574314. Ordinal 120,797 (qply 12, Black, NEITHER) has -22,798
versus +31,895, residual -54,693. Ordinal 50,299 (qply 12, White, SHADOW_ONLY)
has -25,309 versus +29,036, residual -54,345. JSONL retains complete board longs,
transformed board longs, window, iteration, reason, both models and available
parent boards for every selected position, plus the 16 largest outlier IDs.

## Side, qply and decision context

| BRN group | n | Signed mean | Absolute mean | Median abs | p95 abs | Max abs |
|---|---|---|---|---|---|---|
| check/false | 5,842 | -3,490.72 | 12,014.11 | 9,789 | 30,984 | 58,207 |
| check/true | 778 | -2,461.14 | 13,026.05 | 10,958 | 32,716.5 | 51,028 |
| cutoff/BOTH | 1,953 | -1,365.9 | 10,655.88 | 8,494 | 27,163.8 | 54,234 |
| cutoff/DRIVER_ONLY | 1,161 | 1,545.66 | 11,932.12 | 10,044 | 28,865 | 58,207 |
| cutoff/NEITHER | 1,428 | -6,752.57 | 12,477.66 | 10,172 | 32,656 | 54,693 |
| cutoff/SHADOW_ONLY | 1,044 | -9,229.44 | 14,362.74 | 11,701.5 | 35,979.8 | 54,345 |
| cutoff/UNCLASSIFIED | 1,034 | -2,085.37 | 12,421.45 | 9,925.5 | 32,297 | 51,028 |
| parity/0 | 3,102 | -2,401.8 | 11,873.38 | 9,623 | 31,356.05 | 58,207 |
| parity/1 | 3,518 | -4,223.19 | 12,362 | 10,150 | 31,195.7 | 54,234 |
| side/black | 3,943 | -6,184.11 | 12,915.73 | 10,730 | 32,224.7 | 58,207 |
| side/white | 2,677 | 775.63 | 10,980.21 | 8,765 | 29,194 | 54,345 |
| stand-pat/CONTINUE | 2,472 | -7,798.63 | 13,273.79 | 10,697 | 34,312 | 54,693 |
| stand-pat/CUTOFF | 3,114 | -280.38 | 11,131.71 | 9,066.5 | 27,912.35 | 58,207 |
| stand-pat/INELIGIBLE | 1,034 | -2,085.37 | 12,421.45 | 9,925.5 | 32,297 | 51,028 |

There is a clear **population association with STM**, but not a demonstrated
fixed global color bias: sampled boards differ between groups. Black's mean
residual is -6,184.11 versus White's +775.63. Black's residual is negative at
2,671 of 3,943 samples, positive at 1,270, and zero at two; White's is negative
at 1,243 of 2,677 and positive at 1,434.

Even and odd qplies both have negative mean residuals (-2,401.80 and -4,223.19).
Their median absolute errors are similar (9,623 and 10,150). Odd samples are
82.46% Black to move, versus 33.59% of even samples. Conditioning on STM,
even/odd signed means are +340.37/+2,228.84 for White and -7,822.98/-5,595.44
for Black. Thus aggregate parity is substantially mixed with changing color
and board populations. It is not a clean alternating sign defect.

| qply | n | Signed mean | Absolute mean | Median abs | p95 abs | Max abs |
|---|---|---|---|---|---|---|
| 0 | 118 | -4,802.92 | 11,846.81 | 8,921.5 | 34,765.85 | 44,626 |
| 1 | 204 | -7,060.07 | 13,174.7 | 11,576.5 | 32,431.6 | 43,229 |
| 2 | 120 | -6,824.05 | 13,545.43 | 11,924 | 32,300.65 | 37,161 |
| 3 | 242 | -7,563.38 | 13,486.93 | 13,474 | 30,223.3 | 40,839 |
| 4 | 294 | -3,957.35 | 13,643.93 | 12,153.5 | 30,881.4 | 58,207 |
| 5 | 437 | -5,057.32 | 12,565.32 | 10,085 | 30,264.4 | 54,234 |
| 6 | 417 | -1,138.81 | 11,592.42 | 9,855 | 28,886.8 | 44,167 |
| 7 | 550 | -6,329.15 | 12,754.35 | 10,881 | 30,480.6 | 46,147 |
| 8 | 475 | -2,076.43 | 11,937.8 | 9,668 | 29,739 | 52,967 |
| 9 | 619 | -4,231.8 | 11,792.86 | 9,717 | 31,532.2 | 45,379 |
| 10 | 515 | -1,839.69 | 11,415.77 | 9,367 | 29,829.5 | 49,504 |
| 11 | 575 | -3,683.25 | 12,336.84 | 9,332 | 32,439 | 52,617 |
| 12 | 524 | -3,736.58 | 12,320.81 | 9,549 | 33,417.75 | 54,693 |
| 13 | 502 | -2,684.48 | 11,639.4 | 9,326 | 30,934.7 | 53,484 |
| 14 | 406 | -436.95 | 10,758.69 | 8,217.5 | 30,472.75 | 49,138 |
| 15 | 335 | 157.4 | 12,844.78 | 11,084 | 31,050 | 47,411 |
| 16 | 230 | -1,458.75 | 11,142.79 | 8,531 | 31,427.75 | 43,849 |
| 17 | 54 | 2,531.83 | 9,122.98 | 6,389 | 23,918.15 | 31,621 |
| 18 | 3 | -7,261.33 | 8,644 | 3,941 | 18,319.4 | 19,917 |

Most qply means through 15 are negative; differences do not consistently
alternate. At qply 12/13 the trend reverses, and the very small late samples
must not be generalized (no qply-19 sample was selected). The earlier alternating
driver-only/shadow-only excess is therefore not independently explained by a
simple alternating symmetry bias. Large errors occur throughout the chain and
can interact with different windows, positions and color proportions.

Check samples have modestly larger absolute errors, but non-check samples are
the much larger population. The strongest negative conditional mean is at
SHADOW_ONLY nodes (-9,229.44), where BRN actually continues but NNUE reaches beta.
DRIVER_ONLY has mean +1,545.66. Classification is based on original actual scores
and the actual window; errors do not determine sample selection.

A further read-only comparison holds each observed beta fixed and asks whether
the transformed BRN score reaches it. Among 5,586 stand-pat-eligible samples:

| Original / transformed local decision | Count |
|---|---:|
| Cutoff / cutoff | 2,562 |
| Cutoff / continue | 552 |
| Continue / cutoff | 976 |
| Continue / continue | 1,496 |

Thus 1,528/5,586 (27.35%) cross the local cutoff boundary under color reversal.
Of 1,044 SHADOW_ONLY samples, 470 (45.02%) reach beta with the transformed BRN
score. This supplies a plausible local mechanism connecting asymmetry to work
continuation. It is not a substituted search: changing any evaluation changes
ancestor returns/windows and which descendants are visited. No node savings or
causal fraction of the 671,841 nodes is inferred.

## Reproduction and output

Run these PowerShell commands from the repository root with new output filenames
if the recorded files already exist. Output creation deliberately refuses overwrite.

```powershell
$inputs = @(
  '-Pbrn2Checkpoint=E:\SeedV6-Networks\BRN\BRN-2\training\checkpoints\g000315-s000484034-700d920ce6a34a8584539ebd12276972d54e135c17e3384d7f865168be370e36',
  '-PnnueCheckpoint=E:\SeedV6-Networks\NNUE\training\checkpoints\g000074-s000008942-2e22ccbfdeb18f5f26f91f0df183411ac1d01844ccd43e56e9005ee3f5837bc6'
)
foreach ($repeat in 1..2) {
  .\gradlew.bat :app:brn2Diagnostics @inputs "-PdiagnosticOutput=app/build/color-symmetry-final-static-$repeat.jsonl" '-PdiagnosticArgs=--symmetry=true --depth=0 --time-ms=-1 --label=g315-color-symmetry' --console=plain
}
.\gradlew.bat :app:brn2Diagnostics @inputs '-PdiagnosticOutput=app/build/color-symmetry-final-kiwipete.jsonl' '-PdiagnosticArgs=--symmetry=true --qshadow=true --drivers=brn2 --positions=middlegame-kiwipete --symmetry-stride=101 --symmetry-limit=8192 --depth=4 --nodes=1000000 --time-ms=-1 --repetitions=2 --label=g315-color-symmetry' --console=plain
.\gradlew.bat :app:brn2Diagnostics @inputs '-PdiagnosticOutput=app/build/color-symmetry-driver-only.jsonl' '-PdiagnosticArgs=--qshadow=false --drivers=brn2 --positions=middlegame-kiwipete --depth=4 --nodes=1000000 --time-ms=-1 --repetitions=1 --label=g315-color-symmetry-baseline' --console=plain
```

`--symmetry` defaults false; stride defaults 101 and cap 8,192. Positive bounds
are required and cap is at most 20,000. Search symmetry requires `--qshadow=true`
and its explicit NNUE input. Static symmetry can run with only BRN. Existing
schema-2 records remain compatible; new record types carry symmetry data and
`symmetry_policy.version=1`. `symmetry_model_integrity` verifies serialized
in-memory model hashes after measurements. Root/child, side and root-family
summaries include z, v and scores. Edge summaries use scores; raw endpoint
predictions permit additional analyses without pretending units are equivalent.

Local raw files are under ignored `app/build/` and excluded from the commit:

- `color-symmetry-final-static-1.jsonl` and `-2.jsonl`: byte-identical, 553,463 bytes each.
- `color-symmetry-final-kiwipete.jsonl`: 24,917,074 bytes, both repetitions.
- `color-symmetry-driver-only.jsonl`: fresh driving-only comparison.
- Matching `.log` files for those four command outputs.
- `color-symmetry-focused-verified.log`: final focused Gradle run.
- `color-symmetry-verify.py`, `color-symmetry-verification.json`:
  independent structured-output and accounting verification.
- `color-symmetry-checkpoints-before.json` and `-after.json`: checkpoint inventory/hashes.

Earlier development `color-symmetry-*` runs/logs are also retained locally;
the `final-*` runs above are the final measured implementation. This report
preserves the conclusions and key evidence if build cleanup removes raw data.

## Validation, changes and boundaries

Final focused command, **47 tests in seven suites; zero failures/errors/skips;
Gradle exit 0, 20 seconds**:

```powershell
.\gradlew.bat :app:test --tests '*ColorSymmetryDiagnosticsTest' --tests '*Brn2DiagnosticsTest' --tests '*QsearchDecisionTraceTest' --tests '*QuiescenceDiagnosticsTest' --tests '*Brn2SearchIntegrationTest' --tests '*NnueFeatureSchemaTest' --tests '*BoardRuleStateTest' --console=plain
python -B app/build/color-symmetry-verify.py
git diff --check
```

Eight new tests cover transformation/involution, move-set equivalence and
categories, special rule states, identical NNUE feature multisets, evaluator
isolation, model/board preservation, perspective algebra, quantiles/thresholds,
bounded selection/reset/options, unchanged searches under both drivers and
deterministic q reports. Existing suites add checkpoint integrity, production
evaluation paths, qsearch/trace accounting, rule clocks and NNUE feature semantics.
The first development run failed two tests because the test asserted -1 for
absent EP; correcting it to the verified engine sentinel fixed both. No test
failure was suppressed and no production EP behavior changed.

The independent Python verifier parsed **16,275 final JSONL records**, recalculated
all static and q symmetry summaries (including grouped statistics and edge
statistics), validated score mappings/signs, transformed snapshot fields,
selection ordinals, cutoff classification and all repeated non-timing fields.
It checked exact current driver-only search equality and total/main/q accounting.
Both model serialization hashes are unchanged in every final symmetry run;
all eight exact checkpoint files have identical inventory, length and SHA-256.
Every sampled actual BRN static matches its rebuilt integer score (5,842/5,842).
Static runs match completely, and q repeats match after removing timing and
normalizing the explicit repetition identifier.

`:app:fullCheck`, the full routine suite, slow NNUE/training/GUI suites and browser
QA were deliberately not run. The changes are bounded diagnostic instrumentation;
targeted tests and trained-checkpoint measurements cover their material risk.
There is no changed GUI or shared evaluator/search decision behavior. Optional
search-on-T(P) metamorphism was omitted: it is unnecessary for evaluator symmetry
and would introduce ordering/history/TT interpretation issues.

Changed files:

- `app/src/main/java/com/ohinteractive/seedv6/tools/search/ColorReversal.java` (new tool-only transform).
- `app/src/main/java/com/ohinteractive/seedv6/tools/search/ColorSymmetryDiagnostics.java` (new measurements/reporting).
- `app/src/main/java/com/ohinteractive/seedv6/tools/search/Brn2Diagnostics.java` (options/integration/model-integrity check).
- `app/src/main/java/com/ohinteractive/seedv6/search/diagnostics/QsearchDecisionTrace.java` (optional bounded detached snapshots).
- `app/src/test/java/com/ohinteractive/seedv6/tools/search/ColorSymmetryDiagnosticsTest.java` (focused tests).
- `BRN_COLOR_SYMMETRY.md` (this evidence and reproduction report).
- `BRN_DIAGNOSTICS.md` (entry link).

No repository-local AGENTS.md was found in the repository or inspected ancestor
paths. Supplied governance and the repository Search contract were inspected.
Root `CODEXLOG_CURRENT.md` and `VERSION_STATE.txt` were independently absent;
neither capability was activated or file created. The only inherited untracked
content was `app/bin/`, which was preserved and excluded. Generated build outputs
were excluded. The intended source/tests/documentation form one local commit;
the final commit identity and Git status are reported in the completion response.
No push, deployment, training or supervision change occurred.

Remaining limits: this is one checkpoint, 15 synthetic roots and a deterministic
path-weighted Kiwipete sample. Side and ply groups have different positions;
small move categories and late qplies have few observations. Equal fullmove
labels define the snapshot contract described above. The data isolates combined
absolute-color/orientation/STM dependence, not a specific learned weight or
feature family. It strongly supports a material contribution to tactical
instability and local continuation decisions, but does not uniquely explain the
alternating cutoff pattern or quantify search-node causality.

Human actions required after this prompt: None.
