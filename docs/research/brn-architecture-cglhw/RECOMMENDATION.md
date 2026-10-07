# Final BRN architecture recommendation

2026-10-07. Research recommendation under [CONTRACT](CONTRACT.md). Production
adoption and user acceptance are separate decisions and have not occurred.

## Decision

Use **C02 compiled order-2 relational lookup tables** as the next BRN architecture
candidate for production integration review. This is the strongest practical
choice supported by this programme's controlled consumer-hardware experiments.
Keep the current production evaluator unchanged until the owner authorizes a
separate adoption/integration work unit.

The recommended design learns discrete two-square categorical interactions and
compiles the selected function into 294 physical pair tables. It uses incremental
updates, a fixed material prior, and the frozen .25 residual search gain. The
within-600-optimizer-second selection rule selected the 300-second checkpoint in
both fresh source/order runs. The tuned pair recipe uses the documented .01 rate;
compilation changes inference realization, not learned capacity, optimizer,
selected weight bytes, or the training objective. See [C01](C01.md), [C02](C02.md),
[C02 results](C02-RESULTS.md), and the immutable [final design](Z01-FINAL.md).

This is a recommendation within tested budgets, hardware, inputs and search
conditions. It is not a claim of a globally optimal evaluator, necessary pair
interactions, or superiority to every mature NNUE or production configuration.

## How strong is the evidence?

The independent final campaign completed **3,072 games**, with 512 games for each
direct comparison, at 100 ms per move. Each comparison uses 128 common fresh
opening pairs on each of two training source/order runs, with reversed colours.
All 768 batches, model/code/runtime bindings and resource checks passed. No game
was missing, capped or failed; maximum game length was 654 plies. All outcomes
were opened only after the full sample closed. The first rejected opening sample
was replaced in its entirety before any games under the predeclared geometry
rule; its failed evidence remains preserved.

| Opponent | Compiled-pair score | Approximate two-way 95% interval | Run A / B |
|---|---:|---:|---:|
| Fresh current BRN architecture | 80.18% | 75.39–84.57% | 78.32 / 82.03% |
| Current material NNUE v2 | 73.73% | 69.53–77.73% | 73.83 / 73.63% |
| Strict research NNUE | 74.90% | 70.12–79.49% | 73.24 / 76.56% |
| Cheap material evaluator | 64.26% | 57.23–70.70% | 67.77 / 60.74% |
| Captured historical native BRN | 55.57% | 50.98–60.16% | 55.66 / 55.47% |
| Compiled own Gen0 | 73.93% | 66.99–80.47% | 77.73 / 70.12% |

Score counts a win as 1 and a draw as .5. These are score percentages, not win
rates. The primary, unadjusted bootstrap strength gate passes in all six direct
comparisons. The conservative simultaneous bounded-pair sensitivity check also
clears 50% for BRN, both NNUE controls, material and own Gen0. Its material lower
bound is only 50.58%; the native-BRN lower bound is 41.89%. Even the native
single-comparison conservative lower bound is 44.75%, below the 45%
noninferiority threshold. The native advantage is therefore promising in the
primary analysis but not robust under that conservative sensitivity analysis.
Do not advertise unconditional superiority over all six controls.

Both uncertainty methods have limits. Only two source/order runs were observed;
their bootstrap is approximate. Bounded-pair checks condition on fixed models
and independent generated opening clusters, not the training-source population.
The adjacent corpus intervals lack game IDs. Geometry independence is verified;
game-disjoint or fully independent-source training is not established. No Elo
conversion or outcome-dependent extension is needed for the supported claim.

Full W/D/L, opening-only intervals, conservative bounds, adverse accounting,
resources, sealed metrics and hashes are in [final results](Z01-FINAL-RESULTS.md).

## What this says about BRN and NNUE

The direct BRN result supports replacing the **tested freshly trained current
architecture** as the research lead. It uses the common controlled training and
selection substrate and the previously selected .125 BRN gain. That is distinct
from saying that every existing native BRN model, gain, training history or time
control has been beaten. The captured native model is a separate, weaker-certainty
practical reference with unknown historical training resources and exposure.

The current NNUE control is the production material-v2 architecture and objective,
with its original initializer, Adam .001 and calibrated residual mapping. It is
not the old material-off outcome-MSE control. Strict NNUE is a separate zero-head
research control and is not represented as production. Both received the same
maximum 600-second optimizer opportunity, frozen validation-selection rule and
small gain-selection opportunity. Final gains were .125 for these controls and
.25 for compiled pairs. Architecture-specific recipes and their tuning costs are
recorded, so this compares practical architecture/recipe packages rather than
an isolated architecture intervention with every optimizer held identical.

