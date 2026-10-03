# BRN-3 operator and architecture guide

BRN-3 is the retained V1 architecture from the [programme contract](BRN_CONTRACT.md).
It is independent of BRN-0/1/2. The [frontier](BRN_FRONTIER.md) and
[programme ledger](../research/brn/BRN_PROGRAMME_LEDGER.md) record acceptance status
and all retained/rejected experiments. The [result report](../research/brn/BRN_V1_RESULT.md)
records the passed criteria and limitations. A successful software build does not resolve
the inherited version-finalization reservation or establish user acceptance.

## Model and information boundary

The evaluator receives four piece bitboards and the side-to-move bit. It ignores
all remaining status bits, clocks, castling rights, en-passant, hashes, history,
attacks, move counts and search state. Search/rule adjudication still uses normal
engine information outside the evaluator. Legal moves used in diagnostics or games
are not model features.

Fixed material is P=1, N=3.2, B=3.3, R=5, Q=9, K=0 pawn units, signed for the mover.
The residual head starts exactly at zero. Learned square/entity embeddings have
random initialization and no handcrafted piece-square values.

Each physical color has a canonical perspective: swap ownership and flip ranks
for Black. An entity is one of 12 owner/type channels on one of 64 squares.
Every unordered pair of occupied entities indexes an eight-channel learned absolute
relation. Its vector contributes to both endpoints. For each endpoint:

`local = ReLU(unary(entity) + sum(incident relations) / sqrt(max(1, pieces-1)))`

Pool the local vectors separately into 12 owner/type groups, scaled by
`1/sqrt(max(1, pieces))`. Concatenate the 96-value mover and opponent pools, then
apply a 32-unit ReLU dense layer and a scalar linear residual head. Add fixed
material in pawn units. Output conversion uses 100 integer units per pawn,
symmetric nearest-integer rounding, and the existing normal static-score band.
It does not map BRN through NNUE's output scale.

Training adds a shared typed-displacement table to each absolute relation. Its
contribution folds into the absolute table when snapshotting; runtime requires no
extra table. There are 2,627,777 trainable parameters and 2,368,577 inference
parameters. Training uses binary64; immutable inference snapshots use binary32.
Float/cache differences have a declared 1e-4 pawn budget.

The worker-local cache keeps raw relation sums for both physical perspectives.
Small placement changes update incident edges; piece-count changes or distant
positions rebuild. It derives its state only from allowed inputs and does not
require a board-stack history. The immutable model can be shared between workers.

## Training recipe and normal application use

The retained recipe uses minibatch 128, eight epochs, initial learning rate .003,
masked Adam beta1=.9, beta2=.999, epsilon=1e-8, and pure expected-score cross entropy.
Coordinates absent from a minibatch retain their weights and moments. There is
no CP auxiliary. A frozen Stockfish WDL adapter converts CP labels and supplies
the differentiable training link; it is not part of runtime BRN evaluation.
Mate labels and unsupported perspectives are rejected by the BRN-3 adapter.

In the desktop application:

1. Select **BRN-3**, then create a new training lineage in the existing storage UI.
2. In **Training Data**, register an existing supported source, such as the local
   Lichess `.jsonl.zst`, and select its allocation weight. No trained BRN is needed.
3. In **Configuration**, set positions per generation and validation method.
   The initial recommendation is 131072 positions and held-out validation.
4. In **Network**, review minibatch 128, eight epochs and the model/run seed.
5. Start. Stop/Resume restores the exact model, optimizer and minibatch cursor;
   changing campaign settings follows the existing explicit restart semantics.
6. Select the resulting lineage's accepted Best through the existing playing-network
   selector. Latest-training and Best retain their distinct existing meanings.

BRN-3 requires Training Data and has no training-position generator fallback.
Normal held-out validation compares Candidate and Best after each generation;
it measures prediction quality. Game-pair validation measures playing strength.
Eight epochs in the ordinary service are one generation, whereas the research
harness evaluates every epoch and selects the lowest validation loss. These are
different checkpoint-selection schedules, not promises of identical trained weights.

