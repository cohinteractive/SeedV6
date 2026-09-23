# BRN-2 canonical g128 supervision ablation

Measured 2026-09-23 UTC in the authoritative SeedV6 repository, starting from
commit `c3d516d`. This follows
[the accepted canonical g128 diagnostic campaign](BRN_CANONICAL_G128_DIAGNOSTICS.md).

**The WDL control reproduces all 128 generations exactly. Dense NNUE supervision
produces substantially smoother relative local values, strong teacher agreement,
and a completed bounded depth-4 Kiwipete search with the unchanged BRN-2 model
architecture.** The 50/50 arm preserves substantially more WDL predictive performance
and also resolves the measured Kiwipete expansion, but does **not** inherit the
teacher arm's fixed-corpus scale-independent stability. These are diagnostic
results, not playing-strength evidence or authorization to change production.

## Source, initialization and isolation

The source is `E:\SeedV6-Networks\BRN\BRN-2\training001`, architecture
`seedv6.brn.2`, feature schema 2, width 32. The experiment reads the exact persisted
`bootstrap/<parent-checkpoint-id>.plan` and `.data` pairs for generations 1–128.
It decodes them through the production checksummed readers, retaining the saved
training/held-out lists, their order, whole-game membership and saved shuffle seed.
It neither regenerates games nor reconstructs a partition. The 128 data hashes,
plan hashes, seeds and source Candidate identities appear in the experiment's
`replay.jsonl`, linked to the canonical checksummed history and validation evidence.

Exact starting checkpoint:

```text
E:\SeedV6-Networks\BRN\BRN-2\training001\checkpoints\g000000-s000000000-31a42b7e131558c59e7868e45b759f1e5a322eb2bef5ff26e7b9386e4f831507
network.brn2 SHA-256:
195d4300ce1b90a30cb888d6990a872f33165bfadd66cf1f0212ba3f0c2f653f
training.state SHA-256:
dec6eccdf4d9e59d4ea9f9843831548a045e07d050032f3c4ab62767d8c595bc
generation 0; optimizer step 0
```

Each arm independently decodes that exact surviving model and Adam state. There
is no newly initialized substitute. Every arm processes **208,104 training samples**
and **52,974 held-out samples**, with final latest-training step **208,104**.
All use one shuffled online pass, minibatch 1, Adam learning rate 0.001, beta1 0.9,
beta2 0.999, epsilon 1e-8. Every next generation reloads the preceding Candidate's
serialized training state, including Adam moments, whether or not it promoted.

The generating campaign's depth 2, six threads, 64 requested games/generation,
0–8 opening plies, maximum 32 samples/game and 1,024-ply cap remain characteristics
of the **same saved dataset**, not new search invocations. There is no BRN training
search, self-play generation, Candidate-versus-Best game arena or new data.

Exact pinned teacher, verified against every saved generation plan:

```text
E:\SeedV6-Networks\NNUE\training\checkpoints\g000074-s000008942-2e22ccbfdeb18f5f26f91f0df183411ac1d01844ccd43e56e9005ee3f5837bc6
network.nnue SHA-256:
3279ff72654f56c8f5ce3cc89cc55d1f253de6ae9df00bd61a41293f57dd16a9
training.state SHA-256:
453b7259d4e8041fa4abb8cc0887540434871d2b151c33676f36e749e0c6cb34
generation 74; optimizer step 8942
```

`NnueEvaluator.evaluate(sample.board())` rebuilds the static evaluator on the exact
six-long sampled board and selects that board's side to move. Its `boundedValue()`
is native `StrictMath.tanh(raw)`, retained as a double. There is no integer search
score, centipawn conversion, mate encoding, search result, sign reversal to a fixed
White perspective or calibration in any target. The NNUE implementation is unchanged.

For terminal side-to-move WDL `w` and normalized static teacher value `t`:

| Arm | Target | Per-sample loss |
|---|---|---|
| WDL CONTROL | `w` | `0.5 * (prediction - w)^2` |
| BLENDED | `0.5 * w + 0.5 * t` | `0.5 * (prediction - target)^2` |
| TEACHER | `t` | `0.5 * (prediction - t)^2` |

The existing BRN-2 trainer already accepts finite targets throughout [-1, 1]; no
loss incompatibility was found. Persisted `TrajectorySampler.Sample` still enforces
terminal WDL. An explicit callback overload freezes the selected targets before
updates and uses the existing production shuffle, gradient, optimizer and metric
passes. Ordinary calls select `Sample::target`. An analogous explicit held-out
overload applies one target to both actors. No GUI or production source selector
exposes these experimental objectives.

Candidate and incumbent Best are compared on that generation's original held-out
partition, using the arm's own objective. Strictly lower Candidate loss promotes;
ties retain Best. Cross-objective metrics never enter promotion. The experiment
exports separate Best snapshots at boundaries and a final latest-training snapshot;
its JSONL records every Candidate hash and decision. These are explicitly
experimental exports, without production store manifests, promotion records or
`refs/best` that a normal store could adopt. Intermediate non-milestone payloads
are not retained, and the harness has no interrupted-run resume mechanism.

## Reproducibility gate

