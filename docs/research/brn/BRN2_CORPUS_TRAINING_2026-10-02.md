# BRN-2 corpus integration and bounded validation

## Boundary and authority

The authoritative repository was clean at initial HEAD
`7a9c1ec029310c1fd687d7073e93189fb3880bc9`. This unit adds headless corpus
supervision for BRN-2 BASIC_V1, with no GUI, NNUE, search, move-generation,
architecture, importer or corpus-schema change. The user explicitly approved
`side-to-move CP / 32511` for BASIC_V1 corpus mode after inspection established
that existing generated targets were WDL or bounded NNUE values. This approval
does not change targets in existing regimes.

Root `CODEXLOG_CURRENT.md` was absent; no journal was created. Root
`VERSION_STATE.txt` existed with build 33. The maintained finalizer/recovery
bindings were verified against the supplied hashes and maintained-source commit
`53fac440d2da90642e926492f1278d3f98dc9939`. Before mutation, finalizer `begin`
was blocked by inherited operation `9e5cb01570284814a3a7f45a498ae785`.
Read-only status identifies that operation as blocked, with uncertain mutation,
no verified bump, before-build 32 and no verified after-build. Its recorded
failure includes ignored Gradle locks/caches and generated resources changing
outside VERSION_STATE.txt. This task did not own, finish, recover or alter that
operation. Build 33 remained unchanged throughout this unit.

Implementation and functional validation proceeded under the supplied
DEFERRED_FINALIZATION safe branch. This report is separate durable evidence of
the later work, not attribution to the inherited operation. Authoritative version
finalization remains deferred; no version transition is claimed. Resolving the
inherited operation requires a separately authorized governance work unit before
operations dependent on finalized version provenance. No release or push occurred.

## Implementation

`TrainingSource.EXTERNAL_CORPUS` is additive. Select
`TrainingSource.corpus(root)` and `CorpusTrainingConfig(positionsPerGeneration)`;
existing `TrainerConfig.masterSeed` supplies the campaign seed. The service adds
the pinned `viewIdentity` to durable settings. Existing constructors and generated
branches retain their behavior.

The architecture-neutral `CorpusReader` supplies located logical records to an
immutable `CorpusView`. Its unshuffled disk index uses 16 bytes per eligible
record, retains physical shard addresses, and copies no corpus positions or labels.
Metadata binds the original manifest, policy, exclusion counts, shard identities
and hashes, plus index hash. Compatible appends and stronger-label catalog
updates preserve historical labels. Missing or changed pinned files fail clearly.
Index and shard checks run once per service open, not per generation.

Ordering uses six-round balanced Feistel, fixed unsigned integer mixing and
cycle walking over the next power-of-four domain. It uses constant permutation
memory and seed/epoch keys, independent of JVM RNG. A fixed global permutation
partitions eligible identities into disjoint training and holdout strata; each
stratum advances through contiguous epoch ranges. Generation g starts at
`(g-1)*positionsPerGeneration`; exhaustion deterministically crosses into the next
epoch. There are no repeated training identities within a training epoch, even
when one generation spans several epochs. Golden vectors and small/large-domain
tests document replay.

White-perspective CP is negated exactly once when Black moves; STM-perspective
CP keeps its sign. The result is divided by 32511. Mate, raw-source perspective
and out-of-range CP are excluded at admission and counted. Schema 1 rejects
missing/unknown target kinds. Unknown halfmove clocks explicitly use zero;
known clocks use existing Seed saturation. Corpus targets are never rewritten.

`CorpusPosition.toBoard` reconstructs the existing six-long Seed state and key.
The adapter supplies bounded CP targets through the existing target selector on
strict `TrajectorySampler.Sample` wrappers. Existing BRN-2 canonical features,
combined tanh output, half-squared loss, backprop and online Adam perform the
optimization. Corpus mode branches before position generation. It publishes
normal candidates, compares Candidate/Best on identical disjoint CP holdout,
records corpus evidence/history with zero games, and settles through the existing
strict-lower-loss promotion and CandidateLifecycle. Partial optimizer resume and
pending-candidate recovery are covered by tests.

See [operational contract](../../corpus-training.md) for configuration, policy,
storage costs, epoch semantics and runner usage.

## Real production-path validation

The corpus at `E:\SeedV6-Corpus\corpus` contained 10,000 unique logical records
in five shards: 7,865 CP and 2,135 mate records, all White perspective and unknown
halfmove clock. No new data was imported. Two isolated equivalent fresh runs
were performed (initial implementation and final implementation), each one
generation with 2,000 training and 500 held-out examples, seed 71 and existing
learning rate .001. Both completed normally, retained their isolated incumbent,
and produced identical receipts and persisted network bytes.

Final command:

```powershell
.\gradlew.bat :app:brnCorpusTrain --no-configuration-cache -PcorpusRoot='E:\SeedV6-Corpus\corpus' -PcorpusTrainingOutput='C:\Temp\SeedV6-corpus-smoke-final-20261002' -PcorpusTrainingArgs='--seed=71 --positions=2000 --generations=1'
```

The final run passed in 11 seconds. A separate JVM replay passed in 2 seconds:

```powershell
.\gradlew.bat :app:brnCorpusTrain --no-configuration-cache -PcorpusRoot='E:\SeedV6-Corpus\corpus' -PcorpusTrainingOutput='C:\Temp\SeedV6-corpus-smoke-final-20261002' -PcorpusTrainingArgs='--seed=71 --positions=2000 --replay=1'
```

