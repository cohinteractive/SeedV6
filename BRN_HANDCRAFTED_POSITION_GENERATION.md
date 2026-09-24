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
NNUE generation. Fresh BRN-2 GUI choices include Handcrafted (the unchanged default),
NNUE and the existing `SELF_PLAY` mode ("Self-play with BRN"). Self-play uses
Candidate-vs-Best game validation; external generation retains held-out validation.
The selection persists through the existing settings machinery; established BRN-2
lineages retain their source lock. Fresh self-play stores use the normal initial
Best/bootstrap checkpoint lifecycle. `TrainerConfig.teacherStore` and the
corresponding GUI settings field are distinct from `TrainingSource.generatorStore`.

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

## Controlled Handcrafted + WDL preparation: BLOCKED (2026-09-23)

**BLOCKED_PAIRED_GENERATION; not READY_FOR_LONG_HANDCRAFTED_WDL_ONLY_BASELINE.**
The authorized next scientific comparison is Handcrafted + terminal-WDL-only,
using **training004 as the 75% NNUE / 25% WDL control**. Its configuration and
initial parameters can be reproduced, but its six-thread generated data cannot
currently be claimed as a paired stream: an exact-configuration replay changed
boards and labels, and repeated generation of the same indexed game changed the
terminal winner. Preparation stopped before the first experimental training update.
No final lineage or real 128-generation campaign was created or started.

This finding supersedes any assumption above that matched effective seeds alone
establish deterministic six-thread Handcrafted trajectories. Earlier one-thread
fixture tests remain valid within their checked scope. This is a completed
preparation/investigation turn, not a completed or accepted readiness work unit.

### Actual control and proposed fixed configuration

Work started at repository `8a7c36b`, preserving the committed GUI work and inherited
untracked `app/bin/`. No applicable on-disk AGENTS.md was found in the repository or
its inspected ancestor chain. Exact root `CODEXLOG_CURRENT.md` and `VERSION_STATE.txt`
are absent; neither capability was activated or file created.

Read-only production readers checked all **128 checksummed v3 plans and data pairs**,
128 history rows and 129 checkpoint manifests. All 128 generation settings exactly
equal current `TrainerConfig` derivation for the following configuration. All 103
retained optimizer payload headers have the same hyperparameters; 26 historical
payload pairs have already been pruned. Actual g0 payloads were separately loaded
and compared byte for byte with newly constructed model/optimizer payloads.

| Class | Variable | Control / intended WDL experiment |
|---|---|---|
| A: identical | Architecture / schema / width | `seedv6.brn.2` / 2 / 32; canonical us/them symmetry and dual-perspective incremental inference unchanged |
| A | Position source | `HANDCRAFTED`; generator store, checkpoint ID and hash empty |
| A | Search | Depth 4; six root workers; games run sequentially, not six concurrent games |
| A | Search implementation | Existing `SelfPlayRunner` / iterative `RootParallelSearch`; fresh 1,048,576-entry TT per game, shared among its workers and reused within the game; Handcrafted production aspiration, mate-distance, razoring and futility settings unchanged |
| A | Game bounds | 64 games/generation; normal starting FEN; random opening 0..8 plies inclusive; maximum 1,024 plies; no per-move node/time cap (`-1 / -1`) |
| A | Sampling | Maximum 32 positions/completed game; result-independent evenly spaced trajectory indexes including both ends; incomplete/capped games excluded; exact terminal side-to-move -1/0/+1 labels |
| A | Run/data seeds | Persisted master 1 / data 1, independently derived generation/domain streams below |
| A | Initialization | Fixed architecture seed `0x533642524e320001` = `5996052875157045249`; `java.util.Random`, unchanged model constructor; not the GUI run seed |
| A | Optimizer | Online sparse/dense Adam; learning rate .001, beta1 .9, beta2 .999, epsilon 1e-8; zero initial moments/step; no schedule, decay, clipping or reset between generations |
| A | Updates / ordering | One epoch, minibatch 1, shuffle true; one update per retained training sample; descending Fisher-Yates with the generation SHUFFLE stream; no replay buffer |
| A | Holdout | Shuffle eligible whole-game indexes with HOLDOUT; reserve `max(2, (eligible+4)/5)` games; restore ascending game/sample order in each partition; 64 completed games give 51 training / 13 held out |
| A | Continuation / decision rule | Continue latest Candidate and optimizer even after rejection; compare Candidate versus accepted Best on the same generation holdout; strictly lower mean half-squared configured loss promotes, ties retain |
| A | Planned duration | 128 total generations from fresh g0; no elapsed-time limit; unchanged retention rules |
| A | Persisted score mapping | `NnueScoreMapping.V1`, scale 32511.0; this field does not turn Handcrafted search scores into targets |
| B: necessary change | Objective | `NNUE_BLENDED(.75)` becomes canonical `BrnSupervision.WDL`, weight 0, terminal-WDL weight 1 |
| B | Teacher dependency | Control has the exact external g74 teacher documented above; WDL has no teacher store/ID/hash and no `brn-teacher.bin`; never blend with that teacher at zero weight |
| B | Training / validation target | `.25*WDL + .75*teacher` becomes `Sample::target`; promotion's same strict-loss rule now measures WDL loss; no teacher component diagnostics are required |
| B | Expected consequences | Learned parameters, optimizer moments after g0, losses, promotions and accepted Best can differ; new lineage/plan/data-envelope identities and elapsed times can differ |
| C: inactive or descriptive | NNUE-specific GUI minibatch/epochs, match validation | Current GUI minibatch is 32, but BRN forces effective minibatch 1 / one epoch; validation-pairs setting 64, match seed, alpha and margin are inactive for this generated-heldout workflow |
| C | Unused NNUE selection / telemetry | Hidden stale generator/teacher drafts do not authorize a dependency; wall-clock telemetry and presentation are not learning inputs. Scheduling independence, however, is **not** established by that fact |

Normal starting FEN is
`rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1`.
All effective game/training settings above are evidenced by plans, rather than
inferred from GUI defaults. Read-only current Java preferences additionally show
128 generations and zero maximum run minutes. Run duration is not part of the
immutable generation-plan identity, so those current preferences alone do not
prove the historical launch limit; the store proves 128 settled generations.
`maximumGenerations` bounds newly completed generations in each service invocation,
not an immutable lineage ceiling. Any future stopped/milestone workflow must set
the remaining count and verify the displayed endpoint is g128 on Resume.
Current folder preference still names training003, while source/objective/seed
preferences are bound to training004: a future user must explicitly choose a fresh
folder and review controls. No GUI preference was changed in this work.

The fixed model constructor draws node embeddings within +/- .01, other embedding
rows within +/- .005 and output weights within +/- sqrt(6/33), leaving biases zero.
Standard fresh GUI construction calls the same `new Brn2Trainer(.001)` used here.
It reproduces **every initial model and optimizer byte**, without copying a trained
checkpoint or opening a protected writer:

```text
g0 model SHA-256: 195d4300ce1b90a30cb888d6990a872f33165bfadd66cf1f0212ba3f0c2f653f
g0 training.state SHA-256: dec6eccdf4d9e59d4ea9f9843831548a045e07d050032f3c4ab62767d8c595bc
g0 checkpoint: g000000-s000000000-322d79c85ae7bfe7ae8cbac93914ded720f72b7fc6bf492cd95bf84c713765af
```