**PASS, exact, tolerance zero.** For each of all 128 generations, the WDL replay
matches the canonical manifest's model SHA-256, complete optimizer/training-state
SHA-256 and step, the history's post-training training loss, both held-out losses,
the promotion decision and incumbent/Best chain. This also covers original early
payloads that have been pruned: their authenticated manifests and history retain
the expected hashes and losses. No serialization exception or approximate-numeric
waiver was needed. g0 model serialization also matches exactly.

The replay reproduces **64 promotions and 64 retentions**, final published Best
**g121**, and latest-training **g128**. BLENDED and TEACHER are called only after
all 128 comparisons pass; any mismatch throws and stops execution. The actual
control's final 248 static evaluation records also equal the earlier canonical
records exactly. The earlier WDL g121 and NNUE g74 search records were therefore
reused; their saved artifact SHA-256 inventory was rechecked.

The three arms' retained final Best identities are:

| Arm / Best | Model SHA-256 | Training SHA-256 |
|---|---|---|
| WDL / g121 | `cc2072fe57fcfbc8ca8f83111586e3bfff4fe0abb554998e8ca8b859d1786c47` | `5b29899b380937769ebf2f1a90153916797311c3bc95dd706fdf3a3280e50d4c` |
| BLENDED / g125 | `570e78b8e64369ca2fa9ec102c8f5fbbb7a0f39ef205062a4cd44cc12e726935` | `36b77a6aaefcf84616ef7aa0a0f3f040ffeb46955d6790634e75057d319136c9` |
| TEACHER / g127 | `286dd8151abe7e935c1fd8b26834171a7c08565357a4416e42582ceffb2ed1d8` | `7d92e70fdb343b9e4d78df1cf8fb89acf818431a353cf04469edfdbee8363872` |

## Training and cross-objective losses

These are mean **post-training** training losses and Candidate/incumbent held-out
losses, averaged across generations in each block. Each row uses its arm's target.
Different objectives have different irreducible disagreement and cannot be ranked
by their own loss magnitudes. Held-out games change every generation, so these
are not curves on one permanently unseen validation population.

| Arm | Generations | Training loss | Candidate held-out | Incumbent held-out | Promotions |
|---|---|---|---|---|---|
| WDL | 1–32 | 0.078578 | 0.216098 | 0.228552 | 20 |
| WDL | 33–64 | 0.0844821 | 0.191075 | 0.190623 | 14 |
| WDL | 65–96 | 0.0881957 | 0.176186 | 0.179608 | 18 |
| WDL | 97–128 | 0.0887957 | 0.176716 | 0.169031 | 12 |
| BLENDED | 1–32 | 0.0256389 | 0.068939 | 0.0754293 | 15 |
| BLENDED | 33–64 | 0.0235409 | 0.0551489 | 0.0542736 | 15 |
| BLENDED | 65–96 | 0.0239827 | 0.0493799 | 0.049481 | 14 |
| BLENDED | 97–128 | 0.0248509 | 0.0491053 | 0.0475623 | 13 |
| TEACHER | 1–32 | 0.0117116 | 0.0315539 | 0.0368381 | 13 |
| TEACHER | 33–64 | 0.00844553 | 0.0199336 | 0.0198953 | 17 |
| TEACHER | 65–96 | 0.00796281 | 0.0181716 | 0.0176132 | 15 |
| TEACHER | 97–128 | 0.00703279 | 0.0163134 | 0.0161421 | 14 |

The final Best of each arm was additionally evaluated on the same concatenation
of all **52,974 original held-out samples**, equally weighting sampled positions:

| Arm | Final Best | Promotions / retentions | WDL loss | Blended loss | Teacher loss |
|---|---|---|---|---|---|
| WDL | 121 | 64 / 64 | 0.169613 | 0.0754041 | 0.0952011 |
| BLENDED | 125 | 57 / 71 | 0.168871 | 0.0506825 | 0.0464998 |
| TEACHER | 127 | 59 / 69 | 0.219815 | 0.0611599 | 0.0165098 |

TEACHER reduces teacher loss by **82.66%** relative to WDL control, but its WDL
loss rises by **29.60%**. BLENDED's WDL loss is **23.18% lower than TEACHER's**
and only **0.44% lower than WDL control's**; the latter small difference is
descriptive, not a significance or superiority claim. BLENDED reduces teacher
loss by **51.16%** relative to control. These calculations assess final Best,
not the unpromoted generation-128 Candidate.

This pooled population includes the batches used for Best selection. Whole-game
exclusion holds within each original generation, but positions/games are not
globally deduplicated across generations, and early held-out positions may overlap
later training examples. Consequently these are common retrospective descriptive
losses, not an independent out-of-sample strength or generalization test.

## Milestone trajectory on the fixed diagnostic corpus

The unchanged `seedv6-brn2-diagnostics-v1` corpus has 15 roots, 233 legal one-ply
children, 248 pooled outputs, 210 quiet edges and 23 tactical edges. SHA-256:
`0503b850d5ed49686e72e601b1216270cfd40fc3bac18fac4dae697b323f3bd8`.
The corpus definitions and all root FENs remain in `Brn2DiagnosticCorpus` and the
emitted records. No new diagnostic positions were selected after seeing results.

