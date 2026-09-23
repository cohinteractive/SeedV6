# BRN-2 configurable bootstrap supervision

Implemented 2026-09-23 against accepted strength-screen commit `b0d90a8`.
This makes blended supervision available for an independent fresh-data campaign.
**WDL remains the default.** The selected 75% teacher / 25% WDL campaign is a
provisional experiment, not a canonical target, promotion-policy decision or
playing-strength claim. No 128-generation campaign was run in this unit.

## Target and training behavior

`BrnSupervision` holds `WDL` or `NNUE_BLENDED` and a finite binary64 teacher
weight in [0, 1]. Blending requires canonical BRN-2 and **Bootstrap with NNUE**.
The narrow arithmetic and teacher reader were promoted from the accepted replay;
`Brn2SupervisionAblation` now delegates to that shared implementation.

```text
target = (1 - teacherWeight) * terminalWdlTarget
       + teacherWeight * nnueNormalizedTeacherTarget
```

The terminal target is still the existing sampled side-to-move -1/0/+1 value.
The teacher calls `NnueEvaluator.evaluate(sample.board())` on the exact stored
position and reads `boundedValue()` (native `StrictMath.tanh(raw)`). The integer
return value is discarded. No search result, centipawn mapping, calibration or
White-perspective conversion enters the target. The endpoint branches return
WDL exactly at weight 0 and teacher exactly at weight 1; 0.5 and 0.75 preserve the
accepted arithmetic. Sample records continue to contain terminal WDL.

The existing BRN-2 callback training path freezes selected targets before updates,
then uses the same shuffle, one-pass sparse Adam and scalar half-squared loss.
WDL callers continue through their original training/validation overloads.
Candidate and incumbent Best share the configured target on the same held-out
games; strictly lower configured loss promotes, and ties retain Best. No game
arena is restored for bootstrap promotion.

## Persistence, teacher identity and Resume

Fresh BRN-2 stores atomically record immutable `brn-supervision.bin`, framed as
`brn-supervision-v1`, with the mode and an exact `writeDouble` weight. This is
training metadata; BRN-2 feature schema **2** and all model/optimizer codecs stay
unchanged. Existing stores without the record mean WDL. They need no migration
or metadata rewrite and cannot be silently converted to a blend.

Each blended `bootstrap/<parent>.plan` uses `brn-bootstrap-plan-v2`: the original
parent, incumbent, generation, source, exact Generator checkpoint ID, model
SHA-256, settings and split seed, plus the supervision mode/weight. WDL plans
retain their original v1 encoding byte-for-byte. Saved `.data` continues to bind
the exact partition and samples to the plan's hash, which now covers the blended
objective. There is no second teacher selector or copied NNUE payload.

The existing workflow selects the external NNUE store's current accepted Best at
each new generation. That same immutable checkpoint generates games and supplies
teacher values. An unfinished generation reloads its recorded checkpoint even
if NNUE Best changed during downtime; the following generation selects current
Best normally. Changing an allowed stopped generation setting or Generator store
retains the existing archive-and-restart behavior and records a new generation pin.

Mode/weight are fixed for the lineage, including an interrupted initialization.
Explicit incompatible Resume requests fail before generation reconciliation or
archival, with stored/requested objectives and an instruction to select a fresh
store. Unspecified service settings restore the durable objective. Checks run
before opening the writer and again under ownership. Persisted plans are checked
against the lineage objective; missing/replaced metadata cannot reinterpret an
existing blended plan as WDL. Unknown/corrupt records fail closed.

After Stop before Candidate publication, partial optimizer work is discarded and
the exact parent, partition and pinned teacher replay. A pending published
Candidate is validated against the persisted plan's objective before new work.
An already recorded decision remains authoritative. As in the existing lifecycle,
recovery does not invent history timings for an interrupted generation; its
durable validation/promotion records remain reportable.

**The exact external teacher checkpoint must remain available until training and
the held-out decision finish, including when saved samples already exist.** If
it is missing or invalid, Resume explicitly requires restoring that checkpoint;
it never substitutes current Best. After a durable decision, replay/audit still
requires the referenced teacher payload. No new retention policy was introduced.

## GUI and observability

The existing **BRN-2 Configuration** card adds **Supervision** with **WDL** and
**NNUE blended**. The latter exposes **NNUE teacher weight (%)**, with the WDL
complement beside it. A fresh folder starts at WDL; selecting blended initially
offers 50%, and the user can set 75%. No 75% default or future architecture was
introduced. Fields follow startup/running/stopping/closing locks and remain
locked to durable supervision for any resumable lineage. Existing stored values
override GUI drafts; their exact doubles are retained without a percent-display
round trip. Fresh drafts/preferences are bound to the selected folder.

Blended validation records have the distinct kind
`brn-nnue-blended-heldout-v1`. Checksummed `history/generations-v1.tsv` uses row
schema **3** for blends, schema **2** for WDL bootstrap and unchanged schema **1**
for game-pair history. Older WDL plan/validation encodings remain exact. History
and Diagnostics identify the objective, weight, Generator/teacher store, exact
checkpoint and model hash, partition hash, Candidate/Best identities and losses,
decision and resulting Best. Checkpoint IDs/manifests identify resulting Best's
generation even when it is retained.

