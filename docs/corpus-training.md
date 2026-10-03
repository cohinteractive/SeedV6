# Training Data

Network Training consumes original sources sequentially. No import or whole-source
network preparation is required. Existing converted Seed data remains readable;
no downloaded files, shards, checkpoints or histories are deleted by this workflow.

## Setup and controls

- **Training Data** selects the position provider and manages the lineage's source
  registrations, locations, format/version fingerprints and allocation weights.
  Add a Lichess evaluations `.jsonl` or PZstandard `.jsonl.zst` file, or an existing
  Seed data directory. The Lichess adapter reads Stockfish CP/mate labels in that
  export schema; native Stockfish binpack and other schemas need additional readers.
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

Click Apply settings or Start to save source setup. Removing a registration removes
no source files and does not erase its cursor. Inspect sources reports identities,
readiness, known counts, visited seek points, next-unallocated positions and the
reservation history. Relocation checks the same fingerprint; preserve timestamps.
Legacy directory counts come from existing metadata. Text/archive counts remain
unknown until EOF is encountered; counting the whole file is not a startup task.

## Acquisition and seeking

`SourceReaders` returns architecture-neutral `TrainingPosition` values. Format
knowledge remains in readers; `SequentialTraining` applies the existing NNUE
`STOCKFISH_WDL_V1` or BRN-2 `BASIC_V1` target policy and passes the existing common
sample/target interface to unchanged feature kernels and optimizers. BRN-2's
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

A monolithic Zstandard stream without the supported PZstandard framing is rejected
with an explicit efficient-resume error. There is no hidden decompression/rewrite
fallback. Use a compatible framed source or a preserved legacy Seed data source.

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
