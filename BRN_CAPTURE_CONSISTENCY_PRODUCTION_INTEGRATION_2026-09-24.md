# BRN capture-consistency production integration ? 2026-09-24

**Status: IMPLEMENTED; focused validation passed.** Experimental/default-off production capability, subject to user reconciliation; this is not playing-strength acceptance or a default promotion. No empirical campaign, push or deployment was performed.

## Authorized baseline and provenance

Repository: `C:\projects\seed\java\seedv6`. Starting and final HEAD: `43836daf4c55100357005477028b44e8e2c18bb0`.

The first, read-only turn stopped for unexplained sensitive inherited edits. It recovered the exact objective and checked all seven accepted reports against their commits; no implementation or validation had occurred. The user's subsequent explicit authorization resolved that blocker: preserve and build on the **current dirty tree**, not HEAD. This report replaces its blocked-preflight status while retaining that history and accepted evidence.

Before product mutation, the continuation saved all **546 tracked/non-ignored files** byte-for-byte, full porcelain status, HEAD, an inherited binary Git patch and per-file SHA-256 hashes under:

`C:\Users\Central\AppData\Local\Temp\seedv6-capture-integration-20260924`

- `baseline/`: immutable pre-task file snapshot; `baseline.json`: inventory/hashes/status, captured `2026-09-24T06:12:13.418778+00:00`.
- Baseline manifest SHA-256: `01d6fc8db58aa4b89a98705b7a38ffa0f79db2c16bfdd0f21d7c8a5083fcc4b5`.
- Baseline: **41 inherited tracked modifications, 46 inherited untracked files, plus this prior task-created preflight report; index empty**.
- Final task delta: 11 existing code files, 6 new code/test files, and this report. Nine existing code files overlap inherited edits; the other 32 inherited tracked modifications and all 46 inherited untracked files remain byte-identical. No inherited file was removed.
- `task.patch`, `final-provenance.json`, and `final/` preserve the task-only comparison, classification/hashes and after snapshots. These are local evidence, not shipped artifacts.
- Final Git state: **43 modified tracked files, 53 untracked files; empty index; unchanged HEAD**. No task commit: coherent isolation from HEAD would require absorbing inherited constructors, campaign/restart and GUI semantics. Nothing was reset, stashed, discarded, cleaned, reverted, amended or rebased.

Applicable supplied governance and `source/CHESS_SEARCH_CONTRACT.md` were read. No on-disk ancestor/repository AGENTS file was found. Exact root `CODEXLOG_CURRENT.md` and `VERSION_STATE.txt` were independently checked and absent; neither capability was activated.

## Accepted evidence

The original preflight read these reports chronologically, including experimental implementation appendices, and verified byte equality to their accepted commits. The continuation used that completed investigation and inspected the inherited implementation rather than restarting from HEAD.

| Report | Accepted commit |
| --- | --- |
| [Stand-pat diagnostic](BRN_QSEARCH_STAND_PAT_DIAGNOSTIC_2026-09-24.md) | `8ea5cb99b32330c1b85ef8a918dfb24ae5336109` |
| [Calibration diagnostic](BRN_QSEARCH_CALIBRATION_DIAGNOSTIC_2026-09-24.md) | `b1d224f3a51e5bb720a15e6339db6023291b6ff2` |
| [Initial capture training](BRN_CAPTURE_CONSISTENCY_TRAINING_DIAGNOSTIC_2026-09-24.md) | `339b0d763e12141b24918190fd5b9adc00450c5c` |
| [Campaign-faithful replay](BRN_CAMPAIGN_TARGET_CAPTURE_REPLAY_DIAGNOSTIC_2026-09-24.md) | `6eb0df1a1a48c646b1850ceaab5a397c5110eba1` |
| [Fresh generation](BRN_FRESH_GENERATION_CAPTURE_REPLAY_DIAGNOSTIC_2026-09-24.md) | `eb46291fa350c9148218ec725bfb453870785979` |
| [Replication](BRN_CAPTURE_CONSISTENCY_REPLICATION_DIAGNOSTIC_2026-09-24.md) | `0e1e1d5168b8d7c80699bb3a4bef42660b65a6ea` |
| [Fixed-checkpoint search validation](BRN_CAPTURE_CONSISTENCY_SEARCH_VALIDATION_2026-09-24.md) | `43836daf4c55100357005477028b44e8e2c18bb0` |

The final search validation was positive; earlier mixed results and remaining limitations are retained, not reclassified by this integration.

## Exact production objective and capture construction

Native side-to-move BRN values are `y_parent,y_child`; bounded pinned NNUE values are `n_parent,n_child`; `w` is the ordinary parent's actual terminal WDL:

```text
t = 0.5*w + 0.5*n_parent
e = -y_child - y_parent - 0.5*(-n_child - n_parent)
Huber_0.25(e) = 0.5*e^2                  if |e| <= 0.25
                0.25*(|e| - 0.125)       otherwise
L = 0.5*(y_parent-t)^2 + lambda*I(pair)*Huber_0.25(e)
```

