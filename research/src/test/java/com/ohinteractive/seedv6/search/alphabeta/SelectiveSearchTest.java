package com.ohinteractive.seedv6.search.alphabeta;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.alphabeta.SelectiveSearchPolicy.Heuristic;
import com.ohinteractive.seedv6.search.common.SearchControl;
import com.ohinteractive.seedv6.search.common.SearchObserver;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.common.SearchResult;
import com.ohinteractive.seedv6.search.common.TimeSource;
import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnosticsSnapshot.SelectiveMetrics;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;
import com.ohinteractive.seedv6.search.tt.TranspositionTable;
import com.ohinteractive.seedv6.search.tt.TranspositionTable.Probe;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelectiveSearchTest {

    private static final String KIWIPETE =
        "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1";

    @Test
    void retainedPoliciesAreIndependentlySwitchableAndAllOffIsExplicit() {
        assertFalse(SelectiveSearchPolicy.allOff().anyEnabled());
        assertEquals("all-off", SelectiveSearchPolicy.allOff().id());
        assertEquals("production", SelectiveSearchPolicy.production().id());
        for(Heuristic heuristic : Heuristic.values()) {
            final SelectiveSearchPolicy only = SelectiveSearchPolicy.only(heuristic);
            assertTrue(only.anyEnabled(), heuristic.name());
            assertEquals(
                SelectiveSearchPolicy.allOff(), only.with(heuristic, false), heuristic.name()
            );
        }
    }

    @Test
    void mateDistanceBoundsCollapseOnlyAnImpossibleInteriorMateWindow() {
        final long[] board = Board.fromFen(
            "7k/5Q2/6K1/8/8/8/8/8 w - - 0 1"
        );
        final AlphaBetaPvsSearch search = search(SelectiveSearchPolicy.only(Heuristic.MATE_DISTANCE));
        final SearchResult result = window(
            search, board, 2, -TranspositionScores.MATE_SCORE,
            -TranspositionScores.MATE_SCORE + 1
        );

        assertTrue(result.completed());
        assertEquals(-TranspositionScores.MATE_SCORE + 1, result.score());
        assertEquals(1L, result.nodes());
        assertEquals(1L, result.diagnostics().worker().selective().mateDistanceCutoffs());
        AlphaBetaPvsSearchTest.assertLegalPv(board, result);
    }

    @Test
    void razoringUsesWs9ProbeAndTacticalResultCanRejectStaticCandidate() {
        final long[] board = Board.fromFen(KIWIPETE);
        final SearchResult baseline = search(SelectiveSearchPolicy.allOff()).search(request(board, 3));
        final SearchResult razor = search(SelectiveSearchPolicy.only(Heuristic.RAZORING))
            .search(request(board, 3));
        final SelectiveMetrics metrics = razor.diagnostics().worker().selective();

        assertSameSemantics(baseline, razor);
        assertEquals(40L, metrics.razorAttempts());
        assertEquals(40L, metrics.razorQsearchProbes());
        assertEquals(38L, metrics.razorAcceptedResults());
        assertTrue(metrics.razorAcceptedResults() < metrics.razorQsearchProbes());
        assertEquals(8_629L, razor.nodes());
        assertTrue(razor.nodes() < baseline.nodes());
    }

    @Test
    void futilityPrunesOnlyPostFirstQuietMovesAndLeavesExactShallowResult() {
        final long[] board = Board.fromFen(KIWIPETE);
        final SearchResult baseline = search(SelectiveSearchPolicy.allOff()).search(request(board, 3));
        final SearchResult futility = search(SelectiveSearchPolicy.only(Heuristic.FUTILITY))
            .search(request(board, 3));
        final SelectiveMetrics metrics = futility.diagnostics().worker().selective();

        assertSameSemantics(baseline, futility);
        assertEquals(46L, metrics.futilityEligibleNodes());
        assertEquals(1_575L, metrics.futilityQuietMovesPruned());
        assertEquals(8_418L, futility.nodes());
        assertTrue(futility.nodes() < baseline.nodes());
    }

    @Test
    void everyRazorAndFutilityEligibilityGuardIsExplicit() {
        assertTrue(AlphaBetaPvsSearch.razorEligible(1, false, false, 250, 0));
        assertFalse(AlphaBetaPvsSearch.razorEligible(2, false, false, 250, 0));
        assertFalse(AlphaBetaPvsSearch.razorEligible(1, true, false, 250, 0));
        assertFalse(AlphaBetaPvsSearch.razorEligible(1, false, true, 250, 0));
        assertFalse(AlphaBetaPvsSearch.razorEligible(
            1, false, false, TranspositionScores.MATE_THRESHOLD, 0
        ));
        assertFalse(AlphaBetaPvsSearch.razorEligible(1, false, false, 249, 0));

        assertTrue(AlphaBetaPvsSearch.futilityEligible(1, false, false, 180, 0));
        assertFalse(AlphaBetaPvsSearch.futilityEligible(2, false, false, 180, 0));
        assertFalse(AlphaBetaPvsSearch.futilityEligible(1, true, false, 180, 0));
        assertFalse(AlphaBetaPvsSearch.futilityEligible(1, false, true, 180, 0));
        assertFalse(AlphaBetaPvsSearch.futilityEligible(
            1, false, false, TranspositionScores.MATE_THRESHOLD, 0
        ));
        assertFalse(AlphaBetaPvsSearch.futilityEligible(1, false, false, 179, 0));
    }

    @Test
    void cumulativeBundleMatchesAllOffAcrossShallowSpecialAndEndgameCorpus() {
        final List<String> fens = List.of(
            Board.FEN_STARTING_POSITION,
            KIWIPETE,
            "8/2p5/3p4/1P1Pp3/4P3/2K5/8/2k5 w - - 0 1",
            "4k3/8/8/3q4/4P3/8/8/4K3 w - - 0 1",
            "4k3/8/8/8/8/8/3P4/4K3 w - - 0 1",
            "4k3/8/8/8/8/8/4r3/4K3 w - - 0 1",
            "4k3/8/8/8/8/8/1N3N2/4K3 w - - 0 1",
            "4k3/8/2p1p3/3q4/2P1P3/3Q4/8/4K3 w - - 0 1",
            "4k3/P7/8/8/8/8/7p/4K3 w - - 0 1",
            "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1",
            "7k/6Q1/6K1/8/8/8/8/8 b - - 0 1",
            "7k/5Q2/6K1/8/8/8/8/8 b - - 0 1",
            "8/8/8/8/8/2k5/2p5/2R1K3 b - - 0 1",
            "8/8/8/8/8/2k5/2p5/1N2K3 b - - 0 1",
            "k6r/6P1/8/8/8/8/8/7K w - - 0 1"
        );
        for(String fen : fens) {
            final long[] board = Board.fromFen(fen);
            final SearchResult baseline = search(SelectiveSearchPolicy.allOff())
                .search(request(board, 3));
            final SearchResult selective = search(SelectiveSearchPolicy.production())
                .search(request(board, 3));
            assertSameSemantics(baseline, selective);
            AlphaBetaPvsSearchTest.assertLegalPv(board, selective);
        }
    }

    @Test
    void cumulativeOrderPreservesDepthFiveResultAtEveryStage() {
        final long[] board = Board.startingPosition();
        final SearchResult baseline = search(SelectiveSearchPolicy.allOff())
            .search(request(board, 5));
        SelectiveSearchPolicy cumulative = SelectiveSearchPolicy.allOff();
        for(Heuristic heuristic : new Heuristic[] {
            Heuristic.MATE_DISTANCE,
            Heuristic.RAZORING,
            Heuristic.FUTILITY
        }) {
            cumulative = cumulative.with(heuristic, true);
            final SearchResult result = search(cumulative).search(request(board, 5));
            assertSameSemantics(baseline, result);
            AlphaBetaPvsSearchTest.assertLegalPv(board, result);
        }
    }

    @Test
    void nestedRazorCancellationRestoresHistoryAndAllowsDeterministicReuse() {
        assertNestedCancellation(
            Board.fromFen(KIWIPETE), 3,
            SelectiveSearchPolicy.only(Heuristic.RAZORING), 5_523L
        );
    }

    @Test
    void speculativeRazorAndFutilityUpperBoundsAreNotStored() {
        final long[] board = Board.fromFen("4k3/8/8/8/8/8/3P4/4K3 w - - 0 1");
        for(Heuristic heuristic : new Heuristic[] {Heuristic.RAZORING, Heuristic.FUTILITY}) {
            final TranspositionTable table = new TranspositionTable(1 << 12);
            final AlphaBetaPvsSearch search = new AlphaBetaPvsSearch(
                table, SelectiveSearchPolicy.only(heuristic)
            );
            final SearchResult result = window(search, board, 1, 1_000, 1_001);
            assertTrue(result.completed(), heuristic.name());
            final Probe probe = new Probe();
            table.probe(board[Board.KEY], 1, -32_769, 32_769, 0, probe);
            assertFalse(probe.keyMatches(), heuristic + " speculative root bound reached TT");
        }
    }

    @Test
    void formalRepetitionAndFiftyMoveDrawsPrecedeSelectivePolicies() {
        final long[] repeatedBoard = Board.fromFen(
            "4k3/8/8/8/8/8/8/Q3K3 w - - 2 1"
        );
        final GameHistory repeated = GameHistory.builder(repeatedBoard)
            .appendPosition(repeatedBoard).appendPosition(repeatedBoard).snapshot();
        final SearchResult repetition = search(SelectiveSearchPolicy.production()).search(
            new SearchRequest(
                repeatedBoard, repeated, 4, SearchObserver.NONE,
                SearchControl.unlimited(), true
            )
        );
        assertEquals(0, repetition.score());
        assertEquals(0L, repetition.nodes());
        assertEquals(
            new SelectiveMetrics(0L, 0L, 0L, 0L, 0L, 0L),
            repetition.diagnostics().worker().selective()
        );

        final long[] fifty = Board.fromFen(
            "4k3/8/8/8/8/8/8/Q3K3 w - - 100 1"
        );
        final SearchResult fiftyMove = search(SelectiveSearchPolicy.production())
            .search(request(fifty, 4));
        assertEquals(0, fiftyMove.score());
        assertEquals(0L, fiftyMove.nodes());
        assertEquals(
            repetition.diagnostics().worker().selective(),
            fiftyMove.diagnostics().worker().selective()
        );
    }

    private static void assertNestedCancellation(
        long[] board, int depth, SelectiveSearchPolicy policy, long nodeLimit
    ) {
        final GameHistory history = GameHistory.initial(board);
        final SearchControl control = SearchControl.controlled(
            nodeLimit, 0L, -1L, TimeSource.SYSTEM
        );
        final AlphaBetaPvsSearch interrupted = search(policy);
        final SearchResult stopped = interrupted.search(new SearchRequest(
            board, history, depth, SearchObserver.NONE, control, true
        ));
        assertFalse(stopped.completed());
        assertEquals(nodeLimit, stopped.nodes());
        assertEquals(history.size(), interrupted.statistics().historyStartSize());
        assertEquals(history.size(), interrupted.statistics().historyEndSize());
        assertTrue(stopped.diagnostics().worker().selective().razorAttempts() > 0L);
        AlphaBetaPvsSearchTest.assertLegalPv(board, stopped);

        interrupted.newGame();
        final SearchResult reused = interrupted.search(request(board, depth));
        final SearchResult fresh = search(policy).search(request(board, depth));
        assertSameSemantics(fresh, reused);
        assertEquals(fresh.nodes(), reused.nodes());
    }

    private static SearchRequest request(long[] board, int depth) {
        return new SearchRequest(
            board, GameHistory.initial(board), depth, SearchObserver.NONE,
            SearchControl.unlimited(), true
        );
    }

    private static AlphaBetaPvsSearch search(SelectiveSearchPolicy policy) {
        return new AlphaBetaPvsSearch(new TranspositionTable(1 << 15), policy);
    }

    private static SearchResult window(
        AlphaBetaPvsSearch search, long[] board, int depth, int alpha, int beta
    ) {
        search.beginTopLevelSearch();
        try {
            return search.searchWindow(request(board, depth), alpha, beta);
        } finally {
            search.endTopLevelSearch();
        }
    }

    private static void assertSameSemantics(SearchResult expected, SearchResult actual) {
        assertEquals(expected.score(), actual.score());
        assertEquals(expected.bestMove(), actual.bestMove());
        assertEquals(expected.hasMove(), actual.hasMove());
        assertEquals(expected.depth(), actual.depth());
        assertEquals(expected.legalRootMoves(), actual.legalRootMoves());
        assertEquals(expected.completed(), actual.completed());
        assertArrayEquals(expected.principalVariation(), actual.principalVariation());
    }
}
