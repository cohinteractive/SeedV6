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

## Prospective calibration/ablation follow-up

Frozen before these games: `wave3-calibration-{node,clock}-plan.json`,
`wave3-{gate,cubic}-ablation-node-plan.json`.
Half correction means `Gen32Raw + .5 * learnedCorrection`, retaining the existing
quarter search mapping. It never halves Gen32 or changes an original checkpoint.
The .5 value was selected from prediction diagnostics, not searched match scores.

At32768 nodes/move, compare half-GATE32, half-CUBIC32 and persisted full CUBIC
Best31 against frozen Gen32 (32games each). At50ms/move, compare those three plus
unscaled optimized GATE32 and CUBIC32 against Gen32 (32games each).
Separately, at32768 nodes/move, compare constant-mean-gate half-GATE against
conditional half-GATE, and quadratic-only half-CUBIC against full half-CUBIC
(32games each). Total320 games; same16 colour-reversed development openings,
maximum512plies and existing12game/240s batch/resource envelope.

The direct ablations test function contribution with co-adapted weights. They
retain the original evaluator implementation cost and are not independently
retrained/optimized additive controls. Learned gate means come from the previously
recorded label-independent first1024/source fit. A positive nonlinear-value claim
would still need appropriate independently trained comparisons/confirmation.
The continued-BRE primary comparisons remain relevant; any promising newly selected
function must also receive an appropriate continued-control comparison before a
final architecture claim.

All counts are fixed; failures require investigation, caps remain unscored,
and every move receives independent replay. Do not extend these unchanged
comparisons to seek significance. Multiple variants, reused openings and the short
clock remain development limitations. Interpret maturity/calibration/cost jointly;
decide the next research or human-training wave only from the completed evidence.
Variant cache oracles must pass before scoring; runtime/provenance is separately
frozen at build47. Original primary evidence remains unchanged.

The runtime was frozen from clean4cc079c/build47 in `wave3-variant-runtime.json`.
The10-game/80-move legal smoke passed independent replay, with10 intentional
eight-ply caps excluded from strength results (`wave3-variants-smoke-plan.json`,
`raw/wave3/variants-smoke-analysis.json`).

The two32-game ablations are complete and independently replayed (8640moves,
no failures or caps). Constant half-GATE scored8/8/16,37.5% against full half-GATE;
its paired descriptive interval is26.54..48.46%. This supports retaining the
conditional-gate hypothesis, not a Gen32 improvement or independent confirmation.
Quadratic-only half-CUBIC scored11/11/10,51.56%, interval36.41..66.71% against
full half-CUBIC. No useful cubic-term playing contribution is established here.
Raw independent analyses: `raw/wave3/{gate,cubic}-ablation-node-analysis.json`.
`wave3-nonlinear-prediction-summary.json` separately records prediction changes.
The96-game calibration-node screen also completed, with13323 independently replayed
moves and no failures/caps. Half-GATE21/4/7,71.875% (descriptive95%57.12..86.63%);
half-CUBIC18/5/9,64.0625% (49.94..78.19%); full persisted CUBIC Best31
14/7/11,54.6875% (38.42..70.95%), each against Gen32.
The large change from unscaled endpoints makes calibration a material research
finding. It does not identify the effect at practical clocks or remove selection
and reused-opening limitations.

All320 follow-up games are now complete:43240 independently replayed moves,
zero failures/caps/fallbacks; sampled peak working set610308096bytes. Compact
results: `wave3-calibration-results.json`. At50ms/move versus Gen32:

| Function | W/D/L | Score |
| --- | --- | ---: |
| Optimized unscaled GATE32 | 13/7/12 | 51.5625% |
| Optimized unscaled CUBIC32 | 8/5/19 | 32.8125% |
| Half-GATE32 | 9/5/18 | 35.9375% |
| Half-CUBIC32 | 11/7/14 | 45.3125% |
| Full CUBIC Best31 | 13/5/14 | 48.4375% |

Each32 games. None establishes a practical strength gain. The large short-clock
rerun change for unscaled GATE (26.56% before,51.56% after) cannot be assigned to
the measured4.7% cache speedup: timing/search variation and small reused samples
remain material. Half-GATE throughput was about.85M nodes/s, half-CUBIC1.16M,
Gen321.84M across different searched positions. Equal-node gains and conditional
ablation evidence justify further controlled testing rather than rejecting either
family or requesting more training without resolving these comparison limits.

