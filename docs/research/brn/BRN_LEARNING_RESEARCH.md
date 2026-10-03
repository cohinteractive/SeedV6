# BRN learning-strength evidence record

## Bootstrap and provenance — 2026-10-03

Root `C:/projects/seed/java/seedv6`, clean starting HEAD
`b14b804` (`Research fresh BRN network`). No applicable on-disk AGENTS.md found
in this repository or ancestors; current user-supplied instructions apply.
Root CODEXLOG_CURRENT.md is absent, so no journal is created. VERSION_STATE.txt
exists with build33. Maintained finalizer and recovery hashes and source commit
match the supplied canonical binding. Begin is blocked solely by overlapping
prior operation `9e5cb01570284814a3a7f45a498ae785` (blocked/uncertain, no verified
bump). This programme proceeds under DEFERRED_FINALIZATION, preserving the prior
operation and version bytes. This record and current diff independently attribute
later work; nothing is claimed to belong to the earlier operation.

First successful machine UTC observation was 2026-10-03T07:07:40Z, after the
initial attachment/root read (PowerShell lacks Get-Date -AsUTC). No root journal
is active. No Mac resource was accessed. No push/deployment authorized.

### Inspected implementation map

- `core/brn3/Brn3Trainer`, `Brn3Features`, `Brn3Model`, `Brn3Workspace`,
  `Brn3Objective`, `Brn3Codec`: fixed material, pair/typed pools, .003 masked
  Adam and expected-score CE; immutable float model, double optimizer state.
- `training/service/TrainerService`, `SequentialTraining`, corpus/source readers,
  `training/selfplay/Brn3CorpusOptimization`: data-backed minibatches and lineage.
  Service resumes latest-training and publishes Candidate as latest independently
  of Best. This supports, rather than reopens, the supplied lineage evidence.
- `training/validation/ValidationArena`, held-out loss, checkpoint store:
  production searches, paired colors, game history, independent Best promotion.
- `gui/Brn3ConfigurationPanel` and architecture configuration, documented in
  `docs/brn/BRN_V1.md`; old GUI/service checks remain evidence, not newly rerun.
- `search/driver/ProductionSearch`, `ExactSearchAdapter`, `search/exact/ExactSearch`,
  `search/evaluation/SearchEvaluation`: neural static-leaf search without qsearch;
  separate opt-in qsearch diagnostics exist. Normal shared semantics are frozen.
- Verification `BrnResearchData`, `BrnResearchMatch`, diagnostics/bridge and
  runtime tools provide reusable data/codecs/production search seams. Existing
  unit tests cover trainer gradients, codec/resume, service, evaluation and GUI.

### Available evidence and limitations

The old viability canon, result, ledger and tracked `evidence/programme-2026-10-03`
reports remain intact. Local ignored `app/build/research/brn-programme` contains
data-v1, data-v2, data-v3-replication, trained seed71/97 states and production
previews. Use these read-only. Their old sealed-test metrics are historical;
new exploration reads validation partitions only. Exact long-run checkpoints
have not been located, and no Mac/external training state will be accessed.

Prior games: each seed .99 versus fresh NNUE on the same100 opening pairs at
25ms/move. This is not a comparison against Gen 0 and not independent400-game
evidence of generational improvement. Production qsearch was disabled.

### External research

