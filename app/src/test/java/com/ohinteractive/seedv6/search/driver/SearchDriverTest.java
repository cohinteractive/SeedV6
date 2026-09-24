package com.ohinteractive.seedv6.search.driver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.brn.BrnFeatureSchema;
import com.ohinteractive.seedv6.core.brn.BrnModel;
import com.ohinteractive.seedv6.core.brn1.Brn1Model;
import com.ohinteractive.seedv6.core.brn2.Brn2Model;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.evaluation.*;
import com.ohinteractive.seedv6.search.exact.*;
import com.ohinteractive.seedv6.tools.search.ExactSearchHarness;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(30)
class SearchDriverTest {
    @Test void progressesThroughEveryDepthAndMatchesDirectExactAcrossBaselinePositions() {
        int comparisons = 0;
        for(var position : ExactSearchHarness.positions().subList(0, 4)) {
            long[] board = Board.fromFen(position.fen());
            var driver = new SearchDriver();
            var exact = new ExactSearch();
            List<Integer> depths = new ArrayList<>();
            long[] cumulative = {0};
            var observer = new SearchObserver() {
                @Override public void onIterationCompleted(IterationSnapshot snapshot) {
                    var reference = exact.search(board, snapshot.depth());
                    agree(reference, snapshot.result());
                    cumulative[0] += reference.nodes() - 1; // consumer accounting excludes roots
                    assertEquals(cumulative[0], snapshot.nodes());
                    assertEquals(snapshot.depth(), snapshot.diagnostics().iteration().completedIterations());
                    depths.add(snapshot.depth());
                }
            };
            var outcome = driver.search(request(board, 4, observer, SearchControl.unlimited(), true));
            assertEquals(List.of(1, 2, 3, 4), depths);
            comparisons += depths.size();
            assertTrue(outcome.targetDepthCompleted());
            assertFalse(outcome.iterationIncomplete());
            assertEquals(4, outcome.attemptedDepth());
            assertEquals(4, outcome.lastCompletedResult().depth());
            assertEquals(outcome.nodes(), outcome.diagnostics().totalEnteredNodes());
            assertEquals(0, outcome.diagnostics().worker().nodes().qNodes());
            assertEquals(0, outcome.diagnostics().iteration().aspirationAttempts());
            assertEquals(outcome.lastCompletedResult(), driver.search(request(board, 4,
                    SearchObserver.NONE, SearchControl.unlimited(), true)).lastCompletedResult());
        }
        assertEquals(16, comparisons);
    }

    @Test void laterIterationCancellationRetainsOnlyCompletedDepthAndReusableState() {
        var control = control(-1);
        var evaluation = new ExactEvaluator() {
            int iteration;
            @Override public void initialize(long[] board) { iteration++; }
            @Override public int evaluate(long[] board, int ply) {
                if(iteration == 3) control.request(SearchTermination.STOPPED);
                return Eval.evaluate(board);
            }
        };
        var driver = new SearchDriver(new ExactSearchAdapter(evaluation));
        List<Integer> depths = new ArrayList<>();
        var outcome = driver.search(request(Board.startingPosition(), 4, observer(depths), control, true));
        assertEquals(List.of(1, 2), depths);
        assertFalse(outcome.targetDepthCompleted());
        assertTrue(outcome.iterationIncomplete());
        assertEquals(3, outcome.attemptedDepth());
        assertEquals(2, outcome.lastCompletedResult().depth());
        assertTrue(outcome.nodes() > outcome.lastCompletedResult().nodes());
        agree(new ExactSearch().search(Board.startingPosition(), 2), outcome.lastCompletedResult());
        assertEquals(outcome.lastCompletedResult().score(), driver.search(new SearchRequest(
                Board.startingPosition(), 2)).lastCompletedResult().score());
    }

