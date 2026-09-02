package com.ohinteractive.seedv6.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.IterationSnapshot;
import com.ohinteractive.seedv6.search.common.SearchTermination;
import com.ohinteractive.seedv6.search.flat.FlatNegamax;
import com.ohinteractive.seedv6.search.manage.ManagedSearchResult;
import com.ohinteractive.seedv6.search.manage.SearchLifecycleService;
import com.ohinteractive.seedv6.search.manage.SearchLimits;

class EngineSearchAdapterTest {

    @Test
    void workerCallbacksAreOnlyDeliveredThroughTheEdtQueue() throws Exception {
        final ManualQueue queue = new ManualQueue(2);
        final EngineSearchAdapter adapter = onEdt(() -> new EngineSearchAdapter(
            1, queue,
            ignored -> new SearchLifecycleService(com.ohinteractive.seedv6.search.common.TimeSource.SYSTEM, FlatNegamax::new)
        ));
        final List<String> deliveryThreads = new ArrayList<>();
        final AtomicInteger iterations = new AtomicInteger();
        final AtomicInteger results = new AtomicInteger();
        onEdt(() -> start(adapter, new SearchGateway.Listener() {
            @Override
            public void onIteration(Object uiToken, IterationSnapshot snapshot) {
                deliveryThreads.add(Thread.currentThread().getName());
                iterations.incrementAndGet();
            }

            @Override
            public void onComplete(Object uiToken, ManagedSearchResult result) {
                deliveryThreads.add(Thread.currentThread().getName());
                results.incrementAndGet();
            }
        }));
        assertTrue(queue.enqueued.await(5L, TimeUnit.SECONDS));
        assertEquals(0, iterations.get());
        assertEquals(0, results.get());
        assertTrue(queue.producerThreads.stream().allMatch("seedv6-search-worker"::equals));

        onEdt(queue::drain);
        assertEquals(1, iterations.get());
        assertEquals(1, results.get());
        assertTrue(deliveryThreads.stream().allMatch(name -> name.startsWith("AWT-EventQueue")));
        close(adapter);
    }

    @Test
    void invalidationAndServiceReplacementRejectAlreadyQueuedCallbacks() throws Exception {
        final ManualQueue queue = new ManualQueue(2);
        final EngineSearchAdapter adapter = onEdt(() -> new EngineSearchAdapter(
            1, queue,
            ignored -> new SearchLifecycleService(com.ohinteractive.seedv6.search.common.TimeSource.SYSTEM, FlatNegamax::new)
        ));
        final AtomicInteger deliveries = new AtomicInteger();
        onEdt(() -> start(adapter, new SearchGateway.Listener() {
            @Override public void onIteration(Object token, IterationSnapshot snapshot) { deliveries.incrementAndGet(); }
            @Override public void onComplete(Object token, ManagedSearchResult result) { deliveries.incrementAndGet(); }
        }));
        assertTrue(queue.enqueued.await(5L, TimeUnit.SECONDS));
        onEdt(() -> {
            adapter.invalidate(SearchTermination.POSITION_CHANGED);
            adapter.replaceWorkerCount(2);
        });
        onEdt(queue::drain);
        assertEquals(0, deliveries.get());
        assertEquals(2, adapter.workerCount());
        close(adapter);
    }

    @Test
    void productionSearchIsLegalAtThreadsOneTwoAndFour() throws Exception {
        for(int workers : List.of(1, 2, 4)) {
            final EngineSearchAdapter adapter = onEdt(() -> new EngineSearchAdapter(workers));
            final CompletableFuture<ManagedSearchResult> completed = new CompletableFuture<>();
            final AtomicBoolean deliveredOnEdt = new AtomicBoolean();
            onEdt(() -> start(adapter, new SearchGateway.Listener() {
                @Override public void onIteration(Object token, IterationSnapshot snapshot) {
                    deliveredOnEdt.set(SwingUtilities.isEventDispatchThread());
                }

                @Override public void onComplete(Object token, ManagedSearchResult result) {
                    deliveredOnEdt.set(deliveredOnEdt.get() && SwingUtilities.isEventDispatchThread());
                    completed.complete(result);
                }
            }));
            final ManagedSearchResult result = completed.get(10L, TimeUnit.SECONDS);
            assertTrue(result.hasMove(), "workers=" + workers);
            assertTrue(isGeneratedLegal(Board.startingPosition(), result.bestMove()), "workers=" + workers);
            assertTrue(deliveredOnEdt.get(), "workers=" + workers);
            assertFalse(onEdt(adapter::isSearching));
            close(adapter);
        }
    }