Definitions match the canonical report: raw is actual pre-tanh head output;
normalized is `tanh(raw)`; SD is population SD over the 248 outputs. Parent-child
delta is `abs(-f(child) - f(parent))`, with the child negated into the parent's
perspective. Quiet excludes captures, promotions and delivered checks; tactical is
their union. QΔ/TΔ below are median absolute **normalized** deltas. Q/IQR and T/IQR
divide these by the same model's pooled normalized output IQR, using interpolated
quantiles at `(n-1)*p`. Pearson, average-tie-rank Spearman and sign agreement use
normalized outputs in actual side-to-move perspective; zero is a separate sign.

Each boundary uses the Best existing then, not necessarily that generation's
Candidate. The g0 rows are identical independent starting states.

| Arm | Completed boundary | Best gen | Raw SD | Value SD | QΔ | Q/IQR | TΔ | T/IQR | Pearson | Spearman | Sign agreement |
|---|---|---|---|---|---|---|---|---|---|---|---|
| WDL | 0 | 0 | 0.14943 | 0.144224 | 0.106315 | 0.391446 | 0.0417949 | 0.153887 | -0.363805 | -0.320529 | 0.387097 |
| WDL | 32 | 31 | 0.426017 | 0.370955 | 0.447106 | 0.765288 | 0.34072 | 0.583193 | -0.276583 | -0.227717 | 0.407258 |
| WDL | 64 | 64 | 0.314671 | 0.275143 | 0.394074 | 0.923472 | 0.281697 | 0.660127 | 0.481377 | 0.429309 | 0.592742 |
| WDL | 96 | 94 | 0.321435 | 0.284043 | 0.30705 | 0.929186 | 0.258801 | 0.783177 | 0.033383 | 0.105915 | 0.600806 |
| WDL | 128 | 121 | 0.342219 | 0.302591 | 0.299235 | 0.720925 | 0.155616 | 0.374914 | -0.0981314 | -0.0116057 | 0.560484 |
| BLENDED | 0 | 0 | 0.14943 | 0.144224 | 0.106315 | 0.391446 | 0.0417949 | 0.153887 | -0.363805 | -0.320529 | 0.387097 |
| BLENDED | 32 | 31 | 0.288374 | 0.270661 | 0.15556 | 0.372376 | 0.17811 | 0.426356 | 0.71403 | 0.705541 | 0.774194 |
| BLENDED | 64 | 64 | 0.255225 | 0.236581 | 0.310182 | 0.972106 | 0.529032 | 1.65798 | 0.617906 | 0.525943 | 0.612903 |
| BLENDED | 96 | 94 | 0.240865 | 0.225896 | 0.241522 | 0.876115 | 0.17225 | 0.624833 | 0.52482 | 0.557935 | 0.689516 |
| BLENDED | 128 | 125 | 0.258571 | 0.241056 | 0.244353 | 0.762526 | 0.310018 | 0.967439 | 0.645582 | 0.634893 | 0.725806 |
| TEACHER | 0 | 0 | 0.14943 | 0.144224 | 0.106315 | 0.391446 | 0.0417949 | 0.153887 | -0.363805 | -0.320529 | 0.387097 |
| TEACHER | 32 | 30 | 0.437725 | 0.380124 | 0.14137 | 0.267791 | 0.181797 | 0.344371 | 0.826788 | 0.794022 | 0.806452 |
| TEACHER | 64 | 61 | 0.401985 | 0.355785 | 0.151918 | 0.288135 | 0.19893 | 0.377298 | 0.824303 | 0.799884 | 0.798387 |
| TEACHER | 96 | 96 | 0.463073 | 0.387932 | 0.151774 | 0.311064 | 0.21105 | 0.43255 | 0.873566 | 0.837504 | 0.830645 |
| TEACHER | 128 | 127 | 0.514723 | 0.42515 | 0.121372 | 0.224961 | 0.0851553 | 0.157835 | 0.867828 | 0.838724 | 0.834677 |

Pinned NNUE reference on the identical population:

| Raw SD | Value SD | QΔ | Q/IQR | TΔ | T/IQR |
|---|---|---|---|---|---|
| 0.653115 | 0.475713 | 0.103092 | 0.155645 | 0.128999 | 0.194758 |

The trajectory distinguishes supervision more clearly than just the last model.
At every trained milestone TEACHER has much lower quiet relative volatility and
strong positive teacher agreement compared with WDL. Its quiet ratio is
0.268 → 0.288 → 0.311 → 0.225; its normalized Pearson stays 0.824–0.874.
This is not monotonic convergence. Its tactical ratio actually rises through
g96 before falling at the final boundary; all four values remain below their
corresponding WDL milestone values.

At the final boundary, TEACHER's quiet/tactical relative ratios are **68.80% /
57.90% lower** than WDL's. The absolute normalized medians fall **59.44% / 45.28%**.
Its normalized SD is larger (0.42515 versus 0.30259), so the lower local ratios
are not the result of collapsing the output range. Quiet relative volatility
remains above NNUE's 0.15564; tactical is below NNUE's 0.19476 on only 23 edges.
Smaller deltas are not by themselves greater chess accuracy.

