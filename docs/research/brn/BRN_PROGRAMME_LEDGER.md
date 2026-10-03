# New BRN programme experiment ledger

Authoritative [contract](../../brn/BRN_CONTRACT.md) and
[frontier](../../brn/BRN_FRONTIER.md). This programme starts from first principles;
older reports remain evidence, not accepted candidates.

Current outcome: [BRN-3 V1 criteria satisfied](BRN_V1_RESULT.md). Historical entries
below retain their status at the time of each experiment; the final entry closes
the required programme work without claiming user acceptance or a finalized release.

## BOOT-001 — prerequisite recheck and challenged bootstrap — 2026-10-03

Outcome: acquisition blocker resolved; bootstrap complete. No new model has yet
been trained and no viability conclusion has been reached.

Starting HEAD `ab562e6` includes NNUE corpus integration (`1de635e`) and preparation
recovery corrections. The working tree additionally contains inherited sequential
Training Data readers, ledgers, service/GUI integration and documentation. These
pre-existing bytes are separately bound in `evidence/programme-2026-10-03/inherited.json`.
Their hashes were rechecked unchanged after prerequisite tests. No inherited source
was edited. Root CODEXLOG_CURRENT.md is absent; no journal is created.

The maintained finalizer/recovery hashes and maintained-source commit matched the
supplied binding. `begin` returned the inherited overlapping blocked operation
`9e5cb01570284814a3a7f45a498ae785`, uncertain mutation, no new operation and no
verified bump. Existing VERSION_STATE.txt remains build 33. Research and this
documentation proceed under DEFERRED_FINALIZATION and are separate later work;
the inherited operation is not finished/recovered/reassigned. Finalized version
provenance and dependent release actions remain blocked independently.

Prerequisite command (real-source environment variable enabled):

```powershell
$env:SEED_TRAINING_SMOKE_SOURCE = 'E:\SeedV6-Corpus\incoming\lichess\lichess_db_eval.jsonl.zst'
.\gradlew.bat :app:test -Pheadless --no-configuration-cache --tests 'com.ohinteractive.seedv6.training.data.SourceReadersTest' --tests 'com.ohinteractive.seedv6.training.data.SourceLedgerTest' --tests 'com.ohinteractive.seedv6.training.service.SequentialTrainingTest'
```

15/15 passed, zero failures/errors/skips, Gradle elapsed 69 seconds. Reader tests
cover plain/framed seeking, frame-split records, version identity and bounded
local reads. Ledger tests cover durable range reservations and mixtures. Service
tests cover both architectures with forbidden training generators, settled cursors,
exhaustion, exact NNUE optimizer continuation, crash replay and explicit restart.
Compilation tasks were up-to-date; these are actual newly executed runtime tests.

The source is 22,086,532,809 bytes. First and next 10000 records took .150/.150 s;
resume skipped 1808 nearby lines. Navigation metadata: five points, 584 bytes.
Production acquisition/target adaptation read 12500 records for 10000 training and
2500 validation in .215 s. These timings exclude optimization and are not throughput
guarantees. The service optimizer tests use synthetic fixtures, not this real subset.
No whole-source conversion/scan, normal training run, full suite, browser or GUI
launch occurred. Browser interaction is not needed for these Java/headless checks.

Inspection covered NNUE HalfKP 49152 features, 64x2 accumulators, hidden32 and
outcome/tanh semantics; BRN-1 global and BRN-2 local pooling; material/output
coupling; codecs and closed architecture enums; source/provider, service and
architecture-specific GUI configuration; and production search boundaries.
BRN-2 includes castling/EP/halfmove features forbidden by this contract. Its
1,645,665-parameter sum aggregation and tanh-combined prior are not adopted.
Historical material values were verified directly in Git at `c433256` rather than
using the later SEE table or importing the old bishop-pair heuristic.

Prior evidence: BASIC_V1 regression diagnostics show objective/scale mismatch and
attenuated material near tanh saturation; two NNUE raw-to-CP calibration studies
rejected universal mappings. Neither observation proves relational models fail.
The original BRN features/pooling, expensive generated data and current mature
Best are not a suitable fresh parity methodology.

Primary literature consulted as architecture evidence:

