# Windows production promotion — 128-unit direct-output NNUE

15 November 2026 (Pacific/Auckland). **Implementation complete; Windows technical
qualification passed for the owner's revised scope.** The owner explicitly
requested that native Windows GUI checks be skipped during this assignment.
Automated Swing startup/search tests had already passed; no interactive packaged
GUI launch or visual acceptance is claimed. Mac installation/validation and BotLi
were not performed. Owner review of this delivery remains separate.

## Adopted evaluator and identity

The production evaluator is a **128-unit direct-output NNUE trained with a 25%
game-outcome / 75% teacher-target mixture, selected at checkpoint 64** (direct128).
Both perspectives retain 128 accumulator units, SCReLU activation, 7,680
piece-square inputs and eight material-selected output heads. There is no
additional nonlinear residual layer or pawn-pair input extension. CLV_C1,
flags3329490943, worker scheduling, clock policy and score interpretation remain
unchanged: QA255/QB64, scale400, integer truncation, side-to-move reporting and
the existing non-mate clamp.

| Identity | SHA-256 |
|---|---|
| Accepted research artifact, 1,970,524 bytes | `defaf21da465d6f63fc13a2299299e927029d82558d663c7bbc801e7c35e9d1c` |
| Production container, 1,970,520 bytes | `ef6ff57d3a373f7c8aa96c9c06508df0470a139bb10f283fbe0241f2f1bf3e67` |
| Identical parameter payload in both containers | `969dd242ad58c5927d53d87b6c26d0f898a6e9cccc93b5ae0331cc7cabb239be` |

The exact ignored source artifact was present and verified at
`docs/research/nnue-phase2-cglhw/raw/compact128-base-64-assessment-r1/network.p2nnue`.
The converter `tools/promote_direct128.py` accepts only that exact artifact.
It replaces the experimental 76-byte S6P2N002 header with a 72-byte S6CNU003
production header; **every parameter byte is unchanged**. Tests reconstruct the
original envelope and reproduce its exact accepted SHA-256. No retraining,
quantization, checkpoint substitution or research-result rewriting occurred.

S6CNU003 fixes the 128-unit geometry, ten input buckets, eight output heads,
scaling and payload length. The loader checks identity, geometry, checksum,
truncation, trailing bytes and output weights bounded to +/-127. Experimental
S6P2N002 remains rejected. Existing S6CNU001/002 geometries and signed-short
arithmetic remain supported. The SIMD direct head adapts the tested Phase 2
kernel, including its cached shuffle for allocation-free JDK21 widening. The
existing production accumulator and lifecycle are reused with model-sized arrays.

## Production wiring and rollback

- `ConventionalNetwork`, `ConventionalAccumulator` and `Direct128VectorHead`
  implement the strict format and inference. `ConventionalNnueEngine` pins each
  bundled model's file hash, payload hash, size and geometry.
- `BaselineEngine.load()` selects the promoted evaluator; its payload receives
  the existing adopted CLV_C1 configuration. The model is a resource in
  `engine-core.jar`, added by `engine-core/build.gradle` from
  `build-assets/production/conventional-direct128.cnnue`. The adjacent JSON
  manifest records exact provenance and conversion.
- Normal UCI uses that shared factory. Explicit selections are
  `--network direct128`, `--network wdl25` and `--network cp64`.
- GUI Play and Arena use the same baseline binding through the existing
  selectors. Fresh selections default to the promoted evaluator. Existing saved
  explicit selections remain supported; no new model-selection interface was
  introduced. Arena persistence distinguishes all three model identities.
- On this Windows profile, the saved Human-vs-Engine and Black evaluator choices
  were changed from CP64 to the promoted evaluator and read back successfully.
  The saved White BREP2-32 choice was preserved. The prior Play preference subtree
  is backed up at `raw/play-preferences-before.xml`; before/after evaluator values
  are in `raw/evaluator-preferences-*.properties`. This is a configuration change,
  not evidence of a native GUI launch.
- The previous **256-unit direct-output NNUE trained with the 25% outcome / 75%
  teacher mixture** (WDL25) and historical **256-unit teacher-target NNUE** (CP64)
  remain bundled, selectable and byte-identical. Their hashes remain
  `a25e76fcdfe1b854452685bec87bc759bed2ed166312579e024410f0903517ab` and
  `8431626c2fca81e0a1e9ccd5e45ddd8829e6e3282c756c21fad75b06d075bb2b` respectively.
  The historical no-argument CP64 loader and E008 launcher retain their identities.
  A damaged default fails closed; rollback is explicit, never a silent substitution.

The production feature inventory was updated in place. Root README.md, AGENTS.md,
behavioral governance, accepted Phase 2 reports/state/evidence, search algorithms
and both rollback weight files were left unchanged.

## Validation evidence

Passing results, with raw local evidence indexed by [qualification.json](qualification.json):

