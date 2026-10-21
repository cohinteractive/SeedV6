package com.ohinteractive.seedv6.core.brnnext;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn3.Brn3Model;
import java.util.Arrays;
import static com.ohinteractive.seedv6.core.brnnext.BrnNextArchitecture.*;

/** Explicit research cache packing only polynomial components with nonzero output heads.
 * Same periodic rebuild and conservative rounding guard as the dense implementation.
 * The immutable source model/codec remains intact; packed arrays belong to this worker. */
public final class BrnCubicPackedAccumulator {
    final BrnNextModel model;
    final int width=CUBIC.width(),head=CUBIC.head();
    final long[] previous=new long[4];
    final int[][] codes=new int[2][64];
    final double[][] first,second,third;
    final double[] embeddingWeights,head2Weights,head3Weights;
    final int activeWidth;
    final double[] unary=new double[2],last=new double[2],material=new double[2];
    final boolean[] valid=new boolean[2],canonical=new boolean[2];
    private final double correctionErrorBound;
    final int[] ranks;
    final com.ohinteractive.seedv6.core.brnpair2.BrnPair2Model.Workspace base;
    boolean initialized;int changes;public long rebuilds,updates,roundingFallbacks;
    public BrnCubicPackedAccumulator(BrnNextModel model) {
        this(model,BrnNextBase.model());
    }
    public BrnCubicPackedAccumulator(BrnNextModel model,com.ohinteractive.seedv6.core.brnpair2.BrnPair2Model frozenBase) {
        base=java.util.Objects.requireNonNull(frozenBase).newWorkspace();
        if(model.architecture()!=GEN32_CUBIC)throw new IllegalArgumentException("W3 CUBIC only");
        this.model=model;
        double embedding=0,unaryMagnitude=Math.abs(model.weight(0)),head2=0,head3=0;
        for(int i=UNARY;i<EMBEDDING;i++)unaryMagnitude=Math.max(unaryMagnitude,Math.abs(PIECE_NORM*model.weight(i)));
        for(int i=EMBEDDING;i<head;i++)embedding=Math.max(embedding,Math.abs(model.weight(i)));
        for(int r=0;r<width;r++){head2+=Math.abs(model.weight(head+r));head3+=Math.abs(model.weight(head+width+r));}
        double one=64*embedding,two=2016*embedding*embedding,three=41664*embedding*embedding*embedding;
        double e1=0,e2=0,e3=0;
        //64 canonical insertions +64 changes *8 squares *2 remove/add operations,
        // plus64 reference insertions. True intermediate polynomials contain<=64 pieces.
        // Conservative propagation covers both forward and inverse recurrences. Four ulps
        // cover each multiplication/addition and the bound's own rounding; nonfinite bounds
        // force canonical recomputation at the score boundary.
        for(int i=0;i<1152;i++) {
            e1+=4*Math.ulp(Math.max(1,one+e1+embedding));
            e2+=embedding*e1+4*Math.ulp(Math.max(1,two+e2+(one+e1)*embedding));
            e3+=embedding*e2+4*Math.ulp(Math.max(1,three+e3+(two+e2)*embedding));
        }
        correctionErrorBound=4*1153*Math.ulp(Math.max(1,1153*unaryMagnitude))
                +head2*e2*DEGREE2_NORM+head3*e3*DEGREE3_NORM
                +128*Math.ulp(Math.max(1,65*unaryMagnitude+head2*two*DEGREE2_NORM+head3*three*DEGREE3_NORM));
        ranks=java.util.stream.IntStream.range(0,width)
                .filter(r->!Double.isFinite(correctionErrorBound)||model.weight(head+r)!=0||model.weight(head+width+r)!=0).toArray();
        activeWidth=ranks.length;
        first=new double[2][activeWidth];second=new double[2][activeWidth];third=new double[2][activeWidth];
        embeddingWeights=new double[768*activeWidth];head2Weights=new double[activeWidth];head3Weights=new double[activeWidth];
        for(int r=0;r<activeWidth;r++) {
            head2Weights[r]=model.weight(head+ranks[r]);head3Weights[r]=model.weight(head+width+ranks[r]);
            for(int feature=0;feature<768;feature++)embeddingWeights[feature*activeWidth+r]=model.weight(EMBEDDING+feature*width+ranks[r]);
        }
    }
    public double correctionErrorBound() { return correctionErrorBound; }
    public int activeComponents() { return activeWidth; }
    /** Primitive payload; excludes headers, references and alignment. */
    public long primitiveBytes() { return 641+4L*ranks.length+8L*(6L*activeWidth+embeddingWeights.length+head2Weights.length+head3Weights.length)+base.primitiveBytes(); }
    int category(long[] board,int physical,int side) {
        int code=Board.getSquare(board[0],board[1],board[2],board[3],physical),type=code&7;
        if(type==7)throw new IllegalArgumentException("Invalid category");
        return type==0?0:type+(((code>>>3)^side)&1)*6;
    }
    void piece(int side,int square,int code,boolean add) {
        if(code==0)return;
        int feature=(code-1)*64+square;
        unary[side]+=(add?1:-1)*PIECE_NORM*model.weight(UNARY+feature);
        for(int r=0;r<activeWidth;r++) {
            double value=embeddingWeights[feature*activeWidth+r];
            if(add) {
                third[side][r]+=second[side][r]*value;
                second[side][r]+=first[side][r]*value;
                first[side][r]+=value;
            } else {
                // Invert multiplication by(1+value*t), retaining coefficients through degree3.
                first[side][r]-=value;
                second[side][r]-=first[side][r]*value;
                third[side][r]-=second[side][r]*value;
            }
        }
    }
    void rebuild(long[] board) {
        rebuilds++;changes=0;valid[0]=valid[1]=false;canonical[0]=canonical[1]=true;
        for(int side=0;side<2;side++) {
            unary[side]=model.weight(0);Arrays.fill(first[side],0);Arrays.fill(second[side],0);Arrays.fill(third[side],0);
            for(int square=0;square<64;square++) {
                int code=category(board,square^(side*56),side);codes[side][square]=code;piece(side,square,code,true);
            }
        }
    }
    public double rawPawns(long[] board) {
        long changed=0;
        for(int i=0;i<4;i++)changed|=previous[i]^board[i];
        changed&=previous[0]|previous[1]|previous[2]|board[0]|board[1]|board[2];
        if(!initialized||changed!=0) {
            valid[0]=valid[1]=false;
            if(!initialized||changes>=64||Long.bitCount(changed)>8)rebuild(board);
            else {
                updates++;changes++;canonical[0]=canonical[1]=false;
                for(int side=0;side<2;side++)for(long bits=changed;bits!=0;bits&=bits-1) {
                    int physical=Long.numberOfTrailingZeros(bits),square=physical^(side*56);
                    int before=codes[side][square],after=category(board,physical,side);
                    piece(side,square,before,false);piece(side,square,after,true);codes[side][square]=after;
                }
            }
            System.arraycopy(board,0,previous,0,4);initialized=true;
        }
        int side=Board.player((int)board[Board.STATUS]);if(valid[side])return last[side];
        double correction=unary[side];
        for(int r=0;r<activeWidth;r++)correction+=head2Weights[r]*second[side][r]*DEGREE2_NORM
                +head3Weights[r]*third[side][r]*DEGREE3_NORM;
        double value=base.evaluatePawns(board)+correction;material[side]=base.materialPawns(side);
        if(!Double.isFinite(value))throw new ArithmeticException("Nonfinite CUBIC delta evaluation");
        valid[side]=true;return last[side]=value;
    }
    public int score(long[] board) {
        double raw=rawPawns(board),m=material[Board.player((int)board[Board.STATUS])];
        double mapped=m+.25*(raw-m),scaled=100*mapped;
        double guard=25*correctionErrorBound+800*Math.ulp(Math.max(1,Math.abs(raw)))
                +16*Math.ulp(Math.max(1,Math.abs(scaled)));
        double magnitude=Math.abs(scaled);
        if(!canonical[Board.player((int)board[Board.STATUS])]
                &&(!Double.isFinite(guard)||guard>=.25||Math.abs(magnitude-Math.floor(magnitude)-.5)<=guard)) {
            roundingFallbacks++;rebuild(board);raw=rawPawns(board);mapped=m+.25*(raw-m);
        }
        return Brn3Model.score(mapped);
    }
}
