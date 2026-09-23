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

## Remaining work and human actions

**The next GPT conversation must decide the supervision regime for the revised
handcrafted-generated BRN-2 baseline campaign before starting a long run.** WDL is
the software default; this unit does not select a future experimental teacher
weight. Use a new lineage for that future campaign. training001/002/003 and the
accepted experimental stores remain historical evidence.

Human actions required after this prompt: None for this implementation.
The supervision decision above is **blocking any future long campaign**. No such
campaign is authorized or started by this report.
