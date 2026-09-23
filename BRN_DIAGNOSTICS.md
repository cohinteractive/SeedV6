# BRN-2 evaluator and search diagnostics

The controlled three-arm replay of the canonical batches is reported in
[BRN_SUPERVISION_ABLATION.md](BRN_SUPERVISION_ABLATION.md): exact WDL reproduction,
50/50 and static-NNUE teacher targets, milestone trajectories and bounded final search.

The first trained canonical schema-2 campaign is measured in
[BRN_CANONICAL_G128_DIAGNOSTICS.md](BRN_CANONICAL_G128_DIAGNOSTICS.md), including
the published g121 Best after 128 completed generations, bounded search, throughput
and memory. Earlier schema-1 measurements below remain historical evidence.

The trained g315 color-reversal follow-up, including structural feature evidence,
static/edge symmetry statistics and bounded Kiwipete qsearch sampling, is in
[BRN_COLOR_SYMMETRY.md](BRN_COLOR_SYMMETRY.md). Enable its diagnostic-only records
with `--symmetry=true`; see that report for exact checkpoint commands and limits.

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

## Observational qsearch decision trace (2026-09-22 UTC)

Schema 2 adds explicit `--qshadow=true` to this same entry point. Both checkpoints
must be selected; `--drivers=brn2`, `nnue`, or `both` (default) chooses the search
driver. `--positions=<comma-separated-exact-corpus-ids>` restricts the established
corpus. Omission retains all 15 roots. The ordinary mode still has no shadow.
No GUI, search policy, evaluator arithmetic, model, training or promotion change
is part of this extension.

### Decision flow and isolation

Main search enters qsearch at depth zero, using its current side-to-move window
and a copy of its driving accumulator. Neural search enables only mate-distance
bounds; main-search razoring/futility and aspiration are off. Qsearch itself has
no TT probes/stores, SEE rejection, delta pruning, or static-score pruning margin.
SEE affects tactical ordering only. It generates legal tactical moves outside
check and every legal evasion inside check. Terminal legality precedes draw
adjudication. Checked nodes have no stand-pat. Non-check nodes return a fail-soft
stand-pat score at/above beta, otherwise raise alpha if appropriate, then search
tactical children. A negated child return can cut at beta or raise alpha. The
non-check soft qply limit is 16; checked nodes continue evasions beyond it, subject
to the existing absolute-ply capacity failure. Nothing in this flow was changed.

The optional `QsearchDecisionTrace` observes node entry/window/check state, generated
move count, actual static evaluation, searched moves, returned child scores, and
each return reason. A nullable hook is attached only by explicit diagnostic worker
construction. Normal workers allocate no trace, shadow state, histograms or samples.
`tracedReturn` returns its unchanged driving-score argument; trace methods are void.
Only the driver controls its accumulator, alpha/beta, TT, ordering, history, scores,
move and PV. No observer API was added to normal requests or iterative callbacks.

The final concrete trace owns a separate one-position `SearchEvaluation.State`.
It copies the current board, rebuilds the shadow's production root state on that
copy, and evaluates it. It cannot access driver state, TT, control, history, picker
or PV. The copy is checked for mutation. Loaded immutable models use the existing
read-only checkpoint inspection path. Training state is never opened for writing.
In the inverse mode the same ownership applies to BRN as shadow.

Both static scores and alpha/beta are already current-side-to-move values; they
are subtracted directly. Child returns retain both their original child perspective
and their negation at the parent. Adjacent static delta is
`abs(-childStatic-parentStatic)`. Shadow rebuilds can have different floating-point
accumulation order from an evaluator incrementally driven along the full search
path; these are production root-rebuild predictions, not a claim of bitwise
identity with a hypothetical shadow-driven accumulator history.

### Accounting, bounded records and interpretation

