# Wave 9: calibrated fixed-basis controls

22 October 2026. This continues the original CGLHW under contract revision 2.
G8 training and its176-game development assessment are closed. **G9 is verified
complete through Gen176. The CGLHW and incomplete trained assessment are
SUSPENDED BY OWNER DECISION as of22 October2026; see STATE.md and
WAVE9-ASSESSMENT.md. No research success or independent strength superiority is
claimed. Historical restart instructions below require deliberate owner reactivation.**

## Question and decision

Does cubic information add practical value after fitting an equally calibrated
additive baseline? G8 did not establish a cubic advantage over its adapted
quadratic or continued-BRE controls. Its positive, uncertain exact-anchor results
keep a useful cubic signal plausible. The new experiment changes a specific
confound instead of extending unchanged G8 training or matches.

Bounded diagnostics on 2,043 consumed fit rows, 128 updates per arm, found that
four ordinary affine coefficients improve mapped cross-entropy more than
interaction-only pilots on all four consumed evaluation panels. Joint affine
plus quadratic and affine plus cubic improve further; cubic wins only one panel.
These are fitting diagnostics, not substantive training, independent
generalization or strength evidence. Phase conditioning lacked a consistent
advantage and was not implemented. Preserve all results in
`wave8-phase-head-results.json`, `wave8-affine-head-results.json` and
`wave8-calibrated-degree-results.json`.

## Exact initialization and semantics

All three new head lineages read W8 fixed-cubic Latest168, network SHA-256
`c1ecc378294bfee8b06ac957e6115ce07a71b4a10fce63e69bc561dd6d0115c4`.
They preserve its 3,845 frozen Q4 values exactly, unchanged since W7 Q4 Latest160;
omit the four W8 cubic coefficients; initialize new heads and all Adam moments to
zero; and inherit the source's Gen168 data configuration, cursors and CP seek
indexes. This is an explicit projection into new identities, not inherited head
optimization or a modification of any W8 checkpoint.

Let G be frozen Gen32 raw pawns, M material, R the retained Q4 correction, and
q/c the fixed rank-four degree-two/degree-three features. The four affine features
are [1, M/8, (G-M)/4, R]. New raw pawns are:

`G + 0.5 * (R + affineHeads dot affineFeatures + optional degreeHeads dot q/c)`.

Search applies the existing material-plus-quarter mapping once. The new trainer's
cross-entropy objective and held-out half-squared outcome error use this same
floating search-pawn function before integer rounding. Existing wire1..10 models,
objectives and search behavior remain unchanged. **Do not apply the legacy
correctionScale=0.5 a second time to these models.**

| Prepared lineage | Trainable parameters | Optimizer at Gen168 | Target |
| --- | --- | --- | --- |
| W9-CAL-AFFINE-R4-LR001 | Four affine heads; additive control | New Adam, step0 | 176 |
| W9-CAL-QUAD-R4-LR001 | Four affine + four quadratic heads; additive control | New Adam, step0 | 176 |
| W9-CAL-CUBIC-R4-LR001 | Four affine + four cubic heads; degree-three candidate | New Adam, step0 | 176 |
| W9-BRE-CONTROL-LR001 | Ordinary BRE continuation | Exact W8 BRE state, step3,129,344 | 176 |

The head models have 3,849/3,853/3,853 parameters plus the shared frozen BRE model.
Only the final four/eight coefficients train. Latest and Best initially name each
new bootstrap at Gen168. Full roots, IDs, payload/state hashes, source chains and
settings are in `wave9-owner-readiness.json`, `wave9-owner-projection.json` and
`wave9-continuation-plan.json`. BRE retains the exact W8 Latest168 network
`f19d3dd3b7fa9bc3adff21f0bef7f7b2b7d62ff3621d518098486a5ef77b932e`
and all optimizer state. No existing model was overwritten or trained by Codex.

Equal parameter counts do not make the two eight-head feature spaces nested:
affine+quadratic adjusts four existing Q directions independently, whereas
affine+cubic adds four third-degree directions and can rescale the full anchor.
Affine-only is their common adapted control. Use both trained controls and
appropriate masks; an unadapted mask alone cannot establish architectural value.

## Historical human action: G9 (completed and verified)

Use verified **build59**:
`dist/windows/candidates/candidate-20261021T192119-a567d46b/SeedV6-GUI/SeedV6-GUI.exe`.
Implementation commit: `e804482157d6e9064077dbbeb0c4c9a7d9ca06ce`.
The image honestly embeds `e804482157d6 (uncommitted changes)` because research
evidence was being produced. Tracked runtime/source/version paths match that
implementation commit; package/class checks are recorded in
`wave9-package-source-provenance.json`. This is a verified local candidate, not
a claim of a clean-tree release.
The installed shortcut was not replaced. Older builds do not support the new
calibrated architectures.

1. In Network Training select each exact W9 lineage above, sequentially. Confirm
   Latest Gen168 as the starting checkpoint and **8 additional generations**, so
   the session targets Gen176 (generations 169..176).