BLENDED's final quiet/tactical ratios are **0.76253 / 0.96744**, versus control's
0.72093 / 0.37491. Its absolute quiet median falls modestly, but its tactical
median increases. The substantial teacher-arm improvement in fixed-corpus
scale-independent stability is **not retained by the 50/50 blend**. Its improved
correlation and search counts must be reported separately from that negative result.
All 15 milestone runs retain exact zero BRN color-reversal residuals.

## Final bounded search

Only final BLENDED and TEACHER Bests were newly searched. Canonical WDL g121 and
NNUE g74 measurements are reused after the exact control gate. Conditions are
depth 4 requested, one thread, private cold 262,144-entry TT per invocation,
mate-distance-only selectivity, full windows, singleton root history, diagnostics
enabled and qshadow disabled. Each position/backend gets one warmup and two
measured searches in its own JVM. Every search has the exact 1,000,000-node bound
and cooperative 10,000-ms bound; its three-search process has a 40-second hard
Python subprocess watchdog.

All repeated non-timing fields checked (nodes, main/q accounting, completed depth,
status, score, move, PV and evaluation calls) match exactly. Every main + q count
equals total nodes, including the reused capped WDL searches. Times below are
search-only milliseconds; model loading, JVM startup and JSON formatting are
excluded. Both new arms complete all 13 nonterminal roots at depth 4; the two
terminal roots are adjudicated at reported depth 1 with zero nodes.

Aggregate counts below are **one full 15-position corpus**, equivalent for either
measured repetition. Timing sum is the sum of position medians; worst is the
largest individual measured latency, not the largest median. WDL's Kiwipete
count is a capped prefix, so its completed depth-4 cost is unknown.

| Arm | Main nodes | Qnodes | Total | Q/total | Q/main | Sum median ms | Worst ms | Completed / terminal / limited |
|---|---|---|---|---|---|---|---|---|
| WDL | 41285 | 1037239 | 1078524 | 0.961721 | 25.1239 | 7541.71 | 6953.79 | 12 / 2 / 1 |
| BLENDED | 44475 | 90719 | 135194 | 0.671028 | 2.03978 | 1241.68 | 826.72 | 13 / 2 / 0 |
| TEACHER | 32971 | 129918 | 162889 | 0.797586 | 3.94037 | 1227.38 | 674.001 | 13 / 2 / 0 |
| NNUE | 34894 | 48723 | 83617 | 0.582693 | 1.39631 | 253.788 | 85.3744 | 13 / 2 / 0 |

Across the **60 new measured searches** (plus 30 warmups), there are **zero node-limit
events, engine wall-clock timeouts, hard watchdog expirations or search failures**.
The reused WDL records have two measured Kiwipete node-limit events plus its warmup;
no reused NNUE invocation has a limit event. Across the 48 new subprocesses
(one replay, 15 static diagnostics, 30 normal-search processes and two qshadow
processes), all exit zero and no watchdog fires.
Kiwipete is the worst measured position for every arm and the NNUE reference;
the individual worst latencies are included in the sentinel table below.

The same Windows 11 / AMD Ryzen 5 5500 host and Java `21+35-2513` were used.
New processes use `-Xms256m -Xmx1536m`; the earlier reused diagnostic processes
used `-Xms256m -Xmx1024m`. This heap-cap difference and separate measurement times
limit precise latency comparisons, while the exact deterministic node counts
are independent of those timing comparisons. No inference kernel or search
implementation changed. Small millisecond differences are not speed rankings.

### Kiwipete sentinel

| Arm | Main | Qnodes | Total | Q/total | Q/main | Median ms | Worst ms | Completed depth | Status |
|---|---|---|---|---|---|---|---|---|---|
| WDL | 7595 | 992405 | 1000000 | 0.992405 | 130.666 | 6859.76 | 6953.79 | 3 | NODE_LIMIT |
| BLENDED | 13866 | 70895 | 84761 | 0.836411 | 5.11287 | 773.734 | 826.72 | 4 | COMPLETED |
| TEACHER | 8645 | 84435 | 93080 | 0.907123 | 9.76692 | 670.949 | 674.001 | 4 | COMPLETED |
| NNUE | 9135 | 32300 | 41435 | 0.779534 | 3.53585 | 82.5055 | 85.3744 | 4 | COMPLETED |

TEACHER completes depth 4 in **90.69% fewer nodes than the WDL capped prefix**,
with **91.49% fewer qnodes**; BLENDED uses **91.52% fewer nodes** and **92.86% fewer
qnodes**. These are conservative comparisons against already consumed WDL work,
not exact reductions versus its unknown completed depth-4 cost. Both alternatives
still exceed NNUE's 41,435 total nodes: TEACHER by 2.25×, BLENDED by 2.05×.
The teacher arm is smoother on the fixed corpus yet expands more qnodes here
than BLENDED, demonstrating that a corpus median alone does not determine tree size.

### Bounded same-edge qsearch volatility

