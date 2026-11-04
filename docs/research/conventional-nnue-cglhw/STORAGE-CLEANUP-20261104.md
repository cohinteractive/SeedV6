# Completed NNUE/BRN training storage cleanup - 4 November 2026

Owner-authorized repository storage maintenance at starting HEAD `df5659ff933f7dbc127743824409a31447f4d805` (clean working tree). This is not a Search objective or CGLHW. No training, tournaments, production/configuration changes, release or deployment occurred.

**Accepted maintenance closeout, 4 November 2026:** the owner explicitly accepted the completed cleanup and authorized one focused documentation commit containing this report and the conventional/relational STATE notices. This closeout records that acceptance without further deletion or changes to research conclusions. Permanent prevention of checkpoint accumulation is a separate intended work unit; its implementation has not begun.

## Authority and scope

The owner explicitly authorized this identified cleanup and superseded conflicting historical blanket binary-retention requirements **only for disposable intermediate artifacts from completed NNUE/BRN training**. Production models, selected/final comparison models, conclusions, metrics, configurations, failed/partial attempts, suspended training, genuinely required/ambiguous binaries and all Phase 4 Search evidence remain protected. This does not broadly repeal retention requirements or amend frozen research methods/findings.

Applicable records inspected included root AGENTS.md, Phase 4 masters P4-R016/P4-F032, the three training CONTRACT/STATE records, conventional CANDIDATE-HANDOFF/E002-E007 manifests, BRN relational protocols/results and R057 completion/receipt chains, and successor W9 suspension/preservation records. The explicit owner resolution covers such historical clauses as conventional CONTRACT's "Preserve all existing research, data, network bytes, failed experiments", relational CONTRACT's checkpoint preservation, and R057-CAPACITY's "No completed checkpoint is deleted to save space." It does not authorize deletion from an unfinished or ambiguous run.

## Measured result

Deleted **43,506,818,324 bytes (40.51888 GiB)** in **7,416 binary files**, across 88 completed-run directories. Removed 1,269 now-empty initializer/optimizer subdirectories. Experiment/segment directories containing useful records remain. No archive or backup of discarded data was created.

C: available free space immediately around deletion rose from 212,697,702,400 to 255,983,321,088 bytes: **40.31287 GiB observed free-space increase**. This volume-wide delta is distinct from the exact deleted-file byte total and can include concurrent disk activity/allocation effects. Deletion completed at 2026-11-04T00:39:52Z (13:39:52 Pacific/Auckland).

Sizes below are logical file bytes measured before cleanup and immediately after deletion, before adding this small report/notices. GiB = 2^30 bytes.

| Research directory | Original bytes (GiB) | Deleted bytes (GiB) | Retained bytes (GiB) |
| --- | ---: | ---: | ---: |
| `conventional-nnue-cglhw` | 44,899,755,405 (41.816) | 40,442,498,548 (37.665) | 4,457,256,857 (4.151) |
| `brn-relational-cglhw` | 7,874,954,655 (7.334) | 3,064,319,776 (2.854) | 4,810,634,879 (4.480) |
| `brn-successor-cglhw` | 3,344,651,346 (3.115) | 0 (0.000) | 3,344,651,346 (3.115) |
| **Total** | **56,119,361,406 (52.265)** | **43,506,818,324 (40.519)** | **12,612,543,082 (11.746)** |

## Conventional NNUE deletion inventory

All paths in this table are below `conventional-nnue-cglhw/raw/`. Every requested segment interval was complete with a matching receipt/configuration and successful recorded process exit. Across 636 segments, removed `run/initial/{weights,momentum,velocity}.bin`; from 633 unretained checkpoints also removed `raw.bin`, `quantised.bin` and `optimiser_state/{weights,momentum,velocity}.bin`. Those are weights and restart-only AdamW state, not game, probe, metric or configuration evidence. Initializer copies duplicate predecessor state after the first segment. All logs, GPU probes, JSON receipts/manifests and configurations remain byte-identical.