`qtrace_policy` identifies both architectures, checkpoint paths/IDs/generations,
optimizer steps and hashes. It describes the observation/sampling policy.
`qtrace_summary` includes every observed qsearch position, every actual driver
stand-pat cutoff, paired static distributions, all four local cutoff classes,
qply/check/incoming-move breakdowns, child expansion and return-reason counts.
`qtrace_node` holds selected detailed examples with search identity, iteration and
attempt, absolute/q ply, exact board longs in hexadecimal, side, window, actual
and shadow scores, alpha raise/cutoff, generated/searched/returned move counts,
child scores/ranks/categories and actual termination reason. The root's FEN is in
its `position` record; board longs additionally identify intermediate positions
including complete status and key, without requiring reconstruction of a sampled
ancestor path. `AT_ALPHA` is explicit alongside below/inside/at-or-above-beta.

Every observed node is aggregated. Default `--shadow-stride=1` evaluates every
position where the driver actually evaluates. `--shadow-stride=N` selects zero-based
actual-static-evaluation ordinal modulo N equal to zero, across all iterations.
Cutoff-class and score-distribution counters then describe that selected subset;
actual driver cutoffs still describe the full observed population. No extrapolation
is performed. Checks, mates, stalemates and rule draws have no fabricated static
evaluation or stand-pat classification. Soft-limit static returns are paired but
are ineligible for ordinary stand-pat cutoff classification.

Detailed retention is deterministic: first eight completed nodes per cutoff class
(or return reason when ineligible/unpaired), every 4,096th entry ordinal up to
128 such examples, and the 16 largest absolute paired differences. Retained
records are deduplicated and emitted in entry order, with a maximum of 256 nodes
per search. Equal outlier magnitudes retain earlier completed examples. Each node
retains the first seven and last returned child, up to eight, with actual ranks;
generated/searched counts are exhaustive. Records are formatted after search.
Exact integer histograms provide mean/median/p95/max overall and by cutoff class;
smaller breakdowns provide count/mean/max and null quantiles. Child-return deltas
and adjacent static deltas use separate histograms and explicitly distinct populations.
Null-window and difference-at-most-100/1,000 counts are descriptive thresholds in
search units, not calibrated centipawns.

`observed` includes qsearch leaf roots already counted by main search; it is not
the existing `qNodes` counter. `observedCountedQChildren` identifies the latter
population that passed the entry checkpoint. A time/cancellation event between
successful child entry and that checkpoint can leave an entered child unobserved;
the separate counters expose this instead of silently equating them. Reasons sum
to observed nodes after unwind. Aborted nodes have no returned chess score;
exceptions mark accounting incomplete. The experiment below completed every search.

`shadowOnlyAncestorDescendants` counts the union of actual observed descendants
below nodes where the driver continued but the shadow reached beta, within each
qsearch tree. `shadowOnlyFrontierNodes` counts the first such nodes on each path;
their disjoint descendant totals equal that union. `overlappingDescendantSum` is
explicitly overlapping and must not be read as savings. These are locations of
actual work, **not predicted node savings**: substituting a score changes returns,
windows and the visited tree. They cannot establish a standalone cause.

### Exact checkpoint selection and commands

BRN-2 generation 315 (optimizer step 484,034):

```text
E:\SeedV6-Networks\BRN\BRN-2\training\checkpoints\g000315-s000484034-700d920ce6a34a8584539ebd12276972d54e135c17e3384d7f865168be370e36
model SHA-256: ef2760727a1485da30a451c32eeec8e5d541e0c83de97f1d87d8cb57524665fe
training SHA-256: 7dff164f7708077131ef72e5dbc14313a38b3e606c000f090cbb36ed2be9d450
```

Pinned NNUE Generator, generation 74 (optimizer step 8,942):

```text
E:\SeedV6-Networks\NNUE\training\checkpoints\g000074-s000008942-2e22ccbfdeb18f5f26f91f0df183411ac1d01844ccd43e56e9005ee3f5837bc6
model SHA-256: 3279ff72654f56c8f5ce3cc89cc55d1f253de6ae9df00bd61a41293f57dd16a9
training SHA-256: 453b7259d4e8041fa4abb8cc0887540434871d2b151c33676f36e749e0c6cb34
```

