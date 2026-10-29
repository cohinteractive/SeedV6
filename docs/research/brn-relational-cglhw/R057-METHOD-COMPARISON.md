Completed continuation: [R057-ASSESSMENT.md](R057-ASSESSMENT.md) records the
final trained-model and playing results. The measurements/planning boundary below
is preserved as originally recorded.

# R057 comparison limits and verified optimizer details

This records interpretation evidence during the frozen principal run; it does not
change its config, schedule, targets or endpoint. Equal exposure is a substantially
fairer learning opportunity than the old small pilots, not an isolated test of
architecture with every other variable controlled.

| Property | Frozen CP64 | Principal ONE16/4 |
|---|---|---|
| Training samples |33,554,432 original E006 rows|33,554,432 logical rows;39,396 same-source replacements|
| Canonical unique positions |33,164,045|33,180,466|
| Intended exposures |268,435,456 completed historically|268,435,456 target; actual progress is recorded separately|
| Exported/inference parameters |1,970,440|31,489|
| Training parameters |2,167,048 including196,608 factorizer weights|31,489|
| Target |BT4 inverse-Q logit400, sigmoid squared error, WDL mixture0|Same underlying target/scale; no CP64 distillation|
| Batch size |4,096|128|
| Updates at equal exposure |65,536|2,097,152|
| Optimizer |Bullet dense AdamW, float32 GPU|R056 touched-coordinate moments, explicit bias correction, double CPU|
| Moment constants / epsilon |.9/.999;1e-8|.9/.999;1e-8|
| Decoupled decay |.01 per update|.01 per update|
| Learning rate |.001 through checkpoint64|Frozen eight-epoch schedule in R057-MAIN.md, initially.01|
| Clipping |Transformer and factorizer +/-.99; other defaults +/-1.98|All active parameters +/-.99|
| Training input order |DirectSequentialDataLoader on original prepared stream|Seeded block order and bijective within-block permutations|
| Representation |Dual perspective, king buckets,256-hidden SCReLU,8 material output buckets|ONE low-rank piece-square queries/keys,16 content channels,rank4,softsign recipient readout,explicit material baseline|
| Inference |Quantized int16 export, vector CPU head, incremental accumulator|Double immutable products and incremental relational pools|

The CP64 manifest at
`raw/e007-training-64/segments/000064-37ddad38acef/run/seed-cnnue-v1-64/seed-manifest.json`
in the conventional workstream binds Bullet commit
`579b78b37a694cacd6e9ede4706386c15c34837c`, completed superbatch64 and effective
rate0.0010000000474974513. The local upstream checkout has that exact clean HEAD.
Its `crates/trainer/src/optimiser/adam.rs` supplies the defaults and dense update
equations; its AdamW kernel has no explicit moment bias-correction factors.
The pinned trainer source adds `l0f` of256*768 weights to the exported transformer,
sets only `l0w`/`l0f` clipping to.99, selects sigmoid squared loss and uses the
sequential loader. The original config's rate drop is at128 superbatches, beyond
the selected64 endpoint. The resumed first four superbatches retain that recipe.

The initializer, number of optimization steps, learning-rate schedule, sparse
moment semantics, clipping, parameterization and precision are consequential
differences. More samples do not isolate their effects. In particular, the same
decay coefficient is applied on32 times as many ONE updates, at different rates;
this is not matched cumulative regularization. No causal attribution of a playing
gap to any single difference is supported by this one frozen run.

Game IDs are absent from the source. Canonical aliases and known reservations
are excluded, but source-game and future trajectory independence remain unknown.
The new replacement quota differs from the original chunk-prefix quota. CP64's
development history includes the original validation corpus, whereas the new
8192-row panel remains reserved until the frozen principal endpoint.

Common search uses flags262143/ADAPTIVE16 for both evaluators. CP64 production
flags3407871 additionally use NOMINAL_FUTILITY and VERIFIED_NULL. Those settings
are preserved; common-search results do not establish parity with that complete
production composition or practical equal-time strength.

The actual main exposure count, loss curve and checkpoint identity are in
r057-main-progress.json. This methodological comparison does not imply that the
principal run, predictive comparison or playing evaluation is complete.
