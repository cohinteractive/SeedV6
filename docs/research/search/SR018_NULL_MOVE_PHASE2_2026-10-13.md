# SR-018 Phase 2: null-move pruning for BRE-Pair 2

Authorized 2026-10-13 local. **DEFERRED** technical closeout: no confirmed Pair-2
strength gain and no production adoption. Historical HCE SR-018 stays BASELINE.
Owner/GPT reconciliation and both Project Source mirror updates remain external gates.

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

Final continuation state: SR-018 is technically DEFERRED at P2-R015/P2-F019.
Code/semantics, real-model studies, costs and all selected games/audits are complete.
Frozen research candidate v1 is source
`ac7891e14ff78c3d9b49b6be21a3e805dc3cc4f1`, install `app/build/sr018-p2-candidates-v1`,
app.jar `7ba54ad20fff03fef1d8673720ac10e333c59c4d50761c1c2730cb5a3b16746b`.
All 708 main classes match the JAR. SHALLOW confirmation closed at stage 01 for
prespecified computational futility (99/60/97; adjusted interval crosses zero).
Never extend that sample or pool development into it. Fresh ADAPTIVE development
also completes (49/33/46 over 64 pairs), with no established benefit. Nothing is
running or queued for automatic extension. No production adoption is justified.
No successor work is authorized; any resumption needs a new owner/GPT selection.

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

### Prospective screen decision, recorded before games

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
were frozen before the first game.

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
The false-cutoff classification remains valid. The saved full history was replayed
with an independent full window after clock screens finished, as reported below.
This ordinary evidence correction did not interrupt the frozen screens.

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


## Final disposition and decision value

**DEFERRED for the bounded single-worker BRE-Pair 2 extension.** The historical
HCE SR-018 functionality remains BASELINE. This is technical closeout, not owner
acceptance, rejection of all null-move formulations, or deployment completion.
No feature remains ACTIVE and no successor is selected or investigated.

The fresh, complete ADAPTIVE follow-up finishes 64 pairs / 128 normal games in
1,360.29 seconds: **49/33/46 W/D/L**, score .51171875, +8.1445 point Elo.
Exploratory paired-normal 95% interval [-41.4474,+58.0728], bounded Hoeffding
[-113.7138,+132.1277]. `adaptive-development-v2-summary.json` independently passes
all frozen-identity, complete-PGN, pairing, legality, history and accounting checks.
The earlier four screens also re-audit unchanged after the analyzer gained an
optional protocol path. The ADAPTIVE follow-up is not independent confirmation,
and is not pooled with its earlier 10+0.1 screen or SHALLOW confirmation.

Five pruning formulations plus OFF/OBSERVE were investigated. FIXED and VERIFIED
had adverse initial clock signals; reduced verification accepted every sampled
D6 cutoff while adding work. GUARDED100 had negligible opportunity. SHALLOW's
promising short development signal regressed to parity in independent evidence;
ADAPTIVE's materially larger tree savings also produced a weak expanded clock
signal. This weakens the current formulations' adoption case without establishing
inferiority or equivalence. No candidate becomes production on speed alone.

`final-decision-value.json` makes the final allocation decision explicit. A fresh
ADAPTIVE confirmation would spend index2 alpha .0125, with approximate 45/136/363
minutes at 128/384/1024 pairs. Its uncertain observed mean/variance give plug-in
positive-look forecasts of .8%/1.9%/5.8%. A deliberately wider normal predictive
sensitivity including development-mean uncertainty gives 8.2%/21.7%/35.1%; this
is not a calibrated probability and demonstrates that worthwhile benefit remains
possible. Further measurement could change the decision, but existing weak clock
signals, closed SHALLOW futility, unproductive verification and negligible strong
margin opportunity do not justify that additional 45-minute to six-hour investment
now. No universal budget, minimum Elo or mandatory sample count caused closeout.
The prescribed unused SHALLOW stages are closed provenance, not pending work.

The unresolved hypothesis is a modest benefit from a better bounded depth/gate/
verification formulation under this evaluator. Useful resumption evidence would
be a mechanism that predicts legal-versus-synthetic threshold errors across fresh
histories (rather than fitting the two saved witnesses), a stronger fresh
resource-matched development signal, or measurement conditions making adequate
precision inexpensive enough to justify a new confirmation. A changed cumulative
Search/evaluator also warrants reassessment. These are useful reasons to resume,
not immutable restrictions on a future authorized investigation. Preserve the
closed result; any later confirmation must use fresh disjoint data, declared
error allocation and stopping rules. Do not reuse reserved confirmation openings.

## Retained production, evidence and validation

Production remains full-window PVS + SR-001 unpruned qsearch + protected SR-029
quiet LMR + SR-022 WINDOW + SR-005 EFFORT. `ProductionSearch.java` is unchanged.
Omitted or `off` NMP preserves this cumulative baseline; reuse OFF deliberately
selects the older pre-SR-005 control. Non-off NMP requires explicit single-worker
Pair-2 configuration and reports its actual experimental policy. Managed owners,
other evaluators, HCE startup, multiworker, independent references and historical
research controls retain their existing selection paths. The new code is retained
only to reproduce the investigation; no measured formulation was integrated as
a production default, so no production-integration claim or release build is made.

