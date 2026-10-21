# Wave8 fixed-basis investigation and preparation

22 October2026. Continuation of the original CGLHW, contract revision2.
Implementation preparation is active while frozen-build57 Wave7 matches finish.
No owner lineage has yet been created, no G8 training action is requested, and no
success or independent strength claim is made. Consult STATE and WAVE7-ASSESSMENT.

## Discriminating question

Can cubic coefficients add useful chess evaluation when the learned quadratic
function and embeddings are held fixed? Coupled end-to-end optimization changes
the lower-degree function as well as cubic terms. Q4 versus C4 training and masks
therefore do not isolate interference from absence of useful cubic information.

The prespecified small diagnostic in wave7-fixed-head-admission.json fits only
head deltas, with new zero Adam moments and step. Four cubic heads on W7 Q4 improve
all four consumed search-mapped evaluation panels slightly, versus deterioration
for matched quadratic-head changes. Raw results are mixed. R16 does not show a
consistent mapped benefit. These128-update results justify correctness and
integration preparation, not convergence, generalization or a chess claim.
The separate visibility pilot failed its consistency criterion and remains deferred.

## Proposed implementation and controls

Two distinct versioned rank4 layouts retain existing inference mathematics:

- GEN32_FIXED_QUADRATIC_R4, wire9: freeze bias, unary coefficients and embeddings;
  train the four existing quadratic heads only. This remains additive pairwise.
- GEN32_FIXED_CUBIC_R4, wire10: freeze the complete learned Q4 function, including
  quadratic heads; add four zero-initialized cubic heads and train only these.

Both projected functions initially equal their exact Q4 source. No source weight,
optimizer moment, checkpoint or history changes. New head experiments explicitly
start Adam step0 and zero moments, independently of the source optimizer; this is
not represented as inherited Adam continuation. The original source step remains
in origin provenance. The new codecs retain excluded zero moments and reject
nonzero excluded moments on load. Source data/cursors/seek indexes inherit exactly.
Search calibration, Policy4, WDL targets, loss and ordinary architectures remain
unchanged. No extra inference feature, data lookup or research dependency is added.

Tentative consolidated gate, subject to the completed Wave7 practical assessment:
fixed cubic and fixed quadratic from W7 Q4 Latest160, plus ordinary BRE continuation
from W7 BRE Latest160. Explicit rate.001, batch128, eight epochs,131072 training
positions/generation, seed1, one worker, held-out WDL,eight additional generations161..168.
The four-parameter pilot changed rapidly within128 updates; eight diverse generations
provide65536 updates and1048576 new training examples per arm. This shorter exposure
tests adaptation without mechanically repeating the prior32-generation schedule.
BRE retains its full optimizer; the two four-parameter experiments have new head
optimizers. Matched future residual data does not erase older BRE exposure confounds.
Do not prepare or run owner training until this record contains actual verified
lineages, implementation tests, runtime parity, packaged GUI evidence and final
gate instructions. Latest168 would be the prospective primary endpoint; Best must
be audited separately. Existing checkpoints and independent openings remain frozen.

## Current validation and next action

Build58 implementation passes195 targeted tests in15 suites: finite-difference
gradients, independent masked-Adam updates, frozen-prefix preservation, exact
serialization/resume, cache/search parity, source/seek inheritance and GUI-service
continuation. See wave8-implementation-validation.json. Native packaged and actual
owner-lineage checks remain pending. An initial synthetic preparation failed the
existing200-character initializer-description bound; its text was shortened and
all affected validation passed on retry. The integrity bound was not changed. Pause bounded tournaments between batches before Gradle, cache probes
or packaging, then resume unchanged frozen57 protocols. Complete Wave7 assessment
before the final gate decision. If practical results change the expected value of
this proposal, revise the decision explicitly and preserve its diagnostic evidence.
