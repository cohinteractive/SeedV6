# CLV-C1 rolling production adoption

Revision 1, 7 November 2026 (Pacific/Auckland). Owner-authorized integration,
separate from the accepted unsuccessful CLV-R001 research closeout at `25d7d182`.
Starting repository was clean `f2ba5eaf`, build99, P4-R035/P4-F053; no root journal.
Source integration and committed Windows headless artifact qualification are complete.

## Decision and scope

The owner accepts the technically completed, unsuccessful strength-development
outcome and separately selects frozen **CLV-R001 C1/build99** for normal rolling
production across W1-W16, GUI Play, built-in Arena and UCI. Research is formally
closed with its original objective **UNMET**. No separate GPT reconciliation or
owner acceptance of the integrated artifacts is invented. CONTRACT revision 3 and
STATE record that disposition; RESULTS, protocols, budget and frozen evidence retain
their bytes and conclusions.

The actual P4-R035 Contract already contains the applicable **Rolling experimental
production baseline** rule: one common applicable feature policy across W1-W16,
owner adoption despite explicitly accepted competitive uncertainty, mandatory
correctness/safety, distinct historical baseline/champion/production identities.
The incremental-adoption section also separates unsuccessful strength development
from adoption. No missing-policy discrepancy or new automatic-adoption policy was
found. No behavioral registry or CEC changes are made.

## Frozen and integrated identities

Frozen candidate: **46d1543121f9bc13e7c2a4832efa78bd14b5363c/build99**,
`candidate-build99-manifest.json`, research identity `CLV-R001-C1-v1` via
`ClvUciMain --formulation C1`. Core SHA-256:
`9d7cf507fdecd7b7aaad07ff35c1bb54ed7daecd5034cb3e31467eb4a5c85ea4`.
Its W4 confirmation protocol remains `c1-confirm-w4-v2`, against original
**44fc2bf2fe54e87dee7721bdaa451f3b00f3b1d0/build96**, core SHA-256
`3211f4f375d667f3f442a21d88d8b9755d178ea6a261f0fee74f8ffe84bf84ea`.
All 45 pinned C1 runtime files and the original96 core were locally available and
rehashed before integration. Historical build79/source75336761 and the W1-qualified
HL-G1/build84 champion remain distinct and unchanged.

Integrated production: **d71ec4071640916b3821fb7d24a7476e1e0fac57 / build100 /
CLV_C1**, flags **3329490943**, selected without JVM
properties by `BaselineEngine.SearchProfile.PRODUCTION_DEFAULT`. This is a new
production source/build identity, not a replacement or relabelling of frozen99.
The source commit and final artifact hashes are recorded in
[production qualification](production-qualification.json).

| Workers | Applicable CP64 feature policy | Execution |
| --- | --- | --- |
| W1 | CLV_C1, flags3329490943 | OWNER |
| W2-W16 | CLV_C1, flags3329490943 | EAGER_ROOT |

Frozen C1 adds `TT_CRITICAL_RELIEF` to exact unified MW-C4. At interior null-window
nodes of depth at least four, current-generation ordinary exact/lower TT bounds
from one or two plies shallower that reach beta can reduce otherwise-positive
quiet LMR by one ply, after existing adjustments. The hint must be legal. Root,
wide-window, decisive-score, synthetic-null and verification domains are excluded.
It consumes node-level evidence; the stored score and hint need not describe the
same completed move. It is not singular verification or an exhaustive proof.

There is no W4-only condition in that mechanism. W1 has the same consumer and TT
policy in its owner kernel; every parallel worker receives the same flags. Existing
eager finite-window scheduling, shallow/full-window owner-first behavior, private
learning/state and synchronized shared TT remain. ExactSearch, ParallelSearch,
ConstructionSearch, FlatSearchConfig, TTable, ClvStudy and ClvUciMain source bytes
match frozen C1. No kernel adaptation, parameter tuning or BOUNDARY2 redesign is
needed. Integration changes factory selection, truthful formulation descriptions
and packaging expectations only; nodes and moves need not match across workers.

CP64 remains checkpoint64/S6CNU002/eight heads/BT4_Q_LOGIT400_V1, file SHA-256
`8431626c2fca81e0a1e9ccd5e45ddd8829e6e3282c756c21fad75b06d075bb2b`.
PVS/ADAPTIVE16, BOUNDARY2, requested hash allocation and score mapping are unchanged.
HCE/BREP2-32 and unrelated conventional payloads retain their calibrated routes.

## Consumers and source qualification

GUI Play, built-in Arena and UCI continue through one shared engine-core factory.
Loaded profile snapshots survive worker/hash/clock reconstruction. About and UCI
diagnostics name the actual formulation and flags; UCI retains `SeedV6-<build>` and
the `Show Search Configuration` button with reproducible configuration fingerprint.
Normal launches need no experimental property. No production-to-research dependency
or duplicate Search is introduced.

