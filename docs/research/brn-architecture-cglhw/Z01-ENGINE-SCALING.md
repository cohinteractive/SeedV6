# Prospective engine-scaling allocation

Frozen design, 2026-10-07 (Pacific/Auckland), after the complete C02 screen and
before any games in this allocation. This is a progressive screen, not final
confirmation. Its exact inputs and commands are bound in the associated plans.

The completed training studies answer prediction questions. This allocation asks
whether more training compute and more distinct data improve complete-engine
strength. C02's positive implementation/learning/practical screen makes compiled
pair tables the principal practical candidate; original pairs remain a control.

Use eight representations: current BRN, current NNUE, strict NNUE, linear
relations, original pair tables, triple tables, learned edge functions, and
compiled pair tables. The compiled and original pair representations share
training weights and cost; they are not independent architectures or replications.

1. Each representation's eligible within-120-second and within-600-second model
   plays the common cheap-material evaluator: 16 contrasts, 512 games.
2. Each representation's eligible large-data model plays its nested small-data
   model: eight contrasts, 256 games. Each selection had nine eligible checkpoints
   and a maximum 2,097,152 exposures; selected exposure counts and actual optimizer
   seconds can differ. This is a data-size/training-protocol comparison, not an
   equal-wall-time comparison or a claim that selected exposures are identical.
3. Compiled pairs' within-600-second model plays current BRN and current NNUE at
   that budget, the captured native BRN reference, and compiled own Gen0: four
   contrasts, 128 games. Native historical training cost is unknown.

Total: **28 contrasts, 896 games, 112 batches of four opening pairs**. Each
contrast has eight common openings, two source/initialization runs, and reversed
colours: 32 games. All contrasts use the same prospectively frozen fresh openings,
seed840211, indices1200..1207, 50ms/move, depth0, maximum1024plies, 600s internal
and900s external batch guards. Opening geometry must be disjoint from all ten
recorded datasets and every inventoried prior opening, including C02. Preserve
any failed preflight and declare a new unplayed sample before games if needed.

This replaces the larger UNFROZEN draft. Eight opening clusters give low power;
the purpose is to identify large changes and allocate independent confirmation,
not to reject a family from a nonsignificant screen. Reducing the exploratory
sample preserves resources for that confirmation while retaining every planned
family and both scaling questions. Four direct principal-candidate controls
address the remaining practical/learning questions without a full round robin.

The 120/600 selections come only from each completed long-study trajectory and
its existing raw-validation rule. Nested selections come only from the audited
eligible small/large checkpoints. No new checkpoint, gain, optimizer or data
tuning. Gains remain .125 for BRN/current NNUE/strict NNUE/linear, .25 for pair/
triple/edge/compiled pair and material, and .5 for the captured native BRN.
Each of six new compiled weight files requires its own full-validation parity
audit at five gains before use. Existing compiled large/Gen0 views may be reused
only after exact byte identity with the designated original models is checked.
Never attach a different parent's optimizer state to an inference checkpoint.

Analyze every planned outcome after the fixed allocation, retaining caps,
failures, missing-game adverse bounds, WDL, per-run scores, approximate two-way
opening/initialization bootstrap intervals, actor search time/NPS and process
memory. Preserve common opening clusters across budgets and runs when estimating
score differences. Scores against material do not establish transitive head-to-
head rankings. Do not stop early on interim scores or call point estimates wins.
All common code, model bytes and metadata are frozen through the queue.

The next gate is independently frozen finalist confirmation under CONTRACT's
default128pairs/run, two runs, fresh openings and100ms/move, at most two pairs per
batch. Exact comparisons and any justified proportional changes are determined
before their outcomes. Separate bounded qsearch diagnostics and sealed quality
follow frozen final model decisions. No new large training allocation, search
policy change, additional gain screen, or production adoption is admitted here.
