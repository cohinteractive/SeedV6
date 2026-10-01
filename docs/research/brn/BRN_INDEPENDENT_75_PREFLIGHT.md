# BRN-2 independent 75%-teacher lineage: generation-1 preflight

Measured 2026-09-23 UTC in `C:\projects\seed\java\seedv6`, starting at
`5df13cb5e90bd0b89ea423ed270e3ab0ee24eaf7`, following the accepted
[normal supervision integration](BRN_SUPERVISION_TRAINING.md) and
[training002 diagnostics](BRN_FRESH_75_TRAINING_DIAGNOSTICS.md).

**INDEPENDENCE GATE: PASS.** `E:\SeedV6-Networks\BRN\BRN-2\training003`
completed exactly one normal production generation. Both Best and latest-training
are g1, optimizer step 1,632. The service stopped by its existing limit of one;
no g2 attempt, plan, data, update or checkpoint exists. The store is settled and
resumable. This establishes a different deterministic sample dataset, not yet
128-generation generalization, playing strength, or acceptance of a new target.

## Seed investigation and controlled change

The old GUI exposes **Model / run seed**, saved as Java Preferences `seed` and
passed to `TrainerConfig.masterSeed`. Its default and the current prior-campaign
preference are 1. More decisively, every persisted g1-g128 plan in **both** prior
stores exactly matches the generation settings derived from master seed **1**.
Their g0 model and optimizer identities also match.

`TrainerConfig.seed(generation, domain)` uses:

```java
new SplittableRandom(base ^ domainSalt ^ (generation * 0x9E3779B97F4A7C15L)).nextLong()
```

The multiplication uses Java signed-long wrapping arithmetic. Existing salts are
SELF_PLAY `0x6A09E667F3BCC909`, SHUFFLE `0xBB67AE8584CAA73B`, VALIDATION
`0x3C6EF372FE94F82B`, and HOLDOUT `0xA54FF53A5F1D36F1`. Previously every base was
masterSeed. The opt-in data seed now supplies **only SELF_PLAY's base**. There is
no new RNG or parallel generation path.

`SelfPlayRunner.gameSeed(generationSeed, gameIndex)` is
`new SplittableRandom(generationSeed + 0x9E3779B97F4A7C15L * gameIndex).nextLong()`.
That indexed stream chooses opening length and uniformly selected legal opening
moves. Thereafter the pinned NNUE actor performs normal deterministic search.
The BRN student, store path, process identity and wall clock do not generate these
games. `TrajectorySampler` then selects evenly spaced trajectory positions; it
has **no random sampling seed**. `BootstrapPartition` separately shuffles eligible
whole-game IDs with HOLDOUT. BRN training separately shuffles the ordered training
samples with SHUFFLE and performs one online Adam pass.

Consequently training001 and training002 regenerated the same data because all
these generation settings and the NNUE actor were the same. training001 has WDL
supervision; training002 has .75 blended supervision. The objective changes BRN
updates, not the external NNUE generation path.

| Seed role | training001 / training002 | training003 |
|---|---:|---:|
| SELF_PLAY base / **Self-play data seed (optional)** | 1 (inherited master) | **2** |
| **Model / run seed** for remaining domains | 1 | **1**, unchanged |
| g1 derived SELF_PLAY | 7921502845091313457 | **-8423315213392911525** |
| g1 derived SHUFFLE | -6540313355536843707 | unchanged |
| g1 derived HOLDOUT | 8110949293515089404 | unchanged |
| g1 derived VALIDATION (unused for bootstrap) | -5043335890038756556 | unchanged |
| BRN-2 weight initialization | `0x533642524e320001` | unchanged |

The selected base is priorSeed + 1, valid in the existing signed 64-bit Java long
domain. All 64 indexed game seeds change. Examples for game IDs 0-3:

```text
old:  2496977766053201155, -8786997512457846022, -7911419901428938252, -8431097083363358507
new: -1947336248985643692, -2802854351967886024, -4863217132652723203, -1610579614551026933
```

These are identifiers derived from the persisted plan seed, not separately saved
opening move lists. Full move trajectories are not retained by the existing batch
format; the actual sampled six-long boards and terminal WDL targets are persisted.

## Minimal configuration and persistence support

