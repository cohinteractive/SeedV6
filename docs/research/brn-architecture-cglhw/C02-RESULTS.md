# C02 implementation, parity and cost evidence

The immutable prospective hypothesis and allocation gate are in [C02](C02.md).
This result record does not authorize production adoption or extra training.

## Correctness and identity checks complete

Verification-only binary64 compilation combines the original order2 weights over
294 unique physical pairs, maintaining two STM sums and worker-local placement
updates. Original trainer, codec, weight files and evaluator remain available.
The compiled loader uses a separate TUPLE_COMPILED metadata identity. It requires
completed parity and matching source weight identity before loading. Metadata
identities are captured and rechecked by runtime, transition, match and quality
probes; future frozen plans bind them as well as weight bytes. Sealed quality
requires the metadata binding before reading dataset labels.

All28 focused Java tests passed, including seven new tests: three independent
compiled-table/transition checks, actual compilation/common-loader integration,
sealed metadata rejection and two bounded qsearch diagnostic tests. The latter
exercise an existing research search seam; production search is unchanged and no
real qsearch sample is allocated yet. Eleven Python tests passed, including three
new metadata/frozen-input rejection tests. Cumulative distinct unit tests:84 Java
and19 Python. No full-suite claim. Evidence: `c02-validation.json` and retained raw
XML/stdout/source bindings under `c02-validation-02` / `c02-auditor-tests-01`.

The first build attempt failed before compilation on sandbox Gradle network
access. An approved retry completed the offline build/tests in59.374s. The first
attempt's copied historical XML is explicitly not counted as new execution.
Draft review also normalized ownership bits on empty squares in the compiled
rebuild path; fresh-cache and rolling information-invariance checks pass. That
draft correction did not change the historical trainer or evaluator.

## Four actual-model parity runs complete

Plan `c02-parity-plan.json`, SHA256
`8bc6d5a221f4c9a7f6082abb87fabc28ded7aac8bc0e7c4559beed229d7a628e`.
Results: `c02-parity-results.json`. Both original exposure-selected pair models
(211/337) and their own Gen0 were checked on all32768 respective validation
positions at gains0/.125/.25/.5/1. Every run completed with maximum observed pawn
difference0 and zero integer differences at every gain. This is131072 board
visits /655360 board-gain comparisons, not that many independent positions.
Source model/metadata, actual data, source/compiled/runner hashes, exact commands,
unchanged copied weights and absence of an attached optimizer state all pass.
Full process36.9313753s; load compilation0.0104..0.0132s. Labels remain opaque.
Observed equality is not a universal floating-point equivalence proof. Every
different later selected model requires its own parity check before compiled use.

Compiled immutable primitive storage is799688 bytes, versus442108 bytes for the
original weight array; this is a storage trade-off, not extra learned capacity.
Compiled worker primitive arrays/scoring values occupy2920 bytes. Both figures
exclude JVM headers/reference arrays/counters and temporary source/compiler
allocations; process peak memory must be reported separately. Stored model bytes
remain the original442124-byte file.

## First cost allocation complete; one repeat active

`c02-cost-plan.json`, SHA256
`7b335cc9249a5cdc633d190329aac89f7a542d2cfbb97581056869ff51aeaf47`,
freezes20 processes:16 original/compiled native runtime/transition probes and four
same-JVM paired probes. The nested native plan is `c02-runtime-plan.json`, SHA256
`e082b77b489b12d4281463d9ea610028bb63d560a1622625cb74831dc04272b5`.
Use one CPU process,2GiB,300s ceiling,selected pair gain.25. Native probes use the
common128 validation roots/depth4 and legal/special child requests. Same-JVM
checks use64 warmup pass pairs and ten timed pairs with alternating first evaluator.
All models, evaluator metadata, data, code, commands and limits are frozen.

Both trained paired median ratios must be<=.85, all correctness/completion checks
must pass, and aggregate compiled trained depth4 search time must be lower. Gen0
costs are reported separately. The plan specifies objective repeat triggers for
method disagreement/pass variation and permits at most one identical allocation
repeat. A pass admits only a small playing comparison, not a strength conclusion.
All20 processes completed; both audits pass. `c02-runtime-results.json` covers16
native probes,1024 depth4 roots and the common legal/special transitions.
`c02-cost-results.json` binds the four paired probes and full execution evidence.
Total process228.0402056s. Every fixed-depth score, best move, depth and node count
matches between original and compiled evaluators, and all integer transition
comparisons are exact. The two trained paired compiled/original median ratios
are .461024/.350494; separate native child ratios .244850/.217446. Gen0 paired
ratios are .431929/.375000 and native ratios .399663/.358202.

Trained aggregate depth4 time falls from4.4865557s to3.4806561s (about22.4%).
The numerical cost thresholds pass, but the A211 trained methods differ by more
than the predeclared0.20 ratio margin. This triggers the permitted repeat; no
playing allocation is admitted from the first result alone. Timing methods agree
on direction but not the exact magnitude, which must remain visible.

The one identical20-process repeat is now frozen in `c02-repeat-cost-plan.json`,
SHA256 `4049ced57e461f3ca2bfa0c12df6675e566350c677abbe5751db50410eb2461f`.
Its16-run native subplan is `c02-repeat-runtime-plan.json`, SHA256
`7682ff7500b58cdeb95e9fa94836165b4374492ed698c437abd5194f937fff83`.
Same weights, metadata, data, code, ordering, gains, samples and limits; fresh
output paths only. The original result remains. Final allocation uses pooled
twenty-pass medians per model and both allocations' search times, requires all
parity/completion checks and consistent speedup direction in both methods on
each allocation. If still inconclusive, defer; no additional repeat is allowed.
See STATE for the live cursor. No playing outcome is available yet.

