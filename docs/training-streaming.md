# Incremental GUI acquisition and training (current implementation)

This correction builds on the inherited, uncommitted bounded-storage work described
in the historical report below. It is a separate work unit on `main` at
`5f992ded72598cf5f178c12596e86d506f2c85c6`; no commit, reset, release finalization or
version mutation was performed. Build 35 and the prior blocked version operation
remain unchanged under the owner's authorized deferred-finalization branch.
The owner also explicitly authorized assigning generated whole-game holdouts
before generation. No journal exists in the repository, and none was created.

## Why disk storage alone did not start training early

Previously `TrainerService` completed generation/acquisition, source-range
verification/reservation, initial-loss measurement and a global Fisher-Yates
permutation before invoking the update loop. Disk storage bounded heap but left
all those barriers before the first update. The displayed zero updates/visits
was therefore accurate, even after all positions had been read.

Inspection found no global target normalization or dataset-dependent objective
in any of the seven trainers. Corpus adapters compute targets per position.
Generated WDL becomes available after each complete game. Blended BRN-2 and its
capture objective use a pinned teacher. Self-play already supports an immutable
actor snapshot. The one global selection dependency was the generated holdout:
it formerly selected 20% of eligible games after all games had finished.

## Current pipeline and ordering

The GUI now opts into `IncrementalTraining` automatically, without new controls.
A single acquisition worker decodes/generates positions, prepares their final
targets and sends 128-record blocks through a two-slot queue. The trainer owns
all mutable optimizer state and writes those samples, in acquisition order, to
the existing 64-byte disk store while consuming them. Queue backpressure limits
acquisition ahead of training. No generation-sized shuffle or initial-loss pass
precedes updates. Initial loss is accumulated against a frozen initial model
while receiving samples; it is finalized once acquisition is verified.

With shuffling enabled, epoch 1 uses a rolling random buffer of
`min(2048, max(128, 4 * minibatch))` samples. For batch 128, warmup is 512 samples;
the first update occurs after consuming 639 acquired samples. The producer may
be several hundred samples ahead in its bounded queue. Without shuffling the
warmup is one sample. A completed game is still required to supply valid WDL.
EOF drains the buffer without duplicates or omissions, then flushes one partial
minibatch if necessary. Each eligible position is visited exactly once.

This limited shuffle mixes nearby games and positions without claiming a uniform
permutation of a dataset that does not yet exist. Default generated samples span
multiple games within the buffer; extremely long games or long source runs
remain less globally mixed in epoch 1. Source allocation, rejection rules,
selected ranges and validation prefixes are unchanged; every selected position
participates. No source is silently resampled or dropped.

Epochs 2 onward replay the retained final-target samples. A seeded, bijective
Feistel permutation chooses 2,048-position blocks; each is read sequentially and
shuffled locally with an epoch-specific seed. No position index or global shuffle
file is constructed. This spreads reads across the dataset while amortizing seeks
over blocks. A fixed stride alone was rejected: it can create cycles when the
stride and count share factors, and retains periodic correlations. Blocks are
read exactly once, including the short last block; minibatches continue across
block boundaries and flush only at the epoch boundary.

The ordering is deterministic for the same acquisition sequence, settings and
seed, regardless of producer scheduling. It intentionally differs from historical
Fisher-Yates ordering, so trained numerical results differ. Time-limited self-play
can also change under CPU contention, as with any change in scheduling.

## Trainer and supervision compatibility

| Architecture | Existing update semantics retained |
| --- | --- |
| NNUE | Configured minibatches and epochs, existing gradient accumulation and Adam |
| NNUE-MATERIAL | Same batched trainer, including calibrated cross-entropy objective |
| BRN | One online pass, batch size 1, existing trainer |
| BRN-1 | One online pass, batch size 1, existing trainer |
| BRN-2 | One online pass, batch size 1, existing blended/capture options |
| BRN-3 | Configured minibatches and epochs, existing corpus objective |
| BRN Pair 2 | Configured minibatches and epochs, existing corpus objective |

No network mathematics, inference formats, optimizer implementation or learning
rate policy changed. The generation actor and teacher remain immutable while the
student updates. Capture relations use the same sorted legal-capture selection,
teacher values and capture RNG in acquisition order, before mixing. No synthetic
capture child becomes an extra training sample. Later epochs read stored final
targets instead of reevaluating a changing model.

