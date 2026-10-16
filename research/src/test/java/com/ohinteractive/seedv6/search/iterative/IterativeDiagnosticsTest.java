package com.ohinteractive.seedv6.search.iterative;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.alphabeta.AlphaBetaPvsSearch;
import com.ohinteractive.seedv6.search.common.IterationSnapshot;
import com.ohinteractive.seedv6.search.common.SearchControl;
import com.ohinteractive.seedv6.search.common.SearchObserver;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.common.SearchResult;
import com.ohinteractive.seedv6.search.common.SearchTermination;
import com.ohinteractive.seedv6.search.common.WindowedSearch;
import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnosticsSnapshot;
import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnosticsSnapshot.IterationMetrics;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;
import com.ohinteractive.seedv6.search.tt.TranspositionTable;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IterativeDiagnosticsTest {

    @Test
    void ordinaryIterationAndForcedFailLowHighWideningHaveExactControllerCounts() {
        assertAspiration(new ScriptedWindowSearch(0, 0), 1L, 0L, 0L, 0L);
        assertAspiration(new ScriptedWindowSearch(0, 500), 3L, 0L, 3L, 1L);
        assertAspiration(new ScriptedWindowSearch(0, -500), 3L, 3L, 0L, 1L);
    }

    @Test
    void completedSnapshotsAreCumulativeImmutableAndNextIterativeSearchStartsFresh() {
        final long[] board = Board.startingPosition();
        final List<IterationSnapshot> snapshots = new ArrayList<>();
        final IterativeDeepeningSearch search = new IterativeDeepeningSearch(
            new AlphaBetaPvsSearch(new TranspositionTable(1 << 15))
        );
        final IterativeSearchOutcome first = search.search(request(
            board, 3, controlled(-1L), true, snapshots::add
        ));
        assertTrue(first.targetDepthCompleted());
        assertEquals(3, snapshots.size());
        assertEquals(List.of(1L, 2L, 3L), snapshots.stream()
            .map(value -> value.diagnostics().iteration().completedIterations()).toList());
        assertEquals(List.of(0L, 1L, 2L), snapshots.stream()
            .map(value -> value.diagnostics().iteration().aspirationAttempts()).toList());
        assertEquals(List.of(1, 2, 3), snapshots.stream()
            .map(value -> value.diagnostics().iteration().deepestCompletedDepth()).toList());
        assertTrue(snapshots.get(0).diagnostics().totalEnteredNodes()
            < snapshots.get(1).diagnostics().totalEnteredNodes());
        assertTrue(snapshots.get(1).diagnostics().totalEnteredNodes()
            < snapshots.get(2).diagnostics().totalEnteredNodes());
        assertEquals(snapshots.get(2).nodes(), snapshots.get(2).diagnostics().totalEnteredNodes());

        final SearchDiagnosticsSnapshot retained = snapshots.get(0).diagnostics();
        final long retainedNodes = retained.totalEnteredNodes();
        final long[] terminal = Board.fromFen("7k/6Q1/6K1/8/8/8/8/8 b - - 0 1");
        final IterativeSearchOutcome second = search.search(
            request(terminal, 3, controlled(-1L), true, ignored -> { })
        );
        assertEquals(1L, second.diagnostics().iteration().completedIterations());
        assertEquals(1, second.diagnostics().iteration().deepestCompletedDepth());
        assertEquals(0L, second.diagnostics().totalEnteredNodes());
        assertEquals(1L, second.diagnostics().worker().transpositionTable().probes());
        assertEquals(retainedNodes, retained.totalEnteredNodes());
        assertEquals(1L, retained.iteration().completedIterations());
    }

    @Test
    void enabledDisabledIterativeNodeLimitAndCancellationAreIdentical() {
        final long[] board = Board.startingPosition();
        final Run disabledLimited = limited(board, false, 25L, false);
        final Run enabledLimited = limited(board, true, 25L, false);
        assertEquals(SearchTermination.NODE_LIMIT, disabledLimited.control.termination());
        assertEquals(SearchTermination.NODE_LIMIT, enabledLimited.control.termination());
        assertEquals(25L, disabledLimited.control.nodes());
        assertEquals(25L, enabledLimited.control.nodes());
        assertOutcomesEqual(disabledLimited.outcome, enabledLimited.outcome);
        assertEquals(enabledLimited.control.nodes(),
            enabledLimited.outcome.diagnostics().totalEnteredNodes());

        final Run disabledStopped = limited(board, false, -1L, true);
        final Run enabledStopped = limited(board, true, -1L, true);
        assertEquals(SearchTermination.STOPPED, disabledStopped.control.termination());
        assertEquals(SearchTermination.STOPPED, enabledStopped.control.termination());
        assertEquals(disabledStopped.control.nodes(), enabledStopped.control.nodes());
        assertOutcomesEqual(disabledStopped.outcome, enabledStopped.outcome);
        assertEquals(1, enabledStopped.outcome.lastCompletedResult().depth());
        assertEquals(enabledStopped.control.nodes(),
            enabledStopped.outcome.diagnostics().totalEnteredNodes());
    }

    @Test
    void controlledAspirationPathIsResultAndNodeNeutral() {
        final ScriptedWindowSearch disabledExact = new ScriptedWindowSearch(0, 500);
        final ScriptedWindowSearch enabledExact = new ScriptedWindowSearch(0, 500);
        final SearchControl disabledControl = controlled(-1L);
        final SearchControl enabledControl = controlled(-1L);
        final long[] board = Board.startingPosition();
        final IterativeSearchOutcome disabled = new IterativeDeepeningSearch(
            disabledExact, 10, 3, 0
        ).search(request(board, 2, disabledControl, false, ignored -> { }));
        final IterativeSearchOutcome enabled = new IterativeDeepeningSearch(
            enabledExact, 10, 3, 0
        ).search(request(board, 2, enabledControl, true, ignored -> { }));

        assertOutcomesEqual(disabled, enabled);
        assertEquals(disabledControl.nodes(), enabledControl.nodes());
        assertEquals(disabledExact.windows, enabledExact.windows);
        assertFalse(disabled.diagnostics().enabled());
        assertTrue(enabled.diagnostics().enabled());
        assertEquals(new IterationMetrics(2L, 3L, 0L, 3L, 1L, 2),
            enabled.diagnostics().iteration());
    }

    private static void assertAspiration(
        ScriptedWindowSearch exact, long attempts, long failLow,
        long failHigh, long fallback
    ) {
        final long[] board = Board.startingPosition();
        final IterativeSearchOutcome outcome = new IterativeDeepeningSearch(
            exact, 10, 3, 0
        ).search(request(board, 2, controlled(-1L), true, ignored -> { }));
        final IterationMetrics metrics = outcome.diagnostics().iteration();
        assertTrue(outcome.targetDepthCompleted());
        assertEquals(2L, metrics.completedIterations());
        assertEquals(attempts, metrics.aspirationAttempts());
        assertEquals(failLow, metrics.failLowResearches());
        assertEquals(failHigh, metrics.failHighResearches());
        assertEquals(fallback, metrics.fullWindowFallbacks());
        assertEquals(2, metrics.deepestCompletedDepth());
    }

    private static Run limited(
        long[] board, boolean diagnostics, long nodeLimit, boolean stopAfterOne
    ) {
        final SearchControl control = controlled(nodeLimit);
        final SearchObserver observer = stopAfterOne ? new SearchObserver() {
            @Override
            public void onIterationCompleted(IterationSnapshot snapshot) {
                control.request(SearchTermination.STOPPED);
            }
        } : SearchObserver.NONE;
        final IterativeSearchOutcome outcome = new IterativeDeepeningSearch(
            new AlphaBetaPvsSearch(new TranspositionTable(1 << 15))
        ).search(new SearchRequest(
            board, GameHistory.initial(board), 4, observer, control, diagnostics
        ));
        return new Run(outcome, control);
    }

    private static SearchRequest request(
        long[] board, int depth, SearchControl control, boolean diagnostics,
        java.util.function.Consumer<IterationSnapshot> consumer
    ) {
        return new SearchRequest(
            board, GameHistory.initial(board), depth, new SearchObserver() {
                @Override
                public void onIterationCompleted(IterationSnapshot snapshot) {
                    consumer.accept(snapshot);
                }
            }, control, diagnostics
        );
    }

    private static SearchControl controlled(long nodeLimit) {
        return SearchControl.controlled(nodeLimit, 0L, -1L, () -> 0L);
    }

    private static void assertOutcomesEqual(
        IterativeSearchOutcome expected, IterativeSearchOutcome actual
    ) {
        assertEquals(expected.targetDepthCompleted(), actual.targetDepthCompleted());
        assertEquals(expected.terminalRoot(), actual.terminalRoot());
        final SearchResult expectedResult = expected.lastCompletedResult();
        final SearchResult actualResult = actual.lastCompletedResult();
        if(expectedResult == null || actualResult == null) {
            assertEquals(expectedResult, actualResult);
            return;
        }
        assertNotNull(actualResult);
        assertEquals(expectedResult.bestMove(), actualResult.bestMove());
        assertEquals(expectedResult.hasMove(), actualResult.hasMove());
        assertEquals(expectedResult.score(), actualResult.score());
        assertEquals(expectedResult.depth(), actualResult.depth());
        assertEquals(expectedResult.nodes(), actualResult.nodes());
        assertEquals(expectedResult.legalRootMoves(), actualResult.legalRootMoves());
        assertEquals(expectedResult.completed(), actualResult.completed());
        assertArrayEquals(expectedResult.principalVariation(), actualResult.principalVariation());
    }

    private record Run(IterativeSearchOutcome outcome, SearchControl control) { }
    private record Window(int depth, int alpha, int beta) { }

    private static final class ScriptedWindowSearch implements WindowedSearch {
        final List<Window> windows = new ArrayList<>();
        private final int firstScore;
        private final int laterScore;
        private boolean active;

        ScriptedWindowSearch(int firstScore, int laterScore) {
            this.firstScore = firstScore;
            this.laterScore = laterScore;
        }

        @Override
        public SearchResult search(SearchRequest request) {
            beginTopLevelSearch();
            try {
                return searchWindow(
                    request, -TranspositionScores.MATE_SCORE - 1,
                    TranspositionScores.MATE_SCORE + 1
                );
            } finally {
                endTopLevelSearch();
            }
        }

        @Override
        public void beginTopLevelSearch() {
            if(active) throw new AssertionError("already active");
            active = true;
        }

        @Override
        public SearchResult searchWindow(SearchRequest request, int alpha, int beta) {
            if(!active) throw new AssertionError("not active");
            windows.add(new Window(request.depth(), alpha, beta));
            if(!request.control().tryEnterNode()) {
                return new SearchResult(0L, false, 0, request.depth(), 0L, 1, false);
            }
            return new SearchResult(
                0L, false, request.depth() == 1 ? firstScore : laterScore,
                request.depth(), 1L, 1, true
            );
        }

        @Override
        public void endTopLevelSearch() {
            if(!active) throw new AssertionError("not active");
            active = false;
        }

        @Override
        public int maxSupportedDepth() {
            return 8;
        }
    }
}
