package com.ohinteractive.seedv6.corpus;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.*;
import java.sql.*;
import java.util.UUID;

/** Single-writer, bounded-cache append/upsert API. Explicit checkpoint commits; close rolls back
 * any unfinished batch. Completed shards are immutable. No source decoder or trainer dependency.
 */
public final class CorpusWriter implements AutoCloseable {
    public record SourceInfo(String adapter, String identifier, String metadata, String policy) {
        public SourceInfo {
            if (adapter == null || identifier == null || metadata == null || policy == null
                    || adapter.isBlank() || identifier.isBlank() || policy.isBlank())
                throw new IllegalArgumentException("Missing source provenance");
        }
    }
    public record Stats(long read, long accepted, long rejected, long duplicates, long added,
                        long upgraded, long persisted, long completedShards) {}
    public enum Result { ADDED, DUPLICATE, UPGRADED }
    private final Path root;
    private final SourceInfo source;
    private final int shardSize;
    private final FileChannel lockChannel;
    private final FileLock lock;
    private final Connection c;
    private final PreparedStatement find, upsert;
    private final int sourceId;
    private long read, accepted, rejected, duplicates, added, upgraded, persisted, completed;
    private long batchRead, batchAccepted, batchRejected, batchDuplicates, batchAdded, batchUpgraded;
    private long shardId, shardRecords;
    private Path pending, published;
    private FileChannel shard;
    private final ByteBuffer output = ByteBuffer.allocate(65536);
    private boolean failed, closed;

