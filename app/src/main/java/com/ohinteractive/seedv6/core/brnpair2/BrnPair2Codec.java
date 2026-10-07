package com.ohinteractive.seedv6.core.brnpair2;

import java.io.*;
import java.util.BitSet;
import java.util.zip.CRC32;

/** Fixed-size, big-endian V1 payloads. Runtime tables cannot be mistaken for resume state.
 * The checkpoint envelope supplies generation, lineage, hashes and optimizer configuration.
 */
public final class BrnPair2Codec {
    private static final long MODEL_MAGIC=0x5336503252543031L, STATE_MAGIC=0x5336503253543031L;
    public static final int VERSION=1, RELATION_VERSION=1, COMPILED_VERSION=1;
    public static final int PAIRS=294, STATES=169, TABLE_VALUES=PAIRS*STATES*2;
    public static final int TRAINING_PARAMETERS=113231, INFERENCE_PARAMETERS=110527;
    private static final int COVERAGE_WORDS=(INFERENCE_PARAMETERS-1+63)/64;
    // Magic plus nine integer dimensions/versions/formats, then payload and CRC32.
    private static final int HEADER_BYTES=44;
    public static final int MODEL_BYTES=HEADER_BYTES+8+TABLE_VALUES*8+4;
    public static final int TRAINING_BYTES=HEADER_BYTES+16+TRAINING_PARAMETERS*24+COVERAGE_WORDS*8+4;

    private static void header(DataOutputStream out,long magic)throws IOException {
        out.writeLong(magic);
        for(int value:new int[]{VERSION,RELATION_VERSION,COMPILED_VERSION,327,13,2,PAIRS,TRAINING_PARAMETERS,64})out.writeInt(value);
    }
    private static DataInputStream checked(InputStream stream,long magic,int size)throws IOException {
        byte[] bytes=stream.readNBytes(size+1);
        if(bytes.length!=size)throw new IOException("Pair-2 payload length mismatch");
        var crc=new CRC32();crc.update(bytes,0,size-4);
        var tail=new DataInputStream(new ByteArrayInputStream(bytes,size-4,4));
        if(tail.readInt()!=(int)crc.getValue())throw new IOException("Pair-2 checksum mismatch");
        var in=new DataInputStream(new ByteArrayInputStream(bytes));
        if(in.readLong()!=magic)throw new IOException("Wrong Pair-2 artifact/family");
        for(int value:new int[]{VERSION,RELATION_VERSION,COMPILED_VERSION,327,13,2,PAIRS,TRAINING_PARAMETERS,64})
            if(in.readInt()!=value)throw new IOException("Incompatible Pair-2 schema/layout/numerical format");
        return in;
    }
    private static void finish(ByteArrayOutputStream bytes,DataOutputStream out,OutputStream stream)throws IOException {
        out.flush();var crc=new CRC32();crc.update(bytes.toByteArray());out.writeInt((int)crc.getValue());out.flush();bytes.writeTo(stream);
    }
    public static void writeModel(BrnPair2Model model,OutputStream stream)throws IOException {
        var bytes=new ByteArrayOutputStream(MODEL_BYTES);var out=new DataOutputStream(bytes);header(out,MODEL_MAGIC);
        out.writeDouble(model.bias);for(double value:model.weights)out.writeDouble(value);finish(bytes,out,stream);
    }
    public static BrnPair2Model readModel(InputStream stream)throws IOException {
        var in=checked(stream,MODEL_MAGIC,MODEL_BYTES);double bias=finite(in);
        var model=new BrnPair2Model(null,bias);
        for(int i=0;i<model.weights.length;i++)model.weights[i]=finite(in);
        return model;
    }
    public static void writeTraining(BrnPair2Trainer trainer,OutputStream stream)throws IOException {
        var bytes=new ByteArrayOutputStream(TRAINING_BYTES);var out=new DataOutputStream(bytes);header(out,STATE_MAGIC);
        out.writeLong(trainer.step());out.writeDouble(trainer.rate());
        for(var array:new double[][]{trainer.weights,trainer.first,trainer.second})for(double value:array)out.writeDouble(value);
        long[] coverage=trainer.seen.toLongArray();
        for(int i=0;i<COVERAGE_WORDS;i++)out.writeLong(i<coverage.length?coverage[i]:0);
        finish(bytes,out,stream);
    }
    public static BrnPair2Trainer readTraining(InputStream stream)throws IOException {
        var in=checked(stream,STATE_MAGIC,TRAINING_BYTES);long step=in.readLong();double rate=finite(in);
        if(step<0)throw new IOException("Negative Pair-2 optimizer step");
        try {
            var trainer=new BrnPair2Trainer(rate);trainer.updates=step;
            for(var array:new double[][]{trainer.weights,trainer.first,trainer.second})for(int i=0;i<array.length;i++) {
                array[i]=finite(in);
                if(array==trainer.second&&array[i]<0)throw new IOException("Negative Pair-2 Adam variance");
            }
            long[] coverage=new long[COVERAGE_WORDS];for(int i=0;i<coverage.length;i++)coverage[i]=in.readLong();
            trainer.seen.or(BitSet.valueOf(coverage));
            if(trainer.seen.length()>INFERENCE_PARAMETERS-1)throw new IOException("Pair-2 coverage bounds");
            return trainer;
        }catch(IllegalArgumentException invalid){throw new IOException("Invalid Pair-2 trainer",invalid);}
    }
    private static double finite(DataInputStream in)throws IOException {
        double value=in.readDouble();if(!Double.isFinite(value))throw new IOException("Nonfinite Pair-2 value");return value;
    }
    private BrnPair2Codec(){}
}
