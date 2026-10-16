package com.ohinteractive.seedv6.core.brn3;

import static com.ohinteractive.seedv6.core.brn3.Brn3Layout.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn.BrnAdamConfig;
import com.ohinteractive.seedv6.training.nnue.NnueCorpusTargets;
import java.io.*;
import java.util.*;
import java.util.zip.CRC32;

/** Verification-only E01: unchanged BRN local encoder, learned univariate piecewise-linear edge functions.
 * R=b+sum_j f_j(x_j),16 knots spaced .5 with a linear tail and fixed f_j(0)=0.
 * No ordinary model identity, store registration or production selector uses this class. */
public final class BrnSplineTrainer {
    static final int INPUTS=2*POOL_WIDTH,KNOTS=16;
    static final double STEP=.5;
    private static final long STATE_MAGIC=0x533642504c535332L,MODEL_MAGIC=0x533642504c534d32L;
    private static final boolean VECTOR=ModuleLayer.boot().findModule("jdk.incubator.vector").isPresent()
            && !Boolean.getBoolean("seedv6.brn3.scalar");
    final int knots,output,modelParameters,trainingParameters;
    final double spacing;
    final double[] weights,first,second,gradient;
    private final int[] touched,stamps;
    private final int[][] entities=new int[2][64];
    private final int[][][] edges=new int[2][64][64];
    private final double[][][] local=new double[2][64][WIDTH],localGradient=new double[2][64][WIDTH];
    final double[][] pooled=new double[2][POOL_WIDTH];
    private final double[][] poolGradient=new double[2][POOL_WIDTH];
    private final BrnAdamConfig config;
    private int count,serial,size;
    private double relationNorm,poolNorm;
    private long updates;

