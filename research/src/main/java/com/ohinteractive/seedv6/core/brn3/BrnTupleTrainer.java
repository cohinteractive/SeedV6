package com.ohinteractive.seedv6.core.brn3;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn.BrnAdamConfig;
import com.ohinteractive.seedv6.training.nnue.NnueCorpusTargets;
import java.io.*;
import java.util.*;
import java.util.zip.CRC32;

/** C01 verification-only exact categorical tuple potentials. No production model identity. */
public final class BrnTupleTrainer {
    private static final long STATE_MAGIC=0x5336545550533031L,MODEL_MAGIC=0x53365455504d3031L;
    static final int[][] SHAPES={{0,0,1,0,2,0},{0,0,0,1,0,2},{0,0,1,1,2,2},
            {0,0,-1,1,-2,2},{0,0,1,0,0,1},{0,0,-1,0,0,1},{0,0,2,0,0,2},{0,0,3,0,0,3}};
    static final int[][] CELLS;
    static final int[] SHAPE;
    static {
        var cells=new ArrayList<int[]>();var shapes=new ArrayList<Integer>();
        for(int k=0;k<SHAPES.length;k++)for(int y=0;y<8;y++)for(int x=0;x<8;x++) {
            int[] row=new int[3];boolean valid=true;
            for(int j=0;j<3;j++){int a=x+SHAPES[k][2*j],b=y+SHAPES[k][2*j+1];valid&=a>=0&&a<8&&b>=0&&b<8;row[j]=8*b+a;}
            if(valid){cells.add(row);shapes.add(k);}
        }
        CELLS=cells.toArray(int[][]::new);SHAPE=shapes.stream().mapToInt(Integer::intValue).toArray();
        if(CELLS.length!=327)throw new AssertionError("C01 geometry count");
    }
    static final int TEMPLATES=CELLS.length;
    static final double NORM=1/Math.sqrt(2*TEMPLATES);
    final int order,states,modelParameters,trainingParameters,bias;
    final double[] weights,first,second,gradient;
    private final int[] touched,stamps,active;
    private final int[][] codes=new int[2][64];
    final BitSet seen=new BitSet();
    private final BrnAdamConfig config;
    private int serial,size;
    private long updates;

