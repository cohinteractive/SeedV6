package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.core.util.Value;

/** Display-only pawn-equivalent material on the current board, including promoted pieces. */
final class BoardMaterial {
    static final int[] ORDER = {Piece.PAWN, Piece.KNIGHT, Piece.BISHOP, Piece.ROOK, Piece.QUEEN};

    static int balance(long[] board, int side) {
        int white = 0;
        for (int square = 0; square < 64; square++) {
            int piece = Board.getSquare(board[0], board[1], board[2], board[3], square);
            int value = switch (piece & Piece.TYPE) {
                case Piece.PAWN -> 1;
                case Piece.KNIGHT, Piece.BISHOP -> 3;
                case Piece.ROOK -> 5;
                case Piece.QUEEN -> 9;
                default -> 0;
            };
            white += (piece >>> 3) == Value.WHITE ? value : -value;
        }
        return side == Value.WHITE ? white : -white;
    }

    static String signed(int balance) { return "(" + (balance > 0 ? "+" : "") + balance + ")"; }
    private BoardMaterial() {}
}
