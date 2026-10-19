# SeedV6

SeedV6 is a Java 21 chess engine with UCI, desktop Play, and Network Training.
The completed first search programme supplies one production search lineage:
`SearchDriver -> ExactSearchAdapter -> ExactSearch`, with optional cooperative
same-depth `ParallelSearch` for managed multi-thread requests. Its table is `TTable`.

**Arena** compares named model lineages on identical frozen records and targets,
with paired games before training and after each campaign round. Start from a fresh
initializer or copy an exact generation; each competitor keeps its own recipe and
optimizer. Setup, Live and History share the model/data browsers and board/progress
views. **Match only** compares fixed network checkpoints and HCE in either position,
including HCE vs HCE, without training. See the [Arena guide](docs/learning-arena.md).

Start with the [architecture and source boundaries](docs/architecture.md) and
[operating guide](docs/guide.md).

For explicit **construction-v1** experimental gameplay in macOS BotLi or Windows
Play/Arena, use the [construction-v1 activation guide](docs/construction-v1-gameplay.md).
It selects the full flattened Search with the verified Pair-2 g32 model and
aspiration OFF; ordinary standalone UCI continues to select production Policy 4.

The [unified architecture migration contract](docs/unified-architecture/CONTRACT.md)
and [migration state](docs/unified-architecture/STATE.md) govern the staged move to
one shared engine, training, GUI, UCI and research build. The five target Gradle
projects exist alongside temporary `:app`. Core, training, desktop and standalone
UCI have separate owners. `research` owns experiments, independent references and
diagnostics. `app` retains compatibility launchers and production-only checkpoint
configuration; root verification composition preserves mixed integration tests. See [WU5 standalone UCI](docs/unified-architecture/WU5-UCI.md)
for launch instructions, compatibility profiles and the remaining acceptance gates.

```powershell
.\gradlew.bat :app:installDist
.\app\build\install\seedv6\bin\seedv6.bat gui
# Omit gui for UCI.
```

On other platforms use `./gradlew` and the generated `seedv6` launcher.

The standalone production UCI application uses the pinned generation-32 BRE-Pair 2
model and only `engine-core`. Ordinary Git clones include the approved model at
`build-assets/production/bre-pair2-g32.brn-pair2`, with its identity manifest.
For a Java-only development distribution:

```powershell
$env:GRADLE_USER_HOME = "$PWD/build/migration/gradle-user-home"
.\gradlew.bat :app-uci:installDist
.\app-uci\build\install\seedv6-uci\bin\seedv6-uci.bat --model "$PWD/build-assets/production/bre-pair2-g32.brn-pair2"
```