- [Santoro et al., Relation Networks](https://arxiv.org/abs/1706.01427): learned
  entity interactions motivate pair representations; benchmark success is not chess evidence.
- [Zaheer et al., Deep Sets](https://arxiv.org/abs/1703.06114): invariant set
  aggregation motivates pooling. Choice of normalization still needs empirical testing.
- [Stockfish NNUE documentation](https://official-stockfish.github.io/docs/nnue-pytorch-wiki/docs/nnue.html):
  sparse inputs and incremental inference motivate measuring latency alongside quality.
  This programme preserves SeedV6's own NNUE control rather than importing Stockfish.

The contract's challenge section records corrections for forbidden inputs,
CP/outcome semantics, population mismatch, leakage, symmetry, aggregation and
legacy-search diagnostic reuse. Next: DATA and inexpensive R0/CTRL experiments.

## DATA-001 and R0/R1/R2 screening — 2026-10-03

The prerequisite is satisfied through the new sequential source reader, including
real archive reads and both normal architecture lifecycles. All programme changes
remain separate verification/test sources and the canon; inherited production
changes are preserved. This is later work under DEFERRED_FINALIZATION.

`BrnResearchMain prepare` acquired a bounded common CP-eligible population directly
from the production source reader: 201260 records scanned, 37011 unsupported,
171 duplicate inputs, 238 excess partition records; 131072 train, 16384 validation,
16384 sealed test. No search-generated positions. Allowed-input canonical keys
deduplicate observations; SHA-256 geometry groups keep opposite perspectives and
file mirrors in the same partition. The manifest binds the original source and
all payload bytes. This prevents exact/geometry leakage, not all unknown game or
analysis correlations. Test records have not been used for metrics or tuning.

New candidates all implement fixed P/N/B/R/Q values 1/3.2/3.3/5/9, king 0, plus a
learned pawn-unit residual; zero heads/weights give exact material initialization.
Only permitted piece geometry and STM enter inference. All initial candidates
subtract shared opposite-perspective outputs, an extra odd-STM constraint later
ablated. Targets are the same CP labels and frozen Stockfish WDL adapter used by
the unchanged NNUE; NNUE output remains bounded outcome, never mislabelled CP.

- R0: 33168 additive unary and typed-displacement pair parameters, AdaGrad .03,
  fixed or count-normalized sum; CP Huber delta 2 or smooth WDL half-MSE plus .05
  Huber auxiliary. No nonlinear relation composition.
- R1: width 32 learned entity embeddings, all-pair product pooling via the
  sum-products identity, learned 32-wide tanh transform and zero residual head;
  approximately 26k parameters, sparse Adam .001. R1-stm retains color symmetry
  but uses only the current-side canonical output, allowing initiative.
- R2: width 16 piece-local tanh of node embeddings and normalized directed typed
  displacement messages, pooled zero residual head; approximately 531k parameters,
  sparse Adam .001. No forbidden BRN-2 status inputs or coupled material tanh.
- NNUE: repository initialization seed 71, unchanged trainer/Adam .001, batch 128.
  BRN screening uses online updates; exposure and optimization time are reported
  separately, not represented as identical optimizer opportunity.

Each micro run uses the same 8192 training positions, four shuffled epochs (32768
exposures), full frozen validation and seed 71. Selection is by validation outcome
loss, before any test use. Material baseline loss .212609, CP MAE 2.772359 pawns.

| Run | Selected epoch | Validation outcome half-MSE | Balanced half-MSE | CP MAE pawns | Optimization seconds, all epochs |
|---|---:|---:|---:|---:|---:|
| R0 Huber normalized | 2 | .195647 | .129777 | 2.697214 | .229 |
| R0 Huber fixed divisor | 2 | .193415 | .127651 | 2.690550 | .234 |
| R0 WDL + auxiliary | 4 | .185888 | .115467 | 2.689599 | .243 |
| R1 Huber | 2 | .203722 | .141234 | 2.673930 | .796 |
| R1 WDL + auxiliary | 1 | .192206 | .125875 | 2.674010 | .804 |
| R2 WDL + auxiliary | 1 | .188213 | .122307 | 2.678742 | 8.713 |
| R1 STM WDL + auxiliary | 2 | .193623 | .127742 | 2.663416 | see JSON |
| NNUE | 1 | .152160 | .055604 | not CP | 2.477 |

R2 training loss continues falling while validation worsens to .197209 by epoch 4.
Its added local nonlinearity does not justify scaling that implementation. Removing
forced STM antisymmetry does not close the micro gap; this is not evidence that
initiative is irrelevant, only that this ablation did not help this model/run.

To challenge whether the micro slice underrepresented the full population, short
R0, R1 and NNUE runs used all 131072 training positions, two epochs (262144 exposures).
All selected epoch 2:

| Run | Validation half-MSE | Balanced half-MSE | CP MAE pawns | Optimization seconds |
|---|---:|---:|---:|---:|
| R0 WDL + auxiliary | .165933 | .104341 | 2.613797 | 1.893 |
| R1 WDL + auxiliary | .136366 | .100565 | 2.418146 | 6.374 |
| NNUE | .074144 | .042498 | not CP | 53.187 |

Nonlinear global pooling benefits from data but remains far outside the declared
quality gate. Capacity/absolute relation geometry, minibatch optimization and
objective conditioning remain useful cheap hypotheses; no non-viability conclusion
is warranted. No serious training, sealed test, games or production integration yet.

Runtime software checks: R0 four, R1 three and R2 two focused tests passed, including
finite differences, exact material start, permitted-input invariants, color/rank
symmetry, serialization and exact optimizer continuation. The first R0 tiny-fit
test missed 1e-5 after 300 passes (error .000331); 1000 passes achieved the same
tolerance without changing the criterion. These are research checks, not GUI QA.

Initial diagnostic runs on R0/R1/NNUE micro checkpoints completed all 16 depth-3
production roots: 18653 / 20206 / 16179 nodes, respectively. Full-recompute warmed
latencies were approximately 4.89 / 3.51 / 2.54 microseconds. NNUE search uses its
existing incremental seam; BRNs use full recomputation. Production ExactSearch
has static leaves and zero qnodes: these results say nothing about qsearch.
Contract 1.1 corrects the bootstrap assumption and requires the existing opt-in
quiescence research seam separately. A diagnostic bug counted noncapture promotions
as quiet moves; the source now separates promotions, but old diagnostics JSON must
be rerun before those quiet-tail metrics are relied upon.

Reproduction: compiled verification classpath is
`app/build/classes/java/verification;app/build/classes/java/main;app/build/resources/main;app/build/install/seedv6/lib/*`.
Invoke `BrnResearchMain CANDIDATE DATA OUT COUNT EPOCHS SEED RATE NORMALIZED` with
Java 21, `-Xmx1g -Djava.awt.headless=true`; outputs must be new directories. The
frozen dataset is `app/build/research/brn-programme/data-v1`; checkpoint directories
are adjacent. Source and small JSON evidence are durable; ignored binary datasets
and checkpoints are local reproducible experiment artifacts, not shipped models.

## R3 geometry and objective conditioning — 2026-10-03

Hypothesis: compressed displacement / factorized embeddings discarded absolute
relation geometry. R3 instead embeds each king-square/entity-square pair (49152
rows x 32), with shared perspective pooling normalized by sqrt(piece count),
tanh pools, concatenated us/them perspectives, dense32 tanh, zero linear pawn
residual head. The king anchor uses permitted identity/location only. Its indexing
is intentionally related to HalfKP and is not presented as architectural novelty;
fixed material/output semantics and initialization differ. 1.58m parameters,
minibatch128 sparse Adam .003. New two-test sanity suite passed independent finite
differences, exact material, forbidden fields, color equivalence, three-position
overfit and byte-exact optimizer continuation after serialization.

At 8192 x 4, R3 WDL+.05 Huber selected epoch1 loss .197367; pure WDL .195762.
Removing the CP auxiliary does not solve the gap. A two-epoch full-sample probe
cost only 3.20 optimization seconds and selected loss .143959, CP MAE 2.440804.
This cheap probe was warranted by R1's observed data-scale sensitivity; no expensive
run was earned by the micro result alone. A convergence probe of 16 epochs took
26.744 optimization seconds / 38.921 elapsed, selected epoch16 loss .115529,
balanced .083656, CP MAE 2.236310; train loss .058207. More exposure improves it,
but it remains far behind the two-epoch NNUE (.074144), with a generalization gap.
This is an exposure/compute diagnostic, not a matched final comparison.

Objective challenge: outcome squared error has vanishing gradients for confidently
wrong initial material. Binary expected-score cross entropy through the same
smooth WDL link removes that failure, while keeping the runtime pawn output and
fixed material unchanged. Complement probabilities and log sums are evaluated
stably; finite differences across +/-1000 pawns, several material values and soft
targets passed. Confidently wrong values retain nonzero corrective gradients.
Cross entropy plus .05 Huber, with all other conditions preserved, yielded:

| Run | Micro selected loss | Short selected loss | Short balanced loss | Short CP MAE |
|---|---:|---:|---:|---:|
| R1 CE | .195599 | .136286 | .086031 | 2.560764 |
| R3 CE | .202833 | .139295 | .094814 | 2.459343 |

CE improves the short balanced subset but does not close overall parity. This
rules out saturated squared-error gradients as the sole cause. Neither compressed
nor anchored candidate is accepted. The next justified hypothesis is explicit
absolute all-pair geometry with node-local nonlinear composition, so potentially
important non-king relationships do not require reconstruction from global sums.

Corrected R1 short diagnostics (promotions separated) on 256 roots/6795 quiet
children: quiet absolute delta median .155154, p99 1.626072, max 4.187131 pawns,
none above nine. Capture/recapture maxima 9.609083/11.348315, which are not
automatically pathological. All 16 depth-3 production and opt-in qsearch roots
completed. These small samples are screening only, not final acceptance.

## R4 absolute all-pair family — 2026-10-03

R4 uses unordered pairs of the 768 permitted type/owner/square entities, with
294528 learned eight-channel edge embeddings. Each edge contributes to both
endpoint states, normalized by sqrt(n-1); learned node embeddings and nonlinear
local composition precede sum/sqrt(n) pooling. Us/them pools feed dense32 and a
zero pawn-residual head. Approximately 2.36m parameters, minibatch128 Adam .003.
This is a new research implementation; existing BRN/NNUE production code is unchanged.
Five focused tests now cover finite differences (base, one-perspective, rectified
and shared-relative variants), material/information/color invariants, tiny distinct
set fit, codec flags and exact optimizer continuation. The first tiny R4 test uses
.02-pawn tolerance on a combined objective, explicitly looser than R3's CP-heavy test.

| Variant | Micro selected loss (8192 x 4) | Short loss (131072 x 2) | Short CP MAE | Disposition |
|---|---:|---:|---:|---|
| Dual perspective tanh, CE+.05 Huber | .192265 | .097355 | 2.286185 | First materially promising all-pair model |
| Single perspective tanh | .200990 | .122601 | 2.399843 | Faster but loses quality; do not retain |
| Dual perspective rectified, CE+.05 Huber | .187049 | .098903 | 2.285692 | Similar quality, substantially faster |

Both dual variants complete all 16 depth-3 production and separate qsearch roots.
Tanh full recomputation 14.001 us, rectified 6.686 us, NNUE 2.557 us. Tanh fails the
preliminary throughput threshold; rectified clears it. Rectified quiet p99 2.898075,
max 5.251710 pawns, no quiet delta above nine; STM-reversal sum max 3.344318 is a
remaining diagnostic concern, not an exact symmetry failure (tempo is permitted).
Production nodes R4 rectified 21132 vs NNUE 21195; qnodes 118236 vs 78266.
These are small screening samples and do not establish practical game strength.

A staged objective (two CE epochs, then outcome squared-error, always .05 CP Huber)
addresses initial gradient starvation before optimizing the acceptance loss.
Eight-epoch rectified probe reaches ~.0797 validation loss by epoch6, at roughly
the optimization time of the existing two-epoch NNUE run. This is promising
compute evidence only: equal-exposure NNUE eight-epoch control is still required.
No criterion has been relaxed; no test labels or games have been used.

New sharing ablation adds .25 times a zero-initialized typed-displacement embedding
to each absolute edge, with independent learned absolute parameters retained.
This is a parameter-sharing/generalization hypothesis, not a handcrafted chess
feature. It can be algebraically folded into the absolute edge table for inference.
The finite-difference test passes; representative quality is being measured.

A diagnostic-only material-scale probe finds outcome loss .154797 at scale .2
versus .212609 at the required unit scale; zero output yields .158986. This means
material alone is overconfident for this selected teacher population, not that a
universal material conversion has been discovered. The fixed prior remains exactly
unchanged. Learned relationships must supply the needed context and compensation;
no trained global material coefficient is substituted. Source-order/generalization
limits still apply, and larger disjoint replication is required for acceptance.

Equal-opportunity result: the fresh seed-71 NNUE eight-epoch run selects epoch8,
validation loss .061266433, balanced loss recorded in JSON, 224.251 optimization
seconds. R4 relative-sharing staged objective selects epoch7 loss .075015181,
CP MAE 2.030205, 73.691 optimization seconds over eight epochs. Both saw 1048576
examples from the same 131072 distinct positions. The roughly 22% loss gap fails
the predeclared 10% parity tolerance. Better speed of training is retained evidence,
not a reason to substitute the earlier weaker NNUE checkpoint. No sealed test use.

The comparison utility verifies matching dataset identities and computes paired
geometry-group bootstrap uncertainty, material/balanced strata and calibration.
R4 shared contributions are now algebraically folded for inference; a numerical
test verifies agreement and the trainer state remains separate. Historical research
checkpoint formats remain readable. Current investigation is stronger relative
sharing, fine-tuning step size and potentially width/data scale; the family is
promising enough to concentrate research, but no usable V1 integration is claimed.

Calibration comparison confirms the gap is not merely wrong signs: on 8198 labels
with |CP|>=50, R4 has 982 wrong signs and NNUE 994. R4 calibration error is .087692
versus .039305 in bounded outcome units. Overall paired loss ratio 1.224409 has
geometry-group bootstrap 95% interval [1.176581,1.275732] (2000 replicates, seed8017).
Near-balanced ratio is 1.547778; by absolute material buckets under .5 / .5-2 /
2-5 / >=5, ratios are 1.1013 / 1.2385 / 1.3074 / 1.4574. These are validation
diagnostics, not sealed-test estimates. Stronger relative sharing (scale1) improves
selected loss to .073210 but still fails parity. Dropping the .05 CP auxiliary
after the two CE warmup epochs, keeping the .25 sharing scale and .003 learning
rate, improves selected loss to .072315 at epoch7; CP MAE 2.166180 still improves
material-only 2.772359. The auxiliary was one contributor, not the whole cause.

Width is now explicit in the research codec (old states decode as width8).
Width16 passed finite differences, material start and codec agreement. Its micro
probe and a matched eight-epoch pure-fine-tune probe test whether local relation
capacity is the remaining limitation. The acceptance thresholds remain unchanged.
All 50 inherited paths were rehashed unchanged during this work; version remains33.

Width16 selected loss .071561 at epoch6 (eight-epoch opportunity), compared with
width8 pure fine-tune .072315; 143.663 versus 75.546 optimization seconds. That
small improvement does not currently justify doubled capacity/cost. Retain width8.

DATA-002 expands the common bounded population to 524288 training / 32768
validation / 32768 sealed test. 799907 raw records examined, 143281 unsupported,
1371 duplicate inputs, 65431 excess partition observations. The same global hash
split keeps all previously held-out groups excluded from training; DATA-001 is
preserved. Manifest/payload hashes are in `evidence/programme-2026-10-03/data-v2-manifest.json`.
The larger representative experiment is justified by the substantial training /
validation gap and near-control sign accuracy, after correct gradients, usable
search and preliminary speed. It is bounded at four fresh epochs for both models.
R4 uses width8, relative scale1, Adam .003, 262144 CE+.05-Huber warmup exposures,
then pure outcome squared error; fixed warmup exposure avoids unintentionally
quadrupling warmup when enlarging the dataset. NNUE remains unchanged. No games
or test unsealing are earned yet. Seed97 is reserved now for independent replication,
using a later raw source range if the family earns final comparison.

Hardware for these local measurements: AMD Ryzen 5 5500, six physical / twelve
logical cores, Java21, standalone JVM heap limit1GiB. Timings are observed local
optimization/elapsed times, not claims of dedicated-machine isolation. Available
C: space was approximately288GB before expanding the sample.

DATA-002 matched four-epoch results (2097152 exposures each): R4 selected epoch4,
loss .080340552, CP MAE 2.341080, 180.255 optimization seconds; NNUE selected epoch4,
loss .072978219, 616.777 optimization seconds. Ratio 1.100884 narrowly misses the
1.10 point criterion; group-bootstrap 95% interval [1.073440,1.129913] clears the
separate 1.20 uncertainty ceiling. Do not round the point ratio into a pass.
Wrong signs on |CP|>=50: R4 2129, NNUE 2130 of16731. Calibration errors .073240 /
.034547 remain different. More distinct data narrows the gap; no test labels used.

Inference work: a rolling placement cache maintains the same folded node relation
sums by subtracting/adding edges incident on changed entities, rebuilding for count
changes or distant placements. It reads only placement and STM, and is not a
history feature. Equivalence tests passed 2000 legal plies, siblings, castling,
en-passant, promotion and rule/hash changes, raw tolerance1e-9 and identical integer
scores. On the DATA-001 pure-fine-tune checkpoint, full/cached 16-root depth3 runs
had identical 22208 production nodes and 99638 qnodes; observed production times
.2603/.1920 seconds. Cache on unrelated positions is slower (15.176 vs9.822 us),
so this is a search-local optimization, not a claim of faster unrelated inference.
A float32-weight variant passed a separate 1000-ply 1e-4-pawn error-budget test;
its trained-model search/performance evidence is pending, so it is not adopted.

Optimizer audit: existing NNUE uses conventional Adam, including decay and updates
for historical moment rows absent from a batch. R4's earlier masked sparse variant
freezes those coordinates. A selectable conventional double-moment Adam ablation
now has a zero-gradient history test and exact save/resume test; no NNUE code changes.
Pure-CE micro loss without CP auxiliary is .181034 (masked) versus .180775
(conventional), optimization times2.712/7.794 seconds over32768 exposures. This
micro effect is small and does not establish that optimizer semantics explain the
remaining gap. Pure CE on the larger population remains a possible objective probe.

The equal-time match harness uses the existing production driver, HeadlessGame
rules and deterministic legal opening generator. It records reversed-color pairs,
actual time/nodes/depth, full moves and terminal outcomes. Caps/failures are never
draws; two statistics tests confirm incomplete-pair exclusion and both-color
accounting. No match has yet been run. Acceptance still needs100 completed pairs,
not a small pilot. No production integration or accepted new architecture exists.

DATA-002 128 fixed validation roots at depth4 (none terminal), production driver,
4MiB TT, 2-million-node / 2-second guards: both models complete all128. R4 cached
738858 nodes versus NNUE731267 (ratio1.010381), maximum per-root ratio2.5873,
no root above10x. Aggregate elapsed2.8966 versus1.1653 seconds. Unrelated-position
evaluation9.696 versus2.659 us, throughput ratio.2742; the rolling cache is not
an advantage for unrelated inputs. BRN quiet p99/max2.5043/4.0865 pawns, none
above9. These results satisfy the production node/completion gate for this seed,
not practical-play acceptance.

Separate unpruned research qsearch completes126/128 BRN versus128/128 NNUE.
BRN indices12 and127 time out at2seconds (315658/344622 entered nodes); NNUE
completes those at126995/420060 nodes and .3034/.9909 seconds. Across all roots,
including censored BRN tails, entered-node ratio1.2707 and elapsed40.292/13.625
seconds. These are BRN-specific deadline failures in the diagnostic, not production
failures; preserve and investigate rather than claiming qsearch stability. No
production qsearch is enabled. A pure-CE/no-CP-auxiliary representative DATA-001
eight-epoch run now tests ongoing correction gradients before further scaling.

Pure cross entropy throughout, no CP auxiliary, relative scale1, width8 and masked
Adam .003 selects DATA-001 epoch8 at .067765307 (81.307 optimization seconds).
This improves on staged pure-fine-tune .072315 and share1 staged .073210;
the unchanged eight-epoch NNUE remains .061266433. It earns a fresh DATA-002
four-epoch run with the same pure-CE configuration. Test labels remain unopened.

Pilot play of the existing DATA-002 hybrid checkpoint: four reversed-color opening
pairs at25ms/move, cap512, all eight games end by checkmate, BRN8 wins/0 draws/0
losses. Played plies28..109; per-game mean completed depth averaged10.26 BRN /
10.50 NNUE. Actual aggregate search time4.605/4.549 seconds and nodes1.657/3.359
million. This small pilot supports further testing but does not meet the100-pair
gate or estimate broad playing strength. Full pair/move evidence is preserved.

Follow-up of the two quiescence tails retains the original failures and uses a
2-million-node / 10-second diagnostic guard. Both finish: index12 at460134 nodes,
3.103 seconds, maxqply25; index127 at717300 nodes,4.189 seconds,maxqply27.
Relative to the original complete NNUE runs, node ratios3.6233/1.7076; this is
bounded tail growth plus latency, not an observed nonterminating explosion.
The extra time is diagnostic evidence, not a retrospective change to the first run.

Sampling audit: earlier manifests counted duplicate inputs but did not quantify
their label disagreements. They remain immutable with that limitation. Future
prepared datasets retain the same first-observation policy and additionally report
duplicate disagreement count, mean/max absolute CP disagreement. This does not
change the model's permitted information or reuse sealed test metrics.

Pure CE did not carry its DATA-001 advantage to the larger four-epoch opportunity:
DATA-002 epoch4 .081110709,184.180 optimization seconds, versus hybrid .080340552.
Retain both curves; no claim that pure CE has solved calibration. A specific remaining
capacity mechanism is the eight-channel all-node pool: ownership/type identities
are represented in edge inputs but erased by summing every activated node into
the same eight coordinates per perspective. A piece-identity pool ablation retains
12 groups of eight before the same32-unit readout, adding only5632 dense weights.
It introduces no attacks, roles or chess positional features; the groups are the
already permitted relative piece/owner identities. Keep relation normalization and
fixed material unchanged. Finite differences, color equivalence, folded/cache
agreement and exact optimizer continuation pass; a micro run precedes scaling.

Research codec version8 records the pooling choice and reads versions1..7 with
their original semantics. Sampling partition tests cover500 legal-walk positions
and their file/color/rank/STM/rule-field variants. Eleven targeted tests across
R4, its cache and data splitting pass after this change; production code is unchanged.

Identity pooling micro best .18043145 (epoch3), versus untyped pure CE .18103352.
The 131072-position two-epoch probe yields .08299921 versus untyped .08525078
and unchanged NNUE .07414424. This modest consistent gain earns an eight-epoch
comparison. A separate three-position pure-CE tiny-overfit test passes from exact
material initialization, including a target that reverses the material sign.

Sampling review found that DATA-002's32768 validation/test quotas fill earlier in
the source than its524288 training quota. Its geometry partition has no train/test
leakage, but held-out membership is source-prefix-biased; this limits population
claims. DATA-001 quotas approximately match the natural8:1:1 partition ratio.
For subsequent replication a new `prepare-uniform` mode uses Algorithm R reservoirs
with fixed independent seeds73103/73104 for validation/test across the entire
examined raw range. It preserves the global geometry partition and first duplicate
policy, records selected ordinal coverage and seek work, and never changes earlier
datasets. Reservoir admission depends only on occurrence order and RNG, never labels.
This is bounded experimental sampling through SourceReaders, not a corpus migration.

Identity pooling eight-epoch DATA-001 result selects epoch8: .066350366 versus
NNUE .061266433, paired ratio1.082981, group-bootstrap95%[1.040710,1.127072].
BRN balanced loss .047775405, CP MAE2.234520 versus material2.772359, residual
RMS1.805393; signs1026/8198 wrong versus NNUE994. Training108.084 versus224.251
optimization seconds. Parameters2627777 including training-only relative table.
This passes the first-seed validation tolerances, not final acceptance.

The trained float32 folded snapshot differs from double by at most3.775e-7 pawn
on16384 validation positions, no integer score differences, below the declared
1e-4 budget. Initial cached typed inference14.593us fails the speed gate versus
NNUE2.620us. Constant-width kernels alone13.598us; four independent dense sums
10.539us; occupancy traversal10.958us; eight sums11.018us and worse cold search;
moving relation normalization from every edge to every node10.340us. All these
16-root depth3 variants retain22434 production nodes and90693 research qsearch
nodes. The retained node-scale variant uses .1800 production seconds versus
NNUE .1136. Skipping zero pooled coordinates regresses to13.243us and is rejected.
These are observed sequential timings with limited margin, not a robust final
throughput conclusion. Final paired latency and integrated search remain required.

Prospective replication recipe, fixed before examining seed97 metrics: R4 typed
identity pooling, width8, dual perspectives, ReLU local/readout, hidden32, absolute
unordered pair embeddings plus relative training table at scale1, exact fixed
material, zero initial residual. Masked Adam .003/.9/.999/1e-8, batch128, pure
cross entropy against STOCKFISH_WDL_V1 expected score, no CP auxiliary, eight fresh
epochs over131072 distinct examples; minimum validation half-MSE selects checkpoint.
Seed71 uses immutable DATA-001; seed97 uses uniform-held-out DATA-003 beginning
at raw ordinal799907, with16384 validation and16384 still-sealed test. The same
fresh NNUE recipe remains unchanged. Float32 folded inference is retained subject
to the numerical budget and final speed/search checks. No test metrics are opened.

DATA-003 reads raw[799907,997962),198055 records,1187 seek records,33197 unsupported,
69 duplicate inputs and949 surplus partition observations.55 duplicate labels differ,
mean absolute duplicate difference19.522 CP,max276 CP. Both held-out reservoirs span
nearly the full examined range. Counts131072/16384/16384 and all hashes are preserved
in data-v3-replication-manifest.json. Fresh NNUE seed97 completes eight epochs and
selects epoch8 loss .07094772; the fixed BRN recipe is now running.

Production-core implementation begins under DEFERRED_FINALIZATION, separately from
the inherited blocked version operation. Before these new files, all50 inherited
paths still match starting hashes; version33 and both maintained tool hashes are
unchanged, root journal remains absent. New core/brn3 files implement fixed typed
R4 layout, independent material/features, immutable folded float32 model, worker-local
cache, CE supervision adapter, exact double trainer and independent CRC-protected
model/training codecs. No shared production or NNUE files have been edited yet.
Two targeted core tests pass: all2627777 parameters and both moments match the
research implementation bit-for-bit after six minibatches and after save/resume;
2000 legal-walk predictions agree within1e-4 pawn. Material initialization, forbidden
fields, model immutability, size/checksum/truncation/trailing-byte rejection and
symmetric pawn-to-engine mapping pass. This is provisional integration, not V1 acceptance.

Seed97 BRN selects epoch6 of eight opportunities, validation .07685841 versus
NNUE epoch8 .07094772. Both trained131072 distinct positions,1048576 exposures.
The prospective recipe is unchanged. Shared model/search/checkpoint/corpus boundaries
now admit BRN-3 with its own CP-only frozen-WDL adapter; old NNUE target behavior is
unchanged. Normal BRN-3 explicitly requires Training Data, with no generated fallback.
The original50-path baseline is preserved byte-for-byte in inherited-baseline.zip
before any inherited file edits, supplementing the initial hash manifest.

First sealed-test evaluation of the fixed production float32 models:32768 examples,
two source ranges/seeds. Pooled half-MSE BRN .069986193, NNUE .064947225, ratio1.077586;
paired geometry-group bootstrap95%[1.046175,1.107427]. Material loss .223076097,
BRN CP MAE2.248334 pawns; all declared pooled quality gates pass. Individual ratios
are1.025190(seed71) and1.124294(seed97), so seed/range variation remains visible.
Wrong signs on|CP|>=50:2163 versus2027 of17284; residualRMS1.988288 pawns. Per-seed
calibration, phase/material strata, CP/outcome error distributions and initial-NNUE
test controls are in brn3-final-sealed-test.json. Test labels may not tune this model.

Production BRN-3 seed71 completes all128 fixed validation roots at depth4,760418
nodes; research qsearch127/128, index127 deadline tail. Warmed unrelated inference
10.033us in this run. Fresh eight-epoch NNUE128-root control and seed97 search
replication follow. Final game plan is fixed before results:100 reversed-color
opening pairs per seed at25ms/move,512-ply administrative cap, same legal-uniform
opening generator, start index1000 (separate from pilot openings). All incomplete
games remain explicit failures of sample completeness, never draws. Each seed's
point score and paired one-sided95% lower bound are reported separately.

## INTEGRATE-002 — ordinary service, final runtime and search — 2026-10-03

BRN-3 now has its own architecture/schema, immutable model and exact optimizer
formats, production SearchEvaluation state, CP-only target adapter, checkpoint
loading/validation, ordinary corpus minibatches and architecture-specific GUI card.
The existing NNUE feature kernels, optimizer, output mapping and search semantics
are unchanged. Four inherited files have bounded integration additions; the other
46 inherited paths, including two already absent tests, remain unchanged.
`inherited-attribution.json` and `shared-integration.diff` separate these additions
from the inherited corpus migration. The initial archive is preserved.

Integration command selected Brn3CorpusTrainingTest, Brn3GuiTest,
NetworkArchitectureTest, TrainingSettingsValidationTest, NnueCorpusTrainingTest,
SequentialTrainingTest and TrainingDataLayoutTest:26 passed,0 failed,1 skipped
(the environment-dependent real-source acquisition smoke previously passed in
BOOT-001). The service test proves real updates, distinct model payloads,
CP-only perspective normalization, zero generation, exact optimizer-boundary
stop/resume and unchanged source reservation. A further15 tests passed across
the themed GUI, NNUE evaluation-state/search boundaries and production/research
BRN parity. Full repository suite was deliberately not run.

The headless ordinary-service real-source scratch smoke used10000 positions,
2500 held-out,seed71,minibatch128,two epochs. Acquisition examined15660 raw records,
skipped3160,produced20000 exposures and158 optimizer updates,generated0 games.
Training half-MSE .193192626→.074845491; held-out .207770845→.148060517;
isolated Candidate promoted. Deterministic selection replay matched both hashes.
This run uses new initialization and TrainerService, not a supplied research state.
Its report is `brn3-service-smoke.stdout.txt`; existing Best stores are untouched.

Final production depth4 searches: seed71 BRN760418/NNUE838917 nodes (ratio .906428),
seed97 BRN909455/NNUE934479 (.973221). All256 roots complete for each architecture,
zero BRN roots exceed10x control, maximum ratios2.7024/2.7035. Aggregate elapsed
BRN3.6756/3.9925s versusNNUE1.3046/1.5205s, including per-root construction and cold
effects. Production qsearch remains disabled.

Separate unpruned, TT-free research qsearch at2s has BRN1/4 deadline tails and
NNUE2/1 for seeds71/97. Every union-of-failures root completes under the same
2million-node/10s follow-up guard. Retaining original failures, aggregate complete
qsearch node ratios are .51842/.85334. Maximum root ratios5.499/17.942; seed97 has
two of128 above10x. Its worst tail (index21) takes838203 versus46717 nodes and
5.357 versus .180s; maximum qply24 versus21. Another tail is381312 versus50527
nodes. These are significant bounded tactical tree differences, not proof of
universal qsearch safety. They are an explicit limitation before any future
production-qsearch integration; no search semantics were changed to hide them.

No nonfinite local delta or sampled queen-sized discontinuity occurs. Final
seed71/97 quiet p99 is2.2951/1.9067 pawns, maxima3.7740/4.2004; capture p99
2.3997/1.8856, maxima3.1439/2.6087. Full recapture, promotion, STM and geometry-only
probe distributions remain in the final-search reports. STM reversal is not
forced to negate a score because the model may learn a mover advantage; color/rank
equivalence is tested separately.

Nine alternating warmed latency rounds per seed give BRN medians9.380/9.810us,
NNUE2.714/2.660us,paired median throughput ratios .291319/.269187. Both exceed
the predeclared .25 gate. Final float/cache error across32768 validation records
is at most3.775e-7 pawn,zero integer-score differences,zero normal-band saturations.
The model payload is9474352 bytes,training state63066732,trainer large primitive
arrays106289192 excluding JVM/headers/corpus/snapshot temporaries. NNUE payloads
are12599889/37799553. Per-worker BRN scratch is approximately12KiB of primitives.
The runtime reports also measure parent/quiet-child and capture/rebuild sequences.
Training-only time for eight epochs is108.084/129.117s versusNNUE224.251/241.375s;
complete research runs including evaluation/serialization139.726/161.124s versus
231.003/248.962s. These are observed single-machine measurements, not guarantees.

The full100-pair seed71 match completes:198wins,0draws,2losses,point score .99,
paired one-sided95% lower .98. All200 games end in checkmate; no capped/failed
games. Seed97 full replication is running. Test labels and the model recipe remain
frozen. No V1 completion claim is made until all final practical checks finish.

## V1-RESULT — criteria satisfied — 2026-10-03

Both predeclared final matches complete all100 opening pairs: each seed198wins,
0draws,2losses,point score .99,paired one-sided95% lower .98. Every game ends by
checkmate; no caps, search failures or excluded pairs. The same100 openings are
reused across seeds, so400 games are not independent observations. Supplemental
bounded-pair Hoeffding lower .867613 per seed also clears the practical gate.
BRN game NPS275956/278738 versusNNUE697638/702758; mean per-game average completed
depth9.268/9.272 versus9.646/9.842. Both core model hashes differ across seeds and
are bound in raw match reports and brn3-final-experiments.json.

Final26/26 targeted tests pass: actual GUI backend creates/trains/reopens/resumes
BRN-3, architecture/lineage/folder selection works, existing BRN preference identity
tests admit the new architecture, and trained nonzero residuals ignore forbidden
fields and preserve color/rank/STM equivalence across the2000-ply parity walk.
The first switching test failed because its newly added BRN-3 fixture lacked a
registered Training Data source. The application correctly rejected that save;
the fixture was supplied a source and awaited its load, then all26 passed. No
generator fallback or weakened application guard was introduced. Temporary diagnostic
printing was removed. Failure and passing XML are retained for truthful reconciliation.

The operator guide's links and PowerShell command syntax validate. git diff --check
passes. NNUE core/optimizer source is unchanged; its inherited NnueCorpusTargets
adapter change remains byte-identical to the initial baseline. Actual NNUE service,
state and production-search regression checks passed earlier in this same work unit.
The full suite and manual desktop/browser sessions were not run; headless Swing
components, themed render and actual GUI backend/service lifecycle were verified.

Outcome: V1 SUCCESS within the predeclared scope. The retained architecture, recipe,
all final measurements, software evidence, limits and optional future research are
in BRN_V1_RESULT.md and the operator guide. No further architecture tuning is needed
for this work unit; opened tests cannot guide future tuning. Mature controls,
longer games, broader/game-disjoint data, balanced-position calibration, faster
inference and qsearch-tail analysis remain optional separate work.

Final provenance: HEAD remains ab562e62815f955efbaf95063a877372aa2d9955, no commit,
push or deployment. All50 inherited paths accounted for,46 unchanged and four
bounded shared-file additions with original bytes archived. Source-attribution
hashes supplement the review diff without claiming finalized build provenance.
Root journal is absent. VERSION_STATE.txt remains33; maintained tools still match
the supplied binding. A final read-only status confirms the inherited token remains
blocked/uncertain with verified_bump=false. No operation was recovered, reused or
silently finished. Separate human resolution/authority remains blocking only for
authoritative version/build/release provenance; local V1 training and research work
is complete. User acceptance remains a separate reconciliation decision.
