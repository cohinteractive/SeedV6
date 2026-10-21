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
head deltas, with new zero Adam moments and step. The original all-row Q4 pilot gives small mapped improvements, but its apparent
cubic separation fails the later status-valid control: quadratic heads improve
more on all four valid panels. Raw cubic results are mixed; R16 has no consistent
mapped benefit. This downgrades the initial rationale. Fixed-basis optimization
remains an unproved isolation hypothesis, not positive expressive-value evidence,
convergence, generalization or a chess claim.
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
continuation. See wave8-implementation-validation.json. Native package58 passes headless image/archive checks; actual
owner-lineage and native GUI checks remain pending. An initial synthetic preparation failed the
existing200-character initializer-description bound; its text was shortened and
all affected validation passed on retry. The integrity bound was not changed. Pause bounded tournaments between batches before Gradle, cache probes
or packaging, then resume unchanged frozen57 protocols. Complete Wave7 assessment
before the final gate decision. If practical results change the expected value of
this proposal, revise the decision explicitly and preserve its diagnostic evidence.

## Data-quality sensitivity before gate admission

The post-pilot audit finds five nonstandard CP fit positions account for91.59%
of initial absolute cubic-head gradient. One has dozens of queens; corpus syntax
and king-count validation admits it. Original all-row results remain preserved.
This weakens the small diagnostic rationale and is not proof of a full-training
causal effect. The bounded status-valid refit admission tests both Q4 head controls
on2043 fit rows,128 updates each, all original status-valid evaluation panels.
Do not admit the human gate before interpreting this sensitivity and closed G7
matches. No source files, training eligibility policy or old histories are changed.
See wave7-fixed-head-sensitivity.json and wave7-fixed-head-valid-admission.json.

The valid-only refit improves mapped CE in both controls, but quadratic heads improve
more in all four panels. Cubic raw CE improves on fresh BT4 and old CP, worsens on
fresh CP and old BT4. The original apparent cubic separation is not robust; do not
cite it as positive evidence of expressive value. Both extreme prior pilot records
are SEALED and were excluded from ordinary residual training. A bounded32768-record
check of actual Gen129/160 CP ranges finds100/56 eligible TRAIN records with bad
orthodox castling rights, but no material-count anomalies. Their counts are at
most32. This does not justify a training-policy change or explain full-wave strength.
All source/cursor/optimizer artifacts remain unchanged. Exact evidence is in
wave7-fixed-head-valid-results.json and wave7-data-health-results.json.

The remaining fixed-basis question is causal optimizer isolation, not a claim that
the cubic pilot beats its quadratic control. Initial functions are identical;
training only new cubic heads versus existing quadratic heads can distinguish
useful higher-degree adjustment from ordinary recalibration after broader fitting.
The raw/mapped and source disagreements mean loss alone cannot decide chess value.
Gate admission still requires completed G7 strength results and explicit expected
value review; implementation availability is not sufficient justification.

## Isolated actual-weight readiness

Before owner gate admission, three isolated fixtures under raw/wave8/isolated-library-01
use the exact prospective W7 Latest160 parents. Independent byte/CRC inspection
passes all retained weights, new zero head moments/steps and inherited source/seek
metadata. Six payload/state pairs (fixtures, sources, Gen32) pass24-board runtime
checks. The new cubic and quadratic functions exactly match Q4 at initialization;
BRE matches its source. Four full/half probes each pass6333 positions and48 cache
search pairs, with zero integer discrepancies. Qhead/Chead produce identical full
search results in all96 runs per mapping. Sequential cached inference is192.568/
213.598ns full and205.412/234.320ns half; this is readiness/cost evidence, not a
trained quality or speedup claim. See wave8-isolated-projection.json and
wave8-isolated-readiness.json. An initial audit request used frozen inference-only
stores without managed Best refs; its failure is preserved and the corrected v2
managed-reference audit passes. No checkpoint repair or engine change was needed.

The continuation auditor now supports eight-generation fixed-head runs and verifies
every frozen weight and excluded zero Adam moment through168. W6/W7 audit replays
remain exactly unchanged. Actual trained G8 audit awaits future evidence. The native
GUI test now reads each prepared generation budget instead of assuming32; its
packaged test harness compiles, with actual owner GUI execution still pending.
