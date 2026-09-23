# BRN-2 handcrafted position generation

Implemented 2026-09-23 in `C:\projects\seed\java\seedv6`, following accepted
`89d033af474dbf525b0242c7f7a986fbbe548d49`. This is the final implementation unit
before a fresh GPT handoff. **New BRN-2 lineages default to Handcrafted generation
and WDL supervision.** NNUE generation and NNUE blended supervision remain
independent options. No long campaign was started.

The former training003 continuation instructions in the historical independent
75% preflight are superseded. training003 remains protected evidence of exactly
one settled independent-seed, NNUE-generated generation. Do not continue,
repurpose, migrate or delete it.

## First long baseline: settled supervision decision

Selected and preflighted 2026-09-23, after implementation commit
`f109b7172f0c30903e441227ad26bdf956c5ec3c`: **Handcrafted position generation,
75% pinned NNUE static teacher supervision, and 25% terminal WDL supervision**.
The student remains `seedv6.brn.2`, feature schema **2**, width **32**. This is an
explicit campaign selection; the software's Handcrafted + WDL defaults are unchanged.

NNUE **does not generate positions** in this baseline. It remains part of the
learned target: `.25 * terminalWdlTarget + .75 * nnueNormalizedTeacherTarget`.
This is a **teacher-stabilized baseline**, not evidence of an NNUE-independent
BRN. WDL-only handcrafted supervision remains a separate subsequent scientific
question. The 75% choice is provisional scientific context, not a claim of
statistically established superiority or handcrafted-data generalization.
**No long campaign was started, and no final campaign lineage was created.**

