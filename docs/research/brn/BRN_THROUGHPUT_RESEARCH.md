# BRN-3 throughput research record

Programme authority: [contract](../../brn/BRN_THROUGHPUT_CONTRACT.md).
Operational selection: [frontier](../../brn/BRN_THROUGHPUT_FRONTIER.md).

Current result: practical bounded optimization complete; final evidence and resume
conditions are in T04 below. Earlier sections preserve the investigation in order,
including provisional findings later confirmed or rejected. This is a Codex work
result, not owner acceptance or release finalization.

## Bootstrap, 2026-10-04

Clean starting HEAD c6c0bda55cfd40227c845ee805a163bfdcbf3d06; no inherited tracked
changes. Existing ignored build/research/dist assets preserved. Version initialization
returned the historical reservation noted in the contract; this later work is
DEFERRED_FINALIZATION and is not attributed to that token. No root journal exists.
No nested/ancestor AGENTS.md was found; supplied user governance applies.

Environment: Windows, OpenJDK21+35-2513, Gradle8.11.1 toolchain21, Ryzen5 5500
(6 physical/12 logical), 34,139,492,352 bytes RAM. NVIDIA-SMI identifies RTX4060,
8188MiB VRAM, driver617.14, driver-supported CUDA13.4 (not proof of installed CUDA
toolkit). Existing desktop applications remain running and will not be stopped.

Observed implementation: Brn3Trainer owns binary64 weights/first/second/gradients,
int touched/stamps, 2x64x8 local state and per-position edge indices. WIDTH8,
POOL_WIDTH96, HIDDEN32, relative lookup static; dense head always touched; sparse
Adam explicitly leaves absent coordinates unchanged. Per-coordinate add checks a
stamp on every contribution. No worker pool. Brn3CorpusOptimization already shuffles
integer indices and loads once per generation, with full prediction passes before
and after epochs. Normal TrainerService opens SequentialTraining; only historical
unfinished permutation campaigns use CorpusTraining/CorpusView. SourceReaders has
64KiB buffered input, framed Zstd and durable sparse navigation. Examples retain
Sample objects/six-long arrays and IdentityHashMap<Sample,Double> targets.

Available real source from existing manifests:
E:/SeedV6-Corpus/incoming/lichess/lichess_db_eval.jsonl.zst,
identity d8e423b9c6ab7ca56b3383d52d09d0a3c36611c6e42f517316a2cbe448c58842.
Existing research dataset app/build/research/brn-learning/data-prospective-v2
contains 262144 training examples plus validation/test; use only training/validation
for throughput work. No new conclusions from old sealed tests. Production loading
measurements must use SequentialTraining and isolated output, not dataset-read
timing masquerading as source loading.

Hypotheses awaiting profile: repeated coordinate stamps and irregular Adam gathers;
JSON/FEN allocation; relation forward/backward locality; dense head SIMD. No timing
or speedup has yet been established. Owner >1.5h production timing is unverified.

## T01: baseline and first retained candidates

Harness: verification `BrnThroughput train|load INPUT NEW_OUTPUT N EPOCHS REPEATS`.
Local outputs: `app/build/research/brn-throughput`; results are JSON and JFR. The
training harness calls production Brn3CorpusOptimization (including full before/
after loss passes); input preparation is separately timed. Loading calls production
SequentialTraining with a new isolated lineage per trial, real source beginning
at ordinal0, and 25% held-out examples. Warmup is discarded. Fixed seed71, shuffle
73103, batch128. Source-selection hashes/metrics are compared, excluding the new
lineage root path. Filesystem reads are cached; no cold-disk claim.

Baseline 16384x2 median3.609700s (3.719744,3.609700,3.497057), clean unprofiled
run `base-train16k-clean`. Initial JFR samples: forward504, gradient-add463,
trainBatch317 (mostly Adam), backward24 of1326 samples. Training steady owner
allocation~214KB, zero timed GC. Thus dense gradient clearing/GC was NOT the cost.
65536 exploratory baseline13.861885/13.130795/31.405043s is marked contaminated:
the last trial overlapped compilation/testing. Repeat before confirmation. Some
row/hoist exploration also overlapped JFR decoding or tests; do not promote their
small timing differences to evidence.

