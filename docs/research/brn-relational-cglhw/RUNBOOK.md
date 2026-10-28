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
