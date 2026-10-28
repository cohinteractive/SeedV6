# Execution and recovery

Repository root: `C:/projects/seed/java/seedv6-unified`. Read STATE/CONTRACT first.
Current local Java21: `C:/java/jdk-21/bin/java.exe`; chess-enabled Python:
`build/research/venv/Scripts/python.exe`. No global environment changes required.
Check binaries and raw inputs before use. A fresh Git clone lacks ignored evidence.
Do not reconstruct missing weights from descriptions or rerun completed experiments
as if their original evidence were available. Restore recorded artifacts or admit
a separately identified reproduction; preserve original conclusions and hashes.

## Owner control and resource gate

`./nnue-control.cmd status` reads actual permission/ownership. NORMAL alone does
not establish capacity. Before an empirical operation use the bounded resource
probe from tools/brn-relational/baseline.py; runners enforce admission/ongoing
thresholds. No GPU has been used by this workstream. Do not reset holds.

Owner commands in a separate terminal: `./nnue-control.cmd hold`, `pause`,
`hold --now`, `pause --now`. They act only on the supervised owned Windows job.
Respect a later hold/full pause and exit after recording the safe state. Never
kill unrelated Java/Python/VSCode processes by name. Completion requires active:null
and owned_processes:0,plus operation release receipts when applicable.

All empirical commands go through:

```powershell
.\nnue-control.cmd run --name NAME --resume-note 'Exact recovery boundary' --log NEW_LOG_PATH -- build/research/venv/Scripts/python.exe -B SCRIPT PLAN
```

Use a new launch log every attempt. No background queues,scheduled retries or
availability waits. Administrative timeouts preserve incomplete evidence and
cannot generate chess time-forfeit outcomes.

## Build and verification

Use local Gradle home `build/migration/gradle-user-home`,offline dependencies,
`--no-daemon --max-workers=1`,and per-command `JAVA_TOOL_OPTIONS` containing
`-XX:ActiveProcessorCount=1 -XX:+UseSerialGC`. Do not change system/user settings.
Run Gradle through the existing supervisor using `cmd.exe /d /c gradlew.bat ...`.
Targets: `:installResearchWorkspace :research:installResearch`; targeted tests:

- `:engine-core:test --tests '*ConventionalNetworkTest' --tests '*BrnPair2RuntimeBoundaryTest' --tests '*BaselineEngineTest' --tests '*ProductionBaselineTest'`.
- `:research:test --tests '*PairObjectiveDiagnosticTest' --tests '*RelationalGraphEnergyTest'`.
- R004 additionally uses `--tests '*RelationalMessagesTest'`.
- Python: `-B -m unittest discover -s tools/brn-relational -p 'test_*.py' -v`.

Do not overwrite an experiment's frozen runtime with a rebuilt installation.
New source tests use live build outputs; frozen experiments keep their own hashes.
Before staging inspect exact paths and `git check-ignore`; raw/ stays local.
These research-only changes do not alter the shipped application/build number.

## Completed stages and checkpoints

R001 uses baseline.py preflight/match,r001-plan.json and raw/r001/runtime.
Its16 games and48 probes are complete. R002 pilot/depth/coverage use run_objective.py
and named plans/configs; all their arms are complete. Original pilot runner bytes
are raw/r002-pilot-runner.py; depth/coverage bytes are raw/r002-depth-runner.py.
They exactly match original binding hashes. Current runner additionally supports
graph dispatch; do not silently change historical binding files to accept it.
Frozen byte copies support source reconciliation; completed stages need no rerun.

R002 selected playing endpoints are epoch16 under raw/r002-coverage-aligned-{71,97};
each has compiled .brn-pair2 and full optimizer .state plus SHA-bound epoch receipt.
Earlier pilot/depth models and every epoch are preserved. These are research
states,not managed user lineages. Record objective/config with any export; the
ordinary Pair-2 binary alone does not encode the altered training objective.

R003 uses r003-plan.json,raw/r003-runtime and six raw/r003-MODE-SEED directories.
Its8-epoch pilot is complete; CONTEXT has no larger-training admission. Graph
states have separate magic/geometry/CRC and contain weights/moments/step. No graph
runtime is integrated with search or production; full reconstruction is the only
measured graph inference method.

