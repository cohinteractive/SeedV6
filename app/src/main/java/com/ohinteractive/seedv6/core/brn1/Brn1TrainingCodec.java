package com.ohinteractive.seedv6.core.brn1;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import com.ohinteractive.seedv6.core.brn.BrnAdamConfig;
import static com.ohinteractive.seedv6.core.brn1.Brn1Codec.*;

/** Exact optimizer continuation codec; runtime decoding belongs to Brn1Codec. */
public final class Brn1TrainingCodec {
    private Brn1TrainingCodec() {}

    public static void writeTraining(Brn1Trainer trainer, OutputStream output) throws IOException {
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

    public static Brn1Trainer readTraining(InputStream input) throws IOException {
        Reader reader = new Reader(input, TRAINING_MAGIC);
        try {
            long step = reader.data.readLong();
            BrnAdamConfig config = new BrnAdamConfig(reader.data.readDouble(), reader.data.readDouble(),
                    reader.data.readDouble(), reader.data.readDouble());
            double[] weights = reader.parameters();
            double[] first = reader.parameters();
            double[] second = reader.parameters();
            reader.finish();
            return new Brn1Trainer(weights, config, new Brn1AdamState(step, first, second));
        } catch (IllegalArgumentException invalid) {
            throw new IOException("Invalid BRN-1 training state.", invalid);
        }
    }

    public static byte[] encodeTraining(Brn1Trainer trainer) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(TRAINING_BYTES);
        writeTraining(trainer, output);
        return output.toByteArray();
    }

    public static Brn1Trainer decodeTraining(byte[] bytes) throws IOException {
        return readTraining(new ByteArrayInputStream(bytes));
    }
}
