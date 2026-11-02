# TC-R001 evidence and current outcome

3 November 2026. **Strength objective unachieved; active workstream awaiting a
controlled PC-use window.** No finalist or independent claim attempt selected.
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

## Resources, failures and next experiment

[Ledger](resources.json): **4062.6699935/64,800 charged seconds** (1h7m42.67s),
remaining60737.3300065s. Initiated games127/1120; W1 development127/384,
W4 risk0/96, confirmation0/640; claim attempts0/1. Failures and the post-stop audit
are charged once. The original selector-supervisor drain assertion and W4-only
harness assertion remain failed evidence; independent input validation and fresh
corrected W4 checks do not erase them. The first system-Python import failed before
an engine ran. Use the existing research venv.

Protected confirmation320 roots remain unused. Six historical non-key fixtures
cover5364 distinct prefixes with zero overlap; all8094 real positions exposed by
the interrupted screen, including game63, also have zero overlap. No protected
moves/keys were displayed for selection. Recheck later development disjointness.

[Support protocol](p1-support-v1-protocol.json) freezes the unchanged candidate
against exact84 on64 new-to-TC starting roots, in two32-pair segments. It needs one
new controlled two-hour window or two separately granted one-hour windows. The
completed V2 grant does not admit support matches. Retain all samples/interruption
records and analyze the full128 games only after both segments pass. All candidate
and protected-confirmation identities remain unchanged. Full completion leaves129
development games,96 W4 games and all640 independent games.

The bounded PC guard's native sampling, owned dummy termination and expired-window
rejection checks pass; V2 now also qualifies its actual match path. It does not prove
clock equivalence. W4 competitive risk and independent confirmation remain unperformed.
Further source work and bounded light diagnostics stay authorized; the next meaningful
selection decision depends on the prepared wider competitive sample.

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
Current masters are **P4-R012/P4-F028**. Repository edits do not refresh owner mirrors.

Human actions required after this prompt: **Blocking for the next clocked batch:
provide a new controlled two-hour window for support, or separate one-hour segment windows. Blocking before mirror-dependent
planning, non-blocking for repository execution: refresh both Phase4 Project Source
mirrors to P4-R012/P4-F028.** No strength acceptance or promotion is requested.
