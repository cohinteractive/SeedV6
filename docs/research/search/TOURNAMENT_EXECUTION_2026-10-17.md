# Tournament execution efficiency — programme maintenance

17 October 2026 (Pacific/Auckland). Authoritative repository:
`C:/projects/seed/java/seedv6-unified`. Starting HEAD
`18ddd6ece499ab4304a65aeed774992b60196bac`, clean worktree; Search masters
P2-R037/P2-F041. SR-021 is **owner-accepted DEFERRED without adoption**.
No feature is ACTIVE, no successor is selected, SHALLOW4 remains closed to
automatic continuation, and Mac migration is deferred. Both final Project Source
mirrors remain an external handback action; intermediate refresh is not required.

**Disposition: technically complete, pending GPT/owner reconciliation.** Safe
bounded sessions, forecasting and a measured higher-throughput option are
available. The established confirmation concurrency is retained because the
larger concurrency settings show plausible policy-dependent contention; a speed
gain is not certification of an equivalent strength-measurement domain.

## Prospective scope and acceptance

Operational/exploratory infrastructure evidence only; no strength confirmation,
new Search policy, clock-standard change, or retrospective experiment edit.
Reuse Fastchess, existing match adapters, legal/history/telemetry PGN audit and
Windows process accounting. Calibrate current production QXRAY at 10+0.1,
principally four workers at game concurrency 1/2/3. Use short balanced repeats;
add concurrency 4 only if useful. Keep disruptive calibration below four hours.
Retain one-worker concurrency 4 unless evidence establishes a reason to change.

Acceptance before runner changes: frozen unique opening/color schedule; full
legal/result/request audit; exactly-once verified block accounting; clean bounded
pause/resume; changed-input rejection including added classpath entries; exclusive
execution ownership; owned process-tree cleanup; preserved interrupted evidence;
no strength assessment at an operational boundary; usable forecasts with explicit
uncertainty. Demonstrate real-engine session continuation and deliberate failure
handling plus focused fault tests. A completed-block recovery may adopt verified
existing output but may never rerun it. A partial/abnormal block blocks automatic
continuation; no outcome-dependent removal or replacement is permitted.

## Version provenance

This work proceeds under **DEFERRED_FINALIZATION**, independently of intact prior
operation `ebe0487f7c214b398fc2625a8ac053fa`. Local begin rejected its retained
`integrity_failure: Nested repositories are unsupported.` No nested checkout
currently remains; WU7-PACKAGING already records relocation of the old validation
metadata. The prior reservation, baseline and failed attempt are preserved. No
new token was created, no finish/bump is claimed, and build 35 stays unchanged.
This work does not repair the finalizer or attribute maintenance to the old token.
Finalized-version-dependent operations remain blocked by that separate issue.

## Evidence

Local raw outputs belong beneath
`docs/research/search/tournament-execution-2026-10-17/raw/`; raw output is ignored,
preserved and unavailable in a fresh checkout. Historical SR-024/SR-021 records
retain their original repository paths and frozen identities.

## Throughput evidence and interpretation

The original SR-024 passed run reports are available in this checkout. Recomputing
their totals gives 4,096 games / 2,048 pairs in 29,181.054 match seconds at one
worker/concurrency 4: **505.31 games or 252.66 pairs/hour**. Four workers/concurrency
2 gives 1,536 games / 768 pairs in 21,077.651 seconds: **262.34 games or 131.17
pairs/hour**. These are historical report verification, not a new PGN or runtime
recertification. Original report paths, identities and outcomes are untouched.
At 262.34 games/hour, an additional 3,072 games predicts **11.71 hours**; the old
duration concern does not itself establish a regression.

New calibration uses current accepted QXRAY against itself, generation-32 Pair 2,
Java 21, Ryzen 5 5500 (6 physical/12 logical cores), 10+0.1, no pondering/book/
adjudication, equal workers/heaps/JVM CPU ergonomics and twelve fixed development
openings per setting. Each setting has two six-pair blocks, both colors, with
fresh JVMs per block. The main order is C2a/C1a/C3a/C3b/C1b/C2b, followed by native
C4a/C4b. A prospectively documented C4 four-worker extension follows the clear
C3 throughput improvement. All **120 calibration games / 60 pairs** pass full
legal/result/telemetry and lifecycle audits, with identical opening schedules.
Each setting has only 24 games / 12 pairs: this is operational/exploratory evidence.