Row stamps track whole width8 blocks, dense head gradients clear once per batch,
and rows update contiguously. This retains zero-valued touched coordinates and
reduces touched/stamp storage from~21MB to~2.6MB. Alone it offers only modest/noisy
speed benefit. Hoisted local row references alone do not establish a material gain.
Five existing BRN3 parity/material/target/resume/calibration tests passed, including
exact independent research weights/moments and serialized continuation.

Isolated Vector API prototype: lane-wise addition/multiplication/division/sqrt,
no horizontal reduction/FMA. 16384x2 median1.639735s (1.643675,1.608441,1.639735):
2.20x baseline. 65536x2 median6.659419s (6.659419,6.936535,6.610638), pending clean
baseline confirmation. ALL final weights, both moment arrays and step hashes match
baseline at both sizes: 16k `6a3f7c53100e9800d71d4ff8460fd667f3f524c34bcba10fea22e535feae3778`,
64k `20109d0092ef031b4caa36ae1e928b1a08b40cfbc6bc983367476f5e13b3aa4d`.
Now integrating optional preferred-species kernels with scalar fallback, retaining
ordinary Java21 toolchain. Confirm integrated speed and profile before next choice.

Loader profile identified repeated Fen.fields regex/splits. CorpusPosition now uses
one complete Fen.parse and builds its planes/rules without a discarded temporary
Board key. Existing individual Fen accessors retain validation boundaries. At65536
training+16384heldout: baseline median1.140395s vs candidate0.810148s (1.41x);
owner allocation2,111,827,032 ->1,399,541,472 bytes. Training/heldout hashes, source
identity, decoded/skipped counts and targets all match (only isolated root differs).
Targeted Fen/Board/corpus/import/source/BRN3/NNUE corpus tests passed in105seconds;
full/slow suite was not run. Further allocation reduction remains a hypothesis.

## Material external research

