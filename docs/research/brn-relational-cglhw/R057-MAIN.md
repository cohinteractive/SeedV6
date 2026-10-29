# R057 principal training and evaluation freeze

Prospective after the three seed71 pilots and selected-capacity seed97 replication,
before any principal training. This is the one authorized full-scale attempt.
No selected pilot checkpoint initializes the main run.

Selected ONE: width16, rank4,31,489 active/stored double parameters. The frozen
selection rule chose it because its loss is within1% of width32/rank8 and its
measured lifecycle cost is lower. Width8 misses that loss band narrowly; this
threshold-sensitive choice is a limitation, not a strong capacity ranking.
Seed97 reproduces width16 development loss closely. The width16 pilot did not
pass CP64-like latency/NPS criteria; useful full-scale learning remains a separate
authorized question. No inference parity or playing-strength claim is made.

Principal sample manifest: r057-training-corpus.json.33,554,432 samples per epoch,
33,180,466 canonical unique positions,373,966 duplicate samples. Eight complete
bijective visits produce268,435,456 exposures. Sample count is not unique count.
Data seed20261029059, initialization seed20261029, batch128. The frozen epoch
rates are[.01,.01,.007,.005,.003,.002,.001,.0005]. Adam moments, weight decay,
clipping, Gaussian initialization and target math remain those documented in
R057-CAPACITY.md. No validation-selected epoch, rate change, second full run or
automatic continuation beyond eight epochs. Preserve every epoch metric and
checkpoint, including regressions. Numerical invalidity or resource stops retain
their explicit gates; weak prediction alone is not an early-stopping rule.

Checkpoint every262,144 exposures, estimated2-3seconds from pilot throughput;
at most32 checkpoints per supervised invocation. State, moments, exact cursor,
config and receipt-chain hashes survive each boundary. Estimated40-50minutes
wall time with one logical CPU,512MiB heap plus1GiB mapped input; about0.8GiB
checkpoint storage. These are estimates, not resource guarantees. Use the same
existing supervisor, fresh admission and active thresholds, no GPU. Stop execution
on a resource gate, preserving the latest completed receipt; do not retry until
explicit RESUME. Main counts exclude all8,388,608 preliminary pilot exposures.

## Frozen evaluation

Use the epoch8 endpoint only for principal comparisons. Before playing, verify
its full/incremental numerical equivalence, legal/deterministic special-position
search probes and state integrity. Run the same30-root rotating lifecycle and
whole-search harness with vector CP64, reporting all rounds, allocations, memory,
initialization and cost ratios. Quantized CP64 versus double ONE remains a
precision/methodology difference. Repeat measurement on the actual final model;
pilot timing is not its final result.

Predict both models on the8192 original-validation development sample and the
8192 fresh reserved BT4 positions. Compare squared sigmoid loss against original
BT4 labels, phase buckets, paired per-position differences and unary-only ONE
ablation. Preserve per-position predictions locally. Report source-game identity
unknown and descriptive iid error bars as such. Fresh data is not used to choose
capacity, optimizer, epoch or another run. Predictive loss does not establish play.

Fixed-node development screen:32 reserved opening roots, both colours,64 games,
20,000 whole-request nodes including qsearch, one active game and one worker per
engine, TT16MiB, common flags262143/ADAPTIVE16. No clocks, pondering, book or
tablebases. Both engine processes may be resident but only the side to move searches.
640-ply caps and administrative watchdog failures are censored, never draws or
clock losses. Preserve full moves, request receipts and PGNs; independently replay.
No model/search adjustment in response to the screen.

If the uncensored screen score is at least40% with no correctness failure, run
the frozen independent256-root confirmation panel, both colours,512 games with
the identical node/settings budget. Otherwise report the complete negative screen
and do not spend the confirmation allocation. A64-game selected development
screen cannot establish parity. Both allocations and seed20261029060 were fixed
before principal training; r057-openings-selection.json pins source and exclusions.
No extensions to chase significance and no additional architectures.

Report W/D/L, caps and all-game score bounds, opening-pair bootstrap95% intervals
(20,000 resamples, seed20261029061), and distribution-free Hoeffding bounds over
the256 independent opening-pair means where its assumptions are applicable.
The maximum Bernoulli-style normal95% half-width at256 bounded pair means is
about6.1percentage points; actual pairing/draw variance may be smaller. Related
opening families can reduce effective independence. Competitiveness requires
fresh evidence; a noninferiority claim requires95% lower score>=45%, and must
state which uncertainty construction supports it. No bootstrap-only certainty
when the conservative bound or dependence limitation leaves the result unresolved.

CP64 production flags3407871 additionally use NOMINAL_FUTILITY and VERIFIED_NULL;
these are unchanged. The primary match isolates evaluators under common flags,
so parity against the full production composition remains unverified. Do not
claim production competitiveness solely from this match. Report this search-
integration difference alongside NPS, model sizes, optimizer, corpus, target and
precision differences. Equal-time strength remains separately unverified because
clock testing is not authorized. No production promotion.

After the bounded evidence is complete or valid resources prevent continuation,
consolidate A-F answers and actual costs. On completed investigation return to
PAUSED - AWAITING HUMAN RESEARCH DECISION. A resource interruption instead
preserves the unfinished authorized R057 and exact RESUME operation.
