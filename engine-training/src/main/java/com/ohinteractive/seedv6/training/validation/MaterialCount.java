package com.ohinteractive.seedv6.training.validation;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.util.Piece;

/** Actual board material in pawn units (P=1, N/B=3, R=5, Q=9; kings excluded).
 * Computed only at game completion or for a displayed position, never during search. */
public record MaterialCount(int white, int black) {
    public MaterialCount {
        if (white < 0 || black < 0) throw new IllegalArgumentException("Negative material");
    }
    public static MaterialCount from(long[] board) {
        int white = 0, black = 0;
        for (int square = 0; square < 64; square++) {
            int piece = Board.getSquare(board[0], board[1], board[2], board[3], square);
            int value = switch (piece & Piece.TYPE) {
                case Piece.PAWN -> 1;
                case Piece.KNIGHT, Piece.BISHOP -> 3;
                case Piece.ROOK -> 5;
                case Piece.QUEEN -> 9;
                default -> 0;
            };
            if (piece >>> 3 == 0) white += value; else black += value;
        }
        return new MaterialCount(white, black);
    }
}