There is **no initialization confounder**. Proposed inputs remain master/data **1/1**,
not the superseded randomly selected campaign inputs in the earlier preflight.
`TrainerConfig.seed(g,d)` is the first `SplittableRandom` long from
`streamSeed XOR domainSalt XOR (g * 0x9E3779B97F4A7C15)`, with Java long overflow.
SELF_PLAY uses data; SHUFFLE/HOLDOUT use master. Salts are respectively
`0x6A09E667F3BCC909`, `0xBB67AE8584CAA73B`, `0xA54FF53A5F1D36F1`.
Indexed game seed is the first `SplittableRandom` long from
`selfPlaySeed + 0x9E3779B97F4A7C15 * zeroBasedGameIndex`.

| Generation | SELF_PLAY | SHUFFLE | HOLDOUT |
|---:|---:|---:|---:|
| 1 | 7921502845091313457 | -6540313355536843707 | 8110949293515089404 |
| 32 | 3525771389338768354 | 7091595909261158330 | 7382676087462722757 |
| 64 | -1883070537092390124 | -2341096023225869560 | 4560487814634553252 |
| 96 | -5498893638532692338 | -7388802151788537711 | -2449516721883955724 |
| 128 | -2759579733069302082 | -7661710017182582974 | 97128448762020054 |

All intervening generations were also checked, not only these displayed milestones.

### Paired-generation failure and preflight boundary

The disposable service used **exactly the control g1 game/training configuration**,
64 games, depth 4, six workers, seeds 1/1 and canonical WDL. Only its run limit was
one disposable generation. The sole student writer was
`app/build/brn2-handcrafted-wdl-preflight/disposable-student`; it is failed preflight
evidence and must not become the final lineage. Neural-generator overloads were
guarded to fail if called. The probe deliberately checked paired generation before
allowing training and stopped at the first failed assertion:

| Measurement | Saved training004 g1 | First disposable generation | Separate generation-only replay |
|---|---:|---:|---:|
| Completed games | 64 | 64 | 64 |
| Played plies | 8,833 | 8,883 | 8,640 |
| Samples | 2,042 | 2,042 | 2,042 |
| Training / held-out samples | 1,626 / 416 | Not compared after failure | 1,626 / 416 |
| Ordered board differences from control | Reference | Not measured after first failure | 364 / 2,042 |
| Ordered WDL-label differences from control | Reference | Not measured after first failure | 172 / 2,042 |

The first run's aggregate W/D/L was even identical (26/8/30); aggregate equality
therefore was insufficient. The second replay differed in 15 indexed game segments;
game 0 already differed. Its train/holdout lists used the control's saved whole-game
membership. Comparison examined six-long boards and WDL labels directly in order,
not serialized envelopes, embedded teacher targets, plan hashes or timing fields.
Samples actually persist terminal WDL even in the blended control; teacher targets
are constructed later. Ordered-label differences include effects of changed sampled
side-to-move positions and must not be read as 172 independent changed game outcomes.

Position-only SHA-256 (int count followed by six big-endian longs per sample):

```text
Control training: b51749cfbee5a3d77fbcd437a8a18315fb7e73c41dfc03164fc144158bb08960
Replay training:  3dbbf32638698707a82ca045500e6e5ae262e684659240b3ec39a7ccce50547a
Control holdout:  ab249e403d289af16a45188500be2aed2f053ee3deb56367302f95867e055c52
Replay holdout:   3891f35ca753adc8af4019d788dbc68769690eb6d809bf64db24ad81a097c9a1
```

Four further generation-only repetitions of **game index 0**, all with effective
game seed **2496977766053201155**, yielded:

| Repetition | Plies | Terminal result | Sampled-position SHA-256 |
|---:|---:|---|---|
| 0 | 143 | White win | b45bf8d845b525a0e87a81d377c1b4256853d8665650412e0e5a1b6ca68ea6f2 |
| 1, 2, 3 | 110 | Black win | 77b6e0d645cae48ab66bc7aae54920653d138d48ef94e3c818d186621c08aca1 |

Full trajectory board/move comparison first diverged at zero-based position/move
index 95. These repetitions used no training service, student or teacher model,
nor any supervision selection. Thus supervision leakage is **not** established;
generation reproducibility itself fails. Static inspection identifies dynamically
claimed root work, per-worker ordering state and a shared TT as plausible scheduling
mechanisms. Stable root-index reduction does not prove complete trajectory identity.
The precise causal interaction was not isolated and no search fix is claimed.
Core/search sources have no changes between `f109b71` and the inspected HEAD;
historical executable identity was not recorded, but current identical-configuration
repetitions independently disprove the required replay guarantee.

Post-failure read-only checks prove the disposable store retains **g0 / step 0**,
no saved bootstrap `.data`, no candidate and no settled history. Its v3 plan hash is
`b9c7d3551cd319da065b970d60140e376d8147bd7a7f60db46c6fda904b78f05`.
Handcrafted/WDL/1:1 metadata and the full 64-game settings persist; teacher fields
are empty and `brn-teacher.bin` absent. `loadTeacher` explicitly rejects WDL.
No `NnueEvaluator` class was loaded in the logged service JVM. Code inspection of
both training and validation branches confirms teacher loading is conditional on
`blended()`, and canonical WDL delegates to terminal-label overloads. Handcrafted
evaluation scores never enter `GameTrajectory.Position` or `TrajectorySampler.Sample`.
Focused tests additionally verify terminal labels feeding actual online Adam and
codec continuation with exact optimizer bytes.

The planned one-update Stop, separate-JVM Resume, held-out settlement, uninterrupted
optimizer comparison and minimally trained candidate normal-search checks were
**not reached or executed**. Persisted initial configuration and lower-level tests
are not substitutes for that unfinished lifecycle preflight. This report makes no
strength, convergence or search-stability inference from the attempted generation.

### Fresh lineage and decision required

Store membership was inspected: `training`, `training001` through `training004`,
and the two existing `experimental-supervision-ablation-*` directories exist.
**`E:\SeedV6-Networks\BRN\BRN-2\training005` is currently the next unused numbered
training folder**, following the observed convention. The application accepts an
explicit empty/new folder; it does not allocate the numeric suffix. No training005
folder, initial checkpoint or GUI draft was created. Recheck availability before a
future authorized start; never select the disposable student or resume training004.

**Do not start the proposed campaign yet.** A smaller depth, one worker, altered
search, changed openings or acceptance of unequal data would change the controlled
experiment, not resolve it within this unit. Existing saved-sample offline ablation
facilities demonstrate a possible direction, but their current guards require the
older NNUE-generated WDL control and teacher identity; they are not a supported
training004 replay switch. Normal GUI `TrainerConfig` exposes no historical-data
source. Protected plan/data files cannot simply be transplanted into a new lineage:
their identities bind source parent, incumbent, settings and objective.

Required decision: authorize a separate, carefully scoped **frozen-training004-data
WDL replay workflow** (preserving boards, terminal labels, ordering and partitions),
or explicitly revise the scientific design to accept independently regenerated,
non-identical data. Investigating deterministic search would also require separate
authorization under the prohibition on changing search semantics. None of these
decisions was inferred or implemented. Readiness and final-lineage start remain
blocked until an authorized route is verified, including the outstanding lifecycle
and candidate-loading checks.

