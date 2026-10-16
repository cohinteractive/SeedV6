package com.ohinteractive.seedv6.tools.nnue.cglhw;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import java.util.SplittableRandom;

/** E002: small shared representations, always with a learned antisymmetric head.
 * All independent weights are zero-centred uniform random variables. Geometry
 * controls parameter sharing only, never desired piece or square values.
 * Smooth factors are folded once into piece-square embeddings, not evaluated as
 * full-board relationships. The model has no king-conditioned refresh boundary. */
public final class SharedFeatures implements IncrementalModel {
    public enum Kind { PIECE_SQUARE, SMOOTH, MIRROR_SMOOTH, COUNTS }
    private static final int WIDTH=64,HIDDEN=32;
    private final Kind kind;
    private final float[] embeddings,dense=new float[128*HIDDEN],head=new float[HIDDEN];
    private final int independentParameters;

    public SharedFeatures(long seed,Kind kind) {
        this.kind=kind;var rng=new SplittableRandom(seed);
        embeddings=new float[(kind==Kind.COUNTS?12:768)*WIDTH];
        int free;
        if(kind==Kind.SMOOTH||kind==Kind.MIRROR_SMOOTH) {
            float[] factors=new float[12*3*WIDTH];free=factors.length;
            for(int i=0;i<factors.length;i++)factors[i]=(float)(rng.nextDouble(-1.0/64,1.0/64)/Math.sqrt(3));
            for(int c=0;c<12;c++)for(int s=0;s<64;s++)for(int u=0;u<WIDTH;u++) {
                double rank=2.0*(s/8)/7-1;
                double file=kind==Kind.MIRROR_SMOOTH?2.0*Math.min(s%8,7-s%8)/3-1:2.0*(s%8)/7-1;
                embeddings[(c*64+s)*WIDTH+u]=(float)(factors[c*3*WIDTH+u]+rank*factors[(c*3+1)*WIDTH+u]+file*factors[(c*3+2)*WIDTH+u]);
            }
        } else {
            free=embeddings.length;
            for(int i=0;i<embeddings.length;i++)embeddings[i]=(float)rng.nextDouble(-1.0/64,1.0/64);
        }
        for(int i=0;i<dense.length;i++)dense[i]=(float)rng.nextDouble(-1.0/64,1.0/64);
        for(int i=0;i<head.length;i++)head[i]=(float)rng.nextDouble(-1.0/64,1.0/64);
        independentParameters=free+dense.length+head.length;
    }
    @Override public int parameters() { return independentParameters; }
    public int inferenceFloats() { return embeddings.length+dense.length+head.length; }
    private int row(int piece,int square,int perspective) {
        int channel=(piece&7)-1+(((piece>>>3)^perspective)*6);
        return kind==Kind.COUNTS?channel:channel*64+(square^(perspective*56));
    }
    private void add(float[] state,int perspective,int row,int multiple) {
        int start=perspective*WIDTH,offset=row*WIDTH;
        for(int u=0;u<WIDTH;u++)state[start+u]+=multiple*embeddings[offset+u];
    }
    private double readout(float[] state,int stm) {
        float sum=0;
        for(int h=0;h<HIDDEN;h+=4) {
            float a=0,b=0,c=0,d=0;int offset=h*128;
            for(int i=0;i<128;i++) {
                float x=NnueNetwork.clip01(state[(i+stm*64)%128]);
                a+=dense[offset+i]*x;b+=dense[offset+128+i]*x;
                c+=dense[offset+256+i]*x;d+=dense[offset+384+i]*x;
            }
            sum+=head[h]*NnueNetwork.clip01(a);sum+=head[h+1]*NnueNetwork.clip01(b);
            sum+=head[h+2]*NnueNetwork.clip01(c);sum+=head[h+3]*NnueNetwork.clip01(d);
        }
        return sum;
    }
    @Override public Worker worker(int capacity) {
        return new Worker(NnueScoreMapping.V1.scale()) {
            private final float[][] accumulators=new float[capacity][128];
            private final int[][] exactCounts=kind==Kind.COUNTS?new int[capacity][12]:null;
            private final int[] rows=new int[128],counts=new int[128];
            private int used;
            @Override public void refresh(long[] board,int slot) {
                if(kind==Kind.COUNTS) {
                    java.util.Arrays.fill(exactCounts[slot],0);
                    for(long bits=board[0]|board[1]|board[2];bits!=0;bits&=bits-1) {
                        int p=Board.getSquare(board[0],board[1],board[2],board[3],Long.numberOfTrailingZeros(bits));
                        exactCounts[slot][(p&7)-1+(p>>>3)*6]++;
                    }
                    rebuildCountSums(slot);return;
                }
                float[] state=accumulators[slot];java.util.Arrays.fill(state,0);
                for(long occupied=board[0]|board[1]|board[2];occupied!=0;occupied&=occupied-1) {
                    int s=Long.numberOfTrailingZeros(occupied),p=Board.getSquare(board[0],board[1],board[2],board[3],s);
                    for(int perspective=0;perspective<2;perspective++)add(state,perspective,row(p,s,perspective),1);
                }
            }
            private void rebuildCountSums(int slot) {
                float[] state=accumulators[slot];java.util.Arrays.fill(state,0);
                for(int perspective=0;perspective<2;perspective++)for(int channel=0;channel<12;channel++) {
                    int amount=exactCounts[slot][(channel+perspective*6)%12];
                    if(amount!=0)add(state,perspective,channel,amount);
                }
            }
            private void delta(int row,int count) {
                for(int i=0;i<used;i++)if(rows[i]==row){counts[i]+=count;return;}
                rows[used]=row;counts[used++]=count;
            }
            @Override public void transition(long[] parent,long[] child,int source,int destination) {
                if(source==destination)throw new IllegalArgumentException("Distinct accumulator slots required");
                float[] state=accumulators[destination];System.arraycopy(accumulators[source],0,state,0,128);
                long changed=(parent[0]^child[0])|(parent[1]^child[1])|(parent[2]^child[2])|(parent[3]^child[3]);
                if(kind==Kind.COUNTS) {
                    System.arraycopy(exactCounts[source],0,exactCounts[destination],0,12);
                    for(long bits=changed;bits!=0;bits&=bits-1) {
                        int s=Long.numberOfTrailingZeros(bits),old=Board.getSquare(parent[0],parent[1],parent[2],parent[3],s);
                        int next=Board.getSquare(child[0],child[1],child[2],child[3],s);
                        if(old!=0)exactCounts[destination][(old&7)-1+(old>>>3)*6]--;
                        if(next!=0)exactCounts[destination][(next&7)-1+(next>>>3)*6]++;
                    }
                    if(!java.util.Arrays.equals(exactCounts[source],exactCounts[destination]))rebuildCountSums(destination);
                    return;
                }
                for(int perspective=0;perspective<2;perspective++) {
                    used=0;
                    for(long bits=changed;bits!=0;bits&=bits-1) {
                        int s=Long.numberOfTrailingZeros(bits);
                        int old=Board.getSquare(parent[0],parent[1],parent[2],parent[3],s);
                        int next=Board.getSquare(child[0],child[1],child[2],child[3],s);
                        if(old!=0)delta(row(old,s,perspective),-1);
                        if(next!=0)delta(row(next,s,perspective),1);
                    }
                    // Merge repeated identities before modifying floats: quiet moves in
                    // COUNTS preserve every accumulator bit, not just a loose tolerance.
                    for(int i=0;i<used;i++)if(counts[i]!=0)add(state,perspective,rows[i],counts[i]);
                }
            }
            @Override public double raw(long[] board,int slot) {
                int stm=Board.player((int)board[4]);float[] state=accumulators[slot];
                return .5*(readout(state,stm)-readout(state,stm^1));
            }
        };
    }
}
