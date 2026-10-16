package com.ohinteractive.seedv6.search.quiescence;

import java.util.Arrays;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.rules.DrawAdjudicator;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.rules.SearchLineHistory;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;

/**
 * Deliberately allocating, direct-generation qsearch oracle. It shares the
 * score/depth policy but no production move picker, frame storage, or control
 * flow.
 */
final class QuiescenceOracle {

    static int search(long[] board, int alpha, int beta) {
        return search(board, GameHistory.initial(board), 0, 0, alpha, beta);
    }

    static int search(
        long[] board, GameHistory gameHistory, int absolutePly, int qPly,
        int alpha, int beta
    ) {
        final SearchLineHistory history = new SearchLineHistory(gameHistory);
        try {
            return node(
                Arrays.copyOf(board, Board.MAX_BITBOARDS), history,
                absolutePly, qPly, alpha, beta
            );
        } finally {
            history.restoreRoot();
        }
    }

    private static final int MAX_MOVES = 256;

    private static int node(
        long[] board, SearchLineHistory history, int absolutePly, int qPly,
        int alpha, int beta
    ) {
        final long[] moves = new long[MAX_MOVES];
        final long[] scratch = new long[Board.MAX_BITBOARDS];
        final long checkers = checkers(board);
        final boolean inCheck = checkers != 0L;

        if(!inCheck && qPly >= QuiescenceSearch.SOFT_QPLY_LIMIT) {
            final int legalCount = Gen.genAll(
                board[0], board[1], board[2], board[3],
                (int) board[Board.STATUS], board[Board.KEY], true, moves, scratch
            );
            if(legalCount == 0) return 0;
            if(isDraw(board, history)) return 0;
            return Eval.evaluate(board);
        }

        final int moveCount = inCheck
            ? Gen.genEvasion(
                board[0], board[1], board[2], board[3],
                (int) board[Board.STATUS], board[Board.KEY], true,
                checkers, moves, scratch
            )
            : Gen.genTactical(
                board[0], board[1], board[2], board[3],
                (int) board[Board.STATUS], board[Board.KEY], true, moves, scratch
            );

        if(inCheck) {
            if(moveCount == 0) return -TranspositionScores.MATE_SCORE + absolutePly;
            if(isDraw(board, history)) return 0;
            return children(
                board, moves, moveCount, history, absolutePly, qPly,
                alpha, beta, Integer.MIN_VALUE
            );
        }

        if(moveCount == 0) {
            final int quietCount = Gen.genQuiet(
                board[0], board[1], board[2], board[3],
                (int) board[Board.STATUS], board[Board.KEY], true, moves, scratch
            );
            if(quietCount == 0) return 0;
        }
        if(isDraw(board, history)) return 0;

        final int standPat = Eval.evaluate(board);
        if(standPat >= beta) return standPat;
        if(moveCount == 0) return standPat;
        return children(
            board, moves, moveCount, history, absolutePly, qPly,
            Math.max(alpha, standPat), beta, standPat
        );
    }

    private static int children(
        long[] board, long[] moves, int moveCount, SearchLineHistory history,
        int absolutePly, int qPly, int alpha, int beta, int initialBest
    ) {
        int best = initialBest;
        for(int index = 0; index < moveCount; index ++) {
            if(absolutePly == QuiescenceSearch.MAX_ABSOLUTE_PLY) {
                throw new QuiescenceSearch.QuiescenceCapacityException(
                    "Oracle cannot enter a child beyond absolute ply "
                        + QuiescenceSearch.MAX_ABSOLUTE_PLY
                );
            }
            final long[] child = new long[Board.MAX_BITBOARDS];
            Board.makeMoveInto(
                board[0], board[1], board[2], board[3],
                (int) board[Board.STATUS], board[Board.KEY], moves[index], child
            );
            history.pushRealPosition(child);
            final int score;
            try {
                score = -node(
                    child, history, absolutePly + 1, qPly + 1, -beta, -alpha
                );
            } finally {
                history.popRealPosition();
            }
            if(score > best) best = score;
            if(score >= beta) return score;
            if(score > alpha) alpha = score;
        }
        return best;
    }

    private static boolean isDraw(long[] board, SearchLineHistory history) {
        return DrawAdjudicator.adjudicateNonTerminal(board, history)
            != DrawAdjudicator.RuleDraw.NONE;
    }

    private static long checkers(long[] board) {
        final int status = (int) board[Board.STATUS];
        final int player = status & Board.PLAYER_BIT;
        final long occupancy = board[0] | board[1] | board[2];
        final long colour = ~(-(long) player ^ board[3]);
        final long king = board[0] & ~board[1] & ~board[2] & colour;
        return Board.getCheckersPext(
            board[0], board[1], board[2], board[3], colour, player,
            Long.numberOfTrailingZeros(king), occupancy
        );
    }

    private QuiescenceOracle() {}
}
