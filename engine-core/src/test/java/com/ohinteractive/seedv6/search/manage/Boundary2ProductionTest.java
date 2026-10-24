package com.ohinteractive.seedv6.search.manage;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(120)
class Boundary2ProductionTest {
    @Test void everyEvaluatorAndWorkerUsesDefaultAndExplicitFallbackWithoutChangingComposition() throws Exception {
        for (var evaluator : BaselineEngine.Evaluator.values()) {
            var engine = BaselineEngine.load(evaluator);
            var factual = engine.withClockPolicy(ClockPolicy.FACTUAL);
            assertEquals(ClockPolicy.BOUNDARY2, engine.clockPolicy());
            assertSame(engine.evaluation(), factual.evaluation());
            assertEquals(660, engine.clockBudgetMillis(7959, 100, 30));
            assertEquals(330, factual.clockBudgetMillis(7959, 100, 30));
            for (int workers = 1; workers <= 16; workers++) {
                assertEquals(evaluator == BaselineEngine.Evaluator.CONVENTIONAL_CP64 ? 3407871 : 262143,
                        BaselineEngine.configuration(engine.evaluation(), workers, 1).mechanisms().flags());
                for (var selected : List.of(engine, factual)) {
                    var search = selected.create(workers, 1);
                    assertEquals(selected == engine, search instanceof Boundary2ClockPolicy);
                    try (var driver = new SearchDriver(search)) {
                        var board = Board.startingPosition(); var before = board.clone();
                        var result = driver.search(new SearchRequest(board, 2));
                        assertTrue(result.targetDepthCompleted()); assertTrue(result.lastCompletedResult().hasMove());
                        assertEquals(0, driver.activeSearchThreads()); assertArrayEquals(before, board);
                    }
                }
                assertTrue(engine.identity(workers, 1).contains("clockPolicy=boundary2-d6-v1"));
                assertEquals(engine.identity(workers, 1).replace("boundary2-d6-v1", "factual"), factual.identity(workers, 1));
            }
        }
    }
    @Test void propertyOverrideIsSnapshottedAndIndependentInstancesRemainIsolated() throws Exception {
        String prior = System.getProperty(ClockPolicy.PROPERTY);
        try {
            System.setProperty(ClockPolicy.PROPERTY, "factual");
            var first = BaselineEngine.load(BaselineEngine.Evaluator.HCE);
            System.setProperty(ClockPolicy.PROPERTY, "BOUNDARY2");
            var second = BaselineEngine.load(BaselineEngine.Evaluator.HCE);
            assertEquals(ClockPolicy.FACTUAL, first.clockPolicy());
            assertEquals(ClockPolicy.BOUNDARY2, second.clockPolicy());
            assertEquals(330, first.clockBudgetMillis(7959, 100, 30));
            assertEquals(660, second.clockBudgetMillis(7959, 100, 30));
            System.setProperty(ClockPolicy.PROPERTY, "typo");
            assertThrows(IllegalArgumentException.class, () -> BaselineEngine.load(BaselineEngine.Evaluator.HCE));
        } finally { if (prior == null) System.clearProperty(ClockPolicy.PROPERTY); else System.setProperty(ClockPolicy.PROPERTY, prior); }
    }
    @Test void softBoundaryHasNoExtraIterationAndNeverOverridesHardNodeOrExternalStop() {
        for (String mode : List.of("soft", "hard", "nodes", "stop", "explicit", "incomplete")) {
            var time = new AtomicLong(); var published = new ArrayList<Integer>();
            var control = SearchControl.controlled(mode.equals("nodes") ? 6 : -1, 0, 200, time::get, !mode.equals("explicit"));
            SingleDepthSearch exact = new SingleDepthSearch() {
                public int maxSupportedDepth() { return 64; }
                public SearchResult search(SearchRequest request) {
                request.control().tryEnterNode();
                int d = request.depth(); time.set(d < 6 ? d * 5 : 50);
                if (mode.equals("incomplete") && d == 6) {
                    time.set(200); assertFalse(control.checkpoint());
                    return new SearchResult(0, false, 0, d, 1, 20, false);
                }
                return new SearchResult(42, true, 12, d, 1, 20, true);
                }
            };
            try (var driver = new SearchDriver(ClockPolicy.BOUNDARY2.apply(exact))) {
                var board = Board.startingPosition();
                var result = driver.search(new SearchRequest(board, GameHistory.initial(board), 12,
                        new SearchObserver() { public void onIterationCompleted(IterationSnapshot snapshot) {
                            published.add(snapshot.depth());
                            if (snapshot.depth() == 6 && mode.equals("hard")) time.set(200);
                            if (snapshot.depth() == 6 && mode.equals("stop")) control.request(SearchTermination.STOPPED);
                        } }, control, false));
                int depth = mode.equals("explicit") ? 12 : mode.equals("incomplete") ? 5 : 6;
                assertEquals(depth, result.lastCompletedResult().depth());
                assertEquals(depth, published.size());
                assertEquals(mode.equals("explicit"), result.targetDepthCompleted());
                assertEquals(mode.equals("incomplete"), result.iterationIncomplete());
                var expected = switch (mode) {
                    case "soft" -> SearchTermination.TIME_ALLOCATION;
                    case "hard", "incomplete" -> SearchTermination.TIME_LIMIT;
                    case "nodes" -> SearchTermination.NODE_LIMIT;
                    case "stop" -> SearchTermination.STOPPED;
                    default -> SearchTermination.COMPLETED;
                };
                if (!mode.equals("explicit")) assertEquals(expected, control.termination());
            }
        }
    }
    @Test void representativeManagedClocksRepeatCancelReplaceAndDrainForEveryEvaluator() throws Exception {
        for (var evaluator : BaselineEngine.Evaluator.values()) for (int workers : new int[]{1, 4, 16}) {
            var engine = BaselineEngine.load(evaluator);
            var service = engine.service(workers, 1);
            try {
                service.setBookEnabled(false);
                for (var policy : ClockPolicy.values()) {
                    // Each selection owns a separate service; no mutable policy singleton.
                    try (var selected = engine.withClockPolicy(policy).service(workers, 1)) {
                        selected.setBookEnabled(false);
                        for (int cycle = 0; cycle < 2; cycle++) {
                            var completed = run(selected, new SearchLimits(64, -1, policy.allocateClockMillis(1500, 100, 30), false, true));
                            assertNull(completed.failure()); assertTrue(completed.hasMove());
                            selected.invalidate(SearchTermination.NEW_GAME);
                        }
                        var zero = run(selected, new SearchLimits(64, -1, policy.allocateClockMillis(50, 100, 30), false, true));
                        assertEquals(SearchTermination.TIME_LIMIT, zero.termination());
                    }
                }
                var stopped = new CompletableFuture<ManagedSearchResult>(); var board = Board.startingPosition();
                service.start(board, GameHistory.initial(board), new SearchLimits(0, -1, -1, true), SearchObserver.NONE, stopped::complete);
                service.stop(); assertNull(stopped.get(10, TimeUnit.SECONDS).failure());
                // Replace an active request; only the new generation may finish after invalidation.
                service.start(board, GameHistory.initial(board), new SearchLimits(0, -1, -1, true), SearchObserver.NONE, r -> {});
                service.invalidate(SearchTermination.POSITION_CHANGED);
                assertTrue(run(service, new SearchLimits(2, -1, -1, false)).hasMove());
                service.start(board, GameHistory.initial(board), new SearchLimits(0, -1, -1, true), SearchObserver.NONE, r -> {});
            } finally { service.close(); }
            assertTrue(service.isTerminated());
        }
    }
    private static ManagedSearchResult run(SearchLifecycleService service, SearchLimits limits) throws Exception {
        var result = new CompletableFuture<ManagedSearchResult>(); var board = Board.startingPosition();
        service.start(board, GameHistory.initial(board), limits, SearchObserver.NONE, result::complete);
        return result.get(10, TimeUnit.SECONDS);
    }
}
