# Cross-architecture scorecard

Current programme only. Historical values are in E000 links, not fresh results.
Unknown measurements are **pending**, never zero. Training seconds exclude data
loading/validation/serialization; end-to-end times and tuning costs are separate.

## Final decision after independent confirmation

Recommend **C02 compiled order-2 pair tables** for the next BRN architecture /
integration decision. No production adoption occurred. All planned empirical
work is complete; no further training or games are scheduled.
Read [RECOMMENDATION](RECOMMENDATION.md) and [final results](Z01-FINAL-RESULTS.md).

| Final opponent | Compiled-pair score | Approximate two-way95% | Simultaneous conditional95% lower |
|---|---:|---:|---:|
| Fresh BRN |80.18%|75.39?84.57%|66.50%|
| Current NNUE v2 |73.73%|69.53?77.73%|60.06%|
| Strict NNUE |74.90%|70.12?79.49%|61.23%|
| Cheap material |64.26%|57.23?70.70%|50.58%|
| Captured native BRN |55.57%|50.98?60.16%|41.89%|
| Compiled own Gen0 |73.93%|66.99?80.47%|60.25%|

All3072games were scored; no missing/capped/failed final outcome. Primary bootstrap
gates pass all six comparisons; conservative simultaneous checks do not establish
native superiority. Intervals have different assumptions and only two source/order
runs. The recommendation is bounded to tested budgets/search/hardware, not an
unconditional all-controls or long-run scaling claim.

Final game NPS was about2.50M for compiled pairs versus.385M BRN/.780M currentNNUE
in their direct comparisons; material remained faster at4.07M. Compiled model
storage is442124B and immutable primitive runtime storage799688B plus2920B/worker.
Same-function paired C02 cost evidence is distinct from cross-family tree speed.

All22sealed profiles passed, with zero observed original/compiled metric
differences. Pair raw half-MSE.128969/.125921 is worse than BRN.073677/.069346;
edge.072325/.070848 leads only rangeA in this sealed sample. Thus predictive
quality and practical playing strength remain distinct trade-offs. Pair prediction
plateaus and currentNNUE still improves; compute/data playing scaling remains
inconclusive. Family dispositions are in STATE and RECOMMENDATION.

The following decision snapshots are retained chronology. Their prospective
language is superseded by the final state above.

## Historical decision snapshot before final confirmation

The 1,024-game screen and 576-game learning/practical-control gate are complete,
with no capped, missing or failed games. Their full WDL, per-run estimates,
approximate intervals and resource audits are below and in Z01. The longer
120/300/600-second and nested-data prediction studies are complete and audited;
the896-game compute/data scaling screen is complete, with no caps, failures or
missing games and all audits passed. See the appended complete table and
Z01-ENGINE-SCALING for its small-sample limits. The bounded qsearch diagnostic is
complete:703/704 measured roots finished; one linear node cap retained. Final
decisions are frozen in Z01-FINAL:3072 games across six direct controls,28 final
runtime measurements and22 sealed-quality evaluations. Runtime passed all checks;
the final3072-game run is active. The first opening preflight found one training
geometry overlap; a wholly new128-root sample passed all checks under the frozen
failure rule (Z01-FINAL-OPENINGS-02). No final outcomes inspected. No sealed
test metrics have been opened and no production replacement is justified yet.

| Family | Current evidence | Disposition |
|---|---|---|
| Pair tables |Within120/600s:29.69/57.81% vs material; largest positive score-change point estimate, interval touches zero | Original implementation remains a control; compiled version is the practical lead |
| Compiled pair tables (C02) |Within600s:85.94% vs BRN,76.56% vs current NNUE,71.88% vs own Gen0,71.88% vs material,60.94% vs native BRN; last two intervals include .5 | Principal practical candidate; independent confirmation next, no production adoption |
| Linear relational readout |Earlier own-Gen0 learning; within600s32.81% vs material, interval below .5; no observed positive late point change | Further mainline scaling lacks support; preserve useful learning and its practical cost deficit |
| Edge functions |Lowest eligible raw loss; within600s43.75% vs material; data-size play46.88%, inconclusive | Predictive benefit has not established a practical playing lead |
| Triple tables |Later raw-loss and data sensitivity gains; within600s34.38% vs material; data-size play39.06%, inconclusive | Higher capacity/cost has not earned further mainline expansion from this screen |
| Current BRN / current and strict NNUE | BRN own-Gen0 score72.66%; current/strict NNUE own-Gen0 intervals include .5; all have materially different training/inference costs | Within600s models are mandatory direct final controls |
| SELF / CONTEXT | Context40.63% versus SELF, both run points below .5 | Further expansion deferred; no universal message-passing rejection |
| Learned bitboard circuits | Raw learning benefit over fixed gates; direct play58.59% with interval spanning .5 | Further mainline expansion deferred; very small models remain a distinct trade-off |
| Captured native BRN |Compiled pairs60.94% against it, both run points positive but interval includes .5 | Supplementary practical reference; larger confirmation remains, historical training unmatched |
| Cheap fixed material |Most within600s learned alternatives remain below it; compiled pairs71.88% and original pairs57.81% against it, both inconclusive at32 games | Essential practical reference; separate from own-architecture Gen0 |

These are allocation decisions, not a final ranking by playing strength. Read the
full uncertainty tables before interpreting individual percentages. The two pair
training runs vary source/order; their zero initializer is not random.

Longer-budget update: all14 runs/42 endpoints pass their selection, cursor,
resume, model/state, command and code audits. Process8625.973609s;
optimization8400.1035498s. See Z01's complete three-budget table and
`z01-long-results.json` / `z01-long-crosscheck.json`.

|Family|Best raw loss within600s, A / B|Selected endpoint seconds A / B|Interpretation|
|---|---:|---:|---|
|BRN|.076940 / .074739|300 / 600|Small or negative late change|
|Current NNUE v2|.113938 / .117628|600 / 600|Still improving;600s below earlier equal-exposure training time|
|Strict NNUE|.107262 / .106788|600 / 300|Small or mixed late gains|
|Linear|.078223 / .083105|300 / 600|Mixed late gains; raw loss above BRN in both ranges|
|Pair tables|.136992 / .138832|300 / 300|Both regress slightly after300s despite~50M exposures|
|Triple tables|.126771 / .124881|300 / 600|Lower raw loss than pairs; mixed late gains and playing trade-off pending|
|Edge functions|.072031 / .073397|600 / 300|Lowest eligible raw loss in both ranges; no longer-budget strength claim|

