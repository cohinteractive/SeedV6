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

## Resources, failures and next experiment

[Ledger](resources.json): **1990.1967523/64,800 charged seconds** (33m10.2s),
remaining62809.8032477s. Initiated games63/1120; W1 development63/384,
W4 risk0/96, confirmation0/640; claim attempts0/1. Failures and the post-stop audit
are charged once. The original selector-supervisor drain assertion and W4-only
harness assertion remain failed evidence; independent input validation and fresh
corrected W4 checks do not erase them. The first system-Python import failed before
an engine ran. Use the existing research venv.

Protected confirmation320 roots remain unused. Six historical non-key fixtures
cover5364 distinct prefixes with zero overlap; all8094 real positions exposed by
the interrupted screen, including game63, also have zero overlap. No protected
moves/keys were displayed for selection. Recheck later development disjointness.

[V2 protocol](p1-screen-v2-protocol.json) is frozen for the unchanged TC-P1 against
exact84 on32 different starting roots from existing HL exploratory material. It
uses a controlled60-minute owner window, expected35–40 minutes, a3300s runner cap
and a bounded CPU/memory/Overwatch guard. Native sampling and owned dummy termination
were checked; an expired-window test rejects launch before any child process.
This is not an end-to-end competitive qualification of that new guard.
It must retain all samples and interruptions, and the complete batch still requires
operational review. No V1 outcome informed its candidate or input selection.

W4 competitive risk and independent confirmation remain unperformed. A controlled
window is now required for subsequent W1 matches as well; source work and bounded
light checks remain authorized. More candidate implementation before this pending
screen is not presently justified by evidence.

## Repository and authority

The mandatory CH-R001 synchronization is standalone documentation commit
`8e95d23d40afab5567998054fbc6e94006064051`, P4-R008/F024, with no build change.
TC bootstrap is `3cc7c9d3`; implementation/build89 is `a7c45dff`; artifact/protocol
freeze `585b4882`, protected inputs `b3a7d586`, W4 evidence `79bb6e8a`, historical
input review/W1 admission `82478171`. Later interruption/guard documentation is
identified by Git history. All work is local; no push, promotion, deployment,
release, production-default change or successor. Build88→89 was verified once with
the runtime commit; later harness/docs work has no application build impact.

Champion remains exact HL-G1/build84; normal build85 HL-G1 policy is unchanged.
TT-R001 reconciliation remains separate and both frozen BOUNDARY2 studies unopened.
Current masters are **P4-R011/P4-F027**. Repository edits do not refresh owner mirrors.

Human actions required after this prompt: **Blocking for the next clocked batch:
provide a controlled60-minute window for V2. Blocking before mirror-dependent
planning, non-blocking for repository execution: refresh both Phase4 Project Source
mirrors to P4-R011/P4-F027.** No strength acceptance or promotion is requested.
