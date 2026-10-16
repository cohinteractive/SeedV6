package com.ohinteractive.seedv6.core.brnpair2;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import static com.ohinteractive.seedv6.core.brnpair2.BrnPair2Codec.*;
import java.util.BitSet;

/** Exact optimizer continuation codec; runtime decoding belongs to BrnPair2Codec. */
public final class BrnPair2TrainingCodec {
    private BrnPair2TrainingCodec() {}

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
        if(step<0)throw new IOException("Negative BRE-Pair 2 optimizer step");
        try {
            var trainer=new BrnPair2Trainer(rate);trainer.updates=step;
            for(var array:new double[][]{trainer.weights,trainer.first,trainer.second})for(int i=0;i<array.length;i++) {
                array[i]=finite(in);
                if(array==trainer.second&&array[i]<0)throw new IOException("Negative BRE-Pair 2 Adam variance");
            }
            long[] coverage=new long[COVERAGE_WORDS];for(int i=0;i<coverage.length;i++)coverage[i]=in.readLong();
            trainer.seen.or(BitSet.valueOf(coverage));
            if(trainer.seen.length()>INFERENCE_PARAMETERS-1)throw new IOException("BRE-Pair 2 coverage bounds");
            return trainer;
        }catch(IllegalArgumentException invalid){throw new IOException("Invalid BRE-Pair 2 trainer",invalid);}
    }
}