### Existing milestone diagnostics retained

Conditional on resolving the experiment design, reuse `Brn2Diagnostics` at
**g32/g64/g96/g128**, recording the Candidate and contemporaneous accepted Best
separately. `GenerationRetention` keeps these payloads through g128 (each is less
than 100 generations old); later extension would need a new retention check.
Keep training loss and configured held-out WDL loss from history/data, and compare
common saved held-out populations with training004's separately recorded WDL
component. Raw blended and WDL objective losses are not directly interchangeable.

Use the existing fixed 15-root/248-output corpus (SHA-256
`0503b850d5ed49686e72e601b1216270cfd40fc3bac18fac4dae697b323f3bd8`):
`--depth=0 --symmetry=true` records score ranges, local edge changes and color
symmetry. Compare identical-position outputs between checkpoints to measure temporal
volatility. Normal-search diagnostics use `--drivers=brn2 --depth=4 --nodes=1000000
--time-ms=10000 --warmup=1 --repetitions=2 --qshadow=false`, one thread and a cold
262,144-entry TT, with the existing 40-second process watchdog per position. Record
completion/depth, nodes/qnodes and node-limit incidence, retaining bounded failures.
Always include `--positions=middlegame-kiwipete`: training004 **accepted g96** reached
1,000,000 nodes / **996,602 qnodes**, depth 3, in warmup and both measured searches.
Use the whole small corpus when assessing incidence; one sentinel is not a rate.
Existing optional qshadow uses its separate 30-second/one-million-node bound and
the protected g74 only as an offline diagnostic comparator, never a training target.
Prior artifacts are in `app/build/brn2-handcrafted-postcampaign/`. No new diagnostic
framework or future checkpoint results were created.

### Validation, evidence and completion boundary

`:app:classes` passed. **10 focused tests across four suites passed, zero failures,
errors or skips**: BrnSupervisionPersistenceTest (5), Brn2TrainingTargetsTest (3),
BrnRunSeedsTest.defaultSeedsStayExactAndOnlySelfPlayChanges (1), and
BrnHandcraftedGuiTest.freshAxesDefaultsTeacherAvailabilityPreferencesAndLocks (1).
The separate paired-generation assertion **failed and remains an acceptance blocker**;
passing unit tests do not override it. Local probes were compiled with JDK 21 and
run with `java -Xmx1024m`; generation-only investigation comprised one additional
64-game replay and four repetitions of game 0, not a long training run.

Probe sources, raw audit/preflight/replay logs, copied JUnit XML, current read-only
GUI preferences, validation summary and protected inventories are retained locally
under ignored `app/build/brn2-handcrafted-wdl-preflight/`. Two local probe compile
errors used nonexistent accessor names and were corrected before execution; the
summary reader was corrected for PowerShell UTF-16 logs without rerunning generation.
No production-code or checked-in test changes were needed or authorized by the finding.
This commit changes only this established report; inherited `app/bin/` is excluded.
CRLF-aware `git diff --check` passes. No push occurred.

Before/after membership, size, modification time and SHA-256 inventories match for
**all 3,282 protected files** (20,306,873,406 bytes): training001 948, training002 959,
training003 23, training004 951, NNUE training 401. No protected writer was opened;
no historical store, teacher, accepted experimental store, NNUE behavior, architecture,
optimizer configuration, evaluator/search semantics or GUI implementation was changed.
Full/long-running suites, strength matches, further training after the pairing failure,
and the real 128-generation campaign were deliberately skipped. No browser verification
was required for this native Swing/code investigation.

Human actions required after this prompt:

- **Blocking:** choose and authorize the data-reproducibility route above. This blocks
  readiness, remaining preflight, final lineage creation and real campaign start.
- **Non-blocking:** none. This report does not authorize accepting the confounder or
  starting the planned run; no future WDL-only campaign result is claimed.

## Authorized frozen-training004 WDL replay: ready (2026-09-23)

**READY_FOR_FROZEN_TRAINING004_WDL_REPLAY.** The user resolved the preceding blocker
by authorizing **frozen training004 data + canonical WDL-only supervision**, not
regenerated Handcrafted games. This readiness supersedes the blocked next-action
status above; the generation-instability evidence remains valid and unrepaired.
No real 128-generation replay or final experimental lineage was started in this unit.

The scientific comparison is now: given exactly training004's generation-indexed
sampled boards, terminal side-to-move labels, multiplicities, train/holdout membership
and ordering, with identical g0 model/Adam state and applicable training controls,
change the 75% NNUE / 25% WDL target to **100% terminal WDL**. New gradients, models,
held-out WDL losses, decisions and accepted Best may differ. The replay does not
force training004's promotions and does not make new self-play claims.

### Implementation and immutable provenance

The smallest implemented route is an explicit `frozen-wdl` command in the normal
SeedV6 application, using the existing **TrainerService** checkpoint, optimizer,
partial-progress, validation, promotion, history and retention lifecycle. There is
no parallel trainer. The normal GUI's live-training Start/Resume refuses a frozen
store before opening a writer and identifies the command workflow; the source panel
displays the frozen mode accurately. Fresh live-training GUI choices are unchanged.

`TrainingSource.FROZEN_REPLAY` is an explicit source, separate from Handcrafted or
NNUE generation. New checksummed `frozen-replay.bin` records the canonical source
path, g0 model/optimizer hashes, controls and ordered per-generation source parent,
plan/data hashes and normalized content hashes. Its SHA-256 is included as
`|frozen-wdl-v1:<hash>` in every replay generation setting, plan and attempt, so
replacement provenance cannot silently restart or reinterpret existing work.
Plan v3, history schema 4 and existing BRN-2 model/optimizer encodings remain in use.
Old records and normal generation settings retain their old bytes when no replay
identity is present. No historical migration occurs.

```text
Source: E:\SeedV6-Networks\BRN\BRN-2\training004
Frozen corpus: g1..g128, all 128 settled source plans/data pairs
Corpus descriptor SHA-256:
a4cfc022774e41e31bf8f43c81c253900cd39bb805cac047f34faf911696f91b
Initial model SHA-256:
195d4300ce1b90a30cb888d6990a872f33165bfadd66cf1f0212ba3f0c2f653f
Initial complete optimizer SHA-256:
dec6eccdf4d9e59d4ea9f9843831548a045e07d050032f3c4ab62767d8c595bc
```

The source is **referenced read-only**, not resumed or opened by a source-store
writer/lock/recovery operation. Source and destination must resolve to separate,
non-nested real paths, including existing ancestor aliases. Capture verifies source
history/ancestry, generation settings and checksummed data against the explicit
control. Startup verifies all pinned inputs, and consumption verifies the actual
decoded generation again. Missing/changed input fails closed, including when a local
batch already exists. No fallback, regenerated data or changed teacher is substituted.

For each generation the exact source `BootstrapPartition` and historical statistics
are imported into a new destination-owned `BootstrapData` envelope. Boards, WDL
doubles, duplicate samples, sequence and whole-game IDs are unchanged. The envelope
binds the new student's parent/incumbent/objective through its own plan hash, and
generation time is zero: these are imported historical games, not newly played games.
Source envelopes, source plans and their teacher/provenance fields are never rewritten.
The content hash normalizes only plan hash and elapsed generation time; it includes
all ordered boards/labels, membership lists and stored game statistics. Source data
contains terminal WDL, **not static teacher values or Handcrafted search scores**.