This table does not change the playing-based lead. The nested-data and cost gates
remain necessary; previous gains stay fixed for the planned equal-compute screen.

## Earlier data02 exploratory comparison (retained history)

| Architecture / experiment | Train / inference parameters | Training / quality | Inference / search / strength | Replication / distinctiveness / disposition |
|---|---|---|---|---|
| BRN3 current / E001+B01 | 2627777 /2368577 | 131kx8: loss .07505/.07577;74.16/70.81s |8.87..10.00us;pilot59.38% vs NNUE,82.81% vs own BRN Gen0|Two seeds,same data;learning benefit observed;superiority vs NNUE unconfirmed;reference|
| NNUE material v2 / E001+B01 | 3149953 /3149953 |131kx8:.10693/.10730;324.75/313.62s|2.60..2.72us;pilot40.63% vs BRN,wide interval|Two seeds,same data;exact resume;HalfKP64x32 practical control;equal-compute opportunity remains|
| NNUE legacy / E001 | 3149953 /3149953 | 16kx4: .14415/.14284;14.56/11.18s | Pending | Different prior/units; exact resume; historical control |
| NNUE strict / E001+B01 | 3149953 /3149953 |131kx8:.11945/.11560;30.60/38.02s|2.58..3.03us;play pending|Zero-head pawn residual CE;two seeds,exact resume;recipe-isolation control|
| Low-rank relational energy8 / B01 | 2624841 /2365641 |131kx8:.07514/.07651;67.16/63.90s |8.49..12.27us;pilot52.34% vs BRN,two-way95%[32.81,70.31]%|Short loss advantage vanished;play seed-dependent;fresh energy allocation deferred in Z01 in favor of linear;rank16 deferred|
| Linear relational readout / B01 ablation |2621761 /2362561 |131kx8:.07786/.07808;56.75/58.00s |7.32..8.32us;pilot57.81% in both seeds vs BRN,two-way95%[46.88,68.75]%|Fastest BRN-family engine;principal B01 survivor for fresh replication/scaling;no confirmed win|
| Discrete relational LUT / C01 |order2:113231/110527;order3:1471991/1436839|131kx8 order2:.14050/.14079,14.48/15.51s;order3:.13187/.13053,32.53/33.13s|Order2:3.09..3.23us,75%vsBRN;order3:4.79..4.93us,70.31%vsBRN;160games complete|Both retained for fresh replication;0.44/5.75MB models;60.94/68.75%vsownGen0;no confirmed replacement|
| Higher-order / D01 |2626385/2367185|16kx4 cubic .003:.14828/.14807;.006:.14975/.14615;both paired-rate comparisons mixed|8.32/9.69us;256/256depth4 roots;no games allocated|Four correctness tests+component audit;rank8 cubic serious allocation deferred;larger-data behavior unknown|
| Learned edge functions / E01 |2624641/2365441|131kx8:.073416/.073544,71.32/64.26s;5.7..5.8%better loss than linear|Serious8.28/9.86us;96games:62.5%vsBRN,40.63%vslinear,71.88%vsownGen0|Extreme BRN seed variance;secondary predictive survivor for fresh replication/calibration;no strength claim|
| Message passing / F01+H01 |CONTEXT2621897/2362697;SELF2621833/2362633|131kx8:CONTEXT.07580/.07046,74.95/75.43s;SELF.07606/.07435,66.51/66.02s|CONTEXT7.83/8.04us;128games:62.5%vsBRN,53.13%vsSELF,46.88%vslinear,76.56%vsownGen0|Secondary fresh replication with SELF control;lower-loss seed loses more versus linear;no confirmed strength gain|
| Bitboard circuits / G01 |1layer1537/1025float+32gates;4916Bmodel|131kx8 learned.20478/.20310,23.76/23.78s;fixed.20663/.21128,3.46/3.41s|2.115/2.122us;71.88%vsBRN,59.38%vsownGen0;51.56%vsfixed,two-way95%[37.5,65.63]%|Both one-layer variants retained for fresh replication;gate-learning necessity unproven;raw quality weak;no confirmed replacement|

No current winner. Retaining production BRN3 during research is a safety boundary,
not a completed evidence-backed recommendation.

<!-- Z01_FRESH_START -->

Fresh Z01 training COMPLETE:262144x8,two filtered ranges/seeds211/337;all22runs
passed exact resume. Gain selection is complete;independent confirmation remains pending. Compare raw loss
within each range,not against data02's different validation population. See Z01
for curves,provenance and memory. All test labels remain sealed.

|Fresh family|Loss211 /337|Optimization seconds211 /337|State|
|---|---|---|---|
|brn|0.083722 /0.078791|109.22 /103.16|Fresh training/gain selection complete;confirmation pending|
|nnue-v2|0.115668 /0.107761|657.63 /669.14|Fresh training/gain selection complete;confirmation pending|
|nnue-strict|0.125279 /0.113736|56.96 /56.46|Fresh training/gain selection complete;confirmation pending|
|linear|0.086371 /0.086511|88.32 /78.31|Fresh training/gain selection complete;confirmation pending|
|tuple2|0.145949 /0.143518|21.84 /22.22|Fresh training/gain selection complete;confirmation pending|
|tuple3|0.142310 /0.137971|45.13 /44.05|Fresh training/gain selection complete;confirmation pending|
|edge|0.075572 /0.077883|78.09 /73.61|Fresh training/gain selection complete;confirmation pending|
|self|0.086450 /0.085077|96.84 /84.46|Fresh training/gain selection complete;confirmation pending|
|context|0.085628 /0.077692|98.31 /88.97|Fresh training/gain selection complete;confirmation pending|
|logic|0.206938 /0.189937|44.46 /47.83|Fresh training/gain selection complete;confirmation pending|
|logic-fixed|0.226490 /0.199268|6.39 /6.11|Fresh training/gain selection complete;confirmation pending|

