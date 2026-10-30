# TT information foundation â€” execution state

Updated 30 October 2026. Governing [TTF-R001](CONTRACT.md); repository masters
P4-R003/P4-F014 after bootstrap. **Active autonomous investigation. No qualification,
strength claim, owner acceptance or production promotion.**

## Bootstrap and challenge

Initial root: clean `main` at `a553cf73`; strict UTF-8 version tuple 0.0.0/build82,
single regular hard-link path, LF/no BOM/trailing newline. Root CODEXLOG_CURRENT.md
absent; no journal created. CP64 hash matches the Contract. No live match/research
process found; unrelated Java editor services preserved. Existing detached control79
worktree is read-only. The current owner request reports P4-R003/P4-F013 mirrors refreshed;
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

Challenge result: no blocking contradiction; proceed with a bounded prototype and
tests, then select from measured information use/cost rather than assuming success.

## First prototype evidence

Bootstrap committed at `7754ebd5` without a build bump. The first prototype is
uncommitted code with build82 metadata, distinctly frozen by
[prototype manifest](prototype-v1-manifest.json); it is not the historical build82
artifact. Five storage and four Search tests passed, including raw zero/mate domains,
independent lifetimes, collisions/wrap, concurrent publication, avoided inference
against full-rebuild evaluation, null isolation and W1-16 accounting/drain/restart.
Raw logs/XML are in `raw/focused-v1`. The harness's first compile failed because
AspirationSearch constructors require concrete owner types; corrected without an
engine change, preserving `raw/prototype-build-v1.log` and the successful v2 log.
The bootstrap manifest's CRLF was flagged by diff-check and is normalized to LF
in the next attributable change; no data/identity changes.

[TTF-DIAGNOSTIC-V1](diagnostic-v1-protocol.json) completed 108 requests across W1/W4,
nine real-history roots, depth6/1M cap, six causal profiles. All requests passed
legality, accounting, board and drain gates ([summary](diagnostic-v1-summary.json)).
In W1, raw reuse avoided **69,924 / 337,396 = 20.72%** of actual evaluation calls
with identical nodes/score/PV versus H2. Retention-only had the same availability
but avoided no evaluation. This establishes consumption and avoided inference, not
speed or strength. W4 also consumed raw values, with scheduling-dependent trees.
Near-depth bound-aware null admission vetoed23 W1 attempts and reduced null work,
but total nodes rose170 (0.047%): no demonstrated integrated effort benefit.
It remains a candidate dependency test, not automatically part of the final foundation.

TTF-COST-V1 is prospectively fixed in [protocol](cost-v1-protocol.json): same frozen
prototype, five profiles, W1/W4, nine roots, 250k nodes, one warmup plus three
measured rounds, rotated order. Raw output `raw/cost-v1-w1.jsonl` and W4 counterpart.
Do not confuse completed raw outputs with a live process; check process state on resume.

## Current action

**Selected for final qualification: TTF-F3-v1, packed coherent main-node information.**
See [FORMULATIONS](FORMULATIONS.md) for the preserved F1/F2 negative/inconclusive
cost findings and structural adaptations. No qsearch cache or new null policy remains.

F3 combines raw evaluation with existing score/hint information in the existing24-byte
slot, and consumes one coherent snapshot in main Search. Raw values are materialized
only by actual improving/futility/verified-null evaluation consumers. Separate current
request raw validity, H2 score age, hint freshness and provenance remain. Generated
unsigned32 move bits are preserved; unsupported high bits are rejected before mutation
only in opt-in mode. Legacy full-long tables remain compatible. New explicit research
launcher uses flags32767999; production stays3407871, H1/H2 unchanged.

[323 tests](broad-v3-summary.json) passed (206 core,26 UCI,91 selected research).
[54 F3 diagnostic requests](diagnostic-v3-summary.json) preserved W1 nodes/scores/PVs
and aspiration windows, avoiding14,301 evaluator calls, including5,546 during27
repeated-window attempts. [216 clean-cost requests](cost-v3-summary.json), including
warmups, showed wall ratios0.9859 W1/0.9914 W4 and CPU1.0071/0.9759 versus H2.
These small/noisy differences support no material overhead in this domain, not a
proven speed improvement. Actual table-constructor allocation is50,335,976 bytes
in both modes; payload50,331,648 bytes (48MiB),2,097,152 entries. Worker scratch is
additional and JVM/process footprint is a separate quantity.

[624 persistent-history requests](lifetime-v1-summary.json) passed on the frozen F3
prototype. Across104 W1 requests, 490,175 evaluator calls (5.27%) were avoided with
identical nodes/depth/score/PV versus H2. Retention-only avoided none despite having
available values. W4 consumed500,026 raw values, with valid accounting/PV/draining;
its scheduling-dependent trees preclude direct equality. Timing was instrumented.

Final source adds a defensive rejection of legacy TYPE_EVAL writes in the opt-in
information layout: otherwise a caller could mix the legacy untyped evaluation-table
representation with scored records. Legacy tables are unchanged. The F3 normal
producer/consumer paths used by measured diagnostics do not invoke TYPE_EVAL.
The final323-test run and runtime installation are at `raw/final-tests-v1` (check
completion and exact source before resuming). New lifetime/memory harnesses do not
alter the frozen engine. Initial V2 wrapper setup failed before Search because a
Python manifest-preparation syntax error left the runtime absent; retain
`raw/diagnostic-v2-w1.log`; separately named retry1 runs completed.

[Launcher isolation](isolation-v1-retry1-summary.json) passed 104 requests plus two
explicit stop/reset requests. Frozen build80, H1 and H2 match their current W1 routes;
foundation also matches frozen H2. W4 accounting/PV/draining passed. The initial
system-Python invocation failed at import (missing chess) before any engine started;
the existing research venv completed the same frozen protocol. Both logs remain.

[Allocation/reset](allocation-final-v1-summary.json) confirms identical constructor
allocation. Median empty-table clear cost was 0.668 ms H2 versus 2.331 ms foundation;
the packed layout clears a second array. This bounded lifecycle cost is accepted
for the experimental request-scoped foundation, not hidden in timed Search costs.

Next: commit this qualifying implementation with build83, freeze its installed runtime,
and run the prospectively fixed small paired game/lifecycle screen against frozen H2.
The build update, when present in this commit, is 82 to 83 and belongs to this runtime
change only. Do not increment again for final reports or replay the completed studies.
No standalone strength inference, champion advancement or production change is planned.
Then assess A-G and propose limited technical qualification, or continue from any
material adverse evidence. No final qualification has been declared yet.

No blocking human action. Owner mirror refresh to P4-R003/P4-F014 is non-blocking for
repository work and required only before mirror-dependent Project planning.