| Search workers | Concurrent games | Valid games/hour (match) | Complete pairs/hour (match) | Games/hour (whole session) | Mean engine logical-CPU equivalents |
| ---: | ---: | ---: | ---: | ---: | ---: |
| 4 | 1 | 136.06 | 68.03 | 134.27 | 1.78 |
| 4 | 2 | 237.81 | 118.90 | 232.42 | 3.43 |
| 4 | 3 | 318.97 | 159.49 | 309.14 | 4.80 |
| 4 | 4 | 453.88 | 226.94 | 434.35 | 6.81 |
| 1 | 4 | 449.76 | 224.88 | 430.47 | 3.81 |

Four-game/four-worker throughput is **1.91× match** and **1.87× whole-session**
throughput versus the matched two-game pilot, with about **1.98× engine CPU use**.
It is a wall-time/resource tradeoff, not a comparable reduction in total CPU cost.
Its two block rates are 444.19 and 464.02 games/hour. The three-game rates are
317.61 and 320.36; its aggregate gain versus two games is 1.34×. Do not claim the
historical 262-to-new-454 comparison as a controlled speedup: the historical
comparators, much larger samples, openings and conditions differ. Likewise the
new one-worker rate does not demonstrate a slowdown from the historical 505.

Whole-session rates include preflight, startup, input checks, block drain and
audit; overhead is roughly four seconds per small block. Longer prospectively
fixed blocks can amortize that cost. The pilots do not establish sustained
overnight thermal/memory behavior or population confidence bounds. Game lengths,
clocks and non-deterministic multiworker trees vary; identical outcomes are not
required. No calibration result is pooled into independent strength evidence.

Per-process lifetime CPU includes JVM startup/JIT/GC and aborted Search work for
the game engines. The calibration's engine-CPU totals exclude Python/Fastchess
and preflight CPU; the final executor additionally records total owned Job CPU
including those processes. CPU equivalents are logical scheduling time, not
physical-core occupancy. Candidate/baseline A/A lifetime CPU ratios span
0.958–1.027; that is not evidence of neutrality for different search policies.

Sampled free memory remains at least 9.93 GiB. No sampled page-outs occur, but
page-ins are observed, including a 4,423-pages/s endpoint in C1 and smaller counts
elsewhere; absence of paging throughout a match is not claimed. These are sparse
host observations, not per-engine paging attribution. Maximum observed game-JVM
GC pauses rise from about 4.5 ms in C1 to 11.6 ms in C3 and 29.3 ms in C4; native
one-worker C4 reaches 15.4 ms. No time loss, crash or abnormal termination occurs
in the completed calibration. Small samples do not rule out rare failures or
candidate-dependent GC/contention effects. Actual interactive PC responsiveness
and temperature are not measured.

The fixed-node diagnostic reuses the existing SR-024 cost tool, generalized from
two to 2/3/4 concurrent four-worker engines while preserving the old default and
old output keys at concurrency 2. Eight roots, four rotated repetitions, three
one-million-node warmups per engine and 300,000-node requests compare accepted
QXRAY and the retained SEE-OFF control. These are homogeneous control groups;
they probe plausible asymmetry but are not mixed-policy strength trials.

| Concurrent four-worker searches | SEE-OFF latency / serial | QXRAY latency / serial | Relative QXRAY/OFF slowdown | OFF / QXRAY group speedup |
| ---: | ---: | ---: | ---: | ---: |
| 2 | 1.090 | 1.069 | 0.982 | 1.774 / 1.813 |
| 3 | 1.135 | 1.215 | 1.071 | 2.484 / 2.327 |
| 4 | 1.297 | 1.387 | 1.069 | 2.869 / 2.657 |

These 1,152 fixed-node requests (plus warmups) establish real contention, with
QXRAY about 7% more slowed relative to SEE OFF at C3/C4 in this diagnostic. They
do not prove a particular Elo bias: root/tree behavior, homogeneous groups,
limited positions and background variation remain qualifications. A/A alone
cannot resolve that issue. Accordingly:

- **Retain one-worker concurrency 4**, supported by prior long evidence and the
  new integrity pilot.
- **Retain four-worker concurrency 2 as the conservative confirmation setting**
  for continuity with established evidence. Multi-session execution removes the
  need to force its full sample into one disruptive 12-hour run.
