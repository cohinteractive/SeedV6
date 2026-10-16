package com.ohinteractive.seedv6.search.tt;

import java.util.Arrays;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.rules.DrawAdjudicator;
import com.ohinteractive.seedv6.rules.DrawAdjudicator.RuleDraw;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.rules.SearchLineHistory;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.tt.TranspositionTable.Bound;
import com.ohinteractive.seedv6.search.tt.TranspositionTable.Cacheability;
import com.ohinteractive.seedv6.search.tt.TranspositionTable.Probe;
import com.ohinteractive.seedv6.search.tt.TranspositionTable.ProbeOutcome;
import com.ohinteractive.seedv6.search.tt.TranspositionTable.StoreOutcome;

/** Deliberately simple allocating full-width test search using production TT APIs. */
final class TtExactSearchHarness {

    record Result(
        int score,
        long bestMove,
        boolean hasMove,
        int legalRootMoves,
        boolean pathDependent,
        long nodes,
        long probes,
        long keyMatches,
        long scoreHits,
        long stores,
        long uncacheableStores
    ) {}

    static Result search(SearchRequest request, TranspositionTable table) {
        final long[] board = new long[Board.MAX_BITBOARDS];
        request.copyBoardInto(board);
        return search(board, request.gameHistory(), request.depth(), table);
    }

    static Result search(long[] board, GameHistory gameHistory, int depth, TranspositionTable table) {
        if(depth < 1) throw new IllegalArgumentException("Depth must be at least 1.");
        final SearchLineHistory history = new SearchLineHistory(gameHistory);
        final Stats stats = new Stats();
        try {
            final NodeResult node = searchNode(
                Arrays.copyOf(board, Board.MAX_BITBOARDS), depth, 0, history, table, stats
            );
            return new Result(
                node.score, node.bestMove, node.legalMoves != 0, node.legalMoves,
                node.cacheability == Cacheability.PATH_DEPENDENT,
                stats.nodes, stats.probes, stats.keyMatches, stats.scoreHits,
                stats.stores, stats.uncacheableStores
            );
        } finally {
            history.restoreRoot();
        }
    }

    static int scoreAfterRootMove(
        long[] board, GameHistory gameHistory, int depth, long move
    ) {
        if(depth < 1) throw new IllegalArgumentException("Depth must be at least 1.");
        final long[] child = new long[Board.MAX_BITBOARDS];
        Board.makeMoveInto(
            board[0], board[1], board[2], board[3],
            (int) board[Board.STATUS], board[Board.KEY], move, child
        );
        final SearchLineHistory history = new SearchLineHistory(gameHistory);
        history.pushRealPosition(child);
        try {
            return -searchNode(child, depth - 1, 1, history, null, new Stats()).score;
        } finally {
            history.restoreRoot();
        }
    }

    private static final int MAX_MOVES = 256;
    private static final int NEG_INF = -1_000_000_000;

    private static final class NodeResult {
        final int score;
        final long bestMove;
        final int legalMoves;
        final Cacheability cacheability;

        NodeResult(int score, long bestMove, int legalMoves, Cacheability cacheability) {
            this.score = score;
            this.bestMove = bestMove;
            this.legalMoves = legalMoves;
            this.cacheability = cacheability;
        }
    }

    private static final class Stats {
        long nodes;
        long probes;
        long keyMatches;
        long scoreHits;
        long stores;
        long uncacheableStores;
    }

    private static NodeResult searchNode(
        long[] board, int depth, int ply, SearchLineHistory history,
        TranspositionTable table, Stats stats
    ) {
        stats.nodes ++;
        final long[] moves = new long[MAX_MOVES];
        final int moveCount = Gen.genAll(
            board[0], board[1], board[2], board[3],
            (int) board[Board.STATUS], board[Board.KEY], true,
            moves, new long[Board.MAX_BITBOARDS]
        );
        if(moveCount == 0) {
            final int player = (int) board[Board.STATUS] & Board.PLAYER_BIT;
            final int score = Board.isPlayerInCheckPext(
                board[0], board[1], board[2], board[3], player
            ) ? -TranspositionScores.MATE_SCORE + ply : 0;
            store(table, board[Board.KEY], depth, score, ply, 0L, Cacheability.POSITION_ONLY, stats);
            return new NodeResult(score, 0L, 0, Cacheability.POSITION_ONLY);
        }

        final RuleDraw draw = DrawAdjudicator.adjudicateNonTerminal(board, history);
        if(draw != RuleDraw.NONE) {
            store(table, board[Board.KEY], depth, 0, ply, moves[0], Cacheability.PATH_DEPENDENT, stats);
            return new NodeResult(0, moves[0], moveCount, Cacheability.PATH_DEPENDENT);
        }

        if(table != null) {
            stats.probes ++;
            final Probe probe = new Probe();
            final ProbeOutcome outcome = table.probe(
                board[Board.KEY], depth, -TranspositionScores.MATE_SCORE - 1,
                TranspositionScores.MATE_SCORE + 1, ply, probe
            );
            if(probe.keyMatches()) stats.keyMatches ++;
            if(outcome == ProbeOutcome.EXACT_HIT
                && (depth == 0 || contains(moves, moveCount, probe.move()))) {
                stats.scoreHits ++;
                return new NodeResult(
                    probe.score(), probe.move(), moveCount, Cacheability.POSITION_ONLY
                );
            }
        }

        if(depth == 0) {
            final int score = Eval.eval(
                board[0], board[1], board[2], board[3],
                (int) board[Board.STATUS], board[Board.KEY]
            );
            store(table, board[Board.KEY], depth, score, ply, 0L, Cacheability.POSITION_ONLY, stats);
            return new NodeResult(score, 0L, moveCount, Cacheability.POSITION_ONLY);
        }

        int bestScore = NEG_INF;
        long bestMove = 0L;
        Cacheability cacheability = Cacheability.POSITION_ONLY;
        for(int i = 0; i < moveCount; i ++) {
            final long[] child = new long[Board.MAX_BITBOARDS];
            Board.makeMoveInto(
                board[0], board[1], board[2], board[3],
                (int) board[Board.STATUS], board[Board.KEY], moves[i], child
            );
            history.pushRealPosition(child);
            final NodeResult childResult;
            try {
                childResult = searchNode(child, depth - 1, ply + 1, history, table, stats);
            } finally {
                history.popRealPosition();
            }
            final int score = -childResult.score;
            if(score > bestScore) {
                bestScore = score;
                bestMove = moves[i];
            }
            if(childResult.cacheability == Cacheability.PATH_DEPENDENT) {
                cacheability = Cacheability.PATH_DEPENDENT;
            }
        }
        store(table, board[Board.KEY], depth, bestScore, ply, bestMove, cacheability, stats);
        return new NodeResult(bestScore, bestMove, moveCount, cacheability);
    }

    private static void store(
        TranspositionTable table, long key, int depth, int score, int ply,
        long move, Cacheability cacheability, Stats stats
    ) {
        if(table == null) return;
        final StoreOutcome outcome = table.store(
            key, depth, Bound.EXACT, score, ply, move, cacheability
        );
        if(outcome == StoreOutcome.STORED) stats.stores ++;
        if(outcome == StoreOutcome.NOT_CACHEABLE) stats.uncacheableStores ++;
    }

    private static boolean contains(long[] moves, int moveCount, long candidate) {
        if(candidate == 0L) return false;
        for(int i = 0; i < moveCount; i ++) {
            if(moves[i] == candidate) return true;
        }
        return false;
    }

    private TtExactSearchHarness() {}
}