The model file is `network.brn3`; training state is a separate checkpoint payload.
Independent version-one formats use fixed layout/prior headers and CRC32. Truncation,
trailing bytes, invalid numbers, incompatible layouts and checksums fail explicitly.
No old BRN payload is silently converted. Training state preserves exact weights,
moments, step and optimizer hyperparameters. The GUI's fresh learning rate is fixed
to the investigated .003 recipe; a resumed checkpoint supplies its own optimizer.

## Headless scratch run

From the repository root, with Java 21 and the existing Gradle wrapper:

```powershell
$env:DEBUG=''
.\gradlew.bat :app:compileVerificationJava :app:installDist -Pheadless --no-configuration-cache
$brnCp='app/build/classes/java/verification;app/build/classes/java/main;app/build/resources/main;app/build/install/seedv6/lib/*'
& 'C:\java\jdk-21\bin\java.exe' -Xmx2g '-Djava.awt.headless=true' -cp $brnCp `
  com.ohinteractive.seedv6.training.service.Brn3CorpusMain `
  '--corpus=E:\SeedV6-Corpus\incoming\lichess\lichess_db_eval.jsonl.zst' `
  '--output=app/build/research/brn-scratch-new' `
  --positions=131072 --batch=128 --epochs=8 --seed=71 --view-record-limit=2000000
```

`--corpus` names the existing Training Data source file. The output must be new.
The raw-record limit bounds acquisition; a 60-minute service limit bounds the run.
The tool registers the source, initializes fresh BRN-3 state, then uses ordinary
TrainerService optimization, validation, checkpointing and deterministic replay.
For a cheap integration smoke use 10000 positions, two epochs and a 30000-record limit.
The output is an isolated training store, not a replacement for any existing Best.

## Exact research reproduction

The evidence manifests bind sampled source identity, raw ranges, eligibility,
ordered split hashes, payload checksums and seeds. They deliberately do not claim
a full 22 GB source checksum or game-disjoint data. Global geometry-group splitting
keeps equivalent inputs in one partition. Source-order/game correlations remain.
The second final range uses label-independent reservoir sampling for held-out records.

The following arguments follow the package-qualified main class
`com.ohinteractive.seedv6.training.service.BrnResearchMain` on the same classpath:

```text
prepare SOURCE DATA1 131072 16384
prepare SOURCE DATA2 524288 32768
prepare-uniform SOURCE DATA3 131072 16384 799907 DATA2/seek
r4-relu-relative-typed-ce DATA1 BRN71 131072 8 71 .003 true 1 .003 8 0 0 0
nnue DATA1 NNUE71 131072 8 71 .001 true
r4-relu-relative-typed-ce DATA3 BRN97 131072 8 97 .003 true 1 .003 8 0 0 0
nnue DATA3 NNUE97 131072 8 97 .001 true
```

Replace symbolic paths with new output directories and check the resulting
manifests against evidence before training. DATA2 supplies the source seek metadata
used to start DATA3 at raw ordinal 799907; it was an exploratory dataset, not a final
candidate test. Its sealed test remains unused. The manifest and harness source are
authoritative for exact raw ranges and split identities.

`Brn3ResearchBridge RUN RUN/production-preview` exports a research state into
independently checked production model/training formats. Conversion receipts bind source and output hashes. Final
diagnostics, runtime measurements and games explicitly use that production model.
The sealed tests have been opened for the frozen recipe; they must not guide further
model selection. New architecture tuning needs validation evidence and a new final
test plan. Existing test results and rejected experiments remain immutable evidence.

The programme does not establish parity with a mature NNUE Best, general Elo, long
time-control strength, or production quiescence search. Production currently has no
qsearch; the separate bounded research probe is reported as such.
