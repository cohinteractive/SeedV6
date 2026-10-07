package com.ohinteractive.seedv6.core.brn3;

import static com.ohinteractive.seedv6.core.brn3.Brn3Layout.*;

import com.ohinteractive.seedv6.core.Board;
import java.util.*;

/** Verification-only D01 workspace; production cache mechanics retained.
 * Worker-local cache over an immutable, folded float32 parameter snapshot.
 * Updates all affected entity relationships from the previous evaluated placement.
 * Small count-changing placements reuse raw sums; distant placements rebuild. No search/rule/hash input is
 * used; the cache is an inference optimization, never a learned feature. */
public final class BrnCubicWorkspace {
    private final float[] compactWeights;
    private final int rank;
    private final int[] oldCodes=new int[64],codes=new int[64],squares=new int[64],removed=new int[64],added=new int[64];
    private final int[][] entities=new int[2][64],nextEntities=new int[2][64];
    private final double[][][] raw;
    private final double[][] pooled;
    private final long[] previous=new long[4];
    private int count;private boolean initialized;
    private double whiteMaterial;
    public long rebuilds,updates,repeats;

    public BrnCubicWorkspace(BrnCubicTrainer.Model snapshot) {
        Objects.requireNonNull(snapshot);raw=new double[2][64][WIDTH];pooled=new double[2][POOL_WIDTH];
        compactWeights=snapshot.weights;rank=snapshot.rank;
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
    // D01 changes only the readout; the rolling relation cache mirrors production R01.
    private double compactReadout(int stm) {
        int inputs=2*POOL_WIDTH,factors=DENSE+inputs,alpha=factors+2*rank*inputs,third=alpha+rank,beta=third+rank*inputs;
        double residual=compactWeights[beta+rank];
        for(int p=0;p<2;p++)for(int c=0;c<POOL_WIDTH;c++)
            residual+=compactWeights[DENSE+p*POOL_WIDTH+c]*pooled[stm^p][c];
        for(int r=0;r<rank;r++) {
            double u=0,v=0,z=0;int base=factors+2*r*inputs;
            for(int p=0;p<2;p++)for(int c=0;c<POOL_WIDTH;c++) {
                int j=p*POOL_WIDTH+c;double x=pooled[stm^p][c];
                u+=compactWeights[base+j]*x;v+=compactWeights[base+inputs+j]*x;z+=compactWeights[third+r*inputs+j]*x;
            }
            residual+=compactWeights[alpha+r]*u*v+compactWeights[beta+r]*u*v*z;
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
                squares[count++]=square;for(int p=0;p<2;p++)nextEntities[p][square]=Brn3Features.channel(code^(p<<3))*64+(square^(p*56));
                double material=Brn3Features.pieceMaterial(code);
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
                        int a=squares[i],b=squares[j],edge=Brn3Layout.edge(nextEntities[p][a],nextEntities[p][b]);
                        addPair(raw[p][a],raw[p][b],edge);
                    }
                }
            } else {
                updates++;
                for(int p=0;p<2;p++){
                    for(int i=0;i<count;i++){
                        int square=squares[i];if(codes[square]!=oldCodes[square])continue;
                        int entity=nextEntities[p][square];
                        for(int j=0;j<removedCount;j++){int edge=Brn3Layout.edge(entity,entities[p][removed[j]]);adjust(raw[p][square],edge,-norm);}
                        for(int j=0;j<addedCount;j++){int edge=Brn3Layout.edge(entity,nextEntities[p][added[j]]);adjust(raw[p][square],edge,norm);}
                    }
                    for(int i=0;i<addedCount;i++){
                        int square=added[i];node(raw[p][square]);
                        for(int j=0;j<count;j++)if(squares[j]!=square){int edge=Brn3Layout.edge(nextEntities[p][square],nextEntities[p][squares[j]]);
                            adjust(raw[p][square],edge,norm);}
                    }
                }
            }
            double poolNorm=1/Math.sqrt(Math.max(1,count));
            for(int p=0;p<2;p++){
                Arrays.fill(pooled[p],0);
                for(int i=0;i<count;i++)pool(pooled[p],poolIndex(nextEntities[p][squares[i]],0),raw[p][squares[i]],poolNorm,nextEntities[p][squares[i]],norm);
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
