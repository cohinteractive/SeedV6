package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.util.Piece;
import java.util.*;

/** Research-only worker-local cache over an immutable, folded parameter snapshot.
 * Updates all affected entity relationships from the previous evaluated placement.
 * Piece-count changes or distant placements rebuild. No search/rule/hash input is
 * used; the cache is an inference optimization, never a learned feature. */
public final class AbsoluteRelationInference {
    private final AbsoluteRelationCandidate model;
    private final int width;
    private final float[] compactWeights;
    private final int[] oldCodes=new int[64],codes=new int[64],squares=new int[64],removed=new int[64],added=new int[64];
    private final int[][] entities=new int[2][64],nextEntities=new int[2][64];
    private final double[][][] raw;
    private final double[][] pooled;
    private final long[] previous=new long[4];
    private int count,previousCount;private boolean initialized;
    private double whiteMaterial;
    public long rebuilds,updates,repeats;

    public AbsoluteRelationInference(AbsoluteRelationCandidate snapshot) {
        this(snapshot,false);
    }
    public AbsoluteRelationInference(AbsoluteRelationCandidate snapshot, boolean compact) {
        if(snapshot.relative)throw new IllegalArgumentException("Fold relative weights before inference");
        model=snapshot;width=model.WIDTH;raw=new double[2][64][width];pooled=new double[2][model.POOL_WIDTH];
        compactWeights=compact?new float[snapshot.weights.length]:null;
        if(compact)for(int i=0;i<compactWeights.length;i++){compactWeights[i]=(float)snapshot.weights[i];if(!Float.isFinite(compactWeights[i]))throw new IllegalArgumentException("Weight exceeds float32 range");}
    }
    private double weight(int index){return compactWeights==null?model.weights[index]:compactWeights[index];}
    private void node(int entity,double[] target){if(width==8 && compactWeights!=null){Arrays.fill(target,0);return;}int start=model.NODES+entity*width;for(int c=0;c<width;c++)target[c]=weight(start+c);}
    // Constant-width scalar kernels retain accumulation order and double sums.
    // Float32 weights are the already budgeted inference snapshot, not optimizer state.
    private void addPair(double[] a,double[] b,int row,double factor) {
        if(width==8 && compactWeights!=null) {
            double v0=compactWeights[row+0];a[0]+=v0;b[0]+=v0;
            double v1=compactWeights[row+1];a[1]+=v1;b[1]+=v1;
            double v2=compactWeights[row+2];a[2]+=v2;b[2]+=v2;
            double v3=compactWeights[row+3];a[3]+=v3;b[3]+=v3;
            double v4=compactWeights[row+4];a[4]+=v4;b[4]+=v4;
            double v5=compactWeights[row+5];a[5]+=v5;b[5]+=v5;
            double v6=compactWeights[row+6];a[6]+=v6;b[6]+=v6;
            double v7=compactWeights[row+7];a[7]+=v7;b[7]+=v7;
        } else for(int c=0;c<width;c++){double value=factor*weight(row+c);a[c]+=value;b[c]+=value;}
    }
    private void adjust(double[] a,int row,double factor) {
        if(width==8 && compactWeights!=null) {
            a[0]+=Math.copySign(1.0,factor)*compactWeights[row+0];
            a[1]+=Math.copySign(1.0,factor)*compactWeights[row+1];
            a[2]+=Math.copySign(1.0,factor)*compactWeights[row+2];
            a[3]+=Math.copySign(1.0,factor)*compactWeights[row+3];
            a[4]+=Math.copySign(1.0,factor)*compactWeights[row+4];
            a[5]+=Math.copySign(1.0,factor)*compactWeights[row+5];
            a[6]+=Math.copySign(1.0,factor)*compactWeights[row+6];
            a[7]+=Math.copySign(1.0,factor)*compactWeights[row+7];
        } else for(int c=0;c<width;c++)a[c]+=factor*weight(row+c);
    }
    private void pool(double[] target,int offset,double[] local,double factor,int entity,double relationNorm) {
        int unary=model.NODES+entity*width;
        if(width==8 && compactWeights!=null) {
            target[offset+0]+=factor*model.activate(compactWeights[unary+0]+relationNorm*local[0]);
            target[offset+1]+=factor*model.activate(compactWeights[unary+1]+relationNorm*local[1]);
            target[offset+2]+=factor*model.activate(compactWeights[unary+2]+relationNorm*local[2]);
            target[offset+3]+=factor*model.activate(compactWeights[unary+3]+relationNorm*local[3]);
            target[offset+4]+=factor*model.activate(compactWeights[unary+4]+relationNorm*local[4]);
            target[offset+5]+=factor*model.activate(compactWeights[unary+5]+relationNorm*local[5]);
            target[offset+6]+=factor*model.activate(compactWeights[unary+6]+relationNorm*local[6]);
            target[offset+7]+=factor*model.activate(compactWeights[unary+7]+relationNorm*local[7]);
        } else if(width==8 && model.relu) {
            target[offset+0]+=factor*Math.max(0,local[0]);
            target[offset+1]+=factor*Math.max(0,local[1]);
            target[offset+2]+=factor*Math.max(0,local[2]);
            target[offset+3]+=factor*Math.max(0,local[3]);
            target[offset+4]+=factor*Math.max(0,local[4]);
            target[offset+5]+=factor*Math.max(0,local[5]);
            target[offset+6]+=factor*Math.max(0,local[6]);
            target[offset+7]+=factor*Math.max(0,local[7]);
        } else for(int c=0;c<width;c++)target[offset+c]+=factor*model.activate(local[c]);
    }
    private double dot(double sum,int start,double[] input) {
        if(width==8 && compactWeights!=null) {
            for(int c=0;c<input.length;c+=8) {
                sum+=compactWeights[start+c+0]*input[c+0];
                sum+=compactWeights[start+c+1]*input[c+1];
                sum+=compactWeights[start+c+2]*input[c+2];
                sum+=compactWeights[start+c+3]*input[c+3];
                sum+=compactWeights[start+c+4]*input[c+4];
                sum+=compactWeights[start+c+5]*input[c+5];
                sum+=compactWeights[start+c+6]*input[c+6];
                sum+=compactWeights[start+c+7]*input[c+7];
            }
        } else for(int c=0;c<input.length;c++)sum+=weight(start+c)*input[c];
        return sum;
    }
    private double compactReadout(int stm) {
        double residual=compactWeights[model.OUTPUT_BIAS];int stride=2*model.POOL_WIDTH;
        // Four independent sums retain each original feature order.
        for(int h=0;h<AbsoluteRelationCandidate.HIDDEN_WIDTH;h+=4) {
            double s0=compactWeights[model.BIAS+h+0];
            double s1=compactWeights[model.BIAS+h+1];
            double s2=compactWeights[model.BIAS+h+2];
            double s3=compactWeights[model.BIAS+h+3];
            for(int role=0;role<(model.singlePerspective?1:2);role++) {
                int start=model.DENSE+h*stride+role*model.POOL_WIDTH;double[] input=pooled[stm^role];
                for(int c=0;c<model.POOL_WIDTH;c++) {
                    double value=input[c];
                    s0+=compactWeights[start+0*stride+c]*value;
                    s1+=compactWeights[start+1*stride+c]*value;
                    s2+=compactWeights[start+2*stride+c]*value;
                    s3+=compactWeights[start+3*stride+c]*value;
                }
            }
            residual+=compactWeights[model.HEAD+h+0]*model.activate(s0);
            residual+=compactWeights[model.HEAD+h+1]*model.activate(s1);
            residual+=compactWeights[model.HEAD+h+2]*model.activate(s2);
            residual+=compactWeights[model.HEAD+h+3]*model.activate(s3);
        }
        return residual;
    }
    public double predict(long[] board) {
        boolean same=initialized;for(int p=0;p<4;p++)same&=previous[p]==board[p];
        if(same)repeats++;
        else {
            count=0;whiteMaterial=0;int removedCount=0,addedCount=0;Arrays.fill(codes,0);
            long occupied=board[0]|board[1]|board[2];
            for(long bits=occupied;bits!=0;bits&=bits-1) {
                int square=Long.numberOfTrailingZeros(bits),code=Board.getSquare(board[0],board[1],board[2],board[3],square);codes[square]=code;
                squares[count++]=square;for(int p=0;p<2;p++)nextEntities[p][square]=RelationalCandidate.channel(code^(p<<3))*64+(square^(p*56));
                double material=switch(code&Piece.TYPE){case Piece.PAWN->1;case Piece.KNIGHT->3.2;case Piece.BISHOP->3.3;case Piece.ROOK->5;case Piece.QUEEN->9;default->0;};
                whiteMaterial+=(code&8)==0?material:-material;
            }
            long changed=(board[0]^previous[0])|(board[1]^previous[1])|(board[2]^previous[2])|(board[3]^previous[3]);
            changed&=occupied|previous[0]|previous[1]|previous[2];
            boolean distant=Long.bitCount(changed)>8;
            if(initialized && count==previousCount && !distant)for(long bits=changed;bits!=0;bits&=bits-1){int square=Long.numberOfTrailingZeros(bits);
                if(oldCodes[square]!=0)removed[removedCount++]=square;if(codes[square]!=0)added[addedCount++]=square;}
            double norm=1/Math.sqrt(Math.max(1,count-1));
            if(!initialized || count!=previousCount || distant || removedCount+addedCount>8) {
                rebuilds++;
                for(int p=0;p<2;p++){
                    for(int i=0;i<count;i++){int square=squares[i];node(nextEntities[p][square],raw[p][square]);}
                    for(int i=0;i<count;i++)for(int j=i+1;j<count;j++){
                        int a=squares[i],b=squares[j],edge=model.edge(nextEntities[p][a],nextEntities[p][b]);
                        addPair(raw[p][a],raw[p][b],edge,norm);
                    }
                }
            } else {
                updates++;
                for(int p=0;p<2;p++){
                    for(int i=0;i<count;i++){
                        int square=squares[i];if(codes[square]!=oldCodes[square])continue;
                        int entity=nextEntities[p][square];
                        for(int j=0;j<removedCount;j++){int edge=model.edge(entity,entities[p][removed[j]]);adjust(raw[p][square],edge,-norm);}
                        for(int j=0;j<addedCount;j++){int edge=model.edge(entity,nextEntities[p][added[j]]);adjust(raw[p][square],edge,norm);}
                    }
                    for(int i=0;i<addedCount;i++){
                        int square=added[i];node(nextEntities[p][square],raw[p][square]);
                        for(int j=0;j<count;j++)if(squares[j]!=square){int edge=model.edge(nextEntities[p][square],nextEntities[p][squares[j]]);
                            adjust(raw[p][square],edge,norm);}
                    }
                }
            }
            double poolNorm=1/Math.sqrt(Math.max(1,count));
            for(int p=0;p<2;p++){
                Arrays.fill(pooled[p],0);
                for(int i=0;i<count;i++)pool(pooled[p],model.poolIndex(nextEntities[p][squares[i]],0),raw[p][squares[i]],poolNorm,nextEntities[p][squares[i]],norm);
                System.arraycopy(nextEntities[p],0,entities[p],0,64);
            }
            System.arraycopy(codes,0,oldCodes,0,64);System.arraycopy(board,0,previous,0,4);previousCount=count;initialized=true;
        }
        int stm=Board.player((int)board[4]);double residual=compactWeights!=null?compactReadout(stm):weight(model.OUTPUT_BIAS);
        if(compactWeights==null)for(int h=0;h<AbsoluteRelationCandidate.HIDDEN_WIDTH;h++){
            double z=weight(model.BIAS+h);
            for(int role=0;role<(model.singlePerspective?1:2);role++)z=dot(z,model.DENSE+h*2*model.POOL_WIDTH+role*model.POOL_WIDTH,pooled[stm^role]);
            residual+=weight(model.HEAD+h)*model.activate(z);
        }
        double value=(stm==0?whiteMaterial:-whiteMaterial)+residual;
        if(!Double.isFinite(value))throw new ArithmeticException("Nonfinite cached inference");return value;
    }
}
