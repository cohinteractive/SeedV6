# BRE-Pair 2 Search measurement baseline

2026-10-09. Phase 2 prerequisite, **READY WITH DOCUMENTED LIMITATIONS** for
controlled single-worker Search comparisons. Technical completion is distinct
from GPT/owner acceptance. No SR feature was researched or adopted.

## Frozen evaluator and executable configuration

The clean starting checkout was `main` at `f22c76922d0a`; the reported initialization
commit `791814d` is an ancestor. Both active masters were P2-R001/P2-F001.
The final source change adds explicit UCI loading, bounded startup warm-up and
request accounting; it changes neither production defaults nor Search policies.
The measured application JAR is bound by SHA-256 in every retained run receipt.
Its embedded source label is `f22c76922d0a (uncommitted changes)`, build 35.
The evidence source manifest binds the final application source/bytecode to this
work's Git change; the starting binary is **not** the measured baseline.
Rebuilding after commit can change embedded provenance/JAR bytes. Record the new
JAR hash and handshake, and rerun preflight before comparing builds.

Selected lineage: `235d0688-e060-44e3-8f1e-119c39bc8017`, the sole managed
`BRN-Pair2` lineage in the configured library. At discovery, its verified Best was
generation 32, latest-training was generation 96, and the saved white Play
selection was generation 61. The established Best selection rule resolves this
choice; newest-training and a saved explicit Play selection do not replace Best.
Subsequent comparisons freeze this exact generation rather than rereading Best.

```text
checkpoint: g000032-s001753088-5837c7f7192aa3cf9a265388f93649327455e804541398ae3ef6b69e09014304
network SHA-256: 5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7
manifest SHA-256: a51513ddb6e23a8537cc38b8aeb7676af5723c3e3b6bb8151f265bb87ae83891
training.state SHA-256: f8bdda5eda0ff7893ee36c2270baaedecaf50e14ff5202ab786996f72a2788d5
training-metadata SHA-256: 135fb61ed736f313158e5514f2a16a81eda6526c4f034d2232ff26f8ee4176e7
observed loading root: E:/SeedV6-Networks/BRN-Pair2/235d0688-e060-44e3-8f1e-119c39bc8017
```

This external model is available and verified locally; it is not bundled, copied
into Git, retrained or promoted. Supply the store path explicitly on another host.
Preserve its four checkpoint files (about 3.53 MB total). Current managed snapshot
loading verifies the manifest, both payload hashes/lengths, optimizer step,
configuration and exact recompilation of the saved trainer. It therefore requires
the training payload too, although Search only retains immutable runtime tables.
Pruned/missing payloads, incompatible families/layouts, CRC failures, wrong hashes
and failed loads must invalidate a measurement. No fallback is permitted.

The supported family is `BRN_PAIR2`, schema `seedv6.brn.pair2` v1, pure Java C02
compiled order-2 pair lookup runtime. Its 327 anchored templates/eight shapes,
13 categories and two roles fold to 294 physical pair tables with adjacent
binary64 STM entries. Training's 113,231 double parameters fold through 110,527
float inference parameters before compilation. `network.brn-pair2` is 795,032
bytes; `training.state` is 2,731,424 bytes. Codec v1 fixes geometry, folding,
compiler and calibration, with big-endian data and CRC32. Historical tuple
`selected.model` files are research inputs, not managed playable checkpoints.
The codecs are platform-neutral; actual cross-OS execution was not tested here.

## Scores and ownership

For side to move, raw output is `material + bias + sum / sqrt(654)`. Material is
P=1, N=3.2, B=3.3, R=5, Q=9, K=0, signed for STM. The two learned STM tables
need not be exact negatives of one another. Search uses
`material + 0.25 * (raw - material)`, identity
`BRN_PAIR2_MATERIAL_RESIDUAL_QUARTER_V1`. It rounds `100 * abs(value)` half away
from zero and restores sign, saturating ordinary scores at +/-32,511. There is
no inference sigmoid or WDL conversion. Mate bands start at +/-32,512; mate is
+/-32,768 adjusted by real ply, with a maximum supported depth of 256.
Terminal exhaustion and rule draws precede evaluation and TT reuse.

UCI `score cp` exposes these internal integers unchanged; mate values use the
existing exact ply-to-move conversion. A 100-unit material pawn is a scaling
convention, **not empirical centipawn equivalence, win probability or evaluator
accuracy**. The quarter gain is inherited C02 calibration, not newly optimized
here. No BRE-specific pruning thresholds have been validated by this work.

