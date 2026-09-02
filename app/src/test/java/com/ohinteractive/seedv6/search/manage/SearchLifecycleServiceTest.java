package com.ohinteractive.seedv6.search.manage;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.common.SearchResult;
import com.ohinteractive.seedv6.search.common.SearchTermination;
import com.ohinteractive.seedv6.search.common.TimeSource;
import com.ohinteractive.seedv6.search.flat.FlatNegamax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchLifecycleServiceTest {

    @Test
    void normalDepthCompletesAsynchronouslyWithExactWs1Result() throws Exception {
        final AtomicReference<ManagedSearchResult> published = new AtomicReference<>();
        final CountDownLatch done = new CountDownLatch(1);
        try(SearchLifecycleService service = new SearchLifecycleService()) {
            final long generation = start(service, limits(2, -1L), result -> {
                published.set(result);
                done.countDown();
            });

            assertTrue(done.await(5L, TimeUnit.SECONDS));
            assertEquals(generation, published.get().generation());
            assertEquals(SearchTermination.COMPLETED, published.get().termination());
            assertTrue(published.get().lastCompletedResult().completed());
            assertEquals(420L, published.get().lastCompletedResult().nodes());
            assertEquals(420L, published.get().nodes());
            assertFalse(service.isSearching());
        }
    }

    @Test
    void explicitAndRepeatedStopPublishExactlyOneLegalFallback() throws Exception {
        final BlockingFlat search = new BlockingFlat();
        final AtomicInteger publications = new AtomicInteger();
        final AtomicReference<ManagedSearchResult> result = new AtomicReference<>();
        final CountDownLatch done = new CountDownLatch(1);
        try(SearchLifecycleService service = service(search)) {
            start(service, limits(4, -1L), publication -> {
                publications.incrementAndGet();
                result.set(publication);
                done.countDown();
            });
            assertTrue(search.started.await(5L, TimeUnit.SECONDS));

            service.stop();
            service.stop();
            search.release.countDown();

            assertTrue(done.await(5L, TimeUnit.SECONDS));
            assertEquals(1, publications.get());
            assertEquals(SearchTermination.STOPPED, result.get().termination());
            assertTrue(result.get().hasMove());
            assertNull(result.get().lastCompletedResult());
        }
    }

    @Test
    void replacementSuppressesStaleResultAndRunsOnlyNewestPendingGeneration() throws Exception {
        final BlockingFlat search = new BlockingFlat();
        final AtomicInteger stalePublications = new AtomicInteger();
        final AtomicReference<ManagedSearchResult> newest = new AtomicReference<>();
        final CountDownLatch newestDone = new CountDownLatch(1);
        try(SearchLifecycleService service = service(search)) {
            final long first = start(service, limits(4, -1L), result -> stalePublications.incrementAndGet());
            assertTrue(search.started.await(5L, TimeUnit.SECONDS));
            final long second = start(
                service, limits(2, -1L), result -> stalePublications.incrementAndGet()
            );
            final long third = start(service, limits(1, -1L), result -> {
                newest.set(result);
                newestDone.countDown();
            });
            assertTrue(second > first);
            assertTrue(third > second);
            search.release.countDown();

            assertTrue(newestDone.await(5L, TimeUnit.SECONDS));
            assertEquals(0, stalePublications.get());
            assertEquals(third, newest.get().generation());
            assertEquals(SearchTermination.COMPLETED, newest.get().termination());
            assertEquals(2, search.calls.get());
        }
    }

    @Test
    void stopRacingCompletedTraversalCannotDuplicatePublication() throws Exception {
        final CompletingFlat search = new CompletingFlat();
        final AtomicInteger publications = new AtomicInteger();
        final AtomicReference<ManagedSearchResult> published = new AtomicReference<>();
        final CountDownLatch done = new CountDownLatch(1);
        try(SearchLifecycleService service = service(search)) {
            start(service, limits(1, -1L), result -> {
                publications.incrementAndGet();
                published.set(result);
                done.countDown();
            });
            assertTrue(search.traversalCompleted.await(5L, TimeUnit.SECONDS));

            service.stop();
            service.stop();
            search.allowReturn.countDown();

            assertTrue(done.await(5L, TimeUnit.SECONDS));
            assertEquals(1, publications.get());
            assertTrue(published.get().lastCompletedResult().completed());
            assertEquals(SearchTermination.STOPPED, published.get().termination());
        }
    }

    @Test
    void positionAndNewGameInvalidationSuppressActivePublication() throws Exception {
        assertInvalidationSuppresses(SearchTermination.POSITION_CHANGED);
        assertInvalidationSuppresses(SearchTermination.NEW_GAME);
    }

    @Test
    void cumulativeProgressiveNodeBudgetHonorsBelowExactAndExactBoundaries() throws Exception {
        final ManagedSearchResult below = managedNodes(19L);
        assertEquals(SearchTermination.NODE_LIMIT, below.termination());
        assertEquals(19L, below.nodes());
        assertNull(below.lastCompletedResult());
        assertTrue(below.hasMove());

        final ManagedSearchResult exact = managedNodes(20L);
        assertEquals(SearchTermination.NODE_LIMIT, exact.termination());
        assertEquals(20L, exact.nodes());
        assertNotNull(exact.lastCompletedResult());
        assertEquals(1, exact.lastCompletedResult().depth());
        assertEquals(20L, exact.lastCompletedResult().nodes());

        final ManagedSearchResult above = managedNodes(21L);
        assertEquals(SearchTermination.NODE_LIMIT, above.termination());
        assertEquals(21L, above.nodes());
        assertEquals(1, above.lastCompletedResult().depth());
    }

    @Test
    void combinedDepthAndNodesUseOneCumulativeBudgetAcrossIterations() throws Exception {
        final ManagedSearchResult below = managed(new SearchLimits(2, 439L, -1L, false));
        assertEquals(SearchTermination.NODE_LIMIT, below.termination());
        assertEquals(439L, below.nodes());
        assertEquals(1, below.lastCompletedResult().depth());

        final ManagedSearchResult exact = managed(new SearchLimits(2, 440L, -1L, false));
        assertEquals(SearchTermination.COMPLETED, exact.termination());
        assertEquals(440L, exact.nodes());
        assertEquals(2, exact.lastCompletedResult().depth());
        assertEquals(420L, exact.lastCompletedResult().nodes());

        final ManagedSearchResult above = managed(new SearchLimits(2, 441L, -1L, false));
        assertEquals(SearchTermination.COMPLETED, above.termination());
        assertEquals(440L, above.nodes());
    }

    @Test
    void zeroTimeBudgetUsesFallbackWithoutEnteringNodes() throws Exception {
        final AtomicReference<ManagedSearchResult> result = new AtomicReference<>();
        final CountDownLatch done = new CountDownLatch(1);
        final TimeSource fixed = () -> 42L;
        try(SearchLifecycleService service = new SearchLifecycleService(fixed, FlatNegamax::new)) {
            final long[] board = Board.startingPosition();
            service.start(
                board, GameHistory.initial(board), new SearchLimits(0, -1L, 0L, false),
                publication -> {
                    result.set(publication);
                    done.countDown();
                }
            );
            assertTrue(done.await(5L, TimeUnit.SECONDS));
        }

        assertEquals(SearchTermination.TIME_LIMIT, result.get().termination());
        assertEquals(0L, result.get().nodes());
        assertTrue(result.get().hasMove());
        assertNull(result.get().lastCompletedResult());
    }

    @Test
    void terminalInfiniteSearchCompletesImmediatelyWithNoMove() throws Exception {
        final long[] mate = Board.fromFen("7k/6Q1/5K2/8/8/8/8/8 b - - 0 1");
        final AtomicReference<ManagedSearchResult> result = new AtomicReference<>();
        final CountDownLatch done = new CountDownLatch(1);
        try(SearchLifecycleService service = new SearchLifecycleService()) {
            service.start(
                mate, GameHistory.initial(mate), new SearchLimits(0, -1L, -1L, true),
                publication -> {
                    result.set(publication);
                    done.countDown();
                }
            );
            assertTrue(done.await(5L, TimeUnit.SECONDS));
        }

        assertEquals(SearchTermination.COMPLETED, result.get().termination());
        assertFalse(result.get().hasMove());
        assertEquals(0L, result.get().bestMove());
        assertTrue(result.get().lastCompletedResult().completed());
    }

    @Test
    void workerFailurePublishesSafeFallbackAndDoesNotKillReusableWorker() throws Exception {
        final RuntimeException expected = new RuntimeException("expected test failure");
        final FailOnceFlat search = new FailOnceFlat(expected);
        final AtomicReference<ManagedSearchResult> first = new AtomicReference<>();
        final AtomicReference<ManagedSearchResult> second = new AtomicReference<>();
        final CountDownLatch firstDone = new CountDownLatch(1);
        final CountDownLatch secondDone = new CountDownLatch(1);
        try(SearchLifecycleService service = service(search)) {
            start(service, limits(1, -1L), result -> {
                first.set(result);
                firstDone.countDown();
            });
            assertTrue(firstDone.await(5L, TimeUnit.SECONDS));
            assertEquals(SearchTermination.FAILURE, first.get().termination());
            assertTrue(first.get().hasMove());
            assertSame(expected, first.get().failure());
            assertSame(expected, service.lastFailure());

            start(service, limits(1, -1L), result -> {
                second.set(result);
                secondDone.countDown();
            });
            assertTrue(secondDone.await(5L, TimeUnit.SECONDS));
            assertEquals(SearchTermination.COMPLETED, second.get().termination());
            assertTrue(second.get().hasMove());
        }
    }

    @Test
    void listenerFailureIsContainedAndWorkerAcceptsLaterSearch() throws Exception {
        final CountDownLatch firstCalled = new CountDownLatch(1);
        final CountDownLatch secondCalled = new CountDownLatch(1);
        try(SearchLifecycleService service = new SearchLifecycleService()) {
            start(service, limits(1, -1L), result -> {
                firstCalled.countDown();
                throw new RuntimeException("listener");
            });
            assertTrue(firstCalled.await(5L, TimeUnit.SECONDS));
            start(service, limits(1, -1L), result -> secondCalled.countDown());
            assertTrue(secondCalled.await(5L, TimeUnit.SECONDS));
            assertNotNull(service.lastFailure());
        }
    }

    @Test
    void closeTerminatesOwnedWorkerAndRejectsFurtherSearches() {
        final SearchLifecycleService service = new SearchLifecycleService();
        service.close();

        assertTrue(service.isTerminated());
        final long[] board = Board.startingPosition();
        boolean rejected = false;
        try {
            service.start(board, GameHistory.initial(board), limits(1, -1L), result -> {});
        } catch(IllegalStateException expected) {
            rejected = true;
        }
        assertTrue(rejected);
    }

    private static void assertInvalidationSuppresses(SearchTermination reason) throws Exception {
        final BlockingFlat search = new BlockingFlat();
        final AtomicInteger publications = new AtomicInteger();
        try(SearchLifecycleService service = service(search)) {
            start(service, limits(4, -1L), result -> publications.incrementAndGet());
            assertTrue(search.started.await(5L, TimeUnit.SECONDS));
            service.invalidate(reason);
            search.release.countDown();
            assertTrue(search.returned.await(5L, TimeUnit.SECONDS));
            assertEquals(0, publications.get());
            assertFalse(service.isSearching());
        }
    }

    private static ManagedSearchResult managedNodes(long nodes) throws Exception {
        return managed(new SearchLimits(0, nodes, -1L, false));
    }

    private static ManagedSearchResult managed(SearchLimits limits) throws Exception {
        final AtomicReference<ManagedSearchResult> result = new AtomicReference<>();
        final CountDownLatch done = new CountDownLatch(1);
        try(SearchLifecycleService service = new SearchLifecycleService()) {
            start(service, limits, publication -> {
                result.set(publication);
                done.countDown();
            });
            assertTrue(done.await(5L, TimeUnit.SECONDS));
        }
        return result.get();
    }

    private static SearchLifecycleService service(FlatNegamax search) {
        return new SearchLifecycleService(TimeSource.SYSTEM, () -> search);
    }

    private static long start(
        SearchLifecycleService service, SearchLimits limits,
        SearchLifecycleService.Listener listener
    ) {
        final long[] board = Board.startingPosition();
        return service.start(board, GameHistory.initial(board), limits, listener);
    }

    private static SearchLimits limits(int depth, long nodes) {
        return new SearchLimits(depth, nodes, -1L, false);
    }

    private static class BlockingFlat extends FlatNegamax {
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final CountDownLatch returned = new CountDownLatch(1);
        final AtomicInteger calls = new AtomicInteger();

        @Override
        public SearchResult search(SearchRequest request) {
            if(calls.incrementAndGet() == 1) {
                started.countDown();
                try {
                    release.await();
                } catch(InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
                if(request.control().termination() != SearchTermination.NONE) {
                    returned.countDown();
                    return new SearchResult(0L, false, 0, request.depth(), 0L, 20, false);
                }
            }
            final SearchResult result = super.search(request);
            returned.countDown();
            return result;
        }
    }

    private static final class FailOnceFlat extends FlatNegamax {
        private final RuntimeException failure;
        private int calls;

        FailOnceFlat(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public SearchResult search(SearchRequest request) {
            if(calls ++ == 0) throw failure;
            return super.search(request);
        }
    }

    private static final class CompletingFlat extends FlatNegamax {
        final CountDownLatch traversalCompleted = new CountDownLatch(1);
        final CountDownLatch allowReturn = new CountDownLatch(1);

        @Override
        public SearchResult search(SearchRequest request) {
            final SearchResult result = super.search(request);
            traversalCompleted.countDown();
            try {
                allowReturn.await();
            } catch(InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            return result;
        }
    }
}