The following PowerShell invocations reproduce the commands used, factoring only
their repeated checkpoint arguments. Use new output filenames on repeat runs.
The first Kiwipete run preceded addition of adjacent-delta/frontier histograms;
the final full-corpus run includes Kiwipete again with the complete instrumentation.

```powershell
$qinputs = @(
  '-Pbrn2Checkpoint=E:\SeedV6-Networks\BRN\BRN-2\training\checkpoints\g000315-s000484034-700d920ce6a34a8584539ebd12276972d54e135c17e3384d7f865168be370e36',
  '-PnnueCheckpoint=E:\SeedV6-Networks\NNUE\training\checkpoints\g000074-s000008942-2e22ccbfdeb18f5f26f91f0df183411ac1d01844ccd43e56e9005ee3f5837bc6'
)
.\gradlew.bat :app:brn2Diagnostics @qinputs '-PdiagnosticOutput=app/build/qshadow-kiwipete-first.jsonl' '-PdiagnosticArgs=--qshadow=true --positions=middlegame-kiwipete --depth=4 --nodes=1000000 --time-ms=10000 --repetitions=2 --label=g315-qshadow' --console=plain
.\gradlew.bat :app:brn2Diagnostics @qinputs '-PdiagnosticOutput=app/build/qshadow-corpus.jsonl' '-PdiagnosticArgs=--qshadow=true --depth=4 --nodes=1000000 --time-ms=10000 --repetitions=2 --label=g315-qshadow-corpus' --console=plain
.\gradlew.bat :app:brn2Diagnostics @qinputs '-PdiagnosticOutput=app/build/qshadow-driver-only.jsonl' '-PdiagnosticArgs=--qshadow=false --depth=4 --nodes=1000000 --time-ms=10000 --repetitions=1 --label=g315-driver-only' --console=plain
```

Both directions run by default. Add `--drivers=brn2` for only the principal direction,
or `--drivers=nnue` for only the inverse. All reported runs used stride 1, no warmup,
one thread, a fresh 262,144-entry TT per search, singleton root history, depth 4,
1,000,000 nodes and 10,000 ms. No limit relaxation was needed. Full corpus SHA-256
remains `0503b850d5ed49686e72e601b1216270cfd40fc3bac18fac4dae697b323f3bd8`.

### Kiwipete measurements

All figures are per search, cumulative over iterations 1 through 4. Repetitions
have identical non-timing fields, including complete summaries and selected traces.

| Measurement | BRN driver / NNUE shadow | NNUE driver / BRN shadow |
|---|---:|---:|
| Completed depth | 4 | 4 |
| Total nodes | 671,841 | 41,435 |
| Main / counted qnodes | 13,348 / 658,493 | 9,135 / 32,300 |
| Q ratio | 98.0132% | 77.9534% |
| Observed q positions, including leaf roots | 668,618 | 38,843 |
| Q leaf roots | 10,125 | 6,543 |
| Positions shadow-evaluated | 589,811 | 36,279 |
| Stand-pat eligible positions | 563,626 | 36,191 |
| Actual driver stand-pat beta cutoffs | 313,070 | 29,281 |
| Local shadow stand-pat beta classifications | 306,505 | 25,834 |
| Both cutoff | 202,423 | 23,895 |
| Driver only | 110,647 | 5,386 |
| Shadow only | 104,082 | 1,939 |
| Neither | 146,474 | 4,971 |
| Actual stand-pat alpha raises | 2,893 | 144 |
| Absolute static difference mean | 17,066.43 | 17,149.45 |
| Median / p95 / max | 14,709 / 41,447.5 / 61,424 | 13,106 / 46,590.7 / 61,446 |
| Check / non-check observed nodes | 78,807 / 589,811 | 2,564 / 36,279 |
| Evasion / tactical child entries | 129,045 / 529,448 | 4,959 / 27,341 |
| Maximum qply / absolute ply | 19 / 23 | 17 / 21 |
| Shadow-only frontier nodes | 3,780 | 432 |
| Descendants below that frontier, counted once | 637,732 | 15,372 |
| Trace-enabled elapsed seconds | 8.124 / 6.490 | 0.994 / 0.940 |
| Score / root move | 3,665 / d5d6 | 20,697 / e1g1 |
| PV | d5d6 a6e2 d6e7 e2f3 | e1g1 a6e2 c3e2 e6d5 |

