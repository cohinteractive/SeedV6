package com.ohinteractive.seedv6.search.alphabeta;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.alphabeta.AlphaBetaPvsSearch.Configuration;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.common.SearchResult;
import com.ohinteractive.seedv6.search.order.MoveOrdering;
import com.ohinteractive.seedv6.search.tt.TranspositionTable;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlphaBetaCorpusTest {

    static final long CORPUS_SEED = 0x5eed_0010_cafeL;
    static final int RANDOM_POSITIONS = 48;
    static final int SPECIAL_POSITIONS = 10;
    static final int EXACT_CALLS = RANDOM_POSITIONS + SPECIAL_POSITIONS;

    @Test
    void fixedSeedLegalShallowCorpusAgreesExactlyWithAllocatingReference() {
        final Random random = new Random(CORPUS_SEED);
        final AlphaBetaPvsSearch production = new AlphaBetaPvsSearch(
            new TranspositionTable(1 << 12), new Configuration(true, false, true)
        );
        final AlphaBetaPvsSearch selective = new AlphaBetaPvsSearch(
            new TranspositionTable(1 << 12),
            new Configuration(true, false, true, SelectiveSearchPolicy.production())
        );
        int positions = 0;
        int checks = 0;
        int tacticals = 0;

        long[] board = Board.startingPosition();
        GameHistory.Builder history = GameHistory.builder(board);
        int gamePly = 0;
        while(positions < RANDOM_POSITIONS) {
            final GameHistory snapshot = history.snapshot();
            final int depth = positions % 3;
            compare(production, selective, board, snapshot, depth, "random-" + positions);
            final long[] moves = legalMoves(board);
            if(isChecked(board)) checks ++;
            if(hasTactical(board, moves)) tacticals ++;
            positions ++;

            if(moves.length == 0 || gamePly >= 31) {
                board = Board.startingPosition();
                history = GameHistory.builder(board);
                gamePly = 0;
                continue;
            }
            final long selected = selectBiased(board, moves, random);
            board = apply(board, selected);
            history.appendPosition(board);
            gamePly ++;
        }

        final List<String> specials = List.of(
            "4r1k1/8/8/8/8/8/8/4K3 w - - 0 1",
            "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1",
            "4k3/8/8/3pP3/3r4/8/8/4K3 w - d6 0 1",
            "7k/P7/8/8/8/8/8/7K w - - 0 1",
            "k6r/6P1/8/8/8/8/8/7K w - - 0 1",
            "7k/6p1/5KQ1/8/8/8/8/8 w - - 0 1",
            "7k/6Q1/6K1/8/8/8/8/8 b - - 0 1",
            "7k/5Q2/6K1/8/8/8/8/8 b - - 0 1",
            "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1",
            "4k3/8/8/8/8/8/4P3/3QK3 w - - 99 1"
        );
        assertEquals(SPECIAL_POSITIONS, specials.size());
        for(int index = 0; index < specials.size(); index ++) {
            final long[] special = Board.fromFen(specials.get(index));
            compare(
                production, selective, special, GameHistory.initial(special), index % 3,
                "special-" + index
            );
            final long[] moves = legalMoves(special);
            if(isChecked(special)) checks ++;
            if(hasTactical(special, moves)) tacticals ++;
            positions ++;
        }

        assertEquals(EXACT_CALLS, positions);
        assertTrue(checks >= 2, "checks=" + checks);
        assertTrue(tacticals >= 8, "tacticals=" + tacticals);
    }

    private static void compare(
        AlphaBetaPvsSearch production, AlphaBetaPvsSearch selective,
        long[] board, GameHistory history,
        int depth, String label
    ) {
        final long[] before = board.clone();
        final AlphaBetaReference.Result expected =
            AlphaBetaReference.search(board, history, depth);
        final SearchResult actual = production.search(
            new SearchRequest(board, history, depth)
        );
        final SearchResult selectiveResult = selective.search(
            new SearchRequest(board, history, depth)
        );
        assertTrue(actual.completed(), label);
        assertEquals(expected.score(), actual.score(), label + " depth=" + depth);
        assertEquals(expected.legalRootMoves(), actual.legalRootMoves(), label);
        assertEquals(expected.score(), selectiveResult.score(), label + " selective");
        assertEquals(
            expected.legalRootMoves(), selectiveResult.legalRootMoves(), label + " selective"
        );
        AlphaBetaPvsSearchTest.assertLegalPv(board, actual);
        AlphaBetaPvsSearchTest.assertLegalPv(board, selectiveResult);
        assertArrayEquals(before, board, label);
        history.requireCurrent(board);
    }

    private static long selectBiased(
        long[] board, long[] moves, Random random
    ) {
        final long[] tactical = new long[moves.length];
        int count = 0;
        for(long move : moves) {
            if(MoveOrdering.isTactical(board, move)) tactical[count ++] = move;
        }
        if(count != 0 && random.nextBoolean()) return tactical[random.nextInt(count)];
        return moves[random.nextInt(moves.length)];
    }

    private static boolean hasTactical(long[] board, long[] moves) {
        for(long move : moves) {
            if(MoveOrdering.isTactical(board, move)) return true;
        }
        return false;
    }

    private static boolean isChecked(long[] board) {
        final int player = Math.toIntExact(board[Board.STATUS]) & Board.PLAYER_BIT;
        return Board.isPlayerInCheckPext(
            board[0], board[1], board[2], board[3], player
        );
    }

    private static long[] legalMoves(long[] board) {
        final long[] moves = new long[256];
        final int count = Gen.genAll(
            board[0], board[1], board[2], board[3],
            Math.toIntExact(board[Board.STATUS]), board[Board.KEY], true,
            moves, new long[Board.MAX_BITBOARDS]
        );
        return Arrays.copyOf(moves, count);
    }

    private static long[] apply(long[] board, long move) {
        final long[] child = new long[Board.MAX_BITBOARDS];
        Board.makeMoveInto(
            board[0], board[1], board[2], board[3],
            Math.toIntExact(board[Board.STATUS]), board[Board.KEY], move, child
        );
        return child;
    }
}