Both Candidate and incumbent have durable descriptive **WDL loss**, **teacher
loss**, and **configured blended-target loss**. `candidateLoss`/`bestLoss` are the
configured objective; `candidateWdlLoss`/`bestWdlLoss` and
`candidateTeacherLoss`/`bestTeacherLoss` are descriptive only. They never enter
promotion. No large metrics redesign was required.

## Validation and bounded smoke

Final focused evidence covers **95 distinct tests across 12 suites**, with zero
remaining failures, errors or skips. This is the union of the latest relevant
passing results, not a full-suite run or a sum counting retries twice.

| Focused suite/selection | Tests |
|---|---:|
| Accepted supervision replay: formulas, exact optimizer bytes, STM scalar oracle, shared held-out target | 10 |
| Existing BRN bootstrap across BRN-0/1/2 | 17 |
| Existing stopped reconfiguration, including NNUE | 27 |
| Existing bootstrap GUI | 6 |
| New blended bootstrap/data/reload/teacher/decision tests | 6 |
| New supervision GUI, preferences, locks and native Start smoke | 5 |
| New supervision codecs/initialization/corruption tests | 4 |
| History repository/codec, including schema-3 components and damaged records | 9 |
| Existing small-record strict rejection | 1 |
| Existing BRN-2 card locks and small-window rendering | 2 |
| Existing training settings validation | 6 |
| Targeted normal NNUE split/resume and real two-generation lifecycle | 2 |

The new service and native GUI smokes use isolated JUnit temporary stores and a
deterministic NNUE fixture, depth **2**, one search thread, **four games**, and a
legal mate-in-one starting position. Each completed blended generation has four
terminal samples, two training samples/updates and two held-out samples. Real
search, sample publication, teacher consumption, Adam, Candidate publication,
configured validation, promotion/retention settlement and history all execute.
These prove mechanics, not campaign throughput or strength.

The partial-update test stops after one actual update, closes the service, and
reloads with unspecified source/supervision. Its final checkpoint identity and
entire optimizer state equal an uninterrupted run byte-for-byte, with no new
search. Separate tests change accepted NNUE Best during downtime, recover a
published Candidate after injected interruption, remove/restore the exact pinned
teacher after data publication, reject changed weights without archival, preserve
legacy WDL, and reject missing blended metadata. Native GUI Start completes a
generation and locks the persisted 75% objective. Both WDL small-window and
75%-blend configuration screenshots were visually inspected without clipping.

The first wider focused pass exposed help-text clipping and a test using newly
supported schema 3 as an unknown version. The layout, future-schema test and
known-schema damaged-history handling were corrected; affected tests passed on
rerun. Early new-test compile/fixture setup errors were also corrected before
passing execution. No unresolved test failure remains.

Exact invocations/logs and copied JUnit XML are retained locally under
`app/build/brn2-blended-integration/`: `compatibility-initial.log`,
`focused-final.log`, `focused-fixes.log`, `nnue-boundary.log` and
`validation-summary.json`. The successful compatibility command selected
`Brn2SupervisionAblationTest`, `BrnBootstrapTest`, `StoppedReconfigurationTest`
and `BrnBootstrapGuiTest` (two additional unmatched selectors ran no tests).
The later focused commands select the named suites/methods in the table. NNUE's
explicit slow task selected only
`TrainerServiceTest.splitResumeMatchesUninterruptedExactAdamStateAndGenerationIdentity`
and `TrainerServiceTest.realTwoGenerationHeadlessSmokeUsesSelfPlayAdamPublisherAndArena`.
No general slow suite was run. `:app:compileTestJava`, the test-dependent
`:app:installDist`, and CRLF-aware diff/whitespace checks also passed.

Deliberately skipped as instructed: the 128-generation campaign, `fullCheck`,
broad GUI suites, long self-play, strength matches, calibration, search/fallback
work, inference optimization, BRN-3 and unrelated tests. This is a native Swing
application; no browser verification was required or substituted. Long-campaign
performance and playing-strength outcomes remain unmeasured.

## Protected state and scope

Before/after SHA-256, size and modification-time inventories match for all
**1,441 protected files**: 948 canonical BRN `training001`, 401 NNUE training,
55 accepted ablation and 37 accepted sweep files. This includes the stopped
generation-129 attempt/plan. No generation-129 Resume or accepted-store mutation
occurred. The proposed `training002` path was absent and was not created.

NNUE architecture, targets, evaluation mapping, search, checkpoint payloads and
configuration are unchanged. Shared record readers preserve the established
encoding; dedicated normal NNUE lifecycle tests passed. BRN-0/1 retain WDL and
their established behavior. Search/fallback code is untouched. The accepted
strength report already explains its 21 legal fallback moves in seven games:
the clock expired before depth 1 completed. That does not establish a deeper
cause; further investigation remains separate from this implementation.

