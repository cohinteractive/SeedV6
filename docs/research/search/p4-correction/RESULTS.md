# CH-R001 results - correction history and evaluation-guided selectivity

> Subsequent owner disposition, 3 November 2026: technical completion and the
> unsuccessful strength outcome are owner-accepted. CH-C3/build88 remains retained
> experimental evidence; the single confirmation attempt is spent. No continuation,
> champion advancement, production promotion, deployment or additional research
> authority followed from acceptance. The original technical report below retains
> its then-pending reconciliation and mirror statements as dated history, not current
> gates. [STATE](STATE.md) records the synchronized P4-R008/P4-F024 disposition and
> current mirror dependency. Results, thresholds and resource allowances are unchanged.

3 November 2026 (Pacific/Auckland). **Authorized bounded execution is complete;
the governing strength objective is unmet.** GPT/owner reconciliation remains
pending. The sole independent confirmation failed both prospectively frozen
strength conditions. Technical qualification is useful retained work, but this
strength-development objective is not relabelled a successful foundation.

[CONTRACT](CONTRACT.md) preserves the original objective and acceptance rules;
[STATE](STATE.md) is the terminal resume point. The accepted HL-G1/build84 champion
and normal HL-G1 policy integrated in build85 remain unchanged. Repository build88
contains opt-in experiments; it does not designate a new production policy or
deployed artifact. No push, release, deployment or successor was performed.

## Independent result and exact selected configuration

**CH-C3 - decayed completed pawn correction across requests, with main-search and
qsearch consumers** was selected from development. Source
`d530b28aded2b4ed6a6f3cc1c15d0a8373b1dbb9`, build88, profile `corrected-retained`,
flags1043595263. It extends HL-G1 with a bounded worker-private pawn-context estimate
of searched-minus-raw evaluation, used in selective main decisions and qsearch
stand-pat. Completed managed invocations save feedback; abort restores that snapshot;
new requests decay it by7/8; new-game and runtime errors forget it. Other HL histories
remain request-scoped. Feedback is selective and fallible, not independent truth.

The exact opponent was frozen **HL-G1/build84**, source
`b8ea8b2856eed3f6c774860ca540375054e4de30`, profile `guided`, flags104071167.
Build85 was the implementation foundation, not a substituted comparator. The
[candidate manifest](runtime-build88-c3-v1-manifest.json) and
[opponent manifest](../p4-learning/runtime-build84-v1-manifest.json) pin all runtime
files. Local installations are `raw/runtime-build88-c3-v1` and
`../p4-learning/raw/runtime-build84-v1` respectively.

The frozen [confirmation protocol](c3-confirm-v1-protocol.json) was committed in
`9ebc3df4`, with post-W4 [admission](c3-confirm-v1-admission.json) in `aa03b93c`.
No candidate, criterion or stopping rule changed after outcomes. The complete
prospective analysis ran once after all games, audits and process draining.

| Independent W1 measure | Result |
| --- | ---: |
| Fresh opening pairs / normal games | 160 / 320 |
| Candidate wins / draws / losses | 100 / 87 / 133 |
| Candidate score | 44.84375% |
| Pair-score standard error | 2.37809 percentage points |
| Paired-normal two-sided95% interval | 40.18278-49.50472% |
| Conservative bounded95% interval | 34.10701-55.58049% |
| Primary score>=53% AND normal lower>50% | **FAILED both conditions** |
| Audited requests / qualified warnings | 44,291 / 0 |

The normal interval is below equality, an adverse result under the declared normal
approximation. The conservative bounded interval crosses equality; no universal
regression or correction-history rejection follows. This is not merely a positive
result that lacked precision. All execution/audit gates passed, but those gates do
not establish strength. Machine-readable evidence:
[prospective analysis](c3-confirm-v1-champion84-w1-analysis.json) and
[execution summary](c3-confirm-v1-champion84-w1-summary.json).

Primary domain: Windows11, Ryzen5 5500 (6C/12T,32GiB), Java21+35, conventional CP64,
flattened PVS/ADAPTIVE16, W1 OWNER, Hash64 (48MiB TT-entry payload), BOUNDARY2,
UCI10+0.1, one active paired game. CP64 SHA256 is
`8431626c2fca81e0a1e9ccd5e45ddd8829e6e3282c756c21fad75b06d075bb2b`.
Java/runtime/runner identities and resource settings are in the manifests/protocol.
No arbitrary-position, worker, clock or platform generalization is established.

