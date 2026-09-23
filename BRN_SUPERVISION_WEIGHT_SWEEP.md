# BRN-2 supervision-weight sweep: 25% and 75% teacher

Measured 2026-09-23 UTC in `C:\projects\seed\java\seedv6`, starting from
`d475ca2c62c9b953fae17194fdf9b8863b81cb5f`. This is a separate extension of
[the accepted three-arm ablation](BRN_SUPERVISION_ABLATION.md), following
[the canonical g128 diagnostics](BRN_CANONICAL_G128_DIAGNOSTICS.md).
Only teacher weights **0.25 and 0.75** were trained and newly measured.
The accepted 0.00, 0.50 and 1.00 measurements are reused at their saved precision.

**The curve is not a smooth compromise.** Teacher loss and final NNUE correlation
improve monotonically across the five sampled weights, while local stability and
bounded search do not. The 25% arm has the lowest measured WDL loss, but still
caps Kiwipete at one million nodes and raises total corpus cost. The 75% arm
substantially improves 50/50's relative local stability, while preserving more
WDL prediction than teacher-only; its total search cost exceeds both. These are
single-campaign diagnostic trade-offs, not evidence of playing strength or a
production-training decision.

## Exact source, target and replay controls

The canonical source remains `E:\SeedV6-Networks\BRN\BRN-2\training001`,
architecture `seedv6.brn.2`, feature schema 2, width 32. Both independent arms
decode the same surviving g0 model and complete optimizer state:

```text
checkpoints/g000000-s000000000-31a42b7e131558c59e7868e45b759f1e5a322eb2bef5ff26e7b9386e4f831507
network.brn2 SHA-256: 195d4300ce1b90a30cb888d6990a872f33165bfadd66cf1f0212ba3f0c2f653f
training.state SHA-256: dec6eccdf4d9e59d4ea9f9843831548a045e07d050032f3c4ab62767d8c595bc
generation 0; optimizer step 0
```

The exact pinned teacher is:

```text
E:\SeedV6-Networks\NNUE\training\checkpoints\g000074-s000008942-2e22ccbfdeb18f5f26f91f0df183411ac1d01844ccd43e56e9005ee3f5837bc6
network.nnue SHA-256: 3279ff72654f56c8f5ce3cc89cc55d1f253de6ae9df00bd61a41293f57dd16a9
training.state SHA-256: 453b7259d4e8041fa4abb8cc0887540434871d2b151c33676f36e749e0c6cb34
generation 74; optimizer step 8942
```

For teacher weight `w`, the target is exactly
`(1 - w) * terminalWdlTarget + w * nnueNormalizedTeacherTarget`.
Thus 0.25 uses `0.75 * WDL + 0.25 * teacher`, and 0.75 uses
`0.25 * WDL + 0.75 * teacher`. Loss is `0.5 * (prediction - target)^2`.
The teacher is `NnueEvaluator.evaluate(sample.board())` followed by its native
`boundedValue()` (`StrictMath.tanh(raw)`), on the exact sampled board and its side
to move. There is no search target, integer score, centipawn mapping or calibration.

`Brn2SupervisionAblation.Arm` now carries an explicit finite weight in [0, 1].
Its endpoint branches return the original WDL or teacher directly, retaining
exact definitions including signed zero. At 0.50 the arithmetic remains exactly
`0.5 * WDL + 0.5 * teacher`. The existing production callback, frozen targets,
gradient, optimizer, shuffle and held-out comparison are unchanged. The explicit
weight option is confined to offline experiment tooling; no GUI selector or
canonical default changed.

Both arms consume the original checksummed
`bootstrap/<parent-checkpoint-id>.plan` / `.data` pairs, generations 1–128 in
order. The production readers preserve saved training/held-out membership and
order. Every generation's source Candidate, plan hash, data hash, saved shuffle
seed, sample counts and optimizer step is checked against the accepted replay.
The new external `replay.jsonl` preserves all 256 records at full precision.
There are **208,104 training samples** and **52,974 held-out samples** per arm;
latest-training ends at g128, step 208,104.

Configuration remains one shuffled online pass, minibatch 1, Adam learning rate
0.001, beta1 0.9, beta2 0.999, epsilon 1e-8. Each generation reloads the immediately
preceding Candidate's serialized training state, including after a retention.
Candidate and incumbent Best use that arm's weighted target on that generation's
original held-out partition. Strictly lower Candidate loss promotes; a tie retains.
Neither cross-objective losses nor fixed-corpus diagnostics enter promotion.
No Candidate-versus-Best games or new self-play run.

The accepted exact WDL gate is **reused, not rerun**. Its immutable replay file
SHA-256 is `20ebb6aaf994d774301995a985ee832763c139ddd145c0769b925b8ee7fe9f4c`.
The wrapper verifies this evidence, all 15 accepted milestone payload pairs and
three accepted latest-training pairs before training. Current canonical/teacher
inventories must equal the previous experiment's saved after-inventories.
The Java explicit-weight mode also requires the exact accepted replay hash.
Canonical plan/data/history/validation relationships and teacher pin are checked
again by the existing harness. No approximate reproduction waiver was used.

