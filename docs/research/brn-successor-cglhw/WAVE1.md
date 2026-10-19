# Wave 1 implementation and training handoff

20 October 2026. Status: implemented wave, awaiting substantive human training.
Governance: [CONTRACT](CONTRACT.md). Continuation: [STATE](STATE.md).
Machine-specific identities and initial payload hashes: [lineage manifest](wave1-lineages.json).
This is a training-readiness result, not programme acceptance or a strength result.

## Ready candidates

All three managed lineages were created automatically in `E:\SeedV6-Networks`.
They are separate independent replacements, initialized with seed 1, zero residual
heads and fixed material; no Gen32 weights or predictions enter their inference.

| Candidate / GUI lineage prefix | Mechanism | Parameters | Runtime payload | Managed folder / lineage UUID |
| --- | --- | ---: | ---: | --- |
| W1-GATE | Four softmax context gates over shared local pair-pattern experts, plus unary terms | 6,549 | 52,432 bytes | `BRN-GATED/699672ff-5dcc-326a-afef-924996c76f79` |
| W1-CUBIC | Rank16 degree2 and degree3 ANOVA factors over distinct active piece-square features, plus unary terms | 13,089 | 104,752 bytes | `BRN-CUBIC/e04052b9-6784-3c12-abb5-abc75cd21f59` |
| W1-POOL | Rank16 pooled tanh features, learned output head and unary terms; conventional nonlinear control | 13,089 | 104,752 bytes | `BRN-POOLED/97b2fdda-f812-3455-9f62-43e5ee322651` |

The versioned binary64 codecs record architecture, layout, width, feature/numerical
version and calibration identity. Runtime payloads exclude Adam state. Separate
optimizer snapshots preserve weights, both moments, rate and step exactly; ordinary
checkpoint loading verifies hashes and matches the model against the optimizer.
No runtime package depends on training or GUI code.

Common inputs are 768 piece-square indicators with mover-canonical rank/color
transformation. Empty-square ownership bits and rule counters are not features.
GATE uses 210 local horizontal/vertical/diagonal pairs, including empty squares;
its tables share location, and remote embeddings condition the four gates.
CUBIC computes elementary symmetric sums without enumerating triples. POOL is
deliberately a control; its conventional nonlinearity is not an architectural novelty.
All use fixed material P=1, N=3.2, B=3.3, R=5, Q=9. The initial search mapping is
material plus one-quarter residual, scaled by 100 and capped at +/-32511.
This gain is provisional, not empirically calibrated centipawn equivalence.

## Training instructions

Use the application built from this commit. The local runnable distribution is
`app/build/install/seedv6`; from the repository in PowerShell:

```powershell
.\app\build\install\seedv6\bin\seedv6.bat gui
```

An older installed executable will not know the new architecture identities.
In **Network Training**, select each W1 lineage from **Training Lineage**; no manual
creation or import is needed. The existing base storage setting remains
`E:\SeedV6-Networks`. Check the lineage prefix before starting.

Each lineage is preconfigured with:

- Adam learning rate 0.003, minibatch 128, 8 epochs, shuffle/run seed 1.
- 131,072 retained training positions per generation, 32,768 held-out positions.
- Registered `lichess_db_eval.jsonl.zst`, `STOCKFISH_CP_MATE_V1`, weight 1;
  exact path and source/selection identities are in the manifest.
- Held-out validation; no self-play data generation or validation match campaign.
- Unlimited session duration/generations, controlled by Stop or Stop after Generation.
  Run one candidate at a time as a conservative initial consumer-PC workflow.

Start with these matched recipes. Confirm finite loss and progressing optimizer
steps, successful generation/checkpoint publication, and working Stop/Resume.
Watch held-out loss over meaningful exposure and inspect ordinary Play behavior;
do not use a generation count alone as maturity evidence. Running optimization loss
is cross-entropy; final training/held-out loss uses half squared outcome error, so
their magnitudes are not directly comparable. Best denotes the training validation
decision, not proven superiority against Gen32. Latest can differ from Best.