The initial run used `C:\Temp\SeedV6-corpus-smoke-20261002` and its separate JVM
replay also passed. A read-only comparison confirmed complete generation receipt
equality between outputs, equal actual network payload hashes, and unchanged
hashes of every pinned real-corpus shard after the final run. Current BRN Best
stores were not used or replaced.

| Measurement | Result |
| --- | --- |
| Requested / usable training | 2,000 / 2,000 |
| Generation records examined / holdout | 2,500 / 500 |
| View records examined / admitted | 10,000 / 7,865 |
| Admission skips | mate 2,135; unsupported/range 0 |
| Generation selection skips | 0 |
| Optimizer cursor / actual updates | 0 to 2,000 / 2,000 |
| Generated/self-play games | 0 |
| Initial training loss | 2.898316648899138E-4 |
| Final training loss | 2.9310321227498336E-4 |
| Mean online training loss | 2.9847661248760246E-4 |
| Prediction mean / target mean | .004348313119942021 / .0006664052166958876 |
| Candidate / Best held-out loss | 1.3959997063099842E-4 / 1.3176585133503195E-4 |
| Lifecycle decision | RETAIN_INCUMBENT; generation complete |
| Persisted parameters changed | true |
| Eligible index size | 125,840 bytes |

Loss increased slightly; this validates mechanical optimization and normal
retention, not improvement or chess strength.

Deterministic identities (SHA-256):

```text
view:     94f309435a71067caf96154aeee7cd8b8996790a4ca3a8811e9966ed1ec00eb3
training: 38b5a58fa4297a238681893e49b667e82da6b9e039d26de25e74b6166cb5d783
holdout:  3a76469ef594e5b857ffaae5a8d978b264ee908de4232259dd88fc41e207cf75
network:  7d6fb5833ef9f9ed8c3e1b4e7b66171b5e9f783dc8d65894508064ea10d6fa69
```

## Automated validation and limitations

All Gradle test commands used `:app:test -Pheadless --no-configuration-cache`.
Production `compileJava` passed. An initial new-test compilation failure from a
loop variable captured by a lambda was corrected; subsequent test compilation
passed.

- Initial selected BRN/reader/new-test batch: 53 tests, 50 passed and three
  existing `BrnHandcraftedGenerationTest` failures. This also passed the NNUE
  blended BRN tests and existing BRN-2 TrainerService tests. Its `*CorpusTest`
  pattern incidentally ran one AlphaBeta and one Quiescence corpus test; both
  passed.
- An intermediate exact focused batch passed 30/30.
- Final broader focused batch passed 42/42: `CorpusTest` (10),
  `Brn2CheckpointTest` (11), `BrnSupervisionPersistenceTest` (5),
  `Brn2TrainingTargetsTest` (3), `BrnCorpusTrainingTest` (7),
  `CorpusPermutationTest` (5), `Brn2ValidationWiringTest` (1).
- After final resume guards and deterministic receipt map ordering, the focused
  adapter/history batch passed 20/20: `BrnCorpusTrainingTest` (7),
  `HistoryRepositoryTest` (10), `HistoryAnalyticsTest` (3).
- Both fresh real-corpus optimization runs and both separate JVM receipt replays
  passed. Cross-output receipt/network equality and source-shard preservation
  checks passed. `git diff --check` passed.

The three legacy failures were reproduced on an untouched `git archive` of the
initial HEAD at `C:\Temp\SeedV6-corpus-baseline-20261002\source`: first one test
failed in isolation, then the other two failed in a second isolated command.
They are inherited, not integration regressions:

1. `nnueGenerationCanUseAnIndependentTeacherStore`: expected IOException was
   not thrown (line 193).
2. `handcraftedBlendUsesSeparateStaticTeacherAndResumePreservesExactOptimizer`:
   expected true was false (line 63).
3. `defaultFreshSourceAndSeedsNeedNoNnueCheckpoint`: expected IOException was
   not thrown (line 203).

No unrelated remediation was attempted. The complete long suite, long campaigns,
NNUE training campaigns, GUI/browser tests, matches/SPRT/strength benchmarks,
full corpus epochs and imports were deliberately excluded by the task's
risk-based scope. No hundred-million-record throughput benchmark was run.

Remaining limits: view admission and startup hashing are linear disk work; the
address index costs 1.6 GB per 100 million eligible records; random I/O at that
scale is unmeasured. At least four eligible identities are needed for loss
validation. Position-level holdout cannot exclude correlation by game because
the corpus does not preserve game grouping. Unknown clocks use the explicit zero
fallback. Interrupted initial view admission fails closed. None requires a new
training-method decision for this completed bounded integration.

## Changed files

- `README.md`, `app/build.gradle`
- Corpus: `CorpusPosition.java`, `CorpusReader.java`, `CorpusView.java`,
  `package-info.java`
- Training checkpoint/history: `BootstrapEvidence.java`, `CheckpointStore.java`,
  `HistoryCodec.java`
- Training service: `TrainerConfig.java`, `TrainerService.java`,
  `TrainingSource.java`, `CorpusTrainingConfig.java`, `CorpusPermutation.java`,
  `BrnCorpusTraining.java`
- Verification runner: `BrnCorpusMain.java`
- Tests: `CorpusPermutationTest.java`, `BrnCorpusTrainingTest.java`
- Documentation: `docs/corpus-training.md`, this report

All paths are within this repository. This report and the implementation are the
later work's coherent task-owned boundary; VERSION_STATE.txt is not part of it.
