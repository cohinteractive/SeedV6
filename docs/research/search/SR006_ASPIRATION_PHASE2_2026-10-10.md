# SR-006 Phase 2: aspiration windows

2026-10-10 local. **ACTIVE**, authorized feature CGLHW. Only SR-006 is selected.
This report and the existing Frontier provide continuity; the Search Contract
remains governing authority. No aspiration adoption or strength claim yet.

## Bootstrap and historical challenge

Clean starting HEAD `a65a6ed42fe4a8cb7e08b12c0e714e0476eb78e6`, Contract
P2-R004 / Frontier P2-F006. The launch confirms SR-029 owner/GPT acceptance and
both mirror replacements/synchronization. Earlier pending language is historical.
Actual code selects unpruned qsearch plus protected quiet LMR for single-worker
Pair-2. Mandatory frozen cumulative control is `app/build/sr029-p2-production-final`,
source `8940a5f9bf35e3dbb4012b88fdcce4f7294f108e`, app.jar SHA-256
`50976cb719da6e75ef2d63ad836f88ba26514f35e6ead1594801523663181dd2`.
[Preflight](sr006-phase2-2026-10-10/baseline-preflight/run.json) passes actual
UCI identity, deterministic depth, node/time/stop/terminal and stable-input checks.
An initial CLI spelling error exited before any run; corrected to `--baseline-qpolicy`.

All four generation-32 checkpoint hashes match the measurement baseline at
`E:/SeedV6-Networks/BRN-Pair2/235d0688-e060-44e3-8f1e-119c39bc8017`.
The exact checkpoint, mapping and network hash remain unchanged; no latest/Play
substitution, training or store mutation. Both frozen R045/F052 archive hashes
match the active Contract and their bytes remain untouched. Root journal absent.
Pinned version helper hashes verified; local begin was stopped during prolonged
capture before reservation. Subsequent status idle, no token/unfinished/blocker.
Non-blocking version warning; build 35 retained, no finish or replacement bump due.

Read active canon, historical SR-006 R045/F052 entries, retained
`AspirationResearch{,Search,Test}`, and local raw `build/sr006b-final` / `build/sr006c`.
Their 33-position first-repeat node totals reproduce historical conclusions:
CONTROL 263,152,114; ASP-512 264,838,090; ASP-628 262,493,617;
ADAPT-172/628 264,636,284. Fixed immediate-full retry and stable/volatile widths
preserved bounded exact semantics but offered no established wall-time benefit.
The old HCE widths, score parity and cheap TT-assisted retry findings do not
transfer numerically to Pair-2 qsearch/LMR. No Phase 1 strength evidence exists
for these policies. Retained tests cover equality boundaries, mate discovery,
original-window TT types, same-generation retries and cancellation between attempts.

Read SR-001 and SR-029 reports, actual adapter/driver/PVS/TT/lifecycle/UCI seams.
Current qsearch and reduced dependencies suppress ordinary nominal TT stores;
the root often has no reusable proof or hash move. Quiet history resets on every
invocation. Aspiration can therefore duplicate expensive work without the cheap
fallback of the historical experiment. It can also change selective decisions,
so full-window equality is mandatory for exact controls, not assumed for LMR.
Changing TT/history policy to make aspiration win would confound this feature.

The versioned [Stockfish 17.1 source](https://github.com/official-stockfish/Stockfish/blob/sf_17.1/src/search.cpp)
motivates gradual directional widening and volatility-dependent windows.
Its numerical widths, score averaging, root sorting, depth reductions and
selective semantics are not imported. Every candidate here searches the same
nominal depth on retries and preserves original invocation windows and proof.

Retained SR-029 depth-six diagnostic data provide only an initial hypothesis:
96 adjacent ordinary-score changes per policy, quiet LMR median 3, 90th-percentile
14, 92/96 at most 16, maximum 157 internal units. Unpruned qsearch median 4,
90th-percentile 11.9, maximum 79. These small tactical-corpus samples neither
calibrate centipawns nor prove future-window coverage. Fresh opening samples,
resource comparisons and actual games must challenge them.

## Prospective investigation

Compare fixed narrow one-shot/full fallback, wider one-shot, directional geometric
widening and recent-volatility-adaptive widening. Initial exploratory widths
16 and 64 derive from the observed Pair-2 movements, not HCE/Stockfish margins.
No arbitrary grid sweep. Start only after semantic tests; adapt materially distinct
hypotheses to measured failure/retry/root-decision costs.

Depth one, missing prior adjacent completed score, and mate-domain history use
full windows. Narrow endpoints exclude mate scores. Only a strictly inside-window
completed return resolves aspiration; equality fails. A mate return or bounded
retry exhaustion triggers a full window. Cancellation or exhausted budget leaves
the prior completed iteration, with every attempt and aborted child accounted.
All retries share request generation, immutable root/history, depth and evaluator;
no failed bound is relabeled exact or published as a completed iteration.

Use the frozen model, Java 21 baseline flags, three untimed depth-four warmups,
one worker, requested 64 MiB TT, book/tablebases/ponder off. Freeze installations,
commands and hashes. Timed work excludes builds/tests/training. Up to four
independent games may run concurrently, as validated by SR-029; this does not
enable multiworker Search. Record host conditions and preserve failures.

Initial diagnostics use preserved tactical/history roots and fresh partition-0
openings. Measure score changes, retries/direction, failed/aborted costs, legal PV,
root decisions, depth, actual main/q work and whole-request time independently.
Initial exploratory games: 32 pairs per credible family, 10+0.1, partition 0,
seed 20261060, fixed complete samples with no significance-based stopping.
These guide candidate selection, not acceptance. Larger/longer comparisons may
follow evidence. Before independent confirmation, freeze one finalist and declare
fresh partition-1 seed, fixed sample, clock, primary interval/acceptance criterion
and statistical sensitivity. No exploratory pooling or post-hoc sample extension.
Playing strength, with adequate positive evidence, is required for adoption;
efficiency and deeper nominal results alone cannot establish a gain.

## Current continuation

Bootstrap passed; Frontier P2-F007 activates only SR-006. Next implement explicit
research-only driver/adapter windows, correctness tests and broader diagnostics.
Production defaults, exact/reference, HCE and multiworker paths remain unchanged.
No external action blocks this internal work. At feature handback, owner/GPT
reconciliation and replacement/confirmation of both current Project Source mirrors
will be required before a successor can be planned or launched. No deployment.