The exact teacher is established independently by the
[75% weight sweep](BRN_SUPERVISION_WEIGHT_SWEEP.md#exact-source-target-and-replay-controls)
and [normal-training replay diagnostics](BRN_FRESH_75_TRAINING_DIAGNOSTICS.md#exact-campaign-state-and-pinned-models):

```text
Store: E:\SeedV6-Networks\NNUE\training
Checkpoint: g000074-s000008942-2e22ccbfdeb18f5f26f91f0df183411ac1d01844ccd43e56e9005ee3f5837bc6
network.nnue SHA-256: 3279ff72654f56c8f5ce3cc89cc55d1f253de6ae9df00bd61a41293f57dd16a9
training.state SHA-256: 453b7259d4e8041fa4abb8cc0887540434871d2b151c33676f36e749e0c6cb34
Generation: 74; optimizer step: 8942
```

Read-only production readers verified all **131 training002 plans** against this
teacher and the .75 objective. The actual teacher payload hashes match, and the
accepted Best currently resolves to this same checkpoint. Current Best was a
compatibility check, not the source of the teacher decision; no substitute was used.

The existing implementation pins **each generation**, not a campaign-wide checkpoint
selector. Resume of unfinished work verifies that generation's exact ID/hash;
the next new generation reads accepted Best from the persisted teacher store.
Keep this protected teacher store unchanged for the campaign and recheck the above
identity before starting/restarting it. A changed Best does not authorize a newer
teacher for this baseline. No pinning semantics or NNUE behavior were changed.

## Architecture and score isolation

The reused production evaluator is `com.ohinteractive.seedv6.core.Eval.evaluate(long[])`,
selected through `SearchEvaluation.handcrafted()` and its existing `HandcraftedState`.
`SelfPlayRunner` now accepts that existing `SearchEvaluation` definition as well as
its original network overload. Both enter the same `RootParallelSearch` /
`IterativeDeepeningSearch`, `drive`, `HeadlessGame` and `TrajectorySampler` path.
`SelfPlayBatch.generateHandcrafted` supplies that evaluator to the same batch loop.
No evaluator scoring, search algorithm, inference, score mapping or ordinary
engine evaluator-selection implementation changed.

```text
TrainingSource -> production search -> game trajectories -> sampled boards
                                               terminal result -> sample WDL
BrnSupervision + separately pinned NNUE teacher -> configured training target
sampled boards + configured target -> BRN updates and Candidate/Best held-out loss
```

`GameTrajectory.Position` contains board, ply and played move; there is no evaluation
field. `TrajectorySampler.Sample` contains the six-long board and exact terminal
side-to-move -1/0/+1. Sampling derives this value from `GameResult.target(sideToMove)`.
Incomplete/capped games supply no targets. `SelfPlayRunner.drive` passes completed
`SearchResult` only to the optional live presentation after a move; it never passes
scores to `TrajectorySampler` or target construction.

WDL still trains/validates through its existing overloads. For NNUE blends:

```text
target = (1 - teacherWeight) * terminalWdlTarget
       + teacherWeight * nnueNormalizedTeacherTarget
```

`BrnSupervision.teacherValue` calls `NnueEvaluator.evaluate(sample.board())`, discards
its integer return, and reads native `boundedValue()` (`StrictMath.tanh(raw)`).
`TrainerService` obtains this evaluator only via `plan.loadTeacher`, independently
of the generator. Weight 0/1 endpoint arithmetic is unchanged. Sample files still
contain WDL, not teacher or handcrafted scores. Candidate and incumbent use the
same configured target on the same held-out games. Strictly lower configured
half-squared loss promotes; ties retain Best. Descriptive WDL/teacher component
losses do not affect this decision.

Tests intercept the actual training callback, assert exact WDL or .25 WDL + .75
normalized NNUE targets, independently recompute persisted held-out losses and
promotion decisions, and replay handcrafted trajectories to compare every sampled
label with its actual terminal side-to-move result. No handcrafted term is present.

## Configuration and durable identities

`TrainingSource(mode, generatorStore)` gains `HANDCRAFTED`; the existing
`NNUE_BOOTSTRAP` identifier is retained. `bootstrap()` now means the external
position-generator / held-out-validation workflow; `nnue()` specifically identifies
NNUE generation. `SELF_PLAY` remains supported for historical BRN stores and BRN-0/1;
no new BRN-self-generation mode is introduced. Fresh BRN-2 GUI choices expose only
Handcrafted and NNUE. `TrainerConfig.teacherStore` and the corresponding GUI settings
field are distinct from `TrainingSource.generatorStore`.

| Durable record | Semantics |
|---|---|
| `training-source.bin`, `training-source-v1` | Source enum and generator store. HANDCRAFTED has an empty generator store. BRN-2 source and generator-store selection lock to the lineage. |
| `brn-supervision.bin`, unchanged `brn-supervision-v1` | Immutable WDL/NNUE_BLENDED and exact binary64 weight. |
| `brn-teacher.bin`, new `brn-teacher-v1` | Immutable NNUE teacher-store selection for a new blended lineage; absent for WDL. |
| `brn-run-seeds.bin`, unchanged `brn-run-seeds-v1` | New BRN-2 lineages always persist effective master/self-play seeds. Blank optional data seed means the master seed for self-play, not an unlocked seed. |
| `bootstrap/<parent>.plan`, new `brn-bootstrap-plan-v3` | Parent/incumbent IDs, generation, source mode/store, `generatorId`, `generatorHash`, settings, split seed, supervision mode/weight, separately named `teacherStore`, `teacherId`, `teacherHash`. |
| Existing `.data` encoding | Exact plan hash, game partition, samples, game statistics and measured generation time. No new score/target fields. |
| `brn-generated-heldout-v2` validation kind | Source and distinct generator/teacher identities, configured loss, and blend component losses. |
| History row schema 4 | Explicit `generatorMode`, generator identity, teacher identity and objective/components; existing generation/settings/decision fields remain. |

Handcrafted plans require empty generator checkpoint fields; WDL plans require
empty teacher fields. NNUE roles require checkpoint IDs and model SHA-256 values.
Feature schema remains **2**. BRN and NNUE model/optimizer codecs are unchanged.
Generation plans supply the self-play seed, starting FEN and full search/training
settings; history supplies depth, threads, games, Candidate/incumbent/resulting Best
and promotion/retention. Together these preserve the requested audit facts without
a metrics redesign.

Each new generation independently selects accepted Best from the required NNUE
store(s). If both roles use the same store, one selected snapshot can supply both,
but both identities are recorded explicitly. Checkpoints remain external; no NNUE
payload is copied into a BRN store.

## Backward compatibility and Resume

Old v1 plans decode as NNUE generation + WDL with no teacher. Old v2 plans decode
as NNUE generation + blended supervision: the original generator store/ID/hash
also populate the explicit in-memory teacher identity. The record's version is
retained, so re-encoding and plan/data hashes remain byte-exact. Old held-out
validation kinds and history schemas 1/2/3 remain readable with their original
encodings. An old blended lineage without `brn-teacher.bin` retains its historical
NNUE-generator teacher selection; no migration is required. Its subsequent plans
retain v2 semantics. Missing supervision still means legacy WDL. Missing source
on a genuinely pre-bootstrap BRN store still means its historical BRN self-play.
Missing source on a BRN-2 store with generation plans fails clearly.

Read-only production readers inspected all protected canonical metadata:

| Store | Source | Objective | Plans round-tripped to original SHA-256 | Settled history rows |
|---|---|---|---:|---:|
| training001 | NNUE | WDL | 129 (including unfinished attempt) | 128 |
| training002 | NNUE | teacher .75 / WDL .25 | 131 | 131 |
| training003 | NNUE | teacher .75 / WDL .25; master 1/data 2 | 1 | 1 |

Every historical blend has equal generator/teacher checkpoint identity as required.
No protected store was opened through a training writer.

BRN-2 source/store, persisted seeds, supervision mode/weight and teacher-store
mismatches fail before reconciliation/archival, with a second check under exclusive
ownership. Null/unspecified service settings restore durable selections. Missing
or corrupt required metadata fails closed. An unfinished generation reloads its
exact teacher/generator by checkpoint ID and verifies model SHA-256; it never
substitutes current Best. Required missing teachers fail even if samples are saved.
Other permitted stopped settings retain the existing explicit archive/restart
notice. A completed decision remains authoritative. BRN-0/1 retain their previous
source-reconfiguration behavior; ordinary NNUE training is unchanged.

A partial-update handcrafted blend was stopped after one real Adam update, closed,
and reloaded with unspecified source/supervision/seeds/teacher selection. It reused
saved samples without new search and matched uninterrupted Candidate identity and
complete optimizer bytes. Changed source, seed, supervision, weight and teacher
store were rejected without changing the attempt or creating a restart archive.
Removing/restoring the exact teacher demonstrated fail-closed recovery. A separate
test advanced accepted teacher Best during downtime: resumed g1 used the original
pin, and new g2 selected the replacement. Another test used different NNUE stores
for generator and teacher and verified loss against the independent teacher.

## GUI and seeds

The source controls appear inside **BRN-2 Configuration**. Actual labels are
**Position generation** with **Handcrafted** / **NNUE**, then **Supervision** with
**WDL** / **NNUE blended**. **NNUE Generator Store** is visible/relevant only for
NNUE generation. **NNUE Teacher Store**, **Browse...**, and **NNUE teacher weight (%)**
remain available for a fresh blend with either generator. The complement displays
as, for example, **WDL: 25.00%**. Choosing blend initially offers 50%; 75% is only an
explicit test selection, not a default.

Existing store values override stale GUI drafts. Source/teacher/objective fields
follow lifecycle locks and remain locked for a resumable BRN-2 lineage. Folder-bound
preferences retain distinct role selections. History and Diagnostics name generator
and teacher separately. New native Swing Start testing settled a temporary
handcrafted + .75 blend and verified the durable locks. Native configuration images
were inspected; the unused generator label was hidden along with its controls.

Seed domain salts and derivation are unchanged from the accepted independent-seed
unit. Only SELF_PLAY uses `BrnRunSeeds.dataSeed`; SHUFFLE/HOLDOUT retain the master
stream. Indexed game RNGs choose opening lengths/moves; sampling remains evenly
spaced and result-independent. No stream was collapsed. Existing stores without
seed metadata retain their legacy behavior and are not rewritten merely by reading.
Seed-dependent position changes require nonzero opening randomness, as before.

## Bounded deterministic preflight

Four independent temporary production lineages used BRN-2 schema 2, depth **2**,
one search thread, **8 games**, opening plies **0..1**, at most **8 samples/game**,
maximum **128 plies**, one online shuffled Adam pass at .001, master seed **1**, and
one generation. The legal FEN was `7k/8/8/5K2/8/8/2Q5/8 w - - 0 1`.
All eight games completed in every run; all generations trained, validated and
settled normally. NNUE used the deterministic initialized-17 temporary fixture.
This is a mechanics comparison, not a quality or playing-strength comparison.

| Run | Data seed | Training / held-out samples | Updates | Decision |
|---|---:|---:|---:|---|
| Handcrafted A | 2 | 43 / 16 | 43 | RETAIN_INCUMBENT |
| Handcrafted B | 2 | 43 / 16 | 43 | RETAIN_INCUMBENT |
| Handcrafted C | 3 | 48 / 16 | 48 | PROMOTE |
| NNUE | 2 | 43 / 16 | 43 | PROMOTE |

Handcrafted A/B have identical plan hash
`f42cb02ed6ed26b4fb1eb733c36e721e5fe16698e33476b878db323758234410`,
Candidate identity and the content/sample hashes below. C's plan hash is
`a7679cf4658c9561aee4a3c1d77161224ca4d82d1179b5d3cf87b25d58e291ba`.

| SHA-256 content | Handcrafted A = B | Handcrafted C (seed 3) | NNUE (seed 2) |
|---|---|---|---|
| Batch content | `f0b6eeef1319472e1a9d6483911a845aae51215c38aeeacb40dcdd9192003009` | `e0df3bd6ff621413b0e05ecbb25eba18c044feae4de77114761e5537c396120f` | `b99c36c25f45dbff7ed92e6175cddbdf183cde3b26ee6d5aa71da3b02a506123` |
| Training samples | `110e0377fb69593016f2a6feb84148621e8269b73feef60b91d8e5b21d6dbf63` | `7ca60a4b7eed655d1f7a15217d990ee0ad7637c36d070c5a83b1aa180fd11057` | `481c897a319f1ab6683495dcbf29fd7e6ad1d44ad11e18673ae3deb37555fe0b` |
| Held-out samples | `80eee75747cf3233af0f2b7bb8d1573b3d0f48090fb140755f5b88fddc81a14f` | `c3ba485e190c40d19180494b242b04e779ae26b4a403a8199903779150518f91` | `8c2fc9e2b815ffa6dd4a2c5bdaaefdea7741958f9505643b7c91e9194c865187` |
| Training positions only | `f43ec7eae1c1127f922d8f730a4108066884a9d8f81d190d35bc04d93e1f3cce` | `5bd81c2a6aec6bfcf6478b921d724dfb17b208ac79c676a537c48633f45dc1b6` | `eb446470a8302f2ec4b11d989f3ab965ec8ee8e71401fe801829e4716ac5812c` |

Sample hash encoding is DataOutputStream int count, then six board longs and WDL
double for each sample; position hashes omit the double. Batch content hashes use
production BootstrapData serialization with planHash replaced by 64 zero characters
and generationNanos by zero. Physical data envelopes include measured elapsed time,
so byte-identical envelope hashes are not a deterministic-data requirement. Ordered
positions and complete samples differ for both the changed seed and changed generator.
The initial forced-mate fixture correctly produced the same source-dependent batch;
it was unsuitable for testing generator differences and was replaced by this less
forced endgame. The mate fixture remains useful for small lifecycle tests.

## Validation and protected state

**110 distinct focused tests across 15 suites passed, with zero remaining failures,
errors or skips.** This is the union of the latest relevant passing results, not
an inflated sum of reruns. Repeated JUnit parameter display names are counted as
separate executed cases.

| Suite / selected methods | Tests |
|---|---:|
| Brn2SupervisionGuiTest | 5 |
| Brn2TrainingGuiTest | 2 |
| BrnBootstrapGuiTest | 5 |
| BrnHandcraftedGuiTest | 3 |
| BrnRunSeedsGuiTest | 3 |
| TrainingSettingsValidationTest | 6 |
| BrnSupervisionPersistenceTest | 5 |
| HistoryRepositoryTest | 9 |
| Brn2TrainingTargetsTest | 3 |
| SelfPlayRunnerTest | 8 |
| Brn2BlendedBootstrapTest | 7 |
| BrnBootstrapTest | 17 |
| BrnHandcraftedGenerationTest | 5 |
| BrnRunSeedsTest | 5 |
| StoppedReconfigurationTest | 27 |

The BRN-2 GUI selection includes only the card/lifecycle-lock and small-window
methods; the other suites listed were selected directly. BRN-0/1 compatibility,
normal NNUE stopped reconfiguration, real NNUE/handcrafted generation, data/teacher
recovery, legacy codec bytes, all four generator/supervision combinations, seed
separation, model schema preservation, target arithmetic and configured-loss
promotion are covered. `:app:compileTestJava`, test-dependent `:app:installDist`,
and CRLF-aware `git diff --check` passed. Both WDL small-window and handcrafted
blended native screenshots were visually inspected without clipped card content.

The initial pass exposed stale history-version/error-message expectations. The
first source-difference fixture was too forced (documented above). The wider GUI
pass exposed a stopped-state source refresh regression for BRN-0/1; reloading is
now limited to BRN-2, and asynchronous GUI checks wait for durable reads. The seed
GUI fixture now requests the stored source rather than an incompatible override.
All affected checks passed on rerun. An initially unmatched small-window selector
was corrected and the actual method explicitly passed. No failure was suppressed.

Commands/logs, copied JUnit XML, screenshots, content hashes, read-only historical
inspection and inventories are local ignored evidence under
`app/build/brn2-handcrafted/`. The main focused command and narrower final rerun
are recorded in `focused-final.log` and `final-recheck.log`; the latest-case summary
is `validation-summary.json`. Tests use disposable temporary stores; no generated
training payload is needed in the commit.


Deliberately skipped as requested: long/128-generation campaign, training003
continuation, fullCheck, broad GUI/slow suites, strength matches, calibration,
inference optimization, BRN-3 and unrelated tests. This is native Swing; no browser
verification was required. No campaign-level quality, generalization, throughput
or playing-strength conclusion is established by these bounded tests.

Before/after SHA-256, size, modification-time and membership inventories match for
all **2,423 protected files**: training001 948, training002 959, training003 23,
NNUE training 401, accepted ablation 55 and accepted weight sweep 37. This includes
the preserved unsettled training001 attempt. No protected writer was opened and
no protected payload or metadata changed.

Initial Git state was accepted `89d033a` plus inherited untracked `app/bin/` only.
No inherited files were reset, discarded or staged. Generated IDE output remains
untracked; it is not claimed byte-unchanged. The inventory observed no removed
`app/bin/` files, nine added files and 49 changed contents while the IDE Java
language server was active; individual writes were not traced. No temporary training payloads are
committed. Exact root `CODEXLOG_CURRENT.md` and `VERSION_STATE.txt` were absent and
were not created. No push, deployment, amend, rebase or history rewrite occurred.
The implementation, tests and report form one focused commit; its hash and final
Git state are supplied in the completion response. This records implementation
completion, not final user acceptance or campaign completion.

## Changed files

33 task files; generated payloads and inherited `app/bin/` are excluded.

```text
BRN_BOOTSTRAP.md
BRN_DIAGNOSTICS.md
BRN_HANDCRAFTED_POSITION_GENERATION.md
README.md
app/src/main/java/com/ohinteractive/seedv6/gui/Brn2ConfigurationPanel.java
app/src/main/java/com/ohinteractive/seedv6/gui/BrnTrainingSourcePanel.java
app/src/main/java/com/ohinteractive/seedv6/gui/TrainingController.java
app/src/main/java/com/ohinteractive/seedv6/gui/TrainingHistory.java
app/src/main/java/com/ohinteractive/seedv6/gui/TrainingPanel.java
app/src/main/java/com/ohinteractive/seedv6/gui/TrainingProgress.java
app/src/main/java/com/ohinteractive/seedv6/gui/TrainingSettings.java
app/src/main/java/com/ohinteractive/seedv6/training/checkpoint/BootstrapEvidence.java
app/src/main/java/com/ohinteractive/seedv6/training/checkpoint/BootstrapPlan.java
app/src/main/java/com/ohinteractive/seedv6/training/checkpoint/CheckpointInspection.java
app/src/main/java/com/ohinteractive/seedv6/training/checkpoint/CheckpointStore.java
app/src/main/java/com/ohinteractive/seedv6/training/checkpoint/ValidationRecord.java
app/src/main/java/com/ohinteractive/seedv6/training/history/HistoryCodec.java
app/src/main/java/com/ohinteractive/seedv6/training/history/HistoryRepository.java
app/src/main/java/com/ohinteractive/seedv6/training/selfplay/SelfPlayBatch.java
app/src/main/java/com/ohinteractive/seedv6/training/selfplay/SelfPlayRunner.java
app/src/main/java/com/ohinteractive/seedv6/training/service/BrnSupervision.java
app/src/main/java/com/ohinteractive/seedv6/training/service/TrainerConfig.java
app/src/main/java/com/ohinteractive/seedv6/training/service/TrainerService.java
app/src/main/java/com/ohinteractive/seedv6/training/service/TrainingSource.java
app/src/test/java/com/ohinteractive/seedv6/gui/Brn2TrainingGuiTest.java
app/src/test/java/com/ohinteractive/seedv6/gui/BrnBootstrapGuiTest.java
app/src/test/java/com/ohinteractive/seedv6/gui/BrnHandcraftedGuiTest.java
app/src/test/java/com/ohinteractive/seedv6/gui/BrnRunSeedsGuiTest.java
app/src/test/java/com/ohinteractive/seedv6/training/checkpoint/BrnSupervisionPersistenceTest.java
app/src/test/java/com/ohinteractive/seedv6/training/history/HistoryRepositoryTest.java
app/src/test/java/com/ohinteractive/seedv6/training/service/Brn2BlendedBootstrapTest.java
app/src/test/java/com/ohinteractive/seedv6/training/service/BrnHandcraftedGenerationTest.java
app/src/test/java/com/ohinteractive/seedv6/training/service/BrnRunSeedsTest.java
```

## Exact-teacher 75/25 bounded preflight

This subsequent configuration work used the unchanged implementation at `f109b71`.
No production-code or checked-in test correction was needed. A local assertion probe
used the existing `TrainerService.Operations` test seams, delegating real generation,
Adam updates, publication and held-out validation to production implementations.
Its only student writer was the fresh disposable location:

```text
C:\projects\seed\java\seedv6\app\build\brn2-handcrafted-75-preflight\disposable-student
```

Configuration: BRN-2/schema 2/width 32, Handcrafted source with empty neural-generator
store/ID/hash, NNUE_BLENDED at .75, and the exact external teacher above. The bounded
fixture used depth **2**, **one thread**, **8 games**, opening plies **0..1**, maximum
**128 plies**, maximum **8 samples/game**, one shuffled online Adam pass at **.001**,
maximumGenerations **1**, and FEN `7k/8/8/5K2/8/8/2Q5/8 w - - 0 1`.
These small fixture bounds are preflight settings, not prescribed campaign sizing.

Fresh random preflight inputs were master **2963257644693090344** and data
**3333693714392095861**. Separately selected, **unconsumed campaign inputs** are
master **1860967260734946789** and data **2919300965553130346**; record these in
the new campaign's Model / run seed and Self-play data seed fields. Production
`TrainerConfig.seed` derives the independent generation/domain streams as follows:

| Generation-1 stream | Disposable preflight | Unconsumed campaign configuration |
|---|---:|---:|
| SELF_PLAY | 7719046991438532427 | 6812222746938875882 |
| SHUFFLE | 8005726907504823269 | 6372574506065030920 |
| HOLDOUT | -4909585366111090725 | 8941885597769070687 |

All three streams are distinct and differ between the preflight and campaign
configuration. The architecture's fixed model initialization seed is unchanged.
The master/data inputs persist in `brn-run-seeds.bin`; generation settings store
effective self-play/shuffle seeds and plan v3 also stores the holdout split seed.

The runtime probe rejected either neural-generator overload if invoked. It observed
one real `generateHandcrafted` call, a null batch generation network, and **zero
neural-generator calls**. All eight games completed (one White win, seven draws),
yielding **45 training and 16 held-out samples**. For every training and held-out
callback, an independently loaded g74 `NnueEvaluator` supplied `boundedValue()`;
the observed target matched `.25 * WDL + .75 * teacher` bit-exactly. All 61 targets
differed from WDL alone. An independent Handcrafted trajectory replay reproduced
every saved board and its actual terminal side-to-move label, and checked the WDL
selector exactly. Neither WDL nor blended targets contained handcrafted scores.
Persisted held-out losses were independently recomputed against the same teacher.

The first JVM stopped after **one actual Adam update**, without settling g1, then
closed. A separate JVM used normal Resume with source, supervision, teacher and
seed selections unspecified and a stale caller master seed of 999. Durable values
were restored; no self-play was regenerated. Training restarted from the durable
parent over the saved batch, as designed, and settled exactly **one generation / 45
updates**. Source, objective, teacher-store and run-seed record bytes, plan identity,
and the entire data file matched their pre-stop SHA-256 values. Plan v3 retained
empty generator identity and the exact separate NNUE teacher pin. History retained
the same source and teacher. No second disposable generation was started.

```text
Plan hash: 0c208b103b200448ed86ae2a3d12003b896a0744f7abd6ab725bce3f4820841f
Data file SHA-256: c38da70570ce28fca38a0891c6082afc654fc55553987f600af9d7585ab19b29
Candidate: g000001-s000000045-b11b66187ec824550708051d074f77de328ba4f9af53052b0685cc04f00c11a4
Model SHA-256: 4ca379fbda6d4e4e48daca72247abe473cf9fab97c9f4e0645341d3afc885ce0
Optimizer SHA-256: 55b9f50a354492f225b68563daa750663a7d154091834cdb3ca61fa0ab6d6c3b
Held-out loss: Candidate 0.007219572599038561; incumbent 0.027914167728651136
Decision: PROMOTE
```

The candidate was loaded with `CheckpointStore.readSnapshot`, then the ordinary
`NetworkModel.evaluation` / `AlphaBetaPvsSearch` / `IterativeDeepeningSearch` path.
Both depth-2 searches completed within 100,000-node / 5-second per-search bounds:
standard start **165 nodes / 2 qnodes**, fixture **92 nodes / 6 qnodes**. There was
no immediate model-load, serialization, configuration or qsearch failure. This
two-position software check establishes no broad search stability or strength claim.

Validation commands and full local probe sources/logs are retained under the ignored
`app/build/brn2-handcrafted-75-preflight/` directory. `Preflight.java` was compiled
against `app/build/classes/java/main` using JDK 21 `javac`; separate `java -Xmx1024m`
invocations ran `Preflight inspect`, `fresh`, `resume` and `labels`. The latter
replayed labels and derived the unconsumed campaign seeds without creating a store.
The exact focused Gradle selection is:

```powershell
.\gradlew.bat :app:test --tests '*BrnSupervisionPersistenceTest' --tests '*BrnRunSeedsTest' --tests '*Brn2TrainingTargetsTest' --tests '*Brn2BlendedBootstrapTest' --tests '*BrnHandcraftedGenerationTest.handcraftedBlendUsesSeparateStaticTeacherAndResumePreservesExactOptimizer' --tests '*BrnHandcraftedGenerationTest.nnueGenerationCanUseAnIndependentTeacherStore' --tests '*BrnHandcraftedGenerationTest.pinnedTeacherSurvivesBestChangeAndNextGenerationPinsNewBest' --tests '*SelfPlayRunnerTest' --console=plain
```

**31 focused tests in six suites passed, zero failures/errors/skips**, in 3m 51s.
The selection covers plan v1/v2/v3 compatibility, independent source/teacher roles,
seed persistence, exact optimizer replay, missing/changed teacher handling, immutable
lineage settings, WDL/75% target construction and bounded self-play. XML copies and
counts are retained in `focused-xml/` and `validation-summary.json` alongside the
probe evidence. `git diff --check` passed. Root `CODEXLOG_CURRENT.md` and
`VERSION_STATE.txt` remain absent. Only this report is committed for the preflight;
inherited untracked `app/bin/` and generated evidence are excluded. No push occurred.

The before/after inventory matched SHA-256, byte size, modification time and file
membership for all **2,331 protected files**: training001 **948**, training002 **959**,
training003 **23**, and NNUE training **401**. All protected access was read-only;
no historical store was resumed, repurposed or opened through a training writer.

## Remaining work and human actions

**READY_FOR_LONG_HANDCRAFTED_75_25_BASELINE**: the configuration and bounded software
preflight are technically ready for the user to start a separate real campaign.

The supervision decision is settled. Use a **new, empty lineage** for the eventual
long campaign with Handcrafted, NNUE blended, teacher weight **75**, WDL **25**, the
exact protected teacher above and the unconsumed campaign seeds. The disposable
student is test evidence and must not become that campaign. training001/002/003 and
the accepted experimental stores retain their historical semantics.

No final campaign directory was created, no GUI preferences were changed, and no
campaign generations were consumed. No long training, strength matches, extensive benchmarking, fullCheck,
broad GUI tests or browser verification were run. No GUI or search code changed;
no browser work was required. Long-run behavior, learning quality and independent
generalization on handcrafted-generated positions remain unmeasured.

Human actions required after this prompt: **None for the completed preflight**.
Starting the real campaign is a separate, non-blocking future user action; this
report does not start it or claim campaign completion or empirical acceptance.

## Completed Handcrafted 75/25 campaign: retrospective results (2026-09-23)

**Next scientific step: a controlled Handcrafted + WDL-only supervision experiment,
with checkpoint search diagnostics. No additional measurement is required before
that experiment.** This campaign learned its configured objective, but did **not**
establish uniformly reliable BRN search: accepted g96 reproduces a bounded qsearch
failure, and final g128 remains substantially more qsearch-heavy than the historical
75/25 baseline. There is no evidence warranting an architecture change or another
supervision-weight selection before isolating the WDL-only question. No experiment
was started by this analysis. These findings supersede the unmeasured-campaign and
readiness statements above, while preserving the earlier preflight as history.

### Identification and integrity

The unique matching store among the canonical BRN-2 stores is
`E:\SeedV6-Networks\BRN\BRN-2\training004`. It contains exactly 128 settled
generations, 128 v3 plans/data pairs, 128 validations and 65 promotions (63
retentions). The user confirmed this identification after initially suggesting
training003; that protected store still contains only its one NNUE-generated
preflight generation. training001 is WDL/NNUE-generated, training002 has 131
75/25 NNUE-generated generations, and the older training store does not match.

Every generation retains `seedv6.brn.2`, schema 2, width 32, Handcrafted generation,
depth 4, six threads, 64 requested games, and NNUE_BLENDED weight .75. All neural
generator fields are empty. Every plan/history/validation retains the exact g74
teacher checkpoint and model SHA-256 documented above, from
`E:\SeedV6-Networks\NNUE\training`; its actual model/optimizer load and current
accepted Best agree. No teacher substitution is evidenced.

**Configuration discrepancy from the proposal:** persisted master/data seeds are
**1 / 1**, not proposed `1860967260734946789 / 2919300965553130346`. All 128 saved
settings and SELF_PLAY/SHUFFLE/HOLDOUT seeds exactly match production derivation
from the actual 1/1 pair. This is not within-campaign drift. Settings throughout:
standard starting FEN, opening plies 0..8, maximum 32 samples/game, 1,024 maximum
plies, no move time/node limit, one shuffled online pass, minibatch 1. All surviving
optimizer headers retain Adam .001/.9/.999/1e-8. Initial model and optimizer bytes
equal training002's fixed architecture initialization, despite different g0 IDs
because the manifest includes depth.

Production read-only readers verified complete Candidate ancestry, previous-Candidate
continuation after rejection, cumulative optimizer steps, plan/data hashes,
history/validation component equality, strict-loss decisions and the accepted Best
publication chain. All 129 manifests are intact; 103 model/optimizer pairs survive
normal retention, and all 206 payload lengths/SHA-256 values match their manifests.
The other 26 generations have historical metadata, not reloadable payloads.
The final attempt is settled g128 with an empty restart notice; staging is empty,
with no g129 plan, restart archive or recovery transaction. Maximum adjacent
completion-to-start gap is .236046 seconds. No semantic restart/change is evidenced;
historical process Start/Stop events and executable revision were not persisted,
so uninterrupted process execution is not proved.

```text
Accepted Best = latest-training:
g000128-s000208439-06773a15394234744fe303b3b513f41fab3d07a9d42e5597ab832339d5e41489
network.brn2 SHA-256: 5bb5b524c40c4a044efa38f40b1b9861e716007ea2899922b284972a8088e614
training.state SHA-256: 4ef5806e139b736c40304d73e8d503662e32297ce61c8e3b3932f686c6a2ce4e
```

G128 promotes over g126: configured held-out loss **.036557857903 versus
.038288336915**, independently recomputed exactly, including both components.
These supplied dashboard numbers are validation losses; g128 final training loss
is **.016180546874**. WDL loss improves **.379187080184 -> .365181529001**, while
teacher loss worsens **.049186564600 -> .051547776312**. Promotion is a blended
objective trade-off, not simultaneous improvement in both components.

All **8,192/8,192** games completed, with zero capped/aborted games: White wins
3,051, draws 854, Black wins 4,287. There are **208,439 training / 53,011 held-out
samples**, 1,044,926 played plies and 208,439 optimizer updates. The color imbalance
describes generated games, not a violation of structural evaluator symmetry.
Recorded g1-start to g128-completion is **06:38:59.665291Z to 07:54:29.564047600Z**,
or **1h15m29.899s**; summed lifecycle time is 4,529.191 seconds. This is close to
the supplied 1:15:32, but that GUI duration's extra approximately two seconds are
not independently accounted for by the generation history.

### Longitudinal learning and the historical comparison

training002 is the closest verified lineage: same architecture/initial weights,
exact teacher, 75/25 target, optimizer, opening/sample limits, six threads and 64
games, but **NNUE generation at depth 2**. All 131 plans and associated decisions
pass the same integrity checks. Compare its **g1..128 prefix**, whose accepted Best
is **g125**, with training004 g1..128. Its eventual g130 Best/g131 endpoint must
not be substituted silently. Prefix promotions/retentions are **55/73**; lifetime
counts are 56/75. Its historical g3/g4 gap changes neither objective nor seed
derivation. Missing legacy seed metadata is not repaired: all settings independently
match master seed 1, giving the same effective streams as training004's 1/1.

The following are **computed arithmetic means of directly recorded losses** on
each generation's changing batch. Train is final training-partition loss; the
other losses are Candidate held-out values. Configured loss is half-squared error
against `.25 * WDL + .75 * teacher`, not the weighted mean of component losses.

| Source / generations | Train | Held-out blended | Held-out teacher | Held-out WDL | Promotions |
|---|---:|---:|---:|---:|---:|
| Handcrafted 1..32 | .020472 | .063481 | .079882 | .395400 | 18 |
| Handcrafted 33..64 | .018564 | .052199 | .066194 | .369072 | 16 |
| Handcrafted 65..96 | .018278 | .045115 | .057523 | .378524 | 15 |
| Handcrafted 97..128 | .017459 | .041683 | .054490 | .355257 | 16 |
| NNUE 1..32 | .013270 | .034550 | .035426 | .202988 | 15 |
| NNUE 33..64 | .010600 | .023950 | .025455 | .195670 | 16 |
| NNUE 65..96 | .010578 | .021878 | .025015 | .185670 | 15 |
| NNUE 97..128 | .010267 | .021882 | .022356 | .183862 | 9 |

Handcrafted first-to-last block decreases are **14.7% training, 34.3% blended
held-out, 31.8% teacher and 10.2% WDL loss**. WDL improvement is non-monotonic.
G97..112 versus g113..128 blended means decline **.043924 -> .039442**, suggesting
ongoing objective fitting with diminishing gains, not a proven fixed-validation
plateau. Historical blended held-out means flatten near .02188. Different data
distributions prevent treating the raw loss difference as an evaluator ranking.
Training component losses were not recorded separately.

Handcrafted Best at boundaries 32/64/96/128 is g32/g61/g96/g128. Median spacing
between promotions is two generations (mean 1.984, maximum seven); longest rejection
run is six. Historical prefix spacing is median two, mean 2.296, maximum eight,
with seven consecutive rejections. Both continue training from the latest Candidate
after rejection. More internal promotions do not demonstrate greater strength.

The historical prefix has 208,104 training / 52,974 held-out samples and 8,183
completed games, nine capped; its generated draws are 2,723 versus 854. Held-out
W/D/L sample counts are **25,767/5,295/21,949** for Handcrafted and
**18,408/17,820/16,746** for NNUE (side-to-move labels). No corresponding generation
has identical complete training-and-held-out sample hashes. This does not establish
absence of individual position overlap or independent random-seed replication.

**New common-population evaluation**, using the existing native-value pooled-loss
method on both saved g1..128 held-out populations, gives:

| Population | Model | Blended loss | Teacher loss | WDL loss |
|---|---|---:|---:|---:|
| Handcrafted, 53,011 samples | Handcrafted g128 | .041425 | .054989 | .366383 |
| Handcrafted | NNUE-generated Best g125 | .083556 | .112223 | .363209 |
| Handcrafted | NNUE teacher g74 | .030471 | 0 | .487537 |
| NNUE, 52,974 samples | Handcrafted g128 | .048484 | .049636 | .216036 |
| NNUE | NNUE-generated Best g125 | .021032 | .023224 | .185464 |
| NNUE | NNUE teacher g74 | .014251 | 0 | .228011 |

Each BRN model has lower blended loss on its own population. Handcrafted training
approximately halves the historical model's blended loss on Handcrafted data, but
does not beat its WDL loss there. Teacher/terminal-label disagreement is much higher
on Handcrafted data (.487537 versus .228011). This motivates isolating supervision;
it does not establish that either teacher predictions or shallow-game WDL are true
chess values. These are reused, adaptively selected campaign holdouts, not a new
independent generalization test. Samples are position-weighted and game-correlated.

### Bounded evaluator and search measurements

New diagnostics reuse `Brn2Diagnostics` and its established **15 roots, 233 legal
children, 248 outputs, 210 quiet and 23 tactical edges**, corpus SHA-256
`0503b850d5ed49686e72e601b1216270cfd40fc3bac18fac4dae697b323f3bd8`.
All sampled structural color-symmetry residuals are exactly zero. Values below are
normalized native outputs; quiet/tactical ratios are median parent-perspective
absolute edge change divided by the pooled output IQR, not accuracy or centipawns.
These milestone rows are **Candidates at the named generations**, including
rejected Candidates; the historical accepted endpoint is shown separately.

| Model | Output min..max | Teacher Pearson | Teacher MSE | Quiet ratio | Tactical ratio |
|---|---|---:|---:|---:|---:|
| Shared g0 | -.0615..+.4219 | -.3638 | .300528 | .3914 | .1539 |
| Handcrafted g10 | -.5501..+.8891 | .7578 | .099101 | .6224 | .5882 |
| Handcrafted g32 | -.6645..+.8173 | .8386 | .080375 | .3268 | .2207 |
| Handcrafted g64 | -.7120..+.8421 | .8231 | .084980 | .2671 | .3635 |
| Handcrafted g96 | -.7049..+.9533 | .8656 | .067749 | .3170 | .8326 |
| Handcrafted g128 / Best | -.5517..+.8657 | .8143 | .082475 | .5232 | .8201 |
| NNUE-generated g10 | -.4973..+.7077 | .7772 | .103712 | .5535 | .5478 |
| NNUE-generated g32 | -.5842..+.6346 | .7828 | .096417 | .2454 | .2024 |
| NNUE-generated g64 | -.6752..+.7525 | .7847 | .092969 | .5016 | .4189 |
| NNUE-generated g96 | -.6475..+.6140 | .8250 | .086157 | .6418 | .3142 |
| NNUE-generated g128 | -.5505..+.6811 | .8569 | .075027 | .4568 | .5254 |
| NNUE-generated Best g125 | -.6178..+.7281 | .8175 | .087862 | .2924 | .3671 |

Final Handcrafted search scores span **-17,936..28,146**; these are full-range
search units. None of its 248 final outputs has absolute normalized value >=.95;
g96 has one such output. No broad saturation is observed. Final teacher Spearman /
sign agreement are **.7432 / 79.84%**, versus historical Best **.7794 / 80.65%**.
Thus final teacher agreement is substantial but not uniformly improving: g96 has
better fixed-corpus teacher error/correlation than g128, while late tactical
roughness remains elevated.

For a small direct temporal check, the same 248 positions were evaluated at every
Candidate g121..128. Mean absolute normalized change over the seven adjacent
generation pairs is **.08410 Handcrafted versus .07493 historical**. Individual
pair means range .05670.. .13813 versus .05259.. .11521; the largest individual
position changes are .48744 versus .28492. This is a descriptive eight-checkpoint
window, not a statistically established instability threshold or a complete
128-generation volatility trace. Full per-generation static/search metrics were
not persisted. Spatial edge roughness and temporal checkpoint change are distinct.

Normal search uses existing depth-4, one-thread, cold 262,144-entry TT, full-window,
mate-distance-only settings, with **1,000,000 nodes / 10 seconds per search** and a
40-second process watchdog, one warmup plus two measured repetitions. Final g128
and historical Best g125 both complete all **13 nonterminal roots at depth 4** and
correctly adjudicate two terminal roots, without timeout/watchdog/error. Repeated
node/depth/status/score/move/PV/evaluation-call fields agree exactly. Per-corpus
totals are **730,235 nodes / 690,294 qnodes (94.53%)** versus **196,528 / 158,077
(80.43%)**. This is operational completion with markedly different search cost.

The established Kiwipete sentinel reveals the longitudinal qualification:

| Candidate checkpoint | Handcrafted nodes / qnodes | Status/depth | NNUE-generated nodes / qnodes | Status/depth |
|---|---:|---|---:|---|
| g10 | 370,197 / 347,215 | completed / 4 | 184,086 / 168,617 | completed / 4 |
| g32 | 510,715 / 495,345 | completed / 4 | 208,302 / 199,104 | completed / 4 |
| g64 | 721,099 / 712,073 | completed / 4 | 65,656 / 54,153 | completed / 4 |
| g96 | 1,000,000 / 996,602 | NODE_LIMIT / 3 | 49,695 / 40,089 | completed / 4 |
| Final accepted Best (g128 / g125) | 681,257 / 671,068 | completed / 4 | 91,905 / 82,243 | completed / 4 |

G96 was also accepted Best, not merely a rejected Candidate. Its warmup and both
measured searches reach the node limit; this is a search-bound event, not an engine
crash. Final g128's worst measured search takes 5.256 seconds versus .796 seconds
for historical Best on this host. Timing is descriptive; node counts provide the
deterministic comparison. Historical WDL-only Best also hit the same Kiwipete node
limit at depth 3. Therefore claiming this entire campaign escaped that class of
qsearch pathology would be false, despite the operational final endpoint.

Final Kiwipete qshadow instrumentation uses the established separate 30-second /
one-million-node bounds; accounting is complete and normal-search node counts
match. On the Handcrafted-driven tree, median adjacent static change is **8,983
BRN versus 4,794 NNUE-shadow units** over 434,004 edges. Historical Best's own tree
has medians 6,879 versus 4,778 over 56,465 edges. Different trees prevent treating
those cross-tree medians as measurements on identical positions. The same-edge
shadow comparison nevertheless confirms residual local roughness. No strength
games, broad runtime suite or browser verification were required or run.

### Scientific decision and limitations

- **A — Operational/pathology:** final g128 passes this bounded corpus, and all
  generation games completed. The trained evaluator was not used to generate those
  games. Accepted g96 exhibits a qsearch node-limit pathology; freedom from severe
  pathology throughout the campaign is disproved by the sentinel, not established
  by the successful training run. Deeper/broader search remains unmeasured.
- **B — Learning:** substantial early and continuing late objective improvement is
  established, with some WDL improvement. Uniformly healthy evaluator convergence
  is not: fixed-corpus agreement regresses after g96, checkpoint outputs fluctuate,
  and search stability is non-monotonic. Lower validation loss does not ensure
  smoother or stronger search.
- **C/D — Generation-source comparison:** observed data composition, cross-population
  fit and search behavior differ materially. The combined change to Handcrafted
  **and depth 4** can be associated with those differences; source alone cannot be
  isolated from depth and resulting trajectories/labels. Effective seeds are
  matched, not independently replicated. Sample volume differs only slightly;
  draw rate, game length and teacher/WDL disagreement differ substantially. One
  lineage per condition, reused holdouts, small diagnostic corpus and unrecorded
  executable identity limit causal/generalization claims. Neither promotion counts
  nor raw own-population loss establish strength superiority.
- **E — Architecture/supervision:** exact symmetry and functioning inference remain
  intact. The findings justify measuring stability, not changing BRN-2 architecture
  or declaring a new preferred teacher weight. The provisional 75/25 selection is
  not overturned by this confounded source/depth comparison.
- **F — Next experiment:** proceed scientifically to **Handcrafted + WDL-only**, in
  a separate authorized work unit. Hold depth 4, 64 games, six threads, optimizer,
  initialization and the actual effective 1/1 seed streams fixed, and verify saved
  data identity, so supervision is the variable. Reuse frozen Handcrafted samples
  where supported, or verify regenerated samples before claiming a paired comparison.
  Include the same fixed-corpus and bounded qsearch checks at milestones, including
  g96 and the final accepted Best; do not infer stability from promotions. This
  directly tests whether teacher removal changes the measured label disagreement,
  WDL fit and roughness. No further pre-experiment measurement is necessary to pose
  that controlled question. A matched-depth source ablation or independent-seed
  replication is needed later for a source-only or generalization claim, not as a
  prerequisite to this supervision experiment.

### Validation, evidence and mutation boundary

Analysis ran against repository `b0b3026`, preserving its later GUI work. `:app:classes`
passed (up to date). Existing diagnostic CLI plus disposable read-only adapters and
analysis scripts are in ignored `app/build/brn2-handcrafted-postcampaign/`; no
production behavior or checked-in helper changed. The historical cross-campaign
CLI assumes v2 shared generator/teacher pins and four-part settings; its read-only
inspection/loss primitives were adapted locally for v3's separate teacher/seed
fields instead of changing production code or misidentifying the teacher.
Checks include all 259 compared generation records, independent binary/checksum
inspection, production lineage/acceptance readers, exact endpoint loss recomputation,
payload hashes/headers, 26 static invocations, 38 bounded normal-search invocations
and two qshadow invocations. All 66 diagnostic processes exit zero; g96's documented
engine node-limit result is retained, not counted as successful depth-4 completion.
An initial local summary read hit PowerShell's UTF-16 redirection; the decoder was
corrected and the analysis completed without rerunning training or changing evidence.

Before/after complete membership, size, modification-time and SHA-256 inventories
match for **3,282 files**: training001 948, training002 959, training003 23,
training004 951 and NNUE training 401. No protected store writer was opened; no
store, NNUE Best, architecture, supervision implementation or training state was
modified. No training was started/resumed, no strength campaign ran, and no push
occurred. Only this appended report section is committed; inherited untracked
`app/bin/` is excluded. `git diff --check` passes. Exact root `CODEXLOG_CURRENT.md`
and `VERSION_STATE.txt` remain absent and were not created. This is completed
analysis, not user acceptance or authorization to start the next experiment.

Human actions required after this prompt: **None**. Authorizing the recommended
experiment is a separate, non-blocking future decision; its execution has not begun.
