# Wave 3 trained assessment

Continuation of the original contract, following G3. Started from clean
87008af9464c67d9979310783f60dd1076197bdc, build46, 20 October2026.
No new workstream or production-default change.

## Verified human gate

`BrnLineageAudit` uses the production checksummed checkpoint, optimizer, reference,
history, session and source-ledger readers without opening a training writer.
`wave3-training-audit-plan.json` -> `raw/wave3/trained-audit-01`.
All99 W3 payloads (Gen0..32 in each lineage), plus frozen Gen32, load successfully;
each runtime/optimizer pair agrees on24 diagnostic positions. No history warnings.
The first audit's `complete.json` counter named `verifiedPayloadAndOptimizerCounts`
contains the lineage count4; actual payload count100 is established by its
`stores.json` entries. The reusable auditor now names and counts these separately.
References/history remained stable during each audit. All sessions are COMPLETED,
baseline0,target32,completed32. Initial network/optimizer hashes still match the
original `wave3-lineages.json`, which remains unchanged.

Exact endpoints and compact histories: `wave3-trained-checkpoints.json`.
RESGATE Best/latest is32; RESCUBIC Best31/latest32; BRE-CONTROL Best/latest32.
All used LR.003, Adam(.9,.999,1e-8), batch128,8epochs,seed1,oneworker,held-out WDL,
131072 training and32768 validation positions/generation, equal BT4/CP source weights.
Each added4,194,304 training positions,33,554,432 visits and262,144 updates.
Correction steps are262144; continued-BRE steps2015232.
Persisted total times: RESGATE291.904s,RESCUBIC239.702s,CONTROL355.385s.
These are historical GUI measurements, not controlled training benchmarks.

Residual training/validation tranche hashes match each other across all32 generations.
BRE uses different records: its sequential acquisition ends near3.2M/3.1M raw
BT4/CP records; stable-layout residual acquisition ends near9.7M/9.2M.
Equal sample exposure is verified; identical data and effective optimization are not.
All start before Gen32's historical cursors, so training overlap is expected.
Changing validation tranches make between-generation loss trends non-comparable.
Best reflects local WDL promotion, not independent playing strength.

## Diagnostic evidence

`wave3-learning-curve-plan.json` includes Gen8/16/24/32 and RESCUBIC Best31.
`BrnResidualAssessment` reuses the already consumed2048 BT4 +2048 CP development
positions from `raw/wave2/data-01`. No new sealed data or neural updates.
First1024/source fit label-independent constant gate means; second1024/source
provide separate diagnostic metrics. All remain development data.
The first invocation used an incorrect CP filename and failed before evaluation:
`raw/wave3/trained-data-01.stderr`/empty directory are preserved.
Corrected invocation: `raw/wave3/trained-data-02`.
Raw trainer/runtime error is zero for all9 loaded residual checkpoints.

Second-half raw cross entropy (BT4/CP):

| Function | BT4 | CP |
| --- | ---: | ---: |
| Frozen Gen32 | .642954 | .598240 |
| RESGATE32 | .634102 | .600824 |
| RESGATE32 half correction | .630873 | .591721 |
| RESGATE32 constant gates | .637804 | .609477 |
| RESCUBIC32 | .642038 | .624016 |
| RESCUBIC32 half correction | .632361 | .591057 |
| RESCUBIC32 quadratic only | .642626 | .615953 |
| RESCUBIC Best31 | .642569 | .605462 |
| Continued BRE32 | .648588 | .614427 |

Learned conditional gates contribute prediction signal beyond constant gates;
cubic contribution has mixed source effects. Both correction amplitudes warrant
calibration investigation. Neither observation establishes chess value. Intermediate
checkpoint curves fluctuate; more training is not automatically the right remedy.