Only placement and STM enter Pair-2 evaluation. Castling, EP, clock, history and
hash still matter to Search identity/rules, independently of this evaluator.
Each owner/worker has its own rolling placement workspace: 2,920 primitive bytes;
799,688 immutable primitive bytes may be shared within an engine. There is no
Pair-2 position hash evaluation cache or configurable cache capacity. Mutable
boards, histories and evaluator scratch are private. Rolling floating-point
updates matched fresh recomputation on the exercised positions; this is bounded
observed determinism, not a universal numerical proof for arbitrary histories.

## Actual policies and reference boundary

| Path | HCE default | BRE-Pair 2 / other neural definitions |
| --- | --- | --- |
| Production single worker | `ExactSearchAdapter` calibrated composition | Same adapter, unpruned constructor |
| Production 2..16 workers | `ParallelSearch`, calibrated per worker | Same-depth cooperative workers, unpruned per worker |
| NMP | Scout-only d4..6, R=2, surplus 512, clock/material guards | **Off** |
| Reverse futility/static null | Scout-only d2, margin 960, clock/mate guards | **Off** |
| Ordinary exact/reference constructors | Unpruned | Unpruned |

Both production gates compare the definition to the canonical handcrafted
singleton at the factory boundary. `handcraftedIsolation()` and explicit custom
evaluators are also unpruned. No gating bug was found or corrected.

BRE-Pair 2 production retains full-window successive-depth iteration, recursive
nominal-depth PVS with required re-search, static leaves, legal hash first,
SEE-good/bad tacticals by material, main quiet history, staged/lazy generation,
and legal-existence testing at non-check leaves. No qsearch, LMR, aspiration,
extensions, ProbCut, neural pruning, or speculative calibration is enabled.
Quiet history resets per invocation/private worker; changing that is not part
of this prerequisite. Ordinary mathematical TT scores require equal depth and
current request generation; prior entries can offer legal ordering moves.
NO_LEAF_TT, cutoff-only ordinary bounds, real-prefix/path identity and mate
normalization remain intact. No evaluator replacement occurs inside an owner.

The default requested TT capacity is 64 MiB: three primitive arrays with
2,097,152 entries occupy 48 MiB, plus array/lock headers and 32 striped locks.
Competing processes own independent TTs; helpers within a request share their
owner's coherent TT/generation and drain before publication/reset/reuse.

Independent `ExactSearch` TT-off CONTROL ordered alpha-beta and the unpruned
SR-001A qsearch oracle remain available. Research-only alternative ordering,
qsearch and pruning factories do not define this baseline. `exactSearch --tt=on`
uses a direct fixed-depth harness and an explicit **4 MiB** table, not the full
production driver/64 MiB configuration. Its initial smoke test is not substituted
for the UCI production measurements below.

GUI explicit checkpoint selection and UCI explicit loading converge on
`NetworkModel.evaluation` and `ProductionSearch`; ordinary GUI/UCI startup remains
HCE. GUI preferences are not read by UCI. Opening book and optional configured
root Syzygy outcomes are supported production compositions; the comparison
protocol explicitly disables both. No GUI or browser verification was needed
for this headless change, and no visual verification is claimed.

## Reproduction

