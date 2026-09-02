package com.ohinteractive.seedv6.search.quiescence;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.rules.SearchLineHistory;
import com.ohinteractive.seedv6.search.common.SearchControl;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.common.SearchTermination;
import com.ohinteractive.seedv6.search.common.TimeSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuiescenceControlTest {

    private static final int NEG_INF = -1_000_000_000;
    private static final int POS_INF = 1_000_000_000;

    @Test
    void preCancelledEntryReturnsNoFabricatedScore() {
        final long[] board = Board.startingPosition();
        final SearchControl control = controlled(-1L);
        control.request(SearchTermination.STOPPED);

        final QuiescenceSearch.Result result = new QuiescenceSearch().search(
            new SearchRequest(board, GameHistory.initial(board), 1, control),
            NEG_INF, POS_INF
        );

        assertFalse(result.completed());
        assertEquals(0L, result.nodes());
        assertThrows(IllegalStateException.class, result::score);
        assertEquals(SearchTermination.STOPPED, control.termination());
    }

    @Test
    void expiredTimeLimitAtEntryReturnsNoFabricatedScore() {
        final long[] board = Board.startingPosition();
        final SearchControl control = SearchControl.controlled(
            -1L, 0L, 0L, () -> 1L
        );

        final QuiescenceSearch.Result result = new QuiescenceSearch().search(
            new SearchRequest(board, GameHistory.initial(board), 1, control),
            NEG_INF, POS_INF
        );

        assertFalse(result.completed());
        assertEquals(0L, result.nodes());
        assertThrows(IllegalStateException.class, result::score);
        assertEquals(SearchTermination.TIME_LIMIT, control.termination());
    }

    @Test
    void qnodesUseTheEstablishedEnteredChildNodeLimitConvention() {
        final long[] oneCapture = Board.fromFen(
            "4k3/8/8/3q4/4P3/8/8/4K3 w - - 0 1"
        );

        final SearchControl zero = controlled(0L);
        final QuiescenceSearch.Result stopped = new QuiescenceSearch().search(
            new SearchRequest(oneCapture, GameHistory.initial(oneCapture), 1, zero),
            NEG_INF, POS_INF
        );
        assertFalse(stopped.completed());
        assertEquals(0L, stopped.nodes());
        assertEquals(0L, zero.nodes());
        assertEquals(SearchTermination.NODE_LIMIT, zero.termination());

        final SearchControl one = controlled(1L);
        final QuiescenceSearch.Result completed = new QuiescenceSearch().search(
            new SearchRequest(oneCapture, GameHistory.initial(oneCapture), 1, one),
            NEG_INF, POS_INF
        );
        assertTrue(completed.completed());
        assertEquals(1L, completed.nodes());
        assertEquals(1L, one.nodes());
        assertEquals(SearchTermination.NONE, one.termination());
    }

    @Test
    void tacticalSequenceLimitUnwindsBoardHistoryAndReusableState() {
        assertInterruptedAndReusable(
            "4k3/8/8/3pP3/3r4/8/8/4K3 w - d6 0 1",
            1L
        );
    }

    @Test
    void fullEvasionLimitUnwindsBoardHistoryAndReusableState() {
        assertInterruptedAndReusable(
            "4r1k1/8/8/8/8/8/8/4K3 w - - 0 1",
            1L
        );
    }

    private static void assertInterruptedAndReusable(String fen, long limit) {
        final long[] board = Board.fromFen(fen);
        final long[] before = board.clone();
        final GameHistory gameHistory = GameHistory.initial(board);
        final SearchLineHistory line = new SearchLineHistory(gameHistory);
        final int size = line.size();
        final long top = line.currentKey();
        final SearchControl control = controlled(limit);
        final QuiescenceSearch search = new QuiescenceSearch();

        final QuiescenceSearch.Result interrupted = search.searchLeaf(
            board, line, control, 0, NEG_INF, POS_INF
        );
        assertFalse(interrupted.completed(), fen);
        assertEquals(limit, interrupted.nodes(), fen);
        assertEquals(limit, control.nodes(), fen);
        assertEquals(SearchTermination.NODE_LIMIT, control.termination(), fen);
        assertThrows(IllegalStateException.class, interrupted::score);
        assertArrayEquals(before, board, fen);
        assertEquals(size, line.size(), fen);
        assertEquals(top, line.currentKey(), fen);

        final int recovered = search.search(
            new SearchRequest(board, gameHistory, 1), NEG_INF, POS_INF
        ).score();
        final int fresh = new QuiescenceSearch().search(
            new SearchRequest(board, gameHistory, 1), NEG_INF, POS_INF
        ).score();
        assertEquals(fresh, recovered, fen);
        assertArrayEquals(before, board, fen);
        assertEquals(size, line.size(), fen);
        assertEquals(top, line.currentKey(), fen);
    }

    private static SearchControl controlled(long nodeLimit) {
        return SearchControl.controlled(nodeLimit, 0L, -1L, TimeSource.SYSTEM);
    }
}
