package com.ohinteractive.seedv6.core.brn;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.CheckedOutputStream;

/** Independent, versioned BRN payloads; see package documentation for the wire layout. */
public final class BrnCodec {
    public static final long MODEL_MAGIC = 0x533642524e4d3031L; // S6BRNM01
    public static final long TRAINING_MAGIC = 0x533642524e543031L; // S6BRNT01
    public static final int VERSION = 1;
    public static final int HEADER_BYTES = 8 + 4 + 4 + 4;
    public static final int MODEL_BYTES = HEADER_BYTES + 8 * BrnFeatureSchema.PARAMETER_COUNT + 4;
    public static final int TRAINING_BYTES = HEADER_BYTES + 40 + 24 * BrnFeatureSchema.PARAMETER_COUNT + 4;

    private BrnCodec() {}

    public static void writeModel(BrnModel model, OutputStream output) throws IOException {
        Writer writer = new Writer(output, MODEL_MAGIC);
        for (int i = 0; i < BrnFeatureSchema.PARAMETER_COUNT; i++) writer.data.writeDouble(model.weight(i));
        writer.finish();
    }

    public static BrnModel readModel(InputStream input) throws IOException {
        Reader reader = new Reader(input, MODEL_MAGIC);
        double[] weights = reader.parameters();
        reader.finish();
        try {
            return new BrnModel(weights);
        } catch (IllegalArgumentException invalid) {
            throw new IOException("Invalid BRN model.", invalid);
        }
    }

    /** Single-owner optimizer boundary only; does not publish files or close the stream. */
    public static void writeTraining(BrnTrainer trainer, OutputStream output) throws IOException {
        Writer writer = new Writer(output, TRAINING_MAGIC);
        writer.data.writeLong(trainer.optimizer().step());
        BrnAdamConfig config = trainer.config();
        writer.data.writeDouble(config.learningRate());
        writer.data.writeDouble(config.beta1());
        writer.data.writeDouble(config.beta2());
        writer.data.writeDouble(config.epsilon());
        writer.parameters(trainer.weights);
        writer.parameters(trainer.optimizer().first);
        writer.parameters(trainer.optimizer().second);
        writer.finish();
    }

    public static BrnTrainer readTraining(InputStream input) throws IOException {
        Reader reader = new Reader(input, TRAINING_MAGIC);
        try {
            long step = reader.data.readLong();
            BrnAdamConfig config = new BrnAdamConfig(reader.data.readDouble(), reader.data.readDouble(),
                    reader.data.readDouble(), reader.data.readDouble());
            double[] weights = reader.parameters();
            double[] first = reader.parameters();
            double[] second = reader.parameters();
            reader.finish();
            return new BrnTrainer(weights, config, new BrnAdamState(step, first, second));
        } catch (IllegalArgumentException invalid) {
            throw new IOException("Invalid BRN training state.", invalid);
        }
    }

    public static byte[] encodeModel(BrnModel model) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(MODEL_BYTES);
        writeModel(model, output);
        return output.toByteArray();
    }

    public static BrnModel decodeModel(byte[] bytes) throws IOException {
        return readModel(new ByteArrayInputStream(bytes));
    }

    public static byte[] encodeTraining(BrnTrainer trainer) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(TRAINING_BYTES);
        writeTraining(trainer, output);
        return output.toByteArray();
    }

    public static BrnTrainer decodeTraining(byte[] bytes) throws IOException {
        return readTraining(new ByteArrayInputStream(bytes));
    }

    private static final class Writer {
        final OutputStream output;
        final CRC32 crc = new CRC32();
        final DataOutputStream data;

        Writer(OutputStream output, long magic) throws IOException {
            this.output = output;
            data = new DataOutputStream(new CheckedOutputStream(output, crc));
            data.writeLong(magic);
            data.writeInt(VERSION);
            data.writeInt(BrnFeatureSchema.VERSION);
            data.writeInt(BrnFeatureSchema.PARAMETER_COUNT);
        }

        void parameters(double[] values) throws IOException {
            for (double value : values) data.writeDouble(value);
        }

        void finish() throws IOException {
            data.flush();
            DataOutputStream trailer = new DataOutputStream(output);
            trailer.writeInt((int) crc.getValue());
            trailer.flush();
        }
    }

    private static final class Reader {
        final InputStream input;
        final CRC32 crc = new CRC32();
        final DataInputStream data;

        Reader(InputStream input, long magic) throws IOException {
            this.input = input;
            data = new DataInputStream(new CheckedInputStream(input, crc));
            if (data.readLong() != magic || data.readInt() != VERSION
                    || data.readInt() != BrnFeatureSchema.VERSION
                    || data.readInt() != BrnFeatureSchema.PARAMETER_COUNT) {
                throw new IOException("Unsupported BRN signature, format, schema or dimensions.");
            }
        }

        double[] parameters() throws IOException {
            double[] values = new double[BrnFeatureSchema.PARAMETER_COUNT];
            for (int i = 0; i < values.length; i++) values[i] = data.readDouble();
            return values;
        }

        void finish() throws IOException {
            if (new DataInputStream(input).readInt() != (int) crc.getValue()) {
                throw new IOException("BRN checksum mismatch.");
            }
            if (input.read() != -1) throw new IOException("Trailing BRN payload bytes.");
        }
    }
}
