package com.ohinteractive.seedv6.search.flat;

import java.util.Arrays;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.rules.DrawAdjudicator;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.rules.SearchLineHistory;

final class ExactNegamaxOracle {

    record Result(int score, boolean hasMove, int legalRootMoves) {}

    static Result search(long[] board, int depth) {
        return search(board, GameHistory.initial(board), depth);
    }

    static Result search(long[] board, GameHistory gameHistory, int depth) {
        if(depth < 1) throw new IllegalArgumentException("Depth must be at least 1.");
        final SearchLineHistory history = new SearchLineHistory(gameHistory);
        try {
            final NodeResult result = searchNode(
                Arrays.copyOf(board, Board.MAX_BITBOARDS), depth, 0, history
            );
            return new Result(result.score(), result.legalMoves() != 0, result.legalMoves());
        } finally {
            history.restoreRoot();
        }
    }

    private static final int MATE_SCORE = 32768;
    private static final int NEG_INF = -1_000_000_000;
    private static final int MAX_MOVES = 256;

    private record NodeResult(int score, int legalMoves) {}

    private static NodeResult searchNode(
        long[] board, int depth, int ply, SearchLineHistory history
    ) {
        final long[] moves = new long[MAX_MOVES];
        final long[] scratch = new long[Board.MAX_BITBOARDS];
        final int moveCount = Gen.genAll(
            board[0], board[1], board[2], board[3],
            (int) board[Board.STATUS], board[Board.KEY], true, moves, scratch
        );
        if(moveCount == 0) {
            final int player = (int) board[Board.STATUS] & Board.PLAYER_BIT;
            final boolean inCheck = Board.isPlayerInCheckPext(
                board[0], board[1], board[2], board[3], player
            );
            return new NodeResult(inCheck ? -MATE_SCORE + ply : 0, 0);
        }
        if(DrawAdjudicator.adjudicateNonTerminal(board, history)
            != DrawAdjudicator.RuleDraw.NONE) {
            return new NodeResult(0, moveCount);
        }
        if(depth == 0) {
            return new NodeResult(
                Eval.eval(board[0], board[1], board[2], board[3], (int) board[Board.STATUS], board[Board.KEY]),
                moveCount
            );
        }

        int bestScore = NEG_INF;
        for(int i = 0; i < moveCount; i ++) {
            final long[] child = new long[Board.MAX_BITBOARDS];
            Board.makeMoveInto(
                board[0], board[1], board[2], board[3],
                (int) board[Board.STATUS], board[Board.KEY], moves[i], child
            );
            history.pushRealPosition(child);
            final int score;
            try {
                score = -searchNode(child, depth - 1, ply + 1, history).score();
            } finally {
                history.popRealPosition();
            }
            if(score > bestScore) bestScore = score;
        }
        return new NodeResult(bestScore, moveCount);
    }

    private ExactNegamaxOracle() {}

}