The fresh destination receives the exact verified source g0 optimizer bytes, including
its model weights, zero moments and step 0; those bytes also match canonical fresh
construction. Architecture/schema/width remain **seedv6.brn.2 / 2 / 32**. Adam stays
**.001 / .9 / .999 / 1e-8**, minibatch **1**, one shuffled online pass per generation,
with the same master/data **1/1**, per-generation SHUFFLE seeds and original holdouts.
All source generation settings and the seed table above remain exact provenance;
depth 4 / six workers / 64 games describe the frozen source, not work executed by
the replay. No RNG is used to regenerate positions, labels or holdout membership.

Canonical `BrnSupervision.WDL` is mandatory, even rejecting `NNUE_BLENDED(0)`.
Destination teacher fields are empty and `brn-teacher.bin` is absent. Both training
and held-out validation use the existing terminal-label overloads. No g74 model or
NNUE teacher store is resolved, loaded or queried for replay. Historical teacher IDs
are read only as inert source-plan provenance. Strictly lower configured half-squared
loss still promotes; ties retain. Training still continues from the latest Candidate
and its optimizer, including after retention, rather than resetting to Best.

### Bounded production preflight

Only the following disposable student was written:

```text
C:\projects\seed\java\seedv6\app\build\brn2-frozen-wdl-preflight\disposable-student
```

The real application command captured all 128 source identities but was bounded to
**absolute endpoint g1**. Its first JVM used `--stop-after-updates=1`, performed one
real Adam update and safely stopped with g0 still latest-training. A read-only probe
verified the exact persisted cursor (one sample/update, initial step zero) and compared
the complete partial optimizer with an independently calculated first shuffled WDL
update. The first training sample's shuffled index is **1131**. Partial optimizer
SHA-256 is `6360b197cd97f5a958867bf5c5b0d80e4e4ab0164ee8226bf4302da2365876d9`.

All **1,626 training and 416 held-out boards/labels** match training004 g1 directly,
in sequence, with identical **51/13 game membership** and multiplicity. The g1
SELF_PLAY/SHUFFLE/HOLDOUT seeds remain respectively **7921502845091313457 /
-6540313355536843707 / 8110949293515089404**. The source/destination hashes are:

| Checked content | Identical SHA-256 |
|---|---|
| Ordered training boards | `b51749cfbee5a3d77fbcd437a8a18315fb7e73c41dfc03164fc144158bb08960` |
| Ordered held-out boards | `ab249e403d289af16a45188500be2aed2f053ee3deb56367302f95867e055c52` |
| Ordered training boards + WDL | `19a74d9b35802dae08ba9edebeef4ac7d54a4716002a74e9b245f4a29ddd37a5` |
| Ordered held-out boards + WDL | `c025c75e69c23fbd39c4eeb6cbca02d314d355b12380d12d57076e81a6856c02` |
| Training boards + WDL after the persisted shuffle | `3951b76d2aef3e0ff4f7c974084a75d8b32efcda58587a518a1e66d1c53abff5` |

A **separate JVM** resumed from persisted metadata and cursor without a source
argument, completed the remaining 1,625 updates, and settled exactly one generation.
Root replay/source/supervision/seed records and plan/data hashes stayed identical.
The resulting entire model and optimizer payloads equal an uninterrupted canonical
WDL pass over the original source partition, bit for bit:

```text
Candidate: g000001-s000001626-b21c2f5f4b0f8139f834393739bd4189719a05e374c977cfda7a64788caad82e
Model SHA-256: f2402e98ab7f79b1722b86d3665004e51e5dd4563428de69f043de4038c0f036
Optimizer SHA-256: 910204caa3b52e20ba2c019480587b01fea55dfbb50d2c8ef252cfa05d30a9c4
Updates: 1,626 total; generation cursor: 1
Training WDL loss: .4346583780351984 -> .0800294792127642
Held-out WDL loss: Candidate .5092935425296223; Best .5007111245727711
Decision: RETAIN_INCUMBENT (Best remains g0)
```

The retention differs legitimately from the control's g1 promotion. These bounded
losses verify mechanics; they establish no superiority, strength or long-run stability.
Independent held-out WDL recomputation equals the stored decision evidence exactly.
The serialized candidate loaded through `CheckpointStore.readSnapshot`, normal
BRN evaluation and iterative alpha-beta search. Both depth-2 checks completed within
100,000 nodes / five seconds: standard start **227 nodes / 1 qnode** and the established
queen endgame **97 nodes / 8 qnodes**. No engine/search implementation changed.

Generator entry points are guarded to throw in the CLI/preflight. All finished replay
runs therefore made **zero generator calls**. JVM class-load evidence additionally
shows no `SelfPlayRunner`, `RootParallelSearch` or `NnueEvaluator` loaded in either
training process. Search happened only in the separate post-training load check.
Tests observe the actual canonical training/held-out callbacks, verify exact WDL
targets, and succeed with a deliberately nonexistent source teacher directory.
No game or terminal outcome was regenerated anywhere in the frozen preflight.

The final installed Windows launcher was also exercised at the already settled
`--until=1` endpoint: it completed **zero additional generations/updates**, confirming
the absolute bound. The five replay tests cover a two-generation synthetic corpus,
exact partial Resume, uninterrupted optimizer equality, membership/order/duplicates,
missing/corrupt source and local data, changed controls/initialization, loss of replay
metadata, no live fallback/archive, canonical WDL dispatch, and existing reference
recovery after loss of `latest-training`. Source/store corruption is never repaired by
inventing data. Ordinary safe-stop and checkpoint recovery semantics are retained.

### Later full replay and diagnostics

Membership was rechecked: **training005 is still the next unused numbered store**.
No final folder, g0, generation 1 or GUI preference was created for it. The disposable
student is preflight evidence, not the final experiment. From the repository root,
the following is the later, separately initiated full replay command; it was **not run**:

```powershell
.\app\build\install\seedv6\bin\seedv6.bat frozen-wdl --source=E:\SeedV6-Networks\BRN\BRN-2\training004 --store=E:\SeedV6-Networks\BRN\BRN-2\training005 --until=128
```

Type `stop` then Enter for a durable Stop and wait for `STOPPED`; Ctrl+C also requests
the normal safe-stop hook. Do not force-kill the process as a substitute for safe Stop.
Resume the same lineage with:

```powershell
.\app\build\install\seedv6\bin\seedv6.bat frozen-wdl --store=E:\SeedV6-Networks\BRN\BRN-2\training005 --until=128
```

`--until` is an **absolute generation endpoint**, unlike ordinary live-training
`maximumGenerations`. The same command after g128 does not start g129. For milestone
work, use endpoints 32, 64, 96 and 128 successively. Recheck folder availability
before the fresh start; existing non-replay stores are rejected. Use the current
build for the new frozen-source records. The complete protected source corpus and
g0 payloads must stay available and unchanged throughout replay; there is no hidden
fallback if the read-only reference becomes unavailable.