Changing the old master control alone also changes SHUFFLE/HOLDOUT and does not
lock seeds to the lineage. The BRN-2 card therefore adds **Self-play data seed
(optional)**. Blank retains current defaults and legacy behavior exactly. An
explicit value on a fresh store selects `BrnRunSeeds(masterSeed, dataSeed)` and
atomically writes checksummed `brn-run-seeds.bin` before g0 publication. Existing
stores without that record are not migrated and cannot adopt an override on Resume.
NNUE/BRN-0/BRN-1 seed behavior is unchanged; the override requires BRN-2.

Both values are immutable for an opted-in lineage. Unspecified Resume restores
both from the store, even with a stale caller master seed. Explicit incompatible
requests fail before writer opening/reconciliation, and are checked again under
ownership. Generation settings retain the actual derived streams and append
`|brn-run-seeds-v1:1:2`, binding the lineage seeds into plans and attempts. Missing,
corrupt or contradictory seed records fail closed against those durable settings.
Interrupted initialization retains the seed choice. Stop before publication reuses
persisted data and starts again from the settled optimizer boundary.

GUI fields follow startup/running/stopping/closing locks. On selection of this
existing store, **Model / run seed = 1**, **Self-play data seed (optional) = 2**,
**Supervision = NNUE blended**, and **NNUE teacher weight (%) = 75** restore and
lock. Draft preferences are folder-bound; store metadata is authoritative.
The model, optimizer, feature schema, evaluator and generation-limit codecs or
semantics were not changed. Final review corrected the seed-only interrupted-
initialization GUI case: seed and supervision locks follow their own durable
records, so saving seeds alone cannot invent a locked WDL objective. The new
regression test and affected native GUI suite passed after this correction.

Persisted metadata SHA-256:

```text
brn-run-seeds.bin:   5c9b58793b68a115a74e5da9b3717611a30e50ff01d7997c1084b997b2a1e0a1
brn-supervision.bin: 814f457f9ff7c082a7d539bd51abaae243cc264d3d4a01c496b80c648e1115c7
```

## Exact production configuration and teacher

- Architecture **BRN-2**, ID `seedv6.brn.2`, feature schema **2**, hidden width 32.
- Source **Bootstrap with NNUE**; Generator store `E:\SeedV6-Networks\NNUE\training`.
- Target **.25 * terminalWdlTarget + .75 * nnueNormalizedTeacherTarget**, evaluated
  on the stored sample's side to move using the accepted native teacher mapping.
- Training depth **2**, search threads **6**, games/generation **64**.
- Standard chess starting FEN; opening plies **0-8**, maximum samples/game **32**,
  maximum game plies **1,024**; no per-move node/time cap.
- Initial learning rate **.001**, Adam beta1 **.9**, beta2 **.999**, epsilon **1e-8**;
  one shuffled online pass, minibatch 1. Exact existing optimizer semantics.
- Normal held-out strict-loss promotion. Game-pair validation is inactive.
- This service invocation requested **1** additional generation.

The normal source resolver validated current accepted NNUE Best before startup;
it remained the same after settlement. The generation plan, validation, history
and actual model payload agree on the accepted g74 teacher/Generator:

```text
E:\SeedV6-Networks\NNUE\training\checkpoints\g000074-s000008942-2e22ccbfdeb18f5f26f91f0df183411ac1d01844ccd43e56e9005ee3f5837bc6
network.nnue:  3279ff72654f56c8f5ce3cc89cc55d1f253de6ae9df00bd61a41293f57dd16a9
training.state: 453b7259d4e8041fa4abb8cc0887540434871d2b151c33676f36e749e0c6cb34
```

There was no teacher substitution. Its own checkpoint training depth is 6; this
experiment's game search depth is 2, as in the prior campaigns. The normal workflow
still selects current NNUE Best for each new generation and pins it through any
unfinished generation. Future continuation therefore requires keeping NNUE Best
at this g74 if the campaign is to retain the same experimental control.

## Settled g0/g1 identities and publication

Exact checkpoint directory IDs under training003/checkpoints:

```text
g0: g000000-s000000000-31a42b7e131558c59e7868e45b759f1e5a322eb2bef5ff26e7b9386e4f831507
network.brn2:  195d4300ce1b90a30cb888d6990a872f33165bfadd66cf1f0212ba3f0c2f653f
training.state: dec6eccdf4d9e59d4ea9f9843831548a045e07d050032f3c4ab62767d8c595bc

g1: g000001-s000001632-39524fbf0c53614b4e762d0b0a13af2603369978fcd5cc94602cbed576662596
network.brn2:  c1d0fd4f7fd440c14b2a821b8590a31a26691368c24f11d78460559084ddd47c
training.state: 834e6b5ffac7a0a6a4efd7317d7fbd47f889690022a7abb7408f2d9b086bb8ed
validation: v-9a8652abbdbc33320b33837a43d1f8fa1f503118990072ea167240bbcfaca91b
```

G0 is byte-identical to both prior initializations. Both g0/g1 payloads were loaded
and validated against their manifests. G1 completed at
**2026-09-23T05:00:43.532146400Z**, after a measured 14.5664868-second lifecycle.
It made **1,632 updates**. All **64 games** completed, no caps/aborts; White wins
24, draws 20, Black wins 20; 10,523 played plies, 2,048 retained samples.

Training configured loss is .026057892182427128. On the 416 held-out samples:

| Objective | Candidate g1 | Incumbent g0 |
|---|---:|---:|
| Configured blended loss | .064057948522492 | .1910839886008395 |
| Descriptive WDL loss | .15456955845687198 | .2805649378637041 |
| Descriptive teacher loss | .06881362892309584 | .19618322255861492 |

The normal strict configured-loss rule **PROMOTED** g1. The accepted publication
chain, `refs/best`, `refs/latest-training`, one validation and one checksummed
schema-3 history row agree. `generation-attempt.bin` records settled generation
**1**, format `CURRENT`, empty restart notice. `bootstrap/` contains just its one
plan and one data file; `staging/` is empty, with no restarted-generation archive.
The service is STOPPED, terminated, and its writer lock was independently acquired
and released after completion. No generation-2 boundary was entered.

## Persisted independence evidence

SHA-256 of the complete physical plan/data files (including record framing):

| Store / g1 | Plan | Data/batch |
|---|---|---|
| training001 | `a2ab9759d0992f88e6598a6d73477c308dca28fef4398c3e913cfa574b3df03a` | `b4f047f8d2c03363195f9bba14333ad18d60d51fbb9465a3a015c83d66a4f0ec` |
| training002 | `2119b0093b8f191a8d49ca35ab962de3017688b88698b0905ea8b950b7505de2` | `7f0c770af01c565159d8495a6732a40f53b6c3c086f8ec9b5880c83e351bfb38` |
| training003 | `3b65980fc5451d1b9d47515b441c5fee2294bc22618412af96c437f8276dca2c` | `8367864a53653cd2ac4f51de2a66344164325284221a2b2e22a8e60d9e5effca` |

Different envelope hashes alone do not prove independence: supervision and measured
generation time already made the first two stores' envelope hashes different.
The decoded sample/position evidence below removes that ambiguity. All three have
**1,632 training samples in 51 games**, **416 held-out samples in 13 games**.

| Content | training001 = training002 | training003 |
|---|---|---|
| Ordered training samples | `012bdec45cf657cde281575a95c04ad235b0311ba18fb7ecdecfa5116abf435a` | `63a23c291e66d0cefc4a4e26642c0814e9c4ed698141d52cd788777b189a89d2` |
| Ordered held-out samples | `4b272745ddfc496feffe50d4517ac5c57697328e3dac5526fcc76f6e28a2b786` | `01d82f459a73ad1c6ff93eefea9b92caa2cb478731d3a6b4c128338ce786a3ec` |
| Ordered training positions | `37c761cad925b1f64a5012bfbc117e980a1d8646758aa87afef5a18dd411f375` | `7ccfc2319c3ab78a474afa01dfcfdbdddabf12a8a967862daefdf03c03baeeb9` |
| Ordered held-out positions | `b7d2bc9bf9fab934ffccef054dac7d4942c41765eeec3b97083ba6fc69b9a1fa` | `57e036682d0bb78d67e610fec6b8b22325ce4dec9d72287ab423a05406d102bc` |
| Content-only batch | `d835e6eb39ead06fc3b4348e7c418a359e703106fcafdc8e5471b3ed8e296fca` | `9186a9e5f4997a1bd4212396ca95f79c9b821603c053f5fb6eb6718d895c4523` |
| Whole-game partition IDs | `2ba07d038a2f7475439b9e90f6a2190ee3cb7c98109a4a569b9fbd2eda7f6dca` | same |

