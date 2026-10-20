# Wave 3: preserve Gen32 and learn a compact correction

20 October2026. Implemented training wave within the existing successor CGLHW;
not a new contract, trained result or success claim. Wave2's mixture study is complete; neither mixture established improvement.

## Evidence and question

Wave2 verified substantial W1 exposure, correct gradients/runtime/checkpoints and
several material comparison confounds. Common-search games, cache implementations
and calibration controls did not establish improvement over frozen Gen32.
Matched BT4 GATE/BRE endpoints are roughly competitive in small common-policy
samples, while that GATE endpoint still trails Gen32. Faster inference remains a
major BRE advantage. W1's compact nonlinear functions do not contain all of BRE's
absolute-location pair detail. Existing W1/Gen32 prediction errors are complementary;
the separately frozen mixture study tests whether averaging their existing functions
helps play, including its additional cost.

Next hypothesis: retain Gen32's learned pair function exactly and learn only a
compact conditional or cubic correction. This preserves the useful baseline
representation and initialization rather than requiring a small replacement to
relearn it. It does not assume that a correction will improve playing strength.
Inference cost, overfitting, target/search calibration and redundant corrections
remain explicit risks.

## Implemented formulation and programme revision

Two new, unambiguous architecture/schema identities:

- W3-RESGATE: frozen Gen32 raw evaluation plus the trainable W1 gated residual.
- W3-RESCUBIC: frozen Gen32 raw evaluation plus the trainable W1 cubic residual.

Raw function: `Gen32Raw(position) + correction(position)`.
Search mapping: `material + .25 * (raw - material)`, with the existing symmetric
rounding and score bounds. Zero output heads must reproduce Gen32 exactly.
The baseline is immutable, receives no optimizer updates and is an explicit
inference component, not a queried external teacher. Existing W1 layouts, bytes,
initialization and behavior must remain unchanged.

The Wave1 choice to begin with independent material-based replacements is deliberately
revised for these new identities under the owner's authorization for later hybrids.
No existing trained artifact is converted. Corrections retain their own seed,
optimizer, checkpoint and provenance. Bind the fixed base SHA256
`5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7`
in each new codec/configuration identity; missing or different base must fail closed.
The existing bundled Gen32 asset may be shared, but report its bytes and working
memory in total costs. No hidden live-Best/latest lookup or new teacher feature.

Reuse the existing forward/gradient algorithms, corpus split and continuation
infrastructure. New hybrid initialization and training evaluate the combined raw
function in the existing cross-entropy objective. Reuse verified rank/block caches
where applicable. New explicitly selected hybrid models need the reference's common
Policy4 search behavior; preserve existing W1 and other-family routing and all
historical campaign identities. This is evaluator integration, not search tuning
or production/default evaluator adoption.

## Required validation before a human gate

Independent combined forward and all-parameter gradient checks; zero-head Gen32
raw/integer/search parity; source-base immutability; codec base-binding corruption
rejection; deterministic initialization and exact optimizer continuation; special
moves, perspective, cache transitions and worker independence. Tiny learning and
real corpus-service pause/resume are bounded correctness tests only.

Prepare actual separate GUI lineages with usable settings and native Swing
verification before asking the user to train. Include an additive continued-BRE
control cloned into a new identity from Gen32 when feasible; preserve the original
optimizer/model and source progress. Explain unequal optimizer starting conditions
and data/split limits. Choose and freeze a consolidated training batch only after
the remaining Wave2 outcomes and implementation validation are reconciled.

No substantial training is authorized for Codex. The consolidated human gate below
is now ready. Implementation readiness is not strength or programme acceptance.

## Implementation progress and current restart