The established qshadow tool ran once for each new final Best, BRN driving and
the exact NNUE g74 shadow observing identical visited boards. Like the canonical
instrumented pass, these runs retain the depth/node bounds and use a separate
30-second cooperative / 45-second hard process bound, no warmup, and symmetry
sampling stride 101 / limit 8,192. No bound fired. Instrumented node counts,
completed depth and status match each respective normal search; trace timing is
excluded from the normal timing comparison.

The table contains actual adjacent-static absolute deltas in existing search
score units, with both endpoints evaluated, from the parent's perspective:

| Driving arm / tree | Evaluator | Edges | Mean | Median | p95 | Max |
|---|---|---|---|---|---|---|
| WDL | BRN | 659077 | 13264 | 11516 | 31584.2 | 58541 |
| WDL | NNUE shadow | 659077 | 6768.64 | 3869 | 22942 | 56495 |
| BLENDED | BRN | 51488 | 9759.02 | 8578 | 22748 | 45838 |
| BLENDED | NNUE shadow | 51488 | 5961.68 | 3565 | 19979.6 | 50161 |
| TEACHER | BRN | 62927 | 10866.6 | 9315 | 26424.7 | 49349 |
| TEACHER | NNUE shadow | 62927 | 7594.36 | 5170 | 22714 | 52443 |

The BRN/NNUE same-edge median ratio is 2.98 for the WDL capped tree, 2.41 for
BLENDED and 1.80 for TEACHER. TEACHER still has appreciably larger tactical tree
deltas than NNUE on its own visited boards. Different arms visit different trees;
these are paired comparisons **within each row's tree**, not comparisons on one
common cross-arm edge population. In particular, TEACHER's larger absolute median
than BLENDED's does not contradict its lower fixed-corpus ratios. Search windows,
move ordering, cutoff geometry and selected positions change with learned values.
No shadow evaluation influences search decisions, and these records do not assign
counterfactual node savings to particular cutoffs.

## Decisions supported by this experiment

1. **Valid ablation:** yes. Exact WDL model/Adam hashes, losses and promotion path
   establish the reproducibility gate without a tolerance waiver.
2. **Teacher-only normalized stability:** materially improved versus control,
   including scale-independent quiet and tactical measures at every trained
   milestone. Remaining tree volatility and nonmonotonic changes limit claims
   of complete stability or convergence.
3. **Teacher agreement:** materially improved. Final normalized Pearson is
   0.86783, Spearman 0.83872 and sign agreement 83.47%, versus -0.09813, -0.01161
   and 56.05% for control. Agreement is with the pinned teacher, not ground truth.
4. **Qsearch/Kiwipete:** materially improved. Both alternative final Bests finish
   the established depth with far fewer qnodes than the WDL incomplete prefix.
   Their remaining cost still exceeds NNUE. This establishes bounded diagnostic
   efficiency, not playing strength.
5. **Blend tradeoff:** BLENDED retains more WDL predictive performance than
   TEACHER and gains substantial search-efficiency improvement, but fails the
   combined proposition of also retaining substantial **fixed-corpus relative
   stability**. It is not an across-the-board compromise between the endpoint arms.
6. **Limitation diagnosis:** changing only supervision and its corresponding
   prescribed Best-selection objective removes the measured severe failure while
   holding architecture, g0, dataset, shuffle and optimizer fixed. This is strong
   evidence that WDL-only supervision is a major causal limitation of this campaign,
   and that BRN-2 can learn a substantially smoother surface with useful bounded
   search behavior from dense targets. It supports supervision as the primary
   explanation among the alternatives tested here; it does not rank every possible
   optimization, data-distribution or representational limitation. This is not the
   outcome “teacher distillation also fails,” nor evidence of BRN-2 incapacity.
7. **No strength inference:** no games or matches were run, and no strength gain
   is inferred from losses, correlation, node counts or timing.
8. **No BRN-3 recommendation:** the teacher arm positively demonstrates learnability
   under the fixed configuration. The evidence does not meet the proposed threshold
   for concluding that BRN-2 cannot learn the smoother teacher surface.
9. **Implementation boundary:** no calibration, inference optimization, architecture,
   schema, score mapping, NNUE behavior, search logic, GUI or default production
   supervision change is part of this unit. Choosing a production objective would
   be a separate authorized work unit.

The intervention includes the requested arm-specific promotion objective; it does
not separately ablate target gradients from checkpoint selection. There is one
initialization, one saved campaign, one pass per batch and no hyperparameter sweep.
Fixed-corpus siblings are correlated, tactical n=23 is small, and the corpus is
synthetic and not game-frequency weighted. No uncertainty intervals from independent
campaign replications are available. None of these limits negates the exact within-
campaign intervention, but they prevent a universal claim that supervision is the
only remaining limitation or that a production change is already validated.

## Durable outputs and reproduction

Experimental exports reside outside all source stores:

```text
E:\SeedV6-Networks\BRN\BRN-2\experimental-supervision-ablation-20260923\
  replay.jsonl
  wdl\       boundary-{0,32,64,96,128}\{network.brn2,training.state,experiment.json}
  blended\   boundary-{0,32,64,96,128}\{network.brn2,training.state,experiment.json}
  teacher\   boundary-{0,32,64,96,128}\{network.brn2,training.state,experiment.json}
  <arm>\latest-training-g128\{network.brn2,training.state}
  <arm>\best.txt
```