Consulted the primary [Stockfish NNUE technical documentation](https://official-stockfish.github.io/docs/nnue-pytorch-wiki/docs/nnue.html)
on 2026-10-03. It describes loss/link choices and explicitly notes that its
evaluation is not called in check. SeedV6's static-leaf path does evaluate
nonterminal checked leaves. This difference motivates position-distribution
measurement; it does not prove causality or authorize changing SeedV6 search.
No external design is adopted as evidence that it will work here.

## LS-01 — preregistered bounded diagnostic

Question: do available trained BRN-3 models differ from material in useful ways
on corpus validation positions but harmful ways in production search/play?
Measure CE and reported outcome half-MSE, residual and same-side parent/child
deltas; inspect search-selected lines; run color-paired fixed-depth games against
material Gen 0. Use seed71 first, then independent seed97/source replication.
Keep frozen model/dataset hashes and per-ply traces. All tool outputs go to a new
ignored `app/build/research/brn-learning` directory; durable compact evidence is
copied here when interpreted. Failures/caps remain visible and do not become draws.

Initial budget: bounded static diagnostics, small depth-four game screen and
per-move deadlines; expand only if it distinguishes a concrete mechanism.
No new training or production behavior change is justified yet. A poor trained
result supports investigation, not architecture rejection. A good result means
the exact lineage/source/depth difference needs isolating.

### LS-01 results and revised frontier

Local Windows preferences identified an existing E: BRN-3 lineage, unrelated to
the independent Mac control. Read-only immutable capture is under
`app/build/research/brn-learning/windows-lineage-v2`; provenance binds original
manifests and SHA256 for all11 models, selected optimizer states and the history
snapshot. Source and destination hashes were checked before/after copying. No
CheckpointStore writer was opened. The initial archive attempt copied successfully
but failed to serialize Instant metadata; its partial output is preserved, and
the corrected new destination captures history strings plus the original TSV.

History has completed Gen1..9, and a published Gen10 without a completed row.
Gen1 used depth4/thread1 and scored .48636 with55complete/9incomplete pairs.
Gen2..9 used depth6/12threads,64complete pairs each, and scored respectively
.4765625,.42578125,.43359375,.3828125,.25,.390625,.3046875,.3828125.
The supplied long-run evidence is confirmed, not superseded by a small new match.

The fixed data-v1 reference population has material half-MSE .212609.
Gen1/5/9 half-MSE .049696/.094481/.101527 and CE .576072/.661084/.680856.
These are NOT new held-out results: ordinary cumulative corpus training has
consumed positions from those ranges, including the earlier research partitions.
The worsening fixed-population result is distinct from the slightly improving
within-generation training losses on changing populations. Thus loss values from
successive generations are not one comparable longitudinal generalization series.

Depth4, one worker,4MiB TT, legal-uniform6..10-ply balanced openings seed620391:

| Frozen model/intervention | Pairs | Complete pair score | Incomplete pairs | All-game score bounds for unknown caps |
|---|---:|---:|---:|---:|
| V1 seed71, full residual, cap512 |16|.55|6|[.390625,.640625]|
| Windows Gen9, full residual, cap1024 |16|.589286|2|[.546875,.609375]|
| Same Gen9, quarter residual, cap1024 |16|.8125|0|[.8125,.8125]|

The quarter screen has22wins/8draws/2losses; paired bootstrap95% [.6875,.921875].
This is exploratory, selected after examining loss traces, not acceptance-grade
replication. No architecture or production setting changed. Full residual's raw
summary WDL includes completed single games from incomplete pairs, whereas its
point estimate uses complete pairs; analyze with explicit denominators/bounds.

One Gen9 loss (opening index1, candidate White) has static value+.45 pawns at
material-18.4, later+1.21 at-19.4, and a near-zero searched value while severe
material losses accumulate. This is a concrete hypothesis lead: the residual
can erase the independent prior's practical influence under search distribution
shift. It is not by itself proof that all material compensation is incorrect.
Mean candidate material over all traced plies was-12.13 for full residual and
+2.52 for quarter, but these are outcome/endgame-confounded populations.

Software: new probe compiles; two targeted tests pass (nonzero-model search
score/move/nodes/PV parity against production SearchEvaluation and correct
administrative-cap/partial-pair accounting). No full suite was necessary.

## LS-02 — discriminating residual/material intervention

Current hypothesis: corpus fitting learns material compensation that generalizes
poorly to search-selected positions, so minimax accepts material-losing lines.
This differs from a global score-units change, which preserves exact-search
ordering apart from rounding/bands. Scaling only the residual restores material
influence while retaining learned preferences between near-equal-material lines.
Do not conclude Adam, architecture, or target policy is solely responsible.

Before further games, measure scalar optimizer-state versus cached model parity
on actual game states; fit static/target material-response and capture-response
statistics; compare identical critical roots and selected line material under
full and quarter residual. Prediction: parity holds, and quarter residual reduces
large materially-losing selected lines and wrong-sign optimism on the same states.
Quarter loss may worsen even if play improves. If parity fails, investigate the
implementation before calibration. If roots do not improve, reconsider the mechanism.

Bounded depth6 Gen9 full-residual pilot (four pairs) is running to isolate depth;
do not infer depth6 strength from depth4. Further replication must use different
openings and at least earlier and later frozen checkpoints before adoption.

### Matched-state mechanism result

Gen9 scalar double trainer versus cached float model agrees on7746actual game
states, maximum error5.53e-7pawn. A cache/codec mismatch cannot explain these traces.
On the fixed16384reference positions, predicted-value regression slope against
material is .21766 (CP-target slope1.65521, untrimmed and tail-sensitive).
On2243states from the first80plies of screened games the prediction/material
slope is-.02707. Among912states with |material|>=5, full residual disagrees with
material sign630times; quarter residual disagrees0times on the SAME states.
This is evidence of distribution-sensitive material cancellation, not proof
that material is always the correct chess answer.

For438legal capture/promotion transitions from256reference roots, average immediate
material gain1.75822pawn corresponds to average same-side full-value change-.52418;
quarter-value change+1.18762. Tempo and tactical anticipation confound these raw
averages; the full distribution and12history-replayed critical-root comparisons
are retained in `mechanism-g9/mechanism.json`. Example opening0,White,ply11:
full search chooses a line ending material-7.4; quarter chooses-4.2. This prediction
does not require changing search or feeding chess classifiers into inference.
Quarter reference CE worsens .68086->1.13528 and half-MSE .10153->.17647,
while the initial game screen improves. Pure global unit conversion is therefore
not the intervention: only the residual/material balance changes.

## Confirmation protocol frozen before new openings

Question: is one fixed quarter-residual calibration useful across cumulative
checkpoints, independent training seed/source and greater search depth, rather
than fitting one selected late candidate/opening set? The first screen is too
small and used shared selection openings, so cheaper evidence cannot establish
sustained utility. No scale sweep is authorized by this protocol; keep .25 fixed.

1. On new opening seed831907, first index0, run64depth4 color pairs each for
   Windows Gen1,5,9 and independent V1 seed97. Require all128games per model to
   complete within cap1024, point score>=.55, and conservative one-sided
   Hoeffding95% lower bound `score-sqrt(log(20)/(2*pairs)) > .5` for each model.
   Also report paired bootstrap intervals. Bounds are conditional on this opening
   distribution, not general Elo; shared openings/models are not512independent
   games. These four tests jointly support the hypothesis only if all pass.
2. Depth6 quarter-residual screen16pairs on old openings, then decide depth6
   confirmation size from completion/cost before looking at new-opening results.
   No material regression at deeper depth may be hidden by averaging depth4.
3. Check historical Gen2..8 intermediate models with smaller bounded screens if
   anchor results suggest instability. A failed anchor blocks adoption and prompts
   diagnosis, not post-hoc threshold relaxation.
4. Search/qsearch and production mapping integration must be checked separately.
   If adopted, keep raw corpus training/resume intact and make runtime calibration
   explicit; never silently conflate raw supervised scores and deployed scores.

Budget/stop: one-worker fixed-depth games,4MiB private TT,10s/10million-node guard
per move, cap1024; incomplete games invalidate acceptance and are investigated.
Stop an experiment on infrastructure failure or no remaining ability to meet its
gate, preserving results. This is bounded controlled replication, not a duplicate
of the Mac uncontrolled long-run training experiment.

### Prospective continuation and tactical diagnostic plan

In parallel with frozen-checkpoint confirmation, resume the copied Gen9 exact
optimizer for two bounded eight-epoch generations of131072new training examples
each. Prepare an isolated geometry-group dataset from raw ordinal3000000, safely
beyond the copied Gen0..10 prefix, using existing read-only navigation as a seed
for a NEW navigation directory. Keep calibration .25 fixed for downstream games;
do not change the raw CE loss, .003 rate, moments or optimizer step. Record raw
and calibrated validation metrics and both output hashes at each endpoint. This
tests a concrete prospective prediction that usefulness survives further Adam
optimization; it is not waiting for or duplicating the Mac run.

Separate qsearch diagnostic: first32validation roots, existing TT-free opt-in
depth4 qsearch, full/quarter/material controls,2million-node/5s guards. Record
all aborts and tails; do not enable qsearch in production or treat zero production
qnodes as tactical validation. Resource concurrency uses at most the observed
12logical CPUs; wall-time tails under concurrent load require an isolated recheck.

### Cost/gate correction supported by observed failures

Gen1/5/9 each complete64new-opening pairs with scores .8671875/.8515625/.86328125,
zero caps/search failures (maximum game lengths548/982/555). Seed97's confirmation
was stopped after56pairs because opening23,candidateBlack hit cap1024. This
attempt FAILED its declared completion gate and is retained. Deterministic replay
of that pair with cap2048 completed: candidateWhite wins at77plies, candidateBlack
loses at1211plies. The material-only opponent took a long time to convert a
23.7pawn advantage; the cap was not a draw or a hidden winning result.

Protocol revision, before repeating confirmation: use cap2048, still require
every game complete, and retain all original score/confidence requirements.
Repeat the whole seed97 run in a new directory, not just replace the capped game
in the failed report. Other completed deterministic games already fit the stricter
old cap and require no replay solely for the larger administrative budget. This
is a disclosed budget correction based on a finished replay, not a score-gate
relaxation or deletion of unfavorable evidence. No production adoption yet.

The raw3000000data request correctly failed the bounded navigation guard before
reading that prefix. Inspection found the exact local Gen10 reserved range ends
at1578888. Revised new raw range [1578888,1953686), with262144training and16384
validation/test each, starts after all copied historical training. The original
seek metadata was copied by the existing reader into the NEW output before use;
the source and original metadata were not modified. The failed dataset directory
is preserved and excluded. Prospective training uses the successful v2 dataset.

Qsearch Gen9:31/32quarter roots finish under5s; root12hits the deadline at553628
nodes during concurrent load. Material and full residual finish that root at
234675/263103nodes. A longer isolated tail check is required before disposition;
the original abort remains evidence. Production qsearch is still disabled.

Prospective endpoint game protocol (frozen before those games): each of the two
new fixed-exposure endpoints plays64pairs against Gen0, seed831907, cap2048,
depth4/thread1. Reuse the frozen Gen9 confirmation openings deliberately for
paired continuation comparisons, with no new tuning or checkpoint selection.
Require each point score>=.55 and the same conservative Hoeffding95% lower>.5,
all games complete. This is two subsequent endpoints with unchanged Adam, not
independent-lineage replication (seed97 supplies that separate control).

The longer Gen9 qsearch tail completes at654180nodes,646685qnodes,qply29,5.393s;
its material/full controls use234675/263103nodes. This is a measured~2.5x tail
increase, not an infinite/tactically invalid search, and remains a limitation
before any separate production-qsearch programme. No qsearch semantics changed.

Added an explicit BRN-only residual-gain research seam in the existing evaluator
definition to test actual multiworker searches. Ordinary BRN behavior remains
gain1 pending adoption; NNUE branches are unchanged. A96-root production
one-versus12worker score comparison (32positions x3gains) tests whether the
historical12thread setting is a necessary explanation; tied best moves may differ.

### Integration candidate and final depth confirmation

All four frozen anchors now pass: Gen1 .8671875, Gen5 .8515625, Gen9 .86328125,
seed97 .8515625, each64complete pairs. Individual conservative Hoeffding lower
bounds are .71420,.69858,.71030,.69858. The repeated seed97 cap2048 report includes
the long loss and is distinct from the failed original. These are512completed
games on64shared openings, not512independent observations.

96production roots (32positions xmaterial/full/quarter) have identical scores
between1and12workers with default192MiB TT. This rules out a necessary thread
score discrepancy on that sample, not every concurrency defect. Depth4 root
diagnostics finish128/128for all three controls: material706492nodes,
full842735, quarter973113. Added calibration does not collapse search completion.
Twelve selected critical-root PV endpoints are mixed (not uniformly better
material under quarter). Different minimax opponent lines make that proxy weak;
do not cherry-pick the favorable example as a universal root-level prediction.

Candidate integration now makes ordinary BRN-3 play use
`material + .25 * (rawModel - material)`, with explicit original/alternative
research controls. Raw trainer predictions, corpus objective, encoded weights,
optimizer moments/steps, codecs and material values remain unchanged. This is a
runtime calibration policy, not model-file conversion. NNUE and shared recursive
search are unchanged. BRN-3 history records the calibration ID; pending game-pair
attempt settings include it so old partial validations cannot be silently mixed.
Old completed checkpoints load as before; unfinished old game-pair attempts need
the existing explicit restart workflow if later resumed under this software.
Held-out-only optimizer attempts do not need that calibration restart. No user
store was resumed or rewritten by this programme.

Final depth protocol frozen before its results:32new pairs, opening seed999013,
depth6,12production workers, default192MiB tables, cap2048. Use ordinary
SearchEvaluation.brn3 for the candidate and explicit material control, not the
reference lambda evaluator. Require all games complete, score>=.65 and
Hoeffding95% lower>.5; report bootstrap as well. This costs more than shallow
screening, but tests the original run's depth/thread regime through integrated
production construction. The16pair depth6 screen remains exploratory and separate.

### Prospective results and software validation

Both prospective endpoints pass all 64 paired openings: +1 scores .8828125
(bootstrap95% [.83984375,.92578125], Hoeffding lower .72983); +2 scores .86328125
([.8125,.91015625], lower .71030). All 256 games finish. Optimizer steps advance
73728 -> 81920 -> 90112, with exact codec resume between endpoints, unchanged
Adam and two disjoint 131072-example slices of the new range. These are prospective
endpoints from copied Gen9, not the original lineage's Gen10/11. Sealed new test
labels remain unopened; validation data did not select a checkpoint or gain.

Paired differences against frozen Gen9 on the same openings are +.01953125
(descriptive bootstrap95% [-.0234375,.0625]) and zero ([-.05859375,.0625]).
This demonstrates retention of substantial utility over Gen0; it does not establish
monotonic gain or exclude small regressions. The earlier full-versus-quarter
16-pair screen improves by .203125 to .265625 under worst/best assignments of
unknown full-residual caps. Its worst-case paired bootstrap includes zero. Keep
that causal contrast exploratory; independent larger confirmations support adoption.

The original capped seed97 game's 1024-ply trace is identical in every recorded field to the
first 1024 plies of the 1211-ply replay; its White game also matches exactly.
The original cap remains an incomplete outcome, never a scored draw.
The separate depth-six exploratory screen finished all 16 pairs, 27 wins,
4 draws, 1 loss: .90625, bootstrap95% [.828125,.96875].

Targeted software validation passes 39 distinct JUnit tests across BRN-3 corpus
training and exact resume, research/production parity, calibration and forbidden
inputs, codec preservation, GUI components, generation restart/archive, NNUE
evaluation state, search integration and corpus training. The broad run passed
38 tests; the final seven-test rerun includes the new compatibility case. That
new case first failed to compile (missing import), then rejected its invalid
synthetic checkpoint ID; both test-fixture errors were corrected before the
successful final run. No production failure was hidden by those corrections.
XML identities and cases are preserved with compact evidence. The entire test
suite, expensive NNUE training/performance suite, packaging and manual desktop
interaction were not run: targeted checks exercise the changed BRN boundary and
unchanged neighboring paths; headless Swing component tests are actual GUI tests,
not a claim of manual desktop interaction. The generated 1000x850 Network-tab
render was also visually inspected: the new calibration explanation is legible
and unclipped. No browser-dependent work was required.

The settings compatibility check confirms the existing automatic Restart
Generation/archive branch on a later start of an old unfinished game-pair attempt.
It does not add an approval gate. Old completed payloads remain loadable, and raw
held-out-only optimization retains its prior attempt identity. No original store
has been started or mutated here. Original Windows seek metadata still hashes to
`a8710cc2deff3fd892c47773dfba5bfc694a5044cda63163d6c94f36e7057c97`.

### Final depth gate, isolated tail and programme closure

The integrated depth-six/12-worker confirmation finished all 32 pairs: 44 wins,
18 draws and 2 losses, score .828125, pair-bootstrap95% [.765625,.8828125],
one-sided Hoeffding95% lower .6117727. No capped or failed games. All three final
gate conditions pass. Combined with four frozen anchors and two prospective
endpoints, this is 832 completed confirmatory games with opening-level dependence
reported explicitly, not an inflated independent sample count.

After both game jobs and the targeted tests finished, the previously slow qsearch
root12 was repeated without competing research jobs. All controls complete with
identical node counts to the longer replay: material234675 (.218s), full263103
(1.845s), quarter654180 (4.102s), quarter646685qnodes and maximum qply29. The earlier
five-second timeout remains recorded. The finite ~2.5x node tail is acceptable
for this static-leaf production milestone but must be addressed before treating
production qsearch as supported. The prior longer replay was under concurrent
load; only this last run is the isolated check specified by the protocol.

All seven game gates, software checks and preservation checks now pass.
[Result](BRN_LEARNING_RESULT.md) closes V1; [reproduction](BRN_LEARNING_REPRODUCTION.md)
and compact hash-bound evidence preserve continuity. Only explicit deferred work
remains. No claim of user acceptance, general Elo or release completion is made.
Read-only finalizer status still reports the prior token blocked/uncertain with
no verified bump; no competing finalizer or recovery was attempted. Root journal
remains absent, root version bytes unchanged. This programme is independently
attributed under DEFERRED_FINALIZATION.