The implementation adds `GEN32_GATED`/`GEN32_CUBIC` wire IDs4/5, new
TrainingArchitecture/GUI identities, fixed-base SHA bytes in runtime and optimizer
headers, combined raw forward/training and cached inference. The shared frozen
asset loader checks its own immutable SHA independently of future production
selection. New hybrid selections use Policy4; original W1/other-family routing is
unchanged. Cached states no longer allocate unused scalar workspaces. Legacy W1
research probes retain their three-family material-initializer scope, and the old
calibration override rejects new hybrids rather than silently scaling only their
correction.

37 selected unit cases pass:17 core cache/runtime/blend/hybrid cases,15 math cases,
5 gain-control cases. Tests cover zero-head raw/integer Gen32 equality on4,000
legal transitions, exact production single-worker search results on special/root
positions, nonzero correction/cache scores, base-hash tampering with a recomputed
CRC, every-parameter finite differences, codec identity and exact next update.
Both new families fit the three-position/512-update fixture (squared error about
1.06 to below1e-19); this tiny diagnostic is not strength or generalization evidence.
10 real corpus-service cases pass across all five families, including new hybrid
publication, independent partition, paused optimizer resume, bounded sessions and
source cursor continuation. `verifyProjectBoundaries` succeeds.

Initial compile attempts exposed exhaustive enum switches in existing test helpers
(BrnStoreRemediation, HumanEngineSelection, ResumableTraining, TrainingRecipe and
LineageProvenance). They were extended for the new identities before the successful
integration build; no prior runtime experiment was altered. No full-suite claim.
Native GUI/new-store preparation and remaining checks are now complete below.

Existing retention begins only at completed generation100. A first bounded32-
generation wave leaves all new checkpoints materialized under that policy. Existing
owner stores, models, refs, data and paused campaigns remain untouched. Before any
later extension reaches retention ages, preserve required experimental endpoints
explicitly; do not rely on ignore rules or conversation history for preservation.


## Optimizer diagnostic and chosen first recipe

The prospectively fixed `wave3-optimizer-plan.json` completed18 arms,64updates each,
147,456 sample visits total, one CPU/768MiB, about3seconds; no saved networks or owner
training. For each source, first1024 positions train and last1024 development holdout;
Gen32 Adam cloned afresh per rate, corrections fresh seed71. Curves at0/1/4/16/64 and
function displacement/timing are in `raw/wave3/optimizer-smoke-01.json`.

| Architecture | BT4 heldout CE at .001 / .003 / .01 | CP heldout CE at .001 / .003 / .01 |
| --- | --- | --- |
| Continued BRE | .633202 / .633009 / .635048 | .600250 / .599152 / .607022 |
| Gen32 + GATE | .633484 / .633128 / .637060 | .602294 / .601619 / .611592 |
| Gen32 + CUBIC | .636609 / .635106 / .640678 | .598986 / .598317 / .621638 |

Initial heldout CE .642954(BT4),.598240(CP). Every arm is finite and reduces training
loss. Higher rates fit the tiny training sample more but worsen holdout. Even lower
rates generally worsen CP holdout slightly; fitting does not establish generalization.
Choose .003 for all three human runs as a conservative continuation recipe, including
continued BRE. No optimum/equal-effective-update/strength claim or further post-outcome
sweep. The control's initial optimizer payload remains exactly Gen32 at stored .01;
the explicit .003 recipe applies on first training start, retaining moments and step.

## Actual lineages and validation

`wave3-lineages.json` freezes initial identities, hashes and source selection in the
existing E:/SeedV6-Networks library. Read-only second preparation produced identical
entries (`raw/wave3/lineages-recheck-01.json`). Existing settings, checkpoints, source
progress and refs are never reset; foreign target directories fail closed.

| GUI name | Architecture folder / UUID | Initial optimizer step |
| --- | --- | ---: |
| W3-RESGATE | BRN-GEN32-GATED/e6b6775d-39cd-392b-bd18-5e964a445f62 | 0 |
| W3-RESCUBIC | BRN-GEN32-CUBIC/22b42be6-15c4-3f36-a376-3776aa1931f2 | 0 |
| W3-BRE-CONTROL | BRN-Pair2/ed0750a3-3163-3979-9f26-2e466c49c771 | 1753088 |

