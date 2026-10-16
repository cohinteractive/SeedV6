package com.ohinteractive.seedv6.search.alphabeta;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.alphabeta.AlphaBetaPvsSearch.Configuration;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.common.SearchResult;
import com.ohinteractive.seedv6.search.tt.TranspositionTable;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlphaBetaPvsSearchTest {

    private static final List<String> EXACT_FENS = List.of(
        Board.FEN_STARTING_POSITION,
        "4k3/8/8/8/8/8/4P3/3QK3 w - - 0 1",
        "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1",
        "7k/P7/8/8/8/8/8/7K w - - 0 1",
        "7k/6p1/5KQ1/8/8/8/8/8 w - - 0 1",
        "7k/5Q2/6K1/8/8/8/8/8 b - - 0 1"
    );

    @Test
    void depthsZeroOneTwoAndThreeMeanRemainingMainPliesAtRoot() {
        for(String fen : EXACT_FENS) {
            final long[] board = Board.fromFen(fen);
            final long[] before = board.clone();
            final GameHistory history = GameHistory.initial(board);
            for(int depth = 0; depth <= 3; depth ++) {
                final AlphaBetaReference.Result expected =
                    AlphaBetaReference.search(board, history, depth);
                final AlphaBetaPvsSearch production = search(false, false, true);
                final SearchResult actual = production.search(
                    new SearchRequest(board, history, depth)
                );
                assertTrue(actual.completed(), fen + " depth=" + depth);
                assertEquals(expected.score(), actual.score(), fen + " depth=" + depth);
                assertEquals(expected.legalRootMoves(), actual.legalRootMoves());
                assertEquals(depth, actual.depth());
                assertLegalPv(board, actual);
                assertArrayEquals(before, board);
            }
        }
    }

    @Test
    void pvsAndPlainAlphaBetaReturnTheSameExactScoreAndLegalMove() {
        long reSearches = 0L;
        long nullWindows = 0L;
        long rootResearches = 0L;
        long interiorResearches = 0L;
        long failLows = 0L;
        long failHighs = 0L;
        long rootNullWindows = 0L;
        long interiorNullWindows = 0L;
        long firstMoveFullWindows = 0L;
        boolean rootFailHighExercised = false;
        for(String fen : EXACT_FENS) {
            final long[] board = Board.fromFen(fen);
            final GameHistory history = GameHistory.initial(board);
            final AlphaBetaPvsSearch plain = search(false, false, true);
            final AlphaBetaPvsSearch pvs = search(true, false, true);
            final SearchResult expected = plain.search(new SearchRequest(board, history, 3));
            final SearchResult actual = pvs.search(new SearchRequest(board, history, 3));
            assertEquals(expected.score(), actual.score(), fen);
            assertEquals(expected.bestMove(), actual.bestMove(), fen);
            assertEquals(expected.hasMove(), actual.hasMove(), fen);
            assertLegalPv(board, expected);
            assertLegalPv(board, actual);
            reSearches += pvs.statistics().pvsResearches();
            nullWindows += pvs.statistics().pvsNullWindowSearches();
            rootResearches += pvs.statistics().pvsRootResearches();
            interiorResearches += pvs.statistics().pvsInteriorResearches();
            failLows += pvs.statistics().pvsFailLows();
            failHighs += pvs.statistics().pvsFailHighs();
            rootNullWindows += pvs.statistics().pvsRootNullWindows();
            interiorNullWindows += pvs.statistics().pvsInteriorNullWindows();
            firstMoveFullWindows += pvs.statistics().firstMoveFullWindowSearches();

            if(pvs.statistics().pvsRootResearches() > 0L) {
                final AlphaBetaPvsSearch rootFailHigh = search(true, false, true);
                final SearchResult bounded = rootFailHigh.search(
                    new SearchRequest(board, history, 3),
                    actual.score() - 1, actual.score()
                );
                assertEquals(actual.score(), bounded.score());
                rootFailHighExercised |=
                    rootFailHigh.statistics().pvsRootFailHighs() > 0L;
            }
        }
        assertTrue(nullWindows > 0L);
        assertTrue(reSearches > 0L, "A later alpha-improving move must exercise full re-search.");
        assertTrue(rootNullWindows > 0L);
        assertTrue(interiorNullWindows > 0L);
        assertTrue(rootResearches > 0L);
        assertTrue(interiorResearches > 0L);
        assertTrue(failLows > 0L);
        assertTrue(failHighs > 0L);
        assertTrue(firstMoveFullWindows > 0L);
        assertTrue(rootFailHighExercised);
    }

    @Test
    void ttDisabledColdAndWarmRemainExactAndWarmPvMayHonestlyTruncate() {
        final long[] board = Board.startingPosition();
        final GameHistory history = GameHistory.initial(board);
        final SearchResult disabled = search(true, false, true).search(
            new SearchRequest(board, history, 3)
        );
        final TranspositionTable table = new TranspositionTable(1 << 14);
        final AlphaBetaPvsSearch enabled = new AlphaBetaPvsSearch(
            table, new Configuration(true, true, true)
        );
        final SearchResult cold = enabled.search(new SearchRequest(board, history, 3));
        final SearchResult warm = enabled.search(new SearchRequest(board, history, 3));

        assertEquals(disabled.score(), cold.score());
        assertEquals(disabled.score(), warm.score());
        assertEquals(cold.bestMove(), warm.bestMove());
        assertTrue(enabled.statistics().ttCutoffs() > 0L);
        assertLegalPv(board, cold);
        assertLegalPv(board, warm);
        assertTrue(warm.principalVariationLength() < cold.principalVariationLength());
    }

    @Test
    void orderingEnabledAndNeutralAuthoritativeOrderRemainScoreExact() {
        for(String fen : EXACT_FENS) {
            final long[] board = Board.fromFen(fen);
            final GameHistory history = GameHistory.initial(board);
            final SearchResult neutral = search(true, false, false).search(
                new SearchRequest(board, history, 2)
            );
            final SearchResult ordered = search(true, false, true).search(
                new SearchRequest(board, history, 2)
            );
            assertEquals(neutral.score(), ordered.score(), fen);
            assertLegalPv(board, neutral);
            assertLegalPv(board, ordered);
        }
    }

    @Test
    void exportedPvDoesNotAliasReusableWorkerStorage() {
        final AlphaBetaPvsSearch search = search(true, true, true);
        final long[] firstBoard = Board.startingPosition();
        final SearchResult first = search.search(new SearchRequest(firstBoard, 3));
        final long[] retained = first.principalVariation();

        final long[] secondBoard = Board.fromFen(
            "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1"
        );
        search.search(new SearchRequest(secondBoard, 2));
        assertArrayEquals(retained, first.principalVariation());

        final long[] callerCopy = first.principalVariation();
        if(callerCopy.length != 0) callerCopy[0] = 0L;
        assertArrayEquals(retained, first.principalVariation());
    }

    static void assertLegalPv(long[] root, SearchResult result) {
        long[] board = root.clone();
        final long[] variation = result.principalVariation();
        for(long move : variation) {
            final long[] moves = new long[256];
            final int count = Gen.genAll(
                board[0], board[1], board[2], board[3],
                Math.toIntExact(board[Board.STATUS]), board[Board.KEY], true,
                moves, new long[Board.MAX_BITBOARDS]
            );
            assertTrue(contains(moves, count, move),
                "Illegal PV move " + Long.toUnsignedString(move));
            final long[] child = new long[Board.MAX_BITBOARDS];
            Board.makeMoveInto(
                board[0], board[1], board[2], board[3],
                Math.toIntExact(board[Board.STATUS]), board[Board.KEY], move, child
            );
            board = child;
        }
        if(variation.length != 0) {
            assertEquals(result.bestMove(), variation[0]);
        }
    }

    private static boolean contains(long[] moves, int count, long move) {
        for(int index = 0; index < count; index ++) {
            if(moves[index] == move) return true;
        }
        return false;
    }

    private static AlphaBetaPvsSearch search(
        boolean pvs, boolean tt, boolean ordering
    ) {
        return new AlphaBetaPvsSearch(
            new TranspositionTable(1 << 12), new Configuration(pvs, tt, ordering)
        );
    }
}