The final Best files are `<arm>\boundary-128\network.brn2`; their generations are
121 / 125 / 127. All snapshot payload hashes are independently verified after
writing. No experimental Best is the canonical Best or production reference.

Measurement artifacts follow the existing ignored build-output convention:
`app/build/brn2-supervision-ablation-20260923/`. They include 15 static JSONL files,
30 normal-search JSONL files, two qshadow JSONL files, replay/process logs, exact
command/watchdog receipts, full before/after source fingerprints, `analysis.json`
and derived `tables.md`. The external replay JSONL retains all 384 generation
records at full precision. Large stores and generated artifacts are excluded from
Git; the tables and conclusions here remain available after build cleanup.

From the repository root, with new output names on any repeat:

```powershell
.\gradlew.bat :app:installDist --console=plain
python -B tools/brn2-supervision-ablation.py 'E:\SeedV6-Networks\BRN\BRN-2\training001' 'E:\SeedV6-Networks\NNUE\training\checkpoints\g000074-s000008942-2e22ccbfdeb18f5f26f91f0df183411ac1d01844ccd43e56e9005ee3f5837bc6' 'E:\SeedV6-Networks\BRN\BRN-2\experimental-supervision-ablation-NEW' 'app/build/brn2-supervision-ablation-NEW'
python -B tools/analyze-brn2-supervision.py 'E:\SeedV6-Networks\BRN\BRN-2\experimental-supervision-ablation-NEW' 'app/build/brn2-supervision-ablation-NEW' 'app/build/brn2-canonical-g128'
```

The analyzer requires the retained canonical artifacts for reuse and verifies their
saved hashes; if those have been cleaned, reproduce the established canonical
diagnostics rather than fabricating them from this report. New output paths must
not overlap either input store; existing outputs are rejected. The replay has a
20-minute hard process watchdog and completed all arms in **250.232 seconds** in
this run. It neither opens a source store writer nor invokes recovery. Future source
retention may remove g0 or the teacher payload, in which case replay fails closed.

## Validation, provenance and human actions

Applicable supplied governance, the active `source/CHESS_SEARCH_CONTRACT.md`,
`BRN_BOOTSTRAP.md` and the canonical diagnostic/report conventions were inspected.
This unit does not change the Search canon. Exact root `CODEXLOG_CURRENT.md` and
`VERSION_STATE.txt` are independently absent; neither capability was activated
or created.

Targeted command passed **32 tests in five suites, zero failures/errors/skips**:

```powershell
.\gradlew.bat :app:test --tests '*Brn2SupervisionAblationTest' --tests '*Brn2TrainingTargetsTest' --tests '*HeldOutLossTest' --tests '*BootstrapPartitionTest' --tests '*Brn2DiagnosticsTest' --console=plain
git diff --check
```

The eight new tests cover exact default/explicit WDL and independently ordered
online optimizer bytes, dense-target gradient equivalence, target freezing,
invalid targets before updates, teacher/native-scalar agreement on both sides,
sample preservation, objective-dependent held-out decisions, saved-setting
rejection, output overlap/existing-path rejection, and a failing control hash gate.
Existing tests cover terminal target perspective, cancellation/codec continuation,
whole-game partition semantics, loss promotion and static/search diagnostics.
Development caught an incorrect API accessor at compile time and an opposite-side
test FEN retaining an incompatible en-passant rank; both were corrected before
the successful final targeted run or experimental execution.

Runtime validation adds all 384 generation replays, the exact 128-generation
control gate, original plan/data/history/validation cross-links, 15 milestone
corpus runs, 90 new normal-search invocations and two bounded qshadow passes.
The analyzer verifies artifact and exported-payload hashes, full control static
identity, corpus cardinalities/identity, symmetry residuals, optimizer/sample totals,
repeated search fields, node accounting, qshadow/normal node equivalence, subprocess
success and source integrity. These are actual runtime checks, beyond source review.

Full SHA-256, byte sizes, modification times and file inventories of **all 948
canonical BRN store files and all 401 NNUE store files** match before/after the
experiment. That includes references, g0/g74, all saved batches, and the stopped
generation-129 attempt and plan. Generation 129 remains untouched and unresumed.
No push, deployment, reset, rebase, amend or unrelated cleanup was performed.

Deliberately skipped: `fullCheck`, broad or GUI suites, browser/visual QA (no UI
change), unrelated subsystem tests, new self-play, strength matches, long benchmarks,
extra inference benchmarking, calibration and architecture experiments. The
authorized 128-batch arms are the experiment itself, not routine completion tests.
The documentation-only metric comment changed after the test run; it does not
change executed bytecode behavior.

Focused changed files:

