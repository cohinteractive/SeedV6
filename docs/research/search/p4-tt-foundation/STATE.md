# TT information foundation - execution state

Updated 30 October 2026. Governing [TTF-R001](CONTRACT.md); repository masters
P4-R003/P4-F016 at closeout. **TTF-R001 accepted and closed as a successfully completed,
limited foundational engineering objective. GPT technical reconciliation and explicit
owner acceptance are complete. No strength claim, champion advancement, production
promotion or successor authorization.**

## Bootstrap and challenge

Initial root: clean `main` at `a553cf73`; strict UTF-8 version tuple 0.0.0/build82,
single regular hard-link path, LF/no BOM/trailing newline. Root CODEXLOG_CURRENT.md
absent; no journal created. CP64 hash matches the Contract. No live match/research
process found; unrelated Java editor services preserved. Existing detached control79
worktree is read-only. The launch request reported P4-R003/P4-F013 mirrors refreshed;
this supersedes the older Frontier's unconfirmed-refresh statement without claiming
automatic synchronization. Historical campaign reconciliation remains separate.

Read actual Search masters, root AGENTS, unified ownership contract/state and prior
TT CONTRACT/STATE/BRIDGE/H1/H2/qualification/disposition records. No material authority
contradiction: the present owner launch supersedes historical no-objective status.
No global policy/CEC maintenance or registry change is needed.

Bootstrap challenge, performed against source rather than inherited labels:

- Production extensions still suppress ordinary score stores; H1/H2 selective scores
  are separate and only consumed for eligible interior null-window cutoffs. Hints have
  additional actual ordering/protection consumers. Thus score reuse alone is not new.
- `constructionEval` memoizes only within one live node. Main retries/iterations and
  qsearch recompute. `TTable.TYPE_EVAL` is an incompatible separate-table legacy path,
  not raw evaluation coexisting with searched scores. ExactEvaluator requires board-only
  static values with a fixed definition **within a request**, so initial cache lifetime
  must not silently assume cross-request evaluator identity.
- Raw evaluation, selective searched bounds and completed moves need distinct validity.
  Raw writes must not refresh a score or evict useful guidance. Keep full SearchKey and
  synchronized access; external partial keys/racy records are not transferred.
- Flat qsearch currently does not propagate SearchKey history because it has no TT.
  Any raw reuse there requires explicit real-edge history propagation and synthetic
  exclusion; using stale main-ply history would invalidate the design.
- VERIFIED_NULL reenters the same ply, restoring static scratch after its verifier.
  Preserve all callbacks/accounting even on cache hits. A shallow upper-bound veto
  changes heuristic admission, not the raw value, bound truth or legal verification.
- External implementations have different raw/corrected/search meanings and supporting
  heuristics. Their overall Elo and inline Elo comments are not transferable evidence.
- A 4-byte optional lane is sufficient for bounded ordinary raw values, request epoch
  and explicit validity. Avoid a generic information framework or clustered rewrite.
  Actual allocation/collision costs still need measurement, not source-only arithmetic.
- Mere cache hits/guard counters cannot qualify: require independently checked values,
  reduced evaluator calls or useful changed Search work, clean cost and reverse ablations.
  Retain a cache-disabled control with identical layout to separate storage from use.
- A small STATE action list is sufficient. No extra work-selection framework. No
  production-default changes; exact controls and frozen BOUNDARY2 evidence stay intact.

Initial challenge result: no blocking contradiction. This led to a bounded prototype
and selection from measured information use/cost, rather than assumed success.

## Retained first-prototype history

Bootstrap committed at `7754ebd5` without a build bump. The first prototype was
uncommitted code with build82 metadata, distinctly frozen by
[prototype manifest](prototype-v1-manifest.json); it is not the historical build82
artifact. Five storage and four Search tests passed, including raw zero/mate domains,
independent lifetimes, collisions/wrap, concurrent publication, avoided inference
against full-rebuild evaluation, null isolation and W1-16 accounting/drain/restart.
Raw logs/XML are in `raw/focused-v1`. The harness's first compile failed because
AspirationSearch constructors require concrete owner types; corrected without an
engine change, preserving `raw/prototype-build-v1.log` and the successful v2 log.
The bootstrap manifest's CRLF was flagged by diff-check and normalized to LF
in implementation commit32feb907; no data/identity changed.

[TTF-DIAGNOSTIC-V1](diagnostic-v1-protocol.json) completed 108 requests across W1/W4,
nine real-history roots, depth6/1M cap, six causal profiles. All requests passed
legality, accounting, board and drain gates ([summary](diagnostic-v1-summary.json)).
In W1, raw reuse avoided **69,924 / 337,396 = 20.72%** of actual evaluation calls
with identical nodes/score/PV versus H2. Retention-only had the same availability
but avoided no evaluation. This establishes consumption and avoided inference, not
speed or strength. W4 also consumed raw values, with scheduling-dependent trees.
Near-depth bound-aware null admission vetoed23 W1 attempts and reduced null work,
but total nodes rose170 (0.047%): no demonstrated integrated effort benefit.
The guard was rejected from the final formulation; its source and evidence remain.