Baseline measurements come directly from the accepted `analysis.json`, SHA-256
`b45d8f2a432458265bbabac8dd5b17a2437e5c4db95d47def88b62fb386db5de`;
the existing canonical artifact hashes are also rechecked. Their measurements
are not recomputed under different definitions or inferred from rounded prose.

## Experimental identities and promotions

The verified previous root has `wdl`, `blended`, `teacher`, and `replay.jsonl`.
To preserve it byte-for-byte, the sweep uses a new sibling under the same convention:

```text
E:\SeedV6-Networks\BRN\BRN-2\experimental-supervision-ablation-weight-sweep-20260923\
  replay.jsonl
  weight0p25\boundary-{0,32,64,96,128}\{network.brn2,training.state,experiment.json}
  weight0p75\boundary-{0,32,64,96,128}\{network.brn2,training.state,experiment.json}
  <arm>\latest-training-g128\{network.brn2,training.state}
  <arm>\best.txt
```

Final Best is each arm's `boundary-128/network.brn2`, not necessarily its g128
Candidate. These exports have no production manifests, promotion records or
`refs/best`. The harness rejects existing output paths and has no interrupted-run
resume facility. Intermediate non-milestone payloads are not retained.

| Teacher weight | Best generation | Promotions / retentions | Model SHA-256 | Training SHA-256 |
|---|---|---|---|---|
| 0.25 | 127 | 61 / 67 | `8eb98b21711da4d7b645af0e79c7560e5aac7a6d68557373d0d2bb57abd61b32` | `3ebba2f47050242fea3185468de36c43bfbf515b2cd6dce8ef105841a801f6a5` |
| 0.75 | 125 | 55 / 73 | `6124771b5f83e86964aad8fa6f441dde378aca58b0aabe8541bbd9602b456e37` | `557863160233cfbeec56123dc8a5b14382b7830965039667f6c8055122a1fb74` |

The following blocks use each arm's own target. Losses are means across the
32 generation-specific post-training measurements, not one shared holdout.

| Weight | Generations | Training loss | Candidate held-out | Incumbent held-out | Promotions |
|---|---|---|---|---|---|
| 0.25 | 1–32 | 0.0456727621 | 0.13150725 | 0.141410519 | 18 |
| 0.25 | 33–64 | 0.0471492873 | 0.111846358 | 0.110843273 | 15 |
| 0.25 | 65–96 | 0.0478296784 | 0.100247657 | 0.0994739018 | 14 |
| 0.25 | 97–128 | 0.0496134115 | 0.0998587998 | 0.0970202491 | 14 |
| 0.75 | 1–32 | 0.013269948 | 0.0345503541 | 0.0410545547 | 15 |
| 0.75 | 33–64 | 0.0106000175 | 0.023949969 | 0.0237138597 | 16 |
| 0.75 | 65–96 | 0.0105784544 | 0.021878043 | 0.0219884591 | 15 |
| 0.75 | 97–128 | 0.0102674688 | 0.02188213 | 0.0208868149 | 9 |

## Milestone held-out losses

Each row measures the **published Best existing at the completed boundary**.
All four losses use the same concatenation of 52,974 original held-out samples,
with equal weight per sampled position. This is retrospective: even the g0 row
uses all future batches, and the final population includes Best-selection data.
Whole-game exclusion holds within a batch, but there is no global cross-generation
deduplication and an early held-out position can occur in later training. These
are descriptive common-population losses, not independent generalization tests.
Own-objective losses across weights are not directly comparable difficulty scores.

| Weight | Boundary | Best | Own loss | WDL loss | 50/50 loss | Teacher loss |
|---|---|---|---|---|---|---|
| 0.25 | 0 | 0 | 0.270780588 | 0.34296704 | 0.22709548 | 0.225229298 |
| 0.25 | 32 | 28 | 0.108696882 | 0.178666156 | 0.0672289531 | 0.0697971296 |
| 0.25 | 64 | 64 | 0.102321563 | 0.170239252 | 0.0629052194 | 0.0695765656 |
| 0.25 | 96 | 94 | 0.101355883 | 0.169411167 | 0.0618019446 | 0.0681981012 |
| 0.25 | 128 | 127 | 0.0994681133 | 0.166961724 | 0.0604758475 | 0.0679953502 |
| 0.75 | 0 | 0 | 0.211911717 | 0.34296704 | 0.22709548 | 0.225229298 |
| 0.75 | 32 | 27 | 0.0255668171 | 0.190661255 | 0.0520969516 | 0.0275380272 |
| 0.75 | 64 | 61 | 0.0231011683 | 0.18770937 | 0.0494692243 | 0.025234457 |
| 0.75 | 96 | 95 | 0.0237936886 | 0.192021441 | 0.0513682615 | 0.0247204604 |
| 0.75 | 128 | 125 | 0.0210319612 | 0.185464462 | 0.0473414501 | 0.0232238169 |

## Fixed-corpus milestone measurements

