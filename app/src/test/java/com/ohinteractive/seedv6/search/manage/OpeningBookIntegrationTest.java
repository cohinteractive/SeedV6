package com.ohinteractive.seedv6.search.manage;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.book.Book;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.ExactSearchAdapter;
import com.ohinteractive.seedv6.search.driver.SearchDriver;
import com.ohinteractive.seedv6.search.tablebase.RootTablebase;

class OpeningBookIntegrationTest {
    @Test void bookHitNeverInvokesSearchAndMissStillSearches() throws Exception {
        var calls = new AtomicInteger();
        var probes = new AtomicInteger();
        RootTablebase tablebase = (b, h, c) -> { probes.incrementAndGet(); return null; };
        try (var service = new SearchLifecycleService(TimeSource.SYSTEM, () -> counting(calls),
                tablebase, Book.bundled(), new Random(32))) {
            var hit = run(service, Board.startingPosition(), depthOne());
            assertEquals(SearchTermination.BOOK, hit.termination());
            assertTrue(hit.hasMove()); assertNull(hit.lastCompletedResult());
            assertEquals(0, hit.nodes()); assertNull(hit.failure());
            assertEquals(0, calls.get()); assertEquals(0, probes.get());
            var miss = run(service, Board.fromFen("4k3/8/8/8/8/8/4P3/4K3 w - - 0 1"), depthOne());
            assertEquals(SearchTermination.COMPLETED, miss.termination());
            assertNotNull(miss.lastCompletedResult()); assertTrue(miss.nodes() > 0);
            assertEquals(1, calls.get()); assertEquals(1, probes.get());
            assertEquals(SearchTermination.BOOK, run(service, Board.startingPosition(), depthOne()).termination());
            assertEquals(1, calls.get());
        }
    }

    @Test void explicitDisableAndInfiniteAnalysisRemainSearches() throws Exception {
        var calls = new AtomicInteger();
        try (var service = new SearchLifecycleService(TimeSource.SYSTEM, () -> counting(calls),
                RootTablebase.NONE, Book.bundled(), new Random(1))) {
            service.setBookEnabled(false);
            assertEquals(SearchTermination.COMPLETED, run(service, Board.startingPosition(), depthOne()).termination());
            service.setBookEnabled(true);
            var board = Board.startingPosition();
            var done = new CountDownLatch(1);
            var result = new AtomicReference<ManagedSearchResult>();
            service.start(board, GameHistory.initial(board), new SearchLimits(0, -1, -1, true),
                    new SearchObserver() {
                        @Override public void onIterationCompleted(IterationSnapshot snapshot) { service.stop(); }
                    }, r -> { result.set(r); done.countDown(); });
            assertTrue(done.await(5, TimeUnit.SECONDS));
            assertEquals(SearchTermination.STOPPED, result.get().termination());
            assertNotNull(result.get().lastCompletedResult()); assertEquals(2, calls.get());
        }
    }

    @Test void ruleDrawAtBookPositionRetainsNoMoveSemantics() throws Exception {
        var calls = new AtomicInteger();
        try (var service = new SearchLifecycleService(TimeSource.SYSTEM, () -> counting(calls),
                RootTablebase.NONE, Book.bundled(), new Random(1))) {
            long[] fifty = Board.fromFen(Board.FEN_STARTING_POSITION.replace("0 1", "100 80"));
            var result = run(service, fifty, depthOne());
            assertFalse(result.hasMove()); assertEquals(SearchTermination.COMPLETED, result.termination());
            long[] repeated = Board.fromFen(Board.FEN_STARTING_POSITION.replace("0 1", "8 5"));
            var history = GameHistory.builder(repeated).appendPosition(repeated).appendPosition(repeated).snapshot();
            result = run(service, repeated, history, depthOne());
            assertFalse(result.hasMove()); assertEquals(SearchTermination.COMPLETED, result.termination());
            assertEquals(2, calls.get());
        }
    }

