# Conventional NNUE candidate handoff

23 October2026 Auckland. **E00764/vector/E008 selective is a research candidate
with confirmed200ms complete-engine improvement. E009 independent1000ms validation
is running. This is not final owner acceptance,production promotion or authorization
to change the independently governed Search Phase3 programme.**

## Candidate and evidence

Use the exact3,940,952-byte S6CNU002 export at
`raw/e007-evaluation64/network.cnnue`, file SHA256
`8431626c2fca81e0a1e9ccd5e45ddd8829e6e3282c756c21fad75b06d075bb2b`.
`candidate-handoff.json` records the selected and latest preserved checkpoints,
trainer,recipe,runtime/class hashes and evidence references. Paths under `raw/`
in this document are relative to this research directory; build/tool paths are
relative to the repository root. Local ignored artifacts are not in a fresh clone.

The network has256 hidden units per perspective,ten mirrored king input buckets,
SCReLU and eight material-count output heads. Factorized training inputs are merged
for inference. The versioned loader retains exact S6CNU001 compatibility. M003's
optional Java Vector API head preserves scalar integer arithmetic and overflow
bounds; the measured machine uses vector-256-long. Java CPU inference needs no CUDA.

E005 changed the teacher/distribution to established BT4/T80 with verified inverse-Q
targets. E006 expanded coverage to33,554,432 training positions with524,288 diagnostic
held-out positions. E007 added conventional material heads. These developments
improved data fit but did not by themselves establish a complete-engine strength
gain. E00764's vector/unpruned independent128-game result was42W/42D/44L, paired95%
Elo roughly-60..+49. Preserve that inconclusive result.

E008 holds the network bytes fixed and uses the explicit research composition
QXRAY/quiet-LMR/WINDOW-LMP/EFFORT/completed-hints. The same composition call appears
in the frozen Gen32 one-worker production factory; this does not automatically
transfer its empirical authority to NNUE. A same-network development comparison
favored this composition9W/5D/2L. Fresh independent E008 confirmation then finished
**84W/26D/18L**, no caps, at200ms/move,one worker,32MiB hash: descriptive+198 Elo,
paired bootstrap95% [+143,+264] and paired normal95% [+142,+266]. The frozen
statistical gate and full game/artifact audit passed. This is a complete-engine
replacement result, not a claim that a learned-weight change alone explains it.

Read E008-CONFIRMATION.md and e008-confirmation-results.json. E009-ROBUSTNESS.md
freezes the separate1000ms,128-game fresh-opening test; do not pool its clock
condition with E008 or use interim results as acceptance.

## Correctness, cost and scope

Independent Python/Java/pinned-Bullet feature and integer parity passed259 positions
covering all eight heads and special moves. Scalar/vector results are exact; actual
UCI stop/readiness/newgame/search/quit and one/two-worker ordinary NNUE checks passed.
The selective research route supports **one worker only**. A separate14-root depth5
check matched independent full-rebuild scores,moves,PV,nodes/qnodes and selective
provenance, extending beyond depth3's inability to reach non-root LMR. This verifies
evaluator state under that search, not mathematical exactness of pruning.

Measured combined incremental/head inference was~.72us with vectors versus~1.7us
scalar,zero hot allocation. Identical-root/score/node search throughput improved
~1.40x over the same NNUE's scalar backend. Baseline controls varied6..8%; these
measurements are hardware/JDK specific and are not Elo estimates. Runtime evidence:
M003.md,m003-integrated-evidence.json,m003-build64-uci.json,e008-deep-verification.json.

The strength sample uses full legal16-ply prefixes,color reversal,formal/claimable
draws and no score adjudication. Caps are censored. Related opening families and
unknown overlap with training/historic games remain limitations. No tournament-clock,
multiworker selective,cross-platform,GUI packaging or external-engine strength claim.
The existing Gen32/Policy4 and HCE fallback paths remain unchanged.