    @Test void cancellationBeforeFirstCompletionNeverInventsResultOrPv() {
        for(long budget : new long[] {0, 1, 19}) {
            var control = control(budget);
            List<Integer> depths = new ArrayList<>();
            var result = new SearchDriver().search(request(Board.startingPosition(), 4, observer(depths), control, false));
            assertNull(result.lastCompletedResult());
            assertFalse(result.targetDepthCompleted());
            assertEquals(budget > 0, result.iterationIncomplete());
            assertEquals(budget, result.nodes());
            assertEquals(budget, control.nodes());
            assertEquals(SearchTermination.NODE_LIMIT, control.termination());
            assertTrue(depths.isEmpty());
        }
        var stopped = control(-1);
        stopped.request(SearchTermination.STOPPED);
        assertNull(new SearchDriver().search(request(Board.startingPosition(), 1,
                SearchObserver.NONE, stopped, false)).lastCompletedResult());
        var attempt = new ExactSearchAdapter().search(request(Board.startingPosition(), 1,
                SearchObserver.NONE, stopped, false));
        assertFalse(attempt.completed());
        assertEquals(Value.INVALID, attempt.score());
        assertFalse(attempt.hasMove());
        assertEquals(0, attempt.principalVariationLength());
    }

    @Test void cumulativeNodeBudgetAllowsLastNodeToUnwindAndStopsBeforeNextIteration() {
        long[] board = Board.startingPosition();
        long one = new ExactSearch().search(board, 1).nodes() - 1;
        long two = new ExactSearch().search(board, 2).nodes() - 1;
        for(long budget : new long[] {one, one + 1, one + two - 1, one + two, one + two + 1}) {
            var control = control(budget);
            var result = new SearchDriver().search(request(board, 2, SearchObserver.NONE, control, true));
            boolean complete = budget >= one + two;
            assertEquals(complete, result.targetDepthCompleted());
            assertEquals(complete ? 2 : 1, result.lastCompletedResult().depth());
            assertEquals(Math.min(budget, one + two), control.nodes());
            assertEquals(control.nodes(), result.nodes());
            assertEquals(complete ? SearchTermination.NONE : SearchTermination.NODE_LIMIT, control.termination());
        }
    }

    @Test void deadlinesCancelThroughExactPathAndRetainPriorCompletedDepth() {
        AtomicLong now = new AtomicLong();
        var control = SearchControl.controlled(-1, 0, 10, now::get);
        var evaluator = new ExactEvaluator() {
            int iteration;
            @Override public void initialize(long[] board) { iteration++; }
            @Override public int evaluate(long[] board, int ply) {
                if(iteration == 2) now.set(10);
                return Eval.evaluate(board);
            }
        };
        var result = new SearchDriver(new ExactSearchAdapter(evaluator)).search(request(
                Board.startingPosition(), 4, SearchObserver.NONE, control, false));
        assertTrue(result.iterationIncomplete());
        assertEquals(1, result.lastCompletedResult().depth());
        assertEquals(SearchTermination.TIME_LIMIT, control.termination());
        var expired = SearchControl.controlled(-1, 0, 0, () -> 0);
        assertNull(new SearchDriver().search(request(Board.startingPosition(), 1,
                SearchObserver.NONE, expired, false)).lastCompletedResult());
    }

    @Test void evaluatorSelectionAndIncrementalNnueAgreeWithIndependentRecomputation() {
        var network = NnueNetwork.initialized(817);
        String[] fens = {"r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1",
                "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 2", "1r5k/P7/8/8/8/8/8/7K w - - 0 1"};
        var incremental = SearchEvaluation.incremental(network);
        var rebuilt = SearchEvaluation.fullRecompute(network, NnueScoreMapping.V1);
        var driver = new SearchDriver(incremental);
        for(String fen : fens) {
            var board = Board.fromFen(fen);
            var result = driver.search(new SearchRequest(board, 2)).lastCompletedResult();
            agree(new ExactSearch(incremental).search(board, 2), result);
            agree(new ExactSearch(rebuilt).search(board, 2), result);
        }
        // Same contract also carries the three legitimate BRN definitions.
        for(var definition : List.of(SearchEvaluation.brn(new BrnModel(new double[BrnFeatureSchema.PARAMETER_COUNT])),
                SearchEvaluation.brn1(new Brn1Model(new double[Brn1Model.PARAMETER_COUNT])),
                SearchEvaluation.brn2(new Brn2Model(new double[Brn2Model.PARAMETER_COUNT])))) {
            var board = Board.fromFen(fens[1]);
            agree(new ExactSearch(definition).search(board, 2),
                    new SearchDriver(definition).search(new SearchRequest(board, 2)).lastCompletedResult());
        }
    }