For new generated holdout attempts, a seeded permutation assigns a fixed subset
of requested game IDs upfront: `max(2, ceil(games / 5))` games. This is independent
of outcomes. Aborted/ineligible games supply no samples, and each partition must
still have at least two eligible games and two samples. The eligible holdout
fraction can consequently differ from the old post-generation 20% rule. No
fallback redistributes held-out games into training. Insufficient partitions
archive the generation pin and discard provisional updates without publication.

## Verification, stop and recovery

Updates during acquisition are provisional. All existing source identity checks,
range hashes, reservations, receipts, whole-game integrity checks and recovery
data publication must succeed before a Candidate can be published. Source errors,
exhaustion or changed data fail the generation without advancing Latest or Best.
Validation and promotion retain their existing policies and separate datasets.

Stop Now before verification discards provisional optimizer updates and records
a restart from the settled parent. It does not claim exact mid-acquisition
continuation. Source ranges already reserved remain reserved and replayable.
After verification, stop persists the optimizer cursor/state as before. Resume
reconstructs the same ordering, skips completed batches and then continues; it
may reread source data to rebuild the temporary spool. Cancellation during that
reconstruction preserves the previous verified durable continuation.

Stop After Generation lets all configured epochs, validation and settlement
finish, then admits no next dataset. Existing historical attempts with nonzero
optimizer progress retain their old order/reader pipeline for exact compatibility.
Zero-update INDEPENDENT attempts can adopt incremental ordering without abandoning
existing reservations. Frozen research replay and non-GUI callers retain their
existing entry-point behavior.

Generation attempt metadata records the incremental recipe explicitly. Existing
small V1 checkpoint bytes and all network/optimizer codecs remain compatible.
Large sample records no longer hit the former 64/128 MiB limits; payloads above
2 GiB use a V2 frame with a 64-bit length. Older application binaries cannot resume
new incremental attempt metadata or oversized V2 records. This does not change
model-file inference compatibility.

## Memory, disk and responsiveness

Position-dependent heap is bounded by the mixing buffer (at most 2,048), at most
four 128-record acquisition blocks, a 2,048-record replay block, minibatch arrays
and fixed file caches. Network, frozen actor, evaluator and optimizer workspaces
remain architecture dependent. No whole-position collection, target map, capture
relation array, global shuffle array or checkpoint serialization array is used
by the incremental path. Game summaries/IDs and the current game trajectory remain
bounded by existing GUI game/ply/sample settings (maximum 100,000 each); source
catalogs and historical generation ledgers retain their existing per-source and
per-generation metadata, not per-position entries.

Seek points now merge through disk streams instead of a growing TreeMap. During
training, prepared BINP manifests decode into 72-byte disk index records with a
36 KiB cache, preserving manifest identity and JSON compatibility. Explicit
library import/Prepare is a separate pre-existing workflow, not implicitly run
by Network Training. It is not redesigned here. Chunk metadata still requires
verification; legacy sources verify their bounded shard, and BINP decodes its
existing bounded chunk (up to 100 MiB). Those source-dependent costs are additional
to the small streaming queues; the 64 MiB probe is not a JVM recommendation for
all formats or architectures.

Corpus temporary samples need `64 * (training + holdout)` bytes. Generated paths
also retain raw terminal samples and, where applicable, partition spools:
roughly `128 * allSamples + 64 * trainingSamples` temporary bytes, plus durable
56-byte-per-sample recovery records and network checkpoints. Disk requirements
grow with dataset size; no RAM growth or disk-space availability is assumed.
Write failures propagate, block Candidate publication and clean owned temporary
files. Failed checkpoint staging is cleaned while older published continuations
are preserved. Filesystem stalls cannot have a universal shutdown guarantee.

Acquisition, network updates, verification and cleanup run off Swing's EDT.
Progress distinguishes acquired training positions, sample visits, epoch, optimizer
updates, verification/replay, final training-loss measurement and validation.
The first optimizer update and epoch transitions publish immediately; ordinary
updates are throttled to 50 ms, with 250 ms status notices while awaiting the
producer. Generated-game telemetry remains visible during concurrent training.
Overlapped acquisition and training time is charged to the training phase rather
than double-counted as separate sequential durations.

## Files in this corrective work

* `selfplay/IncrementalTraining.java`, `ReplayOrder.java`: bounded acquisition,
  final-target persistence, incremental updates and deterministic later epochs.
* `selfplay/SampleStore.java`, `SelfPlayControl.java`, `SelfPlayBatch.java`,
  `BootstrapPartition.java`: child-worker ownership, sample callbacks, upfront
  whole-game selection and interruptible cleanup.
