package com.ohinteractive.seedv6.training.checkpoint;

import java.io.DataInputStream;
import java.io.IOException;

/** Bootstrap is explicit acceptance, never a fabricated match win. Promotion links prior acceptance evidence. */
public record PromotionRecord(String id, Kind kind, long sequence, long generation, String checkpointId,
                              String previousCheckpointId, String previousRecordId, String validationId) {
    public enum Kind { BOOTSTRAP, PROMOTION }
    public PromotionRecord {
        requireId(id);
        CheckpointManifest.requireId(checkpointId);
        if (sequence < 0 || generation < 0 || kind == null) throw new IllegalArgumentException("Invalid promotion order.");
        if (kind == Kind.BOOTSTRAP) {
            if (sequence != 0 || !previousCheckpointId.isEmpty() || !previousRecordId.isEmpty() || !validationId.isEmpty()) {
                throw new IllegalArgumentException("Invalid bootstrap record.");
            }
        } else {
            if (sequence == 0 || checkpointId.equals(previousCheckpointId)) throw new IllegalArgumentException("Invalid promotion.");
            CheckpointManifest.requireId(previousCheckpointId);
            requireId(previousRecordId);
            ValidationRecord.requireId(validationId);
        }
    }
    static String requireId(String id) {
        if (id == null || !id.matches("p-[0-9a-f]{64}")) throw new IllegalArgumentException("Invalid promotion ID.");
        return id;
    }
    static PromotionRecord create(Kind kind, long sequence, long generation, String checkpoint,
                                  String previous, String previousRecord, String validation) throws IOException {
        var provisional = new PromotionRecord("p-" + "0".repeat(64), kind, sequence, generation,
                checkpoint, previous, previousRecord, validation);
        return new PromotionRecord("p-" + SmallRecord.hash(provisional.encode()), kind, sequence, generation,
                checkpoint, previous, previousRecord, validation);
    }
    byte[] encode() throws IOException {
        return SmallRecord.encode("promotion", out -> {
            out.writeUTF(kind.name()); out.writeLong(sequence); out.writeLong(generation);
            out.writeUTF(checkpointId); out.writeUTF(previousCheckpointId);
            out.writeUTF(previousRecordId); out.writeUTF(validationId);
        });
    }
    static PromotionRecord read(String id, DataInputStream in) throws IOException {
        return new PromotionRecord(id, Kind.valueOf(in.readUTF()), in.readLong(), in.readLong(),
                in.readUTF(), in.readUTF(), in.readUTF(), in.readUTF());
    }
}