`wave3-root-budget-summary.json` compares the first search of each reversed-colour
game on the exact same opening root, with fresh search owners/tables. Half-GATE
loses an average.75 completed ply at50ms (11/16 roots shallower,5 equal) and gets
46.3% of Gen32's nodes at the median root. Half-CUBIC loses.4375 ply (7 shallower,
9 equal), with55.2% of the nodes. At equal nodes, the depth differences shrink to
-.125 and0 respectively. This supports a real budget cost; it does not isolate
intrinsic evaluator latency because searched trees differ. Node-only runs have
no pregame warmup, so their first-root elapsed measurements include startup and
must not replace the warmed cache benchmarks. The clock runner warms each model
on four fixed-node roots before scored games.

Additional additive control: `wave3-control-shrinkage.json` applies the same fixed
half interpolation to the continued BRE's prediction change from Gen32, using
preserved second-half predictions and the independently checked WDL link. BT4 CE
changes.648588->.638566; CP.614427->.598227 (valid CP.613936->.597731).
Thus a shrinkage benefit alone is not evidence of expressiveness. Half residuals
still have lower diagnostic losses on these subsets, but chess value requires
the separate controls. This uses trainer-double Pair2 predictions, whereas native
Gen32 residual bases use compiled runtime weights; their tiny serialization
differences do not establish an implementation discrepancy.

The next fair control must give continued BRE the same fixed half-change option.
Research-only compiled interpolation is now implemented so this additive control
retains one ordinary Pair2 table and ordinary inference cost; composing two
separate evaluators would impose an artificial overhead. Original payloads and
optimizer states remain immutable. Eight correction-control tests passed.
`BrnAdditiveControlProbe` passed12360 actual-weight comparisons across preserved
positions in forward/reverse/repeated order: maximum raw error1.78e-15,
zero integer differences. Evidence: `raw/wave3/additive-half-oracle-01.json`.
Subsequent development uses new opening positions and compares both residuals
against continued BRE, with its half-change variant as a parallel additive control.
No independent-confirmation or next substantial-training gate is admitted yet.

## Training-order investigation

Current source inspection reveals an additional optimization caveat. GUI
`TrainerService.trainIncrementalCorpus` calls `IncrementalTraining`, whose first
epoch uses a512-record random mixer at batch128. Subsequent epochs permute2048-
record blocks and shuffle within each block. `SequentialTraining` acquires one
source's training quota, then its validation quota, before moving to the next
source. At equal weights and131072 training records, each source supplies65536,
an exact multiple of2048. Thus all replay minibatches in epochs2..8 contain one
source, although source blocks occur in shuffled order. This is current-code
inspection, not proof of the unavailable historical GUI executable identity.

This can create source-order sensitivity; it is not yet an established cause of
the W3 strength discrepancy. Larger generations alone do not change the2048-record
mixing block. The earlier optimizer probe globally shuffled each tiny single-source
subset, so it cannot diagnose this two-source interaction.

Completed bounded diagnostic: `wave3-training-order-plan.json`, through the actual
GUI incremental loop, after tournament work stopped. Twelve arms compared identical
records in original/reversed source order, alternating sources and a preshuffled
input; three actual W3 endpoints, LR.001,8epochs,b128,192updates each, no model
publication. First1536/source fit,last512/source diagnose generalization. This
small dataset differs from a full GUI tranche and remains development evidence.
All12 arms completed192 updates,24576 visits each, in one bounded low-priority
process; no models saved. `wave3-training-order-results.json` retains the compact
curves and raw binding. CUBIC CP heldout improved from.614969 to.601927..602928
across all four orders; BT4 changed.634609 to.634108..634432. BRE was more
order-sensitive (final BT4.639921..644703, CP.610246..617394). GATE changed little
on BT4 and improved CP modestly. There is no residual-specific ordering defect
established by these results, and no basis for a production shuffle change.
Lower-rate continuation remains plausible, not yet a required human gate.

A post-cache JFR probe of half-GATE (`raw/wave3/half-gate-profile-02.*`) passed
the existing position/search oracle. Of502 execution samples,151 included its
cached evaluator;95 top-frame samples map to the frozen-base evaluation call.
Inlining and mixed diagnostic/search phases prevent an exact cost attribution.
No further production optimization is admitted from this profile alone.

