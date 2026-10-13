# SR-005 Phase 2: iterative-deepening information reuse

Authorized 2026-10-13 local. **ACTIVE**; only SR-005 is authorized. This record
contains evidence and continuation under the active Contract/Frontier.

## Bootstrap and challenge

Clean HEAD `13004b1`, Contract P2-R011 / Frontier P2-F015, PENDING SR-005 and
no ACTIVE feature match the launch. The owner explicitly accepts SR-030 DEFERRED
and confirms BOTH predecessor mirrors replaced and synchronized. Historical
pending statements impose no outstanding predecessor action.

Mandatory accepted comparator: `app/build/sr022-p2-production-v5`, source
`2f08516d0f24c9a08946bff122dcf5f3a0c8667f`, app.jar
`0ca1af7b730040aa26828133539f5c2977ae69fb8cfbcdf233c16db969e6d2c8`.
Full-window PVS + SR-001 unpruned qsearch + SR-029 quiet LMR + SR-022 WINDOW.
ProductionSearch matches that accepted source; current research code includes
SR-023/030 controls, all off. Retained runtime source `41587d2` is not substituted
for the accepted comparator. [Bootstrap](sr005-phase2-2026-10-13/bootstrap.json)
verifies all 19 installation/checkpoint/archive hashes.

Freeze generation 32, lineage `235d0688-e060-44e3-8f1e-119c39bc8017`, checkpoint
`g000032-s001753088-5837c7f7192aa3cf9a265388f93649327455e804541398ae3ef6b69e09014304`,
network `5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7`.
Store `E:/SeedV6-Networks/BRN-Pair2/235d0688-e060-44e3-8f1e-119c39bc8017`;
quarter-residual score mapping remains fixed. Read the baseline and Phase 2
SR-001/029/006/022/023/030 reports and relevant frozen R045/F052 sections.

Phase 1 SR-005 (historical commit `18f5e00`) observed 90 PV candidates through
D6 and 60 more through D10: all matched legal TT moves. It used HCE/static leaves,
PVS, one generation and 64 MiB requested TT. No strength games were reported.
No separate raw SR-005 report/probe was found among retained repository research
or root build directories; the preserved canon and historical commit remain
the available detailed evidence. This limitation does not turn old redundancy
into a current result. Frozen archives remain byte-identical.

Current code materially changes that information relationship: every qsearch
entry increments conservative selective provenance, so affected ancestors and
roots do not save ordinary TT proof or its associated hash move. LMR/LMP add
suppression. The driver keeps only the completed result; it passes neither PV
nor root alternatives back to the next iteration. Main quiet history resets at
each invocation. Existing TT entries still supply legal hints and exact eligible
proof. Prior PV/root/history reuse may therefore add information without changing
TT architecture. Measure coverage and actual ordering, not merely storage.

