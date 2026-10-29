Current status: **R045 gate cleared by explicit owner RESUME29October2026**.
All saved bindings and CRC/step verified;prior supervisor active:null/owned0.
Fresh runner admission passed before its new attempt log was created. Active
operation dc19c21e09a14a6c8273e10d53cbf2fc resumes only the fixed remaining schedule;
STATE.md is authoritative for current work. The entire prior gate below remains
historical evidence. A new resource failure requires another clean stop,no retry.

---

# Current R045 resource pause - resume through STATE

**PAUSED_RESOURCE_GATE**,2026-10-29T00:54:02Z (29 October Auckland).
SystemCPU84.375% exceeded active80% ceiling;free16.598GiB passed memory guard.
The supervisor terminated only its owned Java/training children and completed
release at00:54:03Z. Verified active:null,owned_processes:0. No resource-availability
retry,queued training,GPU allocation or ongoing tournament. The runner's technical
FAILED status describes resource interruption,not a negative architecture result.

Six R045 correctness/dispatch tests pass;MIX full cost7.298us. Seed71 completed
1,048,576 examples/8,192 optimizer steps and all8 atomic training segments. Both
segment-8.state and epoch-1.state are byte-identical,CRC/mode/rate/step verified:
`raw/r045-mix-71/epoch-1.state`,SHA256
`019bf64d78c978784787a18143ef45a56c9b875840dc42703e888ca34a466485`.
Epoch-end metrics were interrupted before publication;no epoch1 quality result is
claimed. No epoch2 update or seed97 run started. r045-resource-gate.json pins every
segment/receipt/config/runtime/runner and the execution/release evidence.

**Required human action(blocking further empirical work):invoke RESUME when the
computer can accommodate one conservative CPU worker.** Admission must freshly
observeCPU<60%,free>=8GiB and existing supervisor permission. NORMAL alone is not
admission. No request for GPU,clocks,production promotion or other action.
If admission still fails,record the new gate and exit without waiting/retrying.

After instructions/Git/local artifact and frozen binding verification,run:

```powershell
.\nnue-control.cmd run --name brn-relational-mixed-tied-training-resume --resume-note 'R045 resume: preserve seed71 segments1-8, complete interrupted epoch1 scoring, then remaining fixed schedule; no tuning.' --log docs/research/brn-relational-cglhw/raw/r045-training-resume-1-launch.log -- build/research/venv/Scripts/python.exe -B tools/brn-relational/run_objective.py docs/research/brn-relational-cglhw/r045-plan.json
```

Use another unique log if already present;never overwrite old logs/bindings.
The runner performs fresh admission. It verifies/skips seed71 segments1-8 and
reuses epoch-1.state,recomputes only interrupted bounded epoch1 scoring,then
trains seed71 epoch2 and seed97 epochs1-2. One CPU BelowNormal,512MiB,max300s per
arm,5s active monitoring. All completed training updates survive;data loading,
preflight and partial endpoint scoring repeat. Do not repeat completed R012-R044
experiments or resume R010 Pair optimization. After both endpoints complete,
run analyze_mixed_tied.py under a new supervised admission,then reconcile R045's
unchanged BOTH-seed gates. No automatic extra fitting,games or promotion.

Reusable owner instruction:

> RESUME CGLHW seedv6-brn-relational-20261029 in C:\projects\seed\java\seedv6-unified.
> Start at docs/research/brn-relational-cglhw/STATE.md and obey its current gate.

The following earlier gate and its clearance remain historical evidence.

---

# Historical R012 resource pause — resume here after STATE

**CLEARED_BY_OWNER_RESUME on29 October2026**. The resource admission and pending
correctness retry passed; all4 tests passed. See r012-validation.json and STATE.md
for current work. The original gate/commands below are preserved as history and
must not be interpreted as a new request to rerun completed validation.

Original **PAUSED_RESOURCE_GATE**, observed2026-10-28T21:42:28Z (29 October Auckland).
The existing CPU admission probe measured65.6863%,above the strict<60% limit;
16.374GiB free memory passed the>=8GiB requirement. Gradle did not start.
No availability retry occurred. Supervisor active:null,owned_processes:0;
r012-resource-gate.json pins source,failed test,resource log and release receipt.
This is a resource pause,not programme completion or neural research failure.

Saved work: contract revision2/PRIORITY; completed R011 neural diagnosis and
symbolic audit; R010 SQLite checkpoint1,574,296 rows/cursor1,449,984; new R012
neural source/tests. R012 has **no trained checkpoint and no frozen pilot runtime**.
Its first build compiled and passed3 of4 tests. The finite-difference gradient
probe missed1e-10 tolerance by a1.256e-10 unary discrepancy at step1e-4. A smaller
step1e-5 is prepared with the same tolerance and diagnostic error reporting;
this is unverified,not a claimed correction. Preserve both attempts.

Required human action: **blocking — invoke the reusable RESUME when the computer
can accommodate conservative CPU work.** No GPU availability or clock authority
is requested. RESUME only rechecks capacity; if it still fails,record and exit
without sleeping/retrying. No other human action is required.

Exact next admissible operation after reading instructions,GIT and supervisor:

```powershell
$env:GRADLE_USER_HOME="$PWD/build/migration/gradle-user-home"
$env:JAVA_TOOL_OPTIONS='-XX:ActiveProcessorCount=1 -XX:+UseSerialGC'
.\nnue-control.cmd run --name brn-relational-anchor-neural-validation-resume --resume-note 'R012 pending gradient-step validation; no training until all tests pass.' --log docs/research/brn-relational-cglhw/raw/r012-build-3-launch.log -- build/research/venv/Scripts/python.exe -B -c "import sys,subprocess;sys.path.insert(0,'tools/brn-relational');from baseline import admit;print(admit(initial=True),flush=True);subprocess.run(['cmd.exe','/d','/c','gradlew.bat','--offline','--no-daemon','--max-workers=1',':research:test','--tests','*AnchorNeuralNetworkTest',':research:installResearch'],check=True,timeout=180)"
```

Use another unique log if that path already exists. Expected15–40s,one logical
CPU,BelowNormal inherited job,under2GiB working memory,no GPU. A stopped build can
rerun incrementally; no training progress is lost. Inspect actual test assertions;
do not weaken them to admit a model. If they pass,record54 focused passing tests,
freeze a new R012 runtime,wire explicitly allowed AnchorDiagnostic/audit dispatch
(preserving R011 runner bytes),freeze four configs and run capability audit before
the bounded neural training pilot. R012.md contains its unchanged prospective gate.
Do not rerun completed R011 controls or launch R010 Pair2 work.

R010's executed extension script is preserved as raw/r010-selection-runner.py and
maintained as tools/brn-relational/prepare_coverage_extension.py. The older131k
prepare_coverage.py is restored byte-for-byte; its completed experiment is intact.
The clone/recovery test passed against the identical extension function before
the filename separation; its updated import has not been rerun during this gate.

Reusable owner instruction:

> RESUME CGLHW seedv6-brn-relational-20261029 in C:\projects\seed\java\seedv6-unified.
> Start at docs/research/brn-relational-cglhw/STATE.md and obey its current gate.