Sample hash encoding is Java DataOutputStream: big-endian int count, then each
sample's six board longs followed by its WDL double. Position hashes omit the
double. Content-only batch uses production `BootstrapData` serialization with
planHash replaced by 64 zero characters and generationNanos by zero, retaining
all samples, IDs and game statistics. ID hash encodes training count/IDs followed
by held-out count/IDs as big-endian ints.

All stores reserve game IDs **7, 12, 13, 21, 22, 24, 35, 36, 38, 52, 55, 61, 63**;
the remaining IDs in 0-63 train. This unchanged partition assignment is intentional:
the games at those indices have different random opening streams. Each old batch
has 1,737 distinct six-long positions; training003 has **1,738**, with only **32**
in common (1,706 new distinct stored positions). Both ordered position partitions
and the complete sample records differ. This is more than a changed plan, partition or
sample order.

Control assertions compare the new plan with both prior g1 plans: after replacing
only SELF_PLAY's derived seed and removing the new seed metadata suffix, generation
settings match exactly. Parent, incumbent, Generator store/ID/hash and split seed
match. Supervision matches training002 exactly; training001's known WDL objective
is the canonical data comparator, not a new confounding change. G0 payload hashes
match all three. Same-seed temporary production runs reproduce plan, batch content,
Candidate identity and optimizer bytes; different-seed fixture runs differ in
actual samples. Real g1 records were reread through checksummed production readers.

## Validation and provenance

**74 distinct focused tests in eight suites passed, zero failures/errors/skips.**
This is the union of latest applicable results, not a sum counting reruns twice.

| Suite | Tests |
|---|---:|
| BrnRunSeedsTest | 5 |
| BrnRunSeedsGuiTest | 3 |
| Brn2SupervisionGuiTest | 5 |
| Brn2BlendedBootstrapTest | 7 |
| BrnBootstrapTest (BRN-0/1/2 compatibility) | 17 |
| BrnSupervisionPersistenceTest | 4 |
| StoppedReconfigurationTest (including NNUE boundary) | 27 |
| TrainingSettingsValidationTest | 6 |

Tests cover unchanged default domain constants; signed-long seed limits; fresh
persistence and interrupted initialization; exact partial-update Stop/restart/
Resume with no new search and byte-identical optimizer; explicit edit rejection
without archival; corrupt/missing metadata; same/different-seed real-search data;
legacy stores and NNUE; folder-bound preferences; visible stored values and all
lifecycle locks. The bounded native Start smoke completed a temporary generation
with an explicit seed, using normal search, training and publication.

The actual training003 configuration was separately opened read-only through the
rebuilt GUI classes with a stale caller master seed. It restored the durable
1/2 seeds and .75 objective and displayed their locks. No Start/Resume action was
sent. Both native screenshots were visually inspected without field clipping.
A first source-file-mode GUI inspection failed on Java package-private class-loader
access; compiling the local helper into the normal application classpath resolved
that launcher issue. No product/test failure was suppressed.

`:app:compileTestJava`, focused `:app:test` selections, test-dependent
`:app:installDist`, and CRLF-aware `git diff --check` passed. An initial selector
`*TrainingSettingsTest` matched no suite; the actual six-test
`*TrainingSettingsValidationTest` was then explicitly run successfully.

The training003 path was absent immediately before launch; the launcher refuses
any existing path. Only one real generation was run via public `TrainerService.fresh`, using
`new Brn2Trainer(.001)` and maximumGenerations=1. No replay harness generated or
trained training003. Local runner, read-only verifier, commands/logs, copied JUnit
XML, inventories and GUI screenshots are under ignored
`app/build/brn2-independent-75-preflight/`. The durable evidence and hash encodings
above do not depend on retaining that build directory.

Deliberately skipped: remaining 127 real generations, fullCheck, broad GUI/slow
suites, strength matches, supervision sweeps, calibration, inference optimization,
BRN-3 and unrelated testing. No browser was needed for this native Swing work.
No full-campaign performance, generalization or strength conclusion is available.

Before/after SHA-256, size, modification time and membership are identical for all
**2,400 protected files**: training001 948, training002 959, NNUE training 401,
accepted ablation 55, accepted weight sweep 37. No protected writer was opened.
No accepted payload or checkpoint was committed.

