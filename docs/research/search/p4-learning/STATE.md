# HL-R001 resume state

**Research execution complete, 2 November 2026. HL-G1 met the prospectively frozen
W1 champion-relative strength criterion:136W/89D/95L,56.41%, paired-normal lower
bound51.82%. Mandatory W4 risk investigation is complete, with an unresolved adverse
champion-relative point estimate. Conservative bounded sensitivity crosses equality.
GPT/owner reconciliation and acceptance remain pending; no promotion or successor.**
Read [RESULTS](RESULTS.md) for the handback and [qualification summary](qualification-v1-summary.json)
for aggregate evidence. No experiment remains running.
Read [CONTRACT](CONTRACT.md), then this state. Owner request and initial file/hash
checks are in `bootstrap-manifest.json`; external source identities are in
`external-manifest.json`. Raw evidence is local-only and must be checked before reuse.

## Verified bootstrap and challenge

Starting clean `main` HEAD is `ada8032ab64e0febc5b22bb2b6a87ed6e2120cfd`, exactly
the supplied closeout. Masters are P4-R003/P4-F016. Owner confirms those Project
mirrors. Root CODEXLOG_CURRENT.md is absent; do not create it. VERSION_STATE is a
regular single-link UTF-8 JSON file, tuple0.0.0/build83; exact baseline bytes/hash
are recorded. Documentation/diagnostics alone do not bump. Qualifying committed
core changes require one direct build increment in the same coherent commit.
No finalizer or retired machinery is used. No unrelated starting changes found.

The first coherent HL implementation uses **build84**: only the build integer was
changed83→84 after qualification, with exact baseline-byte and resulting-byte/tuple
verification. The user-owned version fields remain0.0.0. The commit contains the
opt-in core policies, research launcher/harness/tests and this objective's records;
find it by `research(search): add retained learning and history-guided effort`.
Production remains flags3407871. `verifyProjectBoundaries` passed. The committed
runtime was rebuilt/frozen and verified against the tested prototype before games.
No push, release, default change or champion advancement is authorized.

Implementation commit: `b8ea8b2856eed3f6c774860ca540375054e4de30`. The committed
build84 runtime is frozen in `raw/runtime-build84-v1`; its manifest and
`build84-artifact-qualification.json` verify2132 entries/files against the prototype,
with only generated version metadata changed, plus4 successful candidate preflight
requests. Screen protocol/input freeze commit is `447f813e`.

Completed `g1-screen-v1-protocol.json`,32pairs W1 against frozen80, under
`raw/g1-screen-v1-w1`. Summary passed:64 normal games,8,729 audited requests,
no missing-score warnings, stable inputs and drained engine handles;1931.67s total
wall (1929.19s games),1946.09s combined engine CPU. Candidate30W/18D/16L,
score60.9375%, paired normal95 interval52.15-69.72%, conservative bounded95
36.93-84.95%. Exploratory reuse/selection and broad uncertainty preclude confirmation.
Frozen80 is not the champion; no games are pooled with historical campaigns.

Completed R1 reverse ablation:32pairs/64 normal games versus80,19W/20D/25L,
45.3125% score, paired normal95 interval35.61-55.01%.8,640 audited requests,
no missing-score warnings, stable inputs, drained engine handles,1933.56s wall.
`g1-r1-ablation-v1-summary.json` preserves common-opening descriptive comparison;
G1's score is15.625 points higher. Separate timed runs/shared development inputs
and exploratory selection preclude an independent component or champion claim.
This supports the integrated G1 formulation rather than retention alone.

Completed G1 W4 risk against80:32pairs/64 normal games,32W/13D/19L,
60.15625% score, paired normal95 interval49.19-71.13%, bounded95 36.15-84.16%.
8,993 audited requests, no missing-score warnings, stable inputs and drained handles;
1969.98s wall. Candidate/base engine CPU2094.86/2119.63s; total admitted nodes
1,181,396,858/1,157,855,386 over roughly979s request wall per side. These operational
and workload observations do not establish W4 speed, superiority or equivalence.
No new correctness/lifecycle defect or adverse competitive point estimate emerged.

