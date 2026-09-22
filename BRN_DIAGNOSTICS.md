# BRN-2 evaluator and search diagnostics

This facility characterizes the existing BRN-2 search evaluator. It changes no
network architecture, score conversion, training target, promotion rule, bootstrap
actor selection, checkpoint/resume policy, pruning, quiescence or aspiration rule.
It does not establish playing strength or prescribe calibration.

## Run

Run from the repository root. A checkpoint input is either an **exact checkpoint
directory** (`<store>/checkpoints/<id>`) or its model file (`network.brn2`), or an
exported standalone model file. An existing store root is deliberately not an
implicit selection of its changing Best. No default training folder is opened.

```powershell
.\gradlew.bat :app:brn2Diagnostics '-Pbrn2Checkpoint=C:\models\BRN-2\checkpoints\<id>' '-PdiagnosticOutput=app/build/brn2-chosen.jsonl' '-PdiagnosticArgs=--depth=4 --nodes=1000000 --time-ms=10000 --warmup=1 --repetitions=3' --console=plain
```

Replace the illustrative checkpoint path with the selected identity. The dedicated
Gradle path properties preserve spaces. Add `-PnnueCheckpoint=<checkpoint-directory-or-network.nnue>`
to collect NNUE final static scores on the same roots/children and NNUE searches
under the same limits, private TT size and neural search policy. NNUE's V1 mapping
is fixed here; historical/custom scale overrides are not silently inferred.

An explicit deterministic initialization run needs no files or training:

```powershell
.\gradlew.bat :app:brn2Diagnostics '-Pbrn2Checkpoint=initialized' '-PnnueCheckpoint=initialized' '-PdiagnosticOutput=app/build/brn2-initialized.jsonl' '-PdiagnosticArgs=--depth=4 --nodes=1000000 --time-ms=-1 --repetitions=1' --console=plain
```

BRN-2 uses its architecture initialization seed; the optional NNUE initialization
uses seed 1, matching `BrnRemediationBenchmark`. Both are explicitly labeled
**not trained** in the output. An initialized NNUE fixture is not a trained Best.
Omitting `brn2Checkpoint` selects the same labeled BRN-2 initialization. Omitting
`nnueCheckpoint` omits NNUE entirely; an invalid input never selects a fallback.

`--depth=0` collects evaluations/deltas only. Defaults are depth 4, 1,000,000
nodes, 10,000 ms, zero warmups, one repetition. `--nodes=-1` or `--time-ms=-1`
disables that limit; at least one must remain. Limits use the engine's existing
cooperative control, not a hard process watchdog. Zero limits intentionally
exercise no-completed-iteration reporting. Optional `--label=<experiment-id>`
is descriptive metadata. Search is always one thread with a new cold 262,144-entry
TT per position/repetition, mate-distance-only selectivity and full windows.

Output is UTF-8 JSON Lines in a **new file**; existing output is never overwritten.
Create any parent output directory first. Without `diagnosticOutput`, records go
to stdout alongside Gradle's own log. Use the output file for machine processing.
For direct Java invocation the equivalent flags are `--brn2=...`, `--nnue=...`,
`--output=...` and the numeric options above.

Exact checkpoint directories use `CheckpointInspection.manifest` and
`CheckpointStore.inspect`; managed model paths use `CheckpointStore.readNetworkBytes`.
These reuse payload reader/pruner coordination and integrity verification, without
opening a store writer, repairing references, promoting, training or resuming.
The existing protocol can create an absent empty `payload.lock` on older stores;
model, optimizer, metadata and references are not rewritten. Pruned/corrupt/mismatched
inputs fail explicitly. Model content SHA-256 is always reported; directory inputs
also report checkpoint ID, generation, optimizer step and training-state SHA-256.
The immutable loaded snapshot is retained for the run even if the source later changes.

## Verified score pipeline

The traced implementation is `Brn2Model`, `Brn2Workspace`, `Brn2Accumulator`,
`SearchEvaluation`, `BrnScoreMapping`, `TranspositionScores`, `NnueAccumulator`,
`NnueNetwork`, `NnueEvaluator` and `NnueScoreMapping`.

BRN-2 computes, for each of 32 channels, piece-local node embedding + raw status
context + local bias + incident endpoint relation embeddings. Each local sum has
ReLU (`max(0,x)`). Their sum plus board bias has another ReLU. The value head is:

```text
z = outputBias + sum(outputWeight[h] * max(0, boardPre[h]))
v = StrictMath.tanh(z)
score = 0                                      if v == 0
score = sign(v) * max(1, Math.round(abs(v)*32511)) otherwise
```