| Directory | Completed segments | Binary files deleted | Bytes deleted | Full checkpoint retained |
| --- | --- | ---: | ---: | --- |
| `e002-training` | 1..16 | 128 | 1,019,591,744 | None; final/comparison runtime export retained separately |
| `e003-training` | 17..80 | 512 | 4,078,366,976 | None; final/comparison runtime export retained separately |
| `e005-training-64` | 1..64 | 507 | 4,040,604,156 | E005/16 (selected data-fork source) |
| `e006-training-128` | 65..128 | 512 | 4,078,366,976 | None; final/comparison runtime export retained separately |
| `e006-training-256` | 129..256 | 1,024 | 8,156,733,952 | None; final/comparison runtime export retained separately |
| `e006-training-64` | 17..64 | 384 | 3,058,775,232 | None; final/comparison runtime export retained separately |
| `e007-training-128` | 65..128 | 512 | 4,085,260,800 | None; final/comparison runtime export retained separately |
| `e007-training-256` | 129..256 | 1,019 | 8,132,694,156 | E007/256 (pinned latest handoff/control pointer) |
| `e007-training-64` | 5..64 | 475 | 3,792,104,556 | E007/64 (selected CP64 handoff) |

All 12 `raw/e*-evaluation*/network.cnnue` exports and their independent parity/metrics evidence remain: E002, E003, E005/16/32/64, E006/64/128/256, and E007/4/64/128/256. Thus later weak or inconclusive endpoints remain available for comparison independently of their discarded optimizer histories. The three retained full checkpoints retain all five receipt-bound binaries and valid tensor geometry; their initial duplicate snapshots were unnecessary. Existing control-validation, interrupted/failed-attempt evidence, frozen runtimes/trainers, opening selections, matches and datasets remain.

## BRN relational deletion inventory

All following paths are below `brn-relational-cglhw/raw/`. `{71,97}` denotes exactly the two listed seed directories, not an open-ended wildcard. Completed result records, per-epoch/segment metrics, hashes, configurations, receipts and logs remain. Deleted ordinary intermediate `epoch-N.state`, redundant `epoch-N.brn-pair2`, and `segment-N.state`; kept final models and their matching exported networks, downstream-selected references, specifically useful comparisons and interrupted-run evidence. A path occurring only in a historical training curve is not an ongoing model dependency.

| Directories (two seeds each) | Binary files deleted | Bytes deleted | Retained checkpoints / reason |
| --- | ---: | ---: | --- |
| `r002-aligned-{71,97}` | 12 | 21,158,736 | Final epoch 4; selected/failed-family comparison endpoint |
| `r002-coverage-aligned-{71,97}` | 56 | 98,740,768 | Final epoch 16; selected/failed-family comparison endpoint; matched-exposure epoch 8 |
| `r002-coverage-legacy-{71,97}` | 56 | 98,740,768 | Final epoch 16; selected/failed-family comparison endpoint; matched-exposure epoch 8 |
| `r002-depth-aligned-{71,97}` | 232 | 409,068,896 | Final epoch 64; selected/failed-family comparison endpoint; best diagnostic epochs 48 (71), 39 (97) |
| `r002-depth-legacy-{71,97}` | 236 | 416,121,808 | Final epoch 64; selected/failed-family comparison endpoint |
| `r002-legacy-{71,97}` | 12 | 21,158,736 | Final epoch 4; selected/failed-family comparison endpoint |
| `r002-scale-aligned-{71,97}` | 28 | 49,370,384 | Epoch 24 Pair-2 reference for both seeds |
| `r003-context-{71,97}` | 14 | 98,236,544 | Final epoch 8; selected/failed-family comparison endpoint |
| `r003-edge-{71,97}` | 14 | 11,145,344 | Final epoch 8; selected/failed-family comparison endpoint |
| `r003-unary-{71,97}` | 14 | 258,944 | Final epoch 8; selected/failed-family comparison endpoint |
| `r004-compose-{71,97}` | 14 | 13,709,696 | Final epoch 8; selected/failed-family comparison endpoint |
| `r004-pair-{71,97}` | 14 | 13,709,696 | Final epoch 8; selected/failed-family comparison endpoint |
| `r006-bounded-{71,97}` | 14 | 13,709,752 | Final epoch 8; selected/failed-family comparison endpoint |
| `r006-unregularized-{71,97}` | 14 | 13,709,752 | Final epoch 8; selected/failed-family comparison endpoint |
| `r007-bt4-{71,97}` | 60 | 105,793,680 | Final epoch 40; selected/failed-family comparison endpoint |
| `r007-distilled-{71,97}` | 60 | 105,793,680 | Final epoch 40; selected/failed-family comparison endpoint |
| `r009-king-{71,97}` | 14 | 371,369,180 | Final epoch 8; selected/failed-family comparison endpoint |
| `r009-shared-{71,97}` | 14 | 37,138,556 | Final epoch 8; selected/failed-family comparison endpoint |
| `r011-bounded-{71,97}` | 14 | 13,709,752 | Final epoch 8; selected/failed-family comparison endpoint |
| `r011-unregularized-{71,97}` | 14 | 13,709,752 | Final epoch 8; selected/failed-family comparison endpoint |
| `r012-linear-{71,97}` | 14 | 6,806,912 | Final epoch 8; selected/failed-family comparison endpoint |
| `r012-nonlinear-{71,97}` | 14 | 6,806,912 | Final epoch 8; selected/failed-family comparison endpoint |
| `r018-local-{71,97}` | 12 | 11,073,792 | Final epoch 8; selected/failed-family comparison endpoint |
| `r028-one-{71,97}` | 16 | 23,962,624 | Final epoch 1; selected/failed-family comparison endpoint |
| `r028-two-{71,97}` | 16 | 23,962,624 | Final epoch 1; selected/failed-family comparison endpoint |
| `r031-control-{71,97}` | 16 | 23,962,624 | Final epoch 2; selected/failed-family comparison endpoint |
| `r031-low-{71,97}` | 16 | 23,962,624 | Final epoch 2; selected/failed-family comparison endpoint |
| `r032-dynamic-{71,97}` | 16 | 25,002,496 | Final epoch 1; selected/failed-family comparison endpoint |
| `r032-geometry-{71,97}` | 16 | 25,002,496 | Final epoch 1; selected/failed-family comparison endpoint |
| `r036-mix-{71,97}` | 16 | 23,962,624 | Final epoch 2; selected/failed-family comparison endpoint |
| `r040-context-{71,97}` | 16 | 5,456,896 | Final epoch 1; selected/failed-family comparison endpoint |
| `r040-geometry-{71,97}` | 16 | 5,456,896 | Final epoch 1; selected/failed-family comparison endpoint |
| `r041-tied-{71,97}` | 16 | 7,816,192 | Final epoch 1; selected/failed-family comparison endpoint |
| `r044-control-{71,97}` | 16 | 7,816,192 | Final epoch 2; selected/failed-family comparison endpoint |
| `r044-low-{71,97}` | 16 | 7,816,192 | Final epoch 2; selected/failed-family comparison endpoint |
| `r045-mix-{71,97}` | 25 | 12,251,200 | Final epoch 2; selected/failed-family comparison endpoint; seed71 epoch1 and segments1..8 resource-gate recovery evidence |
| `r055-gated-{71,97}` | 34 | 16,713,856 | Final epoch 2; selected/failed-family comparison endpoint |

