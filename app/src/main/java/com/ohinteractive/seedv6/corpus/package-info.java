/**
 * Seed external training corpus schema 1. This is architecture-neutral chess state plus raw
 * targets/provenance; it has no connection to NNUE/BRN training or GUI behavior.
 *
 * <h2>Layout and identity</h2>
 * Root contains index.sqlite (authoritative transactional manifest, sources, exact identity B-tree
 * and latest-label pointers), manifest.json (regenerable readable snapshot), writer.lock, and
 * shards/shard-UUID.scs (immutable committed files). SQLite's transient index.sqlite-journal,
 * manifest.json.pending and *.scs.pending are not corpus data. Completed but unreferenced shards
 * are ignored orphans, never implicitly adopted/deleted. Keep catalog and shards together.
 * Missing/corrupt catalog fails closed; automatic index reconstruction is not provided.
 * A reader delegates hot-journal rollback to SQLite before opening its read-only snapshot;
 * this rare crash recovery requires writable catalog access and never deletes journals manually.
 *
 * <p>Piece codes are empty=0, K/Q/R/B/N/P=1/2/3/4/5/6, with bit 3 set for Black;
 * each plane stores one code bit across squares a1=0 through h8=63. Codes 7/8/15 are invalid.
 * All shard primitives are big endian. Header is 32 bytes: magic SEEDCPS1 (8), schema=1 (4),
 * record width=80 (4), catalog shard id (8), reserved zero (8). SHA-256 and record count are in
 * the committed catalog. Record: four absolute Seed piece planes (32); rules (4: STM bit 0,
 * KQkq bits 1-4, EP square bits 5-10, zero means absent); halfmove (4: -1 unknown, otherwise exact
 * source clock); kind (4: cp=1, mate=2); signed raw target (4); perspective (4: White=1, STM=2,
 * source-defined=3); depth (4: -1 unknown); work (8: -1 unknown); work unit (4: unknown=0,
 * nodes=1, kilonodes=2); source id (4); 1-based source record ordinal (8). Lichess's signed mate
 * target is moves to mate, positive for White winning; its raw cp values are not rescaled.
 *
 * <p>Identity is exactly the first 40 record bytes, not a probabilistic hash. Placement, side,
 * castling, EP, and known/unknown exact halfmove distinguish positions. Fullmove, Zobrist,
 * labels and provenance do not. Seed FEN/Board semantics interpret all positions; only missing
 * counters receive parser-only 0/1. Fullmove is validated then excluded. Reconstructed Boards
 * apply Seed's halfmove saturation and synthetic fullmove 1. Unknown halfmove requires an
 * explicit reader fallback. Source FEN/position output omits unknown counters.
 *
 * <h2>Durability, consolidation and scale</h2>
 * Each source-record batch writes/forces/closes and atomically renames an immutable shard BEFORE
 * the SQLite EXTRA synchronous transaction commits pointers, counts and source statistics.
 * A process interrupted before commit leaves previous data intact and an ignored temporary/orphan
 * file; after commit it leaves a valid new checkpoint. close never commits an unfinished batch.
 * manifest.json is refreshed after commit and at writer reopen, so a stale snapshot is harmless.
 * Durability relies on filesystem atomic moves, SQLite's journal, and storage honoring flushes;
 * hardware/power-loss behavior is not certified by process-crash tests.
 *
 * <p>Latest logical records are unique. Stronger comparable labels append revisions and update
 * pointers; old physical records stay immutable. Same adapter/policy compares depth first, then
 * work when units agree. Known work beats unknown work at equal depth. Equal quality or
 * incomparable adapters retain the existing label. Source statistics count attempts (including
 * rescan duplicates); manifest.positions counts logical identities, storedRecords counts physical
 * revisions. A pass is finished only on the requested bound or EOF. Lichess rereads from byte zero,
 * never claims efficient compressed resume. A bound counts source lines, including rejects.
 *
 * <p>The exact disk B-tree uses a 64 MiB page cache, disk temporary storage and no memory mapping;
 * shard output and source input are buffered; JSONL retention is capped at 1 MiB per line. No
 * corpus-sized Java set exists. Each record performs an indexed lookup and each accepted change
 * an upsert, with batched transactions. Hundreds-of-millions throughput/index disk footprint
 * remain to be measured. Full integrity scans are explicit linear work; normal reopen checks
 * schema and committed header/length integrity. No trainer integration, compaction, multiwriter,
 * random training sampler or efficient compressed seek is claimed by this first milestone.
 */
package com.ohinteractive.seedv6.corpus;
