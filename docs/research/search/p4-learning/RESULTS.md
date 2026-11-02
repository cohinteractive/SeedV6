# HL-R001 research handback — 2 November 2026

**HL-G1 met the prospectively frozen W1 champion-relative strength criterion.**
The independent 160-pair comparison against build79 scored136 wins,89 draws and95
losses:56.40625%. The paired-normal one-sided97.5% lower bound is51.8239%, above
equality, and the observed score exceeds the predeclared53% practical threshold.
Mandatory W4 risk investigation is complete, with an adverse/inconclusive direct
champion result that remains unexplained. This supports a **W1-only research result**
under the stated approximate inference. GPT/owner reconciliation and acceptance
are pending. Champion79 and production defaults have not changed.

## Formulations and reproducible identity

Both experimental profiles are committed at
`b8ea8b2856eed3f6c774860ca540375054e4de30`, build84, and preserved in
`raw/runtime-build84-v1` with `runtime-build84-v1-manifest.json`.
Core/UCI/research-launcher sources remain unchanged since that freeze.

| Formulation | Mechanism and intended benefit | Flags | Disposition |
| --- | --- | ---: | --- |
| HL-R1 — request-scoped private learning | Retain main/capture/immediate-continuation histories and killer/countermove guidance across iterations, retries and W4 root jobs; preserve each worker's own learning instead of repeated owner seeding. Clear at request boundaries, abortion and new game; always reset transient scratch. |36962303|Retention-only reverse ablation; adverse/inconclusive W1 screen, not selected.|
| HL-G1 — retained learning with history-guided effort | Adds bounded grading of existing eligible late quiet reductions from main history plus half immediate continuation. Uses the measured SeedV6 scale, preserves tactical exclusions and verification, and keeps deeper-futility eligibility consistent with the reduction. |104071167|Selected integrated formulation; independent W1 criterion passed. W4 strength/neutrality unqualified.|

The relevant technical control is corrected80, source
`0bfb4ea8422ef78443e309930166ad46df5cc454`, frozen artifact built at
`9fec1d2af379827d105d523c32162fd771905cb9`. Champion79 remains
`753367614b8aa7e32dee337fd76b542737141ac1`. Both use flags3407871.
Build80 is not assumed competitively neutral to79. H1/H2/TTF-F3 are not enabled;
their historical conclusions and separate acceptance/reconciliation status remain.

Common competitive settings: CP64 SHA256
`8431626c2fca81e0a1e9ccd5e45ddd8829e6e3282c756c21fad75b06d075bb2b`,
S6CNU002/eight material heads/BT4_Q_LOGIT400_V1, flattened PVS/ADAPTIVE16,
Hash64 (2,097,152 entries;50,331,648 payload bytes), unchanged BOUNDARY2,
10+0.1, one concurrent game, no book/tablebase/adjudication. W1 is OWNER;
W4 is ROOT_REUSE. Windows/Ryzen5500, Java21+35, Xms256m/Xmx512m and
ActiveProcessorCount4 were fixed. Manifests and each run record exact commands.

Pinned Stockfish19, Ethereal14 and Bagatur5.5c implementations informed the lifetime,
representation and effort hypotheses. [EXTERNAL](EXTERNAL.md) distinguishes actual
source behavior, author-reported development evidence and SeedV6 transfer hypotheses.
No external overall strength or copied constant is treated as evidence for this gain.

## Competitive evidence

All results are candidate-centric. Intervals below are descriptive paired-normal95
score intervals; only the final row is the independent predeclared primary endpoint.

| Comparison | Domain | Pairs | W / D / L | Score | Paired95 interval |
| --- | --- | ---: | --- | ---: | --- |
| G1 vs corrected80, development |W1|32|30 /18 /16|60.94%|52.15–69.72%|
| R1 vs corrected80, reverse ablation |W1|32|19 /20 /25|45.31%|35.61–55.01%|
| G1 vs corrected80, risk screen |W4|32|32 /13 /19|60.16%|49.19–71.13%|
| G1 vs champion79, risk bridge |W4|32|19 /21 /24|46.09%|36.48–55.71%|
| **G1 vs champion79, independent confirmation** |**W1**|**160**|**136 /89 /95**|**56.41%**|**51.82–60.99%**|

The reverse ablation supports retaining the integrated effort mechanism; it does
not independently prove an isolated component gain. Development inputs were
explicitly reused for exploration. No games were pooled across formulations,
controls, worker domains or historical campaigns.

The decisive [protocol](g1-confirm-v1-protocol.json) was frozen in commit6b48136b
before confirmation games. One finalist, one W1 attempt and one-sided nominal
alpha.025 were allocated prospectively. The sample and criterion were not extended
or relaxed. The [frozen analysis](g1-confirm-v1-champion79-w1-analysis.json) reports
the actual pair variance, pentanomial12/29/56/32/31 and primary pass.
The logistic point estimate is+44.76 Elo, with approximate paired-normal95 interval
+12.68 to+77.62 Elo in this test domain.

