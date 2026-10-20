# Wave4: exact-state rate and maturity continuation

21 October2026. Part of the existing BRN successor CGLHW; [STATE](STATE.md) and
[CONTRACT](CONTRACT.md) govern. Preparation was verified on isolated fixtures.
**G4 is ready and blocking trained-candidate research.** All six owner lineages
exist and passed native GUI readiness checks following completed Wave3 clock
reconciliation. No substantive training has been performed by Codex.

## Question and controls

Does lower-rate continuation improve the trained corrections' maturity and practical
strength, while retaining exact inherited knowledge and an equally continued BRE
control? Bounded trained-optimizer/order diagnostics support this question, but do
not establish an optimal rate. Fresh equal-node results do not distinguish the
half residuals from half-change continued BRE. Correctness and cost work has found
no demonstrated architecture bug, and explicit delta caches reduce measured search
cost. None of this establishes superiority over frozen Gen32.

The prepared plan, `wave4-continuation-plan.json`, binds the original W3 **latest32**
checkpoint IDs, network/optimizer hashes, source configuration hashes, seeds and
sample counts. CUBIC Best31 remains preserved; it is not silently selected as a
different starting point. Intended arms:

| Starting family | Rate arms | Intended names |
| --- | --- | --- |
| W3-RESGATE latest32 | .001 and .003 | W4-RESGATE-LR001 / LR003 |
| W3-RESCUBIC latest32 | .001 and .003 | W4-RESCUBIC-LR001 / LR003 |
| W3-BRE-CONTROL latest32 | .001 and .003 | W4-BRE-CONTROL-LR001 / LR003 |

Each admitted run adds32 generations,131072 training positions/generation,
batch128,8epochs,seed1,one worker,held-out WDL validation, equal registered
BT4/CP weights. No owner training has occurred. Any eventual handoff must verify
the actual created roots, loaded GUI settings and checkpoint identities first.

The two arms within each family start with identical weights, Adam moments/step,
source offsets and completed reservations. Training generation numbers continue
from32; the first new generation is33 and32 additional generations end at64.
The bootstrap payload retains its original stored learning rate; the explicit
GUI recipe changes the rate at the first new update without resetting moments.
No half-correction inference scaling is baked into a training checkpoint.

Cross-family data remain different: residuals use the stable geometry partition,
BRE uses sequential holdout, and inherited cursors/exposure differ. This is a fair
within-family rate/maturity intervention, not an identical-data architectural trial.
Actual game comparisons must include frozen Gen32, both continued BRE rate arms
where relevant, calibration controls and costs. Independent confirmation remains
separate from these development selections.

## Implemented safeguards

`BrnContinuationWave` prepares explicit new GUI lineages without training. It
requires the exact frozen source latest, settled session/candidates/reservations,
verified payload/configuration hashes and an exclusive lock on the existing source
writer lock. It does not recover or write the source store. Destinations have
distinct deterministic lineage IDs and immutable `continuation-origin.json`.
Existing destinations are inspected only; edited settings and later training
are preserved. Foreign/partial destinations fail without replacement.

Only source selection, corpus configuration and the complete settled cursor ledger
are copied. Original histories/checkpoints remain in their original roots; origin
records link them. New roots begin from an exact parentless generation32 bootstrap,
avoiding collisions with inherited reservations1..32. New session/history counts
describe added work; inherited generation number is not new exposure.

Provenance now authenticates an accepted parentless bootstrap instead of requiring
generation0. Its wire format and immutability remain unchanged. Unaccepted
parentless candidates and ordinary child candidates are rejected. The GUI shows
`Gen 32 (Starting checkpoint)` rather than incorrectly calling it unfinished.

## Verification and remaining boundary

`wave4-preparation-results.json` records13 passing tests, local log/XML paths,
native screenshot hashes and source-capacity evidence:

- Three architecture cases verify nonzero accepted bootstrap provenance,
  immutable exact payloads, model-library reading and invalid-candidate rejection.
- Three GUI/factory tests verify six exact-state/rate arms, source preservation,
  inherited cursor prefixes, equivalent next updates, matched within-family data,
  foreign/active/locked-source rejection and idempotent preservation of edits.
  Each fixture's actual incremental GUI-service session performs only one tiny
  generation:12 positions,8epochs,8updates, then stops at33 from baseline32.
- Six existing provenance regressions retain historical generation0 behaviour.
- One native test loads all six configurations, asserts recipe/budget/validation/
  start readiness and captures screenshots, without creating a training session.
  Representative screenshots from all three families were visually inspected.

Initial test attempts are retained:01 used an inaccessible test seam and failed
compilation;02 omitted GUI session/incremental opt-ins and failed its session
assertion. Corrected03/04 and native01 pass. These were fixture-harness errors,
not evidence of a production training failure.

BT4 preparation records6,670,929,023 raw positions. CP total remains unknown, but
completed historical consumption of the same source reaches303,903,849 raw records,
well beyond the proposed continuation offsets. This does not guarantee arbitrary
future usable quotas or game disjointness. Never wrap/substitute on exhaustion.

Admission evidence: all320 fixed clock games independently replayed44264 moves,
without failures, caps, missing pairs or fallbacks. At200ms, faster half-GATE scored
44.53125%, faster half-CUBIC46.09375%, calibrated continued BRE53.125% against
frozen Gen32. All descriptive intervals include50%. Delta implementations improve
same-root node ratios from.472 to.631 (GATE) and.578 to.711 (CUBIC), but leave a cost
gap. No candidate is ready for a positive independent confirmation claim.
`wave3-clock-results.json` preserves separate protocols, runtimes and uncertainty.

