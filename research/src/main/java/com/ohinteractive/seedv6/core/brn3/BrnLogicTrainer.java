package com.ohinteractive.seedv6.core.brn3;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn.BrnAdamConfig;
import com.ohinteractive.seedv6.training.nnue.NnueCorpusTargets;
import java.io.*;
import java.util.*;
import java.util.zip.CRC32;

/** G01 verification-only spatial Boolean circuits. Soft gate learning, hard bitboard deployment. */
public final class BrnLogicTrainer {
    static final int WIDTH=32,RAW=13,FUNCTIONS=16,REGIONS=16;
    static final double NORM=1/Math.sqrt(WIDTH*REGIONS);
    private static final long STATE_MAGIC=0x53364c4f47533032L,MODEL_MAGIC=0x53364c4f474d3031L;
    static final double[][] TRUTH=new double[16][4];
    static final long[] MASKS=new long[16];
    static {
        for(int op=0;op<16;op++) {
            double z=(op>>>3)&1,a=((op>>>1)&1)-z,b=((op>>>2)&1)-z;
            TRUTH[op]=new double[]{z,a,b,(op&1)-z-a-b};
        }
        for(int i=0;i<8;i++){MASKS[i]=255L<<(8*i);MASKS[8+i]=0x0101010101010101L<<i;}
    }
    final int layers,gates,head,bias;
    final int[][] topology;
    final double[] weights,first,second,gradient;
    final double[][] probabilities,coefficients,coefficientGradient;
    private final int[][][] sourceCells;
    private final long[][] raw=new long[2][RAW],bits;
    final double[][][] activations;
    private final double[][][] activationGradient;
    private final double[] features;
    private final BrnAdamConfig config;
    private final double headRate;
    private boolean hardened;
    private double hardRate;
    private int[] frozenOps;
    private int stm;
    private long updates;

