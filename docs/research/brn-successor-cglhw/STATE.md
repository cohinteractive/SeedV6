# BRN successor workstream state

Updated 20 October 2026. **Active: Wave 2 evidence reconciliation and controlled diagnostics.**
Governing [CONTRACT](CONTRACT.md), [bootstrap/challenge](BOOTSTRAP.md),
[implemented wave and handoff](WAVE1.md), [current findings](WAVE2.md),
[exact initial lineage manifest](wave1-lineages.json).
This is the established successor programme; the completed older BRN programme and Search contracts
retain their own evidence and scope. No playing-strength improvement is established.

| Candidate | Implemented formulation | GUI lineage UUID | Current state |
| --- | --- | --- | --- |
| W1-GATE | Four context-gated local pair experts | `699672ff-5dcc-326a-afef-924996c76f79` | Gen200 frozen for diagnostics; live Best Gen198 |
| W1-CUBIC | Rank16 quadratic/cubic ANOVA factors | `e04052b9-6784-3c12-abb5-abc75cd21f59` | Gen200 frozen; live Best Gen200 |
| W1-POOL | Rank16 pooled tanh nonlinear control | `97b2fdda-f812-3455-9f62-43e5ee322651` | Gen200 frozen; live Best Gen200 |

All live under `E:\SeedV6-Networks`, in the architecture folders recorded in the
manifest. Created with matched Adam 0.003 / batch128 / 8 epochs / seed1,
131072 training positions and held-out validation. The registered external CP
source was selected. User training has now been verified through 200 completed
generations each, 26,214,400 retained training positions, 209,715,200 sample visits
and 1,638,400 optimizer steps. The prior training gate is satisfied for diagnostics;
this does not establish competitive maturity. Historical handoff remains in WAVE1.
Current immutable bindings and hashes: [trained manifest](wave2-trained-checkpoints.json).

Completed: inference and separate training codecs, optimizers and exact continuation;
existing corpus/training/checkpoint services; stable layout-group data partition;
GUI architecture/lineage/settings and Play/Arena model loading; automatic idempotent
wave creation. Math, information-boundary, gradient, tiny learning, runtime/search,
corpus, interruption/resume, shared regression and Arena fixture checks pass.
Native Windows Swing checks loaded all three actual owner lineages with Start
enabled. A model-browser fallback identity defect was found and repaired.
The full Gen32 reference and optimizer were verified; original checkpoint/reference
hashes were preserved. Details, failed attempts and validation limits are in WAVE1.

Measured Gen0 cost: 52-105kB runtime payloads but slower current full-refresh
inference than Gen32 rolling tables. No incremental candidate implementation,
sustained training-throughput conclusion, competitive tournament or strength ranking.
Training loss and latency do not select a winner. GATE's weaker tiny-fit result is
an open diagnostic concern, not a rejection. Features omit explicit attack/pin/
blocker information; empirical gain calibration remains open for every candidate.

## Current evidence and next admissible work

Human actions currently required: **None**. Existing trained checkpoints permit
further autonomous work. Do not launch substantive training or resume the user's
paused Learning Arena campaigns. User-reported poor matches are confirmed as
preliminary observations, with material search-policy and data confounds.

Checksum-validating readers verified all four frozen payloads/optimizer states,
complete parent chains, history (no warnings), source ledgers and saved Arena
campaigns. Gen32 is zero-initialized, with 28,049,408 training positions and
224,395,264 visits: half BT4, half Lichess CP. W1 is CP-only. Nominal generation
counts were misleading. Raw records: `raw/wave2/audit-01` and
`raw/wave2/training-reconciliation.json`. Do not substitute live Best/latest.

Common-policy Gen0 numerical/search parity passed. Production dispatch differs by
family, and historical frozen-reference opponents used pinned Construction V1.
Eight-opening fixed-node diagnostics (`wave2-node-screen-plan.json`) finished:
GATE 9/2/5; CUBIC 10/0/4 on seven valid pairs plus one failed pair; POOL 5/4/7.
These are exploratory, random-opening, short-budget results, not strength claims.
Exact-history replay reproduced the failure: the retained search table's ordering
exhausts8192 nodes before the first completed iteration. It is preserved as a
SEARCH_FAILURE, not a draw or replaced game. Python-chess independently replayed
all6,285 recorded moves and checked all completed game terminations.

