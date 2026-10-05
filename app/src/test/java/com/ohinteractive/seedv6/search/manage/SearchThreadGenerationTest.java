package com.ohinteractive.seedv6.search.manage;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SearchThreadGenerationTest {
    @Test void invalidationAndReplacementHideOldParticipantsBeforeTheyDrain() throws Exception {
        var fixture = new BlockingSearch();
        var board = Board.startingPosition();
        var history = GameHistory.initial(board);
        var finished = new CountDownLatch(1);
        try (var service = new SearchLifecycleService(TimeSource.SYSTEM, () -> fixture)) {
            long old = service.start(board, history, new SearchLimits(1, -1, -1, false), result -> fail("stale callback"));
            try {
                assertTrue(fixture.entered[0].await(5, TimeUnit.SECONDS));
                assertEquals(2, service.activeSearchThreads(old));
                assertEquals(0, service.activeSearchThreads(old + 1));
                service.invalidate(SearchTermination.POSITION_CHANGED);
                assertEquals(0, service.activeSearchThreads(old));
                long next = service.start(board, history, new SearchLimits(1, -1, -1, false), result -> finished.countDown());
                assertEquals(0, service.activeSearchThreads(next), "Old executing workers belong to another generation");
                fixture.release[0].countDown();
                assertTrue(fixture.entered[1].await(5, TimeUnit.SECONDS));
                assertEquals(2, service.activeSearchThreads(next));
                assertEquals(0, service.activeSearchThreads(old));
                fixture.release[1].countDown();
                assertTrue(finished.await(5, TimeUnit.SECONDS));
                assertEquals(0, service.activeSearchThreads(next));
                assertNull(service.lastFailure());
            } finally { for (var release : fixture.release) release.countDown(); }
        }
    }

    private static final class BlockingSearch implements SingleDepthSearch {
        final CountDownLatch[] entered = {new CountDownLatch(1), new CountDownLatch(1)};
        final CountDownLatch[] release = {new CountDownLatch(1), new CountDownLatch(1)};
        final AtomicInteger calls = new AtomicInteger();
        volatile int active;
        public int maxSupportedDepth() { return 10; }
        public int activeSearchThreads() { return active; }
        public SearchResult search(SearchRequest request) {
            int call = calls.getAndIncrement(); active = 2; entered[call].countDown();
            try {
                if (!release[call].await(5, TimeUnit.SECONDS)) throw new AssertionError("fixture timeout");
                return new SearchResult(0, false, 0, request.depth(), 0, 0, true);
            } catch (InterruptedException e) { throw new AssertionError(e); }
            finally { active = 0; }
        }
    }
}