Native Swing loaded all actual lineages, enabled Start, displayed32 Additional
Generations and correct recipes, and checked Play loading against Gen32. Screenshots
were visually inspected at1440x950; explanations fit. All56 sampled original frozen/
W1 payload, root-metadata and ref files are hash-identical before/after preparation.
No original model, pointer or paused user campaign was changed. Evidence is under
`raw/wave3/verification-owner`, `protected-before.json`, `protected-after.json`.

Passed121 selected cases:17 core cache/runtime/blend/hybrid,15 all-parameter math/
continuation,5 calibration guard,10 corpus-service integration,11 streaming initial
loss,17 GUI generation-limit,3 new GUI/idempotence/collision,15 Human Engine selection,
15 recipe/optimizer preservation,5 store remediation and8 shared Learning Arena.
Actual-owner GUI checks were repeated. No full-suite claim. Module boundaries and
local distribution builds pass. Available JUnit XML/logs are in `verification-*`;
earlier observed successful math/corpus invocations' standard Gradle XML was replaced
by later selected suites. No substantive training was performed.

`wave3-smoke-plan.json` completed six deliberately eight-ply-capped actual Gen0 games
across two JVMs. Independent replay verified48 moves; caps are expected and excluded
from scoring, with no unexpected failures or fallbacks. New match bindings record
native residual cache and frozen base hash. Evidence: `raw/wave3/native-load-smoke`,
`native-smoke-analysis-01.json` and frozen `smoke-runtime` jars.

## Initial cost and a diagnosed preflight failure

The actual Gen0 preflight used6,333 saved positions, seven rotating50,000-evaluation
passes and48 matched32768-node requests per full/cache/reference implementation.
`raw/wave3/w3-resgate-preflight-02.json` and `w3-rescubic-preflight-02.json` completed:
full/cache raw difference0, integer differences0, all search results identical to
Gen32. Fresh/history raw sums differ by at most7.994e-15 pawns, with zero integer
differences. First GATE attempt stopped on raw-bitwise history equality: appropriate
for W1 correction alone, but not for included BRE rolling floating arithmetic.
The documented retry amendment requires raw error <=1e-9, zero integer differences
and exact search equality. Original failed stderr/jars remain; no engine arithmetic
was changed to hide this. The initial and revised `preflight-runtime-r2` inventories
are separate. The old probe stdout word 'Trained' is generic wording, corrected for
future runs; these checkpoints are explicitly untrained Gen0 corrections.

| Initial evaluator | Total runtime bytes | Immutable primitive bytes | Cache/base primitive bytes per worker | Median transition ns | Matched search seconds |
| --- | ---: | ---: | ---: | ---: | ---: |
| Gen32 + GATE cached | 847496 | 852080 | 6232 | 1133 | 2.184 |
| Gen32 reference, GATE run | 795032 | 799688 | 2920 | 142 | 1.033 |
| Gen32 + CUBIC cached | 899816 | 904400 | 10162 | 734 | 1.683 |
| Gen32 reference, CUBIC run | 795032 | 799688 | 2920 | 151 | 1.000 |

GATE adds6549 trainable parameters; CUBIC adds13089. The shared baseline is counted
once; object/array overhead, ordinary search state and64MiB TT are additional. Full
refresh took1573ns/879ns. Zero heads have no special fast path. Matched search cost
about2.11x/1.68x Gen32 is a significant risk; these are local initial-weight timings,
not trained throughput/strength. Remeasure trained endpoints before practical claims.

Development errors retained in logs: new test omitted required score mapping;
PowerShell split an unquoted Gradle property; new task used Project at execution
time, incompatible with configuration cache. All repaired before successful owner
preparation. Failed attempts did not train owner models. No browser work was needed.

## Human training gate G3: blocking trained-candidate assessment

Purpose: test Gen32 plus compact correction against further additive BRE training.
All three lineages already exist. Use the local closing implementation build46:

