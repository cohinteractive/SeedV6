package com.ohinteractive.seedv6.search.exact;

import java.util.Arrays;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.rules.DrawAdjudicator;
import com.ohinteractive.seedv6.rules.SearchLineHistory;

/** Test-only unpruned tree enumeration. No windows, ordering, PV cache or production search calls. */
final class ExhaustiveOracle {
    private final ExactEvaluator evaluator;
    long nodes;

    ExhaustiveOracle(ExactEvaluator evaluator) { this.evaluator = evaluator; }

    int score(long[] board, SearchLineHistory history, int remaining, int rootPly) {
        nodes++;
        long[] moves = legalMoves(board);
        if(moves.length == 0) {
            boolean check = Board.isPlayerInCheck(board[0], board[1], board[2], board[3],
                    Board.player((int) board[Board.STATUS]));
            return check ? -32768 + rootPly : 0;
        }
        if(DrawAdjudicator.adjudicateNonTerminal(board, history) != DrawAdjudicator.RuleDraw.NONE) return 0;
        if(remaining <= 0) return evaluator.evaluate(board, rootPly);
        int[] scores = new int[moves.length];
        // Deliberately evaluate EVERY legal child before selecting the maximum.
        for(int i = 0; i < moves.length; i++) {
            long[] child = child(board, moves[i]);
            history.pushRealPosition(child);
            try { scores[i] = -score(child, history, remaining - 1, rootPly + 1); }
            finally { history.popRealPosition(); }
        }
        return Arrays.stream(scores).max().orElseThrow();
    }

    static long[] legalMoves(long[] board) {
        long[] buffer = new long[512];
        int count = Gen.genAll(board[0], board[1], board[2], board[3], (int) board[Board.STATUS],
                board[Board.KEY], true, buffer, new long[Board.MAX_BITBOARDS]);
        return Arrays.copyOf(buffer, count);
    }

    static long[] child(long[] board, long move) {
        long[] child = new long[Board.MAX_BITBOARDS];
        Board.makeMoveInto(board[0], board[1], board[2], board[3], (int) board[Board.STATUS],
                board[Board.KEY], move, child);
        return child;
    }
}
