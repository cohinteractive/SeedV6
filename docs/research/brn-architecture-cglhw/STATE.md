# BRN architecture programme: final state and shared frontier

Updated 2026-10-07. **Research complete: recommend C02 compiled order-2 pair tables
for the next BRN architecture/integration decision.** Production adoption and user
acceptance have not occurred. No empirical process or scheduled experiment remains.

Read [RECOMMENDATION](RECOMMENDATION.md) for the decision and its scope,
[final results](Z01-FINAL-RESULTS.md) for complete confirmation evidence,
[COMPLETION-AUDIT](COMPLETION-AUDIT.md) for all original requirements,
[CONTRACT](CONTRACT.md) for authority, and [SCORECARD](SCORECARD.md) / [Z01](Z01.md)
for the cross-family comparison and chronological findings.

## Exact resume point

The authorized research objective has been answered. All 768 final match batches
and 22 sealed quality profiles completed, and their statistical/resource/identity
audits passed. Final reconciliation verified the refreshed index and current source
bindings. Git history is the authority for the closing local commit; its packaging does not
change the original execution HEAD or source hashes. Do not
start a replacement experiment, rerun completed batches, reopen sealed selection,
or adopt the production evaluator automatically.

If work resumes later, first read the final recommendation and audit. Its
prioritized follow-ups are explicitly deferred proposals, not active allocations.
Production adoption requires a separate owner-authorized work unit. A new
research question needs a prospective design and new output paths; frozen plans
and exclusive-output scripts are historical evidence and must not be overwritten.

## Final evidence and conclusion

All 3,072 final games were scored: zero missing, capped or failed outcomes,
maximum 654 plies. At 100 ms/move compiled pairs scored 80.18% against fresh BRN,
73.73% against current material NNUE v2, 74.90% against strict NNUE, 64.26% against
cheap material, 55.57% against captured native BRN and 73.93% against compiled own
Gen0. Both source/order runs are positive in every comparison. All primary
unadjusted two-way bootstrap gates pass. Conservative simultaneous bounded-pair
checks pass five controls but not native BRN; no unconditional all-controls
superiority or robust native noninferiority claim follows.

The result supports practical fixed-budget strength, not global optimality or
long-run scaling superiority. Pair prediction plateaus; current NNUE was still
improving. Data-size playing intervals remain inconclusive. Sealed prediction
quality is better for BRN/edge than pairs, despite the opposite practical playing
decision. Two adjacent source intervals lack game IDs. Pair initialization is
zero; replication varies data/order, not random initial weights.

Final sealed allocation: 22 x 32,768 positions = 720,896 visits / 65,536 distinct
positions; all checks passed. Original and compiled pair metrics/reliability had
zero observed difference in each range. No selection changed after unsealing.

Canonical final records:

- z01-final-match-plan.json, SHA256 e90f0f04fb2bf33019946f93e8c85391f1d02ab5626e60d36347f76ef6858529.
- z01-final-match-results.json, z01-final-match-resources.json and z01-final-match-crosscheck.json.
- z01-final-quality-plan.json, SHA256 296e6b6ffbc5ed3a022952e854493cb0bf278dac8d4e018635673bf5429182cd.
- z01-final-quality-execution-plan.json and z01-final-quality-results.json.
- z01-final-runtime-results.json / crosscheck: 28 processes, 1,792 depth-4 roots and 14,553 exact child scores.
- z01-final-reconciliation.json: 1,707 receipts, 3,744 pair files, 2,929 frozen bindings; current test source and production-boundary checks passed.
- EXPERIMENTS.json: compact v2 index with commands, receipt/result/pair hashes and source/compiled manifest digests; full raw traces remain at referenced paths.

The final opening sample (seed840223, indices1500..1627) passed warmup/internal,
prior552FEN/2662source and all-ten-dataset checks. The earlier rejected unplayed
sample and immutable Z01-FINAL-OPENINGS-02 amendment remain preserved.

The selected pair checkpoints are the 300-second endpoints of the within600s
rule, gain .25, under z01-scaling-compiled-compute-600-211 and -337 in the ignored
research directory. Exact source-model/metadata identities are bound in the
frozen plans. Compilation does not change fitted capacity or original weight bytes.

## Closed shared frontier

