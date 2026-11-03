# TC-R001 evidence and current outcome

4 November 2026. **Strength objective unachieved; unchanged TC-P1 selected as finalist
for remaining qualification.** Independent confirmation remains unspent.
[CONTRACT](CONTRACT.md) preserves the approved scope and criterion;
[STATE](STATE.md) is the restart point.

## Implemented formulation and source evidence

**TC-P1 — bounded capture verification over HL-G1**, source
`a7c45dffeaffc362bc7c7026d03a2e6434472be2`, build89, explicit `probcut`,
flags1177812991. It checks up to three non-losing legal tacticals at eligible
depth4–8 null-window nodes, with qsearch followed by reduced legal verification
at beta+150. Accepted predictions return a selective beta threshold. Real history,
ordinary/mate score separation, full request accounting and private worker state
remain intact. Probes suppress nested verification and learning/TT side effects.
The normal production selector is unchanged.

[FORMULATIONS](FORMULATIONS.md) distinguishes pinned Stockfish19, Bagatur5.5c and
Ethereal14 implementation findings, authors' historical test claims and Seed
hypotheses. Their ProbCut implementations supply supporting capture order,
thresholds, qsearch/reduced verification and often TT vetoes. Singular extensions
need reliable bound/depth information that Seed's extension-enabled production
path does not normally supply. F3 was not selected; its limited qualification and
adverse W4 evidence remain relevant. External inclusion does not prove Seed gain.

## Technical qualification actually performed

- The expanded selected regression passes288 tests, including four new focused
  tests, with no failures/errors/skips, plus project ownership boundaries. It covers
  observation neutrality and accounting, incremental/full evaluation parity,
  cancellation/failure restoration, node limits, reuse and default isolation.
  Initial and expanded build/test wall73.41s and67.53s are separate from experiments;
  the initial four tests are included in288, not additional distinct tests.
- W1 observation checks pass on nine opening and20 real-history mixed-phase roots,
  with clean repeated controls. Both qsearch/reduced probes and selective cutoffs
  are reached. Aborted/reset invocation counters are incomplete snapshots; full
  request counters own resource accounting.
- W1 UCI isolation44 requests plus2 stops and runtime10 requests plus1 stop pass.
  W4 observation18 requests, isolation8 plus2 stops, runtime10 plus1 stop pass.
  All four private kernels reach the mechanism. W4 is not deterministically
  equivalent to W1 and is not strength-qualified.
- Short W1 cost observations:72 requests including18 warmups,27 measured profile
  pairs; geometric candidate/control wall0.96887, process CPU0.95372, allocation
  0.99967. These changed-work measurements establish neither isolated speed nor Elo.
- Frozen build89 artifact bridge checks2117 JAR entries and29 other files against
  the source-hashed prototype; only generated version metadata differs. Four W1
  preflights pass. Exact84's33 runtime files and Java/CP64 identities were verified.

See STATE's linked manifests and summaries. GUI, native/macOS/mobile, full training
and deployment matrices were not run; none is claimed. Static source inspection
and artifact hashing are not runtime verification.

## First competitive screen: interrupted, no strength result

[V1 protocol](p1-screen-v1-protocol.json) froze32 paired openings/64 games, W1
OWNER/CP64/PVS/ADAPTIVE16/Hash64, UCI10+0.1, against exact guided HL-G1/build84
(`b8ea8b2856eed3f6c774860ca540375054e4de30`, flags104071167). One active game,
symmetric below-normal priority and limited JVM resources preserved desktop headroom
at admission. No W4 game or confirmation game ran.

Later score-blind checks found total CPU68%,92%,97%, with active VS Code Java/Gradle
and other desktop activity. Exact interference onset was not continuously observed.
The owned batch was stopped; unrelated processes were untouched. **63 games were
initiated,62 completed (31 pairs), game63 was interrupted and game64 never started.**
All owned processes drained, confirmed by Job accounting0. Outer elapsed1858.226s
and owned CPU1866.922s include the complete owned execution, not only scored games.

The [limited interruption audit](p1-screen-v1-interruption-audit.json) validates all
62 completed games' legal histories, frozen ordered openings/colors, normal terminal
adjudication and8153 requests' accounting. Their completed trace has zero missing-
score warnings. Frozen inputs remained stable. The full trace has an interrupted
request at its end, and original raw run/process records remain unfinished. This
audit does not relabel the match as passed or establish clock validity.

