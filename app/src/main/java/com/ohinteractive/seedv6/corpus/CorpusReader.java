package com.ohinteractive.seedv6.corpus;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.sql.*;
import java.util.Arrays;

/** Reusable logical corpus reader, independent of all source decoders. One snapshot per reader.
 * Iteration visits only the current label per identity, in shard/ordinal order (one open shard).
 * Immutable superseded labels remain physical history, never additional training positions.
 */
public final class CorpusReader implements AutoCloseable {
    @FunctionalInterface public interface RecordConsumer { void accept(CorpusRecord record) throws IOException; }
    @FunctionalInterface public interface LocatedConsumer { void accept(long shard, long ordinal, CorpusRecord record) throws IOException; }
    public record Shard(long id, String file, long records, String sha256) {}
    public record Integrity(long positions, long storedRecords, long shards) {}
    private final Path root;
    private final Connection c;

    public CorpusReader(Path root) throws IOException, SQLException {
        this.root = root.toAbsolutePath();
        if (!Files.isRegularFile(this.root.resolve("index.sqlite"))) throw new IOException("Missing corpus catalog");
        // SQLite may need writable access to roll back a hot journal after an interrupted cache
        // spill. Delegate that recovery to SQLite before opening the read-only snapshot; never
        // delete journal files manually. An active writer or unavailable recovery fails closed.
        if (Files.exists(this.root.resolve("index.sqlite-journal"))) {
            try (Connection recovery = CorpusCatalog.connect(this.root, false)) {
                CorpusCatalog.checkSchema(recovery);
            }
        }
        c = CorpusCatalog.connect(this.root, true);
        try {
            c.setAutoCommit(false);
            CorpusCatalog.checkSchema(c);
            checkHeaders();
        } catch (IOException | SQLException ex) { c.close(); throw ex; }
    }
    public CorpusManifest manifest() throws SQLException, IOException { return CorpusCatalog.manifest(c); }
    public java.util.List<Shard> shards() throws SQLException {
        var shards = new java.util.ArrayList<Shard>();
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT * FROM shards ORDER BY id")) {
            while (r.next()) shards.add(new Shard(r.getLong("id"), r.getString("file"), r.getLong("records"), r.getString("sha256")));
        }
        return java.util.List.copyOf(shards);
    }

    private void checkHeaders() throws SQLException, IOException {
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT * FROM shards ORDER BY id")) {
            while (r.next()) try (FileChannel f = FileChannel.open(CorpusCatalog.shardPath(root, r.getString("file")))) {
                CorpusCatalog.checkHeader(f, r.getLong("id"), r.getLong("records"));
            }
        }
    }

    public void forEach(RecordConsumer consumer) throws SQLException, IOException {
        forEachLocated((shard, ordinal, record) -> consumer.accept(record));
    }

    /** Stable physical addresses of the current unique logical records in this reader's transaction. */
    public void forEachLocated(LocatedConsumer consumer) throws SQLException, IOException {
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery(
                "SELECT p.identity,p.ordinal,p.shard,p.depth,p.work,p.unit,p.source,p.perspective,s.file FROM positions p JOIN shards s ON p.shard=s.id ORDER BY p.shard,p.ordinal")) {
            FileChannel f = null;
            long shard = -1;
            ByteBuffer b = ByteBuffer.allocate(CorpusRecord.BYTES);
            try {
                while (r.next()) {
                    if (shard != r.getLong("shard")) {
                        if (f != null) f.close();
                        shard = r.getLong("shard");
                        f = FileChannel.open(CorpusCatalog.shardPath(root, r.getString("file")));
                    }
                    long ordinal = r.getLong("ordinal");
                    if (ordinal < 0 || ordinal > (Long.MAX_VALUE - CorpusCatalog.HEADER_BYTES) / CorpusRecord.BYTES)
                        throw new IOException("Invalid catalog ordinal");
                    b.clear();
                    CorpusCatalog.readFully(f, b, CorpusCatalog.HEADER_BYTES + ordinal * CorpusRecord.BYTES);
                    CorpusRecord record;
                    try { record = CorpusRecord.decode(b.array()); }
                    catch (IllegalArgumentException ex) { throw new IOException("Invalid corpus record", ex); }
                    if (!Arrays.equals(record.position().identity(), r.getBytes("identity"))
                            || record.depth() != r.getInt("depth") || record.work() != r.getLong("work")
                            || record.workUnit() != r.getInt("unit") || record.sourceId() != r.getInt("source")
                            || record.perspective() != r.getInt("perspective"))
                        throw new IOException("Catalog/record identity or quality mismatch");
                    consumer.accept(shard, ordinal, record);
                }
            } finally { if (f != null) f.close(); }
        }
    }

    /** Explicit full validation: database integrity, all committed shard checksums/records,
     * catalog pointers, provenance, and aggregate counts. This is linear disk work, never done
     * implicitly on every import. Reopen always checks schema, headers and lengths.
     */
    public Integrity validate() throws SQLException, IOException {
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("PRAGMA integrity_check")) {
            if (!r.next() || !"ok".equals(r.getString(1))) throw new IOException("Corpus catalog integrity failed");
        }
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("PRAGMA foreign_key_check")) {
            if (r.next()) throw new IOException("Corpus catalog foreign key integrity failed");
        }
        long records = 0, shards = 0;
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT * FROM shards ORDER BY id");
             PreparedStatement source = c.prepareStatement("SELECT 1 FROM sources WHERE id=?")) {
            while (r.next()) {
                Path path = CorpusCatalog.shardPath(root, r.getString("file"));
                if (!CorpusCatalog.sha256(path).equals(r.getString("sha256"))) throw new IOException("Shard checksum mismatch: " + path);
                try (FileChannel f = FileChannel.open(path)) {
                    CorpusCatalog.checkHeader(f, r.getLong("id"), r.getLong("records"));
                    ByteBuffer b = ByteBuffer.allocate(CorpusRecord.BYTES);
                    for (long i = 0; i < r.getLong("records"); i++) {
                        b.clear();
                        CorpusCatalog.readFully(f, b, CorpusCatalog.HEADER_BYTES + i * CorpusRecord.BYTES);
                        CorpusRecord record;
                        try { record = CorpusRecord.decode(b.array()); }
                        catch (IllegalArgumentException ex) { throw new IOException("Invalid shard record", ex); }
                        source.setInt(1, record.sourceId());
                        try (ResultSet sr = source.executeQuery()) {
                            if (!sr.next()) throw new IOException("Unknown record provenance");
                        }
                    }
                }
                records += r.getLong("records");
                shards++;
            }
        }
        long[] positions = {0};
        forEach(record -> positions[0]++);
        CorpusManifest m = manifest();
        if (m.positions() != positions[0] || m.storedRecords() != records || m.completedShards() != shards)
            throw new IOException("Corpus manifest counts mismatch");
        return new Integrity(positions[0], records, shards);
    }
    @Override public void close() throws SQLException { c.close(); }
}