## Frozen runtime and verification

The tested application is build64/source6213630. Exact jar copies live under
`build/conventional-nnue/frozen-runtime/build64-e007-confirmation/uci/`; source and
two launcher classes are pinned in e008-verification.json and candidate-handoff.json.
The launcher is `SeedNnueSelectiveResearch` in `build/conventional-nnue/e008-classes`.
It intentionally identifies itself as unaccepted research pending separate policy
adoption. Ordinary application `--nnue` selects the unpruned control, not this candidate.

After the active E009 match ends, verify the frozen runtime copy with the bounded
check below. The new `--runtime-lib` path and this exact handoff command still need
their actual runtime check at this document's creation; do not claim it has run.
Use fresh output/log paths on rerun; preserve previous results.

```powershell
.\nnue-control.cmd status
.\nnue-control.cmd run --name 'Frozen NNUE candidate UCI verification' --resume-note 'Rerun bounded smoke to fresh output/log paths; no training involved' --log docs/research/conventional-nnue-cglhw/raw/candidate-frozen-uci.log -- build/conventional-nnue/venv/Scripts/python.exe -B tools/conventional-nnue/uci_smoke.py --network docs/research/conventional-nnue-cglhw/raw/e007-evaluation64/network.cnnue --selective-research-classes build/conventional-nnue/e008-classes --runtime-lib build/conventional-nnue/frozen-runtime/build64-e007-confirmation/uci --vector --workers 1 --output docs/research/conventional-nnue-cglhw/raw/candidate-frozen-uci
```

All match starts/resumes must use the exact frozen protocol,including installed
paths; do not replace an in-progress match's classpath with the copy above. Its
hash-checked copies are for independent later reproduction. Recompiling the tiny
launcher from tracked source is possible with JDK21 and the frozen jar classpath;
check resulting identity and compatibility rather than assuming byte-identical
compiler output across JDK builds. No system-wide Java/CUDA change is required.

## Checkpoints and continuation

Selected checkpoint64 has268,435,456 total training exposures and held-out sigmoid
MSE.016816835. Its receipt,config,weights,AdamW momentum/velocity and tensor geometry
were reverified for this manifest. The frozen trainer is
`build/conventional-nnue/frozen-trainers/f1b9c5d05a41c1a23a4a6d7555bcfae31b5f7d076549a2cfe08283b8efacbdea.exe`,
Bullet pin579b78b37a694cacd6e9ede4706386c15c34837c. Learning-rate schedule,data cursor,
seed,batch shape,output geometry and dataset hashes are immutable recipe fields.

Training continued through256,1,073,741,824 exposures; that checkpoint and optimizer
are also verified and preserved. Its held-out loss plateaued and its same-search
screen against64 was near-tied4W/9D/3L. Selecting64 does not establish256 is inferior
and does not erase or require repeating65..256. E009 is the current incomplete work
unit. Resume its exact command with `--resume` after reconciling control state;
completed games are skipped. Further training requires a justified new plan, not
automatic continuation of an already-plateaued recipe.

Frozen inference/export bytes are reproducible. GPU training continuation preserves
the verified optimizer,recipe and progress but is not promised bitwise deterministic.
The controller keeps safe superbatch boundaries and a30-second graceful-stop policy,
then terminates only its owned Windows job if necessary. Actual E007 GPU hold took
2.61s with the CUDA process gone; this is a measurement,not a hard interruption SLA.

Use `nnue-control.cmd normal`, `hold`, `pause`, and `status` from another terminal
even while Codex is occupied. `hold --now`/`pause --now` request emergency owned-job
cleanup and may lose only uncheckpointed work. NORMAL has no expiry until the owner
changes it. Missing/corrupt state is not authorization. Read COMPUTE-CONTROL.md for
crash recovery and resource-release evidence. No action on unrelated processes is
permitted. The production evaluator fallback remains available throughout.
