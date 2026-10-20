package com.ohinteractive.seedv6.core.brnnext;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn3.Brn3Model;
import com.ohinteractive.seedv6.core.brnpair2.BrnPair2Features;
import java.util.Arrays;
import static com.ohinteractive.seedv6.core.brnnext.BrnNextArchitecture.*;

/**
 * Worker-private GATE cache. Every dirty partial is recomputed from current codes;
 * fixed-order block sums avoid history-dependent floating delta drift.
 * Arbitrary transitions, undo, null moves and separate qsearch owners are supported.
 */
public final class BrnGatedAccumulator {
    private final BrnNextModel model;
    private final int width=GATED_PAIRS.width(), head=GATED_PAIRS.head();
    private final long[] previous=new long[4];
    private final int[][] codes=new int[2][64];
    private final double[][] unary=new double[2][8], projection=new double[2][8*width],
            experts=new double[2][32*width];
    private final int[] dirtyRanks={255,255}, dirtyBlocks={-1,-1};
    private final double[] first=new double[width], gates=new double[width];
    private final double[] lastRaw=new double[2],lastMaterial=new double[2];
    private final boolean[] valid=new boolean[2];
    private boolean initialized;
    private double material;
    private final com.ohinteractive.seedv6.core.brnpair2.BrnPair2Model.Workspace base;
    long recomputedRanks, recomputedBlocks;

    public BrnGatedAccumulator(BrnNextModel model) {
        if(model.architecture().correctionKind()!=GATED_PAIRS)throw new IllegalArgumentException("GATE model required");
        this.model=model;
        base=model.architecture().frozenBase()?BrnNextBase.model().newWorkspace():null;
    }
    public long primitiveBytes() { return 8L*(4+2*8+2*8*width+2*32*width+2*width+4)+4L*(2*64+4)+2+(base==null?0:base.primitiveBytes()); }
    public double rawPawns(long[] board) {
        long changed=0;
        for(int i=0;i<4;i++)changed|=previous[i]^board[i];
        changed&=previous[0]|previous[1]|previous[2]|board[0]|board[1]|board[2];
        if(!initialized)changed=-1L;
        if(changed!=0)valid[0]=valid[1]=false;
        for(long bits=changed;bits!=0;bits&=bits-1) {
            int square=Long.numberOfTrailingZeros(bits);
            int code=Board.getSquare(board[0],board[1],board[2],board[3],square),type=code&7;
            if(type==7)throw new IllegalArgumentException("Invalid piece category");
            for(int side=0;side<2;side++) {
                int canonical=square^(side*56),rank=canonical>>>3;
                codes[side][canonical]=type==0?0:type+(((code>>>3)^side)&1)*6;
                dirtyRanks[side]|=1<<rank;
                dirtyBlocks[side]|=1<<rank;
                for(int shape=1;shape<4;shape++) {
                    dirtyBlocks[side]|=1<<(shape*8+rank);
                    if(rank>0)dirtyBlocks[side]|=1<<(shape*8+rank-1);
                }
            }
        }
        System.arraycopy(board,0,previous,0,4);initialized=true;
        int side=Board.player((int)board[Board.STATUS]);
        if(valid[side]) { material=lastMaterial[side];return lastRaw[side]; }
        for(int ranks=dirtyRanks[side];ranks!=0;ranks&=ranks-1)rank(side,Integer.numberOfTrailingZeros(ranks));
        for(int blocks=dirtyBlocks[side];blocks!=0;blocks&=blocks-1)block(side,Integer.numberOfTrailingZeros(blocks));
        dirtyRanks[side]=0;dirtyBlocks[side]=0;
        double residual=model.weight(0);
        Arrays.fill(first,0);
        for(int rank=0;rank<8;rank++) {
            residual+=unary[side][rank];
            for(int r=0;r<width;r++)first[r]+=projection[side][rank*width+r];
        }
        double maximum=Double.NEGATIVE_INFINITY,total=0;
        for(int r=0;r<width;r++) {
            gates[r]=first[r]*PIECE_NORM+model.weight(head+r);
            maximum=Math.max(maximum,gates[r]);
        }
        for(int r=0;r<width;r++) { gates[r]=Math.exp(gates[r]-maximum);total+=gates[r]; }
        for(int r=0;r<width;r++) {
            double sum=0;
            for(int block=0;block<32;block++)sum+=experts[side][block*width+r];
            residual+=(gates[r]/total)*(sum*PAIR_NORM);
        }
        double baseValue;
        if(base==null)baseValue=material=BrnPair2Features.material(board);
        else { baseValue=base.evaluatePawns(board);material=base.materialPawns(side); }
        double value=baseValue+residual;
        if(!Double.isFinite(value))throw new ArithmeticException("Nonfinite GATE accumulator");
        lastRaw[side]=value;lastMaterial[side]=material;valid[side]=true;
        return value;
    }
    public int score(long[] board) {
        double raw=rawPawns(board);
        return Brn3Model.score(material+BrnNextModel.RESIDUAL_GAIN*(raw-material));
    }
    private void rank(int side,int rank) {
        recomputedRanks++;
        double sum=0;int offset=rank*width;
        Arrays.fill(projection[side],offset,offset+width,0);
        for(int file=0;file<8;file++) {
            int square=rank*8+file,category=codes[side][square];
            if(category==0)continue;
            int feature=(category-1)*64+square;
            sum+=PIECE_NORM*model.weight(UNARY+feature);
            int base=EMBEDDING+feature*width;
            for(int r=0;r<width;r++)projection[side][offset+r]+=model.weight(base+r);
        }
        unary[side][rank]=sum;
    }
    private void block(int side,int block) {
        recomputedBlocks++;
        int shape=block>>>3,rank=block&7,offset=block*width;
        Arrays.fill(experts[side],offset,offset+width,0);
        int dx=shape==1?0:shape==3?-1:1,dy=shape==0?0:1;
        if(rank+dy>=8)return;
        for(int file=0;file<8;file++) {
            int x=file+dx;if(x<0||x>=8)continue;
            int address=shape*169+codes[side][rank*8+file]*13+codes[side][(rank+dy)*8+x];
            for(int r=0;r<width;r++)experts[side][offset+r]+=model.weight(head+width+r*PAIR_STATES+address);
        }
    }
}