Main Z01 gain selection COMPLETE:704games,zero missing/capped outcomes,all frozen
hash/opening checks pass. Common four-gain opportunity against fresh BRN native.25;
only4shared openings/two ranges. Selected gains are.125 for BRN,current/strict
NNUE,linear,context;.25 for tuple2/3,edge,SELF,learned logic;1for fixed logic.
Order2's selected pilot score81.25% is the largest point estimate,but is selected
from four gains and is not confirmation. Identical BRN.25 self-play itself scored
65.625% with two-way95%[40.625,87.5]%,showing the sample's large variability.
Use z01-gain-selection.json for all44grid scores;no architecture winner is declared.
Native BRN24run/192game gain plan is complete (selection below). Common equal-time,
data-efficiency,cheap-prior,own-Gen0,and50/100ms confirmation remain required.

Native calibration now COMPLETE:192games,no missing/capped outcomes. Selected
reference is captured brn-old at gain.5,71.875%(11/1/4),two-way95%[50,93.75]%,
on the same four calibration openings. This maximum-of12selection is not a
confirmed improvement or a matched historical compute comparison. See native
selection/results JSONs;production gain/defaults and stored Best pointers remain unchanged.

## Fresh runtime campaign COMPLETE

All48runs passed in637.5483857s. All3072depth4roots completed. Across24profiles,
all24948legal child requests matched a fresh evaluator exactly;all parent score
comparisons were exact and input-preservation checks passed. A has1030requests
per profile,B1049;both include king moves,captures,promotions,castling and en
passant. Frozen model/gain/arguments/common-request hashes and unchanged compiled,
source and runner manifests pass. z01-runtime-results.json retains all costs,
pass timings,resource counters,request identities and residual-change diagnostics.
The analysis helper also rejected altered model identity,wrong root count,and a
missing profile run;witness in z01-runtime-analysis-validation.json.

|Family|Initialize+score us A / B|Child+score us A / B|128depth4roots seconds A / B|
|---|---:|---:|---:|
|brn|9.125 / 8.675|5.516 / 4.949|6.844 / 4.989|
|nnue-v2|3.007 / 2.381|1.407 / 1.325|3.077 / 2.412|
|nnue-strict|2.738 / 2.477|1.577 / 1.441|2.858 / 2.719|
|linear|7.872 / 5.482|3.063 / 2.961|5.717 / 3.907|
|tuple2|5.353 / 2.907|0.446 / 0.468|2.205 / 2.170|
|tuple3|6.301 / 5.407|0.826 / 0.920|3.070 / 2.479|
|edge|10.475 / 7.405|4.254 / 3.042|5.882 / 4.546|
|self|9.547 / 7.847|5.146 / 4.584|6.981 / 5.406|
|context|8.282 / 7.821|4.755 / 4.690|6.810 / 5.011|
|logic|2.142 / 2.244|2.102 / 2.216|3.936 / 3.722|
|logic-fixed|2.125 / 2.127|2.155 / 2.307|3.806 / 3.404|
|material-fast|0.112 / 0.100|0.044 / 0.043|1.679 / 1.675|

These are native cache semantics,not forced BRN rebuilds. Search trees differ
across evaluators;node totals/time tails are preserved. Whole measurement-JVM
peak working sets span368.8..392.9MiB and include loaded data/JVM,so they do not
measure isolated model allocation. Repeated-pass timing and two populations show
real variation;do not infer strength from latency or NPS. Tuple2 has the lowest
learned child-update cost and128root time in both ranges;cheap material is faster
still. LOGIC's small model does not yield the fastest search. Fixed-gate selected
gain1 produces residual-change p95 of96/114score units versus learned gain.25
37/34;gain differs,so this is not evidence that learned gates alone smooth scores.

## Equal-optimization-time curves COMPLETE

All22runs/88endpoints completed and passed the frozen plan audit,including whole
training populations,cursor/sample/update arithmetic,one-batch overshoot bounds,
endpoint model/state identities,exact scheduled-batch continuation,Gen0 byte
identity with the exposure study,and independent reconstruction of validation
selection. Source,compiled and runner hash manifests stayed unchanged. The largest
budget overshoot was0.0519783s,within its recorded batch bound. Total optimization
including discarded LOGIC branches was2669.6696318s;full process cost3034.5443668s.
No sealed metrics. Full evidence:z01-timed-results.json;reproduce using
tools/analyze-brn-timed.py and the frozen timed/runtime plan digests. Its three
negative functional checks rejected wrong recipe,budget grid,and population;
witness:z01-timed-analysis-validation.json. This is additional functional QA,
not an increase to the recorded Java/Python unit-test count.

Minimum eligible first4096 raw validation half-MSE at each budget,A211 / B337:

|Family|15s|30s|60s|120s|
|---|---:|---:|---:|---:|
|brn|0.116530 / 0.115517|0.102811 / 0.098556|0.091278 / 0.089825|0.083283 / 0.079395|
|nnue-v2|0.202485 / 0.181553|0.189544 / 0.169250|0.172707 / 0.158880|0.169779 / 0.153887|
|nnue-strict|0.155642 / 0.143446|0.131788 / 0.130380|0.123415 / 0.113646|0.120322 / 0.108325|
|linear|0.114740 / 0.109604|0.103940 / 0.099215|0.093548 / 0.091988|0.082081 / 0.088050|
|tuple2|0.149819 / 0.155208|0.149578 / 0.143227|0.141471 / 0.139082|0.141111 / 0.137275|
|tuple3|0.158963 / 0.161619|0.149717 / 0.145766|0.142349 / 0.141309|0.134112 / 0.134103|
|edge|0.115302 / 0.109081|0.098896 / 0.095970|0.081073 / 0.083884|0.075866 / 0.077595|
|self|0.112650 / 0.110115|0.104134 / 0.102641|0.093216 / 0.087861|0.083321 / 0.084899|
|context|0.118655 / 0.120360|0.103924 / 0.110448|0.096663 / 0.086528|0.084565 / 0.078947|
|logic|0.222908 / 0.204337|0.214872 / 0.197119|0.203730 / 0.187911|0.192977 / 0.180718|
|logic-fixed|0.233483 / 0.198586|0.226901 / 0.198586|0.226901 / 0.197588|0.226901 / 0.194298|