The fresh [160-opening input](confirmation-v1-openings.pgn),
[selection receipt](confirmation-v1-openings.selection.json) and
[input protocol](confirmation-input-v1-protocol.json) exclude the entire prior
legal16 pool, prior eligible receipts including HL confirmation, and all60 actual
mixed-phase sequence roots. Selection seed2026110306 yielded160 of1055 eligible
held-out unique roots; independent reparsing verified legal16-ply nonterminal
histories, receipt hashes and no root overlap. Match seed2026110307 was frozen.
No partial scores informed decisions, no exploration/history was pooled, and no
finite-population precision adjustment was used. Root disjointness does not remove
opening-family dependence or curated-source/exclusion distribution limitations.

## Development path and secondary risk

All screens used the same32 development pairs against exact84, not independent
confirmations. Differences between these selected small screens are descriptive;
they do not isolate component effects. [FORMULATIONS](FORMULATIONS.md) records
mechanisms, pinned external source facts, reported external tests and Seed hypotheses.

| Formulation and source | Difference from comparator / preceding candidate | W-D-L | Score | Paired-normal95% |
| --- | --- | ---: | ---: | ---: |
| CH-C1, build86, `d51d1479`, `corrected`, flags238288895 | HL-G1 plus request-private pawn correction for main selective evaluation/improving consumers | 22-16-26 | 46.875% | 35.08-58.67% |
| CH-C2, build87, `84c95128`, `corrected-q`, flags506724351 | C1 plus corrected ordinary qsearch stand-pat; no qsearch training | 22-13-29 | 44.53125% | 33.37-55.69% |
| CH-C3, build88, `d530b28a`, `corrected-retained`, flags1043595263 | C2 plus decayed completed-invocation feedback across requests, abort rollback and explicit reset | 27-19-18 | 57.03125% | 47.90-66.16% |
| C3 W4 ROOT_REUSE risk, same build/profile | Secondary domain versus exact84 W4 | 22-19-23 | 49.21875% | 39.02-59.42% |

C1's main/qsearch asymmetry justified testing C2. Both were adverse/inconclusive.
Verified Stockfish lifetime differences and Seed's abort/reset behavior then
justified C3's retention design. C3's positive but inconclusive exploratory result
warranted finalist selection, subject to phase/risk qualification and independent
confirmation; it did not prove retention improved strength. No blind parameter
sweep or fourth formulation was selected.

W4 completed32 pairs/64 normal games and8,947 audited requests. Its conservative
bounded95% interval is25.21-73.23%; no superiority, neutrality or regression is
established. The separate historical HL84-versus79 W4 result (46.09%, normal95%
36.48-55.71%) remains adverse/inconclusive. Build79 and accounting-corrected80
retain distinct identities; their competitive bridge, especially W4, remains unknown.
TTF-F3 was not adopted; TT-R001 reconciliation remains separate. Neither frozen
BOUNDARY2 study was opened, replayed or reinterpreted.

## Technical accomplishment, validation and limits

C1/C2 use2x16384 signed shorts (64KiB/worker), hashed pawn occupancy plus side,
four fractional bits, cap256 native CP64 units, depth-weighted EMA
min(depth,8)/32 of a clipped searched-minus-RAW residual. Eligible completed quiet
interior depth>=2 outcomes respect directional bounds; root/check/tactical,
null/verifier, repetition/rule50 and subtree rule outcomes do not train. Raw and
working values remain separate. Corrected qsearch follows terminal/rule handling
and marks selective provenance; null/verifier consumers remain raw. Independent
exact main/qsearch references and clock policy were preserved.

C3 adds64KiB saved feedback per worker (128KiB correction payload total), with about
five requests' decay half-life. A saved unit is a completed managed invocation,
including a completed ROOT_REUSE worker job, not necessarily an entire driver
iteration. Standalone unmanaged ExactSearch does not retain. New-game/error cleanup
drains before clearing private snapshots. It does not change other HL learning
lifetimes, share mutable correction tables or adopt a broad parallel/TT redesign.

Qualification records are preserved with failed/negative intermediate logs:

- [C1 tests](c1-tests-v1-summary.json):215 selected JUnit tests passed after fixing
  a test-only cumulative-null-counter assumption. Search/rules/UCI, Construction,
  exact references, TT/null/accounting, drivers/parallel and dependency boundaries
  were covered. The runner's six supervision/audit tests also passed.