    public BrnLogicTrainer(int layers,long seed,double rate) {
        this(layers,seed,rate,rate);
    }
    public BrnLogicTrainer(int layers,long seed,double rate,double headRate) {
        requireLayers(layers);this.layers=layers;gates=layers*WIDTH;head=gates*FUNCTIONS;bias=head+2*gates*REGIONS;
        config=new BrnAdamConfig(rate);new BrnAdamConfig(headRate);this.headRate=headRate;hardRate=headRate;weights=new double[bias+1];first=new double[weights.length];second=new double[weights.length];gradient=new double[weights.length];
        topology=new int[gates][6];var graph=new Random(seed^0x5eec6ab19L);var initial=new Random(seed^0x4938ac710L);
        for(int g=0;g<gates;g++) {
            int layer=g/WIDTH,inputs=layer==0?RAW:WIDTH,radius=layer==0?2:1;
            for(int k:new int[]{0,3}){topology[g][k]=graph.nextInt(inputs);topology[g][k+1]=graph.nextInt(2*radius+1)-radius;topology[g][k+2]=graph.nextInt(2*radius+1)-radius;}
            for(int j=0;j<16;j++)weights[g*16+j]=initial.nextGaussian();
        }
        sourceCells=cells(topology);activations=new double[2][gates][64];activationGradient=new double[2][gates][64];bits=new long[2][gates];
        probabilities=new double[gates][16];coefficients=new double[gates][4];coefficientGradient=new double[gates][4];features=new double[2*gates*REGIONS];
    }
    static void requireLayers(int layers){if(layers<1||layers>2)throw new IllegalArgumentException("G01 one/two layers");}
    static int[][][] cells(int[][] topology) {
        int[][][] cells=new int[topology.length][2][64];
        for(int g=0;g<topology.length;g++)for(int side=0;side<2;side++)for(int sq=0;sq<64;sq++) {
            int x=(sq&7)-topology[g][side*3+1],y=(sq>>>3)-topology[g][side*3+2];
            cells[g][side][sq]=x<0||x>=8||y<0||y>=8?-1:y*8+x;
        }
        return cells;
    }
    static void rawMaps(long[] board,long[][] out) {
        Arrays.fill(out[0],0);long occupied=board[0]|board[1]|board[2];out[0][0]=~occupied;
        for(long left=occupied;left!=0;left&=left-1) {
            int sq=Long.numberOfTrailingZeros(left),code=Board.getSquare(board[0],board[1],board[2],board[3],sq);
            out[0][1+Brn3Features.channel(code)]|=1L<<sq;
        }
        out[1][0]=Long.reverseBytes(out[0][0]);
        for(int c=0;c<12;c++)out[1][1+c]=Long.reverseBytes(out[0][1+(c+6)%12]);
    }
    /** Shift source toward dx/dy; off-board source cells are zero, with no file wrap. */
    static long shift(long value,int dx,int dy) {
        if(dx>0){value&=~(0x8080808080808080L|(dx==2?0x4040404040404040L:0));value<<=dx;}
        else if(dx<0){value&=~(0x0101010101010101L|(dx==-2?0x0202020202020202L:0));value>>>=-dx;}
        return dy>0?value<<(8*dy):dy<0?value>>>(-8*dy):value;
    }
    static long gate(int op,long a,long b) {
        return switch(op) {
            case 0->0;case 1->a&b;case 2->a&~b;case 3->a;case 4->~a&b;case 5->b;
            case 6->a^b;case 7->a|b;case 8->~(a|b);case 9->~(a^b);case 10->~b;
            case 11->a|~b;case 12->~a;case 13->~a|b;case 14->~(a&b);case 15->-1L;
            default->throw new IllegalArgumentException("operator");
        };
    }
    static void hardMaps(long[][] raw,long[][] output,int[][] topology,int[] ops) {
        for(int p=0;p<2;p++)for(int g=0;g<topology.length;g++) {
            int layer=g/WIDTH,base=(layer-1)*WIDTH;int[] t=topology[g];
            long a=layer==0?raw[p][t[0]]:output[p][base+t[0]],b=layer==0?raw[p][t[3]]:output[p][base+t[3]];
            output[p][g]=gate(ops[g],shift(a,t[1],t[2]),shift(b,t[4],t[5]));
        }
    }
    void prepareCoefficients() {
        for(int g=0;g<gates;g++) {
            double max=Double.NEGATIVE_INFINITY,total=0;for(int j=0;j<16;j++)max=Math.max(max,weights[g*16+j]);
            for(int j=0;j<16;j++){probabilities[g][j]=Math.exp(weights[g*16+j]-max);total+=probabilities[g][j];}
            Arrays.fill(coefficients[g],0);
            for(int j=0;j<16;j++){probabilities[g][j]/=total;for(int c=0;c<4;c++)coefficients[g][c]+=probabilities[g][j]*TRUTH[j][c];}
        }
    }
    private double source(int p,int g,int side,int square) {
        int sq=sourceCells[g][side][square];if(sq<0)return 0;
        int channel=topology[g][side*3],layer=g/WIDTH;
        return layer==0?(raw[p][channel]>>>sq)&1:activations[p][(layer-1)*WIDTH+channel][sq];
    }
    private double softForward(long[] board) {
        rawMaps(board,raw);stm=Board.player((int)board[4]);Arrays.fill(features,0);
        for(int p=0;p<2;p++)for(int g=0;g<gates;g++) {
            double[] c=coefficients[g];int base=((stm^p)*gates+g)*REGIONS;
            for(int sq=0;sq<64;sq++) {
                double a=source(p,g,0,sq),b=source(p,g,1,sq),v=c[0]+c[1]*a+c[2]*b+c[3]*a*b;
                activations[p][g][sq]=v;features[base+(sq>>>3)]+=NORM*v;features[base+8+(sq&7)]+=NORM*v;
            }
        }
        return readout(board);
    }
    private double hardForward(long[] board) {
        rawMaps(board,raw);stm=Board.player((int)board[4]);hardMaps(raw,bits,topology,operators());
        for(int p=0;p<2;p++)for(int g=0;g<gates;g++)for(int region=0;region<REGIONS;region++)
            features[((stm^p)*gates+g)*REGIONS+region]=NORM*Long.bitCount(bits[p][g]&MASKS[region]);
        return readout(board);
    }
    private double readout(long[] board) {
        double value=Brn3Features.material(board)+weights[bias];for(int i=0;i<features.length;i++)value+=weights[head+i]*features[i];
        if(!Double.isFinite(value))throw new ArithmeticException("Nonfinite logic value");return value;
    }
    public double predictSoftPawns(long[] board){prepareCoefficients();return softForward(board);}
    public double predictHardPawns(long[] board){return hardForward(board);}
    public double predictPawns(long[] board){return hardened?hardForward(board):predictSoftPawns(board);}
    void resetGradient(){Arrays.fill(gradient,0);for(var c:coefficientGradient)Arrays.fill(c,0);}
    private void backwardHead(double derivative) {
        gradient[bias]+=derivative;for(int i=0;i<features.length;i++)gradient[head+i]+=derivative*features[i];
    }
    void backwardSoft(double derivative) {
        backwardHead(derivative);
        for(int p=0;p<2;p++)for(int g=0;g<gates;g++) {
            int base=head+((stm^p)*gates+g)*REGIONS;
            for(int sq=0;sq<64;sq++)activationGradient[p][g][sq]=derivative*NORM*(weights[base+(sq>>>3)]+weights[base+8+(sq&7)]);
        }
        for(int p=0;p<2;p++)for(int g=gates-1;g>=0;g--) {
            double[] c=coefficients[g],cg=coefficientGradient[g];int layer=g/WIDTH;
            for(int sq=0;sq<64;sq++) {
                double grad=activationGradient[p][g][sq],a=source(p,g,0,sq),b=source(p,g,1,sq);
                cg[0]+=grad;cg[1]+=grad*a;cg[2]+=grad*b;cg[3]+=grad*a*b;
                if(layer>0) {
                    int ia=sourceCells[g][0][sq],ib=sourceCells[g][1][sq],base=(layer-1)*WIDTH;
                    if(ia>=0)activationGradient[p][base+topology[g][0]][ia]+=grad*(c[1]+c[3]*b);
                    if(ib>=0)activationGradient[p][base+topology[g][3]][ib]+=grad*(c[2]+c[3]*a);
                }
            }
        }
    }
    void finishGateGradient() {
        for(int g=0;g<gates;g++)for(int j=0;j<16;j++) {
            double v=0;for(int c=0;c<4;c++)v+=coefficientGradient[g][c]*(TRUTH[j][c]-coefficients[g][c]);
            gradient[g*16+j]+=probabilities[g][j]*v;
        }
    }
    public void trainBatch(long[][] boards,double[] targets,int batch) {
        if(batch<1||batch>boards.length||batch>targets.length)throw new IllegalArgumentException("batch");
        for(int i=0;i<batch;i++)if(boards[i]==null||!Double.isFinite(targets[i])||Math.abs(targets[i])>1)throw new IllegalArgumentException("example");
        resetGradient();if(!hardened)prepareCoefficients();
        for(int i=0;i<batch;i++) {
            double value=hardened?hardForward(boards[i]):softForward(boards[i]);
            double derivative=Brn3Objective.crossEntropy(value,NnueCorpusTargets.material(boards[i]),targets[i])[1];
            if(hardened)backwardHead(derivative);else backwardSoft(derivative);
        }
        if(!hardened)finishGateGradient();if(updates==Long.MAX_VALUE)throw new ArithmeticException("step exhausted");updates++;
        double c1=1-Math.pow(.9,updates),c2=1-Math.pow(.999,updates);
        for(int i=hardened?head:0;i<weights.length;i++) {
            double g=gradient[i]/batch;first[i]=.9*first[i]+.1*g;second[i]=.999*second[i]+.001*g*g;
            weights[i]-=(hardened?hardRate:i<head?rate():headRate)*(first[i]/c1)/(Math.sqrt(second[i]/c2)+1e-8);
            if(!Double.isFinite(weights[i]))throw new ArithmeticException("Nonfinite logic update");
        }
    }
    int[] operators() {
        if(hardened)return frozenOps;
        int[] ops=new int[gates];for(int g=0;g<gates;g++){int best=0;for(int j=1;j<16;j++)if(weights[g*16+j]>weights[g*16+best])best=j;ops[g]=best;}return ops;
    }
    public void harden(){harden(headRate);}
    public void harden(double rate) {
        new BrnAdamConfig(rate);
        if(hardened){if(rate!=hardRate)throw new IllegalArgumentException("Cannot change an existing hard phase");return;}
        frozenOps=operators();hardRate=rate;hardened=true;
    }
    public boolean hardened(){return hardened;}
    /** Explicit scheduled head-only rate transition; frozen gates and all optimizer moments are retained. */
    public void refitRate(double rate) {
        if(!hardened)throw new IllegalStateException("Head-only schedule requires frozen gates");
        new BrnAdamConfig(rate);hardRate=rate;
    }
    public double hardRate(){return hardRate;}
    public double headRate(){return headRate;}
    public int layers(){return layers;}
    public int parameters(){return weights.length;}
    public int inferenceParameters(){return weights.length-head;}
    public long step(){return updates;}
    public double rate(){return config.learningRate();}
    public Map<String,Object> diagnostics() {
        prepareCoefficients();double modal=0;int constant=0,unary=0;var ops=operators();int[] counts=new int[16];
        for(int g=0;g<gates;g++){modal+=probabilities[g][ops[g]];counts[ops[g]]++;if(ops[g]==0||ops[g]==15)constant++;if(ops[g]==3||ops[g]==5||ops[g]==10||ops[g]==12)unary++;}
        return Map.of("meanModalProbability",modal/gates,"constantGates",constant,"unaryGates",unary,"operatorCounts",counts,"modalOperators",ops.clone(),"hardened",hardened);
    }
    public Model snapshot() {
        float[] values=new float[weights.length-head];for(int i=0;i<values.length;i++)values[i]=(float)weights[head+i];
        return new Model(layers,topology,operators(),values);
    }
    static void validateTopology(int layers,int[][] t) {
        requireLayers(layers);if(t.length!=layers*WIDTH)throw new IllegalArgumentException("topology size");
        for(int g=0;g<t.length;g++){if(t[g].length!=6)throw new IllegalArgumentException("topology row");int range=g<WIDTH?2:1,n=g<WIDTH?RAW:WIDTH;
            for(int k:new int[]{0,3})if(t[g][k]<0||t[g][k]>=n||t[g][k+1]<-range||t[g][k+1]>range||t[g][k+2]<-range||t[g][k+2]>range)throw new IllegalArgumentException("topology value");}
    }
    public static final class Model {
        final int layers;final int[][] topology;final int[] ops;final float[] weights;
        Model(int layers,int[][] topology,int[] ops,float[] weights) {
            validateTopology(layers,topology);if(ops.length!=layers*WIDTH||weights.length!=2*layers*WIDTH*REGIONS+1)throw new IllegalArgumentException("model shape");
            this.layers=layers;this.topology=Arrays.stream(topology).map(int[]::clone).toArray(int[][]::new);this.ops=ops.clone();this.weights=weights.clone();
            for(int op:ops)if(op<0||op>15)throw new IllegalArgumentException("operator");for(float v:weights)if(!Float.isFinite(v))throw new IllegalArgumentException("model value");
        }
        public int parameters(){return weights.length;}
        public BrnLogicWorkspace newWorkspace(){return new BrnLogicWorkspace(this);}
        public void write(OutputStream stream)throws IOException {
            var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);out.writeLong(MODEL_MAGIC);out.writeInt(layers);writeTopology(out,topology);
            for(int op:ops)out.writeByte(op);for(float v:weights)out.writeFloat(v);finish(bytes,out,stream);
        }
        public static Model read(InputStream stream)throws IOException {
            var in=checked(stream,MODEL_MAGIC).input();
            try {int layers=in.readInt();requireLayers(layers);int[][] t=readTopology(in,layers);int[] ops=new int[layers*WIDTH];for(int i=0;i<ops.length;i++)ops[i]=in.readUnsignedByte();
                float[] values=new float[2*layers*WIDTH*REGIONS+1];for(int i=0;i<values.length;i++)values[i]=in.readFloat();if(in.read()!=-1)throw new IOException("Trailing logic model");return new Model(layers,t,ops,values);
            }catch(IllegalArgumentException e){throw new IOException("Invalid logic model",e);}
        }
    }
    public void write(OutputStream stream)throws IOException {
        var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);out.writeLong(STATE_MAGIC);out.writeInt(layers);out.writeLong(updates);out.writeDouble(rate());out.writeDouble(headRate);out.writeDouble(hardRate);out.writeBoolean(hardened);writeTopology(out,topology);
        for(int op:operators())out.writeByte(op);for(var array:new double[][]{weights,first,second})for(double v:array)out.writeDouble(v);finish(bytes,out,stream);
    }
    public static BrnLogicTrainer read(InputStream stream)throws IOException {
        var payload=checked(stream,STATE_MAGIC,STATE_MAGIC-1);var in=payload.input();
        try {int layers=in.readInt();requireLayers(layers);long step=in.readLong();double rate=in.readDouble(),headRate=payload.magic()==STATE_MAGIC?in.readDouble():rate,hardRate=in.readDouble();new BrnAdamConfig(hardRate);boolean hard=in.readBoolean();if(step<0)throw new IOException("Negative step");
            var m=new BrnLogicTrainer(layers,0,rate,headRate);int[][] t=readTopology(in,layers);for(int i=0;i<t.length;i++)System.arraycopy(t[i],0,m.topology[i],0,6);
            int[][][] cells=cells(t);for(int i=0;i<cells.length;i++)for(int side=0;side<2;side++)System.arraycopy(cells[i][side],0,m.sourceCells[i][side],0,64);
            int[] ops=new int[m.gates];for(int i=0;i<ops.length;i++){ops[i]=in.readUnsignedByte();if(ops[i]>15)throw new IOException("Invalid gate");}
            for(var array:new double[][]{m.weights,m.first,m.second})for(int i=0;i<array.length;i++){double v=in.readDouble();if(!Double.isFinite(v)||(array==m.second&&v<0))throw new IOException("Invalid optimizer");array[i]=v;}
            if(!Arrays.equals(ops,m.operators()))throw new IOException("Gate/logit mismatch");m.hardened=hard;m.hardRate=hardRate;m.frozenOps=hard?ops:null;m.updates=step;
            if(in.read()!=-1)throw new IOException("Trailing logic state");return m;
        }catch(IllegalArgumentException e){throw new IOException("Invalid logic state",e);}
    }
    private static void writeTopology(DataOutputStream out,int[][] topology)throws IOException {for(var row:topology)for(int v:row)out.writeInt(v);}
    private static int[][] readTopology(DataInputStream in,int layers)throws IOException {
        int[][] t=new int[layers*WIDTH][6];for(var row:t)for(int i=0;i<row.length;i++)row[i]=in.readInt();validateTopology(layers,t);return t;
    }
    private static void finish(ByteArrayOutputStream bytes,DataOutputStream out,OutputStream stream)throws IOException {
        out.flush();var crc=new CRC32();crc.update(bytes.toByteArray());out.writeInt((int)crc.getValue());out.flush();bytes.writeTo(stream);
    }
    private record Payload(long magic,DataInputStream input){}
    private static Payload checked(InputStream stream,long... allowed)throws IOException {
        byte[] bytes=stream.readAllBytes();if(bytes.length<16)throw new IOException("Truncated logic payload");var crc=new CRC32();crc.update(bytes,0,bytes.length-4);
        if(java.nio.ByteBuffer.wrap(bytes,bytes.length-4,4).getInt()!=(int)crc.getValue())throw new IOException("Logic checksum");
        var in=new DataInputStream(new ByteArrayInputStream(bytes,0,bytes.length-4));long magic=in.readLong();
        for(long expected:allowed)if(magic==expected)return new Payload(magic,in);throw new IOException("Wrong logic payload");
    }
}
