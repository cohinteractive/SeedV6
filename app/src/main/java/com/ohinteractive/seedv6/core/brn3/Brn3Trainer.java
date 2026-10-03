package com.ohinteractive.seedv6.core.brn3;

import static com.ohinteractive.seedv6.core.brn3.Brn3Layout.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn.BrnAdamConfig;
import com.ohinteractive.seedv6.training.nnue.NnueCorpusTargets;
import java.util.*;

/** Single-owner double-precision trainer. Sparse masked Adam updates the coordinates
 * touched by each minibatch; absent coordinates and their moments are unchanged.
 * Relative-table sharing is training-only and folds into the immutable model. */
public final class Brn3Trainer {
    public static final double DEFAULT_LEARNING_RATE=.003;
    private static final boolean VECTOR=ModuleLayer.boot().findModule("jdk.incubator.vector").isPresent()
            && !Boolean.getBoolean("seedv6.brn3.scalar");
    static final int[] RELATIVE_ROW=relativeRows();
    final double[] weights,first,second;
    final BrnAdamConfig config;
    long updates;
    private final double[] gradient=new double[TRAINING_PARAMETERS];
    // Every sparse feature contributes all WIDTH lanes, including zero derivatives.
    private final int[] touched=new int[TRAINING_PARAMETERS/WIDTH+1],stamps=new int[TRAINING_PARAMETERS/WIDTH+1];
    private final int[][] entities=new int[2][64];
    private final int[][][] edges=new int[2][64][64];
    private final double[][][] local=new double[2][64][WIDTH],localGradient=new double[2][64][WIDTH];
    private final double[][] pooled=new double[2][POOL_WIDTH],poolGradient=new double[2][POOL_WIDTH];
    private final double[] hidden=new double[HIDDEN_WIDTH];
    private final double[] denseByInput=new double[2*POOL_WIDTH*HIDDEN_WIDTH];
    private boolean denseCurrent;
    private int count,serial,size;
    private double relationNorm,poolNorm;

