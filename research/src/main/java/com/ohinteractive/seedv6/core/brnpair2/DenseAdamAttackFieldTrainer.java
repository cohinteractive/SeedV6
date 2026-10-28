package com.ohinteractive.seedv6.core.brnpair2;

import java.io.*;

/** Isolates optimizer semantics; evaluation and analytical gradients are unchanged. */
public final class DenseAdamAttackFieldTrainer {
    final boolean dense;
    final AttackFieldNetwork model;
    public DenseAdamAttackFieldTrainer(boolean dense,double rate,long seed){this(dense,new AttackFieldNetwork(true,rate,seed));}
    private DenseAdamAttackFieldTrainer(boolean dense,AttackFieldNetwork model){this.dense=dense;this.model=model;if(!model.allSquares)throw new IllegalArgumentException("All-square field required");}
    double train(AttackFieldNetwork.Features[] batch,double[] targets) {
        if(!dense)return model.train(batch,targets);
        double loss=model.gradients(batch,targets);if(model.step==Long.MAX_VALUE)throw new ArithmeticException("Step");model.step++;
        double c1=1-Math.pow(.9,model.step),c2=1-Math.pow(.999,model.step);
        for(int i=0;i<model.weights.length;i++) {
            double g=model.stamps[i]==model.serial?model.gradient[i]:0;
            model.weights[i]*=1-model.rate*.01;
            model.first[i]=.9*model.first[i]+.1*g;model.second[i]=.999*model.second[i]+.001*g*g;
            model.weights[i]-=model.rate*(model.first[i]/c1)/(Math.sqrt(model.second[i]/c2)+1e-8);
            if(!Double.isFinite(model.weights[i]))throw new ArithmeticException("Nonfinite dense field parameter");
            model.weights[i]=Math.max(-.99,Math.min(.99,model.weights[i]));
        }
        return loss;
    }
    void write(OutputStream stream)throws IOException {
        var payload=new ByteArrayOutputStream();model.write(payload);var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);
        out.writeLong(0x42524e4441463031L);out.writeInt(dense?1:0);out.writeInt(payload.size());payload.writeTo(out);out.flush();var crc=new java.util.zip.CRC32();crc.update(bytes.toByteArray());out.writeLong(crc.getValue());stream.write(bytes.toByteArray());
    }
    static DenseAdamAttackFieldTrainer read(InputStream stream)throws IOException {
        int length=64+24*AttackFieldNetwork.PARAMETERS;byte[] bytes=stream.readNBytes(length+1);if(bytes.length!=length)throw new IOException("Dense field state length");
        var crc=new java.util.zip.CRC32();crc.update(bytes,0,bytes.length-8);var in=new DataInputStream(new ByteArrayInputStream(bytes));
        if(in.readLong()!=0x42524e4441463031L)throw new IOException("Dense field magic");int flag=in.readInt(),payload=in.readInt();if(flag<0||flag>1||payload!=length-24)throw new IOException("Dense field geometry");
        var model=AttackFieldNetwork.read(new ByteArrayInputStream(in.readNBytes(payload)));if(in.readLong()!=crc.getValue()||!model.allSquares)throw new IOException("Dense field optimizer/CRC");return new DenseAdamAttackFieldTrainer(flag==1,model);
    }
}
