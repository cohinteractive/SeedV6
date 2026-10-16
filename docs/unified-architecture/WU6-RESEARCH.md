# WU6 research ownership and production isolation

16 October 2026 (Pacific/Auckland). Accepted WU5 prerequisite:
`3301d457d6f6eb3dad9bc4c0f2c7659c1fa7fcaf`. Work is confined to
`seedv6-unified`, branch `migration/unified-architecture`. Implementation and required headless validation are complete; GPT/user
reconciliation remains required. This record does not declare WU6 accepted or authorize WU7.

## Discovery and approved composition boundary

The starting worktree was clean, version 0.0.0 build 35. Research was a scaffold;
legacy app owned 144 verification Java files, one frozen evaluation corpus,
empirical runners/oracles under test, 55 research JavaExec registrations (including
the four generated SR-007 tasks), and experimental `UciEvaluation` configuration.
The two remaining app main sources were Main and UciEvaluation, not another engine.

Production core has no ambient research-property reads. Its optional Syzygy
configuration is unrelated and remains unchanged. Legacy UciEvaluation accepted
qsearch/LMR/aspiration/LMP/check/reuse/NMP/internal/concurrency/SEE/futility
properties; therefore a research property could select unaccepted behavior in
legacy production composition. Standalone UCI already rejected legacy/research
properties and selected ProductionSearch explicitly. GUI uses the same accepted
lifecycle factory and does not read research selectors.

Several mixed GUI/UCI/training tests use independent research references. Keeping
these in app after relocation would violate the existing all-configuration
project guard; moving them into research would require GUI/UCI/app dependencies.
UciEvaluation also implemented app-uci's UciConfiguration. Before relocation the
owner explicitly approved **root test/research composition only**. CONTRACT
records this narrow amendment. There is no sixth runtime project, root main
source set, root runtime JAR or new engine plugin mechanism.

## Ownership and dependency graph

Research now owns 212 main Java sources: the 144 former verification sources,
66 former test-hosted empirical/oracle helpers, historical UciEvaluation, and
Phase2Oracle extracted from the JUnit fixture. The one OPT5 frozen `.b64` corpus
moves byte-for-byte with its classpath name. Independent FlatNegamax,
FlatExactSearch, AlphaBetaPvsSearch/legacy coordinator, old TranspositionTable,
exhaustive/qsearch/SEE references, FrozenEvalReference/Tuning, BRN native and
scalar references remain independent implementations. They are not replacements
for the accepted production engine.

Most files retain bytes/packages. Phase2QuiescenceTest's RestrictedOracle moves
without algorithm changes into Phase2Oracle, shared by the test and empirical
runners. Its independent legal-PV check now throws AssertionError directly
instead of requiring JUnit from a main-source runner. The SR-021 workers runner
retains the exact original middle-position FEN formerly obtained from a test
constant. No search/evaluation/training math or experiment parameter changes.
Live source-provenance paths in BootstrapAudit, ArchitectureScreen and
ResidualTrainingScreen now name their real owners.

Research has 145 test sources (including a new ownership check). Root
integrationTest owns 131 mixed tests and shared fixtures; these are production
integration checks, not research implementations. Legacy app retains 92 test
sources, including a new production selector-rejection test. The three binpack
oracle fixture files move unchanged to root integration resources with their
original classpath paths. No new fixture ignore exception is needed.

```text
engine-core     -> none
engine-training -> engine-core
app-gui         -> engine-core, engine-training
app-uci         -> engine-core
research        -> engine-core, engine-training
app             -> engine-core, engine-training, app-gui, app-uci (temporary)
root integrationTest -> all six existing owners (test only)
root researchLauncher -> engine-core, research, app-uci (explicit research only)
```

Research declares Gson 2.11.0 for its JSON tools; training retains Gson,
SQLite/Zstandard and all optimizer/corpus dependencies. No library versions
change. Root mixed tests explicitly declare their existing JUnit/Gson/SQLite/
Zstandard/FlatLaf/JNA requirements. No module acquires a dependency on research.
The guard rejects forbidden test/custom edges, overlapping source roots, root
production source sets and root configurations outside the approved composition.

## Production activation and remaining compiled seams

Main uses the small LegacyUciEvaluation profile: unchanged HCE default, explicit
hash-checked Pair-2 checkpoint selection, bounded warmup and accepted production
factory only. Any `seedv6.uci.*Research` property is rejected, including `off` and
`production`, so old control flags cannot silently override current production.
Standalone UCI keeps its pinned model profile and prior selector rejection.
GUI/core/training sources and resources remain unchanged.

The former UciEvaluation is research-owned, with its configuration/identity and
experimental algorithms retained. It no longer implements an application
interface. Root ResearchUciMain supplies a small UciConfiguration adapter and
uses the single app-uci protocol implementation. Research never imports or
compiles against UCI/GUI/app, and no production application discovers or loads
this adapter. There is no reflection, ServiceLoader or ambient plugin activation.