The 637,732 descendant observations are 96.85% of BRN's counted qchildren. This
locates nearly all deep work beneath local shadow-only disagreements; it does
not mean 96.85% would disappear in a modified search. Actual driver-only cutoffs
outnumber shadow-only cutoffs by 6,565, so a simple aggregate shortage of cutoffs
relative to NNUE on this same population is not the explanation.

BRN-driver disagreement scores are far apart: driver-only median/p95 absolute
difference is 26,928/46,378; shadow-only is 28,778/49,116. Of 214,729 disagreements,
185,371 have a width-one window, but **none** have a difference <=100 and only
167 have a difference <=1,000. Narrow windows are common; near-equal static
predictions straddling beta are not the predominant pattern.

| BRN qply | Observed nodes | Driver-only cutoff | Shadow-only cutoff |
|---:|---:|---:|---:|
| 0 | 10,125 | 894 | 1,474 |
| 1 | 20,309 | 3,742 | 1,621 |
| 2 | 13,264 | 2,288 | 3,452 |
| 3 | 27,299 | 7,644 | 3,537 |
| 4 | 27,133 | 5,171 | 6,609 |
| 5 | 44,725 | 12,152 | 5,439 |
| 6 | 42,358 | 6,774 | 9,196 |
| 7 | 57,805 | 13,839 | 6,705 |
| 8 | 52,155 | 6,751 | 10,815 |
| 9 | 61,828 | 12,786 | 7,139 |
| 10 | 53,773 | 6,246 | 11,268 |
| 11 | 56,211 | 10,053 | 7,003 |
| 12 | 49,222 | 5,696 | 10,597 |
| 13 | 48,134 | 7,337 | 6,292 |
| 14 | 39,147 | 4,385 | 8,432 |
| 15 | 35,664 | 4,889 | 4,503 |
| 16–19 | 29,466 | 0 | 0 |

Disagreements span the tactical chain, with shadow-only excess on even qplies
and driver-only excess on odd qplies. This is a population observation, not proof
of an intrinsic parity defect. No ordinary stand-pat classification applies at
qply >=16. Checks continue until a non-check soft-limit return or another terminal.

Of the 104,082 shadow-only nodes, 73,742 follow ordinary tactical captures,
725 capture-promotions, 754 non-capture promotions, 15,795 capturing evasions,
11,592 non-capturing evasions, and 1,474 are qsearch roots. All these disagreements
are at non-check nodes. Checked nodes account for 11.79% of observed nodes and
19.60% of child entries are evasions: significant, but ordinary tactical expansion
is the larger component.

| Actual return reason | BRN driving | NNUE driving |
|---|---:|---:|
| STAND_PAT_BETA | 313,070 | 29,281 |
| CHILD_BETA | 169,041 | 3,596 |
| MOVES_EXHAUSTED | 146,141 | 5,754 |
| NO_TACTICAL_MOVES | 13,292 | 109 |
| CHECKMATE | 889 | 15 |
| SOFT_QPLY_LIMIT | 26,185 | 88 |
| STALEMATE / RULE_DRAW / ABORTED / EXCEPTION | 0 each | 0 each |

For the 104,082 shadow-only BRN nodes, actual return reasons are 53,780 child
beta cutoffs, 45,938 exhausted move lists, and 4,364 no-tactical-move returns.
They directly enter 218,022 children. Non-check nodes overall enter 529,448
children; checked nodes enter 129,045. Of check returns, 56,261 are child beta
cutoffs, 21,657 exhausted evasions and 889 mates.

