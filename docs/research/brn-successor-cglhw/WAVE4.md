# Wave4 continuation preparation

21 October2026. Part of the existing BRN successor CGLHW; [STATE](STATE.md) and
[CONTRACT](CONTRACT.md) govern. **Preparation is verified on isolated fixtures.
No owner-library Wave4 lineages or human-training gate have yet been admitted.**
Finish/reconcile the fixed Wave3 clock studies before deciding to create these arms.

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

Each proposed run adds32 generations,131072 training positions/generation,
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

Next: finish the fixed Wave3 clock studies and decide whether this continuation
has greater expected value than independent confirmation or another bounded
diagnostic. If admitted, create the six owner lineages through the explicit plan,
audit their payloads/cursors, verify their actual native GUI bindings and issue one
consolidated human gate. Do not treat this preparation document, a later invocation
or fixture success as evidence of owner training.