The existing milestone diagnostic plan immediately above remains applicable to
these ordinary BRN-2 candidate/model files, including both Candidate and accepted
Best identities, WDL losses, fixed-corpus magnitude/volatility/symmetry and bounded
normal-search/qsearch checks. Compare new configured WDL loss with training004's
WDL component on the same frozen holdouts, not its blended loss. In particular retain
the accepted-control-g96 Kiwipete comparison (**996,602 qnodes**, one-million-node
limit at depth 3). No milestone diagnostics or future replay results were produced.

No unavoidable data or initialization confounder remains for the frozen supervision
comparison. Limitations remain: one corpus/seed realization, reused adaptive holdouts,
shallow-generated terminal outcomes rather than established chess values, and only
a bounded g1 lifecycle/search preflight. The original generation nondeterminism is
unchanged and is a separate future investigation; frozen replay makes no claim that
six-worker Handcrafted generation has become reproducible.

### Validation and change boundary

**71 distinct focused tests across 11 suites passed, zero failures/errors/skips**:
FrozenReplayTest 5; BrnHandcraftedGuiTest 2 selected methods; TrainingSettingsValidationTest
6; BrnSupervisionPersistenceTest 5; PartialGenerationStoreTest 1; HistoryRepositoryTest
9; Brn2TrainingTargetsTest 3; Brn2BlendedBootstrapTest 7; BrnHandcraftedGenerationTest
1 selected exact-optimizer Resume method; BrnRunSeedsTest 5; StoppedReconfigurationTest
27. This is the union of latest passing selections, counting separate parameter cases
even where JUnit repeats display names, not the sum of reruns. Compilation, test-dependent
`:app:installDist`, installed-launcher endpoint verification and `git diff --check` pass.

Production changes are confined to the new `FrozenReplay` provenance reader and
`FrozenWdlReplay` command, its `Main` dispatch, the explicit source/configuration and
TrainerService import branch, small checkpoint metadata handling and the GUI guard/display.
New replay tests and one GUI guard test cover the additions. Core BRN/NNUE model,
optimizer, feature, evaluator, search, TT and worker-scheduling implementations are
unchanged; normal live-generation behavior is unchanged. Existing GUI work and
untracked `app/bin/` are preserved and excluded from the commit. No push occurred.

Sources/logs, before/after inventories, metadata/sample/optimizer probes, JUnit XML,
class-load evidence and `validation-summary.json` are retained under ignored
`app/build/brn2-frozen-wdl-preflight/`. Before/after membership, byte sizes, modification
times and SHA-256 match for **all 3,282 protected files / 20,306,873,406 bytes**:
training001 948, training002 959, training003 23, training004 951, NNUE training 401.
No protected store writer was opened and no protected store was changed, resumed,
repurposed or migrated. Full/long suites, strength matches, new experimental position generation,
nondeterminism repair and the full 128-generation replay were deliberately not run.
Live-training regression tests retained their usual isolated temporary fixtures.
No browser verification was needed. Root journal/version files remain absent.

Human actions required after this prompt:

- **Blocking:** none for this completed implementation and bounded readiness verification.
- **Non-blocking:** start the fresh full replay separately when ready, using the command
  above (or successive milestone endpoints). Future campaign analysis depends on
  actual replay completion and diagnostic evidence, neither of which is claimed here.


## Completed frozen-training004 WDL replay: paired analysis (2026-09-23)

**The frozen comparison passes integrity checks. WDL-only improves terminal-label
fit, but yields substantially more volatile and more extreme evaluator outputs.
Teacher supervision stabilizes those numerical properties; it does not establish
uniformly better qsearch behavior or playing strength.** Both lineages have
checkpoint-dependent Kiwipete pathologies, and both final Bests complete the bounded
corpus. The WDL-only final Best is cheaper to search in these diagnostics.

This section supersedes the previous readiness-only status. The user completed the
full replay externally; this work only inspected and evaluated saved checkpoints.
The established tracked report is `BRN_HANDCRAFTED_POSITION_GENERATION.md`; the
prompt's nested `BRN\_HANDCRAFTED\_POSITION\_GENERATION.md` path does not exist.
No alternative report hierarchy was created.

### Exact lineage and controlled-experiment integrity

The complete replay is **`E:\SeedV6-Networks\BRN\BRN-2\training005`**. Identification
was by the sole `frozen-replay.bin` discovered recursively under the canonical BRN-2
root, its decoded source identity and its complete persisted ancestry, not its
numeric folder name. Production `FrozenReplay.read/capture/verify`,
`CheckpointInspection`, `HistoryRepository`, the existing model/optimizer codecs
and held-out readers were used without opening a store writer or recovery service.

```text
Source: E:\SeedV6-Networks\BRN\BRN-2\training004
Replay: E:\SeedV6-Networks\BRN\BRN-2\training005
Frozen descriptor SHA-256:
a4cfc022774e41e31bf8f43c81c253900cd39bb805cac047f34faf911696f91b
Initial model SHA-256:
195d4300ce1b90a30cb888d6990a872f33165bfadd66cf1f0212ba3f0c2f653f
Initial complete optimizer SHA-256:
dec6eccdf4d9e59d4ea9f9843831548a045e07d050032f3c4ab62767d8c595bc
Replay final Candidate = accepted Best = latest-training:
g000128-s000208439-e6df08fc5c287993a0f48ccf140e563f4d4f81ed88cd0e91c0b091ed4bd47ec5
Control final Candidate = accepted Best = latest-training:
g000128-s000208439-06773a15394234744fe303b3b513f41fab3d07a9d42e5597ab832339d5e41489
```

Both lineages have contiguous g0..g128 manifests, **128 settled history/validation
records, 128 plan/data pairs, 208,439 training updates/samples and 53,011 held-out
sample occurrences**. Source statistics describe **8,192 completed historical
games**. The replay imported these games; it played zero new games. Staging is empty;
the remaining generation-attempt record is the settled g128 attempt, not pending
g129. History has no warnings; accepted-promotion chains and final references agree.
Replay promotions/retentions are **58/70**, versus control **65/63**.

| Control checked | Evidence and result |
|---|---|
| Frozen boards, terminal labels, multiplicity and sequence | All 128 source plan/data hashes match the expected frozen descriptor; freshly captured descriptor equals the persisted descriptor. Ordered training and held-out sample hashes match source/replay at every generation. |
| Train/holdout membership and game statistics | All ordered whole-game membership lists and statistics match. Normalized full-content hashes match for all 128 pairs; normalization removes only the new plan identity and measured generation duration. |
| Initialization | Source/replay g0 network and complete optimizer payloads are byte-identical and match the expected hashes and canonical fresh BRN-2 initialization. |
| Architecture / optimizer | `seedv6.brn.2`, schema 2, width 32; Adam .001/.9/.999/1e-8. All 103 retained model/optimizer pairs in each lineage pass payload hash/header checks; decoded optimizer snapshots match manifest model hashes and step counts. Older pruned payloads are not claimed to have been reloaded. |
| Ordering and updates | Identical master/data 1/1, generation-indexed SHUFFLE seeds, ordered inputs and unchanged production shuffle path; one online Adam pass, minibatch 1. Each checkpoint step equals cumulative training samples. No optimization updates were executed during analysis. |
| Source settings | Depth 4, six workers, 64 games, openings 0..8, up to 32 samples/game and 1,024 plies are preserved source provenance. They are not replay search work. |
| Promotion rule | Strictly lower configured half-squared held-out loss promotes; ties retain. All decisions, incumbents and resulting Best references agree with persisted evidence. |
| Changed supervision | Control is .75 exact pinned-g74 teacher + .25 terminal WDL. Replay is canonical WDL, with absent teacher-store metadata, empty teacher fields in every plan/history record and no teacher component in held-out evidence. |