**319 selected JUnit checks passed**, zero failures/errors/skips: core215, UCI28,
focused GUI7, Arena1 and research/control68. The multi-domain Arena test includes
W1/W2/W3/W4/W5/W8/W16 and all three competitive evaluator routes, in bounded
temporary-store fixtures. Tests cover all W1-W16 selection/private-state/TT sharing,
representative actual Search, legal PVs, board/history restoration, repetition,
terminals/mate domains, request accounting, cancellation, reset/reuse, lifecycle,
concurrency, exact controls, loaded-profile reconstruction and rollback isolation.
Project dependency boundaries and research-workspace compilation passed.

The C1 consumption test executes four real-history roots with 250,000 nodes each
at W1/W2/W4/W8/W16. Observed eligible TT signals/actual reliefs were respectively
768/193, 895/294, 951/543, 1135/159 and 717/264. Each request reached its exact
node cap, retained a legal completed PV, restored input state and drained. A
separate W1 explicit frozen formulation matched traversal/results exactly. These
are technical witnesses, not decision-quality or strength evidence.

The first core run failed one expected-production fixture that still explicitly
constructed MW-C4. Adding C1 to that expected formulation resolved the failure;
the full215 core suite then passed. No kernel fix was necessary. The first package attempt also correctly refused publication when this production
record was authored during packaging (source snapshot changed); it is retained
and rerun with source editing paused. Initial failed
logs/XML remain alongside the passing evidence under ignored
`raw/production-adoption-v1/`. Raw data are local-only; check availability before
reuse. The qualification JSON identifies commands, summaries and evidence hashes.

## Precommit Windows artifacts

The second native GUI/UCI package attempt passed in61 seconds, with source editing
paused; the first source-stability refusal remains recorded. Both candidates share
core SHA-256 `ccea31b43fc362244bb81a7dd92f3f43ddc30d4ede3ed1cadcf3ed9d687e564e`.
These provisional packages embed build99/uncommitted-source metadata and are **not**
the frozen research build99. Final committed build100 packages are a separate step.
All13 kernel/TT/config class entries match frozen C1 byte-for-byte.

Java UCI passed168 actual search responses,56 each for default CLV_C1, explicit
MW_C4 and historical MW_C4_W4. Each profile covers W1-W16, Hash1/64, clock and
resource reconstruction, on-demand diagnostics, independently recomputed config
fingerprints, legal PVs, limits, stop/reset/reuse, repetition/mates, quit and EOF.
Packaged GUI Play passed77 headless EDT/service requests at W1/W2/W4/W8/W16,
including reconstruction, legal PV/state restoration, stop/reset, history, mate and
shutdown. Packaged Arena passed20 depth4/four-ply technical fixtures (one pair per
profile/worker for CLV_C1 and MW_C4 at W1/W2/W4/W8/W16), including actual telemetry,
persisted resume and helper draining. No strength score is interpreted.

Python packaging checks passed40 with one Windows symlink-privilege fixture skipped.
The default whitespace check flags preserved CRLF as trailing whitespace; the
CR-aware check passes without global Git configuration changes. Plain space/tab
trailing whitespace is checked separately. No runtime acceptance claim follows
from static inspection or source metadata alone.

## Committed build100 artifacts and final checks

Source commit **d71ec4071640916b3821fb7d24a7476e1e0fac57** contains the verified
single99->100 build-token increment, preserving user version fields and all other
version bytes. Clean-source `clv-c1-build100-v3` packaging passed in62 seconds,
using bundled Temurin21.0.12.1. Native GUI/UCI, copied images and separately extracted
archives passed headless resource/JNI/model/protocol checks in paths with spaces.

| Artifact | Repository-relative path |
| --- | --- |
| GUI image | `dist/windows/candidates/clv-c1-build100-v3/SeedV6-GUI/SeedV6-GUI.exe` |
| GUI ZIP | `dist/windows/candidates/clv-c1-build100-v3/SeedV6-GUI-windows-x64.zip` |
| UCI image | `releases/candidates/windows-x64/clv-c1-build100-v3/SeedV6-UCI/SeedV6-UCI.exe` |
| UCI ZIP | `releases/candidates/windows-x64/clv-c1-build100-v3/SeedV6-UCI-windows-x64.zip` |
| Java UCI | `app-uci/build/install/seedv6-uci/bin/seedv6-uci.bat` |

