# Training Data

Network Training consumes sources sequentially. Lichess needs no import or whole-source
preparation. Stockfish BINP/Zstd sources require a one-time application-managed
preparation into restartable native chunks. Existing converted Seed data remains
readable; downloaded files, checkpoints and histories remain untouched.

## Setup and controls

- **Training Data** selects the position provider and manages the lineage's source
  registrations, locations, format/version fingerprints and allocation weights.
  Add a Lichess evaluations `.jsonl` or PZstandard `.jsonl.zst` file, or an existing
  Seed data directory. A Stockfish BINP/Zstd file or folder is also supported with
  the explicitly confirmed `BT4_Q_V1` label profile. Physical format and label profile
  have separate table columns; BINP bytes never determine a teacher calibration.
- **Configuration** contains training positions per generation, run limits and
  independent candidate validation. Self-generation displays games and samples per
  game. Data-backed runs display search depth, threads and game bounds only for
  game-pair validation. Threads are per search, not concurrent training games.
- **Network** contains architecture-specific settings: NNUE minibatches/epochs and
  BRN learning rates, supervision and related setup. The model/run seed is setup;
  it does not choose source positions. Existing optimizer and run-seed locks remain.
- **File > Training storage settings** owns the machine's base training root.
  The derived lineage path appears in Diagnostics. Architecture and lineage remain
  the top-level context. Active-run editing locks remain in force.

Click Apply settings or Start to save source setup. BINP registration is also saved
before its background preparation starts, so it survives an interrupted preparation.
Removing a registration removes
no source files and does not erase its cursor. Inspect sources reports identities,
readiness, known counts, visited seek points, next-unallocated positions and the
reservation history. Relocation checks the same fingerprint; preserve timestamps.
Legacy directory counts come from existing metadata; prepared BINP counts are known.
Lichess text/archive counts remain
unknown until EOF is encountered; counting the whole file is not a startup task.

## Acquisition and seeking

`SourceReaders` returns architecture-neutral `TrainingPosition` values. Format
knowledge remains in readers; `SequentialTraining` selects targets by architecture
AND source label profile, passing the existing sample/target interface to unchanged
feature kernels and optimizers. Lichess retains NNUE `STOCKFISH_WDL_V1`, BRN-2
`BASIC_V1` and BRN-3 `BRN3_CP_WDL_V1`. BT4 encoded scores use the inverse Q mapping
directly for BRN-3. BRN-2's
legacy NONE material prior remains unsupported for CP training. Self-generated
terminal WDL positions retain their existing provider and trainer path.

PZstandard exports contain independent frames preceded by 12-byte length headers.
The reader opens only visited frames and records sparse logical-record/frame-byte/
uncompressed-byte offsets. Resume decodes at most a nearby frame prefix and skips
nearby JSONL lines without parsing them. Frame boundaries may split a JSON record.
Plain JSONL records sparse byte offsets. Legacy Seed data uses the existing SQLite
`(shard, ordinal)` index with keyset continuation, never an OFFSET scan; checksums
are verified only for shards actually consumed. Sidecars contain navigation data,
not positions, targets or features. They grow incrementally as data is consumed.
Preserve this navigation metadata with the lineage. If required seek coverage is
missing, consumption blocks instead of decoding an unbounded source prefix.

Ordinary Zstandard wrapping native BINP is recognized and prepared as described
below. Ordinary Zstandard wrapping JSONL remains unsupported: the Lichess reader
still requires PZstandard framing for efficient durable resume.

## BT4-T80 registration and preparation

Launch from the repository in PowerShell:

```powershell
.\gradlew.bat :app:installDist
.\app\build\install\seedv6\bin\seedv6.bat gui
```

Choose **Network Training**, select the intended **BRN-3** lineage, and open
**Training Data** with **Training Data sources** selected as the position provider.
Click **Add source...**, select `E:\SeedV6-Corpus\incoming\bt4-t80`, and confirm
**BT4_Q_V1** in the label-profile dialog. This is a single logical source named
`bt4-t80`, format `STOCKFISH_BINPACK_ZSTD`, label `BT4_Q_V1`, default weight **1**.
Both compatible peer shards belong to this row, its one version identity and cursor.
The table shows **PREPARING**, compressed-byte progress, then **READY**. Details
show the final raw-position/chunk counts and prepared directory. The EDT remains
available while a worker prepares. Leave SeedV6 open for the potentially long,
disk-intensive initial preparation. No optimizer or training run is involved.

