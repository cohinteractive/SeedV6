package com.ohinteractive.seedv6.core.brnnext;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn3.Brn3Model;
import java.util.Arrays;
import static com.ohinteractive.seedv6.core.brnnext.BrnNextArchitecture.*;

/** Research-only GATE delta cache with periodic canonical rebuilding and a rounding guard.
 * Separate owners support arbitrary placement transitions, undo and null perspectives.
 * Ordinary application loading retains the fixed-order block cache. */
public final class BrnGatedDeltaAccumulator {
    static final int[] A=new int[210],B=new int[210],SHAPE=new int[210];
    static final int[][] INCIDENT=new int[64][];
    static {
        int n=0;
        for(int shape=0;shape<4;shape++) {
            int dx=shape==1?0:shape==3?-1:1,dy=shape==0?0:1;
            for(int square=0;square<64;square++) {
                int x=(square&7)+dx,y=(square>>>3)+dy;
                if(x<0||x>=8||y>=8)continue;
                A[n]=square;B[n]=y*8+x;SHAPE[n++]=shape;
            }
        }
        if(n!=210)throw new IllegalStateException("Pair geometry");
        for(int square=0;square<64;square++) {
            int count=0;for(int p=0;p<210;p++)if(A[p]==square||B[p]==square)count++;
            INCIDENT[square]=new int[count];count=0;
            for(int p=0;p<210;p++)if(A[p]==square||B[p]==square)INCIDENT[square][count++]=p;
        }
    }
    final BrnNextModel model;
    final int head=GATED_PAIRS.head();
    final long[] previous=new long[4];
    final int[][] codes=new int[2][64],addresses=new int[2][210];
    final int[] stamps=new int[210];
    final double[][] projection=new double[2][4],experts=new double[2][4];
    final double[] unary=new double[2],gates=new double[4],last=new double[2],material=new double[2];
    final boolean[] valid=new boolean[2],canonical=new boolean[2];
    private final double correctionErrorBound;
    final com.ohinteractive.seedv6.core.brnpair2.BrnPair2Model.Workspace base;
    boolean initialized;
    int changes,serial;
    public long rebuilds,updates,roundingFallbacks;
    public BrnGatedDeltaAccumulator(BrnNextModel model) {
        this(model,BrnNextBase.model());
    }
    /** Explicit research composition; no source model, base or codec is changed. */
    public BrnGatedDeltaAccumulator(BrnNextModel model,com.ohinteractive.seedv6.core.brnpair2.BrnPair2Model frozenBase) {
        if(model.architecture()!=GEN32_GATED)throw new IllegalArgumentException("W3 GATE only");
        base=java.util.Objects.requireNonNull(frozenBase).newWorkspace();
        this.model=model;
        double u=Math.abs(model.weight(0)),embedding=0,expert=0,bias=0;
        for(int i=UNARY;i<EMBEDDING;i++)u=Math.max(u,Math.abs(PIECE_NORM*model.weight(i)));
        for(int i=EMBEDDING;i<head;i++)embedding=Math.max(embedding,Math.abs(model.weight(i)));
        for(int i=head;i<head+4;i++)bias=Math.max(bias,Math.abs(model.weight(i)));
        for(int i=head+4;i<model.architecture().parameters();i++)expert=Math.max(expert,Math.abs(model.weight(i)));
        // At most64 updates *8 changed squares; each square has at most8 incident pairs.
        // Bound summation roundoff using maximum term magnitudes, including the canonical
        // reference sum. Factor4 exceeds the round-to-nearest gamma_n bound here.
        double logitError=PIECE_NORM*sumError(1088,embedding)
                +4*Math.ulp(Math.max(1,64*PIECE_NORM*embedding+bias));
        // Softmax is2-Lipschitz from infinity norm to L1; its shifted exp inputs are<=0,
        // normalization denominator>=1. Include arithmetic/Math.exp rounding separately.
        double gateError=2*logitError+128*Math.ulp(1.0);
        correctionErrorBound=sumError(1089,u)+PAIR_NORM*(sumError(8402,expert)+210*expert*gateError)
                +32*Math.ulp(Math.max(1,65*u+PAIR_NORM*210*expert));
    }
    private static double sumError(int operations,double termMagnitude) {
        return 4.0*operations*Math.ulp(Math.max(1,operations*termMagnitude));
    }
    public double correctionErrorBound() { return correctionErrorBound; }
    /** Primitive payload only; excludes object/array headers, references and alignment. */
    public long primitiveBytes() { return 3421+base.primitiveBytes(); }
    public static long sharedGeometryPrimitiveBytes() { return 4200; }
    int category(long[] board,int physical,int side) {
        int code=Board.getSquare(board[0],board[1],board[2],board[3],physical),type=code&7;
        if(type==7)throw new IllegalArgumentException("Invalid category");
        return type==0?0:type+(((code>>>3)^side)&1)*6;
    }
    int address(int side,int pair) { return SHAPE[pair]*169+codes[side][A[pair]]*13+codes[side][B[pair]]; }
    void piece(int side,int square,int code,double sign) {
        if(code==0)return;
        int feature=(code-1)*64+square;
        unary[side]+=sign*PIECE_NORM*model.weight(UNARY+feature);
        for(int r=0;r<4;r++)projection[side][r]+=sign*model.weight(EMBEDDING+feature*4+r);
    }
    void rebuild(long[] board) {
        rebuilds++;changes=0;serial=0;Arrays.fill(stamps,0);
        valid[0]=valid[1]=false;canonical[0]=canonical[1]=true;
        for(int side=0;side<2;side++) {
            unary[side]=model.weight(0);Arrays.fill(projection[side],0);Arrays.fill(experts[side],0);
            for(int square=0;square<64;square++) {
                int code=category(board,square^(side*56),side);codes[side][square]=code;piece(side,square,code,1);
            }
            for(int pair=0;pair<210;pair++) {
                int address=addresses[side][pair]=address(side,pair);
                for(int r=0;r<4;r++)experts[side][r]+=model.weight(head+4+r*PAIR_STATES+address);
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
                updates++;changes++;
                canonical[0]=canonical[1]=false;
                for(int side=0;side<2;side++) {
                    serial++;
                    for(long bits=changed;bits!=0;bits&=bits-1) {
                        int physical=Long.numberOfTrailingZeros(bits),square=physical^(side*56);
                        int before=codes[side][square],after=category(board,physical,side);
                        piece(side,square,before,-1);piece(side,square,after,1);codes[side][square]=after;
                    }
                    for(long bits=changed;bits!=0;bits&=bits-1) {
                        int square=Long.numberOfTrailingZeros(bits)^(side*56);
                        for(int pair:INCIDENT[square]) {
                            if(stamps[pair]==serial)continue;stamps[pair]=serial;
                            int before=addresses[side][pair],after=address(side,pair);
                            for(int r=0;r<4;r++)experts[side][r]+=model.weight(head+4+r*PAIR_STATES+after)
                                    -model.weight(head+4+r*PAIR_STATES+before);
                            addresses[side][pair]=after;
                        }
                    }
                }
            }
            System.arraycopy(board,0,previous,0,4);initialized=true;
        }
        int side=Board.player((int)board[Board.STATUS]);
        if(valid[side])return last[side];
        double maximum=Double.NEGATIVE_INFINITY,total=0;
        for(int r=0;r<4;r++){gates[r]=projection[side][r]*PIECE_NORM+model.weight(head+r);maximum=Math.max(maximum,gates[r]);}
        for(int r=0;r<4;r++){gates[r]=Math.exp(gates[r]-maximum);total+=gates[r];}
        double correction=unary[side];
        for(int r=0;r<4;r++)correction+=(gates[r]/total)*(experts[side][r]*PAIR_NORM);
        double value=base.evaluatePawns(board)+correction;material[side]=base.materialPawns(side);
        if(!Double.isFinite(value))throw new ArithmeticException("Nonfinite gated delta evaluation");
        valid[side]=true;return last[side]=value;
    }
    public int score(long[] board) {
        double raw=rawPawns(board),m=material[Board.player((int)board[Board.STATUS])];
        double mapped=m+.25*(raw-m),scaled=100*mapped;
        // Rebuild close to a half-integer boundary rather than relying on tiny rolling
        // errors being harmless. Extreme finite weights conservatively force rebuilding.
        double guard=25*correctionErrorBound+800*Math.ulp(Math.max(1,Math.abs(raw)))
                +16*Math.ulp(Math.max(1,Math.abs(scaled)));
        double magnitude=Math.abs(scaled);
        if(!canonical[Board.player((int)board[Board.STATUS])]
                && (!Double.isFinite(guard)||guard>=.25||Math.abs(magnitude-Math.floor(magnitude)-.5)<=guard)) {
            roundingFallbacks++;rebuild(board);
            raw=rawPawns(board);mapped=m+.25*(raw-m);
        }
        return Brn3Model.score(mapped);
    }
}