GUI, native UCI and generated Java UCI share core SHA-256
`3808b4d6c931d0780b16fa978edcbf4ae6542f640252fd2f55f95226aeb2460d`.
All3302 compared JAR class entries equal the tested precommit packages; only version,
source and desktop timestamp resources differ. The qualification manifest pins
archive/executable-package manifest hashes, runtime/source identities and commands.
No metadata-only full JUnit rerun is claimed.

- **Native UCI:**336 additional searches,56 per profile for default CLV_C1 and
  explicit MW_C4, MW_C4_W4, TC_P1, HL_G1 and LEGACY, each across W1-W16. All report
  SeedV6-100/source d71ec4071640 and the actual expected flags/schedule. Configuration
  fingerprints are independently recomputed from emitted binding identities.
  W4/Hash64/CP64/vector-256/ADAPTIVE16/EAGER_ROOT/BOUNDARY2 is `cfg-4cf89fcfafe20fc8`.
- **Packaged GUI Play:**231 additional headless EDT/service requests,77 each for
  CLV_C1, MW_C4 and TC_P1, covering W1/W2/W4/W8/W16 and return/reuse transitions.
  Actual search, legal moves/PVs, terminals/repetition/mate scores, exact restoration,
  depth/time/stop limits and shutdown drain passed. No interactive window was opened.
- **Packaged Arena:**20 additional depth4/four-ply fixtures across CLV_C1/MW_C4 and
  W1/W2/W4/W8/W16;60 actual telemetry snapshots, persisted completion/resume and
  helper drain passed. These are bounded technical checks, not competitive results.
- **Generated Java UCI:**56 final default-profile searches across W1-W16 passed the
  same identity, reconstruction, chess-rule, accounting and lifecycle gates.
- **Historical artifacts:**original79/source75336761 JAR hashes and its pinned Java
  hash match; original96 remains intact. Four actual depth3 searches at W1/W4 pass,
  retaining flags3407871 for79 and TC-P1-at-W1/MW-C4-at-W4 for96. Current profiles
  never substitute for these historical binaries.

Preparation limitations are retained honestly. The text-mode ignore-path preflight
added CR to Git input; its null-delimited replacement verified every new raw path
ignored. The ensuing build100-v1 was made before the source commit and is provisional.
Build100-v2 correctly refused a source snapshot changed by a probe-generated untracked
Python cache. That attributable cache was moved into ignored evidence, subsequent
probes used `-B`, and clean build100-v3 passed. Neither attempt caused a second
build increment or a change to Search. No failed validation remains unresolved.

Final evidence is a documentation-only successor commit: **no_bump**, strict
build100 bytes retained. Final masters remain **P4-R036/P4-F054**, revised once
for this work unit. Research/results/budget pins were rechecked unchanged after
qualification. No broader training suite, native non-Windows platform, interactive
GUI, installed external client or competitive campaign is represented as tested.

## Competitive limits and preserved controls

CLV's independent640 games remain **242W/191D/207L, 52.734375%, paired-normal95%
49.670255-55.798495%**. Both score>=53% and lower>50% gates failed. All90187 audited
requests and normal game completions stand; all1284 processes drained. Campaign
expenditure remains23274.421601200127 seconds and708 initiated games; the sole
confirmation attempt is spent. The adverse fixed-work depth observations and
supplementary game-order correction remain in the unchanged research evidence.
No timed competitive games, new experiment, continuation or successor are run here.

W4 competitive uncertainty is owner-accepted, not resolved by technical integration.
Other worker counts have no independent C1 strength qualification. Longer clocks,
warmed persistent clients, other platforms and other opponents remain unqualified.
No strength transfer, champion advance or successful foundational relabelling follows.

## Rollback and remaining boundaries

`-Dseedv6.searchProfile=MW_C4` restores the prior unified policy at every worker:
flags1182007295, W1 OWNER/W2-W16 EAGER_ROOT. `MW_C4_W4` preserves the historical96
selector (MW-C4 only at W4, TC-P1 elsewhere); it does not turn a current binary into
the exact historical build96 artifact. Explicit TC_P1, HL_G1 and LEGACY retain
their meanings, as do research CONTROL/C1 and independent exact controls. Use
the preserved original binaries for exact competitive reproduction.

For generated Java UCI use process-local `SEEDV6_UCI_OPTS`; for native GUI/UCI use
process-local `JAVA_TOOL_OPTIONS`, preserving other settings. Restart without the
override to restore normal CLV_C1; verify engine identity and configuration report.

Interactive GUI visual acceptance, installed-client behavior, macOS/non-Windows
native qualification, owner artifact acceptance and deployment remain separate.
No installation, release, push or deployment is performed. Refresh both Phase4
Project Source mirrors to P4-R036/P4-F054 before mirror-dependent coordination;
repository edits do not upload them. These are non-blocking for repository
integration and blocking only for the corresponding later acceptance/operational work.