- **Concurrency 4 is the measured high-throughput option** for operational work
  and fresh comparisons whose prospective protocol explicitly defines that
  resource domain and investigates relevant candidate/comparator contention.
  It is not an automatic programme-wide confirmation-default change or permission
  to mix C2/C4 observations. Concurrency 3 offers lower measured CPU use, but its
  similar differential slowdown does not establish a generally safer replacement.

For a fresh justified C4 domain, **400 whole-session games/hour** is a cautious
initial planning rate below the observed 434, with an additional explicit 25%
slowdown scenario. At 3,072 games this means 7.68 central / 9.60 planning hours;
two configured 6.5-hour windows with 0.3-hour reserves can accommodate either.
This is a forecast, not a promise or a sample-size/stopping rule. C2 studies
continue to use their relevant historical/observed rates and larger session count.

## Implementation and continuation policy

`tools/tournament-session.py` wraps the existing `sr021-match.py` adapter, which
uses accepted QXRAY production when both futility policies are `off`. This does
not launch an SR-021 feature experiment. Fastchess still owns legal game play,
clocks and reverse colors; `search-baseline.py` retains the independent
legal/history/result/request auditor. `sr024-process-cost.py` retains native
engine-lifetime CPU accounting. Existing default runner behavior is preserved.
The operational flag disables periodic Fastchess ratings and Python paired
strength statistics while retaining raw outcomes required for later scheduled
analysis and audit. Fastchess still prints its unavoidable final native summary
to the raw runner log; the session controller does not read it or use it to
decide continuation. No inferential assessor is called at block/session ends.
GC logs are enabled for operational resource evidence. Current research-runtime
identity now includes the launcher marker and adapter classes as well as JARs.

`prepare` creates a new frozen stage, copies both installations, Fastchess and
the executor/adapter dependencies, partitions a unique opening schedule into
prospectively fixed blocks, and binds every input file. It additionally hashes
the checkpoint directory, full JDK directory, Python executable and chess auditor
package. Added or removed input/classpath files are rejected, as are changed
configuration, host/Python domain or optimized Python with disabled assertions.
The plan is bound by a separate identity record. Paths inside the frozen study
are absolute: moving a started study or changing its inputs is unsupported.

Each block starts fresh engine processes, even when several blocks run in one
session. JVM restart/warm-up boundaries therefore do not depend on when the
operator pauses. Blocks are operational units, not statistical looks. Use a
prospectively chosen block size large enough to amortize startup and small enough
to fit the intended window and watchdog. Both colors of every assigned opening
must finish normally; all game moves, results, telemetry and process exits are
audited before an atomic completion receipt is written. Resume rehashes inputs
and all completed output, reaudits each block and checks contiguous progress.
Receipts bind the original pair identities and raw files. It never skips a
completed block based on its score or reruns a completed block.

An OS file lock excludes competing invocations of the same study; its inert file
does not require deletion after a crash. `tournament-process.py` assigns a gated
worker to a Windows kill-on-close Job before allowing it to launch anything.
Creation is suspended until assignment, which also closes the Windows virtual-
environment launcher race; then the initial thread and explicit stdin gate are
released. A fault test delays assignment and verifies no child code can run first.
The Job owns that worker, Fastchess and its engines. Timeout, exception or abrupt
parent death closes that owned tree without image-name killing or attaching to
recycled PIDs. Normal completion requires zero remaining owned descendants.
Different studies must still be scheduled without unwanted resource competition;
this is not a global scheduler or an exclusive reservation of the workstation.

The final executor also records whole-Job CPU and total process count. Its two
real validation blocks each owned 12 processes (including launchers, preflight,
observer and game processes) and closed with zero owned processes remaining.

If output completed and passed its full audit before the outer receipt was
written, reconciliation can adopt that existing output exactly once. If a block
is partial, abnormal, has failed lifecycle/identity evidence, or was launched
without a complete output record, continuation **blocks**. Every raw record,
including completed games and incomplete pairs, stays preserved. There is no
automatic replacement, exclusion, inferred result or partial-pair reconstruction.
Investigation and a separately authorized prospective disposition are required.
This deliberately limited recovery is preferable to pretending that a forced
shutdown is an ordinary pause. It does not reopen the old SHALLOW4 study.

