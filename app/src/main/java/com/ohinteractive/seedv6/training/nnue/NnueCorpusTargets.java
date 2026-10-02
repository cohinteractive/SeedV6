package com.ohinteractive.seedv6.training.nnue;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.corpus.CorpusRecord;

/** Stockfish 17.1 material WDL model, frozen as an outcome-supervision adapter.
 * Source: official-stockfish/Stockfish, commit 03e27488f3d21d8ff4dbf3065603afa21dbd0ef3,
 * src/uci.cpp, win_rate_params, win_rate_model, to_cp and wdl (lines 470-533).
 * Stored CP is treated as the centre of the displayed CP rounding bin: x = cp*a/100.
 * It cannot reconstruct an original engine's hidden integer evaluation or version.
 * Formula, coefficient order, material clamp, StrictMath and per-mille rounding are V1.
 * See docs/training/NNUE_CORPUS.md. Generated terminal WDL labels are unaffected.
 */
public final class NnueCorpusTargets {
    public static final String ID = "STOCKFISH_WDL_V1";
    public static final String SOURCE_COMMIT = "03e27488f3d21d8ff4dbf3065603afa21dbd0ef3";
    public record Wdl(int wins, int draws, int losses) {
        public Wdl {
            if (wins < 0 || draws < 0 || losses < 0 || wins + draws + losses != 1000)
                throw new IllegalArgumentException("Invalid WDL probabilities");
        }
        public double target() { return (wins - losses) / 1000.0; }
    }

    public static String rejection(CorpusRecord record) {
        if (record.perspective() != CorpusRecord.WHITE && record.perspective() != CorpusRecord.SIDE_TO_MOVE)
            return "unsupported-perspective";
        if (record.targetKind() == CorpusRecord.MATE) return record.target() == 0 ? "mate-zero" : null;
        return record.targetKind() == CorpusRecord.CP ? null : "unsupported-target";
    }

    public static double target(CorpusRecord record, long[] board) {
        if (rejection(record) != null) throw new IllegalArgumentException("Unsupported NNUE corpus target");
        long stm = record.target(); // Widen before negating Integer.MIN_VALUE.
        if (record.perspective() == CorpusRecord.WHITE && Board.player((int) board[Board.STATUS]) == Value.BLACK)
            stm = -stm;
        if (record.targetKind() == CorpusRecord.MATE) return stm > 0 ? 1 : -1;
        return wdl(stm, material(board)).target();
    }

    /** Both colours; P/N/B/R/Q = 1/3/3/5/9, kings contribute zero. */
    public static int material(long[] board) {
        int material = 0;
        long occupied = board[0] | board[1] | board[2];
        while (occupied != 0) {
            int square = Long.numberOfTrailingZeros(occupied);
            occupied &= occupied - 1;
            int type = Board.getSquare(board[0], board[1], board[2], board[3], square) & Piece.TYPE;
            material += switch (type) {
                case Piece.PAWN -> 1;
                case Piece.KNIGHT, Piece.BISHOP -> 3;
                case Piece.ROOK -> 5;
                case Piece.QUEEN -> 9;
                case Piece.KING -> 0;
                default -> throw new IllegalArgumentException("Invalid corpus piece");
            };
        }
        return material;
    }

    public static Wdl wdl(long stmCp, int material) {
        if (material < 0) throw new IllegalArgumentException("Negative material");
        double m = Math.clamp(material, 17, 78) / 58.0;
        double a = ((-13.50030198 * m + 40.92780883) * m - 36.82753545) * m + 386.83004070;
        double b = ((96.53354896 * m - 165.79058388) * m + 90.89679019) * m + 49.29561889;
        double x = stmCp * a / 100.0;
        int wins = (int) (0.5 + 1000 / (1 + StrictMath.exp((a - x) / b)));
        int losses = (int) (0.5 + 1000 / (1 + StrictMath.exp((a + x) / b)));
        return new Wdl(wins, 1000 - wins - losses, losses);
    }
    private NnueCorpusTargets() {}
}