The unchanged `seedv6-brn2-diagnostics-v1` corpus has 15 roots, 233 legal children,
248 pooled outputs, 210 quiet edges and 23 tactical edges. SHA-256:
`0503b850d5ed49686e72e601b1216270cfd40fc3bac18fac4dae697b323f3bd8`.
Raw SD is population SD of the actual pre-tanh output; normalized SD uses tanh.
Edge delta is `abs(-f(child) - f(parent))`, using normalized values in a common
parent perspective. Quiet excludes captures, promotions and delivered checks;
tactical is their union. QΔ/TΔ are category medians; Q/IQR and T/IQR divide by
that model's pooled normalized output IQR, using `(n-1)*p` interpolated quantiles.
Correlations use normalized outputs in actual side-to-move perspective;
Spearman uses average tie ranks; sign agreement treats zero as a separate sign.

| Weight | Boundary | Best | Raw SD | Normalized SD | QΔ | Q/IQR | TΔ | T/IQR | Pearson | Spearman | Sign agreement |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 0.25 | 0 | 0 | 0.149429818 | 0.144224045 | 0.106314912 | 0.391445991 | 0.0417948708 | 0.153886546 | -0.363805297 | -0.320528881 | 0.387096774 |
| 0.25 | 32 | 28 | 0.285383619 | 0.256213178 | 0.148632871 | 0.462489202 | 0.191785822 | 0.596764841 | -0.279487018 | -0.221247709 | 0.471774194 |
| 0.25 | 64 | 64 | 0.312497453 | 0.283778468 | 0.336316783 | 0.691131897 | 0.268534968 | 0.551840085 | 0.580183788 | 0.540474528 | 0.661290323 |
| 0.25 | 96 | 94 | 0.397579839 | 0.33639329 | 0.186555266 | 0.49811154 | 0.141779411 | 0.378557853 | 0.747429094 | 0.668912034 | 0.77016129 |
| 0.25 | 128 | 127 | 0.271587596 | 0.249541275 | 0.154348629 | 0.619314248 | 0.137629545 | 0.552229967 | 0.208878399 | 0.211128216 | 0.653225806 |
| 0.75 | 0 | 0 | 0.149429818 | 0.144224045 | 0.106314912 | 0.391445991 | 0.0417948708 | 0.153886546 | -0.363805297 | -0.320528881 | 0.387096774 |
| 0.75 | 32 | 27 | 0.28762288 | 0.27101183 | 0.0899670001 | 0.225215796 | 0.158562564 | 0.396932142 | 0.78885636 | 0.769709261 | 0.778225806 |
| 0.75 | 64 | 61 | 0.264354152 | 0.247585181 | 0.154209657 | 0.467973547 | 0.227921997 | 0.691665277 | 0.698184831 | 0.686636192 | 0.766129032 |
| 0.75 | 96 | 95 | 0.321317863 | 0.293563761 | 0.139860329 | 0.324505814 | 0.310924539 | 0.721411289 | 0.798521271 | 0.796840909 | 0.798387097 |
| 0.75 | 128 | 125 | 0.333614641 | 0.308482891 | 0.134513301 | 0.292391424 | 0.168879751 | 0.367093742 | 0.81752451 | 0.779397618 | 0.806451613 |

Both g0 rows exactly match the accepted static control. All ten new milestone
runs retain exact zero BRN color-reversal residuals. Baseline trajectories remain
in the accepted ablation report; their saved final metrics enter the table below.

## Five-weight final comparison

Losses use the common held-out population; static measurements use the fixed
diagnostic corpus. Search counts cover one full 15-position corpus. WDL and 25%
Kiwipete counts are capped prefixes through completed depth 3; their completed
depth-4 costs are unknown. No composite score is constructed.

| Weight | Best | WDL loss | 50/50 loss | Teacher loss | Pearson | Q/IQR | T/IQR | Qnodes | Total nodes | Kiwipete nodes / depth |
|---|---|---|---|---|---|---|---|---|---|---|
| 0.00 | 121 | 0.16961258 | 0.0754041479 | 0.0952010949 | -0.0981314403 | 0.720925084 | 0.374914027 | 1037239 | 1078524 | 1000000 / 3 |
| 0.25 | 127 | 0.166961724 | 0.0604758475 | 0.0679953502 | 0.208878399 | 0.619314248 | 0.552229967 | 1180461 | 1228358 | 1000000 / 3 |
| 0.50 | 125 | 0.168870628 | 0.0506825086 | 0.0464997683 | 0.645581811 | 0.762525716 | 0.967438587 | 90719 | 135194 | 84761 / 4 |
| 0.75 | 125 | 0.185464462 | 0.0473414501 | 0.0232238169 | 0.81752451 | 0.292391424 | 0.367093742 | 158077 | 196528 | 91905 / 4 |
| 1.00 | 127 | 0.219815373 | 0.0611598749 | 0.0165097553 | 0.867827769 | 0.224961055 | 0.157834529 | 129918 | 162889 | 93080 / 4 |

| Weight | Raw SD | Normalized SD | QΔ | Q/IQR | TΔ | T/IQR | Pearson | Spearman | Sign agreement |
|---|---|---|---|---|---|---|---|---|---|
| 0.00 | 0.342218767 | 0.302590821 | 0.299234632 | 0.720925084 | 0.155615699 | 0.374914027 | -0.0981314403 | -0.0116056517 | 0.560483871 |
| 0.25 | 0.271587596 | 0.249541275 | 0.154348629 | 0.619314248 | 0.137629545 | 0.552229967 | 0.208878399 | 0.211128216 | 0.653225806 |
| 0.50 | 0.258570634 | 0.241056018 | 0.244353326 | 0.762525716 | 0.310018183 | 0.967438587 | 0.645581811 | 0.634892837 | 0.725806452 |
| 0.75 | 0.333614641 | 0.308482891 | 0.134513301 | 0.292391424 | 0.168879751 | 0.367093742 | 0.81752451 | 0.779397618 | 0.806451613 |
| 1.00 | 0.514723261 | 0.425149783 | 0.121371518 | 0.224961055 | 0.0851552562 | 0.157834529 | 0.867827769 | 0.838724004 | 0.834677419 |

