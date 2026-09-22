package com.ohinteractive.seedv6.training.checkpoint;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Locale;
import com.ohinteractive.seedv6.training.model.TrainingArchitecture;
import com.ohinteractive.seedv6.search.alphabeta.AlphaBetaPvsSearch;

/** V1 envelope with explicit architecture schema identity; legacy NNUE bytes are unchanged. Generation is the strictly increasing publication order; wall time is not identity. */
public record CheckpointManifest(String id, long generation, long optimizerStep, int trainingDepth,
                                 String parentId, long networkBytes, String networkSha256,
                                 long trainingBytes, String trainingSha256, TrainingArchitecture architecture) {
    public static final int VERSION = 1;
    public static final String NETWORK_FILE = "network.nnue";
    public static final String TRAINING_FILE = "training.state";
    public static final String MANIFEST_FILE = "manifest.bin";

    public CheckpointManifest {
        requireId(id);
        new Metadata(generation, trainingDepth, parentId);
        if (optimizerStep < 0 || architecture == null || networkBytes != architecture.networkBytes()
                || trainingBytes != architecture.trainingBytes()) {
            throw new IllegalArgumentException("Invalid checkpoint sizes/step.");
        }
        SmallRecord.requireHash(networkSha256);
        SmallRecord.requireHash(trainingSha256);
    }

    /** Legacy constructor and wire schema remain NNUE. */
    public CheckpointManifest(String id, long generation, long optimizerStep, int trainingDepth, String parentId,
            long networkBytes, String networkSha256, long trainingBytes, String trainingSha256) {
        this(id, generation, optimizerStep, trainingDepth, parentId, networkBytes, networkSha256,
                trainingBytes, trainingSha256, TrainingArchitecture.NNUE);
    }
    public String networkFile() { return architecture.networkFile(); }

    public record Metadata(long generation, int trainingDepth, String parentId) {
        public Metadata {
            if (generation < 0 || trainingDepth < 1 || trainingDepth > AlphaBetaPvsSearch.MAX_SUPPORTED_DEPTH
                    || parentId == null) throw new IllegalArgumentException("Invalid checkpoint metadata.");
            if (!parentId.isEmpty()) requireId(parentId);
        }
    }

    static CheckpointManifest create(Metadata metadata, long step, String networkHash, String trainingHash) throws IOException {
        return create(metadata, step, networkHash, trainingHash, TrainingArchitecture.NNUE);
    }

    static CheckpointManifest create(Metadata metadata, long step, String networkHash, String trainingHash,
                                     TrainingArchitecture architecture) throws IOException {
        String id = identity(metadata, step, networkHash, trainingHash, architecture);
        return new CheckpointManifest(id, metadata.generation(), step, metadata.trainingDepth(), metadata.parentId(),
                architecture.networkBytes(), networkHash, architecture.trainingBytes(), trainingHash, architecture);
    }

    static String requireId(String id) {
        if (id == null || !id.matches("g[0-9]{6,19}-s[0-9]{9,19}-[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Invalid checkpoint ID.");
        }
        return id;
    }

    private static String identity(Metadata metadata, long step, String networkHash, String trainingHash, TrainingArchitecture architecture) throws IOException {
        byte[] identity = SmallRecord.encode("checkpoint-identity", out -> {
            out.writeUTF(architecture.schemaId());
            out.writeInt(architecture.schemaVersion());
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
        out.writeUTF(architecture.schemaId());
        out.writeInt(architecture.schemaVersion());
        out.writeLong(generation);
        out.writeLong(optimizerStep);
        out.writeInt(trainingDepth);
        out.writeUTF(parentId);
        out.writeUTF(networkFile());
        out.writeLong(networkBytes);
        out.writeUTF(networkSha256);
        out.writeUTF(TRAINING_FILE);
        out.writeLong(trainingBytes);
        out.writeUTF(trainingSha256);
    }

    static CheckpointManifest read(DataInputStream in) throws IOException {
        String id = in.readUTF();
        TrainingArchitecture architecture = TrainingArchitecture.fromSchema(in.readUTF(), in.readInt());
        long generation = in.readLong(), step = in.readLong();
        int depth = in.readInt();
        String parent = in.readUTF();
        if (!architecture.networkFile().equals(in.readUTF())) throw new IOException("Unknown network filename.");
        long networkBytes = in.readLong();
        String networkHash = in.readUTF();
        if (!TRAINING_FILE.equals(in.readUTF())) throw new IOException("Unknown training filename.");
        var result = new CheckpointManifest(id, generation, step, depth, parent, networkBytes,
                networkHash, in.readLong(), in.readUTF(), architecture);
        if (!id.equals(identity(new Metadata(generation, depth, parent), step, networkHash, result.trainingSha256(), architecture))) {
            throw new IOException("Checkpoint identity mismatch.");
        }
        return result;
    }
}

