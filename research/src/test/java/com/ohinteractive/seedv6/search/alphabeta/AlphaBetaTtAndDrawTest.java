package com.ohinteractive.seedv6.search.alphabeta;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.alphabeta.AlphaBetaPvsSearch.Configuration;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.common.SearchResult;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;
import com.ohinteractive.seedv6.search.tt.TranspositionTable;
import com.ohinteractive.seedv6.search.tt.TranspositionTable.Bound;
import com.ohinteractive.seedv6.search.tt.TranspositionTable.Probe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlphaBetaTtAndDrawTest {

    @Test
    void originalWindowClassifiesExactLowerAndUpperRootStores() {
        final long[] board = Board.fromFen(
            "4k3/8/8/8/8/8/4P3/3QK3 w - - 0 1"
        );
        final GameHistory history = GameHistory.initial(board);
        final int depth = 2;
        final int exactScore = search(new TranspositionTable(1 << 10), false)
            .search(new SearchRequest(board, history, depth)).score();

        assertBound(board, history, depth, exactScore - 1, exactScore + 1,
            exactScore, Bound.EXACT);
        assertBound(board, history, depth, exactScore - 1, exactScore,
            exactScore, Bound.LOWER);
        assertBound(board, history, depth, exactScore, exactScore + 1,
            exactScore, Bound.UPPER);
    }

    @Test
    void mateAndStalematePrecedeRuleDrawsAtTheBoundary() {
        final long[] mate = Board.fromFen(
            "7k/6Q1/6K1/8/8/8/8/8 b - - 100 1"
        );
        final GameHistory mateHistory = GameHistory.builder(mate)
            .appendPosition(mate).appendPosition(mate).snapshot();
        final SearchResult mated = search(new TranspositionTable(256), true).search(
            new SearchRequest(mate, mateHistory, 3)
        );
        assertEquals(-TranspositionScores.MATE_SCORE, mated.score());
        assertFalse(mated.hasMove());

        final long[] stalemate = Board.fromFen(
            "7k/5Q2/6K1/8/8/8/8/8 b - - 100 1"
        );
        final GameHistory stalemateHistory = GameHistory.builder(stalemate)
            .appendPosition(stalemate).appendPosition(stalemate).snapshot();
        final SearchResult drawn = search(new TranspositionTable(256), true).search(
            new SearchRequest(stalemate, stalemateHistory, 3)
        );
        assertEquals(0, drawn.score());
        assertFalse(drawn.hasMove());
    }

    @Test
    void repetitionAndOrdinaryContextsSharingRawKeyRemainIsolatedInBothOrders() {
        final long[] board = Board.fromFen(
            "4k3/8/8/8/8/8/8/Q3K3 w - - 2 1"
        );
        final GameHistory repeated = GameHistory.builder(board)
            .appendPosition(board).appendPosition(board).snapshot();
        final GameHistory ordinary = GameHistory.initial(board);
        final int ordinaryOracle = search(new TranspositionTable(1024), false)
            .search(new SearchRequest(board, ordinary, 2)).score();
        assertNotEquals(0, ordinaryOracle);

        final AlphaBetaPvsSearch drawnFirst = search(new TranspositionTable(1024), true);
        assertEquals(0, drawnFirst.search(new SearchRequest(board, repeated, 2)).score());
        assertEquals(ordinaryOracle,
            drawnFirst.search(new SearchRequest(board, ordinary, 2)).score());

        final AlphaBetaPvsSearch ordinaryFirst = search(new TranspositionTable(1024), true);
        assertEquals(ordinaryOracle,
            ordinaryFirst.search(new SearchRequest(board, ordinary, 2)).score());
        assertEquals(0,
            ordinaryFirst.search(new SearchRequest(board, repeated, 2)).score());
    }

    @Test
    void halfmoveZeroNinetyNineAndHundredContextsDoNotCrossContaminate() {
        final long[] zero = Board.fromFen(
            "4k3/8/8/8/8/8/8/Q3K3 w - - 0 1"
        );
        final long[] ninetyNine = Board.fromFen(
            "4k3/8/8/8/8/8/8/Q3K3 w - - 99 1"
        );
        final long[] hundred = Board.fromFen(
            "4k3/8/8/8/8/8/8/Q3K3 w - - 100 1"
        );
        assertEquals(zero[Board.KEY], ninetyNine[Board.KEY]);
        assertEquals(zero[Board.KEY], hundred[Board.KEY]);

        final int zeroOracle = exactWithoutTt(zero, 2);
        final int ninetyNineOracle = exactWithoutTt(ninetyNine, 2);
        assertEquals(0, exactWithoutTt(hundred, 2));

        final AlphaBetaPvsSearch ascending = search(new TranspositionTable(2048), true);
        assertEquals(zeroOracle, ascending.search(new SearchRequest(zero, 2)).score());
        assertEquals(ninetyNineOracle,
            ascending.search(new SearchRequest(ninetyNine, 2)).score());
        assertEquals(0, ascending.search(new SearchRequest(hundred, 2)).score());

        final AlphaBetaPvsSearch descending = search(new TranspositionTable(2048), true);
        assertEquals(0, descending.search(new SearchRequest(hundred, 2)).score());
        assertEquals(ninetyNineOracle,
            descending.search(new SearchRequest(ninetyNine, 2)).score());
        assertEquals(zeroOracle, descending.search(new SearchRequest(zero, 2)).score());
    }

    @Test
    void mateDistanceAndWarmTtRemainStable() {
        final long[] mateInOne = Board.fromFen(
            "7k/6p1/5KQ1/8/8/8/8/8 w - - 0 1"
        );
        final AlphaBetaPvsSearch search = search(new TranspositionTable(4096), true);
        final SearchResult cold = search.search(new SearchRequest(mateInOne, 2));
        final SearchResult warm = search.search(new SearchRequest(mateInOne, 2));
        assertEquals(TranspositionScores.MATE_SCORE - 1, cold.score());
        assertEquals(cold.score(), warm.score());
        assertEquals(cold.bestMove(), warm.bestMove());
        AlphaBetaPvsSearchTest.assertLegalPv(mateInOne, cold);
        AlphaBetaPvsSearchTest.assertLegalPv(mateInOne, warm);
    }

    private static void assertBound(
        long[] board, GameHistory history, int depth, int alpha, int beta,
        int expectedScore, Bound expectedBound
    ) {
        final TranspositionTable table = new TranspositionTable(1 << 10);
        final AlphaBetaPvsSearch search = search(table, true);
        final SearchResult result = search.search(
            new SearchRequest(board, history, depth), alpha, beta
        );
        assertEquals(expectedScore, result.score());
        assertTrue(result.hasMove());

        final Probe probe = new Probe();
        table.probe(
            board[Board.KEY], depth, -TranspositionScores.MATE_SCORE - 1,
            TranspositionScores.MATE_SCORE + 1, 0, probe
        );
        assertTrue(probe.keyMatches());
        assertEquals(depth, probe.depth());
        assertEquals(expectedBound, probe.bound());
        assertEquals(expectedScore, probe.score());
        assertEquals(result.bestMove(), probe.move());
    }

    private static int exactWithoutTt(long[] board, int depth) {
        return search(new TranspositionTable(1024), false)
            .search(new SearchRequest(board, depth)).score();
    }

    private static AlphaBetaPvsSearch search(
        TranspositionTable table, boolean tt
    ) {
        return new AlphaBetaPvsSearch(table, new Configuration(true, tt, true));
    }
}