Tools are [`search-baseline.py`](../../../tools/search-baseline.py), the existing
Gradle build, and [Fastchess v1.8.2-alpha](https://github.com/Disservin/fastchess/releases/tag/v1.8.2-alpha).
The [runner manual](https://github.com/Disservin/fastchess/blob/v1.8.2-alpha/man.md)
defines engine `args=`, `-repeat`, clocks, PGN tracking and pentanomial reporting.
This is a bounded adapter, not a new game runner. All executable commands,
JAR/network/runner hashes, Java version, selected openings and outcomes are
recorded in each new output directory. Pin those inputs before comparing builds.

```powershell
.\gradlew.bat :app:installDist :app:verificationClasses --offline
python -m venv app/build/search-baseline/venv
$py = 'app/build/search-baseline/venv/Scripts/python.exe'
& $py -m pip install chess==1.11.2
$store = 'E:/SeedV6-Networks/BRN-Pair2/235d0688-e060-44e3-8f1e-119c39bc8017'
$runner = 'app/build/search-baseline/tools/fastchess/fastchess-windows-x86-64/fastchess.exe'
# Obtain the pinned Windows archive from the release above, extract under tools.
& $py tools/search-baseline.py --baseline app/build/install/seedv6 `
  --store $store --out app/build/search-baseline/new-preflight
& $py tools/search-baseline.py --baseline app/build/install/seedv6 `
  --candidate app/build/install/seedv6 --store $store --runner $runner `
  --openings docs/research/search/bre-pair2-baseline-2026-10-09/pilot-openings.pgn `
  --pairs 2 --tc 5+0.05 --timeout 180 --out app/build/search-baseline/new-aa
```

Use two separately frozen install directories for a real candidate comparison;
do not rebuild either while running. `--java` selects an explicit Java executable.
The observed executable is `C:/java/jdk-21/bin/java.exe`; the Fastchess Windows
launcher was verified with an executable path without spaces. Equivalent Unix
commands and foreign platforms require their own preflight.

For direct UCI, use process properties `seedv6.uci.pair2Store`, `checkpoint`,
`networkSha256` and optionally `warmupDepth=0..4`. A store requires an expected
lowercase SHA-256; omitted checkpoint means verified Best, still hash-checked.
The baseline adapter always specifies the exact generation. The UCI handshake
reports evaluator, checkpoint/hash, mapping, warm-up, policies, build and source.
Changing `Threads` preserves the selected definition. Send `OwnBook=false`,
`Threads=1`, `isready`, `ucinewgame`, full `position ... moves ...`, then `go`.

Primary resource controls: OpenJDK 21+35-2513, Windows 11 Pro 10.0.26300, AMD Ryzen
5 5500 (6 cores/12 logical CPUs), approximately 31.8 GiB RAM. Flags are
`-Xms256m -Xmx512m -XX:ActiveProcessorCount=1 -Djava.awt.headless=true`; ambient
Java option variables are removed. Syzygy config/path are explicitly blank.
Three untimed depth-4 production requests warm each new JVM using private state
discarded before play. This is a bounded warm-up, not proof of JIT steady state.
Engines persist between games (`restart=off`), with new-game reset, one Search
worker each and Fastchess concurrency 1. Only the thinking engine searches;
no pondering is enabled. No benchmark/build/training was run alongside matches.
Normal desktop/background services remained; no exclusive CPU isolation or
cross-machine speed claim is made. `ActiveProcessorCount` controls JVM ergonomics,
not OS affinity. Confirm finalists on a quiet host and record affinity/power/load.

The short functional pilot is **5 seconds + 50 ms per move**, no external grace,
score resignation/draw threshold, tablebase adjudication or maximum-game cap.
Representative feature testing should predeclare longer **60+0.6** matches,
with **180+1.8** as an independent longer-control confirmation where affordable;
these longer campaigns were not run here. Multiworker feature strength needs its
own campaign; two-worker integrity checks do not establish speedup or Elo transfer.

TimeManager remains reserve=50 ms, overhead=10 ms, default moves-to-go=30,
allocation `min(remaining-50, max(1, remaining/MTG + increment-increment/4 -10))`
with saturating/floor-safe integer arithmetic. Movetime subtracts 10 ms.
Clock-managed requests may also stop after a completed iteration with one legal
root move or a proven mate in one (`TIME_ALLOCATION`); hard stops take precedence.
Node budgets count prepared child transitions across all iterations/helpers,
including aborted work; roots/PVS repeated entries are not new admitted children.
Direct exact-harness node counts use a different convention. Completed-iteration
UCI `info nodes/time` does not include the subsequently aborted iteration.
The new end-request telemetry does; Fastchess PGN captures it with `match_line`
and the adapter aggregates it separately. PGN also retains runner-measured move
time, time left, final-iteration nodes/NPS and PV. Apparent runner “latency” versus
last completed iteration includes unfinished work, not just communication cost.
Startup/loading/warm-up and total run time are separately recorded. The extra
line is outside recursion; its protocol cost is included equally on both sides.

## Openings and statistical protocol

Source: [Stockfish testing books, 8moves_v3.pgn](https://github.com/official-stockfish/books).
Uncompressed SHA-256 `5835239f88cc2c7511b177c32392a69f3ede21819cf0616f80a7f907cd21d17e`;
34,700 eight-move PGNs. No evaluator-based selection/filtering is added.
Deduplicate by final placement/STM/castling/legal EP, partition by SHA-256 of
those FEN fields modulo 2, then sample sorted unique keys with a recorded Python
seed. Partition 0 (17,348 distinct admitted positions) is exploratory; partition
1 is reserved for finalist confirmation. Preserve full PGN move prefixes so
history and clock state are not invented. The two pilot positions use seed
20261009 and partition 0. Fastchess uses seed 20261009, sequential selected input,
two games per opening and reversed colors. Receipt hashes bind exact selection.

```powershell
& $py tools/search-baseline.py openings path/to/8moves_v3.pgn new-openings.pgn `
  --count 2000 --seed 20261010 --partition 0
# For a frozen finalist: independently predeclare count/seed and use partition 1.
```

The example count is not a universal requirement or permission for an immediate
campaign. Choose sample size from expected pair variance, desired precision,
resource budget and prospective power. Do not repeatedly pick favorable seeds,
reuse confirmation data for tuning, or count repeated games on the same opening
as new independent opening clusters. The repeated pilots here remain separate.
Opening-disjoint confirmation is not proof of training-game/source independence;
training provenance does not establish that relationship. Broader fresh opening
material/time controls should confirm any claimed generalization.

Before each feature experiment, freeze one baseline, one candidate, the same
network/mapping/options, opening partition/seed, clock, worker count, sample size,
hypothesis, alpha, stopping rule and acceptance method. Default fixed-sample
question: does the candidate's expected score exceed .5? W/D/L count 1/.5/0;
each color-reversed pair is one observation in [0,1]. Report the five pair-total
bins 0/.5/1/1.5/2, mean score, and logistic score-equivalent Elo
`400*log10(p/(1-p))`. This Elo is conditional on the tested opponent/resources.

The adapter reports a pair-variance normal 95% score interval and monotone Elo
transform, with SE `sqrt(sample_variance(pairScore)/numberOfPairs)`. This is a
large-sample approximation; n>=30 merely permits display and is not an adequacy
or adoption guarantee. Rare outcomes/skew and distribution stability need review.
It suppresses that interval for tiny samples or zero observed pair variance.
It always also reports a conservative bounded-pair interval
`p +/- sqrt(log(40)/(2*n))`, clipped to [0,1], and its Elo transform. This
Hoeffding sensitivity interval does not collapse spuriously for all draws or
identical pair totals. Endpoints 0/1 yield infinite Elo bounds, explicitly stored
as strings. Synthetic tests cover pairing correlation, draws, degenerate samples,
known Elo and incomplete/color-invalid records.

Predeclare which justified method is primary; never select the significant
interval afterward. Small positive gains can qualify, with proportionately
more data and independent confirmation. A favorable point estimate alone cannot.
Use exploratory data to size an independent final fixed sample, not to repeatedly
extend a nearly significant run. One frozen independent finalist avoids carrying
exploratory multiple-testing claims into confirmation; multiple confirmatory
hypotheses require predeclared family error control (e.g. alpha allocation).
If adopting Fastchess SPRT in a future design, prospectively fix elo0<elo1,
model/units and alpha/beta (e.g. .05/.05), use the runner's paired model and
boundaries, and declare a maximum resource budget with inconclusive exhaustion.
SPRT is available upstream but **was not exercised or certified here**. It is
not needed for the verified fixed-sample path. No arbitrary positive-Elo floor
or inherited Phase 1 adoption threshold is introduced.

Crashes, illegal moves, time losses, stalls, invalid loads, unexpected terminal
reasons or incomplete pairs invalidate a readiness/strength pass. Preserve raw
PGNs, failures and adverse scores, investigate, and rerun a predeclared complete
sample only after repair; do not erase losses or relabel interruptions as draws.
No automatic recovery is enabled. The adapter uses a bounded process-tree
timeout, records failed status, validates opening count before play and verifies
input hashes afterward. It independently parses legal PGNs, color/order/prefix,
normal mate/draw endings and whole-request accounting. Strength claims require
complete planned records and no unexplained adverse outcomes. Reconcile any
interrupted continuation explicitly; this adapter does not auto-resume or
fabricate missing results.

## Validation, evidence and limits

Detailed receipts are in [the evidence directory](bre-pair2-baseline-2026-10-09/).
Raw receipts, PGNs and transcripts preserve their generated bytes, including
Windows line endings, empty stderr markers and trailing record separators.
Source/document whitespace checks exclude those intentional raw-output details.
`evaluator-probe.json` covers nine positions: independent TT-off depth-3 versus
production 1/2-worker scores, nonselective results, fresh/rolling/private-state
agreement, information boundaries, STM/material mapping, mate/stalemate and
clock-100 handling. Existing tests retain special-move/undo and codec-family
oracles. The selected model's start static score is 10, mapped from
0.1033839572578545 pawn units; the depth-3 reference score is 1.

Production UCI depth-4 repeats on start/Kiwipete/endgame returned respectively
3,912/7,761/738 whole-request nodes, scores 7/12/6 and legal best moves
`b1c3`/`e2a6`/`b4f4`. Three repeats per position agreed in score, move, PV and
nodes. Timing varied with warm-up/OS/JVM; raw times remain in the receipts.
The 20,000-node budget, movetime, clocks, stop, new-game and terminal probes
passed. Two-worker checks passed without asserting deterministic node counts,
tied move selection or wall time. Wrong hash/checkpoint process tests exited 1
without `uciok`/`readyok`; Java tests also exercised missing/corrupt loads.

The final targeted Java run passed **141 tests in 20 suites**, no failures,
errors or skips. It covers UCI/session/reporting/launcher, Pair-2 equivalence and
benchmarking, production boundaries, TT, repetition, mates, parallel ownership,
draining/lifecycle and time management. Six Python tests passed. Separate broken
UCI fixtures exercised Fastchess crash, illegal-move and time-forfeit handling:
each exited 1 with an interrupted tournament and no completed PGN accepted.
Commands and raw outputs are retained in `runner-failure-checks.json`. A first
fixture launch without an opening book was stopped by strict-mode warnings
before testing the intended fault; supplying the frozen book corrected that setup.
Full training,
slow-NNUE suites, unrelated GUI tests, installers, cross-platform QA, long
strength campaigns and prolonged multiworker stress were intentionally omitted.

The small A/A pilots exercise infrastructure only. All pilot outcomes, including
the initial runner CLI failure before any games, are preserved. See
`pilot-summary.json` for separate results, timing and uncertainty. The first
four games yielded candidate-label W/D/L 3/0/1; reproduction yielded 2/2/0.
Both used only two opening clusters and cannot support a strength claim or be
pooled as independent openings. The final accounting pilot verifies that the
PGN retains aborted-iteration nodes as well as completed-iteration diagnostics.
Its four games yielded 2/1/1. The first accounting analysis rejected the legitimate
`TIME_ALLOCATION` reason; a parser correction and regression check resolved it.
`accounting-original.json` preserves that failed receipt, while
`accounting-reanalysis.json` verifies unchanged original binaries, model, book,
runner and archived adapter bytes, and audits every move's request accounting.
The corrected result covers 351 requests per side: 72,611,951/73,875,094 nodes
and 32.695/32.836 request seconds (baseline/candidate). No additional games were
invented or discarded to repair the analysis.
Fastchess's tiny-sample NaN/zero error bars are not used for acceptance.

Managed promotion history records generation 32 beating its generation-1
incumbent 128/59/69 over 128 pairs at depth 4/four workers, score .615234375,
recorded lower bound .5070582261. This is promotion evidence with its own repeated
selection context, not a new independent Phase 2 result. Historical C02 research
also reports bounded controlled strength evidence for **different research
models** ([recommendation](../brn-architecture-cglhw/RECOMMENDATION.md)). Neither
establishes this exact checkpoint's superiority to HCE, NNUE or mature alternatives.
That uncertainty does not invalidate fixed-evaluator Search comparisons.

Version bookkeeping is a non-blocking warning under the SeedV6 owner override:
the pinned begin helper's prolonged whole-tree capture was stopped before any
operation/reservation existed. Observed status was idle, with no unfinished
operation; there is no active token to finish and no replacement bump is attempted.
Build 35 remains unchanged; no verified bump is claimed. CODEXLOG_CURRENT.md was
absent and was not created. Frozen Phase 1 bytes remain unchanged. No push,
deployment, training, external-store modification or temporary network copy occurred.

The baseline is technically usable for the next authorized SR-001 investigation,
subject to GPT/owner reconciliation/acceptance and mirror synchronization. SR-001
remains PENDING. Long-control strength, mature evaluator comparisons, cross-OS
performance and multiworker strength are unmeasured, not silently certified.

Human actions required after this prompt:

- **Blocking next GPT research launch:** replace both Project Source mirrors from
  `source/CHESS_SEARCH_CONTRACT.md` (P2-R002) and
  `source/CHESS_SEARCH_RESEARCH_FRONTIER.md` (P2-F002), explicitly confirm sync,
  and reconcile/accept this closeout before preparing the next authorized launch.
- No manual cleanup or version repair is required. No next feature is started here.