No WDL, pair estimate or confidence interval was exposed for candidate selection.
No conveniently chosen subset is declared unaffected; all evidence is preserved at
`raw/p1-screen-v1-w1/`, with hashes in the audit. No automatic replay or completion
of the interrupted protocol is authorized by this record. The planned64-game sample
is incomplete, so this is neither a strength result nor evidence against ProbCut.

## Controlled V2 screen and prediction calibration

V2 completed within the one-hour owner window by11:24:05 NZDT on3 November:
**25W/18D/21L,53.125%**, paired-normal95%**43.8557-62.3943%**, bounded95%
29.1169-77.1331%. All64 games,8810 completed requests, full trace and frozen-input
checks pass; owned processes drained. Its score-blind operational review accepted
all games before outcomes were read.967 PC samples averaged16.02% CPU, p95=32.69%,
maximum70.57%, with two brief samples>=50% and none>=75%; minimum13.95GiB free,
no Overwatch. Retain the spikes as a timing limitation, with no exclusions. This
32-pair result is positive but imprecise and exploratory; it proves no strength gain.
See [analysis](p1-screen-v2-analysis.json) and [PC review](p1-screen-v2-operational-review.json).

A research-only helper, separately source/class hashed against frozen build89,
calibrates discarded predictions against the resumed nominal selective traversal.
On9 opening/20 mixed-phase roots,243 completed invocation pairs pass score/PV,
nominal-node and retained-learning parity;15 aborted pairs remain separate.
Of9467 dependent predictions,12 disagree with the nominal null-window outcome:
5/5258 at node depth4,7/2802 at5,0/803 at6,0/511 at7,0/93 at8. Real witness paths
validate. Completed pairs charge1898287 probe nodes. These conditional observations
are neither a population error rate nor exhaustive chess truth; fail-low labels do
not quantify error magnitude. Instrumented/nested costs cannot establish speed.
[Calibration summary](p1-calibration-v1-summary.json) identifies the exact local evidence.

TC-P1 is retained unchanged for wider development. No parameter tuning or finalist
selection follows. A fixed64-pair/128-game support sample, split prospectively into
two32-pair segments, will test whether the positive direction persists before
spending the sole independent confirmation. Segment outcomes stay unread for
selection until both complete and pass operational audit.

## Complete fixed128-game support and selected finalist

The two prospective32-pair segments completed under the new two-hour owner window.
A first admission failed before any engine/game on one36.655% CPU sample; its failed
receipt is preserved. A20s load observation passed, and a fresh output namespace
retained the scientific protocol. A then B passed, with9217+8254=17471 audited
requests, zero missing-score warnings, stable inputs and all processes drained.
No segment scores were inspected until both score-blind operational reviews passed.

[Independent paired analysis](p1-support-v1-analysis.json): **53W/40D/35L over128 games,
57.03125%, paired-normal95%50.6242-63.4383%, Hoeffding95%40.0550-74.0075%.**
Pentanomial[2,13,23,17,9]. A scored53.125%, B60.9375%. The fixed support sample is
exploratory, not an independent claim. Descriptive adaptive pooling with V2 yields
96 pairs/192 games,55.72917%, normal95%50.4726-60.9857%, bounded41.8681-69.5902%;
selection and opening-dependence limitations preclude confirmatory interpretation.
Interrupted clock-unqualified V1 is excluded throughout.

A had three brief CPU samples>=50%, maximum74.73%, mean18.30%, p95 26.97%, minimum
free15.90GiB; none>=75%. B maximum47.46%, mean17.88%, p95 27.02%, minimumfree15.44GiB.
No gaming or guard violation during either match. Both score-blind reviews retained
all games. A's transient source/per-move impact is unknown; passing operational guards
does not prove clock equivalence. All raw samples and failed admission evidence remain.

The unchanged source/configuration is now selected as finalist for W4 risk and, if
required qualification remains adequate, the sole independent W1 confirmation.
The wider positive evidence, prior V2 and calibrated mechanism justify this choice;
no parameter tuning or extra W1 sampling is selected merely to seek favorable results.
No acceptance, promotion or independent claim has occurred.

