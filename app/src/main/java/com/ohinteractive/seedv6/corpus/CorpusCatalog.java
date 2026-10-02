package com.ohinteractive.seedv6.corpus;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HexFormat;
import org.sqlite.SQLiteConfig;
import com.google.gson.GsonBuilder;

/** Internal catalog/codec shared by the source-independent reader and writer. */
final class CorpusCatalog {
    static final int VERSION = 1, HEADER_BYTES = 32, APPLICATION_ID = 0x53454544;
    static final long MAGIC = 0x5345454443505331L; // SEEDCPS1
    private CorpusCatalog() {}

    static Connection connect(Path root, boolean readOnly) throws SQLException {
        SQLiteConfig config = new SQLiteConfig();
        config.setReadOnly(readOnly);
        config.setBusyTimeout(5000);
        config.setCacheSize(-65536); // 64 MiB; independent of corpus cardinality
        Connection c = config.createConnection("jdbc:sqlite:" + root.resolve("index.sqlite").toAbsolutePath());
        try (Statement s = c.createStatement()) {
            s.execute("PRAGMA foreign_keys=ON");
            s.execute("PRAGMA temp_store=FILE");
            s.execute("PRAGMA mmap_size=0");
            if (!readOnly) {
                s.execute("PRAGMA journal_mode=DELETE");
                s.execute("PRAGMA synchronous=EXTRA");
            }
        } catch (SQLException ex) { c.close(); throw ex; }
        return c;
    }

    static void initialize(Connection c) throws SQLException {
        c.setAutoCommit(false);
        try (Statement s = c.createStatement()) {
            s.execute("CREATE TABLE manifest (id INTEGER PRIMARY KEY CHECK(id=1), positions INTEGER NOT NULL, records INTEGER NOT NULL, shards INTEGER NOT NULL)");
            s.execute("INSERT INTO manifest VALUES(1,0,0,0)");
            s.execute("CREATE TABLE sources (id INTEGER PRIMARY KEY, adapter TEXT NOT NULL, identifier TEXT NOT NULL UNIQUE, metadata TEXT NOT NULL, policy TEXT NOT NULL, read INTEGER NOT NULL DEFAULT 0, accepted INTEGER NOT NULL DEFAULT 0, rejected INTEGER NOT NULL DEFAULT 0, duplicates INTEGER NOT NULL DEFAULT 0, added INTEGER NOT NULL DEFAULT 0, upgraded INTEGER NOT NULL DEFAULT 0, passes INTEGER NOT NULL DEFAULT 0)");
            s.execute("CREATE TABLE shards (id INTEGER PRIMARY KEY, file TEXT NOT NULL UNIQUE, records INTEGER NOT NULL, sha256 TEXT NOT NULL)");
            s.execute("CREATE TABLE positions (identity BLOB PRIMARY KEY CHECK(length(identity)=40), shard INTEGER NOT NULL, ordinal INTEGER NOT NULL, depth INTEGER NOT NULL, work INTEGER NOT NULL, unit INTEGER NOT NULL, source INTEGER NOT NULL REFERENCES sources(id), perspective INTEGER NOT NULL, FOREIGN KEY(shard) REFERENCES shards(id) DEFERRABLE INITIALLY DEFERRED) WITHOUT ROWID");
            s.execute("CREATE INDEX position_location ON positions(shard,ordinal)");
            s.execute("PRAGMA application_id=" + APPLICATION_ID);
            s.execute("PRAGMA user_version=" + VERSION);
            c.commit();
        } finally { c.setAutoCommit(true); }
    }

    static void checkSchema(Connection c) throws SQLException, IOException {
        try (Statement s = c.createStatement()) {
            try (ResultSet r = s.executeQuery("PRAGMA application_id")) {
                if (!r.next() || r.getInt(1) != APPLICATION_ID) throw new IOException("Not a Seed corpus catalog");
            }
            try (ResultSet r = s.executeQuery("PRAGMA user_version")) {
                if (!r.next() || r.getInt(1) != VERSION) throw new IOException("Unsupported Seed corpus schema");
            }
        }
    }

    static CorpusManifest manifest(Connection c) throws SQLException, IOException {
        ArrayList<CorpusManifest.Source> sources = new ArrayList<>();
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT * FROM sources ORDER BY id")) {
            while (r.next()) sources.add(new CorpusManifest.Source(r.getInt("id"), r.getString("adapter"),
                    r.getString("identifier"), r.getString("metadata"), r.getString("policy"), r.getLong("read"),
                    r.getLong("accepted"), r.getLong("rejected"), r.getLong("duplicates"), r.getLong("added"),
                    r.getLong("upgraded"), r.getLong("passes")));
        }
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT * FROM manifest WHERE id=1")) {
            if (!r.next()) throw new IOException("Missing corpus manifest");
            return new CorpusManifest(VERSION, CorpusRecord.BYTES, r.getLong("positions"), r.getLong("records"),
                    r.getLong("shards"), sources);
        }
    }

    static void snapshot(Path root, Connection c) throws IOException, SQLException {
        byte[] json = (new GsonBuilder().setPrettyPrinting().create().toJson(manifest(c)) + "\n")
                .getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Path pending = root.resolve("manifest.json.pending");
        try (FileChannel f = FileChannel.open(pending, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            writeFully(f, ByteBuffer.wrap(json));
            f.force(true);
        }
        Files.move(pending, root.resolve("manifest.json"), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    static ByteBuffer header(long id) {
        return ByteBuffer.allocate(HEADER_BYTES).putLong(MAGIC).putInt(VERSION).putInt(CorpusRecord.BYTES)
                .putLong(id).putLong(0).flip();
    }
    static void checkHeader(FileChannel f, long id, long records) throws IOException {
        if (records <= 0 || records > (Long.MAX_VALUE - HEADER_BYTES) / CorpusRecord.BYTES
                || f.size() != HEADER_BYTES + records * CorpusRecord.BYTES) throw new IOException("Invalid/truncated shard length: " + id);
        ByteBuffer b = ByteBuffer.allocate(HEADER_BYTES);
        readFully(f, b, 0);
        b.flip();
        if (b.getLong() != MAGIC || b.getInt() != VERSION || b.getInt() != CorpusRecord.BYTES
                || b.getLong() != id || b.getLong() != 0) throw new IOException("Invalid shard header/schema: " + id);
    }
    static Path shardPath(Path root, String filename) throws IOException {
        if (!filename.matches("shard-[0-9a-f-]+\\.scs")) throw new IOException("Unsafe shard filename");
        Path path = root.resolve("shards").resolve(filename);
        if (Files.isSymbolicLink(path)) throw new IOException("Symlink shard is unsupported");
        return path;
    }
    static void readFully(FileChannel f, ByteBuffer b, long offset) throws IOException {
        while (b.hasRemaining()) {
            int n = f.read(b, offset);
            if (n < 0) throw new IOException("Truncated corpus shard");
            offset += n;
        }
    }
    static void writeFully(FileChannel f, ByteBuffer b) throws IOException {
        while (b.hasRemaining()) f.write(b);
    }
    static MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException ex) { throw new AssertionError(ex); }
    }
    static String sha256(Path path) throws IOException {
        MessageDigest d = digest();
        try (var in = Files.newInputStream(path)) {
            byte[] buffer = new byte[65536];
            for (int n; (n = in.read(buffer)) >= 0;) d.update(buffer, 0, n);
        }
        return HexFormat.of().formatHex(d.digest());
    }
}
