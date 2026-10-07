# Prospective bounded qsearch diagnostic

2026-10-07. Freeze before inspecting the engine-scaling outcomes. Execute only
after its empirical queue closes; one empirical process at a time. This is a
diagnostic of the existing research qsearch implementation, not a production
search change or a new calibration/architecture selection opportunity.

Use the already selected within600-second checkpoints for current BRN, current
NNUE, strict NNUE, linear, original pairs, triples, edge functions and compiled
pairs, for both source/order runs. Add cheap fixed material, captured native BRN,
and compiled pair Gen0 on both validation ranges: 22 processes. Native is one
historical model, and material/Gen0 repeat fixed initial functions; the two data
ranges do not create independent trained replications of these controls.

Each process attempts the same32 validation roots, indices128..159 in its range,
depth3,100000-node and1000ms per-root guards. Retain the adapter's separate eight
warmup roots at depth2,10000nodes/250ms. Gains remain those frozen for engine
scaling. Use the existing tested `qsearch` adapter,2GiB heap and300s external
process limit. Full-process cost includes initialization, warmup and reporting.

Record every root's FEN, completion, nodes, qnodes, maximum qply, wall time and
guard flags. Scores/moves are meaningful only for completed searches. Keep
completion rates and node/time tails for all attempted roots; do not compare
only a conveniently completed subset. Dataset/model/metadata/code and command
identities must match the frozen plan, with common FENs checked across models.
All-attempted process completion does not mean all roots completed.

Report native research qsearch behavior separately from the production PVS
static-leaf match results. Node/time guards are expected possible outcomes, not
draws or successful searches. Do not extrapolate this small validation sample
to long-time-control strength or enable qsearch in the engine from this result.
No sealed labels, training, tuning or extra search-policy variants are admitted.