* `service/TrainerService.java`, `CorpusTraining.java`, `SequentialTraining.java`,
  `TrainerSnapshot.java`: GUI opt-in, verification/publication boundary, source
  streaming, old zero-update reservation compatibility, recovery and progress.
* `validation/HeldOutLoss.java`: expose the existing immutable-model predictor for
  the frozen initial metric; inference behavior is unchanged.
* `data/SourceReaders.java`, `PreparedBinpack.java`, `corpus/LegacyPositionReader.java`:
  bounded source indexes and resource cleanup.
* `checkpoint/GenerationAttempt.java`, `SmallRecord.java`, `BootstrapData.java`,
  `PartialGeneration.java`, `CheckpointStore.java`: recipe identity, scalable
  framing, compatibility and failure cleanup.
* GUI `TrainingController.java`, `TrainingDashboardModel.java`, `TrainingProgress.java`,
  `TrainingProgressView.java`: automatic streaming and accurate existing displays.
* New `IncrementalTrainingTest`, `IncrementalTrainerServiceTest`,
  `IncrementalGeneratedTest`, `IncrementalTrainingProbe`; additions to
  `StreamingRecordTest`, `PartialGenerationStoreTest`, `PreparedBinpackTest` and
  `TrainingRunPresentationTest`: behavioral, lifecycle, format, UI and memory evidence.

The earlier trainer-loop/storage tests and implementation files remain inherited
work. No engine-core, UCI, ordinary Play, search or release-identity source changed.
Final Git state: 31 modified tracked files and 17 untracked files (48 total,
including inherited work), nothing staged, `main` still ahead of `origin/main`
by seven existing commits. There was no new commit.

## Current validation and measurements

Java 21, repository Gradle wrapper, cache `build/migration/gradle-user-home`.
Targeted tests cover a producer that cannot finish until a real optimizer update;
4,101 positions / eight epochs / batch 128; all seven architectures compared with
independent calls to their original update routines; frozen actor/initial loss;
capture supervision; 200,000,003 logical positions with bijective replay; reader
failure/cancellation/thread cleanup; actual source failure; early rollback and
verified resume with byte-identical optimizer continuation; old zero-update
reservations; generated holdout; Stop After Generation; checkpoint framing and
large records; source formats; controller/presentation; and UCI handshake/session.
Across the targeted invocations, 107 distinct tests passed and three optional
real-source fixtures were skipped (no configured local archives). The main check
ran 31 module tests plus 76 integration cases (73 passed, three skipped).
A final affected-check rerun passed 30 tests, including three additional cases:
a record above the old 64 MiB ceiling, failed partial-publication staging cleanup,
and incremental Stop After Generation. Repository-default slow-test exclusions
were retained. The final `git diff --check` passed; no training spool, shuffle,
capture or seek temporary files remained. Pre-existing baseline directories in
the OS temporary directory were preserved.

A synthetic repeated-legal-position fixture through the real JSONL decoder,
BRN Pair 2 optimizer and two epochs used `-Xmx64m`, batch 128 and shuffling.
The first update happened with 1,023 samples produced in both runs, while the
rest of the requested dataset had not yet been acquired.

| Training positions | First update ms | Acquisition ms | Total ms | Acquired records/s | Training visits/s | Sampled heap MiB | Sample disk bytes |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 50,000 | 221.14 | 776.49 | 1,357.54 | 80,490 | 73,663 | 46.89 | 4,000,000 |
| 500,000 | 235.61 | 4,440.75 | 10,195.21 | 140,742 | 98,085 | 47.02 | 40,000,000 |

Acquisition includes the 25% extra holdout and producer backpressure. Total time
includes decoding, frozen initial loss, both epochs and final training metrics;
source fixture creation, initial model construction, checkpoint publication and
holdout validation are excluded.
These are separate cold-JVM single runs, not a controlled speed comparison with
the previous implementation. Heap figures are sampled occupancy, not RSS or a
profiler-derived peak. The constrained maximum heap supplies the hard bound.
Raw output is retained and ignored at `build/training-streaming/incremental-50000.txt`
and `incremental-500000.txt`. Reproduce with the `IncrementalTrainingProbe` main
class after `:engine-training:testClasses`, using compiled training/core classes
and `app/build/install/seedv6/lib/*` on the classpath, Java 21, `-Xmx64m` and
`--add-modules=jdk.incubator.vector`, passing the training count.

