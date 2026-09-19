package com.ohinteractive.seedv6.core.nnue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/**
 * Immutable network format 1, magic ASCII "S6NNUE01". Framing: {@link NnueBinaryFormat}.
 * Payload is raw binary32 bits, in order: transformer bias[64], transformer weights
 * [49152*64], hidden bias[32], hidden weights[32*128], output bias[1], output weights[32].
 * Each array retains the NnueNetwork contiguous indexing order. No object serialization.
 */
public final class NnueNetworkCodec {
    public static final long MAGIC = 0x53364e4e55453031L;
    public static final int VERSION = 1;
    public static final int ENCODED_BYTES = NnueBinaryFormat.HEADER_BYTES
            + NnueBinaryFormat.PARAMETER_BYTES + 4;

    private NnueNetworkCodec() {}

    public static void write(NnueNetwork network, OutputStream output) throws IOException {
        Objects.requireNonNull(network, "network");
        NnueBinaryFormat.Writer writer = new NnueBinaryFormat.Writer(output, MAGIC, VERSION);
        DataOutputStream data = writer.data;
        for (int i = 0; i < NnueNetwork.ACCUMULATOR_SIZE; i++) writeFloat(data, network.featureBias(i));
        for (int row = 0; row < NnueFeatureSchema.FEATURE_COUNT; row++) {
            for (int i = 0; i < NnueNetwork.ACCUMULATOR_SIZE; i++) writeFloat(data, network.featureWeight(row, i));
        }
        for (int i = 0; i < NnueNetwork.HIDDEN_SIZE; i++) writeFloat(data, network.hiddenBias(i));
        for (int unit = 0; unit < NnueNetwork.HIDDEN_SIZE; unit++) {
            for (int i = 0; i < NnueNetwork.CONCATENATED_SIZE; i++) writeFloat(data, network.hiddenWeight(unit, i));
        }
        writeFloat(data, network.outputBias());
        for (int i = 0; i < NnueNetwork.HIDDEN_SIZE; i++) writeFloat(data, network.outputWeight(i));
        writer.finish();
    }

    public static NnueNetwork read(InputStream input) throws IOException {
        NnueBinaryFormat.Reader reader = new NnueBinaryFormat.Reader(input, MAGIC, VERSION);
        float[] bias = new float[NnueNetwork.ACCUMULATOR_SIZE];
        float[] weights = new float[NnueNetwork.FEATURE_WEIGHT_COUNT];
        float[] hiddenBias = new float[NnueNetwork.HIDDEN_SIZE];
        float[] hiddenWeights = new float[NnueNetwork.HIDDEN_WEIGHT_COUNT];
        float[] outputWeights = new float[NnueNetwork.HIDDEN_SIZE];
        NnueBinaryFormat.readFloats(reader.data, bias);
        NnueBinaryFormat.readFloats(reader.data, weights);
        NnueBinaryFormat.readFloats(reader.data, hiddenBias);
        NnueBinaryFormat.readFloats(reader.data, hiddenWeights);
        float outputBias = NnueBinaryFormat.readFloat(reader.data);
        NnueBinaryFormat.readFloats(reader.data, outputWeights);
        reader.finish();
        return NnueNetwork.of(NnueFeatureSchema.VERSION, bias, weights, hiddenBias,
                hiddenWeights, outputBias, outputWeights);
    }

    public static byte[] encode(NnueNetwork network) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(ENCODED_BYTES);
        write(network, output);
        return output.toByteArray();
    }

    public static NnueNetwork decode(byte[] bytes) throws IOException {
        return read(new ByteArrayInputStream(bytes));
    }

    private static void writeFloat(DataOutputStream data, float value) throws IOException {
        data.writeInt(Float.floatToRawIntBits(value));
    }
}
