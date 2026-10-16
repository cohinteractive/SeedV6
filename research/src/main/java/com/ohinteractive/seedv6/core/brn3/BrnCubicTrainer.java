package com.ohinteractive.seedv6.core.brn3;

import static com.ohinteractive.seedv6.core.brn3.Brn3Layout.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn.BrnAdamConfig;
import com.ohinteractive.seedv6.training.nnue.NnueCorpusTargets;
import java.io.*;
import java.util.*;
import java.util.zip.CRC32;

/** Verification-only D01: unchanged BRN local encoder, nested quadratic plus cubic readout.
 * R=b+w.x+sum alpha*(u.x)*(v.x)+beta*(u.x)*(v.x)*(z.x).
 * No ordinary model identity, store registration or production selector uses this class. */
public final class BrnCubicTrainer {
    static final int INPUTS=2*POOL_WIDTH;
    private static final long STATE_MAGIC=0x5336424355425331L,MODEL_MAGIC=0x5336424355424d31L;
    private static final boolean VECTOR=ModuleLayer.boot().findModule("jdk.incubator.vector").isPresent()
            && !Boolean.getBoolean("seedv6.brn3.scalar");
    final int rank,factors,alpha,third,beta,output,modelParameters,trainingParameters;
    final double[] weights,first,second,gradient;
    private final int[] touched,stamps;
    private final int[][] entities=new int[2][64];
    private final int[][][] edges=new int[2][64][64];
    private final double[][][] local=new double[2][64][WIDTH],localGradient=new double[2][64][WIDTH];
    final double[][] pooled=new double[2][POOL_WIDTH];
    private final double[][] poolGradient=new double[2][POOL_WIDTH];
    private final double[] left,right,cubic;
    private final BrnAdamConfig config;
    private int count,serial,size;
    private double relationNorm,poolNorm;
    private long updates;

