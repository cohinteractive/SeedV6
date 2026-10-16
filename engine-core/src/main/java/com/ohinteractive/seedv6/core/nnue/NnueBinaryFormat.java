package com.ohinteractive.seedv6.core.nnue;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.CheckedOutputStream;

/**
 * Shared strict V1 framing for the two separately versioned codecs. Big-endian throughout.
 * Header: magic:i64, codecVersion:i32, schemaVersion:i32, schemaIdLength:i32,
 * UTF-8 schema ID bytes, then eight i32s: features, accumulator, concatenated, hidden,
 * outputs, transformer weight count, hidden weight count, total parameter count.
 * CRC32 footer:i32 covers EVERY preceding byte (header and payload), excludes itself.
 * Streams must contain exactly one object: truncation, bad CRC and trailing bytes fail.
 * No caller stream is closed. No lengths read from the input control an allocation.
 */
public final class NnueBinaryFormat {
    private static final byte[] SCHEMA = NnueFeatureSchema.ID.getBytes(StandardCharsets.UTF_8);
    private static final int[] DIMENSIONS = {
            NnueFeatureSchema.FEATURE_COUNT, NnueNetwork.ACCUMULATOR_SIZE,
            NnueNetwork.CONCATENATED_SIZE, NnueNetwork.HIDDEN_SIZE, NnueNetwork.OUTPUT_SIZE,
            NnueNetwork.FEATURE_WEIGHT_COUNT, NnueNetwork.HIDDEN_WEIGHT_COUNT,
            NnueNetwork.PARAMETER_COUNT
    };
    public static final int HEADER_BYTES = 20 + SCHEMA.length + 4 * DIMENSIONS.length;
    public static final int PARAMETER_BYTES = 4 * NnueNetwork.PARAMETER_COUNT;

    private NnueBinaryFormat() {}

    public static final class Writer {
        private final CRC32 crc = new CRC32();
        public final DataOutputStream data;

        public Writer(OutputStream output, long magic, int version) throws IOException {
            data = new DataOutputStream(new CheckedOutputStream(output, crc));
            data.writeLong(magic);
            data.writeInt(version);
            data.writeInt(NnueFeatureSchema.VERSION);
            data.writeInt(SCHEMA.length);
            data.write(SCHEMA);
            for (int dimension : DIMENSIONS) data.writeInt(dimension);
        }

        public void finish() throws IOException {
            int checksum = (int) crc.getValue();
            data.writeInt(checksum);
            data.flush();
        }
    }

    public static final class Reader {
        private final CRC32 crc = new CRC32();
        public final DataInputStream data;

        public Reader(InputStream input, long magic, int version) throws IOException {
            data = new DataInputStream(new CheckedInputStream(input, crc));
            if (data.readLong() != magic) throw new IOException("Wrong NNUE codec magic.");
            require(data.readInt(), version, "codec version");
            require(data.readInt(), NnueFeatureSchema.VERSION, "schema version");
            require(data.readInt(), SCHEMA.length, "schema ID length");
            for (byte expected : SCHEMA) require(data.readUnsignedByte(), expected & 255, "schema ID");
            for (int dimension : DIMENSIONS) require(data.readInt(), dimension, "network dimension/count");
        }

        public void finish() throws IOException {
            int expected = (int) crc.getValue();
            if (data.readInt() != expected) throw new IOException("NNUE CRC32 mismatch.");
            if (data.read() != -1) throw new IOException("Trailing NNUE data.");
        }
    }

    public static void writeFloats(DataOutputStream output, float[] values) throws IOException {
        for (float value : values) output.writeInt(Float.floatToRawIntBits(value));
    }

    public static void readFloats(DataInputStream input, float[] destination) throws IOException {
        for (int i = 0; i < destination.length; i++) destination[i] = readFloat(input);
    }

    public static float readFloat(DataInputStream input) throws IOException {
        float value = Float.intBitsToFloat(input.readInt());
        if (!Float.isFinite(value)) throw new IOException("Nonfinite NNUE parameter/moment.");
        return value;
    }

    private static void require(int actual, int expected, String name) throws IOException {
        if (actual != expected) throw new IOException("Unsupported NNUE " + name + ": " + actual);
    }
}
