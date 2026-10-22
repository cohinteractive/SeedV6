# Conventional NNUE CGLHW

Established 22 October 2026 (Pacific/Auckland), revision 3. Authority: the owner's
new conventional NNUE workstream request of that date, subordinate to applicable
repository and higher-level instructions. Read STATE.md before acting. This is
separate from `../nnue-cglhw`, BRN architecture and suspended BRN successor work.

## Objective and scope

Develop a strong, practical conventional NNUE: local Bullet GPU training,
reproducible data/checkpoints, correct quantized Java CPU inference, incremental
search integration and controlled complete-engine evidence. Conventional NNUE is
the intended primary evaluator; promotion requires evidence, not this declaration.
Preserve the working frozen Pair-2 Gen32/Policy 4 fallback and HCE. No historical
Seed feature-parity objective, novel neural architecture programme, BRN/BRE
reactivation, research cleanup, paid/cloud services, push or deployment is included.

The repository is the technical authority. External implementations supply evidence
and interoperability specifications, not architectural authority. Preserve all
existing research, data, network bytes, failed experiments and acceptance boundaries.
Search Phase 3 remains independently governed; do not change its policy/defaults,
claim its pending construction accepted, or absorb concurrent work. Shared evaluator
integration is permitted after checking ownership and targeted compatibility.

## Engineering and research invariants

- Select one compact, conventional Bullet-compatible architecture, initially dual
  king-bucketed piece-square accumulators, SCReLU, simple head and integer inference.
  Dimensions and optimizations are evidence-supported reversible choices, not
  owner-mandated constants. Retain independent full-rebuild and cross-language
  export/inference references. Reject any training/inference disagreement.
- Explicit feature orientation/indexing, bucket/mirror refresh, output scale,
  quantization, rounding, overflow bounds and versioned payload identity. No silent
  reinterpretation of old NNUE or material-bootstrapped models.
- Verify capture, promotion, castling, en passant, king/bucket/mirror changes, null
  move, undo, reset, qsearch transfer, network mismatch and independent worker state.
- Prefer established high-quality data. Record source/license, label meaning,
  perspective, score scale, filters, sample identity and train/validation separation.
  Related-game leakage and unknown provenance are limitations, not silently assumed
  absent. Do not fabricate outcomes for evaluation-only data. Reuse existing data
  read-only where suitable; large self-play generation is not a prerequisite.
- Checkpoint continuation includes optimizer, schedule, data/order and randomness;
  loading weights alone is not exact resume. Record the actual reproducibility level.
- Loss, inference speed, search speed/nodes and playing strength are distinct.
  Strength uses fixed comparable settings/hardware, frozen openings, reversed colors,
  controlled clocks and uncertainty at the paired-opening level. Tiny samples are
  screening only. No claim of gains from lower loss or fixed-node results alone.

## Execution, resources and authority

Autonomously inspect, research, implement, safely configure/install local tools,
perform focused validation, bounded training/experiments and make attributable
coherent commits. Recheck Git ownership before edits/commits. No destructive Git
operations. Follow root AGENTS.md: build increments once in a qualifying application
commit, never for uncommitted work or documentation; no finalizer. Raw output lives
in ignored `raw/` or build paths; track compact manifests, code and findings.

Default expensive execution: one workload, one engine/search worker, one active
match, at most two data-loader/build workers, low priority where available, bounded
heap/queue/batch sizes. Inspect available CPU/RAM/VRAM/disk. No all-core saturation.
Measure before optimizing. Start with bounded batches and a manageable data sample;
expansion needs decision-relevant benefit. No huge speculative downloads.

Owner's overnight continuation steering (22 October2026) authorizes substantial
local GPU training, dataset processing, benchmarking and justified conventional
architecture refinements across successive work units without routine approvals.
Higher GPU utilization is acceptable while the PC is available overnight; respect
VRAM, stability/thermal limits, bounded CPU/RAM/disk use and limited tournaments.
At least eight hours is available, not a deadline or automatic morning shutdown.
NORMAL persists beyond that interval until explicit owner control changes it.
The first256-unit architecture is not a constraint on evidence-supported alternatives.

During active long operations report observed progress to the user at least every
15 minutes (host instructions may require more frequent updates). Use observable,
bounded asynchronous processes and persistent logs/checkpoints; the active tool
session can poll while reporting. No unattended campaign may outlive this reporting
arrangement. If it cannot be maintained, do not launch; request an alternative.

### Owner compute permission (22 October steering)

The owner controls expensive computation independently of the research direction.
At every fresh conversation/resumption, run `nnue-control.cmd status` and reconcile
actual activity, checkpoint evidence and STATE.md. All CGLHW GPU training, dataset
processing, tournaments, substantial builds/tests and CPU validation must run
through `nnue-control.cmd train` or `run`. Alternate state directories are only
for isolated validation, with actual compute supervised by the real owner control.

