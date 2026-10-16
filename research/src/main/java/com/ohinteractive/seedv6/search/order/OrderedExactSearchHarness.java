package com.ohinteractive.seedv6.search.order;

import java.util.Arrays;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.rules.DrawAdjudicator;
import com.ohinteractive.seedv6.rules.DrawAdjudicator.RuleDraw;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.rules.SearchLineHistory;

/** Test-only full-width exact traversal with a baseline/WS8 ordering switch. */
final class OrderedExactSearchHarness {

    record Result(
        int score,
        long bestMove,
        boolean hasMove,
        int legalRootMoves,
        long edges,
        long checkmates,
        long stalemates,
        long ruleDraws,
        long frontiers,
        long semanticChecksum,
        long orderFingerprint
    ) {}

    static Result search(long[] board, int depth, boolean ordered) {
        if(depth < 1) throw new IllegalArgumentException("Depth must be at least 1.");
        final GameHistory gameHistory = GameHistory.initial(board);
        final SearchLineHistory lineHistory = new SearchLineHistory(gameHistory);
        final MoveOrdering ordering = new MoveOrdering(depth + 1);
        final Stats stats = new Stats();
        try {
            final NodeResult node = searchNode(
                Arrays.copyOf(board, Board.MAX_BITBOARDS), depth, 0,
                lineHistory, ordering, ordered, stats
            );
            return new Result(
                node.score, node.bestMove, node.legalMoves != 0, node.legalMoves,
                stats.edges, stats.checkmates, stats.stalemates, stats.ruleDraws,
                stats.frontiers, stats.semanticChecksum, stats.orderFingerprint
            );
        } finally {
            lineHistory.restoreRoot();
        }
    }

    static int rootMoveScore(long[] board, int depth, long move) {
        if(depth < 1) throw new IllegalArgumentException("Depth must be at least 1.");
        final SearchLineHistory lineHistory = new SearchLineHistory(GameHistory.initial(board));
        final long[] child = apply(board, move);
        lineHistory.pushRealPosition(child);
        try {
            return -searchNode(
                child, depth - 1, 1, lineHistory,
                new MoveOrdering(depth + 1), false, new Stats()
            ).score;
        } finally {
            lineHistory.popRealPosition();
            lineHistory.restoreRoot();
        }
    }

    private static final int MATE_SCORE = 32_768;
    private static final int NEG_INF = -1_000_000_000;

    private static final class Stats {
        long edges;
        long checkmates;
        long stalemates;
        long ruleDraws;
        long frontiers;
        long semanticChecksum;
        long orderFingerprint = 0xcbf2_9ce4_8422_2325L;
    }

    private record NodeResult(int score, long bestMove, int legalMoves) {}

    private static NodeResult searchNode(
        long[] board, int depth, int ply, SearchLineHistory lineHistory,
        MoveOrdering ordering, boolean ordered, Stats stats
    ) {
        final long[] generated = new long[StagedMovePicker.MAX_MOVES];
        final long[] scratch = new long[Board.MAX_BITBOARDS];
        final int legalCount = Gen.genAll(
            board[0], board[1], board[2], board[3],
            Math.toIntExact(board[Board.STATUS]), board[Board.KEY], true,
            generated, scratch
        );
        if(legalCount == 0) {
            final int player = Math.toIntExact(board[Board.STATUS]) & Board.PLAYER_BIT;
            if(Board.isPlayerInCheckPext(board[0], board[1], board[2], board[3], player)) {
                stats.checkmates ++;
                return new NodeResult(-MATE_SCORE + ply, StagedMovePicker.NO_MOVE, 0);
            }
            stats.stalemates ++;
            return new NodeResult(0, StagedMovePicker.NO_MOVE, 0);
        }
        if(DrawAdjudicator.adjudicateNonTerminal(board, lineHistory) != RuleDraw.NONE) {
            stats.ruleDraws ++;
            return new NodeResult(0, generated[0], legalCount);
        }
        if(depth == 0) {
            stats.frontiers ++;
            return new NodeResult(
                Eval.eval(
                    board[0], board[1], board[2], board[3],
                    Math.toIntExact(board[Board.STATUS]), board[Board.KEY]
                ),
                generated[0], legalCount
            );
        }

        final StagedMovePicker picker = ordering.picker();
        if(ordered) picker.prepare(board, ply, StagedMovePicker.NO_MOVE);
        int bestScore = NEG_INF;
        long bestMove = StagedMovePicker.NO_MOVE;
        for(int index = 0; index < legalCount; index ++) {
            final long move = ordered ? picker.next(ply) : generated[index];
            if(move == StagedMovePicker.NO_MOVE) {
                throw new AssertionError("Picker exhausted before authoritative legal count.");
            }
            recordEdge(stats, board[Board.KEY], move, ply);
            final long[] child = apply(board, move);
            lineHistory.pushRealPosition(child);
            final int score;
            try {
                score = -searchNode(
                    child, depth - 1, ply + 1, lineHistory,
                    ordering, ordered, stats
                ).score;
            } finally {
                lineHistory.popRealPosition();
            }
            if(score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }
        if(ordered && picker.next(ply) != StagedMovePicker.NO_MOVE) {
            throw new AssertionError("Picker yielded more than authoritative legal count.");
        }
        return new NodeResult(bestScore, bestMove, legalCount);
    }

    private static void recordEdge(Stats stats, long key, long move, int ply) {
        final long identity = mix(key ^ Long.rotateLeft(move, ply & 63) ^ ply);
        stats.edges ++;
        stats.semanticChecksum += identity;
        stats.orderFingerprint = Long.rotateLeft(stats.orderFingerprint, 7) ^ identity;
    }

    private static long mix(long value) {
        value ^= value >>> 30;
        value *= 0xbf58_476d_1ce4_e5b9L;
        value ^= value >>> 27;
        value *= 0x94d0_49bb_1331_11ebL;
        return value ^ value >>> 31;
    }

    private static long[] apply(long[] board, long move) {
        final long[] child = new long[Board.MAX_BITBOARDS];
        Board.makeMoveInto(
            board[0], board[1], board[2], board[3],
            Math.toIntExact(board[Board.STATUS]), board[Board.KEY], move, child
        );
        return child;
    }

    private OrderedExactSearchHarness() {}
}