FutilityPolicy and other experimental policy/factory/hot-loop seams remain
compiled in core. ProductionSearch's accepted QXRAY path itself uses some of
these shared mechanics. Removal would require a separate search rewrite and is
not part of WU6. This establishes activation/configuration isolation and research
classpath isolation; it does **not** claim complete experimental binary exclusion.
Behavioral preservation requires the separately recorded runtime comparisons.

SR-021 remains **DEFERRED without adoption**. No feature is enabled, accepted,
tuned or resumed by this move. Existing research tests and entry points preserve
capability, not authority to launch a new empirical campaign. Search Contract
P2-R037/Frontier P2-F041, acceptance records and both Phase 1 archives are unchanged.
Outstanding research reconciliation/mirror actions remain outside migration scope.

## Entry points and historical reproduction

Use a migration-local GRADLE_USER_HOME, JDK 21 and --max-workers=2. Each of the
55 historical :app research task names delegates to the identically named
:research task, preserving argument properties, main classes, JVM limits and
explicit working directories. :app:verificationClasses delegates to research
classes; it no longer produces old app verification output directories.

All research JavaExec tasks now resolve relative user paths from the repository
root. Four formerly implicit app working directories (`searchBenchmark`,
`exactSearch`, `evaluatorBenchmark`, `brnRemediationBenchmark`) are explicitly
migrated to that convention, including their compatibility aliases. For old
commands with paths relative to app/, use absolute paths or prefix those paths
with `app/`. Input identities, algorithms and numerical parameters are unchanged.

- `:research:exactSearch`, `searchBenchmark`, `evaluatorBenchmark`,
  `developerTools`, `brnDiagnostic` and neural/BRN benchmarks run from research main.
- SR-001/005/006/007/018/021/022/023/029/030/036 tasks preserve their existing
  explicit opt-in study/cost/replay/integrity arguments; no campaign runs by default.
- `:research:test` runs independent/reference/research tests; `:integrationTest`
  runs mixed application comparisons; remaining module test tasks retain ordinary
  unit/production checks. Use `check -Pheadless --continue` for routine combined
  discovery. Existing slow-nnue exclusions remain; explicit slow tasks are retained
  in app/research/root, with :app:fullCheck composing them.
  Eleven SR-001h/i evidence tests retain their original instrumented-source
  property gates; routine production-source verification does not enable them.
  `-PbrnScalar` remains available in research and root integration test tasks.
- `:research:installResearch` exports research/core/training libraries to
  `research/build/install/research/lib`. Java tools use that lib wildcard without
  legacy app. The live Python BRN launchers use tools/research_paths.py and record
  actual core/training/research source/class owners.
- `:installResearchWorkspace` prepares `build/research/runtime`: classes/ contains
  only the small research UCI adapter, lib/ contains research/core/training/UCI
  dependencies, and research-launcher.properties marks this explicit workspace.
  Direct Java: `java <historical -Dseedv6.uci.* options> -cp
  "build/research/runtime/classes;build/research/runtime/lib/*"
  com.ohinteractive.seedv6.integration.ResearchUciMain` (use ':' on Unix).
  `:researchUci` is the corresponding stdin-connected Gradle route.
- tools/search-baseline.py recognizes that explicit research workspace; existing
  SR match wrappers preserve their flags and use the shared UCI implementation.
  Frozen historical installations without the marker still launch their original
  Main. New production Main deliberately rejects historical research/control flags;
  use the research workspace for those experiments, never a production distribution.

Immutable git-show paths in brn-throughput/brn-successor and recorded old raw-output
paths remain historical. analyze-search-delta's live EvalTuning path is corrected.
The frozen-draft reconciliation in analyze-brn-nested intentionally retains its
historical receipt mapping. Historical result files/protocols are not rewritten.
The original checkout also retains the ignored `archive/search-programme/` at
`C:/projects/seed/java/seedv6/archive/search-programme`. Its README limits the
SR-003, SR-017 and SR-001H/I source-substitution scripts to their original revisions
and original root tools/ placement; their anchors do not support the current
engine. This directory was absent from the migration worktree at WU5 and remains
outside all source sets. It is preserved read-only, with current file hashes in
the WU6 evidence, rather than copied into an active build. Fresh checkouts need
that retained local archive to reconstruct those instrumented historical runs.

Known missing BRN app/build/research payloads remain missing as recorded by that
programme. Fresh worktrees do not contain ignored external evidence. Original
models, corpora, checkpoints, frozen openings, adverse/interrupted evidence and
historical releases are preserved at their recorded paths; there is no rerun of
expensive campaigns or training programme.

## Legacy and downstream boundary