## Final bounded search

The established normal diagnostic requests depth 4, one thread, a private cold
262,144-entry TT per search, mate-distance-only selectivity, full windows,
singleton root history, diagnostics enabled and qshadow disabled. Each
position/arm runs in its own JVM with one warmup and two measured searches.
Each search has the exact 1,000,000-node bound and cooperative 10,000-ms bound;
the three-search process has a 40-second hard subprocess watchdog.

All repeated measured non-timing fields match: total/main/qnodes, completed depth,
status, score, move, PV and evaluation calls. Main + q equals total throughout.
Aggregates below are one corpus (either repetition); latency sum is the sum of
position medians, and worst latency is the largest individual measured latency.
Terminal roots report depth 1 and zero nodes; their node ratios are undefined.

| Weight | Main | Qnodes | Total | Q/total | Q/main | Sum median ms | Worst ms | Completed / terminal / limited |
|---|---|---|---|---|---|---|---|---|
| 0.00 | 41285 | 1037239 | 1078524 | 0.961720833 | 25.1238707 | 7541.7084 | 6953.7868 | 12 / 2 / 1 |
| 0.25 | 47897 | 1180461 | 1228358 | 0.961007296 | 24.6458233 | 7573.0068 | 5795.5324 | 12 / 2 / 1 |
| 0.50 | 44475 | 90719 | 135194 | 0.6710283 | 2.03977515 | 1241.6771 | 826.7195 | 13 / 2 / 0 |
| 0.75 | 38451 | 158077 | 196528 | 0.80434849 | 4.11112845 | 1524.96365 | 674.1993 | 13 / 2 / 0 |
| 1.00 | 32971 | 129918 | 162889 | 0.797586086 | 3.94037184 | 1227.3773 | 674.0013 | 13 / 2 / 0 |
| NNUE reference | 34894 | 48723 | 83617 | 0.582692515 | 1.39631455 | 253.78815 | 85.3744 | 13 / 2 / 0 |

Across the 60 new measured normal searches and 30 warmups:

- **0.25:** 24 measured completions at depth 4, four terminal adjudications,
  and two Kiwipete node-limit events at completed depth 3. Its warmup adds one
  Kiwipete node-limit event. The other 12 nonterminal positions complete.
- **0.75:** all 26 measured nonterminal searches complete depth 4, with four
  terminal adjudications; no measured or warmup limit events.
- Both arms: **zero engine timeouts, hard-watchdog expirations, stalls or failures**.
  Kiwipete is the worst-latency position for both.

All 43 subprocesses (one replay, ten static, 30 normal search, two qshadow)
exit zero. Replay completed in 175.392 seconds under its 1,200-second watchdog.
The additional 0.25 qshadow invocation also reaches the node bound; 0.75 does not.
These are ordinary engine bound events, not process timeouts.

Host remains Windows 11 / AMD Ryzen 5 5500, Java `21+35-2513`. New processes use
`-Xms256m -Xmx1536m`, matching the accepted blended/teacher runs; reused canonical
WDL/NNUE used a 1,024-MiB heap cap. Runs occurred at different times. Timing is
search-only, excluding JVM startup/model loading/JSON output, and is descriptive;
small differences are not speed rankings. No inference kernel or search code changed.

### Kiwipete sentinel

| Weight | Main | Qnodes | Total | Q/total | Q/main | Median ms | Worst ms | Depth | Status |
|---|---|---|---|---|---|---|---|---|---|
| 0.00 | 7595 | 992405 | 1000000 | 0.992405 | 130.665569 | 6859.76025 | 6953.7868 | 3 | NODE_LIMIT |
| 0.25 | 9055 | 990945 | 1000000 | 0.990945 | 109.436223 | 5771.0312 | 5795.5324 | 3 | NODE_LIMIT |
| 0.50 | 13866 | 70895 | 84761 | 0.836410613 | 5.112866 | 773.73385 | 826.7195 | 4 | COMPLETED |
| 0.75 | 9662 | 82243 | 91905 | 0.894869702 | 8.5120058 | 667.21865 | 674.1993 | 4 | COMPLETED |
| 1.00 | 8645 | 84435 | 93080 | 0.907122905 | 9.76691729 | 670.94885 | 674.0013 | 4 | COMPLETED |
| NNUE reference | 9135 | 32300 | 41435 | 0.77953421 | 3.53585112 | 82.50555 | 85.3744 | 4 | COMPLETED |

### Kiwipete same-edge qshadow/tree volatility

