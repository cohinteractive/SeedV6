package com.ohinteractive.seedv6.uci;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.alphabeta.AlphaBetaPvsSearch;
import com.ohinteractive.seedv6.search.common.IterationSnapshot;
import com.ohinteractive.seedv6.search.common.SearchControl;
import com.ohinteractive.seedv6.search.common.SearchObserver;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.common.SearchResult;
import com.ohinteractive.seedv6.search.common.TimeSource;
import com.ohinteractive.seedv6.search.iterative.IterativeDeepeningSearch;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UciReportingTest {

    @Test
    void convertsCentipawnsAndBothMateSignsWithExactUciMoveDistance() {
        assertEquals("cp 0", UciScore.fromInternal(0).fields());
        assertEquals("cp 123", UciScore.fromInternal(123).fields());
        assertEquals("cp -456", UciScore.fromInternal(-456).fields());
        assertEquals("mate 1", UciScore.fromInternal(
            TranspositionScores.MATE_SCORE - 1
        ).fields());
        assertEquals("mate 2", UciScore.fromInternal(
            TranspositionScores.MATE_SCORE - 3
        ).fields());
        assertEquals("mate 0", UciScore.fromInternal(
            -TranspositionScores.MATE_SCORE
        ).fields());
        assertEquals("mate -1", UciScore.fromInternal(
            -TranspositionScores.MATE_SCORE + 2
        ).fields());
        assertEquals("mate -2", UciScore.fromInternal(
            -TranspositionScores.MATE_SCORE + 4
        ).fields());
    }

    @Test
    void knownWinningAndLosingMatePositionsReportExactDistanceFromSideToMove() {
        assertKnownMate("7k/8/5KQ1/8/8/8/8/8 w - - 0 1", 1, "mate 1");
        assertKnownMate("7k/p7/5KQ1/8/8/8/8/8 b - - 0 1", 2, "mate -1");
    }

    @Test
    void infoFormattingOmitsUnavailableNpsAndNeverFormatsMateAsCp() {
        final SearchResult zeroTime = new SearchResult(
            0L, false, TranspositionScores.MATE_SCORE - 1, 3,
            17L, 0, true
        );
        final IterationSnapshot noNps = IterationSnapshot.from(zeroTime, 999_999L);
        final String first = UciInfoFormatter.format(noNps);
        assertEquals("info depth 3 score mate 1 nodes 17 time 0", first);
        assertFalse(first.contains(" nps "));
        assertFalse(first.contains("score cp"));

        final SearchResult ordinary = new SearchResult(0L, false, -25, 2, 50L, 0, true);
        final String second = UciInfoFormatter.format(
            IterationSnapshot.from(ordinary, 2_000_000L)
        );
        assertEquals("info depth 2 score cp -25 nodes 50 time 2 nps 25000", second);
    }

    private static void assertKnownMate(String fen, int depth, String expected) {
        final long[] board = Board.fromFen(fen);
        final SearchControl control = SearchControl.controlled(
            -1L, System.nanoTime(), -1L, TimeSource.SYSTEM
        );
        final SearchResult result = new IterativeDeepeningSearch(
            new AlphaBetaPvsSearch()
        ).search(new SearchRequest(
            board, GameHistory.initial(board), depth, SearchObserver.NONE, control
        )).lastCompletedResult();
        assertEquals(expected, UciScore.fromInternal(result.score()).fields());
        assertTrue(result.hasMove());
    }
}