All families selected120s except fixed gates211,which selected30s. The latter
actual60/120s losses worsened to.232762/.234125;the table retains the eligible
best rather than pretending that late training improved it. Normal model paths
use about120s optimization;LOGIC's independent earlier terminal refits add study
cost (learned~121.68s total per run,fixed~133.125s). Diagnostic continuation updates
are unretained and separately counted. Initialization,validation,checkpointing
and shuffling remain outside the optimization clock but inside process cost.
Timed report searchHalfMse uses each trainer's native mapping;it is not selected-
gain calibration. Raw outcome loss is the common selection quantity.

Interpretation,conditional on these recipes and populations:
* Edge beats BRN's120s raw loss by8.91%/2.27%,and linear by7.57%/11.87%. This
  supports a repeated predictive benefit;it still does not establish engine strength.
* Linear beats BRN slightly in A but is worse in B;its practical case remains
  cheaper inference. SELF is close to BRN in A and worse in B.
* CONTEXT versus SELF is1.49%worse in A and7.01%better in B;its asymmetric benefit
  persists. This is not a broad replicated improvement.
* Tuple3 only pulls ahead of tuple2 later:120s loss is4.96%/2.31%better. Tuple2
  remains smaller/faster;its A60-to120s loss is nearly flat. More orders are not
  automatically worth their cost.
* Learned gates beat fixed-gate eligible loss by14.95%/6.99% at120s despite fixed
  gates processing~42million examples. Both remain substantially worse predictors
  than relational models. Fixed-head extra repetitions are not a demonstrated
  route to better quality. Gate-learning playing value still requires direct games.
* Current NNUE v2 is compute-limited here (~0.31million exposures at120s),whereas
  strict NNUE reaches~4.1million. Their equal-exposure results (~2.1million each,
  v2~658/669s versus strict~57/56s) remain separate evidence;do not declare all
  NNUE variants inferior from the120s budget.

These are validation curves,not a strength ranking;independent screening/final confirmation remain.

## Structure and maintenance trade-offs

These are qualitative implementation observations from the family records and
verification sources, not a novelty score or a strength ranking. All candidates
are research implementations; a recommendation would still require separately
authorized production integration.

| Family | What changes from conventional NNUE / current BRN | Maintenance implication |
|---|---|---|
| Current / strict NNUE | Same HalfKP accumulator and dense hidden/readout topology; the strict control changes initialization, objective, optimizer and residual mapping | Existing native inference path; training-recipe changes must remain explicit rather than being credited to architecture |
| Linear relational readout | Keeps BRN's learned relation encoder and local ReLUs, removes the nonlinear dense readout | Simpler head and cheaper search; the overall board function is still nonlinear, and most parameters remain in the encoder |
| Factorized energy / cubic | Adds explicit low-rank products of pooled relation features instead of a generic hidden readout | More factors and gradients to maintain; deferred tested variants did not show a consistent added benefit at their admitted gates |
| Pair / triple tables | Direct scalar functions addressed by joint discrete square states; no hidden floating-point vector or MLP readout | Compact pair table and cheap affected-template updates; triple addressing increases storage and update cost; sparse-pattern coverage must be monitored |
| Edge functions | Keeps BRN's relation encoder, replaces the head with learned piecewise-linear functions of pooled channels | A partial KAN-inspired departure, not a full spline KAN; knot spacing, linear tails and codec metadata are explicit controls |
| SELF / CONTEXT | Adds a residual local transformation; CONTEXT also broadcasts a learned transform of other pieces' updated states | The SELF ablation separates local depth from message passing; derivatives and cache semantics are more involved, with mixed observed benefit |
| Learned / fixed circuits | Hard Boolean bitboard transforms and region popcounts feed a small scalar linear readout; learned gates use a soft training relaxation | Very small models and bitwise inference; hardening, operator selection and refit schedules add training complexity; fixed gates isolate the learned-operator contribution |
| Cheap fixed material | No learned residual | Minimal reference cost, not a learned architecture or evidence of positional learning |

Architectural distance from NNUE is a decision dimension only alongside measured
strength, cost and stability. Greater novelty does not waive the learning-control
or independent-confirmation gates. Pair/triple tables, low-rank polynomials and
logic circuits belong to established broader method families; these experiments
do not establish a claim of universal architectural originality.

## Independent 50 ms engine screen COMPLETE

All 128 frozen batches and 1,024 games completed; no missing, capped or failed outcomes. Maximum observed game length: 499 plies. Process cost: 8,238.6645233 s. Model/gain/opening bindings pass. All actor move counts are unambiguous, and source/compiled/runner manifests match the earlier runtime campaign. Across comparison/seed/actor aggregates, measured search time averages 48.166..49.038 ms per played move; faster candidates were not given extra time. Whole-JVM peak working sets range 199,024,640..278,458,368 bytes, not isolated model memory.

The new resource auditor passed four unit checks and the full real campaign. Evidence: `z01-screen-results.json`, `z01-screen-resources.json`, and `z01-screen-resource-validation.json`. The four checks cover reversed-color odd-ply accounting, failed-search denominators, changed commands/impossible timing, and missing/running evidence. No Java or runner code changed. This raises the cumulative distinct Python unit-check count from six to ten; the 77 Java checks are unchanged.

Each row has 64 games, two training ranges/seeds and 16 shared opening clusters. Scores are win plus half draw. The two-way 95% bootstrap intervals are approximate, particularly with only two training runs. This sample selects subsequent work; it is not final acceptance.