The installed Fastchess is alpha 1.8.2, Windows CI build
`20260726-f618e34`. Its actual help advertises `-config`, `-recover` and per-game
autosave. Existing Seed wrappers explicitly pass `-autosaveinterval 0`, use fresh
output directories and assess full stages; they do not implement durable
partial-pair reconciliation or input/ownership validation for native config
resume. Native crash recovery/config resume is **not qualified or enabled** by
this maintenance. The demonstrated continuation method is the frozen pair-block
wrapper, not a claim that Fastchess native resume meets the Search Contract.

Inspection of the matching upstream commit
[`f618e34540f94f4719ad3817950618dabe441318`](https://github.com/Disservin/fastchess/tree/f618e34540f94f4719ad3817950618dabe441318)
finds that `BaseTournament::saveJson` writes configuration, engines and aggregate
scoreboard statistics; `RoundRobin` reconstructs its scheduler from completed
match count. `TournamentSchedulerBase` initializes round/pair counters from that
count and resets games-within-pair to zero. This is not an inventory of verified
opening/color records. **Inference from source:** arbitrary concurrent completion
holes, odd completed counts and a separately written/appended PGN need additional
reconciliation to meet exactly-once requirements; native config loading alone
cannot certify them. No native-resume runtime qualification is claimed. Source
URLs/hashes and local downloaded files are retained in the evidence manifest.

`export` is available only after the entire planned stage passes. It concatenates
the original PGNs, reaudits all scheduled opening/color identities and publishes
a completion manifest last. An interrupted export lacks that manifest. The export
contains auditable outcomes but no strength assessment. Existing frozen SR-024
protocols/executors are unchanged and must not be retrofitted to these blocks.
For a new study, the prospective stage controller/analyst may feed the complete
stage's audited pair outcomes to the existing statistical functions at its
declared looks; this tool does not choose looks, spend alpha, decide continuation,
or automatically launch a subsequent stage. It is not a drop-in historical
`run-sr024-confirmation.py` protocol migration.

## Windows operating procedure

From the unified root, prepare the explicit research composition if needed:

```powershell
$env:JAVA_HOME = 'C:/java/jdk-21'
$env:GRADLE_USER_HOME = "$PWD/build/maintenance-gradle-home"
./gradlew.bat :installResearchWorkspace --max-workers=2 --no-daemon
python -m venv build/research/venv
& build/research/venv/Scripts/python.exe -m pip install chess==1.11.2
```

Supply a verified Fastchess executable at an explicitly chosen local path; this
task copied the existing binary byte-for-byte into `build/research/tools/`.
Fresh checkouts do not contain that ignored executable or the external model
store. No future command needs to run against the old Seed repository. Match
inputs use `build/research/runtime`, not a production application distribution,
because the latter deliberately rejects research selectors.

A new stage configuration uses the structure of the tracked calibration configs:
`purpose`, a prospective `assessmentPlan`, `blockPairs`, `blockLimitSeconds`, and
`arguments` for baseline/candidate installations, Java, Fastchess, checkpoint
store, frozen openings/pairs, policies, equal worker counts, independent-game
concurrency, time control and seed. Relative input paths resolve against the
configuration file's parent, not an assumed old checkout. The example calibration
configs contain actual absolute paths as experimental provenance; create a new
config for a newly authorized study rather than editing their frozen identity.
`purpose: confirmatory` additionally requires an `assessmentProtocol` file, which
is copied and frozen with the stage. This is the separately approved prospective
sampling/independence/look/multiplicity design; the session runner does not parse
its statistical rules or establish that the chosen openings are fresh evidence.

```powershell
$py = "$PWD/build/research/venv/Scripts/python.exe"
& $py -B tools/tournament-session.py prepare --config <new-config.json> --out <new-study>
& $py -B <new-study>/inputs/tools/tournament-session.py run --plan <new-study>/plan.json --stop-before <ISO-timestamp-with-offset>
```

Use the **frozen executor inside the study** for every later action. Each run
needs a real `--stop-before`; optional `--start-after` waits for a configured start
inside that window. `--max-blocks` can bound a pilot/session further. No Task
Scheduler entry, daemon, automatic next-night launch or assumed sleep hours are
created. Invoke the next authorized window explicitly or through an owner-chosen
external scheduling arrangement; the study remains unchanged across invocations.

`pause --plan ...` requests a stop after the current full block. A subsequent
`run ... --clear-pause` explicitly clears that request after successful preflight.
`status --plan ...` verifies and reconciles progress without strength inference;
it requires the execution lock and therefore does not inspect a running study.
`export --plan ... --out <new-export>` publishes only a complete stage. Never
delete start markers, receipts or output to force a retry. A blocked partial
block cannot be resumed by passing a larger window or clearing PAUSE.

The executor starts a new block only when its entire frozen watchdog plus a
30-second cleanup allowance fits before the end. Hashing/audit overhead is
additional and can finish after engine shutdown; long-window planning must
include it. Choose a block timeout from representative observations with generous
margin. Check normal host load, free memory and other workloads before play;
the host identity check cannot enforce identical background contention or power
management. Memory/paging samples and GC/CPU records describe observations, not
exclusive CPU isolation. Do not run unrelated heavy validation during research.

Forecast without launching games:

```powershell
& $py -B tools/tournament-session.py forecast --pairs 1536 --games-per-hour 262.344 --session-hours 6.5 --reserve-hours .3 --slowdown 1.25
```

This reports games/pairs, play hours and session count under a stated slowdown
scenario. The allowance is not a confidence interval and does not choose sample
size. At the historical four-worker rate, 3,072 games need about 11.71 match hours;
two 6.5-hour windows can fit the central estimate with reserves, while a 25%
slower scenario needs about 14.64 play hours and a third window. Two exactly
six-hour windows are tight once reserves and overhead are included. A roughly
12-hour study can responsibly span sessions when these arrangements are fixed
before play; neither an exact two-night guarantee nor interim score-driven
extension follows. The original inferential sample/looks remain unchanged.

## Validation and preservation

The research workspace built successfully with
`:installResearchWorkspace --max-workers=2 --no-daemon` and Java 21. The current
JDK reports OpenJDK 21 (2023-09-19), matching the historical reports. The copied
Fastchess executable matches both its retained source and the original SR-024
frozen binary hash. No historical installation, source snapshot or protocol was
rebuilt or rewritten.

Focused tests: **52 pass** — 22 session/process fault tests, 18 existing baseline
auditor/selection tests, four statistical-rule tests, five frozen-confirmation
executor tests and three research-path/legacy-command compatibility tests.
Coverage includes exact color/pair accounting, duplicate/missing/unknown games,
changed binaries/model/adapter/configuration and added files, immutable completed
evidence, exclusive locks, ambiguous launches, normal/abnormal process exit,
completed-output receipt recovery, full-stage export, disabled operational
statistics, insufficient windows/PAUSE and prospective-protocol admission.
Real Windows tests cover normal exit, watchdog cleanup, abrupt owner death and
delayed Job assignment. Review found and fixed the virtual-environment launcher
race by creating the root worker suspended; no running tournament was interrupted
for that repair. A waiting continuation helper was replaced before launching work.

Final review adds a second window/PAUSE check after full-tree hashing, so slow
preflight cannot consume the launch margin unnoticed. The session/process suite
passes again, including simulated preflight delay and a pause arriving during
hashing. The real-engine evidence below predates only this admission guard;
its exact frozen executor is preserved in the tool snapshots. Game execution,
pair accounting and process containment are unchanged by that final guard.

The final real-engine study executes two frozen two-pair blocks at four workers /
concurrency 2 / 10+0.1, in separate sessions: **8 valid games / 4 complete pairs**.
An explicit PAUSE prevents the second block from launching; clearing it resumes
only that block. The exported PGN is independently reaudited against all four
original opening identities, once per color. Incomplete-stage export is rejected,
and the complete manifest is written only after successful full-stage validation.
Operational statistics remain disabled. Outcomes are not compared for equality
with independent clocked runs.

A separate, prospectively declared fault test kills the session owner after the
first PGN record. Retained native handles verify all four game engines exit and
an unrelated sentinel process survives. The partial PGN contains **one completed
normal game, zero complete pairs and one incomplete pair**. Status, run/resume
and export all reject continuation; no block receipt or stage export is created,
and every original partial-output byte remains unchanged. This validates safe
refusal and preservation, not automatic salvage of that pair. Completed-output
receipt recovery is separately exercised by the fault tests without replay.

All calibration/live execution is bounded and sequential. It consumes well under
one hour of live tournament/diagnostic/validation time, comfortably below the
four-hour budget. No substantial unattended overnight work, strength confirmation,
SR-021 restart or successor experiment runs. Final process inspection finds no
remaining task Fastchess or research-actor JVM. JVM/GC/CPU monitoring is passive;
unrelated applications are not terminated or retuned.

Skipped deliberately: full application/Search/GUI/training suites, browser/native
client QA, Mac/remote execution, release/deployment, a long overnight endurance
run, native Fastchess partial-resume qualification and independent A/B strength
equivalence across resource domains. None is claimed by the operational pilots.
The task changes Python research tooling and canon only; protected production
and research Java sources, model inputs and all frozen Phase 1 archive bytes are
unchanged. All 48 SR-prefixed Frontier table rows (including repeated summary
rows) are byte-identical; no feature ID/status changes or successor activation.

## Relocation, evidence locations and handback

Active operational scans of tracked `tools/`, `source/` and current top-level
guides find no literal old-root references needing replacement. WU6 had already
migrated the explicit research composition. This task supplies verified local
`build/research/runtime`, `build/research/tools/fastchess.exe` and
`build/research/venv` paths, config-relative input resolution, and complete
research-adapter binary identity. Both active masters now name the unified root.
Current launch/preflight/game execution verifies these paths in practice.

Historical reports, frozen protocols and the adoption-time monolithic-`app.jar`
integration verifier retain their legitimate original artifact assumptions; they
are not new unified-runtime launch defaults. Old-root provenance in those files,
the migration archive descriptions and the runner-copy origin is deliberately
preserved. All five original confirmation PGNs are absent from this unified
checkout; their passed reports remain available, and their original local evidence
paths are retained. No task command writes to the old repository, and the copied
source executable's bytes remain unchanged. This does not claim that unrelated
IDE activity could not affect either checkout's generated caches.

Detailed tracked evidence is under
[`tournament-execution-2026-10-17/`](tournament-execution-2026-10-17/):

- `historical-throughput.json`, `calibration-summary.json`, `contention-summary.json`
  and `forecasts.json`: verified rates, resource measurements, limitations and
  scheduling examples.
- Calibration/validation configs, opening fixtures, driver/execution records,
  `inputs.json` and `tool-snapshots.json`: identities and reproduction inputs.
  Distinct earlier executor sources are preserved as `.py.txt` evidence inputs;
  they are not additional active runners. Unlaunched preparation copies stay
  local and are excluded from measurements.
- `live-validation.json`, `interruption-accounting.json`, `tests.json`,
  `preservation.json`, `active-path-audit.json`, `runner-origin.json` and
  `fastchess-source-review.json`: acceptance checks, provenance and precise limits.
- `raw/`: ignored, preserved PGNs, logs, frozen runtime/input bundles, complete
  plans/receipts, GC/process/host data, source inspection and interrupted output.
  A fresh checkout does not contain this local evidence. New opening fixtures
  use the existing narrow `*-openings.pgn` exception; no ignore policy changes
  or force-adds of raw output are made.

Application version remains 0.0.0 build 35 with the separate, intact finalization
blocker described above; no successful finalization, terminal warning receipt or
bump is claimed. No root CODEXLOG_CURRENT.md exists or is created. Attributable
Git closeout includes only this maintenance; there is no push, release or deploy.

CANON_UPDATED: P2-R037 -> P2-R038

FRONTIER_UPDATED: P2-F041 -> P2-F042

PROJECT_SOURCE_REFRESH_REQUIRED: YES

Human actions required after this prompt:

- **Blocking subsequent feature planning/launch:** GPT/owner reconciliation of
  this maintenance, then owner replacement and confirmation of BOTH Project
  Sources from `source/CHESS_SEARCH_CONTRACT.md` **P2-R038** and
  `source/CHESS_SEARCH_RESEARCH_FRONTIER.md` **P2-F042**. Intermediate P2-R037/
  P2-F041 synchronization is superseded. GPT/owner selects any next feature.
- **Non-blocking for this maintenance:** the owner separately updates external
  Project Instructions to `C:/projects/seed/java/seedv6-unified`; Codex does not
  perform or claim this external update.
- **Blocking only dependent finalized-version/provenance operations:** separately
  authorize supported reconciliation of the pre-existing finalizer operation.
  It does not block use of this verified research method under the existing
  deferred-finalization authority.

No additional infrastructure approval or repair is needed to use the method
within a newly authorized study with a frozen design and actual execution
windows. Higher concurrency remains subject to the prospective resource-domain
and comparability qualifications above. This closeout is not owner acceptance,
external mirror synchronization or authorization to select/start the next feature.
