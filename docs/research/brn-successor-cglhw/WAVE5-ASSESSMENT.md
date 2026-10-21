# Wave 5 trained assessment

21 October 2026. Existing CGLHW, contract revision2. G5 is **complete and verified**;
no programme success or production adoption is established. This root began clean
at b43b761/build55 at observed02:31:57 UTC. No root journal exists; no finalizer used.

## Verified training

All four sessions completed32 additional generations64->96 with no history warnings,
unsettled reservation or partial state. All133 retained checkpoint/optimizer pairs
(four times33 plus frozenGen32) pass checksum/codec checks and24-board integer
runtime/state parity. Exact source bootstrap payloads, compact parameter/moment
projection and full-control payload equality remain intact. Every new parent link
and8192-step increment agrees. Final steps786432(cubic),2539520(BRE). Recipes are
the prepared .001/.003, batch128,8epochs,seed1,one worker,131072 train/32768 validation.
Each adds4194304 train examples,1048576 validation examples and33554432 visits.

| Lineage | Latest | Best | Promotions |
| --- | --- | --- | --- |
| W5-RESCUBIC-R4-LR001 |96|93|17|
| W5-RESCUBIC-R16-LR001 |96|95|15|
| W5-BRE-CONTROL-LR001 |96|95|18|
| W5-BRE-CONTROL-LR003 |96|94|11|

Full IDs/hashes, ancestry, rates, steps, histories and cursor endpoints are in
[trained checkpoints](wave5-trained-checkpoints.json). Read-only Java audit evidence:
`raw/wave5/trained-audit-01`; pre-task owner inventory:`raw/wave5/trained-before-01.json`.
All four stores were copied to distinct `raw/wave5/trained-frozen-01` roots before
evaluation. Original owner models are never training or match outputs.

R4/R16 have identical inherited96 reservation ranges and hashes; the two BRE rates
also match each other. BRE versus cubic data/partition/optimizer differences remain.
R4 validation loss exceeds R16 in all32 matched generations (mean difference.00267273,
last-eight.00245233). BRE.001 beats.003 in all32 (mean difference-.00355518).
Temporal validation means rise for all four arms, but generations use different
examples; this alone cannot establish overtraining. Best is sequential comparison
against an incumbent on fresh validation, not a global minimum on a common panel.
GUI WDL loss remains half-squared target error; optimizer uses its established WDL
gradient objective. Nominal loss values must not be called playing strength.

Consumed fixed panels (2048 BT4+2048 CP) support partial R4 fitting recovery:
with half correction and normal quarter-residual mapping, second-half BT4 CE
.781416->.779777 and CP1.338620->1.331401 from64->96. R16 moves.780619->.779753
and1.330306->1.330501 respectively. Thus R4 closes most of this panel's gap, while
the fresh per-generation validation still favours R16. No independent generalization
claim: these panels have already influenced research, and historical base overlap
is unresolved. Independent Python CE reproduction error<2e-15;21 CP records have
standard-board status flags, preserved without retrospective filtering. Details:
`raw/wave5/trained-predictions-02`, `raw/wave5/prediction-reproduction-01.json`.
The first prediction invocation used a nonexistent CP filename and stopped before
prediction output; its log/directory are preserved, with a separate corrected run.

## Prospective primary strength screen

[Admission](wave5-primary-admission.json):256 fixed development games,128 each at
32768 nodes and200ms/move, one game/one worker, no ponder,64MiB TT,768MiB Java,
one active processor, BelowNormal, max12games/240seconds per resumable batch.
All eight actual Latest/Best endpoints face frozenGen32,16 games each per condition.
Calibration is the prior-established .5 learned change for every cubic/BRE candidate;
no averaging or per-candidate loss tuning. Common QXRAY search, warmed clock starts,
512-ply safety caps counted as failures, no legal fallback. Actual cache parity is
required before games. Resource/receipt wrappers preserve every completed game.

The first8 pairs of32 newly frozen development roots are used. Partition0 sampling
seed2026102105 excludes all prior W3/W4 selected positions; partition1 confirmation
remains unused. Fresh final positions do not establish independent opening families.
The remaining24 pairs are available for separately admitted focused follow-up.
The narrow `.gitignore` exception is only this frozen opening INPUT, not game output.

Complete the fixed budgets without optional favourable-score stopping. Report paired
descriptive uncertainty and failures; no final success follows from this screen.
Use equal-node and equal-time results jointly to select discriminating direct
R4/R16, continued-BRE and additive/quadratic controls. Prefer Latest for equal-exposure
controls when endpoint differences are inconclusive. No next-wave architecture or
training gate is predetermined. G5's pause sentinel is preserved as
`raw/wave5/g5-completed-pause.txt`; no additional human action is currently required.