Starting Git state was accepted `5df13cb` plus inherited untracked `app/bin/` only.
The directory was not removed, reset, staged or committed. Its active IDE build
output refreshed during source edits: no inherited file was removed, five class
files appeared and 14 inherited class contents changed (others had timestamp-only
refreshes). These changes correspond to edited/new Java sources and are consistent
with the running VS Code Java language server; the individual writer was not
traced. They are preserved as generated untracked state, not represented as
byte-unchanged inherited content. Source/report attribution remains isolated.

This work unit changes the eight existing production configuration/service/store
files, adds `BrnRunSeeds.java`, adds two focused seed test classes, extends the
existing native supervision GUI smoke, adds this report, and links it from
`BRN_DIAGNOSTICS.md`: **14 task files**. Model/search/optimizer implementation is
unchanged. These task changes form one focused commit; its exact hash is in the
completion response. Final tracked worktree is clean with `?? app/bin/` remaining.
No push, deployment, amend, rebase or history rewrite occurred. Exact root
`CODEXLOG_CURRENT.md` and `VERSION_STATE.txt` were absent and not created.

Exact committed task files:

```text
BRN_DIAGNOSTICS.md
BRN_INDEPENDENT_75_PREFLIGHT.md
app/src/main/java/com/ohinteractive/seedv6/gui/Brn2ConfigurationPanel.java
app/src/main/java/com/ohinteractive/seedv6/gui/TrainingController.java
app/src/main/java/com/ohinteractive/seedv6/gui/TrainingPanel.java
app/src/main/java/com/ohinteractive/seedv6/gui/TrainingSettings.java
app/src/main/java/com/ohinteractive/seedv6/training/checkpoint/CheckpointInspection.java
app/src/main/java/com/ohinteractive/seedv6/training/checkpoint/CheckpointStore.java
app/src/main/java/com/ohinteractive/seedv6/training/service/BrnRunSeeds.java
app/src/main/java/com/ohinteractive/seedv6/training/service/TrainerConfig.java
app/src/main/java/com/ohinteractive/seedv6/training/service/TrainerService.java
app/src/test/java/com/ohinteractive/seedv6/gui/Brn2SupervisionGuiTest.java
app/src/test/java/com/ohinteractive/seedv6/gui/BrnRunSeedsGuiTest.java
app/src/test/java/com/ohinteractive/seedv6/training/service/BrnRunSeedsTest.java
```

## Human actions required after this prompt

**Blocking completion of the 128-generation experiment, not this completed preflight:**
continue the existing training003 lineage for **127 additional generations**.
Close an older running app and open the rebuilt distribution from the repository:

```powershell
.\app\build\install\seedv6\bin\seedv6.bat gui
```

1. In Training, set **Network Architecture = BRN-2**, open **Configuration**, and
   set **BRN checkpoint store (student)** to
   `E:\SeedV6-Networks\BRN\BRN-2\training003`.
2. Wait for stored values to load. Verify **BRN Training Source = Bootstrap with
   NNUE**, **NNUE Generator Store = E:\SeedV6-Networks\NNUE\training**. Keep its
   current Best at the verified g74 throughout this controlled continuation.
3. Verify locked **Model / run seed = 1**, **Self-play data seed (optional) = 2**,
   **Supervision = NNUE blended**, **NNUE teacher weight (%) = 75**, **WDL: 25.00%**.
   Verify **Training depth = 2**, **Search threads = 6**, **Games / generation = 64**,
   **Opening min. plies = 0**, **Opening max. plies = 8**, **Samples / game = 32**,
   **Maximum game plies = 1024**, and **Initial learning rate = .001** (Resume
   restores the existing optimizer). Validation pairs are inactive.
4. Set **Generations (0 = unlimited) = 127** and click **Start / Resume Training**
   (displayed as **Resume Training** once the controller recognizes Resume).
   Start applies the settings. Use this existing g1 store; do not initialize a new
   store or restart from g0.
5. After automatic stop, verify **128 completed generations** are settled in
   History/Diagnostics before later campaign analysis. If paused with **Stop
   Training**, the next Resume limit is 128 minus the completed lineage count,
   because the unchanged limit counts new completions per service invocation.

**Blocking later full-campaign analysis:** provide that settled g128 evidence.
This preflight does not claim user acceptance or completed campaign operation.
