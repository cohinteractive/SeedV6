# Reproducing the learning-strength research

The [contract](../../brn/BRN_LEARNING_CONTRACT.md) defines the decision gates;
the [research record](BRN_LEARNING_RESEARCH.md) preserves their chronology,
negative results and revisions. The compact evidence directory is
`evidence/learning-2026-10-03`. Full per-ply traces, copied models, optimizer states
and datasets remain in ignored `app/build/research/brn-learning`, outside Git.
Keep those local raw artifacts when continuing this investigation.

## Build and measurement entrypoint

Run from the repository root with Java 21:

```powershell
$env:DEBUG=''
.\gradlew.bat :app:compileVerificationJava :app:installDist -Pheadless --no-configuration-cache
$learningCp='app/build/classes/java/verification;app/build/classes/java/main;app/build/resources/main;app/build/install/seedv6/lib/*'
& 'C:\java\jdk-21\bin\java.exe' -Xmx2g '-Djava.awt.headless=true' -cp $learningCp com.ohinteractive.seedv6.training.service.BrnLearningProbe <arguments>
```

Use the actual Java 21 installation on another machine. Every output directory
must be new. Existing output and original stores are never overwritten. Argument
forms below follow the main class; substitute explicit paths:

```text
diagnose MODEL OUT DATA [ROOTS=32]
mechanism MODEL TRAINING_STATE DATA MATCH_JSON OUT
match MODEL OUT PAIRS DEPTH RESIDUAL_GAIN OPENING_SEED FIRST_INDEX [CAP=512] [WORKERS=0]
threads MODEL DATA OUT
qsearch MODEL DATA OUT COUNT [FIRST=0] [MILLIS=5000]
continue TRAINING_STATE DATA OUT
archive WINDOWS_STORE OUT
```

`WORKERS=0` selects the one-thread reference adapter with a 4 MiB table. Positive
workers select `ProductionSearch` with the default 192 MiB tables. Gain `.25` on
that path uses ordinary `SearchEvaluation.brn3`; other gains are explicit research
controls. The opponent uses the same search construction with gain zero, exactly
Gen0 material. Both sides complete the requested depth or the game is incomplete.
Budgets are 10 seconds and 10 million entered nodes per move. A ply cap never
counts as a draw. These are fixed-depth comparisons, not equal-time Elo estimates.

Every match record binds the model SHA256, exact arguments, opening policy, seed,
depth, workers, table allocation, opening identities, both color outcomes and
complete-pair uncertainty. Older probe versions have a generic exploratory
`purpose` string even for protocol-defined confirmation; the frozen canon defines
the role of each run. Use a new output argument when reproducing a record.

## Key experiments

| Role | Model | Pairs/depth/workers | Seed | Gain/cap |
|---|---|---|---|---|
| Exploratory calibration contrast | Copied Windows Gen9 | 16/4/reference | 620391 | 1 and .25 / 1024 |
| Frozen confirmation | Windows Gen1, Gen5, Gen9 | 64/4/reference each | 831907 | .25 / 1024 |
| Independent source/seed confirmation | Prior programme seed97 production model | 64/4/reference | 831907 | .25 / 2048 |
| Prospective continuation | Each of two new endpoints from Gen9 | 64/4/reference each | 831907 | .25 / 2048 |
| Integrated deeper confirmation | Windows Gen9 | 32/6/12 production | 999013 | .25 / 2048 |

The first index is zero except the disclosed single-pair cap replay, index23.
The failed seed97 cap1024 attempt is preserved separately; it must not be combined
with the successful complete rerun. Shared openings allow matched comparisons;
they do not create independent observations across models. Pair bootstrap uses
10,000 replicates; each frozen/continuation gate also requires the conservative
one-sided bound `score - sqrt(log(20)/(2*pairs)) > .5`.

The prospective preparation used the existing `BrnResearchMain` entrypoint:

```text
prepare-uniform SOURCE NEW_DATA 262144 16384 1578888 ORIGINAL_WINDOWS_SEEK_DIRECTORY
```

The reader copies navigation metadata into the new dataset directory. The resulting
range is `[1578888,1953686)`, beyond the original Gen10 reservation. The manifest
contains source identity, eligibility, geometry grouping and split/payload hashes.
An earlier raw3000000 request failed the bounded navigation guard and is not used.
This is not a full source checksum or a claim of historical game-disjoint data.

`continue` resumes the copied Gen9 state at step73728, uses two consecutive
131072-example slices, eight epochs each, batch128 and unchanged masked Adam .003.
It serializes and reloads between endpoints. No moment reset or checkpoint
selection occurs. The new test partition is not used. The continuation report
binds both model/state outputs and raw/calibrated validation measurements.

## Software and evidence checks

The integration run selected these test classes:

```text
Brn3GuiTest, Brn3SearchCalibrationTest, NnueEvaluationStateTest,
NnueSearchIntegrationTest, GenerationRestartTest, Brn3CorpusTrainingTest,
Brn3ResearchParityTest, BrnLearningProbeTest, NnueCorpusTrainingTest
```

Invoke `:app:test -Pheadless --no-configuration-cache` with one
`--tests '*ClassName'` argument for each selected class. The broad run passed 38
tests. After adding/fixing the compatibility fixture, the final selected rerun of
`Brn3CorpusTrainingTest`, `Brn3SearchCalibrationTest` and `BrnLearningProbeTest`
passed seven tests, for 39 distinct successful test cases across both runs.
The compact `software-qa.json` retains XML hashes, names, timings and cases;
`brn3-network-tab.png` is the inspected headless Swing render.

Export compact evidence only after relevant experiments have finished:

```powershell
python tools/analyze-brn-learning.py app/build/research/brn-learning --export NEW_EVIDENCE_DIRECTORY
```

The exporter refuses an existing destination, independently checks pair scores,
verifies matched opening identities and the cap-replay prefix, computes paired
continuation differences, and records SHA256 hashes of source reports and outputs.
It retains incomplete runs explicitly. Large weights/corpora/per-ply traces are
not copied into Git. Full diagnostic, mechanism, qsearch, continuation, original
lineage and final preservation reports remain byte-for-byte evidence copies.

The current production gain is a code policy, not encoded in old model payloads.
For historical full-residual reproduction, use gain1 explicitly. Do not launch
ordinary training on an original store merely to inspect it, and never touch the
independent Mac control. The prior version-finalization reservation remains a
separate boundary; these research outputs do not finalize an application build.