No full suite, playing-strength experiment, long self-play/training generation,
manual native GUI interaction or hundreds of millions of physical samples was
run. Large logical permutation coverage and bounded-heap real source/training
runs replace such costly routine checks. Optional real archive tests require
local fixtures and were skipped when their environment variables were absent.
Oversized framing is tested directly; a multi-gigabyte physical checkpoint was
not written. Resource limits and storage failures remain relevant operational
constraints; deterministic ordering does not imply identical numerical results
to the old shuffle. Human actions required after this prompt: None.

---

# Historical report: bounded storage before incremental acquisition

The following report and measurements describe the inherited implementation, not
the corrected first-epoch behavior above. They are retained as provenance.

# Bounded GUI training positions

This work starts from clean `main` at
`5f992ded72598cf5f178c12596e86d506f2c85c6`. Version handling is
`DEFERRED_FINALIZATION`, explicitly authorized by the owner for this task.
The inherited operation `ebe0487f7c214b398fc2625a8ac053fa` and its retained
nested-validation integrity failure are preserved. This work is separate from
that operation; `VERSION_STATE.txt` remains build 35 and no finalized version
identity is claimed. See `unified-architecture/WU7-PACKAGING.md` for the original
operation's evidence.

Discovery: corpus acquisition retained decoded six-long boards, an identity target
map and copied lists for the entire generation and holdout. Trainers allocated a
dataset-sized shuffle array; BRN-2 also froze a dataset-sized target array.
Generated games accumulated samples before training and checkpoint serialization
created additional complete byte arrays. NNUE (both objectives), BRN-3 and
BRE-Pair 2 use minibatches and epochs; BRN/BRN-1/BRN-2 use one online pass.
Exact initial loss and complete generation reservation precede optimizer updates.
Those checks are distinct from loading all positions into heap memory.

## Implementation and bounds

`TrainerService` owns a `SampleStore.Workspace` on its existing training worker.
Within that scope, generated samples, corpus training/holdout samples and frozen
BRN-2 targets go into fixed-width temporary files. Outside the service scope,
existing list-based research callers retain their ownership model. There are no
new GUI settings and no dataset I/O on the Swing event-dispatch thread.

Each temporary position occupies 64 disk bytes (six board longs, terminal WDL,
training target). A store has a 64 KiB sequential cache and a 64 KiB buffered
writer while acquiring. Replay uses fixed offsets, without a position-sized
heap index or memory mapping. Read samples remain immutable; trainer board and
target minibatch arrays are reused as before. Corpus targets travel with the
sample instead of relying on object identity.

For more than 256 disk samples, one reader prepares 256-position blocks. A queue
holds two blocks, with at most one additional producer block and one consumer
block (1,024 decoded positions total). Backpressure bounds acquisition ahead of
training. Small datasets avoid the thread. Failures reach the training worker;
closing interrupts and joins the reader before the shuffle or files are released.
Cancellation still takes effect between optimizer updates: a started minibatch
finishes atomically. Epoch changes close their reader before resetting the order.

Shuffle remains the exact seeded Fisher-Yates algorithm and PRNG draw sequence.
Up to 65,536 positions use an int array capped at 256 KiB; larger permutations
use four disk bytes per position and a fixed 1 MiB page cache. Resume reconstructs
the same epoch orders and skips completed optimizer batches. NNUE, NNUE-MATERIAL,
BRN-3 and BRE-Pair 2 retain configured minibatches, partial final batches and
multiple epochs. BRN, BRN-1 and BRN-2 retain their existing single online pass.
BRN-2 capture supervision freezes teacher evaluations and capture choices in the
original order, with 65-byte disk relation records instead of a relation array.

Checkpoint sample payloads are read as views of verified immutable records and
written/hashed incrementally. V1 framing, hashes and network/optimizer bytes are
unchanged. Stop persistence finishes before the workspace is closed. Successful
generations release their temporary files after validation/settlement; failure
and cancellation close the scope. Only files created by the scope are removed.
Existing checkpoint ownership, retention and deletion decisions remain intact.

Source decoders remain incremental: JSON lines have the existing 1 MiB ceiling;
BINPACK has the existing 100 MiB chunk ceiling (multiple chunk-sized transient
buffers can coexist). One self-play trajectory remains bounded by the configured
game limit. Sparse source seek points (one per 4,096 records and frame boundaries),
source/chunk manifests, game summaries and lifecycle history remain metadata in
memory. Thus decoded position, target, shuffle and checkpoint payload memory is
bounded independently of generation size; total process memory also includes
network/optimizer state, configured minibatches, decoder limits and this existing
metadata. This is not a claim of constant memory for arbitrarily many sources,
chunks, games or generations.

