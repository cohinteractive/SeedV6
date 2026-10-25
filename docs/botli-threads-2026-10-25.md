# BotLi four-worker deployment — 25 October 2026

BotLi's configured standard engine now runs the existing production Construction
V1 + CP64 implementation with Threads 4. No Java/search implementation changes
were necessary. The repository started clean at
`1b8aa39550c6` (build 77). No commit or version increment was made.

## Cause and source trace

BotLi previously selected `SeedV6-NNUE-CP64` inside
`releases/candidates/macos-arm64/candidate-20261024T073531-68581efd/SeedV6-UCI/`.
That package was already build 77 from the current revision. The problem was the
selected entry point, not stale engine code.

`app-uci/.../NnueCandidateUciMain.java` explicitly advertises maximum 1 and accepts
only one worker. Its `engine-core/.../ConventionalNnueEngine.java` service also
rejects other counts and builds the historical recursive E008 search. This is an
intentional frozen-profile restriction, not a general CP64 or SeedV6 limitation.
Both restrictions and their tests remain intact.

The production execution path is:

1. `UciMain` loads `BaselineUciConfiguration`, supporting workers 1..16.
2. `UciEngine.setOption` validates Threads and constructs a replacement service
   with the requested count, then closes/drains the previous service.
3. `BaselineEngine.service/create` uses `ConstructionSearch.create` with that count.
4. For one worker, Construction uses the independent `ExactSearchAdapter` owner.
   For four, it creates `ParallelSearch` with four Worker instances and a fixed
   pool of three helpers. `SearchLifecycleService` supplies the participating owner.
5. `ParallelSearch.searchWindow` submits helpers and runs worker zero on the owner.
   Each worker has private mutable evaluator/search state. Shared TT, root-job
   allocation, cancellation and drainage use the existing implementation.

No features or safety checks were removed. Production remains CP64 flags3407871,
ADAPTIVE16, ROOT_REUSE for multiple workers and the existing BOUNDARY2 clock policy.
The E008 entry point remains single-worker. Shallow depths below three and roots
with at most one legal move still use only the owner; four selected workers do
not imply four busy threads throughout every iteration or an opening-book lookup.

## Deployment

Built a new native arm64 image with bundled Java 21:

```text
/Users/charlesclark/projects/seed/java/seedv6-unified/releases/candidates/macos-arm64/candidate-20261025T100803-fb508891/SeedV6-UCI/SeedV6-UCI
```

Only `engines.standard.dir` and `engines.standard.name` changed in
`/Users/charlesclark/chess/BotLi/config.yml`. Threads remains 4, Hash remains 256,
UseBook remains true and ponder remains false. A parsed comparison verified that
every other setting was unchanged. The private, mode-0600 config backup is
`/Users/charlesclark/.local/state/seedv6/botli-config-backups/config.yml.before-seedv6-threads4-20261025`.
The old complete engine image remains untouched.

The previous and rebuilt core JARs have the same SHA-256:
`e3a70c4e2e16d8ba21a2fe64da18e68ac828fade7a3b09e5367e2b99f279c5ec`.
Both embed the same CP64 network SHA-256
`8431626c2fca81e0a1e9ccd5e45ddd8829e6e3282c756c21fad75b06d075bb2b`
and opening-book SHA-256
`55323f02dafbec40b2d43a9c23f8111e3781fce5609c123c73783d46e23fb103`.
The profile selection changes E008 to the already-established production
Construction V1 path; it does not introduce a new search configuration.

## Verification

Executed successfully:

```sh
./gradlew --no-daemon --max-workers=1 -Pheadless -PconventionalVectorTests \
  :engine-core:test \
  --tests com.ohinteractive.seedv6.search.driver.BaselineEngineTest \
  --tests com.ohinteractive.seedv6.search.manage.Boundary2ProductionTest \
  :app-uci:test :app-uci:packageMac
```

- 36 JUnit tests passed: 10 core tests and all 26 app-uci tests, no skips/failures.
- Native package and independently extracted ZIP passed existing UCI checks at
  Threads 1, 4 and 16, including readiness, Hash, depth/timed/clock searches,
  stop, reset, book aliases, quit, active-search EOF/termination and bad assets.
- Direct native UCI advertised `Threads type spin default 1 min 1 max 16` and
  reported effective `workers=4;hashMiB=256` after configuration.
- During an infinite search, successive JVM thread dumps measured CPU increases
  of 1337.10 ms on `seedv6-search-worker` and 429.63, 433.25 and 431.05 ms on its
  three `seedv6-search-helper` threads. All four performed work; this is stronger
  evidence than accepting the option or counting idle JVM threads.
- Stop returned a legal move. Switching to Threads 1 completed depth 5 and left
  no helper threads. Switching back to four completed fixed-movetime and managed
  clock searches with legal moves/PVs, then quit with exit code 0.
- Sampled RSS during the four-worker/Hash256 search was 662.2 MiB. This is one
  bounded observation, not a sustained-load or multiple-game capacity result.
- The installed BotLi `Engine.test` passed using the final config through its
  own config parser and python-chess. Its existing unsupported `Move Overhead`
  option was reported as ignored and left unchanged; it does not block startup.

Only standalone local processes were launched. No BotLi session was interrupted,
no user interface was started, and no Lichess games or connections were initiated.
This is functional native qualification, not a new playing-strength or general
clock-tail guarantee. There is no remaining blocker to running BotLi with Threads 4.

## Local evidence

Generated evidence is ignored/local-only and is not included in a fresh checkout:

- `build/verification/botli-threads-2026-10-25/`: deployment receipt, offline BotLi
  result, verification scripts, native UCI transcript and JVM thread dumps.
- `native-worker-check-02/result.json` inside that directory is the successful
  direct execution result, produced by `verify_workers_cpu.py` with the BotLi venv.
- `native-worker-check-01/` and `verify_workers.py` preserve the initial overly
  strict sampling check. CPU increased on all four threads, but its requirement
  that every helper be inside search during one of three instantaneous dumps
  failed while helpers were parked between root cohorts. The harness stopped its
  own process. The second check measures per-thread CPU deltas instead; no engine
  change was made to obtain the passing result.
- JUnit XML/HTML results remain under `engine-core/build/` and `app-uci/build/`;
  a copy of the 36 test results is preserved under the verification directory's
  `junit/`, with counts and platform/version metadata in `build-summary.json`.
- The new candidate directory contains `candidate.json`, the image's
  `build-manifest.json` and `SeedV6-UCI-macos-arm64.zip`; these record native and
  extracted-package checks, source identity and file hashes.

Only this report and the candidate guide clarification are repository changes.
`VERSION_STATE.txt` remains unchanged at build 77.
