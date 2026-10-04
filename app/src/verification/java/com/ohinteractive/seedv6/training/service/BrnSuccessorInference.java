package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.brn3.*;

import static com.ohinteractive.seedv6.core.brn3.Brn3Layout.*;

import com.ohinteractive.seedv6.core.Board;
import java.util.*;

/** Verification-only folded float inference for admitted width8 symmetric candidates.
 * Derived from the preserved BRN-3 rolling cache; optional second piece-local pass. */
final class BrnSuccessorInference {
    private final float[] compactWeights;
    private final int[] oldCodes=new int[64],codes=new int[64],squares=new int[64],removed=new int[64],added=new int[64];
    private final int[][] entities=new int[2][64],nextEntities=new int[2][64];
    private final double[][][] raw;
    private final double[][] pooled;
    private final long[] previous=new long[4];
    private int count;private boolean initialized;
    private double whiteMaterial;
    public long rebuilds,updates,repeats;

    private final float[] contextWeights;
    private final boolean contextual;
    private final double[][] contextualLocal=new double[64][WIDTH];
    private final double[] contextualSum=new double[WIDTH];
    BrnSuccessorInference(BrnSuccessorCandidate model) {
        if(model.width!=8 || model.relations!=BrnSuccessorCandidate.Relations.ABSOLUTE && model.relations!=BrnSuccessorCandidate.Relations.RELATIVE
                || !Set.of(BrnSuccessorCandidate.Pool.SUM,BrnSuccessorCandidate.Pool.SUM_CONTEXT,BrnSuccessorCandidate.Pool.SUM_SELF).contains(model.pool))
            throw new IllegalArgumentException("Unsupported experimental inference shape");
        raw=new double[2][64][WIDTH];pooled=new double[2][POOL_WIDTH];compactWeights=new float[MODEL_PARAMETERS];
        for(int row=0;row<EDGE_ROWS;row++)for(int c=0;c<WIDTH;c++)compactWeights[row*WIDTH+c]=(float)(
                (model.relations==BrnSuccessorCandidate.Relations.ABSOLUTE?model.weights[row*WIDTH+c]:0)
                +model.weights[model.relative+AbsoluteRelationCandidate.RELATIVE_ROW[row]*WIDTH+c]);
        for(int i=NODES;i<MODEL_PARAMETERS;i++)compactWeights[i]=(float)model.weights[model.nodes+i-NODES];
        contextual=model.pool==BrnSuccessorCandidate.Pool.SUM_CONTEXT;
        contextWeights=model.pool==BrnSuccessorCandidate.Pool.SUM?null:new float[model.parameters-model.context];
        if(contextWeights!=null)for(int i=0;i<contextWeights.length;i++)contextWeights[i]=(float)model.weights[model.context+i];
    }
    private static int edge(int a,int b){return BrnSuccessorProbe.row(a,b)*WIDTH;}
    Brn3Model compatibleModel() {
        if(contextWeights!=null)throw new IllegalStateException("Context topology is not BRN-3");
        return new Brn3Model(compactWeights);
    }
    private static double pieceMaterial(int code){return switch(code&7){case 6->1;case 5->3.2;case 4->3.3;case 3->5;case 2->9;default->0;};}
    private void contextualPool(int p,double relationNorm,double poolNorm) {
        Arrays.fill(contextualSum,0);
        for(int i=0;i<count;i++) {
            int square=squares[i],unary=NODES+nextEntities[p][square]*WIDTH;
            for(int c=0;c<WIDTH;c++) {
                double value=activate(compactWeights[unary+c]+relationNorm*raw[p][square][c]);
                contextualLocal[i][c]=value;contextualSum[c]+=value;
            }
        }
        for(int i=0;i<count;i++)for(int h=0;h<WIDTH;h++) {
            double value=contextualLocal[i][h]+contextWeights[(contextual?2:1)*WIDTH*WIDTH+h];
            for(int c=0;c<WIDTH;c++) {
                double message=contextual?contextWeights[WIDTH*WIDTH+h*WIDTH+c]*(contextualSum[c]-contextualLocal[i][c])/Math.max(1,count-1):0;
                value+=contextWeights[h*WIDTH+c]*contextualLocal[i][c]+message;
            }
            pooled[p][poolIndex(nextEntities[p][squares[i]],h)]+=poolNorm*activate(value);
        }
    }
    private static double activate(double value){return Math.max(0,value);}
    private static int poolIndex(int entity,int channel){return entity/64*WIDTH+channel;}
    private static void node(double[] target){Arrays.fill(target,0);}
    // Raw edge sums use doubles. Apply the relation-count normalization once per node.
    private void addPair(double[] a,double[] b,int row) {
        double v0=compactWeights[row+0];a[0]+=v0;b[0]+=v0;
        double v1=compactWeights[row+1];a[1]+=v1;b[1]+=v1;
        double v2=compactWeights[row+2];a[2]+=v2;b[2]+=v2;
        double v3=compactWeights[row+3];a[3]+=v3;b[3]+=v3;
        double v4=compactWeights[row+4];a[4]+=v4;b[4]+=v4;
        double v5=compactWeights[row+5];a[5]+=v5;b[5]+=v5;
        double v6=compactWeights[row+6];a[6]+=v6;b[6]+=v6;
        double v7=compactWeights[row+7];a[7]+=v7;b[7]+=v7;
    }
    private void adjust(double[] target,int row,double factor) {
        double sign=Math.copySign(1.0,factor);
        target[0]+=sign*compactWeights[row+0];
        target[1]+=sign*compactWeights[row+1];
        target[2]+=sign*compactWeights[row+2];
        target[3]+=sign*compactWeights[row+3];
        target[4]+=sign*compactWeights[row+4];
        target[5]+=sign*compactWeights[row+5];
        target[6]+=sign*compactWeights[row+6];
        target[7]+=sign*compactWeights[row+7];
    }
    private void pool(double[] target,int offset,double[] local,double factor,int entity,double relationNorm) {
        int unary=NODES+entity*WIDTH;
        target[offset+0]+=factor*activate(compactWeights[unary+0]+relationNorm*local[0]);
        target[offset+1]+=factor*activate(compactWeights[unary+1]+relationNorm*local[1]);
        target[offset+2]+=factor*activate(compactWeights[unary+2]+relationNorm*local[2]);
        target[offset+3]+=factor*activate(compactWeights[unary+3]+relationNorm*local[3]);
        target[offset+4]+=factor*activate(compactWeights[unary+4]+relationNorm*local[4]);
        target[offset+5]+=factor*activate(compactWeights[unary+5]+relationNorm*local[5]);
        target[offset+6]+=factor*activate(compactWeights[unary+6]+relationNorm*local[6]);
        target[offset+7]+=factor*activate(compactWeights[unary+7]+relationNorm*local[7]);
    }
    private double compactReadout(int stm) {
        double residual=compactWeights[OUTPUT_BIAS];int stride=2*POOL_WIDTH;
        // Four independent sums retain each original feature order.
        for(int h=0;h<HIDDEN_WIDTH;h+=4) {
            double s0=compactWeights[BIAS+h+0];
            double s1=compactWeights[BIAS+h+1];
            double s2=compactWeights[BIAS+h+2];
            double s3=compactWeights[BIAS+h+3];
            for(int role=0;role<2;role++) {
                int start=DENSE+h*stride+role*POOL_WIDTH;double[] input=pooled[stm^role];
                for(int c=0;c<POOL_WIDTH;c++) {
                    double value=input[c];
                    s0+=compactWeights[start+0*stride+c]*value;
                    s1+=compactWeights[start+1*stride+c]*value;
                    s2+=compactWeights[start+2*stride+c]*value;
                    s3+=compactWeights[start+3*stride+c]*value;
                }
            }
            residual+=compactWeights[HEAD+h+0]*activate(s0);
            residual+=compactWeights[HEAD+h+1]*activate(s1);
            residual+=compactWeights[HEAD+h+2]*activate(s2);
            residual+=compactWeights[HEAD+h+3]*activate(s3);
        }
        return residual;
    }
    public double evaluatePawns(long[] board) {
        boolean same=initialized;for(int p=0;p<4;p++)same&=previous[p]==board[p];
        if(same)repeats++;
        else {
            count=0;whiteMaterial=0;int removedCount=0,addedCount=0;Arrays.fill(codes,0);
            long occupied=board[0]|board[1]|board[2];
            for(long bits=occupied;bits!=0;bits&=bits-1) {
                int square=Long.numberOfTrailingZeros(bits),code=Board.getSquare(board[0],board[1],board[2],board[3],square);codes[square]=code;
                squares[count++]=square;for(int p=0;p<2;p++)nextEntities[p][square]=BrnSuccessorProbe.channel(code^(p<<3))*64+(square^(p*56));
                double material=pieceMaterial(code);
                whiteMaterial+=(code&8)==0?material:-material;
            }
            long changed=(board[0]^previous[0])|(board[1]^previous[1])|(board[2]^previous[2])|(board[3]^previous[3]);
            changed&=occupied|previous[0]|previous[1]|previous[2];
            boolean distant=Long.bitCount(changed)>8;
            if(initialized && !distant)for(long bits=changed;bits!=0;bits&=bits-1){int square=Long.numberOfTrailingZeros(bits);
                if(oldCodes[square]!=0)removed[removedCount++]=square;if(codes[square]!=0)added[addedCount++]=square;}
            double norm=1/Math.sqrt(Math.max(1,count-1));
            // Count changes do not invalidate unnormalized incident sums. Update
            // surviving endpoints, reconstruct added endpoints, then renormalize
            // every local state below. This also handles reverse captures.
            if(!initialized || distant || removedCount+addedCount>8) {
                rebuilds++;
                for(int p=0;p<2;p++){
                    for(int i=0;i<count;i++){int square=squares[i];node(raw[p][square]);}
                    for(int i=0;i<count;i++)for(int j=i+1;j<count;j++){
                        int a=squares[i],b=squares[j],edge=edge(nextEntities[p][a],nextEntities[p][b]);
                        addPair(raw[p][a],raw[p][b],edge);
                    }
                }
            } else {
                updates++;
                for(int p=0;p<2;p++){
                    for(int i=0;i<count;i++){
                        int square=squares[i];if(codes[square]!=oldCodes[square])continue;
                        int entity=nextEntities[p][square];
                        for(int j=0;j<removedCount;j++){int edge=edge(entity,entities[p][removed[j]]);adjust(raw[p][square],edge,-norm);}
                        for(int j=0;j<addedCount;j++){int edge=edge(entity,nextEntities[p][added[j]]);adjust(raw[p][square],edge,norm);}
                    }
                    for(int i=0;i<addedCount;i++){
                        int square=added[i];node(raw[p][square]);
                        for(int j=0;j<count;j++)if(squares[j]!=square){int edge=edge(nextEntities[p][square],nextEntities[p][squares[j]]);
                            adjust(raw[p][square],edge,norm);}
                    }
                }
            }
            double poolNorm=1/Math.sqrt(Math.max(1,count));
            for(int p=0;p<2;p++){
                Arrays.fill(pooled[p],0);
                if(contextWeights!=null)contextualPool(p,norm,poolNorm);
                else for(int i=0;i<count;i++)pool(pooled[p],poolIndex(nextEntities[p][squares[i]],0),raw[p][squares[i]],poolNorm,nextEntities[p][squares[i]],norm);
                System.arraycopy(nextEntities[p],0,entities[p],0,64);
            }
            System.arraycopy(codes,0,oldCodes,0,64);System.arraycopy(board,0,previous,0,4);initialized=true;
        }
        int stm=Board.player((int)board[4]);double residual=compactReadout(stm);
        double value=(stm==0?whiteMaterial:-whiteMaterial)+residual;
        if(!Double.isFinite(value))throw new ArithmeticException("Nonfinite cached inference");return value;
    }

    /** Explicit residual-only calibration. The raw supervision prediction and material prior stay intact. */
    public double evaluatePawns(long[] board, double residualGain) {
        if(!Double.isFinite(residualGain) || residualGain < 0 || residualGain > 1)
            throw new IllegalArgumentException("BRN-3 residual gain must be in [0,1]");
        double rawValue=evaluatePawns(board);
        if(residualGain==1) return rawValue;
        double material=Board.player((int)board[4])==0?whiteMaterial:-whiteMaterial;
        return material+residualGain*(rawValue-material);
    }
}