- [Java21 DoubleVector](https://docs.oracle.com/en/java/javase/21/docs/api/jdk.incubator.vector/jdk/incubator/vector/DoubleVector.html)
  provides lane-wise binary64 arithmetic, loads/stores and preferred species.
  Incubator module requires compile/runtime resolution; tested here without preview
  flags because the array operations do not use preview MemorySegment APIs. Keep
  scalar fallback and do not reassociate dense dot products or introduce FMA.
- [Adam paper](https://arxiv.org/abs/1412.6980) and
  [SparseAdam documentation](https://docs.pytorch.org/docs/stable/generated/torch.optim.SparseAdam.html)
  distinguish moment evolution from a structural sparse mask. Seed's existing
  touched-zero semantics govern, not a generic zero-value mask. No lazy/skipped
  moment shortcut was adopted.
- [JCuda](https://github.com/jcuda/jcuda-main) and
  [JOCL](https://github.com/gpu/JOCL) offer Java bindings to native CUDA/OpenCL.
  This host has an NVIDIA driver and OpenCL DLL but no nvcc/toolkit command found.
  An isolated JOCL2.0.5 artifact from Maven Central (SHA256
  88e425bda01a3edb15777f7831eaa364bde3d19c6a833b51a018a5ffac3ad667)
  is downloaded only under ignored experiment output; no production dependency.
- [TornadoVM current implementation](https://github.com/beehive-lab/TornadoVM)
  supports Java21+ with GPU compilation; older PTX/SPIR-V installation advice is
  stale relative to its [6.0 runtime changes](https://www.tornadovm.org/blogs/tornadovm-6-0-0-zero-jni-faster-runtime).
  Treat a new compiler/runtime dependency as real integration cost, not a free JVM flag.
- [NVIDIA best practices](https://docs.nvidia.com/cuda/cuda-c-best-practices-guide/index.html)
  emphasizes device residency and minimized transfers. Implication for BRN3:
  optimizer-only offload must count host gradients/weights/moments traffic; complete
  offload needs forward/backward, collision-safe sparse reduction and persistent
  device state. A dense matrix benchmark cannot establish BRN3 training speed.

GPU and parallelism remain open; no GPU performance claim or disposition yet.

## T02: confirmation and second bottleneck selection

Synchronous runner `tools/brn-throughput.py` now applies a300-second subprocess
ceiling, preserves stdout and reports medians; its `profile` mode reads JFR without
the very slow PowerShell text redirection observed in exploratory analysis. Do not
run any build, profiler conversion or benchmark alongside a timed comparison.
`baseline REV NEW_DIR` reconstructs only measured original classes from Git and
compiles an isolated classpath overlay. It never changes the worktree or stores.

Clean sequential64k confirmation: original11.680410s (11.778201,11.528577,11.680410),
integrated vectors6.677823s (6.677823,6.793380,6.587336), exact state hashes. This
is1.75x on this host; initial16k2.20x is not a universal ratio. New16k ordinary
SuperWord median3.029827s versus disabled3.003338s: no material end-to-end automatic
vectorization gain. This flag control is not assembly proof that no loop vectorizes.
Explicit vectors materially outperform both. Training profile after vectors still
shows irregular relation/row work, Adam and the scalar dense head as principal costs.

Dense transpose candidate:48KiB cached per trainer, refreshed lazily once after
each update; SIMD runs across hidden units while each dot product retains its
original input order. At16k prototype1.430226s; integrated1.498473s. At64k integrated
median6.093063s (5.878062,6.093063,6.553568), exact original final-state hash.
Retained provisionally pending final wider confirmation. No new corpus relations
are precomputed, and checkpoint layout remains unchanged.

Partial minibatch parallel prototype:2/4/6 workers use private gradient/stamp arrays,
shared read-only minibatch weights and ordered worker reduction.16k medians
1.424246/1.412899/1.425258s versus pre-transpose SIMD~1.64s. Each worker count is
repeatable, but hashes differ across counts and from scalar because gradient
reduction is Class B. Additional workers do not scale on this memory-heavy graph;
each costs~24MiB. The exact dense transpose obtains a comparable benefit without
worker scheduling, reduction or resume-identity complexity. Parallel default not
retained; confirm its remaining opportunity against final implementation before
closure, not against the obsolete scalar bottleneck.

Loader re-profile64k (115samples): JSON string/tree work dominates, with remaining
regex/FEN costs. Cached immutable whitespace Patterns remove repeated compilation.
A selective JSON-tree candidate preserved chosen fields/PVs and reduced allocation,
but a strict test caught Gson2.11 skipValue accepting a raw newline inside an
unused string. A token-validating iterative discard fixed that failure; no weakened
decoder was retained. Corrected128k selective median1.513524s vs ordinary DOM with
cached FEN1.542184s (only1.9%, within variability), allocation2.269GB vs2.524GB.
Decision: REJECT selective decoder; restore original production decoder entirely.
Keep malformed/duplicate-key/first-PV tests as evidence of its required boundary.
Next cheap loader experiment: scan existing input-buffer line slices directly,
allocating an assembly buffer only for boundary-spanning lines.

## T03: actual GPU experiment and disposition

`tools/BrnGpuProbe.java` is a separately compiled JOCL experiment, no application
dependency. It takes the actual157057touched coordinates from minibatch16 of128
real examples, with2,627,777 total parameters. RTX4060 reports OpenCL3.0 CUDA and
cl_khr_fp64. OpenCL kernel is original, not third-party copied code; FP contraction
is disabled. One-update weight error against scalar CPU was exactly0.

First probe: warmed resident GPU median~.140ms/update vs scalarCPU~1.2ms; with
gradient/index upload and updated-weight download~.921ms/update. Follow-up includes
the production SIMD kernel: transfer-path GPU.909/.919/1.005ms, CPU SIMD
.803/.847/1.015ms. Host packing cost is omitted, making this a favorable GPU bound.
64updates per trial, one discarded warmup, three trials; fixed observed touched set
is replayed, not a claimed full training trajectory. Kernel compilation was.378s
first and.026s cached. Raw reports in gpu/optimizer*.json disclose all observations.

Reject optimizer-only offload: no worthwhile gain over the retained SIMD kernel
once necessary transfers are counted. Full device-resident training is technically
plausible: parameter/gradient/moment storage~84MiB fits8GiB, while32-node width8
gathers and shared relative/dense gradient collisions require custom kernels and
reduction. A128-example full-gradient replica would cost~2.5GiB plus clearing/
reduction traffic, so it is not a free dense-GEMM substitution. Forward/backward,
masked touched-zero semantics, deterministic/numerically validated reduction,
checkpoint synchronization, cancellation, driver packaging and CPU fallback would
all need a maintained new native backend. Existing Java21 CPU evidence does not
justify that integration burden. Full offload is explicitly deferred, not measured
or declared impossible; mixed precision/minibatch changes are not used to make it
look faster. JCuda/TornadoVM remain alternative integrations, not installed runtimes.

Build integration keeps Java21 and supplies --add-modules=jdk.incubator.vector;
direct launches without it and -Dseedv6.brn3.scalar=true retain scalar training.
Native image module selection must include existing defaults. Inspection of this
JDK21+35's JLinkBundlerHelper found ALL-DEFAULT must be LAST in its linked set;
both packaging paths use jdk.incubator.vector,ALL-DEFAULT. Native runtime QA remains
pending. No release with finalized provenance is claimed.

## T04: final integration, evidence and stopping decision

Retained Class A implementation: width-eight sparse row stamps and contiguous
updates; lane-wise double-precision Vector API relation/Adam kernels; a 48 KiB
cached dense transpose preserving every dot product's original addition order;
parse-once FEN and direct corpus-plane construction; cached whitespace patterns;
buffer-slice JSONL decoding with assembly only across input-buffer boundaries.
The original strict JSON decoder is unchanged. No optimizer, sampling, target,
loss, minibatch, epoch, precision, checkpoint or play-calibration default changed.
NNUE uses its existing training implementation and the verified shared loader.

Confirmed results on the bootstrap host follow. All rows except the last are
three measured trials after one discarded warmup, with median wall times shown.
Training includes the production before/after full loss passes. Pipeline also
includes source loading/preparation and trainer initialization; it excludes
checkpoint publication, campaign setup, candidate evaluation and GUI interaction.
Loader counts include a further 25% held-out selection.

| Workload | Original seconds | Retained seconds | Ratio |
|---|---:|---:|---:|
| Source load: 131,072 train + 32,768 held-out | 2.2311 | 1.3666 | 1.63x |
| Compute: 65,536 positions x 2 epochs | 11.6804 | 6.0931 | 1.92x |
| Pipeline: 16,384 positions x 2 epochs | 3.2345 | 1.8255 | 1.77x |
| Pipeline: 65,536 positions x 2 epochs | 12.9645 | 6.9636 | 1.86x |
| Compute: 65,536 x 8, one control each, candidate profiled | 43.0781 | 23.1419 | 1.86x, indicative |

The last row is an eight-epoch equivalence/profile control, **not** an independently
repeated timing claim. It makes 4,096 updates / 524,288 example exposures. Exact
weights, first/second moments and step match the original, SHA256
`523d605e39143b1dcbec2681b98ca9c17ae8e2280d9f3aa3b1b37c1528391f64`.
Initial/final/mean loss and prediction/target statistics match exactly at both
two-epoch sizes and eight epochs. For example the eight-epoch training loss goes
from 0.2007726494445824 to 0.025840896934508376 in both implementations. Pipeline
selection identity, examples, held-out examples, skipped/decoded counts and final
training statistics also match exactly; comparisons exclude only isolated paths.
This direct Class A evidence protects efficacy without expensive strength games.

At 65,536 pipeline positions the component medians are load 1.1862 -> 0.7200s
and initialized training 11.7443 -> 6.1766s. Component medians need not sum to the
median total. Owner-thread allocation is 2.230 -> 1.254 GB; timed GC 12 -> 7ms.
The larger loader allocates 4.173 -> 2.271 GB, with GC 42 -> 25ms. Compute-only
steady allocation is about 803 KB over two epochs, zero measured GC, and heap
before timing falls from 153.7 to 135.2 MB. Sparse stamp/touched arrays save about
18.4 MB; the transpose adds 49,152 bytes. These are observed heap/allocated-byte
metrics, not peak RSS or proof that an 8M corpus fits a particular heap. The
profiled eight-epoch allocation includes JFR activity and is not comparable to
unprofiled allocation. Raw timings, heap and startup fields remain in the reports.

Final JFR has 8,097 execution samples: approximately 34% in Adam, 19% in sparse
relation traversal, 33% in backward accumulation and 6% in the vector dense head.
These sampled top-frame counts are approximate attribution, not instrumented phase
timers. Sparse rows and masked Adam dominate after retained SIMD; GC does not.
Precomputing every relation would add roughly 4 KiB per full-board example
(about 32 GB for 8M before objects/other state), competing with useful model/corpus
cache. Relative-row indices, epoch permutations and load-once semantics already
exist. A blanket primitive-corpus rewrite or memory mapping is not supported by a
measured retained-compute allocation/I/O bottleneck. Parallel decoding adds source
ordering/resume/validation complexity to a loader now around 10% of this pipeline.
Neither is selected merely because it could be implemented.

Sorting touched rows before Adam: median 1.5620s vs 1.4985s at 16k, exact state,
rejected as slower. Rechecking the parallel prototype with the dense transpose
gives 1.3499s with two workers vs 1.4985s single owner (about 10%); four/six workers
previously plateaued. Reject the extra private-state memory, scheduling and changed
reduction/resume identity for this modest remaining opportunity. This does not
claim parallelism can never help another CPU or larger topology. GPU disposition
is T03: measured transfer costs reject optimizer-only offload; a complete resident
backend remains technically plausible but disproportionate architectural work.

One last profile-driven candidate vectorized dense backward gradient/pool updates
without reassociation. At 64k, candidate median 5.9491s (6.1550,5.8901,5.9491),
subsequent control 6.1771s (6.1771,6.6323,5.9612), versus the earlier retained
6.0931s. All state/statistics matched. The approximately 2-4% median difference is
within run variability; reject as no established material gain. The retained
production code and published final profile are unchanged by this experiment.

Final validation: 33 targeted tests passed, zero skipped/failures/errors, including
the explicitly enabled real-archive smoke. Coverage includes the independent BRN-3
research recipe, full moments and serialized resume, ordinary BRN-3 and NNUE corpus
service/resume, FEN rule/counter parity, strict JSON selection/import, source seek
and line-boundary/1 MiB limits, exact vector tails/nondefault Adam configuration,
and installed Windows UCI protocol/stderr. An earlier 31-test aggregate included
one skipped environment-gated test; the final aggregate supersedes that count.
One final invocation initially placed Gradle's --tests after installDist and was
rejected before running tests; corrected task ordering produced the passing run.

No-module scalar, preferred SIMD and forced 128-bit SIMD smoke executions share
state SHA256 `b63005bdac6639983b36aece7f7beca659be38a1e9e8f5b9d0b601025b2e5b53`.
A scratch Windows jpackage app image ran the same smoke with vectors enabled and
java.desktop/java.sql/java.logging present. It is local runtime QA, not a released
package. A global launcher module flag initially violated UCI's empty-stderr test
because Java warns about incubator modules. Generated distribution launchers now
enable the module only for `gui`; ordinary UCI remains clean. Gradle developer/test
tasks and native GUI packages enable it. Native macOS was not executable on this
Windows host; its matching packaging arguments were statically reviewed only.
Preferred species permits narrower/wider architectures; actual ARM performance and
native macOS packaging remain unverified. Java 21 is unchanged, no preview flag or
third-party runtime is added to the application. Direct scalar execution is viable.

Evidence files under [throughput-2026-10-04](evidence/throughput-2026-10-04/):
`benchmarks.json` preserves selected original reports, including warmups and the
explicitly invalidated early overlapping run; `comparisons.json` contains asserted
exact equivalence and paired summaries; `profiles.json` contains compact sampled
frames; `gpu.json`, `tests.json` and `runtime.json` preserve the corresponding
checks; `dataset-manifest.json` pins the existing component-benchmark dataset.
Full JFR, local prototypes, QA image and raw source remain ignored local
research artifacts. Do not treat ignored artifacts as the sole continuity source.

Limits: filesystem-cache-warm, sequential source-prefix loading on this machine;
no cold-disk, 8M heap/GC, other-CPU or full-production-time claim. Existing desktop
applications were left running; small differences are interpreted conservatively.
No 8M x 8 run, self-play, strength tournament, full/slow test suite, browser/GUI
QA or native macOS run was performed. Browser interaction was not required for
these headless kernels/readers. Production improvement is likely given exact state
and the scaling ladder, but its magnitude remains an extrapolation. No numerical
forecast is substituted for the owner's original >1.5-hour report.

Stopping rationale: measured practical improvements are retained and verified;
loading, relation traversal, backward work, optimizer, SIMD, workers and GPU have
explicit dispositions. Remaining larger opportunities require a new backend/data
representation or changed learning methodology without a compelling measured
benefit here. No unresolved correctness failure blocks this work unit. Contract
conditions 1/2 are satisfied; owner acceptance and deployment remain separate.
Future work should begin only from new hardware/profile evidence or an authorized
backend/methodology decision, not by rerunning the closed research checklist.

Version provenance remains DEFERRED_FINALIZATION under the unrelated historical
reservation. Root VERSION_STATE.txt remains build33; no bump or recovery is claimed.
The attributable implementation/evidence is committed separately for future
endpoint accounting. Root CODEXLOG_CURRENT.md remains absent. No push/deployment.
Human actions required to complete this bounded work: None. Optional owner-scale
acceptance would establish actual 8M loading/heap/GC behavior and complete campaign
time. Resolution of the prior version reservation remains a separate gate for a
future release that requires finalized version provenance.

## Reproduction and fresh-session operations

Read the contract and current frontier first. Use Java 21 (java, javac and jfr on
PATH), Python 3 and the repository Gradle wrapper. Never benchmark alongside a
build, test suite, another experiment or JFR conversion. Each output path must be
new; the runner refuses overwrite and terminates a Java experiment at 300 seconds.
The original reference is commit c6c0bda55cfd40227c845ee805a163bfdcbf3d06; the runner
builds an isolated overlay from Git without checking out or modifying that commit.

```powershell
.\gradlew.bat :app:compileVerificationJava :app:installDist
python tools/brn-throughput.py baseline c6c0bda55cfd40227c845ee805a163bfdcbf3d06 app/build/research/brn-throughput/recheck-base
$source = 'E:/SeedV6-Corpus/incoming/lichess/lichess_db_eval.jsonl.zst'
python tools/brn-throughput.py pipeline $source app/build/research/brn-throughput/recheck-original --positions 65536 --overlay app/build/research/brn-throughput/recheck-base/classes
python tools/brn-throughput.py pipeline $source app/build/research/brn-throughput/recheck-current --positions 65536
```

Use the same immutable source/seed for both runs; report source identity changes.
Repeat at 16384 for the smaller pipeline point or use `load --positions 131072` for
loader-only confirmation. Actual syntax keeps MODE INPUT NEW_OUTPUT before flags.
`train` takes an existing BrnResearchData directory, not a raw source: recorded
component timings use data-prospective-v2, its first N training records, originally
drawn from raw source range 1,578,888..1,953,686. If that ignored dataset is absent,
the source-based pipeline above remains the self-contained reproduction path.
No sealed test split is needed. Reports distinguish initialization/prepare and
discarded warmup from measured trials; do not include state hashing in timed work.

Add `--record` to one bounded run, then use
`python tools/brn-throughput.py profile PATH/training.jfr NEW_SUMMARY.json` (loader
and pipeline recordings are named loading.jfr). `--scalar` selects the retained
scalar trainer; `--no-superword` provides the ordinary-JIT control. `--workers`
exists only for the isolated rejected prototype and does not enable production
multithreading. For application diagnosis use `-Dseedv6.brn3.scalar=true` or launch
directly without resolving jdk.incubator.vector. Generated GUI/native launchers
select SIMD automatically; no operator training-setting change is needed.

Final targeted validation command (put --tests immediately with the test task):

```powershell
$env:SEED_TRAINING_SMOKE_SOURCE = $source
.\gradlew.bat :app:compileVerificationJava :app:installDist :app:test -Pheadless --tests '*Brn3VectorKernelsTest' --tests '*Brn3ResearchParityTest' --tests '*Brn3CorpusTrainingTest' --tests '*NnueCorpusTrainingTest' --tests '*FenTest' --tests '*CorpusFenParityTest' --tests '*LichessDecoderSelectionTest' --tests '*LichessImporterTest' --tests '*SourceReadersTest' --tests '*UciWindowsLauncherTest'
```

The optional GPU probe is intentionally outside source sets. Obtain JOCL 2.0.5
from Maven Central and verify the T01 digest, compile `tools/BrnGpuProbe.java` with
that JAR plus main/verification/installDist dependencies, then run its full class
`com.ohinteractive.seedv6.core.brn3.BrnGpuProbe DATA NEW_REPORT` with the vector
module enabled. It deliberately requires the inspected RTX4060; adapt device
selection explicitly for a new hardware investigation. Keep the dependency and
class output under ignored research output. Its replayed optimizer microprobe
does not replace a full GPU training/correctness study.
