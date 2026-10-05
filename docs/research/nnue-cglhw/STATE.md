# CGLHW state / experiment frontier

Updated 2026-10-05. **Active: E013 first-generation calibration defect repaired and
validated on Windows. Catastrophic collapse reproduced and removed in tested runs;
remaining trained search-strength/gain question is open.** Owner authorized
"proceed on windows"; original-source access and actual checkpoint access work here.
Read CONTRACT.md, BOOTSTRAP-PARITY.md and E013.md before resuming. No success, exhaustion
or fairness-boundary conclusion has been reached.

## Governing decision

The owner's newest instruction permits only fixed bootstrap knowledge verified
in real BRN-3. Primary Track B gives NNUE equivalent V1 material. Track A retains
original material-off NNUE and all E001–E007 evidence. Historical rejection claims
cannot reject the corrected practical track. BRN-3 semantics remain unchanged.

## Completed evidence

Original clean baseline:245a057; local milestone:a7c5e43, never pushed. Source
hashes in provenance.json. BRN-3 has zero neural head at Gen0, fixed material and
persistent .25 residual-only search gain. Blank HalfKP lost all96 depth3 games;
depth2 had0W/2D/69L/25caps. See BASELINE.md.

Track A: E001 perspective/resolution, E002 sharing/factors/counts, E003 local
patches/zero head, E004 global pairs. Correct and often fast, but poor against
material BRN-3. E005 completed1024 games across eight families/32seeds: all
simultaneous conservative upper score bounds below.333 in that unequal-knowledge
comparison. E007 removal probes show random desired-value signs near chance.
E006 old10ms run incomplete after ten pairs; no usable completed iteration.
Harness now records SEARCH_FAILURE. Its Track A rerun is deferred.

Track B E008: optional integer material term added without changing the original
NNUE network, initialization or score mapping. Six-seed screen61W/10D/25L=68.75%.
32-seed reset79W/10D/39L=65.625%, versus corresponding blank NNUE2W/126L=1.5625%.
No caps/failures for corrected random-head NNUE. Paired improvement64.0625points;
conservative one-sided95% lower improvement20.792points; approximate block-bootstrap
lower56.25points. Zero-head material53W/14D/53L/8caps, completed pairs exactly.5.
Three old six-seed baseline losses exceeded E008's shorter cap; see stopping-rule
audit in E008.md. The primary32-seed paired estimate is unaffected by that change.

E008 cost: two warmed isolated JVMs, transition+score1204/1205ns versus original
NNUE1186/1194ns and BRN-33673/3680ns. Material delta12.3/12.4ns, candidate hot
allocations zero. Gates pass (~1.01x NNUE,~.328x BRN-3); real update advantage kept.

E010 depth4:16 wholly new seeds/openings,32games,17W/11D/4L=70.3125%,no caps/failures.
Approximate one-sided95% seed-block-bootstrap lower score62.5%; supporting depth
evidence, not exact distribution-free inference. No wall-time claim from that run.

## Implementation and validation

E013 is the current production training frontier. Actual material Gen1 saved
0W/128L; Gen2 partial1W/88L (39 unplayed/cancelled placeholders), following the
owner's earlier~1W/55L observation. Small8192x8 reproduction0W/16L; actual Gen1
also loses0W/16L to material-only. Actual material Arena round0 was6W/1D/1L,
rounds1-9 collectively1W/71L. Original stores/campaigns were only read.

Root cause: E012 outcome-MSE fits WDL to full32511-unit scale while fixed pawn
material is100. Material was included once and correctly signed, but its scale
was incompatible with the target link. Adam/gradients, train/search mapping,
checkpoint reload and actual Arena construction are mechanically consistent.
Original Gen1 probe residual RMS13851 units overwhelms material1194/1218 times;
linear validation loss improves while playing strength collapses.

Fresh NNUE_MATERIAL lineages now train CE through the existing supervision-only
WDL link on the combined score in pawns. Gen0, search, fixed material, features,
initializer, Adam.001/batch128/eight epochs are unchanged. New02 model/optimizer
magic and Arena objective binding distinguish v2; old01 stores resume v1 without
silent reinterpretation. Exact selected-model copies retain their source recipe.
Use rebuilt code and a fresh same-seed material Gen0 fork for corrected training;
continuing a historical v1 campaign does not repair it.

Full131072x8 matched-intensity repair: residual RMS131.03; probe CE1.27986->.86757,
physical WDL half-MSE.14672->.11288; depth3 score8W/1D/7L. Depth4 independent
blocks together16W/15D/33L=36.72%, so trained parity is NOT established. Same
weights with diagnostic .25 residual gain score29W/20D/15L=60.94% on the same64
openings/colors. This supports a remaining residual-amplitude/search mismatch;
gain is not promoted without prospective Gen0/multi-initialization controls.
Small second-generation continuation10W/7D/15L, stable residual RMS99.87 on2047
probes after excluding one prior-generation alias; no
full-intensity Gen2 or long-term improvement claim. Pilot seeds1/71 avoid collapse.

84 distinct targeted tests pass across the core and source runs; two opt-in
source tests skip for an unset smoke-source variable (actual sources exercised
separately). Exact
v1/v2 checkpoint/cursor/next-step resume, all-layer finite differences, actual
corpus hashes and Arena compatibility are verified. Full evidence and omissions
are in E013.md and e013-* artifacts. Historical material-v1 trained results are
invalid for architectural learning comparisons but retained as diagnostics.
E008/E010/E011 Gen0 results and E012 integration findings remain valid. E009's
separate strict recipe and the million-position programme remain outstanding.

