# Wave5: recover compact cubic quality through controlled retraining

21 October2026. Continuation of the existing [contract](CONTRACT.md), following
the verified G4 training and [W4 assessment](WAVE4-ASSESSMENT.md). Implementation
and actual owner preparation are complete in build55. **G5 is ready; human training
is blocking and has not begun.** No programme success is claimed.

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

All128 admitted W4 hybrid follow-up games finished with their frozen build54
runtime; all1344 W4 development games are closed. After the owner trains G5, freeze
actual Latest/Best identities and assess compact versus full cubic, both BRE rates
and frozen Gen32. Retain unary/quadratic ablations and equal-node/equal-time
distinctions. A compact model winning a development screen is not confirmation.

## Verified preparation and costs

Implementation82331a5/build55 adds wire6 with four components, preserving old layouts.
The preparation factory creates distinct roots and rejects active/partial sources,
wrong checkpoint/rate bindings, missing seek metadata and foreign destinations.
Existing destinations are inspected without replacing edited settings or trained state.
Source weights, both Adam moments, step and rate are retained by one explicit map;
the full16/BRE controls retain exact source payload bytes. Independent Python byte,
header, CRC and11547 moment/weight comparisons verify the actual compact state.

87 isolated JUnit tests plus one actual native GUI test pass, zero failed/skipped.
They include every-parameter finite differences, independent polynomial forward,
codec integrity, tiny fit, shuffled interrupted optimizer continuation, GUI-service
continuation, same-data cubic acquisition and factory preservation/rejection cases.
No substantive owner training was run. The actual five-payload audit (four new roots
plus frozenGen32) verifies checksums and runtime/state scores on24 boards each.

Three actual compact/full16/masked-packed models pass6333 preserved positions and
48 search root passes each with zero integer/search differences; compact versus
masked-packed search results also match on all48. Maximum raw cache error<9e-15.
Compact model/state bytes30864/92464 versus104784/314224 for full16. With the shared
frozen base, model payloads total825896 versus899816 bytes. Compact delta workspace
3753 primitive bytes versus4329 for full16 and28409 for the old packed mask.
Transition medians219ns compact,306ns full16,227ns masked-packed; associated Gen32
measurements136-145ns. Compact same-function search cost is.954 of masked-packed in
this single sequential probe. These are bounded cost diagnostics, not universal
throughput estimates or strength.64 legal smoke plies pass across all four starts;
eight intentional8-ply caps are excluded from strength statistics.

Native build55, sourcebd65091, is packaged and verified, including bundled model,
runtime/JNI checks and copied/extracted paths with spaces. Native Swing previews
from both development and the packaged JAR/bundled runtime load all four actual
lineages with correct recipes and Gen64/Start Training. Eight packaged images and
the runtime-source assertion are preserved. No owner training button was clicked.
All1405 W4 files,710 W3/frozen files and72 newly prepared W5 files retain their
audited bytes. W1 was untouched, not freshly rehashed. See
[readiness](wave5-owner-readiness.json), [projection](wave5-projection-audit.json),
[cost/parity](wave5-cache-validation.json), [implementation tests](wave5-implementation-validation.json)
and [preservation](wave5-preservation-check.json). Raw evidence is local-only in
raw/wave5; the initial preparation command's quoting failure and incompatible-GA-JDK
relocation attempt remain preserved, with successful separate retries.

## Human training gate G5: exact action

Open the verified build55 executable:
`C:/projects/seed/java/seedv6-unified/dist/windows/candidates/candidate-20261021T013302-0f255e57/SeedV6-GUI/SeedV6-GUI.exe`.
Use **Network Training** and select each existing W5 lineage below, one at a time.
Do not create a replacement, import another checkpoint, or train the original W4.

| Name | Architecture folder / lineage ID | Rate |
| --- | --- | --- |
| W5-RESCUBIC-R4-LR001 | BRN-GEN32-CUBIC-R4 / a1f43385-af0a-31aa-adcc-4aaef1301c0d | .001 |
| W5-RESCUBIC-R16-LR001 | BRN-GEN32-CUBIC / 26df2166-8a6d-3302-877f-a79b3f84549d | .001 |
| W5-BRE-CONTROL-LR001 | BRN-Pair2 / 2d4a7efa-a94a-3a3c-bc8c-6ffe8476f03f | .001 |
| W5-BRE-CONTROL-LR003 | BRN-Pair2 / abf7825e-a5ad-34cd-9231-0f2eb8099fb3 | .003 |

All roots are under `E:/SeedV6-Networks`. Full checkpoint IDs, network/state hashes,
source ancestry, source offsets and inherited optimizer steps are in readiness.
Latest and Best initially both name each new Gen64 bootstrap; zero recorded completed
generations is expected because the new lineage has not trained yet. Source history
is preserved in W4 and bound by origin, not copied as fictitious W5 training.

For every arm, retain **Fixed Number of Generations / Additional Generations32**,
batch128,8epochs,131072 training positions/generation, seed1, one worker,
**Held-out WDL** validation (32768 held-out positions/generation), Training Data
sources and the inherited equal BT4/CP selection. Learning rates are explicit as
above; the prepared “Keep checkpoint learning rate on Resume” box is unchecked.
Do not change rates or other recipe fields during this experiment. Both cubic arms
share exact data cursors/partition/seek indexes; BRE retains its existing continuation.
Confirm Latest Gen64 and **Start Training** before starting. First generation is65;
target96. Stop after96; do not start another32 after completion. Either family may
be trained first. Run one training job at a time and no Arena/tournament concurrently.

Each arm adds4194304 training positions,1048576 validation positions,33554432 visits
and262144 optimizer updates. Expected final step786432 for both cubic arms and
2539520 for BRE. These are audit expectations, not counters to edit. Best may precede96.
Keep every bootstrap, checkpoint65..96, Best/latest pointer, optimizer state, history,
source ledger/index, recipe and origin. This dose is below existing100-generation
retention. Preserve all W1/W3/W4 and frozenGen32 artifacts.

After interruption, select the **same** W5 lineage and use its offered Resume
Generation action with unchanged settings. An ACTIVE/crash/failed session retains
its original target/remainder. An explicit safe Stop ends the session: before a new
Start set Additional Generations to `96 - settled latest completed generation`,
retaining any unfinished-generation state. Do not reset, import, clear partial state
or silently restart at64. Resume semantics were verified on tiny fixture updates;
if errors/nonfinite loss occur, preserve the failure and report it before changing
state. The owner remains responsible for substantive training.

**Human actions required after this prompt: Blocking — train these four lineages
through Gen96, then close the GUI and report completion or any interruption/settings
change.** This blocks W5 trained strength assessment, not acceptance of the programme.
No manual Arena match or G4 retraining is requested. Report unusual loss or contention;
Codex will inspect actual Latest/Best, ancestry, recipes, steps, validation and source
reservations. A later prompt alone does not satisfy the gate.

## Exact restart

The research batch pause sentinel is active at `build/research/brn-successor-batches.pause`.
No match/training process is running. On reported completion, verify all four actual
stores before clearing that sentinel (preserve it in raw/wave5). Freeze Latest/Best
independently, distinguish full16 versus compact retraining, both continued BRE rates,
frozenGen32, calibration and fast additive/quadratic ablations. Reassess costs at the
trained weights. Keep development selection separate from fresh confirmation; no
candidate currently qualifies for a final positive-strength claim.
