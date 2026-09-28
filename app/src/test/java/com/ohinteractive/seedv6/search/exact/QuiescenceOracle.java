package com.ohinteractive.seedv6.search.exact;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.rules.DrawAdjudicator;
import com.ohinteractive.seedv6.rules.SearchLineHistory;

/** Test-only complete enumeration: full legal generation, no windows, SEE, sorting or cutoffs. */
final class QuiescenceOracle {
    private final ExactEvaluator evaluator;
    long nodes;

    QuiescenceOracle(ExactEvaluator evaluator) { this.evaluator = evaluator; }

    int score(long[] board, SearchLineHistory history, int remaining, int ply) {
        // A fixture guard rejects an unsuitable test; it never substitutes a chess value.
        if(++nodes > 200_000 || ply > 64) throw new AssertionError("Unbounded oracle fixture");
        long[] moves = ExhaustiveOracle.legalMoves(board);
        boolean check = Board.isPlayerInCheck(board[0], board[1], board[2], board[3],
                Board.player((int) board[Board.STATUS]));
        if(moves.length == 0) return check ? -32768 + ply : 0;
        if(DrawAdjudicator.adjudicateNonTerminal(board, history) != DrawAdjudicator.RuleDraw.NONE) return 0;
        int best = remaining <= 0 && !check ? evaluator.evaluate(board, ply) : -32769;
        for(long move : moves) {
            if(remaining <= 0 && !check && !tactical(board, move)) continue;
            long[] child = ExhaustiveOracle.child(board, move);
            history.pushRealPosition(child);
            int value;
            try { value = -score(child, history, remaining - 1, ply + 1); }
            finally { history.popRealPosition(); }
            best = Math.max(best, value);
        }
        return best;
    }

    static boolean tactical(long[] board, long move) {
        return ((move >>> Board.TARGET_PIECE_SHIFT) & Board.PIECE_BITS) != 0
                || ((move >>> Board.PROMOTE_PIECE_SHIFT) & Board.PIECE_BITS) != 0
                || (((move >>> Board.START_PIECE_SHIFT) & Piece.TYPE) == Piece.PAWN
                    && Move.toSquare(move) == Board.enPassantSquare((int) board[Board.STATUS]));
    }
}