Current NNUE was still improving at 600 seconds; that budget is below the time
it needed for the earlier common-exposure run. The result establishes useful
short-budget consumer-compute efficiency, not an asymptotic NNUE ceiling or a
win over extensively trained/tuned external engines. The captured native NNUE
Best files were Gen0 and were correctly excluded as mature trained controls.
The separate NNUE research programme is not closed by this result.

## Training, data and scaling

On the fresh 262,144-position ranges, eight epochs / 2,097,152 exposures took
about **21.84–22.22 optimizer seconds for pair tables**, versus 103.16–109.22 for
BRN and 657.63–669.14 for current NNUE. These are optimizer times; loading,
validation, serialization and full process cost are recorded separately. They
are common-exposure measurements, not equal-quality training times. The final
choice used a separate equal-maximum-time trajectory and the selected pair
checkpoints are its 300-second endpoints.

The programme measured short learning, equal exposure, 15/30/60/120-second and
120/300/600-second trajectories, and nested 131,072-versus-262,144 data studies.
Promising mechanisms received serious training and fresh replication. This is
more than a first-epoch comparison, but it does **not** establish better long-run
scaling for pairs. Pair validation quality plateaus and slightly regresses after
300 seconds in both runs, while current NNUE continues improving. The two-way
120-to-600-second playing-score-change intervals all include zero. All eight
large-versus-small-data playing intervals include .5, despite predictive gains
from more distinct data. Common maximum exposures and eligible checkpoint
opportunities do not imply equal selected exposure or selected training time.

The supported advantage is strong fixed-time engine performance at the tested
consumer training budget. Neither an extrapolated scaling law nor a universal
data-efficiency winner is justified. See [Z01](Z01.md), [scaling design](Z01-ENGINE-SCALING.md),
and the retained timed/nested/scaling results and failed-audit reconciliation.

## Inference, memory, search and stability

The selected pair model contains 110,527 inference parameters and occupies
442,124 stored bytes, versus 9,474,352 for BRN and 12,599,889 for the NNUE models.
Compiled immutable primitive storage is 799,688 bytes plus 2,920 bytes per worker;
stored-model size and runtime storage are different quantities. Original pair
training has 113,231 parameters and a 2,731,396-byte state. JVM/data/checkpoint
peaks remain separately reported; they are not isolated evaluator or optimizer
memory measurements.

For the exact final profiles, initialized-board-plus-score cost was approximately
1.24–1.27 microseconds for compiled pairs, 7.31–8.71 for BRN and 2.37–2.94 for
current NNUE. Legal-child-plus-score cost was .142–.152 microseconds for compiled
pairs, 4.31–4.89 for BRN and 1.297–1.336 for current NNUE. These are the measured
protocol boundaries, including their harness costs; initialized BRN may benefit
from its existing cache and is not necessarily a forced full rebuild.

The earlier paired, same-function C02 study observed a **24.20% reduction in
depth-4 root time** after the predeclared cost-repeat rule, with matching scores,
moves, depths and nodes. This is stronger causal evidence for the compilation
mechanism than comparing different evaluators' trees. Model-specific validation
and the final sealed sample found no original/compiled score or metric difference;
that is observed numerical equivalence, not a universal floating-point proof.

In final games, compiled pairs searched about **2.50 million nodes/second**,
versus .385 million for fresh BRN and .780 million for current NNUE in their
respective direct comparisons. Mean measured move times were similar, around
96 ms, with permitted early completion. Cheap material remained faster at about
4.07 million nodes/second, yet lost the direct strength comparison. Different
functions visit different trees; cross-family NPS is an engine outcome, not a
same-node kernel benchmark. No general search policy was changed to rescue a
candidate.

Final runtime checked 1,792 depth-4 roots and 14,553 exact child scores. The separate
bounded qsearch diagnostic completed 703 of 704 measured roots; one linear
node-cap outcome remains explicit. It used the research TT-free qsearch seam,
not production PVS, so no production-qsearch or longer-time-control guarantee
follows. Special moves, undo, symmetry, information boundaries, exact optimizer
continuation, model metadata and compiled loading received targeted validation.

Two fresh source/order runs and repeated compilation/cost checks support stability.
Pairs start from zero; these are not two random weight initializations. Their
relative material/Gen0 gains differ by run, and two runs do not characterize the
full population. Long-term concurrent production operation, other CPUs/JVMs and
large sustained training remain outside the measured boundary.

## Static quality and the other architecture families

