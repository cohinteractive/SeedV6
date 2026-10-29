# BRN research assessment and deliberate decision pause

Workstream `seedv6-brn-relational-20261029`, 29 October 2026. Status:
**PAUSED - AWAITING HUMAN RESEARCH DECISION**. This assessment implements the
owner's controlled-pause instruction. It neither closes the programme nor changes
[CONTRACT revision 2](CONTRACT.md) or the [neural research priority](PRIORITY.md).
The recommendation below is Codex's assessment, not an accepted owner decision.

**Recommendation: SUSPEND further open-ended or substantial resource investment.**
Preserve the models, evidence and implementation. The programme made demonstrable
representational and predictive progress, but has not demonstrated competitive
playing strength or a favorable practical strength/cost trade-off. The strongest
played neural family is substantially slower than CP64 in the measured searches.
Recent extensions have yielded small, inconsistent quality gains or failed cost
gates. This supports suspending the tested programme, not a claim that relational
neural evaluators inherently cannot match NNUE. One small optional pivot could
provide decision-changing evidence; it is specified below and is **not authorized
to run by this assessment**.

## Stop, evidence boundary and preservation

The last operation, R056's cost/capacity audit, completed successfully at
2026-10-29T02:27:19Z before the pause. Its eight focused tests had also completed.
No R056 candidate training, dataset generation or games started. R055's two seeds,
two epochs per seed and all 32 checkpoint segments are complete. Nothing was
interrupted by this steering, and no completed work needs repetition.

At the stop verification the supervisor reported `active:null` and
`owned_processes:0`; a CIM command-line inspection found no matching BRN research
process. There was no child to kill. The shared supervisor remains NORMAL for
other workstreams; this does not admit BRN work through the human decision gate.
[pause-20261029.json](pause-20261029.json) records the stop and final verification.
The programme goal is paused. No empirical work was launched to write this review.

The review uses protocols, source, saved measurements and local artifacts through
R056. [assessment-20261029-evidence.json](assessment-20261029-evidence.json) pins
the reviewed reports, source revision, retained checkpoints and direct recounts
of the five main neural match stages. Those recounts verify recorded scores, not
new engine execution or a new independent legal replay. Existing experiment
manifests retain runtime, input, seed, optimizer, cursor and validation bindings.
Raw evidence remains ignored and local; a fresh checkout alone cannot reproduce
the review. Missing artifacts must be restored or handled as a separately named
reproduction, never silently replaced.

R056's already tested implementation and negative cost result are preserved at
source revision `a9091bc7`; its parent was
`da583170e33231274ab7e833a5944c968d9e57ac`. Its validation manifest pins the exact
pre-commit tested bytes. The principal playing evidence, R047 and R048, uses frozen
source `71d475f85e4ce6f5026322c2a2faf3e79f282291`, build 78. Later research changes
do not change those runtime identities. Production CP64 and Gen32 remain intact.

## A. Best available neural evaluator

There is **no statistically established neural strength champion**. R045 **MIX**
is the best-supported combination of neural predictive improvement, transfer
evidence and playing measurements. Seed 71 has the best observed game score;
that does not establish that it is the better checkpoint. Seed 97 has lower
teacher loss but a worse game score. Both must be retained without selecting a
winner from this small match. The newer R055 GATED seed 97 improves some static
metrics, but its family failed the predeclared two-seed gates and has no transfer
or playing confirmation. It does not supersede MIX on strength evidence.

MIX is a width-8, two-pass learned relational network. A shared learned transform
of relative geometry and occupancy produces message gates. The first pass sums
gated piece/square embeddings and applies softsign separately at each piece. A
learned 8-by-8 matrix mixes channels of these nonlinear piece states; the same
relational operator then composes them in a second pass, followed by another
nonlinearity and piece-conditioned readout. Material and learned unary terms are
also present. Relations and composed contextual states contribute materially:
ablating the mixing or relational readout worsens the measured losses.

This differs substantively from CP64's globally accumulated feature transform
and output head. It is not a CP64 correction, and its context-dependent nonlinear
piece states cannot be reduced to the tested fixed additive pair tables. The
finite gate cache is compiled from learned transforms; it is not a separately
trained pair-table model. This is architectural distinction within this programme,
not a claim of worldwide novelty or universal expressiveness.

