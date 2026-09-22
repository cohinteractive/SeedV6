package com.ohinteractive.seedv6.training.model;

import java.io.IOException;
import com.ohinteractive.seedv6.core.brn1.Brn1Codec;
import com.ohinteractive.seedv6.core.brn2.Brn2Codec;
import com.ohinteractive.seedv6.core.brn.BrnCodec;
import com.ohinteractive.seedv6.core.brn.BrnFeatureSchema;
import com.ohinteractive.seedv6.core.nnue.NnueFeatureSchema;
import com.ohinteractive.seedv6.core.nnue.NnueNetworkCodec;
import com.ohinteractive.seedv6.training.nnue.TrainingStateCodec;

/** Closed set of implemented training payloads. Existing NNUE wire identity stays unchanged. */
public enum TrainingArchitecture {
    NNUE(NnueFeatureSchema.ID, NnueFeatureSchema.VERSION, "network.nnue",
            NnueNetworkCodec.ENCODED_BYTES, TrainingStateCodec.ENCODED_BYTES),
    BRN("seedv6.brn.0", BrnFeatureSchema.VERSION, "network.brn", BrnCodec.MODEL_BYTES, BrnCodec.TRAINING_BYTES),
    BRN1("seedv6.brn.1", BrnFeatureSchema.VERSION, "network.brn1", Brn1Codec.MODEL_BYTES, Brn1Codec.TRAINING_BYTES),
    BRN2("seedv6.brn.2", BrnFeatureSchema.VERSION, "network.brn2", Brn2Codec.MODEL_BYTES, Brn2Codec.TRAINING_BYTES);

    private final String schemaId, networkFile;
    private final int schemaVersion;
    private final long networkBytes, trainingBytes;

    TrainingArchitecture(String schemaId, int schemaVersion, String networkFile, long networkBytes, long trainingBytes) {
        this.schemaId = schemaId; this.schemaVersion = schemaVersion; this.networkFile = networkFile;
        this.networkBytes = networkBytes; this.trainingBytes = trainingBytes;
    }
    public String schemaId() { return schemaId; }
    public int schemaVersion() { return schemaVersion; }
    public String networkFile() { return networkFile; }
    public long networkBytes() { return networkBytes; }
    public long trainingBytes() { return trainingBytes; }

    public static TrainingArchitecture fromSchema(String id, int version) throws IOException {
        for (var architecture : values()) {
            if (architecture.schemaId.equals(id) && architecture.schemaVersion == version) return architecture;
        }
        throw new IOException("Incompatible checkpoint architecture/schema: " + id + " / " + version);
    }
}