R057 uses separately receipt-committed `model.state` files. All five successful runs have complete, contiguous exposure/optimizer cursors and hash-linked receipt chains ending at their result records. All 1,152 receipts and per-epoch metrics remain, while final model hashes were checked against their receipts.

| R057 directory | Intermediate states deleted | Bytes deleted | State retained |
| --- | ---: | ---: | --- |
| `r057/main-16x4-20261029` | 1,023 | 773,162,940 | Final 268,435,456-exposure model |
| `r057/pilot-16x4-71-v2` | 31 | 23,429,180 | Final 2,097,152-exposure model |
| `r057/pilot-16x4-97-v2` | 31 | 23,429,180 | Final 2,097,152-exposure model |
| `r057/pilot-32x8-71-v2` | 31 | 46,284,860 | Final 2,097,152-exposure model |
| `r057/pilot-8x4-71-v2` | 30 | 13,826,040 | Final 2,097,152-exposure model and first 65,536-exposure deliberate hold/resume checkpoint |

No dataset, corpus-index/merge file, SQLite selection database, runtime/JAR, game, prediction/trace output or failed/partial attempt was deleted. In particular the original `r057/pilot-8x4-71` attempt is untouched, as are data-integrity failures/retries and R010's paused selection database.

## Retained models, dependencies and protected areas