- `BRN_SUPERVISION_ABLATION.md` and its index link in `BRN_DIAGNOSTICS.md`.
- `app/src/main/java/com/ohinteractive/seedv6/tools/search/Brn2SupervisionAblation.java`.
- `app/src/main/java/com/ohinteractive/seedv6/training/checkpoint/CheckpointInspection.java`.
- `app/src/main/java/com/ohinteractive/seedv6/training/selfplay/Brn2SelfPlayTraining.java`.
- `app/src/main/java/com/ohinteractive/seedv6/training/selfplay/SelfPlayTraining.java` (metric documentation only).
- `app/src/main/java/com/ohinteractive/seedv6/training/validation/HeldOutLoss.java`.
- `app/src/test/java/com/ohinteractive/seedv6/tools/search/Brn2SupervisionAblationTest.java`.
- `tools/brn2-supervision-ablation.py` and `tools/analyze-brn2-supervision.py`.

Starting Git state had only inherited untracked `app/bin/`. The focused commit
contains only the files above; generated evidence and checkpoints are excluded.
The final worktree retains inherited `app/bin/` as its sole untracked item; the
commit hash is supplied in the completion response. This report records completed
experimental work, not user acceptance or production/operational adoption.

Human actions required after this prompt: **None**.

## Complete final-arm bounded corpus comparison

WDL and NNUE rows are the verified canonical reuse. All counts are per search,
latency is the median of two measured searches, and undefined ratios for terminal
positions are n/a. The compact table retains every requested position and counter.