## Combined cost gate passes; playing screen active

The repeat completed all20 runs in229.939357s and passed both audits. It has no
repeat trigger. Trained paired ratios .451718/.426768, separate native ratios
.381729/.469618; aggregate depth4 seconds4.4880581 original vs3.3225893 compiled.
All fixed-depth scores, moves and node counts remain identical, and all transition
comparisons are exact. Results: `c02-repeat-runtime-results.json`,
`c02-repeat-cost-results.json` and `c02-combined-cost-results.json`.

|Profile|Pooled original child median, ns|Pooled compiled child median, ns|Compiled/original|
|---|---:|---:|---:|
|Trained211|450.340|208.981|.464051|
|Trained337|461.535|183.889|.398430|
|Gen0 211|421.262|186.748|.443305|
|Gen0 337|463.918|188.179|.405630|

Both allocations are included:40 processes,457.9795626s total;32 native probes
include2048 depth4 root visits. The trained combined search time is8.9746138s
original vs6.8032454s compiled, a24.20% reduction. Both trained pooled child ratios
clear .85 and both timing methods agree on speedup direction in each allocation.
The exact speedup magnitude remains method-dependent, as the first attempt shows.
The frozen combined rule therefore admits the small playing comparison; the one
repeat allowance is exhausted. No broader speed or playing-strength claim follows.
Observed OS peak working sets across the40 JVMs range249184256..422428672 bytes;
peak private commit372826112..601542656 bytes. These include JVM/data/compiler
allocation and are not model-only memory measurements.

The prospective192-game screen compares the compiled exposure-selected models
against their original implementation, compiled own Gen0 and cheap material at
gain.25 throughout:16 openings x2 colors x2 training runs x3 comparisons. All
outcomes are retained,50ms per move,24 four-pair batches,1024-ply unscored cap,
600s internal/900s external batch guards. This is an allocation screen, not final
confirmation. `c02-playing-plan.json` SHA256
`6d3363ac8440f369d9f11d0b727a10e66a0cecb6efe7bf5902faad5e802e2513`.

Opening seed840201,indices1100..1115 passed prior-opening and all-ten-dataset
geometry audits; labels stayed opaque. Inventory2117 source files/400 unique
FENs; base audit8.6742657s, dataset audit13.1588181s. Preflight plan SHA256
`460043622c8a2fd5e145b5bbbac023081b04e6e3d41580ccf2bc9b6105ca01fb`;
final augmented audit SHA256
`c761e1123e91acca50bb4c259203e55eabb3fb17a80ddbae1156471090acdfd7`.
Nested datasets share parent positions and do not add source independence.
Games are active; no complete playing result is available yet.


## C02 bounded playing screen COMPLETE

All24 batches/192 games completed, with no capped, failed or missing outcomes.
Maximum527plies; full process1490.5614817s. Score, resource and supplemental
code/model/metadata/command audits pass. Evidence: `c02-playing-results.json`,
`c02-playing-resources.json`, `c02-playing-crosscheck.json`. All actor group mean
search times are48.172..48.726ms per played move under the common50ms budget.
Observed JVM peak working sets209068032..269111296 bytes include full process
allocation, not just the evaluator. No sealed test labels were opened.

|Comparison|W / D / L|Score|Run211 /337|Approximate two-way95%|
|---|---:|---:|---:|---:|
|compiled-vs-original|41 / 7 / 16|69.53%|75.00% / 64.06%|[56.25, 82.81]%|
|compiled-vs-own-gen0|43 / 10 / 11|75.00%|65.62% / 84.38%|[57.81, 90.62]%|
|compiled-vs-material|42 / 10 / 12|73.44%|78.12% / 68.75%|[59.38, 85.94]%|

Each contrast contains64 games on16 common fresh openings and two training
source/order runs. All six run-level point scores exceed .5, and each approximate
two-way interval is above .5. Only two runs and16 opening clusters underpin those
intervals; this is a positive allocation screen, not independent final acceptance.
In direct implementation games, compiled search reaches2.291M NPS versus original
1.225M. Against compiled Gen0 it is2.205M versus2.263M; against cheap material,
2.267M versus3.527M. These are workload-dependent game aggregates, not the same
measurement as fixed-depth search time or per-child latency.

The direct69.53% original/compiled contrast supports practical value from the
equivalent-function speed improvement. The75% own-Gen0 result supports learned
strength on the same runtime topology. The73.44% cheap-prior score closes the
earlier screen's practical gap in this fresh sample. Do not attribute the full
cross-campaign change from39.84% to73.44% to compilation: openings and run timing
differ between campaigns. The same-opening direct implementation contrast is the
more controlled evidence for that mechanism.

Disposition: compiled pair tables are the principal practical candidate for the
remaining scaling/confirmation phase. Original pairs stay visible as an explicit
control; linear remains secondary, edge/triples retain their scaling questions,
and BRN/current/strict NNUE remain mandatory controls. Next test common-reference
engine strength across120/600s training and nested-data choices, with new-model
compilation parity where needed. Then freeze independent finalist comparisons.
No production architecture, model store or default is changed.
