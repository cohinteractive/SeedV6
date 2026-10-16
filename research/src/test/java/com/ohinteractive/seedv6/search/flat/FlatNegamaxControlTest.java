package com.ohinteractive.seedv6.search.flat;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.SearchControl;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.common.SearchResult;
import com.ohinteractive.seedv6.search.common.SearchObserver;
import com.ohinteractive.seedv6.search.common.SearchTermination;
import com.ohinteractive.seedv6.search.common.TimeSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlatNegamaxControlTest {

    @Test
    void nodeLimitBoundariesUseEstablishedEnteredChildDefinition() {
        assertLimited(0L, 1, 0L, false);
        assertLimited(1L, 1, 1L, false);
        assertLimited(19L, 1, 19L, false);
        assertLimited(20L, 1, 20L, true);
        assertLimited(21L, 1, 20L, true);

        assertLimited(419L, 2, 419L, false);
        assertLimited(420L, 2, 420L, true);
        assertLimited(421L, 2, 420L, true);
    }

    @Test
    void preCancelledTraversalReturnsIncompleteFallbackEligibleResult() {
        final long[] board = Board.startingPosition();
        final SearchControl control = controlled(-1L);
        control.request(SearchTermination.STOPPED);

        final SearchResult result = new FlatNegamax().search(
            new SearchRequest(board, GameHistory.initial(board), 4, control)
        );

        assertFalse(result.completed());
        assertFalse(result.hasMove());
        assertEquals(0L, result.nodes());
        assertEquals(20, result.legalRootMoves());
        assertEquals(SearchTermination.STOPPED, control.termination());
    }

    @Test
    void interruptedInstanceIsReusableWithCleanHistoryAndFrames() {
        final long[] board = Board.startingPosition();
        final FlatNegamax search = new FlatNegamax();
        final SearchControl limited = controlled(19L);

        final SearchResult interrupted = search.search(
            new SearchRequest(board, GameHistory.initial(board), 2, limited)
        );
        final SearchResult recovered = search.search(new SearchRequest(board, 2));
        final SearchResult fresh = new FlatNegamax().search(new SearchRequest(board, 2));

        assertFalse(interrupted.completed());
        assertEquals(19L, interrupted.nodes());
        assertTrue(recovered.completed());
        assertEquals(fresh, recovered);
    }

    @Test
    void explicitCancellationWinsWhenCombinedWithAnUnreachedNodeLimit() {
        final long[] board = Board.startingPosition();
        final SearchControl control = controlled(19L);
        final SearchObserver stopper = new SearchObserver() {
            @Override
            public void onRootMoveStarted(int index, int total, long move) {
                control.request(SearchTermination.STOPPED);
            }
        };

        final SearchResult result = new FlatNegamax().search(
            new SearchRequest(board, GameHistory.initial(board), 2, stopper, control)
        );

        assertFalse(result.completed());
        assertEquals(0L, result.nodes());
        assertEquals(SearchTermination.STOPPED, control.termination());
    }

    @Test
    void terminalRootCompletesEvenWithExhaustedControl() {
        final long[] mate = Board.fromFen("7k/6Q1/5K2/8/8/8/8/8 b - - 0 1");
        final SearchControl control = controlled(0L);

        final SearchResult result = new FlatNegamax().search(
            new SearchRequest(mate, GameHistory.initial(mate), 4, control)
        );

        assertTrue(result.completed());
        assertFalse(result.hasMove());
        assertEquals(-32768, result.score());
        assertEquals(0L, result.nodes());
        assertEquals(SearchTermination.NONE, control.termination());
    }

    private static void assertLimited(long budget, int depth, long expectedNodes, boolean completed) {
        final long[] board = Board.startingPosition();
        final SearchControl control = controlled(budget);
        final SearchResult result = new FlatNegamax().search(
            new SearchRequest(board, GameHistory.initial(board), depth, control)
        );

        assertEquals(expectedNodes, result.nodes());
        assertEquals(expectedNodes, control.nodes());
        assertEquals(completed, result.completed());
        if(completed) assertEquals(SearchTermination.NONE, control.termination());
        else assertEquals(SearchTermination.NODE_LIMIT, control.termination());
    }

    private static SearchControl controlled(long nodes) {
        return SearchControl.controlled(nodes, 0L, -1L, TimeSource.SYSTEM);
    }
}