Position-validity sensitivity found21/2048 CP positions flagged by python-chess:
19 inconsistent castling rights,2 excessive white pawn/piece counts. BT4 had0/2048.
The largest cubic correction (11.56 raw pawns) is an impossible composed pawn
position. `tools/analyze-brn-residual-data.py` independently reproduces the recorded
WDL-link means within1.5e-15 and reports original and validity-stratified metrics
in `raw/wave3/data-validity-sensitivity.json`. Of the second CP half,1015/1024 pass
these necessary (not sufficient reachability) checks. Their CE is Gen32.597843,
GATE.600003,CUBIC.607346,half-CUBIC.590675,CONTROL.613936.
The invalid positions explain much of CUBIC's large CP CE penalty, but not all;
half-correction remains preferable on this diagnostic. No original evidence or
training corpus was filtered/rewritten. Training contamination frequency and
playing-strength consequences remain unknown; future data hygiene is a hypothesis.

`wave3-correction-distribution.json` records complete2048/source raw deltas.
Residual corrections have RMS roughly.45-.56pawns, before the .25 search mapping.
All three trained functions shift raw scores downward on average in these sources;
this is not unique evidence against expressive architectures.

Trained cache probes: `raw/wave3/trained-{gate,cubic}-cache.json`.
Each checks6333 preserved positions, no integer differences, maximum raw differences
below8e-15, and48 same-node full/cache searches with exact completed-result equality.
Median transition latency: GATE1121ns vs reference148ns; CUBIC679ns vs140ns.
Worker primitive payloads6232/10162bytes; combined model payloads847496/899816bytes.
Latency includes frozen base and correction. Costs differ from whole-search cost.
The48 fixed-node searches took2.068s cached vs2.679s full for GATE and1.626s
cached vs1.858s full for CUBIC. The trained reference has a different search tree;
do not infer its search slowdown from the Gen0 identical-function measurement.
Runtime/jar/source identities: `wave3-trained-runtime.json`; frozen jars local-only.

## Prospective primary development screen

Fixed before any trained W3 game outcome. Four immutable plans:
`wave3-trained-{gen32,control}-{node,clock}-plan.json`.
Primary candidates are latest32 for all three, so equal completed exposure is used
without selecting a generation from match outcomes. RESCUBIC Best31 is retained
for diagnostic sensitivity if justified; it is not silently substituted.

Against frozen Gen32: both residuals and continued BRE. Against continued BRE:
both residuals. Each comparison has16 reversed-colour opening pairs (32games),
at32768 nodes/move and separately50ms/move:320 games total.
Reuse16 development openings from the recorded Wave2 from-scratch plan.
Explicit common Policy4,64MiB TT/engine,one worker/engine,one simultaneous game,
no pondering,one BelowNormal JVM,768MiB heap,ActiveProcessorCount1.
Each invocation ends after12games or240s (finishing its current game).
512ply cap is reported as incomplete, never an adjudicated draw.
Legal-only fallback on exhausted first iteration is recorded and counted.
No concurrent benchmark/training workload.

Complete the fixed screen unless a correctness/resource problem invalidates it;
do not extend unchanged comparisons seeking significance. Replay every move with
independent python-chess and report paired uncertainty, failures,caps,fallbacks,
nodes,time,depth. All intervals are descriptive: reused openings, short budgets,
candidate selection and multiple comparisons prevent a confirmation claim.
Equal-node results isolate some cost effects but still involve search interaction.
Equal-time losses alone do not reject an expressive family.

After the screen, diagnose amplitude, nonlinear ablations, cost and maturity using
the recorded evidence. Continued BRE measures extra-training effects with the
explicit data-partition caveat. Any eventual success needs a frozen selected
function, fresh independent opening clusters, predeclared multiplicity/error and
stopping rules, practical clocks and useful expressive-value controls.

## Primary screen completed

All320 games completed;41622 moves independently replayed with python-chess1.11.2.
No failed/capped/missing games. Compact results, paired scores/intervals and telemetry:
`wave3-primary-results.json`; raw receipts and independent analysis are under
`raw/wave3/trained-{gen32,control}-{node,clock}*`.

