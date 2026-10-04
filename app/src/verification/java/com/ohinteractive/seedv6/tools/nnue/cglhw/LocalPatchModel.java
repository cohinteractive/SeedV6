package com.ohinteractive.seedv6.tools.nnue.cglhw;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import java.util.Arrays;
import java.util.SplittableRandom;

/** E003: locally connected, shared random convolution maintained incrementally.
 * A changed piece touches only nearby spatial cells; no all-piece relational pass.
 * Piece identity, ownership, geometry and empty off-board padding are the only
 * inputs. Float parameters, double sums prevent summation noise becoming scores. */
public final class LocalPatchModel implements IncrementalModel {
    private static final int WIDTH=8,HIDDEN=32,CELLS=64,STRIDE=CELLS*WIDTH;
    private final int radius,diameter,area;
    private final float[] kernel,dense=new float[2*WIDTH*HIDDEN],head=new float[HIDDEN];
    public LocalPatchModel(long seed,int radius) {
        if(radius<1||radius>2)throw new IllegalArgumentException("Local radius");
        this.radius=radius;diameter=2*radius+1;area=diameter*diameter;kernel=new float[12*area*WIDTH];
        var random=new SplittableRandom(seed);
        fill(random,kernel,Math.sqrt(6.0/(12*area+WIDTH)));
        fill(random,dense,Math.sqrt(6.0/(2*WIDTH+HIDDEN)));
        fill(random,head,Math.sqrt(6.0/(HIDDEN+1)));
    }
    private static void fill(SplittableRandom random,float[] array,double bound) {
        for(int i=0;i<array.length;i++)array[i]=(float)random.nextDouble(-bound,bound);
    }
    @Override public int parameters() { return kernel.length+dense.length+head.length; }
    private long footprint(int square) {
        long mask=0;int rank=square/8,file=square%8;
        for(int dy=-radius;dy<=radius;dy++)for(int dx=-radius;dx<=radius;dx++) {
            int y=rank-dy,x=file-dx;
            if(y>=0&&y<8&&x>=0&&x<8)mask|=1L<<(y*8+x);
        }
        return mask;
    }
    private void piece(double[] cells,int perspective,int code,int square,int sign) {
        square^=perspective*56;int rank=square/8,file=square%8;
        int channel=(code&7)-1+(((code>>>3)^perspective)*6);
        for(int dy=-radius;dy<=radius;dy++)for(int dx=-radius;dx<=radius;dx++) {
            int y=rank-dy,x=file-dx;
            if(y<0||y>=8||x<0||x>=8)continue;
            int cell=perspective*STRIDE+(y*8+x)*WIDTH,weight=(channel*area+(dy+radius)*diameter+dx+radius)*WIDTH;
            for(int u=0;u<WIDTH;u++)cells[cell+u]+=sign*(double)kernel[weight+u];
        }
    }
    private static void pool(double[] cells,double[] pooled,int perspective,long mask,int sign) {
        for(long bits=mask;bits!=0;bits&=bits-1) {
            int cell=perspective*STRIDE+Long.numberOfTrailingZeros(bits)*WIDTH;
            for(int u=0;u<WIDTH;u++)pooled[perspective*WIDTH+u]+=sign*Math.max(0,cells[cell+u])/8;
        }
    }
    private double readout(double[] pooled,int perspective) {
        double out=0;
        for(int h=0;h<HIDDEN;h++) {
            double sum=0;int row=h*2*WIDTH;
            for(int i=0;i<2*WIDTH;i++)sum+=dense[row+i]*pooled[i^(perspective*WIDTH)];
            out+=head[h]*Math.max(0,sum);
        }
        return out;
    }
    @Override public Worker worker(int capacity) {
        return new Worker(NnueScoreMapping.V1.scale()) {
            private final double[][] cells=new double[capacity][2*STRIDE],pooled=new double[capacity][2*WIDTH];
            @Override public void refresh(long[] board,int slot) {
                Arrays.fill(cells[slot],0);Arrays.fill(pooled[slot],0);
                for(long bits=board[0]|board[1]|board[2];bits!=0;bits&=bits-1) {
                    int square=Long.numberOfTrailingZeros(bits),code=Board.getSquare(board[0],board[1],board[2],board[3],square);
                    for(int p=0;p<2;p++)piece(cells[slot],p,code,square,1);
                }
                for(int p=0;p<2;p++)pool(cells[slot],pooled[slot],p,-1L,1);
            }
            @Override public void transition(long[] parent,long[] child,int source,int destination) {
                if(source==destination)throw new IllegalArgumentException("Distinct slots required");
                System.arraycopy(cells[source],0,cells[destination],0,2*STRIDE);
                System.arraycopy(pooled[source],0,pooled[destination],0,2*WIDTH);
                long changed=(parent[0]^child[0])|(parent[1]^child[1])|(parent[2]^child[2])|(parent[3]^child[3]);
                for(int p=0;p<2;p++) {
                    long affected=0;
                    for(long bits=changed;bits!=0;bits&=bits-1)affected|=footprint(Long.numberOfTrailingZeros(bits)^(p*56));
                    pool(cells[destination],pooled[destination],p,affected,-1);
                    for(long bits=changed;bits!=0;bits&=bits-1) {
                        int s=Long.numberOfTrailingZeros(bits),old=Board.getSquare(parent[0],parent[1],parent[2],parent[3],s);
                        int next=Board.getSquare(child[0],child[1],child[2],child[3],s);
                        if(old!=0)piece(cells[destination],p,old,s,-1);
                        if(next!=0)piece(cells[destination],p,next,s,1);
                    }
                    pool(cells[destination],pooled[destination],p,affected,1);
                }
            }
            @Override public double raw(long[] board,int slot) {
                int stm=Board.player((int)board[4]);return .5*(readout(pooled[slot],stm)-readout(pooled[slot],stm^1));
            }
        };
    }
}
