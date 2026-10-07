package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.brn3.Brn3Objective;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn.BrnAdamConfig;
import com.ohinteractive.seedv6.training.nnue.NnueCorpusTargets;
import java.io.*;
import java.util.*;

/** Experimental Pair-2 training, promoted from C01/C02 at d23a05f.
 * Absolute and shared binary64 weights/moments remain separate until snapshot.
 * Geometry, folding, sparse Adam and objective preserve the research arithmetic. */
public final class BrnPair2Trainer {
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
    private BrnAdamConfig config;
    private int serial,size;
    long updates;

    public BrnPair2Trainer(double rate) {
        int order=2;
        requireOrder(order);this.order=order;states=169;
        bias=2*TEMPLATES*states;modelParameters=bias+1;trainingParameters=modelParameters+2*SHAPES.length*states;
        config=new BrnAdamConfig(rate);weights=new double[trainingParameters];first=new double[trainingParameters];
        second=new double[trainingParameters];gradient=new double[trainingParameters];
        touched=new int[trainingParameters];stamps=new int[trainingParameters];active=new int[2*TEMPLATES];
    }
    static void requireOrder(int order){if(order!=2)throw new IllegalArgumentException("Pair-2 requires order 2");}
    static int category(int code,int perspective){return code==0?0:BrnPair2Features.channel(code^(perspective<<3))+1;}
    static int state(int[] codes,int template,int order){int value=0;for(int j=order-1;j>=0;j--)value=13*value+codes[CELLS[template][j]];return value;}
    public int order(){return order;}
    public int parameters(){return trainingParameters;}
    public int inferenceParameters(){return modelParameters;}
    public long step(){return updates;}
    public double rate(){return config.learningRate();}
    public BrnAdamConfig config(){return config;}
    public void setLearningRate(double rate){config=new BrnAdamConfig(rate);}
    public double predictOutcome(long[] board){return Brn3Objective.outcome(predictPawns(board),board);}
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
        double value=BrnPair2Features.material(board)+weights[bias]+NORM*sum;
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
    public double trainBatch(long[][] boards,double[] targets,int batch) {
        if(batch<1||batch>boards.length||batch>targets.length)throw new IllegalArgumentException("batch");
        for(int i=0;i<batch;i++)if(boards[i]==null||!Double.isFinite(targets[i])||Math.abs(targets[i])>1)throw new IllegalArgumentException("example");
        resetGradient(); double loss=0;
        for(int i=0;i<batch;i++) {
            double value=predictPawns(boards[i]);double[] objective=Brn3Objective.crossEntropy(value,NnueCorpusTargets.material(boards[i]),targets[i]);
            loss+=objective[0];backward(objective[1]);
            for(int j=0;j<active.length;j++)seen.set(j*states+active[j]);
        }
        if(updates==Long.MAX_VALUE)throw new ArithmeticException("step exhausted");updates++;
        double c1=1-Math.pow(.9,updates),c2=1-Math.pow(.999,updates);
        for(int k=0;k<size;k++) {
            int i=touched[k];double g=gradient[i]/batch;first[i]=.9*first[i]+.1*g;second[i]=.999*second[i]+.001*g*g;
            weights[i]-=rate()*(first[i]/c1)/(Math.sqrt(second[i]/c2)+1e-8);
            if(!Double.isFinite(weights[i]))throw new ArithmeticException("Nonfinite tuple update");
        }
        return loss/batch;
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
        public Model(int order,float[] weights) {
            requireOrder(order);this.order=order;states=169;
            if(weights.length!=2*TEMPLATES*states+1)throw new IllegalArgumentException("shape");
            this.weights=weights.clone();for(float v:weights)if(!Float.isFinite(v))throw new IllegalArgumentException("Nonfinite tuple model");
        }
        public int parameters(){return weights.length;}
        public float weight(int index){return weights[index];}
    }
    public void write(OutputStream stream)throws IOException { BrnPair2Codec.writeTraining(this,stream); }
    public static BrnPair2Trainer read(InputStream stream)throws IOException { return BrnPair2Codec.readTraining(stream); }
}