| Candidate | Reference | Node W/D/L; score | Time W/D/L; score |
| --- | --- | --- | --- |
| RESGATE32 | Gen32 | 14/1/17;45.31% | 4/9/19;26.56% |
| RESCUBIC32 | Gen32 | 11/8/13;46.88% | 10/7/15;42.19% |
| BRE-CONTROL32 | Gen32 | 15/6/11;56.25% | 13/5/14;48.44% |
| RESGATE32 | BRE-CONTROL32 | 15/6/11;56.25% | 8/4/20;31.25% |
| RESCUBIC32 | BRE-CONTROL32 | 20/6/6;71.88% | 16/4/12;56.25% |

Each cell32 games/16 paired openings. Gen32 node descriptive95% intervals:
GATE28.45..62.18%,CUBIC30.21..63.54%,CONTROL41.76..70.74%.
Gen32 time: GATE13.55..39.58%,CUBIC27.54..56.83%,CONTROL34.67..62.20%.
CUBIC vs CONTROL node54.62..89.13%,time41.08..71.42%, still selected development
results, not confirmation. The non-transitive point estimates do not establish a
causal architectural ranking. Continued training did not establish a Gen32 gain.

The control-clock game `p003-m2-c0.json` had one first-iteration fallback by each
engine (both just over50ms); both legal moves were replayed. CUBIC lost that game.
They remain included. Other studies had no fallback. Observed search throughput
was roughly0.71-0.82M nodes/s for GATE,1.01-1.07M for CUBIC and1.72-1.82M for
the additive models, across different searched positions. GATE loses much more
at equal time than equal nodes. CUBIC's contrast with the continued control
warrants further investigation, not a claim of superiority to frozen Gen32.

The existing GUI preparation reader was rerun against the already-present roots
with a new output only (`raw/wave3/persisted-gui-settings.json`): saved recipes,
32-generation budgets, source selection and actual current32 endpoints agree with
the audit. No lineage creation or training occurred.

The trained optimizer diagnostic completed18 independent64-step arms in memory;
see `wave3-trained-optimizer-results.json` and the prospectively recorded plan.
At LR.001, CUBIC heldout CE changed BT4.642038->.639466 and CP.624016->.616724.
At .003, BT4 reached.638644 but CP.622154 despite lower fit loss.
GATE's BT4 heldout worsened slightly at all rates; CP was best at.001.
These tiny reused subsets support testing a more conservative continuation rate;
they do not establish an optimal rate or a need for indefinite training.
Seven correction-control tests passed; no substantial autonomous training.

## Next admitted investigations

Profile-guided cache cost reduction, then a separately frozen development screen
of half corrections, CUBIC's persisted Best31 and nonlinear ablations. Primary
receipts/protocol/runtime remain unchanged. A JFR run of the same GATE oracle
(`raw/wave3/trained-gate-profile.*`) recorded520 execution samples.83 samples had
the canonical material scan atop the cached-GATE stack, versus149 samples containing
its raw evaluator overall. This motivates reusing the already maintained frozen
base material and avoiding repeated same-position work. Sampling combines diagnostic
and search phases; it is not an exact percentage of total engine time.
Verify integer/raw bounds and identical search results before admitting any speedup.

The cache revision reuses the frozen base's maintained material, and GATE caches
same-placement results separately for each side. It changes no weights, codecs,
training objective or search algorithm. Scalar full recomputation remains the oracle.
Fourteen focused core tests passed, including imbalanced-material repeated/null
perspectives, special moves, separate owners,9000 random transitions per W1 cache
and4000 zero-correction reference transitions. Trained probes retain zero integer
differences and maximum raw discrepancy below8e-15; all recorded old/new completed
search results agree.

The first timing comparison was noisy and did not establish whole-search benefit.
A serial balanced old/new follow-up is preserved in
`wave3-material-cache-results.json`, with all eight raw runs retained.
Mean cached transition latency changed GATE1174->1030ns and CUBIC748->632ns.
Mean warmed fixed-node search time changed GATE1.342->1.278s (~4.7% less elapsed)
and CUBIC1.019->.989s (~3.0%). These modest measurements on16 roots are not a
general speed or strength guarantee, and do not remove the baseline cost gap.
GATE worker primitive payload rises34bytes to6266; CUBIC remains10162.
The small revision is retained on this evidence. The next clock screen must include
unscaled optimized models to distinguish cache and correction-amplitude effects.
