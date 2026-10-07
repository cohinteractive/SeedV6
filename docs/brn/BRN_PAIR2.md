# BRN Pair-2 (experimental)

`BRN_PAIR2` is a supported experimental network family, independent of NNUE and
native BRN-3. It promotes the completed architecture programme's
[C02 compiled order-2 pair tables](../research/brn-architecture-cglhw/RECOMMENDATION.md)
from commit `d23a05f`. It does not change the production/default evaluator or
automatically adopt a research checkpoint. Long-run scaling remains unproven.

## Start, stop and resume

1. Build and launch the normal desktop application:

   ```powershell
   .\gradlew.bat :app:installDist --offline
   .\app\build\install\seedv6\bin\seedv6.bat gui
   ```

2. In **Network Training**, select **BRN Pair-2 (experimental)** and **New
   Lineage...**. Choose a name. Storage settings determine the model-library root.
3. In **Data & exposure**, add registered Training Data from the shared library.
   Lichess/Seed CP sources and explicitly labelled BINP sources use the existing
   source adapters. Pair-2 shares data readers, never another model's source cursor.
4. Set positions per generation (initial default 131,072). The research recipe is
   learning rate **0.01**, batch **128**, **8 epochs**, masked Adam
   beta1=.9/beta2=.999/epsilon=1e-8. The run seed controls ordering; initial learned
   weights are always zero. The training objective is the original expected-score
   cross entropy through the Stockfish WDL link, with the fixed material prior.
5. Start Training. Use the normal generation/time limit or run until stopped.
   **Stop Now** saves the existing exact optimizer-boundary continuation;
   **Stop after Generation** lets the admitted generation settle. Select the same
   lineage and Resume to continue latest-training, including weights, moments,
   step, shuffle position and reserved source ranges. Best can lag latest-training.

Held-out validation is the initial default and promotes strictly lower loss;
game-pair validation uses the existing promotion policy. This is the ordinary
SeedV6 promotion workflow, not the research programme's 600-second selection rule.
Hours/days of continued training consume new source ranges: an exhausted source
stops with an explicit error, without wrapping or silently substituting data.
Existing generation-boundary recipe changes preserve optimizer moments and step.
Pair-2 requires Training Data; generated self-play/bootstrap training is not added.
See [Training Data](../corpus-training.md) for source registration and preparation.

## Representations and persisted identity

The geometry and arithmetic preserve C02: 327 anchored templates over eight
shapes, using only the first two cells of each original triple template, 13 square
categories, two canonical roles, and normalization `1/sqrt(654)`. Training keeps
113,231 binary64 absolute/shared/bias parameters and masked Adam moments. Shared
role/shape terms fold into 110,527 binary32 inference parameters. The compiler
then combines duplicate/reversed terms into **294 physical pair tables**, with
adjacent binary64 values for each side to move. This float-folding boundary is
intentional and matches the validated research function.

The normal checkpoint contains two different artifacts:

| Artifact | Contents | V1 bytes |
| --- | --- | ---: |
| `training.state` | Unfused binary64 weights, both Adam moment arrays, step, rate and address coverage | 2,731,424 |
| `network.brn-pair2` | Compiled binary64 pair tables and bias, ready for inference | 795,032 |

The runtime model alone cannot resume training. Checkpoint loading independently
recompiles the saved trainer and verifies exact compiled-table equality. No
optimizer state is inferred from runtime tables or imported research models.
Compilation runs at snapshot/publication, outside search; loading a standalone
runtime payload reconstructs only its fixed geometry.

Both codecs have distinct magic, CRC32, fixed sizes and an explicit header for
family artifact kind, architecture/relation/compiler versions, template/category/
order/pair/parameter dimensions and binary64 representation. V1 fixes the
binary32 folding step, ownership mapping, material values and quarter-residual
calibration. The existing checkpoint manifest supplies schema `seedv6.brn.pair2`
version 1, generation/model identity, parent, hashes, lengths and optimizer step;
the existing sidecar supplies optimizer configuration. Other network families,
research tuple files, incompatible revisions and corrupt payloads are rejected.

Managed lineages live under `<model-library>/BRN-Pair2/<lineage-UUID>/`, using
ordinary `checkpoints/`, `refs/best`, `refs/latest-training`, promotion/history,
partial-generation and Training Data ledger records. Arena owns its own A/B stores
and copies an explicitly selected checkpoint when requested. It does not update
the source lineage's Best. No BRN or NNUE histories are migrated.

## Search, play and comparison

In **Play**, use the shared model browser to choose a Pair-2 lineage and Best or a
specific generation. Each search worker gets its own rolling placement cache;
immutable tables are shared. Only placement and side to move enter evaluation.
The material prior and `material + .25 * learnedResidual` mapping are unchanged
from C02, with 100 search units per pawn and the existing static-score limits.
Ordinary UCI remains on its existing default; this change does not introduce a
separate UCI model-selection protocol.

The existing exact-search benchmark now accepts any supported model store:

```powershell
.\gradlew.bat :app:exactSearch --offline `
  -PsearchModelStore="C:/path/to/BRN-Pair2/lineage-UUID" `
  -PsearchArgs="--position=start,kiwipete,endgame --depth=4 --tt=on --warmups=3 --repetitions=5"
```