    @Test void initializesReusableEvaluatorPerInvocationAndPreservesInput() {
        var initialized = new AtomicInteger();
        var driver = new SearchDriver(new ExactSearchAdapter(new ExactEvaluator() {
            @Override public void initialize(long[] board) { initialized.incrementAndGet(); }
            @Override public int evaluate(long[] board, int ply) { return 73; }
        }));
        long[] board = Board.startingPosition(), original = board.clone();
        var one = driver.search(new SearchRequest(board, 3));
        assertEquals(-73, one.lastCompletedResult().score());
        assertEquals(3, initialized.get());
        var two = driver.search(new SearchRequest(board, 2));
        assertEquals(73, two.lastCompletedResult().score());
        assertEquals(5, initialized.get());
        assertArrayEquals(original, board);
        assertEquals(-73, one.lastCompletedResult().score());
    }

    @Test void terminalAndRuleDrawResultsDoNotInventMovesOrRepeatDepths() {
        for(String fen : new String[] {"7k/6Q1/5K2/8/8/8/8/8 b - - 0 1",
                "7k/5K2/6Q1/8/8/8/8/8 b - - 0 1", "4k3/8/8/8/8/8/P7/4K3 w - - 100 1"}) {
            var board = Board.fromFen(fen);
            var result = new SearchDriver().search(new SearchRequest(board, 4));
            assertTrue(result.targetDepthCompleted());
            assertTrue(result.terminalRoot());
            assertEquals(1, result.lastCompletedResult().depth());
            assertFalse(result.lastCompletedResult().hasMove());
            agree(new ExactSearch().search(board, 1), result.lastCompletedResult());
        }
    }

    @Test void threadInterruptionProducesIncompleteResultWithoutClearingFlag() {
        try {
            Thread.currentThread().interrupt();
            var control = control(-1);
            assertNull(new SearchDriver().search(request(Board.startingPosition(), 3,
                    SearchObserver.NONE, control, false)).lastCompletedResult());
            assertEquals(SearchTermination.STOPPED, control.termination());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally { Thread.interrupted(); }
    }

    @Test void depthValidationReentryAndObserverFailurePreserveOwnership() {
        var driver = new SearchDriver();
        assertThrows(IllegalArgumentException.class, () -> driver.search(new SearchRequest(Board.startingPosition(), 0)));
        assertThrows(IllegalArgumentException.class, () -> driver.search(new SearchRequest(Board.startingPosition(), 257)));
        var observer = new SearchObserver() {
            @Override public void onIterationCompleted(IterationSnapshot snapshot) {
                assertThrows(IllegalStateException.class, () -> driver.search(new SearchRequest(Board.startingPosition(), 1)));
                throw new IllegalStateException("observer");
            }
        };
        assertThrows(IllegalStateException.class, () -> driver.search(new SearchRequest(Board.startingPosition(), 3, observer)));
        assertEquals(1, driver.lastCompletedResult().depth());
        assertTrue(driver.search(new SearchRequest(Board.startingPosition(), 2)).targetDepthCompleted());
    }

    static void agree(ExactSearchResult exact, SearchResult result) {
        assertTrue(exact.completed());
        assertTrue(result.completed());
        assertEquals(exact.score(), result.score());
        assertEquals(exact.bestMove(), result.bestMove());
        assertEquals(exact.completedDepth(), result.depth());
        assertArrayEquals(exact.principalVariation(), result.principalVariation());
    }
    static SearchControl control(long nodes) { return SearchControl.controlled(nodes, System.nanoTime(), -1, TimeSource.SYSTEM); }
    static SearchRequest request(long[] board, int depth, SearchObserver observer, SearchControl control, boolean diagnostics) {
        return new SearchRequest(board, GameHistory.initial(board), depth, observer, control, diagnostics);
    }
    static SearchObserver observer(List<Integer> depths) {
        return new SearchObserver() {
            @Override public void onIterationCompleted(IterationSnapshot snapshot) { depths.add(snapshot.depth()); }
        };
    }
}