No division by the Huber threshold or pair-count normalization is added. Both native-STM auxiliary output derivatives are `-lambda*clip(e,-0.25,0.25)`. Parent and child Jacobians use their own ReLU masks at the same pre-update weights; shared rows and dense gradients aggregate into **one Adam update**. The child has no Sample, WDL label, ordinary target or separate update.

Before shuffling, original sample order feeds a separate `SplittableRandom`: one uniform selection among coordinate-sorted legal captures, only for active parents and active nonterminal/non-rule-drawn children under singleton-history `HeadlessGame(...,2)`. Capturing promotions and en passant are included; quiet promotions are not. One RNG draw occurs per nonempty eligible set, none otherwise. Unpaired rows use ordinary training. No score filters or move-ranking changes were introduced.

Production indexes the independent capture stream with salt `0x510E527FADE682D1`, the existing master-seed/generation mixing convention, and records its effective seed with lambda under `brn-capture-v1`. This supplies the experiment's previously explicit capture seed without altering any existing RNG domain. Resume reconstructs all relations in original row order, then reconstructs the original shuffle and continues at the durable cursor; it does not consume a shared mutable RNG.

The existing `BootstrapPlan.loadTeacher` authenticates the pinned teacher checkpoint/hash. Auxiliary teacher evaluation reuses the already loaded teacher evaluator; no duplicate teacher model, fallback teacher or teacher mutation is introduced.

## Configuration, UI, durability and restart

**Default lambda is 0.** Existing BRN WDL/default and arbitrary blend settings remain unchanged when OFF. Enabling the option requires canonical BRN-2, NNUE blended supervision with **exactly 50% teacher weight**, and its pinned NNUE teacher. Incompatible WDL/other blends, other architectures and teacher-free frozen replay are rejected rather than silently rewriting the base objective.

`BrnCaptureConsistency` validates a binary64 finite nonnegative lambda (0, 2 and ordinary fractional values supported; negative, NaN and infinities rejected). Null configuration means restore the stored BRN selection; legacy absence resolves to OFF. Every configuration-copy helper retains the setting.

The BRN-2 architecture card exposes **Capture consistency lambda (experimental)**, initially 0, with a concise OFF/50%/restart tooltip. It uses normal numeric entry, root-specific drafts/preferences and active/closing lifecycle locks. Root switching restores the appropriate durable selection. NNUE's panel and preference keys are unchanged. Existing run settings show OFF/lambda; history and immutable generation fingerprints distinguish enabled objectives. Existing training progress/cursor/held-out loss remains explicitly the ordinary **base loss**; there is no existing durable auxiliary-training-loss channel to extend without a telemetry/schema redesign. The low-level update returns both pre-update components for validation and diagnostics.

A checksummed atomic `brn-capture-consistency.bin` sidecar stores the last BRN-2 campaign selection. OFF with no prior sidecar performs no write. The sidecar is recognized during interrupted BRN-2 initialization; invalid data fails closed. Generation/attempt/history settings add a versioned suffix only when nonzero, including the generation's capture seed. **All legacy/OFF identity strings remain unchanged.** Model, network, optimizer, bootstrap-plan and history schemas are unchanged.

Existing `GenerationAttempt` mismatch handling archives incompatible unfinished work and restarts from the last settled parent; legacy depth-only attempts also reject nonzero capture settings. The integration does not continue an unfinished generation under a different lambda. Tests verify identical lambda=2 continuation with an omitted/restored selection and lambda=2?0 archive/restart. Old plans and archived attempts retain their recorded objective.

## Validation actually run

Java 21 compiled all production and test sources into external temporary directories using the repository's cached dependencies. No inherited `app/bin` or production checkpoint data was overwritten. JUnit Platform 1.10.3/Jupiter 5.10.3 ran **75 distinct focused tests**, all passing: 10 new tests, 56 existing regression tests, then 16 tests after final focused refinements (7 repeated, 9 additional NNUE tests). No tests were skipped or aborted in those successful runs. Logs and exact selectors are in the evidence directory (`new-tests.txt`, `regression-selectors.txt`, `regression-tests.txt`, `final-rerun-tests.txt`, `build.py`, `test.py`, `TestRunner.java`). Early fixture errors were corrected; final successful results are reported here.

