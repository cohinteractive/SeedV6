# Wave6: trained degree control and matched continuation

21 October2026. This is the next experiment within the existing BRN successor
CGLHW, governed by CONTRACT revision2. G5 and all544 admitted W5 development
games are complete. No successor or independent strength superiority is established.
**Preparation admitted; owner readiness and human gate activation pending.**

## Question and evidence for admission

Full R16 scores62.5% against its inference-only quadratic ablation at32768 nodes
(9/2/5, eight reversed-colour pairs; descriptive normal95 49.40-75.60%, bounded
interval14.48-100%). R4 scores46.875%. Full-versus-unary removes both quadratic
and cubic interactions; its favourable shallow results cannot isolate degree3.
Neither ablation lets the retained weights adapt. A separately optimized quadratic
control is therefore needed before attributing practical value to cubic terms.

At262144 nodes versus continued BRE001, R4/R16 score28.125/43.75%; at200ms they
score43.75/34.375%. Different shallow/deep opening sets prevent a causal depth
estimate. These results do not support throughput as the sole explanation, nor do
they prove an architecture inherently unsuitable. R4 improves Gen64-to96 prediction
on fixed old and fresh development panels, including standard-valid CP sensitivity,
while still trailing R16 on all32 same-generation validation comparisons. Compact
maturity/capacity remains a useful secondary question. Isolated R4 inference is
cheaper, but measured whole-search benefit over R16 varies substantially by roots.

Keep the same training objective and mapping for this degree intervention. The
quadratic contribution is useful under the raw training objective; selecting a
cubic-only training architecture from mapped prediction loss would be unjustified.
Mapping/objective alignment remains a later hypothesis, not a simultaneous change.

## Admitted arms and preserved initialization

Exact sources, checkpoint IDs and hashes are frozen in
[wave6-continuation-plan.json](wave6-continuation-plan.json).

| New lineage | Source Latest96 | Intervention |
| --- | --- | --- |
| W6-RESQUAD-R16-LR001 | W5-RESCUBIC-R16-LR001 | Wire7 degree2; retain every unary/embedding/quadratic weight and Adam moment, step and rate; omit only16 degree3 heads and their moments |
| W6-RESCUBIC-R16-LR001 | W5-RESCUBIC-R16-LR001 | Exact payload/state continuation; same starting data cursors as quadratic |
| W6-RESCUBIC-R4-LR001 | W5-RESCUBIC-R4-LR001 | Exact compact payload/state continuation; same residual data history |
| W6-BRE-CONTROL-LR001 | W5-BRE-CONTROL-LR001 | Exact ordinary BRE payload/state continuation |

All rates remain.001. BRE003 is preserved, not discarded: its G5 validation is
worse on all32 matched generations, its primary clock point score tiesBRE001,
and there is no development strength evidence requiring another rate fork now.
This is a bounded resource choice, not proof that.001 is globally optimal.

The new quadratic model is an additive pairwise control. It cannot itself satisfy
the programme's higher-order objective. Its dedicated guarded cache avoids unused
third-moment work; three research prototype replications saved7.5-12.3% isolated
R16 time, not a proven end-to-end speedup. No default evaluator is replaced.
The full C16 versus Q16 comparison is the primary mechanism control. R4 is a
secondary maturity/cost arm; a separately trained Q4 may be needed later if R4
becomes the leading candidate. No such success or fifth training arm is assumed.

## Dose, controls and decision rules

Each new arm receives32 additional generations,97 through128: batch128,8epochs,
131072 training and32768 validation positions/generation,seed1,one worker,
Held-out WDL, existing Training Data sources and equal BT4/CP mix. Preserve
source selection, cursors, CP seek indexes, partitions and optimizer continuation.
This adds262144 updates and33554432 training visits per arm. Expected ending
steps are1048576 for the three residual arms and2801664 for BRE. This bounded
dose matches the observed G5 adaptation interval; it is not a claim of convergence
or a universal stopping rule. No extra training is authorized to Codex.

C16/Q16 share exact pre-intervention training history, retained optimizer state,
objective and future data. The projected Q control still inherits a cubic-trained
initialization; inspect its adaptation before claiming architecture-level inferiority.
BRE retains its different historical cursors/splits and sparse optimizer semantics.
Do not claim all arms saw identical examples. W5 diagnostic panels are consumed
development evidence, with potential overlap in future validation; they are not
independent confirmation. Reserved confirmation openings remain untouched.

After G6, audit all actual Latest/Best identities and full trajectories. Use Latest128
as the primary equal-exposure endpoint, with Best as a recorded secondary choice,
not a silently selected winner. First verify actual cache/runtime/training integrity,
then compare C16/Q16 at practical depth and equal time, and compare promising cubic
arms against continued BRE and frozenGen32. Freeze bounded match protocols before
outcomes; do not reuse these development results as independent confirmation.

If C16 separates from a reasonably adapted Q16, investigate whether the quality
gain survives practical cost before confirmation. If Q16 matches or exceeds C16,
do not claim cubic value from full-versus-unary wins; prioritize objective alignment
or a justified new mechanism. If R4 remains materially weaker at practical depth,
do not repeat unchanged continuation indefinitely. Retain uncertainty about maturity,
data and calibration, but require a discriminating hypothesis for any later dose.

## Preparation and restart boundary

Implementation7e28740/build56 passes126 targeted tests (see degree-control-
implementation-validation.json). They cover independent forward/gradients,
projection/Adam persistence, cache/search parity, source-preserving preparation and
interrupted GUI-service continuation. No full-suite claim.

Before activating G6: create the four distinct owner stores from the frozen plan;
independently audit actual projection, sources/seek metadata and payloads; verify
actual inference/search and bounded legal play; package and inspect native build56
with all four owner selections and recipes; rehash preserved artifacts. No owner
training is requested until those checks pass and the readiness section is completed.
Current human actions: None. Exact active step is maintained in STATE.md.
