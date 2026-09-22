package com.ohinteractive.seedv6.core.brn2;

import com.ohinteractive.seedv6.core.brn.BrnAdamConfig;
import com.ohinteractive.seedv6.core.brn.BrnFeatureSchema;

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

/** Independent, versioned BRN-2 payloads; see package documentation for the wire layout. */
public final class Brn2Codec {
    public static final long MODEL_MAGIC = 0x53364252324d3031L; // S6BR2M01
    public static final long TRAINING_MAGIC = 0x5336425232543031L; // S6BR2T01
    public static final int VERSION = 1;
    public static final int HEADER_BYTES = 8 + 8 * 4;
    public static final int MODEL_BYTES = HEADER_BYTES + 8 * Brn2Model.PARAMETER_COUNT + 4;
    public static final int TRAINING_BYTES = HEADER_BYTES + 40 + 24 * Brn2Model.PARAMETER_COUNT + 4;

    private Brn2Codec() {}

    public static void writeModel(Brn2Model model, OutputStream output) throws IOException {
        Writer writer = new Writer(output, MODEL_MAGIC);
        for (int i = 0; i < Brn2Model.PARAMETER_COUNT; i++) writer.data.writeDouble(model.weight(i));
        writer.finish();
    }

    public static Brn2Model readModel(InputStream input) throws IOException {
        Reader reader = new Reader(input, MODEL_MAGIC);
        double[] weights = reader.parameters();
        reader.finish();
        try {
            return new Brn2Model(weights);
        } catch (IllegalArgumentException invalid) {
            throw new IOException("Invalid BRN-2 model.", invalid);
        }
    }

    /** Single-owner optimizer boundary only; does not publish files or close the stream. */
    public static void writeTraining(Brn2Trainer trainer, OutputStream output) throws IOException {
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

    public static Brn2Trainer readTraining(InputStream input) throws IOException {
        Reader reader = new Reader(input, TRAINING_MAGIC);
        try {
            long step = reader.data.readLong();
            BrnAdamConfig config = new BrnAdamConfig(reader.data.readDouble(), reader.data.readDouble(),
                    reader.data.readDouble(), reader.data.readDouble());
            double[] weights = reader.parameters();
            double[] first = reader.parameters();
            double[] second = reader.parameters();
            reader.finish();
            return new Brn2Trainer(weights, config, new Brn2AdamState(step, first, second));
        } catch (IllegalArgumentException invalid) {
            throw new IOException("Invalid BRN-2 training state.", invalid);
        }
    }

    public static byte[] encodeModel(Brn2Model model) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(MODEL_BYTES);
        writeModel(model, output);
        return output.toByteArray();
    }

    public static Brn2Model decodeModel(byte[] bytes) throws IOException {
        return readModel(new ByteArrayInputStream(bytes));
    }

    public static byte[] encodeTraining(Brn2Trainer trainer) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(TRAINING_BYTES);
        writeTraining(trainer, output);
        return output.toByteArray();
    }

    public static Brn2Trainer decodeTraining(byte[] bytes) throws IOException {
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
            data.writeInt(Brn2Model.NODE_ROWS);
            data.writeInt(Brn2Model.RELATION_ROWS);
            data.writeInt(Brn2Model.STATUS_ROWS);
            data.writeInt(Brn2Model.ENDPOINT_COUNT);
            data.writeInt(Brn2Model.HIDDEN_WIDTH);
            data.writeInt(Brn2Model.PARAMETER_COUNT);
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
                    || data.readInt() != Brn2Model.NODE_ROWS
                    || data.readInt() != Brn2Model.RELATION_ROWS
                    || data.readInt() != Brn2Model.STATUS_ROWS
                    || data.readInt() != Brn2Model.ENDPOINT_COUNT
                    || data.readInt() != Brn2Model.HIDDEN_WIDTH
                    || data.readInt() != Brn2Model.PARAMETER_COUNT) {
                throw new IOException("Unsupported BRN-2 signature, format, schema or dimensions.");
            }
        }

        double[] parameters() throws IOException {
            double[] values = new double[Brn2Model.PARAMETER_COUNT];
            for (int i = 0; i < values.length; i++) values[i] = data.readDouble();
            return values;
        }

        void finish() throws IOException {
            if (new DataInputStream(input).readInt() != (int) crc.getValue()) {
                throw new IOException("BRN-2 checksum mismatch.");
            }
            if (input.read() != -1) throw new IOException("Trailing BRN-2 payload bytes.");
        }
    }
}