    @Test void rawDriversUsedByTrainingAndValidationNeverConsultTheBook() throws Exception {
        var calls = new AtomicInteger();
        try (var driver = new SearchDriver(counting(calls))) {
            var result = driver.search(new SearchRequest(Board.startingPosition(), 1));
            assertTrue(result.targetDepthCompleted()); assertEquals(1, calls.get());
            assertNotNull(result.lastCompletedResult()); assertTrue(result.lastCompletedResult().nodes() > 0);
        }
        // Injecting a search facility also preserves the existing search-only managed API.
        try (var service = new SearchLifecycleService(TimeSource.SYSTEM, () -> counting(calls))) {
            assertEquals(SearchTermination.COMPLETED, run(service, Board.startingPosition(), depthOne()).termination());
            assertEquals(2, calls.get());
        }
    }

    @Test void bookFailureCannotPublishPreviousSearchMove() throws Exception {
        var calls = new AtomicInteger();
        Random broken = new Random() { @Override public int nextInt(int bound) { throw new IllegalStateException("fixture"); } };
        try (var service = new SearchLifecycleService(TimeSource.SYSTEM, () -> counting(calls), RootTablebase.NONE, Book.bundled(), broken)) {
            var miss = run(service, Board.fromFen("4k3/8/8/8/8/8/4P3/4K3 w - - 0 1"), depthOne());
            assertTrue(miss.hasMove());
            var failed = run(service, Board.startingPosition(), depthOne());
            assertEquals(SearchTermination.FAILURE, failed.termination());
            assertFalse(failed.hasMove()); assertNull(failed.lastCompletedResult()); assertNotNull(failed.failure());
        }
    }

    @Test void invalidationSuppressesBookPublicationWhileChoiceIsInFlight() throws Exception {
        var choosing = new CountDownLatch(1); var release = new CountDownLatch(1);
        var publications = new AtomicInteger(); var calls = new AtomicInteger();
        Random blocked = new Random() {
            @Override public int nextInt(int bound) {
                choosing.countDown();
                try { assertTrue(release.await(5, TimeUnit.SECONDS)); }
                catch (InterruptedException e) { throw new AssertionError(e); }
                return 0;
            }
        };
        try (var service = new SearchLifecycleService(TimeSource.SYSTEM, () -> counting(calls), RootTablebase.NONE, Book.bundled(), blocked)) {
            long[] board = Board.startingPosition();
            service.start(board, GameHistory.initial(board), depthOne(), r -> publications.incrementAndGet());
            try {
                assertTrue(choosing.await(5, TimeUnit.SECONDS));
                service.invalidate(SearchTermination.POSITION_CHANGED);
            } finally { release.countDown(); }
            assertEquals(SearchTermination.COMPLETED,
                    run(service, Board.fromFen("4k3/8/8/8/8/8/4P3/4K3 w - - 0 1"), depthOne()).termination());
            assertEquals(0, publications.get()); assertEquals(1, calls.get());
        }
    }

    private static SingleDepthSearch counting(AtomicInteger calls) {
        return new SingleDepthSearch() {
            private final ExactSearchAdapter delegate = new ExactSearchAdapter((b, p) -> 0, null);
            @Override public int maxSupportedDepth() { return delegate.maxSupportedDepth(); }
            @Override public SearchResult search(SearchRequest request) { calls.incrementAndGet(); return delegate.search(request); }
            @Override public void close() { delegate.close(); }
        };
    }
    private static SearchLimits depthOne() { return new SearchLimits(1, -1, -1, false); }
    private static ManagedSearchResult run(SearchLifecycleService service, long[] board, SearchLimits limits) throws Exception {
        return run(service, board, GameHistory.initial(board), limits);
    }
    private static ManagedSearchResult run(SearchLifecycleService service, long[] board, GameHistory history, SearchLimits limits) throws Exception {
        var result = new AtomicReference<ManagedSearchResult>(); var done = new CountDownLatch(1);
        service.start(board, history, limits, r -> { result.set(r); done.countDown(); });
        assertTrue(done.await(5, TimeUnit.SECONDS)); return result.get();
    }
}