| Arm | Position | Main | Qnodes | Total | Q/total | Q/main | Median ms | Depth | Status |
|---|---|---|---|---|---|---|---|---|---|
| WDL | check-evasion | 28 | 2 | 30 | 0.0666667 | 0.0714286 | 2.31135 | 4 | COMPLETED |
| WDL | checkmate-terminal | 0 | 0 | 0 | n/a | n/a | 0.2912 | 1 | TERMINAL |
| WDL | en-passant | 625 | 22 | 647 | 0.0340031 | 0.0352 | 6.52275 | 4 | COMPLETED |
| WDL | middlegame-kiwipete | 7595 | 992405 | 1000000 | 0.992405 | 130.666 | 6859.76 | 3 | NODE_LIMIT |
| WDL | opening-ruy-lopez | 9465 | 5510 | 14975 | 0.367947 | 0.582145 | 144.859 | 4 | COMPLETED |
| WDL | opening-start | 5780 | 333 | 6113 | 0.0544741 | 0.0576125 | 59.3089 | 4 | COMPLETED |
| WDL | promotion-race | 464 | 373 | 837 | 0.445639 | 0.803879 | 8.18465 | 4 | COMPLETED |
| WDL | qsearch-exchanges | 1488 | 882 | 2370 | 0.372152 | 0.592742 | 12.8935 | 4 | COMPLETED |
| WDL | queen-endgame | 1887 | 170 | 2057 | 0.0826446 | 0.0900901 | 7.7086 | 4 | COMPLETED |
| WDL | quiet-endgame | 413 | 45 | 458 | 0.0982533 | 0.108959 | 7.7913 | 4 | COMPLETED |
| WDL | quiet-fianchetto | 11565 | 37459 | 49024 | 0.764095 | 3.239 | 410.279 | 4 | COMPLETED |
| WDL | quiet-pawn | 410 | 0 | 410 | 0 | 0 | 7.059 | 4 | COMPLETED |
| WDL | stalemate-terminal | 0 | 0 | 0 | n/a | n/a | 0.3363 | 1 | TERMINAL |
| WDL | tactical-queen | 217 | 8 | 225 | 0.0355556 | 0.0368664 | 6.4925 | 4 | COMPLETED |
| WDL | transposition-knights | 1348 | 30 | 1378 | 0.0217707 | 0.0222552 | 7.91035 | 4 | COMPLETED |
| BLENDED | check-evasion | 28 | 2 | 30 | 0.0666667 | 0.0714286 | 1.73595 | 4 | COMPLETED |
| BLENDED | checkmate-terminal | 0 | 0 | 0 | n/a | n/a | 0.2067 | 1 | TERMINAL |
| BLENDED | en-passant | 712 | 37 | 749 | 0.0493992 | 0.0519663 | 6.85385 | 4 | COMPLETED |
| BLENDED | middlegame-kiwipete | 13866 | 70895 | 84761 | 0.836411 | 5.11287 | 773.734 | 4 | COMPLETED |
| BLENDED | opening-ruy-lopez | 7242 | 3171 | 10413 | 0.304523 | 0.437862 | 99.4364 | 4 | COMPLETED |
| BLENDED | opening-start | 5937 | 239 | 6176 | 0.0386982 | 0.040256 | 68.8843 | 4 | COMPLETED |
| BLENDED | promotion-race | 390 | 294 | 684 | 0.429825 | 0.753846 | 8.81725 | 4 | COMPLETED |
| BLENDED | qsearch-exchanges | 1269 | 645 | 1914 | 0.336991 | 0.508274 | 13.6999 | 4 | COMPLETED |
| BLENDED | queen-endgame | 1885 | 172 | 2057 | 0.0836169 | 0.0912467 | 8.57915 | 4 | COMPLETED |
| BLENDED | quiet-endgame | 313 | 19 | 332 | 0.0572289 | 0.0607029 | 6.36915 | 4 | COMPLETED |
| BLENDED | quiet-fianchetto | 10849 | 15166 | 26015 | 0.582971 | 1.39792 | 232.192 | 4 | COMPLETED |
| BLENDED | quiet-pawn | 498 | 0 | 498 | 0 | 0 | 5.4935 | 4 | COMPLETED |
| BLENDED | stalemate-terminal | 0 | 0 | 0 | n/a | n/a | 0.2317 | 1 | TERMINAL |
| BLENDED | tactical-queen | 168 | 8 | 176 | 0.0454545 | 0.047619 | 5.3022 | 4 | COMPLETED |
| BLENDED | transposition-knights | 1318 | 71 | 1389 | 0.0511159 | 0.0538695 | 10.1415 | 4 | COMPLETED |
| TEACHER | check-evasion | 28 | 2 | 30 | 0.0666667 | 0.0714286 | 1.99935 | 4 | COMPLETED |
| TEACHER | checkmate-terminal | 0 | 0 | 0 | n/a | n/a | 0.22395 | 1 | TERMINAL |
| TEACHER | en-passant | 304 | 0 | 304 | 0 | 0 | 5.40065 | 4 | COMPLETED |
| TEACHER | middlegame-kiwipete | 8645 | 84435 | 93080 | 0.907123 | 9.76692 | 670.949 | 4 | COMPLETED |
| TEACHER | opening-ruy-lopez | 8412 | 5928 | 14340 | 0.413389 | 0.704708 | 133.52 | 4 | COMPLETED |
| TEACHER | opening-start | 2016 | 253 | 2269 | 0.111503 | 0.125496 | 30.8736 | 4 | COMPLETED |
| TEACHER | promotion-race | 425 | 310 | 735 | 0.421769 | 0.729412 | 8.645 | 4 | COMPLETED |
| TEACHER | qsearch-exchanges | 1242 | 682 | 1924 | 0.35447 | 0.549114 | 9.6679 | 4 | COMPLETED |
| TEACHER | queen-endgame | 1885 | 178 | 2063 | 0.0862821 | 0.0944297 | 9.68005 | 4 | COMPLETED |
| TEACHER | quiet-endgame | 376 | 5 | 381 | 0.0131234 | 0.0132979 | 7.61885 | 4 | COMPLETED |
| TEACHER | quiet-fianchetto | 7784 | 38079 | 45863 | 0.830277 | 4.89196 | 328.555 | 4 | COMPLETED |
| TEACHER | quiet-pawn | 495 | 0 | 495 | 0 | 0 | 7.4139 | 4 | COMPLETED |
| TEACHER | stalemate-terminal | 0 | 0 | 0 | n/a | n/a | 0.19325 | 1 | TERMINAL |
| TEACHER | tactical-queen | 181 | 8 | 189 | 0.042328 | 0.0441989 | 5.25895 | 4 | COMPLETED |
| TEACHER | transposition-knights | 1178 | 38 | 1216 | 0.03125 | 0.0322581 | 7.3786 | 4 | COMPLETED |
| NNUE | check-evasion | 28 | 2 | 30 | 0.0666667 | 0.0714286 | 2.4251 | 4 | COMPLETED |
| NNUE | checkmate-terminal | 0 | 0 | 0 | n/a | n/a | 0.23245 | 1 | TERMINAL |
| NNUE | en-passant | 598 | 40 | 638 | 0.0626959 | 0.0668896 | 13.013 | 4 | COMPLETED |
| NNUE | middlegame-kiwipete | 9135 | 32300 | 41435 | 0.779534 | 3.53585 | 82.5055 | 4 | COMPLETED |
| NNUE | opening-ruy-lopez | 11517 | 10223 | 21740 | 0.470239 | 0.887644 | 56.1925 | 4 | COMPLETED |
| NNUE | opening-start | 2217 | 126 | 2343 | 0.0537772 | 0.0568336 | 8.36655 | 4 | COMPLETED |
| NNUE | promotion-race | 467 | 474 | 941 | 0.503719 | 1.01499 | 10.9465 | 4 | COMPLETED |
| NNUE | qsearch-exchanges | 1778 | 1419 | 3197 | 0.443854 | 0.798088 | 9.31615 | 4 | COMPLETED |
| NNUE | queen-endgame | 1888 | 171 | 2059 | 0.08305 | 0.090572 | 6.27585 | 4 | COMPLETED |
| NNUE | quiet-endgame | 300 | 16 | 316 | 0.0506329 | 0.0533333 | 8.64205 | 4 | COMPLETED |
| NNUE | quiet-fianchetto | 4865 | 3928 | 8793 | 0.446719 | 0.8074 | 24.1588 | 4 | COMPLETED |
| NNUE | quiet-pawn | 576 | 0 | 576 | 0 | 0 | 8.21915 | 4 | COMPLETED |
| NNUE | stalemate-terminal | 0 | 0 | 0 | n/a | n/a | 0.3862 | 1 | TERMINAL |
| NNUE | tactical-queen | 219 | 10 | 229 | 0.0436681 | 0.0456621 | 15.3952 | 4 | COMPLETED |
| NNUE | transposition-knights | 1306 | 14 | 1320 | 0.0106061 | 0.0107198 | 7.7132 | 4 | COMPLETED |