Native packaging automatically stages that model at `models/baseline.brn-pair2`
inside each application. Missing or different model bytes fail explicitly.
The legacy `seedv6` launcher retains its HCE evaluator. Both UCI launchers default
to book disabled; `setoption name UseBook value true` (or the `OwnBook` alias)
enables the shared bundled repertoire. No external book path is needed. See the
[UCI guide](docs/guide.md#uci-and-search-lifecycle) for BotLi and packaging details.

Use **Network Training > Data & exposure > Add from library** to register original
Lichess JSONL/PZstandard sources, existing Seed data directories or Stockfish
BINP/Zstd shard folders with an explicit label profile. Register and prepare once,
then select the same source in Training or Arena. Each lineage retains independent
weights and sequential cursors. Recipe & lineage owns learning rate and batch/epochs;
Validation & run owns search/pairing and explicit termination. **File > Training
storage settings** chooses the model-library root. See the [Training Data guide](docs/corpus-training.md).

Legacy import/validation command-line tools remain available for existing artifacts;
Lichess training does not require conversion into a second dataset. BINP preparation
preserves the original BINP chunks and never modifies the downloaded archives.

- `engine-core/src/main`: shared runtime engine, model inference, opening book and optional native tablebase provider.
- `engine-core/src/test`: independent runtime unit and inference-boundary tests.
- `engine-training/src/main`: shared trainers, model workspace, checkpoint continuation, corpus and campaign services.
- `app-gui/src/main`: desktop Play, Network Training, Arena, preferences and GUI resources
  (WU4 structural migration complete; [native user acceptance deferred to WU7](docs/unified-architecture/WU4-GUI.md)).
- `app-uci/src/main`: standalone UCI entry point, one protocol/session implementation, pinned model selection.
- `app/src/main`: compatibility Main and accepted-only legacy checkpoint UCI configuration; no second protocol stack.
- `research/src/main`: research tools, independent references, empirical runners and the frozen evaluation corpus.
- `research/src/test`: independent comparisons and research verification.
- `src/integrationTest`: mixed application/training tests requiring research references.
- `src/researchLauncher`: small explicit research-UCI adapter; excluded from production.
- `app/src/test`: remaining production/legacy integration checks.
- `source`: active Phase 3 Search Contract and Research Frontier.
- `docs`: architecture, operation, frozen Phase 1/2 search canon, historical reports and evidence.
- `archive`: historical instrumentation, excluded from every source set.

Routine headless checks exclude the explicitly expensive NNUE suite:

```powershell
.\gradlew.bat check -Pheadless --continue
```

Developer tasks such as `:app:exactSearch`, `:app:searchBenchmark`,
`:app:brnDiagnostic`, `:app:brn2Diagnostics` and `:app:nnueResearch` use the
research runtime classpath via compatibility delegation. New commands should use
`:research:` instead of `:app:`. These facilities are excluded from production
JARs/distributions. See [WU6 research launch and preservation](docs/unified-architecture/WU6-RESEARCH.md).
The diagnostic command dispatcher is now `:app:developerTools`:

```powershell
.\gradlew.bat :app:developerTools -PtoolArgs="brn-diagnostic help"
# Frozen replay: use -PtoolArgs="frozen-wdl ..." with the explicit input paths.
```

The active Phase 3 [Search Contract](source/CHESS_SEARCH_CONTRACT.md) and
[Research Frontier](source/CHESS_SEARCH_RESEARCH_FRONTIER.md) govern the
strength-focused, architecture-aware combination programme. The complete Phase 1
[Contract R045](docs/search-phase1/CHESS_SEARCH_CONTRACT.md) and
[Frontier F052](docs/search-phase1/CHESS_SEARCH_RESEARCH_FRONTIER.md) are frozen
historical archives. Final Phase 2 [Contract P2-R049](docs/search-phase2/CHESS_SEARCH_CONTRACT.md)
and [Frontier P2-F053](docs/search-phase2/CHESS_SEARCH_RESEARCH_FRONTIER.md)
are also frozen byte-for-byte; historical active and handback statements are not
current instructions. Phase 3 has no active research objective at initialization.
[Search reports](docs/research/search/) and
[BRN reports](docs/research/brn/) preserve the supporting research; their historical
measurements do not establish current performance or strength.

Research reports, configurations, manifests, compact summaries and reusable
frozen opening inputs remain version-controlled. Raw tournament games, detailed
diagnostics, transcripts and generated test results are retained locally at their
recorded paths under `.gitignore`; a fresh checkout does not include that evidence.
Check local availability before reproducing an analysis. See the
[research evidence storage policy](AGENTS.md#local-research-evidence) for retention
and future output conventions.

The [BRN-3 learning-strength result](docs/research/brn/BRN_LEARNING_RESULT.md)
documents the current quarter-residual play calibration, controlled gains over
material-only Gen0, continued-training evidence and remaining limits. The
[BRN-3 operator guide](docs/brn/BRN_V1.md) explains raw training versus calibrated
play and compatibility with existing checkpoints.

The [BRN-3 throughput contract](docs/brn/BRN_THROUGHPUT_CONTRACT.md),
[frontier](docs/brn/BRN_THROUGHPUT_FRONTIER.md) and
[measured results and reproduction commands](docs/research/brn/BRN_THROUGHPUT_RESEARCH.md)
govern the loading/training optimization workstream, including SIMD, scalar
fallback, accelerator findings and bounded validation limits.

The completed [BRN successor investigation](docs/brn/BRN_SUCCESSOR_STATE.md),
[contract](docs/brn/BRN_SUCCESSOR_CONTRACT.md) and
[measured findings](docs/research/brn/BRN_SUCCESSOR_RESEARCH.md) retain BRN-3 with
an exact-semantic capture-cache optimization; no architectural challenger earned
promotion. Original BRN-3 and unchanged NNUE remain independent references.

The completed [BRN architecture research programme](docs/research/brn-architecture-cglhw/STATE.md)
[recommends compiled order-2 pair tables](docs/research/brn-architecture-cglhw/RECOMMENDATION.md)
within the tested consumer-compute regime, with controlled BRN/NNUE comparisons,
3,072 final games and explicit limits. Production adoption remains a separate
owner decision; the current production evaluator is unchanged.

[BRE-Pair 2](docs/brn/BRN_PAIR2.md) now supports independent normal
training, exact resume, generations/Best, Play, Arena and model-selected search
benchmarks. It preserves C02 compilation and keeps full training state separate
from the runtime pair tables; production/default evaluation remains unchanged.

Unified Windows and macOS packaging builds both GUI and UCI with `packageWindows`
and `packageMac`, or independent `:app-gui:` / `:app-uci:` module tasks. These are
local integrity-checked candidates, not official releases. With Python 3.9+,
Git and a complete installed Java 21 JDK (21.0.6 or newer), a fresh clone needs no
model transfer or local configuration file:

```sh
./gradlew packageWindows  # Windows Git Bash; PowerShell: .\gradlew.bat packageWindows
./gradlew packageMac      # Native macOS arm64 host
```

Packaging discovers installed native JDK tools independently of Gradle's daemon
and Java compiler toolchain. Neither JDKs nor models are downloaded by packaging.
See [packaging instructions](docs/unified-architecture/WU7-PACKAGING.md)
for optional overrides, individual application tasks, validation and outstanding
native/human gates. Bundled Fathom sources retain their
[upstream license](engine-core/src/main/native/syzygy/fathom/LICENSE) and provenance.
