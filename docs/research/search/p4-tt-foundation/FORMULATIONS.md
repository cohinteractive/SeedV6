# Formulations and evidence-driven selection

## TTF-F1-v1 - separate raw-evaluation traffic with optional null admission

Frozen in `prototype-v1-manifest.json` and `raw/prototype-v1-source/`. An optional
4-byte raw lane added main/qsearch lookup and admission alongside the H2 control.
A separate near-depth ordinary upper-bound veto could suppress speculative null
attempts. Raw cache lifetime was one request; score/hint lifetimes stayed independent.

Nine focused tests and 108 diagnostic requests passed. W1 raw consumption avoided
20.72% of evaluator calls with identical nodes/score/PV; retention-only avoided none.
The guard vetoed23 attempts but increased total W1 nodes170 (0.047%).
[Clean cost](cost-v1-summary.json), 180 requests/domain including warmup, showed raw
wall ratios1.1055 W1/1.0786 W4 versus H2. Retention-only was also slower. F1 is **not
qualified**: avoided arithmetic did not justify added lookup/hash/locking traffic.
No strength games were run. The guard has no demonstrated benefit and is removed
from active source; its implementation and evidence remain reproducible in the
frozen prototype. This bounded result does not prohibit other null admission policies.

## TTF-F2-v1 - coherent main-node information with selective evaluation reuse

This intermediate formulation kept the independently valid raw lane,
ordinary/heuristic score provenance and completed hint, but obtain them together
in a single coherent main-node snapshot using the already computed SearchKey.
Existing main-node improving/futility/verified-null consumers materialize cached
raw evaluation only when they actually request it. No qsearch cache traffic or
qsearch key propagation; no new null-admission heuristic. Actual use is measured
separately from availability and cutoff-before-consumption hits.

The `foundation` profile adds TT_INFORMATION to H2 (flags32767999); `retain` has
the same layout, unified probe and raw production, but suppresses raw consumption.
H2 and production profiles retain their definitions. Hash64 remains2,097,152 slots;
entry payload grew from 48 to 56 MiB; measured constructor/heap costs appear below.
Current-request raw validity honors ExactEvaluator's fixed-definition contract;
null probes and legal null verifiers retain independent evaluation/score paths.

Hypothesis: fewer main-node synchronization/probe operations plus retained evaluation
avoid useful selective work at acceptable cost. F2 must repeat causal/value-integrity
and clean-cost qualification; it does not inherit F1 success or erase F1 failure.
Potential benefit is computational capacity for integrated Search, not proven Elo.

F2 passed ten focused tests and54 diagnostic requests. W1 main-node materialization
avoided14,301 of337,396 evaluator calls (4.24%), with identical nodes/score/PV.
[Cost V2](cost-v2-summary.json),108 requests/domain including warmup, found wall
ratios1.0158 W1/0.9970 W4 and CPU1.0222/1.0250. This does not demonstrate a speed
improvement. Actual constructor allocation grew50,335,976 to58,724,600 bytes;
approximate retained heap was also larger. F2 is retained as a diagnostic control,
**not qualified**, while a justified representation/publication refinement is tested.

## TTF-F3-v1 - packed coherent main-node information

Selected experimental formulation. Source inspection of Board/Move/Gen establishes that generated
Search moves fit in unsigned32, including the complete declared move/castling metadata.
Use the existing move word's high32 bits for the ordinary raw value, request epoch
and explicit validity. Preserve all low32 move bits; reject unsupported high-bit moves
before mutation only in the opt-in layout. Legacy table full-long behavior is unchanged.
Capacity/payload remain the original24 bytes per slot. Raw/score/hint lifetimes and
same-key update protection remain separate. All five move-publication paths invalidate
raw on collision and preserve it on same-key updates; clearing/wrap clear packed raw.

Retain F2's coherent snapshot and main consumers, and skip a redundant completed-hint
write only when a successful heuristic score write already performed the same
qualified hint update. Rejected/mate score paths and legacy hint diagnostics retain
fallback publication. No new null policy or qsearch cache. The foundation and retain
profiles now denote this source-pinned F3 implementation; frozen F1/F2 artifacts and
protocols remain immutable and distinguish their earlier meanings.

Hypothesis: remove extra memory and duplicated publication work while preserving actual
raw-evaluation consumption. Repeat correctness, causal and cost qualification; no
success is inferred from a smaller layout alone.

F3 passed the final 323-test suite, 54 diagnostic requests, 624 persistent-history
requests and 104 launcher/control requests plus two explicit stop/reset requests.
The W1 persistent stream avoided 490,175 evaluator calls (5.27%) with unchanged
nodes/depth/score/PV; the retention-only ablation avoided none. Clean short-request
wall ratios were 0.9859 W1 and 0.9914 W4, supporting no material observed overhead
in that domain, not a proven speed gain. Table constructor allocation and capacity
match H2. Clearing the second packed array has a measurable reset cost; see
[allocation evidence](allocation-final-v1-summary.json). The [final A-G assessment](QUALIFICATION.md#disposition-and-authority)
was reconciled by GPT and explicitly accepted by the owner on 30 October 2026;
TTF-R001 is accepted and closed for its limited foundational purpose. The adverse
W4 exploratory competitive signal remains a material limitation. No strength gain
or competitive neutrality is established; no champion advancement or production
promotion occurred.
