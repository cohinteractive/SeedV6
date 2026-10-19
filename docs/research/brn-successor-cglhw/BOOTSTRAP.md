# Bootstrap and challenge, 20 October 2026

Initial clean Git HEAD: `38e5626a6bfb76690a539d0ce0e04e70bd57f9d1`, build 40.
Exact root CODEXLOG_CURRENT.md absent; do not create it. VERSION_STATE.txt present;
current owner/root policy prohibits finalizers. First successful clock observation
was 2026-10-19T23:19:09Z, after initial read-only discovery (the first PowerShell
Get-Date -AsUTC attempt failed). No journal capability is active.

## Verified inputs and reuse

The tracked Gen32 payload has the requested SHA-256 and 795032-byte identity.
Its manifest binds the full historical checkpoint and lineage. Runtime availability
does not establish availability of the original optimizer checkpoint; verify the
separate store before claiming that. ProductionModel is the hash-checking loader;
BrnPair2Model/SearchEvaluation preserve quarter-residual STM mapping. The current
Search Contract records Policy 4 as production for Pair-2 only. Ordinary other
families do not inherit it; naïve GUI head-to-head is not evaluator-isolated evidence.

Existing module owners, TrainingArchitecture/NetworkModel/NetworkTrainingState,
CheckpointStore, ModelLibrary, TrainingLineage, CorpusTraining, IncrementalTraining,
TrainerService, Swing Training/Play/Arena and source readers are reusable. No second
model registry or external checkpoint-copy requirement is needed. Runtime payloads
cannot reconstruct optimizer state; new codecs must independently verify snapshot
identity and exact continuation. Research runners remain outside production modules.

Current machine query confirms Ryzen 5 5500, 6 cores/12 logical threads. Java is
`C:/java/jdk-21/bin/java.exe`; exact runtime version and current memory/resources
must accompany measurements. GUI is Swing: native component/window harnesses are
appropriate. Browser tooling is not needed for this desktop integration.

## Prior evidence that changes this plan

The completed `../brn-architecture-cglhw` programme recommended C02. Its B01 energy
head still used a multi-million-parameter relation encoder; C01 triple tables added
large sparse tables; F01 context propagation improved prediction but not playing
strength. These are adverse evidence for those tested recipes, not universal
rejections of all polynomial or conditional mechanisms. Historical raw artifacts
under app/build/research were deliberately removed by the owner on 13 October;
tracked findings remain, and no fresh empirical replication is claimed here.

New hypotheses must change a discriminating mechanism, not rename those experiments.
First wave uses much smaller representations with direct piece-square embeddings
or shared local pair tables; avoids a giant relation encoder and explicit triple LUT.
The small nonlinear baseline is deliberately conventional and labelled as a control.

## Initial wave hypotheses (prospective, not results)

| ID | Formulation | Added relationships and chess motivation | Expected cost and limits |
| --- | --- | --- | --- |
| W1-GATE | Four context-gated shared local-pair experts plus learned unary terms | The value of adjacent occupancy/piece patterns changes with remote piece placement; conditional pawn/coordination patterns beyond an additive pair table | About 6.5k double weights; O(210*4 + pieces*4), four softmax gates. Shared translation tables lose absolute-pair detail; unary terms do not fully replace it. |
| W1-CUBIC | Rank16 degree2+degree3 ANOVA interaction model plus unary terms | Distinct triples of globally placed pieces can represent coordination and conditional dependencies without enumerating every triple | About 13k double weights; O(pieces*16), elementary-symmetric recurrence; low rank may underfit. Different from B01's nonlinear relational encoder. |
| W1-POOL | Rank16 pooled tanh control plus unary terms | Nonlinear global occupancy summaries; tests whether generic nonlinear capacity suffices | About 13k double weights; O(pieces*16), 16 tanh calls. NNUE-like control, explicitly no novelty claim. |

Common initial information: piece type/ownership/square, mover-canonical rank/color
transform and fixed material. No king bucket, explicit attack, pin or move-history
features. This isolates mechanisms but feature poverty may dominate: record failures
without concluding expressive families cannot learn chess. Future sparse geometry/
blocker features require an information audit and matched ablation, not an assumption.

Start with independent replacements, zero output residual over material; no Gen32
teacher at inference and no need for its optimizer. Existing CP/WDL or BT4 Q sources
teach external targets rather than imitate Gen32 exclusively. Calibrated score gain
is versioned and visible; a starting .25 gain is a hypothesis, not evidence that
Gen32 calibration transfers. Gains can be tuned on development only, frozen before
independent comparisons, with equal opportunities across candidates.

All three admit incremental sufficient statistics mathematically; deterministic
full recomputation is the initial correctness reference. Do not adopt a drifting
floating rolling cache without measured parity and a bounded/rebuild strategy.
Inference cost needs measurement and eventual equal-time games. Training can take
longer than inference optimization would suggest; the user owns that work.

## Primary literature consulted

- [Blondel et al., Higher-Order Factorization Machines](https://arxiv.org/abs/1607.07195):
  efficient ANOVA interactions motivate W1-CUBIC; their non-chess tasks establish no
  SeedV6 strength or Java speed result.
- [Polynomial Networks and Factorization Machines](https://proceedings.mlr.press/v48/blondel16.html):
  tensor factorization perspective distinguishes interaction order from a dense MLP.
- [Stockfish NNUE documentation](https://official-stockfish.github.io/docs/nnue-pytorch-wiki/docs/nnue.html):
  sparse inputs, incremental sums, quantization and accumulation drift inform cost
  and correctness checks. W1-POOL is a control, not novel because its inputs differ.

## Challenge outcomes and remaining gates

Do not repeat historical serious-training scripts: they exceed current authorization.
Implement/test the consolidated wave first, then create actual managed GUI lineages.
Use disposable stores and bounded updates for verification; original stores remain
unchanged. Read actual GUI storage/source settings before seeding lineages. Establish
idempotent creation and persisted recipe/seed identity so fresh sessions can resume.

Equal-time tournament utility remains unverified for these models: startup/JIT,
clocks, common search configuration and opening independence require a prospective
preflight after mature checkpoints exist. One game/one worker/64 MiB/ponder off and
below-normal priority are realistic starting bounds on the observed host; no current
tournament or strength claim. Frozen trained artifacts and exposure records are
required before screening; live Best is never a tournament identity.
