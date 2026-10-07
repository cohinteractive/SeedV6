# SeedV6

SeedV6 is a Java 21 chess engine with UCI, desktop Play, and Network Training.
The completed first search programme supplies one production search lineage:
`SearchDriver -> ExactSearchAdapter -> ExactSearch`, with optional cooperative
same-depth `ParallelSearch` for managed multi-thread requests. Its table is `TTable`.

**Arena** compares named model lineages on identical frozen records and targets,
with paired games before training and after each campaign round. Start from a fresh
initializer or copy an exact generation; each competitor keeps its own recipe and
optimizer. Setup, Live and History share the model/data browsers and board/progress
views. See the [Arena guide](docs/learning-arena.md).

Start with the [architecture and source boundaries](docs/architecture.md) and
[operating guide](docs/guide.md).

```powershell
.\gradlew.bat :app:installDist
.\app\build\install\seedv6\bin\seedv6.bat gui
# Omit gui for UCI.
```

On other platforms use `./gradlew` and the generated `seedv6` launcher.

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

- `app/src/main`: current application code and shipped resources.
- `app/src/verification`: active developer tools, comparisons and reference implementations.
- `app/src/test`: tests, independent oracles and test-only research harnesses.
- `docs`: architecture, operation, search masters, historical reports and evidence.
- `archive`: historical instrumentation, excluded from every source set.

Routine headless checks exclude the explicitly expensive NNUE suite:

```powershell
.\gradlew.bat :app:test -Pheadless
```

Developer tasks such as `:app:exactSearch`, `:app:searchBenchmark`,
`:app:brnDiagnostic`, `:app:brn2Diagnostics` and `:app:nnueResearch` use the
verification classpath. These facilities are excluded from the application JAR.
The diagnostic command dispatcher is now `:app:developerTools`:

```powershell
.\gradlew.bat :app:developerTools -PtoolArgs="brn-diagnostic help"
# Frozen replay: use -PtoolArgs="frozen-wdl ..." with the explicit input paths.
```

The [search contract](docs/search/CHESS_SEARCH_CONTRACT.md) and
[frontier](docs/search/CHESS_SEARCH_RESEARCH_FRONTIER.md) preserve the first
programme's decisions. [Search reports](docs/research/search/) and
[BRN reports](docs/research/brn/) preserve the supporting research; their historical
measurements do not establish current performance or strength.

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
[upstream license](app/src/main/native/syzygy/fathom/LICENSE) and provenance.