The fixed data probe (`wave2-data-plan.json`, `raw/wave2/data-01`) found exact W1
trainer/runtime raw parity on 4,096 positions. GATE full-function cross entropy
slightly beat Gen32 on both samples; uniform-gate and unary-only ablations worsened
it. Cubic terms also improve CUBIC's sample prediction. Data errors do not select
the chess winner. The sampled sealed bucket is now development evidence, not
independent future confirmation.

Completed `wave2-clock-screen-plan.json` (128 games), common Policy4,
50ms/move,32 book openings, reversed colours: GATE14/17/33 (35.16%),
CUBIC23/11/30 (44.53%). All16,152 moves/terminations independently replayed.
No failed/capped pairs or fallbacks. GATE regressed under these conditions; CUBIC
is inconclusive. Records remain in `raw/wave2/clock-screen`; do not rerun it.
Check for an active JVM before launching another batch; no parallel benchmark work.
Plans and source-backed reproduction commands are in WAVE2 and the runner CLI.
Trained latency probe and13 selected math/runtime tests pass.
An opt-in GATE block cache is implemented and passes three additional tests plus
actual Gen200 oracle/search probes (`wave2-gate-cache-plan.json`,
`raw/wave2/gate-cache-01.json`): zero integer-score differences on6,333 positions,
identical48 paired searches, approximately1.30x search speedup. Ordinary loading
still uses full recomputation. The original clock screen must continue exclusively
from `raw/wave2/clock-runtime/lib/*`, not rebuilt research distribution jars.
The qualifying cache implementation advances build42→43 in its own commit.
Active: `wave2-calibration-plan.json`,128 exploratory games across cached
GATE gains.25/.5/1 and CUBIC.5, reusing16 development openings. Three gain-scaling
tests and a four-JVM resume smoke pass; original models remain untouched.
Resume `raw/wave2/calibration-screen` exclusively with the frozen
`raw/wave2/calibration-runtime/lib/*`. Each invocation plays at most12 games or
240 seconds (finishing its current game). Stop/investigate any runtime failure.
No success claim from either development screen.

Hypothesis frontier: calibration and runtime cost remain active; test a similarly
history-independent CUBIC cache if justified by its throughput deficit. Separate
GATE's learned mean mixture from conditional variation before claiming that its
extra expressiveness contributes useful value. Re-match existing from-scratch
campaign snapshots under common search if needed to isolate learning efficacy.
Only then select retraining/hybrid architecture work. User training is not yet a
blocker; no new training gate has been invented.
Retain W1 designs pending these results; speculative Wave2 redesign is premature.
Prepare a narrowly scoped explicit common-policy Arena option if needed for fair
user comparisons, preserving historical campaign semantics and production identity.
One game,
one worker per engine, ponder off, initially 64MiB TT, below-normal priority, no
competing heavy jobs. Competitive confirmation and any claimed gain require later
independent evidence. Training changes requiring substantial learning return here.

Later frontier: investigate calibration, normalization and training issues first;
then incremental inference, sparse attack/blocker features, higher-order/gating
hybrids and controlled ablations if evidence supports them. Continue after failures
or an initial success under the contract; this handoff does not close the programme.

## Fresh-session recovery

Read CONTRACT, this STATE and WAVE1; inspect Git and actual files before acting.
Initial HEAD was `38e5626a6bfb76690a539d0ce0e04e70bd57f9d1`, clean, build40.
The coherent implementation commit containing this state advances application build
to41 under the owner's commit-based rule. No finalizer was invoked; root journal
was absent and was not created. No push, production evaluator replacement or installer
deployment. Use the new local distribution launcher in WAVE1 for these architectures.

Raw evidence is local-only under `docs/research/brn-successor-cglhw/raw/wave1` and
`app-gui/build/brn-successor-native`; a fresh checkout lacks it. Preserve prior
failed/preliminary output. Tracked contract, plans, manifest and compact report
provide continuity, not a substitute for missing raw evidence in fresh analysis.
Turn/commit/wave readiness is not user acceptance or programme completion.
