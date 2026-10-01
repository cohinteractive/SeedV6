package com.ohinteractive.seedv6.search.manage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.common.SearchControl;
import com.ohinteractive.seedv6.search.common.SearchResult;
import com.ohinteractive.seedv6.search.common.IterationSnapshot;
import com.ohinteractive.seedv6.search.common.SearchObserver;
import com.ohinteractive.seedv6.search.common.SearchTermination;
import com.ohinteractive.seedv6.search.common.TimeSource;
import com.ohinteractive.seedv6.search.common.SingleDepthSearch;
import com.ohinteractive.seedv6.search.driver.ExactSearchAdapter;
import com.ohinteractive.seedv6.search.exact.ExactEvaluator;
import com.ohinteractive.seedv6.search.flat.FlatNegamax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchLifecycleServiceTest {

    @Test
    void closeWaitTimeoutDoesNotCloseSearchUntilItsOwnerDrains() throws Exception {
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var closes = new AtomicInteger();
        var owner = new AtomicReference<Thread>();
        var closer = new AtomicReference<Thread>();
        SingleDepthSearch blocked = new SingleDepthSearch() {
            @Override public int maxSupportedDepth() { return 256; }
            @Override public SearchResult search(SearchRequest request) {
                owner.set(Thread.currentThread());
                entered.countDown();
                // Deliberately model a dependency that has not returned on interrupt.
                while(release.getCount() != 0) {
                    try { release.await(); } catch(InterruptedException ignored) {}
                }
                return new FlatNegamax().search(request);
            }
            @Override public void close() {
                closer.set(Thread.currentThread());
                closes.incrementAndGet();
            }
        };
        var service = new SearchLifecycleService(TimeSource.SYSTEM, () -> blocked);
        try {
            start(service, limits(4, -1L), result -> {});
            assertTrue(entered.await(5L, TimeUnit.SECONDS));
            service.close();
            assertEquals(0, closes.get());
            assertFalse(service.isTerminated());
        } finally {
            release.countDown();
            service.close();
        }
        assertTrue(service.isTerminated());
        assertEquals(1, closes.get());
        assertSame(owner.get(), closer.get());
    }

    @Test
    void completionListenerCanCloseItsOwnerWithoutJoiningItself() throws Exception {
        var serviceRef = new AtomicReference<SearchLifecycleService>();
        var callbackReturned = new CountDownLatch(1);
        var closes = new AtomicInteger();
        var workerThread = new AtomicReference<Thread>();
        var search = new FlatNegamax() {
            @Override public SearchResult search(SearchRequest request) {
                workerThread.set(Thread.currentThread());
                return super.search(request);
            }
            @Override public void close() {
                assertSame(workerThread.get(), Thread.currentThread());
                closes.incrementAndGet();
            }
        };
        try(var service = new SearchLifecycleService(TimeSource.SYSTEM, () -> search)) {
            serviceRef.set(service);
            start(service, limits(1, -1L), result -> {
                serviceRef.get().close();
                callbackReturned.countDown();
            });
            assertTrue(callbackReturned.await(1L, TimeUnit.SECONDS));
        }
        assertEquals(1, closes.get());
        assertTrue(serviceRef.get().isTerminated());
        assertNull(serviceRef.get().lastFailure());
    }

    @Test
    void newGameDrainsActiveExactSearchAndResetsOnItsOwnerBeforeReplacement() throws Exception {
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var done = new CountDownLatch(1);
        var owner = new AtomicReference<Thread>();
        var initializations = new AtomicInteger();
        var resets = new AtomicInteger();
        var stale = new AtomicInteger();
        var published = new AtomicReference<ManagedSearchResult>();
        ExactEvaluator evaluator = new ExactEvaluator() {
            @Override public void initialize(long[] board) {
                owner.compareAndSet(null, Thread.currentThread());
                if(initializations.getAndIncrement() != 0) return;
                entered.countDown();
                try {
                    assertTrue(release.await(5L, TimeUnit.SECONDS));
                } catch(InterruptedException failure) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(failure);
                }
            }
            @Override public int evaluate(long[] board, int ply) {
                return com.ohinteractive.seedv6.core.Eval.evaluate(board);
            }
        };
        var delegate = new ExactSearchAdapter(evaluator);
        SingleDepthSearch observed = new SingleDepthSearch() {
            @Override public SearchResult search(SearchRequest request) { return delegate.search(request); }
            @Override public int maxSupportedDepth() { return delegate.maxSupportedDepth(); }
            @Override public void beginRequest() { delegate.beginRequest(); }
            @Override public void endRequest() { delegate.endRequest(); }
            @Override public void newGame() {
                assertSame(owner.get(), Thread.currentThread());
                delegate.newGame(); // Fails if the preceding exact request has not drained.
                resets.incrementAndGet();
            }
        };
        try(var service = new SearchLifecycleService(TimeSource.SYSTEM, () -> observed)) {
            long[] board = Board.startingPosition();
            service.start(board, GameHistory.initial(board), limits(2, -1L), r -> stale.incrementAndGet());
            assertTrue(entered.await(5L, TimeUnit.SECONDS));
            service.invalidate(SearchTermination.NEW_GAME);
            service.invalidate(SearchTermination.NEW_GAME);
            assertFalse(service.isSearching());
            assertTrue(service.isWorking());
            assertEquals(0, resets.get());
            long generation = service.start(board, GameHistory.initial(board), limits(2, -1L), r -> {
                published.set(r); done.countDown();
            });
            release.countDown();
            assertTrue(done.await(5L, TimeUnit.SECONDS));
            assertEquals(SearchTermination.COMPLETED, published.get().termination());
            assertEquals(generation, published.get().generation());
            assertEquals(2, published.get().lastCompletedResult().depth());
            assertEquals(0, stale.get());
            assertEquals(1, resets.get());
            assertNull(service.lastFailure());
        } finally {
            release.countDown();
        }
    }

    @Test
    void failedNewGameResetPreventsSearchAndIsRetriedBeforeNextRequest() throws Exception {
        var resets = new AtomicInteger();
        var calls = new AtomicInteger();
        var expected = new IllegalStateException("injected reset failure");
        var search = new FlatNegamax() {
            @Override public void newGame() {
                if(resets.incrementAndGet() == 1) throw expected;
                super.newGame();
            }
            @Override public SearchResult search(SearchRequest request) {
                calls.incrementAndGet();
                return super.search(request);
            }
        };
        var result = new AtomicReference<ManagedSearchResult>();
        var first = new CountDownLatch(1);
        var second = new CountDownLatch(1);
        try(var service = service(search)) {
            service.invalidate(SearchTermination.NEW_GAME);
            start(service, limits(1, -1L), r -> { result.set(r); first.countDown(); });
            assertTrue(first.await(5L, TimeUnit.SECONDS));
            assertEquals(SearchTermination.FAILURE, result.get().termination());
            assertSame(expected, result.get().failure());
            assertFalse(result.get().hasMove());
            assertEquals(0, calls.get());
            start(service, limits(1, -1L), r -> { result.set(r); second.countDown(); });
            assertTrue(second.await(5L, TimeUnit.SECONDS));
            assertEquals(SearchTermination.COMPLETED, result.get().termination());
            assertEquals(2, resets.get());
            assertEquals(1, calls.get());
        }
    }

    @Test
    void managedClockPublishesOneCompletedDecisionAndRemainsReusable() throws Exception {
        long[] board = Board.fromFen("8/8/b7/Pp6/8/8/1rk5/K7 w - b6 0 1");
        var result = new AtomicReference<ManagedSearchResult>();
        var publications = new AtomicInteger();
        var done = new CountDownLatch(1);
        try(var service = new SearchLifecycleService(() -> 0L,
                com.ohinteractive.seedv6.search.driver.ExactSearchAdapter::new)) {
            service.start(board, GameHistory.initial(board), new SearchLimits(4, -1L, 100L, false, true), r -> {
                publications.incrementAndGet(); result.set(r); done.countDown();
            });
            assertTrue(done.await(5L, TimeUnit.SECONDS));
            assertEquals(SearchTermination.TIME_ALLOCATION, result.get().termination());
            assertTrue(result.get().hasMove());
            assertEquals(1, result.get().lastCompletedResult().depth());
            assertTrue(result.get().lastCompletedResult().completed());
            assertEquals(1, publications.get());
            var second = new CountDownLatch(1);
            service.start(board, GameHistory.initial(board), new SearchLimits(2, -1L, -1L, false), r -> {
                result.set(r); second.countDown();
            });
            assertTrue(second.await(5L, TimeUnit.SECONDS));
            assertEquals(SearchTermination.COMPLETED, result.get().termination());
            assertEquals(2, result.get().lastCompletedResult().depth());
        }
    }

    @Test
    void normalDepthCompletesAsynchronouslyWithRebuiltExactResult() throws Exception {
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
            assertEquals(depthTwoNodes(), published.get().lastCompletedResult().nodes());
            assertEquals(depthTwoNodes(), published.get().nodes());
            assertFalse(service.isSearching());
        }
    }

    @Test
    void explicitAndRepeatedStopPublishExactlyOneNoResult() throws Exception {
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
            assertFalse(result.get().hasMove());
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
    void recordedNodeLimitRetainsCompletedProofWithItsActualStopReason() throws Exception {
        var search = new CompletingFlat();
        var published = new AtomicReference<ManagedSearchResult>();
        var done = new CountDownLatch(1);
        try(var service = service(search)) {
            start(service, limits(1, 20L), result -> {
                published.set(result); done.countDown();
            });
            assertTrue(search.traversalCompleted.await(5L, TimeUnit.SECONDS));
            // A speculative helper can exhaust admission after another worker completed.
            assertFalse(search.control.get().tryEnterNode());
            search.allowReturn.countDown();
            assertTrue(done.await(5L, TimeUnit.SECONDS));
            assertEquals(SearchTermination.NODE_LIMIT, published.get().termination());
            assertEquals(20L, published.get().nodes());
            assertTrue(published.get().hasMove());
            assertTrue(published.get().lastCompletedResult().completed());
            assertEquals(1, published.get().lastCompletedResult().depth());
        }
    }

    @Test
    void recordedDeadlineRetainsCompletedProofWithItsActualStopReason() throws Exception {
        var search = new CompletingFlat();
        var now = new AtomicLong();
        var published = new AtomicReference<ManagedSearchResult>();
        var done = new CountDownLatch(1);
        try(var service = new SearchLifecycleService(now::get, () -> search)) {
            start(service, new SearchLimits(1, -1L, 100L, false), result -> {
                published.set(result); done.countDown();
            });
            assertTrue(search.traversalCompleted.await(5L, TimeUnit.SECONDS));
            now.set(100_000_000L);
            assertFalse(search.control.get().checkpoint());
            search.allowReturn.countDown();
            assertTrue(done.await(5L, TimeUnit.SECONDS));
            assertEquals(SearchTermination.TIME_LIMIT, published.get().termination());
            assertEquals(20L, published.get().nodes());
            assertTrue(published.get().hasMove());
            assertTrue(published.get().lastCompletedResult().completed());
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
        assertFalse(below.hasMove());

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
        final ManagedSearchResult below = managed(new SearchLimits(2, depthTwoNodes() - 1, -1L, false));
        assertEquals(SearchTermination.NODE_LIMIT, below.termination());
        assertEquals(depthTwoNodes() - 1, below.nodes());
        assertEquals(1, below.lastCompletedResult().depth());

        final ManagedSearchResult exact = managed(new SearchLimits(2, depthTwoNodes(), -1L, false));
        assertEquals(SearchTermination.COMPLETED, exact.termination());
        assertEquals(depthTwoNodes(), exact.nodes());
        assertEquals(2, exact.lastCompletedResult().depth());
        assertEquals(depthTwoNodes(), exact.lastCompletedResult().nodes());

        final ManagedSearchResult above = managed(new SearchLimits(2, depthTwoNodes() + 1, -1L, false));
        assertEquals(SearchTermination.COMPLETED, above.termination());
        assertEquals(depthTwoNodes(), above.nodes());
    }

    @Test
    void zeroTimeBudgetHasNoValidResultWithoutEnteringNodes() throws Exception {
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
        assertFalse(result.get().hasMove());
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
    void workerFailurePublishesNoResultAndDoesNotKillReusableWorker() throws Exception {
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
            assertFalse(first.get().hasMove());
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

    private static long depthTwoNodes() {
        // The managed worker now shares TT hash ordering across iterations.
        try(var driver = new com.ohinteractive.seedv6.search.driver.SearchDriver()) {
            return driver.search(new SearchRequest(Board.startingPosition(), 2)).nodes();
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
        final AtomicReference<SearchControl> control = new AtomicReference<>();
        final CountDownLatch traversalCompleted = new CountDownLatch(1);
        final CountDownLatch allowReturn = new CountDownLatch(1);

        @Override
        public SearchResult search(SearchRequest request) {
            control.set(request.control());
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