On **452,292 identical visited qsearch edges** with actual static evaluations at
both endpoints, BRN's absolute parent-perspective static delta has mean/median/p95
14,395.71/11,854.5/36,367; NNUE's shadow has 6,832.64/4,290/22,084.45.
On the 182,045 such edges immediately below shadow-only parents, the corresponding
medians are 14,058 versus 4,206 (p95 37,643.8 versus 19,954). Actual negated child
search returns differ from BRN parent static by median 8,475, p95 26,881 over
529,448 returns; these backed-up scores must not be confused with child statics.
The inverse tree independently shows larger BRN static deltas: median 7,533 versus
NNUE 4,445 over 25,403 identical edges, despite its much smaller visited tree.

### Bounded corpus comparison

Every one of the 13 nonterminal roots completed depth 4 in both directions; both
terminal roots completed as terminal. Corpus totals per repetition are 735,903
nodes / 687,134 counted qnodes for BRN, and 83,617 / 48,723 for NNUE. This reproduces
the established BRN corpus total. All 60 traced searches exactly match their
30 driving-only counterparts on every emitted non-timing search field.

The disagreement columns below refer to BRN driving on its own visited boards.

| Root | BRN nodes | NNUE nodes | BRN qnodes | Paired statics | Driver only | Shadow only | Median absolute difference |
|---|---:|---:|---:|---:|---:|---:|---:|
| opening-start | 6,662 | 2,343 | 498 | 5,389 | 1,042 | 799 | 12,730 |
| middlegame-kiwipete | 671,841 | 41,435 | 658,493 | 589,811 | 110,647 | 104,082 | 14,709 |
| quiet-endgame | 399 | 316 | 73 | 306 | 95 | 26 | 19,526 |
| tactical-queen | 187 | 229 | 8 | 119 | 22 | 15 | 8,635 |
| quiet-pawn | 370 | 576 | 0 | 269 | 80 | 50 | 4,509 |
| check-evasion | 50 | 30 | 6 | 24 | 8 | 0 | 4,085 |
| transposition-knights | 1,010 | 1,320 | 2 | 692 | 189 | 190 | 11,695.5 |
| qsearch-exchanges | 3,273 | 3,197 | 1,345 | 2,306 | 252 | 408 | 8,917 |
| promotion-race | 875 | 941 | 377 | 633 | 72 | 60 | 8,239 |
| en-passant | 459 | 638 | 43 | 307 | 140 | 50 | 7,115 |
| checkmate-terminal | 0 | 0 | 0 | 0 | 0 | 0 | — |
| stalemate-terminal | 0 | 0 | 0 | 0 | 0 | 0 | — |
| opening-ruy-lopez | 24,139 | 21,740 | 11,833 | 20,302 | 2,221 | 3,871 | 10,103 |
| quiet-fianchetto | 24,582 | 8,793 | 14,284 | 22,340 | 7,944 | 3,824 | 34,501 |
| queen-endgame | 2,056 | 2,059 | 172 | 867 | 14 | 10 | 3,023 |

The disagreement pattern is widespread, but extreme expansion is concentrated
in Kiwipete. Ruy Lopez has more shadow-only than driver-only decisions with only
a modest node increase; quiet fianchetto has more driver-only decisions and a
2.80x node increase. Therefore disagreement count or global score difference alone
does not explain expansion; where continuation occurs and the available tactical
chains matter.

### Inference and limits

The strongest measured mechanism is large tactical static-score changes interacting
with stand-pat/window decisions across long ordinary non-check tactical chains.
Many expensive continuations occur where NNUE would locally cut, while BRN also
makes many additional cuts elsewhere. Check/evasion work contributes but does
not dominate node or child-entry populations. There are no qsearch margin decisions
to blame in this implementation, and these neural runs disable the main fixed
razoring/futility margins. Width-one windows frequently expose disagreements but
the disagreements overwhelmingly involve large score differences.

No single independent causal defect is established. The trace supports prioritizing
local tactical consistency and node-specific cutoff geometry in the next design
discussion; it does not prove defective scaling, activation, architecture, WDL
supervision or calibration. Root path/history, evaluator-dependent main search and
windows select the observed populations. A shadow cutoff cannot be substituted
while assuming unchanged descendants or ancestor decisions. Inverse evidence
uses different boards/windows. Check nodes have no static comparison. This is a
bounded 15-root diagnostic, not a strength or training experiment. No remedy was
implemented, and no new training, pushing or deployment occurred.

