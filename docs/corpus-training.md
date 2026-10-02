# BRN-2 corpus training

In **Network Training → Configuration**, choose a fresh **BRN-2** lineage and
select **External Seed corpus** under **Training source**. Browse to the Seed
corpus root containing its catalog and shards, rather than an incoming archive
or individual shard. Set **Positions / generation** (integer, minimum 2), then
use **Apply settings** or **Start Training**. **Model / run seed** controls
deterministic corpus ordering; there is no separate corpus seed.
The last valid corpus root is remembered across application restarts and offered
for new configurations without a root. A lineage's own saved root takes precedence.

## Import and manage a corpus in the app

Open **Network Training → Corpus**, or use **Corpus Import / Management** beside
the external-corpus controls in Configuration. Choose the downloaded Lichess
evaluated-position **Source archive** (`.jsonl.zst`) and the **Seed corpus root**.
Select an existing corpus to expand it, or an existing empty directory to create
one. The archive, All/bounded choice and bounded count are remembered across
restarts. The directory chooser and successful imports/validation remember the
same corpus-root preference used by training; a lineage's saved root still wins.

Choose **All available source records** to read to the end of the archive, or
**Up to** a positive count to examine that many source records. All uses the
production importer's unbounded setting (`max-records=0`); the bounded count
includes rejected records and duplicates, rather than promising that many new
positions. Click **Start Import**. No command line or Gradle process is needed.
Shard size and CLI progress interval retain the production defaults of 100,000.

An import always streams from the archive's beginning. Reimporting the first
million records before continuing is safe: existing logical positions are
deduplicated, stronger compatible labels are consolidated by the same importer,
and new positions are appended. There is no compressed random-seek resume.
All may run for a long time. Import runs in the background; the rest of SeedV6
remains usable. Status shows examined/accepted/rejected records, added positions,
duplicates, label upgrades, persisted records, completed shards, committed corpus
total, elapsed seconds and throughput. Current-batch counters can exceed what is
committed until the next checkpoint. On failure, completed checkpoints remain
retained; the underlying source/storage/lock error is shown.

**Stop Import** requests a cooperative stop between records and publishes the
current batch through the existing restart-safe checkpoint protocol. It does not
interrupt disk I/O. The stopped summary reports retained data; starting again
rescans and deduplicates. Closing the app also requests a stop and joins through
the existing background shutdown path. Abrupt process termination retains
completed checkpoints and leaves unfinished work to the writer's existing
rollback/recovery rules. Prefer Stop Import before closing during a large import.

**Validate Corpus** runs the production full integrity check off the UI thread:
catalog integrity, shard checksums/records, provenance, pointers and counts.
It reports valid/invalid and position, stored-record and shard counts. Full
validation can take time and has no separate cancellation action. Import and
validation cannot run concurrently in the management panel. Underlying conflicts
with another writer or reader are reported; training locks are not bypassed.

After completion or stopping, the selected training corpus count is refreshed
without restarting SeedV6. New training configurations can use the expanded
corpus immediately. Existing campaigns retain their pinned views; importing does
not change sampling, targets, validation or pinning. To train a new view, use the
existing configuration/new-lineage workflow. GUI and headless CLI use the same
`LichessImporter` and `CorpusReader.validate` implementations.

The background status check reports an absent, unreadable/invalid, or valid
corpus and its catalog position count. It does not scan training eligibility or
hash the full corpus. Normal startup performs admission and pin verification;
the corpus needs at least four eligible CP identities. Candidate validation is
independent: choose **Candidate vs Best game pairs** or held-out loss (using CP
targets). Generated training-game/objective controls are disabled with their
values retained; search and match controls remain available for game validation.
NNUE training is unchanged.

Configuration saves with the selected lineage. The production service pins the
current valid view on first Start. Reload/Resume restores that campaign's root,
count, seed and read-only view identity. Controls are protected during training
and become editable after Stop, with the existing shared BRN run-seed lock retained.
Changing root or count clears the draft pin; Start resolves or creates the binding
for that configuration. Unchanged settings retain their view, including after
catalog appends. Missing/changed pinned shards fail visibly without fallback.
Changed unfinished work restarts from the settled checkpoint, preserving its old
attempt and corpus receipt in the existing restart archive. Settled generations'
history and receipts retain the root, seed, count and view that produced them.

Start restores Best, Latest Training and generation identity before entering
`PREPARING_CORPUS`. Creating a new pinned view examines the entire corpus snapshot;
reopening a view verifies its index and original shard checksums. A small
positions-per-generation setting does not bound this preparation. Large corpora
can take substantial disk time; status reports examined positions or verified
bytes while the recovered lineage remains visible.

Stop cooperatively cancels preparation between records or hash buffers and
removes unpublished temporary files, as do ordinary preparation failures. Published views and
existing partial-generation state remain intact. Resume still checks the durable
generation settings after pin verification; incompatible edits use the existing
archive/restart workflow. Preparation does not start a new generation or generate
self-play positions. These lifecycle rules also apply to NNUE corpus training.

Preparation owns an exclusive OS lock in each view directory (`preparation.lock`),
in addition to the trainer's lifetime lineage lock. The empty coordination file
remains in place; its presence does not indicate an active owner. When there is
no published view or final index, the next owner removes abandoned
`records.idx.pending` and `view.json.pending` scratch and rebuilds from the
snapshot. Partial address indexes are not resumable. A crash between the two
publication moves leaves a completed `records.idx` and `view.json.pending`;
the next owner checks the descriptor and index checksum before completing the
metadata move, preserving the index bytes. An active owner, a published view,
or an unknown/corrupt final index is never overwritten.

Select `TrainingSource.corpus(root)` and
`withCorpusTraining(new CorpusTrainingConfig(N))` on `TrainerConfig`.
`masterSeed` is the campaign seed; the service fills the durable `viewIdentity`.
Resume with null corpus configuration restores the current count. Supply a new
count with an empty identity when changing the corpus selection; an explicit
incompatible identity fails. Existing persisted BRN run seeds retain their
authoritative resume behavior. Generated sources can be selected after stopping.
Previous generated defaults and NNUE behavior remain unchanged.

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
generations never rebuild the index. Interrupted unpublished admission is
reconciled under exclusive preparation ownership. Missing or corrupt published
pins still fail closed with evidence preserved; restore an intact pin or use a
new empty campaign.
Normal optimizer safe-stop/resume remains supported.
The original binding stays in this location. Changed root/seed/count selections
use immutable bindings under `corpus-training/configurations/`; `current.json`
records the current selection. Each binding uses the same view format and sampling
policy. Creating a new binding may repeat admission/index work at startup.

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
the existing unshuffled online pass and publishes normal Candidate. Held-out mode
compares persisted Candidate/Best on identical pinned CP holdout: **strictly lower
loss promotes; ties retain Best**. Game-pair mode runs the existing validation
arena and promotion policy. Both record durable evidence/history and settle through
`CandidateLifecycle`. No training-position games run; validation matches remain
distinct. Generation
numbers, Latest Training parent, payload codecs, promotion protocol, pruning and
partial optimizer cursor remain unchanged.

Immutable `generation-G.json` receipts bind root/view/seed/count/generation,
ordered training/holdout SHA-256 and examined/usable counts. Selection hashes
cover ordered view ordinals plus exact record bytes; replay must match. Exclusions
occur at admission, while generations read only eligible records (zero additional
skips). Logs distinguish these counts and report optimizer/loss/parameter results.
History records the selected validation method and zero generated training games.

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
