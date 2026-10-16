package com.ohinteractive.seedv6.core.nnue;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;

/** Explicit BRN-3 V1 bootstrap parity; never enabled by a legacy NNUE factory/codec.
 * Exact integer hundredths of the reference pawn values, separate from learned layers.
 * One White-relative integer per stack slot; STM changes only its readout sign.
 */
public final class NnueMaterialBootstrap {
    public static final String ID = "seedv6.nnue.brn3-v1-material-additive.v1";
    /** Historical E012 training recipe: continuous counterpart of E008 additive search, V1 scale only.
     * Targets remain bounded STM outcomes; the network learns the residual around M/32511.
     * Integer quantization (including V1's minimum nonzero score) belongs only to search.
     */
    public static final String TRAINING_ID = "seedv6.nnue.material-additive.outcome-mse.adam.v1";
    /** E013: supervise the same total search score in pawns through the existing WDL/CE link. */
    public static final String CALIBRATED_TRAINING_ID = "seedv6.nnue.material-additive.pawn-wdl-ce.adam.v2";
    public static double combinedOutcome(double neuralOutcome, int materialScore) {
        return Math.max(-1, Math.min(1, neuralOutcome + materialScore / (double) TranspositionScores.MAX_NORMAL_SCORE));
    }
    private NnueMaterialBootstrap() {}

    private static int signedValue(int code) {
        int value = switch (code & Piece.TYPE) {
            case 0, Piece.KING -> 0;
            case Piece.PAWN -> 100;
            case Piece.KNIGHT -> 320;
            case Piece.BISHOP -> 330;
            case Piece.ROOK -> 500;
            case Piece.QUEEN -> 900;
            default -> throw new IllegalArgumentException("Invalid piece identity");
        };
        return (code & 8) == 0 ? value : -value;
    }

    public static int whiteScore(long[] board) {
        int score = 0;
        for (long bits = board[0] | board[1] | board[2]; bits != 0; bits &= bits - 1) {
            score += signedValue(Board.getSquare(board[0], board[1], board[2], board[3], Long.numberOfTrailingZeros(bits)));
        }
        return score;
    }

    /** Changed-square delta handles captures, EP, promotions, castling and null moves.
     * No allocation, floating-point drift, placement rescan, or mutation of parent state.
     */
    public static int update(long[] parent, long[] child, int parentWhiteScore) {
        int score = parentWhiteScore;
        long changed = (parent[0] ^ child[0]) | (parent[1] ^ child[1])
                | (parent[2] ^ child[2]) | (parent[3] ^ child[3]);
        for (long bits = changed; bits != 0; bits &= bits - 1) {
            int square = Long.numberOfTrailingZeros(bits);
            score -= signedValue(Board.getSquare(parent[0], parent[1], parent[2], parent[3], square));
            score += signedValue(Board.getSquare(child[0], child[1], child[2], child[3], square));
        }
        return score;
    }

    public static int forSideToMove(long[] board, int whiteScore) {
        return Board.player((int) board[4]) == 0 ? whiteScore : -whiteScore;
    }

    /** Existing NNUE integer residual plus fixed material in shared engine score units.
     * Preserve the residual's existing mapping/rounding; clamp the sum below mate scores.
     * This does not assert that the legacy learned output is calibrated centipawns.
     */
    public static int combine(int materialScore, int neuralScore) {
        return (int) Math.max(-TranspositionScores.MAX_NORMAL_SCORE,
                Math.min(TranspositionScores.MAX_NORMAL_SCORE, (long) materialScore + neuralScore));
    }
}