Implementation: [MixedTiedPieceNetwork.java](../../../research/src/main/java/com/ohinteractive/seedv6/core/brnpair2/MixedTiedPieceNetwork.java),
research selector `mix2`. It has **20,417 scalar weights**, 163,336 bytes of double
weight payload, a 490,048-byte checkpoint including Adam state, and a derived
gate/projection cache of 1,382,400 bytes. These are different quantities: the
checkpoint is not the inference memory footprint. The research loader retains
training arrays, and workspaces add memory; a production RSS measurement and
lean inference export were not performed. Search currently recomputes the neural
evaluation in full. There is no validated incremental MIX implementation.

Training used 1,048,576 existing positions with CP64 sigmoid-score targets,
batch 128, two epochs, shuffled seeds 71/97, masked Adam with bias correction,
rates .01 then .001, proportional decay .01 and weight bound .99. Each arm has
16,384 updates and 2,097,152 example exposures. The 4,096-position development
set, R035's 1,398 played-development positions and later transfer panels were
not training rows for this run. They are nevertheless consumed development
evidence, not reserved final validation. Game identity is unavailable for the
original corpus, so alias controls do not prove absence of all game correlations.

| Measured endpoint | MIX seed 71 | MIX seed 97 |
|---|---:|---:|
| CP64-target sigmoid MSE, original 4,096 | .00990638 | .00955188 |
| CP64-target MSE, R035 played development 1,398 | .02893958 | .02936485 |
| Original BT4-target MSE, 4,096 | .02268800 | .02207267 |
| CP64-target MSE, transfer 8,192 | .01018306 | .01008522 |
| CP64-target MSE, earlier played panel 991 | .03344809 | .03224903 |
| Full evaluator microbenchmark | 7.649 us | 7.190 us |
| Games versus CP64, 10,000 nodes | 4W / 6D / 6L | 2W / 4D / 10L |
| Score, 16 games each | 43.75% | 25.00% |
| Observed search nodes/second | 132,677 | 151,230 |
| CP64 nodes/second in those comparisons | 542,356 | 561,516 |

Sources: [R045 results](r045-results.json), [R046 transfer](r046-results.json),
[R047 games](r047-results.json). MSE is in sigmoid-target space, not centipawns
or outcome error. The two played panels have different positions and sizes;
their loss levels are not interchangeable.

Against the matched R044 LOW control, MIX's original teacher loss improved
3.55%/6.57% and R035 played loss improved 11.55%/12.04%. R046 confirmed positive
effects on the larger and earlier played panels. On the same 8,192 positions,
Pair24 scored .01177638 teacher MSE and Gen32 .02067040; on the same 991 played
positions they scored .04451691 and .06047022. Thus MIX surpassed these secondary
controls on these predictive measures. **No direct MIX-versus-Pair24 or Gen32
match established a playing-strength ranking.** Pair24's separate 20k-node
results cannot be combined with MIX's 10k-node results as a head-to-head test.

MIX's two-arm training stage cost about 217 seconds including the earlier
interrupted attempt. It ran on one logical CPU with a 512 MiB JVM heap. Games
used two 384 MiB JVMs and one active search. Consumer-CPU trainability is real at
this scale; practical inference efficiency is its serious weakness. Its search
throughput was about 24.5%/26.9% of CP64's in the recorded games. Different search
trees affect NPS, so that is not an isolated kernel ratio, but it provides no
evidence of a throughput advantage that could compensate for weaker evaluations.

Preserved best-family states are `raw/r045-mix-{71,97}/epoch-2.state`; the
assessment manifest contains verified full SHA-256 identities. GATED endpoints,
all earlier models, CP64 and Gen32 are preserved separately. Improved additive
Pair24 remains a training control, not the principal neural architectural result.

## B. Material experimental history

The following groups summarize discriminating findings rather than count every
variant as progress. [STATE](STATE.md) retains the complete R000-R056 dispositions.

