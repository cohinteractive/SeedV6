# Prospective ADAPTIVE16 confirmation and resource requalification

Frozen before any games in these new studies. Subordinate to Contract P3-R017 and
the active TT-effectiveness CGLHW. No production acceptance or promotion authority.

## Question and candidate freeze

Does the existing unmodified ADAPTIVE16 improve clock playing strength over
owner-selected WIDEN16, with all18 Construction V1 controls, CP64 and Hash64?
Both use clock-runtime-v2; its prior binding and qualification remain intact.
CP64 SHA256 is8431626c2fca81e0a1e9ccd5e45ddd8829e6e3282c756c21fad75b06d075bb2b.
No retuning, new widths, production source change or TT policy variant is allowed.

W1 passed the prior32-pair development score gate, with background-load limits.
W4 passed the score-only gate but had substantial unplanned host load. It is NOT
admitted by the prior protocol's complete gate. Preserve those closed results.
One separate32-pair quiet-host W4 development screen is justified by that material
resource change, not an extension of the old sample. Its score>=0.55, full integrity
and prospective resource gates are all required before W4 confirmation. Otherwise
dispose W4 as unpromising/inconclusive under this resource rule; no third screen.
The earlier score and favorable point estimates never substitute for confirmation.

## Domains, sample and operational blocks

- W1 confirmation:128 fresh partition1 opening pairs,256 games, four32-pair blocks.
- W4 requalification:32 fresh partition0 pairs,64 games, one32-pair block.
- W4 confirmation, conditional on requalification: the same128 fresh partition1
  opening pairs,256 games, four32-pair blocks. Never pool correlated worker domains.
- Legal16-ply full-history starts, uniform sample of deduplicated FEN-status keys;
  retain color reversal. Seeds2026102304 confirmation,2026102305 requalification.
  Exclude all prior current-workstream roots/pilots/development and the historical
  CP64 comparator selection. Source book and exclusion identities are in receipts.
- All games10+0.1, no score adjudication/book/tablebase, diagnostics OFF, one-game
  concurrency, BelowNormal, same CP64/Hash64/heaps/JDK21/APC4. Existing actual-profile
  200k-node warmup occurs before UCI handshake. Every fixed block starts fresh JVMs,
  regardless of operational session boundaries. W1/W4 evidence stays separate.
- Reuse the existing frozen pair-block executor with the narrowly scoped CP64
  adaptation. Full-tree input hashes cover added/removed runtime/JDK/tool/model
  files. Reaudit completed games and receipts on resume. OS lock prevents competing
  ownership; owned Windows Jobs drain children. No replay or replacement of partial,
  abnormal, resource-invalid or ambiguously launched blocks. Preserve all evidence.
  Verified completed output before a missing receipt may be adopted exactly once.
- First qualify the new packaging with a two-pair operational pilot in two1-pair
  blocks on already-excluded pilot openings, pause/resume, final export and audit.
  Pilot results are not strength evidence. Unit/fault tests also must pass.

## Resources and admission

Forecast from current64-game studies:28-30min per32-pair block, about4h for both
confirmation domains plus30min W4 requalification. Four-hour total is approximate;
reserve25% slowdown and bounded startup/audit overhead. Each32-pair block has a
3600s owned-worker watchdog, including preflight/audit. Only start if the window
has the entire watchdog+30s remaining. Use bounded sessions under6h, with reliable
completion waits. No simultaneous builds, benchmarks or other research games.

The new prospective resource gate uses Windows cumulative system CPU and memory:
before each block, a30s idle-research window must average<=1 busy logical core,
with>=8GiB available RAM and<=75% physical-memory load at both endpoints. Failure
pauses before launch, consuming no pairs. After the block, subtract the owned
Job's complete CPU from global busy CPU: non-owned average must be<=1 logical core,
with the same endpoint memory requirements. Failure blocks continuation and any
confirmatory interpretation, retaining every game; never selectively exclude or
replace it. These are conservative availability conditions for this12-thread host,
not universal thresholds, isolation guarantees, affinity, or bounds on short bursts.
Record interval counters, CPU, wall time, memory and limitations without outcomes.
Do not manipulate the owner's applications. Check availability at normal15-30min
intervals or on a user availability message, not frequent unchanged-status polling.

## Analysis and fixed stopping

Analyze each confirmation domain only after all128 pairs pass legal/history,
terminal, whole-request telemetry, exact schedule/color, lifecycle, immutable input
and resource audits. Operational blocks/sessions are not statistical looks. No
outcome-based early stop, sample extension, candidate retuning, selective replay,
replacement, pooled worker estimate or transfer of unused alpha is permitted.
The requalification has one exploratory endpoint after32 complete pairs; it does
not contribute to confirmation estimates or remove prior selection effects.

Primary endpoint is mean candidate game score, with the opening/color pair as the
unit: Xi=(scoreWhite+scoreBlack)/2 in[0,1]. For each128-pair confirmation domain use
sample variance across Xi, standard error sqrt(s2/128), and a two-sided97.5% normal
interval (z=NormalDist.inv_cdf(0.9875)). This Bonferroni allocation of alpha0.025 to
each of two domains controls approximate familywise error at0.05, without assuming
independence between worker domains. The normal approximation and opening-sample
exchangeability remain assumptions. For sensitivity also report the two-sided
97.5% bounded-pair Hoeffding interval, radius sqrt(log(80)/(2*128)); do not treat
individual games as independent observations. Report W/D/L, pentanomial, mean,
both intervals and logistic Elo transforms, with infinite endpoints explicit.

Primary lower score bound>0.5 supports a gain in that tested domain, conditional
on all audits and inferential assumptions; if only the normal interval excludes
zero, describe the conservative sensitivity honestly. Upper bound<0.5 supports
inferiority; otherwise result is inconclusive, not equivalence. No fixed minimum
Elo is imposed, and no positive point estimate alone establishes gain. Report all
domains, prior development comparisons/selection and resource limitations.

Also report all complete-request nodes/time including retries/aborts, native
process CPU, peak memory, owned coordinator costs, runner wall time and failures.
These explain economics; they do not independently establish playing strength.
Confirmation is not production promotion or owner acceptance. Resource inability
leaves candidate conditional and confirmation outstanding, without invented games.
