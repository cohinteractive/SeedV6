package com.ohinteractive.seedv6.search.exact;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.ExactSearchAdapter;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.tt.TTable;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SearchThreadTelemetryTest {
    @Test void realSmpParticipantsEnterDrainAndResetOnCancelledAndSubsequentSearch() throws Exception {
        var caller = Executors.newSingleThreadExecutor();
        var board = Board.startingPosition();
        var control = SearchControl.controlled(-1, System.nanoTime(), TimeUnit.SECONDS.toNanos(5), TimeSource.SYSTEM);
        try (var search = new ParallelSearch(4, SearchEvaluation.handcrafted(), new TTable(1))) {
            assertEquals(0, search.activeSearchThreads());
            var future = caller.submit(() -> search.search(new SearchRequest(board, GameHistory.initial(board), 10, control)));
            try {
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                int maximum = 0;
                while (!future.isDone() && System.nanoTime() < deadline) {
                    int count = search.activeSearchThreads();
                    assertTrue(count >= 0 && count <= 4);
                    maximum = Math.max(maximum, count);
                    if (maximum == 4) break;
                    Thread.sleep(2);
                }
                assertEquals(4, maximum, "All configured participants performed real search work");
                System.out.println("SMP telemetry smoke: observed 4/4 live participants");
            } finally {
                control.request(SearchTermination.STOPPED);
                future.get(10, TimeUnit.SECONDS);
            }
            assertEquals(0, search.activeSearchThreads());
            assertTrue(search.search(new SearchRequest(board, 2)).completed());
            assertEquals(0, search.activeSearchThreads());
        } finally {
            caller.shutdownNow();
            assertTrue(caller.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @Test void singleOwnerIsVisibleAndAlwaysClearsIncludingFailure() throws Exception {
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        var caller = Executors.newSingleThreadExecutor();
        var evaluator = new ExactEvaluator() {
            public void initialize(long[] board) {
                entered.countDown();
                try { if (!release.await(5, TimeUnit.SECONDS)) throw new AssertionError("release timeout"); }
                catch (InterruptedException e) { throw new AssertionError(e); }
            }
            public int evaluate(long[] board, int ply) { return 0; }
        };
        try (var search = new ExactSearchAdapter(evaluator, new TTable(1))) {
            var future = caller.submit(() -> search.search(new SearchRequest(Board.startingPosition(), 1)));
            try {
                assertTrue(entered.await(5, TimeUnit.SECONDS));
                assertEquals(1, search.activeSearchThreads());
            } finally { release.countDown(); future.get(10, TimeUnit.SECONDS); }
            assertEquals(0, search.activeSearchThreads());
        } finally { release.countDown(); caller.shutdownNow(); }
        var failing = new ExactSearchAdapter(new ExactEvaluator() {
            public void initialize(long[] board) { throw new IllegalStateException("fixture"); }
            public int evaluate(long[] board, int ply) { return 0; }
        }, new TTable(1));
        assertThrows(IllegalStateException.class, () -> failing.search(new SearchRequest(Board.startingPosition(), 1)));
        assertEquals(0, failing.activeSearchThreads());
    }
}