## Completed W4 risk and independent-confirmation admission

Initial64-game W4 risk:16W/24D/24L,43.75%, normal95%33.67-53.83%. A fixed fresh-root
32-game follow-up:10W/8D/14L,43.75%. All96 games/13707 requests passed operational,
input and owned-drain checks; zero missing-score warnings. Score-blind reviews retained
all games. Initial W4 had one57.81% CPU sample; follow-up maximum41.44%. No guard violation.
The earlier failed W4 PC admission launched no child/game; its failure and settling
observation are preserved and charged separately.

[Adaptive descriptive W4 pool](p1-w4-risk-followup-v1-analysis.json):26W/32D/38L,
43.75%, normal95%34.9359-52.5641%, bounded24.1475-63.3525%. This persistent adverse
point estimate is an unresolved warning, not neutrality or established regression.
No W1 pooling. Prior HL84-vs79 uncertainty/79-80 bridge remains unresolved.

A bounded72-request W4 cost investigation passed27 measured pairs plus18 warmups:
geometric wall1.01834, processCPU0.99887, allocation0.98606; target depth8 completed
8 guided versus6 probcut measured requests. Changed work/scheduling limits inference;
no legality/state/PV/accounting/lifecycle defect was found. It does not explain or
rule out a strength loss. All96 W4 games are spent. Escalate this warning before any
multiworker adoption, which is outside this task. W1-only confirmation remains within
the original authority; W4 superiority/equivalence was not its acceptance gate.

[Independent confirmation](p1-confirmation-v1-protocol.json) is now frozen: unchanged
TC-P1 against exact84, full320 fresh protected pairs/640 games, no interim outcomes,
no tuning/optional stopping/pooling, one attempt. Original score>=53% and paired-normal
lower>50% criterion retained; bounded sensitivity separate. The owner's eight-hour PC
availability through2026-11-03T19:38:59Z covers this work without another scheduling
question. Expected20000s;24000s cap, all original guards retained. At least24060s must
remain before admission. Attempt is spent after the PC gate but before the first child;
any later launch/preflight/runner/audit failure still spends it. No retry claim.

[Ledger](resources.json) at freeze:11128.0127324/64800s charged,53671.9872676s remaining;
351 initiated games (development255, risk96, confirmation0), attempts0/1. All failed and
bounded diagnostic/audit costs included. Live receipt/ledger own later claim status.
Protected inputs and exact runtime/model/Java hashes reverified;13596 W4 and129 cost-PV
positions have zero protected overlap, in addition to all earlier checks. No protected
moves/keys exposed for selection. Historical failed selector evidence is preserved.

The claim-admission supervisor passed15 rejection cases and isolated stale/duplicate
ledger tests, plus native sampling/owned-dummy drain. Historical source/receipts remain
preserved. No application runtime changes; no application build bump or repeated full
application tests. Actual independent result remains unknown.

## Repository and authority

The mandatory CH-R001 synchronization is standalone documentation commit
`8e95d23d40afab5567998054fbc6e94006064051`, P4-R008/F024, with no build change.
TC bootstrap is `3cc7c9d3`; implementation/build89 is `a7c45dff`; artifact/protocol
freeze `585b4882`, protected inputs `b3a7d586`, W4 evidence `79bb6e8a`, historical
input review/W1 admission `82478171`. Interruption/guard documentation is4702d33d and V2 window admission3bbbb1bb.
Later V2 outcome/calibration/support records are identified by Git history. All work is local; no push, promotion, deployment,
release, production-default change or successor. Build88→89 was verified once with
the runtime commit; later harness/docs work has no application build impact.

Champion remains exact HL-G1/build84; normal build85 HL-G1 policy is unchanged.
TT-R001 reconciliation remains separate and both frozen BOUNDARY2 studies unopened.
Current masters are **P4-R014/P4-F030**. Repository edits do not refresh owner mirrors.

Human actions required after this prompt: **None for current execution inside the
granted eight hours. Blocking before multiworker adoption: owner review of the
persistent adverse W4 evidence. Blocking before mirror-dependent planning, non-blocking
for repository work: refresh Phase4 mirrors to P4-R014/P4-F030.** No acceptance,
promotion or deployment is requested. Routine monitoring stays internal per STATE.
