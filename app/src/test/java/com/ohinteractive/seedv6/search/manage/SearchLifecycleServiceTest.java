package com.ohinteractive.seedv6.search.manage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.common.SearchResult;
import com.ohinteractive.seedv6.search.common.IterationSnapshot;
import com.ohinteractive.seedv6.search.common.SearchObserver;
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
    void normalDepthCompletesAsynchronouslyWithIterativeWs10Result() throws Exception {
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
            assertEquals(82L, published.get().lastCompletedResult().nodes());
            assertEquals(82L, published.get().nodes());
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
        final ManagedSearchResult below = managed(new SearchLimits(2, 81L, -1L, false));
        assertEquals(SearchTermination.NODE_LIMIT, below.termination());
        assertEquals(81L, below.nodes());
        assertEquals(1, below.lastCompletedResult().depth());

        final ManagedSearchResult exact = managed(new SearchLimits(2, 82L, -1L, false));
        assertEquals(SearchTermination.COMPLETED, exact.termination());
        assertEquals(82L, exact.nodes());
        assertEquals(2, exact.lastCompletedResult().depth());
        assertEquals(82L, exact.lastCompletedResult().nodes());

        final ManagedSearchResult above = managed(new SearchLimits(2, 83L, -1L, false));
        assertEquals(SearchTermination.COMPLETED, above.termination());
        assertEquals(82L, above.nodes());
    }

    @Test
    void zeroTimeBudgetUsesFallbackWithoutEnteringNodes() throws Exception {
        final AtomicReference<ManagedSearchResult> result = new AtomicReference<>();
        final AtomicInteger iterations = new AtomicInteger();
        final CountDownLatch done = new CountDownLatch(1);
        final TimeSource fixed = () -> 42L;
        try(SearchLifecycleService service = new SearchLifecycleService(fixed, FlatNegamax::new)) {
            final long[] board = Board.startingPosition();
            service.start(
                board, GameHistory.initial(board), new SearchLimits(0, -1L, 0L, false),
                new SearchObserver() {
                    @Override
                    public void onIterationCompleted(IterationSnapshot snapshot) {
                        iterations.incrementAndGet();
                    }
                },
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
        assertEquals(0, iterations.get());
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
    void laterIterationFailureRetainsAlreadyCompletedIteration() throws Exception {
        final RuntimeException expected = new RuntimeException("depth two failure");
        final FailAtSecondDepth search = new FailAtSecondDepth(expected);
        final AtomicReference<ManagedSearchResult> result = new AtomicReference<>();
        final List<Integer> depths = new ArrayList<>();
        final CountDownLatch done = new CountDownLatch(1);
        try(SearchLifecycleService service = service(search)) {
            final long[] board = Board.startingPosition();
            service.start(
                board, GameHistory.initial(board), limits(3, -1L),
                iterationObserver(depths), publication -> {
                    result.set(publication);
                    done.countDown();
                }
            );
            assertTrue(done.await(5L, TimeUnit.SECONDS));
        }
        assertEquals(List.of(1), depths);
        assertEquals(SearchTermination.FAILURE, result.get().termination());
        assertSame(expected, result.get().failure());
        assertEquals(1, result.get().lastCompletedResult().depth());
        assertEquals(result.get().lastCompletedResult().bestMove(), result.get().bestMove());
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
    void completedIterationsAreWorkerOrderedAndObserverFailureIsContained() throws Exception {
        final List<Integer> depths = new ArrayList<>();
        final List<String> threads = new ArrayList<>();
        final AtomicReference<ManagedSearchResult> result = new AtomicReference<>();
        final CountDownLatch done = new CountDownLatch(1);
        try(SearchLifecycleService service = new SearchLifecycleService()) {
            final long[] board = Board.startingPosition();
            service.start(
                board, GameHistory.initial(board), limits(3, -1L),
                new SearchObserver() {
                    @Override
                    public void onIterationCompleted(IterationSnapshot snapshot) {
                        depths.add(snapshot.depth());
                        threads.add(Thread.currentThread().getName());
                        if(snapshot.depth() == 1) throw new RuntimeException("observer");
                    }
                },
                publication -> {
                    result.set(publication);
                    done.countDown();
                }
            );
            assertTrue(done.await(5L, TimeUnit.SECONDS));
            assertEquals(SearchTermination.COMPLETED, result.get().termination());
            assertEquals(List.of(1, 2, 3), depths);
            assertTrue(threads.stream().allMatch("seedv6-search-worker"::equals));
            assertNotNull(service.lastFailure());
        }
    }

    @Test
    void stopDuringSecondIterationRetainsDepthOneAndPublishesNoPartialDepth() throws Exception {
        final BlockingSecondDepth search = new BlockingSecondDepth();
        final List<Integer> depths = new ArrayList<>();
        final AtomicReference<SearchResult> completed = new AtomicReference<>();
        final AtomicReference<ManagedSearchResult> result = new AtomicReference<>();
        final CountDownLatch done = new CountDownLatch(1);
        try(SearchLifecycleService service = service(search)) {
            final long[] board = Board.startingPosition();
            service.start(
                board, GameHistory.initial(board), limits(4, -1L),
                new SearchObserver() {
                    @Override
                    public void onIterationCompleted(IterationSnapshot snapshot) {
                        depths.add(snapshot.depth());
                        completed.set(snapshot.result());
                    }
                },
                publication -> {
                    result.set(publication);
                    done.countDown();
                }
            );
            assertTrue(search.secondStarted.await(5L, TimeUnit.SECONDS));
            service.stop();
            search.release.countDown();
            assertTrue(done.await(5L, TimeUnit.SECONDS));
            assertEquals(List.of(1), depths);
            assertEquals(SearchTermination.STOPPED, result.get().termination());
            assertEquals(1, result.get().lastCompletedResult().depth());
            assertSame(completed.get(), result.get().lastCompletedResult());
        }
    }

    @Test
    void replacementSuppressesOldIterationAndResultAfterGenerationChanges() throws Exception {
        final BlockingSecondDepth search = new BlockingSecondDepth();
        final List<Integer> oldDepths = new ArrayList<>();
        final List<Integer> newDepths = new ArrayList<>();
        final AtomicInteger oldResults = new AtomicInteger();
        final AtomicReference<ManagedSearchResult> newest = new AtomicReference<>();
        final CountDownLatch done = new CountDownLatch(1);
        final AtomicReference<ManagedSearchResult> afterNewGame = new AtomicReference<>();
        final CountDownLatch afterNewGameDone = new CountDownLatch(1);
        try(SearchLifecycleService service = service(search)) {
            final long[] board = Board.startingPosition();
            service.start(
                board, GameHistory.initial(board), limits(4, -1L),
                iterationObserver(oldDepths), true, ignored -> oldResults.incrementAndGet()
            );
            assertTrue(search.secondStarted.await(5L, TimeUnit.SECONDS));
            final long generation = service.start(
                board, GameHistory.initial(board), limits(2, -1L),
                iterationObserver(newDepths), true, publication -> {
                    newest.set(publication);
                    done.countDown();
                }
            );
            search.release.countDown();
            assertTrue(done.await(5L, TimeUnit.SECONDS));
            assertEquals(List.of(1), oldDepths);
            assertEquals(List.of(1, 2), newDepths);
            assertEquals(0, oldResults.get());
            assertEquals(generation, newest.get().generation());
            assertTrue(newest.get().diagnostics().enabled());
            assertEquals(2L, newest.get().diagnostics().iteration().completedIterations());
            assertEquals(2, newest.get().diagnostics().iteration().deepestCompletedDepth());

            service.invalidate(SearchTermination.NEW_GAME);
            service.start(
                board, GameHistory.initial(board), limits(1, -1L),
                SearchObserver.NONE, true, publication -> {
                    afterNewGame.set(publication);
                    afterNewGameDone.countDown();
                }
            );
            assertTrue(afterNewGameDone.await(5L, TimeUnit.SECONDS));
            assertEquals(1L,
                afterNewGame.get().diagnostics().iteration().completedIterations());
            assertEquals(1,
                afterNewGame.get().diagnostics().iteration().deepestCompletedDepth());
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

    private static SearchObserver iterationObserver(List<Integer> depths) {
        return new SearchObserver() {
            @Override
            public void onIterationCompleted(IterationSnapshot snapshot) {
                depths.add(snapshot.depth());
            }
        };
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

    private static final class FailAtSecondDepth extends FlatNegamax {
        private final RuntimeException failure;

        FailAtSecondDepth(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public SearchResult search(SearchRequest request) {
            if(request.depth() == 2) throw failure;
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

    private static final class BlockingSecondDepth extends FlatNegamax {
        final CountDownLatch secondStarted = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        private boolean blocked;

        @Override
        public SearchResult search(SearchRequest request) {
            if(request.depth() == 2 && !blocked) {
                blocked = true;
                secondStarted.countDown();
                try {
                    release.await();
                } catch(InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            }
            return super.search(request);
        }
    }
}
