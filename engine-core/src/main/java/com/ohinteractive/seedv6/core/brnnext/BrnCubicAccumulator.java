package com.ohinteractive.seedv6.core.brnnext;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn3.Brn3Model;
import com.ohinteractive.seedv6.core.brnpair2.BrnPair2Features;
import java.util.Arrays;
import static com.ohinteractive.seedv6.core.brnnext.BrnNextArchitecture.*;

/** Fixed-order products of rank polynomials; dirty ranks never depend on old floating sums. */
public final class BrnCubicAccumulator {
    private final BrnNextModel model;
    private final int width,head;
    private final long[] previous=new long[4];
    private final int[][] codes=new int[2][64];
    private final int[] dirty={255,255};
    private final double[][] unary=new double[2][8],one,two,three;
    private final double[] first,second,third,lastRaw=new double[2],lastMaterial=new double[2];
    private final boolean[] valid=new boolean[2];
    private boolean initialized;
    private final com.ohinteractive.seedv6.core.brnpair2.BrnPair2Model.Workspace base;
    long recomputedRanks;
    public BrnCubicAccumulator(BrnNextModel model) {
        if(model.architecture().correctionKind()!=CUBIC||model.architecture().calibratedHeads())throw new IllegalArgumentException("Ordinary CUBIC model required");
        this.model=model;
        width=model.architecture().width();head=model.architecture().head();
        one=new double[2][8*width];two=new double[2][8*width];three=new double[2][8*width];
        first=new double[width];second=new double[width];third=new double[width];
        base=model.architecture().frozenBase()?BrnNextBase.model().newWorkspace():null;
    }
    public long primitiveBytes() { return 8L*(4+2*8+6*8*width+3*width+4)+4L*(2*64+2)+2+(base==null?0:base.primitiveBytes()); }
    public double rawPawns(long[] board) {
        long changed=0;
        for(int i=0;i<4;i++)changed|=previous[i]^board[i];
        changed&=previous[0]|previous[1]|previous[2]|board[0]|board[1]|board[2];
        if(!initialized)changed=-1L;
        if(changed!=0) {
            valid[0]=valid[1]=false;
            for(long bits=changed;bits!=0;bits&=bits-1) {
                int square=Long.numberOfTrailingZeros(bits);
                int code=Board.getSquare(board[0],board[1],board[2],board[3],square),type=code&7;
                if(type==7)throw new IllegalArgumentException("Invalid piece category");
                for(int side=0;side<2;side++) {
                    int canonical=square^(side*56);
                    codes[side][canonical]=type==0?0:type+(((code>>>3)^side)&1)*6;
                    dirty[side]|=1<<(canonical>>>3);
                }
            }
        }
        System.arraycopy(board,0,previous,0,4);initialized=true;
        int side=Board.player((int)board[Board.STATUS]);
        if(valid[side])return lastRaw[side];
        for(int ranks=dirty[side];ranks!=0;ranks&=ranks-1)rank(side,Integer.numberOfTrailingZeros(ranks));
        dirty[side]=0;
        Arrays.fill(first,0);Arrays.fill(second,0);Arrays.fill(third,0);
        double residual=model.weight(0);
        for(int rank=0;rank<8;rank++) {
            residual+=unary[side][rank];int offset=rank*width;
            for(int r=0;r<width;r++) {
                double a=one[side][offset+r],b=two[side][offset+r],c=three[side][offset+r];
                third[r]+=second[r]*a+first[r]*b+c;
                second[r]+=first[r]*a+b;
                first[r]+=a;
            }
        }
        for(int r=0;r<width;r++)residual+=model.weight(head+r)*second[r]*DEGREE2_NORM
                +model.weight(head+width+r)*third[r]*DEGREE3_NORM;
        double baseValue;
        if(base==null)baseValue=lastMaterial[side]=BrnPair2Features.material(board);
        else { baseValue=base.evaluatePawns(board);lastMaterial[side]=base.materialPawns(side); }
        double result=baseValue+residual;
        if(!Double.isFinite(result))throw new ArithmeticException("Nonfinite CUBIC accumulator");
        lastRaw[side]=result;valid[side]=true;return result;
    }
    public int score(long[] board) {
        double raw=rawPawns(board),material=lastMaterial[Board.player((int)board[Board.STATUS])];
        return Brn3Model.score(material+BrnNextModel.RESIDUAL_GAIN*(raw-material));
    }
    private void rank(int side,int rank) {
        recomputedRanks++;int offset=rank*width;double sum=0;
        Arrays.fill(one[side],offset,offset+width,0);
        Arrays.fill(two[side],offset,offset+width,0);
        Arrays.fill(three[side],offset,offset+width,0);
        for(int file=0;file<8;file++) {
            int square=rank*8+file,category=codes[side][square];if(category==0)continue;
            int feature=(category-1)*64+square,base=EMBEDDING+feature*width;
            sum+=PIECE_NORM*model.weight(UNARY+feature);
            for(int r=0;r<width;r++) {
                int i=offset+r;double value=model.weight(base+r);
                three[side][i]+=two[side][i]*value;two[side][i]+=one[side][i]*value;one[side][i]+=value;
            }
        }
        unary[side][rank]=sum;
    }
}
