package com.ohinteractive.seedv6.core.move;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.core.util.Value;

/** Classification of legal encoded captures, en passant and promotions. */
public final class MoveTactics {
    public static boolean isTactical(long[] board, long move) {
        java.util.Objects.requireNonNull(board, "board");
        if(board.length < Board.MAX_BITBOARDS)
            throw new IllegalArgumentException("Board must contain at least " + Board.MAX_BITBOARDS + " longs.");
        if(move == 0L) return false;

        final int targetPiece = (int) (move >>> Board.TARGET_PIECE_SHIFT) & Board.PIECE_BITS;
        final int promotePiece = (int) (move >>> Board.PROMOTE_PIECE_SHIFT) & Board.PIECE_BITS;
        if(targetPiece != Value.NONE || promotePiece != Value.NONE) return true;

        final int startPiece = (int) (move >>> Board.START_PIECE_SHIFT) & Board.PIECE_BITS;
        if((startPiece & Piece.TYPE) != Piece.PAWN) return false;
        final int from = Move.fromSquare(move);
        final int target = Move.toSquare(move);
        return (from & Value.FILE) != (target & Value.FILE)
            && target == Board.enPassantSquare(Math.toIntExact(board[Board.STATUS]));
    }

    private MoveTactics() {}
}
