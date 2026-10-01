# SeedV6

SeedV6 is a Java 21 chess engine with UCI, desktop Play, and Network Training.
The completed first search programme supplies one production search lineage:
`SearchDriver -> ExactSearchAdapter -> ExactSearch`, with optional cooperative
same-depth `ParallelSearch` for managed multi-thread requests. Its table is `TTable`.

Start with the [architecture and source boundaries](docs/architecture.md) and
[operating guide](docs/guide.md).

```powershell
.\gradlew.bat :app:installDist
.\app\build\install\seedv6\bin\seedv6.bat gui
# Omit gui for UCI.
```

On other platforms use `./gradlew` and the generated `seedv6` launcher.

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

Windows and macOS packaging remain available through `packageWindows` and
`packageMac`; see the operating guide. Bundled Fathom sources retain their
[upstream license](app/src/main/native/syzygy/fathom/LICENSE) and provenance.