The inspected implementation at `32443393b0fdad9ce4eaf63fda1aff1108f51da2` imports
the frozen partition rather than calling generation/sampling. The CLI guards every
generator entry point. All replay data/history generation durations are zero.
`TrainerService` loads a teacher only for blended supervision; WDL training and
validation dispatch to terminal-label overloads. Handcrafted static scores are absent
from saved samples and target construction. The full historical process was not
re-executed or instrumented in this analysis; these findings combine persisted
evidence with the inspected production execution path.

Training continues from the latest Candidate and its complete optimizer after a
retention, in both arms. Promotions select Best and the next comparison incumbent;
they do not reset the next Candidate's optimizer. Thus divergent models, gradients,
losses and promotions are consequences of the changed supervision/objective, not
an independently changed source-data or promotion-rule confounder. No integrity
discrepancy was found.

### Recorded longitudinal WDL-only results

These are arithmetic means of **recorded final training-partition losses** and
recorded Candidate/incumbent held-out losses on each generation's own batch. They
are not a single fixed-population convergence curve and are not online running-loss
averages. WDL and blended configured losses must not be directly ranked.

| Replay generations | Final train WDL | Candidate held-out WDL | Incumbent held-out WDL | Promotions |
|---|---|---|---|---|
| 1..32 | 0.088267 | 0.344827 | 0.332958 | 13 |
| 33..64 | 0.093836 | 0.286361 | 0.281522 | 15 |
| 65..96 | 0.100052 | 0.287568 | 0.277189 | 15 |
| 97..128 | 0.094728 | 0.248334 | 0.250578 | 15 |

Replay g1 final train/held-out loss is **.080029/.509294**; g128 is
**.099414/.210266**. The first-to-last block held-out decrease is **28.0%**;
training-block means do not decrease monotonically and are somewhat higher late
than early. Individual recorded train losses range .064733.. .136677, and Candidate
held-out losses .142460.. .599498. There are no nonfinite-loss or step-count anomalies.

Late Candidate held-out means improve **.257500 (g97..112) -> .239167 (g113..128)**;
corresponding train means are **.095016 -> .094440**. Last-eight means are
.085809 train / .222129 held-out. This supports continuing, noisy held-out fitting
with diminishing gains, not demonstrated convergence or monotonically improving
training loss. The fixed-population comparison below confirms the qualification:
WDL loss improves only .264706 -> .261587 from g64 to g128, with a g96 regression.

Promotion spacing has median **2**, mean **2.193**, maximum **7** generations, with
at most six consecutive retentions; control spacing is 2 / 1.984 / 7. Replay
g113..128 promotes at **115, 118, 119, 121, 124, 125, 128**. Promotion remains active
late, but promotion counts are not a playing-strength measure.

Contemporaneous Best at boundaries g32/g64/g96/g128 is **g32/g64/g95/g128** for WDL
and **g32/g61/g96/g128** for 75/25. In particular WDL g96 is rejected:
Candidate WDL loss .278033 versus g95 incumbent .229821 on the g96 holdout.
Control g64 is also rejected. Their accepted Bests are evaluated separately below.

### Recomputed objectives on exactly the same frozen held-out population

Each model below was evaluated on the **same ordered 53,011 sample occurrences**,
pooled from all training004 g1..g128 held-out partitions, preserving duplicates and
position weighting. Pooled sample SHA-256 (existing count/board/WDL encoding):
`5f163a1ae2c6f0b8241fdcdf6b874d71b1c19350e950baf01a0359042ec71706`.
The exact g74 checkpoint and model hash specified earlier in this report were
verified and loaded **only for offline analysis**, never replay training.

Loss is mean `.5 * (prediction - target)^2` in native bounded output units. Blended
loss uses target `.25 * terminal + .75 * teacher`; it is **not** `.25 * WDL loss +
.75 * teacher loss`. Pearson and MAE compare native prediction to native teacher.
Extreme counts use `abs(prediction) >= .95`; they do not mean chess accuracy.

| Model | Pure WDL | Pure teacher | 75/25 blended | Teacher r | Teacher MAE | Extreme / 53,011 |
|---|---|---|---|---|---|---|
| Shared g0 | 0.460883 | 0.166583 | 0.148745 | -0.0854 | 0.4996 | 0 |
| 75/25 g32 | 0.377218 | 0.065798 | 0.052240 | 0.7566 | 0.2818 | 45 |
| 75/25 g61 | 0.369024 | 0.060260 | 0.046038 | 0.7796 | 0.2673 | 46 |
| 75/25 g64 | 0.366170 | 0.061824 | 0.046498 | 0.7743 | 0.2713 | 58 |
| 75/25 g96 | 0.372338 | 0.057279 | 0.044630 | 0.7937 | 0.2594 | 144 |
| 75/25 g128 | 0.366383 | 0.054989 | 0.041425 | 0.8012 | 0.2533 | 115 |
| WDL g32 | 0.282662 | 0.288621 | 0.195718 | 0.2435 | 0.5953 | 7,543 |
| WDL g64 | 0.264706 | 0.293057 | 0.194556 | 0.2508 | 0.5992 | 10,102 |
| WDL g95 | 0.265570 | 0.284922 | 0.188671 | 0.2706 | 0.5904 | 8,813 |
| WDL g96 | 0.277135 | 0.312940 | 0.212576 | 0.2854 | 0.6334 | 13,316 |
| WDL g128 | 0.261587 | 0.326380 | 0.218769 | 0.2569 | 0.6443 | 16,259 |

Both final Bests are g128. WDL-only reduces paired final WDL loss **28.60%** relative
to 75/25, and **43.24%** relative to shared g0. It gives up teacher agreement:
final teacher loss is **5.94x** control; blended loss **5.28x**; teacher sign agreement
is **58.24% versus 81.49%**. This is objective specialization, not evidence that
either the shallow-game outcomes or teacher outputs are true chess values.

Final WDL/control mean absolute output is **.68379/.37052**, RMS **.75198/.44823**,
absolute p95 **.99682/.82342**, and absolute p99 **.99951/.90201**. Extreme incidence
is **30.67%/0.217%** at .95 and **11.69%/0%** at .99. WDL .95 counts rise
**7,543 -> 10,102 -> 13,316 -> 16,259** across g32/64/96/128; control counts are
45 -> 58 -> 144 -> 115. Final raw pre-tanh mean absolute / maximum absolute is
**1.3374/8.1395** for WDL and **.4396/2.6334** for 75/25. Pooled score ranges are
**-32,511..32,511** and **-31,889..32,177**, respectively; these are engine search
units, not centipawns. Finite outputs can still have materially different scale.

For all ten noninitial models in this table, production `HeldOutLoss` recomputation
of their own-generation Candidate/incumbent comparison agrees **exactly** with
recorded promotion evidence; control WDL/teacher component losses also agree.
Recomputed final training losses agree bit-for-bit with recorded values. The pooled
metrics are new analysis measurements, not metrics recorded by the training run.

### Fixed-corpus evaluator outputs and volatility