All 22 sealed profiles passed their complete-population and identity audits:
720,896 visits over 65,536 distinct positions. Compiled pairs' raw half-MSE was
.128969/.125921, versus BRN's .073677/.069346 and current NNUE's .105277/.112585.
Edge functions achieved .072325/.070848: lowest on A, but BRN was lowest on B.
Every profile reported zero saturated search scores. The full balanced,
piece-count/material-stratified, CP, residual and reliability evidence is retained.
Teacher expected-score calibration is not observed win probability, and piece
count is not known game phase. No checkpoint, gain or family was selected after
these metrics were opened.

The practical playing lead therefore coexists with substantially worse teacher
prediction quality. Preserve that Pareto distinction; do not select the engine
architecture by loss alone.

| Family | Evidence-based disposition |
|---|---|
| Linear / low-rank energy readout | Linear learns, but practical inference cost and 32.81% within-600s material score do not justify it as the next mainline choice. Rank/energy expansion was deferred at its gates. |
| Original pair tables | Same-capacity control. Direct paired cost and playing evidence supports C02's compiled realization. |
| Triple tables / nested cubic products | Additional-order components can contribute, but tested whole-model strength/cost does not earn further mainline allocation. No universal higher-order rejection. |
| Learned piecewise-linear edges | Useful predictive-quality trade-off; no established practical playing lead. |
| One-hop CONTEXT / SELF | Causal context comparison scored 40.625%, both fresh runs below .5; expansion deferred. This does not reject every graph/message architecture. |
| Learned hard bitboard circuits | Very small model and repeated raw gate-learning benefit; direct learned-versus-fixed playing benefit remains inconclusive. Retain as a separate niche, not the main recommendation. |
| Modern learned unary/PST | Considered and explicitly deferred. Historical R0 differs in recipe and geometry; it does not reject a modern incremental unary model. Pair interactions are not proven necessary. |

These are dispositions, not a transitive claim that compiled pairs directly beat
every row. Family records [B01](B01.md), [C01](C01.md), [D01](D01.md), [E01](E01.md),
[F01](F01.md), [G01](G01.md) and [SCORECARD](SCORECARD.md) retain all attempts,
negative evidence, tuning and descendants. Primary literature in [E000](E000.md)
motivates mechanisms; it does not substitute for Java chess measurements.

## Distinctiveness, maintainability and what to adopt

This design materially departs from a conventional NNUE/MLP inference pipeline:
it addresses learned discrete pair-state potentials directly and sums their
contributions, using incremental changes rather than a generic dense hidden
transform. It is still a fitted scalar evaluator using supervised gradients and
a material prior. Distinctiveness supports the recommendation because practical
strength also improved; novelty did not override strength evidence.

The learned representation is compact and inference is structurally simple, but
the compiler, canonical ownership/empty-state mapping, incremental cache and
fallback/undo rules add correctness obligations. A production change would need
an explicitly authorized integration plan for loading, compatibility, lifecycle,
threading, defaults and regression coverage. The experimental variants belong in
verification; this recommendation does not justify moving every research family
or tool into the shipped application.

Recommend the **C02 architecture and its evidenced recipe** for that next decision,
not an automatic promotion of either research checkpoint. Production/default
evaluation and user model stores remain unchanged. No push, deployment or release
was performed. Build 34 is unchanged; the earlier local version invocation warning
was non-blocking and did not establish a successful finalization or version bump.

## Highest-value follow-up research

These are optional subsequent workstreams, not unfinished admitted experiments or
newly scheduled compute:

1. Test a modern incremental learned unary/PST control with proportionate tuning
   and direct common-resource matches. It could determine how much pair interaction
   is needed for the observed practical result and whether a simpler point dominates.
2. Compare mature, longer-trained current NNUE and native BRN at longer time controls
   on additional independent source ranges and opening suites. Current conclusions
   are bounded by the short training/time-control regime and two source runs.
3. Investigate pair capacity/generalization and calibration with a new discriminating
   hypothesis, since raw quality plateaus and scaling superiority is unproved.
   Reopen data growth or a descendant only after a prospective budget and gate.
4. If adoption is authorized, validate the narrow production integration and its
   search/lifecycle behavior on the intended platforms. Keep earlier unsuccessful
   families and their evidence available rather than importing their complexity.

No further empirical allocation is scheduled in this programme. The final
[completion audit](COMPLETION-AUDIT.md) and [state](STATE.md) record closure
verification, provenance and any remaining administrative work. Full/slow suites,
GUI/browser, packaging and deployment checks were not run for this verification-only
research change. The programme records 84 distinct targeted Java tests and 28 Python
tests; final real-model/game/quality audits provide additional runtime evidence.

Human actions required after this prompt: None. Adoption is an optional reserved
owner decision and does not block completion of the research recommendation.
