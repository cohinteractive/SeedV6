# SR-018 Phase 2: null-move pruning for BRE-Pair 2

Authorized 2026-10-13 local. **ACTIVE** single-feature investigation. Historical
HCE SR-018 stays BASELINE; no Pair-2 production adoption at activation.

## Bootstrap and challenged assumptions

Owner confirms SR-005 IMPLEMENTED acceptance and BOTH P2-R013/P2-F017 mirror
replacements/synchronization. Historical pending statements are satisfied; its
closed confirmation is unchanged. Clean starting HEAD `901842282eb2` matches the
handoff. All 315 main-source files match SR-005 integrated source
`450988455af31383d17d126c6f51d277451f5a18`, and all 15 installation files in
`app/build/sr005-p2-production-v2` match the frozen receipt. Primary comparator
app.jar: `dfc9d9cb3e5d372eb40015fcc5b53093c5841d09ddad2960cc7c54f21bb4abf8`.
The accepted composition is full-window PVS + unpruned qsearch + protected quiet
LMR + WINDOW + EFFORT. Reuse OFF is explicitly not the primary comparator.

The four checkpoint payloads and frozen Phase 1 archives match recorded hashes;
[bootstrap receipt](sr018-phase2-2026-10-13/bootstrap.json) retains identities.
Generation 32, lineage `235d0688-e060-44e3-8f1e-119c39bc8017`, checkpoint
`g000032-s001753088-5837c7f7192aa3cf9a265388f93649327455e804541398ae3ef6b69e09014304`,
network `5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7` remains
fixed. Store: `E:/SeedV6-Networks/BRN-Pair2/235d0688-e060-44e3-8f1e-119c39bc8017`.
Quarter-residual mapping and weights are unchanged. Source inspection verifies
ProductionSearch serves managed consumers and UCI; actual runtime preflight follows.

Read active canon, frozen R045 SR-018 and F052 row, SR-005 and measurement reports,
and available historical `C:/Users/Central/Documents/SeedV6-SR018-2026-09-30/REPORT.txt`.
Phase 1 found pass-assumption errors even without deliberate reduction, parity
effects under static leaves, reduced-verification errors, king/pawn zugzwang and
middlegame/mate failures at insufficient surplus. Nominal verification cost about
4.2% over its control; adaptive reduction lacked stable incremental economics.
Its HCE fixed R=2/d4..6/surplus512 policy saved work but received no strength test.
These are historical findings, neither Pair-2 calibration nor universal prohibitions.

Current qsearch can resolve tactical horizon artifacts while increasing probe cost.
LMR/WINDOW already omit work; EFFORT changes opportunities and move order. A null
probe is isolated from TT, LMR/LMP and history writes, so it may search more legal
work than the baseline branch it replaces. Savings must include this cost and
full-request retries/aborts. Ordinary proof suppression already exists; synthetic
qsearch and prior-PV hints require explicit isolation. Qsearch can extend beyond
the nominal clock guard, so the pass must not manufacture a rule draw. Reduced
legal verification is a separate heuristic filter, never a proof discharge.

Pair-2 magnitude is not WDL centipawn calibration; evaluate gates as mechanism
hypotheses. Both STM tables must be evaluated normally, never inferred by merely
negating the parent evaluation. Existing primitive board/history and selective-epoch
facilities can support scoped research without TT redesign. Correctness and
independent clock-strength confirmation are required before adoption.