| Asset | Verified SHA-256 / retention reason |
| --- | --- |
| `build-assets/nnue-candidate/conventional-cp64.cnnue` and `raw/e007-evaluation64/network.cnnue` | `8431626c2fca81e0a1e9ccd5e45ddd8829e6e3282c756c21fad75b06d075bb2b`; 3,940,952 bytes. Phase 4 production CP64. Payload `5da0253642ab33b8e446b5b1c5764750be008cd3dbacc7cd1992a95bf4a881f6`. |
| `build-assets/production/bre-pair2-g32.brn-pair2` | `5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7`; 795,032 bytes. Required Gen32 fallback/comparison. |
| `brn-relational-cglhw/raw/r002-scale-aligned-71/epoch-24.brn-pair2` | `ada3837188a2f1679e2b6c092ac8dc7bb74dce31c30e3e89376c7e3910cd9627`; selected Pair24 secondary control. Both seeds' final state/export remain. |
| `brn-relational-cglhw/raw/r057/main-16x4-20261029/segments/268435456-b5a9ca3b-301/model.state` | `7f081bab949139d768147ea736d68d26b16a8f33593823e209e34e20cc63c1c2`; final principal ONE comparison model, still weaker in the completed screen. |
| R022 COMPOSE/1, R028 ONE/1, R041 TIED/1, R045 MIX/2, R055 GATED/2 (both seeds) | All ten named neural comparison endpoints rehashed against their epoch receipts after deletion. No negative architectural result or played reference discarded. |
| `conventional-nnue-cglhw/raw/e006-data` and all other datasets | Required CP64/BRN corpus and provenance, reserved/consumed sample identities and exclusion inputs; no generated dataset was established disposable in this unit. |
| `brn-relational-cglhw/raw/r010-data` | Paused extraction at an explicit owner-priority gate; SQLite checkpoint remains necessary to preserve that work. |
| Entire `brn-successor-cglhw` | 3,344,651,346 bytes untouched. STATE says suspended, not completed/abandoned; W9 still has unfinished fixed-protocol assessment and explicitly pinned owner checkpoints. Includes frozen stores, native relocation copies and evidence. No inference that a large copy is disposable. |
| `docs/research/search` and all external/live user stores | Entirely outside deletion roots. Phase 4 raw failed/negative/interrupted evidence, frozen Search inputs and protected unopened studies untouched. |

Production resource packaging in `engine-core/build.gradle` reads the two retained `build-assets` payloads; UCI packaging reads the retained NNUE manifest. Source/build/training-tool and tracked documentation/manifest reference inspection found no production dependency on discarded intermediate state. Historical continuation/curve-replay references are provenance, not current operation. Frozen inference remains reproducible from retained exports; arbitrary discarded optimizer states cannot be reconstructed or hash-reverified. GPU regeneration was never promised bitwise identical.

**Availability consequence:** old completed NNUE training commands must not be rerun against these pruned output directories. The current controller scans and verifies every recorded segment, so such a replay will fail on intentionally absent binary state. Do not remove receipts, fabricate a resume chain or rerun completed work to conceal this. Any separately authorized new training must use an appropriate retained endpoint and a new immutable plan/output. This maintenance neither authorizes that work nor silently changes lifecycle semantics.

## Validation and provenance

- Initial HEAD exactly matched the supplied reference; Git was clean before inspection and immediately before deletion. Every deleted file was untracked and ignored by the existing `raw/` rule; none was force-added.
- Compute-control status immediately before deletion was `active:null`, `owned_processes:0`. Current process command lines showed no trainer/experiment; unrelated VSCode/Gradle Java processes were left alone. Completion manifests/terminal STATE records independently established which runs were finished.
- Strict file/ancestor checks rejected reparse points, links/junctions, non-regular files, hard links and paths outside the two exact authorized raw roots. Native PowerShell used literal paths, rechecked size/write time and parent boundaries, and removed only the selected files plus empty optimizer/initializer directories. No cross-shell recursive deletion occurred.
- All 7,416 targets are absent, retained keep-lists are present, and actual removed counts/bytes match the preflight plan. The three NNUE full checkpoints pass the existing `verify_checkpoint` hashes/configuration/tensor checks after deletion.
- All 12 NNUE exports match their documented whole-file and payload hashes. CP64/Gen32/Pair24 and final ONE match pinned identities; 119 retained BRN payloads in affected runs matched their receipts before deletion and remain byte-identical, with ten named neural endpoints additionally checked afterwards.
- Before/after aggregate content fingerprints match for 27,860 retained files across the three scoped directories (excluding the three STATE entry points reserved for notices). This includes all retained datasets, models, receipts, logs, protocols and raw evidence in those directories. All tracked files were unchanged before documentation. The successor STATE was not edited.
- All 1,068 original tracked JSON documents parsed and all 116 original Markdown documents decoded as UTF-8. Compact reports/configurations were retained. Complete R057 receipt chains remain intact.
- Both Phase 4 masters and both production asset manifests match their pre-cleanup hashes. Search evidence protection is established by exact path confinement, link checks and unchanged tracked state; no general Search raw-file hashing or score inspection was performed.
- No fresh runtime/UI test, engine rebuild, training, tournament or research campaign was necessary or run. Static source/build dependency checks and unchanged asset hashes are not a claim of new runtime qualification.
- `VERSION_STATE.txt` remains byte-for-byte `0.0.0`, build **91**: no shipped/runtime change, so **no_bump**. Root `CODEXLOG_CURRENT.md` was absent and was not created. No finalizer was invoked.
- Tracked-surface changes are limited to this report and dated availability notices in the conventional and relational STATE.md files. Contracts, frozen protocols, scientific results, code and production configuration were not edited. Cleanup execution left these three documentation files uncommitted; the subsequent owner-authorized closeout commits them together. No push, release or deployment occurred.

