# BRN architecture research contract

Revision 2, 2026-10-06 (Pacific/Auckland): adds the bounded G01 learning-depth
diagnostic described below;all other boundaries remain. Authority: the owner's CGLHW
request to research practical, technically distinctive BRN architectures.
Start/resume at [STATE](STATE.md); comparisons at [SCORECARD](SCORECARD.md).
This is one programme with one frontier. It does not reopen or rewrite earlier
completed programmes, finish the separate NNUE programme, or authorize production
adoption. A recommendation is the deliverable; adoption remains the owner's decision.

## Question and boundaries

Which evaluator/training architecture gives SeedV6 the strongest practical engine
on consumer hardware, and what evidence justifies it over current BRN and NNUE?
Novelty is a recorded dimension, not a substitute for strength, training/data
efficiency, inference cost, stability, maintainability or search compatibility.
Retain a Pareto comparison when the evidence supports different useful choices.

Use isolated verification sources and new ignored experiment directories under
`app/build/research/brn-architecture/`. Never write user model/corpus stores, select
a new production/default evaluator, migrate checkpoints, publish, push or deploy.
No GUI, general search project or unrelated refactor. Necessary research substrate
repairs are in scope with direct regression evidence. Paid/cloud resources,
destructive actions and final production replacement require owner authorization.

Preserve the established innate-information boundary for initial candidates:
piece identity/ownership/location, geometry, STM, and the same fixed material
P=1,N=3.2,B=3.3,R=5,Q=9,K=0. Raw geometric bitboard transforms are hypotheses;
handcrafted attacks, mobility, king safety or positional values are not inputs.
Fixed material is separate from the residual. No teacher output at inference.
Rule-state extensions need an explicit hypothesis, information audit and split
review; they are not granted merely by an implementation convenience.

## Controls and common substrate

* BRN3 is the current production typed width8 relational network, masked Adam
  .003, batch128, CE/WDL supervision, zero residual head, folded float inference,
  quarter residual search calibration and R01 capture cache.
* NNUE_MATERIAL_V2 is the current production HalfKP64x32 control: original random
  initializer, Adam .001, batch128, calibrated CE/WDL objective, fixed material,
  full-range tanh residual search mapping. Its different calibration is explicit.
* NNUE_LEGACY retains the material-off outcome-MSE reference, separately labelled.
* NNUE_STRICT is the existing E009 research-only zero-head pawn-residual CE/masked
  Adam control. Use when isolating architecture versus objective/bootstrap scale;
  it must not be represented as production NNUE.
* Material-only and fresh Gen0 controls distinguish learning from prior knowledge.

All architectural comparisons use identical frozen CP-eligible positions/targets,
source identity/raw interval, order and partitions. Reuse SourceReaders and the
existing Stockfish WDL adapter; do not generate different teachers per candidate.
Keep source-profile identity explicit (BT4 Q and Lichess CP are not interchangeable).
Group permitted-input aliases and color/rank/file equivalents before partitioning.
Existing BrnResearchData global group split and uniform held-out reservoirs are
the starting substrate. No game-disjoint claim: this corpus lacks game identity.
Old opened tests are historical only. Final confirmation requires new ranges and
openings, with alias overlap checked against earlier selection data. Sealed test
labels cannot select recipes, gains, checkpoints or families.

Choose checkpoints on raw validation outcome half-MSE; report raw CE, calibrated
search loss, balanced/phase/material strata, CP errors where meaningful, residual
amplitudes and saturation separately. Do not label legacy NNUE outputs centipawns.
No winner from loss alone. Compare equal exposure and measured training wall time;
same epochs are not equal compute. Architecture-specific initialization/optimizers
are allowed, documented, and charged to a proportionate tuning ledger. A failed
implementation is not a failed family; diagnose correctness/conditioning first.

## Evidence gates and resource control

One family and one empirical CPU process at a time. Observed initial machine:
Ryzen5 5500,6 cores/12 threads,~32GiB RAM,Windows11,OpenJDK21+35-2513.
Use one search worker, private4MiB TT, unchanged production neural exact/PVS path,
static leaves, no opening book/tablebase. Qsearch is a separate bounded research
diagnostic, never inferred from production zero qnodes.

1. Feasibility: compilation, independent forward/gradient checks, finite outputs,
   symmetry/information boundary, tiny overfit, deterministic codec/exact optimizer
   continuation, plausible inference/memory. No large runs before these pass.