    public Brn3Trainer(long seed){this(seed,new BrnAdamConfig(DEFAULT_LEARNING_RATE));}
    public Brn3Trainer(long seed,BrnAdamConfig config) {
        this.config=Objects.requireNonNull(config);
        weights=new double[TRAINING_PARAMETERS];first=new double[TRAINING_PARAMETERS];second=new double[TRAINING_PARAMETERS];
        var random=new Random(seed);
        for(int i=0;i<DENSE;i++)weights[i]=(2*random.nextDouble()-1)*.1;
        for(int i=DENSE;i<BIAS;i++)weights[i]=(2*random.nextDouble()-1)*Math.sqrt(6.0/(2*POOL_WIDTH+HIDDEN_WIDTH));
        // Readout weights and bias are exactly zero: every fresh prediction is material.
    }
    Brn3Trainer(double[] weights,double[] first,double[] second,long updates,BrnAdamConfig config) {
        this.config=Objects.requireNonNull(config);
        if(updates<0)throw new IllegalArgumentException("Negative optimizer step");
        for(var block:new double[][]{weights,first,second}) {
            if(block.length!=TRAINING_PARAMETERS)throw new IllegalArgumentException("BRN-3 state length");
            for(double value:block)if(!Double.isFinite(value)||block==second&&value<0)throw new IllegalArgumentException("Invalid BRN-3 optimizer state");
        }
        this.weights=weights;this.first=first;this.second=second;this.updates=updates;
    }
    /** Defensive import boundary for independently verified research checkpoints. */
    public static Brn3Trainer restore(double[] weights,double[] first,double[] second,long updates,BrnAdamConfig config) {
        return new Brn3Trainer(weights.clone(),first.clone(),second.clone(),updates,config);
    }
    public BrnAdamConfig config(){return config;}
    public long step(){return updates;}
    public double weight(int index){return weights[index];}
    public double firstMoment(int index){return first[index];}
    public double secondMoment(int index){return second[index];}
    static int[] relativeRows() {
        var rows=new int[EDGE_ROWS];
        for(int large=1;large<768;large++)for(int small=0;small<large;small++) {
            int a=small%64,b=large%64,dx=(b&7)-(a&7),dy=(b>>>3)-(a>>>3);
            rows[large*(large-1)/2+small]=(small/64*12+large/64)*225+(dy+7)*15+dx+7;
        }
        return rows;
    }
    public double predictPawns(long[] board) {
        int stm=Board.player((int)board[4]);
        for(int role=0;role<2;role++) {
            int perspective=stm^role;count=0;Arrays.fill(pooled[role],0);
            for(long bits=board[0]|board[1]|board[2];bits!=0;bits&=bits-1) {
                int square=Long.numberOfTrailingZeros(bits);
                int channel=Brn3Features.channel(Board.getSquare(board[0],board[1],board[2],board[3],square)^(perspective<<3));
                int entity=channel*64+(square^(perspective*56));entities[role][count]=entity;
                System.arraycopy(weights,NODES+entity*WIDTH,local[role][count++],0,WIDTH);
            }
            relationNorm=1/Math.sqrt(Math.max(1,count-1));poolNorm=1/Math.sqrt(Math.max(1,count));
            for(int i=0;i<count;i++)for(int j=i+1;j<count;j++) {
                int row=edge(entities[role][i],entities[role][j]);edges[role][i][j]=row;
                int shared=MODEL_PARAMETERS+RELATIVE_ROW[row/WIDTH]*WIDTH;
                double[] left=local[role][i],right=local[role][j];
                if(VECTOR)Brn3VectorKernels.forward(weights,row,shared,relationNorm,left,right);
                else for(int c=0;c<WIDTH;c++) {
                    double message=relationNorm*(weights[row+c]+weights[shared+c]);
                    left[c]+=message;right[c]+=message;
                }
            }
            for(int i=0;i<count;i++)for(int c=0;c<WIDTH;c++) {
                local[role][i][c]=Math.max(0,local[role][i][c]);
                pooled[role][entities[role][i]/64*WIDTH+c]+=poolNorm*local[role][i][c];
            }
        }
        double residual=weights[OUTPUT_BIAS];
        if(VECTOR) {
            // Refresh once per parameter update, not once per example. Vectorize
            // across hidden units while preserving each dot product's sum order.
            if(!denseCurrent) {
                for(int h=0;h<HIDDEN_WIDTH;h++)for(int c=0;c<2*POOL_WIDTH;c++)
                    denseByInput[c*HIDDEN_WIDTH+h]=weights[DENSE+h*2*POOL_WIDTH+c];
                denseCurrent=true;
            }
            Brn3VectorKernels.hidden(weights,denseByInput,pooled,hidden);
            for(int h=0;h<HIDDEN_WIDTH;h++)residual+=weights[HEAD+h]*hidden[h];
        } else {
            for(int h=0;h<HIDDEN_WIDTH;h++) {
                double z=weights[BIAS+h];
                for(int p=0;p<2;p++)for(int c=0;c<POOL_WIDTH;c++)z+=weights[DENSE+h*2*POOL_WIDTH+p*POOL_WIDTH+c]*pooled[p][c];
                hidden[h]=Math.max(0,z);residual+=weights[HEAD+h]*hidden[h];
            }
        }
        double value=Brn3Features.material(board)+residual;
        if(!Double.isFinite(value))throw new ArithmeticException("Nonfinite BRN-3 prediction");return value;
    }
    public double predictOutcome(long[] board){return Brn3Objective.outcome(predictPawns(board),board);}
    private void resetGradient(){if(++serial==0){Arrays.fill(stamps,0);serial=1;}size=0;Arrays.fill(gradient,DENSE,MODEL_PARAMETERS,0);}
    private void touchRow(int base) {
        int row=base/WIDTH;
        if(stamps[row]!=serial){stamps[row]=serial;Arrays.fill(gradient,base,base+WIDTH,0);touched[size++]=base;}
    }
    private void backward(double derivative) {
        for(var g:poolGradient)Arrays.fill(g,0);gradient[OUTPUT_BIAS]+=derivative;
        for(int h=0;h<HIDDEN_WIDTH;h++) {
            gradient[HEAD+h]+=derivative*hidden[h];double dz=derivative*weights[HEAD+h]*(hidden[h]>0?1:0);gradient[BIAS+h]+=dz;
            for(int p=0;p<2;p++)for(int c=0;c<POOL_WIDTH;c++) {
                int index=DENSE+h*2*POOL_WIDTH+p*POOL_WIDTH+c;
                gradient[index]+=dz*pooled[p][c];poolGradient[p][c]+=dz*weights[index];
            }
        }
        for(int p=0;p<2;p++) {
            for(int i=0;i<count;i++) {
                int node=NODES+entities[p][i]*WIDTH;touchRow(node);
                for(int c=0;c<WIDTH;c++) {
                    double dz=poolGradient[p][entities[p][i]/64*WIDTH+c]*poolNorm*(local[p][i][c]>0?1:0);
                    localGradient[p][i][c]=dz;gradient[node+c]+=dz;
                }
            }
            for(int i=0;i<count;i++)for(int j=i+1;j<count;j++) {
                int row=edges[p][i][j],shared=MODEL_PARAMETERS+RELATIVE_ROW[row/WIDTH]*WIDTH;
                touchRow(row);touchRow(shared);
                double[] left=localGradient[p][i],right=localGradient[p][j];
                if(VECTOR)Brn3VectorKernels.backward(gradient,row,shared,relationNorm,left,right);
                else for(int c=0;c<WIDTH;c++) {
                    double g=relationNorm*(left[c]+right[c]);
                    gradient[row+c]+=g;gradient[shared+c]+=g;
                }
            }
        }
    }
    /** Targets are frozen WDL expected scores in [-1,+1]; the network retains pawn units.
     * Returns pre-update outcome half-MSE for framework progress, while optimizing CE. */
    public double trainBatch(long[][] boards,double[] targets,int batch) {
        if(batch<1||batch>boards.length||batch>targets.length)throw new IllegalArgumentException("BRN-3 minibatch size");
        for(int n=0;n<batch;n++)if(boards[n]==null||!Double.isFinite(targets[n])||Math.abs(targets[n])>1)throw new IllegalArgumentException("BRN-3 training example");
        resetGradient();double loss=0;
        for(int n=0;n<batch;n++) {
            double value=predictPawns(boards[n]);
            double difference=Brn3Objective.outcome(value,boards[n])-targets[n];loss+=.5*difference*difference;
            backward(Brn3Objective.crossEntropy(value,NnueCorpusTargets.material(boards[n]),targets[n])[1]);
        }
        if(updates==Long.MAX_VALUE)throw new ArithmeticException("BRN-3 optimizer step exhausted");
        updates++;double c1=1-Math.pow(config.beta1(),updates),c2=1-Math.pow(config.beta2(),updates);
        denseCurrent=false;
        // Preserve the investigated recipe's literal binary64 complements exactly.
        double complement1=config.beta1()==.9?.1:1-config.beta1(),complement2=config.beta2()==.999?.001:1-config.beta2();
        updateRange(DENSE,MODEL_PARAMETERS,batch,c1,c2,complement1,complement2);
        for(int n=0;n<size;n++)updateRange(touched[n],touched[n]+WIDTH,batch,c1,c2,complement1,complement2);
        return loss/batch;
    }
    private void updateRange(int start,int end,int batch,double c1,double c2,double complement1,double complement2) {
        if(VECTOR) {
            Brn3VectorKernels.update(weights,first,second,gradient,start,end,batch,c1,c2,complement1,complement2,config);
            return;
        }
        for(int i=start;i<end;i++) {
            double g=gradient[i]/batch;
            first[i]=config.beta1()*first[i]+complement1*g;
            second[i]=config.beta2()*second[i]+complement2*g*g;
            weights[i]-=config.learningRate()*(first[i]/c1)/(Math.sqrt(second[i]/c2)+config.epsilon());
            if(!Double.isFinite(weights[i]))throw new ArithmeticException("Nonfinite BRN-3 optimizer update");
        }
    }
    public Brn3Model snapshot() {
        var folded=new float[MODEL_PARAMETERS];for(int i=0;i<folded.length;i++)folded[i]=(float)weights[i];
        for(int row=0;row<EDGE_ROWS;row++)for(int c=0;c<WIDTH;c++)folded[row*WIDTH+c]=(float)(weights[row*WIDTH+c]+weights[MODEL_PARAMETERS+RELATIVE_ROW[row]*WIDTH+c]);
        return new Brn3Model(folded);
    }
}