2. Retain the saved explicit learning rate **0.001**, batch **128**, **8 epochs**,
   **131,072 training positions per generation**, seed **1**, **one worker**,
   held-out WDL validation and inherited Training Data sources. Do not replace
   the data, reset an initializer or import another checkpoint.
3. If interrupted, resume the existing lineage/session toward its original
   Gen176 target. Do not request another eight generations after partial progress.
   Preserve the interrupted attempt and use the GUI's existing Resume action.
   Report a failure instead of deleting state or restarting from Best.
4. Preserve every checkpoint, optimizer, history, source configuration and seek
   index. After all four reach Gen176, close the GUI and report completion or
   anomalies. No manual Arena matches or repeated G8 training are requested.

Expected final optimizer steps are 65,536 for each head arm and 3,194,880 for BRE.
Each adds 1,048,576 training examples, 8,388,608 example visits and 65,536 updates;
the eight changing validation panels contain 262,144 examples in total. Training
must be evidenced from actual histories and payloads, not inferred from a later
Codex invocation. Latest176 is the prospective primary endpoint; audit Best
separately and do not substitute it retrospectively after favorable matches.

## Implementation and validation

Core owns versioned wire11/12/13 inference and guarded cache composition; training
owns the new objective, masks and optimizer projection; GUI owns selection and
preparation; research tools bind variants and audits. Runtime folds constant
coefficients into polynomial caches but retains an independent canonical oracle
and rounding guard. Unsupported legacy compositions fail closed. No search-policy
tuning, production evaluator replacement or unrelated optimization was made.

The final targeted run passed **241 tests, zero failures or skips**; this is not
a full-suite claim. It covers explicit combination forward oracles, calibrated
objective finite differences, frozen parameters/moments, exact Adam resume,
partial corpus generations, source ownership, codecs, color/special moves,
random cache transitions, rounding boundaries, degree-three versus additive
expressiveness and full/cache search parity. See
`wave9-implementation-validation-v2.json`.

Earlier failed QA is preserved. A pawn-channel fixture and exhaustive enum
fixtures were corrected. Review also caught a new-path mismatch between raw
checkpoint validation and mapped training; it was fixed before owner creation,
and run06 verifies trainer/checkpoint outcome parity across all 13 families.
The first isolated audit plan used a payload-only reference directory as a full
store and failed before candidate auditing; the corrected plan reads the actual
reference store. No source-store repair was required.

Independent byte/CRC checks verify exact projection, fresh head state, exact BRE
continuation and complete source/seek inheritance. Actual initial functions agree
with compiled Gen32 plus half the original Q4 correction on 4,096 positions,
maximum error 8.89e-16. Each of three cache probes passes 4,340 played positions
and 48 full/cache search pairs, with zero integer/fresh-state differences and
maximum raw difference 2.09e-15. All three initial functions also agree on every
search result. See `wave9-isolated-readiness.json` and
`wave9-initial-function-parity.json`.

Initial cached medians are affine 204.122ns, quad 202.720ns and cubic 215.632ns,
against separate Gen32 measurements 132.904/132.316/141.220ns. Host/JIT variability
and learned coefficient effects remain; these are not trained W9 cost or strength
claims. Workspaces require 8,205/8,205/8,269 primitive bytes plus roughly 30KiB of
derived polynomial model per worker, excluding object headers.

Native packaging selfchecks pass. Actual packaged-owner verification, screenshots,
payload/runtime checks and preservation results are bound in
`wave9-owner-readiness.json` and `wave9-final-preservation.json`. All 6,620 unique
audited existing/new files are unchanged, including the complete72-file new-owner
inventory; all eight candidate/source cursor probes passed. No substantive
W9 training or full GUI training-session completion is claimed. The local
packaging wrapper's BOM assignment error and the development-runtime timestamp
erratum remain recorded separately; independent candidate/jar checks passed.

## Restart after G9

Keep `build/research/brn-successor-batches.pause` active until owner completion is
verified. Audit actual histories169..176, parent chains, architecture, recipes,
frozen prefixes, zero excluded moments, expected steps, sources/cursors/indexes,
Latest and Best payloads. Resolve material discrepancies before assessment.
The future audit tools support G9 but have not audited nonexistent trained models.

Then compare cubic with both adapted additive controls at equal time and targeted
equal nodes, inspect calibration and learned coefficient magnitude, and include
continued BRE and frozen Gen32 where useful. The BRE training objective and
historical split differ; its GUI loss is not directly comparable with the new
mapped losses. Preserve the untouched confirmation partition for an independently
admitted candidate. Do not extend unchanged heads merely to seek significance.

If cubic again adds no useful evidence beyond the calibrated additive controls,
prioritize a different information basis or selective expressive mechanism. A
negative result concerns this fixed rank-four basis and objective, not all
higher-order chess relationships. Changed fitting may still harm pruning or
actual strength; no optimal calibration or future gain is assumed.

**G9 completion verified22 October2026:** all four Latest176; head Best175, BRE
Best176. Exact evidence: wave9-trained-checkpoints.json and
wave9-trained-admission.json. Prepared instructions above are historical; do not
repeat training. Current assessment and next action: WAVE9-ASSESSMENT.md and STATE.md.