- **NORMAL:** ordinary authorized autonomous work under existing resource limits.
- **COMPUTE HOLD:** deny new expensive operations; finish a prompt safe boundary,
  release owned resources, then do useful lightweight work. When none remains,
  preserve state and end execution without waiting/polling/token consumption.
- **FULL PAUSE:** finish a safe boundary, preserve checkpoints and workstream/Git
  state, clean up owned processes, then end execution until explicitly resumed.

Initial integration preserves NORMAL authorization. Subsequently missing, corrupt
or inconsistent state is never fresh authorization. Holds persist across sessions,
process/Windows restarts until explicitly released by the owner. An authorized
NORMAL window may expire into COMPUTE HOLD, never automatically renew. Do not
infer release from task needs, an idle GPU, elapsed time or a new session.

Operational permission resides separately in this worktree's Git metadata.
COMPUTE-CONTROL.md defines the local interface and recovery. Conversation controls
map to those same commands; the owner can use them independently while Codex is
busy. Stops remain latched across racing NORMAL requests. Report a hold completed
only after no active operation and zero owned job processes are observed. Never
infer ownership from GPU utilization, executable names or PID alone, or terminate
unrelated applications.

Use bounded single-superbatch training processes with verified weights, AdamW
momentum/velocity, immutable recipe/data, schedule settings and progress. Weights
alone are insufficient. GPU continuation is not promised bitwise deterministic.
Measure safe-boundary latency; do not promise unverified interruption bounds.
Default hold timeout is 30 seconds, then emergency termination of only the owned
Windows job. This policy is not a hard real-time Windows/driver guarantee.
`hold --now`/`pause --now` request immediate owned-job cleanup. Preserve partial
outputs and disclose lost uncheckpointed work. Resume from the last verified
checkpoint, never a partial write. Supervisor crashes close kill-on-close jobs;
reconciliation requires explicit continuation. No service, permanent scheduler,
process suspension as a hold mechanism, admin change or global installation.

On explicit continuation, reverify evidence, skip completed segments/games and
resume the established autonomous loop. Restart training from scratch only when
technically necessary and documented. Compute control does not replace user-facing
15-minute progress reports; logs alone are insufficient. If reporting cannot be
maintained, do not start affected unattended work without an agreed alternative.

Owner approval is required for research-direction departures, consequential system
changes, admin/reboot/driver installation, paid/external compute, unresolved licensing
decisions, destructive/high-risk changes, protected research reactivation and materially
larger resource commitments. Stop affected work for missing dependencies/permission,
unreconciled changes, contradictory correctness evidence or required unavailable
validation. Continue genuinely independent admissible work; do not turn a blocker
into an implicit waiver. Report precise human actions and dependencies.

## Evidence gates and stopping

**A Operational:** actual local GPU training and optimizer continuation; valid data
pipeline; independently verified export/import and Java full/incremental inference;
explicit engine selection and lifecycle checks. No strength claim required.

**B Stable for Search Phase 3:** frozen reproducible checkpoint plus resource/cost,
runtime/load/search reliability and bounded fixed-time strength evidence with limits.
A weaker checkpoint stays opt-in beside the verified fallback. B need not await C.

**C Competitive:** meaningful complete-engine improvement supported by prospective
paired confirmation and stated uncertainty against the frozen baseline. A validation
or test set used to select candidates is not independent confirmation.

State tracks dependency milestones and the next admissible action; no separate
frontier is required. Each unit updates facts, decisions, evidence, failures and
resume instructions. Challenge the programme before fixing choices. Do not stop at
bootstrap. Diminishing returns ordinarily stop a particular experiment/configuration
or technical direction, **not the CGLHW**. On rejection, record what the evidence
shows and leaves unexplained, assess justified alternatives, select the strongest
next hypothesis, and execute it. Do not repeat an unsuccessful recipe without a
reason or create arbitrary experiments just to fill time. Completion of a work unit,
training run, milestone, short batch or elapsed interval never closes the programme.

Programme-level stopping is limited to genuine completion of the governing criteria;
a blocker requiring human input with no independent admissible work; a material
authority/safety boundary preventing further work; a broader evidence-supported
assessment that no reasonable in-scope approach remains justified; or a real
execution-environment/session limitation. Poor early candidates alone do not meet
the broader-assessment condition. Continue while productive admissible work remains.
If the execution environment interrupts progress, preserve exact restart state and
report interruption, not acceptance or strategic completion. Closure must identify
unmet criteria and preserve the best verified result. Technical completion, owner
acceptance and operational deployment remain distinct.
