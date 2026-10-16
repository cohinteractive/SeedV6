package com.ohinteractive.seedv6.search.alphabeta;

import java.util.Arrays;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.rules.DrawAdjudicator;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.rules.SearchLineHistory;
import com.ohinteractive.seedv6.search.common.SearchControl;
import com.ohinteractive.seedv6.search.quiescence.QuiescenceSearch;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;

/** Allocating full-width oracle sharing only the accepted WS9 leaf boundary. */
final class AlphaBetaReference {

    record Result(int score, long bestMove, int legalRootMoves, long nodes) {}

    static Result search(long[] board, GameHistory gameHistory, int depth) {
        final SearchLineHistory history = new SearchLineHistory(gameHistory);
        final State state = new State();
        try {
            final Node node = state.searchNode(
                Arrays.copyOf(board, Board.MAX_BITBOARDS), history, depth, 0
            );
            return new Result(node.score, node.bestMove, node.legalMoves, state.nodes);
        } finally {
            history.restoreRoot();
        }
    }

    private record Node(int score, long bestMove, int legalMoves) {}

    private static final class State {
        private static final int MAX_MOVES = 256;
        private static final int NEGATIVE_INFINITY = -TranspositionScores.MATE_SCORE - 1;

        private final QuiescenceSearch quiescence = new QuiescenceSearch();
        private long nodes;

        Node searchNode(
            long[] board, SearchLineHistory history, int depth, int absolutePly
        ) {
            if(depth <= 0) {
                final QuiescenceSearch.Result leaf = quiescence.searchLeaf(
                    board, history, SearchControl.unlimited(), absolutePly,
                    -TranspositionScores.MATE_SCORE - 1,
                    TranspositionScores.MATE_SCORE + 1
                );
                if(!leaf.completed()) throw new AssertionError("Unlimited qsearch aborted.");
                nodes += leaf.nodes();
                final long[] rootMoves = legalMoves(board);
                return new Node(
                    leaf.score(), rootMoves.length == 0 ? 0L : rootMoves[0],
                    rootMoves.length
                );
            }

            final long[] moves = legalMoves(board);
            if(moves.length == 0) {
                final int player = Math.toIntExact(board[Board.STATUS]) & Board.PLAYER_BIT;
                final boolean checked = Board.isPlayerInCheckPext(
                    board[0], board[1], board[2], board[3], player
                );
                return new Node(
                    checked ? -TranspositionScores.MATE_SCORE + absolutePly : 0,
                    0L, 0
                );
            }
            if(DrawAdjudicator.adjudicateNonTerminal(board, history)
                != DrawAdjudicator.RuleDraw.NONE) {
                return new Node(0, moves[0], moves.length);
            }

            int bestScore = NEGATIVE_INFINITY;
            long bestMove = 0L;
            for(long move : moves) {
                final long[] child = new long[Board.MAX_BITBOARDS];
                Board.makeMoveInto(
                    board[0], board[1], board[2], board[3],
                    Math.toIntExact(board[Board.STATUS]), board[Board.KEY], move, child
                );
                nodes ++;
                history.pushRealPosition(child);
                final int score;
                try {
                    score = -searchNode(child, history, depth - 1, absolutePly + 1).score;
                } finally {
                    history.popRealPosition();
                }
                if(score > bestScore) {
                    bestScore = score;
                    bestMove = move;
                }
            }
            return new Node(bestScore, bestMove, moves.length);
        }

        private static long[] legalMoves(long[] board) {
            final long[] moves = new long[MAX_MOVES];
            final int count = Gen.genAll(
                board[0], board[1], board[2], board[3],
                Math.toIntExact(board[Board.STATUS]), board[Board.KEY], true,
                moves, new long[Board.MAX_BITBOARDS]
            );
            return Arrays.copyOf(moves, count);
        }
    }

    private AlphaBetaReference() {}
}
