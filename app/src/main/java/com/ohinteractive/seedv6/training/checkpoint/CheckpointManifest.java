package com.ohinteractive.seedv6.training.checkpoint;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Locale;
import com.ohinteractive.seedv6.core.nnue.NnueFeatureSchema;
import com.ohinteractive.seedv6.core.nnue.NnueNetworkCodec;
import com.ohinteractive.seedv6.search.alphabeta.AlphaBetaPvsSearch;
import com.ohinteractive.seedv6.training.nnue.TrainingStateCodec;

/** V1 manifest. Generation is the strictly increasing publication order; wall time is not identity. */
public record CheckpointManifest(String id, long generation, long optimizerStep, int trainingDepth,
                                 String parentId, long networkBytes, String networkSha256,
                                 long trainingBytes, String trainingSha256) {
    public static final int VERSION = 1;
    public static final String NETWORK_FILE = "network.nnue";
    public static final String TRAINING_FILE = "training.state";
    public static final String MANIFEST_FILE = "manifest.bin";

    public CheckpointManifest {
        requireId(id);
        new Metadata(generation, trainingDepth, parentId);
        if (optimizerStep < 0 || networkBytes != NnueNetworkCodec.ENCODED_BYTES
                || trainingBytes != TrainingStateCodec.ENCODED_BYTES) {
            throw new IllegalArgumentException("Invalid checkpoint sizes/step.");
        }
        SmallRecord.requireHash(networkSha256);
        SmallRecord.requireHash(trainingSha256);
    }

    public record Metadata(long generation, int trainingDepth, String parentId) {
        public Metadata {
            if (generation < 0 || trainingDepth < 1 || trainingDepth > AlphaBetaPvsSearch.MAX_SUPPORTED_DEPTH
                    || parentId == null) throw new IllegalArgumentException("Invalid checkpoint metadata.");
            if (!parentId.isEmpty()) requireId(parentId);
        }
    }

    static CheckpointManifest create(Metadata metadata, long step, String networkHash, String trainingHash) throws IOException {
        String id = identity(metadata, step, networkHash, trainingHash);
        return new CheckpointManifest(id, metadata.generation(), step, metadata.trainingDepth(), metadata.parentId(),
                NnueNetworkCodec.ENCODED_BYTES, networkHash, TrainingStateCodec.ENCODED_BYTES, trainingHash);
    }

    static String requireId(String id) {
        if (id == null || !id.matches("g[0-9]{6,19}-s[0-9]{9,19}-[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Invalid checkpoint ID.");
        }
        return id;
    }

    private static String identity(Metadata metadata, long step, String networkHash, String trainingHash) throws IOException {
        byte[] identity = SmallRecord.encode("checkpoint-identity", out -> {
            out.writeUTF(NnueFeatureSchema.ID);
            out.writeInt(NnueFeatureSchema.VERSION);
            out.writeLong(metadata.generation());
            out.writeLong(step);
            out.writeInt(metadata.trainingDepth());
            out.writeUTF(metadata.parentId());
            out.writeUTF(networkHash);
            out.writeUTF(trainingHash);
        });
        return String.format(Locale.ROOT, "g%06d-s%09d-%s", metadata.generation(), step, SmallRecord.hash(identity));
    }

    byte[] encode() throws IOException { return SmallRecord.encode("manifest", this::write); }

    private void write(DataOutputStream out) throws IOException {
        out.writeUTF(id);
        out.writeUTF(NnueFeatureSchema.ID);
        out.writeInt(NnueFeatureSchema.VERSION);
        out.writeLong(generation);
        out.writeLong(optimizerStep);
        out.writeInt(trainingDepth);
        out.writeUTF(parentId);
        out.writeUTF(NETWORK_FILE);
        out.writeLong(networkBytes);
        out.writeUTF(networkSha256);
        out.writeUTF(TRAINING_FILE);
        out.writeLong(trainingBytes);
        out.writeUTF(trainingSha256);
    }

    static CheckpointManifest read(DataInputStream in) throws IOException {
        String id = in.readUTF();
        if (!NnueFeatureSchema.ID.equals(in.readUTF()) || in.readInt() != NnueFeatureSchema.VERSION) {
            throw new IOException("Incompatible checkpoint schema.");
        }
        long generation = in.readLong(), step = in.readLong();
        int depth = in.readInt();
        String parent = in.readUTF();
        if (!NETWORK_FILE.equals(in.readUTF())) throw new IOException("Unknown network filename.");
        long networkBytes = in.readLong();
        String networkHash = in.readUTF();
        if (!TRAINING_FILE.equals(in.readUTF())) throw new IOException("Unknown training filename.");
        var result = new CheckpointManifest(id, generation, step, depth, parent, networkBytes,
                networkHash, in.readLong(), in.readUTF());
        if (!id.equals(identity(new Metadata(generation, depth, parent), step, networkHash, result.trainingSha256()))) {
            throw new IOException("Checkpoint identity mismatch.");
        }
        return result;
    }
}

