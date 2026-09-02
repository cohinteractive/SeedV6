package com.ohinteractive.seedv6.search.quiescence;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.rules.SearchLineHistory;
import com.ohinteractive.seedv6.search.common.SearchControl;
import com.ohinteractive.seedv6.search.common.SearchObserver;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnostics;
import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnosticsSnapshot;
import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnosticsSnapshot.QsearchMetrics;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuiescenceDiagnosticsTest {

    private static final int NEGATIVE_INFINITY = -TranspositionScores.MATE_SCORE - 1;
    private static final int POSITIVE_INFINITY = TranspositionScores.MATE_SCORE + 1;

    @Test
    void ordinaryStandPatAndStandPatCutoffHaveExactIndependentCounters() {
        final long[] board = Board.fromFen("4k3/8/8/8/8/8/3P4/4K3 w - - 0 1");
        final QuiescenceSearch search = new QuiescenceSearch();

        final QuiescenceSearch.Result ordinary = search.search(
            request(board, true), NEGATIVE_INFINITY, POSITIVE_INFINITY
        );
        final SearchDiagnosticsSnapshot ordinarySnapshot = search.lastDiagnostics();
        assertTrue(ordinary.completed());
        assertEquals(0L, ordinary.nodes());
        assertEquals(0L, ordinarySnapshot.totalEnteredNodes());
        assertEquals(0L, ordinarySnapshot.worker().qsearch().standPatCutoffs());

        final QuiescenceSearch.Result cutoff = search.search(request(board, true), -1, 0);
        final SearchDiagnosticsSnapshot cutoffSnapshot = search.lastDiagnostics();
        assertTrue(cutoff.completed());
        assertEquals(0L, cutoff.nodes());
        assertEquals(1L, cutoffSnapshot.worker().qsearch().standPatCutoffs());
        assertEquals(0L, cutoffSnapshot.worker().qsearch().tacticalMovesSearched());
        assertEquals(0L, cutoffSnapshot.worker().qsearch().evasionMovesSearched());
    }

    @Test
    void tacticalExpansionCheckedEvasionAndCheckedChildAreCountedExactly() {
        final SearchDiagnosticsSnapshot tactical = diagnostics(
            "4k3/8/2p1p3/3q4/2P1P3/3Q4/8/4K3 w - - 0 1"
        );
        assertEquals(22L, tactical.worker().nodes().qNodes());
        assertEquals(5, tactical.worker().nodes().maximumAbsolutePly());
        assertEquals(5, tactical.worker().nodes().maximumQply());
        assertEquals(22L, tactical.worker().qsearch().tacticalMovesSearched());
        assertEquals(0L, tactical.worker().qsearch().evasionMovesSearched());
        assertEquals(5L, tactical.worker().qsearch().standPatCutoffs());

        final SearchDiagnosticsSnapshot evasion = diagnostics(
            "4k3/8/8/8/8/8/4r3/4K3 w - - 0 1"
        );
        assertEquals(3L, evasion.worker().nodes().qNodes());
        assertEquals(3L, evasion.worker().qsearch().evasionMovesSearched());
        assertEquals(0L, evasion.worker().qsearch().tacticalMovesSearched());
        assertEquals(2L, evasion.worker().qsearch().standPatCutoffs());

        final SearchDiagnosticsSnapshot checkedChildren = diagnostics(
            "4k3/P7/8/8/8/8/7p/4K3 w - - 0 1"
        );
        assertEquals(8L, checkedChildren.worker().nodes().qNodes());
        assertEquals(2L, checkedChildren.worker().qsearch().checkedQNodes());
        assertEquals(4L, checkedChildren.worker().qsearch().tacticalMovesSearched());
        assertEquals(4L, checkedChildren.worker().qsearch().evasionMovesSearched());
    }

    @Test
    void qmateAndSoftDepthBoundaryHaveExactCountersWithoutFabricatedPruning() {
        final SearchDiagnosticsSnapshot mate = diagnostics(
            "7k/6Q1/6K1/8/8/8/8/8 b - - 0 1"
        );
        assertEquals(1L, mate.worker().qsearch().qmateTerminals());
        assertEquals(0L, mate.totalEnteredNodes());

        final long[] board = Board.fromFen("4k3/8/8/8/8/8/3P4/4K3 w - - 0 1");
        final SearchLineHistory history = new SearchLineHistory(GameHistory.initial(board));
        final SearchDiagnostics accumulator = new SearchDiagnostics();
        final QuiescenceSearch.Result result = new QuiescenceSearch().searchAtQply(
            board, history, controlled(), 0, QuiescenceSearch.SOFT_QPLY_LIMIT,
            NEGATIVE_INFINITY, POSITIVE_INFINITY, accumulator
        );
        final SearchDiagnosticsSnapshot snapshot = accumulator.snapshot();
        assertTrue(result.completed());
        assertEquals(0L, result.nodes());
        assertEquals(1L, snapshot.worker().qsearch().softDepthLimitEncounters());
        assertEquals(QuiescenceSearch.SOFT_QPLY_LIMIT,
            snapshot.worker().nodes().maximumQply());
        assertEquals(0L, snapshot.worker().qsearch().qmateTerminals());
    }

    @Test
    void disabledStandaloneQsearchPublishesSingletonEmptyDiagnostics() {
        final long[] board = Board.fromFen("4k3/8/8/8/8/8/3P4/4K3 w - - 0 1");
        final QuiescenceSearch search = new QuiescenceSearch();
        search.search(request(board, false), NEGATIVE_INFINITY, POSITIVE_INFINITY);
        assertFalse(search.lastDiagnostics().enabled());
        assertEquals(SearchDiagnosticsSnapshot.disabled(), search.lastDiagnostics());
    }

    private static SearchDiagnosticsSnapshot diagnostics(String fen) {
        final long[] board = Board.fromFen(fen);
        final QuiescenceSearch search = new QuiescenceSearch();
        final SearchControl control = controlled();
        final QuiescenceSearch.Result result = search.search(
            request(board, true, control), NEGATIVE_INFINITY, POSITIVE_INFINITY
        );
        assertTrue(result.completed());
        assertEquals(control.nodes(), result.nodes());
        final SearchDiagnosticsSnapshot snapshot = search.lastDiagnostics();
        assertEquals(result.nodes(), snapshot.totalEnteredNodes());
        return snapshot;
    }

    private static SearchRequest request(long[] board, boolean diagnostics) {
        return request(board, diagnostics, controlled());
    }

    private static SearchRequest request(
        long[] board, boolean diagnostics, SearchControl control
    ) {
        return new SearchRequest(
            board, GameHistory.initial(board), 0, SearchObserver.NONE, control, diagnostics
        );
    }

    private static SearchControl controlled() {
        return SearchControl.controlled(-1L, 0L, -1L, () -> 0L);
    }
}