Primary external references inspected 2026-10-13:
[Stockfish 17 search](https://github.com/official-stockfish/Stockfish/blob/sf_17/src/search.cpp)
retains root scores/PVs and stable root ordering;
[Ethereal search](https://github.com/AndyGrant/Ethereal/blob/master/src/search.c)
offers a separate iterative search composition. These motivate hypotheses, not
copied implementation, transferable constants, proof rules or strength evidence.

## Prospective internal work

1. Quantify completed-PV coverage versus existing TT hints. Compare root-only
   retention and legal exact-prefix PV hints; preserve legal TT precedence.
2. Compare retaining previously searched root alternatives, using bounded
   primitive state and explicitly heuristic scores/effort. PVS fail-low values
   are bounds, not exact rankings. Do not add extra full searches to rank them.
3. Test request-local quiet-history retention from completed iterations as a
   distinct reuse hypothesis; unchanged history indexing/update rules, no
   cross-request persistence or wholesale history architecture change.
4. Study interactions with actual first/global/quiet ordinals, LMR/LMP and PVS,
   finite-resource root choices, complete-request cost and independent references.

Only completed full-window iterations of the same root/history, immutable
evaluator/policy and request may seed reuse. Cancellation, end/new request and
owner changes invalidate it. A hint never supplies a score, PV publication,
deeper depth or TT bound. Exact proof rules and selective suppression stay intact.
No aspiration, IID/IIR, clock policy, TT storage or neighboring feature is added.

Reuse prior four-game concurrency calibration: observed Ryzen 5 5500, six physical
cores/twelve logical, same Java 21/Pair-2 workload and 256/512 MiB heaps with
ActiveProcessorCount=1. SR-029 calibration and completed SR-022/023/030 campaigns
support four concurrent games, one worker per engine. Verify idle resource state,
actual UCI/model/binary identities and A/A before comparisons. Do not overlap
clock games with builds/tests/benchmarks/training. Desktop contention remains a
limitation; reassess material candidate-dependent workload effects.

Initial development uses inexpensive mechanism diagnostics and 16 paired
openings per promoted formulation at 10+0.1, four games, three depth-four warmups,
64 MiB requested TT, no book/tablebase/ponder. Freeze exact samples/configurations
before results. Progressive investment follows joint cost/mechanism/strength
evidence. Screens are exploratory; finalists require fresh prospective independent
confirmation, explicit stopping/error control and positive lower-bound evidence.
No arbitrary minimum gain, automatic game quota or proxy-based adoption.

## Continuation and bookkeeping

Current continuation: the first independent confirmation closes POSITIVE at
128 pairs / 256 normal games under its prospectively adjusted stopping rule.
Proceed to integrate exactly frozen EFFORT v1, preserve explicit OFF and older
controls, and execute the prepared lifecycle, runtime bridge and regression
checks. No further confirmation stages may be run or pooled into this sample.
Technical IMPLEMENTED disposition still depends on integration and validation.

Root journal absent, not created. Local pinned finalizer begin attempted before
mutation; prolonged initial capture stopped before reservation. Subsequent status
idle, active null, unfinished/blockers empty. **Non-blocking version warning**;
build 35 unchanged, no bump/finish/replacement/repair claimed. No push/deployment.

## Initial mechanisms and semantic evidence

All controls are explicit `seedv6.uci.reuseResearch`, production OFF:

| Control | Information consumed at the next completed depth |
| --- | --- |
| observe | Previous PV coverage only; ordering and history remain unchanged. |
| root | Previous root best move, after any legal TT move. |
| pv | Previous PV moves only on the identical real move prefix; after legal TT. |
| score | PV hints plus stable root-alternative ordering by previous fail-soft returns. |
| effort | PV hints plus stable root-alternative ordering by prior inclusive node effort. |
| history | Existing quiet-history table retained across completed adjacent iterations. |
| pvhistory | PV hints plus the same completed-iteration history retention. |

Root alternative returns can be fail-low bounds; they are explicitly heuristic
ranks, not exact scores. No extra searches produce them. Root best remains ahead
of alternative ranks. TT proof processing is unchanged and separate from hint
priority. Generated legal membership validates every consumed hint; following
the exact move prefix from an identical root/full history validates PV context.
The stored immutable history keys and all board/status words are checked at
each iteration boundary. Evaluator/policy are fixed by the private owner factory.
Narrow windows, skipped depths, changed roots/history and incomplete iterations
invalidate retention. End/new request and new game clear eligibility. Qsearch
does not consume hints, and nominal/real/q depths are unchanged.

Only the existing ordering keys change; actual searched indices and quiet counts
still drive LMR/LMP, and prior scores never supply alpha, beta or proof. A nonzero prior-PV
hint (including a tactical hint), or eligible root ranking, forces pending quiet
generation before ordering and therefore can sample quiet history earlier. This
is part of the frozen measured mechanism/cost, not an assumed free lookup. A
possible tactical-hint optimization was not implemented or measured. No
per-node heap allocation is introduced. Root/PV/rank scratch is bounded primitive
owner state. History retains the existing table without adding another table.

Accepted frozen comparator actual-default UCI preflight passes budgets, stop,
terminal, deterministic depth and identity checks. Initial eight focused tests
pass; broad Search/UCI/rules finds 746 tests, **735 passed, 11 historical disabled,
zero failures/errors**. Thirteen Python adapter tests pass. Focused checks cover
observation neutrality, unpruned value agreement, legal PV, request isolation,
aborts, skipped depths/windows/history, terminal/mate/clock boundaries, TT proof
suppression and legal-TT precedence. Additional independent enumeration and UCI
policy gates are being validated before the source freeze.

The real-model diagnostic study completes 624 observations across 26 full-history
roots, eight controls and D4/D6/100k-node resources. Observation-only results
match OFF in score, best move, PV, total nodes, completed depth and selectivity.
Across completed observed iterations there are 1,441 candidate-bearing PV nodes:
144 same TT move, 1,292 absent TT moves, five different TT moves. This directly
refutes transferring Phase 1 redundancy to this cumulative selective baseline.
Counts include repeated requests/modes and are not independent population rates.

| Control | D6 nodes / OFF | Changed D6 best moves (26) | Mean completed depth at 100k |
| --- | ---: | ---: | ---: |
| OFF / observe | 1.0000 | 0 | 7.115 |
| root | .9013 | 8 | 7.731 |
| pv | .7828 | 9 | 8.962 |
| score | .7461 | 9 | 9.000 |
| effort | .7652 | 4 | 8.923 |
| history | .9801 | 6 | 7.462 |
| pvhistory | .7891 | 7 | 8.962 |

These are mechanism/resource observations, not strength. Selective ordering
changes root decisions; greater nominal depth is not presumed more accurate.
The diagnostic run overlapped the broad regression worker, so its instrumented
wall times are excluded from clean cost conclusions. An early results copy
occurred before broad completion; `broad-v1-summary.json` and the copied XML were
subsequently replaced with the complete, verified final 746-test results.
Clean rotated no-callback cost/allocation runs follow after regressions finish.
Raw study, summary, XML and commands are in the linked evidence directory.

### Clean costs and progression

All six separate rotated comparisons complete 312 requests each: 26 roots,
two rounds, two policies, D6/100k/1M resources. Clean D6 elapsed ratios for
root/pv/score/effort/history/pvhistory are **.912/.794/.770/.773/.945/.804**.
At one million nodes they are **.980/.900/.900/.899/.977/.914**. Q/main mix,
completed depth and early terminal exhaustion affect these aggregate ratios;
they are not universal NPS or exact speedup estimates. No callback runs inside
these timed searches. Allocation, compilation and GC totals are retained in
`cost-v1-summary.json`; initial/JIT effects remain observable. Higher completed
depth can allocate more per-request result snapshots despite bounded reuse state.

All six materially distinct controls merit the inexpensive initial clock screen:
root is a causal scope ablation, PV directly restores missing information,
score/effort test alternative evidence, history tests discarded internal learning,
and pvhistory tests the interaction. No superiority ranking or adoption follows
from the cost ordering. The additional 17 focused tests (including independent
enumeration and UCI gates) and all 14 adapter tests pass. An initial opening
selection attempt included a noncanonical selection receipt lacking positionKeys;
it failed before creating a sample or running a game. The corrected selector
uses only canonical full-position selection receipts and retains exclusions.
Automatic approval review rejected removing generated binary test logs with
only "blocked by policy" supplied as the reason. These harmless task artifacts
are retained with the XML; they do not obstruct research or require cleanup.

### Frozen v1 and prospective clock screens

Runtime source `4ecaf783b57d42e82b07da109ce4d264f74c55df`, installation
`app/build/sr005-p2-candidates-v1`, app.jar
`25722f26ce1316871f85e0df5951f472625796c57a2e07fe346131822ae036db`.
All 706 main classes match the frozen JAR, clean source label, build 35.
`candidate-v1.json` binds all installation/main-source bytes. Actual-default
cross-binary UCI bridge passes 64 deterministic comparisons against accepted
`2f08516` (D4, D6, 20k/100k nodes and full histories); preflight also exercises
clock/stop/terminal behavior. These are runtime observations, not static claims.

`screen-protocol-v1.json` prospectively fixes root, pv, score, effort, history,
pvhistory in that order, 16 complete pairs each at 10+0.1, seed 20261401,
partition 0, four games and one worker. Openings exclude 26 canonical prior
selection receipts. Four A/A pairs at 5+0.05 precede these screens, using a
fixed first-four subset only for infrastructure. Every engine is hash-checked
with the unchanged generation-32 checkpoint, mapping and prior operational
controls. No builds/tests/benchmarks/training run alongside clock games.

Host remains Windows 11 10.0.26300, Ryzen 5 5500 6C/12T, Java21, Balanced power;
about 11 GiB free before startup. No Search/test/research JVM was active at the
pre-game inventory; idle Gradle/IDE services remain. `host-before-screens.json`
retains the measured snapshot. This supports reuse of prior four-game calibration
without claiming exclusive host isolation or universal optimal concurrency.

### Preflight adapter correction (before candidate games)

All eight A/A games pass normal-termination, pairing/history, accounting and
stable-input audits. The first root screen then fails preflight, before any
candidate game: the adapter's pre-existing default-LMP validator rejected the
correct explicit reuse `experimental=true` identity. `screen-root/` is retained
as a failed zero-game attempt. Engine code, weights and frozen installation did
not change. The correction requires the explicitly requested reuse name and
experimental flag, preserving strict default/off checks; all 15 adapter tests
pass, including mismatch and missing-flag cases. `screen-protocol-v2.json` and
`search-baseline-v2.py` freeze this correction before outcomes, keeping the same
six formulations, sample, resource controls and complete-screen rule. Outputs
are `screen-v2-POLICY/`; no failed evidence is pooled or hidden.

The pre-start instantaneous CPU reading was 87 percent. The short subsequent
process-delta sample (`host-early-screen-cpu.json`) found low background usage;
it occurred after the preflight failure, with no game JVM active. It is not a
sample of active tournament load. Resource sampling during valid games remains
separate; neither snapshot establishes long-run CPU isolation.

The bounded active-game resource sample (`host-valid-screen-sample.json`)
observes eight game JVMs around 220-223 MB working set each, all with
ActiveProcessorCount=1. Over three seconds their summed CPU is about one third
of twelve logical CPUs, consistent with four active single-worker games; clocks
alternate the active side. Baseline/candidate memory and CPU are comparable in
this sample. It is not peak memory, affinity, or a universal scheduling proof.

### Prepared confirmation accounting (before development selection)

A dedicated assessor is prepared, not yet executed on strength outcomes. It
retains prior auditable pair-level staging at 128/384/1024 cumulative pairs,
required two-sided outcome support for the normal approximation, adjusted
bounded-pair sensitivity and legal/input/resource audits. To account for any
subsequent fresh finalist confirmation within SR-005, prospectively spend family
alpha .05 as .05/2^confirmation-index, starting at index 1 (.025), split across
three looks. Development remains outside every confirmation. A later independent
confirmation uses a new index, fresh openings and frozen prospective identities;
it never extends a closed sample. This reserves error budget without requiring
another finalist or imposing a minimum gain. Normal inference remains approximate.
The exact finalist/protocol, computation-value estimate, independent sample and
assessor hash will be frozen only after the complete development review.

D6 diagnostic event totals further explain the changed composition: relative to
observation-neutral OFF, PV has .761x scouts, .443x full PVS re-searches, .789x
cutoffs, .835x reductions and .854x omissions. SCORE has .743/.417/.759/.822/.826;
EFFORT .744/.443/.770/.822/.822; HISTORY .864/.695/.887/.904/.883;
PVHISTORY .740/.402/.771/.832/.821. These are whole completed-iteration counts,
not conditional accuracy or aggressiveness rates. First/second hinted moves
naturally fall within existing searched-quiet protection; no reduction or pruning
schedule is changed. Quiet generation timing and different alpha discovery can
change descendant ordering, history updates, selective work and root choices.

### Complete development and finalist choice

All six fixed screens finish: 192 normal games, complete paired/history/legal,
whole-request and frozen-input audits. `tools/analyze-sr005.py` independently
reparses every PGN and verifies all configurations/identities. Its initial path
comparison treated Windows slash spellings as different; normalizing only path
arguments to resolved paths corrected that audit assertion without changing
inputs, games or semantic checks. `screen-summary-v1.json` is the passed audit.

| Control | W/D/L | Exploratory point Elo |
| --- | --- | ---: |
| root | 12/9/11 | +10.86 |
| pv | 14/6/12 | +21.74 |
| score | 17/7/8 | +100.42 |
| effort | 17/8/7 | +112.33 |
| history | 18/6/8 | +112.33 |
| pvhistory | 18/4/10 | +88.74 |

Each is only 16 pairs, with wide retained Hoeffding intervals and no normal
interval inferred by the small-sample adapter. No ranking, universal superiority
or adoption is established. Shared exploratory openings create correlated
comparisons; they are not pooled as six independent replications.

Select **EFFORT v1** for first independent confirmation: it combines missing-PV
recovery with actually observed root-alternative effort, has .773x clean D6
elapsed/.765x nodes, a promising clock signal and four changed D6 root choices.
SCORE has similar costs/results but ranks incomparable fail-low returns;
EFFORT avoids that particular measurement interpretation. HISTORY is a credible
materially distinct alternative with less measured cost saving. Neither is
rejected; the tiny screens do not establish that EFFORT is stronger than them.
PVHISTORY demonstrates an interaction worth retaining without proving that
combining all information improves strength. No arbitrary parameter sweep or
new combination is needed before testing whether this frozen finalist gains.

Direct independent confirmation has greater decision value than another large
exploratory ranking round: any selected formulation would still need fresh
confirmation, and the mechanism/cost evidence already supports a plausible gain.
`confirmation-power-planning.json` records variance/power scenarios and scaled
wall-time estimates before computation is allocated. Seven statistical tests
pass, including repeated-look adjustment, family spending, sparse/degenerate
outcomes, cluster variance and invalid looks. No minimum Elo is imposed.

## Confirmation execution and integration boundary review

The complete first confirmation was launched after protocol/evidence commit
`f0fdbd3`; it runs only the frozen sequential stages and assesses only completed
looks. No builds, benchmarks or regression workers overlap these clock games.
The adapter waits for its owned runner, preserves PGN/receipts and audits all
normal terminations and inputs before the independent assessor can decide.

Static integration review identifies the required boundaries if confirmation is
positive. `ProductionSearch.create` is the shared single-worker Pair-2 construction
for managed play, self-play and validation. Its two-worker/other-evaluator branch
must remain static. A new explicit reuse-OFF control must retain accepted WINDOW
LMR/qsearch composition and the production lifecycle constructor's optional book
and configured tablebase; the generic research supplier constructor intentionally
has neither. UCI needs to distinguish omitted/default reuse from explicit OFF,
resolve actual policy after Threads changes, and keep every older experimental
control independent. Preserve the measured recursive classes and bridge actual
default against frozen EFFORT, explicit OFF against the accepted comparator.
This is conditional integration planning, not an adoption claim or source change.

### Implications confined to future research

The Phase 1 redundancy result was composition-dependent: selective proof-store
suppression also removes many potential hash hints. This motivates distinguishing
heuristic information availability from mathematical TT evidence; it does not
authorize a new TT architecture. SR-004/007 could revisit the information boundary
only under separate authorization. The present adjacent full-window eligibility
would deliberately invalidate on aspiration/zero-window retry sequences.

Ordering and selectivity interact through actual searched prefixes. A legal
previous-PV move receives earlier ordinal protection naturally; no additional
LMR/LMP exemption or fabricated searched ordinal is introduced. Root effort is
inclusive actually admitted work, not quality or exact value. The root best line
remains ahead of ranked alternatives, behind any legal TT hint. A truncated
previous line only supplies the available prefix, never invented continuation.

SR-014/016 may later study composition with history, but the six exploratory
formulations do not establish component additivity or rank-order superiority.
SR-034 might care about changed completed-depth distribution and aborted work;
this feature does not change clock management. No neighboring feature is active.

### Equal-admission cost clarification

Post-run descriptive stratification of the unchanged clean EFFORT cost data
(`cost-effort-equal-work.json`) finds 40 matched requests at each finite cap where
both policies admit exactly 100k/1M nodes and terminate NODE_LIMIT. Elapsed ratios
are **1.01135/1.01015**, throughput ratios **.98877/.98995**. Thus the useful D6
saving is principally less Search work, with roughly one percent lower observed
throughput on the matched full-budget subset. This is a descriptive local/JIT/mix
measurement, not a universal mechanical overhead estimate; no strength outcomes
or games are excluded. Aggregate finite-budget .909/.899 time ratios also include
positions that complete early, so cannot be called fixed-work speedups.

The bounded active-confirmation resource sample observes eight owned JVMs at
215-221 MiB and combined CPU around .326 of twelve logical CPUs, consistent with
four active single-worker games. It reads only process/resource metadata, no
partial outcomes; it is not exclusive-host or peak-resource verification.

Static review during confirmation also prepared two additional lifecycle tests:
a changed root followed by a same-root new request without newGame, and injected
evaluator failure between completed iterations. They are source-only pending
post-clock execution; they do not modify the frozen engine or confirmation.
`verify-sr005-bridge.py` and `verify-sr005-controls.py` prepare deterministic
cross-binary and 1->2->1 worker/default-control verification for any justified
integration. Do not claim these new checks passed until their retained results exist.

## Independent confirmation: positive, closed

The first complete look passes every frozen-input, legality, real-history,
reversed-color pairing, normal-termination and whole-request accounting audit.
All **256 games** are normal. Candidate W/D/L: **130/58/68**. Score .62109375,
**+85.85016 Elo**; pair pentanomial **[9,14,41,34,30]**. Pair sample variance
.08561454, standard error .02586240; 23 pairs below half and 64 above satisfy
the prespecified normal-eligibility condition.

With first-confirmation alpha .025, Bonferroni across three prospective looks
(z=2.638257), the primary adjusted paired-normal interval is **[+36.87,+138.45]**.
The required 2.5-SE interval **[+39.38,+135.56]** also passes. Adjusted bounded-pair
Hoeffding **[-17.54,+207.38]** includes zero: support is approximate normal
inference, not distribution-free confirmation. The frozen positive criterion is
met without a minimum-gain rule. Development and A/A games are not pooled.

The prospective stopping rule closes confirmation after stage one; stages two
and three are not started. Their preselected openings remain retained, unplayed
protocol inputs. No extension, retuning, within-stage decision or failed-game
exclusion occurred. Elapsed wall time is 2784.15 seconds, versus the rough 2,991-second
planning estimate. The two optional later stages consume no computation. Source,
model, installation and all 68 frozen input hashes remain verified by the assessor.

`confirmation-assessment-01.json` and `confirmation-stage-01/` retain the complete
statistical/legal/resource audit, PGNs, runner/UCI logs and identities. This
supports integrating the exact measured EFFORT formulation; it does not prove
EFFORT superior to the other exploratory variants, optimality, component
additivity, or strength under other time controls, models or worker counts.

## Production integration and validation in progress

The canonical single-worker Pair-2 factory now selects frozen EFFORT, while
`createIterationReuseControl`/`iterationReuseControl` preserve accepted pre-SR-005
WINDOW composition and production book/tablebase facilities. UCI omitted or
`reuseResearch=production` follows the actual production default; explicit OFF
selects the preserved control. Older research controls retain their measured
pre-SR-005 compositions, reporting actual reuse OFF; explicit reuse variants
remain separately selectable. Other evaluators and multiworker remain static.
The recursive `ExactSearch`, `IterationReuse` and `ExactSearchAdapter` source
bytes are unchanged from measured v1.

Initial integration tests expose a false test-fixture premise, not a Search
failure: zero-residual/material starting-position EFFORT and OFF both admit
1,130 nodes. The new factory-routing test's required node difference is made
meaningful with fixed seeded nonzero tuple weights, without training or touching
the principal model. The node-difference assertion is retained, as are output,
PV, completion, budget and managed-service equivalence assertions. Both failed
attempt XML and logs are retained. All **26 focused integration tests then pass**,
including the two additional lifecycle witnesses and book-enabled production/OFF
services. PowerShell labeled the successful second command exit 1 on the Java
incubator warning; Gradle reports BUILD SUCCESSFUL and all 26 XML cases pass.
Subsequent build commands capture native exit status directly. All 16 updated
adapter tests pass; OFF now sends an explicit property, while `default` omits it
and demands actual EFFORT only in the eligible production composition.

The post-integration broad Search/UCI/rules suite plus two focused BRN2/BRN3
consumer checks completes with native Gradle exit 0: **752 tests discovered,
741 passed, 11 pre-existing disabled, zero failures/errors**. Full XML and native
console output are retained. This includes independent reference/proof, rule and
mate/history boundaries, actual LMR/LMP ordinals, cancellation/accounting,
managed owners, unchanged evaluator/worker paths, self-play/validation consumer
routing and application-JAR separation. No full training or GUI suite was run.
The measured core/adapter source-byte check and focused source whitespace check
also pass. Next freeze a clean integrated installation and verify real-model
runtime bridges, retained controls and worker transitions before disposition.
