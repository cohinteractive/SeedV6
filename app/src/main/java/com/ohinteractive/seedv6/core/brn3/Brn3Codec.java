package com.ohinteractive.seedv6.core.brn3;

import static com.ohinteractive.seedv6.core.brn3.Brn3Layout.*;
import com.ohinteractive.seedv6.core.brn.BrnAdamConfig;
import java.io.*;
import java.util.zip.*;

/** Independent version-one formats: immutable float32 folded model or exact
 * binary64 trainable parameters/moments. Big endian, fixed header, CRC32 trailer.
 * Research and older BRN formats are never silently interpreted as BRN-3. */
public final class Brn3Codec {
    public static final long MODEL_MAGIC=0x53364252334d3031L,TRAINING_MAGIC=0x5336425233543031L;
    public static final int HEADER_BYTES=40;
    public static final int MODEL_BYTES=HEADER_BYTES+4*MODEL_PARAMETERS+4;
    public static final int TRAINING_BYTES=HEADER_BYTES+40+24*TRAINING_PARAMETERS+4;
    private static final int[] HEADER={1,VERSION,WIDTH,POOL_WIDTH,HIDDEN_WIDTH,MODEL_PARAMETERS,TRAINING_PARAMETERS,1};
    public static void writeModel(Brn3Model model,OutputStream output)throws IOException {
        var writer=new Writer(output,MODEL_MAGIC);
        for(float value:model.weights)writer.data.writeFloat(value);writer.finish();
    }
    public static Brn3Model readModel(InputStream input)throws IOException {
        var reader=new Reader(input,MODEL_MAGIC);var weights=new float[MODEL_PARAMETERS];
        for(int i=0;i<weights.length;i++)weights[i]=reader.data.readFloat();reader.finish();
        try{return new Brn3Model(weights);}catch(IllegalArgumentException invalid){throw new IOException("Invalid BRN-3 model",invalid);}
    }
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
    public static byte[] encodeModel(Brn3Model model)throws IOException {
        var output=new ByteArrayOutputStream(MODEL_BYTES);writeModel(model,output);return output.toByteArray();
    }
    public static Brn3Model decodeModel(byte[] bytes)throws IOException{return readModel(new ByteArrayInputStream(bytes));}
    public static byte[] encodeTraining(Brn3Trainer trainer)throws IOException {
        var output=new ByteArrayOutputStream(TRAINING_BYTES);writeTraining(trainer,output);return output.toByteArray();
    }
    public static Brn3Trainer decodeTraining(byte[] bytes)throws IOException{return readTraining(new ByteArrayInputStream(bytes));}
    private static final class Writer {
        final OutputStream output;final CRC32 crc=new CRC32();final DataOutputStream data;
        Writer(OutputStream output,long magic)throws IOException {
            this.output=output;data=new DataOutputStream(new CheckedOutputStream(output,crc));data.writeLong(magic);
            for(int value:HEADER)data.writeInt(value);
        }
        void finish()throws IOException {data.flush();var trailer=new DataOutputStream(output);trailer.writeInt((int)crc.getValue());trailer.flush();}
    }
    private static final class Reader {
        final InputStream input;final CRC32 crc=new CRC32();final DataInputStream data;
        Reader(InputStream input,long magic)throws IOException {
            this.input=input;data=new DataInputStream(new CheckedInputStream(input,crc));
            if(data.readLong()!=magic)throw new IOException("Not a BRN-3 payload");
            for(int expected:HEADER)if(data.readInt()!=expected)throw new IOException("Unsupported BRN-3 layout or material prior");
        }
        double[] parameters()throws IOException {var values=new double[TRAINING_PARAMETERS];for(int i=0;i<values.length;i++)values[i]=data.readDouble();return values;}
        void finish()throws IOException {
            long actual=crc.getValue();int expected=new DataInputStream(input).readInt();
            if(actual!=Integer.toUnsignedLong(expected))throw new IOException("BRN-3 checksum mismatch");
            if(input.read()!=-1)throw new IOException("Trailing BRN-3 payload bytes");
        }
    }
    private Brn3Codec(){}
}