- [C2 tests](c2-tests-v1-summary.json):59 selected tests passed. An optional C1
  deeper-futility trace had passed working evaluation under a raw argument; C2
  repaired it and added a constant-evaluator regression. Game observers were off
  and old diagnostic counters ignored that argument, so C1 games were unaffected.
  All nine frozen C1 W1 diagnostic outcomes reproduced.
- [C3 tests](c3-tests-v1-summary.json):62 selected tests passed; after observation
  additions, [12 focused repeats/build](c3-tests-v3-summary.json) passed. A missing
  Move import initially broke the new diagnostic compilation and was fixed before
  execution; the failed log remains. These overlapping suites are not additive
  counts of distinct test cases.
- [C3 mechanism diagnostics](c3-diagnostic-v1-summary.json):54 main and108 sequence
  observed requests plus81 clean W1 repeats passed state/history/PV/accounting,
  retention, abort, reset and observation-neutrality checks. All nine current C2
  W1 outcomes reproduced. C3's first sequence requests matched C2;14/18 later
  W1 outcomes changed. All18 later requests read retained feedback,12 changed
  first evaluation;249 completed snapshots and25 abort restores were observed.
  W4 exercised private workers, with69 retained requests,23 changed first reads,
  5200 completed snapshots and79 restores. W4 schedule equality was not claimed.
- [Mixed-phase qualification](mixedphase-v1-summary.json):the inherited nine-root
  corpus was opening-only, so a prospective unfiltered phase sample from completed
  C1 histories added20 roots (four unavailable-prefix skips), including11 with
  at most six nonpawn/nonking pieces. Each domain ran120 observed requests, with
  120 clean W1 repeats. All invariants passed. In W1,39/40 later C3 outcomes and
  37/40 first retained evaluations changed. Both C2/C3 had25 near-cap updates
  localized to one root, max255 within256; W4 C3 max245, no near-cap updates.
  Eight sequence pairs shared pawn context despite nonpawn-material changes;
  harmful generalization was not established. The TSV is a tracked input.
- [C3 isolation](c3-isolation-v1-summary.json):78 UCI requests and eight stop/reset
  checks passed; exact84/default/guided W1 controls matched. [Runtime checks](c3-runtime-v1-summary.json)
  passed20 requests and two stops, including clocks/movetime, repetition/mate,
  stale-result/rule50, fresh/Clear Hash equality and repeated-request reuse.
- [Committed artifact qualification](build88-c3-qualification.json):2115 JAR entries
  matched the qualified prototype except generated version metadata; all27
  non-JAR files matched; four committed preflights passed. Prior builds86/87 have
  corresponding frozen manifests/bridges. All match inputs stayed hash-stable;
  every match finished normally, passed audits and drained owned engine handles.

Short clean costs do not establish speed or strength: C3/guided geometric-mean
wall ratios were1.00527 W1/1.00582 W4 over486 requests including162 warmups
([cost summary](c3-cost-v1-summary.json),67.20s execution). C1 ratios were1.0041/
1.0313; C2 earlier ratios0.9681/1.0332. Work and scheduling differ, and these small
measurements do not isolate instruction overhead. Snapshot copying/aging, pawn
aliases, selective label bias and self-reinforcement remain costs/risks.

Full GUI/native/macOS/mobile/training/platform-matrix and deployment verification
were not run. Qualification covers the selected repository tests and bounded
Windows W1/W4 research domains; static inspection is not runtime verification.
No claim is made about untested supported platforms or W2-W16 strength.

## Resource accounting and evidence custody

[Closeout summary](closeout-v1-summary.json) independently aggregates the five
complete match summaries, including setup/audit wall time, without pooling strength
estimates. All576 games finished normally;80,438 request audits passed with zero
qualified warnings. There were no failed/interrupted competitive games to exclude.

| Category | Used pairs / games | Match wall seconds |
| --- | ---: | ---: |
| C1 W1 exploration | 32 / 64 | 1951.7385866 |
| C2 W1 exploration | 32 / 64 | 2037.1115854 |
| C3 W1 exploration | 32 / 64 | 1983.2785154 |
| C3 W4 risk | 32 / 64 | 1946.2667904 |
| C3 independent W1 confirmation | 160 / 320 | 9772.9762377 |
| Total | 288 / 576 | **17691.3717155 (4h54m51s)** |