### Focused validation and work-unit provenance

The final focused command passed **48 tests in seven suites, zero failures,
errors or skips**, Gradle exit 0:

```powershell
.\gradlew.bat :app:test --tests '*QsearchDecisionTraceTest' --tests '*Brn2DiagnosticsTest' --tests '*QuiescenceDiagnosticsTest' --tests '*SearchDiagnosticsTest' --tests '*IterativeDiagnosticsTest' --tests '*QuiescenceSearchTest' --tests '*Brn2SearchIntegrationTest' --console=plain
python -B app/build/qshadow-verify.py
git diff --check
```

Tests exercise extreme opposite shadow predictions under BRN and NNUE drivers
across all corpus roots, fixed-node interrupted work, identical returned scores,
move/PV/nodes and existing counters, board preservation, both-side perspective,
exact alpha/beta boundaries, all four cutoff classes, deterministic summaries and
samples, histogram quantiles, stride selection, reset, trace bounds, non-overlapping
descendant accounting, soft-limit non-eligibility, mates/stalemates/rule draws and
the existing qsearch result/oracle regressions. Initial development failures were
an old schema assertion and two uses of a shortened corpus ID in new tests;
these were corrected, not suppressed. A soft-limit boundary test additionally
guarded the standalone nonzero-qply observation path.

The local Python verifier parsed **4,227 trace JSONL records and 647 baseline
records**, checking every search's total/main/q accounting, reason totals, group
partitions, four-class identities, sampled record classifications and child signs,
sample bounds, descendant union equality, repeat equality of every non-timing
trace field, and search equality against driving-only diagnostics. Checkpoint
hashes and file inventory were compared before/after: all eight files across the
two exact checkpoint directories were unchanged. These are runtime measurements,
in addition to source inspection of state ownership.

`:app:fullCheck` and other broad/long-running suites were deliberately omitted
under the project's risk-based validation rule: this is bounded diagnostic
instrumentation, and targeted tests plus actual checkpoint comparisons cover the
changed behavior. There is no GUI/browser scope. Trace-enabled timings include
shadow/aggregation cost and are not production performance claims; time limits
can produce different prefixes on slower machines. Use a node limit with
`--time-ms=-1` when reproducing deterministic capped prefixes if needed.

Exact files in this work unit:

- `BRN_DIAGNOSTICS.md` (this documentation and durable evidence).
- `app/src/main/java/com/ohinteractive/seedv6/search/diagnostics/QsearchDecisionTrace.java` (new).
- `app/src/main/java/com/ohinteractive/seedv6/search/quiescence/QuiescenceSearch.java`.
- `app/src/main/java/com/ohinteractive/seedv6/search/alphabeta/AlphaBetaPvsSearch.java`.
- `app/src/main/java/com/ohinteractive/seedv6/tools/search/Brn2Diagnostics.java`.
- `app/src/test/java/com/ohinteractive/seedv6/tools/search/QsearchDecisionTraceTest.java` (new).
- `app/src/test/java/com/ohinteractive/seedv6/tools/search/Brn2DiagnosticsTest.java`.
- `app/src/test/java/com/ohinteractive/seedv6/search/quiescence/QuiescenceDiagnosticsTest.java`.

Starting revision was `93aebf6`, with inherited untracked `app/bin/` only. No local
AGENTS.md was present in the repository or ancestor paths inspected. The supplied
operating instructions and repository search contract were followed; no contract
revision or mirror refresh is required by this diagnostic. Root journal/version
files were absent and were not created. Generated JSONL, verifier, hash inventory,
analysis and logs stay under ignored `app/build/` and are excluded from the commit;
the measurements above survive build cleanup. `app/bin/` was preserved.

Human actions required after this prompt: None. Selection of a future intervention
is a separate design work unit, not a blocking action for this completed diagnostic.