R002 playing uses compare_pairs.py and r002-games-plan.json. The binding fixes
runner/helper/model/runtime/opening hashes. Complete game files are atomically
published after replay; recovery replays/verifies then skips them. Only an unfinished
game is replayed from its opening. Prediction audit is saved once; no fitting occurs.

## Interrupted training

Read the latest execution/epoch receipts,config.sha256 and binding.json. Check all
complete checkpoint hashes. Original run config is immutable. Resume the same
plan only with its matching runner/runtime; completed arms/epochs are verified
and skipped. Epoch shuffling is `Random(seed+epoch)` and state preserves Adam
moments/step/rate. In-memory partial epochs repeat from the last complete epoch.
The supervisor's generic latest_checkpoint field belongs to the older NNUE run;
the BRN experiment's own epoch receipts are authoritative for these CPU runs.

A killed write can leave a partial .state/model/receipt. Preserve bytes and inspect
ownership/config before moving only those uncommitted epoch files into a named
attempt subdirectory; never delete/replace a valid completed receipt or edit its
hash. Malformed completed evidence or unexplained changes block dependent work.
Document the diagnosis and smallest replay. No historical source reconstruction
or broad cleanup is part of resumption.

## Later completed stages and current screen

R002-SCALE's data selection and both epoch24 endpoints are complete. The selection
SQLite database checkpoints cursor and accepted rows transactionally; completed
manifest publication pins TSV/ID hashes. Do not repeat it. Run configurations
r002-scale-aligned-{71,97}.json explicitly bind coverage epoch16 optimizer states
as a data fork. r002-scale-plan.json uses the frozen depth runtime.

R004 uses r004-plan.json and raw/r004-runtime; all four8-epoch runs are complete.
Its COMPOSE recipe failed admission. The new MessageDiagnostic dispatch does not
change older graphs; raw/r003-runner.py matches their original runner binding.

R001B uses compare_pairs.py with explicit evaluator types and the frozen R002 games
runtime. raw/r002-games-runner.py preserves the old completed comparison runner.
Do not substitute changed scripts into old binding files. The broader screen's
own plan/checkpoint directories carry its identities. Same game recovery applies.
No training or tournament is queued outside the foreground supervised command.

## R007 through R009

R007 teacher data and all four epoch40 continuation arms are complete; pure
CP64 distillation failed the5% gate in both seeds. Prediction audits use
run_prediction_audit.py and the two r007-prediction-*-plan.json files; completed
outputs have binding and SHA receipts and need no repeat. Check local availability.
The earlier run_objective.py bytes are preserved as raw/r007-objective-runner.py;
current dispatch adds ContextPairDiagnostic and must not replace frozen bindings.

R008's64 small counterfactual quartets are intentional tracked input fixtures,
not bulk measured output; no new ignore exception is needed for JSON inputs.
Its raw measured result stays ignored. run_interaction_audit.py r008-plan.json
supports verified completed-output skip. A killed publication without its receipt
requires preserving/inspecting partial evidence; do not overwrite it automatically.

R007-BT4-GAMES uses r007-bt4-games-plan.json and compare_pairs.py. Its64 fixed games
have the existing atomic-game recovery semantics. R009's prospective protocol
must pass correctness before configs/runtime are frozen and training starts.
Its full optimizer states carry mode,geometry,rate,step,base SHA,weights,moments
and CRC. The separate frozen Pair-2 base remains required; no standalone state
may be treated as a deployable Pair-2 checkpoint. Full inference only; no search
or incremental integration is claimed at this stage.

## Owner neural-priority steering and R011

Contract revision2/PRIORITY.md are active. R010's240.80s tranche safely checkpointed
1,574,296 rows,cursor1,449,984; r010-checkpoint.json pins its immutable saved state.
No automatic further extraction or Pair2 training. Packed-data parity tests passed
and the implementation is retained for justified reuse,not as a queued Pair2 run.

R011 four COMPOSE arms and symbolic support audit are COMPLETE. Reuse their
results and matched completed R006 PAIR controls. r011-validation.json records
byte-identical frozen neural class parity plus9 passing targeted tests. No old
gate or conclusion is overwritten. R012 is the next neural architectural test.

## Current resource pause

RESOURCE-GATE.md is the current operational gate. Do not launch jobs or recheck
availability until owner RESUME. It records a pending R012 numerical-test retry,
not a trained model recovery. No GPU or clocks are authorized by RESUME. The
supervisor's NORMAL state alone does not clear the resource gate.
R010 extension now has the distinct prepare_coverage_extension.py filename;
raw/r010-selection-runner.py preserves exactly executed bytes. The original
prepare_coverage.py131k selector is restored from its committed bytes.