|Comparison (first side scored)|W / D / L|Score %|Seed 211 / 337 %|Two-way 95% %|
|---|---:|---:|---:|---:|
|nnue-v2-vs-brn|37 / 10 / 17|65.62|60.94 / 70.31|[50.00, 79.69]|
|nnue-strict-vs-brn|35 / 9 / 20|61.72|60.94 / 62.50|[48.44, 75.00]|
|linear-vs-brn|32 / 17 / 15|63.28|64.06 / 62.50|[51.56, 75.00]|
|tuple2-vs-brn|50 / 6 / 8|82.81|82.81 / 82.81|[73.44, 92.19]|
|tuple3-vs-brn|41 / 9 / 14|71.09|70.31 / 71.88|[59.38, 82.81]|
|edge-vs-brn|30 / 12 / 22|56.25|54.69 / 57.81|[43.75, 68.75]|
|self-vs-brn|25 / 12 / 27|48.44|54.69 / 42.19|[34.38, 64.06]|
|context-vs-brn|29 / 11 / 24|53.91|57.81 / 50.00|[40.62, 65.62]|
|logic-vs-brn|29 / 16 / 19|57.81|50.00 / 65.62|[42.19, 75.00]|
|logic-fixed-vs-brn|27 / 8 / 29|48.44|53.12 / 43.75|[31.25, 65.62]|
|material-vs-brn|43 / 11 / 10|75.78|76.56 / 75.00|[64.06, 86.72]|
|native-vs-brn|46 / 4 / 14|75.00|76.56 / 73.44|[64.06, 85.94]|
|edge-vs-linear-common|23 / 14 / 27|46.88|51.56 / 42.19|[34.38, 60.94]|
|context-vs-self-common|24 / 4 / 36|40.62|34.38 / 46.88|[26.56, 55.47]|
|logic-vs-fixed-common|30 / 15 / 19|58.59|54.69 / 62.50|[45.31, 70.31]|
|tuple3-vs-tuple2-common|20 / 8 / 36|37.50|39.06 / 35.94|[23.44, 51.56]|

Allocation decision from the complete screen:

* Pair tables are the principal practical candidate: 82.8125% in both fresh runs, the strongest screen result, smallest table model and fastest learned search. This does not establish a replacement: cheap material scored 75.78125% and the captured native BRN scored 75% against the same fresh BRNs. Direct learning/practical controls are necessary.
* Linear readout remains the secondary practical candidate: 64.0625% / 62.5% against BRN, repeating the earlier cost/play signal. Its prediction loss does not consistently beat BRN.
* Edge functions remain a predictive scaling comparison, not a demonstrated playing improvement over linear: direct common-gain score 46.875%, with mixed seeds and an interval spanning .5. Triple tables retain a capacity/data-scaling question, but lose the direct point comparison to pairs in both runs and cost more. Neither is promoted to the main practical lead.
* Further SELF/CONTEXT expansion is deferred: CONTEXT scored 40.625% against SELF, with both seed point estimates below .5, and neither beats the cheaper linear candidate through repeatable direct evidence. This is evidence about the tested one-step family, not a universal message-passing rejection.
* Further mainline circuit expansion is deferred: learned gates improved raw prediction, but direct play against fixed gates is still inconclusive (58.59375%, interval [45.3125,70.3125]%). Their model size remains distinctive; table models search faster and currently offer the stronger practical lead. No universal circuit rejection or proof that gate learning is useless.
* Both current and strict NNUE beat fresh BRN by point estimate in both runs; they remain mandatory controls. Native BRN has unmatched training history. A fresh-BRN win alone cannot answer the architecture question, and no loss-only or indirect ranking is a final strength claim.

Next: prospectively freeze own-Gen0 runtime and fresh-opening learning/practical controls for BRN, current/strict NNUE, linear and pair tables. Keep pair/linear comparisons against cheap material and pair comparisons against current NNUE/native BRN. Further measured-budget and nested-data scaling allocations remain to be frozen; no sealed labels have been opened.

Own-Gen0 runtime for BRN/current NNUE/strict NNUE/linear/pair tables is now complete:20 runs,1,280 depth4 roots,10,395 exact legal-transition checks,250.1945785 s. See z01-gen0-runtime-results.json. The576-game learning/practical-control plan is frozen in z01-learning-plan.json; no outcomes yet.


## Learning/practical-control gate COMPLETE

All 72 batches / 576 games completed without caps, failures or missing outcomes; maximum 702 plies. Process cost was 5,238.4115632 s. Frozen model/gain/opening bindings, move accounting and source/compiled/runner identity against the runtime campaign passed. Actor mean search times were 47.995..49.164 ms per played move. Whole-JVM peak working sets were 215,928,832..314,818,560 bytes. Evidence: `z01-learning-results.json`, `z01-learning-resources.json`, `z01-learning-crosscheck.json`. No sealed metrics were opened.

Each row contains 64 games, two training ranges/runs and 16 shared opening clusters. Intervals are approximate two-way opening/run bootstrap intervals; only two training runs limit their interpretation. This is allocation evidence, not final acceptance.

|Comparison (first side scored)|W / D / L|Score %|Run 211 / 337 %|Approximate 95% %|
|---|---:|---:|---:|---:|
|brn-vs-own-gen0|40 / 13 / 11|72.66|73.44 / 71.88|[62.50, 82.81]|
|nnue-v2-vs-own-gen0|26 / 16 / 22|53.12|48.44 / 57.81|[37.50, 65.62]|
|nnue-strict-vs-own-gen0|31 / 11 / 22|57.03|56.25 / 57.81|[43.75, 70.31]|
|linear-vs-own-gen0|42 / 15 / 7|77.34|71.88 / 82.81|[60.94, 89.06]|
|tuple2-vs-own-gen0|34 / 9 / 21|60.16|54.69 / 65.62|[45.31, 76.56]|
|tuple2-vs-material|20 / 11 / 33|39.84|35.94 / 43.75|[25.00, 54.69]|
|linear-vs-material|16 / 10 / 38|32.81|37.50 / 28.12|[20.31, 48.44]|
|tuple2-vs-nnue-v2|37 / 10 / 17|65.62|64.06 / 67.19|[56.25, 75.78]|
|tuple2-vs-native|29 / 13 / 22|55.47|46.88 / 64.06|[35.94, 73.44]|

BRN and linear readout show clear own-Gen0 learning at this sample. Pair-table learning is positive by point estimate in both runs but remains inconclusive; current/strict NNUE learning is also unconfirmed here. Pair tables beat current NNUE v2 directly, but do not yet establish superiority to native BRN, and both pair tables and linear readout score below the cheap material prior in both runs. Linear versus material has an interval wholly below .5. Native versus material has not been tested directly, so no indirect ranking is asserted.

