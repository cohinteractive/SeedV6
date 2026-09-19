package com.ohinteractive.seedv6.search.quiescence;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.rules.SearchLineHistory;
import com.ohinteractive.seedv6.search.common.SearchControl;
import com.ohinteractive.seedv6.search.common.SearchObserver;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnostics;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.order.MoveOrdering;

import static org.junit.jupiter.api.Assertions.*;

class NnueQuiescenceTest {
    private static final int INF = 32769;
    private static final NnueNetwork NETWORK = NnueNetwork.initialized(73);
    private static final NnueScoreMapping MAPPING = new NnueScoreMapping(1_000_000);
    private static final SearchEvaluation INCREMENTAL = SearchEvaluation.incremental(NETWORK, MAPPING);
    private static final SearchEvaluation REFERENCE = SearchEvaluation.fullRecompute(NETWORK, MAPPING);

    @Test
    void leafHandoffHandlesTacticsChecksQuietEvasionsAndPromotionContinuation() {
        String[] fens = {
            "4r1k1/8/8/8/8/8/8/2B1K3 w - - 0 1",
            "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 2",
            "4k3/8/8/3q4/4P3/8/8/4K3 w - - 0 1",
            "1r5k/P7/8/8/8/8/8/7K w - - 0 1",
            "7k/P7/8/8/8/8/8/7K w - - 0 1",
            "4k3/p7/8/8/8/8/3n4/4K3 w - - 0 1"
        };
        for (int i = 0; i < fens.length; i++) {
            long[] board = Board.fromFen(fens[i]);
            SearchLineHistory history = new SearchLineHistory(GameHistory.initial(board));
            SearchEvaluation.State main = INCREMENTAL.newState(257);
            main.initialize(board, 7);
            int before = main.evaluate(board, 7);
            QuiescenceSearch incremental = search(INCREMENTAL);
            QuiescenceSearch reference = search(REFERENCE);
            SearchDiagnostics diagnostics = new SearchDiagnostics();
            QuiescenceSearch.Result actual = incremental.searchLeaf(board, history, SearchControl.unlimited(),
                    7, -INF, INF, diagnostics, main);
            QuiescenceSearch.Result expected = reference.searchLeaf(board, history, SearchControl.unlimited(),
                    7, -INF, INF);
            assertTrue(actual.completed());
            assertEquals(expected.score(), actual.score(), fens[i]);
            assertEquals(expected.nodes(), actual.nodes(), fens[i]);
            assertEquals(expected.pathDependent(), actual.pathDependent());
            assertTrue(actual.nodes() > 0, fens[i]);
            assertEquals(before, main.evaluate(board, 7));
            assertEquals(1, history.size());
            if (i == 0) assertTrue(diagnostics.snapshot().worker().qsearch().evasionMovesSearched() > 0);
            if (i == 3 || i == 4) {
                assertTrue(diagnostics.snapshot().worker().qsearch().checkedQNodes() > 0);
                assertTrue(diagnostics.snapshot().worker().nodes().maximumQply() >= 2);
            }
        }
    }

    @Test
    void softLimitUsesNnueButCheckedNodesStillSearchQuietEvasions() {
        for (String fen : new String[] {Board.FEN_STARTING_POSITION,
                "4r1k1/8/8/8/8/8/8/2B1K3 w - - 0 1"}) {
            long[] board = Board.fromFen(fen);
            SearchLineHistory history = new SearchLineHistory(GameHistory.initial(board));
            QuiescenceSearch.Result actual = search(INCREMENTAL).searchAtQply(board, history,
                    SearchControl.unlimited(), 20, QuiescenceSearch.SOFT_QPLY_LIMIT, -INF, INF);
            QuiescenceSearch.Result expected = search(REFERENCE).searchAtQply(board, history,
                    SearchControl.unlimited(), 20, QuiescenceSearch.SOFT_QPLY_LIMIT, -INF, INF);
            assertEquals(expected.score(), actual.score());
            assertEquals(expected.nodes(), actual.nodes());
            if (fen.equals(Board.FEN_STARTING_POSITION)) {
                assertEquals(0, actual.nodes());
                assertEquals(REFERENCE.newState(257).evaluate(board, 20), actual.score());
            } else assertTrue(actual.nodes() > 0);
        }
    }

    @Test
    void absoluteCapacityExceptionAndCancellationPermitSubsequentReuse() {
        QuiescenceSearch search = search(INCREMENTAL);
        long[] checked = Board.fromFen("4r1k1/8/8/8/8/8/8/4K3 w - - 0 1");
        SearchLineHistory history = new SearchLineHistory(GameHistory.initial(checked));
        assertThrows(QuiescenceSearch.QuiescenceCapacityException.class, () -> search.searchLeaf(
                checked, history, SearchControl.unlimited(), 256, -INF, INF));
        assertEquals(1, history.size());
        SearchControl limited = SearchControl.controlled(1, 0, -1, () -> 0);
        assertFalse(search.search(new SearchRequest(checked, GameHistory.initial(checked), 0,
                SearchObserver.NONE, limited), -INF, INF).completed());
        long[] quiet = Board.startingPosition();
        QuiescenceSearch.Result result = search.searchLeaf(quiet,
                new SearchLineHistory(GameHistory.initial(quiet)), SearchControl.unlimited(), 256, -INF, INF);
        assertTrue(result.completed());
        assertEquals(REFERENCE.newState(257).evaluate(quiet, 256), result.score());
    }

    private static QuiescenceSearch search(SearchEvaluation definition) {
        return new QuiescenceSearch(new MoveOrdering(257), definition);
    }
}