Each new final Best drives one instrumented search while the exact NNUE g74
observes the same visited boards. The existing separate instrumentation bounds
are depth 4, 1,000,000 nodes, cooperative 30 seconds and hard subprocess 45 seconds;
there is no warmup, and symmetry sampling uses stride 101 / limit 8,192.
Instrumentation time is excluded from normal search timing. The accounting is
complete and the instrumented nodes/depth/status equal the corresponding normal
search. Values below are adjacent-static absolute deltas in the existing integer
search-score units, with both endpoints evaluated from the parent perspective,
matching the accepted qshadow definition. They are neither calibrated centipawns
nor native normalized fixed-corpus median/IQR ratios.

| Driving weight / tree | Evaluator | Edges | Mean | Median | p95 | Max |
|---|---|---|---|---|---|---|
| 0.00 | BRN | 659077 | 13264.0125 | 11516 | 31584.2 | 58541 |
| 0.00 | NNUE shadow | 659077 | 6768.64141 | 3869 | 22942 | 56495 |
| 0.25 | BRN | 634625 | 13485.9321 | 12006 | 30815 | 56761 |
| 0.25 | NNUE shadow | 634625 | 7242.17693 | 4370 | 23995.4 | 55303 |
| 0.50 | BRN | 51488 | 9759.01843 | 8578 | 22748 | 45838 |
| 0.50 | NNUE shadow | 51488 | 5961.684 | 3565 | 19979.65 | 50161 |
| 0.75 | BRN | 56465 | 8447.62892 | 6879 | 21635 | 48739 |
| 0.75 | NNUE shadow | 56465 | 7219.04258 | 4778 | 22540 | 54534 |
| 1.00 | BRN | 62927 | 10866.6361 | 9315 | 26424.7 | 49349 |
| 1.00 | NNUE shadow | 62927 | 7594.36151 | 5170 | 22714 | 52443 |

The new 0.25 pass is a one-million-node prefix, completed depth 3, `NODE_LIMIT`,
9,132.327 ms instrumented latency. The 0.75 pass completes depth 4 in 91,905 nodes,
`COMPLETED`, 1,080.531 ms. Neither hits its wall-clock or hard watchdog bound.
The BRN/NNUE same-edge median ratios at weights 0 / .25 / .50 / .75 / 1 are
**2.97648 / 2.74737 / 2.40617 / 1.43972 / 1.80174**. The 75% tree has the
closest measured paired median, but that observation is tree-specific.

Each BRN/NNUE comparison is paired within that arm's visited tree. Different
weights visit different trees, so rows do not represent one shared cross-arm
edge population. Shadow values do not influence search decisions. This does
not attribute counterfactual node savings to particular cutoffs.

## Evidence-based decisions and limitations

1. **25% versus WDL and 50/50.** Final WDL loss is 0.166961724: 1.56% below
   WDL and 1.13% below 50/50, the lowest observed among these five Bests. Teacher
   loss falls 28.58% versus WDL, but is 46.23% above 50/50. Pearson rises from
   -0.09813 to 0.20888, still well below 50/50's 0.64558. Quiet relative volatility
   is 14.10% below WDL; tactical relative volatility is 47.30% above WDL, despite
   its smaller absolute tactical median. Both ratios are better than 50/50's.
   Search remains problematic: Kiwipete is capped at depth 3 and aggregate
   nodes rise from 1,078,524 to 1,228,358. Quiet-fianchetto alone rises from
   49,024 WDL nodes to 201,111. A lower q/total fraction does not imply fewer qnodes.

2. **75% versus 50/50 and teacher-only.** Relative quiet/tactical volatility falls
   **61.66% / 62.06%** versus 50/50; teacher loss falls **50.06%** and Pearson
   rises to 0.81752. WDL loss costs **9.83%** versus 50/50, but remains **15.63%
   lower than teacher-only**. Teacher-only retains lower teacher loss (0.01651
   versus 0.02322), stronger Pearson/Spearman and lower quiet/tactical ratios.
   The 75% ratios are 29.97% / 132.58% above teacher-only. Kiwipete completes in
   91,905 nodes, between 50/50's 84,761 and teacher-only's 93,080; full-corpus
   cost is 196,528, above both 135,194 and 162,889. Quiet-fianchetto contributes
   76,231 nodes, versus 26,015 and 45,863 respectively.

3. **Monotonicity across final weights.** Teacher loss decreases strictly
   (0.09520 → 0.06800 → 0.04650 → 0.02322 → 0.01651). Pearson increases
   (-0.09813 → 0.20888 → 0.64558 → 0.81752 → 0.86783), as do Spearman and sign
   agreement. Quiet stability is **not monotonic**: Q/IQR is
   0.72093 → 0.61931 → 0.76253 → 0.29239 → 0.22496. Tactical stability is
   **not monotonic**: T/IQR is 0.37491 → 0.55223 → 0.96744 → 0.36709 → 0.15783.
   Search is also **not monotonic**: both corpus nodes and qnodes rise at .25,
   fall sharply at .50, rise at .75 and fall at 1.00. Kiwipete completion changes
   from capped depth 3 at 0/.25 to completed depth 4 at .50/.75/1; among completers,
   node counts rise with weight. These statements concern final Bests, not every
   generation or every position.

4. **WDL cost.** There is no observed loss penalty at .25 or .50 relative to WDL;
   their small improvements are descriptive, not significance claims. The cost
   appears by .75: 0.185464 versus 0.169613 WDL (+9.35%). Teacher-only reaches
   0.219815 (+29.60%). WDL loss bottoms at .25, then rises at each sampled weight.