| Family and motivation | Observations and disposition |
|---|---|
| Aligned Pair training; attack/king-conditioned additive energies, R002-R010 | Objective/scale alignment and bounds improved controls. Pair24 beat Gen32's point score but remained well behind CP64 in the broader common-search match. Added table conditioning did not reliably generalize. R010 coverage is preserved at its owner-directed checkpoint; no open-ended Pair optimization remains authorized. |
| Local neural messages and king-anchor/global context, R004/R011-R016 | Regularization improved local composition 7.56%/8.12%, but it still trailed matched PAIR and had 38/64 structural zeros. Global/king paths enlarged capacity; some learned readouts were useless or harmful. More coverage improved use of paths without clearing architecture gates. CP64 rather than BT4 supervision improved neural teacher loss about 5-8%, confirmed on 8,192 positions. Failed early recipes therefore were not clean tests of graph expressiveness alone. |
| Attack/control fields, empty squares and piece composition, R017-R024 | Fields reached 64/64 structural witnesses. Several useful paths made small predictive gains, but extra epochs, channel mixing, horizontal canonicalization and dense Adam did not provide consistent improvements. Width 16 improved training fit without the required held-out gain. R022 composition transferred modestly; its first playing test was weak. Capacity and fitting were not sufficient. |
| All-piece messages, routing, scale and distribution, R026-R036 | All-piece ONE improved static transfer by 3.25%/2.01% over R022; a separate second pass and dynamic routing failed matched gates. Scale calibration helped only modestly. Played losses remained much larger even after matching piece count and teacher magnitude. The tiny root-separated played-data mixture memorized its training sample and regressed the original panel. This rejects that data recipe, not all search-distribution training. |
| Shared geometric transforms and tied recurrence, R037-R044 | Initial 16.8/29.5 us costs were reduced by exact compilation to 4.25/4.74 us. Learned tied recurrence then produced useful static gains with 6.5-7.9 us cost and passed transfer. Its games were still weak/noisy. Proportional-decay continuation provided useful controls but did not clear both-seed advancement gates. |
| Cross-channel MIX and matched attribution, R045-R048 | Both prediction and transfer gates passed, with the programme's best played-family point score. The matched LOW game control showed opposite directions across seeds; intervals included zero. A learned mixing contribution to practical playing strength was not established. |
| Move-coherence and side-to-move diagnosis, R049-R050 | 4,096 legal parent/child pairs showed about 1.13x restricted child loss, below the predeclared 1.5x gate for adjacent-data work. Zeroing the even-in-turn component improved child loss only 2.94%/1.83%, below 5%; CP64 itself has nonzero even components. Neither diagnosis justified its proposed repair. These failures constrain specific remedies, not all distribution or calibration explanations. |
| More expressive relation transforms/modulation, R051-R055 | Source-and-target transforms passed correctness but failed inference gates; float caching and exact factorization did not clear the prescribed cost bound. No quality result exists for those untrained routes. Target-state gating passed cost and was trained, but teacher gains .29%/1.16% and played gains -.94%/+2.87% missed the required 2%/5% on both seeds. No further epochs or games were admitted. |
| Low-rank learned keys/queries and composition, R056 | Independent quadratic-oracle, gradient, symmetry, state and continuation checks passed. ONE measured 3.848 us and TWO 12.216 us against same-harness MIX 7.176 us. Required TWO <=5 us and <=75% of MIX failed. No model fitting occurred. The ONE result is an untrained cost observation, not a new evaluator success. |

Relevant reports: [R008](R008.md), [R011](R011.md), [R016](R016.md),
[R023](R023.md), [R034](R034.md), [R036](R036.md), [R039](R039.md),
[R041](R041.md), [R048](R048.md), [R049](R049.md), [R050](R050.md),
[R053](R053.md), [R055 results](r055-results.json), [R056 results](r056-results.json).
Earlier architecture/successor workstreams also tested energies, pair/triple
tables, products, pooling and bitboard circuits; their negative reports remain
historical evidence. They were not rerun for this review, and this assessment
does not claim every historical raw artifact was independently recovered.

## C. What explains the remaining gap, and what remains conjecture?

**Demonstrated representational limitation of the old pair models:** R008 selected
64 legal counterfactual quartets whose two relocations have disjoint affected
pair terms. Their mixed difference cancels for every choice of those table
weights; an independent categorical-basis check verified this. CP64 was nonzero
on 63/64, including 31/32 cases without changing king context. More Pair training
cannot remove that exact functional restriction. It does not follow that CP64's
responses are correct or that these selected counterfactuals explain its Elo.

**Neural candidates did remove this specific restriction.** Global messages,
fields and later relational formulations produced nonzero witnesses, and ablations
show that several learned relational paths improve prediction. A witness establishes
representability, not learned accuracy, broad higher-order capacity or search
utility. R056's identical ONE/TWO initial witnesses arise with the second readout
initialized to zero; they do not demonstrate a learned advantage of its second
pass. No experiment established that fitting the R008 interaction values causes
stronger play. It would be unsound to declare the architectural problem solved
because the exact zeros disappeared.