    public BrnCubicTrainer(int rank,long seed,double rate) {
        requireRank(rank);this.rank=rank;config=new BrnAdamConfig(rate);
        factors=DENSE+INPUTS;alpha=factors+2*rank*INPUTS;third=alpha+rank;beta=third+rank*INPUTS;output=beta+rank;
        modelParameters=output+1;trainingParameters=modelParameters+RELATIVE_PARAMETERS;
        weights=new double[trainingParameters];first=new double[trainingParameters];second=new double[trainingParameters];gradient=new double[trainingParameters];
        touched=new int[trainingParameters/WIDTH+1];stamps=new int[touched.length];left=new double[rank];right=new double[rank];cubic=new double[rank];
        var random=new Random(seed);
        // Exactly the current BRN3 encoder initialization, including RNG consumption.
        for(int i=0;i<DENSE;i++)weights[i]=(2*random.nextDouble()-1)*.1;
        for(int i=factors;i<alpha;i++)weights[i]=(2*random.nextDouble()-1)*Math.sqrt(3.0/INPUTS);
        for(int i=third;i<beta;i++)weights[i]=(2*random.nextDouble()-1)*Math.sqrt(3.0/INPUTS);
        // Preserve B01 u/v initialization, then initialize z. All heads/bias are zero.
    }
    static void requireRank(int rank){if(rank!=8)throw new IllegalArgumentException("D01 frozen rank8");}
    public int parameters(){return trainingParameters;}
    public int inferenceParameters(){return modelParameters;}
    public int rank(){return rank;}
    public long step(){return updates;}
    public double rate(){return config.learningRate();}
    public double predictPawns(long[] board) {
        int stm=Board.player((int)board[4]);
        for(int p=0;p<2;p++) {
            int perspective=stm^p;count=0;Arrays.fill(pooled[p],0);
            for(long bits=board[0]|board[1]|board[2];bits!=0;bits&=bits-1) {
                int square=Long.numberOfTrailingZeros(bits);
                int entity=Brn3Features.channel(Board.getSquare(board[0],board[1],board[2],board[3],square)^(perspective<<3))*64+(square^(perspective*56));
                entities[p][count]=entity;System.arraycopy(weights,NODES+entity*WIDTH,local[p][count++],0,WIDTH);
            }
            relationNorm=1/Math.sqrt(Math.max(1,count-1));poolNorm=1/Math.sqrt(Math.max(1,count));
            for(int i=0;i<count;i++)for(int j=i+1;j<count;j++) {
                int row=Brn3Layout.edge(entities[p][i],entities[p][j]);edges[p][i][j]=row;
                int shared=modelParameters+Brn3Trainer.RELATIVE_ROW[row/WIDTH]*WIDTH;
                if(VECTOR)Brn3VectorKernels.forward(weights,row,shared,relationNorm,local[p][i],local[p][j]);
                else for(int c=0;c<WIDTH;c++){double message=relationNorm*(weights[row+c]+weights[shared+c]);local[p][i][c]+=message;local[p][j][c]+=message;}
            }
            for(int i=0;i<count;i++)for(int c=0;c<WIDTH;c++) {
                local[p][i][c]=Math.max(0,local[p][i][c]);pooled[p][entities[p][i]/64*WIDTH+c]+=poolNorm*local[p][i][c];
            }
        }
        double value=weights[output];
        for(int p=0;p<2;p++)for(int c=0;c<POOL_WIDTH;c++)value+=weights[DENSE+p*POOL_WIDTH+c]*pooled[p][c];
        for(int r=0;r<rank;r++) {
            double u=0,v=0,z=0;int base=factors+2*r*INPUTS;
            for(int p=0;p<2;p++)for(int c=0;c<POOL_WIDTH;c++){double x=pooled[p][c];int j=p*POOL_WIDTH+c;u+=weights[base+j]*x;v+=weights[base+INPUTS+j]*x;z+=weights[third+r*INPUTS+j]*x;}
            left[r]=u;right[r]=v;cubic[r]=z;value+=weights[alpha+r]*u*v+weights[beta+r]*u*v*z;
        }
        value+=Brn3Features.material(board);if(!Double.isFinite(value))throw new ArithmeticException("Nonfinite cubic");return value;
    }
    void resetGradient(){if(++serial==0){Arrays.fill(stamps,0);serial=1;}size=0;Arrays.fill(gradient,DENSE,modelParameters,0);}
    private void touchRow(int base) {int row=base/WIDTH;if(stamps[row]!=serial){stamps[row]=serial;Arrays.fill(gradient,base,base+WIDTH,0);touched[size++]=base;}}
    void backward(double derivative) {
        gradient[output]+=derivative;
        for(int p=0;p<2;p++)for(int c=0;c<POOL_WIDTH;c++) {
            int j=p*POOL_WIDTH+c;gradient[DENSE+j]+=derivative*pooled[p][c];poolGradient[p][c]=derivative*weights[DENSE+j];
        }
        for(int r=0;r<rank;r++) {
            gradient[alpha+r]+=derivative*left[r]*right[r];gradient[beta+r]+=derivative*left[r]*right[r]*cubic[r];int base=factors+2*r*INPUTS;
            double coefficient=weights[alpha+r]+weights[beta+r]*cubic[r];
            double du=derivative*coefficient*right[r],dv=derivative*coefficient*left[r],dz=derivative*weights[beta+r]*left[r]*right[r];
            for(int p=0;p<2;p++)for(int c=0;c<POOL_WIDTH;c++) {
                int j=p*POOL_WIDTH+c;gradient[base+j]+=du*pooled[p][c];gradient[base+INPUTS+j]+=dv*pooled[p][c];gradient[third+r*INPUTS+j]+=dz*pooled[p][c];
                poolGradient[p][c]+=du*weights[base+j]+dv*weights[base+INPUTS+j]+dz*weights[third+r*INPUTS+j];
            }
        }
        for(int p=0;p<2;p++) {
            for(int i=0;i<count;i++) {
                int node=NODES+entities[p][i]*WIDTH;touchRow(node);
                for(int c=0;c<WIDTH;c++){double g=poolGradient[p][entities[p][i]/64*WIDTH+c]*poolNorm*(local[p][i][c]>0?1:0);localGradient[p][i][c]=g;gradient[node+c]+=g;}
            }
            for(int i=0;i<count;i++)for(int j=i+1;j<count;j++) {
                int row=edges[p][i][j],shared=modelParameters+Brn3Trainer.RELATIVE_ROW[row/WIDTH]*WIDTH;touchRow(row);touchRow(shared);
                if(VECTOR)Brn3VectorKernels.backward(gradient,row,shared,relationNorm,localGradient[p][i],localGradient[p][j]);
                else for(int c=0;c<WIDTH;c++){double g=relationNorm*(localGradient[p][i][c]+localGradient[p][j][c]);gradient[row+c]+=g;gradient[shared+c]+=g;}
            }
        }
    }
    public void trainBatch(long[][] boards,double[] targets,int batch) {
        if(batch<1||batch>boards.length||batch>targets.length)throw new IllegalArgumentException("batch");
        for(int i=0;i<batch;i++)if(boards[i]==null||!Double.isFinite(targets[i])||Math.abs(targets[i])>1)throw new IllegalArgumentException("example");
        resetGradient();
        for(int i=0;i<batch;i++){double value=predictPawns(boards[i]);backward(Brn3Objective.crossEntropy(value,NnueCorpusTargets.material(boards[i]),targets[i])[1]);}
        if(updates==Long.MAX_VALUE)throw new ArithmeticException("step exhausted");updates++;
        double c1=1-Math.pow(.9,updates),c2=1-Math.pow(.999,updates);
        updateRange(DENSE,modelParameters,batch,c1,c2);for(int i=0;i<size;i++)updateRange(touched[i],touched[i]+WIDTH,batch,c1,c2);
    }
    private void updateRange(int start,int end,int batch,double c1,double c2) {
        if(VECTOR){Brn3VectorKernels.update(weights,first,second,gradient,start,end,batch,c1,c2,.1,.001,config);return;}
        for(int i=start;i<end;i++){double g=gradient[i]/batch;first[i]=.9*first[i]+.1*g;second[i]=.999*second[i]+.001*g*g;
            weights[i]-=config.learningRate()*(first[i]/c1)/(Math.sqrt(second[i]/c2)+1e-8);if(!Double.isFinite(weights[i]))throw new ArithmeticException("Nonfinite update");}
    }
    public Model snapshot() {
        float[] folded=new float[modelParameters];for(int i=0;i<folded.length;i++)folded[i]=(float)weights[i];
        for(int row=0;row<EDGE_ROWS;row++)for(int c=0;c<WIDTH;c++)folded[row*WIDTH+c]=(float)(weights[row*WIDTH+c]+weights[modelParameters+Brn3Trainer.RELATIVE_ROW[row]*WIDTH+c]);
        return new Model(rank,folded);
    }
    public static final class Model {
        final int rank;final float[] weights;
        Model(int rank,float[] weights){requireRank(rank);if(weights.length!=DENSE+INPUTS+3*rank*INPUTS+2*rank+1)throw new IllegalArgumentException("shape");
            this.rank=rank;this.weights=weights.clone();for(float v:weights)if(!Float.isFinite(v))throw new IllegalArgumentException("Nonfinite model");}
        public BrnCubicWorkspace newWorkspace(){return new BrnCubicWorkspace(this);}
        /** Post-training component ablation; encoder and quadratic terms stay frozen. */
        public Model withoutCubic() {
            float[] copy=weights.clone();int beta=DENSE+INPUTS+3*rank*INPUTS+rank;
            Arrays.fill(copy,beta,beta+rank,0);return new Model(rank,copy);
        }
        public int parameters(){return weights.length;}
        public void write(OutputStream stream)throws IOException {
            var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);out.writeLong(MODEL_MAGIC);out.writeInt(rank);for(float v:weights)out.writeFloat(v);finish(bytes,out,stream);
        }
        public static Model read(InputStream stream)throws IOException {
            byte[] bytes=checked(stream);var in=new DataInputStream(new ByteArrayInputStream(bytes));if(in.readLong()!=MODEL_MAGIC)throw new IOException("wrong cubic model");
            try {int rank=in.readInt();requireRank(rank);float[] values=new float[DENSE+INPUTS+3*rank*INPUTS+2*rank+1];for(int i=0;i<values.length;i++)values[i]=in.readFloat();
                in.readInt();if(in.read()!=-1)throw new IOException("Trailing cubic model");return new Model(rank,values);
            }catch(IllegalArgumentException e){throw new IOException("Invalid cubic model",e);}
        }
    }
    public void write(OutputStream stream)throws IOException {
        var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);out.writeLong(STATE_MAGIC);out.writeInt(rank);out.writeLong(updates);out.writeDouble(rate());
        for(var array:new double[][]{weights,first,second})for(double v:array)out.writeDouble(v);finish(bytes,out,stream);
    }
    public static BrnCubicTrainer read(InputStream stream)throws IOException {
        byte[] bytes=checked(stream);var in=new DataInputStream(new ByteArrayInputStream(bytes));if(in.readLong()!=STATE_MAGIC)throw new IOException("wrong cubic state");
        try {int rank=in.readInt();long step=in.readLong();double rate=in.readDouble();if(step<0)throw new IOException("Negative step");
            var model=new BrnCubicTrainer(rank,0,rate);model.updates=step;
            for(var array:new double[][]{model.weights,model.first,model.second})for(int i=0;i<array.length;i++){double v=in.readDouble();if(!Double.isFinite(v)||array==model.second&&v<0)throw new IOException("Invalid optimizer");array[i]=v;}
            in.readInt();if(in.read()!=-1)throw new IOException("Trailing cubic state");return model;
        }catch(IllegalArgumentException e){throw new IOException("Invalid cubic state",e);}
    }
    private static void finish(ByteArrayOutputStream bytes,DataOutputStream out,OutputStream stream)throws IOException {
        out.flush();var crc=new CRC32();crc.update(bytes.toByteArray());out.writeInt((int)crc.getValue());out.flush();bytes.writeTo(stream);
    }
    private static byte[] checked(InputStream stream)throws IOException {
        byte[] bytes=stream.readAllBytes();if(bytes.length<16)throw new IOException("Truncated cubic payload");var crc=new CRC32();crc.update(bytes,0,bytes.length-4);
        var tail=new DataInputStream(new ByteArrayInputStream(bytes,bytes.length-4,4));if(tail.readInt()!=(int)crc.getValue())throw new IOException("Energy checksum");return bytes;
    }
}