This warrants the bounded rate/maturity intervention already supported by the
optimizer diagnostics. It does not establish architectural inferiority or promise
that more training will help. The.003 arm controls additional exposure; the.001
arm tests a conservative rate with identical within-family data and inherited
optimizer state. Keep both expressive families and the equally continued additive
control. Do not silently enlarge this batch if evidence remains negative.

## Actual owner readiness

`wave4-owner-readiness.json` records exact roots, bootstrap checkpoint IDs,
network/optimizer hashes, source offsets, recipes, runtime jars and local evidence.
All six latest/Best references point to the new accepted generation32 bootstrap.
Its ID differs from W3 because it is a parentless bootstrap; **weights and full Adam
state are byte-identical to the bound W3 latest32**. The two rate arms within a
family may share the content-addressed checkpoint ID in distinct lineage roots.

| GUI lineage | Lineage UUID | Rate |
| --- | --- | --- |
| W4-RESGATE-LR001 |9d2412de-8563-35f3-b520-ef41e610a098|.001|
| W4-RESGATE-LR003 |77dad574-34c1-3639-b827-0127fcab4fb2|.003|
| W4-RESCUBIC-LR001 |4e693253-bfdd-38b1-b8e2-be30ac620568|.001|
| W4-RESCUBIC-LR003 |6b995953-5bc1-3b7b-bae2-eaf7a5377d9a|.003|
| W4-BRE-CONTROL-LR001 |d1a2e220-c1a0-3953-ad2c-2e001af93e7d|.001|
| W4-BRE-CONTROL-LR003 |819e0f58-6726-3479-8709-bfa7079ad2d0|.003|

The post-preparation audit verifies691 original checkpoint/history/settings/reference/
session/lock files unchanged. The initial inventory excluded `training-data`: that
is the acquisition directory, not scratch as its initial scope string said. Original
cursors, source selection and configuration are separately verified against the
preparation-bound hashes and all copied destinations. Seek/selection archives were
not independently inventoried beforehand. Frozen Gen32's bundled hash remains exact.

The native owner test passes all six actual configurations, including source count,
seed, worker count, recipe, validation, session budget, enabled Start and starting
checkpoint summary. Twelve screenshots are preserved under `raw/wave4/native-owner-02`;
three families' recipe views and GATE's lineage view were visually inspected.
No history/session was created. The initial native check failed because its new seed
assertion requested a spinner rather than the actual text field; corrected retry02
passes. Both logs/XML remain. An initial independent audit assumed text references;
it stopped read-only, then passed with verified SmallRecord binary framing/checksums.
Neither diagnostic error changed a model or trained an owner lineage.

## Human training gate G4

Purpose: distinguish additional exposure from a lower learning rate for each
expressive formulation and the additive control. Use the verified build50 launcher:

```powershell
.\app\build\install\seedv6\bin\seedv6.bat gui
```

Select the six existing names above in Network Training; do not create/import
replacement lineages. Their settings are already prepared:

- Learning rate.001 for LR001 and.003 for LR003. Leave **Keep checkpoint learning
  rate on Resume unchecked** so the explicit recipe applies without resetting Adam.
- Batch128,8epochs,seed1,one worker,131072 training positions/generation,
  held-out **WDL Loss** validation (32768 positions/generation).
- Registered BT4 Q and Lichess CP, weight1 each; preserve the existing source
  selection, offsets and configuration. Do not reset progress or change the mix.
- Training Duration: **Fixed Number of Generations;32 Additional Generations**.
  Run one lineage at a time, from starting32 through completed64 (new generations
 33..64). The initial GUI correctly shows0 newly recorded completed generations,
  latest32 and next33; inherited exposure remains linked to the W3 source.

Each arm adds4,194,304 training positions,1,048,576 validation positions,
33,554,432 training visits and262,144 optimizer updates. Expected final steps:
524288 for residuals and2277376 for BRE. These are audit expectations, not an
instruction to alter checkpoint counters. No half-correction scaling is baked
into the training state.

Stop at64; this is a controlled diagnostic dose, not a claim that64 is a universal
maturity threshold. Stop earlier for nonfinite/unstable loss, errors or unacceptable
resource contention and report the reason. No intermediate training gate is needed.
An ACTIVE session after a crash resumes its original remainder. An explicit safe
Stop ends the session: before restarting, set Additional Generations to
`64 - settled completed generation`, retaining unfinished optimizer work. Never
accidentally start a second full32-generation session after completion.

Preserve the initial32 bootstrap, all new checkpoints33..64, latest/Best, histories,
settings, source ledgers and origins. Existing retention does not prune this batch
below100 generations. Do not train, rename, overwrite or delete the W3 source
lineages, W1 models or frozen reference. Manual Arena matches are not required to
clear this gate; research delta caches are explicit research paths and GUI timing
alone must not decide architectural value.

Resume condition: report which six lineages reached64, or any early stops/settings
changes; include latest/Best if convenient, unusual loss behaviour and rough elapsed
time/contention. Codex will inspect actual checkpoint identities, parent chains,
sessions, steps, recipes, source ranges and metrics. A later invocation alone does
not complete G4. Separately completed families may be assessed while others remain
pending, without treating missing controls as satisfied.

Next Codex work after evidenced training: audit/freeze endpoints and Best separately,
check inference/optimizer continuation and calibration, then compare rate/maturity
effects against frozen Gen32 and the corresponding continued BRE controls. Keep
fresh independent confirmation, selection/multiplicity/stopping rules, nonlinear
ablations and practical costs separate from development selection.

**Human actions required after this prompt: Blocking: perform G4 GUI training and
report completion or failure.** This blocks trained-W4 comparison and the next
architecture/strength decision. No other human action is required. All W3 protocols
are closed; no tournament or autonomous training remains running or queued.
Programme success, adoption and final user acceptance remain unestablished.