Pair tables remain the practical architectural lead; useful learned strength per evaluation cost is unresolved. Linear remains a secondary candidate with a demonstrated learning effect and a material-cost deficit. Final recommendation cannot follow merely from a fresh-BRN win. Further measured-budget and nested-data scaling, equal-compute play, independent final confirmation and sealed quality remain.


## Nested-data study COMPLETE

All14 runs and the amended audit pass. Each small run used131072 positions x16
epochs versus the original262144 x8, both2097152 exposures/16384 updates, with
nine eligible selection opportunities. All held-out bytes, selected ordinals,
checkpoint identities, exact next-batch resume, Gen0 model/state, aligned curves
and bridge checks pass. Evidence: `z01-nested-results.json`. New optimization
2629.7171026s; full process2937.2147235s (48.95minutes). No new games or sealed metrics.

The first complete audit rejected a faulty historical-source assumption: the
bridge's single source delta described the first fresh run, while later fresh
runs recorded additional uncompiled Material, Transitions and OpeningAudit
helper drafts. ALL historical compiled and runner manifests are identical; shared
source files are unchanged. The added draft paths/hashes and absence of their
compiled classes are audited in `z01-nested-code-reconciliation.json` (SHA256
`5abd4d8459750be591b507e8a2699efd742eff3ffe9e78f6d30bd465f24f346e`).
The failed audit/analyzer are preserved. No data, training, metric or selection
rule was changed. The amended analyzer passes six tests, including rejection of
executable/shared-source/current-code changes; receipt
`z01-nested-amended-analyzer-validation.json`. Cumulative distinct unit tests are
77 Java and16 Python. This is not a full-suite result.

Original training plan remains immutable at SHA256
`80a146cc603797f6b3fa7499b8090caca9b4a2354281fa24fbeebc8b38d19816`.
The analysis-only amendment `z01-nested-analysis-plan.json` has SHA256
`9dc6f0038c12f0a33a689334570814e66bd5b206d4ad47a61c8dc68de3040530`;
only code reconciliation and its analyzer-validation binding differ. Whole
historical/current source manifests are not identical, and every historical
uncompiled draft byte is not claimed retained. Executable identity plus the
previous14 exact first-epoch reproductions support the comparison.

|Family|131072-position best loss A / B|262144-position best loss A / B|Selected small epochs A / B|
|---|---:|---:|---:|
|BRN|0.092477 / 0.098220|0.083722 / 0.078791|16 / 12|
|Current NNUE v2|0.133174 / 0.128797|0.115668 / 0.107761|12 / 12|
|Strict NNUE|0.140036 / 0.130250|0.125279 / 0.113736|16 / 12|
|Linear|0.099210 / 0.103788|0.086371 / 0.086511|12 / 8|
|Pair tables|0.149777 / 0.148426|0.145949 / 0.143518|12 / 14|
|Triple tables|0.151880 / 0.148025|0.142310 / 0.137971|16 / 16|
|Edge functions|0.092349 / 0.101437|0.075572 / 0.077883|10 / 14|

Doubling distinct positions lowers eligible validation loss in all14 comparisons
at the same exposure/update budget. Relative to the small-data loss, the reduction
is2.56%/3.31% for pairs,6.30%/6.79% for triples, and18.17%/23.22% for edge. Triple
tables therefore benefit more from distinct data than pairs in this tested range;
this supports the capacity/data hypothesis but does not establish stronger play.
All models still use the same respective recipe/initializer. Source ranges are
adjacent and subsets dependent; these are not extra independent replications.

Actual optimization time is not matched: for example small-data current NNUE
took865.922/854.304s versus657.629/669.141s for the retained larger-data runs.
Report the measured difference without assigning an unverified cause. Small
odd epochs remain diagnostic-only even when better; BRN211, both current NNUE,
strict337 and pair337 have parent selections that differ from the eligible study
selection. Use the analyzer's exact inference-only epoch paths; never attach the
parent selected optimizer state to those different weights.

Next: C02 source activation, targeted correctness tests and frozen actual-model
parity/cost checks. Then matched-compute and data-size engine comparisons, final
confirmation, frozen sealed quality and recommendation. No larger training-time
or1M-position allocation is admitted by this result alone.


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

## Completed engine compute/data scaling screen

All 112 batches / 896 games completed, with no caps, failures or missing games.
Maximum game length: 646 plies. Measured Java-process cost: 7872.5998811s
(131.21 minutes; outer-controller overhead is separate). Main outcome, resource,
code/metadata and paired-difference audits pass. Source, compiled classes, runner,
JVM command, Java and platform fingerprint match the completed C02 playing screen.
All actor group means are 47.8975..49.1776ms at the nominal 50ms setting.

Frozen plan: `z01-engine-scaling-plan.json`, SHA256
`a2fccfca7856877fd04faba75a4a4dac49ff66a3af3e4c1adb7a38904d0b1c7b`.
Results: `z01-engine-scaling-results.json`, `z01-engine-scaling-resources.json`,
`z01-engine-scaling-crosscheck.json`, and `z01-engine-scaling-differences.json`.
See Z01-ENGINE-SCALING.md for the prospective allocation. Each row below has
32 games, eight common opening clusters and two source/order runs. Approximate
two-way intervals are descriptive and unadjusted; this is not final acceptance.