Add `-PsearchCheckpoint="g..."` for an exact generation; omission loads Best.
The output includes family, checkpoint ID and network hash. Repeat with NNUE,
native BRN or another Pair-2 lineage using the same search settings. Model loading
and worker construction are outside reported search time. Wall time is a bounded
measurement, not evidence of playing strength.

In **Arena**, Pair-2 is available alongside native BRN-3 and both NNUE identities.
Choose a fresh initializer or an exact generation, set shared data/exposure and
bounded match settings, then run or resume the normal campaign. Other supported
families remain selectable in Play and the exact-search benchmark; the existing
Arena training capability boundary for older BRN-0/1/2 is unchanged.

## Developer verification

The original `BrnTupleTrainer`, `BrnTupleWorkspace` and `BrnTupleCompiled` remain
unchanged in `verification`. The shipped `core.brnpair2` implementation retains
their order-2 learning/compilation/cache arithmetic. Focused tests compare exact
weights/moments, snapshot folding, resume bytes, special moves/undo/STM/information
boundaries, search score/move/nodes, codecs, independent stores, UI selection and
normal training/Arena continuation. The benchmark also tests Best/exact-generation
loading. No generalized evaluator framework or research campaign is introduced.

For a retained final research model, the explicit developer parity command checks
its frozen SHA-256, dataset integrity, all validation and sealed positions at five
gains, compiled serialization, and 32 depth-3 roots with one warmup/two measured
passes. It never trains, imports or promotes the research artifact:

```powershell
.\gradlew.bat :app:developerTools --offline `
  -PtoolArgs="pair2-parity app/build/research/brn-architecture/z01-scaling-compiled-compute-600-211/selected.model app/build/research/brn-architecture/z01-data-a c86c4bc006e7503153a71df669e6c68c4c6de9e82f99db3e30ceccb8580588af"
```

The B counterpart uses `...-337`, `z01-data-b`, and hash
`f4baf07b4c678f0cd7c556790a9a7a200bd33bcd61a7ecc0f55748ace33005c0`.
These large retained research inputs are local, ignored artifacts; ordinary tests
do not require them. Neither passing parity nor bounded matches prove long-run
scaling, stronger play on every machine/time control, or production adoption.

## Integration evidence (2026-10-07)

Both frozen final C02 models above passed the integration parity command. Each
covered 32,768 validation plus 32,768 sealed positions at gains 0/.125/.25/.5/1:
**131,072 positions and 655,360 board/gain comparisons in total**, with zero
observed pawn error and zero integer-score differences. The supported compiled
values also matched the original C02 compiled evaluator bit for bit on this sample.
The three original tuple trainer/workspace/compiler sources remain byte-identical
to `d23a05f`. This is observed equivalence, not a universal floating-point proof.

On each model's 32 depth-3 roots, one warmup and two alternating-order measured
passes gave identical scores, moves and node counts. Model A's measured 96,920
nodes took 64.89 ms in the research implementation and 60.79 ms in the supported
implementation; model B's 90,944 nodes took 51.49 / 48.55 ms. These short single-host
measurements are a performance sanity check with substantial timing uncertainty,
not a new strength/performance research claim. Primitive storage remains 799,688
immutable bytes plus 2,920 bytes per worker, excluding JVM overhead.

Application/verification compilation and `:app:assemble` completed offline on
Java 21; JAR, installed launcher and ZIP/TAR distributions were built. The JAR
contains Pair-2 application classes and excludes the research tuple/parity tools.
Latest reconciled JUnit results across 26 relevant suites contain **153 passing
tests, zero failures/errors and seven native-window skips** under `-Pheadless`.
Two old fixed-choice-count/selection fixtures initially failed when the enum grew;
they were extended and rerun successfully. The final focused run completed with
`BUILD SUCCESSFUL`:

```powershell
.\gradlew.bat :app:test --offline -Pheadless `
  --tests '*BrnPair2*' --tests '*LearningArenaGuiTest' `
  --tests '*ResumableTrainingTest' --tests '*ExactSearchHarnessTest'
```

The preceding broader run also covered tuple oracles/compilation, native BRN and
NNUE corpus services, Arena training, model library, recipes, source/generation
continuation, lineage provenance, folder selection, Play selection and existing
BRN GUI controllers. Pair-2 tests demonstrate bit-exact weights/moments against
the original trainer; byte-exact paused continuation; fresh initialization then
restart through generation 2 with new source ranges; separate Best/validation;
codec/family rejection; model-selected benchmarking; and normal one-/two-worker
search. Verification receipts remain in local ignored
`app/build/pair2-integration/`; ordinary unit tests use disposable directories.

The full repository suite, explicit slow-NNUE suite, native visual QA, platform
installers and hours/days of sustained training were not run. No release, push,
production-default change or historical model-store migration occurred. Version
bookkeeping is a non-blocking limitation under the SeedV6 override: the initial
full-tree capture was stopped before any operation/reservation was created;
subsequent status was idle with no unfinished operation. Build 34 is unchanged
and no verified bump is claimed. Root repository journal is absent.
