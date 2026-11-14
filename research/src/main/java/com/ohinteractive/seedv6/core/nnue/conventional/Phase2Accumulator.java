package com.ohinteractive.seedv6.core.nnue.conventional;

import com.ohinteractive.seedv6.core.Board;
import java.util.Objects;

/** Worker/ply owned sums. Pawn deltas touch only pairs containing changed pawns. */
public final class Phase2Accumulator {
    private final Phase2Network model;
    private final int[][] sums=new int[2][256];
    private final int[] scratch;
    private final long[] placement=new long[4];
    private boolean prepared;
    Phase2Accumulator(Phase2Network model){this.model=model;scratch=new int[model.kind()==1?528:16];}
    public void rebuild(long[] board){ConventionalFeatures.requireBoard(board);for(int side=0;side<2;side++)refresh(board,side);remember(board);}
    private void refresh(long[] board,int side) {
        Phase2Network.refreshCount();
        model.reset(sums[side]);int king=ConventionalFeatures.king(board,side);long occupied=board[0]|board[1]|board[2];
        while(occupied!=0){int sq=Long.numberOfTrailingZeros(occupied);occupied&=occupied-1;
            model.add(ConventionalFeatures.index(side,king,Board.getSquare(board[0],board[1],board[2],board[3],sq),sq),1,sums[side]);}
        if(model.kind()==2)pairs(board,side,king,-1L,1);
    }
    private void pairs(long[] board,int side,int king,long changed,int sign) {
        long outer=Phase2Features.pawns(board);
        while(outer!=0){int a=Long.numberOfTrailingZeros(outer);outer&=outer-1;long inner=outer;
            while(inner!=0){int b=Long.numberOfTrailingZeros(inner);inner&=inner-1;
                if((((1L<<a)|(1L<<b))&changed)!=0&&Math.abs(a%8-b%8)<=1)
                    model.add(Phase2Features.pair(board,side,king,a,b),sign,sums[side]);}}
    }
    public void update(long[] parentBoard,long[] board,Phase2Accumulator parent) {
        if(parent==this)throw new IllegalArgumentException("Distinct parent required");requireSource(parentBoard,parent);
        ConventionalFeatures.requireBoard(board);long changed=0;for(int i=0;i<4;i++)changed|=parentBoard[i]^board[i];
        long oldPawns=Phase2Features.pawns(parentBoard),newPawns=Phase2Features.pawns(board);
        long pawnChanges=(oldPawns^newPawns)|((parentBoard[3]^board[3])&(oldPawns|newPawns));
        for(int side=0;side<2;side++) {
            int king=ConventionalFeatures.king(board,side);
            if(ConventionalFeatures.context(side,king)!=ConventionalFeatures.context(side,ConventionalFeatures.king(parentBoard,side))) {refresh(board,side);continue;}
            System.arraycopy(parent.sums[side],0,sums[side],0,256);long squares=changed;
            while(squares!=0){int sq=Long.numberOfTrailingZeros(squares);squares&=squares-1;
                int before=Board.getSquare(parentBoard[0],parentBoard[1],parentBoard[2],parentBoard[3],sq);
                int after=Board.getSquare(board[0],board[1],board[2],board[3],sq);
                if(before!=0)model.add(ConventionalFeatures.index(side,king,before,sq),-1,sums[side]);
                if(after!=0)model.add(ConventionalFeatures.index(side,king,after,sq),1,sums[side]);}
            if(model.kind()==2&&pawnChanges!=0){pairs(parentBoard,side,king,pawnChanges,-1);pairs(board,side,king,pawnChanges,1);}
        }remember(board);
    }
    public void copyFrom(long[] board,Phase2Accumulator source){requireSource(board,source);for(int side=0;side<2;side++)System.arraycopy(source.sums[side],0,sums[side],0,256);remember(board);}
    public int evaluate(long[] board){requirePlacement(board);int stm=Board.player((int)board[4]);return model.score(sums[stm],sums[stm^1],model.bucket(board),scratch,false);}
    public int raw(int side,int unit){if(!prepared)throw new IllegalStateException("Unprepared");return sums[Objects.checkIndex(side,2)][Objects.checkIndex(unit,256)];}
    private void requireSource(long[] b,Phase2Accumulator source){if(source.model!=model)throw new IllegalArgumentException("Model mismatch");source.requirePlacement(b);}
    private void requirePlacement(long[] b){if(!prepared||b==null||b.length<6)throw new IllegalStateException("Unprepared/invalid board");for(int i=0;i<4;i++)if(placement[i]!=b[i])throw new IllegalStateException("Stale placement");}
    private void remember(long[] b){System.arraycopy(b,0,placement,0,4);prepared=true;}
}