Primary reference sources inspected: [Stockfish 17 search](https://github.com/official-stockfish/Stockfish/blob/sf_17/src/search.cpp)
and [Ethereal search](https://github.com/AndyGrant/Ethereal/blob/master/src/search.c).
Their depth/evaluation reductions and verification distinctions motivate hypotheses;
their constants, proof domains and reported strength do not transfer to SeedV6.

## Prospective progression

First implement explicit research-only cumulative EFFORT controls and observation
diagnostics. Examine conservative fixed reduction, shallow opportunities, depth-aware
reduction and selective legal verification where evidence warrants. Preserve root,
check, mate, history and material protections; compare scout provenance with original
narrow windows only as an explicit formulation. No neighboring feature is activated.

Run semantic/adverse witnesses and mechanism diagnostics, then clean rotated
whole-request costs and inexpensive paired clock screens. Freeze each measured
source/install/model and full-history sample. Retain candidate-off identity against
accepted production. Fresh prospective independent confirmation is required for
promising frozen finalists; no development pooling or post-hoc extension.

Reuse four concurrent games, one worker each, if the verified Ryzen 5 5500 / Java21
host and comparable memory/workload remain suitable. Record resource state and A/A;
do not overlap clock games with builds, tests, benchmarks or training. Choose further
investment from mechanism, strength signal, precision and measured cost, not a quota.

## Continuation and bookkeeping

Current resume: bootstrap verified; P2-R014/P2-F018 activates only SR-018. Runtime
baseline preflight and first research implementation/semantic tests are next.
No SR-018 strength result exists yet. No successor work is authorized.

Root journal absent and not created. Pinned local version begin attempted before
mutation; prolonged initial capture stopped before reservation. Local status is
idle, active null, unfinished/blockers empty. Non-blocking version warning; build
35 unchanged, no verified bump, finish, replacement operation or repair pending.
No push, deployment, mirror refresh or SR-018 owner acceptance is claimed.

## Initial candidates and mechanism results

Explicit `seedv6.uci.nmpResearch` controls preserve cumulative EFFORT. OFF is the
accepted comparator. OBSERVE probes but never cuts and removes diagnostic probe
work only from EFFORT's ordering weights (all actual work remains accounted).
FIXED uses R=2 at d>=4; SHALLOW admits d3 with the same reduction, reaching
unpruned qsearch; ADAPTIVE uses R=min(d-2,2+d/4); VERIFIED adds unpruned reduced
legal d-2 verification to FIXED; GUARDED requires 100 units surplus with FIXED.
The zero-margin gate means only E>=beta, not a probability claim. The 100-unit
control tests a material-scale margin; it is not inherited HCE calibration.

All require an actual non-root scout, unchecked ordinary window, own non-pawn
material, >6 pieces and clock+depth<100. The Pair-2 pass preserves the legal clock,
expires EP, toggles side and uses normal evaluator child state. Legal descendants
advance clocks; synthetic history is isolated with no pass occurrence. HCE's
historical clock behavior remains unchanged. The null subtree has no ordinary TT,
LMR/LMP, NMP recursion, prior-PV hints or quiet-history writes. Discarding an isolated
probe restores its epoch; accepted threshold predictions increment it and return
no synthetic PV/mate. Reduced verification retains conservative provenance,
restores generated parent scratch and accounts its same-position admission explicitly.

Seven focused new tests plus HCE/iteration tests pass initially; seventeen adapter
tests pass. The first combined Gradle command put --tests after a JavaExec task and
failed argument parsing before experiments; corrected retry passes. Broad validation
is still running; an early 26-test XML copy is explicitly incomplete and will be
replaced with final results. No failure is concealed as a pass.

The completed 546-observation real-model study covers 26 full-history/adverse roots,
seven controls, D4/D6/100k budgets. OBSERVE exactly matches D4/D6 score, PV, depth and
baseline work plus measured probe work. D6 node ratios versus EFFORT are
FIXED .864, SHALLOW .776, ADAPTIVE .660, VERIFIED .897, GUARDED 1.000; changed best
moves are 3/7/4/3/0. GUARDED attempted only six probes and made five cuts: its
material-scale gate has little opportunity here. VERIFIED accepted all 741 reduced
checks, adding 105,182 recursive nodes; no incremental quality filter was observed
in this corpus. These are diagnostics, not strength conclusions. Callback timing
is not clean whole-request evidence.

A separate ADAPTIVE conditional audit captures full legal histories and original
ply, then reroots an independent unpruned qsearch oracle with mate conversion.
212 sampled fail-highs finish, one disagrees at nominal depth: opening-14, d5,
E=26, beta=-1, synthetic prediction=18, legal oracle=-2. This is a one-unit threshold
failure, not false proof or a missed mate. All raw board/history/PV data are retained
in audit-adaptive-v1.jsonl. Sampling is deterministic and conditional; 1/212 is not
a calibrated population error probability. Captures ran alongside broad tests,
so those timings are excluded from clean economics. Most d7 probe surpluses are
under 32 units, directly warning against transferring HCE's 512 threshold.

Broad Search/rules/UCI validation has now completed: 758 discovered, 747 passed,
11 pre-existing opt-in skips, zero failures/errors. The early copied XML/summary
was replaced with the complete final results. The actual-default cross-binary
bridge passes 64 deterministic comparisons against frozen accepted SR-005 over
full-history D4/D6/node requests, plus budgets, stop and terminal preflights.

Clean cost v1 FIXED completed before a later opening-selection process began.
That selector inadvertently overlapped the remaining cost pipeline; retain those
v1 observations as non-clean diagnostics and repeat SHALLOW/ADAPTIVE/VERIFIED
without the selector before interpreting their timing. No strength games have
started, and no running experiment was interrupted to optimize monitoring.

### Measurement progression and next decision

The 100-unit GUARDED formulation is not promoted to games: it has negligible
observed pruning opportunity, and no identified compensating quality mechanism.
This rejects only that material-scale gate as a current finalist, not all margins.
FIXED/SHALLOW/ADAPTIVE/VERIFIED merit small complete exploratory screens: they test
reduction/horizon scope and legal filtering, show distinct costs or root choices,
and preserve correctness/proof boundaries. No favorable proxy establishes adoption.
Use 16 fresh reversed-color pairs each at 10+0.1, four concurrent games and one
worker, fixed order and sample, plus four separate A/A pairs. Complete all screens
before selecting, except integrity failure. At prior measured ~250 seconds per
32-game screen, the development round costs about 17 minutes and can reject weak
families or justify a fresh frozen confirmation. This is a decision-value estimate,
not a game quota or a promise of strength. Exact source/install/protocol hashes
will be frozen before the first game.

Clean rotated two-round costs now finish (312 requests per comparison):
FIXED/SHALLOW/ADAPTIVE/VERIFIED D6 elapsed ratios .8391/.7494/.6569/.8640;
100k-node aggregate ratios 1.0160/.9768/.9937/1.0199; 1M ratios
.9558/.9546/.9856/.9405. The audit separates pairs that both exhaust identical
node caps from early terminal completion and retains allocation, GC/JIT and mean
completed depths. These are corpus/host observations, not universal throughput
claims. `cost-summary-v1.json` uses only FIXED v1 and the three clean v2 repeats;
overlapped v1 timing is excluded. All seven prospective-assessor unit tests pass.
