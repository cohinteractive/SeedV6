package com.ohinteractive.seedv6.tools.nnue.cglhw;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import java.util.Arrays;
import java.util.SplittableRandom;

/** E004: low-rank global pair interactions from two incrementally maintained moments.
 * For learned piece embeddings e_i, (sum e_i)^2 - sum(e_i^2) equals the sum
 * over distinct ordered pairs. No board-wide pair enumeration is needed.
 * These are random learned relationships, not predetermined chess values. */
public final class GlobalPairModel implements IncrementalModel {
    private static final int WIDTH=32,STRIDE=2*WIDTH;
    private final float[] embeddings=new float[12*64*WIDTH],linear=new float[WIDTH],pair=new float[WIDTH];
    private final boolean interactions;
    public GlobalPairModel(long seed,boolean interactions) {
        this.interactions=interactions;var random=new SplittableRandom(seed);
        for(int i=0;i<embeddings.length;i++)embeddings[i]=(float)random.nextDouble(-1,1);
        double scale=1/Math.sqrt(WIDTH);
        for(int i=0;i<WIDTH;i++)linear[i]=(float)random.nextDouble(-scale,scale);
        for(int i=0;i<WIDTH;i++)pair[i]=(float)random.nextDouble(-scale,scale);
    }
    @Override public int parameters() { return embeddings.length+linear.length+(interactions?pair.length:0); }
    private void piece(double[] state,int code,int square,int sign) {
        for(int p=0;p<2;p++) {
            int channel=(code&7)-1+(((code>>>3)^p)*6),row=(channel*64+(square^(p*56)))*WIDTH,start=p*STRIDE;
            for(int u=0;u<WIDTH;u++) {
                double value=embeddings[row+u];state[start+u]+=sign*value;
                state[start+WIDTH+u]+=sign*value*value;
            }
        }
    }
    private double readout(double[] state,int perspective,int count) {
        double firstNorm=1/Math.sqrt(Math.max(1,count));
        double pairNorm=1/Math.sqrt(Math.max(1,count*(count-1)));double result=0;int start=perspective*STRIDE;
        for(int u=0;u<WIDTH;u++) {
            double sum=state[start+u];result+=linear[u]*sum*firstNorm;
            if(interactions)result+=pair[u]*(sum*sum-state[start+WIDTH+u])*pairNorm;
        }
        return result;
    }
    @Override public Worker worker(int capacity) {
        return new Worker(NnueScoreMapping.V1.scale()) {
            private final double[][] state=new double[capacity][2*STRIDE];private final int[] counts=new int[capacity];
            @Override public void refresh(long[] board,int slot) {
                Arrays.fill(state[slot],0);counts[slot]=Long.bitCount(board[0]|board[1]|board[2]);
                for(long bits=board[0]|board[1]|board[2];bits!=0;bits&=bits-1) {
                    int square=Long.numberOfTrailingZeros(bits),code=Board.getSquare(board[0],board[1],board[2],board[3],square);
                    piece(state[slot],code,square,1);
                }
            }
            @Override public void transition(long[] parent,long[] child,int source,int destination) {
                if(source==destination)throw new IllegalArgumentException("Distinct slots required");
                System.arraycopy(state[source],0,state[destination],0,2*STRIDE);counts[destination]=counts[source];
                long changed=(parent[0]^child[0])|(parent[1]^child[1])|(parent[2]^child[2])|(parent[3]^child[3]);
                for(long bits=changed;bits!=0;bits&=bits-1) {
                    int s=Long.numberOfTrailingZeros(bits),old=Board.getSquare(parent[0],parent[1],parent[2],parent[3],s);
                    int next=Board.getSquare(child[0],child[1],child[2],child[3],s);
                    if(old!=0){piece(state[destination],old,s,-1);counts[destination]--;}
                    if(next!=0){piece(state[destination],next,s,1);counts[destination]++;}
                }
            }
            @Override public double raw(long[] board,int slot) {
                int stm=Board.player((int)board[4]);return .5*(readout(state[slot],stm,counts[slot])-readout(state[slot],stm^1,counts[slot]));
            }
        };
    }
}
