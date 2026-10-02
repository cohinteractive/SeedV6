# Headless BRN-2 corpus training

Select `TrainingSource.corpus(root)` and
`withCorpusTraining(new CorpusTrainingConfig(N))` on `TrainerConfig`.
`masterSeed` is the campaign seed; the service fills the durable `viewIdentity`.
Resume with null corpus configuration restores the count. Explicit changes to
count, root or identity fail; existing persisted BRN run seeds retain their
authoritative resume behavior. Use a separate fresh BASIC_V1 lineage. Pinned
corpus lineages cannot switch to generated objectives. No GUI controls were
added; previous defaults, constructors, generation branches and NNUE remain.

## Targets and pipeline

The user explicitly approved BASIC_V1 full-position corpus supervision in this
unit: `target = side-to-move CP / 32511`, using its established public centipawn
scale, combined tanh output, half squared error and online Adam. No empirical
calibration, new mate scalar or optimizer adjustment is applied. Legacy NONE
models fail closed; generated WDL/NNUE-blended supervision remains unchanged.

White labels are negated exactly once for Black to move. STM labels retain their
sign. The existing canonical encoder reflects Black's ranks and makes STM “us”;
it does not negate targets. Mate, source-defined perspective and CP outside
[-32511,32511] are excluded deterministically. Schema 1 disallows missing/unknown
target kinds. Unknown rule-50 clocks explicitly use zero; known clocks retain
Seed's 127 saturation. Fullmove is one. `CorpusPosition.toBoard` reconstructs
the six-long board/key directly without training-time FEN parsing.

The adapter retains the strict WDL `Sample` wrapper (zero WDL slot) and supplies
the actual bounded CP label through the existing explicit target selector, already
used by NNUE blending. The shared sample invariant and old callers remain intact.
`Brn2SelfPlayTraining.trainSamples` freezes those targets before optimization;
`Brn2Trainer -> Brn2Workspace -> Brn2Features` encodes features on demand.

## Fixed view and scalable ordering

Schema 1's latest-label pointers are mutable and have no stable dense ordinal.
`CorpusReader.forEachLocated` streams its unique logical transactional snapshot
once. `CorpusView` retains eligible physical addresses in an **unshuffled disk
index**, two longs (16 bytes) per record, copying no positions, targets, features
or shards. This supports random addressing and preserves original labels after
catalog upgrades. No corpus schema change is needed.

`corpus-training/view.json` binds original manifest/provenance, exclusion counts,
policy, shard IDs/names/counts/hashes and index hash. `campaign.json` binds root,
seed, count and descriptor SHA-256. Index and pinned shard hashes are verified
once per service open. Compatible appends/upgrades preserve the view; missing
or altered index/shards fail. Admission/startup hashing is linear disk work;
generations never rebuild the index. Interrupted admission fails closed with
evidence preserved; restore an intact pin or use a new empty campaign.
Normal optimizer safe-stop/resume remains supported.

Memory is O(shards) metadata, two small buffers, eight shard channels maximum,
and O(requested examples) buffering consistent with the current trainer. No
corpus-sized Java shuffle, corpus-index array or position/feature cache exists.
Index overhead is 1.6 GB per 100 million eligible records. Initialization and
random-I/O throughput at that size remain unmeasured.

`CorpusPermutation` uses versioned six-round balanced Feistel over the next
power-of-four domain and cycle-walks into `[0,size)`. Fixed unsigned 64-bit
arithmetic, mix constants and seed/epoch keys are reproducible across JVMs;
golden vectors lock the algorithm. The domain is below four times size, giving
expected cycle-walk work below four applications. Memory is constant. Arbitrary
sizes through the positive signed-long range are supported.

A fixed seed-dependent global permutation reserves `max(2,ceil(eligible/5))`
identities for validation. At least four eligible identities meet the current
two-example minimum in each partition. Each stratum has an advancing epoch
permutation. Generation `g` consumes the contiguous training range `(g-1)*N`
through `g*N-1`, with no repeats inside a training epoch. Rollover derives the
next permutation from seed plus epoch; large generations cross epochs. Holdout
uses `max(2,ceil(N/4))` records and a separate cursor. Fixed disjoint identities
prevent leakage even across epochs. Corpus has no game identities: these are
position partitions, never invented games. Position correlation remains a risk.

## Lifecycle and headless evidence

Corpus mode branches before either generator, trains its ordered batch through
the existing unshuffled online pass, publishes normal Candidate, compares
persisted Candidate/Best on identical pinned CP holdout, records durable evidence
and history, then settles through `CandidateLifecycle`. **Strictly lower loss
promotes; ties retain Best** is unchanged. No matches/self-play run. Generation
numbers, Latest Training parent, payload codecs, promotion protocol, pruning and
partial optimizer cursor remain unchanged.

Immutable `generation-G.json` receipts bind root/view/seed/count/generation,
ordered training/holdout SHA-256 and examined/usable counts. Selection hashes
cover ordered view ordinals plus exact record bytes; replay must match. Exclusions
occur at admission, while generations read only eligible records (zero additional
skips). Logs distinguish these counts and report optimizer/loss/parameter results.
History explicitly uses `CORPUS_CP_LOSS` and zero generated games.

The developer runner is not shipped. Fresh mode requires a separate empty output:

```powershell
.\gradlew.bat :app:brnCorpusTrain --no-configuration-cache -PcorpusRoot="E:\SeedV6-Corpus\corpus" -PcorpusTrainingOutput="C:\Temp\SeedV6-corpus-smoke" -PcorpusTrainingArgs="--seed=71 --positions=2000 --generations=1"
# A separate JVM replay, without optimization:
.\gradlew.bat :app:brnCorpusTrain --no-configuration-cache -PcorpusRoot="E:\SeedV6-Corpus\corpus" -PcorpusTrainingOutput="C:\Temp\SeedV6-corpus-smoke" -PcorpusTrainingArgs="--seed=71 --positions=2000 --replay=1"
```

`--resume=true` continues the pin. `--rate` selects the existing fresh-only rate
(default .001), with no strength recommendation. A two-minute service limit
bounds the runner; it checks lifecycle completion, zero generated games, Adam
updates, finite loss, changed persisted weights and history. No strength claim.