| Check | Observed result |
|---|---|
| Targeted Java checks with Vector API (precommit build101) | 26 engine-core, 6 research equivalence, 29 UCI and 11 GUI tests passed; zero skips |
| Module-free fallback checks | 20 engine-core and 29 UCI tests passed; zero skips |
| Independent integer reference | All 259 retained Python reference positions match production scalar, vector and full rebuild |
| Accumulator/lifecycle correctness | Raw sums, full refresh, legal walks, siblings/undo, captures, castling, en passant, underpromotions, king-bucket/mirroring changes, null moves, worker ownership and qsearch transfers passed |
| Same-model search equivalence | All 48 warmed fixed-root requests match research inference exactly in nodes, scores, depths, completion and resolved configuration; additional unit tests match best move/PV |
| Actual multiworker evaluation checks | 268,132 evaluations at W2/W4/W16 match full rebuild, including 165,424 helper-thread evaluations; node limits, legal PVs, restart and drain passed |
| Packaging verifier tests | 41 passed; one symlink-creation test intentionally skipped on Windows because it requires special privileges |
| Final Windows UCI image and extracted ZIP | 66 searches each; startup, ready, depth/time/clocks, stop, new game, hash changes, position setup, book controls, EOF/termination and shutdown passed at W1/W4/W16 |
| Final rollback/asset checks | All three bundled selections load/search; missing, truncated, corrupt and incompatible default assets fail closed; CP64 launcher passes nine scalar/vector/module-absent searches per image/ZIP |
| Final GUI package | Headless resources/JNI and extracted ZIP checks pass; shared core bytes match UCI; interactive native GUI check skipped at owner's request |

The existing `packageWindows` task verified image contents, launcher dependencies,
embedded identities, ZIP extraction and execution with empty PATH / external Java
variables removed. A preliminary build101 candidate also passed before the single
version increment. Initial Java failures were stale old-default test expectations,
corrected before the successful runs. The first final-build packaging attempt
correctly rejected a source-state change when a task-generated Python cache file
was removed after preflight; the clean retry passed. Failed logs remain preserved.

No fresh strength tournament was run: acceptance of Phase 2 was supplied by the
owner and equivalence checks found no discrepancy requiring a strength retest.
Unrelated full/long-running suites, Mac, BotLi and native GUI interaction were
excluded. The prior strength evidence's limited clock/worker/generalization
claims are unchanged; this qualification does not establish new playing strength.

## Performance

The retained Phase 2 legal-walk harness was adapted to the production model;
no alternate inference implementation was introduced for timing. Each kernel
measurement uses 255 deterministic transitions, five warmup samples and five
measured samples, then 100,000 update-plus-evaluation operations for allocation.

| Runtime | Research 128-unit model | Production 128-unit model | 256-unit mixture-trained rollback |
|---|---:|---:|---:|
| Oracle Java 21 GA | 524 ns | 464 ns | 752 ns |
| Temurin 21.0.12.1+1, Windows packaging JDK | 409 ns | 402 ns | 774 ns |

Values are median update-plus-evaluation times. **All six measurements report
zero allocated bytes over 100,000 operations.** The earlier Phase 2 result was
approximately 386 ns; the concurrent reference on the packaging JDK measured
409 ns, so these observations show no material unexpected integration regression.
Runtime/JIT and host variation prevent treating cross-run differences as precise
kernel improvements.

Two warmed search repeats, reversing model order, measured mean NPS of 885,276
for production, 857,949 for the same research model, and 615,869 for the 256-unit
mixture-trained rollback on Temurin21. The original Phase 2 means were 923,086
and 645,837 for the selected model and rollback. Equal-payload trees match exactly;
different learned evaluations change trees. **These NPS ratios are not universal
kernel speedups or new Elo estimates.** Hardware cache/branch counters were not
collected. Commands and complete samples remain in `raw/measurements-r1/`.

## Build and Windows deliverables

Starting tree: clean `main`, `600ae4cd`, build101; the reported integrated commit
`695c6e33` is its ancestor. Implementation commit:
`21757aaacdb70e761a05cf98f9da3e7003f58b37`. Version is **0.0.0/build102**: only the
build integer changed, exactly once, in the implementation commit. No root
CODEXLOG_CURRENT.md existed, so none was created. This report is a subsequent
documentation-only work record and does not warrant another build increment.

Final packages were built from that clean implementation commit using
`gradlew.bat packageWindows -PwindowsPackagingJdk=C:/java/jdk-21-packaging`.
Both embed `build=102`, `sourceRevision=21757aaacdb7` and core JAR SHA-256
`de5bb1d0f24d56aa91b9a37ed853b65157ac46e34069e5e39f25f86fa71fa14d`.

| Local deliverable | SHA-256 |
|---|---|
| `dist/windows/current/SeedV6-GUI-windows-x64.zip` | `19932678f0f1795c38b26e86267112679580c83f1e0d5a5d80a7c37792b1201d` |
| `releases/current/windows-x64/SeedV6-UCI-windows-x64.zip` | `2658964eb5af5bea6f2bd6c13973dfb620ebac2c2ac8f1c39178b005fdb15585` |

Launchers are `dist/windows/current/SeedV6-GUI/SeedV6-GUI.exe` and
`releases/current/windows-x64/SeedV6-UCI/SeedV6-UCI.exe`. Full build manifests and
candidate verification receipts accompany them; durable receipt copies are under
this work unit's ignored `raw/`. Local artifacts are qualified distributions,
not a published release. No push, Mac installation, Mac qualification or BotLi
replacement occurred. Work stops at this Windows delivery for owner review.