TTF-COST-V1 was prospectively frozen in [protocol](cost-v1-protocol.json): same frozen
prototype, five profiles, W1/W4, nine roots, 250k nodes, one warmup plus three
measured rounds, rotated order. Raw output `raw/cost-v1-w1.jsonl` and W4 counterpart.
The run completed with material cost penalties and F1 was not qualified; see
[FORMULATIONS](FORMULATIONS.md) for F1/F2 selection and subsequent F3 evidence.

## Current disposition and resume point

**TTF-F3-v1 - Packed coherent main-node information: accepted limited experimental
foundational technical qualification.** In the 30 October 2026 closeout authorization,
the owner confirmed GPT's reconciliation and acceptance against the agreed A-G
requirements, explicitly accepted the qualification, and authorized documentation
closeout only. Technical execution and acceptance closeout are complete.
Read [QUALIFICATION](QUALIFICATION.md) for the preserved assessment, exact controls,
adverse evidence, costs and limits; acceptance does not broaden those domains.

Implementation: `32feb9078d0aa53a26e6c23bfb772f815e501e7f`, 0.0.0/build83, flags32767999.
[Runtime](runtime-f3-build83-v1-manifest.json) and [artifact equivalence](build83-artifact-equivalence.json)
identify the committed executable. Build82 to83 was verified exactly once in the
qualifying implementation commit; user-owned version components stayed0.0.0.
Do not bump again for these reports or the same completed implementation.

Evidence: final323 tests passed;54 causal diagnostic requests,624 persistent-history
requests,216 clean-cost requests including warmups,104 launcher/control requests
plus two stop/reset requests passed. W1 avoided490,175 of9,304,481 evaluator calls
(5.27%) in persistent real histories with identical nodes/depth/score/PV; the
retention-only ablation avoided none. This removes output-head evaluation, not
accumulator transitions or5.27% of total Search cost. Same-runtime H2 clean-cost
wall ratios0.9859 W1/0.9914 W4 support no material observed overhead in that domain,
not a proven speed gain. Table payload stays48MiB and constructor allocation stays
50,335,976 bytes; per-worker scratch adds about1KiB. Median table clear cost rises
0.668 to2.331ms. See FORMULATIONS for retained F1/F2 negative/intermediate evidence.

Both fixed eight-pair game domains completed against actual frozen H2/build82:
[single worker](screen-v1-w1-summary.json),5W/5D/6L; [four workers](screen-v1-w4-summary.json),
3W/5D/8L. All32 games ended normally and4,723 request traces passed, with zero
missing-score warnings, stable inputs and drained engine handles. Strength is
inconclusive and the W4 point estimate is adverse. [Trace investigation](screen-v1-trace-analysis.json)
found no clock/accounting/fallback failure or broad effort/depth collapse; it does
not rule out a competitive penalty. This foundation is not endorsed as stronger
or neutral, especially in W4. No more games or tuning follow automatically.

All completed and failed outputs remain under `raw/`; [integrity](evidence-integrity.json)
checks cover named source/runtime/JSONL groups and game evidence. Raw files are
local-only and unavailable in a fresh checkout until separately supplied; check
availability and hashes before use. Never overwrite/replay completed protocols.
The initial V2 wrapper and system-Python isolation import failures are retained;
separately named retries passed. Historical TT-R001 records and unopened BOUNDARY2
studies remain intact. No process from this workstream remains active at handback.

**Terminal resume point: TTF-R001 is accepted and closed. No research or technical
qualification step remains authorized.** Do not restart implementation, experiments,
benchmarks or games. Any future objective requires separate GPT/owner selection and
authorization. Future use of this foundation must retain its exact configuration,
relevant H2 dependency/control, operational qualifications, tested domains and
negative W4 signal; reduced evaluator calls do not establish stronger play.
Build79 remains accepted champion;80 is the accounting-corrected technical control.
Historical H1/H2 remain experimental, not independently accepted foundations, with
their previous evidence and separate pending reconciliation unchanged.

The owner confirms the **P4-R003/P4-F015** Project mirrors were refreshed before this
closeout. Current masters are **P4-R003/P4-F016**; only the Frontier revision changed.
Refresh its separate Project Source to P4-F016 before mirror-dependent planning.
This is non-blocking for completed repository closeout; no mirror was updated
automatically. This documentation-only closeout ran no technical qualification and
leaves build83 unchanged. No push, deployment, release, production default change,
champion advancement or successor launch occurred.