| Comparison (first side scored) | W / D / L | Score % | Run 211 / 337 % | Approximate 95% % |
|---|---:|---:|---:|---:|
| compute-brn-120-vs-material | 8 / 10 / 14 | 40.62 | 37.50 / 43.75 | [21.88, 56.25] |
| compute-brn-600-vs-material | 3 / 11 / 18 | 26.56 | 25.00 / 28.12 | [14.06, 37.50] |
| data-brn-large-vs-small | 12 / 7 / 13 | 48.44 | 43.75 / 53.12 | [25.00, 70.31] |
| compute-nnue-v2-120-vs-material | 6 / 7 / 19 | 29.69 | 34.38 / 25.00 | [12.50, 46.88] |
| compute-nnue-v2-600-vs-material | 9 / 7 / 16 | 39.06 | 56.25 / 21.88 | [9.38, 68.75] |
| data-nnue-v2-large-vs-small | 9 / 9 / 14 | 42.19 | 50.00 / 34.38 | [21.88, 65.62] |
| compute-nnue-strict-120-vs-material | 9 / 5 / 18 | 35.94 | 31.25 / 40.62 | [15.62, 59.38] |
| compute-nnue-strict-600-vs-material | 7 / 7 / 18 | 32.81 | 28.12 / 37.50 | [18.75, 46.88] |
| data-nnue-strict-large-vs-small | 12 / 6 / 14 | 46.88 | 50.00 / 43.75 | [28.12, 65.62] |
| compute-linear-120-vs-material | 10 / 3 / 19 | 35.94 | 34.38 / 37.50 | [17.19, 53.12] |
| compute-linear-600-vs-material | 7 / 7 / 18 | 32.81 | 34.38 / 31.25 | [21.88, 43.75] |
| data-linear-large-vs-small | 13 / 5 / 14 | 48.44 | 37.50 / 59.38 | [28.12, 68.75] |
| compute-tuple2-120-vs-material | 7 / 5 / 20 | 29.69 | 25.00 / 34.38 | [12.50, 50.00] |
| compute-tuple2-600-vs-material | 16 / 5 / 11 | 57.81 | 59.38 / 56.25 | [34.38, 79.69] |
| data-tuple2-large-vs-small | 15 / 3 / 14 | 51.56 | 50.00 / 53.12 | [43.75, 62.50] |
| compute-tuple3-120-vs-material | 6 / 6 / 20 | 28.12 | 34.38 / 21.88 | [12.50, 46.88] |
| compute-tuple3-600-vs-material | 10 / 2 / 20 | 34.38 | 31.25 / 37.50 | [12.50, 56.25] |
| data-tuple3-large-vs-small | 10 / 5 / 17 | 39.06 | 40.62 / 37.50 | [18.75, 62.50] |
| compute-edge-120-vs-material | 8 / 6 / 18 | 34.38 | 37.50 / 31.25 | [18.75, 53.12] |
| compute-edge-600-vs-material | 11 / 6 / 15 | 43.75 | 40.62 / 46.88 | [25.00, 62.50] |
| data-edge-large-vs-small | 11 / 8 / 13 | 46.88 | 43.75 / 50.00 | [31.25, 65.62] |
| compute-compiled-pair-120-vs-material | 16 / 4 / 12 | 56.25 | 50.00 / 62.50 | [34.38, 71.88] |
| compute-compiled-pair-600-vs-material | 22 / 2 / 8 | 71.88 | 81.25 / 62.50 | [46.88, 93.75] |
| data-compiled-pair-large-vs-small | 16 / 3 / 13 | 54.69 | 46.88 / 62.50 | [35.94, 75.00] |
| compiled-600-vs-brn | 26 / 3 / 3 | 85.94 | 87.50 / 84.38 | [71.88, 96.88] |
| compiled-600-vs-nnue-v2 | 22 / 5 / 5 | 76.56 | 71.88 / 81.25 | [62.50, 90.62] |
| compiled-600-vs-native | 18 / 3 / 11 | 60.94 | 59.38 / 62.50 | [43.75, 79.69] |
| compiled-600-vs-own-gen0 | 21 / 4 / 7 | 71.88 | 68.75 / 75.00 | [59.38, 87.50] |

Compiled pairs remain the principal practical candidate. At the within-600s
selection they score 85.94% against BRN, 76.56% against current NNUE, and 71.88%
against compiled own Gen0; all three approximate intervals are above .5. The
71.88% versus cheap material and 60.94% versus captured native BRN remain
inconclusive at this small sample, although both source/run points exceed .5.
Independent larger confirmation remains necessary. These results do not
establish transitive rankings among other families from the common opponent.

| Family | 120s score vs material % | 600s score vs material % | Change, percentage points | Paired two-way 95% change |
|---|---:|---:|---:|---:|
| brn | 40.62 | 26.56 | -14.06 | [-35.94, 7.81] |
| compiled-pair | 56.25 | 71.88 | 15.62 | [-18.75, 53.12] |
| edge | 34.38 | 43.75 | 9.38 | [-21.88, 34.38] |
| linear | 35.94 | 32.81 | -3.12 | [-23.44, 21.88] |
| nnue-strict | 35.94 | 32.81 | -3.12 | [-28.12, 21.88] |
| nnue-v2 | 29.69 | 39.06 | 9.38 | [-18.75, 43.75] |
| tuple2 | 29.69 | 57.81 | 28.12 | [0.00, 53.12] |
| tuple3 | 28.12 | 34.38 | 6.25 | [-20.31, 37.50] |

Original pair tables show the largest positive point change (+28.13 percentage
points), with both runs improving; their interval touches zero. Compiled pairs
improve by 15.63 points, driven by run211; run337 is unchanged. No two-way
difference interval excludes zero. BRN/strict/linear do not improve by point
estimate; current NNUE/triple/edge improvements are mixed or uncertain. A
score change versus common material is not a direct later-versus-earlier win.

Every direct large-versus-small data interval includes .5. Thus the earlier
prediction benefit from doubling distinct data has no confirmed playing benefit
in this small screen. Do not declare data independence, equivalence, or a universal
negative scaling law. Selection used equal maximum exposures and equal eligible
checkpoint opportunities; actual selected exposures and training seconds differ.

At the 600s material comparison, candidate NPS is 2.541M for compiled pairs,
1.314M for original pairs, 0.922M for triples, 0.775M for linear, 0.761M for current
NNUE, 0.717M for strict NNUE, 0.653M for edge and 0.414M for BRN. Opponent/game
trajectories differ: these are observed game throughput, not identical-node
microbenchmarks or a causal ranking inferred by transitivity.

Five new paired-scaling and four new qsearch-auditor tests passed. Cumulative
distinct targeted validation is now 84 Java / 28 Python tests; no full-suite run.
Their retained raw receipts bind the tested sources. The qsearch plan was frozen
before these game results were opened: `z01-qsearch-plan.json`, SHA256
`bb2f4431422c96a4d2a668a4e02465b5e74d38d952ca99994ff4cc0c80cdaf01`.
Its 22 processes / 704 measured root attempts remain to execute. No sealed
metrics, new training allocation, final match or production adoption is implied.

## Completed bounded qsearch diagnostic — 2026-10-07