## Prospective fresh-development comparison

`wave3-fair-control-openings.pgn` selects32 new, unique16-ply roots from the
existing legal book, development partition0, seed2026102101, excluding all40
previously recorded BRN opening roots (1224 local receipts plus tracked plans).
Selection and exclusion manifests preserve hashes. No evaluator filtering.
The PGN is an intentional tracked input, with a narrow `.gitignore` exception.
Confirmation partition1 remains unused by this continuation.

Two fixed plans,192 games each:

- `wave3-fair-control-node-plan.json`: continued full BRE32 as reference,
  half-GATE32, half-CUBIC32 and compiled half-change BRE32 as candidates,
  32768 nodes/move.
- `wave3-longer-clock-plan.json`: frozen Gen32 as reference, the same three
  candidates,200ms/move. This investigates whether the short-clock result persists
  with a larger search budget; it is still a development screen, not confirmation.

Same32 reversed-colour pairs per candidate, common Policy4, one game and one
worker/engine, no pondering,64MiB TT,768MiB JVM,ActiveProcessorCount1,BelowNormal.
Each invocation remains at most12 games or240seconds, finishing the current game.
The longer clock increases elapsed duration, not concurrency or peak resource
limits. Freeze the separately hashed runtime, perform legal smoke, then complete
the fixed counts with independent replay. Preserve failures/caps; no optional
extension. Runtime and plans must not overwrite earlier screens.

Interpret both continued training and additive shrinkage controls alongside
expressive value and measured costs. Any later selected-candidate confirmation
requires a fresh protocol and multiplicity/selection accounting. Do not call these
development screens independent confirmation or automatically admit more training.

## Continuation preparation constraint

Read-only investigation of a possible later controlled-rate continuation found
that copying a completed W3 source ledger into a new lineage starting at Gen0
would collide with its generation1..32 reservations. Keeping the bootstrap counter
at32 avoids that collision, and `CheckpointStore.initialize`/`TrainingSession`
already support a nonzero parentless bootstrap, but `LineageProvenance.read` and
`CheckpointStore.writeProvenance` currently require Gen0. `LineageSummary` also
labels any nonzero checkpoint without a validation as unfinished.

Do not implement the earlier tentative "copy ledger and start at32" idea without
resolving and testing those boundaries. A future fork must preserve exact weights,
Adam moments, source offsets and historical completed reservations, distinguish
inherited exposure from new updates, and keep every existing store immutable.
Use the established checkpoint/library mechanisms; do not falsify old reservation
status/generation numbers or reset cursors. No fork, provenance relaxation or
human-training gate is admitted merely by this investigation.

## Bounded GATE cache experiment

The same-root depth penalty motivates a research-only cache experiment,
`wave3-gate-delta-plan.json`. It updates only changed piece terms and incident
pair experts, rebuilding both canonical perspectives after64 changed placements
or a jump changing more than8 squares. It does not change the learned function,
weights, optimizer, search rules or normal runtime routing.

The first prototype passed25143 forward/reverse/repeated comparisons against
full recomputation, including preserved game transitions and CP positions:
max raw error5.33e-15,zero integer differences. Eight balanced samples averaged
1032ns/transition for the existing cache and423ns for the prototype; first samples
were slower in both arms. Last three samples average about951ns vs377ns.
These are transition microbenchmarks, not full-search or strength gains.
`wave3-gate-delta-prototype-results.json` binds raw evidence, source and frozen
runtime under `raw/wave3/gate-delta-prototype-runtime/lib`.

Admit rounding-boundary/transition tests and exact-search comparison before any
activation. Periodic rebuilding alone is not a universal integer-rounding proof;
an explicit conservative rounding guard/fallback is required. Keep the earlier
fixed-order cache as an independent oracle and preserve negative outcomes.

The tournament admission helper now recognizes the local control sentinel
`build/research/brn-successor-batches.pause`. It stops before spawning the next
bounded process; it never interrupts a game or alters a receipt. Admission was
temporarily paused at a completed batch boundary for this bounded verification.
Remove only this owned sentinel when checks finish, then resume missing games
with fresh log prefixes and the unchanged fair-control runtime/plan hashes.
