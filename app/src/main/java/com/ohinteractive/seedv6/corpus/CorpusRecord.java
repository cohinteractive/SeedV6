package com.ohinteractive.seedv6.corpus;

import java.nio.ByteBuffer;
import java.util.Objects;

/** Schema 1, 80 bytes, big endian. Targets are raw source values, never Seed search scores.
 * work is exact in its stated unit; Lichess knodes are retained as knodes, not falsely exact nodes.
 * sourceRecord is the 1-based JSONL line; sourceId resolves through manifest provenance.
 */
public record CorpusRecord(CorpusPosition position, int targetKind, int target, int perspective,
                           int depth, long work, int workUnit, int sourceId, long sourceRecord) {
    public static final int BYTES = 80;
    public static final int CP = 1, MATE = 2;
    public static final int WHITE = 1, SIDE_TO_MOVE = 2, RAW_SOURCE = 3;
    public static final int NODES = 1, KILONODES = 2, UNKNOWN_WORK = 0;
    public CorpusRecord {
        Objects.requireNonNull(position);
        if (targetKind != CP && targetKind != MATE || perspective < WHITE || perspective > RAW_SOURCE
                || depth < -1 || work < -1 || workUnit < 0 || workUnit > 2
                || (work == -1) != (workUnit == UNKNOWN_WORK) || sourceId < 1 || sourceRecord < 1)
            throw new IllegalArgumentException("Invalid corpus target/provenance");
    }
    public byte[] encode() {
        ByteBuffer b = ByteBuffer.allocate(BYTES);
        position.write(b);
        b.putInt(targetKind).putInt(target).putInt(perspective).putInt(depth).putLong(work)
                .putInt(workUnit).putInt(sourceId).putLong(sourceRecord);
        return b.array();
    }
    public static CorpusRecord decode(byte[] bytes) {
        if (bytes.length != BYTES) throw new IllegalArgumentException("Incorrect corpus record width");
        ByteBuffer b = ByteBuffer.wrap(bytes);
        return new CorpusRecord(CorpusPosition.read(b), b.getInt(), b.getInt(), b.getInt(), b.getInt(),
                b.getLong(), b.getInt(), b.getInt(), b.getLong());
    }
    /** Comparable labels from the same adapter: higher depth, then work in the same unit.
     * Equal quality retains the first durable label. Incomparable adapters/units retain it too.
     */
    public boolean strongerThan(int oldDepth, long oldWork, int oldUnit) {
        return depth > oldDepth || depth == oldDepth
                && (oldWork < 0 && work >= 0 || workUnit == oldUnit && work > oldWork);
    }
}
