# NNUE Training Data

NNUE consumes the common TrainingPosition source interface through sequential,
per-lineage source reservations. See the [Training Data guide](../corpus-training.md)
for seeking, setup and lifecycle semantics. Both variants retain the NNUE feature kernels,
minibatch optimization, neural tanh output and Adam. Generated
positions retain exact terminal side-to-move W/D/L labels (+1/0/-1).

**NNUE (legacy, no material)** retains persisted identity `NNUE`, predicts
`tanh(raw)` and uses half-squared outcome loss.

**New NNUE (material parity) lineages use the E013 calibrated objective.** Let
`M` be STM material in engine units (P100,N320,B330,R500,Q900,K0), and
`s = clip(tanh(raw) + M/32511, -1, 1)`. The continuous search score in pawns is
`p = 325.11*s`. Training minimizes binary expected-score cross entropy through
the existing smooth Stockfish WDL link at `p`, with the same outcome targets.
Its raw-output gradient is `CE'(p)*325.11*(1-tanh(raw)^2)` inside the score clamp
and zero at/outside it. Held-out half-squared outcome loss uses that same smooth
WDL prediction; the trainer's initial/final/mean losses report cross entropy.
These losses have different units and should not be compared as one metric.

Search still computes `clip(M + V1.map(tanh(raw)), -32511, 32511)`. The initializer,
material values, geometry, neural inference and V1 mapping are unchanged; no WDL
link runs inside search. Gen-0 search scores therefore remain exactly E008's.
No BRN-3 quarter-residual gain is silently applied. Adam remains .001/.9/.999/1e-8
by default, with existing configurable batches and epochs.

**Historical E012 material checkpoints retain v1 semantics:** prediction `s`,
half-squared outcome loss, and the same additive search score. E013 reproduced
catastrophic degradation under this formulation: learning WDL magnitudes at the
full search range overwhelms the material prior. Its trained results cannot
establish an architectural NNUE learning limitation; retain them as controls.

Calibrated material model/optimizer payloads use `S6NMAT02`/`S6TMAT02`, retaining
the same geometry and payload lengths. Prior `01` payloads remain readable,
round-trip exactly, and resume their original objective. Model/optimizer objective
disagreements are rejected. Fresh Training/Arena initializers select v2; Arena
pins the objective in its binding. Exact model copies inherit the source objective.
Updating the application does not repair or reinterpret an existing trained model.
Use a fresh material-parity lineage with the same seed for a corrected Gen-0 fork.

Both material recipes match the CGLHW E008 Gen-0 evaluator; they are distinct
from the E009 research-only pawn-residual/CE experiment. Legacy `NNUE` schemas,
factories and codecs retain their original behavior. See
[E013 investigation](../research/nnue-cglhw/E013.md) for evidence and limits.

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

This preserves the expected outcome target. The persisted objective uses either
half-squared loss (legacy/E012) or calibrated cross entropy (E013) as defined above.
For material 78, CP 0 → W/D/L 28/944/28 and target 0; CP 50 → 145/850/5 and target .140;
CP 100 → 500/499/1 and target .499; CP 200 → 972/28/0 and target .972. Material 58,
CP 100 → 500/500/0 and target .500. Golden tests freeze these results and the source identity.

Normalize perspective once: STM values retain sign; White values are negated for Black to
move. Widen integers before negation. Source-defined perspective is unsupported. Nonzero
signed mate values map to +1 for STM winning and -1 for STM losing, independent of distance.
Unsigned mate zero is skipped as `mate-zero`. CP values across the stored integer range are
supported; the model naturally approaches saturation. Corpus records are never changed.

## Reproduction and lifecycle

Source order and range reservations are independent of architecture. NNUE retains
its configured epochs/minibatch/optimizer shuffle. Both validators remain supported.
Data acquisition uses a transient generation list and encodes no unused source
positions. The Network tab owns NNUE settings; Training Data owns source setup.

`nnueCorpusTrain` remains an isolated verification-only command. Select
`--variant=material` or `--variant=legacy`; historical invocations default to legacy
and the command prints the resolved identity. `--corpus` accepts
a source file or legacy Seed directory. The compatible `--view-record-limit` option
now caps raw records decoded for the diagnostic; it creates no view/index. The output
must be a new isolated path. For example, request 2,048 training positions, 512 held
out and a 4,096-record limit. It uses the production sequential provider and trainer.

## Historical pre-redesign implementation verification

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