Disk space grows with the selected generation. Training/holdout partitions,
frozen supervision and checkpoint staging can temporarily coexist. An I/O failure
fails the generation through the existing service error path. Abrupt process
termination can leave uniquely named OS temporary files; normal completion,
failure and cancellation clean them. No global cleanup process is introduced.
Existing 64 MiB bootstrap and 128 MiB partial-record safety limits remain.

## Startup and reproducibility

Corpus initial loss is calculated as each accepted position arrives, using the
unchanged initial network and original reduction order. This removes an extra
disk pass and begins useful network work during acquisition. Full source-range
hash verification/reservation and exact initial loss still precede the first
optimizer update; these existing safety/statistics semantics are preserved.
Consequently first-update latency still grows with the generation, and exact
full-dataset shuffling also requires preparation. Later disk reads overlap network
updates. There is no full decoded-dataset heap load and no claim that optimizer
updates begin before the generation has been verified.

No intentional numerical, sampling, training-order or checkpoint-format change.
All seven architectures produced byte-identical final optimizer/model state to
an independent implementation of the previous shuffle/update loop, including
stop, serialized optimizer resume, epochs and partial minibatches. The corpus
initial-loss calculation also matched the previous metric pass bit for bit.
Timing telemetry includes the initial-loss work performed during acquisition;
wall-clock times and interleaving of progress notifications naturally differ.

## Changed files

All Java paths below are under `engine-training/src`.

| Files | Purpose |
| --- | --- |
| `main/.../selfplay/SampleStore.java`, `TrainingOrder.java`, `PrefetchedSamples.java` | Bounded disk positions, deterministic disk permutation and reader backpressure/lifetime. |
| `main/.../selfplay/TrajectorySampler.java`, `SelfPlayBatch.java`, `BootstrapPartition.java` | Carry explicit targets and stream generated samples/whole-game partitions. |
| `main/.../selfplay/SelfPlayTraining.java`, `BrnSelfPlayTraining.java`, `Brn1SelfPlayTraining.java`, `Brn2SelfPlayTraining.java`, `Brn3CorpusOptimization.java`, `BrnPair2CorpusOptimization.java`, `BrnCaptureTraining.java` | Integrate replay without changing architecture-specific optimizer mathematics or batching. |
| `main/.../service/CorpusTraining.java`, `SequentialTraining.java`, `TrainerService.java` | Incremental acquisition/initial loss and workspace lifetime through validation/checkpoint persistence. |
| `main/.../checkpoint/SmallRecord.java`, `BootstrapData.java`, `PartialGeneration.java`, `CheckpointStore.java` | Bounded compatible serialization, verification and resume. |
| `test/.../selfplay/StreamingPositionsTest.java`, `StreamingOptimizerTest.java` | Order/cache boundaries, failure/cancellation/worker cleanup, all-architecture mathematical and resume equivalence. |
| `test/.../checkpoint/StreamingRecordTest.java`, `test/.../service/StreamingInitialLossTest.java` | Exact legacy record bytes, integrity validation, bounded read views, exact initial loss. |
| `test/.../selfplay/StreamingMemoryProbe.java`, `StreamingTrainingBenchmark.java`, `test/.../service/StreamingSourceMemoryProbe.java` | Repeatable standalone memory, acquisition/startup and training-throughput probes. |
| `docs/training-streaming.md` | Design constraints, validation, measurements and separate deferred-version provenance. |

No `engine-core`, `app-uci`, engine search, Play or GUI presentation code changed.

## Validation performed

Java 21 on the current Windows host; Gradle cache at
`build/migration/gradle-user-home`. Commands used the repository wrapper.

* `:engine-training:test --tests '*Streaming*Test'`: 19 passing tests, including
  all seven architectures and a 515-position, three-epoch BRE-Pair 2 case using
  the actual background reader, cancellation after two updates and exact resume.
  Shuffle equivalence covers 65,536/65,537 and 270,001 positions across three
  epochs, exercising disk-cache eviction.