Completed direct W4 champion79 bridge:19W/21D/24L,46.09375%, paired normal95
interval36.48-55.71%, bounded95 22.09-70.10%.64 normal games,9,577 audited requests,
no missing-score warnings, stable inputs and drained handles;2040.14s wall.
Candidate/champion CPU2059.69/2097.86s. This adverse point estimate is inconclusive,
not proof of regression; together with the separate80 screen and historical bridge,
it leaves material W4 competitive uncertainty. Do not pool the controls or claim
W4 neutrality. Node rates against79 are confounded by its known accounting difference.

Proportionate follow-up `w4-replay-v1-protocol.json` selected the first two games by
schedule and seven unique real-history prefixes at plies16/40/64/88, without outcome
filtering. Frozen84 runtime ran126 requests including21 clean W1 observation checks
and21 W4 warmups; all board/PV/admission/deadline/drain assertions passed in35.08s.
Each private W4 worker exercised history-guided effort. Clean W4 G1/control wall
ratio1.0355 is a small, variable seven-root measurement, not a speed/strength claim.
No operational defect was found; the W4 competitive result remains unexplained.
W4 is not strength/equivalence qualified, and broader use is not supported.

Completed independent confirmation: `g1-confirm-v1-protocol.json`,160 fresh pairs
W1 against actual frozen79,136W/89D/95L,56.40625%. The primary one-sided nominal
alpha.025 paired-normal lower bound51.8239% exceeds50%, and the point score exceeds
the predeclared53% practical threshold. Two-sided paired-normal95 interval51.82-60.99%;
logistic point estimate+44.76 Elo (normal interval+12.68 to+77.62). These are approximate
paired-sampling inferences, not exact finite-sample proof. The conservative bounded
lower bound45.67% does not confirm superiority; preserve that disagreement.

All320 games ended normally;44,104 request audits passed with no missing-score
warnings, stable inputs and drained handles. The frozen analysis ran after the
successful audit, unchanged. No partial score informed a decision; the owner's
in-flight progress question was answered using completed-game counts only.
Confirmation wall9736.34s; raw evidence is `raw/g1-confirm-v1-champion79-w1`.
All five matches are complete/drained, and OS inspection found no Fastchess process.
Mandatory W4 risk is characterized as above, without claiming W4 strength/neutrality.

Fresh input preparation checked160 unique legal roots across five first moves and
27 first-move/reply families, with zero overlap against56,553 excluded source/receipt
keys. The curated remaining pool of1,215 roots, W1 OWNER, CP64 Hash64,10+0.1 and
this Windows/Java host define the supported domain. No broader clock/platform,
opening-distribution or multiworker strength conclusion follows.

Actual build79/80 runtime JARs and CP64/Java executable match historical manifests.
Source80's qualified accounting fix remains present. Current optional H1/H2/F3
code is inherited, not this objective's work; production still selects flags3407871.
No need to roll back source or reopen those studies. TTF acceptance is limited;
its negative W4 evidence and TT-R001 pending reconciliation remain unchanged.

Source findings (static, not runtime qualification):

- `FlatSearchState.reset()` clears capture/continuation/killers/counters at each
  ExactSearch invocation. Main quiet history is separate: IterationReuse can retain
  selected completed adjacent iterations, but its eligibility is stricter than
  general re-search retention. ADAPTIVE16 can invoke repeated windows.
- Gravity uses limit16384 and depth-squared bonuses; NOMINAL_HISTORY filters
  unreverified reduced siblings from maluses. Completed quiet cutoff winners are
  checked; synthetic null searches do not contribute normal learning. Capture and
  continuation updates are also nominal-filtered in Construction.