Training owns CPU-intensive learning; Codex has not started it. Source extraction
is sequential and resumable, with no automatic wrapping after exhaustion. The
new split rejects records from other partitions while filling quotas, so consumed
source records exceed retained positions. The shared services can spool data and
restore exact optimizer boundaries after interruption.

When a useful subset or all three have learned meaningfully and play plausibly,
report which lineages are ready, their latest/best generation or checkpoint, and
any loss plateau, instability, speed concern or training changes. No universal
generation minimum is imposed. Codex will inspect the actual histories, exposure,
recipes and optimizer checkpoints before deciding whether screening is admissible.
Further substantive retraining returns to this gate.

## Validation separation and targets

Only new successor architectures use
`brn-next-geometry-sha256-8train-1validation-1sealed-v1`. A stable SHA-256 partition
groups placement aliases across side-to-move, rule counters, color/rank transform
and file reflection. Eight hash buckets train, one validates and one remains unused
by GUI training/validation. Groups stay together across sources, generations and
candidates. Two-generation integration checks exercise the real source acquisition.
This is not game-disjoint or distribution-independent validation: the source has
no game identity guarantee, and related positions can remain correlated.

Existing CP/WDL adapters and BT4 Q adapters are supported. This wave selects the
external CP source, not Gen32 imitation. The Arena shared-tranche path is supported
for model binding and tiny integration smoke, but does not implement this GUI
geometry split. Do not treat Arena loss as the independent validation partition or
conduct substantive Arena training under the present handoff. Before later matches,
audit independent opening/position provenance and prevent training on confirmation
evidence; the unused source bucket alone does not prove tournament independence.

## Verification and repairs

The selected verification suite covered 94 distinct test cases with zero failures
or skips in successful runs (88 in `verification-01`, six additional Arena cases in
`verification-02`; repeats are not additional cases). This is not the full repository
suite. Results and failed exploratory runs are preserved locally under
`raw/wave1/`; XML snapshots are retained there as well.

- Nine math tests: direct independent combination/pair reference, finite-difference
  gradients, tiny bounded learning, exact serialized next update, wrong-family and
  corrupt-payload rejection, color/rank and information-boundary checks, saturation.
- Three core runtime cases: core-only codecs, immutable weights, independent worker
  workspaces, castling/en-passant/promotion child/revisit behavior and repeated legal
  depth-2 search through an explicitly common static search adapter.
- Three service cases: corpus target compatibility, partition separation, checkpoint
  publication, source reservations/cursors, interrupted optimizer resume and exact
  uninterrupted/resumed checkpoint identity for every candidate.
- Shared minibatch/streaming, recipe, continuation and provenance checks plus Pair2
  compatibility tests. Six Arena fixture cases include the three new families,
  actual model binding, tiny training, checkpoint selection, legal termination and
  completed resume; these are tiny mate fixtures, not competitive matches.
- Native Swing windows for all three candidates: lineage loading, architecture
  explanation layout, batch/epoch controls, usable Start button and safe closure.
  The same harness then passed against the three actual owner stores, with all
  three rendered windows visually inspected (`native-owner-01.log`).
  Seeder checks cover repeat invocation, preserved edited settings, full optimizer
  reload, Play model loading and unchanged existing Pair2 metadata.
- `verifyProjectBoundaries` passed. `git diff --check` passed before commit.

Native QA exposed a model-browser fallback registering a resolved lineage under
the earlier default architecture. Registration now uses the resolved identity.
This narrowly scoped fix is necessary for reliable new-candidate selection.
Review also corrected empty-square ownership masking before persistent lineages
were created; an explicit information-boundary regression assertion now covers it.

Tiny fit used only three synthetic examples and 512 updates per formulation.
Initial summed squared outcome error was 1.155974; final errors were approximately
0.355559 (GATE), 1.19e-9 (CUBIC), and 2.01e-5 (POOL). The original 128-update fit
criterion failed for GATE/CUBIC; that negative run is preserved. Extending this tiny
diagnostic established learning, not competitive training or an architecture ranking.
GATE's weaker tiny fit remains a training/normalization concern to watch.

