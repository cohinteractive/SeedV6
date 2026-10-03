# BRN-3 training-throughput contract

Revision 1, 2026-10-04 (Pacific/Auckland). Authority: the owner's CGLHW
training-throughput request. This is one coherent optimization programme, separate
from the closed viability and learning-strength programmes. Execution state and
resume instructions: [frontier](BRN_THROUGHPUT_FRONTIER.md). Measurements, research,
decisions and failures: [record](../research/brn/BRN_THROUGHPUT_RESEARCH.md).

## Objective and boundaries

Reduce Training Data loading/preparation and BRN-3 training wall time without
weakening training. The owner's >1.5-hour 8M-position, batch128, eight-epoch timing
is context, not a measured baseline. Keep examples, sampling, epochs, batch size,
loss/targets, learning rate and optimizer recipe unchanged by default. Preserve
NNUE, fixed material, BRN-3 information boundary, codecs/resume, calibrated search
and ordinary shared engine behavior. No search, GUI or unrelated architecture work.
No source/model/store overwrites, production-scale research, self-play, strength
tournaments, push, deployment or destructive Git operations.

Class A means semantics-preserving implementation. Prefer exact examples, loss,
parameters and moments against the original/reference path. Class B permits
intended-equivalent numerical execution only with declared tolerances and bounded
short-run/held-out evidence before retention. Class C changes learning methodology;
research may be bounded, but a material default change requires owner approval.
Existing BRN-3 uses sparse MASKED Adam: an absent coordinate and its moments do not
evolve; a touched zero-gradient coordinate DOES evolve. Preserve this distinction.

## Selection and evidence protocol

Profile -> rank measured costs -> research credible alternatives -> cheapest
discriminating prototype -> correctness -> warmed repeat measurements -> retain,
revise or reject -> re-profile. Frontier is dynamic, never a mandatory optimization
checklist. Record substantive external sources with URLs and decision implications.
Investigate ordinary HotSpot vectorization, Java 21 Vector API and credible GPU
options on actual hardware. CPU remains supported. A toolchain upgrade or costly
native runtime must be justified as a material decision, never smuggled into a loop edit.

Use existing verification source set and JFR/custom harnesses. Begin at 4096/16384
positions, scale toward 65536 and optionally 131072 when cheap. Batch128; explicit
epoch count (one/two for profiling, bounded eight-epoch confirmation if warranted).
Each routine measurement has a five-minute ceiling; reduce/terminate excessive
runs. Warm paths before timing, repeat >=3 times for retained comparisons, report
median and all trials; separate startup, load, before/after metrics, training,
optimizer when identifiable, memory/allocation/GC and total bounded workflow.
Fixed source ranges, seeds and example hashes establish comparable workloads.
Use a size ladder; disclose filesystem-cache effects and unrelated host load.
Never claim full-corpus performance from a small run. No 8M x 8 acceptance gate.

Risk-based targeted tests protect changed code, resume and immediate integration.
Shared reader changes also require NNUE/source equivalence tests. No full/slow
suite as ritual. Exact reference comparisons justify efficacy for Class A; Class B
needs numerical and short-trajectory evidence. No expensive games are needed to
validate identical optimization state. Retain only worthwhile end-to-end gains.

## Bootstrap challenge and stopping

Bootstrap inspection found: normal campaigns use SequentialTraining/SourceReaders,
not legacy CorpusView permutation; integer-index epoch shuffle already exists;
gradient stamps/touched indices and relative-row mapping are already cached;
training is single-owner; width8 pair messages and width192 dense input may have
different SIMD suitability; optimizer is sparse while dense head is always touched.
Compute versus optimizer cost, loading significance, current vectorization,
memory-bound scaling and accelerator suitability require measurements, not guesses.
Eight million relation caches would be enormous: do not precompute all pairs
without a memory comparison. Object-heavy corpus storage is a hypothesis, not a
demonstrated bottleneck. GPU exists; Java alone is not grounds for dismissal.
The existing contract/frontier/research convention fits this evidence-driven task.
Acceptance compares unchanged exposure and optimizer state, so faster weakened
training cannot pass. Bootstrap authorizes measurement, not an unmeasured rewrite.

Continue autonomously until meaningful loading/training hotspots are dispositioned,
strongest practical measured improvements are retained/verified, SIMD/GPU are
credibly resolved, and no credible worthwhile measured opportunity remains; or
further work needs an owner-gated learning-method/architecture/hardware decision;
or a genuine blocker prevents responsible progress. Number of experiments and
conversation length are not stopping criteria. Keep a fresh-session resume point.
Turn completion is not owner acceptance or release completion.

## Provenance

Start: clean Git HEAD c6c0bda55cfd40227c845ee805a163bfdcbf3d06, build33.
Root CODEXLOG_CURRENT.md is absent; do not create it. The verified maintained
version helper's begin returned only an overlapping prior reservation, token
9e5cb01570284814a3a7f45a498ae785 (blocked historical operation, before build32).
Work proceeds under DEFERRED_FINALIZATION, explicitly separate from that operation.
Do not finish/recover/bypass the old token or claim a verified new build. This
programme's tracked diffs/commits and research record provide later-work evidence.
Release requiring finalized provenance stays gated; independent local QA may proceed.
