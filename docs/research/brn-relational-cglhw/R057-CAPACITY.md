# R057 bounded capacity and training preparation

Prospective before any candidate fitting. The three permitted configurations are
8/4, 16/4 and 32/8, with 19,201, 31,489 and 62,209 active parameters respectively.
The configurable ONE stores only active fields; R056's independent implementation
and its unused TWO fields remain unchanged. No new mathematical family is admitted.

Each first-stage arm uses seed 71, the same 1,048,576 logical samples and two
complete passes (2,097,152 exposures). The sample consists of 16 of 512 shuffled
65,536-row blocks, data seed 20261029059. Every epoch permutes the selected blocks
and bijectively permutes their rows. All arms therefore receive the same samples
and order. Targets retain the original BT4 inverse-Q logit400 methodology.

Optimizer: the existing R056 touched-coordinate Adam moments (.9/.999, epsilon
1e-8), dense proportional weight decay .01, clipped weights +/-.99, batch 128,
epoch rates .01 and .001. Untouched coordinates do not advance their moments;
this is recorded explicitly rather than described as ordinary dense Adam.
Initialization remains the R056 Gaussian .1, unary/bias zero, material baseline.
This is not CP64's Bullet optimizer, capacity or precision. Retain these confounds.

The 8,192-position development panel is a seeded sample of original CP64
validation; its aliases are excluded from every training row. It is development
data, not independent final confirmation. Report initial and per-epoch original-
target MSE, fixed training-panel loss, unary-only ablation, time, clipping and
inference costs. The fresh 8,192-position supplement stays reserved until the
principal configuration and evaluation protocol are frozen.

Select the stable configuration with lowest final development MSE. If multiple
configurations are within 1% of the lowest loss, choose the one with the lowest
measured inference lifecycle cost, breaking remaining ties by parameter count.
Record all arms, including negative results. Repeat only the selected capacity
with seed 97 to characterize preliminary variability; do not choose the seed with
the better endpoint. The principal run starts fresh with seed 20261029. A weak
pilot relative to CP64 does not cancel the agreed full-scale training opportunity.

The main run and its exact eight-epoch rate schedule, fixed-node game allocation,
openings and statistical rules must be frozen before it starts. No full-scale
replication is automatically authorized by this selection record.

Each pilot saves weights, both optimizer moments, exact exposure count, next
epoch/row, config identity and prior-receipt hash at 65,536-row boundaries. The
state is independently deserialized and byte-compared before an atomic receipt
commits progress. Resume verifies the receipt chain and latest state and repeats
only work after the latest completed checkpoint. Partial/orphan files survive.
The first arm is deliberately stopped after its first valid segment and resumed
using the unchanged config; optimizer and data-order continuation also have
focused tests. Initial pilot throughput determines later checkpoint granularity.

Resource allocation: one logical CPU, BelowNormal priority, SerialGC, 512 MiB
heap plus approximately 1 GiB read-only corpus mapping. At least 8 GiB free RAM,
CPU below 60% at admission; existing active thresholds and supervisor apply.
Each initial supervised invocation is at most 300 seconds, with valid checkpoints
inside it. No GPU. Disk has approximately 238 GiB free at preparation; a complete
32/8 run with 65,536-row checkpoints would use approximately 6 GiB. No completed
checkpoint is deleted to save space. A resource gate stops work and returns to
STATE's existing RESUME mechanism without availability retries.

Preparation evidence: r057-supplement-validation.json, r057-supplement-audit.json,
r057-training-corpus.json and r057-trainable-validation.json. The first training
runner build failed a checked-exception declaration; failed source and logs are
preserved under raw/r057. No candidate had been fitted at that failure.