`z` is the finite, otherwise unbounded preactivation. `v` is both the final network
output and the normalized model value; there is no distinct denormalization,
probability conversion or subsequent activation. The diagnostic raw getters
retain the actual `z`, including when tanh saturates; it is not reconstructed with
inverse tanh. The ReLUs have a lower boundary of zero and no upper clipping.
`tanh` bounds the value to [-1,+1] and can round exactly to an endpoint.
`BrnScoreMapping` rejects nonfinite/out-of-range values, rounds magnitude with ties
away from zero, preserves a minimum magnitude of one for nonzero values, and
has no additional clipping. The maximum magnitude is 32511; the mate band starts
at 32512 and the engine's mate score is 32768. These units are **not calibrated
centipawns**. A score of 32511 can result from rounding even before `v` is exactly 1.

The BRN output contract is side-to-move value. There is no explicit white/black
sign multiplication after the head: side to move is encoded in raw status, along
with castling, en-passant and move counters. Piece identities/squares retain their
absolute representation. Changing only side to move is therefore not guaranteed
to negate an untrained model. Zobrist is excluded from BRN inputs. Negamax negates
child search results in the usual parent perspective.

`SearchEvaluation.brn2` chooses incremental accumulators when the model has the
existing conservative finite-intermediate bound; otherwise it uses checked
reference recomputation. This flag is an overflow proof, not a parameter clamp.
The runner uses that same choice, root rebuild and parent-to-child transitions.
The optimized accumulator's reassociated binary64 sums can differ from full
recomputation by roundoff; this unit changes neither arithmetic nor rebuild cadence.

NNUE rebuilds/updates both perspective accumulators, concatenates clipped [0,1]
features with side to move first, applies its clipped [0,1] hidden layer and float
scalar head `u`, then `StrictMath.tanh(u)`. SearchEvaluation passes that bounded
value to `NnueScoreMapping.V1`: the same magnitude rounding and nonzero minimum,
with `min(32511, abs(v)*scale)` and V1 scale 32511. Its explicit normal-band clamp
cannot exceed the bound at V1 but can clip historical larger scales. No NNUE
implementation is changed. The report exposes only its final static search score.

Terminal/draw adjudication, mate scores, TT returns and backed-up search scores
are separate from static model evaluation. Root/child evaluation rows intentionally
include static predictions for terminal fixtures; search rows report actual search
results, which may adjudicate those positions without using that static value.

## Corpus and measurements

`Brn2DiagnosticCorpus` reuses all 12 exact-FEN `SearchBenchmark` fixtures: start,
Kiwipete, quiet endgame, tactical queen, quiet pawn, checked king, transposition
knights, queen exchanges, promotions, en-passant, checkmate and stalemate. It adds
the existing Ruy Lopez fixture from `BrnRemediationBenchmark`, the legal queen
endgame from `BrnBootstrapSmoke` (non-capturing checks), and one bounded quiet
fianchetto middlegame fixture, for 15 roots. No new position is claimed to reproduce an old failure.
Kiwipete is the preserved million-node pathology documented in `BRN_REMEDIATION.md`.
The report includes every FEN, a corpus version and SHA-256 over ordered name/FEN
lines. FEN status is preserved; searches start with singleton root history, not
invented preceding repetitions. Every legal one-ply child is evaluated, sorted by
coordinate move. Child IDs are `<root-id>/<uci-move>` and are reproducible from the
recorded FEN. These synthetic sibling populations are not game-frequency weighted.

Records provide:

- `evaluation`: BRN pre-tanh scalar, normalized value, final static search score,
  and optional NNUE final static search score for every root and child.
- `evaluation_summary`: count, min, max, mean, population standard deviation,
  p1/p5/p25/p50/p75/p95/p99, and ten lowest/highest identified samples. Quantiles
  interpolate at `(n-1)*p`. Root and child summaries remain separate so high-mobility
  roots do not silently dominate the root distribution. Empty groups have count 0.
- `boundary_count`: counts with absolute BRN value >= .95, .99, .999 and exactly 1;
  `score_boundary_count`: counts at either end of the integer normal-score range.
  These are explicit diagnostic thresholds, not calibrated risk cutoffs.
- `delta`: both original side-to-move scores, the negated child score in the
  parent's perspective, signed difference `-childScore-parentScore`, its absolute
  value, and `abs(-childValue-parentValue)`. No comparison mixes opposite perspectives.
  A legal move also changes status counters; those are intentionally included.
- `delta_summary`: the same distribution/extreme statistics for absolute score
  and normalized deltas over all moves and each overlapping category. `quiet`
  means no capture, promotion or delivered check; an evasion may also be quiet.
  `non-capture-non-promotion` includes checking moves. Capture, delivered check,
  evasion, promotion, castling and en-passant categories remain separately visible.
- `search_start` is flushed before each invocation to identify an unfinished run.
  `search` contains status/error, requested/completed depth, whole-search elapsed
  nanoseconds, total/main/q nodes, q ratio, NPS, successful evaluator calls,
  maximum absolute/q ply, soft q-depth encounters, checked qnodes, stand-pat
  cutoffs, last completed score/move/PV, and a flag for q ratio >= .95.