## R012/R013 completed neural diagnostics

Owner RESUME cleared the historical R012 resource gate. All4 anchor tests pass;
r012-validation.json preserves the initial numerical-test failure and successful
step refinement. R012 runtime/configs,64-case capability audit and all four8epoch
arms are COMPLETE. R013 streaming encoding matches the original loop byte-exactly,
including a tail batch and optimizer resume; its four1M/1epoch arms are COMPLETE.
Neither prospective advancement gate passed. Do not repeat completed work.

raw/r011-objective-runner.py and raw/r011-support-runner.py preserve exact prior
runner bytes. raw/r012-objective-runner.py preserves the R012 training runner;
current dispatch includes AnchorCoverageDiagnostic. Future modifications must
preserve the R013 binding before editing. Raw states/receipts are the resume
identity,not the supervisor's historical NNUE latest_checkpoint field. R014 is a
new neural architectural hypothesis with its own correctness/cost gates.

R014 complete: four tests plus independent full-development Java/Python parity;
raw/r014-runtime retains trained executable. See r014-results.json for49.79s serial
training,failed global-context gate,ablations and immutable optimizer hashes.
R015 uses the same frozen runtime and existing R007 labels,not new production code.

R015/R016 complete:neural CP64 target benefit confirmed on a fresh8192-row BRN
panel;global-context composition gate still fails. All eight optimizer states,
predictions and selection keys are preserved. r016-results.json records source
and class parity,descriptive paired-position uncertainty and input limitations.
Future data expansion must purge R016 aliases;its source/index/alias checkpoint is
raw/r016-data/selection.json. R010 itself remains unchanged and owner-paused.

R017 complete:four tests,all64 remote witnesses with empty squares,four immutable
one-epoch checkpoints;positive but below-gate prediction gains. R018 resumes only
R015 LOCAL control checkpoints to calibrate bounded neural optimization depth.
Historical runner bytes:raw/r015-objective-runner.py and raw/r016-prediction-runner.py.

R018 complete:exact continuation test and unchanged neural/updater class parity;
every epoch2-8 state retained. Fixed endpoint fails5% teacher gain;no best-epoch
selection or further same-recipe allocation. Neural curve alone does not separate
capacity from constant-rate optimization limits. New channel composition is R019.

R019 complete:exact fixed-transform control parity and4tests;two active learned
MIX transforms fail2%teacher gate. All source/runtime/state hashes remain pinned.
raw/r018-objective-runner.py preserves the prior runner before R019 dispatch.

R020 complete:three tests,exact CP64 symmetry audit,two canonical pilots failing
predictive gates. Raw audit and canonical runtimes are distinct and immutable.
No failed arm is repeated or promoted. R021 tests a single optimizer mechanism.

R021 complete:two optimizer tests and unchanged R017 inference-class parity;both
dense pilots fail quality gate. Preserve optimizer-mode states and all raw receipts.
Next R022 adds a distinct learned composition across controlled squares.

R022 complete:four tests and two full checkpoints;useful second path but below
fixed improvement gate. R023 reviews accumulated evidence and isolates width.
No previous experiment is reclassified or repeated.

R023 complete:four tests and two width16 checkpoints;training fit improved but
quality gate failed. No scaling. R024 reuses frozen endpoints and R016 panel.

R024 complete:one routing test,byte-identical model classes,8192-row predictions
and exact R016 target parity. R025 is a separate playing-calibration protocol,
not promotion or reversal of prior loss gates. No new training is queued.

R025 integration passes21 focused tests;initial test SearchRequest package error
is preserved and corrected. Build78 accompanies qualifying core extension. Freeze
clean-source runtime and verify final-tested class manifest before fixed-node play.

R025 complete:clean build78 runtime,21 focused checks,32 deterministic probe
requests and32 legally replayed games. Neural point estimates trail CP64 strongly.
All children released;no clock/GPU work. R026 diagnoses saved positions only.

R026 complete:991 played roots,eight fixed endpoints,byte-identical scorer classes
and descriptive calibration analysis. R027 admits only a training-fitted scalar;
no new games or neural-weight updates. Historical audit runner preserved at
raw/r024-prediction-runner.py.
