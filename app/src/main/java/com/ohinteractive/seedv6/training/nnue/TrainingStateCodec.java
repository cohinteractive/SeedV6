package com.ohinteractive.seedv6.training.nnue;

import com.ohinteractive.seedv6.core.nnue.NnueBinaryFormat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Training format 1, magic ASCII "S6TRAIN1". Framing: {@link NnueBinaryFormat}.
 * Payload: completed step:i64, learningRate/beta1/beta2/epsilon:four binary64s,
 * then model, first moments, second moments, each in immutable-network parameter order
 * (transformer bias, transformer weights, hidden bias, hidden weights, output bias, output weights).
 * Binary32 parameter/moment bits are exact. Only optimizer boundaries are represented:
 * scratch/partial gradients are excluded; the Adam row cache is derived from moment bits.
 * Single-owner calls only, never concurrently with training. This is not a file publisher.
 */
public final class TrainingStateCodec {
    public static final long MAGIC = 0x5336545241494e31L;
    /** S6TMAT01: combined material/outcome objective, incompatible with legacy full-outcome moments. */
    public static final long MATERIAL_MAGIC = 0x5336544d41543031L;
    public static final int VERSION = 1;
    public static final int ENCODED_BYTES = NnueBinaryFormat.HEADER_BYTES + 40
            + 3 * NnueBinaryFormat.PARAMETER_BYTES + 4;

    private TrainingStateCodec() {}

    public static void write(NnueTrainer trainer, OutputStream output) throws IOException {
        write(trainer, output, false);
    }
    public static void writeMaterial(NnueTrainer trainer, OutputStream output) throws IOException {
        write(trainer, output, true);
    }
    private static void write(NnueTrainer trainer, OutputStream output, boolean material) throws IOException {
        if (trainer.materialBootstrap() != material) throw new IllegalArgumentException("NNUE training codec/semantics mismatch");
        trainer.model().parameters.validate(false);
        AdamOptimizer adam = trainer.optimizer();
        adam.firstMoment.validate(false);
        adam.secondMoment.validate(true);
        NnueBinaryFormat.Writer writer = new NnueBinaryFormat.Writer(output, material ? MATERIAL_MAGIC : MAGIC, VERSION);
        writer.data.writeLong(adam.step());
        AdamHyperparameters hp = adam.hyperparameters();
        writer.data.writeDouble(hp.learningRate());
        writer.data.writeDouble(hp.beta1());
        writer.data.writeDouble(hp.beta2());
        writer.data.writeDouble(hp.epsilon());
        writeParameters(writer, trainer.model().parameters);
        writeParameters(writer, adam.firstMoment);
        writeParameters(writer, adam.secondMoment);
        writer.finish();
    }

    public static NnueTrainer read(InputStream input) throws IOException {
        return read(input, false);
    }
    public static NnueTrainer readMaterial(InputStream input) throws IOException {
        return read(input, true);
    }
    private static NnueTrainer read(InputStream input, boolean material) throws IOException {
        NnueBinaryFormat.Reader reader = new NnueBinaryFormat.Reader(input, material ? MATERIAL_MAGIC : MAGIC, VERSION);
        try {
            long step = reader.data.readLong();
            if (step < 0) throw new IllegalArgumentException("Negative Adam step.");
            AdamHyperparameters hp = new AdamHyperparameters(reader.data.readDouble(),
                    reader.data.readDouble(), reader.data.readDouble(), reader.data.readDouble());
            Parameters parameters = readParameters(reader);
            Parameters first = readParameters(reader);
            Parameters second = readParameters(reader);
            reader.finish();
            return new NnueTrainer(new TrainableNnue(parameters), new AdamOptimizer(hp, step, first, second), material);
        } catch (IllegalArgumentException invalid) {
            throw new IOException("Invalid NNUE training state.", invalid);
        }
    }

    public static byte[] encodeMaterial(NnueTrainer trainer) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(ENCODED_BYTES);
        writeMaterial(trainer, output);
        return output.toByteArray();
    }
    public static byte[] encode(NnueTrainer trainer) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(ENCODED_BYTES);
        write(trainer, output);
        return output.toByteArray();
    }

    public static NnueTrainer decode(byte[] bytes) throws IOException {
        return read(new ByteArrayInputStream(bytes));
    }

    private static void writeParameters(NnueBinaryFormat.Writer writer, Parameters parameters) throws IOException {
        for (float[] group : parameters.groups) NnueBinaryFormat.writeFloats(writer.data, group);
    }

    private static Parameters readParameters(NnueBinaryFormat.Reader reader) throws IOException {
        Parameters p = new Parameters();
        for (float[] group : p.groups) NnueBinaryFormat.readFloats(reader.data, group);
        return p;
    }
}