All22 processes and704 measured root attempts completed their allocation;703 searches finished within their guards. Linear A211 root136 reached100000nodes (99486qnodes,0.2520181s,maximumqply20); it remains an aborted search with no invented score/move. No measured time guards or other aborts. Separate warmup:150/176 searches completed;26 node-guard outcomes are retained. Process cost222.6182555s.

All input, model/metadata, source/class/runner, JVM/Java/platform, command and common-root checks pass. Whole-JVM peak working set349675520..399216640B, private commit526561280..600748032B; zero memory read failures.

|Profile|Completed A / B (of32)|All-attempt root seconds A / B|Qnode fraction A / B|Maximum qply A / B|
|---|---:|---:|---:|---:|
|brn|32 / 32|2.582851 / 1.006320|0.9836 / 0.9714|28 / 19|
|nnue-v2|32 / 32|0.926666 / 0.374755|0.9838 / 0.9665|28 / 17|
|nnue-strict|32 / 32|0.877071 / 0.425441|0.9837 / 0.9694|28 / 19|
|linear|31 / 32|1.740866 / 0.719256|0.9839 / 0.9691|28 / 20|
|tuple2|32 / 32|0.638203 / 0.339415|0.9850 / 0.9714|28 / 21|
|tuple3|32 / 32|1.137836 / 0.409106|0.9843 / 0.9691|28 / 19|
|edge|32 / 32|2.073379 / 0.811532|0.9835 / 0.9693|26 / 21|
|compiled-pair|32 / 32|0.411822 / 0.200514|0.9850 / 0.9714|28 / 21|
|material|32 / 32|0.156974 / 0.115034|0.9760 / 0.9626|21 / 17|
|native|32 / 32|1.800616 / 0.946971|0.9767 / 0.9642|23 / 19|
|compiled-gen0|32 / 32|0.232136 / 0.151621|0.9760 / 0.9626|21 / 17|

The post hoc original/compiled pair check matches every non-timing field on all64 measured and16 warmup attempts, including scores/moves/nodes and guard outcomes. Measured root sums are0.977617597s original versus0.612336606s compiled. This is supportive diagnostic evidence, not a new independently preregistered cost gate.

The unpruned, TT-free research qsearch produces96.26..98.50% qnodes and tails up to28qplies on this small validation sample. Different models search different trees, so cross-family elapsed times are not isolated inference benchmarks. This neither measures production-PVS qsearch nor establishes longer-time-control strength; production matches retain static leaves. No search policy change or enlarged diagnostic allocation is justified by the single retained node cap.

Evidence: `z01-qsearch-results.json` SHA256`50facabe5ebe4241ce3b03aea1ae1a2a6154e1ae0f5fb6e6e87f276a1af8ae4d`; supplemental `z01-qsearch-crosscheck.json` SHA256`006011d77674f55032fcdfa62c08fd5a5165b9c01e956ca56d8c5894cc3dd1fd`. See immutable `Z01-QSEARCH.md` and its frozen plan. No sealed labels were opened.

## Exact final-model runtime allocation complete — 2026-10-07

All28 processes passed:1792 completed depth4 roots and14553 legal-child scores with zero integer differences. Commands, model/metadata identities, all frozen source/classes/runner, Java/platform and memory sampling checks pass. Process cost351.010774700s. No new Java code or tests were needed; cumulative distinct targeted tests remain84Java/28Python, not a full suite.

|Final profile|Initialize+score us A / B|Child+score us A / B|128depth4 root seconds A / B|Stored model bytes|
|---|---:|---:|---:|---:|
|brn|8.7102 / 7.3082|4.3137 / 4.8883|6.648049 / 4.669988|9474352|
|nnue-v2|2.9449 / 2.3658|1.3357 / 1.2970|2.593086 / 2.229550|12599889|
|nnue-strict|2.5355 / 2.3534|1.2757 / 1.2689|2.509324 / 2.268597|12599889|
|compiled-pair|1.2655 / 1.2362|0.1417 / 0.1524|1.587790 / 1.263442|442124|
|material|0.1111 / 0.0984|0.0445 / 0.0404|1.442843 / 1.537428|0|
|native|8.3672 / 6.9652|3.9483 / 4.0897|6.570818 / 4.733366|9474352|
|compiled-gen0|1.2969 / 1.2354|0.1497 / 0.1644|1.743867 / 1.605867|442124|

Whole-JVM working-set peaks248762368..447492096B and private-commit peaks373014528..584712192B. These include loading/JIT/data and are not model-only costs. C02's compiled in-memory primitive tables use799688B plus2920B worker primitives, despite the unchanged442124B stored original weight file.

These are native cache semantics, warmed measurements on common validation roots and legal/special transitions. Initialize+score is not a forced BRN rebuild; child intervals include timer overhead. Different evaluator functions produce different search trees. The finalist has a clear observed cost advantage over neural controls and remains slower than cheap fixed material; final equal-time games are still required.

Evidence: `z01-final-runtime-results.json` SHA256`f0cc0332753c8020c9329a82de3905e5e2891d82ffc3c41713449a711350c000` and `z01-final-runtime-crosscheck.json` SHA256`57c30a855d506b4ace3760a36a768a3d5a68399af6cdfd05cbfff996963268a4`. The original/compiled same-function mechanism retains its separately repeated C02 evidence. No sealed quality or final game result is inferred here.

## Completed final confirmation and sealed evaluation — 2026-10-07

All 3,072 final games completed with no missing, capped or failed outcome; all statistical, resource and provenance audits passed. Compiled pairs scored 80.18% versus fresh BRN, 73.73% versus current NNUE, 74.90% versus strict NNUE, 64.26% versus material, 55.57% versus captured native BRN and 73.93% versus compiled own Gen0. All primary bootstrap gates pass; the conservative simultaneous bound does not establish native superiority. All 22 sealed profiles passed, including exact observed original/compiled metric parity. No selection was reopened.

See [final results](Z01-FINAL-RESULTS.md) for WDL, per-run scores, both interval conventions, simultaneous bounds, actual search resources, complete sealed metrics and hashes. The [recommendation](RECOMMENDATION.md) and [completion audit](COMPLETION-AUDIT.md) now reconcile this evidence. No further empirical allocation or production adoption is scheduled.
