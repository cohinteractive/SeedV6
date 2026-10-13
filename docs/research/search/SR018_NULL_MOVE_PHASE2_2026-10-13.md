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

Current resume: P2-R014/P2-F018 activates only SR-018. Code/semantics, real-model
mechanism/audit study and clean costs complete; frozen candidate v1 is source
`ac7891e14ff78c3d9b49b6be21a3e805dc3cc4f1`, install `app/build/sr018-p2-candidates-v1`,
app.jar `7ba54ad20fff03fef1d8673720ac10e333c59c4d50761c1c2730cb5a3b16746b`.
All 708 main classes match the JAR. A/A, four exploratory screens, the historical
control bridge and full-window adverse replays are complete. Unchanged SHALLOW
completed independent confirmation, closed at stage 01 for prespecified
computational futility (99/60/97; adjusted interval crosses zero). Never extend
that sample or pool development into it. Unchanged ADAPTIVE v1 is next for a fresh
64-pair development comparison at 15+0.15; inspect `adaptive-development-v2` and
owned processes before starting/restarting. No production adoption is justified.
No successor work is authorized.

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
failed argument parsing before experiments; corrected retry passes. An early
26-test XML copy was explicitly incomplete; it was replaced with the complete
758-test final result below. No failure is concealed as a pass.

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
E=26, beta=-1, synthetic prediction=18, legal oracle returns a fail-low upper
bound of -2. This proves a false threshold prediction, but the narrow-window
return does not establish exact severity or exclude a deeper mate loss. The later
full-window replay below resolves this witness; no ordinary TT proof was published. All raw board/history/PV data are retained
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
v1 observations as non-clean diagnostics. SHALLOW/ADAPTIVE/VERIFIED were repeated
without the selector before interpreting their timing. This occurred before
strength games; no running experiment was interrupted to optimize monitoring.

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

The frozen OFF cross-binary cost/parity audit passes all 156 comparisons, with
identical score/move/PV/depth/node streams. Relative to accepted production,
elapsed ratios are 1.0277 at D6, .9948 at 100k and 1.0051 at 1M: small mixed host/JIT
variation, not proof of universal zero overhead. Source whitespace validation
passes with CRLF recognized; broad raw evidence diff-check reports Windows
record terminators/console whitespace. Raw evidence bytes are preserved, not
normalized to manufacture a clean evidence check. No source whitespace failure
is outstanding.

The active FIXED screen's short resource sample observes eight JVMs with four
single-worker games and ActiveProcessorCount=1. Working sets differ by side in
this instantaneous sample (~178-204 MiB candidate, ~211-213 MiB baseline); this is
not reported as memory equivalence. Clean equal-1M-node request allocation ratios
are FIXED 1.004, SHALLOW 1.016, ADAPTIVE 1.035 and VERIFIED .990, with sparse GC;
this does not identify a large recurring allocation penalty. JVM commitment,
compiler and between-game/warm-up timing can affect working set, but the cause is
not experimentally isolated. Retain ordinary-host contention and memory/JIT
limitations, and inspect later active policies before confirmation. The initial
host has substantial available memory; no paging/saturation claim is inferred
from a single working-set reading.

The first chat description reversed those side labels; the command-labelled raw
receipt and the corrected values above are authoritative. No candidate working-set
increase was observed in that sample. The correction changes no experiment.

A source review caught that the first chat description treated the audit's -2
fail-low bound as an exact one-unit error. That severity claim is withdrawn.
The false-cutoff classification remains valid; replay the saved full history with
an independent full window after clock screens finish. This is ordinary evidence
remediation, not a reason to interrupt the correctly running frozen screens.

## Complete development and frozen finalist choice

All 128 candidate games and eight separate A/A games complete normally. Independent
PGN legality/full-history/pairing/accounting/input audits pass. Complete fixed
screens (W/D/L): FIXED 10/8/14 (-43.66 point Elo), SHALLOW 15/10/7 (+88.74),
ADAPTIVE 14/6/12 (+21.74), VERIFIED 9/8/15 (-65.92). These are only sixteen pairs
per formulation with wide uncertainty; no ranking superiority or strength adoption
is established. Shared development openings are not independent replications.

Full-window replay corrects the ADAPTIVE adverse witness: exact legal d5 value -6
against beta -1. Isolated full null values for R=0/1/2/3 are -16/+4/-20/+18.
This demonstrates reduction/horizon oscillation under current qsearch too, not
solely historical HCE static-leaf parity. A separate SHALLOW audit completes 216
conditional nominal-reference checks, with one false threshold. Full-window legal
d3 value is +3 versus beta +5; R=0/1/2 null values are -1/+12/+5. Both witnesses
retain full real history, exact PV and raw boards. Neither is a missed mate in
these full replays; no universal tactical-safety inference is made. An initial
SHALLOW replay command accidentally supplied the corpus TSV instead of audit JSON;
it failed before producing results, is retained, and the corrected command passes.