* Targeted `integrationTest` filters: `SequentialTrainingTest`,
  `Brn3CorpusTrainingTest`, `BrnPair2CorpusTrainingTest`, `Bt4TrainingTest`,
  `ResumableTrainingTest`, `BrnCaptureTrainingTest`, `TrainerControlTest`,
  `TrainingControllerTest`, `TrainingRunPresentationTest`, `UciSessionTest`;
  plus `BrnBootstrapTest.stopAfterUpdateRestartUsesDurableDataAndExactOptimizerWithoutRegenerating`,
  `Brn2BlendedBootstrapTest.partialUpdateReloadReusesDataWhileChangedObjectivesRestartFromSettledParent`,
  `CheckpointRetentionTest.pendingValidationPromotionAndUnresolvedCandidatesAreProtected`,
  `UciProcessTest.actualMainProvidesHandshakeReadinessAndCleanQuitWithoutStartupNoise`.
  Across these invocations: 65 distinct integration tests passed and one optional
  local-source test skipped because its environment variable was unset.
* Final rerun of the 19 new tests and three capture tests passed after the cleanup
  review. Gradle's project-boundary verification passed, including the UCI/core-only
  dependency boundary. No streaming position/order/capture temporary files remained
  after completed checks.

Full-suite, long self-play/training, strength testing, extended validation and
manual native Swing interaction were deliberately omitted. Existing controller
and presentation tests exercise the GUI-facing lifecycle; they are not a claim
of manual UI verification. Source integration tests cover the existing formats;
the large low-heap source probe uses synthetic JSONL only.

## Measurements and limitations

Raw local evidence is ignored under `build/training-streaming/` and is not present
in a fresh checkout. The three probe classes above are the reproducible harnesses.
Measurements are single-host observations, not a throughput guarantee. Sampled
heap occupancy is not a profiler-derived peak, RSS or retained-live-set estimate.

| Synthetic fixed-record probe (`-Xmx32m`) | Sampled heap MiB | Acquire ms | First read ms | Read positions/s |
| --- | ---: | ---: | ---: | ---: |
| 50,000, sequential | 16.00 | 79.87 | 98.87 | 311,163 |
| 1,000,000, sequential | 18.43 | 212.61 | 230.53 | 371,968 |
| 1,000,000, shuffled | 19.52 | 208.55 | 4,076.66 | 193,290 |

These runs write every record, then replay every record with an asserted checksum;
the shuffled run includes the full disk permutation. They exclude source decoding
and network state. Evidence: `memory-50000.txt`, `memory-1000000.txt`,
`memory-1000000-shuffle.txt`.

| Actual JSONL acquisition (`-Xmx64m`) | Decoded records including holdout | Sampled heap MiB | First sample ms | Acquisition ms |
| --- | ---: | ---: | ---: | ---: |
| 50,000 training + 12,500 holdout | 62,500 | 39.10 | 126.51 | 689.99 |
| 500,000 training + 125,000 holdout | 625,000 | 39.81 | 121.83 | 2,172.72 |

This probe uses real `SequentialTraining`, JSON/FEN decoding, source range hashes,
seek metadata and disk stores. It generates repeated legal synthetic positions;
timing excludes source-file construction and no optimizer runs. Evidence:
`source-50000.txt`, `source-500000.txt`. These heap limits are not recommended JVM
settings for a network training session: network and optimizer memory is extra.

BRN-3 benchmark: seed 71, batch 128, one shuffled epoch, two 2,048-position warmups,
three alternating trials, `-Xmx512m`, legal starting positions with cycling WDL.
Both variants use identical samples and finish with bit-identical final loss.
The in-memory comparison uses the retained list path in the current implementation,
not a separately built historical binary. Full optimizer-state equivalence to
the previous update loop is established by the tests above.

| Positions | Median memory/disk total ms | Median memory/disk first-update ms | Median memory/disk positions/s |
| --- | ---: | ---: | ---: |
| 4,096 | 145.45 / 164.82 | 35.03 / 47.31 | 28,161 / 24,852 |
| 16,384 | 600.64 / 629.93 | 128.46 / 151.83 | 27,277 / 26,009 |
| 65,537 | 2,439.68 / 2,673.81 | 531.73 / 550.55 | 26,863 / 24,511 |

Evidence: `brn3-4096.txt`, `brn3-16384.txt`, `brn3-65537-quiet.txt`. An earlier
`brn3-65537.txt` run overlapped integration tests and remains preserved but is
excluded from this comparison. The quiet run had no concurrent task tests.
At 65,537, both variants use the bounded shuffle implementation. Disk replay
cost about 5-13% in these short benchmarks; no throughput improvement is claimed.
Real corpora, storage devices, network sizes and JIT/cache conditions can differ.