    public CorpusWriter(Path root, int shardSize, SourceInfo source) throws IOException, SQLException {
        if (shardSize < 1 || shardSize > 1_000_000) throw new IllegalArgumentException("Shard size must be 1..1000000");
        this.root = root.toAbsolutePath(); this.shardSize = shardSize; this.source = source;
        Files.createDirectories(this.root);
        lockChannel = FileChannel.open(this.root.resolve("writer.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        FileLock acquired;
        try { acquired = lockChannel.tryLock(); }
        catch (OverlappingFileLockException ex) { lockChannel.close(); throw new IOException("Corpus already has a writer", ex); }
        if (acquired == null) { lockChannel.close(); throw new IOException("Corpus already has a writer"); }
        lock = acquired;
        Connection opened = null;
        try {
            boolean existing = Files.exists(this.root.resolve("index.sqlite"));
            if (!existing) {
                try (var files = Files.list(this.root)) {
                    if (files.anyMatch(p -> !p.getFileName().toString().equals("writer.lock")))
                        throw new IOException("Refusing to initialize a nonempty root without a catalog");
                }
            }
            Files.createDirectories(this.root.resolve("shards"));
            opened = CorpusCatalog.connect(this.root, false);
            if (!existing) CorpusCatalog.initialize(opened);
            CorpusCatalog.checkSchema(opened);
            // Reopen checks every committed header/length; a full checksum scan is explicit.
            try (CorpusReader reader = new CorpusReader(this.root)) { reader.manifest(); }
            c = opened;
            sourceId = registerSource();
            CorpusCatalog.snapshot(this.root, c);
            c.setAutoCommit(false);
            find = c.prepareStatement("SELECT p.depth,p.work,p.unit,p.perspective,s.adapter,s.policy FROM positions p JOIN sources s ON p.source=s.id WHERE p.identity=?");
            upsert = c.prepareStatement("INSERT INTO positions VALUES(?,?,?,?,?,?,?,?) ON CONFLICT(identity) DO UPDATE SET shard=excluded.shard, ordinal=excluded.ordinal, depth=excluded.depth, work=excluded.work, unit=excluded.unit, source=excluded.source, perspective=excluded.perspective");
        } catch (IOException | SQLException | RuntimeException ex) {
            if (opened != null) opened.close();
            lock.release(); lockChannel.close(); throw ex;
        }
    }

    private int registerSource() throws SQLException, IOException {
        try (PreparedStatement s = c.prepareStatement("INSERT INTO sources(adapter,identifier,metadata,policy) VALUES(?,?,?,?) ON CONFLICT(identifier) DO NOTHING")) {
            s.setString(1, source.adapter()); s.setString(2, source.identifier());
            s.setString(3, source.metadata()); s.setString(4, source.policy()); s.executeUpdate();
        }
        try (PreparedStatement s = c.prepareStatement("SELECT id,adapter,metadata,policy FROM sources WHERE identifier=?")) {
            s.setString(1, source.identifier());
            try (ResultSet r = s.executeQuery()) {
                if (!r.next() || !source.adapter().equals(r.getString("adapter"))
                        || !source.metadata().equals(r.getString("metadata")) || !source.policy().equals(r.getString("policy")))
                    throw new IOException("Source identifier already has different provenance/policy");
                return r.getInt("id");
            }
        }
    }
    public int sourceId() { return sourceId; }
    public Stats stats() { return new Stats(read, accepted, rejected, duplicates, added, upgraded, persisted, completed); }

    public Result put(CorpusRecord record) throws IOException, SQLException {
        requireUsable();
        if (record.sourceId() != sourceId) throw new IllegalArgumentException("Wrong source ID");
        try {
            read++; batchRead++; accepted++; batchAccepted++;
            byte[] identity = record.position().identity();
            find.setBytes(1, identity);
            boolean exists, stronger;
            try (ResultSet r = find.executeQuery()) {
                exists = r.next();
                stronger = exists && record.perspective() == r.getInt("perspective")
                        && source.adapter().equals(r.getString("adapter")) && source.policy().equals(r.getString("policy"))
                        && record.strongerThan(r.getInt("depth"), r.getLong("work"), r.getInt("unit"));
            }
            if (exists) { duplicates++; batchDuplicates++; }
            Result result = Result.DUPLICATE;
            if (!exists || stronger) {
                if (shard == null) startShard();
                if (output.remaining() < CorpusRecord.BYTES) flushOutput();
                output.put(record.encode());
                upsert.setBytes(1, identity); upsert.setLong(2, shardId); upsert.setLong(3, shardRecords++);
                upsert.setInt(4, record.depth()); upsert.setLong(5, record.work());
                upsert.setInt(6, record.workUnit()); upsert.setInt(7, sourceId);
                upsert.setInt(8, record.perspective()); upsert.executeUpdate();
                if (exists) { upgraded++; batchUpgraded++; result = Result.UPGRADED; }
                else { added++; batchAdded++; result = Result.ADDED; }
            }
            if (batchRead >= shardSize) checkpoint(false);
            return result;
        } catch (IOException | SQLException | RuntimeException ex) { failed = true; throw ex; }
    }

    public void reject() throws IOException, SQLException {
        requireUsable();
        read++; batchRead++; rejected++; batchRejected++;
        if (batchRead >= shardSize) checkpoint(false);
    }

    private void startShard() throws SQLException, IOException {
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT coalesce(max(id),0)+1 FROM shards")) {
            r.next(); shardId = r.getLong(1);
        }
        published = root.resolve("shards").resolve("shard-" + UUID.randomUUID() + ".scs");
        pending = published.resolveSibling(published.getFileName() + ".pending");
        shard = FileChannel.open(pending, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        CorpusCatalog.writeFully(shard, CorpusCatalog.header(shardId));
    }
    private void flushOutput() throws IOException {
        output.flip(); CorpusCatalog.writeFully(shard, output); output.clear();
    }

    /** A finished pass is recorded only after the decoder reaches its requested bound or EOF.
     * Source counters are cumulative attempts; rereads on restart therefore increase read/duplicate
     * counters, while positions remains logical cardinality. Checkpoints also bound duplicate-only runs.
     */
    public void checkpoint(boolean finishedPass) throws IOException, SQLException {
        requireUsable();
        try {
            if (shard != null) {
                flushOutput(); shard.force(true); shard.close(); shard = null;
                String checksum = CorpusCatalog.sha256(pending);
                Files.move(pending, published, StandardCopyOption.ATOMIC_MOVE);
                try (PreparedStatement s = c.prepareStatement("INSERT INTO shards VALUES(?,?,?,?)")) {
                    s.setLong(1, shardId); s.setString(2, published.getFileName().toString());
                    s.setLong(3, shardRecords); s.setString(4, checksum); s.executeUpdate();
                }
            }
            try (PreparedStatement s = c.prepareStatement("UPDATE sources SET read=read+?,accepted=accepted+?,rejected=rejected+?,duplicates=duplicates+?,added=added+?,upgraded=upgraded+?,passes=passes+? WHERE id=?")) {
                s.setLong(1, batchRead); s.setLong(2, batchAccepted); s.setLong(3, batchRejected);
                s.setLong(4, batchDuplicates); s.setLong(5, batchAdded); s.setLong(6, batchUpgraded);
                s.setInt(7, finishedPass ? 1 : 0); s.setInt(8, sourceId); s.executeUpdate();
            }
            try (PreparedStatement s = c.prepareStatement("UPDATE manifest SET positions=positions+?,records=records+?,shards=shards+? WHERE id=1")) {
                s.setLong(1, batchAdded); s.setLong(2, shardRecords); s.setInt(3, shardRecords == 0 ? 0 : 1); s.executeUpdate();
            }
            c.commit();
            persisted += shardRecords; completed += shardRecords == 0 ? 0 : 1;
            shardRecords = 0; pending = null; published = null;
            batchRead = batchAccepted = batchRejected = batchDuplicates = batchAdded = batchUpgraded = 0;
            CorpusCatalog.snapshot(root, c);
            // Snapshot reads start a JDBC transaction; release it before the next write batch.
            c.commit();
        } catch (IOException | SQLException | RuntimeException ex) { failed = true; throw ex; }
    }

    private void requireUsable() throws IOException {
        if (closed || failed) throw new IOException("Corpus writer is closed or failed; reopen before continuing");
    }
    @Override public void close() throws IOException, SQLException {
        if (closed) return;
        closed = true;
        try {
            c.rollback();
            if (shard != null) { shard.close(); shard = null; }
            // Delete only this writer's unpublished temporary file. Published orphans are ignored,
            // never blindly deleted or adopted after a failed/uncertain database commit.
            if (pending != null) Files.deleteIfExists(pending);
        } finally {
            try { c.close(); } finally { lock.release(); lockChannel.close(); }
        }
    }
}