**Statistical limitation:** the conservative bounded lower score bound is45.67%,
so it does not establish superiority. Normal inference is approximate under the
paired-sampling model; this result is not an exact finite-sample guarantee or proof
of a true minimum three-point gain. The observed gain is6.41 score points.

Confirmation used160 unique legal16-ply histories sampled from1,215 eligible roots
after excluding56,553 source/historical-receipt keys. No exposed confirmation root,
candidate evaluation filter or outcome filter was used. The two frozen BOUNDARY2
studies were not opened. [Input preparation](CONFIRMATION-DESIGN.md) and its receipts
preserve the selection, exclusions, six non-key metadata reviews and limitations.
The curated subset and shared opening families limit generalization. Other clocks,
platforms, arbitrary positions and multiworker strength are not established.

## W4 risk and technical qualification

The direct79 W4 point estimate is adverse but its interval includes equality.
The positive80 result does not establish neutrality against79, and the controls
must not be pooled. No particular mechanism has been established as the cause.
The corrected-accounting foundation and private-worker scheduling/lifetime remain
possible interaction questions, not demonstrated explanations.

Both W4 batches passed full game/request audits, stable-input checks and owned
process/engine-handle draining, with no missing-score warnings. A follow-up selected
seven real-history prefixes from the first two games by schedule, without outcome
filtering. All126 bounded requests, including warmups and clean W1 observation
checks, passed. History-guided effort was exercised in every private W4 worker.
The clean seven-root G1/control wall ratio was1.0355 with root variability, compared
with0.9921 in the earlier corpus. Neither small measurement explains the competitive
result or supports a general speed claim. W4 strength/neutrality remains unqualified.

Other completed qualification:

- 330 selected core/UCI/research tests passed, zero failures/errors/skips, including
  seven new learning-lifetime, effort, cancellation/accounting and isolation tests.
  The earlier16-test run overlaps these; it is not16 additional distinct tests.
- 65 UCI requests plus four stop/reset checks passed. Current production and explicit
  research-control W1 matched frozen80; candidate W4 checks preserved legal PV,
  bounded accounting and lifecycle behavior without claiming deterministic equality.
- Instrumented W1 requests matched clean identical-profile work/completion/score/PV.
  Diagnostics established nonzero learning consumption and actual up/down reductions.
  W4 final-job counters are not whole-request totals.
- 162 initial clean-cost requests including54 warmups completed. G1/control wall
  ratios were1.0153 W1 and0.9921 W4; no speed claim follows.
- 43 prospective stratified independent recursive TT-off reference samples completed.
  One negative-history sample crossed its threshold only at nominal depth. This is
  a horizon-disagreement witness, not an identified flat-kernel correctness defect
  or population error rate. Exact and unpruned references remain unchanged.
- The committed candidate matched2,132 tested artifact entries/files except generated
  version metadata; candidate preflights and module-boundary verification passed.
- All576 tournament games ended normally.80,043 full request audits passed, all
  frozen inputs stayed stable, and all owned engine lifetimes drained. Confirmation
  alone contributed320 games and44,104 audited requests, with zero missing-score warnings.

Full unrelated GUI/training/native-platform validation was not run. Static source
inspection is not substituted for the runtime checks above, and selective Search
is not claimed universally equivalent to exhaustive finite-horizon references.

## Resources, provenance and next action

The campaign used128/128 exploratory pairs and160/160 independent confirmation
pairs in one attempt. Charged game wall was17,611.69 seconds (**4h53m32s**), within
the six-hour ceiling. Confirmation took2h42m16s. Aggregate engine CPU was6h05m32s;
active build/diagnostic/input execution was approximately327 seconds, within its
two-hour ceiling. [qualification-v1-summary.json](qualification-v1-summary.json)
binds each run, audit, resource record and the version check.

Source and evidence are committed on main from the verified clean starting
ada8032a. The qualifying implementation increased build83 to84 exactly once in
its own commit; final analysis/documentation does not warrant another bump.
Version bytes remain0.0.0/build84, identical to that implementation commit.
Root CODEXLOG_CURRENT.md is absent, so no journal was created. No push, release,
deployment, champion advancement or default activation occurred.

Raw outputs remain ignored/local at the recorded paths, including adverse results.
Frozen controls/candidate, protocols, manifests, input fixtures and summaries are
preserved. A fresh checkout must check local evidence availability. The initial
baseline diagnostic installation was mutable and not retained; its manifest
describes the executed artifact. Later candidate/control installations are frozen.

The next action is **GPT/owner reconciliation before acceptance or dependent
progression**. The repository Frontier is now P4-F019 with Contract P4-R003 unchanged;
the owner must refresh its Project mirror before mirror-dependent planning.
Research execution is complete, not owner acceptance or operational deployment.

Deferred opportunities remain: explaining W4 risk with matched scheduling/accounting
and lifetime diagnostics; learning-label quality/normalization; richer continuation
contexts; and bounded cross-request decay. The latter must explicitly address abort
clearing without inventing completed-iteration evidence. F3 integration still needs
a concrete dependency and its adverse evidence preserved. None is an automatic
successor or permission to spend another confirmation allowance.