5. **What explains the 50/50 volatility result?** It sits in a measured
   **non-monotonic interior region**, rather than a smooth interpolation between
   endpoint stability. Tactical relative volatility already worsens at .25 and
   peaks at .50, before improving at .75; quiet relative volatility also has an
   interior peak at .50, though .25 improves on WDL. Thus the adverse tactical
   behavior is not confined to exactly .50. The mechanism, unsampled interval
   widths and repeatability across campaigns remain unexplained. This single
   sweep cannot distinguish a general weight effect from campaign/selection
   interactions or identify a universal instability band.

6. **Distinct new trade-offs.** Yes. At .25, the lowest observed WDL loss coexists
   with unresolved bounded-search expansion. At .75, much better fixed-corpus
   stability and teacher agreement than .50 coexist with less WDL cost than 1.00,
   but more aggregate search work than either. Neither is an across-the-board
   replacement for an established arm.

7. **Broad region or sharp change?** All sampled weights from .50 through 1.00
   resolve the Kiwipete bound, suggesting a useful *sampled search-completion
   region*, with a marked observed change between .25 and .50. Stronger quiet
   relative stability appears by .75 and continues at 1.00; it does not require
   the teacher-only endpoint. Tactical stability shows its strongest improvement
   at 1.00. There is no single broad region improving every objective, and the
   coarse grid cannot establish an actual threshold or behavior between points.
   Milestones reinforce variability: .25 Pearson goes -0.279 → 0.580 → 0.747 →
   0.209; .75 quiet ratio goes 0.225 → 0.468 → 0.325 → 0.292, and tactical ratio
   0.397 → 0.692 → 0.721 → 0.367. No smooth training convergence is inferred.

8. **Non-dominated weights.** Using lower WDL/teacher losses, higher Pearson,
   Spearman and sign agreement, lower quiet/tactical median/IQR ratios, lower
   qnodes/q fractions, lower corpus nodes and better Kiwipete completion/cost,
   **all five weights are non-dominated on this joint observed vector**. This
   is an ordinal Pareto-like description, with no tolerance or composite score:
   .25 has minimum WDL loss; .50 has minimum corpus qnodes/total cost and minimum
   completed Kiwipete cost; .75 trades WDL accuracy against 1.00's stronger teacher
   fit/stability and costs fewer Kiwipete nodes; 1.00 has best teacher fit and
   fixed-corpus stability. WDL survives the joint comparison because the
   lower-WDL-loss blends (.25/.50) have worse tactical relative stability,
   while .75/1.00 have worse WDL loss. This does not endorse WDL operationally.
   On **WDL loss and teacher loss alone**, WDL is dominated by both .25 and .50;
   the other four form the observed predictive-loss frontier. Among the
   depth-4-completing weights, .50/.75/1.00 remain distinct trade-offs. Bound-capped
   costs are censored, and small loss differences have no replication-based
   uncertainty estimate; these are not population-optimality claims.

9. **Playing strength:** unmeasured; no inference from these diagnostics.
10. **Canonical policy:** unchanged; no production training-method adoption.
11. **BRN-3:** no recommendation. The measured supervision trade-offs do not
    establish a representational failure of BRN-2.

This experiment changes supervision and its corresponding Best-selection
objective together; it does not disentangle gradient effects from checkpoint
selection. There is one initialization, one saved campaign, one pass per batch,
and no independent replications or confidence intervals. Five sampled weights
cannot locate an exact transition weight or establish behavior between them.
The diagnostic corpus is synthetic and not game-frequency weighted; siblings
are correlated and tactical n=23 is small. Lower local deltas alone are not
greater chess accuracy. Agreement measures agreement with this teacher, not truth.

No actual playing strength is inferred. Canonical training methodology, architecture,
schema, evaluator inference, search-score mapping, search, NNUE implementation,
Candidate-versus-Best game validation, calibration and inference optimization
remain unchanged. No production adoption or BRN-3 recommendation follows from
the absence of a universal best weight. Generation 129 remains stopped.

## Full new-arm bounded-search records

Counts are per search; latency is the median of two measured searches.
All 15 positions, including Kiwipete and both terminal fixtures, are retained.