- Current LMR starts at fourth quiet/fourth ordinal, excludes root/check/tactical/
  hash/check-giving/near-draw/mate-domain/synthetic cases, uses1/2/3-ply depth/ordinal
  steps and existing improving/caution relief. Fail-high reductions are verified.
  Continuation affects ordering but does not grade reductions.
- W4 ROOT_REUSE orders roots through a call that clears owner history, then copies
  owner's quiet table to helpers after the first completed root job while drained.
  Other histories are not seeded and reset per root job. Retention therefore needs
  an explicit worker policy; changing only the W1 reset is inadequate.
- Request entry/end and worker joins provide safe lifetime boundaries. No shared
  mutable history is necessary for an initial worker-local formulation. TT score
  reuse/raw caching is optional; no present dependency justifies adopting F3.

Challenge resolved: previous master no-successor language describes pre-launch
state and is superseded by the explicit new owner request, not silently by this
record. W1 success is permitted independently of W4 superiority, with mandatory
risk investigation. Existing historical source findings are hypotheses until
rechecked; baseline control equivalence must be measured. Retention may multiply
noisy sign-based pruning protection; do not assume it helps. A160-pair confirmation
has limited power for small effects; insufficient precision is a possible legitimate
resource handback, not success. No blocking authority conflict identified.

## Completed work and deferred opportunities

Completed bootstrap diagnostics, first retention formulation and integrated effort
prototype. [FORMULATIONS](FORMULATIONS.md) describes mechanisms, identities and
limitations. `baseline-v1-summary.json`, `retain-v1-summary.json` and
`guided-v1-summary.json` preserve the measurements; frozen prototype runtimes and
source snapshots are identified in their manifests. Baseline diagnostic build output
was mutable `build/research/runtime`; its recorded hashes describe the executed
artifact, not a retained installation. Later prototypes use immutable raw copies.

Baseline9 W1 roots/102 windows showed64,964 eligible late moves, predominantly
zero or magnitude<65. Retention reduced zeros but increased work on several roots.
G1 exercised3,208 increased/1,254 decreased actual reductions in completed W1
invocations; aborted-invocation counters reset and W4 counters are last-job snapshots,
so these are not whole-request event totals. Raw trace histograms include actual
decisions before cancellation. All observed W1 requests matched clean identical-profile
nodes/completion/score/PV. These are learning/effort facts, not strength or speed.

`guided-broad-v1-summary.json`:330 tests, zero failures/errors/skips across selected
core/UCI/research suites, including7 new lifetime/effort/accounting tests. Existing
exact-reference, null, mate, TT/coherence and Construction checks passed. The prior
retention-only16-test run is overlapping coverage, not16 additional distinct tests.
`isolation-v1-summary.json`:65 UCI requests plus4 stop/reset requests passed;
current production and explicit research-control W1 behavior matched frozen80.
Candidate W4 checks verify bounded legal PV/accounting/lifecycle, not equality.

Clean costs completed:162 requests including54 warmups,45.45s total. G1/control
wall ratios1.0153 W1/0.9921 W4 and R1/control0.9948/0.9918 show no prohibitive
observed overhead; no speed claim. G1's first game screen is now positive as above.
R1 reverse ablation, W4 risk and independent W1 confirmation all completed. G1 was
selected using exploratory strength and technical evidence, not depth/node results.
Nothing has been promoted.

Prospective `reference-audit-v1-protocol.json` was frozen before reading game outcomes.
Research harness build passed(2.55s); core classes match frozen84, only generated
version metadata differs. Audit passed in3.96s:43 sampled reductions across9 roots,
all reduced/nominal recursive TT-off references completed with legal PV and real-history
replay.42 stayed below the original threshold at both depths; one negative-history
sample(opening-6,depth3,reduction1,main-26/continuation-25) crossed from-382 to-357
at threshold-358. It is a horizon-disagreement witness, not a measured flat-kernel
error or population rate; original selective context/mate distance are absent.
There is no demonstrated correctness defect or evidence here to tune a new constant.