    @Test
    void rapidAtoBtoCReplacementDeliversOnlyTheNewestRequest() throws Exception {
        final EngineSearchAdapter adapter = onEdt(() -> new EngineSearchAdapter(1));
        final CompletableFuture<ManagedSearchResult> completed = new CompletableFuture<>();
        final AtomicInteger results = new AtomicInteger();
        final AtomicReference<Object> deliveredToken = new AtomicReference<>();
        final Object newest = new Object();
        onEdt(() -> {
            final long[] board = Board.startingPosition();
            final GameHistory history = GameHistory.initial(board);
            final SearchLimits limits = new SearchLimits(1, -1L, -1L, false);
            for(Object token : List.of(new Object(), new Object(), newest)) {
                adapter.start(board, history, limits, token, new SearchGateway.Listener() {
                    @Override public void onIteration(Object ignored, IterationSnapshot snapshot) {}

                    @Override public void onComplete(Object delivered, ManagedSearchResult result) {
                        deliveredToken.set(delivered);
                        results.incrementAndGet();
                        completed.complete(result);
                    }
                });
            }
        });
        assertTrue(completed.get(10L, TimeUnit.SECONDS).hasMove());
        onEdt(() -> {});
        assertEquals(1, results.get());
        assertTrue(deliveredToken.get() == newest);
        close(adapter);
    }

    @Test
    void activeFourThreadSearchClosesBoundedlyWithoutOwnedThreadLeaks() throws Exception {
        final EngineSearchAdapter adapter = onEdt(() -> new EngineSearchAdapter(4));
        onEdt(() -> {
            final long[] board = Board.startingPosition();
            adapter.start(
                board, GameHistory.initial(board),
                new SearchLimits(0, -1L, -1L, true), new Object(),
                new SearchGateway.Listener() {
                    @Override public void onIteration(Object token, IterationSnapshot snapshot) {}
                    @Override public void onComplete(Object token, ManagedSearchResult result) {}
                }
            );
        });
        assertTrue(hasOwnedSearchThread());
        final Runnable cleanup = onEdt(adapter::beginShutdown);
        final long start = System.nanoTime();
        cleanup.run();
        final long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        assertTrue(elapsedMillis < 4_500L, "shutdown took " + elapsedMillis + "ms");
        assertFalse(hasOwnedSearchThread());
    }

    private static final class ManualQueue implements Consumer<Runnable> {
        private final List<Runnable> tasks = new ArrayList<>();
        private final List<String> producerThreads = new ArrayList<>();
        private final CountDownLatch enqueued;

        private ManualQueue(int expected) {
            enqueued = new CountDownLatch(expected);
        }

        @Override
        public synchronized void accept(Runnable task) {
            tasks.add(task);
            producerThreads.add(Thread.currentThread().getName());
            enqueued.countDown();
        }

        private void drain() {
            final List<Runnable> pending;
            synchronized(this) {
                pending = List.copyOf(tasks);
                tasks.clear();
            }
            pending.forEach(Runnable::run);
        }
    }

    private static void start(EngineSearchAdapter adapter, SearchGateway.Listener listener) {
        final long[] board = Board.startingPosition();
        adapter.start(
            board, GameHistory.initial(board),
            new SearchLimits(1, -1L, -1L, false), new Object(), listener
        );
    }

    private static boolean isGeneratedLegal(long[] board, long expected) {
        final long[] moves = new long[256];
        final int count = Gen.genAll(
            board[0], board[1], board[2], board[3],
            (int) board[Board.STATUS], board[Board.KEY], true,
            moves, new long[Board.MAX_BITBOARDS]
        );
        for(int index = 0; index < count; index ++) {
            if(moves[index] == expected) return true;
        }
        return false;
    }

    private static boolean hasOwnedSearchThread() {
        return Thread.getAllStackTraces().keySet().stream().anyMatch(thread ->
            thread.isAlive() && (thread.getName().equals("seedv6-search-worker")
                || thread.getName().startsWith("seedv6-root-worker-"))
        );
    }

    private static void close(EngineSearchAdapter adapter) throws Exception {
        final Runnable cleanup = onEdt(adapter::beginShutdown);
        cleanup.run();
    }

    private static void onEdt(ThrowingRunnable action) throws Exception {
        onEdt(() -> {
            action.run();
            return null;
        });
    }

    private static <T> T onEdt(Callable<T> action) throws Exception {
        if(SwingUtilities.isEventDispatchThread()) return action.call();
        final FutureTask<T> task = new FutureTask<>(action);
        SwingUtilities.invokeAndWait(task);
        return task.get();
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