No sustained training-throughput benchmark, full match clock/resource preflight,
competitive tournament, statistical strength test, heap/cache profiling, macOS
native verification or installer deployment was performed. The native harness
checks the Windows Swing application; no browser verification is relevant here.

## Baseline and bounded feasibility

Both the tracked runtime payload and original full checkpoint at
`E:\SeedV6-Networks\BRN-Pair2\235d0688-e060-44e3-8f1e-119c39bc8017` were loaded.
The full Gen32 ID and network SHA-256 match CONTRACT. Optimizer step is 1,753,088;
training state is 2,731,424 bytes with SHA-256
`f8bdda5eda0ff7893ee36c2270baaedecaf50e14ff5202ab786996f72a2788d5`.
The baseline checkpoint and existing reference files were hash-compared around new
lineage creation. They were preserved.

Prospective [feasibility plan](wave1-feasibility-plan.json) executed on Ryzen 5 5500,
Windows 11 amd64, OpenJDK `21+35-2513`, one active processor and 768 MiB heap.
The isolated result is `raw/wave1/feasibility-02.json`: 32 recorded legal-walk roots,
20,000 warmup evaluations per model, seven rotating 20,000-evaluation passes.
An earlier `feasibility-01` overlapped GUI testing and predates the empty-ownership
correction; retain it as preliminary evidence, not the final timing result.

| Evaluator | Median ns/evaluation in isolated run | Primitive immutable model bytes |
| --- | ---: | ---: |
| Gen32, rolling Pair2 workspace | 263.700 | 799,688 |
| W1-GATE, full refresh | 1,385.870 | 52,392 |
| W1-CUBIC, full refresh | 814.135 | 104,712 |
| W1-POOL, full refresh | 704.800 | 104,712 |

These are one-JVM Gen0 feasibility measurements, not confidence intervals or search
NPS. JIT/pass variability and the artificial legal walk limit generalization.
The new payloads are much smaller but inference is currently slower. All candidates
admit incremental sufficient statistics; none implements or claims incremental
speed yet. Re-evaluate trained models and actual search before an optimization or
rejection decision. No strength inference follows from this table.

## Reproduction and evidence ownership

Use the offline Gradle cache already configured on this machine:

```powershell
$env:GRADLE_USER_HOME = Join-Path $PWD 'build/migration/gradle-user-home'
.\gradlew.bat :engine-core:test --tests '*BrnNext*' :engine-training:test --tests '*BrnNext*' :app-gui:test --tests '*BrnNextGuiTest' integrationTest --tests '*BrnNextCorpusTrainingTest' --tests '*LearningArenaTest.freshNnueVersusBrn3RoundZeroTrainingCheckpointsArenaAndCompletedResume' verifyProjectBoundaries --offline --max-workers=2
.\gradlew.bat :app-gui:test --tests '*BrnNextGuiTest' -PwaveGuiReport=docs/research/brn-successor-cglhw/wave1-lineages.json --offline --max-workers=1
```

The second command inspects the actual owner wave in native windows without
starting training; normal tests use disposable lineages. GUI PNGs live under
`app-gui/build/brn-successor-native/*-owner.png` and `*-loaded.png` (local only).
The explicit `:app-gui:brnSuccessorWave` command inspects the configured library.
Supplying `-PwaveReport=<new-path>` creates/verifies the deterministic wave and writes
a new report; it refuses to overwrite an existing report and never resets an existing
wave lineage's settings or optimizer. Ordinary application startup does not seed.

Run `:research:brnSuccessorProbe` alone with `-PbaselineLineage=<original-root>` and
`-PprobeReport=<new-result-path>` for bounded feasibility. Do not overwrite old output.
Raw logs/PNGs/experimental output are ignored and not in Git; check local availability
on a fresh checkout. Contract, plans, this compact report and lineage manifest are
the tracked continuity evidence. Production/default evaluator and search policy
remain unchanged. Pair2's production-only search policy still prevents naive GUI
head-to-head from constituting an evaluator-isolated comparison.

Human actions required after this prompt: **Blocking** — substantively train the
ready wave in the GUI and report readiness/checkpoint identities. This blocks
competitive screening and confirmation, not an evidence-driven correctness repair.
