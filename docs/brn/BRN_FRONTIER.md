# BRN research frontier

This is the completed architecture-viability programme. The subsequent completed
learning-strength investigation is tracked in [BRN_LEARNING_FRONTIER.md](BRN_LEARNING_FRONTIER.md)
under its own [contract](BRN_LEARNING_CONTRACT.md); its evidence does not erase V1.

Master contract: [BRN_CONTRACT.md](BRN_CONTRACT.md). Status: **V1 criteria satisfied**;
BRN-3 is retained. [Result report](../research/brn/BRN_V1_RESULT.md). User acceptance
and finalized build/release provenance remain separate. Last updated 2026-10-03
(Pacific/Auckland).

| Priority / ID | Question or work | Status / dependency |
|---|---|---|
| Done / DATA | Two final ranges, each 131072 train / 16384 validation / 16384 sealed test, geometry-group split, payload SHA verified | Final tests opened once for frozen recipe; DATA-002 exploratory test remains unopened |
| Superseded / R0 | Additive typed-displacement potentials; normalization and objective ablations | Learns but short validation loss .16593 versus NNUE .07414 |
| Done / CTRL | Fresh unchanged NNUE controls, seeds71 and97 | Same data/exposure per seed; eight epochs each; genuine learning confirmed |
| Retained baseline / R1 | Factorized pair interactions and nonlinear global pooling | Short loss .13637; removing forced odd STM symmetry did not improve micro loss |
| Rejected at micro / R2 | Piece-local nonlinear typed-displacement messages | Micro loss .18821, overfits by epoch 4, more costly than R1 |
| Screened / R3 | Absolute king/entity relations, minibatch128, nonlinear us/them pool | Short .14396; 16 epochs .11553; insufficient versus two-epoch NNUE .07414 |
| Promising / R4 | Explicit absolute all-pair embeddings and node-local nonlinear composition | CE two-epoch loss .09736; rectified variant .09890 at 6.69 us; hybrid objective ~.0797 at 6 epochs |
| Screened / R4-sharing | Add shared typed-displacement contribution to absolute edges to improve generalization | .25 share scale loss .07502; scale1 .07321; folded inference verified |
| Done / CTRL-8 | Equal-exposure eight-epoch fresh NNUE control | NNUE .06127, R4 .07502: parity fails; grouped ratio CI [1.1766,1.2757] |
| Screened / R4-calibration | Remove CP auxiliary after CE warmup; study capacity versus overconfidence | Width8 .07232; width16 .07156 at ~2x cost, retain width8 |
| Done / DATA-SCALE | Larger common data and separate-range replication | DATA-002: R4 .08034055 versus NNUE .07297822, ratio1.100884 narrowly misses point gate; bootstrap upper1.129913; test sealed |
| Screened / R4-objective | Separate CP auxiliary bias from ongoing confidently-wrong gradient starvation | Pure CE improves DATA-001 to .0677653 but DATA-002 .0811107 is worse than staged .0803406; conventional Adam micro small gain at 2.9x cost |
| Retained / R4-pooling | Avoid erasing entity identity at the eight-channel global bottleneck | Final pooled sealed-test ratio1.077586,95% upper1.107427; both quality gates pass; individual ratios1.02519/1.12429 |
| Done / INFERENCE | Rolling geometry cache, typed pools, float32 folded immutable weights | Core/research exact training parity;2000-ply cache test;32768 validation predictions within3.78e-7 pawn, zero integer-score differences |
| Screened / OPTIMIZER | Pure WDL versus CP auxiliary; cross entropy to avoid confident-material gradient starvation | R1/R3 CE short .13629/.13930; no parity, not sole explanation |
| Done / STABILITY | Final local deltas, production search and separate opt-in qsearch research |256/256 production roots complete; node ratios .9064/.9732, no >10x root; all diagnostic deadline tails complete under10s guard; seed97 qsearch maximum node ratio17.94 remains a limitation |
| Done / PLAY |100 paired openings per seed,25ms/move,512-ply cap, production models | Each seed198wins/0draws/2losses,score.99,paired lower.98;400complete games,zero failures/caps;shared openings across seeds |
| Done / COMPUTE | Alternating warmed latency and memory/update measurements | Median throughput .2913/.2692 of NNUE; gate .25 passes; BRN model9.47MB, trainer large primitive arrays106.29MB |
| Done / SCALE | Fixed versus count-normalized R0, CP Huber versus WDL plus CP auxiliary | Outcome objective helps; normalization alone does not close gap |
| Done / INTEGRATE | Codecs, search state, ordinary TrainerService, architecture configuration and GUI | Exact service resume and NNUE boundaries pass; real-source10000-position scratch run trains20000exposures,158updates, promotes on2500held-out; no generated games; rendered GUI verified |
| Criteria met / ACCEPT | All contract gates and coherent durable completion | V1 engineering success; operator guide BRN_V1.md;user reconciliation remains separate |
| Optional / QUALITY | Improve balanced-position loss and CP tail calibration | Requires new validation evidence; current sealed tests cannot select changes |
| Optional / GENERALIZE | Broader corpora,game-disjoint evidence,longer games,mature controls | Current conclusion is conditional on the tested source ranges and short time control |
| Optional / PERFORMANCE | Faster inference and deeper qsearch-tail investigation | Keep information boundary and NNUE behavior;production qsearch remains outside this V1 |

Preserve the inherited uncommitted sequential Training Data implementation; do not
absorb it into programme commits or rewrite its acquisition semantics. The old
version operation remains reserved (`DEFERRED_FINALIZATION`); this does not block
independent research. Research-only candidate checkpoints exist under app/build;
the isolated BRN-3 service smoke has its own Best, and existing user Best stores
remain untouched. Finalized build/release provenance is still blocked independently.

Future work starts by checking this frontier and the [ledger](../research/brn/BRN_PROGRAMME_LEDGER.md),
verifying source/baseline identities, then selecting an explicitly requested optional unit.
Do not replay the old expensive position-generation programme or treat old BRN
measurements as evidence for a new architecture.