| Weight | Position | Main | Qnodes | Total | Q/total | Q/main | Median ms | Worst ms | Depth | Status |
|---|---|---|---|---|---|---|---|---|---|---|
| 0.25 | check-evasion | 28 | 2 | 30 | 0.0666666667 | 0.0714285714 | 1.7961 | 1.8565 | 4 | COMPLETED |
| 0.25 | checkmate-terminal | 0 | 0 | 0 | n/a | n/a | 0.1985 | 0.2009 | 1 | TERMINAL |
| 0.25 | en-passant | 399 | 47 | 446 | 0.105381166 | 0.117794486 | 6.2056 | 7.7237 | 4 | COMPLETED |
| 0.25 | middlegame-kiwipete | 9055 | 990945 | 1000000 | 0.990945 | 109.436223 | 5771.0312 | 5795.5324 | 3 | NODE_LIMIT |
| 0.25 | opening-ruy-lopez | 7475 | 7845 | 15320 | 0.512075718 | 1.04949833 | 137.21945 | 143.0656 | 4 | COMPLETED |
| 0.25 | opening-start | 4093 | 259 | 4352 | 0.0595128676 | 0.0632787686 | 44.96565 | 46.0047 | 4 | COMPLETED |
| 0.25 | promotion-race | 391 | 296 | 687 | 0.430858806 | 0.757033248 | 8.03925 | 9.6702 | 4 | COMPLETED |
| 0.25 | qsearch-exchanges | 1317 | 762 | 2079 | 0.366522367 | 0.578587699 | 11.8017 | 16.6474 | 4 | COMPLETED |
| 0.25 | queen-endgame | 1886 | 171 | 2057 | 0.083130773 | 0.0906680806 | 9.04725 | 10.2816 | 4 | COMPLETED |
| 0.25 | quiet-endgame | 329 | 43 | 372 | 0.115591398 | 0.130699088 | 7.46545 | 8.7876 | 4 | COMPLETED |
| 0.25 | quiet-fianchetto | 21093 | 180018 | 201111 | 0.895117622 | 8.53449012 | 1554.1402 | 1609.2076 | 4 | COMPLETED |
| 0.25 | quiet-pawn | 436 | 0 | 436 | 0 | 0 | 6.1153 | 8.3717 | 4 | COMPLETED |
| 0.25 | stalemate-terminal | 0 | 0 | 0 | n/a | n/a | 0.2161 | 0.2305 | 1 | TERMINAL |
| 0.25 | tactical-queen | 209 | 8 | 217 | 0.0368663594 | 0.038277512 | 5.02725 | 5.8887 | 4 | COMPLETED |
| 0.25 | transposition-knights | 1186 | 65 | 1251 | 0.0519584333 | 0.0548060708 | 9.7378 | 12.849 | 4 | COMPLETED |
| 0.75 | check-evasion | 28 | 2 | 30 | 0.0666666667 | 0.0714285714 | 1.77695 | 1.8703 | 4 | COMPLETED |
| 0.75 | checkmate-terminal | 0 | 0 | 0 | n/a | n/a | 0.2007 | 0.2008 | 1 | TERMINAL |
| 0.75 | en-passant | 361 | 5 | 366 | 0.0136612022 | 0.0138504155 | 6.75855 | 8.6573 | 4 | COMPLETED |
| 0.75 | middlegame-kiwipete | 9662 | 82243 | 91905 | 0.894869702 | 8.5120058 | 667.21865 | 674.1993 | 4 | COMPLETED |
| 0.75 | opening-ruy-lopez | 9467 | 7694 | 17161 | 0.448342171 | 0.812717862 | 146.8958 | 148.0918 | 4 | COMPLETED |
| 0.75 | opening-start | 4104 | 202 | 4306 | 0.0469112866 | 0.0492202729 | 52.5325 | 60.12 | 4 | COMPLETED |
| 0.75 | promotion-race | 394 | 378 | 772 | 0.489637306 | 0.959390863 | 8.4051 | 9.2709 | 4 | COMPLETED |
| 0.75 | qsearch-exchanges | 1141 | 646 | 1787 | 0.36149972 | 0.566170026 | 9.26845 | 11.3719 | 4 | COMPLETED |
| 0.75 | queen-endgame | 1885 | 171 | 2056 | 0.0831712062 | 0.0907161804 | 9.4922 | 10.6567 | 4 | COMPLETED |
| 0.75 | quiet-endgame | 217 | 26 | 243 | 0.106995885 | 0.119815668 | 7.0547 | 7.5928 | 4 | COMPLETED |
| 0.75 | quiet-fianchetto | 9531 | 66700 | 76231 | 0.874972124 | 6.99821635 | 597.00755 | 612.9096 | 4 | COMPLETED |
| 0.75 | quiet-pawn | 417 | 0 | 417 | 0 | 0 | 6.3284 | 8.1158 | 4 | COMPLETED |
| 0.75 | stalemate-terminal | 0 | 0 | 0 | n/a | n/a | 0.23665 | 0.2485 | 1 | TERMINAL |
| 0.75 | tactical-queen | 179 | 8 | 187 | 0.0427807487 | 0.0446927374 | 5.55115 | 5.9914 | 4 | COMPLETED |
| 0.75 | transposition-knights | 1065 | 2 | 1067 | 0.00187441425 | 0.00187793427 | 6.2363 | 8.4429 | 4 | COMPLETED |

## Reproduction, validation and provenance

Measurement artifacts follow the ignored build-output convention:
`app/build/brn2-supervision-weight-sweep-20260923/`. They contain ten static
JSONL files, 30 normal-search JSONL files, two qshadow JSONL files, process/replay
logs, exact commands and watchdog receipts, before/after fingerprints,
`analysis.json` and `tables.md`. Experimental checkpoints stay outside Git.
The new external replay SHA-256 is
`9c899d5e8d3b9e82e9b15fe9492dab20368b41452c1b4f9cd7259430257325fb`;
combined analysis SHA-256 is
`bdf0fbe753912ccbdde8b0a8fe1ba9e7f1cb046229b718fec6d965389ff8c4ea`.
The measurement directory also retains an `artifact-sha256.json` inventory.