    public BrnTupleTrainer(int order,double rate) {
        requireOrder(order);this.order=order;states=order==2?169:2197;
        bias=2*TEMPLATES*states;modelParameters=bias+1;trainingParameters=modelParameters+2*SHAPES.length*states;
        config=new BrnAdamConfig(rate);weights=new double[trainingParameters];first=new double[trainingParameters];
        second=new double[trainingParameters];gradient=new double[trainingParameters];
        touched=new int[trainingParameters];stamps=new int[trainingParameters];active=new int[2*TEMPLATES];
    }
    static void requireOrder(int order){if(order!=2&&order!=3)throw new IllegalArgumentException("C01 order2 or3");}
    static int category(int code,int perspective){return code==0?0:Brn3Features.channel(code^(perspective<<3))+1;}
    static int state(int[] codes,int template,int order){int value=0;for(int j=order-1;j>=0;j--)value=13*value+codes[CELLS[template][j]];return value;}
    public int order(){return order;}
    public int parameters(){return trainingParameters;}
    public int inferenceParameters(){return modelParameters;}
    public long step(){return updates;}
    public double rate(){return config.learningRate();}
    public double predictPawns(long[] board) {
        for(int square=0;square<64;square++) {
            int code=Board.getSquare(board[0],board[1],board[2],board[3],square);
            for(int perspective=0;perspective<2;perspective++)codes[perspective][square^(perspective*56)]=category(code,perspective);
        }
        double sum=0;int stm=Board.player((int)board[4]);
        for(int role=0;role<2;role++)for(int t=0;t<TEMPLATES;t++) {
            int address=state(codes[stm^role],t,order);active[role*TEMPLATES+t]=address;
            sum+=weights[(role*TEMPLATES+t)*states+address]+weights[modelParameters+(role*SHAPES.length+SHAPE[t])*states+address];
        }
        double value=Brn3Features.material(board)+weights[bias]+NORM*sum;
        if(!Double.isFinite(value))throw new ArithmeticException("Nonfinite tuple");return value;
    }
    void resetGradient(){if(++serial==0){Arrays.fill(stamps,0);serial=1;}size=0;}
    private void addGradient(int i,double value){if(stamps[i]!=serial){stamps[i]=serial;gradient[i]=0;touched[size++]=i;}gradient[i]+=value;}
    void backward(double derivative) {
        addGradient(bias,derivative);
        for(int role=0;role<2;role++)for(int t=0;t<TEMPLATES;t++) {
            int address=active[role*TEMPLATES+t],absolute=(role*TEMPLATES+t)*states+address;
            addGradient(absolute,NORM*derivative);addGradient(modelParameters+(role*SHAPES.length+SHAPE[t])*states+address,NORM*derivative);
        }
    }
    public void trainBatch(long[][] boards,double[] targets,int batch) {
        if(batch<1||batch>boards.length||batch>targets.length)throw new IllegalArgumentException("batch");
        for(int i=0;i<batch;i++)if(boards[i]==null||!Double.isFinite(targets[i])||Math.abs(targets[i])>1)throw new IllegalArgumentException("example");
        resetGradient();
        for(int i=0;i<batch;i++) {
            double value=predictPawns(boards[i]);backward(Brn3Objective.crossEntropy(value,NnueCorpusTargets.material(boards[i]),targets[i])[1]);
            for(int j=0;j<active.length;j++)seen.set(j*states+active[j]);
        }
        if(updates==Long.MAX_VALUE)throw new ArithmeticException("step exhausted");updates++;
        double c1=1-Math.pow(.9,updates),c2=1-Math.pow(.999,updates);
        for(int k=0;k<size;k++) {
            int i=touched[k];double g=gradient[i]/batch;first[i]=.9*first[i]+.1*g;second[i]=.999*second[i]+.001*g*g;
            weights[i]-=rate()*(first[i]/c1)/(Math.sqrt(second[i]/c2)+1e-8);
            if(!Double.isFinite(weights[i]))throw new ArithmeticException("Nonfinite tuple update");
        }
    }
    /** Address coverage only; does not inspect validation labels or mutate training coverage. */
    public double unseenActiveFraction(long[][] boards) {
        if(boards.length==0)throw new IllegalArgumentException("empty coverage population");long unseen=0;
        for(var board:boards){predictPawns(board);for(int j=0;j<active.length;j++)if(!seen.get(j*states+active[j]))unseen++;}
        return (double)unseen/(boards.length*2L*TEMPLATES);
    }
    public int seenAbsoluteStates(){return seen.cardinality();}
    public Model snapshot() {
        float[] folded=new float[modelParameters];folded[bias]=(float)weights[bias];
        for(int role=0;role<2;role++)for(int t=0;t<TEMPLATES;t++)for(int s=0;s<states;s++)
            folded[(role*TEMPLATES+t)*states+s]=(float)(weights[(role*TEMPLATES+t)*states+s]+weights[modelParameters+(role*SHAPES.length+SHAPE[t])*states+s]);
        return new Model(order,folded);
    }
    public static final class Model {
        final int order,states;final float[] weights;
        Model(int order,float[] weights) {
            requireOrder(order);this.order=order;states=order==2?169:2197;
            if(weights.length!=2*TEMPLATES*states+1)throw new IllegalArgumentException("shape");
            this.weights=weights.clone();for(float v:weights)if(!Float.isFinite(v))throw new IllegalArgumentException("Nonfinite tuple model");
        }
        public int parameters(){return weights.length;}
        public BrnTupleWorkspace newWorkspace(){return new BrnTupleWorkspace(this);}
        public void write(OutputStream stream)throws IOException {
            var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);out.writeLong(MODEL_MAGIC);out.writeInt(order);
            for(float v:weights)out.writeFloat(v);finish(bytes,out,stream);
        }
        public static Model read(InputStream stream)throws IOException {
            var in=checked(stream);if(in.readLong()!=MODEL_MAGIC)throw new IOException("Wrong tuple model");
            try {int order=in.readInt();requireOrder(order);float[] values=new float[2*TEMPLATES*(order==2?169:2197)+1];
                for(int i=0;i<values.length;i++)values[i]=in.readFloat();end(in);return new Model(order,values);
            }catch(IllegalArgumentException e){throw new IOException("Invalid tuple model",e);}
        }
    }
    public void write(OutputStream stream)throws IOException {
        var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);out.writeLong(STATE_MAGIC);out.writeInt(order);out.writeLong(updates);out.writeDouble(rate());
        for(var array:new double[][]{weights,first,second})for(double v:array)out.writeDouble(v);
        long[] bits=seen.toLongArray();out.writeInt(bits.length);for(long v:bits)out.writeLong(v);finish(bytes,out,stream);
    }
    public static BrnTupleTrainer read(InputStream stream)throws IOException {
        var in=checked(stream);if(in.readLong()!=STATE_MAGIC)throw new IOException("Wrong tuple state");
        try {int order=in.readInt();long step=in.readLong();double rate=in.readDouble();if(step<0)throw new IOException("Negative step");
            var model=new BrnTupleTrainer(order,rate);model.updates=step;
            for(var array:new double[][]{model.weights,model.first,model.second})for(int i=0;i<array.length;i++) {
                double v=in.readDouble();if(!Double.isFinite(v)||array==model.second&&v<0)throw new IOException("Invalid optimizer");array[i]=v;
            }
            int length=in.readInt();if(length<0||length>(model.bias+63)/64)throw new IOException("Coverage length");
            long[] bits=new long[length];for(int i=0;i<length;i++)bits[i]=in.readLong();model.seen.or(BitSet.valueOf(bits));
            if(model.seen.length()>model.bias)throw new IOException("Coverage bounds");end(in);return model;
        }catch(IllegalArgumentException e){throw new IOException("Invalid tuple state",e);}
    }
    private static void end(DataInputStream in)throws IOException{in.readInt();if(in.read()!=-1)throw new IOException("Trailing tuple payload");}
    private static void finish(ByteArrayOutputStream bytes,DataOutputStream out,OutputStream stream)throws IOException {
        out.flush();var crc=new CRC32();crc.update(bytes.toByteArray());out.writeInt((int)crc.getValue());out.flush();bytes.writeTo(stream);
    }
    private static DataInputStream checked(InputStream stream)throws IOException {
        byte[] bytes=stream.readAllBytes();if(bytes.length<16)throw new IOException("Truncated tuple payload");
        var crc=new CRC32();crc.update(bytes,0,bytes.length-4);var tail=new DataInputStream(new ByteArrayInputStream(bytes,bytes.length-4,4));
        if(tail.readInt()!=(int)crc.getValue())throw new IOException("Tuple checksum");return new DataInputStream(new ByteArrayInputStream(bytes));
    }
}
