package com.ohinteractive.seedv6.corpus;

import com.ohinteractive.seedv6.training.data.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.sql.*;

/** Existing catalog's location index provides keyset continuation without OFFSET or a new full index. */
public final class LegacyPositionReader implements PositionReader {
    private final DataSource source;
    private final SourceReaders.Navigation navigation;
    private final Connection connection;
    private final PreparedStatement statement;
    private final ResultSet records;
    private FileChannel shard;
    private long shardId = -1, position, decoded, seek, previousShard, previousOrdinal;
    private final ByteBuffer bytes = ByteBuffer.allocate(CorpusRecord.BYTES);
    public LegacyPositionReader(DataSource source, Path indexes, long start) throws IOException {
        this.source = source;
        navigation = new SourceReaders.Navigation(source, indexes, new SourceReaders.Point(0, 0, -1));
        var point = navigation.floor(start); position = point.record(); previousShard = point.offset(); previousOrdinal = point.within();
        Connection opened = null;
        try {
            connection = opened = CorpusCatalog.connect(source.path(), true); connection.setAutoCommit(false); CorpusCatalog.checkSchema(connection);
            statement = connection.prepareStatement("SELECT p.shard,p.ordinal,s.file,s.records,s.sha256 FROM positions p JOIN shards s ON s.id=p.shard WHERE (p.shard,p.ordinal) > (?,?) ORDER BY p.shard,p.ordinal");
            statement.setLong(1, previousShard); statement.setLong(2, previousOrdinal); records = statement.executeQuery();
            while (position < start) {
                if (!records.next()) throw new EOFException("Training Data exhausted before position " + start);
                previousShard = records.getLong(1); previousOrdinal = records.getLong(2); position++; seek++;
            }
        } catch (SQLException | IOException failure) {
            if (opened != null) try { opened.close(); } catch (SQLException close) { failure.addSuppressed(close); }
            throw new IOException("Cannot open legacy Training Data source", failure);
        }
    }
    public Entry next() throws IOException {
        try {
            if (position % 4096 == 0) navigation.add(new SourceReaders.Point(position, previousShard, previousOrdinal));
            if (!records.next()) { navigation.eof(position); return null; }
            long id = records.getLong(1), ordinal = records.getLong(2);
            if (id != shardId) {
                if (shard != null) shard.close();
                Path path = CorpusCatalog.shardPath(source.path(), records.getString(3));
                // Verify only shards actually consumed, not all source shards.
                if (!CorpusCatalog.sha256(path).equals(records.getString(5))) throw new IOException("Training Data shard changed: " + path);
                shard = FileChannel.open(path); CorpusCatalog.checkHeader(shard, id, records.getLong(4)); shardId = id;
            }
            bytes.clear(); CorpusCatalog.readFully(shard, bytes, CorpusCatalog.HEADER_BYTES + Math.multiplyExact(ordinal, CorpusRecord.BYTES));
            var record = CorpusRecord.decode(bytes.array()); previousShard = id; previousOrdinal = ordinal; decoded++;
            return new Entry(position++, record, "");
        } catch (SQLException | RuntimeException e) { throw new IOException("Invalid legacy Training Data record", e); }
    }
    public long nextPosition() { return position; }
    public long decodedRecords() { return decoded; }
    public long seekRecords() { return seek; }
    public void close() throws IOException {
        try { navigation.save(); }
        finally {
            try { if (shard != null) shard.close(); }
            finally { try { records.close(); statement.close(); connection.close(); } catch (SQLException e) { throw new IOException(e); } }
        }
    }
}