- **Lambda=0 dirty-baseline replay:** separately compiled immutable `baseline/` versus integrated classes, 128 identical examples each for WDL, 50:50 and 25:75 supervision, stopping/reloading at update 37 in each arm. **All 48 output files byte-identical**, including partial/final weights+Adam payloads, inference models, checkpoint manifests/root records, per-update losses/progress, cursors and legacy seed/settings identities. Combined evidence SHA-256 in both arms: `fc57985ba685871453c89dd9f62eefd24004aaa18b0a4233af8419f6d9d9fd2f`. Reproduction: `Parity.java`, `parity.py`, `baseline-classes-parity/`, `integrated-final-parity/` (final replay: `parity-final.py`).
- **Fast path:** ordinary `Brn2Trainer.train` implementation is unchanged and `Brn2SelfPlayTraining.java` remains byte-identical to baseline. Production dispatch returns to the existing loop before relation allocation, RNG construction, child/teacher work or extra differentiation. Throwing teacher sentinels and null-child/NaN auxiliary-target tests pass at zero. Additional pair scratch is allocated lazily only at the first enabled paired update. No claim of a noisy wall-clock speedup is made; OFF adds only the outer setting dispatch, not per-sample auxiliary machinery.
- **Formula/gradient:** finite differences cover node, both relation endpoint embeddings, status, local bias, board bias, output weight and bias, including parent/child-exclusive and shared features. Both signs and both Huber branches pass at lambda 0.5 and 2; moments/one-step scaling and nonfinite-candidate atomicity pass. An independent manual paired pass matches full production state and base-loss telemetry. No child base update is present.
- **Determinism/teacher:** accepted sorted/uniform selection oracle, both STM orientations, capture occupancy, en passant/promotion/no-capture/terminal cases, exact 28-row stop/reload continuation and teacher serialization immutability pass.
- **Tiny lambda=2 integration smoke:** four copies of a legal completed one-ply mate trajectory, two training rows/two held-out rows, no self-play search. Both training rows have an eligible nonterminal capture; paired updates demonstrably differ from an ordinary-only replay. Production state/checkpoint publication, pinned teacher identity, exact resume after one update, changed-lambda archive/restart, ordinary evaluation and one capped **depth-1 search** pass. Teacher checkpoint bytes remain unchanged.
- **Regression:** existing BRN-2 core/codec/target tests; BRN supervision persistence, checkpoints/retention/partial-state publication; seed-domain invariants; focused blended target/pinned-teacher/optimizer continuation methods; GUI settings, architecture isolation and lifecycle locks. Existing NNUE checkpoint compatibility plus `AdamOptimizerTest` and `NnueScoreMappingTest` pass.
- **GUI:** real Swing components executed on the EDT in headless tests: default 0, numeric/persistence round trips, root restoration, invalid values, architecture isolation and all lifecycle locks. No visible application/GUI launch or browser was needed/performed; headless control tests are not claimed as visual QA.
- **Boundaries/integrity:** 30 search-package source files, 7 core NNUE files and 8 NNUE-training files remain byte-identical to the inherited baseline. No task-created search, qsearch, stand-pat, evaluation mapping, pruning, ordering, NNUE payload/training/UI or SIMD change. Final task diff reviewed; `git diff --check` passes; index empty; inherited snapshot hashes preserved.

## Files in this task

All Java paths below are relative to `app/src/`.

New production files:
- `main/java/com/ohinteractive/seedv6/training/service/BrnCaptureConsistency.java`
- `main/java/com/ohinteractive/seedv6/training/selfplay/BrnCaptureTraining.java`

New focused tests:
- `test/java/com/ohinteractive/seedv6/core/brn2/Brn2CaptureGradientTest.java`
- `test/java/com/ohinteractive/seedv6/training/selfplay/BrnCaptureTrainingTest.java`
- `test/java/com/ohinteractive/seedv6/training/service/BrnCaptureIntegrationTest.java`
- `test/java/com/ohinteractive/seedv6/gui/BrnCaptureGuiTest.java`

Previously clean files extended:
- `main/java/com/ohinteractive/seedv6/core/brn2/Brn2Trainer.java`
- `main/java/com/ohinteractive/seedv6/training/checkpoint/CheckpointInspection.java`

Inherited modified files extended compatibly (only the baseline-to-final delta is attributable):
- GUI: `Brn2ConfigurationPanel.java`, `TrainingController.java`, `TrainingDashboard.java`, `TrainingPanel.java`, `TrainingSettings.java`.
- Training checkpoint: `CheckpointStore.java`, `GenerationAttempt.java`.
- Training service: `TrainerConfig.java`, `TrainerService.java`.

This root report was updated from its prior task-created preflight version. Inherited supervision, plan, restart-transaction, validation, history and unrelated GUI implementations were preserved; no unrelated TODO or formatting cleanup was undertaken.

## Limits and next work

Deliberately not run: full/long test suite, self-play acceptance generations, multi-generation experiments, promotion matches, tournaments, Elo tests, calibration studies, broad search benchmarks or the 24-position acceptance suite. Tiny deterministic lifecycle fixtures are implementation checks, not empirical acceptance.

**Playing strength remains untested.** The main empirical evidence still comes from one starting checkpoint/seed lineage; Fianchetto regression, mixed independent results and the used severe-regression allowance remain risks. Enabled training has additional pair/teacher/forward/backward cost, not yet measured over a campaign. Future maintenance must keep the enabled loop's base/cursor semantics aligned with the preserved ordinary loop.

After user acceptance, the recommended separate work unit is matched multi-generation lambda=0 versus lambda=2 BRN campaigns from identical network/optimizer state, matched generation settings and data semantics. Measure training/held-out and capture-delta behavior, qsearch/stand-pat/search cost over generations, training overhead, stability, candidate quality and **playing-strength retention/improvement** before considering any default change. That experiment was not started.

**Human actions required after this prompt: None.**