**The training and capacity comparison is highly unequal.** CP64 has 7,680 inputs,
a shared width-256 transform, two perspectives, squared clipped activations and
eight material heads. Its selected exported payload contains 1,970,440 scalar
parameters (7,680*256 + 256 + 8*(512+1)); the old one-head source constant is not
the eight-head parameter count. It was trained on 33,554,432 positions and
268,435,456 example exposures using pinned Bullet/CUDA. MIX used about 96.5 times
fewer weights, 32 times fewer distinct rows and 128 times fewer exposures. CP64
uses BT4-derived targets; MIX distills CP64. Their optimizer details, feature
factorization, batching and representation also differ. The comparison establishes
which completed evaluator works better under the tested conditions, not which
architecture would win at equal capacity, data and tuning budget.

Training interventions did matter: aligned objectives helped Pair controls and
CP64 supervision helped neural controls. Yet repeated epochs, wider early models,
optimizer changes and small new-data mixtures failed particular held-out gates.
This is evidence against blindly adding training to those recipes. It is not
evidence that a well-designed larger relational model cannot benefit from broader
data. The original corpus lacks game identifiers, and repeatedly consumed panels
increase model-selection bias. A large-scale relational scaling law has not been
measured. Existing Bullet support does not directly implement arbitrary dynamic
message passing; efficient GPU training would require separate engineering and
parity work. It cannot be assumed available at CP64's cost.

**Generalization and inductive bias remain credible explanations.** R034 retained
86.9%/98.9% of rows after phase/score matching and still found a 2.39x played-data
loss gap in both seeds. R036's small root-separated mixture overfit. Compact
geometry-sharing and width-8 bottlenecks may constrain useful chess functions;
adding mechanisms may also make optimization harder. These are plausible
interpretations, not isolated causal findings. R033 did not find broad softsign
saturation, and R049/R050 did not support their specific coherence/turn remedies.
Those negatives narrow the possibilities without identifying one master cause.

**Inference and search are measured practical problems.** CP64 uses quantized
incremental accumulators, refreshes on king-context changes and preserves parent
states across siblings. The neural candidates use double arithmetic and full
recomputation. Low weight count therefore does not imply cheap search. Common
search controls, node probes, perspective/special-move checks and legal replay
reduce obvious implementation confounds; they do not prove absence of every
search interaction. CP64's production search additionally enables
NOMINAL_FUTILITY and VERIFIED_NULL (flags 3407871 versus common 262143).
The BRN games do not test parity against that full deployed composition.
No search-strength changes should be introduced merely to rescue a candidate.

## D-E. Progress and tournament evidence

Static progress is meaningful: on the same 8,192-position panel, R022 teacher
MSE was .012704/.012929; R028 ONE .012291/.012669; MIX .010183/.010085.
MIX is about 19.8%/22.0% better than R022 there, and it beats the old additive
controls on that panel. Different exposure schedules mean this is programme
progress, not a pure architectural effect. R045/R046's matched controls provide
the stronger evidence that mixing itself improves prediction.

The playing sequence is encouraging only at the level of exploratory point
estimates. These are the five main 32-game neural stages, each with 16 games per
seed, using the same eight development opening roots and both colours:

| Family, stage | Seed 71 score | Seed 97 score | Combined score across two models |
|---|---:|---:|---:|
| R022 COMPOSE, R025 | 9.375% | 21.875% | 15.625% |
| R028 ONE, R030 | 15.625% | 12.500% | 14.0625% |
| R041 TIED, R043 | 12.500% | 34.375% | 23.4375% |
| R045 MIX, R047 | 43.750% | 25.000% | 34.3750% |
| R044 LOW matched control, R048 | 21.875% | 34.375% | 28.1250% |

The reported recollection of approximately 43% over 32 games needs correction:
**43.75% was MIX seed 71 over 16 games. The complete MIX stage scored 6W/10D/16L,
or 34.375% over 32 games across two different checkpoints.** The latter is a
family summary, not a 32-game estimate for one deployed model.

R047's opening-pair bootstrap 95% intervals are [25%,62.5%] for seed 71 and
[12.5%,37.5%] for seed 97. There are only eight opening clusters, reused through
selection and across families. The intervals are descriptive, not adjusted for
choosing the best seed/family after many trials; the conservative recorded
Hoeffding bounds are much wider. Neither intervals nor the best point establish
noninferiority. The predeclared broader-match gate, at least 45% in both seeds,
failed. There was no extension to chase significance and no fresh final panel.