- `search_summary` includes node/latency distributions and identified extremes;
  `peer_comparison` gives each result's multiple of the positive peer median and
  a flag at >=10x. Peers are the same backend's measured non-failed searches,
  including capped prefixes. Zero values are excluded from each positive median:
  zero-node terminals are excluded from the node median, while their measured
  latency remains in the latency population.
  Warmup records are labeled and excluded from summaries. Threshold flags are
  triage aids; raw records remain authoritative.

Existing `SearchDiagnostics` main/q counters classify successful child entries by
their owning loop. A qsearch leaf root already entered by main search is not
double-counted. `mainNodes+qNodes == SearchControl.nodes()` is checked even after
limits abort a later iteration. `qRatio=qNodes/(mainNodes+qNodes)`; zero nodes mean
null, not 0%. NPS is null if time or node count is zero. The added primitive counter
counts successful worker-scope static evaluator calls, including root reporting,
not TT hits, terminal adjudications or training forward passes. It shares existing
diagnostic reset/merge/snapshot behavior and is disabled with diagnostics.

No completed iteration means completed depth 0 and null score/move/PV. A node/time
cap may retain a shallower completed move; the status remains incomplete. Exceptions
produce FAILURE with actual control nodes/time and null worker counters because
the failing attempt might not have published them; the final command fails after
reporting the other cases. `end` distinguishes complete, incomplete searches and
failures; absence of `end` denotes an interrupted process or earlier error.

The existing exact-search observer has root-move timing callbacks, but iterative
deepening suppresses those attempt callbacks. This runner measures complete searches
without changing that interface or adding per-node event collection. It cannot
show elapsed partial work after an externally killed JVM beyond the flushed start.
Timing excludes model/checkpoint loading, worker/TT construction and report formatting;
it includes the normal iterative search, request/history setup and optional counters.

Use identical revision, model SHA, corpus SHA, limits, TT settings and JVM environment
for comparisons. Node-only limits support deterministic capped prefixes. Wall-clock
limits can stop at different prefixes. JVM warmup, GC and host load affect latency;
there are no wall-clock performance assertions in CI. Sequential backend runs are
not a controlled architecture speed ranking. Actual trained checkpoints and a wider
representative workload remain necessary before generalizing calibration or strength.

## Validation and bounded observations

The final focused selection passed **37 tests, zero failures/errors/skips**:

```powershell
.\gradlew.bat :app:test --tests '*Brn2DiagnosticsTest' --tests '*SearchDiagnosticsSnapshotTest' --tests '*Brn2AccumulatorTest' --tests '*Brn2SearchIntegrationTest' --tests '*QuiescenceDiagnosticsTest' --tests '*SearchDiagnosticsTest' --tests '*IterativeDiagnosticsTest' --console=plain
```

It includes 15 new diagnostic cases, raw-output/reference equivalence, explicit
sign handling, all legal parent/child transitions with both evaluators, repeated
corpus identity, categories, saturation/fallback and failure reporting, empty and
extreme statistics, ratio/accounting, zero limits, root/q evaluation counting,
enabled/disabled neural search equality (including capped prefixes and terminals),
and checkpoint/model loading with all existing store-file hashes unchanged. The
snapshot regression also verifies evaluation-call reset, immutable publication and
additive merge. During development the category tests caught use of optional
move-type bits rather than production legacy fields, and the missing checking-quiet
fixture; both were corrected before the passing run. No failure was suppressed.

The unfiltered `.\gradlew.bat :app:fullCheck --console=plain` passed on
2026-09-22 UTC in **36m 12s**, exit 0. Counts were read from the completed JUnit XML:

| Gate | Suites | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|---:|
| Routine | 150 | 875 | 0 | 0 | 0 |
| Slow NNUE / persistence / lifecycle / GUI | 14 | 82 | 0 | 0 | 0 |
| Total | 164 | 957 | 0 | 0 | 0 |

No required validation was skipped. Whitespace and final-newline checks passed for
all 14 task files. The starting repository was commit `6a94f6e` (the later stopped
reconfiguration correction, whose documented gate was 942 tests), with only inherited
untracked `app/bin/`; that directory was preserved. Root `CODEXLOG_CURRENT.md` and
`VERSION_STATE.txt` were absent, so neither capability was activated or created.

The bounded observation run used the command above with `--repetitions=2`, no
warmup, depth 4, a 1,000,000-node limit and no time limit. It completed 60 invocations
(15 roots x two evaluators x two repetitions), with two expected BRN-2 node-limit
outcomes, zero exceptions, and matching non-timing fields for every repeated
search. A separate Python JSON parse verified every record, repetition identity
and total/main/q accounting. No training ran in this observation campaign.

Identities:

- Corpus SHA-256: `0503b850d5ed49686e72e601b1216270cfd40fc3bac18fac4dae697b323f3bd8`.
- Initialized BRN-2 model SHA-256: `1205704cbe3437804e96c0fb62d4c8afcfdd9794af15753b29900f7e69c96150`.
- Initialized NNUE seed-1 model SHA-256: `85a6caf9a8ae24ccaed111477d16469f03c1352912edb7bebaa2918929e87d48`.
- Windows 11 amd64, Java `21+35-2513`, 12 logical processors, `-Xms256m -Xmx1024m`.
  Search timings were collected before starting the full test gate.

| Static population | Count | BRN raw range | BRN normalized range | BRN search-score range | NNUE search-score range |
|---|---:|---:|---:|---:|---:|
| Roots | 15 | -0.026665 to 0.299983 | -0.026659 to 0.291297 | -867 to 9,470 | 1 to 9 |
| Legal children | 233 | -0.166676 to 0.382681 | -0.165149 to 0.365034 | -5,369 to 11,868 | -1 to 11 |

None of the 248 sampled BRN values had absolute magnitude >= .95, and none reached
the integer normal-band endpoint. This does not measure saturation inside the
entire searched tree. BRN root score mean/stddev were 1,617.07/3,418.74; child
mean/stddev were 2,652.64/3,979.52.

The 210 quiet relationships had absolute score-delta median 5,195, p95 16,772.65
and maximum 18,984. The largest was Kiwipete `d5d6`: parent +8,843, child +10,141
for its own side, hence -10,141 in the parent's perspective. Starting-position
`g2g3` similarly gave +9,470 to -9,357 (delta 18,827). These measurements include
untrained side-to-move bias and status dependence; they are not merely differences
between adjacent raw head values. Captures had median 12,172 and maximum 17,877
over 16 relationships. The corpus is too small and uneven to infer relative
playing strength or a calibrated volatility threshold.

| Kiwipete, depth 4 requested | Completed depth | Total nodes | Main nodes | Q nodes | Q ratio | Evaluation calls | Search times, ms |
|---|---:|---:|---:|---:|---:|---:|---:|
| Initialized BRN-2 | 2, node limit | 1,000,000 | 1,038 | 998,962 | 99.8962% | 867,600 | 6,715.265 / 7,317.710 |
| Initialized NNUE | 4 | 44,932 | 13,774 | 31,158 | 69.3448% | 38,924 | 180.038 / 141.947 |

The BRN main/q counts and completed depth exactly reproduce the earlier preserved
Kiwipete bounded prefix. Its node count was 404x the positive BRN peer median and
latency exceeded 1,600x that median. These medians include small endgames and are
triage indicators, not position-matched performance expectations. All other BRN
nonterminal positions completed depth 4; the slowest of those was the Ruy Lopez
fixture at 273.931/336.322 ms, with 28,382 nodes and 67.7507% qnodes. Wall times
varied despite identical search trees, as expected without controlled warmup/load.

The evidence confirms large viewpoint-correct quiet deltas and the preserved
quiescence expansion in this initialized fixture **without sampled tanh saturation**.
It does not establish that mapping, bias, status dependence, untrained weights or
any particular search rule caused the expansion. No calibration change is proposed.
The next investigation can repeat these identical inputs on explicitly selected
trained checkpoints; this unit does not launch that training or comparison campaign.

Local raw output/logs are under ignored `app/build/`:
`brn2-diagnostics-initialized.jsonl`, `brn2-diagnostics-run-final.log`,
`brn2-diagnostics-focused-final.log`, and `brn2-diagnostics-full-gate.log`.
The durable observations above survive build cleanup. No normal GUI changes or
browser/manual GUI validation are part of this developer-tool work unit.

## Changed files

- Root: `README.md`, `BRN_DIAGNOSTICS.md`; Gradle entry point: `app/build.gradle`.
- Under `app/src/main/java/com/ohinteractive/seedv6/`: `core/brn2/Brn2Accumulator.java`,
  `core/brn2/Brn2Workspace.java`, `search/alphabeta/AlphaBetaPvsSearch.java`,
  `search/quiescence/QuiescenceSearch.java`, `search/diagnostics/SearchDiagnostics.java`,
  `search/diagnostics/SearchDiagnosticsSnapshot.java`, and new
  `tools/search/Brn2DiagnosticCorpus.java`, `tools/search/Brn2Diagnostics.java`,
  `tools/search/DiagnosticReport.java`.
- Under `app/src/test/java/com/ohinteractive/seedv6/`: new
  `tools/search/Brn2DiagnosticsTest.java` and updated
  `search/diagnostics/SearchDiagnosticsSnapshotTest.java`.

Human actions required after this prompt: None. Trained-checkpoint interpretation
and any subsequent calibration/architecture decision remain separate work.
