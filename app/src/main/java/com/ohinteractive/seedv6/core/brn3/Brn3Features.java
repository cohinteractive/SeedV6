package com.ohinteractive.seedv6.core.brn3;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.util.Piece;

/** Permitted innate information only: identity, ownership, square, STM and fixed material. */
public final class Brn3Features {
    static int channel(int code) {
        int type=code&Piece.TYPE;
        if(type<Piece.KING || type>Piece.PAWN)throw new IllegalArgumentException("Invalid piece identity");
        return type-1+((code>>>3)&1)*6;
    }
    static double pieceMaterial(int code) {
        return switch(code&Piece.TYPE) {
            case Piece.PAWN->1;case Piece.KNIGHT->3.2;case Piece.BISHOP->3.3;
            case Piece.ROOK->5;case Piece.QUEEN->9;case Piece.KING->0;
            default->throw new IllegalArgumentException("Invalid piece identity");
        };
    }
    public static double material(long[] board) {
        double white=0;
        for(long bits=board[0]|board[1]|board[2];bits!=0;bits&=bits-1) {
            int code=Board.getSquare(board[0],board[1],board[2],board[3],Long.numberOfTrailingZeros(bits));
            double value=pieceMaterial(code);white+=(code&8)==0?value:-value;
        }
        return Board.player((int)board[4])==0?white:-white;
    }
    private Brn3Features(){}
}