    public BrnSplineTrainer(int knots,long seed,double rate) {
        this(knots,seed,rate,STEP);
    }
    public BrnSplineTrainer(int knots,long seed,double rate,double spacing) {
        requireKnots(knots);requireSpacing(spacing);this.knots=knots;this.spacing=spacing;config=new BrnAdamConfig(rate);
        output=DENSE+INPUTS*KNOTS;
        modelParameters=output+1;trainingParameters=modelParameters+RELATIVE_PARAMETERS;
        weights=new double[trainingParameters];first=new double[trainingParameters];second=new double[trainingParameters];gradient=new double[trainingParameters];
        touched=new int[trainingParameters/WIDTH+1];stamps=new int[touched.length];
        var random=new Random(seed);
        // Exactly the current BRN3 encoder initialization, including RNG consumption.
        for(int i=0;i<DENSE;i++)weights[i]=(2*random.nextDouble()-1)*.1;
        // Every learned knot and output bias is zero: exact fixed-material Gen0.
    }
    static void requireKnots(int knots){if(knots!=KNOTS)throw new IllegalArgumentException("E01 frozen16 knots");}
    static void requireSpacing(double spacing){if(spacing!=.5&&spacing!=.125)throw new IllegalArgumentException("E01 spacing .5 or .125");}
    public int parameters(){return trainingParameters;}
    public int inferenceParameters(){return modelParameters;}
    public int knots(){return knots;}
    public double spacing(){return spacing;}
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
        for(int p=0;p<2;p++)for(int c=0;c<POOL_WIDTH;c++)value+=curve(weights,DENSE+(p*POOL_WIDTH+c)*KNOTS,pooled[p][c],spacing);
        value+=Brn3Features.material(board);if(!Double.isFinite(value))throw new ArithmeticException("Nonfinite edge-function");return value;
    }
    static int segment(double x,double spacing){return Math.min(KNOTS-1,(int)(x/spacing));}
    static double curve(double[] weights,int base,double x) {
        return curve(weights,base,x,STEP);
    }
    static double curve(double[] weights,int base,double x,double spacing) {
        int k=segment(x,spacing);double fraction=x/spacing-k,low=k==0?0:weights[base+k-1],high=weights[base+k];
        return spacing*(low+(high-low)*fraction);
    }
    public double maximumPooled(){double maximum=0;for(var row:pooled)for(double x:row)maximum=Math.max(maximum,x);return maximum;}
    public int pooledAboveGrid(){int n=0;for(var row:pooled)for(double x:row)if(x>KNOTS*spacing)n++;return n;}
    void resetGradient(){if(++serial==0){Arrays.fill(stamps,0);serial=1;}size=0;Arrays.fill(gradient,DENSE,modelParameters,0);}
    private void touchRow(int base) {int row=base/WIDTH;if(stamps[row]!=serial){stamps[row]=serial;Arrays.fill(gradient,base,base+WIDTH,0);touched[size++]=base;}}
    void backward(double derivative) {
        gradient[output]+=derivative;
        for(int p=0;p<2;p++)for(int c=0;c<POOL_WIDTH;c++) {
            double x=pooled[p][c];int k=segment(x,spacing),base=DENSE+(p*POOL_WIDTH+c)*KNOTS;double fraction=x/spacing-k;
            double low=k==0?0:weights[base+k-1],high=weights[base+k];
            if(k>0)gradient[base+k-1]+=derivative*spacing*(1-fraction);
            gradient[base+k]+=derivative*spacing*fraction;
            poolGradient[p][c]=derivative*(high-low); // STEP cancels the knot spacing.
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
        return new Model(knots,spacing,folded);
    }
    public static final class Model {
        final int knots;final double spacing;final float[] weights;
        Model(int knots,double spacing,float[] weights){requireKnots(knots);requireSpacing(spacing);if(weights.length!=DENSE+INPUTS*KNOTS+1)throw new IllegalArgumentException("shape");
            this.knots=knots;this.spacing=spacing;this.weights=weights.clone();for(float v:weights)if(!Float.isFinite(v))throw new IllegalArgumentException("Nonfinite model");}
        public BrnSplineWorkspace newWorkspace(){return new BrnSplineWorkspace(this);}
        /** Extend each learned first segment linearly; retain encoder and scalar bias. */
        public Model linearized() {
            float[] copy=weights.clone();for(int j=0;j<INPUTS;j++)for(int k=1;k<=KNOTS;k++)copy[DENSE+j*KNOTS+k-1]=k*weights[DENSE+j*KNOTS];
            return new Model(knots,spacing,copy);
        }
        public int parameters(){return weights.length;}
        public void write(OutputStream stream)throws IOException {
            var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);out.writeLong(MODEL_MAGIC);out.writeInt(knots);out.writeDouble(spacing);for(float v:weights)out.writeFloat(v);finish(bytes,out,stream);
        }
        public static Model read(InputStream stream)throws IOException {
            byte[] bytes=checked(stream);var in=new DataInputStream(new ByteArrayInputStream(bytes));long magic=in.readLong();if(magic!=MODEL_MAGIC&&magic!=MODEL_MAGIC-1)throw new IOException("wrong edge-function model");
            try {int knots=in.readInt();requireKnots(knots);double spacing=magic==MODEL_MAGIC?in.readDouble():STEP;float[] values=new float[DENSE+INPUTS*KNOTS+1];for(int i=0;i<values.length;i++)values[i]=in.readFloat();
                in.readInt();if(in.read()!=-1)throw new IOException("Trailing edge-function model");return new Model(knots,spacing,values);
            }catch(IllegalArgumentException e){throw new IOException("Invalid edge-function model",e);}
        }
    }
    public void write(OutputStream stream)throws IOException {
        var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);out.writeLong(STATE_MAGIC);out.writeInt(knots);out.writeDouble(spacing);out.writeLong(updates);out.writeDouble(rate());
        for(var array:new double[][]{weights,first,second})for(double v:array)out.writeDouble(v);finish(bytes,out,stream);
    }
    public static BrnSplineTrainer read(InputStream stream)throws IOException {
        byte[] bytes=checked(stream);var in=new DataInputStream(new ByteArrayInputStream(bytes));long magic=in.readLong();if(magic!=STATE_MAGIC&&magic!=STATE_MAGIC-1)throw new IOException("wrong edge-function state");
        try {int knots=in.readInt();double spacing=magic==STATE_MAGIC?in.readDouble():STEP;long step=in.readLong();double rate=in.readDouble();if(step<0)throw new IOException("Negative step");
            var model=new BrnSplineTrainer(knots,0,rate,spacing);model.updates=step;
            for(var array:new double[][]{model.weights,model.first,model.second})for(int i=0;i<array.length;i++){double v=in.readDouble();if(!Double.isFinite(v)||array==model.second&&v<0)throw new IOException("Invalid optimizer");array[i]=v;}
            in.readInt();if(in.read()!=-1)throw new IOException("Trailing edge-function state");return model;
        }catch(IllegalArgumentException e){throw new IOException("Invalid edge-function state",e);}
    }
    private static void finish(ByteArrayOutputStream bytes,DataOutputStream out,OutputStream stream)throws IOException {
        out.flush();var crc=new CRC32();crc.update(bytes.toByteArray());out.writeInt((int)crc.getValue());out.flush();bytes.writeTo(stream);
    }
    private static byte[] checked(InputStream stream)throws IOException {
        byte[] bytes=stream.readAllBytes();if(bytes.length<16)throw new IOException("Truncated edge-function payload");var crc=new CRC32();crc.update(bytes,0,bytes.length-4);
        var tail=new DataInputStream(new ByteArrayInputStream(bytes,bytes.length-4,4));if(tail.readInt()!=(int)crc.getValue())throw new IOException("Energy checksum");return bytes;
    }
}