### Historical E012 integration milestone

E012 diagnoses the real Arena campaign `20261004-233713-562e53ca`: it selected the
historical `NNUE` schema/trainer/factory, so its fixed material contribution was OFF.
Nothing was lost on checkpoint reload. Gen-0 and generation-41 audit positions,
including reproduced real opening hashes and frozen training records, match the
legacy path exactly. Gen-0 material-enabled ablations match E008 exactly. The
campaign's completed rounds are11W/1D/316L; those results are **invalid as evidence
for material-parity strength**, and remain legitimate legacy/control evidence.
Existing campaign/checkpoint files are preserved; no resume or conversion was run.

Option B is implemented: UI labels **NNUE (legacy, no material)** (`NNUE`, old wire
identity unchanged) and **NNUE (material parity)** (`NNUE_MATERIAL`, new schema and
model/optimizer magic). Fresh Arena defaults to material parity. Ordinary training,
Play, headless material selection and checkpoint resume preserve the same identity.
Legacy full-outcome checkpoints cannot be silently reinterpreted as residuals.
The then-new E012 recipe preserves E008 initialization and search composition,
training `clip(tanh(raw)+M/32511)` against the existing outcome target. Held-out
validation uses the same combined prediction. Search alone quantizes, adding the
incremental M to the V1 neural score. This recipe is NOT the E009 CE/pawn-residual
strict-control experiment. Disclose the different objective/calibration when
interpreting trained comparisons; equal material alone is not equal learned scale.

E012 isolated eight-game production Arena smoke:5W/1D/1L/1cap; three complete pairs
4W/1D/1L. Eleven real/reproducible audit positions match CGLHW scores exactly.
Small corpus training and exact checkpoint/cursor resume are tested; no large
training run. Final106 targeted tests and3 native GUI tests pass. The post-fix real
campaign audit is identical to its pre-change read-only diagnosis. A separate existing concurrency
test's search-progress timeout also reproduces on unchanged2b5c0d1; see E012.md.
That E012 deployment instruction is superseded by E013's versioned calibrated
initializer; old Resume still preserves its exact historical objective.

Main NnueMaterialBootstrap stores White-relative integer hundredths, updates only
changed squares and signs at STM readout. Explicit SearchEvaluation material
factories and research -material suffix enable it. Legacy factories/codecs stay
material-off. First ablation adds the prior to the unchanged neural integer score
and clamps the sum; no claim that legacy learned units are calibrated centipawns.

E009 new research-only HalfKP pawn-residual recipe: zero head, train M+R with the
same CE link and masked-Adam equations/LR as BRN-3, search M+.25R on both. New
magic/recipe/CRC and independent optimizer state protect legacy checkpoint meaning.
NNUE uses float32 weights/gradients, binary64 moments; BRN-3 training is binary64.
All65 relevant Java tests passed (accounting, incremental/special moves, gradient
finite differences, exact resume/corruption checks, held-out alias purge), plus
27 search integration/mapping/quiescence regressions and5 Python accounting tests.
At the E008/E009 milestone there was no production promotion, existing lineage
change or version-bump claim. E012 adds the explicit production material identity
without changing existing lineages or promoting any trained research checkpoint.

## Completed jobs and historical source dependency

E011 completed:50ms/move,8 new seeds,32games,20W/7D/5L=73.4375%,no caps/failures.
Median completed depth5 both; mean5.999/5.734. Aggregate search nodes/s823823/435774
on actual (different) played positions; see E011.md and timing-summary.json.
No concurrent CPU experiment ran. No research process remains active.

E009 pilot attempted in e009-training-matches, stopped before reading any records
or updating any model: macOS “Operation not permitted” on
/Volumes/Public/Dev/SeedV6/TrainingData/bt4-t80 during DataSource.verify.
Failure/metadata preserved; external isolated root ~/.seedv6-nnue/cglhw/e009-small
contains only the new source registration. Existing prepared local cache exists,
but original-source verification must not be silently bypassed. Async owner
choice pending: enable source-volume access, or explicitly authorize treating the
local prepared cache as a separately identified frozen source with manifest/chunk
checks. No reply was available at E012. Do not retry that denied Mac route without
a changed permission; the later Windows authorization/access is recorded below.

The owner has now authorized Windows and E013 verified both original sources
there. The earlier Mac access decision is no longer a blocker for Windows work;
Mac source permissions were not changed or re-tested.

For the still-unrun strict E009 comparison, use a fresh report/state directory and execute the
predeclared E009 six-seed8192-record,4epoch,batch128 pilot. No tuning based on pilot
outcomes; inspect learning and matches before serious~million-position comparison.
Global-pair/factorized architecture remains admissible if trained HalfKP is weak.
Training and the million-position trial remain outstanding; Gen0 parity alone
cannot complete this programme.

## Environment and continuation rules

E001-E012 environment: MacBook Air arm64,macOS Darwin25.4.0,Java21.0.12.1+1.
E013 environment: Windows checkout C:/projects/seed/java/seedv6,OpenJDK21+35-2513;
stores/corpus on E:. No existing campaign was restarted; no research job remains.
Gradle verification tasks:
nnueBootstrapAudit,nnueArchitectureScreen,nnueResidualTrainingScreen. Exact neural
search uses private4MiBTT/one thread; no qsearch/book/tablebase. HeadlessGame supplies
chess terminations and explicit administrative caps.

Continue across phase/context boundaries. No push, root journal, or Finalizer
(explicit owner workstream override). Preserve coherent local commits and all
failed/capped evidence. No architectural-limit or forbidden-knowledge conclusion
is justified by the training data access failure.