Retained-content fingerprints (sorted recursive paths, each record = repository-relative path, NUL, decimal size, NUL, SHA-256 file digest, LF; exclude each scope's STATE.md; measured before adding this report):

| Scope | Files | Aggregate SHA-256 |
| --- | ---: | --- |
| `conventional-nnue-cglhw` | 6,527 | `c652b2d54e03bbfe77dc96d2aa5a425b05374c0737f31e9772ea1421a0c33a9e` |
| `brn-relational-cglhw` | 6,028 | `681546358ecc62c75ba5c58bfecfe568e6432be73ef0bfd6804b785e194b619b` |
| `brn-successor-cglhw` | 15,305 | `826c2078170db8d5358d62078e00f6504536f6893923b6c415aa0e42ecac46e1` |

## Future accumulation: assessment only

Conventional `tools/conventional-nnue/compute_control.py::train` forces `save_rate=1`, starts a fresh immutable process directory for every superbatch and revalidates the entire completed segment set on re-entry. `tools/conventional-nnue/trainer/src/main.rs` writes three initial optimizer tensors on every process start, then merged raw/quantized weights and another complete optimizer copy. No terminal selection/retirement step exists. This explains the roughly 60.8/60.9 MiB one-head/eight-head segments and the accumulated tens of GiB.

BRN `research/src/main/java/com/ohinteractive/seedv6/core/brnpair2/PairObjectiveDiagnostic.java` publishes an optimizer state and exported Pair-2 network every epoch; the neural `*TrainingDiagnostic.java` runners similarly publish epoch/segment state. `OneTraining.java` writes complete state every configured exposure interval (1,024 principal R057 checkpoints). `tools/brn-relational/run_one_stage.py` supervises bounded execution, while `one_training_progress.py` reconciles receipts; neither finalizes storage. ONE already refuses to repeat a run with result.json and its receipt-chain reader only needs the latest state plus earlier receipts.

Managed GUI stores already have `GenerationRetention.java`, `CheckpointPruner.java` and `CheckpointStore.java`: keep ages 0..99, then logarithmic generation samples, protect Best/latest/pending references, verify payloads, and prune at most eight checkpoints per boundary. This is an active-lineage policy, not minimal completed-research storage. Standalone exports bypass it; for example `tools/admit-brn-wave6-trained.py` copies whole audited stores with `shutil.copytree`. Owner stores and suspended successor copies were not changed.

The smallest coherent follow-on is an explicit **completed-run finalization step for conventional NNUE first**, integrated with the existing controller and candidate-export/handoff workflow. It should:

1. Keep normal checkpoint/recovery behavior while a run is active, interrupted, unselected or legitimately resumable. Never prune on process exit alone. Preserve the latest verified full recovery state and enough preceding state for a safe fallback; retain explicitly selected checkpoints and failed/partial evidence.
2. Require verified terminal progress, no active owner, a settled selection and independently retained/hash-checked selected and final comparison exports before deleting anything. Capture configurations, metrics, provenance, hashes and the terminal cursor in a compact completion record.
3. Plan deletion within that one completed run, reject links/unknown files, show a dry run, then remove redundant initializer copies and unselected weight/optimizer payloads. Keep receipts/logs and specifically justified evidence. For completed runs, optimizer retention must be explicitly justified, rather than hundreds of speculative recovery points.
4. Teach `compute_control.py::train` to recognize finalized/pruned output and give a clear terminal-state response before its all-segment payload verification. Do not rewrite old receipts or silently allow a new training run in a completed output directory. Preserve the trainer's receipt-last publication and hold/resume safety.
5. Extend the same narrow pattern to completed BRN runners only after separate validation of their state/receipt readers. ONE can retain its existing receipt chain and final state; epoch-based runners need an explicit finalized-output guard. Test interruption, failed export/hash validation, selection protection, repeated finalization and link/path rejection.

Changing active checkpoint rotation, Rust initializer-audit behavior, GUI pruning rules, historical suspension gates or automatic pipeline deletion would alter lifecycle semantics and is outside this maintenance. No such code was introduced. No new research-management framework is needed.

Human actions required after this prompt: None. A future retention implementation is optional separate work, not a gate on this completed cleanup.