Clean D6 node/elapsed ratios (candidate / EFFORT): FIXED .8639/.8391,
SHALLOW .7757/.7494, ADAPTIVE .6602/.6569, VERIFIED .8972/.8640. Equal-1M-node
elapsed ratios are .9554/.9542/.9858/.9408; allocation ratios
1.0044/1.0155/1.0351/.9901. These corpus/JIT observations show tree savings without
a material recurring throughput penalty, not universally faster Search. ADAPTIVE
clock requests consume 5,280,031,997 candidate nodes in 2,658.524 seconds versus
5,124,605,451 in 2,653.477 seconds, approximately 2.8% higher aggregate
nodes/elapsed throughput. Different
game paths prevent equal-work or strength inference from that aggregate.

| Validation / evidence | Completed outcome |
| --- | --- |
| Broad Search/rules/UCI Gradle suite | 758 discovered; 747 passed, 11 pre-existing opt-in skips; zero failures/errors. Includes independent exact/reference, TT, rule/history/mate, PVS, cancellation, owner/lifecycle and production boundaries. |
| Focused new semantics and UCI gating | OFF parity, observation neutrality, synthetic status/EP/clock/history, TT exclusion/selective threshold/PV, root/check/sparse/mate/rule guards, cancellation/reuse and reduced-verification scratch/accounting; all passed. |
| Python measurement adapter and confirmation assessor | 17 adapter tests and 8 assessor tests passed; repeated-look, multiplicity, sensitivity, degeneracy and futility decisions covered. |
| Real-model study | 546 observations across 26 full-history/adverse roots, seven controls and D4/D6/100k requests; observation neutrality and work accounting verified. |
| Conditional independent nominal-reference audits | ADAPTIVE 212 complete checks with one false cutoff; SHALLOW 216 with one; no incomplete oracle checks. Full-window legal replay gives -6 vs beta -1 and +3 vs beta +5 respectively. These deterministic conditional samples are not population error rates. |
| Default/OFF bridges | 64 deterministic accepted-production comparisons plus budgets/stop/terminals; separate 156-comparison frozen-binary cost/parity audit passed. |
| Historical control bridge | 291 deterministic comparisons passed, including historical q/LMR/LMP/aspiration/history/check/reuse, HCE, multiworker and 1->2->1 transitions; invalid evaluator selection fails closed. |
| Clean costs | Four comparisons, 312 requests each; initial selector-overlapped timing retained but excluded, replaced by clean repeats. |
| Clock evidence | Eight A/A games, 256 development games and 256 independent-confirmation games; all 520 normal endings, complete legal/history/pairing/input/work audits passed. Development samples and clocks are not pooled for inference. |
| Final integrity | Frozen executable/model/source/archive hashes, unchanged production factory/version bytes, source whitespace, evidence receipts and absence of owned runners checked in `closeout-verification.json`. |

No additional broad rerun is needed after confirmation: application main sources
have not changed since the frozen candidate and successful broad suite. The only
later source changes are witness tooling and the optional analysis protocol path;
both were executed successfully. Deliberately not run: unrelated fullCheck/training,
GUI/browser QA (no GUI change), longer-clock strength, other checkpoints/platforms,
multiworker strength and release/store packaging. They are outside this scoped
claim; default multiworker correctness/control behavior was validated.

The frozen baseline/candidate identities are above and in `bootstrap.json` and
`candidate-v1.json`. Per-run `run.json` records exact Java/runner/options/model/
file hashes and commands; PGNs preserve full opening histories and request metrics.
`confirmation-protocol.json` plus `tools/sr018-confirmation.py` reproduces the
independent decision; `tools/analyze-sr018.py` audits development receipts, and
`tools/analyze-sr018-cost.py` audits selected clean cost files. Gradle tasks
`sr018Study`, `sr018Cost`, `sr018Audit` and `sr018Witness` retain semantic/cost/witness
reproduction. Frozen installs under ignored `app/build` are intentionally retained,
not abandoned temporary files. No manual cleanup is required.

Cross-feature implications stay within evidence: qsearch does not eliminate
reduction/horizon oscillation; EFFORT observation studies must avoid probe-driven
ordering feedback while counting all work; reduced legal verification is not
nominal proof; neural surplus is not HCE/WDL calibration; and clock savings cannot
be inferred from node savings. These inform SR-001/005/019/022/029 and related
selective research without activating or implementing any other feature.

## Canon and human handback

CANON_UPDATED: P2-R013 -> P2-R015 (activation P2-R014, closeout P2-R015).
FRONTIER_UPDATED: P2-F017 -> P2-F019 (activation P2-F018, closeout P2-F019).
PROJECT_SOURCE_REFRESH_REQUIRED: YES.

The exact current masters are `source/CHESS_SEARCH_CONTRACT.md` **P2-R015** and
`source/CHESS_SEARCH_RESEARCH_FRONTIER.md` **P2-F019**. Frozen R045/F052 remain
byte-identical. Attributable boundaries are `ac7891e` (research implementation),
`c5b3150` (frozen finalist/protocol) and `3db1bde` (closed confirmation/follow-up),
plus this technical closeout's evidence/canon commit. Starting worktree was clean;
no inherited/unrelated changes are incorporated. Final commit and Git state are
reported with handback. Build 35 remains unchanged with the non-blocking version
initialization warning described above; no active token, finish, replacement bump,
version repair, push, deployment or mirror synchronization is claimed.

Human actions required after this prompt:
- **Blocking dependent programme planning:** GPT/owner reconciles and accepts or
  redirects this SR-018 DEFERRED technical disposition.
- **Blocking reliance on Project Sources and any subsequent programme launch:**
  owner replaces BOTH mirrors using the P2-R015 Contract and P2-F019 Frontier
  masters, then explicitly confirms synchronization.
- **Non-blocking actions: None.** No cleanup or version repair is requested.
