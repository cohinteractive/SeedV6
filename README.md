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

The [unified architecture migration contract](docs/unified-architecture/CONTRACT.md)
and [migration state](docs/unified-architecture/STATE.md) govern the staged move to
one shared engine, training, GUI, UCI and research build. The five target Gradle
projects exist alongside temporary `:app`. Core, training, desktop and standalone
UCI have separate owners. `app` retains compatibility launchers, checkpoint/research
configuration and verification entry points. See [WU5 standalone UCI](docs/unified-architecture/WU5-UCI.md)
for launch instructions, compatibility profiles and the remaining acceptance gates.

```powershell
.\gradlew.bat :app:installDist
.\app\build\install\seedv6\bin\seedv6.bat gui
# Omit gui for UCI.
```

On other platforms use `./gradlew` and the generated `seedv6` launcher.

The standalone production UCI application uses the pinned generation-32 BRE-Pair 2
model and only `engine-core`. Model bytes are not included in this migration:

```powershell
$env:GRADLE_USER_HOME = "$PWD/build/migration/gradle-user-home"
.\gradlew.bat :app-uci:installDist
.\app-uci\build\install\seedv6-uci\bin\seedv6-uci.bat --model "C:\path\to\baseline.brn-pair2"
```

Use an existing verified local model, or place it at `models/baseline.brn-pair2`
inside that Java distribution. Missing or different model bytes fail explicitly.
The legacy `seedv6` launcher retains its HCE/book defaults.

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
- `app/src/main`: compatibility Main and legacy checkpoint/research UCI configuration; no second protocol stack.
- `app/src/verification`: active developer tools, comparisons and reference implementations.
- `app/src/test`: tests, independent oracles and test-only research harnesses.
- `source`: active Phase 2 Search Contract and Research Frontier.
- `docs`: architecture, operation, frozen Phase 1 search canon, historical reports and evidence.
- `archive`: historical instrumentation, excluded from every source set.

Routine headless checks exclude the explicitly expensive NNUE suite:

```powershell
.\gradlew.bat :engine-core:test :app:test -Pheadless
```

Developer tasks such as `:app:exactSearch`, `:app:searchBenchmark`,
`:app:brnDiagnostic`, `:app:brn2Diagnostics` and `:app:nnueResearch` use the
verification classpath. These facilities are excluded from the application JAR.
The diagnostic command dispatcher is now `:app:developerTools`:

```powershell
.\gradlew.bat :app:developerTools -PtoolArgs="brn-diagnostic help"
# Frozen replay: use -PtoolArgs="frozen-wdl ..." with the explicit input paths.
```

The active Phase 2 [Search Contract](source/CHESS_SEARCH_CONTRACT.md) and
[Research Frontier](source/CHESS_SEARCH_RESEARCH_FRONTIER.md) govern the
strength-focused programme. The complete Phase 1
[Contract R045](docs/search-phase1/CHESS_SEARCH_CONTRACT.md) and
[Frontier F052](docs/search-phase1/CHESS_SEARCH_RESEARCH_FRONTIER.md) are frozen
historical archives; their original active labels describe Phase 1 only.
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

Windows and macOS packaging remain available through `packageWindows` and
`packageMac`; see the operating guide. Bundled Fathom sources retain their
[upstream license](engine-core/src/main/native/syzygy/fathom/LICENSE) and provenance.
