# R057 resource handoff - unfinished bounded experiment

At2026-10-29T03:59:46Z (29October16:59:46 Auckland), the existing resource monitor
observed83.333% system CPU, above the80% active ceiling. Available RAM15.426GiB
passed its threshold. The supervisor stopped the current research build and
released its Job Object at03:59:47Z. Observed status is NORMAL, active:null, owned0.
No resource-availability retry or new experiment followed. The VSCode-owned
Gradle daemon was identified by its parent and global Gradle home and preserved;
it is not an owned research child. See [r057-resource-gate.json](r057-resource-gate.json).

**Bounded R057 is PAUSED_RESOURCE_GATE and incomplete.** The broader programme
remains **PAUSED - AWAITING HUMAN RESEARCH DECISION**. The existing authorization
still covers unfinished R057 after explicit RESUME and fresh safe admission;
there is no requirement for another architecture decision merely to finish it.
Its final research-decision pause happens after the bounded investigation ends.

## Interim answers, not a final research verdict

A. **Yes for the tested small untrained ONE configuration.** Incremental width8,
rank4 ONE measured.510us versus CP641.066us complete lifecycle per readout on the
same84,939-event trace. CP64 used its launcher-compatible vector head. ONE's
whole-search NPS was1.266x CP64 under common settings. Nine rotated timing rounds,
30roots, zero measured hot allocation. [Efficiency report](R057-EFFICIENCY.md)
separates components, memory, initialization, numerical limits and tree effects.
This does not establish efficiency for the eventual larger/trained configuration.

B. **Unanswered: no R057 training yet.** Actual training samples0, exposures0,
optimizer checkpoints0. The original CP64 corpus is verified; equal-exposure
ONE training was not completed or waived. No weak pilot result caused this stop.

C. **Predictive and playing proximity to CP64 is unknown.** No new prediction
comparison, candidate game, tournament, final model or strength estimate exists.
Untrained throughput does not imply equal-node or equal-time playing strength.

D. Computational gains come from immutable products and compact two-orientation
incremental pools; recipient nonlinear readout remains. Learning differences
cannot yet be explained empirically. The small ONE has19,201 active/25,409 stored
parameters versus CP641,970,440. Capacity, optimization and representation remain
confounds. No production search settings or defaults changed.

E. There is a credible **inference** path. Production competitiveness remains
unestablished until adequate training, fresh predictive/playing evidence and
trained-capacity cost checks. No promotion is authorized.

F. No additional experiment beyond the already authorized R057 is proposed or
launched. The present evidence supports preserving its unfinished learning
question, while the broader programme stays suspended. A research-continuation
decision after completed R057 remains the owner's responsibility.

## Preserved data and validation

Original CP64:33,554,432 samples and268,435,456 exposures, eight corpus-equivalent
passes; original source, manifests, targets and data are present. New exact audit:
33,164,045 canonical aliases,390,387 additional duplicate samples;39,396 samples
must be excluded to protect reservations. Independent Python and full Java
accounting pass. [Data report](R057-DATA.md) preserves provenance, distributions,
leakage limitations and the prospectively frozen same-source replacement plan.
All32 index shards, exact merge, exclusion mask and588,429 reserved keys survive.
No completed indexing stage should be repeated.

Latest successful distinct focused suites total29 tests:6incremental ONE,
4unchanged low-rank reference,3research mapping,1MIX integration,13conventional
NNUE and2corpus-index checks. Numerical and whole-search checks are runtime
evidence; source inspection is not counted as testing. Full application suite,
GUI, production packaging, supplementary extraction, capacity training,
full-scale training and playing tests were not completed here.

The prepared supplement initially failed compilation because zstd was only on
the transitive runtime classpath. The same already-pinned zstd dependency was
added explicitly to research compile scope. The retry hit the resource gate
during compilation. **That supplement and its two new tests are unvalidated.**
No supplementary data was extracted; no fresh validation panel was consumed.
Do not claim the pending test class passed or the full current tree is validated.

The22 supervised stage attempts sum385.513s wall time, including failed attempts;
this is neither total researcher time nor summed CPU time. Minimum sampled free
RAM15.120GiB; one logical CPU, conservative heaps and BelowNormal priority. No GPU
training, clocks, external spend, push, deployment or production promotion.

## Exact resume operation

Verify current instructions/Git, root journal/version presence, saved source
hashes in r057-resource-gate.json and supervisor status. The root journal is absent;
do not create it. No finalizer applies. Existing completed source/runtime snapshots
retain build78 metadata. The new optional core lifecycle seam qualifies for a
commit-time build79; it does not alter CP64 or its production selection. No rebuilt
build79 distribution is claimed at this resource stop.

After the owner invokes RESUME, use a new launch-log name if the one below exists:

```powershell
.\nnue-control.cmd run --name brn-r057-supplement-build-resume --resume-note 'R057 resume pending supplement compile/tests only; completed efficiency and data audits preserved.' --log docs/research/brn-relational-cglhw/raw/r057-supplement-build-resume-1-launch.log -- build/research/venv/Scripts/python.exe -B tools/brn-relational/run_one_stage.py docs/research/brn-relational-cglhw/r057-supplement-build-plan.json
```

The runner freshly requires CPU<60%, free RAM>=8GiB and supervisor permission;
it stops on the established active thresholds. Expected build10-30s, one logical
CPU, isolated Gradle home, no GPU. On failure preserve evidence and stop without
availability retries. On success freeze a new runtime and supplement validation
receipt, then run r057-supplement-config.json through a separately bound monitored
stage. Verify all supplemental labels/packing independently before using them.
Next construct the exact logical33,554,432-sample corpus, select within the three
bounded capacities, freeze final training/evaluation methods, and checkpoint the
principal268,435,456-exposure attempt. No existing training cursor needs recovery.

Reusable instruction:

> RESUME CGLHW seedv6-brn-relational-20261029 in C:\projects\seed\java\seedv6-unified.
> Start at docs/research/brn-relational-cglhw/STATE.md and obey its current gate.

Human actions required after this prompt: **Blocking - invoke RESUME when one
conservative CPU worker can coexist with normal use.** This blocks pending build,
data preparation, training and evaluation. No GPU/clock/promotion approval is
requested. This is a resource handoff, not completed or accepted R057 research.