App retains Main GUI/UCI delegation, accepted-only checkpoint composition,
remaining ordinary/mixed checks, task aliases, corpus compatibility and existing
native packaging entry points. It owns no research algorithm. WU7 owns native
packaging and platform/client acceptance, including verification that root
research exports never enter production archives. WU8 owns final app retirement
and explicit cutover. No WU7 work, native release, publication or push occurs here.

## Validation, version and handback

The broad headless JUnit run covers 2,146 cases: 2,085 pass, 19 exact inherited failures and 42 existing skips. It includes all relocated routine tests, beyond WU5's selected coverage.

| Owner/task | Tests | Passed | Failed | Skipped |
| --- | ---: | ---: | ---: | ---: |
| app-gui | 22 | 22 | 0 | 0 |
| app | 469 | 459 | 10 | 0 |
| app-uci | 13 | 13 | 0 | 0 |
| engine-core | 61 | 61 | 0 | 0 |
| engine-training | 11 | 11 | 0 | 0 |
| research | 914 | 903 | 0 | 11 |
| root | 656 | 616 | 9 | 31 |

The initial broad run was 2,146 cases: 2,084 pass, 20 fail and 42 skip. The additional
TrainingSessionTest case exceeded its existing 90-second await-termination limit.
Unchanged WU5 and WU6 artifacts both failed when a paired replay ran concurrently;
two serialized replay pairs subsequently passed with unchanged assertions/limits.
The table reconciles that one case with those later passing results. Original
negative evidence is retained; exact transient full-suite cause remains unknown.
No production/test behavior was changed to make it pass.

The remaining 19 failure identities and messages match WU5 exactly, and their source bytes
are unchanged: GenerationRestartTest's nine archive boundaries; Brn1TrainingGui,
Brn2SupervisionGui, Brn2TrainingGui, BrnBootstrapGui (BRN-0/1), BrnCaptureGui,
BrnPair2Gui, BrnTrainingGui, TablebasePresentation and WorkflowRefinementGui.
No WU6 correctness regression remains evidenced; the timing sensitivity is disclosed. None is disabled, suppressed or removed.
The 11 SR-001 instrumentation skips, 28 headless GUI skips and three opt-in
external-input skips retain their existing conditions; explicit slow-nnue tasks are preserved but were not executed.

The evidence manifest records exact commands and results for compilation; effective
runtime classpaths; 15 expanded Gradle boundary fixtures; three Python launch/path
tests; frozen corpus evaluator execution; six exact-search positions and identical
compatibility-alias results; four frozen research-UCI comparisons and ten research
requests; 22 production selector rejection subprocesses; 203 exact SR-024 vectors
(all 17 fields); 66 legacy production UCI requests with ten exact comparisons;
159 standalone searches; and seven-family checkpoint/optimizer disk continuation.
All worker counts 1..16, node/time/stop limits and pinned model inference are covered
by the production subprocess and source-vector suites. Production class payloads
for core/training/GUI/UCI match WU5, and research and production use the same core
artifact. All six module classpaths and both ZIP/TAR application distributions were
inspected; no research JAR, adapter or research resource enters those distributions.

Independent oracle integrity, task argument preservation and all four relocated
resource hashes pass. Assembly, configuration-cache reuse and Windows/macOS packaging
plus legacy GUI launch dry-runs pass. No native package or window was created.
Both historical repositories' tracked/protected bytes, Git/merge metadata and full
file size/mtime inventories are unchanged. Production model and Search Contract/
frontier identities are unchanged. Raw commands/results remain ignored under
build/migration/wu6; [wu6-evidence.json](wu6-evidence.json) is the durable compact
manifest, including every relocation and hash.

Relocation compile dependencies on test fixtures were resolved with the narrow
Phase2Oracle extraction and root fixture closure. An initial PowerShell redirect
misreported unittest stderr as a shell error despite all 15 architecture tests
passing; direct subprocess capture was rerun successfully. A local ownership-audit
UTF-8 decoding error was likewise corrected in the validation harness only. No check
was weakened.

No root CODEXLOG_CURRENT.md exists or was created. VERSION_STATE remains 0.0.0,
build 35. The owner requires no production/GUI/UCI increment for WU6; local token
d0e75a728fc14f8587564ae3860f7ff8 is finalized no_bump after validation. The final
receipt is recorded in wu6-evidence.json. The first begin returned PermissionError
with no reservation; a supported retry succeeded without deleting locks, changing
tooling or bypassing ownership.

Human actions required after this prompt:

- Immediate implementation blocker: None; root composition approval is recorded.
- Blocking WU7 admission: GPT/user reconciliation of the completed WU6 work unit.
- Deferred, blocking dependent runtime/WU7/cutover gates: separate remediation of
  the 19 inherited GUI/checkpoint failures and user Windows/macOS GUI/client checks;
  public model, release naming and platform/signing decisions at their actual gates.