The autonomous work queue is complete for this objective. Next: GPT/owner reconcile
the frozen evidence, statistical limits and W4 risk before acceptance or dependent
progression. Do not repeat confirmation, tune the selected candidate or launch a
successor merely because a new session opens this state.

Deferred: cross-request decay, richer continuation contexts, stronger selective
coupling or F3 integration require actual evidence of a useful dependency. These
are choices within this objective, not an automatic feature queue.

Further static dependency: `ExactSearch.searchWindow` clears learned tables on
Aborted. A later cross-request formulation cannot work merely by skipping the next
request's initial reset: a budget-ended final iteration would have discarded them.
Any such formulation must distinguish completed subtree cutoff labels (published
only after completion checkpoint) from incomplete enclosing iterations, preserving
truthful completion and completed-root guidance rules. Request-local R1/G1 retain
the existing abort clearing; no cross-request behavior has been implemented.
Also, VERIFIED_NULL requires all original Construction flags, so a lifetime ablation
must preserve that valid composition rather than silently remove a feature/dependency.

## Resources, validation and gates

Bootstrap:12 logical processors, about32GiB RAM/15GiB available, existing research
Python/chess/Fastchess tools. Unrelated IDE/Gradle processes were observed and left
alone. Prior490/529s eight-pair TTF runs anchor the initial cost plan; current costs
are now measured. Technical qualification and all planned game batches are complete.

Aggregate allowance:128 exploratory pairs (reserve32 W4),160 fresh W1 confirmation
pairs/one attempt,6h game wall and2h active build/diagnostic execution. Spent:128/128
exploratory pairs(256 games),160/160 confirmation pairs(320 games),one/one attempt.
Charged game wall17611.69s(4h53m32s), below21600s; total engine CPU21931.61s.
About327s active build/diagnostic execution including W4 replay, input preparation, artifact preflights and reference
audit (about228s through clean costs plus packaging/preflights and6.51s reference work).
Minor setup/analysis/inspection time is not engine compute. All576 games ended
normally and80,043 request audits passed. Both pair allocations are exhausted;
remaining wall/diagnostic allowance does not authorize another confirmation claim.
Full unrelated GUI/training/native-platform matrix was not run; broader resource
domains and universal equivalence of selective Search are not qualified.

Prospective power reminder (not a frozen confirmation protocol): at one-sided
alpha0.025 and80% normal-approximation power,160 independent pairs target roughly
5.54/6.87/7.75 score points at pair SD0.25/0.31/0.35. A3-point gain at SD0.31 needs
about839 pairs, materially outside this envelope. Final planning must use actual
exploratory variance and a credible effect, not the pilot point estimate alone.
The named legal16 opening source is locally available. Fresh confirmation inputs
excluded exposed roots without opening either forbidden BOUNDARY2 study.
[CONFIRMATION-DESIGN](CONFIRMATION-DESIGN.md) records the candidate-independent
input preparation, review of historical receipt schemas and proposed paired inference.
Pinned input source downloaded; full parsing/selection passed after R1 drained.
160 legal distinct roots selected from1,215 eligible held-out roots, excluding56,553
historical/source keys with zero overlap. Receipt records exact inputs/exclusions;
the resulting curated subset limits generalization. Six synthetic inference checks
passed. Frozen protocol/analysis remain unchanged and the sole allowance is spent.
No replay or sample extension after exposed outcomes is authorized.

Final version handling: no additional bump for analysis/documentation. Exact version
bytes match the evidenced implementation commit's0.0.0/build84; core/UCI/launcher
sources remain unchanged since that freeze. No journal was created because root
CODEXLOG_CURRENT.md is absent. All task changes are attributable to this objective.

Human actions: blocking for acceptance/dependent progression — GPT/owner reconcile
the W1 result, approximate inference, conservative disagreement and unresolved W4
risk. Non-blocking for completed research — refresh the Project Frontier mirror to
P4-F019; this blocks mirror-dependent planning until done. Champion/defaults remain
unchanged. No further autonomous research or successor is authorized by this handback.
