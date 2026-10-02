# NNUE external Seed corpus training

NNUE's External Seed corpus source consumes architecture-neutral schema-1 records through
`CorpusReader`, `CorpusView`, `CorpusPermutation`, reconstructed Seed Boards and the existing
NNUE feature, minibatch, tanh, half-squared-loss and Adam implementation. Generated NNUE
training continues to use exact terminal side-to-move W/D/L labels (+1/0/-1).

## Frozen supervision: STOCKFISH_WDL_V1

The authoritative reference is Stockfish **17.1**, commit
`03e27488f3d21d8ff4dbf3065603afa21dbd0ef3`,
[src/uci.cpp lines 470–533](https://github.com/official-stockfish/Stockfish/blob/03e27488f3d21d8ff4dbf3065603afa21dbd0ef3/src/uci.cpp#L470-L533).
The downloaded source SHA-256 verified during implementation is
`709f01751b0f369ba15a9b033f00ddef1d1bdce07062a456581034e969aed42e`.
The official [WDL model project](https://github.com/official-stockfish/WDL_model) describes
the model's Fishtest self-play population. V1 is a fixed Seed supervision adapter, not a claim
that historical Lichess records were produced by this Stockfish release or contained its WDL.
No Stockfish executable or runtime download is needed.

Material M counts pieces of both colours: P=1, N=3, B=3, R=5, Q=9, K=0.
Set m=clamp(M,17,78)/58. Evaluate these cubics in Horner order:

```
a = ((-13.50030198*m + 40.92780883)*m - 36.82753545)*m + 386.83004070
b = (( 96.53354896*m -165.79058388)*m + 90.89679019)*m +  49.29561889
```

Stockfish displays CP by rounding `100*internalEvaluation/a`. Because raw integer internal
evaluation is absent from the corpus, V1 explicitly treats stored CP as the **centre of its
displayed rounding bin**, using `x=stmCP*a/100` without another integer rounding. This is
the inverse of the pre-rounding normalization, not reconstruction of an original evaluation.
Use Stockfish's per-mille probability rounding, with Java `StrictMath.exp`:

```
W = floor(0.5 + 1000/(1+exp((a-x)/b)))
L = floor(0.5 + 1000/(1+exp((a+x)/b)))
D = 1000-W-L
target = (W-L)/1000
```

This preserves NNUE's expected outcome meaning and existing `0.5*(tanh(raw)-target)^2` loss.
For material 78, CP 0 → W/D/L 28/944/28 and target 0; CP 50 → 145/850/5 and target .140;
CP 100 → 500/499/1 and target .499; CP 200 → 972/28/0 and target .972. Material 58,
CP 100 → 500/500/0 and target .500. Golden tests freeze these results and the source identity.

Normalize perspective once: STM values retain sign; White values are negated for Black to
move. Widen integers before negation. Source-defined perspective is unsupported. Nonzero
signed mate values map to +1 for STM winning and -1 for STM losing, independent of distance.
Unsigned mate zero is skipped as `mate-zero`. CP values across the stored integer range are
supported; the model naturally approaches saturation. Corpus records are never changed.

## Reproduction and lifecycle

The existing disk index and Feistel selection serve both architectures. NNUE's policy binds
STOCKFISH_WDL_V1 and the Stockfish commit; BRN's BASIC_V1 policy/targets are preserved.
Pins bind root, master seed, positions/generation, view identity and target policy. A fixed
seed-dependent partition keeps training/holdout identities disjoint across all epochs.
Generation ranges advance independently within each partition; no identity repeats within
an epoch. Appends or stronger labels do not replace records in an existing view. Changed
shard bytes fail integrity verification. Changed root/count/seed uses a separate binding;
the existing restart transaction archives unfinished attempts and receipts, preserving
settled generation evidence. Saved expected identities are checked, never silently replaced.

NNUE corpus mode uses its configured epochs/minibatch/shuffle and never calls a position
generator. Game-pair and held-out validation remain independent selections, using their
existing policies. Execution settings are protected while active and editable after Stop.
Generated controls retain values while disabled in corpus mode. The Corpus management tab
is shared. `lastSeedCorpusRoot` migrates the legacy `lastBrnCorpusRoot` preference; a saved
lineage root has precedence. Lineage configuration v3 records the adapter identity and reads
v1/v2 without destructively changing old lineages. Generation attempts, selection receipts,
history and validation evidence identify the view and supervision used. The controller saves
the effective binding after the service releases its store lock. Existing crash recovery does
not invent absent analytics measurements; immutable selection/validation evidence remains
available for those recovered generations.

## Bounded headless diagnostic

`nnueCorpusTrain` is verification-only and never launches SeedV6 or opens Swing. It requires
a new isolated output path and initializes only that store. Its explicit raw-record prefix
limit uses the existing version-1 view/index format. Unvisited records are counted as
`diagnostic-unexamined`, only visited shards are pinned, and the receipt reports the actual
view examination count. This diagnostic subset is not a full-corpus sampling or strength
claim. Normal application views use the entire snapshot. Initial full-view creation and
integrity verification can require substantial disk time for a large corpus.

Example (output must not exist):

```
gradlew.bat :app:nnueCorpusTrain -PcorpusRoot=E:\SeedV6-Corpus\corpus -PcorpusTrainingOutput=C:\Temp\seedv6-nnue-smoke -PcorpusTrainingArgs="--positions=2048 --batch=128 --epochs=1 --seed=71 --view-record-limit=4096"
```

The diagnostic prints the source/view/adapter, selection/replay hashes, exclusion/mate
counts, real optimizer/loss statistics and held-out result, and verifies changed parameter
bytes with zero generated training-position games. The corpus and other network stores are
untouched. No full import or game-pair strength campaign is performed.

## Implementation verification

The isolated diagnostic on 2026-10-03 used the command above with output
`C:\Users\Central\AppData\Local\Temp\seedv6-nnue-corpus-20261003-smoke`.
The real catalog contained 256,888,296 identities. The explicit prefix examined 4,096
records without target rejections; 2,048 training and 512 disjoint holdout examples were
selected. Training used 465 mate records and holdout used 106. The remaining 256,884,200
identities were explicitly diagnostic-unexamined. View identity:
`afb56cc70763232c9fc96d5149732af64b90cc00a293b0f04762d805b4809643`.

One epoch, batch 128, seed 71 produced 16 real Adam updates and changed network bytes.
Training half-squared loss decreased from 0.224823470561172 to 0.22004430403677708.
Holdout candidate/incumbent losses were 0.21454519724948934 / 0.21703520848657487;
the existing rule promoted the candidate within the isolated store. Deterministic replay
matched both selection hashes. Training-position games generated: zero. SHA-256 hashes
of the existing user NNUE Best reference and its four checkpoint files were unchanged.
This is optimization/lifecycle evidence, not a playing-strength result.

Production compilation and 122 distinct focused tests passed, covering the adapter,
NNUE/BRN corpus optimization and selection, shared view/permutation/import management,
configuration/history, non-visible components and generated NNUE baseline training.
Three existing optional BRN GUI/real-corpus tests were skipped by their environment gates.
The selected expensive baseline tests were NNUE service split/resume Adam identity,
two retained candidates and the non-visible controller fresh/resume lifecycle.
All test tasks used `-Pheadless`; `git diff --check` passed. The full expensive suite,
full-corpus view scan/import, real-corpus game-pair/strength campaign and visible/native
GUI testing were not run. SeedV6 was never launched. Normal app visual acceptance
remains with the user.

Version finalization is separately deferred: maintained finalizer status reported only
the pre-existing incomplete operation `9e5cb01570284814a3a7f45a498ae785`, without a new
token or verified bump. Root build 33 was preserved; no historical recovery was attempted.
This implementation and its validation are distinct from that reserved operation.