```powershell
.\app\build\install\seedv6\bin\seedv6.bat gui
```

Older installed builds do not support these schemas. In Network Training, select
W3-RESGATE, W3-RESCUBIC and W3-BRE-CONTROL in turn; no manual creation/import.
Each is preconfigured with:

- Learning rate0.003, minibatch128,8epochs, shuffle/run seed1, one worker.
- 131,072 retained training positions/generation and32,768 validation positions;
  held-out WDL validation. Running optimizer loss is cross-entropy; final train/
  validation display is half squared outcome error, so magnitudes differ.
- Registered BT4 Q and Lichess CP, weight1 each, source selection identity
  `1cfa6dd2a74bb8f6e1f421c3fa10824ec03291cde92f0dfe293f60438f9e65f5`, same as Gen32.
  Exact paths/identities/target profiles are in the manifest; preserve this mix.
- Fixed Number of Generations, **32 Additional Generations** from fresh Gen0.
  Run one lineage at a time to completed new generation32, then stop. This is a
  first diagnostic batch, not a universal maturity threshold. Stop earlier for
  nonfinite loss, errors, unacceptable contention or clearly unstable behavior;
  report why. No intermediate reporting gate is required.

A crash leaves an ACTIVE session that recovers its original remainder on Start.
An explicit safe Stop ends its session: before a new Start set Additional Generations
as `32 - settled completed generation`, retaining unfinished optimizer work. Do not
accidentally start another full32-generation session after completion. Keep seed,
source selection, batch, epochs and rate unchanged through resume.

Each completed32-generation run adds4,194,304 training positions,33,554,432 visits
and262,144 updates. Expected correction optimizer step262144; continued BRE2015232
includes inherited1753088. Equal added counts do not mean equivalent lifetime
optimization: baseline knowledge is shared, correction parameters are new, and BRE
retains historical sparse Adam moments.

Fresh source progress starts at zero, so examples can overlap Gen32 history. This
probes correction of remaining fit, not wholly new data. Hybrid stable-layout splits
differ from BRE sequential holdout; same mix/exposure is not identical positions.
Even hybrid holdout may have been seen by the frozen base. Neither whole-model
independence nor game-disjointness is established. Later independent chess matches
remain essential. Historical family-dependent GUI Arena comparisons are not decisive.

Preserve every new checkpoint, source ledger, metrics and settings, including Gen0,
8,16,32 and Best. Existing retention does not prune below generation100. Do not extend
beyond this batch or change retention before the next evidence review. Do not rename,
overwrite or train original Gen32/W1 lineages.

Resume condition: report which reached new generation32 (or stopped early and why),
any settings changes, latest/Best generations, observed loss behavior and rough
elapsed training time/contention. Codex will inspect actual histories/hashes/exposure;
a later invocation alone never satisfies this gate. Independently ready candidates
may proceed while another remains pending.

Next Codex work: verify and freeze actual endpoints, raw/runtime/cache parity and
calibration; remeasure costs; prospectively screen common-policy strength including
continued BRE. Before an expressiveness claim, ablate GATE to fitted constant-gate/
additive behavior and CUBIC to quadratic/unary, accounting for real cost. Reserve
fresh opening clusters and predeclare error/multiplicity/stopping rules for independent
confirmation against frozen Gen32. No unchanged Wave2 screen remains pending.

Human actions required after this prompt: **Blocking: perform G3 training and report
readiness or failure.** This blocks trained Wave3 strength/ablation/optimization
assessment. No other human action is required. Success/adoption/deployment/final
user acceptance remain unestablished. This qualifying implementation includes build46;
Git establishes whether the closing commit exists. No push, deployment or finalizer.

Reproducibility note: the executed smoke plan retains its original CRLF bytes,
which participate in the receipt hash. Whitespace verification permits CR at EOL
for this frozen file; no post-execution normalization or receipt rewriting.
