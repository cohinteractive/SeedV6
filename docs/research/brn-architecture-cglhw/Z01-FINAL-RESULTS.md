# Final confirmation results

2026-10-07. All frozen allocations and their audits completed. This is research evidence, not production adoption or user acceptance.

## Fixed-time complete-engine strength

All 768 batches / 3,072 games completed with scored outcomes: no caps, missing games or failures. Maximum observed game length was 654 plies. The independent 128-opening sample was used for both source/order runs and both colours, giving 512 games per comparison. Search used the unchanged production exact/PVS static-leaf path, one worker, private 4 MiB TT and 100 ms per move. No interim outcomes were inspected; selection remained frozen.

| Opponent | W / D / L | Score | Run A / B | Two-way bootstrap 95% | Opening-only 95% | Simultaneous six 95% lower |
|---|---:|---:|---:|---:|---:|---:|
| Fresh BRN | 384 / 53 / 75 | 80.18% | 78.32% / 82.03% | [75.39%, 84.57%] | [77.34%, 82.91%] | 66.50% |
| Current NNUE v2 | 349 / 57 / 106 | 73.73% | 73.83% / 73.63% | [69.53%, 77.73%] | [70.31%, 77.15%] | 60.06% |
| Strict NNUE | 351 / 65 / 96 | 74.90% | 73.24% / 76.56% | [70.12%, 79.49%] | [71.68%, 78.12%] | 61.23% |
| Cheap material | 277 / 104 / 131 | 64.26% | 67.77% / 60.74% | [57.23%, 70.70%] | [60.55%, 67.97%] | 50.58% |
| Captured native BRN | 241 / 87 / 184 | 55.57% | 55.66% / 55.47% | [50.98%, 60.16%] | [51.86%, 59.28%] | 41.89% |
| Compiled own Gen0 | 335 / 87 / 90 | 73.93% | 77.73% / 70.12% | [66.99%, 80.47%] | [70.70%, 77.15%] | 60.25% |

All six comparisons pass the frozen primary bootstrap strength criterion (lower > 50%). Those intervals are approximate and unadjusted, with only two source/order runs. The conservative simultaneous bounded-pair sensitivity check clears 50% for five controls, including cheap material (50.58%), but not captured native BRN (41.89%). Its single-comparison bound is 44.75%, also just below the 45% noninferiority threshold. Thus the native result is promising under the primary analysis, not robustly established under the conservative sensitivity analysis. Do not claim an unconditional all-controls win. Bounded checks condition on the fixed models and independent generated opening clusters; neither they nor two source intervals establish corpus-game independence.

All-planned adverse score bounds equal the point estimates because every game was scored. No extension or outcome-dependent rerun was added.

## Actual search and process resources

| Opponent | Candidate / opponent NPS (millions) | Candidate / opponent ms per played move |
|---|---:|---:|
| Fresh BRN | 2.500 / 0.385 | 95.797 / 96.367 |
| Current NNUE v2 | 2.496 / 0.780 | 95.957 / 96.538 |
| Strict NNUE | 2.509 / 0.779 | 96.111 / 96.647 |
| Cheap material | 2.533 / 4.069 | 97.107 / 97.109 |
| Captured native BRN | 2.534 / 0.405 | 96.629 / 97.084 |
| Compiled own Gen0 | 2.520 / 2.592 | 96.811 / 96.832 |

Java-process time was 47358.968290 seconds (13.1553 hours), excluding outer-controller overhead. Observed whole-JVM peak working set ranged 173,084,672–253,751,296 bytes; private commit 390,410,240–413,192,192 bytes. All post-exit counter reads succeeded and no memory read failed. Per-run/actor mean move times ranged 95.777–97.278 ms; early search completion is permitted. Per-game timing distributions remain in the resource JSON and are not per-move percentiles. Different evaluators visit different trees, so these are observed engine throughputs, not identical-node microbenchmarks. Process memory includes JVM/data/checkpoint allocations and is not isolated model memory.

## Sealed prediction and calibration

All 22 profiles evaluated the full 32,768-position held-out population on each range: 720,896 visits, 65,536 distinct positions, 37.345619 process seconds. All frozen source/class/runner, command, Java/platform, model/metadata, finite-metric, population, strata and reliability checks passed. Original versus compiled pair metrics and reliability differed by exactly 0 on A and 0 on B in this sample. No models or gains were selected from these results.