2. Short training: 4096/16384 examples, up to8 epochs, batch128, seeds71/97.
   Each run <=5 minutes, <=2GiB heap. Initial tuning <=4 justified recipes/family;
   record all attempts and time. Admit scaling for >=2% validation improvement,
   or a credible cost/strength Pareto opportunity; this is not a win threshold.
   G01-specific prospective diagnostic extension:its slow tiny-fit convergence
   and measured~1.6s short training admit one32epoch/16384example one-layer screen
   at the initial recipe,two seeds,<=300s/2GiB. This exceeds the default8epoch
   screen to test4096updates cheaply;it is not a serious-training or win claim.
   See G01 for failed recipes and the frozen scope. Other family budgets unchanged.
3. Serious training: initially131072 examples x8, then262144..1048576 and/or
   additional exposures if curves justify it. Declare question and budget before
   each increase; initial <=30min/run, <=4GiB heap, exact checkpoints where supported.
   Compare curves versus both examples and seconds; no arbitrary generation stop.
4. Replication: at least2 independent seeds/source ranges for a material positive
   mechanism. Fresh held-out confirmation; record source correlation limitations.
5. Engine: common validation roots (initial16 depth3, then128 depth4), warmed
   latency/transition cost, node/time tails, paired reversed-colour matches at
   equal time (initial25ms; survivors50/100ms). Calibrations have identical small
   tuning opportunities and are frozen before confirmation. Gen0/material games
   remain required for claims of learned strength.

Before final matches freeze models, gains, seeds, openings, sample and statistical
rule. Default confirmation:128 pairs per seed,2 seeds, fresh openings, completed
pairs clustered by opening and initialization; report W/D/L, caps, failures,
intervals and Elo only where defensible. Missing/capped games are never draws.
If incomplete, report score bounds counting missing games adversely as well as
completed-pair estimates; do not claim acceptance from a selected complete subset.
Use paired bootstrap intervals as approximate, and conservative bounded-pair
checks where useful. Do not repeatedly peek and stop an ordinary fixed-N test.
A strength recommendation needs a95% score interval above .50, or demonstrated
noninferiority (lower bound >=.45) with a material measured resource/distinctiveness
benefit. Insufficient power is inconclusive, not equivalence. Adjust claimed
certainty for candidate selection and independent confirmation.

## Records, frontier and completion

Every material experiment records question, code HEAD plus source/compiled hashes
for dirty work, data/target/split hashes, architecture/recipe, seed/order, parameter
and memory costs, command/JVM/hardware, elapsed/training time, checkpoint hashes,
all failures, metrics, uncertainty, interpretation and next decision. Compact
results/manifests live here; bulky binaries remain at recorded ignored paths.
Preserve negative findings and useful component mechanisms. Never overwrite runs.
Maintain STATE/frontier and SCORECARD after each material result so chat history
is unnecessary. Reopening rejected work requires a new discriminating hypothesis.

Complete only with current controls characterized, all final-frontier material
families tested or dispositioned with evidence, promising mechanisms seriously
trained and replicated, survivors compared as complete engines, scaling and
resource trade-offs assessed, uncertainties/rejections recorded, and a justified
comparative recommendation. An unfinished promising experiment is not completion.
No requirement to manufacture a replacement. No automatic production adoption.
Stop affected work only for an actual inaccessible prerequisite, unsafe ownership,
unresolved authority/objective decision, necessary unavailable validation, reserved
external/destructive action or out-of-scope expansion. Continue useful independent
authorized work while a dependency is blocked. Human acceptance is separate.

## Bootstrap challenge decisions

1. Legacy-NNUE BRN wins cannot establish superiority over calibrated material NNUE.
   Equal material does not equal residual scale: keep practical and strict controls.
2. Previous factorized edge messages still used an MLP. New relation-product
   readout is a distinct hypothesis, not a rerun under a new name.
3. Simple additive pair tables and weak message networks have prior negative
   evidence; improve the tested mechanism/recipe before spending on repetition.
4. LUT, KAN and circuits overlap but are not synonyms. Discrete state addressing,
   learned continuous edge functions, and hard bitwise inference need separate
   tests. Hybrid combinations wait for isolated mechanisms.
5. Frozen old tests are already opened; no recycled final confirmation. Corpus
   sampling fingerprints are not full-file checksums; payload SHA256 is stronger.
6. Historical successor automatic integration conflicts with this request. Here
   recommendation ends the programme; adoption is expressly reserved.
7. NNUE programme's old no-finalizer rule is scoped to that workstream. This one
   follows current root AGENTS/local noncritical version adapter. Root journal is
   absent and is not created. Bookkeeping is not application evidence.
8. Primary literature motivates mechanisms; FPGA/MNIST/molecular or PDE results
   do not establish Java chess throughput or playing strength.
