package com.ohinteractive.seedv6.core.brn3;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import com.ohinteractive.seedv6.core.brn.BrnAdamConfig;
import static com.ohinteractive.seedv6.core.brn3.Brn3Codec.*;
import static com.ohinteractive.seedv6.core.brn3.Brn3Layout.*;

/** Exact optimizer continuation codec; runtime decoding belongs to Brn3Codec. */
public final class Brn3TrainingCodec {
    private Brn3TrainingCodec() {}

    public static void writeTraining(Brn3Trainer trainer,OutputStream output)throws IOException {
        var writer=new Writer(output,TRAINING_MAGIC);writer.data.writeLong(trainer.step());var config=trainer.config();
        writer.data.writeDouble(config.learningRate());writer.data.writeDouble(config.beta1());writer.data.writeDouble(config.beta2());writer.data.writeDouble(config.epsilon());
        for(var block:new double[][]{trainer.weights,trainer.first,trainer.second})for(double value:block)writer.data.writeDouble(value);
        writer.finish();
    }

    public static Brn3Trainer readTraining(InputStream input)throws IOException {
        var reader=new Reader(input,TRAINING_MAGIC);
        try {
            long step=reader.data.readLong();var config=new BrnAdamConfig(reader.data.readDouble(),reader.data.readDouble(),reader.data.readDouble(),reader.data.readDouble());
            var weights=reader.parameters();var first=reader.parameters();var second=reader.parameters();reader.finish();
            return new Brn3Trainer(weights,first,second,step,config);
        }catch(IllegalArgumentException invalid){throw new IOException("Invalid BRN-3 training state",invalid);}
    }

    public static byte[] encodeTraining(Brn3Trainer trainer)throws IOException {
        var output=new ByteArrayOutputStream(TRAINING_BYTES);writeTraining(trainer,output);return output.toByteArray();
    }

    public static Brn3Trainer decodeTraining(byte[] bytes)throws IOException{return readTraining(new ByteArrayInputStream(bytes));}
}
