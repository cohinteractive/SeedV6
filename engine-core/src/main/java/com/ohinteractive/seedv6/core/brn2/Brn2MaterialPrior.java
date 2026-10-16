package com.ohinteractive.seedv6.core.brn2;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.util.Piece;

/** Fixed output knowledge, separate from trainable weights and Adam moments. */
public enum Brn2MaterialPrior {
    NONE, BASIC_V1;

    /** BASIC_V1 binds one public score unit to one centipawn. Matches BrnScoreMapping.SCALE. */
    public static final int SCORE_SCALE = 32511;

    /** Literal current-piece material from the existing canonical side-to-move perspective. */
    public int score(long[] board) {
        if (this == NONE) return 0;
        int perspective = Board.player((int) board[Board.STATUS]), material = 0;
        for (long remaining = Brn2Features.orient(Brn2Features.occupied(board), perspective);
                remaining != 0; remaining &= remaining - 1) {
            int code = Brn2Features.code(board, Long.numberOfTrailingZeros(remaining), perspective);
            int value = switch (code & Piece.TYPE) {
                case Piece.PAWN -> 100;
                case Piece.KNIGHT -> 320;
                case Piece.BISHOP -> 330;
                case Piece.ROOK -> 500;
                case Piece.QUEEN -> 900;
                default -> 0; // Kings and unused packed piece codes have no material value.
            };
            material += (code & (1 << Board.PLAYER_SHIFT)) == 0 ? value : -value;
        }
        return material;
    }

    /** Invert the verified linear score mapping and tanh; keep the full-position loss unchanged. */
    double combine(long[] board, double residual) {
        if (this == NONE) return residual; // Preserve even signed-zero legacy preactivations.
        double value = score(board) / (double) SCORE_SCALE;
        if (Math.abs(value) >= 1)
            throw new IllegalArgumentException("BRN-2 material exceeds the normal score domain.");
        return residual + .5 * (StrictMath.log1p(value) - StrictMath.log1p(-value));
    }
}