R048 is particularly important. Against matched-training LOW, the MIX score
difference was +21.875 percentage points in seed 71, with bootstrap interval
[-6.25,+46.875], and -9.375 points in seed 97, interval [-34.375,+12.5]. Thus the
stronger predictive mechanism did not show a consistent playing contribution.
Small samples do not prove it has none, but the research cannot claim that it has.

The neural stages use 10,000 whole-request nodes including quiescence, Threads=1,
TT=16 MiB, common flags 262143/ADAPTIVE16, no pondering, book or tablebases, and
one active game. A 640-ply cap would be unscored; a 30-second administrative
watchdog would censor failure, not award a clock loss. The stages above had no
caps or censored games. Budget probes, frozen runtimes and independent legal
replay are recorded. Actual searches may finish early at terminal positions;
fixed nodes are a verified maximum work budget, not a guarantee that every move
uses exactly the same number of nodes. Equal nodes do not mean equal elapsed
time or identical search trees.

The broader additive-control stage R001B used 20,000 nodes and 32 roots, 64 games
per comparison: Pair24 versus Gen32 26W/24D/14L (59.375%); Pair24 versus CP64
5W/22D/37L (25%); CP64 versus Gen32 36W/16D/12L (68.75%). It supports CP64 as a
difficult reference, with its recorded uncertainty, but does not establish the
claimed 150-250 Elo gain or rank MIX against Pair24. Historical clock matches
from the separate NNUE workstream are not new BRN tests. **No equal-time Elo or
practical parity is inferred here.**

The strongest surviving candidate improved on static metrics and exploratory
family point scores, so the entire programme was not stagnant. The latest gains
are diminishing: R055 missed quality gates, R051-R053 and R056 TWO missed cost
gates, and diagnostics did not identify a high-confidence repair. Architectural
novelty has recently outpaced measured progress toward the actual playing goal.

## F. Production prospects

No existing candidate is ready for responsible CP64 replacement. Comparable
practical strength would be sufficient, but it has not been demonstrated. MIX
has neither a convincing equal-node parity result nor a throughput advantage.
R028 ONE was faster than MIX but still ran at roughly half CP64's measured NPS
and had weak game scores. R056 ONE's untrained 3.848 us kernel is not even the
programme's fastest neural kernel (R028 ONE was about 2.8 us); it has no measured
search NPS or quality. It cannot currently rescue the practical case.

A future production proposal would need a frozen candidate with replicated
training gains; fresh reserved opening evidence with justified precision and
selection control; comparison against the actual deployed CP64 search settings;
measured latency, NPS and memory; correct selectable integration and any claimed
incremental updates; and explicit owner production authorization. If equal-time
confirmation is necessary, it remains a separate human-authorized clock-testing
gate. A favourable speed ratio alone cannot be converted into an Elo offset.

## G-H. Remaining opportunity, cost and recommendation

The most concrete remaining opportunity is **quality-preserving low-rank
relational pooling**, using the already checked R056 ONE implementation. Learned
source keys and target queries factor pair weights; associative pooling avoids
materializing every pair transform. Per-piece query-dependent nonlinear states
and self-edge exclusion retain relational computation. It differs materially
from the failed geometric-cache optimizations, and its measured cost is lower
than MIX in the same harness. Risks are serious: rank four may be too restrictive,
explicit blocker geometry is absent, and removing the second pass may sacrifice
the useful composition. R056 TWO failed its original gate; nothing here waives
that result or admits training under its original protocol.

If the owner chooses a narrow **PIVOT** despite the suspension recommendation,
the single most informative next experiment would be a new prospective ONE-only
quality/cost pilot, not a training sweep or another tournament:

- Hypothesis: factorized ONE preserves MIX's predictive quality closely enough
  to improve the computational trade-off, despite its different inductive bias.
- Reuse the original 1M training rows, two seeds and fixed two-epoch MIX recipe;
  preserve weights, optimizer and segment cursors. Freeze a new R057 protocol
  before fitting, including data/runner identities and these proposed criteria.
- Advance only if **both seeds** are within 2% of MIX's original and R035 played
  teacher MSE, BT4 loss is no more than 1% worse, relational ablation is useful,
  and matched full-inference cost is <=4 us and <=60% of MIX. A passing pilot
  would justify a separately bounded transfer stage, not games or production.
- Stop after those two endpoints if any criterion fails. No rate/rank/epoch
  sweeps, threshold relaxation or automatic extension. Preserve the negative.