After READY, **Inspect sources** reports the inventory and readiness; restart
SeedV6 and reopen the same lineage to verify the row remains READY. A successful
full preparation and this restart check are the manual acceptance evidence for
the downloaded corpus. Do not start a BRN-3 training experiment until the source
evidence and intended weights have been reviewed.

Detection gives established legacy `index.sqlite` directories precedence. Otherwise
a shard folder must contain only regular `.zst` files, in lexicographic filename
order. Each candidate must decode a bounded BINP chunk prefix; JSONL/PZstandard,
unrelated files, subdirectories, symlinks and invalid/mixed shards are rejected.
The detector checks at most the first native chunk (maximum 100 MiB, with a 128 MiB
compressed-input cap) and 64 records
per file. Full preparation validates every chunk and continuation; a later corrupt
suffix therefore fails preparation instead of being published as READY.

The shared cache uses existing machine-local application conventions:

- Windows: `%LOCALAPPDATA%\SeedV6-NNUE\prepared-data\<source-identity>\`
- Without LOCALAPPDATA: `~/.seedv6-nnue/prepared-data/<source-identity>/`
- Optional launch property: `-Dseedv6.preparedDataRoot=<absolute-directory>`.
  With the generated launcher, set it in `JAVA_OPTS` before launch. The chosen
  location needs space for the complete recompressed native corpus, which can
  differ substantially from the downloaded sizes. It is independent of a lineage
  and of **Base Training Root**, so unchanged sources share preparation across lineages.

`PreparedBinpack` reads each original once, validates/replays native records, and
compresses each complete BINP chunk (including its original header) independently
with Zstandard level 3. Per-shard files concatenate those independent frames;
`manifest.json` indexes frame offsets, sizes, counts, raw-ordinal starts and full
decoded-chunk SHA-256 checksums. There are no new serialized chess-position records,
network features or transformed labels. The manifest orders chunks round-robin:
shard 0/chunk 0, shard 1/chunk 0, shard 0/chunk 1, shard 1/chunk 1, continuing with
remaining shards when one ends. This is local to the BINP reader; shared sequential
allocation and ledgers have no new interleaving rules.

A seek binary-searches this index, decompresses one nearby chunk and replays only
its preceding records. The work is bounded by a native chunk, never the archive
prefix. Each visited chunk is checksum-verified. Startup reads the manifest,
checks prepared shard lengths and bounded original fingerprints, without hashing
the complete archives/cache. The immutable recipe identity is
`binp-chunks-zstd3-roundrobin-v1`; source identity binds that recipe, explicit label
profile, ordered filenames and each original shard's bounded fingerprint/length.
Preparation is deterministic for unchanged bytes and this recipe.

Preparation holds a per-source OS lock. Payloads are forced in a private `.pending`
directory; the checksummed manifest is published there last, and the complete
directory is atomically renamed into place. A crash cannot expose a partial READY
manifest. Windows has the same directory-force/power-loss limitations as existing
checkpoint publication. Interrupted preparation may have to reread the original
outer frame from the beginning; completed training/resume never does.

Missing, incomplete or invalid caches are NOT READY. **Prepare / retry** checks
existing chunks and rebuilds corrupt/incomplete derived data from unchanged
originals. It cleans application-owned pending files, never originals or cursors.
Disk-full, malformed data and other failures show **FAILED** with retry guidance.
After process interruption, use **Prepare / retry**; ordinary registration of an
already prepared unchanged source reuses it immediately. No automatic eviction is
implemented. Removing a source registration leaves the reusable cache intact.

Adding/removing/renaming a shard, ordinary content edits or timestamp changes
invalidate the registered version. Remove the old registration and add the changed
folder explicitly; it gets a new identity and cursor while historical reservations
remain. **Change location...** is only for the same inventory/fingerprints (preserve
shard names and timestamps). A changed preparation recipe likewise needs a new
source identity; it cannot silently reuse an old cursor.

## BT4 labels, architecture and compatibility

`BT4_Q_V1` means the publisher's signed integer encoding of side-to-move LC0 BT4 Q:

```text
score = round(clamp(660.6 * Q / (1 - 0.9751875 * Q^10), -32000, 32000))
```

`Bt4Targets` builds a 32,001-entry positive lookup once using 56 fixed bisection
steps and StrictMath, then reflects negative scores exactly. Per-record adaptation
is a lookup. Zero maps to zero; score 100 maps to approximately 0.15138. The result
is deterministic, monotonic and bounded in [-1,1]; encoded magnitudes above the
Q=1 endpoint saturate at 1. Encoding quantization limits reconstruction precision.
32002 is skipped, as are other out-of-range labels, but the raw ordinal advances
and its move still reconstructs later continuations. No BT4 score is called CP or
sent through Stockfish WDL calibration.

BRN-3 accepts the direct recovered Q target alongside Lichess's unchanged CP-to-WDL
outcome target. BRN-2 and NNUE explicitly reject BT4; their existing Lichess targets
and optimizer behavior remain unchanged. The UI shows an unsupported status for
BT4 on those architectures. Both UI readiness and headless service acquisition
refuse missing/failed preparation before consuming records.

Source descriptors add `labelProfile` and `shards` to the existing JSON schema.
Old Lichess and Seed descriptors default deterministically to
`STOCKFISH_CP_MATE_V1`, with empty inventory and unchanged version/mix identities.
Generic BINP requires an explicit profile; missing or incompatible bindings fail.
No historical checkpoint is rewritten. Historical BRN-2/NNUE/BRN-3 receipts retain
their original adapter identities and JSON representation. Mixed/BINP BRN-3 receipts
use `BRN3_SOURCE_OUTCOME_V1` with per-source profiles and prepared-manifest hashes;
those bindings also enter reservation hashes, replay validation and checkpoint
validation evidence. Historical selections retain full source descriptors.

The current optimizers require a generation's sample list for repeated NNUE epochs,
metrics and exact optimizer-cursor replay. This bounded, transient in-memory list
is released at the generation boundary. Network features are encoded by the
existing trainers on demand; no transformed full-source or per-generation data
file is created. Acquisition currently precedes optimization rather than overlapping
it. Memory scales with the requested generation, not with source size.

## Source identity and allocation

A source version fingerprint binds format, length, modification time and SHA-256
samples at four byte locations (at most 256 KiB per file). Legacy registration also
binds catalog and manifest samples; visited shards retain their full checksum
checks. Paths and display names are excluded. This is a bounded observed-version
fingerprint, not a claim of a full-file integrity hash. Ordinary edits/appends block
old-cursor consumption; changed versions must be explicitly registered separately.
An adversarial unsampled edit that also restores timestamps is outside this scheme.
Reserved record/target hashes additionally verify every active generation replay.

Weights allocate training and held-out counts separately using integer largest
remainders; ties follow configured source order. Each source advances independently.
Corpus size does not affect allocation: weights 9:1, 4:1 and 1:1 allocate 90:10,
80:20 and 50:50 accepted examples when counts divide exactly. Skipped records consume
raw ordinals but never satisfy these quotas. No Lichess/BT4 policy is selected for
the user; new sources keep the existing default weight of 1.
There is no random source sampling, wraparound or exhaustion fallback. NNUE's
existing seeded within-generation optimizer shuffle is separate from source traversal.
The inspected eight-record prefix of the local Lichess source mixed opening,
middlegame and endgame positions, with some recurring material configurations.
This small check establishes no random ordering guarantee. Sources can contain
correlated order or duplicate positions; this release makes no statistical-
independence or cross-source deduplication claim.

Positions / generation counts training examples. Held-out validation additionally
reserves `max(2, ceil(training/4))` examples, after the training slice for each source.
Game-pair validation acquires no unused holdout examples. Invalid or unsupported
source records advance raw logical ordinals and are recorded as skipped within the
reserved range. Excessive rejects stop a bounded acquisition before reservation.
Exhaustion of any assigned source stops the generation without partial allocation
from the other sources. Existing candidate-vs-best and held-out decision rules remain.

## Persistence and crash boundaries

Each lineage owns `training-data/`:

- `sources.json`: current registrations and mix; `configuration.json`: effective count/target policy.
- `selections/<identity>.json`: retained source descriptors for historical assignments.
- `cursors.json`: checksummed source high-water marks and exact generation reservations.
- `seek/<source-identity>.json`: checksummed, incrementally learned navigation points.

Generation receipts retain their compatible `corpus-training/generation-N.json`
location so existing history and validation envelopes remain readable.

All cursor mutations occur under the existing exclusive checkpoint-store lock.
An empty ledger is persisted before establishing sequential campaign configuration;
subsequent loss or corruption of that ledger blocks consumption rather than resetting it.
Acquisition first reads only enough records to satisfy the deterministic allocation.
Before any optimizer update, one forced atomic ledger replacement records every
`[start,end)` range, content hashes and advanced **next-unallocated** cursors together.
A reserved range is already durably accounted for; it is never allocated twice.
Stop/restart replays the saved ranges and hashes with the existing optimizer cursor.
A crash before reservation can repeat acquisition, but cannot have trained those
records. A crash after reservation but before receipt publication regenerates the
same receipt. No half-published allocation across sources is possible.

After the existing durable candidate decision, promotion or rejection marks the
reservation COMPLETED. Recovery reconciles an already-recorded decision before
allocating the next generation. Candidate rejection still consumes the range.
Changing unfinished-generation settings uses the existing forced restart intent:
its recovery protocol marks the old reservation ABANDONED before replacing the
attempt. Its full ranges remain consumed even if only partly optimized; the reason
and ranges remain inspectable. Failure during this protocol is replayable and never
rewinds source cursors. Atomic rename and directory forcing use the same platform
limits as the existing checkpoint store (directory force is unavailable on Windows).

## Existing lineages and legacy tools

Checkpoint/model formats, history, incumbent/candidate state and saved lineage
configuration versions remain readable. Old permutation campaigns provide no
reliable sequential consumed prefix; generation count is not converted to a cursor.
The Training Data tab explicitly requires acknowledging a new sequential start at
zero, which may overlap older usage. There is no implicit reset or destructive migration.

An existing legacy partial generation with its original receipt can finish using
its original view addresses and exact receipt hashes, without a whole-view startup
hash pass. It stops at that generation's boundary and asks for sequential source
setup. Published legacy views and converted data remain intact. Legacy importer and
explicit integrity/diagnostic APIs remain available for compatibility; the normal
GUI does not invoke them. Historical frozen replay remains a separate explicit workflow.

## Focused verification and local measurement

Reader, ledger, service and Swing component tests cover range boundaries (including
split-frame records), source changes, exhaustion, mixtures, independent lineages,
stop/resume, failed publication, explicit abandonment, both architectures and both
validation methods. The generation path never creates `records.idx` or a new view.

On 2026-10-03, a bounded local test against the existing 22,086,532,809-byte
Lichess archive read the first 10,000 records in 0.502 s and the next 10,000 in
0.215 s. Continuation skipped 1,808 nearby lines; the resulting navigation file
was 584 bytes with five points. A generation acquisition/target-mapping test
produced 10,000 NNUE training plus 2,500 held-out examples in 0.320 s, decoding
exactly 12,500 records. No optimizer or whole-source scan was part of that timing.
These observations demonstrate bounded work, not a machine-independent time target.

The optional test environment variable `SEED_TRAINING_SMOKE_SOURCE` enables these
bounded local-source tests; ordinary runs use synthetic fixtures and skip the local
measurement. Do not run full conversions or long campaigns for routine verification.

BINP tests use small generated archives and a 62-record oracle made by the pinned
upstream writer (fixture generator and expected FENs are under
`app/src/test/resources/binpack/`). The format implementation's upstream provenance
and MIT notice are shipped as `training/data/binpack-NOTICE.txt` in the application
resources. Optional `SEED_BT4_SMOKE_SOURCE` enables detection plus 1,000 decoded
records from the first chunk of each real shard; it never prepares the real corpus.