| Profile | Raw half-MSE | Balanced half-MSE | Search half-MSE | Raw / search bin gap | CP MAE (pawns) | Residual RMS (pawns) | Sign mistakes / population |
|---|---:|---:|---:|---:|---:|---:|---:|
| brn-211 | 0.073677 | 0.060789 | 0.200877 | 0.035671 / 0.137222 | 2.17876 | 1.97860 | 2062 / 18016 |
| nnue-v2-211 | 0.105277 | 0.081809 | 0.205161 | 0.048815 / 0.132320 | 2.27192 | 1.85522 | 3097 / 18016 |
| nnue-strict-211 | 0.103720 | 0.085663 | 0.202589 | 0.053162 / 0.130176 | 2.23713 | 1.85857 | 2904 / 18016 |
| linear-211 | 0.076695 | 0.064351 | 0.200560 | 0.037050 / 0.133663 | 2.21391 | 2.02514 | 2198 / 18016 |
| tuple2-211 | 0.128970 | 0.071580 | 0.197236 | 0.038793 / 0.103432 | 2.49944 | 1.95661 | 4006 / 18016 |
| tuple3-211 | 0.118828 | 0.078889 | 0.191730 | 0.044358 / 0.101794 | 2.43546 | 1.91338 | 3548 / 18016 |
| edge-211 | 0.072325 | 0.063266 | 0.173844 | 0.039779 / 0.101694 | 2.16937 | 2.01124 | 1966 / 18016 |
| compiled-pair-211 | 0.128970 | 0.071580 | 0.197236 | 0.038793 / 0.103432 | 2.49944 | 1.95661 | 4006 / 18016 |
| material-211 | 0.229691 | 0.148700 | 0.229691 | 0.131836 / 0.131836 | 2.72498 | 0.00000 | 7371 / 18016 |
| native-211 | 0.089125 | 0.058603 | 0.146476 | 0.035763 / 0.073426 | 2.30528 | 2.02555 | 2285 / 18016 |
| compiled-gen0-211 | 0.229691 | 0.148700 | 0.229691 | 0.131836 / 0.131836 | 2.72498 | 0.00000 | 7370 / 18016 |
| brn-337 | 0.069346 | 0.063413 | 0.191962 | 0.036901 / 0.129500 | 2.50149 | 2.16904 | 1679 / 19207 |
| nnue-v2-337 | 0.112585 | 0.102172 | 0.195249 | 0.077644 / 0.123279 | 2.65848 | 1.92383 | 2884 / 19207 |
| nnue-strict-337 | 0.099582 | 0.089695 | 0.193535 | 0.052784 / 0.123981 | 2.59306 | 1.92246 | 2560 / 19207 |
| linear-337 | 0.078292 | 0.076090 | 0.190777 | 0.047010 / 0.125146 | 2.54707 | 2.26033 | 1886 / 19207 |
| tuple2-337 | 0.125921 | 0.074500 | 0.189956 | 0.043020 / 0.101995 | 2.92614 | 1.93807 | 3527 / 19207 |
| tuple3-337 | 0.113199 | 0.082868 | 0.180562 | 0.044837 / 0.097195 | 2.81928 | 1.93419 | 2934 / 19207 |
| edge-337 | 0.070848 | 0.065992 | 0.165389 | 0.035972 / 0.095915 | 2.56459 | 1.95422 | 1895 / 19207 |
| compiled-pair-337 | 0.125921 | 0.074500 | 0.189956 | 0.043020 / 0.101995 | 2.92614 | 1.93807 | 3527 / 19207 |
| material-337 | 0.222934 | 0.155002 | 0.222934 | 0.127271 / 0.127271 | 3.11655 | 0.00000 | 6125 / 19207 |
| native-337 | 0.095873 | 0.063159 | 0.144284 | 0.038211 / 0.072492 | 2.76956 | 2.13301 | 2110 / 19207 |
| compiled-gen0-337 | 0.222934 | 0.155002 | 0.222934 | 0.127271 / 0.127271 | 3.11655 | 0.00000 | 6119 / 19207 |

All profiles reported zero saturated search scores. Balanced results use the existing |teacher CP| <= 200 subset. Full material and occupied-piece-count strata and ten-bin reliability tables remain in the quality JSON. Piece count is a phase proxy, not known game phase; reliability compares teacher-derived expected score, not empirical win probability. Native historical exposure is unknown. Sealed raw loss is lowest for edge functions on A and BRN on B, rather than a universal edge advantage. Compiled pairs have worse raw loss than BRN/current NNUE but stronger fixed-time play, confirming that teacher loss alone cannot select the practical architecture.

Sealed-profile whole-JVM peak working set: 250,298,368–269,787,136 bytes; private commit: 374,775,808–380,198,912 bytes. Zero counter-read failures. No isolated training-memory inference follows.

## Evidence identities and reproduction

Plans and raw outputs are immutable. Reproduce into new output paths; do not overwrite completed runs. The match controller, analyzer, resource auditor and independent cross-check are recorded in STATE and the plans. The sealed controller is bound by the match plan and its separate execution plan.

| Artifact | SHA-256 |
|---|---|
| [z01-final-match-plan.json](z01-final-match-plan.json) | `e90f0f04fb2bf33019946f93e8c85391f1d02ab5626e60d36347f76ef6858529` |
| [z01-final-match-results.json](z01-final-match-results.json) | `a3e6d585f7416ef7d77d88893f237f7e32fb62110dd17f1575d6866a4e743a31` |
| [z01-final-match-resources.json](z01-final-match-resources.json) | `39a0b3f6f219881bc675d4ea2f0161f740af5764a16b1811d560ce88976daafe` |
| [z01-final-match-crosscheck.json](z01-final-match-crosscheck.json) | `affc50e2350fd6d6a02f67d8ce6271c6f8d77f3a54b7b3121781756612ef66d8` |
| [z01-final-quality-plan.json](z01-final-quality-plan.json) | `296e6b6ffbc5ed3a022952e854493cb0bf278dac8d4e018635673bf5429182cd` |
| [z01-final-quality-execution-plan.json](z01-final-quality-execution-plan.json) | `6c9c47c53d32cfb3a13da3bab584201c643e3bd3e73aeedc17a0ed36d9ce15cc` |
| [z01-final-quality-results.json](z01-final-quality-results.json) | `80baff741b32392ad6ccb3983a5c8e229fb52d97307755b5b134303946aa73aa` |
| [z01-final-runtime-results.json](z01-final-runtime-results.json) | `f0cc0332753c8020c9329a82de3905e5e2891d82ffc3c41713449a711350c000` |
| [z01-final-runtime-crosscheck.json](z01-final-runtime-crosscheck.json) | `57c30a855d506b4ace3760a36a768a3d5a68399af6cdfd05cbfff996963268a4` |

The source/class/JVM/Java/platform manifests match the earlier scaling campaign. The first rejected opening sample and the fully replaced, accepted geometry-only sample remain in Z01-FINAL-OPENINGS-02 and its receipts. All earlier positive and negative evidence remains in the family files and Z01. No production source, default evaluator, native model store, release or deployment was changed.
