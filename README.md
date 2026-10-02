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

The external training corpus has a separate headless entry point. Import a bounded
Zstandard-compressed Lichess evaluation stream, then reopen and validate it:

```powershell
.\gradlew.bat :app:corpus -PcorpusArgs="import-lichess --max-records 10000 --shard-size 2000 --progress-every 2000" -PcorpusInput="E:\SeedV6-Corpus\incoming\lichess\lichess_db_eval.jsonl.zst" -PcorpusRoot="E:\SeedV6-Corpus\corpus"
.\gradlew.bat :app:corpus -PcorpusArgs="validate --sample 3" -PcorpusRoot="E:\SeedV6-Corpus\corpus"
```

Use `--max-records 0` (or omit it) for a full import; the default shard/progress
interval is 100,000 source records. Repeat imports rescan the stream from its start
and consolidate exact position identities without increasing cardinality for
duplicates. Stronger comparable labels append immutable revisions. This corpus
supports [explicit headless BASIC_V1 BRN-2 corpus training](docs/corpus-training.md).
The [corpus API documentation](app/src/main/java/com/ohinteractive/seedv6/corpus/package-info.java)
defines schema 1, counter availability, identity, provenance and durability.

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