From the repository root (use new output names for another run):

```powershell
.\gradlew.bat :app:test --tests '*Brn2SupervisionAblationTest' --tests '*Brn2TrainingTargetsTest' --tests '*HeldOutLossTest' --tests '*BootstrapPartitionTest' --tests '*Brn2DiagnosticsTest' --console=plain
python -B tools/brn2-supervision-ablation.py 'E:/SeedV6-Networks/BRN/BRN-2/training001' 'E:/SeedV6-Networks/NNUE/training/checkpoints/g000074-s000008942-2e22ccbfdeb18f5f26f91f0df183411ac1d01844ccd43e56e9005ee3f5837bc6' 'E:/SeedV6-Networks/BRN/BRN-2/experimental-supervision-ablation-weight-sweep-20260923' 'app/build/brn2-supervision-weight-sweep-20260923' --teacher-weights 0.25 0.75 --accepted-experiment 'E:/SeedV6-Networks/BRN/BRN-2/experimental-supervision-ablation-20260923' --accepted-measurements 'app/build/brn2-supervision-ablation-20260923'
python -B tools/analyze-brn2-supervision.py 'E:/SeedV6-Networks/BRN/BRN-2/experimental-supervision-ablation-weight-sweep-20260923' 'app/build/brn2-supervision-weight-sweep-20260923' 'app/build/brn2-canonical-g128' --accepted-analysis 'app/build/brn2-supervision-ablation-20260923/analysis.json'
git diff --check
```

The targeted command passed **34 tests in five suites, zero failures/errors/skips**.
The two added tests cover exact definitions at all five weights (including
endpoint signed zero), invalid/duplicate weight rejection, and two successive
serialized-training replays per weight against independently shuffled online
updates with exact optimizer-byte equality. They also verify weighted held-out
loss and promotion behavior. Existing coverage verifies target freezing,
native-teacher/side-to-move agreement, WDL defaults, saved settings, output
isolation, fail-closed control mismatch, partitions and diagnostics.

Runtime validation covers all 256 generation updates, original plan/data/seed
identity against accepted evidence, latest-training and milestone payload hashes,
promotion chains, sample/step totals, ten static corpora, 90 normal searches and
two qshadow passes. The analyzer passed exact g0 static reuse, corpus identity/
cardinality, zero symmetry residuals, final cross-loss identity, repeated search
fields, normal/qshadow node-depth-status equality, search settings and node
accounting checks. All 43 subprocess receipts passed their hard bounds. Python
syntax and whitespace checks passed. Only whitespace/comments in Java were
adjusted after compilation; experiment bytecode behavior was unchanged.
Additional read-only analysis checks confirm exact reuse of every old-arm final,
search, qshadow and aggregate record, unchanged NNUE static metrics, the quadratic
cross-objective identity for all ten own-loss measurements, and both stated
Pareto frontiers. Invalid/incomplete Python weight options reject before any
execution; Markdown table structure and report placeholders were checked.

Complete SHA-256, byte sizes, nanosecond modification times and inventories
match before/after for **948 canonical BRN files, 401 NNUE files, 55 accepted
experimental files and 109 accepted measurement files**. Canonical/NNUE stores
also match the previous accepted experiment's after-inventory. This covers
`refs/best`, `refs/latest-training`, g0/g74, all batches, and the stopped
generation-129 attempt/plan. **Canonical training001 and stopped generation 129
were neither mutated nor resumed.** No source store writer or recovery service
was opened. Accepted experimental outputs and measurements were not overwritten.

Applicable supplied/deployed governance, `source/CHESS_SEARCH_CONTRACT.md`,
`BRN_BOOTSTRAP.md` and the accepted diagnostic reports were inspected. This
experiment does not revise the Search canon. Exact root `CODEXLOG_CURRENT.md`
and `VERSION_STATE.txt` are independently absent; neither was created or activated.

Deliberately skipped: rerunning the three accepted full arms or their expensive
measurements (verified durable evidence reused); `fullCheck`, broad GUI/browser
or unrelated subsystem suites (no UI or engine change); new self-play,
Candidate-versus-Best games, playing-strength matches, long unrelated benchmarks,
calibration and inference optimization (outside the authorized experiment).
No required validation was skipped. The two new 128-generation arms are the
explicitly authorized experiment, not an expanded routine test gate.

Focused files changed:

- `BRN_SUPERVISION_WEIGHT_SWEEP.md` and its link in `BRN_DIAGNOSTICS.md`.
- `app/src/main/java/com/ohinteractive/seedv6/tools/search/Brn2SupervisionAblation.java`.
- `app/src/test/java/com/ohinteractive/seedv6/tools/search/Brn2SupervisionAblationTest.java`.
- `tools/brn2-supervision-ablation.py` and `tools/analyze-brn2-supervision.py`.

Starting Git state contained only inherited untracked `app/bin/`. The focused
commit contains only the six files listed above; generated checkpoints and large
measurement artifacts are excluded. Final worktree retains `app/bin/` as its
only unrelated untracked item; the completion response supplies the commit hash.
No push, deployment, reset, rebase, amend, discard or unrelated cleanup occurred.
This report records completed experimental work, not user acceptance or
production/operational adoption.

Human actions required after this prompt: **None**.
