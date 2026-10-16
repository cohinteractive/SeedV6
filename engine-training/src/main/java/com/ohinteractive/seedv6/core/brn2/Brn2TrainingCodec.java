package com.ohinteractive.seedv6.core.brn2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import com.ohinteractive.seedv6.core.brn.BrnAdamConfig;
import static com.ohinteractive.seedv6.core.brn2.Brn2Codec.*;

/** Exact optimizer continuation codec; runtime decoding belongs to Brn2Codec. */
public final class Brn2TrainingCodec {
    private Brn2TrainingCodec() {}

    public static void writeTraining(Brn2Trainer trainer, OutputStream output) throws IOException {
        Writer writer = new Writer(output, TRAINING_MAGIC, trainer.materialPrior());
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
            return new Brn2Trainer(weights, config, new Brn2AdamState(step, first, second), reader.materialPrior);
        } catch (IllegalArgumentException invalid) {
            throw new IOException("Invalid BRN-2 training state.", invalid);
        }
    }

    public static byte[] encodeTraining(Brn2Trainer trainer) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(TRAINING_BYTES);
        writeTraining(trainer, output);
        return output.toByteArray();
    }

    public static Brn2Trainer decodeTraining(byte[] bytes) throws IOException {
        return readTraining(new ByteArrayInputStream(bytes));
    }
}