- Approximate allocation: one BelowNormal CPU, 512 MiB training heap, no GPU,
  checkpointed segments; roughly 2-5 CPU-worker minutes plus focused validation
  overhead is an estimate from prior runs and disposable update timings, not a
  measured training duration. A maximum bounded tranche and the existing
  resource stop conditions must be fixed in the new protocol.

This offers inexpensive information about one trade-off; it is not currently
evidence of a credible route to CP64 parity. Even a pass would leave a substantial
search-cost and strength problem. It should change the recommendation only if
quality and cost improve together reproducibly, followed by independent transfer
and meaningful playing evidence. The old failed TWO route must remain failed.

The other major uncertainty is unmatched data/capacity. A substantially larger
relational model with diverse search-relevant data could distinguish a tiny-model
or training-budget limitation from a broader architectural problem. Existing
experiments do not resolve that question. However, earlier widening and small-data
augmentation failed, and no measured large-model training/inference budget or
efficient Bullet path exists. This is an unresolved hypothesis, not a justified
next expensive experiment. Incremental computation or quantization could reduce
cost, but engineering them before establishing adequate quality has poor expected
value. Neither opportunity justifies an open-ended queue now.

The machine is a Ryzen 5 5500 (6 cores/12 logical), about 31.8 GiB RAM and an
8 GiB RTX 4060. Research used conservative serial CPU workloads; no GPU was
allocated. R055's two arms took 218.24 seconds, maximum sampled system CPU 25.64%,
minimum free RAM 16.72 GiB. R047/R048's 32-game stages took about 168 seconds each.
R056's audit took 1.92 seconds; builds/checks add time. These receipts show that
individual screens are cheap and interruptible. They do not measure total
programme energy, total developer effort or the cost of production optimization.
Many small mechanisms, custom gradients, export paths and repeated consumed-panel
analyses still impose substantial development and selection costs.

**SUSPEND is preferable to continuing the present sequence** because the latest
experiments have not produced a candidate near practical parity, the strongest
played family is much slower, and the key playing signal is small and selected.
It is preferable to committing to a large pivot because no large-scale budget
or discriminating architecture has yet earned that investment. The evidence
primarily limits the tested small CPU-trained formulations and recipes. It does
not establish a hardware-imposed impossibility, nor a general limitation of BRN.
The cheap optional pilot is reasonable only as an explicit bounded human choice.
No additional experiment is necessary merely to justify preserving this pause.

## Decision and exact resumption boundary

Technical continuity is ready on this machine: selected checkpoints, inputs,
runtime snapshots and completed receipts are present. Research authorization is
paused. Older R010 data coverage remains separately paused by priority; no previous
training or match queue becomes active because this assessment is complete.

| Owner decision | Next admissible operation |
|---|---|
| Keep paused / accept SUSPEND | Record the decision in STATE; preserve all artifacts and exit. No empirical operation, permanent closure or cleanup is implied. |
| Authorize the bounded low-rank ONE PIVOT | Read STATE and contract; inspect Git/supervisor and verify frozen R056/MIX/input identities; prepare the new prospective R057 ONE-only protocol above. Only after those gates and fresh resource admission may its bounded work begin. |
| Request CONTINUE on another direction | Record the explicit decision and formulate one discriminating prospective hypothesis using this assessment. No failed R055/R056 gate or old queue is automatically reopened; a substantially new resource allocation still needs its applicable authorization. |
| Bare RESUME with no research decision | Surface the pending decision gate and remain paused. Do not launch training, probes, games or architecture work. |

Existing reusable instruction:

> RESUME CGLHW seedv6-brn-relational-20261029 in C:\projects\seed\java\seedv6-unified.
> Start at docs/research/brn-relational-cglhw/STATE.md and obey its current gate.

Human actions required after this prompt: **None to retain the pause. An explicit
owner research-continuation decision is blocking any new experimentation.**
No clock permission, GPU allocation, production promotion or external spending is
requested. The assessment commit is a completed work unit, not owner acceptance.

Validation for this pause consists of process/ownership inspection, JSON/source/
checkpoint integrity checks, arithmetic reconciliation of saved game results,
local-link checks and Git review. The eight R056 tests and all earlier empirical
checks occurred before the pause. No new build, training, tournament, complete
test suite, GUI validation or production qualification was run for the review.
Build remains 78; research-only and documentation commits do not change it.