Initial Git state was accepted `b0d90a8` plus inherited untracked `app/bin/` only.
That directory is preserved and excluded. Task files are committed as one focused
implementation; generated test stores/evidence are excluded. No push, deployment,
history rewrite or unrelated cleanup occurred. Exact root `CODEXLOG_CURRENT.md`
and `VERSION_STATE.txt` were absent; neither capability was activated or created.
The final commit hash and observed worktree state are in the completion response.
This records implementation completion, not user acceptance or campaign completion.

## Changed files

31 task files; generated payloads and inherited `app/bin/` are excluded.

```text
BRN_BOOTSTRAP.md
BRN_DIAGNOSTICS.md
BRN_SUPERVISION_TRAINING.md
README.md
app/src/main/java/com/ohinteractive/seedv6/gui/Brn2ConfigurationPanel.java
app/src/main/java/com/ohinteractive/seedv6/gui/BrnTrainingSourcePanel.java
app/src/main/java/com/ohinteractive/seedv6/gui/TrainingController.java
app/src/main/java/com/ohinteractive/seedv6/gui/TrainingDashboard.java
app/src/main/java/com/ohinteractive/seedv6/gui/TrainingHistory.java
app/src/main/java/com/ohinteractive/seedv6/gui/TrainingPanel.java
app/src/main/java/com/ohinteractive/seedv6/gui/TrainingProgress.java
app/src/main/java/com/ohinteractive/seedv6/gui/TrainingSettings.java
app/src/main/java/com/ohinteractive/seedv6/tools/search/Brn2SupervisionAblation.java
app/src/main/java/com/ohinteractive/seedv6/training/checkpoint/BootstrapEvidence.java
app/src/main/java/com/ohinteractive/seedv6/training/checkpoint/BootstrapPlan.java
app/src/main/java/com/ohinteractive/seedv6/training/checkpoint/CheckpointInspection.java
app/src/main/java/com/ohinteractive/seedv6/training/checkpoint/CheckpointStore.java
app/src/main/java/com/ohinteractive/seedv6/training/checkpoint/SmallRecord.java
app/src/main/java/com/ohinteractive/seedv6/training/checkpoint/ValidationRecord.java
app/src/main/java/com/ohinteractive/seedv6/training/history/GenerationRecord.java
app/src/main/java/com/ohinteractive/seedv6/training/history/HistoryCodec.java
app/src/main/java/com/ohinteractive/seedv6/training/history/HistoryRepository.java
app/src/main/java/com/ohinteractive/seedv6/training/selfplay/Brn2SelfPlayTraining.java
app/src/main/java/com/ohinteractive/seedv6/training/service/BrnSupervision.java
app/src/main/java/com/ohinteractive/seedv6/training/service/TrainerConfig.java
app/src/main/java/com/ohinteractive/seedv6/training/service/TrainerService.java
app/src/main/java/com/ohinteractive/seedv6/training/validation/HeldOutLoss.java
app/src/test/java/com/ohinteractive/seedv6/gui/Brn2SupervisionGuiTest.java
app/src/test/java/com/ohinteractive/seedv6/training/checkpoint/BrnSupervisionPersistenceTest.java
app/src/test/java/com/ohinteractive/seedv6/training/history/HistoryRepositoryTest.java
app/src/test/java/com/ohinteractive/seedv6/training/service/Brn2BlendedBootstrapTest.java
```

## Human actions required after this prompt

**Blocking the fresh experiment:** run the rebuilt application. An already running
process cannot acquire these code changes. The verified local build can be opened
from the repository with:

```powershell
.\app\build\install\seedv6\bin\seedv6.bat gui
```

In the Training workspace:

1. Set **Network Architecture** to **BRN-2**, then open **Configuration**.
2. Enter `E:\SeedV6-Networks\BRN\BRN-2\training002` in
   **BRN checkpoint store (student)**. This currently absent sibling is the
   recommended fresh store; Start creates it. Keep `training001` unselected.
3. Set **BRN Training Source** to **Bootstrap with NNUE** and
   **NNUE Generator Store** to `E:\SeedV6-Networks\NNUE\training` (the intended
   trained NNUE Best source).
4. In **BRN-2 Configuration**, set **Supervision** to **NNUE blended** and
   **NNUE teacher weight (%)** to **75**; verify **WDL: 25.00%**.
5. Set **Training depth** to **2**, **Search threads** to **6**, and
   **Games / generation** to **64**. Preserve **Initial learning rate** 0.001,
   **Opening min. plies** 0, **Opening max. plies** 8, **Samples / game** 32 and
   **Maximum game plies** 1024. Validation pairs are inactive for bootstrap.
6. Set **Generations (0 = unlimited)** to **128**, then click
   **Start / Resume Training**. Start applies the settings. A fresh uninterrupted
   run stops automatically after 128 completed generations; verify generation 128
   is settled in History/Diagnostics before further work.

**Blocking later campaign analysis:** complete that user-run campaign and provide
its settled evidence. **Stop Training** can pause safely. On a normal stopped
Resume, use the same store and set the generation limit to the remaining count
(128 minus completed generations), because the existing limit counts new
generations per service run, not the lineage total. Startup recovery of a pending
decision does not count toward that per-run limit. No permanent target/policy or
playing-strength conclusion follows automatically from starting or finishing it.