W1 exploration used96/128 pairs; W4 used32/32; confirmation used160/160 and the
single attempt. Of640 games and43200 seconds,64 exploratory games and25508.63
seconds remain unused. These are ceilings, not obligations or a second confirmation
authorization. More selected development cannot establish a new independent primary
claim under the spent one-attempt design. No responsible autonomous continuation
capable of meeting that objective remains within current authority.

Technical work was separate, not concealed competitive play. Individual
`*-execution.json` records meter observation/cost/mixed-phase/input parsing;
test summaries/logs record build/test durations (C2 approximately51s; C3 53s plus
6s focused build). For example mixed-phase execution took34.63s W1/12.45s W4 and
fresh input parsing42.33s. These are measured components, not an exact total of
all authoring, inspection, compilation, tests or elapsed turn time. Do not add a
summary's duration to the same underlying execution records twice.

Raw local evidence remains under:
`raw/c1-screen-v1-w1`, `raw/c2-screen-v1-w1`, `raw/c3-screen-v1-w1`,
`raw/c3-risk-v1-w4`, `raw/c3-confirm-v1-champion84-w1`, and technical paths named
in their tracked summaries/manifests. PGNs, traces, JSONL, logs, failed tests and
frozen runtime copies remain ignored; no force-add or evidence deletion occurred.
A fresh checkout must verify local availability before replay/analysis. Closeout
verification rechecked all33 build84 and35 files per candidate runtime, nine C3
source files, protocol/run/PGN/analysis hashes, resource and pair-statistic arithmetic,
86 local Markdown links, unchanged strict version bytes, output ignore rules and
absence of owned processes. No runtime source changed after C3 freeze; no additional
runtime suite was needed for this documentation/analysis-only closeout.
Confirmation run/PGN SHA256 are respectively
`27187e522c66a5093bc1661f62fc801b316a593a8fcfaf1bbca7da976b250271` and
`04d99b939e660560b69549948c975f7474451621e2cfd1a89cc0342b347b0cf3`.

## Interpretation, disposition and human dependencies

The selected small-screen improvement did not generalize to the independent
distribution. Sampling/selection noise, opening-distribution shift, broad pawn
aliases, stale feedback and selective/self-referential labels are plausible
explanations, not diagnosed causes. Mixed-phase saturation was localized and
shared with C2; it does not establish why confirmation failed. External engines'
more varied contexts and integration do not prove an alternative would win here.

Recommended disposition: reconcile CH-R001 as an unsuccessful bounded
strength-development execution, preserve opt-in code and all adverse evidence,
and keep the accepted champion/default policy. A future authorized investigation
could first audit context/label reliability or test a material/continuation-aware
integrated formulation. It would need new technical qualification and a separately
approved independent design. At observed throughput, another160-pair confirmation
alone is roughly2.7h match wall, plus development/qualification; no gain or adequate
power for small gains is promised. No such continuation is selected or launched.

Implementation commits `d51d1479`, `84c95128`, `d530b28a` accompany exact build
increments85->86->87->88. Protocol, artifact and evidence commits preserve their
prospective ordering. This closeout changes reports/canon only and leaves every
version byte untouched. Initial clean `main` was `296c0fd1`; no inherited work was
absorbed. The final closeout commit identifies this report and terminal state;
all commits remain local. Root journal was absent and was not created; no finalizer,
shared-governance or CEC changes were made.

Current masters are [P4-R007](../../../../source/CHESS_SEARCH_CONTRACT.md) and
[P4-F023](../../../../source/CHESS_SEARCH_RESEARCH_FRONTIER.md). Owner-confirmed
Project mirrors remain P4-R005/P4-F021 until the owner refreshes them; repository
edits do not synchronize Project Sources. CH-R001 execution completion is distinct
from GPT/owner acceptance, champion advancement, production promotion or deployment.

Human actions required after this prompt:

- **Blocking for acceptance, further strength research or promotion:** GPT/owner
  reconciliation of this failed criterion and explicit authority for any new
  objective, confirmation or promotion. No human action is needed to finish the
  completed repository execution.
- **Blocking before mirror-dependent Project planning; non-blocking for completed
  repository execution:** refresh both Phase4 Project Sources to P4-R007/P4-F023.