Select unchanged **SHALLOW v1** for first fresh independent confirmation. Its
mechanism adds d3/qsearch probes; stronger clock signal and .7494 clean D6 elapsed
ratio warrant testing. FIXED/VERIFIED provide no positive initial clock signal;
verification filters no observed D6 cutoff and adds cost. ADAPTIVE saves more tree
work but supplies weaker clock evidence and a retained R3 horizon failure; it
remains a credible alternative, not universally rejected. A new arbitrary margin
or reduction sweep is not justified before testing this frozen finalist.

Historical controls bridge passes, including explicit prior qsearch, LMR/LMP,
aspiration, history, check and reuse policies, ordinary HCE and multiworker/default
1->2->1 transitions. No historical control silently selects new NMP. Frozen
candidate code/model/policy is unchanged from development. Eight assessor tests
pass after adding a prospective computational-futility rule, before any confirmation.

Prospective first confirmation uses family alpha .05, index 1 allocation .025,
Bonferroni-adjusted paired-normal intervals at 128/384/1024 cumulative fresh pairs,
required 2.5-SE and adjusted Hoeffding sensitivities. Normal inference requires at
least twenty pairs on each side of half score; otherwise use bounded inference.
Positive lower bounds establish scoped support; negative upper bounds establish
scoped loss; maximum non-decisive samples are inconclusive. Before maximum,
normal-eligible plug-in conditional chance below .10 of passing at 1024 stops for
computational futility. This forecast is not proof of no effect, not a minimum Elo
gain, and cannot increase false adoption probability by adding a success route.
It can reduce power; preserve uncertainty and consider remaining credible mechanisms
at feature level after the closed result. No post-stop extension or pooling.

The exploratory pair variance is .1083 and highly uncertain. A roughly +89 true
Elo gain would often resolve at 128 pairs; smaller effects need more precision.
Prior measured throughput forecasts about 45-50 minutes for the first look and
about six hours if all 1024 pairs are justified. Fresh confirmation offers more
decision value than another large development ranking round because it can
support adoption; first-stage uncertainty/futility guides later investment under
fixed rules. Exact protocol, staged openings, identities and power planning were
frozen in commit `c5b3150` before the first game. Longer-clock/platform/weight strength remains
outside this experiment; 15+0.15 matches the accepted SR-005 control conditions.


## Independent SHALLOW confirmation: closed inconclusive

Stage 01 completes 128 pairs / 256 normal games in 2,817.55 seconds, with
99 wins, 60 draws and 97 losses. Score .50390625, point Elo +2.7144. Adjusted
paired-normal Elo interval [-49.7094,+55.2630]; required 2.5-SE
[-46.9266,+52.4673]; adjusted Hoeffding [-101.7715,+107.7088]. All identity,
opening-exclusion, input-stability, legal-PGN, full-history, pairing and work
accounting audits pass. `confirmation-assessment-01.json` applies the frozen
protocol and closes for **computational futility**: plug-in probability .008093
of passing the maximum look, below .10. This is not inferiority, equivalence or
proof that modest benefit is absent. The result is inconclusive; do not extend
or pool this confirmation, or promote the candidate. Unused stages remain frozen
provenance, not unfinished required experiments.

Clock requests consumed baseline 11,142,247,013 nodes / 5,527.730 seconds and
candidate 11,594,109,396 nodes / 5,532.288 seconds; these include main, qsearch,
probe and aborted work. Candidate aggregate nodes/elapsed throughput is about
4.0% higher, not an equal-work quality measure. Game paths differ. Two active
host samples show balanced CPU and similar working sets, with substantial RAM;
this is not a peak-memory or exclusive-host proof. The 25% diagnostic elapsed
saving did not establish practical strength in the independent sample.

Unchanged ADAPTIVE remains the strongest materially distinct unconfirmed
alternative: about 34% clean D6 elapsed saving, deeper nominal reach, a different
reduction schedule, and a weak positive sixteen-pair development point estimate.
Its recorded horizon failure and uncertain clock effect prohibit adoption, but
justify a fresh complete 64-pair development comparison at 15+0.15. Expected cost
is 22-26 minutes from measured throughput. This can identify a promising deeper
formulation or weaken the remaining benefit hypothesis; it has better decision
value than retuning margins to a witness or extending closed SHALLOW data.
`adaptive-development-v2-protocol.json` is declared before games. It is development
only, with fresh partition0 openings excluded from every earlier receipt. No
arbitrary minimum gain or strength acceptance is inferred. Any ensuing independent
confirmation needs fresh partition1 evidence and alpha allocation index2.