The unchanged `Brn2Diagnostics` corpus has 15 roots plus 233 legal children:
**248 outputs, 210 quiet edges and 23 nonquiet edges**, SHA-256
`0503b850d5ed49686e72e601b1216270cfd40fc3bac18fac4dae697b323f3bd8`.
All 24 tested static checkpoint/corpus combinations (milestones, differing Bests
and every g121..128 Candidate) have **exactly zero BRN color-symmetry residuals**
in raw, normalized and integer score space. Loaded models remain unchanged.

The following are new static evaluations. Start/Kiwipete columns give root static
scores, not completed-search scores. Quiet delta is median absolute parent-perspective
normalized change over the 210 quiet edges. Different Best checkpoints are explicit.

| Model | Normalized range | Score range | Start / Kiwipete static | Teacher r | Extreme / 248 | Quiet delta |
|---|---|---|---|---|---|---|
| 75/25 g32 | -0.6645..+0.8173 | -21,604..26,570 | -5,734 / 14,248 | 0.8386 | 0 | 0.1276 |
| 75/25 g64 | -0.7120..+0.8421 | -23,148..27,378 | -4,458 / 8,050 | 0.8231 | 0 | 0.1197 |
| 75/25 g96 | -0.7049..+0.9533 | -22,917..30,991 | -4,225 / 2,790 | 0.8656 | 1 | 0.1387 |
| 75/25 g128 | -0.5517..+0.8657 | -17,936..28,146 | -724 / 228 | 0.8143 | 0 | 0.1652 |
| WDL g32 | -0.9801..+0.8353 | -31,863..27,155 | -8,811 / 15,082 | 0.2197 | 5 | 0.3934 |
| WDL g64 | -0.8889..+0.8271 | -28,898..26,890 | -6,442 / 5,466 | 0.0865 | 0 | 0.3498 |
| WDL g96 | -0.8868..+0.8858 | -28,831..28,799 | -16,814 / 417 | 0.5212 | 0 | 0.5309 |
| WDL g128 | -0.9318..+0.9915 | -30,293..32,233 | 5,882 / 15,770 | 0.5232 | 18 | 0.2813 |
| 75/25 g61 | -0.7641..+0.8336 | -24,841..27,102 | -4,011 / 8,986 | 0.8438 | 0 | 0.1183 |
| WDL g95 | -0.9754..+0.9198 | -31,712..29,902 | -7,082 / -5,275 | -0.3645 | 1 | 0.1803 |

WDL has larger absolute quiet-edge change at all four Candidate milestones. However,
scale-normalized spatial smoothness is not uniformly worse: at g128, quiet
delta/output-IQR is **.4079 WDL versus .5232 control**, while at g96 it is
.9414 versus .3170. Spatial edge roughness and temporal checkpoint volatility are
distinct; neither is an accuracy metric. The neutral corpus has 18 WDL final outputs
at or beyond .95 and one beyond .99, versus none in control; its incidence is much
lower than on the saved-game holdouts, so corpus choice matters.

For temporal comparisons, outputs are matched by exact position ID. Each cell below
is **mean absolute change / RMS change / Spearman rank correlation**. The late row
pools 7 x 248 deltas for MAE/RMS and averages the seven rank correlations.

| Same-position transition | 75/25 | WDL-only |
|---|---|---|
| g32 -> g64 | 0.06890 / 0.08916 / 0.95405 | 0.52177 / 0.68716 / -0.09884 |
| g64 -> g96 | 0.13139 / 0.16512 / 0.91967 | 0.42176 / 0.57001 / 0.16917 |
| g96 -> g128 | 0.11897 / 0.15400 / 0.87675 | 0.30854 / 0.41416 / 0.61124 |
| g121..128, seven adjacent pairs | 0.08410 / 0.11667 / 0.90602 | 0.19156 / 0.30816 / 0.82565 |

Late adjacent-generation mean absolute change is **2.28x** greater for WDL; pooled
RMS is **2.64x** greater. Maximum individual late change is **1.45676 versus .48744**.
WDL's late window has 32 .95-threshold appearances and 14 disappearances; control
has none. WDL milestone ordering loses most rank consistency between g32 and g64
(rank correlation -.09884), whereas control retains .95405. WDL neutral-corpus
teacher correlation moves .2197 -> .0865 -> .5212 -> .5232; its accepted g95 is
**-.3645**, illustrating a large one-generation change. Control milestone teacher
correlations stay .8143.. .8656. This is strong descriptive evidence of greater
WDL temporal volatility on these identical positions, not a statistical claim over
all positions, all generations or independent runs.

### Bounded normal-search and qsearch diagnostics

All ten selected checkpoints were newly tested on **all 15 roots**, preserving the
prior method: depth **4**, **1,000,000 nodes / 10,000 ms per search**, one thread,
cold 262,144-entry TT, full windows, mate-distance-only selectivity, singleton-root
history, **one warmup plus two measurements**, `qshadow=false`, and a **40-second
process watchdog per root**. No bound was increased. Optional qshadow was not
needed for the paired static/normal-search conclusions and was not rerun.

All 450 searches have exact repeat agreement in status, depth, nodes, score, PV,
evaluation counts and recorded non-time diagnostic fields. There are no errors,
timeouts or watchdog terminations. Every checkpoint correctly adjudicates the two
terminal roots: checkmate -32,768, stalemate 0, no legal move and zero entered nodes.
The table counts completed nonterminal roots; node totals are for one corpus pass,
not the sum of repeated measurements. Main + qsearch = total.

| Model | Depth-4 completion | Main nodes | Qnodes | Total nodes | Node-limit roots |
|---|---|---|---|---|---|
| 75/25 g32 | 13/13 | 58,377 | 598,145 | 656,522 | 0 |
| 75/25 g64 | 13/13 | 36,881 | 799,747 | 836,628 | 0 |
| 75/25 g96 | 12/13 | 28,214 | 1,020,478 | 1,048,692 | 1 |
| 75/25 g128 | 13/13 | 39,941 | 690,294 | 730,235 | 0 |
| WDL g32 | 12/13 | 36,440 | 1,072,811 | 1,109,251 | 1 |
| WDL g64 | 13/13 | 51,168 | 88,925 | 140,093 | 0 |
| WDL g96 | 12/13 | 35,932 | 1,216,125 | 1,252,057 | 1 |
| WDL g128 | 13/13 | 35,227 | 207,156 | 242,383 | 0 |
| 75/25 g61 | 12/13 | 36,767 | 1,121,705 | 1,158,472 | 1 |
| WDL g95 | 13/13 | 49,011 | 705,852 | 754,863 | 0 |

All limit hits are at the established Kiwipete root. Exact sentinel results:

| Model | Total nodes | Qnodes | Completed depth | Status |
|---|---|---|---|---|
| 75/25 g32 | 510,715 | 495,345 | 4 | COMPLETED |
| 75/25 g64 | 721,099 | 712,073 | 4 | COMPLETED |
| 75/25 g96 | 1,000,000 | 996,602 | 3 | NODE_LIMIT |
| 75/25 g128 | 681,257 | 671,068 | 4 | COMPLETED |
| WDL g32 | 1,000,000 | 996,963 | 2 | NODE_LIMIT |
| WDL g64 | 78,932 | 61,047 | 4 | COMPLETED |
| WDL g96 | 1,000,000 | 998,945 | 2 | NODE_LIMIT |
| WDL g128 | 211,778 | 198,448 | 4 | COMPLETED |
| 75/25 g61 | 1,000,000 | 996,783 | 2 | NODE_LIMIT |
| WDL g95 | 707,649 | 692,141 | 4 | COMPLETED |