| ID | Family / hypothesis | Final disposition | Next action |
|---|---|---|---|
| E000 | Bootstrap / prior work / external research | Reconciled; literature and methodology challenge retained | None required |
| E001 | BRN, current/legacy/strict NNUE | Characterized and directly controlled at final tested budget | Longer mature-NNUE work is optional; separate programme remains separate |
| E002 | Geometry, native models, resources | Fresh A/B and final opening audits passed; native history remains unmatched | Preserve limitations and rejected preflight |
| B01 | Linear / quadratic energy | Learning observed; practical cost deficit; larger energy/rank work deferred | New discriminating hypothesis required to reopen |
| C01 | Discrete pair / triple tables | Pair mechanism retained; higher order did not earn additional practical allocation | Original pairs remain same-capacity control |
| C02 | Equivalent pair compilation | Recommended practical architecture; parity, repeated cost and final games support it | Optional owner-authorized adoption/integration |
| D01 | Nested cubic products | No consistent whole-model added-order/cost gain; component evidence preserved | Deferred |
| E01 | Piecewise-linear edge functions | Predictive-quality trade-off retained; no established playing lead | Deferred |
| F01/H01 | CONTEXT plus linear / SELF | Context screen 40.625% versus SELF, both runs below .5 | Further expansion deferred |
| G01 | Learned hard bitboard circuits | Small-model niche; learned-gate playing gain unconfirmed | Further mainline expansion deferred |
| I01 | Material / ownGen0 controls | Final direct comparisons establish useful learned strength | Controls retained |
| Follow-up | Modern learned unary/PST | Considered before final freeze; no positive playing admission; high-value optional follow-up | Not a necessary unfinished experiment |
| Z01 | Replication / scaling / recommendation | All planned evidence and recommendation complete | No further empirical allocation |

These dispositions are specific to tested implementations and gates, not universal
rejections or transitive head-to-head rankings. No unresolved admitted experiment
is necessary for the stated recommendation. Final acceptance remains with the owner.

## Reproducibility, data and validation

Raw models, checkpoints, results, command receipts and orchestration scripts remain
under app/build/research/brn-architecture/. Corpora/checkpoints are not committed.
The compact index can be regenerated with:

    python -B tools/summarize-brn-architecture.py app/build/research/brn-architecture docs/research/brn-architecture-cglhw/EXPERIMENTS.json --compact

Byte-identical final controller/audit sources are also retained in [reproduction](reproduction/README.md).
The prior v1 index is preserved at final-index-prior-v1.json in the ignored research
directory. Its 885 receipts are a verified subset of the final 1,707. The index
contains 1,704 completed and three failed executor receipts; separate Gradle and
analysis failures are also retained. A failed historical attempt is not a pending
rerun. Full scientific curves/metrics remain in hashed raw results and dedicated
analysis JSON/Markdown, rather than being duplicated in the index.

Fresh A/B each contain262144train/32768validation/32768sealed positions. Raw
intervals are[2351172,2735463) and[2735463,3122388). Next unconsumed raw endpoint
is3122388 if later justified work is authorized. Earlier data02 overlap and the
nested-data audit amendment remain in E002/Z01. User stores on E: were read-only.
No additional data growth, gain grid, training ladder or match extension is scheduled.

Recorded distinct targeted validation:84Java/28Python tests. Final reconciliation
rechecked the latest28Java tests'11archived XML suites and142current source bindings,
Python receipts, final frozen code/model/data bindings and all final result hashes.
This is read-back verification, not a new full-suite test run. Full/slow suites,
GUI/browser, packaging, deployment and cross-platform production QA were not run.
See RECOMMENDATION for runtime, qsearch and numerical-equivalence limits.

## Git, version and human boundaries

The programme started from clean HEAD121e4a607367903d7c93af5a098da791a71bc44f,
build34. Experiment receipts retain that original HEAD plus exact dirty
source/class hashes; a later coherent commit packages this evidence without
rewriting history. All programme changes are attributable to this work; preserve
the known generated tools/__pycache__/ files outside the commit. No user changes
were discarded. No push, deployment, release or native-store promotion occurred.

Final reconciliation verified no production source, application build-definition
or version changes, including no new untracked production source. Research
implementations remain in verification and are excluded from the shipped JAR.

Exact root CODEXLOG_CURRENT.md is absent; do not create it. VERSION_STATE.txt is
unchanged at build34. The initial local version begin failed on environment/capture
before a token/reservation; the SeedV6 noncritical override applied. Subsequent
read-only status was idle, active=null, unfinished=[], blockers=[], verified_bump=false. This remains a non-blocking invocation
warning, not successful finalization. No finish/replacement bump is due.

Human actions required after this prompt: None. Optional production adoption
requires explicit owner authorization and is outside this completed research task.
