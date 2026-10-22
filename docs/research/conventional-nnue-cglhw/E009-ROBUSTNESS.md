# E009 conditional longer-clock confirmation

**COMPLETED:86W/33D/9L,no caps; statistical and full audit gates passed.**
The prospective protocol below is preserved; final evidence is at the end.

Prospective plan,23 October2026 Auckland, before any E009 games. Start only if the
completed E008 fixed128-game confirmation passes its frozen statistical gate and
full evidence audit. This is an independent longer-clock check of the selected
complete-engine candidate, not an extension of E007 or E008's finished endpoint.
If E008 fails/inconclusive, preserve this unstarted plan and select justified work
from that result. No candidate/model/search tuning is permitted within E009.

## Question and fixed comparison

Does the exact E00764/eight-head/vector/selective research candidate improve over
the frozen Gen32/Policy4 baseline when each move has1000ms rather than200ms?
This checks whether short-clock gains survive increased search time and reduces
the relative influence of JVM startup/JIT. It does not establish tournament-clock,
multiworker,external-engine or cross-platform strength.

Use unchanged build64 jars recorded in e007-confirmation-runtime.json, unchanged
e008-classes recorded in e008-verification.json, and exact network
raw/e007-evaluation64/network.cnnue. The E008 protocol remains the composition
reference. No new training or unrelated search changes enter this comparison.

Freeze64 new color-reversed opening pairs/128 games from the same checksum-pinned
legal16 book, using prepare_confirmation_openings.py,seed2026102303. Track
e009-confirmation-openings.json and its selection manifest before launching games.
Exclude every inspected tracked prior root, including both E007 selections and
E008; explicitly verify disjointness from their actual confirmation roots. Use
uniform selection without evaluator/outcome filtering. Unknown training/historical
overlap and related opening-family dependence remain limitations.

One engine worker,32MiB hash,no book/ponder,one active game,1000ms per move,
800-ply cap censored,formal endings/claimable draws only. No score adjudication.
Complete all128 games except owner controls or genuine correctness/process/resource/
safety failure. No interim significance stop, optional extension or capped-game
imputation. Interrupted current games restart; completed games are retained under
the exact frozen protocol. Future harness invocations include the tested atomic
completion-ledger publication from3fcc375.

## Information value and endpoint

The completed E00764-pair confirmation had paired score SD.318, giving an estimated
95% half-width~.078 at64 pairs if that variability recurs; this is a planning
assumption, not a promised interval. Resolving a material gain at a longer clock
can change candidate/handoff readiness. At observed short-clock throughput, allow
roughly2.5..4 hours for128 games; actual game lengths/search timing may differ.
NORMAL has no expiry; this estimate is not a deadline or automatic compute hold.
Use one active workload, bounded heaps and continuous owner-facing progress reports.

Analyze only the predeclared final endpoint with analyze_confirmation.py: full
legal PGN/history/color/outcome/detail replay and recheck every frozen network,
opening,jars,baseline and launcher-class hash. Report paired mean/descriptive Elo,
10000-resample paired bootstrap95% and paired normal95% (1.96*sampleSD/sqrt64).
The longer-clock statistical gate requires all128 uncensored,mean>.5,and both
lower score bounds>.5. Intervals assume the sampled opening pairs adequately
represent this test distribution; they are not universal Elo guarantees.

E007/E008 selection and earlier development games are excluded from E009 analysis.
Do not pool clock conditions or erase earlier failed/inconclusive candidates.
The frozen E009 result is independent evidence for the already-selected composition;
no programme-wide familywise error or unique architectural causation is claimed.
Positive results support a frozen candidate handoff, subject to correctness and
resource evidence. Inconclusive/negative results restrict the strength claim and
prompt diagnosis/next justified work, never automatic programme termination.
Production selection and Search Phase3/Policy4 acceptance remain separate authority.

## Exact run/resume

From the repository root, after opening selection and frozen identity checks:

```powershell
.\nnue-control.cmd run --name 'E009 fixed128-game longer-clock confirmation' --resume-note 'Keep build64/E00764/e008 classes and E009 openings frozen; resume exact options with --resume' --log docs/research/conventional-nnue-cglhw/raw/e009-confirmation.log -- build/conventional-nnue/venv/Scripts/python.exe -B tools/conventional-nnue/paired_match.py --network docs/research/conventional-nnue-cglhw/raw/e007-evaluation64/network.cnnue --selective-research-classes build/conventional-nnue/e008-classes --vector --confirmation --openings docs/research/conventional-nnue-cglhw/e009-confirmation-openings.json --output docs/research/conventional-nnue-cglhw/raw/e009-confirmation --movetime-ms 1000 --max-plies 800
```

For an interrupted existing protocol, use the same options plus `--resume` and a
fresh controller log path. Reconcile persisted control/processes first. Never
silently restore NORMAL or rebuild the frozen runtime during unfinished games.

## Activation and frozen inputs

E008 completed84W/26D/18L,no caps,gate passed; full game/artifact audit and14-root
depth5 independent-inference comparison passed. Conditional activation is satisfied.
The64 new roots are legal,nonterminal and explicitly disjoint from actual E007 and
E008 confirmation roots. Seed2026102303;40462 source games,28862 recorded prior
roots,12203 excluded source positions,28259 available. Selected opening file SHA256:
c715626081a42a353d0071f1aaafbd91143b013dea747b38387bbeeb7dd7291f.
Selection manifest records every exclusion source/hash. Commit these inputs before
the first E009 game. This activation changes no candidate or inferential rule.

## Completed endpoint and qualification

86W/33D/9L,128 games/64 pairs,no caps,in11522.84s. Paired score.80078125,
descriptive Elo+241.67. Paired bootstrap95% [+187.25,+303.47] and paired normal95%
[+188.19,+307.46]; both frozen lower bounds exceed zero. The predeclared longer-clock
statistical gate PASSED. Do not pool this result with E008 or infer a significant
clock interaction from their different point estimates/opening samples.

All128 games passed full legal replay,opening histories,colors,move details and
outcome checks; all network,opening,jars,baseline and launcher-class hashes passed.
95 checkmates,32 threefold draws,1 insufficient-material draw. No failed pending
ledger writes remained. Read e009-confirmation-results.json and preserve
raw/e009-confirmation plus its audit log. This is a complete-engine research result
on the specified hardware/clock/worker/opening distribution,not universal Elo or
isolated learned-weight causation. All owned match processes exited.

The subsequent bounded UCI smoke using the exact frozen runtime copies PASSED:
model/schema/vector identity,legal searches,stop/readiness/newgame/repeated search/
quit,and jar hashes matching the tested runtime. Source,command,transcript/log hashes
and results are in candidate-frozen-uci.json. The added --runtime-lib option is now
runtime-verified. Controller status after completion showed NORMAL generation3,
no active operation and zero owned processes; this historical observation never
overrides a later owner hold.

Together with the established training/resume,parity,resource and search correctness
evidence,the contract's A/B/C technical gates are satisfied in the tested scope.
The frozen candidate is ready for separately governed Search Phase3 evaluation.
See CANDIDATE-HANDOFF.md and candidate-handoff.json. This technical qualification
does not change production defaults,confer owner acceptance,or authorize unrelated
Search work. No required technical check remains for this scoped handoff; the full
repository suite,GUI packaging,other platforms,selective multiworker and external-
engine/tournament-clock strength were not validated and are not claimed.
