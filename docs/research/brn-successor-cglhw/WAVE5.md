# Wave5: recover compact cubic quality through controlled retraining

21 October2026. Continuation of the existing [contract](CONTRACT.md), following
the verified G4 training and [W4 assessment](WAVE4-ASSESSMENT.md). Implementation
preparation is admitted; **G5 is not yet ready and no training has begun**.

## Discriminating question

Does a four-component cubic correction recover useful chess quality when its
retained parameters are allowed to adapt, while retaining the measured cost benefit
of the compact formulation? W4 only pruned a model optimized with16 components.
That intervention reduced controlled search cost about11%, but its hybrid clock
point score43.75% trailed the full model56.25% against the same BRE001 base. The
separate full-strength node pruning control also lost quality. Prediction loss alone
did not identify a safe pruning choice. These negative findings motivate retraining,
not adoption of the current pruned function or rejection of cubic expressiveness.

The compact candidate retains learned unary terms and cubic components2,3,5,9,
chosen previously on the W4 fit partition. It starts from the actual W4 low-rate
**Latest64**, with weights, both Adam moments and optimizer step projected by the
same explicit parameter map. It does not combine an averaged network with unrelated
moments. Original checkpoints remain intact. A distinct wire/configuration identity
prevents loading the compact payload as the original16-component model.

Prepare four separate GUI lineages, each adding32 generations from64 through96:

| Lineage | Starting state | Rate |
| --- | --- | --- |
| W5-RESCUBIC-R4-LR001 | Explicit projection of W4-RESCUBIC-LR001 Latest64 | .001 |
| W5-RESCUBIC-R16-LR001 | Exact W4-RESCUBIC-LR001 Latest64 | .001 |
| W5-BRE-CONTROL-LR001 | Exact W4-BRE-CONTROL-LR001 Latest64 | .001 |
| W5-BRE-CONTROL-LR003 | Exact W4-BRE-CONTROL-LR003 Latest64 | .003 |

The two cubic arms must inherit identical data selection, cursors, seek indexes,
seed, batch128,8epochs,131072 training positions and held-out WDL validation. This
isolates the compact-capacity/retraining intervention within the cubic family.
Both BRE controls retain their own original data and optimizer continuation. Their
different split/cursor history remains a limitation; do not claim identical-data
architectural causation. This gate does not resolve summed hybrid exposure or
historical base/holdout overlap. Preserve both BRE rates because validation-loss
ordering did not reliably predict their playing strength.

## Acceptance before the human gate

Require explicit schema/codec identity, source-preserving optimizer projection,
finite differences, exact persistence/continuation, canonical and guarded cache
parity, actual starting-checkpoint score/search checks and cost measurements.
Verify source-store preservation, all four prepared GUI recipes, source/seek
continuation and bounded interrupted-generation recovery. Build and verify the
native Windows GUI distribution; headless tests do not establish visual acceptance.
Use tiny fixtures only. No substantive Codex training is authorized.

Finish all128 already admitted W4 hybrid follow-up games with their frozen build54
runtime. New source changes must not replace it. After the owner trains G5, freeze
actual Latest/Best identities and assess compact versus full cubic, both BRE rates
and frozen Gen32. Retain unary/quadratic ablations and equal-node/equal-time
distinctions. A compact model winning a development screen is not confirmation.

Current next action: implement and validate the distinct compact layout and
non-destructive continuation preparation. Do not ask the owner to train until the
actual lineages, native distribution and exact resume instructions are verified.