The original control g96 result reproduces exactly: **1,000,000 / 996,602 qnodes,
depth 3**. WDL Candidate g96 is operationally worse there: **998,945 qnodes and
only depth 2** at the same bound. But it is rejected; contemporaneous WDL Best g95
completes depth 4 at **707,649 / 692,141**. The additional control-Best check is
also material: at the g64 boundary, accepted **g61 hits the limit at depth 2**,
even though Candidate g64 completes. This previously unmeasured accepted checkpoint
must not be omitted from the interpretation.

Across four *Candidate* milestones, WDL has two failing root/checkpoint combinations
and 75/25 one (out of 52 nonterminal combinations per arm). Across their four
*contemporaneous accepted Bests*, WDL has one and 75/25 two. These small correlated
descriptive counts do not establish failure probabilities or superiority. Neither
arm eliminates the pathology. WDL failures recur at g32 and g96 but are absent at
g64, accepted g95 and final g128; persistent failure at every checkpoint is disproved.

Both final Bests complete all 13 nonterminal roots. WDL uses **242,383 total /
207,156 qnodes**, versus control **730,235 / 690,294**; Kiwipete alone is
211,778 / 198,448 versus 681,257 / 671,068. Final qnode fractions are approximately
85.47% versus 94.53%. Thus the more volatile/extreme final evaluator is cheaper in
this bounded search corpus. Node cost cannot be equated with evaluator quality,
and these results do not establish a monotonic relationship between smoothness,
teacher agreement and search cost. The 18 directly comparable prior control search
records (three earlier sentinels plus the complete final corpus) reproduce exactly.

### Scientific answers and next experimental question

**A. Did WDL-only learn terminal WDL?** Yes in the measured loss sense: common
held-out WDL loss is substantially below shared initialization and below 75/25.
Learning is noisy and shows diminishing late gains; convergence, calibration and
independent generalization are not established. Training-batch loss itself is not
a steadily decreasing longitudinal curve.

**B. Smoother or more volatile?** More temporally volatile, with greater output
magnitude, saturation incidence and absolute quiet-edge changes. Structural color
symmetry remains exact. Some scale-normalized spatial ratios improve, so a blanket
claim about every smoothness measure would overstate the result.

**C. Did removing the teacher improve qsearch pathology?** No consistent direction
across the trajectory. The same pathology class remains, Candidate g96 is worse,
and Candidate failures are more frequent among the four sampled milestones; yet
accepted-Best failure counts favor WDL in this sample, and WDL g64/final g128 are
cheaper and complete. It is checkpoint-dependent in both arms, not uniformly fixed
or worsened by removing the teacher.

**D. Does this support teacher stabilization?** Strongly for temporal output
stability, restrained scale and teacher agreement; only partially for the broader
search-operational hypothesis. It does **not** establish that teacher supervision
reliably prevents qsearch explosions or always yields a better operational Best.
The failing accepted control g61/g96 and cheaper WDL endpoint are counterevidence
to that stronger assertion.

**E. Did Handcrafted data alone solve WDL instability?** No. With those exact boards
and labels frozen, WDL still has large temporal changes and repeated bounded
qsearch failures. A completing final checkpoint does not erase the trajectory.

**F. Are differences attributable to supervision?** Reasonably yes within this
single paired experiment: corpus/labels/order/partitions, initialization, optimizer,
architecture and lifecycle controls pass the checks above. Changed training and
validation targets cause different gradients, Candidates, comparisons and Best
selections. Those downstream changes are part of the treatment, not extra
confounders. This does not prove the same effects for other corpora or seeds.

**G. What next is justified?** Retain 75/25 as the measured stability reference,
without declaring it the preferred playing evaluator. The clearest next scientific
question is whether a **fixed intermediate teacher weight on this same frozen
corpus and initial state** can retain some WDL-loss gain while reducing temporal
volatility and extreme outputs. A predeclared midpoint such as .50 is an informative
additional dose, not an evidence-established optimum. Assess both Candidates and
contemporaneous Bests using the same common objectives and bounds; endpoint-only
search checks would miss the observed failures.

This pair does not yet justify teacher annealing over a simpler fixed-weight test,
nor isolate a reason to change loss formulation or BRN-2 architecture first. Those
would add mechanisms before the teacher-weight tradeoff has been measured here.
The existing `frozen-wdl` command deliberately accepts only canonical WDL; an
intermediate-weight experiment would require a separately authorized, verified
workflow, not an undocumented option or mutation of these stores. No next regime
was implemented or run. A later decision about playing quality also requires a
separate direct strength comparison and preferably independent evaluation data.

### Limitations, validation and protected state

This is one frozen corpus/initialization/seed realization. Saved holdouts are
game-correlated, position-weighted, reused for adaptive promotion and not an
independent test set; pooled early-checkpoint evaluation also includes later
generations' saved holdouts. We did not establish independence of every distinct
position across train/holdout generations. Shallow terminal outcomes and exact g74
teacher targets disagree, and neither is stipulated ground truth for chess quality.
Only four main search milestones plus the differing accepted Bests, and an
eight-generation late static window, were inspected; no full 128-generation search
incidence or general statistical error bar is claimed.

**Playing-strength superiority remains unmeasured.** No loss, promotion count,
teacher correlation, static metric or node count above substitutes for strength
games. Live Handcrafted generation nondeterminism remains separate and was neither
investigated nor changed.

Validation performed: `:app:classes`; the temporary read-only Java adapter compiled
and completed all production provenance/lineage/optimizer checks; all 128 exact
batch comparisons; 412 retained payload hash/header checks; 10 exact recorded-loss
recomputations; common-population objectives; 24 static diagnostic processes; 150
bounded search processes / 450 attempts; independent summaries cross-checked using
the existing `tools/analyze-brn2-supervision.py` static/search readers; 18 prior-control
runtime matches; and `git diff --check`. Raw commands, receipts, immutable-store
inventories, metric outputs and temporary helpers are local ignored evidence under
`app/build/brn2-frozen-wdl-analysis/`. This durable section contains the substantive
findings; no checked-in analysis framework or production code change was needed.

Before/after inventories match for **all 4,226 protected files, 25,746,148,209 bytes**:
training001 **948**, training002 **959**, training003 **23**, training004 **951**,
NNUE training **401**, replay training005 **944**. Membership, file sizes,
nanosecond modification times and SHA-256 values are unchanged. No network or
optimizer store was modified. No training was started/resumed, no self-play or
position generation occurred, and no strength campaign was started in this unit.
BRN/search/training behavior is unchanged.

Full/long suites, new training, generation, strength games and further qshadow
instrumentation were deliberately skipped; browser verification is inapplicable
to these headless diagnostics. The only tracked change is this appended report.
Inherited untracked `app/bin/` is preserved and excluded. Root `CODEXLOG_CURRENT.md`
and `VERSION_STATE.txt` are absent, so neither capability was activated or created.
The isolated report is committed without pushing; this analysis completion does
not itself constitute user acceptance or a deployment/regime-selection decision.

Human actions required after this prompt: None.
